import { ref, onUnmounted } from 'vue';
import type { Message } from '@/types';

type WebSocketStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

// 默认 WebSocket URL
const DEFAULT_WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:3000/ws';

export const useWebSocket = (url: string) => {
  const socket = ref<WebSocket | null>(null);
  const status = ref<WebSocketStatus>('disconnected');
  const lastMessage = ref<any>(null);
  const messageHistory = ref<any[]>([]);
  const reconnectAttempts = ref(0);
  const maxReconnectAttempts = 5;
  const reconnectInterval = 3000;

  const connect = (): void => {
    try {
      status.value = 'connecting';
      socket.value = new WebSocket(url);

      socket.value.onopen = () => {
        status.value = 'connected';
        reconnectAttempts.value = 0;
        console.log('WebSocket 连接已建立');
      };

      socket.value.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          lastMessage.value = message;
          messageHistory.value.push(message);

          // 限制历史消息数量
          if (messageHistory.value.length > 100) {
            messageHistory.value = messageHistory.value.slice(-100);
          }
        } catch (error) {
          console.error('解析 WebSocket 消息失败:', error);
        }
      };

      socket.value.onclose = (event) => {
        status.value = 'disconnected';
        console.log('WebSocket 连接已关闭:', event.code, event.reason);

        // 自动重连
        if (reconnectAttempts.value < maxReconnectAttempts) {
          setTimeout(() => {
            reconnectAttempts.value++;
            console.log(`尝试重连 (${reconnectAttempts.value}/${maxReconnectAttempts})`);
            connect();
          }, reconnectInterval);
        }
      };

      socket.value.onerror = (error) => {
        status.value = 'error';
        console.error('WebSocket 错误:', error);
      };
    } catch (error) {
      status.value = 'error';
      console.error('创建 WebSocket 连接失败:', error);
    }
  };

  const disconnect = (): void => {
    if (socket.value) {
      socket.value.close();
      socket.value = null;
    }
    status.value = 'disconnected';
  };

  const send = (data: any): boolean => {
    if (socket.value && status.value === 'connected') {
      try {
        const message = typeof data === 'string' ? data : JSON.stringify(data);
        socket.value.send(message);
        return true;
      } catch (error) {
        console.error('发送 WebSocket 消息失败:', error);
        return false;
      }
    }
    console.warn('WebSocket 未连接，无法发送消息');
    return false;
  };

  const sendMessage = (message: Omit<Message, 'id' | 'timestamp' | 'status'>): boolean => {
    const fullMessage: Message = {
      ...message,
      id: Date.now().toString(),
      timestamp: new Date(),
      status: 'sent',
    };

    return send({
      type: 'message',
      data: fullMessage,
    });
  };

  const sendTyping = (receiverId: string, isTyping: boolean): boolean => {
    return send({
      type: 'typing',
      data: {
        receiverId,
        isTyping,
      },
    });
  };

  const sendStatusUpdate = (status: 'online' | 'away' | 'busy'): boolean => {
    return send({
      type: 'status_update',
      data: { status },
    });
  };

  // 组件卸载时自动断开连接
  onUnmounted(() => {
    disconnect();
  });

  return {
    status,
    lastMessage,
    messageHistory,
    reconnectAttempts,
    connect,
    disconnect,
    send,
    sendMessage,
    sendTyping,
    sendStatusUpdate,
  };
};

// 创建默认 WebSocket 实例
export const defaultWebSocket = () => useWebSocket(DEFAULT_WS_URL);

// 导出默认 URL 供其他组件使用
export { DEFAULT_WS_URL };
