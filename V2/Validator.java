import java.util.HashSet;
import java.util.Set;

public class Validator {
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

    // ✅ 1. التأكد من إن الطابور ما زادش عن السعة
    public synchronized void checkQueueLimit(int queueSize) {
        if (queueSize > waitingCapacity) {
            Logger.log("⚠️ Validator: Queue exceeded capacity (" + queueSize + "/" + waitingCapacity + ")");
        }
    }

    // ✅ 2. التأكد إن عدد الـ Pumps الشغالة مش أكتر من المسموح
    public synchronized void checkActivePumps(int activeCount) {
        if (activeCount > totalPumps) {
            Logger.log("⚠️ Validator: Active pumps exceed total limit (" + activeCount + "/" + totalPumps + ")");
        }
    }

    // ✅ 3. التأكد إن العربية ما بدأتش الخدمة مرتين
    public synchronized void checkCarService(String carName) {
        if (carsInService.contains(carName)) {
            Logger.log("⚠️ Validator: " + carName + " tried to start service twice!");
        } else {
            carsInService.add(carName);
        }
    }

    // ✅ 4. التأكد إن الـ Pump حرّر الـ bay بعد الانتهاء
    public synchronized void markPumpBusy(int pumpId) {
        if (busyPumps.contains(pumpId)) {
            Logger.log("⚠️ Validator: Pump " + pumpId + " already busy but assigned again!");
        } else {
            busyPumps.add(pumpId);
        }
    }

    public synchronized void markPumpFree(int pumpId) {
        if (!busyPumps.contains(pumpId)) {
            Logger.log("⚠️ Validator: Pump " + pumpId + " was already free!");
        } else {
            busyPumps.remove(pumpId);
        }
    }
}
