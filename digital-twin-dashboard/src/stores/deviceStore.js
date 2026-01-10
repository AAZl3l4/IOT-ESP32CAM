/**
 * 设备状态管理 Store
 */
import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'

export const useDeviceStore = defineStore('device', () => {
    // SSE连接状态
    const sseConnected = ref(false)

    // 设备ID (普通字符串)
    const clientId = 'esp32cam'

    // 温湿度数据
    const dhtData = reactive({
        temperature: 0,
        humidity: 0,
        lightDark: false,
        time: ''
    })

    // 温湿度历史数据
    const dhtHistory = reactive({
        labels: [],
        temperatures: [],
        humidities: []
    })

    // 设备状态
    const deviceStatus = reactive({
        rssi: 0,
        freeHeap: 0,
        uptime: 0
    })

    // 状态历史
    const statusHistory = reactive({
        labels: [],
        rssiData: [],
        freeHeapData: []
    })

    // 控制状态
    const controlState = reactive({
        ledStatus: false,
        ledBrightness: 0,
        redLedStatus: false,
        servoAngle: 0,
        relayStatus: false,
        framesize: 11
    })

    // 设备配置
    const deviceConfig = reactive({
        wifiIp: '',
        wifiSsid: '',
        wifiPassword: '',
        mqttBroker: '',
        mqttPort: 1883,
        dhtInterval: 5000,
        statusInterval: 30000,
        uploadUrl: ''
    })

    // 操作日志
    const logs = ref([])

    // 是否正在等待手动拍照结果
    const pendingCapture = ref(false)

    // 摄像头配置
    const cameraConfig = reactive({
        brightness: 0,
        contrast: 0,
        saturation: 0,
        quality: 10,
        specialEffect: 0,
        whiteBalance: 1,
        aec: 1,
        gainCtrl: 1,
        hmirror: 0,
        vflip: 0,
        bpc: 0,
        wpc: 1,
        lenc: 1
    })

    // 更新温湿度
    function updateDht(data) {
        dhtData.temperature = data.temperature
        dhtData.humidity = data.humidity
        dhtData.lightDark = data.lightDark
        dhtData.time = data.time

        if (dhtHistory.labels.length >= 30) {
            dhtHistory.labels.shift()
            dhtHistory.temperatures.shift()
            dhtHistory.humidities.shift()
        }
        dhtHistory.labels.push(data.time)
        dhtHistory.temperatures.push(data.temperature)
        dhtHistory.humidities.push(data.humidity)
    }

    function updateStatus(data) {
        deviceStatus.rssi = data.rssi
        deviceStatus.freeHeap = data.freeHeap
        deviceStatus.uptime = data.uptime

        if (data.ledStatus !== undefined) controlState.ledStatus = data.ledStatus
        if (data.ledBrightness !== undefined) controlState.ledBrightness = data.ledBrightness
        if (data.redLedStatus !== undefined) controlState.redLedStatus = data.redLedStatus
        if (data.servoAngle !== undefined) controlState.servoAngle = data.servoAngle
        if (data.relayStatus !== undefined) controlState.relayStatus = data.relayStatus
        if (data.framesize !== undefined) controlState.framesize = data.framesize

        const time = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
        if (statusHistory.labels.length >= 30) {
            statusHistory.labels.shift()
            statusHistory.rssiData.shift()
            statusHistory.freeHeapData.shift()
        }
        statusHistory.labels.push(time)
        statusHistory.rssiData.push(data.rssi)
        statusHistory.freeHeapData.push(data.freeHeap / 1024)
    }

    function initDhtHistory(data) {
        if (data.labels) {
            dhtHistory.labels = data.labels
            dhtHistory.temperatures = data.temperatures
            dhtHistory.humidities = data.humidities
        }
    }

    function initStatusHistory(data) {
        if (data.labels) {
            statusHistory.labels = data.labels
            statusHistory.rssiData = data.rssiData
            statusHistory.freeHeapData = data.freeHeapData
        }
    }

    // 应用配置
    function applyConfig(config) {
        if (config.ledStatus !== undefined) controlState.ledStatus = config.ledStatus
        if (config.ledBrightness !== undefined) controlState.ledBrightness = config.ledBrightness
        if (config.redLedStatus !== undefined) controlState.redLedStatus = config.redLedStatus
        if (config.servoAngle !== undefined) controlState.servoAngle = config.servoAngle
        if (config.relayStatus !== undefined) controlState.relayStatus = config.relayStatus
        if (config.framesize !== undefined) controlState.framesize = config.framesize

        if (config.wifiIp) deviceConfig.wifiIp = config.wifiIp
        if (config.wifiSsid) deviceConfig.wifiSsid = config.wifiSsid
        if (config.wifiPassword) deviceConfig.wifiPassword = config.wifiPassword
        if (config.mqttBroker) deviceConfig.mqttBroker = config.mqttBroker
        if (config.mqttPort) deviceConfig.mqttPort = config.mqttPort
        if (config.dhtInterval) deviceConfig.dhtInterval = config.dhtInterval
        if (config.statusInterval) deviceConfig.statusInterval = config.statusInterval
        if (config.uploadUrl) deviceConfig.uploadUrl = config.uploadUrl

        if (config.rssi !== undefined) deviceStatus.rssi = config.rssi
        if (config.freeHeap !== undefined) deviceStatus.freeHeap = config.freeHeap
        if (config.uptime !== undefined) deviceStatus.uptime = config.uptime

        // 摄像头参数
        if (config.brightness !== undefined) cameraConfig.brightness = config.brightness
        if (config.contrast !== undefined) cameraConfig.contrast = config.contrast
        if (config.saturation !== undefined) cameraConfig.saturation = config.saturation
        if (config.quality !== undefined) cameraConfig.quality = config.quality
        if (config.specialEffect !== undefined) cameraConfig.specialEffect = config.specialEffect
        if (config.whiteBalance !== undefined) cameraConfig.whiteBalance = config.whiteBalance
        if (config.aec !== undefined) cameraConfig.aec = config.aec
        if (config.gainCtrl !== undefined) cameraConfig.gainCtrl = config.gainCtrl
        if (config.hmirror !== undefined) cameraConfig.hmirror = config.hmirror
        if (config.vflip !== undefined) cameraConfig.vflip = config.vflip
        if (config.bpc !== undefined) cameraConfig.bpc = config.bpc
        if (config.wpc !== undefined) cameraConfig.wpc = config.wpc
        if (config.lenc !== undefined) cameraConfig.lenc = config.lenc
    }

    // 添加日志
    function addLog(log) {
        // 如果是success或failed，尝试查找并更新对应的pending日志
        if (log.result !== 'pending') {
            const existingIndex = logs.value.findIndex(
                l => l.operation === log.operation && l.result === 'pending'
            )
            if (existingIndex !== -1) {
                logs.value[existingIndex].result = log.result
                logs.value[existingIndex].resultMsg = log.resultMsg
                return
            }
        }
        // 新日志直接插入
        logs.value.unshift(log)
        if (logs.value.length > 50) logs.value.pop()
    }

    function setSseConnected(connected) {
        sseConnected.value = connected
    }

    return {
        sseConnected, clientId, dhtData, dhtHistory, deviceStatus, statusHistory,
        controlState, deviceConfig, cameraConfig, logs, pendingCapture,
        updateDht, updateStatus, applyConfig, addLog, setSseConnected,
        initDhtHistory, initStatusHistory
    }
})
