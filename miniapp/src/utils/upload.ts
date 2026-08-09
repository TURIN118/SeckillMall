/**
 * 文件上传封装（对齐 plan.md 第 4.7 节）
 * 小程序端使用 uni.uploadFile 替代 Web 端 FormData + Axios
 */

import { ENV } from '@/utils/env'
import { tokenStorage } from '@/utils/tokenStorage'
import type { Result } from '@/types'

interface UploadOptions {
  /** 临时文件路径（uni.chooseImage 返回） */
  filePath: string
  /** 上传地址（相对 API_PREFIX），默认 /upload */
  url?: string
  /** 文件字段名，默认 file */
  name?: string
  /** 额外表单数据 */
  formData?: Record<string, any>
  /** 是否跳过自动添加 token */
  skipAuth?: boolean
}

/**
 * 文件上传
 * @returns Promise<T>，解包 Result<T>.data
 */
export function uploadFile<T = any>(options: UploadOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const accessToken = !options.skipAuth ? tokenStorage.getAccessToken() : null
    uni.uploadFile({
      url: `${ENV.API_BASE_URL}${ENV.API_PREFIX}${options.url || '/upload'}`,
      filePath: options.filePath,
      name: options.name || 'file',
      formData: options.formData,
      header: {
        'Authorization': accessToken ? `Bearer ${accessToken}` : ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          try {
            const data = JSON.parse(res.data) as Result<T>
            if (data.code === 200) {
              resolve(data.data)
            } else {
              reject(new Error(data.message || '上传失败'))
            }
          } catch (e) {
            reject(new Error('解析上传响应失败'))
          }
        } else {
          reject(new Error(`上传失败，状态码：${res.statusCode}`))
        }
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}