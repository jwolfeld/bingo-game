package com.bingo.service;

import com.bingo.model.BingoGrid;
import com.bingo.model.GameInstance;
import com.bingo.storage.GameStorage;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core business logic for the Bingo game.
 */
@Service
public class GameService {

    private final GameStorage storage;

    public GameService(GameStorage storage) {
        this.storage = storage;
    }

    /**
     * Create a new game with the given word list and number of players.
     * Generates N unique 5x5 bingo grids (each with a FREE center cell).
     *
     * @param words       the administrator's word pool (must be >= 24)
     * @param playerCount number of player grids to generate (N)
     * @return the created GameInstance
     * @throws IllegalArgumentException if word count < 24
     */
    public GameInstance createGame(List<String> words, int playerCount) {
        // Trim, uppercase, and deduplicate words
        List<String> cleanWords = words.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(w -> !w.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (cleanWords.size() < 24) {
            throw new IllegalArgumentException(
                "At least 24 unique words are required. Provided: " + cleanWords.size());
        }
        if (playerCount < 1) {
            throw new IllegalArgumentException("Player count must be at least 1.");
        }

        String gameId = UUID.randomUUID().toString();
        List<BingoGrid> grids = generateGrids(cleanWords, playerCount);
        List<String> remaining = new ArrayList<>(cleanWords);
        Collections.shuffle(remaining);

        GameInstance game = new GameInstance(
                gameId,
                cleanWords,
                grids,
                new ArrayList<>(),
                remaining,
                GameInstance.GameState.SETUP
        );

        return storage.save(game);
    }

    /**
     * Add more player grids to an existing game that is still in SETUP state.
     * New grids are appended to the existing list, with player numbers continuing
     * from where the previous batch left off.
     *
     * @param gameId      the game to add grids to
     * @param playerCount number of additional grids to generate
     * @return the index of the first newly added grid (for partial zip download)
     */
    public int addGrids(String gameId, int playerCount) {
        GameInstance game = requireGame(gameId);
        if (game.getState() != GameInstance.GameState.SETUP) {
            throw new IllegalStateException("Cannot add grids after the game has started.");
        }
        if (playerCount < 1) {
            throw new IllegalArgumentException("Player count must be at least 1.");
        }

        int fromIndex = game.getGrids().size();
        int nextPlayerNumber = fromIndex + 1;

        for (int i = 0; i < playerCount; i++) {
            game.getGrids().add(generateSingleGrid(game.getWordList(), nextPlayerNumber++));
        }

        storage.save(game);
        return fromIndex;
    }

    /**
     * Retrieve a game by ID.
     */
    public Optional<GameInstance> getGame(String gameId) {
        return storage.findById(gameId);
    }

    /**
     * Transition a game from SETUP to PLAYING.
     */
    public GameInstance startGame(String gameId) {
        GameInstance game = requireGame(gameId);
        if (game.getState() != GameInstance.GameState.SETUP) {
            throw new IllegalStateException("Game is not in SETUP state.");
        }
        game.setState(GameInstance.GameState.PLAYING);
        return storage.save(game);
    }

    /**
     * Draw the next random word from the remaining pool.
     *
     * @return the drawn word
     */
    public String nextWord(String gameId) {
        GameInstance game = requireGame(gameId);
        if (game.getState() != GameInstance.GameState.PLAYING) {
            throw new IllegalStateException("Game is not in PLAYING state.");
        }
        List<String> remaining = game.getRemainingWords();
        if (remaining.isEmpty()) {
            throw new IllegalStateException("All words have been called.");
        }

        // Pick and remove a random word from remaining
        int idx = new Random().nextInt(remaining.size());
        String word = remaining.remove(idx);
        game.getCalledWords().add(word);

        storage.save(game);
        return word;
    }

    /**
     * End the game.
     */
    public GameInstance endGame(String gameId) {
        GameInstance game = requireGame(gameId);
        game.setState(GameInstance.GameState.FINISHED);
        return storage.save(game);
    }

    /**
     * Return called words sorted alphabetically.
     */
    public List<String> getCalledWordsSorted(String gameId) {
        GameInstance game = requireGame(gameId);
        return game.getCalledWords().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────

    /**
     * Generate N unique bingo grids from the word pool.
     * Each grid uses 24 randomly chosen words (no duplicates within a grid).
     * The center cell (row 2, col 2) is null (FREE square).
     */
    private List<BingoGrid> generateGrids(List<String> wordPool, int count) {
        List<BingoGrid> grids = new ArrayList<>();
        for (int p = 0; p < count; p++) {
            grids.add(generateSingleGrid(wordPool, p + 1));
        }
        return grids;
    }

    private BingoGrid generateSingleGrid(List<String> wordPool, int playerNumber) {
        List<String> shuffled = new ArrayList<>(wordPool);
        Collections.shuffle(shuffled);

        // Take the first 24 words for this grid
        List<String> selected = shuffled.subList(0, 24);

        String[][] cells = new String[5][5];
        int wordIdx = 0;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                if (row == 2 && col == 2) {
                    cells[row][col] = null; // FREE center
                } else {
                    cells[row][col] = selected.get(wordIdx++);
                }
            }
        }
        return new BingoGrid(playerNumber, cells);
    }

    private GameInstance requireGame(String gameId) {
        return storage.findById(gameId)
                .orElseThrow(() -> new NoSuchElementException("Game not found: " + gameId));
    }
}
