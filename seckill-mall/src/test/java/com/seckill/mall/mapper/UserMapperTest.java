package com.seckill.mall.mapper;

import com.seckill.mall.identity.domain.UserRole;
import com.seckill.mall.identity.domain.UserStatus;
import com.seckill.mall.identity.infrastructure.entity.User;
import com.seckill.mall.identity.infrastructure.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserMapperTest.java
 * 邮箱：nj651217@163.com
 */
@SpringBootTest(classes = MapperTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    private User buildUser(String username) {
        User user = new User();
        user.setId(System.currentTimeMillis());
        user.setUsername(username);
        user.setPassword("$2a$10$encoded");
        user.setPhone("138" + username.hashCode() % 100000000);
        user.setRole(UserRole.BUYER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    @Test
    @DisplayName("insert：写入用户后主键回填")
    void insert_shouldPersistUser() {
        // given
        User user = buildUser("mapper_test_1");

        // when
        int affected = userMapper.insert(user);

        // then
        assertThat(affected).isEqualTo(1);
        assertThat(user.getId()).isNotNull();
    }

    @Test
    @DisplayName("findByUsername：根据用户名精确查询未删除用户")
    void findByUsername_shouldReturnPersistedUser() {
        // given
        User user = buildUser("mapper_test_2");
        userMapper.insert(user);

        // when
        User found = userMapper.findByUsername("mapper_test_2");

        // then
        assertThat(found).isNotNull();
        assertThat(found.getUsername()).isEqualTo("mapper_test_2");
        assertThat(found.getRole()).isEqualTo(UserRole.BUYER);
        assertThat(found.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("findByUsername：查询不存在用户返回 null")
    void findByUsername_shouldReturnNullWhenAbsent() {
        // when
        User found = userMapper.findByUsername("not_exist_user_xyz");

        // then
        assertThat(found).isNull();
    }

    @Test
    @DisplayName("findByPhone：根据手机号查询用户")
    void findByPhone_shouldReturnUser() {
        // given
        User user = buildUser("mapper_test_3");
        user.setPhone("13900000099");
        userMapper.insert(user);

        // when
        User found = userMapper.findByPhone("13900000099");

        // then
        assertThat(found).isNotNull();
        assertThat(found.getUsername()).isEqualTo("mapper_test_3");
    }
}
