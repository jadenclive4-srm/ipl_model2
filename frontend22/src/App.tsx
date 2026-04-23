// frontend22/src/App.tsx

import React from 'react';
import { HashRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Predictions from './pages/Predictions';
import MatchPrediction from './pages/MatchPrediction';
import Admin from './pages/Admin';

const AppRoutes: React.FC = () => {
  const { isAuthenticated, isLoading, user } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center w-full max-w-full overflow-x-hidden">
        <div className="text-xl">Loading...</div>
      </div>
    );
  }

  return (
    <div className="w-full max-w-full overflow-x-hidden">
      <Routes>
        <Route
          path="/login"
          element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <Login />}
        />
        <Route
          path="/dashboard"
          element={isAuthenticated ? <Dashboard /> : <Navigate to="/login" replace />}
        />
        <Route
          path="/predictions"
          element={isAuthenticated ? <Predictions /> : <Navigate to="/login" replace />}
        />
        <Route
          path="/predict/:matchId"
          element={isAuthenticated ? <MatchPrediction /> : <Navigate to="/login" replace />}
        />
        <Route
          path="/admin"
          element={
            isAuthenticated && user?.role === 'ADMIN' ? (
              <Admin />
            ) : (
              <Navigate to="/dashboard" replace />
            )
          }
        />
        <Route
          path="/"
          element={<Navigate to={isAuthenticated ? "/dashboard" : "/login"} replace />}
        />
      </Routes>
    </div>
  );
};

const App: React.FC = () => {
  return (
    <AuthProvider>
      <Router>
        <AppRoutes />
      </Router>
    </AuthProvider>
  );
};

export default App;