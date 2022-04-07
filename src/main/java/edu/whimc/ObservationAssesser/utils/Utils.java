package edu.whimc.ObservationAssesser.utils;

import com.google.common.base.Strings;
import edu.whimc.ObservationAssesser.ObservationAssesser;

import edu.whimc.ObservationAssesser.assessments.*;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Array;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class to define utility methods
 */
public class Utils {

    private static final String PREFIX = "&8&l[&9&lObservations&8&l]&r ";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM d yyyy, h:mm a z");
    private static boolean debug = false;
    private static String debugPrefix = "[Observations] ";

    public static void setDebug(boolean shouldDebug) {
        debug = shouldDebug;
    }

    /**
     * Prints a debug message if "debug" is true.
     *
     * @param str Message to print
     */
    public static void debug(String str) {
        if (!debug) return;
        Bukkit.getLogger().info(color(debugPrefix + str));
    }

    /**
     * Sets the debug message prefix.
     *
     * @param prefix Prefix to be set
     */
    public static void setDebugPrefix(String prefix) {
        debugPrefix = "[" + prefix + "] ";
    }

    /**
     * Gets a nice formatted date.
     *
     * @param timestamp Timestamp of date to format
     * @return A formatted version of the given date
     */
    public static String getDate(Timestamp timestamp) {
        return DATE_FORMAT.format(new Date(timestamp.getTime()));
    }

    public static String getDateNow() {
        return getDate(new Timestamp(System.currentTimeMillis()));
    }

    public static Timestamp parseDate(String str) {
        try {
            return new Timestamp(DATE_FORMAT.parse(str).getTime());
        } catch (ParseException e) {
            return null;
        }
    }

    public static void msg(CommandSender sender, String... messages) {
        for (int ind = 0; ind < messages.length; ind++) {
            if (ind == 0) {
                sender.sendMessage(color(PREFIX + messages[ind]));
            } else {
                sender.sendMessage(color(messages[ind]));
            }
        }
    }

    public static String locationString(Location loc, boolean yawPitch) {
        NumberFormat formatter = new DecimalFormat("#0.00");

        StringBuilder message = new StringBuilder();
        message.append("&7World: &f&o" + loc.getWorld().getName());
        message.append("  &7X: &f&o" + formatter.format(loc.getX()));
        message.append("  &7Y: &f&o" + formatter.format(loc.getY()));
        message.append("  &7Z: &f&o" + formatter.format(loc.getZ()));

        if (yawPitch) {
            message.append("\n" + "    &7Pitch: &f&o" + formatter.format(loc.getPitch()));
            message.append("  &7Yaw: &f&o" + formatter.format(loc.getYaw()));
        }

        return message.toString();
    }


    public static String coloredSubstring(String str, int length) {
        str = color(str);
        StringBuilder newStr = new StringBuilder();
        int count = 0;
        boolean ignore = false;
        for (char chr : str.toCharArray()) {
            if (count >= length) break;
            newStr.append(chr);

            if (ignore) {
                ignore = false;
                continue;
            }

            if (chr == ChatColor.COLOR_CHAR) ignore = true;
            if (chr != ChatColor.COLOR_CHAR && !ignore) count++;
        }

        return newStr.toString().replace(ChatColor.COLOR_CHAR, '&');
    }

    public static void msgNoPrefix(CommandSender sender, Object... messages) {
        for (Object str : messages) {
            if (str instanceof BaseComponent) {
                sender.spigot().sendMessage((BaseComponent) str);
            } else {
                sender.sendMessage(color(str.toString()));
            }
        }
    }

    public static String color(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public static Integer parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer parseIntWithError(CommandSender sender, String str) {
        Integer id = parseInt(str);
        if (id == null) {
            Utils.msg(sender, "&c\"&4" + str + "&c\" is an invalid number!");
            return null;
        }

        return id;
    }

    public static List<String> getWorldsTabComplete(String hint) {
        return Bukkit.getWorlds().stream()
                .filter(v -> v.getName().toLowerCase().startsWith(hint))
                .map(World::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Method to build an open-learner model progress bar for students
     * @param current current skill value for student
     * @param max maximum skill value for skill
     * @param totalBars number of bars that make up progress bar
     * @param symbol symbol to fill progress bar with
     * @param completedColor color for progress bar completion
     * @param notCompletedColor color for empty progress bar
     * @return progress bar of the student skill
     */
    public static String getProgressBar(double current, int max, int totalBars, char symbol, String completedColor,
                                        String notCompletedColor) {
        current = current*100;
        float percent = (float) current / max;
        int progressBars = (int) (totalBars * percent);

        return Utils.color(Strings.repeat("" + completedColor + symbol, progressBars)
                + Strings.repeat("" + notCompletedColor + symbol, totalBars - progressBars));
    }

    /**
     * Method to display open-learner model to student
     * @param player the student to send the OLM to
     * @param skills the students current skills
     */
    public static void sendOpenLearnerModel(Player player, List<Double> skills){
        player.sendMessage("§e§lAnalogy:      §8[§r" + Utils.getProgressBar(skills.get(0), 100, 40, '|', "§a", "§7") + "§8]");
        player.sendMessage("§e§lComparative: §8[§r" + Utils.getProgressBar(skills.get(1), 100, 40, '|', "§9", "§7") + "§8]");
        player.sendMessage("§e§lDescriptive:  §8[§r" + Utils.getProgressBar(skills.get(2), 100, 40, '|', "§6", "§7") + "§8]");
        player.sendMessage("§e§lInference:   §8[§r" + Utils.getProgressBar(skills.get(3), 100, 40, '|', "§d", "§7") + "§8]");
    }


    public static void sendProgressFeedback(OverallAssessment assessment){
        Player player = assessment.getPlayer();
        player.sendMessage("Here is your progress for this session\n"+
                assessment.getObservationAssessment().getName()+": "+assessment.getObservationAssessment().metric()+"\n"+
                assessment.getScienceToolAssessment().getName()+": "+assessment.getScienceToolAssessment().metric()+"\n"+
                assessment.getExplorationAssessment().getName()+": "+assessment.getExplorationAssessment().metric()+"\n"+
                assessment.getQuestAssessment().getName()+": "+assessment.getQuestAssessment().metric()+"\n"+
                assessment.getName()+": "+assessment.metric());
    }

    public static void sendLeaderboardFeedback(Player sender, ArrayList<OverallAssessment> scores){
        sender.sendMessage("LEADERBOARD");
        for(int k = 0; k < scores.size(); k++){
            int position = k+1;
            sender.sendMessage(position+")\t"+ scores.get(k).getPlayer().getName()+": "+scores.get(k).metric());
        }
    }

}
