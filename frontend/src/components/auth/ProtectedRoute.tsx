import React from 'react';
import { useAuth } from '../../hooks/useAuth';
import AuthModal from './AuthModal';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requireAuth?: boolean;
  requiredRoles?: string[];
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ 
  children, 
  requireAuth = true,
  requiredRoles = []
}) => {
  const { isAuthenticated, hasAnyRole } = useAuth();
  const [showAuthModal, setShowAuthModal] = React.useState(false);

  React.useEffect(() => {
    if (requireAuth && !isAuthenticated) {
      setShowAuthModal(true);
    }
  }, [requireAuth, isAuthenticated]);

  if (requireAuth && !isAuthenticated) {
    return (
      <div className="container mt-5">
        <div className="row justify-content-center">
          <div className="col-md-6">
            <div className="card">
              <div className="card-body text-center">
                <h3 className="card-title">Authentication Required</h3>
                <p className="card-text">
                  You need to be logged in to access this page.
                </p>
                <button 
                  className="btn btn-primary"
                  onClick={() => setShowAuthModal(true)}
                >
                  Sign In
                </button>
              </div>
            </div>
          </div>
        </div>

        <AuthModal
          isOpen={showAuthModal}
          onClose={() => setShowAuthModal(false)}
          initialMode="login"
        />
      </div>
    );
  }

  if (requiredRoles.length > 0 && !hasAnyRole(requiredRoles)) {
    return (
      <div className="container mt-5">
        <div className="row justify-content-center">
          <div className="col-md-6">
            <div className="card">
              <div className="card-body text-center">
                <h3 className="card-title">Access Denied</h3>
                <p className="card-text">
                  You don't have permission to access this page.
                </p>
                <p className="text-muted">
                  Required roles: {requiredRoles.join(', ')}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return <>{children}</>;
};

export default ProtectedRoute;