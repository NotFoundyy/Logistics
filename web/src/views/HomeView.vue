<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>快递物流管理系统</span>
      </div>
    </template>
    <p>前端工程初始化完成，可与后端进行联调。</p>
    <el-space wrap>
      <el-button type="primary" @click="checkBackend">检测后端连通性</el-button>
      <el-tag :type="statusType">{{ statusText }}</el-tag>
    </el-space>
  </el-card>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const status = ref<'idle' | 'ok' | 'fail'>('idle')

const statusText = computed(() => {
  if (status.value === 'ok') return '后端可访问'
  if (status.value === 'fail') return '后端不可访问'
  return '未检测'
})

const statusType = computed(() => {
  if (status.value === 'ok') return 'success'
  if (status.value === 'fail') return 'danger'
  return 'info'
})

const checkBackend = async () => {
  try {
    await request.get('/actuator/health')
    status.value = 'ok'
    ElMessage.success('后端连通正常')
  } catch {
    status.value = 'fail'
    ElMessage.error('后端连通失败，请确认后端已启动')
  }
}
</script>
