package com.seckill.mall.seckill.api;

import com.seckill.mall.seckill.api.command.CreateActivityCommand;
import com.seckill.mall.seckill.api.dto.SeckillActivityDTO;

import java.util.List;

/**
 * Seckill 模块秒杀场次 API。
 *
 * <p>对外暴露秒杀场次能力（创建/查询/删除），供 SeckillController 调用。
 *
 * <p>设计原则：
 * <ul>
 *     <li>方法用业务语言命名，禁止 CRUD 命名</li>
 *     <li>入参用 Command/Query 对象，禁止裸露多参数</li>
 *     <li>出参用 Result/DTO/Snapshot，禁止暴露 Entity/Mapper/PO</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出，不泄露堆栈</li>
 * </ul>
 *
 * <p>向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型。
 *
 * <p>原方法映射参见 SECKILL-API-CONTRACT.md 第 9 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
public interface SeckillActivityApi {

    /**
     * 创建秒杀场次（含场次下所有商品）。
     *
     * @param command 创建命令
     * @return 场次 DTO
     */
    SeckillActivityDTO createActivity(CreateActivityCommand command);

    /**
     * 查询所有场次列表（含每场次下商品列表）。
     *
     * @return 场次 DTO 列表
     */
    List<SeckillActivityDTO> listActivities();

    /**
     * 查询场次详情（含商品列表）。
     *
     * @param activityId 场次 ID
     * @return 场次 DTO
     */
    SeckillActivityDTO getActivityDetail(Long activityId);

    /**
     * 删除场次（逻辑删除）。
     *
     * @param activityId 场次 ID
     */
    void deleteActivity(Long activityId);
}