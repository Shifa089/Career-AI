import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Eye, EyeOff, Lock, Mail, Sparkles } from 'lucide-react';
import { useLogin } from '../hooks/useAuth';
import OAuthButtons from '../components/common/OAuthButtons';

interface LocationState {
  from?: string;
}

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const login = useLogin();
  const location = useLocation();
  const from = (location.state as LocationState | null)?.from;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) return;
    login.mutate({ email, password });
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4 py-12 dark:bg-gray-950">
      <div className="w-full max-w-md">
        <Link to="/" className="mb-8 flex items-center justify-center gap-2">
          <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-gradient-to-br from-primary-600 to-accent-500 text-white">
            <Sparkles size={20} />
          </span>
          <span className="text-xl font-bold tracking-tight text-gray-900 dark:text-white">
            Career<span className="text-primary-600 dark:text-primary-400">AI</span>
          </span>
        </Link>

        <div className="card p-8">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Welcome back</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Sign in to continue your job search.
          </p>
          {from && (
            <p className="mt-3 rounded-lg bg-primary-50 px-3 py-2 text-xs text-primary-700 dark:bg-primary-500/10 dark:text-primary-300">
              Please sign in to access that page.
            </p>
          )}

          <form onSubmit={handleSubmit} className="mt-6 space-y-4">
            <div>
              <label htmlFor="email" className="label">
                Email
              </label>
              <div className="relative">
                <Mail size={17} className="pointer-events-none absolute left-3 top-3 text-gray-400" />
                <input
                  id="email"
                  type="email"
                  autoComplete="email"
                  className="input pl-10"
                  placeholder="you@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>
            </div>

            <div>
              <label htmlFor="password" className="label">
                Password
              </label>
              <div className="relative">
                <Lock size={17} className="pointer-events-none absolute left-3 top-3 text-gray-400" />
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  className="input px-10"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((s) => !s)}
                  className="absolute right-3 top-3 text-gray-400 hover:text-gray-600"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
            </div>

            <button type="submit" className="btn-primary w-full" disabled={login.isPending}>
              {login.isPending ? 'Signing in…' : 'Sign In'}
            </button>
          </form>

          <div className="my-6 flex items-center gap-3">
            <div className="h-px flex-1 bg-gray-200 dark:bg-gray-800" />
            <span className="text-xs text-gray-400">or continue with</span>
            <div className="h-px flex-1 bg-gray-200 dark:bg-gray-800" />
          </div>

          <OAuthButtons />

          <p className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
            Don’t have an account?{' '}
            <Link to="/register" className="font-semibold text-primary-600 hover:underline dark:text-primary-400">
              Sign up
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
