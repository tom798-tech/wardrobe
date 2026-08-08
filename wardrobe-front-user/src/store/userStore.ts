import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types'
import request from '@/axios'

const LS_USER_KEY = 'wardrobe:user'
const LS_REMEMBER = 'wardrobe:remember'

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const remember = ref(false)
  const cartCount = ref(0)

  const isLoggedIn = computed(() => !!user.value)
  // 兼容 axios.ts 的旧访问方式 userStore.token，以及直接把 token 从 user 对象里抽出来
  const token = computed(() => user.value?.token ?? null)
  // App.vue 中 userStore.userInfo 的别名访问（老代码兼容）
  const userInfo = computed<User | null>(() => user.value)

  function persist() {
    if (user.value && remember.value) {
      localStorage.setItem(LS_USER_KEY, JSON.stringify(user.value))
      localStorage.setItem(LS_REMEMBER, '1')
    } else if (user.value) {
      sessionStorage.setItem(LS_USER_KEY, JSON.stringify(user.value))
      localStorage.removeItem(LS_USER_KEY)
      localStorage.removeItem(LS_REMEMBER)
    } else {
      localStorage.removeItem(LS_USER_KEY)
      localStorage.removeItem(LS_REMEMBER)
      sessionStorage.removeItem(LS_USER_KEY)
    }
  }

  function tryLoginFromStorage() {
    if (user.value) return
    const raw = sessionStorage.getItem(LS_USER_KEY) ?? localStorage.getItem(LS_USER_KEY)
    remember.value = localStorage.getItem(LS_REMEMBER) === '1'
    if (!raw) return
    try {
      user.value = JSON.parse(raw) as User
    } catch {
      user.value = null
    }
  }

  function login(u: User, rememberMe = true) {
    user.value = { ...u }
    remember.value = rememberMe
    persist()
  }

  function logout() {
    user.value = null
    cartCount.value = 0
    persist()
  }

  async function refreshCartCount() {
    if (!user.value) { cartCount.value = 0; return }
    try {
      const list = await request.get('/cart')
      if (Array.isArray(list)) {
        cartCount.value = list.reduce((s: number, x: { amount?: number }) => s + (x.amount ?? 0), 0)
      }
    } catch {
      cartCount.value = 0
    }
  }

  return {
    user, userInfo, remember, cartCount, token, isLoggedIn,
    tryLoginFromStorage, login, logout, persist, refreshCartCount,
  }
})
