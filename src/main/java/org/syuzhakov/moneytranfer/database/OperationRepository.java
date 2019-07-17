package org.syuzhakov.moneytranfer.database;

import org.syuzhakov.moneytranfer.model.Operation;

public interface OperationRepository {
    Operation createOperation(Operation operation);
}
