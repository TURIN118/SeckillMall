/**
 * Stats 模块根包 - 后台数据看台统计聚合模块。
 *
 * <p>Phase ST 模块化迁移目标结构，参见 STATS-MIGRATION-PLAN.md。
 *
 * <p>Stats 是统计聚合模块，无独立 Entity/Mapper，所有数据均通过跨模块 API 实时查询聚合：
 * identity（UserApi）、seckill（SeckillOrderApi/SeckillGoodsApi）、product（ProductApi）。
 *
 * @author wnj
 * @since Phase ST.0
 */
package com.seckill.mall.stats;