package com.ecosystem.simulation.rules.ast;

/**
 * Immutable AST for a rule's boolean condition — supports {@code AND}/{@code OR}/
 * {@code NOT}, parentheses (implicit in the tree shape), and numeric comparisons.
 *
 * <p>Sealed and closed for the same reason as {@link NumericNode}: the rule
 * grammar is intentionally not Turing-complete, so its condition shapes are
 * fixed and exhaustively enumerable.</p>
 */
public sealed interface ConditionNode {

    record And(ConditionNode left, ConditionNode right) implements ConditionNode {}

    record Or(ConditionNode left, ConditionNode right) implements ConditionNode {}

    record Not(ConditionNode inner) implements ConditionNode {}

    record Compare(NumericNode left, CompareOp op, NumericNode right) implements ConditionNode {}
}
