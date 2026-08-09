/**
 * TypeScript 类型定义（对齐 spec.md 第 2 章 API 契约）
 * 所有 ID 字段显式声明为 string（雪花 ID 约束 C1.6，禁止 Number 转换）
 */

// ============ 通用响应类型 ============

/** 统一响应结构 Result<T>（spec.md 2.1） */
export interface Result<T = any> {
  code: number       // 业务码，200 为成功
  message: string    // 提示信息
  data: T            // 业务数据
  timestamp: string  // 服务器时间，用于时间同步
}

/** 分页结果 */
export interface PageResult<T = any> {
  list: T[]
  total: number
  page: number
  pageSize: number
  hasMore: boolean
}

/** 分页请求参数 */
export interface PageQuery {
  page?: number
  pageSize?: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

/** 业务码枚举（spec.md 2.2） */
export enum BizCode {
  SUCCESS = 200,
  UNAUTHORIZED = 1002,       // Token 过期，触发刷新
  REPLAY_DETECTED = 1011     // 防重放拦截，不触发刷新
}

// ============ 认证 / 用户类型 ============

/** Token 响应 */
export interface TokenVO {
  accessToken: string
  refreshToken: string
  expiresIn?: number
}

/** 登录请求 */
export interface LoginRequest {
  account: string      // 账号/邮箱/手机号
  password: string
  captchaCode: string  // 图形验证码
  captchaKey: string   // 验证码 key
  rememberMe?: boolean
}

/** 登录响应 */
export interface LoginVO extends TokenVO {}

/** 用户信息 */
export interface UserVO {
  id: string
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  gender: 0 | 1 | 2   // 0未知 1男 2女
  birthday: string
  createdAt: string
  updatedAt: string
}

/** 用户信息别名（对齐 plan.md stores/user.ts） */
export type UserInfo = UserVO

/** 注册请求 */
export interface RegisterRequest {
  username: string
  password: string
  email: string
  phone: string
  captchaCode: string
  captchaKey: string
  smsCode?: string
  agreement: boolean
}

/** 图形验证码响应 */
export interface CaptchaVO {
  img: string   // base64 图片字符串（方案 A）
  key: string   // 验证码 key
}

/** 刷新 Token 请求 */
export interface RefreshTokenRequest {
  refreshToken: string
}

/** 修改密码请求 */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  captchaCode?: string
  captchaKey?: string
}

/** 修改资料请求 */
export interface UpdateProfileRequest {
  nickname?: string
  avatar?: string
  gender?: 0 | 1 | 2
  birthday?: string
  email?: string
  phone?: string
}

/** 发送重置验证码请求 */
export interface SendResetCodeRequest {
  email?: string
  phone?: string
}

/** 重置密码请求 */
export interface ResetPasswordRequest {
  email?: string
  phone?: string
  code: string
  newPassword: string
}

// ============ 商品类型 ============

/** 商品 VO */
export interface ProductVO {
  id: string
  name: string
  subtitle: string
  mainImage: string
  images: string[]
  price: number
  originalPrice: number
  stock: number
  sales: number
  categoryId: string
  categoryName: string
  description: string       // 富文本 HTML
  status: number
  createdAt: string
  updatedAt: string
}

/** 商品列表查询参数 */
export interface ProductQuery extends PageQuery {
  categoryId?: string
  keyword?: string
  minPrice?: number
  maxPrice?: number
}

// ============ 分类类型 ============

/** 分类 VO */
export interface CategoryVO {
  id: string
  name: string
  icon: string
  parentId: string
  sort: number
  level: number
  children?: CategoryVO[]
}

// ============ 购物车类型 ============

/** 购物车项 VO */
export interface CartItemVO {
  id: string
  productId: string
  productName: string
  productImage: string
  price: number
  quantity: number
  stock: number
  selected: boolean
  skuId?: string
  skuSpec?: string
}

/** 加入购物车请求 */
export interface AddCartRequest {
  productId: string
  quantity: number
  skuId?: string
}

/** 修改数量请求 */
export interface UpdateCartQuantityRequest {
  quantity: number
}

/** 批量选中请求 */
export interface BatchSelectedRequest {
  ids: string[]
  selected: boolean
}

/** 购物车统计 */
export interface CartCountVO {
  count: number
  selectedCount: number
  totalAmount: number
}

// ============ 订单类型 ============

/** 订单 VO */
export interface OrderVO {
  id: string
  orderNo: string
  status: number           // 订单状态
  type: number             // 订单类型（0普通 1秒杀）
  totalAmount: number
  payAmount: number
  freightAmount: number
  couponAmount: number
  remark: string
  items: OrderItemVO[]
  addressId: string
  addressSnapshot?: AddressVO
  createdAt: string
  updatedAt: string
  payTime?: string
  deliverTime?: string
  confirmTime?: string
}

/** 订单项 VO */
export interface OrderItemVO {
  id: string
  orderId: string
  productId: string
  productName: string
  productImage: string
  price: number
  quantity: number
  skuId?: string
  skuSpec?: string
}

/** 创建订单请求 */
export interface CreateOrderRequest {
  addressId: string
  remark?: string
  couponId?: string
  items: Array<{
    productId: string
    quantity: number
    skuId?: string
  }>
}

/** 从购物车创建订单请求 */
export interface CreateOrderFromCartRequest {
  addressId: string
  remark?: string
  couponId?: string
  cartIds: string[]
}

/** 订单列表查询参数 */
export interface OrderQuery extends PageQuery {
  status?: number
  type?: number
}

// ============ 秒杀类型 ============

/** 秒杀商品 VO */
export interface SeckillGoodsVO {
  id: string
  productId: string
  productName: string
  productImage: string
  seckillPrice: number
  originalPrice: number
  totalStock: number
  availableStock: number
  limitPerUser: number
  startTime: string
  endTime: string
  status: number          // 0未开始 1进行中 2已结束
  activityId: string
}

/** 秒杀活动 VO */
export interface SeckillActivityVO {
  id: string
  name: string
  startTime: string
  endTime: string
  status: number
  goodsList: SeckillGoodsVO[]
}

/** 秒杀库存 VO */
export interface SeckillStockVO {
  id: string
  totalStock: number
  availableStock: number
  soldStock: number
}

/** 秒杀执行请求 */
export interface SeckillExecuteRequest {
  quantity?: number
  addressId?: string
}

/** 秒杀结果 VO */
export interface SeckillResultVO {
  success: boolean
  orderId: string
  orderNo: string
  message: string
  status: number          // 0排队中 1成功 2失败 3已售罄
}

/** 秒杀列表查询参数 */
export interface SeckillQuery extends PageQuery {
  activityId?: string
  status?: number
}

// ============ 收藏类型 ============

/** 收藏 VO */
export interface FavoriteVO {
  id: string
  userId: string
  productId: string
  productName: string
  productImage: string
  price: number
  createdAt: string
}

/** 收藏查询参数 */
export interface FavoriteQuery extends PageQuery {
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

// ============ 收货地址类型 ============

/** 收货地址 VO */
export interface AddressVO {
  id: string
  userId: string
  receiver: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: boolean
  tag?: string
  createdAt: string
  updatedAt: string
}

/** 地址请求 */
export interface AddressRequest {
  receiver: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault?: boolean
  tag?: string
}

// ============ 优惠券类型 ============

/** 优惠券 VO */
export interface CouponVO {
  id: string
  name: string
  type: number            // 1满减 2折扣 3无门槛
  value: number
  minAmount: number
  discountAmount: number
  startTime: string
  endTime: string
  status: number          // 0未使用 1已使用 2已过期
  scope: number           // 适用范围
  description: string
}

/** 优惠券查询参数 */
export interface CouponQuery extends PageQuery {
  status?: number
}

// ============ 钱包类型 ============

/** 钱包 VO */
export interface WalletVO {
  id: string
  userId: string
  balance: number
  frozenBalance: number
  totalRecharge: number
  totalConsume: number
  updatedAt: string
}

// ============ 轮播图类型 ============

/** 轮播图 VO */
export interface BannerVO {
  id: string
  title: string
  image: string
  link: string
  sort: number
  status: number
}

// ============ 评价类型 ============

/** 评价 VO */
export interface ReviewVO {
  id: string
  orderId: string
  productId: string
  userId: string
  username: string
  avatar: string
  rating: number          // 1-5 星
  content: string
  images: string[]
  createdAt: string
  reply?: string
  replyTime?: string
}

/** 评价请求 */
export interface ReviewRequest {
  orderId: string
  productId: string
  rating: number
  content: string
  images?: string[]
}

/** 评价查询参数 */
export interface ReviewQuery extends PageQuery {
  productId?: string
  rating?: number
}

// ============ 上传类型 ============

/** 上传结果 */
export interface UploadResultVO {
  url: string
  filename?: string
  size?: number
  mimeType?: string
}

// ============ 验证码类型 ============

/** 发送验证码请求 */
export interface SendVerificationRequest {
  target: string          // 手机号或邮箱
  type: string            // 验证码类型（register/reset 等）
  captchaCode?: string
  captchaKey?: string
}

/** 验证码校验请求 */
export interface VerifyCodeRequest {
  target: string
  code: string
  type: string
}

// ============ 通用枚举 ============

/** 订单状态 */
export enum OrderStatus {
  PENDING_PAY = 0,        // 待付款
  PENDING_DELIVER = 1,    // 待发货
  PENDING_RECEIVE = 2,    // 待收货
  COMPLETED = 3,          // 已完成
  CANCELLED = 4,          // 已取消
  REFUNDING = 5,          // 退款中
  REFUNDED = 6            // 已退款
}

/** 秒杀状态 */
export enum SeckillStatus {
  NOT_STARTED = 0,
  IN_PROGRESS = 1,
  ENDED = 2
}