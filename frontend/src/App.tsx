import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import CoreUILayout from './components/layout/CoreUILayout';
import HomePage from './pages/HomePage';
import DashboardPage from './pages/DashboardPage';
import RegistrationHistoryPage from './pages/RegistrationHistoryPage';
import EventDetailsPage from './pages/EventDetailsPage';
import RegistrationPage from './pages/RegistrationPage';
import AdminDashboard from './pages/AdminDashboard';
import AttendancePage from './pages/AttendancePage';
import EventBuilder from './pages/EventBuilder';
import ExecutiveDashboard from './components/analytics/ExecutiveDashboard';
import RegistrationTrends from './components/analytics/RegistrationTrends';
import AttendanceAnalytics from './components/analytics/AttendanceAnalytics';
import EventAnalytics from './components/analytics/EventAnalytics';
import ResourceManagement from './components/ResourceManagement';
import CheckInPage from './pages/CheckInPage';

function App(): React.JSX.Element {
  return (
    <Router>
      <CoreUILayout>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/my-registrations" element={<RegistrationHistoryPage />} />
          <Route path="/events/:id" element={<EventDetailsPage />} />
          <Route path="/events/:id/register" element={<RegistrationPage />} />
          <Route path="/events/:id/register/:instanceId" element={<RegistrationPage />} />
          <Route path="/admin" element={<AdminDashboard />} />
          <Route path="/admin/event/new" element={<EventBuilder />} />
          <Route path="/admin/event/:id/edit" element={<EventBuilder />} />
          <Route path="/admin/events/:id/attendance" element={<AttendancePage />} />
          
          {/* Analytics Routes */}
          <Route path="/analytics/executive" element={<ExecutiveDashboard />} />
          <Route path="/analytics/registrations" element={<RegistrationTrends />} />
          <Route path="/analytics/events" element={<EventAnalytics />} />
          <Route path="/analytics/attendance" element={<AttendanceAnalytics />} />
          
          {/* Tools Routes */}
          <Route path="/resources" element={<ResourceManagement />} />
          <Route path="/registrations" element={<RegistrationTrends />} />
          <Route path="/checkin" element={<CheckInPage />} />
        </Routes>
      </CoreUILayout>
    </Router>
  );
}

export default App;
