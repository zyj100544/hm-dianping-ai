<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { fetchBlogById, likeBlog, fetchBlogLikes } from '../services/blog'
import { resolveImg, splitImages } from '../utils/url'

const route = useRoute()
const blog = ref(null)
const likedUsers = ref([])
const loading = ref(true)
const likeAnimating = ref(false)

const images = computed(() => splitImages(blog.value?.images).map(resolveImg))

const loadDetail = async () => {
  loading.value = true
  try {
    const [blogRes, likesRes] = await Promise.all([
      fetchBlogById(route.params.id),
      fetchBlogLikes(route.params.id),
    ])
    blog.value = blogRes.data
    likedUsers.value = likesRes.data || []
  } finally {
    loading.value = false
  }
}

const handleLike = async () => {
  if (!localStorage.getItem('hmdp_token')) {
    alert('请先登录再点赞')
    return
  }
  try {
    likeAnimating.value = true
    await likeBlog(route.params.id)
    if (blog.value) {
      blog.value.isLike = !blog.value.isLike
      blog.value.liked = (blog.value.liked || 0) + (blog.value.isLike ? 1 : -1)
    }
    const likesRes = await fetchBlogLikes(route.params.id)
    likedUsers.value = likesRes.data || []
  } catch (error) {
    alert(error.message || '操作失败')
  } finally {
    setTimeout(() => { likeAnimating.value = false }, 300)
  }
}

onMounted(loadDetail)
</script>

<template>
  <section v-if="blog" class="fade-in">
    <div class="blog-detail-header">
      <h1 class="page-title">{{ blog.title }}</h1>
    </div>

    <div class="card blog-card">
      <!-- Image gallery -->
      <div class="blog-images" v-if="images.length">
        <div class="image-item" v-for="(img, idx) in images" :key="img" :style="{ animationDelay: `${idx * 0.06}s` }">
          <img :src="img" alt="" loading="lazy" />
        </div>
      </div>

      <!-- Content -->
      <div class="blog-content" v-html="blog.content"></div>

      <!-- Actions -->
      <div class="blog-actions">
        <button
          class="like-btn"
          :class="{ liked: blog.isLike, animating: likeAnimating }"
          @click="handleLike"
        >
          <span class="like-icon">
            <svg v-if="!blog.isLike" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
          </span>
          <span class="like-count">{{ blog.liked || 0 }}</span>
        </button>
        <span class="comment-pill">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          {{ blog.comments || 0 }} 条评论
        </span>
      </div>

      <!-- Liked users -->
      <div class="liked-users" v-if="likedUsers.length">
        <div class="liked-label">最近点赞</div>
        <div class="avatars">
          <img
            v-for="user in likedUsers"
            :key="user.id"
            :src="resolveImg(user.icon)"
            :alt="user.nickName"
            :title="user.nickName"
            class="avatar"
          />
        </div>
      </div>
    </div>
  </section>
  <section v-else class="card loading-card">
    <div v-if="loading" class="loading-box">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>
    <div v-else class="muted">未找到内容</div>
  </section>
</template>

<style scoped>
.blog-detail-header {
  margin-bottom: 4px;
}

.blog-card {
  padding: 22px;
}

/* Image gallery */
.blog-images {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 10px;
  margin-bottom: 18px;
}

.image-item {
  border-radius: 14px;
  overflow: hidden;
  animation: fadeIn 0.4s ease forwards;
  opacity: 0;
}

.image-item img {
  height: 200px;
  width: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.image-item:hover img {
  transform: scale(1.04);
}

/* Content */
.blog-content {
  color: var(--ink-2);
  line-height: 1.8;
  font-size: 15px;
}

/* Actions */
.blog-actions {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}

.like-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
  color: var(--muted);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.like-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.like-btn.liked {
  background: linear-gradient(135deg, #ec4899, #f43f5e);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 4px 12px rgba(236, 72, 153, 0.25);
}

.like-btn.animating .like-icon {
  animation: likePopIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes likePopIn {
  0% { transform: scale(1); }
  50% { transform: scale(1.35); }
  100% { transform: scale(1); }
}

.like-icon {
  display: flex;
  align-items: center;
}

.comment-pill {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 14px;
  border: 1px solid var(--line);
  border-radius: 12px;
  font-size: 13px;
  color: var(--muted);
}

/* Liked users */
.liked-users {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.liked-label {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 10px;
}

.avatars {
  display: flex;
  gap: 0;
  flex-wrap: wrap;
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #fff;
  margin-left: -6px;
  transition: transform 0.2s;
}

.avatar:first-child {
  margin-left: 0;
}

.avatar:hover {
  transform: scale(1.15);
  z-index: 1;
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

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
