import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { isExpired } from '../../utils/tokenUtils';
import LoadingSpinner from './LoadingSpinner';

interface ProtectedRouteProps {
  children: ReactNode;
}

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const location = useLocation();
  const { isAuthenticated, isLoading, accessToken, refreshToken } = useAuthStore();

  if (isLoading) {
    return <LoadingSpinner fullScreen label="Checking your session…" />;
  }

  // Authenticated if we hold a non-expired access token, or a refresh token to renew it.
  const hasUsableSession = isAuthenticated && (!isExpired(accessToken) || Boolean(refreshToken));

  if (!hasUsableSession) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <>{children}</>;
}
