/**
 * 验证码 API
 * 邮箱/短信验证码发送与校验 (发送接口可匿名访问)
 */
import { post } from './request'
import type { Result, VerificationSendRequest, VerificationCheckRequest } from '@/types'

/** 发送邮箱验证码 */
export function sendEmailCode(data: VerificationSendRequest): Promise<Result<void>> {
    return post<void>('/api/v1/verification/send-email', data)
}

/** 发送短信验证码 */
export function sendSmsCode(data: VerificationSendRequest): Promise<Result<void>> {
    return post<void>('/api/v1/verification/send-sms', data)
}
