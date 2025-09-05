import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import UserSettingsPage from './UserSettingsPage';
import { useAuth } from '../hooks/useAuth';
import apiClient from '../api/apiClient';

// Mock dependencies
jest.mock('../hooks/useAuth');
jest.mock('../api/apiClient');

const mockUseAuth = useAuth as jest.MockedFunction<typeof useAuth>;
const mockApiClient = apiClient as jest.Mocked<typeof apiClient>;

const mockAuthBase = {
  error: null,
  login: jest.fn(),
  logout: jest.fn(),
  register: jest.fn(),
  updateProfile: jest.fn(),
  changePassword: jest.fn(),
  verifyEmail: jest.fn(),
  forgotPassword: jest.fn(),
  resetPassword: jest.fn(),
  refreshProfile: jest.fn(),
  clearError: jest.fn(),
  hasRole: jest.fn(),
  hasAnyRole: jest.fn(),
  isAdmin: jest.fn(),
  isOrganizer: jest.fn()
};

describe('UserSettingsPage', () => {
  const mockUser = {
    id: 'user-123',
    email: 'test@example.com',
    firstName: 'John',
    lastName: 'Doe',
    role: 'ATTENDEE',
    status: 'ACTIVE',
    emailVerified: true,
    marketingEmails: false,
    eventReminders: true,
    weeklyDigest: true,
    createdAt: '2024-01-01T00:00:00Z'
  };

  beforeEach(() => {
    jest.clearAllMocks();
    
    mockUseAuth.mockReturnValue({
      ...mockAuthBase,
      user: mockUser,
      isAuthenticated: true,
      isLoading: false
    });

    mockApiClient.post.mockResolvedValue({ data: { success: true } });
  });

  it('renders settings page when authenticated', () => {
    render(<UserSettingsPage />);
    
    expect(screen.getByText('Account Settings')).toBeInTheDocument();
    expect(screen.getByText('Security')).toBeInTheDocument();
    expect(screen.getByText('Password')).toBeInTheDocument();
  });

  it('shows not authenticated message when user is not authenticated', () => {
    mockUseAuth.mockReturnValue({
      ...mockAuthBase,
      user: null,
      isAuthenticated: false,
      isLoading: false
    });

    render(<UserSettingsPage />);
    
    expect(screen.getByText('Authentication Required')).toBeInTheDocument();
    expect(screen.getByText('Please sign in to access your settings')).toBeInTheDocument();
  });

  it('shows loading state when auth is loading', () => {
    mockUseAuth.mockReturnValue({
      ...mockAuthBase,
      user: null,
      isAuthenticated: false,
      isLoading: true
    });

    render(<UserSettingsPage />);
    
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  describe('Password Change', () => {
    it('validates current password is required', async () => {
      render(<UserSettingsPage />);
      
      const newPasswordInput = screen.getByLabelText(/new password/i);
      const confirmPasswordInput = screen.getByLabelText(/confirm new password/i);
      
      fireEvent.change(newPasswordInput, { target: { value: 'newpassword123' } });
      fireEvent.change(confirmPasswordInput, { target: { value: 'newpassword123' } });
      
      const changePasswordButton = screen.getByText('Change Password');
      fireEvent.click(changePasswordButton);
      
      await waitFor(() => {
        expect(screen.getByText('Current password is required')).toBeInTheDocument();
      });
      
      expect(mockApiClient.post).not.toHaveBeenCalled();
    });

    it('validates new password meets requirements', async () => {
      render(<UserSettingsPage />);
      
      const currentPasswordInput = screen.getByLabelText(/current password/i);
      const newPasswordInput = screen.getByLabelText(/new password/i);
      
      fireEvent.change(currentPasswordInput, { target: { value: 'oldpassword' } });
      fireEvent.change(newPasswordInput, { target: { value: '123' } }); // Too short
      
      const changePasswordButton = screen.getByText('Change Password');
      fireEvent.click(changePasswordButton);
      
      await waitFor(() => {
        expect(screen.getByText('Password must be at least 8 characters')).toBeInTheDocument();
      });
      
      expect(mockApiClient.post).not.toHaveBeenCalled();
    });

    it('validates password confirmation matches', async () => {
      render(<UserSettingsPage />);
      
      const currentPasswordInput = screen.getByLabelText(/current password/i);
      const newPasswordInput = screen.getByLabelText(/new password/i);
      const confirmPasswordInput = screen.getByLabelText(/confirm new password/i);
      
      fireEvent.change(currentPasswordInput, { target: { value: 'oldpassword' } });
      fireEvent.change(newPasswordInput, { target: { value: 'newpassword123' } });
      fireEvent.change(confirmPasswordInput, { target: { value: 'differentpassword' } });
      
      const changePasswordButton = screen.getByText('Change Password');
      fireEvent.click(changePasswordButton);
      
      await waitFor(() => {
        expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
      });
      
      expect(mockApiClient.post).not.toHaveBeenCalled();
    });

    it('shows password strength indicator', () => {
      render(<UserSettingsPage />);
      
      const newPasswordInput = screen.getByLabelText(/new password/i);
      
      // Weak password
      fireEvent.change(newPasswordInput, { target: { value: 'weak' } });
      expect(screen.getByText('Weak')).toBeInTheDocument();
      
      // Medium password
      fireEvent.change(newPasswordInput, { target: { value: 'Medium123' } });
      expect(screen.getByText('Medium')).toBeInTheDocument();
      
      // Strong password
      fireEvent.change(newPasswordInput, { target: { value: 'Strong123@!' } });
      expect(screen.getByText('Strong')).toBeInTheDocument();
    });

    it('successfully changes password', async () => {
      render(<UserSettingsPage />);
      
      const currentPasswordInput = screen.getByLabelText(/current password/i);
      const newPasswordInput = screen.getByLabelText(/new password/i);
      const confirmPasswordInput = screen.getByLabelText(/confirm new password/i);
      
      fireEvent.change(currentPasswordInput, { target: { value: 'currentpass123' } });
      fireEvent.change(newPasswordInput, { target: { value: 'newpassword123' } });
      fireEvent.change(confirmPasswordInput, { target: { value: 'newpassword123' } });
      
      const changePasswordButton = screen.getByText('Change Password');
      fireEvent.click(changePasswordButton);
      
      await waitFor(() => {
        expect(mockApiClient.post).toHaveBeenCalledWith('/auth/change-password', {
          currentPassword: 'currentpass123',
          newPassword: 'newpassword123'
        });
        expect(screen.getByText('Password changed successfully!')).toBeInTheDocument();
      });
    });

    it('handles password change errors', async () => {
      mockApiClient.post.mockRejectedValue({ 
        response: { data: { message: 'Current password is incorrect' } } 
      });
      
      render(<UserSettingsPage />);
      
      const currentPasswordInput = screen.getByLabelText(/current password/i);
      const newPasswordInput = screen.getByLabelText(/new password/i);
      const confirmPasswordInput = screen.getByLabelText(/confirm new password/i);
      
      fireEvent.change(currentPasswordInput, { target: { value: 'wrongpassword' } });
      fireEvent.change(newPasswordInput, { target: { value: 'newpassword123' } });
      fireEvent.change(confirmPasswordInput, { target: { value: 'newpassword123' } });
      
      const changePasswordButton = screen.getByText('Change Password');
      fireEvent.click(changePasswordButton);
      
      await waitFor(() => {
        expect(screen.getByText('Current password is incorrect')).toBeInTheDocument();
      });
    });

    it('clears form after successful password change', async () => {
      render(<UserSettingsPage />);
      
      const currentPasswordInput = screen.getByLabelText(/current password/i);
      const newPasswordInput = screen.getByLabelText(/new password/i);
      const confirmPasswordInput = screen.getByLabelText(/confirm new password/i);
      
      fireEvent.change(currentPasswordInput, { target: { value: 'currentpass123' } });
      fireEvent.change(newPasswordInput, { target: { value: 'newpassword123' } });
      fireEvent.change(confirmPasswordInput, { target: { value: 'newpassword123' } });
      
      const changePasswordButton = screen.getByText('Change Password');
      fireEvent.click(changePasswordButton);
      
      await waitFor(() => {
        expect(screen.getByText('Password changed successfully!')).toBeInTheDocument();
        expect((currentPasswordInput as HTMLInputElement).value).toBe('');
        expect((newPasswordInput as HTMLInputElement).value).toBe('');
        expect((confirmPasswordInput as HTMLInputElement).value).toBe('');
      });
    });
  });

  describe('Security Overview', () => {
    it('displays security information', () => {
      render(<UserSettingsPage />);
      
      expect(screen.getByText('Security Overview')).toBeInTheDocument();
      expect(screen.getByText('Account Email')).toBeInTheDocument();
      expect(screen.getByText('test@example.com')).toBeInTheDocument();
      expect(screen.getByText('Last Login')).toBeInTheDocument();
      expect(screen.getByText('Password Last Changed')).toBeInTheDocument();
    });

    it('shows password requirements', () => {
      render(<UserSettingsPage />);
      
      expect(screen.getByText('Password Requirements:')).toBeInTheDocument();
      expect(screen.getByText('At least 8 characters')).toBeInTheDocument();
      expect(screen.getByText('At least one lowercase letter')).toBeInTheDocument();
      expect(screen.getByText('At least one uppercase letter')).toBeInTheDocument();
      expect(screen.getByText('At least one number')).toBeInTheDocument();
      expect(screen.getByText('At least one special character')).toBeInTheDocument();
    });
  });

  describe('Form Validation', () => {
    it('shows loading state during password change', async () => {
      let resolvePromise: (value: any) => void;
      mockApiClient.post.mockReturnValue(
        new Promise(resolve => {
          resolvePromise = resolve;
        })
      );
      
      render(<UserSettingsPage />);
      
      const currentPasswordInput = screen.getByLabelText(/current password/i);
      const newPasswordInput = screen.getByLabelText(/new password/i);
      const confirmPasswordInput = screen.getByLabelText(/confirm new password/i);
      
      fireEvent.change(currentPasswordInput, { target: { value: 'currentpass123' } });
      fireEvent.change(newPasswordInput, { target: { value: 'newpassword123' } });
      fireEvent.change(confirmPasswordInput, { target: { value: 'newpassword123' } });
      
      const changePasswordButton = screen.getByText('Change Password');
      fireEvent.click(changePasswordButton);
      
      expect(screen.getByText('Changing password...')).toBeInTheDocument();
      expect(changePasswordButton).toBeDisabled();
      
      // Resolve the promise to complete the test
      resolvePromise!({ data: { success: true } });
    });

    it('validates individual password requirements', () => {
      render(<UserSettingsPage />);
      
      const newPasswordInput = screen.getByLabelText(/new password/i);
      
      // Test different password patterns
      fireEvent.change(newPasswordInput, { target: { value: 'shortpass' } });
      expect(screen.getByText('Weak')).toBeInTheDocument();
      
      fireEvent.change(newPasswordInput, { target: { value: 'LongPassword123' } });
      expect(screen.getByText('Medium')).toBeInTheDocument();
      
      fireEvent.change(newPasswordInput, { target: { value: 'VeryStrongPassword123!@#' } });
      expect(screen.getByText('Strong')).toBeInTheDocument();
    });

    it('disables change password button when form is invalid', async () => {
      render(<UserSettingsPage />);
      
      const changePasswordButton = screen.getByText('Change Password');
      
      // Button should be disabled initially
      expect(changePasswordButton).toBeDisabled();
      
      // Fill in only current password
      const currentPasswordInput = screen.getByLabelText(/current password/i);
      fireEvent.change(currentPasswordInput, { target: { value: 'currentpass123' } });
      
      // Button should still be disabled
      expect(changePasswordButton).toBeDisabled();
      
      // Fill in all fields correctly
      const newPasswordInput = screen.getByLabelText(/new password/i);
      const confirmPasswordInput = screen.getByLabelText(/confirm new password/i);
      
      fireEvent.change(newPasswordInput, { target: { value: 'newpassword123' } });
      fireEvent.change(confirmPasswordInput, { target: { value: 'newpassword123' } });
      
      // Button should now be enabled
      expect(changePasswordButton).not.toBeDisabled();
    });
  });

  describe('Error Handling', () => {
    it('handles network errors gracefully', async () => {
      mockApiClient.post.mockRejectedValue(new Error('Network error'));
      
      render(<UserSettingsPage />);
      
      const currentPasswordInput = screen.getByLabelText(/current password/i);
      const newPasswordInput = screen.getByLabelText(/new password/i);
      const confirmPasswordInput = screen.getByLabelText(/confirm new password/i);
      
      fireEvent.change(currentPasswordInput, { target: { value: 'currentpass123' } });
      fireEvent.change(newPasswordInput, { target: { value: 'newpassword123' } });
      fireEvent.change(confirmPasswordInput, { target: { value: 'newpassword123' } });
      
      const changePasswordButton = screen.getByText('Change Password');
      fireEvent.click(changePasswordButton);
      
      await waitFor(() => {
        expect(screen.getByText('Failed to change password. Please try again.')).toBeInTheDocument();
      });
    });

    it('clears error messages when form is modified', async () => {
      mockApiClient.post.mockRejectedValue({ 
        response: { data: { message: 'Current password is incorrect' } } 
      });
      
      render(<UserSettingsPage />);
      
      const currentPasswordInput = screen.getByLabelText(/current password/i);
      const newPasswordInput = screen.getByLabelText(/new password/i);
      const confirmPasswordInput = screen.getByLabelText(/confirm new password/i);
      
      fireEvent.change(currentPasswordInput, { target: { value: 'wrongpassword' } });
      fireEvent.change(newPasswordInput, { target: { value: 'newpassword123' } });
      fireEvent.change(confirmPasswordInput, { target: { value: 'newpassword123' } });
      
      const changePasswordButton = screen.getByText('Change Password');
      fireEvent.click(changePasswordButton);
      
      await waitFor(() => {
        expect(screen.getByText('Current password is incorrect')).toBeInTheDocument();
      });
      
      // Modify the current password field
      fireEvent.change(currentPasswordInput, { target: { value: 'correctpassword' } });
      
      // Error should be cleared
      expect(screen.queryByText('Current password is incorrect')).not.toBeInTheDocument();
    });
  });
});