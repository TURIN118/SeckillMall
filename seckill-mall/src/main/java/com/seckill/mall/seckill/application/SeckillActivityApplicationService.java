package com.seckill.mall.seckill.application;

import com.seckill.mall.dto.SeckillActivityCreateRequest;
import com.seckill.mall.seckill.api.SeckillActivityApi;
import com.seckill.mall.seckill.api.command.CreateActivityCommand;
import com.seckill.mall.seckill.api.dto.SeckillActivityDTO;
import com.seckill.mall.seckill.application.facade.SeckillApiConverter;
import com.seckill.mall.service.SeckillActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SeckillActivity 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link SeckillActivityApi}，内部委托给旧 {@link SeckillActivityService}，
 * 通过 {@link SeckillApiConverter} 做 VO ↔ DTO 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做：
 * <ol>
 *     <li>从 Command 提取业务参数，转换为旧 Request</li>
 *     <li>委托旧 Service 执行业务</li>
 *     <li>将旧 Service 返回的 VO 转换为 API 层 DTO</li>
 * </ol>
 *
 * <p>委托映射参见 SECKILL-API-CONTRACT.md 第 9 节。
 *
 * @author wnj
 * @since Phase SK.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillActivityApplicationService implements SeckillActivityApi {

    private final SeckillActivityService seckillActivityService;

    @Override
    public SeckillActivityDTO createActivity(CreateActivityCommand command) {
        return SeckillApiConverter.toDTO(
                seckillActivityService.createActivity(toRequest(command)));
    }

    @Override
    public List<SeckillActivityDTO> listActivities() {
        return SeckillApiConverter.toActivityDTOList(
                seckillActivityService.listActivities());
    }

    @Override
    public SeckillActivityDTO getActivityDetail(Long activityId) {
        return SeckillApiConverter.toDTO(
                seckillActivityService.getActivityDetail(activityId));
    }

    @Override
    public void deleteActivity(Long activityId) {
        seckillActivityService.deleteActivity(activityId);
    }

    /**
     * CreateActivityCommand → SeckillActivityCreateRequest（适配旧 Service 入参）。
     *
     * <p>Strangler Pattern 过渡期：旧 Service 接受 SeckillActivityCreateRequest，
     * 新 API 层使用 CreateActivityCommand，此处做反向转换以委托旧 Service。
     */
    private static SeckillActivityCreateRequest toRequest(CreateActivityCommand command) {
        if (command == null) {
            return null;
        }
        SeckillActivityCreateRequest request = new SeckillActivityCreateRequest();
        request.setName(command.getName());
        request.setStartTime(command.getStartTime());
        request.setEndTime(command.getEndTime());
        if (command.getGoodsList() != null) {
            request.setGoodsItems(command.getGoodsList().stream()
                    .map(cmd -> {
                        SeckillActivityCreateRequest.ActivityGoodsItem item =
                                new SeckillActivityCreateRequest.ActivityGoodsItem();
                        item.setProductId(cmd.getProductId());
                        item.setSeckillPrice(cmd.getSeckillPrice());
                        item.setStockCount(cmd.getTotalStock());
                        return item;
                    })
                    .collect(java.util.stream.Collectors.toList()));
        }
        return request;
    }
}