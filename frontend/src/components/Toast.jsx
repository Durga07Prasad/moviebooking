import { useEffect } from 'react';

const Toast = ({ message, type = 'success', onClose }) => {
  useEffect(() => {
    const timer = setTimeout(onClose, 3500);
    return () => clearTimeout(timer);
  }, [onClose]);

  const colors = {
    success: { bg: '#00d4ff22', border: '#00d4ff', text: '#00d4ff' },
    error:   { bg: '#ff003322', border: '#ff0033', text: '#ff4466' },
    info:    { bg: '#8b00ff22', border: '#8b00ff', text: '#cc66ff' },
  };
  const c = colors[type] || colors.success;

  return (
    <div style={{
      position: 'fixed', bottom: '30px', right: '30px', zIndex: 9999,
      background: c.bg, border: `1px solid ${c.border}`,
      color: c.text, padding: '14px 22px', borderRadius: '12px',
      fontSize: '14px', fontWeight: '600', backdropFilter: 'blur(10px)',
      boxShadow: `0 0 20px ${c.border}55`,
      animation: 'slideIn 0.3s ease',
      maxWidth: '320px', display: 'flex', alignItems: 'center', gap: '10px',
    }}>
      <span>{type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️'}</span>
      <span>{message}</span>
      <button onClick={onClose} style={{
        background: 'none', border: 'none', color: c.text,
        cursor: 'pointer', fontSize: '16px', marginLeft: 'auto',
      }}>×</button>
      <style>{`@keyframes slideIn { from { transform: translateX(100px); opacity:0; } to { transform: translateX(0); opacity:1; } }`}</style>
    </div>
  );
};

export default Toast;
