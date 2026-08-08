<template>
  <div class="home-page">
    <section class="hero">
      <h1 class="hero-title">精选服饰 · 穿搭灵感 · 一站式衣橱</h1>
      <p class="hero-desc">探索最新潮牌与精致单品，按分类 / 品牌 / 关键词精准筛选，打造属于你的云衣橱</p>
      <div class="hero-actions">
        <el-input
          v-model="searchKeyword"
          class="search-input"
          placeholder="搜索服饰名称..."
          clearable
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
      </div>
    </section>

    <section class="filters">
      <h3 class="section-title"><el-icon><Filter /></el-icon>筛选条件</h3>
      <div class="filter-row">
        <el-select v-model="typeId" placeholder="分类" class="filter-item" clearable @change="handleTypeFilter">
          <el-option v-for="t in typeList" :key="t.id" :label="t.typeName" :value="t.id" />
        </el-select>
        <el-select v-model="brandId" placeholder="品牌" class="filter-item" clearable @change="handleBrandFilter">
          <el-option v-for="b in brandList" :key="b.id" :label="b.brandName" :value="b.id" />
        </el-select>
        <el-button :icon="RefreshRight" @click="resetQuery">重置</el-button>
      </div>
    </section>

    <section class="recommend">
      <h3 class="section-title"><el-icon><Goods /></el-icon>
        <span>{{ sectionTitle }}</span>
        <span v-if="total > 0" class="section-sub">(共 {{ total }} 件)</span>
      </h3>
      <div v-loading="loading" class="clothes-grid">
        <template v-if="pagedList.length">
          <router-link
            v-for="c in pagedList"
            :key="c.id"
            :to="{ name: 'ClothesDetail', params: { id: c.id } }"
            class="card card-clothes"
          >
            <img :src="resolveCover(c)" :alt="c.clothName" class="clothes-cover" />
            <div class="card-body">
              <div class="row-between">
                <h4 class="cloth-name" :title="c.clothName">{{ c.clothName }}</h4>
              </div>
              <div class="meta">
                <el-tag size="small" type="success" effect="light">{{ resolveTypeName(c) }}</el-tag>
                <el-tag size="small" type="warning" effect="light">{{ resolveBrandName(c) }}</el-tag>
                <el-tag v-if="c.style" size="small" effect="light" type="info">{{ c.style }}</el-tag>
              </div>
              <div class="row-between price-row">
                <span class="price">¥ {{ Number(c.price ?? 0).toFixed(2) }}</span>
                <span class="sales">库存 {{ c.stock ?? 0 }}</span>
              </div>
            </div>
          </router-link>
        </template>
        <el-empty v-else description="暂无商品" />
      </div>
    </section>

    <section v-if="total > pageSize" class="pagination-bar">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[8, 16, 24, 40]"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </section>

    <!-- 猜你喜欢（向量推荐） -->
    <section class="recommend" v-if="recommendList.length">
      <h3 class="section-title"><el-icon><MagicStick /></el-icon>
        <span>💡 猜你喜欢</span>
        <span class="section-sub">(AI 智能推荐)</span>
      </h3>
      <div class="clothes-grid">
        <router-link
          v-for="c in recommendList"
          :key="c.clothId"
          :to="{ name: 'ClothesDetail', params: { id: c.clothId } }"
          class="card card-clothes"
        >
          <img :src="resolveCoverForVector(c)" :alt="c.clothName" class="clothes-cover" />
          <div class="card-body">
            <div class="row-between">
              <h4 class="cloth-name" :title="c.clothName">{{ c.clothName }}</h4>
            </div>
            <div class="meta">
              <el-tag size="small" type="success" effect="light">{{ resolveTypeNameForVector(c) }}</el-tag>
              <el-tag size="small" type="warning" effect="light">{{ resolveBrandNameForVector(c) }}</el-tag>
              <el-tag v-if="c.style" size="small" effect="light" type="info">{{ c.style }}</el-tag>
            </div>
            <div class="row-between price-row">
              <span class="price">¥ {{ Number(c.price ?? 0).toFixed(2) }}</span>
              <span class="sales">库存 {{ c.stock ?? 0 }}</span>
            </div>
          </div>
        </router-link>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Filter, Goods, RefreshRight, Search, MagicStick } from '@element-plus/icons-vue'
import type { Brand, Clothes, Type, VectorSearchResult } from '@/types'
import request from '@/axios'

const loading = ref(false)
const searchKeyword = ref('')
const typeId = ref<number | ''>('')
const brandId = ref<number | ''>('')
const page = ref(1)
const pageSize = ref(12)
const rawList = ref<Clothes[]>([])
const typeList = ref<Type[]>([])
const brandList = ref<Brand[]>([])
const recommendList = ref<VectorSearchResult[]>([])

const total = computed(() => rawList.value.length)
const sectionTitle = computed(() => {
  if (searchKeyword.value) return `“${searchKeyword.value}” 搜索结果`
  if (typeId.value !== '') return '按分类筛选'
  if (brandId.value !== '') return '按品牌筛选'
  return '精选推荐'
})
const pagedList = computed<Clothes[]>(() => {
  const start = (page.value - 1) * pageSize.value
  return rawList.value.slice(start, start + pageSize.value)
})

function resolveCover(c: Clothes) {
  const url = c.image ?? c.images?.split(';')[0] ?? ''
  if (!url) return 'https://placehold.co/400x400/f5f7fa/909399?text=No+Image'
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `/api/images/${url}`
}
function resolveTypeName(c: Clothes) {
  return typeList.value.find(t => t.id === c.typeId)?.typeName ?? '未分类'
}
function resolveBrandName(c: Clothes) {
  return brandList.value.find(b => b.id === c.brandId)?.brandName ?? '无品牌'
}

// 向量检索结果解析函数
function resolveCoverForVector(c: VectorSearchResult) {
  const url = c.image ?? c.images?.split(';')[0] ?? ''
  if (!url) return 'https://placehold.co/400x400/f5f7fa/909399?text=No+Image'
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `/api/images/${url}`
}
function resolveTypeNameForVector(c: VectorSearchResult) {
  return typeList.value.find(t => t.id === c.typeId)?.typeName ?? '未分类'
}
function resolveBrandNameForVector(c: VectorSearchResult) {
  return brandList.value.find(b => b.id === c.brandId)?.brandName ?? '无品牌'
}

async function loadDicts() {
  try {
    const [t, b] = await Promise.all([request.get('/type'), request.get('/brand')])
    typeList.value = (Array.isArray(t) ? t : []) as Type[]
    brandList.value = (Array.isArray(b) ? b : []) as Brand[]
  } catch (e) {
    console.error('加载字典失败', e)
  }
}

async function resetToAll() {
  loading.value = true
  try {
    const res = await request.get('/clothes')
    rawList.value = (Array.isArray(res) ? res : []) as Clothes[]
  } catch {
    rawList.value = []
  } finally {
    loading.value = false
  }
}
function handleTypeFilter() {
  page.value = 1
  if (typeId.value === '') return resetToAll()
  loading.value = true
  request.get(`/clothes/type/${typeId.value}`)
    .then(res => { rawList.value = (Array.isArray(res) ? res : []) as Clothes[] })
    .catch(() => { rawList.value = [] })
    .finally(() => { loading.value = false })
}
function handleBrandFilter() {
  page.value = 1
  if (brandId.value === '') return resetToAll()
  loading.value = true
  request.get(`/clothes/brand/${brandId.value}`)
    .then(res => { rawList.value = (Array.isArray(res) ? res : []) as Clothes[] })
    .catch(() => { rawList.value = [] })
    .finally(() => { loading.value = false })
}
function handleSearch() {
  page.value = 1
  const k = searchKeyword.value.trim()
  if (!k) return resetToAll()
  loading.value = true
  request.get('/clothes/search', { params: { keyword: k } })
    .then(res => { rawList.value = (Array.isArray(res) ? res : []) as Clothes[] })
    .catch(() => { rawList.value = [] })
    .finally(() => { loading.value = false })
}
function resetQuery() {
  searchKeyword.value = ''
  typeId.value = ''
  brandId.value = ''
  page.value = 1
  resetToAll()
}

async function loadRecommendList() {
  try {
    // 使用随机热门词作为推荐查询（实际生产中可基于用户行为）
    const hotKeywords = ['时尚潮流', '休闲舒适', '夏季清凉', '简约风格']
    const randomKeyword = hotKeywords[Math.floor(Math.random() * hotKeywords.length)]
    const res = await request.get('/vector/search', { params: { query: randomKeyword, topK: 6 } })
    recommendList.value = (Array.isArray(res) ? res : []) as VectorSearchResult[]
  } catch (e) {
    console.error('加载推荐列表失败', e)
    recommendList.value = []
  }
}

onMounted(async () => {
  await loadDicts()
  resetToAll()
  loadRecommendList()
})
</script>

<style scoped>
.home-page { display: flex; flex-direction: column; gap: 28px; }
.hero {
  background: linear-gradient(135deg, #409eff 0%, #a0cfff 100%);
  color: #fff; padding: 48px 36px; border-radius: 12px;
}
.hero-title { font-size: 28px; margin: 0 0 8px; }
.hero-desc { margin: 0 0 24px; opacity: .9; }
.hero-actions { display: inline-flex; gap: 12px; align-items: center; width: 100%; max-width: 640px; }
.search-input { flex: 1; }

.section-title {
  display: inline-flex; align-items: center; gap: 8px;
  font-size: 18px; margin: 0 0 16px; color: #303133;
}
.section-sub { color: #909399; font-size: 14px; font-weight: 400; }
.filter-row { display: inline-flex; gap: 12px; flex-wrap: wrap; align-items: center; }
.filter-item { width: 180px; }

.clothes-grid {
  display: grid; gap: 16px;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
}
.card {
  background: #fff; border: 1px solid #ebeef5; border-radius: 8px; overflow: hidden;
  transition: transform .2s ease, box-shadow .2s ease;
}
.card:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(0,0,0,.06); }
.card :deep(img) { display: block; width: 100%; height: 240px; object-fit: cover; }
.card-body { padding: 12px 14px 16px; display: flex; flex-direction: column; gap: 8px; }
.row-between { display: flex; justify-content: space-between; align-items: center; }
.cloth-name {
  margin: 0; font-size: 15px; font-weight: 600; color: #303133;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100%;
}
.meta { display: inline-flex; gap: 6px; flex-wrap: wrap; }
.price-row { margin-top: 4px; }
.price { color: var(--el-color-danger); font-weight: 700; font-size: 18px; }
.sales { color: #909399; font-size: 12px; }

.pagination-bar { display: flex; justify-content: center; }
</style>
