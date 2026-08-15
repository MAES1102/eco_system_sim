package com.ecosystem.simulation.rules;

import com.ecosystem.simulation.entities.Predator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the static-rejection policy (statically detectable target/attribute
 * mismatches fail at rule construction, never silently at runtime) and the
 * writable-attribute range clamping that prevents cumulative mutations
 * (e.g. repeated {@code speed *= 1.1}) from growing without bound.
 */
class RuleVocabularyValidationTest {

    private static final RuleVocabulary VOCAB = new RuleVocabulary();

    @Test
    void unknownReadableAttribute_rejected() {
        RuleParseException ex = assertThrows(RuleParseException.class,
                () -> new Rule("Bad", "Plant", "luck > 5", "die", VOCAB));
        assertTrue(ex.getMessage().contains("luck"));
    }

    @Test
    void unknownWritableAttribute_rejected() {
        RuleParseException ex = assertThrows(RuleParseException.class,
                () -> new Rule("Bad", "Predator", "energy < 10", "luck += 5", VOCAB));
        assertTrue(ex.getMessage().contains("luck"));
    }

    @Test
    void unknownCommand_rejected() {
        RuleParseException ex = assertThrows(RuleParseException.class,
                () -> new Rule("Bad", "Predator", "energy < 10", "teleport", VOCAB));
        assertTrue(ex.getMessage().contains("teleport"));
    }

    @Test
    void attributeNotApplicableToTargetType_rejected() {
        // attackPower only makes sense for Predator, not Herbivore
        RuleParseException ex = assertThrows(RuleParseException.class,
                () -> new Rule("Bad", "Herbivore", "attackPower > 1", "die", VOCAB));
        assertTrue(ex.getMessage().contains("attackPower"));
    }

    @Test
    void writableNotApplicableToTargetType_rejected() {
        assertThrows(RuleParseException.class,
                () -> new Rule("Bad", "Plant", "energy > 0", "defensePower += 1", VOCAB));
    }

    @Test
    void commandNotApplicableToTargetType_rejected() {
        // flee is Herbivore-only
        assertThrows(RuleParseException.class,
                () -> new Rule("Bad", "Predator", "energy > 0", "flee", VOCAB));
    }

    @Test
    void growCommand_onlyValidForPlant() {
        assertThrows(RuleParseException.class,
                () -> new Rule("Bad", "Predator", "energy > 0", "grow", VOCAB));
    }

    @Test
    void reproduceCommand_validForAllReproducibleSpecies() throws RuleParseException {
        // must not throw for any of the three concrete species
        new Rule("R1", "Predator", "energy > 0", "reproduce", VOCAB);
        new Rule("R2", "Herbivore", "energy > 0", "reproduce", VOCAB);
        new Rule("R3", "Plant", "energy > 0", "reproduce", VOCAB);
    }

    @Test
    void organismLevelAttributes_matchAllConcreteTypes() throws RuleParseException {
        // energy/age are Organism-scoped and must validate for every concrete subtype
        new Rule("R1", "Predator", "energy > 0 AND age > 0", "die", VOCAB);
        new Rule("R2", "Herbivore", "energy > 0 AND age > 0", "die", VOCAB);
        new Rule("R3", "Plant", "energy > 0 AND age > 0", "die", VOCAB);
    }

    // ── range clamping ───────────────────────────────────────────────────────

    @Test
    void energyMutation_clampsToZeroAndMaxEnergy() throws RuleParseException {
        Predator predator = new Predator(0, 0, 50, 2.0, 7);
        Rule setHigh = new Rule("SetHigh", "Predator", "energy > 0", "energy = 999", VOCAB);
        setHigh.executeActions(predator);
        assertEquals(predator.getMaxEnergy(), predator.getEnergy(), "energy must clamp at maxEnergy");

        Rule setLow = new Rule("SetLow", "Predator", "energy > 0", "energy = -999", VOCAB);
        setLow.executeActions(predator);
        assertEquals(0, predator.getEnergy(), "energy must clamp at 0, not go negative");
    }

    @Test
    void speedMutation_repeatedMultiplyDoesNotGrowUnbounded() throws RuleParseException {
        Predator predator = new Predator(0, 0, 50, 10.0, 7); // already near the ceiling
        Rule buff = new Rule("Buff", "Predator", "energy > 0", "speed *= 1.1", VOCAB);

        for (int i = 0; i < 50; i++) {
            buff.executeActions(predator);
        }

        assertTrue(predator.getSpeed() <= 10.0, "speed must never exceed its registered ceiling regardless of repeated multiplication");
        assertTrue(predator.getSpeed() >= 0.1, "speed must never fall below its registered floor");
    }

    @Test
    void attackPowerMutation_clampsToRange() throws RuleParseException {
        Predator predator = new Predator(0, 0, 50, 2.0, 7);
        Rule setHigh = new Rule("SetHigh", "Predator", "energy > 0", "attackPower = 9999", VOCAB);
        setHigh.executeActions(predator);
        assertEquals(50, predator.getAttackPower());
    }

    // ── NaN / Infinity policy ────────────────────────────────────────────────

    @Test
    void dynamicDivisionByZero_conditionEvaluatesFalse_doesNotCrash() throws RuleParseException {
        Predator predator = new Predator(0, 0, 50, 2.0, 7); // age == 0
        // age - age == 0 at runtime; the parser cannot see this statically (age is a reference, not a literal)
        Rule rule = new Rule("DivByDynamicZero", "Predator", "energy / (age - age) > 1", "die", VOCAB);

        assertEquals(false, rule.evaluateCondition(predator), "non-finite comparison must evaluate to false, not throw");
        assertTrue(predator.isAlive(), "the simulation must not crash or kill the entity on a NaN/Infinity comparison");
    }

    @Test
    void dynamicDivisionByZero_inMutation_leavesStateUnchanged() throws RuleParseException {
        Predator predator = new Predator(0, 0, 50, 2.0, 7);
        Rule rule = new Rule("BadMutation", "Predator", "energy > 0", "energy = energy / (age - age)", VOCAB);

        rule.executeActions(predator);

        assertEquals(50, predator.getEnergy(), "a non-finite mutation result must be skipped, leaving state unchanged");
    }
}
