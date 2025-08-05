import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import apiClient from '../api/apiClient';
import RegistrationForm from '../components/RegistrationForm';

const EventDetailsPage = () => {
    const { id } = useParams();
    const [event, setEvent] = useState(null);
    const [selectedInstance, setSelectedInstance] = useState('');

    useEffect(() => {
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
    }, [id]);

    if (!event) {
        return <div className="container mt-5">Loading...</div>;
    }

    return (
        <div className="container mt-5">
            <div className="row">
                <div className="col-12">
                    <img src={event.bannerImageUrl || 'https://via.placeholder.com/1200x400'} className="img-fluid rounded mb-4" alt={event.name} style={{ width: '100%', height: '400px', objectFit: 'cover' }} />
                </div>
            </div>
            <div className="row">
                <div className="col-md-8">
                    <div className="card">
                        <div className="card-body">
                            <h1 className="card-title">{event.name}</h1>
                            <div className="text-muted mb-3">
                                {event.tags && event.tags.map(tag => (
                                    <span key={tag} className="badge bg-secondary me-1">{tag}</span>
                                ))}
                            </div>
                            <ReactMarkdown>{event.description}</ReactMarkdown>
                        </div>
                    </div>
                </div>
                <div className="col-md-4">
                    <div className="card">
                        <div className="card-body">
                            <h2 className="card-title">Register</h2>
                            {event.instances && event.instances.length > 0 ? (
                                <>
                                    <div className="mb-3">
                                        <label htmlFor="instance-select" className="form-label">Select Date & Time</label>
                                        <select 
                                            id="instance-select" 
                                            className="form-select" 
                                            value={selectedInstance} 
                                            onChange={e => setSelectedInstance(e.target.value)}
                                        >
                                            {event.instances.map(instance => (
                                                <option key={instance.id} value={instance.id}>
                                                    {new Date(instance.dateTime).toLocaleString()} - {instance.location}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                    <RegistrationForm eventId={id} instanceId={selectedInstance} />
                                </>
                            ) : (
                                <p>No registration dates available.</p>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default EventDetailsPage;