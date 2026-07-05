import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { SkillExtraction } from '../../types';

interface SkillsChartProps {
  skills: SkillExtraction[];
}

// A single coherent categorical palette (indigo / violet / cyan / emerald / amber …).
const CATEGORY_COLORS = ['#6366F1', '#8B5CF6', '#06B6D4', '#10B981', '#F59E0B', '#EC4899', '#64748B'];

const PROFICIENCY_WEIGHT: Record<string, number> = {
  BEGINNER: 1,
  NOVICE: 1,
  INTERMEDIATE: 2,
  ADVANCED: 3,
  PROFICIENT: 3,
  EXPERT: 4,
  MASTER: 4,
};

function proficiencyValue(level: string): number {
  return PROFICIENCY_WEIGHT[(level ?? '').toUpperCase()] ?? 2;
}

export default function SkillsChart({ skills }: SkillsChartProps) {
  if (!skills || skills.length === 0) {
    return <p className="py-8 text-center text-sm text-gray-500">No skills extracted.</p>;
  }

  const categories = Array.from(new Set(skills.map((s) => s.category || 'Other')));
  const colorFor = (category: string) =>
    CATEGORY_COLORS[categories.indexOf(category) % CATEGORY_COLORS.length];

  const data = [...skills]
    .sort((a, b) => proficiencyValue(b.proficiencyLevel) - proficiencyValue(a.proficiencyLevel))
    .slice(0, 15)
    .map((s) => ({
      name: s.skillName,
      value: proficiencyValue(s.proficiencyLevel),
      category: s.category || 'Other',
      proficiency: s.proficiencyLevel,
    }));

  return (
    <div>
      <div className="mb-4 flex flex-wrap gap-3">
        {categories.map((cat) => (
          <div key={cat} className="flex items-center gap-1.5 text-xs text-gray-500 dark:text-gray-400">
            <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: colorFor(cat) }} />
            {cat}
          </div>
        ))}
      </div>
      <ResponsiveContainer width="100%" height={Math.max(240, data.length * 34)}>
        <BarChart data={data} layout="vertical" margin={{ left: 12, right: 24 }}>
          <CartesianGrid horizontal={false} strokeDasharray="3 3" stroke="rgba(148,163,184,0.2)" />
          <XAxis
            type="number"
            domain={[0, 4]}
            ticks={[1, 2, 3, 4]}
            tickFormatter={(v) => ['', 'Beginner', 'Intermediate', 'Advanced', 'Expert'][v] ?? ''}
            tick={{ fontSize: 11, fill: '#94a3b8' }}
          />
          <YAxis
            type="category"
            dataKey="name"
            width={130}
            tick={{ fontSize: 12, fill: '#94a3b8' }}
          />
          <Tooltip
            cursor={{ fill: 'rgba(148,163,184,0.08)' }}
            contentStyle={{
              borderRadius: 12,
              border: '1px solid rgba(148,163,184,0.25)',
              fontSize: 12,
            }}
            formatter={(_v, _n, item) => [item.payload.proficiency, item.payload.category]}
          />
          <Bar dataKey="value" radius={[0, 6, 6, 0]} barSize={18}>
            {data.map((d, i) => (
              <Cell key={i} fill={colorFor(d.category)} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
