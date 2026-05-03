import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api/axios';
import Toast from '../components/Toast';

// UserLogin — Electric cyan/blue theme
// OOAD: Actor: User; Use Case: Login
const UserLogin = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await api.post('/api/auth/login', form);
      const { token, userId, name, role } = res.data.data;
      localStorage.setItem('token', token);
      localStorage.setItem('role', role);
      localStorage.setItem('userId', userId);
      localStorage.setItem('userName', name);
      setToast({ message: `Welcome back, ${name}! 🎬`, type: 'success' });
      setTimeout(() => navigate('/movies'), 1000);
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Login failed', type: 'error' });
    } finally { setLoading(false); }
  };

  return (
    <div style={pageStyle}>
      {/* Background orbs */}
      <div style={{ position:'absolute', width:'400px', height:'400px', borderRadius:'50%',
        background:'radial-gradient(circle, #00d4ff0a, transparent)', top:'-100px', right:'-100px' }}/>

      <div style={cardStyle('#00d4ff')}>
        {/* Back button */}
        <Link to="/" style={{ color:'#00d4ff88', textDecoration:'none', fontSize:'13px', display:'block', marginBottom:'24px' }}>
          ← Back to Home
        </Link>

        <div style={{ textAlign:'center', marginBottom:'32px' }}>
          <div style={{ fontSize:'48px', marginBottom:'8px' }}>🎬</div>
          <h1 style={titleStyle('#00d4ff')}>Welcome Back</h1>
          <p style={{ color:'#667799', fontSize:'14px' }}>Sign in to book your tickets</p>
        </div>

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom:'20px' }}>
            <label style={labelStyle}>Email Address</label>
            <input
              type="email" required placeholder="your@email.com"
              value={form.email} onChange={e => setForm({...form, email: e.target.value})}
              style={inputStyle('#00d4ff')}
              onFocus={e => e.target.style.borderColor='#00d4ff'}
              onBlur={e => e.target.style.borderColor='#ffffff22'}
            />
          </div>
          <div style={{ marginBottom:'28px' }}>
            <label style={labelStyle}>Password</label>
            <input
              type="password" required placeholder="••••••••"
              value={form.password} onChange={e => setForm({...form, password: e.target.value})}
              style={inputStyle('#00d4ff')}
              onFocus={e => e.target.style.borderColor='#00d4ff'}
              onBlur={e => e.target.style.borderColor='#ffffff22'}
            />
          </div>

          <button type="submit" disabled={loading} style={submitBtnStyle('#00d4ff', loading)}>
            {loading ? <span style={spinnerStyle}/> : '🎟️  Sign In'}
          </button>
        </form>

        <p style={{ textAlign:'center', marginTop:'24px', color:'#667799', fontSize:'14px' }}>
          Don't have an account?&nbsp;
          <Link to="/user-register" style={{ color:'#00d4ff', textDecoration:'none', fontWeight:'600' }}>
            Register here →
          </Link>
        </p>
      </div>

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)}/>}
    </div>
  );
};

// ── Shared styles ──
export const pageStyle = {
  minHeight:'100vh', background:'radial-gradient(ellipse at 30% 50%, #0a0a2e 0%, #05050f 100%)',
  display:'flex', alignItems:'center', justifyContent:'center',
  fontFamily:"'Segoe UI', sans-serif", padding:'20px', position:'relative', overflow:'hidden',
};

export const cardStyle = (accent) => ({
  background:'rgba(255,255,255,0.03)', backdropFilter:'blur(20px)',
  border:`1px solid ${accent}44`, borderRadius:'20px',
  padding:'40px', width:'100%', maxWidth:'420px',
  boxShadow:`0 0 60px ${accent}11, inset 0 0 60px rgba(255,255,255,0.01)`,
  position:'relative', zIndex:1,
});

export const titleStyle = (accent) => ({
  fontSize:'28px', fontWeight:'800',
  background:`linear-gradient(135deg, ${accent}, #ffffff)`,
  WebkitBackgroundClip:'text', WebkitTextFillColor:'transparent',
  margin:'0 0 8px',
});

export const labelStyle = {
  display:'block', color:'#8899bb', fontSize:'13px', fontWeight:'600',
  marginBottom:'8px', letterSpacing:'0.5px', textTransform:'uppercase',
};

export const inputStyle = (accent) => ({
  width:'100%', padding:'12px 16px', background:'rgba(255,255,255,0.05)',
  border:'1px solid #ffffff22', borderRadius:'10px', color:'#fff',
  fontSize:'15px', outline:'none', boxSizing:'border-box',
  transition:'border-color 0.3s ease',
});

export const submitBtnStyle = (accent, loading) => ({
  width:'100%', padding:'14px', fontSize:'16px', fontWeight:'700',
  background: loading ? '#333' : `linear-gradient(135deg, ${accent}cc, ${accent})`,
  border:'none', borderRadius:'12px', color:'#fff', cursor: loading ? 'not-allowed' : 'pointer',
  boxShadow: loading ? 'none' : `0 0 30px ${accent}44`,
  display:'flex', alignItems:'center', justifyContent:'center', gap:'8px',
  transition:'all 0.3s ease',
});

export const spinnerStyle = {
  width:'18px', height:'18px', border:'3px solid rgba(255,255,255,0.2)',
  borderTop:'3px solid #fff', borderRadius:'50%',
  animation:'spin 0.8s linear infinite',
};

export default UserLogin;
