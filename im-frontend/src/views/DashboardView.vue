<template>
  <div class="app-container">
    <!-- 错误提示 -->
    <div v-if="showError" class="error-toast">
      <i class="icon-error"></i>
      <span>{{ errorMessage }}</span>
      <button @click="() => showError = false" class="close-btn">×</button>
    </div>
    
    <!-- 成功提示 -->
    <div v-if="showSuccess" class="success-toast">
      <i class="icon-success">✓</i>
      <span>{{ successMessage }}</span>
      <button @click="() => showSuccess = false" class="close-btn">×</button>
    </div>
    
    <!-- 标签详情视图已移除 -->
    
    <!-- 顶部状态栏 -->
    <div class="status-bar">
      <div class="title">IM通信系统</div>
      <button class="right-btn" @click="showSettings" title="设置">
        ⚙️
      </button>
    </div>
    
    <!-- 设置对话框 -->
    <settings-dialog 
      :visible="settingsDialogVisible" 
      @close="closeSettingsDialog"
    />
    
    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 标签页内容 -->
      <div :class="['tab-content', { active: activeTab === 'chat' }]">
        <div class="chats-page">
          <!-- 会话列表面板 -->
          <div class="conversations-list-panel">
          <conversations-panel 
            ref="conversationsPanel"
            :active-chat-id="activeChatId || ''"
            @select-chat="handleSelectChat"
            @pin-chat="handlePinChat"
            @mute-chat="handleMute"
            @archive-chat="handleArchiveChat"
            @delete-chat="handleDeleteChat"
            @error="handlePanelError"
          />
          </div>
          
          <!-- 聊天内容区域 -->
          <div class="chat-content-panel" v-if="activeChatId">
            <chat-panel
              :conversation-id="activeChatId"
              :chat-name="getCurrentChatName()"
              :is-group-chat="isCurrentChatGroup()"
            />
          </div>
          
          <!-- 未选择会话时的提示 -->
          <div class="empty-chat-panel" v-else>
            <div class="empty-chat-icon">💬</div>
            <div class="empty-chat-text">请选择一个会话</div>
          </div>
        </div>
      </div>
      
      <!-- 联系人列表 -->
      <div :class="['tab-content', { active: activeTab === 'contacts' && !showFriendRequestsTab && !showTagDetailsTab }]">
        <div class="contacts-page">
          <div class="contacts-header">
            <div class="search-container">
              <input 
                v-model="contactSearchKeyword"
                type="text" 
                placeholder="搜索用户ID或昵称..." 
                class="search-input"
                @keyup.enter="searchUsers"
              />
              <button class="search-btn" @click="searchUsers" title="搜索用户">
                🔍
              </button>
            </div>
          </div>
          
          <!-- 搜索结果 -->
          <div v-if="showSearchResults" class="search-results">
            <div class="search-results-list">
              <div v-if="searchLoading" class="search-loading">
                正在搜索...
              </div>
              <div v-else-if="searchResults.length === 0" class="search-empty">
                <div>未找到相关用户</div>
                <div>请检查用户ID或昵称是否正确</div>
              </div>
              <div 
                v-for="user in searchResults" 
                :key="user.id"
                class="search-result-item"
              >
                <div class="search-result-avatar">
                  <img v-if="user.avatarUrl" :src="user.avatarUrl" :alt="user.nickname || user.email || '未知用户'" />
                  <span v-else>{{ getAvatarText(user.nickname || user.email || '未知用户') }}</span>
                </div>
                <div class="search-result-info">
                  <div class="search-result-name">{{ user.nickname || user.email || '未知用户' }}</div>
                  <div class="search-result-id">ID: {{ user.userIdString || '未设置' }}</div>
                  <div class="search-result-status">{{ getRelationshipText(user.relationshipStatus) }}</div>
                </div>
                <div class="search-result-actions">
                  <button class="view-profile-btn" @click="viewUserProfile(user.userIdString || user.userId)" title="查看资料">
                    查看资料
                  </button>
                  <button 
                    v-if="user.relationshipStatus === '陌生人'"
                    class="add-friend-btn" 
                    @click="sendFriendRequest(user.userId, user.nickname || user.email || '未知用户')"
                    title="添加好友"
                  >
                    添加好友
                  </button>
                  <button 
                    v-else-if="user.relationshipStatus === '已发送请求'"
                    class="sent-request-btn"
                    disabled
                    title="已发送请求"
                  >
                    已发送
                  </button>
                  <span v-else class="relationship-status">{{ getRelationshipText(user.relationshipStatus) }}</span>
                </div>
              </div>
            </div>
          </div>
          
          <div class="contacts-functions">
            <div class="function-item" @click="openNewFriends">
              <div class="function-icon new-friends">👥</div>
              <div class="function-text">新的朋友</div>
              <div v-if="friendRequestBadge > 0" class="function-badge">{{ friendRequestBadge }}</div>
            </div>
            <div class="function-item" @click="openGroupChats">
              <div class="function-icon group-chats">💬</div>
              <div class="function-text">群聊</div>
            </div>
            <div class="function-item" @click="openTagsPage()">
              <div class="function-icon tags">🏷️</div>
              <div class="function-text">标签</div>
            </div>
          </div>
          
          <!-- 使用新的ContactsList组件 -->
          <contacts-list
            ref="contactsList"
            :current-user-id="Number(getCurrentUserId())"
            @select-contact="(contact) => { selectedContactId = contact.id }"
            @start-chat="handleContactStartChat"
            @edit-alias="handleEditAlias"
            @manage-tags="handleManageTags"
            @delete-contact="handleDeleteContact"
            @error="(msg) => showErrorMessage(msg)"
          />
        </div>
      </div>
      
      <!-- 好友请求页面 -->
      <div :class="['tab-content', 'friend-requests-page', { active: activeTab === 'contacts' && showFriendRequestsTab }]">
        <div class="page-header">
          <button class="back-btn" @click="backToContactsFromFriendRequests">‹</button>
          <div class="page-title">新的朋友</div>
          <button class="refresh-btn" @click="refreshFriendRequests" title="刷新数据">🔄</button>
        </div>
        
        <!-- 好友请求统计 -->
        <div class="friend-request-stats">
          <div class="stat-item">
            <div class="stat-number">{{ pendingRequestsCount }}</div>
            <div class="stat-label">待处理</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ allSentRequests.length }}</div>
            <div class="stat-label">已发送</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ totalAcceptedRequestsCount }}</div>
            <div class="stat-label">已同意</div>
          </div>
        </div>

        <!-- 选项卡 -->
        <div class="friend-request-tabs">
          <div 
            :class="['friend-request-tab', { active: activeRequestTab === 'received' }]"
            @click="activeRequestTab = 'received'"
          >
            收到的请求
          </div>
          <div 
            :class="['friend-request-tab', { active: activeRequestTab === 'sent' }]"
            @click="activeRequestTab = 'sent'"
          >
            发送的请求
          </div>
        </div>

        <!-- 好友请求内容区域 -->
        <div class="friend-request-content-area">
          <!-- 收到的好友请求 -->
          <div v-show="activeRequestTab === 'received'" class="friend-request-section">
            <div v-if="friendRequestsLoading" class="friend-request-loading">
              <div class="spinner"></div>
              <p>加载中...</p>
            </div>
            <div v-else-if="receivedRequests.length === 0" class="no-requests">
              <div class="no-requests-icon">👥</div>
              <div class="no-requests-text">暂无收到的好友请求</div>
            </div>
            <div v-else class="friend-request-list">
              <div v-for="request in receivedRequests" :key="request.requestId" class="friend-request-item">
                <div class="friend-request-header">
                  <div class="friend-request-avatar">
                    <img v-if="request.requesterAvatarUrl" :src="request.requesterAvatarUrl" :alt="request.requesterNickname || request.requesterUsername">
                    <div v-else>{{ getAvatarText(request.requesterNickname || request.requesterUsername) }}</div>
                  </div>
                  <div class="friend-request-user-info">
                    <div class="friend-request-name">{{ request.requesterNickname || request.requesterUsername }}</div>
                    <div class="friend-request-id">ID: {{ request.requesterUserIdStr || request.requesterId || 'N/A' }}</div>
                  </div>
                  <div class="friend-request-header-actions">
                    <button class="friend-request-btn view-profile" @click="viewUserProfile(request.requesterUserIdStr || request.requesterId)">
                      👤 查看资料
                    </button>
                  </div>
                </div>
                <div class="friend-request-content">
                  <div class="friend-request-message">{{ request.verificationMessage || '请求添加您为好友' }}</div>
                </div>
                <div class="friend-request-footer">
                  <div class="friend-request-meta">
                    <div class="friend-request-time">{{ request.createdAt ? formatRelativeTime(new Date(request.createdAt)) : '未知时间' }}</div>
                    <div :class="['friend-request-status', getStatusClass(request.status)]">{{ request.statusDescription || getRequestStatusText(request.status) }}</div>
                  </div>
                  <div class="friend-request-actions">
                    <template v-if="request.status?.toLowerCase() === 'pending'">
                      <button class="friend-request-btn accept" @click="handleFriendRequest(request.requestId, 'approve')">
                        ✓ 同意
                      </button>
                      <button class="friend-request-btn reject" @click="handleFriendRequest(request.requestId, 'reject')">
                        ✗ 拒绝
                      </button>
                    </template>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 发送的好友请求 -->
          <div v-show="activeRequestTab === 'sent'" class="friend-request-section">
            <div v-if="friendRequestsLoading" class="friend-request-loading">
              <div class="spinner"></div>
              <p>加载中...</p>
            </div>
            <div v-else-if="sentRequests.length === 0" class="no-requests">
              <div class="no-requests-icon">📤</div>
              <div class="no-requests-text">暂无发送的好友请求</div>
            </div>
            <div v-else class="friend-request-list">
              <div v-for="request in sentRequests" :key="request.requestId" class="friend-request-item">
                <div class="friend-request-header">
                  <div class="friend-request-avatar">
                    <img v-if="request.recipientAvatarUrl" :src="request.recipientAvatarUrl" :alt="request.recipientNickname || request.recipientUsername">
                    <div v-else>{{ getAvatarText(request.recipientNickname || request.recipientUsername) }}</div>
                  </div>
                  <div class="friend-request-user-info">
                    <div class="friend-request-name">{{ request.recipientNickname || request.recipientUsername }}</div>
                    <div class="friend-request-id">ID: {{ request.recipientUserIdStr || request.recipientId || 'N/A' }}</div>
                  </div>
                  <div class="friend-request-header-actions">
                    <button class="friend-request-btn view-profile" @click="viewUserProfile(request.recipientUserIdStr || request.recipientId)">
                      👤 查看资料
                    </button>
                  </div>
                </div>
                <div class="friend-request-content">
                  <div class="friend-request-message">{{ request.verificationMessage || '等待对方确认' }}</div>
                </div>
                <div class="friend-request-footer">
                  <div class="friend-request-meta">
                    <div class="friend-request-time">{{ request.createdAt ? formatRelativeTime(new Date(request.createdAt)) : '未知时间' }}</div>
                    <div :class="['friend-request-status', getStatusClass(request.status)]">{{ request.statusDescription || getRequestStatusText(request.status) }}</div>
                  </div>
                  <div class="friend-request-actions">
                    <button 
                      v-if="request.canWithdraw && request.status?.toLowerCase() === 'pending'" 
                      class="friend-request-btn cancel" 
                      @click="cancelFriendRequest(request.requestId)"
                    >
                      ✗ 取消
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 动态列表 -->
      <div :class="['tab-content', { active: activeTab === 'moments' }]">
        <div class="moments-page">
          <!-- 使用新的MomentView组件 -->
          <moment-view />
        </div>
      </div>
      
      <!-- 个人资料 -->
      <div :class="['tab-content', { active: activeTab === 'profile' }]">
        <div class="profile-page">
          <div class="profile-header" @click="editProfile" style="cursor: pointer;">
            <div class="profile-cover">
              <div class="profile-avatar">
                <img v-if="currentUser?.avatar" :src="currentUser.avatar" :alt="currentUser.name" />
                <span v-else>{{ getAvatarText(currentUser?.name || '') }}</span>
              </div>
              <div class="profile-info">
                <div class="profile-name">{{ currentUser?.name || currentUser?.nickname || '用户名' }}</div>
                <div class="profile-id">个人ID：{{ currentUser?.userIdString || currentUser?.id || '未设置' }}</div>
                <div class="profile-status">{{ getStatusDisplay() }}</div>
              </div>
              <button class="qr-btn" @click="generateQRCode(); $event.stopPropagation()" title="二维码名片">📱</button>
            </div>
          </div>
          
          <div class="profile-functions">
            <div class="function-item" @click="openSystemNotifications">
              <div class="function-icon">🔔</div>
              <div class="function-text">系统消息</div>
              <div v-if="notificationUnreadCount > 0" class="function-badge">{{ notificationUnreadCount }}</div>
              <div class="function-arrow">›</div>
            </div>
            <div class="function-item" @click="openFileManager">
              <div class="function-icon">📁</div>
              <div class="function-text">文件管理</div>
              <div class="function-arrow">›</div>
            </div>
            <div class="function-item" @click="openAccountSecurity">
              <div class="function-icon">🔒</div>
              <div class="function-text">账户与安全</div>
              <div class="function-arrow">›</div>
            </div>
            <div class="function-item" @click="showSettings">
              <div class="function-icon">⚙️</div>
              <div class="function-text">设置</div>
              <div class="function-arrow">›</div>
            </div>
            <div class="function-item" @click="openThemes">
              <div class="function-icon">🎨</div>
              <div class="function-text">主题与外观</div>
              <div class="function-arrow">›</div>
            </div>
            <div class="function-item" @click="logout">
              <div class="function-icon">🚪</div>
              <div class="function-text">退出登录</div>
              <div class="function-arrow">›</div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 系统消息页面 -->
      <div :class="['tab-content', 'system-notifications-page', { active: activeTab === 'systemNotifications' }]">
        <div class="page-header">
          <button class="back-btn" @click="backToProfile">‹</button>
          <div class="page-title">系统消息</div>
          <button class="refresh-btn" @click="refreshNotifications" title="刷新数据">🔄</button>
        </div>
        
        <!-- 使用SystemNotifications组件 -->
        <SystemNotifications />
      </div>
      
      <!-- 账户与安全页面 -->
      <div :class="['tab-content', 'account-security-page', { active: activeTab === 'accountSecurity' }]">
        <div class="page-header">
          <button class="back-btn" @click="backToProfile">‹</button>
          <div class="page-title">账户与安全</div>
        </div>
        <div class="security-functions">
          <div class="security-function-item" @click="openPasswordManagement">
            <div class="security-function-icon">🔑</div>
            <div class="security-function-text">修改密码</div>
            <div class="security-function-arrow">›</div>
          </div>
          <div class="security-function-item" @click="openLoginDeviceManagement">
            <div class="security-function-icon">📱</div>
            <div class="security-function-text">登录设备管理</div>
            <div class="security-function-arrow">›</div>
          </div>
          <div class="security-function-item" @click="openTwoFactorAuth">
            <div class="security-function-icon">🛡️</div>
            <div class="security-function-text">双重认证</div>
            <div class="security-function-arrow">›</div>
          </div>
          <div class="security-function-item" @click="openPrivacySettings">
            <div class="security-function-icon">🔐</div>
            <div class="security-function-text">隐私设置</div>
            <div class="security-function-arrow">›</div>
          </div>
        </div>
      </div>

      <!-- 文件管理页面 -->
      <div :class="['tab-content', 'file-manager-page', { active: activeTab === 'fileManager' }]">
        <div class="page-header">
          <button class="back-btn" @click="backToProfile">‹</button>
          <div class="page-title">文件管理</div>
          <button class="refresh-btn" @click="refreshFileManager" title="刷新数据">🔄</button>
        </div>
        
        <!-- 统计信息 -->
        <div class="file-stats">
          <div class="stat-item">
            <div class="stat-number">{{ fileStats.totalFiles }}</div>
            <div class="stat-label">总文件数</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ formatFileSize(fileStats.totalSize) }}</div>
            <div class="stat-label">总大小</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ fileStats.imageCount }}</div>
            <div class="stat-label">图片</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ fileStats.documentCount }}</div>
            <div class="stat-label">文档</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ fileStats.videoCount }}</div>
            <div class="stat-label">视频</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ fileStats.audioCount }}</div>
            <div class="stat-label">音频</div>
          </div>
        </div>

        <!-- 选项卡 -->
        <div class="file-tabs">
          <div :class="['file-tab', { active: activeFileTab === 'upload' }]" @click="switchFileTab('upload')">文件上传</div>
          <div :class="['file-tab', { active: activeFileTab === 'manage' }]" @click="switchFileTab('manage')">文件管理</div>
        </div>

        <!-- 上传选项卡 -->
        <div v-if="activeFileTab === 'upload'" class="file-tab-content active">
          <!-- 提示信息 -->
          <div v-if="fileSuccessMessage" class="file-alert success">{{ fileSuccessMessage }}</div>
          <div v-if="fileErrorMessage" class="file-alert error">{{ fileErrorMessage }}</div>

          <!-- 文件上传区域 -->
          <div class="upload-area" 
               @click="triggerFileInput" 
               @dragover.prevent="handleDragOver" 
               @dragleave.prevent="handleDragLeave" 
               @drop.prevent="handleDrop"
               :class="{ dragover: isDragOver }">
            <div class="upload-icon">📁</div>
            <div class="upload-text">点击选择文件或拖拽文件到此处</div>
            <div class="upload-hint">支持图片、视频、音频、文档等格式，单个文件最大50MB</div>
          </div>

          <input type="file" ref="fileInput" class="file-input" multiple @change="handleFileSelect">

          <!-- 上传选项 -->
          <div class="upload-options">
            <div class="option-group">
              <label for="uploadType">上传类型</label>
              <select id="uploadType" v-model="uploadType" @change="toggleImageOptions">
                <option value="file">普通文件</option>
                <option value="image">图片（自动压缩）</option>
              </select>
            </div>
            <div v-if="uploadType === 'image'" class="option-group">
              <label for="maxWidth">最大宽度</label>
              <input type="number" id="maxWidth" v-model="maxWidth" min="100" max="4000">
            </div>
            <div v-if="uploadType === 'image'" class="option-group">
              <label for="maxHeight">最大高度</label>
              <input type="number" id="maxHeight" v-model="maxHeight" min="100" max="4000">
            </div>
          </div>

          <!-- 上传按钮 -->
          <div class="upload-buttons">
            <button v-if="!isUploading" class="btn btn-primary" @click="uploadFiles" :disabled="selectedFiles.length === 0">
              {{ selectedFiles.length > 0 ? `上传 ${selectedFiles.length} 个文件` : '开始上传' }}
            </button>
            <button v-if="isUploading || selectedFiles.length > 0" class="btn btn-secondary" @click="cancelUpload">
              {{ isUploading ? '取消上传' : '取消选择' }}
            </button>
          </div>

          <!-- 上传进度 -->
          <div v-if="isUploading" class="upload-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
            </div>
            <div class="progress-text">{{ uploadProgressText }}</div>
          </div>
        </div>

        <!-- 管理选项卡 -->
        <div v-if="activeFileTab === 'manage'" class="file-tab-content active">
          <!-- 文件筛选 -->
          <div class="file-filter">
            <select v-model="fileTypeFilter" @change="loadFileList">
              <option value="">全部类型</option>
              <option value="IMAGE">图片</option>
              <option value="VIDEO">视频</option>
              <option value="AUDIO">音频</option>
              <option value="DOCUMENT">文档</option>
              <option value="OTHER">其他</option>
            </select>
            <button class="btn btn-secondary" @click="refreshFileManager">刷新列表</button>
          </div>

          <!-- 加载状态 -->
          <div v-if="fileLoading" class="file-loading">
            <div class="spinner"></div>
            <div>加载中...</div>
          </div>

          <!-- 文件列表 -->
          <div v-if="!fileLoading" class="file-list">
            <div v-if="fileList.length === 0" class="no-files">
              暂无文件
            </div>
            <div v-for="file in fileList" :key="file.fileId || file.id || 'unknown'" class="file-item">
              <div class="file-icon" :class="getFileTypeClass(file.fileType)">
                {{ getFileIcon(file.fileType) }}
              </div>
              <div class="file-info">
                <div class="file-name">{{ file.originalFilename || file.originalName || file.fileName }}</div>
                <div class="file-details">
                  {{ formatFileSize(file.fileSize) }} • {{ (file.createdAt || file.createTime || file.uploadTime) ? formatRelativeTime(new Date(String(file.createdAt || file.createTime || file.uploadTime))) : '未知时间' }}
                </div>
              </div>
              <div class="file-actions">
                <button class="file-action-btn" @click="viewFile(file)" title="查看">
                  👁️
                </button>
                <button class="file-action-btn" @click="copyFileUrl(file)" title="复制链接">
                  📋
                </button>
                <button class="file-action-btn" @click="downloadFile(file)" title="下载">
                  ⬇️
                </button>
                <button class="file-action-btn" @click="deleteFile(file)" title="删除">
                  🗑️
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 群聊页面 -->
      <div :class="['tab-content', { active: activeTab === 'groups' }]">
        <group-view />
      </div>
      

    </div>
    
    <!-- 底部标签栏 -->
    <div class="tab-bar">
      <div 
        v-for="tab in navigationTabs" 
        :key="tab.key"
        :class="['tab-item', { active: activeTab === tab.key }]"
        @click="activeTab = tab.key"
      >
        <div class="icon">
          <span v-if="tab.key === 'chat'">💬</span>
          <span v-else-if="tab.key === 'contacts'">👥</span>
          <span v-else-if="tab.key === 'moments'">🌟</span>
          <span v-else-if="tab.key === 'profile'">👤</span>
        </div>
        <div class="label">{{ tab.label }}</div>
        <div v-if="tab.badge > 0" class="badge">{{ tab.badge }}</div>
      </div>
    </div>
  </div>
  
  <!-- 设置模态框 -->
  <div v-if="showSettingsModal" class="modal-overlay" @click="showSettingsModal = false">
    <div class="modal-container settings-modal" @click.stop>
      <div class="modal-header">
        <h3 class="modal-title">设置</h3>
        <button class="close-btn" @click="showSettingsModal = false">×</button>
      </div>
      <div class="modal-content">
        <div class="settings-section">
          <h4>通知设置</h4>
          <div class="setting-item">
            <label for="messageNotification">消息通知</label>
            <div class="toggle-switch">
              <input 
                type="checkbox" 
                id="messageNotification" 
                v-model="userSettings.messageNotification"
              >
              <span class="slider"></span>
            </div>
          </div>
          <div class="setting-item">
            <label for="soundNotification">声音通知</label>
            <div class="toggle-switch">
              <input 
                type="checkbox" 
                id="soundNotification" 
                v-model="userSettings.soundNotification"
              >
              <span class="slider"></span>
            </div>
          </div>
          <div class="setting-item">
            <label for="vibrationNotification">振动通知</label>
            <div class="toggle-switch">
              <input 
                type="checkbox" 
                id="vibrationNotification" 
                v-model="userSettings.vibrationNotification"
              >
              <span class="slider"></span>
            </div>
          </div>
        </div>
        
        <div class="settings-section">
          <h4>聊天设置</h4>
          <div class="setting-item">
            <label for="fontSize">字体大小</label>
            <select id="fontSize" v-model="userSettings.fontSize" class="setting-select">
              <option value="small">小</option>
              <option value="medium">中</option>
              <option value="large">大</option>
            </select>
          </div>
          <div class="setting-item">
            <label for="sendMethod">发送方式</label>
            <select id="sendMethod" v-model="userSettings.sendMethod" class="setting-select">
              <option value="enter">按Enter发送</option>
              <option value="ctrl-enter">按Ctrl+Enter发送</option>
            </select>
          </div>
          <div class="setting-item">
            <label for="autoDownloadImages">自动下载图片</label>
            <div class="toggle-switch">
              <input 
                type="checkbox" 
                id="autoDownloadImages" 
                v-model="userSettings.autoDownloadImages"
              >
              <span class="slider"></span>
            </div>
          </div>
        </div>
        
        <div class="settings-section">
          <h4>隐私设置</h4>
          <div class="setting-item">
            <label for="allowStrangerView">允许陌生人查看资料</label>
            <div class="toggle-switch">
              <input 
                type="checkbox" 
                id="allowStrangerView" 
                v-model="userSettings.allowStrangerView"
              >
              <span class="slider"></span>
            </div>
          </div>
          <div class="setting-item">
            <label for="showOnlineStatus">显示在线状态</label>
            <div class="toggle-switch">
              <input 
                type="checkbox" 
                id="showOnlineStatus" 
                v-model="userSettings.showOnlineStatus"
              >
              <span class="slider"></span>
            </div>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" @click="showSettingsModal = false">取消</button>
        <button class="btn btn-primary" @click="handleSettingsUpdate(userSettings)">保存设置</button>
      </div>
    </div>
  </div>
  
  <!-- 个人资料编辑模态框 -->
  <div v-if="showProfileEditModal" class="modal-overlay" @click="showProfileEditModal = false">
    <div class="modal-container profile-edit-modal" @click.stop>
      <div class="modal-header">
        <h3 class="modal-title">编辑个人资料</h3>
        <button class="close-btn" @click="showProfileEditModal = false">×</button>
      </div>
      <div class="modal-content">
        <div class="profile-form">
          <div class="form-group">
            <label for="profileName">昵称</label>
            <input 
              type="text" 
              id="profileName" 
              v-model="userProfile.name" 
              placeholder="请输入昵称"
              maxlength="50"
            >
          </div>
          <div class="form-group">
            <label for="profileSignature">个性签名</label>
            <textarea 
              id="profileSignature" 
              v-model="userProfile.signature" 
              placeholder="请输入个性签名"
              maxlength="200"
              rows="3"
            ></textarea>
          </div>
          <div class="form-group">
            <label for="profileGender">性别</label>
            <select id="profileGender" v-model="userProfile.gender">
              <option value="">请选择</option>
              <option value="male">男</option>
              <option value="female">女</option>
              <option value="private">保密</option>
            </select>
          </div>
          <div class="form-group">
            <label for="profileBirthday">生日</label>
            <input 
              type="date" 
              id="profileBirthday" 
              v-model="userProfile.birthday"
            >
          </div>
          <div class="form-group">
            <label for="profileLocation">所在地</label>
            <input 
              type="text" 
              id="profileLocation" 
              v-model="userProfile.location" 
              placeholder="请输入所在地"
            >
          </div>
          <div class="form-group">
            <label for="profileOccupation">职业</label>
            <input 
              type="text" 
              id="profileOccupation" 
              v-model="userProfile.occupation" 
              placeholder="请输入职业"
            >
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" @click="showProfileEditModal = false">取消</button>
        <button class="btn btn-primary" @click="handleProfileSave(userProfile)">保存</button>
      </div>
    </div>
  </div>
  
  <!-- 添加好友模态框 -->
  <div v-if="showAddFriendModal" class="modal-overlay" @click="showAddFriendModal = false">
    <div class="modal-container add-friend-modal" @click.stop>
      <div class="modal-header">
        <h3 class="modal-title">添加好友</h3>
        <button class="close-btn" @click="showAddFriendModal = false">×</button>
      </div>
      <div class="modal-content">
        <div class="search-form">
          <div class="form-group">
            <label for="friendSearch">搜索用户</label>
            <input 
              type="text" 
              id="friendSearch" 
              v-model="friendSearchKeyword" 
              placeholder="请输入用户ID、邮箱或昵称"
              @keyup.enter="searchFriend"
            >
            <button class="btn btn-primary search-btn" @click="searchFriend">搜索</button>
          </div>
        </div>
        
        <div v-if="friendSearchLoading" class="search-loading">
          <div class="spinner"></div>
          <p>搜索中...</p>
        </div>
        
        <div v-else-if="friendSearchResults.length > 0" class="search-results">
          <div v-for="user in friendSearchResults" :key="user.id" class="search-result-item">
            <div class="user-avatar">
              <img v-if="user.avatar" :src="user.avatar" :alt="user.name">
              <div v-else class="avatar-placeholder">{{ getAvatarText(user.name || user.email || 'U') }}</div>
            </div>
            <div class="user-info">
              <div class="user-name">{{ user.name || user.nickname || user.email }}</div>
              <div class="user-id" v-if="user.userIdString">ID: {{ user.userIdString }}</div>
              <div class="user-relationship">{{ getRelationshipText(user.relationshipStatus) }}</div>
            </div>
            <div class="action-buttons">
              <button 
                v-if="user.relationshipStatus === '陌生人'" 
                class="btn btn-primary btn-sm" 
                @click="sendFriendRequestFromModal(user.id, user.name || user.nickname || user.email)"
              >
                添加
              </button>
              <button 
                v-else-if="user.relationshipStatus === '已发送请求'"
                class="btn btn-secondary btn-sm" 
                disabled
              >
                已发送
              </button>
              <button 
                v-else-if="user.relationshipStatus === '好友'"
                class="btn btn-success btn-sm" 
                disabled
              >
                已是好友
              </button>
              <button
                v-else
                class="btn btn-secondary btn-sm"
                @click="viewUserProfileFromModal(user.id)"
              >
                查看资料
              </button>
            </div>
          </div>
        </div>
        
        <div v-else-if="friendSearchPerformed && !friendSearchLoading" class="no-results">
          <p>未找到相关用户</p>
          <p>您可以尝试：</p>
          <ul>
            <li>检查输入是否有误</li>
            <li>尝试使用完整的用户ID或邮箱</li>
            <li>尝试其他关键词</li>
          </ul>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" @click="showAddFriendModal = false">关闭</button>
      </div>
    </div>
  </div>
  
  <!-- 好友请求模态框 -->
  <div v-if="showFriendRequestModal" class="modal-overlay" @click="() => showFriendRequestModal = false">
    <div class="modal-container" @click.stop>
      <div class="modal-header">
        <h3 class="modal-title">新的朋友</h3>
        <button class="close-btn" @click="() => showFriendRequestModal = false">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
      <div class="modal-content">
        <div class="friend-requests-tabs">
          <button 
            class="tab-btn" 
            :class="{ active: activeRequestTab === 'received' }"
            @click="activeRequestTab = 'received'"
          >
            收到的请求 ({{ receivedRequests.length }})
          </button>
          <button 
            class="tab-btn" 
            :class="{ active: activeRequestTab === 'sent' }"
            @click="activeRequestTab = 'sent'"
          >
            发送的请求 ({{ sentRequests.length }})
          </button>
        </div>
        
        <div v-if="activeRequestTab === 'received'" class="requests-list">
          <div v-if="receivedRequests.length === 0" class="empty-state">
            <p>暂无收到的好友请求</p>
          </div>
          <div v-else>
            <div v-for="request in receivedRequests" :key="request.requestId" class="request-item">
              <div class="user-avatar">
                <img v-if="request.requesterAvatarUrl" :src="request.requesterAvatarUrl" :alt="request.requesterNickname || '用户头像'">
                <div v-else class="avatar-placeholder">{{ getAvatarText(request.requesterNickname) }}</div>
              </div>
              <div class="request-info">
                <div class="user-name">{{ request.requesterNickname }}</div>
                <div class="request-message">{{ request.verificationMessage || '请求添加您为好友' }}</div>
                <div class="request-time">{{ request.createdAt ? formatRelativeTime(new Date(request.createdAt)) : '未知时间' }}</div>
              </div>
              <div class="request-actions">
                <button class="btn btn-primary btn-sm" @click="handleFriendRequest(request.requestId, 'approve')">
                  同意
                </button>
                <button class="btn btn-secondary btn-sm" @click="handleFriendRequest(request.requestId, 'reject')">
                  拒绝
                </button>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="activeRequestTab === 'sent'" class="requests-list">
          <div v-if="sentRequests.length === 0" class="empty-state">
            <p>暂无发送的好友请求</p>
          </div>
          <div v-else>
            <div v-for="request in sentRequests" :key="request.requestId" class="request-item">
              <div class="user-avatar">
                <img v-if="request.recipientAvatarUrl" :src="request.recipientAvatarUrl" :alt="(request.recipientNickname || '用户头像')">
                <div v-else class="avatar-placeholder">{{ getAvatarText(request.recipientNickname) }}</div>
              </div>
              <div class="request-info">
                <div class="user-name">{{ request.recipientNickname }}</div>
                <div class="request-message">{{ request.verificationMessage || '等待对方确认' }}</div>
                <div class="request-time">{{ request.createdAt ? formatRelativeTime(new Date(request.createdAt)) : '未知时间' }}</div>
                <div :class="['request-status', 'friend-request-status', getStatusClass(request.status)]">{{ request.status === 'PENDING' ? '等待确认' : request.status === 'ACCEPTED' ? '已同意' : '已拒绝' }}</div>
              </div>
              <div class="request-actions">
                <button 
                  v-if="request.status === 'PENDING'" 
                  class="btn btn-secondary btn-sm" 
                  @click="cancelFriendRequest(request.requestId)"
                >
                  取消
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  
  <!-- 联系人操作菜单 -->
  <div v-if="showContactMenuVisible" class="contact-menu" :class="{ show: showContactMenuVisible }">
    <div class="contact-menu-overlay" @click="() => showContactMenuVisible = false"></div>
    <div class="contact-menu-content" @click.stop>
      <div class="contact-menu-header">
        <span class="contact-menu-title">{{ selectedContactName }}</span>
        <button class="contact-menu-close" @click="() => showContactMenuVisible = false">✕</button>
      </div>
      <div class="contact-menu-actions">
        <button class="contact-menu-action" @click="selectedContactId && openContact(selectedContactId); hideContactMenu()">
          <span class="action-icon">💬</span>
          <span class="action-text">发送消息</span>
        </button>
        <button class="contact-menu-action" @click="selectedContactId && setContactAlias(selectedContactId, selectedContactName || ''); hideContactMenu()">
          <span class="action-icon">✏️</span>
          <span class="action-text">设置备注</span>
        </button>
        <button class="contact-menu-action" @click="selectedContactId && openAssignTagModal(selectedContactId, selectedContactName || '', getSelectedContactAvatar()); hideContactMenu()">
          <span class="action-icon">🏷️</span>
          <span class="action-text">分配标签</span>
        </button>
        <button class="contact-menu-action danger" @click="selectedContactId && confirmDeleteContact(selectedContactId, selectedContactName || ''); hideContactMenu()">
          <span class="action-icon">🗑️</span>
          <span class="action-text">删除好友</span>
        </button>
      </div>
    </div>
  </div>
  
  <!-- 设置备注模态框 -->
  <div v-if="showSetAliasModal" class="modal-overlay" @click="() => showSetAliasModal = false">
    <div class="modal-container alias-modal" @click.stop>
      <div class="modal-header">
        <div class="header-content">
          <div class="header-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="header-text">
            <h3 class="modal-title">设置备注</h3>
            <p class="modal-subtitle">为好友设置个性化备注名称</p>
          </div>
        </div>
        <button class="close-btn" @click="() => showSetAliasModal = false">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
      <div class="modal-content">
        <div class="contact-info">
          <div class="contact-avatar">
            <img v-if="aliasForm.contactAvatar" :src="aliasForm.contactAvatar" :alt="aliasForm.contactName" class="avatar-image">
            <span v-else class="avatar-placeholder">{{ aliasForm.contactName.charAt(0).toUpperCase() }}</span>
          </div>
          <div class="contact-details">
            <div class="contact-name">{{ aliasForm.contactName }}</div>
            <div class="contact-label">{{ aliasForm.contactNickname || '好友昵称' }}</div>
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            备注名称
          </label>
          <input 
            type="text" 
            v-model="aliasForm.alias" 
            placeholder="请输入备注名称（最多50个字符）" 
            maxlength="50" 
            class="form-input alias-input"
            ref="aliasInput"
          >
          <div class="input-hint">
            <span class="char-count">{{ aliasForm.alias.length }}/50</span>
            <span class="hint-text">设置备注后，好友列表中将显示您设置的备注名称</span>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" @click="() => showSetAliasModal = false">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          取消
        </button>
        <button type="button" class="btn btn-primary" @click="updateContactAlias">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M20 6L9 17l-5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          保存备注
        </button>
      </div>
    </div>
  </div>
  
  <!-- 分配标签模态框 -->
  <div v-if="showAssignTagModal" id="assignTagModal" class="modal">
    <div class="modal-content">
      <div class="modal-header">
        <h3>分配标签</h3>
        <span class="close" @click="closeAssignTagModal">&times;</span>
      </div>
      <div class="modal-body">
        <div class="contact-info-section">
          <div class="contact-avatar-small" :style="{ backgroundImage: tagAssignForm.contactAvatar ? `url(${tagAssignForm.contactAvatar})` : 'none' }">
            {{ !tagAssignForm.contactAvatar ? getAvatarText(tagAssignForm.contactName) : '' }}
          </div>
          <div class="contact-details-small">
            <div class="contact-name-small">{{ getSelectedContactDisplayName() }}</div>
            <div class="contact-alias-small" v-if="getSelectedContactAlias()">{{ getSelectedContactAlias() }}</div>
          </div>
        </div>
        <div class="tags-selection">
          <div class="tags-selection-header">选择标签</div>
          <div class="available-tags">
            <!-- 创建新标签按钮 -->
            <div class="create-tag-option" @click="() => showCreateTagModal = true">
              <div class="create-tag-icon">+</div>
              <div class="create-tag-text">创建新标签</div>
            </div>
            
            <!-- 暂无标签提示 -->
            <div v-if="availableTags.length === 0" class="no-tags">
              暂无可用标签
            </div>
            
            <!-- 标签选项列表 -->
            <div v-for="tag in availableTags" :key="tag.tagId || tag.id" 
                 class="tag-option" 
                 :class="{ selected: tagAssignForm.selectedTags.includes(String(tag.tagId || tag.id)) }"
                 @click="toggleTagSelection(tag.tagId || tag.id)">
              <div class="tag-option-color" :style="{ backgroundColor: tag.color || '#667eea' }"></div>
              <div class="tag-option-name">{{ tag.name }}</div>
              <div class="tag-checkbox">{{ tagAssignForm.selectedTags.includes(String(tag.tagId || tag.id)) ? '✓' : '' }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" @click="closeAssignTagModal">取消</button>
        <button class="btn btn-primary" @click="saveTagAssignment">保存</button>
      </div>
    </div>
  </div>
  
  <!-- 删除联系人确认模态框 -->
  <div v-if="showDeleteContactModal" class="modal-overlay" @click="() => showDeleteContactModal = false">
    <div class="modal-container" @click.stop>
      <div class="modal-header">
        <h3 class="modal-title">删除好友</h3>
        <button class="close-btn" @click="() => showDeleteContactModal = false">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
      <div class="modal-content">
        <div class="confirm-content">
          <div class="confirm-icon">⚠️</div>
          <p>确定要删除好友 <strong>{{ deleteContactForm.contactName }}</strong> 吗？</p>
          <p class="warning-text">删除后将无法恢复，且聊天记录也将被清除。</p>
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" @click="() => showDeleteContactModal = false">取消</button>
        <button type="button" class="btn btn-danger" @click="deleteContact">确定删除</button>
      </div>
    </div>
  </div>
  
  <!-- 标签管理页面 -->
  <div v-if="showTagsPage" class="modal-overlay" @click="() => showTagsPage = false">
    <div class="modal-container large" @click.stop>
      <div class="modal-header">
        <h3 class="modal-title">标签管理</h3>
        <button class="close-btn" @click="() => showTagsPage = false">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
      <div class="modal-content">
        <div class="tags-header">
          <button class="btn btn-primary" @click="() => showCreateTagModal = true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            新建标签
          </button>
        </div>
        
        <div class="tags-list">
          <div v-if="tags.length === 0" class="empty-state">
            <p>暂无标签，点击上方按钮创建第一个标签</p>
          </div>
          <div v-else>
            <div v-for="tag in tags" :key="tag.id" class="tag-item-row">
              <div class="tag-color-indicator" :style="{ backgroundColor: tag.color || '#667eea' }"></div>
              <div class="tag-info" @click="viewTagContacts(tag)">
                <div class="tag-name">{{ tag.name }}</div>
                <div class="tag-count">{{ tag.contactCount || 0 }} 个联系人</div>
              </div>
              <div class="tag-actions">
                <button class="btn btn-text btn-sm" @click="editTag(tag)">
                  编辑
                </button>
                <button class="btn btn-text btn-sm danger" @click="confirmDeleteTag(tag.id, tag.name)">
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
    
    <!-- 创建标签模态框 -->
    <div v-if="showCreateTagModal" class="modal-overlay" @click="closeCreateTagModal">
      <div class="create-tag-modal-content" @click.stop>
        <div class="create-tag-modal-header">
          <h3>创建新标签</h3>
          <button class="create-tag-close-btn" @click="closeCreateTagModal">&times;</button>
        </div>
        <div class="create-tag-modal-body">
          <div class="create-tag-form-group">
            <label for="createTagNameInput">标签名称</label>
            <div class="create-tag-input-container">
              <input
                id="createTagNameInput"
                v-model="createTagForm.name"
                type="text"
                placeholder="请输入标签名称"
                maxlength="20"
                @input="updateCharCount"
                class="create-tag-input"
              />
              <span class="char-count">{{ createTagForm.name.length }}/20</span>
            </div>
          </div>
          <div class="create-tag-form-group">
            <label for="createTagColorInput">标签颜色</label>
            <div class="create-tag-color-container">
              <div class="color-preview" :style="{ background: createTagForm.color, boxShadow: `0 4px 12px ${createTagForm.color}40` }"></div>
              <input
                id="createTagColorInput"
                v-model="createTagForm.color"
                type="color"
                @input="updateCreateColorPreview"
                class="create-tag-color-input"
              />
            </div>
          </div>
          <div class="create-tag-form-group">
            <label>预设颜色</label>
            <div class="color-options">
              <div
                v-for="color in presetColors"
                :key="color"
                class="color-option"
                :class="{ selected: createTagForm.color === color }"
                :style="{ backgroundColor: color }"
                :data-color="color"
                @click="selectCreateColor(color)"
              ></div>
            </div>
          </div>
        </div>
        <div class="create-tag-modal-footer">
          <button type="button" class="create-tag-btn-secondary" @click="closeCreateTagModal">取消</button>
          <button type="button" class="create-tag-btn-primary" @click="createTag">创建</button>
        </div>
      </div>
    </div>
    
    <!-- 编辑标签模态框 -->
  <div v-if="showEditTagModal" class="modal-overlay" @click="() => showEditTagModal = false">
    <div class="modal-container" @click.stop>
      <div class="modal-header">
        <h3 class="modal-title">编辑标签</h3>
        <button class="close-btn" @click="() => showEditTagModal = false">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
      <div class="modal-content">
        <div class="form-group">
          <label class="form-label">标签名称</label>
          <input 
            type="text" 
            v-model="editTagForm.name" 
            placeholder="请输入标签名称" 
            maxlength="20" 
            class="form-input"
          >
        </div>
        <div class="form-group">
          <label class="form-label">标签颜色</label>
          <div class="color-selection">
            <div class="color-preview" :style="{ backgroundColor: editTagForm.color || '#667eea' }"></div>
            <input 
              type="color" 
              v-model="editTagForm.color" 
              class="color-input"
            >
          </div>
          <div class="color-options">
            <div
              v-for="color in presetColors"
              :key="color"
              class="color-option"
              :class="{ selected: editTagForm.color === color }"
              :style="{ backgroundColor: color }"
              @click="editTagForm.color = color"
            ></div>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" @click="() => showEditTagModal = false">取消</button>
        <button type="button" class="btn btn-primary" @click="updateTag">保存</button>
      </div>
    </div>
  </div>
  
  <!-- 删除标签确认模态框 -->
  <div v-if="showDeleteTagModal" class="modal-overlay" @click="() => showDeleteTagModal = false">
    <div class="modal-container" @click.stop>
      <div class="modal-header">
        <h3 class="modal-title">删除标签</h3>
        <button class="close-btn" @click="() => showDeleteTagModal = false">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
      <div class="modal-content">
        <div class="confirm-content">
          <div class="confirm-icon">⚠️</div>
          <p>确定要删除标签 <strong>{{ deleteTagForm.tagName }}</strong> 吗？</p>
          <p class="warning-text">删除后该标签下的所有联系人将失去此标签分类。</p>
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" @click="() => showDeleteTagModal = false">取消</button>
        <button type="button" class="btn btn-danger" @click="deleteTag">确定删除</button>
      </div>
    </div>
  </div>
  
  <!-- 标签详情页面 -->
  <div v-if="showTagDetailsPage" class="modal-overlay" @click="() => showTagDetailsPage = false">
    <div class="modal-container large" @click.stop>
      <div class="modal-header">
        <h3 class="modal-title">{{ currentTagName }} ({{ tagContacts.length }} 个联系人)</h3>
        <button class="close-btn" @click="() => showTagDetailsPage = false">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
      <div class="modal-content">
        <div class="tag-contacts-list">
          <div v-if="tagContacts.length === 0" class="empty-state">
            <p>该标签下暂无联系人</p>
          </div>
          <div v-else>
            <div v-for="contact in tagContacts" :key="contact.id" class="contact-item">
              <div class="contact-avatar">
                <!-- 修复头像显示问题，同时检查多种可能的头像字段 -->
                <img 
                  v-if="contact.avatarUrl || contact.avatar || (contact.friend && contact.friend.avatarUrl)" 
                  :src="contact.avatarUrl || contact.avatar || (contact.friend && contact.friend.avatarUrl)" 
                  :alt="contact.nickname || contact.name || '头像'"
                  @error="handleAvatarError"
                >
                <div v-else class="avatar-placeholder">
                  {{ getAvatarText(contact.nickname || contact.name || contact.friendUsername || '未知') }}
                </div>
              </div>
              <div class="contact-info">
                <div class="contact-name">{{ contact.nickname || contact.name || contact.friendUsername || '未知用户' }}</div>
                <div class="contact-status">{{ contact.signature || '暂无个性签名' }}</div>
              </div>
              <div class="contact-actions">
                <button class="btn btn-outline btn-sm view-profile-btn" @click="viewUserProfile(contact.id || contact.friendId)">
                  查看资料
                </button>
                <button class="btn btn-outline btn-sm" @click="openContact(contact.id || contact.friendId)">
                  发消息
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 查看用户资料模态框 -->
  <div v-if="showViewUserProfileModal" id="userProfileModal" class="user-profile-modal" @click="closeViewUserProfileModal">
    <div class="user-profile-modal-content" @click.stop>
      <div class="user-profile-modal-header">
        <h3>用户资料</h3>
        <span class="user-profile-close" @click="closeViewUserProfileModal">&times;</span>
      </div>
      <div class="user-profile-modal-body">
        <div class="profile-header">
          <div class="profile-avatar">
            <img v-if="viewingUserProfile.avatarUrl" :src="viewingUserProfile.avatarUrl" alt="头像" style="width: 100%; height: 100%; border-radius: inherit; object-fit: cover;">
            <span v-else>{{ getAvatarText(viewingUserProfile.nickname || viewingUserProfile.email || 'U') }}</span>
          </div>
          <div class="profile-basic-info">
            <h3>{{ viewingUserProfile.nickname || '未设置' }}</h3>
            <p>ID: {{ viewingUserProfile.userIdString || '未设置' }}</p>
            <p>{{ viewingUserProfile.signature || '这个人很懒，什么都没留下' }}</p>
          </div>
        </div>
        <div class="profile-details">
          <div class="profile-item">
            <label>邮箱</label>
            <span>{{ viewingUserProfile.email || '未公开' }}</span>
          </div>
          <div class="profile-item">
            <label>手机号</label>
            <span>{{ viewingUserProfile.phoneNumber || '未公开' }}</span>
          </div>
          <div class="profile-item">
            <label>性别</label>
            <span>{{ getGenderText(viewingUserProfile.gender) }}</span>
          </div>
          <div class="profile-item">
            <label>生日</label>
            <span>{{ viewingUserProfile.birthday || '未公开' }}</span>
          </div>
          <div class="profile-item">
            <label>地区</label>
            <span>{{ viewingUserProfile.location || '未公开' }}</span>
          </div>
          <div class="profile-item">
            <label>职业</label>
            <span>{{ viewingUserProfile.occupation || '未公开' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 编辑个人资料模态框 -->
  <div v-if="showUserProfileModal" class="modal-overlay" @click="closeUserProfileModal">
    <div class="modal-container" @click.stop>
      <div class="modal-header">
        <h3 class="modal-title">编辑个人资料</h3>
        <button class="close-btn" @click="closeUserProfileModal">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
      <div class="modal-content">
        <!-- 头像设置区域 -->
        <div class="avatar-section">
          <h4 class="section-title">头像设置</h4>
          <div class="avatar-container">
            <div class="avatar-preview">
              <img v-if="userProfile.avatar" :src="userProfile.avatar" alt="用户头像" class="avatar-image">
              <div v-else class="avatar-placeholder">
                {{ getAvatarText(userProfile.name || currentUser?.name || '') }}
              </div>
              <div class="avatar-overlay" @click="triggerAvatarUpload">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="12" cy="13" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
            </div>
            <input type="file" ref="avatarInput" accept="image/*" style="display: none;" @change="uploadProfileAvatar">
            <div class="avatar-actions">
              <button class="btn btn-outline" @click="triggerAvatarUpload">更换头像</button>
              <!-- <button v-if="userProfile.avatar" class="btn btn-text" @click="removeAvatar">移除头像</button> -->
            </div>
          </div>
        </div>

        <!-- 基本信息 -->
        <div class="form-section">
          <h4 class="section-title">基本信息</h4>
          <div class="form-group">
            <label for="profileEmail" class="form-label">邮箱地址</label>
            <input type="email" id="profileEmail" :value="currentUser?.email" readonly class="form-input readonly">
            <span class="form-hint">邮箱地址不可修改</span>
          </div>
          <div class="form-group">
            <label for="profileNickname" class="form-label">昵称</label>
            <input type="text" id="profileNickname" v-model="userProfile.name" placeholder="请输入昵称" maxlength="50" class="form-input">
          </div>
          <div class="form-group">
            <label for="profileUserId" class="form-label">个人ID</label>
            <input type="text" id="profileUserId" v-model="userProfile.userIdString" placeholder="设置您的个人ID" maxlength="50" class="form-input">
            <span class="form-hint">个人ID用于他人搜索和添加您，只能包含字母、数字和下划线</span>
          </div>
          <div class="form-group">
            <label for="profileSignature" class="form-label">个性签名</label>
            <textarea id="profileSignature" v-model="userProfile.signature" placeholder="写下您的个性签名..." maxlength="255" class="form-textarea"></textarea>
          </div>
        </div>

        <!-- 联系信息 -->
        <div class="form-section">
          <h4 class="section-title">联系信息</h4>
          <div class="form-group">
            <label for="profilePhoneNumber" class="form-label">手机号</label>
            <input type="tel" id="profilePhoneNumber" v-model="userProfile.phone" placeholder="请输入手机号" maxlength="11" class="form-input">
          </div>
        </div>

        <!-- 个人详情 -->
        <div class="form-section">
          <h4 class="section-title">个人详情</h4>
          <div class="form-row">
            <div class="form-group">
              <label for="profileGender" class="form-label">性别</label>
              <select id="profileGender" v-model="userProfile.gender" class="form-select">
                <option value="">请选择性别</option>
                <option value="male">男</option>
                <option value="female">女</option>
                <option value="private">保密</option>
              </select>
            </div>
            <div class="form-group">
              <label for="profileBirthday" class="form-label">生日</label>
              <input type="date" id="profileBirthday" v-model="userProfile.birthday" min="1900-01-01" :max="new Date().toISOString().split('T')[0] || ''" class="form-input">
            </div>
          </div>
          <div class="form-group">
            <label for="profileLocation" class="form-label">所在地</label>
            <input type="text" id="profileLocation" v-model="userProfile.location" placeholder="请输入所在地" maxlength="100" class="form-input">
          </div>
          <div class="form-group">
            <label for="profileOccupation" class="form-label">职业</label>
            <input type="text" id="profileOccupation" v-model="userProfile.occupation" placeholder="请输入职业" maxlength="100" class="form-input">
          </div>
        </div>

        <!-- 个性化状态 -->
        <div class="form-section">
          <h4 class="section-title">个性化状态</h4>
          <div class="status-display">
            <div class="current-status">
              <span class="status-emoji">{{ userStatus.emoji || '😊' }}</span>
              <span class="status-text">{{ userStatus.text || '暂无状态' }}</span>
            </div>
            <button class="btn btn-outline btn-sm" @click="toggleStatusForm">{{ showStatusForm ? '收起' : '设置状态' }}</button>
          </div>
          
          <div v-if="showStatusForm" class="status-form">
            <!-- 状态类型选择 -->
            <div class="form-group">
              <label class="form-label">状态类型</label>
              <div class="radio-group">
                <label class="radio-item">
                  <input type="radio" name="statusType" value="preset" v-model="statusType">
                  <span class="radio-label">预设状态</span>
                </label>
                <label class="radio-item">
                  <input type="radio" name="statusType" value="custom" v-model="statusType">
                  <span class="radio-label">自定义状态</span>
                </label>
              </div>
            </div>
            
            <!-- 预设状态选项 -->
            <div v-if="statusType === 'preset'" class="form-group">
              <label class="form-label">快速选择状态</label>
              <div class="preset-status-grid">
                <div v-for="preset in presetStatuses" :key="preset.text" class="preset-status-item" @click="selectPresetStatus(preset)">
                  <span class="preset-emoji">{{ preset.emoji }}</span>
                  <span class="preset-text">{{ preset.text }}</span>
                </div>
              </div>
            </div>
            
            <!-- 自定义状态 -->
            <div v-if="statusType === 'custom'" class="form-row">
              <div class="form-group">
                <label for="statusEmoji" class="form-label">表情</label>
                <input type="text" id="statusEmoji" v-model="customStatus.emoji" placeholder="😊" maxlength="10" class="form-input">
              </div>
              <div class="form-group">
                <label for="statusText" class="form-label">状态文本</label>
                <input type="text" id="statusText" v-model="customStatus.text" placeholder="输入您的状态..." maxlength="100" class="form-input">
              </div>
            </div>
            
            <!-- 状态有效期 -->
            <div class="form-group">
              <label class="form-label">状态有效期</label>
              <div class="duration-options">
                <button v-for="duration in statusDurations" :key="duration.value" type="button" class="duration-btn" :class="{ active: selectedDuration === duration.value }" @click="selectDuration(duration.value)">{{ duration.label }}</button>
              </div>
            </div>
            
            <div class="status-actions">
              <button type="button" class="btn btn-primary btn-sm" @click="saveStatus">保存状态</button>
              <button type="button" class="btn btn-outline btn-sm" @click="clearStatus">清除状态</button>
            </div>
          </div>
        </div>
      </div>
      
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" @click="closeUserProfileModal">取消</button>
        <button type="button" class="btn btn-primary" @click="saveProfile">保存资料</button>
      </div>
    </div>
  </div>
  
  <!-- 选项菜单 -->
  <div v-if="showOptionsMenuVisible" class="options-menu-overlay" @click="hideOptionsMenu">
    <div class="options-menu" :class="{ show: showOptionsMenuVisible }" @click.stop>
      <div class="menu-item" @click="markAsRead(selectedChat)">
        <div class="icon">✓</div>
        <div class="text">标记为已读</div>
      </div>
      <div class="menu-item" @click="pinChat(selectedChat)">
        <div class="icon">📌</div>
        <div class="text">置顶聊天</div>
      </div>
      <div class="menu-item" @click="muteChat(selectedChat)">
        <div class="icon">🔕</div>
        <div class="text">消息免打扰</div>
      </div>
      <div class="menu-item danger" @click="deleteChat(selectedChat)">
        <div class="icon">🗑️</div>
        <div class="text">删除聊天</div>
      </div>
    </div>
  </div>

  <!-- 密码管理模态框 -->
  <div v-if="showPasswordModal" class="modal-overlay" @click="closePasswordModal">
    <div class="modal-container" @click.stop>
      <div class="modal-header">
        <h3 class="modal-title">修改密码</h3>
        <button class="close-btn" @click="closePasswordModal">×</button>
      </div>
      <div class="modal-content">
        <div v-if="passwordErrorMessage" class="alert error">{{ passwordErrorMessage }}</div>
        <div v-if="passwordSuccessMessage" class="alert success">{{ passwordSuccessMessage }}</div>
        
        <div class="form-group">
          <label for="currentPassword" class="form-label">当前密码</label>
          <input 
            type="password" 
            id="currentPassword" 
            v-model="passwordForm.currentPassword" 
            placeholder="请输入当前密码" 
            class="form-input"
          >
        </div>
        
        <div class="form-group">
          <label for="newPassword" class="form-label">新密码</label>
          <input 
            type="password" 
            id="newPassword" 
            v-model="passwordForm.newPassword" 
            placeholder="请输入新密码（至少6位）" 
            class="form-input"
          >
        </div>
        
        <div class="form-group">
          <label for="confirmPassword" class="form-label">确认新密码</label>
          <input 
            type="password" 
            id="confirmPassword" 
            v-model="passwordForm.confirmPassword" 
            placeholder="请再次输入新密码" 
            class="form-input"
          >
        </div>
        
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="closePasswordModal">取消</button>
          <button class="btn btn-primary" @click="changePassword" :disabled="passwordLoading">{{ passwordLoading ? '修改中...' : '修改密码' }}</button>
        </div>
      </div>
    </div>
  </div>

  <!-- 登录设备管理模态框 -->
  <div v-if="showDeviceModal" class="modal-overlay" @click="closeDeviceModal">
    <div class="modal-container device-modal" @click.stop>
      <div class="modal-header">
        <h3 class="modal-title">登录设备管理</h3>
        <button class="close-btn" @click="closeDeviceModal">×</button>
      </div>
      <div class="modal-content">
        <!-- 设备统计 -->
        <div class="device-stats">
          <div class="stat-item">
            <div class="stat-number">{{ deviceStats.totalDevices }}</div>
            <div class="stat-label">总设备数</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ deviceStats.onlineDevices }}</div>
            <div class="stat-label">在线设备</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ deviceStats.currentDevice ? 1 : 0 }}</div>
            <div class="stat-label">当前设备</div>
          </div>
        </div>
        
        <!-- 批量操作 -->
        <div class="device-actions">
          <button class="btn btn-secondary" @click="refreshDeviceList">刷新列表</button>
          <button class="btn btn-danger" @click="logoutAllDevices" :disabled="deviceLoading">强制下线所有设备</button>
        </div>
        
        <!-- 设备列表 -->
        <div class="device-list">
          <div v-if="deviceLoading" class="loading">加载中...</div>
          <div v-else-if="deviceList.length === 0" class="empty-devices">
            <div class="empty-icon">📱</div>
            <div class="empty-text">暂无登录设备</div>
          </div>
          <div v-else>
            <div v-for="device in deviceList" :key="device.id" class="device-item">
              <div class="device-icon">
                {{ getDeviceIcon(device.deviceType) }}
              </div>
              <div class="device-info">
                <div class="device-name">
                  {{ getDeviceName(device) }}
                  <span v-if="isCurrentDevice(device)" class="current-device-badge">当前设备</span>
                </div>
                <div class="device-details">
                  <div class="device-ip">IP地址: {{ device.ipAddress || '未知' }}</div>
                  <div class="device-time">最后登录: {{ formatDateTime(device.lastLoginAt) }}</div>
                  <div class="device-status" :class="{ online: device.isActive }">
                    {{ device.isActive ? '在线' : '离线' }}
                  </div>
                </div>
              </div>
              <div class="device-actions">
                <button 
                  v-if="!isCurrentDevice(device) && device.isActive" 
                  class="btn btn-danger btn-sm" 
                  @click="logoutDevice(device)"
                  :disabled="deviceLoading"
                >
                  强制下线
                </button>
                <span 
                  v-else-if="!isCurrentDevice(device) && !device.isActive"
                  class="btn btn-secondary btn-sm disabled-btn"
                >
                  已离线
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 确认模态框 -->
  <div v-if="showConfirmModal" class="modal-overlay" @click="closeConfirmModal">
    <div class="modal-container confirm-modal" @click.stop>
      <div class="modal-header">
        <h3 class="modal-title">{{ confirmModalData.title }}</h3>
        <button class="close-btn" @click="closeConfirmModal">×</button>
      </div>
      <div class="modal-content">
        <div class="confirm-icon" :class="confirmModalData.type">
          <span v-if="confirmModalData.type === 'danger'">⚠️</span>
          <span v-else-if="confirmModalData.type === 'warning'">⚠️</span>
          <span v-else>ℹ️</span>
        </div>
        <div class="confirm-message">{{ confirmModalData.message }}</div>
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="closeConfirmModal">
            {{ confirmModalData.cancelText }}
          </button>
          <button 
            class="btn" 
            :class="{
              'btn-danger': confirmModalData.type === 'danger',
              'btn-primary': confirmModalData.type === 'info',
              'btn-warning': confirmModalData.type === 'warning'
            }"
            @click="handleConfirm"
          >
            {{ confirmModalData.confirmText }}
          </button>
        </div>
      </div>
    </div>
  </div>

  <div class="app-header">
  <h1>IM系统</h1>
  <GlobalSearchButton @navigate-to-message="handleSelectChat" />
  <div class="user-actions">
    <button @click="showSettings" class="settings-btn"><i class="fas fa-cog"></i></button>
    <button @click="logout" class="logout-btn">退出</button>
  </div>
</div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { FileItem, FileStats, FileUploadResult } from '@/types'
import { api } from '@/api/request'
import { contactApi } from '@/api/contact'
import { tagApi } from '@/api/tag'
import { formatFileSize, formatRelativeTime, getCurrentUserId } from '@/utils/helpers'
import ConversationsPanel from '@/components/chat/ConversationsPanel.vue'
import ContactsList from '@/components/chat/ContactsList.vue'
import ChatPanel from '@/components/chat/ChatPanel.vue'
import EmojiPicker from '@/components/chat/EmojiPicker.vue'
import MessageInput from '@/components/chat/MessageInput.vue'
import { messageApi } from '@/api/message'
import { useMessages } from '@/composables/useMessages';
import { useSharedWebSocket } from '@/composables/useWebSocket';
import GroupView from '@/views/GroupView.vue';
import GlobalSearchButton from '@/components/search/GlobalSearchButton.vue';
import GlobalSearch from '@/components/search/GlobalSearch.vue';
import SettingsDialog from '@/components/settings/SettingsDialog.vue';
import MomentView from '@/components/moment/MomentView.vue';
import { getUserSettings } from '@/composables/useUserSettings';
import SystemNotifications from '@/components/SystemNotifications.vue';

interface User {
  id: string
  name: string
  email: string
  nickname?: string
  userIdString?: string
  avatar?: string
  signature?: string
  isOnline?: boolean
  registerTime?: string
  gender?: 'male' | 'female' | 'private'
  phone?: string
  birthday?: string
  location?: string
  occupation?: string
}

interface Chat {
  id: string
  name: string
  avatar?: string
  lastMessage?: string
  lastMessageTime?: string
  unreadCount: number
}

interface Contact {
  id: string
  name: string
  avatar?: string
  signature?: string
  isOnline: boolean
  alias?: string
  tags?: any[]
  friend?: {
    nickname?: string
  }
  nickname?: string
}

// Moment接口定义已移至动态组件

const router = useRouter()

// 响应式数据
const activeTab = ref('chat')
  
// 可用标签页
const tabNames = ['chat', 'contacts', 'moments', 'discover', 'me']
const chatSearchKeyword = ref('')
const contactSearchKeyword = ref('')
// 动态搜索已移至MomentView组件
const userStatus = ref({ emoji: '🚗', text: '在路上' })
const showSettingsModal = ref(false)
const settingsDialogVisible = ref(false)
const showProfileEditModal = ref(false)
const showAddFriendModal = ref(false)
const showUserProfileModal = ref(false)
const showViewUserProfileModal = ref(false)
const viewingUserProfile = ref<any>({})
const isLoading = ref(false)
const errorMessage = ref('')
const showError = ref(false)
const successMessage = ref('')
const showSuccess = ref(false)
const showOptionsMenuVisible = ref(false)
const selectedChat = ref<Chat | null>(null)
const touchTimer = ref<number | null>(null)
const conversationsPanel = ref<InstanceType<typeof ConversationsPanel> | null>(null)
const contactsList = ref<InstanceType<typeof ContactsList> | null>(null)
const activeChatId = ref<string | null>(null)
const notificationUnreadCount = ref(0)

// 聊天相关数据
const messages = ref<any[]>([])
const newMessage = ref('')
const messageContainer = ref<HTMLElement | null>(null)
const currentChatInfo = ref<any>(null)

// 账户与安全相关数据
const showPasswordModal = ref(false)
const showDeviceModal = ref(false)
const showConfirmModal = ref(false)
const confirmModalData = ref({
  title: '',
  message: '',
  confirmText: '确定',
  cancelText: '取消',
  type: 'danger' as 'danger' | 'warning' | 'info',
  onConfirm: () => {}
})
const passwordLoading = ref(false)
const deviceLoading = ref(false)
const passwordErrorMessage = ref('')
const passwordSuccessMessage = ref('')

// 密码表单
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 设备列表和统计
const deviceList = ref<any[]>([])
const deviceStats = ref({
  totalDevices: 0,
  onlineDevices: 0,
  currentDevice: null
})

// 编辑个人资料相关数据
const showStatusForm = ref(false)
const statusType = ref('preset')
const customStatus = ref({ emoji: '😊', text: '' })
const selectedDuration = ref('manual')
const avatarInput = ref<HTMLInputElement | null>(null)

// 预设状态选项
const presetStatuses = ref([
  { emoji: '💼', text: '工作中' },
  { emoji: '📞', text: '会议中' },
  { emoji: '📚', text: '学习中' },
  { emoji: '☕', text: '休息中' },
  { emoji: '⚡', text: '忙碌中' },
  { emoji: '🚗', text: '外出中' },
  { emoji: '🍽️', text: '用餐中' },
  { emoji: '🎮', text: '游戏中' },
  { emoji: '🏃', text: '运动中' },
  { emoji: '🎵', text: '听音乐' },
  { emoji: '😴', text: '睡觉中' },
  { emoji: '🎬', text: '看电影' }
])

// 状态有效期选项
const statusDurations = ref([
  { value: '30s', label: '30秒(测试)' },
  { value: '1h', label: '1小时' },
  { value: '4h', label: '4小时' },
  { value: 'today', label: '今天' },
  { value: 'manual', label: '手动清除' }
])

// 用户信息
const currentUser = ref<User | null>(null)

// 用户设置
const userSettings = ref({
  nickname: '',
  signature: '',
  messageNotification: true,
  soundNotification: true,
  vibrationNotification: true,
  fontSize: 'medium' as 'small' | 'medium' | 'large',
  sendMethod: 'enter' as 'enter' | 'ctrl-enter',
  autoDownloadImages: true,
  allowStrangerView: true,
  showOnlineStatus: true
})

// 用户资料
const userProfile = ref({
  name: '',
  signature: '',
  avatar: '',
  gender: undefined as 'male' | 'female' | 'private' | undefined,
  birthday: '',
  email: '',
  phone: '',
  location: '',
  occupation: '',
  userIdString: '',
  province: '',
  city: '',
  address: '',
  allowStrangerView: true,
  showOnlineStatus: true,
  statusText: '',
  statusEmoji: '',
  statusExpiry: ''
})

// 聊天列表
const chats = ref<Chat[]>([])

// 联系人列表
const contacts = ref<Contact[]>([])

// 动态模块相关状态已移至MomentView组件

// 导航标签页
const navigationTabs = ref([
  { key: 'chat', label: '会话', icon: 'icon-chat', badge: 0 },
  { key: 'contacts', label: '联系人', icon: 'icon-contacts', badge: 0 },
  { key: 'moments', label: '朋友圈', icon: 'icon-moments', badge: 0 },
  { key: 'profile', label: '我', icon: 'icon-profile', badge: 0 }
])

// 计算属性
const filteredChats = computed(() => {
  if (!chatSearchKeyword.value) return chats.value
  return chats.value.filter(chat => 
    chat.name.toLowerCase().includes(chatSearchKeyword.value.toLowerCase()) ||
    (chat.lastMessage && chat.lastMessage.toLowerCase().includes(chatSearchKeyword.value.toLowerCase()))
  )
})

const filteredContacts = computed(() => {
  if (!contactSearchKeyword.value) return contacts.value
  return contacts.value.filter(contact => 
    contact.name.toLowerCase().includes(contactSearchKeyword.value.toLowerCase()) ||
    (contact.signature && contact.signature.toLowerCase().includes(contactSearchKeyword.value.toLowerCase()))
  )
})

// 动态过滤逻辑已移至MomentView组件

// 方法
const getAvatarText = (name: string | undefined): string => {
  if (!name) return 'U'
  return name.charAt(0).toUpperCase()
  }

// 这些方法已在其他位置定义，在此引用

// 格式化会话时间
const formatConversationTime = (time: string | undefined): string => {
  if (!time) return ''
  
  const messageTime = new Date(time)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000)
  const oneWeekAgo = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000)
  
  if (messageTime >= today) {
    // 今天：显示时间
    return messageTime.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (messageTime >= yesterday) {
    // 昨天
    return '昨天'
  } else if (messageTime >= oneWeekAgo) {
    // 一周内：显示"X天前"
    const daysAgo = Math.floor((today.getTime() - messageTime.getTime()) / (24 * 60 * 60 * 1000))
    return `${daysAgo}天前`
  } else {
    // 一周前：显示日期
    return messageTime.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
  }
}

// 获取关系状态文本
const getRelationshipText = (status: string): string => {
  switch (status) {
    case '好友':
      return '已是好友'
    case '已发送请求':
      return '已发送请求'
    case '待处理请求':
      return '待处理请求'
    case '已屏蔽':
      return '已屏蔽'
    case '陌生人':
      return '可以添加'
    case '自己':
      return '这是您自己'
    default:
      return '未知状态'
  }
}

// 发送好友请求
const sendFriendRequest = async (userId: string, userName: string) => {
  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }

    // 获取当前用户ID
    const currentUserId = await getCurrentUserId()
    
    const response = await fetch('/api/contact-requests', {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        requesterId: currentUserId,
        recipientId: parseInt(userId),
        verificationMessage: `我是${currentUser.value?.name || '用户'}，希望能成为好友`
      })
    })

    if (response.status === 401) {
      showErrorMessage('登录已过期，请重新登录')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }

    const data = await response.json()
    if (data.code === 200) {
      showSuccessMessage('好友请求已发送')
      
      // 更新搜索结果中的用户状态
      const user = searchResults.value.find(u => u.userId === userId)
      if (user) {
        user.relationshipStatus = '已发送请求'
      }
    } else {
      showErrorMessage(data.message || '发送好友请求失败')
    }
  } catch (error) {
    console.error('发送好友请求失败:', error)
    showErrorMessage('发送好友请求失败，请稍后重试')
  }
}









// 处理搜索
const handleSearch = () => {
  // 这里可以实现搜索逻辑
  const keyword = chatSearchKeyword.value
  console.log('搜索:', keyword)
  // 可以根据搜索关键词过滤当前列表
  // 实现搜索过滤逻辑
}

// 获取当前会话名称
const getCurrentChatName = (): string => {
  if (!activeChatId.value) return '';
  
  // 如果有当前会话信息
  if (currentChatInfo.value) {
    return currentChatInfo.value.name || '会话';
  }
  
  return '会话';
};

// 判断当前会话是否为群聊
const isCurrentChatGroup = (): boolean => {
  if (!activeChatId.value) return false;
  
  // 如果有当前会话信息
  if (currentChatInfo.value) {
    return currentChatInfo.value.type === 'GROUP' || 
           (currentChatInfo.value.participants && currentChatInfo.value.participants.length > 2);
  }
  
  return false;
};

// 发送消息
const sendMessage = () => {
  if (!newMessage.value.trim() || !activeChatId.value) return;
  
  try {
    // 创建一个临时消息对象
    const tempMessage = {
      id: `temp-${Date.now()}`,
      content: newMessage.value,
      senderId: getCurrentUserId(),
      createdAt: new Date().toISOString(),
      status: 'SENDING',
      isSelf: true
    };
    
    // 添加到消息列表
    messages.value.push(tempMessage);
    
    // 清空输入框
    newMessage.value = '';
    
    // 滚动到底部
    scrollToBottom();
    
    // TODO: 实际发送消息到服务器
    // messageApi.sendMessage({
    //   conversationId: Number(activeChatId.value),
    //   type: 'TEXT',
    //   content: tempMessage.content
    // }).then(response => {
    //   if (response.success) {
    //     // 更新临时消息状态
    //     const index = messages.value.findIndex(m => m.id === tempMessage.id);
    //     if (index !== -1) {
    //       messages.value[index].status = 'SENT';
    //       messages.value[index].id = response.data.id;
    //     }
    //   } else {
    //     // 标记为发送失败
    //     const index = messages.value.findIndex(m => m.id === tempMessage.id);
    //     if (index !== -1) {
    //       messages.value[index].status = 'FAILED';
    //     }
    //   }
    // });
  } catch (error) {
    console.error('发送消息失败:', error);
  }
};

// 加载会话消息
const loadMessages = async (conversationId: string) => {
  if (!conversationId) return;
  
  try {
    // 清空当前消息
    messages.value = [];
    
    // TODO: 实际从服务器加载消息
    // const response = await messageApi.getMessages(Number(conversationId));
    // if (response.success && response.data) {
    //   messages.value = response.data.content.map(msg => ({
    //     ...msg,
    //     isSelf: msg.senderId === getCurrentUserId()
    //   }));
    // }
    
    // 滚动到底部
    scrollToBottom();
  } catch (error) {
    console.error('加载消息失败:', error);
  }
};

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight;
    }
  });
};

// 聚焦消息输入框的辅助函数
const focusMessageInput = () => {
  window.setTimeout(() => {
    try {
      // 滚动到底部
      scrollToBottom();
      
      // 聚焦消息输入框
      const messageInput = document.querySelector('.message-input');
      if (messageInput) {
        (messageInput as HTMLElement).focus();
      }
    } catch (err) {
      console.error('聚焦消息输入框失败:', err);
    }
  }, 300);
};



// 联系人搜索相关
const showSearchResults = ref(false)
const searchResults = ref<any[]>([])
const searchLoading = ref(false)
const contactsLoading = ref(false)
const friendRequestBadge = ref(0)



// 添加好友模态框相关
const friendSearchKeyword = ref('')
const friendSearchResults = ref<any[]>([])
const friendSearchLoading = ref(false)
const friendSearchPerformed = ref(false)

// 好友请求相关
const showFriendRequestModal = ref(false)
const showFriendRequestsTab = ref(false)
const activeRequestTab = ref('received')
const friendRequestsLoading = ref(false)



// 定义好友请求接口
interface FriendRequest {
  requestId: number
  requesterId: number
  requesterUsername: string
  requesterNickname?: string
  requesterAvatarUrl?: string
  requesterUserIdStr?: string
  recipientId: number
  recipientUsername: string
  recipientNickname?: string
  recipientAvatarUrl?: string
  recipientUserIdStr?: string
  verificationMessage?: string
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED'
  statusDescription?: string
  source?: string
  createdAt: string
  processedAt?: string
  canProcess?: boolean
  canWithdraw?: boolean
}

const friendRequests = ref<{
  received: FriendRequest[]
  sent: FriendRequest[]
}>({
  received: [],
  sent: []
})
// 收到的请求（显示所有状态）
const receivedRequests = computed(() => 
  friendRequests.value.received
)
const sentRequests = computed(() => 
  friendRequests.value.sent
)

// 所有状态的请求统计
const allReceivedRequests = computed(() => friendRequests.value.received)
const allSentRequests = computed(() => friendRequests.value.sent)
// 待处理请求数量（只统计收到的待处理请求）
const pendingRequestsCount = computed(() => 
  friendRequests.value.received.filter(request => 
    request.status?.toLowerCase() === 'pending'
  ).length
)

// 已接受的收到请求数量
const acceptedRequestsCount = computed(() => 
  friendRequests.value.received.filter(request => 
    request.status?.toLowerCase() === 'accepted' || request.status?.toLowerCase() === 'approved'
  ).length
)

// 总的已同意请求数量（包括收到的已接受请求和发送的已接受请求）
const totalAcceptedRequestsCount = computed(() => {
  const receivedAccepted = friendRequests.value.received.filter(request => 
    request.status?.toLowerCase() === 'accepted' || request.status?.toLowerCase() === 'approved'
  ).length
  const sentAccepted = friendRequests.value.sent.filter(request => 
    request.status?.toLowerCase() === 'accepted' || request.status?.toLowerCase() === 'approved'
  ).length
  return receivedAccepted + sentAccepted
})

// 已接受的请求列表
const acceptedRequests = computed(() => 
  friendRequests.value.received.filter(request => 
    request.status?.toLowerCase() === 'accepted' || request.status?.toLowerCase() === 'approved'
  )
)

// 标签管理相关
// showTagsTab 已移除，标签管理使用独立模态框
const showTagDetailsTab = ref(false)
const tags = ref<any[]>([])
const tagContacts = ref<any[]>([])

// 联系人操作菜单
const showContactMenuVisible = ref(false)
const selectedContactId = ref<string | null>(null) // 存储为字符串，但实际上是数字的字符串表示
const selectedContactName = ref('')
const contactMenuPosition = ref({ x: 0, y: 0 })

// 模态框状态
const showFriendRequestModalVisible = ref(false)
const showSetAliasModal = ref(false)
const showAssignTagModal = ref(false)
const showDeleteContactModal = ref(false)
const showCreateTagModal = ref(false)
const showEditTagModal = ref(false)
const showDeleteTagModal = ref(false)
const showTagDetailsPage = ref(false)
const showTagsPage = ref(false)
const currentTagName = ref('')

// 表单数据
const friendRequestForm = ref({
  userId: '',
  userName: '',
  message: ''
})
const aliasForm = ref({
  contactId: '',
  contactName: '',
  contactAvatar: '',
  contactNickname: '',
  alias: ''
})
const tagAssignForm = ref({
  contactId: '',
  contactName: '',
  contactAvatar: '',
  selectedTags: [] as string[]
})

const deleteContactForm = ref({
  contactId: '',
  contactName: ''
})

// 标签相关表单数据
const newTagName = ref('')
const createTagForm = ref({
  name: '',
  color: '#667eea'
})
const editTagForm = ref({
  id: '',
  name: '',
  color: '#667eea'
})
const deleteTagForm = ref({
  tagId: '',
  tagName: ''
})

// 预设颜色
const presetColors = ref([
  '#667eea', '#764ba2', '#f093fb', '#4facfe', '#43e97b',
  '#38ef7d', '#ffecd2', '#fcb69f', '#a8edea', '#fed6e3',
  '#ff9a9e', '#fecfef', '#ffeaa7', '#fab1a0', '#fd79a8'
])

const availableTags = computed(() => tags.value)

// 搜索用户
// 添加好友模态框中的搜索方法
const searchFriend = async () => {
  const keyword = friendSearchKeyword.value.trim()
  if (!keyword) {
    showErrorMessage('请输入搜索关键词')
    return
  }
  
  friendSearchLoading.value = true
  friendSearchPerformed.value = true
  
  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }
    
    const response = await fetch(`/api/contact-search/search?keyword=${encodeURIComponent(keyword)}&currentUserId=${await getCurrentUserId()}&page=0&size=20`, {
      method: 'GET',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      }
    })
    
    if (!response.ok) {
      throw new Error('搜索失败')
    }
    
    const data = await response.json()
    if (data.code === 200) {
      friendSearchResults.value = data.data || []
    } else {
      throw new Error(data.message || '搜索失败')
    }
  } catch (error: any) {
    console.error('搜索用户失败:', error)
    showErrorMessage('搜索失败: ' + error.message)
    friendSearchResults.value = []
  } finally {
    friendSearchLoading.value = false
  }
}

// 从模态框发送好友请求
const sendFriendRequestFromModal = async (userId: string, userName: string) => {
  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }
    
    const response = await fetch('/api/contact-requests', {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        requesterId: await getCurrentUserId(),
        recipientId: parseInt(userId),
        verificationMessage: `我是${currentUser.value?.name || '用户'}，希望能成为好友`
      })
    })
    
    const data = await response.json()
    if (data.code === 200) {
      showSuccessMessage('好友请求已发送')
      
      // 更新搜索结果中的用户状态
      const user = friendSearchResults.value.find(u => u.id === userId)
      if (user) {
        user.relationshipStatus = '已发送请求'
      }
    } else {
      showErrorMessage(data.message || '发送好友请求失败')
    }
  } catch (error) {
    console.error('发送好友请求失败:', error)
    showErrorMessage('发送好友请求失败，请稍后重试')
  }
}

// 从模态框查看用户资料
const viewUserProfileFromModal = (userId: string) => {
  viewUserProfile(userId)
  showAddFriendModal.value = false
}

const searchUsers = async () => {
  const keyword = contactSearchKeyword.value.trim()
  if (!keyword) {
    showSearchToast('请输入搜索关键词')
    return
  }

  searchLoading.value = true
  showSearchResults.value = true

  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }

    // 首先获取当前用户信息以获取用户ID
    const profileResponse = await fetch('/api/user/profile', {
      method: 'GET',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      }
    })

    if (profileResponse.status === 401) {
      showErrorMessage('登录已过期，请重新登录')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }

    if (!profileResponse.ok) {
      throw new Error('获取用户信息失败')
    }

    const profileData = await profileResponse.json()
    const currentUserId = (profileData.data || profileData).id

    if (!currentUserId) {
      throw new Error('无法获取当前用户ID')
    }

    // 使用获取到的用户ID进行搜索
    const response = await fetch(`/api/contact-search/search?keyword=${encodeURIComponent(keyword)}&currentUserId=${currentUserId}&page=0&size=20`, {
      method: 'GET',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      }
    })

    if (response.status === 401) {
      showErrorMessage('登录已过期，请重新登录')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }

    if (!response.ok) {
      throw new Error('搜索失败')
    }

    const data = await response.json()
    if (data.code === 200) {
      searchResults.value = data.data || []
    } else {
      throw new Error(data.message || '搜索失败')
    }
  } catch (error: any) {
    console.error('搜索用户失败:', error)
    showErrorMessage('搜索失败: ' + error.message)
  } finally {
    searchLoading.value = false
  }
}

// 关闭搜索
const closeSearch = () => {
  showSearchResults.value = false
  contactSearchKeyword.value = ''
  searchResults.value = []
}

// 显示搜索提示
const showSearchToast = (message: string) => {
  // 创建提示元素
  const toast = document.createElement('div')
  toast.className = 'search-toast'
  toast.innerHTML = `
    <div class="search-toast-icon">🔍</div>
    <div class="search-toast-message">${message}</div>
  `
  
  document.body.appendChild(toast)
  
  // 3秒后移除
  setTimeout(() => {
    toast.classList.add('hide')
    setTimeout(() => {
      if (document.body.contains(toast)) {
        document.body.removeChild(toast)
      }
    }, 300)
  }, 3000)
}

// 查看用户资料
const viewUserProfile = async (userIdOrStr: string | number) => {
  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }

    console.log('查看用户资料:', userIdOrStr)
    
    // 根据参数类型选择不同的API端点
    let apiUrl: string
    if (typeof userIdOrStr === 'string' && userIdOrStr.trim() !== '') {
      // 使用用户ID字符串查询
      apiUrl = `/api/user/public-profile/by-user-id/${userIdOrStr}`
    } else {
      // 使用数字ID查询
      apiUrl = `/api/user/public-profile/${userIdOrStr}`
    }
    
    // 调用API获取用户资料
    const response = await fetch(apiUrl, {
      method: 'GET',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      }
    })

    if (response.status === 401) {
      showErrorMessage('登录已过期，请重新登录')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }

    const data = await response.json()
    if (data.code === 200) {
      console.log('获取用户资料成功:', data.data)
      viewingUserProfile.value = data.data
      showViewUserProfileModal.value = true
    } else {
      showErrorMessage(data.message || '获取用户资料失败')
    }
  } catch (error) {
    console.error('获取用户资料失败:', error)
    showErrorMessage('获取用户资料失败，请稍后重试')
  }
}

// 显示好友请求模态框
const openFriendRequestModal = (user: any) => {
  friendRequestForm.value = {
    userId: user.id,
    userName: user.name,
    message: `我是${currentUser.value?.name || '用户'}，希望能成为好友`
  }
  showFriendRequestModalVisible.value = true
}

// 关闭好友请求模态框
const closeFriendRequestModal = () => {
  showFriendRequestModalVisible.value = false
  friendRequestForm.value = {
    userId: '',
    userName: '',
    message: ''
  }
}

// 处理发送好友请求
const handleSendFriendRequest = async () => {
  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }

    const response = await fetch('/api/contact-requests', {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        requesterId: await getCurrentUserId(),
        recipientId: parseInt(friendRequestForm.value.userId),
        verificationMessage: friendRequestForm.value.message
      })
    })

    if (response.status === 401) {
      showErrorMessage('登录已过期，请重新登录')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }

    const data = await response.json()
    if (data.code === 200) {
      showSuccessMessage('好友请求已发送')
      closeFriendRequestModal()
      
      // 更新搜索结果中的用户状态
      const user = searchResults.value.find(u => u.id === friendRequestForm.value.userId)
      if (user) {
        user.isPending = true
      }
    } else {
      showErrorMessage(data.message || '发送好友请求失败')
    }
  } catch (error) {
    console.error('发送好友请求失败:', error)
    showErrorMessage('发送好友请求失败，请稍后重试')
  }
}

// 打开新的朋友页面
const openNewFriends = () => {
  showFriendRequestsTab.value = true
  activeTab.value = 'contacts'
  loadFriendRequests()
}

// 打开群聊
const openGroupChats = () => {
  activeTab.value = 'groups';
}

// 显示标签管理模态框
const openTagsPage = () => {
  showTagsPage.value = true
  loadTags()
}

// 加载好友请求
const loadFriendRequests = async () => {
  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }

    console.log('开始加载好友请求，token:', token ? '存在' : '不存在')

    // 使用统一的API调用方式
    const [receivedResponse, sentResponse] = await Promise.all([
      api.get('/contact-requests/received'),
      api.get('/contact-requests/sent')
    ])

    console.log('收到的好友请求数据:', receivedResponse)
    console.log('发送的好友请求数据:', sentResponse)
    
    // 详细检查数据结构
    if (receivedResponse.data && receivedResponse.data.length > 0) {
      console.log('收到请求的第一个数据项:', receivedResponse.data[0])
      console.log('收到请求的状态字段:', receivedResponse.data.map((r: any) => r.status))
    }
    if (sentResponse.data && sentResponse.data.length > 0) {
      console.log('发送请求的第一个数据项:', sentResponse.data[0])
      console.log('发送请求的状态字段:', sentResponse.data.map((r: any) => r.status))
    }

    if (receivedResponse.code === 200) {
      friendRequests.value.received = receivedResponse.data || []
      friendRequestBadge.value = friendRequests.value.received.filter(request => 
        request.status?.toLowerCase() === 'pending'
      ).length
      console.log('设置收到的请求数量:', friendRequests.value.received.length)
      console.log('待处理的请求数量:', friendRequestBadge.value)
    } else {
      console.error('获取收到的请求失败:', receivedResponse.message)
    }

    if (sentResponse.code === 200) {
      friendRequests.value.sent = sentResponse.data || []
      console.log('设置发送的请求数量:', friendRequests.value.sent.length)
    } else {
      console.error('获取发送的请求失败:', sentResponse.message)
    }
  } catch (error) {
    console.error('加载好友请求失败:', error)
    showErrorMessage('加载好友请求失败')
  }
}

// 处理好友请求
const handleFriendRequest = async (requestId: number, action: 'approve' | 'reject') => {
  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }

    // 获取当前用户ID作为recipientId
    const currentUserId = await getCurrentUserId()
    
    const response = await fetch(`/api/contact-requests/${requestId}/${action === 'approve' ? 'accept' : 'reject'}?recipientId=${currentUserId}`, {
      method: 'PUT',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      }
    })

    const data = await response.json()
    if (data.code === 200) {
      showSuccessMessage(action === 'approve' ? '已同意好友请求' : '已拒绝好友请求')
      loadFriendRequests() // 重新加载请求列表
      if (action === 'approve') {
        loadContactsList() // 重新加载联系人列表
      }
    } else {
      showErrorMessage(data.message || '操作失败')
    }
  } catch (error) {
    console.error('处理好友请求失败:', error)
    showErrorMessage('操作失败，请稍后重试')
  }
}

// 取消好友请求
const cancelFriendRequest = async (requestId: number) => {
  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }

    // 获取当前用户ID作为requesterId
    const currentUserId = await getCurrentUserId()
    
    const response = await fetch(`/api/contact-requests/${requestId}?requesterId=${currentUserId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      }
    })

    const data = await response.json()
    if (data.code === 200) {
      showSuccessMessage('已取消好友请求')
      loadFriendRequests() // 重新加载请求列表
    } else {
      showErrorMessage(data.message || '取消失败')
    }
  } catch (error) {
    console.error('取消好友请求失败:', error)
    showErrorMessage('取消失败，请稍后重试')
  }
}

// 从好友请求页面返回联系人页面
const backToContactsFromFriendRequests = () => {
  showFriendRequestsTab.value = false
}

// 刷新好友请求
const refreshFriendRequests = async () => {
  friendRequestsLoading.value = true
  try {
    await loadFriendRequests()
  } finally {
    friendRequestsLoading.value = false
  }
}

// 获取请求状态文本
const getRequestStatusText = (status: string) => {
  switch (status) {
    case 'pending':
      return '等待确认'
    case 'approved':
      return '已同意'
    case 'rejected':
      return '已拒绝'
    default:
      return '未知状态'
  }
}

// 获取状态样式类名
const getStatusClass = (status: string) => {
  switch (status?.toLowerCase()) {
    case 'pending':
      return 'pending'
    case 'accepted':
    case 'approved':
      return 'accepted'
    case 'rejected':
      return 'rejected'
    case 'expired':
      return 'expired'
    default:
      return 'pending'
  }
}

// 显示联系人菜单
const showContactMenu = (event: MouseEvent, contactId: string | number, contactName: string) => {
  // 计算菜单位置，确保向上弹出
  const menuHeight = 200; // 菜单的大致高度，增大以确保足够空间
  const windowHeight = window.innerHeight;
  let yPos = event.clientY;
  
  console.log('菜单位置计算 - 窗口高度:', windowHeight, '点击位置Y:', yPos);
  
  // 强制向上弹出菜单，特别是在屏幕底部时
  yPos = yPos - menuHeight;
  
  // 确保不会超出顶部
  if (yPos < 10) yPos = 10;
  
  console.log('调整后的菜单Y位置:', yPos);
  
  // 保存菜单位置
  contactMenuPosition.value = {
    x: event.clientX,
    y: yPos
  }
  
  // 显示菜单
  selectedContactId.value = String(contactId);
  selectedContactName.value = contactName;
  showContactMenuVisible.value = true;
}

// 隐藏联系人菜单
const hideContactMenu = () => {
  showContactMenuVisible.value = false
  selectedContactId.value = null
  selectedContactName.value = ''
}

// 获取选中联系人的头像
const getSelectedContactAvatar = () => {
  if (!selectedContactId.value) return ''
  
  // 将字符串ID转换为数字进行比较
  const numericId = parseInt(selectedContactId.value)
  if (isNaN(numericId)) return ''
  
  const contact = contacts.value.find(c => Number(c.id) === numericId)
  console.log('获取选中联系人头像, ID:', numericId, '找到联系人:', !!contact)
  return contact?.avatar || ''
}

// 获取选中联系人的显示名称
const getSelectedContactDisplayName = () => {
  if (!selectedContactId.value) return tagAssignForm.value.contactName
  
  // 将字符串ID转换为数字进行比较
  const numericId = parseInt(selectedContactId.value)
  if (isNaN(numericId)) return tagAssignForm.value.contactName
  
  const contact = contacts.value.find(c => Number(c.id) === numericId)
  console.log('获取选中联系人显示名称, ID:', numericId, '找到联系人:', !!contact)
  return contact?.alias || contact?.friend?.nickname || contact?.nickname || tagAssignForm.value.contactName
}

// 获取选中联系人的备注
const getSelectedContactAlias = () => {
  if (!selectedContactId.value) return ''
  
  // 将字符串ID转换为数字进行比较
  const numericId = parseInt(selectedContactId.value)
  if (isNaN(numericId)) return ''
  
  const contact = contacts.value.find(c => Number(c.id) === numericId)
  console.log('获取选中联系人备注, ID:', numericId, '找到联系人:', !!contact)
  return contact?.alias || ''
}

// 发送消息给联系人
const openContact = async (contactId: string) => {
  // 实现打开与联系人的聊天
  console.log('打开联系人聊天:', contactId)
  hideContactMenu()
  
  // 查找联系人对象
  const contact = contacts.value.find(c => c.id === contactId)
  if (contact) {
    // 使用现有的openContactChat函数
    await openContactChat(contact)
  } else {
    // 如果找不到联系人对象，创建一个简单的对象
    const simpleContact = {
      id: contactId,
      name: '联系人',
      isOnline: false
    }
    await openContactChat(simpleContact)
  }
}

// 设置联系人备注
const setContactAlias = (contactId: string | number, contactName: string) => {
  // 确保contactId是数字类型
  let numericContactId: number;
  
  // 更严格的检查contactId
  if (contactId === undefined || contactId === null) {
    console.error('无效的联系人ID: undefined');
    showErrorMessage('无效的联系人ID: undefined');
    return;
  }
  
  if (typeof contactId === 'string') {
    // 尝试移除空格并解析
    const trimmedId = contactId.trim();
    numericContactId = parseInt(trimmedId);
    if (isNaN(numericContactId) || trimmedId === '') {
      console.error('无效的联系人ID:', contactId);
      showErrorMessage('无效的联系人ID');
      return;
    }
  } else if (typeof contactId === 'number') {
    numericContactId = contactId;
    if (isNaN(numericContactId) || numericContactId <= 0) {
      console.error('无效的联系人ID值:', contactId);
      showErrorMessage('无效的联系人ID');
      return;
    }
  } else {
    console.error('无效的联系人ID类型:', typeof contactId);
    showErrorMessage('无效的联系人ID类型');
    return;
  }
  
  console.log('设置联系人备注, 联系人ID:', numericContactId, '类型:', typeof numericContactId);
  
  // 使用数字ID查找联系人
  const contact = contacts.value.find(c => Number(c.id) === numericContactId);
  
  aliasForm.value = {
    contactId: String(numericContactId), // 存储为字符串，但确保是有效的数字
    contactName,
    contactAvatar: contact?.avatar || '',
    contactNickname: contact?.friend?.nickname || contact?.nickname || '',
    alias: contact?.alias || ''
  };
  showSetAliasModal.value = true;
  hideContactMenu();
  
  // 自动聚焦到输入框
  nextTick(() => {
    const aliasInput = document.querySelector('.alias-input') as HTMLInputElement;
    if (aliasInput) {
      aliasInput.focus();
      aliasInput.select();
    }
  });
}

// 更新联系人备注
const updateContactAlias = async () => {
  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }

    const currentUserId = await getCurrentUserId()
    if (!currentUserId) {
      showErrorMessage('获取用户信息失败')
      return
    }

    // 确保contactId是有效的数字
    const contactId = parseInt(aliasForm.value.contactId)
    if (isNaN(contactId) || contactId <= 0) {
      console.error('无效的联系人ID:', aliasForm.value.contactId)
      showErrorMessage('无效的联系人ID')
      return
    }

    console.log('设置联系人备注:', {
      contactId: contactId,
      alias: aliasForm.value.alias,
      currentUserId: currentUserId
    })

    const response = await contactApi.setContactAlias(
      contactId,
      { alias: aliasForm.value.alias },
      currentUserId
    )

    if (response.code === 200) {
      // 立即更新本地联系人数据，确保响应式更新
      const contactIndex = contacts.value.findIndex(c => c.id === aliasForm.value.contactId)
      if (contactIndex !== -1 && contacts.value[contactIndex]) {
        console.log('更新联系人备注:', {
          contactId: aliasForm.value.contactId,
          oldAlias: contacts.value[contactIndex].alias,
          newAlias: aliasForm.value.alias
        })
        
        // 使用Vue的响应式更新方式，创建新对象确保触发响应式更新
        contacts.value[contactIndex] = {
          ...contacts.value[contactIndex],
          alias: aliasForm.value.alias
        }
        
        // 强制触发响应式更新
        nextTick(() => {
          console.log('备注更新完成，当前联系人数据:', contacts.value[contactIndex])
        })
      } else {
        console.warn('未找到要更新的联系人:', aliasForm.value.contactId)
      }
      
      showSuccessMessage('备注修改成功')
      showSetAliasModal.value = false
    } else {
      showErrorMessage(response.message || '修改备注失败')
    }
  } catch (error) {
    console.error('修改备注失败:', error)
    showErrorMessage('修改备注失败，请稍后重试')
  }
}

// 分配标签给联系人
const openAssignTagModal = (contactId: string | number, contactName: string, contactAvatar?: string) => {
  // 确保contactId是数字类型
  let numericContactId: number;
  
  // 更严格的检查contactId
  if (contactId === undefined || contactId === null) {
    console.error('无效的联系人ID: undefined');
    showErrorMessage('无效的联系人ID: undefined');
    return;
  }
  
  if (typeof contactId === 'string') {
    // 尝试移除空格并解析
    const trimmedId = contactId.trim();
    numericContactId = parseInt(trimmedId);
    if (isNaN(numericContactId) || trimmedId === '') {
      console.error('无效的联系人ID:', contactId);
      showErrorMessage('无效的联系人ID');
      return;
    }
  } else if (typeof contactId === 'number') {
    numericContactId = contactId;
    if (isNaN(numericContactId) || numericContactId <= 0) {
      console.error('无效的联系人ID值:', contactId);
      showErrorMessage('无效的联系人ID');
      return;
    }
  } else {
    console.error('无效的联系人ID类型:', typeof contactId);
    showErrorMessage('无效的联系人ID类型');
    return;
  }
  
  console.log('分配标签给联系人, 联系人ID:', numericContactId, '类型:', typeof numericContactId);
  
  tagAssignForm.value = {
    contactId: String(numericContactId), // 存储为字符串，但确保是有效的数字
    contactName,
    contactAvatar: contactAvatar || '',
    selectedTags: []
  };
  showAssignTagModal.value = true;
  loadTagsForAssign(String(numericContactId));
  hideContactMenu();
}

// 关闭分配标签模态框
const closeAssignTagModal = () => {
  showAssignTagModal.value = false
  tagAssignForm.value = {
    contactId: '',
    contactName: '',
    contactAvatar: '',
    selectedTags: []
  }
}

// 切换标签选择状态（单选模式）
const toggleTagSelection = (tagId: string | number) => {
  const tagIdStr = String(tagId) // 统一转换为字符串
  const currentSelected = tagAssignForm.value.selectedTags
  
  console.log('切换标签选择:', {
    tagId,
    tagIdStr,
    currentSelected,
    isSelected: currentSelected.includes(tagIdStr)
  })
  
  if (currentSelected.includes(tagIdStr)) {
    // 如果已选中，则取消选择（单选模式下清空选择）
    tagAssignForm.value.selectedTags = []
  } else {
    // 如果未选中，则设置为唯一选择（单选模式）
    tagAssignForm.value.selectedTags = [tagIdStr]
  }
  
  console.log('标签选择状态更新后:', {
    selectedTags: tagAssignForm.value.selectedTags
  })
}

// 加载标签用于分配
const loadTagsForAssign = async (contactId: string) => {
  try {
    const token = getAuthToken()
    if (!token) return

    // 确保contactId是有效的数字
    const numericContactId = parseInt(contactId)
    if (isNaN(numericContactId) || numericContactId <= 0) {
      console.error('无效的联系人ID:', contactId)
      showErrorMessage('无效的联系人ID')
      return
    }

    console.log('加载标签用于分配, 联系人ID:', numericContactId)

    // 并行加载所有标签和联系人当前标签
    const [allTagsResponse, contactTagsResponse] = await Promise.all([
      fetch('/api/tags', {
        headers: { 'Authorization': 'Bearer ' + token }
      }),
      fetch(`/api/contacts/${numericContactId}/tags`, {
        headers: { 'Authorization': 'Bearer ' + token }
      })
    ])

    if (allTagsResponse.ok) {
      const allTagsData = await allTagsResponse.json()
      console.log('所有标签响应:', allTagsData)
      // 兼容不同的响应格式
      if (allTagsData.success || allTagsData.code === 200) {
        tags.value = allTagsData.data || []
        console.log('加载的所有标签:', tags.value)
      }
    }

    if (contactTagsResponse.ok) {
        const contactTagsData = await contactTagsResponse.json()
        console.log('联系人标签响应:', contactTagsData)
        // 兼容不同的响应格式
        if (contactTagsData.success || contactTagsData.code === 200) {
          const contactTags = contactTagsData.data || []
          // 确保使用正确的字段名（tagId 或 id）
          tagAssignForm.value.selectedTags = contactTags.map((tag: any) => {
            const id = tag.tagId || tag.id
            console.log('处理联系人标签:', { tag, id, stringId: String(id) })
            return String(id)
          })
          console.log('联系人当前标签:', contactTags)
          console.log('选中的标签ID:', tagAssignForm.value.selectedTags)
        }
      }
  } catch (error) {
    console.error('加载标签失败:', error)
  }
}

// 保存标签分配
const saveTagAssignment = async () => {
  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }

    // 将标签ID转换为数字类型，因为后端可能期望数字
    const tagIds = tagAssignForm.value.selectedTags.map(id => {
      const numId = parseInt(id)
      return isNaN(numId) ? id : numId
    })
    
    console.log('准备发送的标签ID:', {
      originalIds: tagAssignForm.value.selectedTags,
      convertedIds: tagIds,
      contactId: tagAssignForm.value.contactId
    })

    // 确保contactId是有效的数字
    const contactId = parseInt(tagAssignForm.value.contactId)
    if (isNaN(contactId) || contactId <= 0) {
      console.error('无效的联系人ID:', tagAssignForm.value.contactId)
      showErrorMessage('无效的联系人ID')
      return
    }

    const currentUserId = await getCurrentUserId()
    if (!currentUserId) {
      showErrorMessage('获取用户信息失败')
      return
    }

    // 使用contactApi而不是直接fetch
    const response = await contactApi.assignContactTags(
      contactId,
      { tagIds: tagIds.filter(id => typeof id === 'number') as number[] },
      currentUserId
    )

    console.log('标签分配响应:', response)
    
    // 兼容不同的响应格式
    if (response.success || response.code === 200) {
      showSuccessMessage('标签分配成功')
      showAssignTagModal.value = false
      loadContactsList() // 重新加载联系人列表
    } else {
      showErrorMessage(response.message || '标签分配失败')
    }
  } catch (error) {
    console.error('标签分配失败:', error)
    showErrorMessage('标签分配失败，请稍后重试')
  }
}

// 处理编辑联系人备注
const handleEditAlias = (contact: any) => {
  if (!contact || !contact.id) {
    console.error('无效的联系人数据:', contact);
    showErrorMessage('无效的联系人数据');
    return;
  }
  
  console.log('处理编辑联系人备注:', contact);
  // 确保联系人ID是有效的数字
  const contactId = Number(contact.id);
  if (isNaN(contactId) || contactId <= 0) {
    console.error('无效的联系人ID:', contact.id);
    showErrorMessage('无效的联系人ID');
    return;
  }
  
  // 调用设置备注函数
  setContactAlias(contactId, contact.name || '未知联系人');
};

// 处理管理联系人标签
const handleManageTags = (contact: any) => {
  if (!contact || !contact.id) {
    console.error('无效的联系人数据:', contact);
    showErrorMessage('无效的联系人数据');
    return;
  }
  
  console.log('处理管理联系人标签:', contact);
  // 确保联系人ID是有效的数字
  const contactId = Number(contact.id);
  if (isNaN(contactId) || contactId <= 0) {
    console.error('无效的联系人ID:', contact.id);
    showErrorMessage('无效的联系人ID');
    return;
  }
  
  // 调用分配标签函数
  openAssignTagModal(contactId, contact.name || '未知联系人', contact.avatarUrl || contact.avatar);
};

// 处理删除联系人
const handleDeleteContact = (contact: any) => {
  if (!contact || !contact.id) {
    console.error('无效的联系人数据:', contact);
    showErrorMessage('无效的联系人数据');
    return;
  }
  
  console.log('处理删除联系人:', contact);
  // 确保联系人ID是有效的数字
  const contactId = Number(contact.id);
  if (isNaN(contactId) || contactId <= 0) {
    console.error('无效的联系人ID:', contact.id);
    showErrorMessage('无效的联系人ID');
    return;
  }
  
  // 调用确认删除联系人函数
  confirmDeleteContact(String(contactId), contact.name || '未知联系人');
};

// 确认删除联系人
const confirmDeleteContact = (contactId: string, contactName: string) => {
  deleteContactForm.value = {
    contactId,
    contactName
  }
  showDeleteContactModal.value = true
  hideContactMenu()
}

// 删除联系人
const deleteContact = async () => {
  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }

    // 获取当前用户信息
    const userInfo = getUserInfo()
    if (!userInfo || !userInfo.id) {
      showErrorMessage('无法获取用户信息，请重新登录')
      return
    }

    const response = await fetch(`/api/contacts/${deleteContactForm.value.contactId}?userId=${userInfo.id}`, {
      method: 'DELETE',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      }
    })

    if (response.status === 401) {
      showErrorMessage('登录已过期，请重新登录')
      clearLoginInfo()
      setTimeout(() => {
        window.location.href = '/login'
      }, 2000)
      return
    }

    const data = await response.json()
    if (data.code === 200) {
      showSuccessMessage('好友删除成功')
      showDeleteContactModal.value = false
      
      // 立即从本地列表中移除已删除的联系人，确保UI立即更新
      const contactIndex = contacts.value.findIndex(c => c.id === deleteContactForm.value.contactId)
      if (contactIndex !== -1) {
        contacts.value.splice(contactIndex, 1)
      }
      
      // 然后重新加载联系人列表以确保数据同步
      await nextTick() // 等待DOM更新
      loadContactsList() // 重新加载联系人列表
    } else {
      showErrorMessage(data.message || '删除好友失败')
    }
  } catch (error) {
    console.error('删除好友失败:', error)
    showErrorMessage('删除好友失败，请稍后重试')
  }
}

// 加载标签列表
const loadTags = async () => {
  try {
    const currentUserId = await getCurrentUserId()
    if (!currentUserId) {
      showErrorMessage('请先登录')
      return
    }

    const response = await tagApi.getTags(currentUserId)
    if (response.code === 200) {
      // 添加调试信息
      console.log('加载标签API返回数据:', JSON.stringify(response.data));
      
      // 将后端返回的tagId映射到前端需要的id字段
      tags.value = (response.data || []).map(tag => ({
        ...tag,
        id: tag.tagId // 将tagId映射到id字段
      }))
      
      // 检查标签ID类型和值
      if (tags.value.length > 0) {
        console.log('处理后的标签数据示例:');
        tags.value.forEach(tag => {
          console.log(`标签ID: ${tag.id}, 类型: ${typeof tag.id}, 名称: ${tag.name}`);
        });
      }
    } else {
      showErrorMessage(response.message || '加载标签失败')
    }
  } catch (error) {
    console.error('加载标签失败:', error)
    showErrorMessage('加载标签失败，请稍后重试')
  }
}



// 关闭创建标签模态框
const closeCreateTagModal = () => {
  showCreateTagModal.value = false
  createTagForm.value = {
    name: '',
    color: '#667eea'
  }
}

// 更新字符计数
const updateCharCount = () => {
  // 字符计数在模板中自动计算
}

// 更新创建颜色预览
const updateCreateColorPreview = () => {
  // 颜色预览在模板中自动更新
}

// 选择创建标签预设颜色
const selectCreateColor = (color: string) => {
  createTagForm.value.color = color
}

// 创建标签
const createTag = async () => {
  const name = createTagForm.value.name.trim()
  const color = createTagForm.value.color
  
  // 参数验证
  if (!name) {
    showErrorMessage('标签名称不能为空')
    return
  }
  
  if (name.length > 20) {
    showErrorMessage('标签名称不能超过20个字符')
    return
  }
  
  if (!/^#[0-9A-Fa-f]{6}$/.test(color)) {
    showErrorMessage('颜色格式不正确')
    return
  }

  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }

    // 获取当前用户ID
    const profileResponse = await fetch('/api/user/profile', {
      method: 'GET',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      }
    })

    if (profileResponse.status === 401) {
      showErrorMessage('登录已过期，请重新登录')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }

    if (!profileResponse.ok) {
      throw new Error('获取用户信息失败')
    }

    const profileData = await profileResponse.json()
    const userId = (profileData.data || profileData).id

    const requestBody = {
      userId: userId,
      name: name,
      color: color
    }

    const response = await fetch('/api/tags', {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(requestBody)
    })

    if (response.status === 401) {
      showErrorMessage('登录已过期，请重新登录')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }

    const data = await response.json()
    if (data.success || data.code === 200) {
      showSuccessMessage('标签创建成功')
      closeCreateTagModal()
      loadTags() // 重新加载标签列表
      
      // 如果分配标签模态框是打开的，刷新其标签列表
      if (tagAssignForm.value.contactId) {
        loadTagsForAssign(tagAssignForm.value.contactId)
      }
    } else {
      throw new Error(data.message || '创建标签失败')
    }
  } catch (error: any) {
    console.error('创建标签失败:', error)
    showErrorMessage('创建标签失败: ' + error.message)
  }
}



// 编辑标签
const editTag = (tag: any) => {
  console.log('编辑标签，原始tag对象:', tag);
  console.log('标签ID类型:', typeof tag.id, '标签ID值:', tag.id);
  
  editTagForm.value = {
    id: tag.id,
    name: tag.name,
    color: tag.color || '#667eea'
  }
  console.log('编辑表单数据:', editTagForm.value);
  showEditTagModal.value = true
}

// 更新标签
const updateTag = async () => {
  if (!editTagForm.value.name.trim()) {
    showErrorMessage('请输入标签名称')
    return
  }

  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }

    // 确保ID存在且有效
    console.log('更新标签前，ID类型:', typeof editTagForm.value.id, '标签ID值:', editTagForm.value.id);
    
    if (!editTagForm.value.id) {
      showErrorMessage('标签ID无效')
      return
    }
    
    // 尝试转换为数字类型
    const tagId = Number(editTagForm.value.id);
    console.log('转换后的标签ID:', tagId, '是否为有效数字:', !isNaN(tagId));

    const response = await fetch(`/api/tags/${tagId}`, {
      method: 'PUT',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        name: editTagForm.value.name,
        color: editTagForm.value.color
      })
    })

    const data = await response.json()
    if (data.code === 200) {
      showSuccessMessage('标签更新成功')
      showEditTagModal.value = false
      loadTags() // 重新加载标签列表
    } else {
      showErrorMessage(data.message || '更新标签失败')
    }
  } catch (error) {
    console.error('更新标签失败:', error)
    showErrorMessage('更新标签失败，请稍后重试')
  }
}

// 确认删除标签
const confirmDeleteTag = (tagId: string, tagName: string) => {
  console.log('确认删除标签，ID类型:', typeof tagId, '标签ID值:', tagId);
  
  deleteTagForm.value = {
    tagId,
    tagName
  }
  console.log('删除表单数据:', deleteTagForm.value);
  showDeleteTagModal.value = true
}

// 获取标签名称
const getTagName = (tag: any): string => {
  if (typeof tag === 'object') {
    return tag.name || '未命名';
  } else {
    // 如果标签是ID，尝试在所有标签中查找对应的标签对象
    const tagObj = tags.value.find(t => t.id === tag || t.tagId === tag);
    return tagObj ? tagObj.name : '未命名';
  }
}

// 获取标签颜色
const getTagColor = (tag: any): string => {
  if (typeof tag === 'object') {
    return tag.color || '#667eea';
  } else {
    // 如果标签是ID，尝试在所有标签中查找对应的标签对象
    const tagObj = tags.value.find(t => t.id === tag || t.tagId === tag);
    return tagObj ? tagObj.color : '#667eea';
  }
}

// 删除标签
const deleteTag = async () => {
  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }

    // 确保ID存在且有效
    console.log('删除标签前，ID类型:', typeof deleteTagForm.value.tagId, '标签ID值:', deleteTagForm.value.tagId);
    
    if (!deleteTagForm.value.tagId) {
      showErrorMessage('标签ID无效')
      return
    }
    
    // 尝试转换为数字类型
    const tagId = Number(deleteTagForm.value.tagId);
    console.log('转换后的标签ID:', tagId, '是否为有效数字:', !isNaN(tagId));

    const response = await fetch(`/api/tags/${tagId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      }
    })

    const data = await response.json()
    if (data.code === 200) {
      showSuccessMessage('标签删除成功')
      showDeleteTagModal.value = false
      loadTags() // 重新加载标签列表
      loadContactsList() // 重新加载联系人列表
    } else {
      showErrorMessage(data.message || '删除标签失败')
    }
  } catch (error) {
    console.error('删除标签失败:', error)
    showErrorMessage('删除标签失败，请稍后重试')
  }
}

// 查看标签下的联系人
const viewTagContacts = (tag: any) => {
  console.log('查看标签联系人，标签对象:', tag);
  currentTagName.value = tag.name
  showTagDetailsPage.value = true
  
  // 使用正确的标签ID (tagId或id)
  const tagId = tag.tagId || tag.id;
  console.log('使用的标签ID:', tagId, '类型:', typeof tagId);
  
  // 同步标签联系人数据
  syncTagContactsData(tagId);
  
  // 加载标签联系人
  loadContactsByTag(tagId)
}

// 同步标签联系人数据
const syncTagContactsData = (tagId: number | string) => {
  try {
    // 确保tagId是数字类型
    const numericTagId = typeof tagId === 'string' ? parseInt(tagId) : tagId;
    
    if (isNaN(numericTagId) || numericTagId <= 0) {
      console.error('同步标签联系人数据失败: 无效的标签ID', tagId);
      return;
    }
    
    // 从contacts中提取具有该标签的联系人
    const contactsWithTag = contacts.value.filter(contact => {
      if (Array.isArray(contact.tags)) {
        // 检查联系人的tags数组是否包含当前标签ID
        return contact.tags.some(tag => {
          // 标签可能是对象或者数字ID
          const tagIdToCompare = typeof tag === 'object' ? tag.id : tag;
          return Number(tagIdToCompare) === numericTagId;
        });
      }
      return false;
    });
    
    console.log(`同步了${contactsWithTag.length}个具有标签ID ${numericTagId} 的联系人`);
    
    // 将筛选后的联系人保存到localStorage中的特定键
    try {
      localStorage.setItem(`tag_contacts_${numericTagId}`, JSON.stringify(contactsWithTag));
    } catch (e) {
      console.warn('保存标签联系人数据到localStorage失败:', e);
    }
  } catch (error) {
    console.error('同步标签联系人数据失败:', error);
  }
}

// 加载标签下的联系人
const loadContactsByTag = async (tagId: number | string) => {
  try {
    console.log('开始加载标签联系人，标签ID:', tagId, '类型:', typeof tagId);
    
    if (!tagId) {
      console.error('标签ID无效');
      showErrorMessage('标签ID无效');
      tagContacts.value = []; // 清空联系人列表
      return;
    }

    // 确保tagId是数字类型
    const numericTagId = typeof tagId === 'string' ? parseInt(tagId) : tagId;
    
    if (isNaN(numericTagId) || numericTagId <= 0) {
      console.error('标签ID格式无效:', tagId);
      showErrorMessage('标签ID格式无效');
      tagContacts.value = []; // 清空联系人列表
      return;
    }
    
    console.log('发起API请求，获取标签联系人...');
    // 使用tagApi获取标签下的联系人
    const response = await tagApi.getTagContacts(numericTagId);
    
    console.log('标签联系人API响应:', response);
    
    if (response && (response.code === 200 || response.success)) {
      console.log('获取标签联系人成功:', response.data);
      
      // 确保响应数据是数组
      if (Array.isArray(response.data)) {
        // 先检查数组是否为空
        if (response.data.length === 0) {
          console.log('标签下没有联系人');
          tagContacts.value = [];
          return;
        }
        
        // 处理联系人数据，适应不同的API返回格式
        tagContacts.value = response.data.map((contact: any) => {
          // 提取联系人ID - 适应不同的字段名
          const contactId = contact.friendId || contact.id || contact.contactId || 0;
          const numericContactId = typeof contactId === 'string' ? parseInt(contactId) : contactId;
          
          // 提取联系人名称 - 优先使用别名，然后是昵称，最后是用户名
          const name = contact.alias || contact.nickname || contact.friendNickname || 
                      contact.friendUsername || contact.username || contact.name || '未知用户';
          
          // 提取头像URL - 适应不同的字段名
          const avatarUrl = contact.avatarUrl || contact.friendAvatarUrl || contact.avatar || '';
          
          // 提取签名 - 适应不同的字段名
          const signature = contact.signature || contact.friendSignature || '';
          
          // 提取邮箱 - 适应不同的字段名
          const email = contact.email || contact.friendEmail || '';
          
          // 构建标准化的联系人对象
          return {
            id: isNaN(numericContactId) ? 0 : numericContactId,
            name: name,
            avatarUrl: avatarUrl,
            signature: signature,
            email: email,
            friend: contact.friend || {
              id: numericContactId,
              nickname: contact.friendNickname || contact.nickname || name,
              avatarUrl: avatarUrl,
              signature: signature
            }
          };
        }).filter((contact: any) => contact.id > 0); // 过滤掉无效联系人
      } else {
        // 如果不是数组，设置为空数组
        console.warn('API返回的数据不是数组:', typeof response.data, response.data);
        tagContacts.value = [];
      }
      
      console.log('更新后的标签联系人:', tagContacts.value);
    } else {
      console.error('标签联系人响应错误:', response?.message);
      showErrorMessage(response?.message || '加载联系人失败');
      tagContacts.value = []; // 清空联系人列表
    }
  } catch (error: any) {
    console.error('加载标签联系人失败:', error);
    if (error.status) {
      console.error('错误状态码:', error.status, '错误消息:', error.message);
    }
    showErrorMessage(error.message || '加载联系人失败，请稍后重试');
    tagContacts.value = []; // 清空联系人列表
  }
}

// 返回联系人主页


// 加载联系人列表
const loadContactsList = async () => {
  contactsLoading.value = true
  try {
    const currentUserId = await getCurrentUserId()
    if (!currentUserId) {
      showErrorMessage('请先登录')
      return
    }

    const response = await contactApi.getContacts(currentUserId)
    if (response.code === 200) {
      // 转换API返回的Contact数据为本地Contact接口格式
      const apiContacts = response.data || []
      contacts.value = apiContacts.map((contact: any) => {
        // 确保ID是数字
        let friendId = contact.friendId || contact.id
        if (typeof friendId === 'string') {
          friendId = parseInt(friendId)
          if (isNaN(friendId)) {
            console.warn('联系人ID无效:', contact)
            friendId = 0
          }
        } else if (friendId === undefined || friendId === null) {
          console.warn('联系人ID为空:', contact)
          friendId = 0
        } else if (typeof friendId !== 'number') {
          console.warn('联系人ID类型不是数字:', typeof friendId, contact)
          friendId = 0
        }
        
        // 确保ID是有效的数字
        friendId = Number(friendId)
        if (isNaN(friendId) || friendId <= 0) {
          console.warn('处理后联系人ID仍然无效:', friendId, contact)
          friendId = 0
        }
        
        return {
          id: friendId, // 确保ID是数字
          friendId: friendId, // 添加friendId字段，确保是数字
        name: contact.nickname || contact.friendUsername || '',
        avatar: contact.avatarUrl,
          avatarUrl: contact.avatarUrl,
        signature: contact.signature || '',
        isOnline: contact.isOnline || false,
        alias: contact.alias,
        tags: contact.tags,
        friend: {
            id: friendId, // 确保friend.id也是数字
          nickname: contact.nickname,
          avatarUrl: contact.avatarUrl,
          signature: contact.signature
        },
          nickname: contact.nickname,
          email: contact.email || contact.friend?.email || ''
        }
      }).filter((contact: any) => contact.id > 0) // 过滤掉无效的联系人
      
      // 保存联系人数据到localStorage，供标签联系人功能使用
      try {
        localStorage.setItem('contacts', JSON.stringify(contacts.value));
        console.log('联系人数据已保存到localStorage');
      } catch (e) {
        console.warn('保存联系人数据到localStorage失败:', e);
      }
      
      // 打印联系人ID类型，用于调试
      if (contacts.value.length > 0) {
        console.log('联系人ID示例:')
        contacts.value.slice(0, 3).forEach(contact => {
          console.log(`联系人ID: ${contact.id}, 类型: ${typeof contact.id}, 名称: ${contact.name}`)
        })
      } else {
        console.log('联系人列表为空')
      }
    } else {
      showErrorMessage(response.message || '加载联系人失败')
    }
  } catch (error) {
    console.error('加载联系人失败:', error)
    showErrorMessage('加载联系人失败，请稍后重试')
  } finally {
    contactsLoading.value = false
  }
}



// 处理联系人开始聊天事件
const handleContactStartChat = (data: { contact: any, conversationId: number }) => {
  console.log('收到start-chat事件:', data);
  
  if (!data.conversationId) {
    console.error('无效的会话ID');
    showErrorMessage('无法创建会话，请稍后重试');
    return;
  }
  
  // 创建一个符合Chat类型的对象
  const chatObj: any = { 
    id: String(data.conversationId),
    name: data.contact.name || '私聊',
    avatar: data.contact.avatarUrl || data.contact.avatar || '',
    lastMessage: '',
    lastMessageTime: new Date().toISOString(),
    unreadCount: 0
  };
  
  console.log('准备设置活动会话:', chatObj);
  
  // 防止重复处理相同的会话
  if (activeChatId.value === chatObj.id && activeTab.value === 'chat') {
    console.log('会话已经是当前选中的，只刷新会话列表');
    if (conversationsPanel.value) {
      conversationsPanel.value.loadConversations();
    }
    return;
  }
  
  // 切换到聊天标签页
  activeTab.value = 'chat';
  
  // 立即设置当前活动会话ID
  activeChatId.value = chatObj.id;
  
  // 加载会话消息
  loadMessages(chatObj.id);
  
  // 简化逻辑，避免循环调用
  window.setTimeout(() => {
    // 刷新会话列表
    if (conversationsPanel.value) {
      conversationsPanel.value.loadConversations().then(() => {
        // 聚焦消息输入框
        focusMessageInput();
      });
    }
  }, 100);
};

const openContactChat = async (contact: any) => {
  // 实现打开联系人会话逻辑
  console.log('开始打开联系人会话:', contact)
  
  try {
    // 尝试从不同的属性中获取联系人ID
    let rawContactId = contact.id;
    
    // 如果id为undefined或null，尝试从其他属性获取
    if (rawContactId === undefined || rawContactId === null) {
      if (contact.friendId !== undefined) {
        rawContactId = contact.friendId;
        console.log('使用friendId作为联系人ID:', rawContactId);
      } else if (contact.rawData && contact.rawData.id !== undefined) {
        rawContactId = contact.rawData.id;
        console.log('使用rawData.id作为联系人ID:', rawContactId);
      } else if (contact.rawData && contact.rawData.friendId !== undefined) {
        rawContactId = contact.rawData.friendId;
        console.log('使用rawData.friendId作为联系人ID:', rawContactId);
      } else if (contact.friend && contact.friend.id !== undefined) {
        rawContactId = contact.friend.id;
        console.log('使用friend.id作为联系人ID:', rawContactId);
      } else {
        console.error('无法获取有效的联系人ID');
        showErrorMessage('无法获取有效的联系人ID');
        return;
      }
    }
    
    console.log(`联系人ID ${rawContactId}，ID类型:`, typeof rawContactId);
    
    // 获取当前用户ID
    const userId = getCurrentUserId()
    if (!userId) {
      showErrorMessage('请先登录')
    return
  }
  
    // 确保ID是数字类型
    let contactId: number;
    if (typeof rawContactId === 'string') {
      contactId = parseInt(rawContactId, 10);
      if (isNaN(contactId)) {
        throw new Error(`无效的联系人ID: ${rawContactId}`);
      }
    } else if (typeof rawContactId === 'number') {
      contactId = rawContactId;
    } else {
      throw new Error(`无效的联系人ID类型: ${typeof rawContactId}`);
    }
    
    console.log('处理后的联系人ID:', contactId, '类型:', typeof contactId)
    
    // 调用API获取或创建私聊会话
    const response = await messageApi.getOrCreatePrivateConversation(contactId, userId)
    
    if (response.success && response.data) {
      console.log('成功获取或创建私聊会话:', response.data)
      
      // 获取会话ID
      let conversationId: number | undefined
      
      // 处理不同的响应结构
      if (response.data.id) {
        // 直接返回了会话对象
        conversationId = response.data.id
      } else if (response.data.conversation && response.data.conversation.id) {
        // 返回了包装的会话对象
        conversationId = response.data.conversation.id
      } else if (typeof response.data === 'number') {
        // 直接返回了ID
        conversationId = response.data
      }
      
      if (conversationId) {
        console.log('提取到会话ID:', conversationId)
        
        // 创建一个符合Chat类型的对象
        const chatObj: any = { 
          id: String(conversationId),
          name: contact.name || '私聊',
          avatar: contact.avatar || '',
          lastMessage: '',
          lastMessageTime: new Date().toISOString(),
          unreadCount: 0
        };
        
        console.log('准备设置活动会话:', chatObj);
        
        // 防止重复处理相同的会话
        if (activeChatId.value === chatObj.id && activeTab.value === 'chat') {
          console.log('会话已经是当前选中的，只刷新会话列表');
          if (conversationsPanel.value) {
            conversationsPanel.value.loadConversations();
          }
          return;
        }
        
        // 切换到聊天标签页
        activeTab.value = 'chat'
        
        // 立即设置当前活动会话ID
        activeChatId.value = chatObj.id;
        
        // 加载会话消息
        loadMessages(chatObj.id);
        
        // 简化逻辑，避免循环调用
        window.setTimeout(() => {
          if (conversationsPanel.value) {
            // 刷新会话列表
            conversationsPanel.value.loadConversations().then(() => {
              // 聚焦消息输入框
              focusMessageInput()
            })
          }
        }, 100)
      } else {
        console.error('无法从响应中提取会话ID:', response.data);
        showErrorMessage('无法获取会话ID')
      }
    } else {
      showErrorMessage(response.message || '创建会话失败')
    }
  } catch (error: any) {
    console.error('打开联系人会话失败:', error)
    showErrorMessage(error.message || '打开联系人会话失败')
  }
}

// 所有动态相关的逻辑都已经移至MomentView组件

const editProfile = () => {
  // 初始化个人资料数据
  if (currentUser.value) {
    userProfile.value.name = currentUser.value.name || ''
    userProfile.value.signature = currentUser.value.signature || ''
    userProfile.value.avatar = currentUser.value.avatar || ''
    userProfile.value.gender = currentUser.value.gender || undefined
    userProfile.value.phone = currentUser.value.phone || ''
    userProfile.value.email = currentUser.value.email || ''
    userProfile.value.userIdString = currentUser.value.userIdString || ''
    // 从当前用户数据中获取其他字段
    userProfile.value.birthday = currentUser.value.birthday || ''
    userProfile.value.location = currentUser.value.location || ''
    userProfile.value.occupation = currentUser.value.occupation || ''
  }
  showUserProfileModal.value = true
}





// 处理头像加载错误
const handleAvatarError = (event: Event) => {
  const target = event.target as HTMLImageElement;
  if (target) {
    target.style.display = 'none';
  }
}

const generateQRCode = async () => {
  try {
    // 显示加载状态
    showErrorMessage('正在生成二维码名片...')
    
    // 获取JWT token
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }
    
    // 调用后端API生成二维码
    const response = await fetch('/api/user/qrcode', {
      method: 'GET',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      }
    })
    
    // 处理401未授权错误
    if (response.status === 401) {
      showErrorMessage('登录已过期，请重新登录')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }
    
    const data = await response.json()
    if (data.code === 200 && data.data) {
      // 显示二维码模态框
      showQRCodeModal(data.data)
    } else {
      showErrorMessage(data.message || '生成二维码失败')
    }
  } catch (error) {
    console.error('生成二维码失败:', error)
    showErrorMessage('生成二维码失败，请稍后重试')
  }
}





// 账户与安全相关方法
const openAccountSecurity = () => {
  activeTab.value = 'accountSecurity'
}

const openPasswordManagement = () => {
  // 重置表单
  passwordForm.value = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
  passwordErrorMessage.value = ''
  passwordSuccessMessage.value = ''
  showPasswordModal.value = true
}

const closePasswordModal = () => {
  showPasswordModal.value = false
  passwordForm.value = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
  passwordErrorMessage.value = ''
  passwordSuccessMessage.value = ''
}

const changePassword = async () => {
  // 验证输入 - 严格按照index.html的实现逻辑
  if (!passwordForm.value.currentPassword || !passwordForm.value.newPassword || !passwordForm.value.confirmPassword) {
    passwordErrorMessage.value = '❌ 请填写所有字段'
    return
  }
  
  if (passwordForm.value.newPassword.length < 6) {
    passwordErrorMessage.value = '❌ 新密码至少需要6位字符'
    return
  }
  
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    passwordErrorMessage.value = '❌ 两次输入的新密码不一致'
    return
  }

  passwordLoading.value = true
  passwordErrorMessage.value = ''
  passwordSuccessMessage.value = ''

  try {
    const token = getAuthToken()
    if (!token) {
      passwordErrorMessage.value = '❌ 请先登录后再修改密码'
      return
    }

    const response = await fetch('/api/auth/password/change', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      body: JSON.stringify({
        currentPassword: passwordForm.value.currentPassword,
        newPassword: passwordForm.value.newPassword,
        confirmPassword: passwordForm.value.confirmPassword
      })
    })

    const result = await response.json()

    if (response.ok && result.code === 200) {
      passwordSuccessMessage.value = '✅ 密码修改成功！正在跳转到登录页面...'
      
      // 清除所有登录信息
      setTimeout(() => {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('userInfo')
        sessionStorage.removeItem('accessToken')
        sessionStorage.removeItem('refreshToken')
        sessionStorage.removeItem('userInfo')
        
        // 跳转到登录页面
        router.push('/login')
      }, 2000)
    } else {
      let errorMsg = result.message || '密码修改失败，请重试'
      if (response.status === 401) {
        errorMsg = '❌ 登录已过期，请重新登录后再试'
      } else if (response.status === 403) {
        errorMsg = '❌ 当前密码错误，请检查后重试'
      } else {
        errorMsg = '❌ ' + errorMsg
      }
      passwordErrorMessage.value = errorMsg
    }
  } catch (error) {
    console.error('修改密码失败:', error)
    passwordErrorMessage.value = '❌ 网络错误，请重试'
  } finally {
    passwordLoading.value = false
  }
}

const openLoginDeviceManagement = () => {
  showDeviceModal.value = true
  loadDeviceList()
}

const closeDeviceModal = () => {
  showDeviceModal.value = false
}

// 确认模态框相关函数
const showConfirm = (options: {
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  type?: 'danger' | 'warning' | 'info'
  onConfirm: () => void
}) => {
  confirmModalData.value = {
    title: options.title,
    message: options.message,
    confirmText: options.confirmText || '确定',
    cancelText: options.cancelText || '取消',
    type: options.type || 'danger',
    onConfirm: options.onConfirm
  }
  showConfirmModal.value = true
}

const closeConfirmModal = () => {
  showConfirmModal.value = false
}

const handleConfirm = () => {
  confirmModalData.value.onConfirm()
  closeConfirmModal()
}

const loadDeviceList = async () => {
  deviceLoading.value = true
  try {
    const token = getAuthToken()
    if (!token) {
      console.error('未找到认证令牌')
      return
    }

    const response = await fetch('/api/user/devices', {
      method: 'GET',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      }
    })

    // 处理401未授权错误
    if (response.status === 401) {
      showErrorMessage('登录已过期，请重新登录')
      clearLoginInfo()
      setTimeout(() => {
        window.location.href = '/login'
      }, 2000)
      return
    }

    const result = await response.json()
    
    if (response.ok && result.code === 200) {
      const data = result.data
      deviceList.value = data.devices || []
      // 更新统计信息
      deviceStats.value = {
        totalDevices: data.totalCount || 0,
        onlineDevices: data.activeCount || 0,
        currentDevice: deviceList.value.find(d => isCurrentDevice(d)) || null
      }
    } else {
      throw new Error(result.message || '获取设备列表失败')
    }
  } catch (error: any) {
    console.error('加载设备列表失败:', error)
    showErrorMessage(error.message || '加载设备列表失败')
  } finally {
    deviceLoading.value = false
  }
}

const refreshDeviceList = () => {
  loadDeviceList()
}

const logoutDevice = async (device: any) => {
  showConfirm({
    title: '强制下线设备',
    message: `确定要强制下线设备 "${getDeviceName(device)}" 吗？\n\n此操作将立即断开该设备的连接，该设备需要重新登录才能继续使用。`,
    confirmText: '强制下线',
    cancelText: '取消',
    type: 'danger',
    onConfirm: async () => {
      deviceLoading.value = true
      try {
        const token = getAuthToken()
        if (!token) {
          showErrorMessage('请先登录')
          return
        }

        const response = await fetch(`/api/user/devices/${device.id}`, {
          method: 'DELETE',
          headers: {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json'
          }
        })

        // 处理401未授权错误
        if (response.status === 401) {
          showErrorMessage('登录已过期，请重新登录')
          clearLoginInfo()
          setTimeout(() => {
            window.location.href = '/login'
          }, 2000)
          return
        }

        const result = await response.json()
        
        if (response.ok && result.code === 200) {
          showSuccessMessage('设备已成功下线')
          loadDeviceList() // 重新加载列表
        } else {
          throw new Error(result.message || '强制下线失败')
        }
      } catch (error: any) {
        console.error('强制下线设备失败:', error)
        showErrorMessage(error.message || '强制下线设备失败')
      } finally {
        deviceLoading.value = false
      }
    }
  })
}

const logoutAllDevices = async () => {
  showConfirm({
    title: '强制下线所有设备',
    message: '确定要强制下线所有其他设备吗？\n\n此操作将断开除当前设备外的所有设备连接，这些设备需要重新登录才能继续使用。此操作不可撤销。',
    confirmText: '强制下线所有设备',
    cancelText: '取消',
    type: 'danger',
    onConfirm: async () => {
      deviceLoading.value = true
      try {
        const token = getAuthToken()
        if (!token) {
          showErrorMessage('请先登录')
          return
        }

        // 获取当前设备信息
        const currentDeviceInfo = navigator.userAgent

        const response = await fetch('/api/user/devices/logout-others', {
          method: 'POST',
          headers: {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          body: `currentDeviceInfo=${encodeURIComponent(currentDeviceInfo)}`
        })

        // 处理401未授权错误
        if (response.status === 401) {
          showErrorMessage('登录已过期，请重新登录')
          clearLoginInfo()
          setTimeout(() => {
            window.location.href = '/login'
          }, 2000)
          return
        }

        const result = await response.json()
        
        if (response.ok && result.code === 200) {
          showSuccessMessage('所有其他设备已成功下线')
          loadDeviceList() // 重新加载列表
        } else {
          throw new Error(result.message || '强制下线所有设备失败')
        }
      } catch (error: any) {
        console.error('强制下线所有设备失败:', error)
        showErrorMessage(error.message || '强制下线所有设备失败')
      } finally {
        deviceLoading.value = false
      }
    }
  })
}

const getDeviceIcon = (deviceType: string): string => {
  const icons: { [key: string]: string } = {
    'desktop': '🖥️',
    'mobile': '📱',
    'tablet': '📱',
    'web': '🌐',
    'android': '📱',
    'ios': '📱',
    'windows': '🖥️',
    'mac': '🖥️',
    'linux': '🖥️'
  }
  return icons[deviceType?.toLowerCase()] || '🌐'
}

const getDeviceName = (device: any): string => {
  if (device.deviceInfo) {
    return device.deviceInfo
  }
  
  const typeNames: { [key: string]: string } = {
    'desktop': '桌面设备',
    'mobile': '移动设备',
    'tablet': '平板设备',
    'web': '网页浏览器',
    'android': 'Android设备',
    'ios': 'iOS设备',
    'windows': 'Windows设备',
    'mac': 'Mac设备',
    'linux': 'Linux设备'
  }
  
  return typeNames[device.deviceType?.toLowerCase()] || '未知设备'
}

const isCurrentDevice = (device: any): boolean => {
  // 简单判断：如果是当前IP且是活跃状态，可能是当前设备
  // 实际应用中可能需要更精确的判断逻辑
  return device.isActive && device.deviceType === 'Web'
}

const formatDateTime = (dateTimeStr: string): string => {
  if (!dateTimeStr) return '未知'
  
  try {
    const date = new Date(dateTimeStr)
    const now = new Date()
    const diff = now.getTime() - date.getTime()
    
    if (diff < 60000) { // 1分钟内
      return '刚刚'
    } else if (diff < 3600000) { // 1小时内
      return Math.floor(diff / 60000) + '分钟前'
    } else if (diff < 86400000) { // 24小时内
      return Math.floor(diff / 3600000) + '小时前'
    } else {
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString()
    }
  } catch (e) {
    return dateTimeStr
  }
}

const openTwoFactorAuth = () => {
  showErrorMessage('双重认证功能正在开发中，敬请期待！')
}

const openPrivacySettings = () => {
  showErrorMessage('隐私设置功能正在开发中，敬请期待！')
}

// 文件上传相关类型定义已从 @/types 导入

// 文件上传相关状态
const activeFileTab = ref('upload')
const selectedFiles = ref<File[]>([])
const uploadType = ref<'file' | 'image'>('file')
const maxWidth = ref(1920)
const maxHeight = ref(1080)
const isUploading = ref(false)
const uploadProgress = ref(0)
const uploadProgressText = ref('准备上传...')
const fileSuccessMessage = ref('')
const fileErrorMessage = ref('')
const fileTypeFilter = ref('')
const fileLoading = ref(false)
const fileList = ref<FileItem[]>([])
const fileStats = ref<FileStats>({
  imageCount: 0,
  videoCount: 0,
  documentCount: 0,
  audioCount: 0,
  totalFiles: 0,
  totalSize: 0
})
const fileInput = ref<HTMLInputElement | null>(null)
const uploadCancelled = ref(false)
const currentUploadController = ref<AbortController | null>(null)
const isDragOver = ref(false)

const openSystemNotifications = () => {
  activeTab.value = 'systemNotifications'
}

const refreshNotifications = () => {
  // 可以添加特定的刷新逻辑，如果需要
}

const openFileManager = () => {
  activeTab.value = 'fileManager'
  activeFileTab.value = 'upload'
  refreshFileManager()
}

const backToProfile = () => {
  activeTab.value = 'profile'
  resetFileUpload()
}

const resetFileUpload = () => {
  // 重置状态
  selectedFiles.value = []
  uploadType.value = 'file'
  isUploading.value = false
  uploadProgress.value = 0
  uploadProgressText.value = '准备上传...'
  fileSuccessMessage.value = ''
  fileErrorMessage.value = ''
  uploadCancelled.value = false
  if (currentUploadController.value) {
    currentUploadController.value.abort()
    currentUploadController.value = null
  }
}

const switchFileTab = (tabName: string) => {
  activeFileTab.value = tabName
  if (tabName === 'manage') {
    refreshFileManager()
  }
}



const toggleImageOptions = () => {
  // 图片选项切换逻辑已在模板中处理
}

const triggerFileInput = () => {
  if (fileInput.value) {
    fileInput.value.click()
  }
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files) {
    const files = Array.from(target.files)
    selectedFiles.value = files
    if (files.length > 0) {
      showFileAlert(`已选择 ${files.length} 个文件，可以开始上传`, 'success')
    }
  }
}

const handleDragOver = (event: DragEvent) => {
  event.preventDefault()
  isDragOver.value = true
}

const handleDragLeave = (event: DragEvent) => {
  event.preventDefault()
  isDragOver.value = false
}

const handleDrop = (event: DragEvent) => {
  event.preventDefault()
  isDragOver.value = false
  
  if (event.dataTransfer?.files) {
    const files = Array.from(event.dataTransfer.files)
    selectedFiles.value = files
    if (files.length > 0) {
      showFileAlert(`已选择 ${files.length} 个文件，可以开始上传`, 'success')
    }
  }
}

const showFileAlert = (message: string, type: 'success' | 'error') => {
  if (type === 'success') {
    fileSuccessMessage.value = message
    fileErrorMessage.value = ''
    setTimeout(() => {
      fileSuccessMessage.value = ''
    }, 3000)
  } else {
    fileErrorMessage.value = message
    fileSuccessMessage.value = ''
    setTimeout(() => {
      fileErrorMessage.value = ''
    }, 5000)
  }
}

const uploadFiles = async () => {
  if (selectedFiles.value.length === 0) {
    showFileAlert('请先选择文件', 'error')
    return
  }

  uploadCancelled.value = false
  isUploading.value = true
  uploadProgress.value = 0

  let successCount = 0
  let errorCount = 0

  for (let i = 0; i < selectedFiles.value.length; i++) {
    if (uploadCancelled.value) {
      showFileAlert('上传已取消', 'error')
      break
    }

    const file = selectedFiles.value[i]
    if (!file) {
      console.error('文件不存在:', i)
      errorCount++
      continue
    }

    const progress = ((i + 1) / selectedFiles.value.length) * 100
    uploadProgress.value = progress
    uploadProgressText.value = `正在上传: ${file.name} (${i + 1}/${selectedFiles.value.length})`

    try {
      const result = await uploadSingleFile(file)
      if (result.success) {
        successCount++
      } else {
        errorCount++
        showFileAlert(`文件 "${file.name}" 上传失败: 上传失败`, 'error')
      }
    } catch (error: any) {
      console.error('上传失败:', error)
      errorCount++
      showFileAlert(`文件 "${file.name}" 上传失败: ${error.message}`, 'error')
    }
  }

  isUploading.value = false

  if (!uploadCancelled.value) {
    if (errorCount === 0) {
      showFileAlert(`所有文件上传成功！共 ${successCount} 个文件`, 'success')
    } else {
      showFileAlert(`上传完成：成功 ${successCount} 个，失败 ${errorCount} 个`, 'error')
    }
  }

  // 重置
  selectedFiles.value = []
  if (fileInput.value) {
    fileInput.value.value = ''
  }
  currentUploadController.value = null

  // 刷新文件管理器数据
  if (!uploadCancelled.value) {
    await refreshFileManager()
  }
}

const cancelUpload = () => {
  if (isUploading.value) {
    // 取消上传
    uploadCancelled.value = true
    if (currentUploadController.value) {
      currentUploadController.value.abort()
    }
    isUploading.value = false
    showFileAlert('上传已取消', 'error')
  } else {
    // 取消选择
    selectedFiles.value = []
    if (fileInput.value) {
      fileInput.value.value = ''
    }
    showFileAlert('已取消文件选择', 'success')
  }
}

const uploadSingleFile = async (file: File): Promise<FileUploadResult> => {
  const formData = new FormData()
  formData.append('file', file)

  let url = '/api/files/upload'
  if (uploadType.value === 'image') {
    url = '/api/files/upload/image'
    formData.append('maxWidth', maxWidth.value.toString())
    formData.append('maxHeight', maxHeight.value.toString())
  }

  currentUploadController.value = new AbortController()
  const token = getAuthToken()
  
  if (!token) {
    throw new Error('用户未登录，请重新登录')
  }

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + token
    },
    body: formData,
    signal: currentUploadController.value.signal
  })

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('登录已过期，请重新登录')
    }
    const errorData = await response.json().catch(() => ({ message: '上传失败' }))
    throw new Error(errorData.message || '上传失败')
  }

  const result = await response.json()
  return { success: true, data: result }
}

const loadFileList = async () => {
  fileLoading.value = true
  try {
    const token = getAuthToken()
    
    // 检查是否有有效的token
    if (!token) {
      showFileAlert('用户未登录，请重新登录', 'error')
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }
    
    const params = new URLSearchParams()
    if (fileTypeFilter.value) {
      params.append('fileType', fileTypeFilter.value)
    }
    
    const response = await fetch(`/api/files/list?${params}`, {
      headers: {
        'Authorization': 'Bearer ' + token
      }
    })

    // 处理401未授权错误
    if (response.status === 401) {
      showFileAlert('登录已过期，请重新登录', 'error')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }

    if (!response.ok) {
      throw new Error('获取文件列表失败')
    }

    const result = await response.json()
    // 后端返回的数据结构是 {data: {files: [...], page, size, total}}
    if (result.data && result.data.files) {
      fileList.value = result.data.files as FileItem[]
    } else {
      fileList.value = (result.data || []) as FileItem[]
    }
  } catch (error: any) {
    console.error('加载文件列表失败:', error)
    showFileAlert('加载文件列表失败: ' + error.message, 'error')
  } finally {
    fileLoading.value = false
  }
}

const loadFileStats = async () => {
  try {
    const token = getAuthToken()
    
    // 检查是否有有效的token
    if (!token) {
      showFileAlert('用户未登录，请重新登录', 'error')
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }
    
    const response = await fetch('/api/files/stats', {
      headers: {
        'Authorization': 'Bearer ' + token
      }
    })

    // 处理401未授权错误
    if (response.status === 401) {
      showFileAlert('登录已过期，请重新登录', 'error')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }

    if (response.ok) {
      const result = await response.json()
      console.log('文件统计API返回数据:', result)
      
      if (result.data) {
        const data = result.data
        const typeStats = data.typeStats || {}
        
        // 将后端返回的typeStats转换为前端期望的格式
        fileStats.value = {
          totalFiles: data.totalFiles || 0,
          totalSize: data.totalSize || 0,
          imageCount: (typeStats.IMAGE || typeStats.image) || 0,
          videoCount: (typeStats.VIDEO || typeStats.video) || 0,
          documentCount: (typeStats.DOCUMENT || typeStats.document) || 0,
          audioCount: (typeStats.AUDIO || typeStats.audio) || 0
        } as FileStats
        
        console.log('处理后的文件统计数据:', fileStats.value)
      } else {
        // 默认值
        fileStats.value = {
          imageCount: 0,
          videoCount: 0,
          documentCount: 0,
          audioCount: 0,
          totalFiles: 0,
          totalSize: 0
        } as FileStats
      }
    }
  } catch (error) {
    console.error('加载文件统计失败:', error)
    showFileAlert('加载文件统计失败', 'error')
  }
}

const refreshFileManager = async () => {
  await loadFileList()
  await loadFileStats()
}

const getFileTypeClass = (fileType: string) => {
  switch (fileType?.toUpperCase()) {
    case 'IMAGE': return 'image'
    case 'VIDEO': return 'video'
    case 'AUDIO': return 'audio'
    case 'DOCUMENT': return 'document'
    default: return 'other'
  }
}

const getFileIcon = (fileType: string) => {
  switch (fileType?.toUpperCase()) {
    case 'IMAGE': return '🖼️'
    case 'VIDEO': return '🎥'
    case 'AUDIO': return '🎵'
    case 'DOCUMENT': return '📄'
    default: return '📁'
  }
}

// formatFileSize函数已从helpers.ts导入，无需在此定义

const downloadFile = async (file: FileItem) => {
  try {
    const token = getAuthToken()
    
    if (!token) {
      showFileAlert('用户未登录，请重新登录', 'error')
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }
    
    const response = await fetch(`/api/files/download/${file.fileId || file.id}`, {
      headers: {
        'Authorization': 'Bearer ' + token
      }
    })

    // 处理401未授权错误
    if (response.status === 401) {
      showFileAlert('登录已过期，请重新登录', 'error')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }

    if (!response.ok) {
      throw new Error('下载失败')
    }

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = file.originalFilename || file.originalName || file.fileName || 'download'
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
  } catch (error: any) {
    showFileAlert('下载失败: ' + error.message, 'error')
  }
}

const viewFile = (file: FileItem) => {
  if (file.fileUrl) {
    window.open(file.fileUrl, '_blank')
  } else {
    showFileAlert('文件链接不可用', 'error')
  }
}

const copyFileUrl = async (file: FileItem) => {
  if (!file.fileUrl) {
    showFileAlert('文件链接不可用', 'error')
    return
  }
  
  const fullUrl = window.location.origin + file.fileUrl
  await copyToClipboard(fullUrl)
  showFileAlert('文件链接已复制到剪贴板', 'success')
}

const copyToClipboard = async (text: string) => {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
    } else {
      // 降级方案
      const textArea = document.createElement('textarea')
      textArea.value = text
      textArea.style.position = 'fixed'
      textArea.style.left = '-999999px'
      textArea.style.top = '-999999px'
      document.body.appendChild(textArea)
      textArea.focus()
      textArea.select()
      document.execCommand('copy')
      textArea.remove()
    }
  } catch (error) {
    console.error('复制到剪贴板失败:', error)
    throw error
  }
}

const deleteFile = async (file: FileItem) => {
  const fileName = file.originalFilename || file.originalName || file.fileName
  if (!confirm(`确定要删除文件 "${fileName}" 吗？`)) {
    return
  }

  try {
    const token = getAuthToken()
    
    if (!token) {
      showFileAlert('用户未登录，请重新登录', 'error')
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }
    
    const fileId = file.fileId || file.id
    const response = await fetch(`/api/files/${fileId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': 'Bearer ' + token
      }
    })

    // 处理401未授权错误
    if (response.status === 401) {
      showFileAlert('登录已过期，请重新登录', 'error')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }

    if (!response.ok) {
      throw new Error('删除失败')
    }

    const result = await response.json()
    if (result.code === 200) {
      showFileAlert('文件删除成功', 'success')
      await refreshFileManager()
    } else {
      showFileAlert('删除失败: ' + result.message, 'error')
    }
  } catch (error: any) {
    showFileAlert('删除失败: ' + error.message, 'error')
  }
}



const openThemes = () => {
  settingsDialogVisible.value = true
  // 让SettingsDialog自动选择外观选项卡，需要在下一个事件循环中执行
  setTimeout(() => {
    const appearanceTab = document.querySelector('.tab-item[data-tab="appearance"]') as HTMLElement
    if (appearanceTab) {
      appearanceTab.click()
    }
  }, 0)
}

// 获取状态显示
const getStatusDisplay = () => {
  if (!userStatus.value || !userStatus.value.text) return '😊 暂无状态'
  
  const statusMap: Record<string, string> = {
    '在线': '🟢 在线',
    '忙碌': '🔴 忙碌',
    '离开': '🟡 离开',
    '隐身': '⚫ 隐身',
    '在路上': '✈️ 在路上',
    '工作中': '💼 工作中',
    '学习中': '📚 学习中',
    '休息中': '😴 休息中'
  }
  
  return statusMap[userStatus.value.text] || `${userStatus.value.emoji || '😊'} ${userStatus.value.text}`
}

// 这些方法已在其他位置定义

// 关闭个人信息模态框
const closeUserProfileModal = () => {
  showUserProfileModal.value = false
}

// 关闭查看用户资料模态框
const closeViewUserProfileModal = () => {
  showViewUserProfileModal.value = false
  viewingUserProfile.value = {}
}

// 获取性别文本
const getGenderText = (gender: string | undefined) => {
  if (!gender || gender.trim() === '') {
    return '未公开'
  }
  
  switch (gender.trim()) {
    case '男':
    case 'male':
    case 'MALE':
      return '男'
    case '女':
    case 'female':
    case 'FEMALE':
      return '女'
    case '保密':
    case 'private':
    case 'OTHER':
      return '保密'
    default:
      return '未公开'
  }
}



// 编辑个人资料相关方法
const triggerAvatarUpload = () => {
  avatarInput.value?.click()
}

const uploadProfileAvatar = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  
  // 严格的文件类型验证
  const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type.toLowerCase())) {
    showErrorMessage('请选择有效的图片文件（支持 JPEG、PNG、GIF、WebP 格式）')
    // 清空文件输入框
    target.value = ''
    return
  }
  
  // 文件大小验证（5MB限制）
  if (file.size > 5 * 1024 * 1024) {
    showErrorMessage('图片文件大小不能超过5MB')
    // 清空文件输入框
    target.value = ''
    return
  }
  
  let avatarUploadController: AbortController | null = null
  
  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      // 清空文件输入框
      target.value = ''
      return
    }
    
    // 创建AbortController用于取消上传
    avatarUploadController = new AbortController()
    
    // 使用公共文件上传接口，确保头像作为公共文件上传
    const formData = new FormData()
    formData.append('file', file)
    
    // 显示上传进度提示
    showSuccessMessage('正在上传头像...')
    
    // 调用公共文件上传接口
    const response = await fetch('/api/user/profile/avatar', {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + token
      },
      body: formData,
      signal: avatarUploadController.signal
    })
    
    // 处理401未授权错误
    if (response.status === 401) {
      showErrorMessage('登录已过期，请重新登录')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      // 清空文件输入框
      target.value = ''
      return
    }
    
    // 检查HTTP状态
    if (!response.ok) {
      let errorMessage = '头像上传失败'
      try {
        const errorData = await response.json()
        errorMessage = errorData.message || errorMessage
      } catch {
        // 如果无法解析错误响应，使用默认错误消息
      }
      throw new Error(errorMessage)
    }
    
    // 解析响应数据
    const data = await response.json()
    if (data.code === 200 && data.data && data.data.avatarUrl) {
      // 更新用户头像信息
      userProfile.value.avatar = data.data.avatarUrl
      if (currentUser.value) {
        currentUser.value.avatar = data.data.avatarUrl
      }
      showSuccessMessage('头像上传成功，原有头像已替换')
    } else {
      throw new Error(data.message || '头像上传失败：服务器响应异常')
    }
  } catch (error: any) {
    if (error.name === 'AbortError') {
      showErrorMessage('头像上传已取消')
    } else {
      console.error('头像上传失败:', error)
      showErrorMessage(error.message || '头像上传失败，请稍后重试')
    }
  } finally {
    // 确保清空文件输入框
    if (target) {
      target.value = ''
    }
    avatarUploadController = null
  }
}

// 移除头像功能已注释
/*
const removeAvatar = async () => {
  showConfirm({
    title: '移除头像',
    message: '确定要移除当前头像吗？\n\n移除后将显示默认头像，原有头像文件将从公共存储中删除且不再可见。您可以随时重新上传新头像。',
    confirmText: '移除头像',
    cancelText: '取消',
    type: 'warning',
    onConfirm: async () => {
      try {
        const token = getAuthToken()
        if (!token) {
          showErrorMessage('请先登录')
          return
        }
        
        // 调用头像删除接口，确保从公共存储中删除
        const response = await fetch('/api/user/profile/avatar', {
          method: 'DELETE',
          headers: {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json'
          }
        })
        
        // 处理401未授权错误
        if (response.status === 401) {
          showErrorMessage('登录已过期，请重新登录')
          clearLoginInfo()
          setTimeout(() => {
            router.push('/login')
          }, 2000)
          return
        }
        
        // 检查HTTP状态
        if (!response.ok) {
          let errorMessage = '头像删除失败'
          try {
            const errorData = await response.json()
            errorMessage = errorData.message || errorMessage
          } catch {
            // 如果无法解析错误响应，使用默认错误消息
          }
          throw new Error(errorMessage)
        }
        
        // 解析响应数据
        const data = await response.json()
        if (data.code === 200) {
          // 清除用户头像信息
          userProfile.value.avatar = ''
          if (currentUser.value) {
            currentUser.value.avatar = ''
          }
          showSuccessMessage('头像删除成功，原有头像已从公共存储中移除')
        } else {
          throw new Error(data.message || '头像删除失败：服务器响应异常')
        }
      } catch (error: any) {
        console.error('头像删除失败:', error)
        showErrorMessage(error.message || '头像删除失败，请稍后重试')
      }
    }
  })
}
*/

const toggleStatusForm = () => {
  showStatusForm.value = !showStatusForm.value
}



const selectPresetStatus = (preset: { emoji: string; text: string }) => {
  userStatus.value = { emoji: preset.emoji, text: preset.text }
}

const selectDuration = (duration: string) => {
  selectedDuration.value = duration
}

const saveStatus = async () => {
  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }
    
    let statusData
    if (statusType.value === 'preset') {
      statusData = userStatus.value
    } else {
      statusData = customStatus.value
    }
    
    // 计算过期时间
    let expiryTime = null
    if (selectedDuration.value !== 'manual') {
      const now = new Date()
      switch (selectedDuration.value) {
        case '30s':
          expiryTime = new Date(now.getTime() + 30 * 1000)
          break
        case '1h':
          expiryTime = new Date(now.getTime() + 60 * 60 * 1000)
          break
        case '4h':
          expiryTime = new Date(now.getTime() + 4 * 60 * 60 * 1000)
          break
        case 'today':
          expiryTime = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59)
          break
      }
    }
    
    const requestData = {
      emoji: statusData.emoji,
      text: statusData.text,
      expiryTime: expiryTime?.toISOString() || null
    }
    
    const response = await fetch('/api/user/status', {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(requestData)
    })
    
    if (response.status === 401) {
      showErrorMessage('登录已过期，请重新登录')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }
    
    const data = await response.json()
    if (data.code === 200) {
      userStatus.value = statusData
      // 同时更新userProfile中的状态属性，确保数据一致性
      userProfile.value.statusText = statusData.text
      userProfile.value.statusEmoji = statusData.emoji
      if (expiryTime) {
        userProfile.value.statusExpiry = expiryTime.toISOString()
      } else {
        userProfile.value.statusExpiry = ''
      }
      showStatusForm.value = false
      showSuccessMessage('状态设置成功')
    } else {
      showErrorMessage(data.message || '状态设置失败')
    }
  } catch (error) {
    console.error('状态设置失败:', error)
    showErrorMessage('状态设置失败，请稍后重试')
  }
}

const clearStatus = async () => {
  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }
    
    const response = await fetch('/api/user/status', {
      method: 'DELETE',
      headers: {
        'Authorization': 'Bearer ' + token
      }
    })
    
    if (response.status === 401) {
      showErrorMessage('登录已过期，请重新登录')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }
    
    const data = await response.json()
    if (data.code === 200) {
      userStatus.value = { emoji: '😊', text: '暂无状态' }
      // 同时清除userProfile中的状态属性
      userProfile.value.statusText = ''
      userProfile.value.statusEmoji = ''
      userProfile.value.statusExpiry = ''
      showStatusForm.value = false
      showSuccessMessage('状态已清除')
    } else {
      showErrorMessage(data.message || '清除状态失败')
    }
  } catch (error) {
    console.error('清除状态失败:', error)
    showErrorMessage('清除状态失败，请稍后重试')
  }
}

const saveProfile = async () => {
  try {
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }
    
    // 将前端性别值转换为后端期望的中文值
    const convertGenderToBackend = (gender: string | undefined) => {
      const genderMap: Record<string, string> = {
        'male': '男',
        'female': '女',
        'private': '保密'
      }
      return gender ? genderMap[gender] || gender : ''
    }
    
    const requestData = {
      nickname: userProfile.value.name,
      signature: userProfile.value.signature,
      phoneNumber: userProfile.value.phone,  // 修复：使用phoneNumber字段名
      gender: convertGenderToBackend(userProfile.value.gender),
      birthday: userProfile.value.birthday,
      location: userProfile.value.location,
      occupation: userProfile.value.occupation
      // 注意：个人ID需要通过单独的API设置
    }
    
    const response = await fetch('/api/user/profile', {
      method: 'PUT',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(requestData)
    })
    
    if (response.status === 401) {
      showErrorMessage('登录已过期，请重新登录')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }
    
    const data = await response.json()
    if (data.code === 200) {
      // 如果个人ID有变化，单独设置个人ID
      const currentUserIdString = currentUser.value?.userIdString || ''
      const newUserIdString = userProfile.value.userIdString?.trim() || ''
      
      if (newUserIdString && newUserIdString !== currentUserIdString) {
        try {
          const userIdResponse = await fetch('/api/user/personal-id', {
            method: 'POST',
            headers: {
              'Authorization': 'Bearer ' + token,
              'Content-Type': 'application/json'
            },
            body: JSON.stringify({
              userIdString: newUserIdString
            })
          })
          
          if (!userIdResponse.ok) {
            const userIdData = await userIdResponse.json()
            showErrorMessage(userIdData.message || '设置个人ID失败')
            return
          }
        } catch (error) {
          console.error('设置个人ID失败:', error)
          showErrorMessage('设置个人ID失败')
          return
        }
      }
      
      if (currentUser.value) {
        currentUser.value.name = userProfile.value.name
        currentUser.value.signature = userProfile.value.signature
        currentUser.value.userIdString = userProfile.value.userIdString
      }
      showUserProfileModal.value = false
      showSuccessMessage('个人资料保存成功')
      // 移除 initData() 调用，避免覆盖用户状态
    } else {
      showErrorMessage(data.message || '保存个人资料失败')
    }
  } catch (error) {
    console.error('保存个人资料失败:', error)
    showErrorMessage('保存个人资料失败，请稍后重试')
  }
}

const showSettings = () => {
  settingsDialogVisible.value = true
}

const closeSettingsDialog = () => {
  settingsDialogVisible.value = false
}

const logout = () => {
  // 检查是否有记住我状态
  const rememberMe = localStorage.getItem('rememberMe') === 'true'
  
  if (rememberMe) {
    // 如果有记住我，显示退出选项
    showLogoutModal()
  } else {
    // 如果没有记住我，直接完全退出
    performLogout(false)
  }
}

// 显示退出登录模态框
const showLogoutModal = () => {
  const overlay = document.createElement('div')
  overlay.className = 'logout-modal-overlay'
  overlay.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.5);
    z-index: 10000;
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    visibility: hidden;
    transition: all 0.3s ease;
  `
  
  const modal = document.createElement('div')
  modal.className = 'logout-modal-content'
  modal.style.cssText = `
    background: white;
    border-radius: 12px;
    padding: 24px;
    max-width: 360px;
    width: 90%;
    text-align: center;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
    transform: scale(0.8);
    transition: transform 0.3s ease;
  `
  
  modal.innerHTML = `
    <div style="font-size: 48px; margin-bottom: 16px; color: #ff6b35;">🚪</div>
    <div style="font-size: 18px; font-weight: 600; color: #333; margin-bottom: 8px;">退出登录</div>
    <div style="font-size: 14px; color: #666; margin-bottom: 24px; line-height: 1.4;">请选择退出方式：</div>
    <div style="display: flex; flex-direction: column; gap: 12px;">
      <button id="logoutKeep" style="
        padding: 12px 16px;
        background: #007bff;
        color: white;
        border: none;
        border-radius: 8px;
        cursor: pointer;
        font-size: 14px;
        font-weight: 500;
        transition: all 0.2s ease;
      ">保留登录信息</button>
      <button id="logoutClear" style="
        padding: 12px 16px;
        background: #ff6b35;
        color: white;
        border: none;
        border-radius: 8px;
        cursor: pointer;
        font-size: 14px;
        font-weight: 500;
        transition: all 0.2s ease;
      ">完全退出</button>
      <button id="logoutCancel" style="
        padding: 12px 16px;
        background: #f5f5f5;
        color: #333;
        border: none;
        border-radius: 8px;
        cursor: pointer;
        font-size: 14px;
        font-weight: 500;
        transition: all 0.2s ease;
      ">取消</button>
    </div>
  `
  
  overlay.appendChild(modal)
  document.body.appendChild(overlay)
  
  // 显示动画
  setTimeout(() => {
    overlay.style.opacity = '1'
    overlay.style.visibility = 'visible'
    modal.style.transform = 'scale(1)'
  }, 10)
  
  // 绑定事件
  const keepBtn = modal.querySelector('#logoutKeep') as HTMLButtonElement
  const clearBtn = modal.querySelector('#logoutClear') as HTMLButtonElement
  const cancelBtn = modal.querySelector('#logoutCancel') as HTMLButtonElement
  
  // 添加按钮悬停效果
  keepBtn?.addEventListener('mouseenter', () => {
    keepBtn.style.background = '#0056b3'
  })
  keepBtn?.addEventListener('mouseleave', () => {
    keepBtn.style.background = '#007bff'
  })
  
  clearBtn?.addEventListener('mouseenter', () => {
    clearBtn.style.background = '#e55a2b'
  })
  clearBtn?.addEventListener('mouseleave', () => {
    clearBtn.style.background = '#ff6b35'
  })
  
  cancelBtn?.addEventListener('mouseenter', () => {
    cancelBtn.style.background = '#e8e8e8'
  })
  cancelBtn?.addEventListener('mouseleave', () => {
    cancelBtn.style.background = '#f5f5f5'
  })
  
  // 添加按钮点击效果
  const addClickEffect = (btn: HTMLButtonElement) => {
    btn.addEventListener('mousedown', () => {
      btn.style.transform = 'scale(0.98)'
    })
    btn.addEventListener('mouseup', () => {
      btn.style.transform = 'scale(1)'
    })
  }
  
  if (keepBtn) addClickEffect(keepBtn)
  if (clearBtn) addClickEffect(clearBtn)
  if (cancelBtn) addClickEffect(cancelBtn)
  
  const closeModal = () => {
    overlay.style.opacity = '0'
    overlay.style.visibility = 'hidden'
    modal.style.transform = 'scale(0.8)'
    setTimeout(() => overlay.remove(), 300)
  }
  
  keepBtn?.addEventListener('click', () => {
    closeModal()
    setTimeout(() => performLogout(true), 300)
  })
  
  clearBtn?.addEventListener('click', () => {
    closeModal()
    setTimeout(() => performLogout(false), 300)
  })
  
  cancelBtn?.addEventListener('click', closeModal)
  
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) closeModal()
  })
}

// 执行退出登录
const performLogout = async (keepInfo: boolean) => {
  try {
    // 调用后端退出登录API
    const token = getAuthToken()
    if (token) {
      await fetch('/api/auth/logout', {
        method: 'POST',
        headers: {
          'Authorization': 'Bearer ' + token,
          'Content-Type': 'application/json'
        }
      })
    }
  } catch (error) {
    console.error('退出登录API调用失败:', error)
  }
  
  if (keepInfo) {
    // 保留登录信息：只清除token，保留用户信息和记住我状态
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('refreshToken')
    sessionStorage.removeItem('userInfo')
    // 保留 localStorage 中的 userInfo、rememberMe、savedEmail、savedPassword
    showSuccessMessage('已退出登录，登录信息已保留')
  } else {
    // 完全退出：清除所有信息
    clearLoginInfo()
    localStorage.removeItem('rememberMe')
    localStorage.removeItem('savedEmail')
    localStorage.removeItem('savedPassword')
    showSuccessMessage('已完全退出登录')
  }
  
  // 跳转到登录页
  setTimeout(() => {
    router.push('/login')
  }, 1000)
}

const handleSettingsUpdate = (settings: any) => {
  // 实现设置更新逻辑
  Object.assign(userSettings.value, settings)
  console.log('更新设置:', settings)
}

const handleProfileSave = async (profile: any) => {
  try {
    // 获取JWT token
    const token = getAuthToken()
    if (!token) {
      showErrorMessage('请先登录')
      return
    }
    
    // 构建请求数据
    const requestData = {
      nickname: profile.name || profile.nickname,
      signature: profile.signature || '',
      phoneNumber: profile.phone || '',  // 修复：使用phoneNumber字段名
      gender: profile.gender || '',  // 修复：不设置默认值，让后端处理
      birthday: profile.birthday || null,
      location: profile.location || '',
      occupation: profile.occupation || ''
      // 注意：个人ID和状态信息通过单独的API设置
    }
    
    // 调用后端API保存个人资料
    const response = await fetch('/api/user/profile', {
      method: 'PUT',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(requestData)
    })
    
    // 处理401未授权错误
    if (response.status === 401) {
      showErrorMessage('登录已过期，请重新登录')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 2000)
      return
    }
    
    const data = await response.json()
    if (data.code === 200) {
      // 如果个人ID有变化，单独设置个人ID
      const currentUserIdString = currentUser.value?.userIdString || ''
      const newUserIdString = profile.userIdString?.trim() || ''
      
      if (newUserIdString && newUserIdString !== currentUserIdString) {
        try {
          const userIdResponse = await fetch('/api/user/personal-id', {
            method: 'POST',
            headers: {
              'Authorization': 'Bearer ' + token,
              'Content-Type': 'application/json'
            },
            body: JSON.stringify({
              userIdString: newUserIdString
            })
          })
          
          if (!userIdResponse.ok) {
            const userIdData = await userIdResponse.json()
            showErrorMessage(userIdData.message || '设置个人ID失败')
            return
          }
        } catch (error) {
          console.error('设置个人ID失败:', error)
          showErrorMessage('设置个人ID失败')
          return
        }
      }
      
      // 更新本地用户信息
      Object.assign(userProfile.value, profile)
      if (currentUser.value) {
        currentUser.value.name = profile.name || profile.nickname
        currentUser.value.nickname = profile.nickname
        currentUser.value.email = profile.email
        currentUser.value.avatar = profile.avatar || undefined
        currentUser.value.userIdString = profile.userIdString
      }
      
      showSuccessMessage('个人资料保存成功')
      showProfileEditModal.value = false
      
      // 移除 initData() 调用，避免覆盖用户状态
    } else {
      showErrorMessage(data.message || '保存个人资料失败')
    }
  } catch (error) {
    console.error('保存个人资料失败:', error)
    showErrorMessage('保存个人资料失败，请稍后重试')
  }
}



// 显示二维码模态框
const showQRCodeModal = (qrCodeData: any) => {
  // 创建遮罩层
  const overlay = document.createElement('div')
  overlay.className = 'qr-modal-overlay'
  overlay.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.5);
    z-index: 1000;
    display: flex;
    align-items: center;
    justify-content: center;
  `
  
  // 创建模态框
  const modal = document.createElement('div')
  modal.className = 'qr-modal'
  modal.style.cssText = `
    background: white;
    border-radius: 12px;
    padding: 24px;
    max-width: 400px;
    width: 90%;
    text-align: center;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  `
  
  modal.innerHTML = `
    <h3 style="margin: 0 0 20px 0; color: #333;">我的二维码名片</h3>
    
    <div style="margin-bottom: 16px;">
      ${qrCodeData.userAvatarUrl ? `<img src="${qrCodeData.userAvatarUrl}" alt="头像" style="width: 60px; height: 60px; border-radius: 50%; margin-bottom: 8px; border: 2px solid #eee;" />` : ''}
      <p style="margin: 8px 0 4px 0; font-weight: bold; color: #333;">${qrCodeData.userNickname || '用户'}</p>
      <p style="margin: 0; color: #666; font-size: 12px;">${qrCodeData.userIdString ? `ID: ${qrCodeData.userIdString}` : '暂无个人ID'}</p>
    </div>
    
    <div style="margin: 20px 0;">
      <img src="${qrCodeData.qrCodeBase64}" alt="二维码" style="max-width: 200px; width: 100%; border: 1px solid #eee; border-radius: 8px;" />
    </div>
    <p style="color: #666; font-size: 14px; margin: 16px 0;">扫描二维码添加我为好友</p>
    
    <div style="display: flex; gap: 12px; justify-content: center; margin-top: 20px;">
      <button id="downloadQR" style="
        padding: 8px 16px;
        background: #007bff;
        color: white;
        border: none;
        border-radius: 6px;
        cursor: pointer;
        font-size: 14px;
      ">下载二维码</button>
      <button id="shareQR" style="
        padding: 8px 16px;
        background: #28a745;
        color: white;
        border: none;
        border-radius: 6px;
        cursor: pointer;
        font-size: 14px;
      ">分享二维码</button>
      <button id="closeQR" style="
        padding: 8px 16px;
        background: #6c757d;
        color: white;
        border: none;
        border-radius: 6px;
        cursor: pointer;
        font-size: 14px;
      ">关闭</button>
    </div>
    
    <div style="margin-top: 16px; padding-top: 16px; border-top: 1px solid #eee; font-size: 12px; color: #999;">
      <p style="margin: 4px 0;">生成时间: ${new Date(qrCodeData.generatedAt).toLocaleString()}</p>
      <p style="margin: 4px 0;">有效期至: ${new Date(qrCodeData.expiresAt).toLocaleString()}</p>
    </div>
  `
  
  overlay.appendChild(modal)
  document.body.appendChild(overlay)
  
  // 绑定事件
  const downloadBtn = modal.querySelector('#downloadQR') as HTMLButtonElement
  const shareBtn = modal.querySelector('#shareQR') as HTMLButtonElement
  const closeBtn = modal.querySelector('#closeQR') as HTMLButtonElement
  
  downloadBtn?.addEventListener('click', () => downloadQRCode(qrCodeData))
  shareBtn?.addEventListener('click', () => shareQRCode(qrCodeData))
  closeBtn?.addEventListener('click', () => overlay.remove())
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) overlay.remove()
  })
}

// 下载二维码
const downloadQRCode = (qrCodeData: any) => {
  const link = document.createElement('a')
  link.href = qrCodeData.qrCodeBase64
  link.download = `${qrCodeData.userNickname || '用户'}_二维码名片.png`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  showSuccessMessage('二维码下载成功')
}

// 分享二维码
const shareQRCode = async (qrCodeData: any) => {
  try {
    // 将base64转换为blob
    const response = await fetch(qrCodeData.qrCodeBase64)
    const blob = await response.blob()
    const file = new File([blob], `${qrCodeData.userNickname || '用户'}_二维码名片.png`, { type: 'image/png' })
    
    if (navigator.share && navigator.canShare({ files: [file] })) {
      await navigator.share({
        title: '我的二维码名片',
        text: '扫描二维码添加我为好友',
        files: [file]
      })
    } else {
      // 降级方案：复制到剪贴板
      await navigator.clipboard.writeText('扫描二维码添加我为好友')
      showSuccessMessage('二维码信息已复制到剪贴板')
    }
  } catch (error) {
    console.error('分享失败:', error)
    showErrorMessage('分享功能暂不可用')
  }
}



// 隐藏选项菜单
const hideOptionsMenu = () => {
  showOptionsMenuVisible.value = false
  selectedChat.value = null
  
  const menu = document.querySelector('.options-menu')
  const overlay = document.querySelector('.options-menu-overlay')
  
  if (menu) {
    menu.classList.remove('show')
    setTimeout(() => {
      menu.remove()
    }, 200) // 等待动画完成
  }
  
  if (overlay) {
    overlay.remove()
  }
}







// 获取认证token
const getAuthToken = (): string | null => {
  return localStorage.getItem('token') || localStorage.getItem('accessToken') || 
         sessionStorage.getItem('token') || sessionStorage.getItem('accessToken')
}

// 获取用户信息
const getUserInfo = (): User | null => {
  const userInfoStr = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo')
  const accessToken = getAuthToken()
  
  if (userInfoStr && accessToken) {
    try {
      const userData = JSON.parse(userInfoStr)
      
      // 解析personalizedStatus JSON字符串为status对象
      if (userData.personalizedStatus && typeof userData.personalizedStatus === 'string') {
        try {
          userData.status = JSON.parse(userData.personalizedStatus)
        } catch (e) {
          console.warn('解析本地存储的用户状态失败:', e)
          userData.status = null
        }
      } else if (!userData.personalizedStatus) {
        userData.status = null
      }
      
      return userData
    } catch (e) {
      console.error('解析用户信息失败:', e)
      return null
    }
  }
  return null
}

// 验证token有效性
const validateToken = async (): Promise<boolean> => {
  const token = getAuthToken()
  if (!token) {
    return false
  }

  try {
    const response = await fetch('/api/auth/me', {
      method: 'GET',
      headers: {
        'Authorization': 'Bearer ' + token
      }
    })
    
    // 处理401未授权错误
    if (response.status === 401) {
      console.warn('Token已失效，清除登录信息并跳转到登录页')
      clearLoginInfo()
      setTimeout(() => {
        router.push('/login')
      }, 1000)
      return false
    }
    
    return response.ok
  } catch (error) {
    console.error('Token验证失败:', error)
    return false
  }
}

// 清除登录信息
const clearLoginInfo = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('userInfo')
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('accessToken')
  sessionStorage.removeItem('refreshToken')
  sessionStorage.removeItem('userInfo')
}

// 显示错误信息
const showErrorMessage = (message: string) => {
  errorMessage.value = message
  showError.value = true
  setTimeout(() => {
    showError.value = false
  }, 5000)
}

// 显示成功信息
const showSuccessMessage = (message: string) => {
  successMessage.value = message
  showSuccess.value = true
  setTimeout(() => {
    showSuccess.value = false
  }, 3000)
}

// 初始化会话列表
const initChatList = async () => {
  const token = getAuthToken()
  const userInfo = getUserInfo()
  
  if (!token || !userInfo || !userInfo.id) {
    chats.value = []
    return
  }
  
  try {
    const response = await fetch(`/api/conversations?userId=${userInfo.id}&page=0&size=20`, {
      method: 'GET',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json',
        'X-User-Id': userInfo.id.toString()
      }
    })
    
    if (response.status === 401) {
      clearLoginInfo()
      router.push('/login')
      return
    }
    
    if (!response.ok) {
      throw new Error(`获取会话列表失败: ${response.status}`)
    }
    
    const data = await response.json()
    console.log('从后端接收到的会话数据:', data)
    
    // 增加更多日志以便调试
    if (data.data) {
      console.log('会话数据结构:', JSON.stringify(data.data, null, 2).substring(0, 500) + '...')
    }
    
    // 处理不同的数据结构
    if (data.code === 200 && data.data) {
      let conversationsArray: any[] = []
      
      // 检查各种可能的数据结构
      if (data.data.content && data.data.content.length > 0) {
        // 分页结构
        const firstItem = data.data.content[0]
        if (firstItem.conversations && Array.isArray(firstItem.conversations)) {
          // 如果content中的第一个元素包含conversations数组
          conversationsArray = firstItem.conversations
          console.log(`找到会话数组，包含 ${conversationsArray.length} 个会话`)
        } else if (Array.isArray(data.data.content)) {
          // 如果content本身是会话数组
          conversationsArray = data.data.content
          console.log(`content是会话数组，包含 ${conversationsArray.length} 个会话`)
        }
      } else if (data.data.conversations && Array.isArray(data.data.conversations)) {
        // 直接包含conversations数组
        conversationsArray = data.data.conversations
        console.log(`直接包含会话数组，有 ${conversationsArray.length} 个会话`)
      } else if (Array.isArray(data.data)) {
        // 数据本身就是数组
        conversationsArray = data.data
        console.log(`数据本身是数组，包含 ${conversationsArray.length} 个会话`)
      }
      
      if (conversationsArray.length > 0) {
        // 记录第一个会话的结构，帮助调试
        console.log('第一个会话的结构:', JSON.stringify(conversationsArray[0], null, 2))
        
        chats.value = conversationsArray.map((conv: any) => ({
          id: conv.id,
          name: getConversationDisplayName(conv),
          avatar: getConversationAvatar(conv),
          lastMessage: getLastMessageContent(conv.lastMessage),
          lastMessageTime: conv.lastActiveTime || conv.lastActiveAt,
          unreadCount: conv.unreadCount || 0,
          isPinned: conv.isPinned || false,
          isDnd: conv.isDnd || false,
          type: conv.type,
          participants: conv.participants,
          rawData: conv // 保存原始数据，以便后续操作
        }))
        
        // 获取每个会话的最新消息
        await fetchLatestMessagesForConversations()
      } else {
        console.log('未找到有效的会话数据')
        chats.value = []
      }
    } else {
      console.log('获取会话数据失败或格式不正确:', data)
      chats.value = []
    }
  } catch (error) {
    console.error('获取会话列表失败:', error)
    showErrorMessage('获取会话列表失败，请检查网络连接')
    chats.value = []
  }
}

// 获取每个会话的最新消息
const fetchLatestMessagesForConversations = async () => {
  if (!chats.value || chats.value.length === 0) return
  
  const token = getAuthToken()
  const userInfo = getUserInfo()
  
  if (!token || !userInfo) return
  
  console.log('开始获取所有会话的最新消息')
  
  // 为每个会话获取最新消息
  for (const chat of chats.value) {
    try {
      console.log(`获取会话 ${chat.id} 的最新消息`)
      const response = await fetch(`/api/messages/conversation/${chat.id}?page=0&size=1`, {
        method: 'GET',
        headers: {
          'Authorization': 'Bearer ' + token,
          'Content-Type': 'application/json'
        }
      })
      
      if (!response.ok) {
        console.warn(`获取会话 ${chat.id} 的消息失败: ${response.status}`)
        continue
      }
      
      const data = await response.json()
      console.log(`会话 ${chat.id} 的消息响应:`, data)
      
      if (data.success && data.data && data.data.content && data.data.content.length > 0) {
        const latestMessage = data.data.content[0]
        console.log(`会话 ${chat.id} 的最新消息:`, latestMessage)
        
        // 更新会话的最新消息
        chat.lastMessage = getLastMessageContent({
          messageType: latestMessage.messageType || latestMessage.type,
          content: latestMessage.content
        })
        
        // 更新最后消息时间
        if (latestMessage.createdAt) {
          chat.lastMessageTime = latestMessage.createdAt
        }
        
        console.log(`更新会话 ${chat.id} 的最新消息为: ${chat.lastMessage}`)
      }
    } catch (error) {
      console.error(`获取会话 ${chat.id} 的最新消息失败:`, error)
    }
  }
  
  console.log('所有会话的最新消息获取完成')
}

// 获取会话显示名称
const getConversationDisplayName = (conversation: any): string => {
  const userInfo = getUserInfo()
  
  if (conversation.type === 'PRIVATE') {
    // 私聊：查找对方参与者信息
    const otherParticipant = conversation.participants?.find((p: any) => p.userId !== userInfo?.id)
    if (otherParticipant) {
      // 优先使用备注名（alias），然后是用户昵称，最后是邮箱
      if (otherParticipant.alias) {
        return otherParticipant.alias
      }
      if (otherParticipant.user && otherParticipant.user.nickname) {
        return otherParticipant.user.nickname
      }
      if (otherParticipant.user && otherParticipant.user.email) {
        return otherParticipant.user.email
      }
      // 如果都没有，使用用户ID
      return `用户${otherParticipant.userId}`
    }
    return '私聊'
  } else {
    // 群聊：使用会话名称
    return conversation.name || '群聊'
  }
}

// 获取会话头像
const getConversationAvatar = (conversation: any): string => {
  const userInfo = getUserInfo()
  
  if (conversation.type === 'PRIVATE') {
    // 私聊：使用对方用户的头像
    const otherParticipant = conversation.participants?.find((p: any) => p.userId !== userInfo?.id)
    if (otherParticipant && otherParticipant.user && otherParticipant.user.avatarUrl) {
      return otherParticipant.user.avatarUrl
    }
    // 如果没有头像，使用对方用户名的首字母
    const displayName = getConversationDisplayName(conversation)
    return getAvatarText(displayName)
  } else {
    // 群聊：使用会话头像
    if (conversation.avatarUrl) {
      return conversation.avatarUrl
    }
    // 如果没有头像，使用群聊名称的首字母
    const displayName = conversation.name || '群聊'
    return getAvatarText(displayName)
  }
}

// 获取最后一条消息内容
const getLastMessageContent = (lastMessage: any): string => {
  if (!lastMessage) {
    return '暂无消息'
  }
  
  // 检查消息结构，处理可能的嵌套结构
  const messageType = lastMessage.messageType || lastMessage.type
  const content = lastMessage.content
  
  // 记录消息结构以便调试
  console.log('最后一条消息结构:', lastMessage)
  
  // 根据消息类型显示不同内容
  switch (messageType) {
    case 'TEXT':
      return content || ''
    case 'IMAGE':
      return '[图片]'
    case 'FILE':
      return '[文件]'
    case 'AUDIO':
    case 'VOICE':
      return '[语音]'
    case 'VIDEO':
      return '[视频]'
    case 'SYSTEM':
      return '[系统消息]'
    default:
      // 如果有内容但类型不明确，直接显示内容
      if (content) {
        return content
      }
      return '[消息]'
  }
}

// 初始化联系人列表
const initContactsList = async () => {
  const token = getAuthToken()
  const userInfo = getUserInfo()
  
  if (!token || !userInfo || !userInfo.id) {
    contacts.value = []
    return
  }
  
  try {
    const response = await fetch(`/api/contacts?userId=${userInfo.id}&includeBlocked=false`, {
      method: 'GET',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      }
    })
    
    if (response.status === 401) {
      clearLoginInfo()
      router.push('/login')
      return
    }
    
    if (!response.ok) {
      throw new Error('获取联系人列表失败')
    }
    
    const data = await response.json()
    console.log('从后端接收到的联系人数据:', data)
    
    if (data.code === 200 && data.data) {
      contacts.value = data.data.map((contact: any) => ({
        id: contact.friendId || contact.id,
        name: contact.nickname || contact.friendUsername || '',
        avatar: contact.avatarUrl,
        signature: contact.signature || '',
        isOnline: contact.isOnline || false,
        alias: contact.alias,
        tags: contact.tags,
        friend: {
          id: contact.friendId,
          nickname: contact.nickname,
          avatarUrl: contact.avatarUrl,
          signature: contact.signature
        },
        nickname: contact.nickname
      }))
    } else {
      contacts.value = []
    }
  } catch (error) {
    console.error('获取联系人列表失败:', error)
    showErrorMessage('获取联系人列表失败，请检查网络连接')
    contacts.value = []
  }
}

// 动态列表初始化已移至MomentView组件

// 初始化数据
const initData = async () => {
  try {
    // 获取用户信息
    const userInfo = getUserInfo()
    if (userInfo) {
      // 验证token有效性
      const isTokenValid = await validateToken()
      
      if (isTokenValid) {
        // 获取完整的用户资料信息
        try {
          const token = getAuthToken()
          const profileResponse = await fetch('/api/user/profile', {
            method: 'GET',
            headers: {
              'Authorization': 'Bearer ' + token,
              'Content-Type': 'application/json'
            }
          })
          
          if (profileResponse.ok) {
            const profileData = await profileResponse.json()
            if (profileData.code === 200 && profileData.data) {
              const profile = profileData.data
              // 将后端中文性别值转换为前端英文值
              const convertGenderToFrontend = (gender: string | undefined) => {
                const genderMap: Record<string, string> = {
                  '男': 'male',
                  '女': 'female',
                  '保密': 'private'
                }
                return gender ? genderMap[gender] || gender : undefined
              }
              
              // 更新用户资料
              Object.assign(userProfile.value, {
                name: profile.nickname || profile.name || userInfo.name,
                signature: profile.signature || '',
                avatar: profile.avatarUrl || userInfo.avatar || '',
                gender: convertGenderToFrontend(profile.gender),
                birthday: profile.birthday || '',
                email: profile.email || userInfo.email,
                phone: profile.phoneNumber || '',  // 修复：使用phoneNumber而不是phone
                location: profile.location || '',
                occupation: profile.occupation || '',
                userIdString: profile.userIdString || ''  // 添加个人ID映射
              })
              
              // 处理状态信息
              if (profile.status) {
                userProfile.value.statusText = profile.status.text || ''
                userProfile.value.statusEmoji = profile.status.emoji || ''
                userProfile.value.statusExpiry = profile.status.expiresAt || ''
              } else {
                userProfile.value.statusText = ''
                userProfile.value.statusEmoji = ''
                userProfile.value.statusExpiry = ''
              }
              
              // 更新当前用户信息
              currentUser.value = {
                id: userInfo.id,
                name: profile.nickname || profile.name || userInfo.name || userInfo.email,
                email: profile.email || userInfo.email,
                ...(profile.nickname && { nickname: profile.nickname }),
                ...(profile.avatarUrl || userInfo.avatar ? { avatar: profile.avatarUrl || userInfo.avatar } : {}),
                ...(profile.userIdString || userInfo.userIdString || userInfo.id ? { userIdString: profile.userIdString || userInfo.userIdString || userInfo.id } : {}),
                ...(userInfo.registerTime && { registerTime: userInfo.registerTime }),
                phone: profile.phoneNumber || '',  // 添加手机号显示
                gender: convertGenderToFrontend(profile.gender),  // 添加性别显示
                birthday: profile.birthday || '',  // 添加生日显示
                location: profile.location || '',  // 添加位置显示
                occupation: profile.occupation || '',  // 添加职业显示
                signature: profile.signature || ''  // 添加个性签名显示
              }
              
              // 更新用户状态
              if (profile.status && profile.status.text) {
                userStatus.value = { emoji: profile.status.emoji || '😊', text: profile.status.text }
              } else {
                userStatus.value = { emoji: '😊', text: '暂无状态' }
              }
            }
          }
        } catch (error) {
          console.error('获取用户资料失败:', error)
          // 使用基本用户信息作为后备
          currentUser.value = {
            id: userInfo.id,
            name: userInfo.name || userInfo.email,
            email: userInfo.email,
            ...(userInfo.nickname && { nickname: userInfo.nickname }),
            ...(userInfo.userIdString || userInfo.id ? { userIdString: userInfo.userIdString || userInfo.id } : {}),
            ...(userInfo.avatar && { avatar: userInfo.avatar }),
            ...(userInfo.registerTime && { registerTime: userInfo.registerTime })
          }
        }
        
        // 初始化各个列表
        await initChatList()
        await initContactsList()
        await loadFriendRequests()
        // 动态列表初始化已移至MomentView组件
        
        // 初始化文件管理
        await loadFileList()
        await loadFileStats()
      } else {
        // token已过期，清除登录信息并跳转到登录页
        clearLoginInfo()
        router.push('/login')
      }
    } else {
      // 用户未登录，跳转到登录页
      router.push('/login')
    }
  } catch (error) {
    console.error('初始化数据失败:', error)
    showErrorMessage('初始化失败，请重新登录')
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  }
}

// 定时刷新机制
let refreshInterval: number | null = null

// 检查状态是否过期
const checkStatusExpiry = () => {
  if (userProfile.value.statusExpiry) {
    const now = new Date()
    const expiryTime = new Date(userProfile.value.statusExpiry)
    
    if (now >= expiryTime) {
      // 状态已过期，清除状态
      userProfile.value.statusText = ''
      userProfile.value.statusEmoji = ''
      userProfile.value.statusExpiry = ''
      userStatus.value = { emoji: '😊', text: '暂无状态' }
      
      console.log('用户状态已过期，已自动清除')
    }
  }
}

// 启动定时刷新
const startAutoRefresh = () => {
  // 每30秒刷新一次会话列表和联系人列表，同时检查状态过期
  refreshInterval = window.setInterval(async () => {
    try {
      // 检查状态过期
      checkStatusExpiry()
      
      // 只在不是手动刷新时进行自动刷新
      if (!isLoading.value) {
        if (activeTab.value === 'chat') {
          await initChatList()
        } else if (activeTab.value === 'contacts') {
          await initContactsList()
        }
      }
    } catch (error) {
      console.error('自动刷新失败:', error)
    }
  }, 30000) // 30秒
}

// 停止定时刷新
const stopAutoRefresh = () => {
  if (refreshInterval) {
    window.clearInterval(refreshInterval)
    refreshInterval = null
  }
}

// 显示聊天选项菜单
const showChatOptionsMenu = (_event: Event, chat: Chat) => {
  selectedChat.value = chat
  showOptionsMenuVisible.value = true
}

// 处理长按开始
const handleChatTouchStart = (chat: Chat) => {
  touchTimer.value = window.setTimeout(() => {
    selectedChat.value = chat
    showOptionsMenuVisible.value = true
    // 触发震动反馈（如果支持）
    if (navigator.vibrate) {
      navigator.vibrate(50)
    }
  }, 500) // 500ms 长按
}

// 处理长按结束
const handleChatTouchEnd = () => {
  if (touchTimer.value) {
    clearTimeout(touchTimer.value)
    touchTimer.value = null
  }
}

// 标记为已读
const markAsRead = (chat: Chat | null) => {
  if (!chat || !chat.id) return
  
  // 这里应该调用API标记消息为已读
  console.log('标记为已读:', chat.name)
  
  // 更新本地状态
  const chatIndex = chats.value.findIndex(c => c.id === chat.id)
  if (chatIndex !== -1 && chats.value[chatIndex]) {
    chats.value[chatIndex].unreadCount = 0
  }
  
  hideOptionsMenu()
}

// 置顶聊天
const pinChat = (chat: Chat | null) => {
  if (!chat) return
  
  console.log('置顶聊天:', chat.name)
  // 这里应该调用API置顶聊天
  
  hideOptionsMenu()
}

// 消息免打扰
const muteChat = (chat: Chat | null) => {
  if (!chat) return
  
  console.log('消息免打扰:', chat.name)
  // 这里应该调用API设置免打扰
  
  hideOptionsMenu()
}

// 删除聊天
const deleteChat = (chat: Chat | null) => {
  if (!chat) return
  
  if (confirm(`确定要删除与 ${chat.name} 的聊天记录吗？`)) {
    console.log('删除聊天:', chat.name)
    // 这里应该调用API删除聊天
    
    // 更新本地状态
    const chatIndex = chats.value.findIndex(c => c.id === chat.id)
    if (chatIndex !== -1) {
      chats.value.splice(chatIndex, 1)
    }
  }
  
  hideOptionsMenu()
}

// 键盘事件处理
const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    if (showViewUserProfileModal.value) {
      closeViewUserProfileModal()
    }
  }
}

// 会话面板处理函数
// 处理会话选择
const handleSelectChat = (chat: any) => {
  console.log('选择会话:', chat);
  // 防止无限循环
  if (activeChatId.value === String(chat.id) && activeTab.value === 'chat') {
    console.log('会话已经是当前选中的，跳过处理');
    return;
  }
  
  // 确保chat.id是字符串类型
  activeChatId.value = String(chat.id);
  
  // 保存当前会话信息
  currentChatInfo.value = chat;
  
  // 切换到聊天标签页
  activeTab.value = 'chat';
  
  // 加载会话消息
  loadMessages(String(chat.id));
  
  // 不再触发select-chat事件，避免循环调用
};

// 处理会话置顶
const handlePinChat = async (chat: any) => {
  try {
    console.log('接收到置顶/取消置顶请求:', chat);
    
    // 确保我们有正确的会话ID
    const chatId = chat.chatId || chat.id;
    if (!chatId) {
      console.error('无效的会话ID:', chat);
      throw new Error('无效的会话ID');
    }
    
    // 确定是否置顶
    const isPinned = chat.isPinned !== undefined ? chat.isPinned : true;
    
    console.log(`执行${isPinned ? '置顶' : '取消置顶'}操作，会话ID: ${chatId}`);
    
    // 调用消息API进行置顶或取消置顶
    const response = await messageApi.pinConversation(chatId, isPinned);
    
    if (response.success) {
      console.log(`会话 ${chatId} ${isPinned ? '置顶' : '取消置顶'}成功`);
    
    // 重新加载会话列表
    await conversationsPanel.value?.loadConversations();
    
      const actionText = !isPinned ? '取消置顶' : '置顶';
      showSuccessMessage(`已${actionText}会话：${chat.name || '未命名会话'}`);
    } else {
      throw new Error(response.message || '操作失败');
    }
  } catch (error: any) {
    console.error('置顶/取消置顶操作失败:', error);
    showErrorMessage(`${chat?.isPinned ? '取消置顶' : '置顶'}失败：${error.message || '未知错误'}`);
  }
};

// 处理会话免打扰
const handleMute = async (chat: any) => {
  try {
    console.log('接收到免打扰/取消免打扰请求:', chat);
    
    // 确保我们有正确的会话ID
    const chatId = chat.chatId || chat.id;
    if (!chatId) {
      console.error('无效的会话ID:', chat);
      throw new Error('无效的会话ID');
    }
    
    // 确定是否免打扰
    const isDnd = chat.isDnd !== undefined ? !chat.isDnd : true;
    
    console.log(`执行${isDnd ? '免打扰' : '取消免打扰'}操作，会话ID: ${chatId}`);
    
    // 调用消息API进行免打扰设置
    const response = await messageApi.muteConversation(chatId, isDnd);
    
    if (response.success) {
      console.log(`会话 ${chatId} ${isDnd ? '免打扰' : '取消免打扰'}成功`);
    
    // 重新加载会话列表
    await conversationsPanel.value?.loadConversations();
    
      const actionText = !isDnd ? '取消免打扰' : '设置免打扰';
      showSuccessMessage(`已${actionText}会话：${chat.name || '未命名会话'}`);
    } else {
      throw new Error(response.message || '操作失败');
    }
  } catch (error: any) {
    console.error('免打扰/取消免打扰操作失败:', error);
    showErrorMessage(`${chat?.isDnd ? '取消免打扰' : '设置免打扰'}失败：${error.message || '未知错误'}`);
  }
};

// 处理会话归档
const handleArchiveChat = async (chat: any) => {
  try {
    console.log('接收到归档/取消归档请求:', chat);
    
    // 确保我们有正确的会话ID
    const chatId = chat.chatId || chat.id;
    if (!chatId) {
      throw new Error('无效的会话ID');
    }
    
    // 确定是归档还是取消归档操作
    const isArchived = chat.isArchived !== undefined ? chat.isArchived : true;
    
    console.log(`执行${isArchived ? '归档' : '取消归档'}操作，会话ID: ${chatId}`);
    
    // 调用消息API进行归档或取消归档
    const response = await messageApi.archiveConversation(chatId, isArchived);
    
    if (response.success) {
      console.log(`会话 ${chatId} ${isArchived ? '归档' : '取消归档'}成功`);
    
    // 重新加载会话列表
      if (isArchived) {
        // 如果是归档操作，重新加载常规会话列表
    await conversationsPanel.value?.loadConversations();
      } else {
        // 如果是取消归档操作，重新加载已归档会话列表
        await conversationsPanel.value?.loadArchivedConversations();
      }
    } else {
      throw new Error(response.message || '操作失败');
    }
  } catch (error: any) {
    console.error('归档/取消归档操作失败:', error);
    showErrorMessage(`${chat.isArchived ? '归档' : '取消归档'}失败：${error.message || '未知错误'}`);
  }
};

// 处理会话删除
const handleDeleteChat = async (chat: any) => {
  try {
    // 调用消息API进行删除
    await messageApi.deleteConversation(chat.id);
    
    // 重新加载会话列表
    await conversationsPanel.value?.loadConversations();
    
    showSuccessMessage(`已删除会话：${chat.name}`);
  } catch (error: any) {
    showErrorMessage(`删除失败：${error.message}`);
  }
};

// 处理面板错误
const handlePanelError = (error: string) => {
  console.error('会话面板错误:', error);
  
  // 忽略特定错误
  if (error === '无法获取用户ID') {
    console.warn('忽略用户ID错误，尝试使用默认ID');
    return;
  }
  
  if (error === 'UNAUTHORIZED') {
    showErrorMessage('登录已过期，请重新登录');
    // 处理登录过期逻辑
  } else {
    showErrorMessage(error);
  }
};

// 手动刷新会话列表
const refreshConversations = () => {
  if (conversationsPanel.value) {
    console.log('手动刷新会话列表');
    conversationsPanel.value.loadConversations();
  }
};

// 获取消息相关功能
const { totalUnreadCount, wsStatus } = useMessages();

// WebSocket连接状态
const wsConnectionStatus = computed(() => {
  return wsStatus.value === 'connected' ? true : false;
});

// 更新导航标签的未读消息数
const updateNavigationBadges = () => {
  if (navigationTabs.value && Array.isArray(navigationTabs.value) && navigationTabs.value.length >= 2) {
    // 明确检查数组索引是否存在
    if (navigationTabs.value[0]) {
      navigationTabs.value[0].badge = totalUnreadCount?.value ?? 0;
    }
    
    if (navigationTabs.value[1]) {
      navigationTabs.value[1].badge = friendRequestBadge.value ?? 0;
    }
  }
};

// 生命周期
onMounted(async () => {
  // 初始化共享WebSocket连接
  const { connect: connectWs } = useSharedWebSocket();
  connectWs();
  
  // 创建全局变量用于通知未读数量
  window.notificationUnreadCount = 0;
  
  // 监听全局通知未读数变化
  const checkNotificationCount = () => {
    if (window.notificationUnreadCount !== undefined) {
      notificationUnreadCount.value = window.notificationUnreadCount;
    }
  };
  
  // 定时检查通知未读数
  setInterval(checkNotificationCount, 2000);
  
  // 应用用户个性化设置
  const { applySettingsToUI } = getUserSettings();
  applySettingsToUI();
  
  await initData()
  // 启动定时刷新
  startAutoRefresh()
  // 添加键盘事件监听
  document.addEventListener('keydown', handleKeydown)
  // 初始化会话列表
  conversationsPanel.value?.loadConversations()
  
  // 延迟检查会话列表
  setTimeout(() => {
    console.log('检查会话列表状态，尝试手动刷新');
    refreshConversations();
  }, 2000);
  
  // 设置定时器更新导航标签的未读数
  badgeIntervalId = window.setInterval(updateNavigationBadges, 2000);
  
  // 初始更新一次
  setTimeout(updateNavigationBadges, 500);
});

// 用于存储定时器ID
let badgeIntervalId: number | null = null;

// 组件卸载时清理定时器和事件监听
onUnmounted(() => {
  stopAutoRefresh()
  // 移除键盘事件监听
  document.removeEventListener('keydown', handleKeydown)
  // 清除未读数更新定时器
  if (badgeIntervalId !== null) {
    clearInterval(badgeIntervalId)
  }
})
</script>

<style scoped>
/* 应用容器样式 */
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f7f7f7;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  position: relative;
}

/* 顶部状态栏样式 */
.status-bar {
  height: 44px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #e5e5e5;
  position: relative;
  z-index: 5;
}

.status-bar .title {
  font-size: 17px;
  font-weight: 600;
  color: #000;
}

.status-bar .right-btn {
  position: absolute;
  right: 16px;
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 4px;
}

/* 主内容区样式 */
.main-content {
  flex: 1;
  overflow: hidden;
  position: relative;
}

/* 标签页内容样式 */
.tab-content {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.3s ease;
  overflow-y: auto;
}

.tab-content.active {
  opacity: 1;
  visibility: visible;
}

/* 底部标签栏样式 */
.tab-bar {
  height: 83px;
  background: #fff;
  border-top: 1px solid #e5e5e5;
  display: flex;
  padding-bottom: env(safe-area-inset-bottom);
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.3s ease;
  padding: 8px 0;
  position: relative;
}

.tab-item.active {
  color: #07c160;
}

.tab-item .icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.tab-item .label {
  font-size: 10px;
  font-weight: 400;
}

.tab-item .badge {
  position: absolute;
  top: 4px;
  right: 20px;
  background: #ff4444;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 10px;
  min-width: 16px;
  text-align: center;
  line-height: 12px;
  font-weight: bold;
}

/* 聊天页面样式 */
.chats-page {
  background: #fff;
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: row;
  height: 100%;
}

/* 会话列表面板 */
.conversations-list-panel {
  width: 300px;
  border-right: 1px solid #e5e5e5;
  overflow-y: auto;
  flex-shrink: 0;
}

/* 聊天内容面板 */
.chat-content-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #f8f8f8;
}

.chat-header {
  height: 60px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e5e5e5;
  background-color: #fff;
}

.chat-title {
  font-size: 16px;
  font-weight: 600;
}

.message-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.no-messages {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #999;
}

.no-messages-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #ddd;
}

.no-messages-text {
  font-size: 18px;
  margin-bottom: 8px;
}

.no-messages-hint {
  font-size: 14px;
}

.message-input-area {
  height: 120px;
  border-top: 1px solid #e5e5e5;
  padding: 15px;
  display: flex;
  background-color: #fff;
}

.message-input {
  flex: 1;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  padding: 10px;
  resize: none;
  font-size: 14px;
  outline: none;
  margin-right: 10px;
}

.send-btn {
  width: 80px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.send-btn:hover {
  background-color: #0069d9;
}

.empty-chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #999;
  background-color: #f8f8f8;
}

.empty-chat-icon {
  font-size: 64px;
  margin-bottom: 20px;
  color: #ddd;
}

.empty-chat-text {
  font-size: 18px;
}

.search-input {
  width: 100%;
  height: 36px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 18px;
  padding: 0 16px;
  font-size: 14px;
  outline: none;
}

.chat-list {
  padding: 0;
}

.chats-loading,
.no-chats {
  text-align: left;
  padding: 16px 20px 16px 64px;
  color: #666;
  font-size: 14px;
  margin: 0;
  line-height: 20px;
  height: 52px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
}

.chat-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fff;
}

.chat-item:hover {
  background: #f8f9fa;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.chat-item:active {
  background: #e9ecef;
}

.chat-avatar {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 20px;
  margin-right: 12px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
  overflow: hidden;
}

.chat-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.chat-info {
  flex: 1;
  min-width: 0;
  padding: 2px 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.chat-name {
  font-size: 17px;
  font-weight: 600;
  color: #1a1a1a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.3;
}

.chat-time {
  font-size: 12px;
  color: #8e8e93;
  flex-shrink: 0;
  margin-left: 12px;
  font-weight: 500;
}

.chat-preview {
  font-size: 14px;
  color: #8e8e93;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex;
  align-items: center;
  line-height: 1.4;
}

.chat-badge {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a52 100%);
  color: white;
  border-radius: 12px;
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 600;
  min-width: 20px;
  text-align: center;
  margin-left: 10px;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(255, 107, 107, 0.3);
}

/* 联系人页面样式 */
.contacts-page {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.contacts-header {
  padding: 8px 16px;
  background: #f7f7f7;
}

.search-container {
  position: relative;
  display: flex;
  align-items: center;
}

.search-input {
  width: 100%;
  height: 36px;
  background: #f8f9fa;
  border: 2px solid #d0d7de;
  border-radius: 18px;
  padding: 0 40px 0 16px;
  font-size: 14px;
  outline: none;
  color: #24292f;
  transition: all 0.2s ease;
}

.search-input:focus {
  background: white;
  border-color: #0969da;
  box-shadow: 0 0 0 3px rgba(9, 105, 218, 0.1);
}

.search-input::placeholder {
  color: #656d76;
}

.search-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: #07c160;
  color: white;
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.contacts-functions {
  background: white;
  border-bottom: 1px solid #f0f0f0;
  text-align: left;
}

.function-item {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 12px 16px 12px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  text-align: left;
  transition: background-color 0.2s;
}

.function-item:hover {
  background-color: #f8f9fa;
}

.function-item:active {
  background: #f0f0f0;
}

.function-item:last-child {
  border-bottom: none;
}

.function-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-right: 12px;
}

.function-icon.new-friends {
  background: #ff9500;
  color: white;
}

.function-icon.group-chats {
  background: #07c160;
  color: white;
}

.function-icon.tags {
  background: #576b95;
  color: white;
}

.function-text {
  flex: 1;
  font-size: 16px;
  color: #000;
}

.contacts-list {
  flex: 1;
  overflow-y: auto;
  background: white;
}

.contacts-loading,
.no-contacts {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  font-size: 14px;
}

.contact-item {
  position: relative;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.3s ease;
}

.contact-item:hover {
  background: #f8f9fa;
}

.contact-item:active {
  background: #e9ecef;
}

.contact-avatar {
  width: 48px;
  height: 48px;
  border-radius: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 18px;
  margin-right: 12px;
  flex-shrink: 0;
  overflow: hidden;
}

.contact-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.contact-info {
  flex: 1;
  min-width: 0;
}

.contact-name {
  font-size: 16px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.contact-signature {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 添加标签显示样式 */
.contact-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}

.contact-tag {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  color: white;
  background-color: #667eea;
  max-width: 100px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 修改联系人菜单样式，确保向上弹出 */
.contact-menu {
  position: fixed;
  width: 180px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  padding: 8px 0;
  transform-origin: bottom center;
  animation: menuFadeIn 0.2s ease;
  max-height: 300px;
  overflow-y: auto;
  bottom: auto;
  margin-bottom: 10px;
}

/* 修改菜单定位逻辑，使其向上弹出 */
@keyframes menuFadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.menu-item {
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.menu-item:hover {
  background: #f0f7ff;
}

.menu-item.danger:hover {
  background: #fff5f5;
  color: #e53e3e;
}

.menu-item-icon {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.menu-item-text {
  font-size: 14px;
}

/* 动态页面样式 */
.moments-page {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.moments-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
}

.moments-title {
  font-size: 17px;
  font-weight: 600;
  color: #000;
}

.publish-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 4px;
}

.moments-list {
  flex: 1;
  overflow-y: auto;
  background: white;
}

.no-moments {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  font-size: 14px;
}

.moment-item {
  display: flex;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.moment-avatar {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  background: #07c160;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 16px;
  margin-right: 12px;
  overflow: hidden;
}

.moment-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.moment-content {
  flex: 1;
  min-width: 0;
}

.moment-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.moment-author {
  font-size: 16px;
  font-weight: 500;
  color: #000;
  margin-right: 8px;
}

.moment-time {
  font-size: 12px;
  color: #999;
}

.moment-text {
  font-size: 16px;
  color: #000;
  line-height: 1.4;
  margin-bottom: 8px;
}

.moment-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  margin-bottom: 12px;
}

.moment-image {
  width: 100%;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
}

.moment-actions {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}

.moment-action {
  background: none;
  border: none;
  color: #999;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 0;
}

/* 个人资料页面样式 */
.profile-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f7f7f7;
}

.profile-header {
  background: white;
  margin-bottom: 10px;
}

.profile-cover {
  display: flex;
  align-items: center;
  padding: 20px 16px;
}

.profile-avatar {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  background: #07c160;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 24px;
  margin-right: 16px;
  overflow: hidden;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.profile-name {
  font-size: 20px;
  font-weight: 600;
  color: #000;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-id {
  font-size: 14px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.profile-status {
  font-size: 14px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qr-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #999;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.qr-btn:hover {
  background: #f0f0f0;
}

.profile-functions {
  background: white;
  margin-bottom: 10px;
  text-align: left;
}

.function-item {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  text-align: left;
  transition: background-color 0.2s;
}

.function-item:active {
  background: #f0f0f0;
}

.function-item:last-child {
  border-bottom: none;
}

.function-icon {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  background: #007aff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-right: 12px;
}

.function-text {
  flex: 1;
  font-size: 16px;
  color: #000;
}

.function-arrow {
  font-size: 16px;
  color: #999;
}

.profile-settings {
  background: white;
  text-align: left;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  text-align: left;
}

.setting-item:active {
  background: #f0f0f0;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-icon {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  background: #007aff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-right: 12px;
}

.setting-text {
  flex: 1;
  font-size: 16px;
  color: #000;
}

.setting-arrow {
  font-size: 16px;
  color: #999;
}

/* 选项菜单样式 */
.options-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  cursor: pointer;
}

.options-menu {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-radius: 16px 16px 0 0;
  padding: 20px;
  z-index: 1001;
  transform: translateY(100%);
  transition: transform 0.3s ease;
  max-height: 80vh;
  overflow-y: auto;
}

.options-menu.show {
  transform: translateY(0);
}

.options-menu .menu-item {
  display: flex;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.options-menu .menu-item:last-child {
  border-bottom: none;
}

.options-menu .menu-item:hover {
  background-color: #f8f9fa;
}

.options-menu .menu-item .icon {
  width: 24px;
  height: 24px;
  margin-right: 12px;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.options-menu .menu-item .text {
  flex: 1;
  font-size: 16px;
  color: #333;
}

.options-menu .menu-item.danger .text {
  color: #ff4444;
}

/* 消息样式 */
.message {
  margin-bottom: 16px;
  display: flex;
}

.message.own {
  justify-content: flex-end;
}

.message.other {
  justify-content: flex-start;
}

.message-content {
  max-width: 70%;
  background: #f0f0f0;
  padding: 12px 16px;
  border-radius: 18px;
  position: relative;
}

.message.own .message-content {
  background: #007bff;
  color: white;
}

.message-text {
  word-wrap: break-word;
  line-height: 1.4;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  text-align: right;
}

.message.own .message-time {
  color: rgba(255, 255, 255, 0.7);
}

.no-messages {
  text-align: center;
  color: #999;
  padding: 40px 20px;
  font-size: 14px;
}

/* 编辑个人资料模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}

.modal-container {
  background: white;
  border-radius: 16px;
  max-width: 600px;
  width: 90%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  animation: modalSlideIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: hidden;
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.modal-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}

.close-btn {
  background: none;
  border: none;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #666;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: #f0f0f0;
  color: #333;
  transform: scale(1.05);
}

.modal-content {
  flex: 1;
  overflow-y: auto;
  padding: 32px;
}

/* 表单区域 */
.form-section {
  margin-bottom: 32px;
}

.section-title {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  padding-bottom: 8px;
  border-bottom: 2px solid #f0f0f0;
}

/* 头像设置区域 */
.avatar-section {
  margin-bottom: 32px;
}

.avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.avatar-preview {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 36px;
  font-weight: 600;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s ease;
}

.avatar-preview:hover {
  transform: scale(1.05);
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
  cursor: pointer;
  color: white;
}

.avatar-preview:hover .avatar-overlay {
  opacity: 1;
}

.avatar-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 表单样式 */
.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 4px;
}

.form-input,
.form-select,
.form-textarea {
  padding: 12px 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s ease;
  background: white;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input.readonly {
  background: #f9fafb;
  color: #6b7280;
  cursor: not-allowed;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
  font-family: inherit;
}

.form-hint {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-row .form-group {
  margin-bottom: 0;
}

/* 状态设置区域 */
.status-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 20px;
}

.current-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-emoji {
  font-size: 20px;
}

.status-text {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.status-form {
  background: #f8fafc;
  padding: 20px;
  border-radius: 8px;
  margin-top: 16px;
}

.radio-group {
  display: flex;
  gap: 20px;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.radio-item input[type="radio"] {
  margin: 0;
}

.radio-label {
  font-size: 14px;
  color: #374151;
}

.preset-status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.preset-status-item {
  padding: 12px 16px;
  border: 1px solid #d1d5db;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  text-align: center;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.preset-status-item:hover {
  border-color: #3b82f6;
  background: #eff6ff;
}

.preset-emoji {
  font-size: 18px;
}

.preset-text {
  font-size: 12px;
  color: #6b7280;
}

.duration-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.duration-btn {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s ease;
}

.duration-btn.active {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.duration-btn:hover:not(.active) {
  background: #f3f4f6;
}

.status-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

/* 模态框底部 */
.modal-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 24px 32px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

/* 按钮样式 */
.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 80px;
}

.btn-sm {
  padding: 8px 16px;
  font-size: 12px;
  min-width: 60px;
}

.btn-primary {
  background: #3b82f6;
  color: white;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.2);
}

.btn-primary:hover {
  background: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(59, 130, 246, 0.3);
}

.btn-secondary {
  background: #6b7280;
  color: white;
}

.btn-secondary:hover {
  background: #4b5563;
}

.btn-outline {
  background: transparent;
  color: #3b82f6;
  border: 1px solid #3b82f6;
}

.btn-outline:hover {
  background: #3b82f6;
  color: white;
}

.btn-text {
  background: transparent;
  color: #6b7280;
  border: none;
  padding: 8px 12px;
}

.btn-text:hover {
  color: #374151;
  background: #f3f4f6;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .modal-container {
    width: 95%;
    margin: 20px;
    max-height: 95vh;
  }
  
  .modal-header {
    padding: 20px 24px;
  }
  
  .modal-content {
    padding: 24px;
  }
  
  .modal-footer {
    padding: 20px 24px;
    flex-direction: column;
  }
  
  .form-row {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .preset-status-grid {
    grid-template-columns: 1fr;
  }
  
  .radio-group {
    flex-direction: column;
    gap: 12px;
  }
  
  .duration-options {
    justify-content: center;
  }
  
  .status-actions {
    flex-direction: column;
  }
  
  .avatar-actions {
    flex-direction: column;
    width: 100%;
  }
}

/* 文件上传模态框样式 */
.file-upload-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.file-upload-content {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 900px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  animation: modalSlideIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.file-upload-header {
  padding: 24px 32px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fafafa;
}

.file-upload-header h3 {
  margin: 0;
  color: #1a1a1a;
  font-size: 20px;
  font-weight: 600;
}

.file-upload-body {
  padding: 32px;
}

/* 文件管理和系统通知页面样式 */
.file-manager-page,
.system-notifications-page {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: white;
  z-index: 100;
  overflow-y: auto;
  padding: 0;
}

.file-manager-page.active,
.system-notifications-page.active {
  display: block;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: white;
  margin: 0;
}

.back-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #3b82f6;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: background-color 0.2s ease;
}

.back-btn:hover {
  background: #f0f4ff;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.refresh-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #6b7280;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.refresh-btn:hover {
  background: #f3f4f6;
  transform: rotate(180deg);
}

/* 文件选项卡内容 */
.file-tab-content {
  padding: 0 20px 20px 20px;
}

/* 文件统计 */
.file-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
  margin: 20px;
  padding: 0;
}

.stat-item {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  padding: 20px;
  border-radius: 12px;
  text-align: center;
  border: 1px solid #dee2e6;
  transition: all 0.2s ease;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: #495057;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 12px;
  color: #6c757d;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 500;
}

/* 选项卡 */
.file-tabs {
  display: flex;
  border-bottom: 2px solid #f0f0f0;
  margin: 0 20px 24px 20px;
}

.file-tab {
  padding: 16px 24px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #6c757d;
  border-bottom: 2px solid transparent;
  transition: all 0.2s ease;
}

.file-tab.active {
  color: #3b82f6;
  border-bottom-color: #3b82f6;
}

.file-tab:hover {
  color: #495057;
}

/* 文件警报 */
.file-alert {
  padding: 16px 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 500;
}

.file-alert.success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.file-alert.error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

/* 文件上传区域 */
.upload-area {
  border: 2px dashed #dee2e6;
  border-radius: 12px;
  padding: 60px 20px;
  text-align: center;
  background: #f8f9fa;
  transition: all 0.3s ease;
  cursor: pointer;
  margin-bottom: 24px;
}

.upload-area:hover {
  border-color: #3b82f6;
  background: #f0f4ff;
}

.upload-area.dragover {
  border-color: #3b82f6;
  background: #e3f2fd;
  transform: scale(1.02);
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.15);
}

.upload-icon {
  font-size: 64px;
  color: #6c757d;
  margin-bottom: 20px;
}

.upload-text {
  font-size: 18px;
  color: #495057;
  margin-bottom: 8px;
  font-weight: 500;
}

.upload-hint {
  font-size: 14px;
  color: #6c757d;
}

.file-input {
  display: none;
}

/* 上传选项 */
.upload-options {
  margin-bottom: 24px;
}

.option-group {
  margin-bottom: 20px;
}

.option-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #495057;
  font-size: 14px;
}

.option-group select,
.option-group input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  transition: border-color 0.2s ease;
}

.option-group select:focus,
.option-group input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.image-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 12px;
}

/* 上传按钮区域 */
.upload-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.upload-buttons .btn {
  flex: 1;
}

/* 上传进度 */
.upload-progress {
  margin-bottom: 24px;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: #e9ecef;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 12px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6 0%, #1d4ed8 100%);
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 14px;
  color: #6c757d;
  text-align: center;
  font-weight: 500;
}

/* 文件筛选 */
.file-filter {
  margin-bottom: 24px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.file-filter select {
  width: 200px;
  padding: 12px 16px;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  font-size: 14px;
  background: white;
}

.file-action-btn {
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.2s ease;
}

.file-action-btn:hover {
  background: #f3f4f6;
}

/* 文件加载 */
.file-loading {
  text-align: center;
  padding: 60px;
  color: #6c757d;
  font-size: 16px;
}

/* 文件列表 */
.file-list {
  max-height: 400px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  margin-bottom: 12px;
  background: white;
  transition: all 0.2s ease;
}

.file-item:hover {
  background: #f8f9fa;
  border-color: #3b82f6;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.file-icon {
  font-size: 28px;
  margin-right: 16px;
  width: 50px;
  text-align: center;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-weight: 500;
  color: #495057;
  margin-bottom: 6px;
  word-break: break-all;
  font-size: 14px;
}

.file-details {
  font-size: 12px;
  color: #6c757d;
  display: flex;
  gap: 20px;
}

.file-actions {
  display: flex;
  gap: 8px;
}

.file-actions .btn {
  padding: 8px 12px;
  font-size: 12px;
  min-width: auto;
}

/* 空状态 */
.no-files {
  text-align: center;
  padding: 60px;
  color: #6c757d;
  font-size: 16px;
}

.empty-state {
  text-align: center;
  padding: 60px;
  color: #6c757d;
}

.empty-state .icon {
  font-size: 64px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.empty-state .text {
  font-size: 18px;
  margin-bottom: 8px;
  font-weight: 500;
}

.empty-state .hint {
  font-size: 14px;
  opacity: 0.7;
}

/* 响应式设计 - 文件上传 */
@media (max-width: 768px) {
  .file-upload-content {
    width: 95%;
    margin: 20px;
  }
  
  .file-upload-header {
    padding: 20px 24px;
  }
  
  .file-upload-body {
    padding: 24px;
  }
  
  .file-stats {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .image-options {
    grid-template-columns: 1fr;
  }
  
  .upload-buttons {
    flex-direction: column;
  }
  
  .file-filter select {
    width: 100%;
  }
  
  .file-details {
    flex-direction: column;
    gap: 4px;
  }
  
  .file-actions {
    flex-direction: column;
  }
}

/* 账户与安全页面样式 */
.account-security-page {
  padding: 0;
}

.security-functions {
  padding: 20px;
}

.security-function-item {
  display: flex;
  align-items: center;
  padding: 20px;
  background: white;
  border-radius: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #f0f0f0;
}

.security-function-item:hover {
  background: #f8f9fa;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.security-function-icon {
  font-size: 24px;
  margin-right: 16px;
  width: 40px;
  text-align: center;
}

.security-function-text {
  flex: 1;
  font-size: 16px;
  font-weight: 500;
  color: #495057;
}

.security-function-arrow {
  font-size: 20px;
  color: #6c757d;
}

/* 密码管理模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-container {
  background: white;
  border-radius: 12px;
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
}

.modal-container.device-modal {
  max-width: 700px;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: #495057;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #6c757d;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: #f8f9fa;
  color: #495057;
}

.modal-content {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #495057;
  font-size: 14px;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s ease;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.alert {
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 14px;
}

.alert.error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.alert.success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 80px;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #2563eb;
}

.btn-secondary:hover:not(:disabled) {
  background: #5a6268;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #c82333;
}

.btn-sm {
  padding: 8px 16px;
  font-size: 12px;
  min-width: 60px;
}

/* 设备管理样式 */
.device-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.device-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.device-list {
  max-height: 400px;
  overflow-y: auto;
}

.device-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  margin-bottom: 12px;
  background: white;
  transition: all 0.2s ease;
}

.device-item:hover {
  background: #f8f9fa;
  border-color: #3b82f6;
}

.device-icon {
  font-size: 24px;
  margin-right: 16px;
  width: 40px;
  text-align: center;
}

.device-info {
  flex: 1;
  min-width: 0;
}

.device-name {
  font-weight: 500;
  color: #495057;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.current-device-badge {
  background: #28a745;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.device-details {
  font-size: 12px;
  color: #6c757d;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.device-status {
  font-weight: 500;
}

.device-status.online {
  color: #28a745;
}

.device-actions {
  display: flex;
  gap: 8px;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #6c757d;
  font-size: 16px;
}

.empty-devices {
  text-align: center;
  padding: 60px;
  color: #6c757d;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  font-size: 16px;
}

/* 确认模态框样式 */
.modal-container.confirm-modal {
  max-width: 450px;
}

.confirm-icon {
  text-align: center;
  font-size: 48px;
  margin-bottom: 20px;
}

.confirm-icon.danger {
  color: #dc3545;
}

.confirm-icon.warning {
  color: #ffc107;
}

.confirm-icon.info {
  color: #17a2b8;
}

.confirm-message {
  text-align: center;
  font-size: 16px;
  line-height: 1.5;
  color: #495057;
  margin-bottom: 24px;
  white-space: pre-line;
}

.btn-warning {
  background: #ffc107;
  color: #212529;
}

.btn-warning:hover:not(:disabled) {
  background: #e0a800;
}

/* 禁用按钮样式 */
.disabled-btn {
  opacity: 0.6;
  cursor: not-allowed;
  pointer-events: none;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .modal-container {
    margin: 10px;
    max-width: none;
  }
  
  .modal-container.confirm-modal {
    max-width: none;
  }
  
  .device-stats {
    grid-template-columns: 1fr;
  }
  
  .device-actions {
    flex-direction: column;
  }
  
  .device-details {
    font-size: 11px;
  }
  
  .modal-actions {
    flex-direction: column;
  }
  
  .confirm-message {
    font-size: 14px;
  }
}

/* 好友请求模态框样式 */
.friend-requests-tabs {
  display: flex;
  border-bottom: 1px solid #dee2e6;
  margin-bottom: 20px;
}

.tab-btn {
  flex: 1;
  padding: 12px 16px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: #6c757d;
  transition: all 0.2s ease;
  border-bottom: 2px solid transparent;
}

.tab-btn.active {
  color: #3b82f6;
  border-bottom-color: #3b82f6;
  font-weight: 500;
}

.tab-btn:hover {
  color: #3b82f6;
  background: #f8f9fa;
}

.requests-list {
  max-height: 400px;
  overflow-y: auto;
}

.request-item {
  background: white;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #f0f2f5;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
}

.request-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
}

.request-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(102, 126, 234, 0.2);
  border-color: #667eea;
}

.request-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: bold;
  margin-right: 16px;
  flex-shrink: 0;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.3);
  border: 3px solid white;
  position: relative;
  overflow: hidden;
}

.request-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.request-avatar::after {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  padding: 3px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask-composite: exclude;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: bold;
  color: white;
}

.request-info {
  flex: 1;
  min-width: 0;
}

.request-name {
  font-size: 20px;
  font-weight: 700;
  color: #1a202c;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.request-name::before {
  content: '👤';
  font-size: 18px;
  opacity: 0.8;
}

.request-message {
  font-size: 16px;
  color: #4a5568;
  margin-bottom: 8px;
  line-height: 1.6;
  font-weight: 500;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  border-radius: 12px;
  border-left: 4px solid #e2e8f0;
}

.request-time {
  font-size: 14px;
  color: #718096;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.request-time::before {
  content: '🕐';
  font-size: 14px;
}

.request-status {
  font-size: 13px;
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
  border: 1px solid #f59e0b;
}

.request-status::before {
  content: '⏳';
}

.request-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex-shrink: 0;
  align-items: flex-end;
}

/* 联系人操作菜单样式 */
.contact-menu {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  opacity: 0;
  visibility: hidden;
  transition: all 0.3s ease;
}

.contact-menu.show {
  opacity: 1;
  visibility: visible;
}

.contact-menu-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
}

.contact-menu-content {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-radius: 16px 16px 0 0;
  transform: translateY(100%);
  transition: transform 0.3s ease;
}

.contact-menu.show .contact-menu-content {
  transform: translateY(0);
}

.contact-menu-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.contact-menu-title {
  font-size: 18px;
  font-weight: 600;
  color: #000;
}

.contact-menu-close {
  background: none;
  border: none;
  font-size: 20px;
  color: #999;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}

.contact-menu-close:hover {
  background-color: #f0f0f0;
}

.contact-menu-actions {
  padding: 8px 0 20px 0;
}

.contact-menu-action {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 16px 20px;
  background: none;
  border: none;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.2s;
}

.contact-menu-action:hover {
  background-color: #f8f9fa;
}

.contact-menu-action.danger {
  color: #dc3545;
}

.contact-menu-action.danger:hover {
  background-color: #fff5f5;
}

.action-icon {
  font-size: 20px;
  margin-right: 12px;
  width: 24px;
  text-align: center;
}

.action-text {
  font-size: 16px;
  flex: 1;
}

/* 设置备注模态框专用样式 */
.alias-modal {
  max-width: 480px;
}

.alias-modal .modal-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px 24px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.header-text {
  flex: 1;
}

.modal-subtitle {
  margin: 4px 0 0 0;
  font-size: 14px;
  opacity: 0.9;
  font-weight: 400;
}

.alias-modal .close-btn {
  color: white;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.alias-modal .close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.contact-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
  margin-bottom: 24px;
}

.contact-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.contact-details {
  flex: 1;
}

.contact-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.contact-label {
  font-size: 12px;
  color: #6c757d;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.alias-modal .form-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.alias-input {
  font-size: 16px;
  padding: 14px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  transition: all 0.3s ease;
  background: #fafafa;
}

.alias-input:focus {
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.input-hint {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  font-size: 12px;
}

.char-count {
  color: #667eea;
  font-weight: 500;
}

.hint-text {
  color: #6c757d;
  flex: 1;
  margin-left: 12px;
}

.alias-modal .modal-footer {
  padding: 20px 24px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}

.alias-modal .btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.alias-modal .btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.alias-modal .btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.alias-modal .btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.2);
}

.alias-modal .btn-secondary {
  background: white;
  border: 2px solid #e5e7eb;
  color: #6c757d;
}

.alias-modal .btn-secondary:hover {
  border-color: #d1d5db;
  background: #f9fafb;
  color: #374151;
}

/* 标签相关样式 */
.tag-list {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  padding: 12px;
}

.tag-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s ease;
  margin-bottom: 4px;
}

.tag-item:hover {
  background: #f8f9fa;
}

.tag-item input[type="checkbox"] {
  margin-right: 8px;
}

.tag-name {
  font-size: 14px;
  color: #495057;
}

.tags-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.tags-list {
  max-height: 400px;
  overflow-y: auto;
}

.tag-item-row {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 1px solid #dee2e6;
  border-radius: 12px;
  margin-bottom: 12px;
  background: white;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.tag-item-row:hover {
  background: #f8f9fa;
  border-color: #667eea;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.15);
}

.tag-color-indicator {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  margin-right: 12px;
  flex-shrink: 0;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.color-selection {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.color-input {
  width: 50px;
  height: 40px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  background: none;
}

.color-input::-webkit-color-swatch-wrapper {
  padding: 0;
  border: none;
  border-radius: 8px;
}

.color-input::-webkit-color-swatch {
  border: 2px solid #e9ecef;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.color-input::-webkit-color-swatch:hover {
  border-color: #667eea;
}

.tag-info {
  flex: 1;
  cursor: pointer;
  margin-left: 8px;
}

.tag-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.tag-info .tag-name {
  font-weight: 500;
  color: #495057;
  margin-bottom: 4px;
  font-size: 14px;
}

.tag-count {
  color: #6c757d;
  font-size: 12px;
}

/* 标签管理模态框样式美化 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-container {
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  max-width: 500px;
  width: 90%;
  max-height: 80vh;
  overflow: hidden;
  animation: slideUp 0.3s ease;
}

.modal-container.large {
  max-width: 800px;
  width: 95%;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  padding: 24px 24px 16px 24px;
  border-bottom: 1px solid #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.modal-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
  color: white;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.1);
}

.modal-content {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #f0f2f5;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  background: #f8f9fa;
}

.tags-header {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-primary:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: #6c757d;
  color: white;
  box-shadow: 0 2px 4px rgba(108, 117, 125, 0.3);
}

.btn-secondary:hover {
  background: #5a6268;
  transform: translateY(-2px);
}

.btn-danger {
  background: #dc3545;
  color: white;
  box-shadow: 0 2px 4px rgba(220, 53, 69, 0.3);
}

.btn-danger:hover {
  background: #c82333;
  transform: translateY(-2px);
}

.btn-text {
  background: none;
  color: #667eea;
  padding: 6px 12px;
  font-size: 13px;
}

.btn-text:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #5a6fd8;
}

.btn-text.danger {
  color: #dc3545;
}

.btn-text.danger:hover {
  background: rgba(220, 53, 69, 0.1);
  color: #c82333;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #6c757d;
}

.empty-state p {
  font-size: 16px;
  margin: 0;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  font-size: 16px;
  transition: all 0.3s ease;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.confirm-content {
  text-align: center;
  padding: 20px 0;
}

.confirm-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.warning-text {
  color: #dc3545;
  font-size: 14px;
  margin-top: 8px;
}

/* 好友请求页面样式 */
.friend-requests-page {
  padding: 0;
  background: #fff;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.friend-request-stats {
  display: flex;
  justify-content: space-around;
  gap: 12px;
  margin: 0;
  padding: 12px 20px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
}

.friend-request-stats .stat-item {
  text-align: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 8px;
  flex: 1;
  min-width: 0;
}

.friend-request-stats .stat-number {
  font-size: 18px;
  font-weight: bold;
  color: #667eea;
  margin-bottom: 4px;
}

.friend-request-stats .stat-label {
  font-size: 11px;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.friend-request-tabs {
  display: flex;
  background: white;
  border-radius: 12px;
  margin: 16px 20px 20px 20px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.friend-request-tab {
  flex: 1;
  padding: 15px 20px;
  text-align: center;
  cursor: pointer;
  background: white;
  color: #666;
  font-weight: 500;
  transition: all 0.3s ease;
  border-bottom: 3px solid transparent;
}

.friend-request-tab:hover {
  background: #f8f9fa;
  color: #333;
}

.friend-request-tab.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-color: #667eea;
}

.friend-request-content-area {
  flex: 1;
  overflow-y: auto;
}

.friend-request-section {
  width: 100%;
  height: 100%;
}

.friend-request-list {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin: 0 20px;
  padding: 16px;
}

.friend-request-item {
  background: white;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #f0f2f5;
  position: relative;
  overflow: hidden;
}

.friend-request-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
}

.friend-request-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(102, 126, 234, 0.2);
  border-color: #667eea;
}

.friend-request-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.friend-request-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: bold;
  margin-right: 16px;
  flex-shrink: 0;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.3);
  border: 3px solid white;
  position: relative;
  overflow: hidden;
}

.friend-request-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.friend-request-avatar::after {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  padding: 3px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask-composite: exclude;
}

.friend-request-user-info {
  flex: 1;
}

.friend-request-name {
  font-size: 20px;
  font-weight: 700;
  color: #1a202c;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.friend-request-name::before {
  content: '👤';
  font-size: 18px;
  opacity: 0.8;
}

.friend-request-id {
  font-size: 14px;
  color: #718096;
  font-weight: 500;
}

.friend-request-content {
  margin-bottom: 16px;
}

.friend-request-message {
  font-size: 16px;
  color: #4a5568;
  margin-bottom: 12px;
  line-height: 1.6;
  font-weight: 500;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  border-radius: 12px;
  border-left: 4px solid #e2e8f0;
}

.friend-request-verification {
  font-size: 15px;
  color: #2d3748;
  background: linear-gradient(135deg, #e6fffa 0%, #f0fff4 100%);
  padding: 16px 20px;
  border-radius: 12px;
  border-left: 4px solid #38b2ac;
  line-height: 1.6;
  position: relative;
  box-shadow: 0 2px 10px rgba(56, 178, 172, 0.1);
}

.friend-request-verification::before {
  content: '💬';
  position: absolute;
  left: -2px;
  top: -2px;
  background: #38b2ac;
  color: white;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  box-shadow: 0 2px 8px rgba(56, 178, 172, 0.3);
}

.friend-request-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.friend-request-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.friend-request-time {
  font-size: 14px;
  color: #718096;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.friend-request-time::before {
  content: '🕐';
  font-size: 14px;
}

.friend-request-status {
  font-size: 13px;
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.friend-request-status.pending {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
  border: 1px solid #f59e0b;
}

.friend-request-status.pending::before {
  content: '⏳';
}

.friend-request-status.accepted {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  color: #065f46;
  border: 1px solid #10b981;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.2);
  animation: statusPulse 2s ease-in-out infinite;
}

.friend-request-status.accepted::before {
  content: '✅';
}

.friend-request-status.rejected {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #991b1b;
  border: 1px solid #ef4444;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.2);
}

.friend-request-status.rejected::before {
  content: '❌';
}

.friend-request-status.expired {
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  color: #6b7280;
  border: 1px solid #9ca3af;
  box-shadow: 0 2px 8px rgba(156, 163, 175, 0.2);
}

.friend-request-status.expired::before {
  content: '⏰';
}

@keyframes statusPulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 2px 8px rgba(16, 185, 129, 0.2);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
  }
}

.friend-request-header-actions {
  display: flex;
  align-items: center;
  margin-left: auto;
}

.friend-request-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex-shrink: 0;
  align-items: flex-end;
}

.friend-request-btn {
  padding: 12px 20px;
  border: none;
  border-radius: 25px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  min-width: 90px;
  position: relative;
  overflow: hidden;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.friend-request-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.5s;
}

.friend-request-btn:hover::before {
  left: 100%;
}

.friend-request-btn.accept {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  border: 2px solid transparent;
}

.friend-request-btn.accept:hover {
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 8px 25px rgba(16, 185, 129, 0.4);
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
}

.friend-request-btn.reject {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  border: 2px solid transparent;
}

.friend-request-btn.reject:hover {
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 8px 25px rgba(239, 68, 68, 0.4);
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
}

.friend-request-btn.cancel {
  background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%);
  color: white;
  border: 2px solid transparent;
}

.friend-request-btn.cancel:hover {
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 8px 25px rgba(107, 114, 128, 0.4);
  background: linear-gradient(135deg, #4b5563 0%, #374151 100%);
}

.friend-request-btn.view-profile {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: 2px solid transparent;
}

.friend-request-btn.view-profile:hover {
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.4);
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
}

.friend-request-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1) !important;
  background: #9ca3af !important;
}

.friend-request-btn:active {
  transform: translateY(-1px) scale(0.98);
}

.friend-request-loading {
  text-align: center;
  padding: 40px;
  color: #666;
}

.friend-request-loading .spinner {
  border: 3px solid #f3f3f3;
  border-top: 3px solid #667eea;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  animation: spin 1s linear infinite;
  margin: 0 auto 10px;
}

.no-requests {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.no-requests-icon {
  font-size: 48px;
  margin-bottom: 15px;
  opacity: 0.5;
}

.no-requests-text {
  font-size: 16px;
  color: #666;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .friend-requests-page {
    padding: 10px;
  }

  .friend-request-stats {
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
    padding: 15px;
  }

  .friend-request-item {
    padding: 16px;
    margin-bottom: 12px;
  }

  .friend-request-header {
    margin-bottom: 12px;
  }

  .friend-request-avatar {
    width: 50px;
    height: 50px;
    font-size: 22px;
    margin-right: 12px;
  }

  .friend-request-name {
    font-size: 18px;
    margin-bottom: 2px;
  }

  .friend-request-id {
    font-size: 12px;
  }

  .friend-request-content {
    margin-bottom: 12px;
  }

  .friend-request-message {
    font-size: 14px;
    margin-bottom: 10px;
    padding: 10px 14px;
  }

  .friend-request-verification {
    font-size: 13px;
    padding: 12px 16px;
  }

  .friend-request-footer {
    flex-direction: column;
    gap: 12px;
    padding-top: 12px;
  }

  .friend-request-meta {
    gap: 12px;
    justify-content: space-between;
    width: 100%;
  }

  .friend-request-time {
    font-size: 12px;
  }

  .friend-request-status {
    font-size: 11px;
    padding: 6px 12px;
  }

  .friend-request-actions {
    width: 100%;
    justify-content: center;
  }

  .friend-request-btn {
    padding: 10px 20px;
    font-size: 13px;
    min-width: 90px;
    flex: 1;
    max-width: 140px;
    margin: 0 6px;
  }
}

/* 标签详情页面样式 */
.tag-contacts-list {
  max-height: 500px;
  overflow-y: auto;
}

.contact-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  margin-bottom: 12px;
  background: white;
  transition: all 0.2s ease;
}

.contact-item:hover {
  background: #f8f9fa;
  border-color: #3b82f6;
}

.contact-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  margin-right: 16px;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.contact-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.contact-avatar .avatar-placeholder {
  color: white;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.contact-info {
  flex: 1;
  min-width: 0;
}

.contact-name {
  font-weight: 500;
  color: #495057;
  margin-bottom: 4px;
  font-size: 14px;
}

.contact-status {
  color: #6c757d;
  font-size: 12px;
  word-break: break-word;
}

.contact-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

/* 确认内容样式 */
.confirm-content {
  text-align: center;
  padding: 20px;
}

.confirm-content .confirm-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.confirm-content p {
  margin-bottom: 12px;
  font-size: 16px;
  color: #495057;
}

.warning-text {
  color: #dc3545;
  font-size: 14px;
  font-weight: 500;
}

/* 大尺寸模态框 */
.modal-container.large {
  max-width: 800px;
  width: 90%;
}

/* 用户资料模态框样式 */
.user-profile-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  backdrop-filter: blur(5px);
}

.user-profile-modal-content {
  background: white;
  border-radius: 16px;
  max-width: 500px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: profileModalSlideIn 0.3s ease-out;
}

@keyframes profileModalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-30px) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.user-profile-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.user-profile-modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.user-profile-close {
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  transition: all 0.2s ease;
  line-height: 1;
}

.user-profile-close:hover {
  background: #f5f5f5;
  color: #666;
}

.user-profile-modal-body {
  padding: 24px;
}

.profile-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e9ecef;
}

.profile-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  margin-right: 15px;
  flex-shrink: 0;
}

.profile-basic-info h3 {
  margin: 0 0 5px 0;
  font-size: 18px;
  color: #333;
}

.profile-basic-info p {
  margin: 2px 0;
  color: #666;
  font-size: 14px;
}

.profile-details {
  display: grid;
  gap: 12px;
}

.profile-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}

.profile-item:last-child {
  border-bottom: none;
}

.profile-item label {
  font-weight: 500;
  color: #333;
  min-width: 60px;
}

.profile-item span {
  color: #666;
  text-align: right;
  flex: 1;
}

/* 联系人搜索结果样式 - 匹配 index.html */
.search-results {
  background: #fff;
  margin: 12px 0 16px 0;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 1px solid #f0f0f0;
}

.search-results-list {
  max-height: 300px;
  overflow-y: auto;
}

.search-result-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s ease;
}

.search-result-item:hover {
  background: #f8f8f8;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-avatar {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 16px;
  margin-right: 12px;
  overflow: hidden;
}

.search-result-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.search-result-info {
  flex: 1;
}

.search-result-name {
  font-size: 16px;
  font-weight: 500;
  color: #000;
  margin-bottom: 2px;
}

.search-result-id {
  font-size: 12px;
  color: #999;
}

.search-result-status {
  font-size: 12px;
  color: #666;
  margin-top: 2px;
}

.view-profile-btn {
  background: #576b95;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.3s ease;
  margin-right: 8px;
}

.view-profile-btn:hover {
  background: #4a5a87;
}

.add-friend-btn {
  background: #07c160;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.3s ease;
}

.add-friend-btn:hover {
  background: #06a552;
}

.add-friend-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.sent-request-btn {
  background: #6c757d;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: not-allowed;
  font-size: 12px;
  opacity: 0.7;
}

.search-result-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.relationship-status {
  font-size: 12px;
  color: #999;
  padding: 6px 12px;
}

.search-empty {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}

.search-loading {
  text-align: center;
  padding: 20px;
  color: #666;
}

/* 功能入口样式 */
.contact-functions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.function-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 12px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #f0f0f0;
}

.function-item:hover {
  background: #f8f9fa;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.function-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.function-text {
  font-size: 12px;
  color: #495057;
  font-weight: 500;
}

.function-badge {
  background: #dc3545;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  margin-left: 4px;
  min-width: 16px;
  text-align: center;
}

/* 个人页面功能项中的徽章 */
.profile-functions .function-item .function-badge {
  position: absolute;
  right: 40px;
  top: 50%;
  transform: translateY(-50%);
}

/* 响应式设计 - 联系人功能 */
@media (max-width: 768px) {
  .contact-functions {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    padding: 12px;
  }
  
  .function-item {
    padding: 12px 8px;
  }
  
  .function-icon {
    font-size: 20px;
    margin-bottom: 6px;
  }
  
  .function-text {
    font-size: 11px;
  }
  
  .modal-container.large {
    width: 95%;
    margin: 10px;
  }
  
  .contact-menu {
    min-width: 140px;
  }
  
  .tag-actions {
    flex-direction: column;
    gap: 4px;
  }
  
  .request-actions {
    flex-direction: column;
    gap: 4px;
  }
  
  .contact-actions {
    flex-direction: column;
    gap: 4px;
  }
}

/* 好友请求项样式 - 完全匹配index.html */
.friend-request-item {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.friend-request-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.friend-request-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border-color: #cbd5e0;
}

.friend-request-item:hover::before {
  opacity: 1;
}

.friend-request-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.friend-request-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 18px;
  margin-right: 16px;
  border: 3px solid #ffffff;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  position: relative;
  overflow: hidden;
}

.friend-request-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.friend-request-user-info {
  flex: 1;
}

.friend-request-name {
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 4px;
}

.friend-request-id {
  font-size: 13px;
  color: #718096;
  font-family: 'Courier New', monospace;
}

.friend-request-content {
  margin-bottom: 16px;
}

.friend-request-message {
  color: #4a5568;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 8px;
}

.friend-request-verification {
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  font-size: 13px;
  color: #4a5568;
  font-style: italic;
}

.friend-request-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.friend-request-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.friend-request-time {
  font-size: 12px;
  color: #a0aec0;
}

.friend-request-status {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.friend-request-status.pending {
  background: linear-gradient(135deg, #fed7d7 0%, #feb2b2 100%);
  color: #c53030;
  border: 1px solid #fc8181;
}

.friend-request-status.accepted {
  background: linear-gradient(135deg, #c6f6d5 0%, #9ae6b4 100%);
  color: #2f855a;
  border: 1px solid #68d391;
}

.friend-request-status.rejected {
  background: linear-gradient(135deg, #fed7d7 0%, #feb2b2 100%);
  color: #c53030;
  border: 1px solid #fc8181;
}

.friend-request-header-actions {
  margin-left: auto;
}

.friend-request-actions {
  display: flex;
  gap: 8px;
}

.friend-request-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 6px;
}

.friend-request-btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  transition: all 0.3s ease;
  transform: translate(-50%, -50%);
}

.friend-request-btn:hover::before {
  width: 100px;
  height: 100px;
}

.friend-request-btn.accept {
  background: linear-gradient(135deg, #48bb78 0%, #38a169 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(72, 187, 120, 0.3);
}

.friend-request-btn.accept:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(72, 187, 120, 0.4);
}

.friend-request-btn.reject {
  background: linear-gradient(135deg, #f56565 0%, #e53e3e 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(245, 101, 101, 0.3);
}

.friend-request-btn.cancel:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(160, 174, 192, 0.4);
}

.friend-request-btn.view-profile {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.friend-request-btn.view-profile:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.friend-request-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
}

.friend-request-btn:active {
  transform: translateY(0) scale(0.98);
}

.friend-request-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #a0aec0;
  font-size: 14px;
}

/* 分配标签模态框样式 */
#assignTagModal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  backdrop-filter: blur(8px);
  animation: fadeIn 0.3s ease-out;
  padding: 20px;
  box-sizing: border-box;
}

#assignTagModal .modal-content {
  width: 200px;
  max-width: calc(100vw - 40px);
  max-height: calc(100vh - 40px);
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: auto;
  transform: scale(1);
  animation: modalSlideIn 0.3s ease-out forwards;
  display: flex;
  flex-direction: column;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes modalSlideIn {
  from {
    transform: scale(0.9) translateY(-20px);
    opacity: 0;
  }
  to {
    transform: scale(1) translateY(0);
    opacity: 1;
  }
}

#assignTagModal .modal-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px 24px;
  border-bottom: none;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top-left-radius: 16px;
  border-top-right-radius: 16px;
}

#assignTagModal .modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

#assignTagModal .modal-header .close {
  color: white;
  opacity: 0.8;
  font-size: 24px;
  transition: opacity 0.2s ease;
  cursor: pointer;
  background: none;
  border: none;
  padding: 0;
  line-height: 1;
}

#assignTagModal .modal-header .close:hover {
  opacity: 1;
}

#assignTagModal .modal-body {
  padding: 24px;
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.contact-info-section {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 12px;
  border: 1px solid #e9ecef;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.contact-avatar-small {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  margin-right: 16px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  background-size: cover;
  background-position: center;
}

.contact-details-small {
  flex: 1;
}

.contact-name-small {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 2px;
}

.contact-alias-small {
  font-size: 14px;
  color: #6c757d;
  font-style: italic;
}

.tags-selection {
  margin-top: 0;
}

.tags-selection-header {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}

.tags-selection-header::before {
  content: '🏷️';
  margin-right: 8px;
  font-size: 18px;
}

.available-tags {
  max-height: 320px;
  overflow-y: auto;
  padding-right: 4px;
}

.available-tags::-webkit-scrollbar {
  width: 6px;
}

.available-tags::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.available-tags::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.available-tags::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.tag-option {
  display: flex;
  align-items: center;
  padding: 14px 18px;
  margin: 6px 0;
  border: 2px solid #e9ecef;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
  position: relative;
  overflow: hidden;
}

.tag-option::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.1), transparent);
  transition: left 0.5s ease;
}

.tag-option:hover::before {
  left: 100%;
}

.tag-option:hover {
  border-color: #667eea;
  background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.tag-option.selected {
  border-color: #667eea;
  background: linear-gradient(135deg, #e8f0fe 0%, #dbeafe 100%);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.2);
  transform: translateY(-1px);
}

.tag-option-color {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  margin-right: 16px;
  flex-shrink: 0;
  border: 2px solid white;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  position: relative;
}

.tag-option-color::after {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

.tag-option-name {
  flex: 1;
  font-size: 15px;
  color: #2c3e50;
  font-weight: 500;
  letter-spacing: 0.3px;
}

.tag-checkbox {
  width: 22px;
  height: 22px;
  border: 2px solid #cbd5e0;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #667eea;
  font-weight: bold;
  transition: all 0.3s ease;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.tag-checkbox:hover {
  border-color: #667eea;
  transform: scale(1.05);
}

.tag-option.selected .tag-checkbox {
  border-color: #667eea;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  transform: scale(1.1);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.3);
}

.create-tag-option {
  display: flex;
  align-items: center;
  padding: 16px 18px;
  margin: 8px 0;
  border: 2px dashed #cbd5e0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: linear-gradient(135deg, #fafbfc 0%, #f8f9fa 100%);
  position: relative;
  overflow: hidden;
}

.create-tag-option::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.05), transparent);
  transition: left 0.5s ease;
}

.create-tag-option:hover::before {
  left: 100%;
}

.create-tag-option:hover {
  border-color: #667eea;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8f0fe 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.create-tag-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
  margin-right: 16px;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.3);
  transition: transform 0.3s ease;
}

.create-tag-option:hover .create-tag-icon {
  transform: rotate(90deg) scale(1.1);
}

.create-tag-text {
  color: #667eea;
  font-weight: 600;
  font-size: 15px;
  letter-spacing: 0.3px;
}

.no-tags {
  text-align: center;
  color: #6c757d;
  padding: 32px 20px;
  font-size: 15px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 12px;
  border: 2px dashed #dee2e6;
}

.no-tags::before {
  content: '📝';
  display: block;
  font-size: 32px;
  margin-bottom: 12px;
}

#assignTagModal .modal-footer {
  padding: 20px 24px;
  background: #f8f9fa;
  border-top: 1px solid #e9ecef;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  flex-shrink: 0;
  border-bottom-left-radius: 16px;
  border-bottom-right-radius: 16px;
}

#assignTagModal .btn {
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

#assignTagModal .btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

#assignTagModal .btn:hover::before {
  left: 100%;
}

#assignTagModal .btn-secondary {
  background: #6c757d;
  color: white;
  box-shadow: 0 2px 4px rgba(108, 117, 125, 0.3);
}

#assignTagModal .btn-secondary:hover {
  background: #5a6268;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(108, 117, 125, 0.4);
}

#assignTagModal .btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 4px rgba(102, 126, 234, 0.3);
}

#assignTagModal .btn-primary:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

#assignTagModal .btn:active {
  transform: translateY(0);
}

/* 联系人标签样式 */
.contact-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
  align-items: center;
}

.contact-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
  color: white;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.25);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  letter-spacing: 0.3px;
  line-height: 1.2;
  max-width: 80px;
  white-space: nowrap;
  text-overflow: ellipsis;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.contact-tag::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.contact-tag:hover {
  transform: translateY(-1px) scale(1.05);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  filter: brightness(1.1);
}

.contact-tag:hover::before {
  left: 100%;
}

/* 不同颜色的标签样式 */
.contact-tag[style*="#667eea"] {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.25);
}

.contact-tag[style*="#4facfe"] {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  box-shadow: 0 2px 6px rgba(79, 172, 254, 0.25);
}

.contact-tag[style*="#43e97b"] {
  background: linear-gradient(135deg, #43e97b 0%, #38ef7d 100%);
  box-shadow: 0 2px 6px rgba(67, 233, 123, 0.25);
}

.contact-tag[style*="#ffecd2"] {
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
  box-shadow: 0 2px 6px rgba(255, 236, 210, 0.25);
  color: #8b4513;
}

.contact-tag[style*="#a8edea"] {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  box-shadow: 0 2px 6px rgba(168, 237, 234, 0.25);
  color: #2c3e50;
}

.contact-tag[style*="#ff9a9e"] {
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
  box-shadow: 0 2px 6px rgba(255, 154, 158, 0.25);
}

.contact-tag[style*="#ffeaa7"] {
  background: linear-gradient(135deg, #ffeaa7 0%, #fab1a0 100%);
  box-shadow: 0 2px 6px rgba(255, 234, 167, 0.25);
  color: #8b4513;
}

.contact-tag[style*="#fd79a8"] {
  background: linear-gradient(135deg, #fd79a8 0%, #fdcbf1 100%);
  box-shadow: 0 2px 6px rgba(253, 121, 168, 0.25);
}

/* 响应式设计 */
@media (max-width: 768px) {
  #assignTagModal {
    padding: 10px;
  }
  
  #assignTagModal .modal-content {
    width: 100%;
    max-width: calc(100vw - 20px);
    max-height: calc(100vh - 20px);
  }
  
  #assignTagModal .modal-header {
    padding: 16px 20px;
  }
  
  #assignTagModal .modal-body {
    padding: 20px;
  }
  
  #assignTagModal .modal-footer {
    padding: 16px 20px;
    flex-direction: column;
    gap: 8px;
  }
  
  #assignTagModal .btn {
    width: 100%;
    padding: 14px 20px;
  }
  
  .contact-info-section {
    padding: 16px;
  }
  
  .contact-avatar-small {
    width: 40px;
    height: 40px;
    font-size: 16px;
  }
  
  .contact-name-small {
    font-size: 16px;
  }
  
  .contact-tags {
    gap: 4px;
    margin-top: 4px;
  }
  
  .contact-tag {
    padding: 3px 8px;
    font-size: 10px;
    max-width: 60px;
  }
}

/* 创建标签模态框样式 */
.modal-overlay:has(.create-tag-modal-content) {
  z-index: 10001;
}

.modal-overlay .create-tag-modal-content {
  z-index: 10002;
}

.create-tag-modal-content {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 480px;
  max-height: 90vh;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  animation: createTagModalSlideIn 0.3s ease-out;
  position: relative;
}

@keyframes createTagModalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.create-tag-modal-header {
  padding: 24px 24px 16px;
  border-bottom: 1px solid #e9ecef;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
}

.create-tag-modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
  letter-spacing: 0.5px;
}

.create-tag-close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #6c757d;
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.create-tag-close-btn:hover {
  background: #f8f9fa;
  color: #495057;
  transform: scale(1.1);
}

.create-tag-modal-body {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.create-tag-form-group {
  margin-bottom: 24px;
}

.create-tag-form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
  letter-spacing: 0.3px;
}

.create-tag-input-container {
  position: relative;
}

.create-tag-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  font-size: 16px;
  transition: all 0.3s ease;
  background: white;
  box-sizing: border-box;
}

.create-tag-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.char-count {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  color: #6c757d;
  background: white;
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid #e9ecef;
}

.create-tag-color-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.color-preview {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.create-tag-color-input {
  width: 60px;
  height: 40px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  background: none;
}

.create-tag-color-input::-webkit-color-swatch-wrapper {
  padding: 0;
  border: none;
  border-radius: 8px;
}

.create-tag-color-input::-webkit-color-swatch {
  border: 2px solid #e9ecef;
  border-radius: 8px;
}

.color-options {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(32px, 1fr));
  gap: 8px;
  margin-top: 8px;
}

.color-option {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.3s ease;
  position: relative;
}

.color-option:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.color-option.selected {
  border-color: #2c3e50;
  transform: scale(1.15);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.color-option.selected::after {
  content: '✓';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 14px;
  font-weight: bold;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.create-tag-modal-footer {
  padding: 20px 24px;
  background: #f8f9fa;
  border-top: 1px solid #e9ecef;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  border-bottom-left-radius: 16px;
  border-bottom-right-radius: 16px;
}

.create-tag-btn-secondary,
.create-tag-btn-primary {
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.create-tag-btn-secondary {
  background: #6c757d;
  color: white;
  box-shadow: 0 2px 4px rgba(108, 117, 125, 0.3);
}

.create-tag-btn-secondary:hover {
  background: #5a6268;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(108, 117, 125, 0.4);
}

.create-tag-btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 4px rgba(102, 126, 234, 0.3);
}

.create-tag-btn-primary:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

/* 创建标签模态框响应式设计 */
@media (max-width: 768px) {
  .create-tag-modal-content {
    width: 95%;
    margin: 20px auto;
  }
  
  .create-tag-modal-header,
  .create-tag-modal-body,
  .create-tag-modal-footer {
    padding: 16px;
  }
  
  .create-tag-modal-footer {
    flex-direction: column;
  }
  
  .create-tag-btn-secondary,
  .create-tag-btn-primary {
    width: 100%;
    margin: 4px 0;
  }
}
</style>