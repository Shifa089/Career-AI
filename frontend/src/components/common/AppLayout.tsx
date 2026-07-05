import type { ReactNode } from 'react';
import Navbar from './Navbar';
import Sidebar, { MobileNav } from './Sidebar';

interface AppLayoutProps {
  children: ReactNode;
}

/** Authenticated shell: top navbar, left sidebar (desktop), bottom nav (mobile). */
export default function AppLayout({ children }: AppLayoutProps) {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navbar variant="app" />
      <div className="mx-auto flex max-w-[110rem]">
        <Sidebar />
        <main className="min-w-0 flex-1 pb-20 lg:pb-0">
          <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">{children}</div>
        </main>
      </div>
      <MobileNav />
    </div>
  );
}
