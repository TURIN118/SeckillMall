package com.seckill.mall.coupon.application.facade;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.coupon.api.command.ClaimCouponCommand;
import com.seckill.mall.coupon.api.command.CreateCouponCommand;
import com.seckill.mall.coupon.api.command.DistributeCouponCommand;
import com.seckill.mall.coupon.api.command.RevertCouponCommand;
import com.seckill.mall.coupon.api.command.UpdateCouponCommand;
import com.seckill.mall.coupon.api.command.UpdateCouponStatusCommand;
import com.seckill.mall.coupon.api.command.UseCouponCommand;
import com.seckill.mall.coupon.api.dto.CouponDTO;
import com.seckill.mall.coupon.api.dto.UserCouponDTO;
import com.seckill.mall.coupon.api.query.CouponQuery;
import com.seckill.mall.coupon.api.query.UserCouponQuery;
import com.seckill.mall.coupon.infrastructure.entity.Coupon;
import com.seckill.mall.coupon.infrastructure.entity.UserCoupon;
import com.seckill.mall.coupon.interfaces.vo.AdminCouponRecordVO;
import com.seckill.mall.coupon.interfaces.vo.CouponVO;
import com.seckill.mall.coupon.interfaces.vo.UserCouponVO;
import com.seckill.mall.dto.CouponCreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Coupon API 转换辅助类。
 *
 * <p>集中存放旧 VO/Entity 与新 API 层 DTO/Command 之间的转换方法，
 * 供 CouponApplicationService / CouponUsageApplicationService / Controller 调用。
 * 所有方法均为无状态静态方法，标注 {@code @Component} 仅为便于未来扩展为 Bean 注入方式。
 *
 * <p>转换原则：
 * <ul>
 *     <li>Entity → DTO：枚举字段取 getCode()，丢弃 isDeleted 等基础设施字段</li>
 *     <li>VO → DTO：全字段映射（核心 + 展示），保留前端展示信息</li>
 *     <li>DTO → VO：全字段映射（核心 + 展示），保留前端契约</li>
 *     <li>Request → Command：字段一一对应，丢弃 Bean Validation 注解</li>
 * </ul>
 *
 * @author wnj
 * @since Phase CP.4
 */
@Slf4j
@Component
public class CouponApiConverter {

    // ============================================================
    // Coupon Entity → CouponDTO 转换
    // ============================================================

    /**
     * 将 {@link Coupon} Entity 转换为 {@link CouponDTO}。
     *
     * @param entity 优惠券 Entity
     * @return 优惠券 DTO
     */
    public static CouponDTO toDTO(Coupon entity) {
        if (entity == null) {
            return null;
        }
        return CouponDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType() == null ? null : entity.getType().getCode())
                .amount(entity.getAmount())
                .minAmount(entity.getMinAmount())
                .totalCount(entity.getTotalCount())
                .receivedCount(entity.getReceivedCount())
                .usedCount(entity.getUsedCount())
                .remainCount(entity.getTotalCount() != null && entity.getReceivedCount() != null
                        ? entity.getTotalCount() - entity.getReceivedCount() : null)
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .status(entity.getStatus())
                .scopeType(entity.getScopeType())
                .categoryId(entity.getCategoryId())
                .productId(entity.getProductId())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    /**
     * 将 {@link Coupon} Entity 列表转换为 {@link CouponDTO} 列表。
     */
    public static List<CouponDTO> toDTOList(List<Coupon> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(CouponApiConverter::toDTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // CouponVO → CouponDTO 转换（全字段，保留展示信息）
    // ============================================================

    /**
     * 将 {@link CouponVO} 转换为 {@link CouponDTO}。
     */
    public static CouponDTO toDTOFromVO(CouponVO vo) {
        if (vo == null) {
            return null;
        }
        return CouponDTO.builder()
                .id(vo.getId())
                .name(vo.getName())
                .type(vo.getType())
                .amount(vo.getAmount())
                .minAmount(vo.getMinAmount())
                .totalCount(vo.getTotalCount())
                .receivedCount(vo.getReceivedCount())
                .usedCount(vo.getUsedCount())
                .remainCount(vo.getRemainCount())
                .startTime(vo.getStartTime())
                .endTime(vo.getEndTime())
                .status(vo.getStatus())
                .scopeType(vo.getScopeType())
                .categoryId(vo.getCategoryId())
                .productId(vo.getProductId())
                .scopeLabel(vo.getScopeLabel())
                .createTime(vo.getCreateTime())
                .updateTime(vo.getUpdateTime())
                .build();
    }

    /**
     * 将 {@link CouponVO} 列表转换为 {@link CouponDTO} 列表。
     */
    public static List<CouponDTO> toDTOListFromVO(List<CouponVO> voList) {
        if (voList == null) {
            return Collections.emptyList();
        }
        return voList.stream()
                .map(CouponApiConverter::toDTOFromVO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // CouponDTO → CouponVO 转换（Controller 层前端契约适配，全字段）
    // ============================================================

    /**
     * 将 {@link CouponDTO} 转换为 {@link CouponVO}。
     */
    public static CouponVO toVO(CouponDTO dto) {
        if (dto == null) {
            return null;
        }
        CouponVO vo = new CouponVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setType(dto.getType());
        vo.setAmount(dto.getAmount());
        vo.setMinAmount(dto.getMinAmount());
        vo.setTotalCount(dto.getTotalCount());
        vo.setReceivedCount(dto.getReceivedCount());
        vo.setUsedCount(dto.getUsedCount());
        vo.setRemainCount(dto.getRemainCount());
        vo.setStartTime(dto.getStartTime());
        vo.setEndTime(dto.getEndTime());
        vo.setStatus(dto.getStatus());
        vo.setScopeType(dto.getScopeType());
        vo.setCategoryId(dto.getCategoryId());
        vo.setProductId(dto.getProductId());
        vo.setScopeLabel(dto.getScopeLabel());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    /**
     * 将 {@link CouponDTO} 列表转换为 {@link CouponVO} 列表。
     */
    public static List<CouponVO> toVOList(List<CouponDTO> dtoList) {
        if (dtoList == null) {
            return Collections.emptyList();
        }
        return dtoList.stream()
                .map(CouponApiConverter::toVO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // UserCoupon + Coupon + username → UserCouponDTO 转换
    // ============================================================

    /**
     * 将 {@link UserCoupon} + {@link Coupon} + username 转换为 {@link UserCouponDTO}。
     */
    public static UserCouponDTO toUserCouponDTO(UserCoupon entity, Coupon coupon, String username) {
        if (entity == null) {
            return null;
        }
        return UserCouponDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .username(username)
                .couponId(entity.getCouponId())
                .status(entity.getStatus() == null ? null : entity.getStatus().getCode())
                .receiveTime(entity.getReceiveTime())
                .useTime(entity.getUseTime())
                .orderId(entity.getOrderId())
                .createTime(entity.getCreateTime())
                .couponName(coupon == null ? null : coupon.getName())
                .couponType(coupon == null || coupon.getType() == null ? null : coupon.getType().getCode())
                .couponAmount(coupon == null ? null : coupon.getAmount())
                .minAmount(coupon == null ? null : coupon.getMinAmount())
                .couponStartTime(coupon == null ? null : coupon.getStartTime())
                .couponEndTime(coupon == null ? null : coupon.getEndTime())
                .build();
    }

    // ============================================================
    // UserCouponVO → UserCouponDTO 转换（全字段）
    // ============================================================

    /**
     * 将 {@link UserCouponVO} 转换为 {@link UserCouponDTO}。
     */
    public static UserCouponDTO toUserCouponDTOFromVO(UserCouponVO vo) {
        if (vo == null) {
            return null;
        }
        return UserCouponDTO.builder()
                .id(vo.getId())
                .userId(vo.getUserId())
                .username(vo.getUsername())
                .couponId(vo.getCouponId())
                .couponName(vo.getCouponName())
                .couponType(vo.getCouponType())
                .couponAmount(vo.getCouponAmount())
                .minAmount(vo.getMinAmount())
                .couponStartTime(vo.getCouponStartTime())
                .couponEndTime(vo.getCouponEndTime())
                .status(vo.getStatus())
                .receiveTime(vo.getReceiveTime())
                .useTime(vo.getUseTime())
                .orderId(vo.getOrderId())
                .createTime(vo.getCreateTime())
                .build();
    }

    /**
     * 将 {@link UserCouponVO} 列表转换为 {@link UserCouponDTO} 列表。
     */
    public static List<UserCouponDTO> toUserCouponDTOListFromVO(List<UserCouponVO> voList) {
        if (voList == null) {
            return Collections.emptyList();
        }
        return voList.stream()
                .map(CouponApiConverter::toUserCouponDTOFromVO)
                .collect(Collectors.toList());
    }

    /**
     * 将 {@link AdminCouponRecordVO} 转换为 {@link UserCouponDTO}。
     * <p>
     * AdminCouponRecordVO 含 id/userId/username/couponId/couponName/status/receiveTime/useTime/orderId/createTime，
     * 不含 couponType/couponAmount/minAmount/couponStartTime/couponEndTime（这些字段为 null）。
     */
    public static UserCouponDTO toUserCouponDTOFromAdminRecordVO(AdminCouponRecordVO vo) {
        if (vo == null) {
            return null;
        }
        return UserCouponDTO.builder()
                .id(vo.getId())
                .userId(vo.getUserId())
                .username(vo.getUsername())
                .couponId(vo.getCouponId())
                .couponName(vo.getCouponName())
                .status(vo.getStatus())
                .receiveTime(vo.getReceiveTime())
                .useTime(vo.getUseTime())
                .orderId(vo.getOrderId())
                .createTime(vo.getCreateTime())
                .build();
    }

    // ============================================================
    // UserCouponDTO → UserCouponVO 转换（Controller 层前端契约适配）
    // ============================================================

    /**
     * 将 {@link UserCouponDTO} 转换为 {@link UserCouponVO}。
     */
    public static UserCouponVO toUserCouponVO(UserCouponDTO dto) {
        if (dto == null) {
            return null;
        }
        UserCouponVO vo = new UserCouponVO();
        vo.setId(dto.getId());
        vo.setUserId(dto.getUserId());
        vo.setUsername(dto.getUsername());
        vo.setCouponId(dto.getCouponId());
        vo.setStatus(dto.getStatus());
        vo.setReceiveTime(dto.getReceiveTime());
        vo.setUseTime(dto.getUseTime());
        vo.setOrderId(dto.getOrderId());
        vo.setCouponName(dto.getCouponName());
        vo.setCouponType(dto.getCouponType());
        vo.setCouponAmount(dto.getCouponAmount());
        vo.setMinAmount(dto.getMinAmount());
        vo.setCouponStartTime(dto.getCouponStartTime());
        vo.setCouponEndTime(dto.getCouponEndTime());
        vo.setCreateTime(dto.getCreateTime());
        return vo;
    }

    /**
     * 将 {@link UserCouponDTO} 列表转换为 {@link UserCouponVO} 列表。
     */
    public static List<UserCouponVO> toUserCouponVOList(List<UserCouponDTO> dtoList) {
        if (dtoList == null) {
            return Collections.emptyList();
        }
        return dtoList.stream()
                .map(CouponApiConverter::toUserCouponVO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // UserCouponDTO → AdminCouponRecordVO 转换（后台领取记录）
    // ============================================================

    /**
     * 将 {@link UserCouponDTO} 转换为 {@link AdminCouponRecordVO}。
     */
    public static AdminCouponRecordVO toAdminRecordVO(UserCouponDTO dto) {
        if (dto == null) {
            return null;
        }
        AdminCouponRecordVO vo = new AdminCouponRecordVO();
        vo.setId(dto.getId());
        vo.setUserId(dto.getUserId());
        vo.setUsername(dto.getUsername());
        vo.setCouponId(dto.getCouponId());
        vo.setCouponName(dto.getCouponName());
        vo.setStatus(dto.getStatus());
        vo.setReceiveTime(dto.getReceiveTime());
        vo.setUseTime(dto.getUseTime());
        vo.setOrderId(dto.getOrderId());
        vo.setCreateTime(dto.getCreateTime());
        return vo;
    }

    /**
     * 将 {@link UserCouponDTO} 列表转换为 {@link AdminCouponRecordVO} 列表。
     */
    public static List<AdminCouponRecordVO> toAdminRecordVOList(List<UserCouponDTO> dtoList) {
        if (dtoList == null) {
            return Collections.emptyList();
        }
        return dtoList.stream()
                .map(CouponApiConverter::toAdminRecordVO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // PageResult 转换
    // ============================================================

    /**
     * 将 {@code PageResult<CouponVO>} 转换为 {@code PageResult<CouponDTO>}。
     */
    public static PageResult<CouponDTO> toDTOPageResultFromVO(PageResult<CouponVO> pageResult) {
        if (pageResult == null) {
            return PageResult.of(Collections.emptyList(), 0L, 1L, 10L);
        }
        return PageResult.of(
                toDTOListFromVO(pageResult.getList()),
                pageResult.getTotal(),
                pageResult.getPageNum(),
                pageResult.getPageSize());
    }

    /**
     * 将 {@code PageResult<UserCouponVO>} 转换为 {@code PageResult<UserCouponDTO>}。
     */
    public static PageResult<UserCouponDTO> toUserCouponDTOPageResultFromVO(PageResult<UserCouponVO> pageResult) {
        if (pageResult == null) {
            return PageResult.of(Collections.emptyList(), 0L, 1L, 10L);
        }
        return PageResult.of(
                toUserCouponDTOListFromVO(pageResult.getList()),
                pageResult.getTotal(),
                pageResult.getPageNum(),
                pageResult.getPageSize());
    }

    /**
     * 将 {@code PageResult<CouponDTO>} 转换为 {@code PageResult<CouponVO>}。
     */
    public static PageResult<CouponVO> toVOPageResult(PageResult<CouponDTO> pageResult) {
        if (pageResult == null) {
            return PageResult.of(Collections.emptyList(), 0L, 1L, 10L);
        }
        return PageResult.of(
                toVOList(pageResult.getList()),
                pageResult.getTotal(),
                pageResult.getPageNum(),
                pageResult.getPageSize());
    }

    /**
     * 将 {@code PageResult<UserCouponDTO>} 转换为 {@code PageResult<AdminCouponRecordVO>}。
     */
    public static PageResult<AdminCouponRecordVO> toAdminRecordVOPageResult(PageResult<UserCouponDTO> pageResult) {
        if (pageResult == null) {
            return PageResult.of(Collections.emptyList(), 0L, 1L, 10L);
        }
        return PageResult.of(
                toAdminRecordVOList(pageResult.getList()),
                pageResult.getTotal(),
                pageResult.getPageNum(),
                pageResult.getPageSize());
    }

    // ============================================================
    // CouponCreateRequest → Command 转换
    // ============================================================

    /**
     * 将 {@link CouponCreateRequest} 转换为 {@link CreateCouponCommand}。
     */
    public static CreateCouponCommand toCreateCommand(CouponCreateRequest req) {
        if (req == null) {
            return null;
        }
        return CreateCouponCommand.builder()
                .name(req.getName())
                .type(req.getType())
                .amount(req.getAmount())
                .minAmount(req.getMinAmount())
                .totalCount(req.getTotalCount())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .status(req.getStatus())
                .scopeType(req.getScopeType())
                .categoryId(req.getCategoryId())
                .productId(req.getProductId())
                .build();
    }

    /**
     * 将 (id, {@link CouponCreateRequest}) 转换为 {@link UpdateCouponCommand}。
     */
    public static UpdateCouponCommand toUpdateCommand(Long id, CouponCreateRequest req) {
        if (req == null) {
            return null;
        }
        return UpdateCouponCommand.builder()
                .id(id)
                .name(req.getName())
                .type(req.getType())
                .amount(req.getAmount())
                .minAmount(req.getMinAmount())
                .totalCount(req.getTotalCount())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .status(req.getStatus())
                .scopeType(req.getScopeType())
                .categoryId(req.getCategoryId())
                .productId(req.getProductId())
                .build();
    }

    // ============================================================
    // 裸参数 → Command/Query 转换
    // ============================================================

    /**
     * 将裸参数转换为 {@link UpdateCouponStatusCommand}。
     */
    public static UpdateCouponStatusCommand toUpdateStatusCommand(Long id, Integer status) {
        return UpdateCouponStatusCommand.builder()
                .id(id)
                .status(status)
                .build();
    }

    /**
     * 将裸参数转换为 {@link DistributeCouponCommand}。
     */
    public static DistributeCouponCommand toDistributeCommand(Long couponId, Long userId) {
        return DistributeCouponCommand.builder()
                .couponId(couponId)
                .userId(userId)
                .build();
    }

    /**
     * 将裸参数转换为 {@link ClaimCouponCommand}。
     */
    public static ClaimCouponCommand toClaimCommand(Long couponId, Long userId) {
        return ClaimCouponCommand.builder()
                .couponId(couponId)
                .userId(userId)
                .build();
    }

    /**
     * 将裸参数转换为 {@link CouponQuery}。
     */
    public static CouponQuery toCouponQuery(Integer pageNum, Integer pageSize, String name, Integer status) {
        return CouponQuery.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .name(name)
                .status(status)
                .build();
    }

    /**
     * 将裸参数转换为 {@link UserCouponQuery}。
     */
    public static UserCouponQuery toUserCouponQuery(Long userId, String status) {
        return UserCouponQuery.builder()
                .userId(userId)
                .status(status)
                .build();
    }

    /**
     * 将裸参数转换为 {@link UseCouponCommand}（计价场景）。
     */
    public static UseCouponCommand toUseCouponCommand(Long userCouponId, Long userId,
                                                       BigDecimal orderAmount, List<Long> productIds) {
        return UseCouponCommand.builder()
                .userCouponId(userCouponId)
                .userId(userId)
                .orderAmount(orderAmount)
                .productIds(productIds)
                .build();
    }

    /**
     * 将裸参数转换为 {@link UseCouponCommand}（核销场景）。
     */
    public static UseCouponCommand toUseCouponCommandForUse(Long userCouponId, Long userId, Long orderId) {
        return UseCouponCommand.builder()
                .userCouponId(userCouponId)
                .userId(userId)
                .orderId(orderId)
                .build();
    }

    /**
     * 将裸参数转换为 {@link RevertCouponCommand}。
     */
    public static RevertCouponCommand toRevertCouponCommand(Long userCouponId, Long userId) {
        return RevertCouponCommand.builder()
                .userCouponId(userCouponId)
                .userId(userId)
                .build();
    }
}