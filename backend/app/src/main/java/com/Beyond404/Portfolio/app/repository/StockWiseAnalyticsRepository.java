package com.Beyond404.Portfolio.app.repository;

import com.Beyond404.Portfolio.app.model.PortfolioData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Repository
public class StockWiseAnalyticsRepository {

    private final JdbcTemplate jdbcTemplate;
    private final PortfolioDataRowMapper portfolioDataRowMapper;

    public StockWiseAnalyticsRepository(
            JdbcTemplate jdbcTemplate,
            PortfolioDataRowMapper portfolioDataRowMapper) {

        this.jdbcTemplate = jdbcTemplate;
        this.portfolioDataRowMapper = portfolioDataRowMapper;
    }

    public ArrayList<PortfolioData> getCustomerPortfolio(Long customerId) {

        String sql = """
                SELECT
                    c.customer_id,
                    c.name,
                    c.risk_lvl,

                    s.stock_id,
                    s.stock_name,
                    s.ticker,
                    s.stock_market,

                    i.transaction_type,
                    i.quantity,
                   
                    i.transaction_timestamp
                FROM customers c
                INNER JOIN investments i
                    ON c.customer_id = i.customer_id
                INNER JOIN stocks s
                    ON s.stock_id = i.stock_id
                WHERE c.customer_id = ?
                ORDER BY i.transaction_timestamp
                """;

        return new ArrayList<>(
                jdbcTemplate.query(
                        sql,
                        portfolioDataRowMapper,
                        customerId
                )
        );
    }
}