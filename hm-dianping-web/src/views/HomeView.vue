<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchShopTypes, fetchShopsByType } from '../services/shop'
import ShopCard from '../components/ShopCard.vue'
import EmptyState from '../components/EmptyState.vue'
import { resolveImg } from '../utils/url'

const types = ref([])
const activeType = ref(null)
const shops = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const router = useRouter()

// Soft pastel backgrounds for type icons
const typeColors = [
  { bg: '#fff3e8', border: '#ffe0c4' },
  { bg: '#e8f5e9', border: '#c8e6c9' },
  { bg: '#e3f2fd', border: '#bbdefb' },
  { bg: '#fce4ec', border: '#f8bbd0' },
  { bg: '#f3e5f5', border: '#e1bee7' },
  { bg: '#e0f7fa', border: '#b2ebf2' },
  { bg: '#fff8e1', border: '#ffecb3' },
  { bg: '#fbe9e7', border: '#ffccbc' },
  { bg: '#e8eaf6', border: '#c5cae9' },
  { bg: '#f1f8e9', border: '#dcedc8' },
]

const getTypeColor = (index) => typeColors[index % typeColors.length]

const loadTypes = async () => {
  const res = await fetchShopTypes()
  types.value = res.data || []
  if (types.value.length) {
    activeType.value = types.value[0]
    await loadShopsByType(activeType.value.id)
  }
}

const loadShopsByType = async (typeId) => {
  loading.value = true
  try {
    const res = await fetchShopsByType(typeId, 1)
    shops.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleTypeClick = async (type) => {
  await router.push(`/type/${type.id}`)
}

const handleTypeIconError = (event) => {
  event.target.src = '/icons/type-placeholder.svg'
}

const goSearch = () => {
  const q = searchKeyword.value.trim()
  if (q) {
    router.push(`/search?q=${encodeURIComponent(q)}`)
  } else {
    router.push('/search')
  }
}

onMounted(loadTypes)
</script>

<template>
  <section class="fade-in">
    <!-- Hero -->
    <div class="hero">
      <div class="hero-content">
        <h1 class="hero-title">今天想去哪儿？</h1>
        <p class="hero-sub">探索城市好店，发现生活灵感</p>
      </div>
    </div>

    <!-- Search Bar -->
    <div class="search-bar" @click="goSearch">
      <div class="search-bar-inner">
        <svg class="search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <span class="search-placeholder">搜索店铺、探店笔记...</span>
        <div class="search-badge">AI</div>
      </div>
    </div>

    <!-- Type Grid -->
    <div class="section-title">
      <h3>热门分类</h3>
    </div>
    <div class="type-grid stagger">
      <button
        v-for="(type, index) in types"
        :key="type.id"
        class="type-card"
        @click="handleTypeClick(type)"
      >
        <div
          class="type-icon-wrap"
          :style="{ background: getTypeColor(index).bg, borderColor: getTypeColor(index).border }"
        >
          <img :src="resolveImg(type.icon)" alt="" @error="handleTypeIconError" />
        </div>
        <span class="type-name">{{ type.name }}</span>
      </button>
    </div>

    <!-- Featured Shops -->
    <div class="section-title">
      <h3>{{ activeType ? activeType.name + '精选' : '精选店铺' }}</h3>
      <span class="muted" v-if="loading">加载中...</span>
      <RouterLink v-else :to="activeType ? `/type/${activeType.id}` : '/'" class="more-link">
        更多
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
      </RouterLink>
    </div>
    <div class="list stagger">
      <ShopCard v-for="shop in shops" :key="shop.id" :shop="shop" />
      <EmptyState v-if="!loading && shops.length === 0" />
    </div>
  </section>
</template>

<style scoped>
/* Hero Section */
.hero {
  margin-bottom: 20px;
  padding: 8px 0 4px;
}

.hero-title {
  font-family: var(--display);
  font-size: 30px;
  font-weight: 700;
  color: var(--ink);
  margin: 0 0 6px;
  letter-spacing: 0.01em;
  line-height: 1.2;
}

.hero-sub {
  margin: 0;
  color: var(--muted);
  font-size: 14px;
  letter-spacing: 0.02em;
}

/* Search Bar — enhanced */
.search-bar {
  margin-bottom: 28px;
  cursor: pointer;
}

.search-bar-inner {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 16px;
  padding: 13px 18px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: var(--shadow-xs);
}

.search-bar-inner:hover {
  border-color: var(--accent);
  box-shadow: var(--shadow-md), 0 0 0 3px var(--accent-glow);
  transform: translateY(-1px);
}

.search-icon {
  flex-shrink: 0;
  color: var(--muted);
  transition: color 0.3s;
}

.search-bar-inner:hover .search-icon {
  color: var(--accent);
}

.search-placeholder {
  flex: 1;
  color: var(--muted-light);
  font-size: 14px;
}

.search-badge {
  flex-shrink: 0;
  padding: 3px 8px;
  border-radius: 6px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.05em;
}

/* Type Grid — with colored backgrounds */
.type-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.type-card {
  border: none;
  background: none;
  padding: 14px 4px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.type-card:hover {
  transform: translateY(-3px);
}

.type-card:active {
  transform: scale(0.94);
}

.type-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  transition: all 0.3s;
  box-shadow: var(--shadow-xs);
}

.type-card:hover .type-icon-wrap {
  box-shadow: var(--shadow);
  transform: scale(1.05);
}

.type-icon-wrap img {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.type-name {
  font-size: 12px;
  color: var(--ink-2);
  font-weight: 500;
  text-align: center;
}

/* More Link */
.more-link {
  font-size: 13px;
  color: var(--accent);
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 2px;
  transition: gap 0.2s;
}

.more-link:hover {
  gap: 5px;
}

@media (max-width: 640px) {
  .hero-title {
    font-size: 24px;
  }
  .type-grid {
    grid-template-columns: repeat(5, 1fr);
    gap: 4px;
  }
  .type-icon-wrap {
    width: 46px;
    height: 46px;
    border-radius: 14px;
  }
  .type-icon-wrap img {
    width: 24px;
    height: 24px;
  }
}
</style>
