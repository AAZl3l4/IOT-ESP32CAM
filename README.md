# 🎥 ESP32-CAM 物联网智能摄像头平台

> 基于 **ESP32-CAM** + **Spring Boot** + **MQTT** 的完整物联网系统

## 📋 项目概述

这是一个功能完善的物联网(IoT)平台，支持**远程摄像头控制**、**温湿度监测**、**实时视频推流**和**设备管理**。通过MQTT协议实现设备与服务器的双向通信，并提供友好的Web控制面板。

---

## 🚀 功能清单

### 📷 摄像头功能

| 功能 | 说明 | API/调用方式 |
|------|------|-------------|
| **1080p高清拍照** | 远程触发拍照，自动上传到服务器 | `POST /mqtt/capture/{clientId}` |
| **MJPEG视频流** | 720p实时推流，支持浏览器/VLC播放 | `http://{ESP32_IP}/stream` |
| **分辨率切换** | 支持480p/720p/1080p切换 | `POST /mqtt/stream-resolution/{clientId}` |
| **参数调整** | 亮度/对比度/饱和度/特效/质量等 | `POST /mqtt/param/{clientId}` |

**摄像头参数列表：**

| 参数名 | 说明 | 取值范围 |
|--------|------|----------|
| `brightness` | 亮度 | -2 ~ 2 |
| `contrast` | 对比度 | -2 ~ 2 |
| `saturation` | 饱和度 | -2 ~ 2 |
| `sharpness` | 锐度 | -2 ~ 2 |
| `quality` | JPEG质量 | 0-63 (越小越好) |
| `special_effect` | 特效 | 0=无, 1=负片, 2=黑白, 3=红调... |
| `awb` | 自动白平衡 | 0=关, 1=开 |
| `wb_mode` | 白平衡模式 | 0=自动, 1=晴天, 2=阴天... |
| `aec` | 自动曝光 | 0=关, 1=开 |
| `ae_level` | 曝光补偿 | -2 ~ 2 |
| `agc` | 自动增益 | 0=关, 1=开 |
| `hmirror` | 水平镜像 | 0=关, 1=开 |
| `vflip` | 垂直翻转 | 0=关, 1=开 |
| `framesize` | 分辨率 | 7=480p, 11=720p, 14=1080p |

---

### 💡 LED 控制

| 功能 | 说明 | API |
|------|------|-----|
| **闪光灯开关** | GPIO4白色LED，切换开关 | `POST /mqtt/led/{clientId}` body: `{"value": 1}` |
| **PWM亮度调节** | 0-255级亮度 | `POST /mqtt/led-brightness/{clientId}` body: `{"brightness": 128}` |
| **红色指示灯** | GPIO33红色LED，切换开关 | `POST /mqtt/red-led/{clientId}` body: `{"value": 1}` |

---

### 🪟 窗户控制 (SG90舵机)

| 功能 | 说明 | API |
|------|------|-----|
| **全开** | 舵机转到180° | `POST /mqtt/servo/{clientId}` body: `{"angle": 180}` |
| **半开** | 舵机转到90° | `POST /mqtt/servo/{clientId}` body: `{"angle": 90}` |
| **小开** | 舵机转到45° | `POST /mqtt/servo/{clientId}` body: `{"angle": 45}` |
| **关闭** | 舵机转到0° | `POST /mqtt/servo/{clientId}` body: `{"angle": 0}` |
| **自定义角度** | 任意0-180°角度 | `POST /mqtt/servo/{clientId}` body: `{"angle": 数值}` |

> 硬件接线: SG90舵机信号线接GPIO14，红线接5V，棕线接GND

---

### � 风扇控制 (继电器)

| 功能 | 说明 | API |
|------|------|-----|
| **开启** | 继电器吸合，风扇转动 | `POST /mqtt/relay/{clientId}` body: `{"on": true}` |
| **关闭** | 继电器释放，风扇停止 | `POST /mqtt/relay/{clientId}` body: `{"on": false}` |

> 硬件接线: 继电器IN接GPIO12（烧录时断开！），COM接5V，NO接风扇红线

---

### ☀️ 光照监控 (光敏电阻)

| 功能 | 说明 | 数据 |
|------|------|------|
| **明暗检测** | 读取DO数字输出判断环境亮度 | lightDark: true=暗, false=亮 |
| **实时推送** | 随温湿度一起通过SSE推送 | dht事件中包含lightDark |

> 硬件接线: DO接GPIO2，VCC接3.3V，GND接GND（可通过模块电位器调节灵敏度）

---

### 🎙️ 语音控制 (ASR PRO)

| 语音指令 | 功能 | 串口输出 |
|----------|------|----------|
| "小管家" | 唤醒词 | - |
| "开灯" | LED开 | `CMD:LED_ON` |
| "关灯" | LED关 | `CMD:LED_OFF` |
| "灯光最亮" | LED亮度255 | `CMD:LED_MAX` |
| "灯光中等" | LED亮度128 | `CMD:LED_MID` |
| "开指示灯" | 红灯开 | `CMD:RLED_ON` |
| "关指示灯" | 红灯关 | `CMD:RLED_OFF` |
| "开窗" | 舵机180° | `CMD:WIN_OPEN` |
| "关窗" | 舵机0° | `CMD:WIN_CLOSE` |
| "打开风扇" | 继电器开 | `CMD:FAN_ON` |
| "关闭风扇" | 继电器关 | `CMD:FAN_OFF` |
| "拍照" | 1080p拍照 | `CMD:CAPTURE` |

> **硬件接线**: ASR PRO TX(PA_2) → GPIO15(RX), ASR PRO RX(PA_3) ← GPIO3(TX), 波特率115200  
> ⚠️ GPIO3与USB复用，烧录后需断开USB；GPIO16与PSRAM冲突不可用  
> 语音控制操作会通过MQTT推送到后端，记录操作日志并通过SSE实时推送到前端

---

### 🌡️ 温湿度监测 (DHT22)

| 功能 | 说明 | API/方式 |
|------|------|----------|
| **实时数据采集** | 可配置间隔(1-60秒) | SSE实时推送 `/mqtt/dht/sse/{clientId}` |
| **历史数据图表** | Chart.js可视化 | `GET /mqtt/dht/dashboard/{clientId}` |
| **采集间隔设置** | 远程配置 | `POST /mqtt/dht-interval/{clientId}` body: `{"interval": 5000}` |

---

### 📊 设备状态监控

| 功能 | 说明 | API/方式 |
|------|------|----------|
| **实时状态** | 运行时间/空闲内存/WiFi信号/分辨率 | SSE推送 `/mqtt/sse/{clientId}` |
| **状态历史图表** | RSSI和内存双Y轴折线图 | `GET /mqtt/status-history/chart/{clientId}` |
| **数据持久化** | 存入MySQL数据库 | 自动保存 |

**状态JSON结构：**
```json
{
  "clientId": "esp32cam",
  "uptime": 1234,
  "freeHeap": 152536,
  "rssi": -43,
  "ledStatus": false,
  "ledBrightness": 128,
  "redLedStatus": false,
  "servoAngle": 90,
  "relayStatus": false,
  "framesize": 11
}
```

---

### ⚙️ 设备配置管理

| 功能 | 说明 | API |
|------|------|-----|
| **WiFi配置** | 远程修改WiFi(自动重启) | `POST /mqtt/config/wifi/{clientId}` |
| **MQTT配置** | 修改Broker地址/端口 | `POST /mqtt/config/mqtt/{clientId}` |
| **上传URL设置** | 立即生效无需重启 | `POST /mqtt/config/upload-url/{clientId}` |
| **配置查询** | 获取当前配置 | `POST /mqtt/cam/{clientId}/get_config` |
| **恢复默认** | 重置为出厂配置 | `POST /mqtt/config/reset/{clientId}` |
| **状态上报间隔** | 10秒-5分钟 | `POST /mqtt/cam/{clientId}/set_status_interval` |

---

## 🌟 项目亮点 (Highlights)

### 1. 🖥️ 3D数字孪生与交互
- **Three.js 交互式场景**：构建了完整的 ESP32-CAM 硬件模型的 3D 数字孪生。
- **状态实时同步**：舵机角度、LED 状态、风扇转动等物理状态通过 MQTT -> SSE 毫秒级同步至 3D 模型。
- **👋 手势控制**：集成 MediaPipe Hands，通过电脑摄像头即可实现隔空手势交互（双指指向查看数据、张手旋转视角、捏合缩放）。

### 2. 🎨 现代可视化仪表盘
- **Glassmorphism 风格**：全深色磨砂玻璃 UI 设计，科技感十足。
- **实时图表监控**：集成 ECharts 展示温湿度和设备状态（内存/RSSI）历史趋势。
- **Dock 栏导航**：Mac 风格的底部控制栏，集成画质、AI、自动化等快捷入口。
- **实时天气集成**：顶部动态天气组件，实时获取当地气象数据。

### 3. 🧠 AI 深度融合
- **视觉问答**：集成 Qwen-VL 大模型，支持"拍照并分析"、"这是什么"等多轮对话。
- **智能自动化**：基于此时此刻的传感器数据（温度/湿度/光照）触发自动化场景（如：过热自动开风扇）。
- **ASR 语音控制**：硬件级离线语音识别，支持"打开风扇"、"开灯"等指令，并实时反馈至前端。

---

## 🏗️ 系统架构

```mermaid
graph TB
    subgraph "前端数字孪生"
        A[Vue 3 Dashboard]
        A1[Three.js 3D场景]
        A2[MediaPipe 手势识别]
        A3[ECharts 图表]
    end
    
    subgraph "Spring Boot 后端"
        B[REST API / SSE推送]
        C[业务逻辑 (Service)]
        D[MQTT Gateway]
        E[AI 整合 (Qwen-VL)]
    end
    
    subgraph "物理设备"
        F[ESP32-CAM]
        G[传感器 (DHT22/Light)]
        H[执行器 (Servo/Relay/LED)]
        I[ASR Pro 语音模块]
    end
    
    A <-->|HTTP/SSE| B
    B <--> D
    D <-->|MQTT| F
    F <--> G & H & I
    E -.->|API调用| B
```

---

## 📁 项目结构

```
IOT/
├── CameraWebServer/              # ESP32-CAM固件 (C++)
│   ├── CameraWebServer.ino       # 主循环
│   ├── camera_control.cpp        # 摄像头驱动
│   ├── mqtt_handler.cpp          # MQTT 双向通信
│   └── ... (模块化硬件驱动)
│
├── digital-twin-dashboard/       # 前端数字孪生 (Vue 3 + Vite)
│   ├── src/components/three/     # Three.js 3D场景
│   ├── src/components/panels/    # 监控与控制面板
│   ├── src/components/controls/  # 手势交互组件
│   └── src/stores/               # Pinia 状态管理
│
└── SpringbootIOT/                # 后端服务 (Java)
    ├── controller/               # API 接口
    ├── service/                  # 业务逻辑
    └── configuration/            # MQTT与定时任务配置
```

---

## 🔧 技术栈更新

### 前端 (digital-twin-dashboard)
- **核心框架**: Vue 3 (Composition API) + Vite
- **3D 引擎**: Three.js
- **图表库**: ECharts 5
- **AI 交互**: MediaPipe Hands (手势识别)
- **状态管理**: Pinia
- **通信**: SSE (Server-Sent Events) + Axios

### 后端 (SpringbootIOT)
- **框架**: Spring Boot 3.5.0
- **通信**: MQTT (Spring Integration), SSE
- **数据库**: MyBatis-Plus + MySQL
- **AI**: 阿里云 ModelScope SDK

---

## 📝 记住点 (Key Takeaways)

1.  **cmdId 生成机制**：Java 端生成 10 位数字 ID 以兼容 ESP32 的 32 位 long 类型。
2.  **HTTP/MQTT 隔离**：ESP32 中 MQTT 长连接与 HTTP 图片上传使用独立的 WiFiClient，互不干扰。
3.  **SSE 实时推送**：全链路摒弃轮询，状态变更由设备 -> MQTT -> 后端 -> SSE -> 前端，实现低延迟同步。
4.  **配置持久化**：ESP32 使用 Preferences 库将 WiFi 和 MQTT 配置存入 NVS Flash，掉电不丢失。
5.  **模块化设计**：固件端按功能（LED/Servo/Net）拆分 `.cpp`，前端按面板拆分 `.vue`，后端按领域拆分 Service，维护性极强。

---

## 📄 许可证

本项目仅供学习交流使用。
