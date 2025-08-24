import React, { useState } from 'react';
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
  CNavLink
} from '@coreui/react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faHome,
  faCalendarAlt,
  faUsers,
  faChartBar,
  faQrcode,
  faCog,
  faBuilding,
  faClipboardCheck,
  faUserCheck,
  faBars
} from '@fortawesome/free-solid-svg-icons';
import { Link, useLocation } from 'react-router-dom';

interface CoreUILayoutProps {
  children: React.ReactNode;
}

const CoreUILayout: React.FC<CoreUILayoutProps> = ({ children }) => {
  const [sidebarShow, setSidebarShow] = useState(true);
  const location = useLocation();

  const navigation = [
    {
      component: CNavItem,
      name: 'Dashboard',
      to: '/',
      icon: <FontAwesomeIcon icon={faHome} className="nav-icon" />,
    },
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
      name: 'QR Scanner',
      to: '/qr-scanner',
      icon: <FontAwesomeIcon icon={faQrcode} className="nav-icon" />,
    },
    {
      component: CNavItem,
      name: 'Resource Management',
      to: '/resources',
      icon: <FontAwesomeIcon icon={faBuilding} className="nav-icon" />,
    },
    {
      component: CNavItem,
      name: 'Settings',
      to: '/settings',
      icon: <FontAwesomeIcon icon={faCog} className="nav-icon" />,
    },
  ];

  return (
    <div className="c-app c-default-layout">
      <CSidebar
        show={sidebarShow}
        onShowChange={(val) => setSidebarShow(val)}
        className="c-sidebar-dark c-sidebar-fixed c-sidebar-lg-show"
      >
        <div className="c-sidebar-brand d-flex align-items-center justify-content-center">
          <div className="c-sidebar-brand-full">
            <h4 className="text-white mb-0">EventR</h4>
            <small className="text-white-75">Enterprise</small>
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

      <div className="c-wrapper">
        <CHeader className="c-header c-header-light">
          <CHeaderToggler
            className="ps-1"
            onClick={() => setSidebarShow(!sidebarShow)}
          >
            <FontAwesomeIcon icon={faBars} size="lg" />
          </CHeaderToggler>
          
          <CHeaderNav className="ms-auto">
            <div className="d-flex align-items-center">
              <span className="text-medium-emphasis small me-3">
                Welcome to EventR Enterprise
              </span>
            </div>
          </CHeaderNav>
        </CHeader>

        <div className="c-body">
          <main className="c-main">
            <CContainer fluid className="px-4">
              {children}
            </CContainer>
          </main>
        </div>
      </div>
    </div>
  );
};

export default CoreUILayout;