ALTER TABLE match
    ADD COLUMN winner_score INTEGER,
    ADD COLUMN loser_score INTEGER,
    ADD COLUMN note VARCHAR(500);

ALTER TABLE match
    ADD CONSTRAINT match_score_order_chk
        CHECK (winner_score IS NULL OR loser_score IS NULL OR winner_score > loser_score),
    ADD CONSTRAINT match_score_non_negative_chk
        CHECK ((winner_score IS NULL OR winner_score >= 0) AND (loser_score IS NULL OR loser_score >= 0)),
    ADD CONSTRAINT match_score_completeness_chk
        CHECK (
            (winner_score IS NULL AND loser_score IS NULL)
            OR (winner_score IS NOT NULL AND loser_score IS NOT NULL)
        );

CREATE INDEX idx_match_winner_created_at ON match (winner_id, created_at DESC);
CREATE INDEX idx_match_loser_created_at ON match (loser_id, created_at DESC);
