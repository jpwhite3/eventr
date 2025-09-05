import React, { useState, useEffect, useCallback } from 'react';
import apiClient from '../api/apiClient';
import QRScanner from './QRScanner';
import './MobileCheckInInterface.css';

interface MobileCheckInInterfaceProps {
    eventId?: string;
    sessionId?: string;
    staffMember: string;
    onCheckInSuccess?: (checkIn: any) => void;
    onCheckInError?: (error: string) => void;
}

interface CheckInData {
    id: string;
    userName: string;
    userEmail: string;
    checkedInAt: string;
    method: string;
    sessionTitle?: string;
    userPhoto?: string;
}

interface CheckInStats {
    totalCheckedIn: number;
    totalRegistrations: number;
    checkInRate: number;
    recentCheckIns?: CheckInData[];
}

const MobileCheckInInterface: React.FC<MobileCheckInInterfaceProps> = ({
    eventId,
    sessionId,
    staffMember,
    onCheckInSuccess,
    onCheckInError
}) => {
    const [isScanning, setIsScanning] = useState(false);
    const [recentCheckIns, setRecentCheckIns] = useState<CheckInData[]>([]);
    const [stats, setStats] = useState<CheckInStats>({
        totalCheckedIn: 0,
        totalRegistrations: 0,
        checkInRate: 0
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState<any[]>([]);
    const [showManualSearch, setShowManualSearch] = useState(false);
    const [lastCheckIn, setLastCheckIn] = useState<CheckInData | null>(null);
    const [offlineMode, setOfflineMode] = useState(!navigator.onLine);
    const [offlineQueue, setOfflineQueue] = useState<any[]>([]);

    // Haptic feedback function
    const hapticFeedback = useCallback((type: 'success' | 'error' | 'warning' = 'success') => {
        if ('vibrate' in navigator) {
            switch (type) {
                case 'success':
                    navigator.vibrate([50, 50, 100]); // Success pattern
                    break;
                case 'error':
                    navigator.vibrate([100, 50, 100, 50, 100]); // Error pattern
                    break;
                case 'warning':
                    navigator.vibrate([50, 100, 50]); // Warning pattern
                    break;
            }
        }
    }, []);

    // Play audio feedback
    const audioFeedback = useCallback((type: 'success' | 'error' = 'success') => {
        try {
            const audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
            const oscillator = audioContext.createOscillator();
            const gainNode = audioContext.createGain();
            
            oscillator.connect(gainNode);
            gainNode.connect(audioContext.destination);
            
            oscillator.frequency.value = type === 'success' ? 800 : 400;
            oscillator.type = 'sine';
            
            gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
            gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.3);
            
            oscillator.start(audioContext.currentTime);
            oscillator.stop(audioContext.currentTime + 0.3);
        } catch (error) {
            console.log('Audio feedback not available');
        }
    }, []);

    // Check online status and manage offline queue
    useEffect(() => {
        const handleOnline = () => {
            setOfflineMode(false);
            // Auto-sync when coming back online
            if (offlineQueue.length > 0) {
                syncOfflineQueue();
            }
        };
        const handleOffline = () => setOfflineMode(true);
        
        window.addEventListener('online', handleOnline);
        window.addEventListener('offline', handleOffline);
        
        // Load offline queue on mount
        const savedQueue = localStorage.getItem('mobile_checkin_offline_queue');
        if (savedQueue) {
            try {
                const queue = JSON.parse(savedQueue);
                setOfflineQueue(queue);
            } catch (error) {
                console.error('Failed to load offline queue:', error);
                localStorage.removeItem('mobile_checkin_offline_queue');
            }
        }
        
        return () => {
            window.removeEventListener('online', handleOnline);
            window.removeEventListener('offline', handleOffline);
        };
    }, [offlineQueue]);

    useEffect(() => {
        if (eventId) {
            loadStats();
            loadRecentCheckIns();
            
            // Refresh stats every 30 seconds
            const interval = setInterval(() => {
                loadStats();
                loadRecentCheckIns();
            }, 30000);
            
            return () => clearInterval(interval);
        }
    }, [eventId]);

    const loadStats = async () => {
        if (!eventId) return;

        try {
            const response = await apiClient.get(`/checkin/event/${eventId}/stats`);
            setStats(response.data);
        } catch (error) {
            console.error('Failed to load stats:', error);
        }
    };

    const loadRecentCheckIns = async () => {
        if (!eventId) return;

        try {
            const endpoint = sessionId 
                ? `/checkin/session/${sessionId}/attendance`
                : `/checkin/event/${eventId}/recent`;
            
            const response = await apiClient.get(endpoint);
            const checkIns = sessionId ? response.data : response.data.recentCheckIns || [];
            setRecentCheckIns(checkIns);
        } catch (error) {
            console.error('Failed to load recent check-ins:', error);
        }
    };

    // Offline queue management functions
    const addToOfflineQueue = (checkInData: any) => {
        const queueItem = {
            ...checkInData,
            timestamp: new Date().toISOString(),
            id: `offline_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
        };
        
        const newQueue = [...offlineQueue, queueItem];
        setOfflineQueue(newQueue);
        localStorage.setItem('mobile_checkin_offline_queue', JSON.stringify(newQueue));
        
        // Show offline confirmation
        setSuccess(`📱 Check-in queued offline. Will sync when online.`);
        hapticFeedback('warning');
        
        setTimeout(() => setSuccess(null), 4000);
    };

    const syncOfflineQueue = async () => {
        if (offlineQueue.length === 0 || !navigator.onLine) return;
        
        setLoading(true);
        let syncedCount = 0;
        const failedItems: any[] = [];
        
        for (const item of offlineQueue) {
            try {
                if (item.type === 'qr') {
                    await apiClient.post('/checkin/qr', item.data);
                } else if (item.type === 'manual') {
                    await apiClient.post('/checkin/manual', item.data);
                }
                syncedCount++;
            } catch (error) {
                console.error('Failed to sync offline check-in:', error);
                failedItems.push(item);
            }
        }
        
        // Update queue with only failed items
        setOfflineQueue(failedItems);
        localStorage.setItem('mobile_checkin_offline_queue', JSON.stringify(failedItems));
        
        if (syncedCount > 0) {
            setSuccess(`✅ Synced ${syncedCount} offline check-ins!`);
            hapticFeedback('success');
            // Refresh stats after sync
            loadStats();
            loadRecentCheckIns();
            setTimeout(() => setSuccess(null), 3000);
        }
        
        if (failedItems.length > 0) {
            setError(`⚠️ ${failedItems.length} check-ins failed to sync. Will retry later.`);
            setTimeout(() => setError(null), 5000);
        }
        
        setLoading(false);
    };

    const clearOfflineQueue = () => {
        setOfflineQueue([]);
        localStorage.removeItem('mobile_checkin_offline_queue');
    };

    const handleQRScan = async (qrCode: string) => {
        setLoading(true);
        setError(null);
        setSuccess(null);

        const checkInData = {
            qrCode: qrCode,
            checkedInBy: staffMember,
            deviceId: navigator.userAgent,
            deviceName: 'Mobile Device',
            location: sessionId ? 'Session Check-in' : 'Event Check-in',
            timestamp: new Date().toISOString()
        };

        try {

            const response = await apiClient.post('/checkin/qr', checkInData);
            const checkIn = response.data;

            // Success feedback
            hapticFeedback('success');
            audioFeedback('success');
            
            setSuccess(`✅ ${checkIn.userName} checked in successfully!`);
            setLastCheckIn(checkIn);
            setRecentCheckIns(prev => [checkIn, ...prev.slice(0, 9)]);
            
            // Update stats
            setStats(prev => ({
                ...prev,
                totalCheckedIn: prev.totalCheckedIn + 1,
                checkInRate: ((prev.totalCheckedIn + 1) / prev.totalRegistrations) * 100
            }));

            onCheckInSuccess?.(checkIn);

            // Auto-hide success message after 3 seconds
            setTimeout(() => {
                setSuccess(null);
                setLastCheckIn(null);
            }, 3000);

        } catch (error: any) {
            // Error feedback
            hapticFeedback('error');
            audioFeedback('error');
            
            const errorMessage = error.response?.data?.message || 'Check-in failed';
            
            // If offline, add to queue instead of showing error
            if (!navigator.onLine) {
                const offlineCheckInData = {
                    type: 'qr',
                    data: checkInData,
                    userName: 'QR Code User', // We don't have the name yet
                    userEmail: 'qr@checkin.com'
                };
                addToOfflineQueue(offlineCheckInData);
            } else {
                setError(errorMessage);
                onCheckInError?.(errorMessage);
                setTimeout(() => setError(null), 5000);
            }
        } finally {
            setLoading(false);
        }
    };

    const searchAttendees = async (query: string) => {
        if (!query.trim() || !eventId) {
            setSearchResults([]);
            return;
        }

        try {
            const response = await apiClient.get(`/events/${eventId}/registrations/search?q=${encodeURIComponent(query)}`);
            setSearchResults(response.data || []);
        } catch (error) {
            console.error('Search failed:', error);
            setSearchResults([]);
        }
    };

    const handleManualCheckIn = async (registration: any) => {
        setLoading(true);
        setError(null);

        const checkInData = {
            registrationId: registration.id,
            sessionId: sessionId || null,
            type: sessionId ? 'SESSION' : 'EVENT',
            method: 'MANUAL',
            checkedInBy: staffMember,
            timestamp: new Date().toISOString()
        };

        try {

            const response = await apiClient.post('/checkin/manual', checkInData);
            const checkIn = response.data;

            // Success feedback
            hapticFeedback('success');
            audioFeedback('success');

            setSuccess(`✅ ${checkIn.userName} checked in manually!`);
            setLastCheckIn(checkIn);
            setRecentCheckIns(prev => [checkIn, ...prev.slice(0, 9)]);
            
            // Clear search
            setSearchQuery('');
            setSearchResults([]);
            setShowManualSearch(false);

            // Update stats
            setStats(prev => ({
                ...prev,
                totalCheckedIn: prev.totalCheckedIn + 1,
                checkInRate: ((prev.totalCheckedIn + 1) / prev.totalRegistrations) * 100
            }));

            onCheckInSuccess?.(checkIn);

            setTimeout(() => {
                setSuccess(null);
                setLastCheckIn(null);
            }, 3000);

        } catch (error: any) {
            hapticFeedback('error');
            audioFeedback('error');
            
            const errorMessage = error.response?.data?.message || 'Manual check-in failed';
            
            // If offline, add to queue instead of showing error
            if (!navigator.onLine) {
                const offlineCheckInData = {
                    type: 'manual',
                    data: checkInData,
                    userName: registration?.userName || 'Manual User',
                    userEmail: registration?.userEmail || 'manual@checkin.com'
                };
                addToOfflineQueue(offlineCheckInData);
                
                // Clear search UI even when offline
                setSearchQuery('');
                setSearchResults([]);
                setShowManualSearch(false);
            } else {
                setError(errorMessage);
                onCheckInError?.(errorMessage);
                setTimeout(() => setError(null), 5000);
            }
        } finally {
            setLoading(false);
        }
    };

    const toggleScanner = () => {
        setIsScanning(!isScanning);
        setError(null);
        setSuccess(null);
        if (!isScanning) {
            hapticFeedback('warning');
        }
    };

    const formatTime = (dateString: string) => {
        return new Date(dateString).toLocaleTimeString('en-US', {
            hour: 'numeric',
            minute: '2-digit',
            hour12: true
        });
    };

    return (
        <div className="mobile-checkin-interface">
            {/* Offline Indicator */}
            {offlineMode && (
                <div className="offline-banner">
                    📡 Offline Mode - Check-ins will sync when connected
                    {offlineQueue.length > 0 && (
                        <div className="offline-queue-info">
                            📱 {offlineQueue.length} check-ins queued
                            <button 
                                className="offline-clear-btn"
                                onClick={clearOfflineQueue}
                                title="Clear offline queue"
                            >
                                ✖️
                            </button>
                        </div>
                    )}
                </div>
            )}

            {/* Online sync indicator */}
            {!offlineMode && offlineQueue.length > 0 && (
                <div className="sync-banner">
                    🔄 Syncing {offlineQueue.length} offline check-ins...
                    <button 
                        className="sync-now-btn"
                        onClick={syncOfflineQueue}
                        disabled={loading}
                    >
                        Sync Now
                    </button>
                </div>
            )}

            {/* Header with Enhanced Stats */}
            <div className="mobile-stats-header">
                <div className="stats-row">
                    <div className="stat-item primary">
                        <div className="stat-number">{stats.totalCheckedIn}</div>
                        <div className="stat-label">Checked In</div>
                    </div>
                    <div className="stat-item info">
                        <div className="stat-number">{stats.totalRegistrations - stats.totalCheckedIn}</div>
                        <div className="stat-label">Remaining</div>
                    </div>
                    <div className="stat-item success">
                        <div className="stat-number">{stats.checkInRate.toFixed(0)}%</div>
                        <div className="stat-label">Rate</div>
                    </div>
                </div>
                
                <div className="progress-bar">
                    <div 
                        className="progress-fill" 
                        style={{ width: `${stats.checkInRate}%` }}
                    ></div>
                </div>
                
                <div className="staff-info">
                    👤 {staffMember} • {sessionId ? 'Session' : 'Event'} Check-in
                </div>
            </div>

            {/* Status Messages */}
            {error && (
                <div className="alert alert-error">
                    ❌ {error}
                </div>
            )}
            
            {success && (
                <div className="alert alert-success">
                    {success}
                </div>
            )}

            {/* Last Check-in Display */}
            {lastCheckIn && (
                <div className="last-checkin-card">
                    <div className="checkin-avatar">
                        {lastCheckIn.userPhoto ? (
                            <img src={lastCheckIn.userPhoto} alt={lastCheckIn.userName} />
                        ) : (
                            <div className="avatar-placeholder">
                                {lastCheckIn.userName.charAt(0).toUpperCase()}
                            </div>
                        )}
                    </div>
                    <div className="checkin-details">
                        <div className="checkin-name">{lastCheckIn.userName}</div>
                        <div className="checkin-email">{lastCheckIn.userEmail}</div>
                        <div className="checkin-time">✅ {formatTime(lastCheckIn.checkedInAt)}</div>
                    </div>
                </div>
            )}

            {/* Main Action Buttons */}
            <div className="action-buttons">
                <button 
                    className={`scan-button ${isScanning ? 'active' : ''}`}
                    onClick={toggleScanner}
                    disabled={loading}
                >
                    {isScanning ? (
                        <>
                            <span className="button-icon">⏹️</span>
                            <span className="button-text">Stop Scanner</span>
                        </>
                    ) : (
                        <>
                            <span className="button-icon">📷</span>
                            <span className="button-text">Start QR Scanner</span>
                        </>
                    )}
                </button>

                <button 
                    className={`manual-button ${showManualSearch ? 'active' : ''}`}
                    onClick={() => setShowManualSearch(!showManualSearch)}
                    disabled={loading}
                >
                    <span className="button-icon">🔍</span>
                    <span className="button-text">Manual Check-in</span>
                </button>
            </div>

            {/* QR Scanner */}
            {isScanning && (
                <div className="scanner-container">
                    <QRScanner 
                        onScan={handleQRScan}
                        isActive={isScanning}
                        eventId={eventId}
                        onError={(error) => {
                            setError(error);
                            hapticFeedback('error');
                        }}
                    />
                    <div className="scanner-instructions">
                        📱 Point camera at QR code
                    </div>
                </div>
            )}

            {/* Manual Search */}
            {showManualSearch && (
                <div className="manual-search-container">
                    <div className="search-input-container">
                        <input 
                            type="text"
                            className="search-input"
                            placeholder="Search by name or email..."
                            value={searchQuery}
                            onChange={(e) => {
                                setSearchQuery(e.target.value);
                                searchAttendees(e.target.value);
                            }}
                        />
                    </div>

                    {searchResults.length > 0 && (
                        <div className="search-results">
                            {searchResults.map((result, index) => (
                                <div key={index} className="search-result-item">
                                    <div className="result-info">
                                        <div className="result-name">{result.userName}</div>
                                        <div className="result-email">{result.userEmail}</div>
                                        <div className="result-status">
                                            {result.checkedIn ? '✅ Checked In' : '⏳ Not Checked In'}
                                        </div>
                                    </div>
                                    <button 
                                        className="checkin-result-button"
                                        onClick={() => handleManualCheckIn(result)}
                                        disabled={result.checkedIn || loading}
                                    >
                                        {result.checkedIn ? 'Done' : 'Check In'}
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}

            {/* Recent Check-ins */}
            <div className="recent-checkins">
                <div className="section-header">
                    <h6>Recent Check-ins</h6>
                    <span className="refresh-indicator">
                        🔄 Auto-refresh
                    </span>
                </div>
                
                {recentCheckIns.length === 0 ? (
                    <div className="empty-state">
                        <div className="empty-icon">📋</div>
                        <div>No check-ins yet</div>
                    </div>
                ) : (
                    <div className="checkin-list">
                        {recentCheckIns.slice(0, 5).map((checkIn, index) => (
                            <div key={checkIn.id} className="checkin-item">
                                <div className="checkin-avatar-small">
                                    {checkIn.userName.charAt(0).toUpperCase()}
                                </div>
                                <div className="checkin-info">
                                    <div className="checkin-name-small">{checkIn.userName}</div>
                                    <div className="checkin-time-small">{formatTime(checkIn.checkedInAt)}</div>
                                </div>
                                <div className="checkin-method">
                                    {checkIn.method === 'QR' ? '📷' : '✏️'}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* Loading Overlay */}
            {loading && (
                <div className="loading-overlay">
                    <div className="loading-spinner"></div>
                    <div>Processing...</div>
                </div>
            )}
        </div>
    );
};

export default MobileCheckInInterface;