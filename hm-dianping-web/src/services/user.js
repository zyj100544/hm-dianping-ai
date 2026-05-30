import api from './api'

export const sendCode = (phone) => api.post(`/user/code?phone=${encodeURIComponent(phone)}`)

export const login = (payload) => api.post('/user/login', payload)

export const fetchMe = () => api.get('/user/me')

export const fetchUser = (id) => api.get(`/user/${id}`)

export const logout = () => api.post('/user/logout')

export const fetchUserInfo = (id) => api.get(`/user/info/${id}`)
