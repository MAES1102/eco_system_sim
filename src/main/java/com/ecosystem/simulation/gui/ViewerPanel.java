package com.ecosystem.simulation.gui;

import com.ecosystem.simulation.simulation.World;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that displays the world visualization.
 * Reuses WorldPanel logic for drawing entities.
 * 
 * @author Ecosystem Simulation Team
 * @version 1.0
 */
public class ViewerPanel extends JPanel {
    
    private WorldPanel worldPanel;
    
    public ViewerPanel(World world) {
        setLayout(new BorderLayout());
        worldPanel = new WorldPanel(world);
        
        add(worldPanel, BorderLayout.CENTER);
        setPreferredSize(new Dimension(500, 500));
    }
    
    public void setWorld(World world) {
        worldPanel.setWorld(world);
    }
    
    public void refresh() {
        worldPanel.repaint();
    }
}