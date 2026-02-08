import React, { useState, useEffect, useCallback } from 'react';
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
  CDropdownItem,
  CProgress,
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
  faDollarSign,
  faClone,
  faSearch,
  faFilter,
  faSort,
  faDownload,
  faSync,
  faCheckSquare,
  faEllipsisV,
  faCalendarCheck,
  faShare,
  faCogs,
  faFileExport,
  faUsers as faUsersIcon,
  faShield,
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
    pendingEvents: number;
    publishedEvents: number;
    averageCapacityUtilization: number;
    totalUsers: number;
}

interface BulkAction {
    action: 'publish' | 'unpublish' | 'clone' | 'delete' | 'export';
    eventIds: string[];
}

interface FilterOptions {
    search: string;
    status: string;
    eventType: string;
    category: string;
    dateRange: string;
    sortBy: string;
    sortOrder: 'asc' | 'desc';
}

const AdminDashboard: React.FC = () => {
    const [events, setEvents] = useState<Event[]>([]);
    const [filteredEvents, setFilteredEvents] = useState<Event[]>([]);
    const [stats, setStats] = useState<AdminStats | null>(null);
    const [loading, setLoading] = useState(true);
    const [cloning, setCloning] = useState<string | null>(null);
    
    // Enhanced Admin Features
    const [selectedEvents, setSelectedEvents] = useState<Set<string>>(new Set());
    const [showBulkActions, setShowBulkActions] = useState(false);
    const [bulkProcessing, setBulkProcessing] = useState(false);
    const [filters, setFilters] = useState<FilterOptions>({
        search: '',
        status: 'ALL',
        eventType: 'ALL',
        category: 'ALL',
        dateRange: 'ALL',
        sortBy: 'startDateTime',
        sortOrder: 'desc',
    });
    const [showFilters, setShowFilters] = useState(false);
    const [toast, setToast] = useState<{ message: string; color: string; visible: boolean }>({
        message: '',
        color: 'success',
        visible: false,
    });

    const fetchEvents = useCallback((): void => {
        apiClient.get('/events', { params: { publishedOnly: false } })
            .then(response => {
                setEvents(response.data);
            })
            .catch(error => console.error("Failed to fetch events", error));
    }, []);

    const fetchStats = async (): Promise<void> => {
        try {
            const response = await apiClient.get('/analytics/executive');
            setStats(response.data);
        } catch (error) {
            console.error("Failed to fetch admin stats", error);
        }
    };

    // Bulk action handlers
    const handleBulkAction = async (action: BulkAction['action']) => {
        if (selectedEvents.size === 0) {
            showToast('Please select events first', 'warning');
            return;
        }

        if (action === 'delete' && !window.confirm(`Are you sure you want to delete ${selectedEvents.size} events?`)) {
            return;
        }

        setBulkProcessing(true);
        const eventIds = Array.from(selectedEvents);

        try {
            switch (action) {
                case 'publish':
                    await Promise.all(eventIds.map(id => apiClient.post(`/events/${id}/publish`)));
                    showToast(`Successfully published ${eventIds.length} events`, 'success');
                    break;
                case 'unpublish':
                    // TODO: Add unpublish endpoint
                    showToast(`Unpublish feature coming soon`, 'info');
                    break;
                case 'clone':
                    await Promise.all(eventIds.map(id => apiClient.post(`/events/${id}/clone`)));
                    showToast(`Successfully cloned ${eventIds.length} events`, 'success');
                    break;
                case 'delete':
                    await Promise.all(eventIds.map(id => apiClient.delete(`/events/${id}`)));
                    showToast(`Successfully deleted ${eventIds.length} events`, 'success');
                    break;
                case 'export':
                    // TODO: Implement export functionality
                    showToast('Export feature coming soon', 'info');
                    break;
            }
            
            fetchEvents();
            setSelectedEvents(new Set());
            setShowBulkActions(false);
        } catch (error) {
            showToast(`Failed to ${action} events`, 'danger');
            console.error(`Bulk ${action} failed:`, error);
        } finally {
            setBulkProcessing(false);
        }
    };

    const showToast = (message: string, color: string) => {
        setToast({ message, color, visible: true });
        setTimeout(() => setToast(prev => ({ ...prev, visible: false })), 5000);
    };

    const handleSelectEvent = (eventId: string) => {
        const newSelection = new Set(selectedEvents);
        if (newSelection.has(eventId)) {
            newSelection.delete(eventId);
        } else {
            newSelection.add(eventId);
        }
        setSelectedEvents(newSelection);
        setShowBulkActions(newSelection.size > 0);
    };

    const handleSelectAll = () => {
        if (selectedEvents.size === filteredEvents.length && filteredEvents.length > 0) {
            setSelectedEvents(new Set());
            setShowBulkActions(false);
        } else {
            setSelectedEvents(new Set(filteredEvents.map(e => e.id)));
            setShowBulkActions(true);
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
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // Update derived stats when events change
    useEffect(() => {
        setStats(prev => {
            if (!prev || events.length === 0) return prev;
            return {
                ...prev,
                pendingEvents: events.filter(e => e.status === 'DRAFT').length,
                publishedEvents: events.filter(e => e.status === 'PUBLISHED').length,
                averageCapacityUtilization: 75, // TODO: Calculate from actual data  
                totalUsers: prev.totalUsers || 0,
            };
        });
    }, [events]);

    // Apply filters whenever events or filter options change
    useEffect(() => {
        let filtered = [...events];

        // Search filter
        if (filters.search) {
            const searchLower = filters.search.toLowerCase();
            filtered = filtered.filter(event =>
                event.name.toLowerCase().includes(searchLower) ||
                event.eventType.toLowerCase().includes(searchLower) ||
                event.venueName?.toLowerCase().includes(searchLower) ||
                event.organizerName?.toLowerCase().includes(searchLower)
            );
        }

        // Status filter
        if (filters.status !== 'ALL') {
            filtered = filtered.filter(event => event.status === filters.status);
        }

        // Event type filter
        if (filters.eventType !== 'ALL') {
            filtered = filtered.filter(event => event.eventType === filters.eventType);
        }

        // Date range filter
        if (filters.dateRange !== 'ALL') {
            const now = new Date();
            const filterDate = new Date();
            
            switch (filters.dateRange) {
                case 'TODAY':
                    filterDate.setDate(now.getDate() + 1);
                    filtered = filtered.filter(event => 
                        new Date(event.startDateTime) >= now && 
                        new Date(event.startDateTime) < filterDate
                    );
                    break;
                case 'WEEK':
                    filterDate.setDate(now.getDate() + 7);
                    filtered = filtered.filter(event => 
                        new Date(event.startDateTime) >= now && 
                        new Date(event.startDateTime) < filterDate
                    );
                    break;
                case 'MONTH':
                    filterDate.setMonth(now.getMonth() + 1);
                    filtered = filtered.filter(event => 
                        new Date(event.startDateTime) >= now && 
                        new Date(event.startDateTime) < filterDate
                    );
                    break;
            }
        }

        // Sort events
        filtered.sort((a, b) => {
            let aValue, bValue;
            
            switch (filters.sortBy) {
                case 'name':
                    aValue = a.name.toLowerCase();
                    bValue = b.name.toLowerCase();
                    break;
                case 'status':
                    aValue = a.status;
                    bValue = b.status;
                    break;
                case 'eventType':
                    aValue = a.eventType;
                    bValue = b.eventType;
                    break;
                case 'capacity':
                    aValue = a.capacity || 0;
                    bValue = b.capacity || 0;
                    break;
                default:
                    aValue = new Date(a.startDateTime || 0);
                    bValue = new Date(b.startDateTime || 0);
            }

            if (aValue < bValue) return filters.sortOrder === 'asc' ? -1 : 1;
            if (aValue > bValue) return filters.sortOrder === 'asc' ? 1 : -1;
            return 0;
        });

        setFilteredEvents(filtered);
    }, [events, filters]);

    const handlePublish = (eventId: string): void => {
        apiClient.post(`/events/${eventId}/publish`)
            .then(() => fetchEvents())
            .catch(error => console.error("Failed to publish event", error));
    };

    const handleClone = async (eventId: string): Promise<void> => {
        try {
            setCloning(eventId);
            await apiClient.post(`/events/${eventId}/clone`);
            
            // Show success notification
            alert('Event cloned successfully! The cloned event has been created with DRAFT status.');
            
            // Refresh the events list to show the new clone
            await fetchEvents();
        } catch (error) {
            console.error('Failed to clone event:', error);
            alert('Failed to clone event. Please try again.');
        } finally {
            setCloning(null);
        }
    };

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
            {/* Toast Notifications */}
            {toast.visible && (
                <div className="position-fixed top-0 end-0 p-3" style={{ zIndex: 1050 }}>
                    <div className={`toast show bg-${toast.color}`} role="alert" aria-live="assertive" aria-atomic="true">
                        <div className="toast-header">
                            <strong className="me-auto">Notification</strong>
                            <button 
                                type="button" 
                                className="btn-close" 
                                onClick={() => setToast(prev => ({ ...prev, visible: false }))}
                                aria-label="Close"
                            ></button>
                        </div>
                        <div className="toast-body text-white">
                            {toast.message}
                        </div>
                    </div>
                </div>
            )}

            {/* Header */}
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h1 className="h2 mb-0">Administrative Dashboard</h1>
                    <p className="text-medium-emphasis mb-0">
                        Comprehensive event and user management
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

                    <CDropdown>
                        <CDropdownToggle color="outline-info">
                            <FontAwesomeIcon icon={faCogs} className="me-1" />
                            Admin Tools
                        </CDropdownToggle>
                        <CDropdownMenu>
                            <Link to="/admin/users" className="dropdown-item">
                                <FontAwesomeIcon icon={faUsersIcon} className="me-2" />
                                User Management
                            </Link>
                            <CDropdownItem>
                                <FontAwesomeIcon icon={faShield} className="me-2" />
                                Role Administration
                            </CDropdownItem>
                            <Link to="/admin/reports" className="dropdown-item">
                                <FontAwesomeIcon icon={faFileExport} className="me-2" />
                                System Reports
                            </Link>
                            <hr className="dropdown-divider" />
                            <CDropdownItem>
                                <FontAwesomeIcon icon={faCogs} className="me-2" />
                                System Settings
                            </CDropdownItem>
                        </CDropdownMenu>
                    </CDropdown>

                    <CButton 
                        color="outline-secondary" 
                        onClick={() => { fetchEvents(); fetchStats(); }}
                    >
                        <FontAwesomeIcon icon={faSync} />
                    </CButton>
                </div>
            </div>


            {/* Enhanced Stats Overview */}
            {stats && (
                <>
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

                    {/* Additional Admin Metrics */}
                    <CRow className="mb-4">
                        <CCol sm={6} lg={3}>
                            <CCard>
                                <CCardBody className="text-center">
                                    <div className="fs-4 fw-semibold text-primary">{stats.publishedEvents}</div>
                                    <div className="text-uppercase text-medium-emphasis small">Published Events</div>
                                </CCardBody>
                            </CCard>
                        </CCol>
                        <CCol sm={6} lg={3}>
                            <CCard>
                                <CCardBody className="text-center">
                                    <div className="fs-4 fw-semibold text-secondary">{stats.pendingEvents}</div>
                                    <div className="text-uppercase text-medium-emphasis small">Draft Events</div>
                                </CCardBody>
                            </CCard>
                        </CCol>
                        <CCol sm={6} lg={3}>
                            <CCard>
                                <CCardBody className="text-center">
                                    <div className="fs-4 fw-semibold text-info">{stats.averageCapacityUtilization}%</div>
                                    <div className="text-uppercase text-medium-emphasis small">Avg Capacity</div>
                                    <CProgress value={stats.averageCapacityUtilization} color="info" height={4} className="mt-1" />
                                </CCardBody>
                            </CCard>
                        </CCol>
                        <CCol sm={6} lg={3}>
                            <CCard>
                                <CCardBody className="text-center">
                                    <div className="fs-4 fw-semibold text-success">{stats.totalUsers}</div>
                                    <div className="text-uppercase text-medium-emphasis small">Total Users</div>
                                </CCardBody>
                            </CCard>
                        </CCol>
                    </CRow>
                </>
            )}

            {/* Advanced Filters and Search */}
            <CRow className="mb-4">
                <CCol>
                    <CCard>
                        <CCardHeader>
                            <div className="d-flex justify-content-between align-items-center">
                                <h6 className="mb-0">Advanced Event Management</h6>
                                <CButton
                                    color="outline-secondary"
                                    size="sm"
                                    onClick={() => setShowFilters(!showFilters)}
                                >
                                    <FontAwesomeIcon icon={faFilter} className="me-1" />
                                    {showFilters ? 'Hide Filters' : 'Show Filters'}
                                </CButton>
                            </div>
                        </CCardHeader>
                        
                        <CCardBody>
                            {/* Quick Search */}
                            <CRow className="mb-3">
                                <CCol md={6}>
                                    <div className="input-group">
                                        <span className="input-group-text">
                                            <FontAwesomeIcon icon={faSearch} />
                                        </span>
                                        <input 
                                            className="form-control"
                                            placeholder="Search events, venues, organizers..."
                                            value={filters.search}
                                            onChange={(e) => setFilters(prev => ({ ...prev, search: e.target.value }))}
                                        />
                                    </div>
                                </CCol>
                                <CCol md={6}>
                                    <div className="d-flex gap-2">
                                        <CButton
                                            color="outline-primary"
                                            onClick={() => setFilters({
                                                search: '',
                                                status: 'ALL',
                                                eventType: 'ALL',
                                                category: 'ALL',
                                                dateRange: 'ALL',
                                                sortBy: 'startDateTime',
                                                sortOrder: 'desc',
                                            })}
                                        >
                                            Clear Filters
                                        </CButton>
                                        
                                        <CDropdown>
                                            <CDropdownToggle color="outline-success">
                                                <FontAwesomeIcon icon={faDownload} className="me-1" />
                                                Export
                                            </CDropdownToggle>
                                            <CDropdownMenu>
                                                <CDropdownItem onClick={() => handleBulkAction('export')}>
                                                    Export Filtered Events
                                                </CDropdownItem>
                                            </CDropdownMenu>
                                        </CDropdown>
                                    </div>
                                </CCol>
                            </CRow>

                            {/* Advanced Filters */}
                            {showFilters && (
                                <CRow className="mb-3">
                                    <CCol md={3}>
                                        <select
                                            className="form-select"
                                            value={filters.status}
                                            onChange={(e) => setFilters(prev => ({ ...prev, status: e.target.value }))}
                                        >
                                            <option value="ALL">All Statuses</option>
                                            <option value="DRAFT">Draft</option>
                                            <option value="PUBLISHED">Published</option>
                                            <option value="ACTIVE">Active</option>
                                            <option value="CANCELLED">Cancelled</option>
                                        </select>
                                    </CCol>
                                    <CCol md={3}>
                                        <select
                                            className="form-select"
                                            value={filters.eventType}
                                            onChange={(e) => setFilters(prev => ({ ...prev, eventType: e.target.value }))}
                                        >
                                            <option value="ALL">All Types</option>
                                            <option value="CONFERENCE">Conference</option>
                                            <option value="WORKSHOP">Workshop</option>
                                            <option value="SEMINAR">Seminar</option>
                                            <option value="WEBINAR">Webinar</option>
                                        </select>
                                    </CCol>
                                    <CCol md={3}>
                                        <select
                                            className="form-select"
                                            value={filters.dateRange}
                                            onChange={(e) => setFilters(prev => ({ ...prev, dateRange: e.target.value }))}
                                        >
                                            <option value="ALL">All Dates</option>
                                            <option value="TODAY">Today</option>
                                            <option value="WEEK">This Week</option>
                                            <option value="MONTH">This Month</option>
                                        </select>
                                    </CCol>
                                    <CCol md={3}>
                                        <div className="d-flex">
                                            <select
                                                className="form-select me-1"
                                                value={filters.sortBy}
                                                onChange={(e) => setFilters(prev => ({ ...prev, sortBy: e.target.value }))}
                                            >
                                                <option value="startDateTime">Date</option>
                                                <option value="name">Name</option>
                                                <option value="status">Status</option>
                                                <option value="eventType">Type</option>
                                                <option value="capacity">Capacity</option>
                                            </select>
                                            <CButton
                                                color="outline-secondary"
                                                onClick={() => setFilters(prev => ({ 
                                                    ...prev, 
                                                    sortOrder: prev.sortOrder === 'asc' ? 'desc' : 'asc' 
                                                }))}
                                            >
                                                <FontAwesomeIcon 
                                                    icon={faSort} 
                                                    rotation={filters.sortOrder === 'desc' ? 180 : undefined}
                                                />
                                            </CButton>
                                        </div>
                                    </CCol>
                                </CRow>
                            )}

                            {/* Bulk Actions Bar */}
                            {showBulkActions && (
                                <div className="alert alert-info d-flex justify-content-between align-items-center" role="alert">
                                    <div>
                                        <FontAwesomeIcon icon={faCheckSquare} className="me-2" />
                                        {selectedEvents.size} event{selectedEvents.size !== 1 ? 's' : ''} selected
                                    </div>
                                    <CButtonGroup>
                                        <CButton
                                            color="primary"
                                            size="sm"
                                            onClick={() => handleBulkAction('publish')}
                                            disabled={bulkProcessing}
                                        >
                                            <FontAwesomeIcon icon={faCalendarCheck} className="me-1" />
                                            Publish
                                        </CButton>
                                        <CButton
                                            color="info"
                                            size="sm"
                                            onClick={() => handleBulkAction('clone')}
                                            disabled={bulkProcessing}
                                        >
                                            <FontAwesomeIcon icon={faClone} className="me-1" />
                                            Clone
                                        </CButton>
                                        <CButton
                                            color="success"
                                            size="sm"
                                            onClick={() => handleBulkAction('export')}
                                            disabled={bulkProcessing}
                                        >
                                            <FontAwesomeIcon icon={faDownload} className="me-1" />
                                            Export
                                        </CButton>
                                        <CButton
                                            color="danger"
                                            size="sm"
                                            onClick={() => handleBulkAction('delete')}
                                            disabled={bulkProcessing}
                                        >
                                            <FontAwesomeIcon icon={faTrash} className="me-1" />
                                            Delete
                                        </CButton>
                                    </CButtonGroup>
                                </div>
                            )}
                        </CCardBody>
                    </CCard>
                </CCol>
            </CRow>

            {/* Events Table */}
            <CRow>
                <CCol>
                    <CCard>
                        <CCardHeader>
                            <div className="d-flex justify-content-between align-items-center">
                                <h5 className="card-title mb-0">Event Management</h5>
                                <div className="d-flex align-items-center gap-3">
                                    <CBadge color="light" className="fs-6">
                                        {filteredEvents.length} of {events.length} events
                                    </CBadge>
                                    {filteredEvents.length !== events.length && (
                                        <CBadge color="info">Filtered</CBadge>
                                    )}
                                </div>
                            </div>
                        </CCardHeader>
                        <CCardBody className="p-0">
                            <CTable hover responsive>
                                <CTableHead>
                                    <CTableRow>
                                        <CTableHeaderCell width="50">
                                            <input 
                                                type="checkbox" 
                                                className="form-check-input"
                                                checked={selectedEvents.size === filteredEvents.length && filteredEvents.length > 0}
                                                onChange={handleSelectAll}
                                            />
                                        </CTableHeaderCell>
                                        <CTableHeaderCell>Event Details</CTableHeaderCell>
                                        <CTableHeaderCell>Type & Status</CTableHeaderCell>
                                        <CTableHeaderCell>Schedule</CTableHeaderCell>
                                        <CTableHeaderCell>Capacity</CTableHeaderCell>
                                        <CTableHeaderCell>Performance</CTableHeaderCell>
                                        <CTableHeaderCell>Actions</CTableHeaderCell>
                                    </CTableRow>
                                </CTableHead>
                                <CTableBody>
                                    {filteredEvents.map(event => (
                                        <CTableRow 
                                            key={event.id}
                                            className={selectedEvents.has(event.id) ? 'table-active' : ''}
                                        >
                                            <CTableDataCell>
                                                <input 
                                                    type="checkbox" 
                                                    className="form-check-input"
                                                    checked={selectedEvents.has(event.id)}
                                                    onChange={() => handleSelectEvent(event.id)}
                                                />
                                            </CTableDataCell>
                                            <CTableDataCell>
                                                <div className="fw-semibold">{event.name}</div>
                                                <div className="small text-medium-emphasis">
                                                    <FontAwesomeIcon icon={faCalendarAlt} className="me-1" />
                                                    {event.venueName || 'Virtual Event'}
                                                    {event.organizerName && (
                                                        <>
                                                            <span className="mx-2">•</span>
                                                            {event.organizerName}
                                                        </>
                                                    )}
                                                </div>
                                            </CTableDataCell>
                                            <CTableDataCell>
                                                <div className="mb-1">
                                                    <CBadge color="light">{event.eventType}</CBadge>
                                                </div>
                                                {getStatusBadge(event.status)}
                                            </CTableDataCell>
                                            <CTableDataCell>
                                                <div className="fw-semibold">
                                                    {event.startDateTime ? formatDate(event.startDateTime) : 'TBD'}
                                                </div>
                                                {event.startDateTime && event.endDateTime && (
                                                    <small className="text-medium-emphasis">
                                                        {new Date(event.startDateTime).toLocaleTimeString()} - {' '}
                                                        {new Date(event.endDateTime).toLocaleTimeString()}
                                                    </small>
                                                )}
                                            </CTableDataCell>
                                            <CTableDataCell>
                                                {event.capacity ? (
                                                    <div>
                                                        <div className="fw-semibold">{event.capacity} seats</div>
                                                        <CProgress 
                                                            value={75} 
                                                            color="success" 
                                                            height={4}
                                                            className="mt-1"
                                                        />
                                                        <small className="text-success">75% filled</small>
                                                    </div>
                                                ) : (
                                                    <span className="text-muted">Unlimited</span>
                                                )}
                                            </CTableDataCell>
                                            <CTableDataCell>
                                                <div className="d-flex align-items-center gap-2">
                                                    <div className="text-success">
                                                        <FontAwesomeIcon icon={faUsers} className="me-1" />
                                                        <span className="fw-semibold">45</span>
                                                    </div>
                                                    <div className="text-primary">
                                                        <FontAwesomeIcon icon={faUserCheck} className="me-1" />
                                                        <span className="fw-semibold">32</span>
                                                    </div>
                                                </div>
                                                <small className="text-muted">Reg • Check-ins</small>
                                            </CTableDataCell>
                                            <CTableDataCell>
                                                <div className="d-flex gap-1">
                                                    <Link to={`/events/${event.id}`}>
                                                        <CButton color="outline-primary" size="sm" title="View Event">
                                                            <FontAwesomeIcon icon={faEye} />
                                                        </CButton>
                                                    </Link>
                                                    
                                                    <Link to={`/admin/event/${event.id}/edit`}>
                                                        <CButton color="outline-secondary" size="sm" title="Edit Event">
                                                            <FontAwesomeIcon icon={faEdit} />
                                                        </CButton>
                                                    </Link>
                                                    
                                                    <Link to={`/admin/events/${event.id}/registrations`}>
                                                        <CButton color="outline-warning" size="sm" title="Manage Registrations">
                                                            <FontAwesomeIcon icon={faUserCheck} />
                                                        </CButton>
                                                    </Link>

                                                    <CDropdown>
                                                        <CDropdownToggle color="outline-info" size="sm" caret={false}>
                                                            <FontAwesomeIcon icon={faEllipsisV} />
                                                        </CDropdownToggle>
                                                        <CDropdownMenu>
                                                            <CDropdownItem
                                                                onClick={() => handlePublish(event.id)}
                                                                disabled={event.status === 'PUBLISHED'}
                                                            >
                                                                <FontAwesomeIcon icon={faCalendarCheck} className="me-2" />
                                                                {event.status === 'PUBLISHED' ? 'Published' : 'Publish'}
                                                            </CDropdownItem>
                                                            <CDropdownItem
                                                                onClick={() => handleClone(event.id)}
                                                                disabled={cloning === event.id}
                                                            >
                                                                <FontAwesomeIcon 
                                                                    icon={faClone} 
                                                                    spin={cloning === event.id}
                                                                    className="me-2"
                                                                />
                                                                Clone Event
                                                            </CDropdownItem>
                                                            <hr className="dropdown-divider" />
                                                            <CDropdownItem>
                                                                <FontAwesomeIcon icon={faDownload} className="me-2" />
                                                                Export Data
                                                            </CDropdownItem>
                                                            <CDropdownItem>
                                                                <FontAwesomeIcon icon={faShare} className="me-2" />
                                                                Share Settings
                                                            </CDropdownItem>
                                                            <hr className="dropdown-divider" />
                                                            <CDropdownItem
                                                                onClick={() => handleDelete(event.id)}
                                                                className="text-danger"
                                                            >
                                                                <FontAwesomeIcon icon={faTrash} className="me-2" />
                                                                Delete Event
                                                            </CDropdownItem>
                                                        </CDropdownMenu>
                                                    </CDropdown>
                                                </div>
                                            </CTableDataCell>
                                        </CTableRow>
                                    ))}
                                    {filteredEvents.length === 0 && (
                                        <CTableRow>
                                            <CTableDataCell colSpan={7} className="text-center py-5">
                                                <div className="text-medium-emphasis">
                                                    <FontAwesomeIcon 
                                                        icon={events.length === 0 ? faCalendarAlt : faSearch} 
                                                        size="3x" 
                                                        className="mb-3 text-muted" 
                                                    />
                                                    <h5>
                                                        {events.length === 0 ? 'No events created yet' : 'No events match your filters'}
                                                    </h5>
                                                    <p className="text-muted">
                                                        {events.length === 0 
                                                            ? 'Get started by creating your first event'
                                                            : 'Try adjusting your search or filter criteria'
                                                        }
                                                    </p>
                                                    {events.length === 0 ? (
                                                        <Link to="/admin/event/new">
                                                            <CButton color="primary" size="lg">
                                                                <FontAwesomeIcon icon={faPlus} className="me-2" />
                                                                Create First Event
                                                            </CButton>
                                                        </Link>
                                                    ) : (
                                                        <CButton
                                                            color="outline-secondary"
                                                            onClick={() => setFilters({
                                                                search: '',
                                                                status: 'ALL',
                                                                eventType: 'ALL',
                                                                category: 'ALL',
                                                                dateRange: 'ALL',
                                                                sortBy: 'startDateTime',
                                                                sortOrder: 'desc',
                                                            })}
                                                        >
                                                            Clear All Filters
                                                        </CButton>
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
        </div>
    );
};

export default AdminDashboard;