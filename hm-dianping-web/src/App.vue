<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from './store/user'

const route = useRoute()
const userStore = useUserStore()
const isLogin = computed(() => route.path.startsWith('/login'))
const isSearch = computed(() => route.path.startsWith('/search'))
const isFullPage = computed(() => isLogin.value || isSearch.value)
const isAuthed = computed(() => Boolean(userStore.token))
</script>

<template>
  <div class="app">
    <header class="topbar" v-if="!isFullPage">
      <div class="brand">
        <span class="brand-dot" aria-hidden="true"></span>
        <div>
          <div class="brand-title">黑马点评</div>
          <div class="brand-sub">城市生活新提案</div>
        </div>
      </div>
      <RouterLink v-if="!isAuthed" class="ghost-btn" to="/login">登录</RouterLink>
      <RouterLink v-else class="ghost-btn" to="/me">我的</RouterLink>
    </header>

    <main class="app-body">
      <RouterView />
    </main>

    <nav class="bottom-nav" v-if="!isFullPage">
      <RouterLink to="/" class="nav-item">
        <span>首页</span>
      </RouterLink>
      <RouterLink to="/recommend" class="nav-item">
        <span>推荐</span>
      </RouterLink>
      <RouterLink to="/blog" class="nav-item">
        <span>探店</span>
      </RouterLink>
      <RouterLink to="/me" class="nav-item">
        <span>我的</span>
      </RouterLink>
    </nav>

    <RouterLink v-if="isAuthed && !isFullPage" to="/blog/publish" class="fab" title="发布笔记">
      <span class="fab-icon">+</span>
    </RouterLink>
  </div>
</template>

<style scoped>
.fab {
  position: fixed;
  bottom: 84px;
  right: 24px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), var(--accent-deep));
  box-shadow: var(--shadow-accent), 0 8px 24px rgba(232, 114, 42, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 30;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: pulseGlow 2.5s ease-in-out infinite;
  text-decoration: none;
  overflow: visible;
}

.fab:hover {
  transform: translateY(-3px) scale(1.08);
  box-shadow: 0 12px 32px rgba(232, 114, 42, 0.35);
  animation: none;
}

.fab:active {
  transform: scale(0.95);
}

.fab-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 28px;
  color: #fff;
  line-height: 1;
}
</style>
