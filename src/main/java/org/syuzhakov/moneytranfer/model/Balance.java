package org.syuzhakov.moneytranfer.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class Balance {
    private Long account;
    private Long operation;
    private Long balance;
    private Instant actuality;

    @Builder
    public Balance(Long account, Long operation, Long balance, Instant actuality) {
        this.account = account;
        this.operation = operation;
        this.balance = balance;
        this.actuality = actuality;
    }
}
