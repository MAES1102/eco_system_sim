package com.ecosystem.simulation.gui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Embeddable Rule Builder panel — v7.0 (UX refinement).
 *
 * <p>Visual layout (top → bottom) that fits comfortably in a 200-px-wide cell:</p>
 * <ol>
 *   <li><b>Compact banner</b> — single line, ≤ 24 px tall.</li>
 *   <li><b>Vertical form</b> — Target / Condition / Value / Action; each with a
 *       bold label above a full-width control.</li>
 *   <li><b>Rule Preview</b> — compact tinted box, ~38 px tall (≈ 44 % smaller
 *       than v6), no TitledBorder overhead.</li>
 *   <li><b>Button area</b> (SOUTH):
 *       <ul>
 *         <li>Row 1: {@code Examples ▾} dropdown button (left-aligned, compact)</li>
 *         <li>Row 2: {@code Create Rule} — full-width primary button, 38 px tall</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * @author Ecosystem Simulation Team
 * @version 7.0
 */
public class RuleBuilderPanel extends JPanel {

    // ── static data ───────────────────────────────────────────────────────────

    private static final String[] TARGET_TYPES =
            {"Entity", "Organism", "Animal", "Predator", "Herbivore", "Plant"};

    private static final String[] CONDITION_PRESETS =
            {"energy < value", "energy > value", "age > value", "age < value"};

    private static final String[] ACTIONS = {"die", "reproduce", "move"};

    /**
     * Built-in example rules: {internalName, target, condition, action}.
     * The three entries map to the three popup menu items shown to the user.
     */
    private static final String[][] EXAMPLES = {
            {"StarvingPredator", "Predator",  "energy < 10", "die"},
            {"OldHerbivore",     "Herbivore", "age > 50",    "move"},
            {"PlantGrowth",      "Plant",     "energy > 30", "reproduce"},
    };

    /** Human-readable labels for the Examples ▾ popup menu (same order as EXAMPLES). */
    private static final String[] EXAMPLE_LABELS = {
            "Predator dies when energy < 10",
            "Herbivore flees when age > 50",
            "Plant reproduces when energy > 30",
    };

    // ── colour palette ────────────────────────────────────────────────────────
    private static final Color BANNER_BG      = new Color(0xEEF2FF);
    private static final Color BANNER_BORDER  = new Color(0xBBCCEE);
    private static final Color PREVIEW_BG     = new Color(0xF4F7FF);
    private static final Color PREVIEW_FG     = new Color(0x003580);
    private static final Color PREVIEW_LINE   = new Color(0x99AACC);

    // ── shared fonts ──────────────────────────────────────────────────────────
    private static final Font CONTROL_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font LABEL_FONT   = new Font("SansSerif", Font.BOLD,  13);
    private static final Font SEC_BTN_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font PRIMARY_FONT = new Font("SansSerif", Font.BOLD,  13);
    /** Slightly smaller font for the two-line Rule Preview to keep it compact. */
    private static final Font PREVIEW_FONT = new Font("SansSerif", Font.PLAIN, 12);

    private static final int INPUT_HEIGHT = 28;

    // ── controls ──────────────────────────────────────────────────────────────
    private JComboBox<String> targetCombo;
    private JComboBox<String> conditionCombo;
    private JTextField        valueField;
    private JComboBox<String> actionCombo;
    private JButton           createButton;
    /** Replaces the two separate "Load Example" and "Examples" buttons. */
    private JButton           examplesMenuButton;
    private JLabel            previewLabel;

    private int ruleCounter = 1;

    // ── constructor ───────────────────────────────────────────────────────────

    public RuleBuilderPanel() {
        setLayout(new BorderLayout(0, 4));

        TitledBorder outerTitle = BorderFactory.createTitledBorder("Rule Builder");
        outerTitle.setTitleFont(LABEL_FONT);
        setBorder(BorderFactory.createCompoundBorder(
                outerTitle,
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        initControls();

        add(buildHelpBanner(),  BorderLayout.NORTH);
        add(buildFormPanel(),   BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);

        wirePreviewListeners();
        refreshPreview();
    }

    // ── control initialisation ────────────────────────────────────────────────

    private void initControls() {
        // ── Target combo ─────────────────────────────────────────────────────
        targetCombo = new JComboBox<>(TARGET_TYPES);
        targetCombo.setFont(CONTROL_FONT);
        targetCombo.setPreferredSize(new Dimension(260, INPUT_HEIGHT));
        targetCombo.setToolTipText("Choose which organism this rule affects.");

        // ── Condition combo ───────────────────────────────────────────────────
        conditionCombo = new JComboBox<>(CONDITION_PRESETS);
        conditionCombo.setFont(CONTROL_FONT);
        conditionCombo.setPreferredSize(new Dimension(260, INPUT_HEIGHT));
        conditionCombo.setToolTipText("Choose the property to evaluate.");

        // ── Value text field with placeholder ────────────────────────────────
        valueField = new JTextField();
        valueField.setFont(CONTROL_FONT);
        valueField.setPreferredSize(new Dimension(260, INPUT_HEIGHT));
        // Aqua LAF renders this as ghost text inside the field; other LAFs
        // ignore it gracefully (tooltip covers those platforms).
        valueField.putClientProperty("JTextField.placeholderText", "Example: 10");
        valueField.setToolTipText(
                "Numeric threshold compared against the chosen property. Example: 10");

        // ── Action combo ──────────────────────────────────────────────────────
        actionCombo = new JComboBox<>(ACTIONS);
        actionCombo.setFont(CONTROL_FONT);
        actionCombo.setPreferredSize(new Dimension(260, INPUT_HEIGHT));
        actionCombo.setToolTipText("What happens when the condition is true.");

        // ── Examples dropdown button ──────────────────────────────────────────
        examplesMenuButton = new JButton("Examples  \u25BE"); // ▾ downward triangle
        examplesMenuButton.setFont(SEC_BTN_FONT);
        examplesMenuButton.setMargin(new Insets(2, 8, 2, 8));
        examplesMenuButton.setToolTipText("Load a built-in example rule into the form.");

        JPopupMenu examplesMenu = new JPopupMenu();
        for (int i = 0; i < EXAMPLE_LABELS.length; i++) {
            final int idx = i;
            JMenuItem item = new JMenuItem(EXAMPLE_LABELS[idx]);
            item.setFont(CONTROL_FONT);
            item.addActionListener(e -> loadExampleByRow(idx));
            examplesMenu.add(item);
        }
        examplesMenuButton.addActionListener(e ->
                examplesMenu.show(examplesMenuButton, 0, examplesMenuButton.getHeight()));

        // ── Primary button — full-width, 38 px tall ───────────────────────────
        createButton = new JButton("Create Rule");
        createButton.setFont(PRIMARY_FONT);
        // Height=38 drives the SOUTH panel's preferred height; width is overridden
        // by BorderLayout.CENTER to fill the full panel width.
        createButton.setPreferredSize(new Dimension(200, 38));
        createButton.setMargin(new Insets(4, 8, 4, 8));
        createButton.setToolTipText("Validate and append the rule to rules.txt.");

        // ── Preview label ─────────────────────────────────────────────────────
        previewLabel = new JLabel(" ");
        previewLabel.setFont(PREVIEW_FONT);      // 12 pt — compact
        previewLabel.setForeground(PREVIEW_FG);
        previewLabel.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6)); // tight padding
    }

    // ── panel builders ────────────────────────────────────────────────────────

    /**
     * Compact single-line banner ≤ 24 px tall.
     * EmptyBorder(3,8,3,8) + 13pt label height + 1px bottom border ≈ 3+17+3+1 = 24 px.
     */
    private JPanel buildHelpBanner() {
        JLabel label = new JLabel(
                "<html><b>Create&nbsp;rule:&nbsp;&nbsp;</b>"
                + "Target&nbsp;<b>→</b>&nbsp;Condition&nbsp;<b>→</b>&nbsp;"
                + "Value&nbsp;<b>→</b>&nbsp;Action</html>");
        label.setFont(CONTROL_FONT);
        label.setOpaque(false);

        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(BANNER_BG);
        banner.setOpaque(true);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BANNER_BORDER),
                BorderFactory.createEmptyBorder(3, 8, 3, 8))); // 3 px top/bottom → ≤ 24 px
        banner.add(label, BorderLayout.CENTER);
        return banner;
    }

    /**
     * Single-column form with four control groups + compact Rule Preview.
     *
     * <p>Total form content height ≈ 249 px on a 460-px panel with CENTER ≈ 318 px —
     * leaves ≈ 69 px for the filler, ensuring all rows are always fully visible.</p>
     *
     * <p>Preview box uses a plain {@code LineBorder} instead of {@code TitledBorder},
     * saving ≈ 22 px of title-area overhead and achieving the 40 % height reduction.</p>
     */
    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx   = 0;
        gc.fill    = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.anchor  = GridBagConstraints.NORTHWEST;

        // ── Target ──────────────────────────────────────────────────────────
        gc.gridy = 0; gc.insets = new Insets(2, 0, 1, 0);
        panel.add(formLabel("Target:"), gc);
        gc.gridy = 1; gc.insets = new Insets(0, 0, 5, 0);
        panel.add(targetCombo, gc);

        // ── Condition ────────────────────────────────────────────────────────
        gc.gridy = 2; gc.insets = new Insets(2, 0, 1, 0);
        panel.add(formLabel("Condition:"), gc);
        gc.gridy = 3; gc.insets = new Insets(0, 0, 5, 0);
        panel.add(conditionCombo, gc);

        // ── Value ────────────────────────────────────────────────────────────
        gc.gridy = 4; gc.insets = new Insets(2, 0, 1, 0);
        panel.add(formLabel("Value:"), gc);
        gc.gridy = 5; gc.insets = new Insets(0, 0, 5, 0);
        panel.add(valueField, gc);

        // ── Action ───────────────────────────────────────────────────────────
        gc.gridy = 6; gc.insets = new Insets(1, 0, 1, 0);
        panel.add(formLabel("Action:"), gc);
        gc.gridy = 7; gc.insets = new Insets(0, 0, 5, 0);
        panel.add(actionCombo, gc);

        // ── Rule Preview (compact, no TitledBorder) ──────────────────────────
        gc.gridy  = 8;
        gc.insets = new Insets(4, 0, 1, 0);
        gc.fill   = GridBagConstraints.BOTH;
        gc.weighty = 0.0;
        panel.add(buildPreviewSection(), gc);

        // ── Vertical filler ───────────────────────────────────────────────────
        gc.gridy   = 9;
        gc.weighty = 1.0;
        gc.fill    = GridBagConstraints.VERTICAL;
        gc.insets  = new Insets(0, 0, 0, 0);
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        panel.add(filler, gc);

        return panel;
    }

    /**
     * Compact Rule Preview box — no TitledBorder overhead.
     *
     * <p>Old height: TitledBorder(22px) + EmptyBorder(6+6px) + 2 lines×17px = 68 px.</p>
     * <p>New height: LineBorder(2px) + EmptyBorder(3+3px) + 2 lines×15px = 38 px
     * — a <b>44 % reduction</b>.</p>
     */
    private JPanel buildPreviewSection() {
        JPanel previewBox = new JPanel(new BorderLayout());
        previewBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PREVIEW_LINE, 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        previewBox.setBackground(PREVIEW_BG);
        previewBox.setOpaque(true);
        previewBox.add(previewLabel, BorderLayout.CENTER);
        return previewBox;
    }

    /**
     * SOUTH button area — two rows, no FlowLayout wrapping, fits any panel width.
     *
     * <pre>
     * Row 1: [ Examples ▾ ] ←──── left-aligned, natural width
     * Row 2: [        Create Rule         ] ←──── full-width, 38 px tall
     * </pre>
     *
     * <p>The helper {@code Examples ▾} button opens a popup menu and is left-aligned
     * with glue filling the rest of its row.  {@code Create Rule} fills the full width
     * via {@code BorderLayout.CENTER}.</p>
     */
    private JPanel buildButtonPanel() {
        // Row 1: Examples ▾ left-aligned
        JPanel examplesRow = new JPanel();
        examplesRow.setLayout(new BoxLayout(examplesRow, BoxLayout.X_AXIS));
        examplesRow.setOpaque(false);
        examplesRow.add(examplesMenuButton);
        examplesRow.add(Box.createHorizontalGlue());

        // Outer panel: helper row above primary button
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        panel.add(examplesRow,   BorderLayout.NORTH);
        panel.add(createButton,  BorderLayout.CENTER);

        return panel;
    }

    // ── label helper ──────────────────────────────────────────────────────────

    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(LABEL_FONT);
        return l;
    }

    // ── live preview ──────────────────────────────────────────────────────────

    private void wirePreviewListeners() {
        ActionListener al = e -> refreshPreview();
        targetCombo.addActionListener(al);
        conditionCombo.addActionListener(al);
        actionCombo.addActionListener(al);

        valueField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { refreshPreview(); }
            @Override public void removeUpdate(DocumentEvent e)  { refreshPreview(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshPreview(); }
        });
    }

    private void refreshPreview() {
        String[] fieldOp  = parsePreset((String) conditionCombo.getSelectedItem());
        String   target    = (String) targetCombo.getSelectedItem();
        String   action    = (String) actionCombo.getSelectedItem();
        String   rawValue  = valueField.getText().trim();
        String   display   = rawValue.isEmpty() ? "<i>&lt;value&gt;</i>" : rawValue;
        String   condition = fieldOp[0] + " " + fieldOp[1] + " " + display;

        previewLabel.setText(
                "<html>"
                + "<b>WHEN</b>&nbsp;" + target + "&nbsp;" + condition + "<br>"
                + "<b>THEN</b>&nbsp;" + action
                + "</html>");
    }

    // ── example loading ───────────────────────────────────────────────────────

    private void loadExampleByRow(int row) {
        String   target    = EXAMPLES[row][1];
        String   condition = EXAMPLES[row][2];
        String   action    = EXAMPLES[row][3];
        String[] parts     = condition.split(" ");
        String   presetKey = parts[0] + " " + parts[1] + " value";

        targetCombo.setSelectedItem(target);
        conditionCombo.setSelectedItem(presetKey);
        valueField.setText(parts[2]);
        actionCombo.setSelectedItem(action);
        refreshPreview();
    }

    // ── rule parsing helpers ──────────────────────────────────────────────────

    private String[] parsePreset(String preset) {
        String[] p = preset.split(" ");
        return new String[]{p[0], p[1]};
    }

    private String buildRuleString(String name, String value) {
        String[] fieldOp  = parsePreset((String) conditionCombo.getSelectedItem());
        String   target    = (String) targetCombo.getSelectedItem();
        String   action    = (String) actionCombo.getSelectedItem();
        String   condition = fieldOp[0] + " " + fieldOp[1] + " " + value;
        return name + " | " + target + " | " + condition + " | " + action;
    }

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Registers the external action listener invoked when "Create Rule" is clicked.
     * The listener is responsible for reloading rules in the engine after creation.
     */
    public void setCreateActionListener(ActionListener listener) {
        createButton.addActionListener(listener);
    }

    /**
     * Validates the form and, if valid, appends the generated rule to {@code rules.txt}.
     *
     * @return {@code true} if the rule was created and saved, {@code false} otherwise
     */
    public boolean createRule() {
        String value = valueField.getText().trim();

        if (value.isEmpty()) {
            showError("Please enter a numeric value for the condition (e.g. 10).");
            return false;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            showError("The value must be a whole number (e.g. 10, 50, 100) — \""
                    + value + "\" is not valid.");
            return false;
        }
        if (parsed < 0) {
            showError("The value should be zero or greater.");
            return false;
        }

        String ruleString = buildRuleString(generateRuleName(), value);
        try {
            appendToRulesFile(ruleString);
            clearFields();
            ruleCounter++;
            JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "Rule created successfully and added to Active Rules.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (IOException ex) {
            showError("Failed to write to rules.txt: " + ex.getMessage());
            return false;
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                SwingUtilities.getWindowAncestor(this), message,
                "Invalid Rule", JOptionPane.ERROR_MESSAGE);
    }

    private String generateRuleName() {
        return String.format("Rule_%03d", ruleCounter);
    }

    private void appendToRulesFile(String ruleString) throws IOException {
        try (PrintWriter writer =
                     new PrintWriter(new FileWriter("src/main/resources/rules.txt", true))) {
            writer.println(ruleString);
        }
    }

    private void clearFields() {
        valueField.setText("");
        refreshPreview();
    }
}
