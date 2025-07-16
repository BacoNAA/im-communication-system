<template>
  <div class="emoji-picker">
    <div class="emoji-categories">
      <button 
        v-for="(category, index) in categories" 
        :key="index"
        class="category-button"
        :class="{ active: currentCategory === category.name }"
        @click="selectCategory(category.name)"
      >
        {{ category.icon }}
      </button>
    </div>
    <div class="emoji-list">
      <button 
        v-for="emoji in currentEmojis" 
        :key="emoji"
        class="emoji-button"
        @click="selectEmoji(emoji)"
        :title="emoji"
      >
        {{ emoji }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';

// 定义props
const props = defineProps({
  onSelect: {
    type: Function,
    required: true
  }
});

// 定义事件
const emit = defineEmits(['select']);

// 表情分类
const categories = [
  { name: 'smileys', icon: '😊', emojis: ['😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '😊', '😇', '🙂', '🙃', '😉', '😌', '😍', '🥰', '😘', '😗', '😙', '😚', '😋', '😛', '😝', '😜', '🤪', '🤨', '🧐', '🤓', '😎', '🤩', '🥳'] },
  { name: 'gestures', icon: '👍', emojis: ['👍', '👎', '👌', '✌️', '🤞', '🤟', '🤘', '🤙', '👈', '👉', '👆', '👇', '☝️', '✋', '🤚', '🖐️', '🖖', '👋', '🤏', '✍️', '👏', '👐', '🙌', '🤲', '🙏', '🤝'] },
  { name: 'animals', icon: '🐱', emojis: ['🐶', '🐱', '🐭', '🐹', '🐰', '🦊', '🐻', '🐼', '🐨', '🐯', '🦁', '🐮', '🐷', '🐸', '🐵', '🙈', '🙉', '🙊', '🐔', '🐧', '🐦', '🐤', '🦆', '🦅', '🦉'] },
  { name: 'food', icon: '🍎', emojis: ['🍎', '🍐', '🍊', '🍋', '🍌', '🍉', '🍇', '🍓', '🍈', '🍒', '🍑', '🥭', '🍍', '🥥', '🥝', '🍅', '🍆', '🥑', '🥦', '🥬', '🥒', '🌶️', '🌽', '🥕', '🧄', '🧅', '🥔', '🍠', '🥐', '🥯', '🍞', '🥖', '🥨', '🧀', '🥚', '🍳', '🧈', '🥞', '🧇', '🥓', '🍔', '🍟', '🍕', '🌭', '🥪', '🌮', '🌯', '🥙', '🧆', '🥘', '🍝', '🥫', '🥣', '🥗', '🍲', '🍛', '🍜', '🍢', '🍱', '🍚', '🍘', '🍙', '🍤', '🍣', '🦞', '🦪', '🍦', '🍧', '🍨', '🍩', '🍪', '🎂', '🍰', '🧁', '🥧', '🍫', '🍬', '🍭', '🍮', '🍯', '🥛', '☕', '🍵', '🍶', '🍾', '🍷', '🍸', '🍹'] },
  { name: 'travel', icon: '✈️', emojis: ['🚗', '🚕', '🚙', '🚌', '🚎', '🏎️', '🚓', '🚑', '🚒', '🚐', '🚚', '🚛', '🚜', '🛴', '🚲', '🛵', '🏍️', '🚔', '🚍', '🚘', '🚖', '✈️', '🛫', '🛬', '🚀', '🛸', '🚁', '🛶', '⛵', '🚤', '🛥️', '🛳️', '⛴️', '🚢', '🚂', '🚆', '🚇', '🚊', '🚉', '🚁', '🚞', '🚟', '🚠', '🚡', '🚃', '🚋', '🚝', '🚄', '🚅', '🚈', '🚞', '🚂'] },
  { name: 'symbols', icon: '❤️', emojis: ['❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟', '☮️', '✝️', '☪️', '🕉️', '☸️', '✡️', '🔯', '🕎', '☯️', '☦️', '🛐', '⛎', '♈', '♉', '♊', '♋', '♌', '♍', '♎', '♏', '♐', '♑', '♒', '♓', '🆔', '⚛️'] }
];

// 当前选择的分类
const currentCategory = ref('smileys');

// 根据当前分类获取表情
const currentEmojis = computed(() => {
  const category = categories.find(c => c.name === currentCategory.value);
  return category ? category.emojis : [];
});

// 选择分类
const selectCategory = (categoryName: string) => {
  console.log('选择表情分类:', categoryName);
  currentCategory.value = categoryName;
};

// 选择表情
const selectEmoji = (emoji: string) => {
  console.log('选择表情:', emoji);
  // 只调用一次回调函数，避免重复插入表情
  props.onSelect(emoji);
  // 移除下面这行，防止重复触发
  // emit('select', emoji);
};

// 组件挂载时的调试信息
onMounted(() => {
  console.log('EmojiPicker 组件已挂载');
});
</script>

<style scoped>
.emoji-picker {
  width: 300px;
  background-color: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.emoji-categories {
  display: flex;
  border-bottom: 1px solid #e0e0e0;
  background-color: #f8f8f8;
}

.category-button {
  flex: 1;
  padding: 8px;
  font-size: 16px;
  background: none;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.category-button:hover {
  background-color: #f0f0f0;
}

.category-button.active {
  background-color: #e6f7ff;
  border-bottom: 2px solid #1890ff;
}

.emoji-list {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
  padding: 8px;
  max-height: 200px;
  overflow-y: auto;
}

.emoji-button {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 30px;
  height: 30px;
  font-size: 18px;
  background: none;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.emoji-button:hover {
  background-color: #f0f0f0;
}
</style> 