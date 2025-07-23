<template>
  <div class="reported-content-viewer">
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="3" animated />
    </div>
    
    <div v-else-if="error" class="error-container">
      <el-alert
        title="获取内容失败"
        type="error"
        :description="error"
        show-icon
      />
    </div>
    
    <div v-else class="content-container">
      <!-- 根据内容类型展示不同的内容 -->
      <div v-if="contentType === 'USER'" class="user-content">
        <div class="content-header">
          <h3>用户信息</h3>
        </div>
        <div class="user-info">
          <el-avatar :size="64" :src="contentData?.avatar">
            {{ getInitials(contentData?.nickname) }}
          </el-avatar>
          <div class="user-details">
            <div class="user-name">{{ contentData?.nickname || '未知用户' }}</div>
            <div class="user-email">{{ contentData?.email || '无邮箱信息' }}</div>
            <div class="user-id">用户ID: {{ contentData?.id }}</div>
            <div class="user-status" v-if="contentData?.status">
              状态: {{ formatUserStatus(contentData?.status) }}
            </div>
            <div class="user-created">
              注册时间: {{ formatDate(contentData?.createdAt) }}
            </div>
          </div>
        </div>
      </div>
      
      <div v-else-if="contentType === 'MESSAGE'" class="message-content">
        <div class="content-header">
          <h3>消息内容</h3>
        </div>
        <div class="message-info">
          <div class="sender-info">
            <span class="label">发送者:</span>
            <span class="value">{{ contentData?.senderName }} (#{{ contentData?.senderId }})</span>
          </div>
          <div class="message-time">
            <span class="label">发送时间:</span>
            <span class="value">{{ formatDate(contentData?.createdAt) }}</span>
          </div>
          <div class="message-type" v-if="contentData?.type">
            <span class="label">消息类型:</span>
            <span class="value">{{ formatMessageType(contentData?.type) }}</span>
          </div>
          
          <!-- 文本消息 -->
          <div v-if="!contentData?.mediaFileId" class="message-text">
            <div class="message-bubble">
              {{ contentData?.content }}
            </div>
          </div>
          
          <!-- 媒体消息 -->
          <div v-else class="media-message">
            <div v-if="isImageType(contentData?.type)" class="image-container">
              <img :src="getMediaUrl(contentData?.mediaFileId)" alt="图片消息" @error="handleMediaError" />
              <div class="media-info">图片ID: {{ contentData?.mediaFileId }}</div>
            </div>
            <div v-else-if="isVideoType(contentData?.type)" class="video-container">
              <video controls :src="getMediaUrl(contentData?.mediaFileId)" @error="handleMediaError"></video>
              <div class="media-info">视频ID: {{ contentData?.mediaFileId }}</div>
            </div>
            <div v-else class="file-container">
              <div class="file-icon">📎</div>
              <div class="file-info">
                <div class="file-name">{{ getFileName(contentData?.content) }}</div>
                <a :href="getMediaUrl(contentData?.mediaFileId)" target="_blank" class="file-download">查看文件</a>
                <div class="media-info">文件ID: {{ contentData?.mediaFileId }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div v-else-if="contentType === 'GROUP'" class="group-content">
        <div class="content-header">
          <h3>群组信息</h3>
        </div>
        
        <!-- 显示群组已删除状态 -->
        <div v-if="contentData?.status === 'deleted'" class="group-deleted">
          <el-alert
            title="群组已删除"
            type="warning"
            :description="contentData?.message || '该群组已被解散或删除'"
            show-icon
            :closable="false"
          />
          <div class="deleted-info">
            <div class="group-id">群组ID: {{ contentData?.id }}</div>
          </div>
        </div>
        
        <!-- 显示群组已封禁状态 -->
        <div v-else-if="contentData?.isBanned" class="group-banned">
          <el-alert
            title="群组已被封禁"
            type="error"
            :description="contentData?.bannedReason || '违反社区规定'"
            show-icon
            :closable="false"
          />
          <div class="banned-info">
            <div class="group-name">群组名称: {{ contentData?.name || '未知群组' }}</div>
            <div class="ban-duration" v-if="contentData?.bannedUntil">
              封禁时间: {{ formatDate(contentData?.bannedUntil) }}
            </div>
            <div class="ban-duration" v-else>封禁类型: 永久封禁</div>
          </div>
        </div>
        
        <!-- 正常显示群组信息 -->
        <div v-else class="group-info">
          <el-avatar :size="64" :src="contentData?.avatar" shape="square">
            {{ getInitials(contentData?.name) }}
          </el-avatar>
          <div class="group-details">
            <div class="group-name">{{ contentData?.name || '未知群组' }}</div>
            <div class="group-owner">
              创建者: {{ contentData?.ownerName }} (#{{ contentData?.ownerId }})
            </div>
            <div class="group-member-count">
              成员数: {{ contentData?.memberCount || 0 }}
            </div>
            <div class="group-created">
              创建时间: {{ formatDate(contentData?.createdAt) }}
            </div>
            <div class="group-description">
              {{ contentData?.description || '无描述' }}
            </div>
          </div>
        </div>
      </div>
      
      <div v-else-if="contentType === 'GROUP_MEMBER'" class="group-member-content">
        <div class="content-header">
          <h3>群成员信息</h3>
        </div>
        
        <!-- 显示群组已删除状态 -->
        <div v-if="contentData?.groupStatus === 'deleted'" class="group-deleted">
          <el-alert
            title="群组已删除"
            type="warning"
            :description="contentData?.message || '该群组已被解散或删除'"
            show-icon
            :closable="false"
          />
          <div class="member-basic-info">
            <div class="member-name">
              <span class="label">成员:</span>
              <span class="value">{{ contentData?.nickname || '未知用户' }} (#{{ contentData?.userId }})</span>
            </div>
            <div class="group-id">
              <span class="label">原群组ID:</span>
              <span class="value">{{ contentData?.groupId }}</span>
            </div>
            <div class="note" v-if="contentData?.note">
              <span class="label">备注:</span>
              <span class="value">{{ contentData.note }}</span>
            </div>
          </div>
        </div>
        
        <!-- 显示群组已封禁状态 -->
        <div v-else-if="contentData?.groupStatus === 'banned'" class="group-banned">
          <el-alert
            title="群组已被封禁"
            type="error"
            :description="contentData?.groupBannedReason || '违反社区规定'"
            show-icon
            :closable="false"
          />
        <div class="member-info">
            <div class="member-name">
              <span class="label">成员:</span>
              <span class="value">{{ contentData?.nickname || '未知用户' }} (#{{ contentData?.userId }})</span>
            </div>
            <div class="group-name">
              <span class="label">所属群组:</span>
              <span class="value">{{ contentData?.groupName || '未知群组' }} (#{{ contentData?.groupId }})</span>
            </div>
            <div v-if="contentData?.groupBannedUntil" class="ban-duration">
              <span class="label">封禁截止时间:</span>
              <span class="value">{{ formatDate(contentData?.groupBannedUntil) }}</span>
            </div>
            <div class="member-role" v-if="contentData?.role">
              <span class="label">角色:</span>
              <span class="value">{{ formatGroupRole(contentData?.role) }}</span>
            </div>
          </div>
        </div>
        
        <!-- 正常显示成员信息 -->
        <div v-else class="member-info">
          <div class="member-name">
            <span class="label">成员:</span>
            <span class="value">{{ contentData?.nickname }} (#{{ contentData?.userId }})</span>
          </div>
          <div class="group-name">
            <span class="label">所属群组:</span>
            <span class="value">{{ contentData?.groupName }} (#{{ contentData?.groupId }})</span>
          </div>
          <div class="member-role">
            <span class="label">角色:</span>
            <span class="value">{{ formatGroupRole(contentData?.role) }}</span>
          </div>
          <div class="join-time">
            <span class="label">加入时间:</span>
            <span class="value">{{ formatDate(contentData?.joinTime) }}</span>
          </div>
          <div v-if="contentData?.userNotFound" class="user-not-found">
            <el-alert
              title="用户不在群组中"
              type="info"
              :description="contentData?.userNotFound"
              show-icon
              :closable="false"
            />
          </div>
        </div>
      </div>
      
      <div v-else-if="contentType === 'MOMENT'" class="moment-content">
        <div class="content-header">
          <h3>动态内容</h3>
        </div>
        
        <!-- 显示动态已删除状态 -->
        <div v-if="contentData?.status === 'deleted'" class="moment-deleted">
          <el-alert
            title="动态已删除"
            type="warning"
            :description="contentData?.message || '该动态已被删除'"
            show-icon
            :closable="false"
          />
          <div class="deleted-info">
            <div class="moment-id">
              <span class="label">动态ID:</span>
              <span class="value">{{ contentData?.id }}</span>
            </div>
            <div v-if="contentData?.userId" class="author-info">
              <span class="label">发布者ID:</span>
              <span class="value">{{ contentData?.userId }}</span>
            </div>
          </div>
        </div>
        
        <!-- 正常显示动态信息 -->
        <div v-else class="moment-card">
          <div class="moment-header">
            <div class="user-info">
              <span class="user-name">{{ contentData?.userNickname || '未知用户' }}</span>
              <span class="user-id">(#{{ contentData?.userId }})</span>
              <span class="moment-time">{{ formatDate(contentData?.createdAt) }}</span>
            </div>
            <div class="moment-visibility">
              可见性: {{ formatMomentVisibility(contentData?.visibility) }}
            </div>
          </div>
          
          <div class="moment-text">
            {{ contentData?.content || '无文字内容' }}
          </div>
          
          <!-- 媒体内容 -->
          <div v-if="contentData?.media && contentData.media.length > 0" class="moment-media">
            <!-- 单独处理 mediaType 和 mediaUrls 情况，兼容两种数据结构 -->
            <div v-if="contentData.mediaType === 'IMAGE' && contentData.mediaUrls?.length" class="image-container">
              <div :class="getImageGridClass(contentData.mediaUrls.length)">
                <div v-for="(url, index) in contentData.mediaUrls" :key="index" class="image-item">
                  <el-image 
                    :src="url"
                    fit="cover"
                    loading="lazy"
                    :preview-src-list="contentData.mediaUrls"
                    :initial-index="index"
                    hide-on-click-modal
                    @error="handleMediaError"
                  />
                  <div class="media-info small">图片 {{ index + 1 }}</div>
                </div>
              </div>
            </div>
            
            <!-- 单独处理 mediaType 和 mediaUrls 情况 (视频) -->
            <div v-else-if="contentData.mediaType === 'VIDEO' && contentData.mediaUrls?.length" class="video-container">
              <video 
                class="video-player" 
                :src="contentData.mediaUrls[0]" 
                controls 
                preload="metadata"
                @error="handleMediaError"
              ></video>
              <div class="media-info">视频ID: {{ contentData.mediaUrls[0].split('/').pop() }}</div>
            </div>
            
            <!-- 旧的处理方式，兼容之前的数据结构 -->
            <div v-else>
              <div class="image-grid" v-if="hasMomentImages">
              <div v-for="(media, index) in momentImages" :key="index" class="image-item">
                  <el-image 
                    :src="media.url"
                    fit="cover"
                    loading="lazy"
                    :preview-src-list="momentImages.map(item => item.url)"
                    :initial-index="index"
                    hide-on-click-modal
                    @error="handleMediaError"
                  />
                  <div class="media-info small">图片ID: {{ media.mediaFileId || index + 1 }}</div>
              </div>
            </div>
            
            <div class="video-container" v-if="hasMomentVideos">
              <div v-for="(media, index) in momentVideos" :key="index" class="video-item">
                  <video controls :src="media.url" @error="handleMediaError" class="video-player"></video>
                  <div class="media-info small">视频ID: {{ media.mediaFileId || index + 1 }}</div>
                </div>
              </div>
            </div>
          </div>
          
          <div class="moment-stats">
            <span class="likes">点赞: {{ contentData?.likeCount || 0 }}</span>
            <span class="comments">评论: {{ contentData?.commentCount || 0 }}</span>
          </div>
        </div>
      </div>
      
      <div v-else class="unknown-content">
        <el-alert
          title="不支持的内容类型"
          type="warning"
          :description="'无法显示该类型的内容详情: ' + contentType"
          show-icon
        />
        <div class="raw-data">
          <pre>{{ JSON.stringify(contentData, null, 2) }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { reportApi } from '@/api/report';
import { ElMessage, ElImage } from 'element-plus';

const props = defineProps({
  contentType: {
    type: String,
    required: true
  },
  contentId: {
    type: [Number, String],
    required: true
  }
});

const loading = ref(false);
const error = ref(null);
const contentData = ref(null);

// 加载被举报内容
const loadReportedContent = async () => {
  if (!props.contentType || !props.contentId) return;
  
  loading.value = true;
  error.value = null;
  
  try {
    const response = await reportApi.getReportedContentDetails(
      props.contentType, 
      Number(props.contentId)
    );
    
    if (response.success && response.data.content) {
      contentData.value = response.data.content;
      
      // 调试信息：打印媒体信息
      console.log('加载内容成功，类型:', props.contentType, '内容:', contentData.value);
      
      // 针对不同类型内容的媒体信息调试
      if (props.contentType === 'MESSAGE' && contentData.value.mediaFileId) {
        console.log('消息媒体信息:', {
          mediaFileId: contentData.value.mediaFileId,
          type: contentData.value.type,
          mediaUrl: getMediaUrl(contentData.value.mediaFileId)
        });
      } else if (props.contentType === 'MOMENT' && contentData.value && contentData.value.media) {
        console.log('动态媒体信息:', contentData.value.media);
        
        // 检查并输出图片和视频URL
        const processedImages = momentImages.value;
        const processedVideos = momentVideos.value;
        console.log('处理后的图片:', processedImages);
        console.log('处理后的视频:', processedVideos);
      }

      // 处理兼容性：如果存在media数组但没有mediaUrls，则从media中构造mediaUrls
      if (contentData.value && 
          !contentData.value.mediaUrls && 
          contentData.value.media && 
          contentData.value.media.length > 0) {
        // 确定主要媒体类型
        const mediaTypes = contentData.value.media.map(m => m.type);
        const primaryType = mediaTypes.includes('IMAGE') ? 'IMAGE' : 
                            mediaTypes.includes('VIDEO') ? 'VIDEO' : null;
        
        if (primaryType) {
          // 设置mediaType和mediaUrls属性
          contentData.value.mediaType = primaryType;
          contentData.value.mediaUrls = contentData.value.media
            .filter(m => m.type === primaryType)
            .map(m => m.url || getMediaUrl(m.mediaFileId));
          
          console.log('已构造媒体URLs:', contentData.value.mediaType, contentData.value.mediaUrls);
        }
      }
    } else {
      error.value = response.message || '获取内容详情失败';
    }
  } catch (err) {
    console.error('加载被举报内容失败:', err);
    error.value = err.message || '加载内容失败';
  } finally {
    loading.value = false;
  }
};

// 监听属性变化
watch([() => props.contentType, () => props.contentId], () => {
  loadReportedContent();
});

// 获取用户名称首字母（用于头像显示）
const getInitials = (name) => {
  if (!name) return '?';
  return name.charAt(0).toUpperCase();
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return date.toLocaleString();
};

// 格式化用户状态
const formatUserStatus = (status) => {
  const statusMap = {
    active: '正常',
    banned: '禁用',
    suspended: '临时封禁',
    unverified: '未验证',
    deleted: '已注销'
  };
  return statusMap[status] || status;
};

// 格式化消息类型
const formatMessageType = (type) => {
  const typeMap = {
    TEXT: '文本',
    IMAGE: '图片',
    VIDEO: '视频',
    FILE: '文件',
    AUDIO: '语音',
    LOCATION: '位置',
    SYSTEM: '系统消息'
  };
  return typeMap[type] || type;
};

// 格式化群组角色
const formatGroupRole = (role) => {
  const roleMap = {
    owner: '群主',
    admin: '管理员',
    member: '普通成员'
  };
  return roleMap[role] || role;
};

// 格式化动态可见性
const formatMomentVisibility = (visibility) => {
  const visibilityMap = {
    public: '公开',
    friends: '仅好友可见',
    private: '仅自己可见'
  };
  return visibilityMap[visibility] || visibility;
};

// 判断是否为图片类型
const isImageType = (type) => {
  return type === 'IMAGE';
};

// 判断是否为视频类型
const isVideoType = (type) => {
  return type === 'VIDEO';
};

// 获取文件名
const getFileName = (content) => {
  if (!content) return '未知文件';
  if (content.includes('/')) {
    return content.split('/').pop();
  }
  return content;
};

// 获取媒体URL
const getMediaUrl = (mediaFileId) => {
  if (!mediaFileId) return '';
  // 使用公共内容访问端点，无需认证
  return `/api/media/public/content/${mediaFileId}`;
};

// 处理媒体加载错误
const handleMediaError = (event) => {
  console.error('媒体加载失败:', event.target.src);
  // 可以在这里添加一些用户友好的提示或处理逻辑
};

// 动态中的图片，确保URL可访问
const momentImages = computed(() => {
  if (!contentData.value?.media) return [];
  // 过滤出图片类型并修正URL
  return contentData.value.media
    .filter(item => item && item.type === 'IMAGE')
    .map(item => {
      // 创建新对象，避免修改原对象
      const result = { ...item };
      
      // 检查URL是否已经是完整的，如果不是，则使用mediaFileId构建
      if (result.mediaFileId && (!result.url || !result.url.startsWith('http'))) {
        result.url = getMediaUrl(result.mediaFileId);
      }
      
      // 如果还是没有URL，使用占位符图像
      if (!result.url) {
        console.warn('图片项缺少URL:', result);
        result.url = '/images/image-placeholder.png';
      }
      
      return result;
    });
});

// 动态中的视频，确保URL可访问
const momentVideos = computed(() => {
  if (!contentData.value?.media) return [];
  // 过滤出视频类型并修正URL
  return contentData.value.media
    .filter(item => item && item.type === 'VIDEO')
    .map(item => {
      // 创建新对象，避免修改原对象
      const result = { ...item };
      
      // 检查URL是否已经是完整的，如果不是，则使用mediaFileId构建
      if (result.mediaFileId && (!result.url || !result.url.startsWith('http'))) {
        result.url = getMediaUrl(result.mediaFileId);
      }
      
      // 如果还是没有URL，添加日志
      if (!result.url) {
        console.warn('视频项缺少URL:', result);
        // 视频没有合适的占位符，所以我们不设置占位符
      }
      
      return result;
    });
});

// 是否有动态图片
const hasMomentImages = computed(() => momentImages.value.length > 0);

// 是否有动态视频
const hasMomentVideos = computed(() => momentVideos.value.length > 0);

// 获取图片网格类名
const getImageGridClass = (count) => {
  if (count <= 2) return 'image-grid-2';
  if (count <= 4) return 'image-grid-4';
  return 'image-grid-6';
};

// 组件挂载时加载数据
onMounted(() => {
  loadReportedContent();
});
</script>

<style scoped>
.reported-content-viewer {
  margin: 15px 0;
  border: 1px solid #e6e6e6;
  border-radius: 8px;
  overflow: hidden;
}

.loading-container {
  padding: 20px;
}

.error-container {
  padding: 10px;
}

.content-container {
  padding: 20px;
}

.content-header {
  margin-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 10px;
}

.content-header h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

/* 用户内容样式 */
.user-info {
  display: flex;
  align-items: flex-start;
}

.user-details {
  margin-left: 15px;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 5px;
}

.user-email, .user-id, .user-status, .user-created {
  font-size: 14px;
  color: #606266;
  margin-bottom: 5px;
}

/* 消息内容样式 */
.message-info .label {
  font-weight: 600;
  margin-right: 8px;
  color: #606266;
}

.message-bubble {
  background-color: #f2f6fc;
  padding: 10px 15px;
  border-radius: 8px;
  margin-top: 10px;
  margin-bottom: 10px;
  display: inline-block;
  max-width: 100%;
  word-break: break-word;
}

.media-message {
  margin-top: 10px;
}

.image-container img {
  max-width: 100%;
  max-height: 300px;
  border-radius: 6px;
}

.video-container video {
  max-width: 100%;
  max-height: 300px;
  border-radius: 6px;
}

.file-container {
  display: flex;
  align-items: center;
  background-color: #f2f6fc;
  padding: 10px;
  border-radius: 6px;
}

.file-icon {
  font-size: 24px;
  margin-right: 10px;
}

.file-name {
  font-size: 14px;
  margin-bottom: 5px;
}

.file-download {
  color: #409eff;
  text-decoration: none;
  font-size: 14px;
}

/* 群组内容样式 */
.group-info {
  display: flex;
  align-items: flex-start;
}

.group-details {
  margin-left: 15px;
}

.group-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 5px;
}

.group-owner, .group-member-count, .group-created {
  font-size: 14px;
  color: #606266;
  margin-bottom: 5px;
}

.group-description {
  margin-top: 10px;
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  white-space: pre-line;
}

/* 删除和封禁状态样式 */
.group-deleted, .group-banned {
  margin-bottom: 15px;
}

.deleted-info, .banned-info, .member-basic-info {
  margin-top: 15px;
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.group-id, .ban-duration {
  font-size: 14px;
  color: #606266;
  margin: 5px 0;
}

/* 群成员内容样式 */
.member-info .label, 
.member-basic-info .label {
  font-weight: 600;
  margin-right: 8px;
  color: #606266;
}

.member-info div, 
.member-basic-info div {
  margin-bottom: 10px;
}

.user-not-found {
  margin-top: 10px;
}

.note {
  font-style: italic;
  color: #E6A23C;
}

/* 动态内容样式 */
.moment-card {
  border: 1px solid #e6e6e6;
  border-radius: 8px;
  padding: 15px;
}

.moment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.user-info .user-name {
  font-weight: 600;
  margin-right: 5px;
}

.user-info .user-id {
  color: #909399;
  font-size: 13px;
}

.moment-time {
  color: #909399;
  font-size: 13px;
  margin-left: 10px;
}

.moment-visibility {
  font-size: 13px;
  color: #909399;
}

.moment-text {
  margin: 10px 0;
  line-height: 1.5;
  white-space: pre-line;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  grid-gap: 8px;
  margin-bottom: 15px;
}

.image-item img {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 4px;
}

.video-item video {
  width: 100%;
  max-height: 300px;
  border-radius: 4px;
  margin-bottom: 10px;
}

.moment-stats {
  margin-top: 10px;
  font-size: 13px;
  color: #606266;
}

.moment-stats .likes, .moment-stats .comments {
  margin-right: 15px;
}

/* 未知内容样式 */
.unknown-content {
  margin-top: 10px;
}

.raw-data {
  margin-top: 10px;
  background-color: #f9f9f9;
  padding: 10px;
  border-radius: 4px;
  overflow: auto;
}

.raw-data pre {
  margin: 0;
  font-size: 12px;
  white-space: pre-wrap;
}

/* 动态已删除状态样式 */
.moment-deleted {
  margin-bottom: 15px;
}

.moment-deleted .deleted-info {
  margin-top: 15px;
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.moment-deleted .label {
  font-weight: 600;
  margin-right: 8px;
  color: #606266;
}

.moment-deleted .moment-id,
.moment-deleted .author-info {
  font-size: 14px;
  margin-bottom: 8px;
}

.media-info {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  word-break: break-all;
}

.media-info.small {
  font-size: 10px;
  opacity: 0.8;
}

/* 图片网格布局样式 */
.image-container {
  margin-top: 12px;
  overflow: hidden;
  border-radius: 4px;
}

.image-grid-2 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-gap: 4px;
}

.image-grid-4 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
  grid-gap: 4px;
}

.image-grid-6 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-auto-rows: 120px;
  grid-gap: 4px;
}

.image-item {
  position: relative;
  overflow: hidden;
  height: 150px;
}

.video-player {
  width: 100%;
  max-height: 400px;
  border-radius: 4px;
  background-color: #000;
}

/* 兼容旧样式的同时添加新的图片网格样式 */
.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  grid-gap: 8px;
}

.moment-media {
  margin-top: 15px;
}
</style> 