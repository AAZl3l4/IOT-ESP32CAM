import { ref, onUnmounted } from 'vue'
import { Hands } from '@mediapipe/hands'
import { Camera } from '@mediapipe/camera_utils'

export function useHandGesture() {
    const isEnabled = ref(false)
    const isReady = ref(false)

    // MediaPipe对象不能是响应式的，否则WASM绑定会报错
    let hands = null
    let camera = null
    const videoRef = ref(null)

    // 手势状态
    const gestureState = ref({
        mode: 'IDLE', // IDLE, POINTING, ROTATING, ZOOMING
        cursor: { x: 0, y: 0 },
        dragDelta: { x: 0, y: 0 }, // 拖拽增量（旋转用）
        zoomDelta: 0, // 缩放增量
        handPresent: false
    })

    // 上一帧数据
    let lastPalmCenter = null
    let lastPinchDist = null

    // 回调函数
    let onGestureUpdate = null

    // 初始化 MediaPipe Hands
    const initHands = async (videoElement) => {
        videoRef.value = videoElement
        console.log('[HandGesture] 开始初始化...')

        try {
            hands = new Hands({
                locateFile: (file) => {
                    // 从本地public目录加载，加速首次加载
                    return `/mediapipe/hands/${file}`
                }
            })

            hands.setOptions({
                maxNumHands: 1,
                modelComplexity: 1,
                minDetectionConfidence: 0.5,
                minTrackingConfidence: 0.5
            })

            hands.onResults(onResults)
            console.log('[HandGesture] Hands模型已配置')

            if (videoElement) {
                camera = new Camera(videoElement, {
                    onFrame: async () => {
                        if (isEnabled.value && hands) {
                            try {
                                await hands.send({ image: videoElement })
                            } catch (e) {
                                // 忽略非关键错误
                            }
                        }
                    },
                    width: 320,
                    height: 240
                })

                console.log('[HandGesture] 启动摄像头...')
                await camera.start()
                isReady.value = true
                console.log('[HandGesture] 初始化完成，准备就绪')
            }
        } catch (error) {
            console.error('[HandGesture] 初始化失败:', error)
            alert('手势识别初始化失败，可能是网络问题无法加载模型，请检查控制台/F12日志。')
        }
    }

    // 处理手势结果
    const onResults = (results) => {
        if (results.multiHandLandmarks && results.multiHandLandmarks.length > 0) {
            const landmarks = results.multiHandLandmarks[0]
            gestureState.value.handPresent = true

            // 分析手势
            analyzeGesture(landmarks)

            // 调用更新回调
            if (onGestureUpdate) {
                onGestureUpdate(gestureState.value, landmarks)
            }
        } else {
            gestureState.value.handPresent = false
            gestureState.value.mode = 'IDLE'
            lastPalmCenter = null
            lastPinchDist = null
        }
    }

    // 手势分析逻辑
    const analyzeGesture = (lm) => {
        const thumbTip = lm[4]
        const indexTip = lm[8]
        const middleTip = lm[12]
        const ringTip = lm[16]
        const pinkyTip = lm[20]
        const palmCenter = lm[9]

        // 判断手指伸直
        const isIndexExtended = indexTip.y < lm[6].y
        const isMiddleExtended = middleTip.y < lm[10].y
        const isRingExtended = ringTip.y < lm[14].y
        const isPinkyExtended = pinkyTip.y < lm[18].y

        // 捏合距离
        const pinchDist = distance(thumbTip, indexTip)
        const isPinching = pinchDist < 0.10

        // 五指张开
        const isPalmOpen = isIndexExtended && isMiddleExtended && isRingExtended && isPinkyExtended && !isPinching

        // 双指指向（食指+中指伸出，其他弯曲）
        const isPointing = isIndexExtended && isMiddleExtended && !isRingExtended && !isPinkyExtended && !isPinching

        // === 状态转换 ===

        if (isPointing) {
            // 双指指向
            gestureState.value.mode = 'POINTING'
            gestureState.value.cursor = {
                x: 1 - (indexTip.x + middleTip.x) / 2,
                y: (indexTip.y + middleTip.y) / 2
            }
            lastPalmCenter = null
            lastPinchDist = null

        } else if (isPinching) {
            // 捏合 = 只缩放（不需要移动）
            gestureState.value.mode = 'ZOOMING'

            if (lastPinchDist !== null) {
                gestureState.value.zoomDelta = pinchDist - lastPinchDist
            } else {
                gestureState.value.zoomDelta = 0
            }
            gestureState.value.dragDelta = { x: 0, y: 0 }

            lastPinchDist = pinchDist
            lastPalmCenter = null

        } else if (isPalmOpen) {
            // 五指张开 = 移动旋转
            gestureState.value.mode = 'ROTATING'

            const currentPalmCenter = { x: 1 - palmCenter.x, y: palmCenter.y }

            if (lastPalmCenter) {
                gestureState.value.dragDelta = {
                    x: currentPalmCenter.x - lastPalmCenter.x,
                    y: currentPalmCenter.y - lastPalmCenter.y
                }
            } else {
                gestureState.value.dragDelta = { x: 0, y: 0 }
            }
            gestureState.value.zoomDelta = 0

            lastPalmCenter = currentPalmCenter
            lastPinchDist = null

        } else {
            gestureState.value.mode = 'IDLE'
            lastPalmCenter = null
            lastPinchDist = null
        }
    }

    const distance = (p1, p2) => {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2))
    }

    const start = () => {
        isEnabled.value = true
    }

    const stop = () => {
        isEnabled.value = false
        lastPalmCenter = null
        lastPinchDist = null
    }

    const setOnApiUpdate = (callback) => {
        onGestureUpdate = callback
    }

    onUnmounted(() => {
        if (camera) {
            camera.stop()
        }
        if (hands) {
            hands.close()
        }
    })

    return {
        isEnabled,
        isReady,
        gestureState,
        initHands,
        start,
        stop,
        setOnApiUpdate
    }
}
