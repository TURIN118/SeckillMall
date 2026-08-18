package com.seckill.mall.coupon.api;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.coupon.api.command.ClaimCouponCommand;
import com.seckill.mall.coupon.api.command.CreateCouponCommand;
import com.seckill.mall.coupon.api.command.DistributeCouponCommand;
import com.seckill.mall.coupon.api.command.UpdateCouponCommand;
import com.seckill.mall.coupon.api.command.UpdateCouponStatusCommand;
import com.seckill.mall.coupon.api.dto.CouponDTO;
import com.seckill.mall.coupon.api.dto.UserCouponDTO;
import com.seckill.mall.coupon.api.query.CouponQuery;
import com.seckill.mall.coupon.api.query.UserCouponQuery;

import java.util.List;

/**
 * Coupon 模块优惠券管理 API。
 *
 * <p>对外暴露优惠券后台管理（CRUD/启停/发放/领取记录）与前台领取/查询等契约。
 *
 * <p>设计原则：
 * <ul>
 *     <li>方法用业务语言命名，禁止 CRUD 命名</li>
 *     <li>入参用 Command/Query 对象，禁止裸露多参数</li>
 *     <li>出参用 Result/DTO/Snapshot，禁止暴露 Entity/Mapper/PO</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出，不泄露堆栈</li>
 * </ul>
 *
 * <p>向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型。
 *
 * <p>原方法映射参见 COUPON-API-CONTRACT.md 第 8.1 节。
 *
 * @author wnj
 * @since Phase CP.2
 */
public interface CouponApi {

    // ==================== 前台 ====================

    /**
     * 查询当前可领取的优惠券列表（启用、在有效期内、有剩余）。
     * <p>
     * 支持按商品筛选：当 productId 不为空时，仅返回该商品可用的优惠券
     * （通用券 + 该商品所属分类券 + 该商品专属券）。
     *
     * @param productId 商品 ID（可空，null 表示不按商品筛选，返回全部可领取券）
     * @return 优惠券 DTO 列表
     */
    List<CouponDTO> listAvailableCoupons(Long productId);

    /**
     * 用户领取优惠券（校验启用/有效期/库存/重复领取）。
     *
     * @param command 领取命令
     */
    void claimCoupon(ClaimCouponCommand command);

    /**
     * 查询我的优惠券列表（可按状态筛选）。
     *
     * @param query 查询条件
     * @return 用户优惠券 DTO 列表
     */
    List<UserCouponDTO> listMyCoupons(UserCouponQuery query);

    // ==================== 后台管理 ====================

    /**
     * 分页查询优惠券列表（后台）。
     *
     * @param query 查询条件（含 pageNum/pageSize/name/status）
     * @return 分页结果
     */
    PageResult<CouponDTO> adminListCoupons(CouponQuery query);

    /**
     * 创建优惠券。
     *
     * @param command 创建命令
     * @return 优惠券 DTO
     */
    CouponDTO createCoupon(CreateCouponCommand command);

    /**
     * 编辑优惠券。
     *
     * @param command 编辑命令
     * @return 优惠券 DTO
     */
    CouponDTO updateCoupon(UpdateCouponCommand command);

    /**
     * 删除优惠券（逻辑删除）。
     *
     * @param id 优惠券 ID
     */
    void deleteCoupon(Long id);

    /**
     * 启用/停用优惠券。
     *
     * @param command 启停命令
     */
    void updateCouponStatus(UpdateCouponStatusCommand command);

    /**
     * 后台发放优惠券给指定用户。
     *
     * @param command 发放命令
     * @return 被发放用户的用户名（用于前端展示，避免显示数字ID）
     */
    String distributeCoupon(DistributeCouponCommand command);

    /**
     * 后台分页查询某优惠券的领取记录。
     *
     * @param couponId 优惠券 ID
     * @param query    分页查询条件（含 pageNum/pageSize）
     * @return 领取记录分页结果（含用户名、优惠券名称等关联字段）
     */
    PageResult<UserCouponDTO> adminListRecords(Long couponId, CouponQuery query);
}