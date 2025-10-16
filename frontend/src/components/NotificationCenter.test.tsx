import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import NotificationCenter from './NotificationCenter';
import { useAuth } from '../hooks/useAuth';
import webSocketService from '../services/WebSocketService';

// Mock dependencies
const mockUseAuth = jest.fn();
jest.mock('../hooks/useAuth', () => ({
  useAuth: () => mockUseAuth()
}));
jest.mock('../services/WebSocketService');
const mockWebSocketService = webSocketService as jest.Mocked<typeof webSocketService>;

// Mock WebSocket subscription function
const mockUnsubscribe = jest.fn();
const mockSubscription = {
  id: 'test-subscription',
  destination: '/test',
  callback: jest.fn(),
  unsubscribe: mockUnsubscribe
};

describe('NotificationCenter', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    
    // Mock authenticated user
    mockUseAuth.mockReturnValue({
      user: {
        id: 'user-123',
        email: 'test@example.com',
        firstName: 'Test',
        lastName: 'User',
        role: 'ATTENDEE',
        status: 'ACTIVE',
        emailVerified: true,
        marketingEmails: false,
        eventReminders: true,
        weeklyDigest: true,
        createdAt: '2024-01-01T00:00:00Z'
      },
      isAuthenticated: true,
      isLoading: false,
      error: null,
      login: jest.fn(),
      logout: jest.fn(),
      register: jest.fn(),
      updateProfile: jest.fn(),
      changePassword: jest.fn(),
      verifyEmail: jest.fn(),
      forgotPassword: jest.fn(),
      resetPassword: jest.fn(),
      refreshProfile: jest.fn(),
      clearError: jest.fn(),
      hasRole: jest.fn(),
      hasAnyRole: jest.fn(),
      isAdmin: jest.fn(),
      isOrganizer: jest.fn()
    });

    // Mock WebSocket service methods
    mockWebSocketService.getConnectionStatus = jest.fn().mockReturnValue(true);
    mockWebSocketService.subscribeToUserNotifications = jest.fn().mockReturnValue(mockSubscription);
    mockWebSocketService.subscribeToAllRegistrations = jest.fn().mockReturnValue(mockSubscription);
    mockWebSocketService.subscribeToAllEvents = jest.fn().mockReturnValue(mockSubscription);
    mockWebSocketService.subscribeToSystemAnnouncements = jest.fn().mockReturnValue(mockSubscription);
  });

  it('renders notification bell button', () => {
    render(<NotificationCenter />);
    
    expect(screen.getByRole('button', { name: /notifications/i })).toBeInTheDocument();
  });

  it('shows connection status indicator', () => {
    render(<NotificationCenter />);
    
    // Should show connected status (green dot)
    const statusBadge = screen.getByText('●');
    expect(statusBadge).toHaveClass('bg-success');
  });

  it('shows offline status when disconnected', () => {
    mockWebSocketService.getConnectionStatus = jest.fn().mockReturnValue(false);
    
    render(<NotificationCenter />);
    
    const statusBadge = screen.getByText('○');
    expect(statusBadge).toHaveClass('bg-secondary');
  });

  it('opens notification dropdown when bell is clicked', () => {
    render(<NotificationCenter />);
    
    const bellButton = screen.getByRole('button', { name: /notifications/i });
    fireEvent.click(bellButton);
    
    expect(screen.getByText('Notifications')).toBeInTheDocument();
    expect(screen.getByText('🟢 Live updates')).toBeInTheDocument();
  });

  it('shows no notifications message when empty', () => {
    render(<NotificationCenter />);
    
    const bellButton = screen.getByRole('button', { name: /notifications/i });
    fireEvent.click(bellButton);
    
    expect(screen.getByText('No notifications')).toBeInTheDocument();
  });

  it('subscribes to WebSocket channels when authenticated', () => {
    render(<NotificationCenter />);
    
    expect(mockWebSocketService.subscribeToUserNotifications).toHaveBeenCalledWith('user-123', expect.any(Function));
    expect(mockWebSocketService.subscribeToAllRegistrations).toHaveBeenCalledWith(expect.any(Function));
    expect(mockWebSocketService.subscribeToAllEvents).toHaveBeenCalledWith(expect.any(Function));
    expect(mockWebSocketService.subscribeToSystemAnnouncements).toHaveBeenCalledWith(expect.any(Function));
  });

  it('does not subscribe when user is not authenticated', () => {
    mockUseAuth.mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
      login: jest.fn(),
      logout: jest.fn(),
      register: jest.fn(),
      updateProfile: jest.fn(),
      changePassword: jest.fn(),
      verifyEmail: jest.fn(),
      forgotPassword: jest.fn(),
      resetPassword: jest.fn(),
      refreshProfile: jest.fn(),
      clearError: jest.fn(),
      hasRole: jest.fn(),
      hasAnyRole: jest.fn(),
      isAdmin: jest.fn(),
      isOrganizer: jest.fn()
    });

    render(<NotificationCenter />);
    
    expect(mockWebSocketService.subscribeToUserNotifications).not.toHaveBeenCalled();
  });

  it('displays notification badge when notifications exist', async () => {
    const { rerender } = render(<NotificationCenter />);
    
    // Simulate adding a notification through WebSocket
    const userNotificationCallback = mockWebSocketService.subscribeToUserNotifications.mock.calls[0][1];
    
    await act(async () => {
      userNotificationCallback({
        type: 'info',
        message: 'Test notification',
        timestamp: Date.now()
      });
    });

    rerender(<NotificationCenter />);
    
    // Should show notification badge with count
    expect(screen.getByText('1')).toBeInTheDocument();
  });

  it('formats notification timestamps correctly', async () => {
    render(<NotificationCenter />);
    
    // Open dropdown first
    const bellButton = screen.getByRole('button', { name: /notifications/i });
    fireEvent.click(bellButton);
    
    // Add a notification
    const userNotificationCallback = mockWebSocketService.subscribeToUserNotifications.mock.calls[0][1];
    
    await act(async () => {
      userNotificationCallback({
        type: 'info',
        title: 'Personal Notification',
        message: 'Test message',
        timestamp: Date.now() - 120000 // 2 minutes ago
      });
    });
    
    await waitFor(() => {
      expect(screen.getByText('2m ago')).toBeInTheDocument();
    });
  });

  it('handles registration update notifications', async () => {
    render(<NotificationCenter />);
    
    const registrationCallback = mockWebSocketService.subscribeToAllRegistrations.mock.calls[0][0];
    
    await act(async () => {
      registrationCallback({
        type: 'REGISTRATION_UPDATE',
        registrationType: 'NEW',
        registrationData: {
          userId: 'user-123',
          eventName: 'Test Event'
        },
        timestamp: Date.now()
      });
    });
    
    // Open dropdown to see notification
    const bellButton = screen.getByRole('button', { name: /notifications/i });
    fireEvent.click(bellButton);
    
    await waitFor(() => {
      expect(screen.getByText('Registration Update')).toBeInTheDocument();
    });
  });

  it('clears all notifications when clear button is clicked', async () => {
    render(<NotificationCenter />);
    
    // Add a notification
    const userNotificationCallback = mockWebSocketService.subscribeToUserNotifications.mock.calls[0][1];
    
    await act(async () => {
      userNotificationCallback({
        type: 'info',
        title: 'Test',
        message: 'Test message',
        timestamp: Date.now()
      });
    });
    
    // Open dropdown
    const bellButton = screen.getByRole('button', { name: /notifications/i });
    fireEvent.click(bellButton);
    
    await waitFor(() => {
      expect(screen.getByText('Test')).toBeInTheDocument();
    });
    
    // Click clear all
    const clearButton = screen.getByText('Clear all notifications');
    fireEvent.click(clearButton);
    
    await waitFor(() => {
      expect(screen.getByText('No notifications')).toBeInTheDocument();
    });
  });

  it('marks notification as read when clicked', async () => {
    render(<NotificationCenter />);
    
    // Add a notification
    const userNotificationCallback = mockWebSocketService.subscribeToUserNotifications.mock.calls[0][1];
    
    await act(async () => {
      userNotificationCallback({
        type: 'info',
        title: 'Test Notification',
        message: 'Click to mark as read',
        timestamp: Date.now()
      });
    });
    
    // Open dropdown
    const bellButton = screen.getByRole('button', { name: /notifications/i });
    fireEvent.click(bellButton);
    
    await waitFor(() => {
      expect(screen.getByText('Test Notification')).toBeInTheDocument();
    });
    
    // Click on notification to mark as read
    const notification = screen.getByText('Test Notification').closest('.notification-item');
    fireEvent.click(notification!);
    
    // The unread badge should disappear (notification is now read)
    expect(screen.queryByText('1')).not.toBeInTheDocument();
  });

  it('auto-hides notifications when autoHide is true', async () => {
    jest.useFakeTimers();
    
    render(<NotificationCenter />);
    
    const registrationCallback = mockWebSocketService.subscribeToAllRegistrations.mock.calls[0][0];
    
    await act(async () => {
      registrationCallback({
        type: 'REGISTRATION_UPDATE',
        registrationType: 'NEW',
        registrationData: {
          userId: 'user-123',
          eventName: 'Auto Hide Event'
        },
        timestamp: Date.now()
      });
    });
    
    // Open dropdown to verify notification exists
    const bellButton = screen.getByRole('button', { name: /notifications/i });
    fireEvent.click(bellButton);
    
    await waitFor(() => {
      expect(screen.getByText('Registration Update')).toBeInTheDocument();
    });
    
    // Fast-forward time to trigger auto-hide
    act(() => {
      jest.advanceTimersByTime(5000);
    });
    
    await waitFor(() => {
      expect(screen.getByText('No notifications')).toBeInTheDocument();
    });
    
    jest.useRealTimers();
  });

  it('shows correct notification icons based on type', async () => {
    render(<NotificationCenter />);
    
    const userNotificationCallback = mockWebSocketService.subscribeToUserNotifications.mock.calls[0][1];
    const registrationCallback = mockWebSocketService.subscribeToAllRegistrations.mock.calls[0][0];
    const eventCallback = mockWebSocketService.subscribeToAllEvents.mock.calls[0][0];
    
    // Add notifications of different types
    await act(async () => {
      userNotificationCallback({
        type: 'info',
        title: 'Personal Notification',
        message: 'Personal message',
        timestamp: Date.now()
      });
      
      registrationCallback({
        type: 'REGISTRATION_UPDATE',
        registrationType: 'NEW',
        registrationData: { userId: 'user-123' },
        timestamp: Date.now()
      });
      
      eventCallback({
        type: 'EVENT_UPDATE',
        updateType: 'STATUS_CHANGE',
        eventId: 'event-123',
        newStatus: 'CANCELLED',
        timestamp: Date.now()
      });
    });
    
    // Open dropdown
    const bellButton = screen.getByRole('button', { name: /notifications/i });
    fireEvent.click(bellButton);
    
    await waitFor(() => {
      expect(screen.getByText('Personal Notification')).toBeInTheDocument();
      expect(screen.getByText('Registration Update')).toBeInTheDocument();
      expect(screen.getByText('Event Update')).toBeInTheDocument();
    });
  });

  it('respects maxNotifications prop', async () => {
    render(<NotificationCenter maxNotifications={2} />);
    
    const userNotificationCallback = mockWebSocketService.subscribeToUserNotifications.mock.calls[0][1];
    
    // Add 3 notifications
    await act(async () => {
      userNotificationCallback({ type: 'info', title: 'Notification 1', message: 'First', timestamp: Date.now() });
      userNotificationCallback({ type: 'info', title: 'Notification 2', message: 'Second', timestamp: Date.now() });
      userNotificationCallback({ type: 'info', title: 'Notification 3', message: 'Third', timestamp: Date.now() });
    });
    
    // Open dropdown
    const bellButton = screen.getByRole('button', { name: /notifications/i });
    fireEvent.click(bellButton);
    
    // Should only show 2 notifications (maxNotifications = 2)
    await waitFor(() => {
      expect(screen.getByText('Notification 2')).toBeInTheDocument();
      expect(screen.getByText('Notification 3')).toBeInTheDocument();
      expect(screen.queryByText('Notification 1')).not.toBeInTheDocument();
    });
  });

  it('cleans up subscriptions on unmount', () => {
    const { unmount } = render(<NotificationCenter />);
    
    unmount();
    
    // Should call unsubscribe on all subscriptions
    expect(mockUnsubscribe).toHaveBeenCalledTimes(4); // 4 subscriptions created
  });
});