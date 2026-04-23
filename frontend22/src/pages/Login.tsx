// frontend22/src/pages/Login.tsx

import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';

const Login: React.FC = () => {
  const [isLogin, setIsLogin] = useState(true);
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    email: '',
    fullName: '',
  });
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  // OTP verification state
  const [pendingEmail, setPendingEmail] = useState<string | null>(null);
  const [pendingUsername, setPendingUsername] = useState<string | null>(null);
  const [otp, setOtp] = useState('');
  const [otpError, setOtpError] = useState('');
  const [resendTimer, setResendTimer] = useState(0);

  const { login, register, verifyOtpAndLogin, resendOtp } = useAuth();
  const navigate = useNavigate();

  // Countdown for resend OTP
  useEffect(() => {
    if (resendTimer > 0) {
      const timer = setTimeout(() => setResendTimer(resendTimer - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [resendTimer]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      if (isLogin) {
        await login({
          username: formData.username,
          password: formData.password,
        });
        navigate('/dashboard');
      } else {
        const response = await register({
          username: formData.username,
          password: formData.password,
          email: formData.email,
          fullName: formData.fullName,
        });
        // Registration successful, OTP sent
        const email = response?.email || formData.email;
        const username = response?.username || formData.username;
        if (!email) {
          throw new Error('Registration succeeded but no email returned.');
        }
        setPendingEmail(email);
        setPendingUsername(username);
        setResendTimer(60);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'An error occurred';
      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  const handleOtpSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!pendingEmail) return;
    setOtpError('');
    setIsLoading(true);

    try {
      await verifyOtpAndLogin(pendingEmail, otp);
      navigate('/dashboard');
    } catch (error) {
      setOtpError(error instanceof Error ? error.message : 'Verification failed');
    } finally {
      setIsLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (!pendingEmail || resendTimer > 0) return;
    try {
      await resendOtp(pendingEmail);
      setOtpError('');
      setResendTimer(60);
    } catch (error) {
      setOtpError(error instanceof Error ? error.message : 'Failed to resend OTP');
    }
  };

  const handleBackToForm = () => {
    setPendingEmail(null);
    setPendingUsername(null);
    setOtp('');
    setOtpError('');
    setResendTimer(0);
    setError('');
  };

  // OTP Verification Screen
  if (pendingEmail) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-spotify-dark py-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-md w-full space-y-8">
          <div>
            <h2 className="mt-6 text-center text-3xl font-extrabold text-spotify-text">
              Verify your email
            </h2>
            <p className="mt-2 text-center text-sm text-spotify-textSecondary">
              We sent a 6-digit code to <strong className="break-words" style={{color:'#1db954'}}>{pendingEmail}</strong>
            </p>
          </div>
          <form className="mt-8 space-y-6" onSubmit={handleOtpSubmit}>
            <div className="rounded-md shadow-sm">
              <div>
                <input
                  id="otp"
                  name="otp"
                  type="text"
                  inputMode="numeric"
                  maxLength={6}
                  required
                  className="appearance-none rounded-none relative block w-full px-3 py-3 border border-spotify-surfaceLight bg-spotify-surface placeholder-spotify-textMuted text-spotify-text rounded-t-md focus:outline-none focus:ring-spotify-green focus:border-spotify-green focus:z-10 sm:text-sm text-center text-2xl tracking-widest"
                  placeholder="123456"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                />
              </div>
            </div>

            {(error || otpError) && (
              <div className="bg-red-900/50 border border-red-500 text-red-100 p-4 rounded-md text-center font-medium mb-4" role="alert">
                {error || otpError}
              </div>
            )}

            <div>
              <button
                type="submit"
                disabled={isLoading || otp.length !== 6}
                className="group relative w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-full text-black bg-spotify-green hover:bg-spotify-greenHover focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-spotify-green disabled:opacity-50"
              >
                {isLoading ? 'Verifying...' : 'Verify'}
              </button>
            </div>

            <div className="flex items-center justify-between text-sm">
              <button
                type="button"
                onClick={handleBackToForm}
                className="text-spotify-textSecondary hover:text-spotify-text"
              >
                Back to sign up
              </button>
              <button
                type="button"
                onClick={handleResendOtp}
                disabled={resendTimer > 0}
                className={`text-sm ${resendTimer > 0 ? 'text-spotify-textMuted cursor-not-allowed' : 'text-spotify-green hover:text-spotify-greenHover'}`}
              >
                {resendTimer > 0 ? `Resend in ${resendTimer}s` : 'Resend code'}
              </button>
            </div>
          </form>
        </div>
      </div>
    );
  }

  // Login / Registration Form
  return (
    <div className="min-h-screen flex items-center justify-center bg-spotify-dark py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-spotify-text">
            {isLogin ? 'Sign in to your account' : 'Create your account'}
          </h2>
          <p className="mt-2 text-center text-sm text-spotify-textSecondary">
            {isLogin ? 'Welcome back to IPL Predictor' : 'Join IPL Predictor'}
          </p>
        </div>
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          <div className="rounded-md shadow-sm -space-y-px">
            <div>
              <input
                id="username"
                name="username"
                type="text"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-3 border border-spotify-surfaceLight bg-spotify-surface placeholder-spotify-textMuted text-spotify-text rounded-t-md focus:outline-none focus:ring-spotify-green focus:border-spotify-green focus:z-10 sm:text-sm"
                placeholder="Username"
                value={formData.username}
                onChange={handleInputChange}
              />
            </div>
            {!isLogin && (
              <>
                <div>
                  <input
                    id="email"
                    name="email"
                    type="email"
                    required
                    className="appearance-none rounded-none relative block w-full px-3 py-3 border border-spotify-surfaceLight bg-spotify-surface placeholder-spotify-textMuted text-spotify-text focus:outline-none focus:ring-spotify-green focus:border-spotify-green focus:z-10 sm:text-sm"
                    placeholder="Email address"
                    value={formData.email}
                    onChange={handleInputChange}
                  />
                </div>
                <div>
                  <input
                    id="fullName"
                    name="fullName"
                    type="text"
                    required
                    className="appearance-none rounded-none relative block w-full px-3 py-3 border border-spotify-surfaceLight bg-spotify-surface placeholder-spotify-textMuted text-spotify-text focus:outline-none focus:ring-spotify-green focus:border-spotify-green focus:z-10 sm:text-sm"
                    placeholder="Full Name"
                    value={formData.fullName}
                    onChange={handleInputChange}
                  />
                </div>
              </>
            )}
            <div>
              <input
                id="password"
                name="password"
                type="password"
                required
                className={`appearance-none rounded-none relative block w-full px-3 py-3 border border-spotify-surfaceLight bg-spotify-surface placeholder-spotify-textMuted text-spotify-text ${
                  isLogin ? 'rounded-b-md' : ''
                } focus:outline-none focus:ring-spotify-green focus:border-spotify-green focus:z-10 sm:text-sm`}
                placeholder="Password"
                value={formData.password}
                onChange={handleInputChange}
              />
            </div>
          </div>

          {error && (
            <div className="bg-red-900/50 border border-red-500 text-red-100 p-4 rounded-md text-center font-medium mb-4" role="alert">
              {error}
            </div>
          )}

          <div>
            <button
              type="submit"
              disabled={isLoading}
              className="group relative w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-full text-black bg-spotify-green hover:bg-spotify-greenHover focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-spotify-green disabled:opacity-50"
            >
              {isLoading ? 'Loading...' : (isLogin ? 'Sign in' : 'Sign up')}
            </button>
          </div>

          <div className="text-center">
            <button
              type="button"
              onClick={() => setIsLogin(!isLogin)}
              className="text-spotify-green hover:text-spotify-greenHover text-sm"
            >
              {isLogin ? 'Need an account? Sign up' : 'Already have an account? Sign in'}
            </button>
          </div>
          </form>
      </div>
    </div>
  );
};

export default Login;
