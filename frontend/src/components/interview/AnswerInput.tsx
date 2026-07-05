import { useState } from 'react';
import { Lightbulb, Send } from 'lucide-react';

interface AnswerInputProps {
  onSubmit: (answer: string) => void;
  onHint: (partialAnswer: string) => void;
  disabled?: boolean;
  isSubmitting?: boolean;
}

export default function AnswerInput({ onSubmit, onHint, disabled, isSubmitting }: AnswerInputProps) {
  const [answer, setAnswer] = useState('');

  const handleSubmit = () => {
    const trimmed = answer.trim();
    if (!trimmed) return;
    onSubmit(trimmed);
    setAnswer('');
  };

  return (
    <div className="flex h-full flex-col">
      <label htmlFor="answer" className="label">
        Your answer
      </label>
      <textarea
        id="answer"
        className="input min-h-[220px] flex-1 resize-none leading-relaxed"
        placeholder="Structure your answer clearly — think out loud, use examples…"
        value={answer}
        onChange={(e) => setAnswer(e.target.value)}
        disabled={disabled}
        onKeyDown={(e) => {
          if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') handleSubmit();
        }}
      />
      <div className="mt-2 flex items-center justify-between">
        <span className="text-xs text-gray-400">{answer.trim().length} chars · ⌘/Ctrl+Enter to submit</span>
      </div>

      <div className="mt-4 flex gap-2">
        <button
          type="button"
          className="btn-secondary"
          onClick={() => onHint(answer)}
          disabled={disabled}
        >
          <Lightbulb size={16} /> Get Hint
        </button>
        <button
          type="button"
          className="btn-primary flex-1"
          onClick={handleSubmit}
          disabled={disabled || isSubmitting || !answer.trim()}
        >
          <Send size={16} /> {isSubmitting ? 'Evaluating…' : 'Submit Answer'}
        </button>
      </div>
    </div>
  );
}
