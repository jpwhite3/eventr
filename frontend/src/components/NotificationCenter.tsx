import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../hooks/useAuth';
import webSocketService from '../services/WebSocketService';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faBell,
  faTimes,
  faCheck,
  faExclamationCircle,
  faInfoCircle,
  faTrash,
  faClock
} from '@fortawesome/free-solid-svg-icons';

export interface Notification {
  id: string;
  type: 'info' | 'success' | 'warning' | 'error';
  title: string;
  message: string;
  timestamp: number;
  read: boolean;
  eventId?: string;
  eventName?: string;
  autoHide?: boolean;
  persistent?: boolean;
}

interface NotificationCenterProps {
  className?: string;
  maxNotifications?: number;
  position?: 'top-right' | 'top-left' | 'bottom-right' | 'bottom-left';
  showBadge?: boolean;
}

const NotificationCenter: React.FC<NotificationCenterProps> = ({
  className = '',
  maxNotifications = 10,
  position = 'top-right',
  showBadge = true
}) => {
  const { user, isAuthenticated } = useAuth();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const [isConnected, setIsConnected] = useState(false);

  // Update connection status
  useEffect(() => {
    const updateConnectionStatus = () => {
      setIsConnected(webSocketService.getConnectionStatus());
    };

    updateConnectionStatus();
    const interval = setInterval(updateConnectionStatus, 1000);
    return () => clearInterval(interval);
  }, []);

  const addNotification = useCallback((notification: Omit<Notification, 'id' | 'timestamp' | 'read'> & { timestamp?: number }) => {
    const newNotification: Notification = {
      ...notification,
      id: `${Date.now()}-${Math.random()}`,
      timestamp: notification.timestamp || Date.now(),
      read: false
    };

    setNotifications(prev => {
      const updated = [newNotification, ...prev].slice(0, maxNotifications);
      
      // Auto-hide notifications after 5 seconds if specified
      if (notification.autoHide) {
        setTimeout(() => {
          setNotifications(current => current.filter(n => n.id !== newNotification.id));
        }, 5000);
      }
      
      return updated;
    });
  }, [maxNotifications]);

  // Set up WebSocket subscriptions for notifications
  useEffect(() => {
    if (!isAuthenticated || !user?.id) return;

    const subscriptions = [
      // User-specific notifications
      webSocketService.subscribeToUserNotifications(user.id, (message) => {
        addNotification({
          type: (message.type as 'info' | 'success' | 'warning' | 'error') || 'info',
          title: message.title || 'Personal Notification',
          message: message.message || 'You have a new notification',
          eventId: message.eventId,
          eventName: message.eventName,
          persistent: true,
          timestamp: message.timestamp
        });
      }),

      // Registration updates
      webSocketService.subscribeToAllRegistrations((message) => {
        if (message.registrationData?.userId === user.id) {
          const type = message.registrationType === 'NEW' ? 'success' : 
                      message.registrationType === 'CANCELLED' ? 'warning' : 'info';
          
          addNotification({
            type,
            title: 'Registration Update',
            message: `Registration ${message.registrationType.toLowerCase()} for ${message.registrationData?.eventName || 'event'}`,
            eventId: message.eventId,
            eventName: message.registrationData?.eventName,
            autoHide: type === 'success'
          });
        }
      }),

      // Event status changes for user's registered events
      webSocketService.subscribeToAllEvents((message) => {
        // This would need to check if user is registered for this event
        // For now, we'll show all major event updates
        if (message.updateType === 'STATUS_CHANGE' || message.updateType === 'TIME_CHANGED') {
          addNotification({
            type: message.newStatus === 'CANCELLED' ? 'error' : 'warning',
            title: 'Event Update',
            message: message.updateType === 'STATUS_CHANGE' 
              ? `Event status changed to ${message.newStatus}`
              : 'Event time has been updated',
            eventId: message.eventId,
            persistent: true
          });
        }
      }),

      // System announcements
      webSocketService.subscribeToSystemAnnouncements((message) => {
        addNotification({
          type: 'info',
          title: 'System Announcement',
          message: message.message || 'New system announcement',
          persistent: true
        });
      })
    ];

    return () => {
      subscriptions.forEach(sub => sub.unsubscribe());
    };
  }, [isAuthenticated, user?.id, addNotification]);

  const removeNotification = (id: string) => {
    setNotifications(prev => prev.filter(n => n.id !== id));
  };

  const markAsRead = (id: string) => {
    setNotifications(prev => prev.map(n => 
      n.id === id ? { ...n, read: true } : n
    ));
  };

  const markAllAsRead = () => {
    setNotifications(prev => prev.map(n => ({ ...n, read: true })));
  };

  const clearAll = () => {
    setNotifications([]);
  };

  const clearReadNotifications = () => {
    setNotifications(prev => prev.filter(n => !n.read));
  };

  const unreadCount = notifications.filter(n => !n.read).length;

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'success':
        return faCheck;
      case 'warning':
        return faExclamationCircle;
      case 'error':
        return faExclamationCircle;
      default:
        return faInfoCircle;
    }
  };

  const getNotificationColor = (type: string) => {
    switch (type) {
      case 'success':
        return 'text-success';
      case 'warning':
        return 'text-warning';
      case 'error':
        return 'text-danger';
      default:
        return 'text-info';
    }
  };

  const formatTimestamp = (timestamp: number) => {
    const now = Date.now();
    const diff = now - timestamp;
    const minutes = Math.floor(diff / (1000 * 60));
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    return `${days}d ago`;
  };

  return (
    <div className={`notification-center ${className}`}>
      {/* Notification Bell Button */}
      <div className="position-relative">
        <button
          className="btn btn-outline-secondary position-relative"
          onClick={() => setIsOpen(!isOpen)}
          title="Notifications"
          aria-label="Notifications"
        >
          <FontAwesomeIcon icon={faBell} />
          {showBadge && unreadCount > 0 && (
            <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger">
              {unreadCount > 99 ? '99+' : unreadCount}
            </span>
          )}
        </button>

        {/* Connection Status Indicator */}
        <div className="position-absolute top-0 end-0 translate-middle-x">
          <span
            className={`badge ${isConnected ? 'bg-success' : 'bg-secondary'}`}
            style={{ fontSize: '0.6rem', padding: '2px 4px' }}
          >
            {isConnected ? '●' : '○'}
          </span>
        </div>
      </div>

      {/* Notification Dropdown */}
      {isOpen && (
        <div className={`notification-dropdown position-absolute ${position.includes('right') ? 'end-0' : 'start-0'} mt-2`}
             style={{ 
               width: '350px',
               maxHeight: '500px',
               zIndex: 1050,
               top: position.includes('top') ? '100%' : 'auto',
               bottom: position.includes('bottom') ? '100%' : 'auto'
             }}>
          <div className="card shadow-lg border-0">
            <div className="card-header d-flex justify-content-between align-items-center bg-light">
              <div>
                <h6 className="mb-0">Notifications</h6>
                <small className="text-muted">
                  {isConnected ? '🟢 Live updates' : '⚫ Offline'}
                </small>
              </div>
              <div className="d-flex gap-2">
                {notifications.length > 0 && (
                  <>
                    <button
                      className="btn btn-sm btn-outline-primary"
                      onClick={markAllAsRead}
                      title="Mark all as read"
                    >
                      <FontAwesomeIcon icon={faCheck} size="sm" />
                    </button>
                    <button
                      className="btn btn-sm btn-outline-secondary"
                      onClick={clearReadNotifications}
                      title="Clear read notifications"
                    >
                      <FontAwesomeIcon icon={faTrash} size="sm" />
                    </button>
                  </>
                )}
                <button
                  className="btn btn-sm btn-outline-secondary"
                  onClick={() => setIsOpen(false)}
                  title="Close"
                >
                  <FontAwesomeIcon icon={faTimes} size="sm" />
                </button>
              </div>
            </div>

            <div className="card-body p-0">
              {notifications.length === 0 ? (
                <div className="text-center py-4 text-muted">
                  <FontAwesomeIcon icon={faBell} size="2x" className="mb-2" />
                  <p>No notifications</p>
                </div>
              ) : (
                <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
                  {notifications.map((notification) => (
                    <div
                      key={notification.id}
                      className={`notification-item p-3 border-bottom ${!notification.read ? 'bg-light' : ''} position-relative`}
                      style={{ cursor: 'pointer' }}
                      onClick={() => markAsRead(notification.id)}
                    >
                      <div className="d-flex align-items-start">
                        <div className={`me-2 ${getNotificationColor(notification.type)}`}>
                          <FontAwesomeIcon 
                            icon={getNotificationIcon(notification.type)} 
                            size="sm" 
                          />
                        </div>
                        <div className="flex-grow-1">
                          <div className="d-flex justify-content-between align-items-start">
                            <h6 className="mb-1">{notification.title}</h6>
                            <button
                              className="btn btn-sm btn-link text-muted p-0"
                              onClick={(e) => {
                                e.stopPropagation();
                                removeNotification(notification.id);
                              }}
                              title="Remove"
                            >
                              <FontAwesomeIcon icon={faTimes} size="xs" />
                            </button>
                          </div>
                          <p className="mb-1 small">{notification.message}</p>
                          {notification.eventName && (
                            <small className="text-primary">📅 {notification.eventName}</small>
                          )}
                          <div className="d-flex align-items-center justify-content-between mt-1">
                            <small className="text-muted d-flex align-items-center">
                              <FontAwesomeIcon icon={faClock} size="xs" className="me-1" />
                              {formatTimestamp(notification.timestamp)}
                            </small>
                            {!notification.read && (
                              <span className="badge bg-primary rounded-pill" style={{ width: '8px', height: '8px', padding: 0 }}></span>
                            )}
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {notifications.length > 0 && (
              <div className="card-footer bg-light text-center">
                <button
                  className="btn btn-sm btn-link text-muted"
                  onClick={clearAll}
                >
                  Clear all notifications
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationCenter;