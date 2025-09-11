import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import MobileCheckInInterface from '../components/MobileCheckInInterface';
import apiClient from '../api/apiClient';
import './MobileCheckInPage.css';

interface Event {
    id: string;
    name: string;
    startDateTime: string;
    venueName?: string;
    description?: string;
}

interface Session {
    id: string;
    title: string;
    startTime?: string;
    endTime?: string;
    location?: string;
}

interface StaffMember {
    name: string;
    role: string;
}

const MobileCheckInPage: React.FC = () => {
    const { eventId } = useParams<{ eventId: string }>();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    const [event, setEvent] = useState<Event | null>(null);
    const [session, setSession] = useState<Session | null>(null);
    const [staffMember, setStaffMember] = useState<StaffMember | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Get URL parameters
    const sessionId = searchParams.get('sessionId');
    const staffName = searchParams.get('staff') || 'Staff Member';
    const staffRole = searchParams.get('role') || 'Event Staff';

    useEffect(() => {
        if (!eventId) {
            setError('Event ID is required');
            setLoading(false);
            return;
        }

        loadEventData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [eventId, sessionId]);

    const loadEventData = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);

            // Load event data
            const eventResponse = await apiClient.get(`/events/${eventId}`);
            setEvent(eventResponse.data);

            // Load session data if sessionId is provided
            if (sessionId) {
                try {
                    const sessionResponse = await apiClient.get(`/sessions/${sessionId}`);
                    setSession(sessionResponse.data);
                } catch (sessionError) {
                    console.warn('Failed to load session data:', sessionError);
                    // Continue without session data
                }
            }

            // Set staff member info
            setStaffMember({
                name: decodeURIComponent(staffName),
                role: decodeURIComponent(staffRole)
            });

        } catch (error: any) {
            console.error('Failed to load event data:', error);
            setError(error.response?.data?.message || 'Failed to load event data');
        } finally {
            setLoading(false);
        }
    }, [eventId, sessionId, staffName, staffRole]);

    const handleCheckInSuccess = (checkInData: any) => {
        // Analytics tracking for mobile check-ins
        if ('gtag' in window) {
            (window as any).gtag('event', 'mobile_checkin_success', {
                event_id: eventId,
                session_id: sessionId,
                staff_name: staffMember?.name,
                method: checkInData.method
            });
        }
    };

    const handleCheckInError = (errorMessage: string) => {
        // Analytics tracking for mobile check-in errors
        if ('gtag' in window) {
            (window as any).gtag('event', 'mobile_checkin_error', {
                event_id: eventId,
                session_id: sessionId,
                staff_name: staffMember?.name,
                error_message: errorMessage
            });
        }
    };

    const goBack = () => {
        if (window.history.length > 1) {
            navigate(-1);
        } else {
            navigate('/events');
        }
    };

    if (loading) {
        return (
            <div className="mobile-checkin-page loading">
                <div className="loading-container">
                    <div className="loading-spinner"></div>
                    <div className="loading-text">Loading event data...</div>
                </div>
            </div>
        );
    }

    if (error || !event) {
        return (
            <div className="mobile-checkin-page error">
                <div className="error-container">
                    <div className="error-icon">❌</div>
                    <h2>Error Loading Event</h2>
                    <p>{error || 'Event not found'}</p>
                    <div className="error-actions">
                        <button onClick={goBack} className="btn btn-primary">
                            ← Go Back
                        </button>
                        <button onClick={loadEventData} className="btn btn-outline-primary">
                            🔄 Retry
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="mobile-checkin-page">
            {/* Header */}
            <div className="mobile-header">
                <button onClick={goBack} className="back-button">
                    ← Back
                </button>
                <div className="header-content">
                    <h1 className="event-title">{event.name}</h1>
                    {session && (
                        <h2 className="session-title">{session.title}</h2>
                    )}
                    <div className="event-details">
                        📅 {new Date(event.startDateTime).toLocaleDateString()}
                        {event.venueName && (
                            <>
                                {' • '}
                                📍 {event.venueName}
                            </>
                        )}
                    </div>
                    {session?.location && (
                        <div className="session-location">
                            🏢 {session.location}
                        </div>
                    )}
                </div>
            </div>

            {/* Staff Info Banner */}
            <div className="staff-banner">
                <div className="staff-info">
                    <div className="staff-avatar">
                        {staffMember?.name.charAt(0).toUpperCase()}
                    </div>
                    <div className="staff-details">
                        <div className="staff-name">{staffMember?.name}</div>
                        <div className="staff-role">{staffMember?.role}</div>
                    </div>
                </div>
                <div className="session-indicator">
                    {sessionId ? '📋 Session Check-In' : '🎟️ Event Check-In'}
                </div>
            </div>

            {/* Check-In Interface */}
            <div className="checkin-content">
                <MobileCheckInInterface
                    eventId={eventId}
                    sessionId={sessionId || undefined}
                    staffMember={staffMember?.name || 'Staff Member'}
                    onCheckInSuccess={handleCheckInSuccess}
                    onCheckInError={handleCheckInError}
                />
            </div>

            {/* Quick Actions Footer */}
            <div className="quick-actions-footer">
                <div className="quick-actions">
                    <button 
                        className="quick-action-btn"
                        onClick={() => window.location.reload()}
                        title="Refresh page"
                    >
                        🔄
                    </button>
                    <button 
                        className="quick-action-btn"
                        onClick={() => {
                            if ('share' in navigator) {
                                navigator.share({
                                    title: `${event.name} Check-In`,
                                    text: `Check-in to ${event.name}`,
                                    url: window.location.href
                                });
                            }
                        }}
                        title="Share check-in link"
                    >
                        📤
                    </button>
                    <button 
                        className="quick-action-btn"
                        onClick={() => {
                            const fullscreen = document.fullscreenElement;
                            if (fullscreen) {
                                document.exitFullscreen();
                            } else {
                                document.documentElement.requestFullscreen();
                            }
                        }}
                        title="Toggle fullscreen"
                    >
                        ⛶
                    </button>
                </div>
            </div>
        </div>
    );
};

export default MobileCheckInPage;