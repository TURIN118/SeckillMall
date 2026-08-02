/**
 * 类型定义 - 严格匹配 default.md 接口文档
 */

/* ==================== 通用类型 ==================== */

/** 统一响应结果 */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: string
}

/** 分页结果 */
export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/** 分页请求 */
export interface PageRequest {
  pageNum?: number
  pageSize?: number
}

/* ==================== 认证类型 ==================== */

/** 用户角色 */
export type UserRole = 'BUYER' | 'SELLER' | 'ADMIN'

/** 用户状态 */
export type UserStatus = 'ACTIVE' | 'DISABLED'

/** 用户视图对象 */
export interface UserVO {
  id: number
  username: string
  phone: string
  email: string
  nickname: string
  avatar: string
  role: UserRole
  status: string
  createTime: string
}

/** 登录响应 */
export interface LoginVO {
  accessToken: string
  refreshToken: string
  user: UserVO
}

/** Token 响应 */
export interface TokenVO {
  accessToken: string
  refreshToken: string
}

/** 图形验证码 */
export interface CaptchaVO {
  captchaId: string
  captchaImage: string
}

/* ==================== 商品类型 ==================== */

/** 商品状态 */
export type ProductStatus = 'ON_SALE' | 'OFF_SHELF'

/** 商品视图对象 */
export interface ProductVO {
  id: number
  productName: string
  categoryId: number
  categoryName: string
  description: string
  /** 商品详情富文本(HTML)，由 wangEditor 产生 */
  detailHtml?: string
  originalPrice: number
  images: string[]
  stock: number
  salesCount: number
  status: ProductStatus
  createTime: string
}

/** 分类视图对象 */
export interface CategoryVO {
  id: number
  parentId: number
  categoryName: string
  sortOrder: number
  status: number
}

/** 分类树节点 (前端构建) */
export interface CategoryTreeNode extends CategoryVO {
  children?: CategoryTreeNode[]
}

/* ==================== 商品评论类型 ==================== */

/** 商品评论视图对象 */
export interface ProductReviewVO {
  id: number
  productId: number
  userId: number
  /** 评论用户名 */
  userName: string
  orderId: number | null
  content: string
  /** 评分：1-5 星 */
  rating: number
  /** 评论图片 URL 数组 */
  images: string[]
  /** 状态：1-显示 / 0-隐藏 */
  status: number
  /** 商家回复内容 */
  replyContent: string | null
  /** 回复时间 */
  replyTime: string | null
  createTime: string
}

/** 发表评论请求 */
export interface ReviewCreateRequest {
  productId: number
  content: string
  rating: number
  /** 评论图片 URL 数组（JSON 字符串），可选 */
  images?: string
}

/* ==================== 轮播图类型 ==================== */

/** 轮播图视图对象 */
export interface BannerVO {
  id: number
  title: string
  imageUrl: string
  linkUrl: string
  sortOrder: number
  /** 状态：1-启用 / 0-禁用 */
  status: number
  createTime?: string
  updateTime?: string
}

/* ==================== 购物车类型 ==================== */

/** 购物车项视图对象 (匹配后端 CartItemVO) */
export interface CartItemVO {
  id: number
  productId: number
  quantity: number
  /** 是否选中: true-选中 / false-未选中 */
  selected: boolean
  productName: string
  mainImage: string
  originalPrice: number
  stock: number
  /** 商品状态: ON_SALE-上架 / OFF_SHELF-下架 */
  productStatus: ProductStatus
  /** 小计 = 单价 * 数量 */
  subtotal: number
}

/** 添加购物车请求 */
export interface CartAddRequest {
  productId: number
  quantity: number
}

/* ==================== 收藏夹类型 ==================== */

/** 收藏夹项视图对象 (匹配后端 FavoriteItemVO) */
export interface FavoriteItemVO {
  id: number
  productId: number
  productName: string
  mainImage: string
  originalPrice: number
  salesCount: number
  favoriteCount: number
  /** 商品状态: ON_SALE-上架 / OFF_SHELF-下架 */
  productStatus: ProductStatus
}

/** 添加收藏请求 */
export interface FavoriteAddRequest {
  productId: number
}

/* ==================== 收货地址类型 ==================== */

/** 收货地址视图对象 (匹配后端 UserAddressVO) */
export interface UserAddressVO {
  id: number
  userId: number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  /** 是否默认地址：0-否 / 1-是 */
  isDefault: number
  createTime: string
}

/** 收货地址新增/编辑请求体 */
export interface UserAddressRequest {
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  /** 是否默认地址：0-否 / 1-是 */
  isDefault: number
}

/* ==================== 秒杀类型 ==================== */

/** 秒杀活动状态 */
export type SeckillStatus = 'PENDING' | 'ACTIVE' | 'ENDED' | 'CANCELLED'

/** 秒杀商品视图对象 */
export interface SeckillGoodsVO {
  id: number
  productId: number
  productName: string
  seckillName: string
  seckillPrice: number
  stockCount: number
  availableCount: number
  startTime: string
  endTime: string
  status: SeckillStatus
  perLimit: number
  images: string[]
  description: string
  createTime: string
}

/** 秒杀结果状态: 0=排队中, 1=成功, -1=库存不足, -2=重复购买 */
export type SeckillResultStatus = 0 | 1 | -1 | -2

/** 秒杀结果视图对象 */
export interface SeckillResultVO {
  status: number
  requestId: string
  orderId: number
  orderNo: string
  totalAmount: number
  payExpireTime: string
}

/* ==================== 订单类型 ==================== */

/** 订单状态 */
export type OrderStatus = 'UNPAID' | 'PAID' | 'CANCELLED' | 'TIMEOUT' | 'COMPLETED'

/** 秒杀订单 */
export interface SeckillOrder {
  id: number
  orderNo: string
  userId: number
  seckillId: number
  productId: number
  seckillPrice: number
  quantity: number
  totalAmount: number
  status: OrderStatus
  payTime: string
  payExpireTime: string
  transactionId: string
  payMethod: string
  cancelTime: string
  cancelReason: string
  isDeleted: number
  createTime: string
  updateTime: string
}

/* ==================== 后台管理类型 ==================== */

/** 仪表盘统计 */
export interface DashboardVO {
  userCount: number
  orderCount: number
  totalSales: number
  seckillCount: number
}

/** 系统健康状态 */
export interface SystemHealthVO {
  redis: string
  database: string
  mq: string
  allHealthy: boolean
}

/** 操作日志 */
export interface OperationLogVO {
  id: number
  operatorId: number
  operatorName: string
  module: string
  action: string
  targetId: string
  targetType: string
  detail: string
  ipAddress: string
  operationTime: string
}

/** 登录日志 */
export interface LoginLogVO {
  id: number
  userId: number
  username: string
  loginIp: string
  loginLocation: string
  userAgent: string
  loginResult: string
  failReason: string
  loginTime: string
}

/* ==================== 请求类型 ==================== */

/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
  captchaKey?: string
  captchaCode?: string
}

/** 注册请求 */
export interface RegisterRequest {
  username: string
  password: string
  phone: string
  captchaKey: string
  captchaCode: string
}

/** 刷新令牌请求 */
export interface RefreshTokenRequest {
  refreshToken: string
}

/** 修改密码请求 */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

/** 商品查询请求 */
export interface ProductQueryRequest extends PageRequest {
  categoryId?: number
  keyword?: string
  sortBy?: string
  sortOrder?: string
  /** 商品状态筛选(可选): ON_SALE / OFF_SHELF, 不传表示不筛选 */
  status?: ProductStatus
}

/** 新增商品请求 */
export interface ProductCreateRequest {
  productName: string
  categoryId: number
  originalPrice: number
  stock: number
  description?: string
  /** 商品详情富文本(HTML) */
  detailHtml?: string
  images?: string[]
  status?: ProductStatus
}

/** 编辑商品请求 */
export interface ProductUpdateRequest {
  productName?: string
  categoryId?: number
  originalPrice?: number
  stock?: number
  description?: string
  /** 商品详情富文本(HTML) */
  detailHtml?: string
  images?: string[]
  status?: ProductStatus
}

/** 创建秒杀活动请求 */
export interface SeckillCreateRequest {
  productId: number
  seckillName: string
  seckillPrice: number
  stockCount: number
  startTime: string
  endTime: string
  perLimit?: number
  images?: string[]
  description?: string
}

/** 用户列表请求 */
export interface UserListRequest extends PageRequest {
  role?: string
  status?: string
  keyword?: string
}

/** 用户状态更新请求 */
export interface UserStatusUpdateRequest {
  status: string
}

/** 用户角色更新请求 */
export interface UserRoleUpdateRequest {
  role: string
}

/** 操作日志查询请求 */
export interface OperationLogQueryRequest extends PageRequest {
  module?: string
  operatorId?: number
}

/* ==================== 优惠券类型 ==================== */

/** 优惠券类型: AMOUNT-满减券 / DISCOUNT-折扣券 */
export type CouponType = 'AMOUNT' | 'DISCOUNT'

/** 用户优惠券状态: UNUSED-未使用 / USED-已使用 / EXPIRED-已过期 */
export type UserCouponStatus = 'UNUSED' | 'USED' | 'EXPIRED'

/** 优惠券视图对象 (匹配后端 CouponVO) */
export interface CouponVO {
  id: number
  name: string
  /** 优惠券类型: AMOUNT-满减 / DISCOUNT-折扣 */
  type: CouponType
  /** 面额: 满减券为减免金额, 折扣券为折扣率(如 8.5 表示 8.5 折) */
  amount: number
  /** 最低消费金额 */
  minAmount: number
  /** 发放总数 */
  totalCount: number
  /** 已领取数量 */
  receivedCount: number
  /** 已使用数量 */
  usedCount: number
  /** 生效起始时间 */
  startTime: string
  /** 生效结束时间 */
  endTime: string
  /** 状态: 1-启用 / 0-停用 */
  status: number
  createTime?: string
  updateTime?: string
}

/** 用户优惠券视图对象 (匹配后端 UserCouponVO) */
export interface UserCouponVO {
  id: number
  /** 用户 ID */
  userId: number
  couponId: number
  /** 状态: UNUSED-未使用 / USED-已使用 / EXPIRED-已过期 */
  status: UserCouponStatus
  /** 领取时间 */
  receiveTime: string
  /** 使用时间 */
  useTime: string | null
  /** 关联订单 ID (使用后才有值) */
  orderId: number | null
  /** 优惠券详情 */
  coupon: CouponVO
  createTime?: string
}

/** 优惠券新增/编辑请求 */
export interface CouponRequest {
  name: string
  type: CouponType
  amount: number
  minAmount: number
  totalCount: number
  startTime: string
  endTime: string
  status?: number
}

/* ==================== 充值卡类型 ==================== */

/** 充值卡状态: UNUSED-未使用 / USED-已使用 / DISABLED-已禁用 */
export type RechargeCardStatus = 'UNUSED' | 'USED' | 'DISABLED'

/** 充值卡视图对象 (匹配后端 RechargeCardVO) */
export interface RechargeCardVO {
  id: number
  /** 卡号 */
  cardNo: string
  /** 卡密明文 (仅在批量生成时返回一次，列表查询不返回) */
  cardPassword?: string
  /** 面额 */
  faceValue: number
  /** 状态: UNUSED-未使用 / USED-已使用 / DISABLED-已禁用 */
  status: RechargeCardStatus
  /** 使用者用户 ID (未使用时为 null) */
  usedBy: number | null
  /** 使用时间 */
  usedTime: string | null
  /** 批次号 */
  batchNo: string
  createTime?: string
}

/** 充值卡批量生成请求 */
export interface RechargeCardGenerateRequest {
  /** 面额 */
  faceValue: number
  /** 生成数量 */
  count: number
}

/* ==================== 钱包类型 ==================== */

/** 钱包余额视图对象 */
export interface WalletBalanceVO {
  /** 钱包余额 */
  balance: number
}

/** 钱包交易记录视图对象 (匹配后端 WalletRecordVO) */
export interface WalletRecordVO {
  id: number
  /** 交易类型: RECHARGE-充值 / CONSUME-消费 / REFUND-退款 等 */
  type: string
  /** 交易金额 (正数为收入, 负数为支出) */
  amount: number
  /** 交易时间 */
  createTime: string
  /** 备注 */
  remark: string
}

/** 钱包充值请求 */
export interface WalletRechargeRequest {
  /** 卡号 */
  cardNo: string
  /** 卡密 */
  cardPassword: string
}

/* ==================== 验证码类型 ==================== */

/** 验证码发送请求 (邮件/短信) */
export interface VerificationSendRequest {
  /** 接收目标: 邮箱地址或手机号 */
  target: string
}

/** 验证码校验请求 */
export interface VerificationCheckRequest {
  /** 校验目标: 邮箱地址或手机号 */
  target: string
  /** 验证码 */
  code: string
}

/** 修改手机号请求 (需验证码) */
export interface UpdatePhoneRequest {
  /** 新手机号 */
  phone: string
  /** 短信验证码 */
  code: string
}

/** 修改邮箱请求 (需验证码) */
export interface UpdateEmailRequest {
  /** 新邮箱 */
  email: string
  /** 邮箱验证码 */
  code: string
}

/* ==================== Vue Router 元信息扩展 ==================== */
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    roles?: UserRole[]
  }
}