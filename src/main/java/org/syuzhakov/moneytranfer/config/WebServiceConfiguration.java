package org.syuzhakov.moneytranfer.config;

import lombok.Builder;
import lombok.Getter;

import java.util.Optional;
import java.util.Properties;

@Getter
public class WebServiceConfiguration {
    private int port;
    private boolean enabled = true;

    public WebServiceConfiguration(Properties properties) {
        port = Optional.ofNullable(properties.getProperty("server.port")).map(Integer::parseInt).orElse(0);
        enabled = !"false".equals(properties.getProperty("server.enabled"));
    }

    @Builder
    public WebServiceConfiguration(int port, boolean enabled) {
        this.port = port;
        this.enabled = enabled;
    }
}
