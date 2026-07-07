import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Building2, Eye, EyeOff, Lock, Mail, User } from 'lucide-react';
import toast from 'react-hot-toast';
import { useCompanyRegister } from '../hooks/useCompany';

type Strength = { score: number; label: string; color: string };

function assessPassword(pw: string): Strength {
  let score = 0;
  if (pw.length >= 8) score++;
  if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) score++;
  if (/\d/.test(pw)) score++;
  if (/[^A-Za-z0-9]/.test(pw)) score++;
  if (score <= 1) return { score: 1, label: 'Weak', color: 'bg-error-500' };
  if (score <= 3) return { score: 3, label: 'Medium', color: 'bg-warning-500' };
  return { score: 4, label: 'Strong', color: 'bg-success-500' };
}

export default function CompanyRegisterPage() {
  const [companyName, setCompanyName] = useState('');
  const [website, setWebsite] = useState('');
  const [industry, setIndustry] = useState('');
  const [companySize, setCompanySize] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const register = useCompanyRegister();

  const strength = useMemo(() => assessPassword(password), [password]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (strength.score < 3) {
      toast.error('Please choose a stronger password');
      return;
    }
    register.mutate({
      companyName: companyName.trim(),
      website: website.trim() || undefined,
      industry: industry.trim() || undefined,
      companySize: companySize.trim() || undefined,
      email,
      password,
      firstName: firstName.trim(),
      lastName: lastName.trim(),
    });
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4 py-12 dark:bg-gray-950">
      <div className="w-full max-w-lg">
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
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Create a company account</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Post roles and get matched with qualified candidates.
          </p>

          <form onSubmit={handleSubmit} className="mt-6 space-y-4">
            <div>
              <label htmlFor="companyName" className="label">
                Company name
              </label>
              <div className="relative">
                <Building2 size={17} className="pointer-events-none absolute left-3 top-3 text-gray-400" />
                <input
                  id="companyName"
                  className="input pl-10"
                  placeholder="Acme Inc."
                  value={companyName}
                  onChange={(e) => setCompanyName(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label htmlFor="website" className="label">
                  Website
                </label>
                <input
                  id="website"
                  className="input"
                  placeholder="https://acme.com"
                  value={website}
                  onChange={(e) => setWebsite(e.target.value)}
                />
              </div>
              <div>
                <label htmlFor="industry" className="label">
                  Industry
                </label>
                <input
                  id="industry"
                  className="input"
                  placeholder="Software"
                  value={industry}
                  onChange={(e) => setIndustry(e.target.value)}
                />
              </div>
            </div>

            <div>
              <label htmlFor="companySize" className="label">
                Company size
              </label>
              <select
                id="companySize"
                className="input"
                value={companySize}
                onChange={(e) => setCompanySize(e.target.value)}
              >
                <option value="">Select…</option>
                <option value="1-10">1-10</option>
                <option value="11-50">11-50</option>
                <option value="51-200">51-200</option>
                <option value="201-1000">201-1000</option>
                <option value="1000+">1000+</option>
              </select>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label htmlFor="firstName" className="label">
                  Your first name
                </label>
                <div className="relative">
                  <User size={17} className="pointer-events-none absolute left-3 top-3 text-gray-400" />
                  <input
                    id="firstName"
                    className="input pl-10"
                    placeholder="Ada"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    required
                  />
                </div>
              </div>
              <div>
                <label htmlFor="lastName" className="label">
                  Your last name
                </label>
                <input
                  id="lastName"
                  className="input"
                  placeholder="Lovelace"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  required
                />
              </div>
            </div>

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
                  placeholder="hr@acme.com"
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
                  autoComplete="new-password"
                  className="input px-10"
                  placeholder="At least 8 characters"
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
              {password && (
                <div className="mt-2 flex items-center gap-2">
                  <div className="flex flex-1 gap-1">
                    {[1, 2, 3, 4].map((i) => (
                      <span
                        key={i}
                        className={`h-1.5 flex-1 rounded-full transition ${
                          i <= strength.score ? strength.color : 'bg-gray-200 dark:bg-gray-700'
                        }`}
                      />
                    ))}
                  </div>
                  <span className="w-14 text-right text-xs font-medium text-gray-500">
                    {strength.label}
                  </span>
                </div>
              )}
            </div>

            <button type="submit" className="btn-primary w-full" disabled={register.isPending}>
              {register.isPending ? 'Creating account…' : 'Create Company Account'}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
            Already have a company account?{' '}
            <Link
              to="/company/login"
              className="font-semibold text-primary-600 hover:underline dark:text-primary-400"
            >
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
