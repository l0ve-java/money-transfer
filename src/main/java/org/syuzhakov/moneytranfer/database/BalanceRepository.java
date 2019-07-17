package org.syuzhakov.moneytranfer.database;

import org.syuzhakov.moneytranfer.model.Balance;
import org.syuzhakov.moneytranfer.model.Operation;

public interface BalanceRepository {
    Balance getBalance(Long accountId, boolean forUpdate);

    void updateBalance(Long accountId, Long balance, Operation operation);
}
