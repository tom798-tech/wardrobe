import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router/router'
import './assets/main.css'

const app = createApp(App)

// Pinia 状态管理
const pinia = createPinia()
app.use(pinia)

// Vue Router
app.use(router)

// Element Plus 全局注册 (中文语言)
app.use(ElementPlus, { locale: zhCn })

// 全局注册 Element Plus 所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component as never)
}

app.mount('#app')
