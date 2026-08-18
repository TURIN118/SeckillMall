package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.CouponCreateRequest;
import com.seckill.mall.entity.Category;
import com.seckill.mall.entity.Coupon;
import com.seckill.mall.product.api.ProductApi;
import com.seckill.mall.product.api.dto.ProductSnapshot;
import com.seckill.mall.identity.api.UserApi;
import com.seckill.mall.identity.api.dto.UserSnapshot;
import com.seckill.mall.entity.UserCoupon;
import com.seckill.mall.entity.enums.CouponType;
import com.seckill.mall.entity.enums.UserCouponStatus;
import com.seckill.mall.mapper.CouponMapper;
import com.seckill.mall.mapper.UserCouponMapper;
import com.seckill.mall.service.CategoryService;
import com.seckill.mall.service.CouponService;
import com.seckill.mall.vo.AdminCouponRecordVO;
import com.seckill.mall.vo.CouponVO;
import com.seckill.mall.vo.UserCouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 优惠券服务实现
 * <p>
 * 后台：创建/编辑/删除/启停/发放给用户/分页查询。
 * 前台：可领取列表/领取/我的优惠券。
 * <p>
 * 库存语义：{@code received_count} 在领取时 +1，{@code used_count} 在使用时 +1。
 * 领取校验：启用状态、有效期、剩余库存（{@code total_count - received_count > 0}）。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CouponServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    /** 启用状态 */
    private static final int STATUS_ENABLED = 1;
    /** 停用状态 */
    private static final int STATUS_DISABLED = 0;
    /** 适用范围：全站通用 */
    private static final String SCOPE_ALL = "ALL";
    /** 适用范围：指定分类 */
    private static final String SCOPE_CATEGORY = "CATEGORY";
    /** 适用范围：指定商品 */
    private static final String SCOPE_PRODUCT = "PRODUCT";

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final UserApi userApi;
    private final ProductApi productApi;
    private final CategoryService categoryService;

    // ==================== 后台管理 ====================

    @Override
    public PageResult<CouponVO> listPage(Integer pageNum, Integer pageSize, String name, Integer status) {
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 ? 10 : pageSize;
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<Coupon>()
                .orderByDesc(Coupon::getCreateTime);
        if (name != null && !name.isBlank()) {
            wrapper.like(Coupon::getName, name);
        }
        if (status != null) {
            wrapper.eq(Coupon::getStatus, status);
        }
        IPage<Coupon> page = couponMapper.selectPage(new Page<>(pn, ps), wrapper);
        List<CouponVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponVO create(CouponCreateRequest req) {
        validateCouponRequest(req);
        Coupon coupon = new Coupon();
        coupon.setName(req.getName());
        coupon.setType(CouponType.fromCode(req.getType()));
        coupon.setAmount(req.getAmount());
        coupon.setMinAmount(req.getMinAmount());
        coupon.setTotalCount(req.getTotalCount());
        coupon.setReceivedCount(0);
        coupon.setUsedCount(0);
        coupon.setStartTime(req.getStartTime());
        coupon.setEndTime(req.getEndTime());
        coupon.setStatus(req.getStatus() == null ? STATUS_ENABLED : req.getStatus());
        // 适用范围：默认 ALL
        coupon.setScopeType(req.getScopeType() == null ? SCOPE_ALL : req.getScopeType());
        coupon.setCategoryId(req.getCategoryId());
        coupon.setProductId(req.getProductId());
        couponMapper.insert(coupon);
        log.info("创建优惠券成功，id={}, name={}, scopeType={}", coupon.getId(), coupon.getName(), coupon.getScopeType());
        return toVO(coupon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponVO update(Long id, CouponCreateRequest req) {
        Coupon existing = getExistingCoupon(id);
        validateCouponRequest(req);
        existing.setName(req.getName());
        existing.setType(CouponType.fromCode(req.getType()));
        existing.setAmount(req.getAmount());
        existing.setMinAmount(req.getMinAmount());
        existing.setTotalCount(req.getTotalCount());
        existing.setStartTime(req.getStartTime());
        existing.setEndTime(req.getEndTime());
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }
        // 适用范围：默认 ALL
        existing.setScopeType(req.getScopeType() == null ? SCOPE_ALL : req.getScopeType());
        existing.setCategoryId(req.getCategoryId());
        existing.setProductId(req.getProductId());
        couponMapper.updateById(existing);
        log.info("编辑优惠券成功，id={}", id);
        return toVO(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getExistingCoupon(id);
        couponMapper.deleteById(id);
        log.info("删除优惠券成功，id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Coupon existing = getExistingCoupon(id);
        if (status == null || (status != STATUS_ENABLED && status != STATUS_DISABLED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态值非法");
        }
        Coupon toUpdate = new Coupon();
        toUpdate.setId(id);
        toUpdate.setStatus(status);
        couponMapper.updateById(toUpdate);
        log.info("切换优惠券状态成功，id={}, status={}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String distribute(Long couponId, Long userId) {
        Coupon coupon = getExistingCoupon(couponId);
        if (coupon.getStatus() != STATUS_ENABLED) {
            throw new BusinessException(ErrorCode.COUPON_DISABLED);
        }
        // 校验用户存在
        UserSnapshot user = userApi.getUserById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 检查是否已领取过
        Long existCount = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, couponId));
        if (existCount != null && existCount > 0) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_RECEIVED);
        }
        // 创建用户优惠券记录
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(UserCouponStatus.UNUSED);
        userCoupon.setReceiveTime(LocalDateTime.now());
        userCouponMapper.insert(userCoupon);
        // H8/L20 修复：原子更新 received_count，避免"先查后改"并发超卖
        // WHERE received_count < total_count 保证不超发；返回 0 表示库存不足
        int rows = couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
                .eq(Coupon::getId, couponId)
                .lt(Coupon::getReceivedCount, coupon.getTotalCount())
                .setSql("received_count = received_count + 1"));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.COUPON_OUT_OF_STOCK);
        }
        log.info("发放优惠券成功，couponId={}, userId={}", couponId, userId);
        return user.getUsername();
    }

    @Override
    public PageResult<AdminCouponRecordVO> listRecords(Long couponId, Integer pageNum, Integer pageSize) {
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 ? 10 : pageSize;
        // 分页查询该优惠券的领取记录
        IPage<UserCoupon> page = userCouponMapper.selectPage(
                new Page<>(pn, ps),
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getCouponId, couponId)
                        .orderByDesc(UserCoupon::getCreateTime));
        List<UserCoupon> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(Collections.emptyList(), page.getTotal(), page.getCurrent(), page.getSize());
        }
        // 批量查询关联用户名
        List<Long> userIds = records.stream()
                .map(UserCoupon::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> usernameMap = userApi.getUsernamesByIds(userIds);
        // 查询优惠券名称
        Coupon coupon = couponMapper.selectById(couponId);
        String couponName = coupon == null ? null : coupon.getName();
        // 组装 VO
        List<AdminCouponRecordVO> voList = records.stream()
                .map(uc -> toAdminRecordVO(uc, usernameMap.get(uc.getUserId()), couponName))
                .collect(Collectors.toList());
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    // ==================== 前台 ====================

    @Override
    public List<CouponVO> listAvailable(Long productId) {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> coupons = couponMapper.selectList(
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getStatus, STATUS_ENABLED)
                        .le(Coupon::getStartTime, now)
                        .ge(Coupon::getEndTime, now)
                        .orderByDesc(Coupon::getCreateTime));
        // 过滤出有剩余库存的
        List<Coupon> available = coupons.stream()
                .filter(c -> c.getReceivedCount() == null || c.getTotalCount() == null
                        || c.getReceivedCount() < c.getTotalCount())
                .collect(Collectors.toList());
        // 按商品筛选：仅保留该商品可用的券（通用券 + 该商品分类券 + 该商品专属券）
        if (productId != null) {
            ProductSnapshot product = productApi.getProductById(productId);
            Long productCategoryId = product == null ? null : product.getCategoryId();
            available = available.stream()
                    .filter(c -> isCouponApplicableToProduct(c, productId, productCategoryId))
                    .collect(Collectors.toList());
        }
        return available.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 判断优惠券是否适用于指定商品。
     * <ul>
     *   <li>scopeType=ALL：通用券，适用于所有商品</li>
     *   <li>scopeType=CATEGORY：分类券，仅当商品分类ID等于券的 categoryId 时适用</li>
     *   <li>scopeType=PRODUCT：商品券，仅当商品ID等于券的 productId 时适用</li>
     * </ul>
     *
     * @param coupon             优惠券
     * @param productId          商品ID
     * @param productCategoryId  商品所属分类ID（可空）
     * @return true 表示该商品可用此优惠券
     */
    private boolean isCouponApplicableToProduct(Coupon coupon, Long productId, Long productCategoryId) {
        String scope = coupon.getScopeType();
        if (scope == null || SCOPE_ALL.equals(scope)) {
            return true;
        }
        if (SCOPE_CATEGORY.equals(scope)) {
            return productCategoryId != null && productCategoryId.equals(coupon.getCategoryId());
        }
        if (SCOPE_PRODUCT.equals(scope)) {
            return productId != null && productId.equals(coupon.getProductId());
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long couponId, Long userId) {
        Coupon coupon = getExistingCoupon(couponId);
        if (coupon.getStatus() != STATUS_ENABLED) {
            throw new BusinessException(ErrorCode.COUPON_DISABLED);
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_STARTED);
        }
        if (now.isAfter(coupon.getEndTime())) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }
        // M16: 重复检查非原子，依赖 (user_id, coupon_id) 唯一索引兜底；
        // 若并发穿透触发 DuplicateKeyException，转换为业务异常
        Long existCount = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, couponId));
        if (existCount != null && existCount > 0) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_RECEIVED);
        }
        // 创建领取记录
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(UserCouponStatus.UNUSED);
        userCoupon.setReceiveTime(now);
        try {
            userCouponMapper.insert(userCoupon);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_RECEIVED);
        }
        // H8 修复：原子更新 received_count，避免"先查后改"并发超卖
        // WHERE received_count < total_count 保证不超发；返回 0 表示库存不足
        int rows = couponMapper.update(null, new LambdaUpdateWrapper<Coupon>()
                .eq(Coupon::getId, couponId)
                .lt(Coupon::getReceivedCount, coupon.getTotalCount())
                .setSql("received_count = received_count + 1"));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.COUPON_OUT_OF_STOCK);
        }
        log.info("用户领取优惠券成功，couponId={}, userId={}", couponId, userId);
    }

    @Override
    public List<UserCouponVO> listMine(Long userId, String status) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .orderByDesc(UserCoupon::getCreateTime);
        if (status != null && !status.isBlank()) {
            wrapper.eq(UserCoupon::getStatus, UserCouponStatus.fromCode(status));
        }
        List<UserCoupon> userCoupons = userCouponMapper.selectList(wrapper);
        if (userCoupons.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量查询关联的优惠券信息
        List<Long> couponIds = userCoupons.stream()
                .map(UserCoupon::getCouponId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Coupon> couponMap = couponMapper.selectList(
                        new LambdaQueryWrapper<Coupon>().in(Coupon::getId, couponIds))
                .stream().collect(Collectors.toMap(Coupon::getId, c -> c));
        // 查询当前用户名（批量场景下仅一个用户，单查即可）
        UserSnapshot user = userApi.getUserById(userId);
        String username = user == null ? null : user.getUsername();
        return userCoupons.stream()
                .map(uc -> toUserCouponVO(uc, couponMap.get(uc.getCouponId()), username))
                .collect(Collectors.toList());
    }


    // ==================== 私有方法 ====================

    /**
     * 获取存在的优惠券，否则抛异常
     */
    private Coupon getExistingCoupon(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
        }
        return coupon;
    }

    /** 校验优惠券创建/更新请求 */
    private void validateCouponRequest(CouponCreateRequest req) {
        // 类型校验
        try {
            CouponType.fromCode(req.getType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠券类型非法");
        }
        // 折扣类型：amount 必须在 (0, 1) 之间
        if ("DISCOUNT".equals(req.getType())) {
            if (req.getAmount() == null
                    || req.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0
                    || req.getAmount().compareTo(java.math.BigDecimal.ONE) >= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "折扣值必须介于0和1之间");
            }
        }
        // 满减类型：amount 必须 > 0
        if ("AMOUNT".equals(req.getType())) {
            if (req.getAmount() == null
                    || req.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "满减金额必须大于0");
            }
        }
        // 时间校验
        if (req.getStartTime() != null && req.getEndTime() != null
                && !req.getStartTime().isBefore(req.getEndTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "有效期开始时间必须早于结束时间");
        }
    }

    /** Entity → VO */
    private CouponVO toVO(Coupon coupon) {
        CouponVO vo = new CouponVO();
        vo.setId(coupon.getId());
        vo.setName(coupon.getName());
        vo.setType(coupon.getType() == null ? null : coupon.getType().getCode());
        vo.setAmount(coupon.getAmount());
        vo.setMinAmount(coupon.getMinAmount());
        vo.setTotalCount(coupon.getTotalCount());
        vo.setReceivedCount(coupon.getReceivedCount());
        vo.setUsedCount(coupon.getUsedCount());
        if (coupon.getTotalCount() != null && coupon.getReceivedCount() != null) {
            vo.setRemainCount(coupon.getTotalCount() - coupon.getReceivedCount());
        }
        vo.setStartTime(coupon.getStartTime());
        vo.setEndTime(coupon.getEndTime());
        vo.setStatus(coupon.getStatus());
        // 适用范围
        vo.setScopeType(coupon.getScopeType());
        vo.setCategoryId(coupon.getCategoryId());
        vo.setProductId(coupon.getProductId());
        vo.setScopeLabel(buildScopeLabel(coupon));
        vo.setCreateTime(coupon.getCreateTime());
        vo.setUpdateTime(coupon.getUpdateTime());
        return vo;
    }

    /**
     * 构建适用范围标签：ALL→null / CATEGORY→"仅限XX分类" / PRODUCT→"仅限XX商品"。
     * 查询失败返回兜底文案，不抛异常。
     */
    private String buildScopeLabel(Coupon coupon) {
        String scope = coupon.getScopeType();
        if (scope == null || SCOPE_ALL.equals(scope)) {
            return null;
        }
        if (SCOPE_CATEGORY.equals(scope)) {
            if (coupon.getCategoryId() == null) {
                return "仅限指定分类";
            }
            Category category = categoryService.getCategoryById(coupon.getCategoryId());
            String name = category == null ? "指定分类" : category.getName();
            return "仅限" + name;
        }
        if (SCOPE_PRODUCT.equals(scope)) {
            if (coupon.getProductId() == null) {
                return "仅限指定商品";
            }
            ProductSnapshot product = productApi.getProductById(coupon.getProductId());
            String name = product == null ? "指定商品" : product.getName();
            return "仅限" + name;
        }
        return null;
    }

    /** UserCoupon + Coupon + username → UserCouponVO */
    private UserCouponVO toUserCouponVO(UserCoupon userCoupon, Coupon coupon, String username) {
        UserCouponVO vo = new UserCouponVO();
        vo.setId(userCoupon.getId());
        vo.setUserId(userCoupon.getUserId());
        vo.setUsername(username);
        vo.setCouponId(userCoupon.getCouponId());
        vo.setStatus(userCoupon.getStatus() == null ? null : userCoupon.getStatus().getCode());
        vo.setReceiveTime(userCoupon.getReceiveTime());
        vo.setUseTime(userCoupon.getUseTime());
        vo.setOrderId(userCoupon.getOrderId());
        vo.setCreateTime(userCoupon.getCreateTime());
        if (coupon != null) {
            vo.setCouponName(coupon.getName());
            vo.setCouponType(coupon.getType() == null ? null : coupon.getType().getCode());
            vo.setCouponAmount(coupon.getAmount());
            vo.setMinAmount(coupon.getMinAmount());
            vo.setCouponStartTime(coupon.getStartTime());
            vo.setCouponEndTime(coupon.getEndTime());
        }
        return vo;
    }

    /** UserCoupon → AdminCouponRecordVO（后台领取记录） */
    private AdminCouponRecordVO toAdminRecordVO(UserCoupon userCoupon, String username, String couponName) {
        AdminCouponRecordVO vo = new AdminCouponRecordVO();
        vo.setId(userCoupon.getId());
        vo.setUserId(userCoupon.getUserId());
        vo.setUsername(username);
        vo.setCouponId(userCoupon.getCouponId());
        vo.setCouponName(couponName);
        vo.setStatus(userCoupon.getStatus() == null ? null : userCoupon.getStatus().getCode());
        vo.setReceiveTime(userCoupon.getReceiveTime());
        vo.setUseTime(userCoupon.getUseTime());
        vo.setOrderId(userCoupon.getOrderId());
        vo.setCreateTime(userCoupon.getCreateTime());
        return vo;
    }
}