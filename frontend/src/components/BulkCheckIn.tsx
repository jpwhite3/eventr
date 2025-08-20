import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';

interface BulkCheckInProps {
    eventId: string;
    sessionId?: string;
    staffMember: string;
    onBulkSuccess?: (count: number) => void;
    onBulkError?: (error: string) => void;
}

interface Registration {
    id: string;
    userName: string;
    userEmail: string;
    status: string;
    isCheckedIn?: boolean;
}

interface Session {
    id: string;
    title: string;
    startTime: string;
    expectedAttendees: number;
}

const BulkCheckIn: React.FC<BulkCheckInProps> = ({
    eventId,
    sessionId,
    staffMember,
    onBulkSuccess,
    onBulkError
}) => {
    const [registrations, setRegistrations] = useState<Registration[]>([]);
    const [sessions, setSessions] = useState<Session[]>([]);
    const [selectedSession, setSelectedSession] = useState<string>(sessionId || '');
    const [selectedRegistrations, setSelectedRegistrations] = useState<string[]>([]);
    const [loading, setLoading] = useState(false);
    const [processing, setProcessing] = useState(false);
    const [notes, setNotes] = useState('');
    const [filter, setFilter] = useState('');
    const [showCheckedIn, setShowCheckedIn] = useState(false);

    useEffect(() => {
        loadData();
    }, [eventId]);

    const loadData = async () => {
        setLoading(true);
        try {
            const [registrationsResponse, sessionsResponse] = await Promise.all([
                apiClient.get(`/registrations?eventId=${eventId}`),
                apiClient.get(`/sessions/event/${eventId}`)
            ]);

            // Add check-in status to registrations
            const registrationsWithStatus = await Promise.all(
                registrationsResponse.data.map(async (reg: Registration) => {
                    try {
                        // This would typically be a more efficient batch API call
                        const checkInResponse = await apiClient.get(`/checkin/registration/${reg.id}/status`);
                        return { ...reg, isCheckedIn: checkInResponse.data.isCheckedIn };
                    } catch {
                        return { ...reg, isCheckedIn: false };
                    }
                })
            );

            setRegistrations(registrationsWithStatus);
            setSessions(sessionsResponse.data);
        } catch (error) {
            console.error('Failed to load data:', error);
        } finally {
            setLoading(false);
        }
    };

    const filteredRegistrations = registrations.filter(reg => {
        const matchesFilter = !filter || 
            reg.userName.toLowerCase().includes(filter.toLowerCase()) ||
            reg.userEmail.toLowerCase().includes(filter.toLowerCase());
        
        const matchesCheckedInFilter = showCheckedIn || !reg.isCheckedIn;
        
        return matchesFilter && matchesCheckedInFilter;
    });

    const handleSelectAll = () => {
        const availableRegistrations = filteredRegistrations
            .filter(reg => !reg.isCheckedIn)
            .map(reg => reg.id);
        
        if (selectedRegistrations.length === availableRegistrations.length) {
            setSelectedRegistrations([]);
        } else {
            setSelectedRegistrations(availableRegistrations);
        }
    };

    const handleSelectRegistration = (registrationId: string) => {
        setSelectedRegistrations(prev => 
            prev.includes(registrationId)
                ? prev.filter(id => id !== registrationId)
                : [...prev, registrationId]
        );
    };

    const handleBulkCheckIn = async () => {
        if (selectedRegistrations.length === 0) {
            alert('Please select at least one registration');
            return;
        }

        setProcessing(true);

        try {
            const bulkData = {
                registrationIds: selectedRegistrations,
                sessionId: selectedSession || null,
                type: selectedSession ? 'SESSION' : 'EVENT',
                checkedInBy: staffMember,
                notes: notes.trim() || null
            };

            const response = await apiClient.post('/checkin/bulk', bulkData);
            const successCount = response.data.length;

            // Update registration status locally
            setRegistrations(prev => prev.map(reg => 
                selectedRegistrations.includes(reg.id)
                    ? { ...reg, isCheckedIn: true }
                    : reg
            ));

            setSelectedRegistrations([]);
            setNotes('');
            
            onBulkSuccess?.(successCount);
            
        } catch (error: any) {
            const errorMessage = error.response?.data?.message || 'Bulk check-in failed';
            onBulkError?.(errorMessage);
        } finally {
            setProcessing(false);
        }
    };

    const handleQuickActions = (action: string) => {
        switch (action) {
            case 'select-session-attendees':
                if (!selectedSession) {
                    alert('Please select a session first');
                    return;
                }
                // In a real implementation, you'd filter by session registrations
                const sessionAttendees = filteredRegistrations
                    .filter(reg => !reg.isCheckedIn)
                    .map(reg => reg.id);
                setSelectedRegistrations(sessionAttendees);
                break;
                
            case 'select-vip':
                // Example: select VIP attendees (this would require additional data)
                const vipAttendees = filteredRegistrations
                    .filter(reg => !reg.isCheckedIn && reg.userEmail.includes('vip'))
                    .map(reg => reg.id);
                setSelectedRegistrations(vipAttendees);
                break;
                
            case 'clear-selection':
                setSelectedRegistrations([]);
                break;
        }
    };

    if (loading) {
        return (
            <div className="text-center p-4">
                <div className="spinner-border" role="status">
                    <span className="visually-hidden">Loading registrations...</span>
                </div>
            </div>
        );
    }

    return (
        <div className="bulk-checkin">
            {/* Header Controls */}
            <div className="card mb-4">
                <div className="card-header">
                    <div className="row align-items-center">
                        <div className="col-md-6">
                            <h6 className="mb-0">📋 Bulk Check-In</h6>
                            <small className="text-muted">
                                {selectedRegistrations.length} selected • {filteredRegistrations.filter(r => !r.isCheckedIn).length} available
                            </small>
                        </div>
                        <div className="col-md-6 text-end">
                            <div className="btn-group btn-group-sm">
                                <button 
                                    className="btn btn-outline-primary"
                                    onClick={handleSelectAll}
                                >
                                    {selectedRegistrations.length === filteredRegistrations.filter(r => !r.isCheckedIn).length ? 
                                        '☐ Deselect All' : '☑️ Select All Available'}
                                </button>
                                <div className="dropdown">
                                    <button className="btn btn-outline-secondary dropdown-toggle" 
                                            type="button" data-bs-toggle="dropdown">
                                        Quick Actions
                                    </button>
                                    <ul className="dropdown-menu">
                                        <li>
                                            <button className="dropdown-item" 
                                                    onClick={() => handleQuickActions('select-session-attendees')}>
                                                👥 Select Session Attendees
                                            </button>
                                        </li>
                                        <li>
                                            <button className="dropdown-item" 
                                                    onClick={() => handleQuickActions('clear-selection')}>
                                                🚫 Clear Selection
                                            </button>
                                        </li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="card-body">
                    {/* Filters */}
                    <div className="row mb-3">
                        <div className="col-md-4">
                            <input
                                type="text"
                                className="form-control form-control-sm"
                                placeholder="🔍 Search by name or email..."
                                value={filter}
                                onChange={(e) => setFilter(e.target.value)}
                            />
                        </div>
                        <div className="col-md-4">
                            <select 
                                className="form-select form-select-sm"
                                value={selectedSession}
                                onChange={(e) => setSelectedSession(e.target.value)}
                            >
                                <option value="">📅 All Event Check-ins</option>
                                {sessions.map(session => (
                                    <option key={session.id} value={session.id}>
                                        {session.title} - {new Date(session.startTime).toLocaleTimeString('en-US', {
                                            hour: 'numeric',
                                            minute: '2-digit',
                                            hour12: true
                                        })}
                                    </option>
                                ))}
                            </select>
                        </div>
                        <div className="col-md-4">
                            <div className="form-check">
                                <input 
                                    className="form-check-input"
                                    type="checkbox"
                                    id="showCheckedIn"
                                    checked={showCheckedIn}
                                    onChange={(e) => setShowCheckedIn(e.target.checked)}
                                />
                                <label className="form-check-label small" htmlFor="showCheckedIn">
                                    Show already checked-in
                                </label>
                            </div>
                        </div>
                    </div>

                    {/* Bulk Action Controls */}
                    {selectedRegistrations.length > 0 && (
                        <div className="alert alert-primary">
                            <div className="row align-items-end">
                                <div className="col-md-8">
                                    <label className="form-label small">Notes (optional)</label>
                                    <input
                                        type="text"
                                        className="form-control form-control-sm"
                                        placeholder="Bulk check-in notes..."
                                        value={notes}
                                        onChange={(e) => setNotes(e.target.value)}
                                    />
                                </div>
                                <div className="col-md-4">
                                    <button 
                                        className="btn btn-primary w-100"
                                        onClick={handleBulkCheckIn}
                                        disabled={processing}
                                    >
                                        {processing ? 
                                            `⏳ Processing ${selectedRegistrations.length}...` : 
                                            `✅ Check In ${selectedRegistrations.length} Selected`
                                        }
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            {/* Registration List */}
            <div className="card">
                <div className="card-body p-0">
                    {filteredRegistrations.length === 0 ? (
                        <div className="text-center p-5 text-muted">
                            <span className="h1">👥</span>
                            <p className="mb-0">
                                {filter ? 'No registrations match your search' : 'No registrations found'}
                            </p>
                        </div>
                    ) : (
                        <div className="table-responsive" style={{ maxHeight: '500px', overflowY: 'auto' }}>
                            <table className="table table-hover mb-0">
                                <thead className="table-light sticky-top">
                                    <tr>
                                        <th style={{width: "50px"}}>
                                            <input 
                                                type="checkbox"
                                                className="form-check-input"
                                                checked={selectedRegistrations.length === filteredRegistrations.filter(r => !r.isCheckedIn).length && filteredRegistrations.filter(r => !r.isCheckedIn).length > 0}
                                                onChange={handleSelectAll}
                                            />
                                        </th>
                                        <th>Name</th>
                                        <th>Email</th>
                                        <th>Status</th>
                                        <th style={{width: "80px"}}>Check-In</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {filteredRegistrations.map((registration) => (
                                        <tr 
                                            key={registration.id}
                                            className={selectedRegistrations.includes(registration.id) ? 'table-primary' : ''}
                                        >
                                            <td>
                                                <input 
                                                    type="checkbox"
                                                    className="form-check-input"
                                                    checked={selectedRegistrations.includes(registration.id)}
                                                    onChange={() => handleSelectRegistration(registration.id)}
                                                    disabled={registration.isCheckedIn}
                                                />
                                            </td>
                                            <td>
                                                <strong>{registration.userName}</strong>
                                            </td>
                                            <td>
                                                <small className="text-muted">{registration.userEmail}</small>
                                            </td>
                                            <td>
                                                <span className={`badge ${
                                                    registration.status === 'REGISTERED' ? 'bg-success' : 
                                                    registration.status === 'WAITLIST' ? 'bg-warning' : 'bg-secondary'
                                                } small`}>
                                                    {registration.status}
                                                </span>
                                            </td>
                                            <td>
                                                {registration.isCheckedIn ? (
                                                    <span className="badge bg-primary small">✅ Done</span>
                                                ) : (
                                                    <span className="badge bg-light text-dark small">⏳ Pending</span>
                                                )}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </div>

            {/* Statistics */}
            <div className="row mt-4">
                <div className="col-md-3">
                    <div className="card text-center">
                        <div className="card-body">
                            <div className="h5 text-primary">{registrations.length}</div>
                            <small className="text-muted">Total Registered</small>
                        </div>
                    </div>
                </div>
                <div className="col-md-3">
                    <div className="card text-center">
                        <div className="card-body">
                            <div className="h5 text-success">{registrations.filter(r => r.isCheckedIn).length}</div>
                            <small className="text-muted">Already Checked In</small>
                        </div>
                    </div>
                </div>
                <div className="col-md-3">
                    <div className="card text-center">
                        <div className="card-body">
                            <div className="h5 text-warning">{registrations.filter(r => !r.isCheckedIn).length}</div>
                            <small className="text-muted">Pending Check-in</small>
                        </div>
                    </div>
                </div>
                <div className="col-md-3">
                    <div className="card text-center">
                        <div className="card-body">
                            <div className="h5 text-info">{selectedRegistrations.length}</div>
                            <small className="text-muted">Currently Selected</small>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default BulkCheckIn;