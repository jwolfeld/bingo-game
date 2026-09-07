// src/components/SetupMode.jsx
import React, { useState } from 'react';
import { createGame, addGrids, downloadGrids, fetchRandomWords } from '../api/bingoApi';

export default function SetupMode({ onGameCreated }) {
  const [wordsText, setWordsText]           = useState('');
  const [playerCount, setPlayerCount]       = useState(4);
  const [loading, setLoading]               = useState(false);
  const [error, setError]                   = useState('');
  const [gameId, setGameId]                 = useState(null);
  const [totalGridCount, setTotalGridCount] = useState(0);
  const [addingGrids, setAddingGrids]       = useState(false);
  const [addPlayerCount, setAddPlayerCount] = useState(4);
  const [fetchingWords, setFetchingWords]   = useState(false);
  const [randomCount, setRandomCount]       = useState('');

  // Each entry: { from: number, to: number, downloading: boolean }
  // 'from' and 'to' are 0-based grid indices (to is exclusive, like slice).
  // Label is derived: "Players 1–4", "Players 5–8", etc.
  const [batches, setBatches] = useState([]);

  // ── Derived ────────────────────────────────────────────────────
  const wordList = wordsText
    .split(/[\n,]+/)
    .map(w => w.trim().toUpperCase())
    .filter(w => w.length > 0);

  const uniqueCount      = new Set(wordList).size;
  const defaultRandomCount = Math.max(1, 24 - uniqueCount);
  const effectiveRandomCount = randomCount === ''
    ? defaultRandomCount
    : parseInt(randomCount, 10);

  // ── Helpers ────────────────────────────────────────────────────
  function batchLabel(from, to) {
    // from/to are 0-based indices; player numbers are 1-based
    const first = from + 1;
    const last  = to;   // to is already exclusive so last player = to
    return first === last
      ? `Player ${first}`
      : `Players ${first}–${last}`;
  }

  function batchFilename(from, to) {
    const first = from + 1;
    const last  = to;
    return first === last
      ? `bingo_grid_player_${first}.zip`
      : `bingo_grids_players_${first}-${last}.zip`;
  }

  // ── Random words ───────────────────────────────────────────────
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
        setError("No more words available in the dictionary that aren't already in your list.");
        return;
      }
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

  // ── Create game ────────────────────────────────────────────────
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
      // First batch: players 1..gridCount
      setBatches([{ from: 0, to: result.gridCount, downloading: false }]);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  // ── Add grids ──────────────────────────────────────────────────
  async function handleAddGrids() {
    setError('');
    setAddingGrids(true);
    try {
      const result = await addGrids(gameId, addPlayerCount);
      const newFrom = result.fromIndex;
      const newTo   = result.totalGridCount;
      setTotalGridCount(newTo);
      setBatches(prev => [...prev, { from: newFrom, to: newTo, downloading: false }]);
    } catch (e) {
      setError(e.message);
    } finally {
      setAddingGrids(false);
    }
  }

  // ── Download a specific batch ──────────────────────────────────
  async function handleDownloadBatch(batchIndex) {
    const batch = batches[batchIndex];
    // Mark this batch as downloading
    setBatches(prev => prev.map((b, i) =>
      i === batchIndex ? { ...b, downloading: true } : b));
    setError('');
    try {
      const blob = await downloadGrids(gameId, batch.from);
      const url  = URL.createObjectURL(blob);
      const a    = document.createElement('a');
      a.href     = url;
      a.download = batchFilename(batch.from, batch.to);
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError(e.message);
    } finally {
      setBatches(prev => prev.map((b, i) =>
        i === batchIndex ? { ...b, downloading: false } : b));
    }
  }

  // ── Download all grids ─────────────────────────────────────────
  const [downloadingAll, setDownloadingAll] = useState(false);

  async function handleDownloadAll() {
    setDownloadingAll(true);
    setError('');
    try {
      const blob = await downloadGrids(gameId, 0);
      const url  = URL.createObjectURL(blob);
      const a    = document.createElement('a');
      a.href     = url;
      a.download = `bingo_grids_all_players_1-${totalGridCount}.zip`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError(e.message);
    } finally {
      setDownloadingAll(false);
    }
  }

  function handleStartPlay() {
    onGameCreated(gameId);
  }

  // ── Render ─────────────────────────────────────────────────────
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
                {uniqueCount} unique word{uniqueCount !== 1 ? 's' : ''}{' '}
                {uniqueCount >= 24 ? '✓' : `(need ${24 - uniqueCount} more)`}
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

          {/* ── Download buttons ── */}
          <div className="action-card">
            <h4 className="action-card-title">Download Grids</h4>

            <div className="batch-buttons">
              {batches.map((batch, i) => (
                <button
                  key={i}
                  className="batch-btn"
                  onClick={() => handleDownloadBatch(i)}
                  disabled={batch.downloading}
                >
                  {batch.downloading
                    ? 'Preparing…'
                    : `⬇ ${batchLabel(batch.from, batch.to)}`}
                </button>
              ))}

              {batches.length >= 2 && (
                <button
                  className="batch-btn batch-btn-all"
                  onClick={handleDownloadAll}
                  disabled={downloadingAll}
                >
                  {downloadingAll ? 'Preparing…' : `⬇ All Players 1–${totalGridCount}`}
                </button>
              )}
            </div>
          </div>

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
