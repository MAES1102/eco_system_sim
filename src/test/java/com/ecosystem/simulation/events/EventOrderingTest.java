package com.ecosystem.simulation.events;

import com.ecosystem.simulation.entities.Herbivore;
import org.junit.jupiter.api.Test;

import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the future-event-list ordering contract: events sort strictly by
 * {@code scheduledTime}, and ties at the same {@code scheduledTime} are broken
 * deterministically by insertion order (a monotonic sequence number) rather
 * than relying on {@code PriorityQueue}'s unspecified tie behavior.
 */
class EventOrderingTest {

    private static SimulationEvent movementEventAt(int time) {
        return new MovementEvent(time, new Herbivore(0, 0, 50, 1.5, 5), 0, 0, 1, 1);
    }

    @Test
    void earlierScheduledTime_sortsFirst() {
        SimulationEvent early = movementEventAt(1);
        SimulationEvent late = movementEventAt(5);

        assertTrue(early.compareTo(late) < 0);
        assertTrue(late.compareTo(early) > 0);
    }

    @Test
    void sameScheduledTime_breaksTieByInsertionOrder() {
        SimulationEvent first = movementEventAt(3);
        SimulationEvent second = movementEventAt(3);

        assertTrue(first.compareTo(second) < 0, "the event constructed first must sort first among same-time ties");
        assertTrue(second.compareTo(first) > 0);
    }

    @Test
    void priorityQueue_dequeuesInDeterministicOrder_evenWithManyTies() {
        EventQueue<SimulationEvent> queue = new EventQueue<>();
        // Enqueue 20 events at the same scheduledTime, in a known order.
        SimulationEvent[] inOrder = new SimulationEvent[20];
        for (int i = 0; i < inOrder.length; i++) {
            inOrder[i] = movementEventAt(7);
            queue.enqueue(inOrder[i]);
        }

        for (SimulationEvent expected : inOrder) {
            assertEquals(expected, queue.dequeue(), "events scheduled at the same tick must dequeue in the exact order they were enqueued");
        }
    }

    @Test
    void eventQueue_dequeuesAcrossDifferentTimes_inTimeOrder() {
        EventQueue<SimulationEvent> queue = new EventQueue<>();
        SimulationEvent t5 = movementEventAt(5);
        SimulationEvent t1 = movementEventAt(1);
        SimulationEvent t3 = movementEventAt(3);
        queue.enqueue(t5);
        queue.enqueue(t1);
        queue.enqueue(t3);

        assertEquals(t1, queue.dequeue());
        assertEquals(t3, queue.dequeue());
        assertEquals(t5, queue.dequeue());
    }
}
