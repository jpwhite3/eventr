import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import UserProfilePage from './UserProfilePage';
import { useAuth } from '../hooks/useAuth';

// Mock the useAuth hook
const mockUseAuth = jest.fn();
jest.mock('../hooks/useAuth', () => ({
  useAuth: () => mockUseAuth()
}));

const mockUpdateProfile = jest.fn();

const mockAuthBase = {
  error: null,
  login: jest.fn(),
  logout: jest.fn(),
  register: jest.fn(),
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

describe('UserProfilePage', () => {
  const mockUser = {
    id: 'user-123',
    email: 'test@example.com',
    firstName: 'John',
    lastName: 'Doe',
    phone: '123-456-7890',
    company: 'Test Company',
    jobTitle: 'Test Job',
    bio: 'Test user bio',
    timezone: 'America/New_York',
    role: 'ATTENDEE',
    status: 'ACTIVE',
    emailVerified: true,
    marketingEmails: false,
    eventReminders: true,
    weeklyDigest: true,
    language: 'en',
    createdAt: '2024-01-01T00:00:00Z'
  };

  beforeEach(() => {
    jest.clearAllMocks();
    
    mockUseAuth.mockReturnValue({
      ...mockAuthBase,
      user: mockUser,
      isAuthenticated: true,
      isLoading: false,
      updateProfile: mockUpdateProfile
    });

    mockUpdateProfile.mockResolvedValue(undefined);
  });

  it('renders user profile page with user data', () => {
    render(<UserProfilePage />);
    
    expect(screen.getByText('My Profile')).toBeInTheDocument();
    expect(screen.getByText('John')).toBeInTheDocument();
    expect(screen.getByText('Doe')).toBeInTheDocument();
    expect(screen.getByText('test@example.com')).toBeInTheDocument();
  });

  it('shows not authenticated message when user is not authenticated', () => {
    mockUseAuth.mockReturnValue({
      ...mockAuthBase,
      user: null,
      isAuthenticated: false,
      isLoading: false,
      updateProfile: jest.fn()
    });

    render(<UserProfilePage />);
    
    expect(screen.getByText('Authentication Required')).toBeInTheDocument();
    expect(screen.getByText('Please sign in to view your profile')).toBeInTheDocument();
  });

  it('shows loading state when auth is loading', () => {
    mockUseAuth.mockReturnValue({
      ...mockAuthBase,
      user: null,
      isAuthenticated: false,
      isLoading: true,
      updateProfile: jest.fn()
    });

    render(<UserProfilePage />);
    
    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByText('Loading...', { selector: 'p' })).toBeInTheDocument();
  });

  it('enables editing mode when edit button is clicked', () => {
    render(<UserProfilePage />);
    
    // Initially fields should be readonly text
    expect(screen.getByText('John')).toBeInTheDocument();
    expect(screen.queryByDisplayValue('John')).not.toBeInTheDocument();
    
    // Click edit button
    const editButton = screen.getByText('Edit Profile');
    fireEvent.click(editButton);
    
    // Fields should now be input elements
    expect(screen.getByDisplayValue('John')).toBeInTheDocument();
    expect(screen.getByText('Save Changes')).toBeInTheDocument();
    expect(screen.getByText('Cancel')).toBeInTheDocument();
  });

  it('cancels editing and restores original values', () => {
    render(<UserProfilePage />);
    
    // Enter edit mode
    fireEvent.click(screen.getByText('Edit Profile'));
    
    // Modify a field
    const firstNameInput = screen.getByDisplayValue('John');
    fireEvent.change(firstNameInput, { target: { value: 'Jane' } });
    expect(screen.getByDisplayValue('Jane')).toBeInTheDocument();
    
    // Cancel editing
    fireEvent.click(screen.getByText('Cancel'));
    
    // Should restore original value (back to text display)
    expect(screen.getByText('John')).toBeInTheDocument();
    expect(screen.queryByText('Save Changes')).not.toBeInTheDocument();
  });

  it('saves profile changes successfully', async () => {
    render(<UserProfilePage />);
    
    // Enter edit mode
    fireEvent.click(screen.getByText('Edit Profile'));
    
    // Modify fields
    fireEvent.change(screen.getByDisplayValue('John'), { target: { value: 'Jane' } });
    fireEvent.change(screen.getByDisplayValue('Test user bio'), { target: { value: 'Updated bio' } });
    
    // Save changes
    fireEvent.click(screen.getByText('Save Changes'));
    
    await waitFor(() => {
      expect(mockUpdateProfile).toHaveBeenCalled();
    });
    
    await waitFor(() => {
      expect(screen.getByText('Profile updated successfully!')).toBeInTheDocument();
    });
  });

  it('handles profile update errors', async () => {
    mockUpdateProfile.mockRejectedValue(new Error('Update failed'));
    
    render(<UserProfilePage />);
    
    // Enter edit mode and try to save
    fireEvent.click(screen.getByText('Edit Profile'));
    fireEvent.change(screen.getByDisplayValue('John'), { target: { value: 'Jane' } });
    fireEvent.click(screen.getByText('Save Changes'));
    
    await waitFor(() => {
      expect(screen.getByText('Update failed')).toBeInTheDocument();
    });
  });

  it('validates required fields', async () => {
    render(<UserProfilePage />);
    
    // Enter edit mode
    fireEvent.click(screen.getByText('Edit Profile'));
    
    // Clear required field
    fireEvent.change(screen.getByDisplayValue('John'), { target: { value: '' } });
    fireEvent.click(screen.getByText('Save Changes'));
    
    // Should not call updateProfile with empty required field
    expect(mockUpdateProfile).not.toHaveBeenCalled();
  });

  it('displays all profile sections', () => {
    render(<UserProfilePage />);
    
    expect(screen.getByText('Profile Information')).toBeInTheDocument();
    expect(screen.getByText('Account Summary')).toBeInTheDocument();
    expect(screen.getByText('Email Preferences')).toBeInTheDocument();
  });

  it('shows account information', () => {
    render(<UserProfilePage />);
    
    expect(screen.getByText('Account Summary')).toBeInTheDocument();
    expect(screen.getByText('Member Since')).toBeInTheDocument();
    expect(screen.getByText('Account Status')).toBeInTheDocument();
  });

  it('only sends changed fields in update request', async () => {
    render(<UserProfilePage />);
    
    // Enter edit mode
    fireEvent.click(screen.getByText('Edit Profile'));
    
    // Only change first name
    fireEvent.change(screen.getByDisplayValue('John'), { target: { value: 'Jane' } });
    fireEvent.click(screen.getByText('Save Changes'));
    
    await waitFor(() => {
      expect(mockUpdateProfile).toHaveBeenCalledWith({
        firstName: 'Jane'
      });
    });
    
    // Should not include unchanged fields
    expect(mockUpdateProfile).not.toHaveBeenCalledWith(expect.objectContaining({
      lastName: 'Doe',
      email: 'test@example.com'
    }));
  });

  it('handles phone number input correctly', () => {
    render(<UserProfilePage />);
    
    // Enter edit mode
    fireEvent.click(screen.getByText('Edit Profile'));
    
    const phoneInput = screen.getByDisplayValue('123-456-7890');
    expect(phoneInput).toBeInTheDocument();
    
    // Change phone number
    fireEvent.change(phoneInput, { target: { value: '555-123-4567' } });
    expect(screen.getByDisplayValue('555-123-4567')).toBeInTheDocument();
  });

  it('handles empty optional fields correctly', async () => {
    // Mock user with some empty fields
    const incompleteUser = {
      ...mockUser,
      phone: '',
      bio: '',
      company: ''
    };
    
    mockUseAuth.mockReturnValue({
      ...mockAuthBase,
      user: incompleteUser,
      isAuthenticated: true,
      isLoading: false,
      updateProfile: mockUpdateProfile
    });

    render(<UserProfilePage />);
    
    // Enter edit mode and add values to empty fields
    fireEvent.click(screen.getByText('Edit Profile'));
    
    const bioInput = screen.getByLabelText(/bio/i);
    fireEvent.change(bioInput, { target: { value: 'New bio content' } });
    
    fireEvent.click(screen.getByText('Save Changes'));
    
    await waitFor(() => {
      expect(mockUpdateProfile).toHaveBeenCalledWith({
        bio: 'New bio content'
      });
    });
  });

  it('displays success message after successful update', async () => {
    render(<UserProfilePage />);
    
    fireEvent.click(screen.getByText('Edit Profile'));
    fireEvent.change(screen.getByDisplayValue('John'), { target: { value: 'Updated John' } });
    fireEvent.click(screen.getByText('Save Changes'));
    
    await waitFor(() => {
      expect(screen.getByText('Profile updated successfully!')).toBeInTheDocument();
    });
    
    // Success message should disappear after edit mode is exited
    expect(screen.queryByText('Save Changes')).not.toBeInTheDocument();
  });
});