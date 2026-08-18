package com.seckill.mall.service;

import com.seckill.mall.dto.BannerCreateRequest;
import com.seckill.mall.dto.BannerUpdateRequest;
import com.seckill.mall.banner.interfaces.vo.BannerVO;

import java.util.List;

/**
 * 轮播图服务接口
 *
 * M-D1 修复：create/update 入参从 BannerVO 改为专用请求 DTO。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：BannerService.java
 * 邮箱：nj651217@163.com
 */
public interface BannerService {

    /**
     * 查询所有轮播图（按 sortOrder 升序），用于后台管理
     *
     * @return 轮播图列表
     */
    List<BannerVO> listAll();

    /**
     * 查询启用的轮播图（status=1，按 sortOrder 升序），用于前台首页
     *
     * @return 启用的轮播图列表
     */
    List<BannerVO> listActive();

    /**
     * 新增轮播图
     *
     * @param req 新增请求 DTO（title 非空、URL 协议白名单已校验）
     * @return 新增后的轮播图视图对象
     */
    BannerVO create(BannerCreateRequest req);

    /**
     * 编辑轮播图
     *
     * @param id  轮播图 ID
     * @param req 编辑请求 DTO（字段可选，null 表示不更新）
     * @return 更新后的轮播图视图对象
     */
    BannerVO update(Long id, BannerUpdateRequest req);

    /**
     * 删除轮播图（逻辑删除）
     *
     * @param id 轮播图 ID
     */
    void delete(Long id);

    /**
     * 切换轮播图启用/禁用状态
     *
     * @param id     轮播图 ID
     * @param status 状态：1-启用 / 0-禁用
     */
    void updateStatus(Long id, Integer status);
}
