// src/components/SetupMode.jsx
import React, { useState } from 'react';
import { createGame, downloadGrids } from '../api/bingoApi';

export default function SetupMode({ onGameCreated }) {
  const [wordsText, setWordsText]     = useState('');
  const [playerCount, setPlayerCount] = useState(4);
  const [loading, setLoading]         = useState(false);
  const [error, setError]             = useState('');
  const [gameId, setGameId]           = useState(null);
  const [gridCount, setGridCount]     = useState(0);
  const [downloading, setDownloading] = useState(false);

  const wordList = wordsText
    .split(/[\n,]+/)
    .map(w => w.trim().toUpperCase())
    .filter(w => w.length > 0);

  const uniqueCount = new Set(wordList).size;

  async function handleCreate() {
    setError('');
    if (uniqueCount < 24) {
      setError(`You need at least 24 unique words. Currently: ${uniqueCount}`);
      return;
    }
    setLoading(true);
    try {
      const result = await createGame(wordList, playerCount);
      setGameId(result.gameId);
      setGridCount(result.gridCount);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleDownload() {
    setDownloading(true);
    try {
      const blob = await downloadGrids(gameId);
      const url  = URL.createObjectURL(blob);
      const a    = document.createElement('a');
      a.href     = url;
      a.download = 'bingo_grids.zip';
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError(e.message);
    } finally {
      setDownloading(false);
    }
  }

  function handleStartPlay() {
    onGameCreated(gameId);
  }

  return (
    <div className="setup-mode">
      <div className="setup-header">
        <div className="mode-badge">SETUP MODE</div>
        <h2>Configure Your Bingo Game</h2>
        <p className="subtitle">Enter your word list and number of players to generate unique bingo grids.</p>
      </div>

      {!gameId ? (
        <div className="setup-form">
          <div className="form-section">
            <label className="field-label">
              Word List
              <span className="word-count" data-ok={uniqueCount >= 24}>
                {uniqueCount} unique word{uniqueCount !== 1 ? 's' : ''} {uniqueCount >= 24 ? '✓' : `(need ${24 - uniqueCount} more)`}
              </span>
            </label>
            <textarea
              className="word-input"
              value={wordsText}
              onChange={e => setWordsText(e.target.value.toUpperCase())}
              placeholder={"Enter words separated by commas or new lines:\n\nAPPLE, BANANA, CHERRY\nDRAGON, EAGLE, FOREST\n..."}
              rows={12}
            />
            <p className="field-hint">Minimum 24 unique words required. Duplicates are ignored.</p>
          </div>

          <div className="form-section player-section">
            <label className="field-label">Number of Players</label>
            <div className="player-input-row">
              <button
                className="stepper-btn"
                onClick={() => setPlayerCount(Math.max(1, playerCount - 1))}
              >−</button>
              <input
                type="number"
                className="player-count-input"
                value={playerCount}
                min={1}
                onChange={e => setPlayerCount(Math.max(1, parseInt(e.target.value) || 1))}
              />
              <button
                className="stepper-btn"
                onClick={() => setPlayerCount(playerCount + 1)}
              >+</button>
              <span className="player-label">unique grids will be generated</span>
            </div>
          </div>

          {error && <div className="error-banner">{error}</div>}

          <button
            className="primary-btn"
            onClick={handleCreate}
            disabled={loading || uniqueCount < 24}
          >
            {loading ? 'Generating Grids…' : `Generate ${playerCount} Player Grid${playerCount !== 1 ? 's' : ''}`}
          </button>
        </div>
      ) : (
        <div className="setup-complete">
          <div className="success-card">
            <div className="success-icon">✓</div>
            <h3>Grids Ready!</h3>
            <p>{gridCount} unique player grids have been generated.</p>
            <p className="game-id-label">Game ID: <code>{gameId}</code></p>
          </div>

          <div className="setup-actions">
            <button
              className="secondary-btn"
              onClick={handleDownload}
              disabled={downloading}
            >
              {downloading ? 'Preparing Download…' : '⬇ Download All Grids (ZIP)'}
            </button>

            <button className="primary-btn" onClick={handleStartPlay}>
              Start Game →
            </button>
          </div>

          {error && <div className="error-banner">{error}</div>}
        </div>
      )}
    </div>
  );
}
