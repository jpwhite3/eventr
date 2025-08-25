import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import apiClient from '../api/apiClient';
import RegistrationForm from '../components/RegistrationForm';
import QRCodeDisplay from '../components/QRCodeDisplay';

interface Event {
    id: string;
    name: string;
    description?: string;
    bannerImageUrl?: string;
    eventType?: string;
    category?: string;
    startDateTime?: string;
    endDateTime?: string;
    organizerName?: string;
}

const RegistrationPage: React.FC = () => {
    const { id, instanceId } = useParams<{ id: string; instanceId?: string }>();
    const navigate = useNavigate();
    const [event, setEvent] = useState<Event | null>(null);
    const [loading, setLoading] = useState(true);
    const [registrationComplete, setRegistrationComplete] = useState(false);
    const [registrationData, setRegistrationData] = useState<{id: string, userName: string, userEmail: string} | null>(null);

    useEffect(() => {
        if (id) {
            apiClient.get(`/events/${id}`)
                .then(response => {
                    setEvent(response.data);
                })
                .catch(error => {
                    console.error("There was an error fetching the event details!", error);
                })
                .finally(() => {
                    setLoading(false);
                });
        }
    }, [id]);

    const formatEventDate = (dateString?: string) => {
        if (!dateString) return 'Date TBD';
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', { 
            weekday: 'long', 
            year: 'numeric', 
            month: 'long', 
            day: 'numeric',
            hour: 'numeric',
            minute: '2-digit'
        });
    };

    if (loading) {
        return (
            <div className="container mt-5 text-center">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading...</span>
                </div>
                <p className="mt-3">Loading registration form...</p>
            </div>
        );
    }

    if (!event) {
        return (
            <div className="container mt-5">
                <div className="alert alert-danger">
                    <h4>Event Not Found</h4>
                    <p>The event you're trying to register for could not be found.</p>
                    <button className="btn btn-primary" onClick={() => navigate('/')}>
                        Return to Home
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="bg-light min-vh-100">
            {/* Header */}
            <div className="bg-primary text-white py-4">
                <div className="container">
                    <div className="row align-items-center">
                        <div className="col">
                            <button 
                                className="btn btn-outline-light me-3"
                                onClick={() => navigate(`/events/${id}`)}
                            >
                                ← Back to Event
                            </button>
                            <h1 className="h3 mb-0 d-inline">Event Registration</h1>
                        </div>
                    </div>
                </div>
            </div>

            <div className="container py-5">
                <div className="row justify-content-center">
                    <div className="col-lg-8">
                        {/* Event Summary Card */}
                        <div className="card mb-4">
                            <div className="card-body">
                                <div className="row">
                                    <div className="col-md-3">
                                        <img 
                                            src={event.bannerImageUrl || 'https://via.placeholder.com/300x200'} 
                                            className="img-fluid rounded" 
                                            alt={event.name}
                                        />
                                    </div>
                                    <div className="col-md-9">
                                        <div className="d-flex align-items-start justify-content-between">
                                            <div>
                                                <h2 className="h4 mb-2">{event.name}</h2>
                                                <p className="text-muted mb-2">
                                                    <i className="bi bi-calendar-event me-2"></i>
                                                    {formatEventDate(event.startDateTime)}
                                                </p>
                                                <p className="text-muted mb-2">
                                                    <i className="bi bi-person me-2"></i>
                                                    Organized by {event.organizerName || 'Event Organizer'}
                                                </p>
                                                <span className={`badge ${event.eventType === 'VIRTUAL' ? 'bg-info' : event.eventType === 'HYBRID' ? 'bg-warning' : 'bg-success'}`}>
                                                    {event.eventType === 'VIRTUAL' ? 'Virtual Event' : event.eventType === 'HYBRID' ? 'Hybrid Event' : 'In-Person Event'}
                                                </span>
                                                {event.category && (
                                                    <span className="badge bg-secondary ms-2">
                                                        {event.category.replace(/_/g, ' & ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}
                                                    </span>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Registration Form Card */}
                        <div className="card">
                            <div className="card-header bg-primary text-white">
                                <h3 className="mb-0">
                                    <i className="bi bi-clipboard-check me-2"></i>
                                    Complete Your Registration
                                </h3>
                            </div>
                            <div className="card-body">
                                {!registrationComplete ? (
                                    <RegistrationForm 
                                        eventId={id!} 
                                        instanceId={instanceId || ''} 
                                        onSuccess={(data) => {
                                            setRegistrationData(data);
                                            setRegistrationComplete(true);
                                        }}
                                    />
                                ) : (
                                    <div>
                                        <div className="alert alert-success">
                                            <h4>🎉 Registration Successful!</h4>
                                            <p>Welcome to {event.name}! Your registration has been confirmed.</p>
                                        </div>
                                        
                                        {registrationData && (
                                            <QRCodeDisplay
                                                eventId={id!}
                                                registrationId={registrationData.id}
                                                userName={registrationData.userName}
                                                userEmail={registrationData.userEmail}
                                            />
                                        )}
                                        
                                        <div className="text-center mt-4">
                                            <button 
                                                className="btn btn-outline-primary me-3"
                                                onClick={() => navigate(`/events/${id}`)}
                                            >
                                                ← Back to Event Details
                                            </button>
                                            <button 
                                                className="btn btn-secondary"
                                                onClick={() => {
                                                    setRegistrationComplete(false);
                                                    setRegistrationData(null);
                                                }}
                                            >
                                                Register Another Person
                                            </button>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default RegistrationPage;