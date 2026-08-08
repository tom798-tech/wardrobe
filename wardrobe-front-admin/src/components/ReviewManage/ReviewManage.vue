<template>
  <div>
    <el-card shadow="never">
      <el-table
        :data="currentData"
        border
        stripe
        style="width: 100%; margin-top: 10px"
        height="520"
        highlight-current-row
        empty-text="暂无评论数据"
      >
        <el-table-column prop="id" label="编号" width="80" align="center" />
        <el-table-column prop="userName" label="用户名" width="140" align="center" />
        <el-table-column prop="clothName" label="商品名称" width="180" align="center" />
        <el-table-column prop="content" label="评论内容" min-width="320" show-overflow-tooltip />
        <el-table-column label="评分" width="160" align="center">
          <template #default="{ row }">
            <el-rate v-model="row.rating" disabled show-score text-color="#ff9900" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评论时间" width="180" align="center" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" :icon="Delete" circle @click="deleteReview(row)" />
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
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Review } from '@/types'
import request from '@/axios'

const tableData = ref<Review[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = computed(() => tableData.value.length)

const currentData = computed<Review[]>(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return tableData.value.slice(start, start + pageSize.value)
})

async function load() {
  try {
    const res = await request.get('/review') as Review[]
    tableData.value = Array.isArray(res) ? res : []
    currentPage.value = 1
  } catch {
    tableData.value = []
  }
}

function deleteReview(row: Review) {
  ElMessageBox.confirm('确定要删除该评论吗？', '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
  }).then(async () => {
    try {
      const res = await request.delete(`/review/${row.id}`) as string
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
.pagination { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
