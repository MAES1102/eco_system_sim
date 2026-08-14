package com.ecosystem.simulation.events;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A generic, time-ordered queue of discrete simulation events.
 *
 * <h3>OOP concepts demonstrated</h3>
 * <ul>
 *   <li><b>Parametric polymorphism (Generics)</b>: the type parameter
 *       {@code T extends SimulationEvent} enforces that only event objects
 *       can be enqueued, while allowing the queue to be specialised for any
 *       event subtype.  Example:
 *       <pre>EventQueue&lt;SimulationEvent&gt; allEvents = new EventQueue&lt;&gt;();</pre>
 *   </li>
 *   <li><b>Encapsulation</b>: the internal {@link PriorityQueue} is fully hidden;
 *       callers interact only through {@link #enqueue}, {@link #dequeue},
 *       {@link #processAll}, and {@link #processDue}.</li>
 *   <li><b>Method overloading</b>: {@link #processAll()} processes every event
 *       regardless of time; {@link #processAll(EventProcessListener)} additionally
 *       notifies a listener — same name, different parameter lists.</li>
 * </ul>
 *
 * <h3>Discrete-event semantics</h3>
 * Events are ordered by {@link SimulationEvent#getScheduledTime()}.  When two
 * events share the same tick they are dequeued in the order they were inserted
 * (FIFO within a tick), preserving determinism.
 *
 * @param <T> the concrete event type stored in this queue;
 *            must extend {@link SimulationEvent}
 */
public class EventQueue<T extends SimulationEvent> {

    /**
     * Callback interface so external components can react to each processed event
     * without subclassing {@code EventQueue}.
     *
     * <p>Demonstrates the <em>Observer</em> pattern at the event-processing level.</p>
     */
    public interface EventProcessListener {
        /**
         * Called immediately after {@code event.execute()} returns.
         *
         * @param event the event that was just processed
         */
        void onEventProcessed(SimulationEvent event);
    }

    /** Internal min-heap; events with the smallest scheduledTime come first. */
    private final PriorityQueue<T> heap;

    /** Running total of all events ever enqueued (for statistics). */
    private int totalEnqueued;

    /**
     * Creates an empty event queue.
     */
    public EventQueue() {
        this.heap = new PriorityQueue<>();
        this.totalEnqueued = 0;
    }

    /**
     * Adds an event to the queue.
     * The event will be dequeued before any event with a later scheduled time.
     *
     * @param event the event to schedule; must not be {@code null}
     * @throws IllegalArgumentException if {@code event} is {@code null}
     */
    public void enqueue(T event) {
        if (event == null) {
            throw new IllegalArgumentException("Cannot enqueue a null event");
        }
        heap.offer(event);
        totalEnqueued++;
    }

    /**
     * Removes and returns the next event (earliest scheduled time).
     *
     * @return the next event, or {@code null} if the queue is empty
     */
    public T dequeue() {
        return heap.poll();
    }

    /**
     * Returns (without removing) the next event.
     *
     * @return the next event, or {@code null} if the queue is empty
     */
    public T peek() {
        return heap.peek();
    }

    /**
     * Processes and removes <em>all</em> events currently in the queue,
     * calling {@link SimulationEvent#execute()} on each in scheduled-time order.
     *
     * <p><b>Overload 1 of 2</b> — no listener notification.</p>
     */
    public void processAll() {
        processAll(null);
    }

    /**
     * Processes and removes <em>all</em> events currently in the queue,
     * calling {@link SimulationEvent#execute()} on each and notifying
     * {@code listener} after each execution.
     *
     * <p><b>Overload 2 of 2</b> — demonstrates <em>method overloading</em>
     * (same name, different parameter signature).</p>
     *
     * @param listener optional post-execution callback; {@code null} is accepted
     */
    public void processAll(EventProcessListener listener) {
        while (!heap.isEmpty()) {
            T event = heap.poll();
            event.execute();
            if (listener != null) {
                listener.onEventProcessed(event);
            }
        }
    }

    /**
     * Processes and removes only the events whose scheduled time is
     * &le; {@code currentTime}, leaving future events in the queue.
     *
     * <p>This is the classic DES "advance simulation time to the next event"
     * operation — the simulation clock advances to {@code currentTime} and
     * all events that have "arrived" are fired.</p>
     *
     * @param currentTime the current simulation tick
     * @return list of events that were processed (for logging / testing)
     */
    public List<T> processDue(int currentTime) {
        List<T> processed = new ArrayList<>();
        while (!heap.isEmpty() && heap.peek().getScheduledTime() <= currentTime) {
            T event = heap.poll();
            event.execute();
            processed.add(event);
        }
        return processed;
    }

    /**
     * Returns whether the queue contains no pending events.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return heap.isEmpty();
    }

    /**
     * Returns the number of events currently waiting in the queue.
     *
     * @return pending event count
     */
    public int size() {
        return heap.size();
    }

    /**
     * Returns the total number of events ever enqueued (including already-processed ones).
     * Useful for throughput statistics.
     *
     * @return cumulative enqueue count
     */
    public int getTotalEnqueued() {
        return totalEnqueued;
    }

    /**
     * Discards all pending events without executing them.
     * Typically used when the simulation is reset.
     */
    public void clear() {
        heap.clear();
    }
}
