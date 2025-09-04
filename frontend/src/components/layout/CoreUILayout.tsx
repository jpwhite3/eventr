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
  faUsers,
  faChartBar,
  faBuilding,
  faClipboardCheck,
  faUserCheck,
  faBars,
  faUser,
  faSignOutAlt,
  faSignInAlt,
  faUserPlus,
  faCog
} from '@fortawesome/free-solid-svg-icons';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import AuthModal from '../auth/AuthModal';

interface CoreUILayoutProps {
  children: React.ReactNode;
}

const CoreUILayout: React.FC<CoreUILayoutProps> = ({ children }) => {
  const [sidebarShow, setSidebarShow] = useState(false); // Start hidden on mobile
  const [isMobile, setIsMobile] = useState(true);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [authModalMode, setAuthModalMode] = useState<'login' | 'register'>('login');
  const location = useLocation();
  const { user, isAuthenticated, logout, isLoading } = useAuth();

  // Handle responsive behavior
  useEffect(() => {
    const handleResize = () => {
      const mobile = window.innerWidth < 992;
      setIsMobile(mobile);
      if (!mobile) {
        setSidebarShow(true);
      } else {
        setSidebarShow(false);
      }
    };

    // Set initial state
    handleResize();

    // Add event listener
    window.addEventListener('resize', handleResize);

    // Cleanup
    return () => window.removeEventListener('resize', handleResize);
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
      name: 'Homepage',
      to: '/',
      icon: <FontAwesomeIcon icon={faHome} className="nav-icon" />,
    },
    ...(isAuthenticated ? [
      {
        component: CNavItem,
        name: 'My Dashboard',
        to: '/dashboard',
        icon: <FontAwesomeIcon icon={faUser} className="nav-icon" />,
      },
      {
        component: CNavItem,
        name: 'My Registrations',
        to: '/my-registrations',
        icon: <FontAwesomeIcon icon={faClipboardCheck} className="nav-icon" />,
      }
    ] : []),
    {
      component: CNavTitle,
      name: 'Event Management',
    },
    {
      component: CNavItem,
      name: 'Events',
      to: '/events',
      icon: <FontAwesomeIcon icon={faCalendarAlt} className="nav-icon" />,
    },
    {
      component: CNavItem,
      name: 'Registrations',
      to: '/registrations',
      icon: <FontAwesomeIcon icon={faUsers} className="nav-icon" />,
    },
    {
      component: CNavItem,
      name: 'Check-In',
      to: '/checkin',
      icon: <FontAwesomeIcon icon={faUserCheck} className="nav-icon" />,
    },
    {
      component: CNavTitle,
      name: 'Analytics & Reports',
    },
    {
      component: CNavItem,
      name: 'Executive Dashboard',
      to: '/analytics/executive',
      icon: <FontAwesomeIcon icon={faChartBar} className="nav-icon" />,
    },
    {
      component: CNavItem,
      name: 'Registration Trends',
      to: '/analytics/registrations',
      icon: <FontAwesomeIcon icon={faUsers} className="nav-icon" />,
    },
    {
      component: CNavItem,
      name: 'Event Analytics',
      to: '/analytics/events',
      icon: <FontAwesomeIcon icon={faCalendarAlt} className="nav-icon" />,
    },
    {
      component: CNavItem,
      name: 'Attendance Reports',
      to: '/analytics/attendance',
      icon: <FontAwesomeIcon icon={faClipboardCheck} className="nav-icon" />,
    },
    {
      component: CNavTitle,
      name: 'Tools',
    },
    {
      component: CNavItem,
      name: 'Resource Management',
      to: '/resources',
      icon: <FontAwesomeIcon icon={faBuilding} className="nav-icon" />,
    },
  ];

  return (
    <div className="d-flex">
      {/* Mobile backdrop */}
      {sidebarShow && isMobile && (
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
        onVisibleChange={(visible) => setSidebarShow(visible)}
        className="sidebar-dark sidebar-fixed"
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
        
        <CSidebarToggler
          className="d-none d-lg-flex"
          onClick={() => setSidebarShow(!sidebarShow)}
        />
      </CSidebar>

      <div className="wrapper d-flex flex-column min-vh-100 w-100">
        <CHeader className="header header-sticky mb-4">
          <CHeaderToggler
            className="ps-1"
            onClick={() => setSidebarShow(!sidebarShow)}
          >
            <FontAwesomeIcon icon={faBars} size="lg" />
          </CHeaderToggler>
          
          <CHeaderNav className="ms-auto">
            <div className="d-flex align-items-center">
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
                    <CDropdownItem divider />
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
          <CContainer fluid className="h-auto px-4">
            {children}
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