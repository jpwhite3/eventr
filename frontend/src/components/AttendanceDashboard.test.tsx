import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import AttendanceDashboard from './AttendanceDashboard';
import apiClient from '../api/apiClient';

// Mock the API client
jest.mock('../api/apiClient');
const mockedApiClient = apiClient as jest.Mocked<typeof apiClient>;

// Mock Chart.js components
jest.mock('react-chartjs-2', () => ({
  Line: ({ data, options }: any) => (
    <div data-testid="line-chart" data-chart-data={JSON.stringify(data)}>
      Line Chart: {options?.plugins?.title?.text}
    </div>
  ),
  Bar: ({ data, options }: any) => (
    <div data-testid="bar-chart" data-chart-data={JSON.stringify(data)}>
      Bar Chart: {options?.plugins?.title?.text}
    </div>
  ),
  Doughnut: ({ data, options }: any) => (
    <div data-testid="doughnut-chart" data-chart-data={JSON.stringify(data)}>
      Doughnut Chart: {options?.plugins?.title?.text}
    </div>
  )
}));

describe('AttendanceDashboard', () => {
  const mockEventId = 'event-123';

  const mockAttendanceData = {
    eventId: mockEventId,
    eventName: 'Tech Conference 2024',
    summary: {
      totalRegistrations: 250,
      totalAttendees: 200,
      overallAttendanceRate: 80.0,
      noShowCount: 50,
      checkInMethods: {
        'QR_CODE': 150,
        'MANUAL': 50
      }
    },
    sessionAttendance: [
      {
        sessionId: 'session-1',
        sessionTitle: 'Opening Keynote',
        registrations: 200,
        attendees: 180,
        attendanceRate: 90.0,
        checkInTime: '2024-01-15T09:00:00Z'
      },
      {
        sessionId: 'session-2',
        sessionTitle: 'Technical Workshop',
        registrations: 50,
        attendees: 45,
        attendanceRate: 90.0,
        checkInTime: '2024-01-15T10:30:00Z'
      }
    ],
    timelineData: {
      checkInsByHour: [
        { hour: '09:00', count: 120 },
        { hour: '10:00', count: 50 },
        { hour: '11:00', count: 30 }
      ]
    },
    attendanceByRegion: [
      { region: 'North America', count: 120 },
      { region: 'Europe', count: 60 },
      { region: 'Asia Pacific', count: 20 }
    ]
  };

  beforeEach(() => {
    jest.clearAllMocks();
    
    // Mock successful API response
    mockedApiClient.get.mockResolvedValue({
      data: mockAttendanceData
    });
  });

  it('renders without crashing', () => {
    render(<AttendanceDashboard eventId={mockEventId} />);
    expect(screen.getByText('Attendance Dashboard')).toBeInTheDocument();
  });

  it('displays loading state initially', () => {
    render(<AttendanceDashboard eventId={mockEventId} />);
    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it('loads and displays attendance data', async () => {
    render(<AttendanceDashboard eventId={mockEventId} />);
    
    await waitFor(() => {
      expect(screen.getByText('Tech Conference 2024')).toBeInTheDocument();
      expect(screen.getByText('250')).toBeInTheDocument(); // Total registrations
      expect(screen.getByText('200')).toBeInTheDocument(); // Total attendees
      expect(screen.getByText('80%')).toBeInTheDocument(); // Overall attendance rate
      expect(screen.getByText('50')).toBeInTheDocument(); // No-shows
    });

    expect(mockedApiClient.get).toHaveBeenCalledWith(`/api/checkin/event/${mockEventId}/report`);
  });

  it('displays session attendance data', async () => {
    render(<AttendanceDashboard eventId={mockEventId} />);
    
    await waitFor(() => {
      expect(screen.getByText('Opening Keynote')).toBeInTheDocument();
      expect(screen.getByText('Technical Workshop')).toBeInTheDocument();
      expect(screen.getByText('180 / 200')).toBeInTheDocument(); // Attendees/registrations
      expect(screen.getByText('45 / 50')).toBeInTheDocument();
      expect(screen.getByText('90%')).toBeInTheDocument(); // Attendance rate
    });
  });

  it('displays check-in methods breakdown', async () => {
    render(<AttendanceDashboard eventId={mockEventId} />);
    
    await waitFor(() => {
      expect(screen.getByText('QR Code: 150')).toBeInTheDocument();
      expect(screen.getByText('Manual: 50')).toBeInTheDocument();
    });
  });

  it('renders attendance timeline chart', async () => {
    render(<AttendanceDashboard eventId={mockEventId} />);
    
    await waitFor(() => {
      const lineChart = screen.getByTestId('line-chart');
      expect(lineChart).toBeInTheDocument();
      
      const chartData = JSON.parse(lineChart.getAttribute('data-chart-data') || '{}');
      expect(chartData.labels).toContain('09:00');
      expect(chartData.labels).toContain('10:00');
      expect(chartData.datasets[0].data).toContain(120);
      expect(chartData.datasets[0].data).toContain(50);
    });
  });

  it('renders session attendance bar chart', async () => {
    render(<AttendanceDashboard eventId={mockEventId} />);
    
    await waitFor(() => {
      const barChart = screen.getByTestId('bar-chart');
      expect(barChart).toBeInTheDocument();
      
      const chartData = JSON.parse(barChart.getAttribute('data-chart-data') || '{}');
      expect(chartData.labels).toContain('Opening Keynote');
      expect(chartData.labels).toContain('Technical Workshop');
      expect(chartData.datasets[0].data).toContain(90.0); // Attendance rates
    });
  });

  it('renders regional attendance doughnut chart', async () => {
    render(<AttendanceDashboard eventId={mockEventId} />);
    
    await waitFor(() => {
      const doughnutChart = screen.getByTestId('doughnut-chart');
      expect(doughnutChart).toBeInTheDocument();
      
      const chartData = JSON.parse(doughnutChart.getAttribute('data-chart-data') || '{}');
      expect(chartData.labels).toContain('North America');
      expect(chartData.labels).toContain('Europe');
      expect(chartData.datasets[0].data).toContain(120);
      expect(chartData.datasets[0].data).toContain(60);
    });
  });

  it('handles API errors gracefully', async () => {
    mockedApiClient.get.mockRejectedValue(new Error('API Error'));
    
    render(<AttendanceDashboard eventId={mockEventId} />);
    
    await waitFor(() => {
      expect(screen.getByText(/failed to load attendance data/i)).toBeInTheDocument();
    });
  });

  it('refreshes data when refresh button is clicked', async () => {
    render(<AttendanceDashboard eventId={mockEventId} />);
    
    await waitFor(() => {
      expect(mockedApiClient.get).toHaveBeenCalledTimes(1);
    });

    // Clear mock to count new calls
    mockedApiClient.get.mockClear();
    
    // Click refresh button
    fireEvent.click(screen.getByTestId('refresh-button'));
    
    await waitFor(() => {
      expect(mockedApiClient.get).toHaveBeenCalledTimes(1);
    });
  });

  it('allows filtering by date range', async () => {
    render(<AttendanceDashboard eventId={mockEventId} />);
    
    await waitFor(() => {
      expect(screen.getByLabelText(/start date/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/end date/i)).toBeInTheDocument();
    });

    // Change date filters
    fireEvent.change(screen.getByLabelText(/start date/i), {
      target: { value: '2024-01-15' }
    });
    fireEvent.change(screen.getByLabelText(/end date/i), {
      target: { value: '2024-01-16' }
    });
    
    fireEvent.click(screen.getByText('Apply Filters'));
    
    await waitFor(() => {
      expect(mockedApiClient.get).toHaveBeenCalledWith(
        `/api/checkin/event/${mockEventId}/report?startDate=2024-01-15&endDate=2024-01-16`
      );
    });
  });

  it('exports attendance data', async () => {
    // Mock URL.createObjectURL
    global.URL.createObjectURL = jest.fn(() => 'mocked-url');
    global.URL.revokeObjectURL = jest.fn();
    
    // Mock link click
    const mockClick = jest.fn();
    const mockAppendChild = jest.fn();
    const mockRemoveChild = jest.fn();
    
    Object.defineProperty(document, 'createElement', {
      value: jest.fn(() => ({
        href: '',
        download: '',
        click: mockClick,
      }))
    });
    
    Object.defineProperty(document.body, 'appendChild', {
      value: mockAppendChild
    });
    
    Object.defineProperty(document.body, 'removeChild', {
      value: mockRemoveChild
    });

    render(<AttendanceDashboard eventId={mockEventId} />);
    
    await waitFor(() => {
      expect(screen.getByText('Tech Conference 2024')).toBeInTheDocument();
    });

    // Click export button
    fireEvent.click(screen.getByText(/export/i));
    
    await waitFor(() => {
      expect(mockClick).toHaveBeenCalled();
    });
  });

  it('shows appropriate message when no data is available', async () => {
    mockedApiClient.get.mockResolvedValue({
      data: {
        ...mockAttendanceData,
        sessionAttendance: [],
        summary: {
          ...mockAttendanceData.summary,
          totalRegistrations: 0,
          totalAttendees: 0
        }
      }
    });
    
    render(<AttendanceDashboard eventId={mockEventId} />);
    
    await waitFor(() => {
      expect(screen.getByText(/no attendance data available/i)).toBeInTheDocument();
    });
  });

  it('updates data when eventId changes', async () => {
    const { rerender } = render(<AttendanceDashboard eventId={mockEventId} />);
    
    await waitFor(() => {
      expect(mockedApiClient.get).toHaveBeenCalledWith(`/api/checkin/event/${mockEventId}/report`);
    });

    // Change eventId
    const newEventId = 'event-456';
    rerender(<AttendanceDashboard eventId={newEventId} />);
    
    await waitFor(() => {
      expect(mockedApiClient.get).toHaveBeenCalledWith(`/api/checkin/event/${newEventId}/report`);
    });
  });
});