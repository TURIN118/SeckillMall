package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.entity.RechargeCard;
import com.seckill.mall.entity.User;
import com.seckill.mall.entity.enums.RechargeCardStatus;
import com.seckill.mall.mapper.RechargeCardMapper;
import com.seckill.mall.mapper.UserMapper;
import com.seckill.mall.service.RechargeCardService;
import com.seckill.mall.vo.RechargeCardGenerateVO;
import com.seckill.mall.vo.RechargeCardVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 充值卡服务实现
 * <p>
 * 批量生成：使用 {@link SecureRandom} 生成卡号与卡密，卡密通过
 * {@link PasswordEncoder}（BCrypt）加密后入库；明文仅一次性返回给调用方。
 * 充值：校验卡号存在、卡密匹配、状态为 UNUSED → 原子更新卡状态为 USED
 * 并将面额累加到 {@code t_user.balance}。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RechargeCardServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RechargeCardServiceImpl implements RechargeCardService {

    /** 卡号长度 */
    private static final int CARD_NO_LENGTH = 20;
    /** 卡密长度（明文） */
    private static final int CARD_PWD_LENGTH = 12;
    /** 批次号时间格式 */
    private static final DateTimeFormatter BATCH_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RechargeCardMapper rechargeCardMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RechargeCardGenerateVO> generate(BigDecimal faceValue, Integer count) {
        if (faceValue == null || faceValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "面额必须大于0");
        }
        if (count == null || count <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数量必须大于0");
        }
        String batchNo = "B" + LocalDateTime.now().format(BATCH_FMT);
        List<RechargeCardGenerateVO> result = new ArrayList<>(count);
        // L18: 当前逐条 insert，可优化为批量插入（rechargeCardMapper.insertBatchSomeColumn 或 MyBatis-Plus saveBatch）
        // 批量生成场景 count 可能较大，逐条 insert 性能较差；当前实现优先保证正确性，后续可优化
        for (int i = 0; i < count; i++) {
            String cardNo = generateCardNo();
            String plainPwd = generateCardPassword();
            RechargeCard card = new RechargeCard();
            card.setCardNo(cardNo);
            card.setCardPassword(passwordEncoder.encode(plainPwd));
            card.setFaceValue(faceValue);
            card.setStatus(RechargeCardStatus.UNUSED);
            card.setBatchNo(batchNo);
            rechargeCardMapper.insert(card);

            // 返回 VO（含明文卡密，仅此一次返回）
            RechargeCardGenerateVO vo = new RechargeCardGenerateVO();
            vo.setId(card.getId());
            vo.setCardNo(cardNo);
            vo.setCardPassword(plainPwd);
            vo.setFaceValue(faceValue);
            vo.setStatus(RechargeCardStatus.UNUSED.getCode());
            vo.setBatchNo(batchNo);
            vo.setCreateTime(card.getCreateTime());
            result.add(vo);
        }
        log.info("批量生成充值卡成功，batchNo={}, count={}, faceValue={}", batchNo, count, faceValue);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal recharge(String cardNo, String cardPassword, Long userId) {
        if (cardNo == null || cardNo.isBlank() || cardPassword == null || cardPassword.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "卡号或卡密不能为空");
        }
        // 查询充值卡
        RechargeCard card = rechargeCardMapper.selectOne(
                new LambdaQueryWrapper<RechargeCard>().eq(RechargeCard::getCardNo, cardNo));
        if (card == null) {
            throw new BusinessException(ErrorCode.RECHARGE_CARD_NOT_FOUND);
        }
        // 校验状态
        if (card.getStatus() == RechargeCardStatus.USED) {
            throw new BusinessException(ErrorCode.RECHARGE_CARD_USED);
        }
        if (card.getStatus() == RechargeCardStatus.DISABLED) {
            throw new BusinessException(ErrorCode.RECHARGE_CARD_DISABLED);
        }
        // 校验卡密
        if (!passwordEncoder.matches(cardPassword, card.getCardPassword())) {
            throw new BusinessException(ErrorCode.RECHARGE_CARD_PASSWORD_ERROR);
        }
        // 原子更新卡状态为 USED（乐观锁：条件 status=UNUSED）
        LambdaUpdateWrapper<RechargeCard> cardUpdate = new LambdaUpdateWrapper<RechargeCard>()
                .eq(RechargeCard::getId, card.getId())
                .eq(RechargeCard::getStatus, RechargeCardStatus.UNUSED)
                .set(RechargeCard::getStatus, RechargeCardStatus.USED)
                .set(RechargeCard::getUsedBy, userId)
                .set(RechargeCard::getUsedTime, LocalDateTime.now());
        int updated = rechargeCardMapper.update(null, cardUpdate);
        if (updated == 0) {
            // 并发场景：卡已被他人使用
            throw new BusinessException(ErrorCode.RECHARGE_CARD_USED);
        }
        // 用户余额 += 面额（使用 setSql 避免覆盖更新）
        LambdaUpdateWrapper<User> userUpdate = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("balance = balance + " + card.getFaceValue().toPlainString());
        userMapper.update(null, userUpdate);
        // 查询最新余额
        User user = userMapper.selectById(userId);
        BigDecimal newBalance = user == null ? BigDecimal.ZERO : user.getBalance();
        log.info("充值成功，cardNo={}, userId={}, faceValue={}, newBalance={}",
                cardNo, userId, card.getFaceValue(), newBalance);
        return newBalance;
    }

    @Override
    public PageResult<RechargeCardVO> listPage(Integer pageNum, Integer pageSize, String batchNo, String status) {
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 ? 10 : pageSize;
        LambdaQueryWrapper<RechargeCard> wrapper = new LambdaQueryWrapper<RechargeCard>()
                .orderByDesc(RechargeCard::getCreateTime);
        if (batchNo != null && !batchNo.isBlank()) {
            wrapper.eq(RechargeCard::getBatchNo, batchNo);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(RechargeCard::getStatus, RechargeCardStatus.fromCode(status));
        }
        IPage<RechargeCard> page = rechargeCardMapper.selectPage(new Page<>(pn, ps), wrapper);
        List<RechargeCardVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        RechargeCard card = rechargeCardMapper.selectById(id);
        if (card == null) {
            throw new BusinessException(ErrorCode.RECHARGE_CARD_NOT_FOUND);
        }
        if (card.getStatus() != RechargeCardStatus.UNUSED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅未使用的充值卡可禁用");
        }
        LambdaUpdateWrapper<RechargeCard> wrapper = new LambdaUpdateWrapper<RechargeCard>()
                .eq(RechargeCard::getId, id)
                .set(RechargeCard::getStatus, RechargeCardStatus.DISABLED);
        rechargeCardMapper.update(null, wrapper);
        log.info("禁用充值卡成功，id={}, cardNo={}", id, card.getCardNo());
    }

    @Override
    public List<RechargeCard> getUsedCardsByUser(Long userId) {
        return rechargeCardMapper.selectList(new LambdaQueryWrapper<RechargeCard>()
                .eq(RechargeCard::getUsedBy, userId)
                .eq(RechargeCard::getStatus, RechargeCardStatus.USED));
    }

    // ==================== 私有方法 ====================

    /**
     * 生成唯一卡号：20位数字，带时间戳前缀
     */
    private String generateCardNo() {
        // 重试机制避免极小概率冲突
        for (int retry = 0; retry < 5; retry++) {
            String cardNo = buildRandomDigits(CARD_NO_LENGTH);
            Long exist = rechargeCardMapper.selectCount(
                    new LambdaQueryWrapper<RechargeCard>().eq(RechargeCard::getCardNo, cardNo));
            if (exist == null || exist == 0) {
                return cardNo;
            }
        }
        // 兜底：追加时间戳后缀
        return buildRandomDigits(CARD_NO_LENGTH - 13) + System.currentTimeMillis();
    }

    /**
     * 生成卡密明文：12位字母数字
     */
    private String generateCardPassword() {
        return buildRandomAlphanumeric(CARD_PWD_LENGTH);
    }

    /**
     * 生成指定长度的随机数字串
     */
    private String buildRandomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 生成指定长度的随机字母数字串
     */
    private String buildRandomAlphanumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /** Entity → VO（不含卡密） */
    private RechargeCardVO toVO(RechargeCard card) {
        RechargeCardVO vo = new RechargeCardVO();
        vo.setId(card.getId());
        vo.setCardNo(card.getCardNo());
        vo.setFaceValue(card.getFaceValue());
        vo.setStatus(card.getStatus() == null ? null : card.getStatus().getCode());
        vo.setUsedBy(card.getUsedBy());
        vo.setUsedTime(card.getUsedTime());
        vo.setBatchNo(card.getBatchNo());
        vo.setCreateTime(card.getCreateTime());
        vo.setUpdateTime(card.getUpdateTime());
        return vo;
    }
}