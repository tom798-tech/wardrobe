<template>
  <div class="profile-page">
    <el-row :gutter="24">
      <el-col :xs="24" :md="8">
        <el-card class="avatar-card">
          <div class="avatar-wrap">
            <el-avatar :size="96" class="big-avatar">{{ avatarInitial }}</el-avatar>
          </div>
          <div class="basic">
            <h3 class="name">{{ userStore.user?.userName ?? '未登录' }}</h3>
            <div class="role-tag">
              <el-tag :type="userStore.user?.role === 1 ? 'danger' : 'primary'" effect="light">
                {{ userStore.user?.role === 1 ? '管理员' : '普通用户' }}
              </el-tag>
            </div>
          </div>
          <el-descriptions :column="1" size="small" class="info-list">
            <el-descriptions-item label="用户ID">{{ userStore.user?.id ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机">{{ userStore.user?.phone ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货地址">{{ userStore.user?.address ?? '未设置' }}</el-descriptions-item>
          </el-descriptions>
          <div class="actions">
            <el-button type="primary" plain @click="showEdit = true">修改资料</el-button>
            <el-button type="danger" plain @click="logout">退出登录</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="16">
        <el-card>
          <template #header>
            <div class="card-head"><el-icon><Setting /></el-icon>账号与安全</div>
          </template>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="修改资料" name="info">
              <el-form :model="infoForm" label-width="90px" style="max-width: 560px">
                <el-form-item label="用户名">
                  <el-input v-model="infoForm.userName" disabled />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="infoForm.phone" maxlength="11" />
                </el-form-item>
                <el-form-item label="收货地址">
                  <el-input v-model="infoForm.address" type="textarea" :rows="2" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="savingInfo" @click="saveInfo">保存资料</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="pwd">
              <el-form :model="pwdForm" label-width="90px" style="max-width: 560px">
                <el-form-item label="原密码">
                  <el-input v-model="pwdForm.password" type="password" show-password />
                </el-form-item>
                <el-form-item label="新密码">
                  <el-input v-model="pwdForm.newpsw" type="password" show-password />
                </el-form-item>
                <el-form-item label="确认新密码">
                  <el-input v-model="pwdForm.confirm" type="password" show-password />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="savingPwd" @click="savePwd">修改密码</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showEdit" title="修改资料" width="460px">
      <el-form :model="infoForm" label-width="80px">
        <el-form-item label="手机号">
          <el-input v-model="infoForm.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="收货地址">
          <el-input v-model="infoForm.address" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" :loading="savingInfo" @click="saveInfo">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Setting } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { User } from '@/types'
import request from '@/axios'
import { useUserStore } from '@/store/userStore'

const userStore = useUserStore()
const router = useRouter()

const showEdit = ref(false)
const savingInfo = ref(false)
const savingPwd = ref(false)
const activeTab = ref('info')

const infoForm = reactive({
  id: userStore.user?.id,
  userName: userStore.user?.userName ?? '',
  phone: userStore.user?.phone ?? '',
  address: userStore.user?.address ?? '',
  password: '',
  newpsw: '',
})
const pwdForm = reactive({ password: '', newpsw: '', confirm: '' })

const avatarInitial = computed(() =>
  (userStore.user?.userName ?? 'U').charAt(0).toUpperCase(),
)

async function saveInfo() {
  if (!userStore.user) return
  if (infoForm.phone && !/^1\d{10}$/.test(infoForm.phone)) {
    ElMessage.error('请输入 11 位手机号')
    return
  }
  savingInfo.value = true
  try {
    await request.put('/user', { ...infoForm })
    const latest = await request.get(`/user/${userStore.user.id}`) as User
    userStore.user = latest
    userStore.persist()
    ElMessage.success('保存成功')
    showEdit.value = false
  } catch {
    ElMessage.error('保存失败')
  } finally {
    savingInfo.value = false
  }
}
async function savePwd() {
  if (!pwdForm.password || !pwdForm.newpsw) {
    ElMessage.warning('请输入原密码和新密码')
    return
  }
  if (pwdForm.newpsw !== pwdForm.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (pwdForm.newpsw.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  savingPwd.value = true
  try {
    const payload = {
      id: userStore.user?.id,
      password: pwdForm.password,
      newpsw: pwdForm.newpsw,
    }
    await request.put('/user', payload)
    ElMessage.success('密码修改成功，请重新登录')
    logout()
  } catch {
    ElMessage.error('密码修改失败，请检查原密码是否正确')
  } finally {
    savingPwd.value = false
  }
}
function logout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    .then(() => {
      userStore.logout()
      router.replace({ name: 'Login' })
    })
    .catch(() => { /* cancel */ })
}
</script>

<style scoped>
.profile-page { display: flex; flex-direction: column; gap: 18px; }
.avatar-card { text-align: center; }
.avatar-wrap { padding: 12px 0; }
.big-avatar { background: var(--el-color-primary); color: #fff; font-weight: 700; font-size: 32px; }
.basic { padding: 0 0 8px; }
.basic .name { margin: 6px 0; font-size: 18px; color: #303133; }
.role-tag { margin-bottom: 14px; }
.info-list { text-align: left; margin-bottom: 14px; }
.actions { display: inline-flex; gap: 8px; flex-wrap: wrap; }
.card-head { display: inline-flex; align-items: center; gap: 8px; font-weight: 600; }
</style>
