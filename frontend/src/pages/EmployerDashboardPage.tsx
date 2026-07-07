import { Link } from 'react-router-dom';
import { Briefcase, MapPin, Plus } from 'lucide-react';
import EmployerLayout from '../components/common/EmployerLayout';
import LoadingSpinner from '../components/common/LoadingSpinner';
import { useMyJobs, useSetJobActive } from '../hooks/useCompany';
import type { JobListing } from '../types';

export default function EmployerDashboardPage() {
  const { data: page, isLoading } = useMyJobs();
  const setActive = useSetJobActive();

  const jobs = page?.content ?? [];

  return (
    <EmployerLayout>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Your job postings</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Posted jobs are semantically matched to candidates automatically.
          </p>
        </div>
        <Link to="/employer/jobs/new" className="btn-primary">
          <Plus size={17} /> Post a Job
        </Link>
      </div>

      {isLoading ? (
        <LoadingSpinner className="py-16" label="Loading your jobs…" />
      ) : jobs.length === 0 ? (
        <div className="card p-12 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-gray-100 text-gray-400 dark:bg-gray-800">
            <Briefcase size={26} />
          </div>
          <p className="font-medium text-gray-700 dark:text-gray-200">No jobs posted yet</p>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Post your first role to start matching with candidates.
          </p>
          <Link to="/employer/jobs/new" className="btn-primary mt-4 inline-flex">
            <Plus size={17} /> Post a Job
          </Link>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {jobs.map((job) => (
            <JobCard
              key={job.id}
              job={job}
              onToggle={(active) => setActive.mutate({ jobId: job.id, active })}
              toggling={setActive.isPending}
            />
          ))}
        </div>
      )}
    </EmployerLayout>
  );
}

function JobCard({
  job,
  onToggle,
  toggling,
}: {
  job: JobListing;
  onToggle: (active: boolean) => void;
  toggling: boolean;
}) {
  const active = job.active ?? true;
  return (
    <div className="card flex flex-col p-5">
      <div className="mb-2 flex items-start justify-between gap-2">
        <h3 className="font-semibold text-gray-900 dark:text-gray-100">{job.title}</h3>
        <span
          className={`chip ${
            active
              ? 'bg-success-500/10 text-success-600 dark:text-success-400'
              : 'bg-gray-500/10 text-gray-500'
          }`}
        >
          {active ? 'Open' : 'Closed'}
        </span>
      </div>
      <p className="text-sm text-gray-500 dark:text-gray-400">{job.company}</p>
      {job.location && (
        <p className="mt-1 flex items-center gap-1 text-xs text-gray-400">
          <MapPin size={13} /> {job.location}
        </p>
      )}
      {job.requiredSkills.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-1.5">
          {job.requiredSkills.slice(0, 6).map((s) => (
            <span key={s} className="chip bg-primary-500/10 text-primary-600 dark:text-primary-400">
              {s}
            </span>
          ))}
        </div>
      )}
      <div className="mt-4 flex-1" />
      <button
        onClick={() => onToggle(!active)}
        disabled={toggling}
        className="btn-secondary mt-2 w-full"
      >
        {active ? 'Close job' : 'Re-open job'}
      </button>
    </div>
  );
}
