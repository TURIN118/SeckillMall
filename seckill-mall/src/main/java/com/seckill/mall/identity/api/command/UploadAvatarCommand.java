package com.seckill.mall.identity.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * 上传头像命令。
 *
 * <p>业务语义：上传当前登录用户的头像，并持久化头像 URL。
 *
 * <p>原方法：{@code AuthService.uploadAvatar(MultipartFile)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadAvatarCommand {

    /** 头像文件（image/jpeg、image/png、image/webp，最大 2MB，必填） */
    private MultipartFile file;
}