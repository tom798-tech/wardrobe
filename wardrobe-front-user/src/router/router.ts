import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import UserRoot from '@/components/UserRoot.vue'
import NotFound from '@/components/NotFound.vue'
import { useUserStore } from '@/store/userStore'

const userRoutes: RouteRecordRaw[] = [
  { path: 'Home', name: 'Home', component: () => import('@/components/Home.vue'), meta: { title: '首页' } },
  { path: 'Category', redirect: { name: 'Home' } },
  { path: 'Style', redirect: { name: 'Home' } },
  { path: 'clothes/:id', name: 'ClothesDetail', component: () => import('@/components/ClothesDetail.vue'), meta: { title: '商品详情' } },
  { path: 'Login', name: 'Login', component: () => import('@/components/Login.vue'), meta: { title: '登录' } },
  { path: 'Register', name: 'Register', component: () => import('@/components/Register.vue'), meta: { title: '注册' } },
  { path: 'Cart', name: 'Cart', component: () => import('@/components/Cart.vue'), meta: { title: '购物车', requireLogin: true } },
  { path: 'Checkout', name: 'Checkout', component: () => import('@/components/Checkout.vue'), meta: { title: '确认订单', requireLogin: true } },
  { path: 'Order', name: 'Order', component: () => import('@/components/Order.vue'), meta: { title: '我的订单', requireLogin: true } },
  { path: 'Profile', name: 'Profile', component: () => import('@/components/Profile.vue'), meta: { title: '个人中心', requireLogin: true } },
]

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/user/Home' },
  { path: '/home', redirect: { name: 'Home' } },
  { path: '/user', component: UserRoot, children: userRoutes },
  // ===== 旧项目路径兼容层（防 404 双保险） =====
  { path: '/login', redirect: { name: 'Login' } },
  { path: '/register', redirect: { name: 'Register' } },
  { path: '/cart', redirect: { name: 'Cart' } },
  { path: '/checkout', redirect: { name: 'Checkout' } },
  // 订单：旧项目习惯用小写 order，兼容大小写
  { path: '/order', redirect: { name: 'Order' } },
  { path: '/user/order', redirect: { name: 'Order' } },
  // 个人中心：旧项目路径是 /personal /profile，兼容大小写和 query userId
  { path: '/profile', redirect: { name: 'Profile' } },
  { path: '/personal', redirect: { name: 'Profile' } },
  { path: '/user/profile', redirect: { name: 'Profile' } },
  { path: '/user/personal', redirect: { name: 'Profile' } },
  // 分类 / 风格：旧项目入口独立，现在统一在首页用筛选器浏览
  { path: '/category', redirect: { name: 'Category' } },
  { path: '/style', redirect: { name: 'Style' } },
  { path: '/user/category', redirect: { name: 'Category' } },
  { path: '/user/style', redirect: { name: 'Style' } },
  // 商品详情：旧项目 /clothDetails?clothId=1 或 /clothes/:id
  {
    path: '/clothDetails',
    redirect: (to) => {
      const clothId = to.query.clothId as string | undefined
      if (clothId && /^\d+$/.test(clothId)) {
        return { name: 'ClothesDetail', params: { id: clothId } }
      }
      return { name: 'Home' }
    },
  },
  { path: '/clothDetails/:id(\\d+)', redirect: (to) => ({ name: 'ClothesDetail', params: to.params }) },
  { path: '/clothesDetails', redirect: (to) => {
      const clothId = to.query.clothId as string | undefined
      if (clothId && /^\d+$/.test(clothId)) return { name: 'ClothesDetail', params: { id: clothId } }
      return { name: 'Home' }
  } },
  { path: '/clothes/:id(\\d+)', redirect: (to) => ({ name: 'ClothesDetail', params: to.params }) },
  // ===== 兼容层结束 =====
  { path: '/:pathMatch(.*)*', component: NotFound, meta: { title: '404' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() { return { top: 0 } },
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  userStore.tryLoginFromStorage()
  if (to.meta?.requireLogin && !userStore.isLoggedIn) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }
  if (to.meta?.title && typeof to.meta.title === 'string') {
    document.title = `云衣橱 - ${to.meta.title}`
  }
  return true
})

export default router
