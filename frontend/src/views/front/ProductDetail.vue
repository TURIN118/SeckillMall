<template>
  <!-- 根据设计稿全面重构 ProductDetail.vue -->
  <div class="product-detail-page">
    <!-- 加载骨架屏 -->
    <div v-if="loading" class="loading-wrap">
      <div v-for="i in 8" :key="i" class="skeleton-line"></div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <div class="error-icon">!</div>
      <h3 class="error-title">商品不存在</h3>
      <p class="error-desc">您访问的商品可能已下架或被删除</p>
      <button class="btn-sm primary" @click="router.push('/products')">返回商品列表</button>
    </div>

    <!-- 商品详情内容 -->
    <template v-else-if="product">
      <!-- 面包屑（独立于卡片外） -->
      <nav class="breadcrumb">
        <span class="breadcrumb-home" @click="router.push('/')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/>
            <polyline points="9,22 9,12 15,12 15,22"/>
          </svg>
          首页
        </span>
        <span class="breadcrumb-sep">›</span>
        <span>{{ product.categoryName }}</span>
        <span class="breadcrumb-sep">›</span>
        <span class="breadcrumb-current">{{ product.productName }}</span>
      </nav>

      <!-- 白色卡片：仅包裹详情主体 -->
      <div class="product-hero-card">
        <div class="detail-grid">
          <!-- 左列: 图片轮播 -->
          <div class="detail-left">
            <!-- 轮播图增强：480px高度 + 计数标签 + 放大镜 + 条形指示器 + 左右箭头 -->
            <div class="carousel-wrap" @mouseenter="pauseAutoPlay" @mouseleave="resumeAutoPlay">
              <div class="carousel-main">
                <el-image
                  v-if="currentImage"
                  :src="currentImage"
                  fit="cover"
                  class="carousel-image"

                >
                  <template #error>
                    <div class="img-placeholder">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                        <rect x="3" y="3" width="18" height="18" rx="2" />
                        <circle cx="8.5" cy="8.5" r="1.5" />
                        <path d="m21 15-5-5L5 21" />
                      </svg>
                    </div>
                  </template>
                </el-image>
                <div v-else class="img-placeholder">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <rect x="3" y="3" width="18" height="18" rx="2" />
                    <circle cx="8.5" cy="8.5" r="1.5" />
                    <path d="m21 15-5-5L5 21" />
                  </svg>
                </div>
              </div>

              <!-- 图片计数标签 -->
              <div v-if="displayImages.length > 1" class="carousel-counter">
                {{ currentImageIdx + 1 }} / {{ displayImages.length }}
              </div>

              <!-- 放大镜入口按钮 -->
              <div v-if="currentImage" class="carousel-zoom" @click="handlePreviewImage">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="8"/>
                  <path d="m21 21-4.35-4.35"/>
                </svg>
              </div>

              <!-- 条形指示器（替代圆点） -->
              <div v-if="displayImages.length > 1" class="carousel-indicator">
                <span
                  v-for="(img, idx) in displayImages"
                  :key="idx"
                  :class="idx === currentImageIdx ? 'active' : 'inactive'"
                  @click="currentImageIdx = idx"
                ></span>
              </div>

              <!-- 左右箭头 -->
              <div v-if="displayImages.length > 1" class="carousel-arrow prev" @click="prevImage">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="15,18 9,12 15,6"/>
                </svg>
              </div>
              <div v-if="displayImages.length > 1" class="carousel-arrow next" @click="nextImage">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9,6 15,12 9,18"/>
                </svg>
              </div>
            </div>

            <!-- 缩略图（72px） -->
            <div v-if="displayImages.length > 1" class="thumb-strip">
              <div
                v-for="(img, idx) in displayImages"
                :key="idx"
                class="thumb-item"
                :class="{ active: idx === currentImageIdx }"
                @click="currentImageIdx = idx"
              >
                <el-image :src="formatImageUrl(img)" fit="cover" class="thumb-image" lazy>
                  <template #error>
                    <div class="thumb-placeholder">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                        <rect x="3" y="3" width="18" height="18" rx="2" />
                        <circle cx="8.5" cy="8.5" r="1.5" />
                        <path d="m21 15-5-5L5 21" />
                      </svg>
                    </div>
                  </template>
                </el-image>
              </div>
            </div>

            <!-- 分享/收藏 -->
            <div class="action-row">
              <span class="action-item" :class="{ favorited: isFavorited }" @click="handleFavorite">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path
                    d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" />
                </svg>
                {{ isFavorited ? '已收藏' : '收藏' }}
              </span>
              <span class="action-item" @click="handleShare">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="18" cy="5" r="3" />
                  <circle cx="6" cy="12" r="3" />
                  <circle cx="18" cy="19" r="3" />
                  <path d="M8.59 13.51l6.83 3.98M15.41 6.51l-6.82 3.98" />
                </svg>
                分享
              </span>
            </div>
          </div>

          <!-- 右列: 商品信息区增强 -->
          <div class="detail-info">
            <!-- 标题24px -->
            <h1 class="product-title fade-in-item stagger-1">{{ product.productName }}</h1>
            <!-- 描述增加行高 -->
            <p class="product-desc fade-in-item stagger-2">{{ product.description || '暂无描述' }}</p>

            <!-- 价格区：渐变背景 + 价格数字36px + ¥缩小 -->
            <div class="price-block fade-in-item stagger-3">
              <div class="price-main-row">
                <span class="price-currency">¥</span>
                <span class="price-value">{{ formatPrice(product.originalPrice) }}</span>
                <span v-if="product.status === 'ON_SALE'" class="status-tag on-sale">在售</span>
                <span v-else class="status-tag off-shelf">已下架</span>
              </div>
              <div class="price-stats">
                <span>累计销量 {{ product.salesCount }}</span>
                <span>好评率 {{ reviewStats.goodRate }}%</span>
                <span>库存 {{ product.stock }} 件</span>
              </div>
            </div>

            <!-- 信息卡片式布局 -->
            <div class="info-cards fade-in-item stagger-4">
              <div class="info-card">
                <span class="info-card-label">分类</span>
                <span class="info-card-value">{{ product.categoryName }}</span>
              </div>
              <div class="info-card">
                <span class="info-card-label">库存</span>
                <span class="info-card-value">{{ product.stock }} 件</span>
              </div>
              <div class="info-card">
                <span class="info-card-label">销量</span>
                <span class="info-card-value">{{ product.salesCount }} 件</span>
              </div>
              <div class="info-card">
                <span class="info-card-label">上架时间</span>
                <span class="info-card-value">{{ formatTime(product.createTime) }}</span>
              </div>
            </div>

            <!-- 服务保障：标签式设计 -->
            <div class="service-tags fade-in-item stagger-5">
              <span class="service-tag">
                <svg class="service-tag-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
                  <path d="M22 4L12 14.01l-3-3" />
                </svg>
                正品保障
              </span>
              <span class="service-tag">
                <svg class="service-tag-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
                  <path d="M22 4L12 14.01l-3-3" />
                </svg>
                7天无理由
              </span>
              <span class="service-tag">
                <svg class="service-tag-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
                  <path d="M22 4L12 14.01l-3-3" />
                </svg>
                运费险
              </span>
              <span class="service-tag">
                <svg class="service-tag-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
                  <path d="M22 4L12 14.01l-3-3" />
                </svg>
                极速退款
              </span>
            </div>

            <!-- 数量选择器 -->
            <div class="quantity-row">
              <span class="quantity-label">数量</span>
              <div class="quantity-control">
                <button class="quantity-btn" :class="{ disabled: quantity <= 1 }" @click="decrementQuantity">−</button>
                <input class="quantity-input" :value="quantity" readonly />
                <button class="quantity-btn" :class="{ disabled: quantity >= product.stock }" @click="incrementQuantity">+</button>
              </div>
              <span class="quantity-tip">{{ product.stock > 10 ? '库存充足' : `仅剩 ${product.stock} 件` }}</span>
            </div>

            <!-- 操作按钮：渐变+阴影 -->
            <div class="action-buttons">
              <button
                class="action-btn buy"
                :disabled="product.status === 'OFF_SHELF' || product.stock <= 0"
                @click="handleBuyNow"
              >
                {{ product.stock <= 0 ? '暂无库存' : '立即购买' }}
              </button>
              <button class="action-btn cart" :disabled="addingToCart || product.stock <= 0 || product.status === 'OFF_SHELF'" @click="handleAddToCart">
                {{ addingToCart ? '加入中...' : '加入购物车' }}
              </button>
            </div>

            <!-- 底部提示 -->
            <p class="action-tip">已有 {{ product.salesCount }} 人购买 · 支持7天无理由退换</p>
          </div>
        </div>
      </div>

      <!-- Tab 区域：胶囊式 -->
      <div class="tab-card">
        <div class="tab-section">
        <!-- Tab 头：胶囊式设计 -->
        <div class="tab-header">
          <div class="tab-item" :class="{ active: activeTab === 'detail' }" @click="activeTab = 'detail'">
            商品详情
          </div>
          <div class="tab-item" :class="{ active: activeTab === 'spec' }" @click="activeTab = 'spec'">
            规格参数
          </div>
          <div class="tab-item" :class="{ active: activeTab === 'review' }" @click="switchTab('review')">
            用户评价 <span v-if="reviewTotal > 0" class="tab-badge">{{ reviewTotal }}</span>
          </div>
          <div class="tab-item" :class="{ active: activeTab === 'service' }" @click="activeTab = 'service'">
            售后保障
          </div>
        </div>

        <!-- Tab 内容：卡片容器 -->
        <div class="tab-content-wrap">
          <!-- 商品详情 -->
          <div v-if="activeTab === 'detail'" class="tab-content" :key="'detail'">
            <div v-if="product.detailHtml" class="desc-content" v-html="safeDetailHtml"></div>
            <div v-else-if="product.description" class="desc-content">
              <p>{{ product.description }}</p>
            </div>
            <div v-else class="desc-empty">暂无商品描述</div>
          </div>

          <!-- 规格参数 -->
          <div v-if="activeTab === 'spec'" class="tab-content" :key="'spec'">
            <table class="spec-table">
              <tr>
                <td class="spec-key">商品名称</td>
                <td class="spec-val">{{ product.productName }}</td>
              </tr>
              <tr>
                <td class="spec-key">分类</td>
                <td class="spec-val">{{ product.categoryName }}</td>
              </tr>
              <tr>
                <td class="spec-key">库存</td>
                <td class="spec-val">{{ product.stock }} 件</td>
              </tr>
              <tr>
                <td class="spec-key">销量</td>
                <td class="spec-val">{{ product.salesCount }} 件</td>
              </tr>
            </table>
          </div>

          <!-- 用户评价 -->
          <div v-if="activeTab === 'review'" class="tab-content" :key="'review'">
            <!-- 评分统计概览 -->
            <div class="review-summary">
              <div class="review-score">
                <div class="review-score-value">{{ reviewStats.avgScore }}</div>
                <div class="review-stars-display">
                  <span v-for="star in 5" :key="star" class="star" :class="{ filled: star <= Math.round(Number(reviewStats.avgScore)) }">★</span>
                </div>
                <div class="review-score-label">{{ reviewTotal }}条评价</div>
              </div>
              <div class="review-bars">
                <div v-for="i in 5" :key="i" class="review-bar-row">
                  <span class="review-bar-label">{{ 6 - i }}星</span>
                  <div class="review-bar-track">
                    <div class="review-bar-fill" :style="{ width: reviewStats.starPercents[6 - i] + '%' }"></div>
                  </div>
                  <span class="review-bar-percent">{{ reviewStats.starPercents[6 - i] }}%</span>
                </div>
              </div>
            </div>

            <!-- 评价标签筛选 -->
            <div class="review-filter-tags">
              <span
                class="review-filter-tag"
                :class="{ active: reviewFilter === 'all' }"
                @click="switchReviewFilter('all')"
              >全部 ({{ reviewTotal }})</span>
              <span
                class="review-filter-tag"
                :class="{ active: reviewFilter === 'good' }"
                @click="switchReviewFilter('good')"
              >好评 ({{ reviewStats.goodCount }})</span>
              <span
                class="review-filter-tag"
                :class="{ active: reviewFilter === 'neutral' }"
                @click="switchReviewFilter('neutral')"
              >中评 ({{ reviewStats.neutralCount }})</span>
              <span
                class="review-filter-tag"
                :class="{ active: reviewFilter === 'bad' }"
                @click="switchReviewFilter('bad')"
              >差评 ({{ reviewStats.badCount }})</span>
            </div>

            <!-- 发表评论表单 -->
            <div class="review-form-wrap">
              <h4 class="review-form-title">发表评价</h4>
              <div v-if="!userStore.isLoggedIn" class="review-login-tip">
                请先 <router-link to="/login" class="review-login-link">登录</router-link> 后发表评价
              </div>
              <template v-else>
                <div class="review-form-row">
                  <span class="review-form-label">评分：</span>
                  <div class="rating-star-input">
                    <span v-for="star in 5" :key="star" class="star" :class="{ filled: star <= reviewForm.rating }"
                      @click="reviewForm.rating = star">★</span>
                  </div>
                  <span class="rating-text">{{ reviewForm.rating }} 星</span>
                </div>
                <textarea v-model="reviewForm.content" class="review-textarea" placeholder="请输入您的评价内容（最多 1000 字）"
                  maxlength="1000" rows="4"></textarea>
                <div class="review-form-actions">
                  <button class="btn-sm primary" :disabled="reviewSubmitting || !reviewForm.content.trim()"
                    @click="submitReview">{{ reviewSubmitting ? '提交中...' : '发表评价' }}</button>
                </div>
              </template>
            </div>

            <!-- 评论列表 -->
            <div class="review-list" v-loading="reviewLoading">
              <div v-if="filteredReviewList.length === 0 && !reviewLoading" class="review-empty">
                暂无评价，快来抢沙发吧！
              </div>
              <div v-for="review in filteredReviewList" :key="review.id" class="review-item">
                <div class="review-avatar">{{ (review.userName || '匿')[0] }}</div>
                <div class="review-body">
                  <div class="review-head">
                    <span class="review-user">{{ review.userName || '匿名用户' }}</span>
                    <span class="review-stars">
                      <span v-for="star in 5" :key="star" class="star small" :class="{ filled: star <= review.rating }">★</span>
                    </span>
                  </div>
                  <div class="review-text">{{ review.content }}</div>
                  <div v-if="review.images && review.images.length > 0" class="review-images">
                    <el-image v-for="(img, idx) in review.images" :key="idx" :src="formatImageUrl(img)" fit="cover"
                      class="review-img" lazy />
                  </div>
                  <div class="review-meta">{{ formatTime(review.createTime) }}</div>
                  <div v-if="review.replyContent" class="review-reply">
                    <div class="reply-label">商家回复：</div>
                    <div class="reply-content">{{ review.replyContent }}</div>
                    <div v-if="review.replyTime" class="reply-time">{{ formatTime(review.replyTime) }}</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 评论分页 -->
            <div v-if="reviewTotal > reviewPageSize" class="review-pagination">
              <button class="btn-sm" :disabled="reviewPageNum <= 1"
                @click="changeReviewPage(reviewPageNum - 1)">上一页</button>
              <span class="page-info-text">第 {{ reviewPageNum }} 页 / 共 {{ reviewTotalPages }} 页</span>
              <button class="btn-sm" :disabled="reviewPageNum >= reviewTotalPages"
                @click="changeReviewPage(reviewPageNum + 1)">下一页</button>
            </div>
          </div>

          <!-- 售后保障 -->
          <div v-if="activeTab === 'service'" class="tab-content" :key="'service'">
            <ul class="service-list">
              <li>
                <div class="service-list-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                  </svg>
                </div>
                <div class="service-list-text">
                  <h4>正品保障</h4>
                  <p>所有商品均为正品，假一赔十。支持品牌官方验证，确保每一件商品来源可靠。</p>
                </div>
              </li>
              <li>
                <div class="service-list-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                    <line x1="16" y1="2" x2="16" y2="6"/>
                    <line x1="8" y1="2" x2="8" y2="6"/>
                    <line x1="3" y1="10" x2="21" y2="10"/>
                  </svg>
                </div>
                <div class="service-list-text">
                  <h4>7天无理由退换货</h4>
                  <p>自签收日起 7 天内可无理由退换，商品未使用、包装完好即可申请，运费由商家承担。</p>
                </div>
              </li>
              <li>
                <div class="service-list-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
                  </svg>
                </div>
                <div class="service-list-text">
                  <h4>极速退款</h4>
                  <p>符合条件的退款申请 24 小时内处理完成，退款金额原路返回，最快 1 小时到账。</p>
                </div>
              </li>
              <li>
                <div class="service-list-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="1" y="3" width="15" height="13"/>
                    <polygon points="16,8 20,8 23,11 23,16 16,16 16,8"/>
                    <circle cx="5.5" cy="18.5" r="2.5"/>
                    <circle cx="18.5" cy="18.5" r="2.5"/>
                  </svg>
                </div>
                <div class="service-list-text">
                  <h4>运费险</h4>
                  <p>退换货无忧，运费由商家承担。签收后 15 天内因质量问题产生的退换货运费全额赔付。</p>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>
      </div>
    </template>

    <!-- 图片预览（放在页面最外层，不受overflow影响） -->
    <el-image-viewer
      v-if="showImageViewer"
      :url-list="previewImageList"
      :initial-index="currentImageIdx"
      @close="showImageViewer = false"
    />

    <!-- 移动端底部购买栏（仅768px以下显示） -->
    <div v-if="product" class="mobile-buy-bar">
      <div class="mobile-price">
        <span class="mobile-price-value">¥{{ formatPrice(product.originalPrice) }}</span>
        <span class="mobile-price-label">秒杀价</span>
      </div>
      <button class="action-btn cart" :disabled="addingToCart || product.stock <= 0 || product.status === 'OFF_SHELF'" @click="handleAddToCart">
        {{ addingToCart ? '加入中...' : '加入购物车' }}
      </button>
      <button
        class="action-btn buy"
        :disabled="product.status === 'OFF_SHELF' || product.stock <= 0"
        @click="handleBuyNow"
      >立即购买</button>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * P03 商品详情 - 根据设计稿全面重构
 */
import { ref, computed, reactive, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail } from '@/api/product'
import { getProductReviews, createReview } from '@/api/review'
import { getWalletBalance } from '@/api/wallet'
import { createOrder, payNormalOrder } from '@/api/order'
import { checkFavorite, addFavorite, removeFavorite } from '@/api/favorite'
import { addCart } from '@/api/cart'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ElImageViewer } from 'element-plus'
import dayjs from 'dayjs'
import { formatImageUrl } from '@/utils/image'
import DOMPurify from 'dompurify'
import type { ProductVO, ProductReviewVO } from '@/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const loading = ref<boolean>(false)
const error = ref<boolean>(false)
const product = ref<ProductVO | null>(null)
const currentImageIdx = ref<number>(0)

/** 是否显示图片预览 */
const showImageViewer = ref<boolean>(false)

/** 是否已收藏当前商品 */
const isFavorited = ref<boolean>(false)

/** 当前激活的 Tab */
const activeTab = ref<'detail' | 'spec' | 'review' | 'service'>('detail')

/* === 评论相关状态 === */
const reviewLoading = ref<boolean>(false)
const reviewList = ref<ProductReviewVO[]>([])
const reviewTotal = ref<number>(0)
const reviewPageNum = ref<number>(1)
const reviewPageSize = ref<number>(10)
const reviewSubmitting = ref<boolean>(false)
const reviewForm = reactive({
  rating: 5,
  content: ''
})

/* === 数量选择器 === */
const quantity = ref<number>(1)

/* === 加入购物车状态 === */
const addingToCart = ref<boolean>(false)

/* === 评价标签筛选 === */
const reviewFilter = ref<'all' | 'good' | 'neutral' | 'bad'>('all')

/* === 轮播图自动播放 === */
let autoPlayTimer: ReturnType<typeof setInterval> | null = null

/** 评论总页数 */
const reviewTotalPages = computed(() =>
  Math.max(1, Math.ceil(reviewTotal.value / reviewPageSize.value))
)

/** 显示的图片列表 */
const displayImages = computed<string[]>(() => {
  if (!product.value) return []
  return product.value.images || []
})

/** 预览图片列表（用于 el-image 的 preview-src-list） */
const previewImageList = computed<string[]>(() => {
  return displayImages.value.map(img => formatImageUrl(img))
})

/**
 * C6 修复: 净化后的商品详情 HTML
 * 使用 DOMPurify 移除潜在的 XSS 攻击代码
 */
const safeDetailHtml = computed<string>(() => {
  return product.value?.detailHtml ? DOMPurify.sanitize(product.value.detailHtml) : ''
})

/** 当前展示图片 */
const currentImage = computed<string>(() => {
  return formatImageUrl(displayImages.value[currentImageIdx.value] || '')
})

/** 评分统计概览 */
const reviewStats = computed(() => {
  const list = reviewList.value
  const total = list.length
  if (total === 0) {
    return {
      avgScore: '5.0',
      goodRate: 100,
      goodCount: 0,
      neutralCount: 0,
      badCount: 0,
      starPercents: { 5: 100, 4: 0, 3: 0, 2: 0, 1: 0 }
    }
  }
  const sumRating = list.reduce((sum, r) => sum + (r.rating || 0), 0)
  const avgScore = (sumRating / total).toFixed(1)

  const goodCount = list.filter(r => r.rating >= 4).length
  const neutralCount = list.filter(r => r.rating === 3).length
  const badCount = list.filter(r => r.rating <= 2).length
  const goodRate = Math.round((goodCount / total) * 100)

  const starCounts: Record<number, number> = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 }
  list.forEach(r => {
    const star = Math.max(1, Math.min(5, r.rating || 0))
    starCounts[star]++
  })
  const starPercents: Record<number, number> = {}
  for (let i = 1; i <= 5; i++) {
    starPercents[i] = Math.round((starCounts[i] / total) * 100)
  }

  return { avgScore, goodRate, goodCount, neutralCount, badCount, starPercents }
})

/** 评价标签筛选后的列表 */
const filteredReviewList = computed(() => {
  const list = reviewList.value
  switch (reviewFilter.value) {
    case 'good':
      return list.filter(r => r.rating >= 4)
    case 'neutral':
      return list.filter(r => r.rating === 3)
    case 'bad':
      return list.filter(r => r.rating <= 2)
    default:
      return list
  }
})

/** 从路由参数获取商品ID */
function getProductId(): string {
  return String(route.params.id ?? '')
}

/** 拉取商品详情 */
async function fetchDetail(): Promise<void> {
  const id = getProductId()
  if (!id) {
    error.value = true
    return
  }
  loading.value = true
  error.value = false
  try {
    const res = await getProductDetail(id)
    product.value = res.data
    currentImageIdx.value = 0
    quantity.value = 1
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

/** 拉取评论列表 */
async function fetchReviews(): Promise<void> {
  const id = getProductId()
  if (!id) return
  reviewLoading.value = true
  try {
    const res = await getProductReviews(id, reviewPageNum.value, reviewPageSize.value)
    reviewList.value = res.data.list || []
    reviewTotal.value = res.data.total || 0
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    reviewLoading.value = false
  }
}

/** 切换 Tab */
function switchTab(tab: 'detail' | 'spec' | 'review' | 'service'): void {
  activeTab.value = tab
  if (tab === 'review') {
    fetchReviews()
  }
}

/** 切换评论分页 */
function changeReviewPage(page: number): void {
  if (page < 1 || page > reviewTotalPages.value) return
  reviewPageNum.value = page
  fetchReviews()
}

/** 提交评论 */
async function submitReview(): Promise<void> {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (!reviewForm.content.trim()) {
    ElMessage.warning('请输入评价内容')
    return
  }
  if (reviewForm.rating < 1 || reviewForm.rating > 5) {
    ElMessage.warning('评分必须为 1-5 星')
    return
  }
  const productId = getProductId()
  if (!productId) return

  reviewSubmitting.value = true
  try {
    await createReview({
      productId,
      content: reviewForm.content.trim(),
      rating: reviewForm.rating
    })
    ElMessage.success('评价发表成功')
    reviewForm.content = ''
    reviewForm.rating = 5
    reviewPageNum.value = 1
    await fetchReviews()
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    reviewSubmitting.value = false
  }
}

/** 格式化价格 */
function formatPrice(price: number): string {
  return Number(price || 0).toFixed(2)
}

/** 格式化时间 */
function formatTime(time: string | null | undefined): string {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

/* === 数量选择器 === */
function incrementQuantity(): void {
  if (!product.value) return
  if (quantity.value < product.value.stock) {
    quantity.value++
  }
}

function decrementQuantity(): void {
  if (quantity.value > 1) {
    quantity.value--
  }
}

/* === 轮播图增强 === */
function prevImage(): void {
  if (displayImages.value.length <= 1) return
  currentImageIdx.value = (currentImageIdx.value - 1 + displayImages.value.length) % displayImages.value.length
}

function nextImage(): void {
  if (displayImages.value.length <= 1) return
  currentImageIdx.value = (currentImageIdx.value + 1) % displayImages.value.length
}

function startAutoPlay(): void {
  if (displayImages.value.length <= 1) return
  stopAutoPlay()
  autoPlayTimer = setInterval(() => {
    nextImage()
  }, 4000)
}

function stopAutoPlay(): void {
  if (autoPlayTimer) {
    clearInterval(autoPlayTimer)
    autoPlayTimer = null
  }
}

function pauseAutoPlay(): void {
  stopAutoPlay()
}

function resumeAutoPlay(): void {
  startAutoPlay()
}

/** 放大镜预览：使用独立的 ElImageViewer 组件 */
function handlePreviewImage(): void {
  showImageViewer.value = true
}

/* === 评价标签筛选 === */
function switchReviewFilter(filter: 'all' | 'good' | 'neutral' | 'bad'): void {
  reviewFilter.value = filter
}

/* ==================== 立即购买 (钱包支付) ==================== */

/**
 * 立即购买：弹窗确认 → 钱包余额支付 → 跳转订单详情
 * 使用 quantity ref 的值
 */
async function handleBuyNow(): Promise<void> {
  // 1. 登录校验
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }

  // 2. 商品状态/库存校验
  if (!product.value) return
  if (product.value.status === 'OFF_SHELF') {
    ElMessage.warning('商品已下架')
    return
  }
  if (product.value.stock <= 0) {
    ElMessage.warning('商品库存不足')
    return
  }

  const productId = getProductId()
  if (!productId) {
    ElMessage.error('商品参数错误')
    return
  }

  const productName = product.value.productName
  const unitPrice = Number(product.value.originalPrice || 0)
  const buyQuantity = quantity.value
  const totalAmount = unitPrice * buyQuantity

  // 3. 拉取钱包余额
  let balance = 0
  try {
    const balRes = await getWalletBalance()
    balance = Number(balRes.data || 0)
  } catch {
    // 错误已由全局拦截器提示
    return
  }

  // 4. 余额不足：提示去充值
  if (balance < totalAmount) {
    try {
      await ElMessageBox.confirm(
        `商品「${productName}」应付 ¥${totalAmount.toFixed(2)}，钱包余额 ¥${balance.toFixed(2)}，余额不足。`,
        '余额不足',
        {
          confirmButtonText: '去充值',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      router.push('/user/wallet')
    } catch {
      // 用户取消，无需处理
    }
    return
  }

  // 5. 余额充足：弹窗确认结算
  try {
    await ElMessageBox.confirm(
      `商品：${productName}\n单价：¥${unitPrice.toFixed(2)}\n数量：${buyQuantity}\n合计：¥${totalAmount.toFixed(2)}\n钱包余额：¥${balance.toFixed(2)}`,
      '确认支付',
      {
        confirmButtonText: '确认支付',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
  } catch {
    // 用户取消支付
    return
  }

  // 6. 创建订单 → 钱包支付 → 跳转订单详情
  try {
    const createRes = await createOrder({ productId, quantity: buyQuantity })
    const orderId = createRes.data.id
    await payNormalOrder(orderId, 'WALLET')
    ElMessage.success('支付成功')
    router.push(`/user/orders/${orderId}?type=NORMAL`)
  } catch {
    // 错误已由全局拦截器提示
  }
}

/* ==================== 加入购物车 ==================== */
async function handleAddToCart(): Promise<void> {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }
  if (!product.value) return
  if (product.value.stock <= 0) {
    ElMessage.warning('商品已售罄，无法加入购物车')
    return
  }
  if (product.value.status === 'OFF_SHELF') {
    ElMessage.warning('商品已下架')
    return
  }
  const productId = getProductId()
  if (!productId) return

  addingToCart.value = true
  try {
    await addCart({ productId, quantity: quantity.value })
    ElMessage.success('已加入购物车')
    await cartStore.fetchCount()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    addingToCart.value = false
  }
}

/* ==================== 收藏 / 分享 ==================== */

/** 初始化收藏状态 (已登录时检查当前商品是否已收藏) */
async function initFavoriteStatus(): Promise<void> {
  isFavorited.value = false
  if (!userStore.isLoggedIn) return
  const productId = getProductId()
  if (!productId) return
  try {
    const res = await checkFavorite(productId)
    isFavorited.value = !!res.data
  } catch {
    // 错误已由全局拦截器提示
  }
}

/** 切换收藏状态 */
async function handleFavorite(): Promise<void> {
  // 登录校验
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }
  const productId = getProductId()
  if (!productId) {
    ElMessage.error('商品参数错误')
    return
  }

  try {
    if (isFavorited.value) {
      // 已收藏 → 取消收藏
      await removeFavorite(productId)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      // 未收藏 → 添加收藏
      await addFavorite({ productId })
      isFavorited.value = true
      ElMessage.success('收藏成功')
    }
  } catch {
    // 错误已由全局拦截器提示
  }
}

/** 分享：复制当前商品链接到剪贴板 */
async function handleShare(): Promise<void> {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(window.location.href)
    } else {
      // 兜底：使用 execCommand 兼容非安全上下文
      const input = document.createElement('input')
      input.value = window.location.href
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
    }
    ElMessage.success('商品链接已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制地址栏链接')
  }
}

watch(
  () => route.params.id,
  () => {
    if (route.name === 'ProductDetail') {
      fetchDetail()
      // 切换商品时重置 Tab 和评论
      activeTab.value = 'detail'
      reviewPageNum.value = 1
      reviewList.value = []
      reviewTotal.value = 0
      reviewFilter.value = 'all'
      quantity.value = 1
      // 切换商品时重新检查收藏状态
      initFavoriteStatus()
    }
  }
)

// 监听图片列表变化，启动自动播放
watch(displayImages, (imgs) => {
  if (imgs.length > 1) {
    startAutoPlay()
  }
})

onMounted(() => {
  fetchDetail()
  initFavoriteStatus()
})

onUnmounted(() => {
  stopAutoPlay()
})
</script>

<style scoped>
/* ============================================================
   页面基础
   ============================================================ */
.product-detail-page {
  padding: 16px 24px 24px;
}

.loading-wrap {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-line {
  height: 20px;
  background: var(--color-bg-subtle);
  border-radius: 4px;
  background-image: linear-gradient(90deg, var(--color-bg-subtle) 25%, var(--color-bg-muted) 50%, var(--color-bg-subtle) 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.4s ease infinite;
}

@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 错误状态 */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 24px;
  text-align: center;
}

.error-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--tag-timeout-bg);
  color: var(--color-danger);
  font-size: 36px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.error-title {
  font-size: 22px;
  font-weight: 800;
  margin-bottom: 8px;
}

.error-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 24px;
}

/* ============================================================
   商品主区域卡片
   ============================================================ */
.product-hero-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-card);
  padding: 24px;
  margin-bottom: 24px;
  border: 1px solid var(--color-border);
  overflow: visible;
}



/* ============================================================
   面包屑
   ============================================================ */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
  padding: 12px 0;
  margin-bottom: 16px;
}

.breadcrumb-sep {
  color: var(--color-text-muted);
  font-size: 10px;
}

.breadcrumb-current {
  color: var(--color-text-primary);
  font-weight: 600;
}

.breadcrumb-home {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: color 0.2s;
}

.breadcrumb-home:hover {
  color: var(--color-primary);
}

/* ============================================================
   详情主体布局
   ============================================================ */
.detail-grid {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 40px;
  animation: fadeInUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ============================================================
   左列：图片展示区
   ============================================================ */
.detail-left {
  position: sticky;
  top: 24px;
  align-self: start;
}

.carousel-wrap {
  position: relative;
  border-radius: var(--radius-xl);
  overflow: hidden;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.carousel-main {
  width: 100%;
  height: 480px;
  position: relative;
  overflow: hidden;
}

.carousel-image {
  width: 100%;
  height: 100%;
  display: block;
}

.carousel-image :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.carousel-image :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
}

.img-placeholder svg {
  width: 64px;
  height: 64px;
  color: var(--color-text-muted);
}

/* 图片计数标签 */
.carousel-counter {
  position: absolute;
  top: 16px;
  right: 16px;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(8px);
  color: #fff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  z-index: 5;
  letter-spacing: 0.02em;
}

/* 放大镜按钮 */
.carousel-zoom {
  position: absolute;
  bottom: 16px;
  right: 16px;
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 5;
  box-shadow: var(--shadow-md);
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.carousel-zoom:hover {
  transform: scale(1.1);
  background: #fff;
}

.carousel-zoom svg {
  width: 18px;
  height: 18px;
  color: var(--color-text-primary);
}

/* 条形指示器 */
.carousel-indicator {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 4px;
  z-index: 5;
}

.carousel-indicator span {
  height: 3px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.4);
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  cursor: pointer;
}

.carousel-indicator span.inactive {
  width: 16px;
}

.carousel-indicator span.active {
  width: 36px;
  background: #fff;
}

/* 左右箭头 */
.carousel-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 5;
  opacity: 0;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: var(--shadow-md);
}

.carousel-wrap:hover .carousel-arrow {
  opacity: 1;
}

.carousel-arrow:hover {
  background: #fff;
  transform: translateY(-50%) scale(1.08);
}

.carousel-arrow.prev { left: 12px; }
.carousel-arrow.next { right: 12px; }

.carousel-arrow svg {
  width: 18px;
  height: 18px;
  color: var(--color-text-primary);
}

/* 缩略图 72px */
.thumb-strip {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.thumb-item {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 2px solid transparent;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
}

.thumb-item::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: rgba(0, 0, 0, 0.15);
  opacity: 0;
  transition: opacity 0.2s;
}

.thumb-item:hover::after {
  opacity: 1;
}

.thumb-item.active {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.thumb-item.active::after {
  opacity: 0;
}

.thumb-image {
  width: 100%;
  height: 100%;
  display: block;
}

.thumb-image :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-image :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.thumb-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
}

.thumb-placeholder svg {
  width: 20px;
  height: 20px;
}

/* 分享/收藏 */
.action-row {
  display: flex;
  gap: 20px;
  margin-top: 16px;
  padding: 12px 0;
  border-top: 1px solid var(--color-border);
}

.action-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 6px 12px;
  border-radius: var(--radius-md);
  transition: all 0.2s;
}

.action-item:hover {
  background: var(--color-bg-subtle);
  color: var(--color-primary);
}

.action-item svg {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.action-item.favorited {
  color: var(--color-primary);
}

.action-item.favorited svg {
  fill: var(--color-primary);
  animation: heartBeat 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes heartBeat {
  0% { transform: scale(1); }
  25% { transform: scale(1.3); }
  50% { transform: scale(0.95); }
  75% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

/* ============================================================
   右列：商品信息区
   ============================================================ */
.detail-info {
  animation: fadeInUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.15s both;
}

/* 商品标题 24px */
.product-title {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.35;
  letter-spacing: -0.01em;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

/* 商品描述 */
.product-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.7;
  margin-bottom: 20px;
}

/* 价格区：渐变背景 */
.price-block {
  background: linear-gradient(135deg, var(--color-primary-light) 0%, var(--color-danger-light) 50%, var(--color-primary-light) 100%);
  border: 1px solid rgba(229, 57, 53, 0.12);
  border-radius: var(--radius-xl);
  padding: 20px 24px;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}

.price-block::before {
  content: '';
  position: absolute;
  top: -30px;
  right: -30px;
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, rgba(229, 57, 53, 0.08) 0%, transparent 70%);
  pointer-events: none;
}

.price-main-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 10px;
}

.price-currency {
  font-family: var(--font-price);
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary);
}

.price-value {
  font-family: var(--font-price);
  font-size: 36px;
  font-weight: 800;
  color: var(--color-primary);
  line-height: 1;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 700;
  margin-left: 10px;
}

.status-tag.on-sale {
  background: var(--color-success-light);
  color: var(--color-success);
}

.status-tag.off-shelf {
  background: var(--color-warning-light);
  color: var(--color-warning);
}

.price-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.price-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 信息卡片式布局 */
.info-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 20px;
}

.info-card {
  background: var(--color-bg-subtle);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  transition: all 0.2s;
}

.info-card:hover {
  background: var(--color-primary-light);
}

.info-card-label {
  font-size: 11px;
  color: var(--color-text-muted);
  font-weight: 500;
}

.info-card-value {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-primary);
}

/* 服务保障：标签式设计 */
.service-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.service-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  background: var(--color-bg-subtle);
  border-radius: 16px;
  font-size: 12px;
  color: var(--color-text-secondary);
  transition: all 0.2s;
  border: 1px solid transparent;
}

.service-tag:hover {
  border-color: var(--color-success);
  background: var(--color-success-light);
  color: var(--color-success);
}

.service-tag-icon {
  width: 14px;
  height: 14px;
  color: var(--color-success);
}

/* 数量选择器 */
.quantity-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}

.quantity-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-weight: 600;
  min-width: 40px;
}

.quantity-control {
  display: flex;
  align-items: center;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.quantity-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: var(--color-text-secondary);
  background: var(--color-bg-subtle);
  transition: all 0.2s;
  user-select: none;
  cursor: pointer;
  border: none;
}

.quantity-btn:hover {
  background: var(--color-border);
  color: var(--color-text-primary);
}

.quantity-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.quantity-input {
  width: 52px;
  height: 36px;
  text-align: center;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-primary);
  border: none;
  border-left: 1px solid var(--color-border);
  border-right: 1px solid var(--color-border);
  outline: none;
  font-family: var(--font-price);
}

.quantity-tip {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* 操作按钮：渐变+阴影 */
.action-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.action-btn {
  flex: 1;
  padding: 14px 20px;
  border-radius: var(--radius-xl);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.04em;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  overflow: hidden;
  border: none;
  cursor: pointer;
}

.action-btn.buy {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  color: #fff;
  box-shadow: 0 6px 20px rgba(229, 57, 53, 0.35);
}

.action-btn.buy:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 32px rgba(229, 57, 53, 0.4);
}

.action-btn.buy:active {
  transform: translateY(0);
}

.action-btn.buy::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, transparent 50%);
  pointer-events: none;
}

.action-btn.buy:disabled {
  background: var(--btn-disabled-bg);
  color: var(--btn-disabled-fg);
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.action-btn.buy:disabled::after {
  display: none;
}

.action-btn.cart {
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--btn-polling-fg) 100%);
  color: #fff;
  box-shadow: 0 6px 20px rgba(255, 109, 0, 0.3);
}

.action-btn.cart:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 32px rgba(255, 109, 0, 0.4);
}

.action-btn.cart:active {
  transform: translateY(0);
}

.action-btn.cart:disabled {
  background: var(--btn-disabled-bg);
  color: var(--btn-disabled-fg);
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.action-tip {
  font-size: 12px;
  color: var(--color-text-muted);
  text-align: center;
}

/* ============================================================
   Tab 区域：胶囊式
   ============================================================ */
.tab-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border);
  padding: 20px;
  margin-bottom: 24px;
}

.tab-section {
  animation: fadeInUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.3s both;
}

.tab-header {
  display: flex;
  gap: 6px;
  padding: 6px;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-xl);
  margin-bottom: 20px;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 12px 20px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
}

.tab-item:hover {
  color: var(--color-text-primary);
}

.tab-item.active {
  background: var(--color-bg-card);
  color: var(--color-primary);
  box-shadow: var(--shadow-sm);
  font-weight: 700;
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--color-primary);
  color: #fff;
  border-radius: 9px;
  font-size: 10px;
  font-weight: 700;
  margin-left: 4px;
  vertical-align: middle;
}

.tab-content-wrap {
  background: var(--color-bg-subtle);
  border-radius: var(--radius-lg);
  border: none;
  overflow: hidden;
}

.tab-content {
  padding: 28px;
  animation: tabFadeIn 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes tabFadeIn {
  from { opacity: 0; transform: translateX(8px); }
  to { opacity: 1; transform: translateX(0); }
}

/* 商品详情 */
.desc-content {
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-text-secondary);
  white-space: pre-wrap;
}

.desc-content :deep(img) {
  max-width: 100%;
  height: auto;
}

.desc-content :deep(p) {
  margin: 8px 0;
  line-height: 1.8;
}

.desc-content :deep(h1),
.desc-content :deep(h2),
.desc-content :deep(h3),
.desc-content :deep(h4) {
  margin: 16px 0 8px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.desc-content :deep(ul),
.desc-content :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.desc-content :deep(blockquote) {
  margin: 8px 0;
  padding: 8px 16px;
  border-left: 4px solid var(--color-primary);
  background: var(--color-bg-subtle);
}

.desc-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 8px 0;
}

.desc-content :deep(table td),
.desc-content :deep(table th) {
  border: 1px solid var(--color-border);
  padding: 8px;
}

.desc-empty {
  text-align: center;
  padding: 40px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* 规格参数表格 */
.spec-table {
  width: 100%;
  border-collapse: collapse;
}

.spec-table tr {
  border-bottom: 1px solid var(--color-border);
}

.spec-table tr:last-child {
  border-bottom: none;
}

.spec-table td {
  padding: 14px 16px;
  font-size: 13px;
}

.spec-table .spec-key {
  width: 140px;
  color: var(--color-text-secondary);
  font-weight: 500;
  background: var(--color-bg-subtle);
}

.spec-table .spec-val {
  color: var(--color-text-primary);
  font-weight: 600;
}

/* 评价区：评分统计概览 */
.review-summary {
  display: flex;
  gap: 32px;
  padding: 24px;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-xl);
  margin-bottom: 20px;
  align-items: center;
}

.review-score {
  text-align: center;
  min-width: 100px;
}

.review-score-value {
  font-family: var(--font-price);
  font-size: 42px;
  font-weight: 800;
  color: var(--color-primary);
  line-height: 1;
}

.review-stars-display {
  display: flex;
  gap: 2px;
  justify-content: center;
  margin-top: 6px;
}

.review-stars-display .star {
  color: #ffc107;
  font-size: 14px;
}

.review-stars-display .star.filled {
  color: #ffc107;
}

.review-score-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.review-bars {
  flex: 1;
}

.review-bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  font-size: 12px;
}

.review-bar-label {
  width: 40px;
  color: var(--color-text-secondary);
  text-align: right;
}

.review-bar-track {
  flex: 1;
  height: 6px;
  background: var(--color-border);
  border-radius: 3px;
  overflow: hidden;
}

.review-bar-fill {
  height: 100%;
  border-radius: 3px;
  background: var(--color-primary);
  transition: width 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

.review-bar-percent {
  width: 36px;
  color: var(--color-text-muted);
  font-weight: 600;
}

/* 评价标签筛选 */
.review-filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.review-filter-tag {
  padding: 6px 14px;
  border-radius: 16px;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: var(--color-bg-subtle);
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.review-filter-tag:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.review-filter-tag.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  border-color: var(--color-primary);
  font-weight: 600;
}

/* 评论表单 */
.review-form-wrap {
  background: var(--color-bg-subtle);
  padding: 16px;
  border-radius: var(--radius-xl);
  margin-bottom: 20px;
}

.review-form-title {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--color-text-primary);
}

.review-login-tip {
  font-size: 13px;
  color: var(--color-text-secondary);
  padding: 8px 0;
}

.review-login-link {
  color: var(--color-primary);
  text-decoration: none;
}

.review-login-link:hover {
  text-decoration: underline;
}

.review-form-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.review-form-label {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.rating-star-input {
  display: inline-flex;
  gap: 2px;
}

.star {
  font-size: 20px;
  color: var(--color-text-muted);
  cursor: pointer;
  user-select: none;
  transition: color 0.2s;
}

.star.filled {
  color: #f5a623;
}

.star.small {
  font-size: 12px;
  cursor: default;
}

.rating-text {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-left: 4px;
}

.review-textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  resize: vertical;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
}

.review-textarea:focus {
  border-color: var(--color-primary);
}

.review-form-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

/* 评论列表 */
.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100px;
}

.review-empty {
  text-align: center;
  padding: 40px 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.review-item {
  display: flex;
  gap: 14px;
  padding: 18px;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-xl);
  transition: all 0.2s;
}

.review-item:hover {
  background: var(--color-primary-light);
}

.review-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-accent) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}

.review-body {
  flex: 1;
  min-width: 0;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.review-user {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.review-stars {
  color: #ffc107;
  font-size: 12px;
  letter-spacing: 1px;
}

.review-text {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.7;
  margin-bottom: 6px;
  white-space: pre-wrap;
}

.review-meta {
  font-size: 11px;
  color: var(--color-text-muted);
}

.review-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.review-img {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.review-img :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.review-reply {
  margin-top: 10px;
  padding: 12px;
  background: rgba(229, 57, 53, 0.04);
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--color-primary);
}

.reply-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 4px;
}

.reply-content {
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.reply-time {
  color: var(--color-text-muted);
  margin-top: 4px;
  font-size: 11px;
}

/* 评论分页 */
.review-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
}

.page-info-text {
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* 售后保障列表 */
.service-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.service-list li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--color-border);
}

.service-list li:last-child {
  border-bottom: none;
}

.service-list-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: var(--color-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.service-list-icon svg {
  width: 18px;
  height: 18px;
  color: var(--color-primary);
}

.service-list-text h4 {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 4px;
  color: var(--color-text-primary);
}

.service-list-text p {
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

/* 小按钮 */
.btn-sm {
  padding: 5px 14px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-text-primary);
  letter-spacing: 0.02em;
}

.btn-sm.primary {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.btn-sm.primary:hover {
  background: var(--btn-hover);
}

.btn-sm:disabled {
  background: var(--btn-disabled-bg);
  color: var(--btn-disabled-fg);
  cursor: not-allowed;
  border-color: var(--btn-disabled-bg);
}

/* ============================================================
   移动端底部购买栏
   ============================================================ */
.mobile-buy-bar {
  display: none;
}

/* ============================================================
   交错淡入动画
   ============================================================ */
.stagger-1 { animation-delay: 0.05s; }
.stagger-2 { animation-delay: 0.1s; }
.stagger-3 { animation-delay: 0.15s; }
.stagger-4 { animation-delay: 0.2s; }
.stagger-5 { animation-delay: 0.25s; }

.fade-in-item {
  opacity: 0;
  transform: translateY(12px);
  animation: fadeInUp 0.5s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

/* ============================================================
   响应式适配
   ============================================================ */
@media (max-width: 1024px) {
  .detail-grid {
    grid-template-columns: 360px 1fr;
    gap: 28px;
  }

  .carousel-main {
    height: 400px;
  }
}

@media (max-width: 768px) {
  .product-hero-card {
    padding: 16px;
    border-radius: var(--radius-xl);
  }

  .detail-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .detail-left {
    position: static;
  }

  .carousel-main {
    height: 360px;
  }

  .thumb-strip {
    gap: 8px;
  }

  .thumb-item {
    width: 60px;
    height: 60px;
  }

  .product-title {
    font-size: 20px;
  }

  .price-value {
    font-size: 28px;
  }

  .info-cards {
    grid-template-columns: 1fr 1fr;
  }

  .review-summary {
    flex-direction: column;
    text-align: center;
  }

  .review-bars {
    width: 100%;
  }

  .action-buttons {
    flex-direction: column;
  }

  .tab-header {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .tab-item {
    white-space: nowrap;
    min-width: max-content;
  }

  .tab-content {
    padding: 16px;
  }

  /* 移动端隐藏桌面端操作按钮 */
  .detail-info .action-buttons {
    display: none;
  }

  .detail-info .action-tip {
    display: none;
  }

  /* 移动端固定购买栏 */
  .mobile-buy-bar {
    display: flex;
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    background: var(--color-bg-card);
    border-top: 1px solid var(--color-border);
    padding: 10px 16px;
    gap: 10px;
    z-index: 100;
    box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.08);
  }

  .mobile-buy-bar .action-btn {
    flex: 1;
    padding: 12px;
    font-size: 14px;
    border-radius: var(--radius-md);
  }

  .mobile-buy-bar .mobile-price {
    display: flex;
    flex-direction: column;
    justify-content: center;
    min-width: 80px;
  }

  .mobile-buy-bar .mobile-price-value {
    font-family: var(--font-price);
    font-size: 18px;
    font-weight: 800;
    color: var(--color-primary);
  }

  .mobile-buy-bar .mobile-price-label {
    font-size: 10px;
    color: var(--color-text-muted);
  }

  .product-detail-page {
    padding: 16px 16px 70px;
  }
}
</style>
