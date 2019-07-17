package org.syuzhakov.moneytranfer.service;

import lombok.RequiredArgsConstructor;
import org.syuzhakov.moneytranfer.database.AccountRepository;
import org.syuzhakov.moneytranfer.database.BalanceRepository;
import org.syuzhakov.moneytranfer.error.BadRequestException;
import org.syuzhakov.moneytranfer.error.OperationImpossibleException;
import org.syuzhakov.moneytranfer.error.Require;
import org.syuzhakov.moneytranfer.model.Account;
import org.syuzhakov.moneytranfer.model.AccountStatus;
import org.syuzhakov.moneytranfer.model.Balance;

import java.time.Instant;
import java.util.Optional;

@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final BalanceRepository balanceRepository;

    @Override
    public Account createNewAccount(Account account) {
        Require.notNull(account.getStatus(), "status");
        return accountRepository.createNewAccount(account);
    }

    @Override
    public Account getAccountById(long id) {
        return accountRepository.getAccountById(id, false);
    }

    @Override
    public void updateAccount(Account account) {
        Require.notNull(account.getId(), "id");
        Require.notNull(account.getStatus(), "status");
        accountRepository.updateAccount(account);
    }

    @Override
    public Balance getBalance(Long accountId) {
        final Account account = accountRepository.getAccountById(accountId, false);
        if (account == null) {
            throw new BadRequestException(String.format("Account %s does not exist", accountId));
        } else if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new OperationImpossibleException(String.format("Account %s is not active", accountId));
        }
        final Instant requestTime = Instant.now();
        return Optional.ofNullable(balanceRepository.getBalance(accountId, false))
                .orElse(Balance.builder().account(accountId).balance(0L).actuality(requestTime).build());
    }
}
