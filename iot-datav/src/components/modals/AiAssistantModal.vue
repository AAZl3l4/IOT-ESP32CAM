<script setup>
/**
 * AI助手弹窗
 * 移植自 test-panel.html "AI助手" 部分
 * 支持 Markdown 渲染
 */
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useDeviceStore } from '@/stores/deviceStore'
import { BASE_URL } from '@/config/api'
import ModalDialog from '@/components/common/ModalDialog.vue'
import { marked } from 'marked'

// 配置 marked 选项
marked.setOptions({
    breaks: true, // 支持换行
    gfm: true     // GitHub Flavored Markdown
})

// 渲染 Markdown 为 HTML
function renderMarkdown(content) {
    try {
        return marked.parse(content || '')
    } catch (e) {
        return content
    }
}

const props = defineProps({
  visible: Boolean
})

const emit = defineEmits(['close'])
const store = useDeviceStore()

const messages = ref([])

const inputText = ref('')
const loading = ref(false)
const chatContainer = ref(null)

const AI_SESSION_ID = 'esp32cam_session_' + Date.now()

// 添加消息
function addMessage(role, content) {
    messages.value.push({ role, content, time: new Date() })
    scrollToBottom()
}

function scrollToBottom() {
    nextTick(() => {
        if (chatContainer.value) {
            chatContainer.value.scrollTop = chatContainer.value.scrollHeight
        }
    })
}

async function sendAiMessage() {
    if (!inputText.value.trim()) return
    const msg = inputText.value
    inputText.value = ''
    
    addMessage('user', msg)
    loading.value = true
    addMessage('system', '📸 正在拍照并分析...')

    try {
        const res = await fetch(`${BASE_URL}/ai/chat/${store.clientId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                sessionId: AI_SESSION_ID,
                message: msg
            })
        }).then(r => r.json())
        
        if (res.code !== 0) {
            // 移除loading系统消息（简单处理：直接加一条错误）
            addMessage('error', '❌ ' + (res.msg || '请求失败'))
            loading.value = false
        }
        // 成功后等待SSE推送
    } catch (e) {
        addMessage('error', '❌ 网络错误')
        loading.value = false
    }
}

// 清空历史
async function clearHistory() {
    if (!confirm('确定清空历史？')) return
    messages.value = []
    await fetch(`${BASE_URL}/ai/history/${AI_SESSION_ID}`, { method: 'DELETE' })
}

// 外部调用的分析图片方法
async function analyzeImage(imageFile) {
    addMessage('system', '📸 正在分析图片...')
    loading.value = true
    
    try {
        const res = await fetch(`${BASE_URL}/ai/analyze`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                sessionId: AI_SESSION_ID,
                imageFile: imageFile,
                message: '请描述这张图片的内容，包括环境、光线、可见的物体等'
            })
        }).then(r => r.json())
        
        if (res.code !== 0) {
            addMessage('error', '❌ ' + (res.msg || '请求失败'))
            loading.value = false
        }
    } catch (e) {
        addMessage('error', '❌ 网络错误')
        loading.value = false
    }
}

defineExpose({ analyzeImage })

function handleAiEvent(e) {
    const data = e.detail
    loading.value = false
    if (data.imageFile) {
        addMessage('image', data.imageFile)
    }
    if (data.response) {
        addMessage('assistant', data.response)
    }
}

onMounted(() => {
    window.addEventListener('ai-response', handleAiEvent)
})

onUnmounted(() => {
    window.removeEventListener('ai-response', handleAiEvent)
})

</script>

<template>
  <ModalDialog :visible="visible" title="🤖 AI 视觉助手 (Qwen-VL)" @close="$emit('close')" width="500px">
    <div class="chat-layout">
        <div class="chat-history" ref="chatContainer">
            <div v-if="messages.length === 0" class="empty-state">
                💭 发送消息开始对话（会自动拍照分析）
            </div>
            
            <div v-for="(msg, idx) in messages" :key="idx" 
                class="message-row" :class="msg.role">
                
                <div v-if="msg.role === 'user'" class="bubble user">
                    <div class="role-label">👤 你</div>
                    {{ msg.content }}
                </div>
                
                <div v-if="msg.role === 'assistant'" class="bubble assistant">
                    <div class="role-label">🤖 AI助手</div>
                    <div class="markdown-content" v-html="renderMarkdown(msg.content)"></div>
                </div>
                
                <div v-if="msg.role === 'image'" class="bubble image">
                    <div class="role-label center">📸 拍摄图片</div>
                    <img :src="`${BASE_URL}/file/photos/${msg.content}`" class="chat-img">
                </div>
                
                <div v-if="msg.role === 'system'" class="bubble system">
                    {{ msg.content }}
                </div>

                <div v-if="msg.role === 'error'" class="bubble error">
                    {{ msg.content }}
                </div>
            </div>
        </div>

        <div class="chat-input-area">
            <input type="text" v-model="inputText" @keypress.enter="sendAiMessage" placeholder="输入问题...">
            <button class="btn-send" @click="sendAiMessage">发送</button>
            <button class="btn-clear" @click="clearHistory">🗑️</button>
        </div>
    </div>
  </ModalDialog>
</template>

<style scoped>
.chat-layout {
    display: flex;
    flex-direction: column;
    height: 60vh;
}

.chat-history {
    flex: 1;
    background: rgba(0,0,0,0.2);
    border-radius: 8px;
    padding: 15px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.empty-state {
    text-align: center;
    color: #666;
    margin-top: 50px;
}

.message-row {
    display: flex;
    width: 100%;
}

.message-row.user { justify-content: flex-end; }
.message-row.assistant { justify-content: flex-start; }
.message-row.image { justify-content: center; }
.message-row.system, .message-row.error { justify-content: center; }

.bubble {
    padding: 10px 14px;
    border-radius: 12px;
    max-width: 85%;
    font-size: 13px;
    line-height: 1.4;
    position: relative;
}

.bubble.user { background: linear-gradient(135deg, #667eea, #764ba2); color: white; border-bottom-right-radius: 2px; }
.bubble.assistant { background: linear-gradient(135deg, #11998e, #38ef7d); color: white; border-bottom-left-radius: 2px; }

/* Markdown 内容样式 */
.markdown-content :deep(h1), .markdown-content :deep(h2), .markdown-content :deep(h3) {
    margin: 8px 0 4px 0;
    font-weight: bold;
}
.markdown-content :deep(h1) { font-size: 1.2em; }
.markdown-content :deep(h2) { font-size: 1.1em; }
.markdown-content :deep(h3) { font-size: 1.05em; }
.markdown-content :deep(p) { margin: 4px 0; }
.markdown-content :deep(ul), .markdown-content :deep(ol) { margin: 4px 0; padding-left: 20px; }
.markdown-content :deep(li) { margin: 2px 0; }
.markdown-content :deep(code) { background: rgba(0,0,0,0.3); padding: 1px 4px; border-radius: 3px; font-family: monospace; }
.markdown-content :deep(pre) { background: rgba(0,0,0,0.3); padding: 8px; border-radius: 6px; overflow-x: auto; margin: 6px 0; }
.markdown-content :deep(blockquote) { border-left: 3px solid rgba(255,255,255,0.5); padding-left: 10px; margin: 6px 0; opacity: 0.9; }
.markdown-content :deep(strong) { font-weight: bold; }
.markdown-content :deep(em) { font-style: italic; }
.bubble.image { background: none; padding: 0; }
.bubble.system { background: rgba(255,255,255,0.1); color: #aaa; padding: 6px 12px; font-size: 12px; border-radius: 20px; }
.bubble.error { background: rgba(244, 67, 54, 0.2); color: #ff6b6b; border: 1px solid #ff6b6b; }

.role-label {
    font-size: 10px;
    opacity: 0.7;
    margin-bottom: 4px;
}
.role-label.center { text-align: center; }

.chat-img {
    max-width: 200px;
    border-radius: 8px;
    border: 2px solid rgba(255,255,255,0.1);
}

.chat-input-area {
    display: flex;
    gap: 10px;
    margin-top: 15px;
}

input {
    flex: 1;
    padding: 10px;
    border-radius: 20px;
    border: 1px solid rgba(255,255,255,0.1);
    background: rgba(0,0,0,0.3);
    color: white;
    outline: none;
}

input:focus { border-color: var(--theme-primary); }

.btn-send {
    padding: 0 20px;
    border-radius: 20px;
    background: var(--theme-primary);
    border: none;
    font-weight: bold;
    cursor: pointer;
}

.btn-clear {
    width: 40px;
    border-radius: 50%;
    background: #333;
    border: none;
    cursor: pointer;
    font-size: 16px;
}
</style>
