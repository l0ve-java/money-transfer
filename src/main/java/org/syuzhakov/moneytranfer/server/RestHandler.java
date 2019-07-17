package org.syuzhakov.moneytranfer.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syuzhakov.moneytranfer.error.BadRequestException;
import org.syuzhakov.moneytranfer.error.ErrorResponse;
import org.syuzhakov.moneytranfer.error.ExpectedException;
import org.syuzhakov.moneytranfer.error.UnexpectedException;

import java.io.IOException;
import java.io.OutputStream;

public abstract class RestHandler<T> implements HttpHandler {
    private final static Logger LOGGER = LoggerFactory.getLogger(RestHandler.class);
    private final ObjectMapper mapper = JacksonFactory.getDefaultRestMapper();
    private final Class<T> requestType;

    public RestHandler(Class<T> requestType) {
        this.requestType = requestType;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        //Dispatch execution to XNIO thread pool
        exchange.startBlocking();
        if (exchange.isInIoThread()) {
            exchange.dispatch(() -> performDispatch(exchange));
        }
    }

    private void performDispatch(HttpServerExchange exchange) {
        try {
            //Get request body, if present
            final T requestBody;
            if (Methods.GET.equals(exchange.getRequestMethod())) {
                requestBody = null;
            } else {
                requestBody = readRequestBody(exchange);
            }
            //Log request
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Start=\"{} {}\"; query={}; body={}",
                        exchange.getRequestMethod(),
                        exchange.getRequestPath(),
                        exchange.getQueryString(),
                        requestBody);
            }
            //Add response headers
            applyRestDefaults(exchange);
            //Call execution delegate
            final Object result = execute(requestBody, exchange);
            //Log response
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Success result={}", result);
            }
            //Write response
            writeResponseBody(result, exchange);
        } catch (UnexpectedException ex) {
            LOGGER.error(ex.getMessage(), ex);
            LOGGER.error("Unexpected error {}; code={}; message=\"{}\"",
                    ex.getClass().getSimpleName(),
                    ex.getErrorResponse().getErrorCode(),
                    ex.getErrorResponse().getMessage());
            sendErrorResponse(ex.getErrorResponse(), exchange);
        } catch (ExpectedException ex) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error {}; code={}; message=\"{}\"",
                        ex.getClass().getSimpleName(),
                        ex.getErrorResponse().getErrorCode(),
                        ex.getErrorResponse().getMessage());
            }
            sendErrorResponse(ex.getErrorResponse(), exchange);
        } catch (Exception ex) {
            final UnexpectedException convertedException = new UnexpectedException(ex.getMessage());
            LOGGER.error(ex.getMessage(), ex);
            LOGGER.error("Unexpected error {}; code={}; message=\"{}\"",
                    ex.getClass().getSimpleName(),
                    convertedException.getErrorResponse().getErrorCode(),
                    convertedException.getErrorResponse().getMessage());
            sendErrorResponse(convertedException.getErrorResponse(), exchange);
        }
    }

    private void applyRestDefaults(HttpServerExchange exchange) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    }

    private T readRequestBody(HttpServerExchange exchange) {
        try {
            return mapper.readValue(exchange.getInputStream(), requestType);
        } catch (IOException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    private void writeResponseBody(Object responseObject, HttpServerExchange exchange) {
        try (final OutputStream outputStream = exchange.getOutputStream()) {
            if (responseObject != null) {
                mapper.writeValue(outputStream, responseObject);
            }
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    private void sendErrorResponse(ErrorResponse errorResponse, HttpServerExchange exchange) {
        try {
            exchange.setStatusCode(errorResponse.getHttpStatus());
            mapper.writeValue(exchange.getOutputStream(), errorResponse);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public abstract Object execute(T body, HttpServerExchange exchange);
}
