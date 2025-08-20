import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';

interface PrerequisiteManagementInterfaceProps {
    eventId: string;
}

interface SessionPrerequisite {
    id: string;
    sessionId: string;
    sessionTitle: string;
    type: 'SESSION_ATTENDANCE' | 'SESSION_REGISTRATION' | 'PROFILE_REQUIREMENT' | 'CUSTOM_RULE';
    targetSessionId?: string;
    targetSessionTitle?: string;
    targetUserId?: string;
    targetUserName?: string;
    requirementKey?: string;
    requirementValue?: string;
    customRule?: string;
    operator: 'AND' | 'OR' | 'NOT';
    isRequired: boolean;
    allowAdminOverride: boolean;
    validationMessage?: string;
    description?: string;
    createdAt: string;
    updatedAt?: string;
}

interface SessionDependency {
    id: string;
    sessionId: string;
    sessionTitle: string;
    dependsOnSessionId: string;
    dependsOnSessionTitle: string;
    dependencyType: 'PREREQUISITE' | 'COREQUISITE' | 'MUTUAL_EXCLUSION';
    isStrict: boolean;
    gracePeriodMinutes?: number;
    autoEnrollDependent: boolean;
    notificationMessage?: string;
    createdAt: string;
    updatedAt?: string;
}

interface Session {
    id: string;
    title: string;
    description?: string;
    startTime: string;
    endTime: string;
    capacity?: number;
    registrationCount?: number;
}

interface ValidationResult {
    isValid: boolean;
    violations: string[];
    warnings: string[];
    circularDependencies: string[];
}

const PrerequisiteManagementInterface: React.FC<PrerequisiteManagementInterfaceProps> = ({ eventId }) => {
    const [sessions, setSessions] = useState<Session[]>([]);
    const [prerequisites, setPrerequisites] = useState<SessionPrerequisite[]>([]);
    const [dependencies, setDependencies] = useState<SessionDependency[]>([]);
    const [selectedSession, setSelectedSession] = useState<Session | null>(null);
    const [showPrerequisiteModal, setShowPrerequisiteModal] = useState(false);
    const [showDependencyModal, setShowDependencyModal] = useState(false);
    const [validationResults, setValidationResults] = useState<ValidationResult | null>(null);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState<'prerequisites' | 'dependencies' | 'validation'>('prerequisites');

    useEffect(() => {
        loadData();
    }, [eventId]);

    const loadData = async () => {
        try {
            const [sessionsResponse, prerequisitesResponse, dependenciesResponse] = await Promise.all([
                apiClient.get(`/sessions/events/${eventId}`),
                apiClient.get(`/prerequisites/events/${eventId}`),
                apiClient.get(`/dependencies/events/${eventId}`)
            ]);

            setSessions(sessionsResponse.data);
            setPrerequisites(prerequisitesResponse.data);
            setDependencies(dependenciesResponse.data);
        } catch (error) {
            console.error('Failed to load prerequisite data:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleCreatePrerequisite = async (prerequisiteData: any) => {
        try {
            await apiClient.post('/prerequisites', {
                ...prerequisiteData,
                sessionId: selectedSession?.id
            });
            await loadData();
            setShowPrerequisiteModal(false);
        } catch (error) {
            console.error('Failed to create prerequisite:', error);
            alert('Failed to create prerequisite');
        }
    };

    const handleCreateDependency = async (dependencyData: any) => {
        try {
            await apiClient.post('/dependencies', {
                ...dependencyData,
                sessionId: selectedSession?.id
            });
            await loadData();
            setShowDependencyModal(false);
        } catch (error) {
            console.error('Failed to create dependency:', error);
            alert('Failed to create dependency');
        }
    };

    const handleDeletePrerequisite = async (prerequisiteId: string) => {
        if (!confirm('Are you sure you want to delete this prerequisite?')) return;
        
        try {
            await apiClient.delete(`/prerequisites/${prerequisiteId}`);
            await loadData();
        } catch (error) {
            console.error('Failed to delete prerequisite:', error);
            alert('Failed to delete prerequisite');
        }
    };

    const handleDeleteDependency = async (dependencyId: string) => {
        if (!confirm('Are you sure you want to delete this dependency?')) return;
        
        try {
            await apiClient.delete(`/dependencies/${dependencyId}`);
            await loadData();
        } catch (error) {
            console.error('Failed to delete dependency:', error);
            alert('Failed to delete dependency');
        }
    };

    const validateDependencies = async () => {
        try {
            const response = await apiClient.post(`/dependencies/events/${eventId}/validate`);
            setValidationResults(response.data);
            setActiveTab('validation');
        } catch (error) {
            console.error('Failed to validate dependencies:', error);
            alert('Failed to validate dependencies');
        }
    };

    const getTypeIcon = (type: string) => {
        switch (type) {
            case 'SESSION_ATTENDANCE': return '✅';
            case 'SESSION_REGISTRATION': return '📝';
            case 'PROFILE_REQUIREMENT': return '👤';
            case 'CUSTOM_RULE': return '⚙️';
            case 'PREREQUISITE': return '➡️';
            case 'COREQUISITE': return '🔗';
            case 'MUTUAL_EXCLUSION': return '🚫';
            default: return '📋';
        }
    };

    const getDependencyTypeColor = (type: string) => {
        switch (type) {
            case 'PREREQUISITE': return 'primary';
            case 'COREQUISITE': return 'success';
            case 'MUTUAL_EXCLUSION': return 'danger';
            default: return 'secondary';
        }
    };

    if (loading) {
        return (
            <div className="text-center p-5">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading prerequisite management...</span>
                </div>
                <p className="mt-3">Loading session dependencies and prerequisites...</p>
            </div>
        );
    }

    return (
        <div className="prerequisite-management-interface">
            {/* Header */}
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h3>🔗 Session Prerequisites & Dependencies</h3>
                    <p className="text-muted mb-0">
                        Manage session relationships, prerequisites, and attendance requirements
                    </p>
                </div>
                <div className="btn-group">
                    <button className="btn btn-outline-primary" onClick={loadData}>
                        🔄 Refresh
                    </button>
                    <button className="btn btn-success" onClick={validateDependencies}>
                        🔍 Validate Dependencies
                    </button>
                </div>
            </div>

            {/* Navigation Tabs */}
            <ul className="nav nav-tabs mb-4">
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'prerequisites' ? 'active' : ''}`}
                        onClick={() => setActiveTab('prerequisites')}
                    >
                        📋 Prerequisites ({prerequisites.length})
                    </button>
                </li>
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'dependencies' ? 'active' : ''}`}
                        onClick={() => setActiveTab('dependencies')}
                    >
                        🔗 Dependencies ({dependencies.length})
                    </button>
                </li>
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'validation' ? 'active' : ''}`}
                        onClick={() => setActiveTab('validation')}
                    >
                        ✅ Validation
                        {validationResults && !validationResults.isValid && (
                            <span className="badge bg-danger ms-1">Issues</span>
                        )}
                    </button>
                </li>
            </ul>

            {/* Tab Content */}
            {activeTab === 'prerequisites' && (
                <div className="tab-content">
                    <div className="card">
                        <div className="card-header">
                            <h5>📋 Session Prerequisites</h5>
                            <small className="text-muted">
                                Define requirements that attendees must meet before registering for specific sessions
                            </small>
                        </div>
                        <div className="card-body">
                            {prerequisites.length === 0 ? (
                                <div className="text-center py-5 text-muted">
                                    <span className="h1">📋</span>
                                    <p>No prerequisites configured yet</p>
                                    <p>Select a session below to add prerequisites</p>
                                </div>
                            ) : (
                                <div className="table-responsive">
                                    <table className="table table-hover">
                                        <thead>
                                            <tr>
                                                <th>Session</th>
                                                <th>Type</th>
                                                <th>Requirement</th>
                                                <th>Operator</th>
                                                <th>Required</th>
                                                <th>Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {prerequisites.map((prerequisite) => (
                                                <tr key={prerequisite.id}>
                                                    <td>
                                                        <strong>{prerequisite.sessionTitle}</strong>
                                                    </td>
                                                    <td>
                                                        <div className="d-flex align-items-center">
                                                            <span className="me-2">{getTypeIcon(prerequisite.type)}</span>
                                                            <small>{prerequisite.type.replace('_', ' ')}</small>
                                                        </div>
                                                    </td>
                                                    <td>
                                                        {prerequisite.targetSessionTitle && (
                                                            <div><strong>{prerequisite.targetSessionTitle}</strong></div>
                                                        )}
                                                        {prerequisite.targetUserName && (
                                                            <div><strong>{prerequisite.targetUserName}</strong></div>
                                                        )}
                                                        {prerequisite.requirementKey && (
                                                            <div><code>{prerequisite.requirementKey}: {prerequisite.requirementValue}</code></div>
                                                        )}
                                                        {prerequisite.customRule && (
                                                            <div><small className="text-info">{prerequisite.customRule}</small></div>
                                                        )}
                                                        {prerequisite.description && (
                                                            <div><small className="text-muted">{prerequisite.description}</small></div>
                                                        )}
                                                    </td>
                                                    <td>
                                                        <span className={`badge ${prerequisite.operator === 'AND' ? 'bg-success' : prerequisite.operator === 'OR' ? 'bg-warning' : 'bg-danger'}`}>
                                                            {prerequisite.operator}
                                                        </span>
                                                    </td>
                                                    <td>
                                                        <div>
                                                            {prerequisite.isRequired ? (
                                                                <span className="badge bg-danger">Required</span>
                                                            ) : (
                                                                <span className="badge bg-secondary">Optional</span>
                                                            )}
                                                        </div>
                                                        {prerequisite.allowAdminOverride && (
                                                            <div><small className="text-info">Admin Override</small></div>
                                                        )}
                                                    </td>
                                                    <td>
                                                        <button 
                                                            className="btn btn-sm btn-outline-danger"
                                                            onClick={() => handleDeletePrerequisite(prerequisite.id)}
                                                            title="Delete Prerequisite"
                                                        >
                                                            🗑️
                                                        </button>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Sessions List for Prerequisites */}
                    <div className="card mt-4">
                        <div className="card-header">
                            <h5>📅 Sessions - Add Prerequisites</h5>
                        </div>
                        <div className="card-body p-0">
                            <div className="table-responsive">
                                <table className="table table-hover mb-0">
                                    <thead>
                                        <tr>
                                            <th>Session</th>
                                            <th>Schedule</th>
                                            <th>Capacity</th>
                                            <th>Current Prerequisites</th>
                                            <th>Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {sessions.map((session) => {
                                            const sessionPrerequisites = prerequisites.filter(p => p.sessionId === session.id);
                                            return (
                                                <tr key={session.id}>
                                                    <td>
                                                        <strong>{session.title}</strong>
                                                        {session.description && (
                                                            <><br /><small className="text-muted">{session.description}</small></>
                                                        )}
                                                    </td>
                                                    <td>
                                                        <small>
                                                            {new Date(session.startTime).toLocaleString()}
                                                            <br />
                                                            to {new Date(session.endTime).toLocaleString()}
                                                        </small>
                                                    </td>
                                                    <td>
                                                        {session.capacity && (
                                                            <div>
                                                                <strong>{session.registrationCount || 0}</strong> / {session.capacity}
                                                            </div>
                                                        )}
                                                    </td>
                                                    <td>
                                                        <span className="badge bg-info">
                                                            {sessionPrerequisites.length} prerequisites
                                                        </span>
                                                    </td>
                                                    <td>
                                                        <button 
                                                            className="btn btn-sm btn-primary"
                                                            onClick={() => {
                                                                setSelectedSession(session);
                                                                setShowPrerequisiteModal(true);
                                                            }}
                                                        >
                                                            + Add Prerequisite
                                                        </button>
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {activeTab === 'dependencies' && (
                <div className="tab-content">
                    <div className="card">
                        <div className="card-header">
                            <h5>🔗 Session Dependencies</h5>
                            <small className="text-muted">
                                Define relationships between sessions (prerequisites, corequisites, mutual exclusions)
                            </small>
                        </div>
                        <div className="card-body">
                            {dependencies.length === 0 ? (
                                <div className="text-center py-5 text-muted">
                                    <span className="h1">🔗</span>
                                    <p>No dependencies configured yet</p>
                                    <p>Select a session below to add dependencies</p>
                                </div>
                            ) : (
                                <div className="table-responsive">
                                    <table className="table table-hover">
                                        <thead>
                                            <tr>
                                                <th>Session</th>
                                                <th>Relationship</th>
                                                <th>Depends On</th>
                                                <th>Type</th>
                                                <th>Settings</th>
                                                <th>Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {dependencies.map((dependency) => (
                                                <tr key={dependency.id}>
                                                    <td>
                                                        <strong>{dependency.sessionTitle}</strong>
                                                    </td>
                                                    <td>
                                                        <div className="d-flex align-items-center">
                                                            <span className="me-2">{getTypeIcon(dependency.dependencyType)}</span>
                                                            <span className={`badge bg-${getDependencyTypeColor(dependency.dependencyType)}`}>
                                                                {dependency.dependencyType}
                                                            </span>
                                                        </div>
                                                    </td>
                                                    <td>
                                                        <strong>{dependency.dependsOnSessionTitle}</strong>
                                                    </td>
                                                    <td>
                                                        {dependency.isStrict ? (
                                                            <span className="badge bg-danger">Strict</span>
                                                        ) : (
                                                            <span className="badge bg-warning">Flexible</span>
                                                        )}
                                                        {dependency.gracePeriodMinutes && (
                                                            <><br /><small className="text-muted">{dependency.gracePeriodMinutes}min grace</small></>
                                                        )}
                                                    </td>
                                                    <td>
                                                        {dependency.autoEnrollDependent && (
                                                            <div><small className="text-success">✅ Auto-enroll</small></div>
                                                        )}
                                                        {dependency.notificationMessage && (
                                                            <div><small className="text-info">📧 Custom notification</small></div>
                                                        )}
                                                    </td>
                                                    <td>
                                                        <button 
                                                            className="btn btn-sm btn-outline-danger"
                                                            onClick={() => handleDeleteDependency(dependency.id)}
                                                            title="Delete Dependency"
                                                        >
                                                            🗑️
                                                        </button>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Sessions List for Dependencies */}
                    <div className="card mt-4">
                        <div className="card-header">
                            <h5>📅 Sessions - Add Dependencies</h5>
                        </div>
                        <div className="card-body p-0">
                            <div className="table-responsive">
                                <table className="table table-hover mb-0">
                                    <thead>
                                        <tr>
                                            <th>Session</th>
                                            <th>Schedule</th>
                                            <th>Current Dependencies</th>
                                            <th>Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {sessions.map((session) => {
                                            const sessionDependencies = dependencies.filter(d => d.sessionId === session.id);
                                            return (
                                                <tr key={session.id}>
                                                    <td>
                                                        <strong>{session.title}</strong>
                                                        {session.description && (
                                                            <><br /><small className="text-muted">{session.description}</small></>
                                                        )}
                                                    </td>
                                                    <td>
                                                        <small>
                                                            {new Date(session.startTime).toLocaleString()}
                                                            <br />
                                                            to {new Date(session.endTime).toLocaleString()}
                                                        </small>
                                                    </td>
                                                    <td>
                                                        <span className="badge bg-info">
                                                            {sessionDependencies.length} dependencies
                                                        </span>
                                                    </td>
                                                    <td>
                                                        <button 
                                                            className="btn btn-sm btn-success"
                                                            onClick={() => {
                                                                setSelectedSession(session);
                                                                setShowDependencyModal(true);
                                                            }}
                                                        >
                                                            + Add Dependency
                                                        </button>
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {activeTab === 'validation' && (
                <div className="tab-content">
                    <div className="card">
                        <div className="card-header">
                            <h5>✅ Dependency Validation Results</h5>
                        </div>
                        <div className="card-body">
                            {!validationResults ? (
                                <div className="text-center py-5 text-muted">
                                    <span className="h1">🔍</span>
                                    <p>Click "Validate Dependencies" to check for issues</p>
                                </div>
                            ) : (
                                <div>
                                    <div className={`alert ${validationResults.isValid ? 'alert-success' : 'alert-danger'}`}>
                                        <h5>
                                            {validationResults.isValid ? '✅ Validation Passed' : '❌ Validation Failed'}
                                        </h5>
                                        <p>
                                            {validationResults.isValid 
                                                ? 'All session dependencies are valid with no circular dependencies or conflicts.'
                                                : 'Issues detected in session dependencies. Please review and resolve the problems below.'
                                            }
                                        </p>
                                    </div>

                                    {validationResults.violations.length > 0 && (
                                        <div className="card mb-3">
                                            <div className="card-header bg-danger text-white">
                                                <h6>🚨 Critical Violations ({validationResults.violations.length})</h6>
                                            </div>
                                            <div className="card-body">
                                                <ul className="list-unstyled">
                                                    {validationResults.violations.map((violation, index) => (
                                                        <li key={index} className="mb-2">
                                                            <span className="badge bg-danger me-2">ERROR</span>
                                                            {violation}
                                                        </li>
                                                    ))}
                                                </ul>
                                            </div>
                                        </div>
                                    )}

                                    {validationResults.warnings.length > 0 && (
                                        <div className="card mb-3">
                                            <div className="card-header bg-warning text-dark">
                                                <h6>⚠️ Warnings ({validationResults.warnings.length})</h6>
                                            </div>
                                            <div className="card-body">
                                                <ul className="list-unstyled">
                                                    {validationResults.warnings.map((warning, index) => (
                                                        <li key={index} className="mb-2">
                                                            <span className="badge bg-warning me-2">WARNING</span>
                                                            {warning}
                                                        </li>
                                                    ))}
                                                </ul>
                                            </div>
                                        </div>
                                    )}

                                    {validationResults.circularDependencies.length > 0 && (
                                        <div className="card">
                                            <div className="card-header bg-dark text-white">
                                                <h6>🔄 Circular Dependencies ({validationResults.circularDependencies.length})</h6>
                                            </div>
                                            <div className="card-body">
                                                <ul className="list-unstyled">
                                                    {validationResults.circularDependencies.map((cycle, index) => (
                                                        <li key={index} className="mb-2">
                                                            <span className="badge bg-dark me-2">CYCLE</span>
                                                            {cycle}
                                                        </li>
                                                    ))}
                                                </ul>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* Prerequisite Modal */}
            {showPrerequisiteModal && selectedSession && (
                <PrerequisiteModal
                    session={selectedSession}
                    sessions={sessions}
                    onClose={() => setShowPrerequisiteModal(false)}
                    onCreate={handleCreatePrerequisite}
                />
            )}

            {/* Dependency Modal */}
            {showDependencyModal && selectedSession && (
                <DependencyModal
                    session={selectedSession}
                    sessions={sessions}
                    onClose={() => setShowDependencyModal(false)}
                    onCreate={handleCreateDependency}
                />
            )}
        </div>
    );
};

// Prerequisite Modal Component
const PrerequisiteModal: React.FC<{
    session: Session;
    sessions: Session[];
    onClose: () => void;
    onCreate: (data: any) => void;
}> = ({ session, sessions, onClose, onCreate }) => {
    const [formData, setFormData] = useState({
        type: 'SESSION_ATTENDANCE',
        targetSessionId: '',
        targetUserId: '',
        requirementKey: '',
        requirementValue: '',
        customRule: '',
        operator: 'AND',
        isRequired: true,
        allowAdminOverride: false,
        validationMessage: '',
        description: ''
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onCreate(formData);
    };

    return (
        <div className="modal fade show" style={{display: 'block', backgroundColor: 'rgba(0,0,0,0.5)'}} tabIndex={-1}>
            <div className="modal-dialog modal-lg">
                <div className="modal-content">
                    <div className="modal-header">
                        <h5 className="modal-title">📋 Add Prerequisite - {session.title}</h5>
                        <button type="button" className="btn-close" onClick={onClose}></button>
                    </div>
                    <form onSubmit={handleSubmit}>
                        <div className="modal-body">
                            <div className="row">
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Prerequisite Type *</label>
                                    <select
                                        className="form-select"
                                        value={formData.type}
                                        onChange={(e) => setFormData({...formData, type: e.target.value as any})}
                                        required
                                    >
                                        <option value="SESSION_ATTENDANCE">Session Attendance Required</option>
                                        <option value="SESSION_REGISTRATION">Session Registration Required</option>
                                        <option value="PROFILE_REQUIREMENT">Profile Requirement</option>
                                        <option value="CUSTOM_RULE">Custom Rule</option>
                                    </select>
                                </div>
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Logical Operator</label>
                                    <select
                                        className="form-select"
                                        value={formData.operator}
                                        onChange={(e) => setFormData({...formData, operator: e.target.value as any})}
                                    >
                                        <option value="AND">AND (all conditions)</option>
                                        <option value="OR">OR (any condition)</option>
                                        <option value="NOT">NOT (exclude condition)</option>
                                    </select>
                                </div>
                            </div>

                            {(formData.type === 'SESSION_ATTENDANCE' || formData.type === 'SESSION_REGISTRATION') && (
                                <div className="mb-3">
                                    <label className="form-label">Target Session *</label>
                                    <select
                                        className="form-select"
                                        value={formData.targetSessionId}
                                        onChange={(e) => setFormData({...formData, targetSessionId: e.target.value})}
                                        required
                                    >
                                        <option value="">Select target session...</option>
                                        {sessions.filter(s => s.id !== session.id).map(s => (
                                            <option key={s.id} value={s.id}>{s.title}</option>
                                        ))}
                                    </select>
                                </div>
                            )}

                            {formData.type === 'PROFILE_REQUIREMENT' && (
                                <div className="row">
                                    <div className="col-md-6 mb-3">
                                        <label className="form-label">Profile Field *</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            value={formData.requirementKey}
                                            onChange={(e) => setFormData({...formData, requirementKey: e.target.value})}
                                            placeholder="e.g., department, role, certification"
                                            required
                                        />
                                    </div>
                                    <div className="col-md-6 mb-3">
                                        <label className="form-label">Required Value *</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            value={formData.requirementValue}
                                            onChange={(e) => setFormData({...formData, requirementValue: e.target.value})}
                                            placeholder="Required field value"
                                            required
                                        />
                                    </div>
                                </div>
                            )}

                            {formData.type === 'CUSTOM_RULE' && (
                                <div className="mb-3">
                                    <label className="form-label">Custom Rule *</label>
                                    <textarea
                                        className="form-control"
                                        rows={3}
                                        value={formData.customRule}
                                        onChange={(e) => setFormData({...formData, customRule: e.target.value})}
                                        placeholder="Define custom prerequisite logic..."
                                        required
                                    />
                                </div>
                            )}

                            <div className="mb-3">
                                <label className="form-label">Description</label>
                                <textarea
                                    className="form-control"
                                    rows={2}
                                    value={formData.description}
                                    onChange={(e) => setFormData({...formData, description: e.target.value})}
                                    placeholder="Explain this prerequisite for attendees..."
                                />
                            </div>

                            <div className="mb-3">
                                <label className="form-label">Validation Message</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    value={formData.validationMessage}
                                    onChange={(e) => setFormData({...formData, validationMessage: e.target.value})}
                                    placeholder="Message shown when prerequisite is not met"
                                />
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
                                            Required Prerequisite
                                        </label>
                                    </div>
                                </div>
                                <div className="col-md-6 mb-3">
                                    <div className="form-check">
                                        <input
                                            className="form-check-input"
                                            type="checkbox"
                                            checked={formData.allowAdminOverride}
                                            onChange={(e) => setFormData({...formData, allowAdminOverride: e.target.checked})}
                                        />
                                        <label className="form-check-label">
                                            Allow Admin Override
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
                                Create Prerequisite
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

// Dependency Modal Component
const DependencyModal: React.FC<{
    session: Session;
    sessions: Session[];
    onClose: () => void;
    onCreate: (data: any) => void;
}> = ({ session, sessions, onClose, onCreate }) => {
    const [formData, setFormData] = useState({
        dependsOnSessionId: '',
        dependencyType: 'PREREQUISITE',
        isStrict: false,
        gracePeriodMinutes: 0,
        autoEnrollDependent: false,
        notificationMessage: ''
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onCreate(formData);
    };

    return (
        <div className="modal fade show" style={{display: 'block', backgroundColor: 'rgba(0,0,0,0.5)'}} tabIndex={-1}>
            <div className="modal-dialog modal-lg">
                <div className="modal-content">
                    <div className="modal-header">
                        <h5 className="modal-title">🔗 Add Dependency - {session.title}</h5>
                        <button type="button" className="btn-close" onClick={onClose}></button>
                    </div>
                    <form onSubmit={handleSubmit}>
                        <div className="modal-body">
                            <div className="row">
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Depends On Session *</label>
                                    <select
                                        className="form-select"
                                        value={formData.dependsOnSessionId}
                                        onChange={(e) => setFormData({...formData, dependsOnSessionId: e.target.value})}
                                        required
                                    >
                                        <option value="">Select dependency session...</option>
                                        {sessions.filter(s => s.id !== session.id).map(s => (
                                            <option key={s.id} value={s.id}>{s.title}</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Dependency Type *</label>
                                    <select
                                        className="form-select"
                                        value={formData.dependencyType}
                                        onChange={(e) => setFormData({...formData, dependencyType: e.target.value as any})}
                                    >
                                        <option value="PREREQUISITE">Prerequisite (must complete before)</option>
                                        <option value="COREQUISITE">Corequisite (must take together)</option>
                                        <option value="MUTUAL_EXCLUSION">Mutual Exclusion (cannot take both)</option>
                                    </select>
                                </div>
                            </div>

                            <div className="row">
                                <div className="col-md-6 mb-3">
                                    <div className="form-check">
                                        <input
                                            className="form-check-input"
                                            type="checkbox"
                                            checked={formData.isStrict}
                                            onChange={(e) => setFormData({...formData, isStrict: e.target.checked})}
                                        />
                                        <label className="form-check-label">
                                            Strict Dependency
                                        </label>
                                        <div className="form-text">
                                            If enabled, dependency must be satisfied exactly. Otherwise, some flexibility is allowed.
                                        </div>
                                    </div>
                                </div>
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Grace Period (minutes)</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.gracePeriodMinutes}
                                        onChange={(e) => setFormData({...formData, gracePeriodMinutes: parseInt(e.target.value) || 0})}
                                        min="0"
                                        max="1440"
                                    />
                                    <div className="form-text">
                                        Allow registration within this grace period
                                    </div>
                                </div>
                            </div>

                            <div className="mb-3">
                                <div className="form-check">
                                    <input
                                        className="form-check-input"
                                        type="checkbox"
                                        checked={formData.autoEnrollDependent}
                                        onChange={(e) => setFormData({...formData, autoEnrollDependent: e.target.checked})}
                                    />
                                    <label className="form-check-label">
                                        Auto-enroll in dependent sessions
                                    </label>
                                    <div className="form-text">
                                        Automatically enroll attendees in dependent sessions when they register for prerequisite
                                    </div>
                                </div>
                            </div>

                            <div className="mb-3">
                                <label className="form-label">Notification Message</label>
                                <textarea
                                    className="form-control"
                                    rows={3}
                                    value={formData.notificationMessage}
                                    onChange={(e) => setFormData({...formData, notificationMessage: e.target.value})}
                                    placeholder="Custom message to show attendees about this dependency..."
                                />
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button type="button" className="btn btn-secondary" onClick={onClose}>
                                Cancel
                            </button>
                            <button type="submit" className="btn btn-success">
                                Create Dependency
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default PrerequisiteManagementInterface;