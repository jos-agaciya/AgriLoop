package com.agriloop.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic base repository contract.
 *
 * @param <T> Entity type
 * @param <ID> Primary key identifier type
 */
public interface BaseRepository<T, ID> {
    Optional<T> findById(ID id);
    List<T> findAll();
    T save(T entity);
    boolean deleteById(ID id);
    long count();
}
