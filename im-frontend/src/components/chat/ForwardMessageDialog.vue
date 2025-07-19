<template>
  <div class="forward-dialog-overlay" v-if="isVisible" @click.self="cancel">
    <div class="forward-dialog">
      <div class="forward-dialog-header">
        <h3>转发消息</h3>
        <button class="close-btn" @click="cancel">&times;</button>
      </div>
      
      <div class="forward-dialog-body">
        <!-- 消息预览 -->
        <div class="message-preview">
          <h4>要转发的消息</h4>
          <div class="preview-content">
            <template v-if="messages.length === 1">
              <div class="single-message">
                <div class="message-sender">{{ messages[0].senderName || '用户' }}</div>
                <div class="message-content">
                  <template v-if="messages[0].type === 'TEXT'">{{ messages[0].content }}</template>
                  <template v-else-if="messages[0].type === 'IMAGE'">[图片消息]</template>
                  <template v-else-if="messages[0].type === 'FILE'">[文件: {{ messages[0].fileName || '未命名文件' }}]</template>
                  <template v-else-if="messages[0].type === 'VOICE'">[语音消息]</template>
                  <template v-else-if="messages[0].type === 'VIDEO'">[视频消息]</template>
                  <template v-else>[{{ messages[0].type }}]</template>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="multiple-messages">
                <div class="message-count">{{ messages.length }}条消息</div>
                <div class="message-summary">
                  <div v-for="(msg, index) in messages.slice(0, 3)" :key="index" class="message-item">
                    <span class="sender">{{ msg.senderName || '用户' }}:</span>
                    <span class="content">
                      <template v-if="msg.type === 'TEXT'">{{ truncateText(msg.content, 15) }}</template>
                      <template v-else-if="msg.type === 'IMAGE'">[图片消息]</template>
                      <template v-else-if="msg.type === 'FILE'">[文件]</template>
                      <template v-else-if="msg.type === 'VOICE'">[语音消息]</template>
                      <template v-else-if="msg.type === 'VIDEO'">[视频消息]</template>
                      <template v-else>[{{ msg.type }}]</template>
                    </span>
                  </div>
                  <div v-if="messages.length > 3" class="more-messages">...</div>
                </div>
              </div>
            </template>
          </div>
        </div>
        
        <!-- 转发类型选择 -->
        <div class="forward-type">
          <h4>转发方式</h4>
          <div class="type-options">
            <label class="type-option">
              <input type="radio" v-model="forwardType" value="SEPARATE" />
              <span class="option-text">逐条转发</span>
            </label>
            <label class="type-option">
              <input type="radio" v-model="forwardType" value="MERGE" />
              <span class="option-text">合并转发</span>
            </label>
          </div>
        </div>
        
        <!-- 添加评论 -->
        <div class="add-comment">
          <h4>添加评论 (可选)</h4>
          <textarea 
            v-model="comment" 
            class="comment-input" 
            placeholder="添加评论..."
            maxlength="100"
          ></textarea>
          <div class="comment-counter">{{ comment.length }}/100</div>
        </div>
        
        <!-- 选择目标会话 -->
        <div class="target-selection">
          <h4>选择转发对象</h4>
          <div class="search-box">
            <input 
              type="text" 
              v-model="searchKeyword" 
              placeholder="搜索联系人或群组..." 
              class="search-input" 
            />
          </div>
          
          <div class="targets-container">
            <div v-if="loading" class="loading">
              <div class="loading-spinner"></div>
              <span>加载中...</span>
            </div>
            
            <div v-else-if="loadingError" class="no-results">{{ loadingError }}</div>
            
            <div v-else-if="filteredConversations.length === 0" class="no-results">
              没有找到匹配的联系人或群组
            </div>
            
            <div v-else class="targets-list">
              <div 
                v-for="conv in filteredConversations" 
                :key="conv.id" 
                class="target-item"
                :class="{ 'selected': selectedTargets.includes(conv.id) }"
                @click="toggleTarget(conv.id)"
              >
                <div class="target-avatar">
                  <img :src="conv.avatarUrl || '/favicon.ico'" alt="avatar" />
                </div>
                <div class="target-info">
                  <div class="target-name">{{ conv.name || '未命名会话' }}</div>
                  <div class="target-type">{{ conv.type === 'GROUP' ? '群聊' : '联系人' }}</div>
                </div>
                <div class="target-checkbox">
                  <i class="fas" :class="selectedTargets.includes(conv.id) ? 'fa-check-circle' : 'fa-circle'"></i>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div class="forward-dialog-footer">
        <button class="cancel-btn" @click="cancel">取消</button>
        <button 
          class="forward-btn" 
          :disabled="selectedTargets.length === 0 || loading" 
          @click="handleForward"
        >
          转发 ({{ selectedTargets.length }})
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { messageApi, type ForwardMessageRequest, ConversationType } from '@/api/message';
import { useAuth } from '@/composables/useAuth';
import { contactApi } from '@/api/contact';

const props = defineProps<{
  isVisible: boolean;
  messages: any[];
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'forward-success'): void;
}>();

// 获取当前用户
const { currentUser } = useAuth();

// 状态
const loading = ref(false);
const loadingError = ref('');
const conversations = ref<any[]>([]);
const selectedTargets = ref<number[]>([]);
const forwardType = ref<'MERGE' | 'SEPARATE'>('SEPARATE');
const comment = ref('');
const searchKeyword = ref('');

// 过滤后的会话列表
const filteredConversations = computed(() => {
  if (!searchKeyword.value.trim()) {
    return conversations.value;
  }
  
  const keyword = searchKeyword.value.toLowerCase();
  return conversations.value.filter(conv => {
    const name = conv.name || '';
    return name.toLowerCase().includes(keyword);
  });
});

// 加载会话列表
const loadConversations = async () => {
  // 尝试获取用户ID，首先检查currentUser，然后检查localStorage和sessionStorage
  let userId = currentUser.value?.id;
  
  if (!userId) {
    // 从localStorage获取
    const localUserId = localStorage.getItem('userId');
    if (localUserId) {
      userId = parseInt(localUserId, 10);
    }
    
    // 如果localStorage中没有，从sessionStorage获取
    if (!userId) {
      const sessionUserId = sessionStorage.getItem('userId');
      if (sessionUserId) {
        userId = parseInt(sessionUserId, 10);
      }
    }
    
    // 从userInfo中获取
    if (!userId) {
      try {
        const localUserInfo = localStorage.getItem('userInfo');
        if (localUserInfo) {
          const userInfo = JSON.parse(localUserInfo);
          if (userInfo && userInfo.id) {
            userId = parseInt(userInfo.id, 10);
          }
        }
      } catch (e) {
        console.error('解析localStorage中的userInfo失败:', e);
      }
    }
    
    if (!userId) {
      try {
        const sessionUserInfo = sessionStorage.getItem('userInfo');
        if (sessionUserInfo) {
          const userInfo = JSON.parse(sessionUserInfo);
          if (userInfo && userInfo.id) {
            userId = parseInt(userInfo.id, 10);
          }
        }
      } catch (e) {
        console.error('解析sessionStorage中的userInfo失败:', e);
      }
    }
  }
  
  if (!userId || isNaN(userId) || userId <= 0) {
    console.error('无法加载会话列表：未找到有效的用户ID');
    loadingError.value = '请先登录';
    return;
  }
  
  try {
    loading.value = true;
    loadingError.value = '';
    console.log('开始加载会话列表，用户ID:', userId);
    
    // 尝试直接获取会话列表
    const response = await messageApi.getConversations(userId, 0, 50);
    console.log('获取会话列表响应:', response);
    console.log('响应数据类型:', typeof response.data);
    console.log('响应数据结构:', JSON.stringify(response.data).substring(0, 200) + '...');
    
    if (response.success && response.data) {
      // 处理嵌套的响应结构
      let conversationsData: any[] = [];
      
      // 检查各种可能的数据结构
      const responseData = response.data;
      console.log('开始解析会话数据');
      
      if (Array.isArray(responseData)) {
        console.log('会话数据是数组格式, 长度:', responseData.length);
        conversationsData = responseData;
      } else if (responseData.content && Array.isArray(responseData.content)) {
        console.log('会话数据在content字段中, 长度:', responseData.content.length);
        
        // 检查content中是否包含conversations数组
        if (responseData.content.length > 0 && responseData.content[0].conversations && Array.isArray(responseData.content[0].conversations)) {
          console.log('在content[0].conversations中找到会话数组, 长度:', responseData.content[0].conversations.length);
          conversationsData = responseData.content[0].conversations;
        } else {
          conversationsData = responseData.content;
        }
      } else if (responseData.conversations && Array.isArray(responseData.conversations)) {
        console.log('会话数据在conversations字段中, 长度:', responseData.conversations.length);
        conversationsData = responseData.conversations;
      } else if (responseData.data && Array.isArray(responseData.data)) {
        console.log('会话数据在嵌套的data字段中, 长度:', responseData.data.length);
        conversationsData = responseData.data;
      } else {
        // 尝试解析对象格式
        console.log('尝试解析对象格式的会话数据');
        if (typeof responseData === 'object' && responseData !== null) {
          const keys = Object.keys(responseData);
          console.log('响应数据的键:', keys);
          
          // 检查是否是单个会话对象
          if (responseData.id && (responseData.type || responseData.conversationType)) {
            console.log('找到单个会话对象');
            conversationsData = [responseData];
          } else {
            // 检查是否有conversations字段，可能在不同层级
            if (responseData.conversations && Array.isArray(responseData.conversations)) {
              console.log('在顶层找到conversations数组, 长度:', responseData.conversations.length);
              conversationsData = responseData.conversations;
            } else if (responseData.conversation && responseData.conversation.conversations && 
                      Array.isArray(responseData.conversation.conversations)) {
              console.log('在conversation.conversations中找到数组, 长度:', responseData.conversation.conversations.length);
              conversationsData = responseData.conversation.conversations;
            } else {
              // 递归查找conversations数组
              const findConversationsArray = (obj: any): any[] | null => {
                if (!obj || typeof obj !== 'object') return null;
                
                // 直接检查是否有conversations数组
                if (obj.conversations && Array.isArray(obj.conversations)) {
                  return obj.conversations;
                }
                
                // 递归检查所有对象属性
                for (const key in obj) {
                  if (typeof obj[key] === 'object' && obj[key] !== null) {
                    const result = findConversationsArray(obj[key]);
                    if (result) return result;
                  }
                }
                
                return null;
              };
              
              const foundArray = findConversationsArray(responseData);
              if (foundArray) {
                console.log('在嵌套结构中找到conversations数组, 长度:', foundArray.length);
                conversationsData = foundArray;
              } else {
                // 检查是否有其他可能的数组字段
                for (const key of keys) {
                  const value = (responseData as any)[key];
                  if (Array.isArray(value) && value.length > 0) {
                    console.log(`在字段 ${key} 中找到数组数据, 长度:`, value.length);
                    conversationsData = value;
                    break;
                  }
                }
              }
            }
          }
        }
      }
      
      if (conversationsData.length > 0) {
        console.log(`成功获取到 ${conversationsData.length} 个会话`);
        console.log('第一个会话示例:', JSON.stringify(conversationsData[0]).substring(0, 200) + '...');
        
        // 确保每个会话对象有必要的字段
        conversations.value = conversationsData.map((conv, index) => {
          console.log(`处理会话 ${index + 1}:`, conv.id || '未知ID');
          
          // 获取会话ID
          const id = conv.id || conv.conversationId;
          if (!id) {
            console.warn(`会话 ${index + 1} 没有有效的ID`);
          }
          
          // 获取会话类型
          const type = conv.type || conv.conversationType || 'PRIVATE';
          console.log(`会话 ${index + 1} 类型:`, type);
          
          // 获取会话名称
          let name = '';
          if (conv.name) {
            name = conv.name;
          } else if (type === 'GROUP') {
            name = conv.groupName || '群聊';
          } else if (type === 'PRIVATE') {
            // 尝试从参与者中获取名称
            if (conv.participants && Array.isArray(conv.participants) && conv.participants.length > 0) {
              const otherParticipant = conv.participants.find((p: any) => p.userId !== userId);
              if (otherParticipant) {
                name = otherParticipant.alias || otherParticipant.nickname || otherParticipant.username || '联系人';
              }
            } else if (conv.contactName) {
              name = conv.contactName;
            } else if (conv.otherParticipant) {
              name = conv.otherParticipant.nickname || conv.otherParticipant.username || '联系人';
            }
            
            // 如果还没有名称，使用会话ID
            if (!name) {
              name = `私聊 ${id || index + 1}`;
            }
          }
          
          console.log(`会话 ${index + 1} 名称:`, name);
          
          // 获取头像URL
          const avatarUrl = conv.avatarUrl || conv.avatar || '/favicon.ico';
          
          return {
            id: id,
            name: name,
            avatarUrl: avatarUrl,
            type: type,
            lastActiveTime: conv.lastActiveTime || new Date().toISOString()
          };
        });
        
        console.log('处理后的会话列表:', conversations.value);
      } else {
        console.warn('未找到任何会话数据');
        await loadContactsAsConversations(userId);
      }
    } else {
      console.error('获取会话列表失败:', response.message);
      loadingError.value = response.message || '获取会话列表失败';
      
      // 尝试使用备用API
      console.log('尝试使用备用API获取会话列表');
      try {
        const backupResponse = await fetch(`/api/conversations?userId=${userId}`, {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken') || ''}`,
            'X-User-Id': userId.toString()
          }
        });
        
        if (backupResponse.ok) {
          const backupData = await backupResponse.json();
          console.log('备用API响应:', backupData);
          
          if (backupData.success && backupData.data && Array.isArray(backupData.data)) {
            conversations.value = backupData.data.map((conv: any) => ({
              id: conv.id,
              name: conv.name || `会话 ${conv.id}`,
              avatarUrl: conv.avatarUrl || '/favicon.ico',
              type: conv.type || 'PRIVATE',
              lastActiveTime: conv.lastActiveTime || new Date().toISOString()
            }));
            
            loadingError.value = '';
            console.log('使用备用API成功获取会话列表');
          } else {
            // 如果备用API也失败，尝试加载联系人列表
            await loadContactsAsConversations(userId);
          }
        } else {
          // 如果备用API也失败，尝试加载联系人列表
          await loadContactsAsConversations(userId);
        }
      } catch (backupError) {
        console.error('备用API也失败了:', backupError);
        // 尝试加载联系人列表
        await loadContactsAsConversations(userId);
      }
    }
  } catch (error: any) {
    console.error('加载会话列表出错:', error);
    loadingError.value = error.message || '加载会话列表时发生错误';
    
    // 尝试加载联系人列表
    await loadContactsAsConversations(userId as number);
  } finally {
    loading.value = false;
  }
};

// 从联系人列表加载会话
const loadContactsAsConversations = async (userId: number) => {
  console.log('尝试从联系人列表加载会话');
  try {
    const response = await contactApi.getContacts(userId, false);
    console.log('获取联系人列表响应:', response);
    
    if (response.success && response.data && Array.isArray(response.data) && response.data.length > 0) {
      console.log(`成功获取到 ${response.data.length} 个联系人`);
      console.log('联系人数据样例:', JSON.stringify(response.data[0]));
      
      // 将联系人转换为会话
      conversations.value = response.data.map((contact, index) => {
        console.log(`处理联系人 ${index + 1}:`, contact.id || '未知ID', contact);
        
        // 获取联系人名称 - 详细记录每一步
        let name = '';
        
        // 尝试获取别名（备注）
        if (contact.alias) {
          name = contact.alias;
          console.log(`联系人 ${index + 1} 使用别名:`, name);
        }
        // 尝试获取好友昵称
        else if (contact.friend && contact.friend.nickname) {
          name = contact.friend.nickname || '';
          console.log(`联系人 ${index + 1} 使用好友昵称:`, name);
        }
        // 尝试获取用户昵称
        else if (contact.friend && contact.friend.nickname) {
          name = contact.friend.nickname || '';
          console.log(`联系人 ${index + 1} 使用用户昵称:`, name);
        }
        // 尝试获取用户名
        else if (contact.friend && contact.friend.email) {
          name = (contact.friend.email || '').split('@')[0];
          console.log(`联系人 ${index + 1} 使用用户名:`, name);
        }
        // 尝试从邮箱获取名称
        else if (contact.friend && contact.friend.email) {
          name = (contact.friend.email || '').split('@')[0];
          console.log(`联系人 ${index + 1} 使用邮箱名:`, name);
        }
        // 使用备用名称
        else {
          name = `联系人 ${index + 1}`;
          console.log(`联系人 ${index + 1} 使用备用名称:`, name);
        }
        
        // 获取头像URL - 详细检查各种可能的属性
        let avatarUrl = '/favicon.ico';
        
        if (contact.friend && contact.friend.avatarUrl) {
          avatarUrl = contact.friend.avatarUrl || '/favicon.ico';
          console.log(`联系人 ${index + 1} 使用联系人头像:`, avatarUrl);
        }
        else {
          console.log(`联系人 ${index + 1} 使用默认头像`);
        }
        
        // 获取friendId
        let friendId = null;
        if (contact.friendId) {
          friendId = contact.friendId;
        } else if (contact.friend && contact.friend.id) {
          friendId = contact.friend.id;
        } else if (contact.userId) {
          friendId = contact.userId;
        }
        
        console.log(`联系人 ${index + 1} 最终数据:`, { id: contact.id, name, avatarUrl, friendId });
        
        return {
          id: contact.id,
          name: name,
          avatarUrl: avatarUrl,
          type: 'PRIVATE' as const,
          lastActiveTime: (contact.updatedAt || new Date().toISOString()) as string,
          friendId: friendId
        };
      });
      
      console.log('从联系人生成的会话列表:', conversations.value);
      loadingError.value = '';
    } else {
      console.warn('未找到任何联系人数据，使用硬编码的测试数据');
      // 使用硬编码的测试联系人作为备用选项
      loadDummyContacts();
    }
  } catch (error) {
    console.error('加载联系人列表出错:', error);
    // 使用硬编码的测试联系人作为备用选项
    loadDummyContacts();
  }
};

// 加载硬编码的测试联系人
const loadDummyContacts = () => {
  console.log('加载硬编码的测试联系人');
  
  // 模拟联系人数据
  const dummyContacts = [
    { id: 101, name: "张三", type: "PRIVATE", avatarUrl: "/favicon.ico", friendId: 201 },
    { id: 102, name: "李四", type: "PRIVATE", avatarUrl: "/favicon.ico", friendId: 202 },
    { id: 103, name: "王五", type: "PRIVATE", avatarUrl: "/favicon.ico", friendId: 203 },
    { id: 104, name: "赵六", type: "PRIVATE", avatarUrl: "/favicon.ico", friendId: 204 },
    { id: 105, name: "测试群组", type: "GROUP", avatarUrl: "/favicon.ico" }
  ];
  
  conversations.value = dummyContacts;
  console.log('已加载硬编码的测试联系人:', conversations.value);
  loadingError.value = '使用了测试联系人数据，仅用于演示';
};

// 获取会话显示名称
const getConversationName = (conversation: any): string => {
  if (!conversation) return '未知会话';
  
  // 如果直接有名称，优先使用
  if (conversation.name) return conversation.name;
  
  // 获取当前用户ID
  let currentUserId = currentUser.value?.id;
  if (!currentUserId) {
    // 从localStorage获取
    const localUserId = localStorage.getItem('userId');
    if (localUserId) {
      currentUserId = parseInt(localUserId, 10);
    }
    
    // 如果localStorage中没有，从sessionStorage获取
    if (!currentUserId) {
      const sessionUserId = sessionStorage.getItem('userId');
      if (sessionUserId) {
        currentUserId = parseInt(sessionUserId, 10);
      }
    }
    
    // 从userInfo中获取
    if (!currentUserId) {
      try {
        const localUserInfo = localStorage.getItem('userInfo');
        if (localUserInfo) {
          const userInfo = JSON.parse(localUserInfo);
          if (userInfo && userInfo.id) {
            currentUserId = parseInt(userInfo.id, 10);
          }
        }
      } catch (e) {
        console.error('解析localStorage中的userInfo失败:', e);
      }
    }
  }
  
  // 根据会话类型处理
  if (conversation.type === 'GROUP') {
    return conversation.name || '群聊';
  } else if (conversation.type === 'PRIVATE') {
    // 私聊需要找出对方信息
    if (conversation.participants && conversation.participants.length > 0) {
      // 找出非当前用户的参与者
      const otherParticipant = conversation.participants.find((p: any) => 
        p.userId !== currentUserId
      );
      
      if (otherParticipant) {
        return otherParticipant.alias || 
               otherParticipant.nickname || 
               otherParticipant.username || 
               '联系人';
      }
    }
    
    // 如果没找到对方信息，使用会话ID
    return `私聊 ${conversation.id}`;
  }
  
  // 其他类型
  return `会话 ${conversation.id}`;
};

// 处理转发
const handleForward = async () => {
  if (selectedTargets.value.length === 0) {
    loadingError.value = '请选择至少一个转发目标';
    return;
  }
  
  if (props.messages.length === 0) {
    loadingError.value = '没有可转发的消息';
    return;
  }
  
  try {
    loading.value = true;
    loadingError.value = '';
    
    const messageIds = props.messages.map(msg => msg.id);
    
    // 检查选中的目标是否包含friendId而不是conversationId
    const targetConversationIds: number[] = [];
    const friendIds: number[] = [];
    
    for (const targetId of selectedTargets.value) {
      const target = conversations.value.find(c => c.id === targetId);
      if (target) {
        if (target.friendId && !target.conversationId) {
          // 这是一个联系人，需要先创建会话
          friendIds.push(target.friendId);
        } else {
          // 这是一个现有会话
          targetConversationIds.push(targetId);
        }
      }
    }
    
    console.log('转发到现有会话:', targetConversationIds);
    console.log('需要创建会话的好友:', friendIds);
    
    // 如果有需要创建会话的好友，先创建会话
    if (friendIds.length > 0) {
      for (const friendId of friendIds) {
        try {
          console.log('为好友创建会话:', friendId);
          // 获取当前用户ID
          let userId = currentUser.value?.id;
          if (!userId) {
            userId = parseInt(localStorage.getItem('userId') || sessionStorage.getItem('userId') || '0', 10);
          }
          
          // 创建私聊会话
          const createResponse = await messageApi.getOrCreatePrivateConversation(friendId, userId as number);
          console.log('创建会话响应:', createResponse);
          
          if (createResponse.success && createResponse.data && createResponse.data.id) {
            console.log('成功创建会话:', createResponse.data.id);
            targetConversationIds.push(createResponse.data.id);
          } else {
            console.error('创建会话失败:', createResponse.message);
          }
        } catch (error) {
          console.error('创建会话出错:', error);
        }
      }
    }
    
    // 如果没有有效的会话ID，显示错误
    if (targetConversationIds.length === 0) {
      throw new Error('无法创建会话，请重试');
    }
    
    const forwardRequest: ForwardMessageRequest = {
      messageIds: messageIds,
      targetConversationIds: targetConversationIds,
      forwardType: forwardType.value,
      comment: comment.value || undefined
    };
    
    console.log('转发消息请求数据:', forwardRequest);
    const response = await messageApi.forwardMessage(forwardRequest);
    
    if (response.success) {
      console.log('消息转发成功');
      emit('forward-success');
      closeDialog();
    } else {
      throw new Error(response.message || '转发消息失败');
    }
  } catch (error: any) {
    console.error('转发消息失败:', error);
    loadingError.value = error.message || '转发消息失败';
  } finally {
    loading.value = false;
  }
};

// 关闭对话框
const closeDialog = () => {
  searchKeyword.value = '';
  selectedTargets.value = [];
  forwardType.value = 'SEPARATE';
  comment.value = '';
  loadingError.value = '';
  emit('close');
};

// 会话选择状态切换
const toggleTarget = (id: number) => {
  const index = selectedTargets.value.indexOf(id);
  if (index === -1) {
    selectedTargets.value.push(id);
  } else {
    selectedTargets.value.splice(index, 1);
  }
};

// 截断文本
const truncateText = (text: string, maxLength: number): string => {
  if (!text) return '';
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
};

// 取消转发
const cancel = () => {
  closeDialog();
};

// 监听对话框显示状态
watch(() => props.isVisible, (newVal) => {
  if (newVal) {
    // 对话框显示时加载会话列表
    loadConversations();
  }
});

// 组件挂载时初始化
onMounted(() => {
  if (props.isVisible) {
    loadConversations();
  }
});
</script>

<style scoped>
.forward-dialog-overlay {
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

.forward-dialog {
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
}

.forward-dialog-header {
  padding: 15px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.forward-dialog-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  color: #999;
  cursor: pointer;
}

.forward-dialog-body {
  padding: 15px;
  overflow-y: auto;
  flex: 1;
}

.message-preview {
  margin-bottom: 20px;
}

.message-preview h4 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #666;
}

.preview-content {
  background-color: #f5f5f5;
  border-radius: 8px;
  padding: 10px;
}

.single-message .message-sender {
  font-weight: bold;
  margin-bottom: 5px;
}

.single-message .message-content {
  word-break: break-word;
}

.multiple-messages .message-count {
  font-weight: bold;
  margin-bottom: 5px;
}

.multiple-messages .message-item {
  margin-bottom: 5px;
}

.multiple-messages .sender {
  font-weight: bold;
  margin-right: 5px;
}

.multiple-messages .more-messages {
  color: #999;
  text-align: center;
  margin-top: 5px;
}

.forward-type {
  margin-bottom: 20px;
}

.forward-type h4 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #666;
}

.type-options {
  display: flex;
  gap: 20px;
}

.type-option {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.option-text {
  margin-left: 5px;
}

.add-comment {
  margin-bottom: 20px;
  position: relative;
}

.add-comment h4 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #666;
}

.comment-input {
  width: 100%;
  height: 60px;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: none;
  font-size: 14px;
}

.comment-counter {
  position: absolute;
  right: 5px;
  bottom: 5px;
  font-size: 12px;
  color: #999;
}

.target-selection h4 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #666;
}

.search-box {
  margin-bottom: 10px;
}

.search-input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.targets-container {
  max-height: 250px;
  overflow-y: auto;
  border: 1px solid #eee;
  border-radius: 4px;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: #999;
}

.loading-spinner {
  border: 3px solid #f3f3f3;
  border-top: 3px solid #4caf50;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.no-results {
  padding: 20px;
  text-align: center;
  color: #999;
}

.target-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}

.target-item:last-child {
  border-bottom: none;
}

.target-item:hover {
  background-color: #f9f9f9;
}

.target-item.selected {
  background-color: #e8f5e9;
}

.target-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 10px;
}

.target-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.target-info {
  flex: 1;
}

.target-name {
  font-weight: bold;
  margin-bottom: 3px;
}

.target-type {
  font-size: 12px;
  color: #999;
}

.target-checkbox {
  color: #ddd;
  font-size: 20px;
}

.target-item.selected .target-checkbox {
  color: #4caf50;
}

.forward-dialog-footer {
  padding: 15px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.cancel-btn {
  padding: 8px 15px;
  background-color: #f5f5f5;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.forward-btn {
  padding: 8px 15px;
  background-color: #4caf50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.forward-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}
</style> 