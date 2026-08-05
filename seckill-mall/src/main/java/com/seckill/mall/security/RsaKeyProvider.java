package com.seckill.mall.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 密钥对加载器。
 * 从 PEM 文件加载 RS256 签名所需的私钥和验证所需的公钥。
 */
@Slf4j
@Component
public class RsaKeyProvider {

    @Value("${jwt.rsa.private-key-path:classpath:keys/private.pem}")
    private String privateKeyPath;

    @Value("${jwt.rsa.public-key-path:classpath:keys/public.pem}")
    private String publicKeyPath;

    /**
     * 加载 RSA 私钥（PKCS#8 格式）
     */
    public RSAPrivateKey loadPrivateKey() {
        try {
            String pemContent = readPemResource(privateKeyPath);
            byte[] keyBytes = parsePem(pemContent, "PRIVATE KEY");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("加载 RSA 私钥失败: " + e.getMessage(), e);
        }
    }

    /**
     * 加载 RSA 公钥（X.509 格式）
     */
    public RSAPublicKey loadPublicKey() {
        try {
            String pemContent = readPemResource(publicKeyPath);
            byte[] keyBytes = parsePem(pemContent, "PUBLIC KEY");
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("加载 RSA 公钥失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取 PEM 资源文件内容
     */
    private String readPemResource(String path) throws Exception {
        if (path.startsWith("classpath:")) {
            String classpath = path.substring("classpath:".length());
            Resource resource = new ClassPathResource(classpath);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } else {
            // 绝对路径
            java.io.File file = new java.io.File(path);
            try (InputStream is = new java.io.FileInputStream(file)) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    /**
     * 解析 PEM 文件，提取指定 label 之间的 Base64 编码内容并解码
     */
    private byte[] parsePem(String pemContent, String label) {
        String beginMarker = "-----BEGIN " + label + "-----";
        String endMarker = "-----END " + label + "-----";
        int beginIndex = pemContent.indexOf(beginMarker);
        int endIndex = pemContent.indexOf(endMarker);
        if (beginIndex < 0 || endIndex < 0) {
            throw new IllegalArgumentException("PEM 文件中未找到 " + label + " 标记");
        }
        String base64 = pemContent.substring(beginIndex + beginMarker.length(), endIndex)
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}