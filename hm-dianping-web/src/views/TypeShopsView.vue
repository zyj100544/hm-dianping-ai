<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchShopTypes, fetchShopsByType } from '../services/shop'
import ShopCard from '../components/ShopCard.vue'
import EmptyState from '../components/EmptyState.vue'

const route = useRoute()
const types = ref([])
const shops = ref([])
const loading = ref(false)

const typeId = computed(() => Number(route.params.id))
const currentType = computed(() => types.value.find((item) => item.id === typeId.value))

const loadTypes = async () => {
  const res = await fetchShopTypes()
  types.value = res.data || []
}

const loadShops = async () => {
  loading.value = true
  try {
    const res = await fetchShopsByType(typeId.value, 1)
    shops.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadTypes()
  await loadShops()
})

watch(typeId, loadShops)
</script>

<template>
  <section class="fade-in">
    <div class="type-header">
      <h1 class="page-title">{{ currentType?.name || '分类店铺' }}</h1>
      <RouterLink class="back-pill" to="/">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
        返回首页
      </RouterLink>
    </div>

    <div class="section-title">
      <h3>本类精选</h3>
      <span class="muted" v-if="loading">加载中...</span>
      <span class="muted" v-else>{{ shops.length }} 家店铺</span>
    </div>
    <div class="list stagger">
      <ShopCard v-for="shop in shops" :key="shop.id" :shop="shop" />
      <EmptyState v-if="!loading && shops.length === 0" />
    </div>
  </section>
</template>

<style scoped>
.type-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.back-pill {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 7px 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--line);
  font-size: 13px;
  color: var(--muted);
  font-weight: 500;
  transition: all 0.2s;
  box-shadow: var(--shadow-xs);
}

.back-pill:hover {
  border-color: var(--accent);
  color: var(--accent);
  transform: translateX(-2px);
}
</style>
