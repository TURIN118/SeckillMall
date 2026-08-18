package com.seckill.mall.banner.api;

import com.seckill.mall.banner.api.dto.BannerDTO;
import com.seckill.mall.dto.BannerCreateRequest;
import com.seckill.mall.dto.BannerUpdateRequest;

import java.util.List;

/**
 * Banner 模块轮播图管理 API。
 *
 * <p>对外暴露轮播图管理能力，供 BannerController / BannerPublicController 与其他模块调用。
 *
 * <p>设计原则：
 * <ul>
 *     <li>方法用业务语言命名，禁止 CRUD 命名</li>
 *     <li>出参用 Result/DTO/Snapshot，禁止暴露 Entity/Mapper/PO</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出，不泄露堆栈</li>
 * </ul>
 *
 * <p>向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型。
 *
 * @author wnj
 * @since Phase B.0
 */
public interface BannerApi {

    /**
     * 查询所有轮播图（按 sortOrder 升序），用于后台管理。
     *
     * @return 轮播图 DTO 列表
     */
    List<BannerDTO> listAll();

    /**
     * 查询启用的轮播图（status=1，按 sortOrder 升序），用于前台首页。
     *
     * @return 启用的轮播图 DTO 列表
     */
    List<BannerDTO> listActive();

    /**
     * 新增轮播图。
     *
     * @param req 新增请求 DTO（title 非空、URL 协议白名单已校验）
     * @return 新增后的轮播图 DTO
     */
    BannerDTO create(BannerCreateRequest req);

    /**
     * 编辑轮播图。
     *
     * @param id  轮播图 ID
     * @param req 编辑请求 DTO（字段可选，null 表示不更新）
     * @return 更新后的轮播图 DTO
     */
    BannerDTO update(Long id, BannerUpdateRequest req);

    /**
     * 删除轮播图（逻辑删除）。
     *
     * @param id 轮播图 ID
     */
    void delete(Long id);

    /**
     * 切换轮播图启用/禁用状态。
     *
     * @param id     轮播图 ID
     * @param status 状态：1-启用 / 0-禁用
     */
    void updateStatus(Long id, Integer status);
}