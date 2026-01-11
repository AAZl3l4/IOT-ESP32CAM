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
  // Initial position: Inside Center Stage (Right Sidebar 320 + Gaps + Window 180 + Padding)
  windowPos.value = {
    x: window.innerWidth - 580, 
    y: 120
  }

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
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })
const windowPos = ref({ x: 20, y: 80 }) // Initial Right, Top
const windowRef = ref(null)

function startDrag(e) {
  isDragging.value = true
  const rect = windowRef.value.getBoundingClientRect()
  // Calculate offset from top-right corner (since we position using top/right usually, or let's switch to top/left for easier dragging logic)
  // Actually fixed positioning using left/top is standard for draggable.
  // Initial CSS is right: 20px. I should switch to style binding for left/top.
  
  // Let's use left/top for positioning to make dragging easier.
  // On first drag, convert current right/top to left/top if needed? 
  // Simpler: Just bind style { top, left }. 
  // Initial pos: Right 20, Top 80. Screen width - 20 - width = Left.
  
  dragOffset.value = {
    x: e.clientX - rect.left,
    y: e.clientY - rect.top
  }
  
  window.addEventListener('mousemove', onDrag)
  window.addEventListener('mouseup', stopDrag)
}

function onDrag(e) {
  if (!isDragging.value) return
  windowPos.value = {
    x: e.clientX - dragOffset.value.x,
    y: e.clientY - dragOffset.value.y
  }
}

function stopDrag() {
  isDragging.value = false
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', stopDrag)
}

// Initial position calculation logic might be needed if we want to respect CSS initially.
// But binding :style is easier. We'll default to a fixed position.

</script>

<template>
  <div 
    class="gesture-controls" 
    v-show="active"
    ref="windowRef"
    :style="{ left: windowPos.x + 'px', top: windowPos.y + 'px' }"
  >
    <!-- 拖拽手柄 -->
    <div class="drag-handle" @mousedown="startDrag">
        <span>🖐️ 手势交互</span>
        <div class="window-controls">:::</div>
    </div>

    <div class="content-wrapper">
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
          <span>✌️ 指向</span>
          <span>🖐️ 旋转</span>
          <span>🤏 缩放</span>
        </div>
    </div>
  </div>
</template>

<style scoped>
.gesture-controls {
  position: fixed; /* Changed to fixed for easier dragging */
  z-index: 1000;
  background: rgba(0, 10, 30, 0.85);
  border: 1px solid rgba(0, 242, 255, 0.3);
  border-radius: 12px;
  backdrop-filter: blur(12px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
  
  /* Resizable */
  resize: both;
  overflow: hidden;
  width: 180px;  /* Restore default compact size */
  min-width: 180px;
  min-height: 160px;
  display: flex;
  flex-direction: column;
}

.drag-handle {
    padding: 6px 10px;
    background: rgba(0, 242, 255, 0.1);
    cursor: move;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 12px;
    color: var(--theme-primary);
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    user-select: none;
}

.window-controls {
    color: rgba(255, 255, 255, 0.3);
    font-size: 10px;
    letter-spacing: 2px;
}

.content-wrapper {
    padding: 10px;
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;
    overflow: hidden;
}

.camera-preview {
  position: relative;
  width: 100%;
  flex: 1;
  border-radius: 8px;
  overflow: hidden;
  background: #000;
  border: 1px solid rgba(255, 255, 255, 0.1);
  min-height: 100px;
}

.input_video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1);
}

.output_canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  transform: scaleX(-1);
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
  justify-content: space-around;
  align-items: center;
  background: rgba(255, 255, 255, 0.05);
  padding: 4px 6px;
  border-radius: 4px;
  font-size: 10px;
  color: #ccc;
  white-space: nowrap;
}

/* Custom Scrollbar for overflow content if needed */
::-webkit-scrollbar { width: 4px; height: 4px; }
::-webkit-scrollbar-thumb { background: rgba(0, 242, 255, 0.3); border-radius: 2px; }
</style>
