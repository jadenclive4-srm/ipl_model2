// frontend22/src/components/Leaderboard.tsx

import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';
import { User } from '../types/api';

const Leaderboard: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadLeaderboard();
    
    // Refresh every 10 seconds
    const interval = setInterval(loadLeaderboard, 10000);
    return () => clearInterval(interval);
  }, []);

  const loadLeaderboard = async () => {
    try {
      setLoading(true);
      const data = await apiService.getLeaderboard();
      setUsers(data);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to load leaderboard');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-8">
        <div className="text-lg text-spotify-textSecondary">Loading leaderboard...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-900 border border-red-500 text-red-200 px-4 py-3 rounded mb-4">
        {error}
      </div>
    );
  }

  return (
    <div className="bg-spotify-surface border border-spotify-surfaceLight shadow-lg overflow-hidden sm:rounded-lg">
      <div className="px-4 py-5 sm:px-6 border-b border-spotify-surfaceLight">
        <h3 className="text-lg leading-6 font-medium text-spotify-green">Leaderboard</h3>
        <p className="mt-1 max-w-2xl text-sm text-spotify-textSecondary">
          Top performers in IPL predictions
        </p>
      </div>
      <ul className="divide-y divide-spotify-surfaceLight">
        {users.map((user, index) => (
          <li key={user.id} className="px-4 py-4 sm:px-6 hover:bg-spotify-surfaceLight transition-colors border-b border-spotify-surfaceLight last:border-b-0">
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <span className={`inline-flex items-center justify-center h-8 w-8 rounded-full text-sm font-medium ${
                    index === 0 ? 'bg-yellow-500 text-black' :
                    index === 1 ? 'bg-gray-400 text-black' :
                    index === 2 ? 'bg-orange-500 text-black' :
                    'bg-spotify-surfaceLight text-spotify-text'
                  }`}>
                    #{index + 1}
                  </span>
                </div>
                <div className="ml-4">
                  <div className="text-sm font-medium text-spotify-text">
                    {user.fullName || user.username}
                  </div>
                  <div className="text-sm text-spotify-textMuted">
                    @{user.username}
                  </div>
                </div>
              </div>
              <div className="text-right">
                <div className="text-sm font-medium text-spotify-text">
                  {user.points} points
                </div>
                <div className="text-sm text-spotify-textMuted">
                  Rank: {user.rank}
                </div>
              </div>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default Leaderboard;