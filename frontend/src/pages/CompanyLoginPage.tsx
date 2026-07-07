import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Building2, Eye, EyeOff, Lock, Mail } from 'lucide-react';
import { useCompanyLogin } from '../hooks/useCompany';

export default function CompanyLoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const login = useCompanyLogin();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    login.mutate({ email, password });
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4 py-12 dark:bg-gray-950">
      <div className="w-full max-w-md">
        <Link to="/" className="mb-8 flex items-center justify-center gap-2">
          <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-gradient-to-br from-accent-600 to-primary-500 text-white">
            <Building2 size={20} />
          </span>
          <span className="text-xl font-bold tracking-tight text-gray-900 dark:text-white">
            Career<span className="text-primary-600 dark:text-primary-400">AI</span>
            <span className="ml-1 text-sm font-medium text-gray-400">for Employers</span>
          </span>
        </Link>

        <div className="card p-8">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Employer sign in</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Post jobs and reach matched candidates.
          </p>

          <form onSubmit={handleSubmit} className="mt-6 space-y-4">
            <div>
              <label htmlFor="email" className="label">
                Work email
              </label>
              <div className="relative">
                <Mail size={17} className="pointer-events-none absolute left-3 top-3 text-gray-400" />
                <input
                  id="email"
                  type="email"
                  autoComplete="email"
                  className="input pl-10"
                  placeholder="hr@company.com"
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
                  placeholder="Your password"
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

          <p className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
            New employer?{' '}
            <Link
              to="/company/register"
              className="font-semibold text-primary-600 hover:underline dark:text-primary-400"
            >
              Create a company account
            </Link>
          </p>
          <p className="mt-2 text-center text-sm text-gray-500 dark:text-gray-400">
            Looking for a job?{' '}
            <Link to="/login" className="font-semibold text-primary-600 hover:underline dark:text-primary-400">
              Candidate sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
