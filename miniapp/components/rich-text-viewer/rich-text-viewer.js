// components/rich-text-viewer/rich-text-viewer.js
// 富文本查看器：清洗 HTML 后用 rich-text 渲染
//
// 安全：渲染前必须经过 utils/rich-text.js sanitize 过滤
//       （script/style/link/iframe/onXXX/javascript: 协议等）
//
// 对齐：
//   - design.md 2.7 节 rich-text-viewer
//   - spec.md 4.1 节规则 3（富文本正文渲染前必须经过清洗）
//   - spec.md 4.3 节规则 1（过滤 script/style/link 等标签）
//   - tasks.md P3

const { sanitize } = require('../../utils/rich-text')

Component({
  properties: {
    // 原始 HTML 字符串
    html: {
      type: String,
      value: ''
    }
  },
  data: {
    // 清洗后的 HTML 字符串，供 rich-text 渲染
    safeHtml: ''
  },
  observers: {
    'html': function (html) {
      this._render(html)
    }
  },
  lifetimes: {
    attached() {
      this._render(this.data.html)
    }
  },
  methods: {
    /**
     * 清洗并渲染
     * @param {string} html 原始 HTML
     */
    _render(html) {
      const safe = sanitize(html || '')
      this.setData({ safeHtml: safe })
    }
  }
})