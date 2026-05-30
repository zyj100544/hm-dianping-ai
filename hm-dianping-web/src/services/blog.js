import api from './api'

export const fetchHotBlogs = (current = 1) => api.get(`/blog/hot?current=${current}`)

export const fetchBlogById = (id) => api.get(`/blog/${id}`)

export const likeBlog = (id) => api.put(`/blog/like/${id}`)

export const fetchBlogLikes = (id) => api.get(`/blog/likes/${id}`)

export const saveBlog = (blogData) => api.post('/blog', blogData)

export const fetchMyBlogs = (current = 1) => api.get(`/blog/of/me?current=${current}`)

export const aiAssistBlog = (data) => api.post('/blog/ai-assist', data)
