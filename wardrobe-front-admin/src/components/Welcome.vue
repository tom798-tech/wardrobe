<template>
  <div class="welcome">
    <div class="hero">
      <h2 class="hello">欢迎回来，<b>{{ adminStore.userName }}</b> 👋</h2>
      <p class="sub">今天也要元气满满地管理商城哦，以下是实时数据概览。</p>
    </div>

    <el-row :gutter="16">
      <el-col v-for="(item, i) in stats" :key="i" :xs="12" :sm="8" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" :style="{ background: item.bg }">
            <el-icon :size="24" color="#fff"><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="label">{{ item.label }}</div>
            <div class="value">{{ item.value }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <template #header><b>最近订单</b>（最多 8 条）</template>
          <el-table :data="recentOrders" size="small" stripe>
            <el-table-column prop="id" label="编号" width="80" align="center" />
            <el-table-column prop="userName" label="用户" align="center" />
            <el-table-column label="金额" align="center" width="120">
              <template #default="{ row }">¥ {{ Number(row.price ?? 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="状态" align="center" width="110">
              <template #default="{ row }">
                <el-tag :type="tagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="time" label="时间" align="center" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card shadow="never">
          <template #header><b>最新评论</b>（最多 6 条）</template>
          <div v-if="!recentReviews.length" style="padding: 36px 0">
            <el-empty description="暂无评论" />
          </div>
          <div v-else class="review-list">
            <div v-for="r in recentReviews" :key="r.id" class="review-item">
              <div class="review-head">
                <b>{{ r.userName ?? '匿名' }}</b>
                <el-rate disabled :model-value="r.rating ?? 5" size="small" />
              </div>
              <div class="review-body">{{ r.content }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, markRaw } from 'vue'
import { Goods, Collection, Message, ShoppingCart, User as UserIcon } from '@element-plus/icons-vue'
import type { Brand, Clothes as ClothesT, Order, OrderStatus, Review, User } from '@/types'
import request from '@/axios'
import { useAdminStore } from '@/store/adminStore'

const adminStore = useAdminStore()

const orders = ref<Order[]>([])
const clothes = ref<ClothesT[]>([])
const users = ref<User[]>([])
const brands = ref<Brand[]>([])
const reviews = ref<Review[]>([])

const recentOrders = computed(() => [...orders.value].sort((a, b) => (b.id ?? 0) - (a.id ?? 0)).slice(0, 8))
const recentReviews = computed(() => [...reviews.value].sort((a, b) => (b.id ?? 0) - (a.id ?? 0)).slice(0, 6))

const stats = computed(() => [
  { label: '服装数',  value: clothes.value.length, bg: 'linear-gradient(135deg,#f093fb,#f5576c)', icon: markRaw(Goods) },
  { label: '品牌数',  value: brands.value.length,  bg: 'linear-gradient(135deg,#4facfe,#00f2fe)', icon: markRaw(Collection) },
  { label: '用户数',  value: users.value.length,   bg: 'linear-gradient(135deg,#43e97b,#38f9d7)', icon: markRaw(UserIcon) },
  { label: '订单数',  value: orders.value.length,  bg: 'linear-gradient(135deg,#fa709a,#fee140)', icon: markRaw(ShoppingCart) },
  { label: '评论数',  value: reviews.value.length, bg: 'linear-gradient(135deg,#30cfd0,#330867)', icon: markRaw(Message) },
  { label: '待发货',  value: orders.value.filter(o => o.status === 1).length, bg: 'linear-gradient(135deg,#a18cd1,#fbc2eb)', icon: markRaw(ShoppingCart) },
])

function statusText(s?: number) {
  switch (s) {
    case 0: return '待支付'; case 1: return '待发货'
    case 2: return '待收货'; case 3: return '已完成'
    default: return '未知'
  }
}
function tagType(s: OrderStatus) {
  switch (s) {
    case 0: return 'warning' as const
    case 1: return 'primary' as const
    case 2: return 'info' as const
    case 3: return 'success' as const
    default: return 'info' as const
  }
}

async function loadAll() {
  try {
    const [o, c, u, b, r] = (await Promise.all([
      request.get('/order/all'),
      request.get('/clothes'),
      request.get('/user'),
      request.get('/brand'),
      request.get('/review'),
    ])) as unknown as [Order[], ClothesT[], User[], Brand[], Review[]]
    orders.value = Array.isArray(o) ? o : []
    clothes.value = Array.isArray(c) ? c : []
    users.value = Array.isArray(u) ? u : []
    brands.value = Array.isArray(b) ? b : []
    reviews.value = Array.isArray(r) ? r : []
  } catch { /* ignore */ }
}

onMounted(loadAll)
</script>

<style scoped>
.welcome { display: flex; flex-direction: column; gap: 16px; }
.hero {
  padding: 22px 24px; border-radius: 12px;
  background: linear-gradient(135deg, #4682B4 0%, #87b7e0 100%);
  color: #fff;
}
.hello { margin: 0; font-size: 22px; font-weight: 500; }
.hello b { color: #fff; }
.sub { margin: 6px 0 0; opacity: .95; }

.stat-card {
  display: flex; align-items: center; gap: 14px; padding: 12px 10px;
  border: 1px solid #ebeef5; border-radius: 10px;
}
.stat-icon {
  width: 52px; height: 52px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  flex: 0 0 52px;
  box-shadow: 0 6px 16px rgba(0,0,0,.08);
}
.stat-info { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.stat-info .label { color: #909399; font-size: 13px; }
.stat-info .value { color: #303133; font-size: 24px; font-weight: 700; line-height: 1.1; }

.review-list { display: flex; flex-direction: column; gap: 10px; }
.review-item {
  padding: 10px 12px; border: 1px solid #f0f2f5; border-radius: 8px;
}
.review-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.review-body { color: #606266; line-height: 1.6; }
</style>
