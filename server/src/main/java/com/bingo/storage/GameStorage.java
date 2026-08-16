package com.bingo.storage;

import com.bingo.model.GameInstance;
import java.util.Optional;

/**
 * Storage abstraction for game instances.
 *
 * Implement this interface to provide alternative persistence strategies,
 * e.g. a database-backed implementation, file-based storage, etc.
 *
 * The default implementation is {@link InMemoryGameStorage}.
 */
public interface GameStorage {

    /**
     * Save or update a game instance.
     *
     * @param game the game instance to persist
     * @return the saved game instance (may differ if the implementation assigns an ID)
     */
    GameInstance save(GameInstance game);

    /**
     * Retrieve a game instance by its ID.
     *
     * @param id the game ID
     * @return an Optional containing the game if found, or empty if not found
     */
    Optional<GameInstance> findById(String id);

    /**
     * Delete a game instance by its ID.
     *
     * @param id the game ID to delete
     */
    void deleteById(String id);
}
