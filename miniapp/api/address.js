// api/address.js — 收货地址接口封装
//
// 集中封装收货地址相关 HTTP 调用，页面不直接拼装请求。
// 所有方法返回 Promise<Result<T>>（调用方取 .data）。
//
// 严格对齐后端：
//   - UserAddressVO { id, receiver, phone, province, city, district, detailAddress, isDefault }
//   - 地址 ID 全程 string（雪花 ID），URL 用 encodeURIComponent 防特殊字符
//
// 对齐：
//   - design.md 2.3 节
//   - spec.md 5.5 节（地址管理全部业务规则）

const { get, post, put, del } = require('../utils/request')
const { buildUrl } = require('../utils/id')

// ========== 接口端点常量 ==========
const API = {
    LIST: '/api/v1/addresses/list',     // GET 地址列表
    CREATE: '/api/v1/addresses/create', // POST 新增
    UPDATE: '/api/v1/addresses/',       // PUT /{id} 编辑
    DELETE: '/api/v1/addresses/',       // DELETE /{id} 删除
    SET_DEFAULT: '/api/v1/addresses/'   // PUT /{id}/default 设默认
}

/**
 * 获取地址列表
 * @returns {Promise<Result<Array<UserAddressVO>>>}
 */
function getAddressList() {
    return get(API.LIST)
}

/**
 * 新增地址
 * @param {object} data { receiver, phone, province, city, district, detailAddress, isDefault? }
 * @returns {Promise<Result<{id:string}>>}
 */
function createAddress(data) {
    return post(API.CREATE, data)
}

/**
 * 编辑地址
 * @param {string} id 地址 ID
 * @param {object} data { receiver, phone, province, city, district, detailAddress, isDefault? }
 * @returns {Promise<Result<void>>}
 */
function updateAddress(id, data) {
    const url = buildUrl(API.UPDATE, id)
    return put(url, data)
}

/**
 * 删除地址
 * @param {string} id 地址 ID
 * @returns {Promise<Result<void>>}
 */
function deleteAddress(id) {
    const url = buildUrl(API.DELETE, id)
    return del(url)
}

/**
 * 设默认地址
 * @param {string} id 地址 ID
 * @returns {Promise<Result<void>>}
 */
function setDefaultAddress(id) {
    const url = buildUrl(API.SET_DEFAULT, id) + '/default'
    return put(url)
}

module.exports = {
    getAddressList,
    createAddress,
    updateAddress,
    deleteAddress,
    setDefaultAddress
}