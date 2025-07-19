import { ref, onMounted, nextTick, computed, onBeforeUnmount } from 'vue';
import type { Message } from '@/types';
import { useAuth } from './useAuth';

type WebSocketStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

// 默认 WebSocket URL - 使用相对路径，依靠Vite的代理功能
export const DEFAULT_WS_URL = '/ws-native';

// 修改重连配置，增加更强大的自动重连机制
const DEFAULT_RECONNECT_SETTINGS = {
  maxAttempts: 30,            // 增加最大重试次数
  baseDelay: 1000,           // 基础延迟(ms)
  maxDelay: 30000,           // 最大延迟(ms)
  useExponentialBackoff: true, // 使用指数退避策略
  resetReconnectAttemptsAfter: 60000, // 成功连接后多久重置重连尝试次数(ms)
  enablePing: true,           // 启用ping
  pingInterval: 20000,        // 减少ping间隔，更频繁地检测连接状态
  pingTimeoutInterval: 10000, // ping超时间隔(ms)
  reconnectOnVisibilityChange: true, // 页面从隐藏变为可见时尝试重连
  reconnectOnNetworkChange: true,    // 网络状态变化时尝试重连
  persistentReconnect: true,         // 持续尝试重连，即使达到最大尝试次数
};

// 创建共享的WebSocket实例
let sharedSocket: WebSocket | null = null;
let sharedStatus = ref<WebSocketStatus>('disconnected');
let sharedLastMessage = ref<any>(null);
let sharedMessageHistory = ref<any[]>([]);
let sharedReconnectAttempts = ref(0);
let sharedErrorMessage = ref<string>('');
let messageHandlers: ((message: any) => void)[] = [];
let isInitialized = false;
let sharedWsUrl = '';

// 跟踪定时器
let pingTimer: number | null = null;
let pingTimeoutTimer: number | null = null;
let reconnectTimer: number | null = null;
let connectionCheckTimer: number | null = null;
let lastMessageTimestamp = Date.now();

// 共享的WebSocket方法
const clearAllTimers = () => {
  if (pingTimer) {
    clearInterval(pingTimer);
    pingTimer = null;
  }
  
  if (pingTimeoutTimer) {
    clearTimeout(pingTimeoutTimer);
    pingTimeoutTimer = null;
  }
  
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  
  if (connectionCheckTimer) {
    clearInterval(connectionCheckTimer);
    connectionCheckTimer = null;
  }
};

// 增加WebSocket连接状态日志记录
const logConnectionStatus = (status: WebSocketStatus, details?: string) => {
  const timestamp = new Date().toISOString();
  const message = details ? `WebSocket状态: ${status} - ${details}` : `WebSocket状态: ${status}`;
  
  console.log(`[${timestamp}] ${message}`);
  
  // 保存到日志历史
  const logEntry = {
    timestamp,
    status,
    details: details || '',
  };
  
  // 可以选择将日志保存到本地存储，方便调试
  try {
    const wsLogs = JSON.parse(localStorage.getItem('ws_connection_logs') || '[]');
    wsLogs.push(logEntry);
    
    // 只保留最近的100条日志
    if (wsLogs.length > 100) {
      wsLogs.splice(0, wsLogs.length - 100);
    }
    
    localStorage.setItem('ws_connection_logs', JSON.stringify(wsLogs));
  } catch (e) {
    console.error('保存WebSocket日志失败:', e);
  }
};

// 修改initSharedWebSocket函数，增加错误处理和日志记录
const initSharedWebSocket = (url: string, token: string | null) => {
  if (sharedSocket && sharedSocket.readyState !== WebSocket.CLOSED) {
    logConnectionStatus('disconnected', '关闭旧连接以建立新连接');
    sharedSocket.close();
  }
  
  clearAllTimers();
  
  sharedStatus.value = 'connecting';
  sharedErrorMessage.value = '';
  logConnectionStatus('connecting', `正在连接到 ${url}`);
      
      // 构建WebSocket URL
      let wsUrl = url;
      
  if (wsUrl.startsWith('/')) {
    const isSecure = window.location.protocol === 'https:';
    const protocol = isSecure ? 'wss://' : 'ws://';
    const host = window.location.host;
    wsUrl = protocol + host + wsUrl;
  } else if (!wsUrl.startsWith('ws://') && !wsUrl.startsWith('wss://')) {
    const isSecure = window.location.protocol === 'https:';
    const protocol = isSecure ? 'wss://' : 'ws://';
    const host = window.location.host;
    wsUrl = protocol + host + (wsUrl.startsWith('/') ? wsUrl : '/' + wsUrl);
  }
  
  // 添加token
        if (token) {
          wsUrl += (wsUrl.includes('?') ? '&' : '?') + `token=${token}`;
        }
  
  sharedWsUrl = wsUrl;
  console.log('共享WebSocket: 正在连接', wsUrl.indexOf('token=') > -1 
    ? wsUrl.substring(0, wsUrl.indexOf('token=') + 10) + '...' 
    : wsUrl);
  
  try {
    // 创建WebSocket连接
    sharedSocket = new WebSocket(wsUrl);
    
    sharedSocket.onopen = () => {
      sharedStatus.value = 'connected';
      lastMessageTimestamp = Date.now();
      logConnectionStatus('connected', '连接已成功建立');
      
      // 重置重连尝试次数
      setTimeout(() => {
        sharedReconnectAttempts.value = 0;
      }, DEFAULT_RECONNECT_SETTINGS.resetReconnectAttemptsAfter);
      
      sharedErrorMessage.value = '';
      
      // 设置ping保持连接
      setupPingKeepAlive();
      
      // 设置连接检查
      setupConnectionCheck();
    };
    
    sharedSocket.onmessage = (event: MessageEvent) => {
      lastMessageTimestamp = Date.now();
      
      try {
        let parsedMessage;
        try {
          parsedMessage = JSON.parse(event.data);
        } catch (parseError) {
          parsedMessage = { type: 'text', content: event.data };
        }
        
        // 如果是PONG消息，不需要特殊处理
        if (parsedMessage.type === 'PONG' || parsedMessage.type === 'pong') {
          console.debug('共享WebSocket: 收到PONG响应');
          
          if (pingTimeoutTimer) {
            clearTimeout(pingTimeoutTimer);
            pingTimeoutTimer = null;
          }
          
          return;
        }
        
        // 不记录过于频繁的消息类型，避免日志过多
        if (!['TYPING', 'PING', 'PONG'].includes(parsedMessage.type)) {
          console.log('共享WebSocket: 收到消息', parsedMessage);
        }
        
        sharedLastMessage.value = parsedMessage;
        sharedMessageHistory.value.push(parsedMessage);
        
        if (sharedMessageHistory.value.length > 100) {
          sharedMessageHistory.value = sharedMessageHistory.value.slice(-100);
        }
        
        // 调用所有注册的消息处理器
        messageHandlers.forEach(handler => {
          try {
            handler(parsedMessage);
          } catch (handlerError) {
            console.error('共享WebSocket: 消息处理器执行失败:', handlerError);
          }
        });
      } catch (error) {
        console.error('共享WebSocket: 处理消息失败:', error);
      }
    };
    
    sharedSocket.onclose = (event: CloseEvent) => {
      sharedStatus.value = 'disconnected';
      
      // 记录关闭原因
      let closeReason = getCloseReasonText(event.code);
      logConnectionStatus('disconnected', `连接已关闭: 代码 ${event.code} (${closeReason})${event.reason ? ', 原因: ' + event.reason : ''}`);
      
      if (event.reason) {
        sharedErrorMessage.value = `连接关闭: ${event.reason} (代码: ${event.code} - ${closeReason})`;
      } else if (event.code !== 1000) {
        sharedErrorMessage.value = `连接关闭，代码: ${event.code} - ${closeReason}`;
      }
      
      clearAllTimers();
      handleReconnect(event.code);
    };
    
    sharedSocket.onerror = (error: Event) => {
      sharedStatus.value = 'error';
      sharedErrorMessage.value = '连接错误，请检查网络或服务器状态';
      logConnectionStatus('error', '连接发生错误');
      
        try {
          const errorDetail = JSON.stringify(error, Object.getOwnPropertyNames(error));
        console.error('共享WebSocket: 错误详情:', errorDetail);
        } catch (e) {
          console.error('无法获取WebSocket错误详情');
        }
      };
    
    // 设置连接超时
    setTimeout(() => {
      if (sharedStatus.value === 'connecting' && sharedSocket) {
        logConnectionStatus('error', '连接超时');
        sharedErrorMessage.value = '连接超时，请检查服务器状态';
        sharedSocket.close();
        sharedStatus.value = 'error';
        handleReconnect();
      }
    }, 10000);
  } catch (error) {
    sharedStatus.value = 'error';
    const errorMessage = error instanceof Error ? error.message : '未知错误';
    sharedErrorMessage.value = `创建连接失败: ${errorMessage}`;
    logConnectionStatus('error', `创建连接失败: ${errorMessage}`);
    handleReconnect();
  }
  
  isInitialized = true;
};

// 修改handleReconnect函数，增加持续重连机制
const handleReconnect = (closeCode?: number) => {
  if (closeCode === 1000) {
    console.log('共享WebSocket: 连接正常关闭，不重连');
    return;
  }
  
  if (!navigator.onLine) {
    console.log('共享WebSocket: 网络离线，等待网络恢复后重连');
    return;
  }
  
  // 检查重连次数是否超限
  if (sharedReconnectAttempts.value >= DEFAULT_RECONNECT_SETTINGS.maxAttempts) {
    if (DEFAULT_RECONNECT_SETTINGS.persistentReconnect) {
      console.warn(`共享WebSocket: 已达到最大重连次数 (${DEFAULT_RECONNECT_SETTINGS.maxAttempts})，但将继续尝试重连`);
      // 重置重连尝试次数，继续尝试
      sharedReconnectAttempts.value = Math.floor(DEFAULT_RECONNECT_SETTINGS.maxAttempts / 2);
    } else {
      console.warn(`共享WebSocket: 已达到最大重连次数 (${DEFAULT_RECONNECT_SETTINGS.maxAttempts})，停止重连`);
      return;
    }
  }
  
  let delay = DEFAULT_RECONNECT_SETTINGS.baseDelay;
  
  if (DEFAULT_RECONNECT_SETTINGS.useExponentialBackoff) {
    delay = Math.min(
      DEFAULT_RECONNECT_SETTINGS.baseDelay * Math.pow(1.5, sharedReconnectAttempts.value),
      DEFAULT_RECONNECT_SETTINGS.maxDelay
    );
  }
  
  console.log(`共享WebSocket: 计划在 ${delay}ms 后进行第 ${sharedReconnectAttempts.value + 1} 次重连`);
  
  reconnectTimer = window.setTimeout(() => {
    sharedReconnectAttempts.value++;
    console.log(`共享WebSocket: 正在进行第 ${sharedReconnectAttempts.value}/${DEFAULT_RECONNECT_SETTINGS.maxAttempts} 次重连`);
    
    // 使用现有URL重连
    if (sharedWsUrl) {
      if (sharedSocket && sharedSocket.readyState !== WebSocket.CLOSED) {
        sharedSocket.close();
      }
      
      sharedSocket = new WebSocket(sharedWsUrl);
      setupSharedSocketHandlers();
    }
  }, delay);
};

// 设置共享Socket的事件处理器
const setupSharedSocketHandlers = () => {
  if (!sharedSocket) return;
  
  sharedSocket.onopen = () => {
    sharedStatus.value = 'connected';
    lastMessageTimestamp = Date.now();
    console.log('共享WebSocket: 连接已建立');
    
    setTimeout(() => {
      sharedReconnectAttempts.value = 0;
    }, DEFAULT_RECONNECT_SETTINGS.resetReconnectAttemptsAfter);
    
    sharedErrorMessage.value = '';
    setupPingKeepAlive();
    setupConnectionCheck();
  };
  
  sharedSocket.onmessage = (event: MessageEvent) => {
    lastMessageTimestamp = Date.now();
    
    try {
      let parsedMessage;
      try {
        parsedMessage = JSON.parse(event.data);
      } catch (parseError) {
        parsedMessage = { type: 'text', content: event.data };
      }
      
      if (parsedMessage.type === 'PONG' || parsedMessage.type === 'pong') {
        console.debug('共享WebSocket: 收到PONG响应');
        
        if (pingTimeoutTimer) {
          clearTimeout(pingTimeoutTimer);
          pingTimeoutTimer = null;
        }
        
        return;
      }
      
      console.log('共享WebSocket: 收到消息', parsedMessage);
      sharedLastMessage.value = parsedMessage;
      sharedMessageHistory.value.push(parsedMessage);
      
      if (sharedMessageHistory.value.length > 100) {
        sharedMessageHistory.value = sharedMessageHistory.value.slice(-100);
      }
      
      messageHandlers.forEach(handler => {
        try {
          handler(parsedMessage);
        } catch (handlerError) {
          console.error('共享WebSocket: 消息处理器执行失败:', handlerError);
        }
      });
    } catch (error) {
      console.error('共享WebSocket: 处理消息失败:', error);
    }
  };
  
  sharedSocket.onclose = (event: CloseEvent) => {
    sharedStatus.value = 'disconnected';
    console.log('共享WebSocket: 连接已关闭', event.code, event.reason);
    
    let closeReason = getCloseReasonText(event.code);
    
    if (event.reason) {
      sharedErrorMessage.value = `连接关闭: ${event.reason} (代码: ${event.code} - ${closeReason})`;
    } else if (event.code !== 1000) {
      sharedErrorMessage.value = `连接关闭，代码: ${event.code} - ${closeReason}`;
    }
    
    clearAllTimers();
    handleReconnect(event.code);
  };
  
  sharedSocket.onerror = (error: Event) => {
    sharedStatus.value = 'error';
    sharedErrorMessage.value = '连接错误，请检查网络或服务器状态';
    console.error('共享WebSocket: 错误', error);
    
    try {
      const errorDetail = JSON.stringify(error, Object.getOwnPropertyNames(error));
      console.error('共享WebSocket: 错误详情:', errorDetail);
    } catch (e) {
      console.error('无法获取WebSocket错误详情');
    }
  };
};

// 设置Ping保持连接
const setupPingKeepAlive = () => {
  if (!DEFAULT_RECONNECT_SETTINGS.enablePing || !sharedSocket || sharedStatus.value !== 'connected') {
    return;
  }
  
  if (pingTimer) {
    clearInterval(pingTimer);
  }
  
  pingTimer = window.setInterval(() => {
    if (sharedSocket && sharedSocket.readyState === WebSocket.OPEN) {
      console.debug('共享WebSocket: 发送PING...');
      sendShared({
        type: "PING",
        timestamp: Date.now()
      });
      
      if (pingTimeoutTimer) {
        clearTimeout(pingTimeoutTimer);
      }
      
      pingTimeoutTimer = window.setTimeout(() => {
        console.warn('共享WebSocket: PING超时，重新连接');
        if (sharedSocket) {
          sharedSocket.close();
        }
      }, DEFAULT_RECONNECT_SETTINGS.pingTimeoutInterval);
    }
  }, DEFAULT_RECONNECT_SETTINGS.pingInterval);
};

// 设置连接检查
const setupConnectionCheck = () => {
  if (connectionCheckTimer) {
    clearInterval(connectionCheckTimer);
  }
  
  connectionCheckTimer = window.setInterval(() => {
    if (!sharedSocket || sharedStatus.value !== 'connected') {
      return;
    }
    
    const now = Date.now();
    const timeSinceLastMessage = now - lastMessageTimestamp;
    
    if (timeSinceLastMessage > 120000 && DEFAULT_RECONNECT_SETTINGS.enablePing) {
      console.warn(`共享WebSocket: 已有 ${Math.floor(timeSinceLastMessage / 1000)} 秒未收到消息，发送PING检查连接`);
      
      sendShared({
        type: "PING",
        timestamp: now
      });
      
      setTimeout(() => {
        if (Date.now() - lastMessageTimestamp >= timeSinceLastMessage) {
          console.error('共享WebSocket: PING检查未收到响应，判定连接已断开，重连');
          
          if (sharedSocket) {
            sharedSocket.close();
          }
        }
      }, 10000);
    }
  }, 30000);
};

// 返回关闭代码的文本描述
const getCloseReasonText = (code: number): string => {
  switch (code) {
    case 1000: return '正常关闭';
    case 1001: return '服务端关闭';
    case 1002: return '协议错误';
    case 1003: return '不支持的数据类型';
    case 1005: return '未提供关闭代码';
    case 1006: return '异常关闭';
    case 1007: return '数据格式不一致';
    case 1008: return '违反策略';
    case 1009: return '消息过大';
    case 1010: return '客户端请求扩展未满足';
    case 1011: return '服务器遇到意外情况';
    case 1012: return '服务重启';
    case 1013: return '服务端临时错误';
    case 1014: return '网关或代理错误';
    case 1015: return 'TLS握手失败';
    default: return '未知关闭代码';
  }
};

// 发送消息
const sendShared = (data: any): boolean => {
  if (sharedSocket && sharedSocket.readyState === WebSocket.OPEN) {
      try {
        const message = typeof data === 'string' ? data : JSON.stringify(data);
      sharedSocket.send(message);
        return true;
      } catch (error) {
      console.error('共享WebSocket: 发送消息失败:', error);
      return false;
    }
  } else if (sharedSocket && sharedSocket.readyState === WebSocket.CONNECTING) {
    console.warn('共享WebSocket: 正在连接中，无法发送消息');
        return false;
  } else {
    console.warn('共享WebSocket: 未连接，无法发送消息');
    
    if (sharedStatus.value !== 'connecting') {
      console.log('共享WebSocket: 尝试重新连接...');
      if (sharedWsUrl) {
        if (sharedSocket && sharedSocket.readyState !== WebSocket.CLOSED) {
          sharedSocket.close();
        }
        
        sharedSocket = new WebSocket(sharedWsUrl);
        setupSharedSocketHandlers();
      }
    }
    
    return false;
  }
};

// 断开连接
const disconnectShared = (): void => {
  if (sharedSocket) {
    sharedSocket.close();
    sharedSocket = null;
  }
  
  clearAllTimers();
  sharedStatus.value = 'disconnected';
  sharedErrorMessage.value = '';
};

// 注册在线状态变化监听
window.addEventListener('online', () => {
  console.log('共享WebSocket: 网络已恢复，尝试重新连接');
  if (sharedStatus.value !== 'connected' && sharedStatus.value !== 'connecting' && DEFAULT_RECONNECT_SETTINGS.reconnectOnNetworkChange) {
    if (sharedWsUrl) {
      if (sharedSocket && sharedSocket.readyState !== WebSocket.CLOSED) {
        sharedSocket.close();
      }
      
      sharedSocket = new WebSocket(sharedWsUrl);
      setupSharedSocketHandlers();
    }
  }
});

// 注册页面可见性变化监听
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible' && DEFAULT_RECONNECT_SETTINGS.reconnectOnVisibilityChange) {
    console.log('共享WebSocket: 页面变为可见，检查连接状态');
    
    if (sharedSocket && sharedSocket.readyState === WebSocket.OPEN) {
      console.log('共享WebSocket: 连接正常，发送PING检查');
      sendShared({
        type: "PING",
        timestamp: Date.now()
      });
    } else if (sharedStatus.value !== 'connecting') {
      console.log('共享WebSocket: 连接已断开，尝试重新连接');
      if (sharedWsUrl) {
        if (sharedSocket && sharedSocket.readyState !== WebSocket.CLOSED) {
          sharedSocket.close();
        }
        
        sharedSocket = new WebSocket(sharedWsUrl);
        setupSharedSocketHandlers();
      }
    }
  }
});

// 使用共享的WebSocket连接
export const useSharedWebSocket = (messageHandler?: (message: any) => void) => {
  const { getToken } = useAuth();
  
  // 初始化共享WebSocket（如果尚未初始化）
  const initializeIfNeeded = () => {
    if (!isInitialized) {
      const token = getToken();
      initSharedWebSocket(DEFAULT_WS_URL, token);
    }
  };
  
  // 注册消息处理器
  const registerHandler = (handler?: (message: any) => void) => {
    if (handler && !messageHandlers.includes(handler)) {
      messageHandlers.push(handler);
    }
  };
  
  // 移除消息处理器
  const unregisterHandler = (handler?: (message: any) => void) => {
    if (handler) {
      const index = messageHandlers.indexOf(handler);
      if (index !== -1) {
        messageHandlers.splice(index, 1);
      }
    }
  };
  
  // 组件挂载时自动初始化和注册
  onMounted(() => {
    // 检查是否已登录，如果已登录则立即连接
    const token = getToken();
    if (token) {
      console.log('检测到用户已登录，立即初始化WebSocket连接');
      initializeIfNeeded();
    } else {
      console.log('用户未登录，WebSocket连接将在登录后初始化');
    }
    
    registerHandler(messageHandler);
  });
  
  // 组件卸载时移除处理器
  onBeforeUnmount(() => {
    unregisterHandler(messageHandler);
  });

  return {
    status: sharedStatus,
    lastMessage: sharedLastMessage,
    messageHistory: sharedMessageHistory,
    errorMessage: sharedErrorMessage,
    isConnected: computed(() => sharedStatus.value === 'connected'),
    connect: initializeIfNeeded,
    disconnect: disconnectShared,
    send: sendShared
  };
};

// 保留原来的useWebSocket函数，但内部使用共享连接
export const useWebSocket = (url: string, messageHandler?: (message: any) => void, options?: { 
  useAuth?: boolean, 
  customToken?: string | (() => string | null),
  reconnectSettings?: Partial<typeof DEFAULT_RECONNECT_SETTINGS>
}) => {
  // 直接使用共享的WebSocket连接
  return useSharedWebSocket(messageHandler);
};

// 默认的WebSocket连接
export const defaultWebSocket = () => useSharedWebSocket();

// 重新连接到新URL
export const reconnectWithNewUrl = (newUrl: string): void => {
  const { getToken } = useAuth();
  const token = getToken();
  
  // 重新初始化共享WebSocket
  initSharedWebSocket(newUrl, token);
};
