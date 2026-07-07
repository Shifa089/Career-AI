import { api } from './axios';
import type {
  AuthResponse,
  CompanyRegisterRequest,
  CreateJobRequest,
  JobListing,
  LoginRequest,
  Page,
} from '../types';

/**
 * Employer / company endpoints: a registration + login flow separate from candidates, plus
 * job posting and management. All routed through the gateway under /api/v1.
 */
export const companyApi = {
  register: (payload: CompanyRegisterRequest) =>
    api.post<unknown, AuthResponse>('/auth/company/register', payload),

  login: (payload: LoginRequest) =>
    api.post<unknown, AuthResponse>('/auth/company/login', payload),

  postJob: (payload: CreateJobRequest) =>
    api.post<unknown, JobListing>('/job-matches/jobs', payload),

  getMyJobs: (page = 0, size = 50) =>
    api.get<unknown, Page<JobListing>>('/job-matches/jobs/mine', { params: { page, size } }),

  setJobActive: (jobId: string, active: boolean) =>
    api.patch<unknown, JobListing>(`/job-matches/jobs/${jobId}/active`, null, {
      params: { active },
    }),
};
