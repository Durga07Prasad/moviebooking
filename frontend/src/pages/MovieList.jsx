import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios';
import Navbar from '../components/Navbar';

const genreConfig = {
  'Action':          { color: '#ff4466', bg: '#ff446620' },
  'Horror-Comedy':   { color: '#aa44ff', bg: '#aa44ff20' },
  'Action-Drama':    { color: '#ff6600', bg: '#ff660020' },
  'Sci-Fi':          { color: '#00d4ff', bg: '#00d4ff20' },
  'Action-Thriller': { color: '#ff2244', bg: '#ff224420' },
  'Drama':           { color: '#00cc88', bg: '#00cc8820' },
  'Comedy':          { color: '#ffdd00', bg: '#ffdd0020' },
  'default':         { color: '#888', bg: '#88888820' },
};

const posterColors = [
  ['#1a1a2e','#16213e','#0f3460'],
  ['#2d1b69','#11998e','#38ef7d'],
  ['#c31432','#240b36'],
  ['#141e30','#243b55'],
  ['#0f0c29','#302b63','#24243e'],
  ['#1f1c2c','#928dab'],
  ['#fc4a1a','#f7b733'],
  ['#200122','#6f0000'],
];

export default function MovieList() {
  const [movies, setMovies]       = useState([]);
  const [filtered, setFiltered]   = useState([]);
  const [loading, setLoading]     = useState(true);
  const [search, setSearch]       = useState('');
  const [activeGenre, setGenre]   = useState('All');
  const navigate = useNavigate();

  const genres = ['All','Action','Horror-Comedy','Action-Drama','Sci-Fi','Action-Thriller','Drama','Comedy'];
  const languages = ['All','Kannada','Telugu','Hindi','English'];
  const [activeLang, setLang] = useState('All');

  useEffect(() => { fetchMovies(); }, []);
  useEffect(() => { applyFilters(); }, [movies, search, activeGenre, activeLang]);

  const fetchMovies = async () => {
    try {
      const res = await api.get('/api/movies');
      setMovies(res.data.data || []);
    } catch { setMovies([]); }
    finally { setLoading(false); }
  };

  const applyFilters = () => {
    let list = [...movies];
    if (search)           list = list.filter(m => m.title.toLowerCase().includes(search.toLowerCase()));
    if (activeGenre !== 'All')  list = list.filter(m => m.genre?.includes(activeGenre));
    if (activeLang  !== 'All')  list = list.filter(m => m.language === activeLang);
    setFiltered(list);
  };

  const getGenreStyle = (genre) =>
    genreConfig[genre] || genreConfig['default'];

  const ratingStars = (r) => {
    const full = Math.floor(r / 2);
    return '★'.repeat(full) + '☆'.repeat(5 - full);
  };

  return (
    <div style={{ minHeight:'100vh', background:'#07070e', fontFamily:"'Segoe UI',sans-serif" }}>
      <Navbar theme="user" />

      {/* Hero Banner */}
      <div style={{
        background:'linear-gradient(135deg, #0a0a1a 0%, #1a0a2e 50%, #0a1a2e 100%)',
        padding:'48px 24px 36px', textAlign:'center', borderBottom:'1px solid #ffffff11',
      }}>
        <h1 style={{ color:'#fff', fontSize:'36px', fontWeight:'900', margin:'0 0 8px',
          background:'linear-gradient(135deg,#00d4ff,#aa66ff)', WebkitBackgroundClip:'text',
          WebkitTextFillColor:'transparent' }}>
          🎬 Now Showing
        </h1>
        <p style={{ color:'#667799', margin:'0 0 24px' }}>
          {filtered.length} movies · Bengaluru
        </p>

        {/* Search */}
        <input
          placeholder="🔍  Search movies..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{
            width:'min(500px,90%)', padding:'12px 20px', borderRadius:'30px',
            background:'rgba(255,255,255,0.06)', border:'1px solid #ffffff22',
            color:'#fff', fontSize:'15px', outline:'none',
          }}
        />
      </div>

      <div style={{ maxWidth:'1400px', margin:'0 auto', padding:'24px' }}>

        {/* Genre pills */}
        <div style={{ display:'flex', gap:'8px', flexWrap:'wrap', marginBottom:'12px' }}>
          {genres.map(g => (
            <button key={g} onClick={() => setGenre(g)} style={{
              padding:'6px 18px', borderRadius:'20px', fontSize:'13px', fontWeight:'700',
              cursor:'pointer', border:'none', transition:'all 0.2s',
              background: activeGenre === g
                ? (genreConfig[g]?.color || '#00d4ff')
                : 'rgba(255,255,255,0.06)',
              color: activeGenre === g ? '#fff' : '#aaa',
            }}>{g}</button>
          ))}
          <span style={{ color:'#444', padding:'0 4px' }}>|</span>
          {languages.map(l => (
            <button key={l} onClick={() => setLang(l)} style={{
              padding:'6px 16px', borderRadius:'20px', fontSize:'13px', fontWeight:'600',
              cursor:'pointer', border:'1px solid',
              borderColor: activeLang === l ? '#00d4ff' : '#333',
              background: activeLang === l ? '#00d4ff22' : 'transparent',
              color: activeLang === l ? '#00d4ff' : '#666',
            }}>{l}</button>
          ))}
        </div>

        {/* Movie Grid */}
        {loading ? (
          <div style={{ textAlign:'center', padding:'80px', color:'#667799' }}>
            <div style={spinner} /><p style={{ marginTop:'16px' }}>Loading movies...</p>
          </div>
        ) : filtered.length === 0 ? (
          <div style={{ textAlign:'center', padding:'80px', color:'#667799' }}>
            <div style={{ fontSize:'64px', marginBottom:'16px' }}>🎭</div>
            <p>No movies found. Try a different filter.</p>
          </div>
        ) : (
          <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill,minmax(240px,1fr))', gap:'20px' }}>
            {filtered.map((movie, i) => {
              const gc = getGenreStyle(movie.genre);
              const colors = posterColors[i % posterColors.length];
              const grad = colors.length === 3
                ? `linear-gradient(135deg, ${colors[0]}, ${colors[1]}, ${colors[2]})`
                : `linear-gradient(135deg, ${colors[0]}, ${colors[1]})`;
              const initial = movie.title?.[0]?.toUpperCase() || '?';
              return (
                <div key={movie.id} style={{
                  background:'rgba(255,255,255,0.03)',
                  border:'1px solid #ffffff0d', borderRadius:'16px',
                  overflow:'hidden', transition:'transform 0.25s, box-shadow 0.25s',
                  cursor:'pointer',
                }} onMouseOver={e => { e.currentTarget.style.transform='translateY(-4px)'; e.currentTarget.style.boxShadow='0 16px 40px rgba(0,0,0,0.5)'; }}
                   onMouseOut={e  => { e.currentTarget.style.transform='translateY(0)'; e.currentTarget.style.boxShadow='none'; }}>

                  {/* Poster */}
                  <div style={{
                    height:'200px', background: grad, display:'flex',
                    alignItems:'center', justifyContent:'center', position:'relative',
                  }}>
                    <span style={{
                      fontSize:'80px', fontWeight:'900', color:'rgba(255,255,255,0.15)',
                      position:'absolute',
                    }}>{initial}</span>
                    <div style={{ position:'absolute', top:'10px', right:'10px',
                      background:'rgba(0,0,0,0.7)', borderRadius:'6px',
                      padding:'3px 8px', fontSize:'12px', fontWeight:'800',
                      color: movie.rating >= 8 ? '#00ff88' : movie.rating >= 7 ? '#ffdd00' : '#ff8800',
                    }}>⭐ {movie.rating}</div>
                    <div style={{ position:'absolute', bottom:'10px', left:'10px',
                      background: gc.bg, border:`1px solid ${gc.color}44`,
                      color: gc.color, borderRadius:'6px', padding:'3px 8px',
                      fontSize:'11px', fontWeight:'800',
                    }}>{movie.genre}</div>
                    {movie.certificate && (
                      <div style={{ position:'absolute', top:'10px', left:'10px',
                        background:'rgba(0,0,0,0.8)', borderRadius:'4px',
                        padding:'2px 6px', fontSize:'10px', color:'#ccc', fontWeight:'700',
                      }}>{movie.certificate}</div>
                    )}
                  </div>

                  {/* Info */}
                  <div style={{ padding:'16px' }}>
                    <h3 style={{ color:'#fff', margin:'0 0 6px', fontSize:'15px',
                      fontWeight:'800', lineHeight:'1.3' }}>
                      {movie.title}
                    </h3>
                    <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'12px' }}>
                      <span style={{ color:'#667799', fontSize:'12px' }}>
                        {movie.language} · {movie.durationMinutes} min
                      </span>
                      <span style={{ color:'#ffaa00', fontSize:'12px' }}>
                        {ratingStars(movie.rating)}
                      </span>
                    </div>
                    <button
                      onClick={() => navigate(`/movies/${movie.id}/shows`)}
                      style={{
                        width:'100%', padding:'10px', borderRadius:'10px', border:'none',
                        background:'linear-gradient(135deg, #00d4ff, #0066ff)',
                        color:'#fff', fontWeight:'800', fontSize:'14px',
                        cursor:'pointer', transition:'opacity 0.2s',
                        boxShadow:'0 4px 15px #00d4ff33',
                      }}
                      onMouseOver={e => e.target.style.opacity='0.85'}
                      onMouseOut={e  => e.target.style.opacity='1'}
                    >
                      🎫 Book Now
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
      <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>
    </div>
  );
}

const spinner = {
  width:'36px', height:'36px', margin:'0 auto',
  border:'3px solid #ffffff11', borderTop:'3px solid #00d4ff',
  borderRadius:'50%', animation:'spin 0.8s linear infinite',
};
