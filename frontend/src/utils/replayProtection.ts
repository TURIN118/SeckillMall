/**
 * 防重放签名工具（Web Crypto API 实现）
 * 对应后端 ReplayProtectionFilter 的签名校验逻辑
 * 签名算法：HMAC-SHA256(secret, timestamp + nonce + uri)
 *
 * 为什么用 Web Crypto API 而非 crypto-js：
 * 1. 零依赖：浏览器原生 API，无需安装 npm 包，减少打包体积 ~70KB
 * 2. 性能更优：调用 OS 级加密实现（OpenSSL/BoringSSL），单次 HMAC 约 0.05ms（crypto-js 约 0.5ms）
 * 3. 异步非阻塞：不阻塞主线程，对秒杀按钮点击的 UI 响应更友好
 * 4. 密钥对象不可导出：CryptoKey 默认 extractable=false，即使 XSS 获取 key 对象也无法 exportKey()
 *    （注意：原始密钥字符串仍存在于 JS 内存中，此保护主要防止通过 CryptoKey 接口导出）
 * 5. 标准化：W3C 标准 API，所有现代浏览器均支持
 */

/**
 * 签名密钥
 * 开发环境从 VITE_SIGN_SECRET 读取，生产环境从构建时环境变量注入
 * 必须与后端 seckill.security.sign-secret 配置一致
 */
const SIGN_SECRET: string = import.meta.env.VITE_SIGN_SECRET || ''

/** 缓存的 CryptoKey 对象（避免每次请求重复 importKey） */
let cachedKey: CryptoKey | null = null

if (!SIGN_SECRET) {
    console.warn('[replayProtection] VITE_SIGN_SECRET 未配置，防重放签名将不生效')
}

/**
 * 获取或创建 HMAC-SHA256 的 CryptoKey 对象（带缓存）
 * Web Crypto API 的 importKey 是异步操作，缓存后只需执行一次
 */
async function getSignKey(): Promise<CryptoKey> {
    if (cachedKey) return cachedKey

    const keyBytes = new TextEncoder().encode(SIGN_SECRET)
    cachedKey = await crypto.subtle.importKey(
        'raw',                    // 密钥格式：原始字节
        keyBytes,                 // 密钥数据
        { name: 'HMAC', hash: 'SHA-256' },  // 算法：HMAC-SHA256
        false,                    // extractable=false：密钥对象不可通过 exportKey() 导出
        ['sign']                  // 密钥用途：仅用于签名
    )
    return cachedKey
}

/**
 * 将 ArrayBuffer 转为十六进制小写字符串
 * 与后端 Java 的 HexFormat.of().formatHex() 输出格式一致
 */
function bufferToHex(buffer: ArrayBuffer): string {
    return Array.from(new Uint8Array(buffer))
        .map(byte => byte.toString(16).padStart(2, '0'))
        .join('')
}

/**
 * 生成防重放请求头（异步）
 * @param uri 请求 URI 路径（如 /api/v1/seckill/123/execute），不含 baseURL 和 query string
 * @returns 包含 X-Sign、X-Timestamp、X-Nonce 的请求头对象
 *
 * @example
 * // 调用方式（必须 await）
 * const headers = await generateReplayHeaders('/api/v1/seckill/123/execute')
 * await post(uri, undefined, { headers })
 */
export async function generateReplayHeaders(uri: string): Promise<Record<string, string>> {
    if (!SIGN_SECRET) {
        // 密钥未配置时返回空对象，不添加签名头（开发调试用）
        return {}
    }

    const timestamp = Date.now().toString()
    const nonce = crypto.randomUUID()
    const payload = timestamp + nonce + uri
    const payloadBytes = new TextEncoder().encode(payload)

    // 使用 Web Crypto API 计算 HMAC-SHA256 签名
    const key = await getSignKey()
    const signature = await crypto.subtle.sign('HMAC', key, payloadBytes)
    const sign = bufferToHex(signature)

    return {
        'X-Sign': sign,
        'X-Timestamp': timestamp,
        'X-Nonce': nonce
    }
}
