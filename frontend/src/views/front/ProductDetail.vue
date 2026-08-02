<template>
  <!-- 严格对照 index.html page-product-detail HTML 结构 -->
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
      <!-- 面包屑 -->
      <div class="breadcrumb">
        首页 &gt; {{ product.categoryName }} &gt;
        <span class="breadcrumb-current">{{ product.productName }}</span>
      </div>

      <!-- 详情主体：对照 .detail-grid 样式 -->
      <div class="detail-grid">
        <!-- 左列: 图片轮播 -->
        <div>
          <div class="detail-carousel">
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
            <!-- 轮播指示点 -->
            <div v-if="displayImages.length > 1" class="carousel-dots">
              <span
                v-for="(img, idx) in displayImages"
                :key="idx"
                :class="{ active: idx === currentImageIdx }"
                @click="currentImageIdx = idx"
              ></span>
            </div>
          </div>
          <!-- 缩略图列表 -->
          <div v-if="displayImages.length > 1" class="thumb-list">
            <div
              v-for="(img, idx) in displayImages"
              :key="idx"
              class="thumb-item"
              :class="{ active: idx === currentImageIdx }"
              @click="currentImageIdx = idx"
            >
              <el-image :src="img" fit="cover" class="thumb-image" lazy>
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
            <span class="action-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" />
              </svg>
              收藏
            </span>
            <span class="action-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="18" cy="5" r="3" />
                <circle cx="6" cy="12" r="3" />
                <circle cx="18" cy="19" r="3" />
                <path d="M8.59 13.51l6.83 3.98M15.41 6.51l-6.82 3.98" />
              </svg>
              分享
            </span>
          </div>
        </div>

        <!-- 右列: 商品信息（对照 .detail-info 样式） -->
        <div class="detail-info">
          <h2>{{ product.productName }}</h2>
          <p class="sub-name">{{ product.description || '暂无描述' }}</p>

          <!-- 价格区域 -->
          <div class="price-block">
            <div class="price-row">
              <span class="price-label">价格</span>
              <span class="price-seckill">{{ formatPrice(product.originalPrice) }}</span>
            </div>
            <div class="price-meta">
              <span>累计销量 {{ product.salesCount }}</span>
              <span>库存 {{ product.stock }} 件</span>
              <span v-if="product.status === 'ON_SALE'" class="status-on-sale">在售</span>
              <span v-else class="status-off-shelf">已下架</span>
            </div>
          </div>

          <!-- 信息列表 -->
          <dl class="detail-meta">
            <div>
              <dt>分类</dt>
              <dd>{{ product.categoryName }}</dd>
            </div>
            <div>
              <dt>库存</dt>
              <dd>{{ product.stock }} 件</dd>
            </div>
            <div>
              <dt>销量</dt>
              <dd>{{ product.salesCount }} 件</dd>
            </div>
            <div>
              <dt>上架时间</dt>
              <dd>{{ formatTime(product.createTime) }}</dd>
            </div>
          </dl>

          <!-- 服务保障 -->
          <div class="service-bar">
            <span class="service-item">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--color-success)" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
                <path d="M22 4L12 14.01l-3-3" />
              </svg>
              正品保障
            </span>
            <span class="service-item">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--color-success)" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
                <path d="M22 4L12 14.01l-3-3" />
              </svg>
              7天无理由
            </span>
            <span class="service-item">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--color-success)" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
                <path d="M22 4L12 14.01l-3-3" />
              </svg>
              运费险
            </span>
            <span class="service-item">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--color-success)" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
                <path d="M22 4L12 14.01l-3-3" />
              </svg>
              极速退款
            </span>
          </div>

          <!-- 操作按钮 -->
          <div class="action-buttons">
            <button
              class="action-btn primary"
              :disabled="product.status === 'OFF_SHELF' || product.stock <= 0"
              @click="goProductList"
            >{{ product.stock <= 0 ? '暂无库存' : '立即购买' }}</button>
            <button class="action-btn ghost" @click="router.push('/products')">返回列表</button>
          </div>
        </div>
      </div>

      <!-- 商品详情 Tab -->
      <div class="detail-tabs-wrap">
        <div class="detail-tabs">
          <div
            class="detail-tab"
            :class="{ active: activeTab === 'detail' }"
            @click="activeTab = 'detail'"
          >商品详情</div>
          <div
            class="detail-tab"
            :class="{ active: activeTab === 'spec' }"
            @click="activeTab = 'spec'"
          >规格参数</div>
          <div
            class="detail-tab"
            :class="{ active: activeTab === 'review' }"
            @click="switchTab('review')"
          >用户评价</div>
          <div
            class="detail-tab"
            :class="{ active: activeTab === 'service' }"
            @click="activeTab = 'service'"
          >售后保障</div>
        </div>
        <div class="detail-tab-content">
          <!-- 商品详情 -->
          <template v-if="activeTab === 'detail'">
            <!-- detailHtml 富文本(HTML)用 v-html 渲染，与添加商品时 wangEditor 保存格式一致 -->
            <div v-if="product.detailHtml" class="desc-content" v-html="product.detailHtml"></div>
            <!-- detailHtml 为空时回退显示 description 纯文本(用 <p> 包裹，pre-wrap 保留换行) -->
            <div v-else-if="product.description" class="desc-content">
              <p>{{ product.description }}</p>
            </div>
            <div v-else class="desc-empty">暂无商品描述</div>
          </template>

          <!-- 规格参数 -->
          <template v-else-if="activeTab === 'spec'">
            <dl class="spec-list">
              <div class="spec-row">
                <dt>商品名称</dt>
                <dd>{{ product.productName }}</dd>
              </div>
              <div class="spec-row">
                <dt>分类</dt>
                <dd>{{ product.categoryName }}</dd>
              </div>
              <div class="spec-row">
                <dt>库存</dt>
                <dd>{{ product.stock }} 件</dd>
              </div>
              <div class="spec-row">
                <dt>销量</dt>
                <dd>{{ product.salesCount }} 件</dd>
              </div>
            </dl>
          </template>

          <!-- 用户评价 -->
          <template v-else-if="activeTab === 'review'">
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
                    <span
                      v-for="star in 5"
                      :key="star"
                      class="star"
                      :class="{ filled: star <= reviewForm.rating }"
                      @click="reviewForm.rating = star"
                    >★</span>
                  </div>
                  <span class="rating-text">{{ reviewForm.rating }} 星</span>
                </div>
                <textarea
                  v-model="reviewForm.content"
                  class="review-textarea"
                  placeholder="请输入您的评价内容（最多 1000 字）"
                  maxlength="1000"
                  rows="4"
                ></textarea>
                <div class="review-form-actions">
                  <button
                    class="btn-sm primary"
                    :disabled="reviewSubmitting || !reviewForm.content.trim()"
                    @click="submitReview"
                  >{{ reviewSubmitting ? '提交中...' : '发表评价' }}</button>
                </div>
              </template>
            </div>

            <!-- 评论列表 -->
            <div class="review-list" v-loading="reviewLoading">
              <div v-if="reviewList.length === 0 && !reviewLoading" class="review-empty">
                暂无评价，快来抢沙发吧！
              </div>
              <div v-for="review in reviewList" :key="review.id" class="review-item">
                <div class="review-item-header">
                  <span class="review-user">{{ review.userName || '匿名用户' }}</span>
                  <span class="review-rating">
                    <span
                      v-for="star in 5"
                      :key="star"
                      class="star small"
                      :class="{ filled: star <= review.rating }"
                    >★</span>
                  </span>
                  <span class="review-time">{{ formatTime(review.createTime) }}</span>
                </div>
                <div class="review-content">{{ review.content }}</div>
                <div v-if="review.images && review.images.length > 0" class="review-images">
                  <el-image
                    v-for="(img, idx) in review.images"
                    :key="idx"
                    :src="img"
                    fit="cover"
                    class="review-img"
                    lazy
                  />
                </div>
                <div v-if="review.replyContent" class="review-reply">
                  <div class="reply-label">商家回复：</div>
                  <div class="reply-content">{{ review.replyContent }}</div>
                  <div class="reply-time">{{ formatTime(review.replyTime) }}</div>
                </div>
              </div>
            </div>

            <!-- 评论分页 -->
            <div v-if="reviewTotal > reviewPageSize" class="review-pagination">
              <button
                class="btn-sm"
                :disabled="reviewPageNum <= 1"
                @click="changeReviewPage(reviewPageNum - 1)"
              >上一页</button>
              <span class="page-info-text">第 {{ reviewPageNum }} 页 / 共 {{ reviewTotalPages }} 页</span>
              <button
                class="btn-sm"
                :disabled="reviewPageNum >= reviewTotalPages"
                @click="changeReviewPage(reviewPageNum + 1)"
              >下一页</button>
            </div>
          </template>

          <!-- 售后保障 -->
          <template v-else-if="activeTab === 'service'">
            <div class="service-content">
              <h4>售后保障</h4>
              <ul>
                <li>正品保障：所有商品均为正品，假一赔十</li>
                <li>7 天无理由退换货：自签收日起 7 天内可无理由退换</li>
                <li>极速退款：符合条件的退款申请 24 小时内处理</li>
                <li>运费险：退换货无忧，运费由商家承担</li>
              </ul>
            </div>
          </template>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
/**
 * P03 商品详情
 * 严格对照 index.html .detail-grid / .detail-carousel / .detail-info 样式
 */
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail } from '@/api/product'
import { getProductReviews, createReview } from '@/api/review'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import type { ProductVO, ProductReviewVO } from '@/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref<boolean>(false)
const error = ref<boolean>(false)
const product = ref<ProductVO | null>(null)
const currentImageIdx = ref<number>(0)

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

/** 评论总页数 */
const reviewTotalPages = computed(() =>
  Math.max(1, Math.ceil(reviewTotal.value / reviewPageSize.value))
)

/** 显示的图片列表 */
const displayImages = computed<string[]>(() => {
  if (!product.value) return []
  return product.value.images || []
})

/** 当前展示图片 */
const currentImage = computed<string>(() => {
  return displayImages.value[currentImageIdx.value] || ''
})

/** 从路由参数获取商品ID */
function getProductId(): number {
  return Number(route.params.id)
}

/** 拉取商品详情 */
async function fetchDetail(): Promise<void> {
  const id = getProductId()
  if (!id || Number.isNaN(id)) {
    error.value = true
    return
  }
  loading.value = true
  error.value = false
  try {
    const res = await getProductDetail(id)
    product.value = res.data
    currentImageIdx.value = 0
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

/** 拉取评论列表 */
async function fetchReviews(): Promise<void> {
  const id = getProductId()
  if (!id || Number.isNaN(id)) return
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
  if (!productId || Number.isNaN(productId)) return

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

/** 跳转商品列表 */
function goProductList(): void {
  router.push('/products')
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
    }
  }
)

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.product-detail-page {
  padding-bottom: 24px;
}

.loading-wrap {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
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

/* 面包屑 */
.breadcrumb {
  padding: 12px 24px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.breadcrumb-current {
  color: var(--color-text-primary);
}

/* 严格对照 index.html .detail-grid 样式 */
.detail-grid {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 24px;
  padding: 0 24px 24px;
}

/* 轮播图：对照 .detail-carousel 样式 */
.detail-carousel {
  background: var(--color-bg-subtle);
  border-radius: var(--radius-lg);
  width: 100%;
  height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border);
  position: relative;
  overflow: hidden;
}

.carousel-image {
  width: 100%;
  height: 100%;
  display: block;
}

/* 穿透 scoped CSS，设置 el-image 内部 img 元素的尺寸与填充方式 */
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

/* 轮播指示点 */
.carousel-dots {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
}

.carousel-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-text-muted);
  cursor: pointer;
  transition: all 0.2s;
}

.carousel-dots span.active {
  background: var(--color-primary);
  width: 20px;
  border-radius: 4px;
}

/* 缩略图列表 */
.thumb-list {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.thumb-item {
  width: 56px;
  height: 56px;
  border-radius: 4px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s;
}

.thumb-item.active {
  border: 2px solid var(--color-primary);
}

.thumb-image {
  width: 100%;
  height: 100%;
  display: block;
}

/* 穿透 scoped CSS，确保缩略图 el-image 内部 img 元素正确显示 */
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
  gap: 16px;
  margin-top: 14px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.action-item {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.action-item:hover {
  color: var(--color-primary);
}

/* 右列商品信息：对照 .detail-info 样式 */
.detail-info h2 {
  font-size: 20px;
  font-weight: 800;
  margin-bottom: 8px;
  letter-spacing: -0.01em;
  color: var(--color-text-primary);
}

.sub-name {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 16px;
  line-height: 1.6;
}

/* 价格块 */
.price-block {
  background: var(--color-bg-subtle);
  padding: 16px;
  border-radius: var(--radius-lg);
  margin-bottom: 16px;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.price-label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.price-seckill {
  font-family: var(--font-price);
  font-size: 28px;
  font-weight: 700;
  color: var(--color-primary);
}

.price-meta {
  display: flex;
  gap: 16px;
  font-size: 11px;
  color: var(--color-text-secondary);
}

.status-on-sale {
  color: var(--color-success);
  font-weight: 600;
}

.status-off-shelf {
  color: var(--color-warning);
  font-weight: 600;
}

/* 信息列表：对照 .detail-meta 样式 */
.detail-meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 20px;
  font-size: 13px;
}

.detail-meta dt {
  color: var(--color-text-secondary);
}

.detail-meta dd {
  font-weight: 600;
  color: var(--color-text-primary);
}

/* 服务保障 */
.service-bar {
  display: flex;
  gap: 16px;
  font-size: 11px;
  color: var(--color-text-secondary);
  margin-bottom: 20px;
  padding: 10px 12px;
  background: var(--color-bg-subtle);
  border-radius: 4px;
}

.service-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 12px;
}

.action-btn {
  flex: 1;
  padding: 13px;
  border: none;
  border-radius: var(--radius-lg);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  letter-spacing: 0.02em;
  transition: all 0.2s;
}

.action-btn.primary {
  background: var(--color-primary);
  color: #fff;
  box-shadow: 0 4px 12px rgba(229, 57, 53, 0.3);
}

.action-btn.primary:hover {
  background: var(--btn-hover);
  transform: translateY(-1px);
}

.action-btn.primary:disabled {
  background: var(--btn-disabled-bg);
  color: var(--btn-disabled-fg);
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.action-btn.ghost {
  border: 2px solid var(--color-primary);
  background: #fff;
  color: var(--color-primary);
}

.action-btn.ghost:hover {
  background: var(--price-bg);
}

/* 商品详情 Tab */
.detail-tabs-wrap {
  margin-top: 32px;
  padding: 0 24px;
}

.detail-tabs {
  display: flex;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-card);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  border: 1px solid var(--color-border);
  border-bottom: none;
}

.detail-tab {
  padding: 12px 28px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.detail-tab.active {
  font-weight: 700;
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.detail-tab-content {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-top: none;
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
  padding: 24px;
}

.desc-content {
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-text-secondary);
  white-space: pre-wrap;
}

/* 富文本(v-html)渲染样式：用 :deep() 穿透 scoped CSS，确保 wangEditor 产生的 HTML 正确显示 */
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

/* === 规格参数 === */
.spec-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  font-size: 13px;
}
.spec-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--color-bg-subtle);
  border-radius: 4px;
}
.spec-row dt {
  color: var(--color-text-secondary);
}
.spec-row dd {
  font-weight: 600;
  color: var(--color-text-primary);
}

/* === 售后保障 === */
.service-content h4 {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--color-text-primary);
}
.service-content ul {
  padding-left: 20px;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.8;
}
.service-content li {
  margin-bottom: 6px;
}

/* === 评论区域 === */
.review-form-wrap {
  background: var(--color-bg-subtle);
  padding: 16px;
  border-radius: var(--radius-lg);
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
  font-size: 14px;
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
  border-radius: 4px;
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
  min-height: 100px;
}
.review-empty {
  text-align: center;
  padding: 40px 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}
.review-item {
  padding: 16px 0;
  border-bottom: 1px solid var(--color-border-light, var(--color-border));
}
.review-item:last-child {
  border-bottom: none;
}
.review-item-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.review-user {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.review-rating {
  display: inline-flex;
  gap: 1px;
}
.review-time {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-left: auto;
}
.review-content {
  font-size: 13px;
  color: var(--color-text-primary);
  line-height: 1.6;
  margin-bottom: 8px;
  white-space: pre-wrap;
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
  border-radius: 4px;
  border: 1px solid var(--color-border);
}
.review-img :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.review-reply {
  background: var(--color-bg-subtle);
  padding: 10px 12px;
  border-radius: 4px;
  font-size: 12px;
  margin-top: 8px;
}
.reply-label {
  font-weight: 600;
  color: var(--color-primary);
  margin-bottom: 4px;
}
.reply-content {
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

/* 响应式 */
@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
    padding: 0 16px 16px;
  }
  .detail-meta {
    grid-template-columns: 1fr;
  }
  .detail-tabs-wrap {
    padding: 0 16px;
  }
  .spec-list {
    grid-template-columns: 1fr;
  }
}
</style>
