<script setup>
import { formatDateTime } from '../utils/format'

defineProps({
  comments: {
    type: Array,
    default: () => [],
  },
})
</script>

<template>
  <div class="comment-list">
    <div class="comment" v-for="comment in comments" :key="comment.id">
      <div class="comment-header">
        <div class="comment-user">
          <div class="user-avatar">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          </div>
          <strong class="user-name">{{ comment.userName || '匿名用户' }}</strong>
          <span class="comment-time">{{ formatDateTime(comment.createTime) }}</span>
        </div>
        <span class="score-pill" v-if="comment.score">
          <svg width="11" height="11" viewBox="0 0 24 24" fill="currentColor" stroke="none"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
          {{ comment.score }}
        </span>
      </div>
      <p class="comment-body">{{ comment.content }}</p>
    </div>
  </div>
</template>

<style scoped>
.comment-list {
  display: grid;
  gap: 12px;
}

.comment {
  padding: 14px 16px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--line);
  transition: all 0.2s;
}

.comment:hover {
  box-shadow: var(--shadow-xs);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--bg-warm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--muted);
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-2);
}

.comment-time {
  font-size: 11px;
  color: var(--muted-light);
}

.score-pill {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px;
  border-radius: 8px;
  background: var(--accent-glow);
  color: var(--accent);
  font-size: 12px;
  font-weight: 600;
}

.comment-body {
  margin: 0;
  color: var(--ink-2);
  font-size: 14px;
  line-height: 1.6;
}
</style>
