package edu.whimc.feedback.assessments;

import edu.whimc.feedback.StudentFeedback;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Class to define observation assessment
 */
public class ObservationAssessment extends ProgressAssessment{

    /**
     * Constructor to set instance variables in super class
     * @param player player invoking command
     * @param sessionStart time when the player joined the server
     * @param resultSet the worlds and observation texts of observations during the session
     * @param plugin the StudentFeedback plugin to access the config
     */
    public ObservationAssessment(Player player, Long sessionStart, Object resultSet, StudentFeedback plugin) {
        super(player, sessionStart, resultSet, plugin);
    }

    /**
     * Returns a 0-100 score combining the number of observations and the total
     * word count of those observations during the session (each weighted equally)
     * @return observation metric
     */
    @Override
    public double metric() {
        HashMap<String, ArrayList<String>> observations = (HashMap<String, ArrayList<String>>) this.getResultSet();
        int count = 0;
        int words = 0;
        for(Map.Entry<String, ArrayList<String>> entry : observations.entrySet()) {
            for(String observation : entry.getValue()){
                count++;
                String trimmed = observation == null ? "" : observation.trim();
                if(!trimmed.isEmpty()){
                    words += trimmed.split("\\s+").length;
                }
            }
        }
        return (normalize(count, "observation-count") + normalize(words, "observation-words")) / 2.0;
    }


    /**
     * Returns display name of assessment
     * @return display name of assessment
     */
    @Override
    public String getName() {
        return "Observation Assessment";
    }
}
