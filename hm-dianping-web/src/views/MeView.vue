<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { fetchMe, fetchUserInfo } from '../services/user'
import { fetchMyBlogs } from '../services/blog'
import { resolveImg } from '../utils/url'
import BlogCard from '../components/BlogCard.vue'
import EmptyState from '../components/EmptyState.vue'

const router = useRouter()
const userStore = useUserStore()
const profile = ref(null)
const info = ref(null)
const myBlogs = ref([])
const blogsLoading = ref(false)

const loadProfile = async () => {
  if (!userStore.token) return
  const res = await fetchMe()
  profile.value = res.data
  if (profile.value?.id) {
    const infoRes = await fetchUserInfo(profile.value.id)
    info.value = infoRes.data
  }
}

const loadMyBlogs = async () => {
  if (!userStore.token) return
  blogsLoading.value = true
  try {
    const res = await fetchMyBlogs(1)
    myBlogs.value = res.data || []
  } finally {
    blogsLoading.value = false
  }
}

const handleLogin = () => router.push('/login')
const handleLogout = () => userStore.logout()

onMounted(() => {
  loadProfile()
  loadMyBlogs()
})
</script>

<template>
  <section class="fade-in">
    <div class="page-title">我的</div>

    <!-- Not logged in -->
    <div v-if="!userStore.token" class="login-prompt card">
      <div class="prompt-icon">
        <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="var(--line-strong)" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
      </div>
      <p>登录后可查看积分、关注和评论记录</p>
      <button class="btn" @click="handleLogin">去登录</button>
    </div>

    <!-- Profile card -->
    <div v-else class="profile-card card">
      <div class="profile-bg"></div>
      <div class="profile-content">
        <img class="avatar" :src="resolveImg(profile?.icon)" alt="" />
        <div class="profile-info">
          <h3 class="profile-name">{{ profile?.nickName || '用户' }}</h3>
          <div class="profile-meta">
            <span class="meta-item" v-if="profile?.phone">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
              {{ profile.phone }}
            </span>
            <span class="meta-item" v-if="info?.city">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
              {{ info.city }}
            </span>
          </div>
          <p class="profile-intro muted" v-if="info?.introduce">{{ info.introduce }}</p>
        </div>
      </div>
      <div class="profile-actions">
        <button class="btn" @click="router.push('/blog/publish')">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          发布探店笔记
        </button>
        <button class="btn secondary" @click="handleLogout">退出登录</button>
      </div>
    </div>

    <!-- My blogs -->
    <template v-if="userStore.token">
      <div class="section-title">
        <h3>我的探店笔记</h3>
        <span class="muted">{{ myBlogs.length }} 篇</span>
      </div>
      <div class="list stagger">
        <BlogCard v-for="blog in myBlogs" :key="blog.id" :blog="blog" />
      </div>
      <EmptyState
        v-if="!blogsLoading && myBlogs.length === 0"
        title="还没有发布过笔记"
        subtitle="去分享你的探店体验吧"
      />
    </template>
  </section>
</template>

<style scoped>
/* Login prompt */
.login-prompt {
  text-align: center;
  padding: 40px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.prompt-icon {
  margin-bottom: 4px;
}

.login-prompt p {
  margin: 0;
  color: var(--muted);
  font-size: 14px;
}

/* Profile card */
.profile-card {
  overflow: hidden;
  padding: 0;
  position: relative;
}

.profile-bg {
  height: 80px;
  background: linear-gradient(135deg, var(--accent), var(--accent-2), #e8722a);
  position: relative;
}

.profile-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, transparent 40%, rgba(255, 255, 255, 0.1) 50%, transparent 60%);
  animation: shimmer 4s ease-in-out infinite;
}

@keyframes shimmer {
  0%, 100% { transform: translateX(-100%) rotate(45deg); }
  50% { transform: translateX(100%) rotate(45deg); }
}

.profile-content {
  padding: 0 22px 22px;
  display: flex;
  gap: 16px;
  margin-top: -30px;
  position: relative;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid #fff;
  box-shadow: var(--shadow);
  flex-shrink: 0;
}

.profile-info {
  padding-top: 34px;
  flex: 1;
  min-width: 0;
}

.profile-name {
  margin: 0 0 6px;
  font-family: var(--display);
  font-size: 20px;
  font-weight: 700;
  color: var(--ink);
}

.profile-meta {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--muted);
}

.profile-intro {
  margin: 8px 0 0;
  font-size: 13px;
}

.profile-actions {
  display: flex;
  gap: 10px;
  padding: 0 22px 22px;
}

.profile-actions .btn {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
