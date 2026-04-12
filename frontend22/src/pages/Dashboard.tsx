// frontend22/src/pages/Dashboard.tsx

import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { apiService } from '../services/api';
import { Match, Prediction } from '../types/api';
import MatchCard from '../components/MatchCard';
import Leaderboard from '../components/Leaderboard';
import AIQuery from '../components/AIQuery';
import Predictions from './Predictions';

type TabType = 'matches' | 'predictions' | 'leaderboard' | 'ai';

const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<TabType>('matches');
  const [allMatches, setAllMatches] = useState<Match[]>([]);
  const [todayMatch, setTodayMatch] = useState<Match | null>(null);
  const [upcomingMatches, setUpcomingMatches] = useState<Match[]>([]);
  const [predictions, setPredictions] = useState<Prediction[]>([]);
  const [userPoints, setUserPoints] = useState<number>(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadMatchesData = useCallback(async () => {
    try {
      setLoading(true);
      const [todayData, upcomingData, allMatchesData, predictionsData, pointsData] = await Promise.all([
        apiService.getTodayMatch().catch(() => null),
        apiService.getUpcomingMatches().catch(() => []),
        apiService.getAllMatches(),
        user ? apiService.getUserPredictions(user.id) : Promise.resolve([]),
        user ? apiService.getUserPoints(user.id).catch(() => 0) : Promise.resolve(0),
      ]);
      setTodayMatch(todayData);
      setUpcomingMatches(upcomingData);
      setAllMatches(allMatchesData);
      setPredictions(predictionsData);
      setUserPoints(pointsData);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to load data');
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    if (user) {
      loadMatchesData();
    }
  }, [user, loadMatchesData]);

  useEffect(() => {
    if (activeTab === 'matches' || activeTab === 'predictions') {
      loadMatchesData();
    }
  }, [activeTab]);

  const getUserPredictionForMatch = (matchId: number): Prediction | undefined => {
    return predictions.find(p => p.matchId === matchId);
  };

  const tabs = [
    { id: 'matches' as TabType, name: 'Matches', current: activeTab === 'matches' },
    { id: 'predictions' as TabType, name: 'Predictions', current: activeTab === 'predictions' },
    { id: 'leaderboard' as TabType, name: 'Leaderboard', current: activeTab === 'leaderboard' },
    { id: 'ai' as TabType, name: 'AI Assistant', current: activeTab === 'ai' },
  ];

  return (
    <div className="min-h-screen bg-spotify-dark">
      <header className="bg-spotify-surface border-b border-spotify-surfaceLight">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-6">
            <div className="flex items-center">
              <h1 className="text-3xl font-bold text-spotify-green">IPL Predictor</h1>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-spotify-text">Welcome, {user?.fullName || user?.username}</span>
              <span className="text-sm text-spotify-textMuted">Points: {userPoints}</span>
              {user?.role === 'ADMIN' && (
                <button
                  onClick={() => navigate('/admin')}
                  className="bg-spotify-green hover:bg-spotify-greenHover text-spotify-black px-4 py-2 rounded-full text-sm font-medium"
                >
                  Admin
                </button>
              )}
              <button
                onClick={logout}
                className="bg-spotify-surfaceLight hover:bg-spotify-surfaceHover text-spotify-text px-4 py-2 rounded-full text-sm font-medium"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="bg-spotify-surface border border-spotify-surfaceLight rounded-t-lg">
          <nav className="-mb-px flex space-x-8 px-4" aria-label="Tabs">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm ${
                  tab.current
                    ? 'border-spotify-green text-spotify-green'
                    : 'border-transparent text-spotify-textMuted hover:text-spotify-textSecondary hover:border-spotify-surfaceHover'
                }`}
              >
                {tab.name}
              </button>
            ))}
          </nav>
        </div>

        {error && (
          <div className="mt-4 bg-red-900/50 border border-red-500 text-red-200 px-4 py-3 rounded">
            {error}
          </div>
        )}

        <div className="mt-6">
          {activeTab === 'matches' && (
            <>
              {loading ? (
                <div className="flex items-center justify-center py-8">
                  <div className="text-xl text-spotify-textSecondary">Loading matches...</div>
                </div>
              ) : (
                <>
                  {todayMatch && (
                    <div className="mb-8">
                      <h2 className="text-2xl font-bold text-spotify-text mb-4">Today's Match</h2>
                      <div className="max-w-md mx-auto">
                        <MatchCard
                          match={todayMatch}
                          userPrediction={getUserPredictionForMatch(todayMatch.id)}
                          onPredictClick={() => navigate(`/predict/${todayMatch.id}`)}
                          isLarge={true}
                        />
                      </div>
                    </div>
                  )}

                  {upcomingMatches.length > 0 && (
                    <div className="mb-8">
                      <h2 className="text-2xl font-bold text-spotify-text mb-4">Upcoming Matches</h2>
                      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                        {upcomingMatches.filter(m => !todayMatch || m.id !== todayMatch.id).slice(0, 3).map((match) => (
                          <MatchCard
                            key={match.id}
                            match={match}
                            userPrediction={getUserPredictionForMatch(match.id)}
                            onPredictClick={() => navigate(`/predict/${match.id}`)}
                          />
                        ))}
                      </div>
                    </div>
                  )}
                </>
              )}
            </>
          )}

          {activeTab === 'predictions' && <Predictions />}

          {activeTab === 'leaderboard' && <Leaderboard />}

          {activeTab === 'ai' && <AIQuery />}
        </div>
      </main>
    </div>
  );
};

export default Dashboard;