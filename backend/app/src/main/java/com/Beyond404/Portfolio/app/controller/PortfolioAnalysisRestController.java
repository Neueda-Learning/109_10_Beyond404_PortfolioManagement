package com.Beyond404.Portfolio.app.controller;

import com.Beyond404.Portfolio.app.model.PortfolioData;
import com.Beyond404.Portfolio.app.service.PortfolioAnalysisService;
import com.Beyond404.Portfolio.app.model.PortfolioAnalysisResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/beyond404/Portfolio/analysis")
public class PortfolioAnalysisRestController {

    private final PortfolioAnalysisService portfolioAnalysisService;

    public PortfolioAnalysisRestController(PortfolioAnalysisService portfolioAnalysisService) {
        this.portfolioAnalysisService = portfolioAnalysisService;
    }

    /**
     * Get complete portfolio of a customer
     *
     * Example:
     * GET /beyond404/recommendation/1/portfolio
     */
    // After
    @GetMapping("/{customerId}/portfolio")
    public ResponseEntity<List<PortfolioData>> getCustomerPortfolio(
            @PathVariable Long customerId) {


        List<PortfolioData> portfolio =
                portfolioAnalysisService
                        .getCustomerPortfolio(customerId);



        return ResponseEntity.ok(portfolio);
    }

    /**
     * Portfolio Value
     *
     * Example:
     * GET /beyond404/recommendation/1/value
     */
    @GetMapping("/{customerId}/value")
    public ResponseEntity<Double> getPortfolioValue(
            @PathVariable Long customerId) {

        return ResponseEntity.ok(
                portfolioAnalysisService.getPortfolioValue(customerId)
        );
    }

    /**
     * Total Transactions
     *
     * Example:
     * GET /beyond404/recommendation/1/transactions
     */
    @GetMapping("/{customerId}/transactions")
    public ResponseEntity<Integer> getTransactionCount(
            @PathVariable Long customerId) {

        return ResponseEntity.ok(
                portfolioAnalysisService.getTransactionCount(customerId)
        );
    }

    /**
     * Total BUY Transactions
     *
     * Example:
     * GET /beyond404/recommendation/1/buy
     */
    @GetMapping("/{customerId}/buy")
    public ResponseEntity<Integer> getBuyCount(
            @PathVariable Long customerId) {

        return ResponseEntity.ok(
                portfolioAnalysisService.getBuyCount(customerId)
        );
    }

    /**
     * Total SELL Transactions
     *
     * Example:
     * GET /beyond404/recommendation/1/sell
     */
    @GetMapping("/{customerId}/sell")
    public ResponseEntity<Integer> getSellCount(
            @PathVariable Long customerId) {

        return ResponseEntity.ok(
                portfolioAnalysisService.getSellCount(customerId)
        );
    }

    /**
     * Current Holdings
     *
     * Example:
     * GET /beyond404/recommendation/1/holdings
     */
    @GetMapping("/{customerId}/holdings")
    public ResponseEntity<Double> getCurrentHoldings(
            @PathVariable Long customerId) {

        return ResponseEntity.ok(
                portfolioAnalysisService.getCurrentHoldings(customerId)
        );
    }

    /**
     * Number of Unique Stocks
     *
     * Example:
     * GET /beyond404/recommendation/1/unique-stocks
     */
    @GetMapping("/{customerId}/unique-stocks")
    public ResponseEntity<Integer> getUniqueStocks(
            @PathVariable Long customerId) {

        return ResponseEntity.ok(
                portfolioAnalysisService.getUniqueStocks(customerId)
        );
    }

    /**
     * Average Investment Amount
     *
     * Example:
     * GET /beyond404/recommendation/1/average-investment
     */
    @GetMapping("/{customerId}/average-investment")
    public ResponseEntity<Double> getAverageInvestment(
            @PathVariable Long customerId) {

        return ResponseEntity.ok(
                portfolioAnalysisService.getAverageInvestment(customerId)
        );
    }

    /**
     * Market Distribution
     *
     * Example:
     * GET /beyond404/recommendation/1/distribution
     */
    @GetMapping("/{customerId}/distribution")
    public ResponseEntity<Map<String, Double>> getMarketDistribution(
            @PathVariable Long customerId) {

        return ResponseEntity.ok(
                portfolioAnalysisService.getMarketDistribution(customerId)
        );
    }

    @GetMapping("/{customerId}/summary")
    public ResponseEntity<PortfolioAnalysisResponse> getPortfolioSummary(
            @PathVariable Long customerId) {

        PortfolioAnalysisResponse summary =
                portfolioAnalysisService
                        .getPortfolioAnalysis(customerId);

        if(summary == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .build();

        }

        return ResponseEntity.ok(summary);
    }
}