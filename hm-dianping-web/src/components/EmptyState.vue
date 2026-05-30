<template>
  <div class="empty card">
    <div class="empty-icon">
      <svg width="52" height="52" viewBox="0 0 24 24" fill="none" stroke="var(--line-strong)" stroke-width="1" stroke-linecap="round" stroke-linejoin="round">
        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
        <circle cx="8.5" cy="8.5" r="1.5"/>
        <polyline points="21 15 16 10 5 21"/>
      </svg>
    </div>
    <div class="empty-title">{{ title }}</div>
    <div class="muted empty-sub">{{ subtitle }}</div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '暂无数据',
  },
  subtitle: {
    type: String,
    default: '换个条件试试吧',
  },
})
</script>

<style scoped>
.empty {
  text-align: center;
  padding: 36px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.empty-icon {
  margin-bottom: 4px;
  opacity: 0.6;
}

.empty-title {
  font-family: var(--display);
  font-size: 17px;
  font-weight: 600;
  color: var(--ink-2);
  margin: 0;
}

.empty-sub {
  font-size: 13px;
  margin: 0;
}
</style>
