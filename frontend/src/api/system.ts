/**
 * 系统管理 API - 严格匹配 default.md
 *
 * 说明：本文件内的 SystemResourceVO 是对 types/index.ts 中 SystemHealthVO 的扩展，
 * 用于承载后端在 GET /api/v1/admin/system/health 上补充返回的资源监控字段。
 * 故意不修改 types/index.ts，避免影响其他引用 SystemHealthVO 的位置。
 */
import { get } from './request'
import type {
  Result,
  DashboardVO,
  PageResult,
  OperationLogVO,
  OperationLogQueryRequest
} from '@/types'

/**
 * 系统健康响应（扩展版）
 *
 * - redis/database/mq/allHealthy：与 types/index.ts 的 SystemHealthVO 保持一致
 * - cpuUsage/memoryUsage/diskUsage/redisHitRate：资源监控字段，后端扩展后返回；
 *   前端做兼容处理——有则用，无则 undefined，显示占位而非假数据。
 * - redisResponseTime/dbPoolUsage/mqQueueBacklog：服务元信息，可选。
 *
 * 详见 API-GAP-2.md「系统资源监控接口」一节。
 */
export interface SystemResourceVO {
  redis: string
  database: string
  mq: string
  allHealthy: boolean
  // 资源监控（后端扩展后返回，前端兼容：有则用，无则 undefined）
  cpuUsage?: number
  memoryUsage?: number
  diskUsage?: number
  redisHitRate?: number
  // 服务元信息（可选）
  redisResponseTime?: string
  dbPoolUsage?: string
  mqQueueBacklog?: string
}

/** 仪表盘统计 */
export function getDashboard(): Promise<Result<DashboardVO>> {
  return get<DashboardVO>('/api/v1/admin/dashboard')
}

/** 操作日志列表 (分页+模块筛选) */
export function getOperationLogs(
  params: OperationLogQueryRequest
): Promise<Result<PageResult<OperationLogVO>>> {
  return get<PageResult<OperationLogVO>>('/api/v1/admin/operation-logs', params)
}

/**
 * 系统健康检查
 *
 * 返回类型使用本文件定义的 SystemResourceVO（SystemHealthVO 的扩展），
 * 以兼容后端未来补充的资源监控字段。后端未扩展时，资源字段为 undefined，
 * 前端展示占位符（'—'）而非 mock 数据。
 */
export function getSystemHealth(): Promise<Result<SystemResourceVO>> {
  return get<SystemResourceVO>('/api/v1/admin/system/health')
}
