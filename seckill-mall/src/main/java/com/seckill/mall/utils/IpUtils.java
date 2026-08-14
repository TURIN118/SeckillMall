package com.seckill.mall.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：IpUtils.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
public final class IpUtils {

    /** 默认可信代理白名单 */
    private static final Set<String> DEFAULT_TRUSTED_PROXY_IPS = Set.of("127.0.0.1", "0:0:0:0:0:0:0:1");

    private IpUtils() {}

    /**
     * 从 HttpServletRequest 获取客户端真实IP（使用默认可信代理白名单）。
     * 算法：取 remoteAddr，若在可信代理白名单则从 X-Forwarded-For 从右向左取第一个非可信IP，否则用 X-Real-IP，否则用 remoteAddr。
     * @param request HttpServletRequest
     * @return 真实IP，无法获取时返回 "unknown"
     */
    public static String getClientIp(HttpServletRequest request) {
        return getClientIp(request, null);
    }

    /**
     * 从 RequestContextHolder 获取客户端真实IP（供 Aspect/Service 使用，无 request 参数时）。
     * @return 真实IP，无法获取时返回 "unknown"
     */
    public static String getClientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        return getClientIp(attrs.getRequest());
    }

    /**
     * 从 HttpServletRequest 获取客户端真实IP（支持自定义可信代理白名单）。
     * 算法：
     * 1. 取 request.getRemoteAddr() 作为直连对端地址；
     * 2. 若直连对端是可信代理，则从 X-Forwarded-For 从右向左取第一个非可信 IP（防代理链伪造）；
     * 3. 若无 X-Forwarded-For 则取 X-Real-IP；
     * 4. 否则直接使用 remoteAddr。
     * @param request HttpServletRequest
     * @param trustedProxyIps 可信代理IP白名单（null 时使用默认白名单）
     * @return 真实IP，无法获取时返回 "unknown"
     */
    public static String getClientIp(HttpServletRequest request, Set<String> trustedProxyIps) {
        if (request == null) {
            return "unknown";
        }
        Set<String> trusted = trustedProxyIps == null ? DEFAULT_TRUSTED_PROXY_IPS : trustedProxyIps;
        String remoteAddr = request.getRemoteAddr();
        // 仅当直连对端是可信代理时才信任 forwarded 头
        if (trusted.contains(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(xff) && !"unknown".equalsIgnoreCase(xff)) {
                // 从右向左取第一个非可信 IP，防代理链伪造
                String[] parts = xff.split(",");
                for (int i = parts.length - 1; i >= 0; i--) {
                    String candidate = parts[i].trim();
                    if (StringUtils.hasText(candidate) && !"unknown".equalsIgnoreCase(candidate)
                            && !trusted.contains(candidate)) {
                        return candidate;
                    }
                }
                // 全是可信代理 IP，取最左侧（最原始的客户端 IP）
                return parts[0].trim();
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (StringUtils.hasText(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp)) {
                return xRealIp.trim();
            }
        }
        // 非可信代理直连，或无 forwarded 头：直接使用 remoteAddr
        return remoteAddr;
    }
}