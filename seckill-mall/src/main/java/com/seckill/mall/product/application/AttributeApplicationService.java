package com.seckill.mall.product.application;

import com.seckill.mall.product.api.AttributeApi;
import com.seckill.mall.product.api.command.SaveAttributesCommand;
import com.seckill.mall.product.api.dto.AttributeDTO;
import com.seckill.mall.product.application.facade.ProductApiConverter;
import com.seckill.mall.service.ProductAttributeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 属性应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link AttributeApi}，内部委托给旧 {@link ProductAttributeService}，
 * 通过 {@link ProductApiConverter} 做 VO ↔ DTO 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做参数转换、委托和结果转换。
 *
 * @author wnj
 * @since Phase P.4-A
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttributeApplicationService implements AttributeApi {

    private final ProductAttributeService productAttributeService;

    @Override
    public List<AttributeDTO> listAttributesByProductId(Long productId) {
        return ProductApiConverter.toAttributeDTOListFromVO(
                productAttributeService.listByProductId(productId));
    }

    @Override
    public void saveAttributes(SaveAttributesCommand command) {
        productAttributeService.saveAttributes(
                command.getProductId(),
                ProductApiConverter.toProductAttributeDTOList(command.getAttributes()));
    }

    @Override
    public void deleteAttributesByProductId(Long productId) {
        productAttributeService.deleteByProductId(productId);
    }
}