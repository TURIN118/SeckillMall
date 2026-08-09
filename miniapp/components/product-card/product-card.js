// components/product-card/product-card.js
// 商品卡片组件：展示商品图片、名称、价格，点击触发跳转详情意图
//
// 对齐：
//   - design.md 2.7 节 product-card
//   - tasks.md P2
Component({
  properties: {
    // 商品对象 ProductVO（含 id/productName/images/originalPrice/minPrice 等）
    product: {
      type: Object,
      value: null
    }
  },
  data: {
    // 兜底首图
    coverImage: '',
    // 用于 price-tag 的展示价（优先 minPrice，其次 originalPrice）
    displayPrice: 0,
    // 用于 price-tag 的原价（仅当 minPrice 存在且小于 originalPrice 时展示划线）
    displayOriginal: null
  },
  observers: {
    'product': function (product) {
      if (!product) {
        this.setData({ coverImage: '', displayPrice: 0, displayOriginal: null })
        return
      }
      const images = Array.isArray(product.images) ? product.images : []
      const cover = images.length > 0 ? images[0] : ''
      // 价格优先级：minPrice（区间最低价）> originalPrice
      const minPrice = product.minPrice != null ? product.minPrice : null
      const original = product.originalPrice != null ? product.originalPrice : null
      let displayPrice = 0
      let displayOriginal = null
      if (minPrice != null) {
        displayPrice = minPrice
        // 若存在原价且高于 minPrice，则展示划线原价
        if (original != null && Number(original) > Number(minPrice)) {
          displayOriginal = original
        }
      } else if (original != null) {
        displayPrice = original
      }
      this.setData({
        coverImage: cover,
        displayPrice: displayPrice,
        displayOriginal: displayOriginal
      })
    }
  },
  methods: {
    /** 点击卡片：triggerEvent('tap', { id })，由父页面处理跳转 */
    onTap() {
      const product = this.data.product
      if (!product || product.id == null) return
      this.triggerEvent('tap', { id: String(product.id) })
    },
    /** 图片加载失败兜底 */
    onImageError() {
      this.setData({ coverImage: '' })
    }
  }
})