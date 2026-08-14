package com.ecosystem.simulation.rules;

import com.ecosystem.simulation.entities.Animal;
import com.ecosystem.simulation.entities.Herbivore;
import com.ecosystem.simulation.entities.Plant;
import com.ecosystem.simulation.entities.Predator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Rule#matches(com.ecosystem.simulation.entities.Entity)}.
 *
 * <p>These tests verify two guarantees:</p>
 * <ol>
 *   <li><b>Exact-type matching</b> — rules targeting a concrete type such as
 *       {@code "Predator"} still only match {@code Predator} instances (no
 *       regression from the original behaviour).</li>
 *   <li><b>Hierarchy matching</b> — rules targeting an abstract superclass
 *       such as {@code "Organism"} or {@code "Animal"} correctly match all
 *       concrete subclasses in the entity hierarchy.  This was the reported
 *       bug: {@code Rule.matches()} previously used exact class-name equality,
 *       so the rule {@code MaximumAge | Organism | age > 100 | die} never fired
 *       on any entity.</li>
 * </ol>
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

    // ── factory helpers ───────────────────────────────────────────────────

    /** Creates a Rule whose only meaningful property for these tests is targetType. */
    private static Rule ruleFor(String targetType) {
        return new Rule("TestRule", targetType, "age > 0", "die");
    }

    private static Predator  aPredator()  { return new Predator (0, 0, 50, 2.0, 7); }
    private static Herbivore aHerbivore() { return new Herbivore(0, 0, 30, 1.5, 5); }
    private static Plant     aPlant()     { return new Plant    (0, 0, 50, 2.0);     }

    // ── exact-type matching — existing behaviour must be preserved ────────

    @Test
    void predatorRule_matchesPredator() {
        assertTrue(ruleFor("Predator").matches(aPredator()),
                "A 'Predator' rule must match a Predator instance");
    }

    @Test
    void herbivoreRule_matchesHerbivore() {
        assertTrue(ruleFor("Herbivore").matches(aHerbivore()),
                "A 'Herbivore' rule must match a Herbivore instance");
    }

    @Test
    void plantRule_matchesPlant() {
        assertTrue(ruleFor("Plant").matches(aPlant()),
                "A 'Plant' rule must match a Plant instance");
    }

    // ── exact-type non-matches — rules must NOT bleed across sibling types ─

    @Test
    void predatorRule_doesNotMatch_herbivore() {
        assertFalse(ruleFor("Predator").matches(aHerbivore()),
                "A 'Predator' rule must not match a Herbivore");
    }

    @Test
    void predatorRule_doesNotMatch_plant() {
        assertFalse(ruleFor("Predator").matches(aPlant()),
                "A 'Predator' rule must not match a Plant");
    }

    @Test
    void herbivoreRule_doesNotMatch_predator() {
        assertFalse(ruleFor("Herbivore").matches(aPredator()),
                "A 'Herbivore' rule must not match a Predator");
    }

    @Test
    void herbivoreRule_doesNotMatch_plant() {
        assertFalse(ruleFor("Herbivore").matches(aPlant()),
                "A 'Herbivore' rule must not match a Plant");
    }

    // ── hierarchy matching — the bug that was fixed ───────────────────────

    /**
     * The MaximumAge rule in rules.txt targets "Organism".
     * Before the fix this rule fired on exactly zero entities.
     * After the fix it must fire on all three concrete organism types.
     */
    @Test
    void organismRule_matchesPredator() {
        assertTrue(ruleFor("Organism").matches(aPredator()),
                "Rule targeting 'Organism' must match Predator (Predator extends Animal extends Organism)");
    }

    @Test
    void organismRule_matchesHerbivore() {
        assertTrue(ruleFor("Organism").matches(aHerbivore()),
                "Rule targeting 'Organism' must match Herbivore (Herbivore extends Animal extends Organism)");
    }

    @Test
    void organismRule_matchesPlant() {
        assertTrue(ruleFor("Organism").matches(aPlant()),
                "Rule targeting 'Organism' must match Plant (Plant extends Organism)");
    }

    /**
     * Animal is the common superclass of Predator and Herbivore only.
     * Plant extends Organism directly (not Animal), so an "Animal" rule
     * must NOT match a Plant.
     */
    @Test
    void animalRule_matchesPredator() {
        assertTrue(ruleFor("Animal").matches(aPredator()),
                "Rule targeting 'Animal' must match Predator");
    }

    @Test
    void animalRule_matchesHerbivore() {
        assertTrue(ruleFor("Animal").matches(aHerbivore()),
                "Rule targeting 'Animal' must match Herbivore");
    }

    @Test
    void animalRule_doesNotMatch_plant() {
        assertFalse(ruleFor("Animal").matches(aPlant()),
                "Rule targeting 'Animal' must NOT match Plant — Plant does not extend Animal");
    }

    /**
     * Entity is the root of the hierarchy.
     * A rule targeting "Entity" must match every concrete type.
     */
    @Test
    void entityRule_matchesAllConcreteTypes() {
        assertTrue(ruleFor("Entity").matches(aPredator()),  "Entity rule must match Predator");
        assertTrue(ruleFor("Entity").matches(aHerbivore()), "Entity rule must match Herbivore");
        assertTrue(ruleFor("Entity").matches(aPlant()),     "Entity rule must match Plant");
    }

    // ── edge cases ────────────────────────────────────────────────────────

    @Test
    void nullEntity_returnsFalse() {
        assertFalse(ruleFor("Predator").matches(null),
                "matches(null) must return false without throwing");
    }

    @Test
    void unknownTargetType_returnsFalse() {
        assertFalse(ruleFor("Dragon").matches(aPredator()),
                "An unrecognised target type must not match any entity");
    }
}
