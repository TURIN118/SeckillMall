/**
 * 秒杀防重放工具（对齐 plan.md 第 4.5 节 / spec.md 2.5）
 * 构建一次性 X-Seckill-Token 请求头
 * 流程：GET /seckill/{id}/token 获取一次性 token → POST /seckill/{id}/execute 携带此头
 */

/**
 * 构建秒杀防重放请求头
 * @param seckillToken 一次性秒杀 token（由 GET /seckill/{id}/token 获取）
 * @returns { 'X-Seckill-Token': seckillToken }
 */
export function buildSeckillHeaders(seckillToken: string): Record<string, string> {
  if (!seckillToken) {
    throw new Error('秒杀 token 缺失，请先获取秒杀资格')
  }
  return {
    'X-Seckill-Token': seckillToken
  }
}

/**
 * 构建秒杀防重放请求头（安全版，不抛异常）
 * @param seckillToken 一次性秒杀 token
 * @returns 请求头对象，token 缺失时返回空对象
 */
export function buildSeckillHeadersSafe(seckillToken: string): Record<string, string> {
  if (!seckillToken) {
    console.warn('秒杀 token 缺失')
    return {}
  }
  return {
    'X-Seckill-Token': seckillToken
  }
}