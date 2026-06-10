package edu.whimc.feedback.utils.sql;

import edu.whimc.feedback.StudentFeedback;

import edu.whimc.feedback.assessments.ExplorationAssessment;
import edu.whimc.feedback.assessments.ObservationAssessment;
import edu.whimc.feedback.assessments.OverallAssessment;
import edu.whimc.feedback.assessments.POIExplorationAssessment;
import edu.whimc.feedback.assessments.QuestAssessment;
import edu.whimc.feedback.assessments.ScienceToolsAssessment;
import edu.whimc.feedback.bkt.Skills;
import edu.whimc.feedback.utils.Utils;
import edu.whimc.feedback.utils.sql.MySQLConnection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles storing position data
 *
 * @author Sam
 */
public class Queryer {

    //Query for inserting skills into the database.
    private static final String QUERY_SAVE_SKILLS =
            "INSERT INTO whimc_skills " +
                    "(uuid, username, analogy, comparative, descriptive, inference) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

    //Query for updating skills in the database.
    private static final String QUERY_UPDATE_SKILLS =
            "UPDATE whimc_skills " +
                    "SET analogy = ?, " + "comparative = ?, " + "descriptive = ?, " + "inference = ? " +
                    "WHERE uuid=?";

    //Query for getting skills from the database.
    private static final String QUERY_GET_PLAYER_SKILLS =
            "SELECT * FROM whimc_skills "+
            "WHERE uuid=?;";

    //Query for getting completed quests from the database.
    private static final String QUERY_GET_QUESTS =
            "SELECT * FROM quests_player_completedquests "+
                    "WHERE uuid=?;";

    //Query for getting started (in-progress) quests from the database.
    private static final String QUERY_GET_STARTED_QUESTS =
            "SELECT * FROM quests_player_currentquests "+
                    "WHERE uuid=?;";

    //Query for getting NPC/POI waypoints from the database.
    private static final String QUERY_GET_WAYPOINTS =
            "SELECT * FROM journey_waypoints;";

    //Query for getting time-ordered player positions during session from the database.
    private static final String QUERY_GET_SESSION_TIMED_POSITIONS =
            "SELECT * FROM whimc_player_positions "+
                    "WHERE uuid=? AND time > ? ORDER BY time ASC;";

    //Query for getting science tool use during session from the database.
    private static final String QUERY_GET_SESSION_TOOLS =
            "SELECT * FROM whimc_sciencetools "+
                    "WHERE uuid=? AND time > ?;";

    //Query for getting observation use during session from the database.
    private static final String QUERY_GET_SESSION_OBSERVATIONS =
            "SELECT * FROM whimc_observations "+
                    "WHERE uuid=? AND time > ?;";

    //Query for getting player positions during session from the database.
    private static final String QUERY_GET_SESSION_POSITIONS =
            "SELECT * FROM whimc_player_positions "+
                    "WHERE uuid=? AND time > ?;";

    /**
     * Query for inserting a progress entry into the database.
     */
    private static final String QUERY_SAVE_PROGRESS =
            "INSERT INTO whimc_progress " +
                    "(uuid, username, time, observation, science_tools, exploration, poi_exploration, quest, score) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /**
     * Query for inserting a progress entry into the database.
     */
    private static final String QUERY_SAVE_PROGRESS_COMMANDS =
            "INSERT INTO whimc_progress_commands " +
                    "(uuid, username, world, time, command) " +
                    "VALUES (?, ?, ?, ?, ?)";


    private final StudentFeedback plugin;
    private final MySQLConnection sqlConnection;

    /**
     * Constructor to instantiate instance variables and connect to SQL
     * @param plugin StudentFeedback plugin instance
     * @param callback callback to signal that process completed
     */
    public Queryer(StudentFeedback plugin, Consumer<Queryer> callback) {
        this.plugin = plugin;
        this.sqlConnection = new MySQLConnection(plugin);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean success = sqlConnection.initialize();
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success ? this : null));
        });
    }

    /**
     * Generated a PreparedStatement for saving a new set of skills for a player.
     * @param connection MySQL Connection
     * @param player     Player to save skills
     * @return PreparedStatement
     * @throws SQLException
     */
    private PreparedStatement insertNewSkills(Connection connection, Player player) throws SQLException {
        List<Double> skills = new ArrayList<>();
        for(int k = 0; k < 4; k++) {
            skills.add(0.0);
        }
        PreparedStatement statement = connection.prepareStatement(QUERY_SAVE_SKILLS);
        statement.setString(1, player.getUniqueId().toString());
        statement.setString(2, player.getName());
        statement.setDouble(3, skills.get(0));
        statement.setDouble(4, skills.get(1));
        statement.setDouble(5, skills.get(2));
        statement.setDouble(6, skills.get(3));
        return statement;
    }

    /**
     * Generated a PreparedStatement for updating a set of skills for a player.
     * @param connection MySQL Connection
     * @param player     Player to update skills
     * @param skills     SKills to give to the player
     * @return PreparedStatement
     * @throws SQLException
     */
    public PreparedStatement updatePlayerSkills(Connection connection, Player player, List<Double> skills) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(QUERY_UPDATE_SKILLS);
        statement.setDouble(1, skills.get(0));
        statement.setDouble(2, skills.get(1));
        statement.setDouble(3, skills.get(2));
        statement.setDouble(4, skills.get(3));
        statement.setString(5, player.getUniqueId().toString());
        return statement;
    }

    /**
     * Updates the player's skill based on correctness of observation
     * @param player     Player to update skills
     * @param type     Skill type
     * @param correct     Assessment of the skill application
     * @param callback    Callback to signify process completion
     * @throws SQLException
     */
    public void updateSkills(Player player, String type, int correct, Consumer callback){
        getSkills(player, previousSkills -> {
            async(() -> {

                List<Double> skills = (List<Double>) previousSkills;

                try (Connection connection = this.sqlConnection.getConnection()) {
                    if (skills.size() == 0) {
                        try (PreparedStatement insertStatement = insertNewSkills(connection, player)) {
                            String query = insertStatement.toString().substring(insertStatement.toString().indexOf(" ") + 1);
                            Utils.debug("  " + query);
                            insertStatement.executeUpdate();
                        }
                    }

                    if( correct != -1) {
                        skills = Skills.updateSkills(skills, type, correct);
                    }
                    try (PreparedStatement statement = updatePlayerSkills(connection, player, skills)) {
                        String query = statement.toString().substring(statement.toString().indexOf(" ") + 1);
                        Utils.debug("  " + query);
                        statement.executeUpdate();
                    }
                    sync(callback,skills);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    /**
     * Method to get skills for a player
     * @param player Player to get the skills for
     * @param callback callback to signify process completion
     */
    public void getSkills(Player player, Consumer callback){
        List<Double> skills = new ArrayList<>();
        async(() -> {
            try (Connection connection = this.sqlConnection.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(QUERY_GET_PLAYER_SKILLS)) {
                    statement.setString(1, player.getUniqueId().toString());
                    ResultSet results = statement.executeQuery();
                    while (results.next()) {
                        skills.add(results.getDouble("analogy"));
                        skills.add(results.getDouble("comparative"));
                        skills.add(results.getDouble("descriptive"));
                        skills.add(results.getDouble("inference"));
                    }

                    if (skills.size() == 0) {
                        try (PreparedStatement insertStatement = insertNewSkills(connection, player)) {
                            String query = insertStatement.toString().substring(insertStatement.toString().indexOf(" ") + 1);
                            Utils.debug("  " + query);
                            insertStatement.executeUpdate();
                        }
                        for(int k = 0; k < 4; k++) {
                            skills.add(0.0);
                        }
                    }
                    sync(callback,skills);
                }
            } catch (SQLException exc) {
                exc.printStackTrace();
            }
        });
    }

    /**
     * Method to get quests started and completed by a player
     * @param player Player to get the quests for
     * @param callback callback to signify process completion
     */
    public void getQuests(Player player, Consumer callback){
        async(() -> {
            ArrayList<String> quests = new ArrayList<>();
            try (Connection connection = this.sqlConnection.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(QUERY_GET_QUESTS)) {
                    statement.setString(1, player.getUniqueId().toString());
                    ResultSet results = statement.executeQuery();
                    while (results.next()) {
                        quests.add(results.getString("questid"));
                    }
                } catch (SQLException exc) {
                    exc.printStackTrace();
                }
                try (PreparedStatement statement = connection.prepareStatement(QUERY_GET_STARTED_QUESTS)) {
                    statement.setString(1, player.getUniqueId().toString());
                    ResultSet results = statement.executeQuery();
                    while (results.next()) {
                        quests.add(results.getString("questid"));
                    }
                } catch (SQLException exc) {
                    exc.printStackTrace();
                }
                sync(callback,quests);
            } catch (SQLException exc) {
                exc.printStackTrace();
            }
        });
    }

    /**
     * Method to get skills for a player
     * @param player Player to get the skills for
     * @param callback callback to signify process completion
     */
    public void getSessionScienceTools(Player player, Long sessionStart, Consumer callback){
        HashMap<String,HashSet<String>> tools = new HashMap<>();
        async(() -> {
            try (Connection connection = this.sqlConnection.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(QUERY_GET_SESSION_TOOLS)) {
                    statement.setString(1, player.getUniqueId().toString());
                    statement.setLong(2, sessionStart);
                    ResultSet results = statement.executeQuery();
                    while (results.next()) {
                        String worldName = results.getString("world");
                        String sciTool = results.getString("tool");
                        if(!tools.containsKey(worldName)){
                            tools.put(worldName,new HashSet<String>());
                        }
                        tools.get(worldName).add(sciTool);
                    }
                    sync(callback,tools);
                }
            } catch (SQLException exc) {
                exc.printStackTrace();
            }
        });
    }

    /**
     * Method to get observation texts made by a player during the session (color codes stripped)
     * @param player Player to get the observations for
     * @param callback callback to signify process completion
     */
    public void getSessionObservations(Player player, Long sessionStart, Consumer callback){
        HashMap<String,ArrayList<String>> observations = new HashMap<>();
        async(() -> {
            try (Connection connection = this.sqlConnection.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(QUERY_GET_SESSION_OBSERVATIONS)) {
                    statement.setString(1, player.getUniqueId().toString());
                    statement.setLong(2, sessionStart);
                    ResultSet results = statement.executeQuery();
                    while (results.next()) {
                        String worldName = results.getString("world");
                        String text = results.getString("observation_color_stripped");
                        if (text == null || text.isEmpty()) {
                            text = results.getString("observation");
                            text = text == null ? "" : ChatColor.stripColor(Utils.color(text));
                        }
                        if(!observations.containsKey(worldName)){
                            observations.put(worldName,new ArrayList<String>());
                        }
                        observations.get(worldName).add(text);
                    }
                    sync(callback,observations);
                }
            } catch (SQLException exc) {
                exc.printStackTrace();
            }
        });
    }

    /**
     * Method to get positions for a player (must divide by 1000 because thats how it is stored in db)
     * @param player Player to get the positions for
     * @param callback callback to signify process completion
     */
    public void getSessionPositions(Player player, Long sessionStart, Consumer callback){
        HashMap<String, ArrayList<Point>> positions = new HashMap<>();
        async(() -> {
            try (Connection connection = this.sqlConnection.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(QUERY_GET_SESSION_POSITIONS)) {
                    statement.setString(1, player.getUniqueId().toString());
                    statement.setLong(2, sessionStart/1000);
                    ResultSet results = statement.executeQuery();
                    while (results.next()) {
                        String worldName = results.getString("world");
                        int x = results.getInt("x");
                        int z = results.getInt("z");
                        Point point = new Point(x,z);
                        if(!positions.containsKey(worldName)){
                            positions.put(worldName,new ArrayList<Point>());
                        }
                        positions.get(worldName).add(point);
                    }
                    sync(callback,positions);
                }
            } catch (SQLException exc) {
                exc.printStackTrace();
            }
        });
    }

    /**
     * Method to get the data needed for the POI exploration assessment: all NPC/POI waypoints
     * and the player's time-ordered positions during the session
     * @param player Player to get the positions for
     * @param sessionStart time when the player joined the server
     * @param callback callback to signify process completion, receives Object[]{waypoints, positions}
     */
    public void getSessionPOIData(Player player, Long sessionStart, Consumer callback){
        HashMap<String, ArrayList<double[]>> waypoints = new HashMap<>();
        HashMap<String, ArrayList<long[]>> positions = new HashMap<>();
        async(() -> {
            try (Connection connection = this.sqlConnection.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(QUERY_GET_WAYPOINTS)) {
                    ResultSet results = statement.executeQuery();
                    while (results.next()) {
                        String worldName = results.getString("world");
                        double x = results.getDouble("x");
                        double z = results.getDouble("z");
                        if(!waypoints.containsKey(worldName)){
                            waypoints.put(worldName,new ArrayList<double[]>());
                        }
                        waypoints.get(worldName).add(new double[]{x, z});
                    }
                } catch (SQLException exc) {
                    // journey_waypoints table may not exist; POI score will be 0
                    exc.printStackTrace();
                }
                try (PreparedStatement statement = connection.prepareStatement(QUERY_GET_SESSION_TIMED_POSITIONS)) {
                    statement.setString(1, player.getUniqueId().toString());
                    statement.setLong(2, sessionStart/1000);
                    ResultSet results = statement.executeQuery();
                    while (results.next()) {
                        String worldName = results.getString("world");
                        long time = results.getLong("time");
                        long x = results.getInt("x");
                        long z = results.getInt("z");
                        if(!positions.containsKey(worldName)){
                            positions.put(worldName,new ArrayList<long[]>());
                        }
                        positions.get(worldName).add(new long[]{time, x, z});
                    }
                }
                sync(callback, new Object[]{waypoints, positions});
            } catch (SQLException exc) {
                exc.printStackTrace();
            }
        });
    }

    /**
     * Method to build the full OverallAssessment for a player's session by chaining all queries
     * @param player Player to assess
     * @param sessionStart time when the player joined the server
     * @param callback callback receiving the OverallAssessment
     */
    public void getOverallAssessment(Player player, Long sessionStart, Consumer<OverallAssessment> callback){
        getSessionObservations(player, sessionStart, observations -> {
            ObservationAssessment obs = new ObservationAssessment(player, sessionStart, observations, plugin);
            getSessionScienceTools(player, sessionStart, scienceTools -> {
                ScienceToolsAssessment sci = new ScienceToolsAssessment(player, sessionStart, scienceTools, plugin);
                getSessionPositions(player, sessionStart, positions -> {
                    ExplorationAssessment exp = new ExplorationAssessment(player, sessionStart, positions, plugin);
                    getSessionPOIData(player, sessionStart, poiData -> {
                        POIExplorationAssessment poi = new POIExplorationAssessment(player, sessionStart, poiData, plugin);
                        getQuests(player, quests -> {
                            QuestAssessment quest = new QuestAssessment(player, sessionStart, quests, plugin);
                            callback.accept(new OverallAssessment(player, sessionStart, null, obs, sci, exp, poi, quest, plugin));
                        });
                    });
                });
            });
        });
    }

    /**
     * Generated a PreparedStatement for saving a new progress session.
     *
     * @param connection MySQL Connection
     * @param assessment       Assessment to save
     * @return PreparedStatement
     * @throws SQLException
     */
    private PreparedStatement getStatement(Connection connection, OverallAssessment assessment) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(QUERY_SAVE_PROGRESS, Statement.RETURN_GENERATED_KEYS);


        statement.setString(1, assessment.getPlayer().getUniqueId().toString());
        statement.setString(2, assessment.getPlayer().getName());
        statement.setLong(3, assessment.getSessionStart());
        statement.setDouble(4, assessment.getObservationAssessment().metric());
        statement.setDouble(5, assessment.getScienceToolAssessment().metric());
        statement.setDouble(6, assessment.getExplorationAssessment().metric());
        statement.setDouble(7, assessment.getPOIExplorationAssessment().metric());
        statement.setDouble(8, assessment.getQuestAssessment().metric());
        statement.setDouble(9, assessment.metric());
        return statement;
    }

    /**
     * Stores a progress into the database and returns the ID
     *
     * @param assessment Assessment to save
     * @param callback    Function to call once the observation has been saved
     */
    public void storeNewProgress(OverallAssessment assessment, Consumer<Integer> callback) {
        async(() -> {
            Utils.debug("Storing progress to database:");

            try (Connection connection = this.sqlConnection.getConnection()) {
                try (PreparedStatement statement = getStatement(connection, assessment)) {
                    String query = statement.toString().substring(statement.toString().indexOf(" ") + 1);
                    Utils.debug("  " + query);
                    statement.executeUpdate();

                    try (ResultSet idRes = statement.getGeneratedKeys()) {
                        idRes.next();
                        int id = idRes.getInt(1);

                        Utils.debug("Progress saved with id " + id + ".");
                        sync(callback, id);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * Generated a PreparedStatement for saving a new progress session.
     * @param connection MySQL Connection
     * @param player Checking progress or leaderboard to save
     * @param command Command progress or leaderboard to save
     * @return PreparedStatement
     * @throws SQLException
     */
    private PreparedStatement insertProgressCommand(Connection connection, Player player, String command) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(QUERY_SAVE_PROGRESS_COMMANDS, Statement.RETURN_GENERATED_KEYS);
        statement.setString(1, player.getUniqueId().toString());
        statement.setString(2, player.getName());
        statement.setString(3, player.getWorld().getName());
        statement.setLong(4, System.currentTimeMillis());
        statement.setString(5, command);
        return statement;
    }

    /**
     * Stores a progress command into the database and returns the obervation's ID
     * @param player Checking progress or leaderboard to save
     * @param command Command progress or leaderboard to save
     * @param callback    Function to call once the observation has been saved
     */
    public void storeNewProgressCommand(Player player, String command, Consumer<Integer> callback) {
        async(() -> {
            Utils.debug("Storing command to database:");

            try (Connection connection = this.sqlConnection.getConnection()) {
                try (PreparedStatement statement = insertProgressCommand(connection, player, command)) {
                    String query = statement.toString().substring(statement.toString().indexOf(" ") + 1);
                    Utils.debug("  " + query);
                    statement.executeUpdate();

                    try (ResultSet idRes = statement.getGeneratedKeys()) {
                        idRes.next();
                        int id = idRes.getInt(1);

                        Utils.debug("Command saved with id " + id + ".");
                        sync(callback, id);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
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


}
