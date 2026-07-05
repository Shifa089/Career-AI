import { Link, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import AppLayout from '../components/common/AppLayout';
import LoadingSpinner from '../components/common/LoadingSpinner';
import InterviewRoom from '../components/interview/InterviewRoom';
import SessionComplete from '../components/interview/SessionComplete';
import { useFeedback, useSession } from '../hooks/useInterview';

export default function InterviewSessionPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const { data: session, isLoading, isError } = useSession(sessionId);

  const isFinished = session?.status === 'COMPLETED' || session?.status === 'ABANDONED';
  const { data: feedback, isLoading: feedbackLoading } = useFeedback(sessionId, isFinished);

  if (isLoading) {
    return (
      <AppLayout>
        <LoadingSpinner fullScreen label="Loading session…" />
      </AppLayout>
    );
  }

  if (isError || !session) {
    return (
      <AppLayout>
        <div className="card p-12 text-center">
          <p className="font-medium text-gray-700 dark:text-gray-200">Session not found</p>
          <Link to="/interviews" className="btn-primary mx-auto mt-4">
            <ArrowLeft size={16} /> Back to Interviews
          </Link>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout>
      {isFinished ? (
        feedbackLoading ? (
          <LoadingSpinner fullScreen label="Loading your feedback…" />
        ) : feedback ? (
          <SessionComplete feedback={feedback} />
        ) : (
          <div className="card p-12 text-center">
            <p className="font-medium text-gray-700 dark:text-gray-200">
              This interview has ended.
            </p>
            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
              No detailed feedback is available for this session.
            </p>
            <Link to="/interviews" className="btn-primary mx-auto mt-4">
              <ArrowLeft size={16} /> Back to Interviews
            </Link>
          </div>
        )
      ) : (
        <InterviewRoom session={session} />
      )}
    </AppLayout>
  );
}
