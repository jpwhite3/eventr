import React from 'react';
import { useEventStats } from '../hooks/useWebSocket';

interface RealTimeStatsProps {
  eventId: string;
  showTitle?: boolean;
  compact?: boolean;
}

const RealTimeStats: React.FC<RealTimeStatsProps> = ({ eventId, showTitle = true, compact = false }) => {
  const { attendanceCount, registrationCount, capacityInfo, isConnected, lastUpdate } = useEventStats(eventId);

  const getCapacityColor = () => {
    if (!capacityInfo.percentage) return 'text-muted';
    if (capacityInfo.percentage >= 90) return 'text-danger';
    if (capacityInfo.percentage >= 75) return 'text-warning';
    return 'text-success';
  };

  const getCapacityIcon = () => {
    if (!capacityInfo.percentage) return '📊';
    if (capacityInfo.percentage >= 90) return '🔴';
    if (capacityInfo.percentage >= 75) return '🟡';
    return '🟢';
  };

  if (compact) {
    return (
      <div className="d-flex gap-3 align-items-center">
        <div className="d-flex align-items-center">
          <span className={`badge ${isConnected ? 'bg-success' : 'bg-secondary'} me-2`}>
            {isConnected ? '🟢 Live' : '⚫ Offline'}
          </span>
        </div>
        
        <div className="d-flex align-items-center">
          <span className="me-1">👥</span>
          <span className="fw-bold">{registrationCount}</span>
          <small className="text-muted ms-1">registered</small>
        </div>
        
        <div className="d-flex align-items-center">
          <span className="me-1">✅</span>
          <span className="fw-bold">{attendanceCount}</span>
          <small className="text-muted ms-1">checked in</small>
        </div>
        
        {capacityInfo.max && (
          <div className="d-flex align-items-center">
            <span className="me-1">{getCapacityIcon()}</span>
            <span className={`fw-bold ${getCapacityColor()}`}>
              {capacityInfo.current}/{capacityInfo.max}
            </span>
            {capacityInfo.percentage && (
              <small className="text-muted ms-1">({capacityInfo.percentage}%)</small>
            )}
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="card">
      {showTitle && (
        <div className="card-header d-flex justify-content-between align-items-center">
          <h6 className="mb-0">📊 Live Event Stats</h6>
          <div className="d-flex align-items-center">
            <span className={`badge ${isConnected ? 'bg-success' : 'bg-secondary'} me-2`}>
              {isConnected ? '🟢 Live Updates' : '⚫ Offline'}
            </span>
            {lastUpdate && (
              <small className="text-muted">
                Last updated: {new Date(lastUpdate.timestamp).toLocaleTimeString()}
              </small>
            )}
          </div>
        </div>
      )}
      
      <div className="card-body">
        <div className="row g-3">
          {/* Registration Count */}
          <div className="col-md-6">
            <div className="d-flex align-items-center p-3 bg-light rounded">
              <div className="flex-shrink-0 me-3">
                <div className="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center" 
                     style={{ width: '40px', height: '40px' }}>
                  👥
                </div>
              </div>
              <div className="flex-grow-1">
                <div className="h4 mb-0 fw-bold">{registrationCount}</div>
                <div className="text-muted small">Total Registrations</div>
              </div>
            </div>
          </div>
          
          {/* Attendance Count */}
          <div className="col-md-6">
            <div className="d-flex align-items-center p-3 bg-light rounded">
              <div className="flex-shrink-0 me-3">
                <div className="bg-success text-white rounded-circle d-flex align-items-center justify-content-center" 
                     style={{ width: '40px', height: '40px' }}>
                  ✅
                </div>
              </div>
              <div className="flex-grow-1">
                <div className="h4 mb-0 fw-bold">{attendanceCount}</div>
                <div className="text-muted small">Checked In</div>
                {registrationCount > 0 && (
                  <div className="small text-muted">
                    {Math.round((attendanceCount / registrationCount) * 100)}% attendance rate
                  </div>
                )}
              </div>
            </div>
          </div>
          
          {/* Capacity Info */}
          {capacityInfo.max && (
            <div className="col-12">
              <div className="d-flex align-items-center p-3 bg-light rounded">
                <div className="flex-shrink-0 me-3">
                  <div className={`bg-${capacityInfo.isNearCapacity ? 'danger' : 'info'} text-white rounded-circle d-flex align-items-center justify-content-center`} 
                       style={{ width: '40px', height: '40px' }}>
                    {getCapacityIcon()}
                  </div>
                </div>
                <div className="flex-grow-1">
                  <div className="d-flex align-items-end gap-2">
                    <div className="h4 mb-0 fw-bold">
                      {capacityInfo.current}/{capacityInfo.max}
                    </div>
                    {capacityInfo.percentage !== null && (
                      <div className={`h5 mb-0 ${getCapacityColor()}`}>
                        ({capacityInfo.percentage}%)
                      </div>
                    )}
                  </div>
                  <div className="text-muted small">Current Capacity</div>
                  
                  {/* Capacity Progress Bar */}
                  {capacityInfo.percentage !== null && (
                    <div className="mt-2">
                      <div className="progress" style={{ height: '6px' }}>
                        <div 
                          className={`progress-bar ${capacityInfo.isNearCapacity ? 'bg-danger' : 'bg-info'}`}
                          style={{ width: `${capacityInfo.percentage}%` }}
                        ></div>
                      </div>
                      {capacityInfo.isNearCapacity && (
                        <small className="text-danger fw-bold">⚠️ Near capacity limit!</small>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
        
        {/* Recent Activity Indicator */}
        {lastUpdate && (
          <div className="mt-3 pt-3 border-top">
            <div className="d-flex align-items-center text-muted small">
              <span className="me-2">🔄</span>
              <span>
                Last activity: {lastUpdate.type.replace(/_/g, ' ').toLowerCase()} 
                at {new Date(lastUpdate.timestamp).toLocaleTimeString()}
              </span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default RealTimeStats;