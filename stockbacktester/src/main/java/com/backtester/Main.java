package com.backtester;

import com.formdev.flatlaf.FlatDarkLaf;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.BarSeriesManager;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.Position;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class Main {
    // Shared Data Model
    private static Portfolio myPortfolio;
    
    // UI Components accessed across different methods
    private static DefaultTableModel tableModel;
    private static JLabel cashLabel;
    private static JLabel equityLabel;
    private static JTextArea logArea;
    
    // Strategy Builder Inputs
    private static JComboBox<String> indicatorDropdown;
    private static JSpinner shortSpinner;
    private static JSpinner longSpinner;
    
    // Backtest Page Inputs
    private static JComboBox<String> tickerDropdown;

    public static void main(String[] args) {
        // Fix for Mac/Windows font rendering
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf theme.");
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ProTrade Options & Stock Engine");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);

            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 14));

            // These methods generate the three pages in our app!
            tabbedPane.addTab("Strategy Builder", createStrategyBuilderPage());
            tabbedPane.addTab("Run Backtest", createBacktestPage());
            tabbedPane.addTab("Portfolio", createPortfolioDashboard());

            frame.add(tabbedPane);
            frame.setVisible(true);
        });
    }

    // --- PAGE 1: STRATEGY BUILDER TAB ---
    private static JPanel createStrategyBuilderPage() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); 
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Moving Average Crossover Parameters");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1;
        panel.add(new JLabel("Indicator Type:"), gbc);
        String[] indicators = {"SMA (Simple)", "EMA (Exponential)"};
        indicatorDropdown = new JComboBox<>(indicators);
        gbc.gridx = 1;
        panel.add(indicatorDropdown, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Fast Line (Days):"), gbc);
        shortSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 200, 1)); 
        gbc.gridx = 1;
        panel.add(shortSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Slow Line (Days):"), gbc);
        longSpinner = new JSpinner(new SpinnerNumberModel(5, 2, 200, 1));
        gbc.gridx = 1;
        panel.add(longSpinner, gbc);

        return panel;
    }

    // --- PAGE 2: RUN BACKTEST TAB (The Engine UI) ---
    private static JPanel createBacktestPage() {
        // Modern UI: Add 25px of padding around the whole page
        JPanel panel = new JPanel(new BorderLayout(0, 20)); 
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        // Modern UI: Create a styled "Card" for the top controls
        JPanel cardPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        cardPanel.setBackground(UIManager.getColor("Panel.background").brighter());
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1, true), 
            BorderFactory.createEmptyBorder(5, 10, 5, 10) 
        ));

        // Ticker Dropdown
        JLabel tickerLabel = new JLabel("Select Ticker:");
        tickerLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cardPanel.add(tickerLabel);
        
        String[] availableTickers = {"AAPL", "MSFT", "GOOGL"};
        tickerDropdown = new JComboBox<>(availableTickers);
        tickerDropdown.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cardPanel.add(tickerDropdown);

        // Capital Input
        JLabel capitalLabel = new JLabel("Starting Capital ($):");
        capitalLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cardPanel.add(capitalLabel);
        
        JTextField capitalField = new JTextField("10000", 8);
        capitalField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cardPanel.add(capitalField);

        // Modern UI: Emphasized Run Button
        JButton runButton = new JButton("Run Backtest");
        runButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        runButton.setPreferredSize(new Dimension(150, 40)); 
        runButton.putClientProperty("FlatLaf.styleClass", "primary");
        runButton.putClientProperty("JButton.buttonType", "roundRect");
        cardPanel.add(runButton);

        panel.add(cardPanel, BorderLayout.NORTH);

        // Styled Log Area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        logArea.setMargin(new Insets(10, 10, 10, 10)); 
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1, true));
        panel.add(scrollPane, BorderLayout.CENTER);

        // ENGINE EXECUTION LOGIC
        runButton.addActionListener(e -> {
            String ticker = (String) tickerDropdown.getSelectedItem(); 
            String filepath = ticker + "_test.csv";
            
            try {
                double initialCapital = Double.parseDouble(capitalField.getText());
                
                myPortfolio = new Portfolio(initialCapital);
                refreshDashboard("", 0.0); 
                
                logArea.setText(""); 
                logArea.append("Initializing engine for " + ticker + "...\n");

                BarSeries series = StockDataLoader.loadCsvSeries(filepath, ticker);
                if (series != null && series.getBarCount() > 0) {
                    
                    // Read strategy params from Tab 1
                    String selectedType = (String) indicatorDropdown.getSelectedItem();
                    int shortDays = (Integer) shortSpinner.getValue();
                    int longDays = (Integer) longSpinner.getValue();
                    
                    Strategy strategy = CustomStrategy.build(series, selectedType, shortDays, longDays);
                    BarSeriesManager seriesManager = new BarSeriesManager(series);
                    TradingRecord tradingRecord = seriesManager.run(strategy);

                    logArea.append("Simulation complete. Trades Executed: " + tradingRecord.getPositionCount() + "\n");
                    logArea.append("--------------------------------------------------\n");

                    // Process closed historical trades
                    for (Position position : tradingRecord.getPositions()) {
                        if (position.isClosed()) {
                            double entryPrice = position.getEntry().getNetPrice().doubleValue();
                            double exitPrice = position.getExit().getNetPrice().doubleValue();
                            
                            int sharesToBuy = (int) (myPortfolio.getCashBalance() / entryPrice);

                            if (sharesToBuy > 0) {
                                myPortfolio.buy(ticker, sharesToBuy, entryPrice);
                                myPortfolio.sell(ticker, sharesToBuy, exitPrice);
                                
                                double profit = (exitPrice - entryPrice) * sharesToBuy;
                                logArea.append(String.format("CLOSED TRADE: %d shares | Entry: $%.2f | Exit: $%.2f | P&L: $%.2f\n", 
                                    sharesToBuy, entryPrice, exitPrice, profit));
                            }
                        }
                    }

                    // Check if we are still holding the stock today
                    Position currentPosition = tradingRecord.getCurrentPosition();
                    if (currentPosition != null && currentPosition.isOpened()) {
                        double entryPrice = currentPosition.getEntry().getNetPrice().doubleValue();
                        int sharesToBuy = (int) (myPortfolio.getCashBalance() / entryPrice);
                        
                        if (sharesToBuy > 0) {
                            myPortfolio.buy(ticker, sharesToBuy, entryPrice);
                            logArea.append(String.format("OPEN POSITION: Bought %d shares at $%.2f. Still holding!\n", 
                                sharesToBuy, entryPrice));
                        }
                    }

                    double finalMarketPrice = series.getLastBar().getClosePrice().doubleValue();
                    refreshDashboard(ticker, finalMarketPrice);
                    
                    logArea.append("--------------------------------------------------\n");
                    logArea.append("Engine stopped. Check the Portfolio Tab for updated balances!\n");

                } else {
                    logArea.append("ERROR: No data found in CSV.\n");
                }
            } catch (Exception ex) {
                logArea.append("ERROR: Check inputs or CSV file. Details: " + ex.getMessage() + "\n");
            }
        });

        return panel;
    }

    // --- PAGE 3: PORTFOLIO DASHBOARD TAB ---
    private static JPanel createPortfolioDashboard() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        cashLabel = new JLabel("Available Cash: $10,000.00");
        cashLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        equityLabel = new JLabel("Total Portfolio Value: $10,000.00");
        equityLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        
        summaryPanel.add(cashLabel);
        summaryPanel.add(equityLabel);
        panel.add(summaryPanel, BorderLayout.NORTH);

        String[] columnNames = {"Ticker Symbol", "Shares Owned", "Current Market Price", "Total Value"};
        tableModel = new DefaultTableModel(new Object[][]{}, columnNames);
        JTable table = new JTable(tableModel);
        
        table.setRowHeight(30);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // --- HELPER METHOD: Updates the Dashboard Table ---
    private static void refreshDashboard(String currentTicker, double currentMarketPrice) {
        if (myPortfolio == null) return;

        double cash = myPortfolio.getCashBalance();
        cashLabel.setText(String.format("Available Cash: $%,.2f", cash));

        tableModel.setRowCount(0); 
        
        double totalHoldingsValue = 0.0;
        Map<String, Integer> holdings = myPortfolio.getHoldings();
        
        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
            String ticker = entry.getKey();
            int shares = entry.getValue();
            
            double price = ticker.equals(currentTicker) ? currentMarketPrice : 0.0; 
            double value = shares * price;
            totalHoldingsValue += value;

            tableModel.addRow(new Object[]{
                ticker, 
                shares, 
                String.format("$%,.2f", price), 
                String.format("$%,.2f", value)
            });
        }

        double totalEquity = cash + totalHoldingsValue;
        equityLabel.setText(String.format("Total Portfolio Value: $%,.2f", totalEquity));
    }
}