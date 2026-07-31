package com.seckill.mall.aspect;

import com.seckill.mall.entity.OperationLog;
import com.seckill.mall.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OperationLogRecorder.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogRecorder {

    private final OperationLogMapper operationLogMapper;

    /**
     * 异步落库操作日志，内部兜底异常避免影响主流程。
     */
    @Async
    public void record(OperationLog operationLog) {
        try {
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.warn("操作日志写入失败: module={}, action={}, targetId={}",
                    operationLog.getModule(), operationLog.getAction(), operationLog.getTargetId(), e);
        }
    }
}
