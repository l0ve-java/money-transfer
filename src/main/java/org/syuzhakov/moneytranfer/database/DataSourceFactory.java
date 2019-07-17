package org.syuzhakov.moneytranfer.database;

import javax.sql.DataSource;

public interface DataSourceFactory {
    DataSource getDataSource();
}
