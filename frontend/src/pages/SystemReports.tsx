import React, { useState, useEffect, useCallback } from 'react';
import {
  CRow,
  CCol,
  CCard,
  CCardBody,
  CCardHeader,
  CButton,
  CButtonGroup,
  CDropdown,
  CDropdownToggle,
  CDropdownMenu,
  CDropdownItem,
  CBadge,
  CTable,
  CTableHead,
  CTableRow,
  CTableHeaderCell,
  CTableBody,
  CTableDataCell,
  CWidgetStatsF,
  CProgress,
} from '@coreui/react';

// Import Bootstrap components for compatibility
import { Modal, Form, Alert, Nav, Tab } from 'react-bootstrap';
import ReportScheduler from '../components/ReportScheduler';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faChartBar,
  faDownload,
  faFileExport,
  faCalendar,
  faSync,
  faFilter,
  faUsers,
  faCalendarAlt,
  faDollarSign,
  faUserCheck,
  faEye,
  faPlay,
  faCog,
  faClock,
  faEnvelope,
  faCloudDownload,
  faDatabase,
  faArrowUp,
  faArrowDown,
} from '@fortawesome/free-solid-svg-icons';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement,
} from 'chart.js';
import { Line, Bar, Doughnut } from 'react-chartjs-2';

// Register ChartJS components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement
);

interface ReportTemplate {
  id: string;
  name: string;
  description: string;
  category: 'events' | 'users' | 'registrations' | 'financial' | 'system';
  lastGenerated?: string;
  format: 'pdf' | 'excel' | 'csv';
  schedule?: 'none' | 'daily' | 'weekly' | 'monthly';
}

interface SystemMetrics {
  totalEvents: number;
  totalUsers: number;
  totalRegistrations: number;
  totalRevenue: number;
  systemUptime: number;
  databaseSize: string;
  activeUsers: number;
  errorRate: number;
}

interface ReportFilter {
  dateRange: string;
  category: string;
  format: string;
}

const SystemReports: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'reports' | 'scheduler'>('reports');
  const [reports, setReports] = useState<ReportTemplate[]>([]);
  const [metrics, setMetrics] = useState<SystemMetrics | null>(null);
  const [generating, setGenerating] = useState<Set<string>>(new Set());
  const [filters, setFilters] = useState<ReportFilter>({
    dateRange: '30d',
    category: 'all',
    format: 'all',
  });
  const [showScheduleModal, setShowScheduleModal] = useState(false);
  const [selectedReport, setSelectedReport] = useState<ReportTemplate | null>(null);
  const [scheduleSettings, setScheduleSettings] = useState({
    frequency: 'monthly',
    email: '',
    description: '',
  });

  // Mock report templates
  const mockReports: ReportTemplate[] = [
    {
      id: '1',
      name: 'Event Performance Summary',
      description: 'Comprehensive overview of all event metrics, attendance rates, and revenue',
      category: 'events',
      lastGenerated: '2024-01-15T10:30:00Z',
      format: 'pdf',
      schedule: 'weekly',
    },
    {
      id: '2',
      name: 'User Activity Report',
      description: 'Detailed analysis of user registrations, engagement, and demographics',
      category: 'users',
      lastGenerated: '2024-01-14T15:45:00Z',
      format: 'excel',
      schedule: 'monthly',
    },
    {
      id: '3',
      name: 'Financial Dashboard',
      description: 'Revenue analysis, payment processing stats, and financial forecasting',
      category: 'financial',
      lastGenerated: '2024-01-13T09:15:00Z',
      format: 'excel',
      schedule: 'monthly',
    },
    {
      id: '4',
      name: 'Registration Trends',
      description: 'Registration patterns, conversion rates, and booking analytics',
      category: 'registrations',
      format: 'csv',
    },
    {
      id: '5',
      name: 'System Health Report',
      description: 'Server performance, uptime statistics, and error logs',
      category: 'system',
      format: 'pdf',
    },
  ];

  const mockMetrics: SystemMetrics = {
    totalEvents: 245,
    totalUsers: 1847,
    totalRegistrations: 5639,
    totalRevenue: 487650,
    systemUptime: 99.7,
    databaseSize: '2.3 GB',
    activeUsers: 134,
    errorRate: 0.02,
  };

  const fetchReports = useCallback(async () => {
    // TODO: Replace with actual API call
    setReports(mockReports);
  }, []);

  const fetchMetrics = useCallback(async () => {
    // TODO: Replace with actual API call
    setMetrics(mockMetrics);
  }, []);

  const handleGenerateReport = async (reportId: string) => {
    setGenerating(prev => new Set(prev).add(reportId));
    
    try {
      // TODO: Implement actual report generation
      await new Promise(resolve => setTimeout(resolve, 2000)); // Simulate API call
      
      // Update last generated time
      setReports(prev => prev.map(report => 
        report.id === reportId 
          ? { ...report, lastGenerated: new Date().toISOString() }
          : report
      ));
      
      alert('Report generated successfully! Download will start shortly.');
    } catch (error) {
      console.error('Failed to generate report:', error);
      alert('Failed to generate report. Please try again.');
    } finally {
      setGenerating(prev => {
        const newSet = new Set(prev);
        newSet.delete(reportId);
        return newSet;
      });
    }
  };

  const handleScheduleReport = (report: ReportTemplate) => {
    setSelectedReport(report);
    setScheduleSettings({
      frequency: report.schedule || 'monthly',
      email: '',
      description: '',
    });
    setShowScheduleModal(true);
  };

  const saveScheduleSettings = async () => {
    if (!selectedReport) return;
    
    try {
      // TODO: Implement schedule saving
      setReports(prev => prev.map(report => 
        report.id === selectedReport.id 
          ? { ...report, schedule: scheduleSettings.frequency as any }
          : report
      ));
      
      setShowScheduleModal(false);
      alert('Report schedule updated successfully!');
    } catch (error) {
      console.error('Failed to save schedule:', error);
      alert('Failed to save schedule settings.');
    }
  };

  const getCategoryBadge = (category: string) => {
    const categoryConfig = {
      events: { color: 'primary', text: 'Events' },
      users: { color: 'success', text: 'Users' },
      registrations: { color: 'info', text: 'Registrations' },
      financial: { color: 'warning', text: 'Financial' },
      system: { color: 'danger', text: 'System' },
    };
    
    const config = categoryConfig[category as keyof typeof categoryConfig] || { color: 'secondary', text: category };
    return <CBadge color={config.color}>{config.text}</CBadge>;
  };

  const getFormatIcon = (format: string) => {
    switch (format) {
      case 'pdf': return faFileExport;
      case 'excel': return faDatabase;
      case 'csv': return faDownload;
      default: return faFileExport;
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString();
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
    }).format(amount);
  };

  // Chart data
  const revenueChartData = {
    labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
    datasets: [
      {
        label: 'Revenue ($)',
        data: [12000, 19000, 15000, 25000, 22000, 30000],
        borderColor: 'rgb(75, 192, 192)',
        backgroundColor: 'rgba(75, 192, 192, 0.2)',
        tension: 0.4,
      },
    ],
  };

  const systemHealthData = {
    labels: ['Uptime', 'Downtime'],
    datasets: [
      {
        data: [99.7, 0.3],
        backgroundColor: ['#28a745', '#dc3545'],
        borderWidth: 2,
        borderColor: '#fff',
      },
    ],
  };

  useEffect(() => {
    fetchReports();
    fetchMetrics();
  }, [fetchReports, fetchMetrics]);

  return (
    <div className="animated fadeIn">
      {/* Header */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h2 mb-0">Reports & Analytics Dashboard</h1>
          <p className="text-medium-emphasis mb-0">
            Generate comprehensive reports, schedule automation, and monitor system performance
          </p>
        </div>
        
        <div className="d-flex gap-2">
          <CButton color="primary">
            <FontAwesomeIcon icon={faFileExport} className="me-1" />
            Custom Report
          </CButton>
          
          <CButton 
            color="outline-secondary" 
            onClick={() => { fetchReports(); fetchMetrics(); }}
          >
            <FontAwesomeIcon icon={faSync} />
          </CButton>
        </div>
      </div>

      {/* Navigation Tabs */}
      <Nav variant="tabs" className="mb-4">
        <Nav.Item>
          <Nav.Link
            href="#"
            active={activeTab === 'reports'}
            onClick={(e) => {
              e.preventDefault();
              setActiveTab('reports');
            }}
          >
            <FontAwesomeIcon icon={faChartBar} className="me-2" />
            Report Templates
          </Nav.Link>
        </Nav.Item>
        <Nav.Item>
          <Nav.Link
            href="#"
            active={activeTab === 'scheduler'}
            onClick={(e) => {
              e.preventDefault();
              setActiveTab('scheduler');
            }}
          >
            <FontAwesomeIcon icon={faClock} className="me-2" />
            Automated Scheduling
          </Nav.Link>
        </Nav.Item>
      </Nav>

      {/* Tab Content */}
      <Tab.Content>
        {activeTab === 'reports' && (
        <Tab.Pane role="tabpanel">
          {/* System Overview */}
          {metrics && (
            <CRow className="mb-4">
          <CCol sm={6} lg={3}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faCalendarAlt} size="xl" />}
              title="Total Events"
              value={metrics.totalEvents.toString()}
              color="primary"
            />
          </CCol>
          <CCol sm={6} lg={3}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faUsers} size="xl" />}
              title="Total Users"
              value={metrics.totalUsers.toLocaleString()}
              color="success"
            />
          </CCol>
          <CCol sm={6} lg={3}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faDollarSign} size="xl" />}
              title="Total Revenue"
              value={formatCurrency(metrics.totalRevenue)}
              color="info"
            />
          </CCol>
          <CCol sm={6} lg={3}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faUserCheck} size="xl" />}
              title="Total Registrations"
              value={metrics.totalRegistrations.toLocaleString()}
              color="warning"
            />
          </CCol>
        </CRow>
      )}

      {/* System Health Metrics */}
      {metrics && (
        <CRow className="mb-4">
          <CCol lg={8}>
            <CCard>
              <CCardHeader>
                <h5 className="card-title mb-0">Revenue Trend</h5>
              </CCardHeader>
              <CCardBody>
                <Line 
                  data={revenueChartData}
                  options={{
                    responsive: true,
                    plugins: {
                      legend: {
                        position: 'top' as const,
                      },
                    },
                    scales: {
                      y: {
                        beginAtZero: true,
                        ticks: {
                          callback: function(value) {
                            return formatCurrency(value as number);
                          }
                        }
                      },
                    },
                  }}
                />
              </CCardBody>
            </CCard>
          </CCol>
          
          <CCol lg={4}>
            <CCard>
              <CCardHeader>
                <h5 className="card-title mb-0">System Health</h5>
              </CCardHeader>
              <CCardBody>
                <div className="text-center mb-3">
                  <div className="fs-2 fw-bold text-success">{metrics.systemUptime}%</div>
                  <div className="text-muted">Uptime</div>
                </div>
                <Doughnut 
                  data={systemHealthData}
                  options={{
                    responsive: true,
                    plugins: {
                      legend: {
                        position: 'bottom' as const,
                      },
                    },
                  }}
                />
                <div className="mt-3">
                  <div className="d-flex justify-content-between mb-1">
                    <span>Database Size:</span>
                    <strong>{metrics.databaseSize}</strong>
                  </div>
                  <div className="d-flex justify-content-between mb-1">
                    <span>Active Users:</span>
                    <strong>{metrics.activeUsers}</strong>
                  </div>
                  <div className="d-flex justify-content-between">
                    <span>Error Rate:</span>
                    <strong className="text-success">{metrics.errorRate}%</strong>
                  </div>
                </div>
              </CCardBody>
            </CCard>
          </CCol>
        </CRow>
      )}

      {/* Filters */}
      <CRow className="mb-4">
        <CCol>
          <CCard>
            <CCardBody>
              <CRow>
                <CCol md={4}>
                  <Form.Select
                    value={filters.category}
                    onChange={(e) => setFilters(prev => ({ ...prev, category: e.target.value }))}
                  >
                    <option value="all">All Categories</option>
                    <option value="events">Events</option>
                    <option value="users">Users</option>
                    <option value="registrations">Registrations</option>
                    <option value="financial">Financial</option>
                    <option value="system">System</option>
                  </Form.Select>
                </CCol>
                <CCol md={4}>
                  <Form.Select
                    value={filters.dateRange}
                    onChange={(e) => setFilters(prev => ({ ...prev, dateRange: e.target.value }))}
                  >
                    <option value="7d">Last 7 Days</option>
                    <option value="30d">Last 30 Days</option>
                    <option value="90d">Last 90 Days</option>
                    <option value="1y">Last Year</option>
                    <option value="all">All Time</option>
                  </Form.Select>
                </CCol>
                <CCol md={4}>
                  <Form.Select
                    value={filters.format}
                    onChange={(e) => setFilters(prev => ({ ...prev, format: e.target.value }))}
                  >
                    <option value="all">All Formats</option>
                    <option value="pdf">PDF</option>
                    <option value="excel">Excel</option>
                    <option value="csv">CSV</option>
                  </Form.Select>
                </CCol>
              </CRow>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* Reports Table */}
      <CRow>
        <CCol>
          <CCard>
            <CCardHeader>
              <div className="d-flex justify-content-between align-items-center">
                <h5 className="card-title mb-0">Available Reports</h5>
                <CBadge color="light" className="fs-6">
                  {reports.length} templates
                </CBadge>
              </div>
            </CCardHeader>
            
            <CCardBody className="p-0">
              <CTable hover responsive>
                <CTableHead>
                  <CTableRow>
                    <CTableHeaderCell>Report Name</CTableHeaderCell>
                    <CTableHeaderCell>Category</CTableHeaderCell>
                    <CTableHeaderCell>Format</CTableHeaderCell>
                    <CTableHeaderCell>Schedule</CTableHeaderCell>
                    <CTableHeaderCell>Last Generated</CTableHeaderCell>
                    <CTableHeaderCell>Actions</CTableHeaderCell>
                  </CTableRow>
                </CTableHead>
                
                <CTableBody>
                  {reports
                    .filter(report => 
                      (filters.category === 'all' || report.category === filters.category) &&
                      (filters.format === 'all' || report.format === filters.format)
                    )
                    .map(report => (
                      <CTableRow key={report.id}>
                        <CTableDataCell>
                          <div className="fw-semibold">{report.name}</div>
                          <small className="text-muted">{report.description}</small>
                        </CTableDataCell>
                        <CTableDataCell>
                          {getCategoryBadge(report.category)}
                        </CTableDataCell>
                        <CTableDataCell>
                          <div className="d-flex align-items-center">
                            <FontAwesomeIcon 
                              icon={getFormatIcon(report.format)} 
                              className="me-2 text-muted"
                            />
                            {report.format.toUpperCase()}
                          </div>
                        </CTableDataCell>
                        <CTableDataCell>
                          <div className="d-flex align-items-center">
                            {report.schedule && report.schedule !== 'none' ? (
                              <>
                                <FontAwesomeIcon icon={faClock} className="me-1 text-success" />
                                <span className="text-capitalize">{report.schedule}</span>
                              </>
                            ) : (
                              <span className="text-muted">Manual</span>
                            )}
                          </div>
                        </CTableDataCell>
                        <CTableDataCell>
                          {report.lastGenerated ? (
                            <div>
                              <div>{formatDate(report.lastGenerated)}</div>
                              <small className="text-success">Available</small>
                            </div>
                          ) : (
                            <span className="text-muted">Never</span>
                          )}
                        </CTableDataCell>
                        <CTableDataCell>
                          <CButtonGroup size="sm">
                            <CButton
                              color="primary"
                              onClick={() => handleGenerateReport(report.id)}
                              disabled={generating.has(report.id)}
                            >
                              {generating.has(report.id) ? (
                                <FontAwesomeIcon icon={faSync} spin />
                              ) : (
                                <FontAwesomeIcon icon={faPlay} />
                              )}
                            </CButton>
                            
                            {report.lastGenerated && (
                              <CButton color="outline-success" title="Download Latest">
                                <FontAwesomeIcon icon={faCloudDownload} />
                              </CButton>
                            )}
                            
                            <CButton 
                              color="outline-info" 
                              onClick={() => handleScheduleReport(report)}
                              title="Schedule Report"
                            >
                              <FontAwesomeIcon icon={faClock} />
                            </CButton>
                            
                            <CDropdown>
                              <CDropdownToggle color="outline-secondary" caret={false}>
                                <FontAwesomeIcon icon={faCog} />
                              </CDropdownToggle>
                              <CDropdownMenu>
                                <CDropdownItem>
                                  <FontAwesomeIcon icon={faEye} className="me-2" />
                                  Preview
                                </CDropdownItem>
                                <CDropdownItem>
                                  <FontAwesomeIcon icon={faEnvelope} className="me-2" />
                                  Email Report
                                </CDropdownItem>
                                <hr className="dropdown-divider" />
                                <CDropdownItem>
                                  <FontAwesomeIcon icon={faCog} className="me-2" />
                                  Customize
                                </CDropdownItem>
                              </CDropdownMenu>
                            </CDropdown>
                          </CButtonGroup>
                        </CTableDataCell>
                      </CTableRow>
                    ))}
                </CTableBody>
              </CTable>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>
        </Tab.Pane>
        )}

        {activeTab === 'scheduler' && (
        <Tab.Pane role="tabpanel">
          <ReportScheduler 
            onScheduleCreated={() => console.log('Schedule created')}
            onScheduleUpdated={() => console.log('Schedule updated')}
            onScheduleDeleted={() => console.log('Schedule deleted')}
          />
        </Tab.Pane>
        )}
      </Tab.Content>

      {/* Schedule Modal */}
      <Modal show={showScheduleModal} onHide={() => setShowScheduleModal(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Schedule Report: {selectedReport?.name}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div className="mb-3">
            <Form.Label>Frequency</Form.Label>
            <Form.Select
              value={scheduleSettings.frequency}
              onChange={(e) => setScheduleSettings(prev => ({ ...prev, frequency: e.target.value }))}
            >
              <option value="none">Manual Only</option>
              <option value="daily">Daily</option>
              <option value="weekly">Weekly</option>
              <option value="monthly">Monthly</option>
              <option value="quarterly">Quarterly</option>
            </Form.Select>
          </div>
          
          <div className="mb-3">
            <Form.Label>Email Recipients</Form.Label>
            <Form.Control
              type="email"
              placeholder="Enter email addresses (comma-separated)"
              value={scheduleSettings.email}
              onChange={(e) => setScheduleSettings(prev => ({ ...prev, email: e.target.value }))}
            />
          </div>
          
          <div className="mb-3">
            <Form.Label>Notes</Form.Label>
            <Form.Control as="textarea"
              placeholder="Optional description or special instructions"
              value={scheduleSettings.description}
              onChange={(e) => setScheduleSettings(prev => ({ ...prev, description: e.target.value }))}
              rows={3}
            />
          </div>
        </Modal.Body>
        <Modal.Footer>
          <CButton color="secondary" onClick={() => setShowScheduleModal(false)}>
            Cancel
          </CButton>
          <CButton color="primary" onClick={saveScheduleSettings}>
            Save Schedule
          </CButton>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default SystemReports;