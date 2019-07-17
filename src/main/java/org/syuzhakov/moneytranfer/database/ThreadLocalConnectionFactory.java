package org.syuzhakov.moneytranfer.database;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syuzhakov.moneytranfer.error.UnexpectedException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

public class ThreadLocalConnectionFactory implements ConnectionFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionFactory.class);
    private final DataSource dataSource;
    private final ThreadLocal<Connection> connection = new ThreadLocal<>();

    public ThreadLocalConnectionFactory(DataSourceFactory dataSourceFactory) {
        this.dataSource = buildPooledDataSource(dataSourceFactory);
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Connection getConnection() {
        Connection conn = this.connection.get();
        if (conn == null) {
            conn = getConnectionFromPool();
            this.connection.set(conn);
        }
        return conn;
    }

    @Override
    public <T> T executeInTransaction(Supplier<T> target) {
        final Connection connection = getConnection();
        try {
            connection.setAutoCommit(false);
            final T result = target.get();
            connection.commit();
            return result;
        } catch (RuntimeException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
            throw e;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
            throw new UnexpectedException(e);
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }
    }

    private Connection getConnectionFromPool() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new UnexpectedException(e);
        }
    }

    private DataSource buildPooledDataSource(DataSourceFactory dataSourceFactory) {
        final HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDataSource(dataSourceFactory.getDataSource());
        return dataSource;
    }
}
