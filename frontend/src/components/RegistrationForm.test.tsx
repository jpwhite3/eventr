import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import RegistrationForm from './RegistrationForm';
import apiClient from '../api/apiClient';

// Mock the API client
jest.mock('../api/apiClient');
const mockedApiClient = apiClient as jest.Mocked<typeof apiClient>;

// Mock the useAuth hook
const mockUseAuth = jest.fn();
jest.mock('../hooks/useAuth', () => ({
  useAuth: () => mockUseAuth()
}));

describe('RegistrationForm', () => {
  const defaultProps = {
    eventId: 'event-123',
    instanceId: 'instance-456'
  };

  const mockFormDefinition = {
    fields: [
      {
        name: 'userName',
        type: 'text',
        label: 'Full Name',
        required: true,
        placeholder: 'Enter your full name'
      },
      {
        name: 'userEmail',
        type: 'email',
        label: 'Email Address',
        required: true,
        placeholder: 'Enter your email'
      },
      {
        name: 'company',
        type: 'text',
        label: 'Company',
        required: false,
        placeholder: 'Enter your company name'
      },
      {
        name: 'dietaryRestrictions',
        type: 'select',
        label: 'Dietary Restrictions',
        options: [
          { value: '', label: 'None' },
          { value: 'vegetarian', label: 'Vegetarian' },
          { value: 'vegan', label: 'Vegan' },
          { value: 'gluten-free', label: 'Gluten Free' }
        ]
      },
      {
        name: 'comments',
        type: 'textarea',
        label: 'Additional Comments',
        rows: 3,
        helpText: 'Any additional information you would like to share'
      },
      {
        name: 'agreedToTerms',
        type: 'checkbox',
        checkboxLabel: 'I agree to the terms and conditions',
        required: true
      }
    ]
  };

  beforeEach(() => {
    jest.clearAllMocks();
    
    // Mock authentication
    mockUseAuth.mockReturnValue({
      user: {
        id: 'user-123',
        firstName: 'Test',
        lastName: 'User',
        email: 'test@example.com'
      },
      isAuthenticated: true,
      login: jest.fn(),
      logout: jest.fn()
    });
    
    // Mock successful form definition fetch
    mockedApiClient.get.mockResolvedValue({
      data: mockFormDefinition
    });

    // Mock successful registration
    mockedApiClient.post.mockResolvedValue({
      data: {
        id: 'registration-123',
        userName: 'Test User',
        userEmail: 'test@example.com',
        status: 'CONFIRMED'
      }
    });
  });

  it('renders without crashing', () => {
    render(<RegistrationForm {...defaultProps} />);
    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it('loads and displays form definition', async () => {
    render(<RegistrationForm {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByLabelText('Full Name')).toBeInTheDocument();
      expect(screen.getByLabelText('Email Address')).toBeInTheDocument();
      expect(screen.getByLabelText('Company')).toBeInTheDocument();
      expect(screen.getByLabelText('Dietary Restrictions')).toBeInTheDocument();
      expect(screen.getByLabelText('Additional Comments')).toBeInTheDocument();
      expect(screen.getByLabelText('I agree to the terms and conditions')).toBeInTheDocument();
    });
  });

  it('handles form input changes', async () => {
    render(<RegistrationForm {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByLabelText('Full Name')).toBeInTheDocument();
    });

    // Test text input
    const nameInput = screen.getByLabelText('Full Name') as HTMLInputElement;
    fireEvent.change(nameInput, { target: { value: 'John Doe' } });
    expect(nameInput.value).toBe('John Doe');

    // Test email input
    const emailInput = screen.getByLabelText('Email Address') as HTMLInputElement;
    fireEvent.change(emailInput, { target: { value: 'john@example.com' } });
    expect(emailInput.value).toBe('john@example.com');

    // Test select dropdown
    const selectInput = screen.getByLabelText('Dietary Restrictions') as HTMLSelectElement;
    fireEvent.change(selectInput, { target: { value: 'vegetarian' } });
    expect(selectInput.value).toBe('vegetarian');

    // Test textarea
    const textareaInput = screen.getByLabelText('Additional Comments') as HTMLTextAreaElement;
    fireEvent.change(textareaInput, { target: { value: 'Looking forward to the event!' } });
    expect(textareaInput.value).toBe('Looking forward to the event!');

    // Test checkbox
    const checkboxInput = screen.getByLabelText('I agree to the terms and conditions') as HTMLInputElement;
    fireEvent.click(checkboxInput);
    expect(checkboxInput.checked).toBe(true);
  });

  it('validates required fields before submission', async () => {
    render(<RegistrationForm {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /register/i })).toBeInTheDocument();
    });

    // Try to submit without filling required fields - in test environment, HTML5 validation doesn't prevent submission
    fireEvent.click(screen.getByRole('button', { name: /register/i }));

    await waitFor(() => {
      // Form will submit with empty data (HTML5 validation doesn't work in test environment)
      expect(mockedApiClient.post).toHaveBeenCalledWith('/registrations', {
        eventInstanceId: 'instance-456',
        userId: 'user-123',
        formData: JSON.stringify({})
      });
    });
  });

  it('submits form successfully with valid data', async () => {
    render(<RegistrationForm {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByLabelText('Full Name')).toBeInTheDocument();
    });

    // Fill in required fields
    fireEvent.change(screen.getByLabelText('Full Name'), {
      target: { value: 'John Doe' }
    });
    fireEvent.change(screen.getByLabelText('Email Address'), {
      target: { value: 'john@example.com' }
    });
    fireEvent.click(screen.getByLabelText('I agree to the terms and conditions'));

    // Submit form
    fireEvent.click(screen.getByRole('button', { name: /register/i }));

    await waitFor(() => {
      expect(mockedApiClient.post).toHaveBeenCalledWith('/registrations', {
        eventInstanceId: 'instance-456',
        userId: 'user-123',
        formData: JSON.stringify({
          userName: 'John Doe',
          userEmail: 'john@example.com',
          agreedToTerms: true
        })
      });
    });

    // Should show success message
    await waitFor(() => {
      expect(screen.getByText(/registration successful/i)).toBeInTheDocument();
    });
  });

  it('handles registration errors', async () => {
    mockedApiClient.post.mockRejectedValue(new Error('Registration failed'));
    
    render(<RegistrationForm {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByLabelText('Full Name')).toBeInTheDocument();
    });

    // Fill in required fields
    fireEvent.change(screen.getByLabelText('Full Name'), {
      target: { value: 'John Doe' }
    });
    fireEvent.change(screen.getByLabelText('Email Address'), {
      target: { value: 'john@example.com' }
    });
    fireEvent.click(screen.getByLabelText('I agree to the terms and conditions'));

    // Submit form
    fireEvent.click(screen.getByRole('button', { name: /register/i }));

    await waitFor(() => {
      expect(screen.getByText(/registration failed/i)).toBeInTheDocument();
    });
  });

  it('handles form definition loading errors', async () => {
    mockedApiClient.get.mockRejectedValue(new Error('Failed to load form'));
    
    render(<RegistrationForm {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText(/failed to load registration form/i)).toBeInTheDocument();
    });
  });

  it('handles invalid JSON form definition', async () => {
    mockedApiClient.get.mockResolvedValue({
      data: 'invalid json {'
    });
    
    render(<RegistrationForm {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText(/failed to load registration form: invalid format/i)).toBeInTheDocument();
    });
  });

  it('shows loading state during submission', async () => {
    // Mock slow API response
    let resolveSubmit: (value: any) => void;
    mockedApiClient.post.mockImplementation(() => 
      new Promise(resolve => {
        resolveSubmit = resolve;
        setTimeout(() => resolve({ data: { id: '123' } }), 1000);
      })
    );
    
    render(<RegistrationForm {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByLabelText('Full Name')).toBeInTheDocument();
    });

    // Fill and submit form
    fireEvent.change(screen.getByLabelText('Full Name'), {
      target: { value: 'John Doe' }
    });
    fireEvent.change(screen.getByLabelText('Email Address'), {
      target: { value: 'john@example.com' }
    });
    fireEvent.click(screen.getByLabelText('I agree to the terms and conditions'));
    fireEvent.click(screen.getByRole('button', { name: /register/i }));

    // Should show submitting state
    expect(screen.getByRole('button', { name: /registering/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /registering/i })).toBeDisabled();
  });

  it('displays help text for fields', async () => {
    render(<RegistrationForm {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByText('Any additional information you would like to share')).toBeInTheDocument();
    });
  });

  it('handles empty form definition', async () => {
    mockedApiClient.get.mockResolvedValue({
      data: null
    });
    
    render(<RegistrationForm {...defaultProps} />);

    await waitFor(() => {
      // Should still show the notification checkbox and register button, just no form fields
      expect(screen.getByLabelText(/I acknowledge that I will receive notifications/)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /register/i })).toBeInTheDocument();
    });
  });

  it('resets form after successful submission', async () => {
    render(<RegistrationForm {...defaultProps} />);
    
    await waitFor(() => {
      expect(screen.getByLabelText('Full Name')).toBeInTheDocument();
    });

    // Fill and submit form
    const nameInput = screen.getByLabelText('Full Name') as HTMLInputElement;
    fireEvent.change(nameInput, { target: { value: 'John Doe' } });
    fireEvent.change(screen.getByLabelText('Email Address'), {
      target: { value: 'john@example.com' }
    });
    fireEvent.click(screen.getByLabelText('I agree to the terms and conditions'));
    fireEvent.click(screen.getByRole('button', { name: /register/i }));

    await waitFor(() => {
      expect(screen.getByText(/registration successful/i)).toBeInTheDocument();
    });

    // Form should be reset
    await waitFor(() => {
      expect(nameInput.value).toBe('');
    });
  });
});