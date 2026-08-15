package com.ecosystem.simulation.rules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the external-rule-persistence contract: bootstrap-from-classpath-seed,
 * whole-file validation before replacing active rules, rollback on invalid
 * files (the last valid in-memory rule set survives a bad reload), and safe
 * temp-file-then-replace saving.
 */
class RuleRepositoryTest {

    @TempDir
    Path tempDir;

    private RuleRepository newRepository() {
        RuleEngine engine = new RuleEngine(new RuleVocabulary());
        return new RuleRepository(engine, tempDir.resolve("rules.txt"), "/rules.txt");
    }

    @Test
    void bootstrapsFromClasspathSeed_whenActiveFileAbsent() throws IOException, RuleParseException {
        RuleRepository repo = newRepository();
        assertTrue(Files.notExists(repo.getActiveFile()));

        repo.loadActive();

        assertTrue(Files.exists(repo.getActiveFile()), "active file must be created from the bundled seed");
        assertTrue(repo.getEngine().getRuleCount() > 0, "bootstrapped rules must actually load");
    }

    @Test
    void doesNotOverwriteExistingActiveFile() throws IOException, RuleParseException {
        RuleRepository repo = newRepository();
        Files.writeString(repo.getActiveFile(), "OnlyRule | Predator | energy < 5 | die\n");

        repo.loadActive();

        assertEquals(1, repo.getEngine().getRuleCount(), "an existing active file must be used as-is, not replaced by the seed");
    }

    @Test
    void invalidCandidateText_rejectedWithoutMutatingEngine() throws IOException, RuleParseException {
        RuleRepository repo = newRepository();
        repo.loadActive();
        int before = repo.getEngine().getRuleCount();

        assertThrows(RuleParseException.class, () -> repo.validateCandidate("BadRule | Predator | energy $$ 5 | die"));

        assertEquals(before, repo.getEngine().getRuleCount(), "a failed validation must not touch the engine's active rules");
    }

    @Test
    void saveThenLoad_roundTrips() throws IOException, RuleParseException {
        RuleRepository repo = newRepository();
        List<Rule> rules = repo.validateCandidate(
                "A | Predator | energy < 10 | die\nB | Herbivore | age > 5 AND energy < 30 | flee");
        repo.replaceActive(rules);

        RuleEngine freshEngine = new RuleEngine(new RuleVocabulary());
        RuleRepository freshRepo = new RuleRepository(freshEngine, repo.getActiveFile(), "/rules.txt");
        freshRepo.loadActive();

        assertEquals(2, freshEngine.getRuleCount());
        assertEquals("A", freshEngine.getRules().get(0).getName());
        assertEquals("B", freshEngine.getRules().get(1).getName());
    }

    @Test
    void addAndRemove_persistToDisk() throws IOException, RuleParseException {
        RuleRepository repo = newRepository();
        repo.loadActive();
        int seedCount = repo.getEngine().getRuleCount();

        Rule newRule = new Rule("MyNewRule", "Plant", "energy > 10", "grow", repo.getEngine().getVocabulary());
        repo.addAndSave(newRule);
        assertEquals(seedCount + 1, repo.getEngine().getRuleCount());

        repo.removeAndSave("MyNewRule");
        assertEquals(seedCount, repo.getEngine().getRuleCount());

        // reload from disk to prove the removal was actually persisted, not just in-memory
        RuleEngine freshEngine = new RuleEngine(new RuleVocabulary());
        new RuleRepository(freshEngine, repo.getActiveFile(), "/rules.txt").loadActive();
        assertEquals(seedCount, freshEngine.getRuleCount());
    }

    @Test
    void save_neverLeavesATmpFileBehindOnSuccess() throws IOException, RuleParseException {
        RuleRepository repo = newRepository();
        repo.replaceActive(repo.validateCandidate("A | Predator | energy < 10 | die"));

        Path tmp = repo.getActiveFile().resolveSibling(repo.getActiveFile().getFileName().toString() + ".tmp");
        assertTrue(Files.notExists(tmp), "the temp file must be moved away, not left behind, after a successful save");
    }

    /**
     * The core "invalid file must not destroy the last valid rules" guarantee:
     * write a corrupt file directly to disk (bypassing the repository, as if a
     * user hand-edited it badly), then attempt {@link RuleRepository#loadActive()}
     * again — it must throw, and the engine's previous, still-valid rules must
     * remain exactly as they were.
     */
    @Test
    void reloadingCorruptedFile_preservesLastValidRules() throws IOException, RuleParseException {
        RuleRepository repo = newRepository();
        repo.replaceActive(repo.validateCandidate("Good | Predator | energy < 10 | die"));
        assertEquals(1, repo.getEngine().getRuleCount());

        Files.writeString(repo.getActiveFile(), "Bad | Predator | energy <<< 10 | die\n");

        assertThrows(RuleParseException.class, repo::loadActive);

        assertEquals(1, repo.getEngine().getRuleCount(), "previous valid rules must survive a failed reload");
        assertEquals("Good", repo.getEngine().getRules().get(0).getName());
    }
}
