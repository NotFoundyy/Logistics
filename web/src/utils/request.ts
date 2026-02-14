import axios from 'axios'
import type { AxiosError, InternalAxiosRequestConfig } from 'axios'
import { TOKEN_KEY, USER_KEY } from '../constants/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

let redirectingToLogin = false

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => response,
  (error: AxiosError<{ message?: string }>) => {
    const status = error.response?.status
    if (status === 401) {
      const hasToken = Boolean(localStorage.getItem(TOKEN_KEY))
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)

      if (hasToken && !redirectingToLogin) {
        redirectingToLogin = true
        const loginUrl = '/login?expired=1'
        if (window.location.pathname !== '/login') {
          window.location.replace(loginUrl)
        }
        window.setTimeout(() => {
          redirectingToLogin = false
        }, 1200)
      }

      // 401 在这里统一处理，避免页面内多个请求同时失败时重复弹错。
      return new Promise(() => undefined)
    }

    const message = error.response?.data?.message || error.message || '请求失败'
    return Promise.reject(new Error(message))
  },
)

export default request
