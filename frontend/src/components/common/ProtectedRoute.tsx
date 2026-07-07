import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { isExpired } from '../../utils/tokenUtils';
import type { RoleName } from '../../types';
import LoadingSpinner from './LoadingSpinner';

interface ProtectedRouteProps {
  children: ReactNode;
  /** When set, the user must hold this role or they are redirected away. */
  requiredRole?: RoleName;
}

export default function ProtectedRoute({ children, requiredRole }: ProtectedRouteProps) {
  const location = useLocation();
  const { isAuthenticated, isLoading, accessToken, refreshToken, user } = useAuthStore();

  if (isLoading) {
    return <LoadingSpinner fullScreen label="Checking your session…" />;
  }

  // Authenticated if we hold a non-expired access token, or a refresh token to renew it.
  const hasUsableSession = isAuthenticated && (!isExpired(accessToken) || Boolean(refreshToken));

  if (!hasUsableSession) {
    // Company routes send unauthenticated users to the employer login.
    const loginPath = requiredRole === 'ROLE_COMPANY' ? '/company/login' : '/login';
    return <Navigate to={loginPath} replace state={{ from: location.pathname }} />;
  }

  if (requiredRole && !(user?.roles ?? []).includes(requiredRole)) {
    // Authenticated but wrong audience — send them to their own home.
    const fallback = (user?.roles ?? []).includes('ROLE_COMPANY') ? '/employer/dashboard' : '/dashboard';
    return <Navigate to={fallback} replace />;
  }

  return <>{children}</>;
}
