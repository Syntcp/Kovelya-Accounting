package fr.kovelya.accounting.domain.repository;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class IdempotencyRecord {

    private final UUID commandId;
    private final Instant createdAt;
    private final String resultRef;

    public IdempotencyRecord(UUID commandId, Instant createdAt, String resultRef) {
        this.commandId = Objects.requireNonNull(commandId);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.resultRef = resultRef;
    }

    public UUID commandId() {
        return commandId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String resultRef() {
        return resultRef;
    }

    public static IdempotencyRecord done(UUID commandId, String resultRef) {
        return new IdempotencyRecord(commandId, Instant.now(), resultRef);
    }
}

