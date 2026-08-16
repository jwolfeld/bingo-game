// src/App.jsx
import React, { useState } from 'react';
import SetupMode from './components/SetupMode';
import PlayMode  from './components/PlayMode';
import GameOver  from './components/GameOver';
import './App.css';

// App phases
const PHASE = { SETUP: 'setup', PLAY: 'play', OVER: 'over' };

export default function App() {
  const [phase, setPhase]   = useState(PHASE.SETUP);
  const [gameId, setGameId] = useState(null);

  function handleGameCreated(id) {
    setGameId(id);
    setPhase(PHASE.PLAY);
  }

  function handleGameOver() {
    setPhase(PHASE.OVER);
  }

  function handleNewGame() {
    setGameId(null);
    setPhase(PHASE.SETUP);
  }

  return (
    <div className="app">
      <header className="app-header">
        <div className="logo">
          <span className="logo-b">B</span>
          <span className="logo-i">I</span>
          <span className="logo-n">N</span>
          <span className="logo-g">G</span>
          <span className="logo-o">O</span>
        </div>
        <div className="header-right">
          <span className="phase-indicator">
            {phase === PHASE.SETUP && 'Setup'}
            {phase === PHASE.PLAY  && 'Playing'}
            {phase === PHASE.OVER  && 'Finished'}
          </span>
        </div>
      </header>

      <main className="app-main">
        {phase === PHASE.SETUP && (
          <SetupMode onGameCreated={handleGameCreated} />
        )}
        {phase === PHASE.PLAY && gameId && (
          <PlayMode gameId={gameId} onGameOver={handleGameOver} />
        )}
        {phase === PHASE.OVER && (
          <GameOver onNewGame={handleNewGame} />
        )}
      </main>
    </div>
  );
}
