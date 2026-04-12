# Frontend22 - IPL Predictor Frontend

A modern React TypeScript frontend for the IPL Predictor application, connecting to the Spring Boot backend.

## Features

- **Authentication**: Login/Register with JWT token management
- **Match Predictions**: View matches, make predictions, see probabilities
- **Leaderboard**: Track user rankings and points
- **AI Assistant**: Query IPL statistics and predictions using AI
- **Admin Panel**: Import match data and statistics
- **Responsive Design**: Built with Tailwind CSS

## Tech Stack

- React 18 with TypeScript
- React Router for navigation
- Tailwind CSS for styling
- Context API for state management
- Fetch API for HTTP requests

## Backend Connection

This frontend connects to the IPL Predictor Spring Boot backend with the following API endpoints:

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `GET /api/auth/validate` - Token validation

### Matches
- `GET /api/matches` - Get all matches
- `GET /api/matches/{id}` - Get match by ID
- `GET /api/matches/today` - Get today's match
- `GET /api/matches/upcoming` - Get upcoming matches
- `GET /api/matches/completed` - Get completed matches
- `POST /api/matches` - Create match
- `PUT /api/matches/{id}/result` - Update match result
- `GET /api/matches/h2h` - Get head-to-head stats

### Teams
- `GET /api/teams` - Get all teams
- `GET /api/teams/{id}` - Get team by ID
- `GET /api/teams/standings` - Get team standings

### Predictions
- `POST /api/predictions` - Create prediction
- `GET /api/predictions/user/{userId}` - Get user predictions
- `GET /api/predictions/match/{matchId}` - Get match predictions

### Leaderboard
- `GET /api/leaderboard` - Get full leaderboard
- `GET /api/leaderboard/top/{count}` - Get top users

### AI Queries
- `POST /api/ai/ask` - Ask AI assistant questions

### Admin (Data Import)
- `POST /api/matches/import/excel` - Import matches from Excel
- `POST /api/matches/import/h2h` - Import H2H statistics

## Setup

1. Install dependencies:
   ```bash
   npm install
   ```

2. Configure environment variables in `.env`:
   ```
   REACT_APP_API_URL=http://localhost:8080
   ```

3. Start the development server:
   ```bash
   npm start
   ```

## Project Structure

```
frontend22/
├── public/
│   └── index.html
├── src/
│   ├── components/
│   │   ├── MatchCard.tsx
│   │   ├── Leaderboard.tsx
│   │   └── AIQuery.tsx
│   ├── contexts/
│   │   └── AuthContext.tsx
│   ├── pages/
│   │   ├── Login.tsx
│   │   ├── Dashboard.tsx
│   │   └── Admin.tsx
│   ├── services/
│   │   └── api.ts
│   ├── types/
│   │   └── api.ts
│   ├── App.tsx
│   ├── index.tsx
│   └── index.css
└── package.json
```

## Key Components

- **AuthContext**: Manages authentication state and API calls
- **ApiService**: Centralized API client for all backend communication
- **MatchCard**: Displays match information and prediction interface
- **Leaderboard**: Shows user rankings
- **AIQuery**: Interface for AI-powered questions and answers

## Authentication Flow

1. User logs in/registers via Login page
2. JWT token stored in localStorage
3. AuthContext validates token on app load
4. Protected routes redirect to login if not authenticated
5. Admin routes check for ADMIN role

## Data Flow

1. Components call ApiService methods
2. ApiService makes HTTP requests with auth headers
3. Responses are typed with TypeScript interfaces
4. State updates trigger re-renders
5. Error handling displays user-friendly messages

This frontend provides a complete user interface for the IPL prediction system, mapping all backend functionality to an intuitive React application.