import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';

/**
 * ShowSelection Page — /movies/:movieId/shows
 *
 * Features:
 *  - Live IST clock ticking every second (top-right corner)
 *  - Client-side IST bookability check (mirrors backend logic)
 *  - Auto-refresh every 60 seconds (expired shows disappear live)
 *  - Date tabs — Today / Tomorrow / Day after (only if shows exist)
 *  - Colour-coded show timing buttons with 5 states
 *  - IST notice bar below date tabs
 *
 * OOAD: Observer — the component "observes" IST clock every second
 *        and re-evaluates show availability reactively.
 */

// ─── IST Utilities (client-side mirror of ISTTimeService.java) ─────────────

/** Returns current time adjusted to IST as a JS Date */
function nowIST() {
  return new Date(new Date().toLocaleString('en-US', { timeZone: 'Asia/Kolkata' }));
}

/**
 * Client-side IST bookability check — mirrors backend ISTTimeService.isShowBookable()
 * Grace period: 15 minutes after show start (same as backend)
 *
 * SOLID: O — this function has identical logic to backend; any policy change
 * must be updated in BOTH places (documented in ISTTimeService Javadoc).
 */
function isShowBookableIST(showDate, showTime) {
  try {
    const istNow  = nowIST();
    const [y, m, d] = showDate.split('-').map(Number);
    const [h, min]  = showTime.substring(0, 5).split(':').map(Number);
    const showDT  = new Date(y, m - 1, d, h, min, 0);
    const cutoff  = new Date(showDT.getTime() + 15 * 60 * 1000); // +15 min grace
    return istNow < cutoff;
  } catch { return false; }
}

/** Returns minutes from IST now until show */
function minutesUntilShowIST(showDate, showTime) {
  try {
    const istNow = nowIST();
    const [y, m, d] = showDate.split('-').map(Number);
    const [h, min]  = showTime.substring(0, 5).split(':').map(Number);
    const showDT = new Date(y, m - 1, d, h, min, 0);
    return Math.floor((showDT - istNow) / 60000);
  } catch { return -999; }
}

/** Format 24h time → "2:00 PM" */
function fmt12(t) {
  try {
    const [h, m] = t.substring(0, 5).split(':').map(Number);
    const ampm = h >= 12 ? 'PM' : 'AM';
    const hr   = h % 12 || 12;
    return `${hr}:${String(m).padStart(2, '0')} ${ampm}`;
  } catch { return t; }
}

/** Human-readable date label for a tab */
function dateLabel(dateStr) {
  const istNow = nowIST();
  const today  = new Date(istNow.getFullYear(), istNow.getMonth(), istNow.getDate());
  const [y, m, d] = dateStr.split('-').map(Number);
  const target = new Date(y, m - 1, d);
  const diff   = Math.round((target - today) / 86400000);
  if (diff === 0) return 'Today';
  if (diff === 1) return 'Tomorrow';
  const days = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
  return days[target.getDay()];
}

// ─── Slot config — emoji + colours per time ────────────────────────────────
const SLOT_CONFIG = {
  '10:00': { emoji: '🌅', color: '#F59E0B', bg: '#1a1500', label: 'Morning' },
  '14:00': { emoji: '☀️',  color: '#F97316', bg: '#1a0d00', label: 'Afternoon' },
  '18:00': { emoji: '🌆', color: '#8B5CF6', bg: '#0d0014', label: 'Evening' },
  '21:30': { emoji: '🌙', color: '#3B82F6', bg: '#00081a', label: 'Night' },
};

function getSlotConfig(time24) {
  const key = time24 ? time24.substring(0, 5) : '';
  return SLOT_CONFIG[key] || { emoji: '🎬', color: '#00d4ff', bg: '#001a1a', label: '' };
}

// ─── Main Component ──────────────────────────────────────────────────────────
export default function ShowSelection() {
  const { movieId } = useParams();
  const navigate    = useNavigate();

  const [movie,       setMovie]       = useState(null);
  const [showsByDate, setShowsByDate] = useState({});
  const [theatreMap,  setTheatreMap]  = useState({});
  const [selectedDate, setSelectedDate] = useState(null);
  const [loading,     setLoading]     = useState(true);
  const [error,       setError]       = useState('');
  const [istClock,    setIstClock]    = useState('');

  // ── Live IST clock — ticks every second ──────────────────────────────────
  useEffect(() => {
    const tick = () => setIstClock(
      new Date().toLocaleTimeString('en-IN', { timeZone: 'Asia/Kolkata', hour12: true })
    );
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, []);

  // ── Fetch movie, shows, theatres ─────────────────────────────────────────
  const fetchData = useCallback(async () => {
    try {
      const token = localStorage.getItem('token');
      const headers = token ? { Authorization: `Bearer ${token}` } : {};

      const [movieRes, showsRes, theatresRes] = await Promise.all([
        axios.get(`/api/movies/${movieId}`, { headers }),
        axios.get(`/api/shows/movie/${movieId}`, { headers }),
        axios.get('/api/theatres', { headers }),
      ]);

      setMovie(movieRes.data.data || movieRes.data);

      // Build theatre id → name map
      const tMap = {};
      const theatreList = theatresRes.data.data || theatresRes.data || [];
      theatreList.forEach(t => { tMap[t.id] = t.name; });
      setTheatreMap(tMap);

      // showsByDate from backend is already IST-filtered
      const rawByDate = showsRes.data.data || {};

      // Additional client-side bookability filter (safety net + live updates)
      const filtered = {};
      Object.entries(rawByDate).forEach(([date, shows]) => {
        const bookable = shows.filter(s => isShowBookableIST(s.showDate, s.showTime));
        if (bookable.length > 0) filtered[date] = bookable;
      });

      setShowsByDate(filtered);

      // Default to first available date
      const dates = Object.keys(filtered).sort();
      if (dates.length > 0) setSelectedDate(prev => prev || dates[0]);
      setError('');
    } catch (e) {
      setError('Failed to load shows. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [movieId]);

  // Initial fetch
  useEffect(() => { fetchData(); }, [fetchData]);

  // Auto-refresh every 60 seconds — shows disappear live when they expire
  useEffect(() => {
    const id = setInterval(fetchData, 60000);
    return () => clearInterval(id);
  }, [fetchData]);

  // ── Styles ───────────────────────────────────────────────────────────────
  const S = {
    page: {
      minHeight: '100vh', background: '#06060f',
      fontFamily: "'Inter','Segoe UI',sans-serif", color: '#fff',
      padding: '24px 16px',
    },
    header: {
      display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start',
      maxWidth: '960px', margin: '0 auto 28px',
    },
    clock: {
      background: '#0d0d1a', border: '1px solid #00d4ff33',
      borderRadius: '10px', padding: '8px 16px',
      color: '#00d4ff', fontFamily: 'monospace', fontSize: '14px',
      textAlign: 'center',
    },
    movieTitle: { fontSize: '26px', fontWeight: 700, color: '#fff', marginBottom: '6px' },
    movieMeta:  { color: '#667799', fontSize: '14px' },
    istBar: {
      maxWidth: '960px', margin: '0 auto 20px',
      background: '#0a0a1a', border: '1px solid #00d4ff22',
      borderRadius: '10px', padding: '10px 16px',
      color: '#667799', fontSize: '13px',
      display: 'flex', alignItems: 'center', gap: '8px',
    },
    dateTabs: {
      display: 'flex', gap: '10px', maxWidth: '960px', margin: '0 auto 24px',
      overflowX: 'auto', paddingBottom: '4px',
    },
    dateTab: (selected) => ({
      padding: '10px 24px', borderRadius: '12px', cursor: 'pointer',
      fontWeight: 600, fontSize: '14px', whiteSpace: 'nowrap',
      transition: 'all 0.2s',
      background: selected ? '#00d4ff' : '#0d0d1a',
      color: selected ? '#06060f' : '#8899aa',
      border: selected ? '1px solid #00d4ff' : '1px solid #1a1a2e',
    }),
    theatreSection: {
      maxWidth: '960px', margin: '0 auto 28px',
      background: '#0a0a1a', borderRadius: '16px',
      border: '1px solid #1a1a2e', overflow: 'hidden',
    },
    theatreName: {
      padding: '16px 20px', fontWeight: 600, fontSize: '16px',
      borderBottom: '1px solid #1a1a2e', color: '#cde',
      display: 'flex', alignItems: 'center', gap: '8px',
    },
    slotsRow: { padding: '16px 20px', display: 'flex', flexWrap: 'wrap', gap: '12px' },
    noShows: {
      maxWidth: '960px', margin: '40px auto', textAlign: 'center',
      color: '#667799', padding: '40px',
      background: '#0a0a1a', borderRadius: '16px',
    },
  };

  if (loading) return (
    <div style={{ ...S.page, display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
      <div style={{ color: '#00d4ff', fontSize: '18px' }}>Loading shows…</div>
    </div>
  );

  const dates = Object.keys(showsByDate).sort();
  const currentDateShows = selectedDate ? (showsByDate[selectedDate] || []) : [];

  // Group current date's shows by theatreId
  const byTheatre = {};
  currentDateShows.forEach(show => {
    const tid = show.theatreId;
    if (!byTheatre[tid]) byTheatre[tid] = [];
    byTheatre[tid].push(show);
  });

  return (
    <div style={S.page}>
      {/* Header + IST Clock */}
      <div style={S.header}>
        <div>
          <button
            onClick={() => navigate('/movies')}
            style={{ background: 'none', border: 'none', color: '#00d4ff',
              cursor: 'pointer', fontSize: '14px', marginBottom: '8px', padding: 0 }}>
            ← Back to Movies
          </button>
          {movie && (
            <>
              <div style={S.movieTitle}>{movie.title}</div>
              <div style={S.movieMeta}>
                {movie.genre} · {movie.language} · {movie.durationMinutes} min ·{' '}
                ⭐ {movie.rating}
              </div>
            </>
          )}
        </div>
        <div>
          <div style={S.clock}>🕐 {istClock}</div>
          <div style={{ textAlign: 'center', color: '#667799', fontSize: '11px', marginTop: '4px' }}>
            Indian Standard Time
          </div>
        </div>
      </div>

      {/* IST Notice */}
      <div style={S.istBar}>
        <span>ℹ️</span>
        <span>Show timings are in <strong style={{ color: '#00d4ff' }}>Indian Standard Time (IST)</strong>.
          Past shows are automatically hidden. Page refreshes every 60 seconds.</span>
      </div>

      {/* Date Tabs */}
      {dates.length > 0 && (
        <div style={S.dateTabs}>
          {dates.map(date => (
            <button
              key={date}
              style={S.dateTab(selectedDate === date)}
              onClick={() => setSelectedDate(date)}>
              {dateLabel(date)} &nbsp;
              <span style={{ fontWeight: 400, fontSize: '12px', opacity: 0.7 }}>
                {date.slice(5).replace('-', '/')}
              </span>
            </button>
          ))}
        </div>
      )}

      {/* Error */}
      {error && (
        <div style={{ maxWidth: '960px', margin: '0 auto 20px',
          background: '#1a0000', borderRadius: '10px', padding: '14px',
          color: '#ff6666', border: '1px solid #ff000033' }}>
          {error}
        </div>
      )}

      {/* No shows for selected date */}
      {dates.length === 0 && !loading && (
        <div style={S.noShows}>
          <div style={{ fontSize: '48px', marginBottom: '12px' }}>🎭</div>
          <div style={{ fontSize: '18px', color: '#8899aa', marginBottom: '8px' }}>
            All shows today may have already started.
          </div>
          <div style={{ fontSize: '14px', color: '#667799' }}>
            Please check tomorrow's shows or select another movie.
          </div>
        </div>
      )}

      {/* Theatre show groups */}
      {Object.entries(byTheatre).map(([theatreId, shows]) => (
        <div key={theatreId} style={S.theatreSection}>
          <div style={S.theatreName}>
            🎪 {theatreMap[theatreId] || theatreId}
          </div>
          <div style={S.slotsRow}>
            {shows.sort((a, b) => a.showTime.localeCompare(b.showTime)).map(show => (
              <ShowSlotButton
                key={show.id}
                show={show}
                onClick={() => navigate(`/shows/${show.id}/seats`)}
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

// ─── ShowSlotButton — individual timing button ─────────────────────────────
/**
 * ShowSlotButton — renders one show time button with 5 possible states:
 *   1. Bookable + available  → coloured, clickable
 *   2. Expired / started     → grey, "STARTED" badge
 *   3. Sold out              → grey, "SOLD OUT" badge
 *   4. Urgent (<60 min)      → coloured + "FAST FILLING" amber badge
 *   5. Starting soon (<15 min) → coloured + "Starting soon" label
 *
 * OOAD: Strategy — button appearance strategy selected per show state.
 */
function ShowSlotButton({ show, onClick }) {
  const [, forceUpdate] = useState(0);

  // Re-evaluate state every 30 seconds for live updates
  useEffect(() => {
    const id = setInterval(() => forceUpdate(n => n + 1), 30000);
    return () => clearInterval(id);
  }, []);

  const timeKey  = show.showTime ? show.showTime.substring(0, 5) : '';
  const cfg      = getSlotConfig(show.showTime);
  const minutes  = minutesUntilShowIST(show.showDate, show.showTime);
  const bookable = isShowBookableIST(show.showDate, show.showTime);
  const soldOut  = show.isSoldOut || show.availableSeats === 0;
  const started  = minutes < 0;
  const active   = bookable && !soldOut;
  const urgent   = active && minutes < 60;
  const soon     = active && minutes < 15;

  // Badge determination
  let badge = null;
  if (started)               badge = { text: 'STARTED',    color: '#ff4444' };
  else if (soldOut)          badge = { text: 'SOLD OUT',    color: '#ff4444' };
  else if (soon)             badge = { text: 'Starting soon', color: '#f59e0b' };
  else if (urgent)           badge = { text: 'FAST FILLING', color: '#f59e0b' };

  const statusLabel = show.status || (
    minutes < 0 ? 'STARTED' :
    minutes < 15 ? 'Starting soon' :
    minutes < 60 ? `In ${minutes} min` :
    `In ${Math.floor(minutes/60)}h ${minutes%60 > 0 ? minutes%60+'m' : ''}`
  );

  return (
    <div
      onClick={active ? onClick : undefined}
      style={{
        position: 'relative',
        padding: '12px 18px',
        borderRadius: '12px',
        cursor: active ? 'pointer' : 'not-allowed',
        background: active ? cfg.bg : '#111120',
        border: `1.5px solid ${active ? cfg.color + '55' : '#2a2a3e'}`,
        transition: 'all 0.2s',
        minWidth: '130px',
        opacity: active ? 1 : 0.55,
        userSelect: 'none',
      }}
      onMouseEnter={e => { if (active) e.currentTarget.style.background = cfg.bg.replace('00', '22'); }}
      onMouseLeave={e => { if (active) e.currentTarget.style.background = cfg.bg; }}>

      {/* Badge top-right */}
      {badge && (
        <div style={{
          position: 'absolute', top: '-8px', right: '-4px',
          background: badge.color, color: '#fff',
          fontSize: '9px', fontWeight: 700, padding: '2px 6px',
          borderRadius: '6px', letterSpacing: '0.5px',
        }}>{badge.text}</div>
      )}

      {/* Emoji + time */}
      <div style={{ color: active ? cfg.color : '#556', fontWeight: 700, fontSize: '16px' }}>
        {cfg.emoji} {fmt12(show.showTime)}
      </div>

      {/* Slot label */}
      <div style={{ color: '#8899aa', fontSize: '11px', marginTop: '2px' }}>
        {cfg.label}
      </div>

      {/* Status */}
      <div style={{
        color: active ? cfg.color + 'bb' : '#445',
        fontSize: '11px', marginTop: '4px',
      }}>
        {started ? 'Show started' : soldOut ? 'No seats left' : statusLabel}
      </div>

      {/* Price */}
      <div style={{ color: '#8899aa', fontSize: '11px', marginTop: '3px' }}>
        from ₹{show.price}
      </div>
    </div>
  );
}
