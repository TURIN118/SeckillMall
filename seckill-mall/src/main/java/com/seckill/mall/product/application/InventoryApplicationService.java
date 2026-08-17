package com.seckill.mall.product.application;

import com.seckill.mall.product.api.InventoryApi;
import com.seckill.mall.product.api.command.DeductStockCommand;
import com.seckill.mall.product.api.command.RestoreStockCommand;
import com.seckill.mall.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 库存应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link InventoryApi}，内部委托给旧 {@link InventoryService}。
 *
 * <p>本类不包含任何业务逻辑，仅做参数提取和委托。
 *
 * @author wnj
 * @since Phase P.4-A
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryApplicationService implements InventoryApi {

    private final InventoryService inventoryService;

    @Override
    public int deductProductStock(DeductStockCommand command) {
        return inventoryService.deductProductStock(command.getProductId(), command.getQuantity());
    }

    @Override
    public void rollbackProductStock(RestoreStockCommand command) {
        inventoryService.rollbackProductStock(command.getProductId(), command.getQuantity());
    }

    @Override
    public boolean deductSkuStock(DeductStockCommand command) {
        return inventoryService.deductSkuStock(command.getSkuId(), command.getQuantity());
    }

    @Override
    public void rollbackSkuStock(RestoreStockCommand command) {
        inventoryService.rollbackSkuStock(command.getSkuId(), command.getQuantity());
    }
}