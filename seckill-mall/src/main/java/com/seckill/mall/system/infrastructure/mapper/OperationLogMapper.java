package com.seckill.mall.system.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.mall.system.infrastructure.entity.OperationLog;
import com.seckill.mall.vo.OperationLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    IPage<OperationLogVO> selectOperationLogVOPage(IPage<OperationLogVO> page,
                                                   @Param("module") String module,
                                                   @Param("operatorId") Long operatorId);

    /**
     * 非分页查询操作日志（用于 Excel 导出），按时间倒序，最多返回 limit 条。
     *
     * @param module 模块名筛选，null/空串表示不筛选
     * @param limit  最大返回条数
     * @return 操作日志 VO 列表
     */
    List<OperationLogVO> selectOperationLogVOList(@Param("module") String module,
                                                  @Param("limit") int limit);
}
