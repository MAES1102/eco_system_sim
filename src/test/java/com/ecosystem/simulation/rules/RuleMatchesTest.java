package com.ecosystem.simulation.rules;

import com.ecosystem.simulation.entities.Animal;
import com.ecosystem.simulation.entities.Herbivore;
import com.ecosystem.simulation.entities.Plant;
import com.ecosystem.simulation.entities.Predator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Rule#matches(com.ecosystem.simulation.entities.Entity)}
 * and, per the target-type-aware validation added for the rule DSL, for
 * rejection of unknown target types at construction time.
 *
 * <p>Entity hierarchy under test:</p>
 * <pre>
 *   Entity (abstract)
 *   └── Organism (abstract)
 *       ├── Animal (abstract)
 *       │   ├── Predator   ← concrete
 *       │   └── Herbivore  ← concrete
 *       └── Plant          ← concrete
 * </pre>
 */
class RuleMatchesTest {

    private static final RuleVocabulary VOCAB = new RuleVocabulary();

    // Uses "x" (Entity-scoped) rather than "age" (Organism-scoped) so this helper
    // works for every target type this test exercises, including bare "Entity".
    private static Rule ruleFor(String targetType) throws RuleParseException {
        return new Rule("TestRule", targetType, "x >= 0", "die", VOCAB);
    }

    private static Predator aPredator() { return new Predator(0, 0, 50, 2.0, 7); }
    private static Herbivore aHerbivore() { return new Herbivore(0, 0, 30, 1.5, 5); }
    private static Plant aPlant() { return new Plant(0, 0, 50, 2.0); }

    // ── exact-type matching ─────────────────────────────────────────────────

    @Test
    void predatorRule_matchesPredator() throws RuleParseException {
        assertTrue(ruleFor("Predator").matches(aPredator()));
    }

    @Test
    void herbivoreRule_matchesHerbivore() throws RuleParseException {
        assertTrue(ruleFor("Herbivore").matches(aHerbivore()));
    }

    @Test
    void plantRule_matchesPlant() throws RuleParseException {
        assertTrue(ruleFor("Plant").matches(aPlant()));
    }

    // ── exact-type non-matches ──────────────────────────────────────────────

    @Test
    void predatorRule_doesNotMatch_herbivore() throws RuleParseException {
        assertFalse(ruleFor("Predator").matches(aHerbivore()));
    }

    @Test
    void predatorRule_doesNotMatch_plant() throws RuleParseException {
        assertFalse(ruleFor("Predator").matches(aPlant()));
    }

    @Test
    void herbivoreRule_doesNotMatch_predator() throws RuleParseException {
        assertFalse(ruleFor("Herbivore").matches(aPredator()));
    }

    @Test
    void herbivoreRule_doesNotMatch_plant() throws RuleParseException {
        assertFalse(ruleFor("Herbivore").matches(aPlant()));
    }

    // ── hierarchy matching ──────────────────────────────────────────────────

    @Test
    void organismRule_matchesPredator() throws RuleParseException {
        assertTrue(ruleFor("Organism").matches(aPredator()));
    }

    @Test
    void organismRule_matchesHerbivore() throws RuleParseException {
        assertTrue(ruleFor("Organism").matches(aHerbivore()));
    }

    @Test
    void organismRule_matchesPlant() throws RuleParseException {
        assertTrue(ruleFor("Organism").matches(aPlant()));
    }

    @Test
    void animalRule_matchesPredator() throws RuleParseException {
        assertTrue(ruleFor("Animal").matches(aPredator()));
    }

    @Test
    void animalRule_matchesHerbivore() throws RuleParseException {
        assertTrue(ruleFor("Animal").matches(aHerbivore()));
    }

    @Test
    void animalRule_doesNotMatch_plant() throws RuleParseException {
        assertFalse(ruleFor("Animal").matches(aPlant()));
    }

    @Test
    void entityRule_matchesAllConcreteTypes() throws RuleParseException {
        assertTrue(ruleFor("Entity").matches(aPredator()));
        assertTrue(ruleFor("Entity").matches(aHerbivore()));
        assertTrue(ruleFor("Entity").matches(aPlant()));
    }

    // ── edge cases ────────────────────────────────────────────────────────

    @Test
    void nullEntity_returnsFalse() throws RuleParseException {
        assertFalse(ruleFor("Predator").matches(null));
    }

    /**
     * Semantic change from the original design: an unrecognised target type is
     * now rejected at <b>construction time</b> (statically detectable, per the
     * rule-validation policy), rather than silently constructing a rule that
     * simply never matches anything.
     */
    @Test
    void unknownTargetType_rejectedAtConstruction() {
        RuleParseException ex = assertThrows(RuleParseException.class, () -> ruleFor("Dragon"));
        assertTrue(ex.getMessage().contains("Dragon"));
    }
}
