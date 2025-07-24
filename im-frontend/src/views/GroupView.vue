<!-- 群组视图 -->
<template>
  <div class="group-view">
    <el-container>
      <el-aside width="300px">
        <div class="group-search-button">
          <el-button type="primary" @click="openSearch">
            <el-icon><Search /></el-icon>
            搜索新群组
          </el-button>
        </div>
        <GroupList @select-group="handleGroupSelected" />
      </el-aside>
      <el-main>
        <!-- 搜索界面 -->
        <div v-if="activeTab === 'search'" class="search-container">
          <div class="search-header">
            <h2>搜索群组</h2>
            <p>查找并加入你感兴趣的群组</p>
          </div>
          <div class="search-content">
          <GroupSearch />
          </div>
        </div>

        <!-- 群组内容 -->
        <div v-if="activeGroupId && activeTab !== 'search'" class="group-content">
          <!-- 群组头部 -->
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
            <div class="group-actions">
              <el-button type="primary" @click="showEditForm = true" v-if="isCurrentUserOwnerOrAdmin">
                修改群资料
              </el-button>
              <el-button type="danger" @click="handleLeaveGroup" v-if="!isCurrentUserOwner">
                退出群聊
              </el-button>
              <el-button type="danger" @click="handleDissolveGroup" v-if="isCurrentUserOwner">
                解散群聊
              </el-button>
              <el-button type="warning" @click="handleReportGroup" v-if="!isCurrentUserOwner">
                举报群组
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
          
          <!-- 举报群组对话框 -->
          <el-dialog
            v-model="showReportGroupDialog"
            title="举报群组"
            width="500px"
            destroy-on-close
          >
            <div class="report-form">
              <div class="report-group-info" v-if="currentGroup">
                <div class="report-group-avatar">
                  <el-avatar :size="50">G</el-avatar>
                </div>
                <div class="report-group-details">
                  <h3>{{ currentGroup.name }}</h3>
                  <p>群ID: {{ activeGroupId }}</p>
                </div>
              </div>
              
              <el-form :model="reportGroupForm" label-width="80px">
                <el-form-item label="举报原因" required>
                  <el-select v-model="reportGroupForm.reason" placeholder="请选择举报原因" style="width: 100%">
                    <el-option label="垃圾信息" value="垃圾信息"></el-option>
                    <el-option label="色情内容" value="色情内容"></el-option>
                    <el-option label="暴力内容" value="暴力内容"></el-option>
                    <el-option label="诈骗信息" value="诈骗信息"></el-option>
                    <el-option label="政治敏感" value="政治敏感"></el-option>
                    <el-option label="侮辱谩骂" value="侮辱谩骂"></el-option>
                    <el-option label="其他" value="其他"></el-option>
                  </el-select>
                </el-form-item>
                <el-form-item label="详细描述">
                  <el-input
                    v-model="reportGroupForm.description"
                    type="textarea"
                    :rows="4"
                    placeholder="请描述具体情况，有助于我们更好地处理..."
                  ></el-input>
                </el-form-item>
              </el-form>
            </div>
            <template #footer>
              <div class="dialog-footer">
                <el-button @click="showReportGroupDialog = false">取消</el-button>
                <el-button type="primary" @click="submitReportGroup" :disabled="!reportGroupForm.reason" :loading="submittingReport">
                  提交
                </el-button>
              </div>
            </template>
          </el-dialog>

          <el-tabs v-model="activeTab" class="group-tabs">
            <!-- 群聊标签页已删除 -->

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
                      <el-table-column label="操作" width="280">
                    <template #default="scope">
                      <div class="member-actions">
                        <!-- 群主移除成员按钮 -->
                        <el-button
                          v-if="canOwnerManageMember(scope.row)"
                          type="danger"
                          size="small"
                          @click="handleRemoveMember(scope.row)"
                          class="action-button owner-action"
                        >
                          <el-icon><Delete /></el-icon>
                          移除
                        </el-button>
                        
                        <!-- 管理员移除成员按钮 -->
                        <el-button
                          v-if="canAdminManageMember(scope.row)"
                          type="danger"
                          size="small"
                          @click="handleRemoveMember(scope.row)"
                          class="action-button admin-action"
                        >
                          <el-icon><Delete /></el-icon>
                          管理员移除
                        </el-button>
                        
                        <!-- 群主禁言操作按钮 -->
                        <el-dropdown 
                          v-if="canOwnerManageMute(scope.row)"
                          trigger="click"
                          @command="(command: string) => handleMuteCommand(command, scope.row)"
                          class="action-dropdown"
                        >
                          <el-button
                            :type="scope.row.isMuted ? 'warning' : 'info'" 
                            size="small"
                            class="action-button owner-action"
                          >
                            <el-icon><Mute /></el-icon>
                            {{ scope.row.isMuted ? '已禁言' : '禁言' }}
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
                        
                        <!-- 管理员禁言操作按钮 -->
                        <el-dropdown 
                          v-if="canAdminManageMute(scope.row)"
                          trigger="click"
                          @command="(command: string) => handleMuteCommand(command, scope.row)"
                          class="action-dropdown"
                        >
                          <el-button
                            :type="scope.row.isMuted ? 'warning' : 'info'" 
                            size="small"
                            class="action-button admin-action"
                          >
                            <el-icon><Mute /></el-icon>
                            {{ scope.row.isMuted ? '已禁言' : '管理员禁言' }}
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
                        
                        <!-- 管理员角色操作 -->
                        <el-dropdown 
                          v-if="canSetAdmin(scope.row) || canCancelAdmin(scope.row)"
                          trigger="click"
                          @command="(command: string) => handleAdminCommand(command, scope.row)"
                          class="action-dropdown"
                        >
                          <el-button type="primary" size="small" class="action-button">
                            <el-icon><Setting /></el-icon>
                            管理员
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
                        
                        <!-- 举报成员按钮 -->
                        <el-button
                          v-if="scope.row.userId !== currentUserId"
                          type="warning"
                          size="small"
                          @click="handleReportMember(scope.row)"
                          class="action-button"
                        >
                          <el-icon><Warning /></el-icon>
                          举报
                        </el-button>
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

            <el-tab-pane 
              :label="`加入请求${pendingRequestCount > 0 ? '(' + pendingRequestCount + ')' : ''}`" 
              name="requests" 
              v-if="isCurrentUserOwnerOrAdmin && activeGroupId"
            >
              <div class="group-requests">
                <GroupJoinRequests 
                  :groupId="Number(activeGroupId)" 
                  :isAdmin="isCurrentUserOwnerOrAdmin"
                  @update:count="updateRequestCount" 
                />
              </div>
            </el-tab-pane>
            
            <!-- 添加群投票标签页 -->
            <el-tab-pane
              label="群投票"
              name="polls"
              v-if="activeGroupId"
            >
              <div class="group-polls-container">
                <GroupPolls 
                  :groupId="activeGroupId ? Number(activeGroupId) : 0"
                  :isGroupOwner="isCurrentUserOwner"
                  :isGroupAdmin="isCurrentUserAdmin"
                />
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- 未选择群组且非搜索标签页时的提示 -->
        <div v-if="!activeGroupId && activeTab !== 'search'" class="empty-group-container">
          <el-empty description="请选择一个群组">
            <el-button type="primary" @click="openSearch">搜索新群组</el-button>
          </el-empty>
        </div>
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
    
    <!-- 举报成员对话框 -->
    <el-dialog
      v-model="showReportMemberDialog"
      title="举报群成员"
      width="500px"
      destroy-on-close
    >
      <div class="report-form">
        <div class="report-member-info" v-if="memberToReport">
          <div class="report-member-avatar">
            <el-avatar :size="50" :src="memberToReport.avatarUrl">
              {{ memberToReport.nickname?.substring(0, 1) || memberToReport.username?.substring(0, 1) }}
            </el-avatar>
          </div>
          <div class="report-member-details">
            <h3>{{ memberToReport.nickname || memberToReport.username }}</h3>
            <p>用户ID: {{ memberToReport.userId }}</p>
          </div>
        </div>
        
        <el-form :model="reportMemberForm" label-width="80px">
          <el-form-item label="举报原因" required>
            <el-select v-model="reportMemberForm.reason" placeholder="请选择举报原因" style="width: 100%">
              <el-option label="垃圾信息" value="垃圾信息"></el-option>
              <el-option label="色情内容" value="色情内容"></el-option>
              <el-option label="暴力内容" value="暴力内容"></el-option>
              <el-option label="诈骗信息" value="诈骗信息"></el-option>
              <el-option label="政治敏感" value="政治敏感"></el-option>
              <el-option label="侮辱谩骂" value="侮辱谩骂"></el-option>
              <el-option label="其他" value="其他"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="详细描述">
            <el-input
              v-model="reportMemberForm.description"
              type="textarea"
              :rows="4"
              placeholder="请描述具体情况，有助于我们更好地处理..."
            ></el-input>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showReportMemberDialog = false">取消</el-button>
          <el-button type="primary" @click="submitReportMember" :disabled="!reportMemberForm.reason" :loading="submittingReport">
            提交
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
import { ArrowDown, More, Edit, Top, Delete, Mute, Setting, Warning, Search } from '@element-plus/icons-vue';
import GroupList from '@/components/group/GroupList.vue';
import GroupEditForm from '@/components/group/GroupEditForm.vue';
import GroupSearch from '@/components/group/GroupSearch.vue'; // 导入群组搜索组件
import GroupJoinRequests from '@/components/group/GroupJoinRequests.vue'; // 导入群组加入请求组件
import GroupPolls from '@/components/group/GroupPolls.vue'; // 导入群组投票组件
import { reportApi } from '@/api/report'; // 导入举报API
import { getCurrentUserId } from '@/utils/helpers'; // 导入获取当前用户ID的辅助函数
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
  pinAnnouncement as setGroupAnnouncementPinStatus,
  leaveGroup
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
    GroupEditForm,
    GroupSearch,
    GroupJoinRequests, // 注册GroupJoinRequests组件
    GroupPolls // 注册GroupPolls组件
  },
  setup() {
    const router = useRouter();
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
    const currentUserId = ref<number>(getCurrentUserId());
    
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

    // 加入请求相关变量
    const pendingRequestCount = ref(0);
    const updateRequestCount = (count: number) => {
      pendingRequestCount.value = count;
    };

    // 举报群组相关变量
    const showReportGroupDialog = ref(false);
    const reportGroupForm = ref({
      reason: '',
      description: ''
    });
    const submittingReport = ref(false);
    
    // 举报成员相关变量
    const showReportMemberDialog = ref(false);
    const memberToReport = ref<GroupMember | null>(null);
    const reportMemberForm = ref({
      reason: '',
      description: ''
    });

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
        } else if (event.updateType === 'DELETE' || event.updateType === 'GROUP_DISSOLVED') {
          ElMessage.warning(event.updateType === 'GROUP_DISSOLVED' ? '该群组已被群主解散' : '该群组已被删除');
          router.push('/groups');
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
    
    // 监听activeGroupId变化，如果没有选择群组，默认切换到搜索标签页
    watch(activeGroupId, (newId) => {
      if (!newId && activeTab.value !== 'search') {
        activeTab.value = 'search';
      }
    });
    
    // 处理路由参数变化
    watch(() => router.currentRoute.value.params.id, (newId) => {
      if (newId && typeof newId === 'string') {
        activeGroupId.value = parseInt(newId, 10);
        loadMembers();
        loadGroupDetail();
      }
    }, { immediate: true });

    // 监听标签页变化
    watch(activeTab, (newTab) => {
      if (newTab === 'announcements' && activeGroupId.value) {
        loadAnnouncements();
      }
      if (newTab === 'requests' && activeGroupId.value && (isCurrentUserOwner.value || isCurrentUserAdmin.value)) {
        // 只有群主或管理员才更新请求数量
        updateRequestCount(0);
      }
    });
    
    // 组件挂载时初始化
    onMounted(() => {
      // 组件挂载时，获取当前用户ID
      const userId = getCurrentUserId();
      console.log('组件挂载时获取的用户ID:', userId);
      
      // 如果获取到了有效的用户ID，则更新currentUserId
      if (userId > 0) {
        currentUserId.value = userId;
      }
      
      // 解析路由参数中的群组ID
      const route = useRoute();
      if (route.params.id) {
        const groupId = parseInt(route.params.id as string, 10);
        if (!isNaN(groupId)) {
          activeGroupId.value = groupId;
        loadGroupDetail();
          loadMembers();
      
      // 添加WebSocket监听
      addListener(handleGroupUpdate);
        }
      }
    });
    
    // 在组件卸载时移除监听器
    onUnmounted(() => {
      removeListener(handleGroupUpdate);
    });

    // 处理群组选择
    const handleGroupSelected = (groupId: number) => {
      activeGroupId.value = groupId;
      // 如果在搜索标签页，切换到成员标签页
      if (activeTab.value === 'search') {
        activeTab.value = 'members';
      }
      loadMembers();
      loadGroupDetail();
      
      // 如果当前是公告标签页，则加载公告
      if (activeTab.value === 'announcements') {
        loadAnnouncements();
      }
      // 如果当前是请求标签页且用户是群主或管理员，则更新请求数量
      if (activeTab.value === 'requests' && (isCurrentUserOwner.value || isCurrentUserAdmin.value)) {
        updateRequestCount(0);
      }
      // 如果当前是投票标签页，无需额外操作，GroupPolls组件会通过watch监听groupId的变化自动刷新
      console.log('群组切换，当前标签页:', activeTab.value);
    };

    // 加载群成员
    const loadMembers = () => {
      if (activeGroupId.value === null) return;
      
      loading.value = true;
      try {
        const params: Record<string, any> = {
          page: currentPage.value - 1,
          size: pageSize.value
        };
        
        if (searchKeyword.value) {
          params.keyword = searchKeyword.value;
        }
        
        const groupId = activeGroupId.value;
        
        getGroupMembers(groupId, params).then(response => {
        if (response.code === 200) {
          members.value = response.data.content || [];
          total.value = response.data.totalElements || 0;
          
          // 打印当前用户的角色信息，用于调试
          const currentMember = members.value.find(m => m.userId === currentUserId.value);
          console.log('当前用户:', currentUserId.value);
          console.log('当前用户成员信息:', currentMember);
          
          // 如果找不到当前用户，或当前用户ID为0，尝试重新获取用户ID
          if (!currentMember || currentUserId.value === 0) {
            console.log('找不到当前用户或用户ID为0，尝试重新获取用户ID');
            const newUserId = getCurrentUserId();
            console.log('重新获取的用户ID:', newUserId);
            
            if (newUserId > 0 && newUserId !== currentUserId.value) {
              console.log('用户ID已更新，从', currentUserId.value, '到', newUserId);
              currentUserId.value = newUserId;
              
              // 再次查找当前用户
              const updatedCurrentMember = members.value.find(m => m.userId === currentUserId.value);
              console.log('更新用户ID后的当前用户成员信息:', updatedCurrentMember);
            }
          }
          
          if (currentMember) {
            console.log('当前用户角色 (原始):', currentMember.role);
            // 统一角色格式为小写
            currentMember.role = currentMember.role?.toLowerCase();
            console.log('当前用户角色 (标准化):', currentMember.role);
          }
          
          // 对所有成员的role进行标准化处理
          members.value.forEach(member => {
            // 统一角色格式为小写
            if (member.role) {
              member.role = member.role.toLowerCase();
            }
          });
        } else {
          ElMessage.error(response.message || '获取群成员失败');
        }
          loading.value = false;
        }).catch(error => {
          console.error('获取群成员失败:', error);
          ElMessage.error('获取群成员失败，请稍后重试');
          loading.value = false;
        });
      } catch (error) {
        console.error('获取群成员失败:', error);
        ElMessage.error('获取群成员失败，请稍后重试');
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

    // 转到会话页面功能已删除

    // 加载群组详情
    const loadGroupDetail = () => {
      if (activeGroupId.value === null) return;
      
      const groupId = activeGroupId.value;
      
      getGroupById(groupId).then(response => {
        if (response.code === 200) {
          currentGroup.value = response.data;
        } else {
          ElMessage.error(response.message || '获取群组详情失败');
        }
      }).catch(error => {
        console.error('获取群组详情失败:', error);
        ElMessage.error('获取群组详情失败，请稍后重试');
      });
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
        
        loading.value = true;
        const response = await dissolveGroup(activeGroupId.value);
        if (response.code === 200) {
          ElMessage.success('群组已解散');
          // 重置状态
          activeGroupId.value = null;
          members.value = [];
          // 切换回群组界面
          activeTab.value = 'groups';
        } else {
          ElMessage.error(response.message || '解散群组失败');
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('解散群组失败:', error);
          ElMessage.error('解散群组失败，请稍后重试');
        }
      } finally {
        loading.value = false;
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
      const isCurrentUserOwner = currentUserRole.value === 'owner';
      
      return isCurrentUserOwner && isMemberRegular;
    };

    // 检查是否可以取消管理员（管理员且当前用户是群主）
    const canCancelAdmin = (member: GroupMember) => {
      // 当前成员不能是自己
      if (member.userId === currentUserId.value) return false;
      
      // 当前成员必须是管理员
      const isMemberAdmin = member.role === 'ADMIN' || member.role === 'admin';
      
      // 当前用户必须是群主
      const isCurrentUserOwner = currentUserRole.value === 'owner';
      
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

    // 检查当前用户角色
    const currentUserRole = computed(() => {
      if (!currentUserId.value) return null;
      
      const currentMember = members.value.find(member => member.userId === currentUserId.value);
      if (!currentMember) return null;
      
      const role = currentMember.role?.toLowerCase();
      if (role === 'owner') return 'owner';
      if (role === 'admin') return 'admin';
      return 'member';
    });

    // 检查当前用户是否为管理员
    const isCurrentUserAdmin = computed(() => {
      if (!currentUserId.value) return false;
      
      const currentMember = members.value.find(member => member.userId === currentUserId.value);
      if (!currentMember) return false;
      
      const role = currentMember.role?.toLowerCase();
      return role === 'admin';
    });

    // 检查当前用户是否为群主
    const isCurrentUserOwner = computed(() => {
      if (!currentUserId.value) return false;
      
      const currentMember = members.value.find(member => member.userId === currentUserId.value);
      if (!currentMember) return false;
      
      const role = currentMember.role?.toLowerCase();
      return role === 'owner';
    });

    // 检查是否可管理成员
    const canManageMember = (member: GroupMember) => {
      // 不能移除自己
      if (member.userId === currentUserId.value) {
        return false;
      }
      
      // 检查目标成员是否为群主
      const isTargetOwner = member.role === 'OWNER' || member.role === 'owner';
      
      // 检查目标成员是否为管理员
      const isTargetAdmin = member.role === 'ADMIN' || member.role === 'admin';
      
      // 使用计算属性获取当前用户角色
      const role = currentUserRole.value;
      
      // 群主可以移除任何人（除了自己）
      if (role === 'owner') {
        return !isTargetOwner; // 群主不能被移除
      }
      
      // 管理员只能移除普通成员
      if (role === 'admin') {
        return !isTargetOwner && !isTargetAdmin; // 管理员不能移除群主和其他管理员
      }
      
      // 普通成员不能移除任何人
      return false;
    };

    // 群主是否可以移除成员
    const canOwnerManageMember = (member: GroupMember) => {
      // 不能移除自己
      if (member.userId === currentUserId.value) {
        return false;
      }
      
      // 检查目标成员是否为群主
      const isTargetOwner = member.role === 'OWNER' || member.role === 'owner';
      
      // 使用计算属性获取当前用户角色
      const role = currentUserRole.value;
      
      // 群主可以移除任何人（除了自己）
      return role === 'owner' && !isTargetOwner;
    };

    // 管理员是否可以移除成员
    const canAdminManageMember = (member: GroupMember) => {
      // 不能移除自己
      if (member.userId === currentUserId.value) {
        return false;
      }
      
      // 检查目标成员是否为群主
      const isTargetOwner = member.role?.toLowerCase() === 'owner';
      
      // 检查目标成员是否为管理员
      const isTargetAdmin = member.role?.toLowerCase() === 'admin';
      
      // 显式检查当前用户是否是管理员，不使用currentUserRole计算属性
      const currentMember = members.value.find(m => m.userId === currentUserId.value);
      const isAdmin = currentMember && currentMember.role?.toLowerCase() === 'admin';
      
      console.log('管理员移除权限检查:', {
        isAdmin,
        currentUserRole: currentUserRole.value,
        targetRole: member.role,
        isTargetOwner,
        isTargetAdmin,
        result: isAdmin && !isTargetOwner && !isTargetAdmin
      });
      
      // 管理员只能移除普通成员
      return isAdmin && !isTargetOwner && !isTargetAdmin;
    };

    // 检查是否可以管理禁言
    const canManageMute = (member: GroupMember) => {
      // 不能禁言自己
      if (member.userId === currentUserId.value) {
        return false;
      }
      
      // 检查目标成员是否为群主
      const isTargetOwner = member.role === 'OWNER' || member.role === 'owner';
      
      // 检查目标成员是否为管理员
      const isTargetAdmin = member.role === 'ADMIN' || member.role === 'admin';
      
      // 使用计算属性获取当前用户角色
      const role = currentUserRole.value;
      
      // 群主可以禁言任何人（除了自己）
      if (role === 'owner') {
        return !isTargetOwner; // 群主不能被禁言
      }
      
      // 管理员只能禁言普通成员
      if (role === 'admin') {
        return !isTargetOwner && !isTargetAdmin; // 管理员不能禁言群主和其他管理员
      }
      
      // 普通成员不能禁言任何人
      return false;
    };

    // 群主是否可以禁言成员
    const canOwnerManageMute = (member: GroupMember) => {
      // 不能禁言自己
      if (member.userId === currentUserId.value) {
        return false;
      }
      
      // 检查目标成员是否为群主
      const isTargetOwner = member.role === 'OWNER' || member.role === 'owner';
      
      // 使用计算属性获取当前用户角色
      const role = currentUserRole.value;
      
      // 群主可以禁言任何人（除了自己和其他群主）
      return role === 'owner' && !isTargetOwner;
    };

    // 管理员是否可以禁言成员
    const canAdminManageMute = (member: GroupMember) => {
      // 不能禁言自己
      if (member.userId === currentUserId.value) {
        return false;
      }
      
      // 检查目标成员是否为群主
      const isTargetOwner = member.role?.toLowerCase() === 'owner';
      
      // 检查目标成员是否为管理员
      const isTargetAdmin = member.role?.toLowerCase() === 'admin';
      
      // 显式检查当前用户是否是管理员，不使用currentUserRole计算属性
      const currentMember = members.value.find(m => m.userId === currentUserId.value);
      const isAdmin = currentMember && currentMember.role?.toLowerCase() === 'admin';
      
      console.log('管理员禁言权限检查:', {
        isAdmin,
        currentUserRole: currentUserRole.value,
        targetRole: member.role,
        isTargetOwner,
        isTargetAdmin,
        result: isAdmin && !isTargetOwner && !isTargetAdmin
      });
      
      // 管理员只能禁言普通成员
      return isAdmin && !isTargetOwner && !isTargetAdmin;
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

    // 退出群组
    const handleLeaveGroup = async () => {
      if (!activeGroupId.value) return;
      if (!currentUserId.value) {
        ElMessage.error('获取用户信息失败，请重新登录');
        return;
      }

      try {
        await ElMessageBox.confirm('确定要退出该群组吗？退出后将无法接收和发送群消息。', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        });

        loading.value = true;
        
        // 确保activeGroupId.value和currentUserId.value是数字
        const groupId = Number(activeGroupId.value);
        const userId = Number(currentUserId.value);
        
        console.log(`尝试退出群组: 群组ID=${groupId}, 用户ID=${userId}`);
        
        // 直接使用removeGroupMember API
        const response = await removeGroupMember(groupId, userId);

        if (response.code === 200) {
          ElMessage.success('已退出群组');
          router.push('/groups'); // 退出后跳转到群组列表
        } else {
          ElMessage.error(response.message || '退出群组失败');
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('退出群组失败:', error);
          ElMessage.error('退出群组失败，请稍后重试');
        }
      } finally {
        loading.value = false;
      }
    };

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

    // 处理举报群组
    const handleReportGroup = () => {
      if (!activeGroupId.value) return;
      reportGroupForm.value = { reason: '', description: '' };
      showReportGroupDialog.value = true;
    };

    // 提交举报群组
    const submitReportGroup = async () => {
      if (!activeGroupId.value) {
        ElMessage.warning('无法获取群组信息');
        return;
      }
      
      if (!reportGroupForm.value.reason) {
        ElMessage.warning('请选择举报原因');
        return;
      }

      submittingReport.value = true;
      try {
        const response = await reportApi.reportGroup(
          Number(activeGroupId.value), 
          Number(currentUserId.value),
          reportGroupForm.value.reason,
          reportGroupForm.value.description || undefined
        );
        
        if (response.success) {
          ElMessage.success('群组举报成功，感谢您的反馈！');
          showReportGroupDialog.value = false;
          reportGroupForm.value = { reason: '', description: '' };
        } else {
          ElMessage.error(response.message || '群组举报失败');
        }
      } catch (error: any) {
        let errorMessage = '群组举报失败，请稍后重试';
        if (error && error.message) {
          errorMessage = error.message;
        }
        ElMessage.error(errorMessage);
      } finally {
        submittingReport.value = false;
      }
    };
    
    // 处理举报成员
    const handleReportMember = (member: GroupMember) => {
      memberToReport.value = member;
      reportMemberForm.value = { reason: '', description: '' };
      showReportMemberDialog.value = true;
    };
    
    // 提交举报成员
    const submitReportMember = async () => {
      if (!activeGroupId.value || !memberToReport.value) {
        ElMessage.warning('无法获取成员信息');
        return;
      }
      
      if (!reportMemberForm.value.reason) {
        ElMessage.warning('请选择举报原因');
        return;
      }

      submittingReport.value = true;
      try {
        const response = await reportApi.reportGroupMember(
          memberToReport.value.userId, 
          Number(activeGroupId.value),
          Number(currentUserId.value),
          reportMemberForm.value.reason,
          reportMemberForm.value.description || undefined
        );
        
        if (response.success) {
          ElMessage.success('成员举报成功，感谢您的反馈！');
          showReportMemberDialog.value = false;
          memberToReport.value = null;
          reportMemberForm.value = { reason: '', description: '' };
        } else {
          ElMessage.error(response.message || '成员举报失败');
        }
      } catch (error: any) {
        let errorMessage = '成员举报失败，请稍后重试';
        if (error && error.message) {
          errorMessage = error.message;
        }
        ElMessage.error(errorMessage);
      } finally {
        submittingReport.value = false;
      }
    };

    // 转到搜索页面
    const openSearch = () => {
      // 切换到搜索标签页
      activeTab.value = 'search';
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
      isCurrentUserAdmin,
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
      loadAnnouncements,
      pendingRequestCount,
      updateRequestCount,
      handleLeaveGroup,
      showReportGroupDialog,
      reportGroupForm,
      submittingReport,
      handleReportGroup,
      submitReportGroup,
      showReportMemberDialog,
      memberToReport,
      reportMemberForm,
      handleReportMember,
      submitReportMember,
      // 新增的权限检查函数
      canOwnerManageMember,
      canAdminManageMember,
      canOwnerManageMute,
      canAdminManageMute,
      openSearch,
    };
  }
};
</script>

<style scoped>
.group-view {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.el-container {
  height: 100%;
}

.el-aside {
  background-color: #f9f9f9;
  border-right: 1px solid #e8e8e8;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.05);
  overflow-y: auto;
}

.el-main {
  padding: 0;
  overflow-y: auto;
  background-color: #fff;
}

.group-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background-color: #fff;
  border-bottom: 2px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.group-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.group-details {
  display: flex;
  flex-direction: column;
}

.group-details h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  position: relative;
  padding-left: 10px;
}

.group-details h2::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 18px;
  background: var(--primary-color, #4a7bff);
  border-radius: 2px;
}

.group-meta {
  color: #666;
  font-size: 14px;
  margin-top: 4px;
}

.group-actions {
  display: flex;
  gap: 10px;
}

.group-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 修改Element Plus的标签页样式 */
:deep(.el-tabs__header) {
  margin: 0;
  border-bottom: 1px solid #e8e8e8;
  background-color: #f9f9f9;
}

:deep(.el-tabs__nav) {
  border: none !important;
}

:deep(.el-tabs__item) {
  height: 50px;
  line-height: 50px;
  font-size: 15px;
  color: #666;
  border: none !important;
  position: relative;
}

:deep(.el-tabs__item.is-active) {
  color: var(--primary-color, #4a7bff);
  font-weight: 600;
}

:deep(.el-tabs__item.is-active)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 50%;
  height: 3px;
  background-color: var(--primary-color, #4a7bff);
  border-radius: 3px 3px 0 0;
}

:deep(.el-tabs__active-bar) {
  display: none;
}

.group-chat {
  padding: 24px;
  text-align: center;
  border: 1px dashed #e8e8e8;
  margin: 24px;
  border-radius: 12px;
  background-color: #f9f9f9;
  box-shadow: inset 0 2px 10px rgba(0, 0, 0, 0.03);
}

.group-chat .el-button {
  font-size: 16px;
  padding: 12px 24px;
  border-radius: 8px;
  background-color: var(--primary-color, #4a7bff);
  border: none;
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.group-chat .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.group-members {
  padding: 20px;
}

/* 表格样式 */
:deep(.el-table) {
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

:deep(.el-table__header-wrapper) {
  background-color: #f9f9f9;
}

:deep(.el-table__header) th {
  background-color: #f0f5ff !important;
  color: #444;
  font-weight: 600;
  height: 50px;
}

:deep(.el-table__body) td {
  padding: 12px 0;
}

:deep(.el-table__row:hover) td {
  background-color: #f0f5ff !important;
}

.member-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 搜索容器样式 */
.search-container {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin: 0 20px;
}

.search-header {
  margin-bottom: 20px;
  text-align: center;
  padding: 20px 0;
  background-color: #f5f7fa;
  border-bottom: 1px solid #e8e8e8;
  border-top-left-radius: 10px;
  border-top-right-radius: 10px;
}

.search-header h2 {
  font-size: 24px;
  margin-bottom: 8px;
  color: #409EFF;
}

.search-header p {
  color: #606266;
  font-size: 14px;
}

.group-search-button {
  margin: 10px;
  display: flex;
  justify-content: center;
}

.group-search-button .el-button {
  width: 100%;
}

/* 针对Element Plus的卡片样式 */
:deep(.el-card) {
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.3s;
  margin-bottom: 20px;
  border: 1px solid #e8e8e8;
}

:deep(.el-card):hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1) !important;
}

:deep(.el-card__header) {
  padding: 15px 20px;
  border-bottom: 1px solid #e8e8e8;
  background-color: #f9f9f9;
}

:deep(.el-card__body) {
  padding: 20px;
}

/* 对话框样式 */
:deep(.el-dialog) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  background-color: #f9f9f9;
  padding: 16px 20px;
  margin: 0;
  border-bottom: 1px solid #e8e8e8;
}

:deep(.el-dialog__title) {
  font-weight: 600;
  font-size: 18px;
  color: #333;
}

:deep(.el-dialog__body) {
  padding: 24px;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid #e8e8e8;
  padding: 16px 20px;
  background-color: #f9f9f9;
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

.member-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-button {
  display: flex;
  align-items: center;
  gap: 4px;
  border-radius: 6px;
  transition: all 0.2s;
  padding: 5px 10px;
  min-width: 70px;
  justify-content: center;
}

.action-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.action-dropdown {
  margin: 0;
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

.empty-group-container {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.group-search-tab {
  height: 100%;
}

.group-requests {
  height: 100%;
}

.group-polls-container {
  height: 100%;
}

/* 举报群组对话框样式 */
.report-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 20px;
}

.report-group-info {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  border-radius: 8px;
  background-color: #f5f7fa;
}

.report-group-avatar {
  flex-shrink: 0;
}

.report-group-details {
  display: flex;
  flex-direction: column;
}

.report-group-details h3 {
  margin: 0 0 5px 0;
  font-size: 16px;
  color: #303133;
}

.report-group-details p {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

/* 举报成员样式 */
.report-member-info {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  border-radius: 8px;
  background-color: #f5f7fa;
}

.report-member-avatar {
  flex-shrink: 0;
}

.report-member-details {
  display: flex;
  flex-direction: column;
}

.report-member-details h3 {
  margin: 0 0 5px 0;
  font-size: 16px;
  color: #303133;
}

.report-member-details p {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

/* 响应式布局，在小屏幕上按钮垂直堆叠 */
@media screen and (max-width: 1200px) {
  .member-actions {
    flex-direction: column;
    gap: 5px;
  }
  
  :deep(.el-table__row) {
    height: auto;
    padding: 10px 0;
  }
}

/* 确保表格行足够高以容纳多个按钮 */
:deep(.el-table__row) {
  height: 80px !important;
  padding: 12px 0;
}

.action-button.owner-action {
  background-color: #f56c6c;
  color: white;
  border-color: #f56c6c;
}

.action-button.owner-action:hover {
  background-color: #e74c3c;
  border-color: #e74c3c;
  color: white;
}

.action-button.admin-action {
  background-color: #f39c12;
  color: white;
  border-color: #f39c12;
}

.action-button.admin-action:hover {
  background-color: #e67e22;
  border-color: #e67e22;
  color: white;
}

.search-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px 20px;
}
</style>