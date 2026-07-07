import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import EmployerLayout from '../components/common/EmployerLayout';
import { usePostJob } from '../hooks/useCompany';

const JOB_TYPES = ['REMOTE', 'HYBRID', 'ONSITE'];
const EXPERIENCE_LEVELS = ['JUNIOR', 'MID', 'SENIOR', 'LEAD', 'PRINCIPAL'];

function toSkillList(raw: string): string[] {
  return raw
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
}

export default function PostJobPage() {
  const navigate = useNavigate();
  const postJob = usePostJob();

  const [title, setTitle] = useState('');
  const [company, setCompany] = useState('');
  const [location, setLocation] = useState('');
  const [jobType, setJobType] = useState('REMOTE');
  const [experienceLevel, setExperienceLevel] = useState('MID');
  const [salaryRange, setSalaryRange] = useState('');
  const [descriptionText, setDescriptionText] = useState('');
  const [requiredSkills, setRequiredSkills] = useState('');
  const [niceToHaveSkills, setNiceToHaveSkills] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    postJob.mutate({
      title: title.trim(),
      company: company.trim(),
      location: location.trim() || undefined,
      jobType,
      experienceLevel,
      salaryRange: salaryRange.trim() || undefined,
      descriptionText: descriptionText.trim(),
      requiredSkills: toSkillList(requiredSkills),
      niceToHaveSkills: toSkillList(niceToHaveSkills),
    });
  };

  return (
    <EmployerLayout>
      <button
        onClick={() => navigate('/employer/dashboard')}
        className="mb-4 flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400"
      >
        <ArrowLeft size={16} /> Back to dashboard
      </button>

      <div className="mx-auto max-w-2xl">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Post a job</h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Candidates will be matched to this role by semantic similarity and shown their skill gaps.
        </p>

        <form onSubmit={handleSubmit} className="card mt-6 space-y-4 p-6">
          <div>
            <label htmlFor="title" className="label">
              Job title
            </label>
            <input
              id="title"
              className="input"
              placeholder="Senior Backend Engineer"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
            />
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label htmlFor="company" className="label">
                Company
              </label>
              <input
                id="company"
                className="input"
                placeholder="Acme Inc."
                value={company}
                onChange={(e) => setCompany(e.target.value)}
                required
              />
            </div>
            <div>
              <label htmlFor="location" className="label">
                Location
              </label>
              <input
                id="location"
                className="input"
                placeholder="Remote · London"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <div>
              <label htmlFor="jobType" className="label">
                Work type
              </label>
              <select
                id="jobType"
                className="input"
                value={jobType}
                onChange={(e) => setJobType(e.target.value)}
              >
                {JOB_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t.charAt(0) + t.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="experienceLevel" className="label">
                Experience
              </label>
              <select
                id="experienceLevel"
                className="input"
                value={experienceLevel}
                onChange={(e) => setExperienceLevel(e.target.value)}
              >
                {EXPERIENCE_LEVELS.map((l) => (
                  <option key={l} value={l}>
                    {l.charAt(0) + l.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="salaryRange" className="label">
                Salary range
              </label>
              <input
                id="salaryRange"
                className="input"
                placeholder="80k-110k"
                value={salaryRange}
                onChange={(e) => setSalaryRange(e.target.value)}
              />
            </div>
          </div>

          <div>
            <label htmlFor="description" className="label">
              Job description
            </label>
            <textarea
              id="description"
              className="input min-h-[160px] resize-none leading-relaxed"
              placeholder="Describe the role, responsibilities, and what you're looking for…"
              value={descriptionText}
              onChange={(e) => setDescriptionText(e.target.value)}
              required
            />
          </div>

          <div>
            <label htmlFor="requiredSkills" className="label">
              Required skills <span className="text-gray-400">(comma-separated)</span>
            </label>
            <input
              id="requiredSkills"
              className="input"
              placeholder="Java, Spring Boot, PostgreSQL"
              value={requiredSkills}
              onChange={(e) => setRequiredSkills(e.target.value)}
            />
          </div>

          <div>
            <label htmlFor="niceToHaveSkills" className="label">
              Nice-to-have skills <span className="text-gray-400">(comma-separated)</span>
            </label>
            <input
              id="niceToHaveSkills"
              className="input"
              placeholder="Kafka, Kubernetes"
              value={niceToHaveSkills}
              onChange={(e) => setNiceToHaveSkills(e.target.value)}
            />
          </div>

          <button type="submit" className="btn-primary w-full" disabled={postJob.isPending}>
            {postJob.isPending ? 'Posting…' : 'Post Job'}
          </button>
        </form>
      </div>
    </EmployerLayout>
  );
}
