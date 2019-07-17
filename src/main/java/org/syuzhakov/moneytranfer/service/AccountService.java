package org.syuzhakov.moneytranfer.service;

import org.syuzhakov.moneytranfer.model.Account;
import org.syuzhakov.moneytranfer.model.Balance;

public interface AccountService {
    Account createNewAccount(Account account);

    Account getAccountById(long id);

    void updateAccount(Account account);

    Balance getBalance(Long accountId);
}
