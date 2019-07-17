package org.syuzhakov.moneytranfer.service;

import lombok.RequiredArgsConstructor;
import org.syuzhakov.moneytranfer.database.AccountRepository;
import org.syuzhakov.moneytranfer.database.BalanceRepository;
import org.syuzhakov.moneytranfer.database.OperationRepository;
import org.syuzhakov.moneytranfer.error.BadRequestException;
import org.syuzhakov.moneytranfer.error.OperationImpossibleException;
import org.syuzhakov.moneytranfer.error.Require;
import org.syuzhakov.moneytranfer.error.ValidationException;
import org.syuzhakov.moneytranfer.model.Account;
import org.syuzhakov.moneytranfer.model.AccountStatus;
import org.syuzhakov.moneytranfer.model.Balance;
import org.syuzhakov.moneytranfer.model.Operation;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class OperationServiceImpl implements OperationService {
    private final AccountRepository accountRepository;
    private final BalanceRepository balanceRepository;
    private final OperationRepository operationRepository;

    @Override
    public Operation transferMoney(Operation operation) {
        //Validate input
        Require.notNull(operation.getAmount(), "amount");
        if (operation.getAmount() <= 0) {
            throw new ValidationException("Operation amount must be positive");
        }
        final Long sourceAccountId = operation.getSourceAccount();
        final Long targetAccountId = operation.getTargetAccount();
        if (sourceAccountId == null && targetAccountId == null) {
            throw new ValidationException("At least one of 'sourceAccount' or 'targetAccount' should present");
        }
        if (Objects.equals(sourceAccountId, targetAccountId)) {
            throw new ValidationException("Operation with same source and target account is impossible");
        }

        //Check accounts status
        Stream.of(sourceAccountId, targetAccountId).filter(Objects::nonNull).forEach(accountId -> {
            final Account account = accountRepository.getAccountById(accountId, true);
            if (account == null) {
                throw new BadRequestException(String.format("Account %s does not exist", account));
            } else if (account.getStatus() != AccountStatus.ACTIVE) {
                throw new OperationImpossibleException(String.format("Account %s is not active", account));
            }
        });

        //Check balance of source account and apply lock
        long sourceBalance = 0;
        if (sourceAccountId != null) {
            sourceBalance = Optional.ofNullable(balanceRepository.getBalance(sourceAccountId, true)).map(Balance::getBalance).orElse(0L);
            if (sourceBalance < operation.getAmount()) {
                throw new OperationImpossibleException("Insufficient balance for account: " + sourceAccountId);
            }
        }

        //Apply lock for target account
        long targetBalance = 0;
        if (targetAccountId != null) {
            targetBalance = Optional.ofNullable(balanceRepository.getBalance(targetAccountId, true)).map(Balance::getBalance).orElse(0L);
        }

        //Create operation
        operation = operationRepository.createOperation(operation);

        //Change source account balance
        if (sourceAccountId != null) {
            balanceRepository.updateBalance(sourceAccountId, sourceBalance - operation.getAmount(), operation);
        }

        //Change target account balance
        if (targetAccountId != null) {
            balanceRepository.updateBalance(targetAccountId, targetBalance + operation.getAmount(), operation);
        }

        return operation;
    }

}
