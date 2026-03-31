package com.backtester;

import java.util.HashMap;
import java.util.Map;

public class Portfolio {
    // Encapsulated data fields
    private double cashBalance;
    private Map<String, Integer> holdings; // Stores Ticker -> Number of Shares

    // Constructor
    public Portfolio(double initialCash) {
        this.cashBalance = initialCash;
        this.holdings = new HashMap<>();
    }

    // Method to simulate buying a stock
    public void buy(String ticker, int shares, double price) {
        double cost = shares * price;
        if (cashBalance >= cost) {
            cashBalance -= cost;
            // Add shares to our holdings map
            holdings.put(ticker, holdings.getOrDefault(ticker, 0) + shares);
        } else {
            System.err.println("Insufficient funds to buy " + shares + " of " + ticker);
        }
    }

    // Method to simulate selling a stock
    public void sell(String ticker, int shares, double price) {
        if (holdings.containsKey(ticker) && holdings.get(ticker) >= shares) {
            double revenue = shares * price;
            cashBalance += revenue;
            
            int currentShares = holdings.get(ticker);
            if (currentShares == shares) {
                holdings.remove(ticker); // Sold everything
            } else {
                holdings.put(ticker, currentShares - shares); // Sold some
            }
        } else {
            System.err.println("Not enough shares of " + ticker + " to sell.");
        }
    }

    // Getters so our UI can read the data
    public double getCashBalance() {
        return cashBalance;
    }

    public Map<String, Integer> getHoldings() {
        return holdings;
    }
}