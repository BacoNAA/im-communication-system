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
    window.location.href = 'index.html#profile';
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
    // 显示加载状态
    showMessage('正在生成二维码名片...', 'info');
    
    // 获取JWT token
    const token = localStorage.getItem('token');
    if (!token) {
        showMessage('请先登录', 'error');
        return;
    }
    
    // 调用后端API生成二维码
    fetch('/api/user/qrcode', {
        method: 'GET',
        headers: {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        // 处理401未授权错误
        if (response.status === 401) {
            showMessage('登录已过期，请重新登录', 'error');
            // 清除登录信息
            localStorage.removeItem('token');
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('userInfo');
            sessionStorage.removeItem('accessToken');
            sessionStorage.removeItem('refreshToken');
            sessionStorage.removeItem('userInfo');
            setTimeout(() => {
                window.location.href = '/login.html';
            }, 2000);
            return Promise.reject(new Error('登录已过期'));
        }
        return response.json();
    })
    .then(data => {
        if (data.success && data.data) {
            // 显示二维码
            showQRCodeModal(data.data);
        } else {
            showMessage(data.message || '生成二维码失败', 'error');
        }
    })
    .catch(error => {
        console.error('生成二维码失败:', error);
        showMessage('生成二维码失败，请稍后重试', 'error');
    });
}

/**
 * 显示二维码模态框
 */
function showQRCodeModal(qrCodeData) {
    // 创建模态框HTML
    const modalHtml = `
        <div id="qrCodeModal" class="modal" style="display: block;">
            <div class="modal-content" style="max-width: 400px; text-align: center;">
                <div class="modal-header">
                    <h3>我的二维码名片</h3>
                    <span class="close" onclick="closeQRCodeModal()">&times;</span>
                </div>
                <div class="modal-body">
                    <div class="qr-code-container" style="margin: 20px 0;">
                        <img src="data:image/png;base64,${qrCodeData.qrCodeBase64}" 
                             alt="二维码名片" 
                             style="width: 200px; height: 200px; border: 1px solid #ddd;">
                    </div>
                    <div class="user-info" style="margin: 15px 0;">
                        <p><strong>${qrCodeData.userNickname}</strong></p>
                        <p style="color: #666; font-size: 12px;">扫描二维码，添加我为好友</p>
                    </div>
                    <div class="qr-actions" style="margin: 20px 0;">
                        <button onclick="downloadQRCode('${qrCodeData.qrCodeBase64}', '${qrCodeData.userNickname}')" 
                                class="btn btn-primary" style="margin: 5px;">下载二维码</button>
                        <button onclick="shareQRCode()" 
                                class="btn btn-secondary" style="margin: 5px;">分享</button>
                    </div>
                    <div class="qr-info" style="color: #999; font-size: 11px; margin-top: 15px;">
                        <p>生成时间: ${formatDate(new Date(qrCodeData.generatedAt))}</p>
                        <p>有效期至: ${formatDate(new Date(qrCodeData.expiresAt))}</p>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    // 添加到页面
    document.body.insertAdjacentHTML('beforeend', modalHtml);
}

/**
 * 关闭二维码模态框
 */
function closeQRCodeModal() {
    const modal = document.getElementById('qrCodeModal');
    if (modal) {
        modal.remove();
    }
}

/**
 * 下载二维码
 */
function downloadQRCode(base64Data, nickname) {
    try {
        // 创建下载链接
        const link = document.createElement('a');
        link.href = 'data:image/png;base64,' + base64Data;
        link.download = `${nickname}_二维码名片.png`;
        
        // 触发下载
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        showMessage('二维码已下载', 'success');
    } catch (error) {
        console.error('下载失败:', error);
        showMessage('下载失败，请稍后重试', 'error');
    }
}

/**
 * 分享二维码
 */
function shareQRCode() {
    if (navigator.share) {
        navigator.share({
            title: '我的二维码名片',
            text: '扫描二维码，添加我为好友'
        }).catch(error => {
            console.log('分享失败:', error);
            showMessage('分享功能暂不可用', 'info');
        });
    } else {
        showMessage('分享功能暂不可用', 'info');
    }
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