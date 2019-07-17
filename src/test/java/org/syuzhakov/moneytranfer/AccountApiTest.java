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
import org.syuzhakov.moneytranfer.server.JacksonFactory;

import java.util.UUID;

public class AccountApiTest {
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
    void createModifyAndGetAccount() throws Exception {
        //Create account
        final HttpResponse createResponse = Request.Put(localhost + "/account")
                .bodyString(
                        objectMapper.writeValueAsString(new Account(null, AccountStatus.ACTIVE, null)),
                        ContentType.APPLICATION_JSON)
                .execute()
                .returnResponse();
        final Account createdAccount = objectMapper.readValue(createResponse.getEntity().getContent(), Account.class);
        final Long accountId = createdAccount.getId();
        Assertions.assertEquals(200, createResponse.getStatusLine().getStatusCode());
        Assertions.assertEquals(AccountStatus.ACTIVE, createdAccount.getStatus());
        Assertions.assertNotNull(accountId);

        //Get created account
        final HttpResponse getAfterCreateResponse = Request.Get(localhost + "/account/" + accountId)
                .execute()
                .returnResponse();
        final Account getAfterCreate = objectMapper.readValue(getAfterCreateResponse.getEntity().getContent(), Account.class);
        Assertions.assertEquals(200, getAfterCreateResponse.getStatusLine().getStatusCode());
        Assertions.assertEquals(AccountStatus.ACTIVE, getAfterCreate.getStatus());
        Assertions.assertEquals(accountId, getAfterCreate.getId());


        //Update account
        final HttpResponse updateResponse = Request.Post(localhost + "/account")
                .bodyString(
                        objectMapper.writeValueAsString(new Account(accountId, AccountStatus.BLOCKED, null)),
                        ContentType.APPLICATION_JSON)
                .execute()
                .returnResponse();
        final Account updatedAccount = objectMapper.readValue(updateResponse.getEntity().getContent(), Account.class);
        Assertions.assertEquals(200, updateResponse.getStatusLine().getStatusCode());
        Assertions.assertEquals(AccountStatus.BLOCKED, updatedAccount.getStatus());
        Assertions.assertEquals(accountId, updatedAccount.getId());

        //Get after update account
        final HttpResponse getAfterUpdateResponse = Request.Get(localhost + "/account/" + accountId)
                .execute()
                .returnResponse();
        final Account getAfterUpdate = objectMapper.readValue(getAfterUpdateResponse.getEntity().getContent(), Account.class);
        Assertions.assertEquals(200, getAfterUpdateResponse.getStatusLine().getStatusCode());
        Assertions.assertEquals(AccountStatus.BLOCKED, getAfterUpdate.getStatus());
        Assertions.assertEquals(accountId, getAfterUpdate.getId());
    }

    @Test
    void updateUnexistingAccount() throws Exception {
        final Account account = Account.builder().id(88005553535L).status(AccountStatus.ACTIVE).build();
        final HttpResponse response = Request.Post(localhost + "/account")
                .bodyString(objectMapper.writeValueAsString(account), ContentType.APPLICATION_JSON)
                .execute()
                .returnResponse();
        final ErrorResponse errorResponse = objectMapper.readValue(response.getEntity().getContent(), ErrorResponse.class);

        Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
        Assertions.assertEquals(400002, errorResponse.getErrorCode());
        Assertions.assertNotNull(errorResponse.getMessage());
        Assertions.assertNull(errorResponse.getHttpStatus());
    }

    @Test
    void getUnexistingAccount() throws Exception {
        final HttpResponse response = Request.Get(localhost + "/account/88005553535")
                .execute()
                .returnResponse();

        Assertions.assertNull(response.getEntity());
        Assertions.assertEquals(response.getStatusLine().getStatusCode(), 204);
    }

    @Test
    void getBalanceForNewAccount() throws Exception {
        // Create account
        final HttpResponse createResponse = Request.Put(localhost + "/account")
                .bodyString(
                        objectMapper.writeValueAsString(new Account(null, AccountStatus.ACTIVE, null)),
                        ContentType.APPLICATION_JSON)
                .execute()
                .returnResponse();
        final Account createdAccount = objectMapper.readValue(createResponse.getEntity().getContent(), Account.class);
        final Long accountId = createdAccount.getId();

        //Get balance
        final HttpResponse response = Request.Get(localhost + "/account/" + accountId + "/balance")
                .execute()
                .returnResponse();
        final Balance balance = objectMapper.readValue(response.getEntity().getContent(), Balance.class);
        Assertions.assertEquals(0L, balance.getBalance());
        Assertions.assertEquals(accountId, balance.getAccount());
        Assertions.assertNull(balance.getOperation());
        Assertions.assertNotNull(balance.getActuality());
    }

    @Test
    void getBalanceForMissingAccount() throws Exception {
        final HttpResponse response = Request.Get(localhost + "/account/88005553535/balance")
                .execute()
                .returnResponse();
        final ErrorResponse errorResponse = objectMapper.readValue(response.getEntity().getContent(), ErrorResponse.class);

        Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
        Assertions.assertEquals(400002, errorResponse.getErrorCode());
        Assertions.assertNotNull(errorResponse.getMessage());
        Assertions.assertNull(errorResponse.getHttpStatus());
    }

    @Test
    void getBalanceForBlockedAccount() throws Exception {
        // Create account
        final HttpResponse createResponse = Request.Put(localhost + "/account")
                .bodyString(
                        objectMapper.writeValueAsString(new Account(null, AccountStatus.BLOCKED, null)),
                        ContentType.APPLICATION_JSON)
                .execute()
                .returnResponse();
        final Account createdAccount = objectMapper.readValue(createResponse.getEntity().getContent(), Account.class);
        final Long accountId = createdAccount.getId();

        //Get balance
        final HttpResponse response = Request.Get(localhost + "/account/" + accountId + "/balance")
                .execute()
                .returnResponse();
        final ErrorResponse errorResponse = objectMapper.readValue(response.getEntity().getContent(), ErrorResponse.class);

        Assertions.assertEquals(500, response.getStatusLine().getStatusCode());
        Assertions.assertEquals(500010, errorResponse.getErrorCode());
        Assertions.assertNotNull(errorResponse.getMessage());
        Assertions.assertNull(errorResponse.getHttpStatus());
    }
}
