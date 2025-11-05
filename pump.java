import java.util.Queue;

public class Pump extends Thread {
    private final int pumpId;
    private final Queue<String> waitingQueue; // shared queue of car names
    private final Semaphore mutex;
    private final Semaphore empty;
    private final Semaphore full;
    private final Semaphore pumps;
    private volatile boolean running = true;

    public Pump(int pumpId, Queue<String> waitingQueue, Semaphore mutex, Semaphore empty, Semaphore full, Semaphore pumps) {
        this.pumpId = pumpId;
        this.waitingQueue = waitingQueue;
        this.mutex = mutex;
        this.empty = empty;
        this.full = full;
        this.pumps = pumps;
        setName("Pump " + pumpId);
    }

    public void shutdown() {
        running = false;
        interrupt();
    }

    @Override
    public void run() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                full.waiting();   // wait until at least one car exists
                mutex.waiting();  // enter critical section

                String car;
                try {
                    car = waitingQueue.poll();
                } finally {
                    mutex.signal();
                }
                if (car == null) continue;

                empty.signal();     // one more free slot in waiting area

                synchronized (System.out) {
                    System.out.println("Pump " + pumpId + ": " + car + " Occupied");
                }

                pumps.waiting(); // acquire service bay

                synchronized (System.out) {
                    System.out.println("Pump " + pumpId + ": " + car + " login");
                    System.out.println("Pump " + pumpId + ": " + car + " begins service at Bay " + pumpId);
                }

                try {
                    Thread.sleep(1000); // fixed service time for clearer output
                } catch (InterruptedException e) {
                    pumps.signal();
                    Thread.currentThread().interrupt();
                    break;
                }

                synchronized (System.out) {
                    System.out.println("Pump " + pumpId + ": " + car + " finishes service");
                    System.out.println("Pump " + pumpId + ": Bay " + pumpId + " is now free");
                }
                pumps.signal(); // release bay
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            synchronized (System.out) {
                System.out.println("Pump " + pumpId + " shutting down.");
            }
        }
    }
}
