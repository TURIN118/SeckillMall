/**
 * 上传 API（对齐 spec.md 2.6 上传端点）
 * /api/v1/upload（使用 uni.uploadFile）
 */

import { uploadFile } from '@/utils/upload'
import type { UploadResultVO } from '@/types'

/**
 * 上传文件
 * @param filePath 临时文件路径（uni.chooseImage 返回）
 * @param url 上传地址（相对 API_PREFIX），默认 /upload
 */
export function upload(filePath: string, url?: string): Promise<UploadResultVO> {
  return uploadFile<UploadResultVO>({ filePath, url })
}

/** 上传头像 */
export function uploadAvatar(filePath: string): Promise<UploadResultVO> {
  return uploadFile<UploadResultVO>({ filePath, url: '/upload/avatar' })
}

/** 上传评价图片 */
export function uploadReviewImage(filePath: string): Promise<UploadResultVO> {
  return uploadFile<UploadResultVO>({ filePath, url: '/upload/review' })
}