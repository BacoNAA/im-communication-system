<!-- 群投票组件 -->
<template>
  <div class="group-polls">
    <!-- 创建投票按钮 -->
    <div class="create-poll-btn-container">
      <el-button type="primary" @click="openCreateDialog" :disabled="loading">
        <el-icon><Plus /></el-icon>发起投票
      </el-button>
    </div>
    
    <!-- 状态过滤 -->
    <div class="filters">
      <el-radio-group v-model="filterStatus" size="small" @change="handleFilterChange">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="ACTIVE">进行中</el-radio-button>
        <el-radio-button label="ENDED">已结束</el-radio-button>
        <el-radio-button label="CANCELED">已取消</el-radio-button>
      </el-radio-group>
    </div>
    
    <!-- 投票列表 -->
    <div class="polls-list">
      <el-empty v-if="!loading && (!polls || polls.length === 0)" description="暂无投票" />
      <el-skeleton v-else-if="loading" :rows="3" animated />
      <div v-else>
        <div v-for="poll in polls" :key="poll.id" class="poll-item">
          <el-card shadow="hover" :body-style="{ padding: '15px' }" @click="viewPollDetails(poll.id)">
            <div class="poll-header">
              <div class="poll-title">
                <h3>{{ poll.title }}</h3>
                <el-tag size="small" :type="getStatusTagType(poll.status)">{{ getStatusText(poll.status) }}</el-tag>
              </div>
              <div class="poll-creator">
                <span>发起人: {{ poll.creatorNickname }}</span>
                <span class="poll-time">{{ formatTime(poll.createdAt) }}</span>
              </div>
            </div>
            
            <div class="poll-options">
              <div v-for="option in poll.options" :key="option.id" class="poll-option">
                <div class="option-text">{{ option.optionText }}</div>
                <div class="option-stats">
                  <el-progress 
                    :percentage="option.percentage" 
                    :format="(p: number) => `${option.voteCount}票 (${p.toFixed(1)}%)`" 
                    :stroke-width="10" 
                  />
                </div>
              </div>
            </div>
            
            <div class="poll-footer">
              <div class="poll-stats">
                <span>{{ poll.totalVoters }}人参与</span>
                <span v-if="poll.hasVoted" class="has-voted">已投票</span>
              </div>
              
              <div class="poll-actions">
                <el-button size="small" type="primary" @click.stop="viewPollDetails(poll.id)">查看详情</el-button>
                <el-button 
                  v-if="poll.status === 'ACTIVE' && (isGroupOwner || isGroupAdmin || poll.creatorId === currentUserId)" 
                  size="small" 
                  type="danger" 
                  @click.stop="confirmEndPoll(poll.id)"
                >结束投票</el-button>
              </div>
            </div>
          </el-card>
        </div>
        
        <!-- 分页 -->
        <div class="pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[5, 10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>
    
    <!-- 创建投票对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="发起投票"
      width="500px"
      :close-on-click-modal="false"
      :before-close="handleCloseCreateDialog"
    >
      <el-form :model="pollForm" ref="pollFormRef" :rules="pollFormRules" label-width="100px">
        <el-form-item label="投票标题" prop="title">
          <el-input v-model="pollForm.title" placeholder="请输入投票标题/问题"></el-input>
        </el-form-item>
        
        <el-form-item label="投票描述">
          <el-input v-model="pollForm.description" type="textarea" :rows="2" placeholder="可选，添加补充说明"></el-input>
        </el-form-item>
        
        <el-form-item label="投票类型" prop="isMultiple">
          <el-radio-group v-model="pollForm.isMultiple">
            <el-radio :label="false">单选</el-radio>
            <el-radio :label="true">多选</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="投票设置">
          <el-checkbox v-model="pollForm.isAnonymous">匿名投票</el-checkbox>
          <el-tooltip content="启用后，其他人无法看到谁投了哪个选项" placement="top">
            <el-icon><QuestionFilled /></el-icon>
          </el-tooltip>
        </el-form-item>
        
        <!-- 结束时间选择 -->
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="pollForm.endTime"
            type="datetime"
            placeholder="可选，设置投票结束时间"
            format="YYYY-MM-DD HH:mm"
            :disabled-date="disablePastDates"
            :disabled-time="disablePastTime"
            :editable="false"
          />
        </el-form-item>
        
        <el-form-item label="投票选项" prop="options">
          <div v-for="(option, index) in pollForm.options" :key="index" class="option-item">
            <el-input v-model="pollForm.options[index]" placeholder="选项内容">
              <template #append>
                <el-button 
                  @click="removeOption(index)" 
                  :disabled="pollForm.options.length <= 2"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-input>
          </div>
          
          <div class="add-option" v-if="pollForm.options.length < 20">
            <el-button type="primary" @click="addOption" plain>添加选项</el-button>
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleCloseCreateDialog">取消</el-button>
          <el-button type="primary" @click="handleCreatePoll" :loading="creating">发起投票</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 投票详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="投票详情"
      width="600px"
    >
      <div v-if="loading" class="loading">
        <el-skeleton :rows="10" animated />
      </div>
      <div v-else-if="currentPoll" class="poll-detail">
        <div class="poll-header">
          <h2>{{ currentPoll.title }}</h2>
          <el-tag :type="getStatusTagType(currentPoll.status)">{{ getStatusText(currentPoll.status) }}</el-tag>
        </div>
        
        <div class="poll-info">
          <p v-if="currentPoll.description" class="description">{{ currentPoll.description }}</p>
          <div class="meta">
            <span>发起人: {{ currentPoll.creatorNickname }}</span>
            <span>创建时间: {{ formatTime(currentPoll.createdAt) }}</span>
            <span v-if="currentPoll.endTime">结束时间: {{ formatTime(currentPoll.endTime) }}</span>
            <span>投票类型: {{ currentPoll.isMultiple ? '多选' : '单选' }}</span>
            <span>{{ currentPoll.isAnonymous ? '匿名投票' : '公开投票' }}</span>
          </div>
        </div>
        
        <div class="poll-options">
          <template v-if="currentPoll.status === 'ACTIVE' && !currentPoll.hasVoted">
            <el-checkbox-group 
              v-if="currentPoll.isMultiple" 
              v-model="selectedOptions" 
              class="vote-options"
            >
              <div v-for="option in currentPoll.options" :key="option.id" class="vote-option">
                <el-checkbox :label="option.id">{{ option.optionText }}</el-checkbox>
              </div>
            </el-checkbox-group>
            
            <el-radio-group 
              v-else
              v-model="selectedOptions[0]"
              class="vote-options"
            >
              <div v-for="option in currentPoll.options" :key="option.id" class="vote-option">
                <el-radio :label="option.id">{{ option.optionText }}</el-radio>
              </div>
            </el-radio-group>
            
            <div class="vote-actions">
              <el-button type="primary" @click="handleVote" :loading="voting">投票</el-button>
            </div>
          </template>
          
          <div v-else class="poll-results">
            <div v-for="option in currentPoll.options" :key="option.id" class="result-item">
              <div class="option-header">
                <span class="option-text">{{ option.optionText }}</span>
                <span class="option-count">{{ option.voteCount }}票 ({{ option.percentage.toFixed(1) }}%)</span>
              </div>
              
              <el-progress :percentage="option.percentage" :stroke-width="15" />
              
              <div v-if="!currentPoll.isAnonymous && option.voters && option.voters.length > 0" class="voters">
                <el-avatar
                  v-for="voter in option.voters"
                  :key="voter.userId"
                  :size="24"
                  :src="voter.avatarUrl || ''"
                >{{ voter.nickname?.substring(0, 1) || '?' }}</el-avatar>
              </div>
            </div>
            
            <div class="poll-status">
              <span>共{{ currentPoll.totalVoters }}人参与投票</span>
              <span v-if="currentPoll.hasVoted" class="has-voted">已参与投票</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
// 解决类型问题的方法
import ElementPlus from 'element-plus';
// 定义类型别名
type ElFormInstance = InstanceType<typeof ElementPlus.ElForm>;
type ElFormRules = Record<string, any[]>;

import { createPoll, getGroupPolls, getPollDetails, votePoll, endPoll, cancelPoll } from '@/api/group';
import { formatDate } from '@/utils/helpers';

const props = defineProps({
  groupId: {
    type: Number,
    required: true
  },
  isGroupOwner: {
    type: Boolean,
    default: false
  },
  isGroupAdmin: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update']);

// 当前用户ID
const currentUserId = computed(() => {
  const userInfoStr = localStorage.getItem('userInfo');
  if (userInfoStr) {
    try {
      const userInfo = JSON.parse(userInfoStr);
      return userInfo?.id || 0;
    } catch (e) {
      console.error('解析用户信息失败:', e);
    }
  }
  return 0;
});

// 投票列表
const polls = ref<any[]>([]);
const loading = ref(false);
const creating = ref(false);
const voting = ref(false);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

// 筛选条件
const filterStatus = ref<string | null>(null);

// 创建投票表单
const showCreateDialog = ref(false);
const pollFormRef = ref<ElFormInstance | null>(null);
const pollForm = reactive({
  title: '',
  description: '',
  isMultiple: false,
  isAnonymous: false,
  endTime: null as Date | null,
  options: ['', '']
});

// 投票表单校验规则
const pollFormRules: ElFormRules = {
  title: [
    { required: true, message: '请输入投票标题', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  options: [
    { 
      validator: (rule: any, value: any, callback: any) => {
        const validOptions = value.filter((opt: string) => opt.trim());
        if (validOptions.length < 2) {
          callback(new Error('至少需要提供两个有效选项'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

// 投票详情
const showDetailDialog = ref(false);
const currentPollId = ref<number | null>(null);
const currentPoll = ref<any>({});
const selectedOptions = ref<number[]>([]);

// 处理分页大小变化
const handleSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1; // 重置到第一页
  loadPolls();
};

// 处理页码变化
const handleCurrentChange = (page: number) => {
  currentPage.value = page;
  loadPolls();
};

// 处理筛选条件变化
const handleFilterChange = () => {
  currentPage.value = 1; // 重置到第一页
  loadPolls();
};

// 关闭创建对话框
const handleCloseCreateDialog = () => {
  showCreateDialog.value = false;
  resetPollForm();
};

// 加载投票列表
const loadPolls = async () => {
  if (!props.groupId) return;
  
  loading.value = true;
  
  try {
    const params: {
      status?: string;
      page: number;
      size: number;
    } = {
      page: currentPage.value - 1,
      size: pageSize.value
    };
    
    if (filterStatus.value) {
      params.status = filterStatus.value;
    }
    
    const response = await getGroupPolls(props.groupId, params);
    
    if (response.code === 200) {
      polls.value = response.data.content || [];
      total.value = response.data.totalElements || 0;
    } else {
      ElMessage.error(response.message || '获取投票列表失败');
    }
  } catch (error) {
    console.error('获取投票列表失败:', error);
    ElMessage.error('获取投票列表失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 监听筛选条件和分页变化
watch([filterStatus, currentPage], () => {
  loadPolls();
});

onMounted(() => {
  loadPolls();
});

// 禁用过去的日期
const disablePastDates = (date: Date) => {
  // 获取当前日期（不含时间）
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return date.getTime() < today.getTime();
};

// 禁用过去的时间
const disablePastTime = (date: Date) => {
  const now = new Date();
  // 判断是否是今天
  if (
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  ) {
    // 如果是今天，禁用当前时间之前的时间
    return {
      disabledHours: () => {
        const hours = [];
        for (let i = 0; i < now.getHours(); i++) {
          hours.push(i);
        }
        return hours;
      },
      disabledMinutes: (hour: number) => {
        if (hour === now.getHours()) {
          const minutes = [];
          for (let i = 0; i < now.getMinutes(); i++) {
            minutes.push(i);
          }
          return minutes;
        }
        return [];
      }
    };
  }
  // 不是今天，不限制时间
  return {};
};

// 添加选项
const addOption = () => {
  if (pollForm.options.length >= 20) {
    ElMessage.warning('最多添加20个选项');
    return;
  }
  pollForm.options.push('');
};

// 删除选项
const removeOption = (index: number) => {
  if (pollForm.options.length <= 2) {
    ElMessage.warning('至少需要保留两个选项');
    return;
  }
  pollForm.options.splice(index, 1);
};

// 打开创建投票对话框
const openCreateDialog = () => {
  showCreateDialog.value = true;
  resetPollForm();
};

// 重置表单
const resetPollForm = () => {
  pollForm.title = '';
  pollForm.description = '';
  pollForm.isMultiple = false;
  pollForm.isAnonymous = false;
  pollForm.endTime = null;
  pollForm.options = ['', ''];
  
  // 重置表单校验
  if (pollFormRef.value) {
    pollFormRef.value.resetFields();
  }
};

const handleCreatePoll = async () => {
  if (!pollFormRef.value) return;
  
  await pollFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return;
    
    // 验证结束时间
    if (!validateEndTime()) return;
    
    // 过滤空选项
    const filteredOptions = pollForm.options.filter((opt: string) => opt.trim());
    if (filteredOptions.length < 2) {
      ElMessage.warning('至少需要提供两个有效选项');
      return;
    }
    
    creating.value = true;
    
    try {
      // 处理结束时间，确保转换为UTC时间
      let endTimeForServer = null;
      if (pollForm.endTime) {
        // 记录原始时间值以便调试
        const originalTime = pollForm.endTime;
        console.log('原始结束时间:', originalTime);
        console.log('本地时区偏移量(分钟):', new Date().getTimezoneOffset());
        
        // 创建一个新的时间对象，确保不改变原始对象
        const date = new Date(originalTime);
        
        // 确保时间有效，并比当前时间至少晚5分钟，避免边界问题
        const now = new Date();
        const fiveMinutesLater = new Date(now.getTime() + 5 * 60 * 1000);
        
        if (date <= fiveMinutesLater) {
          ElMessage.warning('结束时间必须至少比当前时间晚5分钟，以避免边界问题');
          creating.value = false;
          return;
        }
        
        // 添加显式时区偏移量
        const localOffset = date.getTimezoneOffset() * 60000; // 转换为毫秒
        const utcTime = new Date(date.getTime() - localOffset);
        
        // 创建一个 ISO 字符串，这将自动转换为 UTC 时间
        endTimeForServer = utcTime.toISOString();
        
        console.log('发送到服务器的时间(ISO UTC格式):', endTimeForServer);
        console.log('当前服务器时间(估计):', new Date().toISOString());
      }
      
      const pollData = {
        title: pollForm.title,
        description: pollForm.description || null,
        isMultiple: pollForm.isMultiple,
        isAnonymous: pollForm.isAnonymous,
        endTime: endTimeForServer,
        options: filteredOptions
      };
      
      console.log('发送投票创建请求:', pollData);
      
      const response = await createPoll(props.groupId, pollData);
      
      if (response.code === 200) {
        ElMessage.success('投票创建成功');
        showCreateDialog.value = false;
        resetPollForm();
        loadPolls(); // 刷新列表
      } else {
        ElMessage.error(response.message || '创建投票失败');
      }
    } catch (error) {
      console.error('创建投票失败:', error);
      ElMessage.error('创建投票失败，请稍后重试');
    } finally {
      creating.value = false;
    }
  });
};

// 查看投票详情
const viewPollDetails = async (pollId: number) => {
  currentPollId.value = pollId;
  loadPollDetails();
  showDetailDialog.value = true;
};

// 加载投票详情
const loadPollDetails = async () => {
  if (!currentPollId.value || !props.groupId) return;
  
  loading.value = true;
  selectedOptions.value = []; // 重置选择
  
  try {
    const response = await getPollDetails(props.groupId, currentPollId.value);
    
    if (response.code === 200) {
      currentPoll.value = response.data;
      
      // 如果已投票且是单选，预设选中项
      if (currentPoll.value.hasVoted && !currentPoll.value.isMultiple) {
        const selectedOption = currentPoll.value.options.find((opt: any) => opt.isSelected);
        if (selectedOption) {
          selectedOptions.value = [selectedOption.id];
        }
      }
      
      // 如果已投票且是多选，预设多个选中项
      if (currentPoll.value.hasVoted && currentPoll.value.isMultiple) {
        selectedOptions.value = currentPoll.value.options
          .filter((opt: any) => opt.isSelected)
          .map((opt: any) => opt.id);
      }
    } else {
      ElMessage.error(response.message || '获取投票详情失败');
    }
  } catch (error) {
    console.error('获取投票详情失败:', error);
    ElMessage.error('获取投票详情失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 参与投票
const handleVote = async () => {
  if (!currentPoll.value || !currentPoll.value.id) {
    ElMessage.warning('投票信息不完整');
    return;
  }
  
  if (!currentPoll.value.isMultiple && !selectedOptions.value[0]) {
    ElMessage.warning('请选择一个选项');
    return;
  }
  
  voting.value = true;
  
  try {
    // 确保选项IDs为数值
    const optionIds = currentPoll.value.isMultiple 
      ? selectedOptions.value.map(id => Number(id)) 
      : [Number(selectedOptions.value[0])];
    
    console.log('准备发送投票请求: ', {
      pollId: currentPoll.value.id,
      isMultiple: currentPoll.value.isMultiple,
      selectedOptions: selectedOptions.value,
      optionIds: optionIds
    });
    
    const response = await votePoll(props.groupId, currentPoll.value.id, { optionIds });
    
    if (response.code === 200) {
      ElMessage.success('投票成功');
      currentPoll.value = response.data;
      
      // 刷新列表
      loadPolls();
    } else {
      ElMessage.error(response.message || '投票失败');
    }
  } catch (error) {
    console.error('投票失败:', error);
    ElMessage.error('投票失败，请稍后重试');
  } finally {
    voting.value = false;
  }
};

// 确认结束投票
const confirmEndPoll = (pollId: number) => {
  ElMessageBox.confirm(
    '结束后投票将不再接受新的投票，是否继续？',
    '结束投票',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(() => {
      handleEndPoll(pollId);
    })
    .catch(() => {
      // 用户取消操作
    });
};

// 结束投票
const handleEndPoll = async (pollId: number) => {
  if (!props.groupId) return;
  
  try {
    const response = await endPoll(props.groupId, pollId);
    
    if (response.code === 200) {
      ElMessage.success('投票已结束');
      
      // 更新列表和详情
      loadPolls();
      
      // 如果当前正在查看该投票，更新详情
      if (currentPollId.value === pollId) {
        loadPollDetails();
      }
    } else {
      ElMessage.error(response.message || '结束投票失败');
    }
  } catch (error) {
    console.error('结束投票失败:', error);
    ElMessage.error('结束投票失败，请稍后重试');
  }
};

// 辅助函数
const getStatusText = (status: string) => {
  switch (status) {
    case 'ACTIVE': return '进行中';
    case 'ENDED': return '已结束';
    case 'CANCELED': return '已取消';
    default: return '未知状态';
  }
};

const getStatusTagType = (status: string) => {
  switch (status) {
    case 'ACTIVE': return 'success';
    case 'ENDED': return 'info';
    case 'CANCELED': return 'danger';
    default: return 'info';
  }
};

// 格式化时间的辅助函数
const formatTime = (time: string | null) => {
  if (!time) return '';
  
  try {
    // 记录原始时间字符串
    console.log('格式化前的时间字符串:', time);
    
    // 创建日期对象
    const date = new Date(time);
    
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
      console.error('无效的日期时间字符串:', time);
      return '时间格式错误';
    }
    
    // 格式化为本地时间
    const formattedDate = new Intl.DateTimeFormat('zh-CN', {
      year: 'numeric', 
      month: '2-digit', 
      day: '2-digit',
      hour: '2-digit', 
      minute: '2-digit',
      hour12: false
    }).format(date);
    
    console.log('格式化后的本地时间:', formattedDate);
    return formattedDate;
  } catch (error) {
    console.error('格式化时间出错:', error);
    return '时间格式错误';
  }
};

// 格式化日期函数
const getFormattedDate = computed(() => {
  return (date: Date) => {
    return formatDate(date, 'yyyy-MM-dd HH:mm');
  };
});

// 检查结束时间是否有效
const validateEndTime = () => {
  if (!pollForm.endTime) return true; // 结束时间可选
  
  const now = new Date();
  const selectedDate = new Date(pollForm.endTime);
  
  if (selectedDate <= now) {
    ElMessage.warning('结束时间必须晚于当前时间');
    return false;
  }
  
  return true;
};
</script>

<style scoped>
.group-polls {
  padding: 16px;
}

.create-poll-btn-container {
  margin-bottom: 16px;
}

.filters {
  margin-bottom: 16px;
}

.polls-list {
  margin-top: 20px;
}

.poll-item {
  margin-bottom: 16px;
}

.poll-header {
  display: flex;
  flex-direction: column;
  margin-bottom: 12px;
}

.poll-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.poll-title h3 {
  margin: 0;
  font-size: 16px;
  flex: 1;
}

.poll-creator {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
  display: flex;
  justify-content: space-between;
}

.poll-options {
  margin: 12px 0;
}

.poll-option {
  margin-bottom: 8px;
}

.option-text {
  font-size: 14px;
  margin-bottom: 4px;
}

.poll-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.poll-stats {
  font-size: 13px;
  color: #666;
}

.has-voted {
  color: #409EFF;
  font-weight: bold;
  margin-left: 8px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 创建投票表单样式 */
.option-item {
  margin-bottom: 8px;
}

.add-option {
  margin-top: 8px;
}

/* 投票详情样式 */
.poll-detail {
  padding: 0 10px;
}

.poll-detail .poll-header {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.poll-detail h2 {
  margin: 0;
  font-size: 18px;
}

.description {
  margin: 16px 0;
  color: #666;
  white-space: pre-line;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.vote-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 20px 0;
}

.vote-actions {
  margin-top: 20px;
}

.poll-results {
  margin-top: 16px;
}

.result-item {
  margin-bottom: 16px;
}

.option-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}

.option-text {
  font-weight: 500;
}

.option-count {
  color: #606266;
}

.voters {
  margin-top: 8px;
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.poll-status {
  margin-top: 16px;
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 10px;
}
</style> 