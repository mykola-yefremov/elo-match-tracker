ALTER TABLE tournament
    ADD COLUMN status TEXT NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
    ADD COLUMN winner_id BIGINT REFERENCES player (player_id),
    ADD COLUMN started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN completed_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE tournament_match
(
    tournament_match_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    tournament_id       BIGINT                   NOT NULL REFERENCES tournament (tournament_id) ON DELETE CASCADE,
    round_number        INTEGER                  NOT NULL CHECK (round_number > 0),
    match_number        INTEGER                  NOT NULL CHECK (match_number > 0),
    first_player_id     BIGINT                   NOT NULL REFERENCES player (player_id),
    second_player_id    BIGINT                   NOT NULL REFERENCES player (player_id),
    winner_id           BIGINT REFERENCES player (player_id),
    status              TEXT                     NOT NULL CHECK (status IN ('PENDING', 'COMPLETED')),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    completed_at        TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_tournament_match_slot UNIQUE (tournament_id, round_number, match_number),
    CONSTRAINT chk_tournament_match_distinct_players CHECK (first_player_id <> second_player_id)
);

CREATE INDEX IF NOT EXISTS idx_tournament_status ON tournament (status);
CREATE INDEX IF NOT EXISTS idx_tournament_winner_id ON tournament (winner_id);
CREATE INDEX IF NOT EXISTS idx_tournament_match_tournament_id ON tournament_match (tournament_id);
CREATE INDEX IF NOT EXISTS idx_tournament_match_status ON tournament_match (status);
CREATE INDEX IF NOT EXISTS idx_tournament_match_winner_id ON tournament_match (winner_id);
