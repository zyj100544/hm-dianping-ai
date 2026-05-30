const staticBase = (import.meta.env.VITE_STATIC_BASE_URL || '').replace(/\/$/, '')

export const resolveImg = (src) => {
  if (!src) return ''
  if (src.startsWith('http://') || src.startsWith('https://')) {
    return src
  }
  if (src.startsWith('/')) {
    return staticBase ? `${staticBase}${src}` : src
  }
  return staticBase ? `${staticBase}/${src}` : src
}

export const splitImages = (images) => {
  if (!images) return []
  return images.split(',').map((item) => item.trim()).filter(Boolean)
}
