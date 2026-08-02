/**
 * 后台用户管理 API - 严格匹配 default.md
 */
import { get, put } from './request'
import type {
  Result,
  PageResult,
  UserVO,
  LoginLogVO,
  UserListRequest,
  UserStatusUpdateRequest,
  UserRoleUpdateRequest
} from '@/types'

/** 用户列表 (分页+角色-状态筛选) */
export function getUserList(params: UserListRequest): Promise<Result<PageResult<UserVO>>> {
  return get<PageResult<UserVO>>('/api/v1/admin/users', params)
}

/** 启用-禁用用户 */
export function updateUserStatus(
  userId: number,
  data: UserStatusUpdateRequest
): Promise<Result<void>> {
  return put<void>(`/api/v1/admin/users/${userId}/status`, data)
}

/** 修改用户角色 */
export function updateUserRole(
  userId: number,
  data: UserRoleUpdateRequest
): Promise<Result<void>> {
  return put<void>(`/api/v1/admin/users/${userId}/role`, data)
}

/** 用户登录日志 (分页) */
export function getUserLoginLogs(
  userId: number,
  params: { pageNum?: number; pageSize?: number }
): Promise<Result<PageResult<LoginLogVO>>> {
  return get<PageResult<LoginLogVO>>(`/api/v1/admin/users/${userId}/logs`, params)
}