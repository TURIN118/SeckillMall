package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.mall.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OperationLogMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    IPage<OperationLog> selectOperationLogPage(IPage<OperationLog> page,
                                               @Param("module") String module,
                                               @Param("operatorId") Long operatorId);
}
