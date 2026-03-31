package com.backtester;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BarSeriesManager;
import org.ta4j.core.Indicator;
import org.ta4j.core.Position;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ProTradeApp extends Application {

    // --- SHARED DATA & UI COMPONENTS ---
    private Portfolio myPortfolio;

    // Strategy Builder Inputs
    private ComboBox<String> indicatorCombo;
    private Spinner<Integer> shortSpinner;
    private Spinner<Integer> longSpinner;

    // Backtest Page Inputs
    private ComboBox<String> tickerCombo;
    private TextField capitalField;
    private TextArea logArea;

    // Portfolio Dashboard
    private Label cashLabel;
    private Label equityLabel;
    private TableView<Holding> portfolioTable;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("ProTrade Engine - Professional Edition");

        BorderPane root = new BorderPane();
        
        // --- 1. THE TOP BAR ---
        HBox topBar = new HBox();
        topBar.getStyleClass().add("top-bar");
        Button hamburgerBtn = new Button("☰");
        hamburgerBtn.getStyleClass().add("hamburger-button");
        Label appTitle = new Label("ProTrade Terminal");
        appTitle.getStyleClass().add("header-text");
        appTitle.setStyle("-fx-font-size: 18px;"); 
        topBar.getChildren().addAll(hamburgerBtn, appTitle);
        root.setTop(topBar);

        // --- 2. BUILD THE SIDEBAR ---
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(220); 

        Button btnStrategy = new Button("Strategy Builder");
        btnStrategy.getStyleClass().add("nav-button");
        btnStrategy.setMaxWidth(Double.MAX_VALUE);

        Button btnMarketData = new Button("Market Data");
        btnMarketData.getStyleClass().add("nav-button");
        btnMarketData.setMaxWidth(Double.MAX_VALUE);

        Button btnBacktest = new Button("Run Engine");
        btnBacktest.getStyleClass().add("nav-button");
        btnBacktest.setMaxWidth(Double.MAX_VALUE);

        Button btnPortfolio = new Button("Live Portfolio");
        btnPortfolio.getStyleClass().add("nav-button");
        btnPortfolio.setMaxWidth(Double.MAX_VALUE);

        sidebar.getChildren().addAll(btnStrategy, btnMarketData, btnBacktest, btnPortfolio);

        // --- 3. THE MAGIC WRAPPER (Push + Slide effect) ---
        Pane sidebarWrapper = new Pane(sidebar);
        sidebarWrapper.setPrefWidth(0); 
        sidebarWrapper.setMinWidth(0);
        sidebar.prefHeightProperty().bind(sidebarWrapper.heightProperty());

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(sidebarWrapper.widthProperty());
        clip.heightProperty().bind(sidebarWrapper.heightProperty());
        sidebarWrapper.setClip(clip);
        sidebar.translateXProperty().bind(sidebarWrapper.widthProperty().subtract(220));

        // --- 4. PRE-BUILD PAGES & LAYOUT ---
        BorderPane pageContainer = new BorderPane();
        VBox strategyPage = createStrategyBuilderPage();
        VBox marketDataPage = createMarketDataPage(); 
        VBox backtestPage = createBacktestPage();
        VBox portfolioPage = createPortfolioDashboard();
        
        pageContainer.setCenter(marketDataPage); // Default landing page

        HBox centerArea = new HBox(sidebarWrapper, pageContainer);
        HBox.setHgrow(pageContainer, Priority.ALWAYS); 
        root.setCenter(centerArea);

        // --- 5. THE ANIMATION TIMELINES ---
        Timeline slideIn = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(sidebarWrapper.prefWidthProperty(), 220)));
        Timeline slideOut = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(sidebarWrapper.prefWidthProperty(), 0)));

        hamburgerBtn.setOnAction(e -> {
            if (sidebarWrapper.getPrefWidth() == 0) slideIn.play(); else slideOut.play();
        });

        btnStrategy.setOnAction(e -> { pageContainer.setCenter(strategyPage); slideOut.play(); });
        btnMarketData.setOnAction(e -> { pageContainer.setCenter(marketDataPage); slideOut.play(); });
        btnBacktest.setOnAction(e -> { pageContainer.setCenter(backtestPage); slideOut.play(); });
        btnPortfolio.setOnAction(e -> { pageContainer.setCenter(portfolioPage); slideOut.play(); });

        // --- 6. APPLY CSS AND SHOW ---
        Scene scene = new Scene(root, 1100, 700);
        String cssPath = getClass().getResource("/style.css").toExternalForm();
        if(cssPath != null) {
            scene.getStylesheets().add(cssPath);
        }
        
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // =========================================================================
    // PAGE 1: STRATEGY LAB (FORM-BASED BUILDER)
    // =========================================================================
    private VBox createStrategyBuilderPage() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(30));

        Label title = new Label("Strategy Lab");
        title.getStyleClass().add("header-text");

        // --- CARD 1: STRATEGY DETAILS ---
        VBox detailsCard = new VBox(15);
        detailsCard.getStyleClass().add("card");
        Label detailsLabel = new Label("1. Strategy Details");
        detailsLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(15); detailsGrid.setVgap(15);
        
        detailsGrid.add(new Label("Strategy Name:"), 0, 0);
        TextField nameField = new TextField("My Alpha Strategy");
        nameField.setPrefWidth(250);
        detailsGrid.add(nameField, 1, 0);
        
        detailsCard.getChildren().addAll(detailsLabel, detailsGrid);

        // --- CARD 2: ENTRY LOGIC ---
        VBox logicCard = new VBox(15);
        logicCard.getStyleClass().add("card");
        Label logicLabel = new Label("2. Entry Condition (Crossover)");
        logicLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");

        GridPane logicGrid = new GridPane();
        logicGrid.setHgap(15); logicGrid.setVgap(15);

        logicGrid.add(new Label("Primary Indicator:"), 0, 0);
        indicatorCombo = new ComboBox<>();
        indicatorCombo.getItems().addAll("SMA (Simple Moving Average)", "EMA (Exponential Moving Average)");
        indicatorCombo.setValue("SMA (Simple Moving Average)");
        logicGrid.add(indicatorCombo, 1, 0);

        logicGrid.add(new Label("Fast Period (Days):"), 0, 1);
        shortSpinner = new Spinner<>(1, 200, 10);
        shortSpinner.setEditable(true);
        logicGrid.add(shortSpinner, 1, 1);

        logicGrid.add(new Label("Slow Period (Days):"), 0, 2);
        longSpinner = new Spinner<>(2, 200, 50);
        longSpinner.setEditable(true);
        logicGrid.add(longSpinner, 1, 2);

        logicGrid.add(new Label("Trigger Action:"), 0, 3);
        ComboBox<String> triggerCombo = new ComboBox<>();
        triggerCombo.getItems().addAll("Buy when Fast crosses ABOVE Slow", "Buy when Fast crosses BELOW Slow");
        triggerCombo.setValue("Buy when Fast crosses ABOVE Slow");
        logicGrid.add(triggerCombo, 1, 3);

        logicCard.getChildren().addAll(logicLabel, logicGrid);

        // --- CARD 3: RISK MANAGEMENT ---
        VBox riskCard = new VBox(15);
        riskCard.getStyleClass().add("card");
        Label riskLabel = new Label("3. Risk Controls");
        riskLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");

        GridPane riskGrid = new GridPane();
        riskGrid.setHgap(15); riskGrid.setVgap(15);

        riskGrid.add(new Label("Stop Loss (%):"), 0, 0);
        Spinner<Double> stopLossSpinner = new Spinner<>(0.0, 100.0, 5.0, 0.5);
        stopLossSpinner.setEditable(true);
        riskGrid.add(stopLossSpinner, 1, 0);

        riskGrid.add(new Label("Take Profit (%):"), 0, 1);
        Spinner<Double> takeProfitSpinner = new Spinner<>(0.0, 500.0, 15.0, 1.0);
        takeProfitSpinner.setEditable(true);
        riskGrid.add(takeProfitSpinner, 1, 1);

        riskCard.getChildren().addAll(riskLabel, riskGrid);

        // --- ACTION BAR ---
        HBox actionBar = new HBox();
        actionBar.setAlignment(Pos.CENTER_RIGHT);
        Button saveBtn = new Button("Save Strategy to Engine");
        saveBtn.getStyleClass().add("primary-button");
        
        saveBtn.setOnAction(e -> {
            // Placeholder for saving logic
            System.out.println("Saved Strategy: " + nameField.getText());
            saveBtn.setText("Saved!");
            saveBtn.setStyle("-fx-background-color: #00E676;"); // Turn green on success
        });

        actionBar.getChildren().add(saveBtn);

        // Assemble Page
        page.getChildren().addAll(title, detailsCard, logicCard, riskCard, actionBar);
        return page;
    }

    // =========================================================================
    // PAGE 2: MARKET DATA (Stock Info & Base Chart)
    // =========================================================================
    private VBox createMarketDataPage() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(30));

        Label title = new Label("Market Data Explorer");
        title.getStyleClass().add("header-text");

        HBox searchCard = new HBox(15);
        searchCard.getStyleClass().add("card");
        searchCard.setStyle("-fx-alignment: center-left;"); 
        
        ComboBox<String> searchCombo = new ComboBox<>();
        searchCombo.getItems().addAll("AAPL", "MSFT", "GOOGL");
        searchCombo.setValue("AAPL");
        
        Button searchButton = new Button("Load Data");
        searchButton.getStyleClass().add("primary-button");
        searchCard.getChildren().addAll(new Label("Select Asset:"), searchCombo, searchButton);

        HBox summaryBox = new HBox(15);
        VBox priceCard = createInfoCard("Last Close Price", "--");
        VBox highCard = createInfoCard("Period High", "--");
        VBox lowCard = createInfoCard("Period Low", "--");
        summaryBox.getChildren().addAll(priceCard, highCard, lowCard);

        VBox chartBox = new VBox();
        chartBox.getStyleClass().add("card");
        VBox.setVgrow(chartBox, Priority.ALWAYS);
        Label chartPlaceholder = new Label("Select an asset and click Load Data.");
        chartPlaceholder.getStyleClass().add("label-text");
        chartBox.setAlignment(Pos.CENTER);
        chartBox.getChildren().add(chartPlaceholder);

        searchButton.setOnAction(e -> {
            String ticker = searchCombo.getValue();
            String filepath = ticker + "_test.csv";
            
            try {
                BarSeries series = StockDataLoader.loadCsvSeries(filepath, ticker);
                if (series != null && series.getBarCount() > 0) {
                    double maxHigh = 0;
                    double minLow = Double.MAX_VALUE;
                    
                    for (int i = 0; i < series.getBarCount(); i++) {
                        Bar bar = series.getBar(i);
                        if (bar.getHighPrice().doubleValue() > maxHigh) maxHigh = bar.getHighPrice().doubleValue();
                        if (bar.getLowPrice().doubleValue() < minLow) minLow = bar.getLowPrice().doubleValue();
                    }
                    
                    double lastClose = series.getLastBar().getClosePrice().doubleValue();

                    ((Label) priceCard.getChildren().get(1)).setText(String.format("$%,.2f", lastClose));
                    ((Label) highCard.getChildren().get(1)).setText(String.format("$%,.2f", maxHigh));
                    ((Label) lowCard.getChildren().get(1)).setText(String.format("$%,.2f", minLow));

                    chartBox.getChildren().clear();
                    LineChart<String, Number> nativeChart = createJavaFXChart(series, ticker);
                    VBox.setVgrow(nativeChart, Priority.ALWAYS);
                    chartBox.getChildren().add(nativeChart);
                }
            } catch (Exception ex) {
                chartBox.getChildren().clear();
                chartBox.getChildren().add(new Label("Error loading data: " + ex.getMessage()));
            }
        });

        page.getChildren().addAll(title, searchCard, summaryBox, chartBox);
        return page;
    }

    // =========================================================================
    // PAGE 3: EXECUTION TERMINAL (Metrics, Equity Curve, Strategy Chart)
    // =========================================================================
    private VBox createBacktestPage() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(30));

        Label title = new Label("Execution Terminal");
        title.getStyleClass().add("header-text");

        HBox controlCard = new HBox(15);
        controlCard.getStyleClass().add("card");
        controlCard.setStyle("-fx-alignment: center-left;"); 

        tickerCombo = new ComboBox<>();
        tickerCombo.getItems().addAll("AAPL", "MSFT", "GOOGL");
        tickerCombo.setValue("AAPL");

        capitalField = new TextField("10000");
        capitalField.setPrefWidth(100);

        Button runButton = new Button("Execute Strategy");
        runButton.getStyleClass().add("primary-button");

        controlCard.getChildren().addAll(
            new Label("Target Asset:"), tickerCombo,
            new Label("Initial Capital ($):"), capitalField,
            runButton
        );

        // --- METRICS DASHBOARD ---
        HBox metricsBox = new HBox(15);
        VBox profitCard = createInfoCard("Net Profit", "$0.00");
        VBox winRateCard = createInfoCard("Win Rate", "0.00%");
        VBox mddCard = createInfoCard("Max Drawdown", "0.00%");
        VBox cagrCard = createInfoCard("CAGR", "0.00%");
        metricsBox.getChildren().addAll(profitCard, winRateCard, mddCard, cagrCard);

        // --- TABBED OUTPUT ---
        TabPane outputTabs = new TabPane();
        VBox.setVgrow(outputTabs, Priority.ALWAYS);

        // Tab A: Logs
        Tab logTab = new Tab("Execution Logs");
        logTab.setClosable(false);
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.getStyleClass().add("text-area");
        logTab.setContent(logArea);

        // Tab B: Equity Curve
        Tab equityTab = new Tab("Equity Curve");
        equityTab.setClosable(false);
        VBox equityBox = new VBox();
        equityBox.setAlignment(Pos.CENTER);
        equityBox.getStyleClass().add("card");
        equityBox.getChildren().add(new Label("Run backtest to generate equity curve."));
        equityTab.setContent(equityBox);

        // Tab C: Strategy Chart (Price + MAs)
        Tab strategyChartTab = new Tab("Strategy Chart");
        strategyChartTab.setClosable(false);
        VBox strategyChartBox = new VBox();
        strategyChartBox.setAlignment(Pos.CENTER);
        strategyChartBox.getStyleClass().add("card");
        strategyChartBox.getChildren().add(new Label("Run backtest to view indicator overlays."));
        strategyChartTab.setContent(strategyChartBox);

        outputTabs.getTabs().addAll(logTab, equityTab, strategyChartTab);

        // --- QUANT ENGINE LOGIC ---
        runButton.setOnAction(e -> {
            String ticker = tickerCombo.getValue();
            String filepath = ticker + "_test.csv";

            try {
                double initialCapital = Double.parseDouble(capitalField.getText());
                myPortfolio = new Portfolio(initialCapital);
                refreshDashboard("", 0.0);

                logArea.clear();
                logArea.appendText("Initializing Institutional Engine for " + ticker + "...\n");
                logArea.appendText("Modeling constraints: 0.1% Slippage/Fees, 95% Capital Deployment.\n\n");

                BarSeries series = StockDataLoader.loadCsvSeries(filepath, ticker);
                if (series != null && series.getBarCount() > 0) {
                    
                    String selectedType = indicatorCombo.getValue();
                    int shortDays = shortSpinner.getValue();
                    int longDays = longSpinner.getValue();

                    Strategy strategy = CustomStrategy.build(series, selectedType, shortDays, longDays);
                    
                    // --- 1. THE COST MODEL UPGRADE ---
                    // This forces ta4j to apply a 0.1% penalty to every single entry and exit 
                    // to simulate real-world slippage and exchange fees.
                    org.ta4j.core.analysis.cost.CostModel transactionCostModel = new org.ta4j.core.analysis.cost.LinearTransactionCostModel(0.001);
                    org.ta4j.core.analysis.cost.CostModel zeroHoldingCost = new org.ta4j.core.analysis.cost.ZeroCostModel();
                    
                    BarSeriesManager seriesManager = new BarSeriesManager(series, transactionCostModel, zeroHoldingCost);
                    TradingRecord tradingRecord = seriesManager.run(strategy);

                    int winningTrades = 0;
                    int losingTrades = 0;
                    double peakEquity = initialCapital;
                    double maxDrawdown = 0.0;
                    
                    XYChart.Series<String, Number> equityData = new XYChart.Series<>();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

                    logArea.appendText("Simulation complete. Trades Executed: " + tradingRecord.getPositionCount() + "\n");
                    logArea.appendText("--------------------------------------------------\n");

                    for (Position position : tradingRecord.getPositions()) {
                        if (position.isClosed()) {
                            // Because we use a CostModel, the "NetPrice" now accurately includes slippage!
                            double rawEntryPrice = position.getEntry().getPricePerAsset().doubleValue();
                            double actualEntryPrice = position.getEntry().getNetPrice().doubleValue(); 
                            
                            double rawExitPrice = position.getExit().getPricePerAsset().doubleValue();
                            double actualExitPrice = position.getExit().getNetPrice().doubleValue();

                            // --- 2. POSITION SIZING UPGRADE ---
                            // Only deploy 95% of available cash to mimic institutional risk margins
                            double usableCapital = myPortfolio.getCashBalance() * 0.95;
                            int sharesToBuy = (int) (usableCapital / actualEntryPrice);

                            if (sharesToBuy > 0) {
                                myPortfolio.buy(ticker, sharesToBuy, actualEntryPrice);
                                myPortfolio.sell(ticker, sharesToBuy, actualExitPrice);
                                
                                // Calculate true Net Profit (after slippage/fees)
                                double netProfit = (actualExitPrice - actualEntryPrice) * sharesToBuy;
                                if (netProfit > 0) winningTrades++; else losingTrades++;

                                double currentEquity = myPortfolio.getCashBalance();
                                if (currentEquity > peakEquity) peakEquity = currentEquity;
                                double drawdown = (peakEquity - currentEquity) / peakEquity;
                                if (drawdown > maxDrawdown) maxDrawdown = drawdown;

                                String exitDate = series.getBar(position.getExit().getIndex()).getEndTime().format(formatter);
                                equityData.getData().add(new XYChart.Data<>(exitDate, currentEquity));

                                logArea.appendText(String.format("[%s] CLOSED: %d shares\n", exitDate, sharesToBuy));
                                logArea.appendText(String.format("    Market Entry: $%.2f -> Actual Fill (w/ Slippage): $%.2f\n", rawEntryPrice, actualEntryPrice));
                                logArea.appendText(String.format("    Market Exit:  $%.2f -> Actual Fill (w/ Slippage): $%.2f\n", rawExitPrice, actualExitPrice));
                                logArea.appendText(String.format("    Net P&L: $%.2f\n", netProfit));
                            }
                        }
                    }

                    // --- CALCULATE FINAL METRICS ---
                    double finalEquity = myPortfolio.getCashBalance();
                    double totalNetProfit = finalEquity - initialCapital;
                    int totalTrades = winningTrades + losingTrades;
                    double winRate = totalTrades > 0 ? ((double) winningTrades / totalTrades) * 100 : 0.0;
                    
                    double years = series.getBarCount() / 252.0;
                    double cagr = (Math.pow((finalEquity / initialCapital), (1.0 / years)) - 1) * 100;

                    // Update UI Cards
                    ((Label) profitCard.getChildren().get(1)).setText(String.format("$%,.2f", totalNetProfit));
                    ((Label) winRateCard.getChildren().get(1)).setText(String.format("%.2f%%", winRate));
                    ((Label) mddCard.getChildren().get(1)).setText(String.format("%.2f%%", maxDrawdown * 100));
                    ((Label) cagrCard.getChildren().get(1)).setText(String.format("%.2f%%", cagr));

                    setStyleBasedOnProfit(profitCard, totalNetProfit);
                    setStyleBasedOnProfit(cagrCard, cagr);

                    // Draw Equity Curve
                    equityBox.getChildren().clear();
                    CategoryAxis xAxis = new CategoryAxis();
                    NumberAxis yAxis = new NumberAxis();
                    yAxis.setAutoRanging(true);
                    yAxis.setForceZeroInRange(false);
                    
                    LineChart<String, Number> equityChart = new LineChart<>(xAxis, yAxis);
                    equityChart.setCreateSymbols(false);
                    equityChart.setLegendVisible(false);
                    equityChart.getData().add(equityData);
                    
                    VBox.setVgrow(equityChart, Priority.ALWAYS);
                    equityBox.getChildren().add(equityChart);

                    // Draw Strategy Chart with MAs
                    strategyChartBox.getChildren().clear();
                    LineChart<String, Number> stratChart = createJavaFXChart(series, ticker, shortDays, longDays, selectedType);
                    VBox.setVgrow(stratChart, Priority.ALWAYS);
                    strategyChartBox.getChildren().add(stratChart);

                    logArea.appendText("--------------------------------------------------\n");
                    logArea.appendText("Engine stopped. Review institutional metrics.\n");

                }
            } catch (Exception ex) {
                logArea.appendText("ERROR: " + ex.getMessage() + "\n");
                ex.printStackTrace();
            }
            });

        page.getChildren().addAll(title, controlCard, metricsBox, outputTabs);
        
        return page;
    }

    // =========================================================================
    // CHART GENERATORS & HELPERS
    // =========================================================================
    
    // Generator 1: Standard Price Chart (For Market Data Page)
    private LineChart<String, Number> createJavaFXChart(BarSeries series, String ticker) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setAnimated(false); 
        xAxis.setTickLabelRotation(45); 

        NumberAxis yAxis = new NumberAxis();
        yAxis.setAutoRanging(true); 
        yAxis.setForceZeroInRange(false); 
        yAxis.setMinorTickVisible(false); 

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setCreateSymbols(false); 
        lineChart.setLegendVisible(false); 
        lineChart.setAnimated(false); 

        XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        for (int i = 0; i < series.getBarCount(); i++) {
            String dateStr = series.getBar(i).getEndTime().format(formatter);
            double closePrice = series.getBar(i).getClosePrice().doubleValue();
            priceSeries.getData().add(new XYChart.Data<>(dateStr, closePrice));
        }

        lineChart.getData().add(priceSeries);
        return lineChart;
    }

    // Generator 2: Strategy Chart with Indicator Overlays (For Terminal)
    private LineChart<String, Number> createJavaFXChart(BarSeries series, String ticker, int shortDays, int longDays, String indicatorType) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setAnimated(false);
        xAxis.setTickLabelRotation(45);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(false);
        yAxis.setMinorTickVisible(false);

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setCreateSymbols(false);
        lineChart.setLegendVisible(true); 
        lineChart.setAnimated(false);

        XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
        priceSeries.setName("Price");

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        Indicator<Num> fastIndicator;
        Indicator<Num> slowIndicator;

        if (indicatorType.contains("EMA")) {
            fastIndicator = new EMAIndicator(closePrice, shortDays);
            slowIndicator = new EMAIndicator(closePrice, longDays);
        } else {
            fastIndicator = new SMAIndicator(closePrice, shortDays);
            slowIndicator = new SMAIndicator(closePrice, longDays);
        }

        XYChart.Series<String, Number> fastSeries = new XYChart.Series<>();
        fastSeries.setName("Fast " + indicatorType.substring(0, 3) + " (" + shortDays + ")");
        
        XYChart.Series<String, Number> slowSeries = new XYChart.Series<>();
        slowSeries.setName("Slow " + indicatorType.substring(0, 3) + " (" + longDays + ")");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        for (int i = 0; i < series.getBarCount(); i++) {
            String dateStr = series.getBar(i).getEndTime().format(formatter);
            double close = series.getBar(i).getClosePrice().doubleValue();
            double fastVal = fastIndicator.getValue(i).doubleValue();
            double slowVal = slowIndicator.getValue(i).doubleValue();

            priceSeries.getData().add(new XYChart.Data<>(dateStr, close));
            if (i >= shortDays) fastSeries.getData().add(new XYChart.Data<>(dateStr, fastVal));
            if (i >= longDays) slowSeries.getData().add(new XYChart.Data<>(dateStr, slowVal));
        }

        lineChart.getData().addAll(priceSeries, fastSeries, slowSeries);
        return lineChart;
    }

    private VBox createInfoCard(String title, String value) {
        VBox card = new VBox(5);
        card.getStyleClass().add("card");
        HBox.setHgrow(card, Priority.ALWAYS); 
        
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("label-text");
        
        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");
        
        card.getChildren().addAll(lblTitle, lblValue);
        return card;
    }

    private void setStyleBasedOnProfit(VBox card, double value) {
        Label valueLabel = (Label) card.getChildren().get(1);
        if (value > 0) {
            valueLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #00E676; -fx-font-weight: bold;"); 
        } else if (value < 0) {
            valueLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #FF1744; -fx-font-weight: bold;"); 
        } else {
            valueLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");
        }
    }

    // =========================================================================
    // PAGE 4: PORTFOLIO DASHBOARD
    // =========================================================================
    private VBox createPortfolioDashboard() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(30));

        Label title = new Label("Live Portfolio");
        title.getStyleClass().add("header-text");

        HBox summaryBox = new HBox(20);
        VBox cashCard = createInfoCard("Available Cash", "$10,000.00");
        cashLabel = (Label) cashCard.getChildren().get(1); 
        
        VBox equityCard = createInfoCard("Total Account Value", "$10,000.00");
        equityLabel = (Label) equityCard.getChildren().get(1);

        summaryBox.getChildren().addAll(cashCard, equityCard);

        portfolioTable = new TableView<>();
        VBox.setVgrow(portfolioTable, Priority.ALWAYS);
        portfolioTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Holding, String> tickerCol = new TableColumn<>("Ticker");
        tickerCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTicker()));
        
        TableColumn<Holding, String> sharesCol = new TableColumn<>("Shares");
        sharesCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getShares())));
        
        TableColumn<Holding, String> priceCol = new TableColumn<>("Market Price");
        priceCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPrice()));
        
        TableColumn<Holding, String> valueCol = new TableColumn<>("Total Value");
        valueCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValue()));
        
        portfolioTable.getColumns().add(tickerCol);
        portfolioTable.getColumns().add(sharesCol);
        portfolioTable.getColumns().add(priceCol);
        portfolioTable.getColumns().add(valueCol);

        page.getChildren().addAll(title, summaryBox, portfolioTable);
        return page;
    }

    private void refreshDashboard(String currentTicker, double currentMarketPrice) {
        if (myPortfolio == null) return;

        double cash = myPortfolio.getCashBalance();
        cashLabel.setText(String.format("$%,.2f", cash));

        portfolioTable.getItems().clear();
        double totalHoldingsValue = 0.0;
        
        Map<String, Integer> holdings = myPortfolio.getHoldings();
        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
            String ticker = entry.getKey();
            int shares = entry.getValue();
            
            double price = ticker.equals(currentTicker) ? currentMarketPrice : 0.0; 
            double value = shares * price;
            totalHoldingsValue += value;

            portfolioTable.getItems().add(new Holding(
                ticker, shares, String.format("$%,.2f", price), String.format("$%,.2f", value)
            ));
        }

        double totalEquity = cash + totalHoldingsValue;
        equityLabel.setText(String.format("$%,.2f", totalEquity));
    }

    public static class Holding {
        private String ticker;
        private int shares;
        private String price;
        private String value;

        public Holding(String ticker, int shares, String price, String value) {
            this.ticker = ticker;
            this.shares = shares;
            this.price = price;
            this.value = value;
        }
        public String getTicker() { return ticker; }
        public int getShares() { return shares; }
        public String getPrice() { return price; }
        public String getValue() { return value; }
    }

    public static void main(String[] args) {
        launch(args);
    }
}