import { create } from 'zustand';
import type { InterviewFeedback, InterviewQuestion, InterviewSession, WsMessage } from '../types';

interface InterviewState {
  activeSession: InterviewSession | null;
  currentQuestion: InterviewQuestion | null;
  sessionMessages: WsMessage[];
  lastAnswerFeedback: { score: number; feedback: string } | null;
  finalFeedback: InterviewFeedback | null;
  isConnected: boolean;
  isLoading: boolean;
  isComplete: boolean;

  setSession: (session: InterviewSession | null) => void;
  setCurrentQuestion: (question: InterviewQuestion | null) => void;
  addMessage: (message: WsMessage) => void;
  setAnswerFeedback: (feedback: { score: number; feedback: string } | null) => void;
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
  lastAnswerFeedback: null,
  finalFeedback: null,
  isConnected: false,
  isLoading: false,
  isComplete: false,

  setSession: (activeSession) => set({ activeSession }),
  setCurrentQuestion: (currentQuestion) => set({ currentQuestion, isLoading: false }),
  addMessage: (message) => set((s) => ({ sessionMessages: [...s.sessionMessages, message] })),
  setAnswerFeedback: (lastAnswerFeedback) => set({ lastAnswerFeedback }),
  setFinalFeedback: (finalFeedback) => set({ finalFeedback }),
  setConnected: (isConnected) => set({ isConnected }),
  setLoading: (isLoading) => set({ isLoading }),
  setComplete: (isComplete) => set({ isComplete }),

  clearSession: () =>
    set({
      activeSession: null,
      currentQuestion: null,
      sessionMessages: [],
      lastAnswerFeedback: null,
      finalFeedback: null,
      isConnected: false,
      isLoading: false,
      isComplete: false,
    }),
}));
