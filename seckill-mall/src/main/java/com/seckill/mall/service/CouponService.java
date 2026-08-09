package com.seckill.mall.service;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.CouponCreateRequest;
import com.seckill.mall.vo.AdminCouponRecordVO;
import com.seckill.mall.vo.CouponVO;
import com.seckill.mall.vo.UserCouponVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券服务接口
 * <p>
 * 提供后台管理与前台领取/查询的完整能力。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CouponService.java
 * 邮箱：nj651217@163.com
 */
public interface CouponService {

    // ==================== 后台管理 ====================

    /**
     * 分页查询优惠券列表（后台）
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param name     名称模糊筛选（可空）
     * @param status   状态筛选（可空）
     * @return 分页结果
     */
    PageResult<CouponVO> listPage(Integer pageNum, Integer pageSize, String name, Integer status);

    /**
     * 创建优惠券
     *
     * @param req 创建请求
     * @return 优惠券视图
     */
    CouponVO create(CouponCreateRequest req);

    /**
     * 编辑优惠券
     *
     * @param id  优惠券ID
     * @param req 更新请求
     * @return 优惠券视图
     */
    CouponVO update(Long id, CouponCreateRequest req);

    /**
     * 删除优惠券（逻辑删除）
     *
     * @param id 优惠券ID
     */
    void delete(Long id);

    /**
     * 启用/停用优惠券
     *
     * @param id     优惠券ID
     * @param status 状态：1-启用 / 0-停用
     */
    void updateStatus(Long id, Integer status);

    /**
     * 后台发放优惠券给指定用户
     *
     * @param couponId 优惠券ID
     * @param userId   用户ID
     * @return 被发放用户的用户名（用于前端展示，避免显示数字ID）
     */
    String distribute(Long couponId, Long userId);

    /**
     * 后台分页查询某优惠券的领取记录
     *
     * @param couponId 优惠券ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 领取记录分页结果（含用户名、优惠券名称等关联字段）
     */
    PageResult<AdminCouponRecordVO> listRecords(Long couponId, Integer pageNum, Integer pageSize);

    // ==================== 前台 ====================

    /**
     * 查询当前可领取的优惠券列表（启用、在有效期内、有剩余）
     * <p>
     * 支持按商品筛选：当 productId 不为空时，仅返回该商品可用的优惠券
     * （通用券 + 该商品所属分类券 + 该商品专属券）。
     *
     * @param productId 商品ID（可空，null 表示不按商品筛选，返回全部可领取券）
     * @return 优惠券列表
     */
    List<CouponVO> listAvailable(Long productId);

    /**
     * 用户领取优惠券
     *
     * @param couponId 优惠券ID
     * @param userId   用户ID
     */
    void receive(Long couponId, Long userId);

    /**
     * 查询我的优惠券列表
     *
     * @param userId 用户ID
     * @param status 状态筛选：UNUSED/USED/EXPIRED（可空）
     * @return 用户优惠券列表
     */
    List<UserCouponVO> listMine(Long userId, String status);

    /**
     * 计算优惠金额（按 scope 筛选适用商品小计）。
     * <p>
     * 校验用户优惠券归属、状态（UNUSED）、有效期，按券类型计算优惠金额：
     * <ul>
     *   <li>AMOUNT（满减）：discount = coupon.amount</li>
     *   <li>DISCOUNT（折扣）：discount = 适用小计 × (1 - coupon.amount)</li>
     * </ul>
     * 未达门槛 / 已过期 / 已使用 等情况返回 {@link BigDecimal#ZERO}。
     *
     * @param userCouponId 用户优惠券ID
     * @param userId       用户ID（校验归属）
     * @param orderAmount  适用商品小计（前端按 scope 预计算的参与优惠的金额）
     * @param productIds   订单商品ID列表（预留，当前简化处理使用 orderAmount）
     * @return 优惠金额（非负），不可用时返回 ZERO
     */
    BigDecimal calculateDiscount(Long userCouponId, Long userId, BigDecimal orderAmount, List<Long> productIds);

    /**
     * 核销优惠券（支付成功时调用）。
     * <p>
     * 校验用户优惠券归属与状态（UNUSED），更新为 USED，记录使用时间与订单ID，
     * 并将 Coupon.usedCount 原子 +1。
     *
     * @param userCouponId 用户优惠券ID
     * @param userId       用户ID（校验归属）
     * @param orderId      关联订单ID
     */
    void useCoupon(Long userCouponId, Long userId, Long orderId);

    /**
     * 回退优惠券（取消订单 / 退款时调用）。
     * <p>
     * 校验用户优惠券归属与状态（USED），更新回 UNUSED，清除使用时间与订单ID，
     * 并将 Coupon.usedCount 原子 -1。
     *
     * @param userCouponId 用户优惠券ID
     * @param userId       用户ID（校验归属）
     */
    void revertCoupon(Long userCouponId, Long userId);
}