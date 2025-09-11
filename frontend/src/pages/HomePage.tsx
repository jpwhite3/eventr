import React, { useState, useEffect, FormEvent, ChangeEvent, useCallback } from 'react';
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
    searchQuery: string;
    radius: number;
    latitude?: number;
    longitude?: number;
    sortBy: string;
    sortOrder: string;
}

const HomePage: React.FC = () => {
    const [events, setEvents] = useState<Event[]>([]);
    const [isUsingMockData, setIsUsingMockData] = useState(false);
    const [filters, setFilters] = useState<Filters>({
        city: '',
        category: '',
        eventType: '',
        tags: '',
        startDate: '',
        endDate: '',
        searchQuery: '',
        radius: 25,
        sortBy: 'startDateTime',
        sortOrder: 'asc'
    });
    const [selectedCity] = useState('Browse All Cities');
    const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
    const [userLocation, setUserLocation] = useState<{latitude: number, longitude: number} | null>(null);
    const [locationPermission, setLocationPermission] = useState<string>('prompt'); // 'granted', 'denied', 'prompt'

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

    // Get user location
    const requestLocation = useCallback(() => {
        if (!navigator.geolocation) {
            setLocationPermission('denied');
            return;
        }

        navigator.geolocation.getCurrentPosition(
            (position) => {
                const { latitude, longitude } = position.coords;
                setUserLocation({ latitude, longitude });
                setLocationPermission('granted');
                setFilters(prev => ({ ...prev, latitude, longitude }));
            },
            (error) => {
                console.error('Geolocation error:', error);
                setLocationPermission('denied');
            },
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 300000 }
        );
    }, []);

    const fetchEvents = useCallback(() => {
        const params: any = {};
        
        // Basic filters
        if (filters.city) params.city = filters.city;
        if (filters.category && filters.category !== 'All') {
            params.category = filters.category.toUpperCase().replace(/\s+/g, '_').replace('&', '');
        }
        if (filters.eventType) params.eventType = filters.eventType;
        if (filters.tags) params.tags = filters.tags.split(',').map(tag => tag.trim()).join(',');
        if (filters.startDate) params.startDate = filters.startDate;
        if (filters.endDate) params.endDate = filters.endDate;
        
        // Search query
        if (filters.searchQuery) params.q = filters.searchQuery;
        
        // Location-based search
        if (filters.latitude && filters.longitude) {
            params.latitude = filters.latitude;
            params.longitude = filters.longitude;
            params.radius = filters.radius;
        }
        
        // Sorting
        if (filters.sortBy) params.sortBy = filters.sortBy;
        if (filters.sortOrder) params.sortOrder = filters.sortOrder;

        apiClient.get('/events', { params }).then(response => {
            let eventData = response.data;
            
            // Client-side sorting if needed
            eventData = sortEvents(eventData, filters.sortBy, filters.sortOrder);
            
            setEvents(eventData);
            setIsUsingMockData(false);
        }).catch(error => {
            console.error("There was an error fetching the events!", error);
            
            // Fallback to mock data when API is unavailable
            console.warn("API unavailable, using mock data for development");
            const mockEvents: Event[] = [
                {
                    id: '1',
                    name: 'Sample Corporate Event',
                    description: 'This is a sample event displayed when the backend API is not available. The application is running in development mode with mock data.',
                    startDateTime: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
                    endDateTime: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000 + 4 * 60 * 60 * 1000).toISOString(),
                    city: 'San Francisco',
                    state: 'CA',
                    category: 'BUSINESS',
                    eventType: 'IN_PERSON',
                    venueName: 'Convention Center',
                    organizerName: 'Sample Organizer',
                    maxRegistrations: 100,
                    requiresApproval: false,
                    tags: ['networking', 'business', 'development']
                },
                {
                    id: '2',
                    name: 'Tech Workshop',
                    description: 'Another sample event. Start the backend server to see real events.',
                    startDateTime: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString(),
                    endDateTime: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000 + 6 * 60 * 60 * 1000).toISOString(),
                    city: 'New York',
                    state: 'NY',
                    category: 'TECHNOLOGY',
                    eventType: 'HYBRID',
                    venueName: 'Tech Hub',
                    organizerName: 'Tech Team',
                    maxRegistrations: 50,
                    requiresApproval: true,
                    tags: ['workshop', 'technology', 'learning']
                }
            ];
            
            const sortedMockEvents = sortEvents(mockEvents, filters.sortBy, filters.sortOrder);
            setEvents(sortedMockEvents);
            setIsUsingMockData(true);
        });
    }, [filters]);

    const sortEvents = (eventList: Event[], sortBy: string, sortOrder: string) => {
        return [...eventList].sort((a, b) => {
            let aValue, bValue;
            
            switch (sortBy) {
                case 'name':
                    aValue = a.name.toLowerCase();
                    bValue = b.name.toLowerCase();
                    break;
                case 'startDateTime':
                    aValue = a.startDateTime ? new Date(a.startDateTime).getTime() : 0;
                    bValue = b.startDateTime ? new Date(b.startDateTime).getTime() : 0;
                    break;
                case 'city':
                    aValue = a.city?.toLowerCase() || '';
                    bValue = b.city?.toLowerCase() || '';
                    break;
                case 'category':
                    aValue = a.category?.toLowerCase() || '';
                    bValue = b.category?.toLowerCase() || '';
                    break;
                default:
                    return 0;
            }
            
            if (sortOrder === 'desc') {
                return aValue > bValue ? -1 : aValue < bValue ? 1 : 0;
            } else {
                return aValue < bValue ? -1 : aValue > bValue ? 1 : 0;
            }
        });
    };

    useEffect(() => {
        fetchEvents();
    }, [fetchEvents]);

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
            {/* Development Mode Banner */}
            {isUsingMockData && (
                <div className="bg-warning text-dark py-2">
                    <div className="container">
                        <div className="text-center">
                            <small>
                                <strong>⚠️ Development Mode:</strong> Backend API unavailable. Showing sample data. 
                                Start the backend server to see real events.
                            </small>
                        </div>
                    </div>
                </div>
            )}
            
            {/* Header Section */}
            <div className="bg-primary text-white homepage-hero">
                <div className="container">
                    <div className="row align-items-center">
                        <div className="col-md-8">
                            <h1 className="display-4 fw-bold mb-3">Corporate Events</h1>
                            <p className="lead">Browse and register for company events</p>
                        </div>
                        <div className="col-md-4">
                            <div className="bg-white p-4 rounded shadow text-dark">
                                <form onSubmit={handleSearch}>
                                    <div className="mb-3">
                                        <input
                                            type="text"
                                            className="form-control form-control-lg"
                                            placeholder="Search events..."
                                            name="searchQuery"
                                            value={filters.searchQuery}
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

                {/* Advanced Filters */}
                <div className="row mb-4">
                    <div className="col-md-12">
                        <div className="card">
                            <div className="card-header d-flex justify-content-between align-items-center">
                                <h6 className="mb-0">🔍 Search Filters</h6>
                                <button 
                                    type="button" 
                                    className="btn btn-outline-secondary btn-sm"
                                    onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
                                >
                                    {showAdvancedFilters ? '↑ Less Filters' : '↓ More Filters'}
                                </button>
                            </div>
                            <div className="card-body">
                                {/* Basic Filters - Always Visible */}
                                <div className="row g-3 mb-3">
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
                                        <button type="button" onClick={fetchEvents} className="btn btn-primary w-100">
                                            🔍 Apply Filters
                                        </button>
                                    </div>
                                </div>

                                {/* Advanced Filters - Collapsible */}
                                {showAdvancedFilters && (
                                    <div className="border-top pt-3">
                                        <div className="row g-3 mb-3">
                                            <div className="col-md-6">
                                                <label className="form-label">Tags (comma-separated)</label>
                                                <input
                                                    type="text"
                                                    className="form-control"
                                                    name="tags"
                                                    placeholder="e.g., networking, workshop, conference"
                                                    value={filters.tags}
                                                    onChange={handleFilterChange}
                                                />
                                            </div>
                                            <div className="col-md-3">
                                                <label className="form-label">Sort By</label>
                                                <select className="form-select" name="sortBy" value={filters.sortBy} onChange={handleFilterChange}>
                                                    <option value="startDateTime">Date</option>
                                                    <option value="name">Name</option>
                                                    <option value="city">City</option>
                                                    <option value="category">Category</option>
                                                </select>
                                            </div>
                                            <div className="col-md-3">
                                                <label className="form-label">Sort Order</label>
                                                <select className="form-select" name="sortOrder" value={filters.sortOrder} onChange={handleFilterChange}>
                                                    <option value="asc">Ascending</option>
                                                    <option value="desc">Descending</option>
                                                </select>
                                            </div>
                                        </div>

                                        {/* Location-based Search */}
                                        <div className="row g-3">
                                            <div className="col-md-6">
                                                <label className="form-label d-flex align-items-center gap-2">
                                                    📍 Location-based Search
                                                    {locationPermission === 'granted' && (
                                                        <span className="badge bg-success">🟢 Location enabled</span>
                                                    )}
                                                </label>
                                                {locationPermission === 'prompt' && (
                                                    <button 
                                                        type="button" 
                                                        className="btn btn-outline-primary btn-sm mb-2"
                                                        onClick={requestLocation}
                                                    >
                                                        📍 Enable Location
                                                    </button>
                                                )}
                                                {locationPermission === 'granted' && userLocation && (
                                                    <div className="small text-success">
                                                        📍 Using your location ({userLocation.latitude.toFixed(2)}, {userLocation.longitude.toFixed(2)})
                                                    </div>
                                                )}
                                                {locationPermission === 'denied' && (
                                                    <div className="small text-muted">
                                                        Location access denied. Use city filter instead.
                                                    </div>
                                                )}
                                            </div>
                                            {locationPermission === 'granted' && (
                                                <div className="col-md-6">
                                                    <label className="form-label">Search Radius (miles)</label>
                                                    <div className="d-flex align-items-center gap-2">
                                                        <input
                                                            type="range"
                                                            className="form-range flex-grow-1"
                                                            name="radius"
                                                            min="1"
                                                            max="100"
                                                            value={filters.radius}
                                                            onChange={handleFilterChange}
                                                        />
                                                        <span className="badge bg-secondary">{filters.radius} mi</span>
                                                    </div>
                                                </div>
                                            )}
                                        </div>

                                        <div className="d-flex gap-2 mt-3">
                                            <button 
                                                type="button" 
                                                className="btn btn-outline-danger btn-sm"
                                                onClick={() => {
                                                    setFilters({
                                                        city: '',
                                                        category: '',
                                                        eventType: '',
                                                        tags: '',
                                                        startDate: '',
                                                        endDate: '',
                                                        searchQuery: '',
                                                        radius: 25,
                                                        sortBy: 'startDateTime',
                                                        sortOrder: 'asc'
                                                    });
                                                }}
                                            >
                                                🗑️ Clear All Filters
                                            </button>
                                        </div>
                                    </div>
                                )}
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

                <div className="row g-4 event-grid">
                    {events.map(event => (
                        <div className="col-12 col-md-6 col-lg-4" key={event.id}>
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
                                            <span className="btn btn-primary btn-sm w-100">
                                                View Details
                                            </span>
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
                                    endDate: '',
                                    searchQuery: '',
                                    radius: 25,
                                    sortBy: 'date',
                                    sortOrder: 'asc'
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