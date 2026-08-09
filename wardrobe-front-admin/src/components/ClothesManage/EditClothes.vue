<template>
  <div>
    <el-dialog v-model="dialogVisible" @close="handleClose" title="修改服装" width="500px">
      <el-form
        :model="formData"
        ref="ruleFormRef"
        :rules="rules"
        status-icon
        style="width: 400px"
        label-width="100px"
      >
        <el-form-item label="服装名称" prop="clothName">
          <el-input v-model="formData.clothName" />
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
          <el-input v-model="formData.style" />
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
        <el-form-item label="服装价格" prop="price">
          <el-input v-model="formData.price" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import type { Clothes, Type as ClothesType } from '@/types'
import request from '@/axios'

const IMG_BASE = '/api/images/'

interface Props {
  editDialog: boolean
  editData: Partial<Clothes> & { typeName?: string | null }
}
const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'ChangeEditDialog', value: boolean): void
}>()

const ruleFormRef = ref<FormInstance>()
const typesData = ref<ClothesType[]>([])
const newFileName = ref('')
const imageUrl = ref('')

const rules: FormRules = {
  clothName: [{ required: true, message: '请输入服装名称', trigger: 'blur' }],
  typeId: [{ required: true, message: '请选择服装类别', trigger: 'change' }],
  style: [{ required: true, message: '请输入服装风格', trigger: 'blur' }],
  price: [
    { required: true, message: '请输入服装价格', trigger: 'blur' },
    { pattern: /^\d+(\.\d+)?$/, message: '请输入合法的数字', trigger: 'blur' },
  ],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
}

const formData = reactive<Partial<Clothes> & { typeId?: number | null }>({
  id: 0,
  clothName: '',
  typeId: null,
  style: '',
  image: '',
  price: undefined,
  stock: 0,
})

const dialogVisible = computed({
  get: () => props.editDialog,
  set: () => {},
})

const imageName = computed(() => newFileName.value || (formData.image ?? ''))

onMounted(async () => {
  try {
    const res = await request.get('/type') as ClothesType[]
    typesData.value = Array.isArray(res) ? res : []
  } catch { /* ignore */ }
})

watch(
  () => props.editData,
  (val) => {
    if (val && props.editDialog) {
      formData.id = val.id ?? 0
      formData.clothName = val.clothName ?? ''
      formData.typeId = val.typeId ?? null
      formData.style = val.style ?? ''
      formData.image = val.image ?? ''
      formData.price = val.price
      formData.stock = val.stock ?? 0
      imageUrl.value = val.image ? IMG_BASE + val.image : ''
      newFileName.value = ''
    }
  },
  { immediate: true, deep: true },
)

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
    newFileName.value = typeof response === 'string' ? response : String(response ?? '')
    ElMessage.success('上传成功')
  }
}

async function submitForm() {
  const ok = await ruleFormRef.value?.validate().catch(() => false)
  if (!ok) return
  const payload = {
    id: formData.id,
    clothName: formData.clothName,
    style: formData.style,
    typeId: formData.typeId,
    image: imageName.value,
    price: formData.price,
    stock: formData.stock ?? 0,
  }
  try {
    const res = await request.put('/clothes', payload) as string
    ElMessage.success(res ?? '修改成功')
    emit('ChangeEditDialog', false)
  } catch { /* ignore */ }
}

function handleClose() {
  ruleFormRef.value?.clearValidate()
  newFileName.value = ''
  emit('ChangeEditDialog', false)
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
