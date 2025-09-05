import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import CoreUILayout from './components/layout/CoreUILayout';
import HomePage from './pages/HomePage';
import EventListPage from './pages/EventListPage';
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
import UserProfilePage from './pages/UserProfilePage';
import UserSettingsPage from './pages/UserSettingsPage';
import EventRegistrationManagement from './pages/EventRegistrationManagement';
import ProtectedRoute from './components/auth/ProtectedRoute';

function App(): React.JSX.Element {
  return (
    <Router>
      <CoreUILayout>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/events" element={<EventListPage />} />
          <Route path="/events/:id" element={<EventDetailsPage />} />
          
          {/* Protected Routes - Require Authentication */}
          <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
          <Route path="/my-registrations" element={<ProtectedRoute><RegistrationHistoryPage /></ProtectedRoute>} />
          <Route path="/profile" element={<ProtectedRoute><UserProfilePage /></ProtectedRoute>} />
          <Route path="/settings" element={<ProtectedRoute><UserSettingsPage /></ProtectedRoute>} />
          <Route path="/events/:id/register" element={<ProtectedRoute><RegistrationPage /></ProtectedRoute>} />
          <Route path="/events/:id/register/:instanceId" element={<ProtectedRoute><RegistrationPage /></ProtectedRoute>} />
          
          {/* Admin Routes - Require Organizer Role */}
          <Route path="/admin" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><AdminDashboard /></ProtectedRoute>} />
          <Route path="/admin/event/new" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><EventBuilder /></ProtectedRoute>} />
          <Route path="/admin/event/:id/edit" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><EventBuilder /></ProtectedRoute>} />
          <Route path="/admin/events/:id/attendance" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><AttendancePage /></ProtectedRoute>} />
          <Route path="/admin/events/:eventId/registrations" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><EventRegistrationManagement /></ProtectedRoute>} />
          
          {/* Analytics Routes - Protected */}
          <Route path="/analytics/executive" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><ExecutiveDashboard /></ProtectedRoute>} />
          <Route path="/analytics/registrations" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><RegistrationTrends /></ProtectedRoute>} />
          <Route path="/analytics/events" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><EventAnalytics /></ProtectedRoute>} />
          <Route path="/analytics/attendance" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><AttendanceAnalytics /></ProtectedRoute>} />
          
          {/* Tools Routes - Protected */}
          <Route path="/resources" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><ResourceManagement /></ProtectedRoute>} />
          <Route path="/registrations" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><RegistrationTrends /></ProtectedRoute>} />
          <Route path="/checkin" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><CheckInPage /></ProtectedRoute>} />
        </Routes>
      </CoreUILayout>
    </Router>
  );
}

export default App;
