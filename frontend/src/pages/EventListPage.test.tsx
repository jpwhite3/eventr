import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import '@testing-library/jest-dom';
import EventListPage from './EventListPage';
import apiClient from '../api/apiClient';

jest.mock('../api/apiClient');
const mockedApiClient = apiClient as jest.Mocked<typeof apiClient>;

const mockEvents = [
  {
    id: 'event-1',
    title: 'Tech Conference 2025',
    description: 'A comprehensive technology conference featuring the latest innovations in AI, blockchain, and cloud computing.',
    startDateTime: '2025-03-15T09:00:00Z',
    endDateTime: '2025-03-15T17:00:00Z',
    location: 'San Francisco Convention Center',
    city: 'San Francisco',
    category: 'TECHNOLOGY',
    maxCapacity: 500,
    currentRegistrations: 350,
    status: 'PUBLISHED',
    imageUrl: 'https://example.com/tech-conference.jpg'
  },
  {
    id: 'event-2',
    title: 'Business Networking Summit',
    description: 'Connect with industry leaders and expand your professional network.',
    startDateTime: '2025-04-20T10:00:00Z',
    endDateTime: '2025-04-20T16:00:00Z',
    location: 'Downtown Business Center',
    city: 'New York',
    category: 'BUSINESS',
    maxCapacity: 200,
    currentRegistrations: 180,
    status: 'PUBLISHED'
  },
  {
    id: 'event-3',
    title: 'Wellness Workshop',
    description: 'Learn mindfulness techniques and healthy living practices.',
    startDateTime: '2025-05-10T14:00:00Z',
    endDateTime: '2025-05-10T18:00:00Z',
    location: 'Community Health Center',
    city: 'Los Angeles',
    category: 'HEALTH_WELLNESS',
    maxCapacity: 50,
    currentRegistrations: 45,
    status: 'PUBLISHED'
  }
];

const renderEventListPage = () => {
  return render(
    <BrowserRouter>
      <EventListPage />
    </BrowserRouter>
  );
};

describe('EventListPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedApiClient.get.mockResolvedValue({
      data: mockEvents
    });
  });

  it('renders without crashing', async () => {
    renderEventListPage();
    
    await waitFor(() => {
      expect(screen.getByText('Events')).toBeInTheDocument();
    });
  });

  it('displays loading spinner initially', () => {
    renderEventListPage();
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('loads and displays events', async () => {
    renderEventListPage();
    
    await waitFor(() => {
      expect(screen.getByText('Tech Conference 2025')).toBeInTheDocument();
      expect(screen.getByText('Business Networking Summit')).toBeInTheDocument();
      expect(screen.getByText('Wellness Workshop')).toBeInTheDocument();
    });

    expect(mockedApiClient.get).toHaveBeenCalledWith('/events', { params: {} });
  });

  it('displays event details correctly', async () => {
    renderEventListPage();
    
    await waitFor(() => {
      expect(screen.getByText('Tech Conference 2025')).toBeInTheDocument();
    });

    expect(screen.getByText(/A comprehensive technology conference/)).toBeInTheDocument();
    expect(screen.getByText('San Francisco Convention Center')).toBeInTheDocument();
    expect(screen.getByText('350/500 registered')).toBeInTheDocument();
    expect(screen.getAllByText('PUBLISHED')).toHaveLength(3);
  });

  it('handles search functionality', async () => {
    renderEventListPage();
    
    await waitFor(() => {
      expect(screen.getByText('Tech Conference 2025')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText('Search events...');
    fireEvent.change(searchInput, { target: { value: 'Tech' } });
    
    const searchButton = screen.getByTestId('hero-search-button');
    fireEvent.click(searchButton);

    await waitFor(() => {
      expect(mockedApiClient.get).toHaveBeenCalledWith('/events', {
        params: { search: 'Tech' }
      });
    });
  });

  it('handles city filter', async () => {
    renderEventListPage();
    
    await waitFor(() => {
      expect(screen.getByText('Tech Conference 2025')).toBeInTheDocument();
    });

    const citySelect = screen.getByDisplayValue('All Cities');
    fireEvent.change(citySelect, { target: { value: 'San Francisco' } });

    await waitFor(() => {
      expect(mockedApiClient.get).toHaveBeenCalledWith('/events', {
        params: { city: 'San Francisco' }
      });
    });
  });

  it('handles category filter', async () => {
    renderEventListPage();
    
    await waitFor(() => {
      expect(screen.getByText('Tech Conference 2025')).toBeInTheDocument();
    });

    const categorySelect = screen.getByDisplayValue('All');
    fireEvent.change(categorySelect, { target: { value: 'Technology' } });

    await waitFor(() => {
      expect(mockedApiClient.get).toHaveBeenCalledWith('/events', {
        params: { category: 'TECHNOLOGY' }
      });
    });
  });

  it('displays view details and register buttons', async () => {
    renderEventListPage();
    
    await waitFor(() => {
      expect(screen.getByText('Tech Conference 2025')).toBeInTheDocument();
    });

    const viewDetailsButtons = screen.getAllByText('View Details');
    const registerButtons = screen.getAllByText('Register');
    
    expect(viewDetailsButtons).toHaveLength(3);
    expect(registerButtons).toHaveLength(3);

    expect(viewDetailsButtons[0].closest('a')).toHaveAttribute('href', '/events/event-1');
    expect(registerButtons[0].closest('a')).toHaveAttribute('href', '/events/event-1/register');
  });

  it('does not show register button for full events', async () => {
    const fullEvent = {
      ...mockEvents[0],
      currentRegistrations: 500
    };
    
    mockedApiClient.get.mockResolvedValue({
      data: [fullEvent]
    });

    renderEventListPage();
    
    await waitFor(() => {
      expect(screen.getByText('Tech Conference 2025')).toBeInTheDocument();
    });

    expect(screen.getByText('View Details')).toBeInTheDocument();
    expect(screen.queryByText('Register')).not.toBeInTheDocument();
  });

  it('handles API errors gracefully', async () => {
    mockedApiClient.get.mockRejectedValue(new Error('Failed to fetch events'));

    renderEventListPage();

    await waitFor(() => {
      expect(screen.getByText('Failed to load events. Please try again.')).toBeInTheDocument();
    });
  });

  it('shows no events message when no events returned', async () => {
    mockedApiClient.get.mockResolvedValue({
      data: []
    });

    renderEventListPage();

    await waitFor(() => {
      expect(screen.getByText('No events found')).toBeInTheDocument();
      expect(screen.getByText('Try adjusting your search criteria or check back later for new events.')).toBeInTheDocument();
    });
  });

  it('formats dates correctly', async () => {
    renderEventListPage();
    
    await waitFor(() => {
      expect(screen.getByText('Tech Conference 2025')).toBeInTheDocument();
    });

    const formattedDate = screen.getByText(/Sat, Mar 15, 2025/);
    expect(formattedDate).toBeInTheDocument();
  });

  it('displays event images when available', async () => {
    renderEventListPage();
    
    await waitFor(() => {
      expect(screen.getByText('Tech Conference 2025')).toBeInTheDocument();
    });

    const eventImage = screen.getByAltText('Tech Conference 2025');
    expect(eventImage).toBeInTheDocument();
    expect(eventImage).toHaveAttribute('src', 'https://example.com/tech-conference.jpg');
  });

  it('populates city filter options from events', async () => {
    renderEventListPage();
    
    await waitFor(() => {
      expect(screen.getByText('Tech Conference 2025')).toBeInTheDocument();
    });

    const citySelect = screen.getByDisplayValue('All Cities');
    expect(citySelect).toBeInTheDocument();
    
    const options = Array.from(citySelect.querySelectorAll('option')).map(option => option.textContent);
    expect(options).toContain('Los Angeles');
    expect(options).toContain('New York');
    expect(options).toContain('San Francisco');
  });

  it('handles multiple filters simultaneously', async () => {
    renderEventListPage();
    
    await waitFor(() => {
      expect(screen.getByText('Tech Conference 2025')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText('Search events...');
    const citySelect = screen.getByDisplayValue('All Cities');
    const categorySelect = screen.getByDisplayValue('All');

    fireEvent.change(searchInput, { target: { value: 'Conference' } });
    fireEvent.change(citySelect, { target: { value: 'San Francisco' } });
    fireEvent.change(categorySelect, { target: { value: 'Technology' } });

    await waitFor(() => {
      expect(mockedApiClient.get).toHaveBeenCalledWith('/events', {
        params: { 
          search: 'Conference', 
          city: 'San Francisco', 
          category: 'TECHNOLOGY' 
        }
      });
    });
  });

  it('shows loading state during search', async () => {
    renderEventListPage();

    // Wait for initial load to complete
    await waitFor(() => {
      expect(screen.getByText('Tech Conference 2025')).toBeInTheDocument();
    });

    // Setup mock to simulate slow response on next call
    let resolveApiCall: (value: any) => void;
    mockedApiClient.get.mockImplementation(() => 
      new Promise(resolve => {
        resolveApiCall = resolve;
      })
    );

    const searchButton = screen.getByTestId('hero-search-button');
    fireEvent.click(searchButton);

    // Should show loading state immediately
    expect(screen.getByText('Searching...')).toBeInTheDocument();
    expect(searchButton).toBeDisabled();
    
    // Clean up by resolving the promise
    if (resolveApiCall!) {
      resolveApiCall({ data: mockEvents });
    }
  });
});