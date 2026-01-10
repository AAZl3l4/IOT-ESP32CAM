<script setup>
/**
 * 右上面板 - 设备控制
 */
import { ref, watch } from 'vue'
import { useDeviceStore } from '@/stores/deviceStore'
import { BASE_URL } from '@/config/api'

const store = useDeviceStore()

// API调用函数
async function apiCall(url, method = 'POST', body = null) {
  try {
    const options = { method }
    if (body) {
      options.headers = { 'Content-Type': 'application/json' }
      options.body = JSON.stringify(body)
    }
    console.log('[API] 请求:', url, body)
    const response = await fetch(url, options)
    const data = await response.json()
    console.log('[API] 响应:', data)
    return data
  } catch (error) {
    console.error('[API] 错误:', error)
    return { code: -1, msg: error.message }
  }
}

// 加载状态
const ledLoading = ref(false)
const redLedLoading = ref(false)
const fanLoading = ref(false)
const servoLoading = ref(false)

// LED控制
async function toggleLed() {
  ledLoading.value = true
  try {
    const newValue = store.controlState.ledStatus ? 0 : 1
    console.log('[LED] 控制请求:', newValue)
    const result = await apiCall(`${BASE_URL}/mqtt/led/${store.clientId}`, 'POST', { value: newValue })
    if (result.code === 0) {
      store.controlState.ledStatus = !store.controlState.ledStatus
    }
  } catch (e) {
    console.error('[LED] 控制失败:', e)
  } finally {
    ledLoading.value = false
  }
}

// 红色指示灯
async function toggleRedLed() {
  redLedLoading.value = true
  try {
    const newValue = store.controlState.redLedStatus ? 0 : 1
    const result = await apiCall(`${BASE_URL}/mqtt/red-led/${store.clientId}`, 'POST', { value: newValue })
    if (result.code === 0) {
      store.controlState.redLedStatus = !store.controlState.redLedStatus
    }
  } catch (e) {
    console.error('[RedLED] 控制失败:', e)
  } finally {
    redLedLoading.value = false
  }
}

// 风扇控制
async function toggleFan() {
  fanLoading.value = true
  try {
    const newValue = !store.controlState.relayStatus
    const result = await apiCall(`${BASE_URL}/mqtt/relay/${store.clientId}`, 'POST', { on: newValue })
    if (result.code === 0) {
      store.controlState.relayStatus = newValue
    }
  } catch (e) {
    console.error('[Fan] 控制失败:', e)
  } finally {
    fanLoading.value = false
  }
}

// 窗户控制
async function setWindow(angle) {
  servoLoading.value = true
  try {
    const result = await apiCall(`${BASE_URL}/mqtt/servo/${store.clientId}`, 'POST', { angle })
    if (result.code === 0) {
      store.controlState.servoAngle = angle
    }
  } catch (e) {
    console.error('[Servo] 控制失败:', e)
  } finally {
    servoLoading.value = false
  }
}

// 亮度调节
const brightnessValue = ref(0)
let brightnessTimer = null

watch(() => store.controlState.ledBrightness, (val) => {
  brightnessValue.value = val
}, { immediate: true })

function onBrightnessChange(e) {
  const value = parseInt(e.target.value)
  brightnessValue.value = value
  
  if (brightnessTimer) clearTimeout(brightnessTimer)
  brightnessTimer = setTimeout(async () => {
    const result = await apiCall(`${BASE_URL}/mqtt/led-brightness/${store.clientId}`, 'POST', { brightness: value })
    if (result.code === 0) {
      store.controlState.ledBrightness = value
    }
  }, 300)
}

// 舵机滑块
const servoSliderValue = ref(0)
let servoTimer = null

watch(() => store.controlState.servoAngle, (val) => {
  servoSliderValue.value = val
}, { immediate: true })

function onServoSliderChange(e) {
  const value = parseInt(e.target.value)
  servoSliderValue.value = value
  
  if (servoTimer) clearTimeout(servoTimer)
  servoTimer = setTimeout(() => setWindow(value), 300)
}

// 分辨率
async function setResolution(framesize) {
  await apiCall(`${BASE_URL}/mqtt/stream-resolution/${store.clientId}`, 'POST', { framesize })
}
</script>

<template>
  <div class="right-top-panel tech-panel">
    <div class="panel-title">
      <span>🎛️</span> 设备控制
    </div>
    
    <div class="panel-content">
      <!-- LED控制 -->
      <div class="control-item" :class="{ active: store.controlState.ledStatus }">
        <div class="control-info">
          <span class="control-icon">{{ store.controlState.ledStatus ? '💡' : '🌑' }}</span>
          <div class="control-text">
            <span class="control-name">闪光灯</span>
            <span class="control-status">{{ store.controlState.ledStatus ? '已开启' : '已关闭' }}</span>
          </div>
        </div>
        <label class="toggle-switch">
          <input type="checkbox" :checked="store.controlState.ledStatus" @change="toggleLed" :disabled="ledLoading">
          <span class="toggle-slider"></span>
        </label>
      </div>
      
      <!-- 亮度 -->
      <div class="control-item brightness-control">
        <div class="slider-row">
          <span class="slider-label">🔆 亮度</span>
          <input type="range" class="slider" min="0" max="255" :value="brightnessValue" @input="onBrightnessChange">
          <span class="slider-value">{{ brightnessValue }}</span>
        </div>
      </div>
      
      <!-- 指示灯 -->
      <div class="control-item" :class="{ active: store.controlState.redLedStatus }">
        <div class="control-info">
          <span class="control-icon">{{ store.controlState.redLedStatus ? '🔴' : '⚫' }}</span>
          <div class="control-text">
            <span class="control-name">指示灯</span>
            <span class="control-status">{{ store.controlState.redLedStatus ? '已开启' : '已关闭' }}</span>
          </div>
        </div>
        <label class="toggle-switch">
          <input type="checkbox" :checked="store.controlState.redLedStatus" @change="toggleRedLed" :disabled="redLedLoading">
          <span class="toggle-slider"></span>
        </label>
      </div>
      
      <!-- 风扇 -->
      <div class="control-item" :class="{ active: store.controlState.relayStatus }">
        <div class="control-info">
          <span class="control-icon fan-icon" :class="{ spinning: store.controlState.relayStatus }">🌀</span>
          <div class="control-text">
            <span class="control-name">风扇</span>
            <span class="control-status">{{ store.controlState.relayStatus ? '运转中' : '已停止' }}</span>
          </div>
        </div>
        <label class="toggle-switch">
          <input type="checkbox" :checked="store.controlState.relayStatus" @change="toggleFan" :disabled="fanLoading">
          <span class="toggle-slider"></span>
        </label>
      </div>
      
      <!-- 窗户 -->
      <div class="control-item window-control">
        <div class="control-header">
          <span class="control-icon">🪟</span>
          <span class="control-name">窗户</span>
          <span class="control-angle">{{ store.controlState.servoAngle }}°</span>
        </div>
        <div class="window-buttons">
          <button class="window-btn" :class="{ active: store.controlState.servoAngle === 0 }" @click="setWindow(0)" :disabled="servoLoading">❌ 关</button>
          <button class="window-btn" :class="{ active: store.controlState.servoAngle === 45 }" @click="setWindow(45)" :disabled="servoLoading">小开</button>
          <button class="window-btn" :class="{ active: store.controlState.servoAngle === 90 }" @click="setWindow(90)" :disabled="servoLoading">半开</button>
          <button class="window-btn" :class="{ active: store.controlState.servoAngle === 180 }" @click="setWindow(180)" :disabled="servoLoading">🌬️ 全</button>
        </div>
        <div class="slider-row" style="margin-top: 8px;">
          <input type="range" class="slider" min="0" max="180" :value="servoSliderValue" @input="onServoSliderChange">
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.right-top-panel { display: flex; flex-direction: column; height: 100%; }
.panel-content { flex: 1; padding: 12px; display: flex; flex-direction: column; gap: 8px; overflow-y: auto; }
.control-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 12px; background: var(--bg-card); border-radius: var(--radius-md); border-left: 3px solid transparent; transition: 0.2s; }
.control-item:hover { background: var(--bg-hover); }
.control-item.active { border-left-color: var(--theme-success); background: rgba(0, 255, 136, 0.1); }
.control-info { display: flex; align-items: center; gap: 10px; }
.control-icon { font-size: 18px; }
.fan-icon.spinning { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.control-text { display: flex; flex-direction: column; }
.control-name { font-size: 13px; font-weight: 500; }
.control-status { font-size: 10px; color: var(--text-muted); }
.brightness-control, .window-control, .resolution-control { flex-direction: column; align-items: stretch; gap: 8px; }
.control-header { display: flex; align-items: center; gap: 8px; }
.control-angle { margin-left: auto; color: var(--theme-primary); font-weight: bold; }
.slider-row { display: flex; align-items: center; gap: 10px; width: 100%; }
.slider-label { font-size: 12px; color: var(--text-secondary); min-width: 50px; }
.slider-value { font-size: 12px; color: var(--theme-primary); min-width: 30px; text-align: right; }
.window-buttons, .resolution-buttons { display: flex; gap: 6px; }
.window-btn, .res-btn { flex: 1; padding: 6px; border: 1px solid rgba(255, 255, 255, 0.1); background: rgba(0, 0, 0, 0.2); color: var(--text-secondary); font-size: 10px; border-radius: var(--radius-sm); cursor: pointer; transition: 0.2s; }
.window-btn:hover, .res-btn:hover { background: var(--bg-hover); border-color: var(--theme-primary); }
.window-btn.active { background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary)); color: #000; border-color: transparent; }
.window-btn:disabled, .res-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
