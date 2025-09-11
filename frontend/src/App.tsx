import React, { Suspense } from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import CoreUILayout from './components/layout/CoreUILayout';
import ProtectedRoute from './components/auth/ProtectedRoute';

// Core pages - keep synchronous for immediate loading
import HomePage from './pages/HomePage';
import EventListPage from './pages/EventListPage';
import EventDetailsPage from './pages/EventDetailsPage';

// Lazy load other pages for code splitting
const DashboardPage = React.lazy(() => import('./pages/DashboardPage'));
const RegistrationHistoryPage = React.lazy(() => import('./pages/RegistrationHistoryPage'));
const RegistrationPage = React.lazy(() => import('./pages/RegistrationPage'));
const AdminDashboard = React.lazy(() => import('./pages/AdminDashboard'));
const AttendancePage = React.lazy(() => import('./pages/AttendancePage'));
const EventBuilder = React.lazy(() => import('./pages/EventBuilder'));
const CheckInPage = React.lazy(() => import('./pages/CheckInPage'));
const UserProfilePage = React.lazy(() => import('./pages/UserProfilePage'));
const UserSettingsPage = React.lazy(() => import('./pages/UserSettingsPage'));
const CalendarViewPage = React.lazy(() => import('./pages/CalendarViewPage'));
const EventRegistrationManagement = React.lazy(() => import('./pages/EventRegistrationManagement'));
const UserManagement = React.lazy(() => import('./pages/UserManagement'));
const SystemReports = React.lazy(() => import('./pages/SystemReports'));
const MobileCheckInPage = React.lazy(() => import('./pages/MobileCheckInPage'));

// Lazy load analytics components
const ExecutiveDashboard = React.lazy(() => import('./components/analytics/ExecutiveDashboard'));
const RegistrationTrends = React.lazy(() => import('./components/analytics/RegistrationTrends'));
const AttendanceAnalytics = React.lazy(() => import('./components/analytics/AttendanceAnalytics'));
const EventAnalytics = React.lazy(() => import('./components/analytics/EventAnalytics'));

// Lazy load other components
const ResourceManagement = React.lazy(() => import('./components/ResourceManagement'));

// Loading component for Suspense fallback
const LoadingFallback = () => (
  <div className="d-flex justify-content-center align-items-center" style={{ height: '200px' }}>
    <div className="spinner-border text-primary" role="status">
      <span className="visually-hidden">Loading...</span>
    </div>
  </div>
);

function App(): React.JSX.Element {
  return (
    <Router>
      <CoreUILayout>
        <Suspense fallback={<LoadingFallback />}>
          <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/events" element={<EventListPage />} />
          <Route path="/events/:id" element={<EventDetailsPage />} />
          
          {/* Protected Routes - Require Authentication */}
          <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
          <Route path="/my-registrations" element={<ProtectedRoute><RegistrationHistoryPage /></ProtectedRoute>} />
          <Route path="/calendar" element={<ProtectedRoute><CalendarViewPage /></ProtectedRoute>} />
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
          <Route path="/admin/users" element={<ProtectedRoute requiredRoles={['ADMIN', 'SUPER_ADMIN']}><UserManagement /></ProtectedRoute>} />
          <Route path="/admin/reports" element={<ProtectedRoute requiredRoles={['ADMIN', 'SUPER_ADMIN']}><SystemReports /></ProtectedRoute>} />
          
          {/* Analytics Routes - Protected */}
          <Route path="/analytics/executive" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><ExecutiveDashboard /></ProtectedRoute>} />
          <Route path="/analytics/registrations" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><RegistrationTrends /></ProtectedRoute>} />
          <Route path="/analytics/events" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><EventAnalytics /></ProtectedRoute>} />
          <Route path="/analytics/attendance" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><AttendanceAnalytics /></ProtectedRoute>} />
          
          {/* Tools Routes - Protected */}
          <Route path="/resources" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><ResourceManagement /></ProtectedRoute>} />
          <Route path="/registrations" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><RegistrationTrends /></ProtectedRoute>} />
          <Route path="/checkin" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']}><CheckInPage /></ProtectedRoute>} />
          
          {/* Mobile Check-In Routes - Staff Access */}
          <Route path="/mobile-checkin/:eventId" element={<MobileCheckInPage />} />
          <Route path="/mobile-checkin" element={<MobileCheckInPage />} />
          </Routes>
        </Suspense>
      </CoreUILayout>
    </Router>
  );
}

export default App;
