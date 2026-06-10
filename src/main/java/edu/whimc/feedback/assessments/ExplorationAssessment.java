package edu.whimc.feedback.assessments;

import edu.whimc.feedback.StudentFeedback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;


/**
 * Class to define exploration assessment (copied from Path-Generator)
 */
public class ExplorationAssessment extends ProgressAssessment{

    /**
     * Constructor to set instance variables in super class and get plugin
     * @param player player invoking command
     * @param sessionStart time when the player joined the server
     * @param resultSet the worlds and points on the map that the player has visited
     * @param plugin the StudentFeedback plugin to access the config
     */
    public ExplorationAssessment(Player player, Long sessionStart, Object resultSet, StudentFeedback plugin) {
        super(player, sessionStart, resultSet, plugin);
    }

    /**
     * Method to return the exploration assessment based on separating maps into a 10x10 grid and measuring the number of grid positions visited,
     * normalized to a 0-100 score
     * @return exploration metric
     */
    @Override
    public double metric() {
        HashMap<String, ArrayList<Point>> positions = (HashMap<String, ArrayList<Point>>) this.getResultSet();
        int score = 0;
        for (Map.Entry<String, ArrayList<Point>> entry : positions.entrySet()) {
            String world = entry.getKey();
            ArrayList<Point> points = entry.getValue();
            int pixelRatio = getPlugin().getConfig().getInt("worlds." + world + ".pixel_to_block_ratio");
            int min_x = getPlugin().getConfig().getInt("worlds." + world + ".top_left_coordinate_x");
            int min_z = getPlugin().getConfig().getInt("worlds." + world + ".top_left_coordinate_z");
            BufferedImage img = null;
            int max_x = 0;
            int max_z = 0;
            try {
                img = ImageIO.read(this.getClass().getResource("/maps/" + world + ".png"));
                max_x = min_x + img.getWidth() / pixelRatio;
                max_z = min_z + img.getHeight() / pixelRatio;
            } catch (Exception e) {
                continue;
            }
            int[][] mapMatrices = new int[10][10];
            for (int k = 0; k < points.size(); k++) {
                double x = points.get(k).getX();
                double z = points.get(k).getY();
                if (!is_inside_view(min_x, min_z, max_x, max_z, x, z)) {
                    continue;
                }
                int row = (int) scale(x, new int[]{min_x, max_x}, new int[]{0, 10});
                int col = (int) scale(z, new int[]{min_z, max_z}, new int[]{0, 10});

                mapMatrices[row][col] = 1;
            }
            for (int i = 0; i < mapMatrices.length; i++) {
                for (int k = 0; k < mapMatrices[i].length; k++) {
                    if (mapMatrices[i][k] == 1)
                        score += mapMatrices[i][k];
                }
            }
        }
            return normalize(score, "exploration");
        }

    /**
     * Determines if this coordinate is inside of the World's view
     * @param min_x the minimum x value on the map
     * @param min_z the minimum z value on the map
     * @param max_x the maximum x value on the map
     * @param max_z the maximum z value on the map
     * @param x the x position visited
     * @param z the z position visited
     * @return whether the player position is inside the World's view
     */
    public boolean is_inside_view(int min_x, int min_z, int max_x, int max_z, double x, double z){
        return (min_x < x && x < max_x) && (min_z < z && z < max_z);
    }

    /**
     * Scale the given value from the scale of src to the scale of dst
     * @param val the value to scale (x or z coordinate)
     * @param src the minimum and maximum values of the dimension
     * @param dst the minimum and maximum values of the final value to scale to
     * @return scaled x or z coordinate within the 10x10 grid
     */
    public double scale(double val, int[]src, int[]dst){
        return ((val - src[0]) / (src[1]-src[0])) * (dst[1]-dst[0]) + dst[0];
    }

    /**
     * Returns display name of assessment
     * @return display name of assessment
     */
    @Override
    public String getName() {
        return "Exploration Assessment";
    }
}
