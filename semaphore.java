public class Semaphore {
    private int value;
    public Semaphore() {
        value = 0;
    }

    public Semaphore(int val) {
        value = val;
    }

    // Wait operation 'p' ,'acquire'
    public synchronized void waiting() throws InterruptedException {
        while (value <= 0) {
            wait();
        }
        value--;
    }

    // Signal operation 'v','release'
    public synchronized void signal() {
        value++;
        notifyAll();
    }

    public synchronized int get() {
        return value;
    }
}

