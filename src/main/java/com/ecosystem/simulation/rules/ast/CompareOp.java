package com.ecosystem.simulation.rules.ast;

/**
 * Comparison operators supported by {@link ConditionNode.Compare}.
 * Named {@code CompareOp} (not {@code Comparator}) to avoid colliding with
 * {@code java.util.Comparator}.
 */
public enum CompareOp {
    LT, LE, GT, GE, EQ, NE
}
