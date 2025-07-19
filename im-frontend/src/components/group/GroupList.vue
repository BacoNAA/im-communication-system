<!-- 群组列表组件 -->
<template>
  <div class="group-list">
    <div class="group-list-header">
      <h3>我的群组</h3>
      <el-button type="primary" size="small" @click="showCreateGroupDialog">
        <el-icon><Plus /></el-icon>
        创建群组
      </el-button>
    </div>

    <el-empty v-if="groups.length === 0" description="暂无群组" />

    <el-scrollbar v-else height="calc(100vh - 200px)">
      <div class="group-list-content">
        <div
          v-for="group in groups"
          :key="group.id"
          class="group-item"
          :class="{ 'active': selectedGroupId === group.id }"
          @click="selectGroup(group)"
        >
          <el-avatar :size="40" :src="group.avatar">
            {{ group.name.substring(0, 1) }}
          </el-avatar>
          <div class="group-info">
            <div class="group-name">{{ group.name }}</div>
            <div class="group-member-count">{{ group.memberCount }}人</div>
          </div>
        </div>
      </div>
    </el-scrollbar>

    <!-- 创建群组对话框 -->
    <create-group-dialog
      v-model:visible="createDialogVisible"
      @created="handleGroupCreated"
    />
  </div>
</template>

<script lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { getUserGroups } from '@/api/group';
import CreateGroupDialog from './CreateGroupDialog.vue';
import useGroupUpdates from '@/composables/useGroupUpdates';
import type { GroupUpdateEvent } from '@/composables/useGroupUpdates';

interface Group {
  id: number;
  name: string;
  avatar?: string;
  avatarUrl?: string;
  description?: string;
  memberCount: number;
  createdAt: string;
}

export default {
  name: 'GroupList',
  components: {
    Plus,
    CreateGroupDialog
  },
  emits: ['select-group'],
  setup(_, { emit }) {
    // 群组列表
    const groups = ref<Group[]>([]);
    
    // 当前选中的群组ID
    const selectedGroupId = ref<number | null>(null);
    
    // 创建群组对话框可见性
    const createDialogVisible = ref(false);
    
    // 分页参数
    const page = ref(0);
    const size = ref(20);
    const hasMore = ref(true);
    const loading = ref(false);

    // 使用群组更新composable
    const { addListener, removeListener } = useGroupUpdates();
    
    // 处理群组更新事件
    const handleGroupUpdate = (event: GroupUpdateEvent) => {
      console.log('GroupList接收到群组更新事件:', event);
      
      if (event.updateType === 'UPDATE') {
        // 更新现有群组信息
        const index = groups.value.findIndex(g => g.id === event.groupId);
        if (index !== -1 && event.data) {
          const updatedData = event.data as Record<string, any>;
          const currentGroup = groups.value[index];
          
          if (currentGroup) {
            const updatedGroup: Group = {
              id: currentGroup.id,
              name: updatedData.name || currentGroup.name,
              memberCount: currentGroup.memberCount,
              createdAt: currentGroup.createdAt
            };
            
            // 添加可选属性
            if (updatedData.avatarUrl || currentGroup.avatarUrl) {
              updatedGroup.avatarUrl = updatedData.avatarUrl || currentGroup.avatarUrl;
              updatedGroup.avatar = updatedData.avatarUrl || currentGroup.avatar;
            }
            
            if (updatedData.description || currentGroup.description) {
              updatedGroup.description = updatedData.description || currentGroup.description;
            }
            
            groups.value[index] = updatedGroup;
          }
        }
      } else if (event.updateType === 'NEW') {
        // 添加新群组
        if (event.data && typeof event.data === 'object' && 'id' in event.data) {
          const groupData = event.data as Record<string, any>;
          
          if (groupData.id && !groups.value.some(g => g.id === groupData.id)) {
            const newGroup: Group = {
              id: groupData.id as number,
              name: (groupData.name as string) || '未命名群组',
              memberCount: (groupData.memberCount as number) || 0,
              createdAt: (groupData.createdAt as string) || new Date().toISOString()
            };
            
            // 添加可选属性
            if (groupData.avatarUrl) {
              newGroup.avatarUrl = groupData.avatarUrl as string;
              newGroup.avatar = groupData.avatarUrl as string;
            }
            
            if (groupData.description) {
              newGroup.description = groupData.description as string;
            }
            
            groups.value.unshift(newGroup);
          }
        }
      } else if (event.updateType === 'DELETE') {
        // 删除群组
        const index = groups.value.findIndex(g => g.id === event.groupId);
        if (index !== -1) {
          groups.value.splice(index, 1);
          
          // 如果当前选中的是被删除的群组，清除选择
          if (selectedGroupId.value === event.groupId) {
            selectedGroupId.value = null;
          }
        }
      }
    };

    // 加载群组列表
    const loadGroups = async (reset = false) => {
      if (reset) {
        page.value = 0;
        groups.value = [];
        hasMore.value = true;
      }

      if (!hasMore.value || loading.value) return;

      loading.value = true;
      try {
        console.log('开始加载群组列表，页码:', page.value, '每页大小:', size.value);
        
        const response = await getUserGroups({
          page: page.value,
          size: size.value
        });

        console.log('获取群组列表原始响应:', JSON.stringify(response));

        if (response.code === 200 && response.data) {
          console.log('群组列表数据结构:', JSON.stringify(response.data, null, 2));
          
          // 尝试从不同位置获取群组数据
          let groupsData = [];
          
          if (Array.isArray(response.data)) {
            console.log('响应数据是数组格式');
            groupsData = response.data;
          } else if (response.data.content && Array.isArray(response.data.content)) {
            console.log('响应数据是标准分页格式');
            groupsData = response.data.content;
          } else if (response.data[0] && Array.isArray(response.data[0].content)) {
            console.log('响应数据是嵌套格式');
            groupsData = response.data[0].content;
          } else if (response.data.data && Array.isArray(response.data.data)) {
            console.log('响应数据是另一种嵌套格式');
            groupsData = response.data.data;
          } else {
            // 尝试遍历响应数据的所有属性，查找可能的数组
            console.log('尝试查找响应中的数组数据');
            for (const key in response.data) {
              if (Array.isArray(response.data[key])) {
                console.log(`在响应的${key}属性中找到数组数据`);
                groupsData = response.data[key];
                break;
              } else if (response.data[key] && typeof response.data[key] === 'object') {
                for (const subKey in response.data[key]) {
                  if (Array.isArray(response.data[key][subKey])) {
                    console.log(`在响应的${key}.${subKey}属性中找到数组数据`);
                    groupsData = response.data[key][subKey];
                    break;
                  }
                }
              }
            }
          }
          
          console.log('提取的群组数据:', groupsData);
          console.log('提取的群组数据类型:', typeof groupsData);
          console.log('提取的群组数据是否为数组:', Array.isArray(groupsData));
          console.log('提取的群组数据长度:', groupsData ? groupsData.length : 0);
          
          if (groupsData && groupsData.length > 0) {
            // 将获取的数据映射到前端需要的格式
            const mappedGroups = groupsData.map((group: any) => {
              console.log('处理群组数据:', group);
              return {
                id: group.id,
                name: group.name || '未命名群组',
                avatar: group.avatarUrl,
                avatarUrl: group.avatarUrl,
                description: group.description || '',
                memberCount: group.memberCount || 0,
                createdAt: group.createdAt || new Date().toISOString()
              };
            });
            
            console.log('映射后的群组数据:', mappedGroups);
            groups.value = [...groups.value, ...mappedGroups];
            console.log('更新后的groups.value:', groups.value);
            console.log('更新后的groups.length:', groups.value.length);
            
            // 更新分页信息
            if (response.data.last !== undefined) {
              hasMore.value = !response.data.last;
            } else {
              // 如果没有明确的分页信息，假设没有更多数据
              hasMore.value = false;
            }
            
            page.value++;
          } else {
            console.log('未找到群组数据或数组为空');
            hasMore.value = false;
          }
        } else {
          console.error('获取群组列表失败:', response.message);
          ElMessage.error(response.message || '获取群组列表失败');
        }
      } catch (error) {
        console.error('获取群组列表失败:', error);
        ElMessage.error('获取群组列表失败，请稍后重试');
      } finally {
        loading.value = false;
        console.log('加载完成，当前群组列表长度:', groups.value.length);
      }
    };

    // 选择群组
    const selectGroup = (group: Group) => {
      selectedGroupId.value = group.id;
      emit('select-group', group.id);
    };

    // 显示创建群组对话框
    const showCreateGroupDialog = () => {
      createDialogVisible.value = true;
    };

    // 处理群组创建成功
    const handleGroupCreated = (group: Group) => {
      groups.value.unshift(group);
      ElMessage.success('群组创建成功');
      selectGroup(group);
    };

    // 组件挂载时加载群组列表和添加WebSocket监听
    onMounted(() => {
      console.log('GroupList组件已挂载，开始加载群组列表');
      loadGroups();
      
      // 添加WebSocket监听
      addListener(handleGroupUpdate);
    });
    
    // 组件卸载时移除WebSocket监听
    onUnmounted(() => {
      removeListener(handleGroupUpdate);
    });

    return {
      groups,
      selectedGroupId,
      createDialogVisible,
      loadGroups,
      selectGroup,
      showCreateGroupDialog,
      handleGroupCreated
    };
  }
};
</script>

<style scoped>
.group-list {
  height: 100%;
  border-right: 1px solid #ebeef5;
}

.group-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  border-bottom: 1px solid #ebeef5;
}

.group-list-content {
  padding: 10px;
}

.group-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.group-item:hover {
  background-color: #f5f7fa;
}

.group-item.active {
  background-color: #ecf5ff;
}

.group-info {
  margin-left: 10px;
  overflow: hidden;
}

.group-name {
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.group-member-count {
  font-size: 12px;
  color: #909399;
}
</style> 