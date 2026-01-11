import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import './assets/main.css'

// 创建Vue应用
const app = createApp(App)

// 使用Pinia
app.use(createPinia())

// 挂载
app.mount('#app')
