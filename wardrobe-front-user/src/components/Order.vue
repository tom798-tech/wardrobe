<template>
  <div class="order-page">
    <div class="page-head">
      <h2 class="page-title"><el-icon><List /></el-icon>我的订单</h2>
      <el-tabs v-model="activeStatus" @tab-change="refresh">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待支付" :name="'0'" />
        <el-tab-pane label="待发货" :name="'1'" />
        <el-tab-pane label="待收货" :name="'2'" />
        <el-tab-pane label="已完成" :name="'3'" />
      </el-tabs>
    </div>

    <div v-loading="loading" class="order-list">
      <el-empty v-if="!list.length" description="暂无订单，去选购心仪的衣服吧~">
        <el-button type="primary" @click="$router.push({ name: 'Home' })">去选购</el-button>
      </el-empty>
      <el-card v-for="o in list" :key="o.id" class="order-card" shadow="never">
        <div class="card-head">
          <span class="order-id">订单号: #{{ o.id }}</span>
          <span class="order-time">{{ o.time ?? '' }}</span>
          <el-tag :type="tagType(o.status)">{{ statusText(o.status) }}</el-tag>
        </div>
        <div class="card-body">
          <div v-for="(g, i) in parseItems(o)" :key="`${o.id}-${i}`" class="goods">
            <img :src="resolveCover(g)" class="cover" />
            <div class="info">
              <div class="name">{{ g.clothName }}</div>
              <div class="tags"><el-tag size="small" effect="light">{{ g.clothSize || '默认' }}</el-tag></div>
              <div class="row-between">
                <span class="price">¥ {{ Number(g.price ?? 0).toFixed(2) }}</span>
                <span>x{{ g.amount }}</span>
              </div>
            </div>
          </div>
          <div class="summary-box">
            <div class="line">总金额：<span class="total">¥ {{ Number(o.price ?? 0).toFixed(2) }}</span></div>
            <div class="line">收货信息：{{ o.address }}</div>
            <div class="actions">
              <el-button
                v-if="o.status === 0"
                type="primary" size="small" :icon="Wallet" @click="pay(o)"
              >去支付</el-button>
              <el-button
                v-if="o.status === 2"
                type="success" size="small" :icon="Check" @click="receive(o)"
              >确认收货</el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Check, List, Wallet } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { Order } from '@/types'
import request from '@/axios'
import { useUserStore } from '@/store/userStore'

const userStore = useUserStore()
const loading = ref(false)
const allList = ref<Order[]>([])
const activeStatus = ref<string>('')

interface OrderItemView { clothName: string; clothSize?: string; price?: number; amount?: number; cover?: string }

const list = computed(() => {
  if (activeStatus.value === '') return allList.value
  const s = Number(activeStatus.value)
  return allList.value.filter(o => o.status === s)
})

function statusText(s?: number) {
  switch (s) {
    case 0: return '待支付'
    case 1: return '待发货'
    case 2: return '待收货'
    case 3: return '已完成'
    default: return '未知状态'
  }
}
function tagType(s?: number) {
  switch (s) {
    case 0: return 'warning'
    case 1: return 'primary'
    case 2: return ''
    case 3: return 'success'
    default: return 'info'
  }
}
function parseItems(o: Order): OrderItemView[] {
  try {
    const raw = o.clothesDetails ?? ''
    if (!raw) return []
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}
function resolveCover(g: OrderItemView) {
  const url = g.cover ?? ''
  if (!url) return 'https://placehold.co/120x120/f5f7fa/909399?text=No+Image'
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `/api/images/${url}`
}

async function refresh() {
  if (!userStore.user) { allList.value = []; return }
  loading.value = true
  try {
    allList.value = (await request.get('/order')) as Order[]
    allList.value.sort((a, b) => (b.id ?? 0) - (a.id ?? 0))
  } catch {
    allList.value = []
  } finally {
    loading.value = false
  }
}
async function pay(o: Order) {
  try {
    await request.put(`/order/pay/${o.id}`)
    ElMessage.success('支付成功')
    refresh()
  } catch { ElMessage.error('支付失败') }
}
async function receive(o: Order) {
  try {
    await request.put(`/order/receive/${o.id}`)
    ElMessage.success('已确认收货')
    refresh()
  } catch { ElMessage.error('操作失败') }
}

onMounted(refresh)
</script>

<style scoped>
.order-page { display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; flex-direction: column; gap: 6px; }
.page-title { display: inline-flex; align-items: center; gap: 8px; margin: 0; font-size: 22px; }
.order-list { display: flex; flex-direction: column; gap: 14px; }
.order-card { border: 1px solid #ebeef5; }
.card-head {
  display: flex; align-items: center; gap: 12px; padding: 12px 16px;
  background: #f5f7fa; border-radius: 6px; margin-bottom: 12px; flex-wrap: wrap;
}
.order-id { font-weight: 600; color: #303133; }
.order-time { color: #909399; }
.card-head :deep(.el-tag) { margin-left: auto; }
.card-body { display: flex; flex-direction: column; gap: 10px; }
.goods {
  display: flex; gap: 12px; padding: 10px; border: 1px solid #f0f2f5; border-radius: 8px;
}
.goods .cover { width: 80px; height: 80px; object-fit: cover; border-radius: 6px; }
.info { flex: 1; display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500; color: #303133; }
.tags { display: inline-flex; gap: 6px; }
.row-between { display: flex; justify-content: space-between; align-items: center; }
.price { color: var(--el-color-danger); font-weight: 600; }
.summary-box { padding: 8px 12px; display: flex; flex-direction: column; gap: 6px; }
.summary-box .line { color: #606266; }
.total { color: var(--el-color-danger); font-size: 22px; font-weight: 700; margin-left: 4px; }
.actions { margin-top: 6px; display: inline-flex; gap: 8px; }
</style>
