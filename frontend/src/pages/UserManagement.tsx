import React, { useState, useEffect } from 'react';
import {
  CRow,
  CCol,
  CCard,
  CCardBody,
  CCardHeader,
  CTable,
  CTableHead,
  CTableRow,
  CTableHeaderCell,
  CTableBody,
  CTableDataCell,
  CButton,
  CButtonGroup,
  CBadge,
  CDropdown,
  CDropdownToggle,
  CDropdownMenu,
  CDropdownItem,
  CWidgetStatsF,
} from '@coreui/react';

// Import Bootstrap components for compatibility
import { Modal, Form, Alert, Toast } from 'react-bootstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faUsers,
  faSearch,
  faFilter,
  faPlus,
  faEdit,
  faTrash,
  faShield,
  faUserCheck,
  faUserTimes,
  faEye,
  faEnvelope,
  faDownload,
  faSort,
  faEllipsisV,
  faUserCog,
  faHistory,
  faBan,
  faCheckCircle,
  faTimesCircle,
  faSync,
} from '@fortawesome/free-solid-svg-icons';
import apiClient from '../api/apiClient';

interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: 'ATTENDEE' | 'ORGANIZER' | 'ADMIN' | 'SUPER_ADMIN';
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  lastLogin?: string;
  registrationCount: number;
  eventCount: number;
  createdAt: string;
}

interface UserStats {
  totalUsers: number;
  activeUsers: number;
  attendeeUsers: number;
  organizerUsers: number;
  adminUsers: number;
  newUsersThisMonth: number;
}

interface FilterOptions {
  search: string;
  role: string;
  status: string;
  dateRange: string;
  sortBy: string;
  sortOrder: 'asc' | 'desc';
}

const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [filteredUsers, setFilteredUsers] = useState<User[]>([]);
  const [stats, setStats] = useState<UserStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedUsers, setSelectedUsers] = useState<Set<string>>(new Set());
  
  const [filters, setFilters] = useState<FilterOptions>({
    search: '',
    role: 'ALL',
    status: 'ALL',
    dateRange: 'ALL',
    sortBy: 'createdAt',
    sortOrder: 'desc',
  });
  
  const [showFilters, setShowFilters] = useState(false);
  const [showBulkActions, setShowBulkActions] = useState(false);
  const [bulkProcessing, setBulkProcessing] = useState(false);
  
  const [toast, setToast] = useState<{ message: string; color: string; visible: boolean }>({
    message: '',
    color: 'success',
    visible: false,
  });

  const fetchUsers = async () => {
    try {
      // TODO: Replace with actual API endpoint
      const mockUsers: User[] = [
        {
          id: '1',
          firstName: 'John',
          lastName: 'Doe',
          email: 'john@example.com',
          role: 'ATTENDEE',
          status: 'ACTIVE',
          lastLogin: new Date().toISOString(),
          registrationCount: 5,
          eventCount: 0,
          createdAt: new Date().toISOString(),
        },
        {
          id: '2',
          firstName: 'Jane',
          lastName: 'Smith',
          email: 'jane@example.com',
          role: 'ORGANIZER',
          status: 'ACTIVE',
          lastLogin: new Date().toISOString(),
          registrationCount: 2,
          eventCount: 8,
          createdAt: new Date().toISOString(),
        },
      ];
      
      setUsers(mockUsers);
    } catch (error) {
      console.error('Failed to fetch users:', error);
      showToast('Failed to load users', 'danger');
    }
  };

  const fetchStats = async () => {
    try {
      // TODO: Replace with actual API endpoint
      const mockStats: UserStats = {
        totalUsers: users.length,
        activeUsers: users.filter(u => u.status === 'ACTIVE').length,
        attendeeUsers: users.filter(u => u.role === 'ATTENDEE').length,
        organizerUsers: users.filter(u => u.role === 'ORGANIZER').length,
        adminUsers: users.filter(u => u.role === 'ADMIN' || u.role === 'SUPER_ADMIN').length,
        newUsersThisMonth: Math.floor(users.length / 3), // Mock calculation
      };
      
      setStats(mockStats);
    } catch (error) {
      console.error('Failed to fetch user stats:', error);
    }
  };

  const applyFilters = () => {
    let filtered = [...users];

    // Search filter
    if (filters.search) {
      const searchLower = filters.search.toLowerCase();
      filtered = filtered.filter(user =>
        user.firstName.toLowerCase().includes(searchLower) ||
        user.lastName.toLowerCase().includes(searchLower) ||
        user.email.toLowerCase().includes(searchLower)
      );
    }

    // Role filter
    if (filters.role !== 'ALL') {
      filtered = filtered.filter(user => user.role === filters.role);
    }

    // Status filter
    if (filters.status !== 'ALL') {
      filtered = filtered.filter(user => user.status === filters.status);
    }

    // Sort users
    filtered.sort((a, b) => {
      let aValue, bValue;
      
      switch (filters.sortBy) {
        case 'name':
          aValue = `${a.firstName} ${a.lastName}`.toLowerCase();
          bValue = `${b.firstName} ${b.lastName}`.toLowerCase();
          break;
        case 'email':
          aValue = a.email.toLowerCase();
          bValue = b.email.toLowerCase();
          break;
        case 'role':
          aValue = a.role;
          bValue = b.role;
          break;
        case 'status':
          aValue = a.status;
          bValue = b.status;
          break;
        case 'lastLogin':
          aValue = new Date(a.lastLogin || 0);
          bValue = new Date(b.lastLogin || 0);
          break;
        default:
          aValue = new Date(a.createdAt);
          bValue = new Date(b.createdAt);
      }

      if (aValue < bValue) return filters.sortOrder === 'asc' ? -1 : 1;
      if (aValue > bValue) return filters.sortOrder === 'asc' ? 1 : -1;
      return 0;
    });

    setFilteredUsers(filtered);
  };

  const handleSelectUser = (userId: string) => {
    const newSelection = new Set(selectedUsers);
    if (newSelection.has(userId)) {
      newSelection.delete(userId);
    } else {
      newSelection.add(userId);
    }
    setSelectedUsers(newSelection);
    setShowBulkActions(newSelection.size > 0);
  };

  const handleSelectAll = () => {
    if (selectedUsers.size === filteredUsers.length && filteredUsers.length > 0) {
      setSelectedUsers(new Set());
      setShowBulkActions(false);
    } else {
      setSelectedUsers(new Set(filteredUsers.map(u => u.id)));
      setShowBulkActions(true);
    }
  };

  const handleBulkAction = async (action: 'activate' | 'suspend' | 'delete' | 'export') => {
    if (selectedUsers.size === 0) {
      showToast('Please select users first', 'warning');
      return;
    }

    if (action === 'delete' && !window.confirm(`Are you sure you want to delete ${selectedUsers.size} users?`)) {
      return;
    }

    setBulkProcessing(true);
    const userIds = Array.from(selectedUsers);

    try {
      switch (action) {
        case 'activate':
          // TODO: Implement bulk activation
          showToast(`Successfully activated ${userIds.length} users`, 'success');
          break;
        case 'suspend':
          // TODO: Implement bulk suspension
          showToast(`Successfully suspended ${userIds.length} users`, 'warning');
          break;
        case 'delete':
          // TODO: Implement bulk deletion
          showToast(`Successfully deleted ${userIds.length} users`, 'success');
          break;
        case 'export':
          // TODO: Implement export functionality
          showToast('Export feature coming soon', 'info');
          break;
      }
      
      await fetchUsers();
      setSelectedUsers(new Set());
      setShowBulkActions(false);
    } catch (error) {
      showToast(`Failed to ${action} users`, 'danger');
      console.error(`Bulk ${action} failed:`, error);
    } finally {
      setBulkProcessing(false);
    }
  };

  const showToast = (message: string, color: string) => {
    setToast({ message, color, visible: true });
    setTimeout(() => setToast(prev => ({ ...prev, visible: false })), 5000);
  };

  const getRoleBadge = (role: string) => {
    const roleConfig = {
      ATTENDEE: { color: 'primary', text: 'Attendee' },
      ORGANIZER: { color: 'success', text: 'Organizer' },
      ADMIN: { color: 'warning', text: 'Admin' },
      SUPER_ADMIN: { color: 'danger', text: 'Super Admin' },
    };
    
    const config = roleConfig[role as keyof typeof roleConfig] || { color: 'secondary', text: role };
    return <CBadge color={config.color}>{config.text}</CBadge>;
  };

  const getStatusBadge = (status: string) => {
    const statusConfig = {
      ACTIVE: { color: 'success', icon: faCheckCircle },
      INACTIVE: { color: 'secondary', icon: faTimesCircle },
      SUSPENDED: { color: 'danger', icon: faBan },
    };
    
    const config = statusConfig[status as keyof typeof statusConfig] || { color: 'secondary', icon: faTimesCircle };
    
    return (
      <CBadge color={config.color}>
        <FontAwesomeIcon icon={config.icon} className="me-1" />
        {status}
      </CBadge>
    );
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString();
  };

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      await Promise.all([fetchUsers(), fetchStats()]);
      setLoading(false);
    };
    loadData();
  }, []);

  useEffect(() => {
    applyFilters();
  }, [users, filters]);

  useEffect(() => {
    fetchStats();
  }, [users]);

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ height: '200px' }}>
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="animated fadeIn">
      {/* Toast Notifications */}
      <div className="position-fixed top-0 end-0 p-3" style={{ zIndex: 1050 }}>
        <Toast show={toast.visible} onClose={() => setToast(prev => ({ ...prev, visible: false }))} bg={toast.color}>
          <Toast.Body>{toast.message}</Toast.Body>
        </Toast>
      </div>

      {/* Header */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h2 mb-0">User Management</h1>
          <p className="text-medium-emphasis mb-0">
            Manage users, roles, and permissions
          </p>
        </div>
        
        <div className="d-flex gap-2">
          <CButton color="primary">
            <FontAwesomeIcon icon={faPlus} className="me-1" />
            Add User
          </CButton>
          
          <CButton 
            color="outline-secondary" 
            onClick={() => { fetchUsers(); fetchStats(); }}
          >
            <FontAwesomeIcon icon={faSync} />
          </CButton>
        </div>
      </div>

      {/* Stats Overview */}
      {stats && (
        <CRow className="mb-4">
          <CCol sm={6} lg={2}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faUsers} size="xl" />}
              title="Total Users"
              value={stats.totalUsers.toString()}
              color="primary"
            />
          </CCol>
          <CCol sm={6} lg={2}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faUserCheck} size="xl" />}
              title="Active Users"
              value={stats.activeUsers.toString()}
              color="success"
            />
          </CCol>
          <CCol sm={6} lg={2}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faUsers} size="xl" />}
              title="Attendees"
              value={stats.attendeeUsers.toString()}
              color="info"
            />
          </CCol>
          <CCol sm={6} lg={2}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faUserCog} size="xl" />}
              title="Organizers"
              value={stats.organizerUsers.toString()}
              color="warning"
            />
          </CCol>
          <CCol sm={6} lg={2}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faShield} size="xl" />}
              title="Admins"
              value={stats.adminUsers.toString()}
              color="danger"
            />
          </CCol>
          <CCol sm={6} lg={2}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faPlus} size="xl" />}
              title="New This Month"
              value={stats.newUsersThisMonth.toString()}
              color="secondary"
            />
          </CCol>
        </CRow>
      )}

      {/* Filters and Search */}
      <CRow className="mb-4">
        <CCol>
          <CCard>
            <CCardHeader>
              <div className="d-flex justify-content-between align-items-center">
                <h6 className="mb-0">User Filters</h6>
                <CButton
                  color="outline-secondary"
                  size="sm"
                  onClick={() => setShowFilters(!showFilters)}
                >
                  <FontAwesomeIcon icon={faFilter} className="me-1" />
                  {showFilters ? 'Hide Filters' : 'Show Filters'}
                </CButton>
              </div>
            </CCardHeader>
            
            <CCardBody>
              {/* Quick Search */}
              <CRow className="mb-3">
                <CCol md={6}>
                  <div className="input-group">
                    <span className="input-group-text">
                      <FontAwesomeIcon icon={faSearch} />
                    </span>
                    <Form.Control
                      placeholder="Search users by name or email..."
                      value={filters.search}
                      onChange={(e) => setFilters(prev => ({ ...prev, search: e.target.value }))}
                    />
                  </div>
                </CCol>
                <CCol md={6}>
                  <div className="d-flex gap-2">
                    <CButton
                      color="outline-primary"
                      onClick={() => setFilters({
                        search: '',
                        role: 'ALL',
                        status: 'ALL',
                        dateRange: 'ALL',
                        sortBy: 'createdAt',
                        sortOrder: 'desc',
                      })}
                    >
                      Clear Filters
                    </CButton>
                    
                    <CButton color="outline-success" onClick={() => handleBulkAction('export')}>
                      <FontAwesomeIcon icon={faDownload} className="me-1" />
                      Export
                    </CButton>
                  </div>
                </CCol>
              </CRow>

              {/* Advanced Filters */}
              {showFilters && (
                <CRow className="mb-3">
                  <CCol md={3}>
                    <Form.Select
                      value={filters.role}
                      onChange={(e) => setFilters(prev => ({ ...prev, role: e.target.value }))}
                    >
                      <option value="ALL">All Roles</option>
                      <option value="ATTENDEE">Attendee</option>
                      <option value="ORGANIZER">Organizer</option>
                      <option value="ADMIN">Admin</option>
                      <option value="SUPER_ADMIN">Super Admin</option>
                    </Form.Select>
                  </CCol>
                  <CCol md={3}>
                    <Form.Select
                      value={filters.status}
                      onChange={(e) => setFilters(prev => ({ ...prev, status: e.target.value }))}
                    >
                      <option value="ALL">All Statuses</option>
                      <option value="ACTIVE">Active</option>
                      <option value="INACTIVE">Inactive</option>
                      <option value="SUSPENDED">Suspended</option>
                    </Form.Select>
                  </CCol>
                  <CCol md={3}>
                    <Form.Select
                      value={filters.dateRange}
                      onChange={(e) => setFilters(prev => ({ ...prev, dateRange: e.target.value }))}
                    >
                      <option value="ALL">All Time</option>
                      <option value="WEEK">This Week</option>
                      <option value="MONTH">This Month</option>
                      <option value="QUARTER">This Quarter</option>
                    </Form.Select>
                  </CCol>
                  <CCol md={3}>
                    <div className="d-flex">
                      <Form.Select
                        value={filters.sortBy}
                        onChange={(e) => setFilters(prev => ({ ...prev, sortBy: e.target.value }))}
                        className="me-1"
                      >
                        <option value="createdAt">Created Date</option>
                        <option value="name">Name</option>
                        <option value="email">Email</option>
                        <option value="role">Role</option>
                        <option value="status">Status</option>
                        <option value="lastLogin">Last Login</option>
                      </Form.Select>
                      <CButton
                        color="outline-secondary"
                        onClick={() => setFilters(prev => ({ 
                          ...prev, 
                          sortOrder: prev.sortOrder === 'asc' ? 'desc' : 'asc' 
                        }))}
                      >
                        <FontAwesomeIcon 
                          icon={faSort} 
                          rotation={filters.sortOrder === 'desc' ? 180 : undefined}
                        />
                      </CButton>
                    </div>
                  </CCol>
                </CRow>
              )}

              {/* Bulk Actions Bar */}
              {showBulkActions && (
                <Alert color="info" className="d-flex justify-content-between align-items-center">
                  <div>
                    <FontAwesomeIcon icon={faUsers} className="me-2" />
                    {selectedUsers.size} user{selectedUsers.size !== 1 ? 's' : ''} selected
                  </div>
                  <CButtonGroup>
                    <CButton
                      color="success"
                      size="sm"
                      onClick={() => handleBulkAction('activate')}
                      disabled={bulkProcessing}
                    >
                      <FontAwesomeIcon icon={faCheckCircle} className="me-1" />
                      Activate
                    </CButton>
                    <CButton
                      color="warning"
                      size="sm"
                      onClick={() => handleBulkAction('suspend')}
                      disabled={bulkProcessing}
                    >
                      <FontAwesomeIcon icon={faBan} className="me-1" />
                      Suspend
                    </CButton>
                    <CButton
                      color="info"
                      size="sm"
                      onClick={() => handleBulkAction('export')}
                      disabled={bulkProcessing}
                    >
                      <FontAwesomeIcon icon={faDownload} className="me-1" />
                      Export
                    </CButton>
                    <CButton
                      color="danger"
                      size="sm"
                      onClick={() => handleBulkAction('delete')}
                      disabled={bulkProcessing}
                    >
                      <FontAwesomeIcon icon={faTrash} className="me-1" />
                      Delete
                    </CButton>
                  </CButtonGroup>
                </Alert>
              )}
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* Users Table */}
      <CRow>
        <CCol>
          <CCard>
            <CCardHeader>
              <div className="d-flex justify-content-between align-items-center">
                <h5 className="card-title mb-0">Users</h5>
                <div className="d-flex align-items-center gap-3">
                  <CBadge color="light" className="fs-6">
                    {filteredUsers.length} of {users.length} users
                  </CBadge>
                  {filteredUsers.length !== users.length && (
                    <CBadge color="info">Filtered</CBadge>
                  )}
                </div>
              </div>
            </CCardHeader>
            
            <CCardBody className="p-0">
              <CTable hover responsive>
                <CTableHead>
                  <CTableRow>
                    <CTableHeaderCell width="50">
                      <input 
                        type="checkbox" 
                        className="form-check-input"
                        checked={selectedUsers.size === filteredUsers.length && filteredUsers.length > 0}
                        onChange={handleSelectAll}
                      />
                    </CTableHeaderCell>
                    <CTableHeaderCell>User Details</CTableHeaderCell>
                    <CTableHeaderCell>Role & Status</CTableHeaderCell>
                    <CTableHeaderCell>Activity</CTableHeaderCell>
                    <CTableHeaderCell>Last Login</CTableHeaderCell>
                    <CTableHeaderCell>Actions</CTableHeaderCell>
                  </CTableRow>
                </CTableHead>
                
                <CTableBody>
                  {filteredUsers.map(user => (
                    <CTableRow 
                      key={user.id}
                      className={selectedUsers.has(user.id) ? 'table-active' : ''}
                    >
                      <CTableDataCell>
                        <input 
                          type="checkbox" 
                          className="form-check-input"
                          checked={selectedUsers.has(user.id)}
                          onChange={() => handleSelectUser(user.id)}
                        />
                      </CTableDataCell>
                      <CTableDataCell>
                        <div className="fw-semibold">{user.firstName} {user.lastName}</div>
                        <div className="small text-medium-emphasis">{user.email}</div>
                        <div className="small text-muted">
                          Joined {formatDate(user.createdAt)}
                        </div>
                      </CTableDataCell>
                      <CTableDataCell>
                        <div className="mb-1">
                          {getRoleBadge(user.role)}
                        </div>
                        {getStatusBadge(user.status)}
                      </CTableDataCell>
                      <CTableDataCell>
                        <div className="d-flex gap-3">
                          <div className="text-center">
                            <div className="fw-semibold text-success">{user.registrationCount}</div>
                            <small className="text-muted">Registrations</small>
                          </div>
                          {user.role === 'ORGANIZER' && (
                            <div className="text-center">
                              <div className="fw-semibold text-primary">{user.eventCount}</div>
                              <small className="text-muted">Events</small>
                            </div>
                          )}
                        </div>
                      </CTableDataCell>
                      <CTableDataCell>
                        {user.lastLogin ? (
                          <div>
                            <div className="small">{formatDateTime(user.lastLogin)}</div>
                            <CBadge color="success" className="small">Online</CBadge>
                          </div>
                        ) : (
                          <span className="text-muted">Never</span>
                        )}
                      </CTableDataCell>
                      <CTableDataCell>
                        <div className="d-flex gap-1">
                          <CButton color="outline-primary" size="sm" title="View Profile">
                            <FontAwesomeIcon icon={faEye} />
                          </CButton>
                          
                          <CButton color="outline-secondary" size="sm" title="Edit User">
                            <FontAwesomeIcon icon={faEdit} />
                          </CButton>
                          
                          <CButton color="outline-info" size="sm" title="Send Email">
                            <FontAwesomeIcon icon={faEnvelope} />
                          </CButton>

                          <CDropdown>
                            <CDropdownToggle color="outline-info" size="sm" caret={false}>
                              <FontAwesomeIcon icon={faEllipsisV} />
                            </CDropdownToggle>
                            <CDropdownMenu>
                              <CDropdownItem>
                                <FontAwesomeIcon icon={faShield} className="me-2" />
                                Change Role
                              </CDropdownItem>
                              <CDropdownItem>
                                <FontAwesomeIcon icon={faHistory} className="me-2" />
                                Activity Log
                              </CDropdownItem>
                              <hr className="dropdown-divider" />
                              <CDropdownItem>
                                <FontAwesomeIcon icon={faBan} className="me-2" />
                                Suspend User
                              </CDropdownItem>
                              <CDropdownItem className="text-danger">
                                <FontAwesomeIcon icon={faTrash} className="me-2" />
                                Delete User
                              </CDropdownItem>
                            </CDropdownMenu>
                          </CDropdown>
                        </div>
                      </CTableDataCell>
                    </CTableRow>
                  ))}
                  
                  {filteredUsers.length === 0 && (
                    <CTableRow>
                      <CTableDataCell colSpan={6} className="text-center py-5">
                        <div className="text-medium-emphasis">
                          <FontAwesomeIcon 
                            icon={users.length === 0 ? faUsers : faSearch} 
                            size="3x" 
                            className="mb-3 text-muted" 
                          />
                          <h5>
                            {users.length === 0 ? 'No users found' : 'No users match your filters'}
                          </h5>
                          <p className="text-muted">
                            {users.length === 0 
                              ? 'Users will appear here once they register for events'
                              : 'Try adjusting your search or filter criteria'
                            }
                          </p>
                          {users.length === 0 ? (
                            <CButton color="primary">
                              <FontAwesomeIcon icon={faPlus} className="me-2" />
                              Add First User
                            </CButton>
                          ) : (
                            <CButton
                              color="outline-secondary"
                              onClick={() => setFilters({
                                search: '',
                                role: 'ALL',
                                status: 'ALL',
                                dateRange: 'ALL',
                                sortBy: 'createdAt',
                                sortOrder: 'desc',
                              })}
                            >
                              Clear All Filters
                            </CButton>
                          )}
                        </div>
                      </CTableDataCell>
                    </CTableRow>
                  )}
                </CTableBody>
              </CTable>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>
    </div>
  );
};

export default UserManagement;