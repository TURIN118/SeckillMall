/**
 * 钱包 API（对齐 spec.md 2.6 钱包端点）
 * /api/v1/wallet
 */

import { get } from '@/utils/request'
import type { WalletVO } from '@/types'

/** 钱包余额 */
export function getWallet(): Promise<WalletVO> {
  return get<WalletVO>('/wallet')
}

/** 钱包流水 */
export function getWalletTransactions(params?: { page?: number; pageSize?: number }): Promise<any> {
  return get('/wallet/transactions', params)
}