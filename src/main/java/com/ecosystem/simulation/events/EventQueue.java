package com.ecosystem.simulation.events;

import java.util.PriorityQueue;

/**
 * A generic, time-ordered future-event-list (FEL) of discrete simulation events.
 *
 * <h3>OOP concepts demonstrated</h3>
 * <ul>
 *   <li><b>Parametric polymorphism (Generics)</b>: the type parameter
 *       {@code T extends SimulationEvent} enforces that only event objects
 *       can be enqueued, while allowing the queue to be specialised for any
 *       event subtype.</li>
 *   <li><b>Encapsulation</b>: the internal {@link PriorityQueue} is fully hidden;
 *       callers interact only through {@link #enqueue}, {@link #dequeue}, and
 *       {@link #peek}.</li>
 * </ul>
 *
 * <p>{@link com.ecosystem.simulation.simulation.SimulationEngine} is the only
 * consumer: it pops one event at a time via {@link #dequeue()}, adopts that
 * event's own {@code scheduledTime} as its clock, and calls
 * {@code event.execute(this)} — the actual FEL-driving loop lives in the
 * engine, not here, so this class stays a small, generic, reusable priority
 * queue rather than growing simulation-specific control flow.</p>
 *
 * @param <T> the concrete event type stored in this queue; must extend {@link SimulationEvent}
 */
public class EventQueue<T extends SimulationEvent> {

    private final PriorityQueue<T> heap;
    private int totalEnqueued;

    public EventQueue() {
        this.heap = new PriorityQueue<>();
        this.totalEnqueued = 0;
    }

    /**
     * Adds an event to the queue. The event will be dequeued before any event
     * with a later scheduled time (ties broken deterministically — see
     * {@link SimulationEvent#compareTo}).
     *
     * @throws IllegalArgumentException if {@code event} is {@code null}
     */
    public void enqueue(T event) {
        if (event == null) {
            throw new IllegalArgumentException("Cannot enqueue a null event");
        }
        heap.offer(event);
        totalEnqueued++;
    }

    /** Removes and returns the next event (earliest scheduled time), or {@code null} if empty. */
    public T dequeue() {
        return heap.poll();
    }

    /** Returns (without removing) the next event, or {@code null} if empty. */
    public T peek() {
        return heap.peek();
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }

    public int size() {
        return heap.size();
    }

    /** Total number of events ever enqueued (including already-processed ones), for throughput statistics. */
    public int getTotalEnqueued() {
        return totalEnqueued;
    }

    /** Discards all pending events without executing them. Used when the simulation is reset. */
    public void clear() {
        heap.clear();
    }
}
