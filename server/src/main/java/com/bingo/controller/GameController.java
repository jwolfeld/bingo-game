package com.bingo.controller;

import com.bingo.dto.BingoDtos.*;
import com.bingo.model.GameInstance;
import com.bingo.service.GameService;
import com.bingo.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * REST controller exposing all Bingo game endpoints.
 *
 * Base path: /api/games
 */
@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "*")   // Adjust for production
public class GameController {

    private final GameService gameService;
    private final PdfService  pdfService;

    public GameController(GameService gameService, PdfService pdfService) {
        this.gameService = gameService;
        this.pdfService  = pdfService;
    }

    // ── Create game ───────────────────────────────────────────────

    /**
     * POST /api/games
     * Body: { words: [...], playerCount: N }
     * Creates a new game instance in SETUP state.
     */
    @PostMapping
    public ResponseEntity<?> createGame(@RequestBody CreateGameRequest request) {
        try {
            GameInstance game = gameService.createGame(request.getWords(), request.getPlayerCount());
            return ResponseEntity.ok(new CreateGameResponse(game.getId(), game.getGrids().size()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── Game state ────────────────────────────────────────────────

    /**
     * GET /api/games/{id}/state
     * Returns full game state including grids, called words, etc.
     */
    @GetMapping("/{id}/state")
    public ResponseEntity<?> getState(@PathVariable String id) {
        return gameService.getGame(id)
                .map(game -> {
                    GameStateResponse resp = new GameStateResponse();
                    resp.setGameId(game.getId());
                    resp.setState(game.getState());
                    resp.setCalledWords(gameService.getCalledWordsSorted(id));
                    resp.setRemainingCount(game.getRemainingWords().size());
                    resp.setGrids(game.getGrids());
                    if (!game.getCalledWords().isEmpty()) {
                        resp.setCurrentWord(game.getCalledWords().get(game.getCalledWords().size() - 1));
                    }
                    return ResponseEntity.ok((Object) resp);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Start game ────────────────────────────────────────────────

    /**
     * POST /api/games/{id}/start
     * Transitions the game from SETUP → PLAYING.
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<?> startGame(@PathVariable String id) {
        try {
            GameInstance game = gameService.startGame(id);
            GameStateResponse resp = new GameStateResponse();
            resp.setGameId(game.getId());
            resp.setState(game.getState());
            resp.setCalledWords(List.of());
            resp.setRemainingCount(game.getRemainingWords().size());
            resp.setGrids(game.getGrids());
            return ResponseEntity.ok(resp);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── Next word ─────────────────────────────────────────────────

    /**
     * POST /api/games/{id}/next-word
     * Draws the next random word. Returns the word and updated called-word list.
     */
    @PostMapping("/{id}/next-word")
    public ResponseEntity<?> nextWord(@PathVariable String id) {
        try {
            String word = gameService.nextWord(id);
            List<String> called = gameService.getCalledWordsSorted(id);
            int remaining = gameService.getGame(id)
                    .map(g -> g.getRemainingWords().size())
                    .orElse(0);
            return ResponseEntity.ok(new NextWordResponse(word, called, remaining));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── End game ──────────────────────────────────────────────────

    /**
     * POST /api/games/{id}/end
     * Marks the game as FINISHED.
     */
    @PostMapping("/{id}/end")
    public ResponseEntity<?> endGame(@PathVariable String id) {
        try {
            GameInstance game = gameService.endGame(id);
            GameStateResponse resp = new GameStateResponse();
            resp.setGameId(game.getId());
            resp.setState(game.getState());
            resp.setCalledWords(gameService.getCalledWordsSorted(id));
            resp.setRemainingCount(0);
            return ResponseEntity.ok(resp);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Download PDFs ─────────────────────────────────────────────

    /**
     * GET /api/games/{id}/download-grids
     * Generates one PDF per player grid and returns them as a zip archive.
     */
    @GetMapping("/{id}/download-grids")
    public ResponseEntity<?> downloadGrids(@PathVariable String id) {
        return gameService.getGame(id).map(game -> {
            try {
                ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();
                try (ZipOutputStream zos = new ZipOutputStream(zipBuffer)) {
                    for (var grid : game.getGrids()) {
                        byte[] pdf = pdfService.generateGridPdf(grid, "Bingo");
                        String fileName = "player_" + grid.getPlayerNumber() + "_grid.pdf";
                        zos.putNextEntry(new ZipEntry(fileName));
                        zos.write(pdf);
                        zos.closeEntry();
                    }
                }

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("application/zip"));
                headers.setContentDispositionFormData("attachment", "bingo_grids.zip");

                return ResponseEntity.ok()
                        .headers(headers)
                        .body(zipBuffer.toByteArray());

            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(("PDF generation failed: " + e.getMessage()).getBytes());
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}
