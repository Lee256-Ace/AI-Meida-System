<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

// 应用启动时检查登录状态
onMounted(() => {
  // 检查token有效性
  const isTokenValid = userStore.checkTokenValidity()
  
  // 如果token无效且当前不在登录页，跳转到登录页
  if (!isTokenValid && router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
})
</script>

<style>
#app {
  height: 100vh;
}
</style>