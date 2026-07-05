import {
  Bar,
  BarChart,
  CartesianGrid,
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

const LEVEL_WEIGHT: Record<string, number> = {
  NONE: 0,
  BEGINNER: 1,
  NOVICE: 1,
  INTERMEDIATE: 2,
  ADVANCED: 3,
  PROFICIENT: 3,
  EXPERT: 4,
  MASTER: 4,
};

function levelValue(level: string): number {
  return LEVEL_WEIGHT[(level ?? '').toUpperCase()] ?? 0;
}

export default function SkillGapChart({ skillGap }: SkillGapChartProps) {
  const data = skillGap.partialMatches.slice(0, 10).map((p) => ({
    skill: p.skill,
    you: levelValue(p.candidateLevel),
    required: levelValue(p.requiredLevel),
  }));

  return (
    <div className="space-y-5">
      {data.length > 0 ? (
        <ResponsiveContainer width="100%" height={Math.max(260, data.length * 40)}>
          <BarChart data={data} layout="vertical" margin={{ left: 12, right: 16 }} barGap={2}>
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
      ) : (
        <p className="py-6 text-center text-sm text-gray-500">No partial-match data available.</p>
      )}

      {skillGap.missingSkills.length > 0 && (
        <div>
          <p className="mb-2 text-sm font-medium text-error-600">Missing skills</p>
          <div className="flex flex-wrap gap-2">
            {skillGap.missingSkills.map((s) => (
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
