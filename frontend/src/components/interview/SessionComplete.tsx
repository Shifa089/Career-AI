import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  PolarAngleAxis,
  PolarGrid,
  Radar,
  RadarChart,
  ResponsiveContainer,
} from 'recharts';
import { ArrowRight, ExternalLink, RotateCcw, Trophy } from 'lucide-react';
import type { InterviewFeedback } from '../../types';
import { scoreColor } from '../../utils/formatters';

interface SessionCompleteProps {
  feedback: InterviewFeedback;
  onRestart?: () => void;
}

function useCountUp(target: number, duration = 1200) {
  const [value, setValue] = useState(0);
  useEffect(() => {
    let raf = 0;
    const start = performance.now();
    const tick = (now: number) => {
      const p = Math.min(1, (now - start) / duration);
      setValue(Math.round(target * (1 - Math.pow(1 - p, 3))));
      if (p < 1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [target, duration]);
  return value;
}

export default function SessionComplete({ feedback, onRestart }: SessionCompleteProps) {
  const navigate = useNavigate();
  const animatedScore = useCountUp(feedback.overallScore ?? 0);
  const { text } = scoreColor(feedback.overallScore ?? 0);

  const radarData = [
    { dimension: 'Technical', value: feedback.technicalScore ?? 0 },
    { dimension: 'Behavioural', value: feedback.behaviouralScore ?? 0 },
    { dimension: 'Communication', value: feedback.communicationScore ?? 0 },
    { dimension: 'Problem Solving', value: feedback.problemSolvingScore ?? 0 },
  ];

  return (
    <div className="mx-auto max-w-4xl animate-fade-in space-y-6">
      <div className="card flex flex-col items-center p-8 text-center">
        <div className="mb-3 flex h-16 w-16 items-center justify-center rounded-full bg-gradient-to-br from-primary-500 to-accent-500 text-white">
          <Trophy size={30} />
        </div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Interview Complete!</h2>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">Here’s how you did overall.</p>
        <div className={`mt-4 text-6xl font-bold ${text}`}>{animatedScore}</div>
        <p className="text-sm font-medium text-gray-400">out of 100</p>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <div className="card p-6">
          <h3 className="mb-2 font-semibold text-gray-900 dark:text-gray-100">Skill Breakdown</h3>
          <ResponsiveContainer width="100%" height={260}>
            <RadarChart data={radarData} outerRadius="72%">
              <PolarGrid stroke="rgba(148,163,184,0.3)" />
              <PolarAngleAxis dataKey="dimension" tick={{ fontSize: 12, fill: '#94a3b8' }} />
              <Radar
                dataKey="value"
                stroke="#6366F1"
                fill="#6366F1"
                fillOpacity={0.35}
                dot
              />
            </RadarChart>
          </ResponsiveContainer>
        </div>

        <div className="card space-y-4 p-6">
          <div>
            <h3 className="mb-2 font-semibold text-success-600">Strong Areas</h3>
            <ul className="space-y-1.5">
              {(feedback.strongAreas ?? []).map((s, i) => (
                <li key={i} className="flex gap-2 text-sm text-gray-600 dark:text-gray-300">
                  <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-success-500" />
                  {s}
                </li>
              ))}
            </ul>
          </div>
          <div>
            <h3 className="mb-2 font-semibold text-warning-600">Areas to Improve</h3>
            <ul className="space-y-1.5">
              {(feedback.improvementAreas ?? []).map((s, i) => (
                <li key={i} className="flex gap-2 text-sm text-gray-600 dark:text-gray-300">
                  <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-warning-500" />
                  {s}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>

      {feedback.detailedFeedback && (
        <div className="card p-6">
          <h3 className="mb-2 font-semibold text-gray-900 dark:text-gray-100">Detailed Feedback</h3>
          <p className="whitespace-pre-line text-sm leading-relaxed text-gray-600 dark:text-gray-300">
            {feedback.detailedFeedback}
          </p>
        </div>
      )}

      {feedback.recommendedResources?.length > 0 && (
        <div className="card p-6">
          <h3 className="mb-4 font-semibold text-gray-900 dark:text-gray-100">Recommended Resources</h3>
          <div className="grid gap-3 sm:grid-cols-2">
            {feedback.recommendedResources.map((r, i) => (
              <a
                key={i}
                href={r.url}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center justify-between gap-3 rounded-xl border border-gray-100 p-4 transition hover:border-primary-300 hover:bg-primary-50/40 dark:border-gray-800 dark:hover:bg-primary-500/5"
              >
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">
                    {r.title}
                  </p>
                  <p className="text-xs text-gray-400">{r.type}</p>
                </div>
                <ExternalLink size={16} className="shrink-0 text-primary-500" />
              </a>
            ))}
          </div>
        </div>
      )}

      <div className="flex flex-col gap-3 sm:flex-row">
        <button className="btn-primary flex-1" onClick={onRestart ?? (() => navigate('/interviews'))}>
          <RotateCcw size={16} /> Start Another Interview
        </button>
        <button className="btn-secondary flex-1" onClick={() => navigate('/dashboard')}>
          Back to Dashboard <ArrowRight size={16} />
        </button>
      </div>
    </div>
  );
}
