<script setup>
import { ref, onMounted } from 'vue'
import { fetchHotBlogs } from '../services/blog'
import BlogCard from '../components/BlogCard.vue'
import EmptyState from '../components/EmptyState.vue'

const blogs = ref([])
const loading = ref(false)

const loadBlogs = async () => {
  loading.value = true
  try {
    const res = await fetchHotBlogs(1)
    blogs.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(loadBlogs)
</script>

<template>
  <section class="fade-in">
    <div class="blog-header">
      <h1 class="page-title">探店灵感</h1>
      <p class="muted">发现城市好店，看看大家怎么说</p>
    </div>
    <div class="section-title">
      <h3>本周热度</h3>
      <span class="muted" v-if="loading">加载中...</span>
      <span class="muted" v-else>{{ blogs.length }} 篇笔记</span>
    </div>
    <div class="list stagger">
      <BlogCard v-for="blog in blogs" :key="blog.id" :blog="blog" />
      <EmptyState v-if="!loading && blogs.length === 0" title="还没有探店内容" />
    </div>
  </section>
</template>

<style scoped>
.blog-header {
  margin-bottom: 4px;
}

.blog-header .page-title {
  margin-bottom: 6px;
}

.blog-header p {
  margin: 0;
}
</style>
