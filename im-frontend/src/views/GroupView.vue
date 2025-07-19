<!-- 群组视图 -->
<template>
  <div class="group-view">
    <el-container>
      <el-aside width="300px">
        <GroupList @select-group="handleGroupSelected" />
      </el-aside>
      <el-main>
        <div v-if="activeGroupId" class="group-content">
          <div class="group-header">
            <div class="group-info">
              <el-avatar :size="60">
                G
              </el-avatar>
              <div class="group-details">
                <h2>群组 #{{ activeGroupId }}</h2>
                <div class="group-meta">
                  {{ members.length }} 成员
                </div>
              </div>
            </div>
            <div class="group-actions" v-if="isCurrentUserOwnerOrAdmin">
              <el-button type="primary" @click="showEditForm = true">
                修改群资料
                </el-button>
            </div>
          </div>

          <!-- 群组编辑表单 -->
          <el-dialog
            v-model="showEditForm"
            title="修改群资料"
            width="600px"
            destroy-on-close
          >
            <GroupEditForm
              :group-id="activeGroupId"
              :initial-data="currentGroup"
              :is-owner="isCurrentUserOwner"
              @update:success="handleUpdateSuccess"
              @cancel="showEditForm = false"
            />
          </el-dialog>

          <el-tabs v-model="activeTab" class="group-tabs">
            <el-tab-pane label="群聊" name="chat">
              <div class="group-chat">
                <el-button type="primary" @click="goToConversation">
                  进入群聊
                </el-button>
              </div>
            </el-tab-pane>
            <el-tab-pane label="成员" name="members">
              <div class="group-members">
                <el-tabs type="card">
                  <el-tab-pane label="全部成员">
                <el-table
                  v-loading="loading"
                  :data="members"
                  style="width: 100%"
                >
                  <el-table-column label="成员" min-width="200">
                    <template #default="scope">
                      <div class="member-info">
                        <el-avatar :size="40" :src="scope.row.avatarUrl">
                          {{ scope.row.nickname?.substring(0, 1) || scope.row.username?.substring(0, 1) }}
                        </el-avatar>
                        <div>
                          <div class="member-name">{{ scope.row.nickname || scope.row.username }}</div>
                          <div class="member-role">
                            <el-tag
                                  v-if="scope.row.role === 'OWNER' || scope.row.role === 'owner'"
                              type="danger"
                              size="small"
                            >
                              群主
                            </el-tag>
                            <el-tag
                                  v-else-if="scope.row.role === 'ADMIN' || scope.row.role === 'admin'"
                              type="warning"
                              size="small"
                            >
                              管理员
                            </el-tag>
                          </div>
                        </div>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column label="加入时间" width="180">
                    <template #default="scope">
                          {{ scope.row.joinedAt ? new Date(scope.row.joinedAt).toLocaleString() : '未知' }}
                    </template>
                  </el-table-column>
                      <el-table-column label="操作" width="180" v-if="isCurrentUserOwnerOrAdmin">
                    <template #default="scope">
                          <div class="member-actions">
                      <el-button
                        v-if="canManageMember(scope.row)"
                        type="danger"
                        size="small"
                        @click="handleRemoveMember(scope.row)"
                      >
                        移除
                      </el-button>
                            <el-dropdown 
                              v-if="canSetAdmin(scope.row) || canCancelAdmin(scope.row)"
                              trigger="click"
                              @command="(command: string) => handleAdminCommand(command, scope.row)"
                            >
                              <el-button type="primary" size="small">
                                管理员操作
                                <el-icon class="el-icon--right"><arrow-down /></el-icon>
                      </el-button>
                              <template #dropdown>
                                <el-dropdown-menu>
                                  <el-dropdown-item 
                                    v-if="canSetAdmin(scope.row)" 
                                    command="set-admin"
                                  >
                                    设为管理员
                                  </el-dropdown-item>
                                  <el-dropdown-item 
                                    v-if="canCancelAdmin(scope.row)" 
                                    command="cancel-admin"
                                  >
                                    取消管理员
                                  </el-dropdown-item>
                                </el-dropdown-menu>
                              </template>
                            </el-dropdown>
                            
                            <!-- 添加禁言操作下拉菜单 -->
                            <el-dropdown 
                              v-if="canManageMute(scope.row)"
                              trigger="click"
                              @command="(command: string) => handleMuteCommand(command, scope.row)"
                            >
                      <el-button
                                :type="scope.row.isMuted ? 'warning' : 'info'" 
                        size="small"
                      >
                                {{ scope.row.isMuted ? '已禁言' : '禁言' }}
                                <el-icon class="el-icon--right"><arrow-down /></el-icon>
                      </el-button>
                              <template #dropdown>
                                <el-dropdown-menu>
                                  <el-dropdown-item 
                                    v-if="!scope.row.isMuted" 
                                    command="mute-custom"
                                  >
                                    设置禁言
                                  </el-dropdown-item>
                                  <el-dropdown-item 
                                    v-if="!scope.row.isMuted" 
                                    command="mute-10"
                                  >
                                    禁言10分钟
                                  </el-dropdown-item>
                                  <el-dropdown-item 
                                    v-if="!scope.row.isMuted" 
                                    command="mute-30"
                                  >
                                    禁言30分钟
                                  </el-dropdown-item>
                                  <el-dropdown-item 
                                    v-if="!scope.row.isMuted" 
                                    command="mute-60"
                                  >
                                    禁言1小时
                                  </el-dropdown-item>
                                  <el-dropdown-item 
                                    v-if="!scope.row.isMuted" 
                                    command="mute-1440"
                                  >
                                    禁言24小时
                                  </el-dropdown-item>
                                  <el-dropdown-item 
                                    v-if="scope.row.isMuted" 
                                    command="unmute"
                                  >
                                    解除禁言
                                  </el-dropdown-item>
                                </el-dropdown-menu>
                              </template>
                            </el-dropdown>
                          </div>
                    </template>
                  </el-table-column>
                </el-table>
                  </el-tab-pane>
                  <el-tab-pane label="管理员">
                    <el-table
                      :data="adminMembers"
                      style="width: 100%"
                    >
                      <el-table-column label="成员" min-width="200">
                        <template #default="scope">
                          <div class="member-info">
                            <el-avatar :size="40" :src="scope.row.avatarUrl">
                              {{ scope.row.nickname?.substring(0, 1) || scope.row.username?.substring(0, 1) }}
                            </el-avatar>
                            <div>
                              <div class="member-name">{{ scope.row.nickname || scope.row.username }}</div>
                              <div class="member-role">
                                <el-tag
                                  v-if="scope.row.role === 'OWNER' || scope.row.role === 'owner'"
                                  type="danger"
                                  size="small"
                                >
                                  群主
                                </el-tag>
                                <el-tag
                                  v-else
                                  type="warning"
                                  size="small"
                                >
                                  管理员
                                </el-tag>
                              </div>
                            </div>
                          </div>
                        </template>
                      </el-table-column>
                    </el-table>
                  </el-tab-pane>
                </el-tabs>
                <div class="pagination-container">
                  <el-pagination
                    background
                    layout="prev, pager, next"
                    :total="total"
                    :page-size="pageSize"
                    :current-page="currentPage"
                    @current-change="handlePageChange"
                    @size-change="handleSizeChange"
                  />
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="公告" name="announcements">
              <div class="group-announcements">
                <div class="announcements-header">
                  <h3>群公告</h3>
                  <el-button 
                    v-if="isCurrentUserOwnerOrAdmin" 
                    type="primary" 
                    @click="showAnnouncementForm = true"
                  >
                    发布公告
                  </el-button>
                </div>
                
                <!-- 置顶公告 -->
                <div v-if="pinnedAnnouncement" class="pinned-announcement">
                  <div class="announcement-card pinned">
                    <div class="announcement-header">
                      <div class="announcement-title">
                        <el-tag type="danger">置顶</el-tag>
                        <h4>{{ pinnedAnnouncement.title }}</h4>
                      </div>
                      <div class="announcement-actions" v-if="isCurrentUserOwnerOrAdmin">
                        <el-button-group>
                          <el-button size="small" type="primary" @click="handleAnnouncementAction('edit', pinnedAnnouncement)">
                            <el-icon><Edit /></el-icon> 编辑
                          </el-button>
                          <el-button size="small" type="warning" @click="handleAnnouncementAction('unpin', pinnedAnnouncement)">
                            <el-icon><Top /></el-icon> 取消置顶
                          </el-button>
                          <el-button size="small" type="danger" @click="handleAnnouncementAction('delete', pinnedAnnouncement)">
                            <el-icon><Delete /></el-icon> 删除
                          </el-button>
                        </el-button-group>
                      </div>
                    </div>
                    <div class="announcement-content" v-html="pinnedAnnouncement.content"></div>
                    <div class="announcement-footer">
                      <span class="announcement-author">{{ pinnedAnnouncement.authorName || `用户#${pinnedAnnouncement.authorId}` }}</span>
                      <span class="announcement-time">{{ formatDate(pinnedAnnouncement.updatedAt) }}</span>
                    </div>
                  </div>
                </div>
                
                <!-- 公告列表 -->
                <div v-loading="loadingAnnouncements" class="announcements-list">
                  <el-empty v-if="announcements.length === 0 && !loadingAnnouncements" description="暂无公告" />
                  <div 
                    v-for="announcement in announcements" 
                    :key="announcement.id" 
                    class="announcement-card"
                    v-show="!pinnedAnnouncement || announcement.id !== pinnedAnnouncement.id"
                  >
                    <div class="announcement-header">
                      <div class="announcement-title">
                        <h4>{{ announcement.title }}</h4>
                      </div>
                      <div class="announcement-actions" v-if="isCurrentUserOwnerOrAdmin">
                        <el-button-group>
                          <el-button size="small" type="primary" @click="handleAnnouncementAction('edit', announcement)">
                            <el-icon><Edit /></el-icon> 编辑
                          </el-button>
                          <el-button size="small" type="success" @click="handleAnnouncementAction('pin', announcement)">
                            <el-icon><Top /></el-icon> 置顶
                          </el-button>
                          <el-button size="small" type="danger" @click="handleAnnouncementAction('delete', announcement)">
                            <el-icon><Delete /></el-icon> 删除
                          </el-button>
                        </el-button-group>
                      </div>
                    </div>
                    <div class="announcement-content" v-html="announcement.content"></div>
                    <div class="announcement-footer">
                      <span class="announcement-author">{{ announcement.authorName || `用户#${announcement.authorId}` }}</span>
                      <span class="announcement-time">{{ formatDate(announcement.updatedAt) }}</span>
                    </div>
                  </div>
                  
                  <!-- 分页 -->
                  <div class="pagination-container" v-if="announcementTotal > 0">
                    <el-pagination
                      background
                      layout="prev, pager, next"
                      :total="announcementTotal"
                      :page-size="announcementPageSize"
                      :current-page="announcementCurrentPage"
                      @current-change="handleAnnouncementPageChange"
                    />
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
        <el-empty v-else description="请选择或创建一个群组" />
      </el-main>
    </el-container>
    
    <!-- 移除成员确认对话框 -->
    <el-dialog
      v-model="showRemoveMemberDialog"
      title="移除成员确认"
      width="400px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div class="remove-member-dialog-content">
        <div class="member-info-preview" v-if="memberToRemove">
          <el-avatar :size="50" :src="memberToRemove.avatarUrl">
            {{ memberToRemove.nickname?.substring(0, 1) || memberToRemove.username?.substring(0, 1) }}
          </el-avatar>
          <div class="member-info-text">
            <div class="member-name-large">{{ memberToRemove.nickname || memberToRemove.username }}</div>
            <div class="member-role-tag">
              <el-tag
                v-if="memberToRemove.role === 'ADMIN' || memberToRemove.role === 'admin'"
                type="warning"
                size="small"
              >
                管理员
              </el-tag>
              <el-tag v-else size="small">成员</el-tag>
            </div>
          </div>
        </div>
        <div class="remove-warning">
          <el-alert
            type="warning"
            :closable="false"
            show-icon
          >
            <p>确定要将该成员移出群组吗？</p>
            <p>移出后，该成员将无法再接收和发送群消息。</p>
          </el-alert>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelRemoveMember">取消</el-button>
          <el-button type="danger" @click="confirmRemoveMember" :loading="removingMember">
            确认移除
          </el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 禁言设置对话框 -->
    <el-dialog
      v-model="showMuteDialog"
      title="设置禁言时间"
      width="400px"
      :close-on-click-modal="false"
    >
      <div class="mute-dialog-content">
        <div class="member-info-preview" v-if="memberToMute">
          <el-avatar :size="50">
            {{ memberToMute.nickname?.substring(0, 1) || memberToMute.username?.substring(0, 1) || 'U' }}
          </el-avatar>
          <div class="member-info-text">
            <div class="member-name-large">{{ memberToMute.nickname || memberToMute.username || `用户${memberToMute.userId}` }}</div>
          </div>
        </div>
        
        <el-form :model="muteForm" label-width="80px">
          <el-form-item label="禁言时长">
            <el-input-number
              v-model="muteForm.minutes"
              :min="1"
              :max="10080"
              :step="5"
              step-strictly
            />
            <span class="mute-unit">分钟</span>
          </el-form-item>
          <el-form-item>
            <div class="mute-presets">
              <el-button size="small" @click="muteForm.minutes = 10">10分钟</el-button>
              <el-button size="small" @click="muteForm.minutes = 30">30分钟</el-button>
              <el-button size="small" @click="muteForm.minutes = 60">1小时</el-button>
              <el-button size="small" @click="muteForm.minutes = 1440">1天</el-button>
              <el-button size="small" @click="muteForm.minutes = 10080">1周</el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelMute">取消</el-button>
          <el-button type="primary" @click="confirmMute" :loading="settingMute">
            确认禁言
          </el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 公告表单对话框 -->
    <el-dialog
      v-model="showAnnouncementForm"
      :title="editingAnnouncement ? '编辑公告' : '发布公告'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="announcementForm" label-width="80px" ref="announcementFormRef">
        <el-form-item label="标题" prop="title" :rules="[{ required: true, message: '请输入公告标题', trigger: 'blur' }]">
          <el-input v-model="announcementForm.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content" :rules="[{ required: true, message: '请输入公告内容', trigger: 'blur' }]">
          <el-input
            v-model="announcementForm.content"
            type="textarea"
            rows="6"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="announcementForm.isPinned" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelAnnouncementForm">取消</el-button>
          <el-button type="primary" @click="submitAnnouncementForm" :loading="submittingAnnouncement">
            {{ editingAnnouncement ? '保存' : '发布' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 删除公告确认对话框 -->
    <el-dialog
      v-model="showDeleteAnnouncementDialog"
      title="删除公告"
      width="400px"
      :close-on-click-modal="false"
    >
      <div class="delete-announcement-content">
        <el-alert
          type="warning"
          :closable="false"
          show-icon
        >
          <p>确定要删除该公告吗？</p>
          <p>标题: {{ announcementToDelete?.title }}</p>
          <p>该操作不可恢复。</p>
        </el-alert>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelDeleteAnnouncement">取消</el-button>
          <el-button type="danger" @click="confirmDeleteAnnouncement" :loading="deletingAnnouncement">
            确认删除
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown, More, Edit, Top, Delete } from '@element-plus/icons-vue';
import GroupList from '@/components/group/GroupList.vue';
import GroupEditForm from '@/components/group/GroupEditForm.vue';
import { 
  getGroupById, 
  getGroupMembers, 
  getGroupAdmins,
  dissolveGroup,
  removeGroupMember, 
  setGroupAdmin,
  setMemberMuteStatus,
  getGroupAnnouncements,
  getPinnedAnnouncements,
  publishAnnouncement as createGroupAnnouncement,
  updateAnnouncement as updateGroupAnnouncement,
  deleteAnnouncement as deleteGroupAnnouncement,
  pinAnnouncement as setGroupAnnouncementPinStatus
} from '@/api/group';
import { useGroupUpdates } from '@/composables/useGroupUpdates';
import type { GroupUpdateEvent } from '@/composables/useGroupUpdates';

// GroupMember类型定义
interface GroupMember {
  id: number;
  userId: number;
  username: string;
  nickname?: string;
  avatarUrl?: string;
  role: string;
  joinedAt: string;
  isMuted?: boolean;
  mutedUntil?: string;
}

export default {
  name: 'GroupView',
  components: {
    GroupList,
    GroupEditForm
  },
  setup() {
    const route = useRouter();
    const activeGroupId = ref<number | null>(null);
    const members = ref<GroupMember[]>([]);
    const loading = ref(false);
    const currentPage = ref(1);
    const pageSize = ref(10);
    const total = ref(0);
    const searchKeyword = ref('');
    const activeTab = ref('members');
    
    // 当前选中的群组信息
    const currentGroup = ref<any>(null);
    
    // 显示编辑表单
    const showEditForm = ref(false);
    
    // 获取当前用户ID
    const currentUserId = ref<number>(0);
    
    // 移除成员相关变量
    const showRemoveMemberDialog = ref(false);
    const memberToRemove = ref<GroupMember | null>(null);
    const removingMember = ref(false);
    
    // 禁言相关变量
    const showMuteDialog = ref(false);
    const memberToMute = ref<GroupMember | null>(null);
    const settingMute = ref(false);
    const muteForm = ref({
      minutes: 10
    });
    
    // 公告相关变量
    const showAnnouncementForm = ref(false);
    const editingAnnouncement = ref(false);
    const announcementForm = ref({
      title: '',
      content: '',
      isPinned: false
    });
    const announcementToDelete = ref<any>(null);
    const showDeleteAnnouncementDialog = ref(false);
    const submittingAnnouncement = ref(false);
    const deletingAnnouncement = ref(false);

    // 公告列表相关变量
    const announcements = ref<any[]>([]);
    const loadingAnnouncements = ref(false);
    const announcementCurrentPage = ref(1);
    const announcementPageSize = ref(10);
    const announcementTotal = ref(0);

    // 置顶公告
    const pinnedAnnouncement = ref<any>(null);

    // 计算属性：成员头像URL（确保不为undefined）
    const memberAvatarUrl = computed(() => {
      return memberToMute.value?.avatarUrl || '';
    });
    
    // 使用群组更新composable
    const { addListener, removeListener } = useGroupUpdates();
    
    // 处理群组更新事件
    const handleGroupUpdate = (event: GroupUpdateEvent) => {
      console.log('接收到群组更新事件:', event);
      
      // 如果是当前查看的群组，则刷新数据
      if (activeGroupId.value && event.groupId === activeGroupId.value) {
        if (event.updateType === 'UPDATE') {
          ElMessage.success('群组信息已更新');
          
          // 更新当前群组信息
          if (event.data) {
            currentGroup.value = event.data;
          }
          
          // 刷新成员列表
          loadMembers();
        } else if (event.updateType === 'DELETE') {
          ElMessage.warning('该群组已被解散');
          route.push('/groups');
        } else if (event.updateType === 'MEMBER_JOIN' || event.updateType === 'MEMBER_LEAVE' || event.updateType === 'MEMBER_REMOVED') {
          // 刷新成员列表
          loadMembers();
        } else if (event.updateType === 'MEMBER_MUTED' || event.updateType === 'MEMBER_UNMUTED') {
          // 刷新成员列表
          loadMembers();
          
          // 显示提示消息
          const memberName = event.data?.nickname || event.data?.username || `用户#${event.data?.userId}`;
          if (event.updateType === 'MEMBER_MUTED') {
            ElMessage.warning(`成员 ${memberName} 已被禁言`);
          } else {
            ElMessage.success(`成员 ${memberName} 已解除禁言`);
          }
        } else if (event.updateType === 'ANNOUNCEMENT_NEW') {
          // 新增公告
          ElMessage.success('有新的群公告');
          
          // 如果当前在公告标签页，刷新公告列表
          if (activeTab.value === 'announcements') {
            loadAnnouncements();
          }
        } else if (event.updateType === 'ANNOUNCEMENT_UPDATE') {
          // 更新公告
          
          // 如果当前在公告标签页，刷新公告列表
          if (activeTab.value === 'announcements') {
            loadAnnouncements();
          }
        } else if (event.updateType === 'ANNOUNCEMENT_DELETE') {
          // 删除公告
          
          // 如果当前在公告标签页，刷新公告列表
          if (activeTab.value === 'announcements') {
            loadAnnouncements();
          }
        } else if (event.updateType === 'ANNOUNCEMENT_PINNED') {
          // 置顶公告
          ElMessage.success('有新的置顶公告');
          
          // 如果当前在公告标签页，刷新公告列表
          if (activeTab.value === 'announcements') {
            loadAnnouncements();
          }
        } else if (event.updateType === 'ANNOUNCEMENT_UNPINNED') {
          // 取消置顶公告
          
          // 如果当前在公告标签页，刷新公告列表
          if (activeTab.value === 'announcements') {
            loadAnnouncements();
          }
        }
      }
    };
    
    // 在组件挂载时添加监听器
    onMounted(() => {
      addListener(handleGroupUpdate);
      
      // 获取当前用户ID
      const userInfoStr = localStorage.getItem('userInfo');
      if (userInfoStr) {
        try {
          const userInfo = JSON.parse(userInfoStr);
          if (userInfo && userInfo.id) {
            currentUserId.value = userInfo.id;
          }
        } catch (e) {
          console.error('解析用户信息失败:', e);
        }
      }
      
      // 从路由参数获取群组ID
      if (route.currentRoute.value.params.id) {
        activeGroupId.value = parseInt(route.currentRoute.value.params.id as string);
        loadMembers();
        loadGroupDetail();
      }
    });
    
    // 在组件卸载时移除监听器
    onUnmounted(() => {
      removeListener(handleGroupUpdate);
    });

    // 加载群成员
    const loadMembers = async () => {
      if (!activeGroupId.value) return;
      
      loading.value = true;
      try {
        const params: Record<string, any> = {
          page: currentPage.value - 1,
          size: pageSize.value
        };
        
        if (searchKeyword.value) {
          params.keyword = searchKeyword.value;
        }
        
        const response = await getGroupMembers(activeGroupId.value, params);
        
        if (response.code === 200) {
          members.value = response.data.content || [];
          total.value = response.data.totalElements || 0;
        } else {
          ElMessage.error(response.message || '获取群成员失败');
        }
      } catch (error) {
        console.error('获取群成员失败:', error);
        ElMessage.error('获取群成员失败，请稍后重试');
      } finally {
        loading.value = false;
      }
    };

    // 处理页码变化
    const handlePageChange = (page: number) => {
      currentPage.value = page;
      loadMembers();
    };

    // 处理每页条数变化
    const handleSizeChange = (size: number) => {
      pageSize.value = size;
      currentPage.value = 1;
      loadMembers();
    };

    // 处理搜索
    const handleSearch = () => {
      currentPage.value = 1;
      loadMembers();
    };

    // 处理群组选择
    const handleGroupSelected = (groupId: number) => {
      activeGroupId.value = groupId;
      loadMembers();
      loadGroupDetail();
      
      // 如果当前是公告标签页，则加载公告
      if (activeTab.value === 'announcements') {
        loadAnnouncements();
      }
    };
    
    // 监听标签页变化
    watch(activeTab, (newTab) => {
      if (newTab === 'announcements' && activeGroupId.value) {
        loadAnnouncements();
      }
    });

    // 加载群组详情
    const loadGroupDetail = async () => {
      if (!activeGroupId.value) return;
      
      try {
        const response = await getGroupById(activeGroupId.value);
        if (response.code === 200) {
          currentGroup.value = response.data;
        } else {
          ElMessage.error(response.message || '获取群组详情失败');
        }
      } catch (error) {
        console.error('获取群组详情失败:', error);
        ElMessage.error('获取群组详情失败，请稍后重试');
      }
    };

    // 解散群组
    const handleDissolveGroup = async () => {
      if (!activeGroupId.value) return;
      
      try {
        await ElMessageBox.confirm('确定要解散该群组吗？此操作不可逆', '警告', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        });
        
        // const response = await dissolveGroup(activeGroupId.value); // 此函数已移除
        // if (response.code === 200) {
        //   ElMessage.success('群组已解散');
        //   activeGroupId.value = null;
        //   members.value = [];
        // } else {
        //   ElMessage.error(response.message || '解散群组失败');
        // }
        ElMessage.warning('解散群组功能待实现'); // 暂时禁用
      } catch (error) {
        if (error !== 'cancel') {
          console.error('解散群组失败:', error);
          ElMessage.error('解散群组失败，请稍后重试');
        }
      }
    };

    // 移除成员
    const handleRemoveMember = async (member: GroupMember) => {
      if (!activeGroupId.value) return;
      
      // 再次检查权限，防止UI状态与实际权限不一致
      if (!canManageMember(member)) {
        ElMessage.error('您没有权限移除该成员');
        return;
      }
      
      // 设置要移除的成员并显示确认对话框
      memberToRemove.value = member;
      showRemoveMemberDialog.value = true;
    };

    // 设置或取消管理员
    const handleToggleAdmin = async (member: GroupMember) => {
      if (!activeGroupId.value) return;
      
      const isAdmin = member.role === 'ADMIN' || member.role === 'admin';
      const action = isAdmin ? '取消' : '设置';
      const memberName = member.nickname || member.username;
      
      try {
        await ElMessageBox.confirm(`确定要${action} ${memberName} 的管理员权限吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        });
        
        loading.value = true;
        const response = await setGroupAdmin(activeGroupId.value, member.userId, !isAdmin);
        
        if (response.code === 200) {
          ElMessage.success(`已${action} ${memberName} 的管理员权限`);
          
          // 重新加载成员列表以确保数据一致
          loadMembers();
        } else {
          ElMessage.error(response.message || `${action}管理员失败`);
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error(`${action}管理员失败:`, error);
          ElMessage.error(`${action}管理员失败，请稍后重试`);
        }
      } finally {
        loading.value = false;
      }
    };

    // 处理管理员操作命令
    const handleAdminCommand = async (command: string, member: GroupMember) => {
      if (!activeGroupId.value) return;
      
      let actionText = '';
      if (command === 'set-admin') {
        actionText = '设置';
      } else if (command === 'cancel-admin') {
        actionText = '取消';
      } else {
        return; // 无效命令
      }

      try {
        await ElMessageBox.confirm(`确定要${actionText} ${member.nickname || member.username} 的管理员权限吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        });
        
        loading.value = true;
        let response;
        
        try {
          if (command === 'set-admin') {
            response = await setGroupAdmin(activeGroupId.value, member.userId, true);
          } else if (command === 'cancel-admin') {
            response = await setGroupAdmin(activeGroupId.value, member.userId, false);
          }

          if (response && response.code === 200) {
            ElMessage.success(`已${actionText} ${member.nickname || member.username} 的管理员权限`);
            loadMembers(); // 重新加载成员列表
        } else {
            const errorMsg = response?.message || `${actionText}管理员失败`;
            ElMessage.error(errorMsg);
            console.error(`${actionText}管理员失败:`, errorMsg);
          }
        } catch (apiError: any) {
          console.error(`${actionText}管理员API错误:`, apiError);
          
          // 提取错误消息
          let errorMessage = `${actionText}管理员失败`;
          if (apiError && apiError.message) {
            errorMessage = apiError.message;
          }
          
          // 显示更友好的错误消息
          if (errorMessage.includes('只有群主才能设置管理员')) {
            ElMessage.error('只有群主才能设置或取消管理员');
          } else if (errorMessage.includes('不能对自己进行此操作')) {
            ElMessage.error('不能对自己进行此操作');
          } else if (errorMessage.includes('用户不是群成员')) {
            ElMessage.error('该用户不是群成员');
          } else {
            ElMessage.error(errorMessage);
          }
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error(`${actionText}管理员失败:`, error);
          ElMessage.error(`${actionText}管理员失败，请稍后重试`);
        }
      } finally {
        loading.value = false;
      }
    };

    // 检查是否为群主
    const isGroupOwner = (member: GroupMember) => {
      return member.role === 'OWNER' || member.role === 'owner';
    };

    // 检查是否为管理员
    const isGroupAdmin = (member: GroupMember) => {
      return member.role === 'ADMIN' || member.role === 'admin';
    };

    // 检查是否可以设置为管理员（普通成员且当前用户是群主）
    const canSetAdmin = (member: GroupMember) => {
      // 当前成员不能是自己
      if (member.userId === currentUserId.value) return false;
      
      // 当前成员必须是普通成员
      const isMemberRegular = member.role === 'MEMBER' || member.role === 'member';
      
      // 当前用户必须是群主
      const currentMember = members.value.find(m => m.userId === currentUserId.value);
      const isCurrentUserOwner = currentMember && (currentMember.role === 'OWNER' || currentMember.role === 'owner');
      
      return isCurrentUserOwner && isMemberRegular;
    };

    // 检查是否可以取消管理员（管理员且当前用户是群主）
    const canCancelAdmin = (member: GroupMember) => {
      // 当前成员不能是自己
      if (member.userId === currentUserId.value) return false;
      
      // 当前成员必须是管理员
      const isMemberAdmin = member.role === 'ADMIN' || member.role === 'admin';
      
      // 当前用户必须是群主
      const currentMember = members.value.find(m => m.userId === currentUserId.value);
      const isCurrentUserOwner = currentMember && (currentMember.role === 'OWNER' || currentMember.role === 'owner');
      
      return isCurrentUserOwner && isMemberAdmin;
    };

    // 管理员成员列表（包括群主）
    const adminMembers = computed(() => {
      return members.value.filter(member => 
        member.role === 'OWNER' || 
        member.role === 'owner' || 
        member.role === 'ADMIN' || 
        member.role === 'admin'
      );
    });

    // 检查当前用户是否为群主或管理员
    const isCurrentUserOwnerOrAdmin = computed(() => {
      if (!currentUserId.value) return false;
      
      return members.value.some(member => 
        member.userId === currentUserId.value && 
        (member.role === 'OWNER' || member.role === 'owner' || member.role === 'ADMIN' || member.role === 'admin')
      );
    });

    // 检查是否可管理成员
    const canManageMember = (member: GroupMember) => {
      // 不能移除自己
      if (member.userId === currentUserId.value) {
        return false;
      }
      
      // 获取当前用户的成员信息
      const currentMember = members.value.find(m => m.userId === currentUserId.value);
      if (!currentMember) {
        return false;
      }
      
      // 检查当前用户是否为群主
      const isCurrentUserOwner = currentMember.role === 'OWNER' || currentMember.role === 'owner';
      
      // 检查当前用户是否为管理员
      const isCurrentUserAdmin = currentMember.role === 'ADMIN' || currentMember.role === 'admin';
      
      // 检查目标成员是否为群主
      const isTargetOwner = member.role === 'OWNER' || member.role === 'owner';
      
      // 检查目标成员是否为管理员
      const isTargetAdmin = member.role === 'ADMIN' || member.role === 'admin';
      
      // 群主可以移除任何人（除了自己）
      if (isCurrentUserOwner) {
        return !isTargetOwner; // 群主不能被移除
      }
      
      // 管理员只能移除普通成员
      if (isCurrentUserAdmin) {
        return !isTargetOwner && !isTargetAdmin; // 管理员不能移除群主和其他管理员
      }
      
      // 普通成员不能移除任何人
      return false;
    };

    // 检查是否可以管理禁言
    const canManageMute = (member: GroupMember) => {
      // 不能禁言自己
      if (member.userId === currentUserId.value) {
        return false;
      }
      
      // 获取当前用户的成员信息
      const currentMember = members.value.find(m => m.userId === currentUserId.value);
      if (!currentMember) {
        return false;
      }
      
      // 检查当前用户是否为群主
      const isCurrentUserOwner = currentMember.role === 'OWNER' || currentMember.role === 'owner';
      
      // 检查当前用户是否为管理员
      const isCurrentUserAdmin = currentMember.role === 'ADMIN' || currentMember.role === 'admin';
      
      // 检查目标成员是否为群主
      const isTargetOwner = member.role === 'OWNER' || member.role === 'owner';
      
      // 检查目标成员是否为管理员
      const isTargetAdmin = member.role === 'ADMIN' || member.role === 'admin';
      
      // 群主可以禁言任何人（除了自己）
      if (isCurrentUserOwner) {
        return !isTargetOwner; // 群主不能被禁言
      }
      
      // 管理员只能禁言普通成员
      if (isCurrentUserAdmin) {
        return !isTargetOwner && !isTargetAdmin; // 管理员不能禁言群主和其他管理员
      }
      
      // 普通成员不能禁言任何人
      return false;
    };

    // 处理禁言命令
    const handleMuteCommand = (command: string, member: GroupMember) => {
      if (!activeGroupId.value) return;
      
      if (command === 'mute-custom') {
        // 打开禁言设置对话框
        memberToMute.value = member;
        muteForm.value.minutes = 10; // 默认10分钟
        showMuteDialog.value = true;
      } else if (command === 'unmute') {
        // 直接解除禁言
        confirmUnmute(member);
      } else {
        // 直接设置预设时间的禁言
        const minutes = parseInt(command.split('-')[1]);
        if (!isNaN(minutes)) {
          setMute(member, minutes);
        }
      }
    };

    // 取消禁言设置
    const cancelMute = () => {
      showMuteDialog.value = false;
      memberToMute.value = null;
    };

    // 确认禁言设置
    const confirmMute = () => {
      if (!memberToMute.value || !activeGroupId.value) return;
      
      const minutes = muteForm.value.minutes;
      if (isNaN(minutes) || minutes <= 0) {
        ElMessage.warning('请输入有效的禁言时长');
        return;
      }
      
      setMute(memberToMute.value, minutes);
      showMuteDialog.value = false;
    };

    // 设置禁言
    const setMute = async (member: GroupMember, minutes: number) => {
      if (!activeGroupId.value) return;
      
      settingMute.value = true;
      try {
        const response = await setMemberMuteStatus(activeGroupId.value, member.userId, true, minutes);
        
        if (response.code === 200) {
          ElMessage.success(`已将 ${member.nickname || member.username} 禁言 ${minutes} 分钟`);
          loadMembers(); // 重新加载成员列表
        } else {
          ElMessage.error(response.message || '设置禁言失败');
        }
      } catch (error: any) {
        console.error('设置禁言失败:', error);
        ElMessage.error(error.message || '设置禁言失败，请稍后重试');
      } finally {
        settingMute.value = false;
        memberToMute.value = null;
      }
    };

    // 确认解除禁言
    const confirmUnmute = async (member: GroupMember) => {
      if (!activeGroupId.value) return;
      
      settingMute.value = true;
      try {
        const response = await setMemberMuteStatus(activeGroupId.value, member.userId, false);
        
        if (response.code === 200) {
          ElMessage.success(`已解除 ${member.nickname || member.username} 的禁言`);
          loadMembers(); // 重新加载成员列表
        } else {
          ElMessage.error(response.message || '解除禁言失败');
        }
      } catch (error: any) {
        console.error('解除禁言失败:', error);
        ElMessage.error(error.message || '解除禁言失败，请稍后重试');
      } finally {
        settingMute.value = false;
      }
    };

    // 跳转到会话页面
    const goToConversation = () => {
      if (!activeGroupId.value) return;
      // 使用路由跳转到会话页面，并传递群组ID
      route.push(`/conversation/group/${activeGroupId.value}`);
    };

    // 监听路由参数变化
    watch(() => route.currentRoute.value.params.id, (newId) => {
      if (newId) {
        activeGroupId.value = parseInt(newId as string, 10);
        loadMembers();
        loadGroupDetail();
      }
    }, { immediate: true });

    onMounted(() => {
      // 如果路由中有群组ID，则加载该群组
      if (route.currentRoute.value.params.id) {
        activeGroupId.value = parseInt(route.currentRoute.value.params.id as string, 10);
        loadMembers();
        loadGroupDetail();
      }
    });

    // 处理群组编辑成功更新
    const handleUpdateSuccess = (updatedGroup: any) => {
      showEditForm.value = false;
      
      // 更新当前群组信息
      if (updatedGroup) {
        currentGroup.value = updatedGroup;
      } else {
        // 如果没有返回更新后的群组信息，则重新加载
        loadGroupDetail();
      }
      
      ElMessage.success('群组信息已更新');
    };

    // 检查当前用户是否为群主
    const isCurrentUserOwner = computed(() => {
      if (!currentUserId.value) return false;
      const currentMember = members.value.find(m => m.userId === currentUserId.value);
      if (!currentMember) return false;
      return currentMember.role === 'OWNER' || currentMember.role === 'owner';
    });

    // 取消移除成员
    const cancelRemoveMember = () => {
      showRemoveMemberDialog.value = false;
      memberToRemove.value = null;
    };

    // 确认移除成员
    const confirmRemoveMember = async () => {
      if (!memberToRemove.value || !activeGroupId.value) return;

      removingMember.value = true;
      try {
        const response = await removeGroupMember(activeGroupId.value, memberToRemove.value.userId);
        if (response.code === 200) {
          ElMessage.success(`已将 ${memberToRemove.value.nickname || memberToRemove.value.username} 移出群组`);
          loadMembers(); // 重新加载成员列表
        } else {
          let errorMsg = response.message || '移除成员失败';
          if (errorMsg.includes('没有权限移除成员')) {
            errorMsg = '您没有权限移除该成员';
          } else if (errorMsg.includes('不能移除群主')) {
            errorMsg = '不能移除群主';
          } else if (errorMsg.includes('管理员不能移除其他管理员')) {
            errorMsg = '管理员不能移除其他管理员';
          }
          ElMessage.error(errorMsg);
        }
      } catch (error: any) {
        let errorMessage = '移除成员失败，请稍后重试';
        if (error && error.message) {
          if (error.message.includes('没有权限移除成员')) {
            errorMessage = '您没有权限移除该成员';
          } else if (error.message.includes('不能移除群主')) {
            errorMessage = '不能移除群主';
          } else if (error.message.includes('管理员不能移除其他管理员')) {
            errorMessage = '管理员不能移除其他管理员';
          } else {
            errorMessage = error.message;
          }
        }
        ElMessage.error(errorMessage);
      } finally {
        showRemoveMemberDialog.value = false;
        memberToRemove.value = null;
        removingMember.value = false;
      }
    };

    // 处理公告操作
    const handleAnnouncementAction = async (command: string, announcement: any) => {
      if (!activeGroupId.value) return;

      let actionText = '';
      if (command === 'edit') {
        editingAnnouncement.value = true;
        announcementToDelete.value = announcement; // Store the announcement for editing
        announcementForm.value = {
          title: announcement.title,
          content: announcement.content,
          isPinned: announcement.isPinned
        };
        showAnnouncementForm.value = true;
      } else if (command === 'pin') {
        actionText = '置顶';
        try {
          await ElMessageBox.confirm(`确定要${actionText}该公告吗？`, '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          });

          loadingAnnouncements.value = true;
          const response = await setGroupAnnouncementPinStatus(activeGroupId.value as number, announcement.id, true);

          if (response.code === 200) {
            ElMessage.success(`已${actionText}公告`);
            loadAnnouncements(); // 重新加载公告列表
          } else {
            ElMessage.error(response.message || `${actionText}公告失败`);
          }
        } catch (error) {
          if (error !== 'cancel') {
            console.error(`${actionText}公告失败:`, error);
            ElMessage.error(`${actionText}公告失败，请稍后重试`);
          }
        } finally {
          loadingAnnouncements.value = false;
        }
      } else if (command === 'unpin') {
        actionText = '取消置顶';
        try {
          await ElMessageBox.confirm(`确定要${actionText}该公告吗？`, '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          });

          loadingAnnouncements.value = true;
          const response = await setGroupAnnouncementPinStatus(activeGroupId.value as number, announcement.id, false);

          if (response.code === 200) {
            ElMessage.success(`已${actionText}公告`);
            loadAnnouncements(); // 重新加载公告列表
          } else {
            ElMessage.error(response.message || `${actionText}公告失败`);
          }
        } catch (error) {
          if (error !== 'cancel') {
            console.error(`${actionText}公告失败:`, error);
            ElMessage.error(`${actionText}公告失败，请稍后重试`);
          }
        } finally {
          loadingAnnouncements.value = false;
        }
      } else if (command === 'delete') {
        announcementToDelete.value = announcement;
        showDeleteAnnouncementDialog.value = true;
      }
    };

    // 提交公告表单
    const announcementFormRef = ref<any>(null);
    const submitAnnouncementForm = async () => {
      if (!announcementFormRef.value || !activeGroupId.value) return;

      await announcementFormRef.value.validate(async (valid: boolean) => {
        if (valid) {
          submittingAnnouncement.value = true;
          try {
            let response;
            if (editingAnnouncement.value && announcementToDelete.value) {
              response = await updateGroupAnnouncement(
                activeGroupId.value, 
                announcementToDelete.value.id, 
                announcementForm.value
              );
            } else {
              response = await createGroupAnnouncement(
                activeGroupId.value, 
                announcementForm.value
              );
            }

            if (response.code === 200) {
              ElMessage.success(`公告${editingAnnouncement.value ? '已更新' : '已发布'}`);
              showAnnouncementForm.value = false;
              editingAnnouncement.value = false;
              announcementForm.value = { title: '', content: '', isPinned: false };
              loadAnnouncements(); // 重新加载公告列表
            } else {
              ElMessage.error(response.message || `${editingAnnouncement.value ? '更新' : '发布'}公告失败`);
            }
          } catch (error: any) {
            console.error(`${editingAnnouncement.value ? '更新' : '发布'}公告失败:`, error);
            ElMessage.error(`${editingAnnouncement.value ? '更新' : '发布'}公告失败，请稍后重试`);
          } finally {
            submittingAnnouncement.value = false;
          }
        }
      });
    };

    // 取消公告表单
    const cancelAnnouncementForm = () => {
      showAnnouncementForm.value = false;
      editingAnnouncement.value = false;
      announcementForm.value = { title: '', content: '', isPinned: false };
      announcementToDelete.value = null;
    };

    // 加载公告列表
    const loadAnnouncements = async () => {
      if (!activeGroupId.value) return;

      loadingAnnouncements.value = true;
      try {
        const params: Record<string, any> = {
          page: announcementCurrentPage.value - 1,
          size: announcementPageSize.value
        };
        
        // 获取公告列表
        const response = await getGroupAnnouncements(activeGroupId.value as number, params);

        if (response.code === 200) {
          announcements.value = response.data.content || [];
          announcementTotal.value = response.data.totalElements || 0;
        } else {
          ElMessage.error(response.message || '获取公告失败');
        }
        
        // 获取置顶公告
        const pinnedResponse = await getPinnedAnnouncements(activeGroupId.value as number);
        if (pinnedResponse.code === 200 && pinnedResponse.data && pinnedResponse.data.length > 0) {
          pinnedAnnouncement.value = pinnedResponse.data[0];
        } else {
          pinnedAnnouncement.value = null;
        }
      } catch (error) {
        console.error('获取公告失败:', error);
        ElMessage.error('获取公告失败，请稍后重试');
      } finally {
        loadingAnnouncements.value = false;
      }
    };

    // 处理公告页码变化
    const handleAnnouncementPageChange = (page: number) => {
      announcementCurrentPage.value = page;
      loadAnnouncements();
    };

    // 处理公告每页条数变化
    const handleAnnouncementSizeChange = (size: number) => {
      announcementPageSize.value = size;
      announcementCurrentPage.value = 1;
      loadAnnouncements();
    };

    // 处理公告搜索
    const handleAnnouncementSearch = () => {
      announcementCurrentPage.value = 1;
      loadAnnouncements();
    };

    // 确认删除公告
    const confirmDeleteAnnouncement = async () => {
      if (!announcementToDelete.value || !activeGroupId.value) return;

      deletingAnnouncement.value = true;
      try {
        const response = await deleteGroupAnnouncement(activeGroupId.value, announcementToDelete.value.id);
        if (response.code === 200) {
          ElMessage.success(`已删除公告: ${announcementToDelete.value.title}`);
          showDeleteAnnouncementDialog.value = false;
          announcementToDelete.value = null;
          loadAnnouncements(); // 重新加载公告列表
        } else {
          ElMessage.error(response.message || '删除公告失败');
        }
      } catch (error: any) {
        let errorMessage = '删除公告失败，请稍后重试';
        if (error && error.message) {
          errorMessage = error.message;
        }
        ElMessage.error(errorMessage);
      } finally {
        deletingAnnouncement.value = false;
      }
    };

    // 取消删除公告
    const cancelDeleteAnnouncement = () => {
      showDeleteAnnouncementDialog.value = false;
      announcementToDelete.value = null;
    };

    // 格式化日期
    const formatDate = (timestamp: string | undefined) => {
      if (!timestamp) return '未知时间';
      
      const date = new Date(timestamp);
      const now = new Date();
      const diffMinutes = (now.getTime() - date.getTime()) / (1000 * 60);

      if (diffMinutes < 60) {
        return `${Math.floor(diffMinutes)}分钟前`;
      } else if (diffMinutes < 1440) { // 24小时
        return `${Math.floor(diffMinutes / 60)}小时前`;
      } else {
        return `${date.toLocaleDateString()} ${date.toLocaleTimeString()}`;
      }
    };

    return {
      activeGroupId,
      members,
      loading,
      currentPage,
      pageSize,
      total,
      searchKeyword,
      currentUserId,
      isCurrentUserOwnerOrAdmin,
      activeTab,
      currentGroup,
      handlePageChange,
      handleSizeChange,
      handleSearch,
      handleGroupSelected,
      handleDissolveGroup,
      handleRemoveMember,
      handleToggleAdmin,
      adminMembers,
      canManageMember,
      canSetAdmin,
      canCancelAdmin,
      goToConversation,
      showEditForm,
      handleUpdateSuccess,
      isCurrentUserOwner,
      loadGroupDetail,
      handleAdminCommand,
      showRemoveMemberDialog,
      memberToRemove,
      cancelRemoveMember,
      confirmRemoveMember,
      removingMember,
      showMuteDialog,
      memberToMute,
      settingMute,
      muteForm,
      canManageMute,
      handleMuteCommand,
      cancelMute,
      confirmMute,
      confirmUnmute,
      memberAvatarUrl,
      showAnnouncementForm,
      editingAnnouncement,
      announcementForm,
      announcementFormRef,
      announcementToDelete,
      showDeleteAnnouncementDialog,
      submittingAnnouncement,
      deletingAnnouncement,
      announcements,
      loadingAnnouncements,
      announcementCurrentPage,
      announcementPageSize,
      announcementTotal,
      handleAnnouncementPageChange,
      handleAnnouncementSizeChange,
      handleAnnouncementSearch,
      formatDate,
      pinnedAnnouncement,
      handleAnnouncementAction,
      cancelAnnouncementForm,
      submitAnnouncementForm,
      confirmDeleteAnnouncement,
      cancelDeleteAnnouncement,
      loadAnnouncements
    };
  }
};
</script>

<style scoped>
.group-view {
  height: 100%;
}

.el-container {
  height: 100%;
}

.el-aside {
  border-right: 1px solid #ebeef5;
  overflow-y: auto;
}

.el-main {
  padding: 20px;
  overflow-y: auto;
}

.group-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.group-header {
  padding: 20px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.group-info {
  display: flex;
  align-items: center;
}

.group-details {
  margin-left: 20px;
}

.group-meta {
  color: #909399;
  font-size: 14px;
  margin-top: 5px;
}

.group-actions {
  display: flex;
  gap: 10px;
}

.group-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.group-members {
  padding: 20px 0;
}

.member-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.member-name {
  font-weight: 500;
  margin-bottom: 4px;
}

.member-role {
  font-size: 12px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.group-chat {
  padding: 20px;
  text-align: center;
}

.member-role .el-tag--danger {
  color: #f56c6c;
}

.member-actions {
  display: flex;
  gap: 8px;
}

.el-dropdown-menu {
  min-width: 120px;
}

/* 移除成员确认对话框样式 */
.remove-member-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.member-info-preview {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  border-radius: 8px;
  background-color: #f5f7fa;
}

.member-info-text {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.member-name-large {
  font-size: 16px;
  font-weight: 600;
}

.member-role-tag {
  margin-top: 2px;
}

.remove-warning {
  margin-top: 10px;
}

.remove-warning p {
  margin: 5px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.mute-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.mute-unit {
  margin-left: 10px;
}

.mute-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 公告样式 */
.group-announcements {
  padding: 20px;
}

.announcements-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.announcements-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.pinned-announcement {
  margin-bottom: 20px;
}

.announcement-card {
  background-color: #f5f7fa;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 15px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.announcement-card.pinned {
  border: 1px solid #e1f3d8;
  background-color: #f0f9eb;
  box-shadow: 0 4px 12px 0 rgba(0, 0, 0, 0.1);
}

.announcement-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
  flex-wrap: wrap;
  gap: 10px;
}

.announcement-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.announcement-title h4 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  font-weight: 500;
}

.announcement-title .el-tag {
  font-size: 12px;
}

.announcement-actions {
  display: flex;
  gap: 8px;
}

.announcement-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  word-break: break-word;
  margin-bottom: 10px;
  padding: 10px 0;
  border-top: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
}

.announcement-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
  padding-top: 10px;
}

.announcement-author {
  font-weight: 500;
  color: #409eff;
}

.delete-announcement-content {
  padding: 20px;
}

.delete-announcement-content p {
  margin: 5px 0;
}
</style> 