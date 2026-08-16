// src/components/GameOver.jsx
import React from 'react';

export default function GameOver({ onNewGame }) {
  return (
    <div className="game-over-screen">
      <div className="game-over-content">
        <div className="game-over-icon">🎉</div>
        <h2>Game Over!</h2>
        <p>The game has ended. We hope everyone had fun!</p>
        <button className="primary-btn" onClick={onNewGame}>
          Start a New Game
        </button>
      </div>
    </div>
  );
}
