<template>
  <div class="media-library">
    <div class="media-library-header">
      <h3>媒体库</h3>
      <div class="media-type-tabs">
        <button 
          v-for="type in mediaTypes" 
          :key="type.value" 
          :class="{ active: activeType === type.value }"
          @click="setActiveType(type.value)"
        >
          <i :class="type.icon"></i> {{ type.label }}
        </button>
      </div>
    </div>
    
    <div class="media-library-content">
      <div v-if="loading" class="loading-container">
        <div class="spinner"></div>
        <span>加载中...</span>
      </div>
      
      <div v-else-if="error" class="error-container">
        <i class="fas fa-exclamation-circle"></i>
        <span>{{ error }}</span>
        <button @click="loadMedia">重试</button>
      </div>
      
      <div v-else-if="mediaItems.length === 0" class="empty-container">
        <i class="fas fa-photo-video"></i>
        <span>暂无{{ getActiveTypeLabel() }}文件</span>
      </div>
      
      <div v-else class="media-grid">
        <!-- 图片和视频网格布局 -->
        <template v-if="['image', 'video'].includes(activeType)">
          <div 
            v-for="(item, index) in mediaItems" 
            :key="item.id || index" 
            class="media-item"
            @click="previewMedia(item)"
          >
            <!-- 图片缩略图 -->
            <div v-if="activeType === 'image'" class="thumbnail">
              <img 
                :src="getMediaUrl(item)" 
                :alt="item.originalFileName || item.fileName || '图片'" 
                :data-original-src="item.url || item.fileUrl || ''"
                :data-media-id="item.id || ''"
                @error="handleImageError"
              />
            </div>
            
            <!-- 视频缩略图 -->
            <div v-else-if="activeType === 'video'" class="thumbnail video-thumbnail">
              <img 
                :src="item.thumbnailUrl || '/images/video-placeholder.png'" 
                :alt="item.originalFileName || item.fileName || '视频'" 
                :data-original-src="item.thumbnailUrl || ''"
                :data-media-id="item.id || ''"
                @error="handleImageError"
              />
              <div class="play-icon">
                <i class="fas fa-play"></i>
              </div>
            </div>
            
            <div class="media-info">
              <span class="file-name">{{ getFileName(item) }}</span>
              <span class="upload-time">{{ formatDate(item.uploadTime) }}</span>
            </div>
          </div>
        </template>
        
        <!-- 文件列表布局 -->
        <template v-else>
          <div 
            v-for="(item, index) in mediaItems" 
            :key="item.id || index" 
            class="file-item"
            @click="previewMedia(item)"
          >
            <div class="file-icon">
              <i :class="getFileIcon(item)"></i>
            </div>
            <div class="file-info">
              <span class="file-name">{{ getFileName(item) }}</span>
              <span class="file-size">{{ formatFileSize(item.fileSize || 0) }}</span>
            </div>
            <div class="file-actions">
              <button class="download-btn" @click.stop="downloadMedia(item)">
                <i class="fas fa-download"></i>
              </button>
            </div>
          </div>
        </template>
      </div>
      
      <!-- 加载更多按钮 -->
      <div v-if="hasMoreItems" class="load-more">
        <button @click="loadMore" :disabled="loadingMore">
          {{ loadingMore ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </div>
    
    <!-- 媒体预览弹窗 -->
    <div v-if="previewItem" class="media-preview-modal" @click="closePreview">
      <div class="preview-content" @click.stop>
        <!-- 图片预览 -->
        <img 
          v-if="isImageFile(previewItem)" 
          :src="getMediaUrl(previewItem)" 
          :alt="getFileName(previewItem)" 
          :data-original-src="previewItem?.url || previewItem?.fileUrl || ''"
          :data-media-id="previewItem?.id || ''"
          class="preview-image"
          @error="handleImageError"
        />
        
        <!-- 视频预览 -->
        <video 
          v-else-if="isVideoFile(previewItem)" 
          :src="getMediaUrl(previewItem)" 
          controls 
          class="preview-video"
          @error="handleVideoError"
        ></video>
        
        <!-- 音频预览 -->
        <audio 
          v-else-if="isAudioFile(previewItem)" 
          :src="getMediaUrl(previewItem)" 
          controls 
          class="preview-audio"
          @error="handleAudioError"
        ></audio>
        
        <!-- 其他文件类型 -->
        <div v-else class="preview-file">
          <div class="file-icon large">
            <i :class="getFileIcon(previewItem)"></i>
          </div>
          <div class="file-name">{{ getFileName(previewItem) }}</div>
          <div class="file-size">{{ formatFileSize(previewItem.fileSize || 0) }}</div>
          <button class="download-btn" @click="downloadMedia(previewItem)">
            <i class="fas fa-download"></i> 下载
          </button>
        </div>
        
        <div class="preview-actions">
          <button class="close-btn" @click="closePreview">
            <i class="fas fa-times"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { messageApi, type MediaUploadResponse } from '@/api/message';
import { formatFileSize, getCurrentUserId } from '@/utils/helpers';
import { useSharedWebSocket } from '@/composables/useWebSocket';

// 定义组件属性
const props = defineProps({
  // 会话ID，如果不提供则显示所有媒体
  conversationId: {
    type: Number,
    required: false
  },
  // 是否在组件挂载时自动加载媒体
  autoLoad: {
    type: Boolean,
    default: true
  }
});

// 媒体类型定义
const mediaTypes = [
  { label: '图片', value: 'image', icon: 'fas fa-image' },
  { label: '视频', value: 'video', icon: 'fas fa-video' },
  { label: '音频', value: 'audio', icon: 'fas fa-music' },
  { label: '文件', value: 'file', icon: 'fas fa-file' },
  { label: '全部', value: 'all', icon: 'fas fa-photo-video' }
];

// 组件状态
const mediaItems = ref<MediaUploadResponse[]>([]);
const activeType = ref('image'); // 默认显示图片
const loading = ref(false);
const loadingMore = ref(false);
const error = ref<string | null>(null);
const page = ref(0);
const size = ref(20);
const hasMoreItems = ref(false);
const previewItem = ref<MediaUploadResponse | null>(null);

// 初始化WebSocket连接
const { 
  lastMessage: wsLastMessage
} = useSharedWebSocket(handleWebSocketMessage); // 使用共享WebSocket

// 监听WebSocket消息
function handleWebSocketMessage(data: any) {
  console.log('MediaLibrary收到WebSocket消息:', data);
  
  try {
    // 标准化消息类型（转换为大写）
    const messageType = data.type ? data.type.toUpperCase() : null;
    
    // 处理不同类型的消息
    switch (messageType) {
      case 'MEDIA_UPLOAD':
        // 新媒体文件上传
        handleMediaUpload(data);
        break;
      case 'MEDIA_DELETE':
        // 媒体文件删除
        handleMediaDelete(data);
        break;
      default:
        // 其他消息类型，不处理
        break;
    }
  } catch (error) {
    console.error('处理WebSocket消息出错:', error);
  }
}

// 处理新媒体上传
function handleMediaUpload(data: any) {
  console.log('处理新媒体上传:', data);
  
  try {
    // 提取媒体数据
    const mediaData = data.data || data;
    
    // 检查是否与当前会话相关
    if (props.conversationId && mediaData.conversationId !== props.conversationId) {
      console.log('媒体不属于当前会话，忽略');
      return;
    }
    
    // 检查媒体类型是否匹配当前筛选
    const mediaType = (mediaData.fileType || '').toLowerCase();
    if (activeType.value !== 'all') {
      if (!mediaType.includes(activeType.value)) {
        console.log(`媒体类型 ${mediaType} 不匹配当前筛选 ${activeType.value}，忽略`);
        return;
      }
    }
    
    // 检查是否已存在
    const isDuplicate = mediaItems.value.some(item => item.id === mediaData.id);
    if (isDuplicate) {
      console.log('媒体已存在，忽略');
      return;
    }
    
    // 添加到媒体列表头部
    mediaItems.value = [mediaData, ...mediaItems.value];
    console.log('新媒体已添加到列表');
  } catch (error) {
    console.error('处理新媒体上传出错:', error);
  }
}

// 处理媒体删除
function handleMediaDelete(data: any) {
  console.log('处理媒体删除:', data);
  
  try {
    // 提取媒体ID
    const mediaId = data.mediaId || (data.data && data.data.mediaId);
    if (!mediaId) {
      console.warn('媒体删除通知缺少媒体ID');
      return;
    }
    
    // 从列表中移除
    mediaItems.value = mediaItems.value.filter(item => item.id !== mediaId);
    console.log(`媒体 ${mediaId} 已从列表中移除`);
    
    // 如果正在预览该媒体，关闭预览
    if (previewItem.value && previewItem.value.id === mediaId) {
      closePreview();
    }
  } catch (error) {
    console.error('处理媒体删除出错:', error);
  }
}

// 监听会话ID变化，重新加载媒体
watch(() => props.conversationId, () => {
  resetAndLoad();
});

// 监听媒体类型变化，重新加载媒体
watch(activeType, () => {
  resetAndLoad();
});

// 组件挂载时自动加载媒体
onMounted(() => {
  if (props.autoLoad) {
    loadMedia();
  }
});

// 设置活动的媒体类型
const setActiveType = (type: string) => {
  activeType.value = type;
};

// 获取当前活动类型的标签
const getActiveTypeLabel = () => {
  const type = mediaTypes.find(t => t.value === activeType.value);
  return type ? type.label : '媒体';
};

// 重置状态并加载媒体
const resetAndLoad = () => {
  mediaItems.value = [];
  page.value = 0;
  hasMoreItems.value = false;
  loadMedia();
};

// 加载媒体
const loadMedia = async () => {
  loading.value = true;
  error.value = null;
  
  try {
    console.log(`开始加载媒体库 - 会话ID: ${props.conversationId}, 类型: ${activeType.value}, 页码: ${page.value}`);
    
    // 获取当前用户ID
    const userId = getCurrentUserId();
    console.log('当前用户ID:', userId);
    
    // 获取认证令牌
    const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
    console.log('认证令牌状态:', token ? '已获取' : '未找到');
    
    // 构建请求头
    const headers: Record<string, string> = {};
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    if (userId) {
      headers['X-User-Id'] = userId.toString();
    }
    
    console.log('请求头:', JSON.stringify(headers));
    
    const response = await messageApi.getMediaLibrary(
      props.conversationId,
      activeType.value === 'all' ? undefined : activeType.value,
      page.value,
      size.value
    );
    
    console.log('媒体库加载响应:', response);
    
    if (response.success && response.data) {
      // 确保我们有内容数组
      if (response.data.content) {
        console.log(`获取到 ${response.data.content.length} 个媒体项`);
        
        // 处理每个媒体项，确保URL是完整的
        const processedItems = response.data.content.map(item => {
          console.log('处理媒体项:', item);
          
          // 如果URL是相对路径，转换为完整URL
          if (item.fileUrl && !item.fileUrl.startsWith('http')) {
            if (!item.fileUrl.startsWith('/')) {
              item.fileUrl = '/' + item.fileUrl;
            }
            console.log('处理后的fileUrl:', item.fileUrl);
          }
          
          if (item.url && !item.url.startsWith('http')) {
            if (!item.url.startsWith('/')) {
              item.url = '/' + item.url;
            }
            console.log('处理后的url:', item.url);
          }
          
          // 确保有正确的文件类型
          if (!item.fileType && item.fileName) {
            const ext = item.fileName.split('.').pop()?.toLowerCase();
            if (ext) {
              if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'].includes(ext)) {
                item.fileType = 'IMAGE';
              } else if (['mp4', 'webm', 'avi', 'mov'].includes(ext)) {
                item.fileType = 'VIDEO';
              } else if (['mp3', 'wav', 'ogg', 'aac'].includes(ext)) {
                item.fileType = 'AUDIO';
              } else {
                item.fileType = 'FILE';
              }
              console.log('根据扩展名设置文件类型:', ext, '->', item.fileType);
            }
          }
          
          return item;
        });
        
        mediaItems.value = processedItems;
        console.log('处理后的媒体项:', processedItems);
      } else {
        console.warn('媒体库响应中没有content字段');
        mediaItems.value = [];
      }
      
      // 设置是否有更多项
      hasMoreItems.value = response.data.last === false;
      console.log('是否有更多项:', hasMoreItems.value);
    } else {
      error.value = response.message || '加载媒体失败';
      console.error('加载媒体失败:', error.value);
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载媒体失败';
    console.error('加载媒体出错:', error.value);
  } finally {
    loading.value = false;
  }
};

// 加载更多媒体
const loadMore = async () => {
  if (loadingMore.value || !hasMoreItems.value) return;
  
  loadingMore.value = true;
  page.value++;
  
  try {
    console.log(`加载更多媒体 - 会话ID: ${props.conversationId}, 类型: ${activeType.value}, 页码: ${page.value}`);
    
    // 获取当前用户ID
    const userId = getCurrentUserId();
    console.log('当前用户ID:', userId);
    
    // 获取认证令牌
    const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
    console.log('认证令牌状态:', token ? '已获取' : '未找到');
    
    const response = await messageApi.getMediaLibrary(
      props.conversationId,
      activeType.value === 'all' ? undefined : activeType.value,
      page.value,
      size.value
    );
    
    console.log('加载更多媒体响应:', response);
    
    if (response.success && response.data) {
      // 确保我们有内容数组
      if (response.data.content && response.data.content.length > 0) {
        console.log(`获取到额外的 ${response.data.content.length} 个媒体项`);
        
        // 处理每个媒体项，确保URL是完整的
        const processedItems = response.data.content.map(item => {
          console.log('处理媒体项:', item);
          
          // 如果URL是相对路径，转换为完整URL
          if (item.fileUrl && !item.fileUrl.startsWith('http')) {
            if (!item.fileUrl.startsWith('/')) {
              item.fileUrl = '/' + item.fileUrl;
            }
            console.log('处理后的fileUrl:', item.fileUrl);
          }
          
          if (item.url && !item.url.startsWith('http')) {
            if (!item.url.startsWith('/')) {
              item.url = '/' + item.url;
            }
            console.log('处理后的url:', item.url);
          }
          
          // 确保有正确的文件类型
          if (!item.fileType && item.fileName) {
            const ext = item.fileName.split('.').pop()?.toLowerCase();
            if (ext) {
              if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'].includes(ext)) {
                item.fileType = 'IMAGE';
              } else if (['mp4', 'webm', 'avi', 'mov'].includes(ext)) {
                item.fileType = 'VIDEO';
              } else if (['mp3', 'wav', 'ogg', 'aac'].includes(ext)) {
                item.fileType = 'AUDIO';
              } else {
                item.fileType = 'FILE';
              }
              console.log('根据扩展名设置文件类型:', ext, '->', item.fileType);
            }
          }
          
          return item;
        });
        
        // 添加到现有列表
        mediaItems.value = [...mediaItems.value, ...processedItems];
        console.log('更新后的媒体项总数:', mediaItems.value.length);
      }
      
      // 设置是否有更多项
      hasMoreItems.value = response.data.last === false;
      console.log('是否有更多项:', hasMoreItems.value);
    } else {
      error.value = response.message || '加载更多媒体失败';
      console.error('加载更多媒体失败:', error.value);
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载更多媒体失败';
    console.error('加载更多媒体出错:', error.value);
  } finally {
    loadingMore.value = false;
  }
};

// 预览媒体
const previewMedia = (item: MediaUploadResponse) => {
  previewItem.value = item;
};

// 关闭预览
const closePreview = () => {
  previewItem.value = null;
};

// 下载媒体
const downloadMedia = (item: MediaUploadResponse) => {
  const url = item.url || item.fileUrl;
  if (!url) return;
  
  // 创建一个临时链接并触发下载
  const a = document.createElement('a');
  a.href = url;
  a.download = getFileName(item);
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
};

// 获取文件名
const getFileName = (item: MediaUploadResponse) => {
  return item.originalFileName || item.fileName || '未命名文件';
};

// 获取文件图标
const getFileIcon = (item: MediaUploadResponse) => {
  const fileType = item.fileType?.toLowerCase() || '';
  
  if (fileType.includes('image')) return 'fas fa-file-image';
  if (fileType.includes('video')) return 'fas fa-file-video';
  if (fileType.includes('audio')) return 'fas fa-file-audio';
  if (fileType.includes('pdf')) return 'fas fa-file-pdf';
  if (fileType.includes('word') || fileType.includes('document')) return 'fas fa-file-word';
  if (fileType.includes('excel') || fileType.includes('sheet')) return 'fas fa-file-excel';
  if (fileType.includes('powerpoint') || fileType.includes('presentation')) return 'fas fa-file-powerpoint';
  if (fileType.includes('zip') || fileType.includes('compressed')) return 'fas fa-file-archive';
  if (fileType.includes('text') || fileType.includes('txt')) return 'fas fa-file-alt';
  if (fileType.includes('code') || fileType.includes('json') || fileType.includes('xml')) return 'fas fa-file-code';
  
  return 'fas fa-file';
};

// 判断是否为图片文件
const isImageFile = (item: MediaUploadResponse) => {
  const fileType = item.fileType?.toLowerCase() || '';
  return fileType.includes('image');
};

// 判断是否为视频文件
const isVideoFile = (item: MediaUploadResponse) => {
  const fileType = item.fileType?.toLowerCase() || '';
  return fileType.includes('video');
};

// 判断是否为音频文件
const isAudioFile = (item: MediaUploadResponse) => {
  const fileType = item.fileType?.toLowerCase() || '';
  return fileType.includes('audio');
};

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '';
  
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  
  if (diffDays === 0) {
    // 今天，显示时间
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  } else if (diffDays === 1) {
    return '昨天';
  } else if (diffDays < 7) {
    return `${diffDays}天前`;
  } else {
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' });
  }
};

// 获取媒体URL
const getMediaUrl = (item: MediaUploadResponse | null) => {
  if (!item) return '';
  
  // 如果有媒体ID，优先使用公共内容API
  if (item.id) {
    // 使用公开API路径，避免权限问题
    const timestamp = new Date().getTime();
    return `http://localhost:3001/api/media/public/content/${item.id}?t=${timestamp}`;
  }
  
  // 检查是否已经是完整URL
  const url = item.url || item.fileUrl || '';
  if (!url) return '';
  
  // 如果已经是完整URL（以http或https开头）
  if (url.startsWith('http://') || url.startsWith('https://')) {
    // 添加时间戳以避免缓存问题
    const timestamp = new Date().getTime();
    const separator = url.includes('?') ? '&' : '?';
    return `${url}${separator}t=${timestamp}`;
  }
  
  // 如果URL不是以/开头，添加/
  if (!url.startsWith('/')) {
    const baseUrl = `/${url}`;
    // 添加时间戳以避免缓存问题
    const timestamp = new Date().getTime();
    const separator = baseUrl.includes('?') ? '&' : '?';
    return `${baseUrl}${separator}t=${timestamp}`;
  }
  
  // 添加时间戳以避免缓存问题
  const timestamp = new Date().getTime();
  const separator = url.includes('?') ? '&' : '?';
  return `${url}${separator}t=${timestamp}`;
};

// 处理图片加载错误
const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement;
  console.error('图片加载失败:', target.src);
  
  // 获取原始URL和媒体ID
  const originalSrc = target.getAttribute('data-original-src') || '';
  const mediaId = target.getAttribute('data-media-id') || '';
  
  // 记录详细错误信息
  console.log('图片加载失败详情:', {
    originalSrc,
    mediaId,
    currentSrc: target.src,
    time: new Date().toISOString()
  });
  
  // 尝试使用备用URL
  if (mediaId) {
    // 防止循环触发error事件
    target.onerror = (e) => {
      console.error('备用URL也加载失败');
      usePlaceholder(target);
    };
    
    // 构建API路径，添加时间戳避免缓存问题
    const timestamp = new Date().getTime();
    const apiUrl = `http://localhost:3001/api/media/public/content/${mediaId}?t=${timestamp}`;
    console.log('尝试使用API路径:', apiUrl);
    target.src = apiUrl;
    return;
  }
  
  // 使用占位符
  usePlaceholder(target);
};

// 使用占位符SVG
const usePlaceholder = (imgElement: HTMLImageElement) => {
  // 防止循环触发error事件
  imgElement.onerror = null;
  
  // 使用内联SVG数据URL作为占位符，但不使用中文（避免btoa编码问题）
  const placeholderSvg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200">
      <rect width="200" height="200" fill="#f0f0f0"/>
      <text x="50%" y="50%" font-size="16" text-anchor="middle" fill="#999999" dy=".3em">Image Failed</text>
      <text x="50%" y="65%" font-size="12" text-anchor="middle" fill="#999999" dy=".3em">Check Network/Permissions</text>
    </svg>
  `;
  
  // 使用安全的方式转换为Base64（处理Unicode字符）
  const safeBase64Encode = (str: string): string => {
    // 将字符串转换为UTF-8编码的字节数组
    const utf8Bytes = new TextEncoder().encode(str);
    // 将字节数组转换为二进制字符串
    let binaryStr = '';
    utf8Bytes.forEach(byte => {
      binaryStr += String.fromCharCode(byte);
    });
    // 使用btoa对二进制字符串进行Base64编码
    return btoa(binaryStr);
  };
  
  const encodedSvg = safeBase64Encode(placeholderSvg.trim());
  imgElement.src = `data:image/svg+xml;base64,${encodedSvg}`;
  
  // 添加CSS类以便样式处理
  imgElement.classList.add('image-error');
};

// 处理视频加载错误
const handleVideoError = (event: Event) => {
  const target = event.target as HTMLVideoElement;
  console.error('视频加载失败:', target.src);
  
  // 阻止播放控件显示
  target.controls = false;
  
  // 创建并显示错误信息
  const parent = target.parentElement;
  if (parent) {
    // 移除视频元素
    target.style.display = 'none';
    
    // 检查是否已经添加了错误信息
    const existingError = parent.querySelector('.media-error-message');
    if (!existingError) {
      const errorMsg = document.createElement('div');
      errorMsg.className = 'media-error-message';
      errorMsg.innerHTML = '<i class="fas fa-exclamation-circle"></i> 视频加载失败';
      parent.appendChild(errorMsg);
    }
  }
};

// 处理音频加载错误
const handleAudioError = (event: Event) => {
  const target = event.target as HTMLAudioElement;
  console.error('音频加载失败:', target.src);
  
  // 阻止播放控件显示
  target.controls = false;
  
  // 创建并显示错误信息
  const parent = target.parentElement;
  if (parent) {
    // 移除音频元素
    target.style.display = 'none';
    
    // 检查是否已经添加了错误信息
    const existingError = parent.querySelector('.media-error-message');
    if (!existingError) {
      const errorMsg = document.createElement('div');
      errorMsg.className = 'media-error-message';
      errorMsg.innerHTML = '<i class="fas fa-exclamation-circle"></i> 音频加载失败';
      parent.appendChild(errorMsg);
    }
  }
};
</script>

<style scoped>
.media-library {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #f5f5f5;
}

.media-library-header {
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
}

.media-library-header h3 {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 500;
}

.media-type-tabs {
  display: flex;
  overflow-x: auto;
  gap: 8px;
  padding-bottom: 8px;
}

.media-type-tabs button {
  padding: 6px 12px;
  border: none;
  border-radius: 16px;
  background-color: #f0f0f0;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 6px;
}

.media-type-tabs button.active {
  background-color: #1976d2;
  color: #fff;
}

.media-type-tabs button i {
  font-size: 14px;
}

.media-library-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.loading-container,
.error-container,
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px;
  text-align: center;
  color: #757575;
}

.loading-container .spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(0, 0, 0, 0.1);
  border-left-color: #1976d2;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-container i,
.empty-container i {
  font-size: 48px;
  margin-bottom: 16px;
  color: #bdbdbd;
}

.error-container button {
  margin-top: 16px;
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  background-color: #1976d2;
  color: #fff;
  cursor: pointer;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 16px;
}

.media-item {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  background-color: #fff;
}

.media-item .thumbnail {
  position: relative;
  width: 100%;
  padding-bottom: 100%; /* 1:1 aspect ratio */
  overflow: hidden;
}

.media-item .thumbnail img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-thumbnail .play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.media-info {
  padding: 8px;
  font-size: 12px;
}

.media-info .file-name {
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.media-info .upload-time {
  display: block;
  color: #757575;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 8px;
  cursor: pointer;
}

.file-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 4px;
  background-color: #f5f5f5;
  margin-right: 12px;
  color: #616161;
}

.file-icon i {
  font-size: 20px;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-info .file-name {
  display: block;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.file-info .file-size {
  display: block;
  font-size: 12px;
  color: #757575;
}

.file-actions {
  margin-left: 8px;
}

.download-btn {
  background: none;
  border: none;
  color: #1976d2;
  cursor: pointer;
  padding: 4px;
  font-size: 16px;
}

.load-more {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.load-more button {
  padding: 8px 24px;
  border: none;
  border-radius: 4px;
  background-color: #f0f0f0;
  color: #616161;
  cursor: pointer;
}

.load-more button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 媒体预览弹窗 */
.media-preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.preview-content {
  position: relative;
  max-width: 90%;
  max-height: 90%;
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.preview-image,
.preview-video {
  max-width: 100%;
  max-height: 80vh;
  display: block;
}

.preview-audio {
  width: 300px;
  margin: 24px;
}

.preview-file {
  padding: 32px;
  text-align: center;
}

.file-icon.large {
  width: 80px;
  height: 80px;
  margin: 0 auto 16px;
}

.file-icon.large i {
  font-size: 40px;
}

.preview-actions {
  position: absolute;
  top: 8px;
  right: 8px;
}

.close-btn {
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

/* 媒体错误样式 */
.image-error {
  background-color: #f0f0f0;
  border: 1px dashed #ccc;
  object-fit: contain !important;
}

.media-error-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background-color: #f5f5f5;
  border-radius: 4px;
  color: #666;
  text-align: center;
}

.media-error-message i {
  font-size: 24px;
  margin-bottom: 8px;
  color: #ff4d4f;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .media-grid {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
    gap: 8px;
  }
}
</style> 