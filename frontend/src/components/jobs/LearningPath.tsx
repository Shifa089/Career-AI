import { BookOpen, Clock, ExternalLink } from 'lucide-react';
import type { LearningPath as LearningPathType } from '../../types';
import { formatEnum } from '../../utils/formatters';

interface LearningPathProps {
  learningPath: LearningPathType;
}

function priorityColor(priority: string): string {
  switch ((priority ?? '').toUpperCase()) {
    case 'HIGH':
      return 'bg-error-500 text-white';
    case 'LOW':
      return 'bg-success-500 text-white';
    case 'MEDIUM':
    default:
      return 'bg-warning-500 text-white';
  }
}

export default function LearningPath({ learningPath }: LearningPathProps) {
  return (
    <div>
      <div className="mb-6 flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
        <Clock size={16} />
        Estimated{' '}
        <span className="font-semibold text-gray-900 dark:text-gray-100">
          {learningPath.totalEstimatedWeeks} weeks
        </span>{' '}
        to close the gap
      </div>

      <ol className="relative space-y-6 border-l-2 border-gray-100 pl-6 dark:border-gray-800">
        {learningPath.prioritizedSkills.map((step, i) => (
          <li key={`${step.skill}-${i}`} className="relative">
            <span
              className={`absolute -left-[31px] flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold ${priorityColor(
                step.priority,
              )}`}
            >
              {i + 1}
            </span>
            <div className="card p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h4 className="font-semibold text-gray-900 dark:text-gray-100">{step.skill}</h4>
                <div className="flex items-center gap-2">
                  <span className={`chip ${priorityColor(step.priority)}`}>
                    {formatEnum(step.priority)}
                  </span>
                  <span className="chip bg-gray-500/10 text-gray-500">
                    <Clock size={11} className="mr-1" /> {step.estimatedWeeks}w
                  </span>
                </div>
              </div>

              {step.resources?.length > 0 && (
                <div className="mt-3 grid gap-2 sm:grid-cols-2">
                  {step.resources.map((r, j) => (
                    <a
                      key={j}
                      href={r.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex items-center gap-2 rounded-lg border border-gray-100 px-3 py-2 text-sm transition hover:border-primary-300 hover:bg-primary-50/40 dark:border-gray-800 dark:hover:bg-primary-500/5"
                    >
                      <BookOpen size={15} className="shrink-0 text-primary-500" />
                      <span className="min-w-0 flex-1 truncate text-gray-700 dark:text-gray-200">
                        {r.title}
                      </span>
                      <ExternalLink size={13} className="shrink-0 text-gray-400" />
                    </a>
                  ))}
                </div>
              )}
            </div>
          </li>
        ))}
      </ol>
    </div>
  );
}
