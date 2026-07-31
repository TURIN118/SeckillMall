package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    User findByUsername(@Param("username") String username);

    User findByPhone(@Param("phone") String phone);
}
