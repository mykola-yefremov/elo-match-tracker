CREATE INDEX IF NOT EXISTS idx_match_winner_id ON match (winner_id);
CREATE INDEX IF NOT EXISTS idx_match_loser_id ON match (loser_id);
CREATE INDEX IF NOT EXISTS idx_match_created_at ON match (created_at);
