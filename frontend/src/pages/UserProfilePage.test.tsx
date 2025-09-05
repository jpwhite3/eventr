import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import UserProfilePage from './UserProfilePage';
import { useAuth } from '../hooks/useAuth';

// Mock the useAuth hook
jest.mock('../hooks/useAuth');
const mockUpdateProfile = jest.fn();

const mockUseAuth = useAuth as jest.MockedFunction<typeof useAuth>;

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
    phoneNumber: '123-456-7890',
    dateOfBirth: '1990-01-01',
    bio: 'Test user bio',
    location: 'Test City',
    website: 'https://example.com',
    linkedinUrl: 'https://linkedin.com/in/johndoe',
    twitterUrl: 'https://twitter.com/johndoe',
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
      isLoading: false,
      updateProfile: mockUpdateProfile
    });

    mockUpdateProfile.mockResolvedValue(undefined);
  });

  it('renders user profile page with user data', () => {
    render(<UserProfilePage />);
    
    expect(screen.getByText('My Profile')).toBeInTheDocument();
    expect(screen.getByDisplayValue('John')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Doe')).toBeInTheDocument();
    expect(screen.getByDisplayValue('test@example.com')).toBeInTheDocument();
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
    
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('enables editing mode when edit button is clicked', () => {
    render(<UserProfilePage />);
    
    // Initially fields should be readonly
    const firstNameInput = screen.getByDisplayValue('John');
    expect(firstNameInput).toHaveAttribute('readOnly');
    
    // Click edit button
    const editButton = screen.getByText('Edit Profile');
    fireEvent.click(editButton);
    
    // Fields should now be editable
    expect(firstNameInput).not.toHaveAttribute('readOnly');
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
    
    // Should restore original value
    expect(screen.getByDisplayValue('John')).toBeInTheDocument();
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
      expect(mockUpdateProfile).toHaveBeenCalledWith({
        firstName: 'Jane',
        bio: 'Updated bio'
      });
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

  it('validates email format', async () => {
    render(<UserProfilePage />);
    
    // Enter edit mode
    fireEvent.click(screen.getByText('Edit Profile'));
    
    // Enter invalid email
    fireEvent.change(screen.getByDisplayValue('test@example.com'), { target: { value: 'invalid-email' } });
    fireEvent.click(screen.getByText('Save Changes'));
    
    await waitFor(() => {
      expect(screen.getByText('Please enter a valid email address')).toBeInTheDocument();
    });
    
    expect(mockUpdateProfile).not.toHaveBeenCalled();
  });

  it('validates phone number format', async () => {
    render(<UserProfilePage />);
    
    // Enter edit mode
    fireEvent.click(screen.getByText('Edit Profile'));
    
    // Enter invalid phone number
    fireEvent.change(screen.getByDisplayValue('123-456-7890'), { target: { value: '123' } });
    fireEvent.click(screen.getByText('Save Changes'));
    
    await waitFor(() => {
      expect(screen.getByText('Please enter a valid phone number')).toBeInTheDocument();
    });
    
    expect(mockUpdateProfile).not.toHaveBeenCalled();
  });

  it('validates website URL format', async () => {
    render(<UserProfilePage />);
    
    // Enter edit mode
    fireEvent.click(screen.getByText('Edit Profile'));
    
    // Enter invalid URL
    fireEvent.change(screen.getByDisplayValue('https://example.com'), { target: { value: 'not-a-url' } });
    fireEvent.click(screen.getByText('Save Changes'));
    
    await waitFor(() => {
      expect(screen.getByText('Please enter a valid URL')).toBeInTheDocument();
    });
    
    expect(mockUpdateProfile).not.toHaveBeenCalled();
  });

  it('displays all profile sections', () => {
    render(<UserProfilePage />);
    
    expect(screen.getByText('Basic Information')).toBeInTheDocument();
    expect(screen.getByText('Contact Information')).toBeInTheDocument();
    expect(screen.getByText('Personal Information')).toBeInTheDocument();
    expect(screen.getByText('Social Links')).toBeInTheDocument();
  });

  it('shows profile statistics', () => {
    render(<UserProfilePage />);
    
    expect(screen.getByText('Profile Statistics')).toBeInTheDocument();
    expect(screen.getByText('Member Since')).toBeInTheDocument();
    expect(screen.getByText('Profile Completeness')).toBeInTheDocument();
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

  it('handles date of birth input correctly', () => {
    render(<UserProfilePage />);
    
    // Enter edit mode
    fireEvent.click(screen.getByText('Edit Profile'));
    
    const dobInput = screen.getByLabelText(/date of birth/i);
    expect(dobInput).toHaveValue('1990-01-01');
    
    // Change date
    fireEvent.change(dobInput, { target: { value: '1985-06-15' } });
    expect(dobInput).toHaveValue('1985-06-15');
  });

  it('handles empty optional fields correctly', async () => {
    // Mock user with some empty fields
    const incompleteUser = {
      ...mockUser,
      phoneNumber: '',
      bio: '',
      website: ''
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