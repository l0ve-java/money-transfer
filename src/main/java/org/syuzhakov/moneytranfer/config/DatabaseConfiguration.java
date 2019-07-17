package org.syuzhakov.moneytranfer.config;

import lombok.Builder;
import lombok.Getter;

import java.util.Properties;

@Getter
public class DatabaseConfiguration {
    private String url;
    private String user;
    private String password;
    private boolean performMigration = true;

    public DatabaseConfiguration(Properties source) {
        url = source.getProperty("datasource.url");
        user = source.getProperty("datasource.user");
        password = source.getProperty("datasource.password");
        performMigration = !"false".equals(source.getProperty("datasource.migrate"));
    }

    @Builder
    public DatabaseConfiguration(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }
}
