/**
 * 通用JavaScript函数库
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */

/**
 * 显示消息提示
 * @param {string} message - 消息内容
 * @param {string} type - 消息类型：success, error, warning, info
 * @param {number} duration - 显示时长（毫秒），默认3000
 */
function showMessage(message, type = 'info', duration = 3000) {
    // 创建消息容器（如果不存在）
    let messageContainer = document.getElementById('messageContainer');
    if (!messageContainer) {
        messageContainer = document.createElement('div');
        messageContainer.id = 'messageContainer';
        messageContainer.className = 'toast-container';
        document.body.appendChild(messageContainer);
    }
    
    // 获取图标
    const icons = {
        success: '✓',
        error: '✕',
        warning: '⚠',
        info: 'ℹ'
    };
    
    // 创建消息元素
    const messageElement = document.createElement('div');
    messageElement.className = `toast toast-${type}`;
    messageElement.innerHTML = `
        <div class="toast-icon">${icons[type] || icons.info}</div>
        <div class="toast-content">
            <div class="toast-message">${message}</div>
        </div>
        <button class="toast-close" onclick="removeToast(this.parentElement)">&times;</button>
    `;
    
    // 添加到容器
    messageContainer.appendChild(messageElement);
    
    // 触发动画
    setTimeout(() => {
        messageElement.classList.add('toast-show');
    }, 10);
    
    // 自动移除
    setTimeout(() => {
        removeToast(messageElement);
    }, duration);
}

/**
 * 移除提示框
 * @param {HTMLElement} toastElement - 提示框元素
 */
function removeToast(toastElement) {
    if (!toastElement || !toastElement.parentElement) return;
    
    toastElement.classList.add('toast-hide');
    setTimeout(() => {
        if (toastElement.parentElement) {
            toastElement.remove();
        }
    }, 300);
}



/**
 * 验证邮箱格式
 * @param {string} email - 邮箱地址
 * @returns {boolean} 是否为有效邮箱格式
 */
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

/**
 * 格式化日期
 * @param {string|Date} dateString - 日期字符串或Date对象
 * @returns {string} 格式化后的日期字符串
 */
function formatDate(dateString) {
    if (!dateString) return '未知';
    
    try {
        const date = new Date(dateString);
        return date.toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch (error) {
        return '格式错误';
    }
}



/**
 * 防抖函数
 * @param {Function} func - 要防抖的函数
 * @param {number} wait - 等待时间（毫秒）
 * @returns {Function} 防抖后的函数
 */
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * 节流函数
 * @param {Function} func - 要节流的函数
 * @param {number} limit - 时间间隔（毫秒）
 * @returns {Function} 节流后的函数
 */
function throttle(func, limit) {
    let inThrottle;
    return function() {
        const args = arguments;
        const context = this;
        if (!inThrottle) {
            func.apply(context, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

/**
 * 生成UUID
 * @returns {string} UUID字符串
 */
function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

/**
 * 复制文本到剪贴板
 * @param {string} text - 要复制的文本
 * @returns {Promise<boolean>} 是否复制成功
 */
async function copyToClipboard(text) {
    try {
        await navigator.clipboard.writeText(text);
        return true;
    } catch (err) {
        // 降级方案
        const textArea = document.createElement('textarea');
        textArea.value = text;
        document.body.appendChild(textArea);
        textArea.select();
        try {
            document.execCommand('copy');
            return true;
        } catch (err) {
            return false;
        } finally {
            document.body.removeChild(textArea);
        }
    }
}

/**
 * 格式化文件大小
 * @param {number} bytes - 字节数
 * @returns {string} 格式化后的文件大小
 */
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

/**
 * 获取文件扩展名
 * @param {string} filename - 文件名
 * @returns {string} 文件扩展名
 */
function getFileExtension(filename) {
    return filename.slice((filename.lastIndexOf('.') - 1 >>> 0) + 2);
}

/**
 * 转义HTML字符
 * @param {string} text - 要转义的文本
 * @returns {string} 转义后的文本
 */
function escapeHtml(text) {
    // 处理 undefined、null 或空值
    if (text === undefined || text === null) {
        return '';
    }
    
    // 确保转换为字符串
    text = String(text);
    
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

/**
 * 检查是否为移动设备
 * @returns {boolean} 是否为移动设备
 */
function isMobileDevice() {
    return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
}

/**
 * 获取URL参数
 * @param {string} name - 参数名
 * @returns {string|null} 参数值
 */
function getUrlParameter(name) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
}

/**
 * 设置URL参数
 * @param {string} name - 参数名
 * @param {string} value - 参数值
 */
function setUrlParameter(name, value) {
    const url = new URL(window.location);
    url.searchParams.set(name, value);
    window.history.pushState({}, '', url);
}

/**
 * 检查用户认证状态
 * 如果未登录，重定向到登录页面
 */
function checkAuth() {
    const token = localStorage.getItem('token') || localStorage.getItem('accessToken') || 
                  sessionStorage.getItem('token') || sessionStorage.getItem('accessToken');
    
    if (!token) {
        console.warn('用户未登录，重定向到登录页面');
        window.location.href = '/login';
        return false;
    }
    
    return true;
}

/**
 * 获取认证令牌
 * @returns {string|null} 认证令牌
 */
function getAuthToken() {
    return localStorage.getItem('token') || localStorage.getItem('accessToken') || 
           sessionStorage.getItem('token') || sessionStorage.getItem('accessToken');
}

/**
 * 获取认证请求头
 * @returns {Object} 包含Authorization头的对象
 */
function getAuthHeaders() {
    const token = getAuthToken();
    const headers = {};
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    return headers;
}

/**
 * 获取当前用户ID
 * @returns {Promise<number>} 当前用户ID
 */
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

/**
 * 清除登录信息
 */
function clearLoginInfo() {
    localStorage.removeItem('token');
    localStorage.removeItem('accessToken');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('accessToken');
    console.log('已清除登录信息');
}

// 页面加载完成后的通用初始化
document.addEventListener('DOMContentLoaded', function() {
    // 添加全局错误处理
    window.addEventListener('error', function(e) {
        console.error('全局错误:', e.error);
    });
    
    // 添加未处理的Promise拒绝处理
    window.addEventListener('unhandledrejection', function(e) {
        console.error('未处理的Promise拒绝:', e.reason);
    });
});

// 标签详情页面相关函数
let currentTagId = null;
let currentTagName = null;
let currentTagColor = null;

/**
 * 查看标签下的好友
 * @param {number} tagId - 标签ID
 * @param {string} tagName - 标签名称
 */
function viewTagContacts(tagId, tagName) {
    console.log('viewTagContacts() 开始执行，标签ID:', tagId, '标签名称:', tagName);
    
    currentTagId = tagId;
    currentTagName = tagName;
    
    // 隐藏标签页面，显示标签详情页面
    console.log('切换页面显示状态');
    const tagsTab = document.getElementById('tagsTab');
    const tagDetailsTab = document.getElementById('tagDetailsTab');
    
    if (tagsTab) {
        tagsTab.style.display = 'none';
        tagsTab.classList.remove('active');
        console.log('隐藏标签管理页面');
    }
    
    if (tagDetailsTab) {
        tagDetailsTab.style.display = 'block';
        tagDetailsTab.classList.add('active');
        console.log('显示标签详情页面并添加active类');
    }
    
    // 设置页面标题
    const titleElement = document.getElementById('tagDetailsTitle');
    if (titleElement) {
        titleElement.textContent = `${tagName} - 标签详情`;
        console.log('设置页面标题:', titleElement.textContent);
    }
    
    // 加载标签详情
    console.log('开始加载标签详情');
    loadTagDetails(tagId);
}

/**
 * 返回标签页面
 */
function backToTags() {
    const tagDetailsTab = document.getElementById('tagDetailsTab');
    const tagsTab = document.getElementById('tagsTab');
    
    if (tagDetailsTab) {
        tagDetailsTab.style.display = 'none';
        tagDetailsTab.classList.remove('active');
    }
    
    if (tagsTab) {
        tagsTab.style.display = 'block';
        tagsTab.classList.add('active');
    }
    
    // 清空当前标签信息
    currentTagId = null;
    currentTagName = null;
    currentTagColor = null;
}

/**
 * 刷新标签详情
 */
function refreshTagDetails() {
    console.log('refreshTagDetails() 被调用，currentTagId:', currentTagId);
    if (currentTagId) {
        console.log('开始重新加载标签详情');
        loadTagDetails(currentTagId);
    } else {
        console.error('currentTagId 为空，无法刷新标签详情');
    }
}

/**
 * 加载标签详情
 * @param {number} tagId - 标签ID
 */
async function loadTagDetails(tagId) {
    console.log('loadTagDetails() 开始执行，标签ID:', tagId);
    
    const token = getAuthToken();
    console.log('获取到的token:', token ? '存在' : '不存在');
    
    if (!token) {
        console.error('没有找到认证token');
        showMessage('请先登录', 'error');
        return;
    }
    
    try {
        // 显示加载状态
        console.log('显示加载状态');
        document.getElementById('tagContactsLoading').style.display = 'block';
        document.getElementById('tagContactsList').style.display = 'none';
        document.getElementById('noTagContacts').style.display = 'none';
        
        // 先获取当前用户ID
        console.log('获取当前用户ID');
        const currentUserId = await getCurrentUserId();
        console.log('当前用户ID:', currentUserId);
        
        // 获取标签信息
        console.log('开始获取标签信息');
        const tagResponse = await fetch(`/api/contact-tags/${tagId}?userId=${currentUserId}`, {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            }
        });
        
        console.log('标签信息API响应状态:', tagResponse.status);
        
        if (tagResponse.status === 401) {
            console.error('认证失败，清除登录信息');
            clearLoginInfo();
            window.location.href = '/login';
            return;
        }
        
        if (!tagResponse.ok) {
            throw new Error('获取标签信息失败');
        }
        
        const tagData = await tagResponse.json();
        console.log('标签信息API响应数据:', tagData);
        
        // 处理API响应数据格式
        let tagInfo = tagData;
        if (tagData.data) {
            tagInfo = tagData.data;
        }
        
        console.log('处理后的标签信息:', tagInfo);
        
        // 更新标签信息显示
        const tagName = tagInfo.name || tagInfo.tagName || '未知标签';
        const tagColor = tagInfo.color || tagInfo.tagColor || '#667eea';
        
        document.getElementById('tagDetailsName').textContent = tagName;
        document.getElementById('tagDetailsColor').style.backgroundColor = tagColor;
        
        // 更新当前标签信息
        currentTagName = tagName;
        currentTagColor = tagColor;
        
        // 获取标签下的好友详情
        console.log('开始获取标签下的好友详情');
        const contactsResponse = await fetch(`/api/v1/contact-tag-assignments/tag-contact-details?userId=${currentUserId}&tagId=${tagId}`, {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            }
        });
        
        console.log('好友详情API响应状态:', contactsResponse.status);
        
        if (!contactsResponse.ok) {
            throw new Error('获取好友列表失败');
        }
        
        const contactsData = await contactsResponse.json();
        console.log('好友详情API响应数据:', contactsData);
        
        // 隐藏加载状态
        console.log('隐藏加载状态');
        document.getElementById('tagContactsLoading').style.display = 'none';
        
        // 显示好友列表并更新数量
        displayTagContacts(contactsData);
        
        // 强制刷新页面显示
        const tagContactsList = document.getElementById('tagContactsList');
        if (tagContactsList) {
            tagContactsList.style.display = 'block';
        }
        
        // 从displayTagContacts处理后的数据更新好友数量
        // 这里需要重新计算，因为displayTagContacts会处理数据格式
        let actualContactCount = 0;
        if (contactsData) {
            if (Array.isArray(contactsData)) {
                actualContactCount = contactsData.length;
            } else if (contactsData.data && Array.isArray(contactsData.data)) {
                actualContactCount = contactsData.data.length;
            } else if (typeof contactsData === 'object') {
                actualContactCount = Object.values(contactsData).filter(item => item && typeof item === 'object').length;
            }
        }
        document.getElementById('tagDetailsCount').textContent = `${actualContactCount} 位好友`;
        console.log('更新好友数量:', actualContactCount);
        
    } catch (error) {
        console.error('加载标签详情失败:', error);
        showMessage('加载标签详情失败: ' + error.message, 'error');
        
        // 隐藏加载状态
        document.getElementById('tagContactsLoading').style.display = 'none';
        document.getElementById('noTagContacts').style.display = 'block';
    }
}

/**
 * 显示标签下的好友列表
 * @param {Array} contacts - 好友列表
 */
function displayTagContacts(contacts) {
    console.log('displayTagContacts() 开始执行，原始数据:', contacts);
    
    const contactsList = document.getElementById('tagContactsList');
    const noContacts = document.getElementById('noTagContacts');
    
    if (!contactsList) {
        console.error('找不到 tagContactsList 元素');
        return;
    }
    console.log('找到 tagContactsList 元素');
    
    // 处理API响应数据格式
    let contactsArray = [];
    if (contacts) {
        if (Array.isArray(contacts)) {
            contactsArray = contacts;
        } else if (contacts.data && Array.isArray(contacts.data)) {
            contactsArray = contacts.data;
        } else if (typeof contacts === 'object') {
            // 如果是对象，尝试转换为数组
            contactsArray = Object.values(contacts).filter(item => item && typeof item === 'object');
        }
    }
    
    console.log('处理后的好友数组:', contactsArray, '数量:', contactsArray.length);
    
    if (!contactsArray || contactsArray.length === 0) {
        console.log('没有好友数据，显示空状态');
        contactsList.style.display = 'none';
        noContacts.style.display = 'block';
        console.log('空状态显示完成');
        return;
    }
    
    console.log('开始渲染好友列表');
    contactsList.style.display = 'block';
    noContacts.style.display = 'none';
    
    const htmlContent = contactsArray.map(contact => {
        console.log('渲染好友:', contact);
        
        // 处理好友信息字段 - 优先显示备注，没有备注再显示昵称
        const friendName = contact.nickname || contact.friendNickname || contact.friendUsername || '未知用户';
        const friendAlias = contact.alias || contact.friendAlias || '';
        const displayName = friendAlias || friendName;
        const avatarUrl = contact.avatarUrl || contact.friendAvatarUrl;
        const friendId = contact.friendId || contact.id;
        
        // 生成头像HTML
        let avatarHtml;
        if (avatarUrl && avatarUrl.trim() !== '') {
            avatarHtml = `<img src="${escapeHtml(avatarUrl)}" alt="${escapeHtml(displayName)}" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';" style="width: 100%; height: 100%; object-fit: cover; border-radius: 50%;"/>
                      <div style="display: none; width: 100%; height: 100%; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; font-weight: 600; font-size: 16px; align-items: center; justify-content: center; border-radius: 50%;">${displayName.charAt(0).toUpperCase()}</div>`;
        } else {
            avatarHtml = `<div style="width: 100%; height: 100%; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; font-weight: 600; font-size: 16px; display: flex; align-items: center; justify-content: center; border-radius: 50%;">${displayName.charAt(0).toUpperCase()}</div>`;
        }
        
        return `
            <div class="tag-contact-item">
                <div class="tag-contact-avatar">${avatarHtml}</div>
                <div class="tag-contact-info">
                    <div class="tag-contact-name">${escapeHtml(displayName)}</div>
                </div>
                <div class="tag-contact-actions">
                    <button class="contact-action-btn" onclick="openContactProfile(${friendId})" title="查看资料">👤</button>
                    <button class="contact-action-btn remove" onclick="removeContactFromTag(${friendId}, '${escapeHtml(displayName)}')" title="移除标签">🏷️</button>
                    <button class="contact-action-btn delete" onclick="confirmDeleteContact(${friendId}, '${escapeHtml(displayName)}')" title="删除好友">🗑️</button>
                </div>
            </div>
        `;
    }).join('');
    
    // 设置HTML内容
    contactsList.innerHTML = htmlContent;
    console.log('好友列表渲染完成，HTML内容长度:', htmlContent.length);
    console.log('contactsList元素样式:', window.getComputedStyle(contactsList));
    console.log('标签详情页面是否可见:', document.getElementById('tagDetailsTab').style.display);
    console.log('contactsList的父元素:', contactsList.parentElement);
    console.log('contactsList的innerHTML:', contactsList.innerHTML.substring(0, 200));
    
    // 强制重新渲染
    contactsList.offsetHeight;
    
    // 检查元素是否真的可见
    const rect = contactsList.getBoundingClientRect();
    console.log('contactsList位置信息:', rect);
    console.log('contactsList是否在视口内:', rect.top >= 0 && rect.left >= 0 && rect.bottom <= window.innerHeight && rect.right <= window.innerWidth);
}

/**
 * 从标签中移除好友
 * @param {number} contactId - 好友ID
 * @param {string} contactName - 好友名称
 */
function removeContactFromTag(contactId, contactName) {
    // 使用美化的模态框替代简单的confirm对话框
    if (typeof showRemoveTagModal === 'function') {
        showRemoveTagModal(contactId, contactName, currentTagName);
    } else {
        // 降级处理：如果模态框函数不存在，使用原来的confirm
        if (!confirm(`确定要从标签"${currentTagName}"中移除好友"${contactName}"吗？`)) {
            return;
        }
        removeContactFromTagAction(contactId);
    }
}

/**
 * 执行从标签中移除好友的操作
 * @param {number} contactId - 好友ID
 */
async function removeContactFromTagAction(contactId) {
    try {
        const currentUserId = await getCurrentUserId();
        const response = await fetch(`/api/v1/contact-tag-assignments?userId=${currentUserId}&friendId=${contactId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                ...getAuthHeaders()
            },
            body: JSON.stringify([currentTagId])
        });
        
        if (!response.ok) {
            throw new Error('移除好友失败');
        }
        
        showMessage('已成功移除好友', 'success');
        
        // 立即清空当前显示的好友列表，避免显示过期数据
        const contactsList = document.getElementById('tagContactsList');
        if (contactsList) {
            contactsList.innerHTML = '';
            contactsList.style.display = 'none';
        }
        
        // 显示加载状态
        const loadingElement = document.getElementById('tagContactsLoading');
        if (loadingElement) {
            loadingElement.style.display = 'block';
        }
        
        // 添加延迟确保数据库操作完成后再刷新
        setTimeout(() => {
            console.log('延迟刷新标签详情开始执行');
            refreshTagDetails();
        }, 1500);
        
    } catch (error) {
        console.error('移除好友失败:', error);
        showMessage('移除好友失败: ' + error.message, 'error');
    }
}

/**
 * 从标签详情页面编辑标签
 */
function editTagFromDetails() {
    if (currentTagId && currentTagName && currentTagColor) {
        editTag(currentTagId, currentTagName, currentTagColor);
    } else {
        showMessage('标签信息不完整，无法编辑', 'error');
    }
}

// 存储选中的好友ID列表
let selectedContactIds = [];
// 存储所有好友列表
let allContacts = [];
// 存储当前标签下已有的好友ID列表
let currentTagContactIds = [];

/**
 * 显示添加好友到标签的模态框
 */
async function showAddContactToTagModal() {
    if (!currentTagId || !currentTagName || !currentTagColor) {
        showMessage('标签信息不完整，无法添加好友', 'error');
        return;
    }
    
    const modal = document.getElementById('addContactToTagModal');
    modal.classList.add('show');
    
    // 设置标签信息
    document.getElementById('addContactTagName').textContent = currentTagName;
    document.getElementById('addContactTagColor').style.backgroundColor = currentTagColor;
    
    // 清空搜索框和选中状态
    document.getElementById('contactSearchInput').value = '';
    selectedContactIds = [];
    updateAddContactsButton();
    
    // 加载好友列表
    await loadAvailableContacts();
}

/**
 * 关闭添加好友到标签的模态框
 */
function closeAddContactToTagModal() {
    const modal = document.getElementById('addContactToTagModal');
    modal.classList.remove('show');
    
    // 清空数据（但保留currentTagContactIds，因为刷新时可能需要）
    selectedContactIds = [];
    allContacts = [];
    // 不清空 currentTagContactIds，让它在下次打开模态框时重新加载
}

/**
 * 加载可用的好友列表
 */
async function loadAvailableContacts() {
    const contactsLoading = document.getElementById('contactsLoading');
    const availableContacts = document.getElementById('availableContacts');
    const noAvailableContacts = document.getElementById('noAvailableContacts');
    
    try {
        // 显示加载状态
        contactsLoading.style.display = 'block';
        availableContacts.style.display = 'none';
        noAvailableContacts.style.display = 'none';
        
        const token = getAuthToken();
        if (!token) {
            throw new Error('请先登录');
        }
        
        const currentUserId = await getCurrentUserId();
        
        // 并行获取所有好友和当前标签下的好友
        const [allContactsResponse, tagContactsResponse] = await Promise.all([
            fetch(`/api/contacts?userId=${currentUserId}&includeBlocked=false`, {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + token,
                    'Content-Type': 'application/json'
                }
            }),
            fetch(`/api/v1/contact-tag-assignments/tag-contact-details?userId=${currentUserId}&tagId=${currentTagId}`, {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + token,
                    'Content-Type': 'application/json'
                }
            })
        ]);
        
        if (!allContactsResponse.ok) {
            throw new Error('获取好友列表失败');
        }
        
        if (!tagContactsResponse.ok) {
            throw new Error('获取标签好友失败');
        }
        
        const allContactsData = await allContactsResponse.json();
        const tagContactsData = await tagContactsResponse.json();
        
        // 处理数据格式
        allContacts = Array.isArray(allContactsData) ? allContactsData : 
                     (allContactsData.data && Array.isArray(allContactsData.data)) ? allContactsData.data : [];
        
        // 处理标签好友数据，从ContactResponse对象中提取friendId
        const tagContactsArray = Array.isArray(tagContactsData) ? tagContactsData : 
                                (tagContactsData.data && Array.isArray(tagContactsData.data)) ? tagContactsData.data : [];
        currentTagContactIds = tagContactsArray.map(contact => contact.friendId || contact.id);
        
        // 过滤出未添加到当前标签的好友
        const availableContactsList = allContacts.filter(contact => 
            !currentTagContactIds.includes(contact.friendId || contact.id)
        );
        
        // 隐藏加载状态
        contactsLoading.style.display = 'none';
        
        if (availableContactsList.length === 0) {
            noAvailableContacts.style.display = 'block';
            availableContacts.style.display = 'none';
        } else {
            displayAvailableContacts(availableContactsList);
            availableContacts.style.display = 'block';
            noAvailableContacts.style.display = 'none';
        }
        
    } catch (error) {
        console.error('加载好友列表失败:', error);
        showMessage('加载好友列表失败: ' + error.message, 'error');
        
        contactsLoading.style.display = 'none';
        noAvailableContacts.style.display = 'block';
        availableContacts.style.display = 'none';
    }
}

/**
 * 显示可用的好友列表
 * @param {Array} contacts - 好友列表
 */
function displayAvailableContacts(contacts) {
    const availableContacts = document.getElementById('availableContacts');
    
    if (!contacts || contacts.length === 0) {
        availableContacts.innerHTML = '<p>没有可添加的好友</p>';
        return;
    }
    
    availableContacts.innerHTML = contacts.map(contact => {
        const friendName = contact.nickname || contact.friendNickname || contact.friendUsername || '未知用户';
        const friendAlias = contact.alias || contact.friendAlias || '';
        const displayName = friendAlias || friendName;
        const avatarUrl = contact.avatarUrl || contact.friendAvatarUrl;
        const friendId = contact.friendId || contact.id;
        
        // 生成头像HTML - 使用方形头像
        let avatarHtml;
        if (avatarUrl && avatarUrl.trim() !== '') {
            avatarHtml = `<img src="${escapeHtml(avatarUrl)}" alt="${escapeHtml(displayName)}" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';" style="width: 100%; height: 100%; object-fit: cover; border-radius: 8px;"/>
                      <div style="display: none; width: 100%; height: 100%; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; font-weight: 600; font-size: 14px; align-items: center; justify-content: center; border-radius: 8px;">${displayName.charAt(0).toUpperCase()}</div>`;
        } else {
            avatarHtml = `<div style="width: 100%; height: 100%; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; font-weight: 600; font-size: 14px; display: flex; align-items: center; justify-content: center; border-radius: 8px;">${displayName.charAt(0).toUpperCase()}</div>`;
        }
        
        return `
            <div class="contact-item" data-contact-id="${friendId}" onclick="toggleContactSelection(${friendId})">
                <div class="contact-checkbox">
                    <input type="checkbox" id="contact_${friendId}" onchange="handleContactCheckboxChange(${friendId}, this.checked)">
                </div>
                <div class="contact-avatar">${avatarHtml}</div>
                <div class="contact-info">
                    <div class="contact-name">${escapeHtml(displayName)}</div>
                </div>
            </div>
        `;
    }).join('');
}

/**
 * 切换好友选择状态
 * @param {number} contactId - 好友ID
 */
function toggleContactSelection(contactId) {
    const checkbox = document.getElementById(`contact_${contactId}`);
    if (checkbox) {
        checkbox.checked = !checkbox.checked;
        handleContactCheckboxChange(contactId, checkbox.checked);
    }
}

/**
 * 处理好友复选框状态变化
 * @param {number} contactId - 好友ID
 * @param {boolean} checked - 是否选中
 */
function handleContactCheckboxChange(contactId, checked) {
    if (checked) {
        if (!selectedContactIds.includes(contactId)) {
            selectedContactIds.push(contactId);
        }
    } else {
        const index = selectedContactIds.indexOf(contactId);
        if (index > -1) {
            selectedContactIds.splice(index, 1);
        }
    }
    
    updateAddContactsButton();
}

/**
 * 更新添加好友按钮状态
 */
function updateAddContactsButton() {
    const addContactsBtn = document.getElementById('addContactsBtn');
    const count = selectedContactIds.length;
    
    if (count > 0) {
        addContactsBtn.disabled = false;
        addContactsBtn.textContent = `添加选中好友 (${count})`;
    } else {
        addContactsBtn.disabled = true;
        addContactsBtn.textContent = '添加选中好友';
    }
}

/**
 * 过滤好友列表（搜索功能）
 */
function filterContactsForTag() {
    const searchInput = document.getElementById('contactSearchInput');
    const searchTerm = searchInput.value.toLowerCase().trim();
    
    if (!allContacts || allContacts.length === 0) {
        return;
    }
    
    // 过滤出未添加到当前标签的好友
    let availableContactsList = allContacts.filter(contact => 
        !currentTagContactIds.includes(contact.friendId || contact.id)
    );
    
    // 根据搜索词过滤
    if (searchTerm) {
        availableContactsList = availableContactsList.filter(contact => {
            const friendName = contact.nickname || contact.friendNickname || contact.friendUsername || '';
            const friendRemark = contact.remark || contact.friendRemark || '';
            const displayName = friendRemark || friendName;
            
            return displayName.toLowerCase().includes(searchTerm) ||
                   friendName.toLowerCase().includes(searchTerm) ||
                   friendRemark.toLowerCase().includes(searchTerm);
        });
    }
    
    displayAvailableContacts(availableContactsList);
    
    // 恢复选中状态
    selectedContactIds.forEach(contactId => {
        const checkbox = document.getElementById(`contact_${contactId}`);
        if (checkbox) {
            checkbox.checked = true;
        }
    });
}

/**
 * 确认添加好友到标签
 */
async function confirmAddContactsToTag() {
    if (selectedContactIds.length === 0) {
        showMessage('请选择要添加的好友', 'warning');
        return;
    }
    
    try {
        const token = getAuthToken();
        if (!token) {
            throw new Error('请先登录');
        }
        
        const currentUserId = await getCurrentUserId();
        
        // 为每个选中的好友添加标签
        const promises = selectedContactIds.map(contactId => 
            fetch('/api/v1/contact-tag-assignments', {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + token,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userId: currentUserId,
                    friendId: contactId,
                    tagIds: [currentTagId]
                })
            })
        );
        
        const results = await Promise.all(promises);
        
        // 检查是否所有请求都成功
        const failedCount = results.filter(response => !response.ok).length;
        const successCount = results.length - failedCount;
        
        if (successCount > 0) {
            showMessage(`成功添加 ${successCount} 位好友到标签`, 'success');
            
            // 关闭模态框
            closeAddContactToTagModal();
            
            // 添加延迟确保数据库操作完成后再刷新
            setTimeout(() => {
                console.log('延迟刷新开始执行');
                refreshTagDetails();
            }, 1000);
        }
        
        if (failedCount > 0) {
            showMessage(`${failedCount} 位好友添加失败`, 'warning');
        }
        
    } catch (error) {
        console.error('添加好友到标签失败:', error);
        showMessage('添加好友到标签失败: ' + error.message, 'error');
    }
}

/**
 * 查看用户资料（基于搜索结果的实现逻辑）
 * @param {string} userId - 用户ID字符串
 */
async function openContactProfile(userId) {
    const token = getAuthToken();
    if (!token) {
        alert('请先登录');
        return;
    }
    
    try {
        const response = await fetch(`/api/user/public-profile/${userId}`, {
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
                window.location.href = '/login';
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

/**
 * 显示用户资料模态框
 * @param {Object} profile - 用户资料对象
 */
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

/**
 * 获取性别文本
 * @param {string} gender - 性别值
 * @returns {string} 格式化的性别文本
 */
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

/**
 * 关闭用户资料模态框
 */
function closeUserProfileModal() {
    const modal = document.getElementById('userProfileModal');
    if (modal) {
        modal.style.display = 'none';
        modal.classList.remove('show');
    }
}