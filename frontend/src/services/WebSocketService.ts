import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface WebSocketMessage {
  type: string;
  eventId?: string;
  timestamp: number;
  [key: string]: any;
}

export interface WebSocketSubscription {
  id: string;
  destination: string;
  callback: (message: WebSocketMessage) => void;
  unsubscribe: () => void;
}

export interface WebSocketCallbacks {
  onAttendanceUpdate?: (update: WebSocketMessage) => void;
  onCapacityUpdate?: (update: WebSocketMessage) => void;
  onEventStatusChange?: (update: WebSocketMessage) => void;
  onRegistrationUpdate?: (update: WebSocketMessage) => void;
  onCheckInUpdate?: (update: WebSocketMessage) => void;
  onEventUpdate?: (update: WebSocketMessage) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: any) => void;
}

class WebSocketService {
  private client: Client | null = null;
  private eventId: string | null = null;
  private callbacks: WebSocketCallbacks = {};
  private subscriptions = new Map<string, WebSocketSubscription>();
  private connectionPromise: Promise<void> | null = null;
  private isConnected = false;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000;

  constructor() {
    this.setupClient();
  }

  private setupClient() {
    this.client = new Client({
      webSocketFactory: () => new SockJS(`${process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080'}/ws`),
      connectHeaders: {},
      debug: (str) => {
        if (process.env.NODE_ENV === 'development') {
          console.log('WebSocket Debug:', str);
        }
      },
      reconnectDelay: this.reconnectDelay,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: (frame) => {
        console.log('WebSocket connected:', frame);
        this.isConnected = true;
        this.reconnectAttempts = 0;
        this.callbacks.onConnect?.();
        this.resubscribeAll();
        this.subscribeToGlobalUpdates();
        if (this.eventId) {
          this.subscribeToEvent(this.eventId);
        }
      },
      onStompError: (frame) => {
        console.error('WebSocket STOMP error:', frame);
        this.isConnected = false;
        this.callbacks.onError?.(frame);
      },
      onWebSocketError: (event) => {
        console.error('WebSocket error:', event);
        this.isConnected = false;
        this.callbacks.onError?.(event);
      },
      onWebSocketClose: (event) => {
        console.log('WebSocket closed:', event);
        this.isConnected = false;
        this.callbacks.onDisconnect?.();
        this.handleReconnection();
      }
    });
  }

  private resubscribeAll() {
    this.subscriptions.forEach((subscription, id) => {
      if (this.client && this.client.connected) {
        const stompSubscription = this.client.subscribe(
          subscription.destination,
          (message) => {
            try {
              const parsedMessage: WebSocketMessage = JSON.parse(message.body);
              subscription.callback(parsedMessage);
            } catch (error) {
              console.error('Error parsing WebSocket message:', error);
            }
          }
        );

        // Update the unsubscribe function
        subscription.unsubscribe = () => {
          stompSubscription.unsubscribe();
          this.subscriptions.delete(id);
        };
      }
    });
  }

  private handleReconnection() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      setTimeout(() => {
        this.connect();
      }, this.reconnectDelay * this.reconnectAttempts);
    } else {
      console.error('Max reconnection attempts reached');
    }
  }

  async connect(): Promise<void> {
    if (this.isConnected) {
      return Promise.resolve();
    }

    if (this.connectionPromise) {
      return this.connectionPromise;
    }

    this.connectionPromise = new Promise<void>((resolve, reject) => {
      if (!this.client) {
        this.setupClient();
      }

      if (this.client) {
        const originalOnConnect = this.client.onConnect;
        this.client.onConnect = (frame) => {
          if (originalOnConnect) {
            originalOnConnect(frame);
          }
          this.connectionPromise = null;
          resolve();
        };

        const originalOnStompError = this.client.onStompError;
        this.client.onStompError = (frame) => {
          if (originalOnStompError) {
            originalOnStompError(frame);
          }
          this.connectionPromise = null;
          reject(new Error(`WebSocket connection failed: ${frame.headers['message']}`));
        };

        this.client.activate();
      } else {
        this.connectionPromise = null;
        reject(new Error('Failed to create WebSocket client'));
      }
    });

    return this.connectionPromise;
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.isConnected = false;
      this.subscriptions.clear();
      this.connectionPromise = null;
      this.reconnectAttempts = 0;
    }
  }

  setCallbacks(callbacks: WebSocketCallbacks) {
    this.callbacks = { ...this.callbacks, ...callbacks };
  }

  subscribeToEvent(eventId: string) {
    if (!this.client || !this.client.connected) {
      console.warn('WebSocket not connected, storing eventId for later subscription');
      this.eventId = eventId;
      return;
    }

    this.eventId = eventId;

    // Subscribe to event-specific topics
    this.client.subscribe(`/topic/events/${eventId}/attendance`, (message) => {
      const update = JSON.parse(message.body);
      this.callbacks.onAttendanceUpdate?.(update);
    });

    this.client.subscribe(`/topic/events/${eventId}/capacity`, (message) => {
      const update = JSON.parse(message.body);
      this.callbacks.onCapacityUpdate?.(update);
    });

    this.client.subscribe(`/topic/events/${eventId}/status`, (message) => {
      const update = JSON.parse(message.body);
      this.callbacks.onEventStatusChange?.(update);
    });

    this.client.subscribe(`/topic/events/${eventId}/registrations`, (message) => {
      const update = JSON.parse(message.body);
      this.callbacks.onRegistrationUpdate?.(update);
    });

    this.client.subscribe(`/topic/events/${eventId}/checkins`, (message) => {
      const update = JSON.parse(message.body);
      this.callbacks.onCheckInUpdate?.(update);
    });

    this.client.subscribe(`/topic/events/${eventId}/updates`, (message) => {
      const update = JSON.parse(message.body);
      this.callbacks.onEventUpdate?.(update);
    });

    // Send subscription confirmation
    this.client.publish({
      destination: `/app/events/${eventId}/subscribe`,
      body: JSON.stringify({ eventId, timestamp: Date.now() })
    });
  }

  subscribeToGlobalUpdates() {
    if (!this.client || !this.client.connected) {
      console.warn('WebSocket not connected');
      return;
    }

    // Subscribe to global event topics
    this.client.subscribe('/topic/events/attendance', (message) => {
      const update = JSON.parse(message.body);
      this.callbacks.onAttendanceUpdate?.(update);
    });

    this.client.subscribe('/topic/events/capacity', (message) => {
      const update = JSON.parse(message.body);
      this.callbacks.onCapacityUpdate?.(update);
    });

    this.client.subscribe('/topic/events/status', (message) => {
      const update = JSON.parse(message.body);
      this.callbacks.onEventStatusChange?.(update);
    });

    this.client.subscribe('/topic/events/registrations', (message) => {
      const update = JSON.parse(message.body);
      this.callbacks.onRegistrationUpdate?.(update);
    });

    this.client.subscribe('/topic/events/checkins', (message) => {
      const update = JSON.parse(message.body);
      this.callbacks.onCheckInUpdate?.(update);
    });

    this.client.subscribe('/topic/system/announcements', (message) => {
      const update = JSON.parse(message.body);
      console.log('System announcement:', update);
    });
  }

  unsubscribeFromEvent() {
    this.eventId = null;
    // Note: STOMP subscriptions are automatically cleaned up on disconnect
  }

  sendPing() {
    if (this.client && this.client.connected) {
      this.client.publish({
        destination: '/app/ping',
        body: JSON.stringify({ timestamp: Date.now() })
      });
    }
  }

  // New flexible subscription methods
  subscribe(destination: string, callback: (message: WebSocketMessage) => void): WebSocketSubscription {
    const subscriptionId = `${destination}-${Date.now()}-${Math.random()}`;

    const subscription: WebSocketSubscription = {
      id: subscriptionId,
      destination,
      callback,
      unsubscribe: () => {
        this.subscriptions.delete(subscriptionId);
      }
    };

    this.subscriptions.set(subscriptionId, subscription);

    // If already connected, subscribe immediately
    if (this.client && this.client.connected) {
      const stompSubscription = this.client.subscribe(destination, (message) => {
        try {
          const parsedMessage: WebSocketMessage = JSON.parse(message.body);
          callback(parsedMessage);
        } catch (error) {
          console.error('Error parsing WebSocket message:', error);
        }
      });

      subscription.unsubscribe = () => {
        stompSubscription.unsubscribe();
        this.subscriptions.delete(subscriptionId);
      };
    }

    return subscription;
  }

  publish(destination: string, body: any) {
    if (this.client && this.client.connected) {
      this.client.publish({
        destination,
        body: JSON.stringify(body)
      });
    } else {
      console.warn('WebSocket not connected. Message not sent:', { destination, body });
    }
  }

  // Convenience methods for event-specific subscriptions
  subscribeToEventUpdates(eventId: string, callback: (message: WebSocketMessage) => void): WebSocketSubscription {
    return this.subscribe(`/topic/events/${eventId}/updates`, callback);
  }

  subscribeToEventRegistrations(eventId: string, callback: (message: WebSocketMessage) => void): WebSocketSubscription {
    return this.subscribe(`/topic/events/${eventId}/registrations`, callback);
  }

  subscribeToEventCheckIns(eventId: string, callback: (message: WebSocketMessage) => void): WebSocketSubscription {
    return this.subscribe(`/topic/events/${eventId}/checkins`, callback);
  }

  subscribeToEventAttendance(eventId: string, callback: (message: WebSocketMessage) => void): WebSocketSubscription {
    return this.subscribe(`/topic/events/${eventId}/attendance`, callback);
  }

  subscribeToEventCapacity(eventId: string, callback: (message: WebSocketMessage) => void): WebSocketSubscription {
    return this.subscribe(`/topic/events/${eventId}/capacity`, callback);
  }

  subscribeToEventStatus(eventId: string, callback: (message: WebSocketMessage) => void): WebSocketSubscription {
    return this.subscribe(`/topic/events/${eventId}/status`, callback);
  }

  subscribeToUserNotifications(userId: string, callback: (message: WebSocketMessage) => void): WebSocketSubscription {
    return this.subscribe(`/queue/users/${userId}/notifications`, callback);
  }

  subscribeToAllEvents(callback: (message: WebSocketMessage) => void): WebSocketSubscription {
    return this.subscribe('/topic/events/updates', callback);
  }

  subscribeToAllRegistrations(callback: (message: WebSocketMessage) => void): WebSocketSubscription {
    return this.subscribe('/topic/events/registrations', callback);
  }

  subscribeToAllCheckIns(callback: (message: WebSocketMessage) => void): WebSocketSubscription {
    return this.subscribe('/topic/events/checkins', callback);
  }

  subscribeToSystemAnnouncements(callback: (message: WebSocketMessage) => void): WebSocketSubscription {
    return this.subscribe('/topic/system/announcements', callback);
  }

  getConnectionStatus(): boolean {
    return this.isConnected;
  }

  getConnectionState(): string {
    if (!this.client) return 'NOT_INITIALIZED';
    if (this.isConnected) return 'CONNECTED';
    if (this.client.active) return 'CONNECTING';
    return 'DISCONNECTED';
  }

  getSubscriptionCount(): number {
    return this.subscriptions.size;
  }
}

// Export a singleton instance
export const webSocketService = new WebSocketService();
export default webSocketService;