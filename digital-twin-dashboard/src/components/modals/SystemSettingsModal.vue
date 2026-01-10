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
const dhtIntervalVal = computed(() => store.deviceConfig.dhtInterval || 5000)

// 表单本地状态（用于编辑）
const wifiForm = ref({ ssid: '', password: '' })
const mqttForm = ref({ broker: '', port: 1883 })
const uploadUrl = ref('')
const reportInterval = ref(30000)
const dhtInterval = ref(5000)

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
        dhtInterval.value = store.deviceConfig.dhtInterval || 5000
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

async function setDhtInterval() {
    const res = await apiCall(`${BASE_URL}/mqtt/dht-interval/${store.clientId}`, 'POST', {
        interval: dhtInterval.value
    })
    if (res.code === 0) alert('DHT采集间隔已更新')
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
  <ModalDialog :visible="visible" title="🔧 系统设置" @close="$emit('close')">
    <div class="settings-container custom-scrollbar">
      <div class="settings-grid">
        <!-- 左侧：网络连接 -->
        <div class="settings-col">
            <div class="section-title">📡 网络连接</div>
            
            <!-- WiFi设置 -->
            <div class="setting-card warning-card">
                <div class="card-header">WiFi 配置</div>
                <div class="form-group">
                    <label>SSID (名称)</label>
                    <input type="text" v-model="wifiForm.ssid" placeholder="请输入WiFi名称">
                </div>
                <div class="form-group">
                    <label>Password (密码)</label>
                    <div class="password-group">
                        <input :type="showPassword ? 'text' : 'password'" v-model="wifiForm.password" placeholder="请输入WiFi密码">
                        <button class="eye-btn" @click="showPassword=!showPassword">{{ showPassword ? '🙈' : '👁️' }}</button>
                    </div>
                </div>
                <button class="btn btn-warning full-width" @click="setWiFi">保存并重启 WiFi</button>
            </div>

            <!-- MQTT设置 -->
            <div class="setting-card warning-card">
                <div class="card-header">MQTT 服务</div>
                <div class="form-group">
                    <label>Broker Address</label>
                    <input type="text" v-model="mqttForm.broker" placeholder="broker.emqx.io">
                </div>
                <div class="form-group">
                    <label>Broker Port</label>
                    <input type="number" v-model="mqttForm.port" placeholder="1883">
                </div>
                <button class="btn btn-warning full-width" @click="setMQTT">保存并重启 MQTT</button>
            </div>
        </div>

        <!-- 右侧：数据与维护 -->
        <div class="settings-col">
            <div class="section-title">📊 数据与上报</div>
            
            <div class="setting-card">
                <div class="card-header">数据采集频率</div>
                
                <!-- DHT采集间隔 -->
                <div class="form-row">
                    <div class="row-label">
                        <span>DHT温湿度采集</span>
                        <small>传感器读取频率</small>
                    </div>
                    <div class="row-action">
                        <select v-model="dhtInterval">
                            <option :value="1000">1秒</option>
                            <option :value="2000">2秒</option>
                            <option :value="5000">5秒</option>
                            <option :value="10000">10秒</option>
                            <option :value="30000">30秒</option>
                            <option :value="60000">60秒</option>
                        </select>
                        <button class="btn-icon" @click="setDhtInterval" title="保存">💾</button>
                    </div>
                </div>

                <!-- 状态上报间隔 -->
                <div class="form-row">
                    <div class="row-label">
                        <span>设备状态上报</span>
                        <small>Heartbeat频率</small>
                    </div>
                    <div class="row-action">
                        <select v-model="reportInterval">
                            <option :value="5000">5秒</option>
                            <option :value="10000">10秒</option>
                            <option :value="30000">30秒</option>
                            <option :value="60000">60秒</option>
                            <option :value="300000">5分钟</option>
                        </select>
                        <button class="btn-icon" @click="setStatusInterval" title="保存">💾</button>
                    </div>
                </div>
            </div>

            <div class="setting-card">
                <div class="card-header">图片上传服务</div>
                <div class="form-group">
                    <label>Upload URL</label>
                    <div class="input-with-btn">
                        <input type="text" v-model="uploadUrl" placeholder="http://...">
                        <button class="btn-icon" @click="setUploadUrl">💾</button>
                    </div>
                </div>
            </div>

            <div class="section-title" style="margin-top:20px;">🛡️ 系统维护</div>
            <div class="maintenance-actions">
                <button class="btn btn-primary" @click="refreshConfig">
                    <span class="btn-icon">🔄</span> 刷新配置
                </button>
                <button class="btn btn-danger" @click="resetConfig">
                    <span class="btn-icon">⚠️</span> 恢复出厂
                </button>
            </div>
        </div>
      </div>
    </div>
  </ModalDialog>
</template>

<style scoped>
.settings-container {
    max-height: 70vh;
    overflow-y: auto;
    padding: 10px;
}

.settings-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24px;
}

@media (max-width: 768px) {
    .settings-grid { grid-template-columns: 1fr; }
}

.settings-col {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.section-title {
    font-size: 13px;
    color: var(--theme-primary);
    font-weight: bold;
    letter-spacing: 1px;
    margin-bottom: 4px;
    padding-left: 8px;
    border-left: 3px solid var(--theme-primary);
}

.setting-card {
    background: rgba(255, 255, 255, 0.03);
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 12px;
    padding: 16px;
    transition: all 0.3s;
}

.setting-card:hover {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.15);
}

.warning-card {
    border-left: 3px solid var(--theme-warning);
    background: linear-gradient(90deg, rgba(255, 152, 0, 0.05) 0%, rgba(255, 255, 255, 0.02) 100%);
}

.card-header {
    font-size: 14px;
    color: white;
    font-weight: 500;
    margin-bottom: 12px;
    opacity: 0.9;
}

.form-group {
    margin-bottom: 12px;
}

.form-group label {
    display: block;
    font-size: 11px;
    color: var(--text-secondary);
    margin-bottom: 4px;
}

input, select {
    width: 100%;
    padding: 8px 12px;
    background: rgba(0, 0, 0, 0.4);
    border: 1px solid rgba(255, 255, 255, 0.1);
    color: white;
    border-radius: 6px;
    font-size: 13px;
    transition: all 0.3s;
}

input:focus, select:focus {
    border-color: var(--theme-primary);
    box-shadow: 0 0 10px rgba(0, 242, 255, 0.2);
    outline: none;
}

.password-group {
    position: relative;
    display: flex;
}

.eye-btn {
    position: absolute;
    right: 8px;
    top: 50%;
    transform: translateY(-50%);
    background: none;
    border: none;
    cursor: pointer;
    font-size: 14px;
    opacity: 0.7;
}

.full-width {
    width: 100%;
    margin-top: 8px;
}

.form-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;
    border-bottom: 1px dashed rgba(255, 255, 255, 0.1);
}

.form-row:last-child {
    border-bottom: none;
}

.row-label {
    display: flex;
    flex-direction: column;
}

.row-label span {
    font-size: 13px;
    color: var(--text-primary);
}

.row-label small {
    font-size: 10px;
    color: var(--text-muted);
}

.row-action {
    display: flex;
    gap: 8px;
    align-items: center;
}

.row-action select {
    width: 80px;
    padding: 4px 8px;
    height: 30px;
}

.input-with-btn {
    display: flex;
    gap: 8px;
}

.btn-icon {
    width: 30px;
    height: 30px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 6px;
    color: white;
    cursor: pointer;
    transition: all 0.2s;
}

.btn-icon:hover {
    background: var(--theme-primary);
    color: black;
}

.maintenance-actions {
    display: flex;
    gap: 12px;
}

.btn {
    padding: 10px 16px;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 12px;
    font-weight: bold;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    transition: all 0.3s;
}

.btn:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.btn-warning { background: linear-gradient(135deg, #ff9800, #f57c00); color: white; }
.btn-primary { background: linear-gradient(135deg, var(--theme-primary), var(--theme-secondary)); color: #000; flex: 1; }
.btn-danger { background: linear-gradient(135deg, #ff4d4d, #c0392b); color: white; flex: 1; }
</style>
