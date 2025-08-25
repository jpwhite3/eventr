import React, { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import { Link } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
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
  faTrophy
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

  useEffect(() => {
    if (isAuthenticated) {
      fetchDashboardData();
    }
  }, [isAuthenticated]);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      
      // Mock data for demonstration
      // In a real application, these would be API calls
      const mockUpcomingEvents: Event[] = [
        {
          id: '1',
          title: 'React Conference 2024',
          description: 'Learn the latest React features and best practices',
          startDateTime: '2024-03-15T09:00:00',
          endDateTime: '2024-03-15T17:00:00',
          location: 'San Francisco Convention Center',
          totalCapacity: 500,
          registeredCount: 387,
          status: 'PUBLISHED',
          imageUrl: 'https://via.placeholder.com/400x200?text=React+Conf',
          registrationStatus: 'REGISTERED'
        },
        {
          id: '2',
          title: 'AI/ML Workshop',
          description: 'Hands-on workshop on machine learning fundamentals',
          startDateTime: '2024-03-20T10:00:00',
          endDateTime: '2024-03-20T16:00:00',
          location: 'Tech Hub Downtown',
          totalCapacity: 50,
          registeredCount: 42,
          status: 'PUBLISHED',
          imageUrl: 'https://via.placeholder.com/400x200?text=AI+Workshop',
          registrationStatus: 'REGISTERED'
        }
      ];

      const mockRecentEvents: Event[] = [
        {
          id: '3',
          title: 'Web Development Bootcamp',
          description: 'Intensive 3-day bootcamp covering full-stack development',
          startDateTime: '2024-02-10T09:00:00',
          endDateTime: '2024-02-12T17:00:00',
          location: 'Innovation Center',
          totalCapacity: 30,
          registeredCount: 28,
          status: 'COMPLETED',
          imageUrl: 'https://via.placeholder.com/400x200?text=Bootcamp',
          registrationStatus: 'REGISTERED'
        }
      ];

      const mockStats: DashboardStats = {
        totalEventsRegistered: 8,
        upcomingEvents: 2,
        completedEvents: 5,
        totalHoursAttended: 42
      };

      setUpcomingEvents(mockUpcomingEvents);
      setRecentEvents(mockRecentEvents);
      setStats(mockStats);
    } catch (error) {
      console.error('Error fetching dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

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
            Discover Events
          </Link>
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
              Discover Events
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