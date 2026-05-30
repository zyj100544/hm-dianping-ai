import api from './api'

export const fetchRecommend = () => api.get('/shop/recommend')
