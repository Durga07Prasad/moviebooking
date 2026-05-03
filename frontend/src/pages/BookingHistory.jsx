import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api/axios';
import Navbar from '../components/Navbar';
import Toast from '../components/Toast';

/**
 * BookingHistory — User's complete booking list with inline cancel confirm + ticket view.
 *
 * KEY FIX: Removed window.confirm() — replaced with inline confirmation widget
 * so the cancel flow is fully React-controlled (no native browser dialogs).
 *
 * OOAD: GRASP Controller — orchestrates booking list, cancel, ticket navigation.
 * SOLID: S — only My Bookings concerns. No payment or seat logic here.
 */
const BookingHistory = () => {
  const [bookings,    setBookings]    = useState([]);
  const [loading,     setLoading]     = useState(true);
  const [cancelling,  setCancelling]  = useState(null);  // bookingId being cancelled
  const [confirmId,   setConfirmId]   = useState(null);  // bookingId awaiting confirm
  const [cancelResult, setCancelResult] = useState(null); // result from API
  const [toast,       setToast]       = useState(null);
  const navigate = useNavigate();

  useEffect(() => { fetchBookings(); }, []);

  const fetchBookings = async () => {
    setLoading(true);
    try {
      const res = await api.get('/api/bookings/my');
      setBookings(res.data.data || []);
    } catch {
      setToast({ message: 'Failed to load bookings', type: 'error' });
    } finally { setLoading(false); }
  };

  // Helper — reads bookingStatus (model field) with fallback
  const getStatus = (b) => b.bookingStatus || b.status || 'PENDING';

  const statusColor = (s) =>
    s === 'CONFIRMED' ? '#00ff88' : s === 'CANCELLED' ? '#ff4466' : '#ffaa00';

  const statusIcon = (s) =>
    s === 'CONFIRMED' ? '✅' : s === 'CANCELLED' ? '❌' : '⏳';

  // Step 1: show inline confirm panel
  const requestCancel = (bookingId, isPending) => {
    setCancelResult(null);
    setConfirmId({ id: bookingId, isPending });
  };

  // Step 2: user confirmed → call API
  const confirmCancel = async (bookingId) => {
    setCancelling(bookingId);
    try {
      const res = await api.post(`/api/bookings/${bookingId}/cancel`);
      const result = res.data.data;
      setCancelResult({ bookingId, ...result });
      setToast({ message: result.message || '✅ Booking cancelled.', type: 'success' });
      fetchBookings();
    } catch (err) {
      const msg = err.response?.data?.message || 'Cancel failed. Please try again.';
      setToast({ message: '❌ ' + msg, type: 'error' });
    } finally {
      setCancelling(null);
      setConfirmId(null);
    }
  };

  const viewTicket = async (bookingId) => {
    try {
      const res = await api.get(`/api/bookings/${bookingId}/ticket`);
      if (res.data.data?.id) navigate(`/ticket/${res.data.data.id}`);
    } catch {
      setToast({ message: 'Ticket not found for this booking', type: 'error' });
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: '#07070e', fontFamily: "'Segoe UI',sans-serif" }}>
      <Navbar theme="user" />

      <div style={{ maxWidth: '900px', margin: '0 auto', padding: '32px 24px' }}>

        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '28px' }}>
          <div>
            <h1 style={{ color: '#fff', fontSize: '26px', fontWeight: '900', margin: 0 }}>📋 My Bookings</h1>
            <p style={{ color: '#667799', fontSize: '14px', margin: '4px 0 0' }}>Your complete booking history</p>
          </div>
          <button onClick={fetchBookings} style={{
            background: 'rgba(0,212,255,0.08)', border: '1px solid #00d4ff44',
            color: '#00d4ff', padding: '8px 18px', borderRadius: '10px',
            cursor: 'pointer', fontSize: '13px', fontWeight: '600',
          }}>🔄 Refresh</button>
        </div>

        {cancelResult && (
          <div style={{
            background: cancelResult.paymentWasMade
              ? (cancelResult.refundEligible ? 'rgba(0,255,136,0.08)' : 'rgba(255,170,0,0.08)')
              : 'rgba(0,212,255,0.08)',
            border: `1px solid ${
              cancelResult.paymentWasMade
                ? (cancelResult.refundEligible ? '#00ff8844' : '#ffaa0044')
                : '#00d4ff44'}`,
            borderRadius: '14px', padding: '16px 20px', marginBottom: '20px',
          }}>
            <div style={{
              color: cancelResult.paymentWasMade
                ? (cancelResult.refundEligible ? '#00ff88' : '#ffaa00')
                : '#00d4ff',
              fontWeight: '800', marginBottom: '6px',
            }}>
              {cancelResult.paymentWasMade
                ? (cancelResult.refundEligible ? '💸 Refund Initiated!' : '✅ Booking Cancelled')
                : '✅ Booking Voided'}
            </div>
            <div style={{ color: '#ccc', fontSize: '13px' }}>{cancelResult.message}</div>
            {cancelResult.paymentWasMade && cancelResult.refundEligible && (
              <div style={{ color: '#00ff88', fontSize: '18px', fontWeight: '900', marginTop: '6px' }}>
                ₹{cancelResult.refundAmount} will be credited in 5–7 business days
              </div>
            )}
            {cancelResult.paymentWasMade && (
              <div style={{ color: '#445566', fontSize: '11px', marginTop: '4px' }}>{cancelResult.gstNote}</div>
            )}
            <button onClick={() => setCancelResult(null)}
              style={{ marginTop: '10px', background: 'transparent', border: '1px solid #ffffff22', color: '#667799', padding: '4px 14px', borderRadius: '8px', cursor: 'pointer', fontSize: '12px' }}>
              Dismiss
            </button>
          </div>
        )}

        {/* Loading */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: '80px 0', color: '#667799' }}>
            <div style={spinnerStyle} />
            <p style={{ marginTop: '16px' }}>Loading your bookings...</p>
          </div>
        ) : bookings.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '80px 0', background: 'rgba(255,255,255,0.02)', borderRadius: '20px', border: '1px solid #ffffff11' }}>
            <div style={{ fontSize: '64px', marginBottom: '16px' }}>🎭</div>
            <h3 style={{ color: '#fff', marginBottom: '8px' }}>No bookings yet</h3>
            <p style={{ color: '#667799', marginBottom: '24px' }}>Book your first movie ticket!</p>
            <Link to="/movies" style={{
              padding: '12px 28px', background: 'linear-gradient(135deg, #00d4ff, #0066ff)',
              borderRadius: '12px', color: '#fff', textDecoration: 'none',
              fontWeight: '700', boxShadow: '0 0 20px #00d4ff44',
            }}>Browse Movies →</Link>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {bookings.map(booking => {
              const status      = getStatus(booking);
              const isConfirming = confirmId?.id === booking.id;
              const isCancelling = cancelling === booking.id;

              return (
                <div key={booking.id} style={{
                  background: 'rgba(255,255,255,0.03)',
                  border: `1px solid ${status === 'CONFIRMED' ? '#00d4ff22' : status === 'CANCELLED' ? '#ff446622' : '#ffaa0022'}`,
                  borderRadius: '16px', padding: '20px 24px', position: 'relative',
                  transition: 'transform 0.2s',
                }}>

                  {/* Status badge */}
                  <div style={{
                    position: 'absolute', top: '16px', right: '16px',
                    background: `${statusColor(status)}15`,
                    border: `1px solid ${statusColor(status)}44`,
                    color: statusColor(status),
                    fontSize: '12px', fontWeight: '800', padding: '4px 12px',
                    borderRadius: '20px', letterSpacing: '1px',
                  }}>
                    {statusIcon(status)} {status}
                  </div>

                  {/* Booking details grid */}
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '12px', marginBottom: '16px' }}>
                    <div>
                      <div style={labelStyle}>🎫 Booking ID</div>
                      <div style={{ color: '#fff', fontFamily: 'monospace', fontSize: '13px', fontWeight: '700' }}>
                        {booking.id?.slice(-10).toUpperCase()}
                      </div>
                    </div>
                    <div>
                      <div style={labelStyle}>💺 Seats</div>
                      <div style={{ color: '#ffdd00', fontWeight: '700', fontSize: '14px' }}>
                        {booking.seatNumbers?.join(', ') || 'N/A'}
                      </div>
                    </div>
                    <div>
                      <div style={labelStyle}>💰 Amount</div>
                      <div style={{ color: '#00d4ff', fontWeight: '900', fontSize: '18px' }}>₹{booking.totalAmount}</div>
                    </div>
                    <div>
                      <div style={labelStyle}>🎭 Count</div>
                      <div style={{ color: '#fff', fontWeight: '700' }}>{booking.numberOfSeats} seat(s)</div>
                    </div>
                    <div>
                      <div style={labelStyle}>📅 Booked On</div>
                      <div style={{ color: '#fff', fontSize: '13px' }}>
                        {booking.bookingTime
                          ? new Date(booking.bookingTime).toLocaleDateString('en-IN', {
                              day: '2-digit', month: 'short', year: 'numeric',
                              hour: '2-digit', minute: '2-digit'
                            })
                          : 'N/A'}
                      </div>
                    </div>
                    <div>
                      <div style={labelStyle}>💳 Payment</div>
                      <div style={{ color: '#fff', fontSize: '13px' }}>
                        {status === 'CONFIRMED' ? '✅ Paid' : status === 'CANCELLED' ? '❌ Cancelled' : '⏳ Pending'}
                      </div>
                    </div>
                  </div>

                  {/* ── Action buttons (normal state) ── */}
                  {!isConfirming && (
                    <div style={{ display: 'flex', gap: '10px', borderTop: '1px solid #ffffff0a', paddingTop: '14px', flexWrap: 'wrap' }}>
                      {status === 'CONFIRMED' && (
                        <button onClick={() => viewTicket(booking.id)} style={btnStyle('#00d4ff', '#0066ff')}>
                          🎟️ View Ticket
                        </button>
                      )}
                      {status === 'PENDING' && (
                        <button onClick={() => navigate(`/payment/${booking.id}`)} style={btnStyle('#ffaa00', '#ff6600')}>
                          ⚡ Complete Payment
                        </button>
                      )}
                      {/* PENDING = no payment yet → "Void Booking" (amber, no refund) */}
                      {status === 'PENDING' && (
                        <button onClick={() => requestCancel(booking.id, true)} style={{
                          padding: '8px 20px', background: 'transparent',
                          border: '1px solid #ffaa0066', borderRadius: '8px',
                          color: '#ffaa00', cursor: 'pointer', fontSize: '13px', fontWeight: '600',
                          transition: 'all 0.2s',
                        }}>
                          🗑️ Void Booking
                        </button>
                      )}
                      {/* CONFIRMED = payment made → "Cancel & Refund" (red) */}
                      {status === 'CONFIRMED' && (
                        <button onClick={() => requestCancel(booking.id, false)} style={{
                          padding: '8px 20px', background: 'transparent',
                          border: '1px solid #ff446666', borderRadius: '8px',
                          color: '#ff4466', cursor: 'pointer', fontSize: '13px', fontWeight: '600',
                          transition: 'all 0.2s',
                        }}>
                          ✕ Cancel & Refund
                        </button>
                      )}
                    </div>
                  )}

                  {/* ── Inline confirm panel ── */}
                  {isConfirming && (
                    <div style={{
                      borderTop: `1px solid ${confirmId?.isPending ? '#ffaa0022' : '#ff446622'}`,
                      paddingTop: '14px', marginTop: '4px',
                      background: confirmId?.isPending ? 'rgba(255,170,0,0.06)' : 'rgba(255,68,102,0.06)',
                      borderRadius: '0 0 12px 12px', padding: '14px 16px',
                    }}>
                      <p style={{ color: confirmId?.isPending ? '#ffcc66' : '#ff8899', fontSize: '14px', fontWeight: '700', margin: '0 0 4px' }}>
                        {confirmId?.isPending ? '🗑️ Void this booking?' : '⚠️ Cancel & request refund?'}
                      </p>
                      <p style={{ color: '#667799', fontSize: '12px', margin: '0 0 12px' }}>
                        {confirmId?.isPending
                          ? 'No payment was made. Seats will be released. No refund needed.'
                          : 'Refund policy: 50% base price if >2h before show. GST is non-refundable.'}
                      </p>
                      <div style={{ display: 'flex', gap: '10px' }}>
                        <button
                          onClick={() => confirmCancel(booking.id)}
                          disabled={isCancelling}
                          style={{
                            padding: '8px 24px',
                            background: isCancelling ? '#1a1a1a' : (confirmId?.isPending ? '#cc7700' : '#ff2244'),
                            border: 'none', borderRadius: '8px', color: '#fff',
                            cursor: isCancelling ? 'not-allowed' : 'pointer',
                            fontSize: '13px', fontWeight: '700', transition: 'all 0.2s',
                          }}>
                          {isCancelling ? '⏳ Processing...' : (confirmId?.isPending ? 'Yes, Void' : 'Yes, Cancel')}
                        </button>
                        <button
                          onClick={() => setConfirmId(null)}
                          disabled={isCancelling}
                          style={{
                            padding: '8px 20px', background: 'transparent',
                            border: '1px solid #ffffff22', borderRadius: '8px',
                            color: '#667799', cursor: 'pointer', fontSize: '13px',
                          }}>
                          Keep Booking
                        </button>
                      </div>
                    </div>
                  )}

                </div>
              );
            })}
          </div>
        )}
      </div>

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
};

// ── Styles ────────────────────────────────────────────────────────────────────
const labelStyle = {
  color: '#667799', fontSize: '11px', textTransform: 'uppercase',
  letterSpacing: '1px', marginBottom: '4px',
};

const btnStyle = (c1, c2) => ({
  padding: '8px 20px',
  background: `linear-gradient(135deg, ${c1}, ${c2})`,
  border: 'none', borderRadius: '8px', color: '#fff',
  cursor: 'pointer', fontSize: '13px', fontWeight: '700',
  boxShadow: `0 0 15px ${c1}33`,
});

const spinnerStyle = {
  width: '36px', height: '36px', margin: '0 auto',
  border: '3px solid #ffffff11', borderTop: '3px solid #00d4ff',
  borderRadius: '50%', animation: 'spin 0.8s linear infinite',
};

export default BookingHistory;
