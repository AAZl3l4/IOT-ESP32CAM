/**
 * API调用封装
 */
import { API } from '@/config/api'

async function apiCall(url, method = 'POST', body = null) {
    try {
        const options = { method }
        if (body) {
            options.headers = { 'Content-Type': 'application/json' }
            options.body = JSON.stringify(body)
        }
        const response = await fetch(url, options)
        return await response.json()
    } catch (error) {
        console.error('[API] 请求失败:', error)
        return { code: -1, msg: error.message }
    }
}

export function useApi(clientId) {
    return {
        capture: () => apiCall(API.capture(clientId), 'POST'),
        setLed: (value) => apiCall(API.led(clientId), 'POST', { value }),
        setLedBrightness: (brightness) => apiCall(API.ledBrightness(clientId), 'POST', { brightness }),
        setRedLed: (value) => apiCall(API.redLed(clientId), 'POST', { value }),
        setServo: (angle) => apiCall(API.servo(clientId), 'POST', { angle }),
        setRelay: (on) => apiCall(API.relay(clientId), 'POST', { on }),
        setParam: (name, value) => apiCall(API.param(clientId), 'POST', { name, value }),
        setStreamResolution: (framesize) => apiCall(API.streamResolution(clientId), 'POST', { framesize }),
        getConfig: () => apiCall(API.getConfig(clientId), 'POST'),
        setDhtInterval: (interval) => apiCall(API.dhtInterval(clientId), 'POST', { interval }),
        getLogs: (limit = 10) => apiCall(`${API.logs}?limit=${limit}`, 'GET'),
        getDhtDashboard: () => apiCall(`${API.dhtDashboard(clientId)}?chartLimit=30`, 'GET'),
        getStatusHistory: () => apiCall(`${API.statusHistory(clientId)}?limit=30`, 'GET'),
        getHitokoto: async () => {
            try {
                const res = await fetch(API.hitokoto)
                return await res.json()
            } catch (e) {
                return { hitokoto: '科技是第一生产力' }
            }
        }
    }
}
