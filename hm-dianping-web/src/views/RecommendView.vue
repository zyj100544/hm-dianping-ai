<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { fetchRecommend } from '../services/recommend'

const loading = ref(true)
const generating = ref(false)
const shops = ref([])
let timer = null

async function load() {
  try {
    const res = await fetchRecommend()
    if (Array.isArray(res.data)) {
      shops.value = res.data
      loading.value = false
      generating.value = false
      if (timer) { clearInterval(timer); timer = null }
    } else if (typeof res.data === 'string') {
      generating.value = true
      loading.value = false
      if (!timer) {
        timer = setInterval(load, 3000)
      }
    }
  } catch {
    loading.value = false
  }
}

onMounted(load)
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<template>
  <div class="recommend-page">
    <div class="page-header">
      <h1 class="page-title">智能推荐</h1>
      <p class="muted">基于你的口味和消费习惯，为你精选</p>
    </div>

    <div v-if="loading" class="state-box">
      <div class="spinner-ai"></div>
      <p>正在分析你的偏好...</p>
    </div>

    <div v-else-if="generating" class="state-box">
      <div class="spinner-ai"></div>
      <p class="generating-text">AI 正在为你生成推荐</p>
      <p class="generating-dots"><span>.</span><span>.</span><span>.</span></p>
    </div>

    <div v-else-if="shops.length === 0" class="state-box">
      <div class="empty-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--line-strong)" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/></svg>
      </div>
      <p>暂无推荐，多评论几家店铺后我就能为你推荐啦</p>
    </div>

    <div v-else class="stagger list">
      <RouterLink
        v-for="item in shops"
        :key="item.shopId"
        :to="`/shop/${item.shopId}`"
        class="recommend-card card"
      >
        <div class="rc-header">
          <div class="rc-info">
            <h3 class="rc-name">{{ item.shopName }}</h3>
            <span class="tag">{{ item.shopType }}</span>
          </div>
          <div class="rc-score">
            <span class="score-num">{{ ((item.score || 0) / 10).toFixed(1) }}</span>
            <span class="score-label">评分</span>
          </div>
        </div>
        <div class="rc-meta">
          <span>¥{{ item.avgPrice || '--' }}<small>/人</small></span>
        </div>
        <div class="rc-reason">
          <span class="reason-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
          </span>
          <span>{{ item.reason }}</span>
        </div>
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.recommend-page {
  max-width: 640px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header .page-title {
  margin-bottom: 6px;
}

.state-box {
  text-align: center;
  padding: 80px 20px;
  color: var(--muted);
}

.state-box p {
  margin: 0;
}

/* AI spinner */
.spinner-ai {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: conic-gradient(var(--accent), #667eea, #764ba2, var(--accent));
  position: relative;
  margin: 0 auto 20px;
  animation: spinRing 1.2s linear infinite;
}

.spinner-ai::after {
  content: '';
  position: absolute;
  inset: 4px;
  background: var(--bg);
  border-radius: 50%;
}

@keyframes spinRing {
  to { transform: rotate(360deg); }
}

.generating-text {
  font-size: 14px;
}

.generating-dots span {
  animation: dotPulse 1.4s ease-in-out infinite;
  opacity: 0;
}

.generating-dots span:nth-child(2) { animation-delay: 0.2s; }
.generating-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0; }
  40% { opacity: 1; }
}

.empty-icon {
  margin-bottom: 16px;
}

/* Recommend cards */
.recommend-card {
  display: block;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.recommend-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}

.rc-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.rc-info {
  flex: 1;
}

.rc-name {
  margin: 0 0 8px;
  font-family: var(--display);
  font-size: 19px;
  font-weight: 600;
  color: var(--ink);
}

.rc-score {
  text-align: center;
  flex-shrink: 0;
  background: var(--accent-glow);
  border-radius: 12px;
  padding: 8px 12px;
}

.score-num {
  font-size: 24px;
  font-weight: 700;
  color: var(--accent);
  font-family: var(--display);
  display: block;
  line-height: 1;
}

.score-label {
  font-size: 11px;
  color: var(--muted);
  display: block;
  margin-top: 2px;
}

.rc-meta {
  margin-top: 10px;
  font-size: 14px;
  color: var(--accent-deep);
  font-weight: 600;
}

.rc-meta small {
  font-size: 11px;
  font-weight: 400;
  color: var(--muted);
}

.rc-reason {
  margin-top: 14px;
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(232, 114, 42, 0.06), rgba(102, 126, 234, 0.04));
  border-radius: 12px;
  font-size: 14px;
  color: var(--ink-2);
  display: flex;
  align-items: flex-start;
  gap: 10px;
  line-height: 1.6;
  border: 1px solid rgba(232, 114, 42, 0.08);
}

.reason-icon {
  flex-shrink: 0;
  color: var(--accent);
  margin-top: 2px;
}

@keyframes spinRing {
  to { transform: rotate(360deg); }
}
</style>
