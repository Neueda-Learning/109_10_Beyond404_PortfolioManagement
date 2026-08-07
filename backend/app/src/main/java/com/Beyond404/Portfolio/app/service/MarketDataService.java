package com.Beyond404.Portfolio.app.service;

import com.Beyond404.Portfolio.app.model.ChartDataPoint;
import com.Beyond404.Portfolio.app.model.ChartDataResponse;
import com.Beyond404.Portfolio.app.model.FastApiCandleData;
import com.Beyond404.Portfolio.app.model.FastApiMarketHistoryResponse;
import com.Beyond404.Portfolio.app.model.FastApiQuoteResponse;
import com.Beyond404.Portfolio.app.model.MarketSearchResponse;

import com.Beyond404.Portfolio.app.model.MarketQuote;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.Beyond404.Portfolio.app.model.MarketSearchResult;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
//<<<<<<< HEAD
//=======
//
//>>>>>>> cfacdff206a7956eb8e54a446ab672bec1be187d
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class MarketDataService {

    private static final Set<String> SUPPORTED_RANGES = Set.of("1D", "1W", "1M", "1Y");

    @Value("${market.data.base-url:http://localhost:8000}")
    private String marketDataBaseUrl;

    @Value("${currency.api.base-url:https://api.frankfurter.app}")
    private String currencyApiBaseUrl;

    private final RestTemplate restTemplate;

    public MarketDataService(RestTemplate restTemplate) {

        this.restTemplate = restTemplate;

    }

    /**
     * Search stock ticker/company symbols
     *
     * API:
     * GET /api/v1/market/search
     */
    public MarketSearchResponse searchSymbols(
            String companyName) {

        String url = UriComponentsBuilder
                .fromUriString(
                        marketDataBaseUrl
                                + "/api/v1/market/search")
                .queryParam(
                        "query",
                        companyName)
                .toUriString();

        return restTemplate.getForObject(
                url,
                MarketSearchResponse.class);
    }

    /**
     * Get latest stock price and quote details
     *
     * API:
     * GET /api/v1/market/quote
     */
    public MarketQuote getQuote(
            String ticker,
            String market) {


        String url = UriComponentsBuilder
                .fromUriString(
                        marketDataBaseUrl
                                + "/api/v1/market/quote")
                .queryParam(
                        "symbol",
                        ticker)
                .toUriString();



        MarketQuote quote =
                restTemplate.getForObject(
                        url,
                        MarketQuote.class);



        if(quote != null &&
                quote.getPrice() != null) {


            Double usdPrice =
                    normalizePriceToUSD(
                            quote.getPrice(),
                            ticker
                    );


            quote.setPrice(
                    usdPrice
            );

            Double usdPreviousClose =
                    normalizePriceToUSD(
                            quote.getPreviousClose(),
                            ticker
                    );


            quote.setPreviousClose(
                    usdPreviousClose
            );

        }


        return quote;

    }

    /**
     * Converts any currency amount into USD
     *
     * Example:
     * EUR -> USD
     * INR -> USD
     * USD -> USD (no conversion)
     */
    public double convertToUSD(
            double amount,
            String currency) {


        if(currency == null ||
                currency.equalsIgnoreCase("USD")) {

            return amount;
        }


        try {


            String url = UriComponentsBuilder
                    .fromUriString(
                            currencyApiBaseUrl
                                    + "/latest"
                    )
                    .queryParam(
                            "from",
                            currency
                    )
                    .queryParam(
                            "to",
                            "USD"
                    )
                    .toUriString();



            Map response =
                    restTemplate.getForObject(
                            url,
                            Map.class
                    );


            Map rates =
                    (Map) response.get(
                            "rates"
                    );


            Double exchangeRate =
                    Double.valueOf(
                            rates.get("USD")
                                    .toString()
                    );


//            System.out.println(
//                    currency
//                            + " conversion rate: "
//                            + exchangeRate
//            );


            double convertedAmount =
                    amount * exchangeRate;


//            System.out.println(
//                    amount
//                            + " "
//                            + currency
//                            + " -> "
//                            + convertedAmount
//                            + " USD"
//            );


            return convertedAmount;


        }
        catch(Exception e) {


//            System.out.println(
//                    "Currency conversion failed for "
//                            + currency
//            );


            return amount;
        }

    }

    private Double normalizePriceToUSD(
            Double price,
            String ticker) {


        if(price == null) {
            return 0.0;
        }


        String currency =
                getCurrencyFromTicker(ticker);


        return convertToUSD(
                price,
                currency
        );

    }

    private String getCurrencyFromTicker(
            String ticker) {


        if(ticker == null) {

            return "USD";

        }


        if(ticker.endsWith(".NS")
                || ticker.endsWith(".BO")) {

            return "INR";

        }


        if(ticker.endsWith(".PA")
                || ticker.endsWith(".AS")
                || ticker.endsWith(".DE")) {

            return "EUR";

        }


        return "USD";

    }

    /**
     * Get historical stock market data
     *
     * API:
     * GET /api/v1/market/history
     */
    public Map<String, Object> getHistoricalData(
            String ticker,
            String market,
            String startDate,
            String endDate) {


        String url = UriComponentsBuilder
                .fromUriString(
                        marketDataBaseUrl
                                + "/api/v1/market/history")
                .queryParam(
                        "symbol",
                        normalizeTicker(ticker))
                .queryParam(
                        "start",
                        startDate)
                .queryParam(
                        "end",
                        endDate)
                .toUriString();


        return restTemplate.getForObject(
                url,
                Map.class
        );

    }

    /**
     * Fetch historical closing price at transaction timestamp
     *
     * Used for calculating:
     *
     * Transaction Value =
     * Quantity × Historical Price
     */
    public Double getHistoricalPrice(
            String ticker,
            LocalDateTime transactionTime) {


        int[] ranges = {
                2,
                5,
                10,
                30
        };


        /*
         * First try original ticker
         */
        for (int days : ranges) {


            Double price =
                    fetchHistoricalPrice(
                            ticker,
                            transactionTime,
                            days
                    );


            if(price != null) {


                return normalizePriceToUSD(
                        price,
                        ticker
                );

            }

        }



        /*
         * Fallback only for BSE stocks
         *
         * Example:
         * RELIANCE.BO -> RELIANCE.NS
         */
        if(ticker.endsWith(".BO")) {


            String nseTicker =
                    ticker.replace(
                            ".BO",
                            ".NS"
                    );



//            System.out.println(
//                    "Trying NSE fallback for "
//                            + ticker
//                            + " -> "
//                            + nseTicker
//            );



            for (int days : ranges) {


                Double price =
                        fetchHistoricalPrice(
                                nseTicker,
                                transactionTime,
                                days
                        );


                if(price != null) {


                    return normalizePriceToUSD(
                            price,
                            nseTicker
                    );

                }

            }

        }



//        System.out.println(
//                "Historical price unavailable for "
//                        + ticker
//        );


        return null;

    }

    private Double fetchHistoricalPrice(
            String ticker,
            LocalDateTime transactionTime,
            int rangeDays) {


        try {


            LocalDateTime start =
                    transactionTime.minusDays(rangeDays);


            LocalDateTime end =
                    transactionTime.plusDays(rangeDays);

            LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
            if (end.isAfter(nowUtc)) {
                end = nowUtc;
            }
            if (!start.isBefore(end)) {
                return null;
            }



            String url =
                    UriComponentsBuilder
                            .fromUriString(
                                    marketDataBaseUrl
                                            + "/api/v1/market/history"
                            )
                            .queryParam(
                                    "symbol",
                                    ticker
                            )
                            .queryParam(
                                    "interval",
                                    "1d"
                            )
                            .queryParam(
                                    "start",
                                    start
                                            .atOffset(
                                                    ZoneOffset.UTC
                                            )
                                            .toString()
                            )
                            .queryParam(
                                    "end",
                                    end
                                            .atOffset(
                                                    ZoneOffset.UTC
                                            )
                                            .toString()
                            )
                            .queryParam(
                                    "adjusted",
                                    true
                            )
                            .toUriString();



            Map response =
                    restTemplate.getForObject(
                            url,
                            Map.class
                    );


            if(response == null) {

                return null;

            }


            List<Map<String,Object>> candles =
                    (List<Map<String,Object>>)
                            response.get("data");



            if(candles == null ||
                    candles.isEmpty()) {

                return null;

            }


            Map<String,Object> candle =
                    candles.get(0);



            return Double.parseDouble(
                    candle.get("close")
                            .toString()
            );


        }
        catch(Exception e) {

            return null;

        }
    }

    private Double extractHistoricalClosePrice(
            Map response) {


        if(response == null) {

            return null;

        }


        List<Map<String,Object>> candles =
                (List<Map<String,Object>>)
                        response.get(
                                "data"
                        );


    /*
       If your API returns "candles"
       instead of "data", use:

       response.get("candles")
    */


        if(candles == null ||
                candles.isEmpty()) {

            return null;

        }



        Map<String,Object> candle =
                candles.get(0);



        Object close =
                candle.get(
                        "close"
                );


        if(close == null) {

            return null;

        }


        return Double.parseDouble(
                close.toString()
        );

    }

    /**
     * Get recent candle data
     *
     * API:
     * GET /api/v1/market/recent
     */
    public Map<String, Object> getRecentData(
            String ticker,
            String market) {


        String url = UriComponentsBuilder
                .fromUriString(
                        marketDataBaseUrl
                                + "/api/v1/market/recent")
                .queryParam(
                        "symbol",
                        normalizeTicker(ticker))
                .toUriString();


        return restTemplate.getForObject(
                url,
                Map.class
        );

    }

    public ChartDataResponse getChartData(String ticker, String range) {
        String normalizedTicker = normalizeTicker(ticker);
        String normalizedRange = normalizeRange(range);
        String interval = intervalForRange(normalizedRange);
        int days = daysForRange(normalizedRange);

        try {
            FastApiQuoteResponse quote = fetchQuote(normalizedTicker);
            FastApiMarketHistoryResponse history = fetchRecentHistory(normalizedTicker, interval, days);

            ChartDataResponse response = new ChartDataResponse();
            response.setTickerId(normalizedTicker);
            response.setCompanyName(resolveCompanyName(normalizedTicker));
            response.setCurrency("USD");
            response.setCurrentPrice(
                    normalizePriceToUSD(
                            quote.getPrice(),
                            normalizedTicker
                    )
            );
            response.setPreviousClose(
                    normalizePriceToUSD(
                            quote.getPreviousClose(),
                            normalizedTicker
                    )
            );

            Map<String, List<ChartDataPoint>> ranges = ChartDataResponse.defaultRanges();
            ranges.put(
                    normalizedRange,
                    toChartDataPoints(
                            history.getData(),
                            normalizedRange,
                            normalizedTicker
                    )
            );
            response.setRanges(ranges);

            return response;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticker not found: " + normalizedTicker);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 400 || e.getStatusCode().value() == 422) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input for ticker/range");
            }
            throw e;

        }  catch (RestClientException e) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Market data server is unavailable"
        );
    }
    }


    private FastApiQuoteResponse fetchQuote(String ticker) {
        String url = UriComponentsBuilder
                .fromUriString(marketDataBaseUrl + "/api/v1/market/quote")
                .queryParam("symbol", ticker)
                .toUriString();

        FastApiQuoteResponse quote = restTemplate.getForObject(url, FastApiQuoteResponse.class);
        if (quote == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty quote response from market data server");
        }
        return quote;
    }

    private FastApiMarketHistoryResponse fetchRecentHistory(String ticker, String interval, int days) {
        String url = UriComponentsBuilder
                .fromUriString(marketDataBaseUrl + "/api/v1/market/recent")
                .queryParam("symbol", ticker)
                .queryParam("interval", interval)
                .queryParam("days", days)
                .toUriString();

        FastApiMarketHistoryResponse history = restTemplate.getForObject(url, FastApiMarketHistoryResponse.class);
        if (history == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty history response from market data server");
        }
        return history;
    }

    private String normalizeTicker(String ticker) {
        String normalizedTicker = ticker == null ? "" : ticker.trim().toUpperCase(Locale.ROOT);
        if (normalizedTicker.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticker is required");
        }
        return normalizedTicker;
    }

    private String normalizeRange(String range) {
        String normalizedRange = range == null ? "" : range.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_RANGES.contains(normalizedRange)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Range must be one of: 1D, 1W, 1M, 1Y");
        }
        return normalizedRange;
    }

    private String resolveCompanyName(String ticker) {
        try {
            MarketSearchResponse searchResponse = searchSymbols(ticker);
            if (searchResponse == null || searchResponse.getResults() == null) {
                return ticker;
            }

            for (MarketSearchResult result : searchResponse.getResults()) {
                if (result != null && ticker.equalsIgnoreCase(result.getSymbol()) && result.getName() != null) {
                    return result.getName();
                }
            }

            MarketSearchResult firstResult = searchResponse.getResults().stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            return firstResult != null && firstResult.getName() != null ? firstResult.getName() : ticker;
        } catch (RestClientException e) {
            return ticker;
        }
    }

    private List<ChartDataPoint> toChartDataPoints(
            List<FastApiCandleData> candles,
            String range,
            String ticker) {
        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChartDataPoint> points = new ArrayList<>();
        for (FastApiCandleData candle : candles) {
            if (candle == null || candle.getTimestamp() == null || candle.getClose() == null) {
                continue;
            }

            points.add(new ChartDataPoint(
                    candle.getTimestamp(),
                    formatDateLabel(candle.getTimestamp(), range),
                    normalizePriceToUSD(
                            candle.getClose(),
                            ticker
                    ),
                    candle.getVolume()));
        }

        return points;
    }

    private String formatDateLabel(String isoTimestamp, String range) {
        try {
            OffsetDateTime parsed = OffsetDateTime.parse(isoTimestamp);
            DateTimeFormatter formatter;

            switch (range) {
                case "1D" -> formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);
                case "1W", "1M" -> formatter = DateTimeFormatter.ofPattern("MMM dd", Locale.US);
                case "1Y" -> formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US);
                default -> formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            }

            return parsed.format(formatter);
        } catch (Exception e) {
            return isoTimestamp;
        }
    }

    private String intervalForRange(String range) {
        return switch (range) {
            case "1D" -> "60m";
            case "1W" -> "1d";
            case "1M" -> "1d";
            case "1Y" -> "1wk";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported range: " + range);
        };
    }

    private int daysForRange(String range) {
        return switch (range) {
            case "1D" -> 1;
            case "1W" -> 7;
            case "1M" -> 30;
            case "1Y" -> 365;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported range: " + range);
        };
    }
}