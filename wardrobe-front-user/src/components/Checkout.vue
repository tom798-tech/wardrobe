<template>
  <div class="checkout-page">
    <h2 class="page-title"><el-icon><Wallet /></el-icon>确认订单</h2>

    <el-row :gutter="24">
      <el-col :xs="24" :lg="14">
        <section class="panel">
          <h3 class="panel-title"><el-icon><Location /></el-icon>收货地址</h3>
          <el-form :model="form" label-width="72px">
            <el-form-item label="收货人"><el-input v-model="form.consignee" placeholder="请输入收货人姓名" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="请输入11位手机号" maxlength="11" /></el-form-item>
            <el-form-item label="地址">
              <el-input v-model="form.address" type="textarea" :rows="2" placeholder="请输入详细收货地址" />
            </el-form-item>
          </el-form>
        </section>

        <section class="panel">
          <h3 class="panel-title"><el-icon><Goods /></el-icon>商品清单</h3>
          <div v-if="!items.length" class="empty-wrap"><el-empty description="没有可结算的商品" /></div>
          <div v-else class="goods-list">
            <div v-for="g in items" :key="g.id" class="goods-item">
              <img :src="resolveCover(g)" class="cover" />
              <div class="info">
                <h4>{{ g.clothes?.clothName ?? '商品' }}</h4>
                <div class="tags">
                  <el-tag size="small" effect="light">{{ g.clothSize || '默认' }}</el-tag>
                </div>
                <div class="row">
                  <span class="price">¥ {{ Number(g.clothes?.price ?? 0).toFixed(2) }}</span>
                  <span class="qty">x {{ g.amount }}</span>
                </div>
              </div>
            </div>
          </div>
        </section>
      </el-col>

      <el-col :xs="24" :lg="10">
        <section class="panel sticky">
          <h3 class="panel-title"><el-icon><Tickets /></el-icon>订单摘要</h3>
          <div class="summary-row"><span>商品总数</span><b>{{ qtyCount }}</b></div>
          <div class="summary-row"><span>商品金额</span><span>¥ {{ subtotal.toFixed(2) }}</span></div>
          <div class="summary-row"><span>运费</span><span class="free">免邮</span></div>
          <el-divider />
          <div class="summary-row total"><span>应付总额</span><span class="total-price">¥ {{ total.toFixed(2) }}</span></div>
          <div class="summary-row tip"><el-icon color="#e6a23c"><InfoFilled /></el-icon>提交后可在“我的订单”中继续支付</div>
          <el-button
            v-if="submitting"
            type="primary" size="large" style="width: 100%" loading
          >提交中...</el-button>
          <el-button
            v-else
            type="primary" size="large" style="width: 100%"
            :icon="Coin" :disabled="!canSubmit"
            @click="submitOrder"
          >提交订单</el-button>
        </section>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Coin, Goods, InfoFilled, Location, Tickets, Wallet } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { Cart, Clothes } from '@/types'
import request from '@/axios'
import { useUserStore } from '@/store/userStore'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const submitting = ref(false)
const items = ref<Cart[]>([])

const form = reactive({
  consignee: '',
  phone: '',
  address: '',
})

type IdempotentTokenResponse = {
  token?: string
}

const qtyCount = computed(() => items.value.reduce((s, x) => s + (x.amount ?? 0), 0))
const subtotal = computed(() =>
  items.value.reduce((s, x) => s + (Number(x.clothes?.price ?? 0) * Number(x.amount ?? 1)), 0),
)
const total = computed(() => subtotal.value)

const canSubmit = computed(() =>
  !!userStore.user && items.value.length > 0 &&
  !!form.consignee.trim() && /^1\d{10}$/.test(form.phone) && !!form.address.trim(),
)

function resolveCover(g: Cart) {
  const c = g.clothes as Clothes | undefined
  const url = c?.image ?? c?.images?.split(';')[0] ?? ''
  if (!url) return 'https://placehold.co/200x200/f5f7fa/909399?text=No+Image'
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `/api/images/${url}`
}

async function loadItems() {
  if (!userStore.user) { items.value = []; return }
  try {
    const all = (await request.get('/cart')) as Cart[]
    const rawIds = String(route.query.cartIds ?? '').split(',').map(x => Number(x)).filter(Boolean)
    items.value = rawIds.length ? all.filter(x => rawIds.includes(x.id)) : all
  } catch {
    items.value = []
  }
}

async function fetchIdempotentToken() {
  const res = (await request.get('/api/idempotent/token', { params: { timeout: 120 } })) as IdempotentTokenResponse
  if (!res.token) throw new Error('未获取到幂等性 Token')
  return res.token
}

async function submitOrder() {
  if (!canSubmit.value || !userStore.user) return
  submitting.value = true
  try {
    const idempotentToken = await fetchIdempotentToken()
    const itemsText = JSON.stringify(items.value.map(g => ({
      id: g.id,
      clothId: g.clothId,
      clothName: (g.clothes as Clothes | undefined)?.clothName ?? '商品',
      clothSize: g.clothSize,
      price: (g.clothes as Clothes | undefined)?.price ?? 0,
      amount: g.amount,
    })))
    const payload = {
      userId: userStore.user.id,
      clothesDetails: itemsText,
      price: total.value,
      status: 0,
      address: `${form.consignee}  ${form.phone}  ${form.address}`,
      time: new Date().toLocaleString('zh-CN', { hour12: false }),
    }
    await request.post('/order', payload, {
      headers: {
        'X-Idempotent-Token': idempotentToken,
      },
    })
    ElMessage.success('订单提交成功')
    for (const g of items.value) try { await request.delete(`/cart/${g.id}`) } catch { /* ignore */ }
    await userStore.refreshCartCount()
    router.push({ name: 'Order' })
  } catch {
    ElMessage.error('订单提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadItems)
</script>

<style scoped>
.checkout-page { display: flex; flex-direction: column; gap: 16px; }
.page-title { display: inline-flex; align-items: center; gap: 8px; margin: 0 0 4px; font-size: 22px; }
.panel { background: #fff; border: 1px solid #ebeef5; border-radius: 10px; padding: 18px 22px; margin-bottom: 18px; }
.panel-title { display: inline-flex; align-items: center; gap: 8px; margin: 0 0 14px; font-size: 16px; }
.sticky { position: sticky; top: 12px; }

.goods-list { display: flex; flex-direction: column; gap: 12px; }
.goods-item { display: flex; gap: 14px; padding: 10px; border: 1px solid #f0f2f5; border-radius: 8px; }
.goods-item .cover { width: 80px; height: 80px; object-fit: cover; border-radius: 6px; }
.goods-item .info { flex: 1; display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.goods-item h4 { margin: 0; font-size: 15px; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tags { display: inline-flex; gap: 6px; }
.row { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; }
.price { color: var(--el-color-danger); font-weight: 600; }
.qty { color: #606266; }

.summary-row { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; }
.summary-row.total { font-size: 16px; }
.summary-row.tip { color: #909399; font-size: 12px; justify-content: flex-start; gap: 6px; }
.total-price { color: var(--el-color-danger); font-weight: 700; font-size: 24px; }
.free { color: var(--el-color-success); font-weight: 600; }
.empty-wrap { padding: 18px 0; }
</style>
