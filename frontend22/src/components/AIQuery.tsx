// frontend22/src/components/AIQuery.tsx

import React, { useState } from 'react';
import { apiService } from '../services/api';
import { AIResponse } from '../types/api';

const AIQuery: React.FC = () => {
  const [query, setQuery] = useState('');
  const [response, setResponse] = useState<AIResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;

    setLoading(true);
    setError('');
    setResponse(null);

    try {
      const result = await apiService.askAI(query);
      setResponse(result);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to get AI response');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
      <div className="bg-spotify-surface border border-spotify-surfaceLight shadow-lg rounded-lg">
        <div className="px-4 py-5 sm:p-6">
          <h3 className="text-lg leading-6 font-medium text-spotify-green mb-4">
            Ask IPL AI Assistant
          </h3>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="query" className="block text-sm font-medium text-spotify-textSecondary">
                Your Question
              </label>
              <div className="mt-1">
                <textarea
                  id="query"
                  name="query"
                  rows={3}
                  className="shadow-sm focus:ring-spotify-green focus:border-spotify-green block w-full sm:text-sm bg-spotify-dark border-spotify-surfaceLight text-spotify-text rounded-md"
                  placeholder="Ask about team performance, match predictions, player stats, or head-to-head records..."
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                />
              </div>
            </div>

            <div>
              <button
                type="submit"
                disabled={loading || !query.trim()}
                className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-full text-black bg-spotify-green hover:bg-spotify-greenHover focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-spotify-green disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {loading ? 'Thinking...' : 'Ask AI'}
              </button>
            </div>
          </form>

          {error && (
            <div className="mt-4 bg-red-900 border border-red-500 text-red-200 px-4 py-3 rounded">
              {error}
            </div>
          )}

          {response && (
            <div className="mt-6">
              {response.error ? (
                <div className="bg-red-900 border border-red-500 rounded-md p-4">
                  <div className="text-sm text-red-200">
                    {response.error}
                  </div>
                </div>
              ) : (
                <div className="bg-spotify-dark border border-spotify-surfaceLight rounded-md p-4">
                  <div className="text-sm text-spotify-text">
                    <strong className="text-spotify-green">AI Response:</strong>
                    <div className="mt-2 whitespace-pre-wrap">
                      {response.response || response.answer}
                    </div>
                  </div>
                  {response.rawData && (
                    <details className="mt-4">
                      <summary className="cursor-pointer text-sm text-spotify-green hover:text-spotify-greenHover">
                        View Raw Data
                      </summary>
                      <pre className="mt-2 text-xs bg-spotify-surface p-2 rounded overflow-x-auto text-spotify-text">
                        {JSON.stringify(response.rawData, null, 2)}
                      </pre>
                    </details>
                  )}
                </div>
              )}
            </div>
          )}

          <div className="mt-6 border-t border-spotify-surfaceLight pt-4">
            <h4 className="text-sm font-medium text-spotify-text mb-2">Example Questions:</h4>
            <ul className="text-sm text-spotify-textSecondary space-y-1">
              <li>• Who will win CSK vs MI?</li>
              <li>• Why is RCB struggling this season?</li>
              <li>• What's the head-to-head record between KKR and SRH?</li>
              <li>• Which team has the best bowling attack?</li>
              <li>• Predict the winner of today's match</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AIQuery;