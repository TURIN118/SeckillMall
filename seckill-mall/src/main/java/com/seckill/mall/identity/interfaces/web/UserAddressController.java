package com.seckill.mall.identity.interfaces.web;

import com.seckill.mall.common.Result;
import com.seckill.mall.identity.api.AddressApi;
import com.seckill.mall.identity.api.command.SaveAddressCommand;
import com.seckill.mall.identity.api.command.UpdateAddressCommand;
import com.seckill.mall.identity.api.dto.AddressDTO;
import com.seckill.mall.identity.application.facade.IdentityApiConverter;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.vo.UserAddressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收货地址管理控制器（Phase I.4-C 已切换到 {@link AddressApi}）。
 *
 * <p>前缀 {@code /api/v1/addresses}，需登录且角色为 BUYER 或 ADMIN。
 *
 * <p>Strangler Pattern：注入 {@link AddressApi} 替代旧 {@code UserAddressService}，
 * 通过 {@link IdentityApiConverter} 做旧 VO → Command、DTO → VO 转换，
 * 保持前端入参/出参结构不变。
 *
 * <p>创建人：@author WNJ
 */
@Tag(name = "收货地址管理", description = "收货地址 CRUD 与设置默认")
@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER','ADMIN')")
public class UserAddressController {

    private final AddressApi addressApi;
    private final SecurityUtils securityUtils;

    @Operation(summary = "查询当前用户地址列表")
    @GetMapping("/list")
    public Result<List<UserAddressVO>> list() {
        Long userId = securityUtils.getCurrentUserId();
        List<AddressDTO> dtoList = addressApi.listAddresses(userId);
        return Result.success(IdentityApiConverter.toUserAddressVOList(dtoList));
    }

    @Operation(summary = "新增收货地址")
    @PostMapping("/create")
    public Result<UserAddressVO> create(@Valid @RequestBody UserAddressVO vo) {
        Long userId = securityUtils.getCurrentUserId();
        SaveAddressCommand command = SaveAddressCommand.builder()
                .userId(userId)
                .receiverName(vo.getReceiverName())
                .receiverPhone(vo.getReceiverPhone())
                .province(vo.getProvince())
                .city(vo.getCity())
                .district(vo.getDistrict())
                .detailAddress(vo.getDetailAddress())
                .isDefault(vo.getIsDefault())
                .build();
        AddressDTO dto = addressApi.saveAddress(command);
        return Result.success("新增地址成功", IdentityApiConverter.toUserAddressVO(dto));
    }

    @Operation(summary = "编辑收货地址")
    @PutMapping("/{id}")
    public Result<UserAddressVO> update(@PathVariable Long id,
                                        @Valid @RequestBody UserAddressVO vo) {
        Long userId = securityUtils.getCurrentUserId();
        UpdateAddressCommand command = UpdateAddressCommand.builder()
                .userId(userId)
                .addressId(id)
                .receiverName(vo.getReceiverName())
                .receiverPhone(vo.getReceiverPhone())
                .province(vo.getProvince())
                .city(vo.getCity())
                .district(vo.getDistrict())
                .detailAddress(vo.getDetailAddress())
                .isDefault(vo.getIsDefault())
                .build();
        AddressDTO dto = addressApi.updateAddress(command);
        return Result.success("编辑地址成功", IdentityApiConverter.toUserAddressVO(dto));
    }

    @Operation(summary = "删除收货地址（逻辑删除）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        addressApi.deleteAddress(userId, id);
        return Result.<Void>success("删除地址成功", null);
    }

    @Operation(summary = "设置默认收货地址")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        addressApi.setDefaultAddress(userId, id);
        return Result.<Void>success("设置默认地址成功", null);
    }
}
