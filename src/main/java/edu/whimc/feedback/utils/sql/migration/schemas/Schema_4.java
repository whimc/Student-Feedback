package edu.whimc.feedback.utils.sql.migration.schemas;

import edu.whimc.feedback.utils.sql.migration.SchemaRepair;
import edu.whimc.feedback.utils.sql.migration.SchemaVersion;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Schema to add the POI exploration score column to the progress table
 */
public class Schema_4 extends SchemaVersion {
    private static final Logger LOGGER = Logger.getLogger("WHIMC-StudentFeedback");

    /**
     * Constructor to specify which migrations to do
     */
    public Schema_4() {
        super(4, null);
    }

    @Override
    protected void migrateRoutine(Connection connection) throws SQLException {
        SchemaRepair.ensureSchema(connection, LOGGER);
    }
}
