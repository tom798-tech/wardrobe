<template>
  <el-dialog v-model="visible" @close="close" title="编辑用户" width="460px">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="84px">
      <el-form-item label="用户名">
        <el-input v-model="form.userName" disabled />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" show-password />
      </el-form-item>
      <el-form-item label="电话" prop="phone">
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
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import type { User } from '@/types'
import request from '@/axios'

const props = defineProps<{ editDialog: boolean; editData: User }>()
const emit = defineEmits<{ (e: 'change-edit-dialog', v: boolean): void }>()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  id: 0, userName: '', password: '', phone: '', address: '', role: 0,
})

watch(() => props.editData, (v) => {
  Object.assign(form, {
    id: v?.id ?? 0,
    userName: v?.userName ?? '',
    password: v?.password ?? '',
    phone: v?.phone ?? '',
    address: v?.address ?? '',
    role: Number(v?.role ?? 0),
  })
}, { immediate: true, deep: true })

const visible = computed({
  get: () => props.editDialog,
  set: (v) => emit('change-edit-dialog', v),
})

const rules: FormRules = {
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' },
  ],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
}

function close() { emit('change-edit-dialog', false) }

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await request.put('/user/updateByAdmin', {
      id: form.id,
      userName: form.userName,
      password: form.password,
      phone: form.phone,
      address: form.address,
      role: form.role,
    }) as string
    ElMessage.success(res ?? '修改成功')
    close()
  } catch { /* ignore */ }
  finally { loading.value = false }
}
</script>
