import { BrowserRouter, Routes, Route } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';

import LandingPage    from './pages/LandingPage';
import UserLogin      from './pages/UserLogin';
import UserRegister   from './pages/UserRegister';
import AdminLogin     from './pages/AdminLogin';
import MovieList      from './pages/MovieList';
import ShowSelection  from './pages/ShowSelection';
import SeatSelection  from './pages/SeatSelection';
import PaymentPage    from './pages/PaymentPage';
import BookingHistory from './pages/BookingHistory';
import AdminDashboard from './pages/AdminDashboard';
import TicketPage     from './pages/TicketPage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/"              element={<LandingPage />} />
        <Route path="/user-login"    element={<UserLogin />} />
        <Route path="/user-register" element={<UserRegister />} />
        <Route path="/admin-login"   element={<AdminLogin />} />

        {/* User */}
        <Route path="/movies"                     element={<ProtectedRoute><MovieList /></ProtectedRoute>} />
        <Route path="/movies/:movieId/shows"      element={<ProtectedRoute><ShowSelection /></ProtectedRoute>} />
        <Route path="/shows/:showId/seats"         element={<ProtectedRoute><SeatSelection /></ProtectedRoute>} />
        <Route path="/payment/:bookingId"          element={<ProtectedRoute><PaymentPage /></ProtectedRoute>} />
        <Route path="/ticket/:ticketId"            element={<ProtectedRoute><TicketPage /></ProtectedRoute>} />
        <Route path="/my-bookings"                 element={<ProtectedRoute><BookingHistory /></ProtectedRoute>} />

        {/* Admin */}
        <Route path="/admin" element={<ProtectedRoute requiredRole="ADMIN"><AdminDashboard /></ProtectedRoute>} />

        {/* 404 */}
        <Route path="*" element={
          <div style={{ minHeight:'100vh', background:'#07070e', display:'flex',
            alignItems:'center', justifyContent:'center', flexDirection:'column',
            fontFamily:"'Segoe UI',sans-serif", color:'#667799' }}>
            <div style={{ fontSize:'80px', marginBottom:'16px' }}>🎭</div>
            <h1 style={{ color:'#fff', marginBottom:'16px' }}>404 — Page Not Found</h1>
            <a href="/" style={{ color:'#00d4ff', textDecoration:'none', padding:'10px 24px',
              border:'1px solid #00d4ff44', borderRadius:'10px' }}>← Back to Home</a>
          </div>
        }/>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
