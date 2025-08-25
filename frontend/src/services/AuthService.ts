import apiClient from '../api/apiClient';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  company?: string;
  jobTitle?: string;
  bio?: string;
  profileImageUrl?: string;
  role: string;
  status: string;
  emailVerified: boolean;
  timezone?: string;
  language?: string;
  marketingEmails: boolean;
  eventReminders: boolean;
  weeklyDigest: boolean;
  createdAt: string;
  lastLoginAt?: string;
}

export interface AuthResponse {
  token: string;
  user: User;
  expiresIn: number;
}

export interface LoginData {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterData {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  company?: string;
  jobTitle?: string;
  marketingEmails?: boolean;
}

export interface UpdateProfileData {
  firstName?: string;
  lastName?: string;
  phone?: string;
  company?: string;
  jobTitle?: string;
  bio?: string;
  timezone?: string;
  language?: string;
  marketingEmails?: boolean;
  eventReminders?: boolean;
  weeklyDigest?: boolean;
}

export interface ChangePasswordData {
  currentPassword: string;
  newPassword: string;
}

class AuthService {
  private static instance: AuthService;
  private token: string | null = null;
  private user: User | null = null;
  private refreshTimeout: NodeJS.Timeout | null = null;

  constructor() {
    this.loadFromStorage();
  }

  static getInstance(): AuthService {
    if (!AuthService.instance) {
      AuthService.instance = new AuthService();
    }
    return AuthService.instance;
  }

  private loadFromStorage() {
    try {
      const token = localStorage.getItem('auth_token');
      const userData = localStorage.getItem('auth_user');
      
      if (token && userData) {
        this.token = token;
        this.user = JSON.parse(userData);
        this.setAuthHeader();
        this.scheduleTokenRefresh();
      }
    } catch (error) {
      console.error('Error loading auth data from storage:', error);
      this.clearAuth();
    }
  }

  private saveToStorage() {
    if (this.token && this.user) {
      localStorage.setItem('auth_token', this.token);
      localStorage.setItem('auth_user', JSON.stringify(this.user));
    }
  }

  private clearStorage() {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
  }

  private setAuthHeader() {
    if (this.token) {
      apiClient.defaults.headers.common['Authorization'] = `Bearer ${this.token}`;
    } else {
      delete apiClient.defaults.headers.common['Authorization'];
    }
  }

  private scheduleTokenRefresh() {
    if (this.refreshTimeout) {
      clearTimeout(this.refreshTimeout);
    }
    
    // Schedule refresh 5 minutes before expiration (simplified)
    this.refreshTimeout = setTimeout(() => {
      this.refreshToken();
    }, 23 * 60 * 60 * 1000); // 23 hours
  }

  private clearAuth() {
    this.token = null;
    this.user = null;
    this.clearStorage();
    this.setAuthHeader();
    if (this.refreshTimeout) {
      clearTimeout(this.refreshTimeout);
    }
  }

  async login(data: LoginData): Promise<AuthResponse> {
    try {
      const response = await apiClient.post('/auth/login', data);
      const authData: AuthResponse = response.data;
      
      this.token = authData.token;
      this.user = authData.user;
      this.setAuthHeader();
      this.saveToStorage();
      this.scheduleTokenRefresh();
      
      return authData;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Login failed');
    }
  }

  async register(data: RegisterData): Promise<AuthResponse> {
    try {
      const response = await apiClient.post('/auth/register', data);
      const authData: AuthResponse = response.data;
      
      this.token = authData.token;
      this.user = authData.user;
      this.setAuthHeader();
      this.saveToStorage();
      this.scheduleTokenRefresh();
      
      return authData;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Registration failed');
    }
  }

  async logout(): Promise<void> {
    try {
      await apiClient.post('/auth/logout');
    } catch (error) {
      // Continue with logout even if server request fails
      console.error('Error during server logout:', error);
    } finally {
      this.clearAuth();
    }
  }

  async getProfile(): Promise<User> {
    try {
      const response = await apiClient.get('/auth/profile');
      this.user = response.data;
      this.saveToStorage();
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to get profile');
    }
  }

  async updateProfile(data: UpdateProfileData): Promise<User> {
    try {
      const response = await apiClient.put('/auth/profile', data);
      this.user = response.data;
      this.saveToStorage();
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to update profile');
    }
  }

  async changePassword(data: ChangePasswordData): Promise<User> {
    try {
      const response = await apiClient.put('/auth/change-password', data);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to change password');
    }
  }

  async verifyEmail(token: string): Promise<User> {
    try {
      const response = await apiClient.post('/auth/verify-email', { token });
      if (this.user) {
        this.user.emailVerified = true;
        this.saveToStorage();
      }
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Email verification failed');
    }
  }

  async forgotPassword(email: string): Promise<void> {
    try {
      await apiClient.post('/auth/forgot-password', { email });
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to send reset email');
    }
  }

  async resetPassword(token: string, newPassword: string): Promise<AuthResponse> {
    try {
      const response = await apiClient.post('/auth/reset-password', { token, newPassword });
      const authData: AuthResponse = response.data;
      
      this.token = authData.token;
      this.user = authData.user;
      this.setAuthHeader();
      this.saveToStorage();
      this.scheduleTokenRefresh();
      
      return authData;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Password reset failed');
    }
  }

  async refreshToken(): Promise<void> {
    // In a real implementation, this would call a refresh endpoint
    // For this simplified version, we'll just check if the current token is still valid
    try {
      await this.getProfile();
      this.scheduleTokenRefresh();
    } catch (error) {
      console.error('Token refresh failed:', error);
      this.clearAuth();
    }
  }

  isAuthenticated(): boolean {
    return this.token !== null && this.user !== null;
  }

  getToken(): string | null {
    return this.token;
  }

  getUser(): User | null {
    return this.user;
  }

  hasRole(role: string): boolean {
    return this.user?.role === role;
  }

  hasAnyRole(roles: string[]): boolean {
    return this.user ? roles.includes(this.user.role) : false;
  }

  isAdmin(): boolean {
    return this.hasAnyRole(['ADMIN', 'SUPER_ADMIN']);
  }

  isOrganizer(): boolean {
    return this.hasAnyRole(['ORGANIZER', 'ADMIN', 'SUPER_ADMIN']);
  }
}

export const authService = AuthService.getInstance();
export default authService;