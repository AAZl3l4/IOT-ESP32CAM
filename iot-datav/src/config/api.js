/**
 * API配置
 */

// 后端基础地址
export const BASE_URL = 'http://localhost:8080'

// 设备ID
export const DEFAULT_CLIENT_ID = 'esp32cam'

// SSE连接地址
export const SSE_URL = `${BASE_URL}/mqtt/sse/dht`

// API端点
export const API = {
    // 拍照
    capture: (clientId) => `${BASE_URL}/mqtt/capture/${clientId}`,

    // LED控制
    led: (clientId) => `${BASE_URL}/mqtt/led/${clientId}`,
    ledBrightness: (clientId) => `${BASE_URL}/mqtt/led-brightness/${clientId}`,
    redLed: (clientId) => `${BASE_URL}/mqtt/red-led/${clientId}`,

    // 舵机控制
    servo: (clientId) => `${BASE_URL}/mqtt/servo/${clientId}`,

    // 继电器控制
    relay: (clientId) => `${BASE_URL}/mqtt/relay/${clientId}`,

    // 摄像头参数
    param: (clientId) => `${BASE_URL}/mqtt/param/${clientId}`,
    streamResolution: (clientId) => `${BASE_URL}/mqtt/stream-resolution/${clientId}`,

    // 设备配置
    getConfig: (clientId) => `${BASE_URL}/mqtt/cam/${clientId}/get_config`,

    // DHT间隔设置
    dhtInterval: (clientId) => `${BASE_URL}/mqtt/dht-interval/${clientId}`,

    // 状态上报间隔
    statusInterval: (clientId) => `${BASE_URL}/mqtt/cam/${clientId}/set_status_interval`,

    // 操作日志
    logs: `${BASE_URL}/mqtt/logs/latest`,

    // 温湿度数据
    dhtDashboard: (clientId) => `${BASE_URL}/mqtt/dht/dashboard/${clientId}`,

    // 设备状态历史
    statusHistory: (clientId) => `${BASE_URL}/mqtt/status-history/chart/${clientId}`,

    // 一言API
    hitokoto: 'https://v1.hitokoto.cn/?c=i',

    // 图片访问
    photo: (filename) => `${BASE_URL}/file/photos/${filename}`,
}
