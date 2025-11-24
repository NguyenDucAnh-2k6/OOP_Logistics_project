package com.oop.logistics.ui;

import com.oop.logistics.core.DisasterLogisticsPipeline;
import com.oop.logistics.models.DisasterAnalysisReport;
import com.oop.logistics.models.DisasterEvent;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Main dashboard GUI for disaster logistics system
 */
public class DashboardFrame extends JFrame {
    
    private final DisasterLogisticsPipeline pipeline;
    private JTabbedPane tabbedPane;
    private JTextArea logArea;
    private JTable eventsTable;
    private DefaultTableModel tableModel;
    private JButton collectButton;
    private JButton analyzeButton;
    private JButton urgentButton;
    private JLabel statusLabel;
    
    public DashboardFrame(DisasterLogisticsPipeline pipeline) {
        this.pipeline = pipeline;
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("Disaster Logistics Management System");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Create main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top panel with controls
        mainPanel.add(createControlPanel(), BorderLayout.NORTH);
        
        // Center with tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Events", createEventsPanel());
        tabbedPane.addTab("Analysis", createAnalysisPanel());
        tabbedPane.addTab("Configuration", createConfigPanel());
        tabbedPane.addTab("Logs", createLogsPanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Bottom status bar
        mainPanel.add(createStatusBar(), BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
    }
    
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Controls"));
        
        collectButton = new JButton("Collect Data");
        collectButton.addActionListener(e -> collectData());
        
        analyzeButton = new JButton("Run Analysis");
        analyzeButton.addActionListener(e -> runAnalysis());
        
        urgentButton = new JButton("Find Urgent Events");
        urgentButton.addActionListener(e -> findUrgentEvents());
        
        JButton refreshButton = new JButton("Refresh Stats");
        refreshButton.addActionListener(e -> refreshStats());
        
        panel.add(collectButton);
        panel.add(analyzeButton);
        panel.add(urgentButton);
        panel.add(refreshButton);
        
        return panel;
    }
    
    private JPanel createEventsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        // Table for displaying events
        String[] columns = {"ID", "Type", "Location", "Severity", "Engagement", "Timestamp"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        eventsTable = new JTable(tableModel);
        eventsTable.setAutoCreateRowSorter(true);
        eventsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        eventsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showEventDetails();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(eventsTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Info panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(new JLabel("Double-click an event to see details"));
        panel.add(infoPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createAnalysisPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        JTextArea analysisArea = new JTextArea();
        analysisArea.setEditable(false);
        analysisArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(analysisArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        JTextArea configArea = new JTextArea();
        configArea.setEditable(false);
        configArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        // Display current configuration
        var config = pipeline.getConfiguration();
        StringBuilder sb = new StringBuilder();
        sb.append("=== CONFIGURATION ===\n\n");
        sb.append("Data Sources:\n");
        for (String source : config.getDataSources()) {
            sb.append("  - ").append(source).append("\n");
        }
        sb.append("\nKeyword Categories:\n");
        for (String category : config.getKeywordCategories()) {
            sb.append("  - ").append(category).append("\n");
        }
        sb.append("\nDamage/Relief Categories:\n");
        for (String category : config.getCategoryNames()) {
            sb.append("  - ").append(category).append("\n");
        }
        
        configArea.setText(sb.toString());
        
        JScrollPane scrollPane = new JScrollPane(configArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add buttons to modify configuration
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton editKeywordsBtn = new JButton("Edit Keywords");
        JButton editCategoriesBtn = new JButton("Edit Categories");
        JButton addSourceBtn = new JButton("Add Data Source");
        
        buttonPanel.add(editKeywordsBtn);
        buttonPanel.add(editCategoriesBtn);
        buttonPanel.add(addSourceBtn);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createLogsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton clearButton = new JButton("Clear Logs");
        clearButton.addActionListener(e -> logArea.setText(""));
        buttonPanel.add(clearButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEtchedBorder());
        
        statusLabel = new JLabel("Ready");
        panel.add(statusLabel);
        
        return panel;
    }
    
    private void collectData() {
        setStatus("Collecting data...");
        disableButtons();
        
        SwingWorker<List<DisasterEvent>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<DisasterEvent> doInBackground() {
                return pipeline.findUrgentEvents();
            }
            
            @Override
            protected void done() {
                try {
                    List<DisasterEvent> events = get();
                    displayEvents(events);
                    setStatus("Collected " + events.size() + " events");
                    log("Data collection completed: " + events.size() + " events");
                } catch (Exception e) {
                    setStatus("Error collecting data");
                    log("ERROR: " + e.getMessage());
                } finally {
                    enableButtons();
                }
            }
        };
        
        worker.execute();
    }
    
    private void runAnalysis() {
        setStatus("Running analysis...");
        disableButtons();
        
        SwingWorker<DisasterLogisticsPipeline.PipelineResult, Void> worker = new SwingWorker<>() {
            @Override
            protected DisasterLogisticsPipeline.PipelineResult doInBackground() {
                return pipeline.execute();
            }
            
            @Override
            protected void done() {
                try {
                    DisasterLogisticsPipeline.PipelineResult result = get();
                    if (result.isSuccess()) {
                        displayEvents(result.getProcessedEvents());
                        displayAnalysisReport(result.getAnalysisReport());
                        setStatus("Analysis completed");
                        log("Analysis completed: " + result.getProcessedEventCount() + " events analyzed");
                    } else {
                        setStatus("Analysis failed");
                        log("ERROR: " + result.getError());
                    }
                } catch (Exception e) {
                    setStatus("Error running analysis");
                    log("ERROR: " + e.getMessage());
                } finally {
                    enableButtons();
                }
            }
        };
        
        worker.execute();
    }
    
    private void findUrgentEvents() {
        setStatus("Finding urgent events...");
        
        SwingWorker<List<DisasterEvent>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<DisasterEvent> doInBackground() {
                return pipeline.findUrgentEvents();
            }
            
            @Override
            protected void done() {
                try {
                    List<DisasterEvent> events = get();
                    displayEvents(events);
                    setStatus("Found " + events.size() + " urgent events");
                    log("Urgent events search completed: " + events.size() + " events");
                } catch (Exception e) {
                    setStatus("Error finding urgent events");
                    log("ERROR: " + e.getMessage());
                }
            }
        };
        
        worker.execute();
    }
    
    private void refreshStats() {
        var stats = pipeline.getCollectionStats();
        log("Collection Stats: " + stats.toString());
        setStatus("Stats refreshed");
    }
    
    private void displayEvents(List<DisasterEvent> events) {
        tableModel.setRowCount(0);
        for (DisasterEvent event : events) {
            Object[] row = {
                event.getSourceId(),
                event.getDisasterType(),
                event.getLocation(),
                event.getSeverity(),
                event.getEngagement(),
                event.getTimestamp()
            };
            tableModel.addRow(row);
        }
    }
    
    private void displayAnalysisReport(DisasterAnalysisReport report) {
        JTextArea analysisArea = (JTextArea) ((JScrollPane) 
            tabbedPane.getComponentAt(1)).getViewport().getView();
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== DISASTER ANALYSIS REPORT ===\n\n");
        sb.append("Total Events: ").append(report.getTotalEvents()).append("\n\n");
        
        sb.append("Events by Type:\n");
        report.getEventsByType().forEach((type, count) -> 
            sb.append("  ").append(type).append(": ").append(count).append("\n"));
        
        sb.append("\nEvents by Location:\n");
        report.getEventsByLocation().forEach((location, count) -> 
            sb.append("  ").append(location).append(": ").append(count).append("\n"));
        
        sb.append("\nEvents by Severity:\n");
        report.getEventsBySeverity().forEach((severity, count) -> 
            sb.append("  ").append(severity).append(": ").append(count).append("\n"));
        
        sb.append("\nAverage Engagement: ").append(
            String.format("%.2f", report.getAverageEngagement())).append("\n");
        
        analysisArea.setText(sb.toString());
        tabbedPane.setSelectedIndex(1);
    }
    
    private void showEventDetails() {
        int selectedRow = eventsTable.getSelectedRow();
        if (selectedRow >= 0) {
            String id = (String) tableModel.getValueAt(selectedRow, 0);
            JOptionPane.showMessageDialog(this, 
                "Event details for: " + id, 
                "Event Details", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void setStatus(String status) {
        statusLabel.setText(status);
    }
    
    private void log(String message) {
        logArea.append("[" + java.time.LocalTime.now() + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    
    private void disableButtons() {
        collectButton.setEnabled(false);
        analyzeButton.setEnabled(false);
        urgentButton.setEnabled(false);
    }
    
    private void enableButtons() {
        collectButton.setEnabled(true);
        analyzeButton.setEnabled(true);
        urgentButton.setEnabled(true);
    }
}