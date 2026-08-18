/**
 * Upload 模块根包 - 通用文件上传模块。
 *
 * <p>Phase U 模块化迁移目标结构，参见 UPLOAD-MIGRATION-PLAN.md。
 *
 * <p>Upload 是最小模块：1 Service（1 方法 uploadImage）+ 1 Controller（1 端点）+ 1 VO（UploadResultVO），
 * 无独立 Entity/Mapper，文件存储能力通过 StorageService 提供。
 *
 * @author wnj
 * @since Phase U.0
 */
package com.seckill.mall.upload;