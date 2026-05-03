import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/axios';
import Navbar from '../components/Navbar';

/**
 * SeatSelection — 10×15 seat grid with IST-aware bookability.
 *
 * API response shape from GET /api/shows/{showId}/seats:
 * {
 *   show:          { id, movieId, theatreId, showDate, showTime, price, ... }
 *   allSeats:      [ { id, seatNumber, row, type, price, isBooked, bookedByUserId } ]
 *   seatsByRow:    { "A": [...], "B": [...], ... }   ← optional
 *   movie:         { title, genre, ... }
 *   theatre:       { name, city, ... }
 *   totalSeats:    150
 *   bookedCount:   N
 *   availableCount: M
 * }
 *
 * Key field names (match Seat.java exactly):
 *   seat.type      (not seatType)
 *   seat.isBooked  (not booked) — serialised as "isBooked" by Jackson
 *
 * SOLID: S — only seat selection UI, no payment logic.
 * OOAD: GRASP Controller — handleProceed creates booking then navigates.
 */

const SEAT_COLORS = {
  VIP:     { avail: '#b8860b', selected: '#ffd700', booked: '#1e1c0c', text: '#ffd700' },
  PREMIUM: { avail: '#4a1a6b', selected: '#aa44ff', booked: '#150d20', text: '#aa44ff' },
  REGULAR: { avail: '#0a3a4a', selected: '#00d4ff', booked: '#06141a', text: '#00d4ff' },
};

export default function SeatSelection() {
  const { showId } = useParams();
  const navigate   = useNavigate();

  const [data,    setData]    = useState(null);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState([]);   // array of seat objects
  const [booking,  setBooking]  = useState(false);
  const [error,    setError]    = useState('');

  useEffect(() => { fetchSeats(); }, [showId]);

  const fetchSeats = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/api/shows/${showId}/seats`);
      const d   = res.data.data;

      // ── NORMALISE: always convert to flat array ──────────────────────
      // Backend sends: { allSeats: [...], seatsByRow: {...}, show, movie, theatre, ... }
      let flatSeats = [];
      if (d?.allSeats && Array.isArray(d.allSeats)) {
        // Primary: use allSeats flat array
        flatSeats = d.allSeats;
      } else if (d?.seats && Array.isArray(d.seats)) {
        // Fallback 1: seats key
        flatSeats = d.seats;
      } else if (d?.seatsByRow && typeof d.seatsByRow === 'object') {
        // Fallback 2: flatten seatsByRow
        flatSeats = Object.values(d.seatsByRow).flat();
      } else if (Array.isArray(d)) {
        // Fallback 3: top-level array
        flatSeats = d;
      }

      console.log(`[SEATS] Loaded ${flatSeats.length} seats for show ${showId}`);
      console.log('[SEATS] Sample:', flatSeats[0]);

      // Normalise field inconsistencies: type vs seatType, isBooked vs booked
      flatSeats = flatSeats.map(s => ({
        ...s,
        type:     s.type     || s.seatType || 'REGULAR',
        isBooked: s.isBooked ?? s.booked ?? false,
        row:      s.row      || (s.seatNumber ? s.seatNumber.charAt(0) : 'A'),
      }));

      setData({ ...d, normalSeats: flatSeats });
    } catch (e) {
      console.error('[SEATS] Error:', e);
      setError('Failed to load seats. ' + (e.response?.data?.message || e.message));
    } finally { setLoading(false); }
  };

  // ── Seat selection toggle ─────────────────────────────────────────────────
  const toggleSeat = (seat) => {
    if (seat.isBooked) return;
    setSelected(prev => {
      const already = prev.find(s => s.id === seat.id);
      if (already) return prev.filter(s => s.id !== seat.id);
      if (prev.length >= 8) { setError('Maximum 8 seats allowed per booking!'); return prev; }
      setError('');
      return [...prev, seat];
    });
  };

  const totalPrice = selected.reduce((sum, s) => sum + (s.price || 0), 0);

  // ── Book selected seats → navigate to payment ────────────────────────────
  const handleProceed = async () => {
    if (selected.length === 0) { setError('Please select at least one seat.'); return; }
    setBooking(true); setError('');
    try {
      // Step 1: create PENDING booking
      const res = await api.post('/api/bookings', {
        showId,
        seatNumbers: selected.map(s => s.seatNumber),
      });
      const bookingData = res.data.data;
      const bookingId   = bookingData?.id;

      // Step 2: navigate to payment with full context
      navigate(`/payment/${bookingId}`, {
        state: {
          bookingId,
          showId,
          movieTitle:   data?.movie?.title,
          theatreName:  data?.theatre?.name,
          showDate:     data?.show?.showDate,
          showTime:     data?.show?.showTime,
          selectedSeats: selected.map(s => s.seatNumber),
          selectedSeatObjects: selected,
          totalAmount: totalPrice,
        },
      });
    } catch (err) {
      setError(err.response?.data?.message || 'Booking failed. Seats may have been taken.');
      fetchSeats();
      setSelected([]);
    } finally { setBooking(false); }
  };

  // ── Helpers ───────────────────────────────────────────────────────────────
  const rowType = (r) =>
    ['A','B'].includes(r) ? 'VIP' :
    ['C','D','E'].includes(r) ? 'PREMIUM' : 'REGULAR';

  // ── Loading ───────────────────────────────────────────────────────────────
  if (loading) return (
    <div style={{ minHeight:'100vh', background:'#07070e', display:'flex', alignItems:'center', justifyContent:'center' }}>
      <div>
        <div style={spinnerStyle}/>
        <p style={{ color:'#667799', textAlign:'center', marginTop:'16px', fontFamily:"'Segoe UI',sans-serif" }}>
          Loading seat map…
        </p>
      </div>
    </div>
  );

  // ── Derive from normalised data ───────────────────────────────────────────
  const seats   = data?.normalSeats || [];
  const show    = data?.show;
  const movie   = data?.movie;
  const theatre = data?.theatre;

  // Build seatsByRow from flat normalised array
  const ROWS = ['A','B','C','D','E','F','G','H','I','J'];
  const seatsByRow = {};
  ROWS.forEach(r => {
    seatsByRow[r] = seats
      .filter(s => s.row === r)
      .sort((a, b) => {
        const na = parseInt(a.seatNumber?.slice(1) || '0');
        const nb = parseInt(b.seatNumber?.slice(1) || '0');
        return na - nb;
      });
  });

  const bookedCount    = seats.filter(s => s.isBooked).length;
  const availableCount = seats.length - bookedCount;

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div style={{ minHeight:'100vh', background:'#07070e', fontFamily:"'Segoe UI',sans-serif" }}>
      <Navbar theme="user"/>

      {/* Header */}
      <div style={{ background:'#0d0d1a', padding:'14px 24px', borderBottom:'1px solid #ffffff11' }}>
        <div style={{ maxWidth:'1200px', margin:'0 auto' }}>
          <button onClick={() => navigate(-1)}
            style={{ background:'none', border:'none', color:'#667799', cursor:'pointer', fontSize:'13px', marginBottom:'6px' }}>
            ← Back
          </button>
          <h2 style={{ color:'#fff', margin:'0 0 4px', fontSize:'18px', fontWeight:'900' }}>
            🎬 {movie?.title || 'Movie'}
          </h2>
          <p style={{ color:'#667799', margin:0, fontSize:'13px' }}>
            🎪 {theatre?.name} &nbsp;·&nbsp;
            📅 {show?.showDate} &nbsp;·&nbsp;
            ⏰ {String(show?.showTime || '').slice(0, 5)} &nbsp;·&nbsp;
            💰 From ₹{show?.price}
          </p>
        </div>
      </div>

      <div style={{ display:'flex', maxWidth:'1200px', margin:'0 auto' }}>

        {/* ── SEAT GRID ── */}
        <div style={{ flex:1, padding:'24px', overflowX:'auto' }}>

          {/* Screen curve */}
          <div style={{ textAlign:'center', marginBottom:'28px' }}>
            <div style={{
              display:'inline-block', padding:'8px 80px 18px',
              background:'linear-gradient(180deg,rgba(255,255,255,0.15) 0%,transparent 100%)',
              borderRadius:'0 0 50% 50% / 0 0 30px 30px',
              borderBottom:'2px solid rgba(255,255,255,0.2)',
              color:'rgba(255,255,255,0.5)', fontSize:'12px', letterSpacing:'6px',
              fontWeight:'700', width:'min(500px,90%)',
            }}>S C R E E N</div>
          </div>

          {/* Legend */}
          <div style={{ display:'flex', justifyContent:'center', gap:'16px', marginBottom:'20px', flexWrap:'wrap' }}>
            {[
              { label:'VIP (A–B)',       color:'#ffd700' },
              { label:'Premium (C–E)',   color:'#aa44ff' },
              { label:'Regular (F–J)',   color:'#00d4ff' },
              { label:'Selected',        color:'#00ff88' },
              { label:'Booked',          color:'#2a2a2a' },
            ].map(l => (
              <div key={l.label} style={{ display:'flex', alignItems:'center', gap:'6px' }}>
                <div style={{ width:'18px', height:'18px', borderRadius:'3px', background:l.color, border:'1px solid rgba(255,255,255,0.2)' }}/>
                <span style={{ color:'#667799', fontSize:'11px' }}>{l.label}</span>
              </div>
            ))}
          </div>

          {/* Error */}
          {error && (
            <div style={{ background:'#ff446620', border:'1px solid #ff4466', color:'#ff4466',
              padding:'10px 16px', borderRadius:'8px', marginBottom:'16px', textAlign:'center', fontSize:'13px' }}>
              {error}
            </div>
          )}

          {/* DEBUG: show count if seats missing */}
          {seats.length === 0 && !loading && (
            <div style={{ color:'#ff9900', textAlign:'center', padding:'20px' }}>
              ⚠️ No seats loaded. Check console for API response.
            </div>
          )}

          {/* Seat rows A–J */}
          <div style={{ display:'flex', flexDirection:'column', gap:'6px', alignItems:'flex-start' }}>
            {ROWS.map(row => {
              const type    = rowType(row);
              const colors  = SEAT_COLORS[type];
              const rowSeats = seatsByRow[row] || [];

              return (
                <div key={row} style={{ display:'flex', alignItems:'center', gap:'6px' }}>
                  {/* Row label */}
                  <div style={{
                    width:'22px', textAlign:'center', color:colors.text,
                    fontSize:'13px', fontWeight:'800', flexShrink:0,
                  }}>{row}</div>

                  {/* Left 7 seats */}
                  <div style={{ display:'flex', gap:'4px' }}>
                    {rowSeats.slice(0, 7).map(seat => (
                      <SeatButton key={seat.id || seat.seatNumber}
                        seat={seat} colors={colors}
                        isSelected={!!selected.find(s => s.id === seat.id)}
                        onClick={() => toggleSeat(seat)} />
                    ))}
                  </div>

                  {/* Aisle */}
                  <div style={{ width:'14px' }}/>

                  {/* Right 8 seats */}
                  <div style={{ display:'flex', gap:'4px' }}>
                    {rowSeats.slice(7, 15).map(seat => (
                      <SeatButton key={seat.id || seat.seatNumber}
                        seat={seat} colors={colors}
                        isSelected={!!selected.find(s => s.id === seat.id)}
                        onClick={() => toggleSeat(seat)} />
                    ))}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Price reference */}
          <div style={{ display:'flex', justifyContent:'center', gap:'24px', marginTop:'20px', flexWrap:'wrap' }}>
            {[
              { label:'VIP',     price: show ? show.price * 2   : '—' },
              { label:'PREMIUM', price: show ? show.price * 1.5 : '—' },
              { label:'REGULAR', price: show ? show.price       : '—' },
            ].map(p => (
              <span key={p.label} style={{ color:'#667799', fontSize:'12px' }}>
                {p.label}: ₹{p.price}
              </span>
            ))}
          </div>
        </div>

        {/* ── RIGHT PANEL ── */}
        <div style={{
          width:'270px', flexShrink:0, background:'rgba(255,255,255,0.02)',
          borderLeft:'1px solid #ffffff0a', padding:'24px', minHeight:'500px',
        }}>
          <h3 style={{ color:'#fff', margin:'0 0 16px', fontSize:'15px', fontWeight:'800' }}>
            🎫 Seats ({selected.length}/8)
          </h3>

          {/* Stats */}
          <div style={{ padding:'10px 12px', background:'rgba(255,255,255,0.03)',
            borderRadius:'8px', marginBottom:'16px', fontSize:'12px', color:'#667799' }}>
            <div>✅ Available: <span style={{ color:'#00ff88' }}>{availableCount}</span></div>
            <div>❌ Booked: <span style={{ color:'#ff4466' }}>{bookedCount}</span></div>
            <div>📊 Total: <span style={{ color:'#fff' }}>{seats.length}</span></div>
          </div>

          {selected.length === 0 ? (
            <p style={{ color:'#445566', fontSize:'13px', lineHeight:'1.6' }}>
              Click on any available seat to select it.<br/>
              Max 8 seats per booking.
            </p>
          ) : (
            <>
              {/* Selected seat list */}
              <div style={{ display:'flex', flexDirection:'column', gap:'6px', marginBottom:'14px' }}>
                {selected.map(s => (
                  <div key={s.id} style={{
                    display:'flex', justifyContent:'space-between', alignItems:'center',
                    background:'rgba(255,255,255,0.04)', borderRadius:'8px', padding:'8px 10px',
                  }}>
                    <div>
                      <div style={{ color:'#fff', fontWeight:'700', fontSize:'13px' }}>{s.seatNumber}</div>
                      <div style={{ color: SEAT_COLORS[s.type]?.text || '#888', fontSize:'10px' }}>{s.type}</div>
                    </div>
                    <div style={{ color:'#00d4ff', fontWeight:'800', fontSize:'13px' }}>₹{s.price}</div>
                  </div>
                ))}
              </div>

              {/* Total */}
              <div style={{ borderTop:'1px solid #ffffff11', paddingTop:'12px', marginBottom:'16px' }}>
                <div style={{ display:'flex', justifyContent:'space-between', color:'#667799', fontSize:'12px', marginBottom:'4px' }}>
                  <span>Subtotal ({selected.length} seats)</span>
                  <span>₹{totalPrice}</span>
                </div>
                <div style={{ display:'flex', justifyContent:'space-between', color:'#fff', fontSize:'16px', fontWeight:'900' }}>
                  <span>Total</span>
                  <span style={{ color:'#00d4ff' }}>₹{totalPrice}</span>
                </div>
                <div style={{ color:'#667799', fontSize:'11px', marginTop:'4px' }}>
                  + GST will be calculated at payment
                </div>
              </div>

              {/* Proceed button */}
              <button onClick={handleProceed} disabled={booking} style={{
                width:'100%', padding:'14px', borderRadius:'12px', border:'none',
                background: booking ? '#1a3a3a' : 'linear-gradient(135deg,#00d4ff,#0066ff)',
                color:'#fff', fontWeight:'900', fontSize:'15px',
                cursor: booking ? 'not-allowed' : 'pointer',
                boxShadow: booking ? 'none' : '0 4px 20px #00d4ff44',
                transition:'all 0.2s',
              }}>
                {booking ? '⏳ Booking…' : `💳 Pay ₹${totalPrice}`}
              </button>

              <button onClick={() => { setSelected([]); setError(''); }} style={{
                width:'100%', padding:'10px', marginTop:'8px', borderRadius:'10px',
                background:'transparent', border:'1px solid #ffffff22',
                color:'#667799', fontSize:'13px', cursor:'pointer',
              }}>
                Clear Selection
              </button>
            </>
          )}
        </div>
      </div>

      <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>
    </div>
  );
}

// ── SeatButton sub-component ──────────────────────────────────────────────
/**
 * SeatButton — renders one seat tile.
 * Uses seat.isBooked (not seat.booked) and seat.type (not seat.seatType).
 */
function SeatButton({ seat, colors, isSelected, onClick }) {
  const bg =
    seat.isBooked ? colors.booked  :
    isSelected    ? '#00ff88'      :
                    colors.avail;

  const border =
    seat.isBooked ? '1px solid rgba(255,255,255,0.04)' :
    isSelected    ? '2px solid #00ff88'                :
                    '1px solid rgba(255,255,255,0.10)';

  return (
    <div
      onClick={onClick}
      title={`${seat.seatNumber} · ${seat.type} · ₹${seat.price}${seat.isBooked ? ' · BOOKED' : ''}`}
      style={{
        width: '28px', height: '28px',
        borderRadius: '5px 5px 3px 3px',
        background: bg,
        border,
        cursor: seat.isBooked ? 'not-allowed' : 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: '9px', color: 'rgba(255,255,255,0.6)',
        transition: 'transform 0.1s',
        transform: isSelected ? 'scale(1.15)' : 'scale(1)',
        opacity: seat.isBooked ? 0.4 : 1,
        userSelect: 'none',
      }}
    >
      {isSelected ? '✓' : ''}
    </div>
  );
}

const spinnerStyle = {
  width: '36px', height: '36px', margin: '0 auto',
  border: '3px solid #ffffff11', borderTop: '3px solid #00d4ff',
  borderRadius: '50%', animation: 'spin 0.8s linear infinite',
};
