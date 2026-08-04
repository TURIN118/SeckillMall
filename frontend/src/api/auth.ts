/**
 * 认证 API - 严格匹配 default.md
 */
import { get, post, put } from './request'
import type {
  Result,
  UserVO,
  LoginVO,
  TokenVO,
  CaptchaVO,
  LoginRequest,
  RegisterRequest,
  RefreshTokenRequest,
  ChangePasswordRequest,
  UpdatePhoneRequest,
  UpdateEmailRequest
} from '@/types'

/** 用户登录 */
export function login(data: LoginRequest): Promise<Result<LoginVO>> {
  return post<LoginVO>('/api/v1/auth/login', data)
}

/** 用户注册 */
export function register(data: RegisterRequest): Promise<Result<UserVO>> {
  return post<UserVO>('/api/v1/auth/register', data)
}

/** 获取图形验证码 */
export function getCaptcha(): Promise<Result<CaptchaVO>> {
  return get<CaptchaVO>('/api/v1/auth/captcha')
}

/** 刷新令牌 */
export function refreshToken(data: RefreshTokenRequest): Promise<Result<TokenVO>> {
  return post<TokenVO>('/api/v1/auth/refresh', data)
}

/** 获取当前登录用户信息 */
export function getMe(): Promise<Result<UserVO>> {
  return get<UserVO>('/api/v1/auth/me')
}

/** 修改密码 */
export function changePassword(data: ChangePasswordRequest): Promise<Result<void>> {
  return put<void>('/api/v1/auth/password', data)
}

/** 个人信息更新请求 (字段均可选, 仅传需要更新的字段) */
export interface ProfileUpdateRequest {
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
}

/** 更新当前用户个人信息 */
export function updateProfile(data: ProfileUpdateRequest): Promise<Result<UserVO>> {
  return put<UserVO>('/api/v1/auth/profile', data)
}

/** 退出登录 */
export function logout(): Promise<Result<void>> {
  return post<void>('/api/v1/auth/logout')
}

/** 修改手机号 (需短信验证码校验) */
export function updatePhone(data: UpdatePhoneRequest): Promise<Result<UserVO>> {
  return put<UserVO>('/api/v1/users/profile/phone', data)
}

/** 修改邮箱 (需邮箱验证码校验) */
export function updateEmail(data: UpdateEmailRequest): Promise<Result<UserVO>> {
  return put<UserVO>('/api/v1/users/profile/email', data)
}

/** 找回密码-发送验证码请求参数 */
export interface ForgotPasswordSendCodeRequest {
  account: string
  type: 'PHONE' | 'EMAIL'
}

/** 找回密码-重置密码请求参数 */
export interface ForgotPasswordResetRequest {
  account: string
  type: 'PHONE' | 'EMAIL'
  code: string
  newPassword: string
}

/** 找回密码-发送验证码（手机短信或邮箱） */
export function sendForgotPasswordCode(data: ForgotPasswordSendCodeRequest): Promise<Result<void>> {
  return post<void>('/api/v1/auth/forgot-password/send-code', data)
}

/** 找回密码-校验验证码并重置密码 */
export function resetPassword(data: ForgotPasswordResetRequest): Promise<Result<void>> {
  return post<void>('/api/v1/auth/forgot-password/reset', data)
}