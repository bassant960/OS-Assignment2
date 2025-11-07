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
    private final Queue<String> waitingQueue; // The shared resource (Bounded Buffer)
    private final Semaphore empty; // Represents available slots (Car checks this)
    private final Semaphore full;  // Represents occupied slots (Car increments this)
    private final Semaphore mutex; // The lock for the critical section
    private static int carsArrived = 0;
    private static final Object arrivalLock = new Object();

    public Car(String name, Queue<String> queue, Semaphore empty, Semaphore full, Semaphore mutex) {
        this.carName = name;
        this.waitingQueue = queue;
        this.empty = empty;
        this.full = full;
        this.mutex = mutex;
    }

    @Override
    public void run() {
        // Show arrival immediately
        synchronized (System.out) {
            System.out.println(carName + " arrived");
        }

        // Simulate arrival timing
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(100, 300));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            empty.waiting();

            mutex.waiting();
            try {
                // Count how many cars have arrived so far
                synchronized (arrivalLock) {
                    carsArrived++;
                }

                boolean shouldShowWaiting = carsArrived >= 4;
                waitingQueue.add(carName);

                if (shouldShowWaiting) {
                    synchronized (System.out) {
                        System.out.println(carName + " arrived and waiting");
                    }
                }
            } finally {
                mutex.signal();
            }

            full.signal();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

public class ServiceStation {
    private final Queue<String> waitingQueue;
    private final Semaphore mutex;
    private final Semaphore empty;
    private final Semaphore full;
    private final Semaphore pumps;
    private final Pump[] pumpThreads;

    public ServiceStation(int waitingAreaSize, int numberOfPumps) {
        waitingQueue = new LinkedList<>();
        mutex = new Semaphore(1);
        empty = new Semaphore(waitingAreaSize);
        full = new Semaphore(0);
        pumps = new Semaphore(numberOfPumps);

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
        new Thread(new Car(carName, waitingQueue, empty, full, mutex)).start();
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

        ServiceStation station = new ServiceStation(waitingSize, numberOfPumps);

        System.out.println("Enter car names in arrival order, separated by commas.");
        System.out.print("Cars: ");

        String input = sc.nextLine().trim();
        String[] cars = input.split(",");

        //Add all cars first
        for (String car : cars) {
            station.addCar(car.trim());
            try {
                Thread.sleep(500); // Delay between arrivals
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Wait for all arrivals to complete
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Start pumps AFTER all arrivals are shown
        station.startPumps();

        // Wait for all processing to complete
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("All cars processed. Station closing.");
        station.shutdown();
        sc.close();
    }
}