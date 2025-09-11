import React, { useState, useEffect, useCallback } from 'react';
import {
  CRow,
  CCol,
  CCard,
  CCardBody,
  CCardHeader,
  CButton,
  CBadge
} from '@coreui/react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faCalendarAlt,
  faMapMarkerAlt,
  faUsers,
  faSearch,
  faFilter,
  faUserCheck
} from '@fortawesome/free-solid-svg-icons';
import { Link } from 'react-router-dom';
import apiClient from '../api/apiClient';

interface Event {
  id: string;
  title: string;
  description: string;
  startDateTime: string;
  endDateTime: string;
  location: string;
  city: string;
  category: string;
  maxCapacity: number;
  currentRegistrations: number;
  status: string;
  imageUrl?: string;
}

interface Filters {
  city: string;
  category: string;
  search: string;
}

const EventListPage: React.FC = () => {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filters, setFilters] = useState<Filters>({
    city: '',
    category: 'All',
    search: ''
  });
  const [cities, setCities] = useState<string[]>([]);
  const [categories] = useState([
    'All',
    'Technology',
    'Business',
    'Health & Wellness',
    'Arts & Culture',
    'Sports & Recreation',
    'Education',
    'Networking',
    'Entertainment'
  ]);

  const fetchEvents = useCallback(() => {
    setLoading(true);
    const params: any = {};
    if (filters.city) params.city = filters.city;
    if (filters.category && filters.category !== 'All') {
      params.category = filters.category.toUpperCase().replace(/\s+/g, '_').replace('&', '');
    }
    if (filters.search) params.search = filters.search;

    apiClient.get('/events', { params })
      .then(response => {
        setEvents(response.data);
        const uniqueCities = Array.from(new Set(response.data.map((event: Event) => event.city))) as string[];
        setCities(uniqueCities.sort());
        setError('');
      })
      .catch(error => {
        console.error('Error fetching events:', error);
        setError('Failed to load events. Please try again.');
      })
      .finally(() => {
        setLoading(false);
      });
  }, [filters]);

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  const handleFilterChange = (key: keyof Filters, value: string) => {
    setFilters(prev => ({
      ...prev,
      [key]: value
    }));
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStatusBadge = (status: string) => {
    const statusColors: { [key: string]: string } = {
      'PUBLISHED': 'success',
      'DRAFT': 'secondary',
      'CANCELLED': 'danger',
      'COMPLETED': 'info'
    };
    return (
      <CBadge color={statusColors[status] || 'secondary'}>
        {status}
      </CBadge>
    );
  };

  if (loading && events.length === 0) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '400px' }}>
        <div className="spinner-border" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    );
  }

  return (
    <div>
      {/* Modern Hero Section */}
      <div className="hero-section">
        <div className="container-fluid">
          <div className="row align-items-center">
            <div className="col-lg-8">
              <h1 className="mb-3">
                <FontAwesomeIcon icon={faCalendarAlt} className="me-3" />
                Corporate Events
              </h1>
              <p className="lead mb-0">
                Browse and register for company events
              </p>
            </div>
            <div className="col-lg-4 text-lg-end mt-4 mt-lg-0">
              <div className="bg-white rounded-modern p-4 shadow-modern">
                <input
                  type="text"
                  className="form-control mb-3"
                  placeholder="Search events..."
                  value={filters.search}
                  onChange={(e) => handleFilterChange('search', e.target.value)}
                />
                <CButton 
                  color="primary" 
                  className="w-100" 
                  onClick={fetchEvents} 
                  disabled={loading}
                >
                  <FontAwesomeIcon icon={faSearch} className="me-2" />
                  Search Events
                </CButton>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Category Browse Section */}
      <div className="mb-5">
        <h3 className="text-center mb-4 text-gradient">Browse by category</h3>
        <div className="category-grid">
          {[
            { name: 'All', icon: '🎯', color: 'primary' },
            { name: 'Business', icon: '💼', color: 'success' },
            { name: 'Technology', icon: '💻', color: 'info' },
            { name: 'Education', icon: '📚', color: 'warning' },
            { name: 'Community', icon: '👥', color: 'secondary' },
            { name: 'Health & Wellness', icon: '🏥', color: 'danger' },
            { name: 'Food & Drink', icon: '🍽️', color: 'primary' },
            { name: 'Sports & Fitness', icon: '⚽', color: 'success' },
            { name: 'Other', icon: '📦', color: 'secondary' }
          ].map(category => (
            <div 
              key={category.name}
              className={`category-item ${
                filters.category === category.name ? 'border-primary' : ''
              }`}
              onClick={() => handleFilterChange('category', category.name)}
            >
              <div className="icon">
                {category.icon}
              </div>
              <h6>{category.name}</h6>
            </div>
          ))}
        </div>
      </div>

      {/* Advanced Filters Section */}
      <div className="search-section mb-4">
        <div className="d-flex align-items-center justify-content-between mb-3">
          <h4 className="mb-0">
            <FontAwesomeIcon icon={faFilter} className="me-2" />
            Search Filters
          </h4>
          <small className="text-muted">Showing {events.length} events</small>
        </div>
        <form className="row g-3">
          <CCol md={6}>
            <label className="form-label fw-bold text-secondary">Location</label>
            <select
              className="form-select"
              value={filters.city}
              onChange={(e) => handleFilterChange('city', e.target.value)}
            >
              <option value="">Browse All Cities</option>
              {cities.map(city => (
                <option key={city} value={city}>{city}</option>
              ))}
            </select>
          </CCol>
          <CCol md={4}>
            <label className="form-label fw-bold text-secondary">Event Type</label>
            <select
              className="form-select"
              value={filters.category}
              onChange={(e) => handleFilterChange('category', e.target.value)}
            >
              {categories.map(category => (
                <option key={category} value={category}>{category}</option>
              ))}
            </select>
          </CCol>
          <CCol md={2}>
            <label className="form-label fw-bold text-secondary">&nbsp;</label>
            <CButton 
              color="primary" 
              className="w-100 d-block" 
              onClick={fetchEvents} 
              disabled={loading}
            >
              <FontAwesomeIcon icon={loading ? faFilter : faSearch} className="me-1" spin={loading} />
              Apply Filters
            </CButton>
          </CCol>
        </form>
      </div>

      {error && (
        <CRow className="mb-4">
          <CCol>
            <div className="alert alert-danger" role="alert">
              {error}
            </div>
          </CCol>
        </CRow>
      )}

      <CRow>
        {events.length === 0 && !loading ? (
          <CCol>
            <div className="text-center py-5">
              <FontAwesomeIcon icon={faCalendarAlt} size="3x" className="text-muted mb-3" />
              <h5 className="text-muted">No events found</h5>
              <p className="text-muted">Try adjusting your search criteria or check back later for new events.</p>
            </div>
          </CCol>
        ) : (
          events.map((event, index) => (
            <CCol key={event.id} xs={12} sm={6} lg={4} xl={3} className="mb-4">
              <CCard className="event-card animate-fade-scale" style={{ animationDelay: `${index * 0.1}s` }}>
                {event.imageUrl ? (
                  <img
                    src={event.imageUrl}
                    alt={event.title}
                    className="card-img-top"
                  />
                ) : (
                  <div 
                    className="card-img-top d-flex align-items-center justify-content-center bg-gradient-primary text-white"
                    style={{ height: '220px' }}
                  >
                    <FontAwesomeIcon icon={faCalendarAlt} size="3x" style={{ opacity: 0.3 }} />
                  </div>
                )}
                <CCardHeader>
                  <div className="d-flex justify-content-between align-items-start">
                    <h6 className="mb-0 flex-grow-1 fw-bold" style={{ color: 'var(--eventr-text-primary)' }}>
                      {event.title}
                    </h6>
                    {getStatusBadge(event.status)}
                  </div>
                </CCardHeader>
                <CCardBody>
                  <p className="text-secondary mb-3 small" style={{ lineHeight: 1.5 }}>
                    {event.description.length > 120 
                      ? `${event.description.substring(0, 120)}...` 
                      : event.description
                    }
                  </p>
                  
                  <div className="mb-2 d-flex align-items-center">
                    <div className="rounded-circle bg-light p-2 me-3 d-flex align-items-center justify-content-center" style={{ width: '32px', height: '32px' }}>
                      <FontAwesomeIcon icon={faCalendarAlt} className="text-primary" size="sm" />
                    </div>
                    <small className="fw-medium" style={{ color: 'var(--eventr-text-secondary)' }}>
                      {formatDate(event.startDateTime)}
                    </small>
                  </div>
                  
                  <div className="mb-2 d-flex align-items-center">
                    <div className="rounded-circle bg-light p-2 me-3 d-flex align-items-center justify-content-center" style={{ width: '32px', height: '32px' }}>
                      <FontAwesomeIcon icon={faMapMarkerAlt} className="text-success" size="sm" />
                    </div>
                    <small className="fw-medium" style={{ color: 'var(--eventr-text-secondary)' }}>
                      {event.location}
                    </small>
                  </div>
                  
                  <div className="mb-4 d-flex align-items-center">
                    <div className="rounded-circle bg-light p-2 me-3 d-flex align-items-center justify-content-center" style={{ width: '32px', height: '32px' }}>
                      <FontAwesomeIcon icon={faUsers} className="text-warning" size="sm" />
                    </div>
                    <div className="flex-grow-1">
                      <small className="fw-medium d-block" style={{ color: 'var(--eventr-text-secondary)' }}>
                        {event.currentRegistrations}/{event.maxCapacity} registered
                      </small>
                      <div className="progress mt-1" style={{ height: '4px' }}>
                        <div 
                          className="progress-bar bg-warning" 
                          style={{ width: `${(event.currentRegistrations / event.maxCapacity) * 100}%` }}
                        ></div>
                      </div>
                    </div>
                  </div>
                  
                  <div className="d-grid gap-2 d-md-flex">
                    <Link to={`/events/${event.id}`} className="btn btn-outline-primary btn-sm flex-md-grow-1">
                      View Details
                    </Link>
                    {event.status === 'PUBLISHED' && event.currentRegistrations < event.maxCapacity ? (
                      <Link to={`/events/${event.id}/register`} className="btn btn-success btn-sm">
                        <FontAwesomeIcon icon={faUserCheck} className="me-1" />
                        Register Now
                      </Link>
                    ) : (
                      <button className="btn btn-secondary btn-sm" disabled>
                        {event.currentRegistrations >= event.maxCapacity ? 'Full' : 'Unavailable'}
                      </button>
                    )}
                  </div>
                </CCardBody>
              </CCard>
            </CCol>
          ))
        )}
      </CRow>
    </div>
  );
};

export default EventListPage;