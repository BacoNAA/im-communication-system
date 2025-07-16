// API统一入口
export { authApi } from './auth';
export { userApi } from './user';
export { messageApi } from './message';
export { contactApi } from './contact';
export { tagApi } from './tag';
export { api } from './request';
export type { ApiResponse } from './request';

// 认证相关类型
export type { LoginRequest, RegisterRequest, AuthResponse } from './auth';