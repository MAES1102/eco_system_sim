package com.ecosystem.simulation.gui;

import com.ecosystem.simulation.rules.Rule;
import com.ecosystem.simulation.rules.RuleParseException;
import com.ecosystem.simulation.rules.RuleRepository;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Free-text rule editor — the direct replacement for the old preset-dropdown
 * {@code RuleBuilderPanel}. Supports the full external-rule workflow: edit,
 * validate (with a precise line/column error message from the parser), save
 * (replaces the entire active rule set atomically via {@link RuleRepository}),
 * reload from disk, and remove one rule by name.
 *
 * <p>This panel never parses or interprets rule text itself — it only calls
 * {@link RuleRepository}, which delegates to {@link com.ecosystem.simulation.rules.RuleEngine}
 * and the {@link com.ecosystem.simulation.rules.lang.Parser}. Keeping parsing
 * out of the GUI layer means the same validation logic runs identically whether
 * a rule comes from this editor, a hand-edited file, or a unit test.</p>
 */
public class RuleEditorPanel extends JPanel {

    private final RuleRepository repository;
    private final JTextArea textArea;
    private final JLabel statusLabel;
    private Runnable onChanged;

    public RuleEditorPanel(RuleRepository repository) {
        this.repository = repository;
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Rule Editor (free text)"));

        textArea = new JTextArea();
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setLineWrap(false);
        textArea.setToolTipText("<html>One rule per line: Name | TargetType | condition | actionList<br>"
                + "Example: ColdStress | Herbivore | env.temperature &lt; 5 AND energy &lt; 40 "
                + "| energy -= 8, speed *= 0.8</html>");
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(220, 150));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JButton validateBtn = new JButton("Validate");
        JButton saveBtn = new JButton("Save (replace all)");
        JButton reloadBtn = new JButton("Reload from file");
        JButton removeBtn = new JButton("Remove by name...");

        validateBtn.setToolTipText("Parse the text above without saving; shows the exact error if invalid.");
        saveBtn.setToolTipText("Validates, then replaces the entire active rule set and persists it safely.");
        reloadBtn.setToolTipText("Discards unsaved edits above and reloads the last saved active file.");
        removeBtn.setToolTipText("Removes one rule by name from the active set and persists.");

        validateBtn.addActionListener(e -> doValidate());
        saveBtn.addActionListener(e -> doSave());
        reloadBtn.addActionListener(e -> doReload());
        removeBtn.addActionListener(e -> doRemove());

        JPanel buttons = new JPanel(new GridLayout(2, 2, 4, 4));
        buttons.add(validateBtn);
        buttons.add(saveBtn);
        buttons.add(reloadBtn);
        buttons.add(removeBtn);

        JPanel south = new JPanel(new BorderLayout(2, 2));
        south.add(statusLabel, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.CENTER);

        add(scroll, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        populateFromEngine();
    }

    /** Registers a callback fired after any successful save/reload/remove, so the caller can refresh other panels. */
    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    private void doValidate() {
        try {
            List<Rule> parsed = repository.validateCandidate(textArea.getText());
            setStatus("Valid — " + parsed.size() + " rule(s) parse successfully.", false);
        } catch (RuleParseException ex) {
            setStatus("Invalid: " + ex.getMessage(), true);
        }
    }

    private void doSave() {
        try {
            List<Rule> parsed = repository.validateCandidate(textArea.getText());
            repository.replaceActive(parsed);
            setStatus("Saved " + parsed.size() + " rule(s) to " + repository.getActiveFile(), false);
            fireChanged();
        } catch (RuleParseException ex) {
            setStatus("Not saved (invalid): " + ex.getMessage(), true);
        } catch (IOException ex) {
            setStatus("Save failed: " + ex.getMessage(), true);
        }
    }

    private void doReload() {
        try {
            repository.loadActive();
            populateFromEngine();
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
            populateFromEngine();
            setStatus("Removed '" + name.trim() + "' (if it existed) and saved.", false);
            fireChanged();
        } catch (IOException ex) {
            setStatus("Remove failed: " + ex.getMessage(), true);
        }
    }

    private void populateFromEngine() {
        StringBuilder sb = new StringBuilder();
        for (Rule r : repository.getEngine().getRules()) {
            sb.append(r.toString()).append('\n');
        }
        textArea.setText(sb.toString());
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setForeground(error ? new Color(0xB00020) : new Color(0x1B5E20));
        statusLabel.setText(message);
    }

    private void fireChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }
}
