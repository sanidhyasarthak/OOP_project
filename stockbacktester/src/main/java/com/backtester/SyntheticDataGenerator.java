package com.backtester;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class SyntheticDataGenerator {

    /**
     * Generates a CSV file with synthetic OHLCV stock data.
     * * @param ticker     The stock symbol (e.g., "AAPL")
     * @param days       How many days of data to generate
     * @param startPrice The starting price of the asset
     * @param volatility The daily volatility (e.g., 0.02 for 2% daily swings)
     */
    public static void generateData(String ticker, int days, double startPrice, double volatility) {
        String filename = ticker + "_test.csv";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Random random = new Random();

        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            // Write the CSV Header. 
            // NOTE: Ensure this matches the format your StockDataLoader.java expects!
            out.println("Date,Open,High,Low,Close,Volume");

            double currentClose = startPrice;
            LocalDate currentDate = LocalDate.now().minusDays(days);

            int tradingDaysGenerated = 0;

            while (tradingDaysGenerated < days) {
                // Skip weekends to mimic real stock markets
                if (currentDate.getDayOfWeek().getValue() >= 6) {
                    currentDate = currentDate.plusDays(1);
                    continue;
                }

                double open = currentClose;
                
                // Calculate the daily return using a standard normal distribution
                double dailyReturn = random.nextGaussian() * volatility;
                double close = open * (1 + dailyReturn);
                
                // Calculate High and Low with some random noise
                double maxOC = Math.max(open, close);
                double minOC = Math.min(open, close);
                double high = maxOC + (Math.abs(random.nextGaussian()) * volatility * open * 0.5);
                double low = minOC - (Math.abs(random.nextGaussian()) * volatility * open * 0.5);
                
                // Edge case protection (stocks can't drop below $0)
                if (low <= 0) low = 0.01;
                if (close <= 0) close = 0.01;

                // Random volume between 1M and 1.5M shares
                long volume = 1000000 + (long)(random.nextDouble() * 500000);

                // Write the row to the CSV
                out.printf("%s,%.2f,%.2f,%.2f,%.2f,%d\n", 
                    currentDate.format(formatter), open, high, low, close, volume);

                currentClose = close;
                currentDate = currentDate.plusDays(1);
                tradingDaysGenerated++;
            }
            
            System.out.println("Successfully generated " + tradingDaysGenerated + " days of synthetic data for " + ticker);

        } catch (IOException e) {
            System.err.println("Error writing synthetic data: " + e.getMessage());
        }
    }

    // A standalone Main method so you can generate your files quickly!
    public static void main(String[] args) {
        System.out.println("Generating Synthetic Market Data...");
        
        // Asset 1: Standard, stable tech stock (1.5% daily volatility)
        generateData("AAPL", 500, 150.0, 0.015);
        
        // Asset 2: High price, moderate volatility (2.0% daily volatility)
        generateData("MSFT", 500, 250.0, 0.020);
        
        // Asset 3: Highly volatile, aggressive growth stock (3.5% daily volatility)
        generateData("GOOGL", 500, 100.0, 0.035);
        
        System.out.println("All data generated! You can now run the ProTradeApp.");
    }
}