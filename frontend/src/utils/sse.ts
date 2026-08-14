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
 * T18 AI 智能客服 SSE 接口: POST /api/v1/ai/customer-service/chat
 */
import { fetchEventSource } from '@microsoft/fetch-event-source'
import axios, { type AxiosResponse } from 'axios'
import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY } from '@/api/request'
import type { Result, TokenVO } from '@/types'

/** API 基础路径, 与 request.ts 的 axios baseURL 保持一致 */
const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL || ''

/** AI 导购助手 SSE 接口完整 URL */
export const AI_SHOPPING_ASSISTANT_URL: string = `${API_BASE_URL}/api/v1/ai/shopping-assistant`

/**
 * 静默刷新 access token（不走 axios 拦截器，避免循环）。
 * 成功返回新 access token，失败返回 null。
 */
async function refreshAccessToken(): Promise<string | null> {
    const refreshTokenValue = localStorage.getItem(REFRESH_TOKEN_KEY)
    if (!refreshTokenValue) return null
    try {
        const res: AxiosResponse<Result<TokenVO>> = await axios.post(
            `${API_BASE_URL}/api/v1/auth/refresh`,
            { refreshToken: refreshTokenValue },
            { headers: { 'Content-Type': 'application/json' }, timeout: 10000 }
        )
        if (res.data?.code === 200 && res.data?.data?.accessToken) {
            const newAccessToken = res.data.data.accessToken
            const newRefreshToken = res.data.data.refreshToken
            localStorage.setItem(ACCESS_TOKEN_KEY, newAccessToken)
            localStorage.setItem(REFRESH_TOKEN_KEY, newRefreshToken)
            return newAccessToken
        }
        return null
    } catch {
        return null
    }
}

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
 * 错误处理:
 *   - 发起前预检 token，若快过期（剩余<30s）则先刷新
 *   - onerror 收到 401 时尝试刷新 token 后重试一次
 *   - 其他错误 throw err 以阻止 fetch-event-source 内置自动重试
 */
export function streamChat(
    url: string,
    body: Record<string, unknown>,
    onMessage: (token: string) => void,
    onClose?: () => void,
    onError?: (err: Error) => void
): Promise<void> {
    return doStreamChat(url, body, onMessage, onClose, onError, false)
}

/** 实际发起 SSE 请求，retried 标记是否已重试过（防无限重试） */
function doStreamChat(
    url: string,
    body: Record<string, unknown>,
    onMessage: (token: string) => void,
    onClose?: () => void,
    onError?: (err: Error) => void,
    retried: boolean = false
): Promise<void> {
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
            // 401 且未重试过：尝试刷新 token 后重试一次
            // fetch-event-source 的 err 通常是 TypeError 或包含状态信息
            // 由于 fetch-event-source 不直接暴露 HTTP 状态码，这里通过错误消息判断
            const errMsg = err?.message || ''
            if (!retried && (errMsg.includes('401') || errMsg.includes('Unauthorized') || errMsg.includes('Expected content-type'))) {
                refreshAccessToken().then((newToken) => {
                    if (newToken) {
                        // 刷新成功，重试一次
                        doStreamChat(url, body, onMessage, onClose, onError, true)
                    } else {
                        // 刷新失败，回调错误
                        const refreshErr = new Error('登录已过期，请重新登录')
                        onError?.(refreshErr)
                    }
                })
                // 返回不 throw，让本次流终止（重试由上面的 doStreamChat 发起）
                return
            }
            onError?.(err)
            // throw 以停止 fetch-event-source 默认的重试, 否则 401/网络错误会无限重连
            throw err
        }
    })
}
