<template>
  <div class="register-container">
    <div class="register-header">
      <h1>邮箱注册</h1>
      <p>创建您的IM通信账户</p>
    </div>

    <div v-if="message.show" :class="['message', message.type]">
      {{ message.text }}
    </div>

    <form id="registerForm" @submit.prevent="handleRegister">
      <div class="form-group">
        <label for="email">邮箱地址</label>
        <input 
          type="email" 
          id="email" 
          name="email" 
          v-model="form.email"
          @blur="validateEmailField"
          required 
          placeholder="请输入您的邮箱地址"
        >
      </div>

      <div class="form-group">
        <label for="nickname">昵称</label>
        <input 
          type="text" 
          id="nickname" 
          name="nickname" 
          v-model="form.nickname"
          @blur="validateNicknameField"
          required 
          placeholder="请输入昵称"
        >
      </div>

      <div class="form-group">
        <label for="password">密码</label>
        <input 
          type="password" 
          id="password" 
          name="password" 
          v-model="form.password"
          @input="validatePassword"
          required 
          placeholder="请输入密码（至少6位，包含字母和数字）"
        >
      </div>

      <div class="form-group">
        <label for="confirmPassword">确认密码</label>
        <input 
          type="password" 
          id="confirmPassword" 
          name="confirmPassword" 
          v-model="form.confirmPassword"
          @input="validatePasswordConfirm"
          required 
          placeholder="请再次输入密码"
        >
      </div>

      <div class="form-group">
        <label for="verificationCode">验证码</label>
        <div class="verification-group">
          <input 
            type="text" 
            id="verificationCode" 
            name="verificationCode" 
            v-model="form.verificationCode"
            @input="validateVerificationCodeField"
            required 
            placeholder="请输入验证码"
          >
          <button 
            type="button" 
            id="sendCodeBtn" 
            class="send-code-btn"
            :disabled="isSendingCode || countdown > 0"
            @click="sendVerificationCode"
          >
            <span v-if="isSendingCode" class="loading"></span>
            {{ getCodeButtonText() }}
          </button>
        </div>
      </div>

      <button 
        type="submit" 
        id="registerBtn" 
        class="register-btn"
        :disabled="isLoading"
      >
        <span v-if="isLoading" class="loading"></span>
        {{ isLoading ? '注册中...' : '注册账户' }}
      </button>
    </form>

    <div class="login-link">
      已有账户？<a href="/login">立即登录</a>
    </div>

    <!-- 登录重定向提示 -->
    <div v-if="loginRedirect.show" id="loginRedirect" class="login-redirect">
      <p style="margin-bottom: 10px;">该邮箱已被注册，您可以：</p>
      <button @click="redirectToLogin" class="redirect-btn primary">直接登录</button>
      <button @click="redirectToForgotPassword" class="redirect-btn secondary">找回密码</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

// 基础URL
const baseUrl = window.location.origin

// 路由实例
const router = useRouter()

// 响应式数据
const isLoading = ref(false)
const isSendingCode = ref(false)
const countdown = ref(0)
let countdownTimer: number | null = null

// 注册表单
const form = reactive({
  email: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  verificationCode: ''
})

// 消息提示
const message = reactive({
  show: false,
  text: '',
  type: 'info' as 'success' | 'error' | 'info'
})

// 登录重定向提示
const loginRedirect = reactive({
  show: false
})

// 显示消息
const showMessage = (text: string, type: 'success' | 'error' | 'info' = 'info') => {
  message.text = text
  message.type = type
  message.show = true
  
  if (type === 'success') {
    setTimeout(() => {
      message.show = false
    }, 5000)
  }
}

// 隐藏消息功能已集成到showMessage中的自动隐藏逻辑

// 显示登录重定向提示
const showLoginRedirect = () => {
  loginRedirect.show = true
  
  // 10秒后自动隐藏
  setTimeout(() => {
    loginRedirect.show = false
  }, 10000)
}

// 隐藏登录重定向提示
const hideLoginRedirect = () => {
  loginRedirect.show = false
}

// 重定向到登录页
const redirectToLogin = () => {
  router.push('/login')
}

// 重定向到找回密码页
const redirectToForgotPassword = () => {
  // 跳转到登录页面，用户可以在那里使用重置密码功能
  router.push('/login')
}

// 验证邮箱格式
const validateEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

// 验证邮箱字段
const validateEmailField = async () => {
  const email = form.email.trim()
  const isValid = validateEmail(email)
  
  if (!isValid) {
    showFieldStatus('email', false, '请输入有效的邮箱地址')
    return false
  }
  
  // 检查邮箱是否已存在
  try {
    const response = await fetch(`${baseUrl}/api/auth/check-email?email=${encodeURIComponent(email)}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    const result = await response.json()
    
    if (result.code === 200) {
      if (result.data && result.data.exists) {
        showFieldStatus('email', false, '该邮箱已被注册，请使用其他邮箱或直接登录')
        showLoginRedirect()
        return false
      } else {
        showFieldStatus('email', true, '邮箱可用')
        hideLoginRedirect()
        return true
      }
    } else {
      // 如果检查接口不存在或出错，仍然显示格式正确
      showFieldStatus('email', true, '邮箱格式正确')
      return true
    }
  } catch (error) {
    // 网络错误时不影响用户体验，仍然显示格式正确
    console.warn('邮箱存在性检查失败:', error)
    showFieldStatus('email', true, '邮箱格式正确')
    return true
  }
}

// 验证昵称字段
const validateNicknameField = () => {
  const nickname = form.nickname.trim()
  const isValid = nickname.length > 0 && nickname.length <= 20
  
  showFieldStatus('nickname', isValid, isValid ? '昵称格式正确' : '昵称不能为空且不超过20个字符')
  return isValid
}

// 验证验证码字段
const validateVerificationCodeField = () => {
  const code = form.verificationCode.trim()
  const isValid = /^\d{6}$/.test(code)
  
  showFieldStatus('verificationCode', isValid, isValid ? '验证码格式正确' : '验证码应为6位数字')
  return isValid
}

// 显示字段状态
const showFieldStatus = (fieldId: string, isValid: boolean, message: string) => {
  let statusEl = document.getElementById(`${fieldId}Status`)
  
  if (!statusEl) {
    statusEl = document.createElement('div')
    statusEl.id = `${fieldId}Status`
    statusEl.style.fontSize = '12px'
    statusEl.style.marginTop = '5px'
    statusEl.style.padding = '5px 8px'
    statusEl.style.borderRadius = '4px'
    
    const inputEl = document.getElementById(fieldId)
    if (inputEl && inputEl.parentNode) {
      inputEl.parentNode.appendChild(statusEl)
    }
  }
  
  statusEl.textContent = message
  
  if (isValid) {
    statusEl.style.color = '#155724'
    statusEl.style.backgroundColor = '#d4edda'
    statusEl.style.border = '1px solid #c3e6cb'
  } else {
    statusEl.style.color = '#721c24'
    statusEl.style.backgroundColor = '#f8d7da'
    statusEl.style.border = '1px solid #f5c6cb'
  }
  
  statusEl.style.display = 'block'
}

// 验证密码
const validatePassword = () => {
  const password = form.password
  const hasLetter = /[a-zA-Z]/.test(password)
  const hasNumber = /\d/.test(password)
  const isLongEnough = password.length >= 6
  
  // 实时显示密码强度提示
  showPasswordStrength(password, hasLetter, hasNumber, isLongEnough)
  
  return hasLetter && hasNumber && isLongEnough
}

// 显示密码强度
const showPasswordStrength = (password: string, hasLetter: boolean, hasNumber: boolean, isLongEnough: boolean) => {
  let strengthEl = document.getElementById('passwordStrength')
  
  if (!strengthEl) {
    strengthEl = document.createElement('div')
    strengthEl.id = 'passwordStrength'
    strengthEl.style.fontSize = '12px'
    strengthEl.style.marginTop = '5px'
    strengthEl.style.padding = '5px 8px'
    strengthEl.style.borderRadius = '4px'
    
    const passwordEl = document.getElementById('password')
    if (passwordEl && passwordEl.parentNode) {
      passwordEl.parentNode.appendChild(strengthEl)
    }
  }
  
  if (password.length === 0) {
    strengthEl.style.display = 'none'
    return
  }
  
  const requirements = [
    { met: isLongEnough, text: '至少6位字符' },
    { met: hasLetter, text: '包含字母' },
    { met: hasNumber, text: '包含数字' }
  ]
  
  const allMet = requirements.every(req => req.met)
  
  strengthEl.innerHTML = requirements.map(req => 
    `<span style="color: ${req.met ? '#155724' : '#721c24'}">${req.met ? '✓' : '✗'} ${req.text}</span>`
  ).join(' | ')
  
  if (allMet) {
    strengthEl.style.color = '#155724'
    strengthEl.style.backgroundColor = '#d4edda'
    strengthEl.style.border = '1px solid #c3e6cb'
  } else {
    strengthEl.style.color = '#721c24'
    strengthEl.style.backgroundColor = '#f8d7da'
    strengthEl.style.border = '1px solid #f5c6cb'
  }
  
  strengthEl.style.display = 'block'
}

// 验证密码确认
const validatePasswordConfirm = () => {
  const password = form.password
  const confirmPassword = form.confirmPassword
  const isMatch = password === confirmPassword
  
  // 实时提示密码确认状态
  if (confirmPassword.length > 0) {
    if (isMatch) {
      showPasswordConfirmStatus('密码确认一致', 'success')
    } else {
      showPasswordConfirmStatus('密码确认不一致', 'error')
    }
  } else {
    hidePasswordConfirmStatus()
  }
  
  return isMatch
}

// 显示密码确认状态
const showPasswordConfirmStatus = (message: string, type: 'success' | 'error') => {
  let statusEl = document.getElementById('passwordConfirmStatus')
  
  if (!statusEl) {
    statusEl = document.createElement('div')
    statusEl.id = 'passwordConfirmStatus'
    statusEl.style.fontSize = '12px'
    statusEl.style.marginTop = '5px'
    statusEl.style.padding = '5px 8px'
    statusEl.style.borderRadius = '4px'
    
    const confirmPasswordEl = document.getElementById('confirmPassword')
    if (confirmPasswordEl && confirmPasswordEl.parentNode) {
      confirmPasswordEl.parentNode.appendChild(statusEl)
    }
  }
  
  statusEl.textContent = message
  
  if (type === 'success') {
    statusEl.style.color = '#155724'
    statusEl.style.backgroundColor = '#d4edda'
    statusEl.style.border = '1px solid #c3e6cb'
  } else if (type === 'error') {
    statusEl.style.color = '#721c24'
    statusEl.style.backgroundColor = '#f8d7da'
    statusEl.style.border = '1px solid #f5c6cb'
  }
  
  statusEl.style.display = 'block'
}

// 隐藏密码确认状态
const hidePasswordConfirmStatus = () => {
  const statusEl = document.getElementById('passwordConfirmStatus')
  if (statusEl) {
    statusEl.style.display = 'none'
  }
}

// 验证表单
const validateForm = (email: string, nickname: string, password: string, confirmPassword: string, verificationCode: string) => {
  const errors: string[] = []
  
  // 邮箱验证
  if (!email) {
    errors.push('请输入邮箱地址')
  } else if (!validateEmail(email)) {
    errors.push('请输入有效的邮箱地址')
  }
  
  // 昵称验证
  if (!nickname) {
    errors.push('请输入昵称')
  } else if (nickname.length > 20) {
    errors.push('昵称不能超过20个字符')
  }
  
  // 密码验证
  if (!password) {
    errors.push('请输入密码')
  } else if (!validatePassword()) {
    errors.push('密码至少6位，且必须包含字母和数字')
  }
  
  // 确认密码验证
  if (!confirmPassword) {
    errors.push('请确认密码')
  } else if (password !== confirmPassword) {
    errors.push('两次输入的密码不一致')
  }
  
  // 验证码验证
  if (!verificationCode) {
    errors.push('请输入验证码')
  } else if (!/^\d{6}$/.test(verificationCode)) {
    errors.push('验证码应为6位数字')
  }
  
  return errors
}

// 获取验证码按钮文本
const getCodeButtonText = () => {
  if (isSendingCode.value) {
    return '发送中...'
  } else if (countdown.value > 0) {
    return `${countdown.value}秒后重发`
  } else {
    return '发送验证码'
  }
}

// 发送验证码
const sendVerificationCode = async () => {
  const email = form.email.trim()
  
  if (!email) {
    showMessage('请先输入邮箱地址', 'error')
    return
  }
  
  if (!validateEmail(email)) {
    showMessage('请输入有效的邮箱地址', 'error')
    return
  }
  
  try {
    isSendingCode.value = true
    
    const response = await fetch(`${baseUrl}/api/auth/verification/send/register?email=${encodeURIComponent(email)}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    const result = await response.json()
    
    if (result.code === 200) {
      showMessage('验证码已发送到您的邮箱，请查收', 'success')
      startCountdown()
    } else {
      // 根据用例表格处理特定错误
      let errorMessage = result.message || '发送验证码失败'
      if (errorMessage.includes('已被注册') || errorMessage.includes('已注册')) {
        errorMessage = '该邮箱已被注册'
        showMessage(errorMessage, 'error')
        showLoginRedirect()
      } else {
        showMessage(errorMessage, 'error')
      }
    }
  } catch (error) {
    console.error('发送验证码错误:', error)
    showMessage('网络错误，请稍后重试', 'error')
  } finally {
    isSendingCode.value = false
  }
}

// 开始倒计时
const startCountdown = () => {
  countdown.value = 60
  
  countdownTimer = window.setInterval(() => {
    countdown.value--
    
    if (countdown.value < 0) {
      clearInterval(countdownTimer as number)
      countdown.value = 0
    }
  }, 1000)
}

// 处理注册
const handleRegister = async () => {
  const email = form.email.trim()
  const nickname = form.nickname.trim()
  const password = form.password
  const confirmPassword = form.confirmPassword
  const verificationCode = form.verificationCode.trim()
  
  // 综合表单验证
  const validationErrors = validateForm(email, nickname, password, confirmPassword, verificationCode)
  
  if (validationErrors.length > 0) {
    showMessage(validationErrors[0]!, 'error')
    return
  }
  
  try {
    isLoading.value = true
    
    const requestData = {
      email: email,
      password: password,
      verificationCode: verificationCode,
      nickname: nickname || null,
      deviceType: 'Web',
      deviceInfo: navigator.userAgent
    }
    
    const response = await fetch(`${baseUrl}/api/auth/register/email`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(requestData)
    })
    
    const result = await response.json()
    
    if (result.code === 200 && result.data) {
      showMessage('注册成功！正在跳转...', 'success')
      
      // 保存用户信息到localStorage
      if (result.data.accessToken) {
        localStorage.setItem('accessToken', result.data.accessToken)
      }
      if (result.data.userInfo) {
        localStorage.setItem('userInfo', JSON.stringify(result.data.userInfo))
      }
      
      // 3秒后跳转到主页
      setTimeout(() => {
        router.push('/')
      }, 3000)
    } else {
      // 根据用例表格处理特定错误情况
      let errorMessage = result.message || '注册失败'
      
      if (errorMessage.includes('验证码') && (errorMessage.includes('错误') || errorMessage.includes('失效') || errorMessage.includes('过期'))) {
        errorMessage = '验证码错误或已失效，请重新获取验证码'
      } else if (errorMessage.includes('已被注册') || errorMessage.includes('已注册')) {
        errorMessage = '该邮箱已被注册'
        showLoginRedirect()
      } else if (errorMessage.includes('密码')) {
        errorMessage = '密码不符合要求，请确保至少6位且包含字母和数字'
      } else if (errorMessage.includes('昵称')) {
        errorMessage = '昵称格式不正确，请重新输入'
      }
      
      showMessage(errorMessage, 'error')
    }
  } catch (error) {
    console.error('注册错误:', error)
    showMessage('网络错误，请稍后重试', 'error')
  } finally {
    isLoading.value = false
  }
}

// 初始化
onMounted(() => {
  // 绑定事件监听器已通过模板中的指令完成
})

// 清理定时器
onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

.register-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  position: relative;
}

.register-header {
  background: white;
  color: #333;
  padding: 40px 30px;
  text-align: center;
  border-radius: 20px 20px 0 0;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 450px;
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.register-header h1 {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #667eea;
}

.register-header p {
  opacity: 0.8;
  font-size: 16px;
  color: #666;
}

.message {
  width: 100%;
  max-width: 450px;
  padding: 12px 20px;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 20px;
  text-align: center;
}

.message.success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.message.error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.message.info {
  background: #d1ecf1;
  color: #0c5460;
  border: 1px solid #bee5eb;
}

form {
  background: white;
  padding: 40px 30px;
  border-radius: 0 0 20px 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 450px;
  margin-top: -1px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
  font-size: 14px;
}

.form-group input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e1e5e9;
  border-radius: 10px;
  font-size: 16px;
  transition: all 0.3s ease;
  background-color: #f8f9fa;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
  background-color: white;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.verification-group {
  display: flex;
  gap: 10px;
}

.verification-group input {
  flex: 1;
}

.send-code-btn {
  background: #667eea;
  color: white;
  border: none;
  padding: 12px 20px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 5px;
}

.send-code-btn:hover:not(:disabled) {
  background: #5a6fd8;
  transform: translateY(-1px);
}

.send-code-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.register-btn {
  width: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 14px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.register-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3);
}

.register-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.loading {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.login-link {
  text-align: center;
  margin-top: 20px;
  color: white;
  font-size: 14px;
}

.login-link a {
  color: white;
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
  text-decoration: underline;
}

.login-redirect {
  margin-top: 15px;
  padding: 12px;
  background-color: #d1ecf1;
  color: #0c5460;
  border: 1px solid #bee5eb;
  border-radius: 8px;
  font-size: 14px;
  text-align: center;
  z-index: 1000;
  width: 100%;
  max-width: 450px;
}

.redirect-btn {
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  margin: 0 5px;
  font-size: 13px;
  transition: all 0.3s ease;
}

.redirect-btn.primary {
  background: #667eea;
  color: white;
}

.redirect-btn.primary:hover {
  background: #5a6fd8;
}

.redirect-btn.secondary {
  background: #6c757d;
  color: white;
}

.redirect-btn.secondary:hover {
  background: #5a6268;
}
</style>