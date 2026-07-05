import { Dialog, Transition } from '@headlessui/react';
import { Fragment, useState } from 'react';
import { X } from 'lucide-react';
import { useCreateSession } from '../../hooks/useInterview';
import type { InterviewType } from '../../types';
import { formatEnum } from '../../utils/formatters';

interface SessionSetupProps {
  open: boolean;
  onClose: () => void;
}

const TYPES: InterviewType[] = ['TECHNICAL', 'BEHAVIOURAL', 'MIXED', 'SYSTEM_DESIGN'];

export default function SessionSetup({ open, onClose }: SessionSetupProps) {
  const [jobTitle, setJobTitle] = useState('');
  const [targetCompany, setTargetCompany] = useState('');
  const [type, setType] = useState<InterviewType>('MIXED');
  const [totalQuestions, setTotalQuestions] = useState(10);
  const createSession = useCreateSession();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!jobTitle.trim()) return;
    createSession.mutate(
      {
        jobTitle: jobTitle.trim(),
        targetCompany: targetCompany.trim() || undefined,
        type,
        totalQuestions,
      },
      { onSuccess: onClose },
    );
  };

  return (
    <Transition appear show={open} as={Fragment}>
      <Dialog as="div" className="relative z-50" onClose={onClose}>
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

        <div className="fixed inset-0 overflow-y-auto">
          <div className="flex min-h-full items-center justify-center p-4">
            <Transition.Child
              as={Fragment}
              enter="ease-out duration-200"
              enterFrom="opacity-0 scale-95"
              enterTo="opacity-100 scale-100"
              leave="ease-in duration-150"
              leaveFrom="opacity-100 scale-100"
              leaveTo="opacity-0 scale-95"
            >
              <Dialog.Panel className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl dark:bg-gray-900">
                <div className="mb-5 flex items-center justify-between">
                  <Dialog.Title className="text-xl font-bold text-gray-900 dark:text-gray-100">
                    Start New Interview
                  </Dialog.Title>
                  <button
                    onClick={onClose}
                    className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800"
                  >
                    <X size={20} />
                  </button>
                </div>

                <form onSubmit={handleSubmit} className="space-y-5">
                  <div>
                    <label htmlFor="jobTitle" className="label">
                      Job title <span className="text-error-500">*</span>
                    </label>
                    <input
                      id="jobTitle"
                      className="input"
                      placeholder="e.g. Senior Frontend Engineer"
                      value={jobTitle}
                      onChange={(e) => setJobTitle(e.target.value)}
                      required
                      autoFocus
                    />
                  </div>

                  <div>
                    <label htmlFor="targetCompany" className="label">
                      Target company <span className="text-gray-400">(optional)</span>
                    </label>
                    <input
                      id="targetCompany"
                      className="input"
                      placeholder="e.g. Stripe"
                      value={targetCompany}
                      onChange={(e) => setTargetCompany(e.target.value)}
                    />
                  </div>

                  <div>
                    <label className="label">Interview type</label>
                    <div className="grid grid-cols-2 gap-2">
                      {TYPES.map((t) => (
                        <button
                          key={t}
                          type="button"
                          onClick={() => setType(t)}
                          className={`rounded-lg border px-3 py-2.5 text-sm font-medium transition ${
                            type === t
                              ? 'border-primary-500 bg-primary-50 text-primary-700 dark:bg-primary-500/10 dark:text-primary-400'
                              : 'border-gray-200 text-gray-600 hover:border-gray-300 dark:border-gray-700 dark:text-gray-300'
                          }`}
                        >
                          {formatEnum(t)}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div>
                    <label htmlFor="totalQuestions" className="label">
                      Number of questions:{' '}
                      <span className="font-semibold text-primary-600 dark:text-primary-400">
                        {totalQuestions}
                      </span>
                    </label>
                    <input
                      id="totalQuestions"
                      type="range"
                      min={5}
                      max={20}
                      value={totalQuestions}
                      onChange={(e) => setTotalQuestions(Number(e.target.value))}
                      className="w-full accent-primary-600"
                    />
                    <div className="flex justify-between text-xs text-gray-400">
                      <span>5</span>
                      <span>20</span>
                    </div>
                  </div>

                  <button
                    type="submit"
                    className="btn-primary w-full"
                    disabled={createSession.isPending || !jobTitle.trim()}
                  >
                    {createSession.isPending ? 'Creating…' : 'Start Interview'}
                  </button>
                </form>
              </Dialog.Panel>
            </Transition.Child>
          </div>
        </div>
      </Dialog>
    </Transition>
  );
}
