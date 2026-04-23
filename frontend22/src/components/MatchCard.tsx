// frontend22/src/components/MatchCard.tsx

import React from 'react';
import { Match, Prediction } from '../types/api';

interface MatchCardProps {
  match: Match;
  userPrediction?: Prediction;
  onPredictClick?: () => void;
  isLarge?: boolean;
}

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
  const sizeClasses = size === 'large' ? 'w-36 h-36' : 'w-20 h-20';
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
        border: `3px solid ${colors.border}`
      }}
    >
      {shortName}
    </div>
  );
};

const MatchCard: React.FC<MatchCardProps> = ({ match, userPrediction, onPredictClick, isLarge }) => {
  const isCompleted = match.matchStatus === 'COMPLETED';
  const isLive = match.matchStatus === 'LIVE';
  const isUpcoming = match.matchStatus === 'UPCOMING' || match.matchStatus === 'SCHEDULED';
  const hasPrediction = userPrediction !== undefined && userPrediction.predictedWinnerId != null;

  return (
    <div 
      className={`bg-spotify-surface border border-spotify-surfaceLight hover:border-spotify-green transition-all duration-300 rounded-xl overflow-hidden shadow-lg hover:shadow-xl hover:shadow-green-900/30 ${isLarge ? 'p-8' : 'p-6'} ${onPredictClick ? 'cursor-pointer' : ''}`}
      onClick={onPredictClick}
    >
      <div className="flex items-center justify-between mb-4">
        <span className={`px-3 py-1 text-xs font-bold rounded-full ${
          isCompleted ? 'bg-spotify-green text-black' :
          isLive ? 'bg-red-500 text-white animate-pulse' :
          'bg-yellow-500 text-black'
        }`}>
          {match.matchStatus}
        </span>
        <span className={`text-xs text-spotify-textMuted ${isLarge ? 'text-sm' : ''}`}>
          {new Date(match.matchDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}
        </span>
      </div>

      <div className="flex items-center justify-between">
        <div className="flex flex-col items-center">
          <TeamLogo name={match.homeTeamName} shortName={match.homeTeamShortName} size={isLarge ? 'large' : 'normal'} logoUrl={match.homeTeamLogoUrl} />
           <span className={`mt-2 font-medium text-spotify-text text-center truncate min-w-0 break-words ${isLarge ? 'text-lg max-w-[100px]' : 'text-sm max-w-[80px]'}`}>
             {match.homeTeamShortName}
           </span>
        </div>

        <div className="flex flex-col items-center px-4">
          <span className={`font-bold text-spotify-textMuted ${isLarge ? 'text-2xl' : 'text-lg'}`}>VS</span>
          {isCompleted && match.homeTeamScore !== undefined && (
            <div className="text-center mt-1">
              <span className={`font-bold text-spotify-green ${isLarge ? 'text-3xl' : 'text-2xl'}`}>
                {match.homeTeamScore}-{match.awayTeamScore}
              </span>
            </div>
          )}
          {isLive && (
            <span className="text-xs text-red-500 animate-pulse mt-1">LIVE</span>
          )}
        </div>

        <div className="flex flex-col items-center">
          <TeamLogo name={match.awayTeamName} shortName={match.awayTeamShortName} size={isLarge ? 'large' : 'normal'} logoUrl={match.awayTeamLogoUrl} />
          <span className={`mt-2 font-medium text-spotify-text text-center truncate ${isLarge ? 'text-lg max-w-[100px]' : 'text-sm max-w-[80px]'}`}>
            {match.awayTeamShortName}
          </span>
        </div>
      </div>

      {match.result && isCompleted && (
        <div className="mt-4 p-3 bg-spotify-dark rounded-lg text-center">
          <p className="text-sm text-spotify-text">
            {match.result}
          </p>
        </div>
      )}

      {!isCompleted && match.venue && (
        <div className={`mt-4 p-3 bg-spotify-dark rounded-lg text-center ${isLarge ? 'py-4' : ''}`}>
          <p className={`text-spotify-textMuted ${isLarge ? 'text-base' : 'text-sm'}`}>
            📍 {match.venue}
          </p>
          <p className={`text-spotify-textSecondary ${isLarge ? 'text-sm mt-1' : 'text-xs mt-1'}`}>
            {new Date(match.matchDate).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}
          </p>
        </div>
      )}

      {match.homeWinProbability && match.awayWinProbability && !isCompleted && (
        <div className="mt-4">
          <div className="flex items-center">
            <span className="text-xs text-spotify-textMuted w-8">{match.homeWinProbability}%</span>
            <div className="flex-1 mx-2 relative h-2 bg-spotify-dark rounded-full overflow-hidden">
              <div 
                className="absolute left-0 top-0 h-full bg-gradient-to-r from-spotify-green to-green-400 rounded-full"
                style={{ width: `${match.homeWinProbability}%` }}
              />
            </div>
            <span className="text-xs text-spotify-textMuted w-8 text-right">{match.awayWinProbability}%</span>
          </div>
        </div>
      )}

      {hasPrediction && (
        <div className="mt-4 p-3 bg-spotify-dark rounded-lg">
          <div className="flex items-center justify-between">
            <span className="text-sm text-spotify-textMuted">Your prediction</span>
            <span className="text-sm font-medium text-spotify-text">
              {userPrediction.predictedWinnerId === match.homeTeamId
                ? match.homeTeamShortName
                : match.awayTeamShortName}
            </span>
          </div>
          {userPrediction.isCorrect === true && (
            <div className="flex items-center justify-between mt-2">
              <span className="text-sm font-bold text-spotify-green">✓ Correct</span>
              {userPrediction.pointsEarned > 0 && (
                <span className="text-sm font-bold text-spotify-green">+{userPrediction.pointsEarned} pts</span>
              )}
            </div>
          )}
        </div>
      )}

      {isUpcoming && onPredictClick && !hasPrediction && isLarge && (
        <div className="mt-4">
          <button
            onClick={onPredictClick}
            className="w-full bg-spotify-green hover:bg-spotify-greenHover text-black py-2 rounded-full font-medium text-sm"
          >
            Predict Now
          </button>
        </div>
      )}
    </div>
  );
};

export default MatchCard;