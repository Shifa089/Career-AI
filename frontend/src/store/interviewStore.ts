import { create } from 'zustand';
import type { InterviewFeedback, InterviewQuestion, InterviewSession, WsMessage } from '../types';

interface InterviewState {
  activeSession: InterviewSession | null;
  currentQuestion: InterviewQuestion | null;
  sessionMessages: WsMessage[];
  hint: string | null;
  finalFeedback: InterviewFeedback | null;
  isConnected: boolean;
  isLoading: boolean;
  isComplete: boolean;

  setSession: (session: InterviewSession | null) => void;
  setCurrentQuestion: (question: InterviewQuestion | null) => void;
  addMessage: (message: WsMessage) => void;
  setHint: (hint: string | null) => void;
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
  finalFeedback: null,
  isConnected: false,
  isLoading: false,
  isComplete: false,

  setSession: (activeSession) => set({ activeSession }),
  // A fresh question clears any hint shown for the previous question.
  setCurrentQuestion: (currentQuestion) => set({ currentQuestion, hint: null, isLoading: false }),
  addMessage: (message) => set((s) => ({ sessionMessages: [...s.sessionMessages, message] })),
  setHint: (hint) => set({ hint }),
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
      finalFeedback: null,
      isConnected: false,
      isLoading: false,
      isComplete: false,
    }),
}));
