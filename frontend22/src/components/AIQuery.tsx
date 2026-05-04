// frontend22/src/components/AIQuery.tsx

import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts';
import { Team } from '../types/api';

interface VoteData {
  teamName: string;
  votes: number;
}

const AIQuery: React.FC = () => {
  const [voteData, setVoteData] = useState<VoteData[]>([]);
  const [totalVotes, setTotalVotes] = useState(0);
  const [accuracy, setAccuracy] = useState<{ correct: number; total: number } | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchData = async () => {
    try {
      const [voteCounts, teams, accuracyData] = await Promise.all([
        apiService.getVoteCounts(),
        apiService.getAllTeams(),
        apiService.getPredictionAccuracy()
      ]);

      const teamMap = new Map<number, string>();
      teams.forEach((team: Team) => {
        teamMap.set(team.id, team.shortName || team.teamName);
      });

      const data: VoteData[] = Object.entries(voteCounts).map(([teamId, votes]) => ({
        teamName: teamMap.get(parseInt(teamId)) || `Team ${teamId}`,
        votes: votes as number
      })).sort((a, b) => b.votes - a.votes);

      const total = data.reduce((sum, item) => sum + item.votes, 0);
      setVoteData(data);
      setTotalVotes(total);
      setAccuracy(accuracyData);
      setError('');
    } catch (err) {
      console.error('Error loading vote data:', err);
      setError(err instanceof Error ? err.message : 'Failed to load vote data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 10000); // Update every 10 seconds
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="max-w-6xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
      <div className="bg-spotify-surface border border-spotify-surfaceLight shadow-lg rounded-lg">
        <div className="px-4 py-5 sm:p-6">
      <h3 className="text-2xl leading-8 font-medium text-spotify-green mb-4 text-center">
        Today's Team Prediction Votes
      </h3>

          {loading && (
            <div className="flex justify-center items-center h-64">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-spotify-green"></div>
            </div>
          )}

          {error && (
            <div className="mt-4 bg-red-900 border border-red-500 text-red-200 px-4 py-3 rounded">
              {error}
            </div>
          )}

          {!loading && !error && (
            <div className="h-64 sm:h-80 md:h-96 max-w-3xl mx-auto">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={voteData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                  <XAxis
                    dataKey="teamName"
                    stroke="#9CA3AF"
                    fontSize={12}
                    fontWeight="bold"
                  />
                  <YAxis stroke="#9CA3AF" />
                  <Tooltip
                    formatter={(value: number) => {
                      if (totalVotes === 0) return ['0 votes (0%)', 'Votes'];
                      const percentage = ((value / totalVotes) * 100).toFixed(1);
                      return [`${value} votes (${percentage}%)`, 'Votes'];
                    }}
                    labelFormatter={(label) => `${label}`}
                    contentStyle={{
                      backgroundColor: '#1F2937',
                      border: '1px solid #374151',
                      borderRadius: '8px',
                      color: '#F3F4F6'
                    }}
                  />
                  <Bar
                    dataKey="votes"
                    fill="#1DB954"
                    animationDuration={1500}
                    animationEasing="ease-out"
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}

          <div className="mt-6 text-sm text-spotify-textSecondary">
            This chart shows the real-time prediction counts for teams in today's matches.
          </div>
        </div>
      </div>

      {/* Prediction Accuracy Pie Chart */}
      <div className="bg-spotify-surface border border-spotify-surfaceLight shadow-lg rounded-lg mt-8">
        <div className="px-4 py-5 sm:p-6">
          <div className="mt-0">
          <h3 className="text-2xl leading-8 font-medium text-spotify-green mb-4 text-center">
            Overall Prediction Accuracy
          </h3>

          {accuracy && accuracy.total > 0 && (
            <div className="text-center mb-4">
              <p className="text-spotify-text">
                {((accuracy.correct / accuracy.total) * 100).toFixed(1)}% of predictions are correct so far
              </p>
            </div>
          )}

          {accuracy && accuracy.total > 0 ? (
            <div className="h-80 sm:h-[400px] md:h-[480px]">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={[
                      { name: 'Correct', value: accuracy.correct },
                      { name: 'Incorrect', value: accuracy.total - accuracy.correct }
                    ]}
                    cx="50%"
                    cy="50%"
                    outerRadius={120}
                    innerRadius={50}
                    dataKey="value"
                    animationBegin={0}
                    animationDuration={2000}
                    animationEasing="ease-out"
                    label={({ name, percent }) => percent > 0 ? `${name}\n${(percent * 100).toFixed(1)}%` : ''}
                    labelLine={false}
                  >
                    <Cell fill="#1DB954" stroke="#1DB954" strokeWidth={2} />
                    <Cell fill="#FF6B6B" stroke="#FF6B6B" strokeWidth={2} />
                  </Pie>
                  <Tooltip
                    formatter={(value: number, name: string) => [`${value} predictions`, name]}
                    contentStyle={{
                      backgroundColor: '#1F2937',
                      border: '1px solid #374151',
                      borderRadius: '8px',
                      color: '#F3F4F6'
                    }}
                  />
                  <Legend
                    verticalAlign="bottom"
                    height={36}
                    formatter={(value, entry) => <span style={{ color: entry.color }}>{value}</span>}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <p className="text-spotify-textSecondary">No prediction data available yet.</p>
          )}
        </div>
        </div>
      </div>
    </div>
  );
};

export default AIQuery;