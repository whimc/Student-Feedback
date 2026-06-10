package edu.whimc.feedback.utils.sql.migration.schemas;

import edu.whimc.feedback.utils.sql.migration.SchemaVersion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Schema to add the POI exploration score column to the progress table
 */
public class Schema_4 extends SchemaVersion {
    private static final String ALTER_PROGRESS =
            "ALTER TABLE `whimc_progress` ADD COLUMN `poi_exploration` DOUBLE NOT NULL DEFAULT 0;";

    /**
     * Constructor to specify which migrations to do
     */
    public Schema_4() {
        super(4, null);
    }

    @Override
    protected void migrateRoutine(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(ALTER_PROGRESS)) {
            statement.execute();
        } catch (Exception e){

        }
    }
}
