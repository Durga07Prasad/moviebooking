import React, { useState, useEffect } from 'react';
import axios from 'axios';

/**
 * CancelBookingModal — cancellation confirmation overlay with refund preview.
 *
 * Props:
 *   booking   — Booking object { id, seatNumbers, totalAmount, bookingStatus, ... }
 *   show      — Show object { showDate, showTime, price, ... }
 *   payment   — Payment object { baseAmount, gstAmount, totalAmount, ... } (may be null)
 *   onCancelled — callback after successful api cancel
 *   onClose     — callback to close modal without cancelling
 *
 * OOAD: GRASP Low Coupling — modal receives data as props, never fetches itself.
 * SOLID: S — only one responsibility: cancellation UX flow.
 *
 * States:
 *   idle      → refund preview + confirm/close buttons
 *   loading   → spinner while API call runs
 *   result    → success screen with refund amount or no-refund message
 */

// ─── Client-side IST refund preview (mirrors RefundService.java) ───────────

function nowIST() {
  return new Date(new Date().toLocaleString('en-US', { timeZone: 'Asia/Kolkata' }));
}

/**
 * Mirrors RefundService.calculate() exactly.
 * Used for client-side PREVIEW before calling the backend.
 * SOLID: O — if refund policy changes, update RefundService.java AND here.
 */
function getRefundPreview(basePaid, showDate, showTime) {
  try {
    const istNow  = nowIST();
    const [y, m, d] = showDate.split('-').map(Number);
    const [h, min]  = (showTime || '00:00').substring(0, 5).split(':').map(Number);
    const showDT  = new Date(y, m - 1, d, h, min, 0);
    const hours   = (showDT - istNow) / 3600000;

    if (hours < 0) {
      return { eligible: false, amount: 0,
        msg: 'Show has already started. No refund.' };
    }
    if (hours < 2) {
      const mins = Math.floor(hours * 60);
      return { eligible: false, amount: 0,
        msg: `Cancelled within 2 hours of show (${mins} min left). No refund.` };
    }
    const refund = Math.round(basePaid * 0.5 * 100) / 100;
    return {
      eligible: true,
      amount: refund,
      msg: `50% refund of ₹${basePaid.toFixed(2)} = ₹${refund.toFixed(2)} approved.`,
    };
  } catch {
    return { eligible: false, amount: 0, msg: 'Unable to calculate refund.' };
  }
}

function fmt12(t) {
  try {
    const [h, m] = (t || '00:00').substring(0, 5).split(':').map(Number);
    return `${h % 12 || 12}:${String(m).padStart(2,'0')} ${h >= 12 ? 'PM' : 'AM'}`;
  } catch { return t; }
}

// ─── Component ───────────────────────────────────────────────────────────────
export default function CancelBookingModal({ booking, show, payment, onCancelled, onClose }) {
  const [phase,   setPhase]   = useState('idle');    // idle | loading | result
  const [result,  setResult]  = useState(null);
  const [apiErr,  setApiErr]  = useState('');

  // Compute refund preview from client-side logic
  const basePaid = payment?.baseAmount || booking?.totalAmount || 0;
  const preview  = show
    ? getRefundPreview(basePaid, String(show.showDate), String(show.showTime))
    : { eligible: false, amount: 0, msg: 'Show data unavailable.' };

  const gstPaid  = payment?.gstAmount || 0;

  // ── Confirm Cancel ─────────────────────────────────────────────────────
  async function handleConfirm() {
    setPhase('loading');
    setApiErr('');
    try {
      const token = localStorage.getItem('token');
      const res = await axios.put(`/api/bookings/${booking.id}/cancel`, {}, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = res.data.data || res.data;
      setResult(data);
      setPhase('result');
      if (onCancelled) onCancelled(data);
    } catch (e) {
      const msg = e.response?.data?.message || 'Cancellation failed. Please try again.';
      setApiErr(msg);
      setPhase('idle');
    }
  }

  // ── Styles ───────────────────────────────────────────────────────────────
  const S = {
    overlay: {
      position: 'fixed', inset: 0, zIndex: 1000,
      background: 'rgba(0,0,0,0.8)', backdropFilter: 'blur(4px)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      padding: '16px',
    },
    modal: {
      background: '#0d0d1a', borderRadius: '20px',
      border: '1px solid #1a1a2e', width: '100%', maxWidth: '480px',
      overflow: 'hidden', fontFamily: "'Inter','Segoe UI',sans-serif",
      boxShadow: '0 25px 80px rgba(0,0,0,0.6)',
    },
    header: {
      padding: '20px 24px 16px',
      borderBottom: '1px solid #1a1a2e',
      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    },
    title:  { fontSize: '18px', fontWeight: 700, color: '#fff' },
    close:  { background: 'none', border: 'none', cursor: 'pointer',
               color: '#667799', fontSize: '20px' },
    body:   { padding: '20px 24px' },
    table:  { width: '100%', borderCollapse: 'collapse', marginBottom: '18px' },
    tr:     { borderBottom: '1px solid #1a1a2e' },
    tdL:    { padding: '8px 0', color: '#667799', fontSize: '13px', width: '50%' },
    tdR:    { padding: '8px 0', color: '#cde', fontSize: '13px',
               textAlign: 'right', fontWeight: 500 },
    refundBox: (eligible) => ({
      borderRadius: '12px', padding: '14px 18px',
      background: eligible ? '#001a12' : '#1a0000',
      border: `1px solid ${eligible ? '#00c47744' : '#ff444444'}`,
      marginBottom: '14px',
    }),
    refundTitle: (eligible) => ({
      fontWeight: 700, fontSize: '14px', marginBottom: '6px',
      color: eligible ? '#00e676' : '#ff6666',
    }),
    refundAmt: {
      fontSize: '28px', fontWeight: 800, color: '#00e676',
      marginBottom: '4px',
    },
    gstNote: {
      fontSize: '12px', color: '#667799', marginTop: '8px',
      padding: '8px 12px', background: '#0a0a1a',
      borderRadius: '8px', borderLeft: '3px solid #667799',
    },
    btnRow: { display: 'flex', gap: '12px', marginTop: '20px' },
    btnKeep: {
      flex: 1, padding: '12px', borderRadius: '12px', cursor: 'pointer',
      background: 'transparent', border: '1.5px solid #2a2a3e',
      color: '#8899aa', fontWeight: 600, fontSize: '15px',
      transition: 'all 0.2s',
    },
    btnCancel: {
      flex: 1, padding: '12px', borderRadius: '12px', cursor: 'pointer',
      background: preview.eligible ? '#ff334422' : '#ff000022',
      border: '1.5px solid #ff444488',
      color: '#ff6666', fontWeight: 700, fontSize: '15px',
      transition: 'all 0.2s',
    },
    spinner: {
      display: 'flex', justifyContent: 'center', padding: '32px',
      color: '#00d4ff', fontSize: '16px',
    },
    resultBox: { padding: '32px 24px', textAlign: 'center' },
    bigEmoji: { fontSize: '56px', marginBottom: '16px' },
    resultTitle: { fontSize: '22px', fontWeight: 700, color: '#fff', marginBottom: '8px' },
    resultSub: { color: '#667799', fontSize: '14px', marginBottom: '20px', lineHeight: 1.5 },
    creditNote: {
      background: '#001a12', border: '1px solid #00c47733',
      borderRadius: '12px', padding: '12px 16px',
      color: '#00c477', fontSize: '13px', marginBottom: '20px',
    },
    btnDone: {
      padding: '12px 40px', borderRadius: '12px', cursor: 'pointer',
      background: '#00d4ff', border: 'none', color: '#06060f',
      fontWeight: 700, fontSize: '15px',
    },
  };

  // ── Result screen ─────────────────────────────────────────────────────────
  if (phase === 'result') {
    const refundAmt = result?.refundAmount || 0;
    const eligible  = result?.refundEligible || refundAmt > 0;

    return (
      <div style={S.overlay}>
        <div style={S.modal}>
          <div style={S.resultBox}>
            <div style={S.bigEmoji}>{eligible ? '💸' : '✅'}</div>
            <div style={S.resultTitle}>
              {eligible ? 'Booking Cancelled & Refund Approved!' : 'Booking Cancelled'}
            </div>
            <div style={S.resultSub}>
              {result?.message || 'Your booking has been successfully cancelled.'}
            </div>

            {eligible && (
              <>
                <div style={{ fontSize: '13px', color: '#667799', marginBottom: '6px' }}>
                  Refund Amount
                </div>
                <div style={S.refundAmt}>₹{refundAmt.toFixed(2)}</div>
                <div style={S.creditNote}>
                  💳 Will be credited in 5–7 business days to your original payment method.
                  GST (₹{gstPaid.toFixed(2)}) is non-refundable per Indian tax regulations.
                </div>
              </>
            )}

            {!eligible && (
              <div style={{ ...S.gstNote, marginBottom: '20px', borderLeftColor: '#ff444488' }}>
                {result?.refundReason || 'No refund applicable as per cancellation policy.'}
              </div>
            )}

            <button style={S.btnDone} onClick={() => { if (onClose) onClose(); }}>
              Done
            </button>
          </div>
        </div>
      </div>
    );
  }

  // ── Loading spinner ───────────────────────────────────────────────────────
  if (phase === 'loading') {
    return (
      <div style={S.overlay}>
        <div style={S.modal}>
          <div style={S.spinner}>
            <span>Cancelling booking…</span>
          </div>
        </div>
      </div>
    );
  }

  // ── Idle — main confirmation screen ──────────────────────────────────────
  return (
    <div style={S.overlay} onClick={(e) => { if (e.target === e.currentTarget) onClose?.(); }}>
      <div style={S.modal}>

        {/* Header */}
        <div style={S.header}>
          <div style={S.title}>⚠️ Cancel Booking?</div>
          <button style={S.close} onClick={onClose}>✕</button>
        </div>

        <div style={S.body}>
          {/* Booking Summary Table */}
          <div style={{ marginBottom: '18px', color: '#8899aa', fontSize: '12px',
            textTransform: 'uppercase', letterSpacing: '0.5px' }}>
            Booking Summary
          </div>
          <table style={S.table}>
            <tbody>
              {show && (
                <>
                  <tr style={S.tr}>
                    <td style={S.tdL}>Show Date</td>
                    <td style={S.tdR}>{show.showDate}</td>
                  </tr>
                  <tr style={S.tr}>
                    <td style={S.tdL}>Show Time</td>
                    <td style={S.tdR}>{fmt12(String(show.showTime))}</td>
                  </tr>
                </>
              )}
              <tr style={S.tr}>
                <td style={S.tdL}>Seats</td>
                <td style={S.tdR}>{booking?.seatNumbers?.join(', ')}</td>
              </tr>
              <tr style={S.tr}>
                <td style={S.tdL}>Base Amount</td>
                <td style={S.tdR}>₹{basePaid.toFixed(2)}</td>
              </tr>
              {gstPaid > 0 && (
                <tr style={S.tr}>
                  <td style={S.tdL}>GST Paid</td>
                  <td style={S.tdR}>₹{gstPaid.toFixed(2)}</td>
                </tr>
              )}
              <tr>
                <td style={{ ...S.tdL, fontWeight: 600, color: '#cde' }}>Total Paid</td>
                <td style={{ ...S.tdR, fontWeight: 700, color: '#fff' }}>
                  ₹{(payment?.totalAmount || booking?.totalAmount || 0).toFixed(2)}
                </td>
              </tr>
            </tbody>
          </table>

          {/* Refund Preview Box */}
          <div style={S.refundBox(preview.eligible)}>
            <div style={S.refundTitle(preview.eligible)}>
              {preview.eligible ? '✅ Refund Eligible' : '❌ No Refund'}
            </div>

            {preview.eligible ? (
              <>
                <div style={{ color: '#667799', fontSize: '12px', marginBottom: '4px' }}>
                  You will receive
                </div>
                <div style={S.refundAmt}>₹{preview.amount.toFixed(2)}</div>
                <div style={{ color: '#8899aa', fontSize: '12px' }}>{preview.msg}</div>
              </>
            ) : (
              <div style={{ color: '#ff8888', fontSize: '13px', lineHeight: 1.5 }}>
                {preview.msg}
              </div>
            )}
          </div>

          {/* GST Note */}
          <div style={S.gstNote}>
            💡 GST (₹{gstPaid.toFixed(2)}) is non-refundable per Indian tax regulations (GST Act 2017).
            Only the base ticket price qualifies for refund.
          </div>

          {/* API Error */}
          {apiErr && (
            <div style={{ marginTop: '12px', padding: '10px 14px', borderRadius: '8px',
              background: '#1a0000', color: '#ff6666', fontSize: '13px',
              border: '1px solid #ff000033' }}>
              {apiErr}
            </div>
          )}

          {/* Action Buttons */}
          <div style={S.btnRow}>
            <button style={S.btnKeep} onClick={onClose}>
              Keep Booking
            </button>
            <button style={S.btnCancel} onClick={handleConfirm}>
              Yes, Cancel
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
