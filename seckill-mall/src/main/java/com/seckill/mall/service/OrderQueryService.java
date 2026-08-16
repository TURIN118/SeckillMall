package com.seckill.mall.service;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.vo.NormalOrderDetailVO;
import com.seckill.mall.vo.OrderListItemVO;

/**
 * 订单查询领域服务（Phase P1-1 从 OrderService 沿查询职责缝拆分而来）。
 * <p>
 * 仅承载订单只读查询 + 跨类型逻辑删除用例，与 {@link OrderService} 的写操作用例完全独立。
 * 业务逻辑与原 OrderServiceImpl 完全一致，仅做机械移动。
 * <p>
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderQueryService.java
 * 邮箱：nj651217@163.com
 */
public interface OrderQueryService {

    /**
     * 查询普通订单详情（含明细列表）。
     *
     * @param userId  用户 ID
     * @param orderId 订单 ID
     * @return 订单详情视图
     */
    NormalOrderDetailVO getNormalOrderDetail(Long userId, Long orderId);

    /**
     * 统一订单列表（秒杀订单 + 普通订单合并展示，需求1）。
     * <p>
     * 按 userId + status + orderType 过滤两套订单表，合并后按 createTime 降序排序，
     * 在内存中分页（订单量不大，内存分页可接受）。
     *
     * @param userId    用户 ID
     * @param status    订单状态筛选（可空，传 null 表示不筛选）；可选值：
     *                  UNPAID/PAID/CANCELLED/TIMEOUT/COMPLETED
     * @param orderType 订单类型筛选（可空，传 null 表示不筛选）；可选值：NORMAL/SECKILL
     * @param pageNum   页码（从 1 开始，默认 1）
     * @param pageSize  每页大小（默认 10，上限 50）
     * @return 统一订单列表分页结果
     */
    PageResult<OrderListItemVO> getUnifiedOrderList(Long userId, String status, String orderType,
                                                    Integer pageNum, Integer pageSize);

    /**
     * 逻辑删除订单（需求：订单逻辑删除+类型筛选）。
     * <p>
     * 支持秒杀订单与普通订单两类，根据订单 ID 自动识别所属表。
     * 仅允许 COMPLETED 或 CANCELLED 状态的订单删除，其他状态抛 {@code ORDER_DELETE_FAILED}。
     * 删除方式为 MyBatis-Plus 逻辑删除（{@code @TableLogic}），不物理删除数据。
     *
     * @param orderId 订单 ID（秒杀订单或普通订单）
     * @param userId  当前操作用户 ID（用于归属校验）
     * @return true 表示删除成功
     * @throws com.seckill.mall.common.BusinessException 订单不存在/不属于当前用户/状态不允许删除
     */
    boolean deleteOrder(Long orderId, Long userId);
}