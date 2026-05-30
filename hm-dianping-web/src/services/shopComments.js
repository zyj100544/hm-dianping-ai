import api from './api'

export const fetchShopComments = (shopId, current = 1) => {
  return api.get(`/shop-comments/of/shop/${shopId}?current=${current}`)
}

export const addShopComment = (payload) => api.post('/shop-comments', payload)
