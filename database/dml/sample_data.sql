-- DML Script for IPL Predictor Database
-- Sample data for testing

-- Insert teams
INSERT INTO teams (team_name, short_name, home_city, stadium, team_color, matches_played, matches_won, matches_lost, nrr, points) VALUES
('Mumbai Indians', 'MI', 'Mumbai', 'Wankhede Stadium', '#004BA0', 0, 0, 0, 0, 0),
('Chennai Super Kings', 'CSK', 'Chennai', 'MA Chidambaram Stadium', '#F2C900', 0, 0, 0, 0, 0),
('Kolkata Knight Riders', 'KKR', 'Kolkata', 'Eden Gardens', '#2E2E3A', 0, 0, 0, 0, 0),
('Royal Challengers Bangalore', 'RCB', 'Bengaluru', 'M. Chinnaswamy Stadium', '#D61F20', 0, 0, 0, 0, 0),
('Delhi Capitals', 'DC', 'New Delhi', 'Arun Jaitley Stadium', '#0078BC', 0, 0, 0, 0, 0),
('Sunrisers Hyderabad', 'SRH', 'Hyderabad', 'Rajiv Gandhi International Cricket Stadium', '#F26422', 0, 0, 0, 0, 0),
('Rajasthan Royals', 'RR', 'Jaipur', 'Sawai Mansingh Stadium', '#EB138D', 0, 0, 0, 0, 0),
('Punjab Kings', 'PBKS', 'Mohali', 'Punjab Cricket Association IS.bindra Stadium', '#B6911A', 0, 0, 0, 0, 0),
('Lucknow Super Giants', 'LSG', 'Lucknow', 'BRSABVE Cricket Stadium', '#0C2461', 0, 0, 0, 0, 0),
('Gujarat Titans', 'GT', 'Ahmedabad', 'Narendra Modi Stadium', '#1D3F6B', 0, 0, 0, 0, 0);

-- Insert sample users
-- Password for all users is: password123 (bcrypt hash)
INSERT INTO users (username, email, password, full_name, points, rank, is_active, role, created_at, updated_at) VALUES
('admin', 'admin@ipl.com', '$2a$10$EqKpc3ZzTxU5cF3B5J5HuetP5F5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z', 'Administrator', 100, 1, TRUE, 'ADMIN', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
('user1', 'user1@ipl.com', '$2a$10$EqKpc3ZzTxU5cF3B5J5HuetP5F5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z', 'User One', 50, 2, TRUE, 'USER', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
('user2', 'user2@ipl.com', '$2a$10$EqKpc3ZzTxU5cF3B5J5HuetP5F5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z', 'User Two', 25, 3, TRUE, 'USER', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

-- Insert sample players for Mumbai Indians
INSERT INTO players (player_name, short_name, team_id, role, batting_style, bowling_style, age, nationality, matches_played, runs, wickets, strike_rate, economy) VALUES
('Rohit Sharma', 'RG Sharma', 1, 'Batsman', 'Right-hand', NULL, 36, 'Indian', 0, 0, 0, 0.0, 0.0),
('Jasprit Bumrah', 'J Bumrah', 1, 'Bowler', 'Right-hand', 'Right-arm fast', 29, 'Indian', 0, 0, 0, 0.0, 0.0),
('Surya Kumar Yadav', 'SK Yadav', 1, 'Batsman', 'Right-hand', NULL, 32, 'Indian', 0, 0, 0, 0.0, 0.0),
('Hardik Pandya', 'HH Pandya', 1, 'All-rounder', 'Right-hand', 'Right-arm fast', 29, 'Indian', 0, 0, 0, 0.0, 0.0);

-- Insert sample players for Chennai Super Kings
INSERT INTO players (player_name, short_name, team_id, role, batting_style, bowling_style, age, nationality, matches_played, runs, wickets, strike_rate, economy) VALUES
('MS Dhoni', 'MS Dhoni', 2, 'Wicket-keeper', 'Right-hand', NULL, 41, 'Indian', 0, 0, 0, 0.0, 0.0),
('Ravindra Jadeja', 'RA Jadeja', 2, 'All-rounder', 'Left-hand', 'Left-arm orthodox', 34, 'Indian', 0, 0, 0, 0.0, 0.0),
(' Ruturaj Gaikwad', 'RT Gaikwad', 2, 'Batsman', 'Right-hand', NULL, 26, 'Indian', 0, 0, 0, 0.0, 0.0);

-- Insert sample matches (schedule for 2026 IPL season)
INSERT INTO matches (home_team_id, away_team_id, venue, match_date, match_number, match_status, match_type, home_win_probability, away_win_probability) VALUES
(1, 2, 'Wankhede Stadium', UNIX_TIMESTAMP('2026-04-01 19:30:00') * 1000, 1, 'SCHEDULED', 'LEAGUE', 55, 45),
(3, 4, 'Eden Gardens', UNIX_TIMESTAMP('2026-04-02 19:30:00') * 1000, 2, 'SCHEDULED', 'LEAGUE', 50, 50),
(5, 6, 'Arun Jaitley Stadium', UNIX_TIMESTAMP('2026-04-03 19:30:00') * 1000, 3, 'SCHEDULED', 'LEAGUE', 52, 48),
(7, 8, 'Sawai Mansingh Stadium', UNIX_TIMESTAMP('2026-04-04 19:30:00') * 1000, 4, 'SCHEDULED', 'LEAGUE', 48, 52),
(9, 10, 'Narendra Modi Stadium', UNIX_TIMESTAMP('2026-04-05 19:30:00') * 1000, 5, 'SCHEDULED', 'LEAGUE', 50, 50),
(1, 4, 'Wankhede Stadium', UNIX_TIMESTAMP('2026-04-06 19:30:00') * 1000, 6, 'SCHEDULED', 'LEAGUE', 53, 47),
(2, 3, 'MA Chidambaram Stadium', UNIX_TIMESTAMP('2026-04-07 19:30:00') * 1000, 7, 'SCHEDULED', 'LEAGUE', 51, 49);

-- Insert 5 Questions for Match 1 (MI vs CSK)
INSERT INTO questions (match_id, question_text, option_a, option_b, option_c, option_d, correct_option, points_value, is_active, question_type, created_at) VALUES
(1, 'Who will win the match?', 'MI', 'CSK', NULL, NULL, 'A', 10, TRUE, 'WINNER', UNIX_TIMESTAMP() * 1000),
(1, 'How many runs will the winning team score?', 'Below 150', '150-170', '170-190', 'Above 190', 'C', 10, TRUE, 'SCORE_RANGE', UNIX_TIMESTAMP() * 1000),
(1, 'Which player will score the most runs?', 'Rohit Sharma', 'Surya Kumar Yadav', 'MS Dhoni', 'Ruturaj Gaikwad', 'B', 10, TRUE, 'TOP_SCORER', UNIX_TIMESTAMP() * 1000),
(1, 'How many wickets will fall in the match?', 'Below 5', '5-7', '8-10', 'Above 10', 'C', 10, TRUE, 'WICKETS', UNIX_TIMESTAMP() * 1000),
(1, 'Which bowler will take the most wickets?', 'Jasprit Bumrah', 'Ravindra Jadeja', 'Other', 'No wickets', 'A', 10, TRUE, 'TOP_BOWLER', UNIX_TIMESTAMP() * 1000);