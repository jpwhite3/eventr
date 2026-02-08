import React, { Suspense } from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import CoreUILayout from './components/layout/CoreUILayout';
import ProtectedRoute from './components/auth/ProtectedRoute';

// Core pages - keep synchronous for immediate loading  
import EventListPage from './pages/EventListPage';
import EventDetailsPage from './pages/EventDetailsPage';

// Lazy load other pages for code splitting
const RegistrationHistoryPage = React.lazy(() => import('./pages/RegistrationHistoryPage'));
const RegistrationPage = React.lazy(() => import('./pages/RegistrationPage'));
const AdminDashboard = React.lazy(() => import('./pages/AdminDashboard'));
const AttendancePage = React.lazy(() => import('./pages/AttendancePage'));
const EventBuilder = React.lazy(() => import('./pages/EventBuilder'));
const CheckInPage = React.lazy(() => import('./pages/CheckInPage'));
const UserProfilePage = React.lazy(() => import('./pages/UserProfilePage'));
const UserSettingsPage = React.lazy(() => import('./pages/UserSettingsPage'));
const EventRegistrationManagement = React.lazy(() => import('./pages/EventRegistrationManagement'));

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
    <Router
      future={{
        v7_startTransition: true,
        v7_relativeSplatPath: true
      }}
    >
      <CoreUILayout>
        <Suspense fallback={<LoadingFallback />}>
          <Routes>
            {/* Public Routes */}
            <Route path="/" element={<EventListPage />} />
            <Route path="/events" element={<EventListPage />} />
            <Route path="/events/:id" element={<EventDetailsPage />} />
            
            {/* Attendee Routes - Require Authentication */}
            <Route path="/my-registrations" element={<ProtectedRoute><RegistrationHistoryPage /></ProtectedRoute>} />
            <Route path="/profile" element={<ProtectedRoute><UserProfilePage /></ProtectedRoute>} />
            <Route path="/settings" element={<ProtectedRoute><UserSettingsPage /></ProtectedRoute>} />
            <Route path="/events/:id/register" element={<ProtectedRoute><RegistrationPage /></ProtectedRoute>} />
            <Route path="/events/:id/register/:instanceId" element={<ProtectedRoute><RegistrationPage /></ProtectedRoute>} />
            
            {/* Organizer Routes */}
            <Route path="/admin" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN']}><AdminDashboard /></ProtectedRoute>} />
            <Route path="/admin/event/new" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN']}><EventBuilder /></ProtectedRoute>} />
            <Route path="/admin/event/:id/edit" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN']}><EventBuilder /></ProtectedRoute>} />
            <Route path="/admin/events/:id/attendance" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN']}><AttendancePage /></ProtectedRoute>} />
            <Route path="/admin/events/:eventId/registrations" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN']}><EventRegistrationManagement /></ProtectedRoute>} />
            <Route path="/admin/checkin" element={<ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN']}><CheckInPage /></ProtectedRoute>} />
          </Routes>
        </Suspense>
      </CoreUILayout>
    </Router>
  );
}

export default App;
