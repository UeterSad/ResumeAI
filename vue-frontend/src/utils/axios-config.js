import axios from 'axios'

// 创建统一请求实例。
const httpClient = axios.create({
  baseURL: 'http://localhost:9090/',
  timeout: 50000,
  headers: {
    // 统一 JSON 请求头。
    'Content-Type': 'application/json'
  }
})

export default httpClient
