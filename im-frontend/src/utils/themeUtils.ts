/**
 * 主题相关工具函数
 * 用于应用主题颜色、字体大小等设置到全局CSS变量
 */

// 保存当前的主题设置，以便在DOM变化时应用
let currentThemeColor = '#1890ff';
let currentFontSize = 14;
let currentChatBackground = 'default';

// 创建一个MutationObserver来监听DOM变化
let observer: MutationObserver | null = null;

// 初始化MutationObserver
const initThemeObserver = () => {
  if (observer) return; // 避免重复初始化
  
  observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      if (mutation.type === 'childList') {
        // 检查是否有新的消息容器被添加
        const addedNodes = Array.from(mutation.addedNodes);
        for (const node of addedNodes) {
          if (node instanceof HTMLElement) {
            // 只查找新添加的消息容器
            const messageContainers = node.querySelectorAll('.message-container');
            if (messageContainers.length > 0) {
              console.log('检测到新的消息容器，应用当前背景设置');
              
              // 不再对消息容器直接应用背景，避免与ChatMessage.vue中的透明设置冲突
              // 背景应该通过CSS变量在messages-area上应用
              
              // 移除对消息相关元素的直接样式设置
              // 这些样式已经在ChatMessage.vue中正确设置
            }
          }
        }
      }
    });
  });
  
  // 开始观察document.body的变化
  observer.observe(document.body, { 
    childList: true,
    subtree: true
  });
  
  console.log('主题观察器已初始化');
};

/**
 * 初始化主题设置
 * 这个函数会在应用启动时被调用，确保主题设置被正确加载和应用
 */
export const initTheme = () => {
  console.log('初始化主题设置');
  
  // 尝试从localStorage加载主题设置
  try {
    const savedSettings = localStorage.getItem('userSettings');
    if (savedSettings) {
      const settings = JSON.parse(savedSettings);
      console.log('从localStorage加载到主题设置:', settings);
      
      if (settings.theme) {
        // 应用主题颜色
        if (settings.theme.color) {
          console.log('应用保存的主题颜色:', settings.theme.color);
          applyThemeColor(settings.theme.color);
        }
        
        // 应用字体大小
        if (settings.theme.fontSize) {
          console.log('应用保存的字体大小:', settings.theme.fontSize);
          applyFontSize(settings.theme.fontSize);
        }
        
        // 应用聊天背景
        if (settings.theme.chatBackground) {
          console.log('应用保存的聊天背景:', settings.theme.chatBackground);
          applyBackground(settings.theme.chatBackground);
        }
      }
    } else {
      console.log('未找到保存的主题设置，使用默认设置');
      applyThemeColor('#1890ff');
      applyFontSize(14);
      applyBackground('default');
    }
  } catch (error) {
    console.error('加载主题设置失败:', error);
    // 应用默认设置
    applyThemeColor('#1890ff');
    applyFontSize(14);
    applyBackground('default');
  }
  
  // 初始化MutationObserver
  initThemeObserver();
};

/**
 * 应用主题颜色到CSS变量
 * @param color 主题颜色，十六进制格式，如 #1890ff
 */
export const applyThemeColor = (color: string): void => {
  // 保存当前颜色
  currentThemeColor = color;
  
  // 设置主题主色调
  document.documentElement.style.setProperty('--primary-color', color);
  
  // 计算衍生颜色
  const r = parseInt(color.slice(1, 3), 16);
  const g = parseInt(color.slice(3, 5), 16);
  const b = parseInt(color.slice(5, 7), 16);
  
  // 设置浅色变体 (用于背景、悬停等)
  document.documentElement.style.setProperty(
    '--primary-light-color', 
    `rgba(${r}, ${g}, ${b}, 0.2)`
  );
  
  // 设置深色变体 (用于激活状态、边框等)
  const darken = (c: number) => Math.max(0, c - 40);
  document.documentElement.style.setProperty(
    '--primary-dark-color',
    `rgb(${darken(r)}, ${darken(g)}, ${darken(b)})`
  );
  
  // 设置对比色 (用于按钮文字等)
  document.documentElement.style.setProperty(
    '--primary-contrast-color',
    isLightColor(r, g, b) ? '#000000' : '#ffffff'
  );
  
  // 直接应用到一些特定的元素
  try {
    // 应用到标签项
    const activeTabItems = document.querySelectorAll('.tab-item.active');
    activeTabItems.forEach(item => {
      (item as HTMLElement).style.color = color;
    });
    
    // 应用到按钮
    const primaryButtons = document.querySelectorAll('.btn-primary, .save-btn, .action-btn');
    primaryButtons.forEach(btn => {
      (btn as HTMLElement).style.backgroundColor = color;
    });
    
    // 应用到链接
    const links = document.querySelectorAll('a, .link, .clickable');
    links.forEach(link => {
      (link as HTMLElement).style.color = color;
    });
    
    // 应用到边框
    const borderElements = document.querySelectorAll('.border-primary, .focus-highlight:focus, .active-highlight.active');
    borderElements.forEach(el => {
      (el as HTMLElement).style.borderColor = color;
    });
    
    // 应用到选择状态
    const selectedElements = document.querySelectorAll('.selected-indicator, .active-indicator, .switch-active, .checkbox-checked');
    selectedElements.forEach(el => {
      (el as HTMLElement).style.backgroundColor = color;
    });
    
    // 应用到徽章
    const badges = document.querySelectorAll('.badge, .notification-dot');
    badges.forEach(badge => {
      (badge as HTMLElement).style.backgroundColor = color;
    });
  } catch (error) {
    console.error('直接应用主题颜色到元素时出错:', error);
  }
  
  console.log(`应用了主题颜色: ${color}`);
};

/**
 * 应用字体大小到CSS变量
 * @param size 字体大小，单位为px
 */
export const applyFontSize = (size: number): void => {
  // 保存当前字体大小
  currentFontSize = size;
  
  // 设置基础字体大小
  document.documentElement.style.setProperty('--font-size-base', `${size}px`);
  
  // 设置衍生字体大小
  document.documentElement.style.setProperty('--font-size-small', `${size * 0.85}px`);
  document.documentElement.style.setProperty('--font-size-large', `${size * 1.25}px`);
  document.documentElement.style.setProperty('--font-size-xlarge', `${size * 1.5}px`);
  document.documentElement.style.setProperty('--font-size-title', `${size * 1.75}px`);
  
  // 直接应用到一些特定的元素
  try {
    // 应用到基础文本
    document.body.style.fontSize = `${size}px`;
    
    // 应用到消息文本
    const messageTexts = document.querySelectorAll('.message-text, .chat-input');
    messageTexts.forEach(text => {
      (text as HTMLElement).style.fontSize = `${size}px`;
    });
    
    // 应用到设置项文本
    const settingsTexts = document.querySelectorAll('.settings-label, .settings-value');
    settingsTexts.forEach(text => {
      (text as HTMLElement).style.fontSize = `${size}px`;
    });
    
    // 应用到导航项文本
    const navItems = document.querySelectorAll('.nav-item, .sidebar-item');
    navItems.forEach(item => {
      (item as HTMLElement).style.fontSize = `${size}px`;
    });
    
    // 应用到列表项文本
    const listItems = document.querySelectorAll('.list-item, .menu-item, .dropdown-item');
    listItems.forEach(item => {
      (item as HTMLElement).style.fontSize = `${size}px`;
    });
    
    // 应用到对话列表文本
    const conversationNames = document.querySelectorAll('.conversation-name');
    conversationNames.forEach(name => {
      (name as HTMLElement).style.fontSize = `${size}px`;
    });
    
    // 应用到对话预览文本
    const conversationPreviews = document.querySelectorAll('.conversation-preview');
    conversationPreviews.forEach(preview => {
      (preview as HTMLElement).style.fontSize = `${size * 0.85}px`;
    });
  } catch (error) {
    console.error('直接应用字体大小到元素时出错:', error);
  }
  
  console.log(`应用了字体大小: ${size}px`);
};

/**
 * 判断颜色是否为浅色
 * 用于决定在该背景上使用深色还是浅色文字
 */
const isLightColor = (r: number, g: number, b: number): boolean => {
  // 使用YIQ公式计算亮度
  // 参考: https://stackoverflow.com/questions/11867545
  return ((r * 299) + (g * 587) + (b * 114)) / 1000 > 128;
};

/**
 * 应用背景到指定元素集合
 */
const applyBackgroundToElements = (elements: NodeListOf<Element> | HTMLElement[], bg: string): void => {
  elements.forEach((panel) => {
    const element = panel as HTMLElement;
    if (bg === 'default') {
      element.style.background = '';
      element.style.backgroundImage = 'none';
    } else if (bg.startsWith('#')) {
      element.style.background = bg;
      element.style.backgroundImage = 'none';
    } else {
      element.style.backgroundImage = `url(${bg})`;
      element.style.backgroundSize = 'cover';
      element.style.backgroundPosition = 'center';
    }
  });
};

/**
 * 应用聊天背景
 * @param bg 背景值，可以是颜色代码或图片URL
 */
export const applyBackground = (bg: string): void => {
  // 保存当前背景设置
  currentChatBackground = bg;
  
  // 不再直接设置message-container的背景，而是通过CSS变量来处理
  // 这样可以避免与ChatMessage.vue中的透明背景设置冲突
  
  // 保存背景设置到CSS变量，以便新创建的聊天面板也能应用相同的背景
  if (bg === 'default') {
    document.documentElement.style.setProperty('--chat-background', '#f9f9f9');
    document.documentElement.style.setProperty('--chat-background-image', 'none');
  } else if (bg.startsWith('#')) {
    // 增强背景颜色深度，使其更加明显
    // 将颜色转换为RGB格式
    const r = parseInt(bg.slice(1, 3), 16);
    const g = parseInt(bg.slice(3, 5), 16);
    const b = parseInt(bg.slice(5, 7), 16);
    
    // 降低亮度，增加饱和度，使颜色更深
    const darken = (c: number) => Math.max(0, Math.floor(c * 0.85));
    const deeperColor = `rgb(${darken(r)}, ${darken(g)}, ${darken(b)})`;
    
    document.documentElement.style.setProperty('--chat-background', deeperColor);
    document.documentElement.style.setProperty('--chat-background-image', 'none');
  } else {
    document.documentElement.style.setProperty('--chat-background', 'transparent');
    document.documentElement.style.setProperty('--chat-background-image', `url(${bg})`);
  }
  
  // 移除对消息相关元素的直接样式设置
  // 这些样式已经在ChatMessage.vue中正确设置为透明背景
  
  console.log(`应用了聊天背景: ${bg}`);
  
  // 确保观察器已初始化
  initThemeObserver();
};