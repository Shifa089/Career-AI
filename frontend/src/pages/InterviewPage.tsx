import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, MessageSquare, Plus } from 'lucide-react';
import AppLayout from '../components/common/AppLayout';
import PageHeader from '../components/common/PageHeader';
import LoadingSpinner from '../components/common/LoadingSpinner';
import SessionSetup from '../components/interview/SessionSetup';
import { useSessions } from '../hooks/useInterview';
import {
  formatDate,
  formatEnum,
  formatScore,
  sessionStatusColor,
} from '../utils/formatters';

export default function InterviewPage() {
  const [setupOpen, setSetupOpen] = useState(false);
  const { data: page, isLoading } = useSessions(0, 30);
  const sessions = page?.content ?? [];

  return (
    <AppLayout>
      <PageHeader
        title="Mock Interviews"
        subtitle="Practice with a real-time AI interviewer and get scored feedback."
        icon={<MessageSquare size={22} />}
        actions={
          <button className="btn-primary" onClick={() => setSetupOpen(true)}>
            <Plus size={17} /> Start New Interview
          </button>
        }
      />

      {isLoading ? (
        <LoadingSpinner className="py-16" label="Loading interviews…" />
      ) : sessions.length === 0 ? (
        <div className="card p-12 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-gray-100 text-gray-400 dark:bg-gray-800">
            <MessageSquare size={26} />
          </div>
          <p className="font-medium text-gray-700 dark:text-gray-200">No interviews yet</p>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Start your first mock interview to build confidence.
          </p>
          <button className="btn-primary mx-auto mt-5" onClick={() => setSetupOpen(true)}>
            <Plus size={17} /> Start New Interview
          </button>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {sessions.map((s) => {
            const resumable = s.status === 'CREATED' || s.status === 'ACTIVE' || s.status === 'PAUSED';
            return (
              <div key={s.id} className="card flex flex-col p-5">
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <h3 className="truncate font-semibold text-gray-900 dark:text-gray-100">
                      {s.jobTitle}
                    </h3>
                    {s.targetCompany && (
                      <p className="truncate text-sm text-gray-500 dark:text-gray-400">
                        {s.targetCompany}
                      </p>
                    )}
                  </div>
                  <span className={`chip shrink-0 ${sessionStatusColor(s.status)}`}>{s.status}</span>
                </div>

                <div className="mt-4 flex items-center gap-3 text-xs text-gray-400">
                  <span className="chip bg-secondary-500/10 text-secondary-600 dark:text-secondary-400">
                    {formatEnum(s.type)}
                  </span>
                  <span className="flex items-center gap-1">
                    <Calendar size={12} /> {formatDate(s.createdAt)}
                  </span>
                </div>

                <div className="mt-4 flex items-center justify-between border-t border-gray-100 pt-4 dark:border-gray-800">
                  <div>
                    <p className="text-xs text-gray-400">
                      {s.questionsAnswered}/{s.totalQuestions} answered
                    </p>
                    {s.overallScore != null && (
                      <p className="text-lg font-bold text-gray-900 dark:text-gray-100">
                        {formatScore(s.overallScore)}
                        <span className="text-xs font-normal text-gray-400"> / 100</span>
                      </p>
                    )}
                  </div>
                  <Link
                    to={`/interviews/${s.id}`}
                    className={resumable ? 'btn-primary py-2 text-xs' : 'btn-secondary py-2 text-xs'}
                  >
                    {resumable ? 'Continue' : 'View Results'}
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <SessionSetup open={setupOpen} onClose={() => setSetupOpen(false)} />
    </AppLayout>
  );
}
