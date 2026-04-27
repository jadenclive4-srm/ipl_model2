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
  const [todayMatches, setTodayMatches] = useState<Match[]>([]);
  const [upcomingMatches, setUpcomingMatches] = useState<Match[]>([]);
  const [predictions, setPredictions] = useState<Prediction[]>([]);
  const [userPoints, setUserPoints] = useState<number>(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

   const loadMatchesData = useCallback(async () => {
     try {
       setLoading(true);
       const [todaysData, upcomingData, allMatchesData, predictionsData, pointsData] = await Promise.all([
         apiService.getTodaysMatches().catch(() => []),
         apiService.getUpcomingMatches().catch(() => []),
         apiService.getAllMatches(),
         user ? apiService.getUserPredictions(user.id) : Promise.resolve([]),
         user ? apiService.getUserPoints(user.id).catch(() => 0) : Promise.resolve(0),
       ]);
       
       console.log('DEBUG - Todays matches:', todaysData);
       console.log('DEBUG - Today length:', todaysData.length);
       console.log('DEBUG - Upcoming:', upcomingData.length);
       console.log('DEBUG - All matches:', allMatchesData.length);
       
       setTodayMatches(todaysData);
       setUpcomingMatches(upcomingData.filter(m => !todaysData.some(tm => tm.id === m.id)));
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
    { id: 'matches' as TabType, name: 'Matches', icon: '🏏', current: activeTab === 'matches' },
    { id: 'predictions' as TabType, name: 'Predictions', icon: '🎯', current: activeTab === 'predictions' },
    { id: 'leaderboard' as TabType, name: 'Leaderboard', icon: '🏆', current: activeTab === 'leaderboard' },
    { id: 'ai' as TabType, name: 'AI Assistant', icon: '🤖', current: activeTab === 'ai' },
  ];

  return (
    <div className="min-h-screen bg-spotify-dark">
      <header className="bg-spotify-surface border-b border-spotify-surfaceLight">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
           {/* Mobile Layout - Title with buttons on sides */}
           <div className="flex sm:hidden items-center justify-between py-4">
             <button
               onClick={() => user?.role === 'ADMIN' && navigate('/admin')}
               className={`bg-spotify-green hover:bg-spotify-greenHover text-spotify-black px-3 py-1 rounded-full text-xs font-medium ${user?.role !== 'ADMIN' ? 'opacity-0 pointer-events-none' : ''}`}
             >
               Admin
             </button>
            <div className="flex-1 text-center">
                <h1 className="text-xl font-bold text-spotify-green">IPL Predictor</h1>
                <div className="flex items-center justify-center space-x-2 mt-1">
                    <span className="text-spotify-green font-semibold">Points:</span>
                    <span className="bg-spotify-green/20 text-spotify-green px-2 py-0.5 rounded-full text-sm font-medium">
                        {userPoints}
                    </span>
                </div>
            </div>
             <button
               onClick={logout}
               className="bg-spotify-surfaceLight hover:bg-spotify-surfaceHover text-spotify-text px-3 py-1 rounded-full text-xs font-medium"
             >
               Logout
             </button>
           </div>

          {/* Desktop Layout */}
          <div className="hidden sm:flex sm:justify-between sm:items-center py-6">
            <div className="flex items-center">
                <h1 className="text-xl md:text-2xl font-bold text-spotify-green">IPL Predictor</h1>
              </div>
             <div className="flex items-center space-x-4">
               <span className="text-spotify-text">Welcome, {user?.fullName || user?.username}</span>
               <div className="flex items-center space-x-2">
                   <span className="text-spotify-green font-semibold">Points:</span>
                   <span className="bg-spotify-green/20 text-spotify-green px-2 py-0.5 rounded-full text-sm font-medium">
                       {userPoints}
                   </span>
               </div>
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

      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8 pb-16 md:pb-6">
        {/* Desktop Navigation - Hidden on mobile, visible on md+ screens */}
        <div className="hidden md:block bg-spotify-surface border border-spotify-surfaceLight rounded-t-lg mb-6">
          <nav className="flex space-x-8 -mb-px px-4" aria-label="Tabs">
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
                    {todayMatches.length > 0 && (
                      <div className="mb-8">
                        <h2 className="text-2xl font-bold text-spotify-text mb-4 text-center sm:text-left">Today's Matches</h2>
                        <div className="px-4 sm:px-0 grid gap-6 md:grid-cols-2 lg:grid-cols-2 justify-items-center max-w-4xl mx-auto">
                          {todayMatches.map((match) => (
                            <div key={match.id} className="w-full max-w-md">
                              <MatchCard
                                match={match}
                                userPrediction={getUserPredictionForMatch(match.id)}
                                onPredictClick={() => navigate(`/predict/${match.id}`)}
                                isLarge={true}
                              />
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                   {upcomingMatches.length > 0 && (
                     <div className="mb-8">
                       <h2 className="text-2xl font-bold text-spotify-text mb-4 text-center sm:text-left">Upcoming Matches</h2>
                       <div className="px-4 sm:px-0 grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                          {upcomingMatches.filter(m => !todayMatches.some(tm => tm.id === m.id)).slice(0, 3).map((match) => (
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

      {/* Bottom Navigation - Visible on mobile/tablet, hidden on desktop */}
      <nav className="block md:hidden fixed bottom-0 left-0 right-0 bg-spotify-surface border-t border-spotify-surfaceLight z-50">
        <div className="flex">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex-1 flex flex-col items-center justify-center py-3 px-2 text-xs font-medium transition-colors ${
                tab.current
                  ? 'text-spotify-green bg-spotify-surfaceLight'
                  : 'text-spotify-textMuted hover:text-spotify-textSecondary hover:bg-spotify-surfaceHover'
              }`}
            >
              <span className="text-lg mb-1">{tab.icon}</span>
              <span className="text-center leading-tight">{tab.name}</span>
            </button>
          ))}
        </div>
      </nav>
    </div>
  );
};

export default Dashboard;