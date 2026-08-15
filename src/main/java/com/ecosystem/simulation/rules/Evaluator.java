package com.ecosystem.simulation.rules;

import com.ecosystem.simulation.rules.ast.ConditionNode;
import com.ecosystem.simulation.rules.ast.NumericNode;
import com.ecosystem.simulation.rules.ast.RuleAction;

import java.util.List;

/**
 * Evaluates parsed {@link ConditionNode}/{@link NumericNode}/{@link RuleAction}
 * trees against one {@link EvaluationContext}.
 *
 * <p>Uses exhaustive pattern-matching {@code switch} over the sealed AST
 * hierarchies rather than virtual dispatch — deliberately: the AST's node set
 * is fixed and closed by the grammar, so the compiler-checked exhaustive switch
 * is the more appropriate (and more type-safe) tool here than adding an
 * {@code evaluate()} method to every node type. See the sealed interfaces'
 * own Javadoc for why this should not be cited as the project's inclusion
 * polymorphism example.</p>
 *
 * <h3>Numeric-failure policy</h3>
 * <p>All arithmetic uses {@code double}, so a runtime division by a dynamically
 * zero value produces {@code Infinity}/{@code NaN} rather than throwing.
 * A comparison whose operand is non-finite evaluates to {@code false}; a
 * mutation whose result is non-finite is skipped (state left unchanged). Both
 * are logged as a diagnostic by the caller, never as a crash. Division by a
 * <em>literal</em> zero is instead rejected at parse time by
 * {@link com.ecosystem.simulation.rules.lang.Parser}, since that case is
 * statically detectable without any runtime state.</p>
 */
public final class Evaluator {

    private Evaluator() {
    }

    public static boolean evaluateCondition(ConditionNode node, EvaluationContext ctx, RuleVocabulary vocab) {
        return switch (node) {
            case ConditionNode.And and ->
                    evaluateCondition(and.left(), ctx, vocab) && evaluateCondition(and.right(), ctx, vocab);
            case ConditionNode.Or or ->
                    evaluateCondition(or.left(), ctx, vocab) || evaluateCondition(or.right(), ctx, vocab);
            case ConditionNode.Not not -> !evaluateCondition(not.inner(), ctx, vocab);
            case ConditionNode.Compare cmp -> evaluateCompare(cmp, ctx, vocab);
        };
    }

    private static boolean evaluateCompare(ConditionNode.Compare cmp, EvaluationContext ctx, RuleVocabulary vocab) {
        double left = evaluateNumeric(cmp.left(), ctx, vocab);
        double right = evaluateNumeric(cmp.right(), ctx, vocab);
        if (!Double.isFinite(left) || !Double.isFinite(right)) {
            return false;
        }
        return switch (cmp.op()) {
            case LT -> left < right;
            case LE -> left <= right;
            case GT -> left > right;
            case GE -> left >= right;
            case EQ -> left == right;
            case NE -> left != right;
        };
    }

    public static double evaluateNumeric(NumericNode node, EvaluationContext ctx, RuleVocabulary vocab) {
        return switch (node) {
            case NumericNode.Literal lit -> lit.value();
            case NumericNode.Reference ref -> vocab.readable(ref.name()).get(ctx);
            case NumericNode.BinaryOp bin -> {
                double left = evaluateNumeric(bin.left(), ctx, vocab);
                double right = evaluateNumeric(bin.right(), ctx, vocab);
                yield switch (bin.op()) {
                    case '+' -> left + right;
                    case '-' -> left - right;
                    case '*' -> left * right;
                    case '/' -> left / right;
                    default -> Double.NaN;
                };
            }
        };
    }

    /**
     * Executes actions sequentially (not transactionally): each action applies
     * immediately, so a later action in the list observes any effect an earlier
     * one just had. If the target became stale mid-list (e.g. a prior action
     * caused its own death), remaining actions on it silently no-op — a
     * legitimate DES staleness case, not a user authoring mistake.
     */
    public static void executeActions(List<RuleAction> actions, EvaluationContext ctx, RuleVocabulary vocab) {
        for (RuleAction action : actions) {
            executeAction(action, ctx, vocab);
        }
    }

    private static void executeAction(RuleAction action, EvaluationContext ctx, RuleVocabulary vocab) {
        if (ctx.entity() == null || !ctx.entity().isAlive() || ctx.entity().isPendingRemoval()) {
            return;
        }
        switch (action) {
            case RuleAction.Mutation m -> {
                double current = vocab.readable(m.attribute()).get(ctx);
                double operand = evaluateNumeric(m.value(), ctx, vocab);
                double newValue = switch (m.op()) {
                    case '=' -> operand;
                    case '+' -> current + operand;
                    case '-' -> current - operand;
                    case '*' -> current * operand;
                    case '/' -> current / operand;
                    default -> Double.NaN;
                };
                if (!Double.isFinite(newValue)) {
                    return;
                }
                vocab.writable(m.attribute()).set(ctx, newValue);
            }
            case RuleAction.Command c -> vocab.command(c.name()).execute(ctx.entity(), ctx.world());
        }
    }
}
