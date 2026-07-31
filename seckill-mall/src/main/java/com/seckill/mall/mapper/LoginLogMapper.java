package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：LoginLogMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {

    List<LoginLog> selectByUserId(@Param("userId") Long userId);
}
