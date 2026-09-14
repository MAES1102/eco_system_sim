package com.ecosystem.simulation.rules;

import com.ecosystem.simulation.entities.Animal;
import com.ecosystem.simulation.entities.Entity;
import com.ecosystem.simulation.entities.Herbivore;
import com.ecosystem.simulation.entities.Movable;
import com.ecosystem.simulation.entities.Organism;
import com.ecosystem.simulation.entities.Plant;
import com.ecosystem.simulation.entities.Predator;
import com.ecosystem.simulation.entities.Reproducible;
import com.ecosystem.simulation.rules.RuleVocabulary.VocabularyEntry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers listReadables()/listWritables()/listCommands() -- the read-only
 * introspection API RuleBuilderDialog uses to populate its dropdowns.
 * Separate from RuleVocabularyValidationTest, which covers static rejection
 * and range clamping instead.
 */
class RuleVocabularyListingTest {

    private static final RuleVocabulary VOCAB = new RuleVocabulary();

    @Test
    void listReadables_containsAllRegisteredAttributes() {
        assertEquals(15, VOCAB.listReadables().size());
    }

    @Test
    void listWritables_containsOnlyTheFiveMutableAttributes() {
        assertEquals(5, VOCAB.listWritables().size());
    }

    @Test
    void listCommands_containsAllFiveDomainCommands() {
        assertEquals(5, VOCAB.listCommands().size());
    }

    @Test
    void listReadables_requiredTypesMatchRegistration() {
        List<VocabularyEntry> readables = VOCAB.listReadables();
        assertEquals(Organism.class, requiredTypeOf(readables, "age"));
        assertEquals(Entity.class, requiredTypeOf(readables, "x"));
        assertEquals(Predator.class, requiredTypeOf(readables, "attackPower"));
        assertEquals(Herbivore.class, requiredTypeOf(readables, "defensePower"));
        assertEquals(Plant.class, requiredTypeOf(readables, "growthRate"));
        assertEquals(Entity.class, requiredTypeOf(readables, "env.temperature"));
        assertEquals(Entity.class, requiredTypeOf(readables, "stat.plantPopulation"));
    }

    @Test
    void listWritables_requiredTypesMatchRegistration() {
        List<VocabularyEntry> writables = VOCAB.listWritables();
        assertEquals(Organism.class, requiredTypeOf(writables, "energy"));
        assertEquals(Animal.class, requiredTypeOf(writables, "speed"));
        assertEquals(Predator.class, requiredTypeOf(writables, "attackPower"));
    }

    @Test
    void listCommands_requiredTypesMatchRegistration() {
        List<VocabularyEntry> commands = VOCAB.listCommands();
        assertEquals(Entity.class, requiredTypeOf(commands, "die"));
        assertEquals(Movable.class, requiredTypeOf(commands, "move"));
        assertEquals(Plant.class, requiredTypeOf(commands, "grow"));
        assertEquals(Reproducible.class, requiredTypeOf(commands, "reproduce"));
        assertEquals(Herbivore.class, requiredTypeOf(commands, "flee"));
    }

    @Test
    void envAndStatAttributes_areReadableButNeverWritable() {
        // env.temperature is deliberately configuration-only (see RuleVocabulary's
        // registerDefaults comment); the stat.* attributes are derived, not stored.
        List<VocabularyEntry> writables = VOCAB.listWritables();
        assertFalse(containsName(writables, "env.foodLevel"));
        assertFalse(containsName(writables, "env.temperature"));
        assertFalse(containsName(writables, "stat.predatorPopulation"));
    }

    @Test
    void everyWritableAttribute_isAlsoReadable() {
        List<VocabularyEntry> readables = VOCAB.listReadables();
        for (VocabularyEntry writable : VOCAB.listWritables()) {
            assertTrue(containsName(readables, writable.name()),
                    "writable '" + writable.name() + "' should also be readable");
        }
    }

    @Test
    void listReadables_preservesRegistrationOrder() {
        List<VocabularyEntry> readables = VOCAB.listReadables();
        assertEquals("age", readables.get(0).name());
        assertEquals("x", readables.get(1).name());
        assertEquals("y", readables.get(2).name());
    }

    private static Class<?> requiredTypeOf(List<VocabularyEntry> entries, String name) {
        for (VocabularyEntry entry : entries) {
            if (entry.name().equals(name)) {
                return entry.requiredType();
            }
        }
        throw new AssertionError("no entry named '" + name + "'");
    }

    private static boolean containsName(List<VocabularyEntry> entries, String name) {
        for (VocabularyEntry entry : entries) {
            if (entry.name().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
