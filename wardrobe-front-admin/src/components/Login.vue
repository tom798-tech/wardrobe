<template>
  <div class="login-page">
    <el-container class="page">
      <el-header class="header">
        <div class="brand">
          <el-icon color="#ffffff" :size="34"><ElementPlus /></el-icon>
          <span class="brand-text">网上衣橱 · 后台管理</span>
        </div>
      </el-header>
      <el-main class="main">
        <el-card class="login-card" shadow="hover">
          <h3 class="card-title">管理员登录</h3>
          <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
            <el-form-item prop="userInfo">
              <el-input v-model="form.userInfo" size="large" placeholder="请输入用户名/手机号" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" size="large" placeholder="请输入密码"
                show-password :prefix-icon="Lock" @keyup.enter="onSubmit" />
            </el-form-item>
            <!-- 验证码输入框 -->
            <el-form-item v-if="needCaptcha" prop="captcha">
              <div class="captcha-row">
                <el-input v-model="captchaCode" size="large" placeholder="请输入验证码" style="flex: 1" />
                <div class="captcha-img-box">
                  <img v-if="captchaImage" :src="captchaImage" class="captcha-img" @click="refreshCaptcha" />
                  <el-button v-else type="text" @click="refreshCaptcha">获取验证码</el-button>
                </div>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="onSubmit">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
          <div class="tips">
            <el-alert type="info" :closable="false" show-icon
              title="提示：管理员账号 role=1，否则提示“您没有权限”。">
            </el-alert>
          </div>
        </el-card>
      </el-main>
      <el-footer class="footer">联系我们：https://www.itcast.cn/</el-footer>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElementPlus, Lock, User } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '@/axios'
import { useAdminStore } from '@/store/adminStore'
import type { AdminUser } from '@/types'

const router = useRouter()
const adminStore = useAdminStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
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
    const res = await request.get('/captcha') as { captchaId?: string; image?: string }
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

async function onSubmit() {
  const ok = await formRef.value?.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      userInfo: form.userInfo.trim(),
      password: form.password,
      isAdminLogin: 'true',
    }
    // 如果需要验证码，添加验证码参数
    if (needCaptcha.value) {
      params.captchaId = captchaId.value
      params.captchaCode = captchaCode.value
    }
    
    const res = await request.post('/login', params) as unknown
    // 统一响应格式处理
    if (typeof res === 'string') {
      ElMessage.error(res)
      return
    }
    // 检查是否有 success 字段（新格式）
    if (res && typeof res === 'object' && 'success' in res) {
      const result = res as { success: boolean; message?: string; needCaptcha?: boolean; data?: AdminUser }
      if (!result.success) {
        ElMessage.error(result.message || '登录失败')
        // 如果需要验证码，刷新验证码
        if (result.needCaptcha) {
          needCaptcha.value = true
          refreshCaptcha()
        }
        return
      }
      // 成功时 data 字段包含用户信息
      const user = result.data
      if (!user?.id) {
        ElMessage.error('登录失败，未返回用户信息')
        return
      }
      if (user.role !== 1) {
        ElMessage.error('您没有管理员权限')
        return
      }
      adminStore.login(user)
      ElMessage.success(`${user.userName} 登录成功`)
      router.replace({ path: '/home' })
      return
    }
    // 旧格式兼容：直接返回用户对象
    const user = res as AdminUser
    if (!user.id) {
      ElMessage.error('登录失败，未返回用户信息')
      return
    }
    if (user.role !== 1) {
      ElMessage.error('您没有管理员权限')
      return
    }
    adminStore.login(user)
    ElMessage.success(`${user.userName} 登录成功`)
    router.replace({ path: '/home' })
  } catch {
    // 已在 axios 拦截器提示
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (adminStore.isLoggedIn) router.replace('/home')
})
</script>

<style scoped>
.login-page, .page { height: 100%; }
.header {
  background: linear-gradient(90deg, #4682B4, #2c5282);
  display: flex; align-items: center;
  border-bottom: 1px solid rgba(255,255,255,.15);
}
.brand { display: inline-flex; align-items: center; gap: 10px; color: #fff; }
.brand-text { font-size: 20px; font-weight: 700; letter-spacing: .5px; }
.main {
  background: linear-gradient(180deg, rgba(70,130,180,.15) 0%, rgba(44,82,130,.05) 100%),
              url('../assets/img/bg.jpg') center/cover no-repeat;
  display: flex; align-items: center; justify-content: center;
}
.login-card {
  width: 420px; padding: 12px 14px 0;
  border-radius: 14px;
  backdrop-filter: blur(6px);
  background: rgba(255,255,255,.95);
}
.card-title { margin: 4px 0 18px; text-align: center; font-size: 20px; color: #303133; }
.tips { margin-top: 12px; }
.footer { background: #fff; color: #909399; text-align: center; line-height: 60px; border-top: 1px solid #ebeef5; }

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
