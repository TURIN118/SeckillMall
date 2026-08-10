// api/wallet.js — 钱包接口封装（usercenter 模块）
//
// 集中封装钱包相关 HTTP 调用，页面不直接拼装请求。
// 所有方法返回 Promise<Result<T>>（调用方取 .data）。
//
// 严格对齐后端：
//   - GET  /api/v1/wallet/balance   余额（BigDecimal）
//   - POST /api/v1/wallet/recharge  充值（WalletRechargeRequest: cardNo/cardPassword）
//   - GET  /api/v1/wallet/records   交易记录（List<WalletRecordVO>）
//
// 注意：
//   - 余额为 BigDecimal，后端可能以 number 或 string 返回，展示时 formatPrice 格式化
//   - 均需登录态（请求拦截器自动注入 Authorization）
//
// 对齐：
//   - .codeartsdoer/specs/usercenter/spec.md 4 节
//   - .codeartsdoer/specs/usercenter/design.md 4 节
//   - 后端 WalletController.java 端点

const { get, post } = require('../utils/request')

// ========== 接口端点常量 ==========
const API = {
    BALANCE: '/api/v1/wallet/balance',  // GET 余额
    RECHARGE: '/api/v1/wallet/recharge', // POST 充值
    RECORDS: '/api/v1/wallet/records'   // GET 交易记录
}

/**
 * 查询钱包余额
 * @returns {Promise<Result<BigDecimal>>} res.data 为余额（number|string）
 */
function getBalance() {
    return get(API.BALANCE)
}

/**
 * 充值（充值卡）
 * @param {string} cardNo 充值卡号
 * @param {string} cardPassword 充值卡密码
 * @returns {Promise<Result<BigDecimal>>} res.data 为新余额
 */
function recharge(cardNo, cardPassword) {
    return post(API.RECHARGE, { cardNo: cardNo, cardPassword: cardPassword })
}

/**
 * 查询交易记录
 * @returns {Promise<Result<Array<WalletRecordVO>>>}
 */
function listRecords() {
    return get(API.RECORDS)
}

module.exports = {
    getBalance,
    recharge,
    listRecords
}