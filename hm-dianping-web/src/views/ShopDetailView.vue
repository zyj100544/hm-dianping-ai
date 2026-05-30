<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { fetchShopById } from '../services/shop'
import { fetchVouchersByShop, seckillVoucher } from '../services/voucher'
import { fetchShopComments, addShopComment } from '../services/shopComments'
import { resolveImg, splitImages } from '../utils/url'
import { formatDateTime } from '../utils/format'
import CommentList from '../components/CommentList.vue'

const route = useRoute()
const shop = ref(null)
const vouchers = ref([])
const comments = ref([])
const loading = ref(true)

const commentForm = ref({
  content: '',
  score: 5,
})

const images = computed(() => splitImages(shop.value?.images).map(resolveImg))

const loadAll = async () => {
  loading.value = true
  try {
    const [shopRes, voucherRes, commentRes] = await Promise.all([
      fetchShopById(route.params.id),
      fetchVouchersByShop(route.params.id),
      fetchShopComments(route.params.id, 1),
    ])
    shop.value = shopRes.data
    vouchers.value = voucherRes.data || []
    comments.value = commentRes.data || []
  } finally {
    loading.value = false
  }
}

const handleSeckill = async (voucherId) => {
  try {
    await seckillVoucher(voucherId)
    alert('抢购成功，请到我的页面查看')
  } catch (error) {
    alert(error.message || '抢购失败')
  }
}

const submitComment = async () => {
  if (!localStorage.getItem('hmdp_token')) {
    alert('请先登录再评论')
    return
  }
  if (!commentForm.value.content.trim()) {
    alert('请填写评论内容')
    return
  }
  try {
    await addShopComment({
      shopId: Number(route.params.id),
      content: commentForm.value.content.trim(),
      score: commentForm.value.score,
    })
    commentForm.value.content = ''
    await loadAll()
  } catch (error) {
    alert(error.message || '提交失败')
  }
}

onMounted(loadAll)
</script>

<template>
  <section v-if="shop" class="fade-in">
    <div class="page-title">{{ shop.name }}</div>

    <!-- Hero card -->
    <div class="card shop-hero-card">
      <div class="shop-hero">
        <img v-if="images.length" :src="images[0]" :alt="shop.name" loading="lazy" />
        <div class="hero-overlay"></div>
      </div>
      <div class="shop-info-pills">
        <div class="pill accent">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor" stroke="none"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
          {{ (shop.score || 0) / 10 }}
        </div>
        <div class="pill">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
          ¥{{ shop.avgPrice || '--' }}/人
        </div>
        <div class="pill">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          {{ shop.comments || 0 }} 评价
        </div>
      </div>
      <div class="shop-detail">
        <div class="detail-row">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="var(--muted)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
          <span>{{ shop.area }} · {{ shop.address }}</span>
        </div>
        <div class="detail-row muted">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          <span>营业时间：{{ shop.openHours || '暂无' }}</span>
        </div>
      </div>
      <div class="shop-gallery" v-if="images.length > 1">
        <div class="gallery-item" v-for="img in images.slice(1, 4)" :key="img">
          <img :src="img" alt="" loading="lazy" />
        </div>
      </div>
    </div>

    <!-- Vouchers -->
    <div class="section-title">
      <h3>本店优惠</h3>
    </div>
    <div class="grid two stagger">
      <div class="card voucher-card" v-for="voucher in vouchers" :key="voucher.id">
        <div class="voucher-tag" :class="voucher.type === 1 ? 'seckill' : 'normal'">
          <svg v-if="voucher.type === 1" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
          {{ voucher.type === 1 ? '秒杀券' : '普通券' }}
        </div>
        <div class="voucher-title">{{ voucher.title }}</div>
        <div class="muted">{{ voucher.subTitle || '限时福利' }}</div>
        <div class="voucher-price">
          <span class="price-num">¥{{ voucher.payValue / 100 }}</span>
          <span class="price-label">到手</span>
        </div>
        <div class="voucher-extra" v-if="voucher.type === 1">
          <div>{{ formatDateTime(voucher.beginTime) }} ~ {{ formatDateTime(voucher.endTime) }}</div>
          <div class="stock-info">剩余 <strong>{{ voucher.stock ?? '--' }}</strong> 张</div>
        </div>
        <button
          class="btn"
          :class="{ secondary: voucher.type !== 1 }"
          :disabled="voucher.type !== 1"
          @click="voucher.type === 1 ? handleSeckill(voucher.id) : null"
        >
          {{ voucher.type === 1 ? '立即抢购' : '暂不支持领取' }}
        </button>
      </div>
      <div class="card" v-if="vouchers.length === 0">
        <div class="muted" style="padding: 12px 0; text-align: center;">暂无优惠券</div>
      </div>
    </div>

    <!-- Comments -->
    <div class="section-title">
      <h3>店铺评论</h3>
      <span class="muted">{{ comments.length }} 条</span>
    </div>
    <div class="card comment-form">
      <textarea
        class="input"
        rows="3"
        v-model="commentForm.content"
        placeholder="写下你的体验感受..."
      ></textarea>
      <div class="comment-actions">
        <div class="score-select">
          <label class="score-label">评分</label>
          <select v-model="commentForm.score" class="input score-input">
            <option v-for="i in 5" :key="i" :value="i">{{ i }} 分</option>
          </select>
        </div>
        <button class="btn" @click="submitComment">发布评论</button>
      </div>
    </div>
    <CommentList :comments="comments" />
  </section>
  <section v-else class="card loading-card">
    <div v-if="loading" class="loading-box">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>
  </section>
</template>

<style scoped>
/* Hero card */
.shop-hero-card {
  padding: 0;
  overflow: hidden;
}

.shop-hero {
  position: relative;
  overflow: hidden;
}

.shop-hero img {
  height: 260px;
  width: 100%;
  object-fit: cover;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 50%, rgba(0, 0, 0, 0.08) 100%);
  pointer-events: none;
}

.shop-info-pills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding: 16px 18px 0;
}

.shop-info-pills .pill {
  display: flex;
  align-items: center;
  gap: 4px;
}

.shop-info-pills .pill.accent {
  background: var(--accent-glow);
  color: var(--accent);
  border-color: transparent;
  font-weight: 600;
}

.shop-detail {
  padding: 14px 18px;
  display: grid;
  gap: 8px;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--ink-2);
}

.shop-gallery {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  padding: 0 18px 18px;
}

.gallery-item {
  border-radius: 10px;
  overflow: hidden;
}

.gallery-item img {
  height: 90px;
  width: 100%;
  object-fit: cover;
  transition: transform 0.4s;
}

.gallery-item:hover img {
  transform: scale(1.05);
}

/* Voucher cards */
.voucher-card {
  display: grid;
  gap: 6px;
}

.voucher-tag {
  width: fit-content;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.voucher-tag.seckill {
  background: linear-gradient(135deg, rgba(232, 114, 42, 0.1), rgba(232, 114, 42, 0.05));
  color: var(--accent);
}

.voucher-tag.normal {
  background: var(--bg-warm);
  color: var(--muted);
}

.voucher-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--ink);
}

.voucher-price {
  margin: 8px 0 4px;
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.price-num {
  font-family: var(--display);
  font-size: 24px;
  font-weight: 700;
  color: var(--accent-deep);
}

.price-label {
  font-size: 12px;
  color: var(--muted);
}

.voucher-extra {
  font-size: 12px;
  color: var(--muted);
  display: grid;
  gap: 4px;
  margin-bottom: 10px;
}

.stock-info strong {
  color: var(--accent);
}

/* Comment form */
.comment-form {
  display: grid;
  gap: 12px;
  margin-bottom: 14px;
}

.comment-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.score-select {
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-label {
  font-size: 13px;
  color: var(--muted);
  font-weight: 500;
}

.score-input {
  width: 80px;
  padding: 8px 10px;
  font-size: 13px;
}

/* Loading */
.loading-card {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-box {
  text-align: center;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--line);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-box p {
  color: var(--muted);
  font-size: 13px;
  margin: 0;
}
</style>
