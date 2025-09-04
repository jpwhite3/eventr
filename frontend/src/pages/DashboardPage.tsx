import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../hooks/useAuth';
import { Link } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import apiClient from '../api/apiClient';
import {
  faCalendarAlt,
  faTicketAlt,
  faMapMarkerAlt,
  faClock,
  faUsers,
  faEye,
  faPlus,
  faChartLine,
  faCalendarCheck,
  faTrophy,
  faSync
} from '@fortawesome/free-solid-svg-icons';
import './DashboardPage.css';

interface Event {
  id: string;
  title: string;
  description: string;
  startDateTime: string;
  endDateTime: string;
  location: string;
  totalCapacity: number;
  registeredCount: number;
  status: string;
  imageUrl?: string;
  registrationStatus?: 'REGISTERED' | 'WAITLISTED' | 'NOT_REGISTERED';
}

interface DashboardStats {
  totalEventsRegistered: number;
  upcomingEvents: number;
  completedEvents: number;
  totalHoursAttended: number;
}

const DashboardPage: React.FC = () => {
  const { user, isAuthenticated } = useAuth();
  const [upcomingEvents, setUpcomingEvents] = useState<Event[]>([]);
  const [recentEvents, setRecentEvents] = useState<Event[]>([]);
  const [stats, setStats] = useState<DashboardStats>({
    totalEventsRegistered: 0,
    upcomingEvents: 0,
    completedEvents: 0,
    totalHoursAttended: 0
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDashboardData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      
      if (!user?.id) {
        setError('User information not available. Please try refreshing the page.');
        return;
      }

      // Fetch user's registrations from the API
      const registrationsResponse = await apiClient.get(`/registrations/user/id/${user.id}`);
      const userRegistrations = registrationsResponse.data || [];

      // Transform registrations to events and separate upcoming vs recent
      const now = new Date();
      const transformedEvents: Event[] = userRegistrations.map((reg: any) => ({
        id: reg.eventInstance?.event?.id || reg.eventInstance?.id || reg.id,
        title: reg.eventInstance?.event?.name || reg.eventInstance?.event?.title || 'Event',
        description: reg.eventInstance?.event?.description || '',
        startDateTime: reg.eventInstance?.event?.startDateTime || reg.eventInstance?.startDateTime || new Date().toISOString(),
        endDateTime: reg.eventInstance?.event?.endDateTime || reg.eventInstance?.endDateTime || new Date().toISOString(),
        location: reg.eventInstance?.event?.location || 'TBD',
        totalCapacity: reg.eventInstance?.event?.maxCapacity || reg.eventInstance?.maxCapacity || 0,
        registeredCount: reg.eventInstance?.currentRegistrations || 0,
        status: reg.eventInstance?.event?.status || 'PUBLISHED',
        imageUrl: reg.eventInstance?.event?.imageUrl || null,
        registrationStatus: reg.status || 'REGISTERED'
      }));

      // Separate upcoming and recent events
      const upcomingEvents = transformedEvents.filter(event => 
        new Date(event.startDateTime) > now
      ).sort((a, b) => new Date(a.startDateTime).getTime() - new Date(b.startDateTime).getTime());

      const recentEvents = transformedEvents.filter(event => 
        new Date(event.startDateTime) <= now
      ).sort((a, b) => new Date(b.startDateTime).getTime() - new Date(a.startDateTime).getTime());

      // Calculate real statistics
      const totalEventsRegistered = transformedEvents.length;
      const upcomingEventsCount = upcomingEvents.length;
      const completedEventsCount = recentEvents.filter(event => 
        new Date(event.endDateTime) <= now
      ).length;

      // Calculate total hours attended (estimate based on event durations)
      const totalHoursAttended = recentEvents
        .filter(event => new Date(event.endDateTime) <= now)
        .reduce((total, event) => {
          const start = new Date(event.startDateTime);
          const end = new Date(event.endDateTime);
          const hours = (end.getTime() - start.getTime()) / (1000 * 60 * 60);
          return total + hours;
        }, 0);

      const realStats: DashboardStats = {
        totalEventsRegistered,
        upcomingEvents: upcomingEventsCount,
        completedEvents: completedEventsCount,
        totalHoursAttended: Math.round(totalHoursAttended)
      };

      setUpcomingEvents(upcomingEvents);
      setRecentEvents(recentEvents);
      setStats(realStats);
    } catch (error) {
      console.error('Error fetching dashboard data:', error);
      setError('Failed to load dashboard data. Please try refreshing the page.');
      
      // Fallback to empty data on error rather than mock data
      setUpcomingEvents([]);
      setRecentEvents([]);
      setStats({
        totalEventsRegistered: 0,
        upcomingEvents: 0,
        completedEvents: 0,
        totalHoursAttended: 0
      });
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    if (isAuthenticated && user) {
      fetchDashboardData();
    }
  }, [isAuthenticated, user, fetchDashboardData]);

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'REGISTERED':
        return 'success';
      case 'WAITLISTED':
        return 'warning';
      default:
        return 'secondary';
    }
  };

  if (!isAuthenticated) {
    return (
      <div className="dashboard-container">
        <div className="auth-required">
          <h2>Welcome to EventR</h2>
          <p>Please sign in to access your personal dashboard</p>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="dashboard-container">
        <div className="loading-state">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="mt-3">Loading your dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard-container">
      {/* Error Display */}
      {error && (
        <div className="alert alert-danger mb-4" role="alert">
          <h5 className="alert-heading">Unable to Load Dashboard</h5>
          <p className="mb-2">{error}</p>
          <button 
            className="btn btn-outline-danger btn-sm" 
            onClick={() => {
              setError(null);
              fetchDashboardData();
            }}
          >
            Try Again
          </button>
        </div>
      )}

      {/* Welcome Header */}
      <div className="welcome-section">
        <div className="welcome-content">
          <h1>Welcome back, {user?.firstName}!</h1>
          <p className="text-muted">
            Here's what's happening with your events
          </p>
        </div>
        <div className="quick-actions">
          <Link to="/events" className="btn btn-primary">
            <FontAwesomeIcon icon={faPlus} className="me-2" />
            Browse Events
          </Link>
          <button 
            className="btn btn-outline-secondary ms-2" 
            onClick={fetchDashboardData}
            disabled={loading}
          >
            <FontAwesomeIcon 
              icon={faSync} 
              className="me-2" 
              spin={loading}
            />
            Refresh
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon bg-primary">
            <FontAwesomeIcon icon={faTicketAlt} />
          </div>
          <div className="stat-content">
            <h3>{stats.totalEventsRegistered}</h3>
            <p>Events Registered</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon bg-success">
            <FontAwesomeIcon icon={faCalendarCheck} />
          </div>
          <div className="stat-content">
            <h3>{stats.upcomingEvents}</h3>
            <p>Upcoming Events</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon bg-info">
            <FontAwesomeIcon icon={faTrophy} />
          </div>
          <div className="stat-content">
            <h3>{stats.completedEvents}</h3>
            <p>Completed Events</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon bg-warning">
            <FontAwesomeIcon icon={faChartLine} />
          </div>
          <div className="stat-content">
            <h3>{stats.totalHoursAttended}</h3>
            <p>Hours Attended</p>
          </div>
        </div>
      </div>

      {/* Upcoming Events */}
      <div className="section">
        <div className="section-header">
          <h2>Upcoming Events</h2>
          <Link to="/events" className="view-all-link">
            View All Events
          </Link>
        </div>

        {upcomingEvents.length > 0 ? (
          <div className="events-grid">
            {upcomingEvents.map((event) => (
              <div key={event.id} className="event-card">
                {event.imageUrl && (
                  <div className="event-image">
                    <img src={event.imageUrl} alt={event.title} />
                  </div>
                )}
                <div className="event-content">
                  <div className="event-header">
                    <h4>{event.title}</h4>
                    {event.registrationStatus && (
                      <span className={`status-badge badge-${getStatusColor(event.registrationStatus)}`}>
                        {event.registrationStatus}
                      </span>
                    )}
                  </div>
                  
                  <p className="event-description">{event.description}</p>
                  
                  <div className="event-details">
                    <div className="detail-item">
                      <FontAwesomeIcon icon={faClock} className="text-muted" />
                      <span>{formatDate(event.startDateTime)}</span>
                    </div>
                    <div className="detail-item">
                      <FontAwesomeIcon icon={faMapMarkerAlt} className="text-muted" />
                      <span>{event.location}</span>
                    </div>
                    <div className="detail-item">
                      <FontAwesomeIcon icon={faUsers} className="text-muted" />
                      <span>{event.registeredCount} / {event.totalCapacity} registered</span>
                    </div>
                  </div>
                  
                  <div className="event-actions">
                    <Link 
                      to={`/events/${event.id}`} 
                      className="btn btn-outline-primary btn-sm"
                    >
                      <FontAwesomeIcon icon={faEye} className="me-1" />
                      View Details
                    </Link>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state">
            <FontAwesomeIcon icon={faCalendarAlt} size="3x" className="text-muted mb-3" />
            <h4>No Upcoming Events</h4>
            <p className="text-muted">You haven't registered for any upcoming events yet.</p>
            <Link to="/events" className="btn btn-primary">
              <FontAwesomeIcon icon={faPlus} className="me-2" />
              Browse Events
            </Link>
          </div>
        )}
      </div>

      {/* Recent Activity */}
      <div className="section">
        <div className="section-header">
          <h2>Recent Activity</h2>
        </div>

        {recentEvents.length > 0 ? (
          <div className="activity-list">
            {recentEvents.map((event) => (
              <div key={event.id} className="activity-item">
                <div className="activity-icon">
                  <FontAwesomeIcon icon={faCalendarCheck} className="text-success" />
                </div>
                <div className="activity-content">
                  <h5>{event.title}</h5>
                  <p className="text-muted">{formatDate(event.startDateTime)}</p>
                  <span className="badge bg-success">Completed</span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state">
            <p className="text-muted">No recent activity</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default DashboardPage;