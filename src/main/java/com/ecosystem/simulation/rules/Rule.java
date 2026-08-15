package com.ecosystem.simulation.rules;

import com.ecosystem.simulation.entities.Entity;
import com.ecosystem.simulation.rules.ast.ConditionNode;
import com.ecosystem.simulation.rules.ast.NumericNode;
import com.ecosystem.simulation.rules.ast.RuleAction;
import com.ecosystem.simulation.rules.lang.Parser;

import java.util.List;

/**
 * A single user-authored rule loaded from the external rules file (or entered
 * as free text and validated live by the GUI).
 *
 * <p>Format: {@code RuleName | TargetType | condition | actionList}, where
 * {@code condition} and {@code actionList} are parsed by
 * {@link com.ecosystem.simulation.rules.lang.Parser} into an immutable
 * {@link ConditionNode} tree and a list of {@link RuleAction}s, then validated
 * against {@link RuleVocabulary} for the resolved target type.</p>
 *
 * <h3>OOP concepts demonstrated</h3>
 * <ul>
 *   <li><b>Encapsulation / information hiding</b>: the raw strings are kept only
 *       for display; callers interact exclusively through {@link #matches},
 *       {@link #evaluateCondition}, and {@link #executeActions}.</li>
 *   <li><b>Composition</b>: a {@code Rule} <em>has</em> a {@link ConditionNode}
 *       and a list of {@link RuleAction}s.</li>
 *   <li><b>Overloading polymorphism</b>: {@link #evaluateCondition} and
 *       {@link #executeActions} each have two overloads — one taking a bare
 *       {@link Entity} (convenience, for simple/unit-test use) and one taking
 *       a full {@link EvaluationContext} (used by the running simulation).</li>
 * </ul>
 *
 * <p>Unlike the original design, this class contains <b>no</b> hardcoded
 * field/operator/action {@code switch} — every attribute, operator, and
 * command is resolved through {@link RuleVocabulary}, the single Open/Closed
 * extension point for the rule language.</p>
 */
public class Rule {

    private final String name;
    private final String targetType;
    private final Class<? extends Entity> targetClass;
    private final String conditionStr;
    private final String actionStr;
    private final ConditionNode condition;
    private final List<RuleAction> actions;
    private final RuleVocabulary vocabulary;

    /**
     * Parses, validates, and constructs a rule.
     *
     * @param lineNumber line number in the source file, for error messages; -1 if not applicable
     * @throws RuleParseException if the condition/action syntax is invalid, the target type is
     *                            unknown, or any referenced attribute/command is not applicable
     *                            to the resolved target type
     */
    public Rule(String name, String targetType, String conditionStr, String actionStr,
                RuleVocabulary vocabulary, int lineNumber) throws RuleParseException {
        this.name = name;
        this.targetType = targetType;
        this.conditionStr = conditionStr;
        this.actionStr = actionStr;
        this.vocabulary = vocabulary;
        this.targetClass = RuleVocabulary.resolveTargetType(targetType, lineNumber);
        this.condition = Parser.parseCondition(conditionStr, lineNumber);
        this.actions = Parser.parseActions(actionStr, lineNumber);
        validateCondition(condition, lineNumber);
        validateActions(lineNumber);
    }

    /** Convenience overload for programmatic/test construction without a source line number. */
    public Rule(String name, String targetType, String conditionStr, String actionStr,
                RuleVocabulary vocabulary) throws RuleParseException {
        this(name, targetType, conditionStr, actionStr, vocabulary, -1);
    }

    // ── static, target-type-aware validation (rejects statically detectable errors) ──

    private void validateCondition(ConditionNode node, int lineNumber) throws RuleParseException {
        switch (node) {
            case ConditionNode.And and -> { validateCondition(and.left(), lineNumber); validateCondition(and.right(), lineNumber); }
            case ConditionNode.Or or -> { validateCondition(or.left(), lineNumber); validateCondition(or.right(), lineNumber); }
            case ConditionNode.Not not -> validateCondition(not.inner(), lineNumber);
            case ConditionNode.Compare cmp -> { validateNumeric(cmp.left(), lineNumber); validateNumeric(cmp.right(), lineNumber); }
        }
    }

    private void validateNumeric(NumericNode node, int lineNumber) throws RuleParseException {
        switch (node) {
            case NumericNode.Literal lit -> { /* nothing to validate */ }
            case NumericNode.Reference ref -> vocabulary.requireReadable(ref.name(), targetClass, lineNumber);
            case NumericNode.BinaryOp bin -> { validateNumeric(bin.left(), lineNumber); validateNumeric(bin.right(), lineNumber); }
        }
    }

    private void validateActions(int lineNumber) throws RuleParseException {
        for (RuleAction action : actions) {
            switch (action) {
                case RuleAction.Mutation m -> {
                    vocabulary.requireWritable(m.attribute(), targetClass, lineNumber);
                    validateNumeric(m.value(), lineNumber);
                }
                case RuleAction.Command c -> vocabulary.requireCommand(c.name(), targetClass, lineNumber);
            }
        }
    }

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Checks whether this rule applies to the given entity by walking its class
     * hierarchy upward (stopping before {@link Object}), so a rule targeting an
     * abstract superclass (e.g. {@code Organism}) matches every concrete subtype.
     */
    public boolean matches(Entity entity) {
        if (entity == null) {
            return false;
        }
        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            if (clazz.getSimpleName().equals(this.targetType)) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    /** Overload 1 of 2: convenience entry point building a minimal context from the entity alone. */
    public boolean evaluateCondition(Entity entity) {
        return evaluateCondition(contextFor(entity));
    }

    /** Overload 2 of 2: full evaluation against an explicit {@link EvaluationContext}. */
    public boolean evaluateCondition(EvaluationContext ctx) {
        if (ctx == null || ctx.entity() == null) {
            return false;
        }
        return Evaluator.evaluateCondition(condition, ctx, vocabulary);
    }

    /** Overload 1 of 2: convenience entry point building a minimal context from the entity alone. */
    public void executeActions(Entity entity) {
        executeActions(contextFor(entity));
    }

    /** Overload 2 of 2: full execution against an explicit {@link EvaluationContext}. */
    public void executeActions(EvaluationContext ctx) {
        if (ctx == null || ctx.entity() == null) {
            return;
        }
        Evaluator.executeActions(actions, ctx, vocabulary);
    }

    private EvaluationContext contextFor(Entity entity) {
        if (entity == null) {
            return new EvaluationContext(null, null, null, null);
        }
        return new EvaluationContext(entity, entity.getWorld(), entity.getStatistics(), entity.getSchedulingContext());
    }

    // ── getters ──────────────────────────────────────────────────────────────

    public String getName() { return name; }
    public String getTargetType() { return targetType; }
    public String getCondition() { return conditionStr; }
    public String getAction() { return actionStr; }

    @Override
    public String toString() {
        return name + " | " + targetType + " | " + conditionStr + " | " + actionStr;
    }
}
