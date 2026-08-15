package com.ecosystem.simulation.entities;

import com.ecosystem.simulation.events.DeathEvent;
import com.ecosystem.simulation.events.SchedulingContext;
import com.ecosystem.simulation.simulation.SimulationEventListener;
import com.ecosystem.simulation.simulation.World;
import com.ecosystem.simulation.statistics.Statistics;

/**
 * Abstract base class for all entities in the ecosystem simulation.
 *
 * Key OOP Principles Demonstrated:
 * - Abstraction: Abstract class with abstract method update()
 * - Encapsulation: Protected fields with public methods
 * - Inheritance: Base class for all entity types
 */
public abstract class Entity {

    protected int id;
    protected int x;
    protected int y;
    protected boolean alive;

    private static int nextId = 1;

    /**
     * True once something has scheduled this entity's removal (a {@link DeathEvent}),
     * but before that event has actually executed. While {@code true}, this entity
     * must not be selected as prey, allowed to reproduce, move, be evaluated against
     * rules, or have a further activity event scheduled for it — see
     * {@link #scheduleRemoval} and {@code EntityActivityEvent.execute}.
     */
    private boolean pendingRemoval = false;

    public Entity(int x, int y) {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Coordinates cannot be negative: x=" + x + ", y=" + y);
        }
        this.id = nextId++;
        this.x = x;
        this.y = y;
        this.alive = true;
    }

    /**
     * Defines the behavior of an entity during each simulation step.
     * Called from inside {@code EntityActivityEvent.execute()} — routine aging,
     * energy consumption, growth, movement, and decision-making are legitimately
     * owned by that activity event, since it is itself the causal discrete event.
     */
    public abstract void update();

    public boolean isAlive() {
        return this.alive;
    }

    /**
     * Marks this entity as dead. The <b>only</b> legitimate caller in the running
     * simulation is {@link DeathEvent#execute}, which is the single owner of the
     * "this entity is dead" mutation regardless of cause (starvation, old age,
     * predation, grazing, or a user rule's {@code die} command). Any other code
     * that wants an entity dead must call {@link #scheduleRemoval(String)} instead.
     */
    public void die() {
        this.alive = false;
    }

    public int getId() { return this.id; }
    public int getX() { return this.x; }
    public int getY() { return this.y; }

    /**
     * Moves this entity to a new position, clamped to the owning {@link World}'s
     * bounds (falls back to a 50x50 default if no world is set yet, e.g. in a
     * bare unit test), so a configured map size genuinely propagates into
     * movement clamping rather than a hardcoded literal.
     */
    public void moveTo(int newX, int newY) {
        int maxX = (world != null) ? world.getWidth() - 1 : 49;
        int maxY = (world != null) ? world.getHeight() - 1 : 49;
        this.x = Math.max(0, Math.min(maxX, newX));
        this.y = Math.max(0, Math.min(maxY, newY));
    }

    public String getPosition() {
        return "(" + x + ", " + y + ")";
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "#" + id + " at " + getPosition();
    }

    protected World world;
    protected Statistics statistics;

    public void setWorld(World world) { this.world = world; }
    public World getWorld() { return this.world; }

    public void setStatistics(Statistics statistics) { this.statistics = statistics; }
    public Statistics getStatistics() { return this.statistics; }

    protected SimulationEventListener simulationEventListener;

    public void setSimulationEventListener(SimulationEventListener simulationEventListener) {
        this.simulationEventListener = simulationEventListener;
    }

    public SimulationEventListener getSimulationEventListener() {
        return this.simulationEventListener;
    }

    /**
     * Current simulation tick, kept in sync by {@code EntityActivityEvent}/the
     * engine so entities can timestamp events they schedule.
     */
    protected int simulationTime = 0;

    public void setSimulationTime(int time) { this.simulationTime = time; }
    public int getSimulationTime() { return this.simulationTime; }

    /**
     * The scheduling seam used to enqueue discrete events (predation, reproduction,
     * removal). Replaces the earlier raw {@code EventQueue} reference: entities never
     * enqueue directly onto a shared queue — they always go through this narrow contract.
     */
    protected SchedulingContext schedulingContext;

    public void setSchedulingContext(SchedulingContext schedulingContext) {
        this.schedulingContext = schedulingContext;
    }

    public SchedulingContext getSchedulingContext() {
        return this.schedulingContext;
    }

    public boolean isPendingRemoval() {
        return this.pendingRemoval;
    }

    /**
     * Requests that this entity be removed from the simulation, regardless of cause.
     *
     * <p>This is the single, unified entry point for "this entity should die" —
     * organic causes (starvation via {@link com.ecosystem.simulation.entities.Organism#consumeEnergy},
     * old age, predation) and rule-triggered {@code die} commands all call this
     * instead of {@link #die()} directly. Sets {@code pendingRemoval} immediately
     * (synchronously guarding against duplicate scheduling) and schedules a
     * {@link DeathEvent}, which is the only place {@link #die()} is ever actually
     * invoked, statistics are recorded, and the entity is removed from the world.</p>
     *
     * <p>If no {@link SchedulingContext} is wired (e.g. a bare entity constructed
     * directly in a unit test, outside a running simulation), this degrades to
     * calling {@link #die()} immediately, for testability — in the real running
     * simulation every entity is always wired by {@code EntityFactory}, so
     * production code always goes through the {@link DeathEvent} path.</p>
     *
     * @param cause short human-readable cause, e.g. {@code "starvation"}, {@code "predation"}
     */
    public void scheduleRemoval(String cause) {
        if (!alive || pendingRemoval) {
            return;
        }
        pendingRemoval = true;
        if (schedulingContext != null) {
            schedulingContext.schedule(new DeathEvent(schedulingContext.getClock(), this, cause));
        } else {
            die();
        }
    }
}
