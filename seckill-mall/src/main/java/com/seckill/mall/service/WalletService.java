package com.seckill.mall.service;

import com.seckill.mall.vo.WalletRecordVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 钱包服务接口
 * <p>
 * M-D2 修复：将 {@code WalletController} 中对 Mapper 的直接调用下沉到 Service，
 * Controller 仅负责编排并返回 {@code Result<VO>}。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：WalletService.java
 * 邮箱：nj651217@163.com
 */
public interface WalletService {

    /**
     * 查询指定用户余额
     *
     * @param userId 用户 ID
     * @return 余额（用户不存在或余额为空时返回 0）
     */
    BigDecimal getBalance(Long userId);

    /**
     * 查询指定用户的交易记录（当前以充值卡使用记录作为钱包交易记录）
     *
     * @param userId 用户 ID
     * @return 交易记录列表，按交易时间倒序
     */
    List<WalletRecordVO> listRecords(Long userId);
}