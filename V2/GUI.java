import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class GUI extends JFrame {

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