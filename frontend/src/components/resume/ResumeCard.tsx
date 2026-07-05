import { Download, FileText, Star, Trash2 } from 'lucide-react';
import type { Resume } from '../../types';
import { formatBytes, formatRelative, resumeStatusColor } from '../../utils/formatters';
import { useDeleteResume, useDownloadResume, useSetPrimary } from '../../hooks/useResume';

interface ResumeCardProps {
  resume: Resume;
  onView?: (resume: Resume) => void;
}

export default function ResumeCard({ resume, onView }: ResumeCardProps) {
  const setPrimary = useSetPrimary();
  const remove = useDeleteResume();
  const download = useDownloadResume();

  return (
    <div className="card group flex flex-col p-5 transition hover:shadow-md">
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-primary-500/10 text-primary-600 dark:text-primary-400">
            <FileText size={20} />
          </div>
          <div className="min-w-0">
            <p className="truncate font-semibold text-gray-900 dark:text-gray-100">
              {resume.originalFileName}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              {formatBytes(resume.fileSizeBytes)} · v{resume.version}
            </p>
          </div>
        </div>
        {resume.primary && (
          <span className="chip bg-primary-500/15 text-primary-600 dark:text-primary-400">
            <Star size={12} className="mr-1 fill-current" /> Primary
          </span>
        )}
      </div>

      <div className="mt-4 flex items-center gap-2">
        <span className={`chip ${resumeStatusColor(resume.status)}`}>{resume.status}</span>
        <span className="text-xs text-gray-400">Uploaded {formatRelative(resume.createdAt)}</span>
      </div>

      <div className="mt-5 flex items-center gap-2 border-t border-gray-100 pt-4 dark:border-gray-800">
        <button
          className="btn-primary flex-1 py-2 text-xs"
          onClick={() => onView?.(resume)}
          disabled={resume.status !== 'ANALYSED'}
        >
          {resume.status === 'ANALYSED' ? 'View Analysis' : 'Analyzing…'}
        </button>
        {!resume.primary && (
          <button
            className="btn-secondary p-2"
            title="Set as primary"
            onClick={() => setPrimary.mutate(resume.id)}
            disabled={setPrimary.isPending}
          >
            <Star size={16} />
          </button>
        )}
        <button
          className="btn-secondary p-2"
          title="Download"
          onClick={() => download.mutate(resume.id)}
          disabled={download.isPending}
        >
          <Download size={16} />
        </button>
        <button
          className="btn-secondary p-2 text-error-600 hover:bg-error-500/10"
          title="Delete"
          onClick={() => {
            if (window.confirm(`Delete "${resume.originalFileName}"?`)) remove.mutate(resume.id);
          }}
          disabled={remove.isPending}
        >
          <Trash2 size={16} />
        </button>
      </div>
    </div>
  );
}
