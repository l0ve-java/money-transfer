package org.syuzhakov.moneytranfer.database;

import org.syuzhakov.moneytranfer.model.Account;

public interface AccountRepository {
    Account createNewAccount(Account account);

    Account getAccountById(long id, boolean forUpdate);

    void updateAccount(Account account);
}
