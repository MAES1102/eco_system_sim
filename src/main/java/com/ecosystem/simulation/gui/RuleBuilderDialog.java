package com.ecosystem.simulation.gui;

import com.ecosystem.simulation.entities.Entity;
import com.ecosystem.simulation.rules.RuleParseException;
import com.ecosystem.simulation.rules.RuleVocabulary;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Point-and-click rule composer: every part of a rule (target type, up to two
 * conditions, one action) is chosen from a dropdown populated directly from
 * {@link RuleVocabulary}, so nothing here can name an attribute or command that
 * does not exist for the chosen type — no syntax to remember or mistype. The
 * only free-typed inputs are the rule's name and its numeric thresholds/values.
 *
 * <p>This dialog does not parse or validate anything itself; it only builds
 * the {@code Name | TargetType | condition | actionList} text the engine's
 * parser already accepts and hands it to {@code onRuleBuilt}, which validates
 * and persists it through {@link RuleEditorPanel}'s single existing path and
 * reports back {@code null} (success) or an error message — this keeps parsing
 * in one place, matching the rest of the project, and this dialog's own status
 * reflects what actually happened rather than assuming success.</p>
 */
public class RuleBuilderDialog extends JDialog {

    private static final String[] TARGET_TYPES = {"Entity", "Organism", "Animal", "Predator", "Herbivore", "Plant"};
    private static final String[] COMPARISONS = {"<", "<=", ">", ">=", "==", "!="};
    private static final String[] MUTATION_OPS = {"=", "+=", "-=", "*=", "/="};
    private static final String NO_CONNECTOR = "(single condition)";
    private static final String[] CONNECTORS = {NO_CONNECTOR, "AND", "OR"};
    private static final String ACTION_MUTATE = "Change an attribute";
    private static final String ACTION_COMMAND = "Run a command";

    private final RuleVocabulary vocabulary;
    private final Function<String, String> onRuleBuilt;

    private final JTextField nameField = new JTextField("NewRule", 14);
    private final JComboBox<String> targetCombo = new JComboBox<>(TARGET_TYPES);

    private final JComboBox<String> attr1Combo = new JComboBox<>();
    private final JComboBox<String> op1Combo = new JComboBox<>(COMPARISONS);
    private final JTextField value1Field = new JTextField("10", 6);

    private final JComboBox<String> connectorCombo = new JComboBox<>(CONNECTORS);
    private final JComboBox<String> attr2Combo = new JComboBox<>();
    private final JComboBox<String> op2Combo = new JComboBox<>(COMPARISONS);
    private final JTextField value2Field = new JTextField("10", 6);

    private final JComboBox<String> actionTypeCombo = new JComboBox<>(new String[]{ACTION_MUTATE, ACTION_COMMAND});
    private final JComboBox<String> writableCombo = new JComboBox<>();
    private final JComboBox<String> mutationOpCombo = new JComboBox<>(MUTATION_OPS);
    private final JTextField actionValueField = new JTextField("10", 6);
    private final JComboBox<String> commandCombo = new JComboBox<>();

    private final JLabel statusLabel = new JLabel(" ");

    public RuleBuilderDialog(Frame owner, RuleVocabulary vocabulary, Function<String, String> onRuleBuilt) {
        super(owner, "Build a Rule", false);
        this.vocabulary = vocabulary;
        this.onRuleBuilt = onRuleBuilt;

        setLayout(new BorderLayout(6, 6));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        form.add(row("Rule name:", nameField));
        form.add(row("Applies to:", targetCombo));
        form.add(Box.createVerticalStrut(6));
        form.add(sectionLabel("IF (condition)"));
        form.add(row("", attr1Combo, op1Combo, value1Field));
        form.add(row("Combine with:", connectorCombo));
        form.add(row("", attr2Combo, op2Combo, value2Field));
        form.add(Box.createVerticalStrut(6));
        form.add(sectionLabel("THEN (action)"));
        form.add(row("", actionTypeCombo));
        form.add(row("Attribute:", writableCombo, mutationOpCombo, actionValueField));
        form.add(row("Command:", commandCombo));

        add(new JScrollPane(form), BorderLayout.CENTER);

        JButton addBtn = new JButton("Add to Editor");
        JButton closeBtn = new JButton("Close");
        addBtn.addActionListener(e -> doAdd());
        closeBtn.addActionListener(e -> dispose());
        JPanel south = new JPanel(new BorderLayout(4, 4));
        south.add(statusLabel, BorderLayout.NORTH);
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        buttonRow.add(closeBtn);
        buttonRow.add(addBtn);
        south.add(buttonRow, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        targetCombo.addActionListener(e -> refreshForTargetType());
        actionTypeCombo.addActionListener(e -> refreshActionVisibility());

        refreshForTargetType();
        refreshActionVisibility();

        setPreferredSize(new Dimension(380, 480));
        pack();
        setLocationRelativeTo(owner);
    }

    private static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JPanel row(String label, JComponent... fields) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (!label.isEmpty()) {
            JLabel l = new JLabel(label);
            l.setPreferredSize(new Dimension(80, l.getPreferredSize().height));
            panel.add(l);
        }
        for (JComponent f : fields) {
            panel.add(f);
        }
        return panel;
    }

    /** Re-populates every attribute/command dropdown to only what {@link RuleVocabulary} allows for the chosen type. */
    private void refreshForTargetType() {
        Class<? extends Entity> targetClass = resolveQuietly((String) targetCombo.getSelectedItem());

        List<String> readableNames = namesFor(vocabulary.listReadables(), targetClass);
        setItems(attr1Combo, readableNames);
        setItems(attr2Combo, readableNames);
        setItems(writableCombo, namesFor(vocabulary.listWritables(), targetClass));
        setItems(commandCombo, namesFor(vocabulary.listCommands(), targetClass));
    }

    /** Names of every vocabulary entry usable for {@code targetClass} — the same check {@link RuleVocabulary} itself uses to validate. */
    private static List<String> namesFor(List<RuleVocabulary.VocabularyEntry> entries, Class<? extends Entity> targetClass) {
        List<String> names = new ArrayList<>();
        for (RuleVocabulary.VocabularyEntry e : entries) {
            if (e.requiredType().isAssignableFrom(targetClass)) {
                names.add(e.name());
            }
        }
        return names;
    }

    private void refreshActionVisibility() {
        boolean mutate = ACTION_MUTATE.equals(actionTypeCombo.getSelectedItem());
        writableCombo.setEnabled(mutate);
        mutationOpCombo.setEnabled(mutate);
        actionValueField.setEnabled(mutate);
        commandCombo.setEnabled(!mutate);
    }

    private static void setItems(JComboBox<String> combo, List<String> items) {
        combo.removeAllItems();
        for (String item : items) {
            combo.addItem(item);
        }
    }

    private static Class<? extends Entity> resolveQuietly(String targetType) {
        try {
            return RuleVocabulary.resolveTargetType(targetType, -1);
        } catch (RuleParseException e) {
            // Unreachable in practice: TARGET_TYPES above is kept identical to the
            // set RuleVocabulary itself resolves, so this name is always known.
            return Entity.class;
        }
    }

    private void doAdd() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            name = "NewRule";
        }
        String targetType = (String) targetCombo.getSelectedItem();

        if (attr1Combo.getSelectedItem() == null) {
            statusLabel.setForeground(new Color(0xB00020));
            statusLabel.setText(targetType + " has no readable attributes to build a condition from.");
            return;
        }

        String condition = attr1Combo.getSelectedItem() + " " + op1Combo.getSelectedItem() + " " + value1Field.getText().trim();
        String connector = (String) connectorCombo.getSelectedItem();
        if (!NO_CONNECTOR.equals(connector) && attr2Combo.getSelectedItem() != null) {
            condition += " " + connector + " " + attr2Combo.getSelectedItem() + " " + op2Combo.getSelectedItem()
                    + " " + value2Field.getText().trim();
        }

        String action;
        if (ACTION_MUTATE.equals(actionTypeCombo.getSelectedItem())) {
            if (writableCombo.getSelectedItem() == null) {
                statusLabel.setForeground(new Color(0xB00020));
                statusLabel.setText(targetType + " has no writable attributes; choose \"Run a command\" instead.");
                return;
            }
            action = writableCombo.getSelectedItem() + " " + mutationOpCombo.getSelectedItem() + " " + actionValueField.getText().trim();
        } else {
            if (commandCombo.getSelectedItem() == null) {
                statusLabel.setForeground(new Color(0xB00020));
                statusLabel.setText(targetType + " has no commands available; choose \"Change an attribute\" instead.");
                return;
            }
            action = (String) commandCombo.getSelectedItem();
        }

        String ruleText = name + " | " + targetType + " | " + condition + " | " + action;
        String error = onRuleBuilt.apply(ruleText);
        if (error == null) {
            statusLabel.setForeground(new Color(0x1B5E20));
            statusLabel.setText("Added and saved: " + ruleText);
        } else {
            statusLabel.setForeground(new Color(0xB00020));
            statusLabel.setText("Not saved: " + error);
        }
    }
}
