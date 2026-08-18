package com.seckill.mall.identity.application;

import com.seckill.mall.common.Result;
import com.seckill.mall.identity.api.FavoriteApi;
import com.seckill.mall.identity.api.command.AddFavoriteCommand;
import com.seckill.mall.identity.api.command.RemoveFavoriteCommand;
import com.seckill.mall.identity.api.dto.FavoriteItemDTO;
import com.seckill.mall.identity.application.facade.IdentityApiConverter;
import com.seckill.mall.service.UserFavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Favorite 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link FavoriteApi}，内部委托给旧 {@link UserFavoriteService}，
 * 通过 {@link IdentityApiConverter} 做 VO ↔ DTO 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做参数转换、委托和结果转换。
 *
 * <p>说明：旧 {@link UserFavoriteService} 返回 {@code Result<T>} 包装类型，
 * 本类解包后取 {@code data} 字段返回。旧 Service 异常时 {@code Result.code != 200}，
 * 过渡期保留此行为，后续 Phase 再细化异常映射。
 *
 * @author wnj
 * @since Phase I.4-A
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteApplicationService implements FavoriteApi {

    private final UserFavoriteService userFavoriteService;

    @Override
    public List<FavoriteItemDTO> listFavorites(Long userId) {
        Result<List<com.seckill.mall.vo.FavoriteItemVO>> result = userFavoriteService.getFavoriteList(userId);
        if (result == null || result.getData() == null) {
            return List.of();
        }
        return IdentityApiConverter.toFavoriteItemDTOListFromVO(result.getData());
    }

    @Override
    public void addFavorite(AddFavoriteCommand command) {
        userFavoriteService.addFavorite(command.getUserId(), command.getProductId());
    }

    @Override
    public void removeFavorite(RemoveFavoriteCommand command) {
        userFavoriteService.removeFavorite(command.getUserId(), command.getProductId());
    }

    @Override
    public boolean checkFavorite(Long userId, Long productId) {
        Result<Boolean> result = userFavoriteService.isFavorited(userId, productId);
        return result != null && Boolean.TRUE.equals(result.getData());
    }

    @Override
    public int getFavoriteCount(Long userId) {
        Result<Integer> result = userFavoriteService.getFavoriteCount(userId);
        return result != null && result.getData() != null ? result.getData() : 0;
    }
}