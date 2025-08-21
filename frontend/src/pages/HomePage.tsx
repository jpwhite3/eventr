import React, { useState, useEffect, FormEvent, ChangeEvent } from 'react';
import { Link } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import apiClient from '../api/apiClient';

interface EventInstance {
    dateTime: string;
    location: string;
}

interface Event {
    id: string;
    name: string;
    description?: string;
    thumbnailImageUrl?: string;
    bannerImageUrl?: string;
    instances?: EventInstance[];
    tags?: string[];
    eventType?: string;
    category?: string;
    venueName?: string;
    city?: string;
    state?: string;
    startDateTime?: string;
    endDateTime?: string;
    requiresApproval?: boolean;
    maxRegistrations?: number;
    organizerName?: string;
}

interface Filters {
    city: string;
    category: string;
    eventType: string;
    tags: string;
    startDate: string;
    endDate: string;
}

const HomePage: React.FC = () => {
    const [events, setEvents] = useState<Event[]>([]);
    const [filters, setFilters] = useState<Filters>({
        city: '',
        category: '',
        eventType: '',
        tags: '',
        startDate: '',
        endDate: ''
    });
    const [selectedCity] = useState('Browse All Cities');

    const categories = [
        { name: 'All', icon: '🎯' },
        { name: 'Business', icon: '💼' },
        { name: 'Technology', icon: '💻' },
        { name: 'Education', icon: '📚' },
        { name: 'Community', icon: '👥' },
        { name: 'Health & Wellness', icon: '🏥' },
        { name: 'Food & Drink', icon: '🍽️' },
        { name: 'Sports & Fitness', icon: '🏃' },
        { name: 'Other', icon: '📋' }
    ];

    const fetchEvents = () => {
        const params: any = {};
        if (filters.city) params.city = filters.city;
        if (filters.category && filters.category !== 'All') params.category = filters.category.toUpperCase().replace(/\s+/g, '_').replace('&', '');
        if (filters.eventType) params.eventType = filters.eventType;
        if (filters.tags) params.tags = filters.tags.split(',').map(tag => tag.trim()).join(',');
        if (filters.startDate) params.startDate = filters.startDate;
        if (filters.endDate) params.endDate = filters.endDate;

        apiClient.get('/events', { params }).then(response => {
            setEvents(response.data);
        }).catch(error => {
            console.error("There was an error fetching the events!", error);
        });
    };

    useEffect(() => {
        fetchEvents();
    }, []);

    const handleFilterChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFilters(prevFilters => ({
            ...prevFilters,
            [name]: value
        }));
    };

    const handleSearch = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        fetchEvents();
    };

    const formatEventDate = (dateString?: string) => {
        if (!dateString) return 'Date TBD';
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', { 
            weekday: 'short',
            month: 'short', 
            day: 'numeric',
            hour: 'numeric',
            minute: '2-digit'
        });
    };

    const getRegistrationStatus = (event: Event) => {
        if (event.requiresApproval) return 'Approval Required';
        if (event.maxRegistrations && event.maxRegistrations > 0) return `${event.maxRegistrations} spots available`;
        return 'Open Registration';
    };

    return (
        <div className="min-vh-100 bg-light">
            {/* Header Section */}
            <div className="bg-primary text-white py-5">
                <div className="container">
                    <div className="row align-items-center">
                        <div className="col-md-8">
                            <h1 className="display-4 fw-bold mb-3">Corporate Events</h1>
                            <p className="lead">Discover and register for company events</p>
                        </div>
                        <div className="col-md-4">
                            <div className="bg-white p-4 rounded shadow">
                                <form onSubmit={handleSearch}>
                                    <div className="mb-3">
                                        <input
                                            type="text"
                                            className="form-control form-control-lg"
                                            placeholder="Search events..."
                                            name="tags"
                                            value={filters.tags}
                                            onChange={handleFilterChange}
                                        />
                                    </div>
                                    <div className="mb-3">
                                        <select
                                            className="form-select"
                                            name="city"
                                            value={filters.city}
                                            onChange={handleFilterChange}
                                        >
                                            <option value="">{selectedCity}</option>
                                            <option value="New York">New York</option>
                                            <option value="Los Angeles">Los Angeles</option>
                                            <option value="Chicago">Chicago</option>
                                            <option value="Houston">Houston</option>
                                            <option value="Phoenix">Phoenix</option>
                                            <option value="Philadelphia">Philadelphia</option>
                                            <option value="San Antonio">San Antonio</option>
                                            <option value="San Diego">San Diego</option>
                                            <option value="Dallas">Dallas</option>
                                            <option value="San Jose">San Jose</option>
                                        </select>
                                    </div>
                                    <button type="submit" className="btn btn-primary btn-lg w-100">
                                        Search Events
                                    </button>
                                </form>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="container py-5">
                {/* Category Filter */}
                <div className="mb-5">
                    <h2 className="mb-4">Browse by category</h2>
                    <div className="row g-3">
                        {categories.map((category, index) => (
                            <div className="col-6 col-md-4 col-lg-2" key={index}>
                                <button
                                    className={`btn w-100 p-3 border-0 ${filters.category === category.name ? 'btn-primary' : 'btn-light'}`}
                                    onClick={() => {
                                        setFilters(prev => ({ ...prev, category: category.name }));
                                        setTimeout(fetchEvents, 100);
                                    }}
                                    style={{ minHeight: '100px' }}
                                >
                                    <div className="d-flex flex-column align-items-center">
                                        <span style={{ fontSize: '2rem' }}>{category.icon}</span>
                                        <small className="mt-2 fw-medium">{category.name}</small>
                                    </div>
                                </button>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Additional Filters */}
                <div className="row mb-4">
                    <div className="col-md-12">
                        <div className="card">
                            <div className="card-body">
                                <div className="row g-3">
                                    <div className="col-md-3">
                                        <label className="form-label">Event Type</label>
                                        <select className="form-select" name="eventType" value={filters.eventType} onChange={handleFilterChange}>
                                            <option value="">All Events</option>
                                            <option value="IN_PERSON">In-Person</option>
                                            <option value="VIRTUAL">Virtual</option>
                                            <option value="HYBRID">Hybrid</option>
                                        </select>
                                    </div>
                                    <div className="col-md-3">
                                        <label className="form-label">Start Date</label>
                                        <input
                                            type="date"
                                            className="form-control"
                                            name="startDate"
                                            value={filters.startDate}
                                            onChange={handleFilterChange}
                                        />
                                    </div>
                                    <div className="col-md-3">
                                        <label className="form-label">End Date</label>
                                        <input
                                            type="date"
                                            className="form-control"
                                            name="endDate"
                                            value={filters.endDate}
                                            onChange={handleFilterChange}
                                        />
                                    </div>
                                    <div className="col-md-3 d-flex align-items-end">
                                        <button type="button" onClick={fetchEvents} className="btn btn-outline-primary w-100">
                                            Apply Filters
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Events Grid */}
                <div className="d-flex justify-content-between align-items-center mb-4">
                    <h2>Events {filters.city && `in ${filters.city}`}</h2>
                    <div className="text-muted">
                        {events.length} event{events.length !== 1 ? 's' : ''} found
                    </div>
                </div>

                <div className="row g-4">
                    {events.map(event => (
                        <div className="col-md-6 col-lg-4" key={event.id}>
                            <Link to={`/events/${event.id}`} className="text-decoration-none">
                                <div className="card h-100 shadow-sm hover-shadow transition-all">
                                    <div className="position-relative">
                                        <img 
                                            src={event.thumbnailImageUrl || event.bannerImageUrl || 'https://via.placeholder.com/400x300'} 
                                            className="card-img-top" 
                                            alt={event.name}
                                            style={{ height: '200px', objectFit: 'cover' }}
                                        />
                                        <div className="position-absolute top-0 end-0 m-2">
                                            <span className={`badge ${event.eventType === 'VIRTUAL' ? 'bg-info' : event.eventType === 'HYBRID' ? 'bg-warning' : 'bg-success'}`}>
                                                {event.eventType === 'VIRTUAL' ? 'Virtual' : event.eventType === 'HYBRID' ? 'Hybrid' : 'In-Person'}
                                            </span>
                                        </div>
                                    </div>
                                    <div className="card-body d-flex flex-column">
                                        <div className="mb-2">
                                            <small className="text-danger fw-bold">
                                                {formatEventDate(event.startDateTime)}
                                            </small>
                                        </div>
                                        <h5 className="card-title text-dark">{event.name}</h5>
                                        <p className="card-text text-muted small mb-2">
                                            {event.venueName && event.city ? 
                                                `${event.venueName} • ${event.city}, ${event.state || ''}`.trim().replace(/,$/, '') :
                                                event.city ? 
                                                    `${event.city}, ${event.state || ''}`.trim().replace(/,$/, '') :
                                                    'Location TBD'
                                            }
                                        </p>
                                        {event.description && (
                                            <div className="card-text text-muted small mb-2" style={{ 
                                                maxHeight: '60px', 
                                                overflow: 'hidden',
                                                display: '-webkit-box',
                                                WebkitLineClamp: 3,
                                                WebkitBoxOrient: 'vertical'
                                            }}>
                                                <ReactMarkdown>
                                                    {event.description.length > 120 
                                                        ? event.description.substring(0, 120) + '...' 
                                                        : event.description
                                                    }
                                                </ReactMarkdown>
                                            </div>
                                        )}
                                        <div className="mt-auto">
                                            <div className="d-flex justify-content-between align-items-center mb-2">
                                                <span className="badge bg-success">
                                                    {getRegistrationStatus(event)}
                                                </span>
                                                {event.organizerName && (
                                                    <small className="text-muted">
                                                        by {event.organizerName}
                                                    </small>
                                                )}
                                            </div>
                                            <button className="btn btn-primary btn-sm w-100">
                                                Register Now
                                            </button>
                                        </div>
                                        {event.tags && event.tags.length > 0 && (
                                            <div className="mt-2">
                                                {event.tags.slice(0, 3).map((tag, index) => (
                                                    <span key={index} className="badge bg-light text-dark me-1 small">
                                                        {tag}
                                                    </span>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </Link>
                        </div>
                    ))}
                </div>

                {events.length === 0 && (
                    <div className="text-center py-5">
                        <div className="mb-3">
                            <span style={{ fontSize: '4rem' }}>🔍</span>
                        </div>
                        <h3>No events found</h3>
                        <p className="text-muted">Try adjusting your search criteria or browse all events.</p>
                        <button 
                            className="btn btn-primary"
                            onClick={() => {
                                setFilters({
                                    city: '',
                                    category: '',
                                    eventType: '',
                                    tags: '',
                                    startDate: '',
                                    endDate: ''
                                });
                                fetchEvents();
                            }}
                        >
                            Show All Events
                        </button>
                    </div>
                )}
            </div>

            <style>{`
                .hover-shadow:hover {
                    box-shadow: 0 .5rem 1rem rgba(0,0,0,.15) !important;
                    transform: translateY(-2px);
                }
                .transition-all {
                    transition: all 0.2s ease-in-out;
                }
                .card:hover {
                    text-decoration: none;
                }
            `}</style>
        </div>
    );
};

export default HomePage;