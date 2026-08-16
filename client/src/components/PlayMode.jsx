// src/components/PlayMode.jsx
import React, { useState, useEffect, useCallback } from 'react';
import { startGame, nextWord, endGame, getGameState } from '../api/bingoApi';

export default function PlayMode({ gameId, onGameOver }) {
  const [state, setState]           = useState('loading'); // loading | playing | finished | error
  const [currentWord, setCurrentWord] = useState(null);
  const [calledWords, setCalledWords] = useState([]);
  const [remainingCount, setRemaining] = useState(0);
  const [error, setError]           = useState('');
  const [calling, setCalling]       = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  // Load initial game state and start it
  const initialize = useCallback(async () => {
    try {
      await startGame(gameId);
      const gs = await getGameState(gameId);
      setState('playing');
      setCalledWords(gs.calledWords || []);
      setRemaining(gs.remainingCount);
    } catch (e) {
      setError(e.message);
      setState('error');
    }
  }, [gameId]);

  useEffect(() => { initialize(); }, [initialize]);

  async function handleNextWord() {
    setCalling(true);
    setError('');
    try {
      const result = await nextWord(gameId);
      setCurrentWord(result.word);
      setCalledWords(result.calledWords);
      setRemaining(result.remainingCount);
    } catch (e) {
      setError(e.message);
    } finally {
      setCalling(false);
    }
  }

  async function handleEndGame() {
    try {
      await endGame(gameId);
      setState('finished');
      onGameOver();
    } catch (e) {
      setError(e.message);
    }
  }

  const totalWords = calledWords.length + remainingCount;
  const progress   = totalWords > 0 ? (calledWords.length / totalWords) * 100 : 0;

  if (state === 'loading') {
    return <div className="loading-screen"><div className="spinner" />Starting game…</div>;
  }

  if (state === 'error') {
    return <div className="error-screen"><p>Error: {error}</p></div>;
  }

  return (
    <div className="play-mode">
      {/* Header bar */}
      <div className="play-header">
        <div className="mode-badge play">PLAY MODE</div>
        <div className="progress-info">
          <span>{calledWords.length} called / {remainingCount} remaining</span>
          <div className="progress-bar">
            <div className="progress-fill" style={{ width: `${progress}%` }} />
          </div>
        </div>
      </div>

      {/* Current word display */}
      <div className="current-word-panel">
        {currentWord ? (
          <>
            <div className="current-word-label">Current Word</div>
            <div className="current-word">{currentWord}</div>
            <div className="current-word-hint">Call this out to players ↑</div>
          </>
        ) : (
          <div className="current-word-empty">
            Press "Next Word" to begin calling
          </div>
        )}
      </div>

      {/* Action buttons */}
      <div className="play-actions">
        <button
          className="next-word-btn"
          onClick={handleNextWord}
          disabled={calling || remainingCount === 0}
        >
          {calling ? 'Drawing…' : remainingCount === 0 ? 'All Words Called' : 'Next Word →'}
        </button>

        <button
          className="game-over-btn"
          onClick={() => setShowConfirm(true)}
        >
          Game Over
        </button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {/* Called words history */}
      {calledWords.length > 0 && (
        <div className="called-words-panel">
          <h3 className="panel-title">
            Called Words
            <span className="called-count">{calledWords.length}</span>
          </h3>
          <div className="called-words-grid">
            {calledWords.map((word, i) => (
              <div
                key={word}
                className={`called-word-chip ${word === currentWord ? 'current' : ''}`}
              >
                {word}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Confirm game over dialog */}
      {showConfirm && (
        <div className="modal-backdrop">
          <div className="modal">
            <h3>End the Game?</h3>
            <p>This will mark the game as finished. You won't be able to call more words.</p>
            <div className="modal-actions">
              <button className="secondary-btn" onClick={() => setShowConfirm(false)}>
                Cancel
              </button>
              <button className="danger-btn" onClick={handleEndGame}>
                Yes, End Game
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
