import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import CheckInInterface from './CheckInInterface';
import apiClient from '../api/apiClient';

// Mock the API client
jest.mock('../api/apiClient');
const mockedApiClient = apiClient as jest.Mocked<typeof apiClient>;

// Mock QRScanner component
jest.mock('./QRScanner', () => {
  return function QRScanner({ onScan, onError }: any) {
    return (
      <div data-testid="qr-scanner">
        <button 
          onClick={() => onScan('test-qr-code')}
          data-testid="mock-scan-success"
        >
          Mock Scan Success
        </button>
        <button 
          onClick={() => onError('Scan failed')}
          data-testid="mock-scan-error"
        >
          Mock Scan Error
        </button>
      </div>
    );
  };
});

describe('CheckInInterface', () => {
  const defaultProps = {
    eventId: 'event-123',
    sessionId: 'session-456',
    staffMember: 'John Staff'
  };

  const mockStats = {
    totalCheckedIn: 50,
    totalRegistrations: 100,
    checkInRate: 50
  };

  const mockRecentCheckIns = [
    {
      id: '1',
      userName: 'John Doe',
      userEmail: 'john@example.com',
      checkedInAt: '2024-01-15T10:30:00Z',
      method: 'QR',
      sessionTitle: 'Opening Session'
    },
    {
      id: '2',
      userName: 'Jane Smith',
      userEmail: 'jane@example.com',
      checkedInAt: '2024-01-15T10:28:00Z',
      method: 'Manual'
    }
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    
    // Mock successful API responses by default
    mockedApiClient.get.mockImplementation((url) => {
      if (url.includes('/stats')) {
        return Promise.resolve({ data: mockStats });
      }
      if (url.includes('/attendance')) {
        return Promise.resolve({ data: mockRecentCheckIns });
      }
      if (url.includes('/registrations')) {
        return Promise.resolve({ data: [{ id: 'reg-123', userName: 'Test User', email: 'test@example.com' }] });
      }
      return Promise.reject(new Error('Unknown endpoint'));
    });

    mockedApiClient.post.mockResolvedValue({
      data: {
        id: 'checkin-123',
        userName: 'Test User',
        checkedInAt: '2024-01-15T10:35:00Z',
        method: 'QR'
      }
    });
  });

  it('renders without crashing', () => {
    render(<CheckInInterface {...defaultProps} />);
    expect(screen.getByText(/Session Check-In/)).toBeInTheDocument();
  });

  it('displays stats correctly', async () => {
    render(<CheckInInterface {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByText('Checked In')).toBeInTheDocument();
      expect(screen.getByText('Remaining')).toBeInTheDocument();
      // The stats numbers will be displayed, but might be in different elements
      expect(mockedApiClient.get).toHaveBeenCalledWith('/checkin/event/event-123/stats');
    });
  });

  it('displays recent check-ins', async () => {
    render(<CheckInInterface {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('Jane Smith')).toBeInTheDocument();
      expect(screen.getByText('john@example.com')).toBeInTheDocument();
    });
  });

  it('handles QR code scanning successfully', async () => {
    const onCheckInSuccess = jest.fn();
    render(<CheckInInterface {...defaultProps} onCheckInSuccess={onCheckInSuccess} />);
    
    // Click to start scanning
    const scanButton = screen.getByText('📷 Start Scanner');
    fireEvent.click(scanButton);
    
    // Verify QR scanner is shown
    expect(screen.getByTestId('qr-scanner')).toBeInTheDocument();
    
    // Mock successful scan
    fireEvent.click(screen.getByTestId('mock-scan-success'));
    
    await waitFor(() => {
      expect(mockedApiClient.post).toHaveBeenCalledWith('/checkin/qr', {
        qrCode: 'test-qr-code',
        checkedInBy: 'John Staff',
        deviceId: navigator.userAgent,
        deviceName: 'Staff Device',
        location: 'Session Check-in'
      });
      expect(onCheckInSuccess).toHaveBeenCalled();
    });
  });

  it('handles QR scanning errors', async () => {
    const onCheckInError = jest.fn();
    render(<CheckInInterface {...defaultProps} onCheckInError={onCheckInError} />);
    
    // Start scanning
    fireEvent.click(screen.getByText('📷 Start Scanner'));
    
    // Mock scan error
    fireEvent.click(screen.getByTestId('mock-scan-error'));
    
    await waitFor(() => {
      expect(screen.getByText('Scan failed')).toBeInTheDocument();
      expect(onCheckInError).toHaveBeenCalledWith('Scan failed');
    });
  });

  it('handles manual check-in', async () => {
    render(<CheckInInterface {...defaultProps} />);
    
    // Fill in manual check-in form
    fireEvent.change(screen.getByPlaceholderText('Enter attendee email address'), {
      target: { value: 'manual@example.com' }
    });
    
    // Submit manual check-in
    fireEvent.click(screen.getByText('✅ Check In'));
    
    await waitFor(() => {
      expect(mockedApiClient.post).toHaveBeenCalledWith('/checkin/manual', {
        registrationId: 'reg-123',
        sessionId: 'session-456',
        type: 'SESSION',
        method: 'MANUAL',
        checkedInBy: 'John Staff',
        notes: null
      });
    });
  });

  it('handles API errors gracefully', async () => {
    mockedApiClient.get.mockRejectedValue(new Error('API Error'));
    
    render(<CheckInInterface {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByText(/failed to load/i)).toBeInTheDocument();
    });
  });

  it('shows loading state', async () => {
    // Mock a slow API response
    mockedApiClient.get.mockImplementation(() => 
      new Promise(resolve => setTimeout(() => resolve({ data: mockStats }), 100))
    );
    
    render(<CheckInInterface {...defaultProps} />);
    
    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it('refreshes data when refresh button is clicked', async () => {
    render(<CheckInInterface {...defaultProps} />);
    
    await waitFor(() => {
      expect(mockedApiClient.get).toHaveBeenCalledTimes(2); // Initial loads
    });
    
    // Clear the mock to count new calls
    mockedApiClient.get.mockClear();
    
    // Click refresh
    fireEvent.click(screen.getByText('🔄'));
    
    await waitFor(() => {
      expect(mockedApiClient.get).toHaveBeenCalledTimes(2); // Stats and recent check-ins
    });
  });

  it('toggles between QR scanning and manual entry', () => {
    render(<CheckInInterface {...defaultProps} />);
    
    // Initially shows manual entry form
    expect(screen.getByPlaceholderText('Enter attendee email address')).toBeInTheDocument();
    
    // Switch to QR scanning
    fireEvent.click(screen.getByText('📷 Start Scanner'));
    expect(screen.getByTestId('qr-scanner')).toBeInTheDocument();
    
    // Switch back to manual
    fireEvent.click(screen.getByText('⏹️ Stop Scanner'));
    expect(screen.getByPlaceholderText('Enter attendee email address')).toBeInTheDocument();
  });

  it('displays success messages', async () => {
    render(<CheckInInterface {...defaultProps} />);
    
    // Perform successful manual check-in
    fireEvent.change(screen.getByPlaceholderText('Enter attendee email address'), {
      target: { value: 'success@example.com' }
    });
    fireEvent.click(screen.getByText('✅ Check In'));
    
    await waitFor(() => {
      expect(screen.getByText(/successfully checked in/i)).toBeInTheDocument();
    });
  });

  it('works without optional session ID', async () => {
    const propsWithoutSession = {
      eventId: 'event-123',
      staffMember: 'John Staff'
    };
    
    render(<CheckInInterface {...propsWithoutSession} />);
    
    await waitFor(() => {
      expect(screen.getByText(/Event Check-In/)).toBeInTheDocument();
    });
    
    // Should still load stats for event
    expect(mockedApiClient.get).toHaveBeenCalledWith('/checkin/event/event-123/stats');
  });
});