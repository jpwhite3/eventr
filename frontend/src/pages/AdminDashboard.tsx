import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
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
  CButtonGroup,
  CWidgetStatsF,
  CBadge,
  CDropdown,
  CDropdownToggle,
  CDropdownMenu,
} from '@coreui/react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faCalendarAlt,
  faUsers,
  faEye,
  faEdit,
  faTrash,
  faPlus,
  faChartBar,
  faUserCheck,
  faDollarSign
} from '@fortawesome/free-solid-svg-icons';
import apiClient from '../api/apiClient';

// Interface for event data
interface Event {
    id: string;
    name: string;
    status: 'DRAFT' | 'PUBLISHED' | 'ACTIVE' | 'CANCELLED';
    eventType: string;
    startDateTime: string;
    endDateTime: string;
    capacity?: number;
    venueName?: string;
    organizerName?: string;
}

interface AdminStats {
    totalEvents: number;
    totalRegistrations: number;
    totalRevenue: number;
    activeEvents: number;
}

const AdminDashboard: React.FC = () => {
    const [events, setEvents] = useState<Event[]>([]);
    const [stats, setStats] = useState<AdminStats | null>(null);
    const [loading, setLoading] = useState(true);

    const fetchEvents = (): void => {
        apiClient.get('/events', { params: { publishedOnly: false } })
            .then(response => {
                setEvents(response.data);
            })
            .catch(error => console.error("Failed to fetch events", error));
    };

    const fetchStats = async (): Promise<void> => {
        try {
            const response = await apiClient.get('/analytics/executive');
            setStats(response.data);
        } catch (error) {
            console.error("Failed to fetch admin stats", error);
        }
    };

    useEffect(() => {
        const loadData = async () => {
            setLoading(true);
            await Promise.all([
                fetchEvents(),
                fetchStats()
            ]);
            setLoading(false);
        };
        loadData();
    }, []);

    const handlePublish = (eventId: string): void => {
        apiClient.post(`/events/${eventId}/publish`)
            .then(() => fetchEvents())
            .catch(error => console.error("Failed to publish event", error));
    };

    // Clone functionality disabled - endpoint not implemented
    // const handleClone = (eventId: string): void => {
    //     apiClient.post(`/events/${eventId}/clone`)
    //         .then(() => fetchEvents())
    //         .catch(error => console.error("Failed to clone event", error));
    // };

    const handleDelete = (eventId: string): void => {
        if (window.confirm('Are you sure you want to delete this event?')) {
            apiClient.delete(`/events/${eventId}`)
                .then(() => fetchEvents())
                .catch(error => console.error("Failed to delete event", error));
        }
    };

    const getStatusBadge = (status: string) => {
        switch (status) {
            case 'DRAFT':
                return <CBadge color="secondary">Draft</CBadge>;
            case 'PUBLISHED':
                return <CBadge color="primary">Published</CBadge>;
            case 'ACTIVE':
                return <CBadge color="success">Active</CBadge>;
            case 'CANCELLED':
                return <CBadge color="danger">Cancelled</CBadge>;
            default:
                return <CBadge color="light">{status}</CBadge>;
        }
    };

    const formatCurrency = (amount: number) => {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD',
            minimumFractionDigits: 0,
        }).format(amount);
    };

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString();
    };

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
                    <h1 className="h2 mb-0">Admin Dashboard</h1>
                    <p className="text-medium-emphasis mb-0">
                        Manage your events and view key metrics
                    </p>
                </div>
                
                <div className="d-flex gap-2">
                    <Link to="/admin/event/new">
                        <CButton color="primary">
                            <FontAwesomeIcon icon={faPlus} className="me-1" />
                            Create Event
                        </CButton>
                    </Link>
                    
                    <CDropdown>
                        <CDropdownToggle color="outline-secondary">
                            <FontAwesomeIcon icon={faChartBar} className="me-1" />
                            Analytics
                        </CDropdownToggle>
                        <CDropdownMenu>
                            <Link to="/analytics/executive" className="dropdown-item">
                                Executive Dashboard
                            </Link>
                            <Link to="/analytics/registrations" className="dropdown-item">
                                Registration Trends
                            </Link>
                            <Link to="/analytics/events" className="dropdown-item">
                                Event Analytics
                            </Link>
                            <Link to="/analytics/attendance" className="dropdown-item">
                                Attendance Analytics
                            </Link>
                        </CDropdownMenu>
                    </CDropdown>
                </div>
            </div>

            {/* Stats Overview */}
            {stats && (
                <CRow className="mb-4">
                    <CCol sm={6} lg={3}>
                        <CWidgetStatsF
                            className="mb-3"
                            icon={<FontAwesomeIcon icon={faCalendarAlt} size="xl" />}
                            title="Total Events"
                            value={stats.totalEvents.toString()}
                            color="primary"
                        />
                    </CCol>
                    <CCol sm={6} lg={3}>
                        <CWidgetStatsF
                            className="mb-3"
                            icon={<FontAwesomeIcon icon={faUsers} size="xl" />}
                            title="Total Registrations"
                            value={stats.totalRegistrations.toLocaleString()}
                            color="success"
                        />
                    </CCol>
                    <CCol sm={6} lg={3}>
                        <CWidgetStatsF
                            className="mb-3"
                            icon={<FontAwesomeIcon icon={faDollarSign} size="xl" />}
                            title="Total Revenue"
                            value={formatCurrency(stats.totalRevenue)}
                            color="info"
                        />
                    </CCol>
                    <CCol sm={6} lg={3}>
                        <CWidgetStatsF
                            className="mb-3"
                            icon={<FontAwesomeIcon icon={faUserCheck} size="xl" />}
                            title="Active Events"
                            value={stats.activeEvents.toString()}
                            color="warning"
                        />
                    </CCol>
                </CRow>
            )}

            {/* Events Table */}
            <CRow>
                <CCol>
                    <CCard>
                        <CCardHeader>
                            <div className="d-flex justify-content-between align-items-center">
                                <h5 className="card-title mb-0">All Events</h5>
                                <CBadge color="light" className="fs-6">
                                    {events.length} total
                                </CBadge>
                            </div>
                        </CCardHeader>
                        <CCardBody className="p-0">
                            <CTable hover responsive>
                                <CTableHead>
                                    <CTableRow>
                                        <CTableHeaderCell>Event Name</CTableHeaderCell>
                                        <CTableHeaderCell>Type</CTableHeaderCell>
                                        <CTableHeaderCell>Status</CTableHeaderCell>
                                        <CTableHeaderCell>Start Date</CTableHeaderCell>
                                        <CTableHeaderCell>Venue</CTableHeaderCell>
                                        <CTableHeaderCell>Organizer</CTableHeaderCell>
                                        <CTableHeaderCell>Actions</CTableHeaderCell>
                                    </CTableRow>
                                </CTableHead>
                                <CTableBody>
                                    {events.map(event => (
                                        <CTableRow key={event.id}>
                                            <CTableDataCell>
                                                <div className="fw-semibold">{event.name}</div>
                                                {event.capacity && (
                                                    <small className="text-medium-emphasis">
                                                        Capacity: {event.capacity}
                                                    </small>
                                                )}
                                            </CTableDataCell>
                                            <CTableDataCell>
                                                <CBadge color="light">{event.eventType}</CBadge>
                                            </CTableDataCell>
                                            <CTableDataCell>
                                                {getStatusBadge(event.status)}
                                            </CTableDataCell>
                                            <CTableDataCell>
                                                {event.startDateTime ? formatDate(event.startDateTime) : 'TBD'}
                                            </CTableDataCell>
                                            <CTableDataCell>
                                                {event.venueName || 'TBD'}
                                            </CTableDataCell>
                                            <CTableDataCell>
                                                {event.organizerName || 'N/A'}
                                            </CTableDataCell>
                                            <CTableDataCell>
                                                <CButtonGroup size="sm">
                                                    <Link to={`/events/${event.id}`}>
                                                        <CButton color="outline-primary" size="sm">
                                                            <FontAwesomeIcon icon={faEye} />
                                                        </CButton>
                                                    </Link>
                                                    
                                                    <Link to={`/admin/event/${event.id}/edit`}>
                                                        <CButton color="outline-secondary" size="sm">
                                                            <FontAwesomeIcon icon={faEdit} />
                                                        </CButton>
                                                    </Link>
                                                    
                                                    <CButton 
                                                        color="outline-info" 
                                                        size="sm" 
                                                        onClick={() => handlePublish(event.id)} 
                                                        disabled={event.status === 'PUBLISHED'}
                                                    >
                                                        Publish
                                                    </CButton>
                                                    
                                                    {/* Clone button disabled - endpoint not implemented */}
                                                    {/* <CButton 
                                                        color="outline-success" 
                                                        size="sm" 
                                                        onClick={() => handleClone(event.id)}
                                                    >
                                                        <FontAwesomeIcon icon={faClone} />
                                                    </CButton> */}
                                                    
                                                    <Link to={`/admin/events/${event.id}/attendance`}>
                                                        <CButton color="outline-warning" size="sm">
                                                            <FontAwesomeIcon icon={faUserCheck} />
                                                        </CButton>
                                                    </Link>
                                                    
                                                    <CButton 
                                                        color="outline-danger" 
                                                        size="sm" 
                                                        onClick={() => handleDelete(event.id)}
                                                    >
                                                        <FontAwesomeIcon icon={faTrash} />
                                                    </CButton>
                                                </CButtonGroup>
                                            </CTableDataCell>
                                        </CTableRow>
                                    ))}
                                    {events.length === 0 && (
                                        <CTableRow>
                                            <CTableDataCell colSpan={7} className="text-center py-4">
                                                <div className="text-medium-emphasis">
                                                    <FontAwesomeIcon icon={faCalendarAlt} size="2x" className="mb-2" />
                                                    <p>No events found</p>
                                                    <Link to="/admin/event/new">
                                                        <CButton color="primary">
                                                            Create your first event
                                                        </CButton>
                                                    </Link>
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
        </div>
    );
};

export default AdminDashboard;