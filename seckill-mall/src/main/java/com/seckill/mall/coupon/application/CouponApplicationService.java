package com.seckill.mall.coupon.application;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.coupon.api.CouponApi;
import com.seckill.mall.coupon.api.command.ClaimCouponCommand;
import com.seckill.mall.coupon.api.command.CreateCouponCommand;
import com.seckill.mall.coupon.api.command.DistributeCouponCommand;
import com.seckill.mall.coupon.api.command.UpdateCouponCommand;
import com.seckill.mall.coupon.api.command.UpdateCouponStatusCommand;
import com.seckill.mall.coupon.api.dto.CouponDTO;
import com.seckill.mall.coupon.api.dto.UserCouponDTO;
import com.seckill.mall.coupon.api.query.CouponQuery;
import com.seckill.mall.coupon.api.query.UserCouponQuery;
import com.seckill.mall.coupon.application.facade.CouponApiConverter;
import com.seckill.mall.dto.CouponCreateRequest;
import com.seckill.mall.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Coupon 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link CouponApi}，内部委托给旧 {@link CouponService}，
 * 通过 {@link CouponApiConverter} 做 VO ↔ DTO 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做：
 * <ol>
 *     <li>从 Command/Query 提取业务参数</li>
 *     <li>委托旧 Service 执行业务</li>
 *     <li>将旧 Service 返回的 VO 转换为 API 层 DTO</li>
 * </ol>
 *
 * @author wnj
 * @since Phase CP.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponApplicationService implements CouponApi {

    private final CouponService couponService;

    // ==================== 前台 ====================

    @Override
    public List<CouponDTO> listAvailableCoupons(Long productId) {
        return CouponApiConverter.toDTOListFromVO(couponService.listAvailable(productId));
    }

    @Override
    public void claimCoupon(ClaimCouponCommand command) {
        couponService.receive(command.getCouponId(), command.getUserId());
    }

    @Override
    public List<UserCouponDTO> listMyCoupons(UserCouponQuery query) {
        return CouponApiConverter.toUserCouponDTOListFromVO(
                couponService.listMine(query.getUserId(), query.getStatus()));
    }

    // ==================== 后台管理 ====================

    @Override
    public PageResult<CouponDTO> adminListCoupons(CouponQuery query) {
        return CouponApiConverter.toDTOPageResultFromVO(
                couponService.listPage(query.getPageNum(), query.getPageSize(),
                        query.getName(), query.getStatus()));
    }

    @Override
    public CouponDTO createCoupon(CreateCouponCommand command) {
        CouponCreateRequest req = toRequest(command);
        return CouponApiConverter.toDTOFromVO(couponService.create(req));
    }

    @Override
    public CouponDTO updateCoupon(UpdateCouponCommand command) {
        CouponCreateRequest req = toRequest(command);
        return CouponApiConverter.toDTOFromVO(couponService.update(command.getId(), req));
    }

    @Override
    public void deleteCoupon(Long id) {
        couponService.delete(id);
    }

    @Override
    public void updateCouponStatus(UpdateCouponStatusCommand command) {
        couponService.updateStatus(command.getId(), command.getStatus());
    }

    @Override
    public String distributeCoupon(DistributeCouponCommand command) {
        return couponService.distribute(command.getCouponId(), command.getUserId());
    }

    @Override
    public PageResult<UserCouponDTO> adminListRecords(Long couponId, CouponQuery query) {
        // 旧 Service 返回 PageResult<AdminCouponRecordVO>，需转为 PageResult<UserCouponDTO>
        // AdminCouponRecordVO 与 UserCouponVO 字段重叠，通过中间 VO 转换
        PageResult<com.seckill.mall.coupon.interfaces.vo.AdminCouponRecordVO> pageResult =
                couponService.listRecords(couponId, query.getPageNum(), query.getPageSize());
        if (pageResult == null) {
            return PageResult.of(List.of(), 0L, 1L, 10L);
        }
        List<UserCouponDTO> dtoList = pageResult.getList() == null ? List.of()
                : pageResult.getList().stream()
                        .map(CouponApiConverter::toUserCouponDTOFromAdminRecordVO)
                        .collect(java.util.stream.Collectors.toList());
        return PageResult.of(dtoList, pageResult.getTotal(), pageResult.getPageNum(), pageResult.getPageSize());
    }

    // ==================== 私有方法 ====================

    /** CreateCouponCommand → CouponCreateRequest */
    private CouponCreateRequest toRequest(CreateCouponCommand command) {
        CouponCreateRequest req = new CouponCreateRequest();
        req.setName(command.getName());
        req.setType(command.getType());
        req.setAmount(command.getAmount());
        req.setMinAmount(command.getMinAmount());
        req.setTotalCount(command.getTotalCount());
        req.setStartTime(command.getStartTime());
        req.setEndTime(command.getEndTime());
        req.setStatus(command.getStatus());
        req.setScopeType(command.getScopeType());
        req.setCategoryId(command.getCategoryId());
        req.setProductId(command.getProductId());
        return req;
    }

    /** UpdateCouponCommand → CouponCreateRequest */
    private CouponCreateRequest toRequest(UpdateCouponCommand command) {
        CouponCreateRequest req = new CouponCreateRequest();
        req.setName(command.getName());
        req.setType(command.getType());
        req.setAmount(command.getAmount());
        req.setMinAmount(command.getMinAmount());
        req.setTotalCount(command.getTotalCount());
        req.setStartTime(command.getStartTime());
        req.setEndTime(command.getEndTime());
        req.setStatus(command.getStatus());
        req.setScopeType(command.getScopeType());
        req.setCategoryId(command.getCategoryId());
        req.setProductId(command.getProductId());
        return req;
    }
}