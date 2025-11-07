import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.*;
import java.awt.*;

class GUI extends JFrame {
    private JTextArea logArea;
    private JLabel queueLabel, pumpsLabel;
    private JPanel pumpsPanel;
    private final Map<Integer, JLabel> pumpLabels = new HashMap<>();
    public int activePumps = 0;

    public synchronized void incrementActivePumps() { activePumps++; updatePumpCount(activePumps); }
    public synchronized void decrementActivePumps() { if (activePumps>0) activePumps--; updatePumpCount(activePumps); }

    public GUI(int numPumps) {
        setTitle("Service Station Dashboard");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10,10));

        JLabel header = new JLabel("Car Wash Simulation", SwingConstants.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 22));
        add(header, BorderLayout.NORTH);

        JPanel statusPanel = new JPanel(new GridLayout(1,2,20,10));
        queueLabel = createStatusLabel("Queue: 0", new Color(70,130,180));
        pumpsLabel = createStatusLabel("Active Pumps: 0", new Color(46,139,87));
        statusPanel.add(queueLabel);
        statusPanel.add(pumpsLabel);
        add(statusPanel, BorderLayout.SOUTH);

        pumpsPanel = new JPanel(new GridLayout(1, numPumps, 10,10));
        for(int i=1;i<=numPumps;i++){
            JLabel label=createPumpLabel(i);
            pumpLabels.put(i,label);
            pumpsPanel.add(label);
        }
        add(pumpsPanel, BorderLayout.CENTER);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("System Log"));
        logScroll.setPreferredSize(new Dimension(200,500));
        add(logScroll, BorderLayout.EAST);

        setVisible(true);
    }

    private JLabel createStatusLabel(String text, Color color){
        JLabel label=new JLabel(text,SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 20));
        label.setForeground(Color.WHITE);
        label.setOpaque(true);
        label.setBackground(color);
        return label;
    }

    private JLabel createPumpLabel(int id){
        JLabel label=new JLabel("Pump "+id+" [FREE]", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setOpaque(true);
        label.setBackground(new Color(144,238,144));
        label.setBorder(BorderFactory.createLineBorder(Color.GRAY,1));
        return label;
    }

    public void updateQueueSize(int size){ SwingUtilities.invokeLater(()->queueLabel.setText("Queue: "+size)); }
    public void updatePumpCount(int active){ SwingUtilities.invokeLater(()->pumpsLabel.setText("Active Pumps: "+active)); }
    public void setPumpBusy(int pumpId,String carName){ SwingUtilities.invokeLater(()->{
        JLabel l=pumpLabels.get(pumpId); if(l!=null){ l.setText("Pump "+pumpId+" --> "+carName); l.setBackground(new Color(255,99,71)); }
    }); }
    public void setPumpFree(int pumpId){ SwingUtilities.invokeLater(()->{
        JLabel l=pumpLabels.get(pumpId); if(l!=null){ l.setText("Pump "+pumpId+" [FREE]"); l.setBackground(new Color(144,238,144)); }
    }); }
    public void addLog(String msg){ SwingUtilities.invokeLater(()->{
        logArea.append(msg+"\n"); logArea.setCaretPosition(logArea.getDocument().getLength());
    }); }
}

class Logger {
    private static GUI gui;
    public static void setGUI(GUI g){ gui=g; }
    public static synchronized void log(String msg){ if(gui!=null) gui.addLog(msg); }
}

class Validator {
    private static Validator instance;
    private int waitingCapacity;
    int totalPumps;
    private GUI gui;
    private Set<String> carsInService=new HashSet<>();
    private Set<Integer> busyPumps=new HashSet<>();

    private Validator(int waitingCapacity,int totalPumps,GUI gui){ this.waitingCapacity=waitingCapacity; this.totalPumps=totalPumps; this.gui=gui; }
    public static void init(int w,int p,GUI g){ instance=new Validator(w,p,g); }
    public static Validator get(){ return instance; }
    public synchronized void checkQueueLimit(int queueSize){ if(queueSize>waitingCapacity) Logger.log("Validator: Queue exceeded capacity ("+queueSize+"/"+waitingCapacity+")"); }
    public synchronized void checkActivePumps(int activeCount){ if(activeCount>totalPumps) Logger.log("Validator: Active pumps exceed total limit ("+activeCount+"/"+totalPumps+")"); }
    public synchronized void checkCarService(String carName){ if(carsInService.contains(carName)) Logger.log("⚠️ Validator: "+carName+" tried to start service twice!"); else carsInService.add(carName); }
    public synchronized void markPumpBusy(int id){ busyPumps.add(id); }
    public synchronized void markPumpFree(int id){ busyPumps.remove(id); }
}

class Semaphore {
    private int value;
    public Semaphore(){ value=0; }
    public Semaphore(int val){ value=val; }
    public synchronized void waiting() throws InterruptedException{ while(value<=0) wait(); value--; }
    public synchronized void signal(){ value++; notify(); }
    public synchronized int get(){ return value; }
}

class Pump extends Thread {
    private int pumpId;
    private Queue<String> waitingQueue;
    private Semaphore mutex, empty, full, pumps;
    private volatile boolean running=true;
    private GUI gui;

    public Pump(int pumpId,Queue<String> q,Semaphore m,Semaphore e,Semaphore f,Semaphore p,GUI gui){
        this.pumpId=pumpId; waitingQueue=q; mutex=m; empty=e; full=f; pumps=p; this.gui=gui;
        setName("Pump "+pumpId);
    }

    public void shutdown(){ running=false; interrupt(); }

    @Override
    public void run(){
        try{
            while(running && !Thread.currentThread().isInterrupted()){
                full.waiting();
                mutex.waiting();
                String car;
                try{ car=waitingQueue.poll(); gui.updateQueueSize(waitingQueue.size()); } finally { mutex.signal(); }
                if(car==null) continue;
                empty.signal();
                pumps.waiting();

                Logger.log("Pump "+pumpId+": "+car+" Occupied");
                Logger.log("Pump "+pumpId+": "+car+" begins service at Bay "+pumpId);
                gui.setPumpBusy(pumpId,car);
                Validator.get().checkCarService(car);
                Validator.get().markPumpBusy(pumpId);
                gui.incrementActivePumps();
                Validator.get().checkActivePumps(gui.activePumps);

                Thread.sleep(2000);

                gui.setPumpFree(pumpId);
                Logger.log("Pump "+pumpId+": "+car+" finishes service");
                Validator.get().markPumpFree(pumpId);
                pumps.signal();
                gui.decrementActivePumps();
            }
        }catch(InterruptedException e){ Thread.currentThread().interrupt(); }
    }
}

class Car implements Runnable {
    private String carName;
    private Queue<String> waitingQueue;
    private Semaphore empty, full, mutex, pumps;
    private GUI gui;
    private static int carsArrived=0;
    private static final Object arrivalLock=new Object();

    public Car(String name,Queue<String> q,Semaphore e,Semaphore f,Semaphore m,Semaphore p,GUI g){
        carName=name; waitingQueue=q; empty=e; full=f; mutex=m; pumps=p; gui=g;
    }

    @Override
    public void run(){
        Logger.log(carName+" arrived");
        try{ Thread.sleep(ThreadLocalRandom.current().nextInt(100,300)); }catch(InterruptedException e){ Thread.currentThread().interrupt(); }

        try{
            empty.waiting();
            mutex.waiting();
            try{
                synchronized(arrivalLock){ carsArrived++; }
                waitingQueue.add(carName);
                gui.updateQueueSize(waitingQueue.size());
                Validator.get().checkQueueLimit(waitingQueue.size());
                if(carsArrived>Validator.get().totalPumps) Logger.log(carName+" arrived and waiting");
            }finally{ mutex.signal(); }
            full.signal();
        }catch(InterruptedException e){ Thread.currentThread().interrupt(); }
    }
}

public class ServiceStation {
    private Queue<String> waitingQueue;
    private Semaphore mutex, empty, full, pumps;
    private Pump[] pumpThreads;
    private GUI gui;

    public ServiceStation(int waitingAreaSize,int numPumps,GUI gui){
        waitingQueue=new LinkedList<>();
        mutex=new Semaphore(1);
        empty=new Semaphore(waitingAreaSize);
        full=new Semaphore(0);
        pumps=new Semaphore(numPumps);
        this.gui=gui;
        pumpThreads=new Pump[numPumps];
        for(int i=0;i<numPumps;i++) pumpThreads[i]=new Pump(i+1,waitingQueue,mutex,empty,full,pumps,gui);
    }

    public void startPumps(){ for(Pump p:pumpThreads) p.start(); }
    public void addCar(String carName){ new Thread(new Car(carName,waitingQueue,empty,full,mutex,pumps,gui)).start(); }

    public void start(String[] cars,int arrivalDelayMillis){
        startPumps();
        new Thread(()->{
            for(String car:cars){
                addCar(car.trim());
                try{ Thread.sleep(arrivalDelayMillis); }catch(InterruptedException e){ Thread.currentThread().interrupt(); break; }
            }
        },"Car-Arrival").start();
    }

    public void shutdown(){ for(Pump p:pumpThreads) p.shutdown(); }

    public static void main(String[] args){
        String wInput=JOptionPane.showInputDialog("Enter waiting area capacity:");
        if(wInput==null) return;
        int wSize=Integer.parseInt(wInput.trim());

        String pInput=JOptionPane.showInputDialog("Enter number of pumps (>=1):");
        if(pInput==null) return;
        int nPumps=Integer.parseInt(pInput.trim());

        String cInput=JOptionPane.showInputDialog("Enter car names (comma separated):");
        if(cInput==null) return;
        String[] cars=cInput.split(",");

        GUI gui=new GUI(nPumps);
        Logger.setGUI(gui);
        Validator.init(wSize,nPumps,gui);

        ServiceStation station=new ServiceStation(wSize,nPumps,gui);
        station.start(cars,500);
        Logger.log("Simulation started successfully!");
    }
}
