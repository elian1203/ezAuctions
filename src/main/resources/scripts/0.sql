-- This creates the base tables used by all plugin version 1.5.4 and under

CREATE TABLE IF NOT EXISTS AUCTION_PLAYERS(
    player CHAR(36) NOT NULL PRIMARY KEY,
    ignoringSpam BOOLEAN,
    ignoringALL BOOLEAN,
    ignoringScoreboard BOOLEAN
);

CREATE TABLE IF NOT EXISTS AUCTION_PLAYERS_IGNORED(
    player CHAR(36),
    ignored CHAR(36)
);

CREATE TABLE IF NOT EXISTS AUCTION_PLAYERS_ITEMS(
    player CHAR(36),
    items TEXT
);