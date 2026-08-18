package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.order.infrastructure.persistence.entity.NormalOrder;
import com.seckill.mall.order.infrastructure.persistence.entity.NormalOrderItem;
import com.seckill.mall.product.api.ProductApi;
import com.seckill.mall.product.api.dto.ProductSnapshot;
import com.seckill.mall.seckill.api.SeckillOrderApi;
import com.seckill.mall.seckill.api.dto.SeckillOrderDTO;
import com.seckill.mall.seckill.infrastructure.entity.SeckillOrder;
import com.seckill.mall.identity.infrastructure.entity.UserAddress;
import com.seckill.mall.order.infrastructure.persistence.entity.OrderStatus;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.order.infrastructure.persistence.mapper.NormalOrderItemMapper;
import com.seckill.mall.order.infrastructure.persistence.mapper.NormalOrderMapper;
import com.seckill.mall.service.OrderQueryService;

import com.seckill.mall.service.SeckillOrderService;
import com.seckill.mall.service.UserAddressService;
import com.seckill.mall.vo.NormalOrderDetailVO;
import com.seckill.mall.vo.NormalOrderItemVO;
import com.seckill.mall.vo.NormalOrderVO;
import com.seckill.mall.vo.OrderListItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单查询领域服务实现（Phase P1-1 从 OrderServiceImpl 沿查询职责缝拆分而来）。
 * <p>
 * 仅承载订单只读查询（详情 / 统一列表）+ 跨类型逻辑删除用例。
 * 业务逻辑与原 OrderServiceImpl 完全一致，仅做机械移动。
 * <p>
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderQueryServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryServiceImpl implements OrderQueryService {

    private final NormalOrderMapper normalOrderMapper;
    private final NormalOrderItemMapper normalOrderItemMapper;
    private final UserAddressService userAddressService;
    private final ProductApi productApi;
    // Phase SK.5：切换到 SeckillOrderApi（getSeckillOrdersForUnifiedList/logicalDeleteSeckillOrder）
    private final SeckillOrderApi seckillOrderApi;
    // 保留 SeckillOrderService：getSeckillOrderById 不在 API 中（API 不暴露 Entity）
    private final SeckillOrderService seckillOrderService;

    // ==================== 普通订单详情查询 ====================

    @Override
    public NormalOrderDetailVO getNormalOrderDetail(Long userId, Long orderId) {
        NormalOrder order = loadAndCheckNormalOrderOwnership(userId, orderId);
        // 超时检查：UNPAID 且已过支付截止时间，自动更新为 TIMEOUT
        checkAndHandleNormalOrderTimeout(order);
        List<NormalOrderItem> items = normalOrderItemMapper.selectList(
                new LambdaQueryWrapper<NormalOrderItem>()
                        .eq(NormalOrderItem::getOrderId, orderId)
                        .orderByAsc(NormalOrderItem::getId));
        return assembleDetail(order, items);
    }

    // ==================== 统一订单列表（需求1 合并秒杀+普通） ====================

    @Override
    public PageResult<OrderListItemVO> getUnifiedOrderList(Long userId, String status, String orderType,
                                                           Integer pageNum, Integer pageSize) {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);

        // 1. 解析状态筛选（null 表示不筛选）
        OrderStatus statusFilter = parseStatusFromString(status);
        // 1.1 解析订单类型筛选（null/空 表示不筛选，仅查询对应类型；NORMAL-仅普通订单，SECKILL-仅秒杀订单）
        String typeFilter = parseOrderType(orderType);

        // 2. 查询秒杀订单（按 userId + status 过滤，按 createTime 降序）
        // H11 修复：原代码全量加载后内存分页，存在性能与内存风险。
        // 此处添加 LIMIT 兜底（最多查 pageNum*pageSize 条），避免大用户量全表加载。
        // 完整方案应改为 DB 层分页（分别按页查询秒杀/普通订单后归并），此处先做限流保护。
        int maxLoad = num * size;
        List<SeckillOrderDTO> seckillOrders = java.util.Collections.emptyList();
        if (typeFilter == null || "SECKILL".equals(typeFilter)) {
            // Phase SK.5：改用 SeckillOrderApi 跨模块调用，返回 DTO 而非 Entity
            // 原本在此处构造 LambdaQueryWrapper，现已下沉至 SeckillOrderApplicationService#getSeckillOrdersForUnifiedList
            seckillOrders = seckillOrderApi.getSeckillOrdersForUnifiedList(userId, toStatusInt(statusFilter), maxLoad);
        }

        // 3. 查询普通订单（按 userId + status 过滤，按 createTime 降序）
        List<NormalOrder> normalOrders = java.util.Collections.emptyList();
        if (typeFilter == null || "NORMAL".equals(typeFilter)) {
            LambdaQueryWrapper<NormalOrder> normalWrapper = new LambdaQueryWrapper<NormalOrder>()
                    .eq(NormalOrder::getUserId, userId)
                    .orderByDesc(NormalOrder::getCreateTime)
                    .last("LIMIT " + maxLoad);
            if (statusFilter != null) {
                normalWrapper.eq(NormalOrder::getStatus, statusFilter);
            }
            normalOrders = normalOrderMapper.selectList(normalWrapper);
        }

        // 4. 批量查询秒杀订单对应的商品快照（避免 N+1）
        List<Long> skProductIds = seckillOrders.stream()
                .map(SeckillOrderDTO::getProductId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, ProductSnapshot> skProductMap = batchQueryProducts(skProductIds);

        // 5. 批量查询普通订单的明细（避免 N+1）
        List<Long> normalOrderIds = normalOrders.stream()
                .map(NormalOrder::getId)
                .collect(Collectors.toList());
        Map<Long, List<NormalOrderItem>> normalItemMap = batchQueryNormalItems(normalOrderIds);

        // 6. 转换为 OrderListItemVO
        List<OrderListItemVO> allList = new ArrayList<>(seckillOrders.size() + normalOrders.size());
        for (SeckillOrderDTO sk : seckillOrders) {
            allList.add(convertSeckillOrder(sk, skProductMap.get(sk.getProductId())));
        }
        for (NormalOrder no : normalOrders) {
            allList.add(convertNormalOrder(no, normalItemMap.getOrDefault(no.getId(), java.util.Collections.emptyList())));
        }

        // 7. 合并后按 createTime 降序排序
        allList.sort(Comparator.comparing(
                OrderListItemVO::getCreateTime,
                Comparator.nullsLast(Comparator.reverseOrder())));

        // 8. 内存分页
        int total = allList.size();
        int fromIndex = Math.min((num - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<OrderListItemVO> pageList = allList.subList(fromIndex, toIndex);

        return PageResult.of(pageList, total, num, size);
    }

    // ==================== 逻辑删除订单 ====================

    /**
     * 逻辑删除订单（需求：订单逻辑删除+类型筛选）。
     * <p>
     * 自动识别秒杀订单与普通订单：先查秒杀订单表，未命中再查普通订单表。
     * 仅允许 COMPLETED 或 CANCELLED 状态的订单逻辑删除，其他状态抛 {@code ORDER_DELETE_FAILED}。
     * 利用 MyBatis-Plus {@code @TableLogic} 注解，调用 deleteById/updateById 即可自动逻辑删除。
     *
     * @param orderId 订单 ID
     * @param userId  当前操作用户 ID
     * @return true 表示删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteOrder(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID/用户ID不能为空");
        }

        // 1. 先尝试秒杀订单表
        // Phase 7：改用 SeckillOrderService 跨模块内部调用，消除 SeckillOrderMapper 依赖
        SeckillOrder seckillOrder = seckillOrderService.getSeckillOrderById(orderId);
        if (seckillOrder != null) {
            // 校验归属
            if (!userId.equals(seckillOrder.getUserId())) {
                throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
            }
            // 校验状态：仅 COMPLETED / CANCELLED 可删除
            checkOrderDeletable(seckillOrder.getStatus());
            // 逻辑删除：@TableLogic 注解使 deleteById 自动 set is_deleted=1
            log.info("逻辑删除秒杀订单成功 orderId={} orderNo={} userId={}", orderId, seckillOrder.getOrderNo(), userId);
            return seckillOrderApi.logicalDeleteSeckillOrder(orderId);
        }

        // 2. 秒杀订单表未命中，尝试普通订单表
        NormalOrder normalOrder = normalOrderMapper.selectById(orderId);
        if (normalOrder != null) {
            // 校验归属
            if (!userId.equals(normalOrder.getUserId())) {
                throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
            }
            // 校验状态：仅 COMPLETED / CANCELLED 可删除
            checkOrderDeletable(normalOrder.getStatus());
            // 逻辑删除：@TableLogic 注解使 deleteById 自动 set is_deleted=1
            int rows = normalOrderMapper.deleteById(orderId);
            log.info("逻辑删除普通订单成功 orderId={} orderNo={} userId={}", orderId, normalOrder.getOrderNo(), userId);
            return rows > 0;
        }

        // 3. 两张表都未命中，订单不存在
        throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 加载普通订单并校验归属当前用户。
     */
    private NormalOrder loadAndCheckNormalOrderOwnership(Long userId, Long orderId) {
        NormalOrder order = normalOrderMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 组装订单详情视图。
     * <p>
     * 将 Entity（NormalOrder / NormalOrderItem）字段复制到独立 VO
     * （NormalOrderVO / NormalOrderItemVO），避免 VO 直接持有 Entity 引用
     * 而违反 ArchUnit VO→Entity 分层规则。字段名一致，JSON 响应结构不变。
     */
    private NormalOrderDetailVO assembleDetail(NormalOrder order, List<NormalOrderItem> items) {
        NormalOrderDetailVO vo = new NormalOrderDetailVO();
        NormalOrderVO orderVO = new NormalOrderVO();
        BeanUtils.copyProperties(order, orderVO);
        vo.setOrder(orderVO);
        List<NormalOrderItemVO> itemVOs = items.stream().map(item -> {
            NormalOrderItemVO itemVO = new NormalOrderItemVO();
            BeanUtils.copyProperties(item, itemVO);
            return itemVO;
        }).collect(Collectors.toList());
        vo.setItems(itemVOs);
        // 查询收货地址
        if (order.getAddressId() != null) {
            try {
                // Phase 7：改用 UserAddressService 跨模块内部调用，消除 UserAddressMapper 依赖
                UserAddress address = userAddressService.getAddressById(order.getAddressId());
                if (address != null) {
                    vo.setReceiverName(address.getReceiverName());
                    vo.setReceiverPhone(address.getReceiverPhone());
                    vo.setProvince(address.getProvince());
                    vo.setCity(address.getCity());
                    vo.setDistrict(address.getDistrict());
                    vo.setDetailAddress(address.getDetailAddress());
                }
            } catch (Exception e) {
                log.warn("查询订单收货地址失败, orderId={}, addressId={}", order.getId(), order.getAddressId(), e);
            }
        }
        return vo;
    }

    /**
     * 检查普通订单超时：如果状态为UNPAID且已过支付截止时间，更新为TIMEOUT。
     * 兜底RabbitMQ延迟消息可能丢失的场景。
     */
    private void checkAndHandleNormalOrderTimeout(NormalOrder order) {
        if (order.getStatus() == OrderStatus.UNPAID
                && order.getPayExpireTime() != null
                && LocalDateTime.now().isAfter(order.getPayExpireTime())) {
            int rows = normalOrderMapper.update(null, new LambdaUpdateWrapper<NormalOrder>()
                    .eq(NormalOrder::getId, order.getId())
                    .eq(NormalOrder::getStatus, OrderStatus.UNPAID)
                    .set(NormalOrder::getStatus, OrderStatus.TIMEOUT)
                    .set(NormalOrder::getCancelTime, LocalDateTime.now())
                    .set(NormalOrder::getCancelReason, "支付超时自动取消"));
            if (rows > 0) {
                order.setStatus(OrderStatus.TIMEOUT);
                order.setCancelTime(LocalDateTime.now());
                order.setCancelReason("支付超时自动取消");
                log.info("普通订单超时自动关闭, orderId={}", order.getId());
            }
        }
    }

    /**
     * 字符串状态 → OrderStatus 枚举（不区分大小写，非法值返回 null 表示不筛选）。
     */
    private OrderStatus parseStatusFromString(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.fromCode(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Phase 7：OrderStatus 枚举 → Integer 序号（与 SeckillOrderService#getSeckillOrdersForUnifiedList 契约对齐）。
     * <p>
     * 序号映射：0=UNPAID 1=PAID 2=SHIPPED 3=CANCELLED 4=TIMEOUT 5=COMPLETED，
     * 与 {@link SeckillOrderServiceImpl#parseStatus(Integer)} 互为逆函数。
     * CANCELLING 为中间状态，不应作为列表筛选条件，返回 null 表示不筛选。
     *
     * @param status 订单状态枚举，null 返回 null
     * @return 状态序号
     */
    private Integer toStatusInt(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case UNPAID -> 0;
            case PAID -> 1;
            case SHIPPED -> 2;
            case CANCELLED -> 3;
            case TIMEOUT -> 4;
            case COMPLETED -> 5;
            case CANCELLING -> null;
        };
    }

    /**
     * 解析订单类型参数（不区分大小写，非法值返回 null 表示不筛选）。
     *
     * @param orderType 订单类型字符串
     * @return "NORMAL" / "SECKILL" / null（不筛选）
     */
    private String parseOrderType(String orderType) {
        if (orderType == null || orderType.isBlank()) {
            return null;
        }
        String upper = orderType.toUpperCase();
        if ("NORMAL".equals(upper) || "SECKILL".equals(upper)) {
            return upper;
        }
        return null;
    }

    /**
     * 批量查询商品，返回 id → ProductSnapshot 映射。
     */
    private Map<Long, ProductSnapshot> batchQueryProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<ProductSnapshot> products = productApi.getProductsByIds(productIds);
        return products.stream().collect(Collectors.toMap(ProductSnapshot::getId, p -> p));
    }

    /**
     * 批量查询普通订单明细，按 orderId 分组。
     */
    private Map<Long, List<NormalOrderItem>> batchQueryNormalItems(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<NormalOrderItem> items = normalOrderItemMapper.selectList(
                new LambdaQueryWrapper<NormalOrderItem>()
                        .in(NormalOrderItem::getOrderId, orderIds)
                        .orderByAsc(NormalOrderItem::getId));
        return items.stream().collect(Collectors.groupingBy(NormalOrderItem::getOrderId));
    }

    /**
     * 秒杀订单 DTO → 统一订单列表项。
     * <p>
     * 秒杀订单仅含单个商品，items 列表只有一项；商品名称/主图取自商品快照，
     * 若商品已被删除则名称/主图为 null，不影响列表展示。
     * <p>
     * Phase SK.5：参数从 SeckillOrder Entity 改为 SeckillOrderDTO，
     * status 已是 String code，payAmount 对应原 totalAmount。
     */
    private OrderListItemVO convertSeckillOrder(SeckillOrderDTO order, ProductSnapshot product) {
        OrderListItemVO vo = new OrderListItemVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderType("SECKILL");
        vo.setStatus(order.getStatus());
        vo.setTotalAmount(order.getPayAmount());
        vo.setPayMethod(order.getPayMethod());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());

        OrderListItemVO.OrderItemSnapshot snapshot = new OrderListItemVO.OrderItemSnapshot();
        snapshot.setProductId(order.getProductId());
        snapshot.setProductName(product != null ? product.getName() : null);
        snapshot.setProductImage(product != null ? product.getMainImage() : null);
        snapshot.setUnitPrice(order.getSeckillPrice());
        snapshot.setQuantity(order.getQuantity());
        vo.setItems(List.of(snapshot));
        return vo;
    }

    /**
     * 普通订单 → 统一订单列表项。
     * <p>
     * items 列表来自普通订单明细，已保留下单时刻的商品快照（名称/主图/单价）。
     */
    private OrderListItemVO convertNormalOrder(NormalOrder order, List<NormalOrderItem> items) {
        OrderListItemVO vo = new OrderListItemVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderType("NORMAL");
        vo.setStatus(order.getStatus() != null ? order.getStatus().getCode() : null);
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayMethod(order.getPayMethod());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());

        List<OrderListItemVO.OrderItemSnapshot> snapshots = new ArrayList<>(items.size());
        for (NormalOrderItem item : items) {
            OrderListItemVO.OrderItemSnapshot snapshot = new OrderListItemVO.OrderItemSnapshot();
            snapshot.setProductId(item.getProductId());
            snapshot.setProductName(item.getProductName());
            snapshot.setProductImage(item.getProductImage());
            snapshot.setUnitPrice(item.getUnitPrice());
            snapshot.setQuantity(item.getQuantity());
            snapshots.add(snapshot);
        }
        vo.setItems(snapshots);
        return vo;
    }

    /**
     * 校验订单状态是否允许逻辑删除。
     * <p>
     * 仅 COMPLETED（已完成）与 CANCELLED（已取消）状态允许删除，
     * 其他状态（UNPAID/PAID/SHIPPED/TIMEOUT/CANCELLING）抛 {@code ORDER_DELETE_FAILED}。
     *
     * @param status 订单状态
     */
    private void checkOrderDeletable(OrderStatus status) {
        if (status != OrderStatus.COMPLETED && status != OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ORDER_DELETE_FAILED);
        }
    }
}