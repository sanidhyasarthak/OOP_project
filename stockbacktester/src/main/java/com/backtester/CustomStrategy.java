package com.backtester;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Indicator;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

public class CustomStrategy {

    // A Factory Method that builds our strategy based on UI inputs
    public static Strategy build(BarSeries series, String indicatorType, int shortTimeframe, int longTimeframe) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // We use the base interface 'Indicator<Num>' so it can hold either an SMA or an EMA
        Indicator<Num> shortIndicator;
        Indicator<Num> longIndicator;

        // Choose the math based on the UI dropdown
        if ("EMA (Exponential)".equals(indicatorType)) {
            shortIndicator = new EMAIndicator(closePrice, shortTimeframe);
            longIndicator = new EMAIndicator(closePrice, longTimeframe);
        } else {
            // Default to SMA
            shortIndicator = new SMAIndicator(closePrice, shortTimeframe);
            longIndicator = new SMAIndicator(closePrice, longTimeframe);
        }

        // Define the rules using our dynamic indicators
        Rule buyingRule = new CrossedUpIndicatorRule(shortIndicator, longIndicator);
        Rule sellingRule = new CrossedDownIndicatorRule(shortIndicator, longIndicator);

        return new BaseStrategy(indicatorType + " Crossover Strategy", buyingRule, sellingRule);
    }
}