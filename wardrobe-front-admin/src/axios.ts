import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

service.interceptors.request.use(
  (config) => {
    const raw = localStorage.getItem('wardrobe:admin')
    if (raw) {
      try {
        const u = JSON.parse(raw) as { token?: string }
        if (u?.token) {
          config.headers.token = u.token
          config.headers.Authorization = `Bearer ${u.token}`
        }
      } catch { /* ignore */ }
    }
    return config
  },
  (err) => Promise.reject(err),
)

service.interceptors.response.use(
  (res) => {
    // 后端大部分接口直接返回 T / List<T> / String（无统一 Result 外壳）
    // 所以这里返回 res.data 以与老代码一致
    return res.data
  },
  (err) => {
    const msg =
      err?.response?.status === 401 ? '登录已过期，请重新登录' :
      err?.response?.status === 403 ? (err?.response?.data?.message ?? '没有管理员权限') :
      err?.code === 'ECONNABORTED' ? '请求超时，请检查网络' :
      typeof err?.response?.data === 'string' ? err.response.data :
      (err?.message ?? '请求失败')
    ElMessage.error(msg)
    return Promise.reject(err)
  },
)

export default service
