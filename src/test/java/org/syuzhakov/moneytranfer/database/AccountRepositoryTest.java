package org.syuzhakov.moneytranfer.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.syuzhakov.moneytranfer.App;
import org.syuzhakov.moneytranfer.config.DatabaseConfiguration;
import org.syuzhakov.moneytranfer.config.WebServiceConfiguration;
import org.syuzhakov.moneytranfer.error.BadRequestException;
import org.syuzhakov.moneytranfer.model.Account;
import org.syuzhakov.moneytranfer.model.AccountStatus;

import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

class AccountRepositoryTest {
    private App app;
    private AccountRepository accountRepository;

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

        accountRepository = app.getAccountRepository();
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
    void createNewAccountAndGetById() {
        final Account account = Account.builder().status(AccountStatus.ACTIVE).build();
        final Account newAccount = accountRepository.createNewAccount(account);
        Assertions.assertNotNull(newAccount.getId());
        Assertions.assertEquals(newAccount.getStatus(), AccountStatus.ACTIVE);

        final Long id = newAccount.getId();
        final Account accountById = accountRepository.getAccountById(id, false);
        Assertions.assertEquals(accountById.getId(), id);
        Assertions.assertEquals(accountById.getStatus(), AccountStatus.ACTIVE);
        Assertions.assertFalse(accountById.getActuality().isAfter(Instant.now()));
    }

    @Test
    void updateAccount() {
        long id = accountRepository
                .createNewAccount(new Account(null, AccountStatus.ACTIVE, null))
                .getId();
        accountRepository.updateAccount(new Account(id, AccountStatus.BLOCKED, null));
        final Account account = accountRepository.getAccountById(id, false);
        Assertions.assertEquals(account.getId(), id);
        Assertions.assertEquals(account.getStatus(), AccountStatus.BLOCKED);
    }

    @Test
    void updateFailsWithUnexistingAccount() {
        Assertions.assertThrows(BadRequestException.class,
                () -> accountRepository.updateAccount(new Account(1L, AccountStatus.BLOCKED, null)));
    }

    @Test
    void getUnexistingAccount() {
        Assertions.assertNull(accountRepository.getAccountById(0L, false));
    }
}