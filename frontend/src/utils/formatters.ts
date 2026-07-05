import { format, formatDistanceToNow, parseISO } from 'date-fns';
import type { MatchStatus, ResumeStatus, SessionStatus, User } from '../types';

/** Join a user's first/last name into a single display string. */
export function fullNameOf(
  user: Pick<User, 'firstName' | 'lastName'> | null | undefined,
): string {
  if (!user) return '';
  return `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim();
}

/** Safely parse an ISO string (backend LocalDateTime has no zone → treat as local). */
function toDate(value: string | number | Date | null | undefined): Date | null {
  if (value == null) return null;
  if (value instanceof Date) return value;
  if (typeof value === 'number') return new Date(value);
  try {
    return parseISO(value);
  } catch {
    return null;
  }
}

export function formatDate(value: string | Date | null | undefined, pattern = 'MMM d, yyyy'): string {
  const d = toDate(value);
  return d ? format(d, pattern) : '—';
}

export function formatDateTime(value: string | Date | null | undefined): string {
  const d = toDate(value);
  return d ? format(d, "MMM d, yyyy 'at' h:mm a") : '—';
}

export function formatRelative(value: string | Date | null | undefined): string {
  const d = toDate(value);
  return d ? formatDistanceToNow(d, { addSuffix: true }) : '—';
}

/** Round a 0–100 score for display; returns em-dash for nullish. */
export function formatScore(score: number | null | undefined): string {
  if (score == null || Number.isNaN(score)) return '—';
  return `${Math.round(score)}`;
}

export function formatPercent(value: number | null | undefined): string {
  if (value == null || Number.isNaN(value)) return '—';
  return `${Math.round(value)}%`;
}

export function formatBytes(bytes: number | null | undefined): string {
  if (!bytes) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
}

/** Convert an UPPER_SNAKE enum to Title Case ("SYSTEM_DESIGN" → "System Design"). */
export function formatEnum(value: string | null | undefined): string {
  if (!value) return '—';
  return value
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}

export function initialsFromName(name: string | null | undefined): string {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export const MATCH_STATUS_LABELS: Record<MatchStatus, string> = {
  PENDING_REVIEW: 'Pending Review',
  SAVED: 'Saved',
  APPLIED: 'Applied',
  REJECTED: 'Rejected',
};

export function matchStatusLabel(status: MatchStatus | string): string {
  return MATCH_STATUS_LABELS[status as MatchStatus] ?? formatEnum(status);
}

/** Tailwind classes for a score band (0–100). */
export function scoreColor(score: number | null | undefined): {
  text: string;
  bg: string;
  ring: string;
  hex: string;
} {
  const s = score ?? 0;
  if (s <= 40) return { text: 'text-error-600', bg: 'bg-error-500', ring: 'ring-error-500', hex: '#EF4444' };
  if (s <= 70) return { text: 'text-warning-600', bg: 'bg-warning-500', ring: 'ring-warning-500', hex: '#F59E0B' };
  return { text: 'text-success-600', bg: 'bg-success-500', ring: 'ring-success-500', hex: '#10B981' };
}

export function difficultyColor(difficulty: string | null | undefined): string {
  switch ((difficulty ?? '').toUpperCase()) {
    case 'EASY':
      return 'bg-success-500/15 text-success-600 dark:text-success-500';
    case 'HARD':
      return 'bg-error-500/15 text-error-600 dark:text-error-500';
    case 'MEDIUM':
    default:
      return 'bg-warning-500/15 text-warning-600 dark:text-warning-500';
  }
}

export function resumeStatusColor(status: ResumeStatus): string {
  switch (status) {
    case 'ANALYSED':
      return 'bg-success-500/15 text-success-600 dark:text-success-500';
    case 'FAILED':
      return 'bg-error-500/15 text-error-600 dark:text-error-500';
    case 'PROCESSING':
    case 'UPLOADED':
    default:
      return 'bg-warning-500/15 text-warning-600 dark:text-warning-500';
  }
}

export function sessionStatusColor(status: SessionStatus): string {
  switch (status) {
    case 'COMPLETED':
      return 'bg-success-500/15 text-success-600 dark:text-success-500';
    case 'ACTIVE':
      return 'bg-primary-500/15 text-primary-600 dark:text-primary-400';
    case 'ABANDONED':
      return 'bg-error-500/15 text-error-600 dark:text-error-500';
    case 'PAUSED':
    case 'CREATED':
    default:
      return 'bg-gray-500/15 text-gray-600 dark:text-gray-400';
  }
}
