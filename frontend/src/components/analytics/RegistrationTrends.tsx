import React, { useState, useEffect } from 'react';
import {
  CRow,
  CCol,
  CCard,
  CCardBody,
  CCardHeader,
  CTable,
  CTableHead,
  CTableRow,
  CTableHeaderCell,
  CTableBody,
  CTableDataCell,
  CButton,
  CButtonGroup,
  CProgress,
  CWidgetStatsF
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
  faUserPlus,
  faCalendarAlt,
  faArrowUp,
  faUsers,
  faChartLine,
  faSync
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

interface RegistrationData {
  totalRegistrations: number;
  dailyRegistrations: number;
  weeklyGrowth: number;
  monthlyGrowth: number;
  conversionRate: number;
  averageTimeToRegister: number;
  peakRegistrationHour: string;
  topReferralSources: Array<{
    source: string;
    registrations: number;
    percentage: number;
  }>;
  registrationsByEventType: Array<{
    type: string;
    count: number;
    percentage: number;
  }>;
  registrationTrends: Array<{
    date: string;
    registrations: number;
    cumulativeRegistrations: number;
  }>;
  demographicBreakdown: {
    ageGroups: Array<{ group: string; count: number; percentage: number }>;
    genderDistribution: Array<{ gender: string; count: number; percentage: number }>;
    locations: Array<{ location: string; count: number; percentage: number }>;
  };
}

const RegistrationTrends: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<RegistrationData | null>(null);
  const [timeframe, setTimeframe] = useState('30d');

  // Mock data - replace with actual API calls
  const mockData: RegistrationData = {
    totalRegistrations: 2847,
    dailyRegistrations: 47,
    weeklyGrowth: 12.5,
    monthlyGrowth: 28.3,
    conversionRate: 67.8,
    averageTimeToRegister: 3.2,
    peakRegistrationHour: "2:00 PM - 3:00 PM",
    topReferralSources: [
      { source: "Direct", registrations: 1243, percentage: 43.7 },
      { source: "Social Media", registrations: 682, percentage: 24.0 },
      { source: "Email Campaign", registrations: 465, percentage: 16.3 },
      { source: "Google Search", registrations: 287, percentage: 10.1 },
      { source: "Partner Sites", registrations: 170, percentage: 6.0 }
    ],
    registrationsByEventType: [
      { type: "Corporate Training", count: 1156, percentage: 40.6 },
      { type: "Conferences", count: 825, percentage: 29.0 },
      { type: "Workshops", count: 487, percentage: 17.1 },
      { type: "Networking Events", count: 248, percentage: 8.7 },
      { type: "Webinars", count: 131, percentage: 4.6 }
    ],
    registrationTrends: [
      { date: "2024-01-01", registrations: 45, cumulativeRegistrations: 1456 },
      { date: "2024-01-02", registrations: 52, cumulativeRegistrations: 1508 },
      { date: "2024-01-03", registrations: 38, cumulativeRegistrations: 1546 },
      { date: "2024-01-04", registrations: 67, cumulativeRegistrations: 1613 },
      { date: "2024-01-05", registrations: 73, cumulativeRegistrations: 1686 },
      { date: "2024-01-06", registrations: 41, cumulativeRegistrations: 1727 },
      { date: "2024-01-07", registrations: 59, cumulativeRegistrations: 1786 }
    ],
    demographicBreakdown: {
      ageGroups: [
        { group: "18-25", count: 342, percentage: 12.0 },
        { group: "26-35", count: 1139, percentage: 40.0 },
        { group: "36-45", count: 854, percentage: 30.0 },
        { group: "46-55", count: 369, percentage: 13.0 },
        { group: "56+", count: 143, percentage: 5.0 }
      ],
      genderDistribution: [
        { gender: "Female", count: 1564, percentage: 54.9 },
        { gender: "Male", count: 1226, percentage: 43.1 },
        { gender: "Other/Prefer not to say", count: 57, percentage: 2.0 }
      ],
      locations: [
        { location: "United States", count: 1708, percentage: 60.0 },
        { location: "Canada", count: 456, percentage: 16.0 },
        { location: "United Kingdom", count: 285, percentage: 10.0 },
        { location: "Australia", count: 228, percentage: 8.0 },
        { location: "Other", count: 170, percentage: 6.0 }
      ]
    }
  };

  useEffect(() => {
    const loadRegistrationData = async () => {
      setLoading(true);
      try {
        // Simulate API call
        await new Promise(resolve => setTimeout(resolve, 800));
        setData(mockData);
      } catch (error) {
        console.error('Error loading registration data:', error);
      } finally {
        setLoading(false);
      }
    };

    loadRegistrationData();
  }, [timeframe]);

  const getGrowthIcon = (growth: number) => {
    return growth > 0 ? (
      <FontAwesomeIcon icon={faArrowUp} className="text-success" />
    ) : (
      <FontAwesomeIcon icon={faArrowUp} className="text-danger" style={{ transform: 'rotate(180deg)' }} />
    );
  };

  // Chart configurations
  const registrationTrendsChartData = {
    labels: data?.registrationTrends.map(item => new Date(item.date).toLocaleDateString()) || [],
    datasets: [
      {
        label: 'Daily Registrations',
        data: data?.registrationTrends.map(item => item.registrations) || [],
        borderColor: 'rgb(75, 192, 192)',
        backgroundColor: 'rgba(75, 192, 192, 0.2)',
        tension: 0.1,
      },
      {
        label: 'Cumulative Registrations',
        data: data?.registrationTrends.map(item => item.cumulativeRegistrations) || [],
        borderColor: 'rgb(255, 99, 132)',
        backgroundColor: 'rgba(255, 99, 132, 0.2)',
        yAxisID: 'y1',
        tension: 0.1,
      }
    ],
  };

  const registrationTrendsChartOptions = {
    responsive: true,
    interaction: {
      mode: 'index' as const,
      intersect: false,
    },
    scales: {
      y: {
        type: 'linear' as const,
        display: true,
        position: 'left' as const,
        title: {
          display: true,
          text: 'Daily Registrations'
        }
      },
      y1: {
        type: 'linear' as const,
        display: true,
        position: 'right' as const,
        title: {
          display: true,
          text: 'Cumulative Total'
        },
        grid: {
          drawOnChartArea: false,
        },
      },
    },
    plugins: {
      legend: {
        position: 'top' as const,
      },
      title: {
        display: true,
        text: 'Registration Trends Over Time',
      },
    },
  };

  const eventTypeChartData = {
    labels: data?.registrationsByEventType.map(item => item.type) || [],
    datasets: [
      {
        data: data?.registrationsByEventType.map(item => item.count) || [],
        backgroundColor: [
          'rgba(255, 99, 132, 0.8)',
          'rgba(54, 162, 235, 0.8)',
          'rgba(255, 205, 86, 0.8)',
          'rgba(75, 192, 192, 0.8)',
          'rgba(153, 102, 255, 0.8)',
        ],
        borderColor: [
          'rgba(255, 99, 132, 1)',
          'rgba(54, 162, 235, 1)',
          'rgba(255, 205, 86, 1)',
          'rgba(75, 192, 192, 1)',
          'rgba(153, 102, 255, 1)',
        ],
        borderWidth: 1,
      },
    ],
  };

  const ageGroupChartData = {
    labels: data?.demographicBreakdown.ageGroups.map(item => item.group) || [],
    datasets: [
      {
        label: 'Registrations by Age Group',
        data: data?.demographicBreakdown.ageGroups.map(item => item.count) || [],
        backgroundColor: 'rgba(54, 162, 235, 0.8)',
        borderColor: 'rgba(54, 162, 235, 1)',
        borderWidth: 1,
      },
    ],
  };

  if (loading || !data) {
    return (
      <div className="d-flex justify-content-center align-items-center min-vh-100">
        <div className="text-center">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="mt-2 text-medium-emphasis">Loading registration analytics...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="animated fadeIn">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="mb-1">
            <FontAwesomeIcon icon={faChartLine} className="me-2" />
            Registration Trends
          </h2>
          <p className="text-medium-emphasis">Track and analyze registration patterns and performance</p>
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
          </CButtonGroup>
          <CButton color="secondary" variant="outline">
            <FontAwesomeIcon icon={faSync} className="me-1" />
            Refresh
          </CButton>
        </div>
      </div>

      {/* Key Metrics */}
      <CRow className="mb-4">
        <CCol xl={3} lg={6} md={6} sm={6}>
          <CWidgetStatsF
            className="mb-3"
            icon={<FontAwesomeIcon icon={faUsers} size="xl" />}
            value={data.totalRegistrations.toLocaleString()}
            title="Total Registrations"
            color="primary"
          />
        </CCol>
        <CCol xl={3} lg={6} md={6} sm={6}>
          <CWidgetStatsF
            className="mb-3"
            icon={<FontAwesomeIcon icon={faUserPlus} size="xl" />}
            value={data.dailyRegistrations}
            title="Daily Registrations"
            color="info"
          />
        </CCol>
        <CCol xl={3} lg={6} md={6} sm={6}>
          <CWidgetStatsF
            className="mb-3"
            icon={getGrowthIcon(data.weeklyGrowth)}
            value={`${data.weeklyGrowth}%`}
            title="Weekly Growth"
            color="success"
          />
        </CCol>
        <CCol xl={3} lg={6} md={6} sm={6}>
          <CWidgetStatsF
            className="mb-3"
            icon={<FontAwesomeIcon icon={faCalendarAlt} size="xl" />}
            value={`${data.conversionRate}%`}
            title="Conversion Rate"
            color="warning"
          />
        </CCol>
      </CRow>

      {/* Registration Trends Chart */}
      <CRow className="mb-4">
        <CCol>
          <CCard>
            <CCardHeader>
              <h5 className="mb-0">Registration Trends Over Time</h5>
            </CCardHeader>
            <CCardBody>
              <div className="chart-container">
                <Line data={registrationTrendsChartData} options={registrationTrendsChartOptions} />
              </div>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* Event Type Distribution and Age Demographics */}
      <CRow className="mb-4">
        <CCol lg={6}>
          <CCard>
            <CCardHeader>
              <h5 className="mb-0">Registrations by Event Type</h5>
            </CCardHeader>
            <CCardBody>
              <div className="chart-container">
                <Doughnut data={eventTypeChartData} />
              </div>
            </CCardBody>
          </CCard>
        </CCol>
        <CCol lg={6}>
          <CCard>
            <CCardHeader>
              <h5 className="mb-0">Age Group Distribution</h5>
            </CCardHeader>
            <CCardBody>
              <div className="chart-container">
                <Bar data={ageGroupChartData} />
              </div>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* Detailed Analytics Tables */}
      <CRow className="mb-4">
        <CCol lg={6}>
          <CCard>
            <CCardHeader>
              <h5 className="mb-0">Top Referral Sources</h5>
            </CCardHeader>
            <CCardBody>
              <CTable hover responsive>
                <CTableHead>
                  <CTableRow>
                    <CTableHeaderCell>Source</CTableHeaderCell>
                    <CTableHeaderCell>Registrations</CTableHeaderCell>
                    <CTableHeaderCell>Percentage</CTableHeaderCell>
                    <CTableHeaderCell>Progress</CTableHeaderCell>
                  </CTableRow>
                </CTableHead>
                <CTableBody>
                  {data.topReferralSources.map((source, index) => (
                    <CTableRow key={index}>
                      <CTableDataCell>
                        <strong>{source.source}</strong>
                      </CTableDataCell>
                      <CTableDataCell>{source.registrations.toLocaleString()}</CTableDataCell>
                      <CTableDataCell>{source.percentage}%</CTableDataCell>
                      <CTableDataCell>
                        <CProgress value={source.percentage} color="primary" />
                      </CTableDataCell>
                    </CTableRow>
                  ))}
                </CTableBody>
              </CTable>
            </CCardBody>
          </CCard>
        </CCol>
        <CCol lg={6}>
          <CCard>
            <CCardHeader>
              <h5 className="mb-0">Geographic Distribution</h5>
            </CCardHeader>
            <CCardBody>
              <CTable hover responsive>
                <CTableHead>
                  <CTableRow>
                    <CTableHeaderCell>Location</CTableHeaderCell>
                    <CTableHeaderCell>Registrations</CTableHeaderCell>
                    <CTableHeaderCell>Percentage</CTableHeaderCell>
                    <CTableHeaderCell>Progress</CTableHeaderCell>
                  </CTableRow>
                </CTableHead>
                <CTableBody>
                  {data.demographicBreakdown.locations.map((location, index) => (
                    <CTableRow key={index}>
                      <CTableDataCell>
                        <strong>{location.location}</strong>
                      </CTableDataCell>
                      <CTableDataCell>{location.count.toLocaleString()}</CTableDataCell>
                      <CTableDataCell>{location.percentage}%</CTableDataCell>
                      <CTableDataCell>
                        <CProgress value={location.percentage} color="info" />
                      </CTableDataCell>
                    </CTableRow>
                  ))}
                </CTableBody>
              </CTable>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* Additional Insights */}
      <CRow>
        <CCol lg={4}>
          <CCard>
            <CCardHeader>
              <h5 className="mb-0">Registration Insights</h5>
            </CCardHeader>
            <CCardBody>
              <div className="mb-3">
                <small className="text-medium-emphasis">Average Time to Register</small>
                <div className="fs-4 fw-bold text-primary">{data.averageTimeToRegister} minutes</div>
              </div>
              <div className="mb-3">
                <small className="text-medium-emphasis">Peak Registration Time</small>
                <div className="fs-6 fw-bold">{data.peakRegistrationHour}</div>
              </div>
              <div>
                <small className="text-medium-emphasis">Monthly Growth Rate</small>
                <div className="fs-4 fw-bold text-success">+{data.monthlyGrowth}%</div>
              </div>
            </CCardBody>
          </CCard>
        </CCol>
        <CCol lg={8}>
          <CCard>
            <CCardHeader>
              <h5 className="mb-0">Gender Distribution</h5>
            </CCardHeader>
            <CCardBody>
              {data.demographicBreakdown.genderDistribution.map((gender, index) => (
                <div key={index} className="d-flex justify-content-between align-items-center mb-3">
                  <div>
                    <strong>{gender.gender}</strong>
                    <small className="text-medium-emphasis ms-2">({gender.count.toLocaleString()})</small>
                  </div>
                  <div className="flex-grow-1 mx-3">
                    <CProgress value={gender.percentage} color={index === 0 ? 'success' : index === 1 ? 'primary' : 'secondary'} />
                  </div>
                  <div className="text-end">
                    <strong>{gender.percentage}%</strong>
                  </div>
                </div>
              ))}
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>
    </div>
  );
};

export default RegistrationTrends;