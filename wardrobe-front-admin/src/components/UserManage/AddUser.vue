<template>
  <el-dialog v-model="visible" @close="close" title="添加用户" width="460px">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="84px" :size="'default'">
      <el-form-item label="用户名" prop="userName">
        <el-input v-model="form.userName" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" show-password />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" maxlength="11" />
      </el-form-item>
      <el-form-item label="地址" prop="address">
        <el-input v-model="form.address" />
      </el-form-item>
      <el-form-item label="角色">
        <el-radio-group v-model="form.role">
          <el-radio :value="0">普通用户</el-radio>
          <el-radio :value="1">管理员</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '@/axios'

const props = defineProps<{ addDialog: boolean }>()
const emit = defineEmits<{ (e: 'change-add-dialog', v: boolean): void }>()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ userName: '', password: '', phone: '', address: '', role: 0 })

const visible = computed({
  get: () => props.addDialog,
  set: (v) => emit('change-add-dialog', v),
})

const rules: FormRules = {
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '2-20 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 16, message: '6-16 个字符', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' },
  ],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
}

function close() {
  emit('change-add-dialog', false)
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const payload = {
      userName: form.userName.trim(),
      password: form.password,
      phone: form.phone.trim(),
      address: form.address.trim(),
      role: form.role,
    }
    const res = await request.post('/user', payload) as string
    ElMessage.success(res ?? '添加成功')
    close()
  } catch { /* ignore */ }
  finally { loading.value = false }
}
</script>
