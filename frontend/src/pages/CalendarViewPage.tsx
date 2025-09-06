import React, { useState, useEffect } from 'react';
import { Calendar, momentLocalizer, View, Event as CalendarEvent } from 'react-big-calendar';
import moment from 'moment';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { 
  faCalendar, 
  faList, 
  faFilter,
  faDownload,
  faSync,
  faEye
} from '@fortawesome/free-solid-svg-icons';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import apiClient from '../api/apiClient';
import CalendarIntegration from '../components/CalendarIntegration';

const localizer = momentLocalizer(moment);

interface EventData {
  id: string;
  name: string;
  description?: string;
  startDateTime: string;
  endDateTime: string;
  location?: string;
  eventType?: string;
  category?: string;
  status?: string;
  registrationStatus?: 'registered' | 'attended' | 'no_show';
  organizerName?: string;
  organizerEmail?: string;
}

interface CalendarEventData extends CalendarEvent {
  resource: EventData;
}

const CalendarViewPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [events, setEvents] = useState<EventData[]>([]);
  const [filteredEvents, setFilteredEvents] = useState<EventData[]>([]);
  const [loading, setLoading] = useState(true);
  const [view, setView] = useState<'calendar' | 'list'>('calendar');
  const [calendarView, setCalendarView] = useState<View>('month');
  const [date, setDate] = useState(new Date());
  const [filters, setFilters] = useState({
    eventType: '',
    category: '',
    status: '',
    registrationStatus: ''
  });
  const [showFilters, setShowFilters] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState<EventData | null>(null);
  const [showEventModal, setShowEventModal] = useState(false);

  useEffect(() => {
    if (user) {
      loadUserEvents();
    }
  }, [user]);

  useEffect(() => {
    applyFilters();
  }, [events, filters]);

  const loadUserEvents = async () => {
    if (!user) return;
    
    setLoading(true);
    try {
      const response = await apiClient.get(`/users/${user.id}/events`);
      setEvents(response.data);
    } catch (error) {
      console.error('Failed to load user events:', error);
    } finally {
      setLoading(false);
    }
  };

  const applyFilters = () => {
    let filtered = [...events];

    if (filters.eventType) {
      filtered = filtered.filter(event => event.eventType === filters.eventType);
    }
    if (filters.category) {
      filtered = filtered.filter(event => event.category === filters.category);
    }
    if (filters.status) {
      filtered = filtered.filter(event => event.status === filters.status);
    }
    if (filters.registrationStatus) {
      filtered = filtered.filter(event => event.registrationStatus === filters.registrationStatus);
    }

    setFilteredEvents(filtered);
  };

  const transformEventsForCalendar = (): CalendarEventData[] => {
    return filteredEvents.map(event => ({
      id: event.id,
      title: event.name,
      start: new Date(event.startDateTime),
      end: new Date(event.endDateTime),
      resource: event
    }));
  };

  const getEventTypeColor = (eventType?: string) => {
    switch (eventType) {
      case 'VIRTUAL': return '#007bff';
      case 'HYBRID': return '#fd7e14';
      case 'IN_PERSON': return '#28a745';
      default: return '#6c757d';
    }
  };

  const getRegistrationStatusBadge = (status?: string) => {
    switch (status) {
      case 'registered':
        return <span className="badge bg-primary">Registered</span>;
      case 'attended':
        return <span className="badge bg-success">Attended</span>;
      case 'no_show':
        return <span className="badge bg-warning">No Show</span>;
      default:
        return <span className="badge bg-secondary">Unknown</span>;
    }
  };

  const handleEventClick = (event: CalendarEventData) => {
    setSelectedEvent(event.resource);
    setShowEventModal(true);
  };

  const handleEventDoubleClick = (event: CalendarEventData) => {
    navigate(`/events/${event.resource.id}`);
  };

  const eventStyleGetter = (event: CalendarEventData) => {
    const backgroundColor = getEventTypeColor(event.resource.eventType);
    return {
      style: {
        backgroundColor,
        borderRadius: '4px',
        opacity: 0.8,
        color: 'white',
        border: '0px',
        display: 'block'
      }
    };
  };

  const formatEventTime = (dateTime: string) => {
    return new Date(dateTime).toLocaleString(undefined, {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getUniqueValues = (field: keyof EventData) => {
    const values = events
      .map(event => event[field])
      .filter((value, index, arr) => value && arr.indexOf(value) === index);
    return values;
  };

  if (!user) {
    return (
      <div className="container mt-5 text-center">
        <h3>Please log in to view your calendar</h3>
        <p>You need to be logged in to see your registered events.</p>
      </div>
    );
  }

  return (
    <div className="calendar-view-page">
      {/* Header */}
      <div className="container-fluid py-4 bg-light">
        <div className="container">
          <div className="row align-items-center">
            <div className="col-md-6">
              <h1 className="h2 mb-0">
                <FontAwesomeIcon icon={faCalendar} className="me-2" />
                My Calendar
              </h1>
              <p className="text-muted mb-0">
                {filteredEvents.length} event{filteredEvents.length !== 1 ? 's' : ''} found
              </p>
            </div>
            <div className="col-md-6 text-md-end">
              <div className="btn-group me-2" role="group">
                <button
                  type="button"
                  className={`btn ${view === 'calendar' ? 'btn-primary' : 'btn-outline-primary'}`}
                  onClick={() => setView('calendar')}
                >
                  <FontAwesomeIcon icon={faCalendar} className="me-1" />
                  Calendar
                </button>
                <button
                  type="button"
                  className={`btn ${view === 'list' ? 'btn-primary' : 'btn-outline-primary'}`}
                  onClick={() => setView('list')}
                >
                  <FontAwesomeIcon icon={faList} className="me-1" />
                  List
                </button>
              </div>
              <button
                className="btn btn-outline-secondary me-2"
                onClick={() => setShowFilters(!showFilters)}
              >
                <FontAwesomeIcon icon={faFilter} className="me-1" />
                Filters
              </button>
              <button
                className="btn btn-outline-secondary"
                onClick={loadUserEvents}
                disabled={loading}
              >
                <FontAwesomeIcon icon={faSync} className={`me-1 ${loading ? 'fa-spin' : ''}`} />
                Refresh
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="container py-4">
        {/* Filters */}
        {showFilters && (
          <div className="card mb-4">
            <div className="card-body">
              <h5 className="card-title">Filters</h5>
              <div className="row">
                <div className="col-md-3">
                  <label className="form-label">Event Type</label>
                  <select
                    className="form-select"
                    value={filters.eventType}
                    onChange={(e) => setFilters(prev => ({ ...prev, eventType: e.target.value }))}
                  >
                    <option value="">All Types</option>
                    {getUniqueValues('eventType').map(type => (
                      <option key={type as string} value={type as string}>
                        {(type as string).replace('_', ' ')}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="col-md-3">
                  <label className="form-label">Category</label>
                  <select
                    className="form-select"
                    value={filters.category}
                    onChange={(e) => setFilters(prev => ({ ...prev, category: e.target.value }))}
                  >
                    <option value="">All Categories</option>
                    {getUniqueValues('category').map(category => (
                      <option key={category as string} value={category as string}>
                        {(category as string).replace(/_/g, ' & ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="col-md-3">
                  <label className="form-label">Event Status</label>
                  <select
                    className="form-select"
                    value={filters.status}
                    onChange={(e) => setFilters(prev => ({ ...prev, status: e.target.value }))}
                  >
                    <option value="">All Statuses</option>
                    {getUniqueValues('status').map(status => (
                      <option key={status as string} value={status as string}>
                        {(status as string).replace('_', ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="col-md-3">
                  <label className="form-label">Registration Status</label>
                  <select
                    className="form-select"
                    value={filters.registrationStatus}
                    onChange={(e) => setFilters(prev => ({ ...prev, registrationStatus: e.target.value }))}
                  >
                    <option value="">All Statuses</option>
                    <option value="registered">Registered</option>
                    <option value="attended">Attended</option>
                    <option value="no_show">No Show</option>
                  </select>
                </div>
              </div>
              <div className="row mt-3">
                <div className="col">
                  <button
                    className="btn btn-outline-secondary"
                    onClick={() => setFilters({ eventType: '', category: '', status: '', registrationStatus: '' })}
                  >
                    Clear Filters
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {loading ? (
          <div className="text-center py-5">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading events...</span>
            </div>
          </div>
        ) : (
          <>
            {/* Calendar View */}
            {view === 'calendar' && (
              <div className="card">
                <div className="card-body" style={{ height: '600px' }}>
                  <Calendar
                    localizer={localizer}
                    events={transformEventsForCalendar()}
                    startAccessor="start"
                    endAccessor="end"
                    style={{ height: '100%' }}
                    view={calendarView}
                    date={date}
                    onView={setCalendarView}
                    onNavigate={setDate}
                    onSelectEvent={handleEventClick}
                    onDoubleClickEvent={handleEventDoubleClick}
                    eventPropGetter={eventStyleGetter}
                    popup
                    tooltipAccessor={(event: CalendarEventData) => 
                      `${event.resource.name}\n${formatEventTime(event.resource.startDateTime)}`
                    }
                  />
                </div>
              </div>
            )}

            {/* List View */}
            {view === 'list' && (
              <div className="row">
                {filteredEvents.length === 0 ? (
                  <div className="col-12 text-center py-5">
                    <h4>No events found</h4>
                    <p className="text-muted">Try adjusting your filters or register for some events!</p>
                  </div>
                ) : (
                  filteredEvents.map(event => (
                    <div key={event.id} className="col-12 mb-3">
                      <div className="card">
                        <div className="card-body">
                          <div className="row align-items-center">
                            <div className="col-md-8">
                              <h5 className="card-title">{event.name}</h5>
                              <p className="card-text text-muted mb-1">
                                {formatEventTime(event.startDateTime)}
                              </p>
                              {event.location && (
                                <p className="card-text text-muted mb-1">
                                  📍 {event.location}
                                </p>
                              )}
                              <div className="d-flex gap-2 mt-2">
                                <span 
                                  className="badge" 
                                  style={{ backgroundColor: getEventTypeColor(event.eventType), color: 'white' }}
                                >
                                  {event.eventType?.replace('_', ' ')}
                                </span>
                                {getRegistrationStatusBadge(event.registrationStatus)}
                              </div>
                            </div>
                            <div className="col-md-4 text-md-end">
                              <button
                                className="btn btn-primary me-2"
                                onClick={() => navigate(`/events/${event.id}`)}
                              >
                                <FontAwesomeIcon icon={faEye} className="me-1" />
                                View Details
                              </button>
                              <CalendarIntegration
                                event={{
                                  id: event.id,
                                  name: event.name,
                                  description: event.description,
                                  startDateTime: event.startDateTime,
                                  endDateTime: event.endDateTime,
                                  location: event.location,
                                  organizerEmail: event.organizerEmail
                                }}
                                userId={user.id}
                                options={['google', 'outlook', 'ics']}
                                className="d-inline-block"
                              />
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}
          </>
        )}
      </div>

      {/* Event Details Modal */}
      {showEventModal && selectedEvent && (
        <div className="modal fade show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-lg">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">{selectedEvent.name}</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowEventModal(false)}
                ></button>
              </div>
              <div className="modal-body">
                <div className="mb-3">
                  <strong>Date & Time:</strong><br />
                  {formatEventTime(selectedEvent.startDateTime)}
                  {selectedEvent.endDateTime && (
                    <> - {formatEventTime(selectedEvent.endDateTime)}</>
                  )}
                </div>
                {selectedEvent.location && (
                  <div className="mb-3">
                    <strong>Location:</strong><br />
                    {selectedEvent.location}
                  </div>
                )}
                {selectedEvent.description && (
                  <div className="mb-3">
                    <strong>Description:</strong><br />
                    {selectedEvent.description}
                  </div>
                )}
                <div className="mb-3">
                  <strong>Status:</strong><br />
                  {getRegistrationStatusBadge(selectedEvent.registrationStatus)}
                </div>
                <div className="mb-3">
                  <CalendarIntegration
                    event={{
                      id: selectedEvent.id,
                      name: selectedEvent.name,
                      description: selectedEvent.description,
                      startDateTime: selectedEvent.startDateTime,
                      endDateTime: selectedEvent.endDateTime,
                      location: selectedEvent.location,
                      organizerEmail: selectedEvent.organizerEmail
                    }}
                    userId={user.id}
                    options={['google', 'outlook', 'ics', 'subscribe']}
                  />
                </div>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowEventModal(false)}
                >
                  Close
                </button>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={() => {
                    setShowEventModal(false);
                    navigate(`/events/${selectedEvent.id}`);
                  }}
                >
                  View Full Details
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <style>{`
        .calendar-view-page .rbc-calendar {
          font-family: inherit;
        }
        
        .calendar-view-page .rbc-event {
          border-radius: 4px;
          padding: 2px 5px;
          font-size: 0.875rem;
        }
        
        .calendar-view-page .rbc-event:focus {
          outline: 2px solid #007bff;
          outline-offset: 2px;
        }
        
        .calendar-view-page .rbc-today {
          background-color: rgba(13, 110, 253, 0.1);
        }
        
        .calendar-view-page .rbc-header {
          padding: 0.5rem;
          font-weight: 600;
          border-bottom: 1px solid #dee2e6;
        }
        
        .calendar-view-page .rbc-date-cell {
          padding: 0.25rem;
        }
        
        .calendar-view-page .rbc-month-view .rbc-day-bg {
          border: 1px solid #e9ecef;
        }
        
        .calendar-view-page .modal {
          backdrop-filter: blur(2px);
        }
      `}</style>
    </div>
  );
};

export default CalendarViewPage;