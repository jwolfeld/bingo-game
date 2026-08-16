package com.bingo.storage;

import com.bingo.model.GameInstance;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link GameStorage}.
 *
 * All game state is held in a ConcurrentHashMap and will be lost on server restart.
 * Replace this bean with a database-backed implementation when persistence is needed.
 */
@Component
public class InMemoryGameStorage implements GameStorage {

    private final Map<String, GameInstance> store = new ConcurrentHashMap<>();

    @Override
    public GameInstance save(GameInstance game) {
        store.put(game.getId(), game);
        return game;
    }

    @Override
    public Optional<GameInstance> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
