import { create } from 'zustand';
import type { InterviewFeedback, InterviewQuestion, InterviewSession, WsMessage } from '../types';

interface InterviewState {
  activeSession: InterviewSession | null;
  currentQuestion: InterviewQuestion | null;
  sessionMessages: WsMessage[];
  hint: string | null;
  isHintLoading: boolean;
  finalFeedback: InterviewFeedback | null;
  isConnected: boolean;
  isLoading: boolean;
  isComplete: boolean;

  setSession: (session: InterviewSession | null) => void;
  setCurrentQuestion: (question: InterviewQuestion | null) => void;
  addMessage: (message: WsMessage) => void;
  setHint: (hint: string | null) => void;
  setHintLoading: (isHintLoading: boolean) => void;
  setFinalFeedback: (feedback: InterviewFeedback | null) => void;
  setConnected: (isConnected: boolean) => void;
  setLoading: (isLoading: boolean) => void;
  setComplete: (isComplete: boolean) => void;
  clearSession: () => void;
}

export const useInterviewStore = create<InterviewState>((set) => ({
  activeSession: null,
  currentQuestion: null,
  sessionMessages: [],
  hint: null,
  isHintLoading: false,
  finalFeedback: null,
  isConnected: false,
  isLoading: false,
  isComplete: false,

  setSession: (activeSession) => set({ activeSession }),
  // A fresh question clears any hint (and pending hint request) shown for the previous question.
  setCurrentQuestion: (currentQuestion) =>
    set({ currentQuestion, hint: null, isHintLoading: false, isLoading: false }),
  addMessage: (message) => set((s) => ({ sessionMessages: [...s.sessionMessages, message] })),
  setHint: (hint) => set({ hint }),
  setHintLoading: (isHintLoading) => set({ isHintLoading }),
  setFinalFeedback: (finalFeedback) => set({ finalFeedback }),
  setConnected: (isConnected) => set({ isConnected }),
  setLoading: (isLoading) => set({ isLoading }),
  setComplete: (isComplete) => set({ isComplete }),

  clearSession: () =>
    set({
      activeSession: null,
      currentQuestion: null,
      sessionMessages: [],
      hint: null,
      isHintLoading: false,
      finalFeedback: null,
      isConnected: false,
      isLoading: false,
      isComplete: false,
    }),
}));
