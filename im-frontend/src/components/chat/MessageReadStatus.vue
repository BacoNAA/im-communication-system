<template>
  <div class="message-read-status" :class="{ 'status-read': isRead }">
    <span v-if="status === 'SENDING'" class="status-sending" :title="statusText">
      <i class="status-icon">⏳</i>
    </span>
    <span v-else-if="!isRead" class="status-unread" :title="statusText">
      <i class="status-icon">✓</i>
    </span>
    <span v-else-if="isRead" class="status-read" :title="statusText">
      <i class="status-icon">👁️</i>
    </span>
    <span v-else-if="status === 'FAILED'" class="status-failed" :title="statusText">
      <i class="status-icon">✗</i>
    </span>
    <span v-if="showStatusText" class="status-text">{{ statusText }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { MessageStatus } from '@/api/message';

const props = defineProps<{
  status?: string | undefined;
  isRead?: boolean;
  showStatusText?: boolean;
}>();

// 计算是否已读
const isRead = computed(() => {
  return props.isRead === true;
});

// 获取状态文本
const statusText = computed(() => {
  if (props.status === MessageStatus.SENDING) {
    return '发送中';
  } else if (props.status === MessageStatus.FAILED) {
    return '发送失败';
  } else if (props.isRead) {
    return '已读';
  } else {
    return '未读';
  }
});
</script>

<style scoped>
.message-read-status {
  font-size: 12px;
  color: #999;
  margin-left: 4px;
  display: inline-flex;
  align-items: center;
}

.status-icon {
  font-style: normal;
  margin-right: 2px;
  font-weight: bold;
}

.status-text {
  margin-left: 2px;
  font-size: 10px;
}

.status-sending {
  color: #999;
}

.status-unread {
  color: #999;
}

.status-read {
  color: #409EFF;
}

.status-failed {
  color: #f56c6c;
}
</style> 