/**
 * SSE连接管理
 */
import { useDeviceStore } from '@/stores/deviceStore'
import { SSE_URL, BASE_URL } from '@/config/api'

let sseConnection = null
let reconnectTimer = null

/**
 * 建立SSE连接
 */
export function connectSse(clientId) {
    const store = useDeviceStore()

    if (sseConnection) {
        sseConnection.close()
    }

    console.log('[SSE] 正在连接:', `${SSE_URL}/${clientId}`)
    sseConnection = new EventSource(`${SSE_URL}/${clientId}`)

    // 连接成功
    sseConnection.addEventListener('connected', (event) => {
        console.log('[SSE] 连接成功:', event.data)
        store.setSseConnected(true)
        if (reconnectTimer) {
            clearTimeout(reconnectTimer)
            reconnectTimer = null
        }
        // 后端SseController会在连接建立时自动发送get_config，无需前端再次请求
    })

    // 温湿度和光照
    sseConnection.addEventListener('dht', (event) => {
        try {
            const data = JSON.parse(event.data)
            store.updateDht(data)
        } catch (e) {
            console.error('[SSE] 解析dht失败:', e)
        }
    })

    // 设备状态
    sseConnection.addEventListener('status', (event) => {
        try {
            const data = JSON.parse(event.data)
            store.updateStatus(data)
        } catch (e) {
            console.error('[SSE] 解析status失败:', e)
        }
    })

    // 设备配置
    sseConnection.addEventListener('config', (event) => {
        try {
            const config = JSON.parse(event.data)
            console.log('[SSE] 设备配置:', config)
            store.applyConfig(config)
        } catch (e) {
            console.error('[SSE] 解析config失败:', e)
        }
    })

    // 操作日志
    sseConnection.addEventListener('log', (event) => {
        try {
            const log = JSON.parse(event.data)
            store.addLog(log)
        } catch (e) {
            console.error('[SSE] 解析log失败:', e)
        }
    })

    // 拍照结果
    sseConnection.addEventListener('capture', (event) => {
        try {
            const data = JSON.parse(event.data)
            window.dispatchEvent(new CustomEvent('capture-result', { detail: data }))
        } catch (e) {
            console.error('[SSE] 解析capture失败:', e)
        }
    })

    // AI响应
    sseConnection.addEventListener('ai-response', (event) => {
        try {
            const data = JSON.parse(event.data)
            window.dispatchEvent(new CustomEvent('ai-response', { detail: data }))
        } catch (e) {
            console.error('[SSE] 解析ai-response失败:', e)
        }
    })

    // 错误处理
    sseConnection.onerror = (error) => {
        console.error('[SSE] 连接错误:', error)
        store.setSseConnected(false)

        if (!reconnectTimer) {
            console.log('[SSE] 5秒后重连...')
            reconnectTimer = setTimeout(() => {
                reconnectTimer = null
                connectSse(clientId)
            }, 5000)
        }
    }
}

/**
 * 断开SSE连接
 */
export function disconnectSse() {
    const store = useDeviceStore()
    if (reconnectTimer) {
        clearTimeout(reconnectTimer)
        reconnectTimer = null
    }
    if (sseConnection) {
        sseConnection.close()
        sseConnection = null
    }
    store.setSseConnected(false)
}
