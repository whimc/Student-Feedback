package edu.whimc.feedback;

import edu.whimc.feedback.assessments.*;
import edu.whimc.feedback.commands.LeaderboardCommand;
import edu.whimc.feedback.commands.ProgressCommand;
import edu.whimc.feedback.utils.Utils;
import edu.whimc.feedback.utils.sql.Queryer;
import edu.whimc.observations.models.Observation;
import edu.whimc.observations.models.ObserveEvent;
import edu.whimc.observations.observetemplate.models.ObservationTemplate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.List;


/**
 * Class to create plugin and enable it in MC
 * @author sam
 */
public class StudentFeedback extends JavaPlugin implements Listener {
    private static StudentFeedback instance;
    private Queryer queryer;
    private HashMap<Player,Long> sessions;
    private Boolean database;
    public static final String PERM_PREFIX = "whimc-feedback";
    /**
     * Method to return instance of plugin
     * @return instance of StudentFeedback plugin
     */
    public static StudentFeedback getInstance() {
        return instance;
    }

    /**
     * Method to enable plugin
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        sessions = new HashMap<>();
        database = this.getConfig().getBoolean("database");
        Permission parent = new Permission(PERM_PREFIX + ".*");
        Bukkit.getPluginManager().addPermission(parent);

        StudentFeedback.instance = this;
        this.queryer = new Queryer(this, q -> {
            // If we couldn't connect to the database disable the plugin
            if (q == null) {
                this.getLogger().severe("Could not establish MySQL connection! Disabling plugin...");
                getCommand("progress").setExecutor(this);
                getCommand("leaderboard").setExecutor(this);
                return;
            }


            });


        getCommand("progress").setExecutor(new ProgressCommand(this));

        getCommand("leaderboard").setExecutor(new LeaderboardCommand(this));

        Bukkit.getServer().getPluginManager().registerEvents(this, this);

    }

    /**
     * Returns queryer
     * @return queryer
     */
    public Queryer getQueryer() {
        return this.queryer;
    }

    /**
     * Returns current sessions on server
     * @return sessions on server
     */
    public HashMap getPlayerSessions(){return this.sessions;}

    /**
     * When players join they are added to sessions
     * @param event PlayerJoinEvent
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        sessions.putIfAbsent(event.getPlayer(), System.currentTimeMillis());
    }

    /**
     * When players leave their progress is saved to db and they are removed from sessions
     * @param event PlayerQuitEvent
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        Long sessionStart = sessions.get(player);
        if(sessionStart == null){
            return;
        }
        if(database) {
            queryer.getOverallAssessment(player, sessionStart, assessment -> {
                queryer.storeNewProgress(assessment, rowID -> {
                    sessions.remove(player);
                });
            });
        } else {
            sessions.remove(player);
        }
    }
    @EventHandler
    public void onObservation(ObserveEvent observationEvent){
        Player player = observationEvent.getPlayer();

        //Need to preprocess lowercase and remove non alpha characters
        Observation observation = observationEvent.getObservation();
        String cleanedObservation = observation.getObservation();
        cleanedObservation = ChatColor.stripColor(Utils.color(observation.getObservation()));
        cleanedObservation = cleanedObservation.replaceAll("[^A-Za-z0-9\\s]", "");
        cleanedObservation = cleanedObservation.toLowerCase();


        String templateString;
        StructureAssessment assessment;
        ObservationTemplate template = observation.getTemplate();
        if(template != null) {
            templateString = template.getType().toString();
        } else {
            templateString = null;
        }
        String worlds = this.getConfig().getString("agent-worlds");
        if(worlds.contains(",")){
            String[] agentWorlds = worlds.split(", ");
            for(int k = 0; k < agentWorlds.length; k++){
                String worldName = agentWorlds[k];
                if(player.getWorld().getName().equalsIgnoreCase(worldName)) {
                    assessment = new StructureAssessment(cleanedObservation, templateString);
                    assessment.predict();
                    if (database) {
                        StructureAssessment finalAssessment = assessment;
                        this.getQueryer().updateSkills(player, templateString, assessment.getCorrect(), currentSkills -> {
                            Utils.sendOpenLearnerModel(player, (List<Double>) currentSkills);
                            player.sendMessage(finalAssessment.getFeedback());
                        });
                        break;
                    }
                }
            }
        } else {
            if(player.getWorld().getName().equalsIgnoreCase(worlds)) {
                assessment = new StructureAssessment(cleanedObservation, templateString);
                assessment.predict();
                if (database) {
                    StructureAssessment finalAssessment1 = assessment;
                    this.getQueryer().updateSkills(player, templateString, assessment.getCorrect(), currentSkills -> {
                        Utils.sendOpenLearnerModel(player, (List<Double>) currentSkills);
                        player.sendMessage(finalAssessment1.getFeedback());
                    });
                }
            }
        }
    }

    public Boolean getDatabase() {
        return database;
    }

    /**
     * Display when plugin cannot be enabled
     * @param sender
     * @param command
     * @param label
     * @param args
     * @return
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Utils.msg(sender, "&cThis plugin is disabled because it was unable to connect to the configured database. " +
                "Please modify the config to ensure the credentials are correct then restart the server.");
        return true;
    }

}
