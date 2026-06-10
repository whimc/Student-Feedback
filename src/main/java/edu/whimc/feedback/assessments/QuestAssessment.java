package edu.whimc.feedback.assessments;

import edu.whimc.feedback.StudentFeedback;
import org.bukkit.entity.Player;

import java.util.ArrayList;

/**
 * Class to define quest assessment
 */
public class QuestAssessment extends ProgressAssessment{
    public QuestAssessment(Player player, Long sessionStart, Object resultSet, StudentFeedback plugin) {
        super(player, sessionStart, resultSet, plugin);
    }

    /**
     * Returns a 0-100 score based on the number of quests started plus quests completed
     * @return quest metric
     */
    @Override
    public double metric() {
        ArrayList<String> quests = (ArrayList<String>) this.getResultSet();
        if(quests != null) {
            return normalize(quests.size(), "quests");
        }
        return 0;
    }

    /**
     * Returns display name of assessment
     * @return display name of assessment
     */
    @Override
    public String getName() {
        return "Quest Assessment";
    }
}
