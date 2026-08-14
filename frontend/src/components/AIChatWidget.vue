<template>
  <!--
    AI 智能客服全局浮窗 (T19)
    - 未展开: 右下角圆形浮动按钮 (fixed)
    - 展开: 右下角聊天面板 (360x520), 头部 + 消息区 + 输入区
    - SSE 流式输出, 复用 @/utils/sse 的 streamChat
    - 未登录引导登录 (跳转 /login?redirect=当前路径)
    - 挂载于 FrontLayout, 全站可用
  -->
  <div class="ai-chat-widget">
    <!-- 浮动按钮 (未展开时显示) -->
    <transition name="float-fade">
      <div v-if="!visible" class="float-btn" @click="openWidget">
        <span class="float-btn__icon">💬</span>
        <span class="float-btn__text">智能客服</span>
        <!-- 未登录小角标提示 -->
        <span v-if="!userStore.isLoggedIn" class="float-btn__badge" title="未登录">!</span>
      </div>
    </transition>

    <!-- 聊天面板 -->
    <transition name="slide-up">
      <div v-if="visible" class="chat-panel" role="dialog" aria-label="智能客服对话">
        <!-- 头部 -->
        <div class="chat-panel__header">
          <div class="chat-panel__title">
            <span class="chat-panel__icon">💬</span>
            <span>智能客服</span>
            <span v-if="loading" class="chat-panel__status">回答中...</span>
          </div>
          <div class="chat-panel__actions">
            <button
              v-if="messages.length > 0"
              class="chat-panel__clear"
              title="清空对话"
              @click="clearMessages"
            >清空</button>
            <el-icon class="chat-panel__close" @click="closeWidget"><Close /></el-icon>
          </div>
        </div>

        <!-- 消息区 -->
        <div class="chat-panel__messages" ref="messagesRef">
          <!-- 欢迎语 -->
          <div v-if="messages.length === 0" class="msg-welcome">
            <div class="welcome-icon">💬</div>
            <div class="welcome-text">
              <p class="welcome-title">你好，我是智能客服</p>
              <p class="welcome-desc">秒杀规则、订单、支付、售后等问题都可以问我。例如：</p>
              <div class="welcome-suggestions">
                <span class="suggestion-chip" @click="useSuggestion('秒杀活动规则是什么？')">秒杀活动规则</span>
                <span class="suggestion-chip" @click="useSuggestion('订单一直未支付怎么办？')">订单未支付</span>
                <span class="suggestion-chip" @click="useSuggestion('如何申请退款？')">如何申请退款</span>
              </div>
            </div>
          </div>

          <!-- 消息气泡 -->
          <div
            v-for="(msg, i) in messages"
            :key="i"
            class="msg"
            :class="`msg--${msg.role}`"
          >
            <div class="msg__avatar">{{ msg.role === 'user' ? '🧑' : '💬' }}</div>
            <div class="msg__bubble">{{ msg.content }}</div>
          </div>

          <!-- loading 占位 (客服正在输入) -->
          <div v-if="loading && !hasAssistantStreaming" class="msg msg--assistant">
            <div class="msg__avatar">💬</div>
            <div class="msg__bubble msg__bubble--typing">
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
            </div>
          </div>
        </div>

        <!-- 错误提示 -->
        <div v-if="errorMsg" class="chat-panel__error">
          <el-alert :title="errorMsg" type="error" show-icon :closable="false" />
        </div>

        <!-- 输入区 -->
        <div class="chat-panel__input">
          <el-input
            v-model="input"
            placeholder="描述你的问题..."
            :disabled="loading"
            @keyup.enter="send"
          />
          <el-button
            type="primary"
            :loading="loading"
            :disabled="!input.trim()"
            @click="send"
          >
            {{ loading ? '生成中' : '发送' }}
          </el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
/**
 * AI 智能客服全局浮窗 (T19)
 *
 * 数据流:
 *   用户输入 → 校验登录 → 追加用户消息 → streamChat(SSE)
 *   → onMessage: 累加 token 到 assistant 消息 + 自动滚动
 *   → onClose/onError: loading=false
 *
 * 与 T14 AIShoppingAssistant 的区别:
 *   - 浮窗形态 (fixed 右下角), 而非内嵌面板
 *   - 组件内部维护消息列表, 不依赖 Pinia store (客服对话为一次性会话, 无需跨页面持久化)
 *   - 接口 URL: /api/v1/ai/customer-service (T18 后端)
 *
 * 依赖:
 *   - @/utils/sse: streamChat (复用 T14 SSE 封装)
 *   - @/stores/user: 登录态判断
 */
import { ref, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { streamChat } from '@/utils/sse'

// 显式声明组件名
defineOptions({ name: 'AIChatWidget' })

/** 客服 SSE 接口完整 URL (与 sse.ts 中 AI_SHOPPING_ASSISTANT_URL 同构, 此处独立常量) */
const AI_CUSTOMER_SERVICE_URL: string = '/api/v1/ai/customer-service'

/** 消息角色 */
type ChatRole = 'user' | 'assistant'

/** 单条消息 */
interface ChatMessage {
  role: ChatRole
  content: string
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/* === 本地状态 === */
/** 浮窗是否展开 */
const visible = ref<boolean>(false)
/** 输入框内容 */
const input = ref<string>('')
/** 是否正在等待 AI 回复 */
const loading = ref<boolean>(false)
/** 错误信息 */
const errorMsg = ref<string>('')
/** 消息列表 */
const messages = ref<ChatMessage[]>([])
/** 消息区滚动容器引用 */
const messagesRef = ref<HTMLElement | null>(null)

/**
 * 是否已有正在流式输出的 assistant 消息
 * 用于区分: loading 但尚无 token → 显示"思考中"动画; 已有 token → 不显示动画, 直接看流式文本
 */
const hasAssistantStreaming = computed<boolean>(() => {
  const last = messages.value[messages.value.length - 1]
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
  () => messages.value.map((m) => m.content).join('|'),
  () => scrollToBottom()
)

/* === 打开/关闭浮窗 === */
/**
 * 打开浮窗: 校验登录态, 未登录引导登录
 * 已登录直接展开
 */
function openWidget(): void {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后使用智能客服')
    // 跳转登录页, 携带 redirect 回到当前页
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }
  visible.value = true
  scrollToBottom()
}

/** 关闭浮窗 */
function closeWidget(): void {
  visible.value = false
}

/** 清空对话 */
function clearMessages(): void {
  messages.value = []
  errorMsg.value = ''
}

/* === 使用推荐问法 === */
function useSuggestion(text: string): void {
  input.value = text
  send()
}

/* === 发送消息 === */
/**
 * 发送流程:
 *   1. 校验输入非空 + 非 loading
 *   2. 二次校验登录态 (防止登录态过期)
 *   3. 追加用户消息, 清空输入, 进入 loading
 *   4. 调用 streamChat 发起 SSE 流式请求
 *   5. onMessage: 累加 token 到 assistant 消息
 *   6. onClose: loading=false
 *   7. onError: loading=false + 错误提示
 */
async function send(): Promise<void> {
  const content = input.value.trim()
  if (!content || loading.value) return

  // 二次校验登录态 (用户可能在浮窗打开后过期)
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后使用智能客服')
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }

  // 1. 追加用户消息
  messages.value.push({ role: 'user', content })
  // 2. 清空输入框
  input.value = ''
  // 3. 进入 loading
  loading.value = true
  errorMsg.value = ''
  // 4. 滚动到底部 (展示用户消息)
  scrollToBottom()

  // 5. 发起 SSE 流式请求
  try {
    await streamChat(
      AI_CUSTOMER_SERVICE_URL,
      { message: content },
      // onMessage: 收到 token 片段, 累加到 assistant 消息
      (token: string) => {
        const last = messages.value[messages.value.length - 1]
        if (last && last.role === 'assistant') {
          // 已有 assistant 消息, 累加 token
          last.content += token
        } else {
          // 新建 assistant 消息
          messages.value.push({ role: 'assistant', content: token })
        }
        scrollToBottom()
      },
      // onClose: 流正常结束
      () => {
        loading.value = false
      },
      // onError: 发生错误
      (err: Error) => {
        loading.value = false
        errorMsg.value = err.message || '客服回复失败，请稍后重试'
        ElMessage.error('客服回复失败，请稍后重试')
      }
    )
  } catch (err) {
    // streamChat reject (如网络错误、401 等)
    loading.value = false
    const msg = err instanceof Error ? err.message : '客服回复失败'
    if (!errorMsg.value) {
      errorMsg.value = msg
      ElMessage.error(msg)
    }
  }
}
</script>

<style scoped>
/* === 根容器 (不占布局, 仅作为定位上下文) === */
.ai-chat-widget {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 1500;
}

/* 让子元素重新接收事件 */
.float-btn,
.chat-panel {
  pointer-events: auto;
}

/* === 浮动按钮 === */
.float-btn {
  position: fixed;
  right: 24px;
  bottom: 24px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 18px;
  height: 52px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, #e53935, #ff6d00);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(229, 57, 53, 0.35);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  user-select: none;
  z-index: 1501;
}

.float-btn:hover {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 10px 28px rgba(229, 57, 53, 0.45);
}

.float-btn__icon {
  font-size: 22px;
  line-height: 1;
}

.float-btn__text {
  letter-spacing: 0.02em;
}

/* 未登录小角标 */
.float-btn__badge {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  color: #e53935;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #e53935;
}

/* 浮动按钮淡入淡出 */
.float-fade-enter-active,
.float-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.float-fade-enter-from,
.float-fade-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

/* === 聊天面板 === */
.chat-panel {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 360px;
  height: 520px;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-card);
  border-radius: var(--radius-2xl);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
  overflow: hidden;
  z-index: 1502;
  border: 1px solid var(--color-border-light);
}

/* 面板向上滑入 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.25s ease;
  transform-origin: bottom right;
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

/* === 头部 === */
.chat-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(90deg, #e53935 0%, #ff6d00 100%);
  color: #fff;
  flex-shrink: 0;
}

.chat-panel__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}

.chat-panel__icon {
  font-size: 18px;
}

.chat-panel__status {
  font-size: 12px;
  background: rgba(255, 255, 255, 0.25);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  margin-left: 4px;
  animation: pulse 1.4s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.chat-panel__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-panel__clear {
  background: rgba(255, 255, 255, 0.18);
  border: none;
  color: #fff;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: background 0.15s;
}

.chat-panel__clear:hover {
  background: rgba(255, 255, 255, 0.32);
}

.chat-panel__close {
  font-size: 18px;
  cursor: pointer;
  color: #fff;
  transition: transform 0.15s;
}

.chat-panel__close:hover {
  transform: rotate(90deg);
}

/* === 消息区 === */
.chat-panel__messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: var(--color-bg-subtle);
  scrollbar-width: thin;
}

.chat-panel__messages::-webkit-scrollbar {
  width: 6px;
}

.chat-panel__messages::-webkit-scrollbar-thumb {
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
.chat-panel__error {
  padding: 8px 14px 0;
  flex-shrink: 0;
}

/* === 输入区 === */
.chat-panel__input {
  display: flex;
  gap: 8px;
  padding: 10px 14px 12px;
  background: var(--color-bg-card);
  border-top: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.chat-panel__input .el-input {
  flex: 1;
}

/* === 响应式: 小屏适配 === */
@media (max-width: 480px) {
  .chat-panel {
    right: 8px;
    left: 8px;
    bottom: 8px;
    width: auto;
    height: 70vh;
  }

  .float-btn {
    right: 16px;
    bottom: 16px;
  }
}
</style>