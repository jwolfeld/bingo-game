package com.bingo.model;

/**
 * Represents a single 5x5 bingo grid for one player.
 * The center cell (row 2, col 2, zero-indexed) is always FREE (null).
 */
public class BingoGrid {

    private int playerNumber;
    // 5x5 array; cells[2][2] is always null (FREE square)
    private String[][] cells;

    public BingoGrid() {}

    public BingoGrid(int playerNumber, String[][] cells) {
        this.playerNumber = playerNumber;
        this.cells = cells;
    }

    public int getPlayerNumber() { return playerNumber; }
    public void setPlayerNumber(int playerNumber) { this.playerNumber = playerNumber; }

    public String[][] getCells() { return cells; }
    public void setCells(String[][] cells) { this.cells = cells; }
}
