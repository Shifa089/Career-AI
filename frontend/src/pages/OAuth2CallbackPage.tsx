import { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../api/auth';
import { getClaims } from '../utils/tokenUtils';
import type { RoleName, User } from '../types';
import LoadingSpinner from '../components/common/LoadingSpinner';

export default function OAuth2CallbackPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const handled = useRef(false);

  useEffect(() => {
    if (handled.current) return;
    handled.current = true;

    const accessToken = params.get('accessToken');
    const refreshToken = params.get('refreshToken');
    const error = params.get('error');

    if (error || !accessToken || !refreshToken) {
      toast.error('Sign-in failed. Please try again.');
      navigate('/login', { replace: true });
      return;
    }

    // Build a provisional user from the JWT claims, then refine via /users/me.
    const claims = getClaims(accessToken);
    const provisional: User = {
      id: claims?.userId ?? '',
      email: claims?.sub ?? '',
      fullName: claims?.sub?.split('@')[0] ?? 'User',
      roles: (claims?.roles as RoleName[]) ?? ['ROLE_USER'],
      provider: 'GOOGLE',
      emailVerified: true,
    };
    setAuth(provisional, accessToken, refreshToken);

    authApi
      .getMe()
      .then((user) => setAuth(user, accessToken, refreshToken))
      .catch(() => undefined)
      .finally(() => {
        toast.success('Signed in successfully');
        navigate('/dashboard', { replace: true });
      });
  }, [params, navigate, setAuth]);

  return <LoadingSpinner fullScreen label="Completing sign-in…" />;
}
