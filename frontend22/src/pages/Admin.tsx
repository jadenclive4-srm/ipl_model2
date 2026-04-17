// frontend22/src/pages/Admin.tsx

import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { apiService } from '../services/api';
import { Match, Prediction, Question, Team, UserPredictionSummary } from '../types/api';

const Admin: React.FC = () => {
  const { user } = useAuth();
  const [importStatus, setImportStatus] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  const [todayMatch, setTodayMatch] = useState<Match | null>(null);
  const [allPredictions, setAllPredictions] = useState<Prediction[]>([]);
  const [quizQuestions, setQuizQuestions] = useState<Question[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [selectedWinner, setSelectedWinner] = useState<string>('');
  const [selectedAnswers, setSelectedAnswers] = useState<Record<string, string>>({});
  const [usersWithPredictions, setUsersWithPredictions] = useState<UserPredictionSummary[]>([]);
  const [filterDate, setFilterDate] = useState<string>('');
  
  const defaultQuizQuestions: Question[] = [
    { id: 1, matchId: 0, questionText: 'How many wickets will fall in the powerplay (first 6 overs)?', optionA: '0-2', optionB: '3-4', optionC: '5-6', optionD: '7+', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
    { id: 2, matchId: 0, questionText: 'What will be the highest individual score in the match?', optionA: 'Under 30', optionB: '30-50', optionC: '50-70', optionD: '70+', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
    { id: 3, matchId: 0, questionText: 'How many boundaries (4s and 6s) will be hit in total?', optionA: 'Under 10', optionB: '10-15', optionC: '15-20', optionD: '20+', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
    { id: 4, matchId: 0, questionText: 'What will be the run rate in the powerplay overs?', optionA: 'Under 6', optionB: '6-7', optionC: '7-8', optionD: '8+', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
    { id: 5, matchId: 0, questionText: 'Will there be a dropped catch in the match?', optionA: 'Yes', optionB: 'No', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
  ];

  // New quiz question form state
  const [newQuestions, setNewQuestions] = useState<Question[]>([
    { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
    { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
    { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
    { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
    { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
  ]);

  // Check if user is admin
  const isAdmin = user?.role === 'ADMIN';

  useEffect(() => {
    if (!isAdmin) return;
    loadTodayMatch();
    loadTeams();
    loadUsersWithPredictions();
  }, [isAdmin]);

  if (!isAdmin) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-xl text-red-600">Access denied. Admin privileges required.</div>
      </div>
    );
  }

  const loadTodayMatch = async () => {
    try {
      const allMatches = await apiService.getAllMatches().catch(() => []);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const tomorrow = new Date(today);
      tomorrow.setDate(tomorrow.getDate() + 1);
      const now = Date.now();
      
      if (todayMatch) {
        const currentMatchInAllMatches = allMatches.find(m => m.id === todayMatch.id);
        if (currentMatchInAllMatches) {
          const currentMatchDate = new Date(currentMatchInAllMatches.matchDate);
          if (currentMatchDate >= today && currentMatchDate < tomorrow) {
            setTodayMatch(currentMatchInAllMatches);
            loadMatchData(currentMatchInAllMatches.id);
            return;
          }
        }
      }
      
      let todayMatchData = null;
      
      for (const m of allMatches) {
        const matchDate = new Date(m.matchDate);
        if (matchDate >= today && matchDate < tomorrow) {
          todayMatchData = m;
          break;
        }
      }
      
      if (!todayMatchData && allMatches.length > 0) {
        for (const m of allMatches) {
          const matchDate = new Date(m.matchDate);
          if (matchDate.getTime() >= now) {
            todayMatchData = m;
            break;
          }
        }
      }
      
      setTodayMatch(todayMatchData);
      if (todayMatchData) {
        loadMatchData(todayMatchData.id);
      }
    } catch (error) {
      console.error('Error loading today match:', error);
    }
  };

  const loadTeams = async () => {
    try {
      const teamsData = await apiService.getAllTeams();
      setTeams(teamsData);
    } catch (error) {
      console.error('Error loading teams:', error);
    }
  };

  const loadUsersWithPredictions = async (date?: string) => {
    try {
      let data: UserPredictionSummary[];
      if (date) {
        data = await apiService.getPredictionsByDate(date);
      } else {
        data = await apiService.getAllUsersWithPredictions();
      }
      setUsersWithPredictions(data);
    } catch (error) {
      console.error('Error loading users with predictions:', error);
    }
  };

  const handleFilterByDate = async () => {
    if (filterDate) {
      await loadUsersWithPredictions(filterDate);
    }
  };

  const loadMatchData = async (matchId: number) => {
    try {
      const [predictions, questions] = await Promise.all([
        apiService.getAllPredictionsForMatch(matchId),
        apiService.getAllQuestionsForMatch(matchId).catch(() => []),
      ]);
      setAllPredictions(predictions);
      
      const loadedQuestions = questions && questions.length > 0 ? questions : defaultQuizQuestions;
      setQuizQuestions(loadedQuestions);
      
      const answers: Record<string, string> = {};
      loadedQuestions.forEach(q => {
        if (q.correctOption) {
          answers[q.id.toString()] = q.correctOption;
        }
      });
      setSelectedAnswers(answers);
    } catch (error) {
      console.error('Error loading match data:', error);
    }
  };

  const handleImportH2h = async () => {
    setLoading(true);
    setError('');
    setImportStatus('');

    try {
      const result = await apiService.importH2hStats();
      setImportStatus(result.message || 'H2H stats imported successfully');
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to import H2H stats');
    } finally {
      setLoading(false);
    }
  };

  const handleFileImport = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setLoading(true);
    setError('');
    setImportStatus('');

    try {
      const result = await apiService.importMatches(file);
      setImportStatus(result.message || 'Matches imported successfully');
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to import matches');
    } finally {
      setLoading(false);
    }
  };

  const handleSetWinner = async () => {
    if (!todayMatch || !selectedWinner) return;
    
    setLoading(true);
    setError('');
    setImportStatus('');

    try {
      await apiService.updateMatchResult(todayMatch.id, {
        winnerTeamName: selectedWinner === 'home' 
          ? todayMatch.homeTeamName 
          : todayMatch.awayTeamName,
      });
      setImportStatus('Match winner set successfully. Predictions evaluated.');
      
      await apiService.evaluatePredictions(todayMatch.id);
      
      const updatedMatch = await apiService.getMatchById(todayMatch.id);
      setTodayMatch(updatedMatch);
      loadMatchData(updatedMatch.id);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to set winner');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateQuestionAnswer = async (questionId: number, correctOption: string) => {
    setSelectedAnswers(prev => ({ ...prev, [questionId.toString()]: correctOption }));
  };

  const handleSaveCorrectAnswers = async () => {
    if (!todayMatch || Object.keys(selectedAnswers).length === 0) return;
    
    setLoading(true);
    setError('');
    setImportStatus('');

    try {
      await apiService.saveCorrectAnswers(todayMatch.id, selectedAnswers);
      setImportStatus('Correct answers saved to database.');
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to save answers');
    } finally {
      setLoading(false);
    }
  };

  const handleEvaluateQuiz = async () => {
    if (!todayMatch) return;
    setLoading(true);
    setError('');
    setImportStatus('');

    try {
      await apiService.evaluateQuizAnswers(todayMatch.id);
      setImportStatus('Quiz answers evaluated and points awarded.');
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to evaluate quiz');
    } finally {
      setLoading(false);
    }
  };

  const handleResetQuiz = async () => {
    if (!todayMatch) return;
    setLoading(true);
    setError('');
    setImportStatus('');

    try {
      await apiService.resetQuizAnswers(todayMatch.id);
      setImportStatus('Quiz answers have been reset.');
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to reset quiz');
    } finally {
      setLoading(false);
    }
  };

  const handleResetAll = async () => {
    if (!todayMatch) return;
    
    setLoading(true);
    setError('');
    setImportStatus('');

    try {
      await apiService.resetPredictions(todayMatch.id);
      await apiService.resetQuizAnswers(todayMatch.id);
      await apiService.resetMatchResult(todayMatch.id);
      setImportStatus('All predictions, quiz answers, and match result have been reset.');
      loadMatchData(todayMatch.id);
      loadUsersWithPredictions();
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to reset');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateNewQuestion = (index: number, field: keyof Question, value: string | number) => {
    setNewQuestions(prev => {
      const updated = [...prev];
      updated[index] = { ...updated[index], [field]: value };
      return updated;
    });
  };

  const handleUploadQuizQuestions = async () => {
    if (!todayMatch) return;
    
    const validQuestions = newQuestions.filter(q => q.questionText && q.optionA && q.optionB);
    if (validQuestions.length === 0) {
      setError('Please add at least one question with options');
      return;
    }
    
    setLoading(true);
    setError('');
    setImportStatus('');

    try {
      const valid = validQuestions.map(q => ({ ...q, matchId: todayMatch.id }));
      await apiService.uploadQuizQuestions(todayMatch.id, valid);
      setImportStatus(`Successfully uploaded ${valid.length} quiz questions.`);
      setNewQuestions([
        { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
        { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
        { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
        { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
        { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
      ]);
      loadMatchData(todayMatch.id);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to upload quiz questions');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-6">
            <div className="flex items-center">
              <h1 className="text-3xl font-bold text-gray-900">Admin Panel</h1>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-gray-700">Welcome, {user?.fullName || user?.username}</span>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        {error && (
          <div className="mb-4 bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
            {error}
          </div>
        )}

        {importStatus && (
          <div className="mb-4 bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded">
            {importStatus}
          </div>
        )}

        {/* Today's Match Section */}
        <div className="bg-white shadow rounded-lg mb-6">
          <div className="px-4 py-5 sm:p-6">
            <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
              Today's Match
            </h3>

            {todayMatch ? (
              <div className="space-y-6">
                <div className="flex items-center justify-between bg-gray-100 p-4 rounded-lg">
                  <div className="flex items-center space-x-4">
                    <span className="text-xl font-bold">{todayMatch.homeTeamShortName}</span>
                    <span className="text-gray-500">vs</span>
                    <span className="text-xl font-bold">{todayMatch.awayTeamShortName}</span>
                  </div>
                  <div className="text-sm text-gray-500">
                    {new Date(todayMatch.matchDate).toLocaleString('en-IN', { 
                      day: 'numeric', 
                      month: 'short',
                      hour: '2-digit',
                      minute: '2-digit'
                    })}
                  </div>
                </div>

                <div className="border-t border-gray-200 pt-4">
                  <h4 className="text-md font-medium text-gray-900 mb-2">Set Match Winner</h4>
                  <div className="flex items-center space-x-4">
                    <select
                      value={selectedWinner}
                      onChange={(e) => setSelectedWinner(e.target.value)}
                      className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md"
                      disabled={loading}
                    >
                      <option value="">Select Winner</option>
                      <option value="home">{todayMatch.homeTeamShortName} (Home)</option>
                      <option value="away">{todayMatch.awayTeamShortName} (Away)</option>
                    </select>
                    <button
                      onClick={handleSetWinner}
                      disabled={loading || !selectedWinner}
                      className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
                    >
                      {loading ? 'Processing...' : 'Set Winner & Evaluate'}
                    </button>
                    <button
                      onClick={handleResetAll}
                      disabled={loading}
                      className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50"
                    >
                      {loading ? 'Resetting...' : 'Reset Predictions'}
                    </button>
                  </div>
                </div>

                {/* Predictions */}
                <div className="border-t border-gray-200 pt-4">
                  <h4 className="text-md font-medium text-gray-900 mb-2">
                    User Predictions ({allPredictions.length})
                  </h4>
                  <div className="mt-2 overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                      <thead className="bg-gray-50">
                        <tr>
                          <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">User</th>
                          <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Prediction</th>
                          <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Confidence</th>
                          <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Result</th>
                          <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Points</th>
                        </tr>
                      </thead>
                      <tbody className="bg-white divide-y divide-gray-200">
                        {allPredictions.map((pred) => (
                          <tr key={pred.id}>
                            <td className="px-3 py-2 whitespace-nowrap text-sm text-gray-900">{pred.username}</td>
                            <td className="px-3 py-2 whitespace-nowrap text-sm text-gray-900">
                              {pred.predictedWinnerName || 'No prediction'}
                            </td>
                            <td className="px-3 py-2 whitespace-nowrap text-sm text-gray-500">
                              {pred.homeProbability && pred.awayProbability 
                                ? `${Math.max(pred.homeProbability, pred.awayProbability)}%`
                                : '-'}
                            </td>
                            <td className="px-3 py-2 whitespace-nowrap">
                              <span className={`px-2 py-1 text-xs font-medium rounded-full ${
                                pred.isCorrect 
                                  ? 'bg-green-100 text-green-800' 
                                  : 'bg-red-100 text-red-800'
                              }`}>
                                {pred.isCorrect ? 'Correct' : 'Wrong'}
                              </span>
                            </td>
                            <td className="px-3 py-2 whitespace-nowrap text-sm font-medium text-gray-900">
                              {pred.pointsEarned || 0}
                            </td>
                          </tr>
                        ))}
                        {allPredictions.length === 0 && (
                          <tr>
                            <td colSpan={5} className="px-3 py-4 text-center text-sm text-gray-500">
                              No predictions yet
                            </td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            ) : (
              <p className="text-gray-500">No match scheduled for today</p>
            )}
          </div>
        </div>

        {/* Quiz Questions Section */}
        <div className="bg-white shadow rounded-lg mb-6">
          <div className="px-4 py-5 sm:p-6">
            <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
              Today's Quiz Questions
            </h3>

            {quizQuestions.length > 0 ? (
              <div className="space-y-4">
                {quizQuestions.map((question, index) => (
                  <div key={question.id} className="border border-gray-200 rounded-lg p-4">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <p className="text-sm font-medium text-gray-900 mb-2">
                          Q{index + 1}: {question.questionText}
                        </p>
                        <div className="grid grid-cols-2 gap-2 mt-2">
                          <button
                            onClick={() => handleUpdateQuestionAnswer(question.id, 'A')}
                            className={`p-2 text-xs rounded border ${
                              selectedAnswers[question.id.toString()] === 'A'
                                ? 'bg-green-100 border-green-500 text-green-800'
                                : 'bg-gray-50 border-gray-200 text-gray-700'
                            }`}
                          >
                            A: {question.optionA}
                          </button>
                          <button
                            onClick={() => handleUpdateQuestionAnswer(question.id, 'B')}
                            className={`p-2 text-xs rounded border ${
                              selectedAnswers[question.id.toString()] === 'B'
                                ? 'bg-green-100 border-green-500 text-green-800'
                                : 'bg-gray-50 border-gray-200 text-gray-700'
                            }`}
                          >
                            B: {question.optionB}
                          </button>
                          {question.optionC && (
                            <button
                              onClick={() => handleUpdateQuestionAnswer(question.id, 'C')}
                              className={`p-2 text-xs rounded border ${
                                selectedAnswers[question.id.toString()] === 'C'
                                  ? 'bg-green-100 border-green-500 text-green-800'
                                  : 'bg-gray-50 border-gray-200 text-gray-700'
                              }`}
                            >
                              C: {question.optionC}
                            </button>
                          )}
                          {question.optionD && (
                            <button
                              onClick={() => handleUpdateQuestionAnswer(question.id, 'D')}
                              className={`p-2 text-xs rounded border ${
                                selectedAnswers[question.id.toString()] === 'D'
                                  ? 'bg-green-100 border-green-500 text-green-800'
                                  : 'bg-gray-50 border-gray-200 text-gray-700'
                              }`}
                            >
                              D: {question.optionD}
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}

                <div className="flex space-x-4 mt-4">
                  <button
                    onClick={handleSaveCorrectAnswers}
                    disabled={loading || Object.keys(selectedAnswers).length === 0}
                    className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
                  >
                    {loading ? 'Saving...' : 'Save to Database'}
                  </button>
                  <button
                    onClick={handleEvaluateQuiz}
                    disabled={loading}
                    className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:opacity-50"
                  >
                    {loading ? 'Evaluating...' : 'Evaluate Quiz'}
                  </button>
                  <button
                    onClick={handleResetQuiz}
                    disabled={loading}
                    className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50"
                  >
                    {loading ? 'Resetting...' : 'Reset Quiz'}
                  </button>
                </div>
              </div>
            ) : (
              <p className="text-gray-500">No quiz questions for today</p>
            )}
          </div>
        </div>

        {/* Data Import Section */}
        <div className="bg-white shadow rounded-lg">
          <div className="px-4 py-5 sm:p-6">
            <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
              Data Import Tools
            </h3>

            <div className="space-y-6">
              <div>
                <h4 className="text-md font-medium text-gray-900 mb-2">Import Matches from Excel</h4>
                <div className="flex items-center space-x-4">
                  <input
                    type="file"
                    accept=".xlsx,.xls"
                    onChange={handleFileImport}
                    disabled={loading}
                    className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
                  />
                </div>
                <p className="mt-1 text-sm text-gray-600">
                  Upload an Excel file containing match data
                </p>
              </div>

              <div className="border-t border-gray-200 pt-6">
                <h4 className="text-md font-medium text-gray-900 mb-2">Import Head-to-Head Statistics</h4>
                <div>
                  <button
                    onClick={handleImportH2h}
                    disabled={loading}
                    className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
                  >
                    {loading ? 'Importing...' : 'Import H2H Stats'}
                  </button>
                </div>
                <p className="mt-1 text-sm text-gray-600">
                  Import head-to-head statistics from the configured CSV file
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* All Users Predictions Table */}
        <div className="bg-white shadow rounded-lg">
          <div className="px-4 py-5 sm:p-6">
            <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
              Users with Predictions
            </h3>

            <div className="flex items-center gap-2 mb-4">
              <input
                type="date"
                value={filterDate}
                onChange={(e) => setFilterDate(e.target.value)}
                className="px-3 py-2 border border-gray-300 rounded-md text-sm"
              />
              <button
                onClick={handleFilterByDate}
                className="px-4 py-2 bg-indigo-600 text-white text-sm rounded-md hover:bg-indigo-700"
              >
                Filter by Date
              </button>
              <button
                onClick={() => { setFilterDate(''); loadUsersWithPredictions(); }}
                className="px-4 py-2 bg-gray-500 text-white text-sm rounded-md hover:bg-gray-600"
              >
                Show All
              </button>
            </div>

            <div className="mt-2 overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">User ID</th>
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Full Name</th>
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Predicted Team</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {usersWithPredictions.map((userPred) => (
                    <tr key={`${userPred.userId}-${userPred.matchId}`}>
                      <td className="px-3 py-2 whitespace-nowrap text-sm text-gray-900">{userPred.userId}</td>
                      <td className="px-3 py-2 whitespace-nowrap text-sm text-gray-900">{userPred.fullName}</td>
                      <td className="px-3 py-2 whitespace-nowrap text-sm text-gray-900">{userPred.predictedTeamName || '-'}</td>
                    </tr>
                  ))}
                  {usersWithPredictions.length === 0 && (
                    <tr>
                      <td colSpan={3} className="px-3 py-4 text-center text-sm text-gray-500">
                        No predictions yet
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Admin;