package edu.whimc.feedback.assessments;

import edu.whimc.feedback.StudentFeedback;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to define POI exploration assessment, which counts dwell events: each time a player
 * spends more than MIN_DWELL_SECONDS near an NPC/POI from the journey_waypoints table
 */
public class POIExplorationAssessment extends ProgressAssessment{

    /** Minimum continuous time (seconds) near a waypoint to count as a dwell event */
    public static final long MIN_DWELL_SECONDS = 2;

    /**
     * Constructor to set instance variables in super class
     * @param player player invoking command
     * @param sessionStart time when the player joined the server
     * @param resultSet Object[] of {waypoints per world, timed positions per world}
     * @param plugin the StudentFeedback plugin to access the config
     */
    public POIExplorationAssessment(Player player, Long sessionStart, Object resultSet, StudentFeedback plugin) {
        super(player, sessionStart, resultSet, plugin);
    }

    /**
     * Returns a 0-100 score based on the number of times the player dwelled near a POI/NPC
     * for more than MIN_DWELL_SECONDS during the session
     * @return POI exploration metric
     */
    @Override
    public double metric() {
        Object[] data = (Object[]) this.getResultSet();
        HashMap<String, ArrayList<double[]>> waypoints = (HashMap<String, ArrayList<double[]>>) data[0];
        HashMap<String, ArrayList<long[]>> positions = (HashMap<String, ArrayList<long[]>>) data[1];
        double radius = getPlugin().getConfig().getDouble("poi-radius", 5);

        int dwellEvents = 0;
        for (Map.Entry<String, ArrayList<double[]>> entry : waypoints.entrySet()) {
            ArrayList<long[]> worldPositions = positions.get(entry.getKey());
            if (worldPositions == null) {
                continue;
            }
            for (double[] waypoint : entry.getValue()) {
                dwellEvents += countDwellEvents(waypoint, worldPositions, radius);
            }
        }
        return normalize(dwellEvents, "poi-exploration");
    }

    /**
     * Counts the number of separate times the player stayed within radius of the waypoint
     * for at least MIN_DWELL_SECONDS (positions must be in time order)
     * @param waypoint the waypoint as [x, z]
     * @param positions the player's session positions in the waypoint's world as [time (seconds), x, z]
     * @param radius distance in blocks considered "near" the waypoint
     * @return number of dwell events at this waypoint
     */
    private int countDwellEvents(double[] waypoint, ArrayList<long[]> positions, double radius) {
        int count = 0;
        long streakStart = -1;
        long lastNear = -1;
        for (long[] position : positions) {
            double dx = position[1] - waypoint[0];
            double dz = position[2] - waypoint[1];
            boolean near = Math.sqrt(dx * dx + dz * dz) <= radius;
            if (near) {
                if (streakStart == -1) {
                    streakStart = position[0];
                }
                lastNear = position[0];
            } else if (streakStart != -1) {
                if (lastNear - streakStart >= MIN_DWELL_SECONDS) {
                    count++;
                }
                streakStart = -1;
            }
        }
        if (streakStart != -1 && lastNear - streakStart >= MIN_DWELL_SECONDS) {
            count++;
        }
        return count;
    }

    /**
     * Returns display name of assessment
     * @return display name of assessment
     */
    @Override
    public String getName() {
        return "POI Exploration Assessment";
    }
}
