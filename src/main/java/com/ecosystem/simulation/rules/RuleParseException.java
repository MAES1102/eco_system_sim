package com.ecosystem.simulation.rules;

/**
 * Exception thrown when a rule cannot be parsed or is invalid.
 * 
 * This custom exception provides specific error information about
 * rule parsing failures, improving error handling in the rule engine.
 * 
 * Key OOP Principles Demonstrated:
 * - Exception Handling: Domain-specific exception for rule errors
 * - Encapsulation: Wraps error details specific to rule parsing
 * 
 * @author Ecosystem Simulation Team
 * @version 1.0
 */
public class RuleParseException extends Exception {
    
    /**
     * Line number where the error occurred.
     */
    private int lineNumber;
    
    /**
     * Constructor for RuleParseException with message.
     * 
     * @param message Error message
     */
    public RuleParseException(String message) {
        super(message);
        this.lineNumber = -1;
    }
    
    /**
     * Constructor for RuleParseException with message and line number.
     * 
     * @param message Error message
     * @param lineNumber Line number where error occurred
     */
    public RuleParseException(String message, int lineNumber) {
        super(message + " (line " + lineNumber + ")");
        this.lineNumber = lineNumber;
    }
    
    /**
     * Constructor for RuleParseException with message and cause.
     * 
     * @param message Error message
     * @param cause Underlying cause
     */
    public RuleParseException(String message, Throwable cause) {
        super(message, cause);
        this.lineNumber = -1;
    }
    
    /**
     * Gets the line number where the error occurred.
     * 
     * @return Line number, or -1 if not applicable
     */
    public int getLineNumber() {
        return this.lineNumber;
    }
}