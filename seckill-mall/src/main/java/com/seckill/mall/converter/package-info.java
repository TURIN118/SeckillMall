/**
 * MapStruct 转换器包
 * <p>
 * M-D5 修复：启用 MapStruct 做 entity↔VO 转换，替代手工 setXxx。
 * 脱敏逻辑通过 {@link org.mapstruct.AfterMapping} 钩子实现。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：package-info.java
 * 邮箱：nj651217@163.com
 */
package com.seckill.mall.converter;