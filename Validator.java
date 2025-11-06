import java.util.Queue;

public class Validator {

    // يتحقق إن الطابور ما تجاوزش سعته
    public static boolean validateQueue(Queue<String> queue, int maxSize) {
        if (queue.size() > maxSize) {
            Logger.log("Queue overflow detected! Queue size: " + queue.size());
            return false;
        }
        return true;
    }

    public static boolean validateSemaphores(Semaphore... semaphores) {
        for (Semaphore s : semaphores) {
            if (s.get() < 0) {
                Logger.log("Invalid semaphore value detected: " + s.get());
                return false;
            }
        }
        return true;
    }
}
