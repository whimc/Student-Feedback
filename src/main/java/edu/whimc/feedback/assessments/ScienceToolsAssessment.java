package edu.whimc.feedback.assessments;

import edu.whimc.feedback.StudentFeedback;
import org.bukkit.entity.Player;


import java.util.*;

/**
 * Class to define science tool assessment
 */
public class ScienceToolsAssessment extends ProgressAssessment{
    public ScienceToolsAssessment(Player player, Long sessionStart, Object resultSet, StudentFeedback plugin) {
        super(player, sessionStart, resultSet, plugin);
    }

    /**
     * Returns a 0-100 score based on the number of unique science tools used per world during the session
     * @return science tools metric
     */
    @Override
    public double metric() {
        HashMap<String, HashSet<String>> tools = (HashMap<String, HashSet<String>>) this.getResultSet();
        int score = 0;
        for(Map.Entry<String, HashSet<String>> entry : tools.entrySet()) {
            for (String ele : entry.getValue()) {
                score++;
            }
        }
        return normalize(score, "science-tools");
    }

    /**
     * Returns display name of assessment
     * @return display name of assessment
     */
    @Override
    public String getName() {
        return "Science Tool Assessment";
    }
}
