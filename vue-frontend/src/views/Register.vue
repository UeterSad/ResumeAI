<template>
  <div class="auth-page">
    <!-- 注册卡片 -->
    <div class="auth-card">
      <!-- Logo区域 -->
      <div class="auth-header">
        <div class="logo-wrapper">
          <div class="welcome-logo-balls register-logo-once">
            <span class="welcome-ball welcome-ball-left"></span>
            <span class="welcome-ball welcome-ball-right"></span>
          </div>
        </div>
        <h1 class="auth-title">加入简优</h1>
        <p class="auth-subtitle">创建账户，开启智能求职之旅</p>
      </div>

      <!-- 表单区域 -->
      <div class="auth-form">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input 
            type="text" 
              v-model="registerForm.username" 
            class="auth-input" 
            placeholder="请输入用户名（2-15个字符）"
          />
        </div>

        <div class="form-group">
          <label class="form-label">邮箱</label>
          <input 
            type="email" 
              v-model="registerForm.email" 
            class="auth-input" 
            placeholder="请输入邮箱地址"
          />
        </div>

        <div class="form-group">
          <label class="form-label">手机号</label>
          <input 
            type="tel" 
              v-model="registerForm.phone" 
            class="auth-input" 
            placeholder="请输入11位手机号"
          />
        </div>

        <div class="form-group">
          <label class="form-label">密码</label>
          <input 
            type="password" 
              v-model="registerForm.password" 
            class="auth-input" 
            placeholder="请输入密码（6-20个字符）"
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

        <button class="auth-btn" @click="submitRegisterForm()">立即注册</button>
      </div>

      <!-- 底部链接 -->
      <div class="auth-footer">
        <span class="footer-text">已有账号？</span>
        <a class="footer-link" @click="goLogin()">返回登录</a>
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

const captchaInput = ref('')
const captchaCode = ref('')
const captchaDictionary = ref('1234567890abcdefjhijklinopqrsduvwxyz')

onMounted(() => {
  captchaCode.value = ''
  generateCaptchaCode(captchaDictionary.value, 4)
})

const getRandomIntInclusive = (min, max) => {
  max = max + 1
  return Math.floor(Math.random() * (max - min) + min)
}

const generateCaptchaCode = (sourceChars, codeLength) => {
  for (let index = 0; index < codeLength; index++) {
    captchaCode.value += sourceChars[getRandomIntInclusive(0, sourceChars.length)]
  }
}

const refreshCode = () => {
  captchaCode.value = ''
  generateCaptchaCode(captchaDictionary.value, 4)
}

const registerForm = ref({
  username: '',
  email: '',
  password: '',
  phone: ''
})

const submitRegisterForm = () => {
  if (!registerForm.value.username || registerForm.value.username.length < 2 || registerForm.value.username.length > 15) {
    ElMessage({ type: 'error', message: '用户名长度需在2-15个字符之间' })
    return
  }

  const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!registerForm.value.email || !emailReg.test(registerForm.value.email)) {
    ElMessage({ type: 'error', message: '请输入正确的邮箱地址' })
    return
  }

  if (!registerForm.value.phone || registerForm.value.phone.length !== 11) {
    ElMessage({ type: 'error', message: '请输入正确的11位手机号' })
    return
  }

  if (!registerForm.value.password || registerForm.value.password.length < 6 || registerForm.value.password.length > 20) {
    ElMessage({ type: 'error', message: '密码长度需在6-20个字符之间' })
    return
  }

  if (!captchaInput.value) {
    ElMessage({ type: 'error', message: '验证码不能为空！' })
    return
  }
  if (captchaInput.value !== captchaCode.value) {
    ElMessage({ type: 'error', message: '验证码错误' })
    refreshCode()
    return
  }
  
  const requestData = {
    username: registerForm.value.username,
    email: registerForm.value.email,
    password: registerForm.value.password,
    phone: registerForm.value.phone
  }
  axios.post('user/add', requestData)
      .then((response) => {
        if (response.data === "新增用户成功！") {
          ElMessage({ type: 'success', message: '注册成功' })
          setTimeout(() => {
            router.push('/login')
          }, 1000)
        } else {
          ElMessage({ type: 'error', message: '注册失败' })
        }
      })
      .catch((error) => {
        ElMessage({ type: 'error', message: '请求出错：' + error })
      })
}

const goLogin = () => {
  router.push('/login')
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
  content: 'job?Get&Trust U';
  position: absolute;
  right: clamp(12px, 6vw, 74px);
  top: 16%;
  font-size: clamp(20px, 3.4vw, 38px);
  font-family: 'Comic Sans MS', 'KaiTi', 'STKaiti', cursive;
  color: rgba(24, 94, 132, 0.48);
  letter-spacing: 1px;
  transform: rotate(7deg);
  pointer-events: none;
}

/* 卡片 */
.auth-card {
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.92);
  position: relative;
  z-index: 1;
  border-radius: 16px;
  padding: 36px 32px;
  box-shadow: 0 10px 30px rgba(36, 108, 143, 0.16);
  backdrop-filter: blur(2px);
}

/* 头部区域 */
.auth-header {
  text-align: center;
  margin-bottom: 28px;
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

.register-logo-once .welcome-ball-left {
  animation: register-rub-once-left 0.85s ease-out 1;
}

.register-logo-once .welcome-ball-right {
  animation: register-rub-once-right 0.85s ease-out 1;
}

@keyframes register-rub-once-left {
  0% {
    transform: translateY(-4px);
  }
  45% {
    transform: translateY(3px);
  }
  100% {
    transform: translateY(0);
  }
}

@keyframes register-rub-once-right {
  0% {
    transform: translateY(4px);
  }
  45% {
    transform: translateY(-3px);
  }
  100% {
    transform: translateY(0);
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
  gap: 18px;
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
  height: 46px;
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
  height: 46px;
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

/* 注册按钮 */
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
    padding: 28px 20px;
  }

  .auth-page::after {
    top: auto;
    bottom: 8%;
    font-size: 24px;
  }
  
  .auth-title {
    font-size: 22px;
  }
}
</style>
