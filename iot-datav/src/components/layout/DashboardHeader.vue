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
const weatherIcon = ref('🌤️')
const weatherTemp = ref('--°C')
let timer = null
let weatherTimer = null

// 一言
const hitokoto = ref('加载中...')

function updateTime() {
    const now = new Date()
    timeStr.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    dateStr.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
    const weeks = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    weekStr.value = weeks[now.getDay()]
}

async function fetchWeather() {
    try {
        // 使用 wttr.in 获取简洁天气信息 (JSON格式不稳定，使用文本格式解析)
        // format=%c+%t => 🌤️ +25°C
        const res = await fetch('https://wttr.in/?format=%c+%t')
        if (res.ok) {
            const text = await res.text()
            // 解析: "🌤️ +25°C" (处理可能的多个空格)
            const parts = text.trim().split(/\s+/)
            if (parts.length >= 2) {
                weatherIcon.value = parts[0]
                weatherTemp.value = parts[1].replace('+', '') // 去掉正号
            }
        }
    } catch (e) {
        console.error('天气获取失败:', e)
        weatherIcon.value = '🌤️'
        weatherTemp.value = 'N/A'
    }
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
    fetchWeather()
    // 每30分钟刷新一次天气
    weatherTimer = setInterval(fetchWeather, 30 * 60 * 1000)
})

onUnmounted(() => {
    if (timer) clearInterval(timer)
    if (weatherTimer) clearInterval(weatherTimer)
})
</script>

<template>
  <div class="dashboard-header">
    <div class="header-decoration-line"></div>
    
    <div class="header-left">
        <div class="logo">
            <div class="weather-widget" title="当地天气">
                <span class="weather-icon">{{ weatherIcon }}</span>
                <span class="weather-temp">{{ weatherTemp }}</span>
            </div>
            <div class="logo-text">
                <span class="brand">ESP32-CAM</span>
                <span class="subtitle">DIGITAL TWIN</span>
            </div>
        </div>
        <div class="hitokoto">「 {{ hitokoto }} 」</div>
    </div>
    
    <div class="header-center">
        <div class="time-box">
            <div class="date-row">
                <span class="week">{{ weekStr }}</span>
                <span class="date">{{ dateStr }}</span>
            </div>
            <span class="time">{{ timeStr }}</span>
        </div>
    </div>
    
    <div class="header-right">
        <!-- Dock栏风格功能钮 -->
        <div class="control-dock">
            <button class="dock-btn" @click="$emit('open-modal', 'camera')" title="画质参数">
                <span class="dock-icon">🎨</span>
                <span class="dock-label">画质</span>
            </button>
            <button class="dock-btn" @click="$emit('open-modal', 'ai')" title="AI助手">
                <span class="dock-icon">🤖</span>
                <span class="dock-label">助手</span>
            </button>
            <button class="dock-btn" @click="$emit('open-modal', 'automation')" title="自动化">
                <span class="dock-icon">⚡</span>
                <span class="dock-label">联动</span>
            </button>
            <div class="dock-divider"></div>
            <button class="dock-btn settings" @click="$emit('open-modal', 'settings')" title="系统设置">
                <span class="dock-icon">⚙️</span>
            </button>
        </div>

        <div class="sse-status" :class="{ online: store.sseConnected }">
            <div class="status-indicator"></div>
            <span class="status-text">{{ store.sseConnected ? 'ONLINE' : 'OFFLINE' }}</span>
        </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-header {
    height: 70px;
    background: linear-gradient(180deg, rgba(0, 10, 20, 0.9) 0%, rgba(0, 10, 20, 0.4) 100%);
    backdrop-filter: blur(12px);
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 24px;
    position: relative;
    z-index: 100;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
}

.header-decoration-line {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, 
        transparent 0%, 
        var(--theme-primary) 20%, 
        var(--theme-secondary) 80%, 
        transparent 100%
    );
    opacity: 0.5;
}

.header-left {
    display: flex;
    align-items: center;
    gap: 30px;
    flex: 1;
}

.logo {
    display: flex;
    align-items: center;
    gap: 12px;
}

.weather-widget {
    background: rgba(0, 242, 255, 0.1);
    border: 1px solid rgba(0, 242, 255, 0.3);
    border-radius: 20px;
    padding: 0 16px;
    display: flex;
    align-items: center;
    gap: 12px;
    cursor: help;
    box-shadow: 0 0 15px rgba(0, 242, 255, 0.1);
    transition: all 0.3s;
    height: 36px;
}

.weather-widget:hover {
    background: rgba(0, 242, 255, 0.2);
    box-shadow: 0 0 20px rgba(0, 242, 255, 0.3);
    transform: translateY(-1px);
}

.weather-icon { font-size: 20px; }
.weather-temp { 
    font-size: 15px; 
    color: white; 
    font-weight: 600; 
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    letter-spacing: 0.5px;
}

.logo-text {
    display: flex;
    flex-direction: column;
    justify-content: center;
    line-height: 1.2;
}

.brand {
    font-size: 18px;
    font-weight: 800;
    letter-spacing: 1px;
    color: white;
    font-family: 'Inter', system-ui, sans-serif;
}

.subtitle {
    font-size: 10px;
    color: var(--theme-primary);
    letter-spacing: 3px;
    text-transform: uppercase;
    font-weight: 600;
}

.hitokoto {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.6);
    padding-left: 24px;
    border-left: 1px solid rgba(255, 255, 255, 0.15);
    max-width: 280px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    font-family: 'Inter', system-ui, sans-serif;
    letter-spacing: 0.5px;
    font-style: italic;
}

.header-center {
    flex: 1;
    display: flex;
    justify-content: center;
}

.time-box {
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
}

.date-row {
    display: flex;
    gap: 12px;
    font-size: 12px;
    color: rgba(255, 255, 255, 0.7);
    letter-spacing: 1px;
    font-family: 'Inter', system-ui, sans-serif;
    font-weight: 500;
}

.time {
    font-size: 32px;
    font-weight: 700;
    color: white;
    letter-spacing: 2px;
    text-shadow: 0 0 15px rgba(255, 255, 255, 0.2);
    font-variant-numeric: tabular-nums;
    font-family: 'Inter', system-ui, sans-serif; /* 更加现代的字体 */
    line-height: 1;
}

.header-right {
    flex: 1;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 30px;
}

/* 核心Dock栏样式 */
.control-dock {
    display: flex;
    align-items: center;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 100px;
    padding: 0 8px;
    gap: 6px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
    height: 44px;
}

.dock-btn {
    background: transparent;
    border: none;
    color: rgba(255, 255, 255, 0.7);
    padding: 0 16px;
    height: 32px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    overflow: hidden;
}

.dock-btn::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    background: rgba(0, 242, 255, 0.1);
    border-radius: 50%;
    transform: translate(-50%, -50%);
    transition: width 0.3s, height 0.3s;
}

.dock-btn:hover::before {
    width: 150px;
    height: 150px;
}

.dock-btn:hover {
    color: white;
    text-shadow: 0 0 8px rgba(255, 255, 255, 0.6);
}

.dock-btn.settings {
    padding: 0 10px;
}

.dock-btn:active {
    transform: scale(0.95);
}

.dock-icon { font-size: 18px; }
.dock-label { 
    font-size: 13px; 
    font-weight: 600; 
    font-family: 'Inter', system-ui, sans-serif;
}

.dock-divider {
    width: 1px;
    height: 18px;
    background: rgba(255, 255, 255, 0.15);
    margin: 0 6px;
}

.sse-status {
    display: flex;
    align-items: center;
    gap: 8px;
    background: rgba(0, 0, 0, 0.4);
    padding: 6px 14px;
    border-radius: 20px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(4px);
}


.status-indicator {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #333;
    box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.05);
    transition: all 0.3s;
}

.sse-status.online .status-indicator {
    background: var(--theme-success);
    box-shadow: 0 0 8px var(--theme-success);
}

.status-text {
    font-size: 10px;
    font-family: monospace;
    opacity: 0.5;
    letter-spacing: 1px;
}

.sse-status.online .status-text {
    color: var(--theme-success);
    opacity: 1;
}
</style>
