// frontend22/src/contexts/AuthContext.tsx

import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { User, AuthRequest } from '../types/api';
import { apiService } from '../services/api';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isLoading: boolean;
  login: (credentials: AuthRequest) => Promise<void>;
  register: (userData: AuthRequest) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const validateToken = useCallback(async (tokenToValidate: string) => {
    try {
      const response = await apiService.validateToken();
      if (response.valid) {
        // Token is valid, keep the stored user data
      } else {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        setToken(null);
        setUser(null);
      }
    } catch (error) {
      console.error('Token validation failed:', error);
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      setToken(null);
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    const storedUser = localStorage.getItem('user');

    if (storedToken && storedUser) {
      setToken(storedToken);
      setUser(JSON.parse(storedUser));
      validateToken(storedToken);
    } else {
      setIsLoading(false);
    }
  }, [validateToken]);

  const login = async (credentials: AuthRequest) => {
    setIsLoading(true);
    try {
      const response = await apiService.login(credentials);

      // Store token and user data
      localStorage.setItem('token', response.token);
      const userData: User = {
        id: response.userId,
        username: response.username,
        email: response.email,
        fullName: response.fullName || '',
        points: 0,
        rank: 0,
        isActive: true,
        role: response.role,
        createdAt: Date.now(),
        updatedAt: Date.now(),
      };
      localStorage.setItem('user', JSON.stringify(userData));

      setToken(response.token);
      setUser(userData);
    } catch (error) {
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (userData: AuthRequest) => {
    setIsLoading(true);
    try {
      const response = await apiService.register({ ...userData, role: userData.role || 'USER' });

      // Store token and user data
      localStorage.setItem('token', response.token);
      const user: User = {
        id: response.userId,
        username: response.username,
        email: response.email,
        fullName: response.fullName || '',
        points: 0,
        rank: 0,
        isActive: true,
        role: response.role,
        createdAt: Date.now(),
        updatedAt: Date.now(),
      };
      localStorage.setItem('user', JSON.stringify(user));

      setToken(response.token);
      setUser(user);
    } catch (error) {
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
    setIsLoading(false);
  };

  const value: AuthContextType = {
    user,
    token,
    isLoading,
    login,
    register,
    logout,
    isAuthenticated: !!user && !!token,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};