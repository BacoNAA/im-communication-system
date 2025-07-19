import { ref, computed, reactive, readonly, watch, onMounted, onUnmounted } from 'vue';
import { messageApi, type Conversation, type Message, type SendMessageRequest, type MessageType, type MarkAsReadRequest, MessageStatus, ConversationType } from '@/api/message';
import { useAuth } from './useAuth';
import { useWebSocket, DEFAULT_WS_URL } from './useWebSocket';

// 扩展Message类型，添加isSelf属性
interface ExtendedMessage extends Message {
  isSelf?: boolean;
}

const conversations = ref<Conversation[]>([]);
const currentConversation = ref<Conversation | null>(null);
const currentMessages = ref<ExtendedMessage[]>([]);
const isLoading = ref(false);
const error = ref<string | null>(null);
const unreadCountsMap = ref<Record<number, number>>({});

export function useMessages() {
  const { currentUser } = useAuth();
  
  // 处理WebSocket消息
  const handleWebSocketMessage = (data: any) => {
    try {
      console.log('接收到WebSocket消息:', data);
      
      // 处理不同类型的消息
      if (data.type === 'MESSAGE' || data.type === 'message') {
        // 新消息
        handleRealTimeMessage(data);
      } else if (data.type === 'STATUS_UPDATE' || data.type === 'status_update') {
        // 状态更新
        handleStatusUpdate(data);
      } else if (data.type === 'RECALL' || data.type === 'recall') {
        // 消息撤回
        handleMessageRecall(data);
      } else if (data.type === 'TYPING' || data.type === 'typing') {
        // 输入状态（未实现）
        console.log('收到输入状态更新:', data);
      } else if (data.type === 'PRESENCE' || data.type === 'presence') {
        // 在线状态（未实现）
        console.log('收到在线状态更新:', data);
      } else {
        console.log('未处理的WebSocket消息类型:', data.type);
      }
    } catch (error) {
      console.error('处理WebSocket消息出错:', error);
    }
  };
  
  // 初始化WebSocket连接
  const { 
    status: wsStatus, 
    connect: wsConnect, 
    disconnect: wsDisconnect, 
    isConnected,
    lastMessage,
    errorMessage: wsErrorMessage
  } = useWebSocket(DEFAULT_WS_URL, handleWebSocketMessage);
  
  // 在用户登录后自动连接WebSocket
  watch(() => currentUser.value?.id, (newVal) => {
    if (newVal) {
      console.log('用户已登录，连接WebSocket');
      wsConnect();
    } else {
      console.log('用户已登出，断开WebSocket');
      wsDisconnect();
    }
  }, { immediate: true });
  
  // 组件卸载时断开WebSocket连接
  onUnmounted(() => {
    console.log('组件卸载，断开WebSocket');
    wsDisconnect();
  });

  // 计算属性
  const totalUnreadCount = computed(() => {
    return Object.values(unreadCountsMap.value).reduce((total, count) => total + count, 0);
  });

  const currentChatId = computed(() => currentConversation.value?.id || null);
  const currentChatName = computed(() => {
    if (!currentConversation.value || !currentUser.value) return '';
    return getConversationDisplayName(currentConversation.value);
  });

  // 获取会话列表
  const loadConversations = async (): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const response = await messageApi.getConversations(currentUser.value.id, 0, 20);
      
      if (response.success && response.data) {
        // 处理嵌套的响应结构
        let conversationsData: Conversation[] = [];
        
        // 检查各种可能的数据结构
        const responseData = response.data as any;
        if (Array.isArray(responseData)) {
          // 如果直接是数组
          conversationsData = responseData;
        } else if (responseData.content && Array.isArray(responseData.content)) {
          // 如果是分页结构中的content
          if (responseData.content.length > 0 && responseData.content[0].conversations) {
            // 如果content中的第一个元素包含conversations数组
            conversationsData = responseData.content[0].conversations;
          } else {
            // 否则直接使用content
            conversationsData = responseData.content;
          }
        } else if (responseData.conversations && Array.isArray(responseData.conversations)) {
          // 如果有conversations字段
          conversationsData = responseData.conversations;
        } else if (responseData.data && Array.isArray(responseData.data)) {
          // 如果有data字段
          conversationsData = responseData.data;
        }
        
        // 更新会话列表
        conversations.value = conversationsData;
        
        // 更新未读消息计数
        conversationsData.forEach(conv => {
          if (conv.id && conv.unreadCount) {
            unreadCountsMap.value[conv.id] = conv.unreadCount;
          }
        });
      } else {
        throw new Error(response.message || '获取会话列表失败');
      }
    } catch (err: any) {
      error.value = err.message || '获取会话列表失败';
      
      // 处理401错误
      if (err.status === 401) {
        // 清除认证信息并跳转到登录页
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userInfo');
        sessionStorage.removeItem('accessToken');
        sessionStorage.removeItem('refreshToken');
        sessionStorage.removeItem('userInfo');
        // 抛出特殊错误，让调用方处理导航
        throw new Error('UNAUTHORIZED');
      }
      
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  // 获取指定会话的消息
  const loadMessages = async (conversationId: number): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      console.log('请求会话消息，会话ID:', conversationId);
      const response = await messageApi.getMessages(conversationId, 0, 50);
      
      console.log('会话消息API响应:', response);
      
      if (response.success && response.data) {
        console.log('原始消息响应数据:', JSON.stringify(response.data).substring(0, 200) + '...');
        console.log('消息数据类型:', typeof response.data);
        console.log('是否有content字段:', response.data.content !== undefined);
        
        // 确定消息数组来源
        let messagesArray = [];
        if (Array.isArray(response.data)) {
          // 如果返回的是数组
          console.log('API直接返回了消息数组');
          messagesArray = response.data;
        } else if (response.data.content && Array.isArray(response.data.content)) {
          // 如果是标准分页结构
          console.log('API返回了分页结构，使用content字段');
          messagesArray = response.data.content;
        } else if (response.data.message && Array.isArray(response.data.message)) {
          // 如果消息在message字段
          console.log('API返回了message字段的数组');
          messagesArray = response.data.message;
        } else if (response.data.data && Array.isArray(response.data.data)) {
          // 如果在嵌套的data字段
          console.log('API返回了嵌套的data字段数组');
          messagesArray = response.data.data;
        } else {
          // 如果找不到消息数组，输出详细日志
          console.error('无法确定消息数组位置。响应数据:', response.data);
          
          // 强制返回空数组
          messagesArray = [];
        }
        
        console.log('提取的消息数组长度:', messagesArray.length);
        
        // 进一步检查消息对象结构
        if (messagesArray.length > 0) {
          console.log('第一条消息结构:', JSON.stringify(messagesArray[0]).substring(0, 200) + '...');
          
          // 检查是否存在嵌套消息结构
          const firstMsg = messagesArray[0];
          if (firstMsg.message && typeof firstMsg.message === 'object') {
            console.log('检测到嵌套消息结构，提取内部消息');
            messagesArray = messagesArray.map((item: any) => {
              // 合并消息对象，优先使用内部消息字段
              return { ...item, ...item.message };
            });
          }
        }
        
        // 消息按时间排序（最新的在底部）
        currentMessages.value = messagesArray.sort((a: any, b: any) => 
          new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        );
        
        console.log('处理后的消息列表，共计', currentMessages.value.length, '条消息');
      } else {
        throw new Error(response.message || '获取消息失败');
      }
    } catch (err: any) {
      error.value = err.message || '获取消息失败';
      console.error('获取消息失败:', err);
      
      if (err.status === 401) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userInfo');
        sessionStorage.removeItem('accessToken');
        sessionStorage.removeItem('refreshToken');
        sessionStorage.removeItem('userInfo');
        // 抛出特殊错误，让调用方处理导航
        throw new Error('UNAUTHORIZED');
      }
      
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  /**
   * 打开会话
   * @param conversationId 会话ID
   */
  const openConversation = async (conversationId: number) => {
    console.log('打开会话:', conversationId);
    
    if (!conversationId) {
      console.error('无效的会话ID:', conversationId);
      error.value = '无效的会话ID';
      return;
    }
    
    try {
      // 清空当前消息列表，避免显示上一个会话的消息
      currentMessages.value = [];
      
      // 设置当前会话
      const conversation = conversations.value.find(c => c.id === conversationId);
      if (conversation) {
        currentConversation.value = { ...conversation };
        
        // 立即将未读消息数重置为0（乐观更新）
        conversation.unreadCount = 0;
        unreadCountsMap.value[conversationId] = 0;
        console.log('乐观更新：已将会话未读消息数量重置为0:', conversationId);
      } else {
        // 如果在现有列表中找不到会话，先创建一个基本对象
        currentConversation.value = {
          id: conversationId,
          type: 'PRIVATE' as ConversationType,
          lastActiveTime: new Date().toISOString(),
          unreadCount: 0,
          isDnd: false,
          isPinned: false
        };
      }
      
      // 加载会话消息
      const response = await messageApi.getMessages(conversationId);
      
      if (response.success && response.data && response.data.content) {
        // 确保每条消息都有有效的数据
        const validMessages = response.data.content
          .filter(msg => msg && msg.id) // 过滤掉无效消息
          .map(msg => {
            // 确保每条消息都有必要的字段
            return {
              ...msg,
              id: msg.id || Date.now() + Math.random(),
              conversationId: msg.conversationId || conversationId,
              createdAt: msg.createdAt || new Date().toISOString(),
              type: msg.type || msg.messageType || 'TEXT',
              content: msg.content || ''
            };
          })
          .sort((a, b) => {
            // 安全地排序，处理无效日期
            const dateA = new Date(a.createdAt || 0).getTime();
            const dateB = new Date(b.createdAt || 0).getTime();
            return dateA - dateB;
          });
        
        // 更新消息列表
        currentMessages.value = validMessages;
        
        console.log('会话消息已加载，共', validMessages.length, '条消息');
        
        // 找到最新的消息ID来更新阅读光标
        if (validMessages.length > 0) {
          const lastMessageId = validMessages[validMessages.length - 1].id;
          console.log('更新阅读光标到最新消息:', lastMessageId);
          
          // 更新阅读光标
          if (lastMessageId) {
            await updateReadCursor(conversationId, lastMessageId);
            
            // 立即标记所有消息为已读
            await messageApi.markConversationAsRead(conversationId)
              .then(() => console.log('会话已标记为已读:', conversationId))
              .catch(err => console.error('标记会话已读失败:', err));
          }
        }
        
        // 确保未读消息数量为0
        if (conversation) {
          conversation.unreadCount = 0;
          unreadCountsMap.value[conversationId] = 0;
          console.log('已将会话未读消息数量重置为0:', conversationId);
        }
      } else {
        console.error('加载会话消息失败:', response.message);
        if (!response.data || !response.data.content) {
          console.warn('服务器返回的消息数据为空');
          // 设置为空数组，而不是undefined
          currentMessages.value = [];
        }
      }
    } catch (error) {
      console.error('打开会话失败:', error);
      // 确保在出错时消息列表是一个空数组，而不是undefined
      currentMessages.value = [];
    }
  };

  /**
   * 更新未读消息数量
   * @param conversationId 会话ID
   */
  const updateUnreadMessageCount = async (conversationId: number) => {
    try {
      const response = await messageApi.getUnreadMessageCount(conversationId);
      
      if (response.success && response.data !== undefined) {
        // 更新会话的未读消息数量
        const unreadCount = typeof response.data === 'number' ? response.data : 0;
        
        // 查找并更新会话的未读消息数量
        const conversation = conversations.value.find(c => c.id === conversationId);
        if (conversation) {
          conversation.unreadCount = unreadCount;
          console.log('更新会话未读消息数量:', conversationId, unreadCount);
          
          // 同时更新未读计数映射
          unreadCountsMap.value[conversationId] = unreadCount;
        }
      }
    } catch (error) {
      console.error('获取未读消息数量失败:', error);
    }
  };

  // 刷新当前会话的消息
  const refreshCurrentMessages = async (): Promise<void> => {
    if (!currentConversation.value || !currentConversation.value.id) {
      console.warn('没有活跃的会话，无法刷新消息');
      return;
    }

    try {
      console.log('刷新当前会话消息:', currentConversation.value.id);
      await loadMessages(currentConversation.value.id);
    } catch (err: any) {
      console.error('刷新消息失败:', err);
      error.value = err.message || '刷新消息失败';
      throw err;
    }
  };
  
  // 处理WebSocket实时消息
  const handleRealTimeMessage = (messageData: any) => {
    console.log('处理实时消息:', messageData);
    
    try {
      // 格式化消息对象
      const formattedMessage = formatWebSocketMessage(messageData);
      
      // 检查消息是否属于当前会话
      if (currentConversation.value && formattedMessage.conversationId === currentConversation.value.id) {
        console.log('收到当前会话的新消息:', formattedMessage);
        
        // 增强重复消息检查逻辑
        const isDuplicate = currentMessages.value.some(msg => {
          // 完全匹配ID
          if (msg.id === formattedMessage.id) {
            console.log('消息ID重复:', msg.id);
            return true;
          }
          
          // 匹配内容和发送者，且时间接近（5秒内）
          if (msg.content === formattedMessage.content && 
           msg.senderId === formattedMessage.senderId &&
              Math.abs(new Date(msg.createdAt).getTime() - new Date(formattedMessage.createdAt).getTime()) < 5000) {
            console.log('消息内容和发送者重复，时间接近:', msg.content, msg.senderId);
            return true;
          }
          
          // 如果当前消息是由当前用户发送的，且已经在消息列表中有一条内容相同的、由当前用户发送的消息
          if (formattedMessage.isSentByCurrentUser && 
              msg.senderId === currentUser.value?.id && 
              msg.content === formattedMessage.content) {
            console.log('当前用户发送的消息内容重复:', msg.content);
            return true;
          }
          
          return false;
        });
        
        if (!isDuplicate) {
          // 将消息添加到当前会话的消息列表中
          currentMessages.value = [...currentMessages.value, formattedMessage];
          console.log('消息已添加到当前会话，当前消息列表长度:', currentMessages.value.length);
          
          // 如果是当前会话，自动标记为已读并更新阅读光标
          if (formattedMessage.id) {
            // 标记消息为已读
            messageApi.markMessageAsRead(formattedMessage.id)
              .then(() => console.log('消息已标记为已读:', formattedMessage.id))
              .catch(err => console.error('标记消息已读失败:', err));
            
            // 更新阅读光标为当前最新消息
            updateReadCursor(formattedMessage.conversationId, formattedMessage.id)
              .catch(err => console.error('更新阅读光标失败:', err));
          }
        } else {
          console.log('跳过重复消息:', formattedMessage.id);
        }
      } else {
        // 如果不是当前会话，直接使用格式化后的消息对象的isSentByCurrentUser标志
        console.log(`消息不是当前会话，检查发送者: isSentByCurrentUser=${formattedMessage.isSentByCurrentUser}`);
        
        if (!formattedMessage.isSentByCurrentUser && formattedMessage.conversationId) {
          console.log('收到其他用户在其他会话中的消息，增加未读计数');
          incrementUnreadCount(formattedMessage.conversationId, formattedMessage);
        } else if (formattedMessage.isSentByCurrentUser) {
          console.log('收到当前用户在其他会话中发送的消息，不增加未读计数');
        }
      }
      
      // 更新会话最后消息
      updateConversationLastMessage(formattedMessage);
      
    } catch (error) {
      console.error('处理实时消息失败:', error);
    }
  };
  
  // 格式化WebSocket消息为标准Message对象
  const formatWebSocketMessage = (messageData: any): ExtendedMessage => {
    console.log('格式化WebSocket消息:', messageData);
    
    // 尝试从不同位置获取消息数据
    let id = messageData.id;
    let conversationId = messageData.conversationId;
    let senderId = messageData.senderId;
    let content = messageData.content;
    let messageType = messageData.messageType || messageData.type;
    let createdAt = messageData.createdAt;
    let updatedAt = messageData.updatedAt;
    let senderName = messageData.senderName || messageData.senderNickname;
    let senderAvatar = messageData.senderAvatar;
    let mediaFileId = messageData.mediaFileId;
    let isRead = messageData.isRead;
    let isSelf = messageData.isSelf;
    let isSentByCurrentUser = messageData.isSentByCurrentUser;
    
    // 如果数据在嵌套的data对象中
    if (messageData.data) {
      id = id || messageData.data.id;
      conversationId = conversationId || messageData.data.conversationId;
      senderId = senderId || messageData.data.senderId;
      content = content || messageData.data.content;
      messageType = messageType || messageData.data.messageType || messageData.data.type;
      createdAt = createdAt || messageData.data.createdAt;
      updatedAt = updatedAt || messageData.data.updatedAt;
      senderName = senderName || messageData.data.senderName || messageData.data.senderNickname;
      senderAvatar = senderAvatar || messageData.data.senderAvatar;
      mediaFileId = mediaFileId || messageData.data.mediaFileId;
      isRead = isRead !== undefined ? isRead : messageData.data.isRead;
      isSelf = isSelf !== undefined ? isSelf : messageData.data.isSelf;
      isSentByCurrentUser = isSentByCurrentUser !== undefined ? isSentByCurrentUser : messageData.data.isSentByCurrentUser;
    }
    
    // 如果数据在嵌套的message对象中
    if (messageData.message) {
      id = id || messageData.message.id;
      conversationId = conversationId || messageData.message.conversationId;
      senderId = senderId || messageData.message.senderId;
      content = content || messageData.message.content;
      messageType = messageType || messageData.message.messageType || messageData.message.type;
      createdAt = createdAt || messageData.message.createdAt;
      updatedAt = updatedAt || messageData.message.updatedAt;
      senderName = senderName || messageData.message.senderName || messageData.message.senderNickname;
      senderAvatar = senderAvatar || messageData.message.senderAvatar;
      mediaFileId = mediaFileId || messageData.message.mediaFileId;
      isRead = isRead !== undefined ? isRead : messageData.message.isRead;
      isSelf = isSelf !== undefined ? isSelf : messageData.message.isSelf;
      isSentByCurrentUser = isSentByCurrentUser !== undefined ? isSentByCurrentUser : messageData.message.isSentByCurrentUser;
    }
    
    // 确保conversationId是数字类型
    if (typeof conversationId === 'string') {
      conversationId = parseInt(conversationId, 10);
    }
    
    // 确保createdAt是有效日期
    if (!createdAt || isNaN(new Date(createdAt).getTime())) {
      createdAt = new Date().toISOString();
    }
    
    // 确保updatedAt是有效日期
    if (!updatedAt || isNaN(new Date(updatedAt).getTime())) {
      updatedAt = createdAt;
    }
    
    // 确保senderId是数字类型
    if (typeof senderId === 'string') {
      senderId = parseInt(senderId, 10);
    }
    
    // 获取当前用户ID
    const currentUserId = currentUser.value?.id ? Number(currentUser.value.id) : null;
    
    // 判断消息是否由当前用户发送 - 增强判断逻辑
    const isSentByCurrent = 
      isSelf === true || 
      isSentByCurrentUser === true || 
      (senderId !== null && currentUserId !== null && senderId === currentUserId);
    
    console.log(`检查是否为当前用户发送的消息: senderId=${senderId}, currentUserId=${currentUserId}, isSelf=${isSelf}, isSentByCurrentUser=${isSentByCurrentUser}, 结果=${isSentByCurrent}`);
    
    // 确定消息状态
    let status = messageData.status || MessageStatus.SENT;
    
    // 如果消息是由当前用户发送的，使用已发送状态
    // 如果不是当前用户发送的且在当前会话中，标记为已读
    const isInCurrentConversation = currentConversation.value?.id === conversationId;
    
    if (isSentByCurrent) {
      // 当前用户发送的消息，保持原状态
      status = status || MessageStatus.SENT;
    } else if (isInCurrentConversation) {
      // 不是当前用户发送的，且在当前会话中，应该标记为已读
      // 但实际上这个标记应该由后端处理，这里我们不修改状态
    } else {
      // 其他情况，保持原状态
      status = status || MessageStatus.DELIVERED;
    }
    
    // 创建标准消息对象
    const message: ExtendedMessage = {
      id: id || Date.now(),
      conversationId: conversationId,
      senderId: senderId,
      type: messageType || 'TEXT',
      messageType: messageType || 'TEXT',
      content: content || '',
      status: status,
      isRead: isRead || false,
      createdAt: createdAt,
      updatedAt: updatedAt,
      senderName: senderName,
      senderAvatar: senderAvatar,
      mediaFileId: mediaFileId,
      isSelf: isSentByCurrent,
      isSentByCurrentUser: isSentByCurrent
    };
    
    return message;
  };
  
  // 更新会话最后消息
  const updateConversationLastMessage = (messageData: any) => {
    console.log('更新会话最后消息:', messageData);
    
    // 获取会话ID
    const numConversationId = messageData.conversationId || messageData.data?.conversationId;
    if (!numConversationId) {
      console.warn('消息数据中没有会话ID，无法更新会话最后消息');
      return;
    }
    
    // 查找对应的会话  
    const conversation = conversations.value.find(c => c.id === numConversationId);
    
    if (!conversation) {
      console.log('在当前列表中找不到会话，尝试刷新会话列表');
      // 如果会话不存在于列表中，刷新会话列表
      loadConversations().catch(err => {
        console.warn('自动刷新会话列表失败:', err);
      });
      return;
    }
    
    // 确认会话存在且具有有效ID
    console.log('找到会话，准备更新:', conversation.id);
    
    try {
      // 获取消息内容
      let content = messageData.content;
      if (!content && messageData.data) {
        content = messageData.data.content;
      }
      
      // 获取消息类型
      let messageType = messageData.messageType || messageData.type;
      if (!messageType && messageData.data) {
        messageType = messageData.data.messageType || messageData.data.type;
      }
      
      // 获取发送者ID
      let senderId = messageData.senderId;
      if (!senderId && messageData.data) {
        senderId = messageData.data.senderId;
      }
      
      // 获取消息状态
      let status = messageData.status || messageData.data?.status;
      
      // 如果是接收到的消息且不在当前会话中，则设置为DELIVERED (已送达)
      const currentUserId = currentUser.value?.id ? Number(currentUser.value.id) : null;
      if (senderId !== currentUserId && 
          (!currentConversation.value || currentConversation.value.id !== numConversationId)) {
        status = MessageStatus.DELIVERED;
      } 
      // 如果没有状态信息，则设为默认值SENT
      else if (!status) {
        status = MessageStatus.SENT;
      }
      
      // 创建新的最后消息对象
      const lastMessageUpdate = {
        id: messageData.id || Date.now(),
        conversationId: numConversationId,
        senderId: senderId,
        type: messageType || 'TEXT',
        messageType: messageType || 'TEXT',
        content: content || '',
        status: status,
        createdAt: messageData.createdAt || new Date().toISOString(),
        updatedAt: messageData.updatedAt || new Date().toISOString()
      } as ExtendedMessage;
      
      console.log('新的最后消息:', lastMessageUpdate);
      
      // 更新会话的最后消息
      if (!conversation.lastMessage) {
        conversation.lastMessage = {} as ExtendedMessage;
      }
      
      // 保存原有的时间
      const oldCreatedAt = conversation.lastMessage.createdAt;
      
      // 复制每个属性而不是直接赋值整个对象
      Object.assign(conversation.lastMessage, lastMessageUpdate);
      
      // 更新会话的活跃时间
      const now = new Date().toISOString();
      conversation.lastActiveTime = now;
      
      console.log('会话最后消息已更新');
      
      // 将此会话移到列表顶部
      const idx = conversations.value.findIndex(c => c.id === numConversationId);
      if (idx > 0) {
        console.log('将会话移到列表顶部');
        const conv = conversations.value.splice(idx, 1)[0];
        conversations.value.unshift(conv);
      }
    } catch (error) {
      console.error('更新会话最后消息时出错:', error);
    }
  };

  // 主动检查消息状态
  const checkMessageStatus = async (messageId: number): Promise<void> => {
    try {
      // 先等待一段时间，让消息有时间被送达
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      // 获取消息状态
      const response = await messageApi.getMessageStatus(messageId);
      
      if (response.success && response.data) {
        const { status } = response.data;
        console.log(`获取到消息 ${messageId} 的状态: ${status}`);
        
        // 更新消息状态
        updateMessageStatus(messageId, status as MessageStatus);
        
        // 如果状态是SENT，继续检查
        if (status === MessageStatus.SENT) {
          // 再次检查，但间隔更长
          setTimeout(() => {
            checkMessageStatus(messageId);
          }, 5000);
        }
      } else {
        console.warn('获取消息状态失败:', response.message);
      }
    } catch (error) {
      console.error('检查消息状态出错:', error);
    }
  };

  // 发送消息
  const sendMessage = async (content: string, messageType: MessageType = 'TEXT' as MessageType, receiverId?: number): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    if (!content.trim()) {
      error.value = '消息内容不能为空';
      return;
    }

    try {
      const messageData: SendMessageRequest = {
        content: content.trim(),
        messageType,
        autoCreateConversation: true
      };

      // 如果有当前会话，使用会话ID
      if (currentConversation.value) {
        messageData.conversationId = currentConversation.value.id;
      }
      
      // 如果指定了接收者，使用接收者ID
      if (receiverId) {
        messageData.receiverId = receiverId;
      }

      console.log('发送消息请求:', messageData);
      const response = await messageApi.sendMessage(messageData);
      
      if (response.success && response.data) {
        console.log('消息发送成功, API响应:', response);
        
        /* 注释掉直接添加消息到列表的逻辑，改为由WebSocket通知处理
        // 确定适当的消息状态
        let initialStatus = MessageStatus.SENT;
        
        console.log(`消息初始状态: ${initialStatus}`);
        
        // 添加消息到当前消息列表
        const newMessage: any = {
          id: response.data.id,
          conversationId: response.data.conversationId,
          senderId: response.data.senderId,
          content: response.data.content,
          type: response.data.messageType || messageType,
          messageType: response.data.messageType || messageType,
          createdAt: response.data.createdAt,
          updatedAt: response.data.createdAt,
          status: initialStatus,
          isSentByCurrentUser: true,
          isSelf: true // 添加isSelf属性以兼容ChatMessage组件
        };
        
        // 使用新数组来触发响应式更新
        currentMessages.value = [...currentMessages.value, newMessage];
        
        console.log('消息已添加到当前会话，当前消息列表长度:', currentMessages.value.length);
        */
        
        // 如果发送成功，我们只启动消息状态检查
        if (response.data.id) {
          checkMessageStatus(response.data.id);
        }
        
        // 重新加载会话列表以更新最后消息
        setTimeout(() => {
          loadConversations();
        }, 100);
      } else {
        throw new Error(response.message || '发送消息失败');
      }
    } catch (err: any) {
      error.value = err.message || '发送消息失败';
      throw err;
    }
  };

  // 获取或创建私聊会话
  const openPrivateChat = async (contactId: number): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      const response = await messageApi.getOrCreatePrivateConversation(contactId, currentUser.value.id);
      
      if (response.success && response.data?.conversation) {
        const conversationId = response.data.conversation.id;
        await openConversation(conversationId);
        
        // 刷新会话列表
        setTimeout(() => {
          loadConversations();
        }, 500);
      } else {
        throw new Error(response.message || '创建会话失败');
      }
    } catch (err: any) {
      error.value = err.message || '创建会话失败';
      throw err;
    }
  };

  // 更新未读消息计数
  const updateUnreadCount = (conversationId: number, count: number) => {
    if (!conversationId) return;
    
    // 更新未读消息计数映射
    unreadCountsMap.value[conversationId] = count;
    
    // 如果会话列表中有该会话，也更新其未读数
    const conv = conversations.value.find(c => c.id === conversationId);
    if (conv) {
      conv.unreadCount = count;
    }
  };

  // 增加未读消息计数
  const incrementUnreadCount = (conversationId: number, messageData?: any) => {
    if (!conversationId) return;
    
    // 如果当前正在查看该会话，则不增加未读数
    if (currentConversation.value?.id === conversationId) {
      console.log(`当前正在查看会话 ${conversationId}，不增加未读计数`);
      return;
    }
    
    // 如果提供了消息数据，检查是否是当前用户发送的消息
    if (messageData) {
      // 获取当前用户ID
      const currentUserId = currentUser.value?.id ? Number(currentUser.value.id) : null;
      
      // 获取发送者ID
      const senderId = messageData.senderId ? Number(messageData.senderId) : null;
      
      // 增强检测逻辑
      const isSentByCurrentUser = 
        messageData.isSelf === true || 
        messageData.isSentByCurrentUser === true || 
        (senderId !== null && currentUserId !== null && senderId === currentUserId);
      
      console.log(`incrementUnreadCount - 消息发送者检查: senderId=${senderId}, currentUserId=${currentUserId}, isSelf=${messageData.isSelf}, isSentByCurrentUser=${messageData.isSentByCurrentUser}, 结果=${isSentByCurrentUser}`);
      
      if (isSentByCurrentUser) {
        console.log(`消息由当前用户发送，不增加会话 ${conversationId} 的未读计数`);
        return;
      }
    }
    
    // 更新未读消息计数映射
    const currentCount = unreadCountsMap.value[conversationId] || 0;
    unreadCountsMap.value[conversationId] = currentCount + 1;
    console.log(`增加会话 ${conversationId} 的未读计数: ${currentCount + 1}`);
    
    // 如果会话列表中有该会话，也更新其未读数
    const conv = conversations.value.find(c => c.id === conversationId);
    if (conv) {
      // 更新未读消息计数
      conv.unreadCount = (conv.unreadCount || 0) + 1;
      
      // 确保会话有最后活动时间
      if (!conv.lastActiveTime) {
        // 如果没有最后活动时间，从最后消息获取时间
        if (conv.lastMessage?.createdAt) {
          conv.lastActiveTime = conv.lastMessage.createdAt;
          console.log(`为会话 ${conversationId} 设置最后活动时间:`, conv.lastActiveTime);
        }
      }
    }
  };

  // 添加更新阅读光标的方法
  const updateReadCursor = async (conversationId: number, lastReadMessageId: number) => {
    if (!conversationId || !lastReadMessageId) {
      console.warn('无效的会话ID或最后读取的消息ID');
      return;
    }
    
    try {
      console.log(`更新会话 ${conversationId} 的阅读光标，最后读取的消息ID: ${lastReadMessageId}`);
      const response = await messageApi.updateReadCursor(conversationId, lastReadMessageId);
      
      if (response.success) {
        console.log(`阅读光标更新成功`);
        
        // 重置未读消息计数
        updateUnreadCount(conversationId, 0);
      } else {
        console.error(`阅读光标更新失败:`, response.message);
      }
    } catch (error) {
      console.error(`阅读光标更新出错:`, error);
    }
  };

  // 获取会话显示名称
  const getConversationDisplayName = (conversation: Conversation): string => {
    if (!conversation) return '';
    
    // 如果是私聊，显示对方的名称
    if (conversation.type === 'PRIVATE' && conversation.participants && conversation.participants.length > 0) {
      // 找到非当前用户的参与者
      const otherParticipant = conversation.participants.find(p => p.userId !== currentUser.value?.id);
      
      if (otherParticipant) {
        // 优先使用别名，然后是昵称，然后是用户名
        return otherParticipant.alias || otherParticipant.nickname || otherParticipant.username || '未知用户';
      }
    }
    
    // 如果是群聊，或者找不到对方信息，使用会话名称
    return conversation.name || '未命名会话';
  };

  // 获取会话头像
  const getConversationAvatar = (conversation: Conversation): string => {
    if (!currentUser.value || !conversation) return '';
    
    if (conversation.type === 'PRIVATE') {
      // 私聊：使用对方用户的头像
      const otherParticipant = conversation.participants?.find(p => p.userId !== currentUser.value!.id);
      if (otherParticipant?.user?.avatarUrl) {
        return otherParticipant.user.avatarUrl;
      }
      // 如果没有头像，返回空字符串，让组件显示默认头像
      return '';
    } else {
      // 群聊：使用会话头像
      return conversation.avatarUrl || '';
    }
  };

  // 格式化消息时间
  const formatMessageTime = (timeStr: string): string => {
    if (!timeStr) return '';
    
    const time = new Date(timeStr);
    const now = new Date();
    const diffMs = now.getTime() - time.getTime();
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
  };

  // 获取最后一条消息内容
  const getLastMessageContent = (lastMessage?: ExtendedMessage): string => {
    if (!lastMessage) {
      return '暂无消息';
    }
    
    // 根据消息类型显示不同内容
    const messageType = lastMessage.messageType || lastMessage.type;
    
    switch (messageType) {
      case 'TEXT':
        return lastMessage.content || '';
      case 'IMAGE':
        return '[图片]';
      case 'FILE':
        return '[文件]';
      case 'AUDIO':
      case 'VOICE':
        return '[语音]';
      case 'VIDEO':
        return '[视频]';
      default:
        return '[消息]';
    }
  };

  // 清除当前会话
  const clearCurrentConversation = (): void => {
    currentConversation.value = null;
    currentMessages.value = [];
  };

  // 清除所有数据
  const clearAllData = (): void => {
    conversations.value = [];
    currentConversation.value = null;
    currentMessages.value = [];
    error.value = null;
    unreadCountsMap.value = {};
  };

  // 更新消息状态
  const updateMessageStatus = (messageId: number, newStatus: MessageStatus, isReadValue?: boolean) => {
    // 更新当前消息列表中的消息状态
    const messageToUpdate = currentMessages.value.find(m => m.id === messageId);
    if (messageToUpdate) {
      console.log(`更新消息 ${messageId} 状态从 ${messageToUpdate.status} 到 ${newStatus}`);
      messageToUpdate.status = newStatus;
      
      // 如果提供了isRead值，也更新它
      if (isReadValue !== undefined) {
        messageToUpdate.isRead = isReadValue;
      }
    }
    
    // 如果是当前会话的最后一条消息，也更新会话列表中的状态
    if (currentConversation.value && currentConversation.value.lastMessage && 
        currentConversation.value.lastMessage.id === messageId) {
      currentConversation.value.lastMessage.status = newStatus;
      
      // 如果提供了isRead值，也更新它
      if (isReadValue !== undefined && 'isRead' in currentConversation.value.lastMessage) {
        // @ts-ignore - 类型可能没有isRead字段
        currentConversation.value.lastMessage.isRead = isReadValue;
      }
    }
    
    // 遍历会话列表，更新相应会话的最后一条消息状态
    conversations.value.forEach(conv => {
      if (conv.lastMessage && conv.lastMessage.id === messageId) {
        conv.lastMessage.status = newStatus;
        
        // 如果提供了isRead值，也更新它
        if (isReadValue !== undefined && 'isRead' in conv.lastMessage) {
          // @ts-ignore - 类型可能没有isRead字段
          conv.lastMessage.isRead = isReadValue;
        }
      }
    });
  };
  
  // 处理WebSocket状态更新消息
  const handleStatusUpdate = (data: any) => {
    if (!data || !data.messageId) {
      console.warn('无效的状态更新数据');
      return;
    }
    
    const { messageId, status } = data;
    
    if (!status || !Object.values(MessageStatus).includes(status as MessageStatus)) {
      console.warn('无效的消息状态:', status);
      return;
    }
    
    console.log(`收到消息 ${messageId} 状态更新: ${status}`);
    updateMessageStatus(messageId, status as MessageStatus);
  };

  // 处理消息撤回
  const handleMessageRecall = (data: any) => {
    if (!data || !data.messageId) {
      console.warn('无效的消息撤回数据');
      return;
    }
    
    const { messageId, conversationId } = data;
    
    console.log(`收到消息 ${messageId} 撤回通知，会话 ${conversationId}`);
    
    // 更新当前消息列表中的消息状态
    const messageToUpdate = currentMessages.value.find(m => m.id === messageId);
    if (messageToUpdate) {
      console.log(`更新消息 ${messageId} 状态为已撤回`);
      messageToUpdate.status = MessageStatus.RECALLED;
    }
    
    // 如果是当前会话的最后一条消息，也更新会话列表中的状态
    if (currentConversation.value && currentConversation.value.lastMessage && 
        currentConversation.value.lastMessage.id === messageId) {
      currentConversation.value.lastMessage.status = MessageStatus.RECALLED;
    }
    
    // 遍历会话列表，更新相应会话的最后一条消息状态
    conversations.value.forEach(conv => {
      if (conv && conv.lastMessage && conv.lastMessage.id === messageId) {
        conv.lastMessage.status = MessageStatus.RECALLED;
      }
    });
  };

  // 定期刷新会话列表，以获取最新未读消息数
  let refreshInterval: number | null = null;
  
  // 启动定时刷新
  const startRefreshInterval = () => {
    if (refreshInterval) return;
    
    refreshInterval = window.setInterval(() => {
      if (currentUser.value?.id) {
        loadConversations().catch(err => {
          console.warn('自动刷新会话列表失败:', err);
        });
      }
    }, 30000); // 每30秒刷新一次
  };
  
  // 停止定时刷新
  const stopRefreshInterval = () => {
    if (refreshInterval) {
      clearInterval(refreshInterval);
      refreshInterval = null;
    }
  };
  
  // 监听用户变化，自动启动/停止刷新
  watch(() => currentUser.value?.id, (newVal) => {
    if (newVal) {
      startRefreshInterval();
      // 用户登录，连接WebSocket
      // wsConnect(); // Removed as per edit hint
    } else {
      stopRefreshInterval();
      // 用户登出，断开WebSocket
      // wsDisconnect(); // Removed as per edit hint
    }
  }, { immediate: true });
  
  // 组件卸载时自动断开连接
  onUnmounted(() => {
    stopRefreshInterval();
    // wsDisconnect(); // Removed as per edit hint
  });

  // 更新消息已读状态
  const updateMessageReadStatus = (messageId: number, isReadValue: boolean) => {
    // 更新当前消息列表中的消息状态
    const messageToUpdate = currentMessages.value.find(m => m.id === messageId);
    if (messageToUpdate) {
      console.log(`更新消息 ${messageId} 的已读状态为 ${isReadValue}`);
      messageToUpdate.isRead = isReadValue;
    }
    
    // 如果是当前会话的最后一条消息，也更新会话列表中的状态
    if (currentConversation.value && currentConversation.value.lastMessage && 
        currentConversation.value.lastMessage.id === messageId) {
      // @ts-ignore - 类型可能没有isRead字段
      if ('isRead' in currentConversation.value.lastMessage) {
        // @ts-ignore - 类型可能没有isRead字段
        currentConversation.value.lastMessage.isRead = isReadValue;
      }
    }
    
    // 遍历会话列表，更新相应会话的最后一条消息状态
    conversations.value.forEach(conv => {
      if (conv.lastMessage && conv.lastMessage.id === messageId) {
        // @ts-ignore - 类型可能没有isRead字段
        if ('isRead' in conv.lastMessage) {
          // @ts-ignore - 类型可能没有isRead字段
          conv.lastMessage.isRead = isReadValue;
        }
      }
    });
  };

  return {
    // 状态
    conversations: readonly(conversations),
    currentConversation: readonly(currentConversation),
    currentMessages: readonly(currentMessages),
    isLoading: readonly(isLoading),
    error: readonly(error),
    unreadCountsMap: readonly(unreadCountsMap),
    wsStatus: readonly(wsStatus),
    wsConnected: isConnected,
    wsErrorMessage: readonly(wsErrorMessage),
    
    // 计算属性
    totalUnreadCount,
    currentChatId,
    currentChatName,
    
    // 方法
    loadConversations,
    loadMessages,
    openConversation,
    sendMessage,
    refreshCurrentMessages,
    openPrivateChat,
    updateUnreadCount,
    incrementUnreadCount,
    getConversationDisplayName,
    getConversationAvatar,
    formatMessageTime,
    getLastMessageContent,
    clearCurrentConversation,
    clearAllData,
    startRefreshInterval,
    stopRefreshInterval,
    wsConnect,
    wsDisconnect,
    
    // 消息状态管理
    updateMessageStatus,
    handleStatusUpdate,
    checkMessageStatus,
    updateMessageReadStatus,
    updateReadCursor
  };
}