package com.tom.wardrobe.config;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataSourceConfigTest {

    @Test
    void mysqlDataSourceEnablesDruidSlowSqlStatFilter() {
        DataSourceConfig config = new DataSourceConfig();
        ReflectionTestUtils.setField(config, "mysqlUrl", "jdbc:mysql://localhost:3307/wardrobe");
        ReflectionTestUtils.setField(config, "mysqlUsername", "root");
        ReflectionTestUtils.setField(config, "mysqlPassword", "123456");
        ReflectionTestUtils.setField(config, "mysqlDriverClassName", "com.mysql.cj.jdbc.Driver");
        ReflectionTestUtils.setField(config, "mysqlInitialSize", 1);
        ReflectionTestUtils.setField(config, "mysqlMaxActive", 2);
        ReflectionTestUtils.setField(config, "mysqlMinIdle", 1);
        ReflectionTestUtils.setField(config, "mysqlMaxWait", 1000L);
        ReflectionTestUtils.setField(config, "slowSqlMillis", 750L);
        ReflectionTestUtils.setField(config, "logSlowSql", true);
        ReflectionTestUtils.setField(config, "mergeSql", true);

        DruidDataSource dataSource = (DruidDataSource) config.dataSource();

        try {
            List<Filter> proxyFilters = dataSource.getProxyFilters();
            assertEquals(1, proxyFilters.size());
            assertInstanceOf(StatFilter.class, proxyFilters.get(0));

            StatFilter statFilter = (StatFilter) proxyFilters.get(0);
            assertEquals(750L, statFilter.getSlowSqlMillis());
            assertTrue(statFilter.isLogSlowSql());
            assertTrue(statFilter.isMergeSql());
        } finally {
            dataSource.close();
        }
    }
}
