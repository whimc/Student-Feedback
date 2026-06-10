package edu.whimc.feedback.assessments;

import edu.whimc.feedback.StudentFeedback;
import org.bukkit.entity.Player;

/**
 * Abstract class to define assessments
 */
public abstract class ProgressAssessment {
    private Player player;
    private Long sessionStart;
    private Object resultSet;
    private StudentFeedback plugin;

    /**
     * Constructor for abstract class
     * @param player player invoking command
     * @param sessionStart time when player joined
     * @param resultSet result of querying the db
     * @param plugin the StudentFeedback plugin to access the config
     */
    public ProgressAssessment(Player player, Long sessionStart, Object resultSet, StudentFeedback plugin){
        this.player = player;
        this.sessionStart = sessionStart;
        this.resultSet = resultSet;
        this.plugin = plugin;
    }

    /**
     * Returns plugin instance
     * @return plugin instance
     */
    public StudentFeedback getPlugin(){
        return plugin;
    }

    /**
     * Normalizes a raw value to a 0-100 score using a maximum from the config
     * so that every category contributes equally to the overall score
     * @param value raw metric value
     * @param maxConfigKey key under score-maximums in the config holding the value worth 100 points
     * @return score between 0 and 100
     */
    protected double normalize(double value, String maxConfigKey){
        double max = plugin.getConfig().getDouble("score-maximums." + maxConfigKey, 0);
        if (max <= 0) {
            return 0;
        }
        return 100.0 * Math.min(value / max, 1.0);
    }

    /**
     * Returns player
     * @return player
     */
    public Player getPlayer(){
        return player;
    }

    /**
     * Returns session start
     * @return session start
     */
    public long getSessionStart(){
        return sessionStart;
    }

    /**
     * Returns result set from query
     * @return result set
     */
    public Object getResultSet(){return resultSet;}

    /**
     * Method to be defined by subclasses for how to assess interest for individual gameplay behaviors
     * @return assessment of interest
     */
    public abstract double metric();

    /**
     * Returns display name of assessment
     * @return display name of assessment
     */
    public abstract String getName();
}
