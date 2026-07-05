import { api } from './axios';
import type {
  FindMatchesRequest,
  JobListing,
  JobMatch,
  LearningPath,
  MatchStatus,
  Page,
  SkillGap,
} from '../types';

export const jobMatchApi = {
  findMatches: (payload: FindMatchesRequest) =>
    api.post<unknown, JobMatch[]>('/job-matches/find', payload),

  getMatches: (params?: { status?: MatchStatus; page?: number; size?: number }) =>
    api.get<unknown, Page<JobMatch>>('/job-matches', {
      params: { page: 0, size: 20, ...params },
    }),

  getMatch: (matchId: string) => api.get<unknown, JobMatch>(`/job-matches/${matchId}`),

  getSkillGap: (matchId: string) =>
    api.get<unknown, SkillGap>(`/job-matches/${matchId}/skill-gap`),

  getLearningPath: (matchId: string) =>
    api.get<unknown, LearningPath>(`/job-matches/${matchId}/learning-path`),

  updateStatus: (matchId: string, status: MatchStatus) =>
    api.patch<unknown, JobMatch>(`/job-matches/${matchId}/status`, null, { params: { status } }),

  searchJobs: (params?: { keyword?: string; location?: string; page?: number; size?: number }) =>
    api.get<unknown, Page<JobListing>>('/job-matches/jobs', {
      params: { page: 0, size: 20, ...params },
    }),
};
