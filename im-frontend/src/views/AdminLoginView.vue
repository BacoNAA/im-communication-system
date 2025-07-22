<template>
  <div class="admin-login-container">
    <div class="admin-login-card">
      <div class="admin-login-header">
        <h1>管理员登录</h1>
        <p>请输入您的管理员账号和密码</p>
      </div>
      
      <div class="admin-login-form">
        <!-- 全局提示 -->
        <div v-if="alertMessage.show" :class="['alert', alertMessage.type]">
          {{ alertMessage.text }}
        </div>
        
        <form @submit.prevent="handleAdminLogin" class="admin-login-form-content">
          <div class="form-group">
            <label for="username">用户名</label>
            <input
              id="username"
              v-model="adminForm.username"
              type="text"
              :class="{ error: adminForm.usernameError }"
              placeholder="请输入管理员用户名"
              @input="handleUsernameInput"
              required
            />
            <div v-if="adminForm.usernameError" class="error-message">{{ adminForm.usernameError }}</div>
          </div>

          <div class="form-group">
            <label for="adminPassword">密码</label>
            <div class="password-input-container">
              <input
                id="adminPassword"
                v-model="adminForm.password"
                :type="showPassword ? 'text' : 'password'"
                :class="{ error: adminForm.passwordError }"
                placeholder="请输入管理员密码"
                autocomplete="off"
                @input="handlePasswordInput"
                required
              />
              <button 
                type="button" 
                class="toggle-password" 
                @click="togglePasswordVisibility"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              >
                <i :class="showPassword ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
              </button>
            </div>
            <div v-if="adminForm.passwordError" class="error-message">{{ adminForm.passwordError }}</div>
          </div>
          
          <div class="form-group remember-me">
            <label class="checkbox-container">
              <input type="checkbox" v-model="adminForm.rememberMe">
              <span class="checkmark"></span>
              记住登录状态
            </label>
          </div>

          <button 
            type="submit" 
            class="admin-login-btn"
            :disabled="isLoading"
          >
            <span v-if="isLoading" class="loading-spinner"></span>
            <span v-else>登录</span>
          </button>
        </form>

        <div class="admin-login-footer">
          <p>仅限系统管理员访问</p>
          <div class="admin-login-links">
          <a href="/" class="back-to-main">返回用户登录</a>
            <span class="link-divider">|</span>
            <a href="#" class="reset-password-link" @click.prevent="showResetPasswordModal = true">重置密码</a>
          </div>
        </div>
      </div>
    </div>

    <!-- 重置密码模态框 -->
    <div v-if="showResetPasswordModal" class="modal-overlay" @click.self="showResetPasswordModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>重置管理员密码</h3>
          <button class="modal-close" @click="showResetPasswordModal = false">&times;</button>
        </div>
        
        <div class="modal-body">
          <!-- 重置密码表单 -->
          <form @submit.prevent="handleResetPassword">
            <div class="form-group">
              <label for="resetUsername">管理员用户名</label>
              <input
                id="resetUsername"
                v-model="resetForm.username"
                type="text"
                :class="{ error: resetForm.usernameError }"
                placeholder="请输入管理员用户名"
                required
              />
              <div v-if="resetForm.usernameError" class="error-message">{{ resetForm.usernameError }}</div>
            </div>

            <div class="form-group">
              <label for="resetNewPassword">新密码</label>
              <div class="password-input-container">
                <input
                  id="resetNewPassword"
                  v-model="resetForm.newPassword"
                  :type="showResetPassword ? 'text' : 'password'"
                  :class="{ error: resetForm.newPasswordError }"
                  placeholder="请输入新密码"
                  required
                />
                <button 
                  type="button" 
                  class="toggle-password" 
                  @click="showResetPassword = !showResetPassword"
                >
                  <i :class="showResetPassword ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                </button>
              </div>
              <div v-if="resetForm.newPasswordError" class="error-message">{{ resetForm.newPasswordError }}</div>
            </div>

            <div class="form-group">
              <label for="resetConfirmPassword">确认新密码</label>
              <div class="password-input-container">
                <input
                  id="resetConfirmPassword"
                  v-model="resetForm.confirmPassword"
                  :type="showResetPassword ? 'text' : 'password'"
                  :class="{ error: resetForm.confirmPasswordError }"
                  placeholder="请再次输入新密码"
                  required
                />
              </div>
              <div v-if="resetForm.confirmPasswordError" class="error-message">{{ resetForm.confirmPasswordError }}</div>
            </div>

            <div class="form-actions">
              <button 
                type="button" 
                class="cancel-btn"
                @click="showResetPasswordModal = false"
              >
                取消
              </button>
              <button 
                type="submit" 
                class="submit-btn"
                :disabled="isResetting"
              >
                <span v-if="isResetting" class="loading-spinner"></span>
                <span v-else>重置密码</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminAuth } from '@/composables/useAdminAuth'
import adminApi from '@/api/admin'

const router = useRouter()
const { adminLogin, checkAdminAuth } = useAdminAuth()
const isLoading = ref(false)
const showPassword = ref(false)
const showResetPassword = ref(false)
const showResetPasswordModal = ref(false)
const isResetting = ref(false)

const adminForm = reactive({
  username: '',
  password: '',
  rememberMe: false,
  usernameError: '',
  passwordError: ''
})

const resetForm = reactive({
  username: '',
  newPassword: '',
  confirmPassword: '',
  usernameError: '',
  newPasswordError: '',
  confirmPasswordError: ''
})

const alertMessage = reactive({
  show: false,
  text: '',
  type: 'error' as 'error' | 'success' | 'info'
})

// 检查是否已登录
onMounted(async () => {
  try {
    console.log('检查管理员登录状态...')
    const isAuthenticated = await checkAdminAuth()
    console.log('管理员登录状态检查结果:', isAuthenticated)
    
    if (isAuthenticated) {
      console.log('管理员已登录，准备跳转到管理后台...')
      router.push('/admin/dashboard')
    } else {
      console.log('管理员未登录，显示登录页面')
    }
  } catch (error) {
    console.error('检查管理员登录状态失败:', error)
  }
})

function handleUsernameInput() {
  adminForm.usernameError = ''
}

function handlePasswordInput() {
  adminForm.passwordError = ''
}

function togglePasswordVisibility() {
  showPassword.value = !showPassword.value
}

function showAlert(message: string, type: 'error' | 'success' | 'info' = 'error') {
  alertMessage.text = message
  alertMessage.type = type
  alertMessage.show = true
  
  setTimeout(() => {
    alertMessage.show = false
  }, 5000)
}

function validateForm(): boolean {
  let isValid = true
  
  if (!adminForm.username.trim()) {
    adminForm.usernameError = '请输入用户名'
    isValid = false
  }
  
  if (!adminForm.password) {
    adminForm.passwordError = '请输入密码'
    isValid = false
  }
  
  return isValid
}

function validateResetForm(): boolean {
  let isValid = true

  // 清除所有错误
  resetForm.usernameError = ''
  resetForm.newPasswordError = ''
  resetForm.confirmPasswordError = ''
  
  if (!resetForm.username.trim()) {
    resetForm.usernameError = '请输入用户名'
    isValid = false
  }
  
  if (!resetForm.newPassword) {
    resetForm.newPasswordError = '请输入新密码'
    isValid = false
  } else if (resetForm.newPassword.length < 6) {
    resetForm.newPasswordError = '密码长度不能少于6位'
    isValid = false
  }
  
  if (!resetForm.confirmPassword) {
    resetForm.confirmPasswordError = '请确认新密码'
    isValid = false
  } else if (resetForm.newPassword !== resetForm.confirmPassword) {
    resetForm.confirmPasswordError = '两次输入的密码不一致'
    isValid = false
  }
  
  return isValid
}

async function handleAdminLogin() {
  // 表单验证
  if (!validateForm()) {
    return
  }
  
  // 显示加载状态
  isLoading.value = true
  
  try {
    console.log('开始管理员登录请求:', { username: adminForm.username, rememberMe: adminForm.rememberMe })
    
    // 调用登录API
    const adminInfo = await adminLogin(
      adminForm.username,
      adminForm.password,
      adminForm.rememberMe
    )
    
    console.log('管理员登录成功:', adminInfo)
    
    // 登录成功
    showAlert('登录成功，正在跳转...', 'success')
    
    // 检查本地存储是否正确保存了令牌
    const savedToken = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken')
    const savedInfo = localStorage.getItem('adminInfo') || sessionStorage.getItem('adminInfo')
    
    console.log('存储的令牌和信息检查:', { 
      hasToken: !!savedToken, 
      tokenPrefix: savedToken ? savedToken.substring(0, 10) + '...' : '无',
      hasInfo: !!savedInfo
    })
    
    // 延迟跳转到管理后台
    setTimeout(() => {
      console.log('准备跳转到管理后台...')
      router.push('/admin/dashboard')
    }, 1000)
    
  } catch (error: any) {
    console.error('管理员登录失败:', error)
    showAlert(error.message || '登录失败，请检查用户名和密码')
  } finally {
    isLoading.value = false
  }
}

async function handleResetPassword() {
  // 表单验证
  if (!validateResetForm()) {
    return
  }
  
  // 显示加载状态
  isResetting.value = true
  
  try {
    console.log('提交重置密码请求:', {
      username: resetForm.username,
      newPassword: resetForm.newPassword.length + ' 位长度密码' // 安全起见不打印实际密码
    })
    
    // 先显示处理中的提示
    showAlert('正在处理请求...', 'info')
    
    // 调用重置密码API
    const response = await adminApi.resetPassword({
      username: resetForm.username,
      newPassword: resetForm.newPassword
    })
    
    console.log('重置密码API响应:', { 
      success: response.success, 
      message: response.message,
      code: response.code
    })
    
    // 在成功重置密码后修正用户名填充
    if (response.success) {
      // 重置密码成功
      showAlert('密码重置成功，请使用新密码登录', 'success')
      
      // 保存用户名供后续使用
      const resetUsername = resetForm.username
      
      // 关闭模态框
      showResetPasswordModal.value = false
      
      // 清空表单
      resetForm.username = ''
      resetForm.newPassword = ''
      resetForm.confirmPassword = ''
      
      // 填充登录表单中的用户名，方便用户直接登录
      adminForm.username = resetUsername
    } else {
      showAlert(response.message || '重置密码失败，请检查用户名是否正确')
    }
    
  } catch (error: any) {
    console.error('重置密码请求失败:', error)
    
    // 更详细的错误信息
    let errorMessage = '重置密码失败'
    
    // 尝试提取具体错误信息
    if (error.message) {
      if (error.message.includes('404')) {
        errorMessage = '用户名不存在'
      } else if (error.message.includes('400')) {
        errorMessage = '密码格式不正确'
      } else {
        errorMessage += ': ' + error.message
      }
    }
    
    showAlert(errorMessage)
  } finally {
    isResetting.value = false
  }
}
</script>

<style scoped>
.admin-login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f7fa;
  padding: 20px;
}

.admin-login-card {
  width: 100%;
  max-width: 450px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.admin-login-header {
  padding: 30px;
  text-align: center;
  background-color: #1a202c;
  color: white;
}

.admin-login-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.admin-login-header p {
  margin: 10px 0 0;
  font-size: 16px;
  opacity: 0.8;
}

.admin-login-form {
  padding: 30px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #4a5568;
}

.form-group input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 16px;
  transition: border-color 0.2s;
}

.form-group input:focus {
  border-color: #4299e1;
  outline: none;
  box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.2);
}

.form-group input.error {
  border-color: #e53e3e;
}

.error-message {
  color: #e53e3e;
  font-size: 14px;
  margin-top: 5px;
}

.password-input-container {
  position: relative;
}

.toggle-password {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: #718096;
}

.remember-me {
  display: flex;
  align-items: center;
}

.checkbox-container {
  display: flex;
  align-items: center;
  position: relative;
  padding-left: 30px;
  cursor: pointer;
  font-size: 14px;
  user-select: none;
  color: #4a5568;
}

.checkbox-container input {
  position: absolute;
  opacity: 0;
  cursor: pointer;
  height: 0;
  width: 0;
}

.checkmark {
  position: absolute;
  left: 0;
  height: 20px;
  width: 20px;
  background-color: #f7fafc;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
}

.checkbox-container:hover input ~ .checkmark {
  background-color: #edf2f7;
}

.checkbox-container input:checked ~ .checkmark {
  background-color: #4299e1;
  border-color: #4299e1;
}

.checkmark:after {
  content: "";
  position: absolute;
  display: none;
}

.checkbox-container input:checked ~ .checkmark:after {
  display: block;
}

.checkbox-container .checkmark:after {
  left: 7px;
  top: 3px;
  width: 5px;
  height: 10px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.admin-login-btn {
  width: 100%;
  padding: 14px;
  background-color: #3182ce;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  justify-content: center;
  align-items: center;
}

.admin-login-btn:hover {
  background-color: #2b6cb0;
}

.admin-login-btn:disabled {
  background-color: #a0aec0;
  cursor: not-allowed;
}

.loading-spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: #ffffff;
  animation: spin 1s ease-in-out infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.alert {
  padding: 12px 16px;
  border-radius: 6px;
  margin-bottom: 20px;
  font-size: 14px;
}

.alert.error {
  background-color: #fed7d7;
  color: #c53030;
}

.alert.success {
  background-color: #c6f6d5;
  color: #2f855a;
}

.alert.info {
  background-color: #e2e8f0;
  color: #4a5568;
}

.admin-login-footer {
  margin-top: 30px;
  text-align: center;
}

.admin-login-footer p {
  font-size: 14px;
  color: #718096;
  margin-bottom: 10px;
}

.admin-login-links {
  display: flex;
  justify-content: center;
  align-items: center;
}

.back-to-main, .reset-password-link {
  color: #3182ce;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
}

.back-to-main:hover, .reset-password-link:hover {
  text-decoration: underline;
}

.link-divider {
  margin: 0 10px;
  color: #a0aec0;
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background-color: white;
  border-radius: 8px;
  width: 100%;
  max-width: 450px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #2d3748;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #a0aec0;
  padding: 0;
}

.modal-close:hover {
  color: #4a5568;
}

.modal-body {
  padding: 20px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.cancel-btn {
  padding: 10px 16px;
  background-color: #e2e8f0;
  color: #4a5568;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.submit-btn {
  padding: 10px 16px;
  background-color: #3182ce;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  min-width: 100px;
}

.cancel-btn:hover {
  background-color: #cbd5e0;
}

.submit-btn:hover {
  background-color: #2b6cb0;
}

.submit-btn:disabled {
  background-color: #a0aec0;
  cursor: not-allowed;
}
</style> 