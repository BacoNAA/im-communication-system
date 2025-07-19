<template>
  <div class="websocket-test">
    <h2>WebSocket 连接测试</h2>
    
    <div class="status-box">
      <div class="status-indicator" :class="{ 'connected': isConnected }">
        <div class="status-dot"></div>
        <div class="status-text">{{ statusText }}</div>
      </div>
      
      <div class="network-indicator">
        <div class="network-dot" :class="{ 'online': isOnline }"></div>
        <div class="network-text">网络: {{ networkStatus }}</div>
      </div>
      
      <div class="ws-url-input">
        <input v-model="wsUrl" placeholder="WebSocket URL" :disabled="isConnected" />
      </div>
      
      <div class="ws-protocol-options">
        <h4>连接选项:</h4>
        <div class="protocol-selector">
          <label>
            <input type="radio" v-model="wsProtocol" value="native" :disabled="isConnected" />
            原生WebSocket (/ws-native)
          </label>
          <label>
            <input type="radio" v-model="wsProtocol" value="stomp" :disabled="isConnected" />
            STOMP协议 (/ws)
          </label>
        </div>
      </div>
      
      <div class="actions">
        <button @click="connect" :disabled="isConnected">连接</button>
        <button @click="disconnect" :disabled="!isConnected">断开</button>
        <button @click="resetConnection" class="reset-btn">重置</button>
      </div>
    </div>
    
    <!-- 添加token手动输入选项 -->
    <div class="auth-options">
      <h4>认证选项:</h4>
      <div class="auth-selector">
        <label>
          <input type="checkbox" v-model="useAuth" :disabled="isConnected" />
          使用认证Token
        </label>
        
        <div v-if="useAuth" class="token-input">
          <input 
            v-model="manualToken" 
            placeholder="手动输入token (可选)" 
            :disabled="isConnected" 
            class="token-field"
          />
          <button @click="copyTokenFromStorage" :disabled="isConnected" class="copy-btn">
            复制存储中的Token
          </button>
        </div>
      </div>
    </div>
    
    <div class="quick-connect">
      <h4>快速连接:</h4>
      <div class="quick-connect-buttons">
        <button 
          v-for="(item, index) in quickConnectUrls" 
          :key="index"
          @click="useQuickConnectUrl(item.url)"
          :disabled="isConnected"
          :class="{ active: wsUrl === item.url }"
        >
          {{ item.name }}
        </button>
      </div>
    </div>
    
    <div class="test-message-box">
      <h3>发送测试消息</h3>
      <div class="input-group">
        <input v-model="testMessage" placeholder="输入测试消息..." />
        <button @click="sendMessage" :disabled="!isConnected">发送</button>
        <button @click="sendPing" :disabled="!isConnected" class="ping-btn">PING</button>
      </div>
      
      <div class="message-templates">
        <h4>消息模板:</h4>
        <button @click="useMessageTemplate('text')" :disabled="!isConnected" class="template-btn">
          文本消息
        </button>
        <button @click="useMessageTemplate('ping')" :disabled="!isConnected" class="template-btn">
          Ping
        </button>
        <button @click="useMessageTemplate('message')" :disabled="!isConnected" class="template-btn">
          消息对象
        </button>
        <button @click="useMessageTemplate('typing')" :disabled="!isConnected" class="template-btn">
          输入状态
        </button>
      </div>
    </div>
    
    <div class="logs">
      <div class="logs-header">
        <h3>WebSocket 日志</h3>
        <button @click="clearLogs" class="clear-btn">清除日志</button>
        <button @click="checkNetworkConnectivity" class="check-btn">检查网络</button>
      </div>
      <div class="log-container">
        <div v-for="(log, index) in logs" :key="index" class="log-item" :class="log.type">
          <div class="log-time">{{ formatTime(log.time) }}</div>
          <div class="log-content">{{ log.message }}</div>
        </div>
      </div>
    </div>
    
    <div class="debug-info">
      <h3>调试信息</h3>
      <pre>{{ debugInfo }}</pre>
    </div>

    <div class="connection-details">
      <h3>连接详情</h3>
      <div class="detail-item">
        <span class="detail-label">当前URL:</span>
        <span class="detail-value">{{ wsUrl }}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">连接状态:</span>
        <span class="detail-value" :class="status">{{ statusText }}</span>
      </div>
      <div class="detail-item" v-if="errorMessage">
        <span class="detail-label">错误信息:</span>
        <span class="detail-value error">{{ errorMessage }}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">协议:</span>
        <span class="detail-value">{{ wsProtocol === 'native' ? '原生WebSocket' : 'STOMP协议' }}</span>
      </div>
    </div>

    <div class="port-options">
      <h4>端口选项:</h4>
      <div class="port-buttons">
        <button 
          @click="changePort(8080)" 
          :class="{ active: wsUrl.includes(':8080/') }"
          :disabled="isConnected"
        >
          端口 8080
        </button>
        <button 
          @click="changePort(3000)" 
          :class="{ active: wsUrl.includes(':3000/') }"
          :disabled="isConnected"
        >
          端口 3000
        </button>
        <button 
          @click="changePort(8081)" 
          :class="{ active: wsUrl.includes(':8081/') }"
          :disabled="isConnected"
        >
          端口 8081
        </button>
        <button 
          @click="changePort(80)" 
          :class="{ active: wsUrl.includes(':80/') || (!wsUrl.includes('://localhost:') && wsUrl.startsWith('ws://')) }"
          :disabled="isConnected"
        >
          端口 80 (默认)
        </button>
      </div>
    </div>

    <div class="auth-options">
      <h4>认证选项:</h4>
      <div class="auth-selector">
        <label>
          <input type="checkbox" v-model="useAuth" :disabled="isConnected" />
          使用认证Token
        </label>
      </div>
    </div>

    <div class="path-options">
      <h4>路径选项:</h4>
      <div class="path-buttons">
        <button 
          @click="changePath('/ws-native')" 
          :class="{ active: wsUrl.includes('/ws-native') }"
          :disabled="isConnected"
        >
          /ws-native (原生WebSocket)
        </button>
        <button 
          @click="changePath('/ws')" 
          :class="{ active: wsUrl.includes('/ws') && !wsUrl.includes('/ws-native') }"
          :disabled="isConnected"
        >
          /ws (STOMP)
        </button>
        <button 
          @click="changePath('/websocket')" 
          :class="{ active: wsUrl.includes('/websocket') }"
          :disabled="isConnected"
        >
          /websocket
        </button>
        <button 
          @click="changePath('/socket')" 
          :class="{ active: wsUrl.includes('/socket') }"
          :disabled="isConnected"
        >
          /socket
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useWebSocket, reconnectWithNewUrl } from '@/composables/useWebSocket';

// 定义WebSocketStatus类型
type WebSocketStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

// 是否使用认证
const useAuth = ref(true);

// 手动输入token
const manualToken = ref('');

// 默认使用代理模式的WebSocket URL
const wsUrl = ref('/ws-native');

// 添加一个测试WebSocket URL列表，用户可以快速切换
const quickConnectUrls = [
  { name: '本地原生WS (代理)', url: '/ws-native' },
  { name: '本地STOMP (代理)', url: '/ws' },
  { name: '直连原生WS (8080)', url: 'ws://localhost:8080/ws-native' },
  { name: '直连STOMP (8080)', url: 'ws://localhost:8080/ws' },
  { name: 'Nginx原生WS (80)', url: 'ws://localhost/ws-native' },
  { name: 'Nginx STOMP (80)', url: 'ws://localhost/ws' },
];

// 测试消息
const testMessage = ref('Hello WebSocket!');

// 日志
interface Log {
  time: Date;
  message: string;
  type: 'info' | 'error' | 'success' | 'warning';
}

const logs = ref<Log[]>([]);

// 添加日志
const addLog = (message: string, type: 'info' | 'error' | 'success' | 'warning' = 'info') => {
  logs.value.unshift({
    time: new Date(),
    message,
    type
  });
  
  // 限制日志数量
  if (logs.value.length > 100) {
    logs.value = logs.value.slice(0, 100);
  }
};

// 格式化时间
const formatTime = (date: Date) => {
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  });
};

// WebSocket 处理
const handleWebSocketMessage = (data: any) => {
  try {
    const messageStr = typeof data === 'string' 
      ? data 
      : JSON.stringify(data, null, 2);
    
    addLog(`收到消息: ${messageStr}`, 'success');
  } catch (error) {
    addLog(`处理消息出错: ${error}`, 'error');
  }
};

// 添加网络检测
const isOnline = ref(navigator.onLine);
const networkStatus = computed(() => {
  return isOnline.value ? '在线' : '离线';
});

// 添加浏览器信息
const browserInfo = computed(() => {
  return {
    userAgent: navigator.userAgent,
    language: navigator.language,
    online: navigator.onLine,
    platform: navigator.platform,
    cookies: navigator.cookieEnabled
  };
});

// 自定义getToken函数，允许使用手动输入的token
const getCustomToken = () => {
  // 如果手动输入了token，则优先使用
  if (manualToken.value.trim()) {
    return manualToken.value.trim();
  }
  
  // 否则尝试从存储中获取
  let token = localStorage.getItem('accessToken') || 
              localStorage.getItem('auth_token') ||
              localStorage.getItem('token');
  
  if (!token) {
    token = sessionStorage.getItem('accessToken') || 
            sessionStorage.getItem('auth_token') ||
            sessionStorage.getItem('token');
  }
  
  return token;
};

// 从存储中复制token到输入框
const copyTokenFromStorage = () => {
  const token = localStorage.getItem('accessToken') || 
                localStorage.getItem('auth_token') || 
                sessionStorage.getItem('accessToken') || 
                sessionStorage.getItem('auth_token');
  
  if (token) {
    manualToken.value = token;
    addLog('已从存储中复制token', 'success');
  } else {
    addLog('存储中未找到token', 'warning');
  }
};

// 初始化 WebSocket
const { 
  status,
  connect: wsConnect, 
  disconnect: wsDisconnect, 
  send: wsSend,
  sendPing: wsSendPing,
  isConnected: wsConnected,
  lastMessage,
  errorMessage,
  reset: wsReset
} = useWebSocket(wsUrl.value, handleWebSocketMessage, { 
  useAuth: useAuth.value,
  customToken: getCustomToken
});

// 监听useAuth变化
watch(useAuth, (newValue) => {
  if (isConnected.value) {
    addLog('需要先断开连接才能更改认证选项', 'warning');
    return;
  }
  
  addLog(`认证选项已更改: ${newValue ? '使用认证' : '不使用认证'}`, 'info');
});

// 监听wsUrl变化
watch(wsUrl, (newValue) => {
  if (isConnected.value) {
    addLog('需要先断开连接才能更改URL', 'warning');
    return;
  }
  
  addLog(`WebSocket URL已更改: ${newValue}`, 'info');
});

// 计算属性
const isConnected = computed(() => wsConnected.value);
const statusText = computed(() => {
  switch (status.value) {
    case 'connected': return '已连接';
    case 'connecting': return '连接中...';
    case 'disconnected': return '未连接';
    case 'error': return `连接错误: ${errorMessage.value || '未知错误'}`;
    default: return '未知状态';
  }
});

// 调试信息
const debugInfo = computed(() => {
  return {
    url: wsUrl.value,
    status: status.value,
    isConnected: wsConnected.value,
    lastMessage: lastMessage.value,
    errorMessage: errorMessage.value,
    userAgent: navigator.userAgent,
    protocol: window.location.protocol,
    host: window.location.host,
    online: navigator.onLine,
    token: getCustomToken() ? '存在' : '不存在',
    manualTokenLength: manualToken.value ? manualToken.value.length : 0
  };
});

// 连接 WebSocket
const connect = () => {
  addLog('尝试连接 WebSocket...', 'info');
  addLog(`连接地址: ${wsUrl.value}`, 'info');
  
  try {
    // 检查URL格式
    if (!wsUrl.value.startsWith('ws://') && !wsUrl.value.startsWith('wss://') && 
        !wsUrl.value.startsWith('/')) {
      addLog('错误: WebSocket URL必须以ws://、wss://开头或是以/开头的相对路径', 'error');
      return;
    }
    
    // 尝试获取token
    const token = getCustomToken();
    if (!token) {
      addLog('警告: 未找到认证Token，连接可能会被拒绝', 'warning');
    } else {
      // 掩码显示token
      const maskToken = token.length > 15 
        ? token.substring(0, 10) + '...' + token.substring(token.length - 5)
        : token;
      addLog(`已找到认证Token: ${maskToken}`, 'info');
    }
    
    // 添加浏览器信息
    addLog(`浏览器: ${navigator.userAgent}`, 'info');
    addLog(`网络状态: ${networkStatus}`, 'info');
    
    // 记录认证状态
    addLog(`认证状态: ${useAuth.value ? '使用认证' : '不使用认证'}`, 'info');
    
    // 连接
    wsConnect();
    
    // 设置监听器，每30秒发送一次ping以保持连接
    if (pingInterval) clearInterval(pingInterval);
    pingInterval = setInterval(() => {
      if (isConnected.value) {
        sendPing();
      }
    }, 30000);
    
  } catch (error) {
    addLog(`连接出错: ${error instanceof Error ? error.message : String(error)}`, 'error');
  }
};

// 断开 WebSocket
const disconnect = () => {
  addLog('断开 WebSocket 连接', 'warning');
  if (pingInterval) clearInterval(pingInterval);
  wsDisconnect();
};

// 重置连接
const resetConnection = () => {
  if (pingInterval) clearInterval(pingInterval);
  wsReset();
  addLog('WebSocket连接已重置', 'warning');
};

// 发送消息
const sendMessage = () => {
  if (!testMessage.value.trim()) {
    addLog('消息不能为空', 'error');
    return;
  }
  
  try {
    // 尝试解析为 JSON
    let message;
    try {
      message = JSON.parse(testMessage.value);
      addLog(`发送 JSON 消息: ${testMessage.value}`, 'info');
    } catch {
      message = testMessage.value;
      addLog(`发送文本消息: ${testMessage.value}`, 'info');
    }
    
    const success = wsSend(message);
    if (success) {
      addLog('消息发送成功', 'success');
    } else {
      addLog('消息发送失败', 'error');
    }
  } catch (error) {
    addLog(`发送消息出错: ${error}`, 'error');
  }
};

// Ping
const sendPing = () => {
  const success = wsSendPing();
  if (success) {
    addLog('PING 发送成功', 'success');
  } else {
    addLog('PING 发送失败', 'error');
  }
};

// 消息模板
const messageTemplates = {
  text: 'Hello WebSocket!',
  ping: 'PING',
  message: JSON.stringify({
    type: 'message',
    data: {
      conversationId: 1,
      content: '这是一条测试消息',
      messageType: 'TEXT'
    }
  }, null, 2),
  typing: JSON.stringify({
    type: 'typing',
    data: {
      conversationId: 1,
      isTyping: true
    }
  }, null, 2)
};

// 使用消息模板
const useMessageTemplate = (templateName: keyof typeof messageTemplates) => {
  testMessage.value = messageTemplates[templateName];
};

// 使用快速连接URL
const useQuickConnectUrl = (url: string) => {
  if (isConnected.value) {
    addLog('请先断开连接再切换URL', 'warning');
    return;
  }
  wsUrl.value = url;
  addLog(`已切换到URL: ${url}`, 'info');
};

// WebSocket协议选择
const wsProtocol = ref<'native' | 'stomp'>('native');

// 监听协议变化
watch(wsProtocol, (newProtocol) => {
  if (isConnected.value) return;
  
  if (newProtocol === 'native') {
    wsUrl.value = 'ws://localhost:8080/ws-native';
  } else {
    wsUrl.value = 'ws://localhost:8080/ws';
  }
  
  addLog(`切换到${newProtocol === 'native' ? '原生WebSocket' : 'STOMP协议'}`, 'info');
});

// 监听状态变化
watch(status, (newStatus: WebSocketStatus, oldStatus: WebSocketStatus) => {
  if (newStatus !== oldStatus) {
    addLog(`WebSocket 状态变化: ${oldStatus} -> ${newStatus}`, 'info');
    
    if (newStatus === 'connected') {
      addLog('WebSocket 连接成功', 'success');
    } else if (newStatus === 'error') {
      addLog(`WebSocket 连接错误: ${errorMessage.value || '未知错误'}`, 'error');
    }
  }
});

// 监听网络状态变化
const handleOnlineChange = () => {
  isOnline.value = navigator.onLine;
  if (navigator.onLine) {
    addLog('网络已连接', 'success');
  } else {
    addLog('网络已断开', 'error');
  }
};

// 更改端口
const changePort = (port: number) => {
  if (isConnected.value) return;
  
  // 解析当前URL
  try {
    const url = new URL(wsUrl.value);
    url.port = port.toString();
    wsUrl.value = url.toString();
    addLog(`更改端口为: ${port}`, 'info');
  } catch (error) {
    addLog(`无法更改端口: ${error instanceof Error ? error.message : String(error)}`, 'error');
  }
};

// 更改路径
const changePath = (path: string) => {
  if (isConnected.value) return;
  
  // 解析当前URL
  try {
    const url = new URL(wsUrl.value);
    url.pathname = path;
    wsUrl.value = url.toString();
    addLog(`更改路径为: ${path}`, 'info');
  } catch (error) {
    addLog(`无法更改路径: ${error instanceof Error ? error.message : String(error)}`, 'error');
  }
};

// 清除日志
const clearLogs = () => {
  logs.value = [];
  addLog('日志已清除', 'info');
};

// 检查网络连通性
const checkNetworkConnectivity = () => {
  addLog('正在检查网络连通性...', 'info');
  
  // 检查基本网络连接
  if (!navigator.onLine) {
    addLog('系统报告网络离线', 'error');
    return;
  }
  
  // 尝试ping后端服务器
  const start = Date.now();
  fetch('/', { method: 'HEAD', cache: 'no-store' })
    .then(response => {
      const time = Date.now() - start;
      addLog(`服务器响应正常，响应时间: ${time}ms`, 'success');
    })
    .catch(error => {
      addLog(`无法连接服务器: ${error}`, 'error');
    });
};

// Ping间隔
let pingInterval: number | null = null;

// 生命周期钩子
onMounted(() => {
  addLog('组件已挂载，准备就绪', 'info');
  
  // 监听网络状态
  window.addEventListener('online', handleOnlineChange);
  window.addEventListener('offline', handleOnlineChange);
});

onUnmounted(() => {
  // 确保在组件卸载时断开连接
  if (pingInterval) clearInterval(pingInterval);
  wsDisconnect();
  
  // 移除网络状态监听
  window.removeEventListener('online', handleOnlineChange);
  window.removeEventListener('offline', handleOnlineChange);
});
</script>

<style scoped>
.websocket-test {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
  font-family: Arial, sans-serif;
}

h2 {
  margin-bottom: 20px;
  color: #333;
}

h3 {
  margin: 15px 0 10px;
  color: #444;
}

.status-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #f5f5f5;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.status-indicator {
  display: flex;
  align-items: center;
}

.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #f44336;
  margin-right: 10px;
}

.status-indicator.connected .status-dot {
  background-color: #4caf50;
}

.status-text {
  font-weight: bold;
}

.network-indicator {
  display: flex;
  align-items: center;
  margin: 0 15px;
}

.network-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #f44336;
  margin-right: 10px;
}

.network-dot.online {
  background-color: #4caf50;
}

.network-text {
  font-weight: bold;
}

.actions {
  display: flex;
  gap: 10px;
}

button {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  background-color: #2196f3;
  color: white;
  cursor: pointer;
  font-weight: bold;
}

button:hover {
  background-color: #1976d2;
}

button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.test-message-box {
  background-color: #f5f5f5;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.input-group {
  display: flex;
  gap: 10px;
}

input {
  flex: 1;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.logs {
  background-color: #f5f5f5;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.logs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.log-container {
  background-color: #333;
  color: #fff;
  padding: 10px;
  border-radius: 4px;
  height: 300px;
  overflow-y: auto;
  font-family: monospace;
}

.log-item {
  padding: 5px 0;
  border-bottom: 1px solid #444;
  display: flex;
}

.log-time {
  color: #888;
  margin-right: 10px;
  flex-shrink: 0;
}

.log-content {
  word-break: break-all;
}

.log-item.info .log-content {
  color: #2196f3;
}

.log-item.success .log-content {
  color: #4caf50;
}

.log-item.error .log-content {
  color: #f44336;
}

.log-item.warning .log-content {
  color: #ff9800;
}

.debug-info {
  background-color: #f5f5f5;
  padding: 15px;
  border-radius: 8px;
}

.debug-info pre {
  background-color: #333;
  color: #fff;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
  font-family: monospace;
}

.ws-url-input {
  flex: 1;
  margin: 0 10px;
}

.ws-url-input input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-family: monospace;
  font-size: 14px;
}

.ws-url-input input:disabled {
  background-color: #f5f5f5;
  color: #666;
}

.message-templates {
  margin-top: 15px;
  border-top: 1px solid #eee;
  padding-top: 10px;
}

.message-templates h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #666;
}

.template-btn {
  margin-right: 8px;
  margin-bottom: 8px;
  padding: 5px 10px;
  background-color: #e0e0e0;
  color: #333;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.template-btn:hover {
  background-color: #d0d0d0;
}

.template-btn:disabled {
  background-color: #f0f0f0;
  color: #999;
  cursor: not-allowed;
}

.ws-protocol-options {
  margin-top: 15px;
  margin-bottom: 15px;
}

.ws-protocol-options h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #666;
}

.protocol-selector {
  display: flex;
  gap: 20px;
}

.protocol-selector label {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}

.protocol-selector input {
  cursor: pointer;
}

.protocol-selector input:disabled {
  cursor: not-allowed;
}

.auth-options {
  margin-top: 15px;
  margin-bottom: 15px;
}

.auth-options h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #666;
}

.auth-selector {
  display: flex;
  gap: 20px;
}

.auth-selector label {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}

.auth-selector input {
  cursor: pointer;
}

.auth-selector input:disabled {
  cursor: not-allowed;
}

.token-input {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}

.token-field {
  flex: 1;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.token-field:disabled {
  background-color: #f5f5f5;
  color: #666;
}

.copy-btn {
  padding: 8px 12px;
  background-color: #4caf50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
}

.copy-btn:hover {
  background-color: #388e3c;
}

.copy-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.path-options {
  margin-top: 15px;
  margin-bottom: 15px;
}

.path-options h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #666;
}

.path-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.path-buttons button {
  padding: 5px 10px;
  background-color: #e0e0e0;
  color: #333;
  border: 1px solid #ccc;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.path-buttons button.active {
  background-color: #2196f3;
  color: white;
  border-color: #1976d2;
}

.path-buttons button:hover:not(:disabled) {
  background-color: #d0d0d0;
}

.path-buttons button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.clear-btn {
  background-color: #f44336;
  color: white;
  padding: 5px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.check-btn {
  background-color: #ff9800;
  color: white;
  padding: 5px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-left: 5px;
}

.reset-btn {
  background-color: #ff9800;
  color: white;
}

.ping-btn {
  background-color: #9c27b0;
  color: white;
  margin-left: 5px;
}

.quick-connect {
  background-color: #f5f5f5;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.quick-connect h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #666;
}

.quick-connect-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.quick-connect-buttons button {
  padding: 5px 10px;
  background-color: #e0e0e0;
  color: #333;
  border: 1px solid #ccc;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.quick-connect-buttons button.active {
  background-color: #2196f3;
  color: white;
  border-color: #1976d2;
}

.quick-connect-buttons button:hover:not(:disabled) {
  background-color: #d0d0d0;
}

.quick-connect-buttons button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style> 