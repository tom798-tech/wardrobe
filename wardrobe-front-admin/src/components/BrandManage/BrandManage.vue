<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="brandName" placeholder="请输入品牌名称搜索" style="width: 260px" clearable>
          <template #append>
            <el-button :icon="Search" @click="searchBrand" />
          </template>
        </el-input>
        <el-button type="primary" @click="openAddDialog">添加品牌</el-button>
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
        <el-table-column prop="brandName" label="品牌名称" width="160" align="center" />
        <el-table-column label="品牌Logo" width="160" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.brandLogo"
              :src="logoUrl(row.brandLogo)"
              :preview-src-list="[logoUrl(row.brandLogo)]"
              fit="contain"
              style="width: 60px; height: 60px"
            />
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="品牌描述" min-width="260" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" :icon="Edit" circle @click="openEditDialog(row)" />
            <el-button type="danger" :icon="Delete" circle @click="deleteBrand(row)" />
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

      <!-- 添加品牌弹窗 -->
      <el-dialog v-model="addDialog" title="添加品牌" width="500px" @close="resetAddForm">
        <el-form :model="addForm" ref="addFormRef" :rules="rules" label-width="100px">
          <el-form-item label="品牌名称" prop="brandName">
            <el-input v-model="addForm.brandName" placeholder="请输入品牌名称" />
          </el-form-item>
          <el-form-item label="品牌Logo" prop="brandLogo">
            <el-input v-model="addForm.brandLogo" placeholder="请输入Logo文件名" />
          </el-form-item>
          <el-form-item label="品牌描述" prop="description">
            <el-input v-model="addForm.description" type="textarea" :rows="3" placeholder="请输入品牌描述" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="addDialog = false">取消</el-button>
          <el-button type="primary" @click="submitAdd">确定</el-button>
        </template>
      </el-dialog>

      <!-- 编辑品牌弹窗 -->
      <el-dialog v-model="editDialog" title="编辑品牌" width="500px" @close="resetEditForm">
        <el-form :model="editForm" ref="editFormRef" :rules="rules" label-width="100px">
          <el-form-item label="品牌名称" prop="brandName">
            <el-input v-model="editForm.brandName" placeholder="请输入品牌名称" />
          </el-form-item>
          <el-form-item label="品牌Logo" prop="brandLogo">
            <el-input v-model="editForm.brandLogo" placeholder="请输入Logo文件名" />
          </el-form-item>
          <el-form-item label="品牌描述" prop="description">
            <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入品牌描述" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editDialog = false">取消</el-button>
          <el-button type="primary" @click="submitEdit">确定</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import type { Brand } from '@/types'
import request from '@/axios'

const IMG_BASE = '/api/images/'

const tableData = ref<Brand[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = computed(() => tableData.value.length)
const brandName = ref('')

const currentData = computed<Brand[]>(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return tableData.value.slice(start, start + pageSize.value)
})

const rules: FormRules = {
  brandName: [{ required: true, message: '请输入品牌名称', trigger: 'blur' }],
}

const addDialog = ref(false)
const addFormRef = ref<FormInstance>()
const addForm = reactive<Partial<Brand>>({ brandName: '', brandLogo: '', description: '' })

const editDialog = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive<Brand>({ id: 0, brandName: '', brandLogo: '', description: '' })

function logoUrl(name?: string | null) {
  return name ? IMG_BASE + name : ''
}

async function load() {
  try {
    const res = await request.get('/brand') as Brand[]
    tableData.value = Array.isArray(res) ? res : []
    currentPage.value = 1
  } catch {
    tableData.value = []
  }
}

function searchBrand() {
  const k = brandName.value.trim()
  if (!k) { load(); return }
  tableData.value = tableData.value.filter(b => (b.brandName ?? '').includes(k))
  currentPage.value = 1
}

function openAddDialog() { addDialog.value = true }
function resetAddForm() {
  Object.assign(addForm, { brandName: '', brandLogo: '', description: '' })
}
async function submitAdd() {
  const ok = await addFormRef.value?.validate().catch(() => false)
  if (!ok) return
  try {
    const res = await request.post('/brand', addForm) as string
    ElMessage.success(res ?? '添加成功')
    addDialog.value = false
    load()
  } catch { /* ignore */ }
}

function openEditDialog(row: Brand) {
  editForm.id = row.id
  editForm.brandName = row.brandName
  editForm.brandLogo = row.brandLogo ?? ''
  editForm.description = row.description ?? ''
  editDialog.value = true
}
function resetEditForm() {
  Object.assign(editForm, { id: 0, brandName: '', brandLogo: '', description: '' })
}
async function submitEdit() {
  const ok = await editFormRef.value?.validate().catch(() => false)
  if (!ok) return
  try {
    const res = await request.put('/brand', {
      id: editForm.id,
      brandName: editForm.brandName,
      brandLogo: editForm.brandLogo,
      description: editForm.description,
    }) as string
    ElMessage.success(res ?? '修改成功')
    editDialog.value = false
    load()
  } catch { /* ignore */ }
}

function deleteBrand(row: Brand) {
  ElMessageBox.confirm('确定要删除该品牌吗？', '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
  }).then(async () => {
    try {
      const res = await request.delete(`/brand/${row.id}`) as string
      ElMessage.success(res ?? '删除成功')
      load()
    } catch { /* ignore */ }
  }).catch(() => {
    ElMessage.info('取消删除')
  })
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: inline-flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.pagination { display: flex; justify-content: flex-end; margin-top: 14px; }
.muted { color: #909399; }
</style>
