package edu.whimc.feedback.assessments;

import edu.whimc.feedback.StudentFeedback;
import edu.whimc.feedback.utils.ObservationQualityScorer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
     * Returns a 0-100 score combining observation count, word count, and scientific
     * quality heuristics (questions, decimals, units, comparison/causal language).
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

        ObservationQualityScorer qualityScorer = new ObservationQualityScorer(getPlugin());
        double quality = qualityScorer.scoreSession(observations);

        double countScore = normalize(count, "observation-count");
        double wordsScore = normalize(words, "observation-words");
        double qualityScore = normalize(quality, "observation-quality");
        return (countScore + wordsScore + qualityScore) / 3.0;
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
