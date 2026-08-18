/**
 * System API 结果对象 - 操作返回值契约。
 *
 * <p>预留结果对象包。当前 SystemApi 所有方法均返回 DTO 或 List&lt;DTO&gt; 或 PageResult&lt;DTO&gt;，无需额外 Result 包装。
 * 未来若需扩展复合结果，可在此包定义。
 *
 * @author wnj
 * @since Phase SY.0
 */
package com.seckill.mall.system.api.result;