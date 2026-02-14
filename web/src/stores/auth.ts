import { defineStore } from 'pinia'
import { TOKEN_KEY, USER_KEY } from '../constants/auth'
import type { AuthLoginResponse, AuthUserProfile } from '../types/auth'

interface AuthState {
  token: string
  user: AuthUserProfile | null
}

function parseUser(raw: string | null): AuthUserProfile | null {
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as AuthUserProfile
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: parseUser(localStorage.getItem(USER_KEY)),
  }),

  getters: {
    isLogin: (state) => Boolean(state.token),
    isAdmin: (state) => Boolean(state.user?.roles.includes('ROLE_ADMIN')),
    isCourier: (state) => Boolean(state.user?.roles.includes('ROLE_COURIER')),
  },

  actions: {
    setLogin(payload: AuthLoginResponse) {
      this.token = payload.accessToken
      this.user = payload.user
      localStorage.setItem(TOKEN_KEY, payload.accessToken)
      localStorage.setItem(USER_KEY, JSON.stringify(payload.user))
    },

    setUser(user: AuthUserProfile) {
      this.user = user
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    },

    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
})
