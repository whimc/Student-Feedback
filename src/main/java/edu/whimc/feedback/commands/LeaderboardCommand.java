package edu.whimc.feedback.commands;

import edu.whimc.feedback.StudentFeedback;
import edu.whimc.feedback.assessments.*;
import edu.whimc.feedback.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Class to define leaderboard command
 */
public class LeaderboardCommand  implements CommandExecutor, TabCompleter {
    private StudentFeedback plugin;
    private final String COMMAND = "leaderboard";
    public static final String LEADERBOARD_PERM = StudentFeedback.PERM_PREFIX + ".leaderboard";
    /**
     * Constructor to set instance variable
     * @param plugin the StudentFeedback plugin instance
     */
    public LeaderboardCommand(StudentFeedback plugin){
        this.plugin = plugin;
    }

    /**
     * Defines behavior of command when invoked
     * @param commandSender the player sending the command
     * @param command the command being sent
     * @param s the command alias
     * @param strings the arguments sent with the command (separate elements are the words separated by spaces in the command)
     * @return boolean for command execution
     */
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)) {
            Utils.msg(commandSender, ChatColor.RED + "You must be a player!");
            return true;
        }
        if (!commandSender.hasPermission(LEADERBOARD_PERM)) {
            commandSender.sendMessage(
                    "You do not have the required permission!");
            return true;
        }
        Player sender = (Player) commandSender;
        HashMap<Player,Long> sessions = plugin.getPlayerSessions();
        if(plugin.getDatabase()) {
            this.plugin.getQueryer().storeNewProgressCommand(sender, COMMAND, id -> {
                this.getSortedLeaderboard(sessions, sorted -> {
                    Utils.sendLeaderboardFeedback(sender, (ArrayList<OverallAssessment>) sorted);
                });
            });
        }
        return true;
    }

    /**
     * Method to synchronously sort the leaderboard with the player's scores
     * @param sessions the players and times of people on the server
     * @param callback the callback
     */
    public void getSortedLeaderboard(HashMap<Player, Long> sessions, Consumer callback){
        if(plugin.getDatabase()) {
            async(() -> {
                ArrayList<OverallAssessment> scores = new ArrayList<>();
                AtomicInteger ctr = new AtomicInteger();
                for (Map.Entry<Player, Long> entry : sessions.entrySet()) {
                    Player player = entry.getKey();
                    Long sessionStart = entry.getValue();

                    plugin.getQueryer().getOverallAssessment(player, sessionStart, assessment -> {
                        scores.add(assessment);
                        ctr.getAndIncrement();
                        if (ctr.get() == sessions.keySet().size()) {
                            scores.sort(new AssessmentComparator());
                            sync(callback, scores);
                        }
                    });
                }

            });
        }
    }

    private <T> void sync(Consumer<T> cons, T val) {
        Bukkit.getScheduler().runTask(this.plugin, () -> cons.accept(val));
    }

    private void sync(Runnable runnable) {
        Bukkit.getScheduler().runTask(this.plugin, runnable);
    }
    private void async(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, runnable);
    }

    /**
     * Allows tab completion
     * @param commandSender player sending the command
     * @param command the command being sent
     * @param s the command alias
     * @param strings the args of the command
     * @return list of string for tab completion for command
     */
    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return null;
    }
}
