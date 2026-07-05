import { PolarAngleAxis, RadialBar, RadialBarChart, ResponsiveContainer } from 'recharts';
import { scoreColor } from '../../utils/formatters';

interface AtsScoreGaugeProps {
  score: number;
}

export default function AtsScoreGauge({ score }: AtsScoreGaugeProps) {
  const clamped = Math.max(0, Math.min(100, score));
  const { hex, text } = scoreColor(clamped);
  const data = [{ name: 'ATS', value: clamped, fill: hex }];

  return (
    <div className="flex flex-col items-center">
      <div className="relative h-52 w-52">
        <ResponsiveContainer width="100%" height="100%">
          <RadialBarChart
            innerRadius="72%"
            outerRadius="100%"
            data={data}
            startAngle={90}
            endAngle={-270}
          >
            <PolarAngleAxis type="number" domain={[0, 100]} angleAxisId={0} tick={false} />
            <RadialBar background={{ fill: 'rgba(148,163,184,0.18)' }} dataKey="value" cornerRadius={16} />
          </RadialBarChart>
        </ResponsiveContainer>
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
          <span className={`text-5xl font-bold ${text}`}>{Math.round(clamped)}</span>
          <span className="text-xs font-medium text-gray-400">/ 100</span>
        </div>
      </div>
      <p className="mt-2 text-sm font-medium text-gray-600 dark:text-gray-300">
        ATS Compatibility Score
      </p>
      <p className="mt-1 text-xs text-gray-400">
        {clamped <= 40 ? 'Needs work' : clamped <= 70 ? 'Good, room to improve' : 'Excellent'}
      </p>
    </div>
  );
}
