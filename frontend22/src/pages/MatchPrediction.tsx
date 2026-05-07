// frontend22/src/pages/MatchPrediction.tsx

import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate, useParams } from 'react-router-dom';
import { apiService } from '../services/api';
import { Match, Prediction, HeadToHead, VenueStats, Question, UserResponse, MatchLeaderboardEntryDTO } from '../types/api';

// Team logo component and supporting functions
const teamColors: Record<string, { bg: string; text: string; border: string }> = {
  'Mumbai Indians': { bg: '#004BA0', text: '#ffffff', border: '#D1AB0E' },
  'Chennai Super Kings': { bg: '#F2C311', text: '#000000', border: '#17459E' },
  'Royal Challengers Bangalore': { bg: '#ED1C24', text: '#ffffff', border: '#000000' },
  'Kolkata Knight Riders': { bg: '#2E2E3A', text: '#B99C40', border: '#B99C40' },
  'Delhi Capitals': { bg: '#0093D0', text: '#ffffff', border: '#F44336' },
  'Sunrisers Hyderabad': { bg: '#FF110D', text: '#FF8F00', border: '#FF8F00' },
  'Rajasthan Royals': { bg: '#DA291C', text: '#ffffff', border: '#EF3340' },
  'Punjab Kings': { bg: '#ED1C24', text: '#ffffff', border: '#F2A902' },
  'Lucknow Super Giants': { bg: '#004B8D', text: '#D1AB0E', border: '#D1AB0E' },
  'Gujarat Titans': { bg: '#00203FFF', text: '#ADEFD1', border: '#ADEFD1' },
};

const getTeamColors = (teamName: string) => {
  return teamColors[teamName] || { bg: '#282828', text: '#ffffff', border: '#1DB954' };
};

const TeamLogo: React.FC<{ name: string; shortName: string; size?: 'normal' | 'large'; logoUrl?: string }> = ({ name, shortName, size = 'normal', logoUrl }) => {
  const colors = getTeamColors(name);
  const sizeClasses = size === 'large' ? 'w-16 h-16' : 'w-12 h-12';
  const [imgError, setImgError] = React.useState(false);

  if (logoUrl && !imgError) {
    return (
      <img
        src={logoUrl}
        alt={shortName}
        className={`${sizeClasses} object-contain`}
        onError={() => setImgError(true)}
      />
    );
  }

  return (
    <div
      className={`${sizeClasses} rounded-full flex items-center justify-center font-bold shadow-lg`}
      style={{
        backgroundColor: colors.bg,
        color: colors.text,
        border: `2px solid ${colors.border}`
      }}
    >
      {shortName}
    </div>
  );
};

type TabType = 'venue' | 'form' | 'headtohead' | 'quiz' | 'leaderboard';

const MatchPrediction: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { matchId } = useParams<{ matchId: string }>();
  const [match, setMatch] = useState<Match | null>(null);
  const [existingPrediction, setExistingPrediction] = useState<Prediction | null>(null);
  const [headToHead, setHeadToHead] = useState<HeadToHead | null>(null);
  const [venueStats, setVenueStats] = useState<VenueStats | null>(null);
  const [activeTab, setActiveTab] = useState<TabType>('venue');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [quizAnswers, setQuizAnswers] = useState<{ [key: string]: string }>({});
  const [quizSubmitted, setQuizSubmitted] = useState(false);
  const [quizQuestions, setQuizQuestions] = useState<Question[]>([]);
  const [loadingQuestions, setLoadingQuestions] = useState(false);
  const [userResponses, setUserResponses] = useState<UserResponse | null>(null);
  const [showResponses, setShowResponses] = useState(false);
  const [leaderboard, setLeaderboard] = useState<MatchLeaderboardEntryDTO[]>([]);
  const [loadingLeaderboard, setLoadingLeaderboard] = useState(false);

  const getDefaultQuestionsForMatch = (match: Match): Question[] => {
    return [
      { 
        id: 1, 
        matchId: match.id, 
        questionText: 'Who will win the toss today?', 
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
         questionText: 'Will at least one player score a century today?',
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

  const loadMatchData = useCallback(async () => {
    if (!matchId) return;
    
    try {
      setLoading(true);
      const matchData = await apiService.getMatchById(parseInt(matchId));
      setMatch(matchData);

      if (matchData.venueStats) {
        setVenueStats(matchData.venueStats);
      }

      if (user) {
        try {
          const pred = await apiService.getUserMatchPrediction(user.id, parseInt(matchId));
          setExistingPrediction(pred);
        } catch (error) {
          console.log('Failed to load user prediction, assuming none exists:', error);
          // Explicitly set to null to ensure prediction buttons show
          setExistingPrediction(null);
        }

        try {
          const quizStatus = await apiService.getQuizStatus(user.id, parseInt(matchId));
          setQuizSubmitted(quizStatus.submitted || false);
        } catch (error) {
          console.log('Failed to load quiz status, assuming not submitted:', error);
          // Explicitly set to false to ensure quiz options show
          setQuizSubmitted(false);
        }
      }

      if (matchData.homeTeamId && matchData.awayTeamId && matchData.homeTeamName && matchData.awayTeamName) {
        try {
          await apiService.importH2hStats();
        } catch (e) {
          console.log('H2H import error:', e);
        }
        
        try {
          console.log('Loading h2h for:', matchData.homeTeamId, matchData.awayTeamId);
          const h2h = await apiService.getHeadToHead(matchData.homeTeamId, matchData.awayTeamId);
          console.log('H2H response:', h2h);
          setHeadToHead(h2h);
        } catch (e) {
          console.log('H2H load error:', e);
          setHeadToHead(null);
        }

        try {
          setLoadingQuestions(true);
          const questions = await apiService.getQuizQuestionsForMatch(parseInt(matchId));
          if (questions && questions.length > 0) {
            setQuizQuestions(questions);
          } else if (matchData) {
            setQuizQuestions(getDefaultQuestionsForMatch(matchData));
          }
        } catch (e) {
          console.log('Quiz questions load error:', e);
          if (matchData) {
            setQuizQuestions(getDefaultQuestionsForMatch(matchData));
          }
        }
      } else {
        console.log('Missing team IDs:', matchData.homeTeamId, matchData.awayTeamId, matchData.homeTeamName, matchData.awayTeamName);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load match');
    } finally {
      setLoading(false);
      setLoadingQuestions(false);
    }
  }, [matchId, user]);

  const loadLeaderboard = useCallback(async () => {
    if (!match) return;
    try {
      setLoadingLeaderboard(true);
      const leaderboardData = await apiService.getMatchLeaderboard(match.id);
      setLeaderboard(leaderboardData);
    } catch (error) {
      console.log('Failed to load leaderboard:', error);
    } finally {
      setLoadingLeaderboard(false);
    }
  }, [match]);

  useEffect(() => {
    loadMatchData();
  }, [loadMatchData]);

  useEffect(() => {
    if (activeTab === 'leaderboard' && match && leaderboard.length === 0) {
      loadLeaderboard();
    }
  }, [activeTab, match, leaderboard.length, loadLeaderboard]);

  const handlePrediction = async (predictedWinnerId: number) => {
    if (!user || !match) return;

    setSubmitting(true);
    setError('');
    setSuccess('');

    try {
      await apiService.createPrediction({
        userId: user.id,
        matchId: match.id,
        predictedWinnerId,
      });
      setSuccess('Prediction submitted successfully!');
      const pred = await apiService.getUserMatchPrediction(user.id, match.id);
      setExistingPrediction(pred);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit prediction');
    } finally {
      setSubmitting(false);
    }
  };

  const handleQuizSubmit = async () => {
    if (!user || !match) return;
    if (quizQuestions.length === 0) {
      setError('No quiz questions available');
      return;
    }
    if (Object.keys(quizAnswers).length < quizQuestions.length) {
      setError('Please answer all questions before submitting');
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      await apiService.submitQuizPrediction({
        userId: user.id,
        matchId: match.id,
        answers: quizAnswers,
      });
      setQuizSubmitted(true);
      setSuccess('Quiz submitted successfully! You earned bonus points.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit quiz');
    } finally {
      setSubmitting(false);
    }
  };

  const loadUserResponses = async () => {
    if (!match) return;
    try {
      const responses = await apiService.getUserQuizResponses(match.id);
      setUserResponses(responses);
    } catch (error) {
      console.log('Failed to load user responses:', error);
    }
  };

  const handleViewResponses = async () => {
    // Ensure quiz questions are loaded
    if (quizQuestions.length === 0 && match) {
      try {
        const questions = await apiService.getQuizQuestionsForMatch(match.id);
        if (questions && questions.length > 0) {
          setQuizQuestions(questions);
        } else if (match) {
          setQuizQuestions(getDefaultQuestionsForMatch(match));
        }
      } catch (e) {
        console.log('Quiz questions load error for responses:', e);
        if (match) {
          setQuizQuestions(getDefaultQuestionsForMatch(match));
        }
      }
    }

    if (!userResponses) {
      await loadUserResponses();
    }
    setShowResponses(true);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-spotify-dark flex items-center justify-center">
        <div className="text-xl text-spotify-textSecondary">Loading match...</div>
      </div>
    );
  }

  if (!match) {
    return (
      <div className="min-h-screen bg-spotify-dark flex items-center justify-center">
        <div className="text-xl text-red-400">Match not found</div>
      </div>
    );
  }

  const isUpcoming = match.matchStatus === 'UPCOMING' || match.matchStatus === 'SCHEDULED';
  const isLive = match.matchStatus === 'LIVE';
  const isCompleted = match.matchStatus === 'COMPLETED';
  const now = Date.now();
  const predictionCloseTime = match.matchDate - (30 * 60 * 1000);
  const isPredictionOpen = !isCompleted && !isLive && now < predictionCloseTime;
  const hasPredicted = existingPrediction && existingPrediction.predictedWinnerId !== null;

  // Read-only mode for completed matches (accessed from My Predictions page)
  const isReadOnlyView = isCompleted;

  const matchDate = new Date(match.matchDate);
  const today = new Date();
  const isTodayMatch = matchDate.toDateString() === today.toDateString() || isLive;

  const tabs = [
    { id: 'venue' as TabType, name: 'Venue Stats', current: activeTab === 'venue' },
    { id: 'form' as TabType, name: 'Current Form', current: activeTab === 'form' },
    { id: 'headtohead' as TabType, name: 'Head to Head', current: activeTab === 'headtohead' },
    ...(isTodayMatch ? [{ id: 'quiz' as TabType, name: 'Answer the Quiz', current: activeTab === 'quiz' }] : []),
    ...(true ? [{ id: 'leaderboard' as TabType, name: 'Leaderboard', current: activeTab === 'leaderboard' }] : []),
  ];

  return (
    <div className="min-h-screen bg-spotify-dark">
       <header className="bg-spotify-surface border-b border-spotify-surfaceLight">
         <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
           <div className="flex justify-between items-center py-4 sm:py-6">
             <div className="flex items-center">
               <h1 className="text-xl sm:text-3xl font-bold text-spotify-green truncate">Make Prediction</h1>
             </div>
              <button
                onClick={() => navigate(isReadOnlyView ? '/predictions' : '/dashboard')}
                className="bg-spotify-surfaceLight hover:bg-spotify-surfaceHover text-spotify-text px-3 sm:px-4 py-2 rounded-full text-xs sm:text-sm font-medium whitespace-nowrap"
              >
                {isReadOnlyView ? 'Back to Predictions' : 'Back to Dashboard'}
              </button>
           </div>
         </div>
       </header>

      <main className="max-w-3xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
        {error && (
          <div className="mb-6 bg-red-900 border border-red-500 text-red-200 px-4 py-3 rounded">
            {error}
          </div>
        )}

        {success && (
          <div className="mb-6 bg-spotify-green bg-opacity-20 border border-spotify-green text-spotify-green px-4 py-3 rounded">
            {success}
          </div>
        )}

        <div className="bg-spotify-surface border border-spotify-surfaceLight rounded-lg overflow-hidden mx-auto max-w-2xl sm:max-w-3xl">
          <div className="p-4 sm:p-8">
            {/* Match Status & Date */}
            <div className="flex flex-col sm:flex-row items-start justify-between mb-4 sm:mb-6 gap-2">
              <div>
                <span className={`px-2 sm:px-3 py-1 text-xs sm:text-sm font-medium rounded-full ${
                  match.matchStatus === 'COMPLETED' ? 'bg-spotify-green text-black' :
                  match.matchStatus === 'LIVE' ? 'bg-red-500 text-white' :
                  'bg-yellow-500 text-black'
                }`}>
                  {match.matchStatus}
                </span>
              </div>
              <div className="text-xs sm:text-sm text-spotify-textMuted text-right">
                {new Date(match.matchDate).toLocaleDateString()} at {match.venue}
              </div>
            </div>

             <div className="text-center mb-8">
               <div className="flex flex-col items-center justify-center mb-4">
                 <div className="flex items-center space-x-3 sm:space-x-6 mb-2">
                   {match.homeTeamLogoUrl && (
                     <img src={match.homeTeamLogoUrl} alt={match.homeTeamShortName} className="w-16 h-16 sm:w-20 sm:h-20 object-contain" />
                   )}
                   <div className="flex items-center space-x-2">
                     <span className="text-xl sm:text-2xl font-bold text-spotify-text">{match.homeTeamShortName}</span>
                     <span className="text-spotify-textMuted">vs</span>
                     <span className="text-xl sm:text-2xl font-bold text-spotify-text">{match.awayTeamShortName}</span>
                   </div>
                   {match.awayTeamLogoUrl && (
                     <img src={match.awayTeamLogoUrl} alt={match.awayTeamShortName} className="w-16 h-16 sm:w-20 sm:h-20 object-contain" />
                   )}
                 </div>
               </div>
             </div>

            {match.homeWinProbability && match.awayWinProbability && (
              <div className="mb-6 sm:mb-8">
                <p className="text-xs sm:text-sm text-spotify-textSecondary mb-3">Win Probability:</p>
                <div className="flex flex-col sm:flex-row items-center gap-3 sm:gap-4">
                  <div className="w-full sm:flex-1">
                    <div className="flex justify-between text-xs sm:text-sm text-spotify-text mb-1">
                      <span>{match.homeTeamShortName}</span>
                      <span>{match.homeWinProbability}%</span>
                    </div>
                    <div className="w-full bg-spotify-surfaceLight rounded-full h-2">
                      <div 
                        className="bg-spotify-green h-2 rounded-full" 
                        style={{ width: `${match.homeWinProbability}%` }}
                      />
                    </div>
                  </div>
                  <div className="w-full sm:flex-1">
                    <div className="flex justify-between text-xs sm:text-sm text-spotify-text mb-1">
                      <span>{match.awayTeamShortName}</span>
                      <span>{match.awayWinProbability}%</span>
                    </div>
                    <div className="w-full bg-spotify-surfaceLight rounded-full h-2">
                      <div 
                        className="bg-spotify-green h-2 rounded-full" 
                        style={{ width: `${match.awayWinProbability}%` }}
                      />
                    </div>
                  </div>
                </div>
              </div>
            )}

            {match.result && (
              <div className="mb-8 p-4 bg-spotify-dark rounded-lg text-center">
                <p className="text-lg text-spotify-text">
                  <span className="text-spotify-green font-bold">Result:</span> {match.result}
                </p>
              </div>
            )}

            {hasPredicted && (!isUpcoming || isReadOnlyView) && (
              <div className="mb-8 p-4 bg-spotify-dark rounded-lg text-center">
                <div className="flex items-center justify-center mb-2">
                  <span className="text-sm font-semibold text-spotify-text mr-3">Your Prediction:</span>
                  <div className="flex items-center">
                    <TeamLogo
                      name={existingPrediction?.predictedWinnerId === match.homeTeamId
                        ? match.homeTeamName
                        : match.awayTeamName}
                      shortName={existingPrediction?.predictedWinnerId === match.homeTeamId
                        ? match.homeTeamShortName
                        : match.awayTeamShortName}
                      logoUrl={existingPrediction?.predictedWinnerId === match.homeTeamId
                        ? match.homeTeamLogoUrl
                        : match.awayTeamLogoUrl}
                    />
                    <span className="text-sm font-bold text-spotify-text ml-2">
                      {existingPrediction?.predictedWinnerId === match.homeTeamId
                        ? match.homeTeamShortName
                        : match.awayTeamShortName}
                    </span>
                  </div>
                </div>
                {existingPrediction?.isCorrect !== undefined && existingPrediction?.isCorrect !== null && (
                  <p className={`text-lg ${
                    existingPrediction.isCorrect ? 'text-spotify-green' : 'text-red-400'
                  }`}>
                    {existingPrediction.isCorrect ? '✓ Correct!' :
                     (existingPrediction.pointsEarned === 0 && match.winnerTeamId) ? '✗ Incorrect' : ''}
                    {existingPrediction.pointsEarned > 0 && ` (+${existingPrediction.pointsEarned} points)`}
                  </p>
                )}
              </div>
            )}

             {isPredictionOpen && !hasPredicted && isTodayMatch && !isReadOnlyView && (
               <div className="text-center">
                 <p className="text-lg text-spotify-textSecondary mb-6">
                   Choose your winner:
                 </p>
                 <div className="flex flex-col sm:flex-row gap-3 sm:gap-4">
                   <button
                     onClick={() => handlePrediction(match.homeTeamId)}
                     disabled={submitting}
                     className="flex-1 bg-spotify-surfaceLight hover:bg-spotify-greenHover text-white py-4 px-6 rounded-2xl text-lg font-bold disabled:opacity-50 flex flex-col items-center justify-center transition-transform duration-200 hover:scale-[1.02] active:scale-95"
                   >
                     {match.homeTeamLogoUrl && (
                       <img src={match.homeTeamLogoUrl} alt={match.homeTeamShortName} className="w-16 h-16 object-contain mb-2" />
                     )}
                     <span className="text-sm sm:text-base">{match.homeTeamShortName}</span>
                   </button>
                   <button
                     onClick={() => handlePrediction(match.awayTeamId)}
                     disabled={submitting}
                     className="flex-1 bg-spotify-surfaceLight hover:bg-spotify-greenHover text-white py-4 px-6 rounded-2xl text-lg font-bold disabled:opacity-50 flex flex-col items-center justify-center transition-transform duration-200 hover:scale-[1.02] active:scale-95"
                   >
                     {match.awayTeamLogoUrl && (
                       <img src={match.awayTeamLogoUrl} alt={match.awayTeamShortName} className="w-16 h-16 object-contain mb-2" />
                     )}
                     <span className="text-sm sm:text-base">{match.awayTeamShortName}</span>
                   </button>
                 </div>
               </div>
             )}

            {!isPredictionOpen && !hasPredicted && (
              <div className="text-center p-4 bg-spotify-dark rounded-lg">
                <p className="text-spotify-textSecondary">
                  {isCompleted ? 'This match has ended. Predictions are closed.' : 'This match is live. Predictions are closed.'}
                </p>
              </div>
            )}

            {isUpcoming && !isTodayMatch && (
              <div className="text-center p-4 bg-spotify-dark rounded-lg">
                <p className="text-spotify-textSecondary">
                  Predictions and quiz are only available on the day of the match.
                </p>
              </div>
            )}
          </div>
        </div>

        <div className="mt-8 bg-spotify-surface border border-spotify-surfaceLight rounded-lg overflow-hidden">
          <div className="border-b border-spotify-surfaceLight">
            <nav className="tab-scroll flex flex-nowrap justify-start overflow-x-auto whitespace-nowrap space-x-2 sm:overflow-visible sm:whitespace-normal sm:justify-center sm:space-x-8" aria-label="Tabs">
              {tabs.map((tab, index) => (
                <div key={tab.id} className="flex items-center">
                  <button
                    onClick={() => setActiveTab(tab.id)}
                    className={`flex-shrink-0 py-4 px-3 sm:px-4 border-b-2 font-medium text-xs sm:text-sm transition-colors ${
                      tab.current
                        ? 'border-spotify-green text-spotify-green'
                        : 'border-transparent text-spotify-textMuted hover:text-spotify-text'
                    }`}
                  >
                    {tab.name}
                  </button>
                  {index < tabs.length - 1 && (
                    <span className="hidden sm:block h-6 w-px bg-spotify-surfaceLight mx-1" />
                  )}
                </div>
              ))}
            </nav>
          </div>

          <div className="p-6">
             {activeTab === 'venue' && venueStats && (
              <div>
                <h3 className="text-lg font-medium text-spotify-text mb-4">{venueStats.stadium}, {venueStats.city}</h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="bg-spotify-dark p-4 rounded-lg">
                    <p className="text-sm text-spotify-textMuted mb-1">Pitch Type</p>
                    <p className="text-xl font-bold text-spotify-text capitalize">{venueStats.pitchType}</p>
                  </div>
                  <div className="bg-spotify-dark p-4 rounded-lg">
                    <p className="text-sm text-spotify-textMuted mb-1">Average Score</p>
                    <p className="text-xl font-bold text-spotify-green">{venueStats.avgScore}</p>
                  </div>
                  <div className="bg-spotify-dark p-4 rounded-lg">
                    <p className="text-sm text-spotify-textMuted mb-1">Chasing Win %</p>
                    <p className="text-xl font-bold text-spotify-text">{venueStats.chasingWinPct}%</p>
                  </div>
                  <div className="bg-spotify-dark p-4 rounded-lg">
                    <p className="text-sm text-spotify-textMuted mb-1">Dew Factor</p>
                    <p className="text-xl font-bold text-spotify-text capitalize">{venueStats.dewFactor}</p>
                  </div>
                  <div className="bg-spotify-dark p-4 rounded-lg col-span-1 sm:col-span-2">
                    <p className="text-sm text-spotify-textMuted mb-1">Boundary Size</p>
                    <p className="text-lg font-medium text-spotify-text capitalize">{venueStats.boundarySize}</p>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'venue' && !venueStats && (
              <div className="text-center">
                <h3 className="text-lg font-medium text-spotify-text mb-4">{match.venue}</h3>
                <div className="grid grid-cols-2 gap-4">
                  <div className="bg-spotify-dark p-4 rounded-lg">
                    <p className="text-sm text-spotify-textMuted mb-1">Match Number</p>
                    <p className="text-xl font-bold text-spotify-text">#{match.matchNumber}</p>
                  </div>
                  <div className="bg-spotify-dark p-4 rounded-lg">
                    <p className="text-sm text-spotify-textMuted mb-1">Match Type</p>
                    <p className="text-xl font-bold text-spotify-text">{match.matchType}</p>
                  </div>
                  <div className="bg-spotify-dark p-4 rounded-lg col-span-2">
                    <p className="text-sm text-spotify-textMuted mb-1">Date & Time</p>
                    <p className="text-lg font-medium text-spotify-text">
                      {new Date(match.matchDate).toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })} at {new Date(match.matchDate).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}
                    </p>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'form' && (
              <div className="text-center">
                <h3 className="text-lg font-medium text-spotify-text mb-4">Recent Form</h3>
                <div className="space-y-4">
                  <div className="bg-spotify-dark p-4 rounded-lg">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-spotify-text font-medium">{match.homeTeamShortName}</span>
                      <span className="text-spotify-green font-bold">Home</span>
                    </div>
                    <p className="text-sm text-spotify-textMuted">
                      Win Probability: {match.homeWinProbability || 50}%
                    </p>
                  </div>
                  <div className="bg-spotify-dark p-4 rounded-lg">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-spotify-text font-medium">{match.awayTeamShortName}</span>
                      <span className="text-spotify-green font-bold">Away</span>
                    </div>
                    <p className="text-sm text-spotify-textMuted">
                      Win Probability: {match.awayWinProbability || 50}%
                    </p>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'headtohead' && headToHead && (
              <div className="text-center">
                <h3 className="text-lg font-medium text-spotify-text mb-4">Head to Head Record</h3>
                <div className="text-center mb-4">
                  <p className="text-3xl font-bold text-spotify-text">
                    {headToHead.team1Wins} - {headToHead.team2Wins}
                  </p>
                  <p className="text-spotify-textMuted">{headToHead.team1Name} - {headToHead.team2Name}</p>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="bg-spotify-dark p-4 rounded-lg text-center">
                    <p className="text-2xl font-bold text-spotify-green">{headToHead.team1Wins}</p>
                    <p className="text-sm text-spotify-textMuted">{match.homeTeamShortName} wins</p>
                  </div>
                  <div className="bg-spotify-dark p-4 rounded-lg text-center">
                    <p className="text-2xl font-bold text-spotify-green">{headToHead.team2Wins}</p>
                    <p className="text-sm text-spotify-textMuted">{match.awayTeamShortName} wins</p>
                  </div>
                  {headToHead.draws > 0 && (
                    <div className="bg-spotify-dark p-4 rounded-lg text-center col-span-2">
                      <p className="text-2xl font-bold text-spotify-textMuted">{headToHead.draws}</p>
                      <p className="text-sm text-spotify-textMuted">Draws</p>
                    </div>
                  )}
                </div>
              </div>
            )}

            {activeTab === 'headtohead' && !headToHead && (
              <div className="text-center py-4">
                <p className="text-spotify-textMuted">No head-to-head data available</p>
              </div>
            )}

            {activeTab === 'quiz' && (
              <div className="text-center py-4">
                <p className="text-spotify-textMuted mb-4">Answer the quiz in the section below to earn bonus points!</p>
                <div className="bg-spotify-dark p-4 rounded-lg">
                  <p className="text-spotify-text font-medium">5 Questions - 5 Bonus Points</p>
                </div>
              </div>
            )}

            {activeTab === 'leaderboard' && (
              <div>
                <h3 className="text-lg font-medium text-spotify-text mb-4">Match Leaderboard</h3>
                {loadingLeaderboard ? (
                  <div className="text-center py-4">
                    <p className="text-spotify-textMuted">Loading leaderboard...</p>
                  </div>
                ) : leaderboard.length > 0 ? (
                  <div className="space-y-2">
                    {leaderboard.map((entry, index) => (
                      <div key={entry.userId} className="bg-spotify-dark p-4 rounded-lg flex items-center justify-between">
                        <div className="flex items-center space-x-3">
                          <span className="text-spotify-green font-bold text-lg">#{entry.rank}</span>
                          <div>
                            <p className="text-spotify-text font-medium">{entry.username}</p>
                            <p className="text-spotify-textMuted text-sm">
                              Prediction: {entry.predictionPoints} pts | Quiz: {entry.quizPoints} pts
                            </p>
                          </div>
                        </div>
                        <div className="text-right">
                          <p className="text-spotify-green font-bold text-xl">{entry.totalPoints}</p>
                          <p className="text-spotify-textMuted text-sm">Total Points</p>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-4">
                    <p className="text-spotify-textMuted">No leaderboard data available</p>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {((isPredictionOpen && isTodayMatch) || quizSubmitted) && (
          <div className="mt-8 bg-spotify-surface border border-spotify-surfaceLight rounded-lg overflow-hidden">
            <div className="p-6">
              <div className="flex items-center justify-between mb-6">
                <h3 className="text-xl font-bold text-spotify-green">Answer the Quiz & Win Bonus Points</h3>
                {quizSubmitted && (
                  <span className="px-3 py-1 bg-spotify-green text-black text-sm font-medium rounded-full">
                    Submitted
                  </span>
                )}
              </div>
              
              {!quizSubmitted ? (
                <div className="space-y-6">
                  {loadingQuestions ? (
                    <p className="text-spotify-textMuted">Loading quiz questions...</p>
                  ) : quizQuestions.length > 0 ? (
                    quizQuestions.map((q, index) => {
                      const options = [q.optionA, q.optionB, q.optionC, q.optionD].filter(Boolean);
                      return (
                        <div key={q.id} className="bg-spotify-dark p-4 rounded-lg">
                          <p className="text-spotify-text font-medium mb-3">
                            <span className="text-spotify-green mr-2">{index + 1}.</span>
                            {q.questionText}
                          </p>
                          <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                            {options.map((option) => (
                              <button
                                key={option}
                                onClick={() => setQuizAnswers({ ...quizAnswers, [q.id]: option })}
                                className={`py-2 px-3 rounded-lg text-sm font-medium transition-colors ${
                                  quizAnswers[q.id] === option
                                    ? 'bg-spotify-green text-black'
                                    : 'bg-spotify-surfaceLight text-spotify-text hover:bg-spotify-surfaceHover'
                                }`}
                              >
                                {option}
                              </button>
                            ))}
                          </div>
                        </div>
                      );
                    })
                  ) : (
                    <p className="text-spotify-textMuted">No quiz questions available</p>
                  )}
                  
                   {quizQuestions.length > 0 && (
                     <button
                       onClick={handleQuizSubmit}
                       disabled={submitting || Object.keys(quizAnswers).length < quizQuestions.length}
                       className="w-full bg-spotify-green hover:bg-spotify-greenHover text-black py-3 px-6 rounded-full text-lg font-bold disabled:opacity-50 transition-colors transition-transform duration-200 hover:scale-[1.02]"
                     >
                       {submitting ? 'Submitting...' : 'Submit Quiz Answers'}
                     </button>
                   )}
                </div>
              ) : (
                <div className="text-center py-4">
                  <p className="text-spotify-green font-medium mb-4">
                    Quiz submitted! Check back after the match to see your results.
                  </p>
                  <button
                    onClick={handleViewResponses}
                    className="bg-spotify-surfaceLight hover:bg-spotify-surfaceHover text-spotify-text px-4 py-2 rounded-full text-sm font-medium transition-colors"
                  >
                    View responses
                  </button>
                </div>
              )}
            </div>
          </div>
        )}

        {showResponses && userResponses && (
          <div className="mt-8 bg-spotify-surface border border-spotify-surfaceLight rounded-lg overflow-hidden">
            <div className="p-6">
              <div className="flex items-center justify-between mb-6">
                <h3 className="text-xl font-bold text-spotify-green">Your Quiz Responses</h3>
                <button
                  onClick={() => setShowResponses(false)}
                  className="text-spotify-textMuted hover:text-spotify-text"
                >
                  ✕
                </button>
              </div>
              <div className="space-y-4">
                {userResponses.responses.map((response, index) => {
                  const question = quizQuestions.find(q => q.id.toString() === response.questionId);
                  if (!question) {
                    console.log('Question not found for response:', response.questionId);
                    return null;
                  }

                  const options = [question.optionA, question.optionB, question.optionC, question.optionD].filter(Boolean);
                  const selectedOption = response.selectedOption;
                  let correctOptionText = null;

                  // Find the correct option text based on correctOption letter
                  if (question.correctOption) {
                    const optionIndex = question.correctOption.toUpperCase().charCodeAt(0) - 65; // A=0, B=1, etc.
                    if (optionIndex >= 0 && optionIndex < options.length) {
                      correctOptionText = options[optionIndex];
                    }
                  }

                  const isCorrect = response.isCorrect;

                  return (
                    <div key={response.questionId} className="bg-spotify-dark p-4 rounded-lg">
                      <p className="text-spotify-text font-medium mb-3">
                        <span className="text-spotify-green mr-2">{index + 1}.</span>
                        {question.questionText}
                      </p>
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                        {options.map((option) => {
                          let bgColor = 'bg-spotify-surfaceLight';
                          let textColor = 'text-spotify-text';

                          if (option === selectedOption) {
                            // User's selected option
                            if (isCorrect === true) {
                              // Quiz validated: user was correct - green
                              bgColor = 'bg-spotify-green';
                              textColor = 'text-black';
                            } else if (isCorrect === false && response.pointsEarned !== null) {
                              // Quiz validated: user was wrong - red
                              bgColor = 'bg-red-500';
                              textColor = 'text-white';
                            } else {
                              // Quiz not validated yet - yellow
                              bgColor = 'bg-yellow-500';
                              textColor = 'text-black';
                            }
                          } else if (option === correctOptionText && isCorrect === false && response.pointsEarned !== null) {
                            // Quiz validated: show correct answer in green when user was wrong
                            bgColor = 'bg-spotify-green';
                            textColor = 'text-black';
                          }

                          return (
                            <div
                              key={option}
                              className={`py-2 px-3 rounded-lg text-sm font-medium ${bgColor} ${textColor}`}
                            >
                              {option}
                            </div>
                          );
                        })}
                      </div>
                      {response.pointsEarned !== undefined && (
                        <div className="mt-2 text-xs text-spotify-textMuted">
                          Points earned: {response.pointsEarned}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        )}

        <div className="mt-8 flex justify-center">
          <button
            onClick={() => navigate(isReadOnlyView ? '/predictions' : '/dashboard')}
            className="bg-spotify-surfaceLight hover:bg-spotify-surfaceHover text-spotify-text px-6 py-2 rounded-full text-sm font-medium"
          >
            {isReadOnlyView ? 'Back to Predictions' : 'Go to Dashboard'}
          </button>
        </div>
      </main>
    </div>
  );
};

export default MatchPrediction;