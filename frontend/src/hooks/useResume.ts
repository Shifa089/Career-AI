import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { resumeApi } from '../api/resume';
import { getErrorMessage } from '../api/axios';
import type { Resume } from '../types';

const RESUME_KEYS = {
  all: ['resumes'] as const,
  detail: (id: string) => ['resumes', id] as const,
  analysis: (id: string) => ['resumes', id, 'analysis'] as const,
};

const isProcessing = (status?: Resume['status']) =>
  status === 'UPLOADED' || status === 'PROCESSING';

export function useResumes() {
  return useQuery({
    queryKey: RESUME_KEYS.all,
    queryFn: () => resumeApi.list(),
    // Keep the list fresh while any resume is still being analysed so status badges
    // (and the open analysis view) transition out of PROCESSING on their own.
    refetchInterval: (query) =>
      (query.state.data ?? []).some((r) => isProcessing(r.status)) ? 5000 : false,
  });
}

export function useResume(resumeId: string | undefined) {
  return useQuery({
    queryKey: RESUME_KEYS.detail(resumeId ?? ''),
    queryFn: () => resumeApi.get(resumeId as string),
    enabled: Boolean(resumeId),
    // Poll the single resume while it's processing so callers observe the ANALYSED/FAILED
    // transition instead of holding a stale PROCESSING snapshot forever.
    refetchInterval: (query) => (isProcessing(query.state.data?.status) ? 4000 : false),
  });
}

/** Polls the analysis while the resume is still being processed. */
export function useAnalysis(resumeId: string | undefined, status?: Resume['status']) {
  const stillProcessing = status === 'UPLOADED' || status === 'PROCESSING';
  return useQuery({
    queryKey: RESUME_KEYS.analysis(resumeId ?? ''),
    queryFn: () => resumeApi.getAnalysis(resumeId as string),
    enabled: Boolean(resumeId) && status !== 'FAILED',
    refetchInterval: stillProcessing ? 4000 : false,
    retry: (count, err) => {
      // 404 while analysis is pending is expected — keep polling a few times.
      const message = getErrorMessage(err);
      return stillProcessing && count < 30 && message.length > 0;
    },
  });
}

export function useUploadResume() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      file,
      targetRole,
      onProgress,
    }: {
      file: File;
      targetRole?: string;
      onProgress?: (percent: number) => void;
    }) => resumeApi.upload(file, targetRole, onProgress),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: RESUME_KEYS.all });
      toast.success('Resume uploaded — analysis in progress');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function useDeleteResume() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (resumeId: string) => resumeApi.remove(resumeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: RESUME_KEYS.all });
      toast.success('Resume deleted');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function useSetPrimary() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (resumeId: string) => resumeApi.setPrimary(resumeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: RESUME_KEYS.all });
      toast.success('Primary resume updated');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function useDownloadResume() {
  return useMutation({
    mutationFn: (resumeId: string) => resumeApi.getDownloadUrl(resumeId),
    onSuccess: (url) => {
      if (url) window.open(url, '_blank', 'noopener,noreferrer');
      else toast.error('Download link unavailable');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}
