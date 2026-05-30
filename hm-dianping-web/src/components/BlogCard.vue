<script setup>
import { computed } from 'vue'
import { resolveImg, splitImages } from '../utils/url'

const props = defineProps({
  blog: {
    type: Object,
    required: true,
  },
})

const cover = computed(() => {
  const images = splitImages(props.blog.images)
  return images.length ? resolveImg(images[0]) : ''
})
</script>

<template>
  <RouterLink class="blog-card" :to="`/blog/${blog.id}`">
    <div class="blog-media" v-if="cover">
      <img :src="cover" :alt="blog.title" loading="lazy" />
      <div class="blog-media-overlay"></div>
    </div>
    <div class="blog-body">
      <h3 class="blog-title">{{ blog.title }}</h3>
      <p class="blog-excerpt muted" v-html="blog.content"></p>
      <div class="blog-meta">
        <div class="meta-item">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
          <span>{{ blog.liked || 0 }}</span>
        </div>
        <div class="meta-item">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          <span>{{ blog.comments || 0 }}</span>
        </div>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.blog-card {
  display: grid;
  gap: 0;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(236, 230, 223, 0.5);
  box-shadow: var(--shadow-xs);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.blog-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}

.blog-media {
  position: relative;
  overflow: hidden;
}

.blog-media img {
  height: 200px;
  width: 100%;
  object-fit: cover;
  transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.blog-card:hover .blog-media img {
  transform: scale(1.05);
}

.blog-media-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 50%, rgba(0, 0, 0, 0.04) 100%);
  pointer-events: none;
}

.blog-body {
  padding: 16px 18px;
}

.blog-title {
  margin: 0 0 8px;
  font-family: var(--display);
  font-size: 17px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1.4;
}

.blog-excerpt {
  margin: 0;
  max-height: 3.2em;
  overflow: hidden;
  line-height: 1.6;
  font-size: 13px;
}

.blog-meta {
  display: flex;
  gap: 16px;
  margin-top: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--muted);
}

.meta-item svg {
  opacity: 0.6;
}
</style>
