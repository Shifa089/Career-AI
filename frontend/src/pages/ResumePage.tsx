import { Fragment, useState } from 'react';
import { Dialog, Transition } from '@headlessui/react';
import { FileText, X } from 'lucide-react';
import AppLayout from '../components/common/AppLayout';
import PageHeader from '../components/common/PageHeader';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ResumeUploader from '../components/resume/ResumeUploader';
import ResumeCard from '../components/resume/ResumeCard';
import AnalysisResults from '../components/resume/AnalysisResults';
import { useResumes } from '../hooks/useResume';
import type { Resume } from '../types';

export default function ResumePage() {
  const { data: resumes, isLoading } = useResumes();
  const [selected, setSelected] = useState<Resume | null>(null);

  return (
    <AppLayout>
      <PageHeader
        title="Resumes"
        subtitle="Upload a resume to get an AI-powered ATS analysis."
        icon={<FileText size={22} />}
      />

      <div className="mb-8">
        <ResumeUploader onUploaded={(r) => setSelected(r)} />
      </div>

      {isLoading ? (
        <LoadingSpinner fullScreen={false} className="py-16" label="Loading resumes…" />
      ) : !resumes || resumes.length === 0 ? (
        <div className="card p-12 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-gray-100 text-gray-400 dark:bg-gray-800">
            <FileText size={26} />
          </div>
          <p className="font-medium text-gray-700 dark:text-gray-200">No resumes yet</p>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Drop a PDF or DOCX above to get started.
          </p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {resumes.map((resume) => (
            <ResumeCard key={resume.id} resume={resume} onView={setSelected} />
          ))}
        </div>
      )}

      {/* Analysis slide-over */}
      <Transition show={Boolean(selected)} as={Fragment}>
        <Dialog onClose={() => setSelected(null)} className="relative z-50">
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
                    <Dialog.Title className="text-lg font-bold text-gray-900 dark:text-gray-100">
                      Resume Analysis
                    </Dialog.Title>
                    <button
                      onClick={() => setSelected(null)}
                      className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800"
                    >
                      <X size={20} />
                    </button>
                  </div>
                  {selected && <AnalysisResults resume={selected} />}
                </Dialog.Panel>
              </Transition.Child>
            </div>
          </div>
        </Dialog>
      </Transition>
    </AppLayout>
  );
}
