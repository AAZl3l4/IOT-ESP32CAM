/**
 * ESP32-CAM 控制面板 - JavaScript逻辑
 * @file test-panel.js
 */

// ===========================
// 基础工具函数
// ===========================
const getBaseUrl = () => document.getElementById('baseUrl').value;
const getClientId = () => document.getElementById('clientId').value;

const framesizeMap = {
    7: '480p (HVGA)',
    9: 'SVGA',
    10: 'XGA',
    11: '720p (HD)',
    12: 'SXGA',
    13: 'UXGA',
    14: '1080p (FHD)'
};

// 切换密码显示/隐藏
function togglePasswordVisibility() {
    const passwordInput = document.getElementById('wifiPass');
    const toggleIcon = document.getElementById('togglePassword');
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        toggleIcon.textContent = '🙈';
    } else {
        passwordInput.type = 'password';
        toggleIcon.textContent = '👁️';
    }
}

function showResponse(data, isError = false) {
    const elem = document.getElementById('responseText');
    const timestamp = new Date().toLocaleTimeString();
    elem.textContent = `[${timestamp}] ${isError ? '❌ 错误' : '✅ 成功'}\n${JSON.stringify(data, null, 2)}`;
    elem.style.color = isError ? '#d32f2f' : '#2e7d32';

    // 如果是设备状态，更新状态卡片
    if (!isError && data.data && data.data.found) {
        updateStatusCard(data.data);
    }
}

function updateStatusCard(status) {
    document.getElementById('statusCard').style.display = 'block';

    // 在线状态
    const online = status.online ? '🟢 在线' : '🔴 离线';
    document.getElementById('statusOnline').textContent = online;
    document.getElementById('statusOnline').style.color = status.online ? '#4caf50' : '#f44336';

    // 运行时间
    const uptime = parseInt(status.uptime);
    const hours = Math.floor(uptime / 3600);
    const minutes = Math.floor((uptime % 3600) / 60);
    const seconds = uptime % 60;
    document.getElementById('statusUptime').textContent = `${hours}时${minutes}分${seconds}秒`;

    // 空闲内存
    const memory = (status.freeHeap / 1024).toFixed(1);
    document.getElementById('statusMemory').textContent = `${memory} KB`;

    // WiFi信号强度
    const rssi = status.rssi;
    let signalText = '';
    let signalColor = '';
    if (rssi > -50) {
        signalText = `${rssi} dBm 📶 优秀`;
        signalColor = '#4caf50';
    } else if (rssi > -70) {
        signalText = `${rssi} dBm 📶 良好`;
        signalColor = '#8bc34a';
    } else if (rssi > -80) {
        signalText = `${rssi} dBm 📶 一般`;
        signalColor = '#ff9800';
    } else {
        signalText = `${rssi} dBm 📶 较弱`;
        signalColor = '#f44336';
    }
    document.getElementById('statusSignal').textContent = signalText;
    document.getElementById('statusSignal').style.color = signalColor;

    // LED状态
    document.getElementById('statusLed').textContent = status.ledStatus ? '💡 开启' : '🌑 关闭';
    document.getElementById('statusLed').style.color = status.ledStatus ? '#ffc107' : '#757575';

    // LED亮度
    document.getElementById('statusBrightness').textContent = status.ledBrightness;

    // 分辨率
    const framesizeText = framesizeMap[status.framesize] || `未知 (${status.framesize})`;
    document.getElementById('statusFramesize').textContent = framesizeText;

    // 最后更新时间
    const lastUpdate = new Date(parseInt(status.lastUpdateTime));
    const now = new Date();
    const diffSeconds = Math.floor((now - lastUpdate) / 1000);
    document.getElementById('statusLastUpdate').textContent = `${diffSeconds}秒前`;
}

function showLoading(button) {
    const original = button.innerHTML;
    button.innerHTML += '<span class="loading"></span>';
    button.disabled = true;
    return () => {
        button.innerHTML = original;
        button.disabled = false;
    };
}

async function apiCall(url, method = 'POST', body = null) {
    try {
        const options = { method };
        if (body) {
            options.headers = { 'Content-Type': 'application/json' };
            options.body = JSON.stringify(body);
        }
        const response = await fetch(url, options);
        const data = await response.json();
        showResponse(data, data.code !== 0);
        return data;
    } catch (error) {
        showResponse({ error: error.message }, true);
    }
}

// ===========================
// 摄像头控制
// ===========================
async function capture() {
    const btn = event.target;
    const done = showLoading(btn);
    await apiCall(`${getBaseUrl()}/mqtt/capture/${getClientId()}`);
    done();
}

async function getStatus() {
    await apiCall(`${getBaseUrl()}/mqtt/status/${getClientId()}`, 'GET');
}

async function setResolution(framesize) {
    const btn = event.target;
    const done = showLoading(btn);
    await apiCall(`${getBaseUrl()}/mqtt/stream-resolution/${getClientId()}`, 'POST', { framesize });
    done();

    // 提示用户刷新视频流
    const container = document.getElementById('streamContainer');
    if (container.style.display !== 'none') {
        alert('✅ 分辨率已设置，视频流将自动应用新分辨率');
    }
}

// ===========================
// LED控制 - 切换模式
// ===========================
let currentLedStatus = false;    // 闪光灯当前状态
let currentRedLedStatus = false; // 指示灯当前状态

/**
 * 切换闪光灯状态
 */
async function toggleLed() {
    const btn = document.getElementById('ledToggleBtn');
    const done = showLoading(btn);

    // 取反当前状态
    const newValue = currentLedStatus ? 0 : 1;
    const result = await apiCall(`${getBaseUrl()}/mqtt/led/${getClientId()}`, 'POST', { value: newValue });

    // 更新UI状态
    if (result && result.code === 0) {
        currentLedStatus = !currentLedStatus;
        updateLedButtonUI();
    }
    done();
}

/**
 * 切换红色指示灯状态
 */
async function toggleRedLed() {
    const btn = document.getElementById('redLedToggleBtn');
    const done = showLoading(btn);

    // 取反当前状态
    const newValue = currentRedLedStatus ? 0 : 1;
    const result = await apiCall(`${getBaseUrl()}/mqtt/red-led/${getClientId()}`, 'POST', { value: newValue });

    // 更新UI状态
    if (result && result.code === 0) {
        currentRedLedStatus = !currentRedLedStatus;
        updateRedLedButtonUI();
    }
    done();
}

/**
 * 更新闪光灯按钮UI
 */
function updateLedButtonUI() {
    const btn = document.getElementById('ledToggleBtn');
    const icon = document.getElementById('ledIcon');
    if (currentLedStatus) {
        btn.className = 'btn-toggle-on';
        icon.textContent = '💡';
    } else {
        btn.className = 'btn-toggle-off';
        icon.textContent = '🌑';
    }
}

/**
 * 更新指示灯按钮UI
 */
function updateRedLedButtonUI() {
    const btn = document.getElementById('redLedToggleBtn');
    const icon = document.getElementById('redLedIcon');
    if (currentRedLedStatus) {
        btn.className = 'btn-toggle-on-red';
        icon.textContent = '🔴';
    } else {
        btn.className = 'btn-toggle-off';
        icon.textContent = '⚫';
    }
}

// 保留原函数用于直接控制（内部使用）
async function ledControl(value) {
    await apiCall(`${getBaseUrl()}/mqtt/led/${getClientId()}`, 'POST', { value });
}

async function redLedControl(value) {
    await apiCall(`${getBaseUrl()}/mqtt/red-led/${getClientId()}`, 'POST', { value });
}

async function setBrightness() {
    const value = document.getElementById('brightness').value;
    const btn = event.target;
    const done = showLoading(btn);
    await apiCall(`${getBaseUrl()}/mqtt/led-brightness/${getClientId()}`, 'POST', { brightness: parseInt(value) });
    done();
}

// 设置DHT读取间隔
async function setDhtInterval() {
    const interval = parseInt(document.getElementById('dhtInterval').value);
    const btn = event.target;
    const done = showLoading(btn);
    await apiCall(`${getBaseUrl()}/mqtt/dht-interval/${getClientId()}`, 'POST', { interval });
    done();
}

// 设置状态上报间隔
async function setStatusInterval() {
    const interval = parseInt(document.getElementById('statusReportInterval').value);
    const btn = event.target;
    const done = showLoading(btn);
    await apiCall(`${getBaseUrl()}/mqtt/cam/${getClientId()}/set_status_interval`, 'POST', { interval });
    done();
}

// ===========================
// 配置管理
// ===========================
async function setWiFi() {
    const ssid = document.getElementById('wifiSsid').value;
    const password = document.getElementById('wifiPass').value;
    if (!ssid || !password) {
        alert('❌ 请输入WiFi名称和密码');
        return;
    }
    if (!confirm('⚠️ 设置WiFi后设备将重启，确认继续？')) return;

    const btn = event.target;
    const done = showLoading(btn);
    await apiCall(`${getBaseUrl()}/mqtt/config/wifi/${getClientId()}`, 'POST', { ssid, password });
    done();
}

async function setMQTT() {
    const server = document.getElementById('mqttServer').value;
    const port = parseInt(document.getElementById('mqttPort').value);
    if (!server) {
        alert('❌ 请输入MQTT服务器地址');
        return;
    }
    if (!confirm('⚠️ 设置MQTT后设备将重启，确认继续？')) return;

    const btn = event.target;
    const done = showLoading(btn);
    await apiCall(`${getBaseUrl()}/mqtt/config/mqtt/${getClientId()}`, 'POST', { server, port });
    done();
}

async function setUploadUrl() {
    const url = document.getElementById('uploadUrl').value;
    if (!url) {
        alert('❌ 请输入上传URL');
        return;
    }
    const btn = event.target;
    const done = showLoading(btn);
    await apiCall(`${getBaseUrl()}/mqtt/config/upload-url/${getClientId()}`, 'POST', { url });
    done();
}

async function getConfig() {
    await apiCall(`${getBaseUrl()}/mqtt/config/${getClientId()}`, 'GET');
}

async function resetConfig() {
    if (!confirm('⚠️ 确认恢复默认配置？设备将重启！')) return;
    const btn = event.target;
    const done = showLoading(btn);
    await apiCall(`${getBaseUrl()}/mqtt/config/reset/${getClientId()}`, 'POST');
    done();
}

// 快捷参数设置
async function quickParam(name, value) {
    const btn = event.target;
    const done = showLoading(btn);
    await apiCall(`${getBaseUrl()}/mqtt/param/${getClientId()}`, 'POST', { name, value });
    done();
}

// ===========================
// 视频流控制
// ===========================
function startStream() {
    const ip = document.getElementById('esp32Ip').value;
    if (!ip) {
        alert('❌ 请输入ESP32-CAM的IP地址');
        return;
    }

    const streamUrl = `http://${ip}/stream?t=${Date.now()}`;
    const img = document.getElementById('videoStream');
    const container = document.getElementById('streamContainer');

    img.src = streamUrl;
    container.style.display = 'block';

    // 保存IP到localStorage
    localStorage.setItem('esp32_ip', ip);

    showResponse({
        code: 0,
        msg: '视频流已启动',
        streamUrl: streamUrl
    });
}

function stopStream() {
    const img = document.getElementById('videoStream');
    const container = document.getElementById('streamContainer');

    img.src = '';
    container.style.display = 'none';

    showResponse({
        code: 0,
        msg: '视频流已停止'
    });
}

// ===========================
// 操作日志相关
// ===========================
async function loadLogs() {
    try {
        const response = await fetch(`${getBaseUrl()}/mqtt/logs/latest?limit=10`);
        const result = await response.json();

        if (result.code === 0 && result.data) {
            displayLogs(result.data);
        }
    } catch (error) {
        console.error('加载日志失败:', error);
    }
}

function displayLogs(logs) {
    const container = document.getElementById('logContainer');
    if (logs.length === 0) {
        container.innerHTML = '<p style="padding: 20px; text-align: center; color: #999;">暂无操作日志</p>';
        return;
    }

    let html = '<table style="width: 100%; border-collapse: collapse;">';
    html += `
        <thead>
            <tr style="background: #f5f5f5; border-bottom: 2px solid #ddd;">
                <th style="padding: 10px; text-align: left;">时间</th>
                <th style="padding: 10px; text-align: left;">设备</th>
                <th style="padding: 10px; text-align: left;">操作</th>
                <th style="padding: 10px; text-align: left;">结果</th>
            </tr>
        </thead>
        <tbody>
    `;

    logs.forEach(log => {
        const resultColor = log.result === 'success' ? '#4caf50' :
            log.result === 'failed' ? '#f44336' : '#ff9800';
        const resultText = log.result === 'success' ? '✓ 成功' :
            log.result === 'failed' ? '✗ 失败' : '⏳ 处理中';

        html += `
            <tr style="border-bottom: 1px solid #eee;">
                <td style="padding: 10px;">${formatTime(log.createTime)}</td>
                <td style="padding: 10px;">${log.clientId}</td>
                <td style="padding: 10px;">${log.operationDesc}</td>
                <td style="padding: 10px; color: ${resultColor}; font-weight: bold;">
                    ${resultText}${log.resultMsg ? ': ' + log.resultMsg : ''}
                </td>
            </tr>
        `;
    });

    html += '</tbody></table>';
    container.innerHTML = html;
}

function formatTime(timeStr) {
    const date = new Date(timeStr);
    return `${date.getMonth() + 1}-${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`;
}

function addLogToTop(logData) {
    const container = document.getElementById('logContainer');
    let tbody = container.querySelector('tbody');

    // 如果没有表格，初始化一个
    if (!tbody) {
        container.innerHTML = `
            <table style="width: 100%; border-collapse: collapse;">
                <thead>
                    <tr style="background: #f5f5f5; border-bottom: 2px solid #ddd;">
                        <th style="padding: 10px; text-align: left;">时间</th>
                        <th style="padding: 10px; text-align: left;">设备</th>
                        <th style="padding: 10px; text-align: left;">操作</th>
                        <th style="padding: 10px; text-align: left;">结果</th>
                    </tr>
                </thead>
                <tbody></tbody>
            </table>`;
        tbody = container.querySelector('tbody');
    }

    const resultColor = logData.result === 'success' ? '#4caf50' :
        logData.result === 'failed' ? '#f44336' : '#ff9800';
    const resultText = logData.result === 'success' ? '✓ 成功' :
        logData.result === 'failed' ? '✗ 失败' : '⏳ 处理中';

    // 如果是success或failed，尝试找到并更新pending行
    if (logData.result !== 'pending') {
        const rows = tbody.querySelectorAll('tr[data-operation]');
        for (const row of rows) {
            if (row.dataset.operation === logData.operation && row.dataset.status === 'pending') {
                // 更新这一行
                row.dataset.status = logData.result;
                row.querySelector('.result-cell').innerHTML =
                    `<span style="color: ${resultColor}; font-weight: bold;">${resultText}${logData.resultMsg ? ': ' + logData.resultMsg : ''}</span>`;
                row.style.backgroundColor = logData.result === 'success' ? '#e8f5e9' : '#ffebee';
                setTimeout(() => {
                    row.style.transition = 'background-color 0.5s';
                    row.style.backgroundColor = '';
                }, 500);
                return; // 已更新，不需要插入新行
            }
        }
    }

    // 插入新行
    const newRow = document.createElement('tr');
    newRow.dataset.operation = logData.operation;
    newRow.dataset.status = logData.result;
    newRow.style.borderBottom = '1px solid #eee';
    newRow.style.backgroundColor = '#fffde7';
    newRow.innerHTML = `
        <td style="padding: 10px;">${logData.time}</td>
        <td style="padding: 10px;">${logData.clientId || '-'}</td>
        <td style="padding: 10px;">${logData.operationDesc}</td>
        <td class="result-cell" style="padding: 10px;">
            <span style="color: ${resultColor}; font-weight: bold;">${resultText}${logData.resultMsg ? ': ' + logData.resultMsg : ''}</span>
        </td>
    `;

    tbody.insertBefore(newRow, tbody.firstChild);

    setTimeout(() => {
        newRow.style.transition = 'background-color 0.5s';
        newRow.style.backgroundColor = '';
    }, 100);

    // 保持最多显示10条
    while (tbody.children.length > 10) {
        tbody.removeChild(tbody.lastChild);
    }
}

// ===========================
// 温湿度图表相关
// ===========================
let dhtChart = null;

function initDhtChart() {
    const ctx = document.getElementById('dhtChart').getContext('2d');
    dhtChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                {
                    label: '温度 (℃)',
                    data: [],
                    borderColor: '#ff6b6b',
                    backgroundColor: 'rgba(255, 107, 107, 0.1)',
                    tension: 0.3,
                    fill: true
                },
                {
                    label: '湿度 (%)',
                    data: [],
                    borderColor: '#74b9ff',
                    backgroundColor: 'rgba(116, 185, 255, 0.1)',
                    tension: 0.3,
                    fill: true
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'top',
                }
            },
            scales: {
                y: {
                    beginAtZero: false
                }
            }
        }
    });
}

// ===========================
// 设备状态图表（WiFi信号/内存）
// ===========================
let statusChart = null;

function initStatusChart() {
    // 销毁旧实例防止重复创建
    if (statusChart) {
        statusChart.destroy();
        statusChart = null;
    }

    const canvas = document.getElementById('statusChart');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    statusChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                {
                    label: 'WiFi信号 (dBm)',
                    data: [],
                    borderColor: '#f093fb',
                    backgroundColor: 'rgba(240, 147, 251, 0.1)',
                    tension: 0.3,
                    fill: true,
                    yAxisID: 'y'
                },
                {
                    label: '空闲内存 (KB)',
                    data: [],
                    borderColor: '#38ef7d',
                    backgroundColor: 'rgba(56, 239, 125, 0.1)',
                    tension: 0.3,
                    fill: true,
                    yAxisID: 'y1'
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false,
            },
            plugins: {
                legend: {
                    position: 'top',
                }
            },
            scales: {
                y: {
                    type: 'linear',
                    display: true,
                    position: 'left',
                    min: -100,  // WiFi信号最小值
                    max: 0,     // WiFi信号最大值
                    title: {
                        display: true,
                        text: 'dBm'
                    }
                },
                y1: {
                    type: 'linear',
                    display: true,
                    position: 'right',
                    min: 0,
                    suggestedMax: 200,  // 空闲内存约150-200KB
                    title: {
                        display: true,
                        text: 'KB'
                    },
                    grid: {
                        drawOnChartArea: false
                    }
                }
            }
        }
    });
}

/**
 * 更新设备状态图表（SSE推送时调用）
 */
function updateStatusChart(data) {
    if (!statusChart) return;

    const time = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

    // 保持最多30个数据点
    if (statusChart.data.labels.length >= 30) {
        statusChart.data.labels.shift();
        statusChart.data.datasets[0].data.shift();
        statusChart.data.datasets[1].data.shift();
    }

    statusChart.data.labels.push(time);
    statusChart.data.datasets[0].data.push(data.rssi);
    statusChart.data.datasets[1].data.push(data.freeHeap / 1024); // 转换为KB
    statusChart.update('none');

    // 更新状态卡片
    document.getElementById('statusRssi').textContent = data.rssi + ' dBm';
    document.getElementById('statusFreeHeap').textContent = (data.freeHeap / 1024).toFixed(1) + ' KB';

    // 运行时间
    if (data.uptime !== undefined) {
        const hours = Math.floor(data.uptime / 3600);
        const mins = Math.floor((data.uptime % 3600) / 60);
        const secs = data.uptime % 60;
        document.getElementById('statusUptime').textContent = hours > 0 ? `${hours}h ${mins}m` : `${mins}m ${secs}s`;
    }

    // LED亮度实时回显到滑块
    if (data.ledBrightness !== undefined) {
        const slider = document.getElementById('brightness');
        const valueSpan = document.getElementById('brightnessValue');
        if (slider) slider.value = data.ledBrightness;
        if (valueSpan) valueSpan.textContent = data.ledBrightness;
    }

    // 分辨率
    if (data.framesize !== undefined) {
        const fsMap = { 0: 'QQVGA', 3: 'HQVGA', 5: 'QVGA', 7: 'VGA', 8: 'SVGA', 9: 'XGA', 10: 'HD', 11: 'SXGA', 13: 'UXGA', 14: 'FHD' };
        document.getElementById('statusFramesize').textContent = fsMap[data.framesize] || data.framesize;
    }

    // LED状态同步
    if (data.ledStatus !== undefined) {
        currentLedStatus = data.ledStatus;
        updateLedButtonUI();
    }
    if (data.redLedStatus !== undefined) {
        currentRedLedStatus = data.redLedStatus;
        updateRedLedButtonUI();
    }
}

async function loadDhtData() {
    try {
        const response = await fetch(`${getBaseUrl()}/mqtt/dht/dashboard/${getClientId()}?chartLimit=30`);
        const result = await response.json();

        if (result.code === 0 && result.data) {
            const data = result.data;

            // 更新当前值
            if (data.hasData) {
                document.getElementById('currentTemp').textContent = data.temperature;
                document.getElementById('currentHumidity').textContent = data.humidity;
                document.getElementById('dhtUpdateTime').textContent = data.updateTime;
            }

            // 更新图表
            if (dhtChart && data.labels) {
                dhtChart.data.labels = data.labels;
                dhtChart.data.datasets[0].data = data.temperatures;
                dhtChart.data.datasets[1].data = data.humidities;
                dhtChart.update();
            }
        }
    } catch (error) {
        console.error('加载温湿度数据失败:', error);
    }
}

/**
 * 加载设备状态历史数据（rssi/freeHeap图表）
 */
async function loadStatusData() {
    try {
        const response = await fetch(`${getBaseUrl()}/mqtt/status-history/chart/${getClientId()}?limit=30`);
        const result = await response.json();

        if (result.code === 0 && result.data) {
            const data = result.data;

            // 更新图表
            if (statusChart && data.hasData) {
                statusChart.data.labels = data.labels;
                statusChart.data.datasets[0].data = data.rssiData;
                statusChart.data.datasets[1].data = data.freeHeapData;
                statusChart.update();
                console.log('设备状态历史数据已加载:', data.labels.length, '条');
            }
        }
    } catch (error) {
        console.error('加载设备状态历史失败:', error);
    }
}

// ===========================
// SSE连接相关
// ===========================
let sseConnection = null;

/**
 * 应用配置到前端UI
 */
function applyConfig(config) {
    console.log('========== 收到设备配置 ==========');
    console.log('完整配置对象:', config);

    // DHT读取间隔
    if (config.dhtInterval) {
        const select = document.getElementById('dhtInterval');
        if (select) {
            select.value = config.dhtInterval;
            console.log('✅ DHT间隔已设置为:', config.dhtInterval, 'ms');
        } else {
            console.warn('⚠️ dhtInterval元素未找到');
        }
    }

    // LED亮度回显
    if (config.ledBrightness !== undefined) {
        const slider = document.getElementById('brightness');
        const valueSpan = document.getElementById('brightnessValue');
        if (slider) {
            slider.value = config.ledBrightness;
            if (valueSpan) valueSpan.textContent = config.ledBrightness;
            console.log('✅ LED亮度已设置为:', config.ledBrightness);
        }
    }

    // LED状态同步到切换按钮
    if (config.ledStatus !== undefined) {
        currentLedStatus = config.ledStatus;
        updateLedButtonUI();
        console.log('✅ 闪光灯状态:', config.ledStatus ? '开启' : '关闭');
    }

    // 红色指示灯状态同步
    if (config.redLedStatus !== undefined) {
        currentRedLedStatus = config.redLedStatus;
        updateRedLedButtonUI();
        console.log('✅ 指示灯状态:', config.redLedStatus ? '开启' : '关闭');
    }

    // WiFi配置回显
    if (config.wifiSsid) {
        const input = document.getElementById('wifiSsid');
        if (input) {
            input.value = config.wifiSsid;
            console.log('✅ WiFi SSID:', config.wifiSsid);
        }
    }
    if (config.wifiPassword) {
        const input = document.getElementById('wifiPass');
        if (input) {
            input.value = config.wifiPassword;
            console.log('✅ WiFi密码已回显');
        }
    }

    // MQTT配置回显
    if (config.mqttBroker) {
        const input = document.getElementById('mqttServer');
        if (input) {
            input.value = config.mqttBroker;
            console.log('✅ MQTT服务器:', config.mqttBroker);
        }
    }
    if (config.mqttPort) {
        const input = document.getElementById('mqttPort');
        if (input) {
            input.value = config.mqttPort;
            console.log('✅ MQTT端口:', config.mqttPort);
        }
    }

    // 设备状态信息
    if (config.rssi !== undefined) {
        console.log('📶 WiFi信号强度:', config.rssi, 'dBm');
    }
    if (config.uptime !== undefined) {
        console.log('⏱️ 运行时间:', config.uptime, '秒');
    }
    if (config.freeHeap !== undefined) {
        console.log('💾 空闲内存:', config.freeHeap, 'bytes');
    }
    if (config.framesize !== undefined) {
        console.log('📷 摄像头分辨率:', config.framesize);
    }

    // 上传URL回显
    if (config.uploadUrl) {
        const input = document.getElementById('uploadUrl');
        if (input) {
            input.value = config.uploadUrl;
            console.log('✅ 上传URL:', config.uploadUrl);
        }
    }

    // === 隐藏加载遮罩，显示主容器 ===
    const loadingOverlay = document.getElementById('loadingOverlay');
    const mainContainer = document.getElementById('mainContainer');
    if (loadingOverlay) loadingOverlay.style.display = 'none';
    if (mainContainer) mainContainer.style.display = 'block';

    // 显示设备状态面板
    const panelEl = document.getElementById('deviceStatusPanel');
    if (panelEl) panelEl.style.display = 'block';

    // 运行时间
    if (config.uptime !== undefined) {
        const el = document.getElementById('statusUptime');
        if (el) {
            const hours = Math.floor(config.uptime / 3600);
            const mins = Math.floor((config.uptime % 3600) / 60);
            const secs = config.uptime % 60;
            el.textContent = hours > 0 ? `${hours}h ${mins}m` : `${mins}m ${secs}s`;
        }
    }

    // 空闲内存
    if (config.freeHeap !== undefined) {
        const el = document.getElementById('statusFreeHeap');
        if (el) el.textContent = (config.freeHeap / 1024).toFixed(1) + 'KB';
    }

    // WiFi信号
    if (config.rssi !== undefined) {
        const el = document.getElementById('statusRssi');
        if (el) el.textContent = config.rssi + ' dBm';
    }

    // LED亮度
    if (config.ledBrightness !== undefined) {
        const el = document.getElementById('statusLedBrightness');
        if (el) el.textContent = config.ledBrightness;
    }

    // 分辨率
    if (config.framesize !== undefined) {
        const el = document.getElementById('statusFramesize');
        const resolutions = { 0: 'QQVGA', 3: 'HQVGA', 5: 'QVGA', 6: 'CIF', 7: 'VGA', 8: 'SVGA', 9: 'XGA', 10: 'SXGA', 11: 'UXGA', 13: 'FHD' };
        if (el) el.textContent = resolutions[config.framesize] || config.framesize;
    }

    // DHT间隔
    if (config.dhtInterval !== undefined) {
        const el = document.getElementById('statusDhtInterval');
        if (el) el.textContent = (config.dhtInterval / 1000) + 's';
    }

    // 状态上报间隔
    if (config.statusInterval !== undefined) {
        const el = document.getElementById('statusInterval');
        if (el) {
            const secs = config.statusInterval / 1000;
            el.textContent = secs + 's';
        }
        // 回显到下拉框
        const select = document.getElementById('statusReportInterval');
        if (select) {
            select.value = config.statusInterval;
            console.log('✅ 状态上报间隔:', config.statusInterval, 'ms');
        }
    }

    console.log('======================================');
}

// 刷新配置（手动触发）
function refreshConfig() {
    const clientId = getClientId();
    console.log('手动刷新配置:', clientId);

    // 显示加载状态
    const loadingEl = document.getElementById('deviceStatusLoading');
    const panelEl = document.getElementById('deviceStatusPanel');
    if (loadingEl) loadingEl.style.display = 'block';
    if (panelEl) panelEl.style.display = 'none';

    // 发送get_config命令
    fetch(`${getBaseUrl()}/mqtt/cam/${clientId}/get_config`, { method: 'POST' })
        .then(r => r.json())
        .then(data => {
            console.log('刷新配置请求已发送:', data);
            showResponse(data, data.code !== 0);
        })
        .catch(e => {
            console.error('刷新配置失败:', e);
            showResponse({ error: e.message }, true);
        });
}

// 建立SSE连接接收实时温湿度数据
function connectSSE() {
    const clientId = getClientId();
    const sseUrl = `${getBaseUrl()}/mqtt/sse/dht/${clientId}`;

    // 关闭已有连接
    if (sseConnection) {
        sseConnection.close();
    }

    console.log('建立SSE连接:', sseUrl);
    sseConnection = new EventSource(sseUrl);

    // 连接成功
    sseConnection.addEventListener('connected', (event) => {
        console.log('SSE连接成功:', event.data);
        document.getElementById('sseStatus').innerHTML = '🟢 在线';
        document.getElementById('sseStatus').style.color = '#4caf50';
    });

    // 接收温湿度数据
    sseConnection.addEventListener('dht', (event) => {
        try {
            const data = JSON.parse(event.data);
            // 更新当前值显示
            document.getElementById('currentTemp').textContent = data.temperature.toFixed(2);
            document.getElementById('currentHumidity').textContent = data.humidity.toFixed(2);
            document.getElementById('dhtUpdateTime').textContent = data.time;

            // 更新图表（追加新数据点）
            if (dhtChart) {
                // 保持最多30个数据点
                if (dhtChart.data.labels.length >= 30) {
                    dhtChart.data.labels.shift();
                    dhtChart.data.datasets[0].data.shift();
                    dhtChart.data.datasets[1].data.shift();
                }
                dhtChart.data.labels.push(data.time);
                dhtChart.data.datasets[0].data.push(data.temperature);
                dhtChart.data.datasets[1].data.push(data.humidity);
                dhtChart.update('none'); // 不带动画更新，更流畅
            }
        } catch (e) {
            console.error('解析SSE数据失败:', e);
        }
    });

    // 接收操作日志
    sseConnection.addEventListener('log', (event) => {
        try {
            const data = JSON.parse(event.data);
            console.log('收到操作日志:', data);
            // 在日志容器顶部插入新日志
            addLogToTop(data);
        } catch (e) {
            console.error('解析日志失败:', e);
        }
    });

    // 接收设备配置
    sseConnection.addEventListener('config', (event) => {
        try {
            const config = JSON.parse(event.data);
            console.log('收到设备配置:', config);
            applyConfig(config);
        } catch (e) {
            console.error('解析配置失败:', e);
        }
    });

    // 接收设备状态（rssi/freeHeap实时数据）
    sseConnection.addEventListener('status', (event) => {
        try {
            const status = JSON.parse(event.data);
            console.log('收到设备状态:', status);
            updateStatusChart(status);
        } catch (e) {
            console.error('解析状态失败:', e);
        }
    });

    // 连接错误
    sseConnection.onerror = (error) => {
        console.error('SSE连接错误:', error);
        document.getElementById('sseStatus').innerHTML = '🔴 离线';
        document.getElementById('sseStatus').style.color = '#f44336';
        // 5秒后重连
        setTimeout(connectSSE, 5000);
    };
}

// ===========================
// 页面初始化
// ===========================
window.addEventListener('DOMContentLoaded', () => {
    // 恢复ESP32 IP
    const savedIp = localStorage.getItem('esp32_ip');
    if (savedIp) {
        document.getElementById('esp32Ip').value = savedIp;
    }

    // 初始化图表
    initDhtChart();
    initStatusChart();

    // 加载历史数据
    loadDhtData();
    loadStatusData();  // 加载设备状态历史（rssi/freeHeap）
    loadLogs();

    // 建立SSE实时推送
    connectSSE();

    // LED亮度滑块：滑动结束自动设置（不需要点击按钮）
    const brightnessSlider = document.getElementById('brightness');
    if (brightnessSlider) {
        brightnessSlider.addEventListener('change', (e) => {
            const value = e.target.value;
            console.log('LED亮度滑块变更:', value);
            setBrightness();  // 调用现有的设置亮度函数
        });
    }
});

// 定时刷新状态
setInterval(() => {
    const statusCard = document.getElementById('statusCard');
    if (statusCard && statusCard.style.display !== 'none') {
        getStatus();
    }
}, 10000); // 每10秒自动刷新一次

// 页面关闭时断开SSE连接
window.addEventListener('beforeunload', () => {
    if (sseConnection) {
        sseConnection.close();
    }
});
