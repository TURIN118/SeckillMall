/**
 * 防重放保护工具（B2 重构 + H-F1 修复）
 *
 * === 架构变更说明 (B2) ===
 * 旧方案: 前端持有 HMAC-SHA256 共享密钥 (VITE_SIGN_SECRET), 对每个秒杀请求生成签名头
 *         (X-Sign/X-Timestamp/X-Nonce). 该方案在 SPA 中不可成立: VITE_ 变量在构建期内联进
 *         JS bundle, 浏览器无法"保管"共享密钥, 攻击者可轻易从 bundle 中提取密钥伪造签名.
 * 新方案: 废弃前端签名, 改用服务端下发的一次性短时效 token (项目已有 getSeckillToken() 接口).
 *         前端仅在秒杀请求中携带后端签发的 X-Seckill-Token 头, 由后端 ReplayProtectionFilter
 *         校验 token 的合法性、时效性与一次性使用.
 *         前端不再需要任何共享密钥, 也不再有 VITE_SIGN_SECRET 配置项.
 *
 * === H-F1 修复 ===
 * 旧方案在生产构建下 VITE_SIGN_SECRET 为空, if (!SIGN_SECRET) return {} 被 tree-shake 消除,
 * 导致 ReplayProtectionFilter 强制要求的三件套缺失, 秒杀 100% 401.
 * 新方案下前端不再生成签名头, 而是携带后端 token, 不存在 tree-shake 问题.
 *
 * === 兼容性 ===
 * 保留 generateReplayHeaders 导出 (返回空对象), 避免调用方 (api/seckill.ts) 立即破坏.
 * 调用方应逐步迁移到直接使用 X-Seckill-Token 头.
 */

/**
 * 生成防重放请求头（已废弃，B2 重构后改为服务端 token 方案）
 *
 * 旧实现: 使用 VITE_SIGN_SECRET 生成 HMAC-SHA256 签名头
 * 新实现: 返回空对象, 防重放改由后端签发的 X-Seckill-Token 头承担
 *
 * @param _uri 请求 URI 路径（保留参数兼容旧调用方，实际不再使用）
 * @returns 空对象（不再添加签名头）
 *
 * @example
 * // 旧调用方式（仍兼容，但不再生成签名头）
 * const headers = await generateReplayHeaders('/api/v1/seckill/123/execute')
 * await post(uri, undefined, { headers })
 *
 * // 新调用方式（推荐，由调用方直接传 X-Seckill-Token）
 * await post(uri, undefined, { headers: { 'X-Seckill-Token': seckillToken } })
 */
export async function generateReplayHeaders(_uri: string): Promise<Record<string, string>> {
    // B2 重构: 前端不再生成 HMAC 签名, 防重放由后端签发的 token 承担.
    // 返回空对象, 保持向后兼容 (调用方解构后不会添加任何头).
    return {}
}

/**
 * 构建秒杀请求头（B2 重构后的推荐方式）
 *
 * 携带后端 getSeckillToken() 接口签发的一次性 token,
 * 由后端 ReplayProtectionFilter 校验 token 的合法性、时效性与一次性使用.
 *
 * @param seckillToken 后端签发的秒杀一次性 token
 * @returns 包含 X-Seckill-Token 的请求头对象
 *
 * @example
 * const tokenRes = await getSeckillToken(seckillId)
 * const headers = buildSeckillHeaders(tokenRes.data)
 * await post(uri, undefined, { headers })
 */
export function buildSeckillHeaders(seckillToken: string): Record<string, string> {
    if (!seckillToken) {
        // H-F1 修复: fail-fast, 不再静默降级返回空对象
        // 抛错让调用方立即感知 token 缺失, 避免请求发出后被后端 401 拒绝
        throw new Error('[replayProtection] 秒杀 token 缺失, 请先调用 getSeckillToken() 获取服务端签发的 token')
    }
    return {
        'X-Seckill-Token': seckillToken
    }
}
