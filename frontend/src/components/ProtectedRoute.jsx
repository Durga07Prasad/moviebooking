import { Navigate } from 'react-router-dom';

// ProtectedRoute — guards pages that require authentication
// OOAD: Demonstrates Single Responsibility Principle
const ProtectedRoute = ({ children, requiredRole }) => {
  const token = localStorage.getItem('token');
  const role = localStorage.getItem('role');

  if (!token) return <Navigate to="/" replace />;
  if (requiredRole && role !== requiredRole) return <Navigate to="/" replace />;

  return children;
};

export default ProtectedRoute;
