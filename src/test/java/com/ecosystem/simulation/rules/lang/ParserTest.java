package com.ecosystem.simulation.rules.lang;

import com.ecosystem.simulation.rules.RuleParseException;
import com.ecosystem.simulation.rules.ast.CompareOp;
import com.ecosystem.simulation.rules.ast.ConditionNode;
import com.ecosystem.simulation.rules.ast.NumericNode;
import com.ecosystem.simulation.rules.ast.RuleAction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserTest {

    @Test
    void simpleComparison() throws RuleParseException {
        ConditionNode node = Parser.parseCondition("energy < 10", 1);
        ConditionNode.Compare cmp = assertInstanceOf(ConditionNode.Compare.class, node);
        assertEquals(CompareOp.LT, cmp.op());
        assertInstanceOf(NumericNode.Reference.class, cmp.left());
        assertInstanceOf(NumericNode.Literal.class, cmp.right());
    }

    @Test
    void andBindsTighterThanOr() throws RuleParseException {
        // "a OR b AND c" must parse as "a OR (b AND c)"
        ConditionNode node = Parser.parseCondition("energy > 1 OR age > 2 AND x > 3", 1);
        ConditionNode.Or or = assertInstanceOf(ConditionNode.Or.class, node);
        assertInstanceOf(ConditionNode.Compare.class, or.left());
        assertInstanceOf(ConditionNode.And.class, or.right());
    }

    @Test
    void notBindsTighterThanAnd() throws RuleParseException {
        // "NOT a AND b" must parse as "(NOT a) AND b"
        ConditionNode node = Parser.parseCondition("NOT energy > 1 AND age > 2", 1);
        ConditionNode.And and = assertInstanceOf(ConditionNode.And.class, node);
        assertInstanceOf(ConditionNode.Not.class, and.left());
        assertInstanceOf(ConditionNode.Compare.class, and.right());
    }

    @Test
    void parenthesesOverridePrecedence() throws RuleParseException {
        // "(a OR b) AND c" must parse with Or nested inside And, not the default And-first grouping
        ConditionNode node = Parser.parseCondition("(energy > 1 OR age > 2) AND x > 3", 1);
        ConditionNode.And and = assertInstanceOf(ConditionNode.And.class, node);
        assertInstanceOf(ConditionNode.Or.class, and.left());
    }

    @Test
    void arithmeticPrecedence_multiplyBeforeAdd() throws RuleParseException {
        // "2 + 3 * 4" must parse as "2 + (3 * 4)"
        ConditionNode node = Parser.parseCondition("2 + 3 * 4 > 0", 1);
        ConditionNode.Compare cmp = (ConditionNode.Compare) node;
        NumericNode.BinaryOp top = assertInstanceOf(NumericNode.BinaryOp.class, cmp.left());
        assertEquals('+', top.op());
        assertInstanceOf(NumericNode.Literal.class, top.left());
        NumericNode.BinaryOp right = assertInstanceOf(NumericNode.BinaryOp.class, top.right());
        assertEquals('*', right.op());
    }

    @Test
    void compoundConditionWithEnvAndStatReferences() throws RuleParseException {
        ConditionNode node = Parser.parseCondition(
                "stat.plantPopulation > 100 OR env.foodLevel < 20", 1);
        assertInstanceOf(ConditionNode.Or.class, node);
    }

    @Test
    void actionList_multipleSequentialActions() throws RuleParseException {
        List<RuleAction> actions = Parser.parseActions("energy -= 8, speed *= 0.8", 1);
        assertEquals(2, actions.size());
        RuleAction.Mutation first = assertInstanceOf(RuleAction.Mutation.class, actions.get(0));
        assertEquals("energy", first.attribute());
        assertEquals('-', first.op());
        RuleAction.Mutation second = assertInstanceOf(RuleAction.Mutation.class, actions.get(1));
        assertEquals("speed", second.attribute());
        assertEquals('*', second.op());
    }

    @Test
    void unaryMinus_inMutationValue() throws RuleParseException {
        List<RuleAction> actions = Parser.parseActions("energy = -999", 1);
        RuleAction.Mutation m = assertInstanceOf(RuleAction.Mutation.class, actions.get(0));
        NumericNode.BinaryOp negated = assertInstanceOf(NumericNode.BinaryOp.class, m.value());
        assertEquals('-', negated.op());
        assertEquals(0.0, ((NumericNode.Literal) negated.left()).value());
        assertEquals(999.0, ((NumericNode.Literal) negated.right()).value());
    }

    @Test
    void unaryMinus_inCondition() throws RuleParseException {
        ConditionNode node = Parser.parseCondition("env.temperature < -5", 1);
        assertInstanceOf(ConditionNode.Compare.class, node);
    }

    @Test
    void actionList_domainCommand() throws RuleParseException {
        List<RuleAction> actions = Parser.parseActions("reproduce", 1);
        assertEquals(1, actions.size());
        assertInstanceOf(RuleAction.Command.class, actions.get(0));
    }

    // ── invalid syntax ───────────────────────────────────────────────────────

    @Test
    void unbalancedParentheses_rejected() {
        assertThrows(RuleParseException.class, () -> Parser.parseCondition("(energy < 10 AND age > 5", 1));
    }

    @Test
    void danglingBooleanOperator_rejected() {
        assertThrows(RuleParseException.class, () -> Parser.parseCondition("energy < 10 AND", 1));
    }

    @Test
    void missingComparator_rejected() {
        assertThrows(RuleParseException.class, () -> Parser.parseCondition("energy 10", 1));
    }

    @Test
    void trailingGarbage_rejected() {
        assertThrows(RuleParseException.class, () -> Parser.parseCondition("energy < 10 extra", 1));
    }

    @Test
    void blankCondition_rejected() {
        assertThrows(RuleParseException.class, () -> Parser.parseCondition("   ", 1));
    }

    @Test
    void blankActionList_rejected() {
        assertThrows(RuleParseException.class, () -> Parser.parseActions("", 1));
    }

    /**
     * Division by a <em>literal</em> zero is statically detectable and rejected
     * at parse time -- see {@link Parser}'s class Javadoc for why this differs
     * from a dynamically-zero divisor, which is instead a graceful runtime no-op.
     */
    @Test
    void divisionByLiteralZero_rejectedAtParseTime() {
        RuleParseException ex = assertThrows(RuleParseException.class,
                () -> Parser.parseCondition("energy / 0 > 1", 1));
        assertTrue(ex.getMessage().toLowerCase().contains("zero"));
    }

    @Test
    void divisionByLiteralZero_inMutation_rejectedAtParseTime() {
        assertThrows(RuleParseException.class, () -> Parser.parseActions("energy -= 10 / 0", 1));
    }
}
