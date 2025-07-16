    <script>
        // 全局变量
        let currentTab = 'chats';
        let userInfo = null;
    
        // 获取用户信息
        function getUserInfo() {
            let userInfoStr = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo');
            let accessToken = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
            
            if (userInfoStr && accessToken) {
                try {
                    const userData = JSON.parse(userInfoStr);
                    
                    // 解析personalizedStatus JSON字符串为status对象
                    if (userData.personalizedStatus && typeof userData.personalizedStatus === 'string') {
                        try {
                            userData.status = JSON.parse(userData.personalizedStatus);
                        } catch (e) {
                            console.warn('解析本地存储的用户状态失败:', e);
                            userData.status = null;
                        }
                    } else if (!userData.personalizedStatus) {
                        userData.status = null;
                    }
                    
                    return userData;
                } catch (e) {
                    console.error('解析用户信息失败:', e);
                    return null;
                }
            }
            return null;
        }
    
        // 验证token有效性
        async function validateToken() {
            const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
            if (!token) {
                return false;
            }
    
            try {
                const response = await fetch('/api/auth/me', {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + token
                    }
                });
                
                // 处理401未授权错误
                if (response.status === 401) {
                    console.warn('Token已失效，清除登录信息并跳转到登录页');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 1000);
                    return false;
                }
                
                return response.ok;
            } catch (error) {
                console.error('Token验证失败:', error);
                return false;
            }
        }
    
        // 清除登录信息
        function clearLoginInfo() {
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('userInfo');
            sessionStorage.removeItem('accessToken');
            sessionStorage.removeItem('refreshToken');
            sessionStorage.removeItem('userInfo');
        }
    
        // 切换标签页
        function switchTab(tabName) {
            // 更新标签栏状态
            document.querySelectorAll('.tab-item').forEach(item => {
                item.classList.remove('active');
            });
            
            const tabElement = document.querySelector(`[data-tab="${tabName}"]`);
            if (tabElement) {
                tabElement.classList.add('active');
            }
    
            // 更新内容区域
            document.querySelectorAll('.tab-content').forEach(content => {
                content.classList.remove('active');
            });
            
            const tabContent = document.getElementById(`${tabName}Tab`);
            if (tabContent) {
                tabContent.classList.add('active');
            }
    
            // 更新顶部标题和右侧按钮
            const titles = {
                'chats': '会话',
                'contacts': '联系人', 
                'moments': '动态',
                'me': '我'
            };
            
            const rightButtons = {
                'chats': '➕',
                'contacts': '➕',
                'moments': '📷',
                'me': '⚙️'
            };
    
            const pageTitle = document.getElementById('pageTitle');
            const rightBtn = document.getElementById('rightBtn');
            
            if (pageTitle) {
                pageTitle.textContent = titles[tabName];
            }
            if (rightBtn) {
                rightBtn.textContent = rightButtons[tabName];
            }
            
            currentTab = tabName;
            
            // 根据切换的标签页重新加载相应数据
            switch(tabName) {
                case 'contacts':
                    // 重新加载联系人相关数据
                    initContactsList();
                    if (typeof loadFriendRequests === 'function') {
                        loadFriendRequests();
                    }
                    if (typeof loadTags === 'function') {
                        loadTags();
                    }
                    break;
                case 'chats':
                    // 重新加载会话列表
                    initChatList();
                    break;
                case 'moments':
                    // 重新加载动态列表
                    initMomentsList();
                    break;
            }
        }
    
        // 处理右侧按钮点击
        function handleRightButton() {
            switch(currentTab) {
                case 'chats':
                    showChatOptions();
                    break;
                case 'contacts':
                    showContactOptions();
                    break;
                case 'moments':
                    publishMoment();
                    break;
                case 'me':
                    openSettings();
                    break;
            }
        }
    
        // 显示聊天选项
        function showChatOptions() {
            alert('发起群聊、添加好友、扫一扫等功能正在开发中...');
        }
    
        // 显示联系人选项
        function showContactOptions() {
            // 创建选项菜单
            const options = [
                { text: '刷新联系人列表', action: 'refresh' },
                { text: '添加朋友', action: 'add' },
                { text: '标签管理', action: 'tags' }
            ];
            
            // 创建菜单HTML
            const menuHtml = options.map(option => 
                `<div class="options-menu-item" onclick="handleContactOption('${option.action}')">${option.text}</div>`
            ).join('');
            
            // 显示选项菜单
            showOptionsMenu(menuHtml);
        }
        
        // 处理联系人选项
        async function handleContactOption(action) {
            hideOptionsMenu();
            
            switch(action) {
                case 'refresh':
                    showMessage('正在刷新联系人列表...', 'info');
                    try {
                        await initContactsList();
                        showMessage('联系人列表已刷新', 'success');
                    } catch (error) {
                        console.error('刷新联系人列表失败:', error);
                        showMessage('刷新失败，请重试', 'error');
                    }
                    break;
                case 'add':
                    alert('添加朋友功能正在开发中...');
                    break;
                case 'tags':
                    showTagsPage();
                    break;
            }
        }
        
        // 显示选项菜单
        function showOptionsMenu(menuHtml) {
            // 移除已存在的菜单
            const existingMenu = document.querySelector('.options-menu');
            if (existingMenu) {
                existingMenu.remove();
            }
            
            // 创建遮罩层
            const overlay = document.createElement('div');
            overlay.className = 'options-menu-overlay';
            overlay.onclick = hideOptionsMenu;
            
            // 创建新菜单
            const menu = document.createElement('div');
            menu.className = 'options-menu';
            menu.innerHTML = menuHtml;
            
            // 添加到页面
            document.body.appendChild(overlay);
            document.body.appendChild(menu);
            
            // 触发显示动画
            setTimeout(() => {
                menu.classList.add('show');
            }, 10);
        }
        
        // 隐藏选项菜单
        function hideOptionsMenu() {
            const menu = document.querySelector('.options-menu');
            const overlay = document.querySelector('.options-menu-overlay');
            
            if (menu) {
                menu.classList.remove('show');
                setTimeout(() => {
                    menu.remove();
                }, 200); // 等待动画完成
            }
            
            if (overlay) {
                overlay.remove();
            }
        }
    
        // 初始化会话列表
        async function initChatList() {
            const chatList = document.getElementById('chatList');
            const token = getAuthToken();
            const userInfo = getUserInfo();
            
            if (!chatList) {
                console.warn('chatList元素不存在，可能当前不在会话页面');
                return;
            }
            
            if (!token || !userInfo || !userInfo.id) {
                chatList.innerHTML = '<div class="no-chats">请先登录</div>';
                return;
            }
            
            try {
                // 显示加载状态
                chatList.innerHTML = '<div class="chats-loading">加载会话中...</div>';
                
                const response = await fetch(`/api/conversations?userId=${userInfo.id}&page=0&size=20`, {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json',
                        'X-User-Id': userInfo.id.toString()
                    }
                });
                
                if (response.status === 401) {
                    clearLoginInfo();
                    window.location.href = '/login.html';
                    return;
                }
                
                if (!response.ok) {
                    throw new Error('获取会话列表失败');
                }
                
                const data = await response.json();
                console.log('从后端接收到的会话数据:', data);
                
                if (data.success && data.data && data.data.content && data.data.content.length > 0) {
                    const conversationResponse = data.data.content[0]; // 获取ConversationResponse
                    if (conversationResponse.conversations && conversationResponse.conversations.length > 0) {
                        displayChatList(conversationResponse.conversations);
                    } else {
                        chatList.innerHTML = '<div class="no-chats">暂无会话</div>';
                    }
                } else {
                    chatList.innerHTML = '<div class="no-chats">暂无会话</div>';
                }
            } catch (error) {
                console.error('获取会话列表失败:', error);
                chatList.innerHTML = '<div class="chats-error">加载失败，请重试</div>';
            }
        }
        
        // 显示会话列表
        function displayChatList(conversations) {
            const chatList = document.getElementById('chatList');
            
            if (!chatList) {
                console.warn('chatList元素不存在，无法显示会话列表');
                return;
            }
            
            if (!conversations || conversations.length === 0) {
                chatList.innerHTML = '<div class="no-chats">暂无会话</div>';
                return;
            }
            
            chatList.innerHTML = conversations.map(conversation => {
                // 获取会话显示名称
                const displayName = getConversationDisplayName(conversation);
                
                // 获取会话头像
                const avatarContent = getConversationAvatar(conversation);
                
                // 格式化时间
                const timeStr = formatConversationTime(conversation.lastActiveTime);
                
                // 获取最后一条消息内容
                const lastMessageContent = getLastMessageContent(conversation.lastMessage);
                
                // 未读消息数量
                const unreadCount = conversation.unreadCount || 0;
                
                // 是否免打扰
                const isMuted = conversation.isDnd || false;
                
                return `<div class="chat-item ${conversation.isPinned ? 'pinned' : ''}" onclick="openChat(${conversation.id})"><div class="chat-avatar">${avatarContent}</div><div class="chat-info"><div class="chat-header"><div class="chat-name">${escapeHtml(displayName)}</div><div class="chat-time">${timeStr}</div></div><div class="chat-preview">${isMuted ? '<span class="mute-icon">🔕</span>' : ''}${lastMessageContent}${unreadCount > 0 ? `<div class="chat-badge">${unreadCount}</div>` : ''}</div></div></div>`;
            }).join('');
        }
        
        // 获取会话显示名称
        function getConversationDisplayName(conversation) {
            const userInfo = getUserInfo();
            
            if (conversation.type === 'PRIVATE') {
                // 私聊：查找对方参与者信息
                const otherParticipant = conversation.participants?.find(p => p.userId !== userInfo.id);
                if (otherParticipant) {
                    // 优先使用备注名（alias），然后是用户昵称，最后是邮箱
                    if (otherParticipant.alias) {
                        return otherParticipant.alias;
                    }
                    if (otherParticipant.user && otherParticipant.user.nickname) {
                        return otherParticipant.user.nickname;
                    }
                    if (otherParticipant.user && otherParticipant.user.email) {
                        return otherParticipant.user.email;
                    }
                    // 如果都没有，使用用户ID
                    return `用户${otherParticipant.userId}`;
                }
                return '私聊';
            } else {
                // 群聊：使用会话名称
                return conversation.name || '群聊';
            }
        }
        
        // 获取会话头像
        function getConversationAvatar(conversation) {
            const userInfo = getUserInfo();
            
            if (conversation.type === 'PRIVATE') {
                // 私聊：使用对方用户的头像
                const otherParticipant = conversation.participants?.find(p => p.userId !== userInfo.id);
                if (otherParticipant && otherParticipant.user && otherParticipant.user.avatarUrl) {
                    return `<img src="${otherParticipant.user.avatarUrl}" alt="头像" style="width: 100%; height: 100%; border-radius: inherit; object-fit: cover;">`;
                }
                // 如果没有头像，使用对方用户名的首字母
                const displayName = getConversationDisplayName(conversation);
                const avatarText = displayName.charAt(0).toUpperCase();
                return avatarText;
            } else {
                // 群聊：使用会话头像
                if (conversation.avatarUrl) {
                    return `<img src="${conversation.avatarUrl}" alt="头像" style="width: 100%; height: 100%; border-radius: inherit; object-fit: cover;">`;
                }
                // 如果没有头像，使用群聊名称的首字母
                const displayName = conversation.name || '群聊';
                const avatarText = displayName.charAt(0).toUpperCase();
                return avatarText;
            }
        }
        
        // 格式化会话时间
        function formatConversationTime(timeStr) {
            if (!timeStr) return '';
            
            const time = new Date(timeStr);
            const now = new Date();
            const diffMs = now - time;
            const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
            
            if (diffDays === 0) {
                // 今天，显示时间
                return time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
            } else if (diffDays === 1) {
                return '昨天';
            } else if (diffDays < 7) {
                return `${diffDays}天前`;
            } else {
                return time.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' });
            }
        }
        
        // 获取最后一条消息内容
        function getLastMessageContent(lastMessage) {
            if (!lastMessage) {
                return '暂无消息';
            }
            
            // 根据消息类型显示不同内容
            switch (lastMessage.messageType) {
                case 'TEXT':
                    return escapeHtml(lastMessage.content || '');
                case 'IMAGE':
                    return '[图片]';
                case 'FILE':
                    return '[文件]';
                case 'AUDIO':
                    return '[语音]';
                case 'VIDEO':
                    return '[视频]';
                default:
                    return '[消息]';
            }
        }
    
        // 初始化联系人列表
        async function initContactsList() {
            const contactsList = document.getElementById('contactsList');
            const token = getAuthToken();
            const userInfo = getUserInfo();
            
            if (!token || !userInfo || !userInfo.id) {
                contactsList.innerHTML = '<div class="no-contacts">请先登录</div>';
                return;
            }
            
            try {
                // 显示加载状态
                contactsList.innerHTML = '<div class="contacts-loading">加载联系人中...</div>';
                
                const response = await fetch(`/api/contacts?userId=${userInfo.id}&includeBlocked=false`, {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    }
                });
                
                if (response.status === 401) {
                    clearLoginInfo();
                    window.location.href = '/login.html';
                    return;
                }
                
                if (!response.ok) {
                    throw new Error('获取联系人列表失败');
                }
                
                const data = await response.json();
                console.log('从后端接收到的原始数据:', data);
                console.log('data.data:', data.data);
                if (data.data && data.data.length > 0) {
                    data.data.forEach((contact, index) => {
                        console.log(`联系人 ${index}:`, {
                            friendId: contact.friendId,
                            friendIdType: typeof contact.friendId,
                            nickname: contact.nickname,
                            alias: contact.alias
                        });
                    });
                }
                if (data.success && data.data) {
                    displayContactsList(data.data);
                } else {
                    contactsList.innerHTML = '<div class="no-contacts">暂无联系人</div>';
                }
            } catch (error) {
                console.error('获取联系人列表失败:', error);
                contactsList.innerHTML = '<div class="contacts-error">加载失败，请重试</div>';
            }
        }
        
        // 显示联系人列表
        function displayContactsList(contacts) {
            const contactsList = document.getElementById('contactsList');
            
            console.log('displayContactsList 被调用，联系人数据:', contacts);
            
            if (!contacts || contacts.length === 0) {
                contactsList.innerHTML = '<div class="no-contacts">暂无联系人</div>';
                return;
            }
            
            contactsList.innerHTML = contacts.map(contact => {
                console.log('渲染联系人:', {
                    friendId: contact.friendId,
                    friendIdType: typeof contact.friendId,
                    nickname: contact.nickname,
                    alias: contact.alias
                });
                const avatarText = (contact.nickname || contact.alias || 'U').charAt(0).toUpperCase();
                const displayName = contact.alias || contact.nickname || '未知用户';
                
                // 处理头像显示
                let avatarContent;
                if (contact.avatarUrl) {
                    avatarContent = `<img src="${contact.avatarUrl}" alt="头像" style="width: 100%; height: 100%; border-radius: inherit; object-fit: cover;">`;
                } else {
                    avatarContent = avatarText;
                }
                
                // 检查 friendId 是否有效
                if (!contact.friendId) {
                    console.error('联系人 friendId 为空:', contact);
                    return ''; // 跳过无效的联系人
                }
                
                // 获取联系人标签（如果有的话）
                const tagsHtml = contact.tags && contact.tags.length > 0 ? 
                    `<div class="contact-tags">
                        ${contact.tags.map(tag => 
                            `<span class="contact-tag" style="background: ${tag.color || '#667eea'}">${escapeHtml(tag.name)}</span>`
                        ).join('')}
                    </div>` : '';
                
                return `
                    <div class="contact-item">
                        <div class="contact-avatar" onclick="openContact(${contact.friendId})">${avatarContent}</div>
                        <div class="contact-info" onclick="openContact(${contact.friendId})">
                            <div class="contact-name">${escapeHtml(displayName)}</div>
                            ${contact.alias && contact.nickname && contact.alias !== contact.nickname ? 
                                `<div class="contact-nickname">${escapeHtml(contact.nickname)}</div>` : ''}
                            ${tagsHtml}
                        </div>
                        <div class="contact-actions">
                            <button class="contact-action-btn" onclick="showContactMenu(${contact.friendId}, '${escapeHtml(displayName)}')" title="更多操作">⋯</button>
                        </div>
                    </div>
                `;
            }).join('');
        }
    
        // 初始化动态列表
        function initMomentsList() {
            const momentsList = document.getElementById('momentsList');
            const mockMoments = [
                {
                    id: 1,
                    user: '张三',
                    avatar: '张',
                    content: '今天天气真不错，出来走走心情都变好了 ☀️',
                    time: '2小时前',
                    images: ['📷', '📷', '📷'],
                    likes: 5,
                    comments: 2
                },
                {
                    id: 2,
                    user: '李四',
                    avatar: '李',
                    content: '刚完成了一个重要项目，感谢团队的努力！🎉',
                    time: '5小时前',
                    images: [],
                    likes: 12,
                    comments: 8
                }
            ];
    
            momentsList.innerHTML = mockMoments.map(moment => `
                <div class="moment-item">
                    <div class="moment-header">
                        <div class="moment-avatar">${moment.avatar}</div>
                        <div class="moment-user">
                            <div class="moment-name">${moment.user}</div>
                            <div class="moment-time">${moment.time}</div>
                        </div>
                    </div>
                    <div class="moment-content">${moment.content}</div>
                    ${moment.images.length > 0 ? `
                        <div class="moment-images">
                            ${moment.images.map(img => `<div class="moment-image">${img}</div>`).join('')}
                        </div>
                    ` : ''}
                    <div class="moment-actions">
                        <div class="moment-likes">
                            <button class="like-btn" onclick="likeMoment(${moment.id})">❤️</button>
                            ${moment.likes} 赞
                        </div>
                        <button class="comment-btn" onclick="commentMoment(${moment.id})">💬</button>
                    </div>
                </div>
            `).join('');
        }
    
        // 显示用户信息
        function displayUserInfo(user) {
            const avatarText = (user.nickname || user.email).charAt(0).toUpperCase();
            
            // 更新动态页面用户信息
            const momentsAvatar = document.getElementById('momentsAvatar');
            const momentsName = document.getElementById('momentsName');
            
            if (momentsAvatar) {
                if (user.avatarUrl) {
                    // 如果有头像URL，创建img元素
                    momentsAvatar.innerHTML = `<img src="${user.avatarUrl}" alt="头像" style="width: 100%; height: 100%; border-radius: inherit; object-fit: cover;">`;
                } else {
                    // 否则显示文字头像
                    momentsAvatar.textContent = avatarText;
                }
            }
            
            if (momentsName) momentsName.textContent = user.nickname || user.email;
            
            // 更新我的页面用户信息
            const meAvatar = document.getElementById('meAvatar');
            const meName = document.getElementById('meName');
            const meId = document.getElementById('meId');
            
            if (meAvatar) {
                if (user.avatarUrl) {
                    // 如果有头像URL，创建img元素
                    meAvatar.innerHTML = `<img src="${user.avatarUrl}" alt="头像" style="width: 100%; height: 100%; border-radius: inherit; object-fit: cover;">`;
                } else {
                    // 否则显示文字头像
                    meAvatar.textContent = avatarText;
                }
            }
            
            if (meName) meName.textContent = user.nickname || user.email;
            if (meId) {
                if (user.userIdString) {
                    meId.textContent = `个人ID：${user.userIdString}`;
                } else {
                    meId.textContent = '个人ID：未设置';
                }
            }
            
            // 更新个性状态显示（如果存在）
            const statusDisplay = document.getElementById('currentStatusDisplay');
            const statusEmoji = document.getElementById('currentStatusEmoji');
            const statusText = document.getElementById('currentStatusText');
            const meStatus = document.getElementById('meStatus');
            
            console.log('displayUserInfo - 用户状态:', user.status);
            
            if (user.status && user.status.text) {
                // 如果有状态文本，显示状态（emoji可选）
                const emoji = user.status.emoji || '😊';
                const text = user.status.text;
                
                if (statusEmoji) statusEmoji.textContent = emoji;
                if (statusText) statusText.textContent = text;
                if (statusDisplay) statusDisplay.style.display = 'flex';
                
                // 更新"我"界面中的状态显示
                if (meStatus) meStatus.textContent = `${emoji} ${text}`;
                
                console.log('显示用户状态:', emoji, text);
            } else {
                // 没有状态时显示默认状态
                if (statusEmoji) statusEmoji.textContent = '😊';
                if (statusText) statusText.textContent = '暂无状态';
                if (statusDisplay) statusDisplay.style.display = 'flex';
                
                // 更新"我"界面中的状态显示
                if (meStatus) meStatus.textContent = '😊 暂无状态';
                
                console.log('显示默认状态 - 用户没有设置状态');
            }
        }
    
        // 标签栏点击事件
        document.addEventListener('DOMContentLoaded', async function() {
            // 绑定标签栏点击事件
            document.querySelectorAll('.tab-item').forEach(item => {
                item.addEventListener('click', function() {
                    const tabName = this.getAttribute('data-tab');
                    switchTab(tabName);
                });
            });
    
            // 检查登录状态
            userInfo = getUserInfo();
            
            if (userInfo) {
                // 验证token有效性
                const isTokenValid = await validateToken();
                
                if (isTokenValid) {
                    // 用户已登录且token有效，从服务器获取最新用户信息
                    try {
                        await updateUserInfoDisplay();
                        // updateUserInfoDisplay已经调用了displayUserInfo，无需重复调用
                    } catch (error) {
                        console.error('获取用户信息失败:', error);
                        // 如果获取失败，使用本地存储的信息
                        displayUserInfo(userInfo);
                    }
                    
                    initChatList();
                    initContactsList();
                    initMomentsList();
                    
                    // 显示主界面
                    document.getElementById('loadingContent').classList.remove('active');
                    document.getElementById('chatsTab').classList.add('active');
                    document.getElementById('tabBar').style.display = 'flex';
                    
                    // 延迟初始化文件管理功能，确保页面完全加载后再执行
                    setTimeout(() => {
                        try {
                            setupFileEventListeners();
                            loadFileList();
                            loadFileStats();
                            // 初始化状态设置功能
                            initializeStatusSettings();
                        } catch (error) {
                            console.error('文件管理初始化失败:', error);
                        }
                    }, 500); // 延迟500ms确保页面完全渲染
                    
                    // 启动定时刷新机制，每30秒检查一次用户状态
                    startAutoRefresh();
                } else {
                    // token已过期，清除登录信息并跳转到登录页
                    clearLoginInfo();
                    window.location.href = '/login.html';
                }
            } else {
                // 用户未登录
                document.getElementById('loadingContent').classList.remove('active');
                document.getElementById('notLoggedInContent').classList.add('active');
            }
        });
    
        // 聊天功能实现
        let currentChatId = null;
        let currentChatReceiverId = null;
        let currentChatName = null;
        
        function openChat(chatId) {
            if (!chatId) {
                showMessage('会话ID无效', 'error');
                return;
            }
            
            currentChatId = chatId;
            
            // 获取会话信息
            getChatInfo(chatId).then(chatInfo => {
                if (chatInfo) {
                    currentChatReceiverId = chatInfo.receiverId;
                    currentChatName = chatInfo.name;
                    showChatWindow(chatInfo);
                    loadChatMessages(chatId);
                } else {
                    showMessage('无法获取会话信息', 'error');
                }
            }).catch(error => {
                console.error('获取会话信息失败:', error);
                showMessage('获取会话信息失败', 'error');
            });
        }
        
        // 获取会话信息
        async function getChatInfo(chatId) {
            const token = getAuthToken();
            const userInfo = getUserInfo();
            
            if (!token || !userInfo) {
                return null;
            }
            
            try {
                const response = await fetch(`/api/conversations/${chatId}?userId=${userInfo.id}`, {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    }
                });
                
                if (response.status === 401) {
                    clearLoginInfo();
                    window.location.href = '/login.html';
                    return null;
                }
                
                if (!response.ok) {
                    throw new Error('获取会话信息失败');
                }
                
                const data = await response.json();
                if (data.success && data.data) {
                    // 确定接收者ID和会话名称
                    const conversation = data.data;
                    let receiverId = null;
                    let chatName = conversation.name || '未知会话';
                    
                    if (conversation.type === 'PRIVATE') {
                        // 私聊：接收者是对方用户
                        const otherParticipant = conversation.participants?.find(p => p.userId !== userInfo.id);
                        if (otherParticipant) {
                            receiverId = otherParticipant.userId;
                            // 优先使用备注名，然后是昵称，最后是用户名
                            chatName = otherParticipant.remark || otherParticipant.nickname || otherParticipant.username || `用户${receiverId}`;
                        }
                    } else {
                        // 群聊：接收者为空（群聊消息）
                        receiverId = null;
                        // 群聊使用会话名称，如果没有则显示群聊
                        chatName = conversation.name || '群聊';
                    }
                    
                    return {
                        id: conversation.id,
                        name: chatName,
                        type: conversation.type,
                        receiverId: receiverId,
                        participants: conversation.participants
                    };
                }
                
                return null;
            } catch (error) {
                console.error('获取会话信息失败:', error);
                return null;
            }
        }
        
        // 显示聊天窗口
        function showChatWindow(chatInfo) {
            // 创建聊天窗口HTML
            const chatWindowHtml = `
                <div id="chatWindow" class="chat-window">
                    <div class="chat-header">
                        <button class="back-btn" onclick="closeChatWindow()">←</button>
                        <div class="chat-title">
                            <div class="chat-name">${escapeHtml(chatInfo.name)}</div>
                            <div class="chat-status">在线</div>
                        </div>
                        <div class="chat-actions">
                            <button class="action-btn">⋯</button>
                        </div>
                    </div>
                    <div class="chat-messages" id="chatMessages">
                        <div class="loading-messages">加载消息中...</div>
                    </div>
                    <div class="chat-input-area">
                        <div class="input-toolbar">
                            <button class="toolbar-btn" title="表情">😊</button>
                            <button class="toolbar-btn" title="文件">📎</button>
                        </div>
                        <div class="input-container">
                            <textarea id="messageInput" placeholder="输入消息..." rows="1"></textarea>
                            <button id="sendButton" onclick="sendMessage()" disabled>发送</button>
                        </div>
                    </div>
                </div>
            `;
            
            // 隐藏所有标签页内容
            document.querySelectorAll('.tab-content').forEach(tab => {
                tab.classList.remove('active');
            });
            
            // 隐藏底部标签栏
            document.getElementById('tabBar').style.display = 'none';
            
            // 添加聊天窗口到页面
            const mainContent = document.querySelector('.main-content');
            mainContent.innerHTML = chatWindowHtml;
            
            // 绑定输入框事件
            const messageInput = document.getElementById('messageInput');
            const sendButton = document.getElementById('sendButton');
            
            messageInput.addEventListener('input', function() {
                const hasContent = this.value.trim().length > 0;
                sendButton.disabled = !hasContent;
                
                // 自动调整高度
                this.style.height = 'auto';
                this.style.height = Math.min(this.scrollHeight, 120) + 'px';
            });
            
            messageInput.addEventListener('keydown', function(e) {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    if (!sendButton.disabled) {
                        sendMessage();
                    }
                }
            });
            
            // 聚焦输入框
            setTimeout(() => {
                messageInput.focus();
            }, 100);
        }
        
        // 关闭聊天窗口
        function closeChatWindow() {
            currentChatId = null;
            currentChatReceiverId = null;
            currentChatName = null;
            
            // 移除聊天窗口
            const chatWindow = document.getElementById('chatWindow');
            if (chatWindow) {
                chatWindow.remove();
            }
            
            // 恢复主内容区域的原始HTML结构
            const mainContent = document.querySelector('.main-content');
            if (mainContent) {
                // 重新加载原始的标签页内容结构
                location.reload();
                return;
            }
            
            // 如果无法重新加载，则尝试手动恢复
            // 显示底部标签栏
            const tabBar = document.getElementById('tabBar');
            if (tabBar) {
                tabBar.style.display = 'flex';
            }
            
            // 延迟执行标签页切换，确保DOM结构已恢复
            setTimeout(() => {
                // 获取当前活跃标签页并切换
                const activeTab = document.querySelector('.tab-item.active');
                if (activeTab) {
                    const tabName = activeTab.getAttribute('data-tab');
                    if (tabName) {
                        switchTab(tabName);
                        // 根据标签页类型初始化对应内容
                        initTabContent(tabName);
                    }
                } else {
                    // 默认显示会话页面
                    switchTab('chats');
                    initTabContent('chats');
                }
            }, 100);
        }
        
        // 根据标签页类型初始化内容
        function initTabContent(tabName) {
            switch(tabName) {
                case 'chats':
                    initChatList();
                    break;
                case 'contacts':
                    initContactsList();
                    break;
                case 'moments':
                    initMomentsList();
                    break;
                case 'me':
                    // 我的页面通常不需要特殊初始化
                    break;
                default:
                    console.warn('未知的标签页类型:', tabName);
            }
        }
        
        // 加载聊天消息
        async function loadChatMessages(chatId) {
            const chatMessages = document.getElementById('chatMessages');
            const token = getAuthToken();
            const userInfo = getUserInfo();
            
            if (!token || !userInfo) {
                chatMessages.innerHTML = '<div class="error-message">请先登录</div>';
                return;
            }
            
            try {
                const response = await fetch(`/api/messages/conversation/${chatId}?page=0&size=50&userId=${userInfo.id}`, {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    }
                });
                
                if (response.status === 401) {
                    clearLoginInfo();
                    window.location.href = '/login.html';
                    return;
                }
                
                if (!response.ok) {
                    throw new Error('获取消息失败');
                }
                
                const data = await response.json();
                if (data.success && data.data && data.data.content) {
                    displayMessages(data.data.content.reverse()); // 反转消息顺序，最新的在下面
                } else {
                    chatMessages.innerHTML = '<div class="no-messages">暂无消息</div>';
                }
            } catch (error) {
                console.error('加载消息失败:', error);
                chatMessages.innerHTML = '<div class="error-message">加载消息失败</div>';
            }
        }
        
        // 显示消息列表
        function displayMessages(messages) {
            const chatMessages = document.getElementById('chatMessages');
            const userInfo = getUserInfo();
            
            if (!messages || messages.length === 0) {
                chatMessages.innerHTML = '<div class="no-messages">暂无消息</div>';
                return;
            }
            
            const messagesHtml = messages.map(message => {
                const isOwn = message.senderId === userInfo.id;
                const messageTime = formatMessageTime(message.createdAt);
                
                return `
                    <div class="message ${isOwn ? 'own' : 'other'}">
                        <div class="message-content">
                            <div class="message-text">${escapeHtml(message.content)}</div>
                            <div class="message-time">${messageTime}</div>
                        </div>
                    </div>
                `;
            }).join('');
            
            chatMessages.innerHTML = messagesHtml;
            
            // 滚动到底部
            setTimeout(() => {
                chatMessages.scrollTop = chatMessages.scrollHeight;
            }, 100);
        }
        
        // 格式化消息时间
        function formatMessageTime(timeStr) {
            if (!timeStr) return '';
            
            const time = new Date(timeStr);
            const now = new Date();
            const diffMs = now - time;
            const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
            
            if (diffDays === 0) {
                return time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
            } else if (diffDays === 1) {
                return '昨天 ' + time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
            } else {
                return time.toLocaleDateString('zh-CN') + ' ' + time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
            }
        }
        
        // 发送消息
        async function sendMessage() {
            const messageInput = document.getElementById('messageInput');
            const sendButton = document.getElementById('sendButton');
            const content = messageInput.value.trim();
            
            if (!content) {
                return;
            }
            
            if (!currentChatId) {
                showMessage('会话信息错误', 'error');
                return;
            }
            
            const token = getAuthToken();
            const userInfo = getUserInfo();
            
            if (!token || !userInfo) {
                showMessage('请先登录', 'error');
                return;
            }
            
            // 禁用发送按钮
            sendButton.disabled = true;
            sendButton.textContent = '发送中...';
            
            try {
                const requestData = {
                    conversationId: currentChatId,
                    receiverId: currentChatReceiverId,
                    content: content,
                    messageType: 'TEXT',
                    autoCreateConversation: true
                };
                
                const response = await fetch('/api/messages/send', {
                    method: 'POST',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json',
                        'X-User-Id': userInfo.id.toString()
                    },
                    body: JSON.stringify(requestData)
                });
                
                if (response.status === 401) {
                    clearLoginInfo();
                    window.location.href = '/login.html';
                    return;
                }
                
                const data = await response.json();
                if (data.success) {
                    // 清空输入框
                    messageInput.value = '';
                    messageInput.style.height = 'auto';
                    
                    // 添加消息到界面
                    addMessageToChat({
                        id: data.data.id,
                        content: content,
                        senderId: userInfo.id,
                        createdAt: new Date().toISOString()
                    });
                    
                    // 重新加载会话列表以更新最后消息
                    initChatList();
                } else {
                    throw new Error(data.message || '发送失败');
                }
            } catch (error) {
                console.error('发送消息失败:', error);
                showMessage('发送失败: ' + error.message, 'error');
            } finally {
                // 恢复发送按钮
                sendButton.disabled = false;
                sendButton.textContent = '发送';
                messageInput.focus();
            }
        }
        
        // 添加消息到聊天界面
        function addMessageToChat(message) {
            const chatMessages = document.getElementById('chatMessages');
            const userInfo = getUserInfo();
            
            if (!chatMessages) return;
            
            // 如果是"暂无消息"状态，先清空
            if (chatMessages.innerHTML.includes('暂无消息') || chatMessages.innerHTML.includes('no-messages')) {
                chatMessages.innerHTML = '';
            }
            
            const isOwn = message.senderId === userInfo.id;
            const messageTime = formatMessageTime(message.createdAt);
            
            const messageHtml = `
                <div class="message ${isOwn ? 'own' : 'other'}">
                    <div class="message-content">
                        <div class="message-text">${escapeHtml(message.content)}</div>
                        <div class="message-time">${messageTime}</div>
                    </div>
                </div>
            `;
            
            chatMessages.insertAdjacentHTML('beforeend', messageHtml);
            
            // 滚动到底部
            setTimeout(() => {
                chatMessages.scrollTop = chatMessages.scrollHeight;
            }, 100);
        }
    
        async function openContact(contactId) {
            const token = getAuthToken();
            const userInfo = getUserInfo();
            
            if (!token || !userInfo) {
                showMessage('请先登录', 'error');
                return;
            }
            
            if (!contactId || contactId === 'null' || contactId === 'undefined') {
                showMessage('联系人信息错误', 'error');
                return;
            }
            
            try {
                // 调用API获取或创建私聊会话
                const response = await fetch(`/api/conversations/private/${contactId}?userId=${userInfo.id}`, {
                    method: 'POST',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    }
                });
                
                if (response.status === 401) {
                    clearLoginInfo();
                    window.location.href = '/login.html';
                    return;
                }
                
                if (!response.ok) {
                    throw new Error('获取会话失败');
                }
                
                const data = await response.json();
                if (data.success && data.conversation) {
                    // 获取会话ID并打开聊天窗口
                    const conversationId = data.conversation.id;
                    openChat(conversationId);
                    
                    // 刷新会话列表以显示新会话
                    setTimeout(() => {
                        initChatList();
                    }, 500);
                } else {
                    throw new Error(data.message || '获取会话失败');
                }
            } catch (error) {
                console.error('打开联系人聊天失败:', error);
                showMessage('打开聊天失败: ' + error.message, 'error');
            }
        }
        
        // 显示联系人菜单
        function showContactMenu(friendId, friendName) {
            // 检查 friendId 是否有效
            if (!friendId || friendId === 'null' || friendId === 'undefined') {
                console.error('showContactMenu: friendId 无效:', friendId);
                showMessage('联系人信息错误，无法操作', 'error');
                return;
            }
            
            // 移除已存在的菜单
            const existingMenu = document.querySelector('.contact-menu');
            if (existingMenu) {
                existingMenu.remove();
            }
            
            // 创建菜单
            const menu = document.createElement('div');
            menu.className = 'contact-menu';
            menu.innerHTML = `
                <div class="contact-menu-overlay" onclick="closeContactMenu()"></div>
                <div class="contact-menu-content">
                    <div class="contact-menu-header">
                        <span class="contact-menu-title">${escapeHtml(friendName)}</span>
                        <button class="contact-menu-close" onclick="closeContactMenu()">✕</button>
                    </div>
                    <div class="contact-menu-actions">
                        <button class="contact-menu-action" onclick="openContact(${friendId}); closeContactMenu();">
                            <span class="action-icon">👤</span>
                            <span class="action-text">查看资料</span>
                        </button>
                        <button class="contact-menu-action" onclick="${friendId ? `setContactAlias(${friendId}, '${escapeHtml(friendName)}'); closeContactMenu();` : 'alert(\"联系人信息错误\");'}">
                            <span class="action-icon">✏️</span>
                            <span class="action-text">设置备注</span>
                        </button>
                        <button class="contact-menu-action" onclick="${friendId ? `showAssignTagModal(${friendId}, '${escapeHtml(friendName)}'); closeContactMenu();` : 'alert(\"联系人信息错误\");'}">
                            <span class="action-icon">🏷️</span>
                            <span class="action-text">分配标签</span>
                        </button>
                        <button class="contact-menu-action danger" onclick="${friendId ? `confirmDeleteContact(${friendId}, '${escapeHtml(friendName)}'); closeContactMenu();` : 'alert(\"联系人信息错误\");'}">
                            <span class="action-icon">🗑️</span>
                            <span class="action-text">删除好友</span>
                        </button>
                    </div>
                </div>
            `;
            
            document.body.appendChild(menu);
            
            // 添加动画效果
            setTimeout(() => {
                menu.classList.add('show');
            }, 10);
        }
        
        // 关闭联系人菜单
        function closeContactMenu() {
            const menu = document.querySelector('.contact-menu');
            if (menu) {
                menu.classList.remove('show');
                setTimeout(() => {
                    if (menu.parentNode) {
                        menu.parentNode.removeChild(menu);
                    }
                }, 300);
            }
        }
        
        // 确认删除好友
        function confirmDeleteContact(friendId, friendName) {
            showDeleteContactModal(friendId, friendName);
        }
        
        // 删除好友
        async function deleteContact(friendId, friendName) {
            const token = getAuthToken();
            if (!token) {
                alert('请先登录');
                return;
            }
            
            // 获取当前用户信息
            const userInfo = getUserInfo();
            if (!userInfo || !userInfo.id) {
                alert('无法获取用户信息，请重新登录');
                return;
            }
            
            try {
                const response = await fetch(`/api/contacts/${friendId}?userId=${userInfo.id}`, {
                    method: 'DELETE',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    }
                });
                
                if (response.status === 401) {
                    alert('登录已过期，请重新登录');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return;
                }
                
                const data = await response.json();
                if (data.success) {
                    showMessage(`已删除好友 "${friendName}"`, 'success');
                    // 刷新联系人列表
                    await initContactsList();
                } else {
                    throw new Error(data.message || '删除好友失败');
                }
            } catch (error) {
                console.error('删除好友失败:', error);
                showMessage('删除好友失败: ' + error.message, 'error');
            }
        }
        
        // 设置联系人备注
        // 全局变量存储当前设置备注的联系人信息


​        
        // 全局变量存储当前设置备注的联系人信息
        let currentAliasContactId = null;
        
        // 标签管理相关全局变量
        let currentAssignContactId = null;
        let currentAssignContactName = null;
        let allTags = [];
        let contactTags = [];
        
        function setContactAlias(friendId, currentName) {
            // 添加调试信息
            console.log('setContactAlias 被调用:', {
                friendId: friendId,
                friendIdType: typeof friendId,
                currentName: currentName,
                currentNameType: typeof currentName,
                previousContactId: currentAliasContactId
            });
            
            // 检查 friendId 是否有效
            if (!friendId || friendId === 'null' || friendId === 'undefined') {
                console.error('setContactAlias: friendId 无效:', friendId);
                showMessage('联系人信息错误，无法设置备注', 'error');
                return;
            }
            
            // 强制清理之前的状态，确保干净的开始
            if (currentAliasContactId !== null) {
                console.warn('检测到之前的联系人ID未清理:', currentAliasContactId);
            }
            
            // 存储当前联系人信息
            currentAliasContactId = friendId;
            
            // 验证存储是否成功
            if (currentAliasContactId !== friendId) {
                console.error('friendId 存储失败:', {
                    expected: friendId,
                    actual: currentAliasContactId
                });
                showMessage('系统错误，无法设置备注', 'error');
                return;
            }
            
            // 设置模态框内容
            document.getElementById('setAliasContactName').textContent = currentName;
            document.getElementById('aliasInput').value = currentName || '';
            
            // 更新字符计数
            updateAliasCharCount();
            
            // 显示模态框
            const modal = document.getElementById('setAliasModal');
            modal.style.display = 'flex';
            
            // 聚焦输入框
            setTimeout(() => {
                const input = document.getElementById('aliasInput');
                input.focus();
                input.select();
            }, 100);
            
            console.log('模态框已打开，当前联系人ID:', currentAliasContactId);
        }
        
        // 关闭设置备注模态框
        function closeSetAliasModal() {
            console.log('closeSetAliasModal 被调用，清理前状态:', {
                currentAliasContactId: currentAliasContactId,
                contactIdType: typeof currentAliasContactId
            });
            
            const modal = document.getElementById('setAliasModal');
            modal.style.display = 'none';
            
            // 清理全局状态
            const previousContactId = currentAliasContactId;
            currentAliasContactId = null;
            
            console.log('模态框已关闭，状态已清理:', {
                previousContactId: previousContactId,
                currentContactId: currentAliasContactId
            });
        }
        
        // 确认设置备注
        function confirmSetAlias() {
            console.log('confirmSetAlias 被调用，当前状态:', {
                currentAliasContactId: currentAliasContactId,
                contactIdType: typeof currentAliasContactId
            });
            
            const aliasInput = document.getElementById('aliasInput');
            const newAlias = aliasInput.value.trim();
            
            // 如果输入为空，则不进行操作
            if (newAlias === '') {
                showMessage('备注名不能为空', 'warning');
                return;
            }
            
            // 双重检查currentAliasContactId的有效性
            if (!currentAliasContactId || currentAliasContactId === 'null' || currentAliasContactId === 'undefined') {
                console.error('confirmSetAlias: currentAliasContactId 无效:', {
                    value: currentAliasContactId,
                    type: typeof currentAliasContactId,
                    isNull: currentAliasContactId === null,
                    isUndefined: currentAliasContactId === undefined
                });
                showMessage('设置备注失败: 联系人信息丢失，请重新打开设置窗口', 'error');
                return;
            }
            
            // 先保存friendId，避免在closeSetAliasModal中被清空
            const friendIdToUpdate = currentAliasContactId;
            console.log('准备更新备注:', {
                friendIdToUpdate: friendIdToUpdate,
                newAlias: newAlias
            });
            
            closeSetAliasModal();
            
            // 最后验证friendIdToUpdate
            if (!friendIdToUpdate || friendIdToUpdate === 'null' || friendIdToUpdate === 'undefined') {
                console.error('friendIdToUpdate 在保存后变为无效:', friendIdToUpdate);
                showMessage('设置备注失败: 系统错误', 'error');
                return;
            }
            
            // 使用保存的friendId调用更新函数
            updateContactAlias(friendIdToUpdate, newAlias);
        }
        
        // 更新字符计数
        function updateAliasCharCount() {
            const input = document.getElementById('aliasInput');
            const charCount = document.getElementById('aliasCharCount');
            charCount.textContent = input.value.length;
        }
        
        // 页面加载完成后添加事件监听器
        document.addEventListener('DOMContentLoaded', function() {
            // 为备注输入框添加实时字符计数
            const aliasInput = document.getElementById('aliasInput');
            if (aliasInput) {
                aliasInput.addEventListener('input', updateAliasCharCount);
                
                // 添加回车键确认功能
                aliasInput.addEventListener('keypress', function(e) {
                    if (e.key === 'Enter') {
                        confirmSetAlias();
                    }
                });
            }
            
            // 点击模态框外部关闭
            const setAliasModal = document.getElementById('setAliasModal');
            if (setAliasModal) {
                setAliasModal.addEventListener('click', function(e) {
                    if (e.target === setAliasModal) {
                        console.log('模态框外部点击关闭，当前contactId:', currentAliasContactId);
                        closeSetAliasModal();
                    }
                });
            }
            
            // 添加全局错误监听器
            window.addEventListener('error', function(e) {
                console.error('全局错误捕获:', e.error);
                if (currentAliasContactId) {
                    console.warn('检测到错误时currentAliasContactId仍有值:', currentAliasContactId);
                }
            });
            
            // 监听页面可见性变化（可能导致状态丢失）
            document.addEventListener('visibilitychange', function() {
                if (document.hidden) {
                    console.log('页面隐藏，当前contactId:', currentAliasContactId);
                } else {
                    console.log('页面显示，当前contactId:', currentAliasContactId);
                }
            });
            
            // 监听ESC键关闭模态框
            document.addEventListener('keydown', function(e) {
                if (e.key === 'Escape') {
                    const modal = document.getElementById('setAliasModal');
                    if (modal && modal.style.display === 'block') {
                        console.log('ESC键关闭模态框，当前contactId:', currentAliasContactId);
                        closeSetAliasModal();
                    }
                }
            });
        });


​        
        // 更新联系人备注
        async function updateContactAlias(friendId, alias) {
            // 最终的参数验证
            console.log('updateContactAlias 被调用:', {
                friendId: friendId,
                friendIdType: typeof friendId,
                alias: alias,
                aliasType: typeof alias
            });
            
            // 严格验证friendId
            if (!friendId || friendId === 'null' || friendId === 'undefined' || friendId === null || friendId === undefined) {
                console.error('updateContactAlias: friendId 参数无效:', {
                    value: friendId,
                    type: typeof friendId,
                    isNull: friendId === null,
                    isUndefined: friendId === undefined,
                    isStringNull: friendId === 'null',
                    isStringUndefined: friendId === 'undefined'
                });
                showMessage('更新备注失败: 联系人ID无效', 'error');
                return;
            }
            
            const token = getAuthToken();
            if (!token) {
                alert('请先登录');
                return;
            }
            
            // 添加调试信息
            console.log('设置备注请求:', {
                friendId: friendId,
                alias: alias,
                token: token ? '已获取' : '未获取'
            });
            
            try {
                const response = await fetch(`/api/contacts/${friendId}/alias`, {
                    method: 'PUT',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ alias: alias })
                });
                
                console.log('API响应状态:', response.status);
                
                if (response.status === 401) {
                    alert('登录已过期，请重新登录');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return;
                }
                
                const data = await response.json();
                console.log('API响应数据:', data);
                
                if (data.success) {
                    showMessage('备注设置成功', 'success');
                    // 刷新联系人列表
                    await initContactsList();
                } else {
                    throw new Error(data.message || '设置备注失败');
                }
            } catch (error) {
                console.error('设置备注失败:', error);
                showMessage('设置备注失败: ' + error.message, 'error');
            }
        }
    
        // 清除联系人备注
        async function clearContactAlias() {
            console.log('clearContactAlias 被调用，当前状态:', {
                currentAliasContactId: currentAliasContactId,
                contactIdType: typeof currentAliasContactId
            });
            
            // 双重检查currentAliasContactId的有效性
            if (!currentAliasContactId || currentAliasContactId === 'null' || currentAliasContactId === 'undefined') {
                console.error('clearContactAlias: currentAliasContactId 无效:', {
                    value: currentAliasContactId,
                    type: typeof currentAliasContactId,
                    isNull: currentAliasContactId === null,
                    isUndefined: currentAliasContactId === undefined
                });
                showMessage('清除备注失败: 联系人信息丢失，请重新打开设置窗口', 'error');
                return;
            }
            
            // 提示用户操作效果
            showMessage('正在清除备注，该好友将显示原始昵称', 'info');
            
            // 先保存friendId，避免在closeSetAliasModal中被清空
            const friendIdToUpdate = currentAliasContactId;
            console.log('准备清除备注:', {
                friendIdToUpdate: friendIdToUpdate
            });
            
            closeSetAliasModal();
            
            // 最后验证friendIdToUpdate
            if (!friendIdToUpdate || friendIdToUpdate === 'null' || friendIdToUpdate === 'undefined') {
                console.error('friendIdToUpdate 在保存后变为无效:', friendIdToUpdate);
                showMessage('清除备注失败: 系统错误', 'error');
                return;
            }
            
            // 使用空字符串作为alias来清除备注
            updateContactAlias(friendIdToUpdate, '');
        }
    
        function openNewFriends() {
            alert('新朋友功能正在开发中...');
        }
    
        function openGroupChats() {
            alert('群聊功能正在开发中...');
        }
    
        function openTags() {
            alert('标签功能正在开发中...');
        }
        
        // 标签管理相关函数
        
        // 显示标签管理页面
        function showTagsPage() {
            console.log('showTagsPage() 被调用');
            
            // 隐藏所有tab-content
            document.querySelectorAll('.tab-content').forEach(tab => {
                tab.classList.remove('active');
            });
            console.log('所有页面已隐藏');
            
            // 显示标签管理页面
            const tagsTab = document.getElementById('tagsTab');
            if (tagsTab) {
                tagsTab.classList.add('active');
                console.log('标签页面已显示（添加active类）');
            } else {
                console.error('找不到 tagsTab 元素');
            }
            
            // 加载标签列表
            console.log('开始加载标签列表');
            loadTags();
        }
        
        // 返回联系人页面
        function backToContacts() {
            // 隐藏所有tab-content
            document.querySelectorAll('.tab-content').forEach(tab => {
                tab.classList.remove('active');
            });
            
            // 显示联系人页面
            const contactsTab = document.getElementById('contactsTab');
            if (contactsTab) {
                contactsTab.classList.add('active');
            }
        }
        
        // 加载标签列表
        async function loadTags() {
            console.log('loadTags() 开始执行');
            
            const token = getAuthToken();
            console.log('获取到的token:', token ? '存在' : '不存在');
            
            if (!token) {
                console.error('没有找到认证token');
                showMessage('请先登录', 'error');
                return;
            }
            
            try {
                console.log('开始发送API请求到 /api/tags');
                const response = await fetch('/api/tags', {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    }
                });
                
                console.log('API响应状态:', response.status);
                
                if (response.status === 401) {
                    console.error('认证失败，清除登录信息');
                    clearLoginInfo();
                    window.location.href = '/login.html';
                    return;
                }
                
                const data = await response.json();
                console.log('API响应数据:', data);
                
                if (data.success) {
                    allTags = data.data || [];
                    console.log('获取到的标签数量:', allTags.length);
                    displayTags(allTags);
                    updateTagStats();
                } else {
                    throw new Error(data.message || '加载标签失败');
                }
            } catch (error) {
                console.error('加载标签失败:', error);
                showMessage('加载标签失败: ' + error.message, 'error');
            }
        }
        
        // 显示标签列表
        function displayTags(tags) {
            console.log('displayTags() 开始执行，标签数量:', tags ? tags.length : 0);
            
            const tagsList = document.getElementById('tagsList');
            if (!tagsList) {
                console.error('找不到 tagsList 元素');
                return;
            }
            console.log('找到 tagsList 元素');
            
            if (!tags || tags.length === 0) {
                console.log('没有标签数据，显示空状态');
                tagsList.innerHTML = '<div class="no-tags">暂无标签，点击右上角创建标签</div>';
                console.log('空状态HTML已设置:', tagsList.innerHTML);
                console.log('tagsList元素样式:', window.getComputedStyle(tagsList));
                console.log('标签页面是否可见:', document.getElementById('tagsTab').style.display);
                return;
            }
            
            console.log('开始渲染标签列表');
            tagsList.innerHTML = tags.map(tag => `
                <div class="tag-item">
                    <div class="tag-color" style="background-color: ${tag.color}"></div>
                    <div class="tag-info">
                        <div class="tag-name">${escapeHtml(tag.name)}</div>
                        <div class="tag-usage">已使用 ${tag.contactCount || 0} 次</div>
                    </div>
                    <div class="tag-actions">
                        <button class="tag-action-btn" onclick="viewTagContacts(${tag.tagId}, '${escapeHtml(tag.name)}')" title="查看好友">
                            👥
                        </button>
                        <button class="tag-action-btn" onclick="editTag(${tag.tagId}, '${escapeHtml(tag.name)}', '${tag.color}')" title="编辑">
                            ✏️
                        </button>
                        <button class="tag-action-btn" onclick="deleteTag(${tag.tagId}, '${escapeHtml(tag.name)}')" title="删除">
                            🗑️
                        </button>
                    </div>
                </div>
            `).join('');
            console.log('标签列表渲染完成');
        }
        
        // 更新标签统计
        function updateTagStats() {
            document.getElementById('totalTagsCount').textContent = allTags.length;
            const usedTags = allTags.filter(tag => (tag.contactCount || 0) > 0).length;
            document.getElementById('usedTagsCount').textContent = usedTags;
            
            // 最近创建的标签数量（最近7天创建的标签）
            const recentTagsCount = allTags.length > 0 ? Math.min(allTags.length, 3) : 0;
            document.getElementById('recentTagsCount').textContent = recentTagsCount;
        }
        
        // 显示创建标签模态框
        function showCreateTagModal() {
            const modal = document.getElementById('createTagModal');
            modal.classList.add('show');
            document.getElementById('tagNameInput').value = '';
            document.getElementById('tagColorInput').value = '#667eea';
            updateCreateColorPreview();
            updateCharCount();
            
            // 清除所有颜色选项的选中状态
            document.querySelectorAll('.color-option').forEach(option => {
                option.classList.remove('selected');
            });
            
            // 设置默认颜色为选中状态
            const defaultColorOption = document.querySelector('.color-option[data-color="#667eea"]');
            if (defaultColorOption) {
                defaultColorOption.classList.add('selected');
            }
        }
        
        // 关闭创建标签模态框
        function closeCreateTagModal() {
            const modal = document.getElementById('createTagModal');
            modal.classList.remove('show');
        }
        
        // 显示编辑标签模态框
        function editTag(tagId, tagName, tagColor) {
            const modal = document.getElementById('editTagModal');
            modal.classList.add('show');
            
            // 设置表单值
            document.getElementById('editTagId').value = tagId;
            document.getElementById('editTagNameInput').value = tagName;
            document.getElementById('editTagColorInput').value = tagColor;
            
            // 更新颜色预览和字符计数
            updateEditColorPreview();
            updateEditCharCount();
            
            // 清除所有颜色选项的选中状态
            document.querySelectorAll('#editTagModal .color-option').forEach(option => {
                option.classList.remove('selected');
            });
            
            // 设置当前颜色为选中状态
            const currentColorOption = document.querySelector(`#editTagModal .color-option[data-color="${tagColor}"]`);
            if (currentColorOption) {
                currentColorOption.classList.add('selected');
            }
        }
        
        // 关闭编辑标签模态框
        function closeEditTagModal() {
            const modal = document.getElementById('editTagModal');
            modal.classList.remove('show');
        }
        
        // 更新创建标签颜色预览
        function updateCreateColorPreview() {
            const colorInput = document.getElementById('tagColorInput');
            const colorPreview = document.querySelector('.color-preview');
            if (colorInput && colorPreview) {
                const color = colorInput.value;
                colorPreview.style.background = color;
                colorPreview.style.boxShadow = `0 4px 12px ${color}40`;
            }
        }
        
        // 更新字符计数
        function updateCharCount() {
            const input = document.getElementById('tagNameInput');
            const charCount = document.querySelector('.char-count');
            if (input && charCount) {
                const currentLength = input.value.length;
                const maxLength = 20;
                charCount.textContent = `${currentLength}/${maxLength}`;
                
                if (currentLength > maxLength * 0.8) {
                    charCount.style.color = '#dc3545';
                } else {
                    charCount.style.color = '#6c757d';
                }
            }
        }
        
        // 更新编辑颜色预览
        function updateEditColorPreview() {
            const colorInput = document.getElementById('editTagColorInput');
            const colorPreview = document.querySelector('#editColorPreview');
            if (colorInput && colorPreview) {
                const color = colorInput.value;
                colorPreview.style.background = color;
                colorPreview.style.boxShadow = `0 4px 12px ${color}40`;
            }
        }
        
        // 更新编辑字符计数
        function updateEditCharCount() {
            const input = document.getElementById('editTagNameInput');
            const charCount = document.getElementById('editCharCount');
            if (input && charCount) {
                const currentLength = input.value.length;
                const maxLength = 20;
                charCount.textContent = `${currentLength}/${maxLength}`;
                
                if (currentLength > maxLength * 0.8) {
                    charCount.style.color = '#dc3545';
                } else {
                    charCount.style.color = '#6c757d';
                }
            }
        }
        
        // 选择创建标签预设颜色
        function selectCreateColor(color) {
            document.getElementById('tagColorInput').value = color;
            updateCreateColorPreview();
            
            // 更新选中状态
            document.querySelectorAll('.color-option').forEach(option => {
                option.classList.remove('selected');
            });
            
            const selectedOption = document.querySelector(`.color-option[data-color="${color}"]`);
            if (selectedOption) {
                selectedOption.classList.add('selected');
            }
        }
        
        // 选择预设颜色（保持向后兼容）
        function selectColor(color) {
            selectCreateColor(color);
        }
        
        // 选择编辑预设颜色
        function selectEditColor(color) {
            document.getElementById('editTagColorInput').value = color;
            updateEditColorPreview();
            
            // 更新选中状态
            document.querySelectorAll('#editTagModal .color-option').forEach(option => {
                option.classList.remove('selected');
            });
            
            const selectedOption = document.querySelector(`#editTagModal .color-option[data-color="${color}"]`);
            if (selectedOption) {
                selectedOption.classList.add('selected');
            }
        }
        
        // 创建标签
        async function createTag() {
            const name = document.getElementById('tagNameInput').value.trim();
            const color = document.getElementById('tagColorInput').value;
            
            // 参数验证
            if (!name) {
                showMessage('标签名称不能为空', 'warning');
                return;
            }
            
            if (name.length > 20) {
                showMessage('标签名称不能超过20个字符', 'warning');
                return;
            }
            
            if (!/^#[0-9A-Fa-f]{6}$/.test(color)) {
                showMessage('颜色格式不正确', 'warning');
                return;
            }
            
            const token = getAuthToken();
            if (!token) {
                showMessage('请先登录', 'error');
                return;
            }
            
            try {
                // 获取当前用户ID
                const userId = await getCurrentUserId();
                
                const requestBody = {
                    userId: userId,
                    name: name,
                    color: color
                };
                
                console.log('发送创建标签请求:', requestBody);
                
                const response = await fetch('/api/tags', {
                    method: 'POST',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(requestBody)
                });
                
                console.log('响应状态码:', response.status);
                
                if (response.status === 401) {
                    clearLoginInfo();
                    window.location.href = '/login.html';
                    return;
                }
                
                if (response.status === 400) {
                    const errorText = await response.text();
                    console.error('400错误详情:', errorText);
                    try {
                        const errorData = JSON.parse(errorText);
                        throw new Error(errorData.message || '参数验证失败');
                    } catch (parseError) {
                        throw new Error('参数验证失败: ' + errorText);
                    }
                }
                
                const data = await response.json();
                console.log('响应数据:', data);
                
                if (data.success) {
                    showMessage('标签创建成功', 'success');
                    closeCreateTagModal();
                    loadTags();
                    
                    // 如果分配标签模态框是打开的，刷新其标签列表
                    if (currentAssignContactId) {
                        loadTagsForAssign(currentAssignContactId);
                    }
                } else {
                    throw new Error(data.message || '创建标签失败');
                }
            } catch (error) {
                console.error('创建标签失败:', error);
                showMessage('创建标签失败: ' + error.message, 'error');
            }
        }
        
        // 更新标签
        async function updateTag() {
            const tagId = document.getElementById('editTagId').value;
            const name = document.getElementById('editTagNameInput').value.trim();
            const color = document.getElementById('editTagColorInput').value;
            
            if (!name) {
                showMessage('标签名称不能为空', 'warning');
                return;
            }
            
            const token = getAuthToken();
            if (!token) {
                showMessage('请先登录', 'error');
                return;
            }
            
            try {
                const response = await fetch(`/api/tags/${tagId}`, {
                    method: 'PUT',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ name, color })
                });
                
                if (response.status === 401) {
                    clearLoginInfo();
                    window.location.href = '/login.html';
                    return;
                }
                
                const data = await response.json();
                if (data.success) {
                    showMessage('标签更新成功', 'success');
                    closeEditTagModal();
                    loadTags();
                } else {
                    throw new Error(data.message || '更新标签失败');
                }
            } catch (error) {
                console.error('更新标签失败:', error);
                showMessage('更新标签失败: ' + error.message, 'error');
            }
        }
        
        // 当前要删除的标签信息
        let currentDeleteTagId = null;
        let currentDeleteTagName = null;
    
        // 删除标签 - 显示确认模态框
        function deleteTag(tagId, tagName) {
            currentDeleteTagId = tagId;
            currentDeleteTagName = tagName;
            showDeleteTagModal(tagName);
        }
    
        // 显示删除标签确认模态框
        function showDeleteTagModal(tagName) {
            const modal = document.getElementById('deleteTagModal');
            const tagNameElement = document.getElementById('deleteTagName');
            
            if (tagNameElement) {
                tagNameElement.textContent = tagName;
            }
            
            if (modal) {
                modal.style.display = 'flex';
            }
        }
    
        // 关闭删除标签确认模态框
        function closeDeleteTagModal() {
            const modal = document.getElementById('deleteTagModal');
            if (modal) {
                modal.style.display = 'none';
            }
            currentDeleteTagId = null;
            currentDeleteTagName = null;
        }
    
        // 确认删除标签操作
        async function confirmDeleteTagAction() {
            if (!currentDeleteTagId) {
                showMessage('删除操作失败：标签信息丢失', 'error');
                closeDeleteTagModal();
                return;
            }
            
            const token = getAuthToken();
            if (!token) {
                showMessage('请先登录', 'error');
                closeDeleteTagModal();
                return;
            }
            
            try {
                const response = await fetch(`/api/tags/${currentDeleteTagId}`, {
                    method: 'DELETE',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    }
                });
                
                if (response.status === 401) {
                    clearLoginInfo();
                    window.location.href = '/login.html';
                    return;
                }
                
                const data = await response.json();
                if (data.success) {
                    showMessage(`标签 "${currentDeleteTagName}" 删除成功`, 'success');
                    loadTags();
                    closeDeleteTagModal();
                    
                    // 如果分配标签模态框是打开的，刷新其标签列表
                    const assignTagModal = document.getElementById('assignTagModal');
                    if (assignTagModal && assignTagModal.style.display === 'flex' && currentAssignContactId) {
                        loadTagsForAssign(currentAssignContactId);
                    }
                } else {
                    throw new Error(data.message || '删除标签失败');
                }
            } catch (error) {
                console.error('删除标签失败:', error);
                showMessage('删除标签失败: ' + error.message, 'error');
                closeDeleteTagModal();
            }
        }
        
        // 显示分配标签模态框
        async function showAssignTagModal(contactId, contactName) {
            console.log('showAssignTagModal() 被调用，contactId:', contactId, 'contactName:', contactName);
            
            currentAssignContactId = contactId;
            currentAssignContactName = contactName;
            
            const assignContactNameEl = document.getElementById('assignContactName');
            const assignContactAvatarEl = document.getElementById('assignContactAvatar');
            const assignTagModalEl = document.getElementById('assignTagModal');
            
            console.log('找到 assignContactName 元素:', assignContactNameEl);
            console.log('找到 assignContactAvatar 元素:', assignContactAvatarEl);
            console.log('找到 assignTagModal 元素:', assignTagModalEl);
            
            if (!assignTagModalEl) {
                console.error('找不到assignTagModal元素!');
                return;
            }
            
            if (assignContactNameEl) {
                assignContactNameEl.textContent = contactName;
                console.log('设置联系人名称:', contactName);
            }
            
            // 设置联系人头像
            if (assignContactAvatarEl) {
                // 使用联系人名称生成默认头像
                const avatarText = contactName.charAt(0).toUpperCase();
                assignContactAvatarEl.innerHTML = '';
                assignContactAvatarEl.textContent = avatarText;
            }
            
            // 使用flex布局显示模态框以实现居中
            assignTagModalEl.style.display = 'flex';
            
            console.log('模态框显示状态:', assignTagModalEl.style.display);
            
            // 加载所有标签和联系人当前标签
            loadTagsForAssign(contactId);
        }
        
        // 关闭分配标签模态框
        function closeAssignTagModal() {
            const assignTagModalEl = document.getElementById('assignTagModal');
            if (assignTagModalEl) {
                assignTagModalEl.style.display = 'none';
            }
            currentAssignContactId = null;
            currentAssignContactName = null;
        }
        
        // 加载分配标签所需的数据
        async function loadTagsForAssign(contactId) {
            const token = getAuthToken();
            if (!token) {
                showMessage('请先登录', 'error');
                return;
            }
            
            try {
                // 并行加载所有标签和联系人标签
                const [tagsResponse, contactTagsResponse] = await Promise.all([
                    fetch('/api/tags', {
                        method: 'GET',
                        headers: {
                            'Authorization': 'Bearer ' + token,
                            'Content-Type': 'application/json'
                        }
                    }),
                    fetch(`/api/contacts/${contactId}/tags`, {
                        method: 'GET',
                        headers: {
                            'Authorization': 'Bearer ' + token,
                            'Content-Type': 'application/json'
                        }
                    })
                ]);
                
                if (tagsResponse.status === 401 || contactTagsResponse.status === 401) {
                    clearLoginInfo();
                    window.location.href = '/login.html';
                    return;
                }
                
                const tagsData = await tagsResponse.json();
                const contactTagsData = await contactTagsResponse.json();
                
                if (tagsData.success && contactTagsData.success) {
                    allTags = tagsData.data || [];
                    contactTags = contactTagsData.data || [];
                    console.log('加载的所有标签:', allTags);
                    console.log('联系人当前标签:', contactTags);
                    displayAvailableTags();
                } else {
                    throw new Error('加载标签数据失败');
                }
            } catch (error) {
                console.error('加载标签数据失败:', error);
                showMessage('加载标签数据失败: ' + error.message, 'error');
            }
        }
        
        // 显示可用标签
        function displayAvailableTags() {
            const availableTagsList = document.getElementById('availableTags');
            console.log('displayAvailableTags被调用，availableTagsList元素:', availableTagsList);
            console.log('allTags数据:', allTags);
            
            // 创建标签按钮
            const createTagButton = `
                <div class="create-tag-option" onclick="showCreateTagModal()">
                    <div class="create-tag-icon">+</div>
                    <div class="create-tag-text">创建新标签</div>
                </div>
            `;
            
            if (!allTags || allTags.length === 0) {
                availableTagsList.innerHTML = `
                    <div class="no-tags">暂无可用标签</div>
                    ${createTagButton}
                `;
                console.log('没有标签数据，显示提示信息和创建按钮');
                return;
            }
            
            const contactTagIds = contactTags.map(tag => tag.tagId);
            
            const tagHtml = allTags.map(tag => {
                const isSelected = contactTagIds.includes(tag.tagId);
                return `
                    <div class="tag-option ${isSelected ? 'selected' : ''}" onclick="toggleTagSelection(${tag.tagId})">
                        <div class="tag-option-color" style="background-color: ${tag.color}"></div>
                        <div class="tag-option-name">${escapeHtml(tag.name)}</div>
                        <div class="tag-checkbox">${isSelected ? '✓' : ''}</div>
                    </div>
                `;
            }).join('');
            
            availableTagsList.innerHTML = tagHtml + createTagButton;
            console.log('生成的标签HTML:', tagHtml);
            console.log('标签列表元素内容:', availableTagsList.innerHTML);
        }
        
        // 切换标签选择状态
        function toggleTagSelection(tagId) {
            const tagOption = document.querySelector(`.tag-option[onclick="toggleTagSelection(${tagId})"]`);
            const checkbox = tagOption.querySelector('.tag-checkbox');
            
            if (tagOption.classList.contains('selected')) {
                tagOption.classList.remove('selected');
                checkbox.textContent = '';
                // 从contactTags中移除
                contactTags = contactTags.filter(tag => tag.tagId !== tagId);
            } else {
                tagOption.classList.add('selected');
                checkbox.textContent = '✓';
                // 添加到contactTags中
                const tag = allTags.find(t => t.tagId === tagId);
                if (tag) {
                    contactTags.push(tag);
                }
            }
        }
        
        // 保存标签分配
        async function saveTagAssignment() {
            if (!currentAssignContactId) {
                showMessage('联系人信息错误', 'error');
                return;
            }
            
            const token = getAuthToken();
            if (!token) {
                showMessage('请先登录', 'error');
                return;
            }
            
            const tagIds = contactTags.map(tag => tag.tagId);
            console.log('准备分配的标签数据:', contactTags);
            console.log('提取的标签ID:', tagIds);
            console.log('标签ID类型检查:', tagIds.map(id => ({ id, type: typeof id, isNumber: Number.isInteger(id) })));
            console.log('当前联系人ID:', currentAssignContactId);
            console.log('所有可用标签:', allTags);
            
            try {
                const response = await fetch(`/api/contacts/${currentAssignContactId}/tags`, {
                    method: 'PUT',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ tagIds })
                });
                
                console.log('分配标签响应状态:', response.status);
                console.log('分配标签响应头:', response.headers);
                
                if (response.status === 401) {
                    clearLoginInfo();
                    window.location.href = '/login.html';
                    return;
                }
                
                const data = await response.json();
                console.log('分配标签响应数据:', data);
                
                if (data.success) {
                    showMessage('标签分配成功', 'success');
                    closeAssignTagModal();
                    // 刷新联系人列表以显示新的标签
                    await initContactsList();
                } else {
                    console.error('分配标签失败详情:', {
                        success: data.success,
                        message: data.message,
                        code: data.code,
                        data: data.data
                    });
                    throw new Error(data.message || '分配标签失败');
                }
            } catch (error) {
                console.error('分配标签失败:', error);
                showMessage('分配标签失败: ' + error.message, 'error');
            }
        }
        
        // 搜索标签
        function searchTags() {
            const keyword = document.getElementById('tagSearchInput').value.trim().toLowerCase();
            
            if (!keyword) {
                displayTags(allTags);
                return;
            }
            
            const filteredTags = allTags.filter(tag => 
                tag.name.toLowerCase().includes(keyword)
            );
            
            displayTags(filteredTags);
        }
    
        // 显示搜索提示框
        function showSearchToast(message, icon = '🔍') {
            // 移除已存在的提示框
            const existingToast = document.querySelector('.search-toast');
            if (existingToast) {
                existingToast.remove();
            }
    
            // 创建新的提示框
            const toast = document.createElement('div');
            toast.className = 'search-toast';
            toast.innerHTML = `
                <div class="search-toast-icon">${icon}</div>
                <div class="search-toast-message">${message}</div>
            `;
    
            // 添加到页面
            document.body.appendChild(toast);
    
            // 2秒后自动隐藏
            setTimeout(() => {
                toast.classList.add('hide');
                setTimeout(() => {
                    if (toast.parentNode) {
                        toast.parentNode.removeChild(toast);
                    }
                }, 300);
            }, 2000);
        }
    
        // 搜索用户功能
        async function searchUsers() {
            const searchInput = document.getElementById('contactSearchInput');
            const keyword = searchInput.value.trim();
            
            if (!keyword) {
                showSearchToast('搜索为空，请输入搜索关键词', '🔍');
                return;
            }
            
            const token = getAuthToken();
            if (!token) {
                alert('请先登录');
                return;
            }
            
            try {
                showSearchLoading();
                
                // 首先获取当前用户信息以获取用户ID
                const profileResponse = await fetch('/api/user/profile', {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    }
                });
                
                if (profileResponse.status === 401) {
                    alert('登录已过期，请重新登录');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return;
                }
                
                if (!profileResponse.ok) {
                    throw new Error('获取用户信息失败');
                }
                
                const profileData = await profileResponse.json();
                const currentUserId = (profileData.data || profileData).id;
                
                if (!currentUserId) {
                    throw new Error('无法获取当前用户ID');
                }
                
                // 使用获取到的用户ID进行搜索
                const response = await fetch(`/api/contact-search/search?keyword=${encodeURIComponent(keyword)}&currentUserId=${currentUserId}&page=0&size=20`, {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    }
                });
                
                if (response.status === 401) {
                    alert('登录已过期，请重新登录');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return;
                }
                
                if (!response.ok) {
                    throw new Error('搜索失败');
                }
                
                const data = await response.json();
                if (data.success) {
                    displaySearchResults(data.data);
                } else {
                    throw new Error(data.message || '搜索失败');
                }
            } catch (error) {
                console.error('搜索用户失败:', error);
                showSearchError('搜索失败: ' + error.message);
            }
        }
        
        // 显示搜索加载状态
        function showSearchLoading() {
            const searchResults = document.getElementById('searchResults');
            const searchResultsList = document.getElementById('searchResultsList');
            
            searchResults.style.display = 'block';
            searchResultsList.innerHTML = '<div class="search-loading">🔍 搜索中...</div>';
        }
        
        // 显示搜索结果
        function displaySearchResults(results) {
            const searchResults = document.getElementById('searchResults');
            const searchResultsList = document.getElementById('searchResultsList');
            
            searchResults.style.display = 'block';
            
            if (!results || results.length === 0) {
                searchResultsList.innerHTML = '<div class="search-empty">😔 未找到相关用户</div>';
                return;
            }
            
            searchResultsList.innerHTML = results.map(user => {
                const avatarText = (user.nickname || user.email || 'U').charAt(0).toUpperCase();
                const relationshipText = getRelationshipText(user.relationshipStatus);
                const canAddFriend = user.relationshipStatus === '陌生人';
                const hasSentRequest = user.relationshipStatus === '已发送请求';
                
                // 处理头像显示
                let avatarContent;
                if (user.avatarUrl) {
                    avatarContent = `<img src="${user.avatarUrl}" alt="头像" style="width: 100%; height: 100%; border-radius: inherit; object-fit: cover;">`;
                } else {
                    avatarContent = avatarText;
                }
                
                // 根据关系状态决定显示的按钮
                let actionButton;
                if (canAddFriend) {
                    actionButton = `<button class="add-friend-btn" onclick="sendFriendRequest('${user.userId}', '${escapeHtml(user.nickname || user.email || '未知用户')}')">添加好友</button>`;
                } else if (hasSentRequest) {
                    actionButton = `<button class="sent-request-btn" disabled>已发送</button>`;
                } else {
                    actionButton = `<span class="relationship-status">${relationshipText}</span>`;
                }
                
                return `
                    <div class="search-result-item">
                        <div class="search-result-avatar">${avatarContent}</div>
                        <div class="search-result-info">
                            <div class="search-result-name">${escapeHtml(user.nickname || user.email || '未知用户')}</div>
                            <div class="search-result-id">ID: ${escapeHtml(user.userIdString || '未设置')}</div>
                            <div class="search-result-status">${relationshipText}</div>
                        </div>
                        <div class="search-result-actions">
                            <button class="view-profile-btn" onclick="viewUserProfile('${user.userIdString}')">查看资料</button>
                            ${actionButton}
                        </div>
                    </div>
                `;
            }).join('');
        }
        
        // 获取关系状态文本
        function getRelationshipText(status) {
            switch (status) {
                case '好友':
                    return '已是好友';
                case '已发送请求':
                    return '已发送请求';
                case '待处理请求':
                    return '待处理请求';
                case '已屏蔽':
                    return '已屏蔽';
                case '陌生人':
                    return '可以添加';
                case '自己':
                    return '这是您自己';
                default:
                    return '未知状态';
            }
        }
        
        // 显示搜索错误
        function showSearchError(message) {
            const searchResults = document.getElementById('searchResults');
            const searchResultsList = document.getElementById('searchResultsList');
            
            searchResults.style.display = 'block';
            searchResultsList.innerHTML = `<div class="search-empty">❌ ${escapeHtml(message)}</div>`;
        }
        
        // 关闭搜索结果
        function closeSearch() {
            const searchResults = document.getElementById('searchResults');
            const searchInput = document.getElementById('contactSearchInput');
            
            searchResults.style.display = 'none';
            searchInput.value = '';
        }
        
        // 添加好友功能已整合到sendFriendRequest函数中
        
        // 查看用户资料
        async function viewUserProfile(userId) {
            const token = getAuthToken();
            if (!token) {
                alert('请先登录');
                return;
            }
            
            try {
                const response = await fetch(`/api/user/public-profile/by-user-id/${userId}`, {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    }
                });
                
                if (response.status === 401) {
                    alert('登录已过期，请重新登录');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return;
                }
                
                if (!response.ok) {
                    throw new Error('获取用户资料失败');
                }
                
                const data = await response.json();
                if (data.success) {
                    showUserProfileModal(data.data);
                } else {
                    throw new Error(data.message || '获取用户资料失败');
                }
            } catch (error) {
                console.error('获取用户资料失败:', error);
                alert('获取用户资料失败: ' + error.message);
            }
        }
        
        // 显示用户资料模态框
        function showUserProfileModal(profile) {
            const modal = document.getElementById('userProfileModal');
            const profileAvatar = document.getElementById('profileModalAvatar');
            const profileNickname = document.getElementById('profileModalNickname');
            const profileUserId = document.getElementById('profileModalUserId');
            const profileSignature = document.getElementById('profileModalSignature');
            const profileEmail = document.getElementById('profileModalEmail');
            const profilePhone = document.getElementById('profileModalPhone');
            const profileGender = document.getElementById('profileModalGender');
            const profileBirthday = document.getElementById('profileModalBirthday');
            const profileLocation = document.getElementById('profileModalLocation');
            const profileOccupation = document.getElementById('profileModalOccupation');
            
            if (!modal) {
                console.error('找不到userProfileModal元素!');
                return;
            }
            
            console.log('显示用户资料模态框，用户数据:', profile);
            
            // 设置头像
            if (profile.avatarUrl) {
                profileAvatar.innerHTML = `<img src="${profile.avatarUrl}" alt="头像" style="width: 100%; height: 100%; border-radius: inherit; object-fit: cover;">`;
            } else {
                const avatarText = (profile.nickname || profile.email || 'U').charAt(0).toUpperCase();
                profileAvatar.innerHTML = avatarText;
            }
            
            // 设置基本信息
            profileNickname.textContent = profile.nickname || '未设置';
            profileUserId.textContent = profile.userIdString || '未设置';
            profileSignature.textContent = profile.signature || '这个人很懒，什么都没留下';
            profileEmail.textContent = profile.email || '未公开';
            profilePhone.textContent = profile.phoneNumber || '未公开';
            profileGender.textContent = getGenderText(profile.gender);
            profileBirthday.textContent = profile.birthday || '未公开';
            profileLocation.textContent = profile.location || '未公开';
            profileOccupation.textContent = profile.occupation || '未公开';
            
            // 使用style.display来确保模态框显示
            modal.style.display = 'flex';
            modal.classList.add('show');
            
            console.log('模态框显示状态:', modal.style.display, modal.classList.contains('show'));
        }
        
        // 获取性别文本
        function getGenderText(gender) {
            // 处理后端返回的中文性别值
            if (!gender || gender.trim() === '') {
                return '未公开';
            }
            
            switch (gender.trim()) {
                case '男':
                    return '男';
                case '女':
                    return '女';
                case '保密':
                    return '保密';
                // 兼容英文值（如果有的话）
                case 'MALE':
                    return '男';
                case 'FEMALE':
                    return '女';
                case 'OTHER':
                    return '其他';
                default:
                    return '未公开';
            }
        }
        
        // 关闭用户资料模态框
        function closeUserProfileModal() {
            const modal = document.getElementById('userProfileModal');
            modal.style.display = 'none';
            modal.classList.remove('show');
        }
    
        // 监听搜索输入框的回车事件
        document.addEventListener('DOMContentLoaded', function() {
            const searchInput = document.getElementById('contactSearchInput');
            if (searchInput) {
                searchInput.addEventListener('keypress', function(e) {
                    if (e.key === 'Enter') {
                        searchUsers();
                    }
                });
            }
            
            // 添加用户资料模态框的事件监听
            const userProfileModal = document.getElementById('userProfileModal');
            if (userProfileModal) {
                // 点击模态框背景关闭
                userProfileModal.addEventListener('click', function(e) {
                    if (e.target === userProfileModal) {
                        closeUserProfileModal();
                    }
                });
            }
            
            // ESC键关闭模态框
            document.addEventListener('keydown', function(e) {
                if (e.key === 'Escape') {
                    const userProfileModal = document.getElementById('userProfileModal');
                    if (userProfileModal && userProfileModal.style.display === 'flex') {
                        closeUserProfileModal();
                    }
                    
                    const removeTagModal = document.getElementById('removeTagModal');
                    if (removeTagModal && removeTagModal.style.display === 'flex') {
                        closeRemoveTagModal();
                    }
                }
            });
            
            // 添加移除标签模态框的事件监听
            const removeTagModal = document.getElementById('removeTagModal');
            if (removeTagModal) {
                // 点击模态框背景关闭
                removeTagModal.addEventListener('click', function(e) {
                    if (e.target === removeTagModal) {
                        closeRemoveTagModal();
                    }
                });
            }
        });
    
        function publishMoment() {
            alert('发布动态功能正在开发中...');
        }
    
        function likeMoment(momentId) {
            alert(`点赞动态 ${momentId}，点赞功能正在开发中...`);
        }
    
        function commentMoment(momentId) {
            alert(`评论动态 ${momentId}，评论功能正在开发中...`);
        }
    
        async function showQRCode() {
            const token = getAuthToken();
            if (!token) {
                alert('请先登录');
                return;
            }
    
            try {
                const response = await fetch('/api/user/qrcode', {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    }
                });
    
                // 处理401未授权错误
                if (response.status === 401) {
                    alert('登录已过期，请重新登录');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return;
                }
    
                if (!response.ok) {
                    throw new Error('生成二维码失败');
                }
    
                const data = await response.json();
                if (data.success) {
                    showQRCodeModal(data.data);
                } else {
                    throw new Error(data.message || '生成二维码失败');
                }
            } catch (error) {
                console.error('生成二维码失败:', error);
                alert('生成二维码失败: ' + error.message);
            }
        }
    
        function showQRCodeModal(qrCodeData) {
            const modal = document.getElementById('qrCodeModal');
            const qrImage = document.getElementById('qrCodeImage');
            const userAvatar = document.getElementById('qrUserAvatar');
            const userName = document.getElementById('qrUserName');
            const userInfo = document.getElementById('qrUserInfo');
            
            // 设置二维码图片 - 后端返回的字段是qrCodeBase64，已包含data:image/png;base64,前缀
            qrImage.src = qrCodeData.qrCodeBase64;
            
            // 设置用户头像
            if (qrCodeData.userAvatarUrl) {
                userAvatar.src = qrCodeData.userAvatarUrl;
            } else {
                // 使用默认头像
                userAvatar.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgdmlld0JveD0iMCAwIDEwMCAxMDAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iNTAiIGN5PSI1MCIgcj0iNTAiIGZpbGw9IiNmMGYwZjAiLz48Y2lyY2xlIGN4PSI1MCIgY3k9IjM1IiByPSIxNSIgZmlsbD0iIzk5OTk5OSIvPjxwYXRoIGQ9Ik0yMCA3NWMwLTE2LjU2OSAxMy40MzEtMzAgMzAtMzBzMzAgMTMuNDMxIDMwIDMwIiBmaWxsPSIjOTk5OTk5Ii8+PC9zdmc+';
            }
            
            // 设置用户信息 - 使用后端返回的字段名
            userName.textContent = qrCodeData.userNickname || '用户';
            userInfo.textContent = qrCodeData.userIdString ? `ID: ${qrCodeData.userIdString}` : '暂无个人ID';
            
            modal.style.display = 'flex';
        }
    
        function closeQRCodeModal() {
            const modal = document.getElementById('qrCodeModal');
            modal.style.display = 'none';
        }
    
        // 移除标签模态框相关函数
        let pendingRemoveContactId = null;
        let pendingRemoveContactName = null;
        
        function showRemoveTagModal(contactId, contactName, tagName) {
            const modal = document.getElementById('removeTagModal');
            const removeTagNameSpan = document.getElementById('removeTagName');
            const removeContactNameSpan = document.getElementById('removeContactName');
            
            if (!modal) {
                console.error('找不到removeTagModal元素!');
                return;
            }
            
            // 设置显示的信息
            removeTagNameSpan.textContent = tagName || '标签';
            removeContactNameSpan.textContent = contactName || '好友';
            
            // 保存待处理的数据
            pendingRemoveContactId = contactId;
            pendingRemoveContactName = contactName;
            
            // 显示模态框
            modal.style.display = 'flex';
        }
        
        function closeRemoveTagModal() {
            const modal = document.getElementById('removeTagModal');
            if (modal) {
                modal.style.display = 'none';
            }
            
            // 清空待处理的数据
            pendingRemoveContactId = null;
            pendingRemoveContactName = null;
        }
        
        function confirmRemoveTagAction() {
            if (pendingRemoveContactId) {
                // 调用common.js中的移除函数
                if (typeof removeContactFromTagAction === 'function') {
                    removeContactFromTagAction(pendingRemoveContactId);
                }
                closeRemoveTagModal();
            }
        }
    
        function downloadQRCode() {
            const qrImage = document.getElementById('qrCodeImage');
            const link = document.createElement('a');
            link.download = '我的二维码名片.png';
            link.href = qrImage.src;
            link.click();
        }
    
        function shareQRCode() {
            if (navigator.share) {
                const qrImage = document.getElementById('qrCodeImage');
                // 将base64转换为blob
                fetch(qrImage.src)
                    .then(res => res.blob())
                    .then(blob => {
                        const file = new File([blob], '我的二维码名片.png', { type: 'image/png' });
                        navigator.share({
                            title: '我的二维码名片',
                            text: '扫描二维码添加我为好友',
                            files: [file]
                        });
                    })
                    .catch(err => {
                        console.error('分享失败:', err);
                        alert('分享功能暂不支持，请使用下载功能');
                    });
            } else {
                alert('您的浏览器不支持分享功能，请使用下载功能');
            }
        }
    
        function openProfile() {
            document.getElementById('profileModal').style.display = 'flex';
            // 设置生日输入框的最大日期为今天
            const today = new Date().toISOString().split('T')[0];
            document.getElementById('profileBirthday').setAttribute('max', today);
            loadUserProfile();
        }
    
        function closeProfileModal() {
            const modal = document.getElementById('profileModal');
            modal.style.display = 'none';
        }
    
        function loadUserProfile() {
            const loadingDiv = document.getElementById('profileLoading');
            const formDiv = document.getElementById('profileEditForm');
            
            loadingDiv.style.display = 'block';
            if (formDiv) formDiv.style.display = 'none';
            
            const token = getAuthToken();
            if (!token) {
                showProfileAlert('请先登录', 'error');
                return;
            }
            
            fetch('/api/user/profile', {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + token,
                    'Content-Type': 'application/json'
                }
            })
            .then(response => {
                // 处理401未授权错误
                if (response.status === 401) {
                    showProfileAlert('登录已过期，请重新登录', 'error');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return Promise.reject(new Error('登录已过期'));
                }
                
                if (!response.ok) {
                    throw new Error('获取用户信息失败');
                }
                return response.json();
            })
            .then(data => {
                console.log('API返回的数据:', data);
                // 处理API响应的嵌套结构
                const profileData = data.data || data;
                populateProfileForm(profileData);
                loadingDiv.style.display = 'none';
                if (formDiv) formDiv.style.display = 'block';
            })
            .catch(error => {
                console.error('Error loading profile:', error);
                showProfileAlert('加载用户信息失败: ' + error.message, 'error');
                loadingDiv.style.display = 'none';
                if (formDiv) formDiv.style.display = 'block';
            });
        }
    
        function populateProfileForm(data) {
            document.getElementById('profileEmail').value = data.email || '';
            document.getElementById('profileNickname').value = data.nickname || '';
            document.getElementById('profileUserId').value = data.userIdString || '';
            document.getElementById('profileSignature').value = data.signature || '';
            document.getElementById('profilePhoneNumber').value = data.phoneNumber || '';
            document.getElementById('profileGender').value = data.gender || '';
            document.getElementById('profileBirthday').value = data.birthday || '';
            document.getElementById('profileLocation').value = data.location || '';
            document.getElementById('profileOccupation').value = data.occupation || '';
            
            const avatarImg = document.getElementById('profileAvatarImg');
            if (data.avatarUrl) {
                avatarImg.src = data.avatarUrl;
            } else {
                avatarImg.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgdmlld0JveD0iMCAwIDEwMCAxMDAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iNTAiIGN5PSI1MCIgcj0iNTAiIGZpbGw9IiNmMGYwZjAiLz48Y2lyY2xlIGN4PSI1MCIgY3k9IjM1IiByPSIxNSIgZmlsbD0iIzk5OTk5OSIvPjxwYXRoIGQ9Ik0yMCA3NWMwLTE2LjU2OSAxMy40MzEtMzAgMzAtMzBzMzAgMTMuNDMxIDMwIDMwIiBmaWxsPSIjOTk5OTk5Ii8+PC9zdmc+';
            }
            
            // 显示个性状态
            const currentStatusDisplay = document.getElementById('currentStatusDisplay');
            const currentStatusEmoji = document.getElementById('currentStatusEmoji');
            const currentStatusText = document.getElementById('currentStatusText');
            const statusEmoji = document.getElementById('statusEmoji');
            const statusText = document.getElementById('statusText');
            
            if (data.status && data.status.text) {
                currentStatusEmoji.textContent = data.status.emoji || '😊';
                currentStatusText.textContent = data.status.text;
                statusEmoji.value = data.status.emoji || '';
                statusText.value = data.status.text;
            } else {
                currentStatusEmoji.textContent = '😊';
                currentStatusText.textContent = '暂无状态';
                statusEmoji.value = '';
                statusText.value = '';
            }
            
            // 确保状态显示区域始终可见
            currentStatusDisplay.style.display = 'flex';
            
            // 确保状态表单初始为隐藏状态
            const statusForm = document.getElementById('statusForm');
            if (statusForm) {
                statusForm.style.display = 'none';
            }
        }
    
        async function saveProfile() {
            const token = getAuthToken();
            if (!token) {
                showProfileAlert('请先登录', 'error');
                return;
            }
            
            const profileData = {
                nickname: document.getElementById('profileNickname').value.trim(),
                signature: document.getElementById('profileSignature').value.trim(),
                phoneNumber: document.getElementById('profilePhoneNumber').value.trim(),
                gender: document.getElementById('profileGender').value.trim(),
                birthday: document.getElementById('profileBirthday').value || null,
                location: document.getElementById('profileLocation').value.trim(),
                occupation: document.getElementById('profileOccupation').value.trim()
            };
            
            // 清理空字符串，将其设为null
            Object.keys(profileData).forEach(key => {
                if (profileData[key] === '') {
                    profileData[key] = null;
                }
            });
            
            const userIdString = document.getElementById('profileUserId').value.trim();
            
            if (!profileData.nickname) {
                showProfileAlert('昵称不能为空', 'error');
                return;
            }
            
            // 验证手机号格式
            if (profileData.phoneNumber && !/^1[3-9]\d{9}$/.test(profileData.phoneNumber)) {
                showProfileAlert('手机号格式不正确', 'error');
                return;
            }
            
            try {
                // 保存基本资料
                const profileResponse = await fetch('/api/user/profile', {
                    method: 'PUT',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(profileData)
                });
                
                if (!profileResponse.ok) {
                    throw new Error('保存基本资料失败');
                }
                
                // 获取当前用户信息，检查个人ID是否有变化
                const currentProfileResponse = await fetch('/api/user/profile', {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    }
                });
                
                if (currentProfileResponse.ok) {
                    const currentData = await currentProfileResponse.json();
                    const currentUserIdString = (currentData.data || currentData).userIdString || '';
                    
                    // 只有当个人ID有变化时才保存
                    if (userIdString !== currentUserIdString) {
                        if (userIdString) {
                            // 保存新的个人ID
                            const userIdResponse = await fetch('/api/user/personal-id', {
                                method: 'POST',
                                headers: {
                                    'Authorization': 'Bearer ' + token,
                                    'Content-Type': 'application/json'
                                },
                                body: JSON.stringify({ userIdString: userIdString })
                            });
                            
                            if (!userIdResponse.ok) {
                                const errorData = await userIdResponse.json();
                                // 正确提取嵌套在响应结构中的错误信息
                                const errorMessage = errorData.message || errorData.data?.message || '保存个人ID失败';
                                throw new Error(errorMessage);
                            }
                        }
                    }
                }
                
                // 保存状态信息（如果有修改）
                const statusEmoji = document.getElementById('statusEmoji').value.trim();
                const statusText = document.getElementById('statusText').value.trim();
                
                if (statusEmoji && statusText) {
                    // 获取选中的有效期
                    const selectedDuration = document.querySelector('.duration-btn.selected');
                    let expiresAt = null;
                    
                    if (selectedDuration) {
                        const duration = selectedDuration.dataset.duration;
                        if (duration !== 'manual') {
                            const now = new Date();
                            switch (duration) {
                                case '30s':
                                    expiresAt = new Date(now.getTime() + 30 * 1000).toISOString();
                                    break;
                                case '1h':
                                    expiresAt = new Date(now.getTime() + 60 * 60 * 1000).toISOString();
                                    break;
                                case '4h':
                                    expiresAt = new Date(now.getTime() + 4 * 60 * 60 * 1000).toISOString();
                                    break;
                                case 'today':
                                    const endOfDay = new Date(now);
                                    endOfDay.setHours(23, 59, 59, 999);
                                    expiresAt = endOfDay.toISOString();
                                    break;
                            }
                        }
                    }
                    
                    const statusRequestBody = {
                        emoji: statusEmoji,
                        statusText: statusText
                    };
                    
                    if (expiresAt) {
                        statusRequestBody.expiresAt = expiresAt;
                    }
                    
                    // 保存状态
                    const statusResponse = await fetch('/api/user/status', {
                        method: 'POST',
                        headers: {
                            'Authorization': 'Bearer ' + token,
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(statusRequestBody)
                    });
                    
                    // 处理401未授权错误
                    if (statusResponse.status === 401) {
                        showProfileAlert('登录已过期，请重新登录', 'error');
                        clearLoginInfo();
                        setTimeout(() => {
                            window.location.href = '/login.html';
                        }, 2000);
                        return;
                    }
                    
                    if (!statusResponse.ok) {
                        console.warn('保存状态失败，但基本资料已保存');
                    } else {
                        // 更新当前状态显示
                        document.getElementById('currentStatusEmoji').textContent = statusEmoji;
                        document.getElementById('currentStatusText').textContent = statusText;
                        document.getElementById('currentStatusDisplay').style.display = 'flex';
                        
                        // 更新"我"界面中的状态显示
                        const meStatus = document.getElementById('meStatus');
                        if (meStatus) meStatus.textContent = `${statusEmoji} ${statusText}`;
                    }
                }
                
                showProfileAlert('保存成功', 'success');
                // 更新页面上的用户信息
                updateUserInfoDisplay();
                // 关闭模态框
                setTimeout(() => {
                    closeProfileModal();
                }, 1000); // 延迟1秒关闭，让用户看到成功提示
            } catch (error) {
                console.error('Error saving profile:', error);
                showProfileAlert('保存失败: ' + error.message, 'error');
            }
        }
    
        function uploadAvatar() {
            const input = document.createElement('input');
            input.type = 'file';
            input.accept = 'image/*';
            input.onchange = function(event) {
                const file = event.target.files[0];
                if (!file) return;
                
                if (file.size > 5 * 1024 * 1024) {
                    showProfileAlert('图片大小不能超过5MB', 'error');
                    return;
                }
                
                const formData = new FormData();
                formData.append('file', file);
                
                const token = getAuthToken();
                if (!token) {
                    showProfileAlert('请先登录', 'error');
                    return;
                }
                
                fetch('/api/user/profile/avatar', {
                    method: 'POST',
                    headers: {
                        'Authorization': 'Bearer ' + token
                    },
                    body: formData
                })
                .then(response => {
                    // 处理401未授权错误
                    if (response.status === 401) {
                        showProfileAlert('登录已过期，请重新登录', 'error');
                        clearLoginInfo();
                        setTimeout(() => {
                            window.location.href = '/login.html';
                        }, 2000);
                        return Promise.reject(new Error('登录已过期'));
                    }
                    
                    if (!response.ok) {
                        throw new Error('上传失败');
                    }
                    return response.json();
                })
                .then(data => {
                    document.getElementById('profileAvatarImg').src = data.data.avatarUrl;
                    showProfileAlert('头像上传成功', 'success');
                    // 更新页面上的头像
                    updateUserInfoDisplay();
                })
                .catch(error => {
                    console.error('Error uploading avatar:', error);
                    showProfileAlert('头像上传失败: ' + error.message, 'error');
                });
            };
            input.click();
        }
    
        function uploadProfileAvatar() {
            const fileInput = document.getElementById('profileAvatarInput');
            const file = fileInput.files[0];
            
            if (!file) {
                showProfileAlert('请选择头像文件', 'error');
                return;
            }
            
            // 验证文件类型
            const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
            if (!allowedTypes.includes(file.type)) {
                showProfileAlert('请选择有效的图片格式（JPG、PNG、GIF、WebP）', 'error');
                return;
            }
            
            // 验证文件大小（5MB限制）
            if (file.size > 5 * 1024 * 1024) {
                showProfileAlert('图片大小不能超过5MB', 'error');
                return;
            }
            
            const formData = new FormData();
            formData.append('file', file);
            
            const token = getAuthToken();
            if (!token) {
                showProfileAlert('请先登录', 'error');
                return;
            }
            
            // 显示上传进度
            showProfileAlert('正在上传头像...', 'info');
            
            fetch('/api/user/profile/avatar', {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + token
                },
                body: formData
            })
            .then(response => {
                // 处理401未授权错误
                if (response.status === 401) {
                    showProfileAlert('登录已过期，请重新登录', 'error');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return Promise.reject(new Error('登录已过期'));
                }
                
                if (!response.ok) {
                    return response.json().then(errorData => {
                        throw new Error(errorData.message || '上传失败');
                    });
                }
                return response.json();
            })
            .then(data => {
                // 更新头像显示
                const avatarImg = document.getElementById('profileAvatarImg');
                if (avatarImg && data.data && data.data.avatarUrl) {
                    avatarImg.src = data.data.avatarUrl;
                }
                
                showProfileAlert('头像上传成功', 'success');
                
                // 更新页面上其他位置的头像显示
                updateUserInfoDisplay();
                
                // 清空文件输入框
                fileInput.value = '';
            })
            .catch(error => {
                console.error('Error uploading avatar:', error);
                showProfileAlert('头像上传失败: ' + error.message, 'error');
                // 清空文件输入框
                fileInput.value = '';
            });
        }
    
        function saveStatus() {
            let emoji, text;
            
            // 检查当前是预设状态还是自定义状态
            const presetSection = document.getElementById('presetStatusSection');
            const customSection = document.getElementById('customStatusSection');
            
            if (presetSection.style.display !== 'none' && window.selectedPresetStatus) {
                // 使用预设状态
                emoji = window.selectedPresetStatus.emoji;
                text = window.selectedPresetStatus.text;
            } else if (customSection.style.display !== 'none') {
                // 使用自定义状态
                emoji = document.getElementById('statusEmoji').value.trim();
                text = document.getElementById('statusText').value.trim();
            } else {
                showProfileAlert('请选择状态类型并设置状态', 'error');
                return;
            }
            
            if (!emoji || !text) {
                showProfileAlert('请填写完整的状态信息', 'error');
                return;
            }
            
            const token = getAuthToken();
            if (!token) {
                showProfileAlert('请先登录', 'error');
                return;
            }
            
            // 获取选中的有效期
            const selectedDuration = document.querySelector('.duration-btn.selected');
            let expiresAt = null;
            
            if (selectedDuration) {
                const duration = selectedDuration.dataset.duration;
                if (duration !== 'manual') {
                    const now = new Date();
                    switch (duration) {
                        case '30s':
                            expiresAt = new Date(now.getTime() + 30 * 1000).toISOString();
                            break;
                        case '1h':
                            expiresAt = new Date(now.getTime() + 60 * 60 * 1000).toISOString();
                            break;
                        case '4h':
                            expiresAt = new Date(now.getTime() + 4 * 60 * 60 * 1000).toISOString();
                            break;
                        case 'today':
                            const endOfDay = new Date(now);
                            endOfDay.setHours(23, 59, 59, 999);
                            expiresAt = endOfDay.toISOString();
                            break;
                    }
                }
            }
            
            const requestBody = {
                emoji: emoji,
                statusText: text
            };
            
            if (expiresAt) {
                requestBody.expiresAt = expiresAt;
            }
            
            fetch('/api/user/status', {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + token,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestBody)
            })
            .then(response => {
                // 处理401未授权错误
                if (response.status === 401) {
                    showProfileAlert('登录已过期，请重新登录', 'error');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return Promise.reject(new Error('登录已过期'));
                }
                
                if (!response.ok) {
                    throw new Error('保存状态失败');
                }
                return response.json();
            })
            .then(data => {
                document.getElementById('currentStatusEmoji').textContent = emoji;
                document.getElementById('currentStatusText').textContent = text;
                document.getElementById('currentStatusDisplay').style.display = 'flex';
                
                // 更新"我"界面中的状态显示
                const meStatus = document.getElementById('meStatus');
                if (meStatus) meStatus.textContent = `${emoji} ${text}`;
                
                showProfileAlert('状态保存成功', 'success');
                
                // 更新本地存储中的用户信息
                updateLocalUserStatus(emoji, text, expiresAt);
                
                // 隐藏状态表单
                document.getElementById('statusForm').style.display = 'none';
            })
            .catch(error => {
                console.error('Error saving status:', error);
                showProfileAlert('保存状态失败: ' + error.message, 'error');
            });
        }
    
        function clearStatus() {
            const token = getAuthToken();
            if (!token) {
                showProfileAlert('请先登录', 'error');
                return;
            }
            
            fetch('/api/user/status', {
                method: 'DELETE',
                headers: {
                    'Authorization': 'Bearer ' + token
                }
            })
            .then(response => {
                // 处理401未授权错误
                if (response.status === 401) {
                    showProfileAlert('登录已过期，请重新登录', 'error');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return Promise.reject(new Error('登录已过期'));
                }
                
                if (!response.ok) {
                    throw new Error('清除状态失败');
                }
                document.getElementById('currentStatusDisplay').style.display = 'none';
                document.getElementById('statusEmoji').value = '';
                document.getElementById('statusText').value = '';
                
                // 更新"我"界面中的状态显示为默认状态
                const meStatus = document.getElementById('meStatus');
                if (meStatus) meStatus.textContent = '😊 暂无状态';
                
                // 清除本地存储中的用户状态
                clearLocalUserStatus();
                
                showProfileAlert('状态已清除', 'success');
            })
            .catch(error => {
                console.error('Error clearing status:', error);
                showProfileAlert('清除状态失败: ' + error.message, 'error');
            });
        }
    
        function showProfileAlert(message, type) {
            const alertDiv = document.getElementById('profileAlert');
            alertDiv.textContent = message;
            alertDiv.className = 'profile-alert ' + type;
            alertDiv.style.display = 'block';
            
            setTimeout(() => {
                alertDiv.style.display = 'none';
            }, 3000);
        }
    
        function updateUserInfoDisplay() {
            // 从服务器重新获取最新用户信息并更新页面显示
            const token = getAuthToken();
            if (!token) {
                console.error('未找到认证令牌');
                return Promise.reject(new Error('未找到认证令牌'));
            }
            
            return fetch('/api/user/profile', {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + token,
                    'Content-Type': 'application/json'
                }
            })
            .then(response => {
                // 处理401未授权错误
                if (response.status === 401) {
                    console.error('登录已过期');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 1000);
                    return Promise.reject(new Error('登录已过期'));
                }
                
                if (!response.ok) {
                    throw new Error('获取用户信息失败');
                }
                return response.json();
            })
            .then(data => {
                console.log('=== API响应原始数据 ===', data);
                // 处理API响应的嵌套结构
                const userData = data.data || data;
                console.log('=== 解析后的用户数据 ===', userData);
                
                // API已经返回解析好的status对象，无需再次解析
                console.log('=== 从API获取到的用户状态 ===', userData.status);
                console.log('=== 状态类型 ===', typeof userData.status);
                if (userData.status) {
                    console.log('=== 状态对象的键 ===', Object.keys(userData.status));
                    console.log('=== 状态文本 ===', userData.status.text);
                    console.log('=== 状态表情 ===', userData.status.emoji);
                } else {
                    console.log('用户没有个性化状态');
                }
                
                // 更新本地存储的用户信息
                const storageKey = localStorage.getItem('accessToken') ? 'userInfo' : 'userInfo';
                if (localStorage.getItem('accessToken')) {
                    localStorage.setItem('userInfo', JSON.stringify(userData));
                } else {
                    sessionStorage.setItem('userInfo', JSON.stringify(userData));
                }
                
                // 更新页面显示
                displayUserInfo(userData);
            })
            .catch(error => {
                console.error('更新用户信息失败:', error);
            });
        }
    
        function updateLocalUserStatus(emoji, text, expiresAt) {
            // 获取当前存储的用户信息
            let userInfoStr = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo');
            if (userInfoStr) {
                try {
                    const userData = JSON.parse(userInfoStr);
                    
                    // 构建新的状态对象
                    const newStatus = {
                        emoji: emoji,
                        text: text,
                        updatedAt: new Date().toISOString()
                    };
                    
                    if (expiresAt) {
                        newStatus.expiresAt = expiresAt;
                    }
                    
                    // 更新用户数据中的状态信息
                    userData.status = newStatus;
                    userData.personalizedStatus = JSON.stringify(newStatus);
                    
                    // 保存回本地存储
                    if (localStorage.getItem('accessToken')) {
                        localStorage.setItem('userInfo', JSON.stringify(userData));
                    } else {
                        sessionStorage.setItem('userInfo', JSON.stringify(userData));
                    }
                    
                    console.log('本地用户状态已更新:', newStatus);
                } catch (e) {
                    console.error('更新本地用户状态失败:', e);
                }
            }
        }
    
        function clearLocalUserStatus() {
            // 获取当前存储的用户信息
            let userInfoStr = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo');
            if (userInfoStr) {
                try {
                    const userData = JSON.parse(userInfoStr);
                    
                    // 清除状态信息
                    userData.status = null;
                    userData.personalizedStatus = null;
                    
                    // 保存回本地存储
                    if (localStorage.getItem('accessToken')) {
                        localStorage.setItem('userInfo', JSON.stringify(userData));
                    } else {
                        sessionStorage.setItem('userInfo', JSON.stringify(userData));
                    }
                    
                    console.log('本地用户状态已清除');
                } catch (e) {
                    console.error('清除本地用户状态失败:', e);
                }
            }
        }
    
        function getAuthToken() {
            return localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
        }
    
        function toggleStatusForm() {
            const statusDisplay = document.getElementById('currentStatusDisplay');
            const statusForm = document.getElementById('statusForm');
            
            if (statusForm.style.display === 'none' || statusForm.style.display === '') {
                statusForm.style.display = 'block';
                statusDisplay.style.display = 'none';
            } else {
                statusForm.style.display = 'none';
                statusDisplay.style.display = 'flex';
            }
        }
        
        // 预设状态选择功能
        // 状态类型切换功能
        function switchStatusType(type) {
            const presetSection = document.getElementById('presetStatusSection');
            const customSection = document.getElementById('customStatusSection');
            const presetBtn = document.querySelector('[data-type="preset"]');
            const customBtn = document.querySelector('[data-type="custom"]');
            
            // 更新按钮状态
            document.querySelectorAll('.status-type-btn').forEach(btn => {
                btn.classList.remove('active');
            });
            
            if (type === 'preset') {
                presetBtn.classList.add('active');
                presetSection.style.display = 'block';
                customSection.style.display = 'none';
            } else {
                customBtn.classList.add('active');
                presetSection.style.display = 'none';
                customSection.style.display = 'block';
            }
        }
        
        function selectPresetStatus(emoji, text) {
            // 不再修改自定义状态输入框，预设状态独立工作
            
            // 更新预设状态的选中状态
            document.querySelectorAll('.preset-status-item').forEach(item => {
                item.classList.remove('selected');
            });
            
            // 找到对应的预设状态项并标记为选中
            const presetItems = document.querySelectorAll('.preset-status-item');
            presetItems.forEach(item => {
                const itemEmoji = item.querySelector('.preset-emoji').textContent;
                const itemText = item.querySelector('.preset-text').textContent;
                if (itemEmoji === emoji && itemText === text) {
                    item.classList.add('selected');
                }
            });
            
            // 存储选中的预设状态
            window.selectedPresetStatus = { emoji, text };
        }
        
        // 有效期选择功能
        function selectDuration(duration) {
            document.querySelectorAll('.duration-btn').forEach(btn => {
                btn.classList.remove('selected');
            });
            
            const selectedBtn = document.querySelector(`[data-duration="${duration}"]`);
            if (selectedBtn) {
                selectedBtn.classList.add('selected');
            }
        }
        
        // 初始化状态设置功能
        function initializeStatusSettings() {
            // 为状态类型切换按钮添加点击事件
            document.querySelectorAll('.status-type-btn').forEach(btn => {
                btn.addEventListener('click', function() {
                    const type = this.dataset.type;
                    switchStatusType(type);
                });
            });
            
            // 为预设状态添加点击事件
            document.querySelectorAll('.preset-status-item').forEach(item => {
                item.addEventListener('click', function() {
                    const emoji = this.querySelector('.preset-emoji').textContent;
                    const text = this.querySelector('.preset-text').textContent;
                    selectPresetStatus(emoji, text);
                });
            });
            
            // 为有效期按钮添加点击事件
            document.querySelectorAll('.duration-btn').forEach(btn => {
                btn.addEventListener('click', function() {
                    const duration = this.dataset.duration;
                    selectDuration(duration);
                });
            });
            
            // 默认选择"手动清除"选项
            selectDuration('manual');
            
            // 默认显示预设状态
            switchStatusType('preset');
        }
    
        function openFileManager() {
            // 隐藏所有标签页内容
            document.querySelectorAll('.tab-content').forEach(tab => {
                tab.classList.remove('active');
            });
            
            // 显示文件管理页面
            const fileManagerTab = document.getElementById('fileManagerTab');
            if (fileManagerTab) {
                fileManagerTab.classList.add('active');
                fileManagerTab.style.display = 'block';
            }
            
            // 隐藏底部标签栏
            const tabBar = document.getElementById('tabBar');
            if (tabBar) {
                tabBar.style.display = 'none';
            }
            
            // 更新页面标题
            const pageTitle = document.getElementById('pageTitle');
            if (pageTitle) {
                pageTitle.textContent = '文件管理';
            }
            
            // 隐藏右侧按钮
            const rightBtn = document.getElementById('rightBtn');
            if (rightBtn) {
                rightBtn.style.display = 'none';
            }
            
            // 检查是否已经初始化过文件管理功能
            const fileList = document.getElementById('fileList');
            if (!fileList || fileList.children.length === 0 || fileList.innerHTML.includes('暂无文件')) {
                // 如果文件列表为空或显示"暂无文件"，则重新加载
                initFileManager();
            }
            // 如果已经有文件列表，则不重新加载，保持现有状态
        }
    
        function openSettings() {
            alert('设置功能正在开发中...');
        }
    
        function openThemes() {
            alert('主题与外观功能正在开发中...');
        }
    
        // 账户与安全相关函数
        function openAccountSecurity() {
            // 隐藏所有标签页内容
            document.querySelectorAll('.tab-content').forEach(tab => {
                tab.classList.remove('active');
            });
            
            // 显示账户与安全页面
            const accountSecurityTab = document.getElementById('accountSecurityTab');
            if (accountSecurityTab) {
                accountSecurityTab.classList.add('active');
                accountSecurityTab.style.display = 'block';
            }
            
            // 隐藏底部标签栏
            const tabBar = document.getElementById('tabBar');
            if (tabBar) {
                tabBar.style.display = 'none';
            }
            
            // 更新页面标题
            const pageTitle = document.getElementById('pageTitle');
            if (pageTitle) {
                pageTitle.textContent = '账户与安全';
            }
            
            // 隐藏右侧按钮
            const rightBtn = document.getElementById('rightBtn');
            if (rightBtn) {
                rightBtn.style.display = 'none';
            }
        }
    
        function backToMe() {
            // 隐藏账户与安全页面
            const accountSecurityTab = document.getElementById('accountSecurityTab');
            if (accountSecurityTab) {
                accountSecurityTab.classList.remove('active');
                accountSecurityTab.style.display = 'none';
            }
            
            // 隐藏文件管理页面
            const fileManagerTab = document.getElementById('fileManagerTab');
            if (fileManagerTab) {
                fileManagerTab.classList.remove('active');
                fileManagerTab.style.display = 'none';
            }
            
            // 显示我的页面
            const meTab = document.getElementById('meTab');
            if (meTab) {
                meTab.classList.add('active');
                meTab.style.display = 'block';
            }
            
            // 显示底部标签栏
            const tabBar = document.getElementById('tabBar');
            if (tabBar) {
                tabBar.style.display = 'flex';
            }
            
            // 更新页面标题
            const pageTitle = document.getElementById('pageTitle');
            if (pageTitle) {
                pageTitle.textContent = '我';
            }
            
            // 显示右侧按钮
            const rightBtn = document.getElementById('rightBtn');
            if (rightBtn) {
                rightBtn.style.display = 'block';
            }
        }
    
        function openTwoFactorAuth() {
            alert('双重认证功能正在开发中...');
        }
    
        function openLoginHistory() {
            document.getElementById('loginDeviceModal').style.display = 'flex';
            loadDeviceList();
        }
    
        function closeLoginDeviceModal() {
            document.getElementById('loginDeviceModal').style.display = 'none';
        }
    
        async function loadDeviceList(showLoading = true) {
            const loading = document.getElementById('deviceLoading');
            const alert = document.getElementById('deviceAlert');
            const container = document.getElementById('deviceListContainer');
            const totalCount = document.getElementById('totalDeviceCount');
            const activeCount = document.getElementById('activeDeviceCount');
            
            try {
                if (showLoading) {
                    loading.style.display = 'block';
                }
                // 只有在没有显示成功提示时才隐藏alert
                if (!alert.classList.contains('success') || alert.style.display === 'none') {
                    alert.style.display = 'none';
                }
                
                const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
                if (!token) {
                    throw new Error('请先登录');
                }
                
                const response = await fetch('/api/user/devices', {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    }
                });
                
                // 处理401未授权错误
                if (response.status === 401) {
                    showDeviceAlert('error', '登录已过期，请重新登录');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    throw new Error('登录已过期');
                }
                
                const result = await response.json();
                
                if (response.ok && result.code === 200) {
                    const data = result.data;
                    totalCount.textContent = data.totalCount || 0;
                    activeCount.textContent = data.activeCount || 0;
                    
                    renderDeviceList(data.devices || []);
                } else {
                    throw new Error(result.message || '获取设备列表失败');
                }
            } catch (error) {
                // 如果是从refreshDeviceList调用的，不显示错误提示，让调用者处理
                if (error.message !== '登录已过期') {
                    showDeviceAlert('error', error.message);
                }
                container.innerHTML = '<div class="empty-device-list"><div class="empty-icon">📱</div><p>加载设备列表失败</p></div>';
                throw error; // 重新抛出错误供refreshDeviceList处理
            } finally {
                if (showLoading) {
                    loading.style.display = 'none';
                }
            }
        }
    
        function renderDeviceList(devices) {
            const container = document.getElementById('deviceListContainer');
            
            if (!devices || devices.length === 0) {
                container.innerHTML = '<div class="empty-device-list"><div class="empty-icon">📱</div><p>暂无登录设备</p></div>';
                return;
            }
            
            const html = devices.map(device => {
                const deviceType = device.deviceType || 'Web';
                const isActive = device.isActive;
                const isCurrent = isCurrentDevice(device);
                
                return `
                    <div class="device-item ${isCurrent ? 'current-device' : ''}">
                        <div class="device-icon ${deviceType}">
                            ${getDeviceIcon(deviceType)}
                        </div>
                        <div class="device-info">
                            <div class="device-name">
                                ${getDeviceName(device)}
                                ${isCurrent ? '<span class="current-device-badge">当前设备</span>' : ''}
                            </div>
                            <div class="device-details">
                                IP地址: ${device.ipAddress || '未知'}<br>
                                最后登录: ${formatDateTime(device.lastLoginAt)}
                            </div>
                        </div>
                        <div class="device-status">
                            <div class="status-indicator ${isActive ? 'active' : 'inactive'}"></div>
                            <span class="status-text">${isActive ? '在线' : '离线'}</span>
                        </div>
                        <div class="device-actions-btn">
                            ${!isCurrent && isActive ? `<button class="btn btn-danger" onclick="logoutDevice(${device.id})">强制下线</button>` : ''}
                        </div>
                    </div>
                `;
            }).join('');
            
            container.innerHTML = html;
        }
    
        function getDeviceIcon(deviceType) {
            const icons = {
                'desktop': '🖥️',
                'mobile': '📱',
                'tablet': '📱',
                'web': '🌐',
                'android': '📱',
                'ios': '📱',
                'windows': '🖥️',
                'mac': '🖥️',
                'linux': '🖥️'
            };
            return icons[deviceType.toLowerCase()] || '🌐';
        }
    
        function getDeviceName(device) {
            if (device.deviceInfo) {
                return device.deviceInfo;
            }
            
            const typeNames = {
                'desktop': '桌面设备',
                'mobile': '移动设备',
                'tablet': '平板设备',
                'web': '网页浏览器',
                'android': 'Android设备',
                'ios': 'iOS设备',
                'windows': 'Windows设备',
                'mac': 'Mac设备',
                'linux': 'Linux设备'
            };
            
            return typeNames[device.deviceType?.toLowerCase()] || '未知设备';
        }
    
        function isCurrentDevice(device) {
            // 简单判断：如果是当前IP且是活跃状态，可能是当前设备
            // 实际应用中可能需要更精确的判断逻辑
            return device.isActive && device.deviceType === 'Web';
        }
    
        function formatDateTime(dateTimeStr) {
            if (!dateTimeStr) return '未知';
            
            try {
                const date = new Date(dateTimeStr);
                const now = new Date();
                const diff = now - date;
                
                if (diff < 60000) { // 1分钟内
                    return '刚刚';
                } else if (diff < 3600000) { // 1小时内
                    return Math.floor(diff / 60000) + '分钟前';
                } else if (diff < 86400000) { // 24小时内
                    return Math.floor(diff / 3600000) + '小时前';
                } else {
                    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
                }
            } catch (e) {
                return dateTimeStr;
            }
        }
    
        // 全局变量存储当前要下线的设备ID
        let currentLogoutDeviceId = null;
    
        // 强制下线设备 - 显示确认对话框
        function logoutDevice(deviceId) {
            if (!deviceId) {
                showDeviceAlert('error', '设备ID无效');
                return;
            }
    
            currentLogoutDeviceId = deviceId;
            showLogoutDeviceModal();
        }
    
        // 显示强制下线设备确认模态框
        function showLogoutDeviceModal() {
            const modal = document.getElementById('logoutDeviceModal');
            if (modal) {
                modal.classList.add('show');
            } else {
                console.error('找不到强制下线设备模态框元素');
            }
        }
    
        // 隐藏强制下线设备确认模态框
        function hideLogoutDeviceModal() {
            const modal = document.getElementById('logoutDeviceModal');
            if (modal) {
                modal.classList.remove('show');
            }
            currentLogoutDeviceId = null;
        }
    
        // 确认强制下线设备
        function confirmLogoutDevice() {
            if (!currentLogoutDeviceId) {
                showDeviceAlert('error', '设备ID无效');
                hideLogoutDeviceModal();
                return;
            }
    
            performLogoutDevice(currentLogoutDeviceId);
            hideLogoutDeviceModal();
        }
    
        // 执行强制下线设备操作
        async function performLogoutDevice(deviceId) {
            try {
                const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
                if (!token) {
                    throw new Error('请先登录');
                }
                
                const response = await fetch(`/api/user/devices/${deviceId}`, {
                    method: 'DELETE',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    }
                });
                
                // 处理401未授权错误
                if (response.status === 401) {
                    showDeviceAlert('error', '登录已过期，请重新登录');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return;
                }
                
                const result = await response.json();
                
                if (response.ok && result.code === 200) {
                    showDeviceAlert('success', '设备已成功下线');
                    loadDeviceList(); // 重新加载列表
                } else {
                    throw new Error(result.message || '强制下线失败');
                }
            } catch (error) {
                showDeviceAlert('error', error.message);
            }
        }
    
        // 强制下线所有其他设备 - 显示确认对话框
        function logoutAllOtherDevices() {
            showLogoutAllDevicesModal();
        }
    
        // 显示强制下线所有其他设备确认模态框
        function showLogoutAllDevicesModal() {
            const modal = document.getElementById('logoutAllDevicesModal');
            if (modal) {
                modal.classList.add('show');
            } else {
                console.error('找不到强制下线所有设备模态框元素');
            }
        }
    
        // 隐藏强制下线所有其他设备确认模态框
        function hideLogoutAllDevicesModal() {
            const modal = document.getElementById('logoutAllDevicesModal');
            if (modal) {
                modal.classList.remove('show');
            }
        }
    
        // 确认强制下线所有其他设备
        function confirmLogoutAllDevices() {
            performLogoutAllDevices();
            hideLogoutAllDevicesModal();
        }
    
        // 获取设备信息
        function getDeviceInfo() {
            const userAgent = navigator.userAgent;
            let deviceType = 'Web';
            
            if (/Mobile|Android|iPhone|iPad/.test(userAgent)) {
                deviceType = 'Mobile';
            } else if (/Tablet|iPad/.test(userAgent)) {
                deviceType = 'Tablet';
            }
            
            return {
                deviceType: deviceType,
                deviceInfo: userAgent
            };
        }
    
        // 执行强制下线所有其他设备操作
        async function performLogoutAllDevices() {
            try {
                const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
                if (!token) {
                    throw new Error('请先登录');
                }
                
                // 获取当前设备信息
                const deviceInfo = getDeviceInfo();
                const currentDeviceInfo = deviceInfo.deviceInfo; // 使用完整的userAgent作为设备信息
                
                const response = await fetch('/api/user/devices/logout-others', {
                    method: 'POST',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    body: `currentDeviceInfo=${encodeURIComponent(currentDeviceInfo)}`
                });
                
                // 处理401未授权错误
                if (response.status === 401) {
                    showDeviceAlert('error', '登录已过期，请重新登录');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return;
                }
                
                const result = await response.json();
                
                if (response.ok && result.code === 200) {
                    // 使用后端返回的消息，区分是否有其他设备
                    showDeviceAlert('success', result.message);
                    // 延迟重新加载列表，避免成功提示被立即隐藏，且不显示loading进度条
                    setTimeout(() => {
                        loadDeviceList(false); 
                    }, 1000);
                } else {
                    throw new Error(result.message || '批量下线失败');
                }
            } catch (error) {
                showDeviceAlert('error', error.message);
            }
        }
    
        async function refreshDeviceList() {
            try {
                await loadDeviceList(false); // 不显示加载进度条
                showDeviceAlert('success', '设备列表刷新成功');
            } catch (error) {
                // 如果是登录过期错误，不显示额外的错误提示，因为loadDeviceList已经处理了
                if (error.message !== '登录已过期') {
                    showDeviceAlert('error', '刷新设备列表失败');
                }
            }
        }
    
        function showDeviceAlert(type, message) {
            const alert = document.getElementById('deviceAlert');
            alert.className = `device-alert ${type}`;
            alert.textContent = message;
            alert.style.display = 'block';
            
            // 3秒后自动隐藏
            setTimeout(() => {
                alert.style.display = 'none';
            }, 3000);
        }
    
        function openPrivacySettings() {
            alert('隐私设置功能正在开发中...');
        }
    
        // 退出登录相关函数
        function logout() {
            const hasSavedInfo = localStorage.getItem('savedEmail') || localStorage.getItem('savedPassword');
            const modal = document.getElementById('logoutModal');
            const message = document.getElementById('logoutMessage');
            const keepBtn = document.getElementById('logoutKeepBtn');
            const clearBtn = document.getElementById('logoutClearBtn');
            
            if (hasSavedInfo) {
                message.innerHTML = '您有保存的登录信息，请选择退出方式：<br><br>' +
                    '<span class="highlight">• 保留登录信息</span>：下次可快速登录<br>' +
                    '<span style="color: #fa5151;">• 完全退出</span>：清除所有保存的信息';
                keepBtn.style.display = 'block';
                clearBtn.textContent = '完全退出';
            } else {
                message.textContent = '确定要退出登录吗？';
                keepBtn.style.display = 'none';
                clearBtn.textContent = '退出登录';
            }
            
            modal.classList.add('show');
        }
    
        function closeLogoutModal() {
            document.getElementById('logoutModal').classList.remove('show');
        }
    
        function logoutKeepInfo() {
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('userInfo');
            sessionStorage.removeItem('accessToken');
            sessionStorage.removeItem('refreshToken');
            sessionStorage.removeItem('userInfo');
            
            closeLogoutModal();
            window.location.href = '/login.html';
        }
    
        function logoutClearInfo() {
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('userInfo');
            localStorage.removeItem('savedEmail');
            localStorage.removeItem('savedPassword');
            localStorage.removeItem('rememberMe');
            sessionStorage.removeItem('accessToken');
            sessionStorage.removeItem('refreshToken');
            sessionStorage.removeItem('userInfo');
            
            closeLogoutModal();
            window.location.href = '/login.html';
        }
    
        // 点击模态框外部关闭
        document.addEventListener('click', function(e) {
            const logoutModal = document.getElementById('logoutModal');
            const profileModal = document.getElementById('profileModal');
            const loginDeviceModal = document.getElementById('loginDeviceModal');
            const logoutDeviceModal = document.getElementById('logoutDeviceModal');
            const logoutAllDevicesModal = document.getElementById('logoutAllDevicesModal');
            const deleteModal = document.getElementById('deleteModal');
            
            if (e.target === logoutModal) {
                closeLogoutModal();
            }
            
            if (e.target === profileModal) {
                closeProfileModal();
            }
            
            if (e.target === loginDeviceModal) {
                closeLoginDeviceModal();
            }
            
            if (e.target === logoutDeviceModal) {
                hideLogoutDeviceModal();
            }
            
            if (e.target === logoutAllDevicesModal) {
                hideLogoutAllDevicesModal();
            }
            
            if (e.target === deleteModal) {
                hideDeleteModal();
            }
        });
    
        // ESC键关闭模态框
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                const logoutModal = document.getElementById('logoutModal');
                const profileModal = document.getElementById('profileModal');
                const loginDeviceModal = document.getElementById('loginDeviceModal');
                const logoutDeviceModal = document.getElementById('logoutDeviceModal');
                const logoutAllDevicesModal = document.getElementById('logoutAllDevicesModal');
                const deleteModal = document.getElementById('deleteModal');
                
                if (logoutModal && logoutModal.classList.contains('show')) {
                    closeLogoutModal();
                } else if (profileModal && profileModal.style.display === 'flex') {
                    closeProfileModal();
                } else if (loginDeviceModal && loginDeviceModal.style.display === 'flex') {
                    closeLoginDeviceModal();
                } else if (logoutDeviceModal && logoutDeviceModal.classList.contains('show')) {
                    hideLogoutDeviceModal();
                } else if (logoutAllDevicesModal && logoutAllDevicesModal.classList.contains('show')) {
                    hideLogoutAllDevicesModal();
                } else if (deleteModal && deleteModal.classList.contains('show')) {
                    hideDeleteModal();
                }
            }
        });
    
        // 密码管理相关函数
         function openPasswordManagement() {
             document.getElementById('passwordManagementModal').style.display = 'flex';
         }
    
         function closePasswordManagement() {
             document.getElementById('passwordManagementModal').style.display = 'none';
             // 清空表单
             document.getElementById('currentPassword').value = '';
             document.getElementById('newPasswordInput').value = '';
             document.getElementById('confirmNewPassword').value = '';
             document.getElementById('passwordErrorMessage').style.display = 'none';
         }
    
        async function changePassword() {
             const currentPassword = document.getElementById('currentPassword').value;
             const newPassword = document.getElementById('newPasswordInput').value;
             const confirmPassword = document.getElementById('confirmNewPassword').value;
             const errorMessage = document.getElementById('passwordErrorMessage');
             const changeBtn = document.getElementById('changePasswordBtn');
             
             // 验证输入
             if (!currentPassword || !newPassword || !confirmPassword) {
                 errorMessage.textContent = '❌ 请填写所有字段';
                 errorMessage.className = 'error-message';
                 errorMessage.style.display = 'block';
                 return;
             }
             
             if (newPassword.length < 6) {
                 errorMessage.textContent = '❌ 新密码至少需要6位字符';
                 errorMessage.className = 'error-message';
                 errorMessage.style.display = 'block';
                 return;
             }
             
             if (newPassword !== confirmPassword) {
                 errorMessage.textContent = '❌ 两次输入的新密码不一致';
                 errorMessage.className = 'error-message';
                 errorMessage.style.display = 'block';
                 return;
             }
             
             try {
                 changeBtn.disabled = true;
                 changeBtn.textContent = '修改中...';
                 errorMessage.style.display = 'none';
                 
                 const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
                 if (!token) {
                     errorMessage.textContent = '❌ 请先登录后再修改密码';
                     errorMessage.className = 'error-message';
                     errorMessage.style.display = 'block';
                     return;
                 }
                 
                 const response = await fetch('/api/auth/password/change', {
                     method: 'POST',
                     headers: {
                         'Content-Type': 'application/json',
                         'Authorization': 'Bearer ' + token
                     },
                     body: JSON.stringify({
                         currentPassword: currentPassword,
                         newPassword: newPassword,
                         confirmPassword: confirmPassword
                     })
                 });
                 
                 const result = await response.json();
                 
                 if (response.ok && result.code === 200) {
                     errorMessage.textContent = '✅ 密码修改成功！正在跳转到登录页面...';
                     errorMessage.className = 'success-message';
                     errorMessage.style.display = 'block';
                     
                     // 清除所有登录信息
                     setTimeout(() => {
                         localStorage.removeItem('accessToken');
                         localStorage.removeItem('refreshToken');
                         localStorage.removeItem('userInfo');
                         sessionStorage.removeItem('accessToken');
                         sessionStorage.removeItem('refreshToken');
                         sessionStorage.removeItem('userInfo');
                         
                         // 跳转到登录页面
                         window.location.href = '/login.html';
                     }, 2000);
                 } else {
                     let errorMsg = result.message || '密码修改失败，请重试';
                     if (response.status === 401) {
                         errorMsg = '❌ 登录已过期，请重新登录后再试';
                     } else if (response.status === 403) {
                         errorMsg = '❌ 当前密码错误，请检查后重试';
                     } else {
                         errorMsg = '❌ ' + errorMsg;
                     }
                     errorMessage.textContent = errorMsg;
                     errorMessage.className = 'error-message';
                     errorMessage.style.display = 'block';
                 }
             } catch (error) {
                 errorMessage.textContent = '❌ 网络错误，请重试';
                 errorMessage.className = 'error-message';
                 errorMessage.style.display = 'block';
             } finally {
                 changeBtn.disabled = false;
                 changeBtn.textContent = '修改密码';
             }
         }
    
        // 点击模态框外部关闭密码管理对话框已移除，只能通过取消按钮关闭
    
        // 文件管理相关函数
        let selectedFiles = [];
        let currentPage = 0;
        const pageSize = 20;
        let uploadCancelled = false;
        let currentUploadController = null;
    
        function initFileManager() {
            setupFileEventListeners();
            
            // 检查用户登录状态
            const token = getAuthToken();
            if (!token) {
                showFileAlert('请先登录后再使用文件管理功能', 'error');
                setTimeout(() => {
                    window.location.href = '/login.html';
                }, 2000);
                return;
            }
            
            // 初始化加载文件管理器数据
            refreshFileManager();
        }
    
        function setupFileEventListeners() {
            const uploadArea = document.querySelector('.upload-area');
            const fileInput = document.getElementById('fileInput');
            const uploadType = document.getElementById('uploadType');
    
            if (fileInput) {
                fileInput.addEventListener('change', handleFileSelect);
            }
    
            if (uploadArea) {
                uploadArea.addEventListener('dragover', handleDragOver);
                uploadArea.addEventListener('dragleave', handleDragLeave);
                uploadArea.addEventListener('drop', handleDrop);
            }
    
            if (uploadType) {
                uploadType.addEventListener('change', toggleImageOptions);
            }
        }
    
        function switchFileTab(tabName) {
            // 更新选项卡样式
            document.querySelectorAll('.file-tab').forEach(tab => tab.classList.remove('active'));
            document.querySelectorAll('.file-tab-content').forEach(content => content.classList.remove('active'));
    
            // 激活选中的选项卡
            event.target.classList.add('active');
            document.getElementById(tabName + 'FileTab').classList.add('active');
    
            // 如果切换到管理选项卡，刷新文件管理器数据
            if (tabName === 'manage') {
                refreshFileManager();
            }
        }
    
        function toggleImageOptions() {
            const uploadType = document.getElementById('uploadType').value;
            const imageSizeOptions = document.getElementById('imageSizeOptions');
            const imageHeightOptions = document.getElementById('imageHeightOptions');
    
            if (uploadType === 'image') {
                imageSizeOptions.style.display = 'block';
                imageHeightOptions.style.display = 'block';
            } else {
                imageSizeOptions.style.display = 'none';
                imageHeightOptions.style.display = 'none';
            }
        }
    
        function handleFileSelect(event) {
            const files = Array.from(event.target.files);
            selectedFiles = files;
            updateUploadButton();
            
            // 显示取消上传按钮（如果有文件选择）
            const cancelBtn = document.getElementById('cancelUploadBtn');
            if (files.length > 0) {
                cancelBtn.style.display = 'inline-block';
                showFileAlert('已选择 ' + files.length + ' 个文件，可以开始上传', 'success');
            } else {
                cancelBtn.style.display = 'none';
            }
        }
    
        function handleDragOver(event) {
            event.preventDefault();
            event.currentTarget.classList.add('dragover');
        }
    
        function handleDragLeave(event) {
            event.currentTarget.classList.remove('dragover');
        }
    
        function handleDrop(event) {
            event.preventDefault();
            event.currentTarget.classList.remove('dragover');
            
            const files = Array.from(event.dataTransfer.files);
            selectedFiles = files;
            updateUploadButton();
            
            // 显示取消上传按钮（如果有文件选择）
            const cancelBtn = document.getElementById('cancelUploadBtn');
            if (files.length > 0) {
                cancelBtn.style.display = 'inline-block';
                showFileAlert('已选择 ' + files.length + ' 个文件，可以开始上传', 'success');
            } else {
                cancelBtn.style.display = 'none';
            }
        }
    
        function updateUploadButton() {
            const uploadBtn = document.getElementById('uploadBtn');
            if (uploadBtn) {
                uploadBtn.disabled = selectedFiles.length === 0;
                uploadBtn.textContent = selectedFiles.length > 0 ? 
                    `上传 ${selectedFiles.length} 个文件` : '开始上传';
            }
        }
    
        async function uploadFiles() {
            if (selectedFiles.length === 0) {
                showFileAlert('请先选择文件', 'error');
                return;
            }
    
            const uploadType = document.getElementById('uploadType').value;
            const maxWidth = document.getElementById('maxWidth').value;
            const maxHeight = document.getElementById('maxHeight').value;
    
            // 重置取消状态
            uploadCancelled = false;
            
            // 显示进度条和取消按钮
            const progressContainer = document.getElementById('uploadProgress');
            const progressFill = document.getElementById('progressFill');
            const progressText = document.getElementById('progressText');
            const uploadBtn = document.getElementById('uploadBtn');
            const cancelBtn = document.getElementById('cancelUploadBtn');
            
            progressContainer.style.display = 'block';
            uploadBtn.style.display = 'none';
            cancelBtn.style.display = 'inline-block';
    
            let successCount = 0;
            let errorCount = 0;
    
            for (let i = 0; i < selectedFiles.length; i++) {
                // 检查是否已取消
                if (uploadCancelled) {
                    showFileAlert('上传已取消', 'warning');
                    break;
                }
                
                const file = selectedFiles[i];
                const progress = ((i + 1) / selectedFiles.length) * 100;
                
                progressFill.style.width = progress + '%';
                progressText.textContent = `正在上传: ${file.name} (${i + 1}/${selectedFiles.length})`;
    
                try {
                    const result = await uploadSingleFile(file, uploadType, maxWidth, maxHeight);
                    if (result.success) {
                        successCount++;
                    } else {
                        errorCount++;
                        // 显示具体的错误信息，延长显示时间
                        const errorMsg = result.message || '上传失败';
                        showFileAlert(`文件 "${file.name}" 上传失败: ${errorMsg}`, 'error');
                    }
                } catch (error) {
                    console.error('上传失败:', error);
                    
                    // 处理认证错误
                    if (error.message.includes('登录') || error.message.includes('未登录')) {
                        showFileAlert(error.message, 'error');
                        clearLoginInfo();
                        setTimeout(() => {
                            window.location.href = '/login.html';
                        }, 2000);
                        return;
                    }
                    
                    errorCount++;
                    showFileAlert(`文件 "${file.name}" 上传失败: ${error.message}`, 'error');
                }
            }
    
            // 隐藏进度条，显示上传按钮
            progressContainer.style.display = 'none';
            uploadBtn.style.display = 'inline-block';
            cancelBtn.style.display = 'none';
    
            // 显示最终结果
            if (!uploadCancelled) {
                if (errorCount === 0) {
                    showFileAlert(`所有文件上传成功！共 ${successCount} 个文件`, 'success');
                } else {
                    showFileAlert(`上传完成：成功 ${successCount} 个，失败 ${errorCount} 个`, 'error');
                }
            }
    
            // 重置
            selectedFiles = [];
            document.getElementById('fileInput').value = '';
            updateUploadButton();
            currentUploadController = null;
            
            // 刷新文件管理器数据
            if (!uploadCancelled) {
                await refreshFileManager();
            }
        }
        
        function cancelUpload() {
            uploadCancelled = true;
            if (currentUploadController) {
                currentUploadController.abort();
            }
            
            // 隐藏进度条，显示上传按钮
            const progressContainer = document.getElementById('uploadProgress');
            const uploadBtn = document.getElementById('uploadBtn');
            const cancelBtn = document.getElementById('cancelUploadBtn');
            
            progressContainer.style.display = 'none';
            uploadBtn.style.display = 'inline-block';
            cancelBtn.style.display = 'none';
            
            // 重置文件选择
            selectedFiles = [];
            document.getElementById('fileInput').value = '';
            updateUploadButton();
            
            showFileAlert('上传已取消', 'warning');
        }
    
        async function uploadSingleFile(file, uploadType, maxWidth, maxHeight) {
            const formData = new FormData();
            formData.append('file', file);
    
            let url = '/api/files/upload';
            if (uploadType === 'image') {
                url = '/api/files/upload/image';
                formData.append('maxWidth', maxWidth);
                formData.append('maxHeight', maxHeight);
            }
    
            const headers = getAuthHeaders();
            
            // 检查是否有有效的token
            if (!headers.Authorization) {
                throw new Error('用户未登录，请重新登录');
            }
            
            delete headers['Content-Type']; // 让浏览器自动设置Content-Type
    
            // 创建AbortController用于取消请求
            currentUploadController = new AbortController();
    
            const response = await fetch(url, {
                method: 'POST',
                headers: headers,
                body: formData,
                signal: currentUploadController.signal
            });
    
            // 处理401未授权错误
            if (response.status === 401) {
                throw new Error('登录已过期，请重新登录');
            }
    
            // 检查响应状态
            if (!response.ok) {
                const errorResult = await response.json();
                throw new Error(errorResult.message || '上传失败');
            }
    
            return await response.json();
        }
    
        async function loadFileList() {
            const loading = document.getElementById('fileLoading');
            const fileList = document.getElementById('fileList');
            const fileTypeFilter = document.getElementById('fileTypeFilter').value;
    
            loading.style.display = 'block';
            fileList.innerHTML = '';
    
            try {
                let url = `/api/files/list?page=${currentPage}&size=${pageSize}`;
                if (fileTypeFilter) {
                    url += `&fileType=${fileTypeFilter}`;
                }
    
                const headers = getAuthHeaders();
                
                // 检查是否有有效的token
                if (!headers.Authorization) {
                    showFileAlert('用户未登录，请重新登录', 'error');
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return;
                }
    
                const response = await fetch(url, {
                    headers: headers
                });
    
                // 处理401未授权错误
                if (response.status === 401) {
                    showFileAlert('登录已过期，请重新登录', 'error');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return;
                }
    
                const result = await response.json();
                if (result.success) {
                    displayFileList(result.data.files);
                } else {
                    showFileAlert('加载文件列表失败: ' + result.message, 'error');
                }
            } catch (error) {
                console.error('加载文件列表失败:', error);
                showFileAlert('加载文件列表失败: ' + error.message, 'error');
            } finally {
                loading.style.display = 'none';
            }
        }
    
        function displayFileList(files) {
            const fileList = document.getElementById('fileList');
            fileList.innerHTML = '';
    
            if (!files || files.length === 0) {
                fileList.innerHTML = '<div style="padding: 40px; text-align: center; color: #666;">暂无文件</div>';
                return;
            }
    
            files.forEach(file => {
                // 跳过空的文件对象（后端返回的空result）
                if (!file || !file.fileId) {
                    console.warn('跳过无效的文件对象:', file);
                    return;
                }
                
                const fileItem = createFileItem(file);
                if (fileItem.innerHTML.trim()) { // 只添加有内容的文件项
                    fileList.appendChild(fileItem);
                }
            });
        }
    
        function createFileItem(file) {
            const item = document.createElement('div');
            item.className = 'file-item';
    
            const icon = getFileIcon(file.fileType);
            const size = formatFileSize(file.fileSize);
            const uploadTime = formatDate(new Date(file.createdAt));
            
            // 确保fileId不为null或undefined
            const fileId = file.fileId || file.id;
            if (!fileId) {
                console.error('文件ID为空:', file);
                return item; // 返回空的item，不显示删除按钮
            }
    
            item.innerHTML = `
                <div class="file-icon ${file.fileType.toLowerCase()}">${icon}</div>
                <div class="file-info">
                    <div class="file-name">${escapeHtml(file.originalFilename)}</div>
                    <div class="file-details">${size} • ${uploadTime}</div>
                </div>
                <div class="file-actions">
                    <button class="btn-small btn-view" onclick="viewFile('${file.fileUrl}')">查看</button>
                    <button class="btn-small btn-copy" onclick="copyFileUrl('${file.fileUrl}')">复制链接</button>
                    <button class="btn-small btn-delete" onclick="deleteFile('${fileId}')">删除</button>
                </div>
            `;
    
            return item;
        }
    
        function getFileIcon(fileType) {
            switch (fileType) {
                case 'IMAGE': return '🖼️';
                case 'VIDEO': return '🎥';
                case 'AUDIO': return '🎵';
                case 'DOCUMENT': return '📄';
                default: return '📁';
            }
        }
    
        function viewFile(fileUrl) {
            window.open(fileUrl, '_blank');
        }
    
        function copyFileUrl(fileUrl) {
            const fullUrl = window.location.origin + fileUrl;
            copyToClipboard(fullUrl);
            showFileAlert('文件链接已复制到剪贴板', 'success');
        }
    
        // 当前要删除的文件ID
        let currentDeleteFileId = null;
    
        // 删除文件 - 显示确认对话框
        function deleteFile(fileId) {
            if (!fileId) {
                showFileAlert('文件ID无效', 'error');
                return;
            }
    
            currentDeleteFileId = fileId;
            showDeleteModal();
        }
    
        // 显示删除确认模态框
        function showDeleteModal() {
            const modal = document.getElementById('deleteModal');
            if (modal) {
                modal.classList.add('show');
            } else {
                console.error('找不到删除模态框元素');
            }
        }
    
        // 隐藏删除确认模态框
        function hideDeleteModal() {
            const modal = document.getElementById('deleteModal');
            if (modal) {
                modal.classList.remove('show');
            }
            currentDeleteFileId = null;
        }
    
        // 确认删除
        function confirmDelete() {
            if (!currentDeleteFileId) {
                showFileAlert('文件ID无效', 'error');
                hideDeleteModal();
                return;
            }
    
            performDelete(currentDeleteFileId);
            hideDeleteModal();
        }
    
        // 执行删除操作
        async function performDelete(fileId) {
            try {
                const headers = getAuthHeaders();
                
                // 检查是否有有效的token
                if (!headers.Authorization) {
                    showFileAlert('用户未登录，请重新登录', 'error');
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return;
                }
                
                const response = await fetch(`/api/files/${fileId}`, {
                    method: 'DELETE',
                    headers: headers
                });
    
                // 处理401未授权错误
                if (response.status === 401) {
                    showFileAlert('登录已过期，请重新登录', 'error');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return;
                }
    
                const result = await response.json();
                if (result.success) {
                    showFileAlert('文件删除成功', 'success');
                    // 刷新文件列表和统计信息
                    await refreshFileManager();
                } else {
                    showFileAlert('删除失败: ' + result.message, 'error');
                }
            } catch (error) {
                console.error('删除文件失败:', error);
                showFileAlert('删除失败: ' + error.message, 'error');
            }
        }
    
        async function loadFileStats() {
            try {
                const headers = getAuthHeaders();
                
                // 检查是否有有效的token
                if (!headers.Authorization) {
                    return;
                }
                
                const response = await fetch('/api/files/stats', {
                    headers: headers
                });
    
                // 处理401未授权错误
                if (response.status === 401) {
                    showFileAlert('登录已过期，请重新登录', 'error');
                    clearLoginInfo();
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 2000);
                    return;
                }
    
                const result = await response.json();
                if (result.success) {
                    updateFileStats(result.data);
                }
            } catch (error) {
                console.error('加载统计信息失败:', error);
            }
        }
    
        function updateFileStats(stats) {
            console.log('收到的统计数据:', stats);
            
            document.getElementById('totalFiles').textContent = stats.totalFiles || 0;
            document.getElementById('totalSize').textContent = formatFileSize(stats.totalSize || 0);
            
            // 从typeStats中获取各类型文件数量
            const typeStats = stats.typeStats || {};
            console.log('typeStats数据:', typeStats);
            
            document.getElementById('imageCount').textContent = typeStats.image || 0;
            document.getElementById('documentCount').textContent = typeStats.document || 0;
            document.getElementById('videoCount').textContent = typeStats.video || 0;
            document.getElementById('audioCount').textContent = typeStats.audio || 0;
            
            console.log('设置后的显示值:', {
                image: typeStats.image || 0,
                document: typeStats.document || 0,
                video: typeStats.video || 0,
                audio: typeStats.audio || 0
            });
        }
    
        // 用于管理提示消息的状态
        let successAlertTimeout = null;
        let errorAlertTimeout = null;
    
        function showFileAlert(message, type, duration) {
            // 根据消息类型设置默认显示时间
            if (duration === undefined) {
                if (type === 'success') {
                    duration = 1500; // 成功提示1.5秒
                } else {
                    duration = 2000; // 错误提示2秒
                }
            }
            
            if (type === 'success') {
                showSuccessAlert(message, duration);
            } else {
                showErrorAlert(message, duration);
            }
        }
    
        function showSuccessAlert(message, duration) {
            const alertDiv = document.getElementById('fileSuccessAlert');
            if (!alertDiv) return;
            
            // 清除之前的定时器
            if (successAlertTimeout) {
                clearTimeout(successAlertTimeout);
            }
            
            alertDiv.textContent = message;
            alertDiv.style.display = 'block';
            
            successAlertTimeout = setTimeout(() => {
                alertDiv.style.display = 'none';
                successAlertTimeout = null;
            }, duration);
        }
    
        function showErrorAlert(message, duration) {
            const alertDiv = document.getElementById('fileErrorAlert');
            if (!alertDiv) return;
            
            // 清除之前的定时器
            if (errorAlertTimeout) {
                clearTimeout(errorAlertTimeout);
            }
            
            alertDiv.textContent = message;
            alertDiv.style.display = 'block';
            
            errorAlertTimeout = setTimeout(() => {
                alertDiv.style.display = 'none';
                errorAlertTimeout = null;
            }, duration);
        }
    
        // 辅助函数
        function formatFileSize(bytes) {
            if (bytes === 0) return '0 B';
            const k = 1024;
            const sizes = ['B', 'KB', 'MB', 'GB'];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
        }
    
        function formatDate(date) {
            const now = new Date();
            const diff = now - date;
            const days = Math.floor(diff / (1000 * 60 * 60 * 24));
            
            if (days === 0) {
                return '今天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
            } else if (days === 1) {
                return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
            } else if (days < 7) {
                return days + '天前';
            } else {
                return date.toLocaleDateString('zh-CN');
            }
        }
    
        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }
    
        // 刷新文件管理器数据
        async function refreshFileManager() {
            try {
                // 显示加载状态
                const refreshBtn = document.querySelector('.refresh-btn');
                if (refreshBtn) {
                    refreshBtn.style.animation = 'spin 1s linear infinite';
                    refreshBtn.disabled = true;
                }
                
                // 同时刷新文件列表和统计信息
                await Promise.all([
                    loadFileList(),
                    loadFileStats()
                ]);
                
                showFileAlert('数据刷新成功', 'success');
                
            } catch (error) {
                console.error('刷新数据失败:', error);
                showFileAlert('刷新失败: ' + error.message, 'error');
            } finally {
                // 恢复按钮状态
                const refreshBtn = document.querySelector('.refresh-btn');
                if (refreshBtn) {
                    refreshBtn.style.animation = '';
                    refreshBtn.disabled = false;
                }
            }
        }
    
        function copyToClipboard(text) {
            if (navigator.clipboard) {
                navigator.clipboard.writeText(text);
            } else {
                // 兼容旧浏览器
                const textArea = document.createElement('textarea');
                textArea.value = text;
                document.body.appendChild(textArea);
                textArea.select();
                document.execCommand('copy');
                document.body.removeChild(textArea);
            }
        }
    
        // 点击模态框背景关闭
        document.addEventListener('click', function(event) {
            const modal = document.getElementById('deleteModal');
            if (event.target === modal) {
                hideDeleteModal();
            }
        });
    
        // ESC键关闭模态框
        document.addEventListener('keydown', function(event) {
            if (event.key === 'Escape') {
                hideDeleteModal();
            }
        });
        
        // 自动刷新机制
        let autoRefreshTimer = null;
        
        function startAutoRefresh() {
            // 如果已有定时器，先清除
            if (autoRefreshTimer) {
                clearInterval(autoRefreshTimer);
            }
            
            // 每30秒自动刷新用户状态和联系人列表
            autoRefreshTimer = setInterval(async () => {
                try {
                    const token = getAuthToken();
                    if (token) {
                        console.log('自动刷新用户状态和联系人列表...');
                        await updateUserInfoDisplay();
                        
                        // 如果当前在联系人页面，则刷新联系人列表
                        const contactsTab = document.getElementById('contactsTab');
                        if (contactsTab && contactsTab.classList.contains('active')) {
                            await initContactsList();
                        }
                        
                        // 如果当前在好友请求页面，则刷新好友请求列表
                        const friendRequestsTab = document.getElementById('friendRequestsTab');
                        if (friendRequestsTab && friendRequestsTab.classList.contains('active')) {
                            await loadFriendRequests();
                        }
                    }
                } catch (error) {
                    console.error('自动刷新失败:', error);
                }
            }, 30000); // 30秒间隔
            
            console.log('自动刷新机制已启动，每30秒检查一次用户状态、联系人列表和好友请求');
        }
        
        function stopAutoRefresh() {
            if (autoRefreshTimer) {
                clearInterval(autoRefreshTimer);
                autoRefreshTimer = null;
                console.log('自动刷新机制已停止');
            }
        }
        
        // 页面可见性变化时的处理
        document.addEventListener('visibilitychange', function() {
            if (document.hidden) {
                // 页面隐藏时停止自动刷新以节省资源
                console.log('页面隐藏，暂停自动刷新');
            } else {
                // 页面重新可见时立即刷新一次并重启定时器
                console.log('页面重新可见，恢复自动刷新');
                const token = getAuthToken();
                if (token) {
                    updateUserInfoDisplay().catch(error => {
                        console.error('页面恢复时刷新用户状态失败:', error);
                    });
                    
                    // 如果当前在联系人页面，则刷新联系人列表
                    const contactsTab = document.getElementById('contactsTab');
                    if (contactsTab && contactsTab.classList.contains('active')) {
                        initContactsList().catch(error => {
                            console.error('页面恢复时刷新联系人列表失败:', error);
                        });
                    }
                    
                    // 如果当前在好友请求页面，则刷新好友请求列表
                    const friendRequestsTab = document.getElementById('friendRequestsTab');
                    if (friendRequestsTab && friendRequestsTab.classList.contains('active')) {
                        loadFriendRequests().catch(error => {
                            console.error('页面恢复时刷新好友请求失败:', error);
                        });
                    }
                }
            }
        });
        
        // 页面卸载时清理定时器
        window.addEventListener('beforeunload', function() {
            stopAutoRefresh();
        });
    
        // 好友请求相关功能
        function openNewFriends() {
            showFriendRequestsPage();
            document.getElementById('pageTitle').textContent = '新的朋友';
            document.getElementById('rightBtn').style.display = 'none';
            loadFriendRequests();
        }
        
        // 显示好友请求页面
        function showFriendRequestsPage() {
            // 隐藏所有主要标签页
            document.querySelectorAll('.tab-content').forEach(content => {
                content.classList.remove('active');
            });
            
            // 显示好友请求页面
            const friendRequestsTab = document.getElementById('friendRequestsTab');
            if (friendRequestsTab) {
                friendRequestsTab.classList.add('active');
                friendRequestsTab.style.display = 'block';
            }
        }
    
        function backToContactsFromFriendRequests() {
            // 隐藏新朋友页面
            const friendRequestsTab = document.getElementById('friendRequestsTab');
            if (friendRequestsTab) {
                friendRequestsTab.classList.remove('active');
                friendRequestsTab.style.display = 'none';
            }
            
            // 显示联系人页面
            const contactsTab = document.getElementById('contactsTab');
            if (contactsTab) {
                contactsTab.classList.add('active');
                contactsTab.style.display = 'block';
            }
            
            // 更新页面标题和按钮
            document.getElementById('pageTitle').textContent = '联系人';
            document.getElementById('rightBtn').style.display = 'block';
        }
    
        function refreshFriendRequests() {
            loadFriendRequests();
        }
    
        function switchFriendRequestTab(tabType) {
            // 切换标签页样式
            document.querySelectorAll('.friend-request-tab').forEach(tab => {
                tab.classList.remove('active');
            });
            document.querySelectorAll('.friend-request-tab-content').forEach(content => {
                content.classList.remove('active');
                content.style.display = 'none';
            });
    
            // 激活选中的标签页
            if (tabType === 'received') {
                document.querySelector('.friend-request-tab[onclick="switchFriendRequestTab(\'received\')"]').classList.add('active');
                document.getElementById('receivedRequestsTab').classList.add('active');
                document.getElementById('receivedRequestsTab').style.display = 'block';
            } else {
                document.querySelector('.friend-request-tab[onclick="switchFriendRequestTab(\'sent\')"]').classList.add('active');
                document.getElementById('sentRequestsTab').classList.add('active');
                document.getElementById('sentRequestsTab').style.display = 'block';
            }
    
            // 加载对应的数据
            loadFriendRequests();
        }
    
        async function loadFriendRequests() {
            try {
                const headers = getAuthHeaders();
                if (!headers.Authorization) {
                    throw new Error('用户未登录，请重新登录');
                }
    
                // 显示加载状态
                document.getElementById('receivedRequestsLoading').style.display = 'block';
                document.getElementById('sentRequestsLoading').style.display = 'block';
    
                // 加载收到的好友请求
                const receivedResponse = await fetch('/api/contact-requests/received', {
                    method: 'GET',
                    headers: headers
                });
    
                if (receivedResponse.ok) {
                    const receivedData = await receivedResponse.json();
                    if (receivedData.success) {
                        displayReceivedRequests(receivedData.data);
                    } else {
                        console.error('加载收到的好友请求失败:', receivedData.message);
                    }
                } else {
                    console.error('加载收到的好友请求失败:', receivedResponse.status);
                }
    
                // 加载发送的好友请求
                const sentResponse = await fetch('/api/contact-requests/sent', {
                    method: 'GET',
                    headers: headers
                });
    
                if (sentResponse.ok) {
                    const sentData = await sentResponse.json();
                    if (sentData.success) {
                        displaySentRequests(sentData.data);
                    } else {
                        console.error('加载发送的好友请求失败:', sentData.message);
                    }
                } else {
                    console.error('加载发送的好友请求失败:', sentResponse.status);
                }
    
                // 更新统计信息
                updateFriendRequestStats();
    
            } catch (error) {
                console.error('加载好友请求失败:', error);
                showAlert('加载好友请求失败: ' + error.message, 'error');
            } finally {
                // 隐藏加载状态
                document.getElementById('receivedRequestsLoading').style.display = 'none';
                document.getElementById('sentRequestsLoading').style.display = 'none';
            }
        }
    
        function displayReceivedRequests(requests) {
            const container = document.getElementById('receivedRequestsList');
            const noRequestsDiv = document.getElementById('noReceivedRequests');
    
            if (!requests || requests.length === 0) {
                container.innerHTML = '';
                noRequestsDiv.style.display = 'block';
                return;
            }
    
            noRequestsDiv.style.display = 'none';
            container.innerHTML = requests.map(request => `
                <div class="friend-request-item">
                    <div class="friend-request-header">
                        <div class="friend-request-avatar">
                            ${request.requesterAvatarUrl ? 
                                `<img src="${request.requesterAvatarUrl}" alt="${request.requesterNickname}" style="width: 100%; height: 100%; border-radius: 50%; object-fit: cover;" onerror="this.style.display='none'; this.parentNode.innerHTML='${request.requesterNickname ? request.requesterNickname.charAt(0).toUpperCase() : 'U'}'">` : 
                                (request.requesterNickname ? request.requesterNickname.charAt(0).toUpperCase() : 'U')}
                        </div>
                        <div class="friend-request-user-info">
                            <div class="friend-request-name">${request.requesterNickname || '未知用户'}</div>
                            <div class="friend-request-id">ID: ${request.requesterUserIdStr || request.requesterId || 'N/A'}</div>
                        </div>
                        <div class="friend-request-header-actions">
                            <button class="friend-request-btn view-profile" onclick="viewUserProfile('${request.requesterUserIdStr || request.requesterId}')">
                                👤 查看资料
                            </button>
                        </div>
                    </div>
                    <div class="friend-request-content">
                        <div class="friend-request-message">请求添加您为好友</div>
                        ${request.verificationMessage && request.verificationMessage.trim() ? `<div class="friend-request-verification">${request.verificationMessage}</div>` : ''}
                    </div>
                    <div class="friend-request-footer">
                        <div class="friend-request-meta">
                            <div class="friend-request-time">${formatDateTime(request.createdAt)}</div>
                            <div class="friend-request-status ${request.status.toLowerCase()}">
                                ${getStatusText(request.status)}
                            </div>
                        </div>
                        <div class="friend-request-actions">
                            ${request.status === 'pending' ? `
                                <button class="friend-request-btn accept" onclick="handleFriendRequest(${request.requestId}, 'accept')">
                                    ✓ 同意
                                </button>
                                <button class="friend-request-btn reject" onclick="handleFriendRequest(${request.requestId}, 'reject')">
                                    ✗ 拒绝
                                </button>
                            ` : ''}
                        </div>
                    </div>
                </div>
            `).join('');
        }
    
        function displaySentRequests(requests) {
            const container = document.getElementById('sentRequestsList');
            const noRequestsDiv = document.getElementById('noSentRequests');
    
            if (!requests || requests.length === 0) {
                container.innerHTML = '';
                noRequestsDiv.style.display = 'block';
                return;
            }
    
            noRequestsDiv.style.display = 'none';
            container.innerHTML = requests.map(request => `
                <div class="friend-request-item">
                    <div class="friend-request-header">
                        <div class="friend-request-avatar">
                            ${request.recipientAvatarUrl ? 
                                `<img src="${request.recipientAvatarUrl}" alt="${request.recipientNickname}" onerror="this.style.display='none'; this.parentNode.innerHTML='${request.recipientNickname ? request.recipientNickname.charAt(0).toUpperCase() : 'U'}'">` : 
                                (request.recipientNickname ? request.recipientNickname.charAt(0).toUpperCase() : 'U')}
                        </div>
                        <div class="friend-request-user-info">
                            <div class="friend-request-name">${request.recipientNickname || '未知用户'}</div>
                            <div class="friend-request-id">ID: ${request.recipientUserIdStr || request.recipientId || 'N/A'}</div>
                        </div>
                        <div class="friend-request-header-actions">
                            <button class="friend-request-btn view-profile" onclick="viewUserProfile('${request.recipientUserIdStr || request.recipientId}')">
                                👤 查看资料
                            </button>
                        </div>
                    </div>
                    <div class="friend-request-content">
                        <div class="friend-request-message">${request.message || '请求添加为好友'}</div>
                    </div>
                    <div class="friend-request-footer">
                        <div class="friend-request-meta">
                            <div class="friend-request-time">${formatDateTime(request.createdAt)}</div>
                            <div class="friend-request-status ${request.status.toLowerCase()}">
                                ${getStatusText(request.status)}
                            </div>
                        </div>
                        <div class="friend-request-actions">
                            ${request.status === 'pending' ? `
                                <button class="friend-request-btn cancel" onclick="cancelFriendRequest(${request.requestId})">
                                    取消
                                </button>
                            ` : ''}
                        </div>
                    </div>
                </div>
            `).join('');
        }
    
        async function handleFriendRequest(requestId, action) {
            try {
                const headers = getAuthHeaders();
                if (!headers.Authorization) {
                    throw new Error('用户未登录，请重新登录');
                }
    
                // 获取当前用户ID作为recipientId
                const currentUserId = await getCurrentUserId();
                
                const response = await fetch(`/api/contact-requests/${requestId}/${action}?recipientId=${currentUserId}`, {
                    method: 'PUT',
                    headers: headers
                });
    
                if (response.ok) {
                    const data = await response.json();
                    if (data.success) {
                        showAlert(action === 'accept' ? '已同意好友请求' : '已拒绝好友请求', 'success');
                        loadFriendRequests(); // 重新加载列表
                        updateFriendRequestStats(); // 更新统计
                    } else {
                        showAlert('操作失败: ' + data.message, 'error');
                    }
                } else {
                    showAlert('操作失败，请稍后重试', 'error');
                }
            } catch (error) {
                console.error('处理好友请求失败:', error);
                showAlert('操作失败: ' + error.message, 'error');
            }
        }
    
        async function cancelFriendRequest(requestId) {
            try {
                const headers = getAuthHeaders();
                if (!headers.Authorization) {
                    throw new Error('用户未登录，请重新登录');
                }
    
                // 获取当前用户ID作为requesterId
                const currentUserId = await getCurrentUserId();
                
                const response = await fetch(`/api/contact-requests/${requestId}?requesterId=${currentUserId}`, {
                    method: 'DELETE',
                    headers: headers
                });
    
                if (response.ok) {
                    const data = await response.json();
                    if (data.success) {
                        showAlert('已取消好友请求', 'success');
                        loadFriendRequests(); // 重新加载列表
                    } else {
                        showAlert('取消失败: ' + data.message, 'error');
                    }
                } else {
                    showAlert('取消失败，请稍后重试', 'error');
                }
            } catch (error) {
                console.error('取消好友请求失败:', error);
                showAlert('取消失败: ' + error.message, 'error');
            }
        }
    
        async function updateFriendRequestStats() {
            try {
                const headers = getAuthHeaders();
                if (!headers.Authorization) {
                    return;
                }
    
                const response = await fetch('/api/contact-requests/stats', {
                    method: 'GET',
                    headers: headers
                });
    
                if (response.ok) {
                    const data = await response.json();
                    if (data.success) {
                        const stats = data.data;
                        document.getElementById('pendingRequestsCount').textContent = stats.receivedPendingCount || 0;
                        document.getElementById('sentRequestsCount').textContent = stats.sentPendingCount || 0;
                        document.getElementById('acceptedRequestsCount').textContent = stats.acceptedReceivedCount || 0;
    
                        // 更新联系人页面的好友请求徽章
                        const badge = document.getElementById('friendRequestBadge');
                        if (stats.receivedPendingCount > 0) {
                            badge.textContent = stats.receivedPendingCount;
                            badge.style.display = 'block';
                        } else {
                            badge.style.display = 'none';
                        }
                    }
                }
            } catch (error) {
                console.error('更新好友请求统计失败:', error);
            }
        }
    
        function getStatusText(status) {
            switch (status) {
                case 'pending':
                case 'PENDING':
                    return '待处理';
                case 'accepted':
                case 'ACCEPTED':
                    return '已同意';
                case 'rejected':
                case 'REJECTED':
                    return '已拒绝';
                default:
                    return '未知状态';
            }
        }
    
        function formatDateTime(dateTimeString) {
            if (!dateTimeString) return '';
            const date = new Date(dateTimeString);
            const now = new Date();
            const diffMs = now - date;
            const diffMins = Math.floor(diffMs / 60000);
            const diffHours = Math.floor(diffMs / 3600000);
            const diffDays = Math.floor(diffMs / 86400000);
    
            if (diffMins < 1) {
                return '刚刚';
            } else if (diffMins < 60) {
                return `${diffMins}分钟前`;
            } else if (diffHours < 24) {
                return `${diffHours}小时前`;
            } else if (diffDays < 7) {
                return `${diffDays}天前`;
            } else {
                return date.toLocaleDateString('zh-CN', {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric'
                });
            }
        }
    
        // 获取当前用户ID
        async function getCurrentUserId() {
            const token = getAuthToken();
            if (!token) {
                throw new Error('用户未登录');
            }
    
            try {
                const profileResponse = await fetch('/api/user/profile', {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    }
                });
    
                if (!profileResponse.ok) {
                    throw new Error('获取用户信息失败');
                }
    
                const profileData = await profileResponse.json();
                const currentUserId = (profileData.data || profileData).id;
    
                if (!currentUserId) {
                    throw new Error('无法获取当前用户ID');
                }
    
                return currentUserId;
            } catch (error) {
                console.error('获取当前用户ID失败:', error);
                throw error;
            }
        }
    
        // 在搜索结果中添加好友请求功能
        async function sendFriendRequest(userId, nickname) {
            // 使用自定义模态框替换prompt
            const message = await showFriendRequestModal(nickname);
            if (message === null) return; // 用户取消
    
            try {
                const headers = getAuthHeaders();
                if (!headers.Authorization) {
                    throw new Error('用户未登录，请重新登录');
                }
    
                const currentUserId = await getCurrentUserId();
    
                const response = await fetch('/api/contact-requests', {
                    method: 'POST',
                    headers: {
                        ...headers,
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        requesterId: currentUserId,
                        recipientId: userId,
                        verificationMessage: message || '请求添加您为好友'
                    })
                });
    
                if (response.ok) {
                    const data = await response.json();
                    if (data.success) {
                        showMessage('好友请求已发送', 'success');
                        // 如果在搜索结果页面，重新搜索以更新状态
                        const searchResults = document.getElementById('searchResults');
                        if (searchResults && searchResults.style.display === 'block') {
                            searchUsers();
                        }
                    } else {
                        showMessage('发送失败: ' + data.message, 'error');
                    }
                } else {
                    const errorData = await response.json().catch(() => ({}));
                    const errorMessage = errorData.message || '发送失败，请稍后重试';
                    showMessage('发送失败: ' + errorMessage, 'error');
                }
            } catch (error) {
                console.error('发送好友请求失败:', error);
                showMessage('发送失败: ' + error.message, 'error');
            }
        }
    
        // 页面加载时初始化好友请求统计
        document.addEventListener('DOMContentLoaded', function() {
            // 延迟加载统计信息，确保用户已登录
            setTimeout(() => {
                const token = getAuthToken();
                if (token) {
                    updateFriendRequestStats();
                }
            }, 1000);
        });
        // 显示全局提示
        function showAlert(message, type = 'error') {
            const alertContainer = document.getElementById('alertContainer');
            if (!alertContainer) {
                console.error('alertContainer element not found');
                return;
            }
            alertContainer.innerHTML = `
                <div class="alert ${type}">
                    ${message}
                </div>
            `;
            const alertElement = alertContainer.querySelector('.alert');
            if (alertElement) {
                alertElement.style.display = 'block';
                // 3秒后自动隐藏
                setTimeout(() => {
                    alertElement.style.opacity = '0';
                    setTimeout(() => {
                        alertContainer.innerHTML = '';
                    }, 300);
                }, 3000);
            }
        }
    
        // 清除全局提示
        function clearAlert() {
            const alertContainer = document.getElementById('alertContainer');
            if (alertContainer) {
                alertContainer.innerHTML = '';
            }
        }
    
        // 好友请求模态框相关函数
        let friendRequestResolve = null;
        let currentFriendRequestData = null;
    
        function showFriendRequestModal(nickname) {
            return new Promise((resolve) => {
                friendRequestResolve = resolve;
                
                // 设置好友昵称
                document.getElementById('friendRequestNickname').textContent = nickname;
                
                // 设置默认验证消息
                const currentUserName = document.getElementById('meName').textContent;
                const defaultMessage = `我是 ${currentUserName}`;
                document.getElementById('verificationMessage').value = defaultMessage;
                
                // 更新字符计数
                updateCharCount();
                
                // 显示模态框
                const modal = document.getElementById('friendRequestModal');
                modal.style.display = 'flex';
                
                // 聚焦到文本框
                setTimeout(() => {
                    document.getElementById('verificationMessage').focus();
                }, 100);
            });
        }
    
        function closeFriendRequestModal() {
            const modal = document.getElementById('friendRequestModal');
            modal.style.display = 'none';
            
            // 清空输入框
            document.getElementById('verificationMessage').value = '';
            updateCharCount();
            
            // 返回null表示用户取消
            if (friendRequestResolve) {
                friendRequestResolve(null);
                friendRequestResolve = null;
            }
        }
    
        function confirmFriendRequest() {
            const message = document.getElementById('verificationMessage').value.trim();
            
            // 关闭模态框
            const modal = document.getElementById('friendRequestModal');
            modal.style.display = 'none';
            
            // 返回验证消息
            if (friendRequestResolve) {
                friendRequestResolve(message || '请求添加您为好友');
                friendRequestResolve = null;
            }
        }
    
        function updateCharCount() {
            const textarea = document.getElementById('verificationMessage');
            const charCount = document.getElementById('charCount');
            const currentLength = textarea.value.length;
            charCount.textContent = currentLength;
            
            // 根据字符数量改变颜色
            if (currentLength > 80) {
                charCount.style.color = '#ff6b35';
            } else if (currentLength > 60) {
                charCount.style.color = '#ffa500';
            } else {
                charCount.style.color = '#666';
            }
        }
    
        // 页面加载完成后绑定事件
        document.addEventListener('DOMContentLoaded', function() {
            // 绑定字符计数事件
            const verificationMessage = document.getElementById('verificationMessage');
            if (verificationMessage) {
                verificationMessage.addEventListener('input', updateCharCount);
                
                // 绑定键盘事件
                verificationMessage.addEventListener('keydown', function(e) {
                    // Ctrl+Enter 或 Cmd+Enter 发送请求
                    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                        e.preventDefault();
                        confirmFriendRequest();
                    }
                    // Escape 关闭模态框
                    if (e.key === 'Escape') {
                        e.preventDefault();
                        closeFriendRequestModal();
                    }
                });
            }
            
            // 点击模态框外部关闭
            const friendRequestModal = document.getElementById('friendRequestModal');
            if (friendRequestModal) {
                friendRequestModal.addEventListener('click', function(e) {
                    if (e.target === friendRequestModal) {
                        closeFriendRequestModal();
                    }
                });
            }
            
            // 删除好友模态框事件绑定
            const deleteContactModal = document.getElementById('deleteContactModal');
            if (deleteContactModal) {
                deleteContactModal.addEventListener('click', function(e) {
                    if (e.target === deleteContactModal) {
                        closeDeleteContactModal();
                    }
                });
                
                // 绑定键盘事件
                document.addEventListener('keydown', function(e) {
                    if (deleteContactModal.style.display === 'flex') {
                        // Escape 关闭模态框
                        if (e.key === 'Escape') {
                            e.preventDefault();
                            closeDeleteContactModal();
                        }
                        // Enter 确认删除
                        if (e.key === 'Enter') {
                            e.preventDefault();
                            confirmDeleteContactAction();
                        }
                    }
                });
            }
            
            // 标签管理相关事件绑定
            
            // 创建标签模态框事件
            const createTagModal = document.getElementById('createTagModal');
            if (createTagModal) {
                createTagModal.addEventListener('click', function(e) {
                    if (e.target === createTagModal) {
                        closeCreateTagModal();
                    }
                });
                
                // 标签颜色输入框变化事件
                const tagColorInput = document.getElementById('tagColorInput');
                if (tagColorInput) {
                    tagColorInput.addEventListener('input', function(e) {
                        updateColorPreview(e.target.value);
                    });
                }
                
                // 标签名称输入框回车事件
                const tagNameInput = document.getElementById('tagNameInput');
                if (tagNameInput) {
                    tagNameInput.addEventListener('keypress', function(e) {
                        if (e.key === 'Enter') {
                            createTag();
                        }
                    });
                }
            }
            
            // 编辑标签模态框事件
            const editTagModal = document.getElementById('editTagModal');
            if (editTagModal) {
                editTagModal.addEventListener('click', function(e) {
                    if (e.target === editTagModal) {
                        closeEditTagModal();
                    }
                });
                
                // 编辑标签颜色输入框变化事件
                const editTagColorInput = document.getElementById('editTagColorInput');
                if (editTagColorInput) {
                    editTagColorInput.addEventListener('input', function(e) {
                        updateEditColorPreview(e.target.value);
                    });
                }
                
                // 编辑标签名称输入框回车事件
                const editTagNameInput = document.getElementById('editTagNameInput');
                if (editTagNameInput) {
                    editTagNameInput.addEventListener('keypress', function(e) {
                        if (e.key === 'Enter') {
                            updateTag();
                        }
                    });
                }
            }
            
            // 分配标签模态框事件
            const assignTagModal = document.getElementById('assignTagModal');
            if (assignTagModal) {
                assignTagModal.addEventListener('click', function(e) {
                    if (e.target === assignTagModal) {
                        closeAssignTagModal();
                    }
                });
            }
            
            // 删除标签模态框事件
            const deleteTagModal = document.getElementById('deleteTagModal');
            if (deleteTagModal) {
                deleteTagModal.addEventListener('click', function(e) {
                    if (e.target === deleteTagModal) {
                        closeDeleteTagModal();
                    }
                });
                
                // 绑定键盘事件
                document.addEventListener('keydown', function(e) {
                    if (deleteTagModal.style.display === 'flex') {
                        // Escape 关闭模态框
                        if (e.key === 'Escape') {
                            e.preventDefault();
                            closeDeleteTagModal();
                        }
                        // Enter 确认删除
                        if (e.key === 'Enter') {
                            e.preventDefault();
                            confirmDeleteTagAction();
                        }
                    }
                });
            }
            
            // 添加好友到标签模态框事件
            const addContactToTagModal = document.getElementById('addContactToTagModal');
            if (addContactToTagModal) {
                addContactToTagModal.addEventListener('click', function(e) {
                    if (e.target === addContactToTagModal) {
                        closeAddContactToTagModal();
                    }
                });
                
                // 绑定键盘事件
                document.addEventListener('keydown', function(e) {
                    if (addContactToTagModal.classList.contains('show')) {
                        // Escape 关闭模态框
                        if (e.key === 'Escape') {
                            e.preventDefault();
                            closeAddContactToTagModal();
                        }
                    }
                });
            }
            
            // 标签搜索输入框事件
            const tagSearchInput = document.getElementById('tagSearchInput');
            if (tagSearchInput) {
                tagSearchInput.addEventListener('input', function() {
                    searchTags();
                });
            }
            
            // 添加好友到标签搜索输入框事件
            const contactSearchInput = document.getElementById('contactSearchInput');
            if (contactSearchInput) {
                contactSearchInput.addEventListener('input', filterContactsForTag);
            }
        });
        
        // 删除好友模态框相关函数
        let deleteContactData = null;
        
        function showDeleteContactModal(friendId, friendName) {
            deleteContactData = { friendId, friendName };
            
            // 设置好友名称
            document.getElementById('deleteContactName').textContent = friendName;
            
            // 显示模态框
            const modal = document.getElementById('deleteContactModal');
            modal.style.display = 'flex';
            
            // 聚焦到取消按钮（安全选择）
            setTimeout(() => {
                const cancelBtn = modal.querySelector('.delete-contact-btn.cancel');
                if (cancelBtn) {
                    cancelBtn.focus();
                }
            }, 100);
        }
        
        function closeDeleteContactModal() {
            const modal = document.getElementById('deleteContactModal');
            modal.style.display = 'none';
            deleteContactData = null;
        }
        
        function confirmDeleteContactAction() {
            if (deleteContactData) {
                const { friendId, friendName } = deleteContactData;
                closeDeleteContactModal();
                deleteContact(friendId, friendName);
            }
        }
    </script>