package com.ecosystem.simulation.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.*;

import com.ecosystem.simulation.simulation.SimulationEventListener;

/**
 * Panel that displays simulation events in real-time.
 * Shows rule triggers, entity movements, and other significant events.
 *
 * <p>Implements {@link SimulationEventListener} so that it can be passed into
 * the domain stack ({@code SimulationEngine} → {@code Entity} subclasses) as a
 * plain interface reference.  This removes any Swing dependency from the domain
 * model while preserving identical runtime behaviour.</p>
 */
public class EventLogPanel extends JPanel implements SimulationEventListener {
    
    private JTextArea logArea;
    private SimpleDateFormat timeFormat;
    
    public EventLogPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Event Log"));
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setBackground(new Color(250, 250, 250));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(600, 100));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        add(scrollPane, BorderLayout.CENTER);
        
        timeFormat = new SimpleDateFormat("HH:mm:ss");
        
        logEvent("Event Log initialized.");
    }
    
    public void logRuleTriggered(String ruleDescription, String entityType) {
        String message = "Rule triggered: " + ruleDescription + " (" + entityType + ")";
        logEvent(message);
    }
    
    public void logEntityEvent(String eventType, String entityType) {
        String message = entityType + " " + eventType;
        logEvent(message);
    }

    /**
     * Satisfies {@link SimulationEventListener}.
     * Delegates to {@link #logEntityEvent} so the existing log format is unchanged.
     */
    @Override
    public void onEntityEvent(String event, String entityType) {
        logEntityEvent(event, entityType);
    }
    
    public void logEvent(String message) {
        String timestamp = timeFormat.format(new Date());
        String logEntry = "[" + timestamp + "]\n" + message;
        
        logArea.append(logEntry + "\n\n");
        
        // Auto-scroll to bottom
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    
    public void clear() {
        logArea.setText("");
        logEvent("Event Log cleared.");
    }
}