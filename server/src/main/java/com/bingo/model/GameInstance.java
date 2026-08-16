package com.bingo.model;

import java.util.List;

/**
 * Represents a single bingo game instance.
 * Contains all game state: word list, grids, called words, etc.
 */
public class GameInstance {

    private String id;
    private List<String> wordList;          // All words provided by the admin
    private List<BingoGrid> grids;          // One grid per player
    private List<String> calledWords;       // Words called so far (in order called)
    private List<String> remainingWords;    // Words not yet called
    private GameState state;

    public enum GameState {
        SETUP,
        PLAYING,
        FINISHED
    }

    public GameInstance() {}

    public GameInstance(String id, List<String> wordList, List<BingoGrid> grids,
                        List<String> calledWords, List<String> remainingWords, GameState state) {
        this.id = id;
        this.wordList = wordList;
        this.grids = grids;
        this.calledWords = calledWords;
        this.remainingWords = remainingWords;
        this.state = state;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<String> getWordList() { return wordList; }
    public void setWordList(List<String> wordList) { this.wordList = wordList; }

    public List<BingoGrid> getGrids() { return grids; }
    public void setGrids(List<BingoGrid> grids) { this.grids = grids; }

    public List<String> getCalledWords() { return calledWords; }
    public void setCalledWords(List<String> calledWords) { this.calledWords = calledWords; }

    public List<String> getRemainingWords() { return remainingWords; }
    public void setRemainingWords(List<String> remainingWords) { this.remainingWords = remainingWords; }

    public GameState getState() { return state; }
    public void setState(GameState state) { this.state = state; }
}
