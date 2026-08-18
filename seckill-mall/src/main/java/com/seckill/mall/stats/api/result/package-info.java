/**
 * Stats API 结果对象 - 操作返回值契约。
 *
 * <p>预留结果对象包。当前 StatsApi 所有方法均返回 DTO 或 List&lt;DTO&gt;，无需 Result 包装。
 * 未来若需扩展复合结果（如带分页信息的趋势结果），可在此包定义。
 *
 * @author wnj
 * @since Phase ST.0
 */
package com.seckill.mall.stats.api.result;