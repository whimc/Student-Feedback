package edu.whimc.feedback.utils.sql.migration.schemas;

import edu.whimc.feedback.utils.sql.migration.SchemaVersion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Schema_2 extends SchemaVersion {
    private static final String CREATE_TABLE =
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
    /**
     * Constructor to specify which migrations to do
     */
    public Schema_2() {
        super(2, new Schema_3());
    }
    @Override
    protected void migrateRoutine(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(CREATE_TABLE)) {
            statement.execute();
        } catch (Exception e){

        }
    }
}
