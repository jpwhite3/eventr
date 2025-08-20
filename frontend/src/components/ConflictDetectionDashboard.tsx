import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';

interface ConflictDetectionDashboardProps {
    eventId: string;
}

interface ScheduleConflict {
    id: string;
    type: 'TIME_OVERLAP' | 'RESOURCE_CONFLICT' | 'CAPACITY_EXCEEDED' | 'USER_CONFLICT';
    severity: 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL';
    title: string;
    description: string;
    primarySessionId?: string;
    primarySessionTitle?: string;
    secondarySessionId?: string;
    secondarySessionTitle?: string;
    resourceId?: string;
    resourceName?: string;
    registrationId?: string;
    userEmail?: string;
    conflictStart?: string;
    conflictEnd?: string;
    affectedCount: number;
    duration?: string;
    resolutionStatus: 'UNRESOLVED' | 'ACKNOWLEDGED' | 'IN_PROGRESS' | 'RESOLVED';
    resolutionNotes?: string;
    resolvedBy?: string;
    resolvedAt?: string;
    canAutoResolve: boolean;
    autoResolutionStrategy?: string;
    detectedAt: string;
    lastCheckedAt?: string;
    ageInHours: number;
    notificationsSent: number;
    isActive: boolean;
    suggestedResolutions?: string[];
}

interface ConflictSummary {
    eventId: string;
    totalConflicts: number;
    unresolvedConflicts: number;
    criticalConflicts: number;
    autoResolvableConflicts: number;
    conflictsByType: Record<string, number>;
    conflictsBySeverity: Record<string, number>;
    oldestUnresolvedConflict?: string;
    averageResolutionTimeHours: number;
}

interface ConflictAnalytics {
    eventId: string;
    totalSessions: number;
    totalResources: number;
    totalRegistrations: number;
    conflictRate: number;
    resolutionRate: number;
    mostCommonConflictType?: string;
    mostConflictedResources: ResourceConflictSummary[];
    sessionsWithMostConflicts: SessionConflictSummary[];
    preventionRecommendations: string[];
    schedulingOptimizationSuggestions: string[];
}

interface ResourceConflictSummary {
    resourceId: string;
    resourceName: string;
    resourceType: string;
    conflictCount: number;
    utilizationRate: number;
    averageConflictDuration: number;
}

interface SessionConflictSummary {
    sessionId: string;
    sessionTitle: string;
    conflictCount: number;
    conflictTypes: string[];
    registrationCount: number;
}

const ConflictDetectionDashboard: React.FC<ConflictDetectionDashboardProps> = ({ eventId }) => {
    const [conflicts, setConflicts] = useState<ScheduleConflict[]>([]);
    const [summary, setSummary] = useState<ConflictSummary | null>(null);
    const [analytics, setAnalytics] = useState<ConflictAnalytics | null>(null);
    const [selectedConflict, setSelectedConflict] = useState<ScheduleConflict | null>(null);
    const [showResolutionModal, setShowResolutionModal] = useState(false);
    const [loading, setLoading] = useState(true);
    const [autoRefresh, setAutoRefresh] = useState(true);
    const [filterType, setFilterType] = useState<string>('ALL');
    const [filterSeverity, setFilterSeverity] = useState<string>('ALL');

    useEffect(() => {
        loadConflictData();
    }, [eventId]);

    useEffect(() => {
        if (!autoRefresh) return;
        
        const interval = setInterval(() => {
            loadConflictData();
        }, 45000); // Refresh every 45 seconds

        return () => clearInterval(interval);
    }, [eventId, autoRefresh]);

    const loadConflictData = async () => {
        try {
            const [conflictsResponse, summaryResponse, analyticsResponse] = await Promise.all([
                apiClient.get(`/conflicts/events/${eventId}`),
                apiClient.get(`/conflicts/events/${eventId}/summary`),
                apiClient.get(`/conflicts/events/${eventId}/analytics`)
            ]);

            setConflicts(conflictsResponse.data);
            setSummary(summaryResponse.data);
            setAnalytics(analyticsResponse.data);
        } catch (error) {
            console.error('Failed to load conflict data:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleDetectConflicts = async () => {
        try {
            setLoading(true);
            await apiClient.post(`/conflicts/events/${eventId}/detect`);
            await loadConflictData();
        } catch (error) {
            console.error('Failed to detect conflicts:', error);
            alert('Failed to detect conflicts');
        }
    };

    const handleAutoResolve = async () => {
        try {
            const response = await apiClient.post('/conflicts/auto-resolve');
            alert(`Auto-resolved ${response.data.length} conflicts`);
            await loadConflictData();
        } catch (error) {
            console.error('Auto-resolve failed:', error);
            alert('Auto-resolve failed');
        }
    };

    const handleResolveConflict = async (conflictId: string, resolutionData: any) => {
        try {
            await apiClient.post(`/conflicts/${conflictId}/resolve`, resolutionData);
            await loadConflictData();
            setShowResolutionModal(false);
            alert('Conflict resolved successfully');
        } catch (error) {
            console.error('Failed to resolve conflict:', error);
            alert('Failed to resolve conflict');
        }
    };

    const getSeverityColor = (severity: string) => {
        switch (severity) {
            case 'CRITICAL': return 'danger';
            case 'ERROR': return 'danger';
            case 'WARNING': return 'warning';
            case 'INFO': return 'info';
            default: return 'secondary';
        }
    };

    const getTypeIcon = (type: string) => {
        switch (type) {
            case 'TIME_OVERLAP': return '⏰';
            case 'RESOURCE_CONFLICT': return '🏢';
            case 'CAPACITY_EXCEEDED': return '👥';
            case 'USER_CONFLICT': return '👤';
            default: return '⚠️';
        }
    };

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'RESOLVED': return 'success';
            case 'IN_PROGRESS': return 'primary';
            case 'ACKNOWLEDGED': return 'info';
            case 'UNRESOLVED': return 'danger';
            default: return 'secondary';
        }
    };

    const filteredConflicts = conflicts.filter(conflict => {
        if (filterType !== 'ALL' && conflict.type !== filterType) return false;
        if (filterSeverity !== 'ALL' && conflict.severity !== filterSeverity) return false;
        return true;
    });

    if (loading) {
        return (
            <div className="text-center p-5">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading conflict detection...</span>
                </div>
                <p className="mt-3">Analyzing conflicts and dependencies...</p>
            </div>
        );
    }

    if (!summary) {
        return (
            <div className="alert alert-warning">
                <h5>⚠️ No Conflict Data Available</h5>
                <p>Unable to load conflict detection data for this event.</p>
                <button className="btn btn-primary" onClick={loadConflictData}>
                    🔄 Retry
                </button>
            </div>
        );
    }

    return (
        <div className="conflict-detection-dashboard">
            {/* Header Controls */}
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h3>🚨 Conflict Detection & Resolution</h3>
                    <p className="text-muted mb-0">
                        Real-time scheduling conflict monitoring and automated resolution
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
                    <button className="btn btn-sm btn-outline-primary" onClick={loadConflictData}>
                        🔄 Refresh
                    </button>
                    <button className="btn btn-sm btn-primary" onClick={handleDetectConflicts}>
                        🔍 Detect Conflicts
                    </button>
                    <button className="btn btn-sm btn-success" onClick={handleAutoResolve}>
                        🤖 Auto-Resolve
                    </button>
                </div>
            </div>

            {/* Summary Cards */}
            <div className="row mb-4">
                <div className="col-lg-3 col-md-6 mb-3">
                    <div className="card bg-primary text-white">
                        <div className="card-body">
                            <div className="d-flex justify-content-between">
                                <div>
                                    <div className="h4 mb-0">{summary.totalConflicts}</div>
                                    <p className="card-text">Total Conflicts</p>
                                </div>
                                <div className="align-self-center">
                                    <span className="h1">🚨</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="col-lg-3 col-md-6 mb-3">
                    <div className={`card ${summary.unresolvedConflicts > 0 ? 'bg-danger' : 'bg-success'} text-white`}>
                        <div className="card-body">
                            <div className="d-flex justify-content-between">
                                <div>
                                    <div className="h4 mb-0">{summary.unresolvedConflicts}</div>
                                    <p className="card-text">Unresolved</p>
                                </div>
                                <div className="align-self-center">
                                    <span className="h1">{summary.unresolvedConflicts > 0 ? '⚠️' : '✅'}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="col-lg-3 col-md-6 mb-3">
                    <div className={`card ${summary.criticalConflicts > 0 ? 'bg-danger' : 'bg-secondary'} text-white`}>
                        <div className="card-body">
                            <div className="d-flex justify-content-between">
                                <div>
                                    <div className="h4 mb-0">{summary.criticalConflicts}</div>
                                    <p className="card-text">Critical</p>
                                </div>
                                <div className="align-self-center">
                                    <span className="h1">🔥</span>
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
                                    <div className="h4 mb-0">{summary.autoResolvableConflicts}</div>
                                    <p className="card-text">Auto-Resolvable</p>
                                </div>
                                <div className="align-self-center">
                                    <span className="h1">🤖</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Analytics & Recommendations */}
            {analytics && (
                <div className="row mb-4">
                    <div className="col-md-8">
                        <div className="card">
                            <div className="card-header">
                                <h5>📊 Conflict Analytics</h5>
                            </div>
                            <div className="card-body">
                                <div className="row">
                                    <div className="col-md-6">
                                        <p><strong>Conflict Rate:</strong> {(analytics.conflictRate * 100).toFixed(1)}% of sessions</p>
                                        <p><strong>Resolution Rate:</strong> {analytics.resolutionRate.toFixed(1)}%</p>
                                        <p><strong>Most Common Type:</strong> {analytics.mostCommonConflictType || 'None'}</p>
                                        <p><strong>Avg Resolution Time:</strong> {summary.averageResolutionTimeHours.toFixed(1)} hours</p>
                                    </div>
                                    <div className="col-md-6">
                                        <p><strong>Total Sessions:</strong> {analytics.totalSessions}</p>
                                        <p><strong>Total Resources:</strong> {analytics.totalResources}</p>
                                        <p><strong>Total Registrations:</strong> {analytics.totalRegistrations}</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="col-md-4">
                        <div className="card">
                            <div className="card-header">
                                <h5>🎯 Prevention Tips</h5>
                            </div>
                            <div className="card-body">
                                {analytics.preventionRecommendations.length > 0 ? (
                                    <ul className="list-unstyled">
                                        {analytics.preventionRecommendations.map((tip, index) => (
                                            <li key={index} className="mb-2">
                                                <small>• {tip}</small>
                                            </li>
                                        ))}
                                    </ul>
                                ) : (
                                    <p className="text-muted">No specific recommendations at this time.</p>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Filters */}
            <div className="card mb-4">
                <div className="card-body">
                    <div className="row">
                        <div className="col-md-6">
                            <label className="form-label">Filter by Type:</label>
                            <select 
                                className="form-select"
                                value={filterType}
                                onChange={(e) => setFilterType(e.target.value)}
                            >
                                <option value="ALL">All Types</option>
                                <option value="TIME_OVERLAP">Time Overlaps</option>
                                <option value="RESOURCE_CONFLICT">Resource Conflicts</option>
                                <option value="CAPACITY_EXCEEDED">Capacity Issues</option>
                                <option value="USER_CONFLICT">User Conflicts</option>
                            </select>
                        </div>
                        <div className="col-md-6">
                            <label className="form-label">Filter by Severity:</label>
                            <select 
                                className="form-select"
                                value={filterSeverity}
                                onChange={(e) => setFilterSeverity(e.target.value)}
                            >
                                <option value="ALL">All Severities</option>
                                <option value="CRITICAL">Critical</option>
                                <option value="ERROR">Error</option>
                                <option value="WARNING">Warning</option>
                                <option value="INFO">Info</option>
                            </select>
                        </div>
                    </div>
                </div>
            </div>

            {/* Conflicts List */}
            <div className="card">
                <div className="card-header">
                    <h5>📋 Detected Conflicts ({filteredConflicts.length})</h5>
                </div>
                <div className="card-body p-0">
                    {filteredConflicts.length === 0 ? (
                        <div className="text-center py-5 text-muted">
                            <span className="h1">✅</span>
                            <p>No conflicts detected matching current filters!</p>
                        </div>
                    ) : (
                        <div className="table-responsive">
                            <table className="table table-hover mb-0">
                                <thead className="table-light">
                                    <tr>
                                        <th>Type</th>
                                        <th>Severity</th>
                                        <th>Description</th>
                                        <th>Affected</th>
                                        <th>Age</th>
                                        <th>Status</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {filteredConflicts.map((conflict) => (
                                        <tr key={conflict.id}>
                                            <td>
                                                <div className="d-flex align-items-center">
                                                    <span className="me-2">{getTypeIcon(conflict.type)}</span>
                                                    <small>{conflict.type.replace('_', ' ')}</small>
                                                </div>
                                            </td>
                                            <td>
                                                <span className={`badge bg-${getSeverityColor(conflict.severity)}`}>
                                                    {conflict.severity}
                                                </span>
                                            </td>
                                            <td>
                                                <div>
                                                    <strong>{conflict.title}</strong>
                                                    <br />
                                                    <small className="text-muted">{conflict.description}</small>
                                                    {conflict.duration && (
                                                        <><br /><small className="text-info">Duration: {conflict.duration}</small></>
                                                    )}
                                                </div>
                                            </td>
                                            <td>
                                                <strong>{conflict.affectedCount}</strong>
                                                {conflict.userEmail && (
                                                    <><br /><small>{conflict.userEmail}</small></>
                                                )}
                                            </td>
                                            <td>
                                                <small>{conflict.ageInHours}h ago</small>
                                                {conflict.notificationsSent > 0 && (
                                                    <><br /><small className="text-muted">{conflict.notificationsSent} notifications</small></>
                                                )}
                                            </td>
                                            <td>
                                                <span className={`badge bg-${getStatusColor(conflict.resolutionStatus)}`}>
                                                    {conflict.resolutionStatus.replace('_', ' ')}
                                                </span>
                                                {conflict.resolvedBy && (
                                                    <><br /><small className="text-muted">by {conflict.resolvedBy}</small></>
                                                )}
                                            </td>
                                            <td>
                                                <div className="btn-group btn-group-sm">
                                                    <button 
                                                        className="btn btn-outline-primary"
                                                        onClick={() => {
                                                            setSelectedConflict(conflict);
                                                            setShowResolutionModal(true);
                                                        }}
                                                        title="Resolve Conflict"
                                                        disabled={conflict.resolutionStatus === 'RESOLVED'}
                                                    >
                                                        {conflict.resolutionStatus === 'RESOLVED' ? '✅' : '🔧'}
                                                    </button>
                                                    {conflict.canAutoResolve && conflict.resolutionStatus === 'UNRESOLVED' && (
                                                        <button 
                                                            className="btn btn-outline-success"
                                                            onClick={async () => {
                                                                try {
                                                                    await apiClient.post(`/conflicts/${conflict.id}/auto-resolve`);
                                                                    await loadConflictData();
                                                                } catch (error) {
                                                                    console.error('Auto-resolve failed:', error);
                                                                    alert('Auto-resolve failed');
                                                                }
                                                            }}
                                                            title="Auto-Resolve"
                                                        >
                                                            🤖
                                                        </button>
                                                    )}
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </div>

            {/* Resolution Modal */}
            {showResolutionModal && selectedConflict && (
                <ConflictResolutionModal
                    conflict={selectedConflict}
                    onClose={() => setShowResolutionModal(false)}
                    onResolve={handleResolveConflict}
                />
            )}
        </div>
    );
};

// Conflict Resolution Modal Component
const ConflictResolutionModal: React.FC<{
    conflict: ScheduleConflict;
    onClose: () => void;
    onResolve: (conflictId: string, resolutionData: any) => void;
}> = ({ conflict, onClose, onResolve }) => {
    const [formData, setFormData] = useState({
        resolutionType: '',
        description: '',
        changesSummary: '',
        implementedBy: '',
        affectedSessions: 0,
        affectedRegistrations: 0,
        affectedResources: 0,
        notes: ''
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onResolve(conflict.id, formData);
    };

    return (
        <div className="modal fade show" style={{display: 'block', backgroundColor: 'rgba(0,0,0,0.5)'}} tabIndex={-1}>
            <div className="modal-dialog modal-lg">
                <div className="modal-content">
                    <div className="modal-header">
                        <h5 className="modal-title">🔧 Resolve Conflict - {conflict.title}</h5>
                        <button type="button" className="btn-close" onClick={onClose}></button>
                    </div>
                    <form onSubmit={handleSubmit}>
                        <div className="modal-body">
                            <div className="alert alert-info">
                                <strong>Conflict Details:</strong><br />
                                {conflict.description}
                                {conflict.autoResolutionStrategy && (
                                    <><br /><strong>Suggested Auto-Resolution:</strong> {conflict.autoResolutionStrategy}</>
                                )}
                            </div>

                            {conflict.suggestedResolutions && conflict.suggestedResolutions.length > 0 && (
                                <div className="mb-3">
                                    <label className="form-label">💡 Suggested Resolutions:</label>
                                    <div className="list-group">
                                        {conflict.suggestedResolutions.map((suggestion, index) => (
                                            <div key={index} className="list-group-item">
                                                <small>• {suggestion}</small>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}

                            <div className="row">
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Resolution Type *</label>
                                    <select
                                        className="form-select"
                                        value={formData.resolutionType}
                                        onChange={(e) => setFormData({...formData, resolutionType: e.target.value})}
                                        required
                                    >
                                        <option value="">Select resolution type...</option>
                                        <option value="MANUAL_ADJUSTMENT">Manual Adjustment</option>
                                        <option value="SCHEDULE_CHANGE">Schedule Change</option>
                                        <option value="RESOURCE_REALLOCATION">Resource Reallocation</option>
                                        <option value="CAPACITY_INCREASE">Capacity Increase</option>
                                        <option value="USER_NOTIFICATION">User Notification</option>
                                        <option value="POSTPONE_SESSION">Postpone Session</option>
                                        <option value="CANCEL_SESSION">Cancel Session</option>
                                        <option value="OTHER">Other</option>
                                    </select>
                                </div>
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Implemented By *</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={formData.implementedBy}
                                        onChange={(e) => setFormData({...formData, implementedBy: e.target.value})}
                                        required
                                        placeholder="Your name or ID"
                                    />
                                </div>
                            </div>

                            <div className="mb-3">
                                <label className="form-label">Resolution Description *</label>
                                <textarea
                                    className="form-control"
                                    rows={3}
                                    value={formData.description}
                                    onChange={(e) => setFormData({...formData, description: e.target.value})}
                                    required
                                    placeholder="Describe how this conflict was resolved..."
                                />
                            </div>

                            <div className="mb-3">
                                <label className="form-label">Changes Summary</label>
                                <textarea
                                    className="form-control"
                                    rows={2}
                                    value={formData.changesSummary}
                                    onChange={(e) => setFormData({...formData, changesSummary: e.target.value})}
                                    placeholder="Summarize what was changed..."
                                />
                            </div>

                            <div className="row">
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Affected Sessions</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.affectedSessions}
                                        onChange={(e) => setFormData({...formData, affectedSessions: parseInt(e.target.value) || 0})}
                                        min="0"
                                    />
                                </div>
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Affected Registrations</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.affectedRegistrations}
                                        onChange={(e) => setFormData({...formData, affectedRegistrations: parseInt(e.target.value) || 0})}
                                        min="0"
                                    />
                                </div>
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Affected Resources</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.affectedResources}
                                        onChange={(e) => setFormData({...formData, affectedResources: parseInt(e.target.value) || 0})}
                                        min="0"
                                    />
                                </div>
                            </div>

                            <div className="mb-3">
                                <label className="form-label">Additional Notes</label>
                                <textarea
                                    className="form-control"
                                    rows={2}
                                    value={formData.notes}
                                    onChange={(e) => setFormData({...formData, notes: e.target.value})}
                                    placeholder="Any additional notes or considerations..."
                                />
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button type="button" className="btn btn-secondary" onClick={onClose}>
                                Cancel
                            </button>
                            <button type="submit" className="btn btn-success">
                                🔧 Resolve Conflict
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default ConflictDetectionDashboard;