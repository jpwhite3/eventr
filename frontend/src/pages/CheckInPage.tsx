import React, { useState, useEffect } from 'react';
import {
  CRow,
  CCol,
  CCard,
  CCardBody,
  CCardHeader,
  CTable,
  CTableHead,
  CTableRow,
  CTableHeaderCell,
  CTableBody,
  CTableDataCell,
  CButton,
  CWidgetStatsF,
  CBadge,
} from '@coreui/react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faUserCheck,
  faQrcode,
  faSearch,
  faUsers,
  faCalendarAlt,
  faClipboardCheck,
  faSync,
  faUserPlus,
  faCheckCircle,
  faTimesCircle,
  faMobile
} from '@fortawesome/free-solid-svg-icons';

interface Event {
  id: string;
  name: string;
  startDateTime: string;
  venueName?: string;
}

interface Session {
  id: string;
  title: string;
  startTime?: string;
  endTime?: string;
  location?: string;
}

interface Registration {
  id: string;
  userName: string;
  userEmail: string;
  eventName: string;
  sessionName?: string;
  status: string;
  checkedIn: boolean;
  checkedInAt?: string;
}

interface CheckInStats {
  totalRegistrations: number;
  checkedInCount: number;
  pendingCount: number;
  checkInRate: number;
}

const CheckInPage: React.FC = () => {
  const [events, setEvents] = useState<Event[]>([]);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [registrations, setRegistrations] = useState<Registration[]>([]);
  const [stats, setStats] = useState<CheckInStats | null>(null);
  const [selectedEvent, setSelectedEvent] = useState<string>('');
  const [selectedSession, setSelectedSession] = useState<string>('');
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [checkingIn, setCheckingIn] = useState<string | null>(null);
  const [showSuccessAlert, setShowSuccessAlert] = useState(false);
  const [lastCheckedInUser, setLastCheckedInUser] = useState<string>('');

  useEffect(() => {
    loadEvents();
  }, []);

  useEffect(() => {
    if (selectedEvent) {
      loadSessionsForEvent(selectedEvent);
    }
  }, [selectedEvent]);

  useEffect(() => {
    loadRegistrations();
  }, [selectedEvent, selectedSession]);

  const loadEvents = async () => {
    try {
      const apiClient = (await import('../api/apiClient')).default;
      const response = await apiClient.get('/events');
      setEvents(response.data);
      if (response.data.length > 0) {
        setSelectedEvent(response.data[0].id);
      }
    } catch (error) {
      console.error('Failed to load events:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadSessionsForEvent = async (eventId: string) => {
    try {
      const apiClient = (await import('../api/apiClient')).default;
      const response = await apiClient.get(`/sessions/event/${eventId}`);
      setSessions(response.data || []);
    } catch (error) {
      console.error('Failed to load sessions:', error);
      setSessions([]);
    }
  };

  const loadRegistrations = async () => {
    try {
      // Mock data for demonstration since endpoint may not exist
      const mockRegistrations: Registration[] = [
        {
          id: '1',
          userName: 'John Doe',
          userEmail: 'john@example.com',
          eventName: 'Tech Conference 2024',
          status: 'CONFIRMED',
          checkedIn: false
        },
        {
          id: '2',
          userName: 'Jane Smith',
          userEmail: 'jane@example.com',
          eventName: 'Tech Conference 2024',
          status: 'CONFIRMED',
          checkedIn: true,
          checkedInAt: new Date().toISOString()
        },
        {
          id: '3',
          userName: 'Bob Johnson',
          userEmail: 'bob@example.com',
          eventName: 'Tech Conference 2024',
          status: 'CONFIRMED',
          checkedIn: false
        }
      ];
      
      setRegistrations(mockRegistrations);
      
      // Calculate stats
      const totalRegistrations = mockRegistrations.length;
      const checkedInCount = mockRegistrations.filter(r => r.checkedIn).length;
      const pendingCount = totalRegistrations - checkedInCount;
      const checkInRate = totalRegistrations > 0 ? (checkedInCount / totalRegistrations * 100) : 0;
      
      setStats({
        totalRegistrations,
        checkedInCount,
        pendingCount,
        checkInRate
      });
      
    } catch (error) {
      console.error('Failed to load registrations:', error);
      setRegistrations([]);
    }
  };

  const handleCheckIn = async (registrationId: string, userName: string) => {
    try {
      setCheckingIn(registrationId);
      
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Update local state
      setRegistrations(prev => prev.map(reg => 
        reg.id === registrationId 
          ? { ...reg, checkedIn: true, checkedInAt: new Date().toISOString() }
          : reg
      ));
      
      setLastCheckedInUser(userName);
      setShowSuccessAlert(true);
      
      // Auto-hide alert after 3 seconds
      setTimeout(() => setShowSuccessAlert(false), 3000);
      
      // Reload stats
      loadRegistrations();
      
    } catch (error) {
      console.error('Failed to check in user:', error);
    } finally {
      setCheckingIn(null);
    }
  };

  const filteredRegistrations = registrations.filter(registration =>
    registration.userName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    registration.userEmail.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const selectedEventData = events.find(e => e.id === selectedEvent);

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ height: '200px' }}>
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="animated fadeIn">
      {/* Header */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h2 mb-0">Event Check-In</h1>
          <p className="text-medium-emphasis mb-0">
            {selectedEventData ? `Check-in for "${selectedEventData.name}"` : 'Select an event to start check-in'}
          </p>
        </div>
        
        <div className="d-flex gap-2">
          <CButton color="outline-secondary" onClick={loadRegistrations}>
            <FontAwesomeIcon icon={faSync} className="me-1" />
            Refresh
          </CButton>
          <CButton color="outline-primary">
            <FontAwesomeIcon icon={faMobile} className="me-1" />
            Mobile Check-In
          </CButton>
          <CButton color="primary">
            <FontAwesomeIcon icon={faQrcode} className="me-1" />
            QR Scanner
          </CButton>
        </div>
      </div>

      {/* Event & Session Selection */}
      <CRow className="mb-4">
        <CCol>
          <CCard>
            <CCardBody>
              <div>
                <CRow className="align-items-end">
                  <CCol md={4}>
                    <label htmlFor="eventSelect" className="form-label">Event</label>
                    <select
                      id="eventSelect"
                      className="form-select"
                      value={selectedEvent}
                      onChange={(e) => setSelectedEvent(e.target.value)}
                    >
                      <option value="">Select an event...</option>
                      {events.map(event => (
                        <option key={event.id} value={event.id}>
                          {event.name}
                        </option>
                      ))}
                    </select>
                  </CCol>
                  <CCol md={4}>
                    <label htmlFor="sessionSelect" className="form-label">Session (Optional)</label>
                    <select
                      id="sessionSelect"
                      className="form-select"
                      value={selectedSession}
                      onChange={(e) => setSelectedSession(e.target.value)}
                      disabled={!sessions.length}
                    >
                      <option value="">All sessions</option>
                      {sessions.map(session => (
                        <option key={session.id} value={session.id}>
                          {session.title}
                        </option>
                      ))}
                    </select>
                  </CCol>
                  <CCol md={4}>
                    <div className="input-group">
                      <span className="input-group-text">
                        <FontAwesomeIcon icon={faSearch} />
                      </span>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="Search attendees..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                      />
                    </div>
                  </CCol>
                </CRow>
              </div>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* Stats Overview */}
      {stats && (
        <CRow className="mb-4">
          <CCol sm={6} lg={3}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faUsers} size="xl" />}
              title="Total Registered"
              value={stats.totalRegistrations.toString()}
              color="primary"
            />
          </CCol>
          <CCol sm={6} lg={3}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faUserCheck} size="xl" />}
              title="Checked In"
              value={stats.checkedInCount.toString()}
              color="success"
            />
          </CCol>
          <CCol sm={6} lg={3}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faCalendarAlt} size="xl" />}
              title="Pending"
              value={stats.pendingCount.toString()}
              color="warning"
            />
          </CCol>
          <CCol sm={6} lg={3}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faClipboardCheck} size="xl" />}
              title="Check-in Rate"
              value={`${stats.checkInRate.toFixed(1)}%`}
              color="info"
            />
          </CCol>
        </CRow>
      )}


      {/* Registrations Table */}
      <CRow>
        <CCol>
          <CCard>
            <CCardHeader>
              <div className="d-flex justify-content-between align-items-center">
                <h5 className="card-title mb-0">Attendee List</h5>
                <CBadge color="light" className="fs-6">
                  {filteredRegistrations.length} attendees
                </CBadge>
              </div>
            </CCardHeader>
            <CCardBody className="p-0">
              <CTable hover responsive>
                <CTableHead>
                  <CTableRow>
                    <CTableHeaderCell>Attendee</CTableHeaderCell>
                    <CTableHeaderCell>Email</CTableHeaderCell>
                    <CTableHeaderCell>Status</CTableHeaderCell>
                    <CTableHeaderCell>Check-in Status</CTableHeaderCell>
                    <CTableHeaderCell>Check-in Time</CTableHeaderCell>
                    <CTableHeaderCell>Actions</CTableHeaderCell>
                  </CTableRow>
                </CTableHead>
                <CTableBody>
                  {filteredRegistrations.map(registration => (
                    <CTableRow key={registration.id}>
                      <CTableDataCell>
                        <div className="fw-semibold">{registration.userName}</div>
                      </CTableDataCell>
                      <CTableDataCell>
                        {registration.userEmail}
                      </CTableDataCell>
                      <CTableDataCell>
                        <CBadge color="success">{registration.status}</CBadge>
                      </CTableDataCell>
                      <CTableDataCell>
                        {registration.checkedIn ? (
                          <CBadge color="success">
                            <FontAwesomeIcon icon={faCheckCircle} className="me-1" />
                            Checked In
                          </CBadge>
                        ) : (
                          <CBadge color="warning">
                            <FontAwesomeIcon icon={faTimesCircle} className="me-1" />
                            Pending
                          </CBadge>
                        )}
                      </CTableDataCell>
                      <CTableDataCell>
                        {registration.checkedInAt ? 
                          new Date(registration.checkedInAt).toLocaleTimeString() : 
                          '-'
                        }
                      </CTableDataCell>
                      <CTableDataCell>
                        <CButton
                          color={registration.checkedIn ? "outline-secondary" : "success"}
                          size="sm"
                          disabled={registration.checkedIn || checkingIn === registration.id}
                          onClick={() => handleCheckIn(registration.id, registration.userName)}
                        >
                          {checkingIn === registration.id ? (
                            <div className="spinner-border spinner-border-sm me-1" role="status" />
                          ) : (
                            <FontAwesomeIcon icon={faUserPlus} className="me-1" />
                          )}
                          {registration.checkedIn ? 'Checked In' : 'Check In'}
                        </CButton>
                      </CTableDataCell>
                    </CTableRow>
                  ))}
                  {filteredRegistrations.length === 0 && (
                    <CTableRow>
                      <CTableDataCell colSpan={6} className="text-center py-4">
                        <div className="text-medium-emphasis">
                          <FontAwesomeIcon icon={faUsers} size="2x" className="mb-2" />
                          <p>No registrations found</p>
                          {searchTerm && (
                            <p className="small">Try adjusting your search criteria</p>
                          )}
                        </div>
                      </CTableDataCell>
                    </CTableRow>
                  )}
                </CTableBody>
              </CTable>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* Success Alert */}
      {showSuccessAlert && (
        <div className="position-fixed top-0 end-0 p-3" style={{ zIndex: 1050 }}>
          <div className="alert alert-success d-flex align-items-center">
            <FontAwesomeIcon icon={faCheckCircle} className="me-2" />
            <strong>{lastCheckedInUser}</strong> has been successfully checked in!
          </div>
        </div>
      )}
    </div>
  );
};

export default CheckInPage;