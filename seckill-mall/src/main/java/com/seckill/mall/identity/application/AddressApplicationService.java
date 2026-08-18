package com.seckill.mall.identity.application;

import com.seckill.mall.identity.api.AddressApi;
import com.seckill.mall.identity.api.command.SaveAddressCommand;
import com.seckill.mall.identity.api.command.UpdateAddressCommand;
import com.seckill.mall.identity.api.dto.AddressDTO;
import com.seckill.mall.identity.application.facade.IdentityApiConverter;
import com.seckill.mall.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Address 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link AddressApi}，内部委托给旧 {@link UserAddressService}，
 * 通过 {@link IdentityApiConverter} 做 VO/Entity ↔ DTO 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做参数转换、委托和结果转换。
 *
 * @author wnj
 * @since Phase I.4-A
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressApplicationService implements AddressApi {

    private final UserAddressService userAddressService;

    @Override
    public List<AddressDTO> listAddresses(Long userId) {
        return IdentityApiConverter.toAddressDTOListFromVO(userAddressService.listByUserId(userId));
    }

    @Override
    public AddressDTO getAddressById(Long addressId) {
        return IdentityApiConverter.toAddressDTO(userAddressService.getAddressById(addressId));
    }

    @Override
    public AddressDTO saveAddress(SaveAddressCommand command) {
        return IdentityApiConverter.toAddressDTOFromVO(
                userAddressService.create(command.getUserId(),
                        IdentityApiConverter.toUserAddressVO(command)));
    }

    @Override
    public AddressDTO updateAddress(UpdateAddressCommand command) {
        return IdentityApiConverter.toAddressDTOFromVO(
                userAddressService.update(command.getUserId(), command.getAddressId(),
                        IdentityApiConverter.toUserAddressVO(command)));
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        userAddressService.delete(userId, addressId);
    }

    @Override
    public void setDefaultAddress(Long userId, Long addressId) {
        userAddressService.setDefault(userId, addressId);
    }
}