/**
 * 主题相关工具函数
 * 用于应用主题颜色、字体大小等设置到全局CSS变量
 */

/**
 * 应用主题颜色到CSS变量
 * @param color 主题颜色，十六进制格式，如 #1890ff
 */
export const applyThemeColor = (color: string): void => {
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
  
  console.log(`应用了主题颜色: ${color}`);
};

/**
 * 应用字体大小到CSS变量
 * @param size 字体大小，单位为px
 */
export const applyFontSize = (size: number): void => {
  // 设置基础字体大小
  document.documentElement.style.setProperty('--font-size-base', `${size}px`);
  
  // 设置衍生字体大小
  document.documentElement.style.setProperty('--font-size-small', `${size * 0.85}px`);
  document.documentElement.style.setProperty('--font-size-large', `${size * 1.25}px`);
  document.documentElement.style.setProperty('--font-size-xlarge', `${size * 1.5}px`);
  document.documentElement.style.setProperty('--font-size-title', `${size * 1.75}px`);
  
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
 * 应用聊天背景
 * @param bg 背景值，可以是颜色代码或图片URL
 */
export const applyBackground = (bg: string): void => {
  const chatPanels = document.querySelectorAll('.message-container');
  chatPanels.forEach((panel) => {
    const element = panel as HTMLElement;
    if (bg === 'default') {
      element.style.background = '';
    } else if (bg.startsWith('#')) {
      element.style.background = bg;
      element.style.backgroundImage = 'none';
    } else {
      element.style.backgroundImage = `url(${bg})`;
      element.style.backgroundSize = 'cover';
      element.style.backgroundPosition = 'center';
    }
  });
  
  console.log(`应用了聊天背景: ${bg}`);
}; 