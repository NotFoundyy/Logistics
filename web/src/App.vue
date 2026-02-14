<template>
  <el-container class="app-shell">
    <el-header v-if="!isGuestPage" class="app-header">
      <div class="brand-wrap" @click="goHome">
        <div class="brand-mark">物</div>
        <div class="brand-text">
          <div class="brand">快递物流管理系统</div>
          <div class="brand-sub">智慧调度与实时追踪平台</div>
        </div>
      </div>

      <div class="header-right" v-if="authStore.isLogin">
        <div class="current-page">
          <span class="current-page__label">当前页面</span>
          <strong>{{ currentPageText }}</strong>
        </div>

        <div class="header-meta">
          <el-tag size="small" type="success" effect="plain">系统在线</el-tag>
          <el-tag size="small" effect="plain">{{ roleText }}</el-tag>
        </div>

        <el-dropdown @command="handleUserCommand">
          <div class="user-trigger">
            <el-avatar class="user-avatar">{{ (authStore.user?.username || 'U').slice(0, 1).toUpperCase() }}</el-avatar>
            <div class="user-meta">
              <strong>{{ authStore.user?.username }}</strong>
              <span>{{ roleText }}</span>
            </div>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-main :class="['app-main', { 'app-main--auth': isGuestPage }]">
      <router-view />
    </el-main>
  </el-container>

  <profile-center-dialog v-model="profileDialogVisible" />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ProfileCenterDialog from './components/ProfileCenterDialog.vue'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const profileDialogVisible = ref(false)

const isGuestPage = computed(() => Boolean(route.meta.guestOnly))
const currentPageText = computed(() => {
  if (route.path === '/main') {
    return '工作台'
  }
  if (route.path === '/login') {
    return '登录'
  }
  if (route.path === '/register') {
    return '注册'
  }
  return '系统'
})
const roleText = computed(() => {
  if (authStore.isAdmin) {
    return '管理员'
  }
  if (authStore.isCourier) {
    return '快递员'
  }
  return '用户'
})

function goHome() {
  if (!authStore.isLogin) {
    void router.push('/login')
    return
  }
  void router.push('/main')
}

function handleUserCommand(command: string | number | object) {
  if (command === 'profile') {
    profileDialogVisible.value = true
    return
  }
  if (command === 'logout') {
    authStore.logout()
    ElMessage.success('已退出登录')
    void router.push('/login')
  }
}
</script>

<style scoped>
.current-page {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid #dce8f5;
  background: rgba(255, 255, 255, 0.88);
}

.current-page__label {
  color: #738aa1;
  font-size: 12px;
}

.current-page strong {
  color: #214a71;
  font-size: 13px;
}
</style>
