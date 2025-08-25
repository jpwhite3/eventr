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
  CWidgetStatsF,
  CBadge,
} from '@coreui/react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faBuilding,
  faCogs,
  faUsers,
  faCar,
  faUtensils,
  faWifi,
  faEdit,
  faTrash,
  faPlus,
  faSearch,
  faFilter,
  faSync,
  faEye,
  faCalendarAlt
} from '@fortawesome/free-solid-svg-icons';

interface Resource {
  id: string;
  name: string;
  description?: string;
  type: 'ROOM' | 'EQUIPMENT' | 'STAFF' | 'VEHICLE' | 'CATERING' | 'TECHNOLOGY' | 'OTHER';
  status: 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE' | 'RESERVED' | 'OUT_OF_SERVICE';
  capacity?: number;
  location?: string;
  floor?: string;
  building?: string;
  specifications?: string;
  serialNumber?: string;
  model?: string;
  manufacturer?: string;
  isBookable: boolean;
  requiresApproval: boolean;
  bookingLeadTimeHours: number;
  maxBookingDurationHours?: number;
  hourlyRate?: number;
  dailyRate?: number;
  setupCost?: number;
  cleanupCost?: number;
  lastMaintenanceDate?: string;
  nextMaintenanceDate?: string;
  maintenanceNotes?: string;
  contactPerson?: string;
  contactEmail?: string;
  contactPhone?: string;
  departmentOwner?: string;
  totalUsageHours: number;
  usageThisMonth: number;
  lastUsedAt?: string;
  tags?: string;
  category?: string;
  isActive: boolean;
}

interface ResourceStats {
  totalResources: number;
  availableResources: number;
  occupiedResources: number;
  maintenanceResources: number;
  utilizationRate: number;
}

const ResourceManagement: React.FC = () => {
  const [resources, setResources] = useState<Resource[]>([]);
  const [stats, setStats] = useState<ResourceStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('');
  const [filterStatus, setFilterStatus] = useState('');

  useEffect(() => {
    loadResources();
  }, []);

  const loadResources = async () => {
    try {
      setLoading(true);
      // This would need to be implemented in the backend
      const apiClient = (await import('../api/apiClient')).default;
      const response = await apiClient.get('/resources');
      setResources(response.data);
      
      // Calculate stats from loaded data
      const totalResources = response.data.length;
      const availableResources = response.data.filter((r: Resource) => r.status === 'AVAILABLE').length;
      const occupiedResources = response.data.filter((r: Resource) => r.status === 'OCCUPIED').length;
      const maintenanceResources = response.data.filter((r: Resource) => r.status === 'MAINTENANCE').length;
      const utilizationRate = totalResources > 0 ? ((occupiedResources + maintenanceResources) / totalResources * 100) : 0;
      
      setStats({
        totalResources,
        availableResources,
        occupiedResources,
        maintenanceResources,
        utilizationRate
      });
    } catch (error) {
      console.error('Failed to load resources:', error);
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'AVAILABLE':
        return <CBadge color="success">Available</CBadge>;
      case 'OCCUPIED':
        return <CBadge color="warning">Occupied</CBadge>;
      case 'MAINTENANCE':
        return <CBadge color="danger">Maintenance</CBadge>;
      case 'RESERVED':
        return <CBadge color="info">Reserved</CBadge>;
      case 'OUT_OF_SERVICE':
        return <CBadge color="dark">Out of Service</CBadge>;
      default:
        return <CBadge color="light">{status}</CBadge>;
    }
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'ROOM':
        return <FontAwesomeIcon icon={faBuilding} />;
      case 'EQUIPMENT':
        return <FontAwesomeIcon icon={faCogs} />;
      case 'STAFF':
        return <FontAwesomeIcon icon={faUsers} />;
      case 'VEHICLE':
        return <FontAwesomeIcon icon={faCar} />;
      case 'CATERING':
        return <FontAwesomeIcon icon={faUtensils} />;
      case 'TECHNOLOGY':
        return <FontAwesomeIcon icon={faWifi} />;
      default:
        return <FontAwesomeIcon icon={faCogs} />;
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString();
  };

  const filteredResources = resources.filter(resource => {
    const matchesSearch = resource.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         (resource.description?.toLowerCase().includes(searchTerm.toLowerCase())) ||
                         (resource.category?.toLowerCase().includes(searchTerm.toLowerCase()));
    const matchesType = !filterType || resource.type === filterType;
    const matchesStatus = !filterStatus || resource.status === filterStatus;
    
    return matchesSearch && matchesType && matchesStatus;
  });

  const handleViewResource = (resource: Resource) => {
    // TODO: Implement resource details view
    console.log('Viewing resource:', resource);
  };

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ height: '200px' }}>
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading resources...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="animated fadeIn">
      {/* Header */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h2 mb-0">Resource Management</h1>
          <p className="text-medium-emphasis mb-0">
            Manage and monitor all your resources and facilities
          </p>
        </div>
        
        <div className="d-flex gap-2">
          <CButton color="outline-secondary" onClick={loadResources}>
            <FontAwesomeIcon icon={faSync} className="me-1" />
            Refresh
          </CButton>
          <CButton color="primary">
            <FontAwesomeIcon icon={faPlus} className="me-1" />
            Add Resource
          </CButton>
        </div>
      </div>

      {/* Stats Overview */}
      {stats && (
        <CRow className="mb-4">
          <CCol sm={6} lg={3}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faCogs} size="xl" />}
              title="Total Resources"
              value={stats.totalResources.toString()}
              color="primary"
            />
          </CCol>
          <CCol sm={6} lg={3}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faSearch} size="xl" />}
              title="Available"
              value={stats.availableResources.toString()}
              color="success"
            />
          </CCol>
          <CCol sm={6} lg={3}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faCalendarAlt} size="xl" />}
              title="Occupied"
              value={stats.occupiedResources.toString()}
              color="warning"
            />
          </CCol>
          <CCol sm={6} lg={3}>
            <CWidgetStatsF
              className="mb-3"
              icon={<FontAwesomeIcon icon={faBuilding} size="xl" />}
              title="Utilization Rate"
              value={`${stats.utilizationRate.toFixed(1)}%`}
              color="info"
            />
          </CCol>
        </CRow>
      )}

      {/* Filters */}
      <CRow className="mb-4">
        <CCol>
          <CCard>
            <CCardBody>
              <div>
                <CRow className="align-items-end">
                  <CCol md={4}>
                    <div className="input-group">
                      <span className="input-group-text">
                        <FontAwesomeIcon icon={faSearch} />
                      </span>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="Search resources..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                      />
                    </div>
                  </CCol>
                  <CCol md={3}>
                    <select
                      className="form-select"
                      value={filterType}
                      onChange={(e) => setFilterType(e.target.value)}
                    >
                      <option value="">All Types</option>
                      <option value="ROOM">Rooms</option>
                      <option value="EQUIPMENT">Equipment</option>
                      <option value="STAFF">Staff</option>
                      <option value="VEHICLE">Vehicles</option>
                      <option value="CATERING">Catering</option>
                      <option value="TECHNOLOGY">Technology</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </CCol>
                  <CCol md={3}>
                    <select
                      className="form-select"
                      value={filterStatus}
                      onChange={(e) => setFilterStatus(e.target.value)}
                    >
                      <option value="">All Status</option>
                      <option value="AVAILABLE">Available</option>
                      <option value="OCCUPIED">Occupied</option>
                      <option value="MAINTENANCE">Maintenance</option>
                      <option value="RESERVED">Reserved</option>
                      <option value="OUT_OF_SERVICE">Out of Service</option>
                    </select>
                  </CCol>
                  <CCol md={2}>
                    <CButton 
                      color="outline-secondary" 
                      onClick={() => {
                        setSearchTerm('');
                        setFilterType('');
                        setFilterStatus('');
                      }}
                    >
                      <FontAwesomeIcon icon={faFilter} className="me-1" />
                      Clear
                    </CButton>
                  </CCol>
                </CRow>
              </div>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* Resources Table */}
      <CRow>
        <CCol>
          <CCard>
            <CCardHeader>
              <div className="d-flex justify-content-between align-items-center">
                <h5 className="card-title mb-0">Resources</h5>
                <CBadge color="light" className="fs-6">
                  {filteredResources.length} of {resources.length}
                </CBadge>
              </div>
            </CCardHeader>
            <CCardBody className="p-0">
              <CTable hover responsive>
                <CTableHead>
                  <CTableRow>
                    <CTableHeaderCell>Resource</CTableHeaderCell>
                    <CTableHeaderCell>Type</CTableHeaderCell>
                    <CTableHeaderCell>Status</CTableHeaderCell>
                    <CTableHeaderCell>Location</CTableHeaderCell>
                    <CTableHeaderCell>Usage</CTableHeaderCell>
                    <CTableHeaderCell>Rate</CTableHeaderCell>
                    <CTableHeaderCell>Actions</CTableHeaderCell>
                  </CTableRow>
                </CTableHead>
                <CTableBody>
                  {filteredResources.map(resource => (
                    <CTableRow key={resource.id}>
                      <CTableDataCell>
                        <div className="d-flex align-items-center">
                          <div className="me-2 text-primary">
                            {getTypeIcon(resource.type)}
                          </div>
                          <div>
                            <div className="fw-semibold">{resource.name}</div>
                            {resource.description && (
                              <small className="text-medium-emphasis">
                                {resource.description.length > 50 
                                  ? `${resource.description.substring(0, 50)}...` 
                                  : resource.description}
                              </small>
                            )}
                            {resource.capacity && (
                              <div>
                                <small className="text-info">Capacity: {resource.capacity}</small>
                              </div>
                            )}
                          </div>
                        </div>
                      </CTableDataCell>
                      <CTableDataCell>
                        <CBadge color="light">{resource.type}</CBadge>
                        {resource.category && (
                          <div>
                            <small className="text-medium-emphasis">{resource.category}</small>
                          </div>
                        )}
                      </CTableDataCell>
                      <CTableDataCell>
                        {getStatusBadge(resource.status)}
                        {!resource.isActive && (
                          <div>
                            <CBadge color="dark" className="mt-1">Inactive</CBadge>
                          </div>
                        )}
                      </CTableDataCell>
                      <CTableDataCell>
                        {resource.building && (
                          <div className="fw-semibold">{resource.building}</div>
                        )}
                        {resource.floor && (
                          <div><small>{resource.floor}</small></div>
                        )}
                        {resource.location && (
                          <div><small className="text-medium-emphasis">{resource.location}</small></div>
                        )}
                      </CTableDataCell>
                      <CTableDataCell>
                        <div className="fw-semibold">{resource.usageThisMonth}h this month</div>
                        <small className="text-medium-emphasis">
                          {resource.totalUsageHours}h total
                        </small>
                        {resource.lastUsedAt && (
                          <div>
                            <small>Last used: {formatDate(resource.lastUsedAt)}</small>
                          </div>
                        )}
                      </CTableDataCell>
                      <CTableDataCell>
                        {resource.hourlyRate && (
                          <div className="fw-semibold">{formatCurrency(resource.hourlyRate)}/hr</div>
                        )}
                        {resource.dailyRate && (
                          <div>{formatCurrency(resource.dailyRate)}/day</div>
                        )}
                        {!resource.hourlyRate && !resource.dailyRate && (
                          <span className="text-medium-emphasis">Free</span>
                        )}
                      </CTableDataCell>
                      <CTableDataCell>
                        <CButtonGroup size="sm">
                          <CButton
                            color="outline-primary"
                            size="sm"
                            onClick={() => handleViewResource(resource)}
                          >
                            <FontAwesomeIcon icon={faEye} />
                          </CButton>
                          <CButton color="outline-secondary" size="sm">
                            <FontAwesomeIcon icon={faEdit} />
                          </CButton>
                          <CButton color="outline-danger" size="sm">
                            <FontAwesomeIcon icon={faTrash} />
                          </CButton>
                        </CButtonGroup>
                      </CTableDataCell>
                    </CTableRow>
                  ))}
                  {filteredResources.length === 0 && (
                    <CTableRow>
                      <CTableDataCell colSpan={7} className="text-center py-4">
                        <div className="text-medium-emphasis">
                          <FontAwesomeIcon icon={faCogs} size="2x" className="mb-2" />
                          <p>No resources found matching your criteria</p>
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

export default ResourceManagement;