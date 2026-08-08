<template>
  <div class="login-page">
    <div class="box">
      <h2 class="title"><el-icon><Avatar /></el-icon>欢迎回来</h2>
      <p class="subtitle">登录你的云衣橱账户</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="用户名 / 邮箱 / 手机号" prop="userInfo">
          <el-input v-model="form.userInfo" placeholder="请输入账号" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" size="large" show-password
            @keyup.enter="onSubmit" />
        </el-form-item>
        <!-- 验证码输入框 -->
        <el-form-item v-if="needCaptcha">
          <div class="captcha-row">
            <el-input v-model="captchaCode" size="large" placeholder="请输入验证码" style="flex: 1" />
            <div class="captcha-img-box">
              <img v-if="captchaImage" :src="captchaImage" class="captcha-img" @click="refreshCaptcha" />
              <el-button v-else type="text" @click="refreshCaptcha">获取验证码</el-button>
            </div>
          </div>
        </el-form-item>
        <el-button
          type="primary" size="large" style="width: 100%; margin: 8px 0 12px"
          :icon="Unlock" :loading="loading" @click="onSubmit">登录</el-button>
        <div class="foot-links">
          <el-checkbox v-model="remember" border>记住我</el-checkbox>
          <router-link class="link" :to="{ name: 'Register' }">没有账号？去注册</router-link>
        </div>
      </el-form>
      <el-divider content-position="center">快速入口</el-divider>
      <div class="bottom-row">
        <el-button :icon="Avatar" plain style="flex:1" @click="fillGuest">游客浏览</el-button>
        <el-button :icon="UserIcon" style="flex:1; background:#b3e19d" plain @click="fillAdmin">管理员登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Avatar, Unlock, User as UserIcon } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import type { User } from '@/types'
import request from '@/axios'
import { useUserStore } from '@/store/userStore'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const remember = ref(true)
const form = reactive({ userInfo: '', password: '' })

// 验证码相关
const needCaptcha = ref(false)
const captchaId = ref('')
const captchaCode = ref('')
const captchaImage = ref('')

const rules: FormRules = {
  userInfo: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

// 获取验证码
async function refreshCaptcha() {
  try {
    const res = await request.get('/captcha')
    if (res?.captchaId && res?.image) {
      captchaId.value = res.captchaId
      captchaImage.value = res.image
      captchaCode.value = ''
    } else {
      ElMessage.error('获取验证码失败')
    }
  } catch {
    ElMessage.error('获取验证码失败')
  }
}

async function submit(isAdmin = false) {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      userInfo: form.userInfo.trim(),
      password: form.password,
      isAdminLogin: isAdmin ? 'true' : 'false',
    }
    // 如果需要验证码，添加验证码参数
    if (needCaptcha.value) {
      params.captchaId = captchaId.value
      params.captchaCode = captchaCode.value
    }
    
    const res = await request.post('/login', params)
    // 统一响应格式处理
    if (typeof res === 'string') {
      ElMessage.error(res)
      return
    }
    // 检查是否有 success 字段（新格式）
    if ('success' in res) {
      if (!res.success) {
        ElMessage.error(res.message || '登录失败')
        // 如果需要验证码，刷新验证码
        if (res.needCaptcha) {
          needCaptcha.value = true
          refreshCaptcha()
        }
        return
      }
      // 成功时 data 字段包含用户信息
      const u = res.data as User
      if (!u?.id) {
        ElMessage.error('登录失败：未返回用户信息')
        return
      }
      userStore.login(u, remember.value)
      ElMessage.success('登录成功，欢迎回来！')
      const redirect = String(route.query.redirect ?? '')
      const defaultTarget = isAdmin ? '/admin' : '/user/Home'
      router.replace(redirect || defaultTarget)
      return
    }
    // 旧格式兼容：直接返回用户对象
    const u = res as User
    if (!u.id) {
      ElMessage.error('登录失败：未返回用户信息')
      return
    }
    userStore.login(u, remember.value)
    ElMessage.success('登录成功，欢迎回来！')
    const redirect = String(route.query.redirect ?? '')
    const defaultTarget = isAdmin ? '/admin' : '/user/Home'
    router.replace(redirect || defaultTarget)
  } catch {
    ElMessage.error('登录失败，请检查账号和密码')
  } finally {
    loading.value = false
  }
}
function onSubmit() { submit(false) }
function fillAdmin() { submit(true) }
function fillGuest() { router.replace({ name: 'Home' }) }
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 120px);
  display: flex; align-items: center; justify-content: center;
  padding: 32px 16px;
}
.box {
  width: 100%; max-width: 420px; background: #fff;
  border-radius: 16px; box-shadow: 0 20px 60px rgba(64,158,255,.15);
  padding: 32px 36px; border: 1px solid #ebeef5;
}
.title { display: inline-flex; align-items: center; gap: 8px; margin: 0; font-size: 24px; color: #303133; }
.subtitle { color: #909399; margin: 6px 0 18px; }
.foot-links { display: flex; justify-content: space-between; align-items: center; }
.link { color: var(--el-color-primary); text-decoration: none; }
.link:hover { text-decoration: underline; }
.bottom-row { display: flex; gap: 10px; }

/* 验证码样式 */
.captcha-row {
  display: flex;
  gap: 10px;
  align-items: center;
}
.captcha-img-box {
  width: 120px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.captcha-img {
  width: 120px;
  height: 40px;
  cursor: pointer;
  border-radius: 4px;
}
</style>
