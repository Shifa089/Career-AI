import { Building2, MapPin, TrendingUp } from 'lucide-react';
import type { JobMatch, MatchStatus } from '../../types';
import { formatEnum, matchStatusLabel, scoreColor } from '../../utils/formatters';
import { useUpdateMatchStatus } from '../../hooks/useJobMatch';

interface JobMatchCardProps {
  match: JobMatch;
  onViewGap?: (match: JobMatch) => void;
}

const STATUS_OPTIONS: MatchStatus[] = ['PENDING_REVIEW', 'SAVED', 'APPLIED', 'REJECTED'];

function MatchRing({ percentage }: { percentage: number }) {
  const p = Math.max(0, Math.min(100, percentage));
  const { hex, text } = scoreColor(p);
  const radius = 26;
  const circ = 2 * Math.PI * radius;
  return (
    <div className="relative h-16 w-16 shrink-0">
      <svg className="h-16 w-16 -rotate-90" viewBox="0 0 64 64">
        <circle cx="32" cy="32" r={radius} fill="none" stroke="rgba(148,163,184,0.2)" strokeWidth="6" />
        <circle
          cx="32"
          cy="32"
          r={radius}
          fill="none"
          stroke={hex}
          strokeWidth="6"
          strokeLinecap="round"
          strokeDasharray={circ}
          strokeDashoffset={circ - (p / 100) * circ}
          className="transition-all duration-700"
        />
      </svg>
      <span className={`absolute inset-0 flex items-center justify-center text-sm font-bold ${text}`}>
        {Math.round(p)}%
      </span>
    </div>
  );
}

export default function JobMatchCard({ match, onViewGap }: JobMatchCardProps) {
  const updateStatus = useUpdateMatchStatus();
  const { job } = match;

  return (
    <div className="card flex flex-col p-5 transition hover:shadow-md">
      <div className="flex items-start gap-4">
        <MatchRing percentage={match.matchPercentage} />
        <div className="min-w-0 flex-1">
          <h3 className="truncate font-semibold text-gray-900 dark:text-gray-100">{job.title}</h3>
          <p className="mt-0.5 flex items-center gap-1.5 text-sm text-gray-500 dark:text-gray-400">
            <Building2 size={14} /> {job.company}
          </p>
          {job.location && (
            <p className="mt-0.5 flex items-center gap-1.5 text-xs text-gray-400">
              <MapPin size={12} /> {job.location}
              {job.jobType ? ` · ${formatEnum(job.jobType)}` : ''}
            </p>
          )}
        </div>
      </div>

      <div className="mt-4 space-y-2">
        {match.matchedSkills.length > 0 && (
          <div>
            <p className="mb-1 text-xs font-medium text-success-600">Matched skills</p>
            <div className="flex flex-wrap gap-1.5">
              {match.matchedSkills.slice(0, 6).map((s) => (
                <span key={s} className="chip bg-success-500/10 text-success-600 dark:text-success-500">
                  {s}
                </span>
              ))}
            </div>
          </div>
        )}
        {match.missingSkills.length > 0 && (
          <div>
            <p className="mb-1 text-xs font-medium text-error-600">Missing skills</p>
            <div className="flex flex-wrap gap-1.5">
              {match.missingSkills.slice(0, 5).map((s) => (
                <span key={s} className="chip bg-error-500/10 text-error-600 dark:text-error-500">
                  {s}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>

      <div className="mt-5 flex items-center gap-2 border-t border-gray-100 pt-4 dark:border-gray-800">
        <button className="btn-primary flex-1 py-2 text-xs" onClick={() => onViewGap?.(match)}>
          <TrendingUp size={15} /> View Skill Gap
        </button>
        <select
          className="input w-auto py-2 text-xs"
          value={match.status}
          onChange={(e) =>
            updateStatus.mutate({ matchId: match.matchId, status: e.target.value as MatchStatus })
          }
          disabled={updateStatus.isPending}
        >
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {matchStatusLabel(s)}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
