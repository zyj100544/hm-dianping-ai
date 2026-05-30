<template>
  <section class="not-found fade-in">
    <div class="not-found-content">
      <div class="error-code">404</div>
      <h2 class="error-title">页面走丢了</h2>
      <p class="muted">你访问的页面不存在或已被移除</p>
      <RouterLink class="btn" to="/">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
        返回首页
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}

.not-found-content {
  text-align: center;
}

.error-code {
  font-family: var(--display);
  font-size: 80px;
  font-weight: 700;
  color: var(--line);
  line-height: 1;
  margin-bottom: 12px;
  letter-spacing: -0.02em;
}

.error-title {
  font-family: var(--display);
  font-size: 22px;
  font-weight: 600;
  color: var(--ink);
  margin: 0 0 8px;
}

.muted {
  margin-bottom: 24px;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style>
