<template>
  <header class="navbar">
    <div class="inner">
      <router-link :to="{ name: 'Home' }" class="logo">
        <el-icon color="#409eff" :size="28"><Goods /></el-icon>
        <span>云衣橱 · Wardrobe</span>
      </router-link>
      <nav class="links">
        <router-link class="link" :to="{ name: 'Home' }">首页</router-link>
        <router-link class="link" :to="{ name: 'Cart' }">
          <el-icon><ShoppingCartFull /></el-icon>
          <span>购物车</span>
          <el-badge v-if="userStore.cartCount > 0" class="cart-badge"
            :value="userStore.cartCount" :max="99" />
        </router-link>
        <router-link v-if="!!userStore.user" class="link" :to="{ name: 'Order' }">我的订单</router-link>
        <router-link v-if="!!userStore.user" class="link" :to="{ name: 'Profile' }">
          <el-avatar :size="24" class="mini-avatar">{{ avatarInitial }}</el-avatar>
          <span>{{ userStore.user.userName }}</span>
        </router-link>
        <router-link v-if="userStore.user?.role === 1" target="_blank" class="link link-admin" :to="'/admin'">
          <el-icon><Management /></el-icon><span>后台</span>
        </router-link>
        <router-link v-if="!userStore.user" class="link link-login" :to="{ name: 'Login' }">登录</router-link>
        <router-link v-if="!userStore.user" class="link link-register" :to="{ name: 'Register' }">注册</router-link>
      </nav>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { Goods, Management, ShoppingCartFull } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/userStore'

const userStore = useUserStore()
const route = useRoute()

const avatarInitial = computed(() =>
  (userStore.user?.userName ?? 'U').charAt(0).toUpperCase(),
)

onMounted(() => { userStore.tryLoginFromStorage(); userStore.refreshCartCount() })
watch(() => route.fullPath, () => userStore.refreshCartCount())
</script>

<style scoped>
.navbar {
  position: sticky; top: 0; z-index: 100;
  background: #fff; border-bottom: 1px solid #ebeef5;
  box-shadow: 0 1px 4px rgba(0,0,0,.04);
}
.inner {
  max-width: 1280px; margin: 0 auto; padding: 12px 24px;
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
}
.logo {
  display: inline-flex; align-items: center; gap: 8px;
  color: #303133; font-weight: 700; font-size: 20px; text-decoration: none;
}
.links { display: inline-flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.link {
  position: relative; padding: 8px 14px; border-radius: 8px;
  display: inline-flex; align-items: center; gap: 6px;
  color: #606266; text-decoration: none; transition: all .2s ease;
}
.link:hover { background: #ecf5ff; color: var(--el-color-primary); }
.link.router-link-exact-active { color: var(--el-color-primary); font-weight: 600; }
.mini-avatar { background: var(--el-color-primary-light-5); color: var(--el-color-primary); font-weight: 600; font-size: 12px; }
.cart-badge { margin-left: 2px; }
.link-admin { color: var(--el-color-danger); }
.link-login { color: var(--el-color-primary); }
.link-register {
  background: var(--el-color-primary); color: #fff; border-radius: 999px; padding: 6px 16px;
}
.link-register:hover { background: var(--el-color-primary-dark-2); color: #fff; }
</style>
