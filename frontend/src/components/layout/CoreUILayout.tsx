import React, { useState, useEffect } from 'react';
import {
  CSidebar,
  CSidebarNav,
  CNavItem,
  CNavTitle,
  CSidebarToggler,
  CContainer,
  CHeader,
  CHeaderNav,
  CHeaderToggler,
  CNavLink,
  CDropdown,
  CDropdownToggle,
  CDropdownMenu,
  CDropdownItem
} from '@coreui/react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faHome,
  faCalendarAlt,
  faClipboardCheck,
  faBars,
  faUser,
  faSignOutAlt,
  faSignInAlt,
  faUserPlus,
  faCog,
  faShield
} from '@fortawesome/free-solid-svg-icons';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import AuthModal from '../auth/AuthModal';

interface CoreUILayoutProps {
  children: React.ReactNode;
}

const CoreUILayout: React.FC<CoreUILayoutProps> = ({ children }) => {
  const [sidebarShow, setSidebarShow] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [authModalMode, setAuthModalMode] = useState<'login' | 'register'>('login');
  const location = useLocation();
  const { user, isAuthenticated, logout, isLoading } = useAuth();

  // Check if screen is mobile and set initial sidebar state
  useEffect(() => {
    const checkScreenSize = () => {
      const mobile = window.innerWidth < 768;
      setIsMobile(mobile);
      // On desktop, sidebar should always be visible
      // On mobile, sidebar should be hidden by default
      setSidebarShow(!mobile);
    };

    checkScreenSize();
    window.addEventListener('resize', checkScreenSize);
    return () => window.removeEventListener('resize', checkScreenSize);
  }, []);

  const handleLogin = () => {
    setAuthModalMode('login');
    setAuthModalOpen(true);
  };

  const handleRegister = () => {
    setAuthModalMode('register');
    setAuthModalOpen(true);
  };

  const handleLogout = async () => {
    try {
      await logout();
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  const navigation = [
    {
      component: CNavItem,
      name: 'Home',
      to: '/',
      icon: <FontAwesomeIcon icon={faHome} className="nav-icon" />,
    },
    {
      component: CNavItem,
      name: 'Events',
      to: '/events',
      icon: <FontAwesomeIcon icon={faCalendarAlt} className="nav-icon" />,
    },
    ...(isAuthenticated ? [
      {
        component: CNavItem,
        name: 'My Registrations',
        to: '/my-registrations',
        icon: <FontAwesomeIcon icon={faClipboardCheck} className="nav-icon" />,
      },
      {
        component: CNavTitle,
        name: 'Admin',
      },
      {
        component: CNavItem,
        name: 'Admin Dashboard',
        to: '/admin',
        icon: <FontAwesomeIcon icon={faShield} className="nav-icon" />,
      },
    ] : []),
  ];

  return (
    <div className="d-flex">
      {/* Backdrop for sidebar overlay - only on mobile */}
      {isMobile && sidebarShow && (
        <div 
          className="position-fixed w-100 h-100"
          style={{ 
            top: 0, 
            left: 0, 
            backgroundColor: 'rgba(0,0,0,0.5)', 
            zIndex: 999 
          }}
          onClick={() => setSidebarShow(false)}
        />
      )}
      
      <CSidebar
        visible={sidebarShow}
        onVisibleChange={(visible) => {
          // Only allow toggling on mobile
          if (isMobile) {
            setSidebarShow(visible);
          }
        }}
        className={`sidebar-dark ${!isMobile ? 'sidebar-fixed' : ''} ${sidebarShow ? 'show' : ''}`}
        colorScheme="dark"
      >
        <div className="sidebar-brand d-flex align-items-center justify-content-center py-3">
          <div className="sidebar-brand-full">
            <h4 className="text-white mb-0">EventR</h4>
            <small className="text-white-50">Enterprise</small>
          </div>
        </div>
        
        <CSidebarNav>
          {navigation.map((item, index) => {
            const { component: Component, name, icon, to, ...rest } = item;
            
            return (
              <Component
                key={index}
                {...rest}
              >
                {to ? (
                  <CNavLink
                    as={Link}
                    to={to}
                    className={location.pathname === to ? 'active' : ''}
                  >
                    {icon && <span className="me-2">{icon}</span>}
                    {name}
                  </CNavLink>
                ) : (
                  name
                )}
              </Component>
            );
          })}
        </CSidebarNav>
        
        {/* Only show sidebar toggler on mobile */}
        {isMobile && (
          <CSidebarToggler
            className="d-flex"
            onClick={() => setSidebarShow(!sidebarShow)}
          />
        )}
      </CSidebar>

      <div 
        className="wrapper d-flex flex-column min-vh-100"
        style={{
          marginLeft: !isMobile && sidebarShow ? '280px' : '0',
          transition: 'margin-left 0.15s ease-in-out',
          width: !isMobile && sidebarShow ? 'calc(100% - 280px)' : '100%'
        }}
      >
        <CHeader className="header header-sticky mb-4">
          {/* Only show header toggle button on mobile */}
          {isMobile && (
            <CHeaderToggler
              className="ps-1"
              onClick={() => setSidebarShow(!sidebarShow)}
            >
              <FontAwesomeIcon icon={faBars} size="lg" />
            </CHeaderToggler>
          )}
          
          <CHeaderNav className="ms-auto">
            <div className="d-flex align-items-center gap-3">
              {isAuthenticated ? (
                <CDropdown variant="nav-item">
                  <CDropdownToggle className="d-flex align-items-center text-decoration-none">
                    <FontAwesomeIcon icon={faUser} className="me-2" />
                    <span className="d-none d-sm-inline">
                      {user?.firstName} {user?.lastName}
                    </span>
                  </CDropdownToggle>
                  <CDropdownMenu>
                    <CDropdownItem as={Link} to="/profile">
                      <FontAwesomeIcon icon={faUser} className="me-2" />
                      Profile
                    </CDropdownItem>
                    <CDropdownItem as={Link} to="/settings">
                      <FontAwesomeIcon icon={faCog} className="me-2" />
                      Settings
                    </CDropdownItem>
                    <hr className="dropdown-divider" />
                    <CDropdownItem onClick={handleLogout} disabled={isLoading}>
                      <FontAwesomeIcon icon={faSignOutAlt} className="me-2" />
                      {isLoading ? 'Signing out...' : 'Sign Out'}
                    </CDropdownItem>
                  </CDropdownMenu>
                </CDropdown>
              ) : (
                <div className="d-flex align-items-center gap-2">
                  <button 
                    className="btn btn-outline-primary btn-sm"
                    onClick={handleLogin}
                    disabled={isLoading}
                  >
                    <FontAwesomeIcon icon={faSignInAlt} className="me-1" />
                    Sign In
                  </button>
                  <button 
                    className="btn btn-primary btn-sm"
                    onClick={handleRegister}
                    disabled={isLoading}
                  >
                    <FontAwesomeIcon icon={faUserPlus} className="me-1" />
                    Sign Up
                  </button>
                </div>
              )}
            </div>
          </CHeaderNav>
        </CHeader>

        <div className="body flex-grow-1">
          <CContainer fluid className="h-auto px-0">
            <div className="content-wrapper">
              {children}
            </div>
          </CContainer>
        </div>
      </div>

      {/* Auth Modal */}
      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
        initialMode={authModalMode}
      />
    </div>
  );
};

export default CoreUILayout;