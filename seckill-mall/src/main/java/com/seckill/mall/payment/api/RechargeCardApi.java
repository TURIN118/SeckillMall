package com.seckill.mall.payment.api;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.payment.api.command.DisableRechargeCardCommand;
import com.seckill.mall.payment.api.command.GenerateRechargeCardCommand;
import com.seckill.mall.payment.api.command.RechargeCommand;
import com.seckill.mall.payment.api.dto.RechargeCardDTO;
import com.seckill.mall.payment.api.dto.RechargeCardGenerateDTO;
import com.seckill.mall.payment.api.query.RechargeCardQuery;
import com.seckill.mall.payment.api.result.RechargeResult;

import java.util.List;

/**
 * Payment 模块充值卡 API。
 *
 * <p>对外暴露充值卡能力（批量生成 / 充值 / 后台查询 / 禁用 / 查询已使用），
 * 供 payment 模块 Controller 和 WalletService 调用。
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
 * <p>原方法映射参见 PAYMENT-API-CONTRACT.md 第 9.3 节。
 *
 * @author wnj
 * @since Phase PM.2
 */
public interface RechargeCardApi {

    /**
     * 批量生成充值卡。
     *
     * <p>使用 SecureRandom 生成卡号与卡密，卡密通过 BCrypt 加密后入库；明文仅一次性返回给调用方。
     *
     * @param command 生成命令（faceValue + count）
     * @return 生成的充值卡列表（含卡号卡密明文，仅此一次返回）
     */
    List<RechargeCardGenerateDTO> generate(GenerateRechargeCardCommand command);

    /**
     * 充值：校验卡号卡密 → 更新用户余额与卡状态。
     *
     * @param command 充值命令（cardNo + cardPassword + userId）
     * @return 充值结果（含 newBalance）
     * @throws com.seckill.mall.exception.BusinessException 卡不存在/已使用/已禁用/卡密错误时抛出
     */
    RechargeResult recharge(RechargeCommand command);

    /**
     * 后台分页查询充值卡列表。
     *
     * @param query 查询条件（pageNum + pageSize + batchNo + status）
     * @return 分页结果（DTO，不含卡密）
     */
    PageResult<RechargeCardDTO> listPage(RechargeCardQuery query);

    /**
     * 禁用充值卡（仅未使用的卡可禁用）。
     *
     * @param command 禁用命令（id）
     * @throws com.seckill.mall.exception.BusinessException 卡不存在或状态非 UNUSED 时抛出
     */
    void disable(DisableRechargeCardCommand command);

    /**
     * 查询用户已使用的充值卡（status=USED）。
     *
     * <p>仅供 WalletApplicationService 调用，用于展示钱包充值记录。
     *
     * @param userId 用户 ID
     * @return 用户已使用的充值卡列表（DTO，不含卡密）
     */
    List<RechargeCardDTO> getUsedCardsByUser(Long userId);
}