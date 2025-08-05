import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../api/apiClient';

// Interface for event data
interface Event {
    id: string;
    name: string;
    status: 'DRAFT' | 'PUBLISHED';
}

const AdminDashboard: React.FC = () => {
    const [events, setEvents] = useState<Event[]>([]);

    const fetchEvents = (): void => {
        apiClient.get('/events', { params: { publishedOnly: false } })
            .then(response => {
                setEvents(response.data);
            })
            .catch(error => console.error("Failed to fetch events", error));
    };

    useEffect(() => {
        fetchEvents();
    }, []);

    const handlePublish = (eventId: string): void => {
        apiClient.post(`/events/${eventId}/publish`)
            .then(() => fetchEvents())
            .catch(error => console.error("Failed to publish event", error));
    };

    const handleClone = (eventId: string): void => {
        apiClient.post(`/events/${eventId}/clone`)
            .then(() => fetchEvents())
            .catch(error => console.error("Failed to clone event", error));
    };

    const handleDelete = (eventId: string): void => {
        if (window.confirm('Are you sure you want to delete this event?')) {
            apiClient.delete(`/events/${eventId}`)
                .then(() => fetchEvents())
                .catch(error => console.error("Failed to delete event", error));
        }
    };

    return (
        <div className="container">
            <h1 className="mt-5">Admin Dashboard</h1>
            <Link to="/admin/event/new" className="btn btn-primary mb-3">Create Event</Link>
            <table className="table">
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {events.map(event => (
                        <tr key={event.id}>
                            <td>{event.name}</td>
                            <td>{event.status}</td>
                            <td>
                                <Link to={`/admin/event/${event.id}/edit`} className="btn btn-secondary btn-sm me-2">Edit</Link>
                                <button onClick={() => handlePublish(event.id)} className="btn btn-info btn-sm me-2" disabled={event.status === 'PUBLISHED'}>Publish</button>
                                <button onClick={() => handleClone(event.id)} className="btn btn-success btn-sm me-2">Clone</button>
                                <Link to={`/events/${event.id}/attendance`} className="btn btn-warning btn-sm me-2">Attendance</Link>
                                <button onClick={() => handleDelete(event.id)} className="btn btn-danger btn-sm">Delete</button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default AdminDashboard;