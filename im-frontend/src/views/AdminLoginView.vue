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
          <a href="/" class="back-to-main">返回用户登录</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
// import { login } from '@/api/auth' // 假设有一个管理员登录API

const router = useRouter()
const isLoading = ref(false)
const showPassword = ref(false)

const adminForm = reactive({
  username: '',
  password: '',
  usernameError: '',
  passwordError: ''
})

const alertMessage = reactive({
  show: false,
  text: '',
  type: 'error'
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

function showAlert(message, type = 'error') {
  alertMessage.text = message
  alertMessage.type = type
  alertMessage.show = true
  
  setTimeout(() => {
    alertMessage.show = false
  }, 5000)
}

async function handleAdminLogin() {
  // 表单验证
  if (!adminForm.username.trim()) {
    adminForm.usernameError = '请输入用户名'
    return
  }
  
  if (!adminForm.password) {
    adminForm.passwordError = '请输入密码'
    return
  }
  
  try {
    isLoading.value = true
    
    // 这里应该调用实际的管理员登录API
    // const response = await login({
    //   username: adminForm.username,
    //   password: adminForm.password
    // })
    
    // 模拟登录成功
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // 登录成功后，重定向到管理后台
    router.push('/admin/dashboard')
    
  } catch (error) {
    console.error('登录失败:', error)
    showAlert(error.message || '登录失败，请检查用户名和密码')
  } finally {
    isLoading.value = false
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

.admin-login-footer {
  margin-top: 30px;
  text-align: center;
}

.admin-login-footer p {
  font-size: 14px;
  color: #718096;
  margin-bottom: 10px;
}

.back-to-main {
  color: #3182ce;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
}

.back-to-main:hover {
  text-decoration: underline;
}
</style> 