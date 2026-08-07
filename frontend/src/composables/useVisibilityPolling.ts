/**
 * 可见性感知轮询 Hook (M-F4 修复)
 *
 * 功能: 在页面可见时按 interval 轮询 callback, 在页面切到后台 (visibilitychange) 时暂停轮询,
 *       减少不必要的网络请求与电量消耗.
 *
 * 用法:
 *   const { start, stop } = useVisibilityPolling(async () => {
 *     await fetchActivities(true)
 *   }, 8000)
 *   onMounted(start)
 *   onUnmounted(stop)
 *
 * 设计要点:
 *   - 仅在 document.hidden === false 时执行 callback
 *   - 页面切到后台时清除定时器, 切回前台时立即执行一次 callback 并恢复定时器
 *   - 支持手动 start/stop (用于组件挂载/卸载)
 *   - 防止 callback 并发执行 (isRunning 标志)
 */
import { ref, onMounted, onUnmounted } from 'vue'

export interface VisibilityPollingOptions {
    /** 是否在页面切回前台时立即执行一次 callback (默认 true) */
    immediateOnVisible?: boolean
}

export function useVisibilityPolling(
    callback: () => Promise<void> | void,
    interval: number,
    options: VisibilityPollingOptions = {}
) {
    const { immediateOnVisible = true } = options

    let timer: ReturnType<typeof setInterval> | null = null
    const isRunning = ref(false)
    const isActive = ref(false)

    /** 执行 callback, 防止并发 */
    async function run(): Promise<void> {
        if (isRunning.value) return
        isRunning.value = true
        try {
            await callback()
        } finally {
            isRunning.value = false
        }
    }

    /** 启动轮询 (仅在页面可见时) */
    function start(): void {
        if (isActive.value) return
        isActive.value = true
        if (!document.hidden) {
            timer = setInterval(run, interval)
        }
    }

    /** 停止轮询 */
    function stop(): void {
        isActive.value = false
        if (timer) {
            clearInterval(timer)
            timer = null
        }
    }

    /** visibilitychange 监听器 */
    function handleVisibilityChange(): void {
        if (!isActive.value) return
        if (document.hidden) {
            // 页面切到后台: 暂停轮询
            if (timer) {
                clearInterval(timer)
                timer = null
            }
        } else {
            // 页面切回前台: 恢复轮询
            if (immediateOnVisible) {
                // 立即执行一次, 让数据尽快同步
                void run()
            }
            if (!timer) {
                timer = setInterval(run, interval)
            }
        }
    }

    onMounted(() => {
        document.addEventListener('visibilitychange', handleVisibilityChange)
    })

    onUnmounted(() => {
        document.removeEventListener('visibilitychange', handleVisibilityChange)
        stop()
    })

    return { start, stop, isRunning, isActive }
}