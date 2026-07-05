import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { jobMatchApi } from '../api/jobMatch';
import { getErrorMessage } from '../api/axios';
import type { FindMatchesRequest, MatchStatus } from '../types';

const KEYS = {
  all: ['jobMatches'] as const,
  list: (status: MatchStatus | undefined, page: number) =>
    ['jobMatches', 'list', status ?? 'ALL', page] as const,
  detail: (id: string) => ['jobMatches', id] as const,
  skillGap: (id: string) => ['jobMatches', id, 'skill-gap'] as const,
  learningPath: (id: string) => ['jobMatches', id, 'learning-path'] as const,
  jobs: (keyword: string, location: string, page: number) =>
    ['jobs', keyword, location, page] as const,
};

export function useMatches(status?: MatchStatus, page = 0, size = 20) {
  return useQuery({
    queryKey: KEYS.list(status, page),
    queryFn: () => jobMatchApi.getMatches({ status, page, size }),
  });
}

export function useMatch(matchId: string | undefined) {
  return useQuery({
    queryKey: KEYS.detail(matchId ?? ''),
    queryFn: () => jobMatchApi.getMatch(matchId as string),
    enabled: Boolean(matchId),
  });
}

export function useSkillGap(matchId: string | undefined, enabled = true) {
  return useQuery({
    queryKey: KEYS.skillGap(matchId ?? ''),
    queryFn: () => jobMatchApi.getSkillGap(matchId as string),
    enabled: Boolean(matchId) && enabled,
  });
}

export function useLearningPath(matchId: string | undefined, enabled = true) {
  return useQuery({
    queryKey: KEYS.learningPath(matchId ?? ''),
    queryFn: () => jobMatchApi.getLearningPath(matchId as string),
    enabled: Boolean(matchId) && enabled,
  });
}

export function useSearchJobs(keyword = '', location = '', page = 0, size = 20) {
  return useQuery({
    queryKey: KEYS.jobs(keyword, location, page),
    queryFn: () => jobMatchApi.searchJobs({ keyword, location, page, size }),
  });
}

export function useFindMatches() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: FindMatchesRequest) => jobMatchApi.findMatches(payload),
    onSuccess: (matches) => {
      queryClient.invalidateQueries({ queryKey: KEYS.all });
      toast.success(`Found ${matches.length} matching ${matches.length === 1 ? 'job' : 'jobs'}`);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}

export function useUpdateMatchStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ matchId, status }: { matchId: string; status: MatchStatus }) =>
      jobMatchApi.updateStatus(matchId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: KEYS.all });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}
