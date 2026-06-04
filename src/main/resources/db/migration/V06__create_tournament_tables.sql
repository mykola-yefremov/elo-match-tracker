CREATE TABLE tournament
(
    tournament_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name          TEXT                     NOT NULL,
    player_count  INTEGER                  NOT NULL CHECK (player_count IN (2, 4, 8, 16)),
    seeding_mode  TEXT                     NOT NULL CHECK (seeding_mode IN ('MANUAL', 'RANDOM')),
    game_format   TEXT                     NOT NULL CHECK (game_format IN ('BO1', 'BO3', 'BO5')),
    winning_points INTEGER                 NOT NULL CHECK (winning_points > 0),
    bracket_type  TEXT                     NOT NULL CHECK (bracket_type IN ('SINGLE_ELIMINATION', 'ROUND_ROBIN')),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE tournament_participant
(
    tournament_participant_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    tournament_id             BIGINT  NOT NULL REFERENCES tournament (tournament_id) ON DELETE CASCADE,
    player_id                 BIGINT  NOT NULL REFERENCES player (player_id),
    seed_number               INTEGER NOT NULL CHECK (seed_number > 0),
    CONSTRAINT uq_tournament_participant_player UNIQUE (tournament_id, player_id),
    CONSTRAINT uq_tournament_participant_seed UNIQUE (tournament_id, seed_number)
);

CREATE INDEX IF NOT EXISTS idx_tournament_created_at ON tournament (created_at);
CREATE INDEX IF NOT EXISTS idx_tournament_participant_tournament_id
    ON tournament_participant (tournament_id);
CREATE INDEX IF NOT EXISTS idx_tournament_participant_player_id
    ON tournament_participant (player_id);
