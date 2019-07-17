package org.syuzhakov.moneytranfer.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class Account {
    private Long id;
    private AccountStatus status;
    private Instant actuality;

    @Builder
    public Account(Long id, AccountStatus status, Instant actuality) {
        this.id = id;
        this.status = status;
        this.actuality = actuality;
    }
}
