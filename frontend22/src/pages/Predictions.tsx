// frontend22/src/pages/Predictions.tsx

import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { apiService } from '../services/api';
import { Match, Prediction } from '../types/api';
import MatchCard from '../components/MatchCard';

const Predictions: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [allMatches, setAllMatches] = useState<Match[]>([]);
  const [predictions, setPredictions] = useState<Prediction[]>([]);
  const [userPoints, setUserPoints] = useState<number>(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadPredictions = useCallback(async () => {
    if (!user) return;
    try {
      setLoading(true);
      const [matchesData, predictionsData, pointsData] = await Promise.all([
        apiService.getAllMatches(),
        apiService.getUserPredictions(user.id),
        apiService.getUserPoints(user.id).catch(() => 0),
      ]);
      setAllMatches(matchesData);
      setPredictions(predictionsData);
      setUserPoints(pointsData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load predictions');
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    loadPredictions();
  }, [loadPredictions]);

  const getMatchForPrediction = (matchId: number): Match | undefined => {
    return allMatches.find(m => m.id === matchId);
  };

  const correctPredictions = predictions.filter(p => p.isCorrect);
  const incorrectPredictions = predictions.filter(p => !p.isCorrect && p.predictedWinnerId !== null);
  const pendingPredictions = predictions.filter(p => p.predictedWinnerId === null);

  return (
    <div className="min-h-screen bg-spotify-dark">
       <header className="bg-spotify-surface border-b border-spotify-surfaceLight">
         <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
           <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center py-4 sm:py-6 gap-2">
             <div>
               <h1 className="text-lg sm:text-xl md:text-2xl font-bold text-spotify-green">My Predictions</h1>
             </div>
             <div className="flex flex-col sm:flex-row items-start sm:items-center gap-1 sm:gap-4 w-full sm:w-auto">
               <span className="text-sm sm:text-base text-spotify-text">Welcome, {user?.fullName || user?.username}</span>
               <span className="text-sm text-spotify-textMuted whitespace-nowrap">Points: <span className="font-bold text-spotify-green">{userPoints}</span></span>
             </div>
           </div>
         </div>
       </header>

       <main className="max-w-4xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
        {error && (
          <div className="mt-4 bg-red-900 border border-red-500 text-red-200 px-4 py-3 rounded">
            {error}
          </div>
        )}

        {loading ? (
          <div className="flex items-center justify-center py-8">
            <div className="text-xl text-spotify-textSecondary">Loading predictions...</div>
          </div>
        ) : (
          <>
            <div className="grid gap-4 sm:gap-6 grid-cols-1 sm:grid-cols-3 mb-8">
              <div className="bg-spotify-surface border border-spotify-surfaceLight hover:border-spotify-green/50 transition-colors rounded-lg p-4 sm:p-6">
                <div className="text-2xl sm:text-3xl font-bold text-spotify-green">{correctPredictions.length}</div>
                <div className="text-xs sm:text-sm text-spotify-textMuted">Correct Predictions</div>
              </div>
              <div className="bg-spotify-surface border border-spotify-surfaceLight hover:border-red-500/50 transition-colors rounded-lg p-4 sm:p-6">
                <div className="text-2xl sm:text-3xl font-bold text-red-400">{incorrectPredictions.length}</div>
                <div className="text-xs sm:text-sm text-spotify-textMuted">Incorrect Predictions</div>
              </div>
              <div className="bg-spotify-surface border border-spotify-surfaceLight hover:border-yellow-500/50 transition-colors rounded-lg p-4 sm:p-6">
                <div className="text-2xl sm:text-3xl font-bold text-yellow-500">{pendingPredictions.length}</div>
                <div className="text-xs sm:text-sm text-spotify-textMuted">Pending Results</div>
              </div>
            </div>

            {predictions.length === 0 ? (
              <div className="text-center py-8 sm:py-12 px-4">
                <p className="text-spotify-textSecondary text-base sm:text-lg">No predictions yet. Go to Matches tab to make your first prediction!</p>
              </div>
            ) : (
              <div className="space-y-6 sm:space-y-8">
                {correctPredictions.length > 0 && (
                  <div>
                    <h2 className="text-lg sm:text-xl font-bold text-spotify-green mb-3 sm:mb-4">Correct Predictions</h2>
                    <div className="grid gap-4 sm:gap-6 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
                      {correctPredictions.map((prediction) => {
                        const match = getMatchForPrediction(prediction.matchId);
                        return match ? (
                          <MatchCard key={prediction.id} match={match} userPrediction={prediction} />
                        ) : null;
                      })}
                    </div>
                  </div>
                )}

                {incorrectPredictions.length > 0 && (
                  <div>
                    <h2 className="text-lg sm:text-xl font-bold text-red-400 mb-3 sm:mb-4">Incorrect Predictions</h2>
                    <div className="grid gap-4 sm:gap-6 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
                      {incorrectPredictions.map((prediction) => {
                        const match = getMatchForPrediction(prediction.matchId);
                        return match ? (
                          <MatchCard key={prediction.id} match={match} userPrediction={prediction} />
                        ) : null;
                      })}
                    </div>
                  </div>
                )}

                {pendingPredictions.length > 0 && (
                  <div>
                    <h2 className="text-lg sm:text-xl font-bold text-yellow-500 mb-3 sm:mb-4">Pending Results</h2>
                    <div className="grid gap-4 sm:gap-6 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
                      {pendingPredictions.map((prediction) => {
                        const match = getMatchForPrediction(prediction.matchId);
                        return match ? (
                          <MatchCard key={prediction.id} match={match} userPrediction={prediction} />
                        ) : null;
                      })}
                    </div>
                  </div>
                )}
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
};

export default Predictions;