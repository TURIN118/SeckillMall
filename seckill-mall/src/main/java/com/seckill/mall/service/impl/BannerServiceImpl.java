package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.XssCleanUtil;
import com.seckill.mall.dto.BannerCreateRequest;
import com.seckill.mall.dto.BannerUpdateRequest;
import com.seckill.mall.entity.Banner;
import com.seckill.mall.mapper.BannerMapper;
import com.seckill.mall.service.BannerService;
import com.seckill.mall.vo.BannerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 轮播图服务实现
 *
 * H-S1 修复：入库前对 title/linkUrl/imageUrl 调 XssCleanUtil.clean，并对 URL 做协议白名单（http/https）。
 * M-F10 修复：新增时校验 title 非空。
 * M-D1 修复：create/update 入参改为专用请求 DTO。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：BannerServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    /** 启用状态 */
    private static final int STATUS_ENABLED = 1;

    /** 默认排序值 */
    private static final int DEFAULT_SORT_ORDER = 0;

    private final BannerMapper bannerMapper;

    @Override
    public List<BannerVO> listAll() {
        // is_deleted=0 由 @TableLogic 自动追加
        List<Banner> banners = bannerMapper.selectList(
                new LambdaQueryWrapper<Banner>()
                        .orderByAsc(Banner::getSortOrder)
                        .orderByAsc(Banner::getId));
        return banners.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<BannerVO> listActive() {
        List<Banner> banners = bannerMapper.selectList(
                new LambdaQueryWrapper<Banner>()
                        .eq(Banner::getStatus, STATUS_ENABLED)
                        .orderByAsc(Banner::getSortOrder)
                        .orderByAsc(Banner::getId));
        return banners.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public BannerVO create(BannerCreateRequest req) {
        // M-F10 修复：title 非空校验（@Valid 已在 Controller 层触发，此处做防御性二次校验）
        if (!StringUtils.hasText(req.getTitle())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "轮播图标题不能为空");
        }

        Banner banner = new Banner();
        // H-S1 修复：入库前 XSS 清洗（title 用严格模式移除所有 HTML 标签，URL 清洗后做协议白名单）
        banner.setTitle(XssCleanUtil.cleanStrict(req.getTitle()));
        banner.setImageUrl(sanitizeUrl(req.getImageUrl()));
        banner.setLinkUrl(sanitizeUrl(req.getLinkUrl()));
        banner.setSortOrder(req.getSortOrder() == null ? DEFAULT_SORT_ORDER : req.getSortOrder());
        banner.setStatus(req.getStatus() == null ? STATUS_ENABLED : req.getStatus());

        bannerMapper.insert(banner);
        log.info("新增轮播图成功，id={}, title={}", banner.getId(), banner.getTitle());
        return toVO(banner);
    }

    @Override
    public BannerVO update(Long id, BannerUpdateRequest req) {
        Banner existing = bannerMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.BANNER_NOT_FOUND);
        }

        if (req.getTitle() != null) {
            // M-F10 修复：编辑时若提供 title 则不能为空字符串
            if (!StringUtils.hasText(req.getTitle())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "轮播图标题不能为空");
            }
            // H-S1 修复：XSS 严格清洗
            existing.setTitle(XssCleanUtil.cleanStrict(req.getTitle()));
        }
        if (req.getImageUrl() != null) {
            existing.setImageUrl(sanitizeUrl(req.getImageUrl()));
        }
        if (req.getLinkUrl() != null) {
            existing.setLinkUrl(sanitizeUrl(req.getLinkUrl()));
        }
        if (req.getSortOrder() != null) {
            existing.setSortOrder(req.getSortOrder());
        }
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }

        bannerMapper.updateById(existing);
        log.info("编辑轮播图成功，id={}", id);
        return toVO(existing);
    }

    @Override
    public void delete(Long id) {
        Banner existing = bannerMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.BANNER_NOT_FOUND);
        }
        // 逻辑删除（@TableLogic 自动处理 is_deleted 字段）
        bannerMapper.deleteById(id);
        log.info("删除轮播图成功，id={}", id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Banner existing = bannerMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.BANNER_NOT_FOUND);
        }
        // L17 修复：校验 status 值合法（0=禁用，1=启用），避免任意值落库
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态值非法，仅允许 0 或 1");
        }
        // 只更新 status 字段
        Banner toUpdate = new Banner();
        toUpdate.setId(id);
        toUpdate.setStatus(status);
        bannerMapper.updateById(toUpdate);
        log.info("切换轮播图状态成功，id={}, status={}", id, status);
    }

    /**
     * H-S1 修复：URL 字段 XSS 清洗 + 协议白名单（仅允许 http/https）。
     * - null/空字符串直接返回（可选字段）
     * - 清洗后若不含 "://" 或不以 http(s):// 开头，拒绝（防 javascript:、data: 等危险协议）
     */
    private String sanitizeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        String cleaned = XssCleanUtil.clean(url);
        String lower = cleaned.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "URL 必须以 http:// 或 https:// 开头");
        }
        return cleaned;
    }

    /** Entity → VO 手动映射 */
    private BannerVO toVO(Banner banner) {
        BannerVO vo = new BannerVO();
        vo.setId(banner.getId());
        vo.setTitle(banner.getTitle());
        vo.setImageUrl(banner.getImageUrl());
        vo.setLinkUrl(banner.getLinkUrl());
        vo.setSortOrder(banner.getSortOrder());
        vo.setStatus(banner.getStatus());
        vo.setCreateTime(banner.getCreateTime());
        vo.setUpdateTime(banner.getUpdateTime());
        return vo;
    }
}
