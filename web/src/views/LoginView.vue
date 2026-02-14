<template>
  <div class="login-scene">
    <div class="orb orb-a" />
    <div class="orb orb-b" />
    <div class="orb orb-c" />
    <div class="grid-overlay" />

    <div class="login-shell">
      <section class="visual-panel">
        <p class="visual-kicker">Logistics Pro Console</p>
        <h1>快递物流管理系统</h1>
        <p class="visual-desc">
          统一调度下单、轨迹、派送与签收流程，实时掌握物流状态，让业务流转更高效。
        </p>

        <div class="metric-list">
          <article v-for="item in metrics" :key="item.label" class="metric-card">
            <p class="metric-value">{{ item.value }}</p>
            <p class="metric-label">{{ item.label }}</p>
          </article>
        </div>

        <div class="status-flow">
          <span>下单</span>
          <span>揽收</span>
          <span>运输</span>
          <span>签收</span>
        </div>
      </section>

      <el-card class="login-card" shadow="never">
        <div class="auth-header">
          <div class="auth-icon">
            <el-icon><UserFilled /></el-icon>
          </div>
          <div>
            <h2 class="page-title">欢迎登录</h2>
            <p class="page-subtitle">登录后自动进入对应角色工作台</p>
          </div>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleLogin">
          <el-form-item label="账号" prop="account">
            <el-input v-model="form.account" size="large" placeholder="请输入用户名 / 手机号 / 邮箱" clearable>
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码">
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-alert
            type="info"
            :closable="false"
            show-icon
            class="login-tip"
            description="支持用户名、手机号、邮箱任一方式登录。"
          />

          <el-form-item class="submit-item">
            <el-button type="primary" size="large" class="login-btn" :loading="submitting" @click="handleLogin">
              登录系统
            </el-button>
          </el-form-item>
        </el-form>

        <div class="helper-actions">
          <el-button text type="primary" @click="handleForgetPassword">忘记密码</el-button>
          <el-button text @click="goRegister">去注册</el-button>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="forgotDialogVisible" width="560px" title="找回密码" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon class="forgot-tip">
        请使用“账号 + 用户名 + 常用地址”验证身份，然后设置新密码。
      </el-alert>
      <el-form ref="forgotFormRef" :model="forgotForm" :rules="forgotRules" label-width="96px">
        <el-form-item label="账号" prop="account">
          <el-input v-model="forgotForm.account" placeholder="用户名 / 手机号 / 邮箱" clearable />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="forgotForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="常用地址" prop="addressId">
          <el-select
            v-model="forgotForm.addressId"
            filterable
            clearable
            placeholder="请先点击“查询地址选项”"
            style="width: 100%"
          >
            <el-option v-for="item in forgotAddressOptions" :key="item.addressId" :label="item.label" :value="item.addressId" />
          </el-select>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="forgotForm.newPassword" type="password" show-password placeholder="8-20位，包含字母和数字" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="forgotForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-space>
          <el-button :loading="forgotLoading" @click="queryForgotAddressOptions">查询地址选项</el-button>
          <el-button type="primary" :loading="forgotResetLoading" @click="submitForgotPasswordReset">重置密码</el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, User, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { forgotPasswordOptions, forgotPasswordReset, login } from '../api/auth'
import { useAuthStore } from '../stores/auth'
import type { ForgotPasswordAddressOption } from '../types/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const forgotFormRef = ref<FormInstance>()
const forgotDialogVisible = ref(false)
const forgotLoading = ref(false)
const forgotResetLoading = ref(false)
const forgotAddressOptions = ref<ForgotPasswordAddressOption[]>([])

const metrics = [
  { label: '状态节点联动', value: '8+ 项' },
  { label: '订单流程闭环', value: '全流程' },
  { label: '角色权限控制', value: 'RBAC' },
]

const form = reactive({
  account: typeof route.query.account === 'string' ? route.query.account : '',
  password: '',
})

const forgotForm = reactive({
  account: '',
  username: '',
  addressId: null as number | null,
  newPassword: '',
  confirmPassword: '',
})

onMounted(() => {
  if (route.query.expired === '1') {
    ElMessage.warning('登录状态已过期，请重新登录')
    const query = { ...route.query }
    delete query.expired
    void router.replace({ path: route.path, query })
  }
})

const rules: FormRules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const forgotRules: FormRules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  addressId: [{ required: true, message: '请选择常用地址', trigger: 'change' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 20, message: '新密码长度必须在8到20位之间', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '新密码必须同时包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== forgotForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

async function handleLogin() {
  if (!formRef.value) {
    return
  }

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    const result = await login({
      account: form.account.trim(),
      password: form.password,
    })

    authStore.setLogin(result)
    ElMessage.success('登录成功')

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    if (redirect) {
      await router.replace(redirect)
      return
    }

    await router.replace('/main')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    submitting.value = false
  }
}

function handleForgetPassword() {
  forgotDialogVisible.value = true
  forgotAddressOptions.value = []
  forgotForm.account = form.account.trim()
  forgotForm.username = ''
  forgotForm.addressId = null
  forgotForm.newPassword = ''
  forgotForm.confirmPassword = ''
}

async function queryForgotAddressOptions() {
  if (!forgotForm.account.trim() || !forgotForm.username.trim()) {
    ElMessage.warning('请先填写账号和用户名')
    return
  }
  forgotLoading.value = true
  try {
    forgotAddressOptions.value = await forgotPasswordOptions({
      account: forgotForm.account.trim(),
      username: forgotForm.username.trim(),
    })
    if (!forgotAddressOptions.value.length) {
      ElMessage.warning('未查询到可用地址选项')
      forgotForm.addressId = null
      return
    }
    forgotForm.addressId = forgotAddressOptions.value[0].addressId
    ElMessage.success('地址选项已加载，请选择常用地址')
  } catch (error) {
    forgotAddressOptions.value = []
    forgotForm.addressId = null
    ElMessage.error(error instanceof Error ? error.message : '查询地址选项失败')
  } finally {
    forgotLoading.value = false
  }
}

async function submitForgotPasswordReset() {
  if (!forgotFormRef.value) {
    return
  }
  const valid = await forgotFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  forgotResetLoading.value = true
  try {
    await forgotPasswordReset({
      account: forgotForm.account.trim(),
      username: forgotForm.username.trim(),
      addressId: forgotForm.addressId as number,
      newPassword: forgotForm.newPassword,
    })
    ElMessage.success('密码重置成功，请使用新密码登录')
    forgotDialogVisible.value = false
    form.account = forgotForm.account.trim()
    form.password = ''
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '密码重置失败')
  } finally {
    forgotResetLoading.value = false
  }
}

function goRegister() {
  void router.push('/register')
}
</script>

<style scoped>
.login-scene {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 10% 15%, rgba(52, 152, 219, 0.24), transparent 35%),
    radial-gradient(circle at 85% 20%, rgba(22, 160, 133, 0.2), transparent 32%),
    linear-gradient(145deg, #f3f8ff 0%, #eef7f5 45%, #f9fbff 100%);
}

.orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(2px);
  animation: float 10s ease-in-out infinite;
}

.orb-a {
  top: 8%;
  left: 4%;
  width: 220px;
  height: 220px;
  background: linear-gradient(135deg, rgba(29, 93, 155, 0.35), rgba(29, 93, 155, 0.05));
}

.orb-b {
  right: 6%;
  bottom: 12%;
  width: 280px;
  height: 280px;
  background: linear-gradient(135deg, rgba(22, 160, 133, 0.25), rgba(22, 160, 133, 0.06));
  animation-delay: -2s;
}

.orb-c {
  right: 24%;
  top: 14%;
  width: 120px;
  height: 120px;
  background: linear-gradient(135deg, rgba(241, 196, 15, 0.22), rgba(241, 196, 15, 0.05));
  animation-delay: -4s;
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(16, 43, 76, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(16, 43, 76, 0.05) 1px, transparent 1px);
  background-size: 36px 36px;
  mask-image: radial-gradient(circle at center, black 55%, transparent 100%);
  pointer-events: none;
}

.login-shell {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  width: min(1200px, 100% - 48px);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  align-items: center;
  gap: 36px;
}

.visual-panel {
  padding: 36px 28px;
  color: #16385c;
  animation: fade-in-up 0.8s ease;
}

.visual-kicker {
  margin: 0 0 14px;
  font-size: 13px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #2a6ca8;
  font-weight: 700;
}

.visual-panel h1 {
  margin: 0;
  font-size: clamp(30px, 4vw, 44px);
  line-height: 1.2;
  color: #0f2842;
}

.visual-desc {
  margin: 16px 0 28px;
  max-width: 560px;
  font-size: 16px;
  line-height: 1.8;
  color: #365778;
}

.metric-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  background: rgba(255, 255, 255, 0.58);
  border: 1px solid rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border-radius: 14px;
  padding: 14px 12px;
}

.metric-value {
  margin: 0;
  color: #134066;
  font-weight: 700;
  font-size: 22px;
}

.metric-label {
  margin: 6px 0 0;
  color: #4e6984;
  font-size: 13px;
}

.status-flow {
  margin-top: 22px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.status-flow span {
  position: relative;
  font-size: 13px;
  color: #315b7d;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.8);
  padding: 6px 12px;
  border-radius: 999px;
}

.status-flow span:not(:last-child)::after {
  content: '→';
  margin-left: 10px;
  color: #5c7b98;
}

.login-card {
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(14px);
  box-shadow: 0 24px 60px rgba(21, 44, 72, 0.16);
  padding: 6px;
  animation: fade-in-up 0.95s ease;
}

.auth-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.auth-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #1d5d9b, #2679c2);
  box-shadow: 0 8px 20px rgba(29, 93, 155, 0.28);
}

.auth-icon :deep(.el-icon) {
  font-size: 20px;
}

.submit-item {
  margin-top: 6px;
  margin-bottom: 10px;
}

.login-tip {
  margin-bottom: 12px;
}

.login-btn {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.helper-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.forgot-tip {
  margin-bottom: 14px;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-14px);
  }
}

@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 980px) {
  .login-shell {
    grid-template-columns: 1fr;
    gap: 10px;
    padding: 24px 0;
    width: min(680px, 100% - 24px);
  }

  .visual-panel {
    padding: 8px 8px 14px;
  }

  .metric-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .metric-list {
    grid-template-columns: 1fr;
  }

  .visual-panel {
    padding: 6px 4px 10px;
  }

  .visual-desc {
    margin-bottom: 18px;
  }

  .login-shell {
    width: calc(100% - 18px);
  }
}
</style>
