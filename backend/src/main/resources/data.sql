-- DML Script for IPL Predictor Database
-- Sample data for testing

-- Insert teams
INSERT INTO teams (team_name, short_name, home_city, stadium, logo_url, team_color, matches_played, matches_won, matches_lost, net_run_rate, points) VALUES
('Mumbai Indians', 'MI', 'Mumbai', 'Wankhede Stadium', '/logos/mi.webp', '#004BA0', 0, 0, 0, 0, 0),
('Chennai Super Kings', 'CSK', 'Chennai', 'MA Chidambaram Stadium', '/logos/csk.png', '#F2C900', 0, 0, 0, 0, 0),
('Kolkata Knight Riders', 'KKR', 'Kolkata', 'Eden Gardens', '/logos/kkr.webp', '#2E2E3A', 0, 0, 0, 0, 0),
('Royal Challengers Bangalore', 'RCB', 'Bengaluru', 'M. Chinnaswamy Stadium', '/logos/rcb.webp', '#D61F20', 0, 0, 0, 0, 0),
('Delhi Capitals', 'DC', 'New Delhi', 'Arun Jaitley Stadium', '/logos/dc.webp', '#0078BC', 0, 0, 0, 0, 0),
('Sunrisers Hyderabad', 'SRH', 'Hyderabad', 'Rajiv Gandhi International Cricket Stadium', '/logos/srh.webp', '#F26422', 0, 0, 0, 0, 0),
('Rajasthan Royals', 'RR', 'Jaipur', 'Sawai Mansingh Stadium', '/logos/rr.webp', '#EB138D', 0, 0, 0, 0, 0),
('Punjab Kings', 'PBKS', 'Mohali', 'Punjab Cricket Association IS.bindra Stadium', '/logos/pbks.webp', '#B6911A', 0, 0, 0, 0, 0),
('Lucknow Super Giants', 'LSG', 'Lucknow', 'BRSABVE Cricket Stadium', '/logos/lsg.webp', '#0C2461', 0, 0, 0, 0, 0),
('Gujarat Titans', 'GT', 'Ahmedabad', 'Narendra Modi Stadium', '/logos/gt.webp', '#1D3F6B', 0, 0, 0, 0, 0);

-- Insert sample matches (schedule for 2026 IPL season)
