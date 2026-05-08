// frontend22/src/contexts/AuthContext.tsx

import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { User, AuthRequest, AuthResponse, RegisterResponse } from '../types/api';
import { apiService } from '../services/api';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isLoading: boolean;
  login: (credentials: AuthRequest) => Promise<void>;
  register: (userData: AuthRequest) => Promise<RegisterResponse>;
  verifyOtpAndLogin: (email: string, otp: string) => Promise<void>;
  resendOtp: (email: string) => Promise<void>;
  changePassword: (identifier: string, currentPassword: string, newPassword: string) => Promise<void>;
  logout: () => void;
  setSession: (auth: AuthResponse) => void;
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
    }
  };

    const register = async (userData: AuthRequest): Promise<RegisterResponse> => {
      try {
        const response = await apiService.register({ ...userData, role: userData.role || 'USER' });
        // Do NOT set session yet; user must verify OTP first
        return response;
      } catch (error) {
        throw error;
      }
    };

   const logout = () => {
     localStorage.removeItem('token');
     localStorage.removeItem('user');
     setToken(null);
     setUser(null);
     setIsLoading(false);
   };

   const setSession = (auth: AuthResponse) => {
     const userData: User = {
       id: auth.userId,
       username: auth.username,
       email: auth.email,
       fullName: auth.fullName || '',
       points: 0,
       rank: 0,
       isActive: true,
       role: auth.role,
       createdAt: Date.now(),
       updatedAt: Date.now(),
     };
     localStorage.setItem('token', auth.token);
     localStorage.setItem('user', JSON.stringify(userData));
     setToken(auth.token);
     setUser(userData);
   };

    const verifyOtpAndLogin = async (email: string, otp: string) => {
      try {
        const response = await apiService.verifyOtp(email, otp);
        setSession(response);
      } catch (error) {
        throw error;
      }
    };

    const resendOtp = async (email: string) => {
      try {
        await apiService.resendOtp(email);
      } catch (error) {
        throw error;
      }
    };

    const changePassword = async (identifier: string, currentPassword: string, newPassword: string) => {
      try {
        await apiService.changePassword(identifier, currentPassword, newPassword);
      } catch (error) {
        throw error;
      }
    };

    const value: AuthContextType = {
      user,
      token,
      isLoading,
      login,
      register,
      verifyOtpAndLogin,
      resendOtp,
      changePassword,
      logout,
      setSession,
      isAuthenticated: !!user && !!token,
    };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};