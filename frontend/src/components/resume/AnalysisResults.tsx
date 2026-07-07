import { Tab } from '@headlessui/react';
import { BarChart3, Gauge, Lightbulb } from 'lucide-react';
import { useAnalysis, useResume } from '../../hooks/useResume';
import LoadingSpinner from '../common/LoadingSpinner';
import AtsScoreGauge from './AtsScoreGauge';
import SkillsChart from './SkillsChart';
import SuggestionsPanel from './SuggestionsPanel';
import type { Resume } from '../../types';

interface AnalysisResultsProps {
  resume: Resume;
}

const TABS = [
  { label: 'ATS Score', icon: Gauge },
  { label: 'Skills', icon: BarChart3 },
  { label: 'Suggestions', icon: Lightbulb },
];

export default function AnalysisResults({ resume }: AnalysisResultsProps) {
  // The `resume` prop is a snapshot taken when the card/upload was opened; poll the live resume
  // so we observe the PROCESSING → ANALYSED/FAILED transition instead of spinning forever.
  const { data: liveResume } = useResume(resume.id);
  const status = liveResume?.status ?? resume.status;
  const { data: analysis, isError } = useAnalysis(resume.id, status);

  if (status === 'FAILED') {
    return (
      <div className="rounded-xl bg-error-500/10 p-6 text-center text-sm text-error-600">
        Analysis failed for this resume. Try re-uploading.
      </div>
    );
  }

  if (status !== 'ANALYSED' || !analysis) {
    // Still processing (or the analysis row isn't queryable yet) — keep showing progress.
    if (isError && status === 'ANALYSED') {
      return (
        <div className="py-16 text-center text-sm text-gray-500">
          Analysis isn’t ready yet. Check back in a moment.
        </div>
      );
    }
    return <LoadingSpinner label="Analysing your resume…" className="py-16" />;
  }

  return (
    <div>
      <div className="mb-6">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
          {resume.originalFileName}
        </h3>
        {analysis.summary && (
          <p className="mt-1 text-sm leading-relaxed text-gray-500 dark:text-gray-400">
            {analysis.summary}
          </p>
        )}
      </div>

      <Tab.Group>
        <Tab.List className="mb-6 flex gap-1 rounded-xl bg-gray-100 p-1 dark:bg-gray-800">
          {TABS.map(({ label, icon: Icon }) => (
            <Tab
              key={label}
              className={({ selected }) =>
                `flex flex-1 items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition focus:outline-none ${
                  selected
                    ? 'bg-white text-primary-700 shadow-sm dark:bg-gray-900 dark:text-primary-400'
                    : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'
                }`
              }
            >
              <Icon size={16} />
              <span className="hidden sm:inline">{label}</span>
            </Tab>
          ))}
        </Tab.List>

        <Tab.Panels>
          <Tab.Panel className="focus:outline-none">
            <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-center sm:justify-around">
              <AtsScoreGauge score={analysis.atsScore} />
              <dl className="grid grid-cols-2 gap-4 text-center">
                <Stat label="Experience" value={analysis.yearsOfExperience != null ? `${analysis.yearsOfExperience} yrs` : '—'} />
                <Stat label="Education" value={analysis.educationLevel ?? '—'} />
                <Stat label="Skills found" value={`${analysis.skills.length}`} />
                <Stat label="Keywords" value={`${analysis.keywords.length}`} />
              </dl>
            </div>
          </Tab.Panel>

          <Tab.Panel className="focus:outline-none">
            <SkillsChart skills={analysis.skills} />
          </Tab.Panel>

          <Tab.Panel className="focus:outline-none">
            <SuggestionsPanel analysis={analysis} />
          </Tab.Panel>
        </Tab.Panels>
      </Tab.Group>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-gray-100 px-5 py-4 dark:border-gray-800">
      <dt className="text-xs font-medium uppercase tracking-wide text-gray-400">{label}</dt>
      <dd className="mt-1 text-lg font-semibold text-gray-900 dark:text-gray-100">{value}</dd>
    </div>
  );
}
