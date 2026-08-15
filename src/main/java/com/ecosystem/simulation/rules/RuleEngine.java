package com.ecosystem.simulation.rules;

import com.ecosystem.simulation.entities.Entity;
import com.ecosystem.simulation.events.SchedulingContext;
import com.ecosystem.simulation.simulation.World;
import com.ecosystem.simulation.statistics.Statistics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads, holds, and applies the active set of user-authored rules.
 *
 * <p>All condition/action semantics live in {@link Rule} (via {@link RuleVocabulary}
 * and {@link Evaluator}) — this class is intentionally a thin orchestrator:
 * parse each file line into a {@link Rule}, then evaluate matching rules against
 * an entity in file order.</p>
 *
 * <p><b>Atomicity</b>: {@link #loadRules(String)} parses the entire file into a
 * staging list first and only replaces the active rule list on complete success —
 * a malformed line anywhere in the file leaves the previously loaded rules
 * untouched, never partially cleared.</p>
 */
public class RuleEngine {

    private List<Rule> rules;
    private final RuleVocabulary vocabulary;

    public interface RuleExecutionListener {
        void onRuleExecuted(String ruleDescription, String entityType);
    }

    private RuleExecutionListener ruleExecutionListener;

    public RuleEngine(RuleVocabulary vocabulary) {
        this.vocabulary = vocabulary;
        this.rules = new ArrayList<>();
    }

    public void setRuleExecutionListener(RuleExecutionListener listener) {
        this.ruleExecutionListener = listener;
    }

    /**
     * Loads rules from a text file. Format: {@code "RuleName | TargetType | Condition | Action"}.
     * Lines starting with {@code #} and blank lines are ignored.
     *
     * @throws IOException         if the file cannot be read
     * @throws RuleParseException  if any rule's syntax or semantics are invalid; in that case
     *                             the previously active rule set is left completely untouched
     */
    public void loadRules(String filename) throws IOException, RuleParseException {
        List<Rule> staged = parseFile(filename);
        this.rules = staged;
    }

    /**
     * Parses in-memory rule-file text (e.g. from a GUI text editor) into a list
     * without mutating this engine's active rules or touching any file.
     */
    public List<Rule> parseText(String text) throws RuleParseException {
        List<Rule> staged = new ArrayList<>();
        String[] lines = (text == null ? "" : text).split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            staged.add(parseRule(line, i + 1));
        }
        return staged;
    }

    /** Parses every rule in a file into a list without mutating this engine's active rules. */
    public List<Rule> parseFile(String filename) throws IOException, RuleParseException {
        List<Rule> staged = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                staged.add(parseRule(line, lineNumber));
            }
        }
        return staged;
    }

    private Rule parseRule(String line, int lineNumber) throws RuleParseException {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 4) {
            throw new RuleParseException(
                    "Invalid rule format: expected 'Name | Target | Condition | Action', got "
                            + parts.length + " parts", lineNumber);
        }

        String name = parts[0].trim();
        String targetType = parts[1].trim();
        String condition = parts[2].trim();
        String action = parts[3].trim();

        if (name.isEmpty()) {
            throw new RuleParseException("Rule name cannot be empty", lineNumber);
        }
        if (targetType.isEmpty()) {
            throw new RuleParseException("Target type cannot be empty", lineNumber);
        }

        return new Rule(name, targetType, condition, action, vocabulary, lineNumber);
    }

    /**
     * Evaluates all applicable rules against an entity, in file/definition order, executing
     * each matching rule's actions left to right. Level-triggered: a rule fires again on every
     * activity event in which its condition still holds.
     */
    public void evaluate(Entity entity, World world, Statistics statistics, SchedulingContext scheduling) {
        if (entity == null) {
            return;
        }
        EvaluationContext ctx = new EvaluationContext(entity, world, statistics, scheduling);
        for (Rule rule : this.rules) {
            if (rule.matches(entity) && rule.evaluateCondition(ctx)) {
                rule.executeActions(ctx);
                notifyListener(rule, entity);
            }
        }
    }

    /** Convenience overload used by simple/unit-test callers that don't have a running engine. */
    public void evaluate(Entity entity) {
        if (entity == null) {
            return;
        }
        evaluate(entity, entity.getWorld(), entity.getStatistics(), entity.getSchedulingContext());
    }

    private void notifyListener(Rule rule, Entity entity) {
        if (ruleExecutionListener == null) {
            return;
        }
        String entityType = entity.getClass().getSimpleName();
        String action = rule.getAction().toLowerCase();
        boolean isImportantEvent = action.contains("die") || action.contains("reproduce");
        if (isImportantEvent) {
            String ruleDescription = rule.getTargetType() + " " + rule.getCondition() + " -> " + rule.getAction();
            ruleExecutionListener.onRuleExecuted(ruleDescription, entityType);
        }
    }

    public List<Rule> getRules() {
        return new ArrayList<>(this.rules);
    }

    public int getRuleCount() {
        return this.rules.size();
    }

    public void clearRules() {
        this.rules.clear();
    }

    public void addRule(Rule rule) {
        if (rule != null) {
            this.rules.add(rule);
        }
    }

    /** Removes the (first) rule with the given name, if present. Used by the GUI's rule list. */
    public boolean removeRule(String name) {
        return this.rules.removeIf(r -> r.getName().equals(name));
    }

    public RuleVocabulary getVocabulary() {
        return vocabulary;
    }
}
