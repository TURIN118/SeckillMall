package com.seckill.mall.service;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.CouponCreateRequest;
import com.seckill.mall.vo.CouponVO;
import com.seckill.mall.vo.UserCouponVO;

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
     */
    void distribute(Long couponId, Long userId);

    // ==================== 前台 ====================

    /**
     * 查询当前可领取的优惠券列表（启用、在有效期内、有剩余）
     *
     * @return 优惠券列表
     */
    List<CouponVO> listAvailable();

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
}