import { ref, onMounted, computed } from 'vue';
import { notificationApi, type SystemNotificationResponse, type SystemNotificationDetailResponse } from '@/api/notification';
import { useSharedWebSocket } from './useWebSocket';
import { ElMessage } from 'element-plus';

// 扩展Window接口，添加通知未读数量全局变量
declare global {
    interface Window {
        notificationUnreadCount?: number;
    }
}

/**
 * 系统通知组合式函数
 * 提供获取通知列表、监听WebSocket通知等功能
 */
export function useNotifications() {
    // 通知列表
    const notifications = ref<SystemNotificationResponse[]>([]);
    // 通知加载状态
    const loading = ref(false);
    // 总通知数
    const totalCount = ref(0);
    // 当前页码
    const currentPage = ref(0);
    // 是否只看未读
    const unreadOnly = ref(false);
    // 未读通知数量
    const unreadCount = ref(0);
    // 错误信息
    const error = ref<string | null>(null);
    
    // 处理WebSocket消息的函数
    const handleWebSocketMessage = (message: any) => {
        // 处理系统通知消息
        if (message && message.type === 'SYSTEM_NOTIFICATION') {
            console.log('收到系统通知:', message.data);
            
            // 显示通知提示
            ElMessage({
                message: `新系统通知: ${message.data.title}`,
                type: 'info',
                duration: 5000,
                showClose: true,
            });
            
            // 更新未读通知数量
            unreadCount.value++;
            
            // 更新全局变量
            if (typeof window !== 'undefined') {
                window.notificationUnreadCount = (window.notificationUnreadCount || 0) + 1;
            }
            
            // 如果在通知列表页面，刷新列表
            if (notifications.value.length > 0) {
                loadNotifications();
            }
        }
    };
    
    // 连接WebSocket
    const { isConnected } = useSharedWebSocket(handleWebSocketMessage);
    
    // 计算是否有未读通知
    const hasUnreadNotifications = computed(() => unreadCount.value > 0);
    
    /**
     * 加载通知列表
     * @param page 页码
     * @param size 每页大小
     */
    async function loadNotifications(page: number = 0, size: number = 10) {
        loading.value = true;
        error.value = null;
        
        try {
            const response = await notificationApi.getNotifications(page, size, unreadOnly.value);
            
            if (response.success) {
                notifications.value = response.data.content;
                totalCount.value = response.data.totalElements;
                currentPage.value = response.data.number;
            } else {
                error.value = response.message || '获取通知列表失败';
                ElMessage.error(error.value);
            }
        } catch (err: any) {
            error.value = err.message || '获取通知列表失败';
            ElMessage.error(error.value);
        } finally {
            loading.value = false;
        }
    }
    
    /**
     * 获取通知详情
     * @param notificationId 通知ID
     * @returns 通知详情
     */
    async function getNotificationDetail(notificationId: number): Promise<SystemNotificationDetailResponse | null> {
        error.value = null;
        
        try {
            const response = await notificationApi.getNotificationDetail(notificationId);
            
            if (response.success) {
                // 更新未读通知数量
                if (!response.data.isRead) {
                    refreshUnreadCount();
                }
                
                return response.data;
            } else {
                error.value = response.message || '获取通知详情失败';
                ElMessage.error(error.value);
                return null;
            }
        } catch (err: any) {
            error.value = err.message || '获取通知详情失败';
            ElMessage.error(error.value);
            return null;
        }
    }
    
    /**
     * 标记通知为已读
     * @param notificationId 通知ID
     * @returns 是否成功
     */
    async function markAsRead(notificationId: number): Promise<boolean> {
        error.value = null;
        
        try {
            const response = await notificationApi.markAsRead(notificationId);
            
            if (response.success) {
                // 更新本地通知状态
                const notification = notifications.value.find(n => n.id === notificationId);
                if (notification && !notification.isRead) {
                    notification.isRead = true;
                    await refreshUnreadCount(); // 使用刷新方法更新本地和全局变量
                }
                
                return true;
            } else {
                error.value = response.message || '标记已读失败';
                ElMessage.error(error.value);
                return false;
            }
        } catch (err: any) {
            error.value = err.message || '标记已读失败';
            ElMessage.error(error.value);
            return false;
        }
    }
    
    /**
     * 标记所有通知为已读
     * @returns 是否成功
     */
    async function markAllAsRead(): Promise<boolean> {
        error.value = null;
        
        try {
            const response = await notificationApi.markAllAsRead();
            
            if (response.success) {
                // 更新本地通知状态
                notifications.value.forEach(notification => {
                    notification.isRead = true;
                });
                
                unreadCount.value = 0;
                
                // 更新全局变量
                if (typeof window !== 'undefined') {
                    window.notificationUnreadCount = 0;
                }
                
                ElMessage.success('已将所有通知标记为已读');
                
                return true;
            } else {
                error.value = response.message || '标记全部已读失败';
                ElMessage.error(error.value);
                return false;
            }
        } catch (err: any) {
            error.value = err.message || '标记全部已读失败';
            ElMessage.error(error.value);
            return false;
        }
    }
    
    /**
     * 刷新未读通知数量
     */
    async function refreshUnreadCount() {
        try {
            const response = await notificationApi.getUnreadCount();
            
            if (response.success) {
                unreadCount.value = response.data.unreadCount;
                
                // 更新全局变量
                if (typeof window !== 'undefined') {
                    window.notificationUnreadCount = response.data.unreadCount;
                }
            }
        } catch (err) {
            console.error('获取未读通知数量失败:', err);
        }
    }
    
    // 初始化时获取未读通知数量
    onMounted(() => {
        refreshUnreadCount();
    });
    
    // 切换只看未读状态
    function toggleUnreadOnly() {
        unreadOnly.value = !unreadOnly.value;
        loadNotifications();
    }
    
    return {
        notifications,
        loading,
        totalCount,
        currentPage,
        unreadOnly,
        unreadCount,
        error,
        hasUnreadNotifications,
        loadNotifications,
        getNotificationDetail,
        markAsRead,
        markAllAsRead,
        refreshUnreadCount,
        toggleUnreadOnly,
    };
} 