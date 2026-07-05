import { Menu, Transition } from '@headlessui/react';
import { Fragment } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { LogOut, Moon, Sparkles, Sun, User as UserIcon } from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import { useLogout } from '../../hooks/useAuth';
import { useTheme } from '../../hooks/useTheme';
import { fullNameOf, initialsFromName } from '../../utils/formatters';

interface NavbarProps {
  variant?: 'app' | 'public';
}

export default function Navbar({ variant = 'app' }: NavbarProps) {
  const { theme, toggleTheme } = useTheme();
  const user = useAuthStore((s) => s.user);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const logout = useLogout();
  const navigate = useNavigate();

  return (
    <header className="sticky top-0 z-30 border-b border-gray-100 bg-white/80 backdrop-blur-md dark:border-gray-800 dark:bg-gray-950/80">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <Link to={isAuthenticated ? '/dashboard' : '/'} className="flex items-center gap-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-primary-600 to-accent-500 text-white">
            <Sparkles size={18} />
          </span>
          <span className="text-lg font-bold tracking-tight text-gray-900 dark:text-white">
            Career<span className="text-primary-600 dark:text-primary-400">AI</span>
          </span>
        </Link>

        <div className="flex items-center gap-2">
          <button
            onClick={toggleTheme}
            aria-label="Toggle theme"
            className="rounded-lg p-2 text-gray-500 transition hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800"
          >
            {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
          </button>

          {variant === 'public' || !isAuthenticated ? (
            <>
              <Link to="/login" className="btn-ghost">
                Login
              </Link>
              <Link to="/register" className="btn-primary">
                Get Started
              </Link>
            </>
          ) : (
            <Menu as="div" className="relative">
              <Menu.Button className="flex items-center gap-2 rounded-full p-1 pr-2 transition hover:bg-gray-100 dark:hover:bg-gray-800">
                <span className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-600 text-xs font-semibold text-white">
                  {initialsFromName(fullNameOf(user))}
                </span>
                <span className="hidden text-sm font-medium text-gray-700 dark:text-gray-200 sm:block">
                  {fullNameOf(user) || 'Account'}
                </span>
              </Menu.Button>
              <Transition
                as={Fragment}
                enter="transition ease-out duration-100"
                enterFrom="opacity-0 scale-95"
                enterTo="opacity-100 scale-100"
                leave="transition ease-in duration-75"
                leaveFrom="opacity-100 scale-100"
                leaveTo="opacity-0 scale-95"
              >
                <Menu.Items className="absolute right-0 mt-2 w-52 origin-top-right overflow-hidden rounded-xl border border-gray-100 bg-white py-1 shadow-lg focus:outline-none dark:border-gray-800 dark:bg-gray-900">
                  <div className="border-b border-gray-100 px-4 py-3 dark:border-gray-800">
                    <p className="truncate text-sm font-semibold text-gray-900 dark:text-gray-100">
                      {fullNameOf(user)}
                    </p>
                    <p className="truncate text-xs text-gray-500 dark:text-gray-400">
                      {user?.email}
                    </p>
                  </div>
                  <Menu.Item>
                    {({ active }) => (
                      <button
                        onClick={() => navigate('/profile')}
                        className={`flex w-full items-center gap-2 px-4 py-2.5 text-sm text-gray-700 dark:text-gray-200 ${
                          active ? 'bg-gray-50 dark:bg-gray-800' : ''
                        }`}
                      >
                        <UserIcon size={16} /> Profile
                      </button>
                    )}
                  </Menu.Item>
                  <Menu.Item>
                    {({ active }) => (
                      <button
                        onClick={() => logout.mutate()}
                        className={`flex w-full items-center gap-2 px-4 py-2.5 text-sm text-error-600 ${
                          active ? 'bg-error-500/5' : ''
                        }`}
                      >
                        <LogOut size={16} /> Sign out
                      </button>
                    )}
                  </Menu.Item>
                </Menu.Items>
              </Transition>
            </Menu>
          )}
        </div>
      </div>
    </header>
  );
}
