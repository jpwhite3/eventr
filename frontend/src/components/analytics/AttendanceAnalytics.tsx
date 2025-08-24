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
  CWidgetStatsF,
  CBadge
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
  faUserCheck,
  faUserTimes,
  faClipboardCheck,
  faClock,
  faCalendarCheck,
  faPercentage,
  faSync,
  faArrowUp,
  faArrowDown
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

interface AttendanceData {
  totalAttendees: number;
  totalRegistered: number;
  attendanceRate: number;
  noShowRate: number;
  lateArrivals: number;
  earlyDepartures: number;
  averageCheckInTime: string;
  peakCheckInHour: string;
  attendanceByEvent: Array<{
    eventName: string;
    registered: number;
    attended: number;
    attendanceRate: number;
    noShowRate: number;
  }>;
  attendanceTrends: Array<{
    date: string;
    registered: number;
    attended: number;
    noShows: number;
  }>;
  checkInTiming: Array<{
    timeSlot: string;
    checkIns: number;
    percentage: number;
  }>;
  attendanceByEventType: Array<{
    type: string;
    totalEvents: number;
    averageAttendance: number;
    attendanceRate: number;
  }>;
  demographicAttendance: {
    byAge: Array<{ ageGroup: string; attendanceRate: number; count: number }>;
    byGender: Array<{ gender: string; attendanceRate: number; count: number }>;
    byLocation: Array<{ location: string; attendanceRate: number; count: number }>;
  };
  attendanceQuality: {
    onTimeArrivals: number;
    lateArrivals: number;
    earlyDepartures: number;
    stayedFullEvent: number;
  };
}

const AttendanceAnalytics: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<AttendanceData | null>(null);
  const [timeframe, setTimeframe] = useState('30d');

  // Mock data - replace with actual API calls
  const mockData: AttendanceData = {
    totalAttendees: 2347,
    totalRegistered: 2847,
    attendanceRate: 82.4,
    noShowRate: 17.6,
    lateArrivals: 156,
    earlyDepartures: 89,
    averageCheckInTime: "9:15 AM",
    peakCheckInHour: "9:00 AM - 10:00 AM",
    attendanceByEvent: [
      { eventName: "Corporate Leadership Summit", registered: 450, attended: 412, attendanceRate: 91.6, noShowRate: 8.4 },
      { eventName: "Digital Transformation Workshop", registered: 280, attended: 234, attendanceRate: 83.6, noShowRate: 16.4 },
      { eventName: "Quarterly All-Hands Meeting", registered: 520, attended: 467, attendanceRate: 89.8, noShowRate: 10.2 },
      { eventName: "Product Launch Event", registered: 350, attended: 298, attendanceRate: 85.1, noShowRate: 14.9 },
      { eventName: "Training Bootcamp Series", registered: 180, attended: 132, attendanceRate: 73.3, noShowRate: 26.7 }
    ],
    attendanceTrends: [
      { date: "2024-01-01", registered: 125, attended: 103, noShows: 22 },
      { date: "2024-01-02", registered: 98, attended: 84, noShows: 14 },
      { date: "2024-01-03", registered: 156, attended: 134, noShows: 22 },
      { date: "2024-01-04", registered: 203, attended: 178, noShows: 25 },
      { date: "2024-01-05", registered: 178, attended: 152, noShows: 26 },
      { date: "2024-01-06", registered: 234, attended: 201, noShows: 33 },
      { date: "2024-01-07", registered: 189, attended: 167, noShows: 22 }
    ],
    checkInTiming: [
      { timeSlot: "Before 8:00 AM", checkIns: 127, percentage: 5.4 },
      { timeSlot: "8:00 - 9:00 AM", checkIns: 456, percentage: 19.4 },
      { timeSlot: "9:00 - 10:00 AM", checkIns: 834, percentage: 35.5 },
      { timeSlot: "10:00 - 11:00 AM", checkIns: 523, percentage: 22.3 },
      { timeSlot: "11:00 AM - 12:00 PM", checkIns: 287, percentage: 12.2 },
      { timeSlot: "After 12:00 PM", checkIns: 120, percentage: 5.1 }
    ],
    attendanceByEventType: [
      { type: "Training Sessions", totalEvents: 45, averageAttendance: 78.5, attendanceRate: 78.5 },
      { type: "Corporate Events", totalEvents: 28, averageAttendance: 156.8, attendanceRate: 89.2 },
      { type: "Workshops", totalEvents: 67, averageAttendance: 34.2, attendanceRate: 85.7 },
      { type: "Conferences", totalEvents: 12, averageAttendance: 234.5, attendanceRate: 91.3 },
      { type: "Networking Events", totalEvents: 34, averageAttendance: 67.3, attendanceRate: 76.8 }
    ],
    demographicAttendance: {
      byAge: [
        { ageGroup: "18-25", attendanceRate: 74.2, count: 342 },
        { ageGroup: "26-35", attendanceRate: 86.7, count: 1139 },
        { ageGroup: "36-45", attendanceRate: 89.3, count: 854 },
        { ageGroup: "46-55", attendanceRate: 91.2, count: 369 },
        { ageGroup: "56+", attendanceRate: 88.1, count: 143 }
      ],
      byGender: [
        { gender: "Female", attendanceRate: 84.7, count: 1564 },
        { gender: "Male", attendanceRate: 79.8, count: 1226 },
        { gender: "Other/Prefer not to say", attendanceRate: 82.5, count: 57 }
      ],
      byLocation: [
        { location: "United States", attendanceRate: 83.2, count: 1708 },
        { location: "Canada", attendanceRate: 81.9, count: 456 },
        { location: "United Kingdom", attendanceRate: 85.1, count: 285 },
        { location: "Australia", attendanceRate: 79.7, count: 228 },
        { location: "Other", attendanceRate: 77.6, count: 170 }
      ]
    },
    attendanceQuality: {
      onTimeArrivals: 1876,
      lateArrivals: 312,
      earlyDepartures: 178,
      stayedFullEvent: 1998
    }
  };

  useEffect(() => {
    const loadAttendanceData = async () => {
      setLoading(true);
      try {
        // Simulate API call
        await new Promise(resolve => setTimeout(resolve, 800));
        setData(mockData);
      } catch (error) {
        console.error('Error loading attendance data:', error);
      } finally {
        setLoading(false);
      }
    };

    loadAttendanceData();
  }, [timeframe]);

  const getAttendanceBadge = (rate: number) => {
    if (rate >= 90) return <CBadge color="success">Excellent</CBadge>;
    if (rate >= 80) return <CBadge color="primary">Good</CBadge>;
    if (rate >= 70) return <CBadge color="warning">Fair</CBadge>;
    return <CBadge color="danger">Poor</CBadge>;
  };

  const getTrendIcon = (current: number, previous: number) => {
    const trend = current - previous;
    return trend > 0 ? (
      <FontAwesomeIcon icon={faArrowUp} className="text-success" />
    ) : (
      <FontAwesomeIcon icon={faArrowDown} className="text-danger" />
    );
  };

  // Chart configurations
  const attendanceTrendsChartData = {
    labels: data?.attendanceTrends.map(item => new Date(item.date).toLocaleDateString()) || [],
    datasets: [
      {
        label: 'Registered',
        data: data?.attendanceTrends.map(item => item.registered) || [],
        borderColor: 'rgb(54, 162, 235)',
        backgroundColor: 'rgba(54, 162, 235, 0.2)',
        tension: 0.1,
      },
      {
        label: 'Attended',
        data: data?.attendanceTrends.map(item => item.attended) || [],
        borderColor: 'rgb(75, 192, 192)',
        backgroundColor: 'rgba(75, 192, 192, 0.2)',
        tension: 0.1,
      },
      {
        label: 'No Shows',
        data: data?.attendanceTrends.map(item => item.noShows) || [],
        borderColor: 'rgb(255, 99, 132)',
        backgroundColor: 'rgba(255, 99, 132, 0.2)',
        tension: 0.1,
      }
    ],
  };

  const attendanceTrendsChartOptions = {
    responsive: true,
    interaction: {
      mode: 'index' as const,
      intersect: false,
    },
    plugins: {
      legend: {
        position: 'top' as const,
      },
      title: {
        display: true,
        text: 'Attendance Trends Over Time',
      },
    },
  };

  const checkInTimingChartData = {
    labels: data?.checkInTiming.map(item => item.timeSlot) || [],
    datasets: [
      {
        label: 'Check-ins',
        data: data?.checkInTiming.map(item => item.checkIns) || [],
        backgroundColor: [
          'rgba(255, 99, 132, 0.8)',
          'rgba(54, 162, 235, 0.8)',
          'rgba(255, 205, 86, 0.8)',
          'rgba(75, 192, 192, 0.8)',
          'rgba(153, 102, 255, 0.8)',
          'rgba(255, 159, 64, 0.8)',
        ],
        borderColor: [
          'rgba(255, 99, 132, 1)',
          'rgba(54, 162, 235, 1)',
          'rgba(255, 205, 86, 1)',
          'rgba(75, 192, 192, 1)',
          'rgba(153, 102, 255, 1)',
          'rgba(255, 159, 64, 1)',
        ],
        borderWidth: 1,
      },
    ],
  };

  const eventTypeAttendanceChartData = {
    labels: data?.attendanceByEventType.map(item => item.type) || [],
    datasets: [
      {
        label: 'Average Attendance Rate (%)',
        data: data?.attendanceByEventType.map(item => item.attendanceRate) || [],
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
          <p className="mt-2 text-medium-emphasis">Loading attendance analytics...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="animated fadeIn">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="mb-1">
            <FontAwesomeIcon icon={faClipboardCheck} className="me-2" />
            Attendance Analytics
          </h2>
          <p className="text-medium-emphasis">Monitor and analyze event attendance patterns and performance</p>
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
            icon={<FontAwesomeIcon icon={faUserCheck} size="xl" />}
            value={data.totalAttendees.toLocaleString()}
            title="Total Attendees"
            color="primary"
          />
        </CCol>
        <CCol xl={3} lg={6} md={6} sm={6}>
          <CWidgetStatsF
            className="mb-3"
            icon={<FontAwesomeIcon icon={faPercentage} size="xl" />}
            value={`${data.attendanceRate}%`}
            title="Attendance Rate"
            color="success"
          />
        </CCol>
        <CCol xl={3} lg={6} md={6} sm={6}>
          <CWidgetStatsF
            className="mb-3"
            icon={<FontAwesomeIcon icon={faUserTimes} size="xl" />}
            value={`${data.noShowRate}%`}
            title="No-Show Rate"
            color="danger"
          />
        </CCol>
        <CCol xl={3} lg={6} md={6} sm={6}>
          <CWidgetStatsF
            className="mb-3"
            icon={<FontAwesomeIcon icon={faClock} size="xl" />}
            value={data.averageCheckInTime}
            title="Avg Check-in Time"
            color="info"
          />
        </CCol>
      </CRow>

      {/* Attendance Trends Chart */}
      <CRow className="mb-4">
        <CCol>
          <CCard>
            <CCardHeader>
              <h5 className="mb-0">Attendance Trends Over Time</h5>
            </CCardHeader>
            <CCardBody>
              <div className="chart-container">
                <Line data={attendanceTrendsChartData} options={attendanceTrendsChartOptions} />
              </div>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* Check-in Timing and Event Type Performance */}
      <CRow className="mb-4">
        <CCol lg={6}>
          <CCard>
            <CCardHeader>
              <h5 className="mb-0">Check-in Time Distribution</h5>
            </CCardHeader>
            <CCardBody>
              <div className="chart-container">
                <Bar data={checkInTimingChartData} />
              </div>
            </CCardBody>
          </CCard>
        </CCol>
        <CCol lg={6}>
          <CCard>
            <CCardHeader>
              <h5 className="mb-0">Attendance by Event Type</h5>
            </CCardHeader>
            <CCardBody>
              <div className="chart-container">
                <Bar data={eventTypeAttendanceChartData} />
              </div>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* Event Performance Table */}
      <CRow className="mb-4">
        <CCol>
          <CCard>
            <CCardHeader>
              <h5 className="mb-0">Event Performance Summary</h5>
            </CCardHeader>
            <CCardBody>
              <CTable hover responsive>
                <CTableHead>
                  <CTableRow>
                    <CTableHeaderCell>Event Name</CTableHeaderCell>
                    <CTableHeaderCell>Registered</CTableHeaderCell>
                    <CTableHeaderCell>Attended</CTableHeaderCell>
                    <CTableHeaderCell>Attendance Rate</CTableHeaderCell>
                    <CTableHeaderCell>No-Show Rate</CTableHeaderCell>
                    <CTableHeaderCell>Performance</CTableHeaderCell>
                  </CTableRow>
                </CTableHead>
                <CTableBody>
                  {data.attendanceByEvent.map((event, index) => (
                    <CTableRow key={index}>
                      <CTableDataCell>
                        <strong>{event.eventName}</strong>
                      </CTableDataCell>
                      <CTableDataCell>{event.registered}</CTableDataCell>
                      <CTableDataCell>{event.attended}</CTableDataCell>
                      <CTableDataCell>
                        <div className="d-flex align-items-center">
                          <span className="me-2">{event.attendanceRate}%</span>
                          <CProgress value={event.attendanceRate} color="primary" style={{ width: '60px', height: '8px' }} />
                        </div>
                      </CTableDataCell>
                      <CTableDataCell>{event.noShowRate}%</CTableDataCell>
                      <CTableDataCell>
                        {getAttendanceBadge(event.attendanceRate)}
                      </CTableDataCell>
                    </CTableRow>
                  ))}
                </CTableBody>
              </CTable>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* Demographic Attendance Analysis */}
      <CRow className="mb-4">
        <CCol lg={4}>
          <CCard>
            <CCardHeader>
              <h5 className="mb-0">Attendance by Age Group</h5>
            </CCardHeader>
            <CCardBody>
              {data.demographicAttendance.byAge.map((age, index) => (
                <div key={index} className="d-flex justify-content-between align-items-center mb-3">
                  <div>
                    <strong>{age.ageGroup}</strong>
                    <small className="text-medium-emphasis ms-2">({age.count})</small>
                  </div>
                  <div className="flex-grow-1 mx-3">
                    <CProgress value={age.attendanceRate} color="primary" />
                  </div>
                  <div className="text-end">
                    <strong>{age.attendanceRate}%</strong>
                  </div>
                </div>
              ))}
            </CCardBody>
          </CCard>
        </CCol>
        <CCol lg={4}>
          <CCard>
            <CCardHeader>
              <h5 className="mb-0">Attendance by Location</h5>
            </CCardHeader>
            <CCardBody>
              {data.demographicAttendance.byLocation.map((location, index) => (
                <div key={index} className="d-flex justify-content-between align-items-center mb-3">
                  <div>
                    <strong>{location.location}</strong>
                    <small className="text-medium-emphasis ms-2">({location.count})</small>
                  </div>
                  <div className="flex-grow-1 mx-3">
                    <CProgress value={location.attendanceRate} color="info" />
                  </div>
                  <div className="text-end">
                    <strong>{location.attendanceRate}%</strong>
                  </div>
                </div>
              ))}
            </CCardBody>
          </CCard>
        </CCol>
        <CCol lg={4}>
          <CCard>
            <CCardHeader>
              <h5 className="mb-0">Attendance Quality Metrics</h5>
            </CCardHeader>
            <CCardBody>
              <div className="mb-3">
                <small className="text-medium-emphasis">On-Time Arrivals</small>
                <div className="fs-4 fw-bold text-success">{data.attendanceQuality.onTimeArrivals.toLocaleString()}</div>
                <small className="text-medium-emphasis">
                  {((data.attendanceQuality.onTimeArrivals / data.totalAttendees) * 100).toFixed(1)}% of attendees
                </small>
              </div>
              <div className="mb-3">
                <small className="text-medium-emphasis">Late Arrivals</small>
                <div className="fs-5 fw-bold text-warning">{data.attendanceQuality.lateArrivals.toLocaleString()}</div>
              </div>
              <div className="mb-3">
                <small className="text-medium-emphasis">Early Departures</small>
                <div className="fs-5 fw-bold text-danger">{data.attendanceQuality.earlyDepartures.toLocaleString()}</div>
              </div>
              <div>
                <small className="text-medium-emphasis">Stayed Full Event</small>
                <div className="fs-4 fw-bold text-primary">{data.attendanceQuality.stayedFullEvent.toLocaleString()}</div>
                <small className="text-medium-emphasis">
                  {((data.attendanceQuality.stayedFullEvent / data.totalAttendees) * 100).toFixed(1)}% retention
                </small>
              </div>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>
    </div>
  );
};

export default AttendanceAnalytics;