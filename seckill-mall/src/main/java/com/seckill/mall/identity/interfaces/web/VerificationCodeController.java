package com.seckill.mall.identity.interfaces.web;

import com.seckill.mall.annotation.RateLimit;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.Result;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.VerificationCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 验证码 Controller
 * <p>
 * 前缀 {@code /api/v1/verification}，发送与校验接口均可匿名访问
 * （在 {@code SecurityConfig} 白名单中配置）。
 * <p>
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：VerificationCodeController.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Tag(name = "验证码", description = "邮箱/短信验证码发送与校验")
@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
public class VerificationCodeController {

    /**
     * 邮箱格式正则
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.-]+@[\\w.-]+\\.\\w+$");
    /**
     * 手机号格式正则：11 位数字、首位为 1
     */
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^1\\d{10}$");

    /**
     * C2 修复：按目标维度的每日发送上限 key 前缀
     */
    private static final String DAILY_LIMIT_KEY_PREFIX = "verify:daily:";
    /**
     * C2 修复：单个目标每日最多发送次数（防短信轰炸/费用泵）
     */
    private static final int DAILY_LIMIT_PER_TARGET = 10;
    /**
     * 每日限额窗口：24 小时（秒）
     */
    private static final long DAILY_WINDOW_SECONDS = 24L * 60L * 60L;

    /**
     * 问题5修复：INCR + EXPIRE 原子化 Lua 脚本，避免 EXPIRE 失败导致 key 永不过期。
     * <p>
     * KEYS[1] = 限流 key
     * ARGV[1] = 过期秒数（24h）
     * 返回值：当前计数（INCR 后的值）
     */
    private static final DefaultRedisScript<Long> DAILY_LIMIT_LUA = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]) "
                    + "if count == 1 then "
                    + "  redis.call('EXPIRE', KEYS[1], ARGV[1]) "
                    + "end "
                    + "return count",
            Long.class);

    private final VerificationCodeService verificationCodeService;
    private final StringRedisTemplate stringRedisTemplate;
    private final SecurityUtils securityUtils;

    /**
     * C2 修复：单个目标（手机号/邮箱）每日发送总量上限，防费用泵/短信轰炸。
     * 配置项允许外部覆盖默认值 10。
     */
    @Value("${seckill.security.verify-daily-limit-per-target:10}")
    private int dailyLimitPerTarget;

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/send-email")
    // 安全修复（M11/C3）：限流防暴力破解，60s 内同一 key 仅允许 1 次（seconds=60 现已生效）
    // Bug1修复：capacity 从 1 调整为 3，允许突发 3 次（应对重试/误操作），
    // 避免 Redis 中残留已消耗但未过期的令牌导致首次点击就 429
    @RateLimit(key = "send-email", capacity = 3, rate = 3, seconds = 60)
    public Result<Void> sendEmail(@RequestBody Map<String, String> body) {
        // 安全修复（H7）：服务端强校验邮箱格式，避免空值/非法字符触发下游异常或被滥用
        // Bug7修复：前端统一使用 target 字段传参，后端需用 target 取值（原 email 字段已废弃）
        String target = body == null ? null : body.get("target");
        String email = null;
        if (Objects.equals(target, "发送修改密码验证码")) {
            email = securityUtils.getCurrentEmail();

        } else {
            email = target;
        }
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            return Result.error(ErrorCode.PARAM_ERROR);
        }
        // C2 修复：按目标邮箱叠加每日总量限制
        if (!checkDailyLimit("email:" + email)) {
            log.warn("邮箱验证码每日上限触发 email={}", email);
            return Result.error(ErrorCode.VERIFICATION_CODE_RATE_LIMIT);
        }
        verificationCodeService.sendEmailCode(email);
        return Result.<Void>success("发送成功", null);
    }

    @Operation(summary = "发送短信验证码")
    @PostMapping("/send-sms")
    // 安全修复（M11/C3）：限流防暴力破解，60s 内同一 key 仅允许 1 次（seconds=60 现已生效）
    // Bug1修复：capacity 从 1 调整为 3，允许突发 3 次（应对重试/误操作），
    // 避免 Redis 中残留已消耗但未过期的令牌导致首次点击就 429
    @RateLimit(key = "send-sms", capacity = 3, rate = 3, seconds = 60)
    public Result<Void> sendSms(@RequestBody Map<String, String> body) {
        // 安全修复（H7）：服务端强校验手机号格式
        // Bug7修复：前端统一使用 target 字段传参，后端需用 target 取值（原 phone 字段已废弃）
        String phone = body == null ? null : body.get("target");
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            return Result.error(ErrorCode.PARAM_ERROR);
        }
        // C2 修复：按目标手机号叠加每日总量限制，防短信轰炸/费用泵
        if (!checkDailyLimit("sms:" + phone)) {
            log.warn("短信验证码每日上限触发 phone={}", phone);
            return Result.error(ErrorCode.VERIFICATION_CODE_RATE_LIMIT);
        }
        verificationCodeService.sendSmsCode(phone);
        return Result.<Void>success("发送成功", null);
    }

    @Operation(summary = "校验验证码")
    @PostMapping("/verify")
    // 安全修复（M11/C3）：限流防暴力枚举验证码，60s 内同一 key 仅允许 5 次（capacity=5, seconds=60）
    @RateLimit(key = "verify-code", capacity = 5, rate = 5, seconds = 60)
    public Result<Boolean> verify(@RequestBody Map<String, String> body) {
        String target = body == null ? null : body.get("target");
        String code = body == null ? null : body.get("code");
        if (target == null || target.isEmpty() || code == null || code.isEmpty()) {
            return Result.error(ErrorCode.PARAM_ERROR);
        }
        boolean ok = verificationCodeService.verifyCode(target, code);
        if (!ok) {
            return Result.error(ErrorCode.VERIFICATION_CODE_INVALID);
        }
        return Result.success(true);
    }

    /**
     * C2 修复：按目标维度检查每日发送上限。
     * <p>
     * 问题5修复：使用 Lua 脚本原子执行 INCR + EXPIRE，避免 INCR 成功但 EXPIRE 失败时
     * key 永不过期导致用户被永久限流。
     *
     * @param targetKey 目标标识（如 "sms:13800000000" / "email:a@b.com"）
     * @return true 未超限，允许发送；false 已超限，拒绝
     */
    private boolean checkDailyLimit(String targetKey) {
        String redisKey = DAILY_LIMIT_KEY_PREFIX + targetKey;
        // 原子 INCR + 首次 EXPIRE(24h)，避免非原子操作的不一致
        Long count = stringRedisTemplate.execute(
                DAILY_LIMIT_LUA,
                Collections.singletonList(redisKey),
                String.valueOf(DAILY_WINDOW_SECONDS));
        int limit = dailyLimitPerTarget > 0 ? dailyLimitPerTarget : DAILY_LIMIT_PER_TARGET;
        return count == null || count <= limit;
    }
}
