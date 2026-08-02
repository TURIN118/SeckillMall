/**
 * 数据看台统计 API - 严格匹配后端 StatsController
 *
 * 说明：本文件内的 StatsOverviewVO / TrendItemVO / SeckillRankingVO 是数据看台专用 VO，
 * 故意不修改 types/index.ts，避免影响其他引用。
 */
import { get } from './request'
import type { Result } from '@/types'

/** 总览统计 */
export interface StatsOverviewVO {
    userCount: number
    orderCount: number
    seckillCount: number
    totalSales: number
    productCount: number
    todayOrderCount: number
    todayUserCount: number
}

/** 趋势数据项 */
export interface TrendItemVO {
    date: string
    count: number
}

/** 秒杀排行榜项 */
export interface SeckillRankingVO {
    seckillId: number
    productName: string
    seckillPrice: number
    salesCount: number
    totalAmount: number
}

/** 总览统计 */
export function getStatsOverview(): Promise<Result<StatsOverviewVO>> {
    return get<StatsOverviewVO>('/api/v1/admin/stats/overview')
}

/** 用户注册趋势 (近 N 天每日注册数) */
export function getUserTrend(days: number): Promise<Result<TrendItemVO[]>> {
    return get<TrendItemVO[]>('/api/v1/admin/stats/user-trend', { days })
}

/** 订单趋势 (近 N 天每日订单数) */
export function getOrderTrend(days: number): Promise<Result<TrendItemVO[]>> {
    return get<TrendItemVO[]>('/api/v1/admin/stats/order-trend', { days })
}

/** 秒杀排行榜 Top N */
export function getSeckillRanking(limit: number): Promise<Result<SeckillRankingVO[]>> {
    return get<SeckillRankingVO[]>('/api/v1/admin/stats/seckill-ranking', { limit })
}