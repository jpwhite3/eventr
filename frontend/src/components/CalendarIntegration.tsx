import React, { useState, useEffect } from 'react';
import { format, formatInTimeZone } from 'date-fns-tz';
import apiClient from '../api/apiClient';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { 
  faCalendarPlus, 
  faDownload, 
  faLink, 
  faCopy,
  faExternalLinkAlt,
  faCheck,
  faSpinner
} from '@fortawesome/free-solid-svg-icons';

interface CalendarEvent {
  id: string;
  name: string;
  description?: string;
  startDateTime: string;
  endDateTime: string;
  location?: string;
  organizerEmail?: string;
}

interface CalendarIntegrationProps {
  event: CalendarEvent;
  userId?: string;
  options?: ('google' | 'outlook' | 'ics' | 'subscribe' | 'apple')[];
  userTimezone?: string;
  className?: string;
}

interface CalendarSubscription {
  url: string;
  token: string;
  expiresAt?: string;
}

const CalendarIntegration: React.FC<CalendarIntegrationProps> = ({
  event,
  userId,
  options = ['google', 'outlook', 'ics', 'subscribe'],
  userTimezone = 'UTC',
  className = ''
}) => {
  const [showOptions, setShowOptions] = useState(false);
  const [subscription, setSubscription] = useState<CalendarSubscription | null>(null);
  const [copied, setCopied] = useState<string | null>(null);
  const [loading, setLoading] = useState<string | null>(null);
  const [userTimeZone, setUserTimeZone] = useState<string>(userTimezone);

  useEffect(() => {
    // Auto-detect user timezone if not provided
    if (!userTimezone || userTimezone === 'UTC') {
      const detectedTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
      setUserTimeZone(detectedTimezone);
    }
  }, [userTimezone]);

  useEffect(() => {
    // Load user's calendar subscription if available
    if (userId && options.includes('subscribe')) {
      loadCalendarSubscription();
    }
  }, [userId, options]);

  const loadCalendarSubscription = async () => {
    if (!userId) return;
    
    try {
      const response = await apiClient.get(`/calendar/user/${userId}/subscription`);
      setSubscription(response.data);
    } catch (error) {
      console.log('No existing calendar subscription found');
    }
  };

  const generateSubscriptionUrl = async () => {
    if (!userId) {
      alert('Please log in to create calendar subscription');
      return;
    }

    setLoading('subscribe');
    try {
      const response = await apiClient.post(`/calendar/user/${userId}/subscription`);
      setSubscription(response.data);
    } catch (error) {
      console.error('Failed to create calendar subscription:', error);
      alert('Failed to create calendar subscription');
    } finally {
      setLoading(null);
    }
  };

  const formatEventTime = (dateTime: string) => {
    try {
      return formatInTimeZone(new Date(dateTime), userTimeZone, 'PPP pp');
    } catch {
      return new Date(dateTime).toLocaleString();
    }
  };

  const generateGoogleCalendarUrl = () => {
    const startDate = new Date(event.startDateTime);
    const endDate = new Date(event.endDateTime);
    
    const formatGoogleDate = (date: Date) => {
      return date.toISOString().replace(/[-:]/g, '').split('.')[0] + 'Z';
    };

    const params = new URLSearchParams({
      action: 'TEMPLATE',
      text: event.name,
      dates: `${formatGoogleDate(startDate)}/${formatGoogleDate(endDate)}`,
      details: event.description || `Event: ${event.name}`,
      location: event.location || '',
      trp: 'false'
    });

    return `https://calendar.google.com/calendar/render?${params.toString()}`;
  };

  const generateOutlookCalendarUrl = () => {
    const startDate = new Date(event.startDateTime);
    const endDate = new Date(event.endDateTime);

    const params = new URLSearchParams({
      subject: event.name,
      startdt: startDate.toISOString(),
      enddt: endDate.toISOString(),
      body: event.description || `Event: ${event.name}`,
      location: event.location || ''
    });

    return `https://outlook.live.com/calendar/0/deeplink/compose?${params.toString()}`;
  };

  const downloadICSFile = async () => {
    setLoading('ics');
    try {
      const calendarUrl = `/api/calendar/event/${event.id}.ics`;
      
      // Create a temporary link element and trigger download
      const link = document.createElement('a');
      link.href = `${process.env.REACT_APP_API_URL || 'http://localhost:8080'}${calendarUrl}`;
      link.download = `event-${event.name?.replace(/[^a-z0-9]/gi, '_').toLowerCase()}.ics`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (error) {
      console.error('Failed to download ICS file:', error);
      alert('Failed to download calendar file');
    } finally {
      setLoading(null);
    }
  };

  const copyToClipboard = async (text: string, type: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(type);
      setTimeout(() => setCopied(null), 2000);
    } catch (error) {
      console.error('Failed to copy to clipboard:', error);
    }
  };

  const handleCalendarAction = async (action: string) => {
    switch (action) {
      case 'google':
        window.open(generateGoogleCalendarUrl(), '_blank');
        break;
      case 'outlook':
        window.open(generateOutlookCalendarUrl(), '_blank');
        break;
      case 'ics':
        await downloadICSFile();
        break;
      case 'subscribe':
        if (!subscription) {
          await generateSubscriptionUrl();
        }
        break;
      case 'apple':
        // Apple Calendar uses ICS files
        await downloadICSFile();
        break;
    }
  };

  const getActionIcon = (action: string) => {
    switch (action) {
      case 'google':
        return '📅';
      case 'outlook':
        return '📆';
      case 'ics':
        return '📄';
      case 'subscribe':
        return '🔗';
      case 'apple':
        return '🍎';
      default:
        return '📅';
    }
  };

  const getActionLabel = (action: string) => {
    switch (action) {
      case 'google':
        return 'Google Calendar';
      case 'outlook':
        return 'Outlook';
      case 'ics':
        return 'Download ICS';
      case 'subscribe':
        return 'Calendar Feed';
      case 'apple':
        return 'Apple Calendar';
      default:
        return action;
    }
  };

  const getActionDescription = (action: string) => {
    switch (action) {
      case 'google':
        return 'Add directly to Google Calendar';
      case 'outlook':
        return 'Add to Outlook/Office 365';
      case 'ics':
        return 'Download calendar file';
      case 'subscribe':
        return 'Subscribe to all your events';
      case 'apple':
        return 'Add to Apple Calendar';
      default:
        return '';
    }
  };

  return (
    <div className={`calendar-integration ${className}`}>
      {/* Simple Add to Calendar Button */}
      {!showOptions && (
        <button
          className="btn btn-outline-primary btn-sm w-100"
          onClick={() => setShowOptions(true)}
        >
          <FontAwesomeIcon icon={faCalendarPlus} className="me-2" />
          📅 Add to Calendar
        </button>
      )}

      {/* Advanced Calendar Options */}
      {showOptions && (
        <div className="calendar-options-panel">
          <div className="card">
            <div className="card-header d-flex justify-content-between align-items-center">
              <div>
                <h6 className="mb-0">📅 Add to Calendar</h6>
                <small className="text-muted">{event.name}</small>
              </div>
              <button
                className="btn btn-sm btn-outline-secondary"
                onClick={() => setShowOptions(false)}
              >
                ✕
              </button>
            </div>
            
            <div className="card-body">
              {/* Event Time Display */}
              <div className="mb-3 p-2 bg-light rounded">
                <div className="d-flex align-items-center mb-1">
                  <strong>🕒 Time:</strong>
                  <span className="ms-2">{formatEventTime(event.startDateTime)}</span>
                </div>
                {event.location && (
                  <div className="d-flex align-items-center">
                    <strong>📍 Location:</strong>
                    <span className="ms-2">{event.location}</span>
                  </div>
                )}
                <div className="mt-1">
                  <small className="text-muted">
                    📍 Timezone: {userTimeZone}
                  </small>
                </div>
              </div>

              {/* Calendar Action Buttons */}
              <div className="d-grid gap-2">
                {options.map((option) => (
                  <button
                    key={option}
                    className="btn btn-outline-primary d-flex align-items-center justify-content-between"
                    onClick={() => handleCalendarAction(option)}
                    disabled={loading === option}
                  >
                    <div className="d-flex align-items-center">
                      <span className="me-2">{getActionIcon(option)}</span>
                      <div className="text-start">
                        <div className="fw-semibold">{getActionLabel(option)}</div>
                        <small className="text-muted">{getActionDescription(option)}</small>
                      </div>
                    </div>
                    {loading === option ? (
                      <FontAwesomeIcon icon={faSpinner} spin />
                    ) : (
                      <FontAwesomeIcon icon={faExternalLinkAlt} className="text-muted" />
                    )}
                  </button>
                ))}
              </div>

              {/* Calendar Subscription Section */}
              {options.includes('subscribe') && subscription && (
                <div className="mt-3 p-3 border rounded">
                  <h6 className="d-flex align-items-center mb-2">
                    <FontAwesomeIcon icon={faLink} className="me-2 text-primary" />
                    Personal Calendar Feed
                  </h6>
                  <p className="small text-muted mb-2">
                    Subscribe to this URL in your calendar app to automatically sync all your registered events:
                  </p>
                  <div className="input-group input-group-sm mb-2">
                    <input
                      type="text"
                      className="form-control font-monospace small"
                      value={subscription.url}
                      readOnly
                    />
                    <button
                      className="btn btn-outline-secondary"
                      type="button"
                      onClick={() => copyToClipboard(subscription.url, 'subscription')}
                    >
                      {copied === 'subscription' ? (
                        <FontAwesomeIcon icon={faCheck} className="text-success" />
                      ) : (
                        <FontAwesomeIcon icon={faCopy} />
                      )}
                    </button>
                  </div>
                  <div className="small text-muted">
                    💡 Copy this URL and add it as a calendar subscription in Google Calendar, 
                    Outlook, or Apple Calendar
                  </div>
                  {subscription.expiresAt && (
                    <div className="small text-warning mt-1">
                      ⏰ Expires: {new Date(subscription.expiresAt).toLocaleDateString()}
                    </div>
                  )}
                </div>
              )}

              {/* Instructions */}
              <div className="mt-3 p-2 bg-info bg-opacity-10 rounded">
                <h6 className="text-info mb-1">💡 Quick Tips</h6>
                <ul className="small mb-0 text-muted">
                  <li><strong>Google/Outlook:</strong> Opens in your browser for direct adding</li>
                  <li><strong>ICS Download:</strong> Save file and open with any calendar app</li>
                  <li><strong>Calendar Feed:</strong> Auto-syncs all your registered events</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      )}

      <style>{`
        .calendar-integration {
          position: relative;
        }

        .calendar-options-panel {
          position: absolute;
          top: 100%;
          left: 0;
          right: 0;
          z-index: 1050;
          margin-top: 8px;
        }

        .calendar-options-panel .card {
          box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
          border: 1px solid #dee2e6;
        }

        .font-monospace {
          font-family: 'Courier New', monospace;
          font-size: 0.875rem;
        }

        @media (max-width: 768px) {
          .calendar-options-panel {
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            width: 90vw;
            max-width: 400px;
            z-index: 1060;
          }

          .calendar-options-panel::before {
            content: '';
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.5);
            z-index: -1;
          }
        }
      `}</style>
    </div>
  );
};

export default CalendarIntegration;