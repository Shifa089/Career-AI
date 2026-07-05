import type { AxiosProgressEvent } from 'axios';
import { api, rawApi } from './axios';
import type { Resume, ResumeAnalysis } from '../types';

export const resumeApi = {
  upload: (
    file: File,
    targetRole?: string,
    onProgress?: (percent: number) => void,
  ): Promise<Resume> => {
    const form = new FormData();
    form.append('file', file);
    if (targetRole) form.append('targetRole', targetRole);

    return api.post<unknown, Resume>('/resumes', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e: AxiosProgressEvent) => {
        if (onProgress && e.total) {
          onProgress(Math.round((e.loaded / e.total) * 100));
        }
      },
    });
  },

  list: () => api.get<unknown, Resume[]>('/resumes'),

  get: (resumeId: string) => api.get<unknown, Resume>(`/resumes/${resumeId}`),

  getAnalysis: (resumeId: string) =>
    api.get<unknown, ResumeAnalysis>(`/resumes/${resumeId}/analysis`),

  remove: (resumeId: string) => api.delete<unknown, void>(`/resumes/${resumeId}`),

  setPrimary: (resumeId: string) => api.patch<unknown, Resume>(`/resumes/${resumeId}/primary`),

  /**
   * The download endpoint replies 302 with a presigned Location. We hit it with the
   * raw client (no unwrap) and don't auto-follow — returning the redirect URL so the
   * caller can open it in a new tab.
   */
  getDownloadUrl: async (resumeId: string): Promise<string> => {
    const res = await rawApi.get(`/resumes/${resumeId}/download`, {
      maxRedirects: 0,
      validateStatus: (s) => s === 302 || s === 200,
    });
    const location = (res.headers?.location as string | undefined) ?? res.request?.responseURL;
    return location ?? '';
  },
};
