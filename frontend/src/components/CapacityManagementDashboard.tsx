import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';

interface CapacityManagementDashboardProps {
    eventId: string;
}

interface SessionCapacity {
    id: string;
    sessionId: string;
    sessionTitle: string;
    capacityType: 'FIXED' | 'DYNAMIC' | 'UNLIMITED';
    maximumCapacity: number;
    minimumCapacity: number;
    currentRegistrations: number;
    availableSpots: number;
    utilizationPercentage: number;
    enableWaitlist: boolean;
    waitlistCapacity?: number;
    currentWaitlistCount: number;
    waitlistStrategy: 'NONE' | 'FIFO' | 'PRIORITY_BASED' | 'REGISTRATION_TIME';
    allowOverbooking: boolean;
    overbookingPercentage: number;
    autoPromoteFromWaitlist: boolean;
    lowCapacityThreshold: number;
    highDemandThreshold: number;
    isLowCapacity: boolean;
    isHighDemand: boolean;
    lastCapacityUpdate?: string;
}

interface CapacityAnalytics {
    eventId: string;
    eventName: string;
    totalSessions: number;
    averageUtilization: number;
    fullSessionsCount: number;
    underCapacitySessionsCount: number;
    totalWaitlistCount: number;
    overbookedSessionsCount: number;
    sessionCapacities: SessionCapacity[];
    waitlistDistribution: Record<string, number>;
    generatedAt: string;
}

interface OptimizationSuggestion {
    sessionId: string;
    sessionTitle: string;
    currentCapacity: number;
    currentRegistrations: number;
    suggestedCapacity: number;
    optimizationType: 'INCREASE' | 'DECREASE' | 'REDISTRIBUTE';
    reason: string;
    potentialImpact: string;
    priority: 'LOW' | 'MEDIUM' | 'HIGH';
}

const CapacityManagementDashboard: React.FC<CapacityManagementDashboardProps> = ({ eventId }) => {
    const [analytics, setAnalytics] = useState<CapacityAnalytics | null>(null);
    const [suggestions, setSuggestions] = useState<OptimizationSuggestion[]>([]);
    const [loading, setLoading] = useState(true);
    const [selectedSession, setSelectedSession] = useState<SessionCapacity | null>(null);
    const [showCapacityModal, setShowCapacityModal] = useState(false);
    const [showWaitlistModal, setShowWaitlistModal] = useState(false);
    const [autoRefresh, setAutoRefresh] = useState(true);

    useEffect(() => {
        loadData();
    }, [eventId]);

    useEffect(() => {
        if (!autoRefresh) return;
        
        const interval = setInterval(() => {
            loadData();
        }, 30000); // Refresh every 30 seconds

        return () => clearInterval(interval);
    }, [eventId, autoRefresh]);

    const loadData = async () => {
        try {
            const [analyticsResponse, suggestionsResponse] = await Promise.all([
                apiClient.get(`/capacity/events/${eventId}/analytics`),
                apiClient.get(`/capacity/events/${eventId}/optimization-suggestions`)
            ]);

            setAnalytics(analyticsResponse.data);
            setSuggestions(suggestionsResponse.data);
        } catch (error) {
            console.error('Failed to load capacity data:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleCapacityUpdate = async (sessionId: string, updates: any) => {
        try {
            await apiClient.put(`/capacity/sessions/${sessionId}`, {
                ...updates,
                sessionId,
                reason: 'Manual capacity adjustment'
            });
            
            await loadData(); // Refresh data
            setShowCapacityModal(false);
        } catch (error) {
            console.error('Failed to update capacity:', error);
            alert('Failed to update capacity');
        }
    };

    const handleAutoPromotion = async () => {
        try {
            await apiClient.post('/capacity/waitlist/auto-promote');
            await loadData();
            alert('Auto-promotion completed successfully!');
        } catch (error) {
            console.error('Auto-promotion failed:', error);
            alert('Auto-promotion failed');
        }
    };

    const handleManualPromotion = async (sessionId: string, registrationIds: string[]) => {
        try {
            await apiClient.post('/capacity/waitlist/promote', {
                sessionId,
                registrationIds,
                promotionReason: 'Manual promotion by administrator',
                notifyAttendees: true
            });
            
            await loadData();
            setShowWaitlistModal(false);
            alert(`Successfully promoted ${registrationIds.length} attendees from waitlist!`);
        } catch (error) {
            console.error('Manual promotion failed:', error);
            alert('Manual promotion failed');
        }
    };

    const getUtilizationColor = (percentage: number) => {
        if (percentage >= 90) return 'danger';
        if (percentage >= 75) return 'warning';
        if (percentage >= 50) return 'success';
        return 'info';
    };

    const getPriorityColor = (priority: string) => {
        switch (priority) {
            case 'HIGH': return 'danger';
            case 'MEDIUM': return 'warning';
            case 'LOW': return 'info';
            default: return 'secondary';
        }
    };

    if (loading) {
        return (
            <div className="text-center p-5">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading capacity analytics...</span>
                </div>
                <p className="mt-3">Loading capacity management dashboard...</p>
            </div>
        );
    }

    if (!analytics) {
        return (
            <div className="alert alert-warning">
                <h5>📊 No Capacity Data Available</h5>
                <p>Unable to load capacity analytics for this event.</p>
                <button className="btn btn-primary" onClick={loadData}>
                    🔄 Retry
                </button>
            </div>
        );
    }

    return (
        <div className="capacity-management-dashboard">
            {/* Header Controls */}
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h3>📊 Capacity Management - {analytics.eventName}</h3>
                    <p className="text-muted mb-0">
                        Real-time capacity monitoring and optimization
                        {autoRefresh && <span className="badge bg-success ms-2">🔄 Auto-refresh</span>}
                    </p>
                </div>
                <div className="btn-group">
                    <button 
                        className={`btn btn-sm ${autoRefresh ? 'btn-success' : 'btn-outline-secondary'}`}
                        onClick={() => setAutoRefresh(!autoRefresh)}
                    >
                        {autoRefresh ? '⏸️ Pause' : '▶️ Resume'}
                    </button>
                    <button className="btn btn-sm btn-outline-primary" onClick={loadData}>
                        🔄 Refresh
                    </button>
                    <button className="btn btn-sm btn-primary" onClick={handleAutoPromotion}>
                        🚀 Auto-Promote Waitlists
                    </button>
                </div>
            </div>

            {/* Key Metrics */}
            <div className="row mb-4">
                <div className="col-lg-3 col-md-6 mb-3">
                    <div className="card bg-primary text-white">
                        <div className="card-body">
                            <div className="d-flex justify-content-between">
                                <div>
                                    <div className="h4 mb-0">{analytics.totalSessions}</div>
                                    <p className="card-text">Total Sessions</p>
                                </div>
                                <div className="align-self-center">
                                    <span className="h1">📅</span>
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
                                    <div className="h4 mb-0">{analytics.averageUtilization.toFixed(1)}%</div>
                                    <p className="card-text">Avg Utilization</p>
                                </div>
                                <div className="align-self-center">
                                    <span className="h1">📈</span>
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
                                    <div className="h4 mb-0">{analytics.totalWaitlistCount}</div>
                                    <p className="card-text">Total Waitlisted</p>
                                </div>
                                <div className="align-self-center">
                                    <span className="h1">⏳</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="col-lg-3 col-md-6 mb-3">
                    <div className="card bg-danger text-white">
                        <div className="card-body">
                            <div className="d-flex justify-content-between">
                                <div>
                                    <div className="h4 mb-0">{analytics.fullSessionsCount}</div>
                                    <p className="card-text">Full Sessions</p>
                                </div>
                                <div className="align-self-center">
                                    <span className="h1">🚫</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Optimization Suggestions */}
            {suggestions.length > 0 && (
                <div className="card mb-4">
                    <div className="card-header">
                        <h5>💡 Capacity Optimization Suggestions</h5>
                    </div>
                    <div className="card-body">
                        {suggestions.map((suggestion, index) => (
                            <div key={index} className={`alert alert-${getPriorityColor(suggestion.priority)} d-flex justify-content-between align-items-start`}>
                                <div>
                                    <h6 className="alert-heading">
                                        <span className={`badge bg-${getPriorityColor(suggestion.priority)} me-2`}>
                                            {suggestion.priority}
                                        </span>
                                        {suggestion.sessionTitle}
                                    </h6>
                                    <p className="mb-1"><strong>Suggestion:</strong> {suggestion.reason}</p>
                                    <p className="mb-1"><strong>Impact:</strong> {suggestion.potentialImpact}</p>
                                    <small>
                                        Current: {suggestion.currentRegistrations}/{suggestion.currentCapacity} → 
                                        Suggested: {suggestion.suggestedCapacity}
                                    </small>
                                </div>
                                <button 
                                    className="btn btn-sm btn-outline-primary"
                                    onClick={() => {
                                        const session = analytics.sessionCapacities.find(s => s.sessionId === suggestion.sessionId);
                                        if (session) {
                                            setSelectedSession(session);
                                            setShowCapacityModal(true);
                                        }
                                    }}
                                >
                                    Adjust
                                </button>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* Session Capacity Details */}
            <div className="card">
                <div className="card-header">
                    <h5>📋 Session Capacity Details</h5>
                </div>
                <div className="card-body p-0">
                    <div className="table-responsive">
                        <table className="table table-hover mb-0">
                            <thead className="table-light">
                                <tr>
                                    <th>Session</th>
                                    <th>Type</th>
                                    <th>Capacity</th>
                                    <th>Registered</th>
                                    <th>Utilization</th>
                                    <th>Waitlist</th>
                                    <th>Status</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {analytics.sessionCapacities.map((session) => (
                                    <tr key={session.id}>
                                        <td>
                                            <strong>{session.sessionTitle}</strong>
                                            {session.isLowCapacity && <span className="badge bg-info ms-2">Low Capacity</span>}
                                            {session.isHighDemand && <span className="badge bg-warning ms-2">High Demand</span>}
                                        </td>
                                        <td>
                                            <span className={`badge ${session.capacityType === 'DYNAMIC' ? 'bg-success' : 'bg-secondary'}`}>
                                                {session.capacityType}
                                            </span>
                                        </td>
                                        <td>
                                            {session.minimumCapacity !== session.maximumCapacity ? 
                                                `${session.minimumCapacity}-${session.maximumCapacity}` : 
                                                session.maximumCapacity
                                            }
                                            {session.allowOverbooking && <small className="text-muted"> (+{session.overbookingPercentage}%)</small>}
                                        </td>
                                        <td>
                                            <strong>{session.currentRegistrations}</strong>
                                            <small className="text-muted"> / {session.availableSpots} available</small>
                                        </td>
                                        <td>
                                            <div className="d-flex align-items-center">
                                                <div className="progress me-2" style={{width: '60px', height: '6px'}}>
                                                    <div 
                                                        className={`progress-bar bg-${getUtilizationColor(session.utilizationPercentage)}`}
                                                        style={{width: `${Math.min(100, session.utilizationPercentage)}%`}}
                                                    />
                                                </div>
                                                <small>{session.utilizationPercentage.toFixed(1)}%</small>
                                            </div>
                                        </td>
                                        <td>
                                            {session.enableWaitlist ? (
                                                <>
                                                    <strong>{session.currentWaitlistCount}</strong>
                                                    {session.waitlistCapacity && <small className="text-muted"> / {session.waitlistCapacity}</small>}
                                                    <br />
                                                    <small className="text-muted">{session.waitlistStrategy}</small>
                                                </>
                                            ) : (
                                                <span className="text-muted">Disabled</span>
                                            )}
                                        </td>
                                        <td>
                                            {session.availableSpots === 0 ? (
                                                <span className="badge bg-danger">Full</span>
                                            ) : session.utilizationPercentage >= 90 ? (
                                                <span className="badge bg-warning">Nearly Full</span>
                                            ) : (
                                                <span className="badge bg-success">Available</span>
                                            )}
                                        </td>
                                        <td>
                                            <div className="btn-group btn-group-sm">
                                                <button 
                                                    className="btn btn-outline-primary"
                                                    onClick={() => {
                                                        setSelectedSession(session);
                                                        setShowCapacityModal(true);
                                                    }}
                                                    title="Manage Capacity"
                                                >
                                                    ⚙️
                                                </button>
                                                {session.currentWaitlistCount > 0 && (
                                                    <button 
                                                        className="btn btn-outline-success"
                                                        onClick={() => {
                                                            setSelectedSession(session);
                                                            setShowWaitlistModal(true);
                                                        }}
                                                        title="Manage Waitlist"
                                                    >
                                                        👥
                                                    </button>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            {/* Capacity Management Modal */}
            {showCapacityModal && selectedSession && (
                <CapacityManagementModal
                    session={selectedSession}
                    onClose={() => setShowCapacityModal(false)}
                    onUpdate={handleCapacityUpdate}
                />
            )}

            {/* Waitlist Management Modal */}
            {showWaitlistModal && selectedSession && (
                <WaitlistManagementModal
                    session={selectedSession}
                    onClose={() => setShowWaitlistModal(false)}
                    onPromote={handleManualPromotion}
                />
            )}
        </div>
    );
};

// Capacity Management Modal Component
const CapacityManagementModal: React.FC<{
    session: SessionCapacity;
    onClose: () => void;
    onUpdate: (sessionId: string, updates: any) => void;
}> = ({ session, onClose, onUpdate }) => {
    const [formData, setFormData] = useState({
        maximumCapacity: session.maximumCapacity,
        minimumCapacity: session.minimumCapacity,
        capacityType: session.capacityType,
        enableWaitlist: session.enableWaitlist,
        waitlistCapacity: session.waitlistCapacity || '',
        waitlistStrategy: session.waitlistStrategy,
        allowOverbooking: session.allowOverbooking,
        overbookingPercentage: session.overbookingPercentage,
        autoPromoteFromWaitlist: session.autoPromoteFromWaitlist
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onUpdate(session.sessionId, {
            ...formData,
            waitlistCapacity: formData.waitlistCapacity || null
        });
    };

    return (
        <div className="modal fade show" style={{display: 'block', backgroundColor: 'rgba(0,0,0,0.5)'}} tabIndex={-1}>
            <div className="modal-dialog modal-lg">
                <div className="modal-content">
                    <div className="modal-header">
                        <h5 className="modal-title">⚙️ Manage Capacity - {session.sessionTitle}</h5>
                        <button type="button" className="btn-close" onClick={onClose}></button>
                    </div>
                    <form onSubmit={handleSubmit}>
                        <div className="modal-body">
                            <div className="row">
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Maximum Capacity *</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.maximumCapacity}
                                        onChange={(e) => setFormData({...formData, maximumCapacity: parseInt(e.target.value)})}
                                        min="1"
                                        required
                                    />
                                </div>
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Minimum Capacity</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.minimumCapacity}
                                        onChange={(e) => setFormData({...formData, minimumCapacity: parseInt(e.target.value)})}
                                        min="0"
                                    />
                                </div>
                            </div>

                            <div className="row">
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Capacity Type</label>
                                    <select
                                        className="form-select"
                                        value={formData.capacityType}
                                        onChange={(e) => setFormData({...formData, capacityType: e.target.value as any})}
                                    >
                                        <option value="FIXED">Fixed</option>
                                        <option value="DYNAMIC">Dynamic</option>
                                        <option value="UNLIMITED">Unlimited</option>
                                    </select>
                                </div>
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Waitlist Strategy</label>
                                    <select
                                        className="form-select"
                                        value={formData.waitlistStrategy}
                                        onChange={(e) => setFormData({...formData, waitlistStrategy: e.target.value as any})}
                                        disabled={!formData.enableWaitlist}
                                    >
                                        <option value="FIFO">First In, First Out</option>
                                        <option value="PRIORITY_BASED">Priority Based</option>
                                        <option value="REGISTRATION_TIME">Registration Time</option>
                                    </select>
                                </div>
                            </div>

                            <div className="row">
                                <div className="col-md-6 mb-3">
                                    <div className="form-check">
                                        <input
                                            className="form-check-input"
                                            type="checkbox"
                                            checked={formData.enableWaitlist}
                                            onChange={(e) => setFormData({...formData, enableWaitlist: e.target.checked})}
                                        />
                                        <label className="form-check-label">Enable Waitlist</label>
                                    </div>
                                </div>
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Waitlist Capacity (optional)</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.waitlistCapacity}
                                        onChange={(e) => setFormData({...formData, waitlistCapacity: e.target.value})}
                                        disabled={!formData.enableWaitlist}
                                        placeholder="Unlimited"
                                    />
                                </div>
                            </div>

                            <div className="row">
                                <div className="col-md-6 mb-3">
                                    <div className="form-check">
                                        <input
                                            className="form-check-input"
                                            type="checkbox"
                                            checked={formData.allowOverbooking}
                                            onChange={(e) => setFormData({...formData, allowOverbooking: e.target.checked})}
                                        />
                                        <label className="form-check-label">Allow Overbooking</label>
                                    </div>
                                </div>
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Overbooking Percentage</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.overbookingPercentage}
                                        onChange={(e) => setFormData({...formData, overbookingPercentage: parseFloat(e.target.value)})}
                                        disabled={!formData.allowOverbooking}
                                        min="0"
                                        max="50"
                                        step="0.1"
                                    />
                                </div>
                            </div>

                            <div className="mb-3">
                                <div className="form-check">
                                    <input
                                        className="form-check-input"
                                        type="checkbox"
                                        checked={formData.autoPromoteFromWaitlist}
                                        onChange={(e) => setFormData({...formData, autoPromoteFromWaitlist: e.target.checked})}
                                        disabled={!formData.enableWaitlist}
                                    />
                                    <label className="form-check-label">Auto-promote from waitlist when spots become available</label>
                                </div>
                            </div>

                            <div className="alert alert-info">
                                <strong>Current Status:</strong> {session.currentRegistrations} registered, {session.availableSpots} spots available
                                {session.currentWaitlistCount > 0 && `, ${session.currentWaitlistCount} waitlisted`}
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button type="button" className="btn btn-secondary" onClick={onClose}>
                                Cancel
                            </button>
                            <button type="submit" className="btn btn-primary">
                                Update Capacity
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

// Waitlist Management Modal Component
const WaitlistManagementModal: React.FC<{
    session: SessionCapacity;
    onClose: () => void;
    onPromote: (sessionId: string, registrationIds: string[]) => void;
}> = ({ session, onClose, onPromote }) => {
    const [waitlistEntries, setWaitlistEntries] = useState<any[]>([]);
    const [selectedEntries, setSelectedEntries] = useState<string[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadWaitlistEntries();
    }, []);

    const loadWaitlistEntries = async () => {
        try {
            // This would need to be implemented - getting waitlist entries for a session
            // const response = await apiClient.get(`/sessions/${session.sessionId}/waitlist`);
            // setWaitlistEntries(response.data);
            
            // Mock data for now
            setWaitlistEntries([
                { id: '1', userName: 'John Doe', userEmail: 'john@example.com', waitlistPosition: 1, registeredAt: '2024-01-15T10:00:00Z' },
                { id: '2', userName: 'Jane Smith', userEmail: 'jane@example.com', waitlistPosition: 2, registeredAt: '2024-01-15T11:00:00Z' }
            ]);
        } catch (error) {
            console.error('Failed to load waitlist:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleSelectAll = () => {
        if (selectedEntries.length === waitlistEntries.length) {
            setSelectedEntries([]);
        } else {
            setSelectedEntries(waitlistEntries.map(entry => entry.id));
        }
    };

    const handlePromoteSelected = () => {
        if (selectedEntries.length === 0) {
            alert('Please select attendees to promote');
            return;
        }

        if (selectedEntries.length > session.availableSpots) {
            const proceed = confirm(`You selected ${selectedEntries.length} attendees but only ${session.availableSpots} spots are available. Continue?`);
            if (!proceed) return;
        }

        onPromote(session.sessionId, selectedEntries);
    };

    if (loading) {
        return (
            <div className="modal fade show" style={{display: 'block', backgroundColor: 'rgba(0,0,0,0.5)'}} tabIndex={-1}>
                <div className="modal-dialog modal-lg">
                    <div className="modal-content">
                        <div className="modal-body text-center p-5">
                            <div className="spinner-border" role="status">
                                <span className="visually-hidden">Loading waitlist...</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="modal fade show" style={{display: 'block', backgroundColor: 'rgba(0,0,0,0.5)'}} tabIndex={-1}>
            <div className="modal-dialog modal-xl">
                <div className="modal-content">
                    <div className="modal-header">
                        <h5 className="modal-title">👥 Manage Waitlist - {session.sessionTitle}</h5>
                        <button type="button" className="btn-close" onClick={onClose}></button>
                    </div>
                    <div className="modal-body">
                        <div className="d-flex justify-content-between align-items-center mb-3">
                            <div>
                                <p className="mb-0">
                                    <strong>{session.currentWaitlistCount}</strong> people waitlisted • 
                                    <strong className="text-success ms-1">{session.availableSpots}</strong> spots available
                                </p>
                            </div>
                            <div>
                                <button className="btn btn-outline-primary btn-sm me-2" onClick={handleSelectAll}>
                                    {selectedEntries.length === waitlistEntries.length ? '☐ Deselect All' : '☑️ Select All'}
                                </button>
                                <button 
                                    className="btn btn-success btn-sm"
                                    onClick={handlePromoteSelected}
                                    disabled={selectedEntries.length === 0}
                                >
                                    🚀 Promote Selected ({selectedEntries.length})
                                </button>
                            </div>
                        </div>

                        {waitlistEntries.length === 0 ? (
                            <div className="text-center py-5 text-muted">
                                <span className="h1">👥</span>
                                <p>No one is currently on the waitlist</p>
                            </div>
                        ) : (
                            <div className="table-responsive">
                                <table className="table table-hover">
                                    <thead>
                                        <tr>
                                            <th style={{width: "50px"}}>
                                                <input 
                                                    type="checkbox" 
                                                    className="form-check-input"
                                                    checked={selectedEntries.length === waitlistEntries.length && waitlistEntries.length > 0}
                                                    onChange={handleSelectAll}
                                                />
                                            </th>
                                            <th>Position</th>
                                            <th>Name</th>
                                            <th>Email</th>
                                            <th>Waitlisted Since</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {waitlistEntries.map((entry, index) => (
                                            <tr key={entry.id} className={selectedEntries.includes(entry.id) ? 'table-primary' : ''}>
                                                <td>
                                                    <input 
                                                        type="checkbox" 
                                                        className="form-check-input"
                                                        checked={selectedEntries.includes(entry.id)}
                                                        onChange={(e) => {
                                                            if (e.target.checked) {
                                                                setSelectedEntries([...selectedEntries, entry.id]);
                                                            } else {
                                                                setSelectedEntries(selectedEntries.filter(id => id !== entry.id));
                                                            }
                                                        }}
                                                    />
                                                </td>
                                                <td>
                                                    <span className="badge bg-secondary">#{entry.waitlistPosition}</span>
                                                </td>
                                                <td><strong>{entry.userName}</strong></td>
                                                <td>{entry.userEmail}</td>
                                                <td>
                                                    <small className="text-muted">
                                                        {new Date(entry.registeredAt).toLocaleString()}
                                                    </small>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                    <div className="modal-footer">
                        <button type="button" className="btn btn-secondary" onClick={onClose}>
                            Close
                        </button>
                        <button 
                            type="button" 
                            className="btn btn-success"
                            onClick={handlePromoteSelected}
                            disabled={selectedEntries.length === 0}
                        >
                            🚀 Promote Selected ({selectedEntries.length})
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default CapacityManagementDashboard;