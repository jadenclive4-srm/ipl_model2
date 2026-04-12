-- DDL Script for IPL Predictor Database

-- Drop tables if they exist
DROP TABLE IF EXISTS user_answers;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS predictions;
DROP TABLE IF EXISTS matches;
DROP TABLE IF EXISTS players;
DROP TABLE IF EXISTS teams;
DROP TABLE IF EXISTS users;

-- Create users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    points INT DEFAULT 0,
    rank INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    role VARCHAR(20) DEFAULT 'USER',
    created_at BIGINT,
    updated_at BIGINT
);

-- Create teams table
CREATE TABLE teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_name VARCHAR(100) UNIQUE NOT NULL,
    short_name VARCHAR(10) UNIQUE NOT NULL,
    home_city VARCHAR(50),
    stadium VARCHAR(100),
    logo_url VARCHAR(255),
    team_color VARCHAR(20),
    matches_played INT DEFAULT 0,
    matches_won INT DEFAULT 0,
    matches_lost INT DEFAULT 0,
    nrr INT DEFAULT 0,
    points INT DEFAULT 0
);

-- Create matches table
CREATE TABLE matches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    home_team_id BIGINT NOT NULL,
    away_team_id BIGINT NOT NULL,
    winner_team_id BIGINT,
    venue VARCHAR(100) NOT NULL,
    match_date BIGINT NOT NULL,
    match_number INT UNIQUE NOT NULL,
    match_status VARCHAR(20),
    match_type VARCHAR(20) NOT NULL,
    home_team_score INT,
    away_team_score INT,
    home_team_overs VARCHAR(10),
    away_team_overs VARCHAR(10),
    result VARCHAR(100),
    home_win_probability INT,
    away_win_probability INT,
    FOREIGN KEY (home_team_id) REFERENCES teams(id),
    FOREIGN KEY (away_team_id) REFERENCES teams(id),
    FOREIGN KEY (winner_team_id) REFERENCES teams(id)
);

-- Create players table
CREATE TABLE players (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_name VARCHAR(100) NOT NULL,
    short_name VARCHAR(20) UNIQUE NOT NULL,
    team_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    batting_style VARCHAR(20),
    bowling_style VARCHAR(20),
    age INT,
    nationality VARCHAR(30),
    image_url VARCHAR(255),
    matches_played INT DEFAULT 0,
    runs INT DEFAULT 0,
    wickets INT DEFAULT 0,
    strike_rate DOUBLE DEFAULT 0.0,
    economy DOUBLE DEFAULT 0.0,
    FOREIGN KEY (team_id) REFERENCES teams(id)
);

-- Create predictions table
CREATE TABLE predictions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    match_id BIGINT NOT NULL,
    predicted_winner_id BIGINT,
    is_correct BOOLEAN DEFAULT FALSE,
    points_earned INT DEFAULT 0,
    created_at BIGINT,
    home_probability INT,
    away_probability INT,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (match_id) REFERENCES matches(id),
    FOREIGN KEY (predicted_winner_id) REFERENCES teams(id),
    UNIQUE KEY unique_user_match (user_id, match_id)
);

-- Create questions table
CREATE TABLE questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    match_id BIGINT NOT NULL,
    question_text VARCHAR(1000) NOT NULL,
    option_a VARCHAR(255) NOT NULL,
    option_b VARCHAR(255) NOT NULL,
    option_c VARCHAR(255),
    option_d VARCHAR(255),
    correct_option VARCHAR(5) NOT NULL,
    points_value INT DEFAULT 10,
    is_active BOOLEAN DEFAULT TRUE,
    question_type VARCHAR(50) NOT NULL,
    created_at BIGINT,
    FOREIGN KEY (match_id) REFERENCES matches(id)
);

-- Create user_answers table
CREATE TABLE user_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option VARCHAR(5) NOT NULL,
    is_correct BOOLEAN DEFAULT FALSE,
    points_earned INT DEFAULT 0,
    answered_at BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (question_id) REFERENCES questions(id),
    UNIQUE KEY unique_user_question (user_id, question_id)
);

-- Create indexes
CREATE INDEX idx_matches_match_date ON matches(match_date);
CREATE INDEX idx_matches_match_status ON matches(match_status);
CREATE INDEX idx_predictions_user_id ON predictions(user_id);
CREATE INDEX idx_predictions_match_id ON predictions(match_id);
CREATE INDEX idx_users_points ON users(points DESC);
CREATE INDEX idx_teams_points ON teams(points DESC);
CREATE INDEX idx_questions_match_id ON questions(match_id);
CREATE INDEX idx_user_answers_user_id ON user_answers(user_id);
CREATE INDEX idx_user_answers_question_id ON user_answers(question_id);