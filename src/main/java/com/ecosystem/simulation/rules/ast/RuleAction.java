package com.ecosystem.simulation.rules.ast;

/**
 * Immutable AST for one action in a rule's (possibly multi-action) THEN-clause.
 *
 * <p>Two shapes only: a generic attribute mutation ({@code speed *= 1.1}) or a
 * named domain command ({@code reproduce}). Every writable attribute a
 * {@code Mutation} can target, and every command a {@code Command} can name,
 * is resolved through {@link com.ecosystem.simulation.rules.RuleVocabulary} —
 * this class never hardcodes a field or command name.</p>
 */
public sealed interface RuleAction {

    /** A generic attribute mutation, e.g. {@code energy -= 5} or {@code speed *= 1.1}. */
    record Mutation(String attribute, char op, NumericNode value) implements RuleAction {}

    /** A named domain command, e.g. {@code die}, {@code reproduce}, {@code flee}. */
    record Command(String name) implements RuleAction {}
}
