import { useNavigate, Link } from 'react-router-dom';

const Navbar = ({ theme = 'user' }) => {
  const navigate = useNavigate();
  const name = localStorage.getItem('userName') || 'User';
  const role = localStorage.getItem('role');

  const isAdmin = theme === 'admin' || role === 'ADMIN';
  const accent = isAdmin ? '#ff6b00' : '#00d4ff';
  const accentGlow = isAdmin ? '#ff6b0044' : '#00d4ff44';

  const logout = () => {
    localStorage.clear();
    navigate('/');
  };

  return (
    <nav style={{
      background: 'rgba(10,10,15,0.95)',
      backdropFilter: 'blur(20px)',
      borderBottom: `1px solid ${accent}33`,
      padding: '0 32px',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      height: '64px', position: 'sticky', top: 0, zIndex: 100,
      boxShadow: `0 4px 30px ${accentGlow}`,
    }}>
      {/* Logo */}
      <Link to={isAdmin ? '/admin' : '/movies'} style={{ textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '10px' }}>
        <span style={{ fontSize: '24px' }}>🎬</span>
        <span style={{
          fontSize: '20px', fontWeight: '800',
          background: `linear-gradient(135deg, ${accent}, #fff)`,
          WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
          letterSpacing: '1px',
        }}>CineBook</span>
        {isAdmin && <span style={{ fontSize: '11px', color: '#ff6b00', border: '1px solid #ff6b00', padding: '2px 8px', borderRadius: '20px' }}>ADMIN</span>}
      </Link>

      {/* Nav links */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
        {!isAdmin && (
          <>
            <Link to="/movies" style={linkStyle(accent)}>🎭 Movies</Link>
            <Link to="/my-bookings" style={linkStyle(accent)}>🎟️ My Bookings</Link>
          </>
        )}
        {isAdmin && (
          <Link to="/admin" style={linkStyle(accent)}>📊 Dashboard</Link>
        )}

        {/* User info */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <span style={{
            color: '#aaa', fontSize: '13px',
            background: `${accent}11`, border: `1px solid ${accent}33`,
            padding: '4px 12px', borderRadius: '20px',
          }}>👤 {name}</span>
          <button onClick={logout} style={{
            background: 'transparent', border: `1px solid ${accent}`,
            color: accent, padding: '6px 16px', borderRadius: '8px',
            cursor: 'pointer', fontSize: '13px', fontWeight: '600',
            transition: 'all 0.3s ease',
          }}
            onMouseEnter={e => { e.target.style.background = accent; e.target.style.color = '#000'; }}
            onMouseLeave={e => { e.target.style.background = 'transparent'; e.target.style.color = accent; }}
          >Logout</button>
        </div>
      </div>
    </nav>
  );
};

const linkStyle = (accent) => ({
  color: '#ccc', textDecoration: 'none', fontSize: '14px', fontWeight: '500',
  padding: '6px 12px', borderRadius: '8px', transition: 'all 0.2s ease',
  onMouseEnter: undefined,
});

export default Navbar;
