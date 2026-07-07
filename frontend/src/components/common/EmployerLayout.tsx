import type { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Building2, LayoutDashboard, LogOut, Plus } from 'lucide-react';
import { useLogout } from '../../hooks/useAuth';
import { useAuthStore } from '../../store/authStore';

interface EmployerLayoutProps {
  children: ReactNode;
}

/** Focused shell for the employer portal — separate from the candidate app chrome. */
export default function EmployerLayout({ children }: EmployerLayoutProps) {
  const location = useLocation();
  const logout = useLogout();
  const user = useAuthStore((s) => s.user);

  const navItem = (to: string, label: string, icon: ReactNode) => {
    const active = location.pathname === to;
    return (
      <Link
        to={to}
        className={`flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition ${
          active
            ? 'bg-primary-600 text-white'
            : 'text-gray-600 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-800'
        }`}
      >
        {icon} {label}
      </Link>
    );
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <header className="border-b border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-900">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-3 sm:px-6">
          <Link to="/employer/dashboard" className="flex items-center gap-2">
            <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-accent-600 to-primary-500 text-white">
              <Building2 size={18} />
            </span>
            <span className="text-lg font-bold tracking-tight text-gray-900 dark:text-white">
              Career<span className="text-primary-600 dark:text-primary-400">AI</span>
              <span className="ml-1 hidden text-xs font-medium text-gray-400 sm:inline">Employers</span>
            </span>
          </Link>

          <nav className="flex items-center gap-1.5">
            {navItem('/employer/dashboard', 'Dashboard', <LayoutDashboard size={16} />)}
            {navItem('/employer/jobs/new', 'Post a Job', <Plus size={16} />)}
            <button
              onClick={() => logout.mutate()}
              className="ml-1 flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-gray-500 transition hover:bg-gray-100 hover:text-error-600 dark:text-gray-400 dark:hover:bg-gray-800"
            >
              <LogOut size={16} /> <span className="hidden sm:inline">Sign out</span>
            </button>
          </nav>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        {user && (
          <p className="mb-2 text-sm text-gray-500 dark:text-gray-400">
            Signed in as {user.firstName} {user.lastName}
          </p>
        )}
        {children}
      </main>
    </div>
  );
}
