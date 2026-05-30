<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchShopsByName, searchShopsByAI } from '../services/shop'
import EmptyState from '../components/EmptyState.vue'
import { resolveImg, splitImages } from '../utils/url'

const route = useRoute()
const router = useRouter()
const keyword = ref('')
const shops = ref([])
const loading = ref(false)
const searched = ref(false)
const searchMode = ref('ai') // 'ai' | 'keyword'
const fallback = ref(false)

const doSearch = async () => {
  const q = keyword.value.trim()
  if (!q) return
  loading.value = true
  searched.value = true
  fallback.value = false
  try {
    if (searchMode.value === 'ai') {
      const res = await searchShopsByAI(q, 10)
      const data = res.data || []
      if (data.length > 0 && data[0].shop) {
        shops.value = data
      } else {
        shops.value = data.map(shop => ({ shop, relevance: null }))
        fallback.value = data.length > 0
      }
    } else {
      const res = await searchShopsByName(q, 1)
      shops.value = (res.data || []).map(shop => ({ shop, relevance: null }))
    }
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

const switchMode = (mode) => {
  searchMode.value = mode
  if (searched.value && keyword.value.trim()) {
    doSearch()
  }
}

const coverImg = (shop) => {
  const images = splitImages(shop.images)
  return images.length ? resolveImg(images[0]) : ''
}

const scoreText = (score) => ((score || 0) / 10).toFixed(1)

const relevanceLabel = (score) => {
  if (score == null) return ''
  return (score * 100).toFixed(0) + '%'
}

const relevanceClass = (score) => {
  if (score == null) return ''
  if (score >= 0.85) return 'high'
  if (score >= 0.6) return 'mid'
  return 'low'
}

onMounted(() => {
  keyword.value = (route.query.q || '').trim()
  if (keyword.value) doSearch()
})
</script>

<template>
  <div class="search-page fade-in">
    <div class="search-bar">
      <button class="back-btn" @click="goBack">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <div class="search-input-wrap">
        <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input
          class="search-input"
          v-model="keyword"
          :placeholder="searchMode === 'ai' ? '试试输入自然语言，如：适合约会的浪漫餐厅' : '输入店铺名称搜索'"
          @keyup.enter="doSearch"
        />
        <button v-if="keyword" class="clear-btn" @click="keyword = ''; searched = false; shops = []">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <button class="search-btn" @click="doSearch">搜索</button>
    </div>

    <div class="mode-tabs">
      <button
        class="mode-tab"
        :class="{ active: searchMode === 'ai' }"
        @click="switchMode('ai')"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
        智能搜索
      </button>
      <button
        class="mode-tab"
        :class="{ active: searchMode === 'keyword' }"
        @click="switchMode('keyword')"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        精确搜索
      </button>
    </div>

    <div class="search-body">
      <template v-if="loading">
        <div class="loading-box">
          <div class="spinner"></div>
          <p>{{ searchMode === 'ai' ? 'AI 正在为你匹配...' : '搜索中...' }}</p>
        </div>
      </template>

      <template v-else-if="searched && shops.length > 0">
        <div class="result-header">
          <span v-if="searchMode === 'ai' && !fallback">
            <span class="result-badge">AI</span> 智能匹配 <span class="count">{{ shops.length }}</span> 个结果
          </span>
          <span v-else-if="fallback">关键词匹配 <span class="count">{{ shops.length }}</span> 个结果（AI 服务暂不可用）</span>
          <span v-else>找到 <span class="count">{{ shops.length }}</span> 个结果</span>
        </div>
        <div class="result-list stagger">
          <RouterLink
            v-for="item in shops"
            :key="item.shop.id"
            :to="`/shop/${item.shop.id}`"
            class="result-card"
          >
            <div class="card-img" v-if="coverImg(item.shop)">
              <img :src="coverImg(item.shop)" :alt="item.shop.name" loading="lazy" />
            </div>
            <div class="card-info">
              <div class="card-name-row">
                <span class="card-name">{{ item.shop.name }}</span>
                <span
                  v-if="item.relevance != null"
                  class="relevance-badge"
                  :class="relevanceClass(item.relevance)"
                >{{ relevanceLabel(item.relevance) }} 匹配</span>
              </div>
              <div class="card-rating">
                <span class="stars">
                  <span v-for="i in 5" :key="i" class="star" :class="{ on: i <= Math.round((item.shop.score || 0) / 10) }">★</span>
                </span>
                <span class="score">{{ scoreText(item.shop.score) }}</span>
                <span class="comment-count">{{ item.shop.comments || 0 }}条评价</span>
              </div>
              <div class="card-meta">
                <span class="price">¥{{ item.shop.avgPrice || '--' }}<small>/人</small></span>
                <span class="area" v-if="item.shop.area">{{ item.shop.area }}</span>
              </div>
              <div class="card-tags" v-if="item.shop.tags">
                <span class="tag" v-for="t in (item.shop.tags || '').split(',')" :key="t">{{ t }}</span>
              </div>
            </div>
          </RouterLink>
        </div>
      </template>

      <EmptyState
        v-else-if="searched && shops.length === 0"
        title="未找到相关店铺"
        subtitle="换个关键词试试吧"
      />

      <div v-else class="search-hint">
        <div class="hint-icon">
          <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="var(--line-strong)" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        </div>
        <div class="hint-text">{{ searchMode === 'ai' ? '试试"适合约会的浪漫餐厅"、"带娃好玩的地方"' : '输入店铺名称开始搜索' }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-page {
  max-width: 720px;
  margin: 0 auto;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--bg);
  padding: 10px 0;
}

.back-btn {
  border: none;
  background: var(--bg-warm);
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--ink-2);
  flex-shrink: 0;
  transition: all 0.2s;
}

.back-btn:hover {
  background: var(--line);
  transform: translateX(-2px);
}

.search-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 0 14px;
  height: 42px;
  transition: all 0.3s;
}

.search-input-wrap:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-glow);
}

.search-icon {
  flex-shrink: 0;
  margin-right: 8px;
  color: var(--muted);
}

.search-input {
  flex: 1;
  border: none;
  background: none;
  font-size: 14px;
  outline: none;
  padding: 0;
  color: var(--ink);
}

.search-input::placeholder {
  color: var(--muted-light);
}

.clear-btn {
  border: none;
  background: none;
  padding: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  flex-shrink: 0;
  color: var(--muted);
  transition: color 0.2s;
}

.clear-btn:hover {
  color: var(--ink);
}

.search-btn {
  flex-shrink: 0;
  border: none;
  background: linear-gradient(135deg, var(--accent), var(--accent-2));
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  padding: 0 20px;
  height: 42px;
  border-radius: 14px;
  cursor: pointer;
  box-shadow: var(--shadow-accent);
  transition: all 0.3s;
}

.search-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(232, 114, 42, 0.3);
}

.search-btn:active {
  transform: scale(0.97);
}

/* Mode tabs */
.mode-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  margin-top: 4px;
}

.mode-tab {
  flex: 1;
  border: 1px solid var(--line);
  background: #fff;
  padding: 10px 0;
  font-size: 13px;
  color: var(--muted);
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.3s;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.mode-tab.active {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
  box-shadow: var(--shadow-accent);
  font-weight: 600;
}

.mode-tab:not(.active):hover {
  border-color: var(--accent);
  color: var(--accent);
}

.search-body {
  padding-bottom: 24px;
}

/* Loading */
.loading-box {
  text-align: center;
  padding: 60px 0;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--line);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 14px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-box p {
  color: var(--muted);
  font-size: 14px;
  margin: 0;
}

/* Results */
.result-header {
  font-size: 13px;
  color: var(--muted);
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.result-header .count {
  color: var(--accent);
  font-weight: 700;
  font-family: var(--display);
}

.result-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 4px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  margin-right: 2px;
}

.result-list {
  display: grid;
  gap: 10px;
}

.result-card {
  display: flex;
  gap: 14px;
  background: #fff;
  border-radius: 16px;
  padding: 14px;
  border: 1px solid rgba(236, 230, 223, 0.5);
  box-shadow: var(--shadow-xs);
  animation: riseUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.result-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: rgba(232, 114, 42, 0.15);
}

.card-img {
  flex-shrink: 0;
  width: 92px;
  height: 92px;
  border-radius: 12px;
  overflow: hidden;
}

.card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s;
}

.result-card:hover .card-img img {
  transform: scale(1.06);
}

.card-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.card-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-name {
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.relevance-badge {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 8px;
}

.relevance-badge.high {
  background: #e8f5e9;
  color: #2e7d32;
}

.relevance-badge.mid {
  background: #fff3e0;
  color: #e65100;
}

.relevance-badge.low {
  background: var(--bg-warm);
  color: var(--muted);
}

.card-rating {
  display: flex;
  align-items: center;
  gap: 6px;
}

.stars {
  display: flex;
  gap: 1px;
}

.star {
  font-size: 12px;
  color: #ddd5cb;
}

.star.on {
  color: var(--accent);
}

.score {
  font-size: 13px;
  font-weight: 700;
  color: var(--accent);
}

.comment-count {
  font-size: 12px;
  color: var(--muted);
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.price {
  color: var(--accent-deep);
  font-weight: 600;
}

.price small {
  font-size: 11px;
  font-weight: 400;
  color: var(--muted);
}

.area {
  color: var(--muted);
  font-size: 12px;
  padding: 2px 8px;
  background: var(--bg-warm);
  border-radius: 6px;
}

.card-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* Hint */
.search-hint {
  text-align: center;
  padding: 80px 0;
}

.hint-icon {
  margin-bottom: 16px;
}

.hint-text {
  color: var(--muted-light);
  font-size: 14px;
}

@keyframes riseUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
