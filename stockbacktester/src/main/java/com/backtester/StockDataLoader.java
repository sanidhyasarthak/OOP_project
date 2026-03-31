package com.backtester;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class StockDataLoader {

    // This method reads a CSV and returns a ta4j BarSeries
    public static BarSeries loadCsvSeries(String filepath, String ticker) {
        // 1. Initialize an empty BarSeries
        BarSeries series = new BaseBarSeriesBuilder().withName(ticker).build();
        
        // 2. Define how our dates are formatted in the CSV (Year-Month-Day)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line = br.readLine(); // Read and ignore the header row
            
            // 3. Loop through every line in the file
            while ((line = br.readLine()) != null) {
                String[] values = line.split(","); // Split the row by commas
                
                // 4. Extract the data
                LocalDate date = LocalDate.parse(values[0], formatter);
                // ta4j requires a time zone for its dates, so we default to the system's zone
                ZonedDateTime zdt = date.atStartOfDay(ZoneId.systemDefault()); 
                
                double open = Double.parseDouble(values[1]);
                double high = Double.parseDouble(values[2]);
                double low = Double.parseDouble(values[3]);
                double close = Double.parseDouble(values[4]);
                double volume = Double.parseDouble(values[5]);

                // 5. Add the bar to the series
                series.addBar(zdt, open, high, low, close, volume);
            }
        } catch (Exception e) {
            System.err.println("Error reading the CSV file: " + e.getMessage());
        }
        
        return series;
    }
}