import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useSharedWebSocket } from './useWebSocket';
import { updateGroup } from '@/api/group';

// 定义群组更新类型
export type GroupUpdateType = 
  | 'UPDATE' 
  | 'NEW' 
  | 'DELETE' 
  | 'MEMBER_JOIN' 
  | 'MEMBER_LEAVE' 
  | 'MEMBER_REMOVED' 
  | 'MEMBER_MUTED' 
  | 'MEMBER_UNMUTED'
  | 'ANNOUNCEMENT_NEW'
  | 'ANNOUNCEMENT_UPDATE'
  | 'ANNOUNCEMENT_DELETE'
  | 'ANNOUNCEMENT_PINNED'
  | 'ANNOUNCEMENT_UNPINNED'
  | 'GROUP_DISSOLVED';

// 群组更新事件
export interface GroupUpdateEvent {
  groupId: number;
  data: any;
  updateType: GroupUpdateType;
  timestamp: number;
}

// 创建全局事件总线
const groupUpdateListeners: ((event: GroupUpdateEvent) => void)[] = [];

// 最近的群组更新
const recentGroupUpdates = ref<GroupUpdateEvent[]>([]);

/**
 * 处理群组更新的composable
 */
export function useGroupUpdates() {
  // 使用共享WebSocket
  const { isConnected } = useSharedWebSocket(handleWebSocketMessage);
  
  // 处理WebSocket消息
  function handleWebSocketMessage(message: any) {
    if (message.type === 'GROUP_UPDATE') {
      console.log('收到群组更新消息:', message);
      
      const updateEvent: GroupUpdateEvent = message.data;
      
      // 添加到最近更新列表
      recentGroupUpdates.value.unshift(updateEvent);
      if (recentGroupUpdates.value.length > 10) {
        recentGroupUpdates.value = recentGroupUpdates.value.slice(0, 10);
      }
      
      // 通知所有监听器
      notifyListeners(updateEvent);
    }
  }
  
  // 添加监听器
  function addListener(listener: (event: GroupUpdateEvent) => void) {
    if (!groupUpdateListeners.includes(listener)) {
      groupUpdateListeners.push(listener);
    }
  }
  
  // 移除监听器
  function removeListener(listener: (event: GroupUpdateEvent) => void) {
    const index = groupUpdateListeners.indexOf(listener);
    if (index !== -1) {
      groupUpdateListeners.splice(index, 1);
    }
  }
  
  // 通知所有监听器
  function notifyListeners(event: GroupUpdateEvent) {
    groupUpdateListeners.forEach(listener => {
      try {
        listener(event);
      } catch (error) {
        console.error('群组更新监听器执行出错:', error);
      }
    });
  }
  
  // 组件卸载时移除监听器
  onBeforeUnmount(() => {
    // 清理工作，如果需要的话
  });
  
  return {
    recentGroupUpdates,
    addListener,
    removeListener,
    isConnected
  };
}

// 提供一个默认导出，方便在其他组件中使用
export default useGroupUpdates; 