import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import HomePage from './pages/HomePage';
import EventDetailsPage from './pages/EventDetailsPage';
import AdminDashboard from './pages/AdminDashboard';
import AttendancePage from './pages/AttendancePage';
import EventBuilder from './pages/EventBuilder';

function App(): React.JSX.Element {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/events/:id" element={<EventDetailsPage />} />
        <Route path="/admin" element={<AdminDashboard />} />
        <Route path="/admin/event/new" element={<EventBuilder />} />
        <Route path="/admin/event/:id/edit" element={<EventBuilder />} />
        <Route path="/admin/events/:id/attendance" element={<AttendancePage />} />
      </Routes>
    </Router>
  );
}

export default App;
