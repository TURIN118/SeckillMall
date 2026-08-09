// subpackages/user-center/review-submit/review-submit.js
// 发表评价：商品信息 + 评分 + 评论 + 图片选择上传
//
// 对齐：
//   - .codeartsdoer/specs/usercenter/spec.md 3.7 节
//   - .codeartsdoer/specs/usercenter/design.md 11 节
//   - .codeartsdoer/specs/usercenter/tasks.md U8
//
// 关键点：
//   1. query 携带 productId/skuId?/orderId?
//   2. 商品信息展示（product.getProductDetail）
//   3. van-rate 评分（1-5 星）
//   4. textarea 评论（最大 1000 字）
//   5. 图片：wx.chooseImage 多选（最多 5 张），循环 wx.uploadFile 上传
//   6. create 提交，images 字段 JSON.stringify(urls)
//   7. 成功后 toast 并返回上一页

const productApi = require('../../../api/product')
const reviewApi = require('../../../api/review')
const { isLoggedIn, navigateToLogin, getAccessToken } = require('../../../utils/auth')
const { BASE_URL } = require('../../../config/env')

// 最大图片数
const MAX_IMAGES = 5
// 评论最大字数
const MAX_CONTENT = 1000
// 通用图片上传端点（后端通用上传接口；如后端路径不同，调整此处即可）
const UPLOAD_IMAGE_URL = '/api/v1/uploads/image'

Page({
  data: {
    // 商品上下文
    productId: '',
    skuId: '',
    orderId: '',
    // 商品信息
    productInfo: null,
    // 评分
    rating: 5,
    // 评论内容
    content: '',
    // 图片列表（本地临时路径 + 已上传 URL）
    imageList: [],
    // 提交中
    submitting: false,
    // 商品加载中
    productLoading: false,
    // 常量暴露给 wxml
    maxImages: MAX_IMAGES,
    maxContent: MAX_CONTENT
  },

  onLoad(options) {
    if (!isLoggedIn()) {
      const pages = getCurrentPages()
      const cur = pages[pages.length - 1]
      const redirect = cur ? '/' + cur.route : ''
      navigateToLogin(redirect)
      return
    }

    const opts = options || {}
    const productId = opts.productId || ''
    if (!productId) {
      wx.showToast({ title: '缺少商品参数', icon: 'none' })
      setTimeout(() => wx.navigateBack({ delta: 1 }), 800)
      return
    }

    this.setData({
      productId: productId,
      skuId: opts.skuId || '',
      orderId: opts.orderId || ''
    })

    this._loadProduct(productId)
  },

  /**
   * 加载商品信息
   */
  _loadProduct(productId) {
    this.setData({ productLoading: true })
    productApi.getProductDetail(productId)
      .then((res) => {
        const info = (res && res.data) || null
        this.setData({
          productInfo: info,
          productLoading: false
        })
      })
      .catch(() => {
        this.setData({ productLoading: false })
      })
  },

  /**
   * 评分变化
   */
  onRatingChange(e) {
    const rating = e.detail
    this.setData({ rating: rating })
  },

  /**
   * 评论内容输入
   */
  onContentInput(e) {
    this.setData({ content: e.detail })
  },

  /**
   * 选择图片
   */
  onChooseImage() {
    const left = MAX_IMAGES - this.data.imageList.length
    if (left <= 0) {
      wx.showToast({ title: '最多上传 ' + MAX_IMAGES + ' 张', icon: 'none' })
      return
    }
    wx.chooseImage({
      count: left,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFiles = res.tempFilePaths || []
        const current = this.data.imageList.slice()
        tempFiles.forEach((p) => {
          current.push({ localPath: p, uploaded: false, url: '' })
        })
        this.setData({ imageList: current })
      },
      fail: () => {}
    })
  },

  /**
   * 删除图片
   */
  onRemoveImage(e) {
    const { index } = e.currentTarget.dataset
    const list = this.data.imageList.slice()
    list.splice(Number(index), 1)
    this.setData({ imageList: list })
  },

  /**
   * 预览图片
   */
  onPreviewImage(e) {
    const { src } = e.currentTarget.dataset
    if (!src) return
    const urls = this.data.imageList.map((it) => it.localPath)
    wx.previewImage({
      current: src,
      urls: urls
    })
  },

  /**
   * 上传单张图片
   * @param {string} filePath 本地临时路径
   * @returns {Promise<string>} 图片 URL
   */
  _uploadOneImage(filePath) {
    const token = getAccessToken()
    if (!token) {
      return Promise.reject(new Error('未登录'))
    }
    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: BASE_URL + UPLOAD_IMAGE_URL,
        filePath: filePath,
        name: 'file',
        header: {
          Authorization: 'Bearer ' + token
        },
        success: (res) => {
          let body = null
          try {
            body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          } catch (e) {
            reject(new Error('上传响应解析失败'))
            return
          }
          const statusCode = res.statusCode
          if (statusCode >= 200 && statusCode < 300 && body && body.code === 200) {
            const data = body.data || {}
            // 兼容 url / imageUrl / data 为字符串等返回结构
            const url = data.url || data.imageUrl || (typeof data === 'string' ? data : '')
            resolve(url)
          } else {
            reject(new Error((body && body.message) || '上传失败'))
          }
        },
        fail: (err) => {
          reject(new Error((err && err.errMsg) || '上传失败'))
        }
      })
    })
  },

  /**
   * 上传所有未上传图片
   * @returns {Promise<Array<string>>} 图片 URL 数组
   */
  _uploadAllImages() {
    const list = this.data.imageList
    if (list.length === 0) return Promise.resolve([])

    // 顺序上传（避免并发过多）
    const tasks = list.map((it) => this._uploadOneImage(it.localPath))
    return Promise.all(tasks)
  },

  /**
   * 提交评论
   */
  onSubmit() {
    if (this.data.submitting) return

    const content = (this.data.content || '').trim()
    if (!content) {
      wx.showToast({ title: '请输入评论内容', icon: 'none' })
      return
    }
    if (content.length > MAX_CONTENT) {
      wx.showToast({ title: '评论最多 ' + MAX_CONTENT + ' 字', icon: 'none' })
      return
    }
    const rating = this.data.rating
    if (!rating || rating < 1 || rating > 5) {
      wx.showToast({ title: '请选择评分', icon: 'none' })
      return
    }

    this.setData({ submitting: true })

    // 1. 上传图片
    this._uploadAllImages()
      .then((urls) => {
        // 2. 提交评论
        const payload = {
          productId: this.data.productId,
          content: content,
          rating: rating,
          images: JSON.stringify(urls || [])
        }
        if (this.data.skuId) payload.skuId = this.data.skuId

        return reviewApi.create(payload)
      })
      .then(() => {
        this.setData({ submitting: false })
        wx.showToast({ title: '评价成功', icon: 'success' })
        setTimeout(() => {
          wx.navigateBack({ delta: 1 })
        }, 600)
      })
      .catch(() => {
        this.setData({ submitting: false })
        // request 拦截器已 toast 错误；图片上传错误在此 toast
      })
  }
})