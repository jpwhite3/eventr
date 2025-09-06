import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';
import ExportManagerComponent from './ExportManager';

interface AdvancedAnalyticsDashboardProps {
    eventId: string;
}

interface EventAnalytics {
    eventId: string;
    eventName: string;
    totalSessions: number;
    totalRegistrations: number;
    totalResources: number;
    averageSessionUtilization: number;
    averageResourceUtilization: number;
    conflictRate: number;
    satisfactionScore?: number;
    analyticsGeneratedAt: string;
}

interface SessionAnalytics {
    sessionId: string;
    sessionTitle: string;
    capacity: number;
    registrations: number;
    utilizationRate: number;
    waitlistCount: number;
    checkInRate: number;
    attendanceRate: number;
    popularityScore: number;
    registrationTrend: 'INCREASING' | 'STABLE' | 'DECREASING';
    peakRegistrationTime?: string;
    averageRegistrationToEventDays: number;
    cancellationRate: number;
    prerequisiteViolations: number;
}

interface ResourceAnalytics {
    resourceId: string;
    resourceName: string;
    resourceType: string;
    utilizationRate: number;
    bookingCount: number;
    conflictCount: number;
    maintenanceOverdue: boolean;
    costEfficiency: number;
    demandScore: number;
    averageBookingDuration: number;
}

interface RegistrationAnalytics {
    totalUsers: number;
    averageSessionsPerUser: number;
    registrationsByDay: Record<string, number>;
    registrationsByHour: Record<string, number>;
    cancellationRate: number;
    noShowRate: number;
    repeatAttendeeRate: number;
    geographicDistribution: Record<string, number>;
    deviceTypeDistribution: Record<string, number>;
    referralSources: Record<string, number>;
}

interface PredictiveAnalytics {
    expectedTotalRegistrations: number;
    peakRegistrationPeriod: string;
    capacityShortfall: number;
    resourceBottlenecks: string[];
    recommendedAdditionalSessions: number;
    optimalSchedulingWindows: string[];
    churnRiskUsers: number;
    revenueForecast?: number;
    engagementTrendPrediction: 'UP' | 'DOWN' | 'STABLE';
}

const AdvancedAnalyticsDashboard: React.FC<AdvancedAnalyticsDashboardProps> = ({ eventId }) => {
    const [eventAnalytics, setEventAnalytics] = useState<EventAnalytics | null>(null);
    const [sessionAnalytics, setSessionAnalytics] = useState<SessionAnalytics[]>([]);
    const [resourceAnalytics, setResourceAnalytics] = useState<ResourceAnalytics[]>([]);
    const [registrationAnalytics, setRegistrationAnalytics] = useState<RegistrationAnalytics | null>(null);
    const [predictiveAnalytics, setPredictiveAnalytics] = useState<PredictiveAnalytics | null>(null);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState<'overview' | 'sessions' | 'resources' | 'registrations' | 'predictions'>('overview');
    const [dateRange, setDateRange] = useState<'7d' | '30d' | '90d' | 'all'>('30d');
    const [exportFormat, setExportFormat] = useState<'pdf' | 'excel' | 'csv'>('pdf');

    useEffect(() => {
        loadAnalyticsData();
    }, [eventId, dateRange]);

    const loadAnalyticsData = async () => {
        try {
            setLoading(true);
            const [
                eventResponse,
                sessionsResponse,
                resourcesResponse,
                registrationsResponse,
                predictionsResponse
            ] = await Promise.all([
                apiClient.get(`/analytics/events/${eventId}?range=${dateRange}`),
                apiClient.get(`/analytics/events/${eventId}/sessions?range=${dateRange}`),
                apiClient.get(`/analytics/events/${eventId}/resources?range=${dateRange}`),
                apiClient.get(`/analytics/events/${eventId}/registrations?range=${dateRange}`),
                apiClient.get(`/analytics/events/${eventId}/predictions`)
            ]);

            setEventAnalytics(eventResponse.data);
            setSessionAnalytics(sessionsResponse.data);
            setResourceAnalytics(resourcesResponse.data);
            setRegistrationAnalytics(registrationsResponse.data);
            setPredictiveAnalytics(predictionsResponse.data);
        } catch (error) {
            console.error('Failed to load analytics data:', error);
        } finally {
            setLoading(false);
        }
    };

    const [showExportPanel, setShowExportPanel] = useState(false);

    const getUtilizationColor = (rate: number) => {
        if (rate >= 90) return 'success';
        if (rate >= 70) return 'warning';
        if (rate >= 50) return 'info';
        return 'secondary';
    };

    const getTrendIcon = (trend: string) => {
        switch (trend) {
            case 'INCREASING': case 'UP': return '📈';
            case 'DECREASING': case 'DOWN': return '📉';
            case 'STABLE': return '➡️';
            default: return '📊';
        }
    };

    const formatPercentage = (value: number) => `${(value * 100).toFixed(1)}%`;

    if (loading) {
        return (
            <div className="text-center p-5">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading advanced analytics...</span>
                </div>
                <p className="mt-3">Generating comprehensive analytics dashboard...</p>
            </div>
        );
    }

    if (!eventAnalytics) {
        return (
            <div className="alert alert-warning">
                <h5>📊 No Analytics Data Available</h5>
                <p>Unable to generate analytics for this event. Please ensure the event has sufficient data.</p>
                <button className="btn btn-primary" onClick={loadAnalyticsData}>
                    🔄 Retry
                </button>
            </div>
        );
    }

    return (
        <div className="advanced-analytics-dashboard">
            {/* Header Controls */}
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h3>📊 Advanced Analytics - {eventAnalytics.eventName}</h3>
                    <p className="text-muted mb-0">
                        Comprehensive event insights, trends, and predictive analytics
                    </p>
                </div>
                <div className="d-flex gap-2">
                    <select 
                        className="form-select"
                        value={dateRange}
                        onChange={(e) => setDateRange(e.target.value as any)}
                    >
                        <option value="7d">Last 7 Days</option>
                        <option value="30d">Last 30 Days</option>
                        <option value="90d">Last 90 Days</option>
                        <option value="all">All Time</option>
                    </select>
                    <button 
                        className="btn btn-success" 
                        onClick={() => setShowExportPanel(!showExportPanel)}
                        disabled={!eventAnalytics}
                    >
                        📥 Export
                    </button>
                    <button className="btn btn-outline-primary" onClick={loadAnalyticsData}>
                        🔄 Refresh
                    </button>
                </div>
            </div>

            {/* Export Panel */}
            {showExportPanel && eventAnalytics && (
                <div className="mb-4">
                    <ExportManagerComponent
                        analyticsData={{
                            ...eventAnalytics,
                            sessionAnalytics,
                            registrationAnalytics,
                            predictiveAnalytics,
                            resourceAnalytics
                        }}
                        eventName={eventAnalytics.eventName}
                        dateRange={dateRange}
                    />
                </div>
            )}

            {/* Navigation Tabs */}
            <ul className="nav nav-tabs mb-4">
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'overview' ? 'active' : ''}`}
                        onClick={() => setActiveTab('overview')}
                    >
                        📊 Overview
                    </button>
                </li>
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'sessions' ? 'active' : ''}`}
                        onClick={() => setActiveTab('sessions')}
                    >
                        📅 Sessions ({sessionAnalytics.length})
                    </button>
                </li>
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'resources' ? 'active' : ''}`}
                        onClick={() => setActiveTab('resources')}
                    >
                        🏢 Resources ({resourceAnalytics.length})
                    </button>
                </li>
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'registrations' ? 'active' : ''}`}
                        onClick={() => setActiveTab('registrations')}
                    >
                        👥 Registrations
                    </button>
                </li>
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'predictions' ? 'active' : ''}`}
                        onClick={() => setActiveTab('predictions')}
                    >
                        🔮 Predictions
                        {predictiveAnalytics?.churnRiskUsers && predictiveAnalytics.churnRiskUsers > 0 && (
                            <span className="badge bg-warning ms-1">{predictiveAnalytics.churnRiskUsers}</span>
                        )}
                    </button>
                </li>
            </ul>

            {/* Tab Content */}
            {activeTab === 'overview' && (
                <div className="tab-content">
                    {/* Key Metrics */}
                    <div className="row mb-4">
                        <div className="col-lg-3 col-md-6 mb-3">
                            <div className="card bg-primary text-white">
                                <div className="card-body">
                                    <div className="d-flex justify-content-between">
                                        <div>
                                            <div className="h4 mb-0">{eventAnalytics.totalSessions}</div>
                                            <p className="card-text">Total Sessions</p>
                                        </div>
                                        <div className="align-self-center">
                                            <span className="h1">📅</span>
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
                                            <div className="h4 mb-0">{eventAnalytics.totalRegistrations}</div>
                                            <p className="card-text">Total Registrations</p>
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
                                            <div className="h4 mb-0">{formatPercentage(eventAnalytics.averageSessionUtilization)}</div>
                                            <p className="card-text">Avg Session Utilization</p>
                                        </div>
                                        <div className="align-self-center">
                                            <span className="h1">📈</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="col-lg-3 col-md-6 mb-3">
                            <div className={`card ${eventAnalytics.conflictRate > 0.1 ? 'bg-warning' : 'bg-secondary'} text-white`}>
                                <div className="card-body">
                                    <div className="d-flex justify-content-between">
                                        <div>
                                            <div className="h4 mb-0">{formatPercentage(eventAnalytics.conflictRate)}</div>
                                            <p className="card-text">Conflict Rate</p>
                                        </div>
                                        <div className="align-self-center">
                                            <span className="h1">{eventAnalytics.conflictRate > 0.1 ? '⚠️' : '✅'}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Summary Cards */}
                    <div className="row">
                        <div className="col-md-8">
                            <div className="card">
                                <div className="card-header">
                                    <h5>📊 Event Summary</h5>
                                </div>
                                <div className="card-body">
                                    <div className="row">
                                        <div className="col-md-6">
                                            <p><strong>Total Resources:</strong> {eventAnalytics.totalResources}</p>
                                            <p><strong>Resource Utilization:</strong> {formatPercentage(eventAnalytics.averageResourceUtilization)}</p>
                                            {eventAnalytics.satisfactionScore && (
                                                <p><strong>Satisfaction Score:</strong> {eventAnalytics.satisfactionScore.toFixed(1)}/10</p>
                                            )}
                                        </div>
                                        <div className="col-md-6">
                                            <p><strong>Generated:</strong> {new Date(eventAnalytics.analyticsGeneratedAt).toLocaleString()}</p>
                                            <p><strong>Data Range:</strong> {dateRange.replace('d', ' days').replace('all', 'All time')}</p>
                                            {predictiveAnalytics && (
                                                <p><strong>Forecast Trend:</strong> {getTrendIcon(predictiveAnalytics.engagementTrendPrediction)} {predictiveAnalytics.engagementTrendPrediction}</p>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div className="col-md-4">
                            <div className="card">
                                <div className="card-header">
                                    <h5>🎯 Quick Insights</h5>
                                </div>
                                <div className="card-body">
                                    <div className="list-group list-group-flush">
                                        <div className="list-group-item px-0">
                                            <small>
                                                <strong>Most Popular Session:</strong><br />
                                                {sessionAnalytics.length > 0 && 
                                                    sessionAnalytics.sort((a, b) => b.popularityScore - a.popularityScore)[0]?.sessionTitle || 'N/A'}
                                            </small>
                                        </div>
                                        <div className="list-group-item px-0">
                                            <small>
                                                <strong>Most Utilized Resource:</strong><br />
                                                {resourceAnalytics.length > 0 && 
                                                    resourceAnalytics.sort((a, b) => b.utilizationRate - a.utilizationRate)[0]?.resourceName || 'N/A'}
                                            </small>
                                        </div>
                                        {registrationAnalytics && (
                                            <div className="list-group-item px-0">
                                                <small>
                                                    <strong>Peak Registration Day:</strong><br />
                                                    {Object.entries(registrationAnalytics.registrationsByDay)
                                                        .sort(([,a], [,b]) => b - a)[0]?.[0] || 'N/A'}
                                                </small>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {activeTab === 'sessions' && (
                <div className="tab-content">
                    <div className="card">
                        <div className="card-header">
                            <h5>📅 Session Performance Analytics</h5>
                        </div>
                        <div className="card-body p-0">
                            <div className="table-responsive">
                                <table className="table table-hover mb-0">
                                    <thead className="table-light">
                                        <tr>
                                            <th>Session</th>
                                            <th>Capacity</th>
                                            <th>Utilization</th>
                                            <th>Attendance</th>
                                            <th>Popularity</th>
                                            <th>Trend</th>
                                            <th>Issues</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {sessionAnalytics.map((session) => (
                                            <tr key={session.sessionId}>
                                                <td>
                                                    <strong>{session.sessionTitle}</strong>
                                                    {session.waitlistCount > 0 && (
                                                        <><br /><small className="text-warning">⏳ {session.waitlistCount} waitlisted</small></>
                                                    )}
                                                </td>
                                                <td>
                                                    <div>
                                                        <strong>{session.registrations}</strong> / {session.capacity}
                                                    </div>
                                                    <div className="progress" style={{width: '80px', height: '4px'}}>
                                                        <div 
                                                            className={`progress-bar bg-${getUtilizationColor(session.utilizationRate * 100)}`}
                                                            style={{width: `${Math.min(100, session.utilizationRate * 100)}%`}}
                                                        />
                                                    </div>
                                                </td>
                                                <td>
                                                    <span className={`badge bg-${getUtilizationColor(session.utilizationRate * 100)}`}>
                                                        {formatPercentage(session.utilizationRate)}
                                                    </span>
                                                </td>
                                                <td>
                                                    <div>Check-in: {formatPercentage(session.checkInRate)}</div>
                                                    <div>Attendance: {formatPercentage(session.attendanceRate)}</div>
                                                </td>
                                                <td>
                                                    <div className="d-flex align-items-center">
                                                        <div className="progress me-2" style={{width: '60px', height: '6px'}}>
                                                            <div 
                                                                className="progress-bar bg-primary"
                                                                style={{width: `${Math.min(100, session.popularityScore)}%`}}
                                                            />
                                                        </div>
                                                        <small>{session.popularityScore.toFixed(0)}</small>
                                                    </div>
                                                </td>
                                                <td>
                                                    <div className="d-flex align-items-center">
                                                        <span className="me-1">{getTrendIcon(session.registrationTrend)}</span>
                                                        <small>{session.registrationTrend}</small>
                                                    </div>
                                                    <div>
                                                        <small className="text-muted">
                                                            Avg: {session.averageRegistrationToEventDays.toFixed(1)} days
                                                        </small>
                                                    </div>
                                                </td>
                                                <td>
                                                    <div>
                                                        {session.cancellationRate > 0.1 && (
                                                            <span className="badge bg-warning">High Cancellation</span>
                                                        )}
                                                    </div>
                                                    {session.prerequisiteViolations > 0 && (
                                                        <div><span className="badge bg-danger">{session.prerequisiteViolations} Violations</span></div>
                                                    )}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {activeTab === 'resources' && (
                <div className="tab-content">
                    <div className="card">
                        <div className="card-header">
                            <h5>🏢 Resource Analytics</h5>
                        </div>
                        <div className="card-body p-0">
                            <div className="table-responsive">
                                <table className="table table-hover mb-0">
                                    <thead className="table-light">
                                        <tr>
                                            <th>Resource</th>
                                            <th>Type</th>
                                            <th>Utilization</th>
                                            <th>Bookings</th>
                                            <th>Efficiency</th>
                                            <th>Status</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {resourceAnalytics.map((resource) => (
                                            <tr key={resource.resourceId}>
                                                <td>
                                                    <strong>{resource.resourceName}</strong>
                                                </td>
                                                <td>
                                                    <span className="badge bg-secondary">
                                                        {resource.resourceType}
                                                    </span>
                                                </td>
                                                <td>
                                                    <div className="d-flex align-items-center">
                                                        <div className="progress me-2" style={{width: '60px', height: '6px'}}>
                                                            <div 
                                                                className={`progress-bar bg-${getUtilizationColor(resource.utilizationRate * 100)}`}
                                                                style={{width: `${Math.min(100, resource.utilizationRate * 100)}%`}}
                                                            />
                                                        </div>
                                                        <small>{formatPercentage(resource.utilizationRate)}</small>
                                                    </div>
                                                    <div>
                                                        <small className="text-muted">
                                                            Avg: {resource.averageBookingDuration.toFixed(1)}h
                                                        </small>
                                                    </div>
                                                </td>
                                                <td>
                                                    <div><strong>{resource.bookingCount}</strong> bookings</div>
                                                    {resource.conflictCount > 0 && (
                                                        <div><small className="text-danger">{resource.conflictCount} conflicts</small></div>
                                                    )}
                                                </td>
                                                <td>
                                                    <div>
                                                        Cost: <strong>{resource.costEfficiency.toFixed(1)}</strong>
                                                    </div>
                                                    <div>
                                                        Demand: <strong>{resource.demandScore.toFixed(0)}</strong>
                                                    </div>
                                                </td>
                                                <td>
                                                    <div>
                                                        {resource.maintenanceOverdue ? (
                                                            <span className="badge bg-danger">Maintenance Due</span>
                                                        ) : (
                                                            <span className="badge bg-success">Operational</span>
                                                        )}
                                                    </div>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {activeTab === 'registrations' && registrationAnalytics && (
                <div className="tab-content">
                    <div className="row">
                        <div className="col-md-8">
                            <div className="card mb-4">
                                <div className="card-header">
                                    <h5>📈 Registration Trends</h5>
                                </div>
                                <div className="card-body">
                                    <div className="row">
                                        <div className="col-md-6">
                                            <h6>📅 Daily Registrations</h6>
                                            <div className="list-group list-group-flush">
                                                {Object.entries(registrationAnalytics.registrationsByDay)
                                                    .sort(([,a], [,b]) => b - a)
                                                    .slice(0, 5)
                                                    .map(([day, count]) => (
                                                        <div key={day} className="list-group-item d-flex justify-content-between px-0">
                                                            <span>{day}</span>
                                                            <span className="badge bg-primary">{count}</span>
                                                        </div>
                                                    ))
                                                }
                                            </div>
                                        </div>
                                        <div className="col-md-6">
                                            <h6>🕐 Hourly Distribution</h6>
                                            <div className="list-group list-group-flush">
                                                {Object.entries(registrationAnalytics.registrationsByHour)
                                                    .sort(([,a], [,b]) => b - a)
                                                    .slice(0, 5)
                                                    .map(([hour, count]) => (
                                                        <div key={hour} className="list-group-item d-flex justify-content-between px-0">
                                                            <span>{hour}:00</span>
                                                            <span className="badge bg-info">{count}</span>
                                                        </div>
                                                    ))
                                                }
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div className="col-md-4">
                            <div className="card mb-4">
                                <div className="card-header">
                                    <h5>👥 User Metrics</h5>
                                </div>
                                <div className="card-body">
                                    <p><strong>Total Users:</strong> {registrationAnalytics.totalUsers}</p>
                                    <p><strong>Avg Sessions/User:</strong> {registrationAnalytics.averageSessionsPerUser.toFixed(1)}</p>
                                    <p><strong>Cancellation Rate:</strong> {formatPercentage(registrationAnalytics.cancellationRate)}</p>
                                    <p><strong>No-Show Rate:</strong> {formatPercentage(registrationAnalytics.noShowRate)}</p>
                                    <p><strong>Repeat Attendees:</strong> {formatPercentage(registrationAnalytics.repeatAttendeeRate)}</p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="row">
                        <div className="col-md-4">
                            <div className="card">
                                <div className="card-header">
                                    <h6>🌍 Geographic Distribution</h6>
                                </div>
                                <div className="card-body">
                                    <div className="list-group list-group-flush">
                                        {Object.entries(registrationAnalytics.geographicDistribution)
                                            .sort(([,a], [,b]) => b - a)
                                            .slice(0, 5)
                                            .map(([region, count]) => (
                                                <div key={region} className="list-group-item d-flex justify-content-between px-0">
                                                    <span>{region}</span>
                                                    <span className="badge bg-success">{count}</span>
                                                </div>
                                            ))
                                        }
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div className="col-md-4">
                            <div className="card">
                                <div className="card-header">
                                    <h6>📱 Device Types</h6>
                                </div>
                                <div className="card-body">
                                    <div className="list-group list-group-flush">
                                        {Object.entries(registrationAnalytics.deviceTypeDistribution)
                                            .sort(([,a], [,b]) => b - a)
                                            .map(([device, count]) => (
                                                <div key={device} className="list-group-item d-flex justify-content-between px-0">
                                                    <span>{device}</span>
                                                    <span className="badge bg-warning">{count}</span>
                                                </div>
                                            ))
                                        }
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div className="col-md-4">
                            <div className="card">
                                <div className="card-header">
                                    <h6>📊 Referral Sources</h6>
                                </div>
                                <div className="card-body">
                                    <div className="list-group list-group-flush">
                                        {Object.entries(registrationAnalytics.referralSources)
                                            .sort(([,a], [,b]) => b - a)
                                            .slice(0, 5)
                                            .map(([source, count]) => (
                                                <div key={source} className="list-group-item d-flex justify-content-between px-0">
                                                    <span>{source}</span>
                                                    <span className="badge bg-info">{count}</span>
                                                </div>
                                            ))
                                        }
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {activeTab === 'predictions' && predictiveAnalytics && (
                <div className="tab-content">
                    <div className="row">
                        <div className="col-md-8">
                            <div className="card mb-4">
                                <div className="card-header">
                                    <h5>🔮 Predictive Analytics</h5>
                                </div>
                                <div className="card-body">
                                    <div className="row">
                                        <div className="col-md-6">
                                            <h6>📊 Registration Forecast</h6>
                                            <p><strong>Expected Total:</strong> {predictiveAnalytics.expectedTotalRegistrations}</p>
                                            <p><strong>Peak Period:</strong> {predictiveAnalytics.peakRegistrationPeriod}</p>
                                            <p><strong>Capacity Shortfall:</strong> {predictiveAnalytics.capacityShortfall}</p>
                                            {predictiveAnalytics.revenueForecast && (
                                                <p><strong>Revenue Forecast:</strong> ${predictiveAnalytics.revenueForecast.toLocaleString()}</p>
                                            )}
                                        </div>
                                        <div className="col-md-6">
                                            <h6>💡 Recommendations</h6>
                                            <p><strong>Additional Sessions:</strong> {predictiveAnalytics.recommendedAdditionalSessions}</p>
                                            <p><strong>Churn Risk Users:</strong> 
                                                <span className={`badge ms-1 ${predictiveAnalytics.churnRiskUsers > 0 ? 'bg-warning' : 'bg-success'}`}>
                                                    {predictiveAnalytics.churnRiskUsers}
                                                </span>
                                            </p>
                                            <p><strong>Engagement Trend:</strong> {getTrendIcon(predictiveAnalytics.engagementTrendPrediction)} {predictiveAnalytics.engagementTrendPrediction}</p>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="card">
                                <div className="card-header">
                                    <h6>⏰ Optimal Scheduling Windows</h6>
                                </div>
                                <div className="card-body">
                                    <div className="row">
                                        {predictiveAnalytics.optimalSchedulingWindows.map((window, index) => (
                                            <div key={index} className="col-md-4 mb-2">
                                                <span className="badge bg-primary w-100">{window}</span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div className="col-md-4">
                            <div className="card mb-4">
                                <div className="card-header">
                                    <h6>🚫 Resource Bottlenecks</h6>
                                </div>
                                <div className="card-body">
                                    {predictiveAnalytics.resourceBottlenecks.length === 0 ? (
                                        <p className="text-success">✅ No bottlenecks detected</p>
                                    ) : (
                                        <div className="list-group list-group-flush">
                                            {predictiveAnalytics.resourceBottlenecks.map((bottleneck, index) => (
                                                <div key={index} className="list-group-item px-0">
                                                    <span className="badge bg-warning me-2">⚠️</span>
                                                    {bottleneck}
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </div>

                            <div className="card">
                                <div className="card-header">
                                    <h6>🎯 Action Items</h6>
                                </div>
                                <div className="card-body">
                                    <div className="list-group list-group-flush">
                                        {predictiveAnalytics.churnRiskUsers > 0 && (
                                            <div className="list-group-item px-0">
                                                <small>
                                                    <strong>Priority:</strong> Follow up with {predictiveAnalytics.churnRiskUsers} at-risk users
                                                </small>
                                            </div>
                                        )}
                                        {predictiveAnalytics.capacityShortfall > 0 && (
                                            <div className="list-group-item px-0">
                                                <small>
                                                    <strong>Urgent:</strong> Add {predictiveAnalytics.capacityShortfall} more capacity
                                                </small>
                                            </div>
                                        )}
                                        {predictiveAnalytics.recommendedAdditionalSessions > 0 && (
                                            <div className="list-group-item px-0">
                                                <small>
                                                    <strong>Recommend:</strong> Schedule {predictiveAnalytics.recommendedAdditionalSessions} more sessions
                                                </small>
                                            </div>
                                        )}
                                        <div className="list-group-item px-0">
                                            <small>
                                                <strong>Next Review:</strong> {new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toLocaleDateString()}
                                            </small>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdvancedAnalyticsDashboard;