<template>
  <div class="message-wrapper" 
       :class="{ 'message-self': message.isSelf, 'message-other': !message.isSelf, 'highlight-message': isHighlighted }"
       :data-message-id="message.id"
       @contextmenu.prevent="handleContextMenu">
    <!-- 消息容器 -->
    <div class="message-container">
      <!-- 左侧头像 - 仅对方消息显示 -->
      <template v-if="!message.isSelf">
        <div class="avatar-container left">
          <img class="avatar" :src="userAvatar" :alt="message.senderName || '用户'" />
        </div>
        
        <!-- 消息内容 -->
        <div class="message-content-wrapper">
          <!-- 发送者名称 - 对方消息总是显示 -->
          <div class="sender-name" v-if="!message.isSelf">
            {{ senderDisplayName }}
          </div>
          
          <!-- 消息气泡 -->
          <div class="message-bubble other-bubble">
            <!-- 已撤回消息 -->
            <template v-if="isRecalled">
              <div class="recalled-message">
                <i class="recalled-icon">↩️</i>
                <span class="recalled-text">此消息已被撤回</span>
              </div>
            </template>
            <!-- 文本消息 -->
            <template v-else-if="isTextMessage">
              <span v-html="formatTextWithEmojis(displayContent)"></span>
              <span v-if="message.edited" class="edited-indicator">(已编辑)</span>
            </template>
            
            <!-- 图片消息 -->
            <template v-else-if="isImageMessage">
              <div v-if="message.mediaFileId" class="image-container">
                <img class="message-image" :src="getPublicMediaUrl(message.mediaFileId)" @click="previewImage(getPublicMediaUrl(message.mediaFileId))" alt="图片消息" />
              </div>
              <div v-else-if="message.content && message.content.startsWith('http')" class="image-container">
                <img class="message-image" :src="message.content" @click="previewImage(message.content)" alt="图片消息" />
              </div>
              <div v-else class="image-placeholder">
                图片消息加载中...
              </div>
            </template>
            
            <!-- 视频消息 -->
            <template v-else-if="isVideoMessage">
              <div class="video-message">
                <video v-if="message.mediaFileId" class="message-video" controls :src="getPublicMediaUrl(message.mediaFileId)"></video>
                <video v-else-if="message.content && message.content.startsWith('http')" class="message-video" controls :src="message.content"></video>
                <div v-else class="video-placeholder">视频消息加载中...</div>
              </div>
            </template>
            
            <!-- 文件消息 -->
            <template v-else-if="isFileMessage">
              <div class="file-message">
                <div class="file-icon">{{ getFileIcon(message.fileName || message.content) }}</div>
                <div class="file-info">
                  <div class="file-name">
                    {{ message.fileName || message.content || getFileName(message.content) }}
                    <span v-if="!message.fileName && !message.content" class="debug-text">(无文件名)</span>
                  </div>
                  <div class="file-actions">
                    <button class="download-btn" @click="downloadFile(message.content)">
                      下载
                    </button>
                  </div>
                </div>
              </div>
            </template>
            
            <!-- 不支持的消息类型 -->
            <template v-else>
              <div class="unsupported-message">不支持的消息类型: {{ message.type }}</div>
            </template>
          </div>
          
          <!-- 消息时间 -->
          <div class="message-time time-other">
            {{ formatMessageTime(message.createdAt) }}
          </div>
        </div>
      </template>
      
      <!-- 自己的消息 -->
      <template v-else>
        <!-- 消息内容 -->
        <div class="message-content-wrapper">
          <!-- 自己发送的消息显示"我" -->
          <div class="sender-name self-name">
            我
          </div>
          
          <!-- 消息气泡 -->
          <div class="message-bubble self-bubble">
            <!-- 已撤回消息 -->
            <template v-if="isRecalled">
              <div class="recalled-message">
                <i class="recalled-icon">↩️</i>
                <span class="recalled-text">你撤回了一条消息</span>
              </div>
            </template>
            <!-- 文本消息 -->
            <template v-else-if="isTextMessage">
              <span v-html="formatTextWithEmojis(displayContent)"></span>
            </template>
            
            <!-- 图片消息 -->
            <template v-else-if="isImageMessage">
              <div v-if="message.mediaFileId" class="image-container">
                <img class="message-image" :src="getPublicMediaUrl(message.mediaFileId)" @click="previewImage(getPublicMediaUrl(message.mediaFileId))" alt="图片消息" />
              </div>
              <div v-else-if="message.content && message.content.startsWith('http')" class="image-container">
                <img class="message-image" :src="message.content" @click="previewImage(message.content)" alt="图片消息" />
              </div>
              <div v-else class="image-placeholder">
                图片消息加载中...
              </div>
            </template>
            
            <!-- 视频消息 -->
            <template v-else-if="isVideoMessage">
              <div class="video-message">
                <video v-if="message.mediaFileId" class="message-video" controls :src="getPublicMediaUrl(message.mediaFileId)"></video>
                <video v-else-if="message.content && message.content.startsWith('http')" class="message-video" controls :src="message.content"></video>
                <div v-else class="video-placeholder">视频消息加载中...</div>
              </div>
            </template>
            
            <!-- 文件消息 -->
            <template v-else-if="isFileMessage">
              <div class="file-message">
                <div class="file-icon">{{ getFileIcon(message.content) }}</div>
                <div class="file-info">
                  <div class="file-name">{{ message.content || getFileName(message.content) }}</div>
                  <div class="file-actions">
                    <button class="download-btn" @click="downloadFile(message.content)">
                      下载
                    </button>
                  </div>
                </div>
              </div>
            </template>
            
            <!-- 不支持的消息类型 -->
            <template v-else>
              <div class="unsupported-message">不支持的消息类型: {{ message.type }}</div>
            </template>
          </div>
          
          <!-- 消息时间 -->
          <div class="message-time time-self">
            {{ formatMessageTime(message.createdAt) }}
          </div>
          
          <!-- 消息状态 - 仅自己的消息 -->
          <div class="message-status">
            <span v-if="message.status === 'SENDING'" class="status-sending">发送中...</span>
            <span v-else-if="message.status === 'FAILED'" class="status-failed">
              发送失败
              <button class="retry-btn" @click="retryMessage">重试</button>
            </span>
            <span v-else-if="message.status === 'RECALLED'" class="status-recalled">已撤回</span>
            <MessageReadStatus v-else :status="message.status" :is-read="message.isRead || false" />
          </div>
        </div>
        
        <!-- 右侧头像 - 自己的消息 -->
        <div class="avatar-container right">
          <img class="avatar" :src="userAvatar" alt="我" />
        </div>
      </template>
    </div>
    
    <!-- 消息操作菜单 -->
    <div v-if="showMenu && !isRecalled" 
         class="context-menu" 
         :style="{ top: menuPos.y + 'px', left: menuPos.x + 'px' }" 
         @click.stop>

      <div class="menu-item" @click="openForwardDialog">
        <span class="menu-icon">↪️</span>
        <span class="menu-text">转发</span>
      </div>
      <div class="menu-item" v-if="canEdit" @click="editMessage">
        <span class="menu-icon">✏️</span>
        <span class="menu-text">编辑</span>
      </div>
      <div class="menu-item recall" v-if="canRecall" @click="recallMessage">
        <span class="menu-icon">🗑️</span>
        <span class="menu-text">撤回</span>
      </div>
      <div class="menu-item report" v-if="!message.isSelf" @click="reportMessage">
        <span class="menu-icon">🚩</span>
        <span class="menu-text">举报</span>
      </div>
    </div>

    <!-- 编辑消息对话框 -->
    <div v-if="showEditDialog" class="edit-dialog-overlay" @click.self="cancelEdit">
      <div class="edit-dialog">
        <div class="edit-dialog-header">
          <h3>编辑消息</h3>
          <button class="close-btn" @click="cancelEdit">×</button>
        </div>
        <div class="edit-dialog-body">
          <textarea 
            v-model="editContent" 
            class="edit-textarea"
            placeholder="输入新的消息内容..."
            @keydown.enter.ctrl="saveEdit"
          ></textarea>
        </div>
        <div class="edit-dialog-footer">
          <button class="cancel-btn" @click="cancelEdit">取消</button>
          <button class="save-btn" @click="saveEdit" :disabled="!editContent.trim()">保存</button>
        </div>
      </div>
    </div>

    <!-- 举报对话框 -->
    <div v-if="showReportDialog" class="report-dialog-overlay" @click.self="cancelReport">
      <div class="report-dialog">
        <div class="report-dialog-header">
          <h3>举报消息</h3>
          <button class="close-btn" @click="cancelReport">×</button>
        </div>
        <div class="report-dialog-body">
          <div class="report-form">
            <div class="form-group">
              <label>举报原因</label>
              <select v-model="reportReason" class="report-reason-select">
                <option value="">请选择举报原因</option>
                <option value="垃圾信息">垃圾信息</option>
                <option value="色情内容">色情内容</option>
                <option value="暴力内容">暴力内容</option>
                <option value="诈骗信息">诈骗信息</option>
                <option value="政治敏感">政治敏感</option>
                <option value="侮辱谩骂">侮辱谩骂</option>
                <option value="其他">其他</option>
              </select>
            </div>
            <div class="form-group">
              <label>详细描述（选填）</label>
              <textarea 
                v-model="reportDescription" 
                class="report-description"
                placeholder="请描述具体情况，有助于我们更好地处理..."
              ></textarea>
            </div>
          </div>
        </div>
        <div class="report-dialog-footer">
          <button class="cancel-btn" @click="cancelReport">取消</button>
          <button class="submit-btn" @click="submitReport" :disabled="!reportReason">提交</button>
        </div>
      </div>
    </div>

    <!-- 转发消息对话框 -->
    <forward-message-dialog 
      :is-visible="showForwardDialog" 
      :messages="[message]" 
      @close="closeForwardDialog" 
      @forward-success="handleForwardSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, defineProps, defineEmits, onMounted, ref, onUnmounted, nextTick } from 'vue';
import { api } from '@/api/request';
import MessageReadStatus from './MessageReadStatus.vue';
import ForwardMessageDialog from './ForwardMessageDialog.vue';
import { messageApi, MessageStatus } from '@/api/message';
import { reportApi } from '@/api/report';
import { ElMessage } from 'element-plus';

interface MessageProps {
  id: string | number;
  content: string;
  type?: string;
  senderId?: number | string;
  senderName?: string;
  senderAvatar?: string;
  createdAt: string;
  status?: string;
  isSelf: boolean;
  mediaFileId?: string; // 新增媒体文件ID
  fileName?: string; // 新增文件名属性
  edited?: boolean; // 新增编辑状态
  isRead?: boolean; // 新增已读状态
  memberRole?: string; // 新增成员角色属性
}

const props = defineProps<{
  message: MessageProps;
  currentUserAvatar?: string;
  showSenderName?: boolean;
  isHighlighted?: boolean; // 新增属性，用于控制是否高亮显示
}>();

const emit = defineEmits<{
  (e: 'retry', messageId: string | number): void;
  (e: 'recall', messageId: string | number): void;
  (e: 'edit', messageId: string | number, newContent: string): void;
}>();

// 默认头像
const defaultAvatar = '/favicon.ico'; // 使用项目中的本地图标作为默认头像

// 消息高亮状态
const isHighlighted = computed(() => props.isHighlighted || false);

// 媒体URL
const mediaUrl = ref('');

// 显示操作菜单
const showMenu = ref(false);
const menuPos = ref({
  x: 0,
  y: 0
});

// 全局的点击外部处理函数
let currentClickOutsideHandler: ((e: MouseEvent) => void) | null = null;

// 处理右键菜单
const handleContextMenu = (event: MouseEvent) => {
  // 1. 阻止浏览器默认右键菜单
  event.preventDefault();

  // 2. 如果消息已撤回，则不显示菜单
  if (isRecalled.value) {
    return;
  }
  
  // 3. 清除之前的监听器
  if (currentClickOutsideHandler) {
    document.removeEventListener('click', currentClickOutsideHandler);
    currentClickOutsideHandler = null;
  }
  
  // 4. 使用 pageX/pageY 作为基础定位坐标，解决布局偏移问题
  let finalX = event.pageX;
  let finalY = event.pageY;

  // 5. 显示菜单，让 DOM 更新以便后续获取尺寸
  showMenu.value = true;
  menuPos.value = { x: finalX, y: finalY };

  // 6. 使用 nextTick，在菜单真实渲染后，进行精确的边界检测和位置微调
  nextTick(() => {
    const menuElement = document.querySelector('.context-menu') as HTMLElement;
    if (!menuElement) return;

    // 获取菜单的真实尺寸
    const menuWidth = menuElement.offsetWidth;
    const menuHeight = menuElement.offsetHeight;
    
    // 获取视口尺寸
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    
    // 【关键修正】使用 clientX/clientY (相对于窗口的坐标) 来进行边界判断
    
    // 检查是否超出右边界
    if (event.clientX + menuWidth > viewportWidth) {
      // 从 pageX 中减去超出的部分，并留出边距
      finalX = event.pageX - (event.clientX + menuWidth - viewportWidth) - 10;
    }

    // 检查是否超出下边界
    if (event.clientY + menuHeight > viewportHeight) {
      // 从 pageY 中减去超出的部分，并留出边距
      finalY = event.pageY - (event.clientY + menuHeight - viewportHeight) - 10;
    }
    
    // 确保菜单不会跑到屏幕左侧或顶部的外面
    if (event.clientX < 10) {
      finalX = event.pageX - event.clientX + 10;
    }
    if (event.clientY < 10) {
      finalY = event.pageY - event.clientY + 10;
    }

    // 设置最终计算好的位置
    menuPos.value = { x: finalX, y: finalY };
    
    // 7. 在菜单渲染完成后再添加点击外部监听器
    setTimeout(() => {
      currentClickOutsideHandler = (e: MouseEvent) => {
        const menuElement = document.querySelector('.context-menu');
        // 如果点击的目标在菜单内部，则什么都不做
        if (menuElement && e.target && menuElement.contains(e.target as Node)) {
          return;
        }
        // 否则，关闭菜单并移除监听器
        showMenu.value = false;
        if (currentClickOutsideHandler) {
          document.removeEventListener('click', currentClickOutsideHandler);
          currentClickOutsideHandler = null;
        }
      };
      document.addEventListener('click', currentClickOutsideHandler);
    }, 100); // 增加延迟时间，确保菜单完全渲染
  });
};



// 加载媒体信息
const loadMediaInfo = async (mediaFileId: string | number) => {
  if (!mediaFileId) {
    console.error('媒体文件ID为空');
    return;
  }
  
  try {
    console.log('开始加载媒体信息，mediaFileId:', mediaFileId);
    const response = await api.get(`/media/files/${mediaFileId}`);
    
    console.log('媒体信息加载成功:', response);
    
    if (response && response.success && response.data) {
      // 设置媒体URL
      if (response.data.fileUrl) {
        mediaUrl.value = response.data.fileUrl;
        console.log('设置媒体URL:', mediaUrl.value);
      } else if (response.data.url) {
        mediaUrl.value = response.data.url;
        console.log('设置媒体URL:', mediaUrl.value);
      } else {
        console.error('媒体信息中没有URL:', response.data);
      }
    } else {
      console.error('加载媒体信息失败:', response);
    }
  } catch (error) {
    console.error('加载媒体信息出错:', error);
  }
};

// 带认证加载媒体
const loadMediaWithAuth = async (mediaFileId: string | number) => {
  if (!mediaFileId) {
    console.error('媒体文件ID为空');
    return;
  }
  
  try {
    console.log('开始带认证加载媒体，mediaFileId:', mediaFileId);
    
    // 获取认证令牌
    const token = localStorage.getItem('accessToken') || 
                  localStorage.getItem('token') || 
                  localStorage.getItem('auth_token') || 
                  sessionStorage.getItem('accessToken') || 
                  sessionStorage.getItem('token') || 
                  sessionStorage.getItem('auth_token');
    
    if (!token) {
      console.error('未找到认证令牌');
      return;
    }
    
    console.log('认证令牌:', token.substring(0, 10) + '...');
    
    // 构建请求头
    const headers: Record<string, string> = {
      'Authorization': `Bearer ${token}`
    };
    
    // 使用fetch API获取媒体文件
    const response = await fetch(`/api/media/content/${mediaFileId}`, {
      method: 'GET',
      headers
    });
    
    if (!response.ok) {
      console.error('获取媒体文件失败:', response.status, response.statusText);
      return;
    }
    
    // 获取媒体文件的blob
    const blob = await response.blob();
    
    // 创建URL
    const url = URL.createObjectURL(blob);
    mediaUrl.value = url;
    
    console.log('媒体文件加载成功，URL:', url);
  } catch (error) {
    console.error('加载媒体文件出错:', error);
  }
};

// 计算属性：用户头像
const userAvatar = computed(() => {
  if (props.message.isSelf) {
    // 如果是自己的消息，优先使用传入的currentUserAvatar
    return props.currentUserAvatar || defaultAvatar;
  } else {
    // 如果是对方的消息，使用消息中的senderAvatar
    return props.message.senderAvatar || defaultAvatar;
  }
});

// 计算发送者显示名称
const senderDisplayName = computed(() => {
  // 拼接角色标识和名称
  let roleBadge = '';
  if (props.message.memberRole) {
    const role = props.message.memberRole.toLowerCase();
    console.log(`消息ID ${props.message.id} 的成员角色: ${role}`);
    if (role === 'owner') {
      roleBadge = '[群主] ';
    } else if (role === 'admin') {
      roleBadge = '[管理员] ';
    } else if (role === 'member') {
      roleBadge = '[成员] ';
    }
  } else {
    console.log(`消息ID ${props.message.id} 没有成员角色信息`);
  }

  if (props.message.senderName) {
    return roleBadge + props.message.senderName;
  } else if (props.message.senderId) {
    return roleBadge + `用户 ${props.message.senderId}`;
  } else {
    return roleBadge + '未知用户';
  }
});

// 调试头像
onMounted(() => {
  if (props.message.isSelf) {
    console.log('自己的消息头像信息:');
    console.log('- 传入的currentUserAvatar:', props.currentUserAvatar);
    console.log('- 消息中的senderAvatar:', props.message.senderAvatar);
    console.log('- 最终使用的头像:', userAvatar.value);
  }
});

// 组件卸载时清理事件监听器
onUnmounted(() => {
  if (currentClickOutsideHandler) {
    document.removeEventListener('click', currentClickOutsideHandler);
    currentClickOutsideHandler = null;
  }
});

// 计算消息类型
const messageType = computed(() => {
  return (props.message.type || '').toUpperCase();
});

// 判断消息是否已撤回
const isRecalled = computed(() => {
  return props.message.status === 'RECALLED' || 
         props.message.status === MessageStatus.RECALLED;
});

// 判断是否可以撤回消息
const canRecall = computed(() => {
  // 只有自己发送的消息才能撤回
  if (!props.message.isSelf) return false;
  
  // 已撤回的消息不能再次撤回
  if (isRecalled.value) return false;
  
  // 检查消息发送时间是否在2分钟内
  const now = new Date();
  const messageTime = new Date(props.message.createdAt);
  const diffMs = now.getTime() - messageTime.getTime();
  const diffMinutes = diffMs / (1000 * 60);
  
  // 2分钟内可撤回
  return diffMinutes <= 2;
});

// 判断是否可以编辑消息
const canEdit = computed(() => {
  // 只有自己发送的文本消息才能编辑
  if (!props.message.isSelf || !isTextMessage.value) return false;
  
  // 已撤回的消息不能编辑
  if (isRecalled.value) return false;
  
  // 检查消息发送时间是否在5分钟内
  const now = new Date();
  const messageTime = new Date(props.message.createdAt);
  const diffMs = now.getTime() - messageTime.getTime();
  const diffMinutes = diffMs / (1000 * 60);
  
  // 5分钟内可编辑
  return diffMinutes <= 5;
});

// 编辑消息相关状态
const showEditDialog = ref(false);
const editContent = ref('');

// 打开编辑对话框
const editMessage = () => {
  console.log('编辑消息:', props.message.id);
  
  // 初始化编辑内容为当前消息内容
  editContent.value = props.message.content || '';
  
  // 显示编辑对话框
  showEditDialog.value = true;
  
  // 关闭上下文菜单
  showMenu.value = false;
  
  // 下一帧聚焦编辑框
  nextTick(() => {
    const textarea = document.querySelector('.edit-textarea') as HTMLTextAreaElement;
    if (textarea) {
      textarea.focus();
      // 将光标放在文本末尾
      textarea.setSelectionRange(textarea.value.length, textarea.value.length);
    }
  });
};

// 取消编辑
const cancelEdit = () => {
  showEditDialog.value = false;
  editContent.value = '';
};

// 保存编辑
const saveEdit = async () => {
  console.log('保存编辑:', props.message.id, editContent.value);
  
  // 检查消息对象是否存在
  if (!props.message) {
    console.error('消息对象为空，无法编辑');
    alert('无法编辑消息：消息对象为空');
    return;
  }
  
  // 检查消息ID是否存在
  if (props.message.id === undefined || props.message.id === null) {
    console.error('消息ID为空，无法编辑', props.message);
    alert('无法编辑消息：消息ID为空');
    return;
  }
  
  // 检查消息状态
  if (props.message.status === 'SENDING') {
    console.error('消息正在发送中，无法编辑:', props.message.id);
    alert('消息正在发送中，请等待发送完成后再编辑');
    return;
  }
  
  if (props.message.status === 'FAILED') {
    console.error('消息发送失败，无法编辑:', props.message.id);
    alert('消息发送失败，无法编辑');
    return;
  }
  
  // 检查内容是否有变化
  if (editContent.value.trim() === props.message.content) {
    console.log('消息内容没有变化，取消编辑');
    cancelEdit();
    return;
  }
  
  try {
    // 检查是否为临时消息ID
    if (typeof props.message.id === 'string' && props.message.id.startsWith('temp-')) {
      console.error('临时消息无法编辑:', props.message.id);
      alert('无法编辑消息：消息尚未发送完成');
      return;
    }
    
    // 打印消息ID的类型和值
    console.log('编辑消息ID:', props.message.id, '类型:', typeof props.message.id);
    
    // 调用API编辑消息
    const response = await messageApi.editMessage(props.message.id, editContent.value.trim());
    
    if (response.success) {
      console.log('消息编辑请求已发送成功');
      
      // 不再在这里直接更新UI，而是等待WebSocket通知
      // 消息状态更新将由WebSocket通知处理
      
      // 关闭编辑对话框
      cancelEdit();
      
      // 关闭上下文菜单
      showMenu.value = false;
    } else {
      console.error('消息编辑请求失败:', response.message);
      alert('消息编辑失败: ' + response.message);
    }
  } catch (error) {
    console.error('编辑消息出错:', error);
    alert('编辑消息失败，请稍后重试');
  }
};

const isTextMessage = computed(() => {
  return messageType.value === 'TEXT';
});

const isImageMessage = computed(() => {
  return messageType.value === 'IMAGE';
});

const isFileMessage = computed(() => {
  return messageType.value === 'FILE';
});

const isVideoMessage = computed(() => {
  return messageType.value === 'VIDEO';
});

// 处理消息内容显示
const displayContent = computed(() => {
  try {
    const content = props.message.content;
    
    // 检查内容是否为空
    if (content === undefined || content === null) {
      console.warn('消息内容为空:', props.message.id);
      return '(空消息)';
    }
    
    // 检查是否为对象或数组（可能是JSON字符串被解析为对象）
    if (typeof content === 'object') {
      try {
        console.log('消息内容是对象:', content);
        return JSON.stringify(content);
      } catch (e) {
        console.error('消息内容序列化失败:', e);
        return '(无法显示的内容)';
      }
    }
    
    // 检查是否为空字符串
    if (content === '') {
      console.warn('消息内容为空字符串:', props.message.id);
      return '(空消息)';
    }
    
    return String(content);
  } catch (error) {
    console.error('处理消息内容时出错:', error);
    return '(消息处理错误)';
  }
});

// 方法
const formatMessageTime = (timeString: string): string => {
  try {
    // 检查timeString是否有效
    if (!timeString) {
      console.warn('消息时间为空');
      return '';
    }
    
    // 尝试创建日期对象
    const date = new Date(timeString);
    
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
      console.warn('无效的日期格式:', timeString);
      return '';
    }
    
    const now = new Date();
    const isToday = date.toDateString() === now.toDateString();
    
    try {
      if (isToday) {
        // 今天的消息只显示时间
        return date.toLocaleTimeString('zh-CN', { 
          hour: '2-digit', 
          minute: '2-digit' 
        });
      } else {
        // 非今天的消息显示日期和时间
        return date.toLocaleString('zh-CN', { 
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit', 
          minute: '2-digit' 
        });
      }
    } catch (formatError) {
      // 如果本地化格式化失败，使用简单格式
      console.warn('格式化日期失败，使用简单格式:', formatError);
      if (isToday) {
        return `${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`;
      } else {
        return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`;
      }
    }
  } catch (error) {
    console.error('格式化消息时间出错:', error);
    return '';
  }
};

const getFileName = (fileUrl: string): string => {
  // 如果消息内容看起来像是文件名而不是URL，直接返回
  if (fileUrl && !fileUrl.startsWith('http') && !fileUrl.startsWith('/api/') && !fileUrl.includes('/')) {
    return fileUrl;
  }
  
  // 如果URL为空，返回默认文件名
  if (!fileUrl || typeof fileUrl !== 'string') {
    return '文件';
  }
  
  try {
    // 从URL中提取文件名
    const urlParts = fileUrl.split('/');
    const lastPart = urlParts[urlParts.length - 1];
    
    // 如果最后一部分为空，返回默认文件名
    if (!lastPart) {
      return '文件';
    }
    
    // 移除查询参数
    const fileNameParts = lastPart.split('?');
    const fileName = fileNameParts[0];
    
    // 如果文件名为空，返回默认文件名
    if (!fileName) {
      return '文件';
    }
    
    // 解码URL编码的字符
    return decodeURIComponent(fileName);
  } catch (error) {
    console.error('获取文件名出错:', error);
    return '文件';
  }
};

const downloadFile = (fileUrl: string) => {
  try {
    if (!fileUrl) {
      alert('文件链接无效');
      return;
    }
    
    // 创建一个隐藏的a标签来下载文件
    const link = document.createElement('a');
    link.href = fileUrl;
    link.download = getFileName(fileUrl) || '下载文件';
    link.target = '_blank';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (error) {
    console.error('下载文件出错:', error);
    alert('下载文件失败，请稍后重试');
  }
};

const previewImage = (imageUrl: string) => {
  // 这里可以实现图片预览功能
  // 例如打开一个模态框显示大图
  window.open(imageUrl, '_blank');
};

const retryMessage = () => {
  emit('retry', props.message.id);
};

const formatTextWithEmojis = (text: string): string => {
  if (!text) return '';
  
  // 使用正则表达式检测文本中是否包含表情符号
  const emojiRegex = /[\p{Emoji}]/gu;
  
  // 如果文本中包含表情符号，添加适当的样式
  if (emojiRegex.test(text)) {
    // 将文本中的表情符号包装在span标签中，以便应用样式
    return text.replace(emojiRegex, (match) => {
      return `<span class="emoji">${match}</span>`;
    });
  }
  
  // 如果没有表情符号，直接返回原文本
  return text;
};

// 获取媒体文件URL
const getMediaUrl = (mediaFileId: string | number): string => {
  if (!mediaFileId) {
    console.error('媒体文件ID为空');
    return '';
  }
  
  console.log('获取媒体文件URL，mediaFileId:', mediaFileId, '类型:', typeof mediaFileId);
  
  // 使用API获取媒体文件URL
  // 这里我们直接构建API URL，实际项目中可能需要使用API调用
  const baseUrl = '/api'; // 可以根据环境变量配置
  const url = `${baseUrl}/media/files/${mediaFileId}`;
  console.log('构建的媒体文件URL:', url);
  return url;
};

// 获取公开媒体URL
const getPublicMediaUrl = (mediaFileId: string | number): string => {
  if (!mediaFileId) {
    console.error('媒体文件ID为空');
    return '';
  }
  
  // 使用统一的URL格式来获取媒体内容
  const url = `/api/media/public/content/${mediaFileId}`;
  console.log('获取媒体URL:', url, '媒体ID:', mediaFileId);
  return url;
};

// 检查登录状态
const checkLoginStatus = () => {
  // 获取认证令牌
  const token = localStorage.getItem('accessToken') || 
                localStorage.getItem('token') || 
                localStorage.getItem('auth_token') || 
                sessionStorage.getItem('accessToken') || 
                sessionStorage.getItem('token') || 
                sessionStorage.getItem('auth_token');
  
  if (!token) {
    console.error('未找到认证令牌，可能未登录');
    return false;
  }
  
  console.log('已找到认证令牌:', token.substring(0, 10) + '...');
  return true;
};

// 获取文件图标
const getFileIcon = (fileName: string) => {
  if (!fileName) return '📄'; // 默认文件图标

  const lowerCaseFileName = fileName.toLowerCase();
  if (lowerCaseFileName.endsWith('.pdf')) return '📕';
  if (lowerCaseFileName.endsWith('.doc') || lowerCaseFileName.endsWith('.docx')) return '📘';
  if (lowerCaseFileName.endsWith('.xls') || lowerCaseFileName.endsWith('.xlsx')) return '📗';
  if (lowerCaseFileName.endsWith('.ppt') || lowerCaseFileName.endsWith('.pptx')) return '📙';
  if (lowerCaseFileName.endsWith('.zip') || lowerCaseFileName.endsWith('.rar')) return '🗜️';
  if (lowerCaseFileName.endsWith('.txt')) return '📝';
  if (lowerCaseFileName.endsWith('.jpg') || lowerCaseFileName.endsWith('.jpeg') || lowerCaseFileName.endsWith('.png') || lowerCaseFileName.endsWith('.gif')) return '🖼️';
  if (lowerCaseFileName.endsWith('.mp4') || lowerCaseFileName.endsWith('.avi') || lowerCaseFileName.endsWith('.mov')) return '🎬';
  if (lowerCaseFileName.endsWith('.mp3') || lowerCaseFileName.endsWith('.wav') || lowerCaseFileName.endsWith('.flac')) return '🎵';

  return '📄'; // 默认文件图标
};

// 调试消息内容
onMounted(() => {
  console.log('ChatMessage组件挂载，消息内容:', JSON.stringify(props.message, null, 2));
  console.log('消息ID:', props.message.id);
  console.log('消息类型:', messageType.value);
  console.log('消息内容类型:', typeof props.message.content);
  console.log('消息内容值:', props.message.content);
  console.log('消息mediaFileId:', props.message.mediaFileId);
  console.log('消息fileName:', props.message.fileName);
  console.log('是否为图片消息:', isImageMessage.value);
  console.log('是否为视频消息:', isVideoMessage.value);
  console.log('是否为文件消息:', isFileMessage.value);
  console.log('是否为自己发送的消息 (isSelf):', props.message.isSelf);
  console.log('消息发送者ID:', props.message.senderId);
  
  // 检查isSelf是否正确应用到CSS类
  console.log('应用到元素的CSS类:', {
    'message-self': props.message.isSelf,
    'message-other': !props.message.isSelf,
    'highlight-message': isHighlighted.value
  });
  
  // 如果是文件消息，特别调试文件名
  if (isFileMessage.value) {
    console.log('文件消息调试:');
    console.log('- 原始内容:', props.message.content);
    console.log('- fileName字段:', props.message.fileName);
    console.log('- 获取的文件名:', getFileName(props.message.content));
    console.log('- 显示的文件名:', props.message.fileName || props.message.content || getFileName(props.message.content));
  }
  
  // 检查登录状态
  const isLoggedIn = checkLoginStatus();
  console.log('登录状态:', isLoggedIn ? '已登录' : '未登录');
  
  // 如果是图片或视频消息，尝试获取URL
  if (isImageMessage.value || isVideoMessage.value) {
    // 无论content是否为空，只要有mediaFileId就尝试加载媒体
    if (props.message.mediaFileId) {
      const url = getPublicMediaUrl(props.message.mediaFileId);
      console.log('使用mediaFileId获取的公开URL:', url);
      mediaUrl.value = url; // 直接设置媒体URL
      
      // 测试URL是否可访问
      fetch(url)
        .then(response => {
          console.log('公开媒体文件URL响应状态:', response.status);
          if (!response.ok) {
            console.error('公开媒体文件URL无法访问:', url, '状态:', response.status, response.statusText);
            
            // 尝试获取错误详情
            return response.text().then(text => {
              console.error('错误详情:', text);
              return Promise.reject(new Error(`HTTP ${response.status}: ${response.statusText}`));
            });
          } else {
            console.log('公开媒体文件URL可以访问，更新mediaUrl');
            return response.blob();
          }
        })
        .then(blob => {
          // 成功获取到blob，创建对象URL
          const objectUrl = URL.createObjectURL(blob);
          mediaUrl.value = objectUrl;
          console.log('成功创建媒体对象URL:', objectUrl);
        })
        .catch(error => {
          console.error('测试公开媒体文件URL时出错:', error);
          
          // 如果公开URL访问失败，尝试带认证的URL
          if (isLoggedIn && props.message.mediaFileId) {
            console.log('尝试使用带认证的方式加载媒体文件');
            loadMediaWithAuth(props.message.mediaFileId);
          }
        });
    } else {
      console.warn('媒体消息没有mediaFileId');
    }
  } else if (props.message.content) {
    console.log('使用content作为媒体URL:', props.message.content);
  }
  
  console.log('显示内容:', displayContent.value);
  console.log('消息是否由当前用户发送 (isSelf):', props.message.isSelf);
  console.log('消息样式类:', {
    'message-self': props.message.isSelf,
    'message-other': !props.message.isSelf
  });
  
  // 调试头像信息
  console.log('消息发送者头像:', props.message.senderAvatar);
  console.log('当前用户头像:', props.currentUserAvatar);
  console.log('计算后的头像URL:', userAvatar.value);
  
  // 检查头像URL是否有效
  if (userAvatar.value && userAvatar.value !== defaultAvatar) {
    const img = new Image();
    img.onload = () => console.log('头像加载成功:', userAvatar.value);
    img.onerror = () => console.error('头像加载失败:', userAvatar.value);
    img.src = userAvatar.value;
  }
  
  // 检查DOM元素是否正确应用了CSS类
  nextTick(() => {
    const messageWrapper = document.querySelector(`[data-message-id="${props.message.id}"]`);
    if (messageWrapper) {
      console.log('消息元素的类名:', messageWrapper.className);
      console.log('消息元素是否包含message-self类:', messageWrapper.classList.contains('message-self'));
      console.log('消息元素是否包含message-other类:', messageWrapper.classList.contains('message-other'));
    } else {
      console.warn('找不到消息元素:', props.message.id);
    }
  });
});

// 撤回消息
const recallMessage = async () => {
  console.log('尝试撤回消息:', props.message);
  
  // 检查消息对象是否存在
  if (!props.message) {
    console.error('消息对象为空，无法撤回');
    alert('无法撤回消息：消息对象为空');
    return;
  }
  
  // 检查消息ID是否存在
  if (props.message.id === undefined || props.message.id === null) {
    console.error('消息ID为空，无法撤回', props.message);
    alert('无法撤回消息：消息ID为空');
    return;
  }
  
  // 检查消息状态
  if (props.message.status === 'SENDING') {
    console.error('消息正在发送中，无法撤回:', props.message.id);
    alert('消息正在发送中，请等待发送完成后再撤回');
    return;
  }
  
  if (props.message.status === 'FAILED') {
    console.error('消息发送失败，无法撤回:', props.message.id);
    alert('消息发送失败，无法撤回');
    return;
  }
  
  try {
    // 检查是否为临时消息ID
    if (typeof props.message.id === 'string' && props.message.id.startsWith('temp-')) {
      console.error('临时消息无法撤回:', props.message.id);
      alert('无法撤回消息：消息尚未发送完成');
      return;
    }
    
    // 打印消息ID的类型和值
    console.log('撤回消息ID:', props.message.id, '类型:', typeof props.message.id);
    
    // 调用API撤回消息
    const response = await messageApi.recallMessage(props.message.id);
    
    if (response.success) {
      console.log('消息撤回请求已发送成功');
      // 消息状态更新将由WebSocket通知处理，这里不再手动更新UI
      // 移除触发父组件更新消息状态的代码，因为现在由WebSocket通知统一处理
      
      // 关闭上下文菜单
      showMenu.value = false;
    } else {
      console.error('消息撤回请求失败:', response.message);
      alert('消息撤回失败: ' + response.message);
    }
  } catch (error) {
    console.error('撤回消息出错:', error);
    alert('撤回消息失败，请稍后重试');
  }
};



// 转发消息相关状态
const showForwardDialog = ref(false);

// 打开转发对话框
const openForwardDialog = () => {
  console.log('打开转发对话框，消息ID:', props.message.id);
  showForwardDialog.value = true;
  showMenu.value = false; // 关闭上下文菜单
};

// 关闭转发对话框
const closeForwardDialog = () => {
  showForwardDialog.value = false;
};

// 处理转发成功
const handleForwardSuccess = () => {
  console.log('消息转发成功');
};

// 转发消息
const forwardMessage = () => {
  openForwardDialog();
};

// 举报相关状态
const showReportDialog = ref(false);
const reportReason = ref('');
const reportDescription = ref('');

// 打开举报对话框
const reportMessage = () => {
  console.log('举报消息:', props.message.id);
  showReportDialog.value = true;
  showMenu.value = false; // 关闭上下文菜单
};

// 取消举报
const cancelReport = () => {
  showReportDialog.value = false;
  reportReason.value = '';
  reportDescription.value = '';
};

// 提交举报
const submitReport = async () => {
  if (!reportReason.value) {
    ElMessage.warning('请选择举报原因');
    return;
  }
  
  try {
    console.log('提交举报，消息ID:', props.message.id);
    
    // 使用类型断言
    const reportRequest: any = {
      reportedContentType: 'MESSAGE',
      reportedContentId: Number(props.message.id),
      reason: reportReason.value,
      description: reportDescription.value
    };
    
    // 只有当senderId存在时才添加reportedUserId字段
    if (props.message.senderId) {
      reportRequest.reportedUserId = Number(props.message.senderId);
    }
    
    const response = await reportApi.submitReport(reportRequest);
    
    if (response.success) {
      ElMessage.success('举报已提交，我们将尽快处理');
      cancelReport(); // 关闭举报对话框并重置表单
    } else {
      ElMessage.error(response.message || '举报提交失败');
    }
  } catch (error) {
    console.error('举报消息出错:', error);
    ElMessage.error('举报提交失败，请稍后重试');
  }
};
</script>

<style scoped>
/* 防止菜单导致页面出现横向滚动条的全局样式 */
:global(body) {
  overflow-x: hidden !important;
}

:global(html) {
  overflow-x: hidden !important;
}

/* 确保聊天容器不会产生横向滚动 */
:global(.chat-panel),
:global(.messages-area),
:global(.message-wrapper),
:global(.message-container) {
  overflow-x: hidden !important;
  max-width: 100% !important;
}
/* 高亮消息样式 - 更加明显和显眼 */
.highlight-message {
  position: relative;
  background-color: rgba(255, 235, 59, 0.3) !important; /* 明亮的黄色背景 */
  border-radius: 12px;
  padding: 4px;
  margin: -4px;
  box-shadow: 0 0 20px rgba(255, 193, 7, 0.6) !important; /* 金色光晕 */
  animation: highlight-glow 3s ease-out;
}

.highlight-message::before {
  content: '';
  position: absolute;
  top: -6px;
  left: -6px;
  right: -6px;
  bottom: -6px;
  background: linear-gradient(45deg, #ff9800, #ffc107, #ffeb3b, #ffc107, #ff9800);
  background-size: 400% 400%;
  border: 4px solid #ff9800; /* 更粗的橙色边框 */
  border-radius: 16px;
  pointer-events: none;
  z-index: -1;
  animation: highlight-rainbow 3s ease-out, highlight-pulse 3s ease-out;
}

.highlight-message::after {
  content: '';
  position: absolute;
  top: -10px;
  left: -10px;
  right: -10px;
  bottom: -10px;
  background: radial-gradient(circle, rgba(255, 193, 7, 0.4) 0%, transparent 70%);
  border-radius: 20px;
  pointer-events: none;
  z-index: -2;
  animation: highlight-outer-glow 3s ease-out;
}

@keyframes highlight-glow {
  0% { 
    background-color: rgba(255, 235, 59, 0.8) !important;
    box-shadow: 0 0 30px rgba(255, 193, 7, 0.9) !important;
  }
  50% { 
    background-color: rgba(255, 235, 59, 0.5) !important;
    box-shadow: 0 0 25px rgba(255, 193, 7, 0.7) !important;
  }
  100% { 
    background-color: transparent !important;
    box-shadow: none !important;
  }
}

@keyframes highlight-rainbow {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

@keyframes highlight-pulse {
  0%, 10% { 
    transform: scale(1.05);
    opacity: 1;
    border-width: 4px;
  }
  25% { 
    transform: scale(1.02);
    opacity: 0.8;
    border-width: 3px;
  }
  50% { 
    transform: scale(1.01);
    opacity: 0.6;
    border-width: 2px;
  }
  100% { 
    transform: scale(1);
    opacity: 0;
    border-width: 0px;
  }
}

@keyframes highlight-outer-glow {
  0% { opacity: 0.6; }
  50% { opacity: 0.3; }
  100% { opacity: 0; }
}

.message-wrapper {
  width: 100%;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  position: relative;
  background-color: transparent !important; /* 确保消息包装器背景透明 */
}

.message-container {
  display: flex;
  align-items: flex-start;
  width: 100%;
  background-image: unset !important; 
  background-color: transparent !important; /* 确保背景完全透明 */
}

.avatar-container {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  position: relative;
}

.avatar-container.left {
  margin-right: 12px;
}

.avatar-container.right {
  margin-left: 12px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e0e0e0;
}

.message-content-wrapper {
  flex: 1;
  max-width: calc(100% - 60px);
  display: flex;
  flex-direction: column;
  min-width: 0;
  background-color: transparent !important; /* 确保背景完全透明 */
}

.message-content-wrapper::after {
  content: "";
  display: table;
  clear: both;
  background-color: transparent !important; /* 确保伪元素也是透明的 */
}

.message-self .message-content-wrapper {
  align-items: flex-end;
  background-color: transparent !important; /* 确保自己消息的容器也是透明的 */
}

.message-other .message-content-wrapper {
  align-items: flex-start;
  background-color: transparent !important; /* 确保对方消息的容器也是透明的 */
}

/* 强化对方消息气泡边框 */
.other-bubble {
  background-color: rgba(255, 255, 255, 0.95) !important;
  border: 2px solid #555555;
  border-top-left-radius: 0;
  float: left;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  color: #333;
}

.other-bubble::after {
  content: '';
  position: absolute;
  top: 0;
  left: -8px;
  width: 0;
  height: 0;
  border-right: 8px solid #555555;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
}

.sender-name {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
  font-weight: 500;
  padding: 0 4px;
  line-height: 1.2;
}

/* 为用户ID添加特殊样式 */
.sender-name .user-id {
  color: #909399;
  font-weight: 400;
  font-size: 12px;
}

.self-name {
  text-align: right;
  color: #409eff;
}

.emoji {
  font-size: 1.5em;
  line-height: 1;
  vertical-align: middle;
  display: inline-block;
  margin: 0 2px;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  max-width: 100%;
  word-break: break-word;
  position: relative;
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.5;
  display: inline-block;
  margin-bottom: 4px;
}

.self-bubble {
  background-color: rgba(230, 247, 255, 0.95) !important;
  border: 1px solid #91d5ff;
  border-top-right-radius: 0;
  float: right;
  color: #0050b3;
}

.self-bubble::after {
  content: '';
  position: absolute;
  top: 0;
  right: -8px;
  width: 0;
  height: 0;
  border-left: 8px solid #91d5ff;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.message-status {
  font-size: 11px;
  margin-top: 2px;
}

.status-sending {
  color: #999;
}

.status-failed {
  color: #ff4d4f;
}

.status-read {
  color: #52c41a;
}

.status-recalled {
  color: #999;
  font-style: italic;
}

.retry-btn {
  background: none;
  border: none;
  color: #1890ff;
  cursor: pointer;
  padding: 0;
  font-size: 11px;
  margin-left: 4px;
}

.retry-btn:hover {
  text-decoration: underline;
}

.message-image {
  max-width: 180px;
  max-height: 150px;
  border-radius: 4px;
  cursor: pointer;
}

.media-filename {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
  text-align: center;
  word-break: break-all;
  max-width: 180px;
}

.video-message {
  width: 100%;
  max-width: 220px;
}

.message-video {
  width: 100%;
  max-height: 150px;
  border-radius: 4px;
  background-color: #000;
}

.file-message {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  font-size: 24px; /* 文件图标大小 */
  color: #555; /* 文件图标颜色 */
}

.file-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.file-name {
  font-weight: bold;
  color: #333;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.download-btn {
  background: none;
  border: 1px solid #1890ff;
  color: #1890ff;
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 12px;
  cursor: pointer;
  transition: background-color 0.2s, color 0.2s;
}

.download-btn:hover {
  background-color: #1890ff;
  color: white;
}

.unsupported-message {
  color: #999;
  font-style: italic;
}

.image-container {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background-color: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.image-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background-color: #f0f0f0;
  border-radius: 4px;
  color: #999;
  font-style: italic;
}

.video-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background-color: #f0f0f0;
  border-radius: 4px;
  color: #999;
  font-style: italic;
}

.debug-info {
  background-color: #f0f8ff;
  border: 1px solid #1890ff;
  border-radius: 4px;
  padding: 4px;
  margin-bottom: 4px;
  font-size: 12px;
  color: #1890ff;
  text-align: center;
}

.debug-button {
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 2px 4px;
  margin-left: 4px;
  font-size: 12px;
  cursor: pointer;
}

.debug-text {
  color: #ff4d4f;
  font-size: 12px;
  margin-left: 4px;
}

.context-menu {
  position: fixed;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  z-index: 9999;
  min-width: 150px;
  pointer-events: auto;
  user-select: none;
}

.menu-item {
  padding: 10px 16px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
  user-select: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: flex;
  align-items: center;
  gap: 8px;
}

.menu-item:hover {
  background-color: #f5f5f5;
}

.menu-item.recall {
  color: #e74c3c;
}

.menu-item.recall:hover {
  background-color: #fee;
}

.menu-item.report {
  color: #9b59b6;
}

.menu-item.report:hover {
  background-color: #f8f0fc;
}

.menu-icon {
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none; /* 防止span元素影响点击 */
}

.menu-text {
  font-size: 14px;
  pointer-events: none; /* 防止span元素影响点击 */
}

.recalled-message {
  color: #999;
  font-style: italic;
  display: flex;
  align-items: center;
}

.recalled-icon {
  font-style: normal;
  margin-right: 4px;
}

.recalled-text {
  font-size: 13px;
}

/* 新增：时间限制提示样式 */
.time-limit-reminder {
  font-size: 11px;
  color: #999;
  margin-left: 8px;
  font-style: italic;
}

/* 编辑对话框样式 */
.edit-dialog-overlay {
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

.edit-dialog {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.edit-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e0e0e0;
}

.edit-dialog-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
}

.close-btn:hover {
  color: #333;
}

.edit-dialog-body {
  padding: 16px;
  flex: 1;
  overflow-y: auto;
}

.edit-textarea {
  width: 100%;
  min-height: 100px;
  padding: 8px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  resize: vertical;
  font-family: inherit;
  font-size: 14px;
}

.edit-dialog-footer {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px;
  border-top: 1px solid #e0e0e0;
  gap: 8px;
}

.cancel-btn {
  background-color: #f5f5f5;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  padding: 6px 16px;
  cursor: pointer;
}

.save-btn {
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 6px 16px;
  cursor: pointer;
}

.save-btn:disabled {
  background-color: #d9d9d9;
  cursor: not-allowed;
}

.edited-indicator {
  font-size: 11px;
  color: #999;
  margin-left: 4px;
  font-style: italic;
}

/* 举报对话框样式 */
.report-dialog-overlay {
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

.report-dialog {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.report-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e0e0e0;
}

.report-dialog-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.report-dialog-body {
  padding: 16px;
  flex: 1;
  overflow-y: auto;
}

.report-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  font-size: 14px;
  color: #555;
  margin-bottom: 4px;
}

.report-reason-select {
  width: 100%;
  padding: 8px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 14px;
  color: #333;
  background-color: #f9f9f9;
}

.report-description {
  width: 100%;
  min-height: 80px;
  padding: 8px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  resize: vertical;
  font-size: 14px;
  color: #333;
  background-color: #f9f9f9;
}

.report-dialog-footer {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px;
  border-top: 1px solid #e0e0e0;
  gap: 8px;
}

.submit-btn {
  background-color: #ff4d4f;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 6px 16px;
  cursor: pointer;
}

.submit-btn:hover {
  background-color: #ff7875;
}

.submit-btn:disabled {
  background-color: #d9d9d9;
  cursor: not-allowed;
  color: #999;
}
</style>