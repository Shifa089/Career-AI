import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { companyApi } from '../api/company';
import { getErrorMessage } from '../api/axios';
import { useAuthStore } from '../store/authStore';
import type { CompanyRegisterRequest, CreateJobRequest, LoginRequest } from '../types';

export function useCompanyLogin() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  return useMutation({
    mutationFn: (payload: LoginRequest) => companyApi.login(payload),
    onSuccess: (auth) => {
      setAuth(auth.user, auth.accessToken, auth.refreshToken);
      toast.success(`Welcome back, ${auth.user.firstName}!`);
      navigate('/employer/dashboard', { replace: true });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function useCompanyRegister() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  return useMutation({
    mutationFn: (payload: CompanyRegisterRequest) => companyApi.register(payload),
    onSuccess: (auth) => {
      setAuth(auth.user, auth.accessToken, auth.refreshToken);
      toast.success('Company account created!');
      navigate('/employer/dashboard', { replace: true });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function useMyJobs() {
  return useQuery({
    queryKey: ['employerJobs'],
    queryFn: () => companyApi.getMyJobs(),
    staleTime: 60 * 1000,
  });
}

export function usePostJob() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateJobRequest) => companyApi.postJob(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employerJobs'] });
      toast.success('Job posted — candidates can now be matched to it.');
      navigate('/employer/dashboard', { replace: true });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function useSetJobActive() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ jobId, active }: { jobId: string; active: boolean }) =>
      companyApi.setJobActive(jobId, active),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['employerJobs'] });
      toast.success(variables.active ? 'Job re-opened' : 'Job closed');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}
