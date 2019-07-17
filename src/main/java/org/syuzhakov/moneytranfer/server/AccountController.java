package org.syuzhakov.moneytranfer.server;

import lombok.RequiredArgsConstructor;
import org.syuzhakov.moneytranfer.model.Account;
import org.syuzhakov.moneytranfer.service.AccountService;

@RequiredArgsConstructor
public class AccountController {
    private AccountService accountService;

    public Account createNewAccount(Account account) {
        return accountService.createNewAccount(account);
    }

    public Account getAccountById(long id) {
        return accountService.getAccountById(id);
    }

    public Account updateAccount(Account account) {
        accountService.updateAccount(account);
        return account;
    }

}
