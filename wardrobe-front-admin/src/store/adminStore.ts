import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { AdminUser } from '@/types'

const LS_ADMIN_KEY = 'wardrobe:admin'

export const useAdminStore = defineStore('admin', () => {
  const raw = localStorage.getItem(LS_ADMIN_KEY)
  const user = ref<AdminUser | null>(raw ? (JSON.parse(raw) as AdminUser) : null)

  const isLoggedIn = computed(() => !!user.value)
  const userName = computed(() => user.value?.userName ?? '未登录')

  function login(u: AdminUser) {
    user.value = { ...u }
    localStorage.setItem(LS_ADMIN_KEY, JSON.stringify(u))
  }

  function logout() {
    user.value = null
    localStorage.removeItem(LS_ADMIN_KEY)
  }

  return { user, isLoggedIn, userName, login, logout }
})
