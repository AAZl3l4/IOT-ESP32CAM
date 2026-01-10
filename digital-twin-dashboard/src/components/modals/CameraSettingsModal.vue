<script setup>
/**
 * 摄像头参数设置弹窗
 * 数据从store.cameraConfig读取，由SSE config事件更新
 */
import { computed } from 'vue'
import { useDeviceStore } from '@/stores/deviceStore'
import { BASE_URL } from '@/config/api'
import ModalDialog from '@/components/common/ModalDialog.vue'

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['close'])
const store = useDeviceStore()

const SPECIAL_EFFECT_MAP = {
  0: '无特效', 1: '负片', 2: '黑白', 3: '复古', 4: '蓝调', 5: '绿调', 6: '红调'
}

const FRAMESIZE_MAP = {
  0: { name: '96x96' },
  1: { name: 'QQVGA (160x120)' },
  2: { name: 'QCIF (176x144)' },
  3: { name: 'HQVGA (240x176)' },
  4: { name: '240x240' },
  5: { name: 'QVGA (320x240)' },
  6: { name: 'CIF (400x296)' },
  7: { name: 'HVGA (480x320)' },
  8: { name: 'VGA (640x480)' },
  9: { name: 'SVGA (800x600)' },
  10: { name: 'XGA (1024x768)' },
  11: { name: 'HD (1280x720)' },
  12: { name: 'SXGA (1280x1024)' },
  13: { name: 'UXGA (1600x1200)' }
}

// 从store读取摄像头配置
const cam = computed(() => store.cameraConfig)

async function apiCall(url, method = 'POST', body = null) {
  try {
    const options = { method }
    if (body) {
      options.headers = { 'Content-Type': 'application/json' }
      options.body = JSON.stringify(body)
    }
    const response = await fetch(url, options)
    return await response.json()
  } catch (error) {
    return { code: -1 }
  }
}

async function quickParam(name, value) {
  await apiCall(`${BASE_URL}/mqtt/param/${store.clientId}`, 'POST', { name, value })
  // 乐观更新store
  if (name === 'framesize') store.controlState.framesize = Number(value)
  else if (name === 'brightness') store.cameraConfig.brightness = value
  else if (name === 'contrast') store.cameraConfig.contrast = value
  else if (name === 'saturation') store.cameraConfig.saturation = value
  else if (name === 'quality') store.cameraConfig.quality = value
  else if (name === 'special_effect') store.cameraConfig.specialEffect = value
}

async function toggleParam(paramName) {
  const keyMap = {
    'awb': 'whiteBalance', 'aec': 'aec', 'agc': 'gainCtrl',
    'hmirror': 'hmirror', 'vflip': 'vflip', 'bpc': 'bpc', 'wpc': 'wpc', 'lenc': 'lenc'
  }
  const key = keyMap[paramName]
  const currentVal = cam.value[key] || 0
  const newValue = currentVal ? 0 : 1
  await apiCall(`${BASE_URL}/mqtt/param/${store.clientId}`, 'POST', { name: paramName, value: newValue })
  store.cameraConfig[key] = newValue
}
</script>

<template>
  <ModalDialog :visible="visible" title="🎨 摄像头参数调整" @close="$emit('close')">
    <div class="cam-settings-grid">
      <!-- 分辨率 -->
      <div class="setting-group">
        <div class="group-header">📐 分辨率 <span class="current-val">{{ FRAMESIZE_MAP[store.controlState.framesize]?.name || store.controlState.framesize }}</span></div>
        <div class="btn-grid three-col">
          <button v-for="size in [5, 6, 7, 8, 9, 10, 11, 12, 13]" :key="size" 
            class="param-btn"
            :class="{ active: Number(store.controlState.framesize) === size }"
            @click="quickParam('framesize', size)">
            {{ FRAMESIZE_MAP[size]?.name || size }}
          </button>
        </div>
      </div>

      <!-- 质量 -->
      <div class="setting-group">
        <div class="group-header">🎯 质量 <span class="current-val">{{ cam.quality <= 10 ? '高' : cam.quality <= 20 ? '标准' : '快速' }}</span></div>
        <div class="btn-grid">
          <button class="param-btn" :class="{ active: cam.quality <= 10 }" @click="quickParam('quality', 10)">🏆 高质量</button>
          <button class="param-btn" :class="{ active: cam.quality > 10 && cam.quality <= 20 }" @click="quickParam('quality', 20)">⚪ 标准</button>
          <button class="param-btn" :class="{ active: cam.quality > 20 }" @click="quickParam('quality', 40)">⚡ 快速</button>
        </div>
      </div>

      <!-- 亮度 -->
      <div class="setting-group">
        <div class="group-header">☀️ 亮度 <span class="current-val">{{ cam.brightness }}</span></div>
        <div class="btn-grid five-col">
          <button v-for="v in [-2,-1,0,1,2]" :key="v" class="param-btn" :class="{ active: cam.brightness === v }" @click="quickParam('brightness', v)">{{ v > 0 ? '+'+v : v }}</button>
        </div>
      </div>

      <!-- 对比度 -->
      <div class="setting-group">
        <div class="group-header">🎚️ 对比度 <span class="current-val">{{ cam.contrast }}</span></div>
        <div class="btn-grid five-col">
          <button v-for="v in [-2,-1,0,1,2]" :key="v" class="param-btn" :class="{ active: cam.contrast === v }" @click="quickParam('contrast', v)">{{ v > 0 ? '+'+v : v }}</button>
        </div>
      </div>

      <!-- 饱和度 -->
      <div class="setting-group">
        <div class="group-header">🌈 饱和度 <span class="current-val">{{ cam.saturation }}</span></div>
        <div class="btn-grid five-col">
          <button v-for="v in [-2,-1,0,1,2]" :key="v" class="param-btn" :class="{ active: cam.saturation === v }" @click="quickParam('saturation', v)">{{ v > 0 ? '+'+v : v }}</button>
        </div>
      </div>

      <!-- 特效 -->
      <div class="setting-group full-width">
        <div class="group-header">✨ 特效 <span class="current-val">{{ SPECIAL_EFFECT_MAP[cam.specialEffect] }}</span></div>
        <div class="btn-grid wrap">
          <button v-for="(label, val) in SPECIAL_EFFECT_MAP" :key="val" 
            class="param-btn" :class="{ active: cam.specialEffect === parseInt(val) }" @click="quickParam('special_effect', parseInt(val))">
            {{ label }}
          </button>
        </div>
      </div>

      <!-- 高级开关 -->
      <div class="setting-group full-width">
        <div class="group-header">⚙️ 高级设置</div>
        <div class="switch-grid">
          <button class="switch-btn" :class="{on: cam.whiteBalance}" @click="toggleParam('awb')">白平衡</button>
          <button class="switch-btn" :class="{on: cam.aec}" @click="toggleParam('aec')">自动曝光</button>
          <button class="switch-btn" :class="{on: cam.gainCtrl}" @click="toggleParam('agc')">自动增益</button>
          <button class="switch-btn" :class="{on: cam.hmirror}" @click="toggleParam('hmirror')">水平镜像</button>
          <button class="switch-btn" :class="{on: cam.vflip}" @click="toggleParam('vflip')">垂直翻转</button>
          <button class="switch-btn" :class="{on: cam.bpc}" @click="toggleParam('bpc')">黑点校正</button>
          <button class="switch-btn" :class="{on: cam.wpc}" @click="toggleParam('wpc')">白点校正</button>
          <button class="switch-btn" :class="{on: cam.lenc}" @click="toggleParam('lenc')">镜头校正</button>
        </div>
      </div>
    </div>
  </ModalDialog>
</template>

<style scoped>
.cam-settings-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
.setting-group { background: rgba(0,0,0,0.2); padding: 10px; border-radius: 8px; }
.setting-group.full-width { grid-column: span 2; }
.group-header { font-size: 12px; color: var(--text-secondary); margin-bottom: 8px; font-weight: bold; display: flex; justify-content: space-between; }
.current-val { color: var(--theme-primary); font-weight: normal; }
.btn-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(60px, 1fr)); gap: 5px; }
.btn-grid.three-col { grid-template-columns: repeat(3, 1fr); }
.btn-grid.five-col { grid-template-columns: repeat(5, 1fr); }
.btn-grid.wrap { display: flex; flex-wrap: wrap; gap: 5px; }
.param-btn { background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); color: #ccc; padding: 5px; font-size: 11px; border-radius: 4px; cursor: pointer; transition: all 0.2s; }
.param-btn:hover { background: var(--theme-primary); color: #000; }
.param-btn.active { background: var(--theme-primary); color: #000; border-color: var(--theme-primary); font-weight: bold; }
.switch-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.switch-btn { padding: 8px; background: #2a2a2a; border: 1px solid #444; color: #888; border-radius: 4px; cursor: pointer; font-size: 11px; position: relative; padding-left: 20px; }
.switch-btn:before { content: ''; position: absolute; left: 6px; top: 50%; transform: translateY(-50%); width: 6px; height: 6px; border-radius: 50%; background: #555; transition: 0.3s; }
.switch-btn.on { border-color: var(--theme-success); color: var(--theme-success); }
.switch-btn.on:before { background: var(--theme-success); box-shadow: 0 0 5px var(--theme-success); }
.loading-state { position: absolute; top: 50px; right: 20px; font-size: 12px; color: var(--theme-primary); display: flex; align-items: center; gap: 5px; }
</style>
