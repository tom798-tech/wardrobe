<template>
  <footer class="footer">
    <div class="inner">
      <div class="col">
        <h4 class="title">关于云衣橱</h4>
        <p class="text">一站式服饰电商 & 个人穿搭灵感平台。精选好货、贴心服务。</p>
      </div>
      <div class="col">
        <h4 class="title">快捷入口</h4>
        <ul class="list">
          <li><router-link :to="{ name: 'Home' }">首页</router-link></li>
          <li><router-link :to="{ name: 'Cart' }">购物车</router-link></li>
          <li><router-link :to="{ name: 'Order' }">我的订单</router-link></li>
          <li><router-link :to="{ name: 'Profile' }">个人中心</router-link></li>
        </ul>
      </div>
      <div class="col">
        <h4 class="title">联系我们</h4>
        <ul class="list text">
          <li><el-icon><Phone /></el-icon> 400-000-0000</li>
          <li><el-icon><Message /></el-icon> support@wardrobe.demo</li>
          <li><el-icon><Location /></el-icon> 上海</li>
        </ul>
      </div>
      <div class="col">
        <h4 class="title">技术栈</h4>
        <ul class="chips">
          <li>Vue 3.5</li>
          <li>Vite 5</li>
          <li>TypeScript 5.6</li>
          <li>Element Plus</li>
          <li>Pinia</li>
          <li>Vue Router 4</li>
        </ul>
      </div>
    </div>
    <div class="copy">
      © {{ year }} Wardrobe Cloud · Built with ❤️ for fashion lovers
    </div>
  </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Location, Message, Phone } from '@element-plus/icons-vue'
const year = computed(() => new Date().getFullYear())
</script>

<style scoped>
.footer {
  margin-top: 48px;
  background: #1f2937; color: #cbd5e1;
  padding: 36px 24px 0;
}
.inner {
  max-width: 1280px; margin: 0 auto;
  display: grid; gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}
.col { display: flex; flex-direction: column; gap: 10px; }
.title { margin: 0 0 4px; color: #f8fafc; font-size: 15px; letter-spacing: .5px; }
.text { margin: 0; line-height: 1.7; color: #94a3b8; }
.list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
.list li { display: inline-flex; align-items: center; gap: 6px; color: #94a3b8; }
.list a { color: #94a3b8; text-decoration: none; }
.list a:hover { color: #60a5fa; text-decoration: underline; }
.chips { list-style: none; padding: 0; margin: 0; display: flex; flex-wrap: wrap; gap: 6px; }
.chips li {
  padding: 4px 10px; border-radius: 999px; background: rgba(148,163,184,.15);
  color: #cbd5e1; font-size: 12px;
}
.copy { max-width: 1280px; margin: 28px auto 0; padding: 16px 0; border-top: 1px solid rgba(255,255,255,.08);
  color: #64748b; font-size: 13px; text-align: center; }
</style>
