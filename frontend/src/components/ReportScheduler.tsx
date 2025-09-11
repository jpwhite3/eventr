import React, { useState, useEffect, useCallback } from 'react';
import { AutomatedReportingManager, ReportSchedule } from '../utils/exportUtils';
import apiClient from '../api/apiClient';

interface ReportSchedulerProps {
  onScheduleCreated?: (schedule: ReportSchedule) => void;
  onScheduleUpdated?: (schedule: ReportSchedule) => void;
  onScheduleDeleted?: (scheduleId: string) => void;
}

interface ScheduleSummary {
  totalSchedules: number;
  activeSchedules: number;
  dueSchedules: number;
  nextUpcoming: ReportSchedule[];
}

const ReportScheduler: React.FC<ReportSchedulerProps> = ({
  onScheduleCreated,
  onScheduleUpdated,
  onScheduleDeleted
}) => {
  const [schedules, setSchedules] = useState<ReportSchedule[]>([]);
  const [summary, setSummary] = useState<ScheduleSummary | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [loading, setLoading] = useState(true);
  const [reportingManager] = useState(() => new AutomatedReportingManager());

  const [newSchedule, setNewSchedule] = useState<Partial<ReportSchedule>>({
    name: '',
    description: '',
    frequency: 'weekly',
    format: 'pdf',
    emailRecipients: [],
    dataSource: 'event_analytics',
    active: true
  });

  useEffect(() => {
    loadSchedules();
    loadSummary();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadSchedules = useCallback(async () => {
    try {
      const response = await apiClient.get('/reports/schedules');
      const backendSchedules = response.data.map((s: any) => ({
        id: s.id,
        name: s.name,
        description: s.description,
        frequency: s.frequency.toLowerCase(),
        format: s.format.toLowerCase(),
        emailRecipients: s.emailRecipients,
        dataSource: s.dataSource,
        lastGenerated: s.lastGenerated ? new Date(s.lastGenerated) : undefined,
        nextScheduled: s.nextScheduled ? new Date(s.nextScheduled) : undefined,
        active: s.active,
        template: s.template
      } as ReportSchedule));
      
      setSchedules(backendSchedules);
    } catch (error) {
      console.error('Failed to load schedules:', error);
      // Fallback to local schedules
      setSchedules(reportingManager.getAllSchedules());
    } finally {
      setLoading(false);
    }
  }, [reportingManager]);

  const loadSummary = useCallback(async () => {
    try {
      // In a real implementation, this would call the backend
      const mockSummary: ScheduleSummary = {
        totalSchedules: schedules.length,
        activeSchedules: schedules.filter(s => s.active).length,
        dueSchedules: schedules.filter(s => 
          s.active && s.nextScheduled && s.nextScheduled <= new Date()
        ).length,
        nextUpcoming: schedules
          .filter(s => s.active && s.nextScheduled)
          .sort((a, b) => (a.nextScheduled!.getTime() - b.nextScheduled!.getTime()))
          .slice(0, 3)
      };
      setSummary(mockSummary);
    } catch (error) {
      console.error('Failed to load schedule summary:', error);
    }
  }, [schedules]);

  const handleCreateSchedule = async () => {
    if (!newSchedule.name || !newSchedule.emailRecipients?.length) {
      alert('Please fill in required fields');
      return;
    }

    try {
      const scheduleToCreate: ReportSchedule = {
        name: newSchedule.name,
        description: newSchedule.description || '',
        frequency: newSchedule.frequency as ReportSchedule['frequency'],
        format: newSchedule.format as ReportSchedule['format'],
        emailRecipients: newSchedule.emailRecipients,
        dataSource: newSchedule.dataSource || 'event_analytics',
        active: newSchedule.active ?? true
      };

      // Try backend first, fallback to local manager
      try {
        const response = await apiClient.post('/reports/schedules', {
          ...scheduleToCreate,
          frequency: scheduleToCreate.frequency.toUpperCase(),
          format: scheduleToCreate.format.toUpperCase()
        });
        
        const createdSchedule = {
          ...scheduleToCreate,
          id: response.data.id,
          lastGenerated: response.data.lastGenerated ? new Date(response.data.lastGenerated) : undefined,
          nextScheduled: response.data.nextScheduled ? new Date(response.data.nextScheduled) : undefined
        };
        
        setSchedules(prev => [...prev, createdSchedule]);
        onScheduleCreated?.(createdSchedule);
      } catch (apiError) {
        // Fallback to local manager
        const scheduleId = reportingManager.addSchedule(scheduleToCreate);
        const createdSchedule = reportingManager.getSchedule(scheduleId);
        if (createdSchedule) {
          setSchedules(prev => [...prev, createdSchedule]);
          onScheduleCreated?.(createdSchedule);
        }
      }

      // Reset form
      setNewSchedule({
        name: '',
        description: '',
        frequency: 'weekly',
        format: 'pdf',
        emailRecipients: [],
        dataSource: 'event_analytics',
        active: true
      });
      
      setShowCreateModal(false);
      loadSummary();
    } catch (error) {
      console.error('Failed to create schedule:', error);
      alert('Failed to create schedule. Please try again.');
    }
  };

  const handleToggleSchedule = async (schedule: ReportSchedule) => {
    try {
      if (schedule.id) {
        const response = await apiClient.post(`/reports/schedules/${schedule.id}/toggle`);
        const updatedSchedule = {
          ...schedule,
          active: response.data.active
        };
        
        setSchedules(prev => prev.map(s => s.id === schedule.id ? updatedSchedule : s));
        onScheduleUpdated?.(updatedSchedule);
      } else {
        // Fallback to local manager
        const wasUpdated = reportingManager.updateSchedule(schedule.id!, { active: !schedule.active });
        if (wasUpdated) {
          const updatedSchedule = { ...schedule, active: !schedule.active };
          setSchedules(prev => prev.map(s => s.id === schedule.id ? updatedSchedule : s));
          onScheduleUpdated?.(updatedSchedule);
        }
      }
      
      loadSummary();
    } catch (error) {
      console.error('Failed to toggle schedule:', error);
      alert('Failed to update schedule status.');
    }
  };

  const handleExecuteSchedule = async (schedule: ReportSchedule) => {
    if (!schedule.id) return;
    
    try {
      await apiClient.post(`/reports/schedules/${schedule.id}/execute`);
      alert(`Report generation initiated for: ${schedule.name}`);
      loadSchedules();
      loadSummary();
    } catch (error) {
      console.error('Failed to execute schedule:', error);
      alert('Failed to execute schedule. Please try again.');
    }
  };

  const handleDeleteSchedule = async (schedule: ReportSchedule) => {
    if (!schedule.id || !window.confirm(`Are you sure you want to delete "${schedule.name}"?`)) {
      return;
    }

    try {
      await apiClient.delete(`/reports/schedules/${schedule.id}`);
      setSchedules(prev => prev.filter(s => s.id !== schedule.id));
      onScheduleDeleted?.(schedule.id);
      loadSummary();
    } catch (error) {
      console.error('Failed to delete schedule:', error);
      alert('Failed to delete schedule. Please try again.');
    }
  };

  const formatNextRun = (date?: Date) => {
    if (!date) return 'Not scheduled';
    
    const now = new Date();
    const timeDiff = date.getTime() - now.getTime();
    
    if (timeDiff < 0) return 'Overdue';
    
    const days = Math.floor(timeDiff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((timeDiff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    
    if (days > 0) return `In ${days} day${days > 1 ? 's' : ''}`;
    if (hours > 0) return `In ${hours} hour${hours > 1 ? 's' : ''}`;
    return 'Soon';
  };

  const getFrequencyBadgeColor = (frequency: string) => {
    const colors: Record<string, string> = {
      daily: 'primary',
      weekly: 'success',
      monthly: 'info',
      quarterly: 'warning'
    };
    return colors[frequency] || 'secondary';
  };

  const getFormatIcon = (format: string) => {
    const icons: Record<string, string> = {
      pdf: '📄',
      excel: '📊',
      csv: '📋'
    };
    return icons[format] || '📄';
  };

  if (loading) {
    return (
      <div className="text-center p-4">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading report schedules...</span>
        </div>
        <p className="mt-2">Loading automated reporting system...</p>
      </div>
    );
  }

  return (
    <div className="report-scheduler">
      {/* Summary Dashboard */}
      {summary && (
        <div className="row mb-4">
          <div className="col-md-3">
            <div className="card bg-primary text-white">
              <div className="card-body">
                <div className="d-flex justify-content-between">
                  <div>
                    <div className="h3 mb-0">{summary.totalSchedules}</div>
                    <p className="card-text">Total Schedules</p>
                  </div>
                  <div className="align-self-center">
                    <span className="h1">📋</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div className="col-md-3">
            <div className="card bg-success text-white">
              <div className="card-body">
                <div className="d-flex justify-content-between">
                  <div>
                    <div className="h3 mb-0">{summary.activeSchedules}</div>
                    <p className="card-text">Active Schedules</p>
                  </div>
                  <div className="align-self-center">
                    <span className="h1">✅</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="col-md-3">
            <div className={`card ${summary.dueSchedules > 0 ? 'bg-warning' : 'bg-info'} text-white`}>
              <div className="card-body">
                <div className="d-flex justify-content-between">
                  <div>
                    <div className="h3 mb-0">{summary.dueSchedules}</div>
                    <p className="card-text">Due Now</p>
                  </div>
                  <div className="align-self-center">
                    <span className="h1">{summary.dueSchedules > 0 ? '⏰' : '🕒'}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="col-md-3">
            <div className="card bg-info text-white">
              <div className="card-body">
                <div className="d-flex justify-content-between">
                  <div>
                    <div className="h3 mb-0">{summary.nextUpcoming.length}</div>
                    <p className="card-text">Upcoming</p>
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

      {/* Controls */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h4 className="mb-0">📊 Automated Report Schedules</h4>
          <p className="text-muted mb-0">Manage recurring analytics reports and email delivery</p>
        </div>
        <button 
          className="btn btn-primary"
          onClick={() => setShowCreateModal(true)}
        >
          ➕ Create Schedule
        </button>
      </div>

      {/* Schedules Table */}
      <div className="card">
        <div className="card-body p-0">
          {schedules.length === 0 ? (
            <div className="text-center p-5">
              <span className="h1">📋</span>
              <h5 className="mt-3">No Schedules Created</h5>
              <p className="text-muted">Create your first automated report schedule to get started.</p>
              <button 
                className="btn btn-primary"
                onClick={() => setShowCreateModal(true)}
              >
                Create Your First Schedule
              </button>
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Report Name</th>
                    <th>Format</th>
                    <th>Frequency</th>
                    <th>Recipients</th>
                    <th>Next Run</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {schedules.map((schedule) => (
                    <tr key={schedule.id}>
                      <td>
                        <div>
                          <strong>{schedule.name}</strong>
                          {schedule.description && (
                            <><br /><small className="text-muted">{schedule.description}</small></>
                          )}
                        </div>
                      </td>
                      <td>
                        <span className="d-flex align-items-center">
                          <span className="me-2">{getFormatIcon(schedule.format)}</span>
                          {schedule.format.toUpperCase()}
                        </span>
                      </td>
                      <td>
                        <span className={`badge bg-${getFrequencyBadgeColor(schedule.frequency)}`}>
                          {schedule.frequency.charAt(0).toUpperCase() + schedule.frequency.slice(1)}
                        </span>
                      </td>
                      <td>
                        <div>
                          <strong>{schedule.emailRecipients.length}</strong> recipient{schedule.emailRecipients.length !== 1 ? 's' : ''}
                          <br />
                          <small className="text-muted" title={schedule.emailRecipients.join(', ')}>
                            {schedule.emailRecipients[0]}
                            {schedule.emailRecipients.length > 1 && ` +${schedule.emailRecipients.length - 1} more`}
                          </small>
                        </div>
                      </td>
                      <td>
                        <div>
                          <strong className={schedule.nextScheduled && schedule.nextScheduled <= new Date() ? 'text-warning' : ''}>
                            {formatNextRun(schedule.nextScheduled)}
                          </strong>
                          {schedule.lastGenerated && (
                            <><br /><small className="text-success">Last: {schedule.lastGenerated.toLocaleDateString()}</small></>
                          )}
                        </div>
                      </td>
                      <td>
                        <div className="form-check form-switch">
                          <input
                            className="form-check-input"
                            type="checkbox"
                            checked={schedule.active}
                            onChange={() => handleToggleSchedule(schedule)}
                          />
                          <label className="form-check-label">
                            {schedule.active ? 'Active' : 'Inactive'}
                          </label>
                        </div>
                      </td>
                      <td>
                        <div className="btn-group btn-group-sm">
                          <button
                            className="btn btn-outline-primary"
                            onClick={() => handleExecuteSchedule(schedule)}
                            title="Run Now"
                          >
                            ▶️
                          </button>
                          <button
                            className="btn btn-outline-secondary"
                            onClick={() => {/* TODO: Implement edit functionality */}}
                            title="Edit (Coming Soon)"
                            disabled
                          >
                            ✏️
                          </button>
                          <button
                            className="btn btn-outline-danger"
                            onClick={() => handleDeleteSchedule(schedule)}
                            title="Delete"
                          >
                            🗑️
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Create Schedule Modal */}
      {showCreateModal && (
        <div className="modal show" style={{ display: 'block', backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-lg">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">📊 Create New Report Schedule</h5>
                <button 
                  type="button" 
                  className="btn-close"
                  onClick={() => setShowCreateModal(false)}
                />
              </div>
              
              <div className="modal-body">
                <div className="row">
                  <div className="col-md-6">
                    <div className="mb-3">
                      <label className="form-label">Report Name *</label>
                      <input
                        type="text"
                        className="form-control"
                        value={newSchedule.name || ''}
                        onChange={(e) => setNewSchedule(prev => ({ ...prev, name: e.target.value }))}
                        placeholder="e.g., Weekly Event Performance Report"
                      />
                    </div>
                  </div>
                  
                  <div className="col-md-6">
                    <div className="mb-3">
                      <label className="form-label">Format</label>
                      <select
                        className="form-select"
                        value={newSchedule.format || 'pdf'}
                        onChange={(e) => setNewSchedule(prev => ({ ...prev, format: e.target.value as any }))}
                      >
                        <option value="pdf">📄 PDF Report</option>
                        <option value="excel">📊 Excel Workbook</option>
                        <option value="csv">📋 CSV Data</option>
                      </select>
                    </div>
                  </div>
                </div>

                <div className="mb-3">
                  <label className="form-label">Description</label>
                  <textarea
                    className="form-control"
                    rows={2}
                    value={newSchedule.description || ''}
                    onChange={(e) => setNewSchedule(prev => ({ ...prev, description: e.target.value }))}
                    placeholder="Brief description of what this report includes..."
                  />
                </div>

                <div className="row">
                  <div className="col-md-6">
                    <div className="mb-3">
                      <label className="form-label">Frequency</label>
                      <select
                        className="form-select"
                        value={newSchedule.frequency || 'weekly'}
                        onChange={(e) => setNewSchedule(prev => ({ ...prev, frequency: e.target.value as any }))}
                      >
                        <option value="daily">📅 Daily</option>
                        <option value="weekly">🗓️ Weekly</option>
                        <option value="monthly">📆 Monthly</option>
                        <option value="quarterly">🗓️ Quarterly</option>
                      </select>
                    </div>
                  </div>
                  
                  <div className="col-md-6">
                    <div className="mb-3">
                      <label className="form-label">Data Source</label>
                      <select
                        className="form-select"
                        value={newSchedule.dataSource || 'event_analytics'}
                        onChange={(e) => setNewSchedule(prev => ({ ...prev, dataSource: e.target.value }))}
                      >
                        <option value="event_analytics">📊 Event Analytics</option>
                        <option value="registration_analytics">👥 Registration Data</option>
                        <option value="financial_analytics">💰 Financial Data</option>
                        <option value="attendance_analytics">✅ Attendance Data</option>
                      </select>
                    </div>
                  </div>
                </div>

                <div className="mb-3">
                  <label className="form-label">Email Recipients *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={newSchedule.emailRecipients?.join(', ') || ''}
                    onChange={(e) => setNewSchedule(prev => ({ 
                      ...prev, 
                      emailRecipients: e.target.value.split(',').map(email => email.trim()).filter(email => email)
                    }))}
                    placeholder="email1@company.com, email2@company.com"
                  />
                  <div className="form-text">Enter email addresses separated by commas</div>
                </div>

                <div className="form-check">
                  <input
                    className="form-check-input"
                    type="checkbox"
                    checked={newSchedule.active ?? true}
                    onChange={(e) => setNewSchedule(prev => ({ ...prev, active: e.target.checked }))}
                    id="scheduleActive"
                  />
                  <label className="form-check-label" htmlFor="scheduleActive">
                    Activate schedule immediately
                  </label>
                </div>
              </div>
              
              <div className="modal-footer">
                <button 
                  type="button" 
                  className="btn btn-secondary"
                  onClick={() => setShowCreateModal(false)}
                >
                  Cancel
                </button>
                <button 
                  type="button" 
                  className="btn btn-primary"
                  onClick={handleCreateSchedule}
                  disabled={!newSchedule.name || !newSchedule.emailRecipients?.length}
                >
                  📊 Create Schedule
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ReportScheduler;