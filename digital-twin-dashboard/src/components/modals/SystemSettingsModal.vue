<script setup>
/**
 * 系统设置弹窗 - 数据从store.deviceConfig读取，由SSE config事件更新
 */
import { ref, computed } from 'vue'
import { useDeviceStore } from '@/stores/deviceStore'
import { BASE_URL } from '@/config/api'
import ModalDialog from '@/components/common/ModalDialog.vue'

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['close'])
const store = useDeviceStore()

// 直接绑定store配置（双向绑定用于显示，修改时发送API）
const wifiSsid = computed(() => store.deviceConfig.wifiSsid || '')
const wifiPassword = computed(() => store.deviceConfig.wifiPassword || '')
const mqttBroker = computed(() => store.deviceConfig.mqttBroker || '')
const mqttPort = computed(() => store.deviceConfig.mqttPort || 1883)
const uploadUrlVal = computed(() => store.deviceConfig.uploadUrl || '')
const statusIntervalVal = computed(() => store.deviceConfig.statusInterval || 30000)

// 表单本地状态（用于编辑）
const wifiForm = ref({ ssid: '', password: '' })
const mqttForm = ref({ broker: '', port: 1883 })
const uploadUrl = ref('')
const reportInterval = ref(30000)

// 每次打开弹窗时从store同步到本地表单
import { watch } from 'vue'
watch(() => props.visible, (val) => {
    if (val) {
        wifiForm.value.ssid = store.deviceConfig.wifiSsid || ''
        wifiForm.value.password = store.deviceConfig.wifiPassword || ''
        mqttForm.value.broker = store.deviceConfig.mqttBroker || ''
        mqttForm.value.port = store.deviceConfig.mqttPort || 1883
        uploadUrl.value = store.deviceConfig.uploadUrl || ''
        reportInterval.value = store.deviceConfig.statusInterval || 30000
    }
})

async function apiCall(url, method = 'POST', body = null) {
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

async function setWiFi() {
    if (!wifiForm.value.ssid || !wifiForm.value.password) return alert('请输入完整WiFi信息')
    if (!confirm('⚠️ 危险操作！\n如果密码错误设备将无法连接网络。\n确认修改WiFi配置吗？')) return

    const res = await apiCall(`${BASE_URL}/mqtt/config/wifi/${store.clientId}`, 'POST', {
        ssid: wifiForm.value.ssid,
        password: wifiForm.value.password
    })
    if (res.code === 0) alert('WiFi配置指令已发送，设备重启后生效')
}

async function setMQTT() {
    if (!mqttForm.value.broker) return alert('请输入Broker地址')
    if (!confirm('⚠️ 修改MQTT配置将导致设备重启。\n确认继续？')) return

    const res = await apiCall(`${BASE_URL}/mqtt/config/mqtt/${store.clientId}`, 'POST', {
        server: mqttForm.value.broker,
        port: parseInt(mqttForm.value.port)
    })
    if (res.code === 0) alert('MQTT配置已发送')
}

async function setUploadUrl() {
    if (!uploadUrl.value) return alert('请输入URL')
    const res = await apiCall(`${BASE_URL}/mqtt/config/upload-url/${store.clientId}`, 'POST', {
        url: uploadUrl.value
    })
    if (res.code === 0) alert('上传URL已更新')
}

async function setStatusInterval() {
    const res = await apiCall(`${BASE_URL}/mqtt/cam/${store.clientId}/set_status_interval`, 'POST', {
        interval: reportInterval.value
    })
    if (res.code === 0) alert('上报间隔已更新')
}

async function refreshConfig() {
    await apiCall(`${BASE_URL}/mqtt/cam/${store.clientId}/get_config`, 'POST')
    alert('请求刷新配置指令已发送')
}

async function resetConfig() {
    if (!confirm('⚠️ 确认恢复出厂设置？所有配置将丢失。')) return
    await apiCall(`${BASE_URL}/mqtt/config/reset/${store.clientId}`, 'POST')
}

const showPassword = ref(false)
</script>

<template>
  <ModalDialog :visible="visible" title="🔧 系统配置管理" @close="$emit('close')">
    <div class="settings-content">
      
      <!-- WiFi设置 -->
      <div class="section warning">
        <h3>📶 WiFi 设置</h3>
        <div class="form-group">
            <label>SSID</label>
            <input type="text" v-model="wifiForm.ssid" placeholder="WiFi名称">
        </div>
        <div class="form-group">
            <label>密码</label>
            <div class="password-group">
                <input :type="showPassword ? 'text' : 'password'" v-model="wifiForm.password" placeholder="WiFi密码">
                <button class="icon-btn" @click="showPassword=!showPassword">{{ showPassword ? '🙈' : '👁️' }}</button>
            </div>
        </div>
        <button class="btn btn-warning full-width" @click="setWiFi">⚠️ 设置WiFi (重启)</button>
      </div>

      <!-- MQTT设置 -->
      <div class="section warning">
        <h3>📡 MQTT 设置</h3>
        <div class="form-group">
            <label>Broker</label>
            <input type="text" v-model="mqttForm.broker" placeholder="broker.emqx.io">
        </div>
        <div class="form-group">
            <label>Port</label>
            <input type="number" v-model="mqttForm.port" placeholder="1883">
        </div>
        <button class="btn btn-warning full-width" @click="setMQTT">⚠️ 设置MQTT (重启)</button>
      </div>

      <!-- 其他设置 -->
      <div class="section">
        <h3>⚙️ 常规设置</h3>
        <div class="form-group">
            <label>图片上传URL</label>
            <div class="action-input">
                <input type="text" v-model="uploadUrl">
                <button class="btn btn-small" @click="setUploadUrl">保存</button>
            </div>
        </div>
        
        <div class="form-group">
            <label>状态上报间隔</label>
            <div class="action-input">
                <select v-model="reportInterval">
                    <option :value="10000">10秒</option>
                    <option :value="30000">30秒</option>
                    <option :value="60000">60秒</option>
                    <option :value="300000">5分钟</option>
                </select>
                <button class="btn btn-small" @click="setStatusInterval">设置</button>
            </div>
        </div>
      </div>

      <!-- 维护 -->
      <div class="footer-actions">
        <button class="btn btn-primary" @click="refreshConfig">🔄 刷新设备配置</button>
        <button class="btn btn-danger" @click="resetConfig">⚠️ 恢复出厂设置</button>
      </div>

    </div>
  </ModalDialog>
</template>

<style scoped>
.settings-content {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.section {
    background: rgba(0,0,0,0.2);
    padding: 15px;
    border-radius: 8px;
    border-left: 3px solid var(--theme-primary);
}

.section.warning {
    border-left-color: var(--theme-warning);
    background: rgba(255, 152, 0, 0.05);
}

h3 {
    margin: 0 0 15px 0;
    font-size: 14px;
    color: var(--text-primary);
}

.form-group {
    margin-bottom: 12px;
}

.form-group label {
    display: block;
    font-size: 12px;
    color: var(--text-secondary);
    margin-bottom: 5px;
}

input, select {
    width: 100%;
    padding: 8px;
    background: rgba(0,0,0,0.3);
    border: 1px solid rgba(255,255,255,0.1);
    color: white;
    border-radius: 4px;
}

.password-group {
    position: relative;
    display: flex;
}

.icon-btn {
    position: absolute;
    right: 8px;
    top: 50%;
    transform: translateY(-50%);
    background: none;
    border: none;
    cursor: pointer;
    font-size: 16px;
}

.full-width {
    width: 100%;
    margin-top: 5px;
}

.action-input {
    display: flex;
    gap: 8px;
}

.btn {
    padding: 8px 15px;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 12px;
    transition: 0.2s;
}

.btn:active { transform: scale(0.98); }

.btn-warning { background: var(--theme-warning); color: #000; }
.btn-primary { background: var(--theme-primary); color: #000; }
.btn-danger { background: var(--theme-danger); color: white; }
.btn-small { padding: 4px 10px; background: #444; color: #ccc; }

.footer-actions {
    display: flex;
    justify-content: space-between;
    margin-top: 10px;
    padding-top: 15px;
    border-top: 1px solid rgba(255,255,255,0.1);
}
</style>
