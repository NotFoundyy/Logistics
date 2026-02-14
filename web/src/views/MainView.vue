<template>
  <div v-if="bootstrapping" class="main-loading">
    <el-card class="loading-card" shadow="never">
      <el-skeleton :rows="4" animated />
      <div class="loading-actions">
        <el-text class="muted-text">正在加载工作台数据...</el-text>
      </div>
    </el-card>
  </div>

  <el-result
    v-else-if="bootstrapError"
    icon="error"
    title="工作台初始化失败"
    :sub-title="bootstrapError"
    class="main-error"
  >
    <template #extra>
      <el-space>
        <el-button @click="retryBootstrap">重试</el-button>
        <el-button type="primary" @click="backToLogin">返回登录</el-button>
      </el-space>
    </template>
  </el-result>

  <component v-else :is="currentView" />
</template>

<script setup lang="ts">
import { computed, markRaw, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchMe } from '../api/auth'
import { useAuthStore } from '../stores/auth'
import AdminMainView from './main/AdminMainView.vue'
import CourierMainView from './main/CourierMainView.vue'
import UserMainView from './main/UserMainView.vue'

const authStore = useAuthStore()
const router = useRouter()
const bootstrapping = ref(true)
const bootstrapError = ref('')

onMounted(async () => {
  await bootstrap()
})

async function bootstrap() {
  bootstrapping.value = true
  bootstrapError.value = ''
  try {
    const me = await fetchMe()
    authStore.setUser(me)
  } catch (error) {
    const message = error instanceof Error ? error.message : '获取用户信息失败'
    bootstrapError.value = message
    ElMessage.error(message)
  } finally {
    bootstrapping.value = false
  }
}

const currentView = computed(() => {
  if (authStore.isAdmin) {
    return markRaw(AdminMainView)
  }
  if (authStore.isCourier) {
    return markRaw(CourierMainView)
  }
  return markRaw(UserMainView)
})

async function retryBootstrap() {
  await bootstrap()
}

async function backToLogin() {
  authStore.logout()
  await router.replace('/login')
}
</script>

<style scoped>
.main-loading {
  min-height: 60vh;
  display: grid;
  place-items: center;
}

.loading-card {
  width: min(560px, 100%);
}

.loading-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.main-error {
  min-height: 60vh;
  display: grid;
  place-items: center;
}
</style>
