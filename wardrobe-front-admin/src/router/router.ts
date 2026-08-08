import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import Home from '@/components/Home.vue'
import Login from '@/components/Login.vue'
import Welcome from '@/components/Welcome.vue'
import ClothesManage from '@/components/ClothesManage/ClothesManage.vue'
import OrderManage from '@/components/OrderManage/OrderManage.vue'
import UserManage from '@/components/UserManage/UserManage.vue'
import BrandManage from '@/components/BrandManage/BrandManage.vue'
import ReviewManage from '@/components/ReviewManage/ReviewManage.vue'
import { useAdminStore } from '@/store/adminStore'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/index' },
  { path: '/login', name: 'Login', component: Login, meta: { title: '管理员登录' } },
  {
    path: '/home',
    component: Home,
    redirect: '/index',
    meta: { title: '后台主页', requiresAuth: true },
    children: [
      { path: '/index', name: 'Welcome', component: Welcome, meta: { title: '首页概览' } },
      { path: '/clothes', name: 'ClothesManage', component: ClothesManage, meta: { title: '服装管理', requiresAuth: true } },
      { path: '/order', name: 'OrderManage', component: OrderManage, meta: { title: '订单管理', requiresAuth: true } },
      { path: '/user', name: 'UserManage', component: UserManage, meta: { title: '用户管理', requiresAuth: true } },
      { path: '/brand', name: 'BrandManage', component: BrandManage, meta: { title: '品牌管理', requiresAuth: true } },
      { path: '/review', name: 'ReviewManage', component: ReviewManage, meta: { title: '评论管理', requiresAuth: true } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/index' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to) => {
  const adminStore = useAdminStore()
  if (to.meta?.requiresAuth && !adminStore.isLoggedIn) {
    return { name: 'Login' }
  }
  if (to.meta?.title && typeof to.meta.title === 'string') {
    document.title = `云衣橱后台 - ${to.meta.title}`
  }
  return true
})

export default router
