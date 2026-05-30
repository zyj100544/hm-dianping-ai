import api from './api'

export const fetchShopTypes = () => api.get('/shop-type/list')

export const fetchShopsByType = (typeId, current = 1, x, y) => {
  const params = new URLSearchParams({ typeId, current })
  if (x !== undefined && y !== undefined) {
    params.set('x', x)
    params.set('y', y)
  }
  return api.get(`/shop/of/type?${params.toString()}`)
}

export const fetchShopById = (id) => api.get(`/shop/${id}`)

export const searchShopsByName = (name, current = 1) => {
  const params = new URLSearchParams({ name, current })
  return api.get(`/shop/of/name?${params.toString()}`)
}

export const searchShopsByAI = (keyword, topK = 10) => {
  const params = new URLSearchParams({ keyword, topK })
  return api.get(`/shop/search/ai?${params.toString()}`)
}
