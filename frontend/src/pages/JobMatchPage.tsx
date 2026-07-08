import { Fragment, useMemo, useState, type ReactNode } from 'react';
import { Dialog, Tab, Transition } from '@headlessui/react';
import { Briefcase, ChevronDown, Clock, Search, SlidersHorizontal, X } from 'lucide-react';
import AppLayout from '../components/common/AppLayout';
import PageHeader from '../components/common/PageHeader';
import LoadingSpinner from '../components/common/LoadingSpinner';
import JobMatchCard from '../components/jobs/JobMatchCard';
import JobListingCard from '../components/jobs/JobListingCard';
import SkillGapChart from '../components/jobs/SkillGapChart';
import LearningPath from '../components/jobs/LearningPath';
import { useResumes } from '../hooks/useResume';
import {
  useFindMatches,
  useLearningPath,
  useMatches,
  useSearchJobs,
  useSkillGap,
} from '../hooks/useJobMatch';
import { matchStatusLabel } from '../utils/formatters';
import type { JobMatch, MatchStatus } from '../types';

const STATUS_FILTERS: (MatchStatus | 'ALL')[] = ['ALL', 'PENDING_REVIEW', 'SAVED', 'APPLIED', 'REJECTED'];

type JobsTab = 'matches' | 'latest';

export default function JobMatchPage() {
  const [tab, setTab] = useState<JobsTab>('matches');
  const [gapMatch, setGapMatch] = useState<JobMatch | null>(null);

  return (
    <AppLayout>
      <PageHeader
        title="Job Matches"
        subtitle="Browse the latest openings, or let semantic matching find the roles that fit your resume best."
        icon={<Briefcase size={22} />}
      />

      {/* Tabs */}
      <div className="mb-6 flex gap-1 rounded-xl bg-gray-100 p-1 dark:bg-gray-800 sm:w-fit">
        {(
          [
            { id: 'matches', label: 'My Matches', icon: <Briefcase size={15} /> },
            { id: 'latest', label: 'Latest Jobs', icon: <Clock size={15} /> },
          ] as { id: JobsTab; label: string; icon: ReactNode }[]
        ).map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={`flex flex-1 items-center justify-center gap-1.5 rounded-lg px-4 py-2 text-sm font-medium transition sm:flex-none ${
              tab === t.id
                ? 'bg-white text-primary-700 shadow-sm dark:bg-gray-900 dark:text-primary-400'
                : 'text-gray-500 hover:text-gray-700 dark:text-gray-400'
            }`}
          >
            {t.icon}
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'matches' ? (
        <MatchesTab onViewGap={setGapMatch} />
      ) : (
        <LatestJobsTab />
      )}

      <SkillGapDrawer match={gapMatch} onClose={() => setGapMatch(null)} />
    </AppLayout>
  );
}

function MatchesTab({ onViewGap }: { onViewGap: (m: JobMatch) => void }) {
  const { data: resumes } = useResumes();
  const [resumeId, setResumeId] = useState('');
  const [minMatch, setMinMatch] = useState(0);
  const [statusFilter, setStatusFilter] = useState<MatchStatus | 'ALL'>('ALL');
  const [filtersOpen, setFiltersOpen] = useState(true);

  const findMatches = useFindMatches();
  const { data: page, isLoading } = useMatches(
    statusFilter === 'ALL' ? undefined : statusFilter,
    0,
    50,
  );

  const analysedResumes = useMemo(
    () => (resumes ?? []).filter((r) => r.status === 'ANALYSED'),
    [resumes],
  );

  const effectiveResumeId =
    resumeId || analysedResumes.find((r) => r.primary)?.id || analysedResumes[0]?.id || '';

  const matches = (page?.content ?? []).filter((m) => m.matchPercentage >= minMatch);

  const handleFind = () => {
    if (!effectiveResumeId) return;
    findMatches.mutate({ resumeId: effectiveResumeId, limit: 15 });
  };

  return (
    <>
      {/* Find bar */}
      <div className="card mb-6 flex flex-col gap-3 p-4 sm:flex-row sm:items-end">
        <div className="flex-1">
          <label htmlFor="resume" className="label">
            Resume
          </label>
          <select
            id="resume"
            className="input"
            value={effectiveResumeId}
            onChange={(e) => setResumeId(e.target.value)}
            disabled={analysedResumes.length === 0}
          >
            {analysedResumes.length === 0 ? (
              <option value="">No analysed resumes — upload one first</option>
            ) : (
              analysedResumes.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.originalFileName}
                  {r.primary ? ' (primary)' : ''}
                </option>
              ))
            )}
          </select>
        </div>
        <button
          className="btn-primary"
          onClick={handleFind}
          disabled={findMatches.isPending || !effectiveResumeId}
        >
          <Search size={17} /> {findMatches.isPending ? 'Finding…' : 'Find Jobs for My Resume'}
        </button>
      </div>

      {/* Filters */}
      <div className="card mb-6 p-4">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
          <button
            type="button"
            onClick={() => setFiltersOpen((o) => !o)}
            aria-expanded={filtersOpen}
            aria-controls="filter-controls"
            className="flex items-center gap-2 text-sm font-medium text-gray-600 transition hover:text-primary-600 dark:text-gray-300 dark:hover:text-primary-400"
          >
            <SlidersHorizontal size={16} /> Filters
            <ChevronDown
              size={15}
              className={`transition-transform ${filtersOpen ? 'rotate-180' : ''}`}
            />
          </button>

          {filtersOpen && (
            <div id="filter-controls" className="flex flex-1 flex-col gap-4 sm:flex-row sm:items-center">
              <div className="flex flex-1 items-center gap-3">
                <label htmlFor="minMatch" className="whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                  Min match:{' '}
                  <span className="font-semibold text-primary-600 dark:text-primary-400">{minMatch}%</span>
                </label>
                <input
                  id="minMatch"
                  type="range"
                  min={0}
                  max={100}
                  step={5}
                  value={minMatch}
                  onChange={(e) => setMinMatch(Number(e.target.value))}
                  className="flex-1 accent-primary-600"
                />
              </div>
              <div className="flex flex-wrap gap-1.5">
                {STATUS_FILTERS.map((s) => (
                  <button
                    key={s}
                    onClick={() => setStatusFilter(s)}
                    className={`chip transition ${
                      statusFilter === s
                        ? 'bg-primary-600 text-white'
                        : 'bg-gray-100 text-gray-500 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-400'
                    }`}
                  >
                    {s === 'ALL' ? 'All' : matchStatusLabel(s)}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Matches grid */}
      {isLoading ? (
        <LoadingSpinner className="py-16" label="Loading matches…" />
      ) : matches.length === 0 ? (
        <div className="card p-12 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-gray-100 text-gray-400 dark:bg-gray-800">
            <Briefcase size={26} />
          </div>
          <p className="font-medium text-gray-700 dark:text-gray-200">No matches to show</p>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Pick a resume and hit “Find Jobs” to generate matches — or browse the Latest Jobs tab.
          </p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {matches.map((m) => (
            <JobMatchCard key={m.matchId} match={m} onViewGap={onViewGap} />
          ))}
        </div>
      )}
    </>
  );
}

function LatestJobsTab() {
  const [keyword, setKeyword] = useState('');
  const [location, setLocation] = useState('');
  const { data: page, isLoading } = useSearchJobs(keyword, location, 0, 50);
  const jobs = page?.content ?? [];

  return (
    <>
      {/* Search bar */}
      <div className="card mb-6 flex flex-col gap-3 p-4 sm:flex-row sm:items-end">
        <div className="flex-1">
          <label htmlFor="jobKeyword" className="label">
            Keyword
          </label>
          <input
            id="jobKeyword"
            className="input"
            placeholder="Title, company or keyword…"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
        </div>
        <div className="flex-1">
          <label htmlFor="jobLocation" className="label">
            Location
          </label>
          <input
            id="jobLocation"
            className="input"
            placeholder="Anywhere…"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
          />
        </div>
      </div>

      {isLoading ? (
        <LoadingSpinner className="py-16" label="Loading latest jobs…" />
      ) : jobs.length === 0 ? (
        <div className="card p-12 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-gray-100 text-gray-400 dark:bg-gray-800">
            <Briefcase size={26} />
          </div>
          <p className="font-medium text-gray-700 dark:text-gray-200">No jobs found</p>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {keyword || location
              ? 'Try a broader search.'
              : 'No open roles right now — check back soon.'}
          </p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {jobs.map((job) => (
            <JobListingCard key={job.id} job={job} />
          ))}
        </div>
      )}
    </>
  );
}

function SkillGapDrawer({ match, onClose }: { match: JobMatch | null; onClose: () => void }) {
  const { data: skillGap, isLoading: gapLoading } = useSkillGap(match?.matchId, Boolean(match));
  const { data: learningPath, isLoading: pathLoading } = useLearningPath(
    match?.matchId,
    Boolean(match),
  );

  return (
    <Transition show={Boolean(match)} as={Fragment}>
      <Dialog onClose={onClose} className="relative z-50">
        <Transition.Child
          as={Fragment}
          enter="ease-out duration-200"
          enterFrom="opacity-0"
          enterTo="opacity-100"
          leave="ease-in duration-150"
          leaveFrom="opacity-100"
          leaveTo="opacity-0"
        >
          <div className="fixed inset-0 bg-gray-900/50 backdrop-blur-sm" />
        </Transition.Child>

        <div className="fixed inset-0 overflow-hidden">
          <div className="absolute inset-y-0 right-0 flex max-w-full pl-10">
            <Transition.Child
              as={Fragment}
              enter="transform transition ease-out duration-300"
              enterFrom="translate-x-full"
              enterTo="translate-x-0"
              leave="transform transition ease-in duration-200"
              leaveFrom="translate-x-0"
              leaveTo="translate-x-full"
            >
              <Dialog.Panel className="w-screen max-w-xl overflow-y-auto bg-white p-6 shadow-2xl dark:bg-gray-900">
                <div className="mb-4 flex items-center justify-between">
                  <div>
                    <Dialog.Title className="text-lg font-bold text-gray-900 dark:text-gray-100">
                      {match?.job.title}
                    </Dialog.Title>
                    <p className="text-sm text-gray-500 dark:text-gray-400">{match?.job.company}</p>
                  </div>
                  <button
                    onClick={onClose}
                    className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800"
                  >
                    <X size={20} />
                  </button>
                </div>

                <Tab.Group>
                  <Tab.List className="mb-6 flex gap-1 rounded-xl bg-gray-100 p-1 dark:bg-gray-800">
                    {['Skill Gap', 'Learning Path'].map((label) => (
                      <Tab
                        key={label}
                        className={({ selected }) =>
                          `flex-1 rounded-lg px-3 py-2 text-sm font-medium transition focus:outline-none ${
                            selected
                              ? 'bg-white text-primary-700 shadow-sm dark:bg-gray-900 dark:text-primary-400'
                              : 'text-gray-500 hover:text-gray-700 dark:text-gray-400'
                          }`
                        }
                      >
                        {label}
                      </Tab>
                    ))}
                  </Tab.List>
                  <Tab.Panels>
                    <Tab.Panel>
                      {gapLoading || !skillGap ? (
                        <LoadingSpinner className="py-12" />
                      ) : (
                        <SkillGapChart skillGap={skillGap} />
                      )}
                    </Tab.Panel>
                    <Tab.Panel>
                      {pathLoading || !learningPath ? (
                        <LoadingSpinner className="py-12" />
                      ) : (
                        <LearningPath learningPath={learningPath} />
                      )}
                    </Tab.Panel>
                  </Tab.Panels>
                </Tab.Group>
              </Dialog.Panel>
            </Transition.Child>
          </div>
        </div>
      </Dialog>
    </Transition>
  );
}
