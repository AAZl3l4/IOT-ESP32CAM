<script setup>
/**
 * 仪表盘主布局
 */
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useDeviceStore } from '@/stores/deviceStore'
import { useApi } from '@/composables/useApi'
import { BASE_URL } from '@/config/api'
import DashboardHeader from './DashboardHeader.vue'
import LeftTopPanel from '../panels/LeftTopPanel.vue'
import LeftBottomPanel from '../panels/LeftBottomPanel.vue'
import RightTopPanel from '../panels/RightTopPanel.vue'
import RightBottomPanel from '../panels/RightBottomPanel.vue'
import DigitalTwinScene from '../three/DigitalTwinScene.vue'

// 引入模态框
import CameraSettingsModal from '../modals/CameraSettingsModal.vue'
import SystemSettingsModal from '../modals/SystemSettingsModal.vue'
import AutomationModal from '../modals/AutomationModal.vue'
import AiAssistantModal from '../modals/AiAssistantModal.vue'
import PhotoPreviewModal from '../modals/PhotoPreviewModal.vue'

const store = useDeviceStore()
const { getDhtDashboard, getStatusHistory } = useApi(store.clientId)

// 模态框状态
const modals = ref({
    camera: false,
    settings: false,
    automation: false,
    ai: false,
    preview: false
})

// 预览状态
const previewUrl = ref('')
const previewFile = ref('')
const aiModalRef = ref(null)

function openModal(name) {
    modals.value[name] = true
}

function closeModal(name) {
    modals.value[name] = false
}

// 处理拍照结果
function handleCaptureResult(e) {
    const data = e.detail
    if (store.pendingCapture) {
        store.pendingCapture = false // 消费掉这个状态
        if (data.imageFile) {
            previewFile.value = data.imageFile
            previewUrl.value = `${BASE_URL}/file/photos/${data.imageFile}`
            openModal('preview')
        }
    }
}

// 触发AI分析
function handleAnalyze(imageFile) {
    closeModal('preview')
    openModal('ai')
    // 等待模态框打开后调用分析方法
    nextTick(() => {
        if (aiModalRef.value) {
            aiModalRef.value.analyzeImage(imageFile)
        }
    })
}

// 加载历史数据
async function loadHistoryData() {
    try {
        const dhtRes = await getDhtDashboard()
        if (dhtRes.code === 0 && dhtRes.data) {
            store.initDhtHistory(dhtRes.data)
        }
        
        const statusRes = await getStatusHistory()
        if (statusRes.code === 0 && statusRes.data) {
            store.initStatusHistory(statusRes.data)
        }
    } catch(e) {
        console.error('加载历史数据失败:', e)
    }
}

onMounted(() => {
    window.addEventListener('capture-result', handleCaptureResult)
    loadHistoryData()
})

onUnmounted(() => {
    window.removeEventListener('capture-result', handleCaptureResult)
})
</script>

<template>
  <div class="dashboard-layout">
    <!-- 顶部 -->
    <DashboardHeader @open-modal="openModal" />
    
    <!-- 主体内容 -->
    <div class="main-content">
      <!-- 左侧边栏 -->
      <div class="sidebar left-sidebar">
        <div class="panel-wrapper h-40">
          <LeftTopPanel />
        </div>
        <div class="panel-wrapper h-60">
          <LeftBottomPanel />
        </div>
      </div>
      
      <!-- 中央3D场景 -->
      <div class="center-stage">
        <DigitalTwinScene />
      </div>
      
      <!-- 右侧边栏 -->
      <div class="sidebar right-sidebar">
        <div class="panel-wrapper h-45">
          <RightTopPanel />
        </div>
        <div class="panel-wrapper h-55">
          <RightBottomPanel />
        </div>
      </div>
    </div>

    <!-- 模态框挂载点 -->
    <CameraSettingsModal 
        :visible="modals.camera" 
        @close="closeModal('camera')" 
    />
    <SystemSettingsModal 
        :visible="modals.settings" 
        @close="closeModal('settings')" 
    />
    <AutomationModal 
        :visible="modals.automation" 
        @close="closeModal('automation')" 
    />
    <AiAssistantModal 
        ref="aiModalRef"
        :visible="modals.ai" 
        @close="closeModal('ai')" 
    />
    <PhotoPreviewModal
        :visible="modals.preview"
        :image-url="previewUrl"
        :image-file="previewFile"
        @close="closeModal('preview')"
        @analyze="handleAnalyze"
    />
  </div>
</template>

<style scoped>
.dashboard-layout {
  width: 100vw;
  height: 100vh;
  background: #050a14;
  background-image: 
    linear-gradient(rgba(0, 242, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 242, 255, 0.03) 1px, transparent 1px),
    radial-gradient(circle at 50% 50%, rgba(20, 40, 60, 0.5) 0%, rgba(0, 5, 10, 1) 100%);
  background-size: 40px 40px, 40px 40px, 100% 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.main-content {
  flex: 1;
  display: flex;
  position: relative;
  overflow: hidden;
  padding: 10px;
  gap: 10px;
}

.sidebar {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 10;
}

.panel-wrapper {
  background: rgba(16, 30, 60, 0.4);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-top: 2px solid rgba(0, 242, 255, 0.3);
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.panel-wrapper:hover {
  border-top-color: var(--theme-primary);
  box-shadow: 0 8px 24px rgba(0, 242, 255, 0.15);
  transform: translateY(-2px);
}

.h-40 { height: 40%; }
.h-45 { height: 50%; }
.h-55 { height: 50%; }
.h-60 { height: 60%; }

.center-stage {
  flex: 1;
  position: relative;
  background: radial-gradient(circle at center, #112 0%, #000 100%);
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-top: 2px solid rgba(0, 242, 255, 0.3);
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.5);
}
</style>
