package org.syuzhakov.moneytranfer.service;

import org.syuzhakov.moneytranfer.model.Operation;

public interface OperationService {
    Operation transferMoney(Operation operation);
}
