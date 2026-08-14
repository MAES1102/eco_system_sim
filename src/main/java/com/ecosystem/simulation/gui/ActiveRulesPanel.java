package com.ecosystem.simulation.gui;

import com.ecosystem.simulation.rules.Rule;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Panel that displays currently active rules in a human-readable format.
 * Shows rules as "Target Field Operator Value → Action"
 * 
 * @author Ecosystem Simulation Team
 * @version 1.0
 */
public class ActiveRulesPanel extends JPanel {
    
    private JTextArea rulesTextArea;
    
    public ActiveRulesPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Active Rules"));
        
        rulesTextArea = new JTextArea();
        rulesTextArea.setEditable(false);
        rulesTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        rulesTextArea.setBackground(new Color(240, 240, 240));
        
        JScrollPane scrollPane = new JScrollPane(rulesTextArea);
        scrollPane.setPreferredSize(new Dimension(200, 150));
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    public void updateRules(List<Rule> rules) {
        StringBuilder sb = new StringBuilder();
        
        if (rules == null || rules.isEmpty()) {
            sb.append("No active rules.");
        } else {
            for (int i = 0; i < rules.size(); i++) {
                Rule rule = rules.get(i);
                sb.append("✓ ");
                sb.append(rule.getTargetType());
                sb.append(" ");
                sb.append(rule.getCondition());
                sb.append(" → ");
                sb.append(rule.getAction());
                
                if (i < rules.size() - 1) {
                    sb.append("\n");
                }
            }
        }
        
        rulesTextArea.setText(sb.toString());
    }
}