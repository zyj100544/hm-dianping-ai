import api from './api'

export const fetchVouchersByShop = (shopId) => api.get(`/voucher/list/${shopId}`)

export const seckillVoucher = (voucherId) => api.post(`/voucher-order/seckill/${voucherId}`)
