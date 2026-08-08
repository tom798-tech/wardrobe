<template>
  <el-container class="app-layout">
    <el-header class="app-header">
      <div class="header-inner">
        <router-link to="/home" class="brand">
          <el-icon :size="24"><ShoppingBag /></el-icon>
          <span>云衣橱</span>
        </router-link>
        <el-menu mode="horizontal" :default-active="route.path" router class="nav-menu">
          <el-menu-item index="/user/Home">首页</el-menu-item>
          <el-menu-item index="/user/Cart">
            <el-badge v-if="cartCount > 0" :value="cartCount" :max="99" class="cart-badge">
              购物车
            </el-badge>
            <span v-else>购物车</span>
          </el-menu-item>
          <el-menu-item index="/user/Order">我的订单</el-menu-item>
          <el-menu-item index="/user/Profile">个人中心</el-menu-item>
        </el-menu>
        <div class="header-right">
          <template v-if="userStore.isLoggedIn && userStore.userInfo">
            <el-avatar :src="resolveAvatar(userStore.userInfo.avatar)" :size="32" />
            <span class="welcome">{{ userStore.userInfo.userName }}</span>
            <el-button size="small" @click="handleLogout">退出登录</el-button>
          </template>
          <template v-else>
            <router-link to="/login">
              <el-button size="small" type="primary">登录</el-button>
            </router-link>
            <router-link to="/register">
              <el-button size="small">注册</el-button>
            </router-link>
          </template>
        </div>
      </div>
    </el-header>

    <el-main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>

    <el-footer class="app-footer">
      <p>© {{ year }} 云衣橱 Wardrobe Demo</p>
    </el-footer>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ShoppingBag } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/userStore'
import request from '@/axios'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const year = new Date().getFullYear()
const cartCount = ref<number>(0)

function resolveAvatar(url?: string | null) {
  if (!url) return 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `/api/images/${url}`
}

async function loadCartCount() {
  if (!userStore.isLoggedIn) {
    cartCount.value = 0
    return
  }
  try {
    const list = (await request.get('/cart')) as unknown[]
    cartCount.value = Array.isArray(list) ? list.length : 0
  } catch {
    cartCount.value = 0
  }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录?', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await request.post('/logout')
  } catch { /* ignore */ }
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'Home' })
}

onMounted(loadCartCount)
</script>

<style scoped>
.app-layout { min-height: 100vh; display: flex; flex-direction: column; }
.app-header {
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, .04);
  padding: 0;
}
.header-inner {
  max-width: 1280px; margin: 0 auto; height: 100%;
  display: flex; align-items: center; gap: 24px; padding: 0 16px;
}
.brand {
  display: inline-flex; align-items: center; gap: 8px;
  font-size: 20px; font-weight: 700; color: var(--el-color-primary);
  text-decoration: none;
}
.nav-menu { flex: 1; border-bottom: none; }
.cart-badge { margin-right: 8px; }
.header-right { display: inline-flex; align-items: center; gap: 12px; }
.welcome { color: #606266; font-size: 14px; }
.app-main {
  flex: 1; width: 100%; max-width: 1280px; margin: 0 auto;
  padding: 24px 16px; box-sizing: border-box;
}
.app-footer {
  text-align: center; color: #909399; font-size: 12px;
  padding: 24px 0; border-top: 1px solid #ebeef5; background: #fafafa;
}
.fade-enter-active, .fade-leave-active { transition: opacity .2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
