<template>
  <div class="contacts-list-container">
    <div class="search-bar">
      <input 
        v-model="searchKeyword"
        type="text" 
        placeholder="搜索联系人..." 
        class="search-input"
        @input="handleSearch"
      />
    </div>
    
    <div class="contacts-list">
      <div v-if="loading && contacts.length === 0" class="contacts-loading">
        加载中...
      </div>
      <div v-else-if="filteredContacts.length === 0 && !loading" class="no-contacts">
        {{ searchKeyword ? '未找到匹配的联系人' : '暂无联系人' }}
      </div>
      
      <contact-item
        v-for="contact in filteredContacts" 
        :key="contact.id"
        :contact="contact"
        :current-user-id="currentUserId"
        @click="handleContactClick"
        @context-menu="showContextMenu($event, contact)"
        @start-chat="handleStartChat"
        @chat-error="handleChatError"
      />
    </div>

    <!-- 上下文菜单 -->
    <div 
      v-if="showMenu" 
      class="context-menu"
      :style="{ top: menuPos.y + 'px', left: menuPos.x + 'px' }"
      @click.stop
    >
      <div class="menu-item" @click="handleStartChatFromMenu">
        开始聊天
      </div>
      <div class="menu-item" @click="handleEditAlias">
        修改备注
      </div>
      <div class="menu-item" @click="handleManageTags">
        管理标签
      </div>
      <div v-if="selectedContact && !isContactBlocked(selectedContact)" 
           class="menu-item warning" 
           @click="handleBlockContact">
        拉黑联系人
      </div>
      <div v-else-if="selectedContact && isContactBlocked(selectedContact)" 
           class="menu-item success" 
           @click="handleUnblockContact">
        解除拉黑
      </div>
      <div class="menu-item report" @click="handleReportContact">
        举报联系人
      </div>
      <div class="menu-item delete" @click="handleDeleteContact">
        删除联系人
      </div>
    </div>

    <!-- 举报对话框 -->
    <div v-if="showReportDialog" class="report-dialog-overlay" @click.self="cancelReport">
      <div class="report-dialog">
        <div class="report-dialog-header">
          <h3>举报联系人</h3>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue';
import ContactItem from './ContactItem.vue';
import { messageApi } from '@/api/message';
import { contactApi } from '@/api/contact';
import { reportApi } from '@/api/report';
import { ElMessage } from 'element-plus';

// 定义props
const props = defineProps({
  currentUserId: {
    type: Number,
    required: true
  }
});

// 定义事件
const emit = defineEmits([
  'select-contact', 
  'start-chat', 
  'edit-alias', 
  'manage-tags', 
  'delete-contact',
  'block-contact',
  'unblock-contact',
  'error'
]);

// 联系人数据
const contacts = ref<any[]>([]);
const loading = ref(false);
const searchKeyword = ref('');
const error = ref<string | null>(null);

// 上下文菜单相关
const showMenu = ref(false);
const menuPos = ref({ x: 0, y: 0 });
const selectedContact = ref<any>(null);

// 举报对话框相关
const showReportDialog = ref(false);
const reportReason = ref('');
const reportDescription = ref('');

// 计算属性：筛选联系人
const filteredContacts = computed(() => {
  if (!searchKeyword.value) {
    return contacts.value;
  }
  
  const keyword = searchKeyword.value.toLowerCase();
  return contacts.value.filter(contact => 
    contact.name.toLowerCase().includes(keyword) || 
    (contact.signature && contact.signature.toLowerCase().includes(keyword))
  );
});

// 加载联系人列表
const loadContacts = async () => {
  if (!props.currentUserId) {
    error.value = '用户未登录';
    return;
  }
  
  try {
    loading.value = true;
    error.value = null;
    
    // includeBlocked设置为true，以便加载被拉黑的联系人
    const response = await contactApi.getContacts(props.currentUserId, true);
    
    if (response.success && response.data) {
      console.log('从API获取的原始联系人数据:', response.data);
      
      contacts.value = response.data.map((contact: any) => {
        // 确保ID是有效的数字
        let contactId = contact.id;
        if (typeof contactId === 'string') {
          contactId = parseInt(contactId);
          if (isNaN(contactId)) {
            console.warn('联系人ID无效(字符串):', contact);
            contactId = 0;
          }
        } else if (contactId === undefined || contactId === null) {
          // 尝试从friendId获取
          contactId = contact.friendId;
          if (typeof contactId === 'string') {
            contactId = parseInt(contactId);
          }
          if (isNaN(contactId) || contactId === undefined || contactId === null) {
            console.warn('联系人ID无效(undefined/null):', contact);
            contactId = 0;
          }
        } else if (typeof contactId !== 'number') {
          console.warn('联系人ID类型不是数字:', typeof contactId, contact);
          contactId = 0;
        }
        
        // 确保ID是有效的数字
        contactId = Number(contactId);
        
        // 确定拉黑状态
        // 可能的字段：isBlocked, is_blocked, status
        let isBlocked = false;
        
        if (typeof contact.isBlocked === 'boolean') {
          isBlocked = contact.isBlocked;
        } else if (typeof contact.is_blocked === 'boolean') {
          isBlocked = contact.is_blocked;
        } else if (contact.status === 'BLOCKED') {
          isBlocked = true;
        }
        
        console.log('处理联系人拉黑状态:', {
          id: contactId,
          name: contact.alias || contact.nickname || contact.email || `用户${contactId}`,
          原始isBlocked: contact.isBlocked,
          原始is_blocked: contact.is_blocked,
          原始status: contact.status,
          最终拉黑状态: isBlocked
        });
        
        return {
          id: contactId, // 确保ID是数字
          friendId: contactId, // 添加friendId字段，确保是数字
          name: contact.alias || contact.nickname || contact.email || `用户${contactId}`,
          signature: contact.signature || '',
          avatarUrl: contact.avatarUrl || '',
          online: contact.online || false,
          alias: contact.alias,
          email: contact.email,
          isBlocked: isBlocked, // 明确设置拉黑状态
          status: contact.status, // 保留原始状态
          rawData: contact
        };
      }).filter((contact: any) => contact.id > 0); // 过滤掉无效的联系人
      
      // 打印联系人ID类型，用于调试
      if (contacts.value.length > 0) {
        console.log('联系人列表加载完成，ID示例:');
        contacts.value.slice(0, 3).forEach(contact => {
          console.log(`联系人ID: ${contact.id}, 类型: ${typeof contact.id}, 名称: ${contact.name}, 状态: ${contact.status}, 是否拉黑: ${contact.isBlocked}`);
        });
      } else {
        console.log('联系人列表为空');
      }
    } else {
      throw new Error(response.message || '获取联系人列表失败');
    }
  } catch (err: any) {
    error.value = err.message || '获取联系人列表失败';
    emit('error', error.value);
  } finally {
    loading.value = false;
  }
};

// 处理搜索
const handleSearch = () => {
  // 前端搜索已通过计算属性实现
};

// 处理联系人点击
const handleContactClick = (contact: any) => {
  emit('select-contact', contact);
};

// 处理开始聊天
const handleStartChat = (data: { contact: any, conversationId: number }) => {
  emit('start-chat', data);
};

// 处理聊天错误
const handleChatError = (errorMessage: string) => {
  error.value = errorMessage;
  emit('error', errorMessage);
};

// 显示上下文菜单
const showContextMenu = (event: MouseEvent, contact: any) => {
  event.preventDefault();
  selectedContact.value = contact;
  menuPos.value = {
    x: event.clientX,
    y: event.clientY
  };
  showMenu.value = true;
  
  // 点击其他地方关闭菜单
  nextTick(() => {
    document.addEventListener('click', closeMenu, { once: true });
  });
};

// 关闭上下文菜单
const closeMenu = () => {
  showMenu.value = false;
};

// 从菜单开始聊天
const handleStartChatFromMenu = async () => {
  if (!selectedContact.value) return;
  
  try {
    // 打印联系人信息以便调试
    console.log('从菜单开始聊天，联系人信息:', selectedContact.value);
    
    // 尝试从不同的属性中获取联系人ID
    let rawContactId = selectedContact.value.id;
    
    // 如果id为undefined，尝试从其他属性获取
    if (rawContactId === undefined) {
      if (selectedContact.value.friendId !== undefined) {
        rawContactId = selectedContact.value.friendId;
        console.log('使用friendId作为联系人ID:', rawContactId);
      } else if (selectedContact.value.rawData && selectedContact.value.rawData.id !== undefined) {
        rawContactId = selectedContact.value.rawData.id;
        console.log('使用rawData.id作为联系人ID:', rawContactId);
      } else if (selectedContact.value.rawData && selectedContact.value.rawData.friendId !== undefined) {
        rawContactId = selectedContact.value.rawData.friendId;
        console.log('使用rawData.friendId作为联系人ID:', rawContactId);
      } else if (selectedContact.value.friend && selectedContact.value.friend.id !== undefined) {
        rawContactId = selectedContact.value.friend.id;
        console.log('使用friend.id作为联系人ID:', rawContactId);
      } else {
        throw new Error('无法获取有效的联系人ID');
      }
    }
    
    console.log(`联系人ID ${rawContactId}，ID类型:`, typeof rawContactId);
    
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
    
    console.log('处理后的联系人ID:', contactId, '类型:', typeof contactId);
    
    const response = await messageApi.getOrCreatePrivateConversation(
      contactId,
      props.currentUserId
    );
    
    if (response.success && response.data) {
      let conversationId: number | undefined;
      
      // 处理不同的响应结构
      if (response.data.id) {
        conversationId = response.data.id;
      } else if (response.data.conversation && response.data.conversation.id) {
        conversationId = response.data.conversation.id;
      } else if (typeof response.data === 'number') {
        // 直接返回了ID
        conversationId = response.data;
      }
      
      if (conversationId) {
        console.log('提取到会话ID:', conversationId);
        emit('start-chat', {
          contact: selectedContact.value,
          conversationId: conversationId
        });
      } else {
        console.error('无法从响应中提取会话ID:', response.data);
        throw new Error('无法获取会话ID');
      }
    } else {
      throw new Error(response.message || '创建会话失败');
    }
  } catch (err: any) {
    error.value = err.message || '开始聊天失败';
    emit('error', error.value);
  } finally {
    closeMenu();
  }
};

// 修改备注
const handleEditAlias = () => {
  if (!selectedContact.value) return;
  
  // 确保联系人ID是有效的
  const contactId = ensureValidContactId(selectedContact.value.id);
  if (!contactId) {
    console.error('无法获取有效的联系人ID');
    emit('error', '无法获取有效的联系人ID');
    return;
  }
  
  // 创建一个新对象，确保id是有效的数字
  const contactWithValidId = {
    ...selectedContact.value,
    id: contactId
  };
  
  emit('edit-alias', contactWithValidId);
  closeMenu();
};

// 管理标签
const handleManageTags = () => {
  if (!selectedContact.value) return;
  
  // 确保联系人ID是有效的
  const contactId = ensureValidContactId(selectedContact.value.id);
  if (!contactId) {
    console.error('无法获取有效的联系人ID');
    emit('error', '无法获取有效的联系人ID');
    return;
  }
  
  // 创建一个新对象，确保id是有效的数字
  const contactWithValidId = {
    ...selectedContact.value,
    id: contactId
  };
  
  emit('manage-tags', contactWithValidId);
  closeMenu();
};

// 拉黑联系人
const handleBlockContact = async () => {
  if (!selectedContact.value) return;
  
  const contactId = ensureValidContactId(selectedContact.value.id);
  if (!contactId) {
    console.error('无法获取有效的联系人ID');
    emit('error', '无法获取有效的联系人ID');
    return;
  }

  try {
    const response = await contactApi.blockFriend(contactId, props.currentUserId);
    if (response.success) {
      console.log('联系人已拉黑');
      // 刷新联系人列表或更新UI
      loadContacts();
      emit('block-contact', contactId); // 发射拉黑事件
      emit('error', '联系人已拉黑');
    } else {
      throw new Error(response.message || '拉黑联系人失败');
    }
  } catch (err: any) {
    error.value = err.message || '拉黑联系人失败';
    emit('error', error.value);
  } finally {
    closeMenu();
  }
};

// 解除拉黑联系人
const handleUnblockContact = async () => {
  if (!selectedContact.value) return;
  
  const contactId = ensureValidContactId(selectedContact.value.id);
  if (!contactId) {
    console.error('无法获取有效的联系人ID');
    emit('error', '无法获取有效的联系人ID');
    return;
  }

  try {
    const response = await contactApi.unblockFriend(contactId, props.currentUserId);
    if (response.success) {
      console.log('联系人已解除拉黑');
      // 刷新联系人列表或更新UI
      loadContacts();
      emit('unblock-contact', contactId); // 发射解除拉黑事件
      emit('error', '联系人已解除拉黑');
    } else {
      throw new Error(response.message || '解除拉黑联系人失败');
    }
  } catch (err: any) {
    error.value = err.message || '解除拉黑联系人失败';
    emit('error', error.value);
  } finally {
    closeMenu();
  }
};

// 举报联系人
const handleReportContact = () => {
  if (!selectedContact.value) return;
  
  showMenu.value = false; // 关闭菜单
  showReportDialog.value = true;
  reportReason.value = '';
  reportDescription.value = '';
};

// 取消举报
const cancelReport = () => {
  showReportDialog.value = false;
  reportReason.value = '';
  reportDescription.value = '';
};

// 提交举报
const submitReport = async () => {
  if (!selectedContact.value) {
    ElMessage.warning('无法获取联系人信息');
    return;
  }
  
  const contactId = ensureValidContactId(selectedContact.value.id);
  if (!contactId) {
    console.error('无法获取有效的联系人ID');
    ElMessage.warning('无法获取有效的联系人ID');
    return;
  }

  if (!reportReason.value) {
    ElMessage.warning('请选择举报原因');
    return;
  }

  try {
    const response = await reportApi.reportContact(contactId, props.currentUserId, reportReason.value, reportDescription.value);
    if (response.success) {
      ElMessage.success('举报已提交，我们将尽快处理');
      console.log('联系人已举报');
    } else {
      throw new Error(response.message || '举报提交失败');
    }
  } catch (err: any) {
    error.value = err.message || '举报提交失败';
    ElMessage.error(error.value);
  } finally {
    showReportDialog.value = false;
    reportReason.value = '';
    reportDescription.value = '';
  }
};

// 删除联系人
const handleDeleteContact = () => {
  if (!selectedContact.value) return;
  
  // 确保联系人ID是有效的
  const contactId = ensureValidContactId(selectedContact.value.id);
  if (!contactId) {
    console.error('无法获取有效的联系人ID');
    emit('error', '无法获取有效的联系人ID');
    return;
  }
  
  // 创建一个新对象，确保id是有效的数字
  const contactWithValidId = {
    ...selectedContact.value,
    id: contactId
  };
  
  if (confirm(`确定要删除联系人"${selectedContact.value.name}"吗？`)) {
    emit('delete-contact', contactWithValidId);
    closeMenu();
  }
};

// 辅助函数：确保联系人ID是有效的数字
const ensureValidContactId = (rawId: any): number | null => {
  // 如果ID为undefined或null，尝试从其他属性获取
  if (rawId === undefined || rawId === null) {
    if (selectedContact.value.friendId !== undefined) {
      rawId = selectedContact.value.friendId;
    } else if (selectedContact.value.rawData && selectedContact.value.rawData.id !== undefined) {
      rawId = selectedContact.value.rawData.id;
    } else if (selectedContact.value.friend && selectedContact.value.friend.id !== undefined) {
      rawId = selectedContact.value.friend.id;
    } else {
      return null;
    }
  }
  
  // 确保ID是数字类型
  let contactId: number;
  if (typeof rawId === 'string') {
    contactId = parseInt(rawId, 10);
    if (isNaN(contactId)) {
      return null;
    }
  } else if (typeof rawId === 'number') {
    contactId = rawId;
  } else {
    return null;
  }
  
  return contactId;
};

// 判断联系人是否被拉黑
const isContactBlocked = (contact: any) => {
  if (!contact) {
    console.warn('isContactBlocked: 联系人对象为空');
    return false;
  }
  
  // 详细记录联系人拉黑状态检查
  console.log('isContactBlocked检查:', {
    id: contact.id,
    name: contact.name,
    isBlocked: contact.isBlocked,
    status: contact.status
  });
  
  // 直接检查isBlocked字段
  return Boolean(contact.isBlocked);
};

// 组件挂载时加载联系人列表
onMounted(() => {
  if (props.currentUserId) {
    loadContacts();
  }
});

// 导出方法给父组件
defineExpose({
  loadContacts,
  contacts
});
</script>

<style scoped>
.contacts-list-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.search-bar {
  padding: 16px;
  border-bottom: 1px solid #eee;
}

.search-input {
  width: 100%;
  padding: 10px 16px;
  border-radius: 20px;
  border: 1px solid #ddd;
  outline: none;
  font-size: 14px;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #3498db;
}

.contacts-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.contacts-loading,
.no-contacts {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100px;
  color: #666;
  font-size: 14px;
}

.context-menu {
  position: fixed;
  background: white;
  border-radius: 4px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  min-width: 150px;
}

.menu-item {
  padding: 10px 16px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.menu-item:hover {
  background-color: #f5f5f5;
}

.menu-item.delete {
  color: #e74c3c;
}

.menu-item.delete:hover {
  background-color: #fee;
}

.menu-item.warning {
  color: #f39c12;
}

.menu-item.warning:hover {
  background-color: #fdf5e6;
}

.menu-item.success {
  color: #2ecc71;
}

.menu-item.success:hover {
  background-color: #e8f5e9;
}

.menu-item.report {
  color: #9b59b6;
}

.menu-item.report:hover {
  background-color: #f8f0fc;
}

.report-dialog-overlay {
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
}

.report-dialog {
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
  width: 90%;
  max-width: 450px;
  max-height: 90%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.report-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  background-color: #f9f9f9;
}

.report-dialog-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #888;
  cursor: pointer;
  padding: 5px;
  line-height: 1;
}

.close-btn:hover {
  color: #555;
}

.report-dialog-body {
  padding: 20px;
  overflow-y: auto;
  flex-grow: 1;
}

.report-form {
  display: flex;
  flex-direction: column;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #555;
}

.report-reason-select,
.report-description {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.report-reason-select:focus,
.report-description:focus {
  border-color: #3498db;
}

.report-description {
  min-height: 80px;
  resize: vertical;
  font-family: inherit;
}

.report-dialog-footer {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #eee;
  background-color: #f9f9f9;
}

.cancel-btn,
.submit-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.cancel-btn {
  background-color: #e0e0e0;
  color: #333;
  margin-right: 10px;
}

.cancel-btn:hover {
  background-color: #d5d5d5;
}

.submit-btn {
  background-color: #3498db;
  color: white;
}

.submit-btn:hover {
  background-color: #2980b9;
}

.submit-btn:disabled {
  background-color: #a0c4ff;
  cursor: not-allowed;
  color: #888;
}
</style> 