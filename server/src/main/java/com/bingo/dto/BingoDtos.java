package com.bingo.dto;

import com.bingo.model.BingoGrid;
import com.bingo.model.GameInstance;

import java.util.List;

/**
 * Data Transfer Objects for the Bingo REST API.
 */
public class BingoDtos {

    // ── Requests ──────────────────────────────────────────────────

    /** POST /api/games — create a new game */
    public static class CreateGameRequest {
        private List<String> words;
        private int playerCount;

        public List<String> getWords() { return words; }
        public void setWords(List<String> words) { this.words = words; }

        public int getPlayerCount() { return playerCount; }
        public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    }

    /** POST /api/games/{id}/add-grids — generate additional grids for an existing game */
    public static class AddGridsRequest {
        private int playerCount;

        public int getPlayerCount() { return playerCount; }
        public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    }

    /** POST /api/random-words — generate random words excluding existing ones */
    public static class RandomWordsRequest {
        private int count;
        private List<String> excludeWords;

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }

        public List<String> getExcludeWords() { return excludeWords; }
        public void setExcludeWords(List<String> excludeWords) { this.excludeWords = excludeWords; }
    }

    /** Returned for POST /api/random-words */
    public static class RandomWordsResponse {
        private List<String> words;

        public RandomWordsResponse(List<String> words) { this.words = words; }
        public List<String> getWords() { return words; }
    }

    /** Returned for POST /api/games/{id}/add-grids */
    public static class AddGridsResponse {
        private int newGridCount;
        private int totalGridCount;
        private int fromIndex;   // first new grid index (for partial download)

        public AddGridsResponse(int newGridCount, int totalGridCount, int fromIndex) {
            this.newGridCount   = newGridCount;
            this.totalGridCount = totalGridCount;
            this.fromIndex      = fromIndex;
        }

        public int getNewGridCount()   { return newGridCount; }
        public int getTotalGridCount() { return totalGridCount; }
        public int getFromIndex()      { return fromIndex; }
    }

    // ── Responses ─────────────────────────────────────────────────

    /** Returned when a game is created */
    public static class CreateGameResponse {
        private String gameId;
        private int gridCount;

        public CreateGameResponse(String gameId, int gridCount) {
            this.gameId = gameId;
            this.gridCount = gridCount;
        }

        public String getGameId() { return gameId; }
        public int getGridCount() { return gridCount; }
    }

    /** Returned for GET /api/games/{id}/state */
    public static class GameStateResponse {
        private String gameId;
        private GameInstance.GameState state;
        private String currentWord;
        private List<String> calledWords;   // alphabetically sorted
        private int remainingCount;
        private List<BingoGrid> grids;

        public GameStateResponse() {}

        public String getGameId() { return gameId; }
        public void setGameId(String gameId) { this.gameId = gameId; }

        public GameInstance.GameState getState() { return state; }
        public void setState(GameInstance.GameState state) { this.state = state; }

        public String getCurrentWord() { return currentWord; }
        public void setCurrentWord(String currentWord) { this.currentWord = currentWord; }

        public List<String> getCalledWords() { return calledWords; }
        public void setCalledWords(List<String> calledWords) { this.calledWords = calledWords; }

        public int getRemainingCount() { return remainingCount; }
        public void setRemainingCount(int remainingCount) { this.remainingCount = remainingCount; }

        public List<BingoGrid> getGrids() { return grids; }
        public void setGrids(List<BingoGrid> grids) { this.grids = grids; }
    }

    /** Returned for POST /api/games/{id}/next-word */
    public static class NextWordResponse {
        private String word;
        private List<String> calledWords;   // alphabetically sorted
        private int remainingCount;

        public NextWordResponse(String word, List<String> calledWords, int remainingCount) {
            this.word = word;
            this.calledWords = calledWords;
            this.remainingCount = remainingCount;
        }

        public String getWord() { return word; }
        public List<String> getCalledWords() { return calledWords; }
        public int getRemainingCount() { return remainingCount; }
    }

    /** Generic error wrapper */
    public static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) { this.error = error; }
        public String getError() { return error; }
    }
}
