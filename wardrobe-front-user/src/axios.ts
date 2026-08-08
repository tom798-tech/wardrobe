import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/userStore'
import type { ResultEnvelope } from '@/types'

// axios 实例：baseURL '/api' 走 Vite dev proxy / Nginx 反代
const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
})

// 请求拦截器：自动带上登录 token
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    try {
      const userStore = useUserStore()
      if (userStore.token) {
        // Sa-Token / 原项目使用的 tokenName = 'token'
        config.headers.token = userStore.token
      }
    } catch {
      // Pinia 在某些早期生命周期未初始化时忽略
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器：
//   1. 原项目后端直接返回字符串 "成功"/"操作失败" 或 JSON，
//      所以这里同时兼容两种情况，返回 response.data
//   2. 401 / token 失效则清空 store 并跳转登录
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const data = response.data as ResultEnvelope | string | unknown

    // 字符串直接返回（原项目大多接口返回字符串）
    if (typeof data === 'string') return data

    // 有 code/message 结构的通用响应
    if (data && typeof data === 'object' && 'code' in data) {
      const env = data as ResultEnvelope
      if (env.code !== undefined && env.code !== 200 && env.code !== 0) {
        ElMessage.error(typeof env.message === 'string' ? env.message : '请求失败')
      }
    }

    // 401 未授权
    if (response.status === 401) {
      try {
        const userStore = useUserStore()
        userStore.logout()
      } catch { /* ignore */ }
    }

    return data
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      try {
        const userStore = useUserStore()
        userStore.logout()
      } catch { /* ignore */ }
      ElMessage.error('登录已过期，请重新登录')
    } else {
      const msg = error?.response?.data?.message ?? error?.message ?? '网络请求失败'
      ElMessage.error(typeof msg === 'string' ? msg : '网络请求失败')
    }
    return Promise.reject(error)
  },
)

export default request
