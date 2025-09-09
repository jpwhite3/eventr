import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';

interface AttendanceDashboardProps {
    eventId: string;
    refreshInterval?: number; // in seconds
}

interface SessionAttendance {
    sessionId: string;
    sessionTitle: string;
    sessionStartTime: string;
    expectedAttendees: number;
    actualAttendees: number;
    attendanceRate: number;
    noShows: number;
}

interface AttendanceStats {
    totalRegistrations: number;
    totalCheckedIn: number;
    eventCheckedIn: number;
    sessionCheckedIn: number;
    checkInRate: number;
    recentCheckIns: any[];
    checkInsByHour: { [hour: string]: number };
    checkInsByMethod: { [method: string]: number };
}

interface AttendanceReport {
    eventName: string;
    totalSessions: number;
    totalRegistrations: number;
    totalAttendees: number;
    overallAttendanceRate: number;
    sessionAttendance: SessionAttendance[];
    generatedAt: string;
}

const AttendanceDashboard: React.FC<AttendanceDashboardProps> = ({ 
    eventId, 
    refreshInterval = 30 
}) => {
    const [stats, setStats] = useState<AttendanceStats | null>(null);
    const [report, setReport] = useState<AttendanceReport | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
    const [autoRefresh, setAutoRefresh] = useState(true);

    useEffect(() => {
        loadDashboardData();
    }, [eventId]);

    useEffect(() => {
        if (!autoRefresh) return;

        const interval = setInterval(() => {
            loadDashboardData();
        }, refreshInterval * 1000);

        return () => clearInterval(interval);
    }, [eventId, refreshInterval, autoRefresh]);

    const loadDashboardData = async () => {
        try {
            setError(null);
            
            const [statsResponse, reportResponse] = await Promise.all([
                apiClient.get(`/checkin/event/${eventId}/stats`),
                apiClient.get(`/checkin/event/${eventId}/report`)
            ]);

            setStats(statsResponse.data);
            setReport(reportResponse.data);
            setLastUpdated(new Date());
            
        } catch (error: any) {
            console.error('Failed to load dashboard data:', error);
            const errorMessage = error.response?.data?.message || 'Failed to load attendance data';
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    const generateQRCode = async (sessionId?: string) => {
        try {
            const endpoint = sessionId 
                ? `/checkin/qr/staff/session/${sessionId}?eventId=${eventId}`
                : `/checkin/qr/staff/event/${eventId}`;
            
            const response = await apiClient.get(endpoint);
            
            // Create and download the QR code image
            const link = document.createElement('a');
            link.href = `data:image/png;base64,${response.data.qrCodeBase64}`;
            link.download = sessionId 
                ? `session-checkin-${sessionId}.png`
                : `event-checkin-${eventId}.png`;
            link.click();
            
        } catch (error) {
            console.error('Failed to generate QR code:', error);
            alert('Failed to generate QR code');
        }
    };

    const exportReport = () => {
        if (!report) return;
        
        const csvContent = generateCSVReport(report);
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', `attendance-report-${eventId}.csv`);
        link.click();
    };

    const generateCSVReport = (report: AttendanceReport): string => {
        const headers = [
            'Session Title',
            'Start Time',
            'Expected Attendees',
            'Actual Attendees',
            'Attendance Rate (%)',
            'No Shows'
        ];

        const rows = report.sessionAttendance.map(session => [
            session.sessionTitle,
            new Date(session.sessionStartTime).toLocaleString(),
            session.expectedAttendees.toString(),
            session.actualAttendees.toString(),
            session.attendanceRate.toFixed(1),
            session.noShows.toString()
        ]);

        const csvContent = [
            `Event: ${report.eventName}`,
            `Generated: ${new Date(report.generatedAt).toLocaleString()}`,
            `Overall Attendance: ${report.overallAttendanceRate.toFixed(1)}%`,
            '',
            headers.join(','),
            ...rows.map(row => row.join(','))
        ].join('\n');

        return csvContent;
    };

    const formatTime = (dateString: string) => {
        return new Date(dateString).toLocaleTimeString('en-US', {
            hour: 'numeric',
            minute: '2-digit',
            hour12: true
        });
    };

    const getAttendanceColor = (rate: number) => {
        if (rate >= 80) return 'success';
        if (rate >= 60) return 'warning';
        return 'danger';
    };

    if (loading) {
        return (
            <div className="text-center p-5">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading dashboard...</span>
                </div>
                <p className="mt-3 text-muted">Loading attendance dashboard...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="alert alert-danger">
                <h5>❌ Dashboard Error</h5>
                <p>{error}</p>
                <button className="btn btn-primary" onClick={loadDashboardData}>
                    🔄 Retry
                </button>
            </div>
        );
    }

    return (
        <div className="attendance-dashboard">
            {/* Dashboard Header */}
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h4>📊 {report?.eventName || 'Event'} - Live Attendance</h4>
                    <p className="text-muted mb-0">
                        Last updated: {lastUpdated?.toLocaleTimeString() || 'Never'}
                        {autoRefresh && (
                            <span className="badge bg-success ms-2">🔄 Auto-refresh</span>
                        )}
                    </p>
                </div>
                <div className="btn-group">
                    <button 
                        className={`btn btn-sm ${autoRefresh ? 'btn-success' : 'btn-outline-secondary'}`}
                        onClick={() => setAutoRefresh(!autoRefresh)}
                    >
                        {autoRefresh ? '⏸️ Pause' : '▶️ Resume'}
                    </button>
                    <button className="btn btn-sm btn-outline-primary" onClick={loadDashboardData}>
                        🔄 Refresh
                    </button>
                    <button className="btn btn-sm btn-outline-info" onClick={() => generateQRCode()}>
                        📱 Event QR
                    </button>
                    <button className="btn btn-sm btn-outline-success" onClick={exportReport}>
                        📊 Export CSV
                    </button>
                </div>
            </div>

            {/* Overall Stats Cards */}
            {stats && (
                <div className="row mb-4">
                    <div className="col-lg-3 col-md-6 mb-3">
                        <div className="card bg-primary text-white">
                            <div className="card-body">
                                <div className="d-flex justify-content-between">
                                    <div>
                                        <div className="h4 mb-0">{stats.totalCheckedIn}</div>
                                        <p className="card-text">Total Checked In</p>
                                    </div>
                                    <div className="align-self-center">
                                        <span className="h1">👥</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div className="col-lg-3 col-md-6 mb-3">
                        <div className="card bg-info text-white">
                            <div className="card-body">
                                <div className="d-flex justify-content-between">
                                    <div>
                                        <div className="h4 mb-0">{stats.totalRegistrations - stats.totalCheckedIn}</div>
                                        <p className="card-text">Not Checked In</p>
                                    </div>
                                    <div className="align-self-center">
                                        <span className="h1">⏳</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div className="col-lg-3 col-md-6 mb-3">
                        <div className="card bg-success text-white">
                            <div className="card-body">
                                <div className="d-flex justify-content-between">
                                    <div>
                                        <div className="h4 mb-0">{stats.checkInRate.toFixed(1)}%</div>
                                        <p className="card-text">Attendance Rate</p>
                                    </div>
                                    <div className="align-self-center">
                                        <span className="h1">📈</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div className="col-lg-3 col-md-6 mb-3">
                        <div className="card bg-warning text-dark">
                            <div className="card-body">
                                <div className="d-flex justify-content-between">
                                    <div>
                                        <div className="h4 mb-0">{report?.totalSessions || 0}</div>
                                        <p className="card-text">Total Sessions</p>
                                    </div>
                                    <div className="align-self-center">
                                        <span className="h1">📅</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            <div className="row">
                {/* Session Attendance Breakdown */}
                <div className="col-lg-8">
                    <div className="card mb-4">
                        <div className="card-header d-flex justify-content-between align-items-center">
                            <h5>📅 Session Attendance Breakdown</h5>
                            <small className="text-muted">Real-time updates</small>
                        </div>
                        <div className="card-body">
                            {report?.sessionAttendance.length === 0 ? (
                                <div className="text-center py-5 text-muted">
                                    <span className="h1">📅</span>
                                    <p>No sessions scheduled yet</p>
                                </div>
                            ) : (
                                <div className="table-responsive">
                                    <table className="table table-hover">
                                        <thead>
                                            <tr>
                                                <th>Session</th>
                                                <th>Time</th>
                                                <th>Expected</th>
                                                <th>Actual</th>
                                                <th>Rate</th>
                                                <th>Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {report?.sessionAttendance.map((session, index) => (
                                                <tr key={session.sessionId || index}>
                                                    <td>
                                                        <strong>{session.sessionTitle}</strong>
                                                    </td>
                                                    <td>
                                                        <small>{formatTime(session.sessionStartTime)}</small>
                                                    </td>
                                                    <td>
                                                        <span className="badge bg-secondary">
                                                            {session.expectedAttendees}
                                                        </span>
                                                    </td>
                                                    <td>
                                                        <span className="badge bg-primary">
                                                            {session.actualAttendees}
                                                        </span>
                                                    </td>
                                                    <td>
                                                        <span className={`badge bg-${getAttendanceColor(session.attendanceRate)}`}>
                                                            {session.attendanceRate.toFixed(1)}%
                                                        </span>
                                                    </td>
                                                    <td>
                                                        <button
                                                            className="btn btn-sm btn-outline-primary"
                                                            onClick={() => generateQRCode(session.sessionId)}
                                                            title="Generate QR Code for Session Check-in"
                                                        >
                                                            📱 QR
                                                        </button>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Check-in Methods Chart */}
                    {stats?.checkInsByMethod && Object.keys(stats.checkInsByMethod).length > 0 && (
                        <div className="card">
                            <div className="card-header">
                                <h6>📊 Check-in Methods</h6>
                            </div>
                            <div className="card-body">
                                <div className="row">
                                    {Object.entries(stats.checkInsByMethod).map(([method, count]) => (
                                        <div key={method} className="col-md-4 mb-3">
                                            <div className="text-center">
                                                <div className="h5 text-primary">{count}</div>
                                                <small className="text-muted">
                                                    {method === 'QR_CODE' ? '📱 QR Code' : 
                                                     method === 'MANUAL' ? '✏️ Manual' : 
                                                     method === 'BULK' ? '📋 Bulk' : method}
                                                </small>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    )}
                </div>

                {/* Recent Activity */}
                <div className="col-lg-4">
                    <div className="card">
                        <div className="card-header">
                            <h6>🕒 Recent Check-ins</h6>
                        </div>
                        <div className="card-body p-0" style={{ maxHeight: '400px', overflowY: 'auto' }}>
                            {!stats?.recentCheckIns || stats.recentCheckIns.length === 0 ? (
                                <div className="p-4 text-center text-muted">
                                    <span className="h3">👥</span>
                                    <p className="mb-0">No recent activity</p>
                                </div>
                            ) : (
                                <div className="list-group list-group-flush">
                                    {stats.recentCheckIns.map((checkIn, index) => (
                                        <div key={checkIn.id || index} className="list-group-item">
                                            <div className="d-flex justify-content-between align-items-start">
                                                <div>
                                                    <h6 className="mb-1">{checkIn.userName}</h6>
                                                    <small className="text-muted">{checkIn.userEmail}</small>
                                                    {checkIn.sessionTitle && (
                                                        <div className="mt-1">
                                                            <span className="badge bg-info small">
                                                                {checkIn.sessionTitle}
                                                            </span>
                                                        </div>
                                                    )}
                                                </div>
                                                <div className="text-end">
                                                    <small className="text-muted">
                                                        {formatTime(checkIn.checkedInAt)}
                                                    </small>
                                                    <div className="mt-1">
                                                        <span className={`badge ${checkIn.method === 'QR_CODE' ? 'bg-primary' : 'bg-secondary'} small`}>
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

export default AttendanceDashboard;