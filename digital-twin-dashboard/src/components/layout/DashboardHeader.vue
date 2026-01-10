<script setup>
/**
 * 顶部信息栏
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { useDeviceStore } from '@/stores/deviceStore'
import { useApi } from '@/composables/useApi'
import { BASE_URL } from '@/config/api'

// 定义事件
const emit = defineEmits(['open-modal'])

const store = useDeviceStore()

// 时间日期
const timeStr = ref('')
const dateStr = ref('')
const weekStr = ref('')
let timer = null

// 一言
const hitokoto = ref('加载中...')

function updateTime() {
    const now = new Date()
    timeStr.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    dateStr.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
    const weeks = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    weekStr.value = weeks[now.getDay()]
}

async function fetchHitokoto() {
    try {
        const res = await fetch('https://v1.hitokoto.cn/?c=i')
        const data = await res.json()
        hitokoto.value = data.hitokoto
    } catch (e) {
        hitokoto.value = '科技是第一生产力'
    }
}

onMounted(() => {
    updateTime()
    timer = setInterval(updateTime, 1000)
    fetchHitokoto()
})

onUnmounted(() => {
    if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="dashboard-header">
    <div class="header-left">
        <div class="logo">
            <span class="logo-icon">🛸</span>
            <span class="logo-text">ESP32-CAM 数字孪生</span>
        </div>
        <div class="hitokoto">「 {{ hitokoto }} 」</div>
    </div>
    
    <div class="header-center">
        <div class="time-box">
            <span class="date">{{ dateStr }} {{ weekStr }}</span>
            <span class="time">{{ timeStr }}</span>
        </div>
    </div>
    
    <div class="header-right">
        <!-- 功能按钮组 -->
        <div class="action-buttons">
            <button class="nav-btn" @click="$emit('open-modal', 'camera')">🎨 画质</button>
            <button class="nav-btn" @click="$emit('open-modal', 'ai')">🤖 AI助手</button>
            <button class="nav-btn" @click="$emit('open-modal', 'automation')">⚡ 自动化</button>
            <button class="nav-btn" @click="$emit('open-modal', 'settings')">⚙️ 设置</button>
        </div>

        <div class="sse-status" :class="{ online: store.sseConnected }">
            <span class="status-dot"></span>
            <span class="status-text">{{ store.sseConnected ? '配置同步' : '连接中断' }}</span>
        </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-header {
    height: 60px;
    background: rgba(10, 20, 40, 0.8);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid rgba(0, 242, 255, 0.2);
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px;
    color: white;
}

.header-left {
    display: flex;
    align-items: center;
    gap: 20px;
    flex: 1;
}

.logo {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 20px;
    font-weight: bold;
    color: var(--theme-primary);
    text-shadow: 0 0 10px rgba(0, 242, 255, 0.5);
}

.hitokoto {
    font-size: 12px;
    color: var(--text-secondary);
    max-width: 300px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
}

.header-center {
    flex: 1;
    display: flex;
    justify-content: center;
}

.time-box {
    text-align: center;
}

.date {
    display: block;
    font-size: 12px;
    color: var(--text-secondary);
}

.time {
    font-size: 24px;
    font-family: 'Courier New', monospace;
    font-weight: bold;
    color: white;
    text-shadow: 0 0 10px rgba(255, 255, 255, 0.5);
}

.header-right {
    flex: 1;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 20px;
}

.action-buttons {
    display: flex;
    gap: 10px;
}

.nav-btn {
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.2);
    color: white;
    padding: 6px 12px;
    border-radius: 4px;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.2s;
}

.nav-btn:hover {
    background: var(--theme-primary);
    color: #000;
    box-shadow: 0 0 10px rgba(0, 242, 255, 0.5);
}

.sse-status {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 12px;
    background: rgba(0, 0, 0, 0.3);
    border-radius: 20px;
    border: 1px solid rgba(244, 67, 54, 0.5);
}

.sse-status.online {
    border-color: rgba(76, 175, 80, 0.5);
}

.status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #f44336;
    box-shadow: 0 0 5px #f44336;
}

.sse-status.online .status-dot {
    background: #4caf50;
    box-shadow: 0 0 5px #4caf50;
}

.status-text {
    font-size: 12px;
    color: #f44336;
}

.sse-status.online .status-text {
    color: #4caf50;
}
</style>
