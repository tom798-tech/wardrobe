<template>
  <div class="detail-page" v-loading="loading">
    <el-row v-if="clothes" :gutter="28">
      <el-col :xs="24" :md="10">
        <div class="gallery">
          <img class="cover" :src="cover" :alt="clothes.clothName" @click="showPreview = true" />
          <div v-if="subImages.length > 0" class="thumb-list">
            <img
              v-for="(src, i) in subImages"
              :key="i"
              :class="['thumb', { active: i === 0 }]"
              :src="resolveImage(src)"
              @click="cover = resolveImage(src)"
            />
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :md="14">
        <h2 class="cloth-title">{{ clothes.clothName }}</h2>
        <div class="tags">
          <el-tag type="success" effect="light">{{ typeName }}</el-tag>
          <el-tag type="warning" effect="light">{{ brandName }}</el-tag>
          <el-tag v-if="clothes.style" effect="light" type="info">{{ clothes.style }}</el-tag>
        </div>
        <div class="price-box">
          <span class="currency">¥</span>
          <span class="price">{{ Number(clothes.price ?? 0).toFixed(2) }}</span>
          <span class="stock">库存：{{ clothes.stock ?? 0 }}</span>
        </div>
        <div class="desc-block" v-if="clothes.description">
          <h4>商品描述</h4>
          <p>{{ clothes.description }}</p>
        </div>
        <div class="actions">
          <div class="size-row">
            <label>尺码</label>
            <el-radio-group v-model="selectedSize">
              <el-radio v-for="sz in sizeOptions" :key="sz" :value="sz">{{ sz }}</el-radio>
            </el-radio-group>
          </div>
          <div class="qty-row">
            <label>数量</label>
            <el-input-number v-model="qty" :min="1" :max="clothes.stock ?? 99" />
          </div>
          <div class="btn-row">
            <el-button type="primary" size="large" :icon="ShoppingCart" :disabled="!canBuy" @click="addToCart">
              加入购物车
            </el-button>
            <el-button size="large" :icon="Coin" type="success" :disabled="!canBuy" @click="buyNow">
              立即购买
            </el-button>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- AI 评论摘要 -->
    <section class="review-summary" v-if="clothes && reviewSummary">
      <h3 class="section-title"><el-icon><Star /></el-icon>
        <span>📊 AI 评论分析</span>
      </h3>
      <div class="summary-content">
        <div class="summary-row">
          <div class="summary-item positive">
            <h4>👍 好评优点</h4>
            <ul>
              <li v-for="(point, i) in reviewSummary.positivePoints" :key="i">{{ point }}</li>
            </ul>
          </div>
          <div class="summary-item negative">
            <h4>👎 差评缺点</h4>
            <ul>
              <li v-for="(point, i) in reviewSummary.negativePoints" :key="i">{{ point }}</li>
            </ul>
          </div>
        </div>
        <div class="summary-summary">
          <h4>📝 综合评价</h4>
          <p>{{ reviewSummary.summary }}</p>
        </div>
      </div>
    </section>

    <section class="reviews" v-if="clothes">
      <h3 class="section-title"><el-icon><ChatDotRound /></el-icon>用户评价 ({{ reviews.length }})</h3>
      <el-empty v-if="!reviews.length" description="暂无评价，快来第一个评价吧！" />
      <div v-else class="review-list">
        <div v-for="r in reviews" :key="r.id" class="review-item">
          <div class="review-header">
            <el-avatar :size="36">{{ resolveInitial(r) }}</el-avatar>
            <div class="review-meta">
              <div class="user-line">
                <strong>{{ resolveUser(r) }}</strong>
                <el-rate disabled :model-value="r.rating ?? 5" size="small" />
              </div>
              <span class="time">{{ r.reviewTime ?? '' }}</span>
            </div>
          </div>
          <p class="review-content">{{ r.content }}</p>
        </div>
      </div>
      <el-divider />
      <div class="review-form">
        <h4>发表评价</h4>
        <el-form :model="reviewForm" label-width="60px" style="max-width: 720px">
          <el-form-item label="评分">
            <el-rate v-model="reviewForm.rating" />
          </el-form-item>
          <el-form-item label="内容">
            <el-input v-model="reviewForm.content" type="textarea" :rows="4" placeholder="分享你的穿搭感受..." />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Edit" :disabled="!canReview" @click="submitReview">提交评价</el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>

    <el-image-viewer
      v-if="showPreview"
      :url-list="[cover]"
      @close="showPreview = false"
    />

    <!-- 相似推荐（向量推荐） -->
    <section class="similar" v-if="clothes && similarList.length">
      <h3 class="section-title"><el-icon><Link /></el-icon>
        <span>👗 相似推荐</span>
        <span class="section-sub">(AI 智能匹配)</span>
      </h3>
      <div class="clothes-grid">
        <router-link
          v-for="c in similarList"
          :key="c.clothId"
          :to="{ name: 'ClothesDetail', params: { id: c.clothId } }"
          class="card card-clothes"
        >
          <img :src="resolveImage(c.image ?? c.images?.split(';')[0])" :alt="c.clothName" class="clothes-cover" />
          <div class="card-body">
            <div class="row-between">
              <h4 class="cloth-name" :title="c.clothName">{{ c.clothName }}</h4>
            </div>
            <div class="meta">
              <el-tag size="small" type="success" effect="light">{{ resolveTypeNameForSimilar(c) }}</el-tag>
              <el-tag size="small" type="warning" effect="light">{{ resolveBrandNameForSimilar(c) }}</el-tag>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Coin, Edit, Link, ShoppingCart, Star } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Brand, Clothes, Review, Size, Type, VectorSearchResult } from '@/types'
import request from '@/axios'
import { useUserStore } from '@/store/userStore'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const id = Number(route.params.id!)
const loading = ref(false)
const clothes = ref<Clothes | null>(null)
const reviews = ref<Review[]>([])
const brandMap = ref<Record<number, Brand>>({})
const typeMap = ref<Record<number, Type>>({})
const cover = ref('')
const subImages = ref<string[]>([])
const selectedSize = ref('')
const sizeOptions = ref<string[]>([])
const qty = ref(1)
const showPreview = ref(false)
const similarList = ref<VectorSearchResult[]>([])
const reviewSummary = ref<{ positivePoints: string[]; negativePoints: string[]; summary: string } | null>(null)

const reviewForm = reactive({ rating: 5, content: '' })

/** 从任意 Size-like 对象中提取尺码字符串（多重兜底，兼容各种字段命名差异） */
function extractSizeName(s: Record<string, unknown> | null | undefined): string {
  if (!s) return ''
  const direct = (s.sizeName ?? s.sizeValue ?? s.name ?? s.label ?? s.value ?? s.title) as string | undefined
  if (direct && typeof direct === 'string' && direct.trim()) return direct.trim()
  // 兜底：遍历对象值，找第一个不是 id/数字/typeId 的字符串
  for (const v of Object.values(s)) {
    if (typeof v === 'string' && v.trim() && !/^\d+$/.test(v.trim())) return v.trim()
  }
  return ''
}

function rebuildSizeOptions() {
  const list = clothes.value?.sizeList
  if (list && Array.isArray(list) && list.length > 0) {
    const mapped = (list as Record<string, unknown>[])
      .map(extractSizeName)
      .filter((s): s is string => !!s)
    sizeOptions.value = mapped.length ? mapped : ['S', 'M', 'L', 'XL']
  } else {
    sizeOptions.value = ['S', 'M', 'L', 'XL']
  }
}

const canBuy = computed(() => selectedSize.value && (clothes.value?.stock ?? 0) > 0)
const canReview = computed(() => !!userStore.user && reviewForm.content.trim().length > 0)
const typeName = computed(() => clothes.value?.typeId ? typeMap.value[clothes.value.typeId]?.typeName ?? '未分类' : '未分类')
const brandName = computed(() => clothes.value?.brandId ? brandMap.value[clothes.value.brandId]?.brandName ?? '无品牌' : '无品牌')

function resolveTypeNameForSimilar(c: VectorSearchResult) {
  return c.typeId ? typeMap.value[c.typeId]?.typeName ?? '未分类' : '未分类'
}
function resolveBrandNameForSimilar(c: VectorSearchResult) {
  return c.brandId ? brandMap.value[c.brandId]?.brandName ?? '无品牌' : '无品牌'
}

function resolveImage(url: string | null | undefined) {
  if (!url) return 'https://placehold.co/400x400/f5f7fa/909399?text=No+Image'
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return `/api/images/${url}`
}
function resolveInitial(r: Review) {
  const name = resolveUser(r)
  return name.charAt(0).toUpperCase() || 'U'
}
function resolveUser(r: Review) {
  return r.userName ?? '匿名用户'
}

async function loadDicts() {
  try {
    const [bs, ts] = await Promise.all([
      request.get('/brand'),
      request.get('/type'),
    ]) as [Brand[], Type[]]
    bs.forEach(b => { brandMap.value[b.id] = b })
    ts.forEach(t => { typeMap.value[t.id] = t })
  } catch { /* ignore */ }
}

async function loadClothes() {
  loading.value = true
  try {
    const c = (await request.get(`/clothes/${id}`)) as Clothes
    clothes.value = c
    rebuildSizeOptions()
    const first = c.image ?? c.images?.split(';')[0] ?? ''
    cover.value = resolveImage(first)
    const others = (c.images?.split(';') ?? []).filter(Boolean)
    if (others.length > 1) subImages.value = others
    if (!c.image && others[0]) subImages.value = others.slice(1)
    // 选默认尺码：优先选后端返回第一个，否则选第一个可选
    if (Array.isArray(c.sizeList) && c.sizeList.length) {
      const firstSz = extractSizeName(c.sizeList[0] as Record<string, unknown>)
      if (firstSz) selectedSize.value = firstSz
      else selectedSize.value = sizeOptions.value[0] ?? ''
    } else {
      selectedSize.value = sizeOptions.value[0] ?? ''
    }
  } catch {
    clothes.value = null
    rebuildSizeOptions()
  } finally {
    loading.value = false
  }
}

async function loadReviews() {
  try {
    const list = (await request.get(`/review/cloth/${id}`)) as Review[]
    reviews.value = list
  } catch {
    reviews.value = []
  }
}

function ensureLogin() {
  if (!userStore.user) {
    ElMessageBox.confirm('需要登录后才能操作，是否前往登录？', '提示', { type: 'warning' })
      .then(() => router.push({ name: 'Login' }))
      .catch(() => { /* cancel */ })
    return false
  }
  return true
}

async function addToCart() {
  if (!ensureLogin() || !clothes.value) return
  try {
    const res = await request.post('/cart', {
      userId: userStore.user!.id,
      clothId: clothes.value.id,
      clothSize: selectedSize.value,
      amount: qty.value,
    }) as string
    if (typeof res === 'string' && !res.includes('成功')) {
      ElMessage.error(res || '添加购物车失败')
      return
    }
    ElMessage.success('添加到购物车成功！')
    userStore.refreshCartCount()
  } catch {
    ElMessage.error('添加购物车失败')
  }
}

async function buyNow() {
  if (!ensureLogin() || !clothes.value) return
  await addToCart()
  router.push({ name: 'Cart' })
}

async function submitReview() {
  if (!ensureLogin() || !clothes.value) return
  try {
    await request.post('/review', {
      clothId: clothes.value.id,
      userId: userStore.user!.id,
      rating: reviewForm.rating,
      content: reviewForm.content.trim(),
    })
    ElMessage.success('评价成功！')
    reviewForm.content = ''
    reviewForm.rating = 5
    loadReviews()
  } catch {
    ElMessage.error('评价失败')
  }
}

async function loadSimilarList() {
  try {
    const res = await request.get(`/vector/recommend/${id}`, { params: { topK: 6 } })
    similarList.value = (Array.isArray(res) ? res : []) as VectorSearchResult[]
  } catch (e) {
    console.error('加载相似推荐失败', e)
    similarList.value = []
  }
}

async function loadReviewSummary() {
  try {
    const res = await request.get(`/ai/comment-summary/${id}`) as { success: boolean; data: { positivePoints: string[]; negativePoints: string[]; summary: string } }
    if (res.success) {
      reviewSummary.value = res.data
    }
  } catch (e) {
    console.error('加载评论摘要失败', e)
    reviewSummary.value = null
  }
}

onMounted(async () => {
  await loadDicts()
  loadClothes()
  loadReviews()
  loadSimilarList()
  loadReviewSummary()
})
</script>

<style scoped>
.detail-page { display: flex; flex-direction: column; gap: 28px; }
.gallery { background: #fff; border: 1px solid #ebeef5; border-radius: 10px; padding: 12px; }
.gallery .cover { width: 100%; height: 440px; object-fit: cover; border-radius: 8px; cursor: zoom-in; }
.thumb-list { display: flex; gap: 8px; margin-top: 10px; flex-wrap: wrap; }
.thumb { width: 72px; height: 72px; object-fit: cover; border-radius: 6px; border: 2px solid transparent; cursor: pointer; }
.thumb.active { border-color: var(--el-color-primary); }

.cloth-title { margin: 0 0 12px; font-size: 24px; color: #303133; }
.tags { display: inline-flex; gap: 8px; }
.price-box {
  margin: 24px 0; padding: 18px 20px; border-radius: 10px;
  background: linear-gradient(90deg, #fef0f0, #fdf6ec); display: flex; align-items: baseline; gap: 14px;
}
.currency { color: var(--el-color-danger); font-size: 20px; }
.price { color: var(--el-color-danger); font-size: 36px; font-weight: 700; }
.stock { margin-left: auto; color: #606266; }
.desc-block { margin-bottom: 18px; }
.desc-block h4 { margin: 0 0 8px; color: #303133; }
.desc-block p { color: #606266; line-height: 1.7; margin: 0; }
.actions { display: flex; flex-direction: column; gap: 14px; }
.size-row, .qty-row, .btn-row { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
.actions label { width: 56px; color: #909399; }

.section-title { display: inline-flex; align-items: center; gap: 8px; margin: 0 0 16px; }
.review-list { display: flex; flex-direction: column; gap: 14px; }
.review-item {
  background: #fff; border: 1px solid #ebeef5; border-radius: 10px; padding: 16px 20px;
}
.review-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.review-meta { flex: 1; }
.user-line { display: flex; align-items: center; gap: 10px; }
.time { color: #909399; font-size: 12px; }
.review-content { margin: 0; color: #303133; line-height: 1.7; }
.review-form h4 { margin: 0 0 12px; }

/* AI 评论摘要模块 */
.review-summary {
  background: #fff; border: 1px solid #ebeef5; border-radius: 10px; padding: 20px;
}
.summary-content {
  display: flex; flex-direction: column; gap: 16px;
}
.summary-row {
  display: grid; grid-template-columns: 1fr 1fr; gap: 16px;
}
.summary-item {
  padding: 14px; border-radius: 8px;
}
.summary-item.positive {
  background: linear-gradient(135deg, #f0fdf4, #dcfce7);
  border: 1px solid #bbf7d0;
}
.summary-item.negative {
  background: linear-gradient(135deg, #fef2f2, #fee2e2);
  border: 1px solid #fecaca;
}
.summary-item h4 {
  margin: 0 0 10px; font-size: 14px; color: #303133;
}
.summary-item ul {
  margin: 0; padding-left: 20px;
}
.summary-item li {
  color: #606266; font-size: 13px; margin-bottom: 4px;
}
.summary-summary {
  padding: 14px; background: #f8fafc; border-radius: 8px;
  border-left: 4px solid var(--el-color-primary);
}
.summary-summary h4 {
  margin: 0 0 8px; font-size: 14px; color: #303133;
}
.summary-summary p {
  margin: 0; color: #606266; font-size: 13px; line-height: 1.7;
}

/* 相似推荐模块 */
.clothes-grid {
  display: grid; gap: 16px;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
}
.card {
  background: #fff; border: 1px solid #ebeef5; border-radius: 8px; overflow: hidden;
  transition: transform .2s ease, box-shadow .2s ease;
}
.card:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(0,0,0,.06); }
.card :deep(img) { display: block; width: 100%; height: 200px; object-fit: cover; }
.card-body { padding: 10px 12px 14px; display: flex; flex-direction: column; gap: 6px; }
.row-between { display: flex; justify-content: space-between; align-items: center; }
.cloth-name {
  margin: 0; font-size: 14px; font-weight: 600; color: #303133;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100%;
}
.meta { display: inline-flex; gap: 6px; flex-wrap: wrap; }
.price-row { margin-top: 4px; }
.price { color: var(--el-color-danger); font-weight: 700; font-size: 16px; }
.sales { color: #909399; font-size: 12px; }
</style>
