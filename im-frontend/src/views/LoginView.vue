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
import { getUserSettings } from '@/composables/useUserSettings';

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
  type: 'error' as 'success' | 'error' | 'warning' | 'info',
  timer: null as number | null
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
function showAlert(message: string, type: 'success' | 'error' | 'warning' | 'info' = 'error', duration: number = 5000) {
  alertMessage.text = message;
  alertMessage.type = type;
  alertMessage.show = true;
  
  // 清除任何现有的超时
  if (alertMessage.timer) {
    clearTimeout(alertMessage.timer);
  }
  
  // 设置新的超时
  alertMessage.timer = setTimeout(() => {
    clearAlert();
  }, duration);
}

// 清除提示
function clearAlert() {
  alertMessage.show = false;
  alertMessage.text = '';
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

// 保存用户数据并应用设置
const saveUserData = (authData: any) => {
  const rememberMe = passwordForm.rememberMe;
  
  // 确保我们有用户ID
  if (!authData || !authData.userInfo || !authData.userInfo.id) {
    console.error('保存用户数据失败：无效的用户信息', authData);
    return;
  }
  
  const userId = String(authData.userInfo.id);
  console.log('保存用户数据，用户ID:', userId);
  
  if (rememberMe) {
    // 记住登录状态：保存到localStorage（持久化）
    localStorage.setItem('accessToken', authData.accessToken);
    localStorage.setItem('refreshToken', authData.refreshToken);
    localStorage.setItem('userInfo', JSON.stringify(authData.userInfo));
    localStorage.setItem('userId', userId);
    // 保存邮箱、密码和记住我状态，用于下次自动填充
    localStorage.setItem('savedEmail', passwordForm.email);
    // 只有在后端返回密码时才保存（安全考虑）
    if (authData.userInfo && authData.userInfo.password) {
      localStorage.setItem('savedPassword', authData.userInfo.password);
    }
    localStorage.setItem('rememberMe', 'true');
    // 清除sessionStorage中可能存在的数据
    sessionStorage.removeItem('accessToken');
    sessionStorage.removeItem('refreshToken');
    sessionStorage.removeItem('userInfo');
    sessionStorage.removeItem('userId');
  } else {
    // 不记住登录状态：保存到sessionStorage（会话级别）
    sessionStorage.setItem('accessToken', authData.accessToken);
    sessionStorage.setItem('refreshToken', authData.refreshToken);
    sessionStorage.setItem('userInfo', JSON.stringify(authData.userInfo));
    sessionStorage.setItem('userId', userId);
    // 清除localStorage中的登录信息
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userInfo');
    localStorage.removeItem('userId');
    localStorage.removeItem('savedEmail');
    localStorage.removeItem('savedPassword');
    localStorage.removeItem('rememberMe');
    // 清空密码输入框，防止刷新页面后密码仍然存在
    passwordForm.password = '';
  }
  
  // 在控制台输出当前存储的用户ID，用于调试
  console.log('当前存储的用户ID:',
    'localStorage:', localStorage.getItem('userId'),
    'sessionStorage:', sessionStorage.getItem('userId')
  );
};

// 加载用户设置并应用
const loadAndApplyUserSettings = async () => {
  // 加载并应用用户个性化设置
  const { fetchSettings, applySettingsToUI, settings } = getUserSettings();
  try {
    console.log('正在加载用户个性化设置...');
    
    // 检查settings是否为null，如果是则初始化默认值
    if (!settings.value) {
      console.log('设置为空，初始化默认值');
      settings.value = {
        theme: {
          color: '#1890ff',
          chatBackground: 'default',
          fontSize: 14
        },
        privacy: {
          showOnlineStatus: true,
          allowFriendRequests: true,
          showLastSeen: true
        },
        notification: {
          enableNotifications: true,
          enableSoundNotifications: true,
          enableVibration: true
        }
      };
    }
    
    // 检查用户ID是否已保存
    const userId = localStorage.getItem('userId') || sessionStorage.getItem('userId');
    console.log('当前用户ID:', userId);
    
    if (!userId) {
      console.warn('无法加载用户设置：用户ID未保存');
      return;
    }
    
    // 从服务器获取设置
    await fetchSettings();
    console.log('设置加载成功，正在应用...', settings.value);
    
    // 确保多次应用设置，以防设置未正确应用
    applySettingsToUI();
    
    // 延迟再次应用，确保DOM已更新
    setTimeout(() => {
      console.log('延迟再次应用设置...', settings.value);
      if (!settings.value) {
        console.warn('设置仍然为空，初始化默认值');
        settings.value = {
          theme: {
            color: '#1890ff',
            chatBackground: 'default',
            fontSize: 14
          },
          privacy: {
            showOnlineStatus: true,
            allowFriendRequests: true,
            showLastSeen: true
          },
          notification: {
            enableNotifications: true,
            enableSoundNotifications: true,
            enableVibration: true
          }
        };
      }
      
      applySettingsToUI();
      
      // 强制刷新样式
      document.body.style.visibility = 'hidden';
      setTimeout(() => {
        document.body.style.visibility = '';
      }, 5);
    }, 500);
    
    console.log('用户设置已成功应用');
  } catch (error) {
    console.error('加载用户设置失败:', error);
    // 即使加载失败，仍然继续登录流程
  }
};

// 处理密码登录
async function handlePasswordLogin() {
  if (!validatePasswordForm()) {
    return;
  }
  
  // 显示加载状态
  isLoading.value = true;
  
  try {
    const deviceInfo = getDeviceInfo();
    const email = passwordForm.email.trim();
    const password = passwordForm.password;

    if (!email || !password) {
      showAlert('邮箱和密码不能为空', 'error');
      isLoading.value = false;
      return;
    }

    const formData = {
      email: email,
      password: password,
      deviceType: deviceInfo.deviceType,
      deviceInfo: deviceInfo.deviceInfo,
      rememberMe: passwordForm.rememberMe
    };
    
    const response = await fetch('/api/auth/login/password', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(formData)
    });
    
    const result = await response.json();
    
    if (response.ok && result.success) {
      // 登录成功
      const authData = result.data;
      
      // 检查用户ID是否存在
      if (!authData.userInfo || !authData.userInfo.id) {
        console.error('登录成功但用户ID缺失:', authData);
        showAlert('登录成功但无法获取用户信息，请重试', 'error');
        isLoading.value = false;
        return;
      }
      
      // 存储认证信息
      saveUserData(authData);
      
      // 加载并应用用户设置
      await loadAndApplyUserSettings();
      
      // 立即连接WebSocket
      connectWebSocket();
      console.log('登录成功，已激活WebSocket连接');
      
      showAlert('登录成功，正在跳转...', 'success');
      
      // 延迟跳转到仪表板
      setTimeout(() => {
        router.push('/dashboard');
      }, 1500);
      
    } else {
      // 登录失败
      const errorMessage = result.message || '登录失败，请重试';
      
      // 特殊处理封禁账号错误
      if (result.code) {
        switch (result.code) {
          case 'ACCOUNT_BANNED_TEMP':
            showAlert(`您的账号已被临时封禁：${errorMessage}`, 'error', 10000);
            break;
          case 'ACCOUNT_BANNED_PERM':
            showAlert(`您的账号已被永久封禁：${errorMessage}`, 'error', 10000);
            break;
          default:
            showAlert(errorMessage, 'error');
        }
      } else {
        showAlert(errorMessage, 'error');
      }
      
      // 恢复表单显示
      isLoading.value = false;
      
      // 清空密码输入框
      passwordForm.password = '';
    }
    
  } catch (error) {
    console.error('登录请求失败:', error);
    showAlert('网络错误，请检查网络连接后重试', 'error');
    
    // 恢复表单显示
    isLoading.value = false;
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
    return;
  }
  
  // 显示加载状态
  isLoading.value = true;
  
  try {
    const deviceInfo = getDeviceInfo();
    const email = verificationForm.email.trim();
    const code = verificationForm.code.trim();
    
    if (!email || !code) {
      showAlert('邮箱和验证码不能为空', 'error');
      isLoading.value = false;
      return;
    }
    
    const formData = {
      email: email,
      verificationCode: code,
      deviceType: deviceInfo.deviceType,
      deviceInfo: deviceInfo.deviceInfo
    };
    
    const response = await fetch('/api/auth/login/verification', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(formData)
    });
    
    const result = await response.json();
    
    if (response.ok && result.success) {
      // 登录成功
      const authData = result.data;
      
      // 检查用户ID是否存在
      if (!authData.userInfo || !authData.userInfo.id) {
        console.error('登录成功但用户ID缺失:', authData);
        showAlert('登录成功但无法获取用户信息，请重试', 'error');
        isLoading.value = false;
        return;
      }
      
      // 存储认证信息到sessionStorage（验证码登录默认不记住）
      saveUserData({...authData, rememberMe: false});
      
      // 加载并应用用户设置
      await loadAndApplyUserSettings();
      
      // 立即连接WebSocket
      connectWebSocket();
      console.log('登录成功，已激活WebSocket连接');
      
      showAlert('登录成功，正在跳转...', 'success');
      
      // 延迟跳转到仪表板
      setTimeout(() => {
        router.push('/dashboard');
      }, 1500);
      
    } else {
      // 登录失败
      const errorMessage = result.message || '验证码登录失败，请重试';
      
      // 特殊处理封禁账号错误
      if (result.code) {
        switch (result.code) {
          case 'ACCOUNT_BANNED_TEMP':
            showAlert(`您的账号已被临时封禁：${errorMessage}`, 'error', 10000);
            break;
          case 'ACCOUNT_BANNED_PERM':
            showAlert(`您的账号已被永久封禁：${errorMessage}`, 'error', 10000);
            break;
          default:
            showAlert(errorMessage, 'error');
        }
      } else {
        showAlert(errorMessage, 'error');
      }
      
      // 恢复表单显示
      isLoading.value = false;
      
      // 清空验证码输入框
      verificationForm.code = '';
    }
    
  } catch (error) {
    console.error('登录请求失败:', error);
    showAlert('网络错误，请检查网络连接后重试', 'error');
    
    // 恢复表单显示
    isLoading.value = false;
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
  background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%);
  padding: 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.login-container > div {
  border-radius: 24px;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  width: 100%;
  max-width: 420px;
  animation: fadeIn 0.8s ease-out;
  backdrop-filter: blur(10px);
  background-color: rgba(255, 255, 255, 0.95);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(40px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-header {
  background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%);
  color: white;
  padding: 50px 30px;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.login-header::before {
  content: "";
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.2) 0%, rgba(255,255,255,0) 60%);
  opacity: 0.6;
}

.login-header h1 {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 12px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  position: relative;
}

.login-header p {
  opacity: 0.9;
  font-size: 18px;
  position: relative;
}

.login-form {
  background: white;
  padding: 40px 35px;
}

/* 全局提示样式 */
.alert {
  padding: 14px 18px;
  border-radius: 12px;
  margin-bottom: 24px;
  font-size: 14px;
  font-weight: 500;
  animation: slideDown 0.4s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.alert.success {
  background: rgba(209, 250, 229, 0.8);
  color: #047857;
  border-left: 4px solid #10b981;
}

.alert.error {
  background: rgba(254, 226, 226, 0.8);
  color: #b91c1c;
  border-left: 4px solid #ef4444;
}

.alert.warning {
  background: rgba(254, 243, 199, 0.8);
  color: #92400e;
  border-left: 4px solid #f59e0b;
}

.alert.info {
  background: rgba(219, 234, 254, 0.8);
  color: #1e40af;
  border-left: 4px solid #3b82f6;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-tabs {
  display: flex;
  margin-bottom: 35px;
  border-radius: 16px;
  background: #f8f9fa;
  padding: 5px;
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.03);
}

.tab-btn {
  flex: 1;
  padding: 14px;
  border: none;
  background: transparent;
  border-radius: 12px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s ease;
  color: #64748b;
}

.tab-btn.active {
  background: white;
  color: #4f46e5;
  box-shadow: 0 4px 15px rgba(79, 70, 229, 0.15);
}

.tab-btn:hover:not(.active) {
  background: rgba(255, 255, 255, 0.5);
  color: #1e293b;
}

.login-form-content {
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(15px); }
  to { opacity: 1; transform: translateY(0); }
}

.form-group {
  margin-bottom: 28px;
  position: relative;
}

.form-group label {
  display: block;
  margin-bottom: 10px;
  font-weight: 600;
  color: #1e293b;
  font-size: 15px;
}

.form-group input {
  width: 100%;
  padding: 16px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  font-size: 16px;
  transition: all 0.3s ease;
  background-color: #f8fafc;
}

.form-group input:focus {
  outline: none;
  border-color: #4f46e5;
  background-color: white;
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.1);
}

.form-group input.error {
  border-color: #ef4444;
  background-color: #fef2f2;
}

.error-message {
  color: #ef4444;
  font-size: 14px;
  margin-top: 8px;
  font-weight: 500;
}

.verification-input-group {
  display: flex;
  gap: 12px;
}

.verification-input-group input {
  flex: 1;
}

.send-code-btn {
  background: #4f46e5;
  color: white;
  border: none;
  padding: 0 20px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
  transition: all 0.3s ease;
  font-weight: 600;
}

.send-code-btn:hover:not(:disabled) {
  background: #4338ca;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
}

.send-code-btn:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}

.send-code-btn.countdown {
  background: #94a3b8;
}

.remember-me {
  display: flex;
  align-items: center;
  margin-bottom: 28px;
}

.remember-me input[type="checkbox"] {
  width: 18px;
  height: 18px;
  margin-right: 10px;
  accent-color: #4f46e5;
}

.remember-me label {
  margin-bottom: 0;
  font-size: 15px;
  color: #334155;
  cursor: pointer;
}

.forgot-password-link {
  text-align: right;
  margin-bottom: 20px;
}

.forgot-password-link a {
  color: #4f46e5;
  text-decoration: none;
  font-size: 15px;
  font-weight: 600;
  transition: color 0.3s ease;
}

.forgot-password-link a:hover {
  color: #4338ca;
  text-decoration: underline;
}

.login-btn {
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 25px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  box-shadow: 0 4px 15px rgba(79, 70, 229, 0.3);
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 10px 25px rgba(79, 70, 229, 0.4);
}

.login-btn:disabled {
  background: linear-gradient(135deg, #a1a1aa 0%, #71717a 100%);
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.loading {
  text-align: center;
  padding: 30px;
}

.loading-spinner {
  display: inline-block;
  width: 30px;
  height: 30px;
  border: 3px solid rgba(79, 70, 229, 0.2);
  border-top: 3px solid #4f46e5;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.divider {
  text-align: center;
  margin: 30px 0;
  position: relative;
  color: #64748b;
}

.divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #e2e8f0;
}

.divider span {
  background: white;
  padding: 0 20px;
  position: relative;
  font-size: 15px;
}

.register-link {
  text-align: center;
  font-size: 15px;
  color: #334155;
}

.register-link a {
  color: #4f46e5;
  text-decoration: none;
  font-weight: 600;
  margin-left: 5px;
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
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

.modal-dialog {
  background: white;
  border-radius: 20px;
  width: 90%;
  max-width: 450px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.2);
  animation: modalSlideUp 0.4s ease;
}

@keyframes modalSlideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
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
  padding: 25px 30px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  font-size: 28px;
  color: #64748b;
  cursor: pointer;
  padding: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.modal-close:hover {
  background: #f1f5f9;
  color: #334155;
}

.modal-body {
  padding: 30px;
}

.reset-step .form-group {
  margin-bottom: 24px;
}

.reset-step .form-group label {
  display: block;
  margin-bottom: 10px;
  font-weight: 600;
  color: #1e293b;
  font-size: 15px;
}

.reset-step .form-group input {
  width: 100%;
  padding: 16px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  font-size: 16px;
  transition: all 0.3s ease;
  background-color: #f8fafc;
}

.reset-step .form-group input:focus {
  outline: none;
  border-color: #4f46e5;
  background-color: white;
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.1);
}

.reset-step .verification-input-group {
  display: flex;
  gap: 12px;
}

.reset-step .verification-input-group input {
  flex: 1;
}

.reset-step .send-code-btn {
  padding: 0 20px;
  font-size: 14px;
}

.reset-step .login-btn {
  width: 100%;
  padding: 16px;
  margin-top: 24px;
  margin-bottom: 0;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-container {
    padding: 15px;
  }
  
  .login-form {
    padding: 30px 25px;
  }

  .login-header {
    padding: 40px 25px;
  }
  
  .verification-input-group {
    flex-direction: column;
  }
  
  .send-code-btn {
    width: 100%;
    padding: 14px;
  }
  
  .modal-dialog {
    margin: 15px;
  }
  
  .modal-header,
  .modal-body {
    padding: 20px;
  }

  .form-group input {
    padding: 14px;
    font-size: 15px;
  }

  .login-btn {
    padding: 14px;
  }
}
</style>