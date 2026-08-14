/**
 * SSE (Server-Sent Events) 流式请求封装
 *
 * 使用 @microsoft/fetch-event-source 而非原生 EventSource, 因为:
 *   1. 原生 EventSource 仅支持 GET, 不支持 POST (AI 导购需 POST 请求体)
 *   2. 原生 EventSource 无法自定义请求头, 无法携带 JWT Authorization
 *
 * 参照 request.ts 的 baseURL 与 token 策略:
 *   - baseURL 取 import.meta.env.VITE_API_BASE_URL (开发环境为空, 走 Vite 代理 /api → 后端)
 *   - token 从 localStorage 读取, key 复用 request.ts 的 ACCESS_TOKEN_KEY ('access_token')
 *
 * T14 AI 导购助手 SSE 接口: POST /api/v1/ai/shopping-assistant
 */
import { fetchEventSource } from '@microsoft/fetch-event-source'
import { ACCESS_TOKEN_KEY } from '@/api/request'

/** API 基础路径, 与 request.ts 的 axios baseURL 保持一致 */
const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL || ''

/** AI 导购助手 SSE 接口完整 URL */
export const AI_SHOPPING_ASSISTANT_URL: string = `${API_BASE_URL}/api/v1/ai/shopping-assistant`

/**
 * 发起 SSE 流式对话请求
 *
 * @param url SSE 接口完整 URL (默认 AI 导购助手接口)
 * @param body 请求体 (如 { message, conversationId })
 * @param onMessage 每收到一个 token 片段时的回调
 * @param onClose 流正常结束时的回调
 * @param onError 发生错误时的回调
 * @returns fetchEventSource 返回的 Promise (可用于 abort)
 *
 * 错误处理: onerror 中 throw err 以阻止 fetch-event-source 内置的自动重试,
 * 避免后端 401/500 时无限重连。
 */
export function streamChat(
  url: string,
  body: Record<string, unknown>,
  onMessage: (token: string) => void,
  onClose?: () => void,
  onError?: (err: Error) => void
): Promise<void> {
  // 从 localStorage 读取 JWT, 与 request.ts 拦截器取 token 方式一致
  const token: string = localStorage.getItem(ACCESS_TOKEN_KEY) || ''

  return fetchEventSource(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(body),
    // 关闭内置事件缓冲, 让 onmessage 尽快收到每个 data 片段
    onmessage: (ev) => {
      // ev.data 为后端推送的 token 文本片段; 忽略空 data (如心跳注释行)
      if (ev.data) {
        onMessage(ev.data)
      }
    },
    onclose: () => {
      onClose?.()
    },
    onerror: (err: Error) => {
      onError?.(err)
      // throw 以停止 fetch-event-source 默认的重试, 否则 401/网络错误会无限重连
      throw err
    }
  })
}