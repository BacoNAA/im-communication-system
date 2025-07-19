<!-- 创建群组对话框 -->
<template>
  <div class="create-group-dialog">
    <el-dialog
      v-model="dialogVisible"
      title="创建群组"
      width="500px"
      :before-close="handleClose"
    >
      <el-form
        ref="formRef"
        :model="groupForm"
        :rules="rules"
        label-width="80px"
        class="group-form"
      >
        <el-form-item label="群名称" prop="name">
          <el-input v-model="groupForm.name" placeholder="请输入群组名称" />
        </el-form-item>

        <el-form-item label="群头像">
          <el-upload
            class="avatar-uploader"
            action="/api/public-files/upload-group-avatar"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
            :headers="uploadHeaders"
            :with-credentials="true"
            :data="uploadData"
          >
            <img v-if="groupForm.avatarUrl" :src="groupForm.avatarUrl" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="群描述" prop="description">
          <el-input
            v-model="groupForm.description"
            type="textarea"
            placeholder="请输入群组描述"
            :rows="3"
          />
        </el-form-item>

        <el-form-item label="入群审批">
          <el-switch v-model="groupForm.requiresApproval" />
        </el-form-item>

        <el-form-item label="群成员" prop="memberIds">
          <div v-if="contactsLoading" class="loading-contacts">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>正在加载联系人...</span>
          </div>
          <div v-else-if="isContactsEmpty" class="no-contacts">
            <el-icon><Warning /></el-icon>
            <span>没有可选择的联系人</span>
            <div class="debug-info">
              <small>调试信息: 联系人数量: {{ contactsCount }}</small>
              <br>
              <small>contacts.value是否存在: {{ hasContactsValue }}</small>
            </div>
          </div>
          <div v-else>
            <div class="debug-info">
              <small>联系人数量: {{ contactsCount }}</small>
            </div>
          <el-select
            v-model="groupForm.memberIds"
            multiple
            filterable
            placeholder="请选择群成员"
            class="member-select"
          >
            <el-option
                v-for="contact in contactsList"
                :key="contact.friend.id"
                :label="contact.alias || contact.friend.nickname || contact.friend.email"
                :value="contact.friend.id"
            >
              <div class="user-option">
                  <el-avatar :size="30" :src="contact.friend.avatarUrl" />
                  <span class="user-nickname">{{ contact.alias || contact.friend.nickname || contact.friend.email }}</span>
              </div>
            </el-option>
          </el-select>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleClose">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitting">
            创建
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { ref, reactive, watch, computed, onMounted } from 'vue';
import type { ElForm } from 'element-plus';
import { ElMessage } from 'element-plus';
import { Plus, Loading, Warning } from '@element-plus/icons-vue';
import { createGroup } from '@/api/group';
import { useContacts } from '@/composables/useContacts';
import { useAuth } from '@/composables/useAuth';
import { contactApi } from '@/api/contact';
import type { Contact, ContactStatus } from '@/api/contact';
import { storage } from '@/utils';
import type { User } from '@/types';

type FormInstance = InstanceType<typeof ElForm>;

interface GroupForm {
  name: string;
  description: string;
  avatarUrl: string;
  requiresApproval: boolean;
  memberIds: number[];
}

export default {
  name: 'CreateGroupDialog',
  components: {
    Plus,
    Loading,
    Warning
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:visible', 'created'],
  setup(props, { emit }) {
    // 对话框可见性
    const dialogVisible = ref(props.visible);

    // 表单引用
    const formRef = ref<FormInstance | null>(null);
    
    // 上传头部配置 - 改为计算属性
    const uploadHeaders = computed(() => ({
      Authorization: `Bearer ${localStorage.getItem('accessToken') || localStorage.getItem('auth_token') || sessionStorage.getItem('accessToken') || sessionStorage.getItem('auth_token') || ''}`
    }));
    
    // 生成一个临时的群组ID
    const tempGroupId = ref(Date.now());
    
    // 上传数据
    const uploadData = computed(() => ({
      groupId: tempGroupId.value
    }));

    // 表单数据
    const groupForm = reactive<GroupForm>({
      name: '',
      description: '',
      avatarUrl: '',
      requiresApproval: false,
      memberIds: []
    });

    // 表单验证规则
    const rules = {
      name: [
        { required: true, message: '请输入群组名称', trigger: 'blur' },
        { min: 1, max: 50, message: '长度在1到50个字符之间', trigger: 'blur' }
      ],
      description: [
        { max: 500, message: '长度不能超过500个字符', trigger: 'blur' }
      ],
      memberIds: [
        { required: true, message: '请选择至少一名群成员', trigger: 'change' },
        { type: 'array', min: 1, message: '请选择至少一名群成员', trigger: 'change' }
      ]
    };

    // 联系人相关 - 修改为单例模式
    const contactsComposable = useContacts();
    const contacts = contactsComposable.contacts;
    const contactsLoading = ref(false);
    const { currentUser, getCurrentUserId } = useAuth();

    // 计算属性：联系人列表
    const contactsList = computed(() => {
      // 使用类型断言来处理只读数组
      const contactsArray = Array.isArray(contacts.value) ? contacts.value : [];
      return contactsArray;
    });

    // 计算属性：联系人列表是否为空
    const isContactsEmpty = computed(() => {
      // 使用contactsList计算属性
      return contactsList.value.length === 0;
    });

    // 计算属性：联系人数量
    const contactsCount = computed(() => {
      return contactsList.value.length;
    });

    // 计算属性：contacts.value是否存在
    const hasContactsValue = computed(() => {
      return !!contacts.value;
    });

    // 调试函数：显示所有可能的用户ID来源
    const debugUserIdSources = () => {
      console.group('调试用户ID来源');
      
      // 从currentUser获取
      console.log('currentUser:', currentUser.value);
      
      // 从localStorage直接获取userId
      console.log('localStorage.userId:', localStorage.getItem('userId'));
      
      // 从sessionStorage直接获取userId
      console.log('sessionStorage.userId:', sessionStorage.getItem('userId'));
      
      // 从localStorage的current_user获取
      try {
        const currentUserStr = localStorage.getItem('current_user');
        console.log('localStorage.current_user原始值:', currentUserStr);
        if (currentUserStr) {
          const currentUserObj = JSON.parse(currentUserStr);
          console.log('localStorage.current_user解析后:', currentUserObj);
        }
      } catch (e) {
        console.error('解析localStorage.current_user失败:', e);
      }
      
      // 从sessionStorage的current_user获取
      try {
        const currentUserStr = sessionStorage.getItem('current_user');
        console.log('sessionStorage.current_user原始值:', currentUserStr);
        if (currentUserStr) {
          const currentUserObj = JSON.parse(currentUserStr);
          console.log('sessionStorage.current_user解析后:', currentUserObj);
        }
      } catch (e) {
        console.error('解析sessionStorage.current_user失败:', e);
      }
      
      // 从localStorage的userInfo获取
      try {
        const userInfoStr = localStorage.getItem('userInfo');
        console.log('localStorage.userInfo原始值:', userInfoStr);
        if (userInfoStr) {
          const userInfo = JSON.parse(userInfoStr);
          console.log('localStorage.userInfo解析后:', userInfo);
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败:', e);
      }
      
      // 从sessionStorage的userInfo获取
      try {
        const userInfoStr = sessionStorage.getItem('userInfo');
        console.log('sessionStorage.userInfo原始值:', userInfoStr);
        if (userInfoStr) {
          const userInfo = JSON.parse(userInfoStr);
          console.log('sessionStorage.userInfo解析后:', userInfo);
        }
      } catch (e) {
        console.error('解析sessionStorage.userInfo失败:', e);
      }
      
      // 获取认证令牌
      const token = localStorage.getItem('accessToken') || 
                   localStorage.getItem('auth_token') || 
                   sessionStorage.getItem('accessToken') || 
                   sessionStorage.getItem('auth_token');
      console.log('认证令牌存在:', !!token);
      
      // 使用getCurrentUserId获取
      const userId = getCurrentUserId();
      console.log('getCurrentUserId()返回的用户ID:', userId);
      
      console.groupEnd();
      
      return userId;
    };

    // 测试方法：用于在控制台中调试联系人数据
    const testContacts = () => {
      console.group('测试联系人数据');
      console.log('contacts:', contacts);
      console.log('contacts.value:', contacts.value);
      console.log('contactsList.value:', contactsList.value);
      console.log('isContactsEmpty.value:', isContactsEmpty.value);
      console.log('contactsCount.value:', contactsCount.value);
      console.groupEnd();
      
      // 返回一个可以在控制台中调用的函数，用于手动设置联系人数据
      return {
        setTestContacts: (testData: any[]) => {
          console.log('手动设置测试联系人数据:', testData);
          (contacts as any).value = testData;
          console.log('设置后的联系人数量:', contactsList.value.length);
          console.log('设置后的联系人列表为空?', isContactsEmpty.value);
        }
      };
    };

    // 将测试方法挂载到window对象上，方便在控制台中调用
    if (typeof window !== 'undefined') {
      (window as any).__testContacts = testContacts();
      (window as any).__directLoadContacts = async (userId: number) => {
        return directLoadContacts(userId);
      };
    }

    // 监听对话框可见性
    watch(() => props.visible, async (newVal) => {
      dialogVisible.value = newVal;
      if (newVal) {
        console.log('对话框打开，即将加载联系人列表');
        
        // 使用调试函数获取用户ID
        const userId = debugUserIdSources();
        console.log('对话框打开时获取的用户ID:', userId);
        
        if (!userId) {
          console.warn('未找到有效的用户ID，可能未登录');
          ElMessage.warning('请先登录后再创建群组');
          emit('update:visible', false); // 关闭对话框
          return;
        }
        
        // 检查联系人是否已加载
        if (contactsCount.value > 0) {
          console.log('联系人已加载，数量:', contactsCount.value);
        } else {
          console.log('联系人未加载，开始加载');
          
          // 尝试直接获取联系人
          const processedContacts = await directLoadContacts(userId);
          
          if (processedContacts && processedContacts.length > 0) {
            console.log('对话框打开时直接获取联系人成功，数量:', processedContacts.length);
            
            // 手动更新contacts的值
            (contacts as any).value = processedContacts;
            
            // 检查是否成功更新
            console.log('对话框打开时更新后的联系人数量:', contactsList.value.length);
          } else {
            // 使用loadContactsList加载
            console.log('对话框打开时直接获取联系人失败，使用loadContactsList');
            await loadContactsList();
          }
        }
        
        // 重新生成临时群组ID
        tempGroupId.value = Date.now();
      }
    }, { immediate: true });

    // 确保在组件挂载时初始化用户信息
    onMounted(async () => {
      // 检查用户ID是否存在
      const userId = debugUserIdSources();
      console.log('组件挂载时获取的用户ID:', userId);
      
      // 预先加载联系人数据
      if (userId) {
        console.log('组件挂载时预先加载联系人数据');
        
        // 尝试直接获取联系人
        const processedContacts = await directLoadContacts(userId);
        
        if (processedContacts && processedContacts.length > 0) {
          console.log('组件挂载时直接获取联系人成功，数量:', processedContacts.length);
          
          // 手动更新contacts的值
          (contacts as any).value = processedContacts;
          
          // 检查是否成功更新
          console.log('组件挂载时更新后的联系人数量:', contactsList.value.length);
        } else {
          // 尝试使用contactsComposable
          console.log('组件挂载时直接获取联系人失败，尝试使用contactsComposable');
          contactsComposable.loadContacts(false, userId).catch(err => {
            console.error('组件挂载时预加载联系人失败:', err);
          });
        }
      }
      
      // 测试联系人数据
      testContacts();
    });

    // 加载联系人列表
    const loadContactsList = async () => {
      try {
        contactsLoading.value = true;
        
        // 使用调试函数获取用户ID
        const userId = debugUserIdSources();
        console.log('加载联系人列表时获取的用户ID:', userId);
        
        if (!userId) {
          console.error('未找到用户ID');
          ElMessage.warning('请先登录后再创建群组');
          return;
        }
        
        console.log('准备加载联系人列表，用户ID:', userId);
        
        // 尝试方法1：使用contactsComposable加载联系人，手动传入用户ID
        await contactsComposable.loadContacts(false, userId);
        
        // 检查是否成功加载联系人
        console.log('方法1加载后的联系人数量:', contactsCount.value);
        
        if (contactsCount.value === 0) {
          console.warn('方法1加载联系人失败，尝试方法2：直接调用API');
          
          // 尝试方法2：直接调用API获取联系人
          const processedContacts = await directLoadContacts(userId);
          
          if (processedContacts && processedContacts.length > 0) {
            console.log('方法2加载成功，联系人数量:', processedContacts.length);
            
            // 手动更新contacts的值
            (contacts as any).value = processedContacts;
            
            // 检查是否成功更新
            console.log('更新后的联系人数量:', contactsList.value.length);
          } else {
            console.warn('方法2加载联系人失败，尝试方法3：使用contactApi');
            
            // 尝试方法3：使用contactApi
            const response = await contactApi.getContacts(userId, false);
            console.log('方法3 API调用结果:', response);
            
            if (response.success && response.data && response.data.length > 0) {
              console.log('方法3加载成功，联系人数量:', response.data.length);
              
              // 手动更新contacts的值
              (contacts as any).value = response.data;
              
              // 检查是否成功更新
              console.log('更新后的联系人数量:', contactsList.value.length);
            } else {
              console.error('所有方法都失败，无法加载联系人');
              ElMessage.error('无法加载联系人列表');
            }
          }
        }
      } catch (error) {
        console.error('加载联系人列表失败:', error);
        ElMessage.error('加载联系人列表失败，请刷新页面重试');
      } finally {
        contactsLoading.value = false;
      }
    };

    // 提交状态
    const submitting = ref(false);

    // 监听对话框可见性变化，同步到父组件
    watch(dialogVisible, (newVal) => {
      emit('update:visible', newVal);
    });

    // 关闭对话框
    const handleClose = () => {
      dialogVisible.value = false;
      resetForm();
    };

    // 重置表单
    const resetForm = () => {
      if (formRef.value) {
        formRef.value.resetFields();
      }
      groupForm.name = '';
      groupForm.description = '';
      groupForm.avatarUrl = '';
      groupForm.requiresApproval = false;
      groupForm.memberIds = [];
    };

    // 头像上传前的验证
    const beforeAvatarUpload = (file: File) => {
      const isImage = file.type.startsWith('image/');
      const isLt2M = file.size / 1024 / 1024 < 2;
      
      console.log('准备上传头像文件:', file.name, file.type, file.size);
      console.log('当前认证头:', uploadHeaders.value);
      
      // 检查认证令牌是否存在
      const token = localStorage.getItem('accessToken') || 
                    localStorage.getItem('auth_token') || 
                    sessionStorage.getItem('accessToken') || 
                    sessionStorage.getItem('auth_token');
      
      if (!token) {
        console.warn('警告: 未找到认证令牌，上传可能会失败');
        ElMessage.warning('请先登录再上传文件');
        return false;
      }

      if (!isImage) {
        ElMessage.error('头像必须是图片格式!');
        return false;
      }
      if (!isLt2M) {
        ElMessage.error('头像大小不能超过2MB!');
        return false;
      }
      return true;
    };

    // 头像上传成功
    const handleAvatarSuccess = (response: any) => {
      console.log('头像上传成功:', response);
      
      if (response.code === 200 && response.data) {
        // 根据后端返回的数据结构处理
        if (response.data.avatarUrl) {
          // 从PublicFileUploadController返回的avatarUrl字段
          groupForm.avatarUrl = response.data.avatarUrl;
        } else if (response.data.imageUrl) {
          // 兼容性字段
          groupForm.avatarUrl = response.data.imageUrl;
        } else if (typeof response.data === 'string') {
          // 如果直接返回URL字符串
        groupForm.avatarUrl = response.data;
        } else if (response.data.url) {
          // 如果返回对象中包含url字段
          groupForm.avatarUrl = response.data.url;
        } else if (response.data.fileUrl) {
          // 如果返回对象中包含fileUrl字段
          groupForm.avatarUrl = response.data.fileUrl;
        } else {
          console.error('无法从响应中获取头像URL:', response);
          ElMessage.error('头像上传成功，但无法获取URL');
        }
      } else {
        console.error('头像上传失败:', response);
        ElMessage.error(response.message || '头像上传失败');
      }
    };

    // 提交表单
    const submitForm = async () => {
      if (!formRef.value) return;
      
      await formRef.value.validate(async (valid: boolean) => {
        if (valid) {
          submitting.value = true;
          try {
            // 获取认证令牌
            const authTokens = [
              localStorage.getItem('accessToken'),
              localStorage.getItem('auth_token'),
              localStorage.getItem('token'),
              sessionStorage.getItem('accessToken'),
              sessionStorage.getItem('auth_token'),
              sessionStorage.getItem('token')
            ].filter(Boolean);
            
            const token = authTokens[0];
            
            if (!token) {
              ElMessage.error('您尚未登录或登录已过期，请重新登录');
              submitting.value = false;
              return;
            }
            
            // 获取用户ID
            const userId = getCurrentUserId();
            if (!userId || userId <= 0) {
              ElMessage.error('未找到有效的用户ID，无法创建群组，请重新登录');
              submitting.value = false;
              return;
            }
            
            console.log('准备创建群组，用户ID:', userId, '令牌:', token.substring(0, 10) + '...');
            
            // 确保至少有一个成员（后端要求）
            if (!groupForm.memberIds || groupForm.memberIds.length === 0) {
              // 如果没有选择成员，至少包含自己
              groupForm.memberIds = [userId];
            } else if (!groupForm.memberIds.includes(userId)) {
              // 确保创建者也是群成员
              groupForm.memberIds.push(userId);
            }
            
            // 构建符合API要求的数据结构
            const groupData = {
              name: groupForm.name,
              description: groupForm.description || '',
              avatarUrl: groupForm.avatarUrl || '', // 使用avatarUrl字段
              memberIds: groupForm.memberIds.map(id => Number(id)), // 确保是数字类型
              requiresApproval: groupForm.requiresApproval !== undefined ? groupForm.requiresApproval : false,
              creatorId: userId // 显式添加创建者ID
            };
            
            console.log('准备提交的群组数据:', groupData);
            
            // 使用createGroup API
            try {
              const response = await createGroup(groupData);
              console.log('创建群组成功:', response);
              ElMessage.success('群组创建成功');
              emit('created', response.data);
              handleClose();
            } catch (error: any) {
              console.error('创建群组失败:', error);
              
              // 提供更详细的错误信息
              let errorMessage = '创建群组失败，请稍后重试';
              if (error && error.message) {
                errorMessage = `创建群组失败: ${error.message}`;
                
                // 如果是认证错误，提示用户重新登录
                if (error.message.includes('UNAUTHORIZED') || 
                    error.message.includes('未授权') || 
                    error.message.includes('未找到有效的认证令牌') ||
                    error.message.includes('登录已过期')) {
                  errorMessage = '登录已过期，请重新登录';
                  // 可以在这里添加重定向到登录页面的逻辑
                }
              }
              
              ElMessage.error(errorMessage);
            }
          } catch (error) {
            console.error('创建群组失败:', error);
            ElMessage.error('创建群组失败，请稍后重试');
          } finally {
            submitting.value = false;
          }
        }
      });
    };

    // 直接获取联系人，绕过useContacts
    const directLoadContacts = async (userId: number) => {
      console.log('直接获取联系人，用户ID:', userId);
      
      try {
        // 获取认证令牌
        const token = localStorage.getItem('accessToken') || 
                     localStorage.getItem('auth_token') || 
                     sessionStorage.getItem('accessToken') || 
                     sessionStorage.getItem('auth_token');
                     
        if (!token) {
          console.error('directLoadContacts: 未找到认证令牌');
          return null;
        }
        
        // 设置请求头
        const headers = {
          'X-User-Id': userId.toString(),
          'Authorization': `Bearer ${token}`
        };
        
        // 直接使用fetch API调用
        const response = await fetch(`/api/contacts?userId=${userId}&includeBlocked=false`, {
          method: 'GET',
          headers
        });
        
        if (!response.ok) {
          console.error('directLoadContacts: 请求失败', response.status, response.statusText);
          return null;
        }
        
        const data = await response.json();
        console.log('directLoadContacts: 响应数据', data);
        
        if (data.success && data.data && data.data.length > 0) {
          console.log('directLoadContacts: 获取到联系人数量', data.data.length);
          
          // 处理联系人数据
          const processedContacts = data.data.map((contact: any) => {
            // 确保好友信息存在
            if (!contact.friend) {
              console.warn('联系人缺少friend字段:', contact);
              return null;
            }
            
            // 确保ID是有效的数字
            let friendId = contact.friend.id;
            if (typeof friendId === 'string') {
              friendId = parseInt(friendId);
              if (isNaN(friendId)) {
                console.warn('好友ID无效(字符串):', contact);
                return null;
              }
            } else if (friendId === undefined || friendId === null) {
              console.warn('好友ID无效(undefined/null):', contact);
              return null;
            } else if (typeof friendId !== 'number') {
              console.warn('好友ID类型不是数字:', typeof friendId, contact);
              return null;
            }
            
            // 确保ID是有效的数字
            friendId = Number(friendId);
            
            // 返回处理后的联系人对象
            return {
              id: contact.id,
              userId: contact.userId,
              friendId: friendId,
              alias: contact.alias,
              status: contact.status,
              createdAt: contact.createdAt,
              updatedAt: contact.updatedAt,
              friend: {
                id: friendId,
                email: contact.friend.email,
                nickname: contact.friend.nickname || contact.friend.email?.split('@')[0] || `用户${friendId}`,
                avatarUrl: contact.friend.avatarUrl || '',
                personalId: contact.friend.personalId,
                status: contact.friend.status,
                signature: contact.friend.signature
              },
              tags: contact.tags || []
            };
          }).filter(Boolean); // 过滤掉null值
          
          return processedContacts;
        }
        
        return null;
      } catch (error) {
        console.error('directLoadContacts: 异常', error);
        return null;
      }
    };

    // 直接使用fetch API创建群组
    const directCreateGroup = async (groupData: any) => {
      console.log('使用fetch API直接创建群组:', groupData);
      
      try {
        // 获取认证令牌
        const token = localStorage.getItem('accessToken') || 
                     localStorage.getItem('auth_token') || 
                     sessionStorage.getItem('accessToken') || 
                     sessionStorage.getItem('auth_token');
                     
        if (!token) {
          console.error('directCreateGroup: 未找到认证令牌');
          return { success: false, message: '未找到认证令牌' };
        }
        
        // 获取用户ID
        const userId = getCurrentUserId();
        
        // 设置请求头
        const headers: Record<string, string> = {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        };
        
        if (userId > 0) {
          headers['X-User-Id'] = String(userId);
        }
        
        // 尝试不同的API路径
        const apiPaths = [
          '/api/groups/create',
          '/api/groups',
          '/api/group/create',
          '/api/group',
          '/api/chat/groups/create',
          '/api/chat/groups',
          '/api/chat/group/create',
          '/api/chat/group'
        ];
        
        // 尝试不同的数据结构
        const payloads = [
          // 结构1：标准结构
          {
            name: groupData.name,
            description: groupData.description || '',
            avatar: groupData.avatar || '',
            memberIds: groupData.memberIds || [],
            requiresApproval: groupData.requiresApproval !== undefined ? groupData.requiresApproval : false,
            creatorId: userId > 0 ? userId : undefined
          },
          // 结构2：简化结构
          {
            name: groupData.name,
            description: groupData.description || '',
            avatar: groupData.avatar || '',
            memberIds: groupData.memberIds || []
          },
          // 结构3：使用members代替memberIds
          {
            name: groupData.name,
            description: groupData.description || '',
            avatar: groupData.avatar || '',
            members: groupData.memberIds || []
          }
        ];
        
        // 依次尝试不同的API路径和数据结构
        for (const path of apiPaths) {
          for (const payload of payloads) {
            try {
              console.log(`尝试API路径: ${path}, 数据结构:`, JSON.stringify(payload, null, 2));
              console.log('请求头:', headers);
              
              const response = await fetch(path, {
                method: 'POST',
                headers,
                body: JSON.stringify(payload)
              });
              
              console.log('响应状态:', response.status, response.statusText);
              
              if (response.ok) {
                const data = await response.json();
                console.log('API调用成功:', data);
                return { success: true, data };
              } else {
                const errorText = await response.text();
                console.error('API调用失败:', errorText);
              }
            } catch (error) {
              console.error(`API路径 ${path} 使用数据结构 ${JSON.stringify(payload)} 调用失败:`, error);
            }
          }
        }
        
        return { success: false, message: '所有API路径和数据结构组合都调用失败' };
      } catch (error) {
        console.error('directCreateGroup: 异常', error);
        return { success: false, message: String(error) };
      }
    };

    // 使用XMLHttpRequest直接发送请求
    const sendDirectRequest = (url: string, data: any, headers: Record<string, string> = {}): Promise<any> => {
      return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.open('POST', url, true);
        
        // 设置请求头
        Object.keys(headers).forEach(key => {
          if (headers[key] !== undefined) {
            xhr.setRequestHeader(key, headers[key]);
          }
        });
        
        // 如果没有设置Content-Type，默认设置为application/json
        if (!headers['Content-Type']) {
          xhr.setRequestHeader('Content-Type', 'application/json');
        }
        
        // 设置超时
        xhr.timeout = 30000;
        
        // 监听请求完成
        xhr.onload = function() {
          console.log('XHR响应状态:', xhr.status);
          console.log('XHR响应头:', xhr.getAllResponseHeaders());
          console.log('XHR响应内容:', xhr.responseText);
          
          if (xhr.status >= 200 && xhr.status < 300) {
            try {
              const response = JSON.parse(xhr.responseText);
              resolve(response);
            } catch (e) {
              console.error('解析XHR响应JSON失败:', e);
              resolve({ code: 200, message: '请求成功，但解析响应失败', data: xhr.responseText });
            }
          } else {
            console.error('XHR请求失败:', xhr.status, xhr.statusText);
            try {
              const errorResponse = JSON.parse(xhr.responseText);
              reject(errorResponse);
            } catch (e) {
              reject({ code: xhr.status, message: xhr.statusText || '请求失败' });
            }
          }
        };
        
        // 监听错误
        xhr.onerror = function() {
          console.error('XHR请求错误');
          reject({ code: 0, message: '网络错误' });
        };
        
        // 监听超时
        xhr.ontimeout = function() {
          console.error('XHR请求超时');
          reject({ code: 408, message: '请求超时' });
        };
        
        // 发送请求
        const requestBody = typeof data === 'string' ? data : JSON.stringify(data);
        console.log('XHR请求URL:', url);
        console.log('XHR请求头:', headers);
        console.log('XHR请求体:', requestBody);
        xhr.send(requestBody);
      });
    };

    return {
      dialogVisible,
      formRef,
      groupForm,
      rules,
      contacts,
      contactsLoading,
      loading: contactsLoading,
      submitting,
      handleClose,
      beforeAvatarUpload,
      handleAvatarSuccess,
      submitForm,
      uploadHeaders,
      uploadData,
      isContactsEmpty,
      contactsList,
      contactsCount,
      hasContactsValue,
      directLoadContacts,
      directCreateGroup,
      sendDirectRequest
    };
  }
};
</script>

<style scoped>
.group-form {
  margin-top: 20px;
}

.avatar-uploader {
  display: flex;
  justify-content: center;
}

.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  text-align: center;
  line-height: 100px;
}

.avatar {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover;
}

.member-select {
  width: 100%;
}

.user-option {
  display: flex;
  align-items: center;
}

.user-nickname {
  margin-left: 10px;
}

.loading-contacts {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  color: #909399;
}

.loading-contacts .el-icon {
  margin-right: 8px;
}

.no-contacts {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  color: #909399;
}

.no-contacts .el-icon {
  margin-right: 8px;
}

.debug-info {
  margin-top: 5px;
  color: #999;
  font-size: 12px;
}
</style> 