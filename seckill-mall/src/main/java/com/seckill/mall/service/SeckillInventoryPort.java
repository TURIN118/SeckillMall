package com.seckill.mall.service;

/**
 * 秒杀库存端口：统一秒杀库存的回补操作（DB available_count + Redis 预减库存）。
 * <p>
 * 业务代码依赖此接口而非直接调用 SeckillGoodsMapper / SeckillLuaService，
 * 实现秒杀库存操作与业务逻辑的解耦。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillInventoryPort.java
 * 邮箱：nj651217@163.com
 */
public interface SeckillInventoryPort {

    /**
     * 统一回补秒杀库存（DB available_count + Redis 预减库存）。
     * <p>
     * DB 回补失败仅记录日志，由补偿任务兜底；
     * Redis 回补失败仅记录日志，不影响主流程。
     *
     * @param seckillId 秒杀活动 ID
     * @param userId    用户 ID
     */
    void rollback(Long seckillId, Long userId);
}