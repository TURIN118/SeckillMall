# static/tabbar/ tabBar 图标

> 本目录存放 tabBar 图标文件。当前为占位说明，实际图标文件需后续替换。

## 需要的图标文件（对齐 pages.json tabBar.list）

| 文件名 | 用途 | 尺寸建议 |
|--------|------|----------|
| home.png | 首页 tab 默认图标 | 81x81 px |
| home-active.png | 首页 tab 选中图标 | 81x81 px |
| category.png | 分类 tab 默认图标 | 81x81 px |
| category-active.png | 分类 tab 选中图标 | 81x81 px |
| cart.png | 购物车 tab 默认图标 | 81x81 px |
| cart-active.png | 购物车 tab 选中图标 | 81x81 px |
| profile.png | 我的 tab 默认图标 | 81x81 px |
| profile-active.png | 我的 tab 选中图标 | 81x81 px |

## 说明

- 微信小程序 tabBar 图标尺寸建议 81x81 px（或 40x40 px），格式 PNG
- 默认图标用灰色（#999999），选中图标用品牌色（#FF4D4F）
- 开发期可先用 1x1 透明 PNG 占位，编译时 tabBar 不显示图标但不报错
- 正式图标由 UI 设计提供，放至此目录即可