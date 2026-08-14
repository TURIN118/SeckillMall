/**
 * AI 导购对话 Store (Pinia setup 风格, 与 user.ts 保持一致)
 *
 * 职责:
 *   - 维护对话消息列表 (用户消息 + AI 流式回复)
 *   - 维护 loading 状态 (SSE 流式输出中)
 *   - 维护 conversationId (多轮对话上下文, 由后端首次响应返回, 此处预留)
 *
 * 流式追加策略: appendAssistantToken 把 token 累加到最后一条 assistant 消息,
 * 若最后一条非 assistant (即新一轮回复开始) 则新建一条 assistant 消息。
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 对话消息角色 */
export type ChatRole = 'user' | 'assistant'

/** 单条对话消息 */
export interface ChatMessage {
  role: ChatRole
  content: string
}

export const useChatStore = defineStore('chat', () => {
  /* === State === */
  /** 对话消息列表 */
  const messages = ref<ChatMessage[]>([])
  /** 是否正在等待/接收 AI 流式回复 */
  const loading = ref<boolean>(false)
  /** 多轮对话 ID (后端首次响应返回, 用于上下文续接; 此处预留) */
  const conversationId = ref<string | null>(null)
  /** 最近一次错误信息 (用于 UI 提示) */
  const error = ref<string | null>(null)

  /* === Actions === */

  /** 追加一条用户消息 */
  function addUserMessage(content: string): void {
    messages.value.push({ role: 'user', content })
  }

  /**
   * 追加 AI 回复的 token 片段 (流式累加)
   * 若最后一条已是 assistant 消息, 则累加到其 content;
   * 否则新建一条 assistant 消息 (代表新一轮回复的第一个 token)。
   */
  function appendAssistantToken(token: string): void {
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant') {
      last.content += token
    } else {
      messages.value.push({ role: 'assistant', content: token })
    }
  }

  /** 设置多轮对话 ID */
  function setConversationId(id: string | null): void {
    conversationId.value = id
  }

  /** 设置错误信息 */
  function setError(msg: string | null): void {
    error.value = msg
  }

  /** 清空对话, 重置所有状态 */
  function reset(): void {
    messages.value = []
    conversationId.value = null
    error.value = null
    loading.value = false
  }

  return {
    messages,
    loading,
    conversationId,
    error,
    addUserMessage,
    appendAssistantToken,
    setConversationId,
    setError,
    reset
  }
})