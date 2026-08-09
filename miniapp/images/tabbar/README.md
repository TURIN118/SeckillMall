# images/tabbar/ — tabBar 图标占位

## 当前状态

本目录下的 10 个 PNG 文件均为 **1x1 透明占位图标**，仅用于满足微信小程序 `app.json` 中 `tabBar.list[].iconPath / selectedIconPath` 的必填字段要求，确保微信开发者工具编译通过。

视觉上 tabBar **只显示文字**（color/selectedColor 生效），图标因透明不可见。

## 文件清单

| 文件 | 用途 |
|------|------|
| home.png / home-active.png | 首页 tab |
| cart.png / cart-active.png | 购物车 tab |
| seckill.png / seckill-active.png | 秒杀 tab |
| orders.png / orders-active.png | 订单 tab |
| profile.png / profile-active.png | 我的 tab |

## 后续替换

正式接入设计稿时，请用设计稿提供的图标替换本目录下同名文件：

- 尺寸：81x81 px（微信官方推荐）
- 格式：PNG，支持透明
- 普通态：color #999999 灰度图标
- 选中态：selectedColor #FF4444 红色图标
- 替换后无需修改 `app.json`，路径已配置完成