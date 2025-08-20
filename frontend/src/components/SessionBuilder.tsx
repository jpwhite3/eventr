import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';

interface Session {
    id?: string;
    eventId: string;
    title: string;
    description?: string;
    type: string;
    startTime: string;
    endTime: string;
    location?: string;
    room?: string;
    building?: string;
    capacity?: number;
    isRegistrationRequired: boolean;
    isWaitlistEnabled: boolean;
    presenter?: string;
    presenterTitle?: string;
    presenterBio?: string;
    presenterEmail?: string;
    materialUrl?: string;
    prerequisites?: string;
    targetAudience?: string;
    difficultyLevel?: string;
    tags?: string[];
    registeredCount?: number;
    waitlistCount?: number;
    availableSpots?: number;
}

interface SessionBuilderProps {
    eventId: string;
    onSessionsChange?: (sessions: Session[]) => void;
}

const SessionBuilder: React.FC<SessionBuilderProps> = ({ eventId, onSessionsChange }) => {
    const [sessions, setSessions] = useState<Session[]>([]);
    const [showForm, setShowForm] = useState(false);
    const [editingSession, setEditingSession] = useState<Session | null>(null);
    const [conflicts, setConflicts] = useState<string[]>([]);
    const [loading, setLoading] = useState(false);

    const sessionTypes = [
        { value: 'KEYNOTE', label: '🎯 Keynote' },
        { value: 'PRESENTATION', label: '📊 Presentation' },
        { value: 'WORKSHOP', label: '🛠️ Workshop' },
        { value: 'LAB', label: '⚗️ Lab' },
        { value: 'BREAKOUT', label: '👥 Breakout Session' },
        { value: 'NETWORKING', label: '🤝 Networking' },
        { value: 'MEAL', label: '🍽️ Meal' },
        { value: 'BREAK', label: '☕ Break' },
        { value: 'TRAINING', label: '📚 Training' },
        { value: 'PANEL', label: '🗣️ Panel Discussion' },
        { value: 'QA', label: '❓ Q&A' },
        { value: 'OTHER', label: '📋 Other' }
    ];

    const difficultyLevels = ['Beginner', 'Intermediate', 'Advanced'];

    const [formData, setFormData] = useState<Session>({
        eventId,
        title: '',
        description: '',
        type: 'PRESENTATION',
        startTime: '',
        endTime: '',
        location: '',
        room: '',
        building: '',
        capacity: undefined,
        isRegistrationRequired: true,
        isWaitlistEnabled: true,
        presenter: '',
        presenterTitle: '',
        presenterBio: '',
        presenterEmail: '',
        materialUrl: '',
        prerequisites: '',
        targetAudience: '',
        difficultyLevel: 'Beginner',
        tags: []
    });

    useEffect(() => {
        fetchSessions();
    }, [eventId]);

    useEffect(() => {
        if (onSessionsChange) {
            onSessionsChange(sessions);
        }
    }, [sessions, onSessionsChange]);

    const fetchSessions = async () => {
        try {
            const response = await apiClient.get(`/sessions/event/${eventId}`);
            setSessions(response.data);
        } catch (error) {
            console.error('Failed to fetch sessions:', error);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setConflicts([]);

        try {
            if (editingSession) {
                await apiClient.put(`/sessions/${editingSession.id}`, formData);
            } else {
                await apiClient.post('/sessions', formData);
            }
            
            await fetchSessions();
            resetForm();
        } catch (error: any) {
            if (error.response?.status === 400) {
                setConflicts([error.response.data.message || 'Validation error occurred']);
            } else {
                console.error('Failed to save session:', error);
                setConflicts(['Failed to save session. Please try again.']);
            }
        } finally {
            setLoading(false);
        }
    };

    const handleEdit = (session: Session) => {
        setEditingSession(session);
        setFormData({ ...session });
        setShowForm(true);
    };

    const handleDelete = async (sessionId: string) => {
        if (!window.confirm('Are you sure you want to delete this session? All registrations will be cancelled.')) {
            return;
        }

        try {
            await apiClient.delete(`/sessions/${sessionId}`);
            await fetchSessions();
        } catch (error) {
            console.error('Failed to delete session:', error);
        }
    };

    const resetForm = () => {
        setFormData({
            eventId,
            title: '',
            description: '',
            type: 'PRESENTATION',
            startTime: '',
            endTime: '',
            location: '',
            room: '',
            building: '',
            capacity: undefined,
            isRegistrationRequired: true,
            isWaitlistEnabled: true,
            presenter: '',
            presenterTitle: '',
            presenterBio: '',
            presenterEmail: '',
            materialUrl: '',
            prerequisites: '',
            targetAudience: '',
            difficultyLevel: 'Beginner',
            tags: []
        });
        setEditingSession(null);
        setShowForm(false);
        setConflicts([]);
    };

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const { name, value, type } = e.target;
        const checked = (e.target as HTMLInputElement).checked;
        
        setFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : 
                   type === 'number' ? (value ? parseInt(value) : undefined) : 
                   value
        }));
    };

    const handleTagsChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const tags = e.target.value.split(',').map(tag => tag.trim()).filter(tag => tag);
        setFormData(prev => ({ ...prev, tags }));
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

    const getSessionTypeIcon = (type: string) => {
        return sessionTypes.find(st => st.value === type)?.label.split(' ')[0] || '📋';
    };

    return (
        <div className="session-builder">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h4>📅 Session Management</h4>
                <button 
                    className="btn btn-primary"
                    onClick={() => setShowForm(!showForm)}
                >
                    {showForm ? '❌ Cancel' : '➕ Add Session'}
                </button>
            </div>

            {showForm && (
                <div className="card mb-4">
                    <div className="card-header">
                        <h5>{editingSession ? '✏️ Edit Session' : '➕ Add New Session'}</h5>
                    </div>
                    <div className="card-body">
                        {conflicts.length > 0 && (
                            <div className="alert alert-danger">
                                <h6>⚠️ Conflicts Detected:</h6>
                                <ul className="mb-0">
                                    {conflicts.map((conflict, index) => (
                                        <li key={index}>{conflict}</li>
                                    ))}
                                </ul>
                            </div>
                        )}

                        <form onSubmit={handleSubmit}>
                            <div className="row">
                                <div className="col-md-8 mb-3">
                                    <label className="form-label">Session Title *</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        name="title"
                                        value={formData.title}
                                        onChange={handleInputChange}
                                        required
                                    />
                                </div>
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Session Type</label>
                                    <select
                                        className="form-select"
                                        name="type"
                                        value={formData.type}
                                        onChange={handleInputChange}
                                    >
                                        {sessionTypes.map(type => (
                                            <option key={type.value} value={type.value}>
                                                {type.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            </div>

                            <div className="mb-3">
                                <label className="form-label">Description</label>
                                <textarea
                                    className="form-control"
                                    name="description"
                                    value={formData.description}
                                    onChange={handleInputChange}
                                    rows={3}
                                />
                            </div>

                            <div className="row">
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Start Time *</label>
                                    <input
                                        type="datetime-local"
                                        className="form-control"
                                        name="startTime"
                                        value={formData.startTime}
                                        onChange={handleInputChange}
                                        required
                                    />
                                </div>
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">End Time *</label>
                                    <input
                                        type="datetime-local"
                                        className="form-control"
                                        name="endTime"
                                        value={formData.endTime}
                                        onChange={handleInputChange}
                                        required
                                    />
                                </div>
                            </div>

                            <div className="row">
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Building</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        name="building"
                                        value={formData.building}
                                        onChange={handleInputChange}
                                        placeholder="Main Building"
                                    />
                                </div>
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Room</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        name="room"
                                        value={formData.room}
                                        onChange={handleInputChange}
                                        placeholder="Conference Room A"
                                    />
                                </div>
                                <div className="col-md-4 mb-3">
                                    <label className="form-label">Capacity</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        name="capacity"
                                        value={formData.capacity || ''}
                                        onChange={handleInputChange}
                                        min="1"
                                    />
                                </div>
                            </div>

                            <div className="row">
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Presenter</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        name="presenter"
                                        value={formData.presenter}
                                        onChange={handleInputChange}
                                    />
                                </div>
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Presenter Title</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        name="presenterTitle"
                                        value={formData.presenterTitle}
                                        onChange={handleInputChange}
                                    />
                                </div>
                            </div>

                            <div className="row">
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Target Audience</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        name="targetAudience"
                                        value={formData.targetAudience}
                                        onChange={handleInputChange}
                                        placeholder="Developers, Managers, All Staff"
                                    />
                                </div>
                                <div className="col-md-6 mb-3">
                                    <label className="form-label">Difficulty Level</label>
                                    <select
                                        className="form-select"
                                        name="difficultyLevel"
                                        value={formData.difficultyLevel}
                                        onChange={handleInputChange}
                                    >
                                        {difficultyLevels.map(level => (
                                            <option key={level} value={level}>{level}</option>
                                        ))}
                                    </select>
                                </div>
                            </div>

                            <div className="mb-3">
                                <label className="form-label">Tags (comma-separated)</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    value={formData.tags?.join(', ') || ''}
                                    onChange={handleTagsChange}
                                    placeholder="technical, hands-on, leadership"
                                />
                            </div>

                            <div className="row">
                                <div className="col-md-6 mb-3">
                                    <div className="form-check">
                                        <input
                                            className="form-check-input"
                                            type="checkbox"
                                            name="isRegistrationRequired"
                                            id="requireRegistration"
                                            checked={formData.isRegistrationRequired}
                                            onChange={handleInputChange}
                                        />
                                        <label className="form-check-label" htmlFor="requireRegistration">
                                            Require Registration
                                        </label>
                                    </div>
                                </div>
                                <div className="col-md-6 mb-3">
                                    <div className="form-check">
                                        <input
                                            className="form-check-input"
                                            type="checkbox"
                                            name="isWaitlistEnabled"
                                            id="enableWaitlist"
                                            checked={formData.isWaitlistEnabled}
                                            onChange={handleInputChange}
                                        />
                                        <label className="form-check-label" htmlFor="enableWaitlist">
                                            Enable Waitlist
                                        </label>
                                    </div>
                                </div>
                            </div>

                            <div className="d-flex gap-2">
                                <button type="submit" className="btn btn-primary" disabled={loading}>
                                    {loading ? '⏳ Saving...' : (editingSession ? '💾 Update Session' : '➕ Add Session')}
                                </button>
                                <button type="button" className="btn btn-secondary" onClick={resetForm}>
                                    Cancel
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {sessions.length === 0 ? (
                <div className="text-center p-5 text-muted">
                    <h5>📅 No Sessions Yet</h5>
                    <p>Add your first session to get started with multi-session event management.</p>
                </div>
            ) : (
                <div className="sessions-list">
                    <div className="row">
                        {sessions.map(session => (
                            <div key={session.id} className="col-lg-6 mb-3">
                                <div className="card h-100">
                                    <div className="card-header d-flex justify-content-between align-items-start">
                                        <div>
                                            <h6 className="mb-1">
                                                {getSessionTypeIcon(session.type)} {session.title}
                                            </h6>
                                            <small className="text-muted">
                                                {formatDateTime(session.startTime)} - {formatDateTime(session.endTime)}
                                            </small>
                                        </div>
                                        <div className="dropdown">
                                            <button className="btn btn-sm btn-outline-secondary dropdown-toggle" 
                                                    type="button" data-bs-toggle="dropdown">
                                                ⚙️
                                            </button>
                                            <ul className="dropdown-menu">
                                                <li>
                                                    <button className="dropdown-item" onClick={() => handleEdit(session)}>
                                                        ✏️ Edit
                                                    </button>
                                                </li>
                                                <li>
                                                    <button className="dropdown-item text-danger" 
                                                            onClick={() => session.id && handleDelete(session.id)}>
                                                        🗑️ Delete
                                                    </button>
                                                </li>
                                            </ul>
                                        </div>
                                    </div>
                                    <div className="card-body">
                                        {session.description && (
                                            <p className="text-muted small mb-2">{session.description}</p>
                                        )}
                                        
                                        {(session.room || session.building) && (
                                            <p className="mb-2">
                                                📍 {[session.building, session.room].filter(Boolean).join(', ')}
                                            </p>
                                        )}
                                        
                                        {session.presenter && (
                                            <p className="mb-2">
                                                🎤 {session.presenter}
                                                {session.presenterTitle && ` (${session.presenterTitle})`}
                                            </p>
                                        )}
                                        
                                        <div className="d-flex justify-content-between align-items-end">
                                            <div>
                                                {session.capacity && (
                                                    <span className="badge bg-primary me-2">
                                                        👥 {session.registeredCount}/{session.capacity}
                                                    </span>
                                                )}
                                                {(session.waitlistCount || 0) > 0 && (
                                                    <span className="badge bg-warning">
                                                        ⏳ {session.waitlistCount} waitlisted
                                                    </span>
                                                )}
                                            </div>
                                            <small className="text-muted">
                                                {session.difficultyLevel}
                                            </small>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
};

export default SessionBuilder;