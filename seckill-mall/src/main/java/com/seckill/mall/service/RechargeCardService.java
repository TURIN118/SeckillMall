package com.seckill.mall.service;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.vo.RechargeCardGenerateVO;
import com.seckill.mall.vo.RechargeCardVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 充值卡服务接口
 * <p>
 * 提供批量生成、充值、后台查询/禁用能力。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RechargeCardService.java
 * 邮箱：nj651217@163.com
 */
public interface RechargeCardService {

    /**
     * 批量生成充值卡
     *
     * @param faceValue 面额
     * @param count     数量
     * @return 生成的充值卡列表（含卡号卡密明文，仅此一次返回）
     */
    List<RechargeCardGenerateVO> generate(BigDecimal faceValue, Integer count);

    /**
     * 充值：校验卡号卡密 → 更新用户余额与卡状态
     *
     * @param cardNo       卡号
     * @param cardPassword 卡密（明文）
     * @param userId       用户ID
     * @return 充值后的最新余额
     */
    BigDecimal recharge(String cardNo, String cardPassword, Long userId);

    /**
     * 后台分页查询充值卡列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param batchNo  批次号筛选（可空）
     * @param status   状态筛选（可空）
     * @return 分页结果
     */
    PageResult<RechargeCardVO> listPage(Integer pageNum, Integer pageSize, String batchNo, String status);

    /**
     * 禁用充值卡
     *
     * @param id 充值卡ID
     */
    void disable(Long id);
}