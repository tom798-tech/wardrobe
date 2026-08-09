<template>
  <div>
    <el-dialog v-model="dialogVisible" @close="handleClose" title="上架服装" width="500px">
      <el-form
        :model="formData"
        ref="ruleFormRef"
        :rules="rules"
        status-icon
        style="width: 400px"
        label-width="100px"
      >
        <el-form-item label="服装名称" prop="clothName">
          <el-input v-model="formData.clothName" placeholder="请输入服装名称" />
        </el-form-item>
        <el-form-item label="服装类别" prop="typeId">
          <el-select v-model="formData.typeId" placeholder="请选择服装类别" style="width: 100%">
            <el-option
              v-for="t in typesData"
              :key="t.id"
              :label="t.typeName"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="服装风格" prop="style">
          <el-input v-model="formData.style" placeholder="请输入服装风格" />
        </el-form-item>
        <el-form-item label="服装品牌" prop="brand">
          <el-input v-model="formData.brand" placeholder="请输入服装品牌" />
        </el-form-item>
        <el-form-item label="图片">
          <el-upload
            class="avatar-uploader"
            action="/api/upload"
            :show-file-list="false"
            :on-success="handleImgSuccess"
            :before-upload="beforeImgUpload"
            name="clothesImage"
          >
            <img v-if="imageUrl" :src="imageUrl" class="avatar" alt="" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入商品描述"
          />
          <el-button
            type="primary"
            size="small"
            style="margin-top: 8px"
            :loading="generating"
            @click="generateDescription"
          >
            AI 生成描述
          </el-button>
        </el-form-item>
        <el-form-item label="服装价格" prop="price">
          <el-input v-model="formData.price" placeholder="请输入价格" />
        </el-form-item>
        <el-form-item label="初始库存" prop="stock">
          <el-input-number v-model="formData.stock" :min="0" :precision="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleClose">取消</el-button>
          <el-button type="primary" @click="submitForm">提交</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import type { Clothes, Type as ClothesType } from '@/types'
import request from '@/axios'

interface Props {
  addDialog: boolean
}
const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'ChangeAddDialog', value: boolean): void
}>()

const ruleFormRef = ref<FormInstance>()
const typesData = ref<ClothesType[]>([])
const imageUrl = ref('')
const fileName = ref('')

const rules: FormRules = {
  clothName: [{ required: true, message: '请输入服装名称', trigger: 'blur' }],
  typeId: [{ required: true, message: '请选择服装类别', trigger: 'change' }],
  style: [{ required: true, message: '请输入服装风格', trigger: 'blur' }],
  price: [
    { required: true, message: '请输入服装价格', trigger: 'blur' },
    { pattern: /^\d+(\.\d+)?$/, message: '请输入合法的数字', trigger: 'blur' },
  ],
  stock: [{ required: true, message: '请输入初始库存', trigger: 'blur' }],
}

const generating = ref(false)

const formData = reactive<Partial<Clothes> & { typeId?: number | null; brand?: string; description?: string }>({
  clothName: '',
  typeId: null,
  style: '',
  brand: '',
  description: '',
  price: undefined,
  stock: 0,
})

const dialogVisible = computed({
  get: () => props.addDialog,
  set: () => {},
})

onMounted(async () => {
  try {
    const res = await request.get('/type') as ClothesType[]
    typesData.value = Array.isArray(res) ? res : []
  } catch { /* ignore */ }
})

function beforeImgUpload(file: UploadFile) {
  const accept = ['image/jpeg', 'image/jpg', 'image/png']
  const raw = file.raw ?? (file as unknown as File)
  if (!accept.includes(raw.type)) {
    ElMessage.warning('上传图片只能是 JPG/PNG 格式！')
    return false
  }
  return true
}

function handleImgSuccess(response: unknown, uploadFile: UploadFile) {
  const raw = uploadFile.raw
  if (raw) {
    imageUrl.value = URL.createObjectURL(raw)
    fileName.value = typeof response === 'string' ? response : String(response ?? '')
    ElMessage.success('上传成功')
  }
}

async function generateDescription() {
  if (!formData.clothName) {
    ElMessage.warning('请先输入商品名称')
    return
  }
  generating.value = true
  try {
    const typeName = typesData.value.find(t => t.id === formData.typeId)?.typeName || ''
    const res = await request.post('/ai/generate-description', {
      clothName: formData.clothName,
      typeName,
      style: formData.style,
      brand: formData.brand,
    }) as { success: boolean; description: string }
    if (res.success) {
      formData.description = res.description
    } else {
      ElMessage.error('生成失败，请重试')
    }
  } catch {
    ElMessage.error('生成失败，请重试')
  } finally {
    generating.value = false
  }
}

async function submitForm() {
  const ok = await ruleFormRef.value?.validate().catch(() => false)
  if (!ok) return
  const payload = {
    clothName: formData.clothName,
    style: formData.style,
    typeId: formData.typeId,
    image: fileName.value,
    price: formData.price,
    stock: formData.stock ?? 0,
    brand: formData.brand,
    description: formData.description,
  }
  try {
    const res = await request.post('/clothes', payload) as string
    ElMessage.success(res ?? '上架成功')
    emit('ChangeAddDialog', false)
  } catch { /* ignore */ }
}

function handleClose() {
  // 重置状态
  Object.assign(formData, { clothName: '', typeId: null, style: '', brand: '', description: '', price: undefined, stock: 0 })
  imageUrl.value = ''
  fileName.value = ''
  ruleFormRef.value?.clearValidate()
  emit('ChangeAddDialog', false)
}
</script>

<style scoped>
.avatar-uploader .avatar {
  width: 178px;
  height: 178px;
  display: block;
  border-radius: 6px;
}
.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}
.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}
.el-icon.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  text-align: center;
  line-height: 178px;
}
</style>
