import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('hmdp_token')
  if (token) {
    config.headers.authorization = token
  }
  return config
})

api.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && payload.success === false) {
      return Promise.reject(new Error(payload.errorMsg || '请求失败'))
    }
    return payload
  },
  (error) => Promise.reject(error)
)

export default api
