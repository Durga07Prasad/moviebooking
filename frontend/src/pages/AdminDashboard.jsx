import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios';
import Navbar from '../components/Navbar';
import Toast from '../components/Toast';

// AdminDashboard — Purple + orange theme, full CRUD
// OOAD: Actor: Admin; Use Cases: Manage Movies, Shows, Theatres
const AdminDashboard = () => {
  const navigate = useNavigate();
  const [tab, setTab] = useState('movies');
  const [movies, setMovies] = useState([]);
  const [shows, setShows] = useState([]);
  const [theatres, setTheatres] = useState([]);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);
  const [showMovieModal, setShowMovieModal] = useState(false);
  const [showShowModal, setShowShowModal] = useState(false);
  const [showTheatreModal, setShowTheatreModal] = useState(false);
  const [editMovie, setEditMovie] = useState(null);

  const [movieForm, setMovieForm] = useState({
    title:'', genre:'', language:'English', durationMinutes:120,
    rating:7.0, description:'', posterUrl:'', director:'', certificate:'UA', cast:[]
  });
  const [showForm, setShowForm] = useState({
    movieId:'', theatreId:'', showDate:'', showTime:'', ticketPrice:150, screen:'Screen 1', availableSeats:150
  });
  const [theatreForm, setTheatreForm] = useState({
    name:'', location:'', city:'', totalSeats:150, rows:10, columns:15
  });

  useEffect(() => { fetchData(); }, [tab]);

  const fetchData = async () => {
    setLoading(true);
    try {
      if (tab==='movies') {
        const res = await api.get('/api/admin/movies');
        setMovies(res.data.data || []);
      } else if (tab==='shows') {
        const [sr, mr, tr] = await Promise.all([
          api.get('/api/admin/shows'),
          api.get('/api/admin/movies'),
          api.get('/api/admin/theatres'),
        ]);
        setShows(sr.data.data || []);
        setMovies(mr.data.data || []);
        setTheatres(tr.data.data || []);
      } else {
        const res = await api.get('/api/admin/theatres');
        setTheatres(res.data.data || []);
      }
    } catch { setToast({ message:'Failed to load data', type:'error' }); }
    finally { setLoading(false); }
  };

  const saveMovie = async () => {
    try {
      if (editMovie) {
        await api.put(`/api/admin/movies/${editMovie.id}`, movieForm);
        setToast({ message:'Movie updated ✅', type:'success' });
      } else {
        await api.post('/api/admin/movies', movieForm);
        setToast({ message:'Movie added ✅', type:'success' });
      }
      setShowMovieModal(false); setEditMovie(null);
      resetMovieForm(); fetchData();
    } catch (err) { setToast({ message: err.response?.data?.message || 'Failed', type:'error' }); }
  };

  const deleteMovie = async (id) => {
    if (!window.confirm('Delete this movie?')) return;
    try {
      await api.delete(`/api/admin/movies/${id}`);
      setToast({ message:'Movie deleted', type:'info' });
      fetchData();
    } catch { setToast({ message:'Delete failed', type:'error' }); }
  };

  const saveShow = async () => {
    try {
      await api.post('/api/admin/shows', showForm);
      setToast({ message:'Show created with 150 seats! ✅', type:'success' });
      setShowShowModal(false); fetchData();
    } catch (err) { setToast({ message: err.response?.data?.message || 'Failed', type:'error' }); }
  };

  const saveTheatre = async () => {
    try {
      await api.post('/api/admin/theatres', theatreForm);
      setToast({ message:'Theatre added ✅', type:'success' });
      setShowTheatreModal(false); fetchData();
    } catch (err) { setToast({ message: err.response?.data?.message || 'Failed', type:'error' }); }
  };

  const openEditMovie = (m) => {
    setEditMovie(m);
    setMovieForm({ title:m.title, genre:m.genre, language:m.language, durationMinutes:m.durationMinutes,
      rating:m.rating, description:m.description, posterUrl:m.posterUrl||'', director:m.director||'',
      certificate:m.certificate||'UA', cast:m.cast||[] });
    setShowMovieModal(true);
  };

  const resetMovieForm = () => setMovieForm({
    title:'', genre:'', language:'English', durationMinutes:120,
    rating:7.0, description:'', posterUrl:'', director:'', certificate:'UA', cast:[]
  });

  const tabs = ['movies','shows','theatres'];
  const accent = '#ff6b00';

  return (
    <div style={{ minHeight:'100vh', background:'#080810', fontFamily:"'Segoe UI', sans-serif" }}>
      <Navbar theme="admin"/>

      <div style={{ display:'flex', minHeight:'calc(100vh - 64px)' }}>
        {/* Sidebar */}
        <div style={{
          width:'220px', background:'rgba(139,0,255,0.05)',
          borderRight:'1px solid #8b00ff22', padding:'24px 16px', flexShrink:0,
        }}>
          <div style={{ color:'#8899bb', fontSize:'11px', fontWeight:'700', letterSpacing:'2px', marginBottom:'16px' }}>
            MANAGEMENT
          </div>
          {tabs.map(t => (
            <button key={t} onClick={() => setTab(t)} style={{
              width:'100%', padding:'12px 16px', marginBottom:'8px',
              background: tab===t ? 'rgba(255,107,0,0.15)' : 'transparent',
              border: tab===t ? '1px solid #ff6b0044' : '1px solid transparent',
              borderRadius:'10px', color: tab===t ? '#ff6b00' : '#8899bb',
              cursor:'pointer', fontSize:'14px', fontWeight:tab===t ? '700' : '400',
              textAlign:'left', display:'flex', alignItems:'center', gap:'10px',
              transition:'all 0.2s',
            }}>
              {t==='movies'?'🎬':t==='shows'?'📅':'🏛️'} {t.charAt(0).toUpperCase()+t.slice(1)}
            </button>
          ))}

          {/* Quick stats */}
          <div style={{ marginTop:'32px', borderTop:'1px solid #ffffff11', paddingTop:'20px' }}>
            <div style={{ color:'#8899bb', fontSize:'11px', fontWeight:'700', letterSpacing:'2px', marginBottom:'12px' }}>STATS</div>
            {[
              { label:'Movies', val: movies.length, color:'#ff6b00' },
              { label:'Shows', val: shows.length, color:'#8b00ff' },
              { label:'Theatres', val: theatres.length, color:'#00d4ff' },
            ].map(s => (
              <div key={s.label} style={{ display:'flex', justifyContent:'space-between', marginBottom:'8px' }}>
                <span style={{ color:'#667799', fontSize:'13px' }}>{s.label}</span>
                <span style={{ color:s.color, fontWeight:'700' }}>{s.val}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Main content */}
        <div style={{ flex:1, padding:'32px', overflowY:'auto' }}>
          {/* Header */}
          <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'28px' }}>
            <h1 style={{
              fontSize:'26px', fontWeight:'900',
              background:'linear-gradient(135deg, #ff6b00, #ffcc00)',
              WebkitBackgroundClip:'text', WebkitTextFillColor:'transparent', margin:0,
            }}>
              {tab==='movies'?'🎬 Movie Management':tab==='shows'?'📅 Show Management':'🏛️ Theatre Management'}
            </h1>
            <button onClick={() => {
              if (tab==='movies') { resetMovieForm(); setEditMovie(null); setShowMovieModal(true); }
              else if (tab==='shows') setShowShowModal(true);
              else setShowTheatreModal(true);
            }} style={{
              padding:'10px 22px', background:'linear-gradient(135deg, #ff6b00, #ff9900)',
              border:'none', borderRadius:'10px', color:'#fff', cursor:'pointer',
              fontWeight:'700', fontSize:'14px', boxShadow:'0 0 20px #ff6b0033',
            }}>
              + Add {tab==='movies'?'Movie':tab==='shows'?'Show':'Theatre'}
            </button>
          </div>

          {loading && <div style={{ textAlign:'center', padding:'60px', color:'#667799' }}>Loading...</div>}

          {/* ----- MOVIES TABLE ----- */}
          {!loading && tab==='movies' && (
            <div style={{ overflowX:'auto' }}>
              <table style={{ width:'100%', borderCollapse:'collapse' }}>
                <thead>
                  <tr style={{ borderBottom:'1px solid #ffffff11' }}>
                    {['Title','Genre','Language','Duration','Rating','Cert','Status','Actions'].map(h => (
                      <th key={h} style={{ padding:'12px 16px', textAlign:'left', color:'#8899bb',
                        fontSize:'12px', fontWeight:'700', letterSpacing:'1px', textTransform:'uppercase' }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {movies.map(m => (
                    <tr key={m.id} style={{ borderBottom:'1px solid #ffffff08', transition:'background 0.2s' }}
                      onMouseEnter={e => e.currentTarget.style.background='rgba(255,107,0,0.03)'}
                      onMouseLeave={e => e.currentTarget.style.background='transparent'}>
                      <td style={{ padding:'14px 16px', color:'#fff', fontWeight:'600' }}>{m.title}</td>
                      <td style={{ padding:'14px 16px' }}>
                        <span style={{ background:'#ff6b0022', color:'#ff6b00', padding:'3px 10px',
                          borderRadius:'20px', fontSize:'12px', fontWeight:'700' }}>{m.genre}</span>
                      </td>
                      <td style={{ padding:'14px 16px', color:'#aaa' }}>{m.language}</td>
                      <td style={{ padding:'14px 16px', color:'#aaa' }}>{m.durationMinutes}m</td>
                      <td style={{ padding:'14px 16px', color:'#ffaa00' }}>⭐ {m.rating}</td>
                      <td style={{ padding:'14px 16px', color:'#aaa' }}>{m.certificate}</td>
                      <td style={{ padding:'14px 16px' }}>
                        <span style={{ color: m.active?'#00ff88':'#ff4466', fontSize:'12px', fontWeight:'700' }}>
                          {m.active?'● Active':'○ Inactive'}
                        </span>
                      </td>
                      <td style={{ padding:'14px 16px' }}>
                        <div style={{ display:'flex', gap:'8px' }}>
                          <button onClick={() => openEditMovie(m)} style={actionBtn('#00d4ff')}>Edit</button>
                          <button onClick={() => deleteMovie(m.id)} style={actionBtn('#ff4466')}>Delete</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {movies.length===0 && (
                    <tr><td colSpan={8} style={{ padding:'60px', textAlign:'center', color:'#667799' }}>
                      No movies yet. Add your first movie!
                    </td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}

          {/* ----- SHOWS TABLE ----- */}
          {!loading && tab==='shows' && (
            <div style={{ overflowX:'auto' }}>
              <table style={{ width:'100%', borderCollapse:'collapse' }}>
                <thead>
                  <tr style={{ borderBottom:'1px solid #ffffff11' }}>
                    {['Movie ID','Theatre ID','Date','Time','Price','Seats Left','Screen'].map(h => (
                      <th key={h} style={{ padding:'12px 16px', textAlign:'left', color:'#8899bb',
                        fontSize:'12px', fontWeight:'700', letterSpacing:'1px', textTransform:'uppercase' }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {shows.map(s => (
                    <tr key={s.id} style={{ borderBottom:'1px solid #ffffff08' }}>
                      <td style={{ padding:'14px 16px', color:'#ccc', fontSize:'12px' }}>{s.movieId?.slice(-8)}</td>
                      <td style={{ padding:'14px 16px', color:'#ccc', fontSize:'12px' }}>{s.theatreId?.slice(-8)}</td>
                      <td style={{ padding:'14px 16px', color:'#fff', fontWeight:'600' }}>{String(s.showDate)}</td>
                      <td style={{ padding:'14px 16px', color:'#fff' }}>{String(s.showTime)}</td>
                      <td style={{ padding:'14px 16px', color:'#ff6b00', fontWeight:'700' }}>₹{s.ticketPrice}</td>
                      <td style={{ padding:'14px 16px' }}>
                        <span style={{ color: s.availableSeats>20?'#00ff88':'#ffaa00', fontWeight:'700' }}>
                          {s.availableSeats}
                        </span>
                      </td>
                      <td style={{ padding:'14px 16px', color:'#aaa' }}>{s.screen}</td>
                    </tr>
                  ))}
                  {shows.length===0 && (
                    <tr><td colSpan={7} style={{ padding:'60px', textAlign:'center', color:'#667799' }}>
                      No shows yet. Create a show!
                    </td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}

          {/* ----- THEATRES ----- */}
          {!loading && tab==='theatres' && (
            <div style={{ display:'grid', gap:'16px', gridTemplateColumns:'repeat(auto-fill, minmax(280px, 1fr))' }}>
              {theatres.map(t => (
                <div key={t.id} style={{
                  background:'rgba(139,0,255,0.05)', border:'1px solid #8b00ff22',
                  borderRadius:'16px', padding:'20px',
                }}>
                  <div style={{ color:'#ff6b00', fontSize:'20px', fontWeight:'800', marginBottom:'8px' }}>
                    🏛️ {t.name}
                  </div>
                  <div style={{ color:'#8899bb', fontSize:'13px', lineHeight:'1.8' }}>
                    <div>📍 {t.location}, {t.city}</div>
                    <div>💺 {t.totalSeats} total seats ({t.rows} rows × {t.columns} cols)</div>
                    <div style={{ color: t.active?'#00ff88':'#ff4466', marginTop:'8px', fontWeight:'700' }}>
                      {t.active?'● Active':'○ Inactive'}
                    </div>
                  </div>
                </div>
              ))}
              {theatres.length===0 && (
                <div style={{ gridColumn:'1/-1', textAlign:'center', padding:'60px', color:'#667799' }}>
                  No theatres yet. Add a theatre!
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* ----- MOVIE MODAL ----- */}
      {showMovieModal && (
        <Modal title={editMovie ? '✏️ Edit Movie' : '🎬 Add New Movie'} onClose={() => { setShowMovieModal(false); setEditMovie(null); }}>
          <FormGrid>
            {[
              { label:'Title', field:'title', type:'text', placeholder:'Movie title' },
              { label:'Genre', field:'genre', type:'text', placeholder:'Action / Drama / Comedy...' },
              { label:'Language', field:'language', type:'text', placeholder:'English' },
              { label:'Director', field:'director', type:'text', placeholder:'Director name' },
              { label:'Duration (min)', field:'durationMinutes', type:'number', placeholder:'120' },
              { label:'Rating (0-10)', field:'rating', type:'number', placeholder:'7.5' },
              { label:'Certificate', field:'certificate', type:'text', placeholder:'U / UA / A' },
              { label:'Poster URL', field:'posterUrl', type:'text', placeholder:'https://...' },
            ].map(({ label, field, type, placeholder }) => (
              <div key={field} style={{ marginBottom:'14px' }}>
                <label style={{ display:'block', color:'#8899bb', fontSize:'12px', fontWeight:'700',
                  marginBottom:'6px', textTransform:'uppercase', letterSpacing:'0.5px' }}>{label}</label>
                <input type={type} placeholder={placeholder} value={movieForm[field]}
                  onChange={e => setMovieForm({ ...movieForm, [field]: type==='number' ? +e.target.value : e.target.value })}
                  style={modalInput}/>
              </div>
            ))}
          </FormGrid>
          <div style={{ marginBottom:'14px' }}>
            <label style={{ display:'block', color:'#8899bb', fontSize:'12px', fontWeight:'700',
              marginBottom:'6px', textTransform:'uppercase' }}>Description</label>
            <textarea placeholder="Movie description..." rows={3} value={movieForm.description}
              onChange={e => setMovieForm({ ...movieForm, description: e.target.value })}
              style={{ ...modalInput, resize:'vertical', height:'80px' }}/>
          </div>
          <button onClick={saveMovie} style={saveBtnStyle}>
            {editMovie ? '✅ Update Movie' : '🚀 Add Movie'}
          </button>
        </Modal>
      )}

      {/* ----- SHOW MODAL ----- */}
      {showShowModal && (
        <Modal title="📅 Create Show" onClose={() => setShowShowModal(false)}>
          <div style={{ marginBottom:'14px' }}>
            <label style={modalLabel}>Select Movie</label>
            <select value={showForm.movieId} onChange={e => setShowForm({...showForm, movieId:e.target.value})} style={modalInput}>
              <option value="">-- Select Movie --</option>
              {movies.map(m => <option key={m.id} value={m.id}>{m.title}</option>)}
            </select>
          </div>
          <div style={{ marginBottom:'14px' }}>
            <label style={modalLabel}>Select Theatre</label>
            <select value={showForm.theatreId} onChange={e => setShowForm({...showForm, theatreId:e.target.value})} style={modalInput}>
              <option value="">-- Select Theatre --</option>
              {theatres.map(t => <option key={t.id} value={t.id}>{t.name} - {t.city}</option>)}
            </select>
          </div>
          <FormGrid>
            {[
              { label:'Show Date', field:'showDate', type:'date' },
              { label:'Show Time', field:'showTime', type:'time' },
              { label:'Ticket Price (₹)', field:'ticketPrice', type:'number', placeholder:'150' },
              { label:'Screen', field:'screen', type:'text', placeholder:'Screen 1' },
            ].map(({ label, field, type, placeholder }) => (
              <div key={field} style={{ marginBottom:'14px' }}>
                <label style={modalLabel}>{label}</label>
                <input type={type} placeholder={placeholder} value={showForm[field]}
                  onChange={e => setShowForm({ ...showForm, [field]: type==='number'?+e.target.value:e.target.value })}
                  style={modalInput}/>
              </div>
            ))}
          </FormGrid>
          <button onClick={saveShow} style={saveBtnStyle}>🚀 Create Show</button>
        </Modal>
      )}

      {/* ----- THEATRE MODAL ----- */}
      {showTheatreModal && (
        <Modal title="🏛️ Add Theatre" onClose={() => setShowTheatreModal(false)}>
          <FormGrid>
            {[
              { label:'Name', field:'name', type:'text', placeholder:'PVR Cinemas' },
              { label:'Location', field:'location', type:'text', placeholder:'Forum Mall' },
              { label:'City', field:'city', type:'text', placeholder:'Bangalore' },
              { label:'Total Seats', field:'totalSeats', type:'number', placeholder:'150' },
              { label:'Rows', field:'rows', type:'number', placeholder:'10' },
              { label:'Columns', field:'columns', type:'number', placeholder:'15' },
            ].map(({ label, field, type, placeholder }) => (
              <div key={field} style={{ marginBottom:'14px' }}>
                <label style={modalLabel}>{label}</label>
                <input type={type} placeholder={placeholder} value={theatreForm[field]}
                  onChange={e => setTheatreForm({ ...theatreForm, [field]: type==='number'?+e.target.value:e.target.value })}
                  style={modalInput}/>
              </div>
            ))}
          </FormGrid>
          <button onClick={saveTheatre} style={saveBtnStyle}>🚀 Add Theatre</button>
        </Modal>
      )}

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)}/>}
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
};

// ── Modal wrapper ──
const Modal = ({ title, children, onClose }) => (
  <div style={{ position:'fixed', inset:0, background:'rgba(0,0,0,0.85)', zIndex:1000,
    display:'flex', alignItems:'center', justifyContent:'center', padding:'20px' }} onClick={onClose}>
    <div style={{
      background:'#0d0d1a', border:'1px solid #ff6b0044', borderRadius:'20px',
      padding:'32px', maxWidth:'640px', width:'100%', maxHeight:'85vh', overflowY:'auto',
    }} onClick={e => e.stopPropagation()}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'24px' }}>
        <h2 style={{ color:'#ff6b00', margin:0, fontSize:'20px' }}>{title}</h2>
        <button onClick={onClose} style={{ background:'none', border:'none', color:'#667799',
          fontSize:'24px', cursor:'pointer' }}>×</button>
      </div>
      {children}
    </div>
  </div>
);

const FormGrid = ({ children }) => (
  <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'0 16px' }}>{children}</div>
);

const modalInput = {
  width:'100%', padding:'10px 14px', background:'rgba(255,255,255,0.05)',
  border:'1px solid #ffffff22', borderRadius:'8px', color:'#fff',
  fontSize:'14px', outline:'none', boxSizing:'border-box',
  fontFamily:"'Segoe UI', sans-serif",
};

const modalLabel = {
  display:'block', color:'#8899bb', fontSize:'12px', fontWeight:'700',
  marginBottom:'6px', textTransform:'uppercase', letterSpacing:'0.5px',
};

const saveBtnStyle = {
  width:'100%', marginTop:'8px', padding:'14px', fontSize:'15px', fontWeight:'700',
  background:'linear-gradient(135deg, #ff6b00, #ff9900)',
  border:'none', borderRadius:'12px', color:'#fff', cursor:'pointer',
  boxShadow:'0 0 25px #ff6b0033', transition:'all 0.3s',
};

const actionBtn = (color) => ({
  padding:'6px 14px', background:`${color}11`, border:`1px solid ${color}44`,
  borderRadius:'6px', color:color, cursor:'pointer', fontSize:'12px', fontWeight:'700',
});

export default AdminDashboard;
