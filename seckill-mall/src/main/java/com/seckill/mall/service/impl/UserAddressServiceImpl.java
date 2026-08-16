package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.entity.UserAddress;
import com.seckill.mall.mapper.UserAddressMapper;
import com.seckill.mall.service.UserAddressService;
import com.seckill.mall.vo.UserAddressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 收货地址服务实现
 * <p>
 * 基于 {@link UserAddressMapper} 进行 CRUD，使用 {@link LambdaQueryWrapper} /
 * {@link LambdaUpdateWrapper} 构造条件。所有写操作均校验地址归属当前用户，
 * 设置默认地址时先取消该用户其他默认地址，再设目标地址为默认。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserAddressServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    /** 默认地址标识：1=是默认 */
    private static final int DEFAULT_FLAG = 1;
    /** 非默认地址标识：0=非默认 */
    private static final int NON_DEFAULT_FLAG = 0;

    private final UserAddressMapper userAddressMapper;

    @Override
    public List<UserAddressVO> listByUserId(Long userId) {
        List<UserAddress> entities = userAddressMapper.selectByUserId(userId);
        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAddressVO create(Long userId, UserAddressVO vo) {
        UserAddress entity = new UserAddress();
        entity.setUserId(userId);
        entity.setReceiverName(vo.getReceiverName());
        entity.setReceiverPhone(vo.getReceiverPhone());
        entity.setProvince(vo.getProvince());
        entity.setCity(vo.getCity());
        entity.setDistrict(vo.getDistrict());
        entity.setDetailAddress(vo.getDetailAddress());

        // 判断是否为用户首个地址：若是则强制设为默认
        Long existCount = userAddressMapper.selectCount(
                new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId));
        boolean isFirst = existCount == null || existCount == 0L;

        Integer isDefault = vo.getIsDefault();
        if (isFirst) {
            // 首个地址自动设为默认
            entity.setIsDefault(DEFAULT_FLAG);
        } else if (isDefault != null && isDefault == DEFAULT_FLAG) {
            // 显式设为默认：先取消该用户其他默认地址
            clearDefaultForUser(userId);
            entity.setIsDefault(DEFAULT_FLAG);
        } else {
            entity.setIsDefault(NON_DEFAULT_FLAG);
        }

        userAddressMapper.insert(entity);
        log.info("新增收货地址成功，id={}, userId={}", entity.getId(), userId);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAddressVO update(Long userId, Long id, UserAddressVO vo) {
        UserAddress entity = getOwnedAddress(userId, id);

        if (vo.getReceiverName() != null) {
            entity.setReceiverName(vo.getReceiverName());
        }
        if (vo.getReceiverPhone() != null) {
            entity.setReceiverPhone(vo.getReceiverPhone());
        }
        if (vo.getProvince() != null) {
            entity.setProvince(vo.getProvince());
        }
        if (vo.getCity() != null) {
            entity.setCity(vo.getCity());
        }
        if (vo.getDistrict() != null) {
            entity.setDistrict(vo.getDistrict());
        }
        if (vo.getDetailAddress() != null) {
            entity.setDetailAddress(vo.getDetailAddress());
        }

        // 处理默认地址切换
        Integer isDefault = vo.getIsDefault();
        if (isDefault != null) {
            if (isDefault == DEFAULT_FLAG && (entity.getIsDefault() == null || entity.getIsDefault() != DEFAULT_FLAG)) {
                // 由非默认改为默认：先取消其他默认地址（排除当前 id），再设当前为默认
                clearDefaultForUser(userId, id);
                entity.setIsDefault(DEFAULT_FLAG);
            } else if (isDefault == NON_DEFAULT_FLAG) {
                entity.setIsDefault(NON_DEFAULT_FLAG);
            }
        }

        userAddressMapper.updateById(entity);
        log.info("编辑收货地址成功，id={}, userId={}", id, userId);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        // 校验归属（同时确认地址存在且未删除）
        getOwnedAddress(userId, id);
        // 逻辑删除（@TableLogic 自动处理 is_deleted 字段）
        userAddressMapper.deleteById(id);
        log.info("删除收货地址成功，id={}, userId={}", id, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long id) {
        // 校验归属
        getOwnedAddress(userId, id);
        // 1. 取消该用户所有默认地址
        clearDefaultForUser(userId);
        // 2. 设目标地址为默认
        UserAddress toUpdate = new UserAddress();
        toUpdate.setId(id);
        toUpdate.setIsDefault(DEFAULT_FLAG);
        userAddressMapper.updateById(toUpdate);
        log.info("设置默认收货地址成功，id={}, userId={}", id, userId);
    }

    /**
     * 获取指定用户拥有的地址（校验存在 + 归属），否则抛业务异常。
     *
     * @param userId 用户 ID
     * @param id     地址 ID
     * @return 地址实体
     */
    private UserAddress getOwnedAddress(Long userId, Long id) {
        UserAddress entity = userAddressMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        if (!userId.equals(entity.getUserId())) {
            // 地址不属于当前用户：出于安全考虑统一返回"不存在"，避免泄露存在性
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        return entity;
    }

    /**
     * 取消指定用户所有地址的默认标记（不排除任何 id）。
     *
     * @param userId 用户 ID
     */
    private void clearDefaultForUser(Long userId) {
        clearDefaultForUser(userId, null);
    }

    /**
     * 取消指定用户所有地址的默认标记，可排除某个 id（如正在编辑/设默认的那条）。
     *
     * @param userId     用户 ID
     * @param excludeId  需排除的地址 ID，可为 null 表示不排除
     */
    private void clearDefaultForUser(Long userId, Long excludeId) {
        LambdaUpdateWrapper<UserAddress> wrapper = new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, DEFAULT_FLAG)
                .set(UserAddress::getIsDefault, NON_DEFAULT_FLAG);
        if (excludeId != null) {
            wrapper.ne(UserAddress::getId, excludeId);
        }
        userAddressMapper.update(null, wrapper);
    }

    /**
     * 实体 → 视图对象
     */
    private UserAddressVO toVO(UserAddress entity) {
        UserAddressVO vo = new UserAddressVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setReceiverName(entity.getReceiverName());
        vo.setReceiverPhone(entity.getReceiverPhone());
        vo.setProvince(entity.getProvince());
        vo.setCity(entity.getCity());
        vo.setDistrict(entity.getDistrict());
        vo.setDetailAddress(entity.getDetailAddress());
        vo.setIsDefault(entity.getIsDefault());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    // ==================== Phase 7：跨模块内部调用入口（供 OrderServiceImpl 使用） ====================

    @Override
    public UserAddress getAddressById(Long addressId) {
        return userAddressMapper.selectById(addressId);
    }
}