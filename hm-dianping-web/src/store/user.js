import { defineStore } from 'pinia'
import { logout as logoutApi } from '../services/user'

const TOKEN_KEY = 'hmdp_token'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    profile: null,
  }),
  actions: {
    setToken(token) {
      this.token = token || ''
      if (token) {
        localStorage.setItem(TOKEN_KEY, token)
      } else {
        localStorage.removeItem(TOKEN_KEY)
      }
    },
    setProfile(profile) {
      this.profile = profile
    },
    async logout() {
      try {
        await logoutApi()
      } finally {
        this.setToken('')
        this.profile = null
      }
    },
  },
})
