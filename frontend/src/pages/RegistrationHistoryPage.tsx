import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../hooks/useAuth';
import { Link } from 'react-router-dom';
import apiClient from '../api/apiClient';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faCalendarAlt,
  faMapMarkerAlt,
  faClock,
  faCheckCircle,
  faTimesCircle,
  faSpinner,
  faEye,
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
  const { isAuthenticated, user } = useAuth();
  const [registrations, setRegistrations] = useState<Registration[]>([]);
  const [filteredRegistrations, setFilteredRegistrations] = useState<Registration[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [dateFilter, setDateFilter] = useState('ALL');

  const applyFilters = useCallback(() => {
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
  }, [registrations, searchQuery, statusFilter, dateFilter]);

  const fetchRegistrations = useCallback(async () => {
    try {
      setLoading(true);
      
      if (!user?.id) {
        console.error('User ID not available');
        setRegistrations([]);
        return;
      }

      // Fetch user's registrations from API
      const response = await apiClient.get(`/registrations/user/id/${user.id}`);
      
      // Transform the response to match our interface
      const transformedRegistrations: Registration[] = response.data.map((reg: any) => ({
        id: reg.id,
        event: {
          id: reg.eventInstance?.event?.id || reg.eventInstance?.id || 'unknown',
          title: reg.eventInstance?.event?.name || 'Event',
          description: reg.eventInstance?.event?.description || '',
          startDateTime: reg.eventInstance?.event?.startDateTime || reg.eventInstance?.startDateTime || new Date().toISOString(),
          endDateTime: reg.eventInstance?.event?.endDateTime || reg.eventInstance?.endDateTime || new Date().toISOString(),
          location: reg.eventInstance?.event?.location?.address || 'Location TBD',
          imageUrl: reg.eventInstance?.event?.bannerImageUrl,
          status: reg.eventInstance?.event?.status || 'UNKNOWN'
        },
        registrationDateTime: reg.createdAt || new Date().toISOString(),
        status: reg.status === 'REGISTERED' ? 'CONFIRMED' : (reg.status || 'CONFIRMED'),
        checkInDateTime: reg.checkedIn ? reg.updatedAt : undefined
      }));

      setRegistrations(transformedRegistrations);
    } catch (error) {
      console.error('Error fetching registrations:', error);
      setRegistrations([]);
    } finally {
      setLoading(false);
    }
  }, [user?.id]);

  useEffect(() => {
    if (isAuthenticated) {
      fetchRegistrations();
    }
  }, [isAuthenticated, fetchRegistrations]);

  useEffect(() => {
    applyFilters();
  }, [applyFilters]);



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
              Browse Events
            </Link>
          )}
        </div>
      )}
    </div>
  );
};

export default RegistrationHistoryPage;