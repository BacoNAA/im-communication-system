import { ref, reactive } from 'vue';

export type MessageType = 'success' | 'error' | 'warning' | 'info';

export interface Message {
  id: string;
  type: MessageType;
  content: string;
  duration?: number;
  closable?: boolean;
}

const messages = ref<Message[]>([]);
let messageIdCounter = 0;

export function useMessage() {
  // 显示消息
  const showMessage = (
    content: string,
    type: MessageType = 'info',
    duration: number = 3000,
    closable: boolean = true
  ): string => {
    const id = `message_${++messageIdCounter}_${Date.now()}`;
    
    const message: Message = {
      id,
      type,
      content,
      duration,
      closable
    };
    
    messages.value.push(message);
    
    // 自动关闭消息
    if (duration > 0) {
      setTimeout(() => {
        removeMessage(id);
      }, duration);
    }
    
    return id;
  };
  
  // 显示成功消息
  const showSuccess = (content: string, duration?: number): string => {
    return showMessage(content, 'success', duration);
  };
  
  // 显示错误消息
  const showError = (content: string, duration?: number): string => {
    return showMessage(content, 'error', duration || 5000); // 错误消息默认显示更长时间
  };
  
  // 显示警告消息
  const showWarning = (content: string, duration?: number): string => {
    return showMessage(content, 'warning', duration);
  };
  
  // 显示信息消息
  const showInfo = (content: string, duration?: number): string => {
    return showMessage(content, 'info', duration);
  };
  
  // 移除消息
  const removeMessage = (id: string): void => {
    const index = messages.value.findIndex(msg => msg.id === id);
    if (index > -1) {
      messages.value.splice(index, 1);
    }
  };
  
  // 清除所有消息
  const clearAllMessages = (): void => {
    messages.value = [];
  };
  
  // 获取消息图标
  const getMessageIcon = (type: MessageType): string => {
    switch (type) {
      case 'success':
        return '✅';
      case 'error':
        return '❌';
      case 'warning':
        return '⚠️';
      case 'info':
      default:
        return 'ℹ️';
    }
  };
  
  // 获取消息样式类
  const getMessageClass = (type: MessageType): string => {
    return `message-${type}`;
  };
  
  return {
    messages,
    showMessage,
    showSuccess,
    showError,
    showWarning,
    showInfo,
    removeMessage,
    clearAllMessages,
    getMessageIcon,
    getMessageClass
  };
}

// 全局消息实例
const globalMessage = useMessage();

// 导出全局方法，方便在组件外使用
export const showMessage = globalMessage.showMessage;
export const showSuccess = globalMessage.showSuccess;
export const showError = globalMessage.showError;
export const showWarning = globalMessage.showWarning;
export const showInfo = globalMessage.showInfo;