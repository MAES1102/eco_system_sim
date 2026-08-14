package com.ecosystem.simulation.rules;

import com.ecosystem.simulation.entities.Entity;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Engine for loading and applying user-defined rules from configuration files.
 * 
 * This class demonstrates the OOP principle of ENCAPSULATION by hiding
 * the complexity of rule parsing and execution behind a simple interface.
 * 
 * Key OOP Principles Demonstrated:
 * - Encapsulation: Rule loading and execution logic hidden behind simple methods
 * - No Inheritance: Standalone class, no need for inheritance hierarchy
 * - Simple Design: Plain text file parsing, no advanced frameworks
 * - Extensibility: Players add rules by editing text files, not Java code
 * - Exception Handling: Uses custom exceptions for meaningful error reporting
 * 
 * @author Ecosystem Simulation Team
 * @version 1.0
 */
public class RuleEngine {
    
    /**
     * List of loaded rules.
     */
    private List<Rule> rules;
    
    /**
     * Interface for receiving rule execution events.
     */
    public interface RuleExecutionListener {
        void onRuleExecuted(String ruleDescription, String entityType);
    }
    
    /**
     * Listener for rule execution events.
     */
    private RuleExecutionListener ruleExecutionListener;
    
    /**
     * Constructor for RuleEngine.
     * Initializes empty rule list.
     */
    public RuleEngine() {
        this.rules = new ArrayList<>();
    }
    
    /**
     * Sets a listener to receive rule execution events.
     * 
     * @param listener The listener to set
     */
    public void setRuleExecutionListener(RuleExecutionListener listener) {
        this.ruleExecutionListener = listener;
    }
    
    /**
     * Loads rules from a text file.
     * File format: "RuleName | TargetType | Condition | Action"
     * Lines starting with # are treated as comments and ignored.
     * 
     * Example file content:
     * # This is a comment
     * StarvingPredator | Predator | energy < 10 | die
     * OldHerbivore | Herbivore | age > 50 | flee
     * FastPlantGrowth | Plant | energy > 30 | grow
     * 
     * @param filename Path to the rule configuration file
     * @throws IOException If file cannot be read
     * @throws RuleParseException If rule format is invalid
     */
    public void loadRules(String filename) throws IOException, RuleParseException {
        this.rules.clear();  // Clear existing rules
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Skip empty lines and comments
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Parse rule
                Rule rule = parseRule(line, lineNumber);
                this.rules.add(rule);
            }
        }
    }
    
    /**
     * Parses a single rule from a string.
     * Format: "RuleName | TargetType | Condition | Action"
     * 
     * @param line The rule string
     * @param lineNumber Line number for error reporting
     * @return Parsed Rule object
     * @throws RuleParseException If rule format is invalid
     */
    private Rule parseRule(String line, int lineNumber) throws RuleParseException {
        // Split by pipe character
        String[] parts = line.split("\\|");
        
        if (parts.length != 4) {
            throw new RuleParseException(
                "Invalid rule format: expected 'Name | Target | Condition | Action', got " + 
                parts.length + " parts", lineNumber);
        }
        
        String name = parts[0].trim();
        String targetType = parts[1].trim();
        String condition = parts[2].trim();
        String action = parts[3].trim();
        
        // Validate that no part is empty
        if (name.isEmpty()) {
            throw new RuleParseException("Rule name cannot be empty", lineNumber);
        }
        if (targetType.isEmpty()) {
            throw new RuleParseException("Target type cannot be empty", lineNumber);
        }
        if (condition.isEmpty()) {
            throw new RuleParseException("Condition cannot be empty", lineNumber);
        }
        if (action.isEmpty()) {
            throw new RuleParseException("Action cannot be empty", lineNumber);
        }

        Rule.validateConditionSyntax(condition, lineNumber);
        Rule.validateActionSyntax(action, lineNumber);

        return new Rule(name, targetType, condition, action);
    }
    
    /**
     * Evaluates all applicable rules against an entity.
     * Rules are evaluated in the order they were loaded.
     * If a rule's condition is met, its action is executed.
     * 
     * @param entity The entity to evaluate rules against
     */
    public void evaluate(Entity entity) {
        if (entity == null) {
            return;
        }
        
        for (Rule rule : this.rules) {
            // Check if rule applies to this entity type
            if (rule.matches(entity)) {
                // Check if condition is met
                if (rule.evaluateCondition(entity)) {
                    // Execute the action
                    rule.executeAction(entity);
                    
                    // Notify listener that rule was executed
                    // Only log important events, not repetitive ones like plant growth
                    if (ruleExecutionListener != null) {
                        String entityType = entity.getClass().getSimpleName();
                        String action = rule.getAction().toLowerCase();
                        
                        // Filter out repetitive events
                        boolean isImportantEvent = 
                            action.contains("die") || 
                            action.contains("reproduce") ||
                            entityType.equals("Predator") && action.contains("hunt");
                        
                        if (isImportantEvent) {
                            String ruleDescription = rule.getTargetType() + " " + rule.getCondition() + " → " + rule.getAction();
                            ruleExecutionListener.onRuleExecuted(ruleDescription, entityType);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Gets the list of loaded rules.
     * 
     * @return Copy of the rules list
     */
    public List<Rule> getRules() {
        return new ArrayList<>(this.rules);
    }
    
    /**
     * Gets the number of loaded rules.
     * 
     * @return Number of rules
     */
    public int getRuleCount() {
        return this.rules.size();
    }
    
    /**
     * Clears all loaded rules.
     */
    public void clearRules() {
        this.rules.clear();
    }
    
    /**
     * Adds a rule programmatically.
     * Useful for testing or dynamic rule creation.
     * 
     * @param rule The rule to add
     */
    public void addRule(Rule rule) {
        if (rule != null) {
            this.rules.add(rule);
        }
    }
}