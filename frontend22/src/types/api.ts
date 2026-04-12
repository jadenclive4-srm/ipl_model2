// frontend22/src/types/api.ts

export interface User {
  id: number;
  username: string;
  email: string;
  fullName?: string;
  points: number;
  rank: number;
  isActive: boolean;
  role: string;
  createdAt: number;
  updatedAt: number;
}

export interface Team {
  id: number;
  teamName: string;
  shortName: string;
  homeCity?: string;
  stadium?: string;
  logoUrl?: string;
  teamColor?: string;
  matchesPlayed: number;
  matchesWon: number;
  matchesLost: number;
  netRunRate: number;
  points: number;
}

export interface Match {
  id: number;
  homeTeamId: number;
  awayTeamId: number;
  winnerTeamId?: number;
  venue: string;
  matchDate: number;
  matchNumber: number;
  matchStatus: string;
  matchType: string;
  homeTeamScore?: number;
  awayTeamScore?: number;
  homeTeamOvers?: string;
  awayTeamOvers?: string;
  result?: string;
  homeWinProbability?: number;
  awayWinProbability?: number;
  matchDuration?: number;
  homeTeamName: string;
  awayTeamName: string;
  homeTeamShortName: string;
  awayTeamShortName: string;
  homeTeamLogoUrl?: string;
  awayTeamLogoUrl?: string;
  winnerTeamName?: string;
  venueStats?: VenueStats;
}

export interface Player {
  id: number;
  playerName: string;
  shortName: string;
  teamId: number;
  role: string;
  battingStyle?: string;
  bowlingStyle?: string;
  age?: number;
  nationality?: string;
  imageUrl?: string;
  matchesPlayed: number;
  runs: number;
  wickets: number;
  strikeRate: number;
  economy: number;
}

export interface Prediction {
  id: number;
  userId: number;
  username?: string;
  matchId: number;
  predictedWinnerId?: number;
  predictedWinnerName?: string;
  isCorrect: boolean;
  pointsEarned: number;
  createdAt: number;
  homeProbability?: number;
  awayProbability?: number;
}

export interface Question {
  id: number;
  matchId: number;
  questionText: string;
  optionA: string;
  optionB: string;
  optionC?: string;
  optionD?: string;
  correctOption: string;
  pointsValue: number;
  isActive: boolean;
  questionType: string;
  createdAt: number;
  userAnswer?: string;
  isAnswered?: boolean;
}

export interface UserAnswer {
  id: number;
  userId: number;
  questionId: number;
  selectedOption: string;
  isCorrect: boolean;
  pointsEarned: number;
  answeredAt: number;
}

export interface HeadToHead {
  totalMatches: number;
  team1Wins: number;
  team2Wins: number;
  draws: number;
  team1Name: string;
  team2Name: string;
}

export interface AuthRequest {
  username: string;
  userId?: string;
  password: string;
  email?: string;
  fullName?: string;
  role?: string;
}

export interface AuthResponse {
  username: string;
  email: string;
  fullName?: string;
  token: string;
  userId: number;
  role: string;
}

export interface AIQueryRequest {
  query: string;
  teams?: Record<string, string>;
  predictions?: any;
}

export interface AIResponse {
  answer?: string;
  response?: string;
  rawData?: any;
  error?: string;
  teams?: Record<string, string>;
}

export interface PredictionRequest {
  userId: number;
  matchId: number;
  predictedWinnerId?: number;
  homeProbability?: number;
  awayProbability?: number;
}

export interface QuestionAnswerRequest {
  userId: number;
  questionId: number;
  selectedOption: string;
}

export interface BatchAnswerRequest {
  userId: number;
  matchId: number;
  answers: string[];
  questionIds: number[];
}

export interface VenueStats {
  stadium: string;
  city: string;
  pitchType: string;
  avgScore: number;
  chasingWinPct: number;
  dewFactor: string;
  boundarySize: string;
}

export interface UserPredictionSummary {
  userId: number;
  fullName: string;
  predictedTeamName: string;
  matchId: number;
}