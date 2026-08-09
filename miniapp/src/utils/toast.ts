/**
 * 错误提示封装（对齐 plan.md 第 4.8 节）
 * 统一封装 uni.showToast / uni.showModal / uni.showLoading
 */

type ToastType = 'success' | 'error' | 'loading' | 'none'

/** 显示轻提示 */
export function showToast(title: string, type: ToastType = 'none', duration = 2000): void {
  let icon: 'success' | 'error' | 'loading' | 'none' = 'none'
  switch (type) {
    case 'success':
      icon = 'success'
      break
    case 'error':
      icon = 'error'
      break
    case 'loading':
      icon = 'loading'
      break
    default:
      icon = 'none'
  }
  uni.showToast({ title, icon, duration })
}

/** 显示确认弹窗，返回 Promise<boolean>（true 确认 / false 取消） */
export function showConfirm(content: string, title = '提示'): Promise<boolean> {
  return new Promise((resolve) => {
    uni.showModal({
      title,
      content,
      success: (res) => {
        resolve(res.confirm)
      },
      fail: () => {
        resolve(false)
      }
    })
  })
}

/** 显示加载中 */
export function showLoading(title = '加载中...'): void {
  uni.showLoading({ title, mask: true })
}

/** 隐藏加载中 */
export function hideLoading(): void {
  uni.hideLoading()
}