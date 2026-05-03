import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api/axios';
import Toast from '../components/Toast';
import { pageStyle, cardStyle, titleStyle, labelStyle, inputStyle, submitBtnStyle, spinnerStyle } from './UserLogin';

// UserRegister — Same blue/cyan theme as login
// OOAD: Actor: User; Use Case: Register Account
const UserRegister = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({ name:'', email:'', password:'', phone:'' });
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (form.password.length < 6) {
      setToast({ message: 'Password must be at least 6 characters', type: 'error' });
      return;
    }
    setLoading(true);
    try {
      // Register
      await api.post('/api/auth/register', { ...form, role: 'USER' });
      // Auto-login
      const loginRes = await api.post('/api/auth/login', { email: form.email, password: form.password });
      const { token, userId, name, role } = loginRes.data.data;
      localStorage.setItem('token', token);
      localStorage.setItem('role', role);
      localStorage.setItem('userId', userId);
      localStorage.setItem('userName', name);
      setToast({ message: `Account created! Welcome, ${name} 🎉`, type: 'success' });
      setTimeout(() => navigate('/movies'), 1200);
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Registration failed', type: 'error' });
    } finally { setLoading(false); }
  };

  const focusIn = e => e.target.style.borderColor='#00d4ff';
  const focusOut = e => e.target.style.borderColor='#ffffff22';

  return (
    <div style={{ ...pageStyle }}>
      <div style={{ position:'absolute', width:'500px', height:'500px', borderRadius:'50%',
        background:'radial-gradient(circle, #00d4ff08, transparent)', bottom:'-150px', left:'-150px' }}/>

      <div style={{ ...cardStyle('#00d4ff'), maxWidth:'460px' }}>
        <Link to="/" style={{ color:'#00d4ff88', textDecoration:'none', fontSize:'13px', display:'block', marginBottom:'24px' }}>
          ← Back to Home
        </Link>

        <div style={{ textAlign:'center', marginBottom:'32px' }}>
          <div style={{ fontSize:'48px', marginBottom:'8px' }}>🎭</div>
          <h1 style={titleStyle('#00d4ff')}>Create Account</h1>
          <p style={{ color:'#667799', fontSize:'14px' }}>Join CineBook and start booking</p>
        </div>

        <form onSubmit={handleSubmit}>
          {[
            { label:'Full Name', type:'text', field:'name', placeholder:'Durga Sravani', required:true },
            { label:'Email Address', type:'email', field:'email', placeholder:'you@email.com', required:true },
            { label:'Password', type:'password', field:'password', placeholder:'Min. 6 characters', required:true },
            { label:'Phone Number', type:'tel', field:'phone', placeholder:'+91 9876543210', required:false },
          ].map(({ label, type, field, placeholder, required }) => (
            <div key={field} style={{ marginBottom:'18px' }}>
              <label style={labelStyle}>{label}</label>
              <input type={type} required={required} placeholder={placeholder}
                value={form[field]} onChange={set(field)}
                style={inputStyle('#00d4ff')}
                onFocus={focusIn} onBlur={focusOut}
              />
            </div>
          ))}

          <div style={{ marginBottom:'28px' }}/>
          <button type="submit" disabled={loading} style={submitBtnStyle('#00d4ff', loading)}>
            {loading ? <span style={spinnerStyle}/> : '🚀  Create Account'}
          </button>
        </form>

        <p style={{ textAlign:'center', marginTop:'24px', color:'#667799', fontSize:'14px' }}>
          Already have an account?&nbsp;
          <Link to="/user-login" style={{ color:'#00d4ff', textDecoration:'none', fontWeight:'600' }}>Sign in →</Link>
        </p>
      </div>

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)}/>}
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
};

export default UserRegister;
