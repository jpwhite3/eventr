import React, { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { ChangePasswordData } from '../services/AuthService';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faKey,
  faEye,
  faEyeSlash,
  faSave,
  faShieldAlt,
  faBell,
  faGlobe,
  faPalette,
  faTrash,
  faExclamationTriangle,
  faCheckCircle,
  faSpinner,
  faLock,
  faUserShield
} from '@fortawesome/free-solid-svg-icons';
import './UserSettingsPage.css';

const UserSettingsPage: React.FC = () => {
  const { user, changePassword, isLoading, error, clearError } = useAuth();
  
  // Password change form state
  const [passwordData, setPasswordData] = useState<ChangePasswordData>({
    currentPassword: '',
    newPassword: ''
  });
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  
  // Local state for messages
  const [localError, setLocalError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    if (name === 'confirmPassword') {
      setConfirmPassword(value);
    } else {
      setPasswordData(prev => ({ ...prev, [name]: value }));
    }
    
    // Clear errors when user starts typing
    if (error) clearError();
    if (localError) setLocalError(null);
  };

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Validation
    if (passwordData.newPassword !== confirmPassword) {
      setLocalError('New passwords do not match.');
      return;
    }
    
    if (passwordData.newPassword.length < 8) {
      setLocalError('New password must be at least 8 characters long.');
      return;
    }
    
    if (passwordData.currentPassword === passwordData.newPassword) {
      setLocalError('New password must be different from current password.');
      return;
    }
    
    try {
      setLocalError(null);
      setSuccessMessage(null);
      
      await changePassword(passwordData);
      
      // Clear form on success
      setPasswordData({ currentPassword: '', newPassword: '' });
      setConfirmPassword('');
      setSuccessMessage('Password changed successfully!');
      
      // Clear success message after 5 seconds
      setTimeout(() => {
        setSuccessMessage(null);
      }, 5000);
      
    } catch (error: any) {
      setLocalError(error.message || 'Failed to change password. Please try again.');
    }
  };

  const getPasswordStrength = (password: string): { score: number; label: string; color: string } => {
    let score = 0;
    let label = 'Weak';
    let color = 'danger';
    
    if (password.length >= 8) score++;
    if (/[a-z]/.test(password)) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/\d/.test(password)) score++;
    if (/[^a-zA-Z0-9]/.test(password)) score++;
    
    if (score >= 4) {
      label = 'Strong';
      color = 'success';
    } else if (score >= 3) {
      label = 'Medium';
      color = 'warning';
    }
    
    return { score, label, color };
  };

  const passwordStrength = getPasswordStrength(passwordData.newPassword);

  if (!user) {
    return (
      <div className="settings-container">
        <div className="loading-state">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="mt-3">Loading your settings...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="settings-container">
      {/* Header */}
      <div className="settings-header">
        <div className="header-content">
          <h1>Account Settings</h1>
          <p className="text-muted">
            Manage your account security, preferences, and privacy settings
          </p>
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
        {/* Security Settings */}
        <div className="col-md-8">
          {/* Change Password */}
          <div className="card mb-4">
            <div className="card-header">
              <h5 className="mb-0">
                <FontAwesomeIcon icon={faKey} className="me-2" />
                Change Password
              </h5>
            </div>
            <div className="card-body">
              <form onSubmit={handlePasswordSubmit}>
                <div className="mb-3">
                  <label htmlFor="currentPassword" className="form-label">
                    Current Password *
                  </label>
                  <div className="input-group">
                    <input
                      type={showCurrentPassword ? 'text' : 'password'}
                      className="form-control"
                      id="currentPassword"
                      name="currentPassword"
                      value={passwordData.currentPassword}
                      onChange={handlePasswordChange}
                      required
                      autoComplete="current-password"
                    />
                    <button
                      type="button"
                      className="btn btn-outline-secondary"
                      onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                    >
                      <FontAwesomeIcon icon={showCurrentPassword ? faEyeSlash : faEye} />
                    </button>
                  </div>
                </div>

                <div className="mb-3">
                  <label htmlFor="newPassword" className="form-label">
                    New Password *
                  </label>
                  <div className="input-group">
                    <input
                      type={showNewPassword ? 'text' : 'password'}
                      className="form-control"
                      id="newPassword"
                      name="newPassword"
                      value={passwordData.newPassword}
                      onChange={handlePasswordChange}
                      required
                      minLength={8}
                      autoComplete="new-password"
                    />
                    <button
                      type="button"
                      className="btn btn-outline-secondary"
                      onClick={() => setShowNewPassword(!showNewPassword)}
                    >
                      <FontAwesomeIcon icon={showNewPassword ? faEyeSlash : faEye} />
                    </button>
                  </div>
                  
                  {/* Password Strength Indicator */}
                  {passwordData.newPassword && (
                    <div className="password-strength mt-2">
                      <div className="d-flex justify-content-between align-items-center mb-1">
                        <small className="text-muted">Password Strength:</small>
                        <small className={`text-${passwordStrength.color} fw-bold`}>
                          {passwordStrength.label}
                        </small>
                      </div>
                      <div className="progress" style={{ height: '4px' }}>
                        <div
                          className={`progress-bar bg-${passwordStrength.color}`}
                          role="progressbar"
                          style={{ width: `${(passwordStrength.score / 5) * 100}%` }}
                        />
                      </div>
                      <small className="text-muted">
                        Password should contain uppercase, lowercase, numbers, and special characters
                      </small>
                    </div>
                  )}
                </div>

                <div className="mb-3">
                  <label htmlFor="confirmPassword" className="form-label">
                    Confirm New Password *
                  </label>
                  <div className="input-group">
                    <input
                      type={showConfirmPassword ? 'text' : 'password'}
                      className="form-control"
                      id="confirmPassword"
                      name="confirmPassword"
                      value={confirmPassword}
                      onChange={handlePasswordChange}
                      required
                      autoComplete="new-password"
                    />
                    <button
                      type="button"
                      className="btn btn-outline-secondary"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    >
                      <FontAwesomeIcon icon={showConfirmPassword ? faEyeSlash : faEye} />
                    </button>
                  </div>
                  {confirmPassword && passwordData.newPassword !== confirmPassword && (
                    <small className="text-danger">Passwords do not match</small>
                  )}
                </div>

                <div className="d-grid">
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={
                      isLoading ||
                      !passwordData.currentPassword ||
                      !passwordData.newPassword ||
                      !confirmPassword ||
                      passwordData.newPassword !== confirmPassword
                    }
                  >
                    {isLoading ? (
                      <>
                        <FontAwesomeIcon icon={faSpinner} spin className="me-2" />
                        Changing Password...
                      </>
                    ) : (
                      <>
                        <FontAwesomeIcon icon={faSave} className="me-2" />
                        Change Password
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>

          {/* Privacy & Data */}
          <div className="card mb-4">
            <div className="card-header">
              <h5 className="mb-0">
                <FontAwesomeIcon icon={faShieldAlt} className="me-2" />
                Privacy & Data
              </h5>
            </div>
            <div className="card-body">
              <div className="setting-item">
                <div className="setting-info">
                  <h6>Data Export</h6>
                  <p className="text-muted mb-2">
                    Download a copy of your data including profile information and event history.
                  </p>
                </div>
                <button className="btn btn-outline-primary btn-sm">
                  Request Export
                </button>
              </div>
              
              <hr />
              
              <div className="setting-item">
                <div className="setting-info">
                  <h6 className="text-danger">
                    <FontAwesomeIcon icon={faTrash} className="me-2" />
                    Delete Account
                  </h6>
                  <p className="text-muted mb-2">
                    Permanently delete your account and all associated data. This action cannot be undone.
                  </p>
                </div>
                <button className="btn btn-outline-danger btn-sm">
                  Delete Account
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Account Overview */}
        <div className="col-md-4">
          {/* Security Overview */}
          <div className="card mb-4">
            <div className="card-header">
              <h5 className="mb-0">
                <FontAwesomeIcon icon={faUserShield} className="me-2" />
                Security Overview
              </h5>
            </div>
            <div className="card-body">
              <div className="security-item">
                <div className="d-flex align-items-center mb-3">
                  <div className="security-icon me-3">
                    <FontAwesomeIcon icon={faLock} className="text-success" />
                  </div>
                  <div>
                    <h6 className="mb-1">Password</h6>
                    <small className="text-muted">Strong password set</small>
                  </div>
                </div>
              </div>
              
              <div className="security-item">
                <div className="d-flex align-items-center mb-3">
                  <div className="security-icon me-3">
                    <FontAwesomeIcon 
                      icon={faCheckCircle} 
                      className={user.emailVerified ? 'text-success' : 'text-warning'} 
                    />
                  </div>
                  <div>
                    <h6 className="mb-1">Email Verification</h6>
                    <small className={`text-${user.emailVerified ? 'success' : 'warning'}`}>
                      {user.emailVerified ? 'Verified' : 'Not verified'}
                    </small>
                  </div>
                </div>
              </div>
              
              <div className="security-tip">
                <h6>Security Tip</h6>
                <p className="text-muted small">
                  Use a unique, strong password and keep your email verified to ensure account security.
                </p>
              </div>
            </div>
          </div>

          {/* Quick Actions */}
          <div className="card">
            <div className="card-header">
              <h5 className="mb-0">Quick Actions</h5>
            </div>
            <div className="card-body">
              <div className="d-grid gap-2">
                <button className="btn btn-outline-primary btn-sm">
                  <FontAwesomeIcon icon={faBell} className="me-2" />
                  Notification Settings
                </button>
                
                <button className="btn btn-outline-secondary btn-sm">
                  <FontAwesomeIcon icon={faGlobe} className="me-2" />
                  Language & Region
                </button>
                
                <button className="btn btn-outline-info btn-sm">
                  <FontAwesomeIcon icon={faPalette} className="me-2" />
                  Theme Preferences
                </button>
              </div>
              
              <hr />
              
              <div className="text-center">
                <small className="text-muted">
                  Need help? <a href="/support" className="text-decoration-none">Contact Support</a>
                </small>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default UserSettingsPage;