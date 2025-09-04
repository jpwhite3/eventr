import React, { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import { UpdateProfileData } from '../services/AuthService';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faUser,
  faEdit,
  faSave,
  faTimes,
  faEnvelope,
  faPhone,
  faBuilding,
  faBriefcase,
  faClock,
  faCalendarAlt,
  faCheckCircle,
  faExclamationTriangle,
  faSpinner
} from '@fortawesome/free-solid-svg-icons';
import './UserProfilePage.css';

const UserProfilePage: React.FC = () => {
  const { user, updateProfile, isLoading, error, clearError } = useAuth();
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState<UpdateProfileData>({});
  const [localError, setLocalError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    if (user) {
      setFormData({
        firstName: user.firstName,
        lastName: user.lastName,
        phone: user.phone || '',
        company: user.company || '',
        jobTitle: user.jobTitle || '',
        bio: user.bio || '',
        timezone: user.timezone || '',
        language: user.language || 'en',
        marketingEmails: user.marketingEmails,
        eventReminders: user.eventReminders,
        weeklyDigest: user.weeklyDigest
      });
    }
  }, [user]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    if (type === 'checkbox') {
      const checked = (e.target as HTMLInputElement).checked;
      setFormData(prev => ({ ...prev, [name]: checked }));
    } else {
      setFormData(prev => ({ ...prev, [name]: value }));
    }
    
    // Clear errors when user starts typing
    if (error) clearError();
    if (localError) setLocalError(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    try {
      setLocalError(null);
      setSuccessMessage(null);
      
      // Remove empty strings and convert to null for optional fields
      const cleanedData: UpdateProfileData = {};
      Object.entries(formData).forEach(([key, value]) => {
        if (value !== '' && value !== undefined) {
          cleanedData[key as keyof UpdateProfileData] = value as any;
        }
      });
      
      await updateProfile(cleanedData);
      setSuccessMessage('Profile updated successfully!');
      setIsEditing(false);
      
      // Clear success message after 3 seconds
      setTimeout(() => {
        setSuccessMessage(null);
      }, 3000);
      
    } catch (error: any) {
      setLocalError(error.message || 'Failed to update profile. Please try again.');
    }
  };

  const handleCancel = () => {
    if (user) {
      setFormData({
        firstName: user.firstName,
        lastName: user.lastName,
        phone: user.phone || '',
        company: user.company || '',
        jobTitle: user.jobTitle || '',
        bio: user.bio || '',
        timezone: user.timezone || '',
        language: user.language || 'en',
        marketingEmails: user.marketingEmails,
        eventReminders: user.eventReminders,
        weeklyDigest: user.weeklyDigest
      });
    }
    setIsEditing(false);
    setLocalError(null);
    if (error) clearError();
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (!user) {
    return (
      <div className="profile-container">
        <div className="loading-state">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="mt-3">Loading your profile...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="profile-container">
      {/* Header */}
      <div className="profile-header">
        <div className="header-content">
          <h1>My Profile</h1>
          <p className="text-muted">
            Manage your personal information and account details
          </p>
        </div>
        <div className="header-actions">
          {!isEditing ? (
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => setIsEditing(true)}
            >
              <FontAwesomeIcon icon={faEdit} className="me-2" />
              Edit Profile
            </button>
          ) : (
            <div className="edit-actions">
              <button
                type="button"
                className="btn btn-outline-secondary me-2"
                onClick={handleCancel}
                disabled={isLoading}
              >
                <FontAwesomeIcon icon={faTimes} className="me-2" />
                Cancel
              </button>
              <button
                type="submit"
                form="profile-form"
                className="btn btn-primary"
                disabled={isLoading}
              >
                {isLoading ? (
                  <>
                    <FontAwesomeIcon icon={faSpinner} spin className="me-2" />
                    Saving...
                  </>
                ) : (
                  <>
                    <FontAwesomeIcon icon={faSave} className="me-2" />
                    Save Changes
                  </>
                )}
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Success Message */}
      {successMessage && (
        <div className="alert alert-success mb-4" role="alert">
          <FontAwesomeIcon icon={faCheckCircle} className="me-2" />
          {successMessage}
        </div>
      )}

      {/* Error Message */}
      {(error || localError) && (
        <div className="alert alert-danger mb-4" role="alert">
          <FontAwesomeIcon icon={faExclamationTriangle} className="me-2" />
          {error || localError}
        </div>
      )}

      <div className="row">
        {/* Profile Information */}
        <div className="col-md-8">
          <div className="card">
            <div className="card-header">
              <h5 className="mb-0">
                <FontAwesomeIcon icon={faUser} className="me-2" />
                Profile Information
              </h5>
            </div>
            <div className="card-body">
              <form id="profile-form" onSubmit={handleSubmit}>
                <div className="row">
                  <div className="col-md-6">
                    <div className="mb-3">
                      <label htmlFor="firstName" className="form-label">
                        First Name *
                      </label>
                      {isEditing ? (
                        <input
                          type="text"
                          className="form-control"
                          id="firstName"
                          name="firstName"
                          value={formData.firstName || ''}
                          onChange={handleInputChange}
                          required
                        />
                      ) : (
                        <p className="form-control-plaintext">{user.firstName}</p>
                      )}
                    </div>
                  </div>
                  <div className="col-md-6">
                    <div className="mb-3">
                      <label htmlFor="lastName" className="form-label">
                        Last Name *
                      </label>
                      {isEditing ? (
                        <input
                          type="text"
                          className="form-control"
                          id="lastName"
                          name="lastName"
                          value={formData.lastName || ''}
                          onChange={handleInputChange}
                          required
                        />
                      ) : (
                        <p className="form-control-plaintext">{user.lastName}</p>
                      )}
                    </div>
                  </div>
                </div>

                <div className="mb-3">
                  <label htmlFor="email" className="form-label">
                    <FontAwesomeIcon icon={faEnvelope} className="me-2" />
                    Email Address
                  </label>
                  <div className="d-flex align-items-center">
                    <p className="form-control-plaintext mb-0 me-3">{user.email}</p>
                    {user.emailVerified ? (
                      <span className="badge bg-success">
                        <FontAwesomeIcon icon={faCheckCircle} className="me-1" />
                        Verified
                      </span>
                    ) : (
                      <span className="badge bg-warning">
                        <FontAwesomeIcon icon={faExclamationTriangle} className="me-1" />
                        Not Verified
                      </span>
                    )}
                  </div>
                  <small className="text-muted">
                    Email address cannot be changed from this page. Contact support if needed.
                  </small>
                </div>

                <div className="row">
                  <div className="col-md-6">
                    <div className="mb-3">
                      <label htmlFor="phone" className="form-label">
                        <FontAwesomeIcon icon={faPhone} className="me-2" />
                        Phone Number
                      </label>
                      {isEditing ? (
                        <input
                          type="tel"
                          className="form-control"
                          id="phone"
                          name="phone"
                          value={formData.phone || ''}
                          onChange={handleInputChange}
                          placeholder="(555) 123-4567"
                        />
                      ) : (
                        <p className="form-control-plaintext">
                          {user.phone || <span className="text-muted">Not provided</span>}
                        </p>
                      )}
                    </div>
                  </div>
                  <div className="col-md-6">
                    <div className="mb-3">
                      <label htmlFor="company" className="form-label">
                        <FontAwesomeIcon icon={faBuilding} className="me-2" />
                        Company
                      </label>
                      {isEditing ? (
                        <input
                          type="text"
                          className="form-control"
                          id="company"
                          name="company"
                          value={formData.company || ''}
                          onChange={handleInputChange}
                          placeholder="Your company name"
                        />
                      ) : (
                        <p className="form-control-plaintext">
                          {user.company || <span className="text-muted">Not provided</span>}
                        </p>
                      )}
                    </div>
                  </div>
                </div>

                <div className="row">
                  <div className="col-md-6">
                    <div className="mb-3">
                      <label htmlFor="jobTitle" className="form-label">
                        <FontAwesomeIcon icon={faBriefcase} className="me-2" />
                        Job Title
                      </label>
                      {isEditing ? (
                        <input
                          type="text"
                          className="form-control"
                          id="jobTitle"
                          name="jobTitle"
                          value={formData.jobTitle || ''}
                          onChange={handleInputChange}
                          placeholder="Your job title"
                        />
                      ) : (
                        <p className="form-control-plaintext">
                          {user.jobTitle || <span className="text-muted">Not provided</span>}
                        </p>
                      )}
                    </div>
                  </div>
                  <div className="col-md-6">
                    <div className="mb-3">
                      <label htmlFor="timezone" className="form-label">
                        <FontAwesomeIcon icon={faClock} className="me-2" />
                        Timezone
                      </label>
                      {isEditing ? (
                        <select
                          className="form-select"
                          id="timezone"
                          name="timezone"
                          value={formData.timezone || ''}
                          onChange={handleInputChange}
                        >
                          <option value="">Select timezone</option>
                          <option value="America/New_York">Eastern Time (ET)</option>
                          <option value="America/Chicago">Central Time (CT)</option>
                          <option value="America/Denver">Mountain Time (MT)</option>
                          <option value="America/Los_Angeles">Pacific Time (PT)</option>
                          <option value="UTC">UTC</option>
                          <option value="Europe/London">London (GMT)</option>
                          <option value="Europe/Paris">Paris (CET)</option>
                          <option value="Asia/Tokyo">Tokyo (JST)</option>
                          <option value="Australia/Sydney">Sydney (AEDT)</option>
                        </select>
                      ) : (
                        <p className="form-control-plaintext">
                          {user.timezone || <span className="text-muted">Not set</span>}
                        </p>
                      )}
                    </div>
                  </div>
                </div>

                <div className="mb-3">
                  <label htmlFor="bio" className="form-label">
                    Bio
                  </label>
                  {isEditing ? (
                    <textarea
                      className="form-control"
                      id="bio"
                      name="bio"
                      rows={3}
                      value={formData.bio || ''}
                      onChange={handleInputChange}
                      placeholder="Tell us a bit about yourself..."
                      maxLength={500}
                    />
                  ) : (
                    <p className="form-control-plaintext">
                      {user.bio || <span className="text-muted">No bio provided</span>}
                    </p>
                  )}
                  {isEditing && (
                    <small className="text-muted">
                      {(formData.bio || '').length}/500 characters
                    </small>
                  )}
                </div>
              </form>
            </div>
          </div>
        </div>

        {/* Account Summary */}
        <div className="col-md-4">
          <div className="card">
            <div className="card-header">
              <h5 className="mb-0">Account Summary</h5>
            </div>
            <div className="card-body">
              <div className="account-info">
                <div className="info-item">
                  <strong>Account Status</strong>
                  <span className={`badge bg-${user.status === 'ACTIVE' ? 'success' : 'warning'}`}>
                    {user.status}
                  </span>
                </div>
                
                <div className="info-item">
                  <strong>Role</strong>
                  <span className="badge bg-primary">{user.role}</span>
                </div>

                <div className="info-item">
                  <strong>Member Since</strong>
                  <span className="text-muted">
                    <FontAwesomeIcon icon={faCalendarAlt} className="me-1" />
                    {formatDate(user.createdAt)}
                  </span>
                </div>

                {user.lastLoginAt && (
                  <div className="info-item">
                    <strong>Last Login</strong>
                    <span className="text-muted">
                      <FontAwesomeIcon icon={faClock} className="me-1" />
                      {formatDate(user.lastLoginAt)}
                    </span>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Email Preferences */}
          <div className="card mt-3">
            <div className="card-header">
              <h5 className="mb-0">Email Preferences</h5>
            </div>
            <div className="card-body">
              <form>
                <div className="form-check mb-2">
                  <input
                    className="form-check-input"
                    type="checkbox"
                    id="marketingEmails"
                    name="marketingEmails"
                    checked={isEditing ? (formData.marketingEmails ?? false) : user.marketingEmails}
                    onChange={handleInputChange}
                    disabled={!isEditing}
                  />
                  <label className="form-check-label" htmlFor="marketingEmails">
                    Marketing emails
                  </label>
                </div>
                
                <div className="form-check mb-2">
                  <input
                    className="form-check-input"
                    type="checkbox"
                    id="eventReminders"
                    name="eventReminders"
                    checked={isEditing ? (formData.eventReminders ?? false) : user.eventReminders}
                    onChange={handleInputChange}
                    disabled={!isEditing}
                  />
                  <label className="form-check-label" htmlFor="eventReminders">
                    Event reminders
                  </label>
                </div>
                
                <div className="form-check">
                  <input
                    className="form-check-input"
                    type="checkbox"
                    id="weeklyDigest"
                    name="weeklyDigest"
                    checked={isEditing ? (formData.weeklyDigest ?? false) : user.weeklyDigest}
                    onChange={handleInputChange}
                    disabled={!isEditing}
                  />
                  <label className="form-check-label" htmlFor="weeklyDigest">
                    Weekly digest
                  </label>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default UserProfilePage;