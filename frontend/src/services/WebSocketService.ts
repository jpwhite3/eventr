import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface WebSocketUpdate {
  type: string;
  eventId: string;
  timestamp: number;
  [key: string]: any;
}

export interface WebSocketCallbacks {
  onAttendanceUpdate?: (update: WebSocketUpdate) => void;
  onCapacityUpdate?: (update: WebSocketUpdate) => void;
  onEventStatusChange?: (update: WebSocketUpdate) => void;
  onRegistrationUpdate?: (update: WebSocketUpdate) => void;
  onCheckInUpdate?: (update: WebSocketUpdate) => void;
  onEventUpdate?: (update: WebSocketUpdate) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: any) => void;
}

class WebSocketService {
  private client: Client | null = null;
  private eventId: string | null = null;
  private callbacks: WebSocketCallbacks = {};
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000;

  constructor() {
    this.setupClient();
  }

  private setupClient() {
    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: {},
      debug: (str) => {
        console.log('WebSocket Debug:', str);
      },
      reconnectDelay: this.reconnectDelay,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.client.onConnect = (frame) => {
      console.log('WebSocket connected:', frame);
      this.reconnectAttempts = 0;
      this.callbacks.onConnect?.();
      this.subscribeToGlobalUpdates();
      if (this.eventId) {
        this.subscribeToEvent(this.eventId);
      }
    };

    this.client.onDisconnect = () => {
      console.log('WebSocket disconnected');
      this.callbacks.onDisconnect?.();
    };

    this.client.onStompError = (frame) => {
      console.error('WebSocket STOMP error:', frame);
      this.callbacks.onError?.(frame);
    };

    this.client.onWebSocketError = (error) => {
      console.error('WebSocket error:', error);
      this.callbacks.onError?.(error);
    };

    this.client.onWebSocketClose = () => {
      console.log('WebSocket connection closed');
      this.handleReconnection();
    };
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

  connect() {
    if (this.client && !this.client.connected) {
      this.client.activate();
    }
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
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

  isConnected(): boolean {
    return this.client?.connected || false;
  }

  getConnectionState(): string {
    if (!this.client) return 'NOT_INITIALIZED';
    if (this.client.connected) return 'CONNECTED';
    if (this.client.active) return 'CONNECTING';
    return 'DISCONNECTED';
  }
}

// Export a singleton instance
export const webSocketService = new WebSocketService();
export default webSocketService;