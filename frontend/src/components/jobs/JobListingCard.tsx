import { Briefcase, Clock, MapPin } from 'lucide-react';
import type { JobListing } from '../../types';
import { formatEnum, formatRelative } from '../../utils/formatters';

/**
 * A single job in the candidate-facing "Latest Jobs" feed. Purely presentational — shows the posting
 * with its recency, so a freshly posted job is immediately visible without needing a resume.
 */
export default function JobListingCard({ job }: { job: JobListing }) {
  return (
    <div className="card flex flex-col p-5">
      <div className="mb-1 flex items-start justify-between gap-2">
        <h3 className="font-semibold text-gray-900 dark:text-gray-100">{job.title}</h3>
        {job.jobType && (
          <span className="chip shrink-0 bg-primary-500/10 text-primary-600 dark:text-primary-400">
            {formatEnum(job.jobType)}
          </span>
        )}
      </div>
      <p className="text-sm text-gray-600 dark:text-gray-300">{job.company}</p>

      <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-gray-400">
        {job.location && (
          <span className="flex items-center gap-1">
            <MapPin size={13} /> {job.location}
          </span>
        )}
        {job.experienceLevel && (
          <span className="flex items-center gap-1">
            <Briefcase size={13} /> {formatEnum(job.experienceLevel)}
          </span>
        )}
        {job.postedAt && (
          <span className="flex items-center gap-1">
            <Clock size={13} /> {formatRelative(job.postedAt)}
          </span>
        )}
      </div>

      {job.salaryRange && (
        <p className="mt-2 text-sm font-medium text-success-600 dark:text-success-400">{job.salaryRange}</p>
      )}

      {job.descriptionText && (
        <p className="mt-3 line-clamp-3 text-sm text-gray-500 dark:text-gray-400">{job.descriptionText}</p>
      )}

      {job.requiredSkills.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-1.5">
          {job.requiredSkills.slice(0, 6).map((s) => (
            <span key={s} className="chip bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-300">
              {s}
            </span>
          ))}
        </div>
      )}

      {job.sourceUrl && (
        <>
          <div className="mt-4 flex-1" />
          <a
            href={job.sourceUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="btn-secondary mt-2 w-full justify-center"
          >
            View & Apply
          </a>
        </>
      )}
    </div>
  );
}
