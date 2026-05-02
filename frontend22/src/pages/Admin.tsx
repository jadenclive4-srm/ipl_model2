// frontend22/src/pages/Admin.tsx

import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { apiService } from '../services/api';
import { Match, Prediction, Question, Team, UserPredictionSummary } from '../types/api';

const Admin: React.FC = () => {
  const { user } = useAuth();
  const [importStatus, setImportStatus] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
   
  const [todayMatches, setTodayMatches] = useState<Match[]>([]);
  const [selectedMatchId, setSelectedMatchId] = useState<number | null>(null);
  const [allPredictions, setAllPredictions] = useState<Prediction[]>([]);
  const [quizQuestions, setQuizQuestions] = useState<Question[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [selectedWinner, setSelectedWinner] = useState<string>('');
  const [selectedAnswers, setSelectedAnswers] = useState<Record<string, string>>({});
  const [usersWithPredictions, setUsersWithPredictions] = useState<UserPredictionSummary[]>([]);
  const [filterDate, setFilterDate] = useState<string>('');

  // User creation state
  const [newUser, setNewUser] = useState({
    username: '',
    email: '',
    password: '',
    fullName: '',
    role: 'USER'
  });

  // Password reset state
  const [resetPasswordData, setResetPasswordData] = useState({
    email: '',
    newPassword: '',
    confirmPassword: ''
  });
  
  const getDefaultQuestionsForMatch = (match: Match): Question[] => {
    return [
      { 
        id: 1, 
        matchId: match.id, 
        questionText: `Who will win the toss today?`, 
        optionA: match.homeTeamShortName || match.homeTeamName, 
        optionB: match.awayTeamShortName || match.awayTeamName, 
        optionC: '', 
        optionD: '', 
        correctOption: '', 
        pointsValue: 10, 
        isActive: true, 
        questionType: 'QUIZ', 
        createdAt: 0 
      },
      { 
        id: 2, 
        matchId: match.id, 
        questionText: 'What will be the highest individual score in the match?', 
        optionA: 'Under 30', 
        optionB: '30-50', 
        optionC: '50-70', 
        optionD: '70+', 
        correctOption: '', 
        pointsValue: 10, 
        isActive: true, 
        questionType: 'QUIZ', 
        createdAt: 0 
      },
      { 
        id: 3, 
        matchId: match.id, 
        questionText: 'How many boundaries (4s and 6s) will be hit in total?', 
        optionA: 'Under 10', 
        optionB: '10-15', 
        optionC: '15-20', 
        optionD: '20+', 
        correctOption: '', 
        pointsValue: 10, 
        isActive: true, 
        questionType: 'QUIZ', 
        createdAt: 0 
      },
      { 
        id: 4, 
        matchId: match.id, 
        questionText: 'What will be the run rate in the powerplay overs?', 
        optionA: 'Under 6', 
        optionB: '6-7', 
        optionC: '7-8', 
        optionD: '8+', 
        correctOption: '', 
        pointsValue: 10, 
        isActive: true, 
        questionType: 'QUIZ', 
        createdAt: 0 
      },
      { 
        id: 5, 
        matchId: match.id, 
        questionText: 'Will there be a dropped catch in the match?', 
        optionA: 'Yes', 
        optionB: 'No', 
        optionC: '', 
        optionD: '', 
        correctOption: '', 
        pointsValue: 10, 
        isActive: true, 
        questionType: 'QUIZ', 
        createdAt: 0 
      },
    ];
  };

  // New quiz question form state
  const [newQuestions, setNewQuestions] = useState<Question[]>([
    { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
    { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
    { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
    { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
    { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
  ]);

  const isAdmin = user?.role === 'ADMIN';

  const selectedMatch = todayMatches.find(m => m.id === selectedMatchId) || null;

  const loadMatchData = useCallback(async (matchId: number) => {
    if (!selectedMatch) return;
    try {
      const [predictions, questions] = await Promise.all([
        apiService.getAllPredictionsForMatch(matchId),
        apiService.getAllQuestionsForMatch(matchId).catch(() => []),
      ]);
      setAllPredictions(predictions);
      
      let loadedQuestions = questions && questions.length > 0 ? questions : [];
      if (loadedQuestions.length === 0) {
        loadedQuestions = getDefaultQuestionsForMatch(selectedMatch);
      }
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
  }, [selectedMatch]);

  const loadTodayMatch = useCallback(async () => {
    try {
      const allMatches = await apiService.getAllMatches().catch(() => []);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const tomorrow = new Date(today);
      tomorrow.setDate(tomorrow.getDate() + 1);
      const now = Date.now();
      
      let todaysMatches: Match[] = [];
      
      for (const m of allMatches) {
        const matchDate = new Date(m.matchDate);
        if (matchDate >= today && matchDate < tomorrow) {
          todaysMatches.push(m);
        }
      }
      
      // Sort by matchDate ascending
      todaysMatches.sort((a, b) => new Date(a.matchDate).getTime() - new Date(b.matchDate).getTime());
      
      setTodayMatches(todaysMatches);
      
      // Set selected match
      if (todaysMatches.length > 0) {
        // Try to keep current selection if still valid
        if (selectedMatchId && todaysMatches.some(m => m.id === selectedMatchId)) {
          // keep existing
        } else {
          // Select first match that hasn't started or is live
          const upcomingMatch = todaysMatches.find(m => 
            new Date(m.matchDate).getTime() > now || m.matchStatus === 'LIVE'
          );
          setSelectedMatchId(upcomingMatch ? upcomingMatch.id : todaysMatches[0].id);
        }
      } else {
        setSelectedMatchId(null);
      }
    } catch (error) {
      console.error('Error loading today match:', error);
    }
  }, [selectedMatchId]);

  const loadTeams = useCallback(async () => {
    try {
      const teamsData = await apiService.getAllTeams();
      setTeams(teamsData);
    } catch (error) {
      console.error('Error loading teams:', error);
    }
  }, []);

  const loadUsersWithPredictions = useCallback(async (date?: string) => {
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
  }, []);

  useEffect(() => {
    if (!isAdmin) return;
    loadTodayMatch();
    loadTeams();
    loadUsersWithPredictions();
  }, [isAdmin, loadTodayMatch, loadTeams, loadUsersWithPredictions]);

  useEffect(() => {
    if (selectedMatchId) {
      loadMatchData(selectedMatchId);
    }
  }, [selectedMatchId, loadMatchData]);

  // Reset form fields when selected match changes
  useEffect(() => {
    setSelectedWinner('');
    setSelectedAnswers({});
    if (selectedMatch) {
      const defaults = getDefaultQuestionsForMatch(selectedMatch);
      // For new questions to be uploaded, keep id=0 to indicate creation
      setNewQuestions(defaults.map(q => ({
        ...q,
        id: 0,
        correctOption: '',
        createdAt: 0
      })));
    } else {
      setNewQuestions([
        { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
        { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
        { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
        { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
        { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
      ]);
    }
  }, [selectedMatchId, selectedMatch]);

  if (!isAdmin) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-xl text-red-600">Access denied. Admin privileges required.</div>
      </div>
    );
  }

  const handleSetWinner = async () => {
    if (!selectedMatch || !selectedWinner) return;
    
    setLoading(true);
    setError('');
    setImportStatus('');

    try {
      await apiService.updateMatchResult(selectedMatch.id, {
        winnerTeamName: selectedWinner === 'home' 
          ? selectedMatch.homeTeamName 
          : selectedMatch.awayTeamName,
      });
      setImportStatus('Match winner set successfully. Predictions evaluated.');
      
      await apiService.evaluatePredictions(selectedMatch.id);
      
      const updatedMatch = await apiService.getMatchById(selectedMatch.id);
      setTodayMatches(prev => prev.map(m => m.id === updatedMatch.id ? updatedMatch : m));
      loadMatchData(updatedMatch.id);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to set winner');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateQuestionAnswer = (questionId: number, correctOption: string) => {
    setSelectedAnswers(prev => ({ ...prev, [questionId.toString()]: correctOption }));
  };

  const handleSaveCorrectAnswers = async () => {
    if (!selectedMatch || Object.keys(selectedAnswers).length === 0) return;
    
    setLoading(true);
    setError('');
    setImportStatus('');

    try {
      await apiService.saveCorrectAnswers(selectedMatch.id, selectedAnswers);
      setImportStatus('Correct answers saved to database.');
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to save answers');
    } finally {
      setLoading(false);
    }
  };

   const handleEvaluateQuiz = async () => {
     if (!selectedMatch) return;
     if (Object.keys(selectedAnswers).length === 0) {
       setError('Please set correct answers for all questions before evaluating.');
       return;
     }
     setLoading(true);
     setError('');
     setImportStatus('');

     try {
       // First save the correct answers to ensure they are in the database
       await apiService.saveCorrectAnswers(selectedMatch.id, selectedAnswers);
       // Then evaluate the quiz
       await apiService.evaluateQuizAnswers(selectedMatch.id);
       setImportStatus('Quiz answers evaluated and points awarded.');
     } catch (error) {
       setError(error instanceof Error ? error.message : 'Failed to evaluate quiz');
     } finally {
       setLoading(false);
     }
   };

  const handleResetQuiz = async () => {
    if (!selectedMatch) return;
    setLoading(true);
    setError('');
    setImportStatus('');

    try {
      await apiService.resetQuizAnswers(selectedMatch.id);
      setImportStatus('Quiz answers have been reset.');
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to reset quiz');
    } finally {
      setLoading(false);
    }
  };

  const handleResetAll = async () => {
    if (!selectedMatch) return;
    
    setLoading(true);
    setError('');
    setImportStatus('');

    try {
      await apiService.resetPredictions(selectedMatch.id);
      await apiService.resetQuizAnswers(selectedMatch.id);
      const resetMatch = await apiService.resetMatchResult(selectedMatch.id);
      setImportStatus('All predictions, quiz answers, and match result have been reset.');
      setTodayMatches(prev => prev.map(m => m.id === resetMatch.id ? resetMatch : m));
      loadMatchData(selectedMatch.id);
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
    if (!selectedMatch) return;
    
    const validQuestions = newQuestions.filter(q => q.questionText && q.optionA && q.optionB);
    if (validQuestions.length === 0) {
      setError('Please add at least one question with options');
      return;
    }
    
    setLoading(true);
    setError('');
    setImportStatus('');

    try {
      const valid = validQuestions.map(q => ({ ...q, matchId: selectedMatch.id }));
      await apiService.uploadQuizQuestions(selectedMatch.id, valid);
      setImportStatus(`Successfully uploaded ${valid.length} quiz questions.`);
      setNewQuestions([
        { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
        { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
        { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
        { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
        { id: 0, matchId: 0, questionText: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: '', pointsValue: 10, isActive: true, questionType: 'QUIZ', createdAt: 0 },
      ]);
      loadMatchData(selectedMatch.id);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to upload quiz questions');
    } finally {
      setLoading(false);
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

  const handleFilterByDate = async () => {
    if (filterDate) {
      await loadUsersWithPredictions(filterDate);
    }
  };

  const handleCreateUser = async () => {
    if (!newUser.username || !newUser.email || !newUser.password) {
      setError('Username, email, and password are required');
      return;
    }

    setLoading(true);
    setError('');
    setImportStatus('');

    try {
      const result = await apiService.createUser(newUser);
      setImportStatus(result.message);
      setNewUser({
        username: '',
        email: '',
        password: '',
        fullName: '',
        role: 'USER'
      });
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to create user');
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async () => {
    if (!resetPasswordData.email || !resetPasswordData.newPassword) {
      setError('Email and new password are required');
      return;
    }

    if (resetPasswordData.newPassword !== resetPasswordData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setLoading(true);
    setError('');
    setImportStatus('');

    try {
      const result = await apiService.resetPassword(resetPasswordData.email, resetPasswordData.newPassword);
      setImportStatus(result.message);
      setResetPasswordData({
        email: '',
        newPassword: '',
        confirmPassword: ''
      });
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to reset password');
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
               <h1 className="text-lg font-bold text-gray-900 sm:text-xl md:text-2xl">Admin Panel</h1>
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

        {/* Today's Matches Section */}
        <div className="bg-white shadow rounded-lg mb-6">
          <div className="px-4 py-5 sm:p-6">
            <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
              Today's Matches
            </h3>

            {/* Match Selector */}
            {todayMatches.length > 0 && (
              <div className="mb-4">
                <div className="flex gap-4 overflow-x-auto pb-2">
                  {todayMatches.map(match => (
                    <div 
                      key={match.id}
                      onClick={() => setSelectedMatchId(match.id)}
                      className={`flex-shrink-0 cursor-pointer rounded-lg p-3 border-2 transition-all ${
                        selectedMatchId === match.id 
                          ? 'border-indigo-500 bg-indigo-50' 
                          : 'border-gray-200 hover:border-gray-300 bg-white'
                      }`}
                    >
                      <div className="flex items-center space-x-2">
                        <span className="font-bold">{match.homeTeamShortName}</span>
                        <span className="text-gray-500">vs</span>
                        <span className="font-bold">{match.awayTeamShortName}</span>
                      </div>
                      <div className="text-xs text-gray-500 mt-1">
                        {new Date(match.matchDate).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}
                      </div>
                      <div className="mt-1">
                        <span className={`text-xs px-2 py-0.5 rounded-full ${
                          match.matchStatus === 'COMPLETED' ? 'bg-green-100 text-green-800' :
                          match.matchStatus === 'LIVE' ? 'bg-red-100 text-red-800' :
                          'bg-yellow-100 text-yellow-800'
                        }`}>
                          {match.matchStatus}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {selectedMatch ? (
              <div className="space-y-6">
                <div className="flex items-center justify-between bg-gray-100 p-4 rounded-lg">
                  <div className="flex items-center space-x-4">
                    <span className="text-xl font-bold">{selectedMatch.homeTeamShortName}</span>
                    <span className="text-gray-500">vs</span>
                    <span className="text-xl font-bold">{selectedMatch.awayTeamShortName}</span>
                  </div>
                  <div className="text-sm text-gray-500">
                    {new Date(selectedMatch.matchDate).toLocaleString('en-IN', { 
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
                      <option value="home">{selectedMatch.homeTeamShortName} (Home)</option>
                      <option value="away">{selectedMatch.awayTeamShortName} (Away)</option>
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
              {selectedMatch ? `Quiz Questions: ${selectedMatch.homeTeamShortName} vs ${selectedMatch.awayTeamShortName}` : 'Today\'s Quiz Questions'}
            </h3>

            {selectedMatch && quizQuestions.length > 0 ? (
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

        {/* User Management Section */}
        <div className="bg-white shadow rounded-lg mb-6">
          <div className="px-4 py-5 sm:p-6">
            <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
              User Management
            </h3>

            <div className="space-y-4">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label htmlFor="username" className="block text-sm font-medium text-gray-700">
                    Username *
                  </label>
                  <input
                    type="text"
                    id="username"
                    value={newUser.username}
                    onChange={(e) => setNewUser(prev => ({ ...prev, username: e.target.value }))}
                    className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="Enter username"
                    disabled={loading}
                  />
                </div>

                <div>
                  <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                    Email *
                  </label>
                  <input
                    type="email"
                    id="email"
                    value={newUser.email}
                    onChange={(e) => setNewUser(prev => ({ ...prev, email: e.target.value }))}
                    className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="Enter email address"
                    disabled={loading}
                  />
                </div>

                <div>
                  <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                    Password *
                  </label>
                  <input
                    type="password"
                    id="password"
                    value={newUser.password}
                    onChange={(e) => setNewUser(prev => ({ ...prev, password: e.target.value }))}
                    className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="Enter password"
                    disabled={loading}
                  />
                </div>

                <div>
                  <label htmlFor="fullName" className="block text-sm font-medium text-gray-700">
                    Full Name
                  </label>
                  <input
                    type="text"
                    id="fullName"
                    value={newUser.fullName}
                    onChange={(e) => setNewUser(prev => ({ ...prev, fullName: e.target.value }))}
                    className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="Enter full name"
                    disabled={loading}
                  />
                </div>

                <div>
                  <label htmlFor="role" className="block text-sm font-medium text-gray-700">
                    Role
                  </label>
                  <select
                    id="role"
                    value={newUser.role}
                    onChange={(e) => setNewUser(prev => ({ ...prev, role: e.target.value }))}
                    className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    disabled={loading}
                  >
                    <option value="USER">USER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </div>
              </div>

              <div className="flex justify-end">
                <button
                  onClick={handleCreateUser}
                  disabled={loading || !newUser.username || !newUser.email || !newUser.password}
                  className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:opacity-50"
                >
                  {loading ? 'Creating...' : 'Create User'}
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Password Reset Section */}
        <div className="bg-white shadow rounded-lg mb-6">
          <div className="px-4 py-5 sm:p-6">
            <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
              Password Reset (Admin Only)
            </h3>

            <div className="space-y-4">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                <div>
                  <label htmlFor="resetEmail" className="block text-sm font-medium text-gray-700">
                    User Email *
                  </label>
                  <input
                    type="email"
                    id="resetEmail"
                    value={resetPasswordData.email}
                    onChange={(e) => setResetPasswordData(prev => ({ ...prev, email: e.target.value }))}
                    className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="Enter user email"
                    disabled={loading}
                  />
                </div>

                <div>
                  <label htmlFor="resetPassword" className="block text-sm font-medium text-gray-700">
                    New Password *
                  </label>
                  <input
                    type="password"
                    id="resetPassword"
                    value={resetPasswordData.newPassword}
                    onChange={(e) => setResetPasswordData(prev => ({ ...prev, newPassword: e.target.value }))}
                    className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="Enter new password"
                    disabled={loading}
                  />
                </div>

                <div>
                  <label htmlFor="confirmResetPassword" className="block text-sm font-medium text-gray-700">
                    Confirm Password *
                  </label>
                  <input
                    type="password"
                    id="confirmResetPassword"
                    value={resetPasswordData.confirmPassword}
                    onChange={(e) => setResetPasswordData(prev => ({ ...prev, confirmPassword: e.target.value }))}
                    className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    placeholder="Confirm new password"
                    disabled={loading}
                  />
                </div>
              </div>

              <div className="flex justify-end">
                <button
                  onClick={handleResetPassword}
                  disabled={loading || !resetPasswordData.email || !resetPasswordData.newPassword || !resetPasswordData.confirmPassword}
                  className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50"
                >
                  {loading ? 'Resetting...' : 'Reset Password'}
                </button>
              </div>

              <div className="mt-2 text-sm text-gray-600">
                <p>⚠️ <strong>Admin Only:</strong> This resets the user's password without requiring their current password. Use with caution.</p>
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
