import React, { useState, useEffect, ChangeEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import apiClient from '../api/apiClient';
import RealTimeStats from '../components/RealTimeStats';
import { useWebSocket } from '../hooks/useWebSocket';
import webSocketService, { WebSocketMessage } from '../services/WebSocketService';

interface EventInstance {
    id: string;
    dateTime: string;
    location: string;
}

interface Event {
    id: string;
    name: string;
    description?: string;
    bannerImageUrl?: string;
    thumbnailImageUrl?: string;
    tags?: string[];
    instances?: EventInstance[];
    eventType?: string;
    category?: string;
    venueName?: string;
    address?: string;
    city?: string;
    state?: string;
    zipCode?: string;
    country?: string;
    virtualUrl?: string;
    dialInNumber?: string;
    accessCode?: string;
    requiresApproval?: boolean;
    maxRegistrations?: number;
    organizerName?: string;
    organizerEmail?: string;
    organizerPhone?: string;
    organizerWebsite?: string;
    startDateTime?: string;
    endDateTime?: string;
    timezone?: string;
    agenda?: string;
}

const EventDetailsPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [event, setEvent] = useState<Event | null>(null);
    const [selectedInstance, setSelectedInstance] = useState('');
    const [showFullDescription, setShowFullDescription] = useState(false);
    const [liveNotifications, setLiveNotifications] = useState<WebSocketMessage[]>([]);
    const [showNotifications, setShowNotifications] = useState(false);
    
    // WebSocket integration for real-time updates
    const webSocket = useWebSocket({
        eventId: id,
        autoConnect: true
    });

    const fetchEventDetails = () => {
        apiClient.get(`/events/${id}`)
            .then(response => {
                setEvent(response.data);
                if (response.data.instances && response.data.instances.length > 0) {
                    setSelectedInstance(response.data.instances[0].id);
                }
            })
            .catch(error => {
                console.error("There was an error fetching the event details!", error);
            });
    };

    useEffect(() => {
        fetchEventDetails();
    }, [id]);

    // Set up WebSocket subscriptions manually
    useEffect(() => {
        if (!id) return;

        const subscriptions = [
            webSocketService.subscribeToEventUpdates(id, (message: WebSocketMessage) => {
                setLiveNotifications(prev => [...prev.slice(-4), message]);
                if (message.updateType === 'DETAILS_CHANGED' || message.updateType === 'TIME_CHANGED') {
                    fetchEventDetails();
                }
            }),
            webSocketService.subscribeToEventRegistrations(id, (message: WebSocketMessage) => {
                setLiveNotifications(prev => [...prev.slice(-4), message]);
            }),
            webSocketService.subscribeToEventStatus(id, (message: WebSocketMessage) => {
                setLiveNotifications(prev => [...prev.slice(-4), message]);
                fetchEventDetails();
            })
        ];

        return () => {
            subscriptions.forEach(sub => sub.unsubscribe());
        };
    }, [id]);

    if (!event) {
        return (
            <div className="container mt-5 text-center">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading...</span>
                </div>
                <p className="mt-3">Loading event details...</p>
            </div>
        );
    }

    const formatEventDate = (dateString?: string) => {
        if (!dateString) return {
            weekday: 'TBD',
            date: 'Date TBD',
            time: 'Time TBD'
        };
        const date = new Date(dateString);
        return {
            weekday: date.toLocaleDateString('en-US', { weekday: 'long' }),
            date: date.toLocaleDateString('en-US', { month: 'long', day: 'numeric' }),
            time: date.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', timeZoneName: 'short' })
        };
    };

    const getRegistrationInfo = () => {
        const info: string[] = [];
        if (event.requiresApproval) info.push('Approval Required');
        if (event.maxRegistrations && event.maxRegistrations > 0) {
            info.push(`${event.maxRegistrations} spots available`);
        }
        return info.length > 0 ? info : ['Open Registration'];
    };

    const getFullAddress = () => {
        const parts = [event.address, event.city, event.state, event.zipCode].filter(Boolean);
        return parts.join(', ');
    };

    const eventDate = formatEventDate(event.startDateTime);

    const handleRegisterClick = () => {
        if (selectedInstance) {
            navigate(`/events/${id}/register/${selectedInstance}`);
        } else {
            navigate(`/events/${id}/register`);
        }
    };

    const handleAddToCalendar = () => {
        const calendarUrl = `http://localhost:8080/api/calendar/event/${id}.ics`;
        
        // Create a temporary link element and trigger download
        const link = document.createElement('a');
        link.href = calendarUrl;
        link.download = `event-${event.name?.replace(/[^a-z0-9]/gi, '_').toLowerCase()}.ics`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    };

    const handleShareEvent = () => {
        // TODO: Implement share functionality
        if (navigator.share) {
            navigator.share({
                title: event.name,
                text: event.description,
                url: window.location.href,
            });
        } else {
            navigator.clipboard.writeText(window.location.href);
            alert('Event link copied to clipboard!');
        }
    };

    const handleContactOrganizer = () => {
        if (event.organizerEmail) {
            window.location.href = `mailto:${event.organizerEmail}?subject=Question about ${event.name}`;
        } else {
            alert('Organizer contact information not available.');
        }
    };

    const handleGetDirections = () => {
        const address = getFullAddress();
        if (address) {
            const encodedAddress = encodeURIComponent(address);
            window.open(`https://www.google.com/maps/search/${encodedAddress}`, '_blank');
        }
    };

    return (
        <div className="bg-light min-vh-100">
            {/* Hero Banner */}
            <div className="position-relative">
                <img 
                    src={event.bannerImageUrl || 'https://via.placeholder.com/1200x600'} 
                    className="w-100" 
                    alt={event.name}
                    style={{ height: '400px', objectFit: 'cover' }}
                />
                <div className="position-absolute top-0 start-0 w-100 h-100" style={{ background: 'linear-gradient(to bottom, rgba(0,0,0,0.3), rgba(0,0,0,0.7))' }}>
                    <div className="container h-100 d-flex align-items-end">
                        <div className="text-white pb-5">
                            <div className="mb-2">
                                <span className={`badge ${event.eventType === 'VIRTUAL' ? 'bg-info' : event.eventType === 'HYBRID' ? 'bg-warning' : 'bg-success'} me-2`}>
                                    {event.eventType === 'VIRTUAL' ? 'Virtual Event' : event.eventType === 'HYBRID' ? 'Hybrid Event' : 'In-Person Event'}
                                </span>
                                {event.category && (
                                    <span className="badge bg-dark">
                                        {event.category.replace(/_/g, ' & ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}
                                    </span>
                                )}
                            </div>
                            <h1 className="display-4 fw-bold">{event.name}</h1>
                            <p className="lead">
                                By {event.organizerName || 'Event Organizer'}
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            <div className="container py-5">
                <div className="row">
                    <div className="col-lg-8">
                        {/* Date and Time Section */}
                        <div className="card mb-4">
                            <div className="card-body">
                                <div className="d-flex align-items-start">
                                    <div className="me-4">
                                        <div className="bg-primary text-white rounded p-3 text-center" style={{ minWidth: '80px' }}>
                                            <div className="fw-bold">{eventDate.weekday.substring(0, 3).toUpperCase()}</div>
                                            <div className="h4 mb-0">{new Date(event.startDateTime || '').getDate()}</div>
                                            <div className="small">{new Date(event.startDateTime || '').toLocaleDateString('en-US', { month: 'short' }).toUpperCase()}</div>
                                        </div>
                                    </div>
                                    <div>
                                        <h3>Date and time</h3>
                                        <p className="mb-1 h5">{eventDate.weekday}, {eventDate.date}</p>
                                        <p className="text-muted mb-0">{eventDate.time}</p>
                                        {event.endDateTime && (
                                            <p className="text-muted">
                                                Ends {formatEventDate(event.endDateTime).time}
                                            </p>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Location Section */}
                        <div className="card mb-4">
                            <div className="card-body">
                                <div className="d-flex align-items-start">
                                    <div className="me-4">
                                        <div className="bg-light rounded-circle p-3 d-flex align-items-center justify-content-center" style={{ width: '60px', height: '60px' }}>
                                            📍
                                        </div>
                                    </div>
                                    <div className="flex-grow-1">
                                        <h3>Location</h3>
                                        {event.eventType === 'VIRTUAL' ? (
                                            <div>
                                                <p className="h5 mb-2">Virtual Event</p>
                                                <p className="text-muted mb-2">Join from anywhere</p>
                                                {event.virtualUrl && (
                                                    <p className="mb-1">
                                                        <strong>Meeting URL:</strong> <a href={event.virtualUrl} target="_blank" rel="noopener noreferrer">{event.virtualUrl}</a>
                                                    </p>
                                                )}
                                                {event.dialInNumber && (
                                                    <p className="mb-1">
                                                        <strong>Dial-in:</strong> {event.dialInNumber}
                                                    </p>
                                                )}
                                                {event.accessCode && (
                                                    <p className="mb-1">
                                                        <strong>Access Code:</strong> {event.accessCode}
                                                    </p>
                                                )}
                                            </div>
                                        ) : (
                                            <div>
                                                <p className="h5 mb-2">{event.venueName || 'Venue TBD'}</p>
                                                <p className="text-muted mb-2">{getFullAddress() || 'Address TBD'}</p>
                                                {getFullAddress() && (
                                                    <button 
                                                        className="btn btn-outline-primary btn-sm"
                                                        onClick={handleGetDirections}
                                                    >
                                                        Get directions
                                                    </button>
                                                )}
                                                {event.eventType === 'HYBRID' && event.virtualUrl && (
                                                    <div className="mt-3 pt-3 border-top">
                                                        <p className="fw-bold mb-2">Virtual Option Available:</p>
                                                        <p className="mb-1">
                                                            <strong>Meeting URL:</strong> <a href={event.virtualUrl} target="_blank" rel="noopener noreferrer">{event.virtualUrl}</a>
                                                        </p>
                                                        {event.dialInNumber && (
                                                            <p className="mb-1">
                                                                <strong>Dial-in:</strong> {event.dialInNumber}
                                                            </p>
                                                        )}
                                                        {event.accessCode && (
                                                            <p className="mb-1">
                                                                <strong>Access Code:</strong> {event.accessCode}
                                                            </p>
                                                        )}
                                                    </div>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* About this event */}
                        <div className="card mb-4">
                            <div className="card-body">
                                <h3 className="mb-4">About this event</h3>
                                <div className="mb-3">
                                    {event.description && (
                                        <div>
                                            <ReactMarkdown>
                                                {showFullDescription || event.description.length <= 300 
                                                    ? event.description 
                                                    : event.description.substring(0, 300) + '...'
                                                }
                                            </ReactMarkdown>
                                            {event.description.length > 300 && (
                                                <button 
                                                    className="btn btn-link p-0"
                                                    onClick={() => setShowFullDescription(!showFullDescription)}
                                                >
                                                    {showFullDescription ? 'Show less' : 'Read more'}
                                                </button>
                                            )}
                                        </div>
                                    )}
                                </div>
                                {event.tags && event.tags.length > 0 && (
                                    <div>
                                        <h5 className="mb-3">Tags</h5>
                                        <div>
                                            {event.tags.map((tag, index) => (
                                                <span key={index} className="badge bg-light text-dark me-2 mb-2 p-2">
                                                    {tag}
                                                </span>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Agenda */}
                        {event.agenda && (
                            <div className="card mb-4">
                                <div className="card-body">
                                    <h3 className="mb-4">Agenda</h3>
                                    <ReactMarkdown>{event.agenda}</ReactMarkdown>
                                </div>
                            </div>
                        )}
                    </div>

                    <div className="col-lg-4">
                        {/* Connection Status */}
                        <div className="mb-3">
                            <div className="d-flex align-items-center justify-content-between">
                                <small className={`badge ${webSocket.isConnected ? 'bg-success' : 'bg-secondary'}`}>
                                    {webSocket.isConnected ? '🟢 Live Updates Active' : '⚫ Offline'}
                                </small>
                                {liveNotifications.length > 0 && (
                                    <button
                                        className="btn btn-sm btn-outline-info"
                                        onClick={() => setShowNotifications(!showNotifications)}
                                    >
                                        🔔 {liveNotifications.length} notifications
                                    </button>
                                )}
                            </div>
                        </div>

                        {/* Live Notifications */}
                        {showNotifications && liveNotifications.length > 0 && (
                            <div className="card mb-4">
                                <div className="card-header d-flex justify-content-between align-items-center">
                                    <h6 className="mb-0">📡 Live Updates</h6>
                                    <button
                                        className="btn btn-sm btn-outline-secondary"
                                        onClick={() => setLiveNotifications([])}
                                    >
                                        Clear
                                    </button>
                                </div>
                                <div className="card-body">
                                    {liveNotifications.map((notification, index) => (
                                        <div key={index} className="alert alert-info alert-sm mb-2">
                                            <div className="d-flex align-items-center">
                                                <span className="me-2">
                                                    {notification.type === 'REGISTRATION_UPDATE' ? '👥' :
                                                     notification.type === 'EVENT_UPDATE' ? '📝' :
                                                     notification.type === 'EVENT_STATUS_CHANGE' ? '🔄' : '📡'}
                                                </span>
                                                <div className="flex-grow-1">
                                                    <small>
                                                        <strong>
                                                            {notification.type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}
                                                        </strong>
                                                        <br />
                                                        {new Date(notification.timestamp).toLocaleString()}
                                                    </small>
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Real-time Stats */}
                        <div className="mb-4">
                            <RealTimeStats eventId={id!} />
                        </div>

                        {/* Registration Card */}
                        <div className="card mb-4 sticky-top" style={{ top: '20px' }}>
                            <div className="card-header bg-primary text-white">
                                <h4 className="mb-0">📝 Event Registration</h4>
                            </div>
                            <div className="card-body">
                                <div className="mb-4">
                                    <div className="registration-status mb-3">
                                        {getRegistrationInfo().map((info, index) => (
                                            <div key={index} className="d-flex align-items-center mb-2">
                                                <span className="badge bg-success me-2">✓</span>
                                                <span>{info}</span>
                                            </div>
                                        ))}
                                    </div>
                                    
                                    <button 
                                        className="btn btn-success btn-lg w-100 mb-3"
                                        onClick={handleRegisterClick}
                                    >
                                        🎯 Register Now
                                    </button>
                                    
                                    <div className="row g-2">
                                        <div className="col-6">
                                            <button 
                                                className="btn btn-outline-primary btn-sm w-100"
                                                onClick={handleAddToCalendar}
                                            >
                                                📅 Add to Calendar
                                            </button>
                                        </div>
                                        <div className="col-6">
                                            <button 
                                                className="btn btn-outline-secondary btn-sm w-100"
                                                onClick={handleShareEvent}
                                            >
                                                📤 Share Event
                                            </button>
                                        </div>
                                    </div>
                                </div>

                                {event.instances && event.instances.length > 0 && (
                                    <div className="mb-3">
                                        <label className="form-label fw-bold">Select Date & Time</label>
                                        <select 
                                            className="form-select"
                                            value={selectedInstance} 
                                            onChange={(e: ChangeEvent<HTMLSelectElement>) => setSelectedInstance(e.target.value)}
                                        >
                                            {event.instances.map(instance => (
                                                <option key={instance.id} value={instance.id}>
                                                    {new Date(instance.dateTime).toLocaleDateString()} - {instance.location}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Organizer Info */}
                        {event.organizerName && (
                            <div className="card">
                                <div className="card-body">
                                    <h4 className="mb-3">Event Organizer</h4>
                                    <div className="d-flex align-items-center mb-3">
                                        <div className="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center me-3" style={{ width: '50px', height: '50px' }}>
                                            <span className="fw-bold">{event.organizerName.charAt(0).toUpperCase()}</span>
                                        </div>
                                        <div>
                                            <div className="fw-bold">{event.organizerName}</div>
                                            {event.organizerEmail && (
                                                <small className="text-muted">{event.organizerEmail}</small>
                                            )}
                                        </div>
                                    </div>
                                    <div className="d-grid gap-2">
                                        {event.organizerWebsite && (
                                            <a href={event.organizerWebsite} target="_blank" rel="noopener noreferrer" className="btn btn-outline-primary btn-sm">
                                                Visit Website
                                            </a>
                                        )}
                                        <button 
                                            className="btn btn-outline-secondary btn-sm"
                                            onClick={handleContactOrganizer}
                                        >
                                            Contact Organizer
                                        </button>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default EventDetailsPage;