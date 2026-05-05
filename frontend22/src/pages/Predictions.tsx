// frontend22/src/pages/Predictions.tsx

import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { apiService } from '../services/api';
import { Match, Prediction } from '../types/api';
import MatchCard from '../components/MatchCard';

type FilterType = 'correct' | 'incorrect' | 'all';

const Predictions: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [allMatches, setAllMatches] = useState<Match[]>([]);
  const [predictions, setPredictions] = useState<Prediction[]>([]);
  const [userPoints, setUserPoints] = useState<number>(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeFilter, setActiveFilter] = useState<FilterType>('all');

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

  const getFilteredPredictions = () => {
    let filteredPredictions;
    switch (activeFilter) {
      case 'correct':
        filteredPredictions = correctPredictions;
        break;
      case 'incorrect':
        filteredPredictions = incorrectPredictions;
        break;
      default:
        filteredPredictions = [...correctPredictions, ...incorrectPredictions];
        break;
    }

    // Sort by match date (latest first)
    return filteredPredictions.sort((a, b) => {
      const matchA = getMatchForPrediction(a.matchId);
      const matchB = getMatchForPrediction(b.matchId);

      if (!matchA && !matchB) return 0;
      if (!matchA) return 1;
      if (!matchB) return -1;

      return matchB.matchDate - matchA.matchDate; // Descending order (latest first)
    });
  };

  const handleMatchClick = (matchId: number) => {
    navigate(`/predict/${matchId}`);
  };

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
            <div className="grid gap-3 sm:gap-4 md:gap-6 grid-cols-2 sm:grid-cols-3 mb-6 sm:mb-8">
              <button
                onClick={() => setActiveFilter(activeFilter === 'all' ? 'correct' : 'all')}
                className={`border transition-all duration-200 rounded-lg p-3 sm:p-4 md:p-6 cursor-pointer transform hover:scale-[1.02] active:scale-[0.98] ${
                  activeFilter === 'all'
                    ? 'bg-spotify-green/20 border-spotify-green shadow-lg shadow-spotify-green/20'
                    : 'bg-spotify-surface border-spotify-surfaceLight hover:border-spotify-green/50 hover:shadow-md'
                }`}
              >
                <div className="text-xl sm:text-2xl md:text-3xl font-bold text-spotify-green">{predictions.length}</div>
                <div className="text-xs sm:text-sm text-spotify-textMuted mt-1">All Predictions</div>
              </button>
              <button
                onClick={() => setActiveFilter(activeFilter === 'correct' ? 'all' : 'correct')}
                className={`border transition-all duration-200 rounded-lg p-3 sm:p-4 md:p-6 cursor-pointer transform hover:scale-[1.02] active:scale-[0.98] ${
                  activeFilter === 'correct'
                    ? 'bg-spotify-green/20 border-spotify-green shadow-lg shadow-spotify-green/20'
                    : 'bg-spotify-surface border-spotify-surfaceLight hover:border-spotify-green/50 hover:shadow-md'
                }`}
              >
                <div className="text-xl sm:text-2xl md:text-3xl font-bold text-spotify-green">{correctPredictions.length}</div>
                <div className="text-xs sm:text-sm text-spotify-textMuted mt-1">Correct Predictions</div>
              </button>
              <button
                onClick={() => setActiveFilter(activeFilter === 'incorrect' ? 'all' : 'incorrect')}
                className={`border transition-all duration-200 rounded-lg p-3 sm:p-4 md:p-6 cursor-pointer transform hover:scale-[1.02] active:scale-[0.98] ${
                  activeFilter === 'incorrect'
                    ? 'bg-red-400/20 border-red-400 shadow-lg shadow-red-400/20'
                    : 'bg-spotify-surface border-spotify-surfaceLight hover:border-red-500/50 hover:shadow-md'
                }`}
              >
                <div className="text-xl sm:text-2xl md:text-3xl font-bold text-red-400">{incorrectPredictions.length}</div>
                <div className="text-xs sm:text-sm text-spotify-textMuted mt-1">Incorrect Predictions</div>
              </button>
            </div>

            {predictions.length === 0 ? (
              <div className="text-center py-8 sm:py-12 px-4">
                <p className="text-spotify-textSecondary text-base sm:text-lg">No predictions yet. Go to Matches tab to make your first prediction!</p>
              </div>
            ) : (
              <div className="space-y-6 sm:space-y-8">
                {getFilteredPredictions().length > 0 ? (
                    <div>
                      <div className="grid gap-4 sm:gap-6 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
                        {getFilteredPredictions().map((prediction) => {
                          const match = getMatchForPrediction(prediction.matchId);
                          return match ? (
                            <MatchCard
                              key={prediction.id}
                              match={match}
                              userPrediction={prediction}
                              hideStatus={true}
                              onPredictClick={() => handleMatchClick(match.id)}
                            />
                          ) : null;
                        })}
                      </div>
                    </div>
                ) : (
                  <div className="text-center py-8 sm:py-12 px-4">
                    <p className="text-spotify-textSecondary text-base sm:text-lg">
                      {activeFilter === 'correct' ? 'No correct predictions yet.' : activeFilter === 'incorrect' ? 'No incorrect predictions yet.' : 'No predictions yet. Go to Matches tab to make your first prediction!'}
                    </p>
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