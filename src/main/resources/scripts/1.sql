--    This creates a settings table allowing for version control
--    It also modifies the auction players items table, allowing for world-specific items

ALTER TABLE AUCTION_PLAYERS_ITEMS ADD COLUMN world VARCHAR(40);

CREATE TABLE IF NOT EXISTS EZAUC_SETTINGS(
    property VARCHAR(16),
    value INT
);

INSERT INTO EZAUC_SETTINGS(property, value) VALUES('version', 1);