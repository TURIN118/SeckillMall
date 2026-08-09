# components/ 公共组件

> 本阶段（阶段 0）仅创建目录占位，公共组件在后续阶段按需实现。

## 组件清单（对齐 plan.md 第 2.1 节 / tasks.md T0.18）

| 组件 | 用途 | 实现阶段 |
|------|------|----------|
| NavBar | 自定义导航栏 | 阶段 1+ |
| ProductCard | 商品卡片 | 阶段 2 |
| EmptyState | 空状态 | 阶段 2 |
| LoadMore | 加载更多 | 阶段 2 |
| PriceTag | 价格标签 | 阶段 2 |
| CountdownTimer | 倒计时组件 | 阶段 4 |
| CaptchaInput | 图形验证码输入 | 阶段 1 T1.1 |
| AddressSelector | 地址选择器 | 阶段 3 |
| SkuSelector | SKU 规格选择器 | 阶段 2 T2.5 |
| RichTextRenderer | 富文本渲染（rich-text 封装） | 阶段 2 T2.4 |

## 说明

- 所有组件使用 Vue 3 Composition API + `<script setup lang="ts">`
- 组件目录结构：`ComponentName/ComponentName.vue` + `ComponentName/index.ts`（可选）