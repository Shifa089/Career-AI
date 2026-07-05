import { Disclosure } from '@headlessui/react';
import { ChevronDown, Lightbulb, ThumbsUp, TriangleAlert } from 'lucide-react';
import type { ReactNode } from 'react';
import type { ResumeAnalysis } from '../../types';

interface SectionProps {
  title: string;
  items: string[];
  icon: ReactNode;
  accent: string;
  defaultOpen?: boolean;
}

function Section({ title, items, icon, accent, defaultOpen }: SectionProps) {
  if (!items || items.length === 0) return null;
  return (
    <Disclosure defaultOpen={defaultOpen}>
      {({ open }) => (
        <div className="overflow-hidden rounded-xl border border-gray-100 dark:border-gray-800">
          <Disclosure.Button className="flex w-full items-center justify-between px-4 py-3 text-left transition hover:bg-gray-50 dark:hover:bg-gray-800/50">
            <span className="flex items-center gap-2.5 font-semibold text-gray-900 dark:text-gray-100">
              <span className={accent}>{icon}</span>
              {title}
              <span className="chip bg-gray-500/10 text-gray-500">{items.length}</span>
            </span>
            <ChevronDown
              size={18}
              className={`text-gray-400 transition ${open ? 'rotate-180' : ''}`}
            />
          </Disclosure.Button>
          <Disclosure.Panel className="px-4 pb-4">
            <ul className="space-y-2">
              {items.map((item, i) => (
                <li
                  key={i}
                  className="flex gap-2.5 text-sm leading-relaxed text-gray-600 dark:text-gray-300"
                >
                  <span className={`mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full ${accent.replace('text-', 'bg-')}`} />
                  {item}
                </li>
              ))}
            </ul>
          </Disclosure.Panel>
        </div>
      )}
    </Disclosure>
  );
}

interface SuggestionsPanelProps {
  analysis: ResumeAnalysis;
}

export default function SuggestionsPanel({ analysis }: SuggestionsPanelProps) {
  return (
    <div className="space-y-4">
      <Section
        title="Strengths"
        items={analysis.strengths}
        icon={<ThumbsUp size={18} />}
        accent="text-success-600"
        defaultOpen
      />
      <Section
        title="Weaknesses"
        items={analysis.weaknesses}
        icon={<TriangleAlert size={18} />}
        accent="text-error-600"
      />
      <Section
        title="Suggestions"
        items={analysis.suggestions}
        icon={<Lightbulb size={18} />}
        accent="text-primary-600"
        defaultOpen
      />

      {analysis.missingKeywords?.length > 0 && (
        <div className="rounded-xl border border-gray-100 p-4 dark:border-gray-800">
          <p className="mb-3 font-semibold text-gray-900 dark:text-gray-100">Missing Keywords</p>
          <div className="flex flex-wrap gap-2">
            {analysis.missingKeywords.map((kw) => (
              <span key={kw} className="chip bg-error-500/10 text-error-600 dark:text-error-500">
                {kw}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
