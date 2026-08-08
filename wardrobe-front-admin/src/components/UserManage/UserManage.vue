<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="nameOrPhone" placeholder="请输入用户名或手机号" style="width: 360px" clearable
          @keyup.enter="applyFilter">
          <template #append>
            <el-button :icon="Search" @click="applyFilter" />
          </template>
        </el-input>
        <el-button type="primary" :icon="Plus" @click="addDialog = true">添加用户</el-button>
      </div>

      <el-table
        :data="currentPage"
        border
        stripe
        style="width: 100%; margin-top: 14px"
        height="520"
        empty-text="无数据"
        highlight-current-row
      >
        <el-table-column prop="id" label="编号" width="90" align="center" />
        <el-table-column prop="userName" label="用户名" width="160" align="center" />

        <el-table-column prop="phone" label="电话" width="160" align="center" />
        <el-table-column label="角色" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'danger' : ''" effect="light">
              {{ row.role === 1 ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" circle class="btn-icon-circle" :icon="Edit" @click="openEdit(row)" />
            <el-button type="danger"  circle class="btn-icon-circle" :icon="Delete" @click="remove(row)" />
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
        />
      </div>
    </el-card>

    <AddUser v-if="addDialog" :add-dialog="addDialog" @change-add-dialog="(v) => { addDialog = v; if (!v) load() }" />
    <EditUser v-if="editDialog" :edit-dialog="editDialog" :edit-data="editData!"
      @change-edit-dialog="(v) => { editDialog = v; if (!v) load() }" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { User } from '@/types'
import request from '@/axios'
import AddUser from './AddUser.vue'
import EditUser from './EditUser.vue'

const tableData = ref<User[]>([])
const page = ref(1)
const pageSize = ref(10)
const nameOrPhone = ref('')
const addDialog = ref(false)
const editDialog = ref(false)
const editData = ref<User | null>(null)

const filtered = computed(() => {
  const k = nameOrPhone.value.trim()
  if (!k) return tableData.value
  return tableData.value.filter(u =>
    (u.userName ?? '').includes(k) || (u.phone ?? '').includes(k),
  )
})
const total = computed(() => filtered.value.length)
const currentPage = computed(() => {
  const s = (page.value - 1) * pageSize.value
  return filtered.value.slice(s, s + pageSize.value)
})

function applyFilter() { page.value = 1 }

async function load() {
  const res = await request.get('/user') as User[]
  tableData.value = Array.isArray(res) ? res : []
}

function openEdit(row: User) {
  editData.value = { ...row }
  editDialog.value = true
}
async function remove(row: User) {
  try {
    await ElMessageBox.confirm('确定要注销该用户账号吗？', '提示', { type: 'warning' })
    const res = await request.delete(`/user/${row.id}`) as string
    ElMessage.success(res ?? '删除成功')
    load()
  } catch { /* cancel or error */ }
}

onMounted(load)
// 让 reactive 被使用以避免 TS 提示
void reactive
</script>

<style scoped>
.toolbar { display: inline-flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.pagination { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
