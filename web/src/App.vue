<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <div class="brand">Logistics Web</div>
      <el-menu mode="horizontal" :default-active="activePath" router>
        <el-menu-item index="/">首页</el-menu-item>
      </el-menu>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activePath = computed(() => route.path)
</script>
