import React, { useState, useEffect, useCallback } from 'react';
import {
  CRow,
  CCol,
  CCard,
  CCardBody,
  CCardHeader,
  CButton,
  CForm,
  CFormInput,
  CFormSelect,
  CBadge,
  CSpinner
} from '@coreui/react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faCalendarAlt,
  faMapMarkerAlt,
  faUsers,
  faSearch,
  faFilter
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
        const uniqueCities = [...new Set(response.data.map((event: Event) => event.city))];
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
        <CSpinner />
      </div>
    );
  }

  return (
    <div>
      <CRow className="mb-4">
        <CCol>
          <CCard>
            <CCardHeader>
              <h2 className="mb-0">
                <FontAwesomeIcon icon={faCalendarAlt} className="me-2" />
                Events
              </h2>
            </CCardHeader>
            <CCardBody>
              <CForm className="row g-3">
                <CCol md={4}>
                  <CFormInput
                    type="text"
                    placeholder="Search events..."
                    value={filters.search}
                    onChange={(e) => handleFilterChange('search', e.target.value)}
                  />
                </CCol>
                <CCol md={3}>
                  <CFormSelect
                    value={filters.city}
                    onChange={(e) => handleFilterChange('city', e.target.value)}
                  >
                    <option value="">All Cities</option>
                    {cities.map(city => (
                      <option key={city} value={city}>{city}</option>
                    ))}
                  </CFormSelect>
                </CCol>
                <CCol md={3}>
                  <CFormSelect
                    value={filters.category}
                    onChange={(e) => handleFilterChange('category', e.target.value)}
                  >
                    {categories.map(category => (
                      <option key={category} value={category}>{category}</option>
                    ))}
                  </CFormSelect>
                </CCol>
                <CCol md={2}>
                  <CButton color="primary" onClick={fetchEvents} disabled={loading}>
                    <FontAwesomeIcon icon={loading ? faFilter : faSearch} className="me-1" />
                    {loading ? 'Searching...' : 'Search'}
                  </CButton>
                </CCol>
              </CForm>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

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
          events.map(event => (
            <CCol key={event.id} xs={12} sm={6} lg={4} className="mb-4">
              <CCard className="h-100 shadow-sm">
                {event.imageUrl && (
                  <img
                    src={event.imageUrl}
                    alt={event.title}
                    className="card-img-top"
                    style={{ height: '200px', objectFit: 'cover' }}
                  />
                )}
                <CCardHeader className="d-flex justify-content-between align-items-start">
                  <h6 className="mb-0 flex-grow-1">{event.title}</h6>
                  {getStatusBadge(event.status)}
                </CCardHeader>
                <CCardBody className="d-flex flex-column">
                  <p className="text-muted small mb-2 flex-grow-1">
                    {event.description.length > 100 
                      ? `${event.description.substring(0, 100)}...` 
                      : event.description
                    }
                  </p>
                  
                  <div className="mb-2">
                    <small className="text-muted">
                      <FontAwesomeIcon icon={faCalendarAlt} className="me-1" />
                      {formatDate(event.startDateTime)}
                    </small>
                  </div>
                  
                  <div className="mb-2">
                    <small className="text-muted">
                      <FontAwesomeIcon icon={faMapMarkerAlt} className="me-1" />
                      {event.location}
                    </small>
                  </div>
                  
                  <div className="mb-3">
                    <small className="text-muted">
                      <FontAwesomeIcon icon={faUsers} className="me-1" />
                      {event.currentRegistrations}/{event.maxCapacity} registered
                    </small>
                  </div>
                  
                  <div className="mt-auto d-flex gap-2">
                    <Link to={`/events/${event.id}`} className="btn btn-primary btn-sm flex-grow-1">
                      View Details
                    </Link>
                    {event.status === 'PUBLISHED' && event.currentRegistrations < event.maxCapacity && (
                      <Link to={`/events/${event.id}/register`} className="btn btn-success btn-sm">
                        Register
                      </Link>
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