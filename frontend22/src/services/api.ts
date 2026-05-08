// frontend22/src/services/api.ts

import {
  User,
  Team,
  Match,
  Prediction,
  Question,
  UserAnswer,
  HeadToHead,
  UserResponse,
  AuthRequest,
  AuthResponse,
  RegisterResponse,
  AIQueryRequest,
  AIResponse,
  PredictionRequest,
  QuestionAnswerRequest,
  BatchAnswerRequest,
  VenueStats,
  UserPredictionSummary,
  MatchLeaderboardEntryDTO,
} from '../types/api';

const API_BASE_URL = process.env.REACT_APP_API_URL || (typeof window !== 'undefined' ? window.location.origin : 'http://localhost:8081');

class ApiService {
  private getAuthHeaders(): HeadersInit {
    const token = localStorage.getItem('token');
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${API_BASE_URL}${endpoint}`;
    const config: RequestInit = {
      headers: {
        'Content-Type': 'application/json',
        ...this.getAuthHeaders(),
        ...options.headers,
      },
      ...options,
    };

    const response = await fetch(url, config);
    
    if (response.ok) {
      return response.json();
    } else {
      // Try to get error details from response
      let errorMessage = `${response.status} ${response.statusText}`;
      const contentType = response.headers.get('content-type');
      
      try {
        if (contentType && contentType.includes('application/json')) {
          const errorData = await response.json();
          console.error('API Error response:', { status: response.status, data: errorData });
          if (errorData.error) {
            errorMessage = errorData.error;
          } else if (errorData.message) {
            errorMessage = errorData.message;
          } else if (typeof errorData === 'string') {
            errorMessage = errorData;
          }
        } else {
          const text = await response.text();
          console.error('API Error response (text):', { status: response.status, text });
          if (text) errorMessage = text;
        }
      } catch (e) {
        console.error('Failed to parse error response:', e);
      }
      
      throw new Error(errorMessage);
    }
  }

   // Auth endpoints
  async register(authData: AuthRequest): Promise<RegisterResponse> {
    return this.request<RegisterResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(authData),
    });
  }

  async login(authData: AuthRequest): Promise<AuthResponse> {
    return this.request<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(authData),
    });
  }

  async validateToken(): Promise<{ valid: boolean }> {
    return this.request<{ valid: boolean }>('/api/auth/validate');
  }

  async verifyOtp(email: string, otp: string): Promise<AuthResponse> {
    return this.request<AuthResponse>('/api/auth/verify-otp', {
      method: 'POST',
      body: JSON.stringify({ email, otp }),
    });
  }

  async resendOtp(email: string): Promise<{ message: string }> {
    return this.request<{ message: string }>('/api/auth/resend-otp', {
      method: 'POST',
      body: JSON.stringify({ email }),
    });
  }

  async changePassword(identifier: string, currentPassword: string, newPassword: string): Promise<{ message: string }> {
    return this.request<{ message: string }>('/api/auth/change-password', {
      method: 'POST',
      body: JSON.stringify({ identifier, currentPassword, newPassword }),
    });
  }

  async resetPassword(email: string, newPassword: string): Promise<{ message: string }> {
    return this.request<{ message: string }>('/api/auth/admin/reset-password', {
      method: 'POST',
      body: JSON.stringify({ email, newPassword }),
    });
  }

  // Match endpoints
  async getAllMatches(): Promise<Match[]> {
    return this.request<Match[]>('/api/matches');
  }

  async getMatchById(id: number): Promise<Match> {
    return this.request<Match>(`/api/matches/${id}`);
  }

  async getTodayMatch(): Promise<Match> {
    const timestamp = Date.now();
    return this.request<Match>(`/api/matches/today?_t=${timestamp}`);
  }

  async getTodayMatchForEvaluation(): Promise<Match> {
    const timestamp = Date.now();
    return this.request<Match>(`/api/matches/today/for-evaluation?_t=${timestamp}`);
  }

  async getTodaysMatches(): Promise<Match[]> {
    return this.request<Match[]>('/api/matches/today/all');
  }

  async getUpcomingMatches(): Promise<Match[]> {
    return this.request<Match[]>('/api/matches/upcoming');
  }

  async getCompletedMatches(): Promise<Match[]> {
    return this.request<Match[]>('/api/matches/completed');
  }

  async createMatch(matchData: Partial<Match>): Promise<Match> {
    return this.request<Match>('/api/matches', {
      method: 'POST',
      body: JSON.stringify(matchData),
    });
  }

  async updateMatchResult(id: number, resultData: Partial<Match>): Promise<Match> {
    return this.request<Match>(`/api/matches/${id}/result`, {
      method: 'PUT',
      body: JSON.stringify(resultData),
    });
  }

  async getMatchesByTeam(teamId: number): Promise<Match[]> {
    return this.request<Match[]>(`/api/matches/team/${teamId}`);
  }

  async getHeadToHead(team1Id: number, team2Id: number): Promise<HeadToHead> {
    return this.request<HeadToHead>(`/api/matches/headtohead/${team1Id}/${team2Id}`);
  }

  async getH2hByTeamNames(team1Name: string, team2Name: string): Promise<HeadToHead> {
    const params = new URLSearchParams({ team1Name, team2Name });
    return this.request<HeadToHead>(`/api/matches/h2h?${params}`);
  }

  // Team endpoints
  async getAllTeams(): Promise<Team[]> {
    return this.request<Team[]>('/api/teams');
  }

  async getTeamById(id: number): Promise<Team> {
    return this.request<Team>(`/api/teams/${id}`);
  }

  async getTeamByName(teamName: string): Promise<Team> {
    return this.request<Team>(`/api/teams/name/${encodeURIComponent(teamName)}`);
  }

  async createTeam(teamData: Partial<Team>): Promise<Team> {
    return this.request<Team>('/api/teams', {
      method: 'POST',
      body: JSON.stringify(teamData),
    });
  }

  async getTeamStandings(): Promise<Team[]> {
    return this.request<Team[]>('/api/teams/standings');
  }

  // User endpoints
  async getAllUsers(): Promise<User[]> {
    return this.request<User[]>('/api/users');
  }

  async getUserById(id: number): Promise<User> {
    return this.request<User>(`/api/users/${id}`);
  }

  async getUserByUsername(username: string): Promise<User> {
    return this.request<User>(`/api/users/username/${username}`);
  }

  async updateUser(id: number, userData: Partial<User>): Promise<User> {
    return this.request<User>(`/api/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(userData),
    });
  }

  async createUser(userData: { username: string; email: string; password: string; fullName?: string; role?: string }): Promise<{ message: string; userId: number; username: string; email: string; role: string }> {
    return this.request<{ message: string; userId: number; username: string; email: string; role: string }>('/api/admin/users', {
      method: 'POST',
      body: JSON.stringify(userData),
    });
  }

  // Prediction endpoints
  async createPrediction(predictionData: PredictionRequest): Promise<Prediction> {
    return this.request<Prediction>('/api/predictions', {
      method: 'POST',
      body: JSON.stringify(predictionData),
    });
  }

  async getUserPredictions(userId: number): Promise<Prediction[]> {
    return this.request<Prediction[]>(`/api/predictions/user/${userId}`);
  }

  async getMatchPredictions(matchId: number): Promise<Prediction[]> {
    return this.request<Prediction[]>(`/api/predictions/match/${matchId}`);
  }

  async getUserMatchPrediction(userId: number, matchId: number): Promise<Prediction> {
    return this.request<Prediction>(`/api/predictions/user/${userId}/match/${matchId}`);
  }

  // Leaderboard endpoints
  async getLeaderboard(): Promise<User[]> {
    return this.request<User[]>('/api/leaderboard');
  }

  async getUserRank(userId: number): Promise<User> {
    return this.request<User>(`/api/leaderboard/user/${userId}/rank`);
  }

  async getUserPoints(userId: number): Promise<number> {
    return this.request<number>(`/api/leaderboard/user/${userId}/points`);
  }

  async getTopUsers(count: number): Promise<User[]> {
    return this.request<User[]>(`/api/leaderboard/top/${count}`);
  }

  async sendTopToTeams(gc: string = 'default', mode: string = 'webhook'): Promise<{ success: boolean; message: string; topUsers?: User[]; target?: string }> {
    return this.request<{ success: boolean; message: string; topUsers?: User[]; target?: string }>(
      `/api/leaderboard/send-to-teams?gc=${encodeURIComponent(gc)}&mode=${encodeURIComponent(mode)}`,
      { method: 'POST' }
    );
  }

  async sendTopToGroupChat(chatId: string = 'default', mode: string = 'groupchat'): Promise<{ success: boolean; message: string }> {
    return this.sendTopToTeams(chatId, mode);
  }

  async sendViaPowerAutomate(): Promise<{ success: boolean; message: string }> {
    return this.sendTopToTeams('default', 'powerautomate');
  }

  async getAvailableTeamsChannels(): Promise<{ channels: string[]; count: number; type?: string }> {
    return this.request<{ channels: string[]; count: number; type?: string }>('/api/leaderboard/teams-channels');
  }

  async getMatchLeaderboard(matchId: number): Promise<MatchLeaderboardEntryDTO[]> {
    return this.request<MatchLeaderboardEntryDTO[]>(`/api/predictions/leaderboard/${matchId}`);
  }

   async getAvailableGroupChats(): Promise<{ groupChats: string[]; count: number; type?: string }> {
     return this.request<{ groupChats: string[]; count: number; type?: string }>('/api/leaderboard/teams-groupchats');
   }

   async getTeamsDeepLink(users?: string): Promise<{ success: boolean; message: string; deepLink?: string; users?: any[] }> {
     const params = users ? `?users=${encodeURIComponent(users)}` : '';
     return this.request<{ success: boolean; message: string; deepLink?: string; users?: any[] }>(
       `/api/leaderboard/teams-deeplink${params}`
     );
   }

  // Question endpoints
  async getQuestionsByMatch(matchId: number, userId: number): Promise<Question[]> {
    const params = new URLSearchParams({ userId: userId.toString() });
    return this.request<Question[]>(`/api/questions/match/${matchId}?${params}`);
  }

  async submitAnswer(answerData: QuestionAnswerRequest): Promise<UserAnswer> {
    const params = new URLSearchParams({
      userId: answerData.userId.toString(),
      questionId: answerData.questionId.toString(),
      selectedOption: answerData.selectedOption,
    });
    return this.request<UserAnswer>(`/api/questions/answer?${params}`, {
      method: 'POST',
    });
  }

  async submitBatchAnswers(request: BatchAnswerRequest): Promise<UserAnswer[]> {
    return this.request<UserAnswer[]>('/api/questions/answers/batch', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  // AI endpoints
  async queryAI(request: AIQueryRequest): Promise<{ answer: string }> {
    return this.request<{ answer: string }>('/api/ai/query', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async askAI(query: string): Promise<AIResponse> {
    return this.request<AIResponse>('/api/ai/ask', {
      method: 'POST',
      body: JSON.stringify({ query }),
    });
  }

  // File upload endpoints
  async importMatches(file: File): Promise<{ message: string }> {
    const formData = new FormData();
    formData.append('file', file);

    const url = `${API_BASE_URL}/api/matches/import/excel`;
    const response = await fetch(url, {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: formData,
    });

    if (!response.ok) {
      throw new Error(`Import Error: ${response.status} ${response.statusText}`);
    }
    return response.json();
  }

  async importH2hStats(): Promise<{ message: string }> {
    return this.request<{ message: string }>('/api/matches/import/h2h', {
      method: 'POST',
    });
  }

  async importVenueStats(): Promise<{ message: string }> {
    return this.request<{ message: string }>('/api/matches/import/venue', {
      method: 'POST',
    });
  }

  // Scraper endpoints
  async scrapeLastMatch(): Promise<any> {
    return this.request<any>('/api/scraper/last-match', { method: 'POST' });
  }

  async scrapePointsTable(): Promise<any> {
    return this.request<any>('/api/scraper/points-table', { method: 'POST' });
  }

  async scrapePlayerStats(teamOrPlayer: string): Promise<any> {
    return this.request<any>(`/api/scraper/player-stats/${encodeURIComponent(teamOrPlayer)}`, { method: 'POST' });
  }

  async scrapeUpcomingMatches(): Promise<any> {
    return this.request<any>('/api/scraper/upcoming-matches', { method: 'POST' });
  }

  async scrapeTeamStats(): Promise<any> {
    return this.request<any>('/api/scraper/team-stats', { method: 'POST' });
  }

  async getVenueStats(stadium: string): Promise<VenueStats> {
    return this.request<VenueStats>(`/api/matches/venue/${encodeURIComponent(stadium)}`);
  }

  async submitQuizPrediction(request: { userId: number; matchId: number; answers: { [key: string]: string } }): Promise<{ message: string }> {
    return this.request<{ message: string }>('/api/predictions/quiz', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async getQuizStatus(userId: number, matchId: number): Promise<{ submitted: boolean }> {
    return this.request<{ submitted: boolean }>(`/api/predictions/quiz/status?userId=${userId}&matchId=${matchId}`);
  }

  async getUserQuizResponses(matchId: number): Promise<UserResponse> {
    return this.request<UserResponse>(`/api/predictions/quiz/responses?matchId=${matchId}`);
  }

  async evaluatePredictions(matchId: number): Promise<{ message: string }> {
    return this.request<{ message: string }>(`/api/predictions/evaluate/${matchId}`, {
      method: 'POST',
    });
  }

  async getAllPredictionsForMatch(matchId: number): Promise<Prediction[]> {
    return this.request<Prediction[]>(`/api/predictions/match/${matchId}/all`);
  }

  async getVoteCounts(): Promise<Record<number, number>> {
    return this.request<Record<number, number>>('/api/predictions/vote-counts');
  }

  async getPredictionAccuracy(): Promise<{ correct: number; total: number }> {
    return this.request<{ correct: number; total: number }>('/api/predictions/accuracy');
  }

  async getAllQuestionsForMatch(matchId: number): Promise<Question[]> {
    return this.request<Question[]>(`/api/questions/match/${matchId}/all`);
  }

  async updateQuestionCorrectOption(questionId: number, correctOption: string): Promise<void> {
    return this.request<void>(`/api/questions/${questionId}/correct`, {
      method: 'PUT',
      body: JSON.stringify({ correctOption }),
    });
  }

  async resetPredictions(matchId: number): Promise<void> {
    return this.request<void>(`/api/predictions/reset/${matchId}`, {
      method: 'POST',
    });
  }

  async resetQuizAnswers(matchId: number): Promise<void> {
    return this.request<void>(`/api/questions/reset/match/${matchId}`, {
      method: 'POST',
    });
  }

  async resetMatchResult(matchId: number): Promise<Match> {
    return this.request<Match>(`/api/matches/${matchId}/reset`, {
      method: 'POST',
    });
  }

  async saveCorrectAnswers(matchId: number, correctAnswers: Record<string, string>): Promise<void> {
    return this.request<void>(`/api/questions/match/${matchId}/correct-answers`, {
      method: 'POST',
      body: JSON.stringify(correctAnswers),
    });
  }

  async getCorrectAnswers(matchId: number): Promise<Record<string, string>> {
    return this.request<Record<string, string>>(`/api/questions/match/${matchId}/correct-answers`);
  }

  async evaluateQuizFromMongo(matchId: number): Promise<void> {
    return this.request<void>(`/api/questions/evaluate-from-db/match/${matchId}`, {
      method: 'POST',
    });
  }

  async evaluateQuizAnswers(matchId: number): Promise<{ message: string }> {
    return this.request<{ message: string }>(`/api/questions/evaluate/match/${matchId}`, {
      method: 'POST',
    });
  }

  async getQuizQuestionsForMatch(matchId: number): Promise<Question[]> {
    return this.request<Question[]>(`/api/questions/match/${matchId}/all`);
  }

  async uploadQuizQuestions(matchId: number, questions: Question[]): Promise<Question[]> {
    return this.request<Question[]>(`/api/questions/match/${matchId}/upload`, {
      method: 'POST',
      body: JSON.stringify(questions),
    });
  }

  async getAllUsersWithPredictions(): Promise<UserPredictionSummary[]> {
    return this.request<UserPredictionSummary[]>('/api/predictions/all-users-predictions');
  }

    async getPredictionsByDate(date: string): Promise<UserPredictionSummary[]> {
        return this.request<UserPredictionSummary[]>(`/api/predictions/predictions-by-date?date=${date}`);
    }

    async exportUsersToCsv(): Promise<string> {
        return this.request<string>('/api/admin/export-users-csv', {
            method: 'GET',
            headers: {
                'Accept': 'text/plain'
            }
        });
    }

    async importUsersFromCsv(file: File): Promise<any> {
        const formData = new FormData();
        formData.append('file', file);

        const url = `${API_BASE_URL}/api/admin/import-users-csv`;
        const response = await fetch(url, {
            method: 'POST',
            headers: this.getAuthHeaders(),
            body: formData,
        });

        if (!response.ok) {
            throw new Error(`Import Error: ${response.status} ${response.statusText}`);
        }
        return response.json();
    }

    async deleteAllPredictions(matchId: number): Promise<void> {
    return this.request<void>(`/api/predictions/delete/${matchId}`, {
      method: 'POST',
    });
  }
}

export const apiService = new ApiService();