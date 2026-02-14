<template>
  <div class="register-scene">
    <div class="scene-glow glow-a" />
    <div class="scene-glow glow-b" />

    <el-card class="page-card register-card" shadow="never">
      <div class="auth-header">
        <p class="auth-kicker">Create Account</p>
        <h2 class="page-title">用户注册</h2>
        <p class="page-subtitle">注册成功后可直接登录进入用户工作台。</p>
      </div>

      <div class="register-tips">
        <el-tag effect="plain">仅需手机号即可注册</el-tag>
        <el-tag effect="plain" type="success">支持手机号 / 邮箱登录</el-tag>
        <el-tag effect="plain" type="warning">密码建议字母+数字组合</el-tag>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="95px" class="register-form">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" maxlength="50" show-word-limit placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="请输入11位手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="可选，如 demo@example.com" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少8位，包含字母和数字" />
        </el-form-item>
        <el-progress :percentage="passwordStrength" :status="passwordStrength >= 80 ? 'success' : undefined" />
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入密码" />
        </el-form-item>
        <el-form-item>
          <el-space>
            <el-button type="primary" :loading="submitting" @click="handleRegister">注册</el-button>
            <el-button @click="goLogin">返回登录</el-button>
          </el-space>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { register } from '../api/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  username: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const passwordStrength = computed(() => {
  const value = form.password || ''
  if (!value) {
    return 0
  }
  let score = 20
  if (value.length >= 8) {
    score += 30
  }
  if (/[A-Z]/.test(value) || /[a-z]/.test(value)) {
    score += 20
  }
  if (/\d/.test(value)) {
    score += 20
  }
  if (/[^A-Za-z0-9]/.test(value)) {
    score += 10
  }
  return Math.min(100, score)
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/, message: '密码至少8位且包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.password) {
          callback(new Error('两次密码输入不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

async function handleRegister() {
  if (!formRef.value) {
    return
  }

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    await register({
      username: form.username.trim(),
      phone: form.phone.trim(),
      email: form.email.trim() || undefined,
      password: form.password,
    })

    ElMessage.success('注册成功，请登录')
    await router.replace({
      path: '/login',
      query: { account: form.phone.trim() },
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '注册失败')
  } finally {
    submitting.value = false
  }
}

function goLogin() {
  void router.push('/login')
}
</script>

<style scoped>
.register-scene {
  position: relative;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  overflow: hidden;
}

.scene-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(3px);
}

.glow-a {
  width: 260px;
  height: 260px;
  left: -40px;
  top: 12%;
  background: radial-gradient(circle, rgba(56, 140, 220, 0.28), rgba(56, 140, 220, 0.04));
}

.glow-b {
  width: 320px;
  height: 320px;
  right: -80px;
  bottom: -40px;
  background: radial-gradient(circle, rgba(82, 199, 165, 0.25), rgba(82, 199, 165, 0.04));
}

.register-card {
  position: relative;
  z-index: 1;
  width: 620px;
  max-width: 100%;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.92);
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(14px);
  box-shadow: 0 24px 48px rgba(17, 48, 80, 0.14);
}

.auth-header {
  margin-bottom: 16px;
}

.auth-kicker {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #3c7cb5;
  font-weight: 700;
}

.register-form {
  padding-right: 6px;
}

.register-tips {
  margin-bottom: 14px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 720px) {
  .register-scene {
    padding: 10px;
  }
}
</style>
