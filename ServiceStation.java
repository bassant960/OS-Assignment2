import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.JOptionPane;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

class GUI extends JFrame {

    private JTextArea logArea;
    private JLabel queueLabel, pumpsLabel;
    private JPanel pumpsPanel;
    private final Map<Integer, JLabel> pumpLabels = new HashMap<>();
     
    public int activePumps = 0; 
    public synchronized void incrementActivePumps() {
        activePumps++;
        updatePumpCount(activePumps);
    }
    
    
    public synchronized void decrementActivePumps() {
        if (activePumps > 0) activePumps--;
        updatePumpCount(activePumps);
    }
    

    public GUI(int numPumps) {
        setTitle("Service Station Dashboard");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(240, 240, 240));
       

        JLabel header = new JLabel("Car Wash Simulation", SwingConstants.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 22));
        header.setForeground(new Color(40, 60, 90));
        add(header, BorderLayout.NORTH);

        
        JPanel statusPanel = new JPanel(new GridLayout(1, 2, 20, 10));
        statusPanel.setBackground(new Color(240, 240, 240));

        queueLabel = createStatusLabel("Queue: 0", new Color(70, 130, 180));
        pumpsLabel = createStatusLabel("Active Pumps: 0", new Color(46, 139, 87));
        statusPanel.add(queueLabel);
        statusPanel.add(pumpsLabel);

        add(statusPanel, BorderLayout.SOUTH);

        
        pumpsPanel = new JPanel(new GridLayout(1, numPumps, 10, 10));
        pumpsPanel.setBackground(new Color(250, 250, 250));
        pumpsPanel.setBorder(BorderFactory.createTitledBorder("Pumps Status"));

        for (int i = 1; i <= numPumps; i++) {
            JLabel pumpLabel = createPumpLabel(i);
            pumpLabels.put(i, pumpLabel);
            pumpsPanel.add(pumpLabel);
        }

        add(pumpsPanel, BorderLayout.CENTER);

        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setBackground(new Color(250, 250, 250));
        logArea.setForeground(new Color(50, 50, 50));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("System Log"));
        logScroll.setPreferredSize(new Dimension(750, 180));
        add(logScroll, BorderLayout.EAST);

        setVisible(true);
    }

    private JLabel createStatusLabel(String text, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 20));
        label.setForeground(Color.WHITE);
        label.setOpaque(true);
        label.setBackground(color);
        label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return label;
    }

    private JLabel createPumpLabel(int id) {
        JLabel label = new JLabel("Pump " + id + " [FREE]", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setOpaque(true);
        label.setBackground(new Color(144, 238, 144)); // أخضر فاضي
        label.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        return label;
    }

    
    public void updateQueueSize(int size) {
        SwingUtilities.invokeLater(() ->
                queueLabel.setText("Queue: " + size)
        );
    }

    public void updatePumpCount(int active) {
        SwingUtilities.invokeLater(() ->
                pumpsLabel.setText("Active Pumps: " + active)
        );
    }

    public void setPumpBusy(int pumpId, String carName) {
        SwingUtilities.invokeLater(() -> {
            JLabel label = pumpLabels.get(pumpId);
            if (label != null) {
                label.setText("Pump " + pumpId + " --> " + carName);
                label.setBackground(new Color(255, 99, 71)); // أحمر (مشغول)
            }
        });
    }

    public void setPumpFree(int pumpId) {
        SwingUtilities.invokeLater(() -> {
            JLabel label = pumpLabels.get(pumpId);
            if (label != null) {
                label.setText("Pump " + pumpId + " [FREE]");
                label.setBackground(new Color(144, 238, 144)); // أخضر (فاضي)
            }
        });
    }

    
    public void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
class Logger {
    private static GUI gui;

    public static void setGUI(GUI guiInstance) {
        gui = guiInstance;
    }

    public static synchronized void log(String message) {
        if (gui != null) {
            gui.addLog(message);
        }
    }
}
class Validator {
    private static Validator instance;
    private int waitingCapacity;
    private int totalPumps;
    private GUI gui;
    
    private Set<String> carsInService = new HashSet<>();
    private Set<Integer> busyPumps = new HashSet<>();

    private Validator(int waitingCapacity, int totalPumps, GUI gui) {
        this.waitingCapacity = waitingCapacity;
        this.totalPumps = totalPumps;
        this.gui = gui;
    }

    public static void init(int waitingCapacity, int totalPumps, GUI gui) {
        instance = new Validator(waitingCapacity, totalPumps, gui);
    }

    public static Validator get() {
        return instance;
    }

    public synchronized void checkQueueLimit(int queueSize) {
        if (queueSize > waitingCapacity) {
            Logger.log("Validator: Queue exceeded capacity (" + queueSize + "/" + waitingCapacity + ")");
        }
    }

    public synchronized void checkActivePumps(int activeCount) {
        if (activeCount > totalPumps) {
            Logger.log("Validator: Active pumps exceed total limit (" + activeCount + "/" + totalPumps + ")");
        }
    }
    public synchronized void checkCarService(String carName) {
        if (carsInService.contains(carName)) {
            Logger.log("⚠️ Validator: " + carName + " tried to start service twice!");
        } else {
            carsInService.add(carName);
        }
    }

    public synchronized void markPumpBusy(int pumpId) {
        if (busyPumps.contains(pumpId)) {
            Logger.log("Validator: Pump " + pumpId + " already busy but assigned again!");
        } else {
            busyPumps.add(pumpId);
        }
    }

    public synchronized void markPumpFree(int pumpId) {
        if (!busyPumps.contains(pumpId)) {
            Logger.log("Validator: Pump " + pumpId + " was already free!");
        } else {
            busyPumps.remove(pumpId);
        }
    }
}


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
    private final GUI gui;

    public Pump(int pumpId, Queue<String> waitingQueue, Semaphore mutex, Semaphore empty, Semaphore full, Semaphore pumps, GUI gui) {
        this.pumpId = pumpId;
        this.waitingQueue = waitingQueue;
        this.mutex = mutex;
        this.empty = empty;
        this.full = full;
        this.pumps = pumps;
        this.gui = gui;
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
                    gui.updateQueueSize(waitingQueue.size());

                } finally {
                    mutex.signal();
                }
                if (car == null) continue;

                empty.signal(); // one more free slot in waiting area
                pumps.waiting(); // acquire service bay

                
                Logger.log("Pump " + pumpId + ": " + car + " Occupied");
                Logger.log("Pump " + pumpId + ": " + car + " login");
                Logger.log("Pump " + pumpId + ": " + car + " begins service at Bay " + pumpId);
                
                gui.setPumpBusy(pumpId, car);
                Validator.get().checkCarService(car);
                Validator.get().markPumpBusy(pumpId);
                Validator.get().checkActivePumps(gui.activePumps);
                gui.incrementActivePumps();
                
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    pumps.signal();
                    Thread.currentThread().interrupt();
                    break;
                }

                gui.setPumpBusy(pumpId, car);
                Thread.sleep(1000); 
                 gui.setPumpFree(pumpId);

                Logger.log("Pump " + pumpId + ": " + car + " finishes service");
                Logger.log("Pump " + pumpId + ": Bay " + pumpId + " is now free");
                Validator.get().markPumpFree(pumpId);

                pumps.signal(); // release bay

                gui.setPumpFree(pumpId);
                gui.decrementActivePumps();
                

                
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
    private final GUI gui;


    public Car(String name, Queue<String> queue, Semaphore empty, Semaphore full, Semaphore mutex, GUI gui) {
        this.carName = name;
        this.waitingQueue = queue;
        this.empty = empty;
        this.full = full;
        this.mutex = mutex;
        this.gui = gui;
    }

    @Override
    public void run() {
        // Show arrival immediately
        Logger.log(carName + " arrived");
        

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
                gui.updateQueueSize(waitingQueue.size());
                Validator.get().checkQueueLimit(waitingQueue.size());



                if (shouldShowWaiting) {
                    
                        Logger.log(carName + " arrived and waiting");
                    
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
    private final GUI gui;

    public ServiceStation(int waitingAreaSize, int numberOfPumps, GUI gui) {
        waitingQueue = new LinkedList<>();
        mutex = new Semaphore(1);
        empty = new Semaphore(waitingAreaSize);
        full = new Semaphore(0);
        pumps = new Semaphore(numberOfPumps);
        this.gui = gui;

        pumpThreads = new Pump[numberOfPumps];
        for (int i = 0; i < numberOfPumps; i++) {
            pumpThreads[i] = new Pump(i + 1, waitingQueue, mutex, empty, full, pumps, gui);
        }
    }

    public void startPumps() {
        for (Pump pump : pumpThreads) {
            pump.start();
        }
    }

    public void addCar(String carName) {
        new Thread(new Car(carName, waitingQueue, empty, full, mutex, gui)).start();
    }
    
    public void start(String[] cars, int arrivalDelayMillis) {
        startPumps();
    
        new Thread(() -> {
            for (String car : cars) {
                addCar(car.trim());
                try {
                    Thread.sleep(arrivalDelayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Car-Arrival").start();
    }
    
    public void shutdown() {
        for (Pump pump : pumpThreads) {
            pump.shutdown();
        }
    }

    
    public static void main(String[] args) {
   
    GUI gui = new GUI(0);
    Logger.setGUI(gui);

    
    String waitingInput = JOptionPane.showInputDialog("Enter waiting area capacity:");
    if (waitingInput == null) return;
    int waitingSize = Integer.parseInt(waitingInput.trim());

    String pumpsInput = JOptionPane.showInputDialog("Enter number of pumps (>=1):");
    if (pumpsInput == null) return;
    int numberOfPumps = Integer.parseInt(pumpsInput.trim());

    String carsInput = JOptionPane.showInputDialog("Enter car names (comma separated):");
    if (carsInput == null) return;
    String[] cars = carsInput.split(",");

    
    gui.dispose(); 
    GUI mainGui = new GUI(numberOfPumps);
    Logger.setGUI(mainGui);

    Validator.init(waitingSize, numberOfPumps, mainGui);

    ServiceStation station = new ServiceStation(waitingSize, numberOfPumps, mainGui);
  

    
    station.start(cars, 500);

    Logger.log("Simulation started successfully!");

    
}


}
