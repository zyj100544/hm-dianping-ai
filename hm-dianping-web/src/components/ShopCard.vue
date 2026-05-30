<script setup>
import { computed } from 'vue'
import { resolveImg, splitImages } from '../utils/url'

const props = defineProps({
  shop: {
    type: Object,
    required: true,
  },
})

const cover = computed(() => {
  const images = splitImages(props.shop.images)
  return images.length ? resolveImg(images[0]) : ''
})

const scoreText = computed(() => ((props.shop.score || 0) / 10).toFixed(1))
const starCount = computed(() => Math.round((props.shop.score || 0) / 10))
</script>

<template>
  <RouterLink class="shop-card" :to="`/shop/${shop.id}`">
    <div class="shop-img" v-if="cover">
      <img :src="cover" :alt="shop.name" loading="lazy" />
      <div class="shop-img-overlay"></div>
    </div>
    <div class="shop-info">
      <div class="shop-name">{{ shop.name }}</div>
      <div class="shop-rating">
        <span class="stars">
          <span v-for="i in 5" :key="i" class="star" :class="{ on: i <= starCount }">★</span>
        </span>
        <span class="score">{{ scoreText }}</span>
        <span class="reviews">{{ shop.comments || 0 }}条评价</span>
      </div>
      <div class="shop-meta">
        <span class="price">¥{{ shop.avgPrice || '--' }}<small>/人</small></span>
        <span class="area" v-if="shop.area">{{ shop.area }}</span>
      </div>
    </div>
    <div class="shop-arrow">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
    </div>
  </RouterLink>
</template>

<style scoped>
.shop-card {
  display: flex;
  gap: 14px;
  background: #fff;
  border-radius: 16px;
  padding: 14px;
  border: 1px solid rgba(236, 230, 223, 0.5);
  box-shadow: var(--shadow-xs);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  align-items: center;
  position: relative;
}

.shop-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: rgba(232, 114, 42, 0.15);
}

.shop-card:active {
  transform: scale(0.985);
}

.shop-img {
  flex-shrink: 0;
  width: 96px;
  height: 96px;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
}

.shop-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.shop-card:hover .shop-img img {
  transform: scale(1.06);
}

.shop-img-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 60%, rgba(0, 0, 0, 0.06) 100%);
  pointer-events: none;
}

.shop-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.shop-name {
  font-size: 16px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ink);
}

.shop-rating {
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
  line-height: 1;
}

.star.on {
  color: var(--accent);
}

.score {
  font-size: 14px;
  font-weight: 700;
  color: var(--accent);
  font-family: var(--display);
}

.reviews {
  font-size: 12px;
  color: var(--muted);
}

.shop-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
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

.shop-arrow {
  flex-shrink: 0;
  color: var(--muted-light);
  transition: all 0.3s;
}

.shop-card:hover .shop-arrow {
  color: var(--accent);
  transform: translateX(2px);
}
</style>
