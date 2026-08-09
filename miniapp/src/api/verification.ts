/**
 * 验证码 API（对齐 spec.md 2.6 验证码端点）
 * /api/v1/verification
 */

import { post } from '@/utils/request'
import type { SendVerificationRequest, VerifyCodeRequest } from '@/types'

/** 发送验证码（短信/邮件） */
export function sendVerificationCode(data: SendVerificationRequest): Promise<any> {
  return post('/verification/send', data, { skipAuth: true })
}

/** 校验验证码 */
export function verifyCode(data: VerifyCodeRequest): Promise<boolean> {
  return post<boolean>('/verification/check', data, { skipAuth: true })
}