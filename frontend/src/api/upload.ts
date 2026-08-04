/**
 * 图片上传 API
 * 对接后端 POST /api/v1/upload/image（multipart/form-data）
 */
import { post } from './request'
import type { Result } from '@/types'

/** 上传结果视图对象 */
export interface UploadResultVO {
    /** 完整可访问 URL */
    url: string
    /** 原始文件名 */
    originalName: string
    /** 文件大小（字节） */
    size: number
    /** 图片宽度（像素），无法解析时为 null */
    width: number | null
    /** 图片高度（像素），无法解析时为 null */
    height: number | null
}

/**
 * 上传图片
 * @param file    图片文件
 * @param bizType 业务类型（可选，如 products/seckill/avatar/category）
 */
export function uploadImage(file: File, bizType?: string): Promise<Result<UploadResultVO>> {
    const formData = new FormData()
    formData.append('file', file)
    if (bizType) {
        formData.append('bizType', bizType)
    }
    return post<UploadResultVO>('/api/v1/upload/image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}