<script setup>
/**
 * 拍照预览弹窗
 */
import { computed } from 'vue'
import { BASE_URL } from '@/config/api'
import ModalDialog from '@/components/common/ModalDialog.vue'

const props = defineProps({
  visible: Boolean,
  imageUrl: String,
  imageFile: String
})

const emit = defineEmits(['close', 'analyze'])

function analyze() {
    emit('analyze', props.imageFile)
}
</script>

<template>
  <ModalDialog :visible="visible" title="📸 拍照预览" @close="$emit('close')" width="800px">
    <div class="preview-content">
      <div class="image-container">
        <img v-if="imageUrl" :src="imageUrl" alt="Preview">
      </div>
      <div class="preview-actions">
        <button class="btn btn-secondary" @click="$emit('close')">关闭</button>
        <button class="btn btn-primary" @click="analyze">🤖 AI 智能分析</button>
      </div>
    </div>
  </ModalDialog>
</template>

<style scoped>
.preview-content {
    display: flex;
    flex-direction: column;
    gap: 15px;
    align-items: center;
}

.image-container {
    width: 100%;
    min-height: 300px;
    background: #000;
    border-radius: 8px;
    overflow: hidden;
    display: flex;
    justify-content: center;
    align-items: center;
}

.image-container img {
    max-width: 100%;
    max-height: 60vh;
    object-fit: contain;
}

.preview-actions {
    display: flex;
    gap: 15px;
}

.btn {
    padding: 10px 25px;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    font-weight: bold;
    font-size: 14px;
}

.btn-secondary {
    background: #444;
    color: white;
}

.btn-primary {
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: white;
}
</style>
