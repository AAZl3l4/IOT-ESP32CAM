<script setup>
import { ref, onMounted, watch } from 'vue'
import { useHandGesture } from '@/composables/useHandGesture'
import { drawConnectors, drawLandmarks } from '@mediapipe/drawing_utils'
import { HAND_CONNECTIONS } from '@mediapipe/hands'

const props = defineProps({
  active: Boolean
})

const emit = defineEmits(['update'])

const videoRef = ref(null)
const canvasRef = ref(null)
const { isReady, isEnabled, gestureState, initHands, start, stop, setOnApiUpdate } = useHandGesture()

// 手势模式对应的中文
const modeLabels = {
  'IDLE': '等待手势...',
  'POINTING': '✌️ 双指指向',
  'ROTATING': '🖐️ 旋转视角',
  'ZOOMING': '🤏 缩放视角'
}

onMounted(async () => {
  await initHands(videoRef.value)
  
  // 监听手势更新
  setOnApiUpdate((state, landmarks) => {
    try {
      if (props.active) {
          emit('update', state)
      }
      
      // 有手时绘制骨骼，没手时清空
      if (state.handPresent && landmarks) {
        drawResults(landmarks)
      } else {
        clearCanvas()
      }
    } catch (e) {
      console.error('[GestureControls] Error in update callback:', e)
    }
  })
})

// 监听激活状态
watch(() => props.active, (val) => {
  if (val) {
    start()
  } else {
    stop()
    // 清空画布
    clearCanvas()
  }
})

// 绘制骨架
function drawResults(landmarks) {
  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')
  if (!canvas || !ctx) return

  ctx.save()
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  
  // 绘制手部连接线
  drawConnectors(ctx, landmarks, HAND_CONNECTIONS, {
    color: '#00f2ff',
    lineWidth: 2
  })
  
  // 绘制关键点
  drawLandmarks(ctx, landmarks, {
    color: '#ff0000',
    lineWidth: 1,
    radius: 3
  })
  
  ctx.restore()
}

// 清空画布
function clearCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (ctx) {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
  }
}
</script>

<template>
  <div class="gesture-controls" v-show="active">
    <div class="camera-preview">
      <!-- 视频层 -->
      <video ref="videoRef" class="input_video" autoplay playsinline></video>
      <!-- 骨架绘制层 -->
      <canvas ref="canvasRef" class="output_canvas" width="320" height="240"></canvas>
      
      <!-- 状态标签 -->
      <div class="status-badge" :class="gestureState.mode.toLowerCase()">
        {{ modeLabels[gestureState.mode] }}
      </div>
    </div>
    
    <div class="gesture-guide">
      <div class="guide-item">✌️ 双指指向</div>
      <div class="guide-item">🖐️ 张手移动旋转</div>
      <div class="guide-item">🤏 捏合缩放</div>
    </div>
  </div>
</template>

<style scoped>
.gesture-controls {
  position: absolute;
  top: 80px;
  right: 20px;
  z-index: 100;
  background: rgba(0, 10, 30, 0.8);
  border: 1px solid rgba(0, 242, 255, 0.3);
  border-radius: 12px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  backdrop-filter: blur(10px);
  box-shadow: 0 0 20px rgba(0, 242, 255, 0.1);
}

.camera-preview {
  position: relative;
  width: 160px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  background: #000;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.input_video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1); /* 镜像显示 */
}

.output_canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  transform: scaleX(-1); /* 镜像显示 */
  pointer-events: none;
}

.status-badge {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  font-size: 10px;
  padding: 4px;
  text-align: center;
  font-weight: bold;
}

.status-badge.pointing { color: #00ff00; }
.status-badge.rotating { color: #00f2ff; }
.status-badge.zooming { color: #ff00ff; }

.gesture-guide {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.guide-item {
  color: #aaa;
  font-size: 10px;
  padding: 2px 4px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 4px;
}
</style>
