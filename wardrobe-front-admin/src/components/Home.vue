<template>
  <div class="admin-home">
    <el-container class="home-container">
      <el-header class="topbar">
        <el-row style="width: 100%" align="middle">
          <el-col :span="5">
            <span class="system-name">网上衣橱 · 后台管理</span>
          </el-col>
          <el-col :span="19">
            <div class="topbar-right">
              <el-avatar shape="square" :size="36" :src="avatarSrc" class="avatar" />
              <el-dropdown @command="handleCommand">
                <span class="dropdown-link">
                  {{ adminStore.userName }}
                  <el-icon class="arrow"><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="logout">退出系统</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </el-col>
        </el-row>
      </el-header>

      <el-container class="body">
        <el-aside class="sidebar" :width="isCollapse ? '64px' : '210px'">
          <div class="collapse-btn" @click="isCollapse = !isCollapse" :title="isCollapse ? '展开' : '收起'">
            <el-icon :size="18"><Expand v-if="isCollapse" /><Fold v-else /></el-icon>
          </div>
          <el-menu
            :default-active="activePath"
            :collapse="isCollapse"
            router
            background-color="#ffffff"
            text-color="#303133"
            active-text-color="#ffffff"
            class="nav-menu"
          >
            <el-menu-item index="/index" @click="saveActive('/index')">
              <el-icon><House /></el-icon>
              <template #title>首页</template>
            </el-menu-item>
            <el-menu-item index="/clothes" @click="saveActive('/clothes')">
              <el-icon><Goods /></el-icon>
              <template #title>服装管理</template>
            </el-menu-item>
            <el-menu-item index="/order" @click="saveActive('/order')">
              <el-icon><ShoppingCart /></el-icon>
              <template #title>订单管理</template>
            </el-menu-item>
            <el-menu-item index="/user" @click="saveActive('/user')">
              <el-icon><UserFilled /></el-icon>
              <template #title>用户管理</template>
            </el-menu-item>
            <el-menu-item index="/brand" @click="saveActive('/brand')">
              <el-icon><Collection /></el-icon>
              <template #title>品牌管理</template>
            </el-menu-item>
            <el-menu-item index="/review" @click="saveActive('/review')">
              <el-icon><ChatLineSquare /></el-icon>
              <template #title>评论管理</template>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-container class="main">
          <el-main class="content">
            <router-view v-slot="{ Component }">
              <transition name="fade" mode="out-in">
                <component :is="Component" />
              </transition>
            </router-view>
          </el-main>
          <el-footer class="footer">联系我们：https://www.itcast.cn/</el-footer>
        </el-container>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown, ChatLineSquare, Collection, Expand, Fold, Goods, House, ShoppingCart, UserFilled,
} from '@element-plus/icons-vue'
import avatarImg from '@/assets/img/avator.jpg'
import { useAdminStore } from '@/store/adminStore'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

const avatarSrc = ref(avatarImg)
const isCollapse = ref(false)
const activePath = ref('/index')

function saveActive(path: string) {
  localStorage.setItem('wardrobe:activePath', path)
  activePath.value = path
}

function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      .then(() => {
        adminStore.logout()
        router.replace({ path: '/login' })
      }).catch(() => { /* cancel */ })
  }
}

onMounted(() => {
  if (!adminStore.isLoggedIn) {
    router.replace({ path: '/login' })
    return
  }
  activePath.value = localStorage.getItem('wardrobe:activePath') ?? route.path ?? '/index'
})
</script>

<style scoped>
.admin-home, .home-container { height: 100%; }
.topbar {
  background: #4682B4; color: #fff;
  padding: 0 20px;
  display: flex; align-items: center;
  border-bottom: 1px solid rgba(255,255,255,.15);
}
.system-name { color: #fff; font-size: 18px; font-weight: 700; letter-spacing: .5px; }
.topbar-right { display: flex; align-items: center; justify-content: flex-end; gap: 12px; }
.avatar { background: #fff; }
.dropdown-link { color: #fff; cursor: pointer; display: inline-flex; align-items: center; gap: 6px; }
.arrow { font-size: 12px; }

.body { height: calc(100vh - 60px); }
.sidebar {
  background: #fff; border-right: 1px solid #ebeef5;
  display: flex; flex-direction: column; transition: width .2s ease;
  overflow: hidden;
}
.collapse-btn {
  text-align: center;
  padding: 10px 0; cursor: pointer;
  color: #606266; background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}
.nav-menu { flex: 1; border-right: none !important; padding-top: 6px; }
.nav-menu :deep(.el-menu-item.is-active) {
  background-color: #4682B4 !important;
  border-radius: 4px;
  margin: 2px 6px !important;
  color: #fff !important;
  font-weight: 600;
  height: 46px !important;
  line-height: 46px !important;
}

.main { background: #f2f3f5; }
.content { padding: 16px; overflow: auto; }
.footer {
  background: #fff; color: #cccccc; text-align: center; line-height: 60px;
  border-top: 1px solid #ebeef5;
}
.footer:hover { color: #4682B4; }

.fade-enter-active, .fade-leave-active { transition: opacity .2s ease, transform .2s ease; }
.fade-enter-from { opacity: 0; transform: translateY(6px); }
.fade-leave-to { opacity: 0; transform: translateY(-6px); }
</style>
