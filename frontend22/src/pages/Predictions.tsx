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
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-6">
           <div className="flex items-center">
               <h1 className="text-lg font-bold text-spotify-green sm:text-xl md:text-2xl">My Predictions</h1>
             </div>
            <div className="flex items-center space-x-4">
              <span className="text-spotify-text">Welcome, {user?.fullName || user?.username}</span>
              <span className="text-sm text-spotify-textMuted">Points: {userPoints}</span>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
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
            <div className="grid gap-6 md:grid-cols-3 mb-8">
              <div className="bg-spotify-surface border border-spotify-surfaceLight hover:border-spotify-green/50 transition-colors rounded-lg p-6">
                <div className="text-3xl font-bold text-spotify-green">{correctPredictions.length}</div>
                <div className="text-sm text-spotify-textMuted">Correct Predictions</div>
              </div>
              <div className="bg-spotify-surface border border-spotify-surfaceLight hover:border-red-500/50 transition-colors rounded-lg p-6">
                <div className="text-3xl font-bold text-red-400">{incorrectPredictions.length}</div>
                <div className="text-sm text-spotify-textMuted">Incorrect Predictions</div>
              </div>
              <div className="bg-spotify-surface border border-spotify-surfaceLight hover:border-yellow-500/50 transition-colors rounded-lg p-6">
                <div className="text-3xl font-bold text-yellow-500">{pendingPredictions.length}</div>
                <div className="text-sm text-spotify-textMuted">Pending Results</div>
              </div>
            </div>

            {predictions.length === 0 ? (
              <div className="text-center py-8">
                <p className="text-spotify-textSecondary text-lg">No predictions yet. Go to Matches tab to make your first prediction!</p>
              </div>
            ) : (
              <div className="space-y-8">
                {correctPredictions.length > 0 && (
                  <div>
                    <h2 className="text-xl font-bold text-spotify-green mb-4">Correct Predictions</h2>
                    <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
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
                    <h2 className="text-xl font-bold text-red-400 mb-4">Incorrect Predictions</h2>
                    <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
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
                    <h2 className="text-xl font-bold text-yellow-500 mb-4">Pending Results</h2>
                    <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
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