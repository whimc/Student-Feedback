package edu.whimc.feedback.utils.sql.migration.schemas;

import edu.whimc.feedback.utils.sql.migration.SchemaVersion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Schema_3  extends SchemaVersion {
    private static final String CREATE_PROGRESS =
            "CREATE TABLE IF NOT EXISTS `whimc_progress_commands` (" +
                    "  `rowid`       INT    AUTO_INCREMENT NOT NULL," +
                    "  `uuid`        VARCHAR(36)           NOT NULL," +
                    "  `username`    VARCHAR(16)           NOT NULL," +
                    "  `world`    VARCHAR(36)           NOT NULL," +
                    "  `time`        BIGINT                NOT NULL," +
                    "  `command`    VARCHAR(36)           NOT NULL," +
                    "  PRIMARY KEY    (`rowid`));";

    private static final String CREATE_INTERACTION =
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

    /**
     * Constructor to specify which migrations to do
     */
    public Schema_3() {
        super(3, new Schema_4());
    }
    @Override
    protected void migrateRoutine(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(CREATE_PROGRESS)) {
            statement.execute();
        } catch (Exception e){

        }
        try (PreparedStatement statement = connection.prepareStatement(CREATE_INTERACTION)) {
            statement.execute();
        } catch (Exception e){

        }
    }
}
