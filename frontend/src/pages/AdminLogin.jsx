import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api/axios';
import Toast from '../components/Toast';

// AdminLogin — Deep purple + electric orange theme
// OOAD: Actor: Admin; Use Case: Admin Login
const AdminLogin = () => {
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
      if (role !== 'ADMIN') {
        setToast({ message: '🚫 Access denied. Admin credentials required.', type: 'error' });
        setLoading(false);
        return;
      }
      localStorage.setItem('token', token);
      localStorage.setItem('role', role);
      localStorage.setItem('userId', userId);
      localStorage.setItem('userName', name);
      setToast({ message: `Welcome, Admin ${name}! ⚡`, type: 'success' });
      setTimeout(() => navigate('/admin'), 1000);
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Login failed', type: 'error' });
    } finally { setLoading(false); }
  };

  return (
    <div style={{
      minHeight:'100vh',
      background:'radial-gradient(ellipse at 70% 50%, #1a0030 0%, #0a000f 100%)',
      display:'flex', alignItems:'center', justifyContent:'center',
      fontFamily:"'Segoe UI', sans-serif", padding:'20px', position:'relative', overflow:'hidden',
    }}>
      {/* Purple back-glow */}
      <div style={{ position:'absolute', width:'600px', height:'600px', borderRadius:'50%',
        background:'radial-gradient(circle, #8b00ff0a, transparent)', top:'-150px', left:'-150px' }}/>
      <div style={{ position:'absolute', width:'400px', height:'400px', borderRadius:'50%',
        background:'radial-gradient(circle, #ff6b000a, transparent)', bottom:'-100px', right:'-100px' }}/>

      <div style={{
        background:'rgba(255,255,255,0.03)', backdropFilter:'blur(20px)',
        border:'1px solid #ff6b0044', borderRadius:'20px', padding:'40px',
        width:'100%', maxWidth:'420px', position:'relative', zIndex:1,
        boxShadow:'0 0 80px #8b00ff11, 0 0 40px #ff6b0011',
      }}>
        <Link to="/" style={{ color:'#ff6b0088', textDecoration:'none', fontSize:'13px', display:'block', marginBottom:'24px' }}>
          ← Back to Home
        </Link>

        <div style={{ textAlign:'center', marginBottom:'32px' }}>
          <div style={{ fontSize:'48px', marginBottom:'8px' }}>⚡</div>
          <h1 style={{
            fontSize:'28px', fontWeight:'800',
            background:'linear-gradient(135deg, #ff6b00, #ffcc00)',
            WebkitBackgroundClip:'text', WebkitTextFillColor:'transparent', margin:'0 0 8px',
          }}>Admin Portal</h1>
          <p style={{ color:'#aa8866', fontSize:'14px' }}>Authorized personnel only</p>
          <div style={{ display:'flex', justifyContent:'center', gap:'6px', marginTop:'12px' }}>
            {['🎬','🎭','🍿'].map((e,i) => <span key={i} style={{ fontSize:'16px' }}>{e}</span>)}
          </div>
        </div>

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom:'20px' }}>
            <label style={{ display:'block', color:'#aa8866', fontSize:'13px', fontWeight:'600',
              marginBottom:'8px', letterSpacing:'0.5px', textTransform:'uppercase' }}>
              Admin Email
            </label>
            <input type="email" required placeholder="admin@cinebook.com"
              value={form.email} onChange={e => setForm({...form, email: e.target.value})}
              style={{
                width:'100%', padding:'12px 16px', background:'rgba(255,255,255,0.05)',
                border:'1px solid #ffffff22', borderRadius:'10px', color:'#fff',
                fontSize:'15px', outline:'none', boxSizing:'border-box', transition:'border-color 0.3s',
              }}
              onFocus={e => e.target.style.borderColor='#ff6b00'}
              onBlur={e => e.target.style.borderColor='#ffffff22'}
            />
          </div>
          <div style={{ marginBottom:'28px' }}>
            <label style={{ display:'block', color:'#aa8866', fontSize:'13px', fontWeight:'600',
              marginBottom:'8px', letterSpacing:'0.5px', textTransform:'uppercase' }}>
              Password
            </label>
            <input type="password" required placeholder="••••••••"
              value={form.password} onChange={e => setForm({...form, password: e.target.value})}
              style={{
                width:'100%', padding:'12px 16px', background:'rgba(255,255,255,0.05)',
                border:'1px solid #ffffff22', borderRadius:'10px', color:'#fff',
                fontSize:'15px', outline:'none', boxSizing:'border-box', transition:'border-color 0.3s',
              }}
              onFocus={e => e.target.style.borderColor='#ff6b00'}
              onBlur={e => e.target.style.borderColor='#ffffff22'}
            />
          </div>

          <button type="submit" disabled={loading} style={{
            width:'100%', padding:'14px', fontSize:'16px', fontWeight:'700',
            background: loading ? '#333' : 'linear-gradient(135deg, #ff6b00, #ff9900)',
            border:'none', borderRadius:'12px', color:'#fff',
            cursor: loading ? 'not-allowed' : 'pointer',
            boxShadow: loading ? 'none' : '0 0 30px #ff6b0044',
            display:'flex', alignItems:'center', justifyContent:'center', gap:'8px',
            transition:'all 0.3s',
          }}>
            {loading
              ? <span style={{ width:'18px',height:'18px',border:'3px solid rgba(255,255,255,0.3)',
                  borderTop:'3px solid #fff',borderRadius:'50%',animation:'spin 0.8s linear infinite' }}/>
              : '⚡  Access Admin Panel'}
          </button>
        </form>

        {/* Security badge */}
        <div style={{
          marginTop:'24px', padding:'12px', borderRadius:'10px',
          background:'#ff6b0011', border:'1px solid #ff6b0033', textAlign:'center',
        }}>
          <span style={{ color:'#ff6b00', fontSize:'12px' }}>🔒 Secured with JWT Authentication</span>
        </div>
      </div>

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)}/>}
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
};

export default AdminLogin;
