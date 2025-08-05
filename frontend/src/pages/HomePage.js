import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../api/apiClient';

const HomePage = () => {
    const [events, setEvents] = useState([]);
    const [filters, setFilters] = useState({
        location: '',
        date_start: '',
        date_end: '',
        tags: ''
    });
    const [sort, setSort] = useState('name,asc');

    const fetchEvents = () => {
        const params = {
            sort: sort
        };
        if (filters.location) params.location = filters.location;
        if (filters.date_start) params.date_start = filters.date_start;
        if (filters.date_end) params.date_end = filters.date_end;
        if (filters.tags) params.tags = filters.tags.split(',').map(tag => tag.trim()).join(',');

        apiClient.get('/events', { params }).then(response => {
            setEvents(response.data);
        }).catch(error => {
            console.error("There was an error fetching the events!", error);
        });
    };

    useEffect(() => {
        fetchEvents();
    }, [sort]);

    const handleFilterChange = (e) => {
        const { name, value } = e.target;
        setFilters(prevFilters => ({
            ...prevFilters,
            [name]: value
        }));
    };

    const handleSearch = (e) => {
        e.preventDefault();
        fetchEvents();
    };

    return (
        <div className="container">
            <h1 className="mt-5">Events</h1>
            <form onSubmit={handleSearch} className="card mb-4">
                <div className="card-body">
                    <h5 className="card-title">Filter & Sort</h5>
                    <div className="row g-3 align-items-center">
                        <div className="col-md-3">
                            <input
                                type="text"
                                className="form-control"
                                placeholder="Location"
                                name="location"
                                value={filters.location}
                                onChange={handleFilterChange}
                            />
                        </div>
                        <div className="col-md-2">
                            <input
                                type="date"
                                className="form-control"
                                name="date_start"
                                value={filters.date_start}
                                onChange={handleFilterChange}
                            />
                        </div>
                        <div className="col-md-2">
                            <input
                                type="date"
                                className="form-control"
                                name="date_end"
                                value={filters.date_end}
                                onChange={handleFilterChange}
                            />
                        </div>
                        <div className="col-md-3">
                            <input
                                type="text"
                                className="form-control"
                                placeholder="Tags (comma-separated)"
                                name="tags"
                                value={filters.tags}
                                onChange={handleFilterChange}
                            />
                        </div>
                        <div className="col-md-2">
                            <button type="submit" className="btn btn-primary w-100">Search</button>
                        </div>
                    </div>
                </div>
            </form>

            <div className="row">
                {events.map(event => (
                    <div className="col-md-4" key={event.id}>
                        <div className="card mb-4 shadow-sm">
                            <img src={event.thumbnailImageUrl || 'https://via.placeholder.com/300x200'} className="card-img-top" alt={event.name} />
                            <div className="card-body">
                                <h5 className="card-title">{event.name}</h5>
                                <p className="card-text text-muted">
                                    {event.instances && event.instances.length > 0 ? 
                                        `${new Date(event.instances[0].dateTime).toLocaleDateString()} - ${event.instances[0].location}` : 'Date & Location TBD'}
                                </p>
                                <p className="card-text">{event.description ? event.description.substring(0, 100) + '...' : ''}</p>
                                <div className="d-flex justify-content-between align-items-center">
                                    <div className="btn-group">
                                        <Link to={`/events/${event.id}`} className="btn btn-sm btn-outline-secondary">View</Link>
                                    </div>
                                    <small className="text-muted">{event.tags ? event.tags.join(', ') : ''}</small>
                                </div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default HomePage;