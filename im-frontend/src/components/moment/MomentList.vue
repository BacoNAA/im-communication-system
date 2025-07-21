<template>
  <div class="moment-list-container">
    <div v-if="loading && !moments.length" class="loading-container">
      <el-skeleton :rows="3" animated />
      <el-skeleton :rows="3" animated class="mt-4" />
    </div>
    
    <div v-if="!loading && !moments.length" class="empty-container">
      <el-empty description="暂无动态" />
    </div>
    
    <div v-else class="moment-list">
      <div v-for="moment in moments" :key="moment.id" class="moment-item">
        <moment-card 
          :moment="moment" 
          @like="handleLike" 
          @comment="handleComment"
        />
      </div>
      
      <div v-if="loading && moments.length" class="loading-more">
        <el-skeleton :rows="2" animated />
      </div>
      
      <div v-if="noMoreData" class="no-more">
        没有更多内容了
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { ElSkeleton, ElEmpty } from 'element-plus';
import MomentCard from './MomentCard.vue';
import { api } from '@/api/request';
import { getMomentTimeline, getUserMoments, likeMoment, unlikeMoment } from '@/api/moment';

const props = defineProps({
  userId: {
    type: Number,
    default: null
  }
});

const emit = defineEmits(['refresh', 'comment']);

// 状态
const moments = ref([]);
const loading = ref(false);
const page = ref(0);
const size = ref(10);
const noMoreData = ref(false);
const scrollHandler = ref(null);

// 加载动态列表
// 使用专用的API函数
const loadMoments = async (isRefresh = false) => {
  if (loading.value || (noMoreData.value && !isRefresh)) return;
  
  loading.value = true;
  console.log('开始加载动态列表, 刷新:', isRefresh, '页码:', page.value, '用户ID:', props.userId);
  
  try {
    const currentPage = isRefresh ? 0 : page.value;
    let response;
    
    // 使用API模块中的函数
    if (props.userId) {
      console.log(`加载用户 ${props.userId} 的动态（包含私有动态）`);
      response = await getUserMoments(props.userId, currentPage, size.value);
    } else {
      console.log('加载朋友圈动态时间线（包含所有动态，后端会根据隐私属性过滤）');
      response = await getMomentTimeline(currentPage, size.value);
    }
    
    console.log('动态加载响应:', response);
    
    if (response && response.success && response.data) {
      const { content, last } = response.data;
      
      console.log(`获取到 ${content?.length || 0} 条动态, 是否最后一页: ${last}`);
      
      if (content && content.length > 0) {
        if (isRefresh) {
          moments.value = content;
        } else {
          moments.value = [...moments.value, ...content];
        }
      } else {
        console.log('未获取到动态数据或数据为空');
      }
      
      noMoreData.value = last;
      page.value = currentPage + 1;
    } else {
      console.warn('API响应格式不符合预期:', response);
    }
  } catch (error) {
    console.error('加载动态列表失败:', error);
  } finally {
    loading.value = false;
    console.log('动态列表加载完成, 当前列表长度:', moments.value.length, '用户ID:', props.userId);
  }
};

// 刷新动态列表
const refresh = () => {
  page.value = 0;
  noMoreData.value = false;
  loadMoments(true);
  emit('refresh');
};

// 滚动加载更多
const handleScroll = () => {
  const scrollElement = document.querySelector('.moment-list-container');
  if (!scrollElement) return;
  
  const { scrollTop, scrollHeight, clientHeight } = scrollElement;
  
  // 当滚动到距离底部100px时，加载更多数据
  if (scrollHeight - scrollTop - clientHeight < 100 && !loading.value && !noMoreData.value) {
    loadMoments();
  }
};

// 点赞动态
const handleLike = async (momentId, isLiked) => {
  try {
    if (isLiked) {
      // 已点赞，执行取消点赞
      await unlikeMoment(momentId);
    } else {
      // 未点赞，执行点赞
      await likeMoment(momentId);
    }
    
    // 更新本地状态
    const index = moments.value.findIndex(moment => moment.id === momentId);
    if (index !== -1) {
      const moment = moments.value[index];
      moment.isLiked = !isLiked;
      moment.likeCount = isLiked 
        ? Math.max(0, moment.likeCount - 1)
        : moment.likeCount + 1;
    }
  } catch (error) {
    console.error('Failed to like/unlike moment:', error);
  }
};

// 评论动态
const handleComment = (momentId) => {
  // 触发评论事件，将由父组件处理
  emit('comment', momentId);
};

// 组件挂载时加载数据并添加滚动监听
onMounted(() => {
  loadMoments();
  
  const scrollElement = document.querySelector('.moment-list-container');
  if (scrollElement) {
    scrollHandler.value = handleScroll;
    scrollElement.addEventListener('scroll', scrollHandler.value);
  }
});

// 组件卸载时移除滚动监听
onUnmounted(() => {
  const scrollElement = document.querySelector('.moment-list-container');
  if (scrollElement && scrollHandler.value) {
    scrollElement.removeEventListener('scroll', scrollHandler.value);
  }
});

// 暴露方法
defineExpose({
  refresh
});
</script>

<style scoped>
.moment-list-container {
  height: 100%;
  overflow-y: auto;
  padding: 16px;
}

.loading-container, 
.empty-container {
  padding: 20px;
}

.moment-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.moment-item {
  background: var(--el-bg-color);
  border-radius: 8px;
  overflow: hidden;
}

.loading-more {
  padding: 16px 0;
  text-align: center;
}

.no-more {
  text-align: center;
  padding: 16px 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
</style> 