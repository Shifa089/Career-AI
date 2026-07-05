import { useCallback, useState } from 'react';
import { useDropzone, type FileRejection } from 'react-dropzone';
import { FileText, UploadCloud, X } from 'lucide-react';
import toast from 'react-hot-toast';
import { useUploadResume } from '../../hooks/useResume';
import { formatBytes } from '../../utils/formatters';
import type { Resume } from '../../types';

const ACCEPTED = {
  'application/pdf': ['.pdf'],
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
  'application/msword': ['.doc'],
};
const MAX_SIZE = 10 * 1024 * 1024; // 10 MB

interface ResumeUploaderProps {
  onUploaded?: (resume: Resume) => void;
}

export default function ResumeUploader({ onUploaded }: ResumeUploaderProps) {
  const [file, setFile] = useState<File | null>(null);
  const [targetRole, setTargetRole] = useState('');
  const [progress, setProgress] = useState(0);
  const upload = useUploadResume();

  const onDrop = useCallback((accepted: File[], rejections: FileRejection[]) => {
    if (rejections.length > 0) {
      const reason = rejections[0].errors[0]?.message ?? 'Invalid file';
      toast.error(reason);
      return;
    }
    if (accepted[0]) setFile(accepted[0]);
  }, []);

  const { getRootProps, getInputProps, isDragActive, isDragReject } = useDropzone({
    onDrop,
    accept: ACCEPTED,
    maxSize: MAX_SIZE,
    multiple: false,
  });

  const handleUpload = () => {
    if (!file) return;
    setProgress(0);
    upload.mutate(
      { file, targetRole: targetRole.trim() || undefined, onProgress: setProgress },
      {
        onSuccess: (resume) => {
          setFile(null);
          setTargetRole('');
          setProgress(0);
          onUploaded?.(resume);
        },
      },
    );
  };

  return (
    <div className="card p-6">
      {!file ? (
        <div
          {...getRootProps()}
          className={`flex cursor-pointer flex-col items-center justify-center gap-3 rounded-xl border-2 border-dashed px-6 py-12 text-center transition ${
            isDragReject
              ? 'border-error-400 bg-error-500/5'
              : isDragActive
                ? 'border-primary-500 bg-primary-50 dark:bg-primary-500/10'
                : 'border-gray-300 hover:border-primary-400 hover:bg-gray-50 dark:border-gray-700 dark:hover:bg-gray-800/50'
          }`}
        >
          <input {...getInputProps()} />
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-primary-500/10 text-primary-600 dark:text-primary-400">
            <UploadCloud size={26} />
          </div>
          <div>
            <p className="font-semibold text-gray-900 dark:text-gray-100">
              {isDragActive ? 'Drop your resume here' : 'Drag & drop your resume'}
            </p>
            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
              or <span className="text-primary-600 dark:text-primary-400">browse files</span> — PDF or
              DOCX, up to 10&nbsp;MB
            </p>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="flex items-center gap-3 rounded-lg border border-gray-200 bg-gray-50 p-3 dark:border-gray-700 dark:bg-gray-800/50">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-500/10 text-primary-600 dark:text-primary-400">
              <FileText size={20} />
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">
                {file.name}
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-400">{formatBytes(file.size)}</p>
            </div>
            {!upload.isPending && (
              <button
                onClick={() => setFile(null)}
                className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-200 hover:text-gray-600 dark:hover:bg-gray-700"
                aria-label="Remove file"
              >
                <X size={18} />
              </button>
            )}
          </div>

          <div>
            <label htmlFor="targetRole" className="label">
              Target role <span className="text-gray-400">(optional)</span>
            </label>
            <input
              id="targetRole"
              className="input"
              placeholder="e.g. Senior Backend Engineer"
              value={targetRole}
              onChange={(e) => setTargetRole(e.target.value)}
              maxLength={150}
              disabled={upload.isPending}
            />
          </div>

          {upload.isPending && (
            <div>
              <div className="mb-1 flex justify-between text-xs text-gray-500 dark:text-gray-400">
                <span>Uploading…</span>
                <span>{progress}%</span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
                <div
                  className="h-full rounded-full bg-primary-600 transition-all"
                  style={{ width: `${progress}%` }}
                />
              </div>
            </div>
          )}

          <button
            className="btn-primary w-full"
            onClick={handleUpload}
            disabled={upload.isPending}
          >
            {upload.isPending ? 'Uploading…' : 'Upload & Analyze'}
          </button>
        </div>
      )}
    </div>
  );
}
