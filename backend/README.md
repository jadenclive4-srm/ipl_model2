# IPL Predictor - Backend

A Spring Boot REST API application for the IPL Predictor frontend.

## 🏏 Features

- RESTful API endpoints for match data, predictions, and user management
- User authentication with JWT
- Match prediction logic
- Leaderboard calculations
- MySQL database integration

## 🚀 Tech Stack

- Java 17+
- Spring Boot 3.x
- Spring Security
- MySQL
- Maven

## 📦 Prerequisites

- Java Development Kit (JDK) 17 or higher
- Maven 3.6+
- MySQL 8.0+

## 🛠️ Installation

1. Navigate to the backend directory:
```bash
cd backend
```

2. Update database configuration in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ipl_predictor
spring.datasource.username=root
spring.datasource.password=your_password
```

3. Build the project:
```bash
mvn clean install
```

## ▶️ Running the Application

### Option 1: Using Maven
```bash
mvn spring-boot:run
```
The server will start at [http://localhost:8080](http://localhost:8080)

### Option 2: Using JAR
```bash
java -jar target/ipl-predictor-1.0.0.jar
```

## 📁 Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/ipl/
│   │   │       ├── config/       # Configuration classes
│   │   │       ├── controller/  # REST controllers
│   │   │       ├── dto/          # Data Transfer Objects
│   │   │       ├── model/        # Entity classes
│   │   │       ├── repository/  # Data repositories
│   │   │       ├── service/      # Business logic
│   │   │       └── util/         # Utility classes
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── pom.xml
└── README.md
```

## 🔗 API Endpoints

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

## 🎨 UI Theme

- **Primary Color**: #0A1F44 (Deep Blue)
- **Accent Color**: #FF6B00 (Vibrant Orange)

## 📄 License

This project is for educational purposes.
