package edu.whimc.feedback.utils.sql.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Idempotent schema repairs run on every plugin startup to recover from
 * partially applied migrations or missing tables.
 */
public final class SchemaRepair {

    private static final String CREATE_SKILLS =
            "CREATE TABLE IF NOT EXISTS `whimc_skills` (" +
                    "  `uuid`        VARCHAR(36)           NOT NULL," +
                    "  `username`    VARCHAR(16)           NOT NULL," +
                    "  `analogy`           DOUBLE                NOT NULL," +
                    "  `comparative`           DOUBLE                NOT NULL," +
                    "  `descriptive`           DOUBLE                NOT NULL," +
                    "  `inference`           DOUBLE                NOT NULL," +
                    "  PRIMARY KEY    (`uuid`));";

    private static final String CREATE_PROGRESS =
            "CREATE TABLE IF NOT EXISTS `whimc_progress` (" +
                    "  `rowid`       INT    AUTO_INCREMENT NOT NULL," +
                    "  `uuid`        VARCHAR(36)           NOT NULL," +
                    "  `username`    VARCHAR(16)           NOT NULL," +
                    "  `time`        BIGINT                NOT NULL," +
                    "  `observation`           DOUBLE                NOT NULL," +
                    "  `science_tools`           DOUBLE                NOT NULL," +
                    "  `exploration`           DOUBLE                NOT NULL," +
                    "  `poi_exploration`       DOUBLE                NOT NULL DEFAULT 0," +
                    "  `quest`           DOUBLE                NOT NULL," +
                    "  `score`           DOUBLE                NOT NULL," +
                    "  PRIMARY KEY    (`rowid`));";

    private static final String CREATE_PROGRESS_COMMANDS =
            "CREATE TABLE IF NOT EXISTS `whimc_progress_commands` (" +
                    "  `rowid`       INT    AUTO_INCREMENT NOT NULL," +
                    "  `uuid`        VARCHAR(36)           NOT NULL," +
                    "  `username`    VARCHAR(16)           NOT NULL," +
                    "  `world`    VARCHAR(36)           NOT NULL," +
                    "  `time`        BIGINT                NOT NULL," +
                    "  `command`    VARCHAR(36)           NOT NULL," +
                    "  PRIMARY KEY    (`rowid`));";

    private static final String CREATE_DIALOGUE =
            "CREATE TABLE IF NOT EXISTS `whimc_dialogue` (" +
                    "  `rowid`       INT    AUTO_INCREMENT NOT NULL," +
                    "  `uuid`        VARCHAR(36)           NOT NULL," +
                    "  `username`    VARCHAR(16)           NOT NULL," +
                    "  `world`    VARCHAR(36)           NOT NULL," +
                    "  `time`        BIGINT                NOT NULL," +
                    "  `overall_observation`    INT           NOT NULL," +
                    "  `quests`    INT           NOT NULL," +
                    "  `session_observation`    INT           NOT NULL," +
                    "  `science_tools`    INT           NOT NULL," +
                    "  `exploration_metric`    INT           NOT NULL," +
                    "  `science_topics`    INT           NOT NULL," +
                    "  PRIMARY KEY    (`rowid`));";

    private SchemaRepair() {
    }

    /**
     * Ensures all tables and columns required by the current plugin version exist.
     * @param connection active database connection
     * @param logger plugin logger
     */
    public static void ensureSchema(Connection connection, Logger logger) {
        try {
            ensureTable(connection, "whimc_skills", CREATE_SKILLS, logger);
            ensureTable(connection, "whimc_progress", CREATE_PROGRESS, logger);
            ensureTable(connection, "whimc_progress_commands", CREATE_PROGRESS_COMMANDS, logger);
            ensureTable(connection, "whimc_dialogue", CREATE_DIALOGUE, logger);
            ensureColumn(connection, "whimc_progress", "poi_exploration",
                    "DOUBLE NOT NULL DEFAULT 0", logger);
        } catch (SQLException exc) {
            logger.warning("Failed to repair database schema: " + exc.getMessage());
            exc.printStackTrace();
        }
    }

    private static void ensureTable(Connection connection, String table, String createSql, Logger logger)
            throws SQLException {
        if (tableExists(connection, table)) {
            return;
        }
        logger.info("Creating missing table " + table);
        try (PreparedStatement statement = connection.prepareStatement(createSql)) {
            statement.execute();
        }
    }

    private static void ensureColumn(Connection connection, String table, String column,
                                   String definition, Logger logger) throws SQLException {
        if (!tableExists(connection, table) || columnExists(connection, table, column)) {
            return;
        }
        logger.info("Adding missing column " + table + "." + column);
        try (PreparedStatement statement = connection.prepareStatement(
                "ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition)) {
            statement.execute();
        }
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?")) {
            statement.setString(1, table);
            try (ResultSet results = statement.executeQuery()) {
                results.next();
                return results.getInt(1) > 0;
            }
        }
    }

    private static boolean columnExists(Connection connection, String table, String column)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet results = statement.executeQuery()) {
                results.next();
                return results.getInt(1) > 0;
            }
        }
    }
}
