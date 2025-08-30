// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import '@testing-library/jest-dom';
import { TextEncoder, TextDecoder } from 'util';

// Polyfill for TextEncoder/TextDecoder
global.TextEncoder = TextEncoder;
global.TextDecoder = TextDecoder as any;

// Mock apiClient for tests with better default data
jest.mock('./api/apiClient', () => ({
  get: jest.fn().mockImplementation((url: string) => {
    // Return appropriate mock data based on the URL
    if (url.includes('/stats')) {
      return Promise.resolve({ 
        data: {
          totalRegistrations: 0,
          totalCheckIns: 0,
          checkInRate: 0,
          attendanceRate: 0,
          recentCheckIns: []
        }
      });
    }
    if (url.includes('/form')) {
      return Promise.resolve({ 
        data: {
          fields: [
            { name: 'fullName', type: 'text', label: 'Full Name', required: true },
            { name: 'comments', type: 'textarea', label: 'Comments', helpText: 'Any additional information you would like to share' }
          ]
        }
      });
    }
    if (url.includes('/registrations/user/id/')) {
      return Promise.resolve({ data: [] });
    }
    return Promise.resolve({ data: {} });
  }),
  post: jest.fn().mockResolvedValue({ data: { id: 'mock-id' } }),
  put: jest.fn().mockResolvedValue({ data: { id: 'mock-id' } }),
  delete: jest.fn().mockResolvedValue({ data: {} }),
}));

// Mock authentication hook to provide default authenticated state for tests
jest.mock('./hooks/useAuth', () => ({
  useAuth: () => ({
    isAuthenticated: true,
    user: {
      id: 'test-user-id',
      firstName: 'Test',
      lastName: 'User',
      email: 'test@example.com',
      role: 'USER'
    },
    login: jest.fn(),
    logout: jest.fn(),
    register: jest.fn(),
    hasRole: (role: string) => role === 'USER',
    hasAnyRole: (roles: string[]) => roles.includes('USER'),
  }),
}));

// Suppress console errors and warnings in tests to reduce noise
const originalError = console.error;
const originalWarn = console.warn;

beforeAll(() => {
  console.error = (...args: any[]) => {
    if (
      typeof args[0] === 'string' &&
      (args[0].includes('Warning: ReactDOM.render is deprecated') ||
       args[0].includes('Warning: An update to') ||
       args[0].includes('Warning: `ReactDOMTestUtils.act`') ||
       args[0].includes('act(...)')))
    {
      return;
    }
    originalError.call(console, ...args);
  };

  console.warn = (...args: any[]) => {
    if (
      typeof args[0] === 'string' &&
      args[0].includes('componentWillReceiveProps has been renamed')
    ) {
      return;
    }
    originalWarn.call(console, ...args);
  };
});

afterAll(() => {
  console.error = originalError;
  console.warn = originalWarn;
});
