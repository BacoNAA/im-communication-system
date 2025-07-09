/**
 * 仪表板页面JavaScript
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    initDashboard();
});

/**
 * 初始化仪表板
 */
function initDashboard() {
    // 绑定导航事件
    bindNavigationEvents();
    
    // 初始化默认标签页
    showTab('chat');
}

/**
 * 绑定导航事件
 */
function bindNavigationEvents() {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', function() {
            const tab = this.getAttribute('data-tab');
            showTab(tab);
            
            // 更新导航状态
            navItems.forEach(nav => nav.classList.remove('active'));
            this.classList.add('active');
        });
    });
}

/**
 * 显示指定标签页
 */
function showTab(tabName) {
    // 隐藏所有面板
    const panels = document.querySelectorAll('.content-panel');
    panels.forEach(panel => {
        panel.style.display = 'none';
    });
    
    // 显示指定面板
    const targetPanel = document.getElementById(tabName + 'Panel');
    if (targetPanel) {
        targetPanel.style.display = 'block';
    }
}



/**
 * 显示设置
 */
function showSettings() {
    showMessage('设置功能正在开发中...', 'info');
}

/**
 * 开始新聊天
 */
function startNewChat() {
    showMessage('新建会话功能正在开发中...', 'info');
}

/**
 * 添加联系人
 */
function addContact() {
    showMessage('添加联系人功能正在开发中...', 'info');
}

/**
 * 发布动态
 */
function publishMoment() {
    showMessage('发布动态功能正在开发中...', 'info');
}

/**
 * 编辑个人资料
 */
function editProfile() {
    window.location.href = 'profile.html';
}

/**
 * 更换头像
 */
function changeAvatar() {
    showMessage('更换头像功能正在开发中...', 'info');
}

/**
 * 生成二维码名片
 */
function generateQRCode() {
    showMessage('生成二维码名片功能正在开发中...', 'info');
}

/**
 * 自定义主页
 */
function customizeHomepage() {
    showMessage('自定义主页功能正在开发中...', 'info');
}

/**
 * 设备管理
 */
function manageDevices() {
    showMessage('设备管理功能正在开发中...', 'info');
}



/**
 * 关闭聊天窗口
 */
function closeChatWindow() {
    const chatWindow = document.getElementById('chatWindow');
    if (chatWindow) {
        chatWindow.style.display = 'none';
    }
}

/**
 * 发送表情
 */
function sendEmoji() {
    showMessage('发送表情功能正在开发中...', 'info');
}

/**
 * 发送文件
 */
function sendFile() {
    showMessage('发送文件功能正在开发中...', 'info');
}

/**
 * 发送图片
 */
function sendImage() {
    showMessage('发送图片功能正在开发中...', 'info');
}

/**
 * 发送消息
 */
function sendMessage() {
    const messageInput = document.getElementById('messageInput');
    const message = messageInput.value.trim();
    
    if (!message) {
        showMessage('请输入消息内容', 'warning');
        return;
    }
    
    showMessage('发送消息功能正在开发中...', 'info');
    messageInput.value = '';
}

// formatDate 函数已在 common.js 中定义