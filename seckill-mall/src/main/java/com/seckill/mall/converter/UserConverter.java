package com.seckill.mall.converter;

import com.seckill.mall.entity.User;
import com.seckill.mall.vo.UserVO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * 用户 entity ↔ VO 转换器（MapStruct）
 * <p>
 * M-D5 修复：启用 MapStruct 替代 {@code UserServiceImpl.toUserVO} 手工 setXxx。
 * <ul>
 *   <li>{@code password} 字段不映射（脱敏，且 VO 无此字段）</li>
 *   <li>{@code avatarUrl} → {@code avatar} 字段名映射</li>
 *   <li>{@code role}/{@code status} 枚举通过 {@link AfterMapping} 转为 code</li>
 *   <li>手机号/邮箱脱敏在 {@link AfterMapping} 中完成（M31 安全说明）</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserConverter.java
 * 邮箱：nj651217@163.com
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    /** 便捷的静态访问器（与 Spring Bean 共存，便于单元测试） */
    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

    /**
     * entity → VO
     * <p>
     * role/status 枚举在 {@link AfterMapping} 中处理，
     * phone/email 脱敏也在 {@link AfterMapping} 中完成。
     */
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(source = "avatarUrl", target = "avatar")
    UserVO toVO(User entity);

    /**
     * 后置映射：枚举→code，phone/email 脱敏。
     * <p>
     * 脱敏逻辑内联于此方法，不暴露为独立 default 方法，
     * 避免 MapStruct 将其误识别为 String→String 候选映射方法导致歧义。
     */
    @AfterMapping
    default void enrichAndMask(User entity, @MappingTarget UserVO vo) {
        if (entity.getRole() != null) {
            vo.setRole(entity.getRole().getCode());
        }
        if (entity.getStatus() != null) {
            vo.setStatus(entity.getStatus().getCode());
        }
        // M31 安全说明：手机号脱敏（如 138****8000）
        vo.setPhone(maskPhoneInternal(entity.getPhone()));
        // M31 安全说明：邮箱脱敏（如 w***@ex.com）
        vo.setEmail(maskEmailInternal(entity.getEmail()));
    }

    /** 手机号脱敏：保留前 3 后 4（私有辅助，不作为 MapStruct 映射方法） */
    private static String maskPhoneInternal(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 邮箱脱敏：保留首字符 + 域名（私有辅助，不作为 MapStruct 映射方法） */
    private static String maskEmailInternal(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
