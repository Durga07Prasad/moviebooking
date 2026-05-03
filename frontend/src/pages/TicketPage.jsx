import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/axios';

const TYPE_STYLE = {
  VIP:     { color:'#ffd700', bg:'linear-gradient(135deg,#2d1a00,#4d3000)', border:'#ffd70044', label:'👑 VIP' },
  PREMIUM: { color:'#aa44ff', bg:'linear-gradient(135deg,#1a0a2e,#2d0a4e)', border:'#aa44ff44', label:'⭐ PREMIUM' },
  REGULAR: { color:'#00d4ff', bg:'linear-gradient(135deg,#001a2e,#002a4e)', border:'#00d4ff44', label:'🎟️ REGULAR' },
};

export default function TicketPage() {
  const { ticketId } = useParams();
  const navigate     = useNavigate();
  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => { fetchTicket(); }, [ticketId]);

  const fetchTicket = async () => {
    try {
      const res = await api.get(`/api/tickets/${ticketId}`);
      setTicket(res.data.data);
    } catch { console.error('Ticket not found'); }
    finally { setLoading(false); }
  };

  if (loading) return (
    <div style={{ minHeight:'100vh', background:'#07070e', display:'flex', alignItems:'center', justifyContent:'center' }}>
      <div style={spinner}/>
      <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>
    </div>
  );
  if (!ticket) return (
    <div style={{ minHeight:'100vh', background:'#07070e', display:'flex', alignItems:'center', justifyContent:'center', color:'#ff4466' }}>
      Ticket not found.
    </div>
  );

  const codePrefix = ticket.ticketCode?.split('-')[0] || 'REG';
  const ts = TYPE_STYLE[codePrefix] || TYPE_STYLE.REGULAR;

  return (
    <div style={{ minHeight:'100vh', background:'#07070e', fontFamily:"'Segoe UI',sans-serif",
      display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', padding:'24px' }}>

      {/* Ticket card */}
      <div style={{
        width:'min(460px,100%)',
        background: ts.bg,
        border: `2px solid ${ts.border}`,
        borderRadius:'20px',
        boxShadow: `0 0 60px ${ts.color}22`,
        overflow:'hidden',
        animation:'fadeUp 0.5s ease',
      }}>

        {/* Header */}
        <div style={{
          background: `linear-gradient(135deg, ${ts.color}22, ${ts.color}11)`,
          padding:'20px 24px', textAlign:'center',
          borderBottom:`2px dashed ${ts.border}`,
        }}>
          <div style={{ fontSize:'36px', marginBottom:'6px' }}>🎬</div>
          <h1 style={{ color:ts.color, margin:'0 0 4px', fontSize:'22px', fontWeight:'900' }}>
            CineBook Ticket
          </h1>
          <div style={{
            display:'inline-block', background: `${ts.color}22`, border:`1px solid ${ts.border}`,
            color:ts.color, padding:'4px 16px', borderRadius:'20px', fontSize:'13px', fontWeight:'800',
            letterSpacing:'1px',
          }}>
            {ts.label}
          </div>
        </div>

        {/* Movie + Theatre */}
        <div style={{ padding:'20px 24px', borderBottom:`1px dashed ${ts.border}` }}>
          <h2 style={{ color:'#fff', margin:'0 0 6px', fontSize:'20px', fontWeight:'900' }}>
            {ticket.movieTitle}
          </h2>
          <p style={{ color:'#8899aa', margin:'0 0 3px', fontSize:'13px' }}>
            🎪 {ticket.theatreName}
          </p>
          <div style={{ display:'flex', gap:'16px', marginTop:'12px' }}>
            <div>
              <div style={{ color:'#667799', fontSize:'11px', textTransform:'uppercase', letterSpacing:'1px' }}>Date</div>
              <div style={{ color:'#fff', fontWeight:'700' }}>{ticket.showDate}</div>
            </div>
            <div>
              <div style={{ color:'#667799', fontSize:'11px', textTransform:'uppercase', letterSpacing:'1px' }}>Time</div>
              <div style={{ color:'#fff', fontWeight:'700' }}>{ticket.showTime}</div>
            </div>
            <div>
              <div style={{ color:'#667799', fontSize:'11px', textTransform:'uppercase', letterSpacing:'1px' }}>Seats</div>
              <div style={{ color:ts.color, fontWeight:'900' }}>{ticket.seatNumbers?.join(', ')}</div>
            </div>
          </div>
        </div>

        {/* Torn edge divider */}
        <div style={{
          height:'20px', position:'relative',
          backgroundImage: `radial-gradient(circle at 0 50%, transparent 12px, ${ts.color}11 0) left,
                            radial-gradient(circle at 100% 50%, transparent 12px, ${ts.color}11 0) right`,
          backgroundSize:'24px 20px', backgroundRepeat:'repeat-x',
          borderTop: `2px dashed ${ts.border}aa`,
        }}/>

        {/* Ticket Code */}
        <div style={{ padding:'16px 24px', textAlign:'center', borderBottom:`1px dashed ${ts.border}` }}>
          <div style={{ color:'#667799', fontSize:'11px', marginBottom:'6px', letterSpacing:'1px' }}>TICKET CODE</div>
          <div style={{
            fontFamily:'monospace', fontSize:'22px', fontWeight:'900',
            color: ts.color, letterSpacing:'3px',
            textShadow: `0 0 20px ${ts.color}66`,
          }}>
            {ticket.ticketCode}
          </div>
        </div>

        {/* QR + Amount */}
        <div style={{ padding:'16px 24px', display:'flex', alignItems:'center', justifyContent:'space-between' }}>
          {/* QR Placeholder */}
          <div style={{
            width:'80px', height:'80px', background:'rgba(255,255,255,0.05)',
            border:`1px solid ${ts.border}`, borderRadius:'10px',
            display:'flex', alignItems:'center', justifyContent:'center',
            flexDirection:'column', gap:'4px',
          }}>
            <div style={{ fontSize:'24px' }}>▣</div>
            <div style={{ color:'#667799', fontSize:'9px' }}>QR CODE</div>
          </div>

          <div style={{ textAlign:'right' }}>
            <div style={{ color:'#667799', fontSize:'12px' }}>Holder</div>
            <div style={{ color:'#fff', fontWeight:'700', marginBottom:'8px' }}>{ticket.userName || 'Guest'}</div>
            <div style={{ color:'#667799', fontSize:'12px' }}>Amount Paid</div>
            <div style={{ color:ts.color, fontSize:'22px', fontWeight:'900' }}>₹{ticket.totalAmount}</div>
          </div>
        </div>

        {/* Status */}
        <div style={{
          padding:'10px 24px', textAlign:'center',
          background: ticket.status === 'VALID' ? '#00ff8811' : '#ff446611',
          borderTop: `1px solid ${ts.border}`,
        }}>
          <span style={{ color: ticket.status==='VALID' ? '#00ff88' : '#ff4466', fontSize:'13px', fontWeight:'800' }}>
            {ticket.status === 'VALID' ? '✅ VALID TICKET' : '❌ ' + ticket.status}
          </span>
        </div>
      </div>

      {/* Actions */}
      <div style={{ display:'flex', gap:'12px', marginTop:'20px' }}>
        <button
          onClick={() => alert('Ticket saved! (In production this would download PDF)')}
          style={{
            padding:'12px 24px', borderRadius:'12px', border:'none',
            background: `linear-gradient(135deg, ${ts.color}44, ${ts.color}22)`,
            border: `1px solid ${ts.border}`,
            color: ts.color, fontWeight:'800', fontSize:'14px', cursor:'pointer',
          }}>
          ⬇️ Download Ticket
        </button>
        <button
          onClick={() => navigate('/my-bookings')}
          style={{
            padding:'12px 24px', borderRadius:'12px',
            background:'transparent', border:'1px solid #ffffff22',
            color:'#667799', fontWeight:'600', fontSize:'14px', cursor:'pointer',
          }}>
          📋 My Bookings
        </button>
        <button
          onClick={() => navigate('/movies')}
          style={{
            padding:'12px 24px', borderRadius:'12px',
            background:'rgba(0,212,255,0.08)', border:'1px solid #00d4ff44',
            color:'#00d4ff', fontWeight:'700', fontSize:'14px', cursor:'pointer',
          }}>
          🎬 Book More
        </button>
      </div>

      <style>{`
        @keyframes fadeUp{from{opacity:0;transform:translateY(20px)}to{opacity:1;transform:translateY(0)}}
        @keyframes spin{to{transform:rotate(360deg)}}
      `}</style>
    </div>
  );
}

const spinner = {
  width:'40px', height:'40px',
  border:'3px solid #ffffff11', borderTop:'3px solid #00d4ff',
  borderRadius:'50%', animation:'spin 0.8s linear infinite',
};
