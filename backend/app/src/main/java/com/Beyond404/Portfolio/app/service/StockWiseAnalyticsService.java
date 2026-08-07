package com.Beyond404.Portfolio.app.service;

import com.Beyond404.Portfolio.app.model.MarketQuote;
import com.Beyond404.Portfolio.app.model.PortfolioData;
import com.Beyond404.Portfolio.app.model.StockwiseAnalytics;
import com.Beyond404.Portfolio.app.portfolioanalysis.PortfolioAnalyzer;
import com.Beyond404.Portfolio.app.repository.StockWiseAnalyticsRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@Service
public class StockWiseAnalyticsService {


    private final StockWiseAnalyticsRepository stockWiseAnalyticsRepository;

    private final PortfolioAnalyzer portfolioAnalyzer;

    private final MarketDataService marketDataService;



    public StockWiseAnalyticsService(
            StockWiseAnalyticsRepository stockWiseAnalyticsRepository,
            PortfolioAnalyzer portfolioAnalyzer,
            MarketDataService marketDataService) {


        this.stockWiseAnalyticsRepository =
                stockWiseAnalyticsRepository;


        this.portfolioAnalyzer =
                portfolioAnalyzer;


        this.marketDataService =
                marketDataService;

    }



    public ArrayList<StockwiseAnalytics> getStockWiseAnalytics(
            Long customerId) {


        ArrayList<PortfolioData> portfolio =
                stockWiseAnalyticsRepository
                        .getCustomerPortfolio(customerId);



        ArrayList<StockwiseAnalytics> output =
                new ArrayList<>();


        if(portfolio.isEmpty()) {

            return output;

        }



        Map<String, Double> holdings =
                portfolioAnalyzer
                        .getStockHoldings(portfolio);



        Map<String, Double> investedByTicker =
                portfolioAnalyzer
                        .getNetInvestedByStock(portfolio);



        Map<String, PortfolioData> stockMeta =
                new HashMap<>();


        for(PortfolioData row : portfolio) {


            stockMeta.putIfAbsent(
                    row.getTicker(),
                    row
            );

        }



        for(Map.Entry<String, Double> entry :
                holdings.entrySet()) {



            String ticker =
                    entry.getKey();



            double netQty =
                    entry.getValue();



            if(netQty <= 0) {

                continue;

            }



            PortfolioData meta =
                    stockMeta.get(ticker);



            if(meta == null) {

                continue;

            }



            double invested =
                    investedByTicker
                            .getOrDefault(
                                    ticker,
                                    0.0
                            );



            double lastPrice = 0.0;

            double prevPrice = 0.0;

            String volume = "";

            String marketCap = "";



            try {


                MarketQuote quote =
                        marketDataService.getQuote(
                                ticker,
                                meta.getStockMarket()
                        );



                if(quote != null &&
                        quote.getPrice() != null) {


                    /*
                     * Already normalized to USD
                     */
                    lastPrice =
                            quote.getPrice();



                    prevPrice = quote.getPreviousClose() != null ? quote.getPreviousClose() : 0.0;



                    volume =
                            formatLargeNumber(
                                    quote.getVolume()
                            );

                }



                /*
                 * Market cap is currently
                 * unavailable from API
                 */
                marketCap = "";



            }
            catch(Exception e) {


//                System.out.println(
//                        "Quote unavailable for "
//                                + ticker
//                                + ": "
//                                + e.getMessage()
//                );

            }



            double currentValue =
                    netQty *
                            lastPrice;



            double pnl =
                    currentValue -
                            invested;



            double pnlPercent =
                    invested == 0
                            ?
                            0.0
                            :
                            (pnl / invested) * 100.0;



            StockwiseAnalytics item =
                    new StockwiseAnalytics();



            item.setStockId(
                    meta.getStockId()
            );


            item.setTicker(
                    meta.getTicker()
            );


            item.setCompanyName(
                    meta.getStockName()
            );



            item.setInvested(
                    (int)
                            Math.round(invested)
            );



            item.setCurrentValue(
                    (int)
                            Math.round(currentValue)
            );



            item.setPnl(
                    (int)
                            Math.round(pnl)
            );



            item.setPnlPercent(
                    round2(pnlPercent)
            );



            item.setLastPrice(
                    round2(lastPrice)
            );



            item.setPrevPrice(
                    round2(prevPrice)
            );



            item.setMarketCap(
                    marketCap
            );



            item.setVolume(
                    volume
            );



            output.add(item);

        }



        return output;

    }




    private String formatLargeNumber(
            Object value) {


        if(value == null) {

            return "";

        }



        double number =
                asDouble(value);



        if(number >= 1_000_000_000_000.0) {

            return round1(
                    number / 1_000_000_000_000.0
            )
                    + "T";

        }



        if(number >= 1_000_000_000.0) {

            return round1(
                    number / 1_000_000_000.0
            )
                    + "B";

        }



        if(number >= 1_000_000.0) {

            return round1(
                    number / 1_000_000.0
            )
                    + "M";

        }



        if(number >= 1_000.0) {

            return round1(
                    number / 1_000.0
            )
                    + "K";

        }



        return String.valueOf(
                (long) number
        );

    }




    private double asDouble(
            Object value) {


        if(value == null) {

            return 0.0;

        }


        if(value instanceof Number number) {

            return number.doubleValue();

        }



        try {

            return Double.parseDouble(
                    value.toString()
            );

        }
        catch(NumberFormatException e) {

            return 0.0;

        }

    }




    private String round1(
            double value) {


        return BigDecimal
                .valueOf(value)
                .setScale(
                        1,
                        RoundingMode.HALF_UP
                )
                .toPlainString();

    }




    private BigDecimal round2(
            double value) {


        return BigDecimal
                .valueOf(value)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );

    }

}