import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import axios from 'axios'

// 全局 axios 默认后端地址（保留现有行为）。
axios.defaults.baseURL = 'http://localhost:9090'

const vueApp = createApp(App)
vueApp.use(router)
vueApp.use(ElementPlus)
vueApp.mount('#app')
