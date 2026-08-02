/**
 * 钱包 API
 * 查余额、充值、交易记录 (均需登录)
 */
import { get, post } from './request'
import type {
    Result,
    WalletBalanceVO,
    WalletRecordVO,
    WalletRechargeRequest
} from '@/types'

/** 查询钱包余额 */
export function getWalletBalance(): Promise<Result<WalletBalanceVO>> {
    return get<WalletBalanceVO>('/api/v1/wallet/balance')
}

/** 钱包充值 (使用充值卡卡号 + 卡密) */
export function rechargeWallet(data: WalletRechargeRequest): Promise<Result<void>> {
    return post<void>('/api/v1/wallet/recharge', data)
}

/** 查询钱包交易记录 */
export function getWalletRecords(): Promise<Result<WalletRecordVO[]>> {
    return get<WalletRecordVO[]>('/api/v1/wallet/records')
}