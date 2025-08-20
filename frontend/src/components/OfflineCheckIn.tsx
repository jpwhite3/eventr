import React, { useState, useEffect, useCallback } from 'react';
import apiClient from '../api/apiClient';

interface OfflineCheckInProps {
    eventId: string;
    sessionId?: string;
    staffMember: string;
}

interface OfflineCheckIn {
    id: string;
    registrationId: string;
    sessionId?: string;
    type: 'EVENT' | 'SESSION';
    method: 'QR_CODE' | 'MANUAL' | 'BULK';
    checkedInAt: string;
    checkedInBy?: string;
    deviceId?: string;
    qrCodeUsed?: string;
    notes?: string;
    needsSync: boolean;
}

const OfflineCheckIn: React.FC<OfflineCheckInProps> = ({ 
    eventId, 
    sessionId, 
    staffMember 
}) => {
    const [isOnline, setIsOnline] = useState(navigator.onLine);
    const [offlineQueue, setOfflineQueue] = useState<OfflineCheckIn[]>([]);
    const [syncing, setSyncing] = useState(false);
    const [syncStatus, setSyncStatus] = useState<string | null>(null);

    // Load offline queue from localStorage on mount
    useEffect(() => {
        const savedQueue = localStorage.getItem(`offlineCheckIns_${eventId}`);
        if (savedQueue) {
            try {
                setOfflineQueue(JSON.parse(savedQueue));
            } catch (error) {
                console.error('Failed to parse offline check-ins:', error);
            }
        }
    }, [eventId]);

    // Save queue to localStorage whenever it changes
    useEffect(() => {
        localStorage.setItem(`offlineCheckIns_${eventId}`, JSON.stringify(offlineQueue));
    }, [offlineQueue, eventId]);

    // Monitor online status
    useEffect(() => {
        const handleOnline = () => {
            setIsOnline(true);
            // Auto-sync when coming back online
            if (offlineQueue.length > 0) {
                syncOfflineCheckIns();
            }
        };

        const handleOffline = () => {
            setIsOnline(false);
        };

        window.addEventListener('online', handleOnline);
        window.addEventListener('offline', handleOffline);

        return () => {
            window.removeEventListener('online', handleOnline);
            window.removeEventListener('offline', handleOffline);
        };
    }, [offlineQueue]);

    const addToOfflineQueue = useCallback((checkInData: Omit<OfflineCheckIn, 'id' | 'needsSync'>) => {
        const offlineCheckIn: OfflineCheckIn = {
            ...checkInData,
            id: `offline_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
            needsSync: true
        };

        setOfflineQueue(prev => [...prev, offlineCheckIn]);
        setSyncStatus(`Added to offline queue. ${offlineQueue.length + 1} check-ins pending sync.`);
    }, [offlineQueue.length]);

    const syncOfflineCheckIns = async () => {
        if (offlineQueue.length === 0 || syncing) return;

        setSyncing(true);
        setSyncStatus('Syncing offline check-ins...');

        try {
            const response = await apiClient.post('/checkin/sync', offlineQueue);
            
            if (response.data && Array.isArray(response.data)) {
                const syncedCount = response.data.length;
                setOfflineQueue([]); // Clear queue after successful sync
                setSyncStatus(`✅ Successfully synced ${syncedCount} check-ins!`);
                
                // Clear success message after 5 seconds
                setTimeout(() => setSyncStatus(null), 5000);
            }
        } catch (error: any) {
            console.error('Sync failed:', error);
            setSyncStatus(`❌ Sync failed: ${error.response?.data?.message || error.message}`);
            
            // Clear error message after 10 seconds
            setTimeout(() => setSyncStatus(null), 10000);
        } finally {
            setSyncing(false);
        }
    };

    const handleOfflineCheckIn = (checkInData: {
        registrationId: string;
        userEmail: string;
        userName: string;
        method: 'QR_CODE' | 'MANUAL';
        qrCodeUsed?: string;
        notes?: string;
    }) => {
        const offlineCheckIn = {
            registrationId: checkInData.registrationId,
            sessionId: sessionId,
            type: (sessionId ? 'SESSION' : 'EVENT') as 'EVENT' | 'SESSION',
            method: checkInData.method,
            checkedInAt: new Date().toISOString(),
            checkedInBy: staffMember,
            deviceId: navigator.userAgent,
            qrCodeUsed: checkInData.qrCodeUsed,
            notes: checkInData.notes
        };

        addToOfflineQueue(offlineCheckIn);
    };

    const removeFromQueue = (id: string) => {
        setOfflineQueue(prev => prev.filter(item => item.id !== id));
    };

    const clearQueue = () => {
        if (window.confirm('Are you sure you want to clear all offline check-ins? This cannot be undone.')) {
            setOfflineQueue([]);
            setSyncStatus('Offline queue cleared.');
            setTimeout(() => setSyncStatus(null), 3000);
        }
    };

    const exportOfflineData = () => {
        if (offlineQueue.length === 0) {
            alert('No offline data to export');
            return;
        }

        const csvContent = [
            'ID,Registration ID,Session ID,Type,Method,Checked In At,Checked In By,Notes',
            ...offlineQueue.map(item => [
                item.id,
                item.registrationId,
                item.sessionId || '',
                item.type,
                item.method,
                item.checkedInAt,
                item.checkedInBy || '',
                item.notes || ''
            ].join(','))
        ].join('\n');

        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', `offline-checkins-${eventId}-${Date.now()}.csv`);
        link.click();
    };

    return (
        <div className="offline-checkin-manager">
            {/* Connection Status */}
            <div className={`alert ${isOnline ? 'alert-success' : 'alert-warning'} d-flex align-items-center`}>
                <div className="flex-grow-1">
                    <strong>
                        {isOnline ? '🟢 Online' : '🔴 Offline'} - 
                    </strong>
                    {isOnline 
                        ? ' Check-ins will be processed immediately'
                        : ' Check-ins will be queued for later sync'
                    }
                    {offlineQueue.length > 0 && (
                        <span className="ms-2">
                            <span className="badge bg-warning text-dark">
                                {offlineQueue.length} pending sync
                            </span>
                        </span>
                    )}
                </div>
                {isOnline && offlineQueue.length > 0 && (
                    <button 
                        className="btn btn-sm btn-outline-primary"
                        onClick={syncOfflineCheckIns}
                        disabled={syncing}
                    >
                        {syncing ? '⏳ Syncing...' : '🔄 Sync Now'}
                    </button>
                )}
            </div>

            {/* Sync Status */}
            {syncStatus && (
                <div className={`alert ${syncStatus.includes('✅') ? 'alert-success' : syncStatus.includes('❌') ? 'alert-danger' : 'alert-info'}`}>
                    {syncStatus}
                </div>
            )}

            {/* Offline Queue Management */}
            {offlineQueue.length > 0 && (
                <div className="card mt-4">
                    <div className="card-header d-flex justify-content-between align-items-center">
                        <h6 className="mb-0">📱 Offline Check-in Queue ({offlineQueue.length})</h6>
                        <div className="btn-group btn-group-sm">
                            <button 
                                className="btn btn-outline-success"
                                onClick={syncOfflineCheckIns}
                                disabled={!isOnline || syncing}
                                title={!isOnline ? 'Sync requires internet connection' : 'Sync all offline check-ins'}
                            >
                                🔄 Sync All
                            </button>
                            <button 
                                className="btn btn-outline-info"
                                onClick={exportOfflineData}
                                title="Export offline data as CSV"
                            >
                                📊 Export
                            </button>
                            <button 
                                className="btn btn-outline-danger"
                                onClick={clearQueue}
                                title="Clear all offline check-ins"
                            >
                                🗑️ Clear
                            </button>
                        </div>
                    </div>
                    <div className="card-body p-0">
                        <div className="table-responsive" style={{ maxHeight: '300px', overflowY: 'auto' }}>
                            <table className="table table-sm table-striped mb-0">
                                <thead className="table-light sticky-top">
                                    <tr>
                                        <th>Time</th>
                                        <th>Type</th>
                                        <th>Method</th>
                                        <th>Registration ID</th>
                                        <th>Notes</th>
                                        <th style={{width: "50px"}}>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {offlineQueue.map((item) => (
                                        <tr key={item.id}>
                                            <td>
                                                <small>
                                                    {new Date(item.checkedInAt).toLocaleTimeString('en-US', {
                                                        hour: '2-digit',
                                                        minute: '2-digit'
                                                    })}
                                                </small>
                                            </td>
                                            <td>
                                                <span className={`badge ${item.type === 'SESSION' ? 'bg-info' : 'bg-primary'} small`}>
                                                    {item.type}
                                                </span>
                                            </td>
                                            <td>
                                                <span className={`badge ${item.method === 'QR_CODE' ? 'bg-success' : 'bg-secondary'} small`}>
                                                    {item.method === 'QR_CODE' ? '📱' : '✏️'}
                                                </span>
                                            </td>
                                            <td>
                                                <small className="font-monospace">
                                                    {item.registrationId.substring(0, 8)}...
                                                </small>
                                            </td>
                                            <td>
                                                <small>{item.notes || '-'}</small>
                                            </td>
                                            <td>
                                                <button
                                                    className="btn btn-sm btn-outline-danger"
                                                    onClick={() => removeFromQueue(item.id)}
                                                    title="Remove from queue"
                                                >
                                                    ❌
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            )}

            {/* Offline Instructions */}
            {!isOnline && (
                <div className="card mt-4 border-warning">
                    <div className="card-header bg-warning text-dark">
                        <h6 className="mb-0">📱 Offline Mode Instructions</h6>
                    </div>
                    <div className="card-body">
                        <ul className="mb-0">
                            <li><strong>QR Scanning:</strong> QR codes will be stored locally and synced when online</li>
                            <li><strong>Manual Check-ins:</strong> Enter attendee details to queue for sync</li>
                            <li><strong>Data Safety:</strong> All offline data is saved in your device's storage</li>
                            <li><strong>Auto-Sync:</strong> Check-ins will automatically sync when connection is restored</li>
                        </ul>
                        
                        <div className="alert alert-info mt-3 mb-0">
                            <small>
                                <strong>💡 Tip:</strong> You can continue checking in attendees even without internet. 
                                All data will be preserved and synced automatically when you're back online.
                            </small>
                        </div>
                    </div>
                </div>
            )}

            {/* Service Worker Registration Status */}
            <div className="mt-3">
                <small className="text-muted">
                    <strong>Offline Support:</strong> {
                        'serviceWorker' in navigator 
                            ? '✅ Enabled - Your check-ins are safe offline' 
                            : '❌ Not supported in this browser'
                    }
                </small>
            </div>
        </div>
    );
};

export default OfflineCheckIn;