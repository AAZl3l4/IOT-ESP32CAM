<script setup>
/**
 * 右下面板 - 操作日志
 */
import { useDeviceStore } from '@/stores/deviceStore'

const store = useDeviceStore()
</script>

<template>
  <div class="right-bottom-panel tech-panel">
    <div class="panel-title"><span>📝</span> 操作日志</div>

    <!-- 日志列表 -->
    <div class="logs-container custom-scrollbar">
      <div v-for="(log, index) in store.logs" :key="index" class="log-item">
        <div class="log-time">{{ log.time || '--:--:--' }}</div>
        <div class="log-content">
          <div class="log-desc">{{ log.operationDesc }}</div>
          <div class="log-result" :class="log.result">
            {{ log.result === 'success' ? '✓ 成功' : log.result === 'pending' ? '⏳ 处理中' : '✗ 失败' }}
            <span v-if="log.resultMsg" class="log-msg">({{ log.resultMsg }})</span>
          </div>
        </div>
      </div>
      <div v-if="store.logs.length === 0" class="empty-log">暂无日志</div>
    </div>
  </div>
</template>

<style scoped>
.right-bottom-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.logs-container {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.log-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 11px;
  padding-bottom: 8px;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.1);
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn { from { opacity: 0; transform: translateY(-5px); } to { opacity: 1; transform: translateY(0); } }

.log-time {
  color: var(--text-muted);
  font-family: monospace;
  min-width: 50px;
}

.log-content {
  flex: 1;
}

.log-desc {
  color: var(--text-secondary);
  margin-bottom: 2px;
}

.log-result {
  font-weight: bold;
}

.log-result.success { color: var(--theme-success); }
.log-result.failed { color: var(--theme-danger); }
.log-result.pending { color: var(--theme-warning); }

.log-msg {
    font-weight: normal;
    opacity: 0.8;
    margin-left: 4px;
}

.empty-log {
  text-align: center;
  color: var(--text-muted);
  margin-top: 20px;
  font-size: 12px;
}
</style>
