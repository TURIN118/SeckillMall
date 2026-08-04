/**
 * 钱包 API
 * 查余额、充值、交易记录 (均需登录)
 */
import { get, post } from './request'
import type {
    Result,
    WalletRecordVO,
    WalletRechargeRequest
} from '@/types'

/**
 * 查询钱包余额
 * 后端 WalletController.balance() 返回 Result<BigDecimal>，
 * 即 res.data 直接是数字 (如 100.00)，不是 { balance: number } 对象。
 */
export function getWalletBalance(): Promise<Result<number>> {
    return get<number>('/api/v1/wallet/balance')
}

/** 钱包充值 (使用充值卡卡号 + 卡密) */
export function rechargeWallet(data: WalletRechargeRequest): Promise<Result<void>> {
    return post<void>('/api/v1/wallet/recharge', data)
}

/** 查询钱包交易记录 */
export function getWalletRecords(): Promise<Result<WalletRecordVO[]>> {
    return get<WalletRecordVO[]>('/api/v1/wallet/records')
}
