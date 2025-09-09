import React, { useState, useEffect } from 'react';
import {
  CRow,
  CCol,
  CCard,
  CCardBody,
  CCardHeader,
  CBadge,
  CTable,
  CTableRow,
  CTableBody,
  CTableDataCell,
  CWidgetStatsF,
  CProgress
} from '@coreui/react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faBuilding,
  faCogs,
  faUsers,
  faCar,
  faUtensils,
  faWifi,
  faMapMarkerAlt,
  faClock,
  faCalendarAlt,
  faDollarSign,
  faTools,
  faPhone,
  faEnvelope,
  faUser,
  faChartLine,
  faTag,
  faInfoCircle,
  faTimes,
  faCheckCircle,
  faExclamationTriangle,
  faWrench
} from '@fortawesome/free-solid-svg-icons';
import apiClient from '../api/apiClient';

interface ResourceDetailModalProps {
  resourceId: string | null;
  visible: boolean;
  onClose: () => void;
}

interface ResourceDetail {
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
  daysSinceLastMaintenance?: number;
  daysUntilMaintenance?: number;
  contactPerson?: string;
  contactEmail?: string;
  contactPhone?: string;
  departmentOwner?: string;
  totalUsageHours: number;
  usageThisMonth: number;
  lastUsedAt?: string;
  utilizationRate: number;
  tags: string[];
  category?: string;
  isAvailable: boolean;
  currentBookings: number;
  upcomingBookings: number;
}

const ResourceDetailModal: React.FC<ResourceDetailModalProps> = ({
  resourceId,
  visible,
  onClose,
}) => {
  const [resource, setResource] = useState<ResourceDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (visible && resourceId) {
      fetchResourceDetails(resourceId);
    }
  }, [visible, resourceId]);

  const fetchResourceDetails = async (id: string) => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await apiClient.get(`/simple-resources/${id}`);
      setResource(response.data);
    } catch (error: any) {
      console.error('Failed to fetch resource details:', error);
      const errorMessage = error.response?.data?.message || 'Failed to load resource details. Please try again.';
      setError(errorMessage);
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
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getMaintenanceStatus = (resource: ResourceDetail) => {
    if (resource.daysUntilMaintenance !== undefined) {
      if (resource.daysUntilMaintenance <= 0) {
        return { color: 'danger', text: 'Overdue', icon: faExclamationTriangle };
      } else if (resource.daysUntilMaintenance <= 7) {
        return { color: 'warning', text: 'Due Soon', icon: faWrench };
      } else {
        return { color: 'success', text: 'On Track', icon: faCheckCircle };
      }
    }
    return { color: 'secondary', text: 'Unknown', icon: faInfoCircle };
  };

  if (!visible) return null;

  return (
    <div 
      className={`modal fade ${visible ? 'show d-block' : ''}`} 
      style={{ backgroundColor: visible ? 'rgba(0,0,0,0.5)' : 'transparent' }}
      onClick={onClose}
    >
      <div className="modal-dialog modal-xl" onClick={(e) => e.stopPropagation()}>
        <div className="modal-content">
          <div className="modal-header">
            <h5 className="modal-title">
              <div className="d-flex align-items-center">
                {resource && (
                  <div className="me-2 text-primary">
                    {getTypeIcon(resource.type)}
                  </div>
                )}
                Resource Details
              </div>
            </h5>
            <button 
              type="button" 
              className="btn-close" 
              onClick={onClose}
              aria-label="Close"
            ></button>
          </div>
          
          <div className="modal-body">
        {loading && (
          <div className="d-flex justify-content-center align-items-center py-5">
            <div className="spinner-border text-primary me-3" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
            <span>Loading resource details...</span>
          </div>
        )}
        
        {error && (
          <div className="alert alert-danger mb-4" role="alert">
            <FontAwesomeIcon icon={faExclamationTriangle} className="me-2" />
            {error}
          </div>
        )}
        
        {resource && (
          <div>
            {/* Header Information */}
            <CRow className="mb-4">
              <CCol>
                <CCard>
                  <CCardBody>
                    <div className="d-flex justify-content-between align-items-start">
                      <div>
                        <h3 className="fw-bold mb-2">{resource.name}</h3>
                        <div className="mb-2">
                          <CBadge color="light" className="me-2">{resource.type}</CBadge>
                          {getStatusBadge(resource.status)}
                          {resource.category && (
                            <CBadge color="outline-secondary" className="ms-2">
                              {resource.category}
                            </CBadge>
                          )}
                        </div>
                        {resource.description && (
                          <p className="text-muted mb-3">{resource.description}</p>
                        )}
                      </div>
                      <div className="text-end">
                        {resource.isAvailable ? (
                          <CBadge color="success" className="fs-6">
                            <FontAwesomeIcon icon={faCheckCircle} className="me-1" />
                            Available
                          </CBadge>
                        ) : (
                          <CBadge color="danger" className="fs-6">
                            <FontAwesomeIcon icon={faTimes} className="me-1" />
                            Not Available
                          </CBadge>
                        )}
                      </div>
                    </div>
                  </CCardBody>
                </CCard>
              </CCol>
            </CRow>

            {/* Stats Cards */}
            <CRow className="mb-4">
              <CCol sm={6} lg={3}>
                <CWidgetStatsF
                  className="mb-3"
                  icon={<FontAwesomeIcon icon={faChartLine} size="xl" />}
                  title="Utilization Rate"
                  value={`${resource.utilizationRate.toFixed(1)}%`}
                  color="primary"
                />
              </CCol>
              <CCol sm={6} lg={3}>
                <CWidgetStatsF
                  className="mb-3"
                  icon={<FontAwesomeIcon icon={faClock} size="xl" />}
                  title="Usage This Month"
                  value={`${resource.usageThisMonth}h`}
                  color="success"
                />
              </CCol>
              <CCol sm={6} lg={3}>
                <CWidgetStatsF
                  className="mb-3"
                  icon={<FontAwesomeIcon icon={faCalendarAlt} size="xl" />}
                  title="Current Bookings"
                  value={resource.currentBookings.toString()}
                  color="warning"
                />
              </CCol>
              <CCol sm={6} lg={3}>
                <CWidgetStatsF
                  className="mb-3"
                  icon={<FontAwesomeIcon icon={faCalendarAlt} size="xl" />}
                  title="Upcoming Bookings"
                  value={resource.upcomingBookings.toString()}
                  color="info"
                />
              </CCol>
            </CRow>

            <CRow>
              {/* Basic Information */}
              <CCol md={6}>
                <CCard className="mb-4">
                  <CCardHeader>
                    <h5 className="mb-0">
                      <FontAwesomeIcon icon={faInfoCircle} className="me-2" />
                      Basic Information
                    </h5>
                  </CCardHeader>
                  <CCardBody>
                    <CTable responsive>
                      <CTableBody>
                        <CTableRow>
                          <CTableDataCell className="fw-semibold">Type</CTableDataCell>
                          <CTableDataCell>
                            <div className="d-flex align-items-center">
                              {getTypeIcon(resource.type)}
                              <span className="ms-2">{resource.type}</span>
                            </div>
                          </CTableDataCell>
                        </CTableRow>
                        <CTableRow>
                          <CTableDataCell className="fw-semibold">Status</CTableDataCell>
                          <CTableDataCell>{getStatusBadge(resource.status)}</CTableDataCell>
                        </CTableRow>
                        {resource.capacity && (
                          <CTableRow>
                            <CTableDataCell className="fw-semibold">Capacity</CTableDataCell>
                            <CTableDataCell>
                              <FontAwesomeIcon icon={faUsers} className="me-1 text-muted" />
                              {resource.capacity}
                            </CTableDataCell>
                          </CTableRow>
                        )}
                        {resource.building && (
                          <CTableRow>
                            <CTableDataCell className="fw-semibold">Location</CTableDataCell>
                            <CTableDataCell>
                              <FontAwesomeIcon icon={faMapMarkerAlt} className="me-1 text-muted" />
                              {resource.building}
                              {resource.floor && `, ${resource.floor}`}
                              {resource.location && (
                                <div>
                                  <small className="text-muted">{resource.location}</small>
                                </div>
                              )}
                            </CTableDataCell>
                          </CTableRow>
                        )}
                        {resource.specifications && (
                          <CTableRow>
                            <CTableDataCell className="fw-semibold">Specifications</CTableDataCell>
                            <CTableDataCell>{resource.specifications}</CTableDataCell>
                          </CTableRow>
                        )}
                        {resource.tags.length > 0 && (
                          <CTableRow>
                            <CTableDataCell className="fw-semibold">Tags</CTableDataCell>
                            <CTableDataCell>
                              {resource.tags.map((tag, index) => (
                                <CBadge key={index} color="light" className="me-1">
                                  <FontAwesomeIcon icon={faTag} className="me-1" />
                                  {tag}
                                </CBadge>
                              ))}
                            </CTableDataCell>
                          </CTableRow>
                        )}
                      </CTableBody>
                    </CTable>
                  </CCardBody>
                </CCard>
              </CCol>

              {/* Booking & Cost Information */}
              <CCol md={6}>
                <CCard className="mb-4">
                  <CCardHeader>
                    <h5 className="mb-0">
                      <FontAwesomeIcon icon={faDollarSign} className="me-2" />
                      Booking & Pricing
                    </h5>
                  </CCardHeader>
                  <CCardBody>
                    <CTable responsive>
                      <CTableBody>
                        <CTableRow>
                          <CTableDataCell className="fw-semibold">Bookable</CTableDataCell>
                          <CTableDataCell>
                            {resource.isBookable ? (
                              <CBadge color="success">Yes</CBadge>
                            ) : (
                              <CBadge color="secondary">No</CBadge>
                            )}
                          </CTableDataCell>
                        </CTableRow>
                        <CTableRow>
                          <CTableDataCell className="fw-semibold">Requires Approval</CTableDataCell>
                          <CTableDataCell>
                            {resource.requiresApproval ? (
                              <CBadge color="warning">Yes</CBadge>
                            ) : (
                              <CBadge color="success">No</CBadge>
                            )}
                          </CTableDataCell>
                        </CTableRow>
                        <CTableRow>
                          <CTableDataCell className="fw-semibold">Lead Time</CTableDataCell>
                          <CTableDataCell>
                            <FontAwesomeIcon icon={faClock} className="me-1 text-muted" />
                            {resource.bookingLeadTimeHours} hours
                          </CTableDataCell>
                        </CTableRow>
                        {resource.maxBookingDurationHours && (
                          <CTableRow>
                            <CTableDataCell className="fw-semibold">Max Duration</CTableDataCell>
                            <CTableDataCell>
                              <FontAwesomeIcon icon={faClock} className="me-1 text-muted" />
                              {resource.maxBookingDurationHours} hours
                            </CTableDataCell>
                          </CTableRow>
                        )}
                        {resource.hourlyRate && (
                          <CTableRow>
                            <CTableDataCell className="fw-semibold">Hourly Rate</CTableDataCell>
                            <CTableDataCell className="fw-semibold text-success">
                              {formatCurrency(resource.hourlyRate)}
                            </CTableDataCell>
                          </CTableRow>
                        )}
                        {resource.dailyRate && (
                          <CTableRow>
                            <CTableDataCell className="fw-semibold">Daily Rate</CTableDataCell>
                            <CTableDataCell className="fw-semibold text-success">
                              {formatCurrency(resource.dailyRate)}
                            </CTableDataCell>
                          </CTableRow>
                        )}
                        {resource.setupCost && (
                          <CTableRow>
                            <CTableDataCell className="fw-semibold">Setup Cost</CTableDataCell>
                            <CTableDataCell>{formatCurrency(resource.setupCost)}</CTableDataCell>
                          </CTableRow>
                        )}
                        {resource.cleanupCost && (
                          <CTableRow>
                            <CTableDataCell className="fw-semibold">Cleanup Cost</CTableDataCell>
                            <CTableDataCell>{formatCurrency(resource.cleanupCost)}</CTableDataCell>
                          </CTableRow>
                        )}
                      </CTableBody>
                    </CTable>
                  </CCardBody>
                </CCard>
              </CCol>
            </CRow>

            <CRow>
              {/* Equipment Details */}
              {(resource.serialNumber || resource.model || resource.manufacturer) && (
                <CCol md={6}>
                  <CCard className="mb-4">
                    <CCardHeader>
                      <h5 className="mb-0">
                        <FontAwesomeIcon icon={faCogs} className="me-2" />
                        Equipment Details
                      </h5>
                    </CCardHeader>
                    <CCardBody>
                      <CTable responsive>
                        <CTableBody>
                          {resource.manufacturer && (
                            <CTableRow>
                              <CTableDataCell className="fw-semibold">Manufacturer</CTableDataCell>
                              <CTableDataCell>{resource.manufacturer}</CTableDataCell>
                            </CTableRow>
                          )}
                          {resource.model && (
                            <CTableRow>
                              <CTableDataCell className="fw-semibold">Model</CTableDataCell>
                              <CTableDataCell>{resource.model}</CTableDataCell>
                            </CTableRow>
                          )}
                          {resource.serialNumber && (
                            <CTableRow>
                              <CTableDataCell className="fw-semibold">Serial Number</CTableDataCell>
                              <CTableDataCell>
                                <code>{resource.serialNumber}</code>
                              </CTableDataCell>
                            </CTableRow>
                          )}
                        </CTableBody>
                      </CTable>
                    </CCardBody>
                  </CCard>
                </CCol>
              )}

              {/* Maintenance Information */}
              <CCol md={6}>
                <CCard className="mb-4">
                  <CCardHeader>
                    <h5 className="mb-0">
                      <FontAwesomeIcon icon={faTools} className="me-2" />
                      Maintenance
                    </h5>
                  </CCardHeader>
                  <CCardBody>
                    {resource.lastMaintenanceDate || resource.nextMaintenanceDate ? (
                      <CTable responsive>
                        <CTableBody>
                          {resource.lastMaintenanceDate && (
                            <CTableRow>
                              <CTableDataCell className="fw-semibold">Last Maintenance</CTableDataCell>
                              <CTableDataCell>
                                {formatDate(resource.lastMaintenanceDate)}
                                {resource.daysSinceLastMaintenance && (
                                  <div>
                                    <small className="text-muted">
                                      {resource.daysSinceLastMaintenance} days ago
                                    </small>
                                  </div>
                                )}
                              </CTableDataCell>
                            </CTableRow>
                          )}
                          {resource.nextMaintenanceDate && (
                            <CTableRow>
                              <CTableDataCell className="fw-semibold">Next Maintenance</CTableDataCell>
                              <CTableDataCell>
                                <div className="d-flex align-items-center">
                                  <span className="me-2">{formatDate(resource.nextMaintenanceDate)}</span>
                                  {(() => {
                                    const maintenanceStatus = getMaintenanceStatus(resource);
                                    return (
                                      <CBadge color={maintenanceStatus.color}>
                                        <FontAwesomeIcon icon={maintenanceStatus.icon} className="me-1" />
                                        {maintenanceStatus.text}
                                      </CBadge>
                                    );
                                  })()}
                                </div>
                                {resource.daysUntilMaintenance !== undefined && (
                                  <div>
                                    <small className="text-muted">
                                      {resource.daysUntilMaintenance > 0 
                                        ? `in ${resource.daysUntilMaintenance} days`
                                        : `${Math.abs(resource.daysUntilMaintenance)} days overdue`
                                      }
                                    </small>
                                  </div>
                                )}
                              </CTableDataCell>
                            </CTableRow>
                          )}
                          {resource.maintenanceNotes && (
                            <CTableRow>
                              <CTableDataCell className="fw-semibold">Notes</CTableDataCell>
                              <CTableDataCell>
                                <small className="text-muted">{resource.maintenanceNotes}</small>
                              </CTableDataCell>
                            </CTableRow>
                          )}
                        </CTableBody>
                      </CTable>
                    ) : (
                      <p className="text-muted mb-0">No maintenance information available</p>
                    )}
                  </CCardBody>
                </CCard>
              </CCol>
            </CRow>

            {/* Contact & Usage Information */}
            <CRow>
              {(resource.contactPerson || resource.contactEmail || resource.contactPhone || resource.departmentOwner) && (
                <CCol md={6}>
                  <CCard className="mb-4">
                    <CCardHeader>
                      <h5 className="mb-0">
                        <FontAwesomeIcon icon={faUser} className="me-2" />
                        Contact Information
                      </h5>
                    </CCardHeader>
                    <CCardBody>
                      <CTable responsive>
                        <CTableBody>
                          {resource.contactPerson && (
                            <CTableRow>
                              <CTableDataCell className="fw-semibold">Contact Person</CTableDataCell>
                              <CTableDataCell>
                                <FontAwesomeIcon icon={faUser} className="me-1 text-muted" />
                                {resource.contactPerson}
                              </CTableDataCell>
                            </CTableRow>
                          )}
                          {resource.contactEmail && (
                            <CTableRow>
                              <CTableDataCell className="fw-semibold">Email</CTableDataCell>
                              <CTableDataCell>
                                <FontAwesomeIcon icon={faEnvelope} className="me-1 text-muted" />
                                <a href={`mailto:${resource.contactEmail}`}>
                                  {resource.contactEmail}
                                </a>
                              </CTableDataCell>
                            </CTableRow>
                          )}
                          {resource.contactPhone && (
                            <CTableRow>
                              <CTableDataCell className="fw-semibold">Phone</CTableDataCell>
                              <CTableDataCell>
                                <FontAwesomeIcon icon={faPhone} className="me-1 text-muted" />
                                <a href={`tel:${resource.contactPhone}`}>
                                  {resource.contactPhone}
                                </a>
                              </CTableDataCell>
                            </CTableRow>
                          )}
                          {resource.departmentOwner && (
                            <CTableRow>
                              <CTableDataCell className="fw-semibold">Department</CTableDataCell>
                              <CTableDataCell>{resource.departmentOwner}</CTableDataCell>
                            </CTableRow>
                          )}
                        </CTableBody>
                      </CTable>
                    </CCardBody>
                  </CCard>
                </CCol>
              )}

              {/* Usage Statistics */}
              <CCol md={6}>
                <CCard className="mb-4">
                  <CCardHeader>
                    <h5 className="mb-0">
                      <FontAwesomeIcon icon={faChartLine} className="me-2" />
                      Usage Statistics
                    </h5>
                  </CCardHeader>
                  <CCardBody>
                    <div className="mb-3">
                      <div className="d-flex justify-content-between align-items-center mb-2">
                        <span className="fw-semibold">Utilization Rate</span>
                        <span>{resource.utilizationRate.toFixed(1)}%</span>
                      </div>
                      <CProgress 
                        value={resource.utilizationRate} 
                        color={resource.utilizationRate > 80 ? 'danger' : resource.utilizationRate > 60 ? 'warning' : 'success'}
                      />
                    </div>
                    <CTable responsive>
                      <CTableBody>
                        <CTableRow>
                          <CTableDataCell className="fw-semibold">Total Usage</CTableDataCell>
                          <CTableDataCell>{resource.totalUsageHours} hours</CTableDataCell>
                        </CTableRow>
                        <CTableRow>
                          <CTableDataCell className="fw-semibold">This Month</CTableDataCell>
                          <CTableDataCell>{resource.usageThisMonth} hours</CTableDataCell>
                        </CTableRow>
                        {resource.lastUsedAt && (
                          <CTableRow>
                            <CTableDataCell className="fw-semibold">Last Used</CTableDataCell>
                            <CTableDataCell>{formatDateTime(resource.lastUsedAt)}</CTableDataCell>
                          </CTableRow>
                        )}
                      </CTableBody>
                    </CTable>
                  </CCardBody>
                </CCard>
              </CCol>
            </CRow>
          </div>
        )}
          </div>
          
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Close
            </button>
            {resource && resource.isBookable && (
              <button type="button" className="btn btn-primary">
                <FontAwesomeIcon icon={faCalendarAlt} className="me-2" />
                Book Resource
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ResourceDetailModal;