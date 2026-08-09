// components/sku-selector/sku-selector.js
// SKU 选择器组件：底部弹出规格矩阵，用户选择属性值后匹配对应 SKU
//
// 数据结构约定（兼容多种后端格式）：
//   attributes: Array<{ name: string, values: string[] }>
//                或 Array<{ attributeName: string, values: string[] }>
//   skus:       Array<{ id: string, price: number, stock: number,
//                       specs: { [attrName]: attrValue } | Array<{ name, value }> }>
//
// 事件：
//   confirm(e) { detail: { sku } }  选中 SKU 后回传
//   close()                          关闭面板
//
// 对齐：
//   - design.md 2.7 节 sku-selector
//   - spec.md 5.3.1 节规则 2（SKU 选择规则）
//   - tasks.md P4

Component({
  properties: {
    // SKU 数组
    skus: {
      type: Array,
      value: []
    },
    // 属性数组（规格矩阵维度）
    attributes: {
      type: Array,
      value: []
    },
    // 是否显示面板
    show: {
      type: Boolean,
      value: false
    }
  },
  data: {
    // 标准化后的属性矩阵：[{ name, values: [string] }]
    attrMatrix: [],
    // 各属性的选中值：{ [attrName]: value }
    selected: {},
    // 匹配到的 SKU
    matchedSku: null,
    // 提示文案
    tipText: '请选择规格'
  },
  observers: {
    'attributes, skus': function (attributes, skus) {
      this._buildMatrix(attributes, skus)
    },
    'show': function (show) {
      // 打开时重置匹配状态（保留已选）
      if (show) {
        this._reMatch()
      }
    }
  },
  lifetimes: {
    attached() {
      this._buildMatrix(this.data.attributes, this.data.skus)
    }
  },
  methods: {
    /**
     * 构建规格矩阵
     * 兼容 attributes 的 name / attributeName 两种字段
     */
    _buildMatrix(attributes, skus) {
      const attrs = Array.isArray(attributes) ? attributes : []
      const matrix = attrs.map((attr) => {
        const name = attr.name || attr.attributeName || attr.attrName || ''
        let values = attr.values || attr.valueList || []
        if (!Array.isArray(values)) values = []
        return { name, values: values.slice() }
      })
      this.setData({ attrMatrix: matrix }, () => {
        this._reMatch()
      })
    },

    /**
     * 从 SKU 中提取规格键值对
     * 兼容 specs 为对象或数组两种格式
     * @returns {object} { [attrName]: attrValue }
     */
    _extractSpecs(sku) {
      if (!sku) return {}
      const specs = sku.specs || sku.specValues || sku.attributeValues || sku.attributes
      if (!specs) return {}
      // 对象格式：直接返回
      if (!Array.isArray(specs)) return Object.assign({}, specs)
      // 数组格式：[{ name, value }] / [{ attributeName, attributeValue }] / [{ key, value }]
      const result = {}
      specs.forEach((item) => {
        const k = item.name || item.attributeName || item.key || item.attrName
        const v = item.value || item.attributeValue || item.attrValue
        if (k) result[k] = v
      })
      return result
    },

    /**
     * 重新匹配 SKU
     * 当所有属性都已选时，在 skus 中找规格完全匹配的 SKU
     */
    _reMatch() {
      const { attrMatrix, selected, skus } = this.data
      const skuArr = Array.isArray(skus) ? skus : []

      // 判断是否所有属性都已选
      const allSelected = attrMatrix.every((attr) => {
        return selected[attr.name] != null && selected[attr.name] !== ''
      })

      if (!allSelected) {
        this.setData({ matchedSku: null, tipText: '请选择规格' })
        return
      }

      // 在 skus 中找规格完全匹配的 SKU
      const matched = skuArr.find((sku) => {
        const specs = this._extractSpecs(sku)
        return attrMatrix.every((attr) => {
          return specs[attr.name] === selected[attr.name]
        })
      })

      if (matched) {
        const stock = matched.stock != null ? matched.stock : 0
        this.setData({
          matchedSku: matched,
          tipText: stock > 0 ? '已选：' + this._formatSelected() : '库存不足'
        })
      } else {
        this.setData({ matchedSku: null, tipText: '该规格组合无货' })
      }
    },

    /**
     * 格式化已选规格文案
     */
    _formatSelected() {
      const { attrMatrix, selected } = this.data
      return attrMatrix.map((attr) => selected[attr.name]).join(' / ')
    },

    /**
     * 点击属性值
     */
    onTapValue(e) {
      const { name, value } = e.currentTarget.dataset
      const selected = Object.assign({}, this.data.selected)
      // 再次点击同一个值则取消选择
      if (selected[name] === value) {
        delete selected[name]
      } else {
        selected[name] = value
      }
      this.setData({ selected }, () => {
        this._reMatch()
      })
    },

    /**
     * 点击确定
     */
    onConfirm() {
      const sku = this.data.matchedSku
      if (!sku) {
        wx.showToast({ title: this.data.tipText || '请选择规格', icon: 'none' })
        return
      }
      const stock = sku.stock != null ? sku.stock : 0
      if (stock <= 0) {
        wx.showToast({ title: '库存不足', icon: 'none' })
        return
      }
      this.triggerEvent('confirm', { sku })
      // 关闭面板
      this.triggerEvent('close')
    },

    /**
     * 关闭面板（点击遮罩或关闭按钮）
     */
    onClose() {
      this.triggerEvent('close')
    },

    /**
     * 阻止内部点击冒泡
     */
    noop() {}
  }
})