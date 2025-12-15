package fr.kovelya.accounting.application.service.impl;

import fr.kovelya.accounting.domain.repository.IdempotencyRecord;
import fr.kovelya.accounting.domain.repository.IdempotencyRepository;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class IdempotencyExecutor {

    private final IdempotencyRepository idempotencyRepository;

    public IdempotencyExecutor(IdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = Objects.requireNonNull(idempotencyRepository);
    }

    public <T> T run(UUID commandId, Supplier<T> action, Supplier<T> replayResult) {
        if (commandId == null) {
            throw new IllegalArgumentException("commandId is required");
        }
        if (idempotencyRepository.find(commandId).isPresent()) {
            return replayResult.get();
        }
        T result = action.get();
        String ref = result == null ? null : result.toString();
        idempotencyRepository.save(IdempotencyRecord.done(commandId, ref));
        return result;
    }

    public void runVoid(UUID commandId, Runnable action, Runnable replayAction) {
        if (commandId == null) {
            throw new IllegalArgumentException("commandId is required");
        }
        if (idempotencyRepository.find(commandId).isPresent()) {
            replayAction.run();
            return;
        }
        action.run();
        idempotencyRepository.save(IdempotencyRecord.done(commandId, null));
    }
}
