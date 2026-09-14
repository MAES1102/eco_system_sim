package com.ecosystem.simulation;

import com.ecosystem.simulation.entities.Animal;
import com.ecosystem.simulation.entities.Entity;
import com.ecosystem.simulation.entities.Movable;
import com.ecosystem.simulation.entities.Organism;
import com.ecosystem.simulation.entities.Predator;
import com.ecosystem.simulation.entities.Reproducible;
import com.ecosystem.simulation.gui.MainWindow;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Main entry point for the Ecosystem Simulation.
 * Launches the integrated desktop application with GUI.
 */
public class Main {

    public static void main(String[] args) {
        demonstrateMultityping();
        useNimbusLookAndFeel();

        // Launch the integrated desktop application
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainWindow mainWindow = new MainWindow();
                mainWindow.setVisible(true);
                mainWindow.setExtendedState(JFrame.MAXIMIZED_BOTH);
                mainWindow.toFront();
                mainWindow.requestFocus();
            }
        });
    }

    /**
     * Switches every Swing component to the Nimbus look and feel instead of
     * the very dated default. Nimbus is not guaranteed to be registered under
     * a fixed class name across JDK vendors, so it is looked up by name among
     * the installed look-and-feels; if it is unavailable for any reason, the
     * platform default is left in place rather than blocking the simulation.
     */
    private static void useNimbusLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            // Nimbus unavailable -- keep the platform default look and feel.
        }
    }

    /**
     * Client-side proof of subtyping/multityping: a single {@code Predator}
     * object is referenced through five different declared types, and a
     * type-specific method is invoked through each reference. The object
     * itself is created exactly once — only the compile-time type of the
     * variable observing it changes, which is what distinguishes multityping
     * from simply creating five separate objects.
     */
    private static void demonstrateMultityping() {
        Predator predator = new Predator(0, 0, 80, 2.0, 7);

        Entity entityRef = predator;               // by inheritance (Predator -> ... -> Entity)
        Organism organismRef = predator;            // by inheritance (Predator -> ... -> Organism)
        Animal animalRef = predator;                // by inheritance (Predator -> Animal)
        Movable movableRef = predator;              // by interface (Animal implements Movable)
        Reproducible reproducibleRef = predator;    // by interface (Predator implements Reproducible)

        System.out.println("Multityping demonstration: one Predator instance, five reference types");
        System.out.println("  as Entity       -> id=" + entityRef.getId() + ", alive=" + entityRef.isAlive());
        System.out.println("  as Organism     -> energy=" + organismRef.getEnergy());
        System.out.println("  as Animal       -> speed=" + animalRef.getSpeed());
        System.out.println("  as Movable      -> getSpeed() via interface=" + movableRef.getSpeed());
        System.out.println("  as Reproducible -> " + reproducibleRef.getClass().getSimpleName() + " implements reproduce()");
        System.out.println("  identity check  -> entityRef == animalRef: " + (entityRef == animalRef));
    }
}