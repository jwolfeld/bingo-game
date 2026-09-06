// src/components/SetupMode.jsx
import React, { useState } from 'react';
import { createGame, addGrids, downloadGrids, fetchRandomWords } from '../api/bingoApi';

export default function SetupMode({ onGameCreated }) {
  const [wordsText, setWordsText]         = useState('');
  const [playerCount, setPlayerCount]     = useState(4);
  const [loading, setLoading]             = useState(false);
  const [error, setError]                 = useState('');
  const [gameId, setGameId]               = useState(null);
  const [totalGridCount, setTotalGridCount] = useState(0);
  const [downloading, setDownloading]     = useState(false);
  const [downloadCount, setDownloadCount] = useState(0); // how many zip files produced
  const [lastDownloadedIndex, setLastDownloadedIndex] = useState(0);
  const [addingGrids, setAddingGrids]     = useState(false);
  const [addPlayerCount, setAddPlayerCount] = useState(4);
  const [fetchingWords, setFetchingWords] = useState(false);
  const [randomCount, setRandomCount]     = useState('');  // controlled input, string

  const wordList = wordsText
    .split(/[\n,]+/)
    .map(w => w.trim().toUpperCase())
    .filter(w => w.length > 0);

  const uniqueCount = new Set(wordList).size;

  // Default random word count = max(1, 24 - current unique word count)
  const defaultRandomCount = Math.max(1, 24 - uniqueCount);

  // The count field shows defaultRandomCount as placeholder when empty
  const effectiveRandomCount = randomCount === ''
    ? defaultRandomCount
    : parseInt(randomCount, 10);

  async function handleFetchRandomWords() {
    const count = effectiveRandomCount;
    if (isNaN(count) || count < 1 || count > 99) {
      setError('Please enter a number between 1 and 99.');
      return;
    }
    setFetchingWords(true);
    setError('');
    try {
      const result = await fetchRandomWords(count, wordList);
      if (result.words.length === 0) {
        setError('No more words available in the dictionary that aren\'t already in your list.');
        return;
      }
      // Append new words to the textarea
      const newWords = result.words.join('\n');
      setWordsText(prev => {
        const trimmed = prev.trimEnd();
        return trimmed ? trimmed + '\n' + newWords : newWords;
      });
    } catch (e) {
      setError(e.message);
    } finally {
      setFetchingWords(false);
    }
  }

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
      setTotalGridCount(result.gridCount);
      setLastDownloadedIndex(0);
      setDownloadCount(0);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleDownload(fromIndex, label) {
    setDownloading(true);
    setError('');
    try {
      const blob = await downloadGrids(gameId, fromIndex);
      const url  = URL.createObjectURL(blob);
      const a    = document.createElement('a');
      a.href     = url;
      a.download = label;
      a.click();
      URL.revokeObjectURL(url);
      setLastDownloadedIndex(totalGridCount);
      setDownloadCount(c => c + 1);
    } catch (e) {
      setError(e.message);
    } finally {
      setDownloading(false);
    }
  }

  async function handleAddGrids() {
    setError('');
    setAddingGrids(true);
    try {
      const result = await addGrids(gameId, addPlayerCount);
      setTotalGridCount(result.totalGridCount);
      // fromIndex tells us where the new batch starts — store it so the
      // download button fetches only the new grids
      setLastDownloadedIndex(result.fromIndex);
      setDownloadCount(c => c + 1); // treat pending new batch as next download
    } catch (e) {
      setError(e.message);
    } finally {
      setAddingGrids(false);
    }
  }

  function handleStartPlay() {
    onGameCreated(gameId);
  }

  // Download button label for the initial download vs additional batches
  const newGridsAvailable = totalGridCount > lastDownloadedIndex;
  const newGridCount      = totalGridCount - lastDownloadedIndex;

  return (
    <div className="setup-mode">
      <div className="setup-header">
        <div className="mode-badge">SETUP MODE</div>
        <h2>Configure Your Bingo Game</h2>
        <p className="subtitle">Enter your word list and number of players to generate unique bingo grids.</p>
      </div>

      {!gameId ? (
        <div className="setup-form">

          {/* ── Word list ── */}
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
              rows={10}
            />
            <p className="field-hint">Minimum 24 unique words required. Duplicates are ignored.</p>
          </div>

          {/* ── Random word generator ── */}
          <div className="form-section random-words-section">
            <label className="field-label">Add Random Words from Dictionary</label>
            <div className="random-words-row">
              <span className="random-label">Generate</span>
              <input
                type="number"
                className="random-count-input"
                value={randomCount}
                min={1}
                max={99}
                placeholder={String(defaultRandomCount)}
                onChange={e => setRandomCount(e.target.value)}
              />
              <span className="random-label">words</span>
              <button
                className="random-btn"
                onClick={handleFetchRandomWords}
                disabled={fetchingWords}
              >
                {fetchingWords ? 'Fetching…' : '+ Add Random Words'}
              </button>
            </div>
            <p className="field-hint">
              Words are drawn from a built-in dictionary and will not duplicate words already in your list.
              Default count fills to 24. You can add more as many times as you like.
            </p>
          </div>

          {/* ── Player count ── */}
          <div className="form-section player-section">
            <label className="field-label">Number of Players</label>
            <div className="player-input-row">
              <button className="stepper-btn" onClick={() => setPlayerCount(Math.max(1, playerCount - 1))}>−</button>
              <input
                type="number"
                className="player-count-input"
                value={playerCount}
                min={1}
                onChange={e => setPlayerCount(Math.max(1, parseInt(e.target.value) || 1))}
              />
              <button className="stepper-btn" onClick={() => setPlayerCount(playerCount + 1)}>+</button>
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

          {/* ── Summary card ── */}
          <div className="success-card">
            <div className="success-icon">✓</div>
            <h3>Grids Ready!</h3>
            <p>{totalGridCount} unique player grid{totalGridCount !== 1 ? 's' : ''} generated in total.</p>
            <p className="game-id-label">Game ID: <code>{gameId}</code></p>
          </div>

          {/* ── Download current batch ── */}
          {newGridsAvailable && (
            <div className="action-card">
              <h4 className="action-card-title">
                {downloadCount === 0
                  ? `Download all ${totalGridCount} grid${totalGridCount !== 1 ? 's' : ''}`
                  : `Download ${newGridCount} new grid${newGridCount !== 1 ? 's' : ''} (players ${lastDownloadedIndex + 1}–${totalGridCount})`}
              </h4>
              <button
                className="secondary-btn"
                onClick={() => handleDownload(
                  lastDownloadedIndex,
                  downloadCount === 0
                    ? 'bingo_grids.zip'
                    : `bingo_grids_players_${lastDownloadedIndex + 1}-${totalGridCount}.zip`
                )}
                disabled={downloading}
              >
                {downloading ? 'Preparing Download…' : '⬇ Download ZIP'}
              </button>
            </div>
          )}

          {/* ── Add more grids ── */}
          <div className="action-card">
            <h4 className="action-card-title">Generate Additional Grids</h4>
            <div className="player-input-row">
              <button className="stepper-btn" onClick={() => setAddPlayerCount(Math.max(1, addPlayerCount - 1))}>−</button>
              <input
                type="number"
                className="player-count-input"
                value={addPlayerCount}
                min={1}
                onChange={e => setAddPlayerCount(Math.max(1, parseInt(e.target.value) || 1))}
              />
              <button className="stepper-btn" onClick={() => setAddPlayerCount(addPlayerCount + 1)}>+</button>
              <span className="player-label">additional grids</span>
            </div>
            <button
              className="secondary-btn"
              style={{ marginTop: '12px' }}
              onClick={handleAddGrids}
              disabled={addingGrids}
            >
              {addingGrids ? 'Generating…' : `+ Generate ${addPlayerCount} More Grid${addPlayerCount !== 1 ? 's' : ''}`}
            </button>
          </div>

          {error && <div className="error-banner">{error}</div>}

          <button className="primary-btn" onClick={handleStartPlay}>
            Start Game →
          </button>
        </div>
      )}
    </div>
  );
}
