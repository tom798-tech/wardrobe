<template>
  <div class="review-card">
    <div class="head">
      <el-avatar :size="40">{{ initial }}</el-avatar>
      <div class="meta">
        <div class="row">
          <strong>{{ review.userName ?? userNick }}</strong>
          <el-rate disabled :model-value="rating" size="small" />
        </div>
        <span class="time">{{ review.reviewTime ?? '' }}</span>
      </div>
    </div>
    <p class="content">{{ review.content }}</p>
    <el-row v-if="images.length" :gutter="8" class="images">
      <el-col v-for="(src, i) in images" :key="i" :span="4">
        <el-image class="thumb" :src="src" fit="cover" :preview-src-list="images" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Review } from '@/types'

const props = defineProps<{
  review: Review
  userNick?: string
}>()

const initial = computed(() =>
  (props.review.userName ?? props.userNick ?? 'U').charAt(0).toUpperCase(),
)
const rating = computed(() => Number(props.review.rating ?? 5))
const images = computed<string[]>(() => {
  const raw = props.review.images
  if (!raw) return []
  return raw.split(';').filter(Boolean)
})
</script>

<style scoped>
.review-card { background: #fff; border: 1px solid #ebeef5; border-radius: 10px; padding: 14px 16px; }
.head { display: flex; gap: 12px; align-items: center; margin-bottom: 10px; }
.meta { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.row { display: inline-flex; align-items: center; gap: 10px; }
.time { color: #909399; font-size: 12px; }
.content { margin: 0; color: #303133; line-height: 1.7; white-space: pre-wrap; }
.images { margin-top: 10px; }
.thumb { width: 100%; aspect-ratio: 1; border-radius: 6px; }
</style>
