/**
 * System 模块根包 - 后台系统运维聚合模块。
 *
 * <p>Phase SY 模块化迁移目标结构，参见 SYSTEM-MIGRATION-PLAN.md。
 *
 * <p>System 是后台运维聚合模块，包含两部分：
 * 1. 操作日志管理（依赖独立 Entity + Mapper，本模块持久化资产）
 * 2. 运维聚合查询（依赖跨模块 API：seckill.SeckillOrderApi + 内部健康监控 SystemHealthMonitor）
 *
 * @author wnj
 * @since Phase SY.0
 */
package com.seckill.mall.system;