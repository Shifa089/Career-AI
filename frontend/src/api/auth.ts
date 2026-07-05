import { api } from './axios';
import type {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
  UpdateProfileRequest,
  User,
} from '../types';

export const authApi = {
  register: (payload: RegisterRequest) =>
    api.post<unknown, AuthResponse>('/auth/register', payload),

  login: (payload: LoginRequest) => api.post<unknown, AuthResponse>('/auth/login', payload),

  refreshToken: (refreshToken: string) =>
    api.post<unknown, AuthResponse>('/auth/refresh', { refreshToken }),

  logout: (refreshToken: string) =>
    api.post<unknown, void>('/auth/logout', { refreshToken }).catch(() => undefined),

  forgotPassword: (payload: ForgotPasswordRequest) =>
    api.post<unknown, void>('/auth/forgot-password', payload),

  resetPassword: (payload: ResetPasswordRequest) =>
    api.post<unknown, void>('/auth/reset-password', payload),

  getMe: () => api.get<unknown, User>('/users/me'),

  updateProfile: (payload: UpdateProfileRequest) => api.put<unknown, User>('/users/me', payload),
};

/** OAuth2 authorization-code entry points (full-page redirect through the gateway). */
export const oauthUrls = {
  google: '/oauth2/authorization/google',
  github: '/oauth2/authorization/github',
};
