<template>
  <div class="cart-page">
    <div class="page-head">
      <h2 class="page-title"><el-icon><ShoppingCartFull /></el-icon>我的购物车</h2>
      <div class="head-actions">
        <el-checkbox v-model="selectAll" :indeterminate="indeterminate" @change="toggleAll">全选</el-checkbox>
        <el-button :icon="Delete" type="danger" plain :disabled="!checkedCount" @click="handleClearChecked">
          删除选中 ({{ checkedCount }})
        </el-button>
        <el-button :icon="Delete" type="danger" plain @click="handleClearAll">清空购物车</el-button>
      </div>
    </div>

    <div v-loading="loading" class="cart-body">
      <el-empty v-if="!list.length" description="购物车空空如也，去首页逛逛吧~">
        <el-button type="primary" @click="$router.push({ name: 'Home' })">去购物</el-button>
      </el-empty>
      <template v-else>
        <div v-for="c in list" :key="c.id" class="cart-item">
          <el-checkbox v-model="c.checked" />
          <router-link :to="{ name: 'ClothesDetail', params: { id: c.clothes?.id ?? c.clothId } }" class="cover-link">
            <img :src="resolveCover(c)" :alt="c.clothes?.clothName ?? '商品'" class="cover" />
          </router-link>
          <div class="info">
            <h4 class="cloth-name">{{ c.clothes?.clothName ?? '商品' }}</h4>
            <div class="tags">
              <el-tag size="small" effect="light">{{ c.clothSize || '默认' }}</el-tag>
            </div>
            <div class="row-between align-center">
              <span class="price">¥ {{ Number(c.clothes?.price ?? 0).toFixed(2) }}</span>
              <el-input-number v-model="c.amount" :min="1" :max="c.clothes?.stock ?? 99" size="small" @change="updateItem(c)" />
              <el-button link type="danger" :icon="Delete" @click="removeItem(c)">删除</el-button>
            </div>
          </div>
        </div>

        <div class="checkout-bar">
          <div class="summary">
            已选 <b class="count">{{ checkedCount }}</b> 件，合计：
            <span class="currency">¥</span>
            <span class="total">{{ totalPrice.toFixed(2) }}</span>
          </div>
          <el-button type="primary" size="large" :icon="Coin" :disabled="!checkedCount" @click="goCheckout">
            去结算
          </el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Coin, Delete, ShoppingCartFull } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Cart, Clothes } from '@/types'
import request from '@/axios'
import { useUserStore } from '@/store/userStore'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

interface CartRow extends Cart { checked: boolean }
const list = ref<CartRow[]>([])

function resolveCover(c: Cart) {
  const c2 = c.clothes as Clothes | undefined
  const url = c2?.image ?? c2?.images?.split(';')[0] ?? ''
  if (!url) return 'https://placehold.co/200x200/f5f7fa/909399?text=No+Image'
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `/api/images/${url}`
}

const selectedList = computed(() => list.value.filter(x => x.checked))
const checkedCount = computed(() => selectedList.value.reduce((s, x) => s + (x.amount ?? 0), 0))
const totalPrice = computed(() =>
  selectedList.value.reduce((s, x) => s + (Number(x.clothes?.price ?? 0) * Number(x.amount ?? 1)), 0),
)
const indeterminate = computed(() => selectedList.value.length > 0 && selectedList.value.length < list.value.length)
const selectAll = computed({
  get: () => list.value.length > 0 && selectedList.value.length === list.value.length,
  set: (v: boolean) => list.value.forEach(x => { x.checked = v }),
})

function toggleAll() { /* noop, setter handles */ }

async function reload() {
  if (!userStore.user) { list.value = []; return }
  loading.value = true
  try {
    const rows = (await request.get('/cart')) as CartRow[]
    list.value = rows.map(r => ({ ...r, checked: true }))
    userStore.cartCount = list.value.reduce((s, x) => s + (x.amount ?? 0), 0)
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}
async function updateItem(c: CartRow) {
  try { await request.put('/cart', c as Cart) }
  catch { ElMessage.error('更新失败') }
}
async function removeItem(c: CartRow) {
  try {
    await request.delete(`/cart/${c.id}`)
    ElMessage.success('删除成功')
    reload()
  } catch {
    ElMessage.error('删除失败')
  }
}
function handleClearChecked() {
  const ids = selectedList.value.map(x => x.id)
  ElMessageBox.confirm(`确定删除选中的 ${ids.length} 项？`, '提示', { type: 'warning' })
    .then(async () => {
      for (const id of ids) try { await request.delete(`/cart/${id}`) } catch { /* ignore */ }
      ElMessage.success('删除成功')
      reload()
    })
    .catch(() => { /* cancel */ })
}
function handleClearAll() {
  if (!userStore.user || !list.value.length) return
  ElMessageBox.confirm('确定清空购物车？', '提示', { type: 'warning' })
    .then(async () => {
      await request.delete('/cart/clear')
      ElMessage.success('已清空')
      reload()
    })
    .catch(() => { /* cancel */ })
}
function goCheckout() {
  if (!selectedList.value.length) return
  const ids = selectedList.value.map(x => x.id)
  router.push({ name: 'Checkout', query: { cartIds: ids.join(',') } })
}

onMounted(reload)
</script>

<style scoped>
.cart-page { display: flex; flex-direction: column; gap: 20px; }
.page-head { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
.page-title { display: inline-flex; align-items: center; gap: 8px; margin: 0; font-size: 22px; }
.head-actions { display: inline-flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.cart-body { display: flex; flex-direction: column; gap: 12px; }
.cart-item {
  background: #fff; border: 1px solid #ebeef5; border-radius: 10px; padding: 14px 18px;
  display: flex; align-items: center; gap: 16px;
}
.cover-link { flex: 0 0 100px; }
.cover { width: 100px; height: 100px; object-fit: cover; border-radius: 8px; }
.info { flex: 1; display: flex; flex-direction: column; gap: 8px; min-width: 0; }
.cloth-name { margin: 0; font-size: 16px; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tags { display: inline-flex; gap: 6px; }
.row-between { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }
.align-center { align-items: center; }
.price { color: var(--el-color-danger); font-weight: 600; font-size: 16px; }

.checkout-bar {
  position: sticky; bottom: 12px;
  display: flex; align-items: center; justify-content: space-between; padding: 14px 22px;
  border-radius: 10px; background: #fff; border: 1px solid #ebeef5; box-shadow: 0 4px 18px rgba(0,0,0,.05);
}
.summary { display: inline-flex; align-items: baseline; gap: 6px; }
.summary .count { color: var(--el-color-primary); font-size: 18px; margin: 0 2px; }
.summary .currency { color: var(--el-color-danger); }
.summary .total { color: var(--el-color-danger); font-weight: 700; font-size: 28px; }
</style>
