package com.seckill.mall.banner.application;

import com.seckill.mall.banner.api.BannerApi;
import com.seckill.mall.banner.api.dto.BannerDTO;
import com.seckill.mall.banner.application.facade.BannerApiConverter;
import com.seckill.mall.dto.BannerCreateRequest;
import com.seckill.mall.dto.BannerUpdateRequest;
import com.seckill.mall.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Banner 模块 Application 层门面服务（Strangler Pattern）。
 *
 * <p>实现 {@link BannerApi}，作为新 API 层与旧 {@link BannerService} 实现之间的
 * 绞杀者门面（Strangler Facade）。本类不重写任何业务逻辑，仅做：
 * <ul>
 *     <li>委托：所有方法体调用旧 BannerService 对应方法</li>
 *     <li>适配：通过 {@link BannerApiConverter} 将旧 VO 返回值转换为新 DTO</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *     <li>不修改旧 BannerService / BannerServiceImpl 的任何业务行为</li>
 *     <li>不引入新的 SQL / Mapper / 跨模块 Service 依赖</li>
 *     <li>保持旧 BannerService 在 Spring 容器中仍可被其他潜在调用方注入（向后兼容）</li>
 * </ul>
 *
 * <p>迁移路径：Phase B.3 完成后，BannerController / BannerPublicController 切换为依赖 BannerApi（本类），
 * 旧 BannerService 仅被本类引用。后续 Phase 可将 BannerServiceImpl 的业务逻辑
 * 平滑迁入本类或新建的领域服务，再删除旧 BannerService。
 *
 * <p>参见 BANNER-API-CONTRACT.md。
 *
 * @author wnj
 * @since Phase B.0
 */
@Service
@RequiredArgsConstructor
public class BannerApplicationService implements BannerApi {

    private final BannerService bannerService;

    @Override
    public List<BannerDTO> listAll() {
        return BannerApiConverter.toDTOList(bannerService.listAll());
    }

    @Override
    public List<BannerDTO> listActive() {
        return BannerApiConverter.toDTOList(bannerService.listActive());
    }

    @Override
    public BannerDTO create(BannerCreateRequest req) {
        return BannerApiConverter.toDTO(bannerService.create(req));
    }

    @Override
    public BannerDTO update(Long id, BannerUpdateRequest req) {
        return BannerApiConverter.toDTO(bannerService.update(id, req));
    }

    @Override
    public void delete(Long id) {
        bannerService.delete(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        bannerService.updateStatus(id, status);
    }
}