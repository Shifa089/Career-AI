import { HelpCircle } from 'lucide-react';
import type { InterviewQuestion } from '../../types';
import { difficultyColor, formatEnum } from '../../utils/formatters';

interface QuestionCardProps {
  question: InterviewQuestion;
}

export default function QuestionCard({ question }: QuestionCardProps) {
  return (
    <div key={question.questionId} className="card animate-fade-in p-6 sm:p-8">
      <div className="mb-5 flex flex-wrap items-center gap-3">
        <span className="flex h-9 items-center justify-center rounded-full bg-primary-600 px-3 text-sm font-semibold text-white">
          Q{question.questionNumber}
          <span className="ml-1 opacity-70">/ {question.totalQuestions}</span>
        </span>
        {question.difficulty && (
          <span className={`chip ${difficultyColor(question.difficulty)}`}>
            {formatEnum(question.difficulty)}
          </span>
        )}
        <span className="chip bg-secondary-500/15 text-secondary-600 dark:text-secondary-400">
          {formatEnum(question.type)}
        </span>
      </div>

      <div className="flex gap-3">
        <HelpCircle className="mt-1 shrink-0 text-primary-400" size={22} />
        <p className="text-lg font-medium leading-relaxed text-gray-900 dark:text-gray-100">
          {question.questionText}
        </p>
      </div>
    </div>
  );
}
