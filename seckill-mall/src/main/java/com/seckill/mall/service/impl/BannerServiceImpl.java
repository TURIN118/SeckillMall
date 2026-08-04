package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.entity.Banner;
import com.seckill.mall.mapper.BannerMapper;
import com.seckill.mall.service.BannerService;
import com.seckill.mall.vo.BannerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 轮播图服务实现
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
    public BannerVO create(BannerVO vo) {
        Banner banner = new Banner();
        banner.setTitle(vo.getTitle());
        banner.setImageUrl(vo.getImageUrl());
        banner.setLinkUrl(vo.getLinkUrl());
        banner.setSortOrder(vo.getSortOrder() == null ? DEFAULT_SORT_ORDER : vo.getSortOrder());
        banner.setStatus(vo.getStatus() == null ? STATUS_ENABLED : vo.getStatus());

        bannerMapper.insert(banner);
        log.info("新增轮播图成功，id={}, title={}", banner.getId(), banner.getTitle());
        return toVO(banner);
    }

    @Override
    public BannerVO update(Long id, BannerVO vo) {
        Banner existing = bannerMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.BANNER_NOT_FOUND);
        }

        if (vo.getTitle() != null) {
            existing.setTitle(vo.getTitle());
        }
        if (vo.getImageUrl() != null) {
            existing.setImageUrl(vo.getImageUrl());
        }
        if (vo.getLinkUrl() != null) {
            existing.setLinkUrl(vo.getLinkUrl());
        }
        if (vo.getSortOrder() != null) {
            existing.setSortOrder(vo.getSortOrder());
        }
        if (vo.getStatus() != null) {
            existing.setStatus(vo.getStatus());
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