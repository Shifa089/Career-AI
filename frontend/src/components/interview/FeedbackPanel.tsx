import { CheckCircle2 } from 'lucide-react';
import { scoreColor } from '../../utils/formatters';

interface FeedbackPanelProps {
  score: number;
  feedback: string;
}

export default function FeedbackPanel({ score, feedback }: FeedbackPanelProps) {
  const clamped = Math.max(0, Math.min(100, score));
  const { text, bg } = scoreColor(clamped);

  return (
    <div className="card animate-slide-up p-5">
      <div className="mb-3 flex items-center justify-between">
        <span className="flex items-center gap-2 font-semibold text-gray-900 dark:text-gray-100">
          <CheckCircle2 size={18} className={text} />
          Answer Feedback
        </span>
        <span className={`text-2xl font-bold ${text}`}>{Math.round(clamped)}</span>
      </div>

      <div className="mb-4 h-2 overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
        <div className={`h-full rounded-full ${bg} transition-all duration-500`} style={{ width: `${clamped}%` }} />
      </div>

      <p className="text-sm leading-relaxed text-gray-600 dark:text-gray-300">{feedback}</p>
    </div>
  );
}
