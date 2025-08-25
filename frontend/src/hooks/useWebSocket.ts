import { useEffect, useState, useCallback } from 'react';
import { webSocketService, WebSocketCallbacks, WebSocketUpdate } from '../services/WebSocketService';

interface UseWebSocketOptions {
  eventId?: string;
  autoConnect?: boolean;
}

interface WebSocketState {
  isConnected: boolean;
  connectionState: string;
  lastUpdate: WebSocketUpdate | null;
  attendanceCount: number;
  registrationCount: number;
  capacityInfo: {
    current: number;
    max: number | null;
    percentage: number | null;
    isNearCapacity: boolean;
  };
}

export const useWebSocket = (options: UseWebSocketOptions = {}) => {
  const { eventId, autoConnect = true } = options;

  const [state, setState] = useState<WebSocketState>({
    isConnected: false,
    connectionState: 'DISCONNECTED',
    lastUpdate: null,
    attendanceCount: 0,
    registrationCount: 0,
    capacityInfo: {
      current: 0,
      max: null,
      percentage: null,
      isNearCapacity: false
    }
  });

  const [notifications, setNotifications] = useState<WebSocketUpdate[]>([]);

  const updateConnectionState = useCallback(() => {
    setState(prev => ({
      ...prev,
      isConnected: webSocketService.isConnected(),
      connectionState: webSocketService.getConnectionState()
    }));
  }, []);

  const addNotification = useCallback((update: WebSocketUpdate) => {
    setNotifications(prev => [...prev.slice(-9), update]); // Keep last 10 notifications
  }, []);

  const callbacks: WebSocketCallbacks = {
    onConnect: () => {
      updateConnectionState();
    },

    onDisconnect: () => {
      updateConnectionState();
    },

    onError: (error) => {
      console.error('WebSocket error:', error);
      updateConnectionState();
    },

    onAttendanceUpdate: (update) => {
      if (!eventId || update.eventId === eventId) {
        setState(prev => ({
          ...prev,
          lastUpdate: update,
          attendanceCount: update.attendanceCount || prev.attendanceCount,
          registrationCount: update.registrationCount || prev.registrationCount
        }));
        addNotification(update);
      }
    },

    onCapacityUpdate: (update) => {
      if (!eventId || update.eventId === eventId) {
        setState(prev => ({
          ...prev,
          lastUpdate: update,
          capacityInfo: {
            current: update.currentCapacity || prev.capacityInfo.current,
            max: update.maxCapacity ?? prev.capacityInfo.max,
            percentage: update.capacityPercentage ?? prev.capacityInfo.percentage,
            isNearCapacity: update.isNearCapacity || false
          }
        }));
        addNotification(update);
      }
    },

    onEventStatusChange: (update) => {
      if (!eventId || update.eventId === eventId) {
        setState(prev => ({
          ...prev,
          lastUpdate: update
        }));
        addNotification(update);
      }
    },

    onRegistrationUpdate: (update) => {
      if (!eventId || update.eventId === eventId) {
        setState(prev => ({
          ...prev,
          lastUpdate: update
        }));
        addNotification(update);
      }
    },

    onCheckInUpdate: (update) => {
      if (!eventId || update.eventId === eventId) {
        setState(prev => ({
          ...prev,
          lastUpdate: update
        }));
        addNotification(update);
      }
    },

    onEventUpdate: (update) => {
      if (!eventId || update.eventId === eventId) {
        setState(prev => ({
          ...prev,
          lastUpdate: update
        }));
        addNotification(update);
      }
    }
  };

  const connect = useCallback(() => {
    webSocketService.connect();
  }, []);

  const disconnect = useCallback(() => {
    webSocketService.disconnect();
  }, []);

  const clearNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  const subscribeToEvent = useCallback((newEventId: string) => {
    webSocketService.subscribeToEvent(newEventId);
  }, []);

  const unsubscribeFromEvent = useCallback(() => {
    webSocketService.unsubscribeFromEvent();
  }, []);

  useEffect(() => {
    webSocketService.setCallbacks(callbacks);

    if (autoConnect) {
      webSocketService.connect();
    }

    if (eventId) {
      webSocketService.subscribeToEvent(eventId);
    }

    // Set up periodic connection state updates
    const interval = setInterval(updateConnectionState, 1000);

    return () => {
      clearInterval(interval);
      if (eventId) {
        webSocketService.unsubscribeFromEvent();
      }
    };
  }, [eventId, autoConnect, updateConnectionState]);

  return {
    ...state,
    notifications,
    connect,
    disconnect,
    clearNotifications,
    subscribeToEvent,
    unsubscribeFromEvent
  };
};

// Specialized hook for real-time event stats
export const useEventStats = (eventId: string) => {
  const webSocket = useWebSocket({ eventId });

  return {
    attendanceCount: webSocket.attendanceCount,
    registrationCount: webSocket.registrationCount,
    capacityInfo: webSocket.capacityInfo,
    isConnected: webSocket.isConnected,
    lastUpdate: webSocket.lastUpdate
  };
};

// Hook for real-time notifications
export const useRealTimeNotifications = () => {
  const webSocket = useWebSocket({ autoConnect: true });

  const getNotificationsByType = useCallback((type: string) => {
    return webSocket.notifications.filter(n => n.type === type);
  }, [webSocket.notifications]);

  return {
    notifications: webSocket.notifications,
    clearNotifications: webSocket.clearNotifications,
    getNotificationsByType,
    isConnected: webSocket.isConnected
  };
};