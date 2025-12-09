package CanteenRushh;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import CanteenRushh.Customer;


/**
 * CustomerQueue
 * - Manages list of Customer objects
 * - Spawns customers from bottom-right
 * - Assigns waiting slots (stacked upward along the right side)
 * - Moves front customer to counter, handles arrival and patience ticks
 *
 * Usage:
 * - call update(dtSeconds) every tick
 * - call paint(g2d) during paintComponent
 * - call spawnCustomer() to add a new one at the end
 *
 * Events: implement QueueEventListener for life changes or for external reaction when someone leaves.
 */
public class CustomerQueue {
    public interface QueueEventListener {
        void onCustomerLeftAngrily(Customer c);
        void onLivesChanged(int lives);
    }

    private final Rectangle area; // area where queue slots are computed
    private final Point counterPos; // absolute pixel position of counter
    private final List<Customer> customers = new ArrayList<>();
    private final List<Point> slotPositions = new ArrayList<>();
    private final int maxSlots;
    private final QueueEventListener listener;
    private final Random rng = new Random();

    private int nextCustomerId = 1;
    private int lives = 3;
    private final double arrivalThreshold = 8.0; // px to consider arrived at counter

    // timing
    private double globalAccumulator = 0.0;

    // configuration
    private final int slotSpacing = 72; // vertical spacing between waiting slots
    private final int slotOffsetX = -160; // shift left from right edge for slots (was -40)
    private final int spawnOffset = 140; // spawn offset off-screen bottom-right (was 80)


    public CustomerQueue(Rectangle queueArea, Point counterPos, int maxSlots, QueueEventListener listener) {
        this.area = new Rectangle(queueArea);
        this.counterPos = new Point(counterPos);
        this.maxSlots = maxSlots;
        this.listener = listener;
        computeSlots();
    }

    private void computeSlots() {
        slotPositions.clear();
        // Build slots stacked up from bottom-right of 'area'
        int rightX = area.x + area.width + slotOffsetX; // a bit left inside area
        int baseY = area.y + area.height - 8; // bottom inside area
        for (int i = 0; i < maxSlots; i++) {
            int sx = rightX;
            int sy = baseY - i * slotSpacing;
            slotPositions.add(new Point(sx, sy));
        }
    }

    public void setLives(int lives) {
        this.lives = lives;
        if (listener != null) listener.onLivesChanged(this.lives);
    }

    public int getLives() { return lives; }

    /**
     * Spawn a new customer at the end of line (off-screen bottom-right) which will walk into its assigned slot.
     */
    public void spawnCustomer() {
        // spawn position: a bit off bottom-right of area
        int sx = area.x + area.width + spawnOffset;
        int sy = area.y + area.height + spawnOffset;
        Customer c = new Customer(nextCustomerId++, new Point(sx, sy));
        // assign target to the computed slot for last position
        int posIndex = Math.min(customers.size(), maxSlots - 1);
        Point desired = slotPositions.get(posIndex);
        c.setTarget(desired);
        c.setState(Customer.State.WAITING);
        customers.add(c);
    }

    /**
     * Main update to be called every frame.
     * @param dtSeconds seconds elapsed since last call
     */
    public void update(double dtSeconds) {
        if (customers.isEmpty()) return;

        // 1) If no one physically AT_COUNTER, move front-of-line to counter.
        Customer front = customers.get(0);
        boolean someoneAtCounter = false;
        for (Customer c : customers) {
            if (c.getState() == Customer.State.AT_COUNTER) { someoneAtCounter = true; break; }
        }
        if (!someoneAtCounter && front.getState() != Customer.State.MOVING_TO_COUNTER && front.getState() != Customer.State.AT_COUNTER) {
            front.setState(Customer.State.MOVING_TO_COUNTER);
            front.setTarget(counterPos);
        }

        // 2) Update each customer (movement and patience ticks)
        List<Customer> toRemove = new ArrayList<>();
        for (int i = 0; i < customers.size(); i++) {
            Customer c = customers.get(i);

            // Ensure waiting customers have their proper slot target (smooth stepping forward)
            if (c.getState() == Customer.State.WAITING) {
                // their slot index is i (0 is front)
                int slotIndex = Math.min(i, slotPositions.size() - 1);
                Point slot = slotPositions.get(slotIndex);
                c.setTarget(slot);
            }

            c.update(dtSeconds);

            // If moving to counter and has arrived physically, set AT_COUNTER & choose request
            if (c.getState() == Customer.State.MOVING_TO_COUNTER) {
                double dx = c.getTarget().x - c.x;
                double dy = c.getTarget().y - c.y;
                double dist = Math.hypot(dx, dy);
                if (dist <= arrivalThreshold) {
                    c.arriveAtCounterAndChooseRequest();
                }
            }

            // Patience check -> angry leave
            if (c.isAngry()) {
                toRemove.add(c);
            }
        }

        // 3) Handle removals (angry customers)
        for (Customer c : toRemove) {
            customers.remove(c);
            lives = Math.max(0, lives - 1);
            if (listener != null) listener.onCustomerLeftAngrily(c);
            if (listener != null) listener.onLivesChanged(lives);
            // spawn a replacement at the end (so total # in queue remains fairly constant)
            spawnCustomer();
        }

        // 4) If queue longer than slots, extra customers should still have slot assigned at last position
        reassignSlotTargets();
    }

    private void reassignSlotTargets() {
        for (int i = 0; i < customers.size(); i++) {
            Customer c = customers.get(i);
            if (c.getState() == Customer.State.WAITING) {
                int slotIndex = Math.min(i, slotPositions.size() - 1);
                c.setTarget(slotPositions.get(slotIndex));
            }
        }
    }

    /**
     * Mark front-of-line as paid (called by whatever handles Payment).
     * This will set paid = true and the customer can be cleared (or continue)
     */
    public void markFrontPaid() {
        if (customers.isEmpty()) return;
        Customer front = customers.get(0);
        if (front.getState() == Customer.State.AT_COUNTER) {
            front.paid = true;
            // After paying: remove customer (served) after a short delay or immediately:
            customers.remove(0);
            // spawn replacement to keep queue active
            spawnCustomer();
        }
    }

    public void draw(Graphics2D g) {
        // draw slots (optional) as faint markers
        g.setColor(new Color(0,0,0,40));
        for (Point p : slotPositions) {
            g.fillOval(p.x - 8, p.y - 8, 16, 16);
        }
        // draw counter marker
        g.setColor(new Color(0,0,0,80));
        g.fillRect(counterPos.x - 24, counterPos.y - 8, 48, 16);

        // draw customers in back-to-front order for nicer overlap
        for (int i = customers.size() - 1; i >= 0; i--) {
            customers.get(i).draw(g);
        }
    }

    public int getQueueSize() { return customers.size(); }

    // Optional simple initialization helper: spawn n customers
    public void spawnInitial(int n) {
        for (int i = 0; i < n; i++) spawnCustomer();
    }
}
