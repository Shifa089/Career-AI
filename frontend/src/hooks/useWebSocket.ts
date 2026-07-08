import { useCallback, useEffect, useRef, useState } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import toast from 'react-hot-toast';
import { useAuthStore } from '../store/authStore';
import { useInterviewStore } from '../store/interviewStore';
import type {
  InterviewFeedback,
  InterviewQuestion,
  WsMessage,
  WsMessageType,
} from '../types';

const WS_URL = import.meta.env.VITE_WS_URL || '/ws';

interface UseWebSocketResult {
  isConnected: boolean;
  sendStartSession: () => void;
  sendAnswer: (answer: string, questionId: string) => void;
  sendHint: (partialAnswer: string) => void;
  sendEnd: () => void;
  disconnect: () => void;
}

/**
 * STOMP-over-SockJS client for a single interview session. Subscribes to the
 * per-user queue and routes incoming WsMessages into the interview store.
 */
export function useWebSocket(sessionId: string | undefined): UseWebSocketResult {
  const clientRef = useRef<Client | null>(null);
  const [isConnected, setIsConnected] = useState(false);

  const accessToken = useAuthStore((s) => s.accessToken);
  const {
    addMessage,
    setCurrentQuestion,
    setHint,
    setHintLoading,
    setFinalFeedback,
    setConnected,
    setLoading,
    setComplete,
  } = useInterviewStore();

  const handleMessage = useCallback(
    (frame: IMessage) => {
      let msg: WsMessage;
      try {
        msg = JSON.parse(frame.body) as WsMessage;
      } catch {
        return;
      }
      addMessage(msg);

      switch (msg.type as WsMessageType) {
        case 'QUESTION':
          setCurrentQuestion(msg.payload as InterviewQuestion);
          setLoading(false);
          break;
        case 'HINT': {
          const payload = msg.payload as Record<string, unknown>;
          if ('hint' in payload) {
            setHint(String(payload.hint ?? ''));
          }
          setHintLoading(false);
          break;
        }
        case 'SESSION_COMPLETE':
          setFinalFeedback(msg.payload as InterviewFeedback);
          setComplete(true);
          setLoading(false);
          setHintLoading(false);
          break;
        case 'ERROR': {
          // Surface the failure instead of silently dropping the user back onto the same question.
          const payload = (msg.payload ?? {}) as Record<string, unknown>;
          const message =
            typeof payload.message === 'string' && payload.message
              ? payload.message
              : 'Something went wrong. Please try again.';
          toast.error(message);
          setLoading(false);
          setHintLoading(false);
          break;
        }
      }
    },
    [addMessage, setCurrentQuestion, setHint, setHintLoading, setFinalFeedback, setComplete, setLoading],
  );

  useEffect(() => {
    if (!sessionId || !accessToken) return;

    const client = new Client({
      // SockJS handles the transport; disable the native brokerURL.
      webSocketFactory: () => new SockJS(WS_URL) as WebSocket,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setIsConnected(true);
        setConnected(true);
        client.subscribe(`/user/queue/interview/${sessionId}`, handleMessage);
      },
      onDisconnect: () => {
        setIsConnected(false);
        setConnected(false);
      },
      onWebSocketClose: () => {
        setIsConnected(false);
        setConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      setIsConnected(false);
      setConnected(false);
    };
  }, [sessionId, accessToken, handleMessage, setConnected]);

  const publish = useCallback(
    (suffix: string, body: unknown = {}) => {
      const client = clientRef.current;
      if (!client || !client.connected || !sessionId) return;
      client.publish({
        destination: `/app/interview/${sessionId}/${suffix}`,
        body: JSON.stringify(body),
      });
    },
    [sessionId],
  );

  const sendStartSession = useCallback(() => {
    setLoading(true);
    publish('start', { type: 'START_SESSION' });
  }, [publish, setLoading]);

  const sendAnswer = useCallback(
    (answer: string, questionId: string) => {
      setLoading(true);
      publish('answer', {
        type: 'SUBMIT_ANSWER',
        content: answer,
        metadata: { questionId },
      });
    },
    [publish, setLoading],
  );

  const sendHint = useCallback(
    (partialAnswer: string) => {
      setHintLoading(true);
      publish('hint', { type: 'REQUEST_HINT', content: partialAnswer });
    },
    [publish, setHintLoading],
  );

  const sendEnd = useCallback(() => {
    publish('end', { type: 'END_SESSION' });
  }, [publish]);

  const disconnect = useCallback(() => {
    clientRef.current?.deactivate();
    clientRef.current = null;
    setIsConnected(false);
    setConnected(false);
  }, [setConnected]);

  return { isConnected, sendStartSession, sendAnswer, sendHint, sendEnd, disconnect };
}
