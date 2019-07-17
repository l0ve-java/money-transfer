package org.syuzhakov.moneytranfer.database;

import org.h2.jdbcx.JdbcDataSource;
import org.syuzhakov.moneytranfer.config.DatabaseConfiguration;

import javax.sql.DataSource;

public class H2DataSourceFactory implements DataSourceFactory {
    private DatabaseConfiguration configuration;

    public H2DataSourceFactory(DatabaseConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public DataSource getDataSource() {
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(configuration.getUrl());
        dataSource.setUser(configuration.getUser());
        dataSource.setPassword(configuration.getPassword());
        return dataSource;
    }
}
