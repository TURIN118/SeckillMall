package com.seckill.mall.controller;

import com.seckill.mall.common.Result;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.UserAddressService;
import com.seckill.mall.vo.UserAddressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收货地址管理控制器
 * <p>
 * 前缀 {@code /api/v1/addresses}，需登录且角色为 BUYER 或 ADMIN。
 * 当前用户 ID 通过 {@link SecurityUtils#getCurrentUserId()} 获取，
 * 用户仅能操作属于自己的地址。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserAddressController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "收货地址管理", description = "收货地址 CRUD 与设置默认")
@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER','ADMIN')")
public class UserAddressController {

    private final UserAddressService userAddressService;

    @Operation(summary = "查询当前用户地址列表")
    @GetMapping("/list")
    public Result<List<UserAddressVO>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userAddressService.listByUserId(userId));
    }

    @Operation(summary = "新增收货地址")
    @PostMapping("/create")
    public Result<UserAddressVO> create(@Valid @RequestBody UserAddressVO vo) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success("新增地址成功", userAddressService.create(userId, vo));
    }

    @Operation(summary = "编辑收货地址")
    @PutMapping("/{id}")
    public Result<UserAddressVO> update(@PathVariable Long id,
                                        @Valid @RequestBody UserAddressVO vo) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success("编辑地址成功", userAddressService.update(userId, id, vo));
    }

    @Operation(summary = "删除收货地址（逻辑删除）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        userAddressService.delete(userId, id);
        return Result.<Void>success("删除地址成功", null);
    }

    @Operation(summary = "设置默认收货地址")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        userAddressService.setDefault(userId, id);
        return Result.<Void>success("设置默认地址成功", null);
    }
}