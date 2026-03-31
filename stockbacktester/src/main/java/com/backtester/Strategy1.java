package com.backtester;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

public class Strategy1 {

    public static Strategy build(BarSeries series) {
        // 1. Define our Indicators
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // A 3-day simple moving average (Short trend)
        SMAIndicator shortSma = new SMAIndicator(closePrice, 3); 
        // A 5-day simple moving average (Long trend - kept short for our small test CSV)
        SMAIndicator longSma = new SMAIndicator(closePrice, 5);  

        // 2. Define our Rules
        // BUY: When the short-term average crosses UP over the long-term average
        Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma);

        // SELL: When the short-term average crosses DOWN under the long-term average
        Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma);

        // 3. Combine into a Strategy
        return new BaseStrategy("Moving Average Crossover", buyingRule, sellingRule);
    }
}