import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { authApi } from '../api/auth';
import { getErrorMessage } from '../api/axios';
import { useAuthStore } from '../store/authStore';
import type {
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
  UpdateProfileRequest,
} from '../types';

export function useLogin() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  return useMutation({
    mutationFn: (payload: LoginRequest) => authApi.login(payload),
    onSuccess: (auth) => {
      setAuth(auth.user, auth.accessToken, auth.refreshToken);
      toast.success(`Welcome back, ${auth.user.fullName.split(' ')[0]}!`);
      navigate('/dashboard', { replace: true });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function useRegister() {
  const navigate = useNavigate();

  return useMutation({
    mutationFn: (payload: RegisterRequest) => authApi.register(payload),
    onSuccess: () => {
      toast.success('Account created! Please check your email to verify, then log in.');
      navigate('/login', { replace: true });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function useLogout() {
  const navigate = useNavigate();
  const clearAuth = useAuthStore((s) => s.clearAuth);
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => authApi.logout(),
    onSettled: () => {
      clearAuth();
      queryClient.clear();
      toast.success('Signed out');
      navigate('/', { replace: true });
    },
  });
}

export function useCurrentUser() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const setUser = useAuthStore((s) => s.setUser);

  return useQuery({
    queryKey: ['currentUser'],
    queryFn: async () => {
      const user = await authApi.getMe();
      setUser(user);
      return user;
    },
    enabled: isAuthenticated,
    staleTime: 5 * 60 * 1000,
  });
}

export function useUpdateProfile() {
  const setUser = useAuthStore((s) => s.setUser);
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UpdateProfileRequest) => authApi.updateProfile(payload),
    onSuccess: (user) => {
      setUser(user);
      queryClient.setQueryData(['currentUser'], user);
      toast.success('Profile updated');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function useForgotPassword() {
  return useMutation({
    mutationFn: (email: string) => authApi.forgotPassword({ email }),
    onSuccess: () => toast.success('If that email exists, a reset link is on its way.'),
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function useResetPassword() {
  const navigate = useNavigate();
  return useMutation({
    mutationFn: (payload: ResetPasswordRequest) => authApi.resetPassword(payload),
    onSuccess: () => {
      toast.success('Password reset. Please log in.');
      navigate('/login');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}
