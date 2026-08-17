package com.seckill.mall.identity.api;

import com.seckill.mall.identity.api.command.SaveAddressCommand;
import com.seckill.mall.identity.api.command.UpdateAddressCommand;
import com.seckill.mall.identity.api.dto.AddressDTO;

import java.util.List;

/**
 * Identity 模块收货地址能力 API。
 *
 * <p>对外暴露收货地址 CRUD + 设置默认地址 + 跨模块只读查询等契约。
 *
 * <p>设计原则：
 * <ul>
 *     <li>方法用业务语言命名，禁止 CRUD 命名</li>
 *     <li>入参用 Command 对象，禁止裸露多参数</li>
 *     <li>出参用 DTO，禁止暴露 Entity/Mapper/PO</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出，不泄露堆栈</li>
 * </ul>
 *
 * @author wnj
 * @since Phase I.2
 */
public interface AddressApi {

    /**
     * 查询指定用户的所有收货地址（按默认地址优先、更新时间倒序排列）。
     *
     * @param userId 用户 ID
     * @return 地址列表
     */
    List<AddressDTO> listAddresses(Long userId);

    /**
     * 根据地址 ID 查询地址（跨模块只读访问）。
     *
     * @param addressId 地址 ID
     * @return 地址 DTO（不存在返回 null）
     */
    AddressDTO getAddressById(Long addressId);

    /**
     * 新增收货地址。
     *
     * @param command 保存地址命令
     * @return 含生成的 id 的地址 DTO
     * @throws com.seckill.mall.exception.BusinessException {@code PARAM_ERROR}
     */
    AddressDTO saveAddress(SaveAddressCommand command);

    /**
     * 编辑收货地址。
     *
     * @param command 更新地址命令
     * @return 更新后的地址 DTO
     * @throws com.seckill.mall.exception.BusinessException {@code ADDRESS_NOT_FOUND}、{@code ADDRESS_NOT_BELONG_TO_USER}、{@code PARAM_ERROR}
     */
    AddressDTO updateAddress(UpdateAddressCommand command);

    /**
     * 逻辑删除收货地址。
     *
     * @param userId    用户 ID
     * @param addressId 地址 ID
     * @throws com.seckill.mall.exception.BusinessException {@code ADDRESS_NOT_FOUND}、{@code ADDRESS_NOT_BELONG_TO_USER}
     */
    void deleteAddress(Long userId, Long addressId);

    /**
     * 设置默认地址（先将该用户所有地址 is_default 置 0，再将目标置 1，事务内完成）。
     *
     * @param userId    用户 ID
     * @param addressId 地址 ID
     * @throws com.seckill.mall.exception.BusinessException {@code ADDRESS_NOT_FOUND}、{@code ADDRESS_NOT_BELONG_TO_USER}
     */
    void setDefaultAddress(Long userId, Long addressId);
}