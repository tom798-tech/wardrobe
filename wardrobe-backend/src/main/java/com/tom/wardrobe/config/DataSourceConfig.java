package com.tom.wardrobe.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.filter.stat.StatFilter;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.List;

/**
 * 数据源配置
 */
@Configuration
@EnableTransactionManagement
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String mysqlUrl;

    @Value("${spring.datasource.username}")
    private String mysqlUsername;

    @Value("${spring.datasource.password}")
    private String mysqlPassword;

    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String mysqlDriverClassName;

    @Value("${spring.datasource.druid.initial-size:10}")
    private int mysqlInitialSize;

    @Value("${spring.datasource.druid.max-active:40}")
    private int mysqlMaxActive;

    @Value("${spring.datasource.druid.min-idle:5}")
    private int mysqlMinIdle;

    @Value("${spring.datasource.druid.max-wait:60000}")
    private long mysqlMaxWait;

    @Value("${spring.datasource.druid.stat.slow-sql-millis:1000}")
    private long slowSqlMillis;

    @Value("${spring.datasource.druid.stat.log-slow-sql:true}")
    private boolean logSlowSql;

    @Value("${spring.datasource.druid.stat.merge-sql:true}")
    private boolean mergeSql;

    @Value("${spring.ai.pgvector.store.url:jdbc:postgresql://localhost:5432/wardrobe_vec}")
    private String pgVectorUrl;

    @Value("${spring.ai.pgvector.store.username:postgres}")
    private String pgVectorUsername;

    @Value("${spring.ai.pgvector.store.password:123456}")
    private String pgVectorPassword;

    /**
     * MySQL 主数据源（Primary）
     */
    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl(mysqlUrl);
        ds.setUsername(mysqlUsername);
        ds.setPassword(mysqlPassword);
        ds.setDriverClassName(mysqlDriverClassName);
        ds.setInitialSize(mysqlInitialSize);
        ds.setMaxActive(mysqlMaxActive);
        ds.setMinIdle(mysqlMinIdle);
        ds.setMaxWait(mysqlMaxWait);
        ds.setProxyFilters(List.of(slowSqlStatFilter()));
        return ds;
    }

    private Filter slowSqlStatFilter() {
        StatFilter statFilter = new StatFilter();
        statFilter.setSlowSqlMillis(slowSqlMillis);
        statFilter.setLogSlowSql(logSlowSql);
        statFilter.setMergeSql(mergeSql);
        return statFilter;
    }

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Primary
    @Bean(name = "jdbcTemplate")
    public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * PostgreSQL 向量数据源
     */
    @Bean(name = "pgVectorDataSource")
    public DataSource pgVectorDataSource() {
        return DataSourceBuilder.create()
                .url(pgVectorUrl)
                .username(pgVectorUsername)
                .password(pgVectorPassword)
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    /**
     * PostgreSQL JdbcTemplate
     */
    @Bean(name = "pgJdbcTemplate")
    public JdbcTemplate pgJdbcTemplate(@Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) {
        return new JdbcTemplate(pgVectorDataSource);
    }
}
