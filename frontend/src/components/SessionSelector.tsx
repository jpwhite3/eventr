import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';

interface Session {
    id: string;
    title: string;
    description?: string;
    type: string;
    startTime: string;
    endTime: string;
    location?: string;
    room?: string;
    building?: string;
    capacity?: number;
    presenter?: string;
    presenterTitle?: string;
    prerequisites?: string;
    targetAudience?: string;
    difficultyLevel?: string;
    tags?: string[];
    registeredCount: number;
    waitlistCount: number;
    availableSpots: number;
    isRegistrationRequired: boolean;
    isWaitlistEnabled: boolean;
}

interface SessionSelectorProps {
    eventId: string;
    registrationId?: string;
    selectedSessionIds: string[];
    onSelectionChange: (sessionIds: string[]) => void;
    readonly?: boolean;
}

const SessionSelector: React.FC<SessionSelectorProps> = ({ 
    eventId, 
    registrationId,
    selectedSessionIds, 
    onSelectionChange,
    readonly = false
}) => {
    const [sessions, setSessions] = useState<Session[]>([]);
    const [conflicts, setConflicts] = useState<string[]>([]);
    const [loading, setLoading] = useState(true);
    const [viewMode, setViewMode] = useState<'grid' | 'timeline'>('grid');

    useEffect(() => {
        fetchSessions();
    }, [eventId]);

    useEffect(() => {
        checkConflicts();
    }, [selectedSessionIds, sessions]);

    const fetchSessions = async () => {
        try {
            const response = await apiClient.get(`/sessions/event/${eventId}`);
            setSessions(response.data.filter((session: Session) => session.isRegistrationRequired));
        } catch (error) {
            console.error('Failed to fetch sessions:', error);
        } finally {
            setLoading(false);
        }
    };

    const checkConflicts = () => {
        const conflicts: string[] = [];
        const selectedSessions = sessions.filter(s => selectedSessionIds.includes(s.id));
        
        for (let i = 0; i < selectedSessions.length; i++) {
            for (let j = i + 1; j < selectedSessions.length; j++) {
                const session1 = selectedSessions[i];
                const session2 = selectedSessions[j];
                
                const start1 = new Date(session1.startTime);
                const end1 = new Date(session1.endTime);
                const start2 = new Date(session2.startTime);
                const end2 = new Date(session2.endTime);
                
                if ((start1 < end2 && end1 > start2)) {
                    conflicts.push(`"${session1.title}" conflicts with "${session2.title}"`);
                }
            }
        }
        
        setConflicts(conflicts);
    };

    const handleSessionToggle = (sessionId: string) => {
        if (readonly) return;
        
        const newSelection = selectedSessionIds.includes(sessionId)
            ? selectedSessionIds.filter(id => id !== sessionId)
            : [...selectedSessionIds, sessionId];
            
        onSelectionChange(newSelection);
    };

    const formatDateTime = (dateTime: string) => {
        return new Date(dateTime).toLocaleString('en-US', {
            weekday: 'short',
            month: 'short',
            day: 'numeric',
            hour: 'numeric',
            minute: '2-digit',
            hour12: true
        });
    };

    const formatTimeRange = (startTime: string, endTime: string) => {
        const start = new Date(startTime);
        const end = new Date(endTime);
        
        return `${start.toLocaleTimeString('en-US', { 
            hour: 'numeric', 
            minute: '2-digit', 
            hour12: true 
        })} - ${end.toLocaleTimeString('en-US', { 
            hour: 'numeric', 
            minute: '2-digit', 
            hour12: true 
        })}`;
    };

    const getDifficultyColor = (level?: string) => {
        switch (level) {
            case 'Beginner': return 'success';
            case 'Intermediate': return 'warning';
            case 'Advanced': return 'danger';
            default: return 'secondary';
        }
    };

    const getSessionTypeIcon = (type: string) => {
        const typeMap: { [key: string]: string } = {
            'KEYNOTE': '🎯',
            'PRESENTATION': '📊',
            'WORKSHOP': '🛠️',
            'LAB': '⚗️',
            'BREAKOUT': '👥',
            'NETWORKING': '🤝',
            'TRAINING': '📚',
            'PANEL': '🗣️'
        };
        return typeMap[type] || '📋';
    };

    const groupSessionsByDate = (sessions: Session[]) => {
        return sessions.reduce((groups, session) => {
            const date = new Date(session.startTime).toDateString();
            if (!groups[date]) {
                groups[date] = [];
            }
            groups[date].push(session);
            groups[date].sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
            return groups;
        }, {} as { [date: string]: Session[] });
    };

    const getAvailabilityStatus = (session: Session) => {
        if (!session.capacity) return 'unlimited';
        if (session.availableSpots > 0) return 'available';
        if (session.isWaitlistEnabled) return 'waitlist';
        return 'full';
    };

    const getAvailabilityBadge = (session: Session) => {
        const status = getAvailabilityStatus(session);
        
        switch (status) {
            case 'available':
                return <span className="badge bg-success">✓ Available ({session.availableSpots} spots)</span>;
            case 'waitlist':
                return <span className="badge bg-warning">⏳ Waitlist Only</span>;
            case 'full':
                return <span className="badge bg-danger">❌ Full</span>;
            case 'unlimited':
                return <span className="badge bg-info">♾️ No Limit</span>;
            default:
                return null;
        }
    };

    if (loading) {
        return (
            <div className="text-center p-4">
                <div className="spinner-border" role="status">
                    <span className="visually-hidden">Loading sessions...</span>
                </div>
            </div>
        );
    }

    if (sessions.length === 0) {
        return (
            <div className="text-center p-4 text-muted">
                <h5>📅 No Sessions Available</h5>
                <p>This event doesn't have individual sessions to register for.</p>
            </div>
        );
    }

    const sessionGroups = groupSessionsByDate(sessions);

    return (
        <div className="session-selector">
            <div className="d-flex justify-content-between align-items-center mb-3">
                <h5>📅 Select Sessions to Attend</h5>
                {!readonly && (
                    <div className="btn-group" role="group">
                        <input
                            type="radio"
                            className="btn-check"
                            name="viewMode"
                            id="gridView"
                            checked={viewMode === 'grid'}
                            onChange={() => setViewMode('grid')}
                        />
                        <label className="btn btn-outline-primary btn-sm" htmlFor="gridView">
                            🔳 Grid
                        </label>
                        
                        <input
                            type="radio"
                            className="btn-check"
                            name="viewMode"
                            id="timelineView"
                            checked={viewMode === 'timeline'}
                            onChange={() => setViewMode('timeline')}
                        />
                        <label className="btn btn-outline-primary btn-sm" htmlFor="timelineView">
                            📅 Timeline
                        </label>
                    </div>
                )}
            </div>

            {conflicts.length > 0 && (
                <div className="alert alert-warning">
                    <h6>⚠️ Schedule Conflicts Detected:</h6>
                    <ul className="mb-0">
                        {conflicts.map((conflict, index) => (
                            <li key={index}>{conflict}</li>
                        ))}
                    </ul>
                </div>
            )}

            {selectedSessionIds.length > 0 && (
                <div className="alert alert-info">
                    <strong>📋 Selected Sessions:</strong> {selectedSessionIds.length} session{selectedSessionIds.length !== 1 ? 's' : ''} selected
                </div>
            )}

            {viewMode === 'grid' ? (
                <div className="row">
                    {sessions.map(session => {
                        const isSelected = selectedSessionIds.includes(session.id);
                        const availabilityStatus = getAvailabilityStatus(session);
                        const canSelect = availabilityStatus !== 'full' || isSelected;
                        
                        return (
                            <div key={session.id} className="col-lg-6 mb-3">
                                <div 
                                    className={`card h-100 ${isSelected ? 'border-primary bg-light' : ''} ${!readonly && canSelect ? 'session-selectable' : ''}`}
                                    onClick={() => canSelect && handleSessionToggle(session.id)}
                                    style={{ cursor: !readonly && canSelect ? 'pointer' : 'default' }}
                                >
                                    <div className="card-header d-flex justify-content-between align-items-start">
                                        <div className="flex-grow-1">
                                            <div className="d-flex align-items-center mb-1">
                                                {!readonly && (
                                                    <input
                                                        type="checkbox"
                                                        className="form-check-input me-2"
                                                        checked={isSelected}
                                                        onChange={() => {}} // Handled by card click
                                                        disabled={!canSelect}
                                                    />
                                                )}
                                                <h6 className="mb-0">
                                                    {getSessionTypeIcon(session.type)} {session.title}
                                                </h6>
                                            </div>
                                            <small className="text-muted">
                                                {formatTimeRange(session.startTime, session.endTime)}
                                            </small>
                                        </div>
                                        {getAvailabilityBadge(session)}
                                    </div>
                                    
                                    <div className="card-body">
                                        {session.description && (
                                            <p className="text-muted small mb-2">{session.description}</p>
                                        )}
                                        
                                        {(session.room || session.building) && (
                                            <p className="mb-1">
                                                <strong>📍 Location:</strong> {[session.building, session.room].filter(Boolean).join(', ')}
                                            </p>
                                        )}
                                        
                                        {session.presenter && (
                                            <p className="mb-1">
                                                <strong>🎤 Presenter:</strong> {session.presenter}
                                                {session.presenterTitle && ` (${session.presenterTitle})`}
                                            </p>
                                        )}
                                        
                                        {session.targetAudience && (
                                            <p className="mb-1">
                                                <strong>👥 Target Audience:</strong> {session.targetAudience}
                                            </p>
                                        )}
                                        
                                        {session.prerequisites && (
                                            <p className="mb-2">
                                                <strong>📚 Prerequisites:</strong> {session.prerequisites}
                                            </p>
                                        )}
                                        
                                        <div className="d-flex justify-content-between align-items-center">
                                            <div>
                                                {session.difficultyLevel && (
                                                    <span className={`badge bg-${getDifficultyColor(session.difficultyLevel)} me-2`}>
                                                        {session.difficultyLevel}
                                                    </span>
                                                )}
                                                {session.tags && session.tags.map(tag => (
                                                    <span key={tag} className="badge bg-secondary me-1 small">
                                                        {tag}
                                                    </span>
                                                ))}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>
            ) : (
                <div className="timeline-view">
                    {Object.entries(sessionGroups).map(([date, dateSessions]) => (
                        <div key={date} className="mb-4">
                            <h6 className="text-primary border-bottom pb-2">
                                📅 {new Date(date).toLocaleDateString('en-US', { 
                                    weekday: 'long', 
                                    year: 'numeric', 
                                    month: 'long', 
                                    day: 'numeric' 
                                })}
                            </h6>
                            
                            {dateSessions.map(session => {
                                const isSelected = selectedSessionIds.includes(session.id);
                                const availabilityStatus = getAvailabilityStatus(session);
                                const canSelect = availabilityStatus !== 'full' || isSelected;
                                
                                return (
                                    <div 
                                        key={session.id} 
                                        className={`card mb-2 ${isSelected ? 'border-primary bg-light' : ''}`}
                                        onClick={() => !readonly && canSelect && handleSessionToggle(session.id)}
                                        style={{ cursor: !readonly && canSelect ? 'pointer' : 'default' }}
                                    >
                                        <div className="card-body py-3">
                                            <div className="row align-items-center">
                                                <div className="col-auto">
                                                    {!readonly && (
                                                        <input
                                                            type="checkbox"
                                                            className="form-check-input"
                                                            checked={isSelected}
                                                            onChange={() => {}} // Handled by card click
                                                            disabled={!canSelect}
                                                        />
                                                    )}
                                                </div>
                                                <div className="col-2">
                                                    <div className="text-center">
                                                        <strong>{formatTimeRange(session.startTime, session.endTime)}</strong>
                                                    </div>
                                                </div>
                                                <div className="col-6">
                                                    <h6 className="mb-1">
                                                        {getSessionTypeIcon(session.type)} {session.title}
                                                    </h6>
                                                    {session.presenter && (
                                                        <small className="text-muted">👤 {session.presenter}</small>
                                                    )}
                                                </div>
                                                <div className="col-3">
                                                    <div className="text-end">
                                                        {getAvailabilityBadge(session)}
                                                        {(session.room || session.building) && (
                                                            <div className="small text-muted mt-1">
                                                                📍 {[session.building, session.room].filter(Boolean).join(', ')}
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    ))}
                </div>
            )}

            <style dangerouslySetInnerHTML={{
                __html: `
                .session-selectable:hover {
                    box-shadow: 0 4px 8px rgba(0,0,0,0.1);
                    transform: translateY(-1px);
                    transition: all 0.2s ease;
                }
                `
            }} />
        </div>
    );
};

export default SessionSelector;