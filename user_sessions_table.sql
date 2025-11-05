-- Create user_sessions table for tracking login sessions
-- Run this in your Supabase SQL Editor

CREATE TABLE IF NOT EXISTS user_sessions (
    session_id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    login_time TIMESTAMP NOT NULL DEFAULT NOW(),
    logout_time TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ip_address VARCHAR(50),
    device_info TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_user_sessions_email ON user_sessions(email);
CREATE INDEX IF NOT EXISTS idx_user_sessions_active ON user_sessions(is_active);
CREATE INDEX IF NOT EXISTS idx_user_sessions_login_time ON user_sessions(login_time);

-- Enable Row Level Security
ALTER TABLE user_sessions ENABLE ROW LEVEL SECURITY;

-- Create policies for authenticated users
CREATE POLICY "Users can view their own sessions"
    ON user_sessions
    FOR SELECT
    USING (auth.email() = email);

CREATE POLICY "System can insert sessions"
    ON user_sessions
    FOR INSERT
    WITH CHECK (true);

CREATE POLICY "System can update sessions"
    ON user_sessions
    FOR UPDATE
    USING (true);

-- Add comment
COMMENT ON TABLE user_sessions IS 'Tracks user login sessions for anomaly detection';
