package fr.kovelya.accounting.infrastructure.persistence.memory;

import fr.kovelya.accounting.domain.repository.IdempotencyRecord;
import fr.kovelya.accounting.domain.repository.IdempotencyRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryIdempotencyRepository implements IdempotencyRepository {

    private final Map<UUID, IdempotencyRecord> storage = new HashMap<>();

    @Override
    public Optional<IdempotencyRecord> find(UUID commandId) {
        return Optional.ofNullable(storage.get(commandId));
    }

    @Override
    public void save(IdempotencyRecord record) {
        storage.put(record.commandId(), record);
    }
}
