package com.ecosystem.simulation.gui;

import com.ecosystem.simulation.rules.Rule;
import com.ecosystem.simulation.rules.RuleParseException;
import com.ecosystem.simulation.rules.RuleRepository;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Rule editor — composes rules exclusively through the point-and-click
 * {@link RuleBuilderDialog}; there is no free-text authoring surface in this
 * GUI (an earlier version had one, with a live vocabulary reference and
 * error highlighting on top of it, but real usability feedback was that the
 * whole idea of writing syntax to change behavior was the problem, not the
 * ergonomics of writing it).
 *
 * <p>What's currently active is shown by the separate, always-up-to-date
 * {@link ActiveRulesPanel}, not duplicated here. This panel only offers the
 * three actions that make sense once authoring is fully visual: build a new
 * rule, reload the active file from disk (to pick up an edit made with an
 * external text editor — {@code config/rules.txt} is still a plain text
 * file), and remove one rule by name.</p>
 *
 * <p>This panel still never parses or interprets rule text itself — every
 * path here still goes through {@link RuleRepository}, which delegates to
 * {@link com.ecosystem.simulation.rules.RuleEngine} and the
 * {@link com.ecosystem.simulation.rules.lang.Parser}. Only the *source* of
 * the text changed (a dialog's dropdowns instead of a hand-typed line); the
 * validation path is exactly the one every other caller already uses.</p>
 */
public class RuleEditorPanel extends JPanel {

    private final RuleRepository repository;
    private final JLabel statusLabel;
    private Runnable onChanged;

    public RuleEditorPanel(RuleRepository repository) {
        this.repository = repository;
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Rule Editor"),
                BorderFactory.createEmptyBorder(4, 8, 8, 8)));

        JLabel info = new JLabel("<html>Compose a rule by picking options — nothing to type<br>"
                + "except numeric thresholds. See <b>Active Rules</b> for what's running.</html>");
        info.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JButton buildRuleBtn = new JButton("Build Rule...");
        JButton reloadBtn = new JButton("Reload from file");
        JButton removeBtn = new JButton("Remove by name...");

        buildRuleBtn.setToolTipText("Opens the visual rule builder; each rule you add is validated and saved immediately.");
        reloadBtn.setToolTipText("Picks up config/rules.txt if it was edited outside this app.");
        removeBtn.setToolTipText("Removes one rule by name from the active set and persists.");

        buildRuleBtn.addActionListener(e -> openRuleBuilder());
        reloadBtn.addActionListener(e -> doReload());
        removeBtn.addActionListener(e -> doRemove());

        JPanel buttons = new JPanel(new GridLayout(3, 1, 4, 4));
        buttons.add(buildRuleBtn);
        buttons.add(reloadBtn);
        buttons.add(removeBtn);

        JPanel center = new JPanel(new BorderLayout(4, 8));
        center.add(info, BorderLayout.NORTH);
        center.add(buttons, BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

        add(center, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    /** Registers a callback fired after any successful add/reload/remove, so the caller can refresh other panels. */
    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    private void openRuleBuilder() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        Frame frameOwner = (owner instanceof Frame) ? (Frame) owner : null;
        RuleBuilderDialog dialog = new RuleBuilderDialog(frameOwner, repository.getEngine().getVocabulary(), this::addRule);
        dialog.setVisible(true);
    }

    /**
     * Validates one rule-builder-composed line against the current active set and,
     * on success, persists it immediately — "Add" in the dialog is now the only save
     * step there is. Returns {@code null} on success or a message to show in the
     * dialog on failure (the dialog's own status label reflects this, not just an
     * optimistic "added" assumption).
     */
    private String addRule(String ruleText) {
        StringBuilder combined = new StringBuilder();
        for (Rule r : repository.getEngine().getRules()) {
            combined.append(r.toString()).append('\n');
        }
        combined.append(ruleText);
        try {
            List<Rule> parsed = repository.validateCandidate(combined.toString());
            repository.replaceActive(parsed);
            setStatus("Added and saved: " + ruleText, false);
            fireChanged();
            return null;
        } catch (RuleParseException ex) {
            setStatus("Not saved: " + ex.getMessage(), true);
            return ex.getMessage();
        } catch (IOException ex) {
            setStatus("Save failed: " + ex.getMessage(), true);
            return ex.getMessage();
        }
    }

    private void doReload() {
        try {
            repository.loadActive();
            setStatus("Reloaded from " + repository.getActiveFile(), false);
            fireChanged();
        } catch (IOException ex) {
            setStatus("Reload failed: " + ex.getMessage(), true);
        } catch (RuleParseException ex) {
            setStatus("Active file on disk is invalid, keeping previous in-memory rules: " + ex.getMessage(), true);
        }
    }

    private void doRemove() {
        String name = JOptionPane.showInputDialog(this, "Rule name to remove:");
        if (name == null || name.isBlank()) {
            return;
        }
        try {
            repository.removeAndSave(name.trim());
            setStatus("Removed '" + name.trim() + "' (if it existed) and saved.", false);
            fireChanged();
        } catch (IOException ex) {
            setStatus("Remove failed: " + ex.getMessage(), true);
        }
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setForeground(error ? new Color(0xB00020) : new Color(0x1B5E20));
        statusLabel.setFont(statusLabel.getFont().deriveFont(error ? Font.BOLD : Font.PLAIN));
        statusLabel.setText(message);
    }

    private void fireChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }
}
