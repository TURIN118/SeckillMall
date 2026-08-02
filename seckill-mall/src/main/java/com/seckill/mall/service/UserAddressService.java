package com.seckill.mall.service;

import com.seckill.mall.vo.UserAddressVO;

import java.util.List;

/**
 * 收货地址服务接口
 * <p>
 * 提供收货地址的 CRUD 及设置默认地址能力，
 * 所有写操作均需校验地址归属当前用户。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserAddressService.java
 * 邮箱：nj651217@163.com
 */
public interface UserAddressService {

    /**
     * 查询指定用户的所有收货地址（按默认地址优先、更新时间倒序排列）。
     *
     * @param userId 用户 ID
     * @return 地址列表
     */
    List<UserAddressVO> listByUserId(Long userId);

    /**
     * 新增收货地址。
     * <p>
     * 若该用户此前没有任何地址，则自动将本条设为默认地址；
     * 若新增时 isDefault=1，则先取消该用户其他默认地址。
     *
     * @param userId 用户 ID
     * @param vo     地址视图对象（不含 id、userId、createTime）
     * @return 新增后的地址视图对象（含生成的 id）
     */
    UserAddressVO create(Long userId, UserAddressVO vo);

    /**
     * 编辑收货地址（校验归属当前用户）。
     * <p>
     * 若将 isDefault 由 0 改为 1，则先取消该用户其他默认地址。
     *
     * @param userId 用户 ID
     * @param id     地址 ID
     * @param vo     地址视图对象
     * @return 更新后的地址视图对象
     */
    UserAddressVO update(Long userId, Long id, UserAddressVO vo);

    /**
     * 删除收货地址（逻辑删除，校验归属当前用户）。
     *
     * @param userId 用户 ID
     * @param id     地址 ID
     */
    void delete(Long userId, Long id);

    /**
     * 设置默认地址。
     * <p>
     * 先将该用户所有地址的 is_default 置为 0，再将目标地址置为 1，
     * 操作在一个事务内完成，校验地址归属当前用户。
     *
     * @param userId 用户 ID
     * @param id     地址 ID
     */
    void setDefault(Long userId, Long id);
}