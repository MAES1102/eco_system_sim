package com.ecosystem.simulation.rules;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Owns the on-disk lifecycle of the <em>active</em>, user-writable rule file,
 * kept separate from the read-only bundled defaults on the classpath.
 *
 * <h3>Layout</h3>
 * <ul>
 *   <li>Bundled default: {@code src/main/resources/rules.txt} (packaged inside the
 *       jar), read only once, as a bootstrap seed — never written to.</li>
 *   <li>Active file: {@code config/rules.txt}, resolved relative to the current
 *       working directory. Created from the bundled seed on first run if absent.
 *       Every load/save after that touches only this file.</li>
 * </ul>
 *
 * <p>This gives identical behaviour whether launched via {@code mvn exec:java}
 * or as a packaged {@code java -jar ...} — both are documented to run from the
 * project root, so both resolve {@code config/} the same way.</p>
 *
 * <h3>Safety</h3>
 * <ul>
 *   <li>{@link #loadActive()} validates the <em>entire</em> file before it can
 *       replace the engine's active rules — a single bad line rejects the whole
 *       load and leaves the previous in-memory rules untouched
 *       ({@link RuleEngine#loadRules} stages into a temporary list first).</li>
 *   <li>{@link #save(List)} writes to a sibling {@code .tmp} file, then attempts
 *       {@link StandardCopyOption#ATOMIC_MOVE}, falling back to a plain
 *       {@link StandardCopyOption#REPLACE_EXISTING} move if the filesystem
 *       doesn't support atomic moves. The active file is never opened for
 *       writing directly, so a failed save can only ever leave behind an
 *       incomplete {@code .tmp} — the last valid active file is untouched.</li>
 * </ul>
 */
public class RuleRepository {

    private final RuleEngine engine;
    private final Path activeFile;
    private final String classpathSeedResource;

    public RuleRepository(RuleEngine engine, Path activeFile, String classpathSeedResource) {
        this.engine = engine;
        this.activeFile = activeFile;
        this.classpathSeedResource = classpathSeedResource;
    }

    /** Copies the bundled classpath default to the active file location if it doesn't exist yet. */
    public void bootstrapIfAbsent() throws IOException {
        if (Files.exists(activeFile)) {
            return;
        }
        Path parent = activeFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (InputStream in = RuleRepository.class.getResourceAsStream(classpathSeedResource)) {
            if (in == null) {
                throw new IOException("Bundled default rules resource not found on classpath: " + classpathSeedResource);
            }
            Files.copy(in, activeFile);
        }
    }

    /**
     * Bootstraps if needed, then loads and validates the active file into the engine.
     * On any parse error, the engine's previously active rules are left exactly as they were.
     */
    public void loadActive() throws IOException, RuleParseException {
        bootstrapIfAbsent();
        engine.loadRules(activeFile.toString());
    }

    /**
     * Validates candidate rule-file text without mutating the engine or any file.
     * Used by the GUI's "Validate" action on unsaved free-text edits.
     *
     * @throws RuleParseException with a precise line/column message if invalid
     */
    public List<Rule> validateCandidate(String text) throws RuleParseException {
        return engine.parseText(text);
    }

    /**
     * Replaces the engine's active rules with an already-validated list and persists
     * them to the active file. Callers should obtain {@code validated} from
     * {@link #validateCandidate(String)} first.
     */
    public void replaceActive(List<Rule> validated) throws IOException {
        engine.clearRules();
        for (Rule r : validated) {
            engine.addRule(r);
        }
        save(engine.getRules());
    }

    /** Adds one rule to the active set and persists, in one step (used by "Add Rule"). */
    public void addAndSave(Rule newRule) throws IOException {
        engine.addRule(newRule);
        save(engine.getRules());
    }

    /** Removes one rule by name from the active set and persists, in one step (used by "Remove"). */
    public void removeAndSave(String name) throws IOException {
        engine.removeRule(name);
        save(engine.getRules());
    }

    /** Safe temp-file-then-replace save of the given rule set to the active file. */
    public void save(List<Rule> rulesToSave) throws IOException {
        Path parent = activeFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = activeFile.resolveSibling(activeFile.getFileName().toString() + ".tmp");
        try (var writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            writer.write("# Ecosystem Simulation - active rules (edited via the Rule Editor or by hand)\n");
            writer.write("# Format: Name | TargetType | condition | actionList\n");
            for (Rule r : rulesToSave) {
                writer.write(r.toString());
                writer.newLine();
            }
        }
        try {
            Files.move(tmp, activeFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, activeFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Path getActiveFile() {
        return activeFile;
    }

    /** Exposes the wrapped engine so the GUI can read the currently active rule list for display. */
    public RuleEngine getEngine() {
        return engine;
    }
}
