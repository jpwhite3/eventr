import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';

interface ResourceManagementDashboardProps {
    eventId: string;
}

interface Resource {
    id: string;
    name: string;
    description?: string;
    type: 'ROOM' | 'EQUIPMENT' | 'STAFF' | 'VEHICLE' | 'CATERING' | 'TECHNOLOGY' | 'OTHER';
    status: 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE' | 'RESERVED' | 'OUT_OF_SERVICE';
    capacity?: number;
    location?: string;
    floor?: string;
    building?: string;
    specifications?: string;
    isBookable: boolean;
    requiresApproval: boolean;
    bookingLeadTimeHours: number;
    maxBookingDurationHours?: number;
    hourlyRate?: number;
    dailyRate?: number;
    contactPerson?: string;
    contactEmail?: string;
    totalUsageHours: number;
    usageThisMonth: number;
    lastUsedAt?: string;
    tags: string[];
    isAvailable: boolean;
    currentBookings: number;
    upcomingBookings: number;
}

interface SessionResource {
    id: string;
    sessionId: string;
    sessionTitle: string;
    resourceId: string;
    resourceName: string;
    resourceType: string;
    quantityNeeded: number;
    quantityAllocated: number;
    setupTimeMinutes: number;
    usageTimeMinutes: number;
    cleanupTimeMinutes: number;
    bookingStart?: string;
    bookingEnd?: string;
    isRequired: boolean;
    notes?: string;
    estimatedCost?: number;
    actualCost?: number;
    status: 'REQUESTED' | 'APPROVED' | 'ALLOCATED' | 'IN_USE' | 'COMPLETED' | 'CANCELLED' | 'CONFLICT';
    approvedBy?: string;
    approvedAt?: string;
}

interface ResourceAvailability {
    resourceId: string;
    resourceName: string;
    resourceType: string;
    isAvailable: boolean;
    availableQuantity: number;
    totalQuantity: number;
    conflictingBookings: any[];
    nextAvailableSlot?: string;
}

interface ResourceSearchCriteria {
    searchTerm?: string;
    type?: string;
    location?: string;
    minCapacity?: number;
    availableFrom?: string;
    availableTo?: string;
    tags: string[];
    includeUnavailable: boolean;
}

const ResourceManagementDashboard: React.FC<ResourceManagementDashboardProps> = ({ eventId }) => {
    const [resources, setResources] = useState<Resource[]>([]);
    const [sessionBookings, setSessionBookings] = useState<SessionResource[]>([]);
    const [availableResources, setAvailableResources] = useState<ResourceAvailability[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchCriteria, setSearchCriteria] = useState<ResourceSearchCriteria>({
        tags: [],
        includeUnavailable: false
    });
    const [showBookingModal, setShowBookingModal] = useState(false);
    const [showResourceModal, setShowResourceModal] = useState(false);
    const [selectedResource, setSelectedResource] = useState<Resource | null>(null);
    const [selectedSession, setSelectedSession] = useState<string>('');
    const [sessions, setSessions] = useState<any[]>([]);

    useEffect(() => {
        loadData();
        loadSessions();
    }, [eventId]);

    const loadData = async () => {
        try {
            setLoading(true);
            
            // Search for available resources
            const searchResponse = await apiClient.post('/resources/search', searchCriteria);
            setAvailableResources(searchResponse.data);
            
            // Get all resources for management
            const resourcesResponse = await apiClient.post('/resources/search', {
                ...searchCriteria,
                includeUnavailable: true
            });
            setResources(resourcesResponse.data.map((ra: ResourceAvailability) => ({
                id: ra.resourceId,
                name: ra.resourceName,
                type: ra.resourceType,
                isAvailable: ra.isAvailable,
                // Mock additional properties - in real implementation these would come from the API
                status: ra.isAvailable ? 'AVAILABLE' : 'OCCUPIED',
                isBookable: true,
                requiresApproval: false,
                bookingLeadTimeHours: 0,
                totalUsageHours: 0,
                usageThisMonth: 0,
                tags: [],
                currentBookings: 0,
                upcomingBookings: 0
            })));

        } catch (error) {
            console.error('Failed to load resources:', error);
        } finally {
            setLoading(false);
        }
    };

    const loadSessions = async () => {
        try {
            const response = await apiClient.get(`/sessions/event/${eventId}`);
            setSessions(response.data);
        } catch (error) {
            console.error('Failed to load sessions:', error);
        }
    };

    const handleBookResource = async (resourceId: string, sessionId: string, bookingDetails: any) => {
        try {
            await apiClient.post(`/resources/sessions/${sessionId}/book`, {
                resourceId,
                quantityNeeded: bookingDetails.quantity || 1,
                setupTimeMinutes: bookingDetails.setupTime || 0,
                usageTimeMinutes: bookingDetails.usageTime || 0,
                cleanupTimeMinutes: bookingDetails.cleanupTime || 0,
                isRequired: bookingDetails.isRequired || true,
                notes: bookingDetails.notes,
                estimatedCost: bookingDetails.estimatedCost
            });
            
            await loadData();
            setShowBookingModal(false);
            alert('Resource booked successfully!');
        } catch (error) {
            console.error('Failed to book resource:', error);
            alert('Failed to book resource');
        }
    };

    const handleApproveBooking = async (bookingId: string, approverName: string) => {
        try {
            await apiClient.post(`/resources/bookings/${bookingId}/approve?approverName=${encodeURIComponent(approverName)}`);
            await loadData();
            alert('Booking approved successfully!');
        } catch (error) {
            console.error('Failed to approve booking:', error);
            alert('Failed to approve booking');
        }
    };

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'AVAILABLE': return 'success';
            case 'OCCUPIED': case 'IN_USE': return 'warning';
            case 'MAINTENANCE': return 'info';
            case 'RESERVED': return 'primary';
            case 'OUT_OF_SERVICE': case 'CONFLICT': return 'danger';
            case 'REQUESTED': return 'secondary';
            case 'APPROVED': case 'ALLOCATED': return 'success';
            case 'COMPLETED': return 'success';
            case 'CANCELLED': return 'secondary';
            default: return 'secondary';
        }
    };

    const getTypeIcon = (type: string) => {
        switch (type) {
            case 'ROOM': return '🏢';
            case 'EQUIPMENT': return '⚙️';
            case 'STAFF': return '👤';
            case 'VEHICLE': return '🚗';
            case 'CATERING': return '🍽️';
            case 'TECHNOLOGY': return '💻';
            default: return '📦';
        }
    };

    if (loading) {
        return (
            <div className="text-center p-5">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading resources...</span>
                </div>
                <p className="mt-3">Loading resource management dashboard...</p>
            </div>
        );
    }

    return (
        <div className="resource-management-dashboard">
            {/* Header */}
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h3>🏢 Resource Management</h3>
                    <p className="text-muted mb-0">Manage event resources, bookings, and availability</p>
                </div>
                <div className="btn-group">
                    <button className="btn btn-outline-primary" onClick={loadData}>
                        🔄 Refresh
                    </button>
                    <button 
                        className="btn btn-primary"
                        onClick={() => {
                            setSelectedResource(null);
                            setShowResourceModal(true);
                        }}
                    >
                        ➕ Add Resource
                    </button>
                </div>
            </div>

            {/* Search and Filters */}
            <div className="card mb-4">
                <div className="card-header">
                    <h5>🔍 Search & Filter Resources</h5>
                </div>
                <div className="card-body">
                    <div className="row">
                        <div className="col-md-3 mb-3">
                            <label className="form-label">Search</label>
                            <input
                                type="text"
                                className="form-control"
                                placeholder="Search resources..."
                                value={searchCriteria.searchTerm || ''}
                                onChange={(e) => setSearchCriteria({
                                    ...searchCriteria,
                                    searchTerm: e.target.value
                                })}
                            />
                        </div>
                        <div className="col-md-2 mb-3">
                            <label className="form-label">Type</label>
                            <select
                                className="form-select"
                                value={searchCriteria.type || ''}
                                onChange={(e) => setSearchCriteria({
                                    ...searchCriteria,
                                    type: e.target.value || undefined
                                })}
                            >
                                <option value="">All Types</option>
                                <option value="ROOM">Rooms</option>
                                <option value="EQUIPMENT">Equipment</option>
                                <option value="STAFF">Staff</option>
                                <option value="VEHICLE">Vehicles</option>
                                <option value="CATERING">Catering</option>
                                <option value="TECHNOLOGY">Technology</option>
                                <option value="OTHER">Other</option>
                            </select>
                        </div>
                        <div className="col-md-2 mb-3">
                            <label className="form-label">Location</label>
                            <input
                                type="text"
                                className="form-control"
                                placeholder="Location..."
                                value={searchCriteria.location || ''}
                                onChange={(e) => setSearchCriteria({
                                    ...searchCriteria,
                                    location: e.target.value || undefined
                                })}
                            />
                        </div>
                        <div className="col-md-2 mb-3">
                            <label className="form-label">Min Capacity</label>
                            <input
                                type="number"
                                className="form-control"
                                placeholder="0"
                                value={searchCriteria.minCapacity || ''}
                                onChange={(e) => setSearchCriteria({
                                    ...searchCriteria,
                                    minCapacity: e.target.value ? parseInt(e.target.value) : undefined
                                })}
                            />
                        </div>
                        <div className="col-md-3 mb-3">
                            <label className="form-label">Available Period</label>
                            <div className="d-flex gap-2">
                                <input
                                    type="datetime-local"
                                    className="form-control form-control-sm"
                                    value={searchCriteria.availableFrom || ''}
                                    onChange={(e) => setSearchCriteria({
                                        ...searchCriteria,
                                        availableFrom: e.target.value || undefined
                                    })}
                                />
                                <input
                                    type="datetime-local"
                                    className="form-control form-control-sm"
                                    value={searchCriteria.availableTo || ''}
                                    onChange={(e) => setSearchCriteria({
                                        ...searchCriteria,
                                        availableTo: e.target.value || undefined
                                    })}
                                />
                            </div>
                        </div>
                    </div>
                    <div className="row">
                        <div className="col-md-6">
                            <div className="form-check">
                                <input
                                    className="form-check-input"
                                    type="checkbox"
                                    checked={searchCriteria.includeUnavailable}
                                    onChange={(e) => setSearchCriteria({
                                        ...searchCriteria,
                                        includeUnavailable: e.target.checked
                                    })}
                                />
                                <label className="form-check-label">
                                    Include unavailable resources
                                </label>
                            </div>
                        </div>
                        <div className="col-md-6 text-end">
                            <button className="btn btn-primary" onClick={loadData}>
                                🔍 Apply Filters
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            {/* Resource Statistics */}
            <div className="row mb-4">
                <div className="col-lg-3 col-md-6 mb-3">
                    <div className="card bg-primary text-white">
                        <div className="card-body">
                            <div className="d-flex justify-content-between">
                                <div>
                                    <div className="h4 mb-0">{resources.length}</div>
                                    <p className="card-text">Total Resources</p>
                                </div>
                                <div className="align-self-center">
                                    <span className="h1">📦</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="col-lg-3 col-md-6 mb-3">
                    <div className="card bg-success text-white">
                        <div className="card-body">
                            <div className="d-flex justify-content-between">
                                <div>
                                    <div className="h4 mb-0">{resources.filter(r => r.isAvailable).length}</div>
                                    <p className="card-text">Available Now</p>
                                </div>
                                <div className="align-self-center">
                                    <span className="h1">✅</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="col-lg-3 col-md-6 mb-3">
                    <div className="card bg-warning text-dark">
                        <div className="card-body">
                            <div className="d-flex justify-content-between">
                                <div>
                                    <div className="h4 mb-0">{resources.filter(r => !r.isAvailable).length}</div>
                                    <p className="card-text">In Use/Booked</p>
                                </div>
                                <div className="align-self-center">
                                    <span className="h1">⏳</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="col-lg-3 col-md-6 mb-3">
                    <div className="card bg-info text-white">
                        <div className="card-body">
                            <div className="d-flex justify-content-between">
                                <div>
                                    <div className="h4 mb-0">{resources.filter(r => r.requiresApproval).length}</div>
                                    <p className="card-text">Require Approval</p>
                                </div>
                                <div className="align-self-center">
                                    <span className="h1">🔒</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Resources Table */}
            <div className="card">
                <div className="card-header">
                    <h5>📋 Resource Inventory</h5>
                </div>
                <div className="card-body p-0">
                    <div className="table-responsive">
                        <table className="table table-hover mb-0">
                            <thead className="table-light">
                                <tr>
                                    <th>Resource</th>
                                    <th>Type</th>
                                    <th>Status</th>
                                    <th>Location</th>
                                    <th>Capacity</th>
                                    <th>Usage</th>
                                    <th>Bookings</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {resources.map((resource) => (
                                    <tr key={resource.id}>
                                        <td>
                                            <div className="d-flex align-items-center">
                                                <span className="me-2">{getTypeIcon(resource.type)}</span>
                                                <div>
                                                    <strong>{resource.name}</strong>
                                                    {resource.description && (
                                                        <br />
                                                        <small className="text-muted">{resource.description}</small>
                                                    )}
                                                    {resource.requiresApproval && (
                                                        <span className="badge bg-warning ms-2">Requires Approval</span>
                                                    )}
                                                </div>
                                            </div>
                                        </td>
                                        <td>
                                            <span className="badge bg-secondary">
                                                {resource.type}
                                            </span>
                                        </td>
                                        <td>
                                            <span className={`badge bg-${getStatusColor(resource.status)}`}>
                                                {resource.status}
                                            </span>
                                        </td>
                                        <td>
                                            {resource.location ? (
                                                <>
                                                    {resource.location}
                                                    {resource.floor && <><br /><small className="text-muted">Floor {resource.floor}</small></>}
                                                </>
                                            ) : (
                                                <span className="text-muted">Not specified</span>
                                            )}
                                        </td>
                                        <td>
                                            {resource.capacity ? (
                                                <span className="badge bg-info">{resource.capacity}</span>
                                            ) : (
                                                <span className="text-muted">N/A</span>
                                            )}
                                        </td>
                                        <td>
                                            <div>
                                                <strong>{resource.totalUsageHours}</strong>h total
                                                <br />
                                                <small className="text-muted">{resource.usageThisMonth}h this month</small>
                                            </div>
                                        </td>
                                        <td>
                                            <div>
                                                <span className="badge bg-primary me-1">{resource.currentBookings}</span> active
                                                <br />
                                                <span className="badge bg-secondary">{resource.upcomingBookings}</span> upcoming
                                            </div>
                                        </td>
                                        <td>
                                            <div className="btn-group btn-group-sm">
                                                <button 
                                                    className="btn btn-outline-primary"
                                                    onClick={() => {
                                                        setSelectedResource(resource);
                                                        setShowBookingModal(true);
                                                    }}
                                                    disabled={!resource.isBookable || !resource.isAvailable}
                                                    title="Book Resource"
                                                >
                                                    📅
                                                </button>
                                                <button 
                                                    className="btn btn-outline-secondary"
                                                    onClick={() => {
                                                        setSelectedResource(resource);
                                                        setShowResourceModal(true);
                                                    }}
                                                    title="Edit Resource"
                                                >
                                                    ✏️
                                                </button>
                                                <button 
                                                    className="btn btn-outline-info"
                                                    title="View Details"
                                                >
                                                    👁️
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    {resources.length === 0 && (
                        <div className="text-center py-5 text-muted">
                            <span className="h1">📦</span>
                            <p className="mb-0">No resources match your search criteria</p>
                        </div>
                    )}
                </div>
            </div>

            {/* Resource Booking Modal */}
            {showBookingModal && selectedResource && (
                <ResourceBookingModal
                    resource={selectedResource}
                    sessions={sessions}
                    onClose={() => setShowBookingModal(false)}
                    onBook={handleBookResource}
                />
            )}

            {/* Resource Management Modal */}
            {showResourceModal && (
                <ResourceManagementModal
                    resource={selectedResource}
                    onClose={() => setShowResourceModal(false)}
                    onSave={() => {
                        setShowResourceModal(false);
                        loadData();
                    }}
                />
            )}
        </div>
    );
};

// Resource Booking Modal
const ResourceBookingModal: React.FC<{
    resource: Resource;
    sessions: any[];
    onClose: () => void;
    onBook: (resourceId: string, sessionId: string, details: any) => void;
}> = ({ resource, sessions, onClose, onBook }) => {
    const [formData, setFormData] = useState({
        sessionId: '',
        quantity: 1,
        setupTime: 0,
        usageTime: 0,
        cleanupTime: 0,
        isRequired: true,
        notes: '',
        estimatedCost: resource.hourlyRate || 0
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!formData.sessionId) {
            alert('Please select a session');
            return;
        }
        onBook(resource.id, formData.sessionId, formData);
    };

    return (
        <div className="modal fade show" style={{display: 'block', backgroundColor: 'rgba(0,0,0,0.5)'}} tabIndex={-1}>
            <div className="modal-dialog modal-lg">
                <div className="modal-content">
                    <div className="modal-header">
                        <h5 className="modal-title">📅 Book Resource - {resource.name}</h5>
                        <button type="button" className="btn-close" onClick={onClose}></button>
                    </div>
                    <form onSubmit={handleSubmit}>
                        <div className="modal-body">
                            <div className="row">
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Session *</label>
                                    <select
                                        className="form-select"
                                        value={formData.sessionId}
                                        onChange={(e) => setFormData({...formData, sessionId: e.target.value})}
                                        required
                                    >
                                        <option value="">Select a session...</option>
                                        {sessions.map(session => (
                                            <option key={session.id} value={session.id}>
                                                {session.title} - {new Date(session.startTime).toLocaleString()}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Quantity Needed</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.quantity}
                                        onChange={(e) => setFormData({...formData, quantity: parseInt(e.target.value)})}
                                        min="1"
                                        max={resource.capacity || 999}
                                    />
                                </div>
                            </div>

                            <div className="row">
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Setup Time (minutes)</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.setupTime}
                                        onChange={(e) => setFormData({...formData, setupTime: parseInt(e.target.value) || 0})}
                                        min="0"
                                    />
                                </div>
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Usage Time (minutes)</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.usageTime}
                                        onChange={(e) => setFormData({...formData, usageTime: parseInt(e.target.value) || 0})}
                                        min="0"
                                        placeholder="Use session duration"
                                    />
                                    <small className="text-muted">Leave 0 to use full session duration</small>
                                </div>
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Cleanup Time (minutes)</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.cleanupTime}
                                        onChange={(e) => setFormData({...formData, cleanupTime: parseInt(e.target.value) || 0})}
                                        min="0"
                                    />
                                </div>
                            </div>

                            <div className="row">
                                <div className="col-md-6 mb-3">
                                    <div className="form-check">
                                        <input
                                            className="form-check-input"
                                            type="checkbox"
                                            checked={formData.isRequired}
                                            onChange={(e) => setFormData({...formData, isRequired: e.target.checked})}
                                        />
                                        <label className="form-check-label">
                                            Required for session
                                        </label>
                                    </div>
                                </div>
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Estimated Cost</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.estimatedCost}
                                        onChange={(e) => setFormData({...formData, estimatedCost: parseFloat(e.target.value) || 0})}
                                        min="0"
                                        step="0.01"
                                    />
                                </div>
                            </div>

                            <div className="mb-3">
                                <label className="form-label">Notes</label>
                                <textarea
                                    className="form-control"
                                    rows={3}
                                    value={formData.notes}
                                    onChange={(e) => setFormData({...formData, notes: e.target.value})}
                                    placeholder="Special requirements, setup instructions, etc."
                                />
                            </div>

                            <div className="alert alert-info">
                                <h6>Resource Details:</h6>
                                <ul className="mb-0">
                                    <li><strong>Type:</strong> {resource.type}</li>
                                    <li><strong>Location:</strong> {resource.location || 'Not specified'}</li>
                                    {resource.capacity && <li><strong>Capacity:</strong> {resource.capacity}</li>}
                                    {resource.bookingLeadTimeHours > 0 && (
                                        <li><strong>Lead Time:</strong> {resource.bookingLeadTimeHours} hours</li>
                                    )}
                                    {resource.requiresApproval && (
                                        <li><strong>Approval:</strong> Required before use</li>
                                    )}
                                </ul>
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button type="button" className="btn btn-secondary" onClick={onClose}>
                                Cancel
                            </button>
                            <button type="submit" className="btn btn-primary">
                                📅 Book Resource
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

// Resource Management Modal
const ResourceManagementModal: React.FC<{
    resource: Resource | null;
    onClose: () => void;
    onSave: () => void;
}> = ({ resource, onClose, onSave }) => {
    const [formData, setFormData] = useState({
        name: resource?.name || '',
        description: resource?.description || '',
        type: resource?.type || 'OTHER',
        capacity: resource?.capacity || '',
        location: resource?.location || '',
        floor: resource?.floor || '',
        building: resource?.building || '',
        specifications: resource?.specifications || '',
        isBookable: resource?.isBookable ?? true,
        requiresApproval: resource?.requiresApproval ?? false,
        bookingLeadTimeHours: resource?.bookingLeadTimeHours || 0,
        hourlyRate: resource?.hourlyRate || '',
        contactPerson: resource?.contactPerson || '',
        contactEmail: resource?.contactEmail || '',
        tags: resource?.tags?.join(', ') || ''
    });

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const payload = {
                ...formData,
                capacity: formData.capacity ? parseInt(formData.capacity as string) : null,
                hourlyRate: formData.hourlyRate ? parseFloat(formData.hourlyRate as string) : null,
                tags: formData.tags.split(',').map(t => t.trim()).filter(t => t)
            };

            if (resource) {
                await apiClient.put(`/resources/${resource.id}`, payload);
            } else {
                await apiClient.post('/resources', payload);
            }
            
            onSave();
            alert('Resource saved successfully!');
        } catch (error) {
            console.error('Failed to save resource:', error);
            alert('Failed to save resource');
        }
    };

    return (
        <div className="modal fade show" style={{display: 'block', backgroundColor: 'rgba(0,0,0,0.5)'}} tabIndex={-1}>
            <div className="modal-dialog modal-xl">
                <div className="modal-content">
                    <div className="modal-header">
                        <h5 className="modal-title">
                            {resource ? '✏️ Edit Resource' : '➕ Add New Resource'}
                        </h5>
                        <button type="button" className="btn-close" onClick={onClose}></button>
                    </div>
                    <form onSubmit={handleSubmit}>
                        <div className="modal-body">
                            <div className="row">
                                <div className="col-md-8 mb-3">
                                    <label className="form-label">Resource Name *</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={formData.name}
                                        onChange={(e) => setFormData({...formData, name: e.target.value})}
                                        required
                                    />
                                </div>
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Type</label>
                                    <select
                                        className="form-select"
                                        value={formData.type}
                                        onChange={(e) => setFormData({...formData, type: e.target.value as any})}
                                    >
                                        <option value="ROOM">Room</option>
                                        <option value="EQUIPMENT">Equipment</option>
                                        <option value="STAFF">Staff</option>
                                        <option value="VEHICLE">Vehicle</option>
                                        <option value="CATERING">Catering</option>
                                        <option value="TECHNOLOGY">Technology</option>
                                        <option value="OTHER">Other</option>
                                    </select>
                                </div>
                            </div>

                            <div className="mb-3">
                                <label className="form-label">Description</label>
                                <textarea
                                    className="form-control"
                                    rows={2}
                                    value={formData.description}
                                    onChange={(e) => setFormData({...formData, description: e.target.value})}
                                />
                            </div>

                            <div className="row">
                                <div className="col-md-3 mb-3">
                                    <label className="form-label">Capacity</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.capacity}
                                        onChange={(e) => setFormData({...formData, capacity: e.target.value})}
                                        min="1"
                                    />
                                </div>
                                <div className="col-md-3 mb-3">
                                    <label className="form-label">Location</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={formData.location}
                                        onChange={(e) => setFormData({...formData, location: e.target.value})}
                                    />
                                </div>
                                <div className="col-md-3 mb-3">
                                    <label className="form-label">Floor</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={formData.floor}
                                        onChange={(e) => setFormData({...formData, floor: e.target.value})}
                                    />
                                </div>
                                <div className="col-md-3 mb-3">
                                    <label className="form-label">Building</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={formData.building}
                                        onChange={(e) => setFormData({...formData, building: e.target.value})}
                                    />
                                </div>
                            </div>

                            <div className="row">
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Lead Time (hours)</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.bookingLeadTimeHours}
                                        onChange={(e) => setFormData({...formData, bookingLeadTimeHours: parseInt(e.target.value) || 0})}
                                        min="0"
                                    />
                                </div>
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Hourly Rate</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.hourlyRate}
                                        onChange={(e) => setFormData({...formData, hourlyRate: e.target.value})}
                                        min="0"
                                        step="0.01"
                                    />
                                </div>
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Contact Person</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={formData.contactPerson}
                                        onChange={(e) => setFormData({...formData, contactPerson: e.target.value})}
                                    />
                                </div>
                            </div>

                            <div className="row">
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Contact Email</label>
                                    <input
                                        type="email"
                                        className="form-control"
                                        value={formData.contactEmail}
                                        onChange={(e) => setFormData({...formData, contactEmail: e.target.value})}
                                    />
                                </div>
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Tags (comma-separated)</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={formData.tags}
                                        onChange={(e) => setFormData({...formData, tags: e.target.value})}
                                        placeholder="audio, video, presentation, etc."
                                    />
                                </div>
                            </div>

                            <div className="mb-3">
                                <label className="form-label">Specifications</label>
                                <textarea
                                    className="form-control"
                                    rows={3}
                                    value={formData.specifications}
                                    onChange={(e) => setFormData({...formData, specifications: e.target.value})}
                                    placeholder="Technical specifications, features, requirements..."
                                />
                            </div>

                            <div className="row">
                                <div className="col-md-6 mb-3">
                                    <div className="form-check">
                                        <input
                                            className="form-check-input"
                                            type="checkbox"
                                            checked={formData.isBookable}
                                            onChange={(e) => setFormData({...formData, isBookable: e.target.checked})}
                                        />
                                        <label className="form-check-label">
                                            Resource is bookable
                                        </label>
                                    </div>
                                </div>
                                <div className="col-md-6 mb-3">
                                    <div className="form-check">
                                        <input
                                            className="form-check-input"
                                            type="checkbox"
                                            checked={formData.requiresApproval}
                                            onChange={(e) => setFormData({...formData, requiresApproval: e.target.checked})}
                                        />
                                        <label className="form-check-label">
                                            Requires approval before booking
                                        </label>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button type="button" className="btn btn-secondary" onClick={onClose}>
                                Cancel
                            </button>
                            <button type="submit" className="btn btn-primary">
                                {resource ? '💾 Update Resource' : '➕ Create Resource'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default ResourceManagementDashboard;