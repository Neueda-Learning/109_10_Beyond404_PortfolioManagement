package com.Beyond404.Portfolio.app.portfolioanalysis;

import com.Beyond404.Portfolio.app.model.MarketQuote;
import com.Beyond404.Portfolio.app.model.PortfolioAnalysisResponse;
import com.Beyond404.Portfolio.app.model.PortfolioData;
import com.Beyond404.Portfolio.app.service.MarketDataService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PortfolioAnalyzer {

    private final MarketDataService marketDataService;

    public PortfolioAnalyzer(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public double calculateTotalInvestment(
            List<PortfolioData> portfolio) {


        double total = 0;


        for (PortfolioData data : portfolio) {


            if (data.getTransactionType()
                    .equalsIgnoreCase("BUY")) {


                total += calculateTransactionValue(data);

            }
        }


        return total;
    }

    public double calculateTotalSellValue(
            List<PortfolioData> portfolio) {


        double total = 0;


        for (PortfolioData data : portfolio) {


            if (data.getTransactionType()
                    .equalsIgnoreCase("SELL")) {


                total += calculateTransactionValue(data);

            }
        }


        return total;
    }

    /*
     * Calculates current quantity held for every stock.
     */
    private Map<String, Double> calculateStockHoldings(
            List<PortfolioData> portfolio) {

        Map<String, Double> holdings = new HashMap<>();

        for (PortfolioData data : portfolio) {

            String ticker = data.getTicker();

            double quantity = data.getQuantity();

            if (data.getTransactionType()
                    .equalsIgnoreCase("BUY")) {

                holdings.put(
                        ticker,
                        holdings.getOrDefault(ticker, 0.0)
                                + quantity
                );

            } else {

                holdings.put(
                        ticker,
                        holdings.getOrDefault(ticker, 0.0)
                                - quantity
                );
            }
        }

        return holdings;
    }

    /*
     * Calculates average buying price of each stock.
     */
    private Map<String, Double> calculateAverageBuyPrice(
            List<PortfolioData> portfolio) {


        Map<String, Double> totalAmount =
                new HashMap<>();

        Map<String, Double> totalQuantity =
                new HashMap<>();


        for (PortfolioData data : portfolio) {


            if(data.getTransactionType()
                    .equalsIgnoreCase("BUY")) {


                String ticker =
                        data.getTicker();


                double value =
                        calculateTransactionValue(data);



                totalAmount.put(
                        ticker,
                        totalAmount.getOrDefault(
                                ticker,
                                0.0
                        )
                                +
                                value
                );


                totalQuantity.put(
                        ticker,
                        totalQuantity.getOrDefault(
                                ticker,
                                0.0
                        )
                                +
                                data.getQuantity()
                );
            }
        }


        Map<String, Double> averagePrice =
                new HashMap<>();


        for(String ticker : totalAmount.keySet()) {


            averagePrice.put(
                    ticker,
                    totalAmount.get(ticker)
                            /
                            totalQuantity.get(ticker)
            );

        }


        return averagePrice;
    }

    /*
     * Cost of remaining holdings.
     */
    private double calculateRemainingCost(
            List<PortfolioData> portfolio) {

        Map<String, Double> holdings =
                calculateStockHoldings(portfolio);

        Map<String, Double> averagePrice =
                calculateAverageBuyPrice(portfolio);

        double cost = 0;

        for (String ticker : holdings.keySet()) {

            double quantity = holdings.get(ticker);

            if (quantity > 0) {

                cost += quantity *
                        averagePrice.get(ticker);
            }
        }

        return cost;
    }

    /*
     * Current market value using live prices.
     */
    private Map<String, PortfolioData> createStockMap(
            List<PortfolioData> portfolio) {


        Map<String, PortfolioData> stockMap =
                new HashMap<>();


        for(PortfolioData data : portfolio) {


            stockMap.put(
                    data.getTicker()
                            .toUpperCase(),
                    data
            );

        }


        return stockMap;

    }

    public double calculateCurrentValue(
            List<PortfolioData> portfolio) {

        double currentValue = 0;

        Map<String, Double> holdings =
                calculateStockHoldings(portfolio);

        Map<String, PortfolioData> stockMap =
                createStockMap(portfolio);


        for (String ticker : holdings.keySet()) {

            double quantity =
                    holdings.get(ticker);


            PortfolioData stock =
                    stockMap.get(ticker.toUpperCase());


            if (stock == null) {
                continue;
            }


            MarketQuote quote =
                    marketDataService.getQuote(
                            ticker,
                            stock.getStockMarket()
                    );


            if (quote != null &&
                    quote.getPrice() != null) {

                double priceInUSD =
                        quote.getPrice();


                double stockValue =
                        quantity * priceInUSD;

                currentValue += stockValue;

            }
        }
        return currentValue;
    }

    public Map<String, Double> getCurrentStockPrices(
            List<PortfolioData> portfolio) {

        Map<String, Double> prices = new HashMap<>();

        Map<String, Double> holdings =
                calculateStockHoldings(portfolio);

        Map<String, PortfolioData> stockMap =
                createStockMap(portfolio);

        for (String ticker : holdings.keySet()) {

            PortfolioData stock =
                    stockMap.get(
                            ticker.toUpperCase()
                    );

            if (stock == null) {
                continue;
            }

            try {

                MarketQuote quote =
                        marketDataService.getQuote(
                                ticker,
                                stock.getStockMarket()
                        );

                if (quote != null &&
                        quote.getPrice() != null) {

                    double priceInUSD =
                            quote.getPrice();


                    prices.put(
                            ticker,
                            priceInUSD
                    );
                }

            } catch (Exception e) {

//                System.out.println(
//                        "Price unavailable for "
//                                + ticker
//                );
            }
        }

        return prices;
    }

    private PortfolioData getStockData(
            List<PortfolioData> portfolio,
            String ticker) {

        for (PortfolioData data : portfolio) {

            if (data.getTicker()
                    .equalsIgnoreCase(ticker)) {

                return data;
            }
        }

        return null;
    }

    private double calculateTransactionValue(
            PortfolioData transaction) {


        Double historicalPrice =
                marketDataService.getHistoricalPrice(
                        transaction.getTicker(),
                        transaction.getTransactionTimestamp()
                );


        if (historicalPrice == null) {

            return 0;

        }


        double value =
                transaction.getQuantity()
                        *
                        historicalPrice;

// historicalPrice is already normalized to USD in MarketDataService#getHistoricalPrice
        return value;

    }

    private String getCurrencyFromMarket(
            String market) {


        if (market.equalsIgnoreCase("NSE")
                || market.equalsIgnoreCase("BSE")) {

            return "INR";
        }


        if (market.equalsIgnoreCase("NASDAQ")
                || market.equalsIgnoreCase("NYSE")) {

            return "USD";
        }


        if (market.equalsIgnoreCase("EURONEXT")) {

            return "EUR";
        }


        return "USD";
    }

    public int calculateTotalTransactions(
            List<PortfolioData> portfolio) {

        return portfolio.size();
    }

    public int calculateBuyTransactions(
            List<PortfolioData> portfolio) {

        int count = 0;

        for (PortfolioData data : portfolio) {

            if (data.getTransactionType()
                    .equalsIgnoreCase("BUY")) {

                count++;
            }
        }

        return count;
    }

    public int calculateSellTransactions(
            List<PortfolioData> portfolio) {

        int count = 0;

        for (PortfolioData data : portfolio) {

            if (data.getTransactionType()
                    .equalsIgnoreCase("SELL")) {

                count++;
            }
        }

        return count;
    }

    public Map<String, Double> getStockHoldings(List<PortfolioData> portfolio) {
    return calculateStockHoldings(portfolio);
}

    public Map<String, Double> getNetInvestedByStock(
            List<PortfolioData> portfolio) {


        Map<String, Double> invested =
                new HashMap<>();


        for(PortfolioData tx : portfolio) {


            double amount =
                    calculateTransactionValue(tx);


            String ticker =
                    tx.getTicker();



            if(tx.getTransactionType()
                    .equalsIgnoreCase("BUY")) {


                invested.put(
                        ticker,
                        invested.getOrDefault(
                                ticker,
                                0.0
                        )
                                +
                                amount
                );


            } else {


                invested.put(
                        ticker,
                        invested.getOrDefault(
                                ticker,
                                0.0
                        )
                                -
                                amount
                );
            }

        }


        return invested;
    }






    public double calculateCurrentHoldings(
            List<PortfolioData> portfolio) {

        double quantity = 0;

        for (PortfolioData data : portfolio) {

            if (data.getTransactionType()
                    .equalsIgnoreCase("BUY")) {

                quantity += data.getQuantity();

            } else {

                quantity -= data.getQuantity();
            }
        }

        return quantity;
    }

    public Map<String, Double> calculateMarketDistribution(
            List<PortfolioData> portfolio) {

        Map<String, Double> distribution =
                new HashMap<>();

        double totalInvestment =
                calculateTotalInvestment(portfolio);

        for (PortfolioData data : portfolio) {

            if (data.getTransactionType()
                    .equalsIgnoreCase("BUY")) {

                String market =
                        data.getStockMarket();

                distribution.put(
                        market,
                        distribution.getOrDefault(
                                market,
                                0.0
                        )
                                +
                                (calculateTransactionValue(data)
                                        /
                                        totalInvestment)
                                        * 100
                );
            }
        }

        return distribution;
    }

    public int calculateUniqueStocks(
            List<PortfolioData> portfolio) {

        Map<Long, Boolean> stocks =
                new HashMap<>();

        for (PortfolioData data : portfolio) {

            stocks.put(
                    data.getStockId(),
                    true
            );
        }

        return stocks.size();
    }

    public double calculateAverageInvestment(
            List<PortfolioData> portfolio) {


        double total = 0;

        int count = 0;


        for(PortfolioData data : portfolio) {


            if(data.getTransactionType()
                    .equalsIgnoreCase("BUY")) {


                total += calculateTransactionValue(data);

                count++;

            }

        }


        return count == 0
                ? 0
                :
                total / count;
    }

    public PortfolioAnalysisResponse analyzePortfolio(
            List<PortfolioData> portfolio) {

        if (portfolio.isEmpty()) {
            return null;
        }

        PortfolioData customer =
                portfolio.get(0);

        double invested =
                calculateTotalInvestment(portfolio);

        double sold =
                calculateTotalSellValue(portfolio);

        double currentValue =
                calculateCurrentValue(portfolio);

        double remainingCost =
                calculateRemainingCost(portfolio);

        double profitLoss =
                currentValue - remainingCost;

        double returnPercentage =
                remainingCost == 0
                        ? 0
                        :
                        (profitLoss / remainingCost) * 100;

        return new PortfolioAnalysisResponse(

                customer.getCustomerId(),

                customer.getCustomerName(),

                customer.getRiskLevel(),

                "USD",

                invested,

                sold,

                currentValue,

                getCurrentStockPrices(portfolio),

                Math.max(profitLoss, 0),

                Math.max(-profitLoss, 0),

                profitLoss,

                returnPercentage,

                calculateCurrentHoldings(portfolio),

                calculateTotalTransactions(portfolio),

                calculateBuyTransactions(portfolio),

                calculateSellTransactions(portfolio),

                calculateUniqueStocks(portfolio),

                calculateAverageInvestment(portfolio),

                calculateMarketDistribution(portfolio)
        );
    }
}