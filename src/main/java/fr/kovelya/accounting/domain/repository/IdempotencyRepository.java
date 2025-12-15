package fr.kovelya.accounting.domain.repository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository {

    Optional<IdempotencyRecord> find(UUID commandId);
    void save(IdempotencyRecord record);

}
