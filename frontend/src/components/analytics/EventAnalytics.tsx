import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
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
  CProgress,
  CWidgetStatsF,
  CBadge,
} from '@coreui/react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement,
} from 'chart.js';
import { Line, Doughnut } from 'react-chartjs-2';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faUsers,
  faCalendarAlt,
  faDollarSign,
  faUserCheck,
  faChartLine,
  faSync,
  faDownload
} from '@fortawesome/free-solid-svg-icons';
import ExportManagerComponent from '../ExportManager';

// Register ChartJS components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement
);

interface Event {
  id: string;
  name: string;
  eventType: string;
  status: string;
  startDateTime: string;
}

interface EventAnalyticsData {
  eventId: string;
  eventName: string;
  totalRegistrations: number;
  totalCheckIns: number;
  attendanceRate: number;
  sessionCount: number;
  avgSessionAttendance: number;
  revenue: number;
  registrationsByDay: Array<{
    date: string;
    registrations: number;
  }>;
  checkInMethods: Array<{
    method: string;
    count: number;
    percentage: number;
  }>;
  sessionAnalytics: Array<{
    sessionId: string;
    sessionTitle: string;
    registrations: number;
    checkedIn: number;
    attendanceRate: number;
    capacity: number;
  }>;
}

const EventAnalytics: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [events, setEvents] = useState<Event[]>([]);
  const [selectedEventId, setSelectedEventId] = useState<string>('');
  const [analyticsData, setAnalyticsData] = useState<EventAnalyticsData | null>(null);
  const [loading, setLoading] = useState(false);
  const [eventsLoading, setEventsLoading] = useState(true);
  const [showExportPanel, setShowExportPanel] = useState(false);

  // Load events list on component mount
  useEffect(() => {
    const loadEvents = async () => {
      try {
        const apiClient = (await import('../../api/apiClient')).default;
        const response = await apiClient.get('/events');
        setEvents(response.data);
        
        // If there's an event ID in URL params, use it
        const eventIdParam = searchParams.get('eventId');
        if (eventIdParam && response.data.some((e: Event) => e.id === eventIdParam)) {
          setSelectedEventId(eventIdParam);
        } else if (response.data.length > 0) {
          // Default to first event
          setSelectedEventId(response.data[0].id);
        }
      } catch (error) {
        console.error('Failed to load events:', error);
      } finally {
        setEventsLoading(false);
      }
    };

    loadEvents();
  }, [searchParams]);

  // Load analytics data when event is selected
  useEffect(() => {
    if (!selectedEventId) return;

    const loadAnalytics = async () => {
      setLoading(true);
      try {
        const apiClient = (await import('../../api/apiClient')).default;
        const response = await apiClient.get(`/analytics/events/${selectedEventId}`);
        setAnalyticsData(response.data);
      } catch (error) {
        console.error('Failed to load event analytics:', error);
      } finally {
        setLoading(false);
      }
    };

    loadAnalytics();
  }, [selectedEventId]);

  const handleEventChange = (eventId: string) => {
    setSelectedEventId(eventId);
    setSearchParams({ eventId });
  };

  const selectedEvent = events.find(e => e.id === selectedEventId);

  const registrationTrendData = analyticsData ? {
    labels: analyticsData.registrationsByDay.map(item => 
      new Date(item.date).toLocaleDateString()
    ),
    datasets: [
      {
        label: 'Daily Registrations',
        data: analyticsData.registrationsByDay.map(item => item.registrations),
        borderColor: 'rgb(54, 162, 235)',
        backgroundColor: 'rgba(54, 162, 235, 0.1)',
        fill: true,
        tension: 0.4,
      },
    ],
  } : { labels: [], datasets: [] };

  const checkInMethodsData = analyticsData ? {
    labels: analyticsData.checkInMethods.map(item => item.method),
    datasets: [
      {
        data: analyticsData.checkInMethods.map(item => item.count),
        backgroundColor: [
          '#FF6384',
          '#36A2EB',
          '#FFCE56',
          '#4BC0C0',
          '#9966FF',
          '#FF9F40'
        ],
        borderWidth: 2,
        borderColor: '#fff'
      },
    ],
  } : { labels: [], datasets: [] };

  const getAttendanceBadge = (rate: number) => {
    if (rate >= 90) return <CBadge color="success">Excellent</CBadge>;
    if (rate >= 80) return <CBadge color="primary">Good</CBadge>;
    if (rate >= 70) return <CBadge color="warning">Fair</CBadge>;
    return <CBadge color="danger">Poor</CBadge>;
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
    }).format(amount);
  };

  if (eventsLoading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ height: '200px' }}>
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading events...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="animated fadeIn">
      {/* Header */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h2 mb-0">Event Analytics</h1>
          <p className="text-medium-emphasis mb-0">
            {selectedEvent ? `Analytics for "${selectedEvent.name}"` : 'Select an event to view analytics'}
          </p>
        </div>
        
        <div className="d-flex gap-2 align-items-center">
          <div className="d-flex align-items-center gap-2">
            <label htmlFor="eventSelect" className="col-form-label">Event:</label>
            <select
              id="eventSelect"
              className="form-select"
              value={selectedEventId}
              onChange={(e) => handleEventChange(e.target.value)}
              style={{ minWidth: '200px' }}
            >
              <option value="">Select an event...</option>
              {events.map(event => (
                <option key={event.id} value={event.id}>
                  {event.name}
                </option>
              ))}
            </select>
          </div>
          
          <CButton
            color="outline-secondary"
            onClick={() => selectedEventId && handleEventChange(selectedEventId)}
            disabled={!selectedEventId || loading}
          >
            <FontAwesomeIcon icon={faSync} className={loading ? 'fa-spin me-1' : 'me-1'} />
            Refresh
          </CButton>
          
          <CButton
            color="primary"
            disabled={!analyticsData}
            onClick={() => setShowExportPanel(!showExportPanel)}
          >
            <FontAwesomeIcon icon={faDownload} className="me-1" />
            Export
          </CButton>
        </div>
      </div>

      {loading && (
        <div className="d-flex justify-content-center align-items-center" style={{ height: '200px' }}>
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading analytics...</span>
          </div>
        </div>
      )}

      {!loading && !analyticsData && selectedEventId && (
        <CCard>
          <CCardBody className="text-center">
            <h5>No analytics data available</h5>
            <p className="text-medium-emphasis">
              Analytics data for this event is not yet available or the event has no registrations.
            </p>
          </CCardBody>
        </CCard>
      )}

      {!loading && !selectedEventId && (
        <CCard>
          <CCardBody className="text-center">
            <h5>Select an Event</h5>
            <p className="text-medium-emphasis">
              Choose an event from the dropdown above to view detailed analytics.
            </p>
          </CCardBody>
        </CCard>
      )}

      {/* Export Panel */}
      {showExportPanel && analyticsData && (
        <div className="mb-4">
          <ExportManagerComponent
            analyticsData={analyticsData}
            eventName={selectedEvent?.name || 'Event Analytics'}
            dateRange="all"
          />
        </div>
      )}

      {analyticsData && !loading && (
        <>
          {/* Key Metrics */}
          <CRow className="mb-4">
            <CCol sm={6} lg={3}>
              <CWidgetStatsF
                className="mb-3"
                icon={<FontAwesomeIcon icon={faUsers} size="xl" />}
                title="Total Registrations"
                value={analyticsData.totalRegistrations.toString()}
                color="primary"
              />
            </CCol>
            <CCol sm={6} lg={3}>
              <CWidgetStatsF
                className="mb-3"
                icon={<FontAwesomeIcon icon={faUserCheck} size="xl" />}
                title="Total Check-ins"
                value={analyticsData.totalCheckIns.toString()}
                color="success"
              />
            </CCol>
            <CCol sm={6} lg={3}>
              <CWidgetStatsF
                className="mb-3"
                icon={<FontAwesomeIcon icon={faChartLine} size="xl" />}
                title="Attendance Rate"
                value={`${analyticsData.attendanceRate.toFixed(1)}%`}
                color="info"
              />
            </CCol>
            <CCol sm={6} lg={3}>
              <CWidgetStatsF
                className="mb-3"
                icon={<FontAwesomeIcon icon={faDollarSign} size="xl" />}
                title="Revenue"
                value={formatCurrency(analyticsData.revenue)}
                color="warning"
              />
            </CCol>
          </CRow>

          {/* Sessions Overview */}
          <CRow className="mb-4">
            <CCol lg={4}>
              <CCard>
                <CCardBody>
                  <div className="d-flex justify-content-between">
                    <div>
                      <h6 className="card-title text-medium-emphasis">Sessions</h6>
                      <div className="fs-4 fw-semibold">{analyticsData.sessionCount}</div>
                    </div>
                    <div className="bg-primary bg-opacity-25 text-primary p-3 rounded">
                      <FontAwesomeIcon icon={faCalendarAlt} size="lg" />
                    </div>
                  </div>
                </CCardBody>
              </CCard>
            </CCol>
            <CCol lg={8}>
              <CCard>
                <CCardBody>
                  <div className="d-flex justify-content-between">
                    <div>
                      <h6 className="card-title text-medium-emphasis">Average Session Attendance</h6>
                      <div className="fs-4 fw-semibold">{analyticsData.avgSessionAttendance.toFixed(1)}%</div>
                    </div>
                    <div className="bg-success bg-opacity-25 text-success p-3 rounded">
                      <FontAwesomeIcon icon={faUserCheck} size="lg" />
                    </div>
                  </div>
                  <CProgress className="mt-3" value={analyticsData.avgSessionAttendance} color="success" />
                  <small className="text-medium-emphasis">Session utilization rate</small>
                </CCardBody>
              </CCard>
            </CCol>
          </CRow>

          {/* Charts Row */}
          <CRow className="mb-4">
            <CCol lg={8}>
              <CCard>
                <CCardHeader>
                  <h5 className="card-title mb-0">Registration Timeline</h5>
                </CCardHeader>
                <CCardBody>
                  <div id="registration-trend-chart">
                    <Line 
                      data={registrationTrendData} 
                      options={{
                        responsive: true,
                        plugins: {
                          legend: {
                            position: 'top' as const,
                          },
                        },
                        scales: {
                          y: {
                            beginAtZero: true,
                          },
                        },
                      }}
                    />
                  </div>
                </CCardBody>
              </CCard>
            </CCol>
            
            <CCol lg={4}>
              <CCard>
                <CCardHeader>
                  <h5 className="card-title mb-0">Check-in Methods</h5>
                </CCardHeader>
                <CCardBody>
                  <div id="checkin-methods-chart">
                    <Doughnut 
                      data={checkInMethodsData}
                      options={{
                        responsive: true,
                        plugins: {
                          legend: {
                            position: 'bottom' as const,
                          },
                        },
                      }}
                    />
                  </div>
                </CCardBody>
              </CCard>
            </CCol>
          </CRow>

          {/* Session Analytics Table */}
          <CRow>
            <CCol>
              <CCard>
                <CCardHeader>
                  <h5 className="card-title mb-0">Session Performance</h5>
                </CCardHeader>
                <CCardBody className="p-0">
                  <div id="session-analytics-chart">
                    <CTable hover responsive>
                    <CTableHead>
                      <CTableRow>
                        <CTableHeaderCell>Session Name</CTableHeaderCell>
                        <CTableHeaderCell>Registrations</CTableHeaderCell>
                        <CTableHeaderCell>Check-ins</CTableHeaderCell>
                        <CTableHeaderCell>Capacity</CTableHeaderCell>
                        <CTableHeaderCell>Attendance Rate</CTableHeaderCell>
                        <CTableHeaderCell>Status</CTableHeaderCell>
                      </CTableRow>
                    </CTableHead>
                    <CTableBody>
                      {analyticsData.sessionAnalytics.map((session, index) => (
                        <CTableRow key={session.sessionId}>
                          <CTableDataCell>
                            <div className="fw-semibold">{session.sessionTitle}</div>
                          </CTableDataCell>
                          <CTableDataCell>
                            <strong>{session.registrations}</strong>
                          </CTableDataCell>
                          <CTableDataCell>
                            {session.checkedIn}
                          </CTableDataCell>
                          <CTableDataCell>
                            {session.capacity || 'Unlimited'}
                            {session.capacity && (
                              <CProgress 
                                className="mt-1" 
                                value={(session.registrations / session.capacity) * 100} 
                                height={4}
                                color="info"
                              />
                            )}
                          </CTableDataCell>
                          <CTableDataCell>
                            <span className={`fw-semibold ${
                              session.attendanceRate >= 90 ? 'text-success' : 
                              session.attendanceRate >= 80 ? 'text-warning' : 'text-danger'
                            }`}>
                              {session.attendanceRate.toFixed(1)}%
                            </span>
                          </CTableDataCell>
                          <CTableDataCell>
                            {getAttendanceBadge(session.attendanceRate)}
                          </CTableDataCell>
                        </CTableRow>
                      ))}
                    </CTableBody>
                    </CTable>
                  </div>
                </CCardBody>
              </CCard>
            </CCol>
          </CRow>
        </>
      )}
    </div>
  );
};

export default EventAnalytics;