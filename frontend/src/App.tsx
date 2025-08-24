import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import CoreUILayout from './components/layout/CoreUILayout';
import HomePage from './pages/HomePage';
import EventDetailsPage from './pages/EventDetailsPage';
import AdminDashboard from './pages/AdminDashboard';
import AttendancePage from './pages/AttendancePage';
import EventBuilder from './pages/EventBuilder';
import ExecutiveDashboard from './components/analytics/ExecutiveDashboard';
import RegistrationTrends from './components/analytics/RegistrationTrends';
import AttendanceAnalytics from './components/analytics/AttendanceAnalytics';

function App(): React.JSX.Element {
  return (
    <Router>
      <CoreUILayout>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/events/:id" element={<EventDetailsPage />} />
          <Route path="/admin" element={<AdminDashboard />} />
          <Route path="/admin/event/new" element={<EventBuilder />} />
          <Route path="/admin/event/:id/edit" element={<EventBuilder />} />
          <Route path="/admin/events/:id/attendance" element={<AttendancePage />} />
          
          {/* Analytics Routes */}
          <Route path="/analytics/executive" element={<ExecutiveDashboard />} />
          <Route path="/analytics/registrations" element={<RegistrationTrends />} />
          <Route path="/analytics/events" element={<div className="p-4"><h2>Event Analytics</h2><p>Coming soon...</p></div>} />
          <Route path="/analytics/attendance" element={<AttendanceAnalytics />} />
          
          {/* Tools Routes */}
          <Route path="/qr-scanner" element={<div className="p-4"><h2>QR Scanner</h2><p>Coming soon...</p></div>} />
          <Route path="/resources" element={<div className="p-4"><h2>Resource Management</h2><p>Coming soon...</p></div>} />
          <Route path="/settings" element={<div className="p-4"><h2>Settings</h2><p>Coming soon...</p></div>} />
          <Route path="/registrations" element={<RegistrationTrends />} />
          <Route path="/checkin" element={<div className="p-4"><h2>Check-In</h2><p>Coming soon...</p></div>} />
        </Routes>
      </CoreUILayout>
    </Router>
  );
}

export default App;
