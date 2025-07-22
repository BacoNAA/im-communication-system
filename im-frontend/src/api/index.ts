// 导出所有API模块
export { api } from './request';
export { messageApi } from './message';
export { authApi } from './auth';
export { userApi } from './user';
export { contactApi } from './contact';
export * as groupApi from './group';
export { reportApi } from './report';
export { notificationApi } from './notification';
export type { ApiResponse } from './request';

// 认证相关类型
export type { LoginRequest, RegisterRequest, AuthResponse } from './auth';