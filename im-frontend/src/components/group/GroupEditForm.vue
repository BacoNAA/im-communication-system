<!-- 群组编辑表单组件 -->
<template>
  <div class="group-edit-form">
    <el-form :model="formData" :rules="rules" ref="formRef" label-width="120px">
      <el-form-item label="群组名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入群组名称" />
      </el-form-item>
      
      <el-form-item label="群组介绍" prop="description">
        <el-input 
          v-model="formData.description" 
          type="textarea" 
          :rows="3" 
          placeholder="请输入群组介绍" 
        />
      </el-form-item>
      
      <el-form-item label="群组头像" prop="avatarUrl">
        <el-upload
          class="avatar-uploader"
          action="/api/public-files/upload-group-avatar"
          :headers="uploadHeaders"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
          :before-upload="beforeAvatarUpload"
          :data="{ groupId: groupId }"
        >
          <img v-if="formData.avatarUrl" :src="formData.avatarUrl" class="avatar" />
          <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
        </el-upload>
        <div class="avatar-tip">点击上传头像，建议使用正方形图片</div>
      </el-form-item>
      
      <el-form-item label="入群审批" prop="requiresApproval" v-if="isOwner">
        <el-switch
          v-model="formData.requiresApproval"
          :active-text="formData.requiresApproval ? '需要审批' : '无需审批'"
        />
        <div class="approval-tip">开启后，新成员加入需要群主或管理员审批</div>
      </el-form-item>
      
      <el-form-item>
        <el-button type="primary" @click="submitForm">保存修改</el-button>
        <el-button @click="cancel">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script lang="ts">
import { ref, reactive, defineComponent, onMounted } from 'vue';
import type { PropType } from 'vue';
import { ElMessage } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { updateGroup } from '@/api/group';

export default defineComponent({
  name: 'GroupEditForm',
  components: {
    Plus
  },
  props: {
    groupId: {
      type: Number,
      required: true
    },
    initialData: {
      type: Object as PropType<{
        name?: string;
        description?: string;
        avatarUrl?: string;
        requiresApproval?: boolean;
      }>,
      default: () => ({})
    },
    isOwner: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:success', 'cancel'],
  setup(props, { emit }) {
    const formRef = ref();
    
    // 表单数据
    const formData = reactive({
      name: props.initialData.name || '',
      description: props.initialData.description || '',
      avatarUrl: props.initialData.avatarUrl || '',
      requiresApproval: props.initialData.requiresApproval || false
    });
    
    // 表单验证规则
    const rules = {
      name: [
        { required: true, message: '请输入群组名称', trigger: 'blur' },
        { min: 2, max: 50, message: '群组名称长度应为2-50个字符', trigger: 'blur' }
      ]
    };
    
    // 上传头像的请求头
    const uploadHeaders = reactive({
      Authorization: `Bearer ${localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken') || ''}`
    });
    
    // 头像上传成功回调
    const handleAvatarSuccess = (response: any) => {
      if (response.code === 200 && response.data) {
        const avatarUrl = response.data.avatarUrl || response.data.imageUrl;
        formData.avatarUrl = avatarUrl;
        
        // 自动更新群组信息
        updateGroup(props.groupId, { avatarUrl })
          .then(updateResponse => {
            if (updateResponse.code === 200) {
              ElMessage.success('头像上传并更新成功');
            } else {
              ElMessage.warning('头像上传成功，但更新群组信息失败');
            }
          })
          .catch(error => {
            console.error('更新群组信息失败:', error);
            ElMessage.warning('头像上传成功，但更新群组信息失败');
          });
      } else {
        ElMessage.error(response.message || '头像上传失败');
      }
    };
    
    // 头像上传前的验证
    const beforeAvatarUpload = (file: File) => {
      const isImage = file.type.startsWith('image/');
      const isLt2M = file.size / 1024 / 1024 < 2;
      
      if (!isImage) {
        ElMessage.error('上传头像图片只能是图片格式!');
        return false;
      }
      
      if (!isLt2M) {
        ElMessage.error('上传头像图片大小不能超过 2MB!');
        return false;
      }
      
      return true;
    };
    
    // 提交表单
    const submitForm = async () => {
      if (!formRef.value) return;
      
      try {
        await formRef.value.validate();
        
        // 构建更新数据
        const updateData: Record<string, any> = {};
        
        // 只包含已修改的字段
        if (formData.name !== props.initialData.name) {
          updateData.name = formData.name;
        }
        
        if (formData.description !== props.initialData.description) {
          updateData.description = formData.description;
        }
        
        if (formData.avatarUrl !== props.initialData.avatarUrl) {
          updateData.avatarUrl = formData.avatarUrl;
        }
        
        if (props.isOwner && formData.requiresApproval !== props.initialData.requiresApproval) {
          updateData.requiresApproval = formData.requiresApproval;
        }
        
        // 如果没有修改任何内容，直接返回
        if (Object.keys(updateData).length === 0) {
          ElMessage.info('未修改任何内容');
          emit('cancel');
          return;
        }
        
        // 调用API更新群组信息
        const response = await updateGroup(props.groupId, updateData);
        
        if (response.code === 200) {
          ElMessage.success('群组信息更新成功');
          emit('update:success', response.data);
        } else {
          ElMessage.error(response.message || '群组信息更新失败');
        }
      } catch (error) {
        console.error('更新群组信息失败:', error);
        ElMessage.error('表单验证失败，请检查输入');
      }
    };
    
    // 取消编辑
    const cancel = () => {
      emit('cancel');
    };
    
    return {
      formRef,
      formData,
      rules,
      uploadHeaders,
      handleAvatarSuccess,
      beforeAvatarUpload,
      submitForm,
      cancel
    };
  }
});
</script>

<style scoped>
.group-edit-form {
  padding: 20px;
  max-width: 600px;
  margin: 0 auto;
}

.avatar-uploader {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100px;
  height: 100px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.avatar-uploader:hover {
  border-color: #409eff;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.approval-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style> 