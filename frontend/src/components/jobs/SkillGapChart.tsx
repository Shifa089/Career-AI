import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { SkillGap } from '../../types';

interface SkillGapChartProps {
  skillGap: SkillGap;
}

// Canonical proficiency levels on a 0-4 scale. The backend is asked to emit one of
// NONE|BEGINNER|INTERMEDIATE|ADVANCED|EXPERT, but Claude free-text can still drift, so
// levelValue() below is deliberately tolerant of synonyms and never collapses a real
// answer to a zero-height (invisible) bar.
const LEVEL_WEIGHT: Record<string, number> = {
  NONE: 0,
  BEGINNER: 1,
  NOVICE: 1,
  BASIC: 1,
  ELEMENTARY: 1,
  FAMILIAR: 1,
  INTERMEDIATE: 2,
  MODERATE: 2,
  WORKING: 2,
  COMPETENT: 2,
  ADVANCED: 3,
  PROFICIENT: 3,
  STRONG: 3,
  SENIOR: 3,
  EXPERT: 4,
  MASTER: 4,
  EXPERTISE: 4,
};

function levelValue(level: string): number {
  const raw = (level ?? '').trim();
  if (!raw) return 0;
  const upper = raw.toUpperCase();

  // Exact keyword match first.
  if (upper in LEVEL_WEIGHT) return LEVEL_WEIGHT[upper];

  // Substring match (e.g. "working knowledge", "advanced/expert", "no experience").
  if (/\bNO\b|NONE|ZERO/.test(upper)) return 0;
  for (const key of Object.keys(LEVEL_WEIGHT)) {
    if (upper.includes(key)) return LEVEL_WEIGHT[key];
  }

  // Numeric hints like "2 years", "3+ yrs" → rough band.
  const years = parseInt(upper, 10);
  if (!Number.isNaN(years)) return Math.max(1, Math.min(4, Math.round(years / 2)));

  // Recognised-but-unmapped string: assume a real, moderate level rather than 0.
  return 2;
}

const READINESS_STYLES: Record<string, string> = {
  HIGH: 'bg-success-500/15 text-success-600 dark:text-success-400',
  READY: 'bg-success-500/15 text-success-600 dark:text-success-400',
  MEDIUM: 'bg-amber-500/15 text-amber-600 dark:text-amber-400',
  MODERATE: 'bg-amber-500/15 text-amber-600 dark:text-amber-400',
  LOW: 'bg-error-500/15 text-error-600 dark:text-error-500',
};

function readinessStyle(level: string): string {
  const upper = (level ?? '').toUpperCase();
  for (const key of Object.keys(READINESS_STYLES)) {
    if (upper.includes(key)) return READINESS_STYLES[key];
  }
  return 'bg-primary-500/15 text-primary-600 dark:text-primary-400';
}

export default function SkillGapChart({ skillGap }: SkillGapChartProps) {
  const partial = skillGap.partialMatches ?? [];
  const matched = skillGap.matchedSkills ?? [];
  const missing = skillGap.missingSkills ?? [];

  const levelData = partial.slice(0, 10).map((p) => ({
    skill: p.skill,
    you: levelValue(p.candidateLevel),
    required: levelValue(p.requiredLevel),
  }));

  // Fallback chart: when there is no partial-match data, visualise the overall
  // matched-vs-missing skill breakdown so the graph is never blank.
  const overviewData = [
    { label: 'Matched', count: matched.length, fill: '#10B981' },
    { label: 'Partial', count: partial.length, fill: '#F59E0B' },
    { label: 'Missing', count: missing.length, fill: '#EF4444' },
  ].filter((d) => d.count > 0);

  const gapScore = Math.round(skillGap.gapScore ?? 0);

  return (
    <div className="space-y-5">
      {/* Summary header — always shown */}
      <div className="flex flex-wrap items-center gap-3">
        {skillGap.readinessLevel && (
          <span className={`chip ${readinessStyle(skillGap.readinessLevel)}`}>
            Readiness: {skillGap.readinessLevel}
          </span>
        )}
        <span className="chip bg-gray-500/10 text-gray-600 dark:text-gray-300">
          Match score: {gapScore}%
        </span>
        <span className="chip bg-success-500/10 text-success-600 dark:text-success-400">
          {matched.length} matched
        </span>
        <span className="chip bg-error-500/10 text-error-600 dark:text-error-500">
          {missing.length} missing
        </span>
      </div>

      {skillGap.summary && (
        <p className="text-sm leading-relaxed text-gray-600 dark:text-gray-300">{skillGap.summary}</p>
      )}

      {/* Primary chart: your level vs required for partially-matched skills */}
      {levelData.length > 0 ? (
        <ResponsiveContainer width="100%" height={Math.max(260, levelData.length * 40)}>
          <BarChart data={levelData} layout="vertical" margin={{ left: 12, right: 16 }} barGap={2}>
            <CartesianGrid horizontal={false} strokeDasharray="3 3" stroke="rgba(148,163,184,0.2)" />
            <XAxis
              type="number"
              domain={[0, 4]}
              ticks={[1, 2, 3, 4]}
              tickFormatter={(v) => ['', 'Beg', 'Int', 'Adv', 'Exp'][v] ?? ''}
              tick={{ fontSize: 11, fill: '#94a3b8' }}
            />
            <YAxis type="category" dataKey="skill" width={120} tick={{ fontSize: 12, fill: '#94a3b8' }} />
            <Tooltip
              cursor={{ fill: 'rgba(148,163,184,0.08)' }}
              contentStyle={{ borderRadius: 12, border: '1px solid rgba(148,163,184,0.25)', fontSize: 12 }}
            />
            <Legend wrapperStyle={{ fontSize: 12 }} />
            <Bar name="You have" dataKey="you" fill="#10B981" radius={[0, 4, 4, 0]} barSize={11} />
            <Bar name="Required" dataKey="required" fill="#6366F1" radius={[0, 4, 4, 0]} barSize={11} />
          </BarChart>
        </ResponsiveContainer>
      ) : overviewData.length > 0 ? (
        // Fallback: skill breakdown counts
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={overviewData} margin={{ left: 4, right: 16, top: 8 }}>
            <CartesianGrid vertical={false} strokeDasharray="3 3" stroke="rgba(148,163,184,0.2)" />
            <XAxis dataKey="label" tick={{ fontSize: 12, fill: '#94a3b8' }} />
            <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: '#94a3b8' }} />
            <Tooltip
              cursor={{ fill: 'rgba(148,163,184,0.08)' }}
              contentStyle={{ borderRadius: 12, border: '1px solid rgba(148,163,184,0.25)', fontSize: 12 }}
            />
            <Bar name="Skills" dataKey="count" radius={[4, 4, 0, 0]} barSize={48}>
              {overviewData.map((d) => (
                <Cell key={d.label} fill={d.fill} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      ) : (
        <p className="py-6 text-center text-sm text-gray-500">No skill-gap data available.</p>
      )}

      {matched.length > 0 && (
        <div>
          <p className="mb-2 text-sm font-medium text-success-600">Skills you already have</p>
          <div className="flex flex-wrap gap-2">
            {matched.map((s) => (
              <span key={s} className="chip bg-success-500/10 text-success-600 dark:text-success-400">
                {s}
              </span>
            ))}
          </div>
        </div>
      )}

      {missing.length > 0 && (
        <div>
          <p className="mb-2 text-sm font-medium text-error-600">Missing skills</p>
          <div className="flex flex-wrap gap-2">
            {missing.map((s) => (
              <span key={s} className="chip bg-error-500/10 text-error-600 dark:text-error-500">
                {s}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
