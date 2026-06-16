package edu.whimc.feedback.utils;

import edu.whimc.feedback.StudentFeedback;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Scores observation text for scientific quality using lightweight heuristics
 * (no external LLM API required).
 */
public final class ObservationQualityScorer {

    private static final Pattern DECIMAL_NUMBER = Pattern.compile("\\d+\\.\\d+");
    private static final Pattern WHOLE_NUMBER = Pattern.compile("\\b\\d+\\b");
    private static final Pattern PERCENT = Pattern.compile("\\d+\\s*%|\\bpercent\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER_NEAR_UNIT = Pattern.compile(
            "\\d+(?:\\.\\d+)?\\s*[^\\s.,;!?]{0,12}", Pattern.CASE_INSENSITIVE);
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("[.!?]+");

    private final StudentFeedback plugin;
    private List<String> measurementUnits = Collections.emptyList();
    private List<String> scienceToolNames = Collections.emptyList();
    private long vocabularyLoadedAt;

    public ObservationQualityScorer(StudentFeedback plugin) {
        this.plugin = plugin;
    }

    /**
     * Sums quality points across all observation texts in the session.
     * @param observations world -> observation texts
     * @return total quality points
     */
    public double scoreSession(HashMap<String, ArrayList<String>> observations) {
        reloadVocabularyIfNeeded();
        double total = 0;
        Set<String> seenNormalized = new HashSet<>();

        for (ArrayList<String> texts : observations.values()) {
            for (String text : texts) {
                total += scoreObservation(text, seenNormalized);
            }
        }
        total += scoreMultiWorldBonus(observations);
        return total;
    }

    private double scoreMultiWorldBonus(HashMap<String, ArrayList<String>> observations) {
        int worldsWithObservations = 0;
        for (ArrayList<String> texts : observations.values()) {
            if (texts != null && !texts.isEmpty()) {
                worldsWithObservations++;
            }
        }
        int minWorlds = plugin.getConfig().getInt("observation-quality.multi-world-min-worlds", 2);
        if (worldsWithObservations >= minWorlds) {
            return plugin.getConfig().getDouble("observation-quality.multi-world-bonus", 2);
        }
        return 0;
    }

    /**
     * Scores a single observation for scientific quality signals.
     * @param observation observation text
     * @param seenNormalized normalized texts already scored this session (for duplicate detection)
     * @return quality points for this observation
     */
    private double scoreObservation(String observation, Set<String> seenNormalized) {
        if (observation == null) {
            return 0;
        }
        String text = observation.trim();
        if (text.isEmpty()) {
            return 0;
        }
        String lower = text.toLowerCase(Locale.ROOT);

        double points = 0;
        if (text.contains("?")) {
            points += plugin.getConfig().getDouble("observation-quality.question-mark-bonus", 2);
        }
        if (DECIMAL_NUMBER.matcher(text).find()) {
            points += plugin.getConfig().getDouble("observation-quality.decimal-number-bonus", 2);
        }
        if (containsMeasurementUnit(lower)) {
            points += plugin.getConfig().getDouble("observation-quality.measurement-unit-bonus", 2);
        }
        if (PERCENT.matcher(text).find()) {
            points += plugin.getConfig().getDouble("observation-quality.percent-bonus", 1);
        }
        if (countTermMatches(lower, plugin.getConfig().getStringList("observation-quality.comparison-words")) > 0) {
            points += plugin.getConfig().getDouble("observation-quality.comparison-word-bonus", 1);
        }
        if (countTermMatches(lower, plugin.getConfig().getStringList("observation-quality.causal-words")) > 0) {
            points += plugin.getConfig().getDouble("observation-quality.causal-word-bonus", 1);
        }
        if (countTermMatches(lower, plugin.getConfig().getStringList("observation-quality.scientific-terms")) > 0) {
            points += plugin.getConfig().getDouble("observation-quality.scientific-term-bonus", 1);
        }
        if (countDistinctNumbers(text) >= 2) {
            points += plugin.getConfig().getDouble("observation-quality.multiple-numbers-bonus", 1);
        }
        if (text.length() >= plugin.getConfig().getInt("observation-quality.min-length-for-detail-bonus", 40)) {
            points += plugin.getConfig().getDouble("observation-quality.detail-length-bonus", 1);
        }
        if (mentionsScienceTool(lower)) {
            points += plugin.getConfig().getDouble("observation-quality.science-tool-name-bonus", 2);
        }
        if (hasNumberUnitInSameSentence(text, lower)) {
            points += plugin.getConfig().getDouble("observation-quality.number-unit-sentence-bonus", 2);
        }
        if (crossReferencesMeasurements(lower, text)) {
            points += plugin.getConfig().getDouble("observation-quality.cross-measurement-bonus", 2);
        }

        int minWords = plugin.getConfig().getInt("observation-quality.min-words-before-penalty", 3);
        int minLength = plugin.getConfig().getInt("observation-quality.min-length-before-penalty", 15);
        int wordCount = text.split("\\s+").length;
        if (text.length() < minLength || wordCount < minWords) {
            points += plugin.getConfig().getDouble("observation-quality.short-observation-penalty", -2);
        }

        String normalized = normalizeForDuplicateCheck(text);
        if (seenNormalized.contains(normalized)) {
            points += plugin.getConfig().getDouble("observation-quality.duplicate-observation-penalty", -2);
        } else {
            seenNormalized.add(normalized);
        }

        return points;
    }

    private boolean mentionsScienceTool(String lowerText) {
        for (String toolName : scienceToolNames) {
            if (toolName.isEmpty()) {
                continue;
            }
            String normalized = toolName.toLowerCase(Locale.ROOT).trim();
            if (normalized.length() <= 3) {
                if (lowerText.matches(".*\\b" + Pattern.quote(normalized) + "\\b.*")) {
                    return true;
                }
            } else if (lowerText.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNumberUnitInSameSentence(String text, String lowerText) {
        String[] sentences = SENTENCE_SPLIT.split(text);
        if (sentences.length == 0 || (sentences.length == 1 && sentences[0].isBlank())) {
            sentences = new String[]{text};
        }
        for (String sentence : sentences) {
            String sentenceLower = sentence.toLowerCase(Locale.ROOT).trim();
            if (sentenceLower.isEmpty()) {
                continue;
            }
            boolean hasNumber = WHOLE_NUMBER.matcher(sentence).find() || DECIMAL_NUMBER.matcher(sentence).find();
            if (!hasNumber) {
                continue;
            }
            for (String unit : measurementUnits) {
                if (unit.isEmpty()) {
                    continue;
                }
                String normalizedUnit = unit.toLowerCase(Locale.ROOT).trim();
                if (!sentenceLower.contains(normalizedUnit)) {
                    continue;
                }
                if (numberAdjacentToUnit(sentence, normalizedUnit)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean numberAdjacentToUnit(String sentence, String unit) {
        String lower = sentence.toLowerCase(Locale.ROOT);
        int unitIndex = lower.indexOf(unit);
        while (unitIndex >= 0) {
            int searchStart = Math.max(0, unitIndex - 20);
            String window = sentence.substring(searchStart, unitIndex);
            if (WHOLE_NUMBER.matcher(window).find() || DECIMAL_NUMBER.matcher(window).find()) {
                return true;
            }
            int afterEnd = Math.min(sentence.length(), unitIndex + unit.length() + 20);
            String after = sentence.substring(unitIndex + unit.length(), afterEnd);
            if (WHOLE_NUMBER.matcher(after).find() || DECIMAL_NUMBER.matcher(after).find()) {
                return true;
            }
            unitIndex = lower.indexOf(unit, unitIndex + 1);
        }
        return NUMBER_NEAR_UNIT.matcher(sentence).find() && lower.contains(unit);
    }

    private boolean crossReferencesMeasurements(String lowerText, String text) {
        int conceptMatches = countTermMatches(lowerText,
                plugin.getConfig().getStringList("observation-quality.measurement-concepts"));
        if (conceptMatches < 2) {
            return false;
        }
        if (countDistinctNumbers(text) < 2) {
            return false;
        }
        boolean hasConnector = countTermMatches(lowerText,
                plugin.getConfig().getStringList("observation-quality.cross-reference-words")) > 0;
        boolean hasMultipleUnits = countUnitMentions(lowerText) >= 2;
        return hasConnector || hasMultipleUnits;
    }

    private int countUnitMentions(String lowerText) {
        int count = 0;
        for (String unit : measurementUnits) {
            if (unit.isEmpty()) {
                continue;
            }
            String normalizedUnit = unit.toLowerCase(Locale.ROOT).trim();
            if (lowerText.contains(normalizedUnit)) {
                count++;
            }
        }
        return count;
    }

    private String normalizeForDuplicateCheck(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean containsMeasurementUnit(String lowerText) {
        for (String unit : measurementUnits) {
            if (unit.isEmpty()) {
                continue;
            }
            String normalizedUnit = unit.toLowerCase(Locale.ROOT).trim();
            if (normalizedUnit.length() <= 2 && !normalizedUnit.startsWith("°")) {
                if (lowerText.matches(".*\\b" + Pattern.quote(normalizedUnit) + "\\b.*")) {
                    return true;
                }
            } else if (lowerText.contains(normalizedUnit)) {
                return true;
            }
        }
        return false;
    }

    private int countTermMatches(String lowerText, List<String> terms) {
        int matches = 0;
        for (String term : terms) {
            if (term == null || term.isBlank()) {
                continue;
            }
            if (lowerText.contains(term.toLowerCase(Locale.ROOT).trim())) {
                matches++;
            }
        }
        return matches;
    }

    private int countDistinctNumbers(String text) {
        Set<String> numbers = new HashSet<>();
        var matcher = WHOLE_NUMBER.matcher(text);
        while (matcher.find()) {
            numbers.add(matcher.group());
        }
        matcher = DECIMAL_NUMBER.matcher(text);
        while (matcher.find()) {
            numbers.add(matcher.group());
        }
        return numbers.size();
    }

    private void reloadVocabularyIfNeeded() {
        long refreshMs = plugin.getConfig().getLong("observation-quality.unit-cache-refresh-ms", 300_000L);
        if (!measurementUnits.isEmpty() && System.currentTimeMillis() - vocabularyLoadedAt < refreshMs) {
            return;
        }
        Vocabulary vocabulary = loadVocabulary();
        measurementUnits = vocabulary.units;
        scienceToolNames = vocabulary.toolNames;
        vocabularyLoadedAt = System.currentTimeMillis();
    }

    private Vocabulary loadVocabulary() {
        Set<String> units = new HashSet<>(plugin.getConfig().getStringList("observation-quality.extra-units"));
        Set<String> toolNames = new HashSet<>();

        File scienceToolsConfig = new File(plugin.getDataFolder().getParentFile(),
                "WHIMC-ScienceTools/config.yml");
        if (scienceToolsConfig.isFile()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(scienceToolsConfig);
            ConfigurationSection conversions = yaml.getConfigurationSection("conversions");
            if (conversions != null) {
                for (String key : conversions.getKeys(false)) {
                    addTerm(units, conversions.getString(key + ".unit"));
                }
            }
            ConfigurationSection tools = yaml.getConfigurationSection("tools");
            if (tools != null) {
                for (String key : tools.getKeys(false)) {
                    addTerm(units, tools.getString(key + ".unit"));
                    addTerm(toolNames, key.replace('_', ' '));
                    addTerm(toolNames, key);
                    addTerm(toolNames, tools.getString(key + ".display-name"));
                    for (String alias : tools.getStringList(key + ".aliases")) {
                        addTerm(toolNames, alias);
                        addTerm(units, alias);
                    }
                }
            }
        }

        List<String> sortedUnits = new ArrayList<>(units);
        sortedUnits.sort((a, b) -> Integer.compare(b.length(), a.length()));
        List<String> sortedToolNames = new ArrayList<>(toolNames);
        sortedToolNames.sort((a, b) -> Integer.compare(b.length(), a.length()));
        return new Vocabulary(sortedUnits, sortedToolNames);
    }

    private static void addTerm(Set<String> terms, String raw) {
        if (raw == null) {
            return;
        }
        String cleaned = raw.trim();
        if (!cleaned.isEmpty()) {
            terms.add(cleaned);
        }
    }

    private static final class Vocabulary {
        private final List<String> units;
        private final List<String> toolNames;

        private Vocabulary(List<String> units, List<String> toolNames) {
            this.units = units;
            this.toolNames = toolNames;
        }
    }
}
