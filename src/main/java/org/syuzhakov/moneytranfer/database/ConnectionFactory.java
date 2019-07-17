package org.syuzhakov.moneytranfer.database;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Supplier;

public interface ConnectionFactory {
    DataSource getDataSource();

    Connection getConnection();

    <T> T executeInTransaction(Supplier<T> target);
}
