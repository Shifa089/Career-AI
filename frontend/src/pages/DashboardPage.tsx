import { Link } from 'react-router-dom';
import {
  ArrowUpRight,
  Award,
  Briefcase,
  FileText,
  MessageSquare,
  Target,
  TrendingUp,
  Upload,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import AppLayout from '../components/common/AppLayout';
import LoadingSpinner from '../components/common/LoadingSpinner';
import { useAuthStore } from '../store/authStore';
import { useCurrentUser } from '../hooks/useAuth';
import { useResumes } from '../hooks/useResume';
import { useInterviewStats, useSessions } from '../hooks/useInterview';
import { useMatches } from '../hooks/useJobMatch';
import {
  formatRelative,
  formatScore,
  resumeStatusColor,
  sessionStatusColor,
} from '../utils/formatters';

function StatCard({
  label,
  value,
  icon: Icon,
  accent,
}: {
  label: string;
  value: string;
  icon: LucideIcon;
  accent: string;
}) {
  return (
    <div className="card flex items-center gap-4 p-5">
      <div className={`flex h-12 w-12 items-center justify-center rounded-xl ${accent}`}>
        <Icon size={22} />
      </div>
      <div>
        <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{value}</p>
        <p className="text-xs font-medium text-gray-500 dark:text-gray-400">{label}</p>
      </div>
    </div>
  );
}

function QuickAction({
  to,
  title,
  description,
  icon: Icon,
  accent,
}: {
  to: string;
  title: string;
  description: string;
  icon: LucideIcon;
  accent: string;
}) {
  return (
    <Link
      to={to}
      className="card group flex items-center gap-4 p-6 transition hover:-translate-y-0.5 hover:shadow-md"
    >
      <div className={`flex h-12 w-12 items-center justify-center rounded-xl text-white ${accent}`}>
        <Icon size={22} />
      </div>
      <div className="flex-1">
        <p className="font-semibold text-gray-900 dark:text-gray-100">{title}</p>
        <p className="text-sm text-gray-500 dark:text-gray-400">{description}</p>
      </div>
      <ArrowUpRight
        size={18}
        className="text-gray-300 transition group-hover:text-primary-500"
      />
    </Link>
  );
}

export default function DashboardPage() {
  useCurrentUser();
  const user = useAuthStore((s) => s.user);
  const { data: resumes, isLoading: resumesLoading } = useResumes();
  const { data: sessionsPage, isLoading: sessionsLoading } = useSessions(0, 5);
  const { data: stats } = useInterviewStats();
  const { data: matchesPage } = useMatches(undefined, 0, 1);

  const recentResumes = resumes?.slice(0, 5) ?? [];
  const recentSessions = sessionsPage?.content?.slice(0, 5) ?? [];
  const firstName = user?.fullName?.split(' ')[0] ?? 'there';

  return (
    <AppLayout>
      <div className="mb-8">
        <h1 className="text-2xl font-bold tracking-tight text-gray-900 dark:text-gray-50">
          Welcome back, {firstName} 👋
        </h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Here’s a snapshot of your job-search progress.
        </p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard
          label="Total Resumes"
          value={`${resumes?.length ?? 0}`}
          icon={FileText}
          accent="bg-primary-500/10 text-primary-600 dark:text-primary-400"
        />
        <StatCard
          label="Interviews Completed"
          value={`${stats?.completedSessions ?? 0}`}
          icon={MessageSquare}
          accent="bg-secondary-500/10 text-secondary-600 dark:text-secondary-400"
        />
        <StatCard
          label="Avg Interview Score"
          value={formatScore(stats?.averageScore)}
          icon={Award}
          accent="bg-success-500/10 text-success-600 dark:text-success-500"
        />
        <StatCard
          label="Job Matches"
          value={`${matchesPage?.totalElements ?? 0}`}
          icon={Briefcase}
          accent="bg-accent-500/10 text-accent-600 dark:text-accent-400"
        />
      </div>

      {/* Quick actions */}
      <h2 className="mb-4 mt-10 text-lg font-semibold text-gray-900 dark:text-gray-100">
        Quick Actions
      </h2>
      <div className="grid gap-4 md:grid-cols-3">
        <QuickAction
          to="/resumes"
          title="Upload Resume"
          description="Analyze & optimize"
          icon={Upload}
          accent="bg-gradient-to-br from-primary-500 to-primary-700"
        />
        <QuickAction
          to="/interviews"
          title="Start Interview"
          description="Practice with AI"
          icon={Target}
          accent="bg-gradient-to-br from-secondary-500 to-secondary-600"
        />
        <QuickAction
          to="/jobs"
          title="Find Jobs"
          description="Smart matching"
          icon={TrendingUp}
          accent="bg-gradient-to-br from-accent-500 to-accent-600"
        />
      </div>

      {/* Recent activity */}
      <div className="mt-10 grid gap-6 lg:grid-cols-2">
        <div className="card p-6">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="font-semibold text-gray-900 dark:text-gray-100">Recent Resumes</h3>
            <Link to="/resumes" className="text-xs font-medium text-primary-600 dark:text-primary-400">
              View all
            </Link>
          </div>
          {resumesLoading ? (
            <LoadingSpinner className="py-8" />
          ) : recentResumes.length === 0 ? (
            <EmptyState text="No resumes yet — upload one to get started." />
          ) : (
            <ul className="space-y-3">
              {recentResumes.map((r) => (
                <li key={r.id} className="flex items-center gap-3">
                  <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary-500/10 text-primary-600 dark:text-primary-400">
                    <FileText size={16} />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">
                      {r.originalFileName}
                    </p>
                    <p className="text-xs text-gray-400">{formatRelative(r.createdAt)}</p>
                  </div>
                  <span className={`chip ${resumeStatusColor(r.status)}`}>{r.status}</span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="card p-6">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="font-semibold text-gray-900 dark:text-gray-100">Recent Interviews</h3>
            <Link to="/interviews" className="text-xs font-medium text-primary-600 dark:text-primary-400">
              View all
            </Link>
          </div>
          {sessionsLoading ? (
            <LoadingSpinner className="py-8" />
          ) : recentSessions.length === 0 ? (
            <EmptyState text="No interviews yet — start your first mock interview." />
          ) : (
            <ul className="space-y-3">
              {recentSessions.map((s) => (
                <li key={s.id} className="flex items-center gap-3">
                  <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-secondary-500/10 text-secondary-600 dark:text-secondary-400">
                    <MessageSquare size={16} />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">
                      {s.jobTitle}
                    </p>
                    <p className="text-xs text-gray-400">{formatRelative(s.createdAt)}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    {s.overallScore != null && (
                      <span className="text-sm font-semibold text-gray-700 dark:text-gray-200">
                        {formatScore(s.overallScore)}
                      </span>
                    )}
                    <span className={`chip ${sessionStatusColor(s.status)}`}>{s.status}</span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </AppLayout>
  );
}

function EmptyState({ text }: { text: string }) {
  return <p className="py-8 text-center text-sm text-gray-400">{text}</p>;
}
