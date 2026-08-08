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

      <!-- 白色卡片：仅包裹详情主体 -->
      <div class="product-hero-card">
        <div class="detail-grid">
          <!-- 左列: 图片轮播 -->
          <div class="detail-left">
            <!-- 轮播图增强：480px高度 + 计数标签 + 放大镜 + 条形指示器 + 左右箭头 -->
            <div class="carousel-wrap" @mouseenter="pauseAutoPlay" @mouseleave="resumeAutoPlay">
              <div class="carousel-main">
                <el-image v-if="currentImage" :src="currentImage" fit="cover" class="carousel-image">
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
                  <circle cx="11" cy="11" r="8" />
                  <path d="m21 21-4.35-4.35" />
                </svg>
              </div>

              <!-- 条形指示器（替代圆点） -->
              <div v-if="displayImages.length > 1" class="carousel-indicator">
                <span v-for="(img, idx) in displayImages" :key="idx"
                  :class="idx === currentImageIdx ? 'active' : 'inactive'" @click="currentImageIdx = idx"></span>
              </div>

              <!-- 左右箭头 -->
              <div v-if="displayImages.length > 1" class="carousel-arrow prev" @click="prevImage">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="15,18 9,12 15,6" />
                </svg>
              </div>
              <div v-if="displayImages.length > 1" class="carousel-arrow next" @click="nextImage">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9,6 15,12 9,18" />
                </svg>
              </div>
            </div>

            <!-- 缩略图（72px） -->
            <div v-if="displayImages.length > 1" class="thumb-strip">
              <div v-for="(img, idx) in displayImages" :key="idx" class="thumb-item"
                :class="{ active: idx === currentImageIdx }" @click="currentImageIdx = idx">
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
                <span class="price-value">{{ displayPrice }}</span>
                <span v-if="product.status === 'ON_SALE'" class="status-tag on-sale">在售</span>
                <span v-else class="status-tag off-shelf">已下架</span>
              </div>
              <div class="price-stats">
                <span>累计销量 {{ product.salesCount }}</span>
                <span>好评率 {{ reviewStats.goodRate }}%</span>
                <span>库存 {{ displayStock }} 件</span>
              </div>
            </div>

            <!-- 信息卡片式布局 -->
            <div class="info-cards fade-in-item stagger-4">
              <div class="info-card">
                <span class="info-card-label">分类</span>
                <span class="info-card-value">{{categoryPath.map(c => c.categoryName).join(' > ') ||
                  product.categoryName
                }}</span>
              </div>
              <div class="info-card">
                <span class="info-card-label">库存</span>
                <span class="info-card-value">{{ displayStock }} 件</span>
              </div>
              <div class="info-card">
                <span class="info-card-label">销量</span>
                <span class="info-card-value">{{ product.salesCount }} 件</span>
              </div>
              <div class="info-card">
                <span class="info-card-label">上架时间</span>
                <span class="info-card-value">{{ formatDate(product.createTime) }}</span>
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

            <!-- SKU 规格选择器（对接后端 attributes/skus） -->
            <div v-if="hasSku" class="sku-section fade-in-item stagger-5">
              <div class="sku__group" v-for="attr in attributes" :key="attr.id">
                <div class="sku__label">
                  <span class="sku__label-name">{{ attr.name }}</span>
                  <span class="sku__label-selected">{{ selectedAttributes[attr.name] || '请选择' }}</span>
                </div>
                <div class="sku__values">
                  <!-- 图片型：色块 -->
                  <template v-if="attr.type === 'IMAGE'">
                    <div v-for="val in attr.values" :key="val.id" class="sku-color" :class="{
                      active: selectedAttributes[attr.name] === val.value,
                      disabled: isAttributeValueDisabled(attr.name, val.value)
                    }" :style="val.imageUrl ? { background: formatImageUrl(val.imageUrl) } : {}" :title="val.value"
                      @click="selectAttributeValue(attr.name, val.value)">
                      <span v-if="!val.imageUrl" class="sku-color__text">{{ val.value }}</span>
                    </div>
                  </template>
                  <!-- 文字型：按钮 -->
                  <template v-else>
                    <button v-for="val in attr.values" :key="val.id" type="button" class="sku-text" :class="{
                      active: selectedAttributes[attr.name] === val.value,
                      disabled: isAttributeValueDisabled(attr.name, val.value)
                    }" :disabled="isAttributeValueDisabled(attr.name, val.value)"
                      @click="selectAttributeValue(attr.name, val.value)">
                      {{ val.value }}
                    </button>
                  </template>
                </div>
              </div>
            </div>

            <!-- 数量选择器 -->
            <div class="quantity-row">
              <span class="quantity-label">数量</span>
              <div class="quantity-control">
                <button class="quantity-btn" :class="{ disabled: quantity <= 1 }" @click="decrementQuantity">−</button>
                <input class="quantity-input" :value="quantity" readonly />
                <button class="quantity-btn" :class="{ disabled: quantity >= displayStock }"
                  @click="incrementQuantity">+</button>
              </div>
              <span class="quantity-tip">{{ displayStock > 10 ? '库存充足' : `仅剩 ${displayStock} 件` }}</span>
            </div>

            <!-- 操作按钮：渐变+阴影 -->
            <div class="action-buttons">
              <button class="action-btn buy" :disabled="!canAddToCart" @click="handleBuyNow">
                {{ !canAddToCart ? (hasSku ? '请选择规格' : '暂无库存') : '立即购买' }}
              </button>
              <button class="action-btn cart" :disabled="addingToCart || !canAddToCart" @click="handleAddToCart">
                {{ addingToCart ? '加入中...' : '加入购物车' }}
              </button>
            </div>

            <!-- 底部提示 -->
            <p class="action-tip">已有 {{ product.salesCount }} 人购买 · 支持7天无理由退换</p>
          </div>
        </div>
      </div>

      <!-- Tab 区域：胶囊式 + 左右分栏（左栏Tab内容 / 右栏sticky侧边信息卡） -->
      <div class="tab-card">
        <div class="tab-layout">
          <!-- 左栏: Tab头 + Tab内容 -->
          <div class="tab-main">
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
                  <tbody>
                    <tr>
                      <td class="spec-key">商品名称</td>
                      <td class="spec-val">{{ product.productName }}</td>
                    </tr>
                    <tr>
                      <td class="spec-key">分类</td>
                      <td class="spec-val">{{categoryPath.map(c => c.categoryName).join(' > ') || product.categoryName}}
                      </td>
                    </tr>
                    <tr v-if="hasSku">
                      <td class="spec-key">价格区间</td>
                      <td class="spec-val">
                        ¥{{ formatPrice(product.minPrice) }} ~ ¥{{ formatPrice(product.maxPrice) }}
                      </td>
                    </tr>
                    <tr>
                      <td class="spec-key">库存</td>
                      <td class="spec-val">{{ hasSku ? (product.totalStock || 0) : product.stock }} 件</td>
                    </tr>
                    <tr>
                      <td class="spec-key">销量</td>
                      <td class="spec-val">{{ product.salesCount }} 件</td>
                    </tr>
                    <template v-if="hasSku && currentSku">
                      <tr>
                        <td class="spec-key">当前规格</td>
                        <td class="spec-val">{{ formatSkuAttributes(currentSku.attributes) }}</td>
                      </tr>
                      <tr>
                        <td class="spec-key">当前单价</td>
                        <td class="spec-val">¥{{ formatPrice(currentSku.price) }}</td>
                      </tr>
                      <tr>
                        <td class="spec-key">当前库存</td>
                        <td class="spec-val">{{ currentSku.stock }} 件</td>
                      </tr>
                    </template>
                  </tbody>
                </table>
              </div>

              <!-- 用户评价（Tab面板内完整评价 + 写评价按钮 + 弹窗） -->
              <div v-if="activeTab === 'review'" class="tab-content" :key="'review'">
                <!-- 评分概览 + 筛选标签 + 写评价按钮（设计稿 review-summary 结构） -->
                <div class="review-summary">
                  <!-- 评分 -->
                  <div class="review-score">
                    <div class="review-score__num">{{ reviewStats.avgScore }}</div>
                    <div class="review-score__stars">
                      <svg v-for="star in 5" :key="star" viewBox="0 0 24 24"
                        :fill="star <= Math.round(Number(reviewStats.avgScore)) ? 'currentColor' : 'none'"
                        :stroke="star <= Math.round(Number(reviewStats.avgScore)) ? 'none' : 'currentColor'"
                        stroke-width="2">
                        <path
                          d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
                      </svg>
                    </div>
                    <div class="review-score__count">共 {{ reviewTotal }} 条评价</div>
                  </div>
                  <!-- 筛选标签云 -->
                  <div class="review-tags">
                    <span class="review-tag" :class="{ active: reviewFilter === 'all' }"
                      @click="switchReviewFilter('all')">
                      全部 <span class="count">{{ reviewTotal }}</span>
                    </span>
                    <span class="review-tag" :class="{ active: reviewFilter === 'good' }"
                      @click="switchReviewFilter('good')">
                      好评 <span class="count">{{ reviewStats.goodCount }}</span>
                    </span>
                    <span class="review-tag" :class="{ active: reviewFilter === 'neutral' }"
                      @click="switchReviewFilter('neutral')">
                      中评 <span class="count">{{ reviewStats.neutralCount }}</span>
                    </span>
                    <span class="review-tag" :class="{ active: reviewFilter === 'bad' }"
                      @click="switchReviewFilter('bad')">
                      差评 <span class="count">{{ reviewStats.badCount }}</span>
                    </span>
                  </div>
                  <!-- 写评价按钮（点击打开弹窗） -->
                  <button class="review-write-btn" @click="openReviewModal">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 20h9M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z" />
                    </svg>
                    写评价
                  </button>
                </div>

                <!-- 评价列表（设计稿 review-list / review-item 结构） -->
                <div class="review-list" v-loading="reviewLoading">
                  <div v-if="filteredReviewList.length === 0 && !reviewLoading" class="review-empty">
                    暂无评价，快来抢沙发吧！
                  </div>
                  <div v-for="review in filteredReviewList" :key="review.id" class="review-item">
                    <div class="review-item__head">
                      <div class="review-item__avatar">{{ (review.userName || '匿')[0] }}</div>
                      <div class="review-item__user-info">
                        <div class="review-item__user">{{ review.userName || '匿名用户' }}</div>
                        <div v-if="review.skuAttributes" class="review-item__sku">{{ review.skuAttributes }}</div>
                      </div>
                      <div class="review-item__stars">
                        <svg v-for="star in 5" :key="star" viewBox="0 0 24 24"
                          :fill="star <= review.rating ? 'currentColor' : 'none'"
                          :stroke="star <= review.rating ? 'none' : 'currentColor'" stroke-width="2">
                          <path
                            d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
                        </svg>
                      </div>
                    </div>
                    <div class="review-item__content">{{ review.content }}</div>
                    <div v-if="review.images && review.images.length > 0" class="review-item__images">
                      <el-image v-for="(img, idx) in review.images" :key="idx" :src="formatImageUrl(img)" fit="cover"
                        class="review-item__img" lazy />
                    </div>
                    <div class="review-item__meta">
                      <span>{{ formatTime(review.createTime) }}</span>
                      <span v-if="review.skuAttributes">规格：{{ review.skuAttributes }}</span>
                    </div>
                    <div v-if="review.replyContent" class="review-reply">
                      <div class="reply-label">商家回复：</div>
                      <div class="reply-content">{{ review.replyContent }}</div>
                      <div v-if="review.replyTime" class="reply-time">{{ formatTime(review.replyTime) }}</div>
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
                        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
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
                        <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                        <line x1="16" y1="2" x2="16" y2="6" />
                        <line x1="8" y1="2" x2="8" y2="6" />
                        <line x1="3" y1="10" x2="21" y2="10" />
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
                        <path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6" />
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
                        <rect x="1" y="3" width="15" height="13" />
                        <polygon points="16,8 20,8 23,11 23,16 16,16 16,8" />
                        <circle cx="5.5" cy="18.5" r="2.5" />
                        <circle cx="18.5" cy="18.5" r="2.5" />
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

          <!-- 右栏: sticky侧边信息卡 -->
          <div class="tab-aside">
            <!-- 参数速览卡 -->
            <div class="aside-card">
              <h4 class="aside-title">参数速览</h4>
              <div class="aside-params">
                <div class="aside-param">
                  <span class="label">商品名称</span>
                  <span class="value">{{ product.productName }}</span>
                </div>
                <div class="aside-param">
                  <span class="label">分类</span>
                  <span class="value">{{categoryPath.map(c => c.categoryName).join(' > ') || product.categoryName
                  }}</span>
                </div>
                <div class="aside-param">
                  <span class="label">价格</span>
                  <span class="value">¥{{ displayPrice }}</span>
                </div>
                <div class="aside-param">
                  <span class="label">库存</span>
                  <span class="value">{{ displayStock }} 件</span>
                </div>
                <div class="aside-param">
                  <span class="label">销量</span>
                  <span class="value">{{ product.salesCount }} 件</span>
                </div>
                <div class="aside-param">
                  <span class="label">好评率</span>
                  <span class="value">{{ reviewStats.goodRate }}%</span>
                </div>
                <div class="aside-param">
                  <span class="label">上架时间</span>
                  <span class="value">{{ formatDate(product.createTime) }}</span>
                </div>
              </div>
            </div>

            <!-- 服务保障卡 -->
            <div class="aside-card">
              <h4 class="aside-title">服务保障</h4>
              <div class="aside-services">
                <div class="aside-service">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                  <div>
                    <strong>正品保障</strong>
                    <p>假一赔十</p>
                  </div>
                </div>
                <div class="aside-service">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                  <div>
                    <strong>7天无理由</strong>
                    <p>无忧退换</p>
                  </div>
                </div>
                <div class="aside-service">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                  <div>
                    <strong>运费险</strong>
                    <p>退换无忧</p>
                  </div>
                </div>
                <div class="aside-service">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                  <div>
                    <strong>极速退款</strong>
                    <p>最快1小时</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- 购买提示卡 -->
            <div class="aside-card aside-card-highlight">
              <h4 class="aside-title">购买提示</h4>
              <ul class="aside-tips">
                <li>已有 {{ product.salesCount }} 人购买</li>
                <li>支持7天无理由退换货</li>
                <li>正品保障·极速发货</li>
              </ul>
              <div class="aside-actions">
                <button class="aside-btn" :class="{ favorited: isFavorited }" @click="handleFavorite">
                  {{ isFavorited ? '已收藏' : '收藏' }}
                </button>
                <button class="aside-btn" @click="handleShare">分享</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 发表评价弹窗（设计稿 review-modal 结构） -->
    <div v-if="showReviewModal" class="review-modal">
      <div class="review-modal__mask" @click="closeReviewModal"></div>
      <div class="review-modal__dialog">
        <div class="review-modal__header">
          <h3>发表评价</h3>
          <button class="review-modal__close" @click="closeReviewModal" aria-label="关闭">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div class="review-modal__body">
          <!-- 未登录提示 -->
          <div v-if="!userStore.isLoggedIn" class="review-login-tip">
            请先
            <router-link to="/login" class="review-login-link">登录</router-link>
            后发表评价
          </div>
          <template v-else>
            <!-- 评分 -->
            <div class="review-form__group">
              <label class="review-form__label">评分</label>
              <div class="review-form__rating">
                <svg v-for="star in 5" :key="star" class="review-form__star"
                  :class="{ active: star <= reviewForm.rating }" viewBox="0 0 24 24" fill="currentColor"
                  @click="reviewForm.rating = star" @mouseenter="reviewHoverRating = star"
                  @mouseleave="reviewHoverRating = 0">
                  <path
                    d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
                </svg>
                <span class="review-form__rating-text">{{ (reviewHoverRating || reviewForm.rating) }}分</span>
              </div>
            </div>
            <!-- 评价内容 -->
            <div class="review-form__group">
              <label class="review-form__label">评价内容</label>
              <textarea v-model="reviewForm.content" class="review-form__textarea" rows="5" maxlength="500"
                placeholder="请分享您的使用体验，帮助其他买家做出选择（10-500字）"></textarea>
              <div class="review-form__count"><span>{{ reviewContentCount }}</span>/500</div>
            </div>
            <!-- 图片上传占位 -->
            <div class="review-form__group">
              <label class="review-form__label">晒图（可选）</label>
              <div class="review-form__images">
                <div class="review-form__upload-btn">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12" />
                  </svg>
                  <span>添加图片</span>
                </div>
              </div>
              <div class="review-form__tip">最多可上传5张图片，每张不超过5MB</div>
            </div>
          </template>
        </div>
        <div v-if="userStore.isLoggedIn" class="review-modal__footer">
          <button class="review-modal__btn review-modal__btn--cancel" @click="closeReviewModal">取消</button>
          <button class="review-modal__btn review-modal__btn--submit"
            :disabled="reviewSubmitting || !reviewForm.content.trim()" @click="submitReviewFromModal">
            {{ reviewSubmitting ? '提交中...' : '发表评价' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 图片预览（放在页面最外层，不受overflow影响） -->
    <el-image-viewer v-if="showImageViewer" :url-list="previewImageList" :initial-index="currentImageIdx"
      @close="showImageViewer = false" />

    <!-- 移动端底部购买栏（仅768px以下显示） -->
    <div v-if="product" class="mobile-buy-bar">
      <div class="mobile-price">
        <span class="mobile-price-value">¥{{ displayPrice }}</span>
        <span class="mobile-price-label">秒杀价</span>
      </div>
      <button class="action-btn cart" :disabled="addingToCart || !canAddToCart" @click="handleAddToCart">
        {{ addingToCart ? '加入中...' : '加入购物车' }}
      </button>
      <button class="action-btn buy" :disabled="!canAddToCart" @click="handleBuyNow">立即购买</button>
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


import { checkFavorite, addFavorite, removeFavorite } from '@/api/favorite'
import { addCart } from '@/api/cart'
import { getCategoryTree } from '@/api/category'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { ElMessage } from 'element-plus'
import { ElImageViewer } from 'element-plus'
import dayjs from 'dayjs'
import { formatImageUrl } from '@/utils/image'
import DOMPurify from 'dompurify'
import type { ProductVO, ProductReviewVO, ProductAttributeVO, ProductSkuVO, CategoryTreeNode } from '@/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const loading = ref<boolean>(false)
const error = ref<boolean>(false)
const product = ref<ProductVO | null>(null)
const currentImageIdx = ref<number>(0)

/* === 面包屑分类树（问题8修复） === */
// 后端 getCategoryTree 返回树形结构(一级分类对象含 children 数组)
// 接口签名是 CategoryVO[]，但实际数据满足 CategoryTreeNode 结构，做类型断言
const categoryTree = ref<CategoryTreeNode[]>([])

/** 拉取分类树，用于面包屑路径计算 */
async function fetchCategoryTree(): Promise<void> {
  try {
    const res = await getCategoryTree()
    categoryTree.value = (res.data as CategoryTreeNode[]) || []
  } catch {
    categoryTree.value = []
  }
}

/** 当前商品分类路径 (一级 + 二级)，用于面包屑显示 */
const categoryPath = computed<CategoryTreeNode[]>(() => {
  if (!product.value) return []
  const idStr = String(product.value.categoryId)
  for (const cat of categoryTree.value) {
    if (String(cat.id) === idStr) return [cat]
    if (cat.children && cat.children.length > 0) {
      const child = cat.children.find(c => String(c.id) === idStr)
      if (child) return [cat, child]
    }
  }
  return []
})

/** 跳转到分类商品列表 */
function goCategory(categoryId: number | string): void {
  router.push({ path: '/products', query: { categoryId: String(categoryId) } })
}

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

/* === 发表评价弹窗（设计稿 review-modal） === */
/** 弹窗显示状态 */
const showReviewModal = ref<boolean>(false)
/** 星级悬停预览（0 表示未悬停，恢复实际选择） */
const reviewHoverRating = ref<number>(0)
/** 评价内容字数统计 */
const reviewContentCount = computed<number>(() => reviewForm.content.length)

/* === SKU 相关状态 === */
/** 商品属性列表（来自后端 ProductVO.attributes） */
const attributes = ref<ProductAttributeVO[]>([])
/** 商品 SKU 列表（来自后端 ProductVO.skus） */
const skus = ref<ProductSkuVO[]>([])
/** 是否多规格商品 */
const hasSku = ref<boolean>(false)
/** 用户选择的属性键值对：{ 属性名: 属性值 } */
const selectedAttributes = reactive<Record<string, string>>({})
/**
 * 静态可达性地图（建议9）：Map<属性名, Set<可用属性值>>
 * 基于所有启用 SKU 预处理一次，用于 isAttributeValueDisabled 的 O(1) 静态可达性检查
 */
const staticAvailabilityMap = ref<Map<string, Set<string>>>(new Map())

/* === 轮播图自动播放 === */
let autoPlayTimer: ReturnType<typeof setInterval> | null = null

/* === 建议11 已落实：currentSku 改为 computed 派生 === */
/* 当 selectedAttributes 或 skus 变化时自动更新，无需手动调用 updateCurrentSku */
const currentSku = computed<ProductSkuVO | null>(() => {
  if (!hasSku.value) return null
  // 检查是否所有属性都已选择
  const allSelected = attributes.value.every(a => selectedAttributes[a.name])
  if (!allSelected) return null
  // 查找完全匹配的启用 SKU
  const matched = skus.value.find(sku => {
    if (sku.status !== 1) return false
    try {
      const attrs = JSON.parse(sku.attributes)
      return Object.keys(selectedAttributes).every(k => attrs[k] === selectedAttributes[k])
    } catch {
      return false
    }
  })
  return matched || null
})

/* === 当前展示价格（随 SKU 选择联动，始终返回格式化字符串） === */
const displayPrice = computed<string>(() => {
  if (!product.value) return '0.00'
  // 无规格：取商品原价
  if (!hasSku.value) return Number(product.value.originalPrice || 0).toFixed(2)
  // SKU 完整选中：取当前 SKU 价格
  if (currentSku.value) return Number(currentSku.value.price || 0).toFixed(2)
  // 未完整选择：展示价格区间
  const min = product.value.minPrice
  const max = product.value.maxPrice
  if (min != null && max != null) {
    return min === max
      ? Number(min).toFixed(2)
      : `${Number(min).toFixed(2)} - ${Number(max).toFixed(2)}`
  }
  return Number(product.value.originalPrice || 0).toFixed(2)
})

/* === 当前展示库存（随 SKU 选择联动） === */
const displayStock = computed<number>(() => {
  if (!product.value) return 0
  // 无规格：取商品库存
  if (!hasSku.value) return product.value.stock || 0
  // SKU 完整选中：取当前 SKU 库存
  if (currentSku.value) return currentSku.value.stock
  // 未完整选择：取总库存
  return product.value.totalStock || 0
})

/* === 是否可加购 / 购买 === */
const canAddToCart = computed<boolean>(() => {
  if (!product.value) return false
  if (product.value.status === 'OFF_SHELF') return false
  // 多规格商品必须完整选择规格
  if (hasSku.value && !currentSku.value) return false
  // 当前 SKU 库存为 0
  if (currentSku.value && currentSku.value.stock <= 0) return false
  // 无规格商品库存为 0
  if (!hasSku.value && product.value.stock <= 0) return false
  return true
})

/** 评论总页数 */
const reviewTotalPages = computed(() =>
  Math.max(1, Math.ceil(reviewTotal.value / reviewPageSize.value))
)

/** 显示的图片列表（SKU 选中且有主图时，SKU 主图优先置于首位） */
const displayImages = computed<string[]>(() => {
  if (!product.value) return []
  const baseImages = product.value.images || []
  // SKU 选中且有专属主图：将 SKU 主图置于首位（去重）
  const skuImg = currentSku.value?.mainImage
  if (skuImg) {
    if (baseImages.includes(skuImg)) return baseImages
    return [skuImg, ...baseImages]
  }
  return baseImages
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
    // === 7.1.1 SKU 选择器对接后端 ===
    // 从商品详情接口获取 attributes / skus / hasSku
    attributes.value = res.data?.attributes || []
    skus.value = res.data?.skus || []
    hasSku.value = !!res.data?.hasSku
    // 清空旧选择
    Object.keys(selectedAttributes).forEach(k => delete selectedAttributes[k])
    // 初始化选择：默认选第一个属性值
    attributes.value.forEach(attr => {
      if (attr.values && attr.values.length > 0) {
        selectedAttributes[attr.name] = attr.values[0].value
      }
    })
    // 建议9：预处理静态可达性地图（基于所有启用 SKU）
    buildStaticAvailabilityMap()
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

/* === 建议9 已落实：构建静态可达性地图 === */
/* 遍历所有启用 SKU，收集每个属性名下出现过的属性值，用于 O(1) 静态可达性检查 */
function buildStaticAvailabilityMap(): void {
  const map = new Map<string, Set<string>>()
  for (const sku of skus.value) {
    if (sku.status !== 1) continue
    try {
      const attrs = JSON.parse(sku.attributes)
      for (const [k, v] of Object.entries(attrs)) {
        if (!map.has(k)) map.set(k, new Set())
        map.get(k)!.add(v as string)
      }
    } catch {
      /* ignore JSON parse error */
    }
  }
  staticAvailabilityMap.value = map
}

/* === 选择属性值（建议9+11 已落实） === */
function selectAttributeValue(attrName: string, value: string): void {
  if (isAttributeValueDisabled(attrName, value)) return
  selectedAttributes[attrName] = value
  // 建议11：无需手动调用 updateCurrentSku，currentSku 是 computed 自动派生
}

/* === 建议9 已落实：判断属性值是否禁用（基于可达性地图，O(1) 静态查找 + 动态匹配） === */
function isAttributeValueDisabled(attrName: string, value: string): boolean {
  // 第一层：静态可达性检查（O(1)）
  const staticSet = staticAvailabilityMap.value.get(attrName)
  if (!staticSet || !staticSet.has(value)) return true

  // 第二层：动态可达性检查（基于当前其他属性的选择）
  // 临时选中该值，检查是否存在启用的 SKU 且库存 > 0
  const tempSelected = { ...selectedAttributes, [attrName]: value }
  const matched = skus.value.filter(sku => {
    if (sku.status !== 1) return false
    try {
      const attrs = JSON.parse(sku.attributes)
      return Object.keys(tempSelected).every(k => attrs[k] === tempSelected[k])
    } catch {
      return false
    }
  })
  return matched.length === 0 || matched.every(s => s.stock === 0)
}

/** 格式化 SKU 属性 JSON 字符串为可读文本（如 '{"颜色":"曜石黑","版本":"标准版"}' -> '曜石黑 / 标准版'） */
function formatSkuAttributes(attrJson: string | null | undefined): string {
  if (!attrJson) return ''
  try {
    const attrs = JSON.parse(attrJson)
    return Object.values(attrs).join(' / ')
  } catch {
    return ''
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
      // 7.4.2 发表评论携带 skuId（多规格商品选中 SKU 时带入，无规格商品传 null）
      skuId: currentSku.value?.id || null,
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

/* === 发表评价弹窗交互（设计稿 review-modal） === */
/** 打开弹窗：未登录则引导登录 */
function openReviewModal(): void {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }
  // 重置表单状态
  reviewForm.rating = 5
  reviewForm.content = ''
  reviewHoverRating.value = 0
  showReviewModal.value = true
}

/** 关闭弹窗 */
function closeReviewModal(): void {
  showReviewModal.value = false
  reviewHoverRating.value = 0
}

/** 弹窗内提交评价：复用已有 submitReview 逻辑，提交成功后关闭弹窗 */
async function submitReviewFromModal(): Promise<void> {
  await submitReview()
  // 提交成功（showReviewModal 仍为 true 且未抛出异常）后关闭弹窗
  if (showReviewModal.value) {
    closeReviewModal()
  }
}

/** 格式化价格 */
function formatPrice(price: number | undefined | null): string {
  return Number(price || 0).toFixed(2)
}

/** 格式化时间 */
function formatTime(time: string | null | undefined): string {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

/** 格式化日期（仅年月日） */
function formatDate(time: string | null | undefined): string {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD')
}

/* === 数量选择器 === */
function incrementQuantity(): void {
  if (!product.value) return
  // 7.1.3 数量选择器上限随 SKU 库存变化
  if (quantity.value < displayStock.value) {
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

/* ==================== 立即购买 (跳转结算页选择地址) ==================== */

/**
 * 立即购买：校验 → 写入 sessionStorage → 跳转 /checkout?mode=buynow 结算确认页
 * 由 Checkout.vue 统一处理收货地址选择 + 支付方式选择 + 创建订单
 * 使用 quantity ref 的值
 */
async function handleBuyNow(): Promise<void> {
  // 1. 登录校验
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }

  // 2. 商品状态/库存校验（含 SKU 规格校验）
  if (!product.value) return
  if (!canAddToCart.value) {
    ElMessage.warning(hasSku.value ? '请选择完整规格' : '商品不可购买')
    return
  }

  const productId = getProductId()
  if (!productId) {
    ElMessage.error('商品参数错误')
    return
  }

  // 7.1.2 立即购买携带 skuId；单价随 SKU 选择联动
  const skuId = currentSku.value?.id || null
  const unitPrice = hasSku.value && currentSku.value
    ? Number(currentSku.value.price || 0)
    : Number(product.value.originalPrice || 0)
  const buyQuantity = quantity.value

  // 3. 主图：SKU 主图优先，否则取商品首图
  const mainImage = currentSku.value?.mainImage
    || product.value.images?.[0]
    || ''

  // 4. 写入 sessionStorage (结构与 Cart.vue handleCheckout 写入一致, 供 Checkout.vue 读取)
  //    立即购买模式无 cartId, 使用 `buynow_${productId}` 占位; Checkout.vue 在 buynow 模式下不会使用 cartId
  const checkoutItem = {
    cartId: `buynow_${productId}`,
    productId,
    productName: product.value.productName,
    mainImage,
    price: unitPrice,
    quantity: buyQuantity,
    subtotal: unitPrice * buyQuantity
  }
  try {
    sessionStorage.setItem('checkout_items', JSON.stringify([checkoutItem]))
  } catch {
    ElMessage.error('结算数据保存失败，请重试')
    return
  }

  // 5. 跳转结算确认页, 通过 query 传递立即购买模式 + 商品参数
  router.push({
    path: '/checkout',
    query: {
      mode: 'buynow',
      productId: String(productId),
      skuId: skuId ? String(skuId) : '',
      quantity: String(buyQuantity)
    }
  })
}

/* ==================== 加入购物车 ==================== */
async function handleAddToCart(): Promise<void> {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }
  if (!product.value) return
  // 7.1.2 加入购物车携带 skuId；含 SKU 规格校验
  if (!canAddToCart.value) {
    ElMessage.warning(hasSku.value ? '请选择完整规格' : '商品不可加购')
    return
  }
  const productId = getProductId()
  if (!productId) return

  // 多规格商品携带 skuId，无规格商品传 null
  const skuId = currentSku.value?.id || null

  addingToCart.value = true
  try {
    await addCart({ productId, skuId, quantity: quantity.value })
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

// 7.1.1 SKU 选择联动：当 currentSku 变化时（用户切换规格），
// 重置轮播图为第一张（SKU 主图优先展示），并校正数量不超过新库存
watch(currentSku, () => {
  currentImageIdx.value = 0
  // 数量超过当前库存时，回退到库存上限
  if (quantity.value > displayStock.value) {
    quantity.value = Math.max(1, displayStock.value)
  }
})

onMounted(() => {
  fetchDetail()
  initFavoriteStatus()
  fetchCategoryTree()
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
  /* 与首页 .page-home 一致：仅控制左右及底部留白，顶部由各子区域自行控制 */
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
  0% {
    background-position: 200% 0;
  }

  100% {
    background-position: -200% 0;
  }
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

.breadcrumb-link {
  cursor: pointer;
  transition: color 0.2s;
}

.breadcrumb-link:hover {
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
  from {
    opacity: 0;
    transform: translateY(20px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
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

.carousel-arrow.prev {
  left: 12px;
}

.carousel-arrow.next {
  right: 12px;
}

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
  0% {
    transform: scale(1);
  }

  25% {
    transform: scale(1.3);
  }

  50% {
    transform: scale(0.95);
  }

  75% {
    transform: scale(1.15);
  }

  100% {
    transform: scale(1);
  }
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

/* Tab区域左右分栏布局 */
.tab-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 20px;
}

.tab-main {
  min-width: 0;
  /* 防止内容溢出 */
  animation: fadeInUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.3s both;
}

/* 右栏侧边信息卡 */
.tab-aside {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 24px;
  align-self: start;
}

.aside-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 16px;
}

.aside-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border);
}

/* 参数速览 */
.aside-params {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.aside-param {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
}

.aside-param .label {
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.aside-param .value {
  color: var(--color-text-primary);
  font-weight: 500;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 服务保障 */
.aside-services {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.aside-service {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.aside-service svg {
  width: 18px;
  height: 18px;
  color: var(--color-primary);
  flex-shrink: 0;
  margin-top: 2px;
}

.aside-service strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  display: block;
}

.aside-service p {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin: 2px 0 0;
}

/* 购买提示卡 */
.aside-card-highlight {
  background: linear-gradient(135deg, #fff5f5 0%, #ffffff 100%);
  border-color: var(--color-primary-light);
}

.aside-tips {
  list-style: none;
  padding: 0;
  margin: 0 0 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.aside-tips li {
  font-size: 13px;
  color: var(--color-text-secondary);
  padding-left: 16px;
  position: relative;
}

.aside-tips li::before {
  content: '•';
  color: var(--color-primary);
  position: absolute;
  left: 0;
  font-weight: bold;
}

.aside-actions {
  display: flex;
  gap: 8px;
}

.aside-btn {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-card);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  cursor: pointer;
  transition: all 0.2s;
}

.aside-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.aside-btn.favorited {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light);
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
  from {
    opacity: 0;
    transform: translateX(8px);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
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

/* ============================================================
   评价区：评分概览 + 筛选标签 + 写评价按钮（设计稿 review-summary）
   ============================================================ */
.review-summary {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  padding: 20px;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-md);
  margin-bottom: 20px;
}

.review-score {
  text-align: center;
  width: 200px;
  flex-shrink: 0;
}

.review-score__num {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-primary);
  font-family: var(--font-price);
  line-height: 1;
}

.review-score__stars {
  display: flex;
  justify-content: center;
  gap: 2px;
  color: #ff9500;
  margin: 6px 0;
}

.review-score__stars svg {
  width: 14px;
  height: 14px;
}

.review-score__count {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* 筛选标签云 */
.review-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-content: center;
  flex: 1;
}

.review-tag {
  padding: 4px 12px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 12px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.review-tag:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.review-tag.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  border-color: var(--color-primary);
  font-weight: 600;
}

.review-tag .count {
  color: var(--color-text-muted);
  margin-left: 4px;
}

.review-tag.active .count {
  color: var(--color-primary);
}

/* 写评价按钮 */
.review-write-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  background: #e53935;
  border: none;
  border-radius: 4px;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin-left: auto;
  align-self: flex-start;
}

.review-write-btn svg {
  width: 16px;
  height: 16px;
}

.review-write-btn:hover {
  background: #c62828;
}

/* ============================================================
   评价列表（设计稿 review-list / review-item 结构）
   ============================================================ */
.review-list {
  display: grid;
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
  padding: 16px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  transition: all 0.2s;
}

.review-item:hover {
  border-color: var(--color-primary);
  box-shadow: 0 2px 8px rgba(229, 57, 53, 0.06);
}

.review-item__head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.review-item__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e53935, #ff6d00);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.review-item__user-info {
  min-width: 0;
}

.review-item__user {
  font-size: 13px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.review-item__sku {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.review-item__stars {
  display: flex;
  gap: 2px;
  color: #ff9500;
  margin-left: auto;
  align-items: center;
}

.review-item__stars svg {
  width: 12px;
  height: 12px;
}

.review-item__content {
  font-size: 14px;
  color: var(--color-text-primary);
  line-height: 1.7;
  margin-bottom: 10px;
  white-space: pre-wrap;
}

.review-item__images {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.review-item__img {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border-light);
  cursor: pointer;
}

.review-item__img :deep(.el-image__inner),
.review-item__img :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.review-item__meta {
  font-size: 12px;
  color: var(--color-text-muted);
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
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

/* ============================================================
   发表评价弹窗（设计稿 review-modal / review-form）
   ============================================================ */
.review-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 9999;
}

.review-modal__mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
}

.review-modal__dialog {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 560px;
  max-width: 90vw;
  max-height: 90vh;
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.review-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.review-modal__header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.review-modal__close {
  background: none;
  border: none;
  padding: 4px;
  cursor: pointer;
  color: #9ca3af;
  display: flex;
  align-items: center;
  justify-content: center;
}

.review-modal__close svg {
  width: 20px;
  height: 20px;
}

.review-modal__close:hover {
  color: #1f2937;
}

.review-modal__body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.review-form__group {
  margin-bottom: 20px;
}

.review-form__label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}

.review-form__rating {
  display: flex;
  align-items: center;
  gap: 4px;
}

.review-form__star {
  width: 28px;
  height: 28px;
  color: #e0e0e0;
  cursor: pointer;
  transition: color 0.15s;
}

.review-form__star.active {
  color: #ff9500;
}

.review-form__star:hover {
  color: #ff9500;
}

.review-form__rating-text {
  margin-left: 8px;
  font-size: 14px;
  color: #ff9500;
  font-weight: 500;
}

.review-form__textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.6;
  color: #1f2937;
  resize: vertical;
  min-height: 100px;
  font-family: inherit;
  box-sizing: border-box;
  outline: none;
}

.review-form__textarea:focus {
  border-color: #e53935;
  box-shadow: 0 0 0 2px rgba(229, 57, 53, 0.1);
}

.review-form__count {
  text-align: right;
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.review-form__images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.review-form__upload-btn {
  width: 80px;
  height: 80px;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.2s;
}

.review-form__upload-btn svg {
  width: 24px;
  height: 24px;
}

.review-form__upload-btn span {
  font-size: 12px;
}

.review-form__upload-btn:hover {
  border-color: #e53935;
  color: #e53935;
}

.review-form__tip {
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.review-modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
}

.review-modal__btn {
  padding: 8px 24px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.review-modal__btn--cancel {
  background: #fff;
  border-color: #d9d9d9;
  color: #6b7280;
}

.review-modal__btn--cancel:hover {
  border-color: #9ca3af;
  color: #1f2937;
}

.review-modal__btn--submit {
  background: #e53935;
  color: #fff;
}

.review-modal__btn--submit:hover {
  background: #c62828;
}

.review-modal__btn--submit:disabled {
  background: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
}

/* 弹窗内未登录提示 */
.review-login-tip {
  font-size: 14px;
  color: var(--color-text-secondary);
  padding: 16px 0;
  text-align: center;
}

.review-login-link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 500;
}

.review-login-link:hover {
  text-decoration: underline;
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
.stagger-1 {
  animation-delay: 0.05s;
}

.stagger-2 {
  animation-delay: 0.1s;
}

.stagger-3 {
  animation-delay: 0.15s;
}

.stagger-4 {
  animation-delay: 0.2s;
}

.stagger-5 {
  animation-delay: 0.25s;
}

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

  .tab-layout {
    grid-template-columns: 1fr;
  }

  .tab-aside {
    position: static;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .tab-aside .aside-card {
    flex: 1;
    min-width: 280px;
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

  .review-score {
    width: auto;
  }

  .review-write-btn {
    margin-left: 0;
    align-self: center;
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

  .tab-aside {
    flex-direction: column;
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

/* ============================================================
   SKU 规格选择器（7.1.1）
   ============================================================ */
.sku-section {
  margin-bottom: 20px;
  padding: 16px;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border-light);
}

.sku__group {
  margin-bottom: 16px;
}

.sku__group:last-child {
  margin-bottom: 0;
}

.sku__label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 13px;
}

.sku__label-name {
  color: var(--color-text-secondary);
  font-weight: 600;
}

.sku__label-selected {
  color: var(--color-primary);
  font-weight: 700;
}

.sku__values {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

/* 文字型属性按钮 */
.sku-text {
  padding: 6px 16px;
  font-size: 13px;
  color: var(--color-text-primary);
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1.4;
}

.sku-text:hover:not(.disabled):not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.sku-text.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
  font-weight: 600;
}

.sku-text.disabled,
.sku-text:disabled {
  color: var(--color-text-muted);
  background: var(--color-bg-subtle);
  border-color: var(--color-border-light);
  cursor: not-allowed;
  opacity: 0.5;
}

/* 图片型属性色块 */
.sku-color {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid var(--color-border);
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  background-size: cover;
  background-position: center;
  flex-shrink: 0;
}

.sku-color:hover:not(.disabled) {
  border-color: var(--color-primary);
  transform: scale(1.08);
}

.sku-color.active {
  border-color: var(--color-primary);
  border-width: 3px;
  box-shadow: 0 0 0 2px rgba(229, 57, 53, 0.15);
}

.sku-color.disabled {
  opacity: 0.35;
  cursor: not-allowed;
  position: relative;
}

.sku-color.disabled::after {
  content: '';
  position: absolute;
  width: 70%;
  height: 2px;
  background: var(--color-text-muted);
  transform: rotate(-45deg);
}

.sku-color__text {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.sku-color.active .sku-color__text {
  color: #fff;
}

/* ============================================================
   评论 SKU 属性展示（7.4.1）
   ============================================================ */
.review-item__sku {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
  margin: 4px 0;
  padding: 2px 8px;
  background: var(--color-bg-subtle);
  border-radius: 4px;
  border: 1px solid var(--color-border-light);
}

.review-item__sku-icon {
  width: 12px;
  height: 12px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}
</style>
