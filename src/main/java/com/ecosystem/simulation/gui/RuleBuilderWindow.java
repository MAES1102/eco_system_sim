package com.ecosystem.simulation.gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Standalone GUI window for creating ecosystem simulation rules.
 *
 * <p>A professor should be able to build a valid rule within five seconds:
 * a numbered step-by-step header, all choices are drop-downs, a one-click
 * "Load Example" button pre-fills the canonical starter rule, the preview
 * shows a human-readable WHEN/THEN format (not raw pipe syntax), and
 * an information dialog confirms success after rule creation.</p>
 *
 * <p>The rule written to {@code rules.txt} always follows the engine format:</p>
 * <pre>Name | TargetType | field operator value | action</pre>
 */
public class RuleBuilderWindow extends JFrame {

    private static final String[] TARGET_TYPES =
            {"Entity", "Organism", "Animal", "Predator", "Herbivore", "Plant"};

    private static final String[] CONDITION_PRESETS =
            {"energy < value", "energy > value", "age > value", "age < value"};

    private static final String[] ACTIONS = {"die", "reproduce", "move"};

    private static final String[][] EXAMPLES = {
            {"MaximumAge",       "Organism", "age > 100",   "die"},
            {"StarvingPredator", "Predator", "energy < 10", "die"},
            {"PlantGrowth",      "Plant",    "energy > 80", "reproduce"},
    };

    // ── controls ──────────────────────────────────────────────────────────────
    private JTextField        ruleNameField;
    private JComboBox<String> targetTypeCombo;
    private JComboBox<String> conditionCombo;
    private JTextField        valueField;
    private JComboBox<String> actionCombo;
    private JLabel            previewLabel;
    private JLabel            validationLabel;
    private JTable            examplesTable;

    // ── constructor ───────────────────────────────────────────────────────────

    public RuleBuilderWindow() {
        setTitle("Ecosystem Rule Builder");
        setSize(740, 480);
        setMinimumSize(new Dimension(680, 440));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
        wireLivePreview();
        updatePreview();
    }

    // ── init ──────────────────────────────────────────────────────────────────

    private void initComponents() {
        ruleNameField = new JTextField(18);
        ruleNameField.setToolTipText(
                "A unique label for this rule, e.g. \"StarvingPredator\". "
                + "Shown in the Active Rules list. No spaces or | characters.");

        targetTypeCombo = new JComboBox<>(TARGET_TYPES);
        targetTypeCombo.setToolTipText(
                "Which entities the rule applies to. \"Organism\" matches Predator, Herbivore and Plant; "
                + "\"Animal\" matches Predator and Herbivore.");

        conditionCombo = new JComboBox<>(CONDITION_PRESETS);
        conditionCombo.setToolTipText(
                "When the rule fires: choose a field (energy or age) and a comparison, "
                + "then type the threshold in the Value box.");

        valueField = new JTextField(8);
        valueField.setToolTipText("Whole number compared against the chosen field, e.g. 10, 50 or 100.");

        actionCombo = new JComboBox<>(ACTIONS);
        actionCombo.setToolTipText(
                "What happens: die (removed), reproduce (creates offspring), or move.");

        previewLabel = new JLabel(" ");
        previewLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        previewLabel.setForeground(new Color(0x003580));
        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);

        validationLabel = new JLabel(" ");
        validationLabel.setForeground(new Color(0xB00020));
        validationLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }

    // ── layout ────────────────────────────────────────────────────────────────

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        add(buildHeader(),   BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        center.add(buildFormPanel(),     BorderLayout.CENTER);
        center.add(buildExamplesPanel(), BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    /**
     * Numbered step-by-step header — a professor can follow the five steps
     * without reading any documentation.
     */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JLabel title = new JLabel("Create a Simulation Rule");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        JLabel steps = new JLabel(
                "<html>"
                + "<b>Steps:</b>&nbsp;&nbsp;"
                + "1&nbsp;Choose&nbsp;<b>target</b>&nbsp;&nbsp;&rarr;&nbsp;&nbsp;"
                + "2&nbsp;Choose&nbsp;<b>condition</b>&nbsp;&nbsp;&rarr;&nbsp;&nbsp;"
                + "3&nbsp;Enter&nbsp;<b>value</b>&nbsp;&nbsp;&rarr;&nbsp;&nbsp;"
                + "4&nbsp;Choose&nbsp;<b>action</b>&nbsp;&nbsp;&rarr;&nbsp;&nbsp;"
                + "5&nbsp;Click&nbsp;<b>Create&nbsp;Rule</b>"
                + "</html>");
        steps.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        header.add(title, BorderLayout.NORTH);
        header.add(steps, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Rule Definition"));

        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx  = 0;
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(6, 8, 6, 8);

        GridBagConstraints fc = new GridBagConstraints();
        fc.gridx   = 1;
        fc.anchor  = GridBagConstraints.WEST;
        fc.fill    = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets  = new Insets(6, 8, 6, 8);

        int row = 0;

        lc.gridy = row; fc.gridy = row++;
        form.add(new JLabel("Rule Name:"), lc);
        form.add(ruleNameField, fc);

        lc.gridy = row; fc.gridy = row++;
        form.add(new JLabel("Target Type:"), lc);
        form.add(targetTypeCombo, fc);

        lc.gridy = row; fc.gridy = row++;
        form.add(new JLabel("Condition:"), lc);
        form.add(conditionCombo, fc);

        lc.gridy = row; fc.gridy = row++;
        form.add(new JLabel("Value:"), lc);
        form.add(valueField, fc);

        lc.gridy = row; fc.gridy = row++;
        form.add(new JLabel("Action:"), lc);
        form.add(actionCombo, fc);

        // ── Load Example quick-fill row ──────────────────────────────────────
        GridBagConstraints btnGbc = new GridBagConstraints();
        btnGbc.gridx     = 0;
        btnGbc.gridy     = row;
        btnGbc.gridwidth = 2;
        btnGbc.anchor    = GridBagConstraints.WEST;
        btnGbc.insets    = new Insets(2, 8, 6, 8);

        JButton loadExampleBtn = new JButton("Load Example");
        loadExampleBtn.setToolTipText(
                "Fills in a ready-to-use rule: Predator dies when energy < 10.");
        loadExampleBtn.addActionListener(e -> loadDefaultExample());
        form.add(loadExampleBtn, btnGbc);

        return form;
    }

    private JPanel buildExamplesPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Examples"));
        panel.setToolTipText(
                "Valid example rules. Select a row and click \"Use Selected Example\" to load it.");

        String[] columns = {"Name", "Target", "Condition", "Action"};
        DefaultTableModel model = new DefaultTableModel(EXAMPLES, columns) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        examplesTable = new JTable(model);
        examplesTable.setRowSelectionAllowed(true);
        examplesTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        examplesTable.setPreferredScrollableViewportSize(new Dimension(300, 90));

        JButton useExample = new JButton("Use Selected Example");
        useExample.setToolTipText("Load the selected example rule into the form fields.");
        useExample.addActionListener(e -> loadSelectedExample());

        panel.add(new JScrollPane(examplesTable), BorderLayout.CENTER);
        panel.add(useExample, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // ── WHEN/THEN preview ────────────────────────────────────────────────
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder("Rule Preview"));
        previewPanel.setToolTipText(
                "Human-readable preview. The text written to rules.txt follows the "
                + "engine format: Name | Target | condition | action.");
        previewPanel.add(previewLabel, BorderLayout.CENTER);

        // ── Create Rule button — bold, taller, centred ───────────────────────
        JButton createButton = new JButton("Create Rule");
        createButton.setFont(createButton.getFont().deriveFont(Font.BOLD, 14f));
        createButton.setPreferredSize(new Dimension(160, 38));
        createButton.setToolTipText("Validate the form and append the generated rule to rules.txt.");
        createButton.addActionListener(new CreateRuleListener());

        JPanel south = new JPanel(new BorderLayout(5, 5));
        south.add(validationLabel, BorderLayout.NORTH);
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonWrapper.add(createButton);
        south.add(buttonWrapper, BorderLayout.CENTER);

        bottom.add(previewPanel, BorderLayout.CENTER);
        bottom.add(south, BorderLayout.SOUTH);
        return bottom;
    }

    // ── preview ───────────────────────────────────────────────────────────────

    private void wireLivePreview() {
        ActionListener al = e -> updatePreview();
        targetTypeCombo.addActionListener(al);
        conditionCombo.addActionListener(al);
        actionCombo.addActionListener(al);

        DocumentListener dl = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { updatePreview(); }
            @Override public void removeUpdate(DocumentEvent e)  { updatePreview(); }
            @Override public void changedUpdate(DocumentEvent e) { updatePreview(); }
        };
        ruleNameField.getDocument().addDocumentListener(dl);
        valueField.getDocument().addDocumentListener(dl);
    }

    /**
     * Updates the WHEN/THEN live preview.
     * The actual string written to disk is always the pipe-separated engine format
     * produced by {@link #buildRuleString}.
     */
    private void updatePreview() {
        String[] fieldOp  = parsePreset((String) conditionCombo.getSelectedItem());
        String   target    = (String) targetTypeCombo.getSelectedItem();
        String   action    = (String) actionCombo.getSelectedItem();
        String   rawName   = ruleNameField.getText().trim();
        String   rawValue  = valueField.getText().trim();
        String   nameDisp  = rawName.isEmpty()  ? "<i>&lt;name&gt;</i>"  : rawName;
        String   valDisp   = rawValue.isEmpty() ? "<i>&lt;value&gt;</i>" : rawValue;
        String   condition = fieldOp[0] + " " + fieldOp[1] + " " + valDisp;

        previewLabel.setText(
                "<html>"
                + "<b>WHEN</b> " + target + " " + condition + "<br>"
                + "<b>THEN</b> " + action
                + "&nbsp;&nbsp;<font color='#888888'>(rule name: " + nameDisp + ")</font>"
                + "</html>");
    }

    // ── example loading ───────────────────────────────────────────────────────

    /** Fills in the canonical starter rule: Predator / energy &lt; 10 / die. */
    private void loadDefaultExample() {
        ruleNameField.setText("StarvingPredator");
        targetTypeCombo.setSelectedItem("Predator");
        conditionCombo.setSelectedItem("energy < value");
        valueField.setText("10");
        actionCombo.setSelectedItem("die");
        validationLabel.setText(" ");
        updatePreview();
    }

    private void loadSelectedExample() {
        int row = examplesTable.getSelectedRow();
        if (row < 0) {
            validationLabel.setText("Select an example row first.");
            return;
        }
        String name      = (String) examplesTable.getValueAt(row, 0);
        String target    = (String) examplesTable.getValueAt(row, 1);
        String condition = (String) examplesTable.getValueAt(row, 2);
        String action    = (String) examplesTable.getValueAt(row, 3);

        String[] parts    = condition.split(" ");
        String   presetKey = parts[0] + " " + parts[1] + " value";

        ruleNameField.setText(name);
        targetTypeCombo.setSelectedItem(target);
        conditionCombo.setSelectedItem(presetKey);
        valueField.setText(parts[2]);
        actionCombo.setSelectedItem(action);
        validationLabel.setText(" ");
        updatePreview();
    }

    // ── validation & persistence ──────────────────────────────────────────────

    private String validateFields() {
        String name = ruleNameField.getText().trim();
        if (name.isEmpty()) {
            return "Please enter a rule name (e.g. \"StarvingPredator\").";
        }
        if (name.contains("|")) {
            return "Rule name cannot contain the '|' character.";
        }
        if (name.contains(" ")) {
            return "Rule name cannot contain spaces — use CamelCase like \"MaximumAge\".";
        }
        String value = valueField.getText().trim();
        if (value.isEmpty()) {
            return "Please enter a numeric value for the condition (e.g. 10).";
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return "The value must be a whole number (e.g. 10, 50, 100) — \""
                    + value + "\" is not valid.";
        }
        if (parsed < 0) {
            return "The value should be zero or greater.";
        }
        return null;
    }

    /** Splits a preset such as {@code "energy < value"} into {@code ["energy", "<"]}. */
    private String[] parsePreset(String preset) {
        String[] p = preset.split(" ");
        return new String[]{p[0], p[1]};
    }

    private String buildRuleString(String name, String value) {
        String[] fieldOp  = parsePreset((String) conditionCombo.getSelectedItem());
        String   target    = (String) targetTypeCombo.getSelectedItem();
        String   action    = (String) actionCombo.getSelectedItem();
        String   condition = fieldOp[0] + " " + fieldOp[1] + " " + value;
        return name + " | " + target + " | " + condition + " | " + action;
    }

    private void appendToRulesFile(String ruleString) throws IOException {
        try (PrintWriter writer =
                     new PrintWriter(new FileWriter("src/main/resources/rules.txt", true))) {
            writer.println(ruleString);
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Invalid Rule", JOptionPane.ERROR_MESSAGE);
    }

    private void clearFields() {
        ruleNameField.setText("");
        valueField.setText("");
        targetTypeCombo.setSelectedIndex(0);
        conditionCombo.setSelectedIndex(0);
        actionCombo.setSelectedIndex(0);
        updatePreview();
    }

    // ── inner listener ────────────────────────────────────────────────────────

    private class CreateRuleListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String error = validateFields();
            if (error != null) {
                validationLabel.setText(error);
                showError(error);
                return;
            }

            String name       = ruleNameField.getText().trim();
            String value      = valueField.getText().trim();
            String ruleString = buildRuleString(name, value);

            try {
                appendToRulesFile(ruleString);
                validationLabel.setText(" ");
                clearFields();
                JOptionPane.showMessageDialog(
                        RuleBuilderWindow.this,
                        "Rule created successfully and added to Active Rules.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                showError("Failed to write to rules.txt: " + ex.getMessage());
            }
        }
    }
}
