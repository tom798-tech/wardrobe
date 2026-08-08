<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="searchUserName" placeholder="请输入用户姓名" style="width: 220px" clearable />
        <el-select v-model="orderStatus" placeholder="请选择订单状态" style="width: 180px" clearable>
          <el-option v-for="s in statusList" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="applyFilter">搜索</el-button>
        <el-button plain :icon="RefreshRight" @click="reset">重置</el-button>
      </div>

      <el-table
        :data="currentPage"
        border
        stripe
        style="width: 100%; margin-top: 14px"
        height="520"
        highlight-current-row
        empty-text="无数据"
      >
        <el-table-column prop="id" label="编号" width="80" align="center" />
        <el-table-column label="商品详情" min-width="320">
          <template #default="{ row }">
            <div class="goods-col">
              <template v-for="(g, i) in parseGoods(row)" :key="i" class="goods-row">
                <span class="goods-name" :title="g.clothName">{{ g.clothName }}</span>
                <el-tag size="small" effect="plain">{{ g.clothSize || '默认' }}</el-tag>
                <span>¥{{ Number(g.price ?? 0).toFixed(2) }}</span>
                <span>x{{ g.amount }}</span>
              </template>
              <span v-if="!parseGoods(row).length" class="muted">—</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="收货信息" width="220">
          <template #default="{ row }">
            <div class="addr-col">
              <div><b>{{ row.userName ?? '-' }}</b></div>
              <div class="muted">{{ row.phone ?? '-' }}</div>
              <div class="muted small">{{ row.address ?? '-' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userName" label="下单用户" width="120" align="center" />
        <el-table-column prop="time" label="下单时间" width="180" align="center" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" type="success" size="small" @click="delivery(row)">发货</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { Order, OrderStatus } from '@/types'
import request from '@/axios'

interface GoodLine { clothName: string; clothSize?: string; price?: number; amount?: number }

const tableData = ref<Order[]>([])
const page = ref(1)
const pageSize = ref(10)
const searchUserName = ref('')
const orderStatus = ref<number | ''>('')
const statusList = reactive([
  { id: 0, name: '未支付' },
  { id: 1, name: '未发货' },
  { id: 2, name: '已发货' },
  { id: 3, name: '已收货' },
])

const filtered = computed<Order[]>(() => {
  let list = tableData.value
  if (searchUserName.value.trim()) {
    const k = searchUserName.value.trim()
    list = list.filter(o => (o.userName ?? '').includes(k))
  }
  if (orderStatus.value !== '') {
    list = list.filter(o => o.status === orderStatus.value)
  }
  return list
})
const total = computed(() => filtered.value.length)
const currentPage = computed(() => {
  const s = (page.value - 1) * pageSize.value
  return filtered.value.slice(s, s + pageSize.value)
})

function statusText(s?: number) {
  switch (s) {
    case 0: return '未支付'; case 1: return '未发货'
    case 2: return '已发货'; case 3: return '已收货'
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
function parseGoods(o: Order): GoodLine[] {
  try {
    const raw = o.clothesDetails ?? ''
    if (!raw) return []
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
}

async function load() {
  const res = await request.get('/order') as Order[]
  tableData.value = Array.isArray(res) ? res : []
}
function applyFilter() { page.value = 1 }
function reset() {
  searchUserName.value = ''
  orderStatus.value = ''
  page.value = 1
}
async function delivery(row: Order) {
  try {
    const res = await request.put(`/order/delivery/${row.id}`) as string
    ElMessage.success(res ?? '发货成功')
    load()
  } catch { /* ignore */ }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: inline-flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.pagination { display: flex; justify-content: flex-end; margin-top: 14px; }
.goods-col { display: flex; flex-direction: column; gap: 4px; }
.goods-row { display: inline-flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.goods-name { max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.addr-col { display: flex; flex-direction: column; gap: 2px; }
.muted { color: #909399; }
.small { font-size: 12px; line-height: 1.5; }
</style>
