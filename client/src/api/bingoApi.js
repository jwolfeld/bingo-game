// src/api/bingoApi.js
// All REST calls to the Java Spring Boot backend.

const BASE = '/bingo/api/games';

async function handleResponse(res) {
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const msg = data?.error || `HTTP ${res.status}`;
    throw new Error(msg);
  }
  return data;
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

/** GET /api/games/:id/download-grids — returns a blob */
export async function downloadGrids(gameId) {
  const res = await fetch(`${BASE}/${gameId}/download-grids`);
  if (!res.ok) throw new Error(`Download failed: HTTP ${res.status}`);
  return res.blob();
}
