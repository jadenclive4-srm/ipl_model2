# IPL AI Chat (Groq + SearXNG)

🧠 Overview

This project is an IPL AI chatbot that:

- Uses Groq for generating answers (llama-3.3-70b-versatile)
- Uses SearXNG for web search (no API key required)
- Supports match, player, and general queries

⚙️ Requirements

- Java 17+
- Spring Boot project
- Groq API Key
- SearXNG instance (local or public)

🔑 Environment Variables

The application uses `application.properties` for configuration:

```properties
groq.api.key=your_groq_api_key
groq.model=llama-3.3-70b-versatile
```

For SearXNG, set the environment variable or update application.properties:
```properties
# SEARXNG_URL is optional - defaults to http://localhost:8080
SEARXNG_URL=http://localhost:8080
```

Additional configs in application.properties:

```properties
# Groq API Configuration
# Get a free API key from https://console.groq.com/
# Set GROQ_API_KEY environment variable or replace below
groq.api.key=YOUR_GROQ_API_KEY_HERE
groq.model=llama-3.3-70b-versatile

# Brave Search API Configuration (alternative)
brave.search.api.key=${BRAVE_SEARCH_API_KEY:}
brave.search.api.url=https://api.search.brave.com/res/v1/web/search
```

🌐 SearXNG Setup

Option A: Use Public Instance (Quick Start)

Use any public instance:
- https://searx.be
- https://search.sapti.me

Set in environment:
```bash
SEARXNG_URL=https://searx.be
```

Option B: Run Locally (Docker)
```bash
docker run -d -p 8080:8080 searxng/searxng
```

Then set:
```bash
SEARXNG_URL=http://localhost:8080
```

🧠 Groq Setup

Use model: `llama-3.3-70b-versatile`

Get your API key from: https://console.groq.com/

🔍 API Endpoints

### AI Chat

**POST** `/api/ai/ask` - Ask AI a question

Request:
```json
{
  "query": "RCB injury update today"
}
```

Response:
```json
{
  "response": "Final AI answer here",
  "rawData": {...}  // included for MATCH queries
}
```

**GET** `/api/ai/ask?query=...` - Same as POST (GET version)

**POST** `/api/ai/query` - Legacy match prediction endpoint

### Matches
- `GET /api/matches` - Get all matches
- `GET /api/matches/today` - Get today's match
- `GET /api/matches/{id}` - Get match details
- `GET /api/matches/upcoming` - Get upcoming matches

### Predictions
- `POST /api/predictions` - Create a prediction
- `GET /api/predictions/user/{userId}` - Get user predictions

### Leaderboard
- `GET /api/leaderboard` - Get leaderboard

### Users
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user

🧪 Test Queries

Try these queries:
- `Who will win MI vs CSK?`
- `RCB injury update today`
- `Top IPL batsman`
- `Who is better Kohli or Rohit`

⚙️ Expected Behavior

| Query Type | Intent | Behavior |
|-----------|--------|----------|
| Match (e.g., "MI vs CSK") | MATCH | Uses match service + Groq |
| Player (e.g., "Kohli stats") | PLAYER | Groq only |
| Latest/Trending (e.g., "injury update today") | LATEST | SearXNG + Groq |
| General (e.g., "Who is better Kohli or Rohit") | GENERAL | Groq only |

Intent Detection:
- **MATCH**: Contains vs, win, beat, defeat, winner, prediction, predict
- **PLAYER**: Contains score, runs, century, batsman, bowler, wicket, and player names
- **LATEST**: Contains latest, today, news, update, recent, injury
- **GENERAL**: All other queries

## 🎨 UI Theme

- **Primary Color**: #0A1F44 (Deep Blue)
- **Accent Color**: #FF6B00 (Vibrant Orange)

## 📄 License

This project is for educational purposes.
