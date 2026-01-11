<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useDeviceStore } from '@/stores/deviceStore'
import { connectSse, disconnectSse } from '@/composables/useSse'
import DashboardLayout from '@/components/layout/DashboardLayout.vue'

const store = useDeviceStore()

onMounted(() => {
  // 建立SSE连接
  connectSse(store.clientId)
})

onUnmounted(() => {
  disconnectSse()
})
</script>

<template>
  <DashboardLayout />
</template>

<style scoped>
</style>
