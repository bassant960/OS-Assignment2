public class Logger {
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