<script setup>
/**
 * 智能自动化配置弹窗
 * 移植自 test-panel.html "智能自动化" 部分
 */
import { ref, watch, onMounted } from 'vue'
import { useDeviceStore } from '@/stores/deviceStore'
import { BASE_URL } from '@/config/api'
import ModalDialog from '@/components/common/ModalDialog.vue'

const props = defineProps({
  visible: Boolean
})

const emit = defineEmits(['close'])
const store = useDeviceStore()
const loading = ref(false)

// 自动化配置对象
const config = ref({
  enabled: true,
  tempHigh: 25,
  tempLow: 17,
  humidHigh: 90,
  humidLow: 45,
  memoryThreshold: 51200, // bytes
  rssiThreshold: -75,
  manualPauseMs: 300000
})

watch(() => props.visible, (val) => {
  if (val) loadConfig()
})

async function apiCall(url, method = 'GET', body = null) {
  try {
    const options = { method }
    if (body) {
        options.headers = { 'Content-Type': 'application/json' }
        options.body = JSON.stringify(body)
    }
    const response = await fetch(url, options)
    return await response.json()
  } catch (e) {
    return { code: -1, msg: e.message }
  }
}

async function loadConfig() {
  loading.value = true
  const res = await apiCall(`${BASE_URL}/automation/config/${store.clientId}`)
  if (res.code === 0 && res.data) {
    config.value = res.data
  }
  loading.value = false
}

async function saveConfig() {
  loading.value = true
  const payload = { ...config.value }
  const res = await apiCall(`${BASE_URL}/automation/config/${store.clientId}`, 'POST', payload)
  loading.value = false
  if (res.code === 0) {
    alert('✅ 自动化配置保存成功')
    emit('close')
  } else {
    alert('❌ 保存失败: ' + res.msg)
  }
}

const memoryKB = ref(50)
watch(() => config.value.memoryThreshold, (val) => {
    memoryKB.value = Math.floor(val / 1024)
})
function updateMemory(e) {
    memoryKB.value = e.target.value
    config.value.memoryThreshold = e.target.value * 1024
}

</script>

<template>
  <ModalDialog :visible="visible" title="🤖 智能自动化配置" @close="$emit('close')" width="700px">
    <div class="auto-config-content">
      
      <!-- 启用开关 -->
      <div class="enable-row">
        <label class="toggle-switch large">
          <input type="checkbox" v-model="config.enabled">
          <span class="toggle-slider"></span>
        </label>
        <span class="enable-text">{{ config.enabled ? '自动化已启用' : '自动化已禁用' }}</span>
      </div>

      <div class="config-grid">
        <!-- 温度阈值 -->
        <div class="config-card temp">
          <div class="card-icon">🌡️</div>
          <div class="card-title">温度控制</div>
          <div class="input-row">
            <label>高温阈值</label>
            <div class="input-wrapper">
                <input type="number" v-model.number="config.tempHigh"> <span>℃</span>
            </div>
          </div>
          <div class="input-row">
            <label>低温阈值</label>
            <div class="input-wrapper">
                <input type="number" v-model.number="config.tempLow"> <span>℃</span>
            </div>
          </div>
        </div>

        <!-- 湿度阈值 -->
        <div class="config-card humid">
          <div class="card-icon">💧</div>
          <div class="card-title">湿度控制</div>
          <div class="input-row">
            <label>高湿阈值</label>
             <div class="input-wrapper">
                <input type="number" v-model.number="config.humidHigh"> <span>%</span>
             </div>
          </div>
          <div class="input-row">
            <label>低湿阈值</label>
            <div class="input-wrapper">
                <input type="number" v-model.number="config.humidLow"> <span>%</span>
            </div>
          </div>
        </div>

        <!-- 设备监控 -->
        <div class="config-card monitor">
          <div class="card-icon">⚠️</div>
          <div class="card-title">设备监控</div>
          <div class="input-row">
            <label>内存阈值</label>
            <div class="input-wrapper">
                <input type="number" :value="memoryKB" @input="updateMemory"> <span>KB</span>
            </div>
          </div>
          <div class="input-row">
            <label>信号阈值</label>
            <div class="input-wrapper">
                <input type="number" v-model.number="config.rssiThreshold"> <span>dBm</span>
            </div>
          </div>
        </div>

        <!-- 暂停设置 -->
        <div class="config-card pause">
          <div class="card-icon">⏸️</div>
          <div class="card-title">手动暂停</div>
          <div class="input-row">
            <label>暂停时间</label>
            <select v-model.number="config.manualPauseMs">
                <option :value="60000">1分钟</option>
                <option :value="180000">3分钟</option>
                <option :value="300000">5分钟</option>
                <option :value="600000">10分钟</option>
                <option :value="1800000">30分钟</option>
            </select>
          </div>
          <div class="hint">手动操作后暂停自动化的时间</div>
        </div>
      </div>

      <div class="action-footer">
        <button class="btn btn-primary" @click="loadConfig">🔄 重置</button>
        <button class="btn btn-success" @click="saveConfig" :disabled="loading">
            {{ loading ? '保存中...' : '💾 保存配置' }}
        </button>
      </div>
    </div>
  </ModalDialog>
</template>

<style scoped>
.auto-config-content {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.enable-row {
    display: flex;
    align-items: center;
    gap: 15px;
    padding: 10px;
    background: rgba(0,0,0,0.2);
    border-radius: 8px;
}

.enable-text {
    font-size: 16px;
    font-weight: bold;
    color: var(--text-primary);
}

.config-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 15px;
}

.config-card {
    padding: 15px;
    border-radius: 12px;
    color: white;
    position: relative;
    overflow: hidden;
}

.config-card.temp { background: linear-gradient(135deg, rgba(255, 107, 107, 0.2), rgba(238, 90, 36, 0.2)); border: 1px solid #ff6b6b; }
.config-card.humid { background: linear-gradient(135deg, rgba(116, 185, 255, 0.2), rgba(9, 132, 227, 0.2)); border: 1px solid #74b9ff; }
.config-card.monitor { background: linear-gradient(135deg, rgba(162, 155, 254, 0.2), rgba(108, 92, 231, 0.2)); border: 1px solid #a29bfe; }
.config-card.pause { background: linear-gradient(135deg, rgba(253, 203, 110, 0.2), rgba(225, 112, 85, 0.2)); border: 1px solid #fdcb6e; }

.card-title {
    font-size: 14px;
    font-weight: bold;
    margin-bottom: 15px;
    display: flex;
    align-items: center;
    gap: 8px;
}

.card-icon {
    position: absolute;
    top: 10px;
    right: 15px;
    font-size: 24px;
    opacity: 0.2;
}

.input-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
}

.input-wrapper {
    display: flex;
    align-items: center;
    gap: 5px;
}

.input-row label {
    font-size: 12px;
    opacity: 0.9;
}

.input-row input, .input-row select {
    width: 70px;
    padding: 4px;
    background: rgba(0,0,0,0.3);
    border: 1px solid rgba(255,255,255,0.2);
    border-radius: 4px;
    color: white;
    text-align: right;
}

.hint {
    font-size: 10px;
    opacity: 0.7;
    margin-top: 5px;
    text-align: right;
}

.action-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 10px;
}

.btn {
    padding: 10px 25px;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    font-weight: bold;
}

.btn-primary { background: var(--bg-card); color: var(--text-primary); border: 1px solid #555; }
.btn-success { background: var(--theme-success); color: #000; }
</style>
