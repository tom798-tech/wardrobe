<template>
  <div class="register-page">
    <div class="box">
      <h2 class="title"><el-icon><UserFilled /></el-icon>创建新账户</h2>
      <p class="subtitle">加入云衣橱，开启你的时尚旅程</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="form.userName" placeholder="2-20 位字符，唯一标识" size="large" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="用于找回密码，选填" size="large" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="11 位手机号" maxlength="11" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" size="large" show-password
            @keyup.enter="onSubmit" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="form.confirm" type="password" size="large" show-password
            @keyup.enter="onSubmit" />
        </el-form-item>
        <el-button
          type="primary" size="large" style="width: 100%; margin: 8px 0 12px"
          :icon="Promotion" :loading="loading" @click="onSubmit">立即注册</el-button>
        <div class="foot-links">
          <el-checkbox v-model="agree">我已阅读并同意《用户协议》</el-checkbox>
          <router-link class="link" :to="{ name: 'Login' }">已有账号？去登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Promotion, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '@/axios'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const agree = ref(false)
const form = reactive({ userName: '', email: '', phone: '', password: '', confirm: '' })

const rules: FormRules = {
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '2-20 位字符', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '至少 6 位密码', trigger: 'blur' },
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_r, v, cb) => {
        if (v !== form.password) cb(new Error('两次输入密码不一致'))
        else cb()
      },
      trigger: 'blur',
    },
  ],
}

async function onSubmit() {
  if (!agree.value) {
    ElMessage.warning('请先同意《用户协议》')
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const payload = {
      userName: form.userName.trim(),
      email: form.email.trim() || null,
      phone: form.phone.trim(),
      password: form.password,
    }
    const res = await request.post('/register', payload)
    const text = String(res ?? '')
    if (!text.includes('成功')) {
      ElMessage.error(text || '注册失败')
      return
    }
    ElMessage.success('注册成功，请登录！')
    router.replace({ name: 'Login' })
  } catch {
    ElMessage.error('注册失败，请稍后再试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: calc(100vh - 120px);
  display: flex; align-items: center; justify-content: center;
  padding: 32px 16px;
}
.box {
  width: 100%; max-width: 460px; background: #fff;
  border-radius: 16px; box-shadow: 0 20px 60px rgba(64,158,255,.15);
  padding: 32px 36px; border: 1px solid #ebeef5;
}
.title { display: inline-flex; align-items: center; gap: 8px; margin: 0; font-size: 24px; color: #303133; }
.subtitle { color: #909399; margin: 6px 0 18px; }
.foot-links { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.link { color: var(--el-color-primary); text-decoration: none; }
.link:hover { text-decoration: underline; }
</style>
