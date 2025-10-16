import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';
import QRScanner from './QRScanner';

interface CheckInInterfaceProps {
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
}

const CheckInInterface: React.FC<CheckInInterfaceProps> = ({
    eventId,
    sessionId,
    staffMember,
    onCheckInSuccess,
    onCheckInError
}) => {
    const [isScanning, setIsScanning] = useState(false);
    const [recentCheckIns, setRecentCheckIns] = useState<CheckInData[]>([]);
    const [stats, setStats] = useState({
        totalCheckedIn: 0,
        totalRegistrations: 0,
        checkInRate: 0
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    useEffect(() => {
        if (eventId) {
            loadStats();
            loadRecentCheckIns();
        }
    }, [eventId]);

    const refreshData = () => {
        loadStats();
        loadRecentCheckIns();
    };

    const loadStats = async () => {
        if (!eventId) return;

        try {
            const response = await apiClient.get(`/checkin/event/${eventId}/stats`);
            setStats(response.data);
        } catch (error) {
            console.error('Failed to load stats:', error);
            setError('Failed to load statistics');
        }
    };

    const loadRecentCheckIns = async () => {
        if (!eventId) return;

        try {
            const endpoint = sessionId 
                ? `/checkin/session/${sessionId}/attendance`
                : `/checkin/event/${eventId}/stats`;
            
            const response = await apiClient.get(endpoint);
            const checkIns = sessionId ? response.data : response.data.recentCheckIns || [];
            setRecentCheckIns(checkIns);
        } catch (error) {
            console.error('Failed to load recent check-ins:', error);
            setError('Failed to load check-in data');
        }
    };

    const handleQRScan = async (qrCode: string) => {
        setLoading(true);
        setError(null);
        setSuccess(null);

        try {
            const checkInData = {
                qrCode: qrCode,
                checkedInBy: staffMember,
                deviceId: navigator.userAgent,
                deviceName: 'Staff Device',
                location: sessionId ? 'Session Check-in' : 'Event Check-in'
            };

            const response = await apiClient.post('/checkin/qr', checkInData);
            const checkIn = response.data;

            setSuccess(`✅ ${checkIn.userName} checked in successfully!`);
            setRecentCheckIns(prev => [checkIn, ...prev.slice(0, 9)]);
            
            // Update stats
            setStats(prev => ({
                ...prev,
                totalCheckedIn: prev.totalCheckedIn + 1,
                checkInRate: ((prev.totalCheckedIn + 1) / prev.totalRegistrations) * 100
            }));

            onCheckInSuccess?.(checkIn);

            // Auto-hide success message
            setTimeout(() => setSuccess(null), 3000);

        } catch (error: any) {
            const errorMessage = error.response?.data?.message || 'Check-in failed';
            setError(errorMessage);
            onCheckInError?.(errorMessage);

            // Auto-hide error message
            setTimeout(() => setError(null), 5000);
        } finally {
            setLoading(false);
        }
    };

    const handleManualCheckIn = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setLoading(true);
        setError(null);

        const formData = new FormData(e.currentTarget);
        const userEmail = formData.get('userEmail') as string;

        try {
            // First, find the registration by email
            const registrationsResponse = await apiClient.get(`/registrations?email=${userEmail}&eventId=${eventId}`);
            
            if (!registrationsResponse.data || registrationsResponse.data.length === 0) {
                throw new Error('Registration not found for this email');
            }

            const registration = registrationsResponse.data[0];

            const checkInData = {
                registrationId: registration.id,
                sessionId: sessionId || null,
                type: sessionId ? 'SESSION' : 'EVENT',
                method: 'MANUAL',
                checkedInBy: staffMember,
                notes: formData.get('notes') as string || null
            };

            const response = await apiClient.post('/checkin/manual', checkInData);
            const checkIn = response.data;

            setSuccess(`✅ ${checkIn.userName} checked in manually!`);
            setRecentCheckIns(prev => [checkIn, ...prev.slice(0, 9)]);

            // Reset form
            (e.target as HTMLFormElement).reset();

            // Update stats
            setStats(prev => ({
                ...prev,
                totalCheckedIn: prev.totalCheckedIn + 1,
                checkInRate: ((prev.totalCheckedIn + 1) / prev.totalRegistrations) * 100
            }));

            onCheckInSuccess?.(checkIn);

            setTimeout(() => setSuccess(null), 3000);

        } catch (error: any) {
            const errorMessage = error.response?.data?.message || error.message || 'Manual check-in failed';
            setError(errorMessage);
            onCheckInError?.(errorMessage);
            setTimeout(() => setError(null), 5000);
        } finally {
            setLoading(false);
        }
    };

    const toggleScanner = () => {
        setIsScanning(!isScanning);
        setError(null);
        setSuccess(null);
    };

    const formatTime = (dateString: string) => {
        return new Date(dateString).toLocaleTimeString('en-US', {
            hour: 'numeric',
            minute: '2-digit',
            hour12: true
        });
    };

    const formatDateTime = (dateString: string) => {
        return new Date(dateString).toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: 'numeric',
            minute: '2-digit',
            hour12: true
        });
    };

    return (
        <div className="check-in-interface">
            {/* Header with Stats */}
            <div className="card mb-4">
                <div className="card-header">
                    <div className="d-flex justify-content-between align-items-center">
                        <h5 className="mb-0">
                            📱 {sessionId ? 'Session' : 'Event'} Check-In
                        </h5>
                        <div className="text-end">
                            <small className="text-muted d-block">Staff: {staffMember}</small>
                            <small className="text-muted">
                                {stats.totalCheckedIn} / {stats.totalRegistrations} 
                                ({stats.checkInRate.toFixed(1)}%)
                            </small>
                        </div>
                    </div>
                </div>
                <div className="card-body">
                    <div className="row text-center">
                        <div className="col-4">
                            <div className="h4 text-primary mb-0">{stats.totalCheckedIn}</div>
                            <small className="text-muted">Checked In</small>
                        </div>
                        <div className="col-4">
                            <div className="h4 text-info mb-0">{stats.totalRegistrations - stats.totalCheckedIn}</div>
                            <small className="text-muted">Remaining</small>
                        </div>
                        <div className="col-4">
                            <div className="h4 text-success mb-0">{stats.checkInRate.toFixed(0)}%</div>
                            <small className="text-muted">Rate</small>
                        </div>
                    </div>
                    
                    <div className="progress mt-3" style={{ height: '8px' }}>
                        <div 
                            className="progress-bar bg-success"
                            style={{ width: `${stats.checkInRate}%` }}
                        />
                    </div>
                </div>
            </div>

            {/* Status Messages */}
            {error && (
                <div className="alert alert-danger alert-dismissible">
                    <strong>❌ Error:</strong> {error}
                    <button type="button" className="btn-close" onClick={() => setError(null)} />
                </div>
            )}

            {success && (
                <div className="alert alert-success alert-dismissible">
                    <strong>{success}</strong>
                    <button type="button" className="btn-close" onClick={() => setSuccess(null)} />
                </div>
            )}

            {/* Check-in Methods */}
            <div className="row">
                <div className="col-lg-8">
                    {/* QR Scanner */}
                    <div className="card mb-4">
                        <div className="card-header d-flex justify-content-between align-items-center">
                            <h6 className="mb-0">📱 QR Code Scanner</h6>
                            <button 
                                className={`btn btn-sm ${isScanning ? 'btn-danger' : 'btn-primary'}`}
                                onClick={toggleScanner}
                                disabled={loading}
                            >
                                {isScanning ? '⏹️ Stop Scanner' : '📷 Start Scanner'}
                            </button>
                        </div>
                        <div className="card-body text-center">
                            {isScanning ? (
                                <QRScanner
                                    isActive={isScanning}
                                    onScan={handleQRScan}
                                    onError={(error) => {
                                        setError(error);
                                        onCheckInError?.(error);
                                    }}
                                />
                            ) : (
                                <div className="py-5 text-muted">
                                    <div className="h1">📱</div>
                                    <p>Click "Start Scanner" to scan QR codes</p>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Manual Check-in */}
                    <div className="card">
                        <div className="card-header">
                            <h6 className="mb-0">✏️ Manual Check-In</h6>
                        </div>
                        <div className="card-body">
                            <form onSubmit={handleManualCheckIn}>
                                <div className="row">
                                    <div className="col-md-8 mb-3">
                                        <label className="form-label">Attendee Email *</label>
                                        <input
                                            type="email"
                                            name="userEmail"
                                            className="form-control"
                                            placeholder="Enter attendee email address"
                                            required
                                            disabled={loading}
                                        />
                                    </div>
                                    <div className="col-md-4 mb-3 d-flex align-items-end">
                                        <button 
                                            type="submit" 
                                            className="btn btn-outline-primary w-100"
                                            disabled={loading}
                                        >
                                            {loading ? '⏳ Checking In...' : '✅ Check In'}
                                        </button>
                                    </div>
                                </div>
                                <div className="mb-3">
                                    <label className="form-label">Notes (Optional)</label>
                                    <input
                                        type="text"
                                        name="notes"
                                        className="form-control"
                                        placeholder="Additional notes..."
                                        disabled={loading}
                                    />
                                </div>
                            </form>
                        </div>
                    </div>
                </div>

                {/* Recent Check-ins */}
                <div className="col-lg-4">
                    <div className="card">
                        <div className="card-header d-flex justify-content-between align-items-center">
                            <h6 className="mb-0">🕒 Recent Check-ins</h6>
                            <button 
                                className="btn btn-sm btn-outline-secondary"
                                onClick={refreshData}
                            >
                                🔄
                            </button>
                        </div>
                        <div className="card-body p-0">
                            {recentCheckIns?.length === 0 ? (
                                <div className="p-4 text-center text-muted">
                                    <div className="h3">👥</div>
                                    <p className="mb-0">No recent check-ins</p>
                                </div>
                            ) : (
                                <div className="list-group list-group-flush">
                                    {recentCheckIns?.map((checkIn, index) => (
                                        <div key={checkIn.id || index} className="list-group-item">
                                            <div className="d-flex justify-content-between align-items-start">
                                                <div className="flex-grow-1">
                                                    <h6 className="mb-1">{checkIn.userName}</h6>
                                                    <small className="text-muted d-block">
                                                        {checkIn.userEmail}
                                                    </small>
                                                    {checkIn.sessionTitle && (
                                                        <small className="badge bg-info">
                                                            {checkIn.sessionTitle}
                                                        </small>
                                                    )}
                                                </div>
                                                <div className="text-end">
                                                    <small className="text-muted">
                                                        {formatTime(checkIn.checkedInAt)}
                                                    </small>
                                                    <div>
                                                        <span className={`badge bg-${checkIn.method === 'QR_CODE' ? 'primary' : 'secondary'} small`}>
                                                            {checkIn.method === 'QR_CODE' ? '📱' : '✏️'}
                                                        </span>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default CheckInInterface;