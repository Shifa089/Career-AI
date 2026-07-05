import { useEffect, useState } from 'react';
import { LogOut, Mail, ShieldCheck, UserCircle } from 'lucide-react';
import AppLayout from '../components/common/AppLayout';
import PageHeader from '../components/common/PageHeader';
import { useAuthStore } from '../store/authStore';
import { useCurrentUser, useLogout, useUpdateProfile } from '../hooks/useAuth';
import { formatEnum, fullNameOf, initialsFromName } from '../utils/formatters';

export default function ProfilePage() {
  useCurrentUser();
  const user = useAuthStore((s) => s.user);
  const updateProfile = useUpdateProfile();
  const logout = useLogout();
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');

  useEffect(() => {
    if (user?.firstName) setFirstName(user.firstName);
    if (user?.lastName) setLastName(user.lastName);
  }, [user?.firstName, user?.lastName]);

  const dirty =
    firstName.trim().length > 0 &&
    (firstName.trim() !== (user?.firstName ?? '') || lastName.trim() !== (user?.lastName ?? ''));

  return (
    <AppLayout>
      <PageHeader
        title="Profile"
        subtitle="Manage your account details."
        icon={<UserCircle size={22} />}
      />

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Identity card */}
        <div className="card flex flex-col items-center p-6 text-center">
          <span className="flex h-20 w-20 items-center justify-center rounded-full bg-gradient-to-br from-primary-600 to-accent-500 text-2xl font-bold text-white">
            {initialsFromName(fullNameOf(user))}
          </span>
          <h2 className="mt-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
            {fullNameOf(user)}
          </h2>
          <p className="text-sm text-gray-500 dark:text-gray-400">{user?.email}</p>
          <div className="mt-3 flex flex-wrap justify-center gap-2">
            {(user?.roles ?? []).map((r) => (
              <span key={r} className="chip bg-primary-500/10 text-primary-600 dark:text-primary-400">
                {formatEnum(r.replace('ROLE_', ''))}
              </span>
            ))}
            {user?.emailVerified && (
              <span className="chip bg-success-500/10 text-success-600 dark:text-success-500">
                <ShieldCheck size={12} className="mr-1" /> Verified
              </span>
            )}
          </div>
        </div>

        {/* Edit form */}
        <div className="card p-6 lg:col-span-2">
          <h3 className="mb-5 font-semibold text-gray-900 dark:text-gray-100">Account details</h3>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (dirty) {
                updateProfile.mutate({
                  firstName: firstName.trim(),
                  lastName: lastName.trim(),
                  profilePictureUrl: user?.profilePictureUrl ?? null,
                });
              }
            }}
            className="space-y-4"
          >
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="firstName" className="label">
                  First name
                </label>
                <input
                  id="firstName"
                  className="input"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                />
              </div>
              <div>
                <label htmlFor="lastName" className="label">
                  Last name
                </label>
                <input
                  id="lastName"
                  className="input"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                />
              </div>
            </div>

            <div>
              <label className="label">Email</label>
              <div className="relative">
                <Mail size={17} className="pointer-events-none absolute left-3 top-3 text-gray-400" />
                <input className="input pl-10" value={user?.email ?? ''} disabled />
              </div>
              <p className="mt-1 text-xs text-gray-400">Email cannot be changed.</p>
            </div>

            <div>
              <label className="label">Sign-in method</label>
              <input className="input" value={formatEnum(user?.provider)} disabled />
            </div>

            <div className="flex items-center justify-between border-t border-gray-100 pt-5 dark:border-gray-800">
              <button
                type="button"
                className="btn-secondary text-error-600"
                onClick={() => logout.mutate()}
              >
                <LogOut size={16} /> Sign out
              </button>
              <button type="submit" className="btn-primary" disabled={!dirty || updateProfile.isPending}>
                {updateProfile.isPending ? 'Saving…' : 'Save changes'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </AppLayout>
  );
}
