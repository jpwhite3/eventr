import React, { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import { Link } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faCalendarAlt,
  faMapMarkerAlt,
  faClock,
  faUsers,
  faCheckCircle,
  faTimesCircle,
  faSpinner,
  faEye,
  faFilter,
  faDownload,
  faSearch
} from '@fortawesome/free-solid-svg-icons';
import './RegistrationHistoryPage.css';

interface Registration {
  id: string;
  event: {
    id: string;
    title: string;
    description: string;
    startDateTime: string;
    endDateTime: string;
    location: string;
    imageUrl?: string;
    status: string;
  };
  registrationDateTime: string;
  status: 'CONFIRMED' | 'WAITLISTED' | 'CANCELLED' | 'ATTENDED' | 'NO_SHOW';
  checkInDateTime?: string;
  qrCode?: string;
}

const RegistrationHistoryPage: React.FC = () => {
  const { user, isAuthenticated } = useAuth();
  const [registrations, setRegistrations] = useState<Registration[]>([]);
  const [filteredRegistrations, setFilteredRegistrations] = useState<Registration[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [dateFilter, setDateFilter] = useState('ALL');

  useEffect(() => {
    if (isAuthenticated) {
      fetchRegistrations();
    }
  }, [isAuthenticated]);

  useEffect(() => {
    applyFilters();
  }, [registrations, searchQuery, statusFilter, dateFilter]);

  const fetchRegistrations = async () => {
    try {
      setLoading(true);
      
      // Mock data for demonstration
      const mockRegistrations: Registration[] = [
        {
          id: '1',
          event: {
            id: '1',
            title: 'React Conference 2024',
            description: 'Learn the latest React features and best practices',
            startDateTime: '2024-03-15T09:00:00',
            endDateTime: '2024-03-15T17:00:00',
            location: 'San Francisco Convention Center',
            imageUrl: 'https://via.placeholder.com/100x60?text=React',
            status: 'PUBLISHED'
          },
          registrationDateTime: '2024-02-20T14:30:00',
          status: 'CONFIRMED',
          checkInDateTime: undefined
        },
        {
          id: '2',
          event: {
            id: '2',
            title: 'AI/ML Workshop',
            description: 'Hands-on workshop on machine learning fundamentals',
            startDateTime: '2024-03-20T10:00:00',
            endDateTime: '2024-03-20T16:00:00',
            location: 'Tech Hub Downtown',
            imageUrl: 'https://via.placeholder.com/100x60?text=AI',
            status: 'PUBLISHED'
          },
          registrationDateTime: '2024-02-25T11:15:00',
          status: 'WAITLISTED'
        },
        {
          id: '3',
          event: {
            id: '3',
            title: 'Web Development Bootcamp',
            description: 'Intensive 3-day bootcamp covering full-stack development',
            startDateTime: '2024-02-10T09:00:00',
            endDateTime: '2024-02-12T17:00:00',
            location: 'Innovation Center',
            imageUrl: 'https://via.placeholder.com/100x60?text=Bootcamp',
            status: 'COMPLETED'
          },
          registrationDateTime: '2024-01-15T16:45:00',
          status: 'ATTENDED',
          checkInDateTime: '2024-02-10T08:45:00'
        },
        {
          id: '4',
          event: {
            id: '4',
            title: 'UX Design Principles',
            description: 'Learn modern UX design principles and practices',
            startDateTime: '2024-01-20T13:00:00',
            endDateTime: '2024-01-20T17:00:00',
            location: 'Design Studio',
            imageUrl: 'https://via.placeholder.com/100x60?text=UX',
            status: 'COMPLETED'
          },
          registrationDateTime: '2024-01-05T09:30:00',
          status: 'NO_SHOW'
        },
        {
          id: '5',
          event: {
            id: '5',
            title: 'Startup Pitch Competition',
            description: 'Present your startup idea to industry experts',
            startDateTime: '2024-02-28T18:00:00',
            endDateTime: '2024-02-28T21:00:00',
            location: 'Entrepreneur Hub',
            imageUrl: 'https://via.placeholder.com/100x60?text=Startup',
            status: 'CANCELLED'
          },
          registrationDateTime: '2024-02-15T12:00:00',
          status: 'CANCELLED'
        }
      ];

      setRegistrations(mockRegistrations);
    } catch (error) {
      console.error('Error fetching registrations:', error);
    } finally {
      setLoading(false);
    }
  };

  const applyFilters = () => {
    let filtered = registrations;

    // Search filter
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(reg =>
        reg.event.title.toLowerCase().includes(query) ||
        reg.event.location.toLowerCase().includes(query) ||
        reg.event.description.toLowerCase().includes(query)
      );
    }

    // Status filter
    if (statusFilter !== 'ALL') {
      filtered = filtered.filter(reg => reg.status === statusFilter);
    }

    // Date filter
    if (dateFilter !== 'ALL') {
      const now = new Date();
      filtered = filtered.filter(reg => {
        const eventDate = new Date(reg.event.startDateTime);
        switch (dateFilter) {
          case 'UPCOMING':
            return eventDate > now;
          case 'PAST':
            return eventDate < now;
          case 'THIS_MONTH':
            return eventDate.getMonth() === now.getMonth() && 
                   eventDate.getFullYear() === now.getFullYear();
          default:
            return true;
        }
      });
    }

    setFilteredRegistrations(filtered);
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'CONFIRMED':
        return 'success';
      case 'WAITLISTED':
        return 'warning';
      case 'CANCELLED':
        return 'danger';
      case 'ATTENDED':
        return 'primary';
      case 'NO_SHOW':
        return 'secondary';
      default:
        return 'secondary';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'CONFIRMED':
        return faCheckCircle;
      case 'WAITLISTED':
        return faSpinner;
      case 'CANCELLED':
        return faTimesCircle;
      case 'ATTENDED':
        return faCheckCircle;
      case 'NO_SHOW':
        return faTimesCircle;
      default:
        return faSpinner;
    }
  };

  const exportRegistrations = () => {
    // Mock export functionality
    const csvContent = [
      ['Event Title', 'Location', 'Event Date', 'Registration Date', 'Status'],
      ...filteredRegistrations.map(reg => [
        reg.event.title,
        reg.event.location,
        formatDate(reg.event.startDateTime),
        formatDate(reg.registrationDateTime),
        reg.status
      ])
    ].map(row => row.join(',')).join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'registration-history.csv';
    a.click();
    window.URL.revokeObjectURL(url);
  };

  if (!isAuthenticated) {
    return (
      <div className="registration-history-container">
        <div className="auth-required">
          <h2>Authentication Required</h2>
          <p>Please sign in to view your registration history</p>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="registration-history-container">
        <div className="loading-state">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="mt-3">Loading your registration history...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="registration-history-container">
      {/* Header */}
      <div className="page-header">
        <div className="header-content">
          <h1>Registration History</h1>
          <p className="text-muted">
            View and manage your event registrations
          </p>
        </div>
        <div className="header-actions">
          <button 
            className="btn btn-outline-primary"
            onClick={exportRegistrations}
            disabled={filteredRegistrations.length === 0}
          >
            <FontAwesomeIcon icon={faDownload} className="me-2" />
            Export
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="filters-section">
        <div className="row g-3">
          <div className="col-md-4">
            <div className="search-input">
              <FontAwesomeIcon icon={faSearch} className="search-icon" />
              <input
                type="text"
                className="form-control"
                placeholder="Search events..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
          </div>
          <div className="col-md-4">
            <select
              className="form-select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="ALL">All Statuses</option>
              <option value="CONFIRMED">Confirmed</option>
              <option value="WAITLISTED">Waitlisted</option>
              <option value="ATTENDED">Attended</option>
              <option value="NO_SHOW">No Show</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>
          <div className="col-md-4">
            <select
              className="form-select"
              value={dateFilter}
              onChange={(e) => setDateFilter(e.target.value)}
            >
              <option value="ALL">All Events</option>
              <option value="UPCOMING">Upcoming</option>
              <option value="PAST">Past Events</option>
              <option value="THIS_MONTH">This Month</option>
            </select>
          </div>
        </div>
      </div>

      {/* Results Summary */}
      <div className="results-summary">
        <span className="text-muted">
          Showing {filteredRegistrations.length} of {registrations.length} registrations
        </span>
      </div>

      {/* Registration List */}
      {filteredRegistrations.length > 0 ? (
        <div className="registration-list">
          {filteredRegistrations.map((registration) => (
            <div key={registration.id} className="registration-card">
              <div className="registration-content">
                {registration.event.imageUrl && (
                  <div className="event-thumbnail">
                    <img 
                      src={registration.event.imageUrl} 
                      alt={registration.event.title} 
                    />
                  </div>
                )}
                
                <div className="event-info">
                  <div className="event-header">
                    <h4>{registration.event.title}</h4>
                    <span className={`status-badge bg-${getStatusColor(registration.status)}`}>
                      <FontAwesomeIcon 
                        icon={getStatusIcon(registration.status)} 
                        className="me-1" 
                      />
                      {registration.status.replace('_', ' ')}
                    </span>
                  </div>
                  
                  <p className="event-description">
                    {registration.event.description}
                  </p>
                  
                  <div className="event-details">
                    <div className="detail-item">
                      <FontAwesomeIcon icon={faClock} className="text-muted" />
                      <span>{formatDate(registration.event.startDateTime)}</span>
                    </div>
                    <div className="detail-item">
                      <FontAwesomeIcon icon={faMapMarkerAlt} className="text-muted" />
                      <span>{registration.event.location}</span>
                    </div>
                  </div>
                  
                  <div className="registration-meta">
                    <small className="text-muted">
                      Registered: {formatDate(registration.registrationDateTime)}
                    </small>
                    {registration.checkInDateTime && (
                      <small className="text-success">
                        Checked in: {formatDate(registration.checkInDateTime)}
                      </small>
                    )}
                  </div>
                </div>
                
                <div className="registration-actions">
                  <Link 
                    to={`/events/${registration.event.id}`} 
                    className="btn btn-outline-primary btn-sm"
                  >
                    <FontAwesomeIcon icon={faEye} className="me-1" />
                    View Event
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="empty-state">
          <FontAwesomeIcon icon={faCalendarAlt} size="3x" className="text-muted mb-3" />
          <h4>No Registrations Found</h4>
          <p className="text-muted">
            {searchQuery || statusFilter !== 'ALL' || dateFilter !== 'ALL'
              ? 'No registrations match your current filters.'
              : 'You haven\'t registered for any events yet.'
            }
          </p>
          {(!searchQuery && statusFilter === 'ALL' && dateFilter === 'ALL') && (
            <Link to="/" className="btn btn-primary">
              <FontAwesomeIcon icon={faCalendarAlt} className="me-2" />
              Discover Events
            </Link>
          )}
        </div>
      )}
    </div>
  );
};

export default RegistrationHistoryPage;