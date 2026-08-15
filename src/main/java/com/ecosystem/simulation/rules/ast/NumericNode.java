package com.ecosystem.simulation.rules.ast;

/**
 * Immutable AST for arithmetic expressions used inside conditions and mutation
 * actions (e.g. {@code energy - 5}, {@code attackPower * 2}).
 *
 * <p>A closed (sealed) hierarchy: the grammar defines exactly these three shapes,
 * so the compiler exhaustively checks every {@code switch} over {@code NumericNode}
 * in {@link com.ecosystem.simulation.rules.Evaluator} — no {@code default} branch,
 * no risk of forgetting a case if the grammar never grows.</p>
 *
 * <p>This is deliberately <b>not</b> an example of inclusion polymorphism for the
 * OOP report: nothing here is dispatched virtually. Evaluation lives in one place
 * ({@code Evaluator}) using pattern-matching {@code switch}, which is the
 * appropriate tool for a fixed, closed set of node shapes — see {@code Entity.update()}
 * / {@code SimulationEvent.execute()} for the project's genuine dynamic-dispatch example.</p>
 */
public sealed interface NumericNode {

    /** A literal numeric constant, e.g. {@code 10} or {@code 0.5}. */
    record Literal(double value) implements NumericNode {}

    /**
     * A named reference resolved at evaluation time through
     * {@link com.ecosystem.simulation.rules.RuleVocabulary} — an entity attribute
     * ({@code energy}), an environment attribute ({@code env.foodLevel}), or a
     * simulation statistic ({@code stat.predatorPopulation}).
     */
    record Reference(String name) implements NumericNode {}

    /** A binary arithmetic operation: {@code left op right}. */
    record BinaryOp(NumericNode left, char op, NumericNode right) implements NumericNode {}
}
