<script setup>
/**
 * 左下面板 - 视频流
 */
import { ref, computed, watch } from 'vue'
import { useDeviceStore } from '@/stores/deviceStore'
import { BASE_URL } from '@/config/api'

const store = useDeviceStore()

const streamActive = ref(false)
const streamUrl = ref('')
const streamError = ref(false)

// 计算ESP32 IP
const esp32Ip = computed(() => store.deviceConfig.wifiIp || '')

// 开始视频流
function startStream() {
  const ip = esp32Ip.value
  if (!ip) {
    alert('设备IP未获取，请等待设备配置同步')
    return
  }
  streamError.value = false
  streamUrl.value = `http://${ip}/stream?t=${Date.now()}`
  streamActive.value = true
}

// 停止视频流
function stopStream() {
  streamUrl.value = ''
  streamActive.value = false
  streamError.value = false
}

// 视频流错误
function onStreamError() {
  streamError.value = true
}

// 拍照
async function capture() {
  try {
    store.pendingCapture = true
    const response = await fetch(`${BASE_URL}/mqtt/capture/${store.clientId}`, {
      method: 'POST'
    })
    const result = await response.json()
    console.log('[Capture] 结果:', result)
  } catch (e) {
    console.error('[Capture] 失败:', e)
    store.pendingCapture = false
  }
}
</script>

<template>
  <div class="left-bottom-panel tech-panel">
    <div class="panel-title">
      <span>📹</span> 视频监控
      <span v-if="esp32Ip" class="ip-badge">{{ esp32Ip }}</span>
      <span v-else class="ip-badge offline">等待IP...</span>
    </div>
    
    <div class="panel-content">
      <div class="video-container">
        <template v-if="streamActive && !streamError">
          <img :src="streamUrl" alt="视频流" class="video-stream" @error="onStreamError">
          <div class="video-overlay">
            <span class="live-badge">● LIVE</span>
            <button class="stop-btn" @click="stopStream">⏹ 停止</button>
          </div>
        </template>
        
        <div v-else-if="streamError" class="video-placeholder error" @click="startStream">
          <span class="placeholder-icon">❌</span>
          <span class="placeholder-text">视频流加载失败</span>
          <span class="placeholder-hint">点击重试</span>
        </div>
        
        <div v-else class="video-placeholder" @click="startStream">
          <span class="placeholder-icon">📷</span>
          <span class="placeholder-text">点击开启视频流</span>
          <span class="placeholder-hint" v-if="esp32Ip">IP: {{ esp32Ip }}</span>
          <span class="placeholder-hint" v-else>等待设备配置...</span>
        </div>
      </div>
      
      <div class="control-bar">
        <button v-if="!streamActive" class="btn btn-primary" @click="startStream" :disabled="!esp32Ip">▶ 开始</button>
        <button v-else class="btn btn-stop" @click="stopStream">⏹ 停止</button>
        <button class="btn btn-capture" @click="capture">📸 拍照</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.left-bottom-panel { display: flex; flex-direction: column; height: 100%; }
.panel-title { display: flex; align-items: center; gap: 8px; }
.ip-badge { margin-left: auto; background: rgba(0, 255, 136, 0.2); color: var(--theme-success); padding: 2px 8px; border-radius: 10px; font-size: 10px; font-family: monospace; }
.ip-badge.offline { background: rgba(255, 51, 102, 0.2); color: var(--theme-danger); }
.panel-content { flex: 1; padding: 12px; display: flex; flex-direction: column; gap: 10px; }
.video-container { flex: 1; background: #000; border-radius: var(--radius-md); overflow: hidden; position: relative; min-height: 150px; }
.video-stream { width: 100%; height: 100%; object-fit: contain; display: block; }
.video-overlay { position: absolute; top: 0; left: 0; right: 0; padding: 8px; display: flex; justify-content: space-between; align-items: center; background: linear-gradient(180deg, rgba(0,0,0,0.5) 0%, transparent 100%); }
.live-badge { background: var(--theme-danger); color: white; padding: 2px 8px; border-radius: 4px; font-size: 10px; font-weight: bold; animation: blink 1s infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.7; } }
.stop-btn { background: rgba(255, 51, 102, 0.8); color: white; border: none; padding: 4px 10px; border-radius: 4px; font-size: 10px; cursor: pointer; }
.video-placeholder { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; cursor: pointer; color: var(--text-muted); transition: 0.2s; }
.video-placeholder:hover { color: var(--theme-primary); background: rgba(0, 242, 255, 0.05); }
.video-placeholder.error { color: var(--theme-danger); }
.placeholder-icon { font-size: 40px; }
.placeholder-text { font-size: 12px; }
.placeholder-hint { font-size: 10px; opacity: 0.7; }
.control-bar { display: flex; gap: 8px; }
.btn { flex: 1; padding: 10px; border: none; border-radius: var(--radius-md); font-size: 12px; cursor: pointer; transition: 0.2s; display: flex; align-items: center; justify-content: center; gap: 4px; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary { background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary)); color: #000; }
.btn-stop { background: var(--theme-danger); color: white; }
.btn-capture { background: linear-gradient(135deg, #667eea, #764ba2); color: white; }
.btn:hover:not(:disabled) { transform: translateY(-1px); }
</style>
