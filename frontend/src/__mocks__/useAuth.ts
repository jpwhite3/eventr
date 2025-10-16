/**
 * Mock implementation of useAuth hook for testing
 */

const mockUser = {
  id: '1',
  email: 'test@example.com',
  firstName: 'Test',
  lastName: 'User',
  emailVerified: true,
  role: 'USER'
};

// Mock state that can be updated by tests
let mockState = {
  user: mockUser,
  isAuthenticated: true,
  isLoading: false,
  error: null
};

const mockChangePassword = jest.fn();

const mockAuthBase = {
  login: jest.fn(),
  logout: jest.fn(),
  register: jest.fn(),
  updateProfile: jest.fn(),
  changePassword: mockChangePassword,
  verifyEmail: jest.fn(),
  forgotPassword: jest.fn(),
  resetPassword: jest.fn(),
  refreshProfile: jest.fn(),
  clearError: jest.fn(),
  hasRole: jest.fn(() => true),
  hasAnyRole: jest.fn(() => true),
  isAdmin: jest.fn(() => false),
  isOrganizer: jest.fn(() => false)
};

export const mockUseAuth = jest.fn(() => ({
  ...mockState,
  ...mockAuthBase
}));

export const useAuth = mockUseAuth;

// Helper functions for tests
export const setMockAuthState = (newState: Partial<typeof mockState>) => {
  mockState = { ...mockState, ...newState };
};

export const resetMockAuthState = () => {
  mockState = {
    user: mockUser,
    isAuthenticated: true,
    isLoading: false,
    error: null
  };
  mockChangePassword.mockClear();
};

export { mockChangePassword };