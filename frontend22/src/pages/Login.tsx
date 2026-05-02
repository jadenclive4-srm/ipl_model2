// frontend22/src/pages/Login.tsx

import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';

const Login: React.FC = () => {
  const [isChangePassword, setIsChangePassword] = useState(false);
  const [loginData, setLoginData] = useState({
    username: '',
    password: '',
  });
  const [changePasswordData, setChangePasswordData] = useState({
    email: '',
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [showLoginPassword, setShowLoginPassword] = useState(false);
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  // OTP verification state
  const [pendingEmail, setPendingEmail] = useState<string | null>(null);
  const [pendingUsername, setPendingUsername] = useState<string | null>(null);
  const [otp, setOtp] = useState('');
  const [otpError, setOtpError] = useState('');
  const [resendTimer, setResendTimer] = useState(0);

  const { login, register, verifyOtpAndLogin, resendOtp, changePassword } = useAuth();
  const navigate = useNavigate();

  // Countdown for resend OTP
  useEffect(() => {
    if (resendTimer > 0) {
      const timer = setTimeout(() => setResendTimer(resendTimer - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [resendTimer]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (isChangePassword) {
      setChangePasswordData({
        ...changePasswordData,
        [e.target.name]: e.target.value,
      });
    } else {
      setLoginData({
        ...loginData,
        [e.target.name]: e.target.value,
      });
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setIsLoading(true);

    try {
      if (isChangePassword) {
        // Validate passwords match
        if (changePasswordData.newPassword !== changePasswordData.confirmPassword) {
          throw new Error('New passwords do not match');
        }
        await changePassword(changePasswordData.email, changePasswordData.currentPassword, changePasswordData.newPassword);
        setSuccess('Password changed successfully');
        setChangePasswordData({
          email: '',
          currentPassword: '',
          newPassword: '',
          confirmPassword: '',
        });
        setShowCurrentPassword(false);
        setShowNewPassword(false);
        setShowConfirmPassword(false);
      } else {
        await login({
          username: loginData.username,
          password: loginData.password,
        });
        navigate('/dashboard');
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
    setSuccess('');
    setIsChangePassword(false);
    setShowLoginPassword(false);
    setShowCurrentPassword(false);
    setShowNewPassword(false);
    setShowConfirmPassword(false);
  };

   // OTP Verification Screen
   if (pendingEmail) {
     return (
       <div className="min-h-screen flex items-center justify-center bg-spotify-dark py-12 px-4 sm:px-6 lg:px-8">
         <div className="max-w-md w-full space-y-8 bg-spotify-surface/50 backdrop-blur-sm rounded-2xl border border-spotify-surfaceLight/50 shadow-2xl">
           <div className="p-8">
             <div className="text-center mb-6">
               <h2 className="mt-2 text-center text-3xl font-extrabold text-spotify-text">
                 Verify your email
               </h2>
               <p className="mt-2 text-center text-sm text-spotify-textSecondary">
                 We sent a 6-digit code to <strong className="break-words" style={{color:'#1db954'}}>{pendingEmail}</strong>
               </p>
             </div>
             <form className="space-y-6" onSubmit={handleOtpSubmit}>
               <div className="space-y-4">
                 <div>
                   <label htmlFor="otp" className="sr-only">OTP</label>
                   <input
                     id="otp"
                     name="otp"
                     type="text"
                     inputMode="numeric"
                     maxLength={6}
                     required
                     className="appearance-none block w-full px-4 py-3 mt-1 text-center text-2xl tracking-widest font-mono text-spotify-text border border-spotify-surfaceLight rounded-lg bg-spotify-surface placeholder-spotify-textMuted focus:outline-none focus:ring-2 focus:ring-spotify-green focus:border-spotify-green transition-all duration-200 sm:text-sm"
                     placeholder="123456"
                     value={otp}
                     onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                   />
                 </div>
               </div>

               {(error || otpError) && (
                 <div className="bg-red-900/50 border border-red-500 text-red-100 p-4 rounded-lg text-center font-medium mb-4" role="alert">
                   {error || otpError}
                 </div>
               )}

               <div className="space-y-4">
                 <div>
                   <button
                     type="submit"
                     disabled={isLoading || otp.length !== 6}
                     className="group relative w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-lg text-black bg-spotify-green hover:bg-spotify-greenHover focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-spotify-green transition-all duration-200 disabled:opacity-50 hover:shadow-md"
                   >
                     {isLoading ? 'Verifying...' : 'Verify'}
                   </button>
                 </div>

                  <div className="flex items-center justify-between text-sm">
                    <button
                      type="button"
                      onClick={handleBackToForm}
                      className="text-spotify-textSecondary hover:text-spotify-text font-medium transition-colors duration-200"
                    >
                      Back to sign in
                    </button>
                   <button
                     type="button"
                     onClick={handleResendOtp}
                     disabled={resendTimer > 0}
                     className={`text-sm font-medium ${resendTimer > 0 ? 'text-spotify-textMuted cursor-not-allowed' : 'text-spotify-green hover:text-spotify-greenHover'} transition-colors duration-200`}
                   >
                     {resendTimer > 0 ? `Resend in ${resendTimer}s` : 'Resend code'}
                   </button>
                 </div>
               </div>
             </form>
           </div>
         </div>
       </div>
     );
   }

   // Login / Registration Form
   return (
     <div className="min-h-screen flex items-center justify-center bg-spotify-dark py-12 px-4 sm:px-6 lg:px-8">
       <div className="max-w-md w-full space-y-8 bg-spotify-surface/50 backdrop-blur-sm rounded-2xl border border-spotify-surfaceLight/50 shadow-2xl">
         <div className="p-8">
            <div className="text-center mb-6">
              <h2 className="mt-2 text-center text-3xl font-extrabold text-spotify-text">
                {isChangePassword ? 'Change your password' : 'Sign in to your account'}
              </h2>
              <p className="mt-2 text-center text-sm text-spotify-textSecondary">
                {isChangePassword ? 'Update your account password' : 'Welcome back to IPL Predictor'}
              </p>
            </div>
            <form className="space-y-6" onSubmit={handleSubmit}>
              <div className="space-y-4">
                {!isChangePassword && (
                  <div>
                    <label htmlFor="username" className="sr-only">Username</label>
                    <input
                      id="username"
                      name="username"
                      type="text"
                      required
                      className="appearance-none block w-full px-4 py-3 mt-1 text-spotify-text border border-spotify-surfaceLight rounded-lg bg-spotify-surface placeholder-spotify-textMuted focus:outline-none focus:ring-2 focus:ring-spotify-green focus:border-spotify-green transition-all duration-200 sm:text-sm"
                      placeholder="Username"
                      value={loginData.username}
                      onChange={handleInputChange}
                    />
                  </div>
                )}
                {isChangePassword ? (
                  <>
                    <div>
                      <label htmlFor="email" className="sr-only">Email address</label>
                      <input
                        id="email"
                        name="email"
                        type="email"
                        required
                        className="appearance-none block w-full px-4 py-3 mt-1 text-spotify-text border border-spotify-surfaceLight rounded-lg bg-spotify-surface placeholder-spotify-textMuted focus:outline-none focus:ring-2 focus:ring-spotify-green focus:border-spotify-green transition-all duration-200 sm:text-sm"
                        placeholder="Email address"
                        value={changePasswordData.email}
                        onChange={handleInputChange}
                      />
                    </div>
                    <div className="relative">
                      <label htmlFor="currentPassword" className="sr-only">Current Password</label>
                      <input
                        id="currentPassword"
                        name="currentPassword"
                        type={showCurrentPassword ? "text" : "password"}
                        required
                        className="appearance-none block w-full px-4 py-3 mt-1 pr-12 text-spotify-text border border-spotify-surfaceLight rounded-lg bg-spotify-surface placeholder-spotify-textMuted focus:outline-none focus:ring-2 focus:ring-spotify-green focus:border-spotify-green transition-all duration-200 sm:text-sm"
                        placeholder="Current Password"
                        value={changePasswordData.currentPassword}
                        onChange={handleInputChange}
                      />
                      <button
                        type="button"
                        className="absolute inset-y-0 right-0 pr-3 flex items-center text-spotify-textMuted hover:text-spotify-text transition-colors duration-200"
                        onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                      >
                        {showCurrentPassword ? 'Hide' : 'Show'}
                      </button>
                    </div>
                    <div className="relative">
                      <label htmlFor="newPassword" className="sr-only">New Password</label>
                      <input
                        id="newPassword"
                        name="newPassword"
                        type={showNewPassword ? "text" : "password"}
                        required
                        className="appearance-none block w-full px-4 py-3 mt-1 pr-12 text-spotify-text border border-spotify-surfaceLight rounded-lg bg-spotify-surface placeholder-spotify-textMuted focus:outline-none focus:ring-2 focus:ring-spotify-green focus:border-spotify-green transition-all duration-200 sm:text-sm"
                        placeholder="New Password"
                        value={changePasswordData.newPassword}
                        onChange={handleInputChange}
                      />
                      <button
                        type="button"
                        className="absolute inset-y-0 right-0 pr-3 flex items-center text-spotify-textMuted hover:text-spotify-text transition-colors duration-200"
                        onClick={() => setShowNewPassword(!showNewPassword)}
                      >
                        {showNewPassword ? 'Hide' : 'Show'}
                      </button>
                    </div>
                    <div className="relative">
                      <label htmlFor="confirmPassword" className="sr-only">Confirm New Password</label>
                      <input
                        id="confirmPassword"
                        name="confirmPassword"
                        type={showConfirmPassword ? "text" : "password"}
                        required
                        className="appearance-none block w-full px-4 py-3 mt-1 pr-12 text-spotify-text border border-spotify-surfaceLight rounded-lg bg-spotify-surface placeholder-spotify-textMuted focus:outline-none focus:ring-2 focus:ring-spotify-green focus:border-spotify-green transition-all duration-200 sm:text-sm"
                        placeholder="Confirm New Password"
                        value={changePasswordData.confirmPassword}
                        onChange={handleInputChange}
                      />
                      <button
                        type="button"
                        className="absolute inset-y-0 right-0 pr-3 flex items-center text-spotify-textMuted hover:text-spotify-text transition-colors duration-200"
                        onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      >
                        {showConfirmPassword ? 'Hide' : 'Show'}
                      </button>
                    </div>
                  </>
                ) : (
                  <div className="relative">
                    <label htmlFor="password" className="sr-only">Password</label>
                    <input
                      id="password"
                      name="password"
                      type={showLoginPassword ? "text" : "password"}
                      required
                      className="appearance-none block w-full px-4 py-3 mt-1 pr-12 text-spotify-text border border-spotify-surfaceLight rounded-lg bg-spotify-surface placeholder-spotify-textMuted focus:outline-none focus:ring-2 focus:ring-spotify-green focus:border-spotify-green transition-all duration-200 sm:text-sm"
                      placeholder="Password"
                      value={loginData.password}
                      onChange={handleInputChange}
                    />
                    <button
                      type="button"
                      className="absolute inset-y-0 right-0 pr-3 flex items-center text-spotify-textMuted hover:text-spotify-text transition-colors duration-200"
                      onClick={() => setShowLoginPassword(!showLoginPassword)}
                    >
                      {showLoginPassword ? 'Hide' : 'Show'}
                    </button>
                  </div>
                )}
              </div>
 
              {error && (
                <div className="bg-red-900/50 border border-red-500 text-red-100 p-4 rounded-lg text-center font-medium mb-4" role="alert">
                  {error}
                </div>
              )}

              {success && (
                <div className="bg-green-900/50 border border-green-500 text-green-100 p-4 rounded-lg text-center font-medium mb-4" role="alert">
                  {success}
                </div>
              )}
 
              <div>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="group relative w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-lg text-black bg-spotify-green hover:bg-spotify-greenHover focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-spotify-green transition-all duration-200 disabled:opacity-50 hover:shadow-md"
                >
                  {isLoading ? 'Loading...' : (isChangePassword ? 'Change Password' : 'Sign in')}
                </button>
              </div>
 
              <div className="text-center mt-4">
                {!isChangePassword && (
                  <button
                    type="button"
                    onClick={() => setIsChangePassword(true)}
                    className="text-spotify-green hover:text-spotify-greenHover text-sm font-medium transition-colors duration-200"
                  >
                    Change Password
                  </button>
                )}
                {isChangePassword && (
                  <button
                    type="button"
                    onClick={handleBackToForm}
                    className="text-spotify-textSecondary hover:text-spotify-text font-medium transition-colors duration-200"
                  >
                    Back to Sign in
                  </button>
                )}
              </div>
           </form>
         </div>
       </div>
     </div>
   );
};

export default Login;
