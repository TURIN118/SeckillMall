/**
 * 认证 API（对齐 spec.md 2.6 认证端点）
 * /api/v1/auth/*
 */

import { get, post, put } from '@/utils/request'
import type {
  LoginRequest,
  LoginVO,
  RegisterRequest,
  CaptchaVO,
  RefreshTokenRequest,
  TokenVO,
  UserVO,
  ChangePasswordRequest,
  UpdateProfileRequest,
  SendResetCodeRequest,
  ResetPasswordRequest
} from '@/types'

/** 登录 */
export function login(data: LoginRequest): Promise<LoginVO> {
  return post<LoginVO>('/auth/login', data, { skipAuth: true })
}

/** 注册 */
export function register(data: RegisterRequest): Promise<any> {
  return post('/auth/register', data, { skipAuth: true })
}

/** 获取图形验证码 */
export function getCaptcha(): Promise<CaptchaVO> {
  return get<CaptchaVO>('/auth/captcha', undefined, { skipAuth: true })
}

/** 刷新 Token */
export function refresh(data: RefreshTokenRequest): Promise<TokenVO> {
  return post<TokenVO>('/auth/refresh', data, { skipAuth: true })
}

/** 获取当前用户信息 */
export function me(): Promise<UserVO> {
  return get<UserVO>('/auth/me')
}

/** 登出 */
export function logout(): Promise<any> {
  return post('/auth/logout')
}

/** 修改密码 */
export function changePassword(data: ChangePasswordRequest): Promise<any> {
  return put('/auth/password', data)
}

/** 修改个人资料 */
export function updateProfile(data: UpdateProfileRequest): Promise<UserVO> {
  return put<UserVO>('/auth/profile', data)
}

/** 发送重置密码验证码 */
export function sendResetCode(data: SendResetCodeRequest): Promise<any> {
  return post('/auth/forgot-password/send-code', data, { skipAuth: true })
}

/** 重置密码 */
export function resetPassword(data: ResetPasswordRequest): Promise<any> {
  return post('/auth/forgot-password/reset', data, { skipAuth: true })
}