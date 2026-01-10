<script setup>
/**
 * 左上面板 - 数据监控和图表（温湿度+设备状态切换）
 */
import { computed, ref, watch } from 'vue'
import { useDeviceStore } from '@/stores/deviceStore'
import { BASE_URL } from '@/config/api'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const store = useDeviceStore()

// Tab切换: dht温湿度 / status设备状态
const activeTab = ref('dht')

// Tab切换: dht温湿度 / status设备状态
const activeTab = ref('dht')

// 温湿度图表
const dhtChartOption = computed(() => ({
  tooltip: { trigger: 'axis', backgroundColor: 'rgba(10, 20, 40, 0.9)', borderColor: 'rgba(0, 242, 255, 0.3)', textStyle: { color: '#fff', fontSize: 11 } },
  legend: { data: ['温度', '湿度'], textStyle: { color: '#94a3b8', fontSize: 10 }, top: 0, itemWidth: 12, itemHeight: 8 },
  grid: { left: 35, right: 10, top: 28, bottom: 20 },
  xAxis: { type: 'category', data: store.dhtHistory.labels, axisLine: { lineStyle: { color: '#334155' } }, axisLabel: { color: '#64748b', fontSize: 8, rotate: 30 }, splitLine: { show: false } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#1e293b' } }, axisLabel: { color: '#64748b', fontSize: 9 } },
  series: [
    { name: '温度', type: 'line', data: store.dhtHistory.temperatures, smooth: true, lineStyle: { color: '#ff6b6b', width: 2 }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(255, 107, 107, 0.3)' }, { offset: 1, color: 'rgba(255, 107, 107, 0)' }] } }, symbol: 'none' },
    { name: '湿度', type: 'line', data: store.dhtHistory.humidities, smooth: true, lineStyle: { color: '#74b9ff', width: 2 }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(116, 185, 255, 0.3)' }, { offset: 1, color: 'rgba(116, 185, 255, 0)' }] } }, symbol: 'none' }
  ]
}))

// 设备状态图表
const statusChartOption = computed(() => ({
  tooltip: { trigger: 'axis', backgroundColor: 'rgba(10, 20, 60, 0.9)', borderColor: '#00f2ff', textStyle: { color: '#fff' } },
  legend: { data: ['WiFi(dBm)', '内存(KB)'], textStyle: { color: '#999', fontSize: 10 }, top: 0 },
  grid: { left: 40, right: 40, top: 28, bottom: 20 },
  xAxis: { type: 'category', data: store.statusHistory.labels, axisLabel: { color: '#666', fontSize: 8, rotate: 30 } },
  yAxis: [
    { type: 'value', name: 'dBm', max: 0, min: -100, splitLine: { show: false }, axisLabel: { color: '#666', fontSize: 9 } },
    { type: 'value', name: 'KB', splitLine: { lineStyle: { color: '#222' } }, axisLabel: { color: '#666', fontSize: 9 } }
  ],
  series: [
    { name: 'WiFi(dBm)', type: 'line', data: store.statusHistory.rssiData, smooth: true, lineStyle: { color: '#f093fb', width: 2 }, areaStyle: { opacity: 0.1 }, symbol: 'none' },
    { name: '内存(KB)', type: 'line', yAxisIndex: 1, data: store.statusHistory.freeHeapData, smooth: true, lineStyle: { color: '#38ef7d', width: 2 }, areaStyle: { opacity: 0.1 }, symbol: 'none' }
  ]
}))

function formatUptime(seconds) {
  if (!seconds) return '--'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}分`
}
</script>

<template>
  <div class="left-top-panel tech-panel">
    <!-- Tab切换 -->
    <div class="panel-tabs">
      <div class="tab-item" :class="{ active: activeTab === 'dht' }" @click="activeTab = 'dht'">🌡️ 温湿度</div>
      <div class="tab-item" :class="{ active: activeTab === 'status' }" @click="activeTab = 'status'">📶 设备状态</div>
    </div>
    
    <div class="panel-content">
      <!-- 温湿度视图 -->
      <template v-if="activeTab === 'dht'">
        <!-- 数据卡片 -->
        <div class="data-cards">
          <div class="data-card temp">
            <div class="card-icon">🌡️</div>
            <div class="card-value">{{ store.dhtData.temperature.toFixed(1) }}</div>
            <div class="card-unit">°C</div>
            <div class="card-label">温度</div>
          </div>
          <div class="data-card humid">
            <div class="card-icon">💧</div>
            <div class="card-value">{{ store.dhtData.humidity.toFixed(1) }}</div>
            <div class="card-unit">%</div>
            <div class="card-label">湿度</div>
          </div>
          <div class="data-card light" :class="{ dark: store.dhtData.lightDark }">
            <div class="card-icon">{{ store.dhtData.lightDark ? '🌙' : '☀️' }}</div>
            <div class="card-value">{{ store.dhtData.lightDark ? '暗' : '亮' }}</div>
            <div class="card-label">光照</div>
          </div>
        </div>
        
        <!-- 温湿度图表 -->
        <div class="chart-container">
          <VChart :option="dhtChartOption" autoresize />
        </div>
      </template>
      
      <!-- 设备状态视图 -->
      <template v-else>
        <!-- 状态卡片 -->
        <div class="data-cards">
          <div class="data-card signal" :class="{ weak: store.deviceStatus.rssi < -70 }">
            <div class="card-icon">📶</div>
            <div class="card-value">{{ store.deviceStatus.rssi }}</div>
            <div class="card-unit">dBm</div>
            <div class="card-label">WiFi信号</div>
          </div>
          <div class="data-card memory" :class="{ low: store.deviceStatus.freeHeap < 50000 }">
            <div class="card-icon">💾</div>
            <div class="card-value">{{ (store.deviceStatus.freeHeap / 1024).toFixed(0) }}</div>
            <div class="card-unit">KB</div>
            <div class="card-label">可用内存</div>
          </div>
          <div class="data-card uptime">
            <div class="card-icon">⏱️</div>
            <div class="card-value">{{ formatUptime(store.deviceStatus.uptime) }}</div>
            <div class="card-label">运行时间</div>
          </div>
        </div>
        
        <!-- 设备状态图表 -->
        <div class="chart-container">
          <VChart :option="statusChartOption" autoresize />
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.left-top-panel { display: flex; flex-direction: column; height: 100%; }

.panel-tabs {
  display: flex;
  background: rgba(0, 0, 0, 0.3);
  padding: 4px;
  border-radius: 6px;
  margin-bottom: 10px;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 6px 0;
  font-size: 11px;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s;
  border: 1px solid transparent;
}

.tab-item:hover { color: white; }

.tab-item.active {
  color: white;
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.1);
  font-weight: bold;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}

.panel-content { flex: 1; padding: 10px; display: flex; flex-direction: column; gap: 10px; overflow: hidden; }
.data-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.data-card { background: var(--bg-card); border-radius: var(--radius-md); padding: 10px 8px; text-align: center; position: relative; }
.data-card.temp { border-top: 3px solid #ff6b6b; }
.data-card.humid { border-top: 3px solid #74b9ff; }
.data-card.light { border-top: 3px solid #ffd700; }
.data-card.light.dark { border-top-color: #6c5ce7; background: rgba(108, 92, 231, 0.1); }
.data-card.signal { border-top: 3px solid #f093fb; }
.data-card.signal.weak { border-top-color: #fdcb6e; background: rgba(253, 203, 110, 0.1); }
.data-card.memory { border-top: 3px solid #38ef7d; }
.data-card.memory.low { border-top-color: #e74c3c; background: rgba(231, 76, 60, 0.1); }
.data-card.uptime { border-top: 3px solid #00cec9; }
.card-icon { font-size: 16px; margin-bottom: 2px; }
.card-value { font-size: 20px; font-weight: bold; color: var(--text-primary); line-height: 1.2; }
.card-unit { font-size: 10px; color: var(--text-muted); }
.card-label { font-size: 10px; color: var(--text-secondary); margin-top: 2px; }
.chart-container { flex: 1; min-height: 80px; }

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: linear-gradient(135deg, #1a2a4a 0%, #0d1a2d 100%);
  border: 1px solid rgba(0, 242, 255, 0.3);
  border-radius: 12px;
  width: 320px;
  box-shadow: 0 0 30px rgba(0, 242, 255, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  color: var(--theme-primary);
  font-weight: bold;
}

.close-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 20px;
  cursor: pointer;
}

.close-btn:hover { color: white; }

.modal-body {
  padding: 20px;
}

.interval-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.interval-option {
  padding: 12px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: 0.2s;
}

.interval-option:hover {
  background: rgba(0, 242, 255, 0.1);
  border-color: var(--theme-primary);
  color: white;
}

.interval-option.active {
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary));
  color: #000;
  border-color: transparent;
  font-weight: bold;
}

.current-interval {
  margin-top: 15px;
  text-align: center;
  color: var(--text-muted);
  font-size: 12px;
}

.current-interval strong {
  color: var(--theme-primary);
}

.modal-footer {
  display: flex;
  gap: 10px;
  padding: 15px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.btn-cancel, .btn-confirm {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: 0.2s;
}

.btn-cancel {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-secondary);
}

.btn-cancel:hover {
  background: rgba(255, 255, 255, 0.2);
}

.btn-confirm {
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary));
  color: #000;
  font-weight: bold;
}

.btn-confirm:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(0, 242, 255, 0.3);
}
</style>
