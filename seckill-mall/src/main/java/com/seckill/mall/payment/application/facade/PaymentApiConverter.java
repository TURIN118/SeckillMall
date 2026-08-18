package com.seckill.mall.payment.application.facade;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.payment.api.command.DisableRechargeCardCommand;
import com.seckill.mall.payment.api.command.GenerateRechargeCardCommand;
import com.seckill.mall.payment.api.command.PayCommand;
import com.seckill.mall.payment.api.command.RechargeCommand;
import com.seckill.mall.payment.api.dto.RechargeCardDTO;
import com.seckill.mall.payment.api.dto.RechargeCardGenerateDTO;
import com.seckill.mall.payment.api.dto.WalletRecordDTO;
import com.seckill.mall.payment.api.query.RechargeCardQuery;
import com.seckill.mall.payment.api.result.RechargeResult;
import com.seckill.mall.payment.infrastructure.entity.RechargeCard;
import com.seckill.mall.payment.interfaces.vo.RechargeCardGenerateVO;
import com.seckill.mall.payment.interfaces.vo.RechargeCardVO;
import com.seckill.mall.payment.interfaces.vo.WalletRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Payment API 转换辅助类。
 *
 * <p>集中存放旧 VO/Entity 与新 API 层 DTO/Command 之间的转换方法，
 * 供 PaymentApplicationService / WalletApplicationService / RechargeCardApplicationService / Controller 调用。
 * 所有方法均为无状态静态方法，标注 {@code @Component} 仅为便于未来扩展为 Bean 注入方式。
 *
 * <p>转换原则：
 * <ul>
 *     <li>Entity → DTO：枚举字段取 getCode()，丢弃 isDeleted/cardPassword 等基础设施/敏感字段</li>
 *     <li>VO → DTO：全字段映射（核心 + 展示），保留前端展示信息</li>
 *     <li>DTO → VO：全字段映射（核心 + 展示），保留前端契约</li>
 *     <li>Request → Command：字段一一对应，丢弃 Bean Validation 注解</li>
 *     <li>BigDecimal → Result：RechargeResult 仅含 newBalance 字段，包装裸 BigDecimal</li>
 * </ul>
 *
 * @author wnj
 * @since Phase PM.4
 */
@Slf4j
@Component
public class PaymentApiConverter {

    // ============================================================
    // 裸参数 → Command 转换
    // ============================================================

    /** 裸参数 → PayCommand */
    public static PayCommand toPayCommand(Long userId, BigDecimal amount, String payMethod) {
        return PayCommand.builder()
                .userId(userId)
                .amount(amount)
                .payMethod(payMethod)
                .build();
    }

    /** 裸参数 → RechargeCommand */
    public static RechargeCommand toRechargeCommand(String cardNo, String cardPassword, Long userId) {
        return RechargeCommand.builder()
                .cardNo(cardNo)
                .cardPassword(cardPassword)
                .userId(userId)
                .build();
    }

    /** 裸参数 → GenerateRechargeCardCommand */
    public static GenerateRechargeCardCommand toGenerateCommand(BigDecimal faceValue, Integer count) {
        return GenerateRechargeCardCommand.builder()
                .faceValue(faceValue)
                .count(count)
                .build();
    }

    /** 裸参数 → DisableRechargeCardCommand */
    public static DisableRechargeCardCommand toDisableCommand(Long id) {
        return DisableRechargeCardCommand.builder()
                .id(id)
                .build();
    }

    /** 裸参数 → RechargeCardQuery */
    public static RechargeCardQuery toRechargeCardQuery(Integer pageNum, Integer pageSize,
                                                        String batchNo, String status) {
        return RechargeCardQuery.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .batchNo(batchNo)
                .status(status)
                .build();
    }

    /** BigDecimal → RechargeResult */
    public static RechargeResult toRechargeResult(BigDecimal newBalance) {
        return RechargeResult.builder()
                .newBalance(newBalance)
                .build();
    }

    // ============================================================
    // WalletRecordVO ↔ WalletRecordDTO 转换
    // ============================================================

    /** WalletRecordVO → WalletRecordDTO */
    public static WalletRecordDTO toDTO(WalletRecordVO vo) {
        if (vo == null) {
            return null;
        }
        return WalletRecordDTO.builder()
                .id(vo.getId())
                .type(vo.getType())
                .amount(vo.getAmount())
                .balanceAfter(vo.getBalanceAfter())
                .createTime(vo.getCreateTime())
                .remark(vo.getRemark())
                .build();
    }

    /** WalletRecordVO 列表 → WalletRecordDTO 列表 */
    public static List<WalletRecordDTO> toDTOList(List<WalletRecordVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return Collections.emptyList();
        }
        return voList.stream().map(PaymentApiConverter::toDTO).collect(Collectors.toList());
    }

    /** WalletRecordDTO → WalletRecordVO（Controller 层前端契约适配） */
    public static WalletRecordVO toVO(WalletRecordDTO dto) {
        if (dto == null) {
            return null;
        }
        WalletRecordVO vo = new WalletRecordVO();
        vo.setId(dto.getId());
        vo.setType(dto.getType());
        vo.setAmount(dto.getAmount());
        vo.setBalanceAfter(dto.getBalanceAfter());
        vo.setCreateTime(dto.getCreateTime());
        vo.setRemark(dto.getRemark());
        return vo;
    }

    /** WalletRecordDTO 列表 → WalletRecordVO 列表 */
    public static List<WalletRecordVO> toVOList(List<WalletRecordDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return Collections.emptyList();
        }
        return dtoList.stream().map(PaymentApiConverter::toVO).collect(Collectors.toList());
    }

    // ============================================================
    // RechargeCard Entity → RechargeCardDTO 转换
    // ============================================================

    /** RechargeCard Entity → RechargeCardDTO */
    public static RechargeCardDTO toRechargeCardDTO(RechargeCard entity) {
        if (entity == null) {
            return null;
        }
        return RechargeCardDTO.builder()
                .id(entity.getId())
                .cardNo(entity.getCardNo())
                .faceValue(entity.getFaceValue())
                .status(entity.getStatus() == null ? null : entity.getStatus().getCode())
                .usedBy(entity.getUsedBy())
                .usedTime(entity.getUsedTime())
                .batchNo(entity.getBatchNo())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    /** RechargeCard Entity 列表 → RechargeCardDTO 列表 */
    public static List<RechargeCardDTO> toRechargeCardDTOListFromEntity(List<RechargeCard> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(PaymentApiConverter::toRechargeCardDTO).collect(Collectors.toList());
    }

    // ============================================================
    // RechargeCardVO ↔ RechargeCardDTO 转换
    // ============================================================

    /** RechargeCardVO → RechargeCardDTO */
    public static RechargeCardDTO toRechargeCardDTO(RechargeCardVO vo) {
        if (vo == null) {
            return null;
        }
        return RechargeCardDTO.builder()
                .id(vo.getId())
                .cardNo(vo.getCardNo())
                .faceValue(vo.getFaceValue())
                .status(vo.getStatus())
                .usedBy(vo.getUsedBy())
                .usedTime(vo.getUsedTime())
                .batchNo(vo.getBatchNo())
                .createTime(vo.getCreateTime())
                .updateTime(vo.getUpdateTime())
                .build();
    }

    /** RechargeCardVO 列表 → RechargeCardDTO 列表 */
    public static List<RechargeCardDTO> toRechargeCardDTOList(List<RechargeCardVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return Collections.emptyList();
        }
        return voList.stream().map(PaymentApiConverter::toRechargeCardDTO).collect(Collectors.toList());
    }

    /** RechargeCardDTO → RechargeCardVO */
    public static RechargeCardVO toRechargeCardVO(RechargeCardDTO dto) {
        if (dto == null) {
            return null;
        }
        RechargeCardVO vo = new RechargeCardVO();
        vo.setId(dto.getId());
        vo.setCardNo(dto.getCardNo());
        // cardPassword 不设置（脱敏）
        vo.setFaceValue(dto.getFaceValue());
        vo.setStatus(dto.getStatus());
        vo.setUsedBy(dto.getUsedBy());
        vo.setUsedTime(dto.getUsedTime());
        vo.setBatchNo(dto.getBatchNo());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    /** RechargeCardDTO 列表 → RechargeCardVO 列表 */
    public static List<RechargeCardVO> toRechargeCardVOList(List<RechargeCardDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return Collections.emptyList();
        }
        return dtoList.stream().map(PaymentApiConverter::toRechargeCardVO).collect(Collectors.toList());
    }

    /** PageResult<RechargeCardVO> → PageResult<RechargeCardDTO> */
    public static PageResult<RechargeCardDTO> toRechargeCardDTOPageResult(PageResult<RechargeCardVO> page) {
        if (page == null) {
            return null;
        }
        return PageResult.of(
                toRechargeCardDTOList(page.getList()),
                page.getTotal(),
                page.getPageNum(),
                page.getPageSize());
    }

    /** PageResult<RechargeCardDTO> → PageResult<RechargeCardVO> */
    public static PageResult<RechargeCardVO> toRechargeCardVOPageResult(PageResult<RechargeCardDTO> page) {
        if (page == null) {
            return null;
        }
        return PageResult.of(
                toRechargeCardVOList(page.getList()),
                page.getTotal(),
                page.getPageNum(),
                page.getPageSize());
    }

    // ============================================================
    // RechargeCardGenerateVO ↔ RechargeCardGenerateDTO 转换
    // ============================================================

    /** RechargeCardGenerateVO → RechargeCardGenerateDTO */
    public static RechargeCardGenerateDTO toGenerateDTO(RechargeCardGenerateVO vo) {
        if (vo == null) {
            return null;
        }
        return RechargeCardGenerateDTO.builder()
                .id(vo.getId())
                .cardNo(vo.getCardNo())
                .cardPassword(vo.getCardPassword())
                .faceValue(vo.getFaceValue())
                .status(vo.getStatus())
                .batchNo(vo.getBatchNo())
                .createTime(vo.getCreateTime())
                .build();
    }

    /** RechargeCardGenerateVO 列表 → RechargeCardGenerateDTO 列表 */
    public static List<RechargeCardGenerateDTO> toGenerateDTOList(List<RechargeCardGenerateVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return Collections.emptyList();
        }
        return voList.stream().map(PaymentApiConverter::toGenerateDTO).collect(Collectors.toList());
    }

    /** RechargeCardGenerateDTO → RechargeCardGenerateVO */
    public static RechargeCardGenerateVO toGenerateVO(RechargeCardGenerateDTO dto) {
        if (dto == null) {
            return null;
        }
        RechargeCardGenerateVO vo = new RechargeCardGenerateVO();
        vo.setId(dto.getId());
        vo.setCardNo(dto.getCardNo());
        vo.setCardPassword(dto.getCardPassword());
        vo.setFaceValue(dto.getFaceValue());
        vo.setStatus(dto.getStatus());
        vo.setBatchNo(dto.getBatchNo());
        vo.setCreateTime(dto.getCreateTime());
        return vo;
    }

    /** RechargeCardGenerateDTO 列表 → RechargeCardGenerateVO 列表 */
    public static List<RechargeCardGenerateVO> toGenerateVOList(List<RechargeCardGenerateDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return Collections.emptyList();
        }
        return dtoList.stream().map(PaymentApiConverter::toGenerateVO).collect(Collectors.toList());
    }
}