package com.ecosystem.simulation.gui;

import com.ecosystem.simulation.entities.Entity;
import com.ecosystem.simulation.entities.Herbivore;
import com.ecosystem.simulation.entities.Plant;
import com.ecosystem.simulation.entities.Predator;
import com.ecosystem.simulation.simulation.World;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Panel that displays the world visualization.
 * Draws entities as colored circles based on their type.
 */
public class WorldPanel extends JPanel {
    
    private World world;
    private int cellSize = 10;
    
    public WorldPanel(World world) {
        this.world = world;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(
            world.getWidth() * cellSize,
            world.getHeight() * cellSize + 30
        ));
    }
    
    public void setWorld(World world) {
        this.world = world;
        setPreferredSize(new Dimension(
            world.getWidth() * cellSize,
            world.getHeight() * cellSize + 30
        ));
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (world == null) {
            return;
        }
        
        int availableWidth = getWidth();
        int availableHeight = getHeight() - 30;
        int worldWidth = world.getWidth();
        int worldHeight = world.getHeight();
        
        int scaleX = availableWidth / worldWidth;
        int scaleY = availableHeight / worldHeight;
        cellSize = Math.min(scaleX, scaleY);
        cellSize = Math.max(cellSize, 8);
        
        g.setColor(Color.LIGHT_GRAY);
        for (int x = 0; x <= worldWidth; x++) {
            g.drawLine(x * cellSize, 0, x * cellSize, worldHeight * cellSize);
        }
        for (int y = 0; y <= worldHeight; y++) {
            g.drawLine(0, y * cellSize, worldWidth * cellSize, y * cellSize);
        }
        
        List<Entity> entities = world.getEntities();
        for (Entity entity : entities) {
            if (entity.isAlive()) {
                drawEntity(g, entity);
            }
        }
        
        drawLegend(g, worldHeight * cellSize + 5);
    }
    
    private void drawEntity(Graphics g, Entity entity) {
        int x = entity.getX() * cellSize;
        int y = entity.getY() * cellSize;
        int size = cellSize - 2;
        
        Color color = getColorForEntity(entity);
        g.setColor(color);
        g.fillOval(x + 1, y + 1, size, size);
    }
    
    private void drawLegend(Graphics g, int y) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        
        int x = 10;
        int circleSize = 10;
        
        g.setColor(Color.RED);
        g.fillOval(x, y, circleSize, circleSize);
        g.setColor(Color.BLACK);
        g.drawString("Predator", x + circleSize + 5, y + circleSize - 1);
        
        x += 100;
        g.setColor(Color.GREEN);
        g.fillOval(x, y, circleSize, circleSize);
        g.setColor(Color.BLACK);
        g.drawString("Herbivore", x + circleSize + 5, y + circleSize - 1);
        
        x += 100;
        g.setColor(Color.BLUE);
        g.fillOval(x, y, circleSize, circleSize);
        g.setColor(Color.BLACK);
        g.drawString("Plant", x + circleSize + 5, y + circleSize - 1);
    }
    
    private Color getColorForEntity(Entity entity) {
        if (entity instanceof Predator) {
            return Color.RED;
        } else if (entity instanceof Herbivore) {
            return Color.GREEN;
        } else if (entity instanceof Plant) {
            return Color.BLUE;
        } else {
            return Color.GRAY;
        }
    }
}