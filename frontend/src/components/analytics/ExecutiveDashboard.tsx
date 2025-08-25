import React, { useState, useEffect, useCallback } from 'react';
import {
  CRow,
  CCol,
  CCard,
  CCardBody,
  CCardHeader,
  CWidgetStatsF,
  CProgress,
  CTable,
  CTableHead,
  CTableRow,
  CTableHeaderCell,
  CTableBody,
  CTableDataCell,
  CBadge,
  CButton,
  CButtonGroup,
  CDropdown,
  CDropdownToggle,
  CDropdownMenu,
  CDropdownItem
} from '@coreui/react';
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
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faUsers,
  faCalendarAlt,
  faDollarSign,
  faChartLine,
  faUserCheck,
  faArrowUp,
  faArrowDown,
  faDownload,
  faSync,
} from '@fortawesome/free-solid-svg-icons';

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

interface ExecutiveMetrics {
  totalEvents: number;
  totalRegistrations: number;
  totalRevenue: number;
  attendanceRate: number;
  activeEvents: number;
  upcomingEvents: number;
  completedEvents: number;
  avgEventCapacity: number;
  registrationTrend: number;
  revenueTrend: number;
}

interface EventSummary {
  id: string;
  title: string;
  registrations: number;
  capacity: number;
  attendanceRate: number;
  revenue: number;
  status: 'active' | 'upcoming' | 'completed';
  startDate: string;
}

interface ChartData {
  labels: string[];
  registrationData: number[];
  revenueData: number[];
  attendanceData: number[];
}

const ExecutiveDashboard: React.FC = () => {
  const [metrics, setMetrics] = useState<ExecutiveMetrics | null>(null);
  const [topEvents, setTopEvents] = useState<EventSummary[]>([]);
  const [chartData, setChartData] = useState<ChartData | null>(null);
  const [loading, setLoading] = useState(true);
  const [timeframe, setTimeframe] = useState<'7d' | '30d' | '90d' | '1y'>('30d');
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

  const loadExecutiveMetrics = async (): Promise<ExecutiveMetrics> => {
    const apiClient = (await import('../../api/apiClient')).default;
    const response = await apiClient.get(`/analytics/executive?timeframe=${timeframe}`);
    return response.data;
  };

  const loadTopEvents = async (): Promise<EventSummary[]> => {
    const apiClient = (await import('../../api/apiClient')).default;
    const response = await apiClient.get('/analytics/executive/events?limit=5');
    return response.data;
  };

  const loadChartData = async (): Promise<ChartData> => {
    const apiClient = (await import('../../api/apiClient')).default;
    const response = await apiClient.get(`/analytics/executive/charts?timeframe=${timeframe}`);
    return response.data;
  };

  const loadDashboardData = useCallback(async () => {
    setLoading(true);
    try {
      // Use real API endpoints
      const [metricsResponse, eventsResponse, chartsResponse] = await Promise.all([
        loadExecutiveMetrics(),
        loadTopEvents(),
        loadChartData()
      ]);

      setMetrics(metricsResponse);
      setTopEvents(eventsResponse);
      setChartData(chartsResponse);
      setLastUpdated(new Date());
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    } finally {
      setLoading(false);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [timeframe]);

  useEffect(() => {
    loadDashboardData();
  }, [timeframe, loadDashboardData]);


  const registrationTrendData = chartData ? {
    labels: chartData.labels,
    datasets: [
      {
        label: 'Registrations',
        data: chartData.registrationData,
        borderColor: 'rgb(54, 162, 235)',
        backgroundColor: 'rgba(54, 162, 235, 0.1)',
        fill: true,
        tension: 0.4,
      },
    ],
  } : { labels: [], datasets: [] };

  const revenueComparisonData = chartData ? {
    labels: chartData.labels,
    datasets: [
      {
        label: 'Revenue ($)',
        data: chartData.revenueData,
        backgroundColor: 'rgba(75, 192, 192, 0.8)',
        borderColor: 'rgba(75, 192, 192, 1)',
        borderWidth: 1,
      },
    ],
  } : { labels: [], datasets: [] };

  const attendanceDistributionData = {
    labels: ['Excellent (90%+)', 'Good (80-89%)', 'Average (70-79%)', 'Poor (<70%)'],
    datasets: [
      {
        data: [45, 35, 15, 5],
        backgroundColor: [
          '#28a745',
          '#17a2b8', 
          '#ffc107',
          '#dc3545'
        ],
        borderWidth: 2,
        borderColor: '#fff'
      },
    ],
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'active':
        return <CBadge color="success">Active</CBadge>;
      case 'upcoming':
        return <CBadge color="warning">Upcoming</CBadge>;
      case 'completed':
        return <CBadge color="secondary">Completed</CBadge>;
      default:
        return <CBadge color="light">Unknown</CBadge>;
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
    }).format(amount);
  };

  const getTrendIcon = (trend: number) => {
    return trend > 0 ? (
      <FontAwesomeIcon icon={faArrowUp} className="text-success" />
    ) : (
      <FontAwesomeIcon icon={faArrowDown} className="text-danger" />
    );
  };

  if (loading || !metrics) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ height: '200px' }}>
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="animated fadeIn">
      {/* Header */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h2 mb-0">Executive Dashboard</h1>
          <p className="text-medium-emphasis mb-0">
            Last updated: {lastUpdated.toLocaleTimeString()}
          </p>
        </div>
        <div className="d-flex gap-2">
          <CButtonGroup>
            <CButton
              color={timeframe === '7d' ? 'primary' : 'outline-primary'}
              onClick={() => setTimeframe('7d')}
            >
              7 Days
            </CButton>
            <CButton
              color={timeframe === '30d' ? 'primary' : 'outline-primary'}
              onClick={() => setTimeframe('30d')}
            >
              30 Days
            </CButton>
            <CButton
              color={timeframe === '90d' ? 'primary' : 'outline-primary'}
              onClick={() => setTimeframe('90d')}
            >
              90 Days
            </CButton>
            <CButton
              color={timeframe === '1y' ? 'primary' : 'outline-primary'}
              onClick={() => setTimeframe('1y')}
            >
              1 Year
            </CButton>
          </CButtonGroup>
          
          <CButton color="outline-secondary" onClick={loadDashboardData}>
            <FontAwesomeIcon icon={faSync} className="me-1" />
            Refresh
          </CButton>
          
          <CDropdown>
            <CDropdownToggle color="primary">
              <FontAwesomeIcon icon={faDownload} className="me-1" />
              Export
            </CDropdownToggle>
            <CDropdownMenu>
              <CDropdownItem>PDF Report</CDropdownItem>
              <CDropdownItem>Excel Export</CDropdownItem>
              <CDropdownItem>CSV Data</CDropdownItem>
            </CDropdownMenu>
          </CDropdown>
        </div>
      </div>

      {/* Key Performance Indicators */}
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
            title="Total Registrations"
            value={metrics.totalRegistrations.toLocaleString()}
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
            title="Attendance Rate"
            value={`${metrics.attendanceRate}%`}
            color="warning"
          />
        </CCol>
      </CRow>

      {/* Detailed Metrics */}
      <CRow className="mb-4">
        <CCol lg={3}>
          <CCard className="mb-4">
            <CCardBody>
              <div className="d-flex justify-content-between">
                <div>
                  <h6 className="card-title text-medium-emphasis">Active Events</h6>
                  <div className="fs-4 fw-semibold">{metrics.activeEvents}</div>
                </div>
                <div className="bg-primary bg-opacity-25 text-primary p-3 rounded">
                  <FontAwesomeIcon icon={faChartLine} size="lg" />
                </div>
              </div>
              <div className="d-flex align-items-center mt-3">
                {getTrendIcon(metrics.registrationTrend)}
                <span className="text-success ms-2">
                  {Math.abs(metrics.registrationTrend)}% from last period
                </span>
              </div>
            </CCardBody>
          </CCard>
        </CCol>

        <CCol lg={3}>
          <CCard className="mb-4">
            <CCardBody>
              <div className="d-flex justify-content-between">
                <div>
                  <h6 className="card-title text-medium-emphasis">Upcoming Events</h6>
                  <div className="fs-4 fw-semibold">{metrics.upcomingEvents}</div>
                </div>
                <div className="bg-warning bg-opacity-25 text-warning p-3 rounded">
                  <FontAwesomeIcon icon={faCalendarAlt} size="lg" />
                </div>
              </div>
              <CProgress className="mt-3" value={75} color="warning" />
              <small className="text-medium-emphasis">Pipeline health: Good</small>
            </CCardBody>
          </CCard>
        </CCol>

        <CCol lg={3}>
          <CCard className="mb-4">
            <CCardBody>
              <div className="d-flex justify-content-between">
                <div>
                  <h6 className="card-title text-medium-emphasis">Avg Capacity</h6>
                  <div className="fs-4 fw-semibold">{metrics.avgEventCapacity}%</div>
                </div>
                <div className="bg-success bg-opacity-25 text-success p-3 rounded">
                  <FontAwesomeIcon icon={faUsers} size="lg" />
                </div>
              </div>
              <CProgress className="mt-3" value={metrics.avgEventCapacity} color="success" />
              <small className="text-medium-emphasis">Utilization rate</small>
            </CCardBody>
          </CCard>
        </CCol>

        <CCol lg={3}>
          <CCard className="mb-4">
            <CCardBody>
              <div className="d-flex justify-content-between">
                <div>
                  <h6 className="card-title text-medium-emphasis">Revenue Growth</h6>
                  <div className="fs-4 fw-semibold">+{metrics.revenueTrend}%</div>
                </div>
                <div className="bg-info bg-opacity-25 text-info p-3 rounded">
                  <FontAwesomeIcon icon={faDollarSign} size="lg" />
                </div>
              </div>
              <div className="d-flex align-items-center mt-3">
                {getTrendIcon(metrics.revenueTrend)}
                <span className="text-success ms-2">
                  vs. previous {timeframe}
                </span>
              </div>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* Charts Row */}
      <CRow className="mb-4">
        <CCol lg={8}>
          <CCard>
            <CCardHeader>
              <h5 className="card-title mb-0">Registration Trends</h5>
            </CCardHeader>
            <CCardBody>
              <Line 
                data={registrationTrendData} 
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
              <h5 className="card-title mb-0">Attendance Distribution</h5>
            </CCardHeader>
            <CCardBody>
              <Doughnut 
                data={attendanceDistributionData}
                options={{
                  responsive: true,
                  plugins: {
                    legend: {
                      position: 'bottom' as const,
                    },
                  },
                }}
              />
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* Revenue Chart */}
      <CRow className="mb-4">
        <CCol>
          <CCard>
            <CCardHeader>
              <h5 className="card-title mb-0">Revenue Performance</h5>
            </CCardHeader>
            <CCardBody>
              <Bar 
                data={revenueComparisonData} 
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
      </CRow>

      {/* Top Events Table */}
      <CRow>
        <CCol>
          <CCard>
            <CCardHeader>
              <h5 className="card-title mb-0">Top Performing Events</h5>
            </CCardHeader>
            <CCardBody className="p-0">
              <CTable hover responsive>
                <CTableHead>
                  <CTableRow>
                    <CTableHeaderCell>Event Name</CTableHeaderCell>
                    <CTableHeaderCell>Status</CTableHeaderCell>
                    <CTableHeaderCell>Registrations</CTableHeaderCell>
                    <CTableHeaderCell>Capacity</CTableHeaderCell>
                    <CTableHeaderCell>Attendance Rate</CTableHeaderCell>
                    <CTableHeaderCell>Revenue</CTableHeaderCell>
                    <CTableHeaderCell>Start Date</CTableHeaderCell>
                  </CTableRow>
                </CTableHead>
                <CTableBody>
                  {topEvents.map((event, index) => (
                    <CTableRow key={event.id}>
                      <CTableDataCell>
                        <div className="fw-semibold">{event.title}</div>
                      </CTableDataCell>
                      <CTableDataCell>
                        {getStatusBadge(event.status)}
                      </CTableDataCell>
                      <CTableDataCell>
                        <strong>{event.registrations}</strong>
                      </CTableDataCell>
                      <CTableDataCell>
                        {event.registrations}/{event.capacity}
                        <CProgress 
                          className="mt-1" 
                          value={(event.registrations / event.capacity) * 100} 
                          height={4}
                          color="info"
                        />
                      </CTableDataCell>
                      <CTableDataCell>
                        <span className={`fw-semibold ${event.attendanceRate >= 90 ? 'text-success' : event.attendanceRate >= 80 ? 'text-warning' : 'text-danger'}`}>
                          {event.attendanceRate}%
                        </span>
                      </CTableDataCell>
                      <CTableDataCell>
                        {formatCurrency(event.revenue)}
                      </CTableDataCell>
                      <CTableDataCell>
                        {new Date(event.startDate).toLocaleDateString()}
                      </CTableDataCell>
                    </CTableRow>
                  ))}
                </CTableBody>
              </CTable>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>
    </div>
  );
};

export default ExecutiveDashboard;