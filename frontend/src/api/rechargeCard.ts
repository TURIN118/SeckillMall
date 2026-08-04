/**
 * 充值卡 API (后台 ADMIN)
 * 批量生成、列表查询、禁用
 */
import { get, post, put } from './request'
import type {
    Result,
    PageResult,
    RechargeCardVO,
    RechargeCardGenerateRequest
} from '@/types'

/** 后台分页查询充值卡列表 (可按批次号筛选) */
export function adminGetRechargeCardList(
    pageNum = 1,
    pageSize = 10,
    batchNo?: string
): Promise<Result<PageResult<RechargeCardVO>>> {
    const params: Record<string, unknown> = { pageNum, pageSize }
    if (batchNo) params.batchNo = batchNo
    return get<PageResult<RechargeCardVO>>('/api/v1/admin/recharge-cards/list', params)
}

/** 后台批量生成充值卡 */
export function adminGenerateRechargeCards(
    data: RechargeCardGenerateRequest
): Promise<Result<RechargeCardVO[]>> {
    return post<RechargeCardVO[]>('/api/v1/admin/recharge-cards/generate', data)
}

/** 后台禁用充值卡 (仅未使用的卡可禁用) */
export function adminDisableRechargeCard(id: number | string): Promise<Result<void>> {
    return put<void>(`/api/v1/admin/recharge-cards/${id}/disable`)
}