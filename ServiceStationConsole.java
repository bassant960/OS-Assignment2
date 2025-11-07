import java.util.Queue;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

class Semaphore {
    private int value;
    public Semaphore() {
        value = 0;
    }

    public Semaphore(int val) {
        value = val;
    }

    public synchronized void waiting() throws InterruptedException {
        while (value <= 0) {
            wait();
        }
        value--;
    }

    public synchronized void signal() {
        value++;
        notifyAll();
    }

    public synchronized int get() {
        return value;
    }
}

class Pump extends Thread {
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
                full.waiting(); // wait until at least one car exists
                mutex.waiting(); // enter critical section

                String car;
                try {
                    car = waitingQueue.poll();
                } finally {
                    mutex.signal();
                }
                if (car == null) continue;

                empty.signal(); // one more free slot in waiting area
                pumps.waiting(); // acquire service bay

                synchronized (System.out) {
                    System.out.println("Pump " + pumpId + ": " + car + " Occupied");
                    System.out.println("Pump " + pumpId + ": " + car + " login");
                    System.out.println("Pump " + pumpId + ": " + car + " begins service at Bay " + pumpId);
                }

                try {
                    Thread.sleep(2000);
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
        }
    }
}


class Car implements Runnable {
    private final String carName;
    private final Queue<String> waitingQueue;
    private final Semaphore empty;
    private final Semaphore full;
    private final Semaphore mutex;
    private final Semaphore pumps;
    private static int carsArrived = 0;
    private static final Object arrivalLock = new Object();

    /**
     * Constructor for the Car (Producer) thread.
     * @param name The name/ID of the car.
     * @param queue The shared waiting queue.
     * @param empty The 'empty' semaphore.
     * @param full The 'full' semaphore.
     * @param mutex The 'mutex' semaphore (lock).
     * @param pumps The 'pumps' semaphore to check if all pumps are busy.
     */
    public Car(String name, Queue<String> queue, Semaphore empty, Semaphore full, Semaphore mutex, Semaphore pumps) {
        this.carName = name;
        this.waitingQueue = queue;
        this.empty = empty;
        this.full = full;
        this.mutex = mutex;
        this.pumps = pumps;
    }

    /**
     * Simulates a random arrival time delay.
     */
    private void simulateArrival() {
        try {
            // Simulate random delay before entering the queue
            long sleepTime = ThreadLocalRandom.current().nextInt(100, 300);
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        // Print arrival immediately
        System.out.println(carName + " arrived");

        simulateArrival(); // Simulate arrival delay

        try {
            // Count cars arrived
            synchronized (arrivalLock) {
                carsArrived++;
            }

            // Check if all pumps are busy (pumps semaphore is 0)
            boolean allPumpsBusy = pumps.get() == 0;

            // 1. Wait until a space is available in the waiting area
            empty.waiting();

            // 2. Acquire the lock for exclusive access to the critical section
            mutex.waiting();

            // --- Critical Section: Adding the item to the buffer ---
            waitingQueue.add(carName);

            // Print "arrived and waiting" if all pumps are busy AND we've reached the pump capacity
            if (allPumpsBusy ) {
                System.out.println(carName + " arrived and waiting");
            }
            // --------------------------------------------------------

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(carName + " was interrupted while waiting.");
            return;
        } finally {
            // 3. Release the mutual exclusion lock
            mutex.signal();
        }

        // 4. Signal that a new item (car) is available in the queue
        full.signal();
    }
}

public class ServiceStationConsole {
    private final Queue<String> waitingQueue;
    private final Semaphore mutex;
    private final Semaphore empty;
    private final Semaphore full;
    private final Semaphore pumps;
    private final Pump[] pumpThreads;
    private final int numberOfPumps;

    public ServiceStationConsole(int waitingAreaSize, int numberOfPumps) {
        waitingQueue = new LinkedList<>();
        mutex = new Semaphore(1);
        empty = new Semaphore(waitingAreaSize);
        full = new Semaphore(0);
        pumps = new Semaphore(numberOfPumps);
        this.numberOfPumps = numberOfPumps;

        pumpThreads = new Pump[numberOfPumps];
        for (int i = 0; i < numberOfPumps; i++) {
            pumpThreads[i] = new Pump(i + 1, waitingQueue, mutex, empty, full, pumps);
        }
    }

    public void startPumps() {
        for (Pump pump : pumpThreads) {
            pump.start();
        }
    }

    public void addCar(String carName) {
        new Thread(new Car(carName, waitingQueue, empty, full, mutex, pumps)).start();
    }

    public void shutdown() {
        for (Pump pump : pumpThreads) {
            pump.shutdown();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Waiting area capacity : ");
        int waitingSize = sc.nextInt();

        System.out.print("Number of pumps (>=1): ");
        int numberOfPumps = sc.nextInt();
        sc.nextLine();

        ServiceStationConsole station = new ServiceStationConsole(waitingSize, numberOfPumps);

        System.out.println("Enter car names in arrival order, separated by commas.");
        System.out.print("Cars: ");

        String input = sc.nextLine().trim();
        String[] cars = input.split(",");

        // Start pumps FIRST before adding any cars
        station.startPumps();

        // Add all cars with proper timing
        for (int i = 0; i < cars.length; i++) {
            station.addCar(cars[i].trim());

            // Add strategic delays to ensure proper pump assignment
            if (i < numberOfPumps) {
                try {
                    Thread.sleep(170);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                try {
                    Thread.sleep(500); // Normal delay for subsequent cars
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Wait for all processing to complete - calculate based on number of cars
        int processingTime = Math.max(3000, cars.length * 600);
        try {
            Thread.sleep(processingTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("All cars processed. Station closing.");
        station.shutdown();
        sc.close();
    }
}