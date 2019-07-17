package org.syuzhakov.moneytranfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.syuzhakov.moneytranfer.config.DatabaseConfiguration;
import org.syuzhakov.moneytranfer.config.WebServiceConfiguration;
import org.syuzhakov.moneytranfer.error.ErrorResponse;
import org.syuzhakov.moneytranfer.model.Account;
import org.syuzhakov.moneytranfer.model.AccountStatus;
import org.syuzhakov.moneytranfer.model.Balance;
import org.syuzhakov.moneytranfer.model.Operation;
import org.syuzhakov.moneytranfer.server.JacksonFactory;

import java.util.UUID;

public class OperationApiTest {
    private static App app;
    private static ObjectMapper objectMapper;
    private static String localhost;

    @BeforeAll
    static void beforeAll() {
        app = App.builder()
                .databaseConfiguration(DatabaseConfiguration.builder()
                        .url("jdbc:h2:mem:" + UUID.randomUUID().toString())
                        .user("sa")
                        .password("sa")
                        .build())
                .webServiceConfiguration(WebServiceConfiguration.builder()
                        .enabled(true)
                        .port(0)
                        .build())
                .start();
        localhost = "http://127.0.0.1:" + app.getListenerPort();
        objectMapper = JacksonFactory.getDefaultRestMapper();
    }

    @AfterAll
    static void afterAll() {
        app.stop();
    }

    @Test
    void topupBalanceOnAccount() throws Exception {
        //Create account
        final HttpResponse createResponse = Request.Put(localhost + "/account")
                .bodyString(
                        objectMapper.writeValueAsString(new Account(null, AccountStatus.ACTIVE, null)),
                        ContentType.APPLICATION_JSON)
                .execute()
                .returnResponse();
        final Account createdAccount = objectMapper.readValue(createResponse.getEntity().getContent(), Account.class);
        final Long accountId = createdAccount.getId();

        //Topup money
        final HttpResponse topupResponse = Request.Post(localhost + "/operation/transfer")
                .bodyString(
                        objectMapper.writeValueAsString(Operation.builder().targetAccount(accountId).amount(1000L).build()),
                        ContentType.APPLICATION_JSON)
                .execute()
                .returnResponse();
        final Operation operation = objectMapper.readValue(topupResponse.getEntity().getContent(), Operation.class);
        Assertions.assertEquals(accountId, operation.getTargetAccount());
        Assertions.assertEquals(1000L, operation.getAmount());
        Assertions.assertNotNull(operation.getId());
        Assertions.assertNull(operation.getSourceAccount());
        Assertions.assertNotNull(operation.getTimestamp());

        //Get balance
        final HttpResponse getBalanceResponse = Request.Get(localhost + "/account/" + accountId + "/balance")
                .execute()
                .returnResponse();
        final Balance balance = objectMapper.readValue(getBalanceResponse.getEntity().getContent(), Balance.class);
        Assertions.assertEquals(1000L, balance.getBalance());
        Assertions.assertEquals(accountId, balance.getAccount());
        Assertions.assertEquals(operation.getId(), balance.getOperation());
        Assertions.assertNotNull(balance.getActuality());
    }

    @Test
    void withdrawMoneyFromAccount() throws Exception {
        final long accountId = accountWithMoney(1000L);
        //Withdraw money
        final HttpResponse operationResponse = Request.Post(localhost + "/operation/transfer")
                .bodyString(
                        objectMapper.writeValueAsString(Operation.builder().sourceAccount(accountId).amount(900L).build()),
                        ContentType.APPLICATION_JSON)
                .execute()
                .returnResponse();
        final Operation operation = objectMapper.readValue(operationResponse.getEntity().getContent(), Operation.class);
        Assertions.assertEquals(accountId, operation.getSourceAccount());
        Assertions.assertEquals(900L, operation.getAmount());
        Assertions.assertNotNull(operation.getId());
        Assertions.assertNull(operation.getTargetAccount());
        Assertions.assertNotNull(operation.getTimestamp());

        //Get balance
        final HttpResponse getBalanceResponse = Request.Get(localhost + "/account/" + accountId + "/balance")
                .execute()
                .returnResponse();
        final Balance balance = objectMapper.readValue(getBalanceResponse.getEntity().getContent(), Balance.class);
        Assertions.assertEquals(100L, balance.getBalance());
        Assertions.assertEquals(accountId, balance.getAccount());
        Assertions.assertEquals(operation.getId(), balance.getOperation());
        Assertions.assertNotNull(balance.getActuality());
    }

    @Test
    void transferMoneyBetweenTwoAccounts() throws Exception {
        final long account1 = accountWithMoney(1000L);
        final long account2 = accountWithMoney(1000L);

        //Make transfer operation 1
        final HttpResponse operationResponse1 = Request.Post(localhost + "/operation/transfer")
                .bodyString(
                        objectMapper.writeValueAsString(Operation.builder()
                                .sourceAccount(account1)
                                .targetAccount(account2)
                                .amount(300L).build()),
                        ContentType.APPLICATION_JSON)
                .execute()
                .returnResponse();
        final Operation operation1 = objectMapper.readValue(operationResponse1.getEntity().getContent(), Operation.class);
        Assertions.assertEquals(account1, operation1.getSourceAccount());
        Assertions.assertEquals(account2, operation1.getTargetAccount());
        Assertions.assertEquals(300L, operation1.getAmount());
        Assertions.assertNotNull(operation1.getId());
        Assertions.assertNotNull(operation1.getTimestamp());

        //Make transfer operation 2
        final HttpResponse operationResponse2 = Request.Post(localhost + "/operation/transfer")
                .bodyString(
                        objectMapper.writeValueAsString(Operation.builder()
                                .sourceAccount(account1)
                                .targetAccount(account2)
                                .amount(500L).build()),
                        ContentType.APPLICATION_JSON)
                .execute()
                .returnResponse();
        final Operation operation2 = objectMapper.readValue(operationResponse2.getEntity().getContent(), Operation.class);
        Assertions.assertEquals(account1, operation2.getSourceAccount());
        Assertions.assertEquals(account2, operation2.getTargetAccount());
        Assertions.assertEquals(500L, operation2.getAmount());
        Assertions.assertNotNull(operation2.getId());
        Assertions.assertNotNull(operation2.getTimestamp());

        //Check balance account 1
        final HttpResponse getBalanceResponse1 = Request.Get(localhost + "/account/" + account1 + "/balance")
                .execute()
                .returnResponse();
        final Balance balance1 = objectMapper.readValue(getBalanceResponse1.getEntity().getContent(), Balance.class);
        Assertions.assertEquals(200L, balance1.getBalance());
        Assertions.assertEquals(account1, balance1.getAccount());
        Assertions.assertEquals(operation2.getId(), balance1.getOperation());
        Assertions.assertNotNull(balance1.getActuality());

        //Check balance account 2
        final HttpResponse getBalanceResponse2 = Request.Get(localhost + "/account/" + account2 + "/balance")
                .execute()
                .returnResponse();
        final Balance balance2 = objectMapper.readValue(getBalanceResponse2.getEntity().getContent(), Balance.class);
        Assertions.assertEquals(1800L, balance2.getBalance());
        Assertions.assertEquals(account2, balance2.getAccount());
        Assertions.assertEquals(operation2.getId(), balance2.getOperation());
        Assertions.assertNotNull(balance2.getActuality());
    }

    @Test
    void insufficientFundsForTransfer() throws Exception {
        final long account1 = accountWithMoney(1L);
        final long account2 = accountWithMoney(1L);

        final HttpResponse transferResponse = Request.Post(localhost + "/operation/transfer")
                .bodyString(
                        objectMapper.writeValueAsString(Operation.builder()
                                .sourceAccount(account1)
                                .targetAccount(account2)
                                .amount(100L).build()),
                        ContentType.APPLICATION_JSON)
                .execute()
                .returnResponse();
        final ErrorResponse errorResponse = objectMapper.readValue(transferResponse.getEntity().getContent(), ErrorResponse.class);

        Assertions.assertEquals(500, transferResponse.getStatusLine().getStatusCode());
        Assertions.assertEquals(500010, errorResponse.getErrorCode());
        Assertions.assertNotNull(errorResponse.getMessage());
        Assertions.assertNull(errorResponse.getHttpStatus());
    }


    private long accountWithMoney(long money) throws Exception {
        //Create account
        final HttpResponse createResponse = Request.Put(localhost + "/account")
                .bodyString(
                        objectMapper.writeValueAsString(new Account(null, AccountStatus.ACTIVE, null)),
                        ContentType.APPLICATION_JSON)
                .execute()
                .returnResponse();
        final Account createdAccount = objectMapper.readValue(createResponse.getEntity().getContent(), Account.class);

        //Topup money
        final HttpResponse topupResponse = Request.Post(localhost + "/operation/transfer")
                .bodyString(
                        objectMapper.writeValueAsString(Operation.builder().targetAccount(createdAccount.getId()).amount(money).build()),
                        ContentType.APPLICATION_JSON)
                .execute()
                .returnResponse();

        return createdAccount.getId();
    }

}