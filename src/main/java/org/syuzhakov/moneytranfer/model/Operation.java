package org.syuzhakov.moneytranfer.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class Operation {
    private Long id;
    private Long sourceAccount;
    private Long targetAccount;
    private Long amount;
    private Instant timestamp;

    @Builder
    public Operation(Long id, Long sourceAccount, Long targetAccount, Long amount, Instant timestamp) {
        this.id = id;
        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
        this.amount = amount;
        this.timestamp = timestamp;
    }
}
