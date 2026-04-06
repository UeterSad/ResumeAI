<template>
  <div class="auth-page">
    <!-- 登录卡片 -->
    <div class="auth-card">
      <!-- Logo区域 -->
      <div class="auth-header">
        <div class="logo-wrapper">
          <div class="welcome-logo-balls login-logo-once">
            <span class="welcome-ball welcome-ball-left"></span>
            <span class="welcome-ball welcome-ball-right"></span>
          </div>
        </div>
        <h1 class="auth-title">简优助手</h1>
        <p class="auth-subtitle">AI驱动的智能求职平台</p>
      </div>

      <!-- 表单区域 -->
      <div class="auth-form">
        <div class="form-group">
          <label class="form-label">账号</label>
          <input 
            type="text" 
              v-model="loginForm.email" 
            class="auth-input" 
            placeholder="请输入用户名或邮箱"
          />
        </div>

        <div class="form-group">
          <label class="form-label">密码</label>
          <input 
            type="password" 
              v-model="loginForm.password" 
            class="auth-input" 
            placeholder="请输入密码"
          />
        </div>

        <div class="form-group">
          <label class="form-label">验证码</label>
          <div class="captcha-wrapper">
            <input 
              type="text" 
              v-model="captchaInput" 
              class="auth-input captcha-input" 
              placeholder="请输入验证码"
            />
            <div class="captcha-display" @click="refreshCode">
              <SIdentify :identifyCode="captchaCode"></SIdentify>
            </div>
          </div>
        </div>

        <button class="auth-btn" @click="submitLoginForm()">登 录</button>
      </div>

      <!-- 底部链接 -->
      <div class="auth-footer">
        <span class="footer-text">还没有账号？</span>
        <a class="footer-link" @click="goRegister()">立即注册</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import SIdentify from '@/utils/SidentifyView.vue'
import { ElMessage } from 'element-plus'
import { ref } from 'vue'
import axios from '@/utils/axios-config.js'
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 验证码输入值
const captchaInput = ref('')
// 当前验证码
const captchaCode = ref('')
// 验证码字符集合（仅数字，便于辨认）
const captchaDictionary = ref('0123456789')

//组件挂载
onMounted(() => {
  captchaCode.value = ''
  generateCaptchaCode(captchaDictionary.value, 4)
})

const getRandomIntInclusive = (min, max) => {
  max = max + 1
  return Math.floor(Math.random() * (max - min) + min)
}

// 生成验证码字符串。
const generateCaptchaCode = (sourceChars, codeLength) => {
  for (let index = 0; index < codeLength; index++) {
    captchaCode.value += sourceChars[getRandomIntInclusive(0, sourceChars.length - 1)]
  }
}

// 更新验证码
const refreshCode = () => {
  captchaCode.value = ''
  generateCaptchaCode(captchaDictionary.value, 4)
}

const loginForm = ref({
  email: '',
  password: ''
})

const submitLoginForm = () => {
  if (!captchaInput.value) {
    ElMessage({ type: 'error', message: '验证码不能为空！' })
    return
  }

  if (captchaInput.value.toLowerCase() !== captchaCode.value.toLowerCase()) {
    ElMessage({ type: 'error', message: '验证码错误' })
    refreshCode()
    return
  } else {
    const requestData = {
      email: loginForm.value.email,
      password: loginForm.value.password
    }

    console.log('请求数据' + requestData.email + ' ' + requestData.password)

    axios
        .post('user/login', requestData)
        .then(function (response) {
          console.log('响应数据：', response.data)
          if (response.data !== null ) {
            ElMessage({ type: 'success', message: '登录成功' })
            localStorage.setItem('username', response.data.username)
            localStorage.setItem('userId', response.data.id)
            localStorage.setItem('email', response.data.email)
            localStorage.setItem('userType', response.data.userType)
            console.log('localStorage.getItem(username):' + localStorage.getItem('username'))
            router.push('/chatView')
          } else {
            ElMessage({ type: 'error', message: response.data.msg })
          }
        })
        .catch(function (error) {
          console.error('请求出错：', error)
        })
  }
}

const goRegister = () => {
  router.push('/register')
}
</script>

<style scoped>
/* 页面容器 */
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: #eef8ff;
  font-family: 'PingFang SC', 'Noto Sans SC', sans-serif;
  padding: 20px;
}

.auth-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
      linear-gradient(rgba(42, 152, 194, 0.14) 1px, transparent 1px),
      linear-gradient(90deg, rgba(42, 152, 194, 0.14) 1px, transparent 1px);
  background-size: 34px 34px;
}

.auth-page::after {
  content: 'ResumeAI';
  position: absolute;
  left: clamp(12px, 6vw, 74px);
  top: 14%;
  font-size: clamp(26px, 4vw, 44px);
  font-family: 'Comic Sans MS', 'KaiTi', 'STKaiti', cursive;
  color: rgba(24, 94, 132, 0.48);
  letter-spacing: 1px;
  transform: rotate(-8deg);
  pointer-events: none;
}

/* 卡片 */
.auth-card {
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.92);
  position: relative;
  z-index: 1;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 10px 30px rgba(36, 108, 143, 0.16);
  backdrop-filter: blur(2px);
}

/* 头部区域 */
.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-wrapper {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.welcome-logo-balls {
  position: relative;
  width: 44px;
  height: 28px;
}

.welcome-ball {
  position: absolute;
  width: 28px;
  height: 28px;
  border-radius: 50%;
}

.welcome-ball-left {
  left: 0;
  background: #10b981;
}

.welcome-ball-right {
  right: 0;
  background: #3b82f6;
  opacity: 0.85;
}

.login-logo-once .welcome-ball-left {
  animation: login-collision-once-left 0.8s ease-out 1;
}

.login-logo-once .welcome-ball-right {
  animation: login-collision-once-right 0.8s ease-out 1;
}

@keyframes login-collision-once-left {
  0% {
    transform: translateX(-5px);
  }
  55% {
    transform: translateX(3px);
  }
  100% {
    transform: translateX(0);
  }
}

@keyframes login-collision-once-right {
  0% {
    transform: translateX(5px);
  }
  55% {
    transform: translateX(-3px);
  }
  100% {
    transform: translateX(0);
  }
}

.auth-title {
  font-size: 24px;
  font-weight: 600;
  color: #1e1b4b;
  margin: 0 0 6px 0;
}

.auth-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

/* 表单区域 */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  width: 100%;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #1e1b4b;
  margin-bottom: 8px;
}

.auth-input {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  font-size: 15px;
  color: #1e1b4b;
  background: #fafafa;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.auth-input::placeholder {
  color: #9ca3af;
}

.auth-input:focus {
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

/* 验证码区域 */
.captcha-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-display {
  height: 48px;
  min-width: 110px;
  background: #fafafa;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
}

/* 登录按钮 */
.auth-btn {
  width: 100%;
  height: 48px;
  background: #10b981;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  margin-top: 4px;
  transition: background 0.2s;
}

.auth-btn:hover {
  background: #059669;
}

/* 底部链接 */
.auth-footer {
  text-align: center;
  margin-top: 24px;
}

.footer-text {
  font-size: 14px;
  color: #64748b;
}

.footer-link {
  font-size: 14px;
  color: #6366f1;
  font-weight: 500;
  cursor: pointer;
  margin-left: 4px;
}

.footer-link:hover {
  text-decoration: underline;
}

/* 响应式 */
@media (max-width: 480px) {
  .auth-card {
    padding: 32px 24px;
  }

  .auth-page::after {
    top: auto;
    bottom: 8%;
    font-size: 26px;
  }
  
  .auth-title {
    font-size: 22px;
  }
}
</style>