// config/constants.js — 业务常量
//
// 集中维护业务码、存储键名、订单状态枚举，禁止在各业务模块分散硬编码。
// 对齐：spec.md 6.1 节、design.md 2.2 节、主计划 4.6 节

module.exports = {
    // ========== 业务码 ==========
    CODE_SUCCESS: 200,            // 成功
    CODE_UNAUTHORIZED: 1002,      // 访问令牌过期，需刷新
    CODE_REPLAY_DETECTED: 1011,   // 秒杀防重放拦截，不刷新仅提示

    // ========== 存储键名（全局唯一，禁止重复） ==========
    ACCESS_TOKEN_KEY: 'access_token',
    REFRESH_TOKEN_KEY: 'refresh_token',
    USER_INFO_KEY: 'user_info',
    TIME_OFFSET_KEY: 'time_offset',

    // ========== 订单状态枚举 ==========
    ORDER_STATUS: {
        PENDING_PAY: 0,      // 待付款
        PENDING_SHIP: 1,     // 待发货
        PENDING_RECEIVE: 2,  // 待收货
        COMPLETED: 3,        // 已完成
        CANCELLED: 4         // 已取消
    },

    // ========== 订单状态文本映射（便于 UI 直接展示） ==========
    ORDER_STATUS_TEXT: {
        0: '待付款',
        1: '待发货',
        2: '待收货',
        3: '已完成',
        4: '已取消'
    },

    // ========== HTTP 状态码 ==========
    HTTP_STATUS: {
        UNAUTHORIZED: 401,
        FORBIDDEN: 403,
        TOO_MANY_REQUESTS: 429,
        INTERNAL_SERVER_ERROR: 500
    },

    // ========== API 路径前缀 ==========
    API_PREFIX: '/api/v1',
    REFRESH_TOKEN_URL: '/api/v1/auth/refresh'
}