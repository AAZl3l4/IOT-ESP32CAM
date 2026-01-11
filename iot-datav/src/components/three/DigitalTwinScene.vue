<script setup>
/**
 * 3D数字孪生场景
 * 使用Three.js创建物联网设备的3D可视化
 */
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useDeviceStore } from '@/stores/deviceStore'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls'
import GestureControls from '@/components/controls/GestureControls.vue'

const store = useDeviceStore()
const containerRef = ref(null)


// 手势控制状态
const isGestureActive = ref(false)
const gestureCursor = ref({ x: 0, y: 0, visible: false })

// 切换手势控制
function toggleGesture() {
  isGestureActive.value = !isGestureActive.value
  if (isGestureActive.value && controls) {
    // 开启手势时关闭自动旋转
    controls.autoRotate = false
    isAutoRotating.value = false
  }
}

// 处理手势更新
function handleGestureUpdate(state) {
  if (!controls || !camera) return

  // 1. 旋转模式（捏合移动 或 五指张开移动）
  if (state.mode === 'ROTATING') {
    const deltaX = state.dragDelta.x
    const deltaY = state.dragDelta.y
    
    const deadZone = 0.003
    
    if (Math.abs(deltaX) > deadZone || Math.abs(deltaY) > deadZone) {
      const offset = new THREE.Vector3().copy(camera.position).sub(controls.target)
      const spherical = new THREE.Spherical().setFromVector3(offset)
      
      const speed = 4.0 // 旋转灵敏度
      
      spherical.theta -= deltaX * speed // 左右旋转
      spherical.phi += deltaY * speed   // 上下旋转
      
      // 限制角度
      spherical.phi = Math.max(0.1, Math.min(Math.PI - 0.1, spherical.phi))
      
      offset.setFromSpherical(spherical)
      camera.position.copy(controls.target).add(offset)
      camera.lookAt(controls.target)
      controls.update()
    }
  }

  // 2. 缩放模式（捏合张开/收紧）
  if (state.mode === 'ZOOMING') {
    const zoomDelta = state.zoomDelta
    
    if (Math.abs(zoomDelta) > 0.002) {
      const offset = new THREE.Vector3().copy(camera.position).sub(controls.target)
      const currentDist = offset.length()
      
      // 距离变大(张开) -> 放大(拉近)，距离变小(捏紧) -> 缩小(拉远)
      const scaleFactor = 1 - zoomDelta * 8 // 缩放灵敏度
      const newDist = currentDist * scaleFactor
      
      if (newDist > controls.minDistance && newDist < controls.maxDistance) {
        offset.setLength(newDist)
        camera.position.copy(controls.target).add(offset)
        controls.update()
      }
    }
  }

  // 3. 双指指向交互
  if (state.mode === 'POINTING') {
    // 映射手势坐标(0-1)到设备坐标(-1到1)
    mouse.x = state.cursor.x * 2 - 1
    mouse.y = -(state.cursor.y * 2) + 1
    
    // 更新光圈和Tooltip位置
    if (containerRef.value) {
      const rect = containerRef.value.getBoundingClientRect()
      const x = state.cursor.x * rect.width
      const y = state.cursor.y * rect.height
      
      // 更新光圈位置
      gestureCursor.value = { x: x, y: y, visible: true }
      
      if (tooltipRef.value) {
        tooltipRef.value.style.left = `${x}px`
        tooltipRef.value.style.top = `${y}px`
      }
    }
    
    checkIntersection()
  } else {
    // 隐藏光圈
    gestureCursor.value.visible = false
  }
}

// Three.js 对象
let scene, camera, renderer, controls
let animationId = null
let isAutoRotating = ref(true)

// 交互相关
const raycaster = new THREE.Raycaster()
const mouse = new THREE.Vector2()
const tooltipRef = ref(null)

// 设备模型引用
let esp32Model = null
let ledLight = null
let redLedLight = null
let servoArm = null
let fanBlade = null
let dht22Model = null
let lightSensorModel = null
let asrModel = null
let relayGroup = null // 继电器组引用

// 初始化场景
function initScene() {
  const container = containerRef.value
  const width = container.clientWidth
  const height = container.clientHeight
  
  // 场景
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x050810)
  scene.fog = new THREE.FogExp2(0x050810, 0.008)
  
  // 相机
  camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000)
  camera.position.set(15, 12, 15)
  camera.lookAt(0, 0, 0)
  
  // 渲染器
  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(window.devicePixelRatio)
  renderer.shadowMap.enabled = true
  container.appendChild(renderer.domElement)
  
  // 轨道控制器
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.autoRotate = true
  controls.autoRotateSpeed = 0.5
  controls.minDistance = 8
  controls.maxDistance = 40
  controls.maxPolarAngle = Math.PI / 2
  
  // 灯光
  const ambientLight = new THREE.AmbientLight(0x404050, 0.8)
  scene.add(ambientLight)
  
  const dirLight = new THREE.DirectionalLight(0xffffff, 1)
  dirLight.position.set(10, 20, 10)
  dirLight.castShadow = true
  scene.add(dirLight)
  
  const pointLight = new THREE.PointLight(0x00f2ff, 0.5, 30)
  pointLight.position.set(0, 10, 0)
  scene.add(pointLight)
  
  // 地面网格
  const gridHelper = new THREE.GridHelper(40, 40, 0x1a3a5c, 0x0a1a2c)
  scene.add(gridHelper)
  
  // 创建设备模型
  createDevices()
  
  // 事件监听
  window.addEventListener('resize', onWindowResize)
  container.addEventListener('mousemove', onMouseMove)
  
  // 开始动画循环
  animate()
}

// 鼠标移动处理
function onMouseMove(event) {
    if (!containerRef.value) return
    
    const rect = containerRef.value.getBoundingClientRect()
    mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
    mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
    
    // 更新Tooltip位置
    if (tooltipRef.value && tooltipRef.value.style.display !== 'none') {
        tooltipRef.value.style.left = `${event.clientX - rect.left}px`
        tooltipRef.value.style.top = `${event.clientY - rect.top}px`
    }
}

// 射线检测
function checkIntersection() {
    raycaster.setFromCamera(mouse, camera)
    
    // 检测DHT22
    if (dht22Model) {
        const intersects = raycaster.intersectObject(dht22Model, true)
        if (intersects.length > 0) {
            // 显示Tooltip
            if (tooltipRef.value) tooltipRef.value.style.display = 'block'
            document.body.style.cursor = 'pointer'
        } else {
            // 隐藏Tooltip
            if (tooltipRef.value) tooltipRef.value.style.display = 'none'
            document.body.style.cursor = 'default'
        }
    }
}

// 创建所有设备模型
function createDevices() {
  // 创建基座平台
  const platformGeo = new THREE.CylinderGeometry(6, 6.5, 0.3, 32)
  const platformMat = new THREE.MeshPhongMaterial({ 
    color: 0x1a2a3a,
    transparent: true,
    opacity: 0.8
  })
  const platform = new THREE.Mesh(platformGeo, platformMat)
  platform.position.y = -0.15
  platform.receiveShadow = true
  scene.add(platform)
  
  // ESP32-CAM (中心位置)
  esp32Model = createESP32CAM()
  esp32Model.position.set(0, 0.5, 0)
  scene.add(esp32Model)
  
  // DHT22 温湿度传感器
  dht22Model = createDHT22()
  dht22Model.position.set(-3, 0.4, -1)
  scene.add(dht22Model)
  
  // 舵机+窗户
  const servoGroup = createServoWindow()
  servoGroup.position.set(3, 0.3, -2)
  scene.add(servoGroup)
  
  // 继电器+风扇
  relayGroup = createRelayFan()
  relayGroup.position.set(-2, 0.3, 3)
  scene.add(relayGroup)
  
  // 光敏电阻
  lightSensorModel = createLightSensor()
  lightSensorModel.position.set(2, 0.3, 3)
  scene.add(lightSensorModel)
  
  // ASR PRO 语音模块
  asrModel = createASRPRO()
  asrModel.position.set(0, 0.4, -4)
  scene.add(asrModel)
  
  // 连接线路
  createWires()
}

// 创建 ESP32-CAM 模型
function createESP32CAM() {
  const group = new THREE.Group()
  
  // 1. PCB主板 (黑色哑光)
  const boardGeo = new THREE.BoxGeometry(2.7, 0.1, 4.0) // 27mm x 40.5mm 比例
  const boardMat = new THREE.MeshPhongMaterial({ color: 0x1a1a1a, shininess: 30 })
  const board = new THREE.Mesh(boardGeo, boardMat)
  board.castShadow = true
  group.add(board)

  // 2. 金属屏蔽罩 (ESP32-S芯片)
  const shieldGeo = new THREE.BoxGeometry(1.8, 0.15, 1.8)
  const shieldMat = new THREE.MeshStandardMaterial({ 
    color: 0xc0c0c0, 
    metalness: 0.9, 
    roughness: 0.4 
  })
  const shield = new THREE.Mesh(shieldGeo, shieldMat)
  shield.position.set(0, 0.12, -0.6)
  group.add(shield)
  
  // 3. 摄像头接口座 (FPC连接器)
  const fpcGeo = new THREE.BoxGeometry(1.2, 0.15, 0.5)
  const fpcMat = new THREE.MeshPhongMaterial({ color: 0xFFF8DC }) // 米白色
  const fpc = new THREE.Mesh(fpcGeo, fpcMat)
  fpc.position.set(0, 0.12, 1.2)
  group.add(fpc)

  // 4. 摄像头模块 (OV2640)
  const camModuleGroup = new THREE.Group()
  camModuleGroup.position.set(0, 0.4, 0.8) // 悬空连接
  
  // 软排线
  const flexCableGeo = new THREE.BoxGeometry(0.8, 0.02, 1.0)
  const flexCableMat = new THREE.MeshLambertMaterial({ color: 0xcc8800 }) // 金黄色
  const flexCable = new THREE.Mesh(flexCableGeo, flexCableMat)
  flexCable.position.set(0, -0.2, 0.2)
  flexCable.rotation.x = -0.3
  camModuleGroup.add(flexCable)

  // 摄像头本体
  const camBodyGeo = new THREE.BoxGeometry(0.8, 0.1, 0.8)
  const camBodyMat = new THREE.MeshPhongMaterial({ color: 0x111111 })
  const camBody = new THREE.Mesh(camBodyGeo, camBodyMat)
  camModuleGroup.add(camBody)

  // 镜头
  const lensGeo = new THREE.CylinderGeometry(0.25, 0.25, 0.3, 16)
  const lensMat = new THREE.MeshPhongMaterial({ color: 0x000000, shininess: 100 })
  const lens = new THREE.Mesh(lensGeo, lensMat)
  lens.rotation.x = Math.PI / 2
  lens.position.z = 0.15
  camModuleGroup.add(lens)
  
  group.add(camModuleGroup)
  
  // 5. 闪光灯 LED (GPIO4) - 白色矩形
  const flashLedGeo = new THREE.BoxGeometry(0.3, 0.05, 0.3)
  const flashLedMat = new THREE.MeshPhongMaterial({ 
    color: 0xffffee,
    emissive: 0x222222 
  })
  const flashLed = new THREE.Mesh(flashLedGeo, flashLedMat)
  flashLed.position.set(0.6, 0.06, 1.6) // 右下角
  group.add(flashLed)
  
  // 闪光灯光源
  ledLight = new THREE.PointLight(0xffffee, 0, 8)
  ledLight.position.set(0.6, 0.5, 1.6)
  group.add(ledLight)
  
  // 6. 红色指示灯 (GPIO33) - 贴片LED (背面，但在模型中为了可见放在正面左下角反面对应位置)
  // ESP32-CAM的红色LED通常在背面，但为了可视化，我们做一个小的在正面或者做成透光效果
  // 这里做在板子背面稍微露出的地方
  const redLedGeo = new THREE.BoxGeometry(0.15, 0.05, 0.15)
  const redLedMat = new THREE.MeshPhongMaterial({ 
    color: 0xaa0000,
    emissive: 0x000000
  })
  const redLed = new THREE.Mesh(redLedGeo, redLedMat)
  redLed.position.set(-0.8, 0.06, 1.6) 
  group.add(redLed)
  
  // 红色指示灯光源
  redLedLight = new THREE.PointLight(0xff0000, 0, 2)
  redLedLight.position.set(-0.8, 0.2, 1.6)
  group.add(redLedLight)

  // 7. SD卡槽 (背面)
  const sdSlotGeo = new THREE.BoxGeometry(1.5, 0.15, 1.6)
  const sdSlotMat = new THREE.MeshStandardMaterial({ color: 0xc0c0c0, metalness: 0.8 })
  const sdSlot = new THREE.Mesh(sdSlotGeo, sdSlotMat)
  sdSlot.position.set(0, -0.12, 0.5)
  group.add(sdSlot)

  // 8. 左右排针 (黑色塑料底座 + 金色针脚)
  const headerGeo = new THREE.BoxGeometry(0.2, 0.2, 3.8)
  const headerMat = new THREE.MeshPhongMaterial({ color: 0x111111 })
  
  const leftHeader = new THREE.Mesh(headerGeo, headerMat)
  leftHeader.position.set(-1.25, -0.1, 0)
  group.add(leftHeader)
  
  const rightHeader = new THREE.Mesh(headerGeo, headerMat)
  rightHeader.position.set(1.25, -0.1, 0)
  group.add(rightHeader)
  
  // 针脚 (简化为每侧一排点)
  const pinGeo = new THREE.CylinderGeometry(0.03, 0.03, 0.4, 8)
  const pinMat = new THREE.MeshStandardMaterial({ color: 0xffd700, metalness: 1.0, roughness: 0.2 })
  
  for(let i=0; i<8; i++) {
     const z = -1.6 + i * 0.45
     // 左侧针脚
     const lp = new THREE.Mesh(pinGeo, pinMat)
     lp.position.set(-1.25, -0.3, z)
     group.add(lp)
     
     // 右侧针脚
     const rp = new THREE.Mesh(pinGeo, pinMat)
     rp.position.set(1.25, -0.3, z)
     group.add(rp)
  }

  // 标签
  addLabel(group, 'ESP32-CAM', 0, 1.2, 0)
  
  // 保存LED引用用于状态更新
  group.userData.flashLed = flashLed
  group.userData.redLed = redLed
  
  return group
}

// 创建 DHT22 温湿度传感器 (白色外壳 + 蓝色内芯)
function createDHT22() {
  const group = new THREE.Group()
  group.userData.type = 'dht22' // 用于射线检测
  
  // 1. 白色外壳
  const caseGeo = new THREE.BoxGeometry(1.5, 2.0, 0.6)
  const caseMat = new THREE.MeshPhongMaterial({ color: 0xffffff })
  const dhtCase = new THREE.Mesh(caseGeo, caseMat)
  dhtCase.castShadow = true
  group.add(dhtCase)
  
  // 2. 蓝色传感器网格区
  const sensorAreaGeo = new THREE.PlaneGeometry(1.2, 1.2)
  const sensorAreaMat = new THREE.MeshPhongMaterial({ color: 0x2196f3, side: THREE.DoubleSide })
  const sensorArea = new THREE.Mesh(sensorAreaGeo, sensorAreaMat)
  sensorArea.position.set(0, 0.2, 0.31)
  group.add(sensorArea)
  
  // 3. 栅格纹理 (使用线条模拟)
  const gridGroup = new THREE.Group()
  gridGroup.position.set(0, 0.2, 0.32)
  for(let i=0; i<6; i++) {
    const lineGeo = new THREE.BoxGeometry(1.1, 0.05, 0.01)
    const lineMat = new THREE.MeshBasicMaterial({ color: 0xdddddd })
    const line = new THREE.Mesh(lineGeo, lineMat)
    line.position.y = -0.5 + i * 0.2
    gridGroup.add(line)
  }
  group.add(gridGroup)
  
  // 4. 引脚 (4个)
  for(let i=0; i<4; i++) {
      const pinGeo = new THREE.CylinderGeometry(0.04, 0.04, 0.5, 8)
      const pinMat = new THREE.MeshStandardMaterial({ color: 0xc0c0c0, metalness: 0.8 })
      const pin = new THREE.Mesh(pinGeo, pinMat)
      pin.position.set(-0.45 + i * 0.3, -1.25, 0)
      group.add(pin)
  }
  
  // 标签
  addLabel(group, 'DHT22', 0, 1.3, 0)
  
  return group
}

// 创建舵机和窗户 (SG90蓝色半透明 + 旋转窗)
function createServoWindow() {
  const group = new THREE.Group()
  
  // 1. SG90 舵机本体 (蓝色半透明)
  const servoGeo = new THREE.BoxGeometry(1.2, 1.2, 0.6)
  const servoMat = new THREE.MeshPhongMaterial({ 
    color: 0x0055ff, 
    transparent: true, 
    opacity: 0.7 
  })
  const servo = new THREE.Mesh(servoGeo, servoMat)
  servo.castShadow = true
  group.add(servo)
  
  // 舵机耳朵 (固定孔)
  const earGeo = new THREE.BoxGeometry(1.6, 0.1, 0.6)
  const ear = new THREE.Mesh(earGeo, servoMat)
  ear.position.y = 0.2
  group.add(ear)

  // 2. 输出轴
  const axleGeo = new THREE.CylinderGeometry(0.15, 0.15, 0.3, 16)
  const axleMat = new THREE.MeshStandardMaterial({ color: 0xffffff })
  const axle = new THREE.Mesh(axleGeo, axleMat)
  axle.position.set(0, 0.7, 0.15) // 偏心输出
  group.add(axle)
  
  // 3. 摇臂 (白色单臂)
  const armGroup = new THREE.Group()
  armGroup.position.set(0, 0.8, 0.15) // 旋转中心
  
  const armGeo = new THREE.BoxGeometry(0.2, 0.1, 1.0)
  const armMat = new THREE.MeshStandardMaterial({ color: 0xffffff })
  const arm = new THREE.Mesh(armGeo, armMat)
  arm.position.z = 0.4
  armGroup.add(arm)
  
  // 4. 窗户框架 (连接在摇臂上)
  // 窗户应该立在旁边，舵机旋转推开窗户，简单起见，我们让窗户直接固定在摇臂上旋转，模拟"推窗"
  const windowGroup = new THREE.Group()
  windowGroup.position.set(0, 0, 0.8) // 连在摇臂末端
  
  // 窗框
  const frameGeo = new THREE.BoxGeometry(0.1, 2.0, 1.5) // 厚, 高, 宽
  const frameMat = new THREE.MeshPhongMaterial({ color: 0x5d4037 }) // 深褐色木纹
  const frame = new THREE.Mesh(frameGeo, frameMat)
  frame.position.set(0, 1.0, 0) // 立起来
  windowGroup.add(frame)
  
  // 玻璃
  const glassGeo = new THREE.BoxGeometry(0.05, 1.8, 1.3)
  const glassMat = new THREE.MeshPhongMaterial({ 
    color: 0xaaddff,
    transparent: true,
    opacity: 0.4,
    shininess: 90
  })
  const glass = new THREE.Mesh(glassGeo, glassMat)
  glass.position.set(0, 1.0, 0)
  windowGroup.add(glass)
  
  // 把手
  const handleGeo = new THREE.CylinderGeometry(0.05, 0.05, 0.3)
  const handleMat = new THREE.MeshStandardMaterial({ color: 0xc0c0c0 })
  const handle = new THREE.Mesh(handleGeo, handleMat)
  handle.rotation.x = Math.PI / 2
  handle.position.set(0.1, 1.0, 0.6)
  windowGroup.add(handle)
  
  armGroup.add(windowGroup)
  
  group.add(armGroup)
  servoArm = armGroup
  
  addLabel(group, '智能窗户(SG90)', 0, 1.5, 0)
  
  return group
}

// 创建继电器和风扇 (继电器带指示灯 + 4010风扇)
function createRelayFan() {
  const group = new THREE.Group()
  
  // 1. 继电器模块
  const relayBoardGeo = new THREE.BoxGeometry(1.8, 0.1, 1.2)
  const relayBoardMat = new THREE.MeshPhongMaterial({ color: 0x1a472a })
  const relayBoard = new THREE.Mesh(relayBoardGeo, relayBoardMat)
  relayBoard.castShadow = true
  group.add(relayBoard)
  
  // 蓝色继电器本体 (松乐)
  const relayBoxGeo = new THREE.BoxGeometry(0.8, 0.6, 0.7)
  const relayBoxMat = new THREE.MeshPhongMaterial({ color: 0x2196f3 })
  const relayBox = new THREE.Mesh(relayBoxGeo, relayBoxMat)
  relayBox.position.set(-0.2, 0.35, 0)
  group.add(relayBox)
  
  // 接线端子
  const terminalGeo = new THREE.BoxGeometry(0.6, 0.5, 0.4)
  const terminalMat = new THREE.MeshStandardMaterial({ color: 0x338833 })
  const terminal = new THREE.Mesh(terminalGeo, terminalMat)
  terminal.position.set(0.6, 0.3, 0)
  group.add(terminal)
  
  // 电源指示灯 (绿灯 - 常亮)
  const pwrLedGeo = new THREE.CylinderGeometry(0.04, 0.04, 0.05)
  const pwrLedMat = new THREE.MeshBasicMaterial({ color: 0x00ff00 }) // 始终绿色
  const pwrLed = new THREE.Mesh(pwrLedGeo, pwrLedMat)
  pwrLed.position.set(-0.7, 0.1, 0.3)
  group.add(pwrLed)
  
  // 状态指示灯 (红灯 - 吸合亮)
  const stsLedGeo = new THREE.CylinderGeometry(0.04, 0.04, 0.05)
  const stsLedMat = new THREE.MeshBasicMaterial({ color: 0x330000 }) // 默认暗
  const stsLed = new THREE.Mesh(stsLedGeo, stsLedMat)
  stsLed.position.set(-0.7, 0.1, -0.3)
  stsLed.userData.material = stsLedMat // 标记用于更新材质
  group.add(stsLed)
  
  // 3D状态更新时会寻找这个对象更新emissive或color
  stsLed.userData.target = 'relayLed'
  
  // 2. 风扇支架 (简化为连接线连接)
  // 假设风扇放在旁边，电源线连到继电器
  
  // 3. 4010风扇 (更真实的版本)
  const fanGroup = new THREE.Group()
  fanGroup.position.set(2.0, 0.6, 0)
  
  // 方形外框
  const frameThickness = 0.08
  const frameSize = 1.2
  const frameDepth = 0.35
  const frameMat = new THREE.MeshStandardMaterial({ color: 0x1a1a1a, roughness: 0.7 })
  
  // 四条边框
  const edgeGeoH = new THREE.BoxGeometry(frameSize, frameThickness, frameDepth)
  const edgeGeoV = new THREE.BoxGeometry(frameThickness, frameSize - frameThickness * 2, frameDepth)
  
  const edgeTop = new THREE.Mesh(edgeGeoH, frameMat)
  edgeTop.position.y = frameSize / 2 - frameThickness / 2
  fanGroup.add(edgeTop)
  
  const edgeBottom = new THREE.Mesh(edgeGeoH, frameMat)
  edgeBottom.position.y = -frameSize / 2 + frameThickness / 2
  fanGroup.add(edgeBottom)
  
  const edgeLeft = new THREE.Mesh(edgeGeoV, frameMat)
  edgeLeft.position.x = -frameSize / 2 + frameThickness / 2
  fanGroup.add(edgeLeft)
  
  const edgeRight = new THREE.Mesh(edgeGeoV, frameMat)
  edgeRight.position.x = frameSize / 2 - frameThickness / 2
  fanGroup.add(edgeRight)
  
  // 中心轴承座 (圆形白色)
  const hubGeo = new THREE.CylinderGeometry(0.18, 0.18, 0.38, 24)
  const hubMat = new THREE.MeshStandardMaterial({ color: 0xeeeeee, roughness: 0.3 })
  const hub = new THREE.Mesh(hubGeo, hubMat)
  hub.rotation.x = Math.PI / 2
  fanGroup.add(hub)
  
  // 扇叶组 (绕Z轴旋转)
  const bladeGroup = new THREE.Group()
  
  // 7片扇叶 - 使用扁平的造型
  const bladeCount = 7
  for(let i = 0; i < bladeCount; i++) {
      const angle = (i / bladeCount) * Math.PI * 2
      
      // 扇叶形状：扁平长条，有倾斜
      const bladeGeo = new THREE.BoxGeometry(0.4, 0.08, 0.04)
      const bladeMat = new THREE.MeshStandardMaterial({ 
          color: 0x888888,
          side: THREE.DoubleSide
      })
      const blade = new THREE.Mesh(bladeGeo, bladeMat)
      
      // 位置在中心向外
      blade.position.x = Math.cos(angle) * 0.32
      blade.position.y = Math.sin(angle) * 0.32
      
      // 旋转：径向排列 + 攻角
      blade.rotation.z = angle
      blade.rotation.x = 0.4 // 攻角让风扇能吹风
      
      bladeGroup.add(blade)
  }
  
  fanGroup.add(bladeGroup)
  fanBlade = bladeGroup // 用于动画旋转
  
  group.add(fanGroup)
  
  addLabel(group, '风扇控制', 1.0, 1.5, 0)
  
  // 给状态指示灯引用存一下方便更新
  // 由于结构变化，我们需要在updateDeviceStates里找这个对象
  // 为了方便，直接挂在return的group上
  group.userData.stsLed = stsLed
  
  return group
}

// 创建光敏传感器 (PCB + 蛇形光敏电阻)
function createLightSensor() {
  const group = new THREE.Group()
  group.userData.type = 'lightSensor'
  
  // 1. PCB板
  const pcbGeo = new THREE.BoxGeometry(0.8, 0.1, 1.2)
  const pcbMat = new THREE.MeshPhongMaterial({ color: 0x1e3a5f })
  const pcb = new THREE.Mesh(pcbGeo, pcbMat)
  group.add(pcb)
  
  // 2. 光敏电阻头
  const sensorHeadGeo = new THREE.CylinderGeometry(0.25, 0.25, 0.1, 32)
  const sensorHeadMat = new THREE.MeshPhongMaterial({ color: 0xeeeeee })
  const sensorHead = new THREE.Mesh(sensorHeadGeo, sensorHeadMat)
  sensorHead.position.set(0, 0.1, 0.3)
  sensorHead.rotation.x = 0.2 // 稍微倾斜
  group.add(sensorHead)
  
  // 3. 蛇形纹理 (橙色线条模拟)
  const lineGeo = new THREE.BoxGeometry(0.3, 0.01, 0.02)
  const lineMat = new THREE.MeshBasicMaterial({ color: 0xff4400 })
  for(let i=0; i<3; i++) {
      const line = new THREE.Mesh(lineGeo, lineMat)
      line.position.set(0, 0.16, 0.2 + i * 0.08)
      line.rotation.x = 0.2
      group.add(line)
  }
  
  // 4. 电位器 (蓝色方块+十字)
  const potGeo = new THREE.BoxGeometry(0.25, 0.2, 0.25)
  const potMat = new THREE.MeshPhongMaterial({ color: 0x2196f3 })
  const pot = new THREE.Mesh(potGeo, potMat)
  pot.position.set(0, 0.15, -0.3)
  group.add(pot)
  
  const crossGeo1 = new THREE.BoxGeometry(0.15, 0.02, 0.04)
  const crossMat = new THREE.MeshStandardMaterial({ color: 0xffffff })
  const cross1 = new THREE.Mesh(crossGeo1, crossMat)
  cross1.position.set(0, 0.26, -0.3)
  group.add(cross1)
  
  const cross2 = cross1.clone()
  cross2.rotation.y = Math.PI / 2
  group.add(cross2)
  
  // 5. 芯片
  const chipGeo = new THREE.BoxGeometry(0.3, 0.08, 0.2)
  const chipMat = new THREE.MeshPhongMaterial({ color: 0x111111 })
  const chip = new THREE.Mesh(chipGeo, chipMat)
  chip.position.set(0, 0.1, -0.05)
  group.add(chip)
  
  addLabel(group, '光敏传感器', 0, 0.8, 0)
  
  return group
}

// 创建 ASR PRO 语音模块 (黑色板子 + 喇叭)
function createASRPRO() {
  const group = new THREE.Group()
  
  // 1. ASR 主板 (TW-ASR01)
  const boardGeo = new THREE.BoxGeometry(1.5, 0.1, 2.0)
  const boardMat = new THREE.MeshPhongMaterial({ color: 0x111111 }) // 黑色沉金
  const board = new THREE.Mesh(boardGeo, boardMat)
  board.castShadow = true
  group.add(board)
  
  // 2. 核心芯片
  const chipGeo = new THREE.BoxGeometry(0.7, 0.05, 0.7)
  const chipMat = new THREE.MeshPhongMaterial({ color: 0x222222 })
  const chip = new THREE.Mesh(chipGeo, chipMat)
  chip.position.set(0, 0.08, 0)
  group.add(chip)
  
  // 3. 麦克风 (银色圆柱)
  const micGeo = new THREE.CylinderGeometry(0.15, 0.15, 0.15, 16)
  const micMat = new THREE.MeshStandardMaterial({ color: 0xc0c0c0, metalness: 1.0 })
  const mic = new THREE.Mesh(micGeo, micMat)
  mic.position.set(0, 0.1, 0.7)
  group.add(mic)
  
  // 4. 板载指示灯 (红灯 - 同步ESP32指示灯)
  const ledGeo = new THREE.SphereGeometry(0.08, 16, 16)
  const ledMat = new THREE.MeshBasicMaterial({ color: 0x330000 }) // 默认暗
  const led = new THREE.Mesh(ledGeo, ledMat)
  led.position.set(0.5, 0.1, 0.6)
  led.userData.material = ledMat
  group.add(led)
  
  // 关联到更新逻辑
  led.userData.target = 'asrLed'
  // 记录到group上方便查找
  group.userData.asrLed = led
  
  // 5. 喇叭 (放置在旁边)
  const speakerGroup = new THREE.Group()
  speakerGroup.position.set(1.5, 0.5, 0)
  
  // 磁体
  const magnetGeo = new THREE.CylinderGeometry(0.4, 0.4, 0.3, 32)
  const magnetMat = new THREE.MeshStandardMaterial({ color: 0xcccccc, metalness: 0.8 })
  const magnet = new THREE.Mesh(magnetGeo, magnetMat)
  magnet.rotation.x = Math.PI / 2
  speakerGroup.add(magnet)
  
  // 盆架
  const basketGeo = new THREE.CylinderGeometry(0.8, 0.4, 0.4, 32, 1, true)
  const basketMat = new THREE.MeshStandardMaterial({ color: 0x222222, side: THREE.DoubleSide })
  const basket = new THREE.Mesh(basketGeo, basketMat)
  basket.rotation.x = Math.PI / 2
  basket.position.z = 0.35
  speakerGroup.add(basket)
  
  // 纸盆
  const coneGeo = new THREE.ConeGeometry(0.7, 0.2, 32, 1, true)
  const coneMat = new THREE.MeshStandardMaterial({ color: 0x333333, roughness: 1.0 })
  const cone = new THREE.Mesh(coneGeo, coneMat)
  cone.rotation.x = -Math.PI / 2
  cone.position.z = 0.4
  speakerGroup.add(cone)
  
  group.add(speakerGroup)
  
  addLabel(group, 'ASR PRO', 0, 1.0, 0)
  
  return group
}

// 创建连接线路 (点对点自然连线)
function createWires() {
  // 定义颜色
  const C_VCC = 0xff0000
  const C_GND = 0x222222
  const C_SIG = 0x00ff00 // 默认信号线绿色
  const C_RX = 0xffff00
  const C_TX = 0x00ffff
  
  // ESP32-CAM 引脚位置 (相对 group 中心)
  // 板子中心在 (0, 0.5, 0)
  // 左排针 x = -1.25, 右排针 x = 1.25
  // z范围 -1.6 到 1.2
  
  const espPos = new THREE.Vector3(0, 0.5, 0)
  
  // 辅助函数：获取ESP32引脚绝对位置
  const getPinPos = (side, index) => {
      // side: -1左, 1右
      // index: 0-7, 从后往前
      const x = side * 1.25
      const y = 0
      const z = -1.6 + index * 0.45
      return new THREE.Vector3(x, y, z).add(espPos)
  }

  // 1. DHT22连线 (位置 -3, 0.4, -1)
  // VCC, DATA, NC, GND
  const dhtPos = new THREE.Vector3(-3, 0.4, -1)
  createSingleWire(getPinPos(-1, 0), dhtPos.clone().add(new THREE.Vector3(-0.45, -0.4, 0)), C_VCC) // 3.3V
  createSingleWire(getPinPos(-1, 3), dhtPos.clone().add(new THREE.Vector3(-0.15, -0.4, 0)), C_SIG) // IO14
  createSingleWire(getPinPos(-1, 1), dhtPos.clone().add(new THREE.Vector3(0.45, -0.4, 0)), C_GND) // GND

  // 2. 舵机连线 (位置 3, 0.3, -2)
  // GND, VCC, SIG
  const servoPos = new THREE.Vector3(3, 0.3, -2)
  createSingleWire(getPinPos(1, 1), servoPos.clone().add(new THREE.Vector3(-0.5, -0.2, 0)), C_GND) // GND
  createSingleWire(getPinPos(1, 0), servoPos.clone().add(new THREE.Vector3(0, -0.2, 0)), C_VCC)   // 5V
  createSingleWire(getPinPos(1, 4), servoPos.clone().add(new THREE.Vector3(0.5, -0.2, 0)), 0xffa500)   // IO13 (橙色)

  // 3. 继电器连线 (位置 -2, 0.3, 3)
  // VCC, GND, IN
  const relayPos = new THREE.Vector3(-2, 0.3, 3)
  createSingleWire(getPinPos(1, 0), relayPos.clone().add(new THREE.Vector3(0.6, 0.3, 0.2)), C_VCC) // 5V (从右侧取)
  createSingleWire(getPinPos(1, 1), relayPos.clone().add(new THREE.Vector3(0.6, 0.3, 0)), C_GND)   // GND
  createSingleWire(getPinPos(-1, 4), relayPos.clone().add(new THREE.Vector3(0.6, 0.3, -0.2)), C_SIG) // IO15

  // 4. 光敏连线 (位置 2, 0.3, 3)
  // VCC, GND, DO, AO
  const lightPos = new THREE.Vector3(2, 0.3, 3)
  createSingleWire(getPinPos(-1, 0), lightPos.clone().add(new THREE.Vector3(-0.35, 0.1, 0.2)), C_VCC) // 3.3V
  createSingleWire(getPinPos(1, 1), lightPos.clone().add(new THREE.Vector3(-0.35, 0.1, 0.1)), C_GND)  // GND
  createSingleWire(getPinPos(-1, 5), lightPos.clone().add(new THREE.Vector3(-0.35, 0.1, -0.1)), C_SIG) // IO12

  // 5. ASR PRO连线 (位置 0, 0.4, -4)
  // TX, RX, GND, VCC
  const asrPos = new THREE.Vector3(0, 0.4, -4)
  // ESP TX(1) -> ASR RX
  // ESP RX(3) -> ASR TX
  createSingleWire(getPinPos(1, 7), asrPos.clone().add(new THREE.Vector3(-0.6, 0.1, 0.9)), C_TX) // U0TX
  createSingleWire(getPinPos(1, 6), asrPos.clone().add(new THREE.Vector3(-0.4, 0.1, 0.9)), C_RX) // U0RX
  createSingleWire(getPinPos(1, 1), asrPos.clone().add(new THREE.Vector3(-0.2, 0.1, 0.9)), C_GND) // GND
  createSingleWire(getPinPos(1, 0), asrPos.clone().add(new THREE.Vector3(0, 0.1, 0.9)), C_VCC)    // 5V

  // 6. ASR PRO板子到喇叭的内部连线 (喇叭在asrPos偏移1.5, 0.5, 0处)
  const speakerPos = asrPos.clone().add(new THREE.Vector3(1.5, 0.5, 0))
  createSingleWire(asrPos.clone().add(new THREE.Vector3(0.5, 0.1, 0)), speakerPos.clone().add(new THREE.Vector3(-0.3, 0, 0)), 0xff6600) // SPK+
  createSingleWire(asrPos.clone().add(new THREE.Vector3(0.5, 0.1, -0.2)), speakerPos.clone().add(new THREE.Vector3(-0.3, 0, 0.2)), C_GND) // SPK-

  // 7. 继电器到风扇的内部连线 (风扇在relayPos偏移2.0, 0.6, 0处)
  const fanPos = relayPos.clone().add(new THREE.Vector3(2.0, 0.6, 0))
  createSingleWire(relayPos.clone().add(new THREE.Vector3(0.6, 0.4, 0)), fanPos.clone().add(new THREE.Vector3(-0.7, 0, 0)), C_VCC) // 风扇+
  createSingleWire(relayPos.clone().add(new THREE.Vector3(0.6, 0.4, -0.2)), fanPos.clone().add(new THREE.Vector3(-0.7, 0, 0.2)), C_GND) // 风扇-
}

// 创建单根导线
function createSingleWire(vStart, vEnd, color) {
    // 中间控制点，让线拱起来
    const mid = vStart.clone().add(vEnd).multiplyScalar(0.5)
    mid.y += 1.5 // 拱起高度
    
    // 随机偏移一点，避免重合
    mid.x += (Math.random() - 0.5) * 0.5
    mid.z += (Math.random() - 0.5) * 0.5

    const points = [vStart, mid, vEnd]
    const curve = new THREE.CatmullRomCurve3(points) // 平滑曲线
    
    const geometry = new THREE.TubeGeometry(curve, 24, 0.015, 8, false)
    const material = new THREE.MeshPhongMaterial({ color: color })
    const wire = new THREE.Mesh(geometry, material)
    scene.add(wire)
}

// 添加标签
function addLabel(parent, text, x, y, z) {
  // 使用CSS2D标签更好，这里简化处理
  // 实际项目中可以使用 CSS2DRenderer
}

// 窗口大小改变
function onWindowResize() {
  if (!containerRef.value) return
  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight
  
  camera.aspect = width / height
  camera.updateProjectionMatrix()
  renderer.setSize(width, height)
}

// 动画循环
function animate() {
  animationId = requestAnimationFrame(animate)
  
  controls.update()
  
  // 射线检测交互
  checkIntersection()
  
  // 风扇旋转动画
  if (fanBlade && store.controlState.relayStatus) {
    fanBlade.rotation.z += 0.3
  }
  
  // 窗户旋转动画 (每帧执行lerp实现平滑过渡)
  if (servoArm) {
    // servoAngle: 0-180度 -> rotation: 0-PI弧度
    const targetAngle = (store.controlState.servoAngle / 180) * Math.PI
    servoArm.rotation.y = THREE.MathUtils.lerp(servoArm.rotation.y, targetAngle, 0.1)
  }
  
  renderer.render(scene, camera)
}

// 更新设备状态到3D模型
function updateDeviceStates() {
  // LED闪光灯状态 (GPIO4)
  if (ledLight) {
    const isOn = store.controlState.ledStatus
    const brightness = isOn ? (store.controlState.ledBrightness / 255) * 3 : 0
    ledLight.intensity = brightness
    
    // 同步更新LED材质发光
    if (esp32Model && esp32Model.userData.flashLed) {
        const mat = esp32Model.userData.flashLed.material
        if (isOn) {
            mat.emissive.setHex(0xffffaa)
            mat.emissiveIntensity = brightness / 3
        } else {
            mat.emissive.setHex(0x000000)
            mat.emissiveIntensity = 0
        }
    }
  }
  
  // 红色指示灯 (ESP32 GPIO33)
  if (redLedLight) {
    const isOn = store.controlState.redLedStatus
    redLedLight.intensity = isOn ? 1.5 : 0
    
    // 同步更新红色LED材质
    if (esp32Model && esp32Model.userData.redLed) {
        const mat = esp32Model.userData.redLed.material
        if (isOn) {
            mat.emissive.setHex(0xff0000)
            mat.emissiveIntensity = 1
        } else {
            mat.emissive.setHex(0x000000)
            mat.emissiveIntensity = 0
        }
    }
  }

  // 红色指示灯 (ASR PRO 同步ESP32)
  if (asrModel && asrModel.userData.asrLed) {
      const isOn = store.controlState.redLedStatus
      const mat = asrModel.userData.asrLed.material
      mat.color.setHex(isOn ? 0xff0000 : 0x330000)
  }
  
  // 窗户旋转已移至animate()函数中实现平滑动画

  // 继电器状态指示灯
  if (relayGroup && relayGroup.userData.stsLed) {
      const isOn = store.controlState.relayStatus
      relayGroup.userData.stsLed.material.color.setHex(isOn ? 0xff0000 : 0x330000)
  }
}

// 切换自动旋转
function toggleAutoRotate() {
  isAutoRotating.value = !isAutoRotating.value
  if (controls) {
    controls.autoRotate = isAutoRotating.value
  }
}

// 重置视角
function resetCamera() {
  if (camera && controls) {
    camera.position.set(15, 12, 15)
    controls.target.set(0, 0, 0)
    controls.update()
  }
}

// 监听状态变化
watch(() => store.controlState, updateDeviceStates, { deep: true })

onMounted(() => {
  initScene()
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  window.removeEventListener('resize', onWindowResize)
  if (renderer) {
    renderer.dispose()
  }
})
</script>

<template>
  <div class="scene-container" ref="containerRef">
    <!-- 手势控制组件 -->
    <GestureControls :active="isGestureActive" @update="handleGestureUpdate" />

    <!-- 控制按钮 -->
    <div class="scene-controls">
      <button class="control-btn" @click="resetCamera" title="重置视角">
        🔄
      </button>
      <button
        class="control-btn"
        :class="{ active: isAutoRotating }"
        @click="toggleAutoRotate"
        title="自动旋转"
      >
        ⟳
      </button>
      <button
        class="control-btn"
        :class="{ active: isGestureActive }"
        @click="toggleGesture"
        title="手势控制"
      >
        🖐️
      </button>
    </div>

    <!-- 手势指向光圈 -->
    <div 
      v-show="isGestureActive && gestureCursor.visible" 
      class="gesture-cursor"
      :style="{ left: gestureCursor.x + 'px', top: gestureCursor.y + 'px' }"
    ></div>

    <!-- 一言浮层 -->
    <div class="scene-overlay">
      <div class="device-label">
        <span class="icon">🌐</span>
        ESP32-CAM 物联网数字孪生
      </div>
    </div>

    <!-- 传感器悬浮提示 -->
    <div ref="tooltipRef" class="sensor-tooltip" style="display: none;">
        <div class="tooltip-title">DHT22 传感器</div>
        <div class="tooltip-row">
            <span>🌡️ 温度:</span>
            <span class="value">{{ store.dhtHistory.temperatures.length ? store.dhtHistory.temperatures[store.dhtHistory.temperatures.length-1].toFixed(1) : '--' }}°C</span>
        </div>
        <div class="tooltip-row">
            <span>💧 湿度:</span>
            <span class="value">{{ store.dhtHistory.humidities.length ? store.dhtHistory.humidities[store.dhtHistory.humidities.length-1].toFixed(1) : '--' }}%</span>
        </div>
    </div>
  </div>
</template>

<style scoped>
.scene-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: radial-gradient(ellipse at center, #0d1a2d 0%, #050810 100%);
}

.scene-controls {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
  z-index: 10;
}

.control-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 242, 255, 0.2);
  color: var(--theme-primary);
  font-size: 18px;
  cursor: pointer;
  transition: 0.2s;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 242, 255, 0.3);
}

.control-btn:hover {
  background: rgba(0, 242, 255, 0.4);
  transform: scale(1.1);
}

.control-btn.active {
  background: var(--theme-primary);
  color: #000;
}

.scene-overlay {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  pointer-events: none;
}

.device-label {
  background: rgba(0, 20, 40, 0.8);
  backdrop-filter: blur(10px);
  padding: 8px 20px;
  border-radius: 20px;
  border: 1px solid rgba(0, 242, 255, 0.3);
  font-size: 12px;
  color: var(--theme-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.device-label .icon {
  font-size: 16px;
}

.sensor-tooltip {
  position: absolute;
  background: rgba(16, 30, 60, 0.9);
  border: 1px solid #00f2ff;
  border-radius: 8px;
  padding: 10px;
  color: #fff;
  font-size: 12px;
  pointer-events: none; /* 让鼠标事件透传 */
  z-index: 20;
  backdrop-filter: blur(5px);
  box-shadow: 0 0 15px rgba(0, 242, 255, 0.3);
  transform: translate(-50%, -100%); /* 居中并在上方 */
  margin-top: -15px;
}

.tooltip-title {
  font-weight: bold;
  color: #00f2ff;
  margin-bottom: 5px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  padding-bottom: 3px;
}

.tooltip-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 2px;
}

.tooltip-row .value {
  font-family: monospace;
  color: #ffeb3b;
}

/* 手势指向光圈 */
.gesture-cursor {
  position: absolute;
  width: 40px;
  height: 40px;
  border: 3px solid #00f2ff;
  border-radius: 50%;
  pointer-events: none;
  z-index: 100;
  transform: translate(-50%, -50%);
  box-shadow: 0 0 15px rgba(0, 242, 255, 0.6), inset 0 0 10px rgba(0, 242, 255, 0.3);
  animation: cursor-pulse 1s ease-in-out infinite;
}

.gesture-cursor::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 8px;
  height: 8px;
  background: #00f2ff;
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

@keyframes cursor-pulse {
  0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 1; }
  50% { transform: translate(-50%, -50%) scale(1.1); opacity: 0.8; }
}
</style>
