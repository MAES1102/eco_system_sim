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

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int availableWidth = getWidth();
        int availableHeight = getHeight() - 30;
        int worldWidth = world.getWidth();
        int worldHeight = world.getHeight();

        int scaleX = availableWidth / worldWidth;
        int scaleY = availableHeight / worldHeight;
        cellSize = Math.min(scaleX, scaleY);
        cellSize = Math.max(cellSize, 8);

        g2.setColor(Color.LIGHT_GRAY);
        for (int x = 0; x <= worldWidth; x++) {
            g2.drawLine(x * cellSize, 0, x * cellSize, worldHeight * cellSize);
        }
        for (int y = 0; y <= worldHeight; y++) {
            g2.drawLine(0, y * cellSize, worldWidth * cellSize, y * cellSize);
        }

        List<Entity> entities = world.getEntities();
        for (Entity entity : entities) {
            if (entity.isAlive()) {
                drawEntity(g2, entity);
            }
        }

        drawLegend(g2, worldHeight * cellSize + 5);
    }

    /**
     * Species are told apart by shape as well as color (predator = triangle,
     * herbivore = circle, plant = square), not color alone.
     */
    private void drawEntity(Graphics2D g2, Entity entity) {
        int x = entity.getX() * cellSize;
        int y = entity.getY() * cellSize;
        int size = cellSize - 2;
        Color color = getColorForEntity(entity);

        if (entity instanceof Predator) {
            drawTriangle(g2, x + 1, y + 1, size, color);
        } else if (entity instanceof Herbivore) {
            drawCircle(g2, x + 1, y + 1, size, color);
        } else if (entity instanceof Plant) {
            drawSquare(g2, x + 1, y + 1, size, color);
        } else {
            drawCircle(g2, x + 1, y + 1, size, color);
        }
    }

    private void drawTriangle(Graphics2D g2, int x, int y, int size, Color color) {
        int[] xs = {x + size / 2, x, x + size};
        int[] ys = {y, y + size, y + size};
        g2.setColor(color);
        g2.fillPolygon(xs, ys, 3);
        g2.setColor(color.darker());
        g2.drawPolygon(xs, ys, 3);
    }

    private void drawCircle(Graphics2D g2, int x, int y, int size, Color color) {
        g2.setColor(color);
        g2.fillOval(x, y, size, size);
        g2.setColor(color.darker());
        g2.drawOval(x, y, size, size);
    }

    private void drawSquare(Graphics2D g2, int x, int y, int size, Color color) {
        g2.setColor(color);
        g2.fillRoundRect(x, y, size, size, 3, 3);
        g2.setColor(color.darker());
        g2.drawRoundRect(x, y, size, size, 3, 3);
    }

    private void drawLegend(Graphics2D g2, int y) {
        g2.setFont(new Font("Arial", Font.PLAIN, 11));

        int x = 10;
        int swatchSize = 10;

        drawTriangle(g2, x, y, swatchSize, Color.RED);
        g2.setColor(Color.BLACK);
        g2.drawString("Predator", x + swatchSize + 5, y + swatchSize - 1);

        x += 100;
        drawCircle(g2, x, y, swatchSize, Color.GREEN);
        g2.setColor(Color.BLACK);
        g2.drawString("Herbivore", x + swatchSize + 5, y + swatchSize - 1);

        x += 100;
        drawSquare(g2, x, y, swatchSize, Color.BLUE);
        g2.setColor(Color.BLACK);
        g2.drawString("Plant", x + swatchSize + 5, y + swatchSize - 1);
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