# IM 通信系统前端

基于 Vue 3 + TypeScript + Vite 的即时通讯系统前端项目。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **TypeScript** - JavaScript 的超集，提供类型安全
- **Vite** - 快速的前端构建工具
- **Vue Router** - Vue.js 官方路由管理器
- **Pinia** - Vue 的状态管理库
- **Element Plus** - 基于 Vue 3 的组件库
- **Axios** - HTTP 客户端

## 项目结构

```
src/
├── api/           # API 接口定义
├── assets/        # 静态资源
├── components/    # 公共组件
│   └── common/    # 通用组件
├── composables/   # 组合式函数
├── router/        # 路由配置
├── stores/        # 状态管理
├── types/         # TypeScript 类型定义
├── utils/         # 工具函数
├── views/         # 页面组件
├── App.vue        # 根组件
└── main.ts        # 入口文件
```

## 开发指南

### 环境要求

- Node.js >= 16
- npm >= 8

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

### 构建生产版本

```bash
npm run build
```

### 类型检查

```bash
npm run type-check
```

### 代码检查

```bash
npm run lint
```

### 运行测试

```bash
npm run test:unit
npm run test:e2e
```

## 开发规范

### 代码风格

- 使用 ESLint + Prettier 进行代码格式化
- 遵循 Vue 3 Composition API 最佳实践
- 使用 TypeScript 严格模式

### 组件命名

- 组件文件使用 PascalCase 命名
- 组件名称使用多个单词，避免与 HTML 元素冲突

### 提交规范

使用 Conventional Commits 规范：

- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

## 许可证

MIT
