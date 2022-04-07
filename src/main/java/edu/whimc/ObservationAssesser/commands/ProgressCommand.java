package edu.whimc.ObservationAssesser.commands;

import edu.whimc.ObservationAssesser.ObservationAssesser;
import edu.whimc.ObservationAssesser.assessments.*;
import edu.whimc.ObservationAssesser.utils.Utils;
import edu.whimc.ObservationAssesser.utils.sql.Queryer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

import edu.whimc.ObservationAssesser.utils.Utils;
/**
 * Class to define assessment command
 */
public class ProgressCommand implements CommandExecutor, TabCompleter {

    private final ObservationAssesser plugin;

    /**
     * Constructor to set instance variable
     * @param plugin the ObservationAssesser plugin instance
     */
    public ProgressCommand(ObservationAssesser plugin) {
        this.plugin = plugin;
    }

    /**
     * Defines behavior of command when invoked
     * @param commandSender the player sending the command
     * @param command the command being sent
     * @param s the command alias
     * @param args the arguments sent with the command (separate elements are the words separated by spaces in the command)
     * @return boolean for command execution
     */
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        Player player = (Player) commandSender;

        HashMap<Player,Long> sessions = plugin.getPlayerSessions();
        Long sessionStart = sessions.get(player);
        if(sessionStart == null){
            return false;
        }
        plugin.getQueryer().getSessionObservations(player, sessionStart, observations -> {
            ObservationAssessment obs = new ObservationAssessment(player, sessionStart,observations);
            plugin.getQueryer().getSessionScienceTools(player, sessionStart, scienceTools -> {
                ScienceToolsAssessment sci = new ScienceToolsAssessment(player, sessionStart, scienceTools);
                plugin.getQueryer().getSessionPositions(player,sessionStart, positions -> {
                    ExplorationAssessment exp = new ExplorationAssessment(player, sessionStart, positions, plugin);
                    QuestAssessment quest = new QuestAssessment(player, sessionStart, null);
                    OverallAssessment assessment = new OverallAssessment(player, sessionStart, null, obs, sci, exp, quest);
                    Utils.sendProgressFeedback(assessment);
                });
            });
        });
        return true;
    }

    /**
     * Not necessary since command should not be invoked manually
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