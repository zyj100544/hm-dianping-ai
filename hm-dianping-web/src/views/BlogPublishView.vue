<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { searchShopsByName } from '../services/shop'
import { uploadImage } from '../services/upload'
import { saveBlog, aiAssistBlog } from '../services/blog'
import { resolveImg } from '../utils/url'

const router = useRouter()
const userStore = useUserStore()

const MAX_IMAGES = 9

const title = ref('')
const content = ref('')
const selectedShop = ref(null)
const shopKeyword = ref('')
const shopResults = ref([])
const shopSearching = ref(false)
const uploadedImages = ref([])
const uploading = ref(false)
const submitting = ref(false)
const errorMsg = ref('')

const aiPanelOpen = ref(false)
const aiAction = ref('generate')
const aiKeywords = ref('')
const aiGenerating = ref(false)

const canSubmit = computed(() =>
  selectedShop.value && title.value.trim() && content.value.trim() && !submitting.value
)

let searchTimer = null
const handleShopSearch = async () => {
  clearTimeout(searchTimer)
  const kw = shopKeyword.value.trim()
  if (!kw) {
    shopResults.value = []
    return
  }
  searchTimer = setTimeout(async () => {
    shopSearching.value = true
    try {
      const res = await searchShopsByName(kw)
      shopResults.value = (res.data || []).slice(0, 6)
    } catch {
      shopResults.value = []
    } finally {
      shopSearching.value = false
    }
  }, 300)
}

const selectShop = (shop) => {
  selectedShop.value = shop
  shopKeyword.value = shop.name
  shopResults.value = []
}

const clearShop = () => {
  selectedShop.value = null
  shopKeyword.value = ''
}

const handleImageUpload = async (e) => {
  const files = e.target.files
  if (!files || files.length === 0) return

  const remaining = MAX_IMAGES - uploadedImages.value.length
  if (remaining <= 0) {
    errorMsg.value = `最多上传 ${MAX_IMAGES} 张图片`
    return
  }

  const toUpload = Array.from(files).slice(0, remaining)
  errorMsg.value = ''

  for (const file of toUpload) {
    if (!file.type.startsWith('image/')) {
      errorMsg.value = '仅支持图片文件'
      continue
    }
    if (file.size > 10 * 1024 * 1024) {
      errorMsg.value = '图片大小不能超过 10MB'
      continue
    }
    uploading.value = true
    try {
      const res = await uploadImage(file)
      uploadedImages.value.push(res.data)
    } catch (err) {
      errorMsg.value = err.message || '图片上传失败'
    } finally {
      uploading.value = false
    }
  }
  e.target.value = ''
}

const removeImage = (idx) => {
  uploadedImages.value.splice(idx, 1)
}

const handleSubmit = async () => {
  if (!canSubmit.value) return
  submitting.value = true
  errorMsg.value = ''
  try {
    await saveBlog({
      shopId: selectedShop.value.id,
      title: title.value.trim(),
      content: content.value.trim(),
      images: uploadedImages.value.join(','),
    })
    router.push('/blog')
  } catch (err) {
    errorMsg.value = err.message || '发布失败'
  } finally {
    submitting.value = false
  }
}

const handleAIAssist = async () => {
  if (!selectedShop.value) {
    errorMsg.value = '请先选择探店店铺'
    return
  }
  aiGenerating.value = true
  errorMsg.value = ''
  try {
    const res = await aiAssistBlog({
      action: aiAction.value,
      shopName: selectedShop.value.name,
      draft: aiAction.value !== 'generate' ? content.value : '',
      keywords: aiKeywords.value,
    })
    const generated = res.data
    content.value = generated
    aiPanelOpen.value = false
  } catch (err) {
    errorMsg.value = err.message || 'AI生成失败，请稍后再试'
  } finally {
    aiGenerating.value = false
  }
}

onMounted(() => {
  if (!userStore.token) {
    router.replace('/login')
  }
})
</script>

<template>
  <section class="fade-in publish-page">
    <div class="page-title">发布探店笔记</div>

    <div class="card form-card">
      <!-- 选择店铺 -->
      <div class="field">
        <label class="field-label">探店店铺 <span class="required">*</span></label>
        <div v-if="!selectedShop" class="shop-search">
          <div class="input-wrap">
            <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
            <input
              class="input field-input"
              v-model="shopKeyword"
              placeholder="搜索店铺名称..."
              @input="handleShopSearch"
            />
          </div>
          <div class="shop-results" v-if="shopResults.length">
            <button
              class="shop-item"
              v-for="shop in shopResults"
              :key="shop.id"
              @click="selectShop(shop)"
            >
              <img v-if="shop.images" :src="resolveImg(shop.images.split(',')[0])" class="shop-thumb" alt="" />
              <div>
                <div class="shop-item-name">{{ shop.name }}</div>
                <div class="muted">{{ shop.area }} · {{ shop.address }}</div>
              </div>
            </button>
          </div>
          <p class="muted hint" v-else-if="!shopSearching && shopKeyword.trim()">
            未找到匹配店铺
          </p>
        </div>
        <div v-else class="shop-selected">
          <div class="shop-card">
            <img v-if="selectedShop.images" :src="resolveImg(selectedShop.images.split(',')[0])" class="shop-thumb" alt="" />
            <div>
              <div class="shop-item-name">{{ selectedShop.name }}</div>
              <div class="muted">{{ selectedShop.area }} · {{ selectedShop.address }}</div>
            </div>
            <button class="btn secondary sm" @click="clearShop">更换</button>
          </div>
        </div>
      </div>

      <!-- 标题 -->
      <div class="field">
        <label class="field-label">笔记标题 <span class="required">*</span></label>
        <input
          class="input"
          v-model="title"
          placeholder="比如：这家店的招牌菜太绝了！"
          maxlength="60"
        />
      </div>

      <!-- 内容 -->
      <div class="field">
        <div class="field-label-row">
          <label class="field-label">探店心得 <span class="required">*</span></label>
          <button
            class="ai-btn"
            :class="{ active: aiPanelOpen }"
            @click="aiPanelOpen = !aiPanelOpen"
            type="button"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
            AI帮写
          </button>
        </div>

        <!-- AI Panel -->
        <transition name="slide">
          <div v-if="aiPanelOpen" class="ai-panel">
            <div class="ai-actions">
              <button
                class="ai-action-btn"
                :class="{ active: aiAction === 'generate' }"
                @click="aiAction = 'generate'"
                type="button"
              >帮你写</button>
              <button
                class="ai-action-btn"
                :class="{ active: aiAction === 'polish' }"
                @click="aiAction = 'polish'"
                type="button"
              >润色</button>
              <button
                class="ai-action-btn"
                :class="{ active: aiAction === 'expand' }"
                @click="aiAction = 'expand'"
                type="button"
              >扩写</button>
            </div>
            <input
              v-if="aiAction === 'generate'"
              class="input ai-keywords"
              v-model="aiKeywords"
              placeholder="想突出什么？比如：招牌菜、环境好、性价比高..."
            />
            <p v-if="aiAction !== 'generate' && !content.trim()" class="muted hint">
              请先在下方输入草稿内容，再使用此功能
            </p>
            <button
              class="ai-generate-btn"
              :disabled="aiGenerating || (aiAction !== 'generate' && !content.trim())"
              @click="handleAIAssist"
              type="button"
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
              {{ aiGenerating ? 'AI写作中...' : '开始生成' }}
            </button>
          </div>
        </transition>

        <textarea
          class="input content-input"
          v-model="content"
          placeholder="分享你的探店体验，味道、环境、服务等..."
          rows="6"
          maxlength="2000"
        ></textarea>
        <p class="muted hint">{{ content.length }}/2000</p>
      </div>

      <!-- 图片上传 -->
      <div class="field">
        <label class="field-label">图片 <span class="muted">(最多{{ MAX_IMAGES }}张)</span></label>
        <div class="image-grid">
          <div
            class="image-item"
            v-for="(img, idx) in uploadedImages"
            :key="img"
          >
            <img :src="resolveImg(img)" alt="" class="preview-img" />
            <button class="remove-btn" @click="removeImage(idx)" title="移除">×</button>
          </div>
          <label v-if="uploadedImages.length < MAX_IMAGES" class="upload-trigger">
            <input
              type="file"
              accept="image/*"
              class="file-input"
              @change="handleImageUpload"
              :disabled="uploading"
            />
            <div class="upload-placeholder">
              <svg v-if="!uploading" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              <span v-else class="uploading-text">上传中...</span>
            </div>
          </label>
        </div>
      </div>

      <!-- 错误提示 -->
      <transition name="fade">
        <p v-if="errorMsg" class="error-text">{{ errorMsg }}</p>
      </transition>

      <!-- 提交 -->
      <button
        class="btn submit-btn"
        :disabled="!canSubmit"
        @click="handleSubmit"
      >
        {{ submitting ? '发布中...' : '发布笔记' }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.publish-page {
  max-width: 680px;
  margin: 0 auto;
}

.form-card {
  display: grid;
  gap: 22px;
  padding: 24px;
}

.field {
  display: grid;
  gap: 8px;
}

.field-label {
  font-weight: 600;
  font-size: 14px;
  color: var(--ink-2);
}

.required {
  color: var(--accent);
}

.hint {
  font-size: 12px;
  margin: 2px 0 0;
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

.field-input {
  background: none !important;
  border: none !important;
  padding: 11px 0 !important;
  box-shadow: none !important;
}

.field-input:focus {
  box-shadow: none !important;
  background: none !important;
  border: none !important;
}

.content-input {
  resize: vertical;
  min-height: 130px;
  font-family: var(--font);
  line-height: 1.7;
}

.shop-search {
  position: relative;
}

.shop-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  box-shadow: var(--shadow-md);
  z-index: 10;
  overflow: hidden;
  margin-top: 4px;
}

.shop-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  border: none;
  background: none;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s;
}

.shop-item:hover {
  background: var(--accent-glow);
}

.shop-item + .shop-item {
  border-top: 1px solid var(--line);
}

.shop-thumb {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
}

.shop-item-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--ink);
}

.shop-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}

.sm {
  padding: 7px 12px;
  font-size: 12px;
  margin-left: auto;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.image-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--line);
  animation: fadeIn 0.3s ease;
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  backdrop-filter: blur(4px);
}

.remove-btn:hover {
  background: rgba(0, 0, 0, 0.75);
  transform: scale(1.1);
}

.upload-trigger {
  aspect-ratio: 1;
  border: 2px dashed var(--line);
  border-radius: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.upload-trigger:hover {
  border-color: var(--accent);
  background: var(--accent-glow);
}

.file-input {
  display: none;
}

.upload-placeholder {
  color: var(--muted-light);
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-trigger:hover .upload-placeholder {
  color: var(--accent);
}

.uploading-text {
  font-size: 12px;
  color: var(--muted);
}

.error-text {
  color: var(--accent);
  font-size: 13px;
  text-align: center;
  margin: 0;
  padding: 10px;
  background: var(--accent-glow);
  border-radius: 10px;
}

.submit-btn {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  border-radius: 14px;
}

.submit-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  box-shadow: none;
}

.field-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* AI Button */
.ai-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  font-size: 13px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.25);
}

.ai-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(102, 126, 234, 0.35);
}

.ai-btn.active {
  opacity: 0.85;
  box-shadow: none;
}

/* AI Panel */
.ai-panel {
  background: linear-gradient(135deg, #f8f7ff, #f0eeff);
  border: 1px solid #e0dcf7;
  border-radius: 16px;
  padding: 16px;
  display: grid;
  gap: 12px;
}

.ai-actions {
  display: flex;
  gap: 8px;
}

.ai-action-btn {
  flex: 1;
  padding: 9px 0;
  font-size: 13px;
  border-radius: 10px;
  border: 1px solid #e0dcf7;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
  color: var(--ink-2);
}

.ai-action-btn:hover {
  border-color: #667eea;
  color: #667eea;
}

.ai-action-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.25);
}

.ai-keywords {
  font-size: 13px;
  padding: 9px 14px;
}

.ai-generate-btn {
  width: 100%;
  padding: 11px;
  font-size: 14px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.25);
}

.ai-generate-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  box-shadow: none;
}

.ai-generate-btn:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(102, 126, 234, 0.35);
}

/* Transitions */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@keyframes fadeIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

@media (max-width: 640px) {
  .image-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
