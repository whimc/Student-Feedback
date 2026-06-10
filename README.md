# WHIMC-StudentFeedback

Plugin to assess student in-game behaviors and provide feedback

## Download
Grab the latest built `.jar` from the [latest release](https://github.com/whimc/Student-Feedback/releases/latest). A new release is published automatically on every push to `main`, and the version number is bumped after each release.

## How Scoring Works

This plugin does not track gameplay itself (aside from logging `/progress` and `/leaderboard` usage). Instead, it queries tables populated by other WHIMC plugins and scores a player's **current session** — i.e. rows with `time` greater than when the player joined the server.

### Data Sources (tables read)

| Table | Populated by | Columns used | Session-filtered? |
|---|---|---|---|
| `whimc_observations` | WHIMC-Observations | `uuid`, `time`, `world`, `observation`, `observation_color_stripped` | Yes |
| `whimc_sciencetools` | WHIMC-ScienceTools | `uuid`, `time`, `world`, `tool` | Yes |
| `whimc_player_positions` | WHIMC-PositionTracker | `uuid`, `time`, `world`, `x`, `z` | Yes |
| `quests_player_completedquests` | Quests plugin | `uuid`, `questid` | No (all-time) |
| `quests_player_currentquests` | Quests plugin | `uuid`, `questid` | No (all-time) |
| `journey_waypoints` | Journey/waypoints plugin | `world`, `x`, `z` | n/a (POI locations) |
| `whimc_skills` | This plugin | `analogy`, `comparative`, `descriptive`, `inference` | n/a (persistent) |

### Interest Measures

These five measures are computed on demand (when `/progress` or `/leaderboard` is run, and on logout when the session is saved). Each category's raw value is **normalized to a 0–100 score** using the `score-maximums` section of the config (values at or above the configured maximum earn the full 100), so every category carries equal weight in the Overall Score regardless of its natural scale.

| Measure | Raw value | Details |
|---|---|---|
| **Observation Assessment** | Number of observations *and* total word count this session | Two equally weighted halves: the observation count (normalized against `score-maximums.observation-count`) and the total words written across all observations (normalized against `score-maximums.observation-words`). Color codes are stripped before counting words. |
| **Science Tool Assessment** | Number of *unique* science tool types used per world this session | Diversity, not volume: tools are deduplicated per world (e.g. using `/temperature` ten times in one world counts once; using it in two different worlds counts twice). Counts are summed across worlds, then normalized against `score-maximums.science-tools`. |
| **Exploration Assessment** | Number of distinct map regions visited this session | Each world configured under `worlds:` in the config has its bundled map image (`resources/maps/<world>.png`) divided into a 10×10 grid. The player's logged positions are mapped onto that grid using the world's `pixel_to_block_ratio` and top-left coordinates, and each visited cell counts as 1 (summed across worlds, normalized against `score-maximums.exploration`). Positions outside the map bounds, or in worlds without config/map entries, are ignored. |
| **POI Exploration Assessment** | Number of times the player dwelled near an NPC/POI this session | POI locations come from the `journey_waypoints` table. Using the session's time-ordered position log, each continuous stretch of at least 2 seconds spent within `poi-radius` blocks (default 5) of a waypoint counts as one dwell event; leaving and returning counts again. Normalized against `score-maximums.poi-exploration`. |
| **Quest Assessment** | Number of quests started plus quests completed | Started quests come from `quests_player_currentquests` and completed quests from `quests_player_completedquests`. Normalized against `score-maximums.quests`. Note: both are queried by player UUID only, so this reflects all-time activity rather than the current session. |
| **Overall Score** | Average of the five normalized scores above | A 0–100 score: `(observation + science tools + exploration + POI exploration + quests) / 5`. Used for `/progress` feedback (which also nudges the student toward their lowest measure), to rank players in `/leaderboard`, and saved to `whimc_progress` when a player logs out. |

### Observation Structure Assessment (skills)

Independent of the interest measures above, whenever a player makes an observation in one of the configured `agent-worlds`, the plugin:

1. Cleans the observation text (strips colors/punctuation, lowercases) and classifies it with a bundled ML model (`model.pmml`) into one of: `analogy`, `comparative`, `descriptive`, `inference`, `factual`, `off topic`, `measurement mistake`.
2. Marks the observation "correct" if the predicted type matches the type the student self-selected from the observation template (unlabeled observations are not scored either way).
3. Updates the student's per-skill mastery estimate in `whimc_skills` using Bayesian Knowledge Tracing (BKT) with per-skill guess/slip/transfer parameters, then displays the updated open learner model (progress bars for Comparative, Descriptive, and Inference) plus written feedback.

### Tables Created by This Plugin

| Table | Purpose |
|---|---|
| `whimc_skills` | Per-player BKT mastery estimates (0–1) for analogy, comparative, descriptive, and inference observation skills |
| `whimc_progress` | Snapshot of a session's interest measures (observation, science tools, exploration, POI exploration, quest, overall score), written automatically when a player logs out |
| `whimc_progress_commands` | Log of each `/progress` and `/leaderboard` invocation (player, world, time) |
| `whimc_dialogue` | Log of agent dialogue interactions and the metrics shown during them |

## Building
Requires JDK 21+ (the Java version required by Spigot/Minecraft 1.21.x servers). Compile an uberjar from the command line by doing a "Build" via Maven:
```
$ mvn clean package
```
It should show up in the target directory use .

## Config
### MySQL
| Key | Type | Description |
|---|---|---|
|`mysql.host`|`string`|The host of the database|
|`mysql.port`|`integer`|The port of the database|
|`mysql.database`|`string`|The name of the database to use|
|`mysql.username`|`string`|Username for credentials|
|`mysql.password`|`string`|Password for credentials|

#### Example
```yaml
mysql:
  host: localhost
  port: 3306
  database: minecraft
  username: user
  password: pass
```

### Scoring
| Key | Type | Description |
|---|---|---|
|`poi-radius`|`number`|Distance in blocks a player must be within of a `journey_waypoints` POI/NPC to count as "near" it (default 5)|
|`score-maximums.observation-count`|`number`|Observations in a session worth a full 100 points|
|`score-maximums.observation-words`|`number`|Total observation words in a session worth a full 100 points|
|`score-maximums.science-tools`|`number`|Unique science tools (per world) worth a full 100 points|
|`score-maximums.exploration`|`number`|Visited map grid cells worth a full 100 points|
|`score-maximums.poi-exploration`|`number`|POI dwell events worth a full 100 points|
|`score-maximums.quests`|`number`|Quests started + completed worth a full 100 points|

## Commands
| Command                                                     | Description                                                                                                                |
|-------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `/progress`                                                 | Display student progress on interest measures during session                                                               |
| `/leaderboard`                                              | Display leaderboard of averages of student interest measures during session                                                |
| `/agentdialogue`                                            | Display dialogue prompts for agent (should be called by right clicking on agent and not directly invoked)                  |
| `/structureassessment <observation> <type>`                 | Display progress bars for observation structure skills (should be called from Observations and not directly invoked)       |

