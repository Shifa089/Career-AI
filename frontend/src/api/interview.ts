import { api } from './axios';
import type {
  CreateSessionRequest,
  InterviewFeedback,
  InterviewSession,
  InterviewStats,
  Page,
} from '../types';

export const interviewApi = {
  createSession: (payload: CreateSessionRequest) =>
    api.post<unknown, InterviewSession>('/interviews', payload),

  getSessions: (page = 0, size = 20) =>
    api.get<unknown, Page<InterviewSession>>('/interviews', { params: { page, size } }),

  getSession: (sessionId: string) =>
    api.get<unknown, InterviewSession>(`/interviews/${sessionId}`),

  getFeedback: (sessionId: string) =>
    api.get<unknown, InterviewFeedback>(`/interviews/${sessionId}/feedback`),

  abandonSession: (sessionId: string) => api.delete<unknown, void>(`/interviews/${sessionId}`),

  getStats: () => api.get<unknown, InterviewStats>('/interviews/stats'),
};
