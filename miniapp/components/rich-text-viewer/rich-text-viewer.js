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
const { formatImageUrl } = require('../../utils/image')

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
        safeHtml: '',
        // 从富文本中提取出的视频列表，用微信原生 <video> 组件渲染
        // 微信 rich-text 不支持 <video> 标签，需单独提取
        videos: []
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
            // 拼接富文本中 <img src> 的相对路径 BASE_URL
            const rewritten = this._rewriteImgSrc(safe)
            // 提取 <video> 标签，单独用微信原生 <video> 组件渲染
            // （微信 rich-text 不支持 video 标签，会直接丢弃）
            const { html: htmlWithoutVideo, videos } = this._extractVideos(rewritten)
            this.setData({ safeHtml: htmlWithoutVideo, videos: videos })
        },
        /**
         * 提取 HTML 中的 <video> 标签
         * 微信 rich-text 组件不支持 <video> 标签，需将其从 HTML 中剥离，
         * 单独用微信原生 <video> 组件渲染。
         * @param {string} html
         * @returns {{html: string, videos: Array<{src: string, poster: string}>}}
         */
        _extractVideos(html) {
            if (!html || typeof html !== 'string') return { html: '', videos: [] }
            const videos = []
            // 匹配 <video ...>...</video> 和 <video .../>
            const videoRegex = /<video\b[^>]*>([\s\S]*?)<\/video>|<video\b[^>]*\/?>/gi
            let result = html.replace(videoRegex, (match) => {
                // 提取 src
                const srcMatch = match.match(/\bsrc\s*=\s*("([^"]*)"|'([^']*)')/i)
                const src = srcMatch ? (srcMatch[2] || srcMatch[3] || '') : ''
                // 提取 poster
                const posterMatch = match.match(/\bposter\s*=\s*("([^"]*)"|'([^']*)')/i)
                const poster = posterMatch ? (posterMatch[2] || posterMatch[3] || '') : ''
                // 也检查 <source src="xxx"> 子标签
                if (!src) {
                    const sourceMatch = match.match(/<source\b[^>]*\bsrc\s*=\s*("([^"]*)"|'([^']*)')/i)
                    if (sourceMatch) {
                        videos.push({ src: sourceMatch[2] || sourceMatch[3] || '', poster: '' })
                        return ''
                    }
                }
                if (src) {
                    videos.push({ src: src, poster: poster })
                }
                return ''
            })
            return { html: result, videos: videos }
        },
        /**
         * 重写富文本中所有 <img> 的 src：相对路径拼接 BASE_URL
         * @param {string} html
         * @returns {string}
         */
        _rewriteImgSrc(html) {
            if (!html || typeof html !== 'string') return ''
            return html.replace(/<img\b[^>]*>/gi, (imgTag) => {
                return imgTag.replace(/\bsrc\s*=\s*("([^"]*)"|'([^']*)')/i, (m, quoted, dq, sq) => {
                    const raw = dq != null ? dq : (sq != null ? sq : '')
                    return 'src="' + formatImageUrl(raw) + '"'
                })
            })
        }
    }
})