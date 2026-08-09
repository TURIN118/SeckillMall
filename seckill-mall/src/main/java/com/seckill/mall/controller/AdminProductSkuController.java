package com.seckill.mall.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.ProductSkuDTO;
import com.seckill.mall.dto.SkuGenerateRequest;
import com.seckill.mall.dto.SkuGenerateRequest.AttributeDefinition;
import com.seckill.mall.dto.SkuGenerateRequest.AttributeValue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台商品 SKU 管理
 * <p>
 * 前缀 /api/v1/admin/product/skus，提供 SKU 笛卡尔积生成等后台能力。
 * 仅 ADMIN 角色可访问。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AdminProductSkuController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "后台商品SKU管理", description = "SKU笛卡尔积生成")
@RestController
@RequestMapping("/api/v1/admin/product/skus")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductSkuController {

    private final ObjectMapper objectMapper;

    /**
     * 生成 SKU 笛卡尔积。
     * <p>
     * 根据前端提交的属性定义（如 颜色:[黑,白] × 版本:[8+128,8+256]）展开所有组合，
     * 每个组合生成一个 {@link ProductSkuDTO}：
     * <ul>
     *   <li>attributes：JSON 字符串，保持属性插入顺序，如 {"颜色":"黑","版本":"8+128"}</li>
     *   <li>price：使用请求中的 defaultPrice</li>
     *   <li>stock：0（待用户填写）</li>
     *   <li>mainImage：取第一个非空的属性值 imageUrl，都没有则为 null</li>
     *   <li>skuCode：null（保存商品时后端自动生成）</li>
     *   <li>status：1（启用）</li>
     * </ul>
     *
     * @param req 属性定义 + 默认价格
     * @return 笛卡尔积展开后的 SKU 列表
     */
    @Operation(summary = "生成SKU笛卡尔积")
    @PostMapping("/generate")
    public Result<List<ProductSkuDTO>> generateSkus(@Valid @RequestBody SkuGenerateRequest req) {
        List<AttributeDefinition> attrDefs = req.getAttributes();

        // 1. 提取每个属性的值列表
        List<List<AttributeValue>> valueLists = new ArrayList<>(attrDefs.size());
        for (AttributeDefinition attr : attrDefs) {
            valueLists.add(attr.getValues());
        }

        // 2. 迭代法生成笛卡尔积：初始为一个空组合，逐个属性扩展
        List<List<AttributeValue>> combinations = new ArrayList<>();
        combinations.add(new ArrayList<>());
        for (List<AttributeValue> values : valueLists) {
            List<List<AttributeValue>> newCombinations = new ArrayList<>();
            for (List<AttributeValue> existing : combinations) {
                for (AttributeValue value : values) {
                    List<AttributeValue> newCombo = new ArrayList<>(existing);
                    newCombo.add(value);
                    newCombinations.add(newCombo);
                }
            }
            combinations = newCombinations;
        }

        // 3. 将每个组合转为 ProductSkuDTO
        List<ProductSkuDTO> result = new ArrayList<>(combinations.size());
        for (List<AttributeValue> combo : combinations) {
            // 用 LinkedHashMap 保持属性插入顺序，序列化后 JSON 字段顺序稳定
            Map<String, String> attrMap = new LinkedHashMap<>(attrDefs.size());
            String mainImage = null;
            for (int i = 0; i < attrDefs.size(); i++) {
                String attrName = attrDefs.get(i).getName();
                AttributeValue av = combo.get(i);
                attrMap.put(attrName, av.getValue());
                if (mainImage == null && av.getImageUrl() != null && !av.getImageUrl().isEmpty()) {
                    mainImage = av.getImageUrl();
                }
            }

            ProductSkuDTO dto = new ProductSkuDTO();
            dto.setAttributes(toJson(attrMap));
            dto.setPrice(req.getDefaultPrice());
            dto.setStock(0);
            dto.setMainImage(mainImage);
            dto.setStatus(1);
            // id / skuCode 保持 null，保存商品时由后端自动生成
            result.add(dto);
        }

        return Result.success(result);
    }

    /**
     * 将属性键值对序列化为 JSON 字符串。序列化失败转 RuntimeException 抛出（理论上不会发生）。
     */
    private String toJson(Map<String, String> attrMap) {
        try {
            return objectMapper.writeValueAsString(attrMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("SKU 属性序列化失败: " + attrMap, e);
        }
    }
}