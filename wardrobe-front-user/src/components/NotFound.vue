<template>
  <div class="not-found">
    <div class="card">
      <div class="code">404</div>
      <h2 class="title">页面走丢了</h2>
      <p class="desc">你访问的页面不存在，或者已被移动。</p>
      <div class="actions">
        <el-button @click="$router.back()">返回上一页</el-button>
        <el-button type="primary" @click="$router.replace({ name: 'Home' })">去首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/* component only */
</script>

<style scoped>
.not-found {
  min-height: calc(100vh - 120px);
  display: flex; align-items: center; justify-content: center; padding: 32px 16px;
}
.card { text-align: center; }
.code {
  font-size: 160px; font-weight: 900; line-height: 1; margin-bottom: 8px;
  background: linear-gradient(135deg, #409eff, #a0cfff);
  -webkit-background-clip: text; background-clip: text; color: transparent;
}
.title { font-size: 28px; color: #303133; margin: 0 0 6px; }
.desc { color: #909399; margin: 0 0 20px; }
.actions { display: inline-flex; gap: 10px; }
</style>
