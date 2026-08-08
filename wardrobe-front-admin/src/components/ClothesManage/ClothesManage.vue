<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="searchClothesName" placeholder="请输入服装名称" style="width: 200px" clearable />
        <el-select v-model="searchType" placeholder="请选择服装类别" clearable style="width: 180px">
          <el-option
            v-for="t in typesData"
            :key="t.id"
            :label="t.typeName"
            :value="t.id"
          />
        </el-select>
        <el-select v-model="searchStyle" placeholder="请选择服装风格" clearable style="width: 180px">
          <el-option
            v-for="(s, idx) in styles"
            :key="idx"
            :label="s"
            :value="s"
          />
        </el-select>
        <el-button type="primary" :icon="Search" @click="applyFilter">搜索</el-button>
        <el-button plain @click="reset">重置</el-button>
        <el-button type="success" plain style="margin-left: auto" @click="openAdd">
          <el-icon style="margin-right: 4px"><Upload /></el-icon>
          上架服装
        </el-button>
      </div>

      <el-table
        :data="currentData"
        border
        stripe
        style="width: 100%; margin-top: 14px"
        height="520"
        highlight-current-row
        empty-text="无数据"
      >
        <el-table-column prop="id" label="编号" width="80" align="center" />
        <el-table-column prop="clothName" label="服装名称" width="160" align="center" show-overflow-tooltip />
        <el-table-column label="图片" width="120" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.image"
              :src="IMG_BASE + row.image"
              :preview-src-list="[IMG_BASE + row.image]"
              fit="cover"
              style="width: 60px; height: 72px; border-radius: 4px"
            />
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="尺码信息" width="220" align="center">
          <template #default="{ row }">
            <div class="size-tags">
              <el-tag
                v-for="s in (row.sizeList ?? [])"
                :key="s.id"
                size="small"
                effect="plain"
              >
                {{ s.sizeName }}
              </el-tag>
              <span v-if="!row.sizeList?.length" class="muted">—</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类别" width="120" align="center">
          <template #default="{ row }">
            {{ typeNameOf(row.typeId) }}
          </template>
        </el-table-column>
        <el-table-column prop="style" label="风格" width="120" align="center" />
        <el-table-column label="价格" width="120" align="center">
          <template #default="{ row }">¥ {{ Number(row.price ?? 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" :icon="Edit" circle @click="openEdit(row)" />
            <el-button type="danger" :icon="Delete" circle @click="removeClothes(row)" />
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
        />
      </div>

      <EditClothes
        v-if="editVisible"
        :edit-dialog="editVisible"
        :edit-data="editData"
        @change-edit-dialog="closeEdit"
      />
      <AddClothes
        v-if="addVisible"
        :add-dialog="addVisible"
        @change-add-dialog="closeAdd"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Search, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Clothes, Type as ClothesType } from '@/types'
import request from '@/axios'
import EditClothes from './EditClothes.vue'
import AddClothes from './AddClothes.vue'

const IMG_BASE = '/api/images/'

interface ClothesRow extends Clothes {
  typeName?: string | null
}

const rawList = ref<ClothesRow[]>([])
const filtered = ref<ClothesRow[]>([])
const typesData = ref<ClothesType[]>([])
const styles = ref<string[]>([])
const currentPage = ref(1)
const pageSize = ref(5)

const searchClothesName = ref('')
const searchType = ref<number | ''>('')
const searchStyle = ref('')

const total = computed(() => filtered.value.length)
const currentData = computed<ClothesRow[]>(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})

function typeNameOf(typeId?: number | null) {
  if (typeId == null) return '-'
  return typesData.value.find(t => t.id === typeId)?.typeName ?? '-'
}

async function loadAll() {
  try {
    const [clothes, types] = (await Promise.all([
      request.get('/clothes'),
      request.get('/type'),
    ])) as unknown as [ClothesRow[], ClothesType[]]
    rawList.value = Array.isArray(clothes) ? clothes : []
    typesData.value = Array.isArray(types) ? types : []
    const styleSet = new Set<string>()
    rawList.value.forEach(c => { if (c.style) styleSet.add(c.style) })
    styles.value = Array.from(styleSet)
    applyFilter()
  } catch {
    rawList.value = []
    filtered.value = []
  }
}

function applyFilter() {
  let list = rawList.value
  const kw = searchClothesName.value.trim()
  if (kw) list = list.filter(c => (c.clothName ?? '').includes(kw))
  if (searchStyle.value) list = list.filter(c => c.style === searchStyle.value)
  if (searchType.value !== '') list = list.filter(c => c.typeId === searchType.value)
  filtered.value = list
  currentPage.value = 1
}

function reset() {
  searchClothesName.value = ''
  searchType.value = ''
  searchStyle.value = ''
  applyFilter()
}

const addVisible = ref(false)
function openAdd() { addVisible.value = true }
function closeAdd(val: boolean) {
  addVisible.value = val
  if (!val) loadAll()
}

const editVisible = ref(false)
const editData = ref<ClothesRow>({} as ClothesRow)
function openEdit(row: ClothesRow) {
  editData.value = row
  editVisible.value = true
}
function closeEdit(val: boolean) {
  editVisible.value = val
  if (!val) loadAll()
}

function removeClothes(row: ClothesRow) {
  ElMessageBox.confirm('确定要下架该服装吗？', '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
  }).then(async () => {
    try {
      const res = await request.delete(`/clothes/${row.id}`) as string
      ElMessage.success(res ?? '下架成功')
      loadAll()
    } catch { /* ignore */ }
  }).catch(() => {
    ElMessage.info('取消操作')
  })
}

onMounted(loadAll)
</script>

<style scoped>
.toolbar { display: inline-flex; align-items: center; gap: 10px; flex-wrap: wrap; width: 100%; }
.pagination { display: flex; justify-content: flex-end; margin-top: 14px; }
.size-tags { display: inline-flex; flex-wrap: wrap; gap: 4px; justify-content: center; }
.muted { color: #909399; }
</style>
