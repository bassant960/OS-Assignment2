import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

public class Car implements Runnable {

    private final String carName;
    private final Queue<String> waitingQueue; // The shared resource (Bounded Buffer)
    private final Semaphore empty;          // Represents available slots (Car checks this)
    private final Semaphore full;           // Represents occupied slots (Car increments this)
    private final Semaphore mutex;          // The lock for the critical section

    /**
     * Constructor for the Car (Producer) thread.
     * @param name The name/ID of the car.
     * @param queue The shared waiting queue.
     * @param empty The 'empty' semaphore.
     * @param full The 'full' semaphore.
     * @param mutex The 'mutex' semaphore (lock).
     */
    public Car(String name, Queue<String> queue, Semaphore empty, Semaphore full, Semaphore mutex) {
        this.carName = name;
        this.waitingQueue = queue;
        this.empty = empty;
        this.full = full;
        this.mutex = mutex;
    }

    /**
     * Simulates a random arrival time delay.
     */
    private void simulateArrival() {
        try {
            // Simulate random delay (e.g., 0.5 to 2 seconds) before entering the queue
            long sleepTime = ThreadLocalRandom.current().nextInt(500, 2000);
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            // Restore interrupt status
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        System.out.println(carName + " arrived [cite: 18]");
        simulateArrival(); // Simulate arrival delay

        try {
            // 1. Wait until a space is available in the waiting area (queue).
            // If 'empty' is 0, the thread blocks, fulfilling the requirement:
            // "If the waiting area is full, the car must wait until space is available." [cite: 10]
            System.out.println(carName + " is checking for queue space...");
            empty.waiting();

            // 2. Acquire the lock (mutex) for exclusive access to the critical section.
            mutex.waiting();

            // --- Critical Section: Adding the item to the buffer ---
            if (waitingQueue.size() < empty.get() + 1) { // Basic safety check
                waitingQueue.add(carName);
                System.out.println(carName + " enters the queue[cite: 19]. Queue size: " + waitingQueue.size());
            }
            // --------------------------------------------------------

        } catch (InterruptedException e) {
            // Handle thread interruption
            Thread.currentThread().interrupt();
            System.err.println(carName + " was interrupted while waiting.");
            return; // Exit the run method
        } finally {
            // 3. Release the mutual exclusion lock, even if an exception occurred in the critical section.
            mutex.signal();
        }

        // 4. Signal that a new item (car) is available in the queue.
        // This increments the 'full' count, potentially waking up a Pump (Consumer) thread.
        full.signal();
    }
}