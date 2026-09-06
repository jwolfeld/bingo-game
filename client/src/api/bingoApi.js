// src/api/bingoApi.js
// All REST calls to the Java Spring Boot backend.

const BASE       = '/bingo/api/games';
const WORDS_BASE = '/bingo/api/random-words';

async function handleResponse(res) {
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const msg = data?.error || `HTTP ${res.status}`;
    throw new Error(msg);
  }
  return data;
}

/** POST /api/random-words — get N random words excluding the provided list */
export async function fetchRandomWords(count, excludeWords) {
  const res = await fetch(WORDS_BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ count, excludeWords }),
  });
  return handleResponse(res);
}

/** POST /api/games — create a new game */
export async function createGame(words, playerCount) {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ words, playerCount }),
  });
  return handleResponse(res);
}

/** POST /api/games/:id/add-grids — add more player grids */
export async function addGrids(gameId, playerCount) {
  const res = await fetch(`${BASE}/${gameId}/add-grids`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ playerCount }),
  });
  return handleResponse(res);
}

/** POST /api/games/:id/start */
export async function startGame(gameId) {
  const res = await fetch(`${BASE}/${gameId}/start`, { method: 'POST' });
  return handleResponse(res);
}

/** GET /api/games/:id/state */
export async function getGameState(gameId) {
  const res = await fetch(`${BASE}/${gameId}/state`);
  return handleResponse(res);
}

/** POST /api/games/:id/next-word */
export async function nextWord(gameId) {
  const res = await fetch(`${BASE}/${gameId}/next-word`, { method: 'POST' });
  return handleResponse(res);
}

/** POST /api/games/:id/end */
export async function endGame(gameId) {
  const res = await fetch(`${BASE}/${gameId}/end`, { method: 'POST' });
  return handleResponse(res);
}

/**
 * GET /api/games/:id/download-grids?from=N — returns a blob.
 * from=0 (default) downloads all grids; from=N downloads only grids from index N onward.
 */
export async function downloadGrids(gameId, from = 0) {
  const url = `${BASE}/${gameId}/download-grids?from=${from}`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Download failed: HTTP ${res.status}`);
  return res.blob();
}
