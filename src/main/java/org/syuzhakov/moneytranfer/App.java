package org.syuzhakov.moneytranfer;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import lombok.Builder;
import lombok.Getter;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syuzhakov.moneytranfer.config.DatabaseConfiguration;
import org.syuzhakov.moneytranfer.config.WebServiceConfiguration;
import org.syuzhakov.moneytranfer.database.AccountRepository;
import org.syuzhakov.moneytranfer.database.AccountRepositoryImpl;
import org.syuzhakov.moneytranfer.database.BalanceRepository;
import org.syuzhakov.moneytranfer.database.BalanceRepositoryImpl;
import org.syuzhakov.moneytranfer.database.ConnectionFactory;
import org.syuzhakov.moneytranfer.database.DataSourceFactory;
import org.syuzhakov.moneytranfer.database.H2DataSourceFactory;
import org.syuzhakov.moneytranfer.database.OperationRepository;
import org.syuzhakov.moneytranfer.database.OperationRepositoryImpl;
import org.syuzhakov.moneytranfer.database.ThreadLocalConnectionFactory;
import org.syuzhakov.moneytranfer.logger.Slf4jAccessLogReceiver;
import org.syuzhakov.moneytranfer.model.Account;
import org.syuzhakov.moneytranfer.model.Operation;
import org.syuzhakov.moneytranfer.server.RestHandler;
import org.syuzhakov.moneytranfer.service.AccountService;
import org.syuzhakov.moneytranfer.service.AccountServiceImpl;
import org.syuzhakov.moneytranfer.service.OperationService;
import org.syuzhakov.moneytranfer.service.OperationServiceImpl;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

@Getter
public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    private DatabaseConfiguration databaseConfiguration;
    private WebServiceConfiguration webServiceConfiguration;
    private DataSourceFactory dataSourceFactory;
    private ConnectionFactory connectionFactory;
    private Undertow server;
    private AccountRepository accountRepository;
    private BalanceRepository balanceRepository;
    private OperationRepository operationRepository;
    private AccountService accountService;
    private OperationService operationService;

    public static void main(String[] args) {
        try {
            final InputStream appPropertiesStream =
                    Optional.ofNullable(getProperiesFromCmdArgs(args))
                            .orElseGet(() -> Optional.ofNullable(getPropertiesFromWorkDir())
                                    .orElseGet(() -> Optional.ofNullable(getPropertiesFromClasspath())
                                            .orElseThrow(() -> new IllegalStateException("Cannot resolve application.properties as first argument, " +
                                                    "at work dir or at classpath"))));

            final Properties applicationProperties = new Properties();
            applicationProperties.load(appPropertiesStream);
            appPropertiesStream.close();

            final DatabaseConfiguration databaseConfiguration = new DatabaseConfiguration(applicationProperties);
            final WebServiceConfiguration webServiceConfiguration = new WebServiceConfiguration(applicationProperties);

            App.builder()
                    .databaseConfiguration(databaseConfiguration)
                    .webServiceConfiguration(webServiceConfiguration)
                    .start();
        } catch (Exception e) {
            System.out.println("ERROR " + e.getMessage());
            LOGGER.error("Application failed to start: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    private static InputStream getProperiesFromCmdArgs(String[] args) {
        if (args.length > 0) {
            try {
                return Files.newInputStream(Path.of(args[0]));
            } catch (IOException e) {
                throw new IllegalStateException("Cannot open " + args[0], e);
            }
        } else {
            return null;
        }
    }

    private static InputStream getPropertiesFromWorkDir() {
        final Path path = Paths.get("application.properties");
        if (Files.exists(path)) {
            try {
                return Files.newInputStream(path);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot open " + path.toString(), e);
            }
        } else {
            return null;
        }
    }

    private static InputStream getPropertiesFromClasspath() {
        return App.class.getClassLoader().getResourceAsStream("application.properties");
    }

    @Builder(buildMethodName = "start")
    public App(DatabaseConfiguration databaseConfiguration, WebServiceConfiguration webServiceConfiguration) {
        this.databaseConfiguration = databaseConfiguration;
        this.webServiceConfiguration = webServiceConfiguration;
        dataSourceFactory = new H2DataSourceFactory(databaseConfiguration);
        connectionFactory = new ThreadLocalConnectionFactory(dataSourceFactory);
        accountRepository = new AccountRepositoryImpl(connectionFactory);
        balanceRepository = new BalanceRepositoryImpl(connectionFactory);
        operationRepository = new OperationRepositoryImpl(connectionFactory);
        accountService = new AccountServiceImpl(accountRepository, balanceRepository);
        operationService = new OperationServiceImpl(accountRepository, balanceRepository, operationRepository);

        if (databaseConfiguration.isPerformMigration()) {
            performDatabaseMigration();
        }

        if (webServiceConfiguration.isEnabled()) {
            startWebServer();
        }
    }

    private void performDatabaseMigration() {
        Flyway.configure().dataSource(connectionFactory.getDataSource()).load().migrate();
    }

    private void startWebServer() {
        server = Undertow.builder()
                .addHttpListener(webServiceConfiguration.getPort(), "0.0.0.0")
                .setHandler(new AccessLogHandler(new RoutingHandler()
                        // PUT /account
                        .put("/account", new RestHandler<>(Account.class) {
                            @Override
                            public Object execute(Account body, HttpServerExchange exchange) {
                                return connectionFactory.executeInTransaction(() ->
                                        accountService.createNewAccount(body));
                            }
                        })
                        // POST /account
                        .post("/account", new RestHandler<>(Account.class) {
                            @Override
                            public Object execute(Account body, HttpServerExchange exchange) {
                                return connectionFactory.executeInTransaction(() -> {
                                    accountService.updateAccount(body);
                                    return body;
                                });
                            }
                        })
                        // GET /account
                        .get("/account/{id}", new RestHandler<>(Void.class) {
                            @Override
                            public Object execute(Void body, HttpServerExchange exchange) {
                                return connectionFactory.executeInTransaction(() -> {
                                    final String id = exchange.getQueryParameters().get("id").getFirst();
                                    final Account result = accountService.getAccountById(Long.parseLong(id));
                                    if (result == null) {
                                        exchange.setStatusCode(204);
                                    }
                                    return result;
                                });
                            }
                        })
                        // GET /account/{id}/balance
                        .get("/account/{id}/balance", new RestHandler<>(Void.class) {
                            @Override
                            public Object execute(Void body, HttpServerExchange exchange) {
                                return connectionFactory.executeInTransaction(() -> {
                                    final String id = exchange.getQueryParameters().get("id").getFirst();
                                    return accountService.getBalance(Long.parseLong(id));
                                });
                            }
                        })
                        // POST /operation/transfer
                        .post("/operation/transfer", new RestHandler<>(Operation.class) {
                            @Override
                            public Object execute(Operation body, HttpServerExchange exchange) {
                                return connectionFactory.executeInTransaction(() -> operationService.transferMoney(body));
                            }
                        })
                        //Access log configuration
                        , new Slf4jAccessLogReceiver(), "common", App.class.getClassLoader()))
                .build();
        server.start();
    }

    public Integer getListenerPort() {
        return Optional.ofNullable(server).map(Undertow::getListenerInfo)
                .map(Collection::stream).flatMap(Stream::findFirst)
                .map(Undertow.ListenerInfo::getAddress).map(addr -> ((InetSocketAddress) addr).getPort())
                .orElse(null);
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }


}
