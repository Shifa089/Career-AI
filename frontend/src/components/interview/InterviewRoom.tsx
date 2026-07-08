import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Lightbulb, Loader2, LogOut, Wifi, WifiOff } from 'lucide-react';
import { useWebSocket } from '../../hooks/useWebSocket';
import { useAbandonSession } from '../../hooks/useInterview';
import { useInterviewStore } from '../../store/interviewStore';
import type { InterviewSession } from '../../types';
import { formatEnum } from '../../utils/formatters';
import QuestionCard from './QuestionCard';
import AnswerInput from './AnswerInput';
import SessionComplete from './SessionComplete';
import LoadingSpinner from '../common/LoadingSpinner';

interface InterviewRoomProps {
  session: InterviewSession;
}

export default function InterviewRoom({ session }: InterviewRoomProps) {
  const navigate = useNavigate();
  const abandon = useAbandonSession();
  const {
    currentQuestion,
    hint,
    isHintLoading,
    finalFeedback,
    isLoading,
    isComplete,
    setSession,
    setHint,
    clearSession,
  } = useInterviewStore();

  const { isConnected, sendStartSession, sendAnswer, sendHint } = useWebSocket(session.id);

  // Seed the store and auto-start once the socket connects (fresh sessions only).
  useEffect(() => {
    setSession(session);
    return () => clearSession();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session.id]);

  useEffect(() => {
    if (isConnected && !currentQuestion && !isComplete) {
      sendStartSession();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isConnected]);

  const handleSubmit = (answer: string) => {
    if (!currentQuestion) return;
    setHint(null);
    sendAnswer(answer, currentQuestion.questionId);
  };

  const handleEnd = () => {
    if (window.confirm('End this interview? Your progress will be saved.')) {
      abandon.mutate(session.id, { onSettled: () => navigate('/interviews') });
    }
  };

  if (isComplete && finalFeedback) {
    return (
      <SessionComplete
        feedback={finalFeedback}
        onRestart={() => {
          clearSession();
          navigate('/interviews');
        }}
      />
    );
  }

  const progress = currentQuestion
    ? Math.round(((currentQuestion.questionNumber - 1) / currentQuestion.totalQuestions) * 100)
    : 0;

  return (
    <div className="space-y-6">
      {/* Top bar */}
      <div className="card flex flex-wrap items-center justify-between gap-3 p-4">
        <div className="min-w-0">
          <h1 className="truncate text-lg font-bold text-gray-900 dark:text-gray-100">
            {session.jobTitle}
          </h1>
          <p className="text-xs text-gray-500 dark:text-gray-400">
            {formatEnum(session.type)}
            {session.targetCompany ? ` · ${session.targetCompany}` : ''}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <span
            className={`flex items-center gap-1.5 text-xs font-medium ${
              isConnected ? 'text-success-600' : 'text-gray-400'
            }`}
          >
            {isConnected ? <Wifi size={14} /> : <WifiOff size={14} />}
            {isConnected ? 'Connected' : 'Connecting…'}
          </span>
          {currentQuestion && (
            <span className="chip bg-primary-500/15 text-primary-600 dark:text-primary-400">
              Q{currentQuestion.questionNumber} / {currentQuestion.totalQuestions}
            </span>
          )}
          <button className="btn-secondary py-2 text-error-600" onClick={handleEnd}>
            <LogOut size={15} /> End
          </button>
        </div>
      </div>

      {/* Progress bar */}
      <div className="h-1.5 overflow-hidden rounded-full bg-gray-200 dark:bg-gray-800">
        <div
          className="h-full rounded-full bg-gradient-to-r from-primary-600 to-accent-500 transition-all duration-500"
          style={{ width: `${progress}%` }}
        />
      </div>

      {/* Room layout */}
      <div className="grid gap-6 lg:grid-cols-5">
        <div className="lg:col-span-3">
          {currentQuestion ? (
            <QuestionCard question={currentQuestion} />
          ) : (
            <div className="card flex flex-col items-center justify-center gap-3 p-12">
              {isLoading || !isConnected ? (
                <>
                  <div className="flex gap-1.5">
                    {[0, 1, 2].map((i) => (
                      <span
                        key={i}
                        className="h-2.5 w-2.5 animate-pulse-dot rounded-full bg-primary-500"
                        style={{ animationDelay: `${i * 0.2}s` }}
                      />
                    ))}
                  </div>
                  <p className="text-sm text-gray-500">AI is preparing your interview…</p>
                </>
              ) : (
                <LoadingSpinner label="Loading…" />
              )}
            </div>
          )}

          {hint && (
            <div className="mt-6 flex gap-3 rounded-xl border border-amber-300/60 bg-amber-50 p-4 dark:border-amber-500/30 dark:bg-amber-500/10">
              <Lightbulb className="mt-0.5 shrink-0 text-amber-500" size={18} />
              <div className="min-w-0">
                <p className="text-sm font-semibold text-amber-700 dark:text-amber-400">Hint</p>
                <p className="mt-1 text-sm text-amber-900/90 dark:text-amber-100/90">{hint}</p>
              </div>
            </div>
          )}
        </div>

        <div className="lg:col-span-2">
          <div className="card sticky top-24 p-6">
            {isLoading && currentQuestion ? (
              <div className="flex min-h-[300px] flex-col items-center justify-center gap-2 text-gray-500">
                <Loader2 className="animate-spin text-primary-600" size={24} />
                <p className="text-sm">AI is thinking…</p>
              </div>
            ) : (
              <AnswerInput
                onSubmit={handleSubmit}
                onHint={sendHint}
                disabled={!currentQuestion || !isConnected}
                isSubmitting={isLoading}
                isHintLoading={isHintLoading}
              />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
