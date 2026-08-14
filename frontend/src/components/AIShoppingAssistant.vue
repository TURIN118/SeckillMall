<template>
  <!--
    AI 导购助手对话组件 (T14)
    - 消息列表: 用户消息右对齐(主色气泡), AI 消息左对齐(浅色气泡)
    - 输入框 + 发送按钮, Enter 发送
    - SSE 流式输出, loading 时显示"AI 正在思考..."
    - 自动滚动到底部
    - 可折叠, 减少对商品列表的遮挡
  -->
  <div class="ai-assistant" :class="{ 'ai-assistant--collapsed': !expanded }">
    <!-- 头部 -->
    <div class="ai-assistant__header" @click="toggleExpand">
      <div class="header-title">
        <span class="header-icon">🤖</span>
        <span class="header-text">AI 导购助手</span>
        <span v-if="chatStore.loading" class="header-status">正在回答...</span>
      </div>
      <div class="header-actions" @click.stop>
        <el-button
          v-if="expanded && chatStore.messages.length > 0"
          text
          size="small"
          @click="chatStore.reset()"
        >清空</el-button>
        <el-button text size="small" :icon="expanded ? ArrowUp : ChatDotRound" @click="toggleExpand">
          {{ expanded ? '收起' : '展开' }}
        </el-button>
      </div>
    </div>

    <!-- 展开内容区 -->
    <div v-show="expanded" class="ai-assistant__body">
      <!-- 消息列表 -->
      <div class="ai-assistant__messages" ref="messagesRef">
        <!-- 欢迎语 -->
        <div v-if="chatStore.messages.length === 0" class="msg-welcome">
          <div class="welcome-icon">🤖</div>
          <div class="welcome-text">
            <p class="welcome-title">你好！我是 AI 导购助手</p>
            <p class="welcome-desc">告诉我你的需求，我来帮你找商品。例如：</p>
            <div class="welcome-suggestions">
              <span class="suggestion-chip" @click="useSuggestion('3000元以内的游戏本')">3000元以内的游戏本</span>
              <span class="suggestion-chip" @click="useSuggestion('适合送女友的口红')">适合送女友的口红</span>
              <span class="suggestion-chip" @click="useSuggestion('降噪蓝牙耳机推荐')">降噪蓝牙耳机推荐</span>
            </div>
          </div>
        </div>

        <!-- 消息气泡 -->
        <div
          v-for="(msg, i) in chatStore.messages"
          :key="i"
          class="msg"
          :class="`msg--${msg.role}`"
        >
          <div class="msg__avatar">{{ msg.role === 'user' ? '🧑' : '🤖' }}</div>
          <div class="msg__bubble">{{ msg.content }}</div>
        </div>

        <!-- loading 占位 (AI 正在思考) -->
        <div v-if="chatStore.loading && !hasAssistantStreaming" class="msg msg--assistant">
          <div class="msg__avatar">🤖</div>
          <div class="msg__bubble msg__bubble--typing">
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
          </div>
        </div>
      </div>

      <!-- 错误提示 -->
      <div v-if="chatStore.error" class="ai-assistant__error">
        <el-alert :title="chatStore.error" type="error" show-icon :closable="false" />
      </div>

      <!-- 输入区 -->
      <div class="ai-assistant__input">
        <el-input
          v-model="input"
          placeholder="描述你想要的商品，如：3000元以内的游戏本"
          :disabled="chatStore.loading"
          @keyup.enter="send"
        />
        <el-button
          type="primary"
          :loading="chatStore.loading"
          :disabled="!input.trim()"
          @click="send"
        >
          {{ chatStore.loading ? '生成中' : '发送' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * AI 导购助手对话组件 (T14)
 *
 * 数据流:
 *   用户输入 → chatStore.addUserMessage → streamChat(SSE)
 *   → onMessage: chatStore.appendAssistantToken (流式累加) + 自动滚动
 *   → onClose/onError: loading=false
 *
 * 依赖:
 *   - @/stores/chat: 对话状态 (Pinia)
 *   - @/utils/sse: SSE 流式请求封装 (fetch-event-source)
 */
import { ref, computed, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowUp, ChatDotRound } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores/chat'
import { streamChat, AI_SHOPPING_ASSISTANT_URL } from '@/utils/sse'

// 显式声明组件名
defineOptions({ name: 'AIShoppingAssistant' })

const chatStore = useChatStore()

/* === 本地状态 === */
/** 输入框内容 */
const input = ref<string>('')
/** 是否展开 (默认展开) */
const expanded = ref<boolean>(true)
/** 消息列表滚动容器引用 */
const messagesRef = ref<HTMLElement | null>(null)

/**
 * 是否已有正在流式输出的 assistant 消息
 * 用于区分: loading 阶段但尚无 token → 显示"思考中"动画; 已有 token → 不显示动画, 直接看流式文本
 */
const hasAssistantStreaming = computed<boolean>(() => {
  const last = chatStore.messages[chatStore.messages.length - 1]
  return !!last && last.role === 'assistant'
})

/* === 滚动到底部 === */
function scrollToBottom(): void {
  nextTick(() => {
    const el = messagesRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

// 监听消息列表变化, 自动滚动到底部 (流式追加 token 时也会触发)
watch(
  () => chatStore.messages.map((m) => m.content).join('|'),
  () => scrollToBottom()
)

/* === 折叠/展开 === */
function toggleExpand(): void {
  expanded.value = !expanded.value
  if (expanded.value) {
    scrollToBottom()
  }
}

/* === 使用推荐问法 === */
function useSuggestion(text: string): void {
  input.value = text
  send()
}

/* === 发送消息 === */
async function send(): Promise<void> {
  const content = input.value.trim()
  if (!content || chatStore.loading) return

  // 1. 追加用户消息
  chatStore.addUserMessage(content)
  // 2. 清空输入框
  input.value = ''
  // 3. 进入 loading
  chatStore.loading = true
  chatStore.setError(null)
  // 4. 滚动到底部 (展示用户消息)
  scrollToBottom()

  // 5. 发起 SSE 流式请求
  try {
    await streamChat(
      AI_SHOPPING_ASSISTANT_URL,
      {
        message: content,
        conversationId: chatStore.conversationId
      },
      // onMessage: 收到 token 片段, 累加到 assistant 消息
      (token: string) => {
        chatStore.appendAssistantToken(token)
        scrollToBottom()
      },
      // onClose: 流正常结束
      () => {
        chatStore.loading = false
      },
      // onError: 发生错误
      (err: Error) => {
        chatStore.loading = false
        chatStore.setError(err.message || 'AI 回复失败，请稍后重试')
        ElMessage.error('AI 回复失败，请稍后重试')
      }
    )
  } catch (err) {
    // streamChat reject (如网络错误、401 等)
    chatStore.loading = false
    const msg = err instanceof Error ? err.message : 'AI 回复失败'
    chatStore.setError(msg)
    // ElMessage 已在 onError 中提示, 此处避免重复提示
    // 但若 onError 未被调用 (如 fetch 直接抛错), 这里兜底提示
    if (!chatStore.error) {
      ElMessage.error(msg)
    }
  }
}
</script>

<style scoped>
/* === 容器 === */
.ai-assistant {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  margin-bottom: 12px;
  /* 渐变左边框, 突出 AI 入口 */
  border-left: 3px solid var(--color-primary);
}

/* === 头部 === */
.ai-assistant__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
  background: linear-gradient(90deg, var(--color-primary-light) 0%, var(--color-bg-card) 100%);
  transition: background 0.15s;
}

.ai-assistant__header:hover {
  background: linear-gradient(90deg, #ffe4e4 0%, var(--color-bg-card) 100%);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 18px;
}

.header-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.header-status {
  font-size: 12px;
  color: var(--color-primary);
  background: var(--color-primary-light);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  margin-left: 4px;
  animation: pulse 1.4s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* === 主体 === */
.ai-assistant__body {
  display: flex;
  flex-direction: column;
}

/* === 消息列表 === */
.ai-assistant__messages {
  max-height: 320px;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: var(--color-bg-subtle);
  scrollbar-width: thin;
}

.ai-assistant__messages::-webkit-scrollbar {
  width: 6px;
}

.ai-assistant__messages::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 3px;
}

/* === 欢迎语 === */
.msg-welcome {
  display: flex;
  gap: 10px;
  padding: 8px 4px;
}

.welcome-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.welcome-text {
  flex: 1;
}

.welcome-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 4px 0;
}

.welcome-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0 0 8px 0;
}

.welcome-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.suggestion-chip {
  font-size: 12px;
  color: var(--color-primary);
  background: var(--color-bg-card);
  border: 1px solid var(--color-primary);
  padding: 4px 10px;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all 0.15s;
}

.suggestion-chip:hover {
  background: var(--color-primary);
  color: #fff;
}

/* === 消息气泡 === */
.msg {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  max-width: 100%;
}

/* 用户消息: 右对齐 (头像在右) */
.msg--user {
  flex-direction: row-reverse;
}

.msg__avatar {
  font-size: 20px;
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.msg__bubble {
  padding: 8px 12px;
  border-radius: var(--radius-lg);
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
  max-width: 80%;
  box-sizing: border-box;
}

/* 用户气泡: 主色 */
.msg--user .msg__bubble {
  background: var(--color-primary);
  color: #fff;
  border-top-right-radius: 2px;
}

/* AI 气泡: 白色卡片 */
.msg--assistant .msg__bubble {
  background: var(--color-bg-card);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border-light);
  border-top-left-radius: 2px;
}

/* === 思考中动画 === */
.msg__bubble--typing {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 10px 14px;
}

.typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-text-muted);
  animation: typing 1.2s ease-in-out infinite;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}

/* === 错误提示 === */
.ai-assistant__error {
  padding: 8px 14px 0;
}

/* === 输入区 === */
.ai-assistant__input {
  display: flex;
  gap: 8px;
  padding: 10px 14px 12px;
  background: var(--color-bg-card);
  border-top: 1px solid var(--color-border-light);
}

.ai-assistant__input .el-input {
  flex: 1;
}

/* === 折叠态 === */
.ai-assistant--collapsed .ai-assistant__header {
  /* 折叠时头部更紧凑 */
  padding: 8px 14px;
}
</style>