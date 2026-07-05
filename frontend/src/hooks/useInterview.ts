import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { interviewApi } from '../api/interview';
import { getErrorMessage } from '../api/axios';
import type { CreateSessionRequest } from '../types';

const KEYS = {
  all: ['interviews'] as const,
  list: (page: number, size: number) => ['interviews', 'list', page, size] as const,
  detail: (id: string) => ['interviews', id] as const,
  feedback: (id: string) => ['interviews', id, 'feedback'] as const,
  stats: ['interviews', 'stats'] as const,
};

export function useSessions(page = 0, size = 20) {
  return useQuery({
    queryKey: KEYS.list(page, size),
    queryFn: () => interviewApi.getSessions(page, size),
  });
}

export function useSession(sessionId: string | undefined) {
  return useQuery({
    queryKey: KEYS.detail(sessionId ?? ''),
    queryFn: () => interviewApi.getSession(sessionId as string),
    enabled: Boolean(sessionId),
  });
}

export function useFeedback(sessionId: string | undefined, enabled = true) {
  return useQuery({
    queryKey: KEYS.feedback(sessionId ?? ''),
    queryFn: () => interviewApi.getFeedback(sessionId as string),
    enabled: Boolean(sessionId) && enabled,
  });
}

export function useInterviewStats() {
  return useQuery({
    queryKey: KEYS.stats,
    queryFn: () => interviewApi.getStats(),
  });
}

export function useCreateSession() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateSessionRequest) => interviewApi.createSession(payload),
    onSuccess: (session) => {
      queryClient.invalidateQueries({ queryKey: KEYS.all });
      navigate(`/interviews/${session.id}`);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function useAbandonSession() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (sessionId: string) => interviewApi.abandonSession(sessionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: KEYS.all });
      toast('Interview ended', { icon: '👋' });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}
