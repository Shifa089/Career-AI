import { Component, type ErrorInfo, type ReactNode } from 'react';
import { AlertTriangle } from 'lucide-react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('Uncaught UI error:', error, info.componentStack);
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: undefined });
    window.location.assign('/');
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-gray-50 px-4 text-center dark:bg-gray-950">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-error-500/10">
            <AlertTriangle className="text-error-600" size={32} />
          </div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            Something went wrong
          </h1>
          <p className="max-w-md text-sm text-gray-500 dark:text-gray-400">
            {this.state.error?.message ?? 'An unexpected error occurred. Please try again.'}
          </p>
          <button className="btn-primary mt-2" onClick={this.handleReset}>
            Back to home
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
