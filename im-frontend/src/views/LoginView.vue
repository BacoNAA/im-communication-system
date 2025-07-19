<template>
  <div class="login-container">
    <div class="login-header">
      <h1>欢迎回来</h1>
      <p>登录您的账户继续使用</p>
    </div>
    
    <div class="login-form">
      <!-- 全局提示 -->
      <div v-if="alertMessage.show" :class="['alert', alertMessage.type]">
        {{ alertMessage.text }}
      </div>
      
      <!-- 登录方式切换 -->
      <div class="login-tabs">
        <button 
          type="button"
          :class="['tab-btn', { active: currentLoginMode === 'password' }]"
          @click="switchLoginMode('password')"
        >
          密码登录
        </button>
        <button 
          type="button"
          :class="['tab-btn', { active: currentLoginMode === 'verification' }]"
          @click="switchLoginMode('verification')"
        >
          验证码登录
        </button>
      </div>

      <!-- 密码登录表单 -->
      <form 
        v-if="currentLoginMode === 'password'" 
        @submit.prevent="handlePasswordLogin" 
        class="login-form-content"
        style="display: block;"
      >
        <div class="form-group">
          <label for="email">邮箱地址</label>
          <input
            id="email"
            v-model="passwordForm.email"
            type="email"
            :class="{ error: passwordForm.emailError }"
            placeholder="请输入您的邮箱地址"
            @input="handleEmailInput"
            required
          />
          <div v-if="passwordForm.emailError" class="error-message">{{ passwordForm.emailError }}</div>
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="passwordForm.password"
            type="password"
            :class="{ error: passwordForm.passwordError }"
            placeholder="请输入您的密码"
            autocomplete="off"
            @input="handlePasswordInput"
            required
          />
          <div v-if="passwordForm.passwordError" class="error-message">{{ passwordForm.passwordError }}</div>
        </div>
        
        <div class="forgot-password-link" style="text-align: right; margin-bottom: 15px;">
          <a href="#" @click.prevent="showResetPassword" style="color: #667eea; text-decoration: none; font-size: 14px; font-weight: 500;">忘记密码？</a>
        </div>

        <div class="remember-me">
          <input
            id="rememberMe"
            v-model="passwordForm.rememberMe"
            type="checkbox"
          />
          <label for="rememberMe">记住登录状态</label>
        </div>

        <button 
          type="submit" 
          class="login-btn"
          :disabled="isLoading"
        >
          登录
        </button>
      </form>

      <!-- 验证码登录表单 -->
      <form 
        v-if="currentLoginMode === 'verification'" 
        @submit.prevent="handleVerificationLogin" 
        class="login-form-content"
        :style="{ display: currentLoginMode === 'verification' ? 'block' : 'none' }"
      >
        <div class="form-group">
          <label for="verificationEmail">邮箱地址</label>
          <input
            id="verificationEmail"
            v-model="verificationForm.email"
            type="email"
            :class="{ error: verificationForm.emailError }"
            placeholder="请输入您的邮箱地址"
            @input="handleVerificationEmailInput"
            required
          />
          <div v-if="verificationForm.emailError" class="error-message">{{ verificationForm.emailError }}</div>
        </div>

        <div class="form-group">
          <label for="verificationCode">验证码</label>
          <div class="verification-input-group">
            <input
              id="verificationCode"
              v-model="verificationForm.code"
              type="text"
              :class="{ error: verificationForm.codeError }"
              placeholder="请输入6位验证码"
              maxlength="6"
              @input="handleVerificationCodeInput"
              @keypress="handleVerificationCodeKeypress"
              required
            />
            <button
              type="button"
              :class="['send-code-btn', { countdown: countdown > 0 }]"
              :disabled="isSendingCode || countdown > 0"
              @click="sendVerificationCode"
            >
              {{ countdown > 0 ? `${countdown}秒后重发` : (isSendingCode ? '发送中...' : '获取验证码') }}
            </button>
          </div>
          <div v-if="verificationForm.codeError" class="error-message">{{ verificationForm.codeError }}</div>
        </div>

        <button 
          type="submit" 
          class="login-btn"
          :disabled="isLoading"
        >
          登录
        </button>
      </form>
      
      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading" style="display: block;">
        <div class="loading-spinner"></div>
        <p>正在登录...</p>
      </div>

      <div class="divider">
        <span>或</span>
      </div>

      <div class="register-link">
        还没有账户？<router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
  
  <!-- 重置密码模态框 -->
  <div v-if="resetPasswordModal.show" class="modal-overlay">
    <div class="modal-dialog">
      <div class="modal-header">
        <h3 class="modal-title">重置密码</h3>
        <button class="modal-close" @click="closeResetPassword">&times;</button>
      </div>
      <div class="modal-body">
        <div class="reset-step">
          <div class="form-group">
            <label for="resetEmail">邮箱地址</label>
            <input 
              id="resetEmail" 
              v-model="resetPasswordModal.email"
              type="email" 
              placeholder="请输入您的邮箱地址" 
              required
            />
          </div>
          <div class="form-group">
            <label for="resetCode">验证码</label>
            <div class="verification-input-group">
              <input 
                id="resetCode" 
                v-model="resetPasswordModal.code"
                type="text" 
                placeholder="请输入验证码" 
                maxlength="6" 
                required
              />
              <button 
                type="button" 
                class="send-code-btn" 
                :disabled="resetPasswordModal.isSendingCode || resetPasswordModal.countdown > 0"
                @click="sendResetCode"
              >
                {{ resetPasswordModal.countdown > 0 ? `${resetPasswordModal.countdown}秒后重发` : (resetPasswordModal.isSendingCode ? '发送中...' : '发送验证码') }}
              </button>
            </div>
          </div>
          <div class="form-group">
            <label for="newPassword">新密码</label>
            <input 
              id="newPassword" 
              v-model="resetPasswordModal.newPassword"
              type="password" 
              placeholder="请输入新密码" 
              required
            />
          </div>
          <div class="form-group">
            <label for="confirmPassword">确认密码</label>
            <input 
              id="confirmPassword" 
              v-model="resetPasswordModal.confirmPassword"
              type="password" 
              placeholder="请再次输入新密码" 
              required
            />
          </div>
          <div v-if="resetPasswordModal.errorMessage" class="error-message" style="display: block; color: #e74c3c;">
            {{ resetPasswordModal.errorMessage }}
          </div>
          <div v-if="resetPasswordModal.successMessage" class="error-message" style="display: block; color: #16a34a;">
            {{ resetPasswordModal.successMessage }}
          </div>
          <button 
            type="button" 
            class="login-btn" 
            :disabled="resetPasswordModal.isResetting"
            @click="resetPassword"
          >
            {{ resetPasswordModal.isResetting ? '重置中...' : '重置密码' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useSharedWebSocket } from '@/composables/useWebSocket'

// 路由实例
const router = useRouter()

// 获取设备信息
function getDeviceInfo() {
  const userAgent = navigator.userAgent
  let deviceType = 'Web'
  
  if (/Mobile|Android|iPhone|iPad/.test(userAgent)) {
    deviceType = 'Mobile'
  } else if (/Tablet|iPad/.test(userAgent)) {
    deviceType = 'Tablet'
  }
  
  return {
    deviceType: deviceType,
    deviceInfo: userAgent
  }
}

// 验证邮箱格式 - 严格按照原始逻辑
function validateEmail(email: string): boolean {
  // 更严格的邮箱格式验证
  const emailRegex = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/
  
  // 基本格式检查
  if (!emailRegex.test(email)) {
    return false
  }
  
  // 长度检查
  if (email.length > 254) {
    return false
  }
  
  // 本地部分长度检查（@符号前的部分）
  const localPart = email.split('@')[0]
  if (localPart && localPart.length > 64) {
    return false
  }
  
  // 域名部分检查
  const domain = email.split('@')[1]
  if (domain && domain.length > 253) {
    return false
  }
  
  // 检查是否以点开头或结尾
  if (email.startsWith('.') || email.endsWith('.')) {
    return false
  }
  
  // 检查连续的点
  if (email.includes('..')) {
    return false
  }
  
  return true
}

// 验证验证码格式
function validateVerificationCode(code: string) {
  // 检查是否为空
  if (!code || code.trim() === '') {
    return { valid: false, message: '请输入验证码' }
  }
  
  // 去除空格
  code = code.trim()
  
  // 检查长度
  if (code.length !== 6) {
    return { valid: false, message: '验证码必须是6位数字' }
  }
  
  // 检查是否全为数字
  if (!/^\d{6}$/.test(code)) {
    return { valid: false, message: '验证码只能包含数字' }
  }
  
  // 检查是否为连续数字或重复数字（增强安全性）
  const isSequential = /^(012345|123456|234567|345678|456789|567890|654321|543210|432109|321098|210987|109876)$/.test(code)
  const isRepeated = /^(\d)\1{5}$/.test(code)
  
  if (isSequential || isRepeated) {
    return { valid: false, message: '请输入有效的验证码' }
  }
  
  return { valid: true, message: '' }
}

// 当前登录模式
const currentLoginMode = ref<'password' | 'verification'>('password')
const isLoading = ref(false)
const isSendingCode = ref(false)
const countdown = ref(0)
let countdownTimer: number | null = null

// 密码登录表单
const passwordForm = reactive({
  email: '',
  password: '',
  rememberMe: false,
  emailError: '',
  passwordError: ''
})

// 验证码登录表单
const verificationForm = reactive({
  email: '',
  code: '',
  emailError: '',
  codeError: ''
})

// 全局提示消息
const alertMessage = reactive({
  show: false,
  text: '',
  type: 'error' as 'success' | 'error' | 'warning' | 'info'
})

// 重置密码模态框
const resetPasswordModal = reactive({
  show: false,
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
  errorMessage: '',
  successMessage: '',
  isSendingCode: false,
  isResetting: false,
  countdown: 0
})
let resetCountdownTimer: number | null = null

// 显示全局提示
function showAlert(message: string, type: 'success' | 'error' | 'warning' | 'info' = 'error') {
  alertMessage.text = message
  alertMessage.type = type
  alertMessage.show = true
}

// 清除全局提示
function clearAlert() {
  alertMessage.show = false
  alertMessage.text = ''
}

// 显示错误信息
function showError(fieldType: 'email' | 'password' | 'verificationEmail' | 'verificationCode', message: string) {
  if (fieldType === 'email') {
    passwordForm.emailError = message
  } else if (fieldType === 'password') {
    passwordForm.passwordError = message
  } else if (fieldType === 'verificationEmail') {
    verificationForm.emailError = message
  } else if (fieldType === 'verificationCode') {
    verificationForm.codeError = message
  }
}

// 清除错误信息
function clearError(fieldType: 'email' | 'password' | 'verificationEmail' | 'verificationCode') {
  if (fieldType === 'email') {
    passwordForm.emailError = ''
  } else if (fieldType === 'password') {
    passwordForm.passwordError = ''
  } else if (fieldType === 'verificationEmail') {
    verificationForm.emailError = ''
  } else if (fieldType === 'verificationCode') {
    verificationForm.codeError = ''
  }
}

// 清除所有错误提示
function clearAllErrors() {
  clearError('email')
  clearError('password')
  clearError('verificationEmail')
  clearError('verificationCode')
}

// 切换登录模式
function switchLoginMode(mode: 'password' | 'verification') {
  currentLoginMode.value = mode
  
  // 清除所有错误提示
  clearAllErrors()
  clearAlert()
}

// 验证密码登录表单
function validatePasswordForm(): boolean {
  let isValid = true
  
  // 清除之前的错误
  clearError('email')
  clearError('password')
  clearAlert()
  
  const email = passwordForm.email.trim()
  const password = passwordForm.password
  
  // 验证邮箱
  if (!email) {
    showError('email', '请输入邮箱地址')
    isValid = false
  } else if (!validateEmail(email)) {
    showError('email', '请输入有效的邮箱地址')
    isValid = false
  }
  
  // 验证密码
  if (!password) {
    showError('password', '请输入密码')
    isValid = false
  }
  
  return isValid
}

// 验证验证码登录表单
function validateVerificationForm(): boolean {
  let isValid = true
  
  // 清除之前的错误
  clearError('verificationEmail')
  clearError('verificationCode')
  clearAlert()
  
  const email = verificationForm.email.trim()
  const code = verificationForm.code.trim()
  
  // 验证邮箱
  if (!email) {
    showError('verificationEmail', '请输入邮箱地址')
    isValid = false
  } else if (!validateEmail(email)) {
    showError('verificationEmail', '请输入有效的邮箱地址')
    isValid = false
  }
  
  // 验证验证码
  const codeValidation = validateVerificationCode(code)
  if (!codeValidation.valid) {
    showError('verificationCode', codeValidation.message)
    isValid = false
  }
  
  return isValid
}

// 初始化共享WebSocket
const { connect: connectWebSocket } = useSharedWebSocket()

// 处理密码登录
async function handlePasswordLogin() {
  if (!validatePasswordForm()) {
    return
  }
  
  // 显示加载状态
  isLoading.value = true
  
  try {
    const deviceInfo = getDeviceInfo()
    const email = passwordForm.email.trim()
    const password = passwordForm.password

    if (!email || !password) {
      showAlert('邮箱和密码不能为空', 'error')
      isLoading.value = false
      return
    }

    const formData = {
      email: email,
      password: password,
      deviceType: deviceInfo.deviceType,
      deviceInfo: deviceInfo.deviceInfo,
      rememberMe: passwordForm.rememberMe
    }
    
    const response = await fetch('/api/auth/login/password', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(formData)
    })
    
    const result = await response.json()
    
    if (response.ok && result.success) {
      // 登录成功
      const authData = result.data
      
      // 存储认证信息
      if (formData.rememberMe) {
        // 记住登录状态：保存到localStorage（持久化）
        localStorage.setItem('accessToken', authData.accessToken)
        localStorage.setItem('refreshToken', authData.refreshToken)
        localStorage.setItem('userInfo', JSON.stringify(authData.userInfo))
        // 保存邮箱、密码和记住我状态，用于下次自动填充
        localStorage.setItem('savedEmail', formData.email)
        // 只有在后端返回密码时才保存（安全考虑）
        if (authData.userInfo && authData.userInfo.password) {
          localStorage.setItem('savedPassword', authData.userInfo.password)
        }
        localStorage.setItem('rememberMe', 'true')
        // 清除sessionStorage中可能存在的数据
        sessionStorage.removeItem('accessToken')
        sessionStorage.removeItem('refreshToken')
        sessionStorage.removeItem('userInfo')
      } else {
        // 不记住登录状态：保存到sessionStorage（会话级别）
        sessionStorage.setItem('accessToken', authData.accessToken)
        sessionStorage.setItem('refreshToken', authData.refreshToken)
        sessionStorage.setItem('userInfo', JSON.stringify(authData.userInfo))
        // 清除localStorage中的登录信息
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('userInfo')
        localStorage.removeItem('savedEmail')
        localStorage.removeItem('savedPassword')
        localStorage.removeItem('rememberMe')
        // 清空密码输入框，防止刷新页面后密码仍然存在
        passwordForm.password = ''
      }
      
      // 立即连接WebSocket
      connectWebSocket()
      console.log('登录成功，已激活WebSocket连接')
      
      showAlert('登录成功，正在跳转...', 'success')
      
      // 延迟跳转到仪表板
      setTimeout(() => {
        router.push('/dashboard')
      }, 1500)
      
    } else {
      // 登录失败
      const errorMessage = result.message || '登录失败，请重试'
      showAlert(errorMessage, 'error')
      
      // 恢复表单显示
      isLoading.value = false
      
      // 清空密码输入框
      passwordForm.password = ''
    }
    
  } catch (error) {
    console.error('登录请求失败:', error)
    showAlert('网络错误，请检查网络连接后重试', 'error')
    
    // 恢复表单显示
    isLoading.value = false
  }
}

// 检查并填充保存的登录信息
function loadSavedLoginInfo() {
  // 检查localStorage中是否有保存的邮箱（记住登录状态）
  const savedEmail = localStorage.getItem('savedEmail')
  const savedPassword = localStorage.getItem('savedPassword')
  const rememberMe = localStorage.getItem('rememberMe') === 'true'
  
  if (savedEmail && rememberMe) {
    // 填充邮箱
    passwordForm.email = savedEmail
    // 填充密码（如果有保存）
    if (savedPassword) {
      passwordForm.password = savedPassword
    }
    // 设置记住我选项
    passwordForm.rememberMe = true
    
    // 显示提示信息
    showAlert('已自动填充上次保存的登录信息', 'success')
    
    // 3秒后自动清除提示
    setTimeout(() => {
      clearAlert()
    }, 3000)
  }
}

// 处理验证码登录
async function handleVerificationLogin() {
  if (!validateVerificationForm()) {
    return
  }
  
  // 显示加载状态
  isLoading.value = true
  
  try {
    const deviceInfo = getDeviceInfo()
    const email = verificationForm.email.trim()
    const code = verificationForm.code.trim()
    
    if (!email || !code) {
      showAlert('邮箱和验证码不能为空', 'error')
      isLoading.value = false
      return
    }
    
    const formData = {
      email: email,
      verificationCode: code,
      deviceType: deviceInfo.deviceType,
      deviceInfo: deviceInfo.deviceInfo
    }
    
    const response = await fetch('/api/auth/login/verification', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(formData)
    })
    
    const result = await response.json()
    
    if (response.ok && result.success) {
      // 登录成功
      const authData = result.data
      
      // 存储认证信息到sessionStorage（验证码登录默认不记住）
      sessionStorage.setItem('accessToken', authData.accessToken)
      sessionStorage.setItem('refreshToken', authData.refreshToken)
      sessionStorage.setItem('userInfo', JSON.stringify(authData.userInfo))
      
      // 立即连接WebSocket
      connectWebSocket()
      console.log('登录成功，已激活WebSocket连接')
      
      showAlert('登录成功，正在跳转...', 'success')
      
      // 延迟跳转到仪表板
      setTimeout(() => {
        router.push('/dashboard')
      }, 1500)
      
    } else {
      // 登录失败
      const errorMessage = result.message || '登录失败，请重试'
      showAlert(errorMessage, 'error')
      
      // 恢复表单显示
      isLoading.value = false
      
      // 清空验证码输入框
      verificationForm.code = ''
    }
    
  } catch (error) {
    console.error('登录请求失败:', error)
    showAlert('网络错误，请检查网络连接后重试', 'error')
    
    // 恢复表单显示
    isLoading.value = false
  }
}

// 发送验证码
async function sendVerificationCode() {
  const email = verificationForm.email.trim()
  
  // 验证邮箱
  if (!email) {
    showError('verificationEmail', '请输入邮箱地址')
    return
  }
  
  if (!validateEmail(email)) {
    showError('verificationEmail', '请输入有效的邮箱地址')
    return
  }
  
  // 清除邮箱错误提示
  clearError('verificationEmail')
  
  try {
    // 禁用按钮
    isSendingCode.value = true
    
    const response = await fetch('/api/auth/verification/send/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: `email=${encodeURIComponent(email)}`
    })
    
    const result = await response.json()
    
    if (response.ok && result.success) {
      showAlert('验证码已发送到您的邮箱，请查收', 'success')
      
      // 开始倒计时
      startCountdown(60)
      
    } else {
      const errorMessage = result.message || '发送验证码失败，请重试'
      showAlert(errorMessage, 'error')
      
      // 恢复按钮状态
      isSendingCode.value = false
    }
    
  } catch (error) {
    console.error('发送验证码失败:', error)
    showAlert('网络错误，请检查网络连接后重试', 'error')
    
    // 恢复按钮状态
    isSendingCode.value = false
  }
}

// 倒计时功能
function startCountdown(seconds: number) {
  let remaining = seconds
  
  // 清除之前的计时器
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  
  countdown.value = remaining
  countdownTimer = setInterval(() => {
    remaining--
    countdown.value = remaining
    
    if (remaining < 0) {
      clearInterval(countdownTimer!)
      isSendingCode.value = false
      countdown.value = 0
      countdownTimer = null
    }
  }, 1000)
}

// 输入事件处理
function handleEmailInput() {
  clearError('email')
  const email = passwordForm.email.trim()
  if (email && !validateEmail(email)) {
    showError('email', '请输入有效的邮箱地址')
  }
}

function handlePasswordInput() {
  clearError('password')
  const password = passwordForm.password
  if (password && password.length < 6) {
    showError('password', '密码长度至少6位')
  }
}

function handleVerificationEmailInput() {
  clearError('verificationEmail')
  const email = verificationForm.email.trim()
  if (email && !validateEmail(email)) {
    showError('verificationEmail', '请输入有效的邮箱地址')
  }
}

function handleVerificationCodeInput() {
  clearError('verificationCode')
  const code = verificationForm.code.trim()
  if (code) {
    const validation = validateVerificationCode(code)
    if (!validation.valid) {
      showError('verificationCode', validation.message)
    }
  }
  
  // 限制验证码输入框最大长度
  if (verificationForm.code.length > 6) {
    verificationForm.code = verificationForm.code.slice(0, 6)
  }
}

function handleVerificationCodeKeypress(e: KeyboardEvent) {
  // 只允许数字输入
  if (!/\d/.test(e.key) && !['Backspace', 'Delete', 'Tab', 'Enter'].includes(e.key)) {
    e.preventDefault()
  }
}

// 重置密码相关功能
function showResetPassword() {
  resetPasswordModal.show = true
}

function closeResetPassword() {
  resetPasswordModal.show = false
  // 清空表单
  resetPasswordModal.email = ''
  resetPasswordModal.code = ''
  resetPasswordModal.newPassword = ''
  resetPasswordModal.confirmPassword = ''
  resetPasswordModal.errorMessage = ''
  resetPasswordModal.successMessage = ''
}

function showResetError(message: string, type: 'error' | 'success' = 'error') {
  if (type === 'success') {
    resetPasswordModal.successMessage = message
    resetPasswordModal.errorMessage = ''
  } else {
    resetPasswordModal.errorMessage = message
    resetPasswordModal.successMessage = ''
  }
}

// 发送重置密码验证码
async function sendResetCode() {
  const email = resetPasswordModal.email.trim()
  
  if (!email) {
    showResetError('请输入邮箱地址')
    return
  }
  
  if (!validateEmail(email)) {
    showResetError('请输入有效的邮箱地址')
    return
  }
  
  try {
    resetPasswordModal.isSendingCode = true
    
    const response = await fetch('/api/auth/verification/send/reset-password', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: `email=${encodeURIComponent(email)}`
    })
    
    const result = await response.json()
    
    if (response.ok && result.success) {
      showResetError('验证码已发送到您的邮箱', 'success')
      startResetCountdown(60)
    } else {
      showResetError(result.message || '发送失败，请重试')
      resetPasswordModal.isSendingCode = false
    }
  } catch (error) {
    showResetError('网络错误，请重试')
    resetPasswordModal.isSendingCode = false
  }
}

// 重置密码倒计时
function startResetCountdown(seconds: number) {
  let remaining = seconds
  resetPasswordModal.countdown = remaining
  
  resetCountdownTimer = setInterval(() => {
    remaining--
    resetPasswordModal.countdown = remaining
    
    if (remaining < 0) {
      clearInterval(resetCountdownTimer!)
      resetPasswordModal.isSendingCode = false
      resetPasswordModal.countdown = 0
      resetCountdownTimer = null
    }
  }, 1000)
}

// 重置密码
async function resetPassword() {
  const email = resetPasswordModal.email.trim()
  const code = resetPasswordModal.code.trim()
  const newPassword = resetPasswordModal.newPassword
  const confirmPassword = resetPasswordModal.confirmPassword
  
  // 验证输入
  if (!email || !code || !newPassword || !confirmPassword) {
    showResetError('请填写所有字段')
    return
  }
  
  if (!validateEmail(email)) {
    showResetError('请输入有效的邮箱地址')
    return
  }
  
  if (newPassword.length < 6) {
    showResetError('密码长度至少6位')
    return
  }
  
  if (newPassword !== confirmPassword) {
    showResetError('两次输入的密码不一致')
    return
  }
  
  try {
    resetPasswordModal.isResetting = true
    
    const response = await fetch('/api/auth/password/reset', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        email: email,
        verificationCode: code,
        newPassword: newPassword,
        confirmPassword: confirmPassword
      })
    })
    
    const result = await response.json()
    
    if (response.ok && result.success) {
      showResetError('密码重置成功，请使用新密码登录', 'success')
      setTimeout(() => {
        closeResetPassword()
      }, 2000)
    } else {
      showResetError(result.message || '重置失败，请重试')
    }
  } catch (error) {
    showResetError('网络错误，请重试')
  } finally {
    resetPasswordModal.isResetting = false
  }
}

// 页面初始化
onMounted(() => {
  // 加载保存的登录信息
  loadSavedLoginInfo()
})

// 清理定时器
onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  if (resetCountdownTimer) {
    clearInterval(resetCountdownTimer)
  }
})
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.login-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.login-container > div {
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  width: 100%;
  max-width: 400px;
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

.login-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 40px 30px;
  text-align: center;
}

.login-header h1 {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
}

.login-header p {
  opacity: 0.9;
  font-size: 16px;
}

.login-form {
  background: white;
  padding: 40px 30px;
}

/* 全局提示样式 */
.alert {
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 14px;
  font-weight: 500;
  animation: slideDown 0.3s ease;
}

.alert.success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.alert.error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.alert.warning {
  background: #fff3cd;
  color: #856404;
  border: 1px solid #ffeaa7;
}

.alert.info {
  background: #d1ecf1;
  color: #0c5460;
  border: 1px solid #bee5eb;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-tabs {
  display: flex;
  margin-bottom: 30px;
  border-radius: 10px;
  background: #f8f9fa;
  padding: 4px;
}

.tab-btn {
  flex: 1;
  padding: 12px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  color: #666;
}

.tab-btn.active {
  background: white;
  color: #667eea;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.login-form-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.form-group {
  margin-bottom: 25px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.form-group input {
  width: 100%;
  padding: 15px;
  border: 2px solid #e1e5e9;
  border-radius: 10px;
  font-size: 16px;
  transition: all 0.3s ease;
  background-color: #f8f9fa;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
  background-color: white;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-group input.error {
  border-color: #e74c3c;
  background-color: #fdf2f2;
}

.error-message {
  color: #e74c3c;
  font-size: 14px;
  margin-top: 8px;
}

.verification-input-group {
  display: flex;
  gap: 10px;
}

.verification-input-group input {
  flex: 1;
}

.send-code-btn {
  background: #667eea;
  color: white;
  border: none;
  padding: 15px 20px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
  transition: all 0.3s ease;
}

.send-code-btn:hover:not(:disabled) {
  background: #5a6fd8;
  transform: translateY(-1px);
}

.send-code-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.send-code-btn.countdown {
  background: #bbb;
}

.remember-me {
  display: flex;
  align-items: center;
  margin-bottom: 25px;
}

.remember-me input[type="checkbox"] {
  width: auto;
  margin-right: 8px;
}

.remember-me label {
  margin-bottom: 0;
  font-size: 14px;
  color: #666;
}

.forgot-password-link {
  text-align: right;
  margin-bottom: 15px;
}

.forgot-password-link a {
  color: #667eea;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: color 0.3s ease;
}

.forgot-password-link a:hover {
  color: #5a6fd8;
  text-decoration: underline;
}

.login-btn {
  width: 100%;
  padding: 15px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3);
}

.login-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.loading {
  text-align: center;
  padding: 20px;
}

.loading-spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.divider {
  text-align: center;
  margin: 25px 0;
  position: relative;
  color: #999;
}

.divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #e1e5e9;
}

.divider span {
  background: white;
  padding: 0 15px;
}

.register-link {
  text-align: center;
  margin-top: 20px;
}

.register-link a {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.register-link a:hover {
  text-decoration: underline;
}

/* 重置密码模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

.modal-dialog {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 400px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  animation: modalSlideUp 0.3s ease;
}

@keyframes modalSlideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 30px;
  border-bottom: 1px solid #e1e5e9;
}

.modal-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.modal-close:hover {
  background: #f5f5f5;
  color: #666;
}

.modal-body {
  padding: 30px;
}

.reset-step .form-group {
  margin-bottom: 20px;
}

.reset-step .form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.reset-step .form-group input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e1e5e9;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  background: #fafbfc;
}

.reset-step .form-group input:focus {
  outline: none;
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.reset-step .verification-input-group {
  display: flex;
  gap: 10px;
}

.reset-step .verification-input-group input {
  flex: 1;
}

.reset-step .send-code-btn {
  padding: 12px 16px;
  font-size: 13px;
}

.reset-step .login-btn {
  width: 100%;
  padding: 12px;
  margin-top: 20px;
  margin-bottom: 0;
}

.message {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 12px 20px;
  border-radius: 8px;
  font-size: 14px;
  z-index: 1000;
  animation: slideInRight 0.3s ease;
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
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

.message.warning {
  background: #fff3cd;
  color: #856404;
  border: 1px solid #ffeaa7;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-container {
    padding: 10px;
  }
  
  .login-form {
    padding: 30px 20px;
  }
  
  .verification-input-group {
    flex-direction: column;
  }
  
  .send-code-btn {
    width: 100%;
  }
  
  .modal-dialog {
    margin: 10px;
  }
  
  .modal-header,
  .modal-body {
    padding: 20px;
  }
}
</style>