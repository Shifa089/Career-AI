import { Link } from 'react-router-dom';
import { ArrowRight, Briefcase, FileText, MessageSquare, Sparkles, Star } from 'lucide-react';
import Navbar from '../components/common/Navbar';

const FEATURES = [
  {
    icon: FileText,
    title: 'Resume Analysis',
    description:
      'Get an instant ATS compatibility score, extracted skills, and AI-powered suggestions to make your resume stand out.',
    accent: 'from-primary-500 to-primary-700',
  },
  {
    icon: MessageSquare,
    title: 'Mock Interviews',
    description:
      'Practice with a real-time AI interviewer tailored to your target role, and get scored feedback on every answer.',
    accent: 'from-secondary-500 to-secondary-600',
  },
  {
    icon: Briefcase,
    title: 'Smart Job Matching',
    description:
      'Semantic matching surfaces the roles that fit you best, with a clear skill-gap breakdown and learning path.',
    accent: 'from-accent-500 to-accent-600',
  },
];

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-white dark:bg-gray-950">
      <Navbar variant="public" />

      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-b from-primary-50/60 to-transparent dark:from-primary-500/5" />
        <div className="relative mx-auto max-w-7xl px-4 py-24 text-center sm:px-6 lg:px-8 lg:py-32">
          <span className="mb-6 inline-flex items-center gap-2 rounded-full border border-primary-200 bg-primary-50 px-4 py-1.5 text-sm font-medium text-primary-700 dark:border-primary-500/30 dark:bg-primary-500/10 dark:text-primary-300">
            <Sparkles size={15} /> Powered by Claude AI
          </span>
          <h1 className="mx-auto max-w-4xl text-4xl font-extrabold tracking-tight text-gray-900 sm:text-5xl lg:text-6xl dark:text-white">
            Land Your Dream Job with{' '}
            <span className="bg-gradient-to-r from-primary-600 to-accent-500 bg-clip-text text-transparent">
              AI-Powered Interview Prep
            </span>
          </h1>
          <p className="mx-auto mt-6 max-w-2xl text-lg leading-relaxed text-gray-600 dark:text-gray-300">
            CareerAI analyzes your resume, runs realistic mock interviews, and matches you to the
            right roles — so you walk in prepared and confident.
          </p>
          <div className="mt-10 flex flex-col items-center justify-center gap-3 sm:flex-row">
            <Link to="/register" className="btn-primary px-6 py-3 text-base">
              Get Started Free <ArrowRight size={18} />
            </Link>
            <a href="#features" className="btn-secondary px-6 py-3 text-base">
              See Demo
            </a>
          </div>
          <div className="mt-8 flex items-center justify-center gap-1 text-sm text-gray-500 dark:text-gray-400">
            {[...Array(5)].map((_, i) => (
              <Star key={i} size={16} className="fill-warning-400 text-warning-400" />
            ))}
            <span className="ml-2">Trusted by ambitious job seekers</span>
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div className="mb-14 text-center">
          <h2 className="text-3xl font-bold tracking-tight text-gray-900 dark:text-white">
            Everything you need to get hired
          </h2>
          <p className="mt-3 text-gray-600 dark:text-gray-400">
            Three AI-powered tools that work together to sharpen your job search.
          </p>
        </div>
        <div className="grid gap-6 md:grid-cols-3">
          {FEATURES.map(({ icon: Icon, title, description, accent }) => (
            <div key={title} className="card group p-8 transition hover:-translate-y-1 hover:shadow-lg">
              <div
                className={`mb-5 flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br ${accent} text-white`}
              >
                <Icon size={26} />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 dark:text-gray-100">{title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-gray-600 dark:text-gray-400">
                {description}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* CTA */}
      <section className="mx-auto max-w-7xl px-4 pb-24 sm:px-6 lg:px-8">
        <div className="overflow-hidden rounded-3xl bg-gradient-to-r from-primary-600 to-accent-500 px-8 py-16 text-center text-white">
          <h2 className="text-3xl font-bold">Ready to ace your next interview?</h2>
          <p className="mx-auto mt-3 max-w-xl text-primary-50">
            Create a free account and get your first resume analysis in minutes.
          </p>
          <Link
            to="/register"
            className="mt-8 inline-flex items-center gap-2 rounded-lg bg-white px-6 py-3 text-base font-semibold text-primary-700 transition hover:bg-primary-50"
          >
            Get Started Free <ArrowRight size={18} />
          </Link>
        </div>
      </section>

      <footer className="border-t border-gray-100 py-8 text-center text-sm text-gray-400 dark:border-gray-800">
        © {new Date().getFullYear()} CareerAI. Built with React & Claude.
      </footer>
    </div>
  );
}
