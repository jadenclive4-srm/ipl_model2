// frontend22/src/components/MatchCard.tsx

import React from 'react';
import { Match, Prediction } from '../types/api';

interface MatchCardProps {
  match: Match;
  userPrediction?: Prediction;
  onPredictClick?: () => void;
  isLarge?: boolean;
  hideStatus?: boolean;
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

const MatchCard: React.FC<MatchCardProps> = ({ match, userPrediction, onPredictClick, isLarge, hideStatus }) => {
  const isCompleted = match.matchStatus === 'COMPLETED';
  const isLive = match.matchStatus === 'LIVE';
  const isUpcoming = match.matchStatus === 'UPCOMING' || match.matchStatus === 'SCHEDULED';
  // Show prediction button if no prediction exists or prediction data is unavailable
  const hasPrediction = userPrediction && userPrediction.predictedWinnerId != null;

  return (
    <div
      className={`bg-white hover:bg-gray-50 border-2 border-gray-100 hover:border-yellow-400 transition-all duration-300 rounded-2xl overflow-hidden shadow-lg hover:shadow-xl hover:shadow-yellow-400/20 ${isLarge ? 'pt-20 px-8 pb-10' : 'pt-16 px-6 pb-8'} ${onPredictClick ? 'cursor-pointer' : ''} relative group`}
      onClick={onPredictClick}
    >
      {/* Yellow strip at top - expanded to include date/status */}
      <div className={`absolute top-0 left-0 right-0 bg-gradient-to-r from-yellow-400 via-yellow-500 to-yellow-400 rounded-t-2xl flex items-center justify-between px-4 ${isLarge ? 'h-16' : 'h-12'} z-10`}>
        {!hideStatus && (
          <span className={`px-3 py-1 text-sm font-bold rounded-full shadow-lg ${
            isCompleted ? 'bg-green-600 text-white shadow-green-600/30' :
            isLive ? 'bg-red-500 text-white animate-pulse shadow-red-500/30' :
            'bg-green-500 text-white shadow-green-500/30'
          }`}>
            {match.matchStatus}
          </span>
        )}
        <span className={`text-base text-gray-800 font-medium ${isLarge ? 'text-lg' : ''} ${!hideStatus ? '' : 'ml-auto'}`}>
          {new Date(match.matchDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}
        </span>
      </div>
      {/* Subtle inner shadow for depth */}
      <div className={`absolute left-0 right-0 h-1 bg-gradient-to-b from-black/10 to-transparent ${isLarge ? 'top-16' : 'top-12'}`}></div>
      {/* Subtle hover glow effect */}
      <div className="absolute inset-0 bg-gradient-to-r from-yellow-400/0 via-yellow-400/3 to-yellow-400/0 opacity-0 group-hover:opacity-100 transition-all duration-300 rounded-2xl"></div>

      <div className="flex items-center justify-between">
        <div className="flex flex-col items-center">
          <TeamLogo name={match.homeTeamName} shortName={match.homeTeamShortName} size={isLarge ? 'large' : 'normal'} logoUrl={match.homeTeamLogoUrl} />
           <span className={`mt-2 font-medium text-black text-center truncate min-w-0 break-words ${isLarge ? 'text-lg max-w-[100px]' : 'text-sm max-w-[80px]'}`}>
              {match.homeTeamShortName}
            </span>
        </div>

        <div className="flex flex-col items-center px-4">
          <span className={`font-bold text-black ${isLarge ? 'text-2xl' : 'text-lg'}`}>VS</span>
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
          <span className={`mt-2 font-medium text-black text-center truncate ${isLarge ? 'text-lg max-w-[100px]' : 'text-sm max-w-[80px]'}`}>
            {match.awayTeamShortName}
          </span>
        </div>
      </div>

      {match.result && isCompleted && (
        <div className="mt-4 p-3 bg-gray-50 rounded-lg text-center">
          <p className="text-sm text-black">
            {match.result}
          </p>
        </div>
      )}

      {!isCompleted && match.venue && (
        <div className={`mt-4 p-3 bg-yellow-200 rounded-lg text-center ${isLarge ? 'py-4' : ''}`}>
          <p className={`text-black ${isLarge ? 'text-base' : 'text-sm'}`}>
            📍 {match.venue}
          </p>
          <p className={`text-black ${isLarge ? 'text-sm mt-1' : 'text-xs mt-1'}`}>
            {new Date(match.matchDate).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}
          </p>
        </div>
      )}

      {match.homeWinProbability && match.awayWinProbability && !isCompleted && (
        <div className="mt-4">
          <div className="flex items-center">
            <span className="text-xs text-black w-8">{match.homeWinProbability}%</span>
            <div className="flex-1 mx-2 relative h-2 bg-gray-200 rounded-full overflow-hidden">
              <div
                className="absolute left-0 top-0 h-full bg-gradient-to-r from-green-400 to-green-500 rounded-full"
                style={{ width: `${match.homeWinProbability}%` }}
              />
            </div>
            <span className="text-xs text-black w-8 text-right">{match.awayWinProbability}%</span>
          </div>
        </div>
      )}

      {hasPrediction && (
        <div className="mt-4 p-3 bg-gray-50 rounded-lg">
          <div className="flex items-center justify-between">
            <span className="text-sm text-black">Your prediction</span>
            <span className="text-sm font-medium text-black">
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
        <div className="mt-4 relative z-10">
          <button
            onClick={onPredictClick}
            className="w-full bg-spotify-green hover:bg-spotify-greenHover text-black py-3 rounded-xl font-medium text-sm transition-all duration-200 hover:shadow-lg hover:shadow-spotify-green/30 hover:scale-[1.02]"
          >
            Predict Now 🚀
          </button>
        </div>
      )}
    </div>
  );
};

export default MatchCard;