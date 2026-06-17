package org.ptrades.flairhq.processor;

import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ProcessorUtils {

    private ProcessorUtils() {}

    public static <ID> void deleteOrThrow(CrudRepository<?, ID> repository, ID id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        repository.deleteById(id);
    }
}
