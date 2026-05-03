import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';

// LandingPage — Cinematic dark hero with animated particles
// OOAD: Entry point for all actors (User, Admin)
const LandingPage = () => {
  const navigate = useNavigate();
  const canvasRef = useRef(null);

  // Animated star particles using Canvas
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;

    const stars = Array.from({ length: 120 }, () => ({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      r: Math.random() * 1.5 + 0.3,
      speed: Math.random() * 0.4 + 0.1,
      opacity: Math.random(),
      dir: Math.random() > 0.5 ? 1 : -1,
    }));

    let animId;
    const animate = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      stars.forEach(s => {
        s.opacity += 0.008 * s.dir;
        if (s.opacity > 1 || s.opacity < 0) s.dir *= -1;
        ctx.beginPath();
        ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(0, 212, 255, ${s.opacity * 0.7})`;
        ctx.fill();
        s.y -= s.speed;
        if (s.y < 0) s.y = canvas.height;
      });
      animId = requestAnimationFrame(animate);
    };
    animate();
    return () => cancelAnimationFrame(animId);
  }, []);

  return (
    <div style={{
      minHeight: '100vh', background: 'radial-gradient(ellipse at center, #0d0d2b 0%, #050508 70%)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontFamily: "'Segoe UI', sans-serif", overflow: 'hidden', position: 'relative',
    }}>
      {/* Animated canvas background */}
      <canvas ref={canvasRef} style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%' }} />

      {/* Glowing orbs */}
      <div style={{ position:'absolute', width:'600px', height:'600px', borderRadius:'50%',
        background:'radial-gradient(circle, #00d4ff08, transparent 70%)',
        top:'-100px', left:'-100px', pointerEvents:'none' }} />
      <div style={{ position:'absolute', width:'500px', height:'500px', borderRadius:'50%',
        background:'radial-gradient(circle, #8b00ff08, transparent 70%)',
        bottom:'-100px', right:'-100px', pointerEvents:'none' }} />

      {/* Hero content */}
      <div style={{ position: 'relative', textAlign: 'center', zIndex: 10, padding: '40px' }}>

        {/* Film reel SVG icon */}
        <div style={{ marginBottom: '24px', animation: 'spin 8s linear infinite', display: 'inline-block' }}>
          <svg width="80" height="80" viewBox="0 0 80 80" fill="none">
            <circle cx="40" cy="40" r="38" stroke="#00d4ff" strokeWidth="2" strokeDasharray="6 4"/>
            <circle cx="40" cy="40" r="12" stroke="#00d4ff" strokeWidth="2"/>
            <circle cx="40" cy="40" r="4" fill="#00d4ff"/>
            {[0,60,120,180,240,300].map((deg,i) => (
              <circle key={i} cx={40 + 24*Math.cos(deg*Math.PI/180)} cy={40 + 24*Math.sin(deg*Math.PI/180)}
                r="5" fill="#00d4ff44" stroke="#00d4ff" strokeWidth="1"/>
            ))}
          </svg>
        </div>

        {/* Title */}
        <h1 style={{
          fontSize: '64px', fontWeight: '900', margin: '0 0 8px',
          background: 'linear-gradient(135deg, #00d4ff 0%, #fff 50%, #ff6b00 100%)',
          WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
          letterSpacing: '-2px', lineHeight: 1.1,
        }}>CineBook</h1>

        <p style={{
          fontSize: '20px', color: '#8899bb', marginBottom: '8px', fontWeight: '300', letterSpacing: '4px',
        }}>MOVIE TICKET BOOKING</p>

        <div style={{
          width: '120px', height: '2px', margin: '20px auto 32px',
          background: 'linear-gradient(90deg, transparent, #00d4ff, transparent)',
        }}/>

        <p style={{ color: '#667799', marginBottom: '48px', fontSize: '16px', letterSpacing: '1px' }}>
          🎬 Book tickets · Choose your seats · Enjoy the show
        </p>

        {/* CTA Buttons */}
        <div style={{ display: 'flex', gap: '24px', justifyContent: 'center', flexWrap: 'wrap' }}>
          <button onClick={() => navigate('/user-login')} style={btnStyle('#00d4ff')}>
            🎟️ &nbsp;Login as User
          </button>
          <button onClick={() => navigate('/admin-login')} style={btnStyle('#ff6b00', '#8b00ff')}>
            ⚡ &nbsp;Admin Portal
          </button>
        </div>

        <p style={{ color: '#445566', marginTop: '32px', fontSize: '13px' }}>
          New user? &nbsp;
          <span onClick={() => navigate('/user-register')} style={{ color: '#00d4ff', cursor: 'pointer', textDecoration: 'underline' }}>
            Create an account
          </span>
        </p>

        {/* Tags */}
        <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', marginTop: '48px', flexWrap: 'wrap' }}>
          {['CS202', 'CS237', 'CS209', 'CS221'].map(tag => (
            <span key={tag} style={{
              fontSize: '11px', padding: '4px 14px', borderRadius: '20px',
              background: '#ffffff08', border: '1px solid #ffffff15', color: '#aabbcc',
            }}>{tag}</span>
          ))}
        </div>
      </div>

      <style>{`
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
      `}</style>
    </div>
  );
};

const btnStyle = (color, color2 = color) => ({
  padding: '16px 40px', fontSize: '16px', fontWeight: '700', border: 'none',
  borderRadius: '50px', cursor: 'pointer', letterSpacing: '1px',
  background: `linear-gradient(135deg, ${color}, ${color2})`,
  color: '#fff', boxShadow: `0 0 30px ${color}55`,
  transition: 'all 0.3s ease', transform: 'scale(1)',
});

export default LandingPage;
