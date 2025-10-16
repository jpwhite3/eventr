import { Client } from '@stomp/stompjs';

// Mock STOMP client
jest.mock('@stomp/stompjs');
jest.mock('sockjs-client', () => {
  return jest.fn().mockImplementation(() => ({
    close: jest.fn()
  }));
});

const MockedClient = Client as jest.MockedClass<typeof Client>;

describe('WebSocketService', () => {
  let mockStompClient: any;
  let mockSubscription: { unsubscribe: jest.MockedFunction<() => void> };
  let webSocketService: any;

  beforeEach(() => {
    jest.clearAllMocks();
    
    mockSubscription = {
      unsubscribe: jest.fn()
    };
    
    mockStompClient = {
      activate: jest.fn(),
      deactivate: jest.fn(),
      subscribe: jest.fn().mockReturnValue(mockSubscription),
      publish: jest.fn(),
      connected: false,
      active: false,
      onConnect: null,
      onStompError: null,
      onWebSocketError: null,
      onWebSocketClose: null
    };

    MockedClient.mockImplementation(() => mockStompClient);
    
    // Reset modules and re-import to get fresh instances
    jest.resetModules();
    
    // Import the singleton instance
    const module = require('./WebSocketService');
    webSocketService = module.default;
  });

  afterEach(() => {
    jest.clearAllTimers();
  });

  describe('Connection Management', () => {
    it('should initialize with disconnected state', () => {
      expect(webSocketService.getConnectionStatus()).toBe(false);
      expect(webSocketService.getConnectionState()).toBe('DISCONNECTED');
    });

    it('should connect to WebSocket server', async () => {
      console.log('Test starting');
      
      // Mock activate to immediately trigger the onConnect callback
      mockStompClient.activate.mockImplementation(() => {
        console.log('Activate called, onConnect is:', typeof mockStompClient.onConnect);
        // Call onConnect immediately if it exists
        if (mockStompClient.onConnect) {
          console.log('Calling onConnect');
          mockStompClient.onConnect({});
        } else {
          console.log('onConnect is not set yet');
        }
      });
      
      console.log('Calling connect');
      const connectPromise = webSocketService.connect();
      console.log('Connect called, awaiting...');
      
      // Wait for the promise to resolve
      try {
        await connectPromise;
        console.log('Connect promise resolved');
      } catch (error) {
        console.log('Connect promise rejected:', error);
        throw error;
      }
      
      expect(mockStompClient.activate).toHaveBeenCalled();
    });

    it('should handle connection errors', async () => {
      // Set up activate to trigger onStompError callback immediately
      mockStompClient.activate.mockImplementation(() => {
        if (mockStompClient.onStompError) {
          const errorFrame = { headers: { message: 'Connection failed' } };
          mockStompClient.onStompError(errorFrame as any);
        }
      });
      
      await expect(webSocketService.connect()).rejects.toThrow('WebSocket connection failed: Connection failed');
      expect(mockStompClient.onStompError).toBeDefined(); // Should be set by connect method
    });

    it('should disconnect from WebSocket server', async () => {
      // First establish a connection
      mockStompClient.activate.mockImplementation(() => {
        if (mockStompClient.onConnect) {
          mockStompClient.onConnect({} as any);
        }
      });
      
      await webSocketService.connect();
      
      // Now disconnect
      webSocketService.disconnect();
      
      expect(mockStompClient.deactivate).toHaveBeenCalled();
    });

    it('should return existing connection if already connected', async () => {
      // Mock already connected state
      webSocketService.isConnected = true;
      jest.spyOn(webSocketService, 'getConnectionStatus').mockReturnValue(true);
      
      const result = await webSocketService.connect();
      
      expect(result).toBeUndefined();
      expect(mockStompClient.activate).not.toHaveBeenCalled();
    });

    it('should handle reconnection attempts', () => {
      jest.useFakeTimers();
      const setTimeoutSpy = jest.spyOn(global, 'setTimeout');
      
      // Simulate connection loss which triggers reconnection
      if (mockStompClient.onWebSocketClose) {
        mockStompClient.onWebSocketClose({} as any);
      }
      
      // Should attempt to reconnect after delay
      expect(setTimeoutSpy).toHaveBeenCalledWith(expect.any(Function), expect.any(Number));
      
      setTimeoutSpy.mockRestore();
      jest.useRealTimers();
    });
  });

  describe('Subscription Management', () => {
    beforeEach(() => {
      Object.defineProperty(mockStompClient, 'connected', { value: true, writable: true });
    });

    it('should create subscription with callback', () => {
      const callback = jest.fn();
      const destination = '/topic/test';
      
      const subscription = webSocketService.subscribe(destination, callback);
      
      expect(mockStompClient.subscribe).toHaveBeenCalledWith(destination, expect.any(Function));
      expect(subscription.destination).toBe(destination);
      expect(typeof subscription.unsubscribe).toBe('function');
    });

    it('should handle message parsing in subscription', () => {
      const callback = jest.fn();
      const destination = '/topic/test';
      const testMessage = { type: 'TEST', data: 'test data', timestamp: Date.now() };
      
      webSocketService.subscribe(destination, callback);
      
      // Get the message handler passed to STOMP subscribe
      const messageHandler = mockStompClient.subscribe.mock.calls[0][1];
      
      // Simulate receiving a message
      messageHandler({ 
        body: JSON.stringify(testMessage),
        command: '',
        headers: {},
        ack: jest.fn(),
        nack: jest.fn(),
        binaryBody: new Uint8Array(),
        isBinaryBody: false
      });
      
      expect(callback).toHaveBeenCalledWith(testMessage);
    });

    it('should handle malformed JSON in messages', () => {
      const callback = jest.fn();
      const consoleError = jest.spyOn(console, 'error').mockImplementation();
      
      webSocketService.subscribe('/topic/test', callback);
      
      const messageHandler = mockStompClient.subscribe.mock.calls[0][1];
      
      // Simulate malformed JSON
      messageHandler({ 
        body: 'invalid json',
        command: '',
        headers: {},
        ack: jest.fn(),
        nack: jest.fn(),
        binaryBody: new Uint8Array(),
        isBinaryBody: false
      });
      
      expect(consoleError).toHaveBeenCalledWith('Error parsing WebSocket message:', expect.any(Error));
      expect(callback).not.toHaveBeenCalled();
      
      consoleError.mockRestore();
    });

    it('should unsubscribe properly', () => {
      const subscription = webSocketService.subscribe('/topic/test', jest.fn());
      
      subscription.unsubscribe();
      
      expect(mockSubscription.unsubscribe).toHaveBeenCalled();
    });

    it('should track subscription count', () => {
      webSocketService.subscribe('/topic/test1', jest.fn());
      webSocketService.subscribe('/topic/test2', jest.fn());
      
      expect(webSocketService.getSubscriptionCount()).toBe(2);
    });
  });

  describe('Event-Specific Subscriptions', () => {
    beforeEach(() => {
      Object.defineProperty(mockStompClient, 'connected', { value: true, writable: true });
    });

    it('should create event updates subscription', () => {
      const callback = jest.fn();
      const eventId = 'event-123';
      
      webSocketService.subscribeToEventUpdates(eventId, callback);
      
      expect(mockStompClient.subscribe).toHaveBeenCalledWith(`/topic/events/${eventId}/updates`, expect.any(Function));
    });

    it('should create event registrations subscription', () => {
      const callback = jest.fn();
      const eventId = 'event-123';
      
      webSocketService.subscribeToEventRegistrations(eventId, callback);
      
      expect(mockStompClient.subscribe).toHaveBeenCalledWith(`/topic/events/${eventId}/registrations`, expect.any(Function));
    });

    it('should create event check-ins subscription', () => {
      const callback = jest.fn();
      const eventId = 'event-123';
      
      webSocketService.subscribeToEventCheckIns(eventId, callback);
      
      expect(mockStompClient.subscribe).toHaveBeenCalledWith(`/topic/events/${eventId}/checkins`, expect.any(Function));
    });

    it('should create event attendance subscription', () => {
      const callback = jest.fn();
      const eventId = 'event-123';
      
      webSocketService.subscribeToEventAttendance(eventId, callback);
      
      expect(mockStompClient.subscribe).toHaveBeenCalledWith(`/topic/events/${eventId}/attendance`, expect.any(Function));
    });

    it('should create event capacity subscription', () => {
      const callback = jest.fn();
      const eventId = 'event-123';
      
      webSocketService.subscribeToEventCapacity(eventId, callback);
      
      expect(mockStompClient.subscribe).toHaveBeenCalledWith(`/topic/events/${eventId}/capacity`, expect.any(Function));
    });

    it('should create event status subscription', () => {
      const callback = jest.fn();
      const eventId = 'event-123';
      
      webSocketService.subscribeToEventStatus(eventId, callback);
      
      expect(mockStompClient.subscribe).toHaveBeenCalledWith(`/topic/events/${eventId}/status`, expect.any(Function));
    });

    it('should create user notifications subscription', () => {
      const callback = jest.fn();
      const userId = 'user-123';
      
      webSocketService.subscribeToUserNotifications(userId, callback);
      
      expect(mockStompClient.subscribe).toHaveBeenCalledWith(`/queue/users/${userId}/notifications`, expect.any(Function));
    });
  });

  describe('Global Subscriptions', () => {
    beforeEach(() => {
      Object.defineProperty(mockStompClient, 'connected', { value: true, writable: true });
    });

    it('should create all events subscription', () => {
      const callback = jest.fn();
      
      webSocketService.subscribeToAllEvents(callback);
      
      expect(mockStompClient.subscribe).toHaveBeenCalledWith('/topic/events/updates', expect.any(Function));
    });

    it('should create all registrations subscription', () => {
      const callback = jest.fn();
      
      webSocketService.subscribeToAllRegistrations(callback);
      
      expect(mockStompClient.subscribe).toHaveBeenCalledWith('/topic/events/registrations', expect.any(Function));
    });

    it('should create all check-ins subscription', () => {
      const callback = jest.fn();
      
      webSocketService.subscribeToAllCheckIns(callback);
      
      expect(mockStompClient.subscribe).toHaveBeenCalledWith('/topic/events/checkins', expect.any(Function));
    });

    it('should create system announcements subscription', () => {
      const callback = jest.fn();
      
      webSocketService.subscribeToSystemAnnouncements(callback);
      
      expect(mockStompClient.subscribe).toHaveBeenCalledWith('/topic/system/announcements', expect.any(Function));
    });
  });

  describe('Message Publishing', () => {
    beforeEach(() => {
      Object.defineProperty(mockStompClient, 'connected', { value: true, writable: true });
    });

    it('should publish messages when connected', () => {
      const destination = '/app/test';
      const message = { type: 'TEST', data: 'test data' };
      
      webSocketService.publish(destination, message);
      
      expect(mockStompClient.publish).toHaveBeenCalledWith({
        destination,
        body: JSON.stringify(message)
      });
    });

    it('should not publish messages when disconnected', () => {
      Object.defineProperty(mockStompClient, 'connected', { value: false, writable: true });
      const consoleWarn = jest.spyOn(console, 'warn').mockImplementation();
      
      const destination = '/app/test';
      const message = { type: 'TEST', data: 'test data' };
      
      webSocketService.publish(destination, message);
      
      expect(mockStompClient.publish).not.toHaveBeenCalled();
      expect(consoleWarn).toHaveBeenCalledWith('WebSocket not connected. Message not sent:', { destination, body: message });
      
      consoleWarn.mockRestore();
    });
  });

  describe('Connection State Management', () => {
    it('should return correct connection state when not initialized', () => {
      expect(webSocketService.getConnectionState()).toBe('DISCONNECTED');
    });

    it('should return connecting state when active but not connected', () => {
      Object.defineProperty(mockStompClient, 'active', { value: true, writable: true });
      Object.defineProperty(mockStompClient, 'connected', { value: false, writable: true });
      webSocketService.client = mockStompClient;
      
      expect(webSocketService.getConnectionState()).toBe('CONNECTING');
    });

    it('should return connected state when connected', () => {
      Object.defineProperty(mockStompClient, 'connected', { value: true, writable: true });
      webSocketService.isConnected = true;
      
      expect(webSocketService.getConnectionStatus()).toBe(true);
    });
  });
});