package edu.whimc.feedback.assessments;

import edu.whimc.feedback.StudentFeedback;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to define science tool assessment
 */
public class ScienceToolsAssessment extends ProgressAssessment{
    public ScienceToolsAssessment(Player player, Long sessionStart, Object resultSet, StudentFeedback plugin) {
        super(player, sessionStart, resultSet, plugin);
    }

    /**
     * Returns a 0-100 score based on unique science tools used per world plus a smaller
     * bonus for repeated uses of the same tool.
     * @return science tools metric
     */
    @Override
    public double metric() {
        HashMap<String, HashMap<String, Integer>> tools =
                (HashMap<String, HashMap<String, Integer>>) this.getResultSet();
        double uniquePoints = getPlugin().getConfig().getDouble("science-tools.unique-tool-points", 1.0);
        double repeatPoints = getPlugin().getConfig().getDouble("science-tools.repeat-use-points", 0.25);
        int repeatCap = getPlugin().getConfig().getInt("science-tools.repeat-use-cap-per-tool", 4);

        double score = 0;
        for (Map.Entry<String, HashMap<String, Integer>> worldEntry : tools.entrySet()) {
            for (Map.Entry<String, Integer> toolEntry : worldEntry.getValue().entrySet()) {
                int uses = toolEntry.getValue();
                if (uses <= 0) {
                    continue;
                }
                score += uniquePoints;
                int extraUses = Math.max(0, uses - 1);
                score += Math.min(repeatCap, extraUses) * repeatPoints;
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
