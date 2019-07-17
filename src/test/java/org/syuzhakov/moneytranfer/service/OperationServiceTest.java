package org.syuzhakov.moneytranfer.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.syuzhakov.moneytranfer.App;
import org.syuzhakov.moneytranfer.config.DatabaseConfiguration;
import org.syuzhakov.moneytranfer.config.WebServiceConfiguration;
import org.syuzhakov.moneytranfer.error.OperationImpossibleException;
import org.syuzhakov.moneytranfer.error.ValidationException;
import org.syuzhakov.moneytranfer.model.Account;
import org.syuzhakov.moneytranfer.model.AccountStatus;
import org.syuzhakov.moneytranfer.model.Balance;
import org.syuzhakov.moneytranfer.model.Operation;

import java.sql.SQLException;
import java.util.UUID;

public class OperationServiceTest {
    private App app;
    private OperationService operationService;
    private AccountService accountService;

    @BeforeEach
    void beforeEach() {
        app = App.builder()
                .databaseConfiguration(DatabaseConfiguration.builder()
                        .url("jdbc:h2:mem:" + UUID.randomUUID().toString())
                        .user("sa")
                        .password("sa")
                        .build())
                .webServiceConfiguration(WebServiceConfiguration.builder()
                        .enabled(false)
                        .build())
                .start();

        accountService = app.getAccountService();
        operationService = app.getOperationService();
    }

    @AfterEach
    void afterEach() {
        try {
            app.getConnectionFactory().getConnection().close();
        } catch (SQLException e) {
            //ignore
        }
    }

    @Test
    void topupBalance() {
        final Long accountId = accountService.createNewAccount(Account.builder().status(AccountStatus.ACTIVE).build()).getId();
        //Make topup operation
        final Operation operation = operationService.transferMoney(Operation.builder().targetAccount(accountId).amount(1000L).build());
        Assertions.assertEquals(accountId, operation.getTargetAccount());
        Assertions.assertEquals(1000L, operation.getAmount());
        Assertions.assertNotNull(operation.getId());
        Assertions.assertNull(operation.getSourceAccount());
        Assertions.assertNotNull(operation.getTimestamp());
        //Check balance
        final Balance balance = accountService.getBalance(accountId);
        Assertions.assertEquals(1000L, balance.getBalance());
        Assertions.assertEquals(accountId, balance.getAccount());
        Assertions.assertNotNull(balance.getOperation());
        Assertions.assertNotNull(balance.getActuality());
    }

    @Test
    void insufficientFundsForTransfer() {
        Assertions.assertThrows(OperationImpossibleException.class, () -> {
            final long account = accountWithMoney(100L);
            operationService.transferMoney(Operation.builder().sourceAccount(account).amount(200L).build());
        });
    }

    @Test
    void incorrectOperationAmount() {
        Assertions.assertThrows(ValidationException.class, () -> {
            accountWithMoney(-1L);
        });
    }

    @Test
    void withdrawMoney() {
        final long accountId = accountWithMoney(1000L);
        //Make withdraw operation
        final Operation operation = operationService.transferMoney(Operation.builder().sourceAccount(accountId).amount(900L).build());
        Assertions.assertEquals(accountId, operation.getSourceAccount());
        Assertions.assertEquals(900L, operation.getAmount());
        Assertions.assertNotNull(operation.getId());
        Assertions.assertNull(operation.getTargetAccount());
        Assertions.assertNotNull(operation.getTimestamp());
        //Check balance
        final Balance balance = accountService.getBalance(accountId);
        Assertions.assertEquals(100L, balance.getBalance());
        Assertions.assertEquals(accountId, balance.getAccount());
        Assertions.assertNotNull(balance.getOperation());
        Assertions.assertNotNull(balance.getActuality());
    }

    @Test
    void transferMoneyBetweenTwoAccounts() {
        final long account1 = accountWithMoney(1000L);
        final long account2 = accountWithMoney(1000L);
        //Make transfer operation 1
        final Operation operation1 = operationService.transferMoney(
                Operation.builder().sourceAccount(account1).amount(300L).targetAccount(account2).build());
        Assertions.assertEquals(account1, operation1.getSourceAccount());
        Assertions.assertEquals(account2, operation1.getTargetAccount());
        Assertions.assertEquals(300L, operation1.getAmount());
        Assertions.assertNotNull(operation1.getId());
        Assertions.assertNotNull(operation1.getTimestamp());
        //Make transfer operation 2
        final Operation operation2 = operationService.transferMoney(
                Operation.builder().sourceAccount(account1).amount(500L).targetAccount(account2).build());
        Assertions.assertEquals(account1, operation2.getSourceAccount());
        Assertions.assertEquals(account2, operation2.getTargetAccount());
        Assertions.assertEquals(500L, operation2.getAmount());
        Assertions.assertNotNull(operation2.getId());
        Assertions.assertNotNull(operation2.getTimestamp());
        //Check balance account 1
        final Balance balance1 = accountService.getBalance(account1);
        Assertions.assertEquals(200L, balance1.getBalance());
        Assertions.assertEquals(account1, balance1.getAccount());
        Assertions.assertEquals(operation2.getId(), balance1.getOperation());
        Assertions.assertNotNull(balance1.getActuality());
        //Check balance account 2
        final Balance balance2 = accountService.getBalance(account2);
        Assertions.assertEquals(1800L, balance2.getBalance());
        Assertions.assertEquals(account2, balance2.getAccount());
        Assertions.assertEquals(operation2.getId(), balance2.getOperation());
        Assertions.assertNotNull(balance2.getActuality());
    }

    @Test
    void operationsWithBlockedAccount() {
        final Long accountId = accountService.createNewAccount(Account.builder().status(AccountStatus.BLOCKED).build()).getId();
        Assertions.assertThrows(OperationImpossibleException.class, () -> {
            operationService.transferMoney(Operation.builder().sourceAccount(accountId).amount(100L).build());
        });
        Assertions.assertThrows(OperationImpossibleException.class, () -> {
            operationService.transferMoney(Operation.builder().targetAccount(accountId).amount(100L).build());
        });
    }

    @Test
    void missingRequiredFields() {
        Assertions.assertThrows(ValidationException.class, () -> {
            operationService.transferMoney(Operation.builder().amount(100L).build());
        });
        Assertions.assertThrows(ValidationException.class, () -> {
            operationService.transferMoney(Operation.builder().sourceAccount(1L).targetAccount(2L).build());
        });
        Assertions.assertThrows(ValidationException.class, () -> {
            operationService.transferMoney(Operation.builder().sourceAccount(1L).targetAccount(1L).amount(1L).build());
        });
    }


    private long accountWithMoney(long money) {
        final Long accountId = accountService.createNewAccount(Account.builder().status(AccountStatus.ACTIVE).build()).getId();
        return operationService.transferMoney(Operation.builder().targetAccount(accountId).amount(money).build()).getTargetAccount();
    }

}