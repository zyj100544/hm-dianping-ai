<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { sendCode, login } from '../services/user'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()

const form = ref({
  phone: '',
  code: '',
})

const status = ref('')
const codeSent = ref(false)

const handleSendCode = async () => {
  if (!form.value.phone.trim()) {
    status.value = '请输入手机号'
    return
  }
  try {
    await sendCode(form.value.phone.trim())
    status.value = '验证码已发送，请查看后端日志'
    codeSent.value = true
  } catch (error) {
    status.value = error.message || '发送失败'
  }
}

const handleLogin = async () => {
  if (!form.value.phone.trim() || !form.value.code.trim()) {
    status.value = '手机号和验证码必填'
    return
  }
  try {
    const res = await login({
      phone: form.value.phone.trim(),
      code: form.value.code.trim(),
    })
    userStore.setToken(res.data)
    status.value = '登录成功'
    router.push('/')
  } catch (error) {
    status.value = error.message || '登录失败'
  }
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}
</script>

<template>
  <div class="login">
    <!-- Background decoration -->
    <div class="login-bg">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
    </div>

    <div class="login-card fade-in">
      <!-- Back button -->
      <button class="back-btn" @click="goBack">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
      </button>

      <!-- Brand -->
      <div class="login-brand">
        <div class="brand-dot-lg"></div>
        <h1 class="login-title">黑马点评</h1>
        <p class="login-sub">城市生活新提案</p>
      </div>

      <!-- Form -->
      <div class="login-form">
        <div class="input-group">
          <label class="input-label">手机号</label>
          <div class="input-wrap">
            <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
            <input class="input" placeholder="请输入手机号" v-model="form.phone" type="tel" maxlength="11" />
          </div>
        </div>

        <div class="input-group">
          <label class="input-label">验证码</label>
          <div class="code-row">
            <div class="input-wrap code-input-wrap">
              <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <input class="input" placeholder="请输入验证码" v-model="form.code" />
            </div>
            <button class="btn-code" :class="{ sent: codeSent }" @click="handleSendCode">
              {{ codeSent ? '重新发送' : '发送验证码' }}
            </button>
          </div>
        </div>

        <button class="btn login-btn" @click="handleLogin">
          登录
        </button>

        <transition name="fade">
          <div class="status-msg" v-if="status">
            <span>{{ status }}</span>
          </div>
        </transition>

        <p class="login-hint muted">
          登录后可解锁点赞、关注、抢券与评论
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
  background: var(--bg-warm);
}

/* Decorative background circles */
.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
}

.bg-circle-1 {
  width: 500px;
  height: 500px;
  background: var(--accent);
  top: -200px;
  right: -150px;
  animation: floatSlow 12s ease-in-out infinite;
}

.bg-circle-2 {
  width: 300px;
  height: 300px;
  background: #667eea;
  bottom: -100px;
  left: -80px;
  animation: floatSlow 10s ease-in-out infinite reverse;
}

.bg-circle-3 {
  width: 200px;
  height: 200px;
  background: var(--accent-2);
  top: 40%;
  left: 60%;
  animation: floatSlow 8s ease-in-out infinite 2s;
}

@keyframes floatSlow {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(20px, -30px) scale(1.05); }
}

/* Card */
.login-card {
  max-width: 420px;
  width: 100%;
  background: #fff;
  border-radius: 24px;
  padding: 32px 28px;
  box-shadow: var(--shadow-lg);
  border: 1px solid rgba(236, 230, 223, 0.6);
  position: relative;
  z-index: 1;
}

.back-btn {
  position: absolute;
  top: 20px;
  left: 20px;
  border: none;
  background: var(--bg-warm);
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--ink-2);
  transition: all 0.2s;
}

.back-btn:hover {
  background: var(--line);
  transform: translateX(-2px);
}

/* Brand */
.login-brand {
  text-align: center;
  margin-bottom: 32px;
  padding-top: 8px;
}

.brand-dot-lg {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  background: linear-gradient(135deg, var(--accent), var(--accent-2));
  margin: 0 auto 16px;
  box-shadow: 0 8px 24px rgba(232, 114, 42, 0.25);
  position: relative;
  overflow: hidden;
}

.brand-dot-lg::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(135deg, transparent 40%, rgba(255, 255, 255, 0.2) 50%, transparent 60%);
  animation: shimmer 3s ease-in-out infinite;
}

@keyframes shimmer {
  0%, 100% { transform: translateX(-100%) rotate(45deg); }
  50% { transform: translateX(100%) rotate(45deg); }
}

.login-title {
  font-family: var(--display);
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 4px;
  color: var(--ink);
}

.login-sub {
  margin: 0;
  font-size: 13px;
  color: var(--muted);
  letter-spacing: 0.06em;
}

/* Form */
.login-form {
  display: grid;
  gap: 18px;
}

.input-group {
  display: grid;
  gap: 6px;
}

.input-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-2);
  padding-left: 4px;
}

.input-wrap {
  display: flex;
  align-items: center;
  background: var(--bg);
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 0 14px;
  transition: all 0.3s;
}

.input-wrap:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-glow);
  background: #fff;
}

.input-icon {
  flex-shrink: 0;
  color: var(--muted-light);
  margin-right: 10px;
}

.input-wrap .input {
  background: none;
  border: none;
  padding: 12px 0;
  border-radius: 0;
  box-shadow: none;
}

.input-wrap .input:focus {
  box-shadow: none;
  background: none;
}

.code-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
}

.code-input-wrap {
  flex: 1;
}

.btn-code {
  flex-shrink: 0;
  border: none;
  background: var(--bg-warm);
  color: var(--accent);
  font-size: 13px;
  font-weight: 600;
  padding: 11px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-code:hover {
  background: var(--accent-glow);
}

.btn-code.sent {
  color: var(--muted);
}

.login-btn {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  border-radius: 14px;
  margin-top: 4px;
}

/* Status */
.status-msg {
  text-align: center;
  font-size: 13px;
  color: var(--accent);
  padding: 8px 12px;
  background: var(--accent-glow);
  border-radius: 10px;
}

.login-hint {
  text-align: center;
  font-size: 12px;
  margin: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@media (max-width: 640px) {
  .login-card {
    border-radius: 20px;
    padding: 28px 22px;
  }
}
</style>
