import { useState, useEffect, useCallback } from 'react';
import { authService, User, LoginData, RegisterData, UpdateProfileData, ChangePasswordData } from '../services/AuthService';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}

export const useAuth = () => {
  const [state, setState] = useState<AuthState>({
    user: authService.getUser(),
    isAuthenticated: authService.isAuthenticated(),
    isLoading: false,
    error: null
  });

  const setError = useCallback((error: string | null) => {
    setState(prev => ({ ...prev, error, isLoading: false }));
  }, []);

  const setLoading = useCallback((isLoading: boolean) => {
    setState(prev => ({ ...prev, isLoading }));
  }, []);

  const updateAuthState = useCallback(() => {
    setState(prev => ({
      ...prev,
      user: authService.getUser(),
      isAuthenticated: authService.isAuthenticated(),
      isLoading: false,
      error: null
    }));
  }, []);

  const login = useCallback(async (data: LoginData) => {
    setLoading(true);
    try {
      await authService.login(data);
      updateAuthState();
    } catch (error: any) {
      setError(error.message);
      throw error;
    }
  }, [setLoading, updateAuthState, setError]);

  const register = useCallback(async (data: RegisterData) => {
    setLoading(true);
    try {
      await authService.register(data);
      updateAuthState();
    } catch (error: any) {
      setError(error.message);
      throw error;
    }
  }, [setLoading, updateAuthState, setError]);

  const logout = useCallback(async () => {
    setLoading(true);
    try {
      await authService.logout();
      updateAuthState();
    } catch (error: any) {
      setError(error.message);
    }
  }, [setLoading, updateAuthState, setError]);

  const updateProfile = useCallback(async (data: UpdateProfileData) => {
    setLoading(true);
    try {
      await authService.updateProfile(data);
      updateAuthState();
    } catch (error: any) {
      setError(error.message);
      throw error;
    }
  }, [setLoading, updateAuthState, setError]);

  const changePassword = useCallback(async (data: ChangePasswordData) => {
    setLoading(true);
    try {
      await authService.changePassword(data);
      setLoading(false);
    } catch (error: any) {
      setError(error.message);
      throw error;
    }
  }, [setLoading, setError]);

  const verifyEmail = useCallback(async (token: string) => {
    setLoading(true);
    try {
      await authService.verifyEmail(token);
      updateAuthState();
    } catch (error: any) {
      setError(error.message);
      throw error;
    }
  }, [setLoading, updateAuthState, setError]);

  const forgotPassword = useCallback(async (email: string) => {
    setLoading(true);
    try {
      await authService.forgotPassword(email);
      setLoading(false);
    } catch (error: any) {
      setError(error.message);
      throw error;
    }
  }, [setLoading, setError]);

  const resetPassword = useCallback(async (token: string, newPassword: string) => {
    setLoading(true);
    try {
      await authService.resetPassword(token, newPassword);
      updateAuthState();
    } catch (error: any) {
      setError(error.message);
      throw error;
    }
  }, [setLoading, updateAuthState, setError]);

  const refreshProfile = useCallback(async () => {
    setLoading(true);
    try {
      await authService.getProfile();
      updateAuthState();
    } catch (error: any) {
      setError(error.message);
    }
  }, [setLoading, updateAuthState, setError]);

  const clearError = useCallback(() => {
    setError(null);
  }, [setError]);

  // Helper functions
  const hasRole = useCallback((role: string): boolean => {
    return authService.hasRole(role);
  }, []);

  const hasAnyRole = useCallback((roles: string[]): boolean => {
    return authService.hasAnyRole(roles);
  }, []);

  const isAdmin = useCallback((): boolean => {
    return authService.isAdmin();
  }, []);

  const isOrganizer = useCallback((): boolean => {
    return authService.isOrganizer();
  }, []);

  // Listen for auth state changes (in case auth service is used elsewhere)
  useEffect(() => {
    const checkAuthState = () => {
      const currentUser = authService.getUser();
      const currentAuth = authService.isAuthenticated();
      
      if (currentUser !== state.user || currentAuth !== state.isAuthenticated) {
        updateAuthState();
      }
    };

    // Check auth state periodically
    const interval = setInterval(checkAuthState, 1000);
    
    return () => clearInterval(interval);
  }, [state.user, state.isAuthenticated, updateAuthState]);

  return {
    // State
    user: state.user,
    isAuthenticated: state.isAuthenticated,
    isLoading: state.isLoading,
    error: state.error,

    // Actions
    login,
    register,
    logout,
    updateProfile,
    changePassword,
    verifyEmail,
    forgotPassword,
    resetPassword,
    refreshProfile,
    clearError,

    // Helper functions
    hasRole,
    hasAnyRole,
    isAdmin,
    isOrganizer
  };
};