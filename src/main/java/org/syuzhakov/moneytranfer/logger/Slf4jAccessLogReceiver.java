package org.syuzhakov.moneytranfer.logger;

import io.undertow.server.handlers.accesslog.AccessLogReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jAccessLogReceiver implements AccessLogReceiver {
    private final static Logger LOGGER = LoggerFactory.getLogger("undertow.access.log");

    @Override
    public void logMessage(String message) {
        LOGGER.info(message);
    }
}
