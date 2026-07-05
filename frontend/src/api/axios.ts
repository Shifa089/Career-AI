import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios';
import { useAuthStore } from '../store/authStore';
import type { ApiResponse, AuthResponse, ErrorResponse } from '../types';

const API_PREFIX = '/api/v1';

/**
 * Primary client. A response interceptor unwraps the `ApiResponse<T>` envelope so
 * callers receive `T` directly. Use `rawApi` when you need the untouched response
 * (e.g. the 302 download redirect).
 */
export const api: AxiosInstance = axios.create({
  baseURL: API_PREFIX,
  headers: { 'Content-Type': 'application/json' },
});

/** Client that does NOT unwrap — for redirects / non-enveloped responses. */
export const rawApi: AxiosInstance = axios.create({ baseURL: API_PREFIX });

// --- Request interceptor: attach the access token -------------------------
function attachToken(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
}
api.interceptors.request.use(attachToken);
rawApi.interceptors.request.use(attachToken);

// --- Response interceptor: unwrap ApiResponse + refresh on 401 ------------
interface RetriableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

// Single-flight refresh: concurrent 401s await one refresh call.
let refreshPromise: Promise<string> | null = null;

async function performRefresh(): Promise<string> {
  const { refreshToken, setAuth, clearAuth } = useAuthStore.getState();
  if (!refreshToken) {
    clearAuth();
    throw new Error('No refresh token');
  }
  try {
    // Bare axios call to avoid recursing through this interceptor.
    const res = await axios.post<ApiResponse<AuthResponse>>(
      `${API_PREFIX}/auth/refresh`,
      { refreshToken },
      { headers: { 'Content-Type': 'application/json' } },
    );
    const auth = res.data.data;
    setAuth(auth.user, auth.accessToken, auth.refreshToken);
    return auth.accessToken;
  } catch (err) {
    clearAuth();
    throw err;
  }
}

api.interceptors.response.use(
  // Success: unwrap the envelope so callers resolve to `T` directly.
  // 204 / non-envelope bodies resolve to the raw body (usually undefined).
  (response: AxiosResponse<ApiResponse<unknown>>): any => {
    const body = response.data;
    if (body && typeof body === 'object' && 'success' in body && 'data' in body) {
      return body.data;
    }
    return body;
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const original = error.config as RetriableConfig | undefined;
    const status = error.response?.status;
    const isAuthEndpoint = original?.url?.includes('/auth/');

    if (status === 401 && original && !original._retry && !isAuthEndpoint) {
      original._retry = true;
      try {
        refreshPromise = refreshPromise ?? performRefresh();
        const newToken = await refreshPromise;
        refreshPromise = null;
        original.headers.set('Authorization', `Bearer ${newToken}`);
        return api(original);
      } catch (refreshErr) {
        refreshPromise = null;
        redirectToLogin();
        return Promise.reject(refreshErr);
      }
    }

    return Promise.reject(normalizeError(error));
  },
);

function redirectToLogin(): void {
  if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
    window.location.assign('/login');
  }
}

/** Extract a human-friendly message + code from an axios error. */
export interface NormalizedError extends Error {
  status?: number;
  code?: string;
  fieldErrors?: ErrorResponse['fieldErrors'];
}

export function normalizeError(error: unknown): NormalizedError {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as ApiResponse<unknown> | undefined;
    const errResp = body?.error;
    const message =
      errResp?.message ||
      body?.message ||
      error.message ||
      'Something went wrong. Please try again.';
    const normalized: NormalizedError = new Error(message);
    normalized.status = error.response?.status;
    normalized.code = errResp?.code;
    normalized.fieldErrors = errResp?.fieldErrors;
    return normalized;
  }
  if (error instanceof Error) return error;
  return new Error('Unexpected error');
}

export function getErrorMessage(error: unknown): string {
  return normalizeError(error).message;
}
